package dev.streamsreflowing.offload;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.FileNetworkStore;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamBiomeBlacklist;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.RegistryAccess.ImmutableRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class WorldSnapshot {
   private static final Set<ResourceKey<? extends Registry<?>>> REGISTRIES = Set.of(
      Registries.NOISE, Registries.DENSITY_FUNCTION, Registries.NOISE_SETTINGS, Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST
   );

   private WorldSnapshot() {
   }

   public static byte[] capture(ServerLevel level, RiverEngine engine) throws IOException {
      ChunkGenerator generator = level.getChunkSource().getGenerator();
      RegistryAccess access = level.registryAccess();
      RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, access);
      CompoundTag root = new CompoundTag();
      root.putLong("seed", level.getSeed());
      root.putString("dim", level.dimension().location().toString());
      root.putInt("minY", level.getMinBuildHeight());
      root.putInt("height", level.getHeight());
      root.putBoolean("oceanSkip", Services.PLATFORM.fastOceanSkip());
      root.putInt("heights", StreamsWorldgen.heightsOf(engine));
      StreamsWorldgen.FastSpec fast = StreamsWorldgen.fastOf(engine);
      if (fast != null) {
         CompoundTag f = new CompoundTag();
         f.putInt("policy", fast.gradePolicy());
         f.putBoolean("baked", fast.baked());
         f.putLongArray("centers", bits(fast.centers()));
         f.putLongArray("bias", bits(fast.bias()));
         byte[] safe = new byte[fast.safe().length];

         for (int i = 0; i < safe.length; i++) {
            safe[i] = (byte)(fast.safe()[i] ? 1 : 0);
         }

         f.putByteArray("safe", safe);
         f.putInt("verdict", engine.estCostVerdict());
         root.put("fast", f);
      }

      root.put("gen", Mc.orThrow(ChunkGenerator.CODEC.encodeStart(ops, generator)));
      ResourceLocation ownSettings = generator instanceof NoiseBasedChunkGenerator noise
         ? noise.generatorSettings().unwrapKey().map(k -> k.location()).orElse(null)
         : null;
      CompoundTag regs = new CompoundTag();

      for (RegistryData<?> data : Mc.worldgenRegistries()) {
         if (REGISTRIES.contains(data.key())) {
            regs.put(
               data.key().location().toString(),
               pack(data, access, ops, data.key().equals(Registries.NOISE_SETTINGS) ? ownSettings : null, level.getServer().getResourceManager())
            );
         }
      }

      root.put("regs", regs);
      ByteArrayOutputStream cfg = new ByteArrayOutputStream();
      engine.config().write(new DataOutputStream(cfg));
      root.putByteArray("cfg", cfg.toByteArray());
      CompoundTag wet = new CompoundTag();
      CompoundTag hold = new CompoundTag();
      boolean barPlanned = engine.biomeBarAtPlanning();
      ListTag barred = new ListTag();
       Mc.holders((Registry<Biome>)Mc.registry(access, Registries.BIOME).orElseThrow()).forEach((Holder<Biome> biome) -> {
          String id = biome.unwrapKey().orElseThrow().location().toString();
         wet.putDouble(id, Services.PLATFORM.biomeWetness(biome));
         hold.putDouble(id, RiverCarverFeature.bankCohesion(level, biome));
         if (barPlanned && StreamBiomeBlacklist.isBlocked(biome)) {
            barred.add(StringTag.valueOf(id));
         }
      });
      root.put("wet", wet);
      root.put("hold", hold);
      root.put("barred", barred);
      root.putBoolean("barPlanned", barPlanned);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(65536);
      NbtIo.writeCompressed(root, bytes);
      return bytes.toByteArray();
   }

   private static <T> ListTag pack(RegistryData<T> data, RegistryAccess access, RegistryOps<Tag> ops, ResourceLocation only, ResourceManager files) {
      ListTag out = new ListTag();
      Optional<Registry<T>> registry = Mc.registry(access, data.key());
      if (registry.isEmpty()) {
         return out;
      }

      for (Entry<ResourceKey<T>, T> e : registry.get().entrySet()) {
         ResourceLocation id = e.getKey().location();
         if (only == null || only.equals(id)) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            Tag written = null;
            boolean faithful = false;

            try {
               written = Mc.orThrow(data.elementCodec().encodeStart(ops, e.getValue()));
               faithful = data.elementCodec().parse(ops, written).result().map(back -> back.equals(e.getValue())).orElse(false);
            } catch (RuntimeException var19) {
            }

            Tag live = faithful ? written : null;
            if (live == null) {
               ResourceLocation file = new ResourceLocation(id.getNamespace(), Mc.folderOf(data.key()) + "/" + id.getPath() + ".json");

               try (Reader reader = files.openAsReader(file)) {
                  live = (Tag)JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, JsonParser.parseReader(reader));
               } catch (IOException | RuntimeException missing) {
                  live = written;
               }

               if (live == null) {
                  throw new IllegalStateException(id + " can be neither written nor found as data");
               }
            }

            entry.put("d", live);
            out.add(entry);
         }
      }

      if (Boolean.getBoolean("streamsreflowing.offloaddiag")) {
         StringBuilder odd = new StringBuilder();
         Mc.holders(registry.get()).forEach(h -> {
            if (h.key().location().getPath().startsWith("__")) {
               odd.append(' ').append(h.key().location()).append(h.isBound() ? "(bound)" : "(UNBOUND)");
            }
         });
         Constants.LOG
            .info("[SR-OFFLOAD] packed {} of {} ({} keys){}", new Object[]{out.size(), data.key().location(), registry.get().keySet().size(), odd});
      }

      return out;
   }

   public static WorldSnapshot.World restore(byte[] packed, RegistryAccess local) throws IOException {
      CompoundTag root = Mc.readCompressed(new ByteArrayInputStream(packed));
      long seed = Mc.getLong(root, "seed");
      Map<ResourceKey<? extends Registry<?>>, List<Mc.Sent>> elements = new HashMap<>();
      List<RegistryData<?>> wanted = new ArrayList<>();
      CompoundTag regs = Mc.getCompound(root, "regs");

      for (RegistryData<?> data : Mc.worldgenRegistries()) {
         if (REGISTRIES.contains(data.key())) {
            wanted.add(data);
            List<Mc.Sent> entries = new ArrayList<>();
            ListTag list = Mc.getList(regs, data.key().location().toString(), false);

            for (int i = 0; i < list.size(); i++) {
               CompoundTag entry = Mc.compoundAt(list, i);
               entries.add(new Mc.Sent(Mc.id(Mc.getString(entry, "id")), entry.get("d")));
            }

            elements.put(data.key(), entries);
         }
      }

      Frozen sent = Mc.load(elements, local, wanted);
      Set<ResourceKey<? extends Registry<?>>> sentKeys = new HashSet<>();
      sent.registries().forEach(r -> sentKeys.add(r.key()));
      RegistryAccess both = new ImmutableRegistryAccess(Stream.concat(sent.registries(), local.registries().filter(r -> !sentKeys.contains(r.key()))))
         .freeze();
      ChunkGenerator generator = Mc.orThrow(ChunkGenerator.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, both), root.get("gen")));
      if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
         throw new IOException("this world's generator is not one a region can be prepared for here");
      } else {
         RandomState var35 = Mc.randomState((NoiseGeneratorSettings)noise.generatorSettings().value(), both, seed);
         LevelHeightAccessor heights = LevelHeightAccessor.create(Mc.getInt(root, "minY"), Mc.getInt(root, "height"));
         StreamsConfig cfg = StreamsConfig.read(OffloadWire.reader(Mc.getByteArray(root, "cfg")));
         final BiomeSource biomeSource = generator.getBiomeSource();
         final Sampler climate = var35.sampler();
         int how = Mc.getInt(root, "heights");
         StreamsWorldgen.FastSpec fast = null;
         int verdict = 0;
         if (Mc.hasCompound(root, "fast")) {
            CompoundTag f = Mc.getCompound(root, "fast");
            byte[] safeBytes = Mc.getByteArray(f, "safe");
            boolean[] safe = new boolean[safeBytes.length];

            for (int i = 0; i < safe.length; i++) {
               safe[i] = safeBytes[i] != 0;
            }

            fast = new StreamsWorldgen.FastSpec(
               Mc.getInt(f, "policy"), Mc.getBoolean(f, "baked"), doubles(Mc.getLongArray(f, "centers")), doubles(Mc.getLongArray(f, "bias")), safe
            );
            verdict = Mc.getInt(f, "verdict");
         }

         StreamsWorldgen.SamplerParts parts = StreamsWorldgen.SamplerParts.plain(
            generator,
            heights,
            var35,
            RiverFlowFields.newClassifier(biomeSource, climate, generator.getSeaLevel()),
            cfg.outletBiomes(),
            Mc.getBoolean(root, "oceanSkip"),
            vanilla -> {
               if (how == 0) {
                  return vanilla;
               }

               FormSampler form = StreamsWorldgen.formHeights(vanilla, noise, var35, heights, both, seed);
               form.setAquiferHandBack(how == 2);
               return form;
            },
            fast
         );
         final Map<String, Double> wet = doubles(Mc.getCompound(root, "wet"));
         final Map<String, Double> hold = doubles(Mc.getCompound(root, "hold"));
         final Set<String> barred = new HashSet<>();
         ListTag barredList = Mc.getList(root, "barred", true);

         for (int i = 0; i < barredList.size(); i++) {
            barred.add(Mc.stringAt(barredList, i));
         }

         WetnessSampler wetness = new WetnessSampler() {
            @Override
            public double wetness(int x, int y, int z) {
               return WorldSnapshot.lookup(wet, this.at(x, y, z), 0.5);
            }

            @Override
            public double cohesion(int x, int y, int z) {
               return WorldSnapshot.lookup(hold, this.at(x, y, z), 0.5);
            }

            @Override
            public boolean barsAnything() {
               return !barred.isEmpty();
            }

            @Override
            public boolean barred(int x, int y, int z) {
               return !barred.isEmpty() && this.at(x, y, z).unwrapKey().map(k -> barred.contains(k.location().toString())).orElse(false);
            }

            private Holder<Biome> at(int x, int y, int z) {
               return biomeSource.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), climate);
            }
         };
         Path dir = Files.createTempDirectory("streamsreflowing-regions");
         FileNetworkStore store = new FileNetworkStore(dir, seed, cfg);
         RiverEngine engine = StreamsWorldgen.detachedEngine(seed, cfg, parts, wetness, store);
         engine.setBiomeBarAtPlanning(Mc.getBoolean(root, "barPlanned"));
         if (fast != null) {
            engine.setEstCostVerdict(verdict);
         }

         return new WorldSnapshot.World(generator, var35, heights, engine, store, dir);
      }
   }

   public static String fields(RandomState rs, int x, int z) {
      NoiseRouter r = rs.router();
      SinglePointContext at = new SinglePointContext(x, 80, z);
      return String.format(
         Locale.ROOT,
         "cont %.4f ero %.4f ridge %.4f depth %.4f temp %.4f final %.4f",
         r.continents().compute(at),
         r.erosion().compute(at),
         r.ridges().compute(at),
         r.depth().compute(at),
         r.temperature().compute(at),
         r.finalDensity().compute(at)
      );
   }

   private static long[] bits(double[] values) {
      long[] out = new long[values.length];

      for (int i = 0; i < out.length; i++) {
         out[i] = Double.doubleToRawLongBits(values[i]);
      }

      return out;
   }

   private static double[] doubles(long[] bits) {
      double[] out = new double[bits.length];

      for (int i = 0; i < out.length; i++) {
         out[i] = Double.longBitsToDouble(bits[i]);
      }

      return out;
   }

   private static Map<String, Double> doubles(CompoundTag tag) {
      Map<String, Double> out = new HashMap<>();

      for (String key : Mc.keys(tag)) {
         out.put(key, Mc.getDouble(tag, key));
      }

      return out;
   }

   private static double lookup(Map<String, Double> byBiome, Holder<Biome> biome, double otherwise) {
      Double v = biome.unwrapKey().map(k -> byBiome.get(k.location().toString())).orElse(null);
      return v == null ? otherwise : v;
   }

   public static final class World {
      private final ChunkGenerator generator;
      private final RandomState randomState;
      private final LevelHeightAccessor heights;
      private final RiverEngine engine;
      private final FileNetworkStore store;
      private final Path dir;

      private World(ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights, RiverEngine engine, FileNetworkStore store, Path dir) {
         this.generator = generator;
         this.randomState = randomState;
         this.heights = heights;
         this.engine = engine;
         this.store = store;
         this.dir = dir;
      }

      public int groundAt(int x, int z) {
         return this.generator.getBaseHeight(x, z, Types.OCEAN_FLOOR_WG, this.heights, this.randomState);
      }

      public String fieldsAt(int x, int z) {
         return WorldSnapshot.fields(this.randomState, x, z);
      }

      public byte[] build(PlateKey key) throws IOException {
         RiverNetwork net = this.engine.baseFor(key);
         Path file = this.store.fileOf(key);
         if (!Files.isRegularFile(file)) {
            this.store.save(key, net);
         }

         if (!Files.isRegularFile(file)) {
            throw new IOException("the region was prepared but could not be written");
         }

         byte[] bytes = Files.readAllBytes(file);
         Files.deleteIfExists(file);
         return bytes;
      }

      public RiverEngine engine() {
         return this.engine;
      }

      public void close() {
         try (Stream<Path> files = Files.list(this.dir)) {
             for (Path f : (Iterable<Path>)files::iterator) {
               Files.deleteIfExists(f);
            }

            Files.deleteIfExists(this.dir);
         } catch (IOException | RuntimeException var6) {
         }
      }
   }
}
