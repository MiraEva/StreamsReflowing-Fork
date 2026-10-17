package dev.streamsreflowing.offload;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

final class Mc {
   private static final PackResources PACK = new PackResources() {
      public IoSupplier<InputStream> getRootResource(String... path) {
         return null;
      }

      public IoSupplier<InputStream> getResource(PackType type, ResourceLocation file) {
         return null;
      }

      public void listResources(PackType type, String namespace, String folder, ResourceOutput out) {
      }

      public Set<String> getNamespaces(PackType type) {
         return Set.of();
      }

      public <T> T getMetadataSection(MetadataSectionSerializer<T> section) {
         return null;
      }

      public String packId() {
         return "streamsreflowing-sent";
      }

      public void close() {
      }
   };

   private Mc() {
   }

   static long getLong(CompoundTag tag, String key) {
      return tag.getLong(key);
   }

   static int getInt(CompoundTag tag, String key) {
      return tag.getInt(key);
   }

   static boolean getBoolean(CompoundTag tag, String key) {
      return tag.getBoolean(key);
   }

   static double getDouble(CompoundTag tag, String key) {
      return tag.getDouble(key);
   }

   static String getString(CompoundTag tag, String key) {
      return tag.getString(key);
   }

   static byte[] getByteArray(CompoundTag tag, String key) {
      return tag.getByteArray(key);
   }

   static long[] getLongArray(CompoundTag tag, String key) {
      return tag.getLongArray(key);
   }

   static CompoundTag getCompound(CompoundTag tag, String key) {
      return tag.getCompound(key);
   }

   static boolean hasCompound(CompoundTag tag, String key) {
      return tag.get(key) instanceof CompoundTag;
   }

   static ListTag getList(CompoundTag tag, String key, boolean strings) {
      return tag.getList(key, strings ? 8 : 10);
   }

   static CompoundTag compoundAt(ListTag list, int i) {
      return list.getCompound(i);
   }

   static String stringAt(ListTag list, int i) {
      return list.getString(i);
   }

   static Set<String> keys(CompoundTag tag) {
      return tag.getAllKeys();
   }

   static CompoundTag readCompressed(InputStream in) throws IOException {
      return NbtIo.readCompressed(in);
   }

   static <T> T orThrow(DataResult<T> result) {
      return (T)result.getOrThrow(false, problem -> {});
   }

   static ResourceLocation id(String text) {
      return new ResourceLocation(text);
   }

   static String folderOf(ResourceKey<? extends Registry<?>> registry) {
      return registry.location().getPath();
   }

   static <T> Optional<Registry<T>> registry(RegistryAccess access, ResourceKey<? extends Registry<T>> key) {
      return access.registry(key);
   }

   static List<RegistryData<?>> worldgenRegistries() {
      return RegistryDataLoader.WORLDGEN_REGISTRIES;
   }

   static RandomState randomState(NoiseGeneratorSettings settings, RegistryAccess registries, long seed) {
      return RandomState.create(settings, registries.lookupOrThrow(Registries.NOISE), seed);
   }

   static <T> Stream<Reference<T>> holders(Registry<T> registry) {
      return registry.holders();
   }

   static Frozen load(Map<ResourceKey<? extends Registry<?>>, List<Mc.Sent>> sent, RegistryAccess local, List<RegistryData<?>> wanted) {
      return RegistryDataLoader.load(new Mc.SentFiles(sent), local, wanted);
   }

   record Sent(ResourceLocation id, Tag data) {
   }

   private static final class SentFiles implements ResourceManager {
      private final Map<ResourceLocation, byte[]> files = new HashMap<>();

      SentFiles(Map<ResourceKey<? extends Registry<?>>, List<Mc.Sent>> sent) {
         sent.forEach(
            (registry, entries) -> {
               for (Mc.Sent entry : entries) {
                  String json = ((JsonElement)NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, entry.data())).toString();
                  this.files
                     .put(
                        new ResourceLocation(
                           entry.id().getNamespace(), Mc.folderOf((ResourceKey<? extends Registry<?>>)registry) + "/" + entry.id().getPath() + ".json"
                        ),
                        json.getBytes(StandardCharsets.UTF_8)
                     );
               }
            }
         );
      }

      private static Resource resource(byte[] bytes) {
         return new Resource(Mc.PACK, () -> new ByteArrayInputStream(bytes));
      }

      public Set<String> getNamespaces() {
         Set<String> out = new HashSet<>();
         this.files.keySet().forEach(file -> out.add(file.getNamespace()));
         return out;
      }

      public Optional<Resource> getResource(ResourceLocation file) {
         byte[] bytes = this.files.get(file);
         return bytes == null ? Optional.empty() : Optional.of(resource(bytes));
      }

      public List<Resource> getResourceStack(ResourceLocation file) {
         byte[] bytes = this.files.get(file);
         return bytes == null ? List.of() : List.of(resource(bytes));
      }

      public Map<ResourceLocation, Resource> listResources(String folder, Predicate<ResourceLocation> filter) {
         Map<ResourceLocation, Resource> out = new TreeMap<>();
         this.files.forEach((file, bytes) -> {
            if (file.getPath().startsWith(folder + "/") && filter.test(file)) {
               out.put(file, resource(bytes));
            }
         });
         return out;
      }

      public Map<ResourceLocation, List<Resource>> listResourceStacks(String folder, Predicate<ResourceLocation> filter) {
         Map<ResourceLocation, List<Resource>> out = new TreeMap<>();
         this.listResources(folder, filter).forEach((file, resource) -> out.put(file, List.of(resource)));
         return out;
      }

      public Stream<PackResources> listPacks() {
         return Stream.of(Mc.PACK);
      }
   }
}
