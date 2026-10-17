package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.DynamicTreesCompat;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.api.FootprintVeto;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.config.HydraulicGeometry;
import dev.streamsreflowing.core.config.QualityPreset;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.math.Hashing;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.core.terrain.ChunkFillProbe;
import dev.streamsreflowing.core.terrain.ChunkHeightCache;
import dev.streamsreflowing.core.terrain.ReadLattice;
import dev.streamsreflowing.core.terrain.ReadLocality;
import dev.streamsreflowing.core.terrain.SampleClock;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.bank.BankFallenLogFeature;
import dev.streamsreflowing.worldgen.bank.BankRockFeature;
import dev.streamsreflowing.worldgen.bank.BankTemplateFeature;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FieldFormBuilder;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import dev.streamsreflowing.worldgen.regionprep.HeightAccuracy;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelResource;

public final class StreamsWorldgen {
   public static final TagKey<EntityType<?>> IGNORES_CURRENT = TagKey.create(
      Registries.ENTITY_TYPE, new ResourceLocation("streamsreflowing", "ignores_current")
   );
   public static final TagKey<EntityType<?>> DRIFTS_IN_CURRENT = TagKey.create(
      Registries.ENTITY_TYPE, new ResourceLocation("streamsreflowing", "drifts_in_current")
   );
   public static final ResourceLocation RIVER_CARVER_ID = new ResourceLocation("streamsreflowing", "river_carver");
   public static final Feature<NoneFeatureConfiguration> RIVER_CARVER = new RiverCarverFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, new ResourceLocation("streamsreflowing", "river")
   );
   public static final ResourceKey<PlacedFeature> RIVER_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("streamsreflowing", "river"));
   public static final ResourceLocation BANK_ROCK_ID = new ResourceLocation("streamsreflowing", "bank_rock");
   public static final Feature<BankRockFeature.Config> BANK_ROCK = new BankRockFeature(BankRockFeature.Config.CODEC);
   public static final ResourceLocation BANK_TEMPLATE_ID = new ResourceLocation("streamsreflowing", "bank_template");
   public static final Feature<BankTemplateFeature.Config> BANK_TEMPLATE = new BankTemplateFeature(BankTemplateFeature.Config.CODEC);
   public static final ResourceLocation FALLEN_LOG_ID = new ResourceLocation("streamsreflowing", "fallen_log");
   public static final Feature<BankFallenLogFeature.Config> FALLEN_LOG = new BankFallenLogFeature(BankFallenLogFeature.Config.CODEC);
   public static final ResourceLocation STREAM_VEGETATION_ID = new ResourceLocation("streamsreflowing", "stream_vegetation");
   public static final Feature<NoneFeatureConfiguration> STREAM_VEGETATION = new StreamVegetationFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> STREAM_VEGETATION_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, new ResourceLocation("streamsreflowing", "stream_vegetation")
   );
   public static final ResourceKey<PlacedFeature> STREAM_VEGETATION_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, new ResourceLocation("streamsreflowing", "stream_vegetation")
   );
   public static final TagKey<PlacedFeature> BIOME_BOOST_EXCLUDED = TagKey.create(
      Registries.PLACED_FEATURE, new ResourceLocation("streamsreflowing", "biome_boost_excluded")
   );
   public static final TagKey<Biome> NO_BANK_GRASS = TagKey.create(Registries.BIOME, new ResourceLocation("streamsreflowing", "no_bank_grass"));
   public static final TagKey<Block> UNDERWATER_VEGETATION_FLOOR = TagKey.create(
      Registries.BLOCK, new ResourceLocation("streamsreflowing", "underwater_vegetation_floor")
   );
   public static final ResourceLocation SNOW_CLEANUP_ID = new ResourceLocation("streamsreflowing", "snow_cleanup");
   public static final Feature<NoneFeatureConfiguration> SNOW_CLEANUP = new StreamSnowCleanupFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> SNOW_CLEANUP_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, new ResourceLocation("streamsreflowing", "snow_cleanup")
   );
   public static final ResourceKey<PlacedFeature> SNOW_CLEANUP_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, new ResourceLocation("streamsreflowing", "snow_cleanup")
   );
   public static final ResourceLocation RIVER_FLOW_ID = new ResourceLocation("streamsreflowing", "river_flow");
   public static final Feature<NoneFeatureConfiguration> RIVER_FLOW = new StreamRiverFlowFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_FLOW_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, new ResourceLocation("streamsreflowing", "river_flow")
   );
   public static final ResourceKey<PlacedFeature> RIVER_FLOW_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, new ResourceLocation("streamsreflowing", "river_flow")
   );
   public static final ResourceLocation WATER_SKIN_ID = new ResourceLocation("streamsreflowing", "water_skin");
   public static final Feature<NoneFeatureConfiguration> WATER_SKIN = new WaterSkinFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> WATER_SKIN_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, new ResourceLocation("streamsreflowing", "water_skin")
   );
   public static final ResourceKey<PlacedFeature> WATER_SKIN_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, new ResourceLocation("streamsreflowing", "water_skin")
   );
   private static final Map<ServerLevel, RiverEngine> a = Collections.synchronizedMap(new WeakHashMap<>());
   private static final double b = 6.0;
   private static final double c = 0.8;
   private static final int d = Integer.getInteger("streamsreflowing.samplecache", 65536);
   private static volatile RiverEngine e;
   private static volatile RegistryAccess f;
   private static volatile ServerLevel g;
   private static final double h = 0.390625;
   private static final int i = 200;
   private static final int j = 800;
   private static final int k = 768;
   private static final int l = 2;
   private static final int m = 80;
   private static final int n = 16;
   private static final int o = 6;
   private static final String p = "fastcal-v10.txt";
   private static final double q = 0.95;
   private static final double r = 6.0;
   private static final int s = 20;
   private static final boolean t = Boolean.getBoolean("streamsreflowing.caldiag");
   private static final int u = 40;
   private static final int v = 2000;
   private static final int w = 300;
   private static final int x = 384;
   private static final double y = 0.7;
   private static final double z = 0.5;
   private static final Class<?> A;
   private static final Method B;
   private static final Method C;
   private static final Object D;
   private static final Object E;
   private static final double F = 1.1;
   private static final int G = 120;
   private static final int H = 1800;
   private static final int I = 12;
   public static final int HEIGHTS_GAME = 0;
   public static final int HEIGHTS_FORM = 1;
   public static final int HEIGHTS_FORM_HANDBACK = 2;
   private static final Map<RiverEngine, StreamsWorldgen.EngineTerrain> J;
   private static final Map<String, Integer> K;
   private static final ConcurrentHashMap<Object, Integer> L;
   private static final long M = 180L;
   private static final int N;
   private static final int O = 6;
   private static final TagKey<Structure> P;
   private static volatile FootprintVeto Q;
   public static final ThreadLocal<Boolean> STRUCTURE_TRIAL;
   private static final ConcurrentHashMap<PlateKey, Long> R;
   private static final long S = 600000000000L;
   private static volatile Boolean T;
   private static final long U = 45L;
   private static final long V;
   private static final long W = 5L;
   private static final long X = 15L;
   private static final AtomicInteger Y;
   private static final AtomicInteger Z;

   private StreamsWorldgen() {
   }

   private static boolean a(TerrainSampler var0, TerrainSampler var1, BiomeSource var2, Sampler var3, int var4) {
      long var5 = 0L;
      long var7 = 0L;
      int var9 = 0;

      for (int var10 = 0; var10 < 200 && var9 < 16; var10++) {
         int var11 = 768 * (var10 * 5 % 29 - 14);
         int var12 = 768 * (var10 * 7 % 23 - 11);
         if (!var2.getNoiseBiome(QuartPos.fromBlock(var11), var4, QuartPos.fromBlock(var12), var3).is(BiomeTags.IS_OCEAN)) {
            long var13 = System.nanoTime();
            var0.height(var11, var12);
            long var15 = System.nanoTime();
            var1.height(var11, var12);
            var5 += var15 - var13;
            var7 += System.nanoTime() - var15;
            var9++;
         }
      }

      return var7 > 0L && var9 >= 8 && (double)var5 / var7 > 0.7;
   }

   private static double a(DensityFunction var0, int var1, int var2, int var3, int var4, int var5) {
      if (var0.compute(new SinglePointContext(var1, var4, var2)) > 0.390625) {
         return var4 + 1;
      }

      int var6 = var3;
      int var7 = var4;

      while (var7 - var6 > 1) {
         int var8 = var6 + var7 >>> 1;
         if (var0.compute(new SinglePointContext(var1, var8, var2)) > 0.390625) {
            var6 = var8;
         } else {
            var7 = var8;
         }
      }

      int var9 = Math.min(var4, var7 + var5);
      return var0.compute(new SinglePointContext(var1, var9, var2)) > 0.390625 ? b(var0, var1, var2, var3, var4, var5) : var6 + 1;
   }

   private static DensityFunction a(DensityFunction var0) {
      if (A == null) {
         return var0;
      }

      boolean[] var1 = new boolean[]{false};

      try {
         DensityFunction var2 = var0.mapAll(var1x -> {
            if (A.isInstance(var1x)) {
               try {
                  Object var2x = B.invoke(var1x);
                  if (var2x == D || var2x == E) {
                     var1[0] = true;
                     return new StreamsWorldgen.ColumnCachedNode((DensityFunction)C.invoke(var1x));
                  }
               } catch (ReflectiveOperationException var3x) {
                  throw new IllegalStateException(var3x);
               }
            }

            return var1x;
         });
         return var1[0] ? var2 : var0;
      } catch (RuntimeException var3) {
         return var0;
      }
   }

   private static double b(DensityFunction var0, int var1, int var2, int var3, int var4, int var5) {
      int var6 = var4;

      while (var6 >= var3) {
         if (var0.compute(new SinglePointContext(var1, var6, var2)) > 0.390625) {
            int var7 = Math.min(var4, var6 + var5 - 1);

            for (int var8 = var7; var8 > var6; var8--) {
               if (var0.compute(new SinglePointContext(var1, var8, var2)) > 0.390625) {
                  return var8 + 1;
               }
            }

            return var6 + 1;
         }

         var6 -= var5;
      }

      return var3;
   }

   public static FormSampler formHeights(
      TerrainSampler var0, NoiseBasedChunkGenerator var1, RandomState var2, LevelHeightAccessor var3, RegistryAccess var4, long var5
   ) {
      NoiseSettings var7 = ((NoiseGeneratorSettings)var1.generatorSettings().value()).noiseSettings().clampToHeightAccessor(var3);
      int var8 = var7.minY();
      int var9 = var7.minY() + var7.height();
      int var10 = Math.max(1, var7.getCellHeight());
      FieldForm var11 = FieldFormBuilder.build(
         var4, var5, ((NoiseGeneratorSettings)var1.generatorSettings().value()).useLegacyRandomSource(), var2.router().finalDensity(), var8, var9, var10
      );
      return new FormSampler(var0, var11, var8, var9, var10, Math.max(1, var7.getCellWidth()), 0.0, var1.getSeaLevel());
   }

   private static TerrainSampler a(TerrainSampler var0, ChunkGenerator var1, RandomState var2, ServerLevel var3, long var4) {
      if (!Boolean.getBoolean("streamsreflowing.noformgrades") && var1 instanceof NoiseBasedChunkGenerator var6) {
         try {
            FormSampler var7 = formHeights(var0, var6, var2, var3, var3.registryAccess(), var4);
            RiverFlowField.BiomeClass var8 = RiverFlowFields.classifierFor(var3);
            Boolean var9 = TerrainModeStore.a(var3, var4);
            boolean var10 = var9 != null ? var9 : a(var0, var7, var2.router().finalDensity(), var8);
            if (var9 == null) {
               TerrainModeStore.a(var3, var4, var10);
            }

            if (!var10) {
               a(var0, var7, var8);
               return var0;
            }

            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
               a(var0, var7);
               Constants.LOG.info("[SR-PREP] grade sampling: accelerated path in use for {}", var3.dimension().location());
            }

            a(var7, var8);
            a(var0, var7, var8);
            Constants.LOG.info("Streams Reflowing: stream preparation optimised for {}.", var3.dimension().location());
            return var7;
         } catch (RuntimeException var11) {
            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
               Constants.LOG.info("[SR-PREP] IR grades declined (full stack):", var11);
            }

            return var0;
         }
      } else {
         if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            Constants.LOG
               .info(
                  "[SR-PREP] grade sampling not attempted: generator is {} (need a noise generator){}",
                  var1.getClass().getName(),
                  Boolean.getBoolean("streamsreflowing.noformgrades") ? ", and the kill switch is on" : ""
               );
         }

         return var0;
      }
   }

   private static void a(TerrainSampler var0, RiverFlowField.BiomeClass var1) {
      if (Boolean.getBoolean("streamsreflowing.prepdiag") && var1 != null) {
         try {
            byte var2 = 15;
            short var3 = 211;
            int var4 = 100000;
            long var5 = System.nanoTime();
            int var7 = 0;

            for (int var8 = 0; var8 < 15; var8++) {
               for (int var9 = 0; var9 < 15; var9++) {
                  var1.classify(100000 + var8 * 211, 100000 + var9 * 211);
                  var7++;
               }
            }

            var5 = System.nanoTime() - var5;
            long var20 = System.nanoTime();

            for (int var10 = 0; var10 < 15; var10++) {
               for (int var11 = 0; var11 < 15; var11++) {
                  var1.classify(100000 + var10 * 211, 100000 + var11 * 211);
               }
            }

            var20 = System.nanoTime() - var20;
            long var22 = System.nanoTime();

            for (int var12 = 0; var12 < 15; var12++) {
               for (int var13 = 0; var13 < 15; var13++) {
                  var0.height(100000 + var12 * 211, 100000 + var13 * 211);
               }
            }

            var22 = System.nanoTime() - var22;
            double var24 = (double)var5 / var7;
            double var14 = (double)var20 / var7;
            double var16 = (double)var22 / var7;
            Constants.LOG
               .info(
                  "[SR-PREP] cost per column: classify cold {}us, warm {}us; height {}us -> height is {}x a cold classify, {}x a warm one",
                  new Object[]{
                     a(var24),
                     a(var14),
                     a(var16),
                     String.format(Locale.ROOT, "%.0f", var24 > 0.0 ? var16 / var24 : 0.0),
                     String.format(Locale.ROOT, "%.0f", var14 > 0.0 ? var16 / var14 : 0.0)
                  }
               );
         } catch (RuntimeException var18) {
            Constants.LOG.info("[SR-PREP] cost probe failed:", var18);
         }
      }
   }

   private static String a(double var0) {
      return String.format(Locale.ROOT, "%.2f", var0 / 1000.0);
   }

   private static void a(TerrainSampler var0, TerrainSampler var1, RiverFlowField.BiomeClass var2) {
      int var3 = Integer.getInteger("streamsreflowing.iraudit", 0);
      if (var3 > 0) {
         int var4 = 0;
         int var5 = Math.max(3, (int)Math.round(Math.sqrt(var3 / 2.0)));
         int var6 = var5 / 2;
         int[] var7 = new int[4];
         int var8 = 0;
         int var9 = 0;
         double var10 = 0.0;
         double var12 = 0.0;
         StringBuilder var14 = new StringBuilder();
         long var15 = System.nanoTime();

         for (int var17 = 0; var17 < 2; var17++) {
            int var18 = var17 == 0 ? 211 : 512;

            for (int var19 = -var6; var19 <= var6; var19++) {
               for (int var20 = -var6; var20 <= var6; var20++) {
                  int var21 = var19 * var18;
                  int var22 = var20 * var18;
                  if (var2 != null && var2.classify(var21, var22) == 2) {
                     var4++;
                  } else {
                     double var23;
                     try {
                        var23 = var1.height(var21, var22) - var0.height(var21, var22);
                     } catch (RuntimeException var27) {
                        continue;
                     }

                     if (Double.isFinite(var23)) {
                        var9++;
                        var10 = Math.max(var10, var23);
                        var12 = Math.min(var12, var23);
                        double var25 = Math.abs(var23);
                        var7[var25 == 0.0 ? 0 : (var25 <= 3.0 ? 1 : (var25 <= 6.0 ? 2 : 3))]++;
                        if (!HeightAccuracy.inWindow(var23)) {
                           var8++;
                           if (var14.length() < 700) {
                              var14.append(String.format(Locale.ROOT, "%n    (%d,%d) d=%.1f", var21, var22, var23));
                           }
                        }
                     }
                  }
               }
            }
         }

         Constants.LOG
            .info(
               "[SR-AUDIT] {} land columns in {}s ({} ocean skipped): {} outside the window (+{}/-{}), highest {}, lowest {} | exact {}, 1-3 {}, 4-6 {}, 7+ {}{}",
               new Object[]{
                  var9,
                  String.format(Locale.ROOT, "%.1f", (System.nanoTime() - var15) / 1.0E9),
                  var4,
                  var8,
                  3.0,
                  6.0,
                  var10,
                  var12,
                  var7[0],
                  var7[1],
                  var7[2],
                  var7[3],
                  var8 == 0 ? "" : var14
               }
            );
      }
   }

   private static boolean a(TerrainSampler var0, TerrainSampler var1, DensityFunction var2, RiverFlowField.BiomeClass var3) {
      FormSampler var4 = var1 instanceof FormSampler var5 ? var5 : null;
      if (var4 != null) {
         var4.setAquiferHandBack(false);
      }

      boolean var6 = a(var0, var1, var2, var4 != null, var3);
      if (!var6 && var4 != null) {
         Constants.LOG.info("[SR-PREP] arming the aquifer hand-back and re-judging");
         var4.setAquiferHandBack(true);
         var6 = a(var0, var1, var2, false, var3);
         if (!var6) {
            var4.setAquiferHandBack(false);
         }
      }

      return var6;
   }

   private static boolean a(TerrainSampler var0, TerrainSampler var1, DensityFunction var2, boolean var3, RiverFlowField.BiomeClass var4) {
      int var5 = 0;
      double var6 = 0.0;
      double var8 = 0.0;
      int var10 = 0;
      int var11 = 0;
      double var12 = 0.0;
      double var14 = 0.0;
      double var16 = 0.0;
      int var18 = 0;
      int var19 = 0;
      long var20 = 0L;
      long var22 = 0L;
      StringBuilder var24 = new StringBuilder();
      int var25 = 0;
      int var26 = 0;
      int var27 = 0;

      try {
         ArrayList var28 = new ArrayList(1800);

         for (int var29 = 0; var29 < 2; var29++) {
            int var30 = var29 == 0 ? 211 : 512;

            for (int var31 = -7; var31 <= 7; var31++) {
               for (int var32 = -7; var32 <= 7; var32++) {
                  var28.add(new int[]{var31 * var30, var32 * var30, var29});
               }
            }
         }

         var25 = var28.size();
         int var50 = 8;
         int var52 = 0;

         while (true) {
            if (var52 >= var28.size()) {
               if (var19 >= 120 || var27 >= 1800) {
                  break;
               }

               for (int var53 = -var50; var53 <= var50; var53++) {
                  for (int var56 = -var50; var56 <= var50; var56++) {
                     if (Math.max(Math.abs(var53), Math.abs(var56)) == var50) {
                        var28.add(new int[]{var53 * 512, var56 * 512, 1});
                     }
                  }
               }

               var50++;
               var26++;
            }

            int[] var54 = (int[])var28.get(var52);
            int var57 = var54[0];
            int var33 = var54[1];
            int var34 = var54[2];
            var27++;
            if (var4 != null && var4.classify(var57, var33) == 2) {
               var5++;
            } else {
               long var35 = System.nanoTime();
               double var37 = var1.height(var57, var33);
               long var39 = System.nanoTime();
               double var41 = var0.height(var57, var33);
               long var43 = System.nanoTime();
               if (var34 == 1) {
                  var20 += var39 - var35;
                  var22 += var43 - var39;
               }

               double var45 = var37 - var41;
               if (!Double.isFinite(var45)) {
                  return false;
               }

               var19++;
               var6 = Math.max(var6, var45);
               var8 = Math.min(var8, var45);
               if (!HeightAccuracy.inWindow(var45)) {
                  var18++;
                  if (var24.length() < 900) {
                     var24.append(String.format(Locale.ROOT, "%n    (%d,%d) ours=%.1f vanilla=%.1f d=%.1f", var57, var33, var37, var41, var45));
                  }
               }

               if (Math.abs(var45) > var16) {
                  var16 = Math.abs(var45);
                  var10 = var57;
                  var11 = var33;
                  var12 = var37;
                  var14 = var41;
               }
            }

            var52++;
         }
      } catch (RuntimeException var47) {
         Constants.LOG.warn("Streams Reflowing: terrain sampling failed while judging this world ({}); staying on the world's own terrain.", var47.toString());
         return false;
      }

      if (var26 > 0) {
         Constants.LOG
            .info(
               "[SR-PREP] the base lattices ({} columns) were mostly sea; walked {} ring(s) further out to {} columns for {} land columns ({} ocean, answered by the fast path)",
               new Object[]{var25, var26, var27, var19, var5}
            );
      }

      if (var19 < 12) {
         Constants.LOG
            .info(
               "[SR-PREP] only {} land columns in {} sampled ({} ocean, answered by the fast path) -- too few to judge on, staying on host terrain",
               new Object[]{var19, var27, var5}
            );
         return false;
      }

      if (var19 < 120) {
         Constants.LOG.info("[SR-PREP] judging on {} land columns (fewer than the preferred {}: the world is mostly sea out to the walk's reach)", var19, 120);
      }

      if (Boolean.getBoolean("streamsreflowing.prepdiag") && var5 > 0) {
         Constants.LOG
            .info("[SR-PREP] judged {} land columns; {} ocean columns skipped (the fast path answers those before this sampler is asked)", var19, var5);
      }

      boolean var49 = var18 == 0;
      if (!var49 && var3) {
         return false;
      }

      if (!var49 && !var3) {
         Constants.LOG.info("[SR-PREP] terrain accuracy: {} of {} sampled columns disagreed with this world's own terrain -- staying on it", var18, var19);
      }

      if (!var49 && Boolean.getBoolean("streamsreflowing.prepdiag")) {
         Constants.LOG
            .info(
               "[SR-PREP] IR grades REFUSED: {}; worst at ({},{}) ours={} vanilla={}",
               new Object[]{HeightAccuracy.describe(var18, var19, var6, var8), var10, var11, var12, var14}
            );
         Constants.LOG.info("[SR-PREP] offending columns:{}", var24);
         a(var1, var2, var10, var11, var12, var14);
      }

      if (!var49) {
         return false;
      }

      double var51 = var20 > 0L ? (double)var22 / var20 : 0.0;
      if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
         long var55 = FormSampler.handedBack.sum();
         long var58 = FormSampler.keptIr.sum();
         Constants.LOG
            .info(
               "[SR-PREP] aquifer hand-back {} of {} sampled columns ({}%)",
               new Object[]{var55, var55 + var58, String.format(Locale.ROOT, "%.1f", var55 + var58 == 0L ? 0.0 : 100.0 * var55 / (var55 + var58))}
            );
      }

      if (var51 < 1.1) {
         Constants.LOG.info("[SR-PREP] grade sampling: host terrain is already fast here ({}x), staying on it", String.format(Locale.ROOT, "%.2f", var51));
         return false;
      }

      if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
         Constants.LOG.info("[SR-PREP] grade speed: {}x over host terrain on the timed lattice", String.format(Locale.ROOT, "%.2f", var51));
      }

      return true;
   }

   private static void a(TerrainSampler var0, DensityFunction var1, int var2, int var3, double var4, double var6) {
      if (var0 instanceof FormSampler var8 && var1 != null) {
         try {
            Constants.LOG
               .info(
                  "[SR-PREP] walkCheck ({},{}) ours={} vanilla={} | both={} pinOnly={} interpOnly={} neither={}",
                  new Object[]{
                     var2,
                     var3,
                     var4,
                     var6,
                     var8.heightWithSemantics(var2, var3, true, true),
                     var8.heightWithSemantics(var2, var3, true, false),
                     var8.heightWithSemantics(var2, var3, false, true),
                     var8.heightWithSemantics(var2, var3, false, false)
                  }
               );
            int var9 = (int)Math.min(var4, var6) - 2;
            int var10 = (int)Math.max(var4, var6) + 2;
            double var11 = 0.0;
            int var13 = Integer.MIN_VALUE;
            StringBuilder var14 = new StringBuilder();

            for (int var15 = var10; var15 >= var9; var15--) {
               double var16 = var8.density(var2, var15, var3);
               double var18 = var1.compute(new SinglePointContext(var2, var15, var3));
               double var20 = Math.abs(var16 - var18);
               if (var20 > var11) {
                  var11 = var20;
                  var13 = var15;
               }

               var14.append(
                  String.format(
                     Locale.ROOT,
                     "%n    y=%d ours=%+.6f real=%+.6f d=%.3e%s",
                     var15,
                     var16,
                     var18,
                     var20,
                     var18 > var8.solidThreshold() ? "  <- real says SOLID" : ""
                  )
               );
            }

            Constants.LOG
               .info(
                  "[SR-PREP] walkCheck density band [{}..{}] worst d={} at y={} -> {}{}",
                  new Object[]{
                     var9,
                     var10,
                     String.format(Locale.ROOT, "%.3e", var11),
                     var13,
                     var11 < 1.0E-9
                        ? "densities AGREE, so the disagreement is in the column WALK"
                        : "density DIVERGES, so the disagreement is in the TRANSLATION",
                     var14
                  }
               );
         } catch (RuntimeException var22) {
            Constants.LOG.info("[SR-PREP] walkCheck failed:", var22);
         }
      }
   }

   private static void a(TerrainSampler var0, TerrainSampler var1) {
      for (int var2 = -2; var2 <= 2; var2++) {
         for (int var3 = -2; var3 <= 2; var3++) {
            var0.height(var2 * 512, var3 * 512);
            var1.height(var2 * 512, var3 * 512);
         }
      }

      long var29 = 0L;
      long var4 = 0L;
      double var6 = 0.0;
      int var8 = 0;
      int var9 = 0;
      double var10 = 0.0;
      double var12 = 0.0;
      int var14 = 0;

      for (int var15 = -7; var15 <= 7; var15++) {
         for (int var16 = -7; var16 <= 7; var16++) {
            int var17 = var15 * 211;
            int var18 = var16 * 211;
            long var19 = System.nanoTime();
            double var21 = var0.height(var17, var18);
            long var23 = System.nanoTime();
            double var25 = var1.height(var17, var18);
            long var27 = System.nanoTime();
            var29 += var23 - var19;
            var4 += var27 - var23;
            if (Math.abs(var21 - var25) > var6) {
               var6 = Math.abs(var21 - var25);
               var8 = var17;
               var9 = var18;
               var10 = var25;
               var12 = var21;
            }

            var14++;
         }
      }

      Constants.LOG
         .info(
            "[SR-PREP] IR-grades benchmark, {} columns: vanilla {} ms, FormEval {} ms ({}x faster), maxDiff={} blocks at ({},{}) ours={} real={}",
            new Object[]{
               var14,
               String.format(Locale.ROOT, "%.1f", var29 / 1000000.0),
               String.format(Locale.ROOT, "%.1f", var4 / 1000000.0),
               String.format(Locale.ROOT, "%.2f", (double)var29 / Math.max(1L, var4)),
               var6,
               var8,
               var9,
               var10,
               var12
            }
         );
   }

   private static StreamsWorldgen.FastCal a(
      ChunkGenerator var0, RandomState var1, BiomeSource var2, Sampler var3, int var4, TerrainSampler var5, ServerLevel var6
   ) {
      int var7 = Boolean.getBoolean("streamsreflowing.exact") ? 2 : (Services.PLATFORM.fastTerrainEstimate() ? Services.PLATFORM.terrainSamplingMode() : 2);
      if (var7 < 2 && var0 instanceof NoiseBasedChunkGenerator var8) {
         double var9 = Services.PLATFORM.gradeWindowAbove();
         double var11 = Services.PLATFORM.gradeWindowBelow();
         TerrainSampler var13 = a(var8, var1);
         if (Boolean.getBoolean("streamsreflowing.calanalyze")) {
            b(var13, var5, var2, var3, var4);
         }

         return a(var13, var0, var2, var3, var4, var5, var6, var7, var9, var11);
      } else {
         return new StreamsWorldgen.FastCal(var5, null, false, 0, null, null);
      }
   }

   static TerrainSampler a(NoiseBasedChunkGenerator var0, RandomState var1) {
      NoiseSettings var2 = ((NoiseGeneratorSettings)var0.generatorSettings().value()).noiseSettings();
      DensityFunction var3 = var1.router().initialDensityWithoutJaggedness();
      int var4 = var2.minY();
      int var5 = var2.minY() + var2.height();
      int var6 = Math.max(1, var2.getCellHeight());
      DensityFunction var7 = a(var3);
      ThreadLocal var8 = var7 == var3 ? null : ThreadLocal.withInitial(() -> a(var3));
      return var8 == null
         ? (var4x, var5x) -> a(var3, var4x, var5x, var4, var5, var6)
         : (var4x, var5x) -> a((DensityFunction)var8.get(), var4x, var5x, var4, var5, var6);
   }

   private static StreamsWorldgen.FastCal a(
      TerrainSampler var0,
      ChunkGenerator var1,
      BiomeSource var2,
      Sampler var3,
      int var4,
      TerrainSampler var5,
      ServerLevel var6,
      int var7,
      double var8,
      double var10
   ) {
      int var12 = Integer.getInteger("streamsreflowing.fastcaldata", 0);
      if (var12 > 0) {
         try (BufferedWriter var13 = Files.newBufferedWriter(Path.of("streamsreflowing-fastcal-data.csv"))) {
            var13.write("x,z,est,real\n");

            for (int var14 = -var12; var14 <= var12; var14 += 64) {
               for (int var15 = -var12; var15 <= var12; var15 += 64) {
                  if (!var2.getNoiseBiome(QuartPos.fromBlock(var15), var4, QuartPos.fromBlock(var14), var3).is(BiomeTags.IS_OCEAN)) {
                     var13.write(var15 + "," + var14 + "," + var0.height(var15, var14) + "," + var5.height(var15, var14) + "\n");
                  }
               }
            }

            Constants.LOG.info("[SR-FASTCALDATA] wrote streamsreflowing-fastcal-data.csv");
         } catch (IOException var45) {
            Constants.LOG.warn("fastcaldata dump failed", var45);
         }
      }

      boolean var46 = Boolean.getBoolean("streamsreflowing.fastcaldump");
      Path var47 = null;

      try {
         String var49 = var6.dimension().location().toString().replace(':', '_').replace('/', '_');
         Path var16 = var6.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var49);
         Files.createDirectories(var16);
         var47 = var16.resolve("fastcal-v10.txt");
         if (!var46) {
            StreamsWorldgen.FastCal var17 = a(var47, var5, var0, var7, var8, var10, var1.getSeaLevel());
            if (var17 != null) {
               return var17;
            }
         }
      } catch (IOException var42) {
         var47 = null;
      }

      if (!var46) {
         BakedBandTable var50 = BakedBandTable.lookup(Services.PLATFORM::isModLoaded, Services.PLATFORM::modVersion);
         if (var50 != null) {
            boolean[] var53 = var50.safeForWindow(var8, var10, 20);
            int var55 = 0;

            for (boolean var64 : var53) {
               if (var64) {
                  var55++;
               }
            }

            StreamsWorldgen.BandCorrection var57 = new StreamsWorldgen.BandCorrection(var50.centers(), var50.bias(), var53);
            boolean var60 = a(var50.centers(), var53, var1.getSeaLevel());
            if (!"HYBRID".equals(var50.verdict()) && var60) {
               if (t) {
                  Constants.LOG
                     .info(
                        "fast terrain estimate: ON (baked '{}' table, {} bands, {} grade-safe for [-{},+{}], mode {})",
                        new Object[]{var50.name(), var53.length, var55, var10, var8, var7}
                     );
               }

               return a(
                  var47,
                  var8,
                  var10,
                  "ON",
                  var57,
                  new StreamsWorldgen.FastCal(a(var0, var57, 1.0), a(var0, var57, 0.0), false, var7 == 0 ? 2 : 1, var57, var0, true)
               );
            }

            if (t) {
               Constants.LOG
                  .info(
                     "fast terrain estimate: HYBRID (baked '{}' table{})",
                     var50.name(),
                     "HYBRID".equals(var50.verdict()) ? "" : "; routing-critical bands unproven"
                  );
            }

            return a(var47, var8, var10, "HYBRID", var57, new StreamsWorldgen.FastCal(var5, a(var0, var57, 0.0), true, 0, null, null, false));
         }
      }

      if (Boolean.getBoolean("streamsreflowing.bakedonly")) {
         if (t) {
            Constants.LOG.info("fast terrain estimate: OFF (bakedonly -- no table for this combo, no per-world fit)");
         }

         return new StreamsWorldgen.FastCal(var5, null, false, 0, null, null);
      } else {
                ArrayList<double[]> var51 = new ArrayList<>();
         int var52 = var46 ? 289 : 800;
         int var54 = var46 ? 289 : Math.min(200, 384);
         ArrayList var18 = new ArrayList();

         for (int var19 = 0; var19 < var52 && var18.size() < var54; var19++) {
            int var20 = var46 ? 128 * (var19 % 17 - 8) : 768 * (var19 * 5 % 29 - 14);
            int var21 = var46 ? 128 * (var19 / 17 - 8) : 768 * (var19 * 7 % 23 - 11);
            if (!var2.getNoiseBiome(QuartPos.fromBlock(var20), var4, QuartPos.fromBlock(var21), var3).is(BiomeTags.IS_OCEAN)) {
               var18.add(new int[]{var20, var21});
            }
         }

         double[][] var58 = new double[var18.size()][];
         AtomicLong var61 = new AtomicLong();
         AtomicLong var63 = new AtomicLong();
         LowResGrid.parallelFor(var18.size(), var6x -> {
            int[] var7x = (int[])var18.get(var6x);
            long var8x = System.nanoTime();
            double var10x = var0.height(var7x[0], var7x[1]);
            long var12x = System.nanoTime();
            double var14x = var5.height(var7x[0], var7x[1]);
            var61.addAndGet(var12x - var8x);
            var63.addAndGet(System.nanoTime() - var12x);
            var58[var6x] = new double[]{var10x, var14x};
         });
         Collections.addAll(var51, var58);
         long var22 = var61.get();
         long var24 = var63.get();
         double var26 = var24 > 0L ? (double)var22 / var24 : 1.0;
         if (var26 > 0.7) {
            if (t) {
               Constants.LOG.debug("fast terrain estimate: OFF (estimate too costly: {}% of a real call)", Math.round(var26 * 100.0));
            }

            return a(var47, var8, var10, "OFF", null, new StreamsWorldgen.FastCal(var5, null, false, 0, null, null));
         } else {
            boolean var28 = var26 <= 0.5;
            if (var51.size() < 100) {
               if (!var28) {
                  if (t) {
                     Constants.LOG
                        .debug("fast terrain estimate: OFF (few land points; estimate {}% of a real call, too costly for hybrid)", Math.round(var26 * 100.0));
                  }

                  return a(var47, var8, var10, "OFF", null, new StreamsWorldgen.FastCal(var5, null, false, 0, null, null));
               } else {
                  if (t) {
                     Constants.LOG.debug("fast terrain estimate: HYBRID (only {} land calibration points found)", var51.size());
                  }

                  return a(var47, var8, var10, "HYBRID", null, new StreamsWorldgen.FastCal(var5, var0, true, 0, null, null));
               }
            } else {
                HashMap<Integer, Integer> var29 = new HashMap<>();

               for (double[] var31 : var51) {
                  var29.merge((int)Math.floor(var31[0] / 16.0) * 16, 1, Integer::sum);
               }

               int var65 = var51.size();
               int var66 = 0;
               int var32 = var46 ? 300 : Math.max(0, Math.min(300, 384 - var51.size()));
                ArrayList<int[]> var33 = new ArrayList<>();
                ArrayList<Double> var34 = new ArrayList<>();

               for (int var35 = 0; var35 < 2000 && var66 < var32; var35++) {
                  int var36 = 768 * (var35 * 11 % 31 - 15);
                  int var37 = 768 * (var35 * 13 % 27 - 13);
                  if (!var2.getNoiseBiome(QuartPos.fromBlock(var36), var4, QuartPos.fromBlock(var37), var3).is(BiomeTags.IS_OCEAN)) {
                     double var38 = var0.height(var36, var37);
                     Integer var40 = (Integer)var29.get((int)Math.floor(var38 / 16.0) * 16);
                     if (var40 != null && var40 < 40) {
                        var33.add(new int[]{var36, var37});
                        var34.add(var38);
                        var29.merge((int)Math.floor(var38 / 16.0) * 16, 1, Integer::sum);
                        var66++;
                     }
                  }
               }

               double[] var67 = new double[var33.size()];
               LowResGrid.parallelFor(var33.size(), var3x -> {
                  int[] var4x = (int[])var33.get(var3x);
                  var67[var3x] = var5.height(var4x[0], var4x[1]);
               });

               for (int var68 = 0; var68 < var33.size(); var68++) {
                  var51.add(new double[]{(Double)var34.get(var68), var67[var68]});
               }

               StreamsWorldgen.BandCorrection var69 = a(var51, var8, var10);
               int var70 = 0;

               for (int var71 = 0; var71 < var65; var71++) {
                  double[] var39 = (double[])var51.get(var71);
                  double var74 = (var69 != null ? var69.correct(var39[0]) : var39[0]) - var39[1];
                  if (Math.abs(var74) <= 2.0) {
                     var70++;
                  }
               }

               int var72 = var70 * 100 / var65;
               if (var46) {
                  Constants.LOG
                     .debug(
                        "fastcal dump: n={} bands={} corrected within +/-{}: {}%  costRatio={}%",
                        new Object[]{var51.size(), var69 == null ? 0 : var69.centers().length, 2, var72, Math.round(var26 * 100.0)}
                     );
               }

               if (var69 != null && var72 >= 80) {
                  if (!a(var69.centers(), var69.safe(), var1.getSeaLevel())) {
                     if (t) {
                        Constants.LOG.debug("fast terrain estimate: HYBRID (banded correction passed {}% but routing-critical bands unproven)", var72);
                     }

                     return a(var47, var8, var10, "HYBRID", var69, new StreamsWorldgen.FastCal(var5, a(var0, var69, 0.0), true, 0, null, null));
                  } else {
                     if (t) {
                        Constants.LOG
                           .debug(
                              "fast terrain estimate: ON (banded correction, {} bands, {} grade-safe, {}% of {} points within +/-{})",
                              new Object[]{var69.centers().length, var69.safeCount(), var72, var65, 2}
                           );
                     }

                     return a(
                        var47,
                        var8,
                        var10,
                        "ON",
                        var69,
                        new StreamsWorldgen.FastCal(a(var0, var69, 1.0), a(var0, var69, 0.0), false, var7 == 0 ? 2 : 1, var69, var0)
                     );
                  }
               } else if (!var28) {
                  if (t) {
                     Constants.LOG
                        .debug(
                           "fast terrain estimate: OFF (banded correction {}% within +/-{} AND estimate {}% of a real call, too costly for hybrid)",
                           new Object[]{var72, 2, Math.round(var26 * 100.0)}
                        );
                  }

                  return a(var47, var8, var10, "OFF", null, new StreamsWorldgen.FastCal(var5, null, false, 0, null, null));
               } else {
                  if (t) {
                     Constants.LOG
                        .debug(
                           "fast terrain estimate: HYBRID (banded correction {}% of {} points within +/-{}; estimate {}% of a real call)",
                           new Object[]{var72, var65, 2, Math.round(var26 * 100.0)}
                        );
                  }

                  TerrainSampler var73 = var69 == null ? var0 : a(var0, var69, 0.0);
                  return a(var47, var8, var10, "HYBRID", var69, new StreamsWorldgen.FastCal(var5, var73, true, 0, null, null));
               }
            }
         }
      }
   }

   private static TerrainSampler a(TerrainSampler var0, StreamsWorldgen.BandCorrection var1, double var2) {
      return (var4, var5) -> var1.correct(var0.height(var4, var5)) - var2;
   }

   private static StreamsWorldgen.BandCorrection a(List<double[]> var0, double var1, double var3) {
       TreeMap<Integer, List<double[]>> var5 = new TreeMap<>();

      for (int var6 = 0; var6 < var0.size(); var6 += 2) {
         double[] var7 = (double[])var0.get(var6);
         var5.computeIfAbsent((int)Math.floor(var7[0] / 16.0) * 16, var0x -> new ArrayList<>()).add(var7);
      }

       ArrayList<Double> var24 = new ArrayList<>();
       ArrayList<Double> var25 = new ArrayList<>();

       for (Entry<Integer, List<double[]>> var9 : var5.entrySet()) {
          if (var9.getValue().size() >= 6) {
             ArrayList<Double> var10 = new ArrayList<>();

             for (double[] var12 : var9.getValue()) {
               var10.add(var12[0] - var12[1]);
            }

            Collections.sort(var10);
            double var29 = (Double)var10.get(var10.size() / 2);
            double var13 = 0.0;

            for (double var15 = 0.0; var15 <= 2.0; var15++) {
               int var17 = 0;
               int var18 = 0;

               for (double var20 : var10) {
                  double var22 = var20 - var29 - var15 - 1.0;
                  if (!(var22 >= 6.0)) {
                     var17++;
                     if (var22 > var1 || var22 < -var3) {
                        var18++;
                     }
                  }
               }

               if (var17 > 0 && var18 <= var17 * 0.050000000000000044) {
                  var13 = var15;
                  break;
               }
            }

            var24.add(((Integer)var9.getKey()).intValue() + 8.0);
            var25.add(var29 + var13);
         }
      }

      if (var24.isEmpty()) {
         return null;
      }

      double[] var26 = var24.stream().mapToDouble(Double::doubleValue).toArray();
      double[] var27 = var25.stream().mapToDouble(Double::doubleValue).toArray();
      StreamsWorldgen.BandCorrection var28 = new StreamsWorldgen.BandCorrection(var26, var27, new boolean[var26.length]);
      int[] var30 = new int[var26.length];
      int[] var31 = new int[var26.length];

      for (int var32 = 1; var32 < var0.size(); var32 += 2) {
         double[] var14 = (double[])var0.get(var32);
         double var35 = Math.floor(var14[0] / 16.0) * 16.0 + 8.0;
         int var36 = Arrays.binarySearch(var26, var35);
         if (var36 >= 0) {
            double var37 = var28.correct(var14[0]) - 1.0 - var14[1];
            if (!(var37 >= 6.0)) {
               var30[var36]++;
               if (var37 > var1 || var37 < -var3) {
                  var31[var36]++;
               }
            }
         }
      }

      boolean[] var33 = new boolean[var26.length];

      for (int var34 = 0; var34 < var26.length; var34++) {
         var33[var34] = var30[var34] >= 20 && var31[var34] <= var30[var34] * 0.050000000000000044;
      }

      return new StreamsWorldgen.BandCorrection(var26, var27, var33);
   }

   private static boolean a(double[] var0, boolean[] var1, int var2) {
      for (int var3 = 0; var3 < var1.length; var3++) {
         if (var0[var3] >= var2 - 24 && var0[var3] <= var2 + 24 && !var1[var3]) {
            return false;
         }
      }

      return true;
   }

   private static StreamsWorldgen.FastCal a(Path var0, double var1, double var3, String var5, StreamsWorldgen.BandCorrection var6, StreamsWorldgen.FastCal var7) {
      if (var0 != null) {
         try {
            StringBuilder var8 = new StringBuilder(var5).append(' ').append(var1).append(' ').append(var3).append('\n');
            if (var6 != null) {
               for (int var9 = 0; var9 < var6.centers().length; var9++) {
                  var8.append(var6.centers()[var9]).append(',').append(var6.bias()[var9]).append(',').append(var6.safe()[var9] ? 1 : 0).append('\n');
               }
            }

            Files.writeString(var0, var8.toString());
         } catch (IOException var10) {
            if (t) {
               Constants.LOG.debug("fast terrain calibration save failed: {}", var10.toString());
            }
         }
      }

      return var7;
   }

   private static StreamsWorldgen.FastCal a(Path var0, TerrainSampler var1, TerrainSampler var2, int var3, double var4, double var6, int var8) {
      try {
         if (!Files.isRegularFile(var0)) {
            return null;
         }

         List var9 = Files.readAllLines(var0);
         if (var9.isEmpty()) {
            return null;
         }

         String[] var10 = ((String)var9.get(0)).trim().split(" ");
         String var11 = var10[0];
         if (var10.length >= 3 && !(Math.abs(Double.parseDouble(var10[1]) - var4) > 1.0E-9) && !(Math.abs(Double.parseDouble(var10[2]) - var6) > 1.0E-9)) {
             ArrayList<Double> var12 = new ArrayList<>();
             ArrayList<Double> var13 = new ArrayList<>();
             ArrayList<Boolean> var14 = new ArrayList<>();

            for (int var15 = 1; var15 < var9.size(); var15++) {
               String var16 = ((String)var9.get(var15)).trim();
               if (!var16.isEmpty()) {
                  String[] var17 = var16.split(",");
                  var12.add(Double.parseDouble(var17[0]));
                  var13.add(Double.parseDouble(var17[1]));
                  var14.add("1".equals(var17[2]));
               }
            }

            StreamsWorldgen.BandCorrection var19;
            if (var12.isEmpty()) {
               var19 = null;
            } else {
               boolean[] var20 = new boolean[var14.size()];

               for (int var22 = 0; var22 < var20.length; var22++) {
                  var20[var22] = (Boolean)var14.get(var22);
               }

               var19 = new StreamsWorldgen.BandCorrection(
                  var12.stream().mapToDouble(Double::doubleValue).toArray(), var13.stream().mapToDouble(Double::doubleValue).toArray(), var20
               );
            }

            switch (var11) {
               case "OFF":
                  if (t) {
                     Constants.LOG.debug("fast terrain estimate: OFF (persisted calibration)");
                  }

                  return new StreamsWorldgen.FastCal(var1, null, false, 0, null, null);
               case "HYBRID":
                  if (t) {
                     Constants.LOG.debug("fast terrain estimate: HYBRID (persisted calibration)");
                  }

                  return new StreamsWorldgen.FastCal(var1, var19 == null ? var2 : a(var2, var19, 0.0), true, 0, null, null);
               case "ON":
                  if (var19 == null) {
                     return null;
                  } else {
                     if (!a(var19.centers(), var19.safe(), var8)) {
                        if (t) {
                           Constants.LOG.debug("fast terrain estimate: HYBRID (persisted ON calibration; routing-critical bands unproven)");
                        }

                        return new StreamsWorldgen.FastCal(var1, a(var2, var19, 0.0), true, 0, null, null);
                     }

                     if (t) {
                        Constants.LOG
                           .debug(
                              "fast terrain estimate: ON (persisted calibration, {} bands, {} grade-safe, mode {})",
                              new Object[]{var19.centers().length, var19.safeCount(), var3}
                           );
                     }

                     return new StreamsWorldgen.FastCal(a(var2, var19, 1.0), a(var2, var19, 0.0), false, var3 == 0 ? 2 : 1, var19, var2);
                  }
               default:
                  return null;
            }
         } else {
            return null;
         }
      } catch (RuntimeException | IOException var18) {
         return null;
      }
   }

   public static RiverEngine engineIfPresent(ServerLevel var0) {
      synchronized (a) {
         return a.get(var0);
      }
   }

   public static void parseOutletBiomes(String var0, List<TagKey<Biome>> var1, Set<ResourceLocation> var2) {
      for (String var6 : var0.split(",")) {
         String var7 = var6.trim();
         if (!var7.isEmpty()) {
            boolean var8 = var7.charAt(0) == '#';
            ResourceLocation var9 = ResourceLocation.tryParse(var8 ? var7.substring(1) : var7);
            if (var9 == null) {
               Constants.LOG.warn("Streams Reflowing: ignoring unreadable outlet biome entry '{}'.", var7);
            } else if (var8) {
               var1.add(TagKey.create(Registries.BIOME, var9));
            } else {
               var2.add(var9);
            }
         }
      }
   }

   static DensityFunction a(ChunkGenerator var0, RandomState var1) {
      DensityFunction var2 = null;
      if (var0 instanceof NoiseBasedChunkGenerator) {
         var2 = var1.router().initialDensityWithoutJaggedness();
      }

      return var2;
   }

   public static boolean plainTerrain(RiverEngine var0) {
      StreamsWorldgen.EngineTerrain var1 = J.get(var0);
      return var1 != null && (var1.fast() == null || var0.estCostVerdict() != 0);
   }

   public static int heightsOf(RiverEngine var0) {
      StreamsWorldgen.EngineTerrain var1 = J.get(var0);
      return var1 == null ? -1 : var1.heights();
   }

   public static StreamsWorldgen.FastSpec fastOf(RiverEngine var0) {
      StreamsWorldgen.EngineTerrain var1 = J.get(var0);
      return var1 == null ? null : var1.fast();
   }

   public static ServerLevel levelOf(RiverEngine var0) {
      synchronized (a) {
         for (Entry var3 : a.entrySet()) {
            if (var3.getValue() == var0) {
               return (ServerLevel)var3.getKey();
            }
         }

         return null;
      }
   }

   public static RiverEngine detachedEngine(long var0, StreamsConfig var2, StreamsWorldgen.SamplerParts var3, WetnessSampler var4, NetworkStore var5) {
      CachingTerrainSampler var6 = new CachingTerrainSampler(worldSampler(var3), d, -1000.0);
      RiverEngine var7 = new RiverEngine(var0, var2, var6, var4);
      var7.setNetworkStore(var5);
      return var7;
   }

   public static TerrainSampler worldSampler(StreamsWorldgen.SamplerParts var0) {
      ChunkGenerator var1 = var0.generator();
      LevelHeightAccessor var2 = var0.level();
      RandomState var3 = var0.randomState();
      final BiomeSource var4 = var0.biomeSource();
      final Sampler var5 = var0.climateSampler();
      final TerrainSampler var6 = var0.vanillaGround();
      final TerrainSampler var7 = var0.realHeight();
      final TerrainSampler var8 = var0.landHeight();
      final TerrainSampler var9 = var0.bulkLand();
      final TerrainSampler var10 = var0.bulkGround();
      final TerrainSampler var11 = var0.surfaceReal();
      final TerrainSampler var12 = var0.surface();
      StreamsWorldgen.FastCal var13 = (StreamsWorldgen.FastCal)var0.fastCal();
      final RTFBridge.Handle var14 = var0.rtf();
      final RiverFlowField.BiomeClass var15 = var0.oceanClass();
      final boolean var16 = var0.outletGate();
       final List<TagKey<Biome>> var17 = var0.outletTags();
       final Set<ResourceLocation> var18 = var0.outletIds();
      final boolean var19 = var0.fastOceanSkip();
      final DensityFunction var20 = var0.solidity();
      final int var21 = var0.seaLevel();
      final TerrainSampler var22 = var13.estimator();
      final boolean var23 = var13.hybridGrid();
      final int var24 = var13.gradePolicy();
      final StreamsWorldgen.BandCorrection var25 = var13.corr();
      final TerrainSampler var26 = var13.rawEst();
      final boolean var27 = var13.baked();
      return new TerrainSampler() {
         @Override
         public boolean outletBiomeOk(int var1, int var2x, int var3x) {
            if (!var16) {
               return true;
            }

             Holder<Biome> var4x = var4.getNoiseBiome(QuartPos.fromBlock(var1), QuartPos.fromBlock(var2x), QuartPos.fromBlock(var3x), var5);

            for (TagKey var6x : var17) {
               if (var4x.is(var6x)) {
                  return true;
               }
            }

            return !var18.isEmpty() && var4x.unwrapKey().map(var1x -> var18.contains(var1x.location())).orElse(false);
         }

         @Override
         public int waterBiomeClass(int var1, int var2x) {
            return var14 != null && var14.river(var1, var2x) ? 1 : var15.classify(var1, var2x);
         }

         @Override
         public int waterBiomeClassAt(int var1, int var2x, int var3x) {
            if (var14 != null) {
               return this.waterBiomeClass(var1, var3x);
            } else {
                Holder<Biome> var4x = var4.getNoiseBiome(QuartPos.fromBlock(var1), QuartPos.fromBlock(var2x), QuartPos.fromBlock(var3x), var5);
               if (var4x.is(BiomeTags.IS_OCEAN)) {
                  return 2;
               } else {
                  return !var4x.is(BiomeTags.IS_RIVER) && !var4x.is(Biomes.RIVER) && !var4x.is(Biomes.FROZEN_RIVER) ? 0 : 1;
               }
            }
         }

         @Override
         public double riverSurface(int var1, int var2x) {
            return var14 != null ? var14.riverSurface(var1, var2x) : Double.NaN;
         }

         @Override
         public double height(int var1, int var2x) {
            if (var19 && var15.classify(var1, var2x) == 2) {
               return -1000.0;
            } else {
               return var14 != null ? var14.height(var1, var2x) : var8.height(var1, var2x);
            }
         }

         @Override
         public boolean batchCapable() {
            return var9.batchCapable();
         }

         @Override
         public void heightsBatch(int[] var1, int[] var2x, double[] var3x, int var4x) {
            int[] var5x = new int[var4x];
            int[] var6x = new int[var4x];
            int[] var7x = new int[var4x];
            int var8x = 0;

            for (int var9x = 0; var9x < var4x; var9x++) {
               if (var19 && var15.classify(var1[var9x], var2x[var9x]) == 2) {
                  var3x[var9x] = -1000.0;
               } else {
                  var5x[var8x] = var1[var9x];
                  var6x[var8x] = var2x[var9x];
                  var7x[var8x] = var9x;
                  var8x++;
               }
            }

            if (var8x != 0) {
               double[] var11x = new double[var8x];
               if (var9.batchCapable()) {
                  var9.heightsBatch(var5x, var6x, var11x, var8x);
               } else {
                  for (int var10x = 0; var10x < var8x; var10x++) {
                     var11x[var10x] = var8.height(var5x[var10x], var6x[var10x]);
                  }
               }

               for (int var12x = 0; var12x < var8x; var12x++) {
                  var3x[var7x[var12x]] = var11x[var12x];
               }
            }
         }

         @Override
         public double groundHeight(int var1, int var2x) {
            if (var24 == 2) {
               return this.height(var1, var2x);
            }

            if (var24 == 1) {
               double var3x = var26.height(var1, var2x);
               if (var25.gradeSafe(var3x)) {
                  return var25.correct(var3x) - 1.0;
               }
            }

            return var19 && var15.classify(var1, var2x) == 2 ? -1000.0 : var7.height(var1, var2x);
         }

         @Override
         public void groundHeightsBatch(int[] var1, int[] var2x, double[] var3x, int var4x) {
            if (var24 == 0 && var10.batchCapable()) {
               int[] var11x = new int[var4x];
               int[] var6x = new int[var4x];
               int[] var7x = new int[var4x];
               int var8x = 0;

               for (int var9x = 0; var9x < var4x; var9x++) {
                  if (var19 && var15.classify(var1[var9x], var2x[var9x]) == 2) {
                     var3x[var9x] = -1000.0;
                  } else {
                     var11x[var8x] = var1[var9x];
                     var6x[var8x] = var2x[var9x];
                     var7x[var8x] = var9x;
                     var8x++;
                  }
               }

               if (var8x != 0) {
                  double[] var12x = new double[var8x];
                  var10.heightsBatch(var11x, var6x, var12x, var8x);

                  for (int var10x = 0; var10x < var8x; var10x++) {
                     var3x[var7x[var10x]] = var12x[var10x];
                  }
               }
            } else {
               for (int var5x = 0; var5x < var4x; var5x++) {
                  var3x[var5x] = this.groundHeight(var1[var5x], var2x[var5x]);
               }
            }
         }

         @Override
         public double groundHeightExact(int var1, int var2x) {
            return var19 && var15.classify(var1, var2x) == 2 ? -1000.0 : var7.height(var1, var2x);
         }

         @Override
         public boolean bakedCalibration() {
            return var27;
         }

         @Override
         public boolean hasEstimate() {
            return var14 != null || var22 != null;
         }

         @Override
         public boolean useHybridGridFill() {
            return var23;
         }

         @Override
         public double estimate(int var1, int var2x) {
            if (var22 != null) {
               return var22.height(var1, var2x);
            } else {
               return var14 != null ? var14.height(var1, var2x) : this.height(var1, var2x);
            }
         }

         @Override
         public double groundHeightReal(int var1, int var2x) {
            return var6.height(var1, var2x);
         }

         @Override
         public double surfaceHeightReal(int var1, int var2x) {
            return var11.height(var1, var2x);
         }

         @Override
         public double surfaceHeight(int var1, int var2x) {
            return var15.classify(var1, var2x) == 2 ? var21 : var12.height(var1, var2x);
         }

         @Override
         public boolean openAt(int var1, int var2x, int var3x) {
            return var20 == null ? var2x > this.height(var1, var3x) : var20.compute(new SinglePointContext(var1, var2x, var3x)) <= 0.390625;
         }
      };
   }

   public static RiverEngine engine(WorldGenLevel var0) {
      final ServerLevel var1 = var0.getLevel();
      DynamicTreesCompat.reportOnce();
      synchronized (a) {
         RiverEngine var3 = a.get(var1);
         if (var3 != null) {
            if (var1.dimension().equals(Level.OVERWORLD)) {
               e = var3;
               f = var1.registryAccess();
               g = var1;
            }

            return var3;
         } else {
            final ChunkGenerator var4 = var1.getChunkSource().getGenerator();
            final RandomState var5 = var1.getChunkSource().randomState();
            long var6 = var0.getSeed();
            final BiomeSource var8 = var4.getBiomeSource();
            final Sampler var9 = var5.sampler();
            final ChunkHeightCache var10 = Boolean.getBoolean("streamsreflowing.nochunkheights") ? null : new ChunkHeightCache(var1);

            final class _HostRead {
               double a(int var1x, int var2, int var3) {
                  int var4x = ReadLattice.cell();
                  if (var4x > 1) {
                     var1x = ReadLattice.snap(var1x, var4x);
                     var2 = ReadLattice.snap(var2, var4x);
                  }

                  ReadLocality.miss(var1x, var2);
                  if (var10 != null) {
                     double var5x = var10.heightOrNaN(var1x, var2);
                     if (!Double.isNaN(var5x)) {
                        return var5x;
                     }
                  }

                  long var9 = SampleClock.now();
                  double var7 = var4.getBaseHeight(var1x, var2, Types.OCEAN_FLOOR_WG, var1, var5);
                  SampleClock.read(var3, var9);
                  if (var4x > 1 && var10 != null) {
                     var10.remember(var1x, var2, var7);
                  }

                  return var7;
               }

               double b(int var1x, int var2, int var3) {
                  int var4x = ReadLattice.cell();
                  if (var4x > 1) {
                     var1x = ReadLattice.snap(var1x, var4x);
                     var2 = ReadLattice.snap(var2, var4x);
                  }

                  long var5x = SampleClock.now();
                  double var7 = var4.getBaseHeight(var1x, var2, Types.WORLD_SURFACE_WG, var1, var5);
                  SampleClock.read(var3, var5x);
                  return var7;
               }
            }

            _HostRead var11 = new _HostRead();
            TerrainSampler var12 = (var1x, var2) -> var11.a(var1x, var2, 0);
            TerrainSampler var13 = a(var12, var4, var5, var1, var6);
            ArrayList var14 = new ArrayList();
            HashSet var15 = new HashSet();

            for (String var19 : Services.PLATFORM.outletBiomes().split(",")) {
               String var20 = var19.trim();
               if (!var20.isEmpty()) {
                  boolean var21 = var20.charAt(0) == '#';
                  ResourceLocation var22 = ResourceLocation.tryParse(var21 ? var20.substring(1) : var20);
                  if (var22 == null) {
                     Constants.LOG.warn("Streams Reflowing: ignoring unreadable outlet biome entry '{}'.", var20);
                  } else if (var21) {
                     var14.add(TagKey.create(Registries.BIOME, var22));
                  } else {
                     var15.add(var22);
                  }
               }
            }

            boolean var70 = !var14.isEmpty() || !var15.isEmpty();
            boolean var71 = Services.PLATFORM.fastOceanSkip();
            RTFBridge.Handle var72 = Boolean.getBoolean("streamsreflowing.nortfheights") ? null : RTFBridge.handleFor(var5);
            RiverFlowField.BiomeClass var73 = Boolean.getBoolean("streamsreflowing.noshareoceancache")
               ? (var3x, var4x) -> var8.getNoiseBiome(QuartPos.fromBlock(var3x), QuartPos.fromBlock(var4.getSeaLevel()), QuartPos.fromBlock(var4x), var9)
                     .is(BiomeTags.IS_OCEAN)
                  ? 2
                  : 0
               : RiverFlowFields.classifierFor(var1);
            int var74 = QuartPos.fromBlock(var4.getSeaLevel());
            int var75 = var4.getSeaLevel();
            if (var1.dimension().equals(Level.OVERWORLD) || StreamBiomeBridge.landProbe == null) {
               StreamBiomeBridge.landProbe = StreamBiomeBridge.memoised(
                  (var2, var3x, var4x) -> var8.getNoiseBiome(QuartPos.fromBlock(var2), QuartPos.fromBlock(var3x), QuartPos.fromBlock(var4x), var9)
               );
            }

            StreamsWorldgen.FastCal var76 = a(var4, var5, var8, var9, var74, var13, var1);
            StreamsWorldgen.FastCal var23 = var76.gradePolicy() < 1 && !Boolean.getBoolean("streamsreflowing.hybridgrid")
               ? new StreamsWorldgen.FastCal(var13, null, false, 0, null, null)
               : var76;
            Constants.LOG
               .info(
                  "Streams Reflowing: {} builds from y={} to y={}, sea level {}, terrain accuracy {}.",
                  new Object[]{var1.dimension().location(), var1.getMinBuildHeight(), var1.getMaxBuildHeight(), var4.getSeaLevel(), Services.PLATFORM.terrainAccuracyPreset()}
               );
            if (var23.gradePolicy() >= 1) {
               Constants.LOG.info("Streams Reflowing: fast terrain mode ENABLED for {}.", var1.dimension().location());
            } else if (var23.hybridGrid()) {
               Constants.LOG.info("Streams Reflowing: fast terrain mode PARTIALLY enabled for {}.", var1.dimension().location());
            } else {
               Constants.LOG.info("Streams Reflowing: fast terrain mode OFF for {}.", var1.dimension().location());
            }

            TerrainSampler var24 = var23.land();
            ChunkFillProbe.run(var1, var24);
            TerrainSampler var25 = var24;
            TerrainSampler var26 = var13;
            TerrainSampler var27 = var23.estimator();
            boolean var28 = var23.hybridGrid();
            int var29 = var23.gradePolicy();
            StreamsWorldgen.BandCorrection var30 = var23.corr();
            TerrainSampler var31 = var23.rawEst();
            boolean var32 = var23.baked();
            DensityFunction var33 = null;
            if (var4 instanceof NoiseBasedChunkGenerator) {
               var33 = var5.router().initialDensityWithoutJaggedness();
            }

            DensityFunction var34 = var33;
            TerrainSampler var35 = worldSampler(
               new StreamsWorldgen.SamplerParts(
                  var4,
                  var1,
                  var5,
                  var8,
                  var9,
                  (var1x, var2) -> var11.a(var1x, var2, 2),
                  var13,
                  var24,
                  var25,
                  var23,
                  var72,
                  var73,
                  var70,
                  var14,
                  var15,
                  var71,
                  var34,
                  var75,
                  var26,
                  (var1x, var2) -> var11.b(var1x, var2, 3),
                  (var1x, var2) -> var11.b(var1x, var2, 1)
               )
            );
            final CachingTerrainSampler var36 = new CachingTerrainSampler(var35, d, -1000.0);
            boolean var37 = true;

            try {
               var37 = StreamBiomeBlacklist.a(
                  var1.getServer()
                     .getWorldPath(LevelResource.ROOT)
                     .resolve("streamsreflowing")
                     .resolve(var1.dimension().location().toString().replace(':', '_').replace('/', '_'))
               );
            } catch (RuntimeException var68) {
               Constants.LOG.warn("Streams Reflowing: could not resolve this world's blacklist rule; using the new one", var68);
            }

            final boolean var38 = var37;
            WetnessSampler var39 = new WetnessSampler() {
               @Override
               public double wetness(int var1x, int var2, int var3x) {
                  return Services.PLATFORM.biomeWetness(var8.getNoiseBiome(QuartPos.fromBlock(var1x), QuartPos.fromBlock(var2), QuartPos.fromBlock(var3x), var9));
               }

               @Override
               public boolean barsAnything() {
                  if (!var38) {
                     return false;
                  }

                  for (Holder var2 : var8.possibleBiomes()) {
                     if (StreamBiomeBlacklist.isBlocked(var2)) {
                        return true;
                     }
                  }

                  return false;
               }

               @Override
               public boolean barred(int var1x, int var2, int var3x) {
                  return var38
                     && StreamBiomeBlacklist.isBlocked(var8.getNoiseBiome(QuartPos.fromBlock(var1x), QuartPos.fromBlock(var2), QuartPos.fromBlock(var3x), var9));
               }

               @Override
               public double cohesion(int var1x, int var2, int var3x) {
                  return RiverCarverFeature.bankCohesion(
                     var1, var8.getNoiseBiome(QuartPos.fromBlock(var1x), QuartPos.fromBlock(var2), QuartPos.fromBlock(var3x), var9)
                  );
               }
            };
            int var40 = Services.PLATFORM.gridSpacing();
            int var41 = Services.PLATFORM.minStreamLength() / var40;
            if (var41 == 0 && Services.PLATFORM.minStreamLength() > 0) {
               var41 = 1;
            }

            int var42 = Services.PLATFORM.plateScale();

            try {
               String var43 = var1.dimension().location().toString().replace(':', '_').replace('/', '_');
               var42 = WatershedPin.a(var1.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var43), var6, var42);
            } catch (RuntimeException var67) {
               Constants.LOG.warn("Streams Reflowing: could not resolve this world's watershed size; using the setting", var67);
            }

            boolean var77 = Services.PLATFORM.legacyShaping() || Boolean.getBoolean("streamsreflowing.legacyshaping");
            int var44 = var77 ? QualityPreset.legacyValleySnapRadius(Services.PLATFORM.terrainAccuracyPreset()) : Services.PLATFORM.valleySnapRadius();
            RiverEngine var45 = new RiverEngine(
               var6,
               StreamsConfig.defaults(
                     var40,
                     Services.PLATFORM.widthScale(),
                     var42,
                     Services.PLATFORM.terrainSampleSpacing(),
                     var44,
                     Services.PLATFORM.terrainFit(),
                     Services.PLATFORM.lakeSampleSpacing(),
                     Services.PLATFORM.legacyShaping()
                  )
                  .withAdaptiveGridRead(Services.PLATFORM.adaptiveGridRead() && !var77)
                  .withSeaLevel(var4.getSeaLevel())
                  .withMinRiverAccumulation(6.0)
                  .withWetnessMultiplier(Services.PLATFORM.wetnessMultiplier() * 0.8)
                  .withWetnessDryFloor(Services.PLATFORM.wetnessDryFloor())
                  .withLakeArea(Services.PLATFORM.generateLakes() ? Services.PLATFORM.lakeMinArea() : Double.POSITIVE_INFINITY, Services.PLATFORM.lakeMaxArea())
                  .withMinSeaChainNodes(var41)
                  .withMinLengthScope(Services.PLATFORM.minLengthScope())
                  .withOutletBiomes(Services.PLATFORM.outletBiomes())
                  .withConnect(Services.PLATFORM.connectNearbyWater(), Services.PLATFORM.connectRadius())
                  .withSinkRooted(Services.PLATFORM.sinkRooted())
                  .withGrowDensity(Services.PLATFORM.growDensity())
                  .withConfineToBanks(Services.PLATFORM.confineToBanks())
                  .withContainmentBerm(Services.PLATFORM.containmentBerm())
                  .withAppearance(
                     Services.PLATFORM.meanderStrength(), Services.PLATFORM.baseWidth(), Services.PLATFORM.maxWidth(), Services.PLATFORM.streamDepth()
                  )
                  .withBankNoise(Services.PLATFORM.bankNoise())
                  .withHydraulics(
                     new HydraulicGeometry(
                        Services.PLATFORM.widthFollowsSlope(),
                        Services.PLATFORM.widthFollowsClimate(),
                        Services.PLATFORM.widthVariation(),
                        Services.PLATFORM.depthFollowsTerrain(),
                        Services.PLATFORM.bendAsymmetry(),
                        Services.PLATFORM.channelBars(),
                        Services.PLATFORM.deltaMouths()
                     )
                  ),
               var36,
               var39
            );
            if (System.getProperty("streamsreflowing.benchcalls") != null) {
               RiverFlowField.BiomeClass var46 = RiverFlowFields.classifierFor(var1);
               TerrainSampler var47 = var76.estimator();
               Random var48 = new Random(20260810L);
               short var49 = 1000;
               int[] var50 = new int[var49];
               int[] var51 = new int[var49];
               double var52 = 0.0;

               for (int var54 = 1; var54 <= 2; var54++) {
                  for (int var55 = 0; var55 < var49; var55++) {
                     var50[var55] = var48.nextInt(8192) - 4096;
                     var51[var55] = var48.nextInt(8192) - 4096;
                  }

                  long var96 = System.nanoTime();
                  if (var47 != null) {
                     for (int var57 = 0; var57 < var49; var57++) {
                        var52 += var47.height(var50[var57], var51[var57]);
                     }
                  }

                  long var97 = System.nanoTime();

                  for (int var59 = 0; var59 < var49; var59++) {
                     var52 += var4.getBaseHeight(var50[var59], var51[var59], Types.OCEAN_FLOOR_WG, var1, var5);
                  }

                  long var98 = System.nanoTime();

                  for (int var61 = 0; var61 < var49; var61++) {
                     var52 += var46.classify(var50[var61], var51[var61]);
                  }

                  long var99 = System.nanoTime();
                  Constants.LOG
                     .info(
                        "[SR-BENCH] pass {} ({}), {} calls each: IR estimate {} | getBaseHeight {} ms ({} us/call) | biome classify {} ms ({} us/call) [sink {}]",
                        new Object[]{
                           var54,
                           var54 == 1 ? "cold" : "warm",
                           Integer.valueOf(var49),
                           var47 != null
                              ? String.format(Locale.ROOT, "%.1f ms (%.2f us/call)", (var97 - var96) / 1000000.0, (var97 - var96) / 1000.0 / var49)
                              : "n/a (no calibrated evaluator for this router)",
                           String.format(Locale.ROOT, "%.1f", (var98 - var97) / 1000000.0),
                           String.format(Locale.ROOT, "%.2f", (var98 - var97) / 1000.0 / var49),
                           String.format(Locale.ROOT, "%.1f", (var99 - var98) / 1000000.0),
                           String.format(Locale.ROOT, "%.2f", (var99 - var98) / 1000.0 / var49),
                           (long)var52
                        }
                     );
               }
            }

            var45.setBuildListener(
               (var2, var3x, var4x, var5x, var7) -> {
                  int var8x = var3x + var4x;
                  int var9x = var8x > 0 ? var4x * 100 / var8x : 0;
                  Constants.LOG
                     .debug(
                        "Built river region {} @ gridSpacing {}: {} samples ({} real, {} ocean-skipped = {}%), {} segments, {} ms, sample-cache {}% hits",
                        new Object[]{var2, var40, var8x, var3x, var4x, var9x, var7, String.format("%.1f", var5x), var36.hitPercent()}
                     );
               }
            );

            try {
               String var78 = var1.dimension().location().toString().replace(':', '_').replace('/', '_');
               Path var81 = var1.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var78);
               FileNetworkStore var85 = new FileNetworkStore(var81, var6, var45.config());
               var45.setNetworkStore(var85);
               if (var1.dimension().equals(Level.OVERWORLD)) {
                  RiverFlowFields.initStore(var81, var85.fingerprint());
                  final int var88 = var4.getSeaLevel();
                  RiverFlowFields.setWaterProbe((var2, var3x) -> var36.height(var2, var3x) <= var88 - 1.5);
                  if (var4 instanceof NoiseBasedChunkGenerator) {
                     final DensityFunction var90 = var5.router().finalDensity();
                     byte var92 = 18;
                     final AtomicReferenceArray var93 = new AtomicReferenceArray(262144);
                     RiverFlowFields.setDeepChannelProbe(new RiverFlowField.DeepChannelProbe() {
                        @Override
                        public boolean channelAt(int var1, int var2) {
                           return this.densityAt(var1, var2) < 0.0;
                        }

                        @Override
                        public double densityAt(int var1, int var2) {
                           if (var36.height(var1, var2) < var88 + 1) {
                              return Double.POSITIVE_INFINITY;
                           } else {
                              long var3x = (long)var1 << 32 ^ var2 & 4294967295L;
                              int var5x = (int)(Hashing.mix(var3x) >>> 46);
                              long[] var6x = (long[])var93.get(var5x);
                              if (var6x != null && var6x[0] == var3x) {
                                 RiverFlowFields.DEEP_DENSITY_MEMO.increment();
                                 return Double.longBitsToDouble(var6x[1]);
                              } else {
                                 double var7 = var90.compute(new SinglePointContext(var1, var88 - 1, var2));
                                 RiverFlowFields.DEEP_DENSITY_COMPUTED.increment();
                                 var93.set(var5x, new long[]{var3x, Double.doubleToRawLongBits(var7)});
                                 return var7;
                              }
                           }
                        }

                        @Override
                        public int verdictAt(int var1, int var2) {
                           if (var36.height(var1, var2) < var88 + 1) {
                              return 1;
                           } else {
                              return this.densityAt(var1, var2) < 0.0 ? 0 : 2;
                           }
                        }

                        @Override
                        public int floorClass(int var1, int var2) {
                           double var3x = var36.height(var1, var2);
                           if (var3x >= var88 + 1) {
                              return 0;
                           } else {
                              return var3x >= var88 - 6 ? 1 : 2;
                           }
                        }
                     });
                  } else {
                     RiverFlowFields.setDeepChannelProbe(null);
                  }

                  RawFlowTileStore.initStore(var81, var85.fingerprint());
               }
            } catch (RuntimeException var66) {
               Constants.LOG.warn("River network cache unavailable; networks won't persist this session ({})", var66.toString());
            }

            if (var32 || var29 >= 1 || var28) {
               try {
                  String var79 = var1.dimension().location().toString().replace(':', '_').replace('/', '_');
                  Path var82 = var1.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var79);
                  Path var86 = var82.resolve("fastcal-v10.txt");
                  Path var89 = var82.resolve("estcost.flag");
                  double var91 = Services.PLATFORM.gradeWindowAbove();
                  double var94 = Services.PLATFORM.gradeWindowBelow();

                  try {
                     if (Files.isRegularFile(var89)) {
                        String var95 = Files.readString(var89).trim();
                        if (var95.startsWith("ON")) {
                           var45.setEstCostVerdict(1);
                        } else if (var95.startsWith("OFF")) {
                           var45.setEstCostVerdict(2);
                        }
                     }
                  } catch (IOException var64) {
                  }

                  var45.setEstCostPersist(var6x -> {
                     try {
                        Files.writeString(var89, var6x == 2 ? "OFF\n" : "ON\n");
                        if (var6x == 2) {
                           Files.writeString(var86, "OFF " + var91 + " " + var94 + "\n");
                        }
                     } catch (IOException var8x) {
                     }
                  });
               } catch (RuntimeException var65) {
               }
            }

            var45.setStreamsEnabled(Services.PLATFORM.generateStreams());
            if (var4 instanceof NoiseBasedChunkGenerator && var72 == null && !var28 && var25 == var24) {
               int var80 = var13 == var12 ? 0 : -1;
               if (var13 instanceof FormSampler var83) {
                  var80 = var83.aquiferHandBack() ? 2 : 1;
               }

               StreamsWorldgen.FastSpec var84 = null;
               boolean var87 = var29 == 0 && var27 == null && var24 == var13;
               if (!var87 && var29 >= 1 && var30 != null && var31 != null) {
                  var84 = new StreamsWorldgen.FastSpec(var29, var32, var30.centers(), var30.bias(), var30.safe());
               }

               if (var80 >= 0 && (var87 || var84 != null)) {
                  J.put(var45, new StreamsWorldgen.EngineTerrain(var80, var84));
               }
            }

            var45.setBiomeBarAtPlanning(var38);
            a.put(var1, var45);
            ChunkStallWatch.start(var1.getServer());
            if (var1.dimension().equals(Level.OVERWORLD)) {
               e = var45;
               f = var1.registryAccess();
               g = var1;
               StreamPrefetcher.warmWorldOrigin(var45);
            }

            return var45;
         }
      }
   }

   public static RiverEngine active() {
      return e;
   }

   public static boolean wideFeatureBlocked(RiverEngine var0, Feature<?> var1, int var2, int var3) {
      Integer var4 = L.get(var1);
      if (var4 == null) {
         ResourceLocation var5 = BuiltInRegistries.FEATURE.getKey(var1);
         var4 = var5 == null ? 0 : K.getOrDefault(var5.toString(), 0);
         L.put(var1, var4);
      }

      if (var4 == 0) {
         return false;
      }

      int var6 = var4;
      return var0.corridorTouchesBox(var2 - var6, var3 - var6, var2 + var6, var3 + var6) || var0.lakeNearChunk(var2 - 8, var3 - 8, var6);
   }

   public static boolean structurePromisedAt(String var0, int var1, int var2) {
      ServerLevel var3 = g;
      boolean var4 = var3 != null && StructurePromises.promised(var3, var0, var1, var2);
      if (var4) {
         a("EXEMPT " + var0 + " at chunk " + var1 + "," + var2 + " -- a search already told a player it is here, so the water gives way");
      }

      return var4;
   }

   public static String structureId(Structure var0) {
      RegistryAccess var1 = f;
      if (var1 == null) {
         return null;
      }

       Registry<Structure> var2 = (Registry<Structure>)var1.registry(Registries.STRUCTURE).orElse(null);
      if (var2 == null) {
         return null;
      }

      ResourceLocation var3 = var2.getKey(var0);
      return var3 == null ? null : var3.toString();
   }

   public static boolean structureAlwaysGenerates(Structure var0) {
      RegistryAccess var1 = f;
      if (var1 == null) {
         return false;
      }

      Registry var2 = (Registry)var1.registry(Registries.STRUCTURE).orElse(null);
      if (var2 == null) {
         return false;
      }

       Optional<ResourceKey<Structure>> var3 = var2.getResourceKey(var0);
      if (var3.isEmpty()) {
         return false;
      }

       Optional<Holder.Reference<Structure>> var5 = var2.getHolder(var3.get());
       boolean var4 = var5.map(var0x -> var0x.is(P)).orElse(false);
      if (var4) {
          a("EXEMPT " + var3.get().location() + " -- it is tagged to generate whatever the water says");
      }

      return var4;
   }

   public static boolean structureFootprintOnStream(StructureStart var0) {
      RiverEngine var1 = e;
      if (var1 == null) {
         BoundingBox var4 = piecesBox(var0);
         a("ALLOWED", var4.minX(), var4.maxX(), var4.minZ(), var4.maxZ(), var4.maxY(), "no stream engine for this world yet");
         return false;
      }

      int var2 = var0.getChunkPos().getMinBlockX() >> 4;
      int var3 = var0.getChunkPos().getMinBlockZ() >> 4;
      if (t) {
         a("JUDGING " + structureId(var0.getStructure()) + " start chunk " + var2 + "," + var3);
      }

      return a(var1, piecesBox(var0), var1.platesOwningChunk(var2, var3));
   }

   public static void setFootprintVeto(FootprintVeto var0) {
      Q = var0;
   }

   public static BoundingBox piecesBox(StructureStart var0) {
       List<StructurePiece> var1 = var0.getPieces();
      if (var1 != null && !var1.isEmpty()) {
         BoundingBox var2 = null;

         for (StructurePiece var4 : var1) {
            BoundingBox var5 = var4.getBoundingBox();
            var2 = var2 == null
               ? new BoundingBox(var5.minX(), var5.minY(), var5.minZ(), var5.maxX(), var5.maxY(), var5.maxZ())
               : var2.encapsulate(var5);
         }

         return var2;
      } else {
         return var0.getBoundingBox();
      }
   }

   public static int claimedTopY(BoundingBox var0, int var1) {
      return var0.maxY() >= var1 ? var0.minY() : var0.maxY();
   }

   public static boolean structureFootprintOnStream(BoundingBox var0) {
      RiverEngine var1 = e;
      if (var1 == null) {
         return false;
      }

      int var2 = var0.minX() + var0.maxX() >> 1 >> 4;
      int var3 = var0.minZ() + var0.maxZ() >> 1 >> 4;
      return a(var1, var0, var1.platesOwningChunk(var2, var3));
   }

   public static void noteStructureWaitGaveUp(PlateKey[] var0, RiverEngine var1) {
      long var2 = System.nanoTime();

      for (PlateKey var7 : var0) {
         if (var7 != null && (var1 == null || !var1.isPlateReady(var7))) {
            R.put(var7, var2);
         }
      }
   }

   public static boolean structureWaitGaveUp(PlateKey[] var0) {
      if (R.isEmpty()) {
         return false;
      }

      long var1 = System.nanoTime();

      for (PlateKey var6 : var0) {
         Long var7 = var6 == null ? null : R.get(var6);
         if (var7 != null) {
            if (var1 - var7 < 600000000000L) {
               return true;
            }

            R.remove(var6, var7);
         }
      }

      return false;
   }

   public static int structureWaiters() {
      return Y.get();
   }

   public static long structureWaitLimitS() {
      if (V >= 0L) {
         return V;
      }

      Boolean var0 = T;
      if (var0 == null) {
         var0 = Services.PLATFORM.isModLoaded("c2me");
         T = var0;
      }

      return var0 ? 180L : 45L;
   }

   public static boolean awaitStructurePlates(RiverEngine var0, PlateKey[] var1, String var2) {
      if (structureWaitGaveUp(var1)) {
         return false;
      }

      CompletableFuture var3 = var0.whenPlatesReady(var1);
      if (var3.isDone()) {
         return true;
      }

      long var4 = structureWaitLimitS();
      if (var4 <= 0L) {
         return false;
      }

      long var6 = System.nanoTime();
      long var8 = 15L;
      Y.incrementAndGet();

      try {
         while (true) {
            long var10 = (System.nanoTime() - var6) / 1000000000L;
            if (var10 >= var4 || StreamPrefetcher.isServerStopping()) {
               break;
            }

            try {
               var3.get(Math.min(5L, var4 - var10), TimeUnit.SECONDS);
               return true;
            } catch (TimeoutException var19) {
               var10 = (System.nanoTime() - var6) / 1000000000L;
               if (var10 >= var8 && var10 < var4) {
                  var8 += 15L;
                  Constants.LOG
                     .info(
                        "Streams Reflowing: world generation is waiting for the streams around {} to be planned ({} s so far, at most {}).",
                        new Object[]{var2, var10, var4}
                     );
               }
            } catch (InterruptedException var20) {
               Thread.currentThread().interrupt();
               return false;
            } catch (ExecutionException var21) {
               return false;
            }
         }
      } finally {
         Y.decrementAndGet();
      }

      if (StreamPrefetcher.isServerStopping()) {
         return false;
      }

      StringBuilder var24 = new StringBuilder();

      for (PlateKey var14 : var1) {
         if (var14 != null && !var0.isPlateReady(var14)) {
            var24.append(" region (")
               .append(var14.cx())
               .append(',')
               .append(var14.cz())
               .append(") ")
               .append(StreamPrefetcher.plateDiag(var0, var14))
               .append(';');
         }
      }

      Constants.LOG
         .warn(
            "Streams Reflowing: the streams around {} were not planned after {} s. It is placed without the water check and generation carries on; structures in that area are not held up again while the planning finishes.",
            var2,
            var4
         );
      Constants.LOG.warn("Streams Reflowing (please include this line in a bug report):{} {}", var24, StreamPrefetcher.laneDiag());
      if (Z.getAndIncrement() < 3) {
         Constants.LOG.warn("Streams Reflowing (and these, what the planning was doing):{}", StreamPrefetcher.workingThreadStacks());
      }

      noteStructureWaitGaveUp(var1, var0);
      return false;
   }

   private static void a(String var0) {
      if (t) {
         Constants.LOG.info("[SR-STRUCT] {}", var0);
      }
   }

   private static void a(String var0, int var1, int var2, int var3, int var4, int var5, String var6) {
      if (t) {
         Constants.LOG.info("[SR-STRUCT] {} box x {}..{} z {}..{} top {} | {}", new Object[]{var0, var1, var2, var3, var4, var5, var6});
      }
   }

   private static boolean a(RiverEngine var0, BoundingBox var1, PlateKey[] var2) {
      int var3 = N;
      double var4 = (var1.maxX() - var1.minX()) / 2.0 + var3 + 1.0;
      double var6 = (var1.maxZ() - var1.minZ()) / 2.0 + var3 + 1.0;
      double var8 = (var1.minX() + var1.maxX()) / 2.0;
      double var10 = (var1.minZ() + var1.maxZ()) / 2.0;
      double var12 = Math.sqrt(var4 * var4 + var6 * var6);
      ServerLevel var14 = g;
      int var15 = var14 == null ? var1.maxY() : claimedTopY(var1, var14.getMaxBuildHeight() - 1);
      if (!var0.platesReady(var2)) {
         if (STRUCTURE_TRIAL.get()) {
            a(
               "DEFER",
               var1.minX(),
               var1.maxX(),
               var1.minZ(),
               var1.maxZ(),
               var15,
               "its regions are not built yet; the step re-runs when they are"
            );
            throw new StreamsWorldgen.StructurePlatesPending(var2);
         }

         RiverEngine.DEMAND_REASON
            .set("structure footprint x " + var1.minX() + ".." + var1.maxX() + " z " + var1.minZ() + ".." + var1.maxZ());

         boolean var16;
         try {
            var16 = awaitStructurePlates(var0, var2, "the structure at (" + (int)var8 + "," + (int)var10 + ")");
         } finally {
            RiverEngine.DEMAND_REASON.remove();
         }

         if (!var16) {
            a(
               "ALLOWED",
               var1.minX(),
               var1.maxX(),
               var1.minZ(),
               var1.maxZ(),
               var15,
               "its regions were not ready within the wait limit (reach " + (int)var12 + ")"
            );
            return false;
         }

         if (!var0.platesReady(var2)) {
            a("ALLOWED", var1.minX(), var1.maxX(), var1.minZ(), var1.maxZ(), var15, "its regions went out of the cache while it waited");
            return false;
         }
      }

      PlateKey[] var21 = var0.platesTouchingBox(var1.minX() - var3, var1.minZ() - var3, var1.maxX() + var3, var1.maxZ() + var3);
      if (a(var0, var21, var1.minX() - var3, var1.maxX() + var3, var1.minZ() - var3, var1.maxZ() + var3, var15, var3)) {
         boolean var22 = !t || a(var0, var2, var1.minX() - var3, var1.maxX() + var3, var1.minZ() - var3, var1.maxZ() + var3, var15, var3);
         a(
            "VETOED",
            var1.minX(),
            var1.maxX(),
            var1.minZ(),
            var1.maxZ(),
            var15,
            "stream or lake water inside the footprint"
               + (var22 ? "" : " -- SEEN ONLY BY THE WIDER SCOPE (" + var21.length + " region(s) vs " + var2.length + ")")
         );
         return true;
      } else {
         a(
            "ALLOWED",
            var1.minX(),
            var1.maxX(),
            var1.minZ(),
            var1.maxZ(),
            var15,
            "no water found in the footprint, from " + var21.length + " region(s) reaching it"
         );
         FootprintVeto var17 = Q;
         ServerLevel var18 = g;
         return var17 != null
            && var18 != null
            && var17.blocks(
               var18,
               var0,
               var1.minX() - var3,
               var1.minY() - var3,
               var1.minZ() - var3,
               var1.maxX() + var3,
               var1.maxY() + var3,
               var1.maxZ() + var3
            );
      }
   }

   public static boolean generationPointOnWater(int var0, int var1, int var2) {
      return generationPointOnWater(var0, var1, var2, null);
   }

   public static boolean generationPointOnWater(int var0, int var1, int var2, String var3) {
      RiverEngine var4 = e;
      if (var4 == null) {
         return false;
      }

      int var5 = N;
      int var6 = var0 >> 4 << 4;
      int var7 = var2 >> 4 << 4;
      double var8 = 8.0 + var5 + 1.0;
      PlateKey[] var10 = var4.platesOwningChunk(var0 >> 4, var2 >> 4);
      if (!var4.platesReady(var10)) {
         ServerLevel var11 = g;
         if (var3 != null && var11 != null && StructurePromises.promise(var11, var3, var0 >> 4, var2 >> 4)) {
            Constants.LOG
               .debug(
                  "Promised {} at chunk {},{} -- its area was not worked out yet, so the water will give way there instead.",
                  new Object[]{var3, var0 >> 4, var2 >> 4}
               );
         }

         return false;
      } else {
         return a(
            var4,
            var4.platesTouchingBox(var6 - var5, var7 - var5, var6 + 15 + var5, var7 + 15 + var5),
            var6 - var5,
            var6 + 15 + var5,
            var7 - var5,
            var7 + 15 + var5,
            var1,
            var5
         );
      }
   }

   private static boolean a(RiverEngine var0, PlateKey[] var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      for (int var8 = var2; var8 <= var3; var8 += 16) {
         for (int var9 = var4; var9 <= var5; var9 += 16) {
            if (var0.corridorNearChunkAmong(var1, var8, var9, var7)) {
               int var10 = Math.max(var2, var8 - var7);
               int var11 = Math.min(var3, var8 + 15 + var7);
               int var12 = Math.max(var4, var9 - var7);
               int var13 = Math.min(var5, var9 + 15 + var7);

               for (int var14 = var10; var14 <= var11 + 3; var14 += 4) {
                  int var15 = Math.min(var14, var11);

                  for (int var16 = var12; var16 <= var13 + 3; var16 += 4) {
                     int var17 = Math.min(var16, var13);
                     if (var0.inCorridorAmong(var1, var15, var17) && var6 >= var0.bedYAmong(var1, var15, var17) - 6) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      for (int var18 = var2; var18 <= var3 + 7; var18 += 8) {
         int var19 = Math.min(var18, var3);

         for (int var20 = var4; var20 <= var5 + 7; var20 += 8) {
            int var21 = Math.min(var20, var5);
            Lake var22 = var0.lakeAtAmong(var1, var19, var21);
            if (var22 != null) {
               int var23 = var22.level();
               int var24 = (int)Math.min(var23, Math.floor(var0.groundHeight(var19, var21)));
               if (var6 >= var24 - 6) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static void unloadLevel(ServerLevel var0) {
      RiverEngine var1;
      synchronized (a) {
         var1 = a.remove(var0);
      }

      if (var1 != null) {
         var1.releaseThreadMemos();
         if (var1 == e) {
            e = null;
            f = null;
            g = null;
         }
      }

      RiverFlowFields.unloadLevel(var0);
   }

   public static void clearActive() {
      ServerLevel var0 = g;
      if (var0 != null) {
         StructurePromises.forget(var0);
      }

      e = null;
      f = null;
      g = null;
      synchronized (a) {
         for (RiverEngine var3 : a.values()) {
            var3.releaseThreadMemos();
         }

         a.clear();
      }

      StreamPrefetcher.clearPending();
      RiverFlowFields.clear();
      StreamFlowGrid.clear();
      ChunkPlanCache.clear();
      RawFlowTileStore.close();
      RawFlowTiles.clear();
      StreamBiomeBridge.clear();
      RiverCarverFeature.releaseWorld();
      StreamVegetationFeature.releaseWorld();
   }

   private static void b(TerrainSampler var0, TerrainSampler var1, BiomeSource var2, Sampler var3, int var4) {
      int var5 = Integer.getInteger("streamsreflowing.calanalyze.n", 800);
      int var6 = Integer.getInteger("streamsreflowing.calanalyze.spread", 512);
      int var7 = Math.max(6000, var5 * 8);
      ArrayList var8 = new ArrayList();

      for (int var9 = 0; var9 < var7 && var8.size() < var5; var9++) {
         int var10 = var9 * 13 % 79 - 39;
         int var11 = var9 * 29 % 83 - 41;
         int var12 = var10 * var6;
         int var13 = var11 * var6;
         if (!var2.getNoiseBiome(QuartPos.fromBlock(var12), var4, QuartPos.fromBlock(var13), var3).is(BiomeTags.IS_OCEAN)) {
            var8.add(new int[]{var12, var13});
         }
      }

      int var32 = var8.size();
      double[] var33 = new double[var32];
      double[] var34 = new double[var32];
      String[] var35 = new String[var32];
      double[][] var36 = new double[var32][];
      LowResGrid.parallelFor(var32, var10x -> {
         int[] var11x = (int[])var8.get(var10x);
         var33[var10x] = var0.height(var11x[0], var11x[1]);
         var34[var10x] = var1.height(var11x[0], var11x[1]);
          Holder<Biome> var12x = var2.getNoiseBiome(QuartPos.fromBlock(var11x[0]), var4, QuartPos.fromBlock(var11x[1]), var3);
         var35[var10x] = var12x.unwrapKey().map(var0xx -> var0xx.location().toString()).orElse("?");
         var36[var10x] = a(var3, var11x[0], var11x[1]);
      });
      StringBuilder var14 = new StringBuilder("x,z,est,real,err,biome,continentalness,erosion,weirdness,temperature,humidity\n");

      for (int var15 = 0; var15 < var32; var15++) {
         int[] var16 = (int[])var8.get(var15);
         var14.append(var16[0])
            .append(',')
            .append(var16[1])
            .append(',')
            .append(var33[var15])
            .append(',')
            .append(var34[var15])
            .append(',')
            .append(var33[var15] - var34[var15])
            .append(',')
            .append(var35[var15]);

         for (double var20 : var36[var15]) {
            var14.append(',').append(String.format(Locale.ROOT, "%.4f", var20));
         }

         var14.append('\n');
      }

      try {
         Files.writeString(Path.of("streamsreflowing-calanalysis.csv"), var14);
      } catch (IOException var31) {
         Constants.LOG.info("[SR-CAL] could not write analysis csv: {}", var31);
      }

      double[] var37 = new double[var32];

      for (int var38 = 0; var38 < var32; var38++) {
         var37[var38] = var33[var38] - var34[var38];
      }

      Constants.LOG.info("[SR-CAL] calibration analysis: {} land columns, raw error sigma {} (mean {})", new Object[]{var32, b(b(var37)), b(a(var37))});
      String[] var39 = new String[]{
         "elevation/16 (SHIPPED)",
         "biome",
         "continentalness/0.05",
         "erosion/0.05",
         "weirdness/0.05",
         "temperature/0.05",
         "humidity/0.05",
         "elevation/16 x biome"
      };
       List<IntFunction<String>> var40 = List.<IntFunction<String>>of(
         var1x -> "e" + Math.floorDiv((int)var34[var1x], 16),
         var1x -> var35[var1x],
         var1x -> "c" + Math.round(var36[var1x][0] / 0.05),
         var1x -> "r" + Math.round(var36[var1x][1] / 0.05),
         var1x -> "w" + Math.round(var36[var1x][2] / 0.05),
         var1x -> "t" + Math.round(var36[var1x][3] / 0.05),
         var1x -> "h" + Math.round(var36[var1x][4] / 0.05),
         var2x -> "e" + Math.floorDiv((int)var34[var2x], 16) + "|" + var35[var2x]
      );

      for (int var41 = 0; var41 < var39.length; var41++) {
          IntFunction<String> var42 = var40.get(var41);
          LinkedHashMap<String, List<Integer>> var43 = new LinkedHashMap<>();

         for (int var21 = 0; var21 < var32; var21++) {
            var43.computeIfAbsent((String)var42.apply(var21), var0x -> new ArrayList<>()).add(var21);
         }

          ArrayList<Double> var44 = new ArrayList<>();
          ArrayList<Integer> var22 = new ArrayList<>();
         int var23 = 0;

          for (List<Integer> var25 : var43.values()) {
            if (var25.size() < 6) {
               var22.addAll(var25);
            } else {
               var23++;
               double var26 = 0.0;

               for (int var29 : var25) {
                  var26 += var37[var29];
               }

               var26 /= var25.size();

               for (int var58 : var25) {
                  var44.add(var37[var58] - var26);
               }
            }
         }

         if (!var22.isEmpty()) {
            double var45 = 0.0;

            for (int var27 : var22) {
               var45 += var37[var27];
            }

            var45 /= var22.size();

            for (int var54 : var22) {
               var44.add(var37[var54] - var45);
            }
         }

         double[] var47 = new double[var44.size()];

         for (int var48 = 0; var48 < var47.length; var48++) {
            var47[var48] = (Double)var44.get(var48);
         }

         int var49 = 0;

         for (double var59 : var47) {
            if (Math.abs(var59) <= 2.0) {
               var49++;
            }
         }

         Constants.LOG
            .info(
               "[SR-CAL]   {}: residual sigma {}, {}% within +-2 ({} usable buckets, {} sparse pts)",
               new Object[]{var39[var41], b(b(var47)), Math.round(100.0 * var49 / Math.max(1, var47.length)), var23, var22.size()}
            );
      }

      Constants.LOG.info("[SR-CAL] lowest residual sigma = the variable the correction should key on; wrote streamsreflowing-calanalysis.csv");
   }

   private static double[] a(Sampler var0, int var1, int var2) {
      double[] var3 = new double[5];

      try {
         TargetPoint var4 = var0.sample(QuartPos.fromBlock(var1), 0, QuartPos.fromBlock(var2));
         String[] var5 = new String[]{"continentalness", "erosion", "weirdness", "temperature", "humidity"};

         for (int var6 = 0; var6 < var5.length; var6++) {
            Object var7 = var4.getClass().getMethod(var5[var6]).invoke(var4);
            var3[var6] = ((Number)var7).doubleValue() / 10000.0;
         }
      } catch (ReflectiveOperationException | RuntimeException var8) {
         Arrays.fill(var3, Double.NaN);
      }

      return var3;
   }

   private static double a(double[] var0) {
      double var1 = 0.0;

      for (double var6 : var0) {
         var1 += var6;
      }

      return var0.length == 0 ? 0.0 : var1 / var0.length;
   }

   private static double b(double[] var0) {
      if (var0.length < 2) {
         return 0.0;
      }

      double var1 = a(var0);
      double var3 = 0.0;

      for (double var8 : var0) {
         var3 += (var8 - var1) * (var8 - var1);
      }

      return Math.sqrt(var3 / (var0.length - 1));
   }

   private static String b(double var0) {
      return String.format(Locale.ROOT, "%.2f", var0);
   }

   static {
      RiverEngine.setBuildRequester(StreamPrefetcher::submitChunkDemand);
      Class var0 = null;
      Method var1 = null;
      Method var2 = null;
      Object var3 = null;
      Object var4 = null;

      try {
         DensityFunction var5 = DensityFunctions.flatCache(DensityFunctions.zero());
         DensityFunction var6 = DensityFunctions.cache2d(DensityFunctions.zero());
         if (var5.getClass() == var6.getClass()) {
            var0 = var5.getClass();

            for (Method var10 : var0.getDeclaredMethods()) {
               if (var10.getParameterCount() == 0) {
                  if (var10.getReturnType().isEnum()) {
                     var1 = var10;
                  } else if (DensityFunction.class.isAssignableFrom(var10.getReturnType())) {
                     var2 = var10;
                  }
               }
            }

            if (var1 != null && var2 != null) {
               var1.setAccessible(true);
               var2.setAccessible(true);
               var3 = var1.invoke(var5);
               var4 = var1.invoke(var6);
            } else {
               var0 = null;
            }
         }
      } catch (Throwable var11) {
         var0 = null;
      }

      A = var0;
      B = var0 != null ? var1 : null;
      C = var0 != null ? var2 : null;
      D = var0 != null ? var3 : null;
      E = var0 != null ? var4 : null;
      J = Collections.synchronizedMap(new WeakHashMap<>());
      K = Map.of("biomeswevegone:noise_sphere", 16, "minecraft:forest_rock", 4, "minecraft:block_blob", 4);
      L = new ConcurrentHashMap<>();
      N = Math.max(0, Integer.getInteger("streamsreflowing.structurepad", 8));
      P = TagKey.create(Registries.STRUCTURE, new ResourceLocation("streamsreflowing", "always_generates"));
      STRUCTURE_TRIAL = ThreadLocal.withInitial(() -> Boolean.FALSE);
      R = new ConcurrentHashMap<>();
      V = Long.getLong("streamsreflowing.structurewait", -1L);
      Y = new AtomicInteger();
      Z = new AtomicInteger();
   }

   private record BandCorrection(double[] centers, double[] bias, boolean[] safe) {

      private BandCorrection {
bias = bias;
}

      double correct(double var1) {
         if (this.centers.length != 1 && !(var1 <= this.centers[0])) {
            if (var1 >= this.centers[this.centers.length - 1]) {
               return var1 - this.bias[this.bias.length - 1];
            }

            int var3 = 1;

            while (this.centers[var3] < var1) {
               var3++;
            }

            double var4 = (var1 - this.centers[var3 - 1]) / (this.centers[var3] - this.centers[var3 - 1]);
            return var1 - (this.bias[var3 - 1] + var4 * (this.bias[var3] - this.bias[var3 - 1]));
         } else {
            return var1 - this.bias[0];
         }
      }

      boolean gradeSafe(double var1) {
         if (var1 < this.centers[0] - 16.0 || var1 > this.centers[this.centers.length - 1] + 16.0) {
            return false;
         }

         if (this.centers.length == 1) {
            return this.safe[0];
         }

         int var3 = 1;

         while (var3 < this.centers.length && this.centers[var3] < var1) {
            var3++;
         }

         boolean var4 = this.safe[Math.max(0, var3 - 1)];
         boolean var5 = this.safe[Math.min(this.safe.length - 1, var3)];
         return var4 && var5;
      }

      int safeCount() {
         int var1 = 0;

         for (boolean var5 : this.safe) {
            if (var5) {
               var1++;
            }
         }

         return var1;
      }
   }

   private static final class ColumnCachedNode implements SimpleFunction {
      private final DensityFunction a;
      private long b = Long.MIN_VALUE;
      private double c;

      ColumnCachedNode(DensityFunction var1) {
         this.a = var1;
      }

      public double compute(FunctionContext var1) {
         long var2 = (long)var1.blockX() << 32 ^ var1.blockZ() & 4294967295L;
         if (var2 != this.b) {
            this.c = this.a.compute(var1);
            this.b = var2;
         }

         return this.c;
      }

      public double minValue() {
         return this.a.minValue();
      }

      public double maxValue() {
         return this.a.maxValue();
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return this.a.codec();
      }
   }

   private record EngineTerrain(int heights, StreamsWorldgen.FastSpec fast) {

      private EngineTerrain {
fast = fast;
}
   }

   private record FastCal(TerrainSampler land, TerrainSampler estimator, boolean hybridGrid, int gradePolicy, StreamsWorldgen.BandCorrection corr, TerrainSampler rawEst, boolean baked) {

      FastCal(TerrainSampler var1, TerrainSampler var2, boolean var3, int var4, StreamsWorldgen.BandCorrection var5, TerrainSampler var6) {
         this(var1, var2, var3, var4, var5, var6, false);
      }

      private FastCal {
estimator = estimator;
gradePolicy = gradePolicy;
rawEst = rawEst;
}
   }

   public record FastSpec(int gradePolicy, boolean baked, double[] centers, double[] bias, boolean[] safe) {

      public FastSpec {
baked = baked;
bias = bias;
}
   }

   public record SamplerParts(ChunkGenerator generator, LevelHeightAccessor level, RandomState randomState, BiomeSource biomeSource, Sampler climateSampler, TerrainSampler vanillaGround, TerrainSampler realHeight, TerrainSampler landHeight, TerrainSampler bulkLand, Object fastCal, RTFBridge.Handle rtf, RiverFlowField.BiomeClass oceanClass, boolean outletGate, List<TagKey<Biome>> outletTags, Set<ResourceLocation> outletIds, boolean fastOceanSkip, DensityFunction solidity, int seaLevel, TerrainSampler bulkGround, TerrainSampler surfaceReal, TerrainSampler surface) {

      public SamplerParts {
level = level;
biomeSource = biomeSource;
vanillaGround = vanillaGround;
landHeight = landHeight;
fastCal = fastCal;
oceanClass = oceanClass;
outletTags = outletTags;
fastOceanSkip = fastOceanSkip;
seaLevel = seaLevel;
surfaceReal = surfaceReal;
}

      public static StreamsWorldgen.SamplerParts plain(
         ChunkGenerator var0,
         LevelHeightAccessor var1,
         RandomState var2,
         RiverFlowField.BiomeClass var3,
         String var4,
         boolean var5,
         UnaryOperator<TerrainSampler> var6,
         StreamsWorldgen.FastSpec var7
      ) {
         TerrainSampler var8 = (var3x, var4x) -> var0.getBaseHeight(var3x, var4x, Types.OCEAN_FLOOR_WG, var1, var2);
         TerrainSampler var9 = var6.apply(var8);
         StreamsWorldgen.FastCal var10 = new StreamsWorldgen.FastCal(var9, null, false, 0, null, null);
         if (var7 != null && var0 instanceof NoiseBasedChunkGenerator var11) {
            TerrainSampler var12 = StreamsWorldgen.a(var11, var2);
            StreamsWorldgen.BandCorrection var13 = new StreamsWorldgen.BandCorrection(var7.centers(), var7.bias(), var7.safe());
            var10 = new StreamsWorldgen.FastCal(
               StreamsWorldgen.a(var12, var13, 1.0), StreamsWorldgen.a(var12, var13, 0.0), false, var7.gradePolicy(), var13, var12, var7.baked()
            );
         }

         ArrayList var14 = new ArrayList();
         HashSet var15 = new HashSet();
         StreamsWorldgen.parseOutletBiomes(var4, var14, var15);
         TerrainSampler var16 = (var3x, var4x) -> var0.getBaseHeight(var3x, var4x, Types.WORLD_SURFACE_WG, var1, var2);
         return new StreamsWorldgen.SamplerParts(
            var0,
            var1,
            var2,
            var0.getBiomeSource(),
            var2.sampler(),
            var8,
            var9,
            var10.land(),
            var10.land(),
            var10,
            null,
            var3,
            !var14.isEmpty() || !var15.isEmpty(),
            var14,
            var15,
            var5,
            StreamsWorldgen.a(var0, var2),
            var0.getSeaLevel(),
            var9,
            var16,
            var16
         );
      }
   }

   public static final class StructurePlatesPending extends RuntimeException {
      public final PlateKey[] owners;

      StructurePlatesPending(PlateKey[] var1) {
         super(null, null, false, false);
         this.owners = var1;
      }
   }
}
