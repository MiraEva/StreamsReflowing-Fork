package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.bank.BankFeature;
import dev.streamsreflowing.worldgen.bank.BankFeatureLoader;
import dev.streamsreflowing.worldgen.bank.BankRing;
import dev.streamsreflowing.worldgen.bank.BankStyle;
import dev.streamsreflowing.worldgen.bank.BankStyleLoader;
import dev.streamsreflowing.worldgen.bank.BankTemplateFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class StreamVegetationFeature extends Feature<NoneFeatureConfiguration> {
   private static final double a = 0.75;
   private static final BlockState b = Blocks.GRASS.defaultBlockState();
   private static final BlockState c = Blocks.TALL_GRASS.defaultBlockState();
   private static final BlockState d = Blocks.FERN.defaultBlockState();
   private static volatile Set<ResourceLocation> e;
   private static volatile Set<String> f;
   private static volatile String g;
   private static volatile Object h;
   private static volatile List<BankFeature> i = List.of();
   private static volatile Map<String, BankFeature> j = Map.of();
   private static volatile List<StreamVegetationFeature.ResolvedBankFeature> k = List.of();
   private static volatile Map<String, StreamVegetationFeature.ResolvedBankFeature> l = Map.of();
   private static final double m = 8.0;

   public StreamVegetationFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      }

      WorldGenLevel var2 = var1.level();
      ChunkPos var3 = new ChunkPos(var1.origin());
      int var4 = var3.getMinBlockX();
      int var5 = var3.getMinBlockZ();
      RiverEngine var6 = StreamsWorldgen.engine(var2);
      RandomSource var7 = var1.random();
      ChunkGenerator var8 = var1.chunkGenerator();
      int var9 = Mth.clamp(Services.PLATFORM.floraRadius(), 1, 16);
      MutableBlockPos var10 = new MutableBlockPos();
      boolean var11 = false;
      var11 |= this.a(var2, var6, var8, var7, var4, var5, var9, var10);
      int var12 = Services.PLATFORM.underwaterFlora();
      int var13 = Services.PLATFORM.underwaterFloraPercent();
      if (var12 > 0 && var13 > 0) {
         if (var12 >= 3) {
            var11 |= this.a(var2, var7, var4, var5, var13);
         } else {
            var11 |= this.a(var2, var6, var7, var4, var5, var13);
            if (var12 >= 2) {
               var11 |= this.b(var2, var6, var7, var4, var5, var13);
            }
         }
      }

      int var14 = Services.PLATFORM.vegetation();
      int var15 = Services.PLATFORM.allWaterFlora();
      if (var14 <= 0 && var15 <= 0) {
         return var11;
      }

      if (var15 > 0) {
         var11 |= this.a(var2, var4, var5, var9, var10, (var6x, var7x, var8x, var10x) -> this.a(var2, var8, var7, var10, var6x, var7x, var8x, var15));
      } else {
         var11 |= this.a(var2, var6, var4, var5, var9, (var6x, var7x, var8x, var10x) -> this.a(var2, var8, var7, var10, var6x, var7x, var8x, var14));
      }

      return var11;
   }

   public static void bankMap(ServerLevel var0, int var1, int var2) {
      RiverEngine var3 = StreamsWorldgen.engine(var0);
      if (var3 == null) {
         Constants.LOG.info("[SR-BANKMAP] no engine (rivers off?)");
      } else {
         StreamVegetationFeature var4 = new StreamVegetationFeature(NoneFeatureConfiguration.CODEC);
         Optional var5 = var0.registryAccess().registry(Registries.PLACED_FEATURE);
         if (!var5.isEmpty()) {
             Registry<PlacedFeature> var6 = (Registry<PlacedFeature>)var5.get();
             ArrayList<StreamVegetationFeature.ResolvedBankFeature> var7 = new ArrayList<>();

            for (BankFeature var9 : b(var0)) {
                ArrayList<ConfiguredFeature<?, ?>> var10 = new ArrayList<>();

               for (ResourceLocation var12 : var9.features()) {
                  var6.getOptional(var12).ifPresent(var1x -> var10.add((ConfiguredFeature)var1x.feature().value()));
               }

               if (!var10.isEmpty()) {
                  var7.add(new StreamVegetationFeature.ResolvedBankFeature(var9, var10));
               }
            }

            int var26 = Services.PLATFORM.bankFeatures();
            int var27 = Mth.clamp(Services.PLATFORM.floraRadius(), 1, 16);
            Constants.LOG.info("[SR-BANKMAP] around ({},{}), scope={}, radius={}, entries={}", new Object[]{var1, var2, var26, var27, var7.size()});
            QuartBiomeMemo var28 = new QuartBiomeMemo();
            MutableBlockPos var29 = new MutableBlockPos();
            long[] var30 = new long[8];
            double[] var13 = new double[8];
            double[] var14 = new double[8];

            for (int var15 = -1; var15 <= 1; var15++) {
               for (int var16 = -1; var16 <= 1; var16++) {
                  int var17 = (var1 >> 4) + var15 << 4;
                  int var18 = (var2 >> 4) + var16 << 4;
                  var4.a(var0, var3, var17, var18, var27, (var6x, var7x, var8, var10x) -> {
                     a(var0, var3, var7, var28, var29, var6x, var7x, var10x, var13);
                     return false;
                  });
                  if (var26 >= 2) {
                     ColumnPlan[] var19 = ChunkPlanCache.get(var3, var17, var18);
                     var4.a(var0, var17, var18, var27, var29, (var11, var12x, var13x, var15x) -> {
                        ColumnPlan var17x = var19 != null ? var19[(var11 - var17) * 16 + (var12x - var18)] : var3.plan(var11, var12x);
                        if (var17x != ColumnPlan.NONE && !var17x.inChannel() && var17x.distance() - var17x.width() <= var27) {
                           return false;
                        }

                        if (var26 == 2 && var3.lakeNear(var11, var12x, 1) == null) {
                           return false;
                        }

                        a(var0, var3, var7, var28, var29, var11, var12x, -1.0, var14);
                        return false;
                     });
                  }
               }
            }

            for (int var31 = 0; var31 < 2; var31++) {
               double[] var33 = var31 == 0 ? var13 : var14;
               Constants.LOG
                  .info(
                     "[SR-BANKMAP] {}: columns={} matched={} avgChance%={} avgFit%={} (fits sampled {}) | denials: occupied={} submerged={} unseated={}",
                     new Object[]{
                        var31 == 0 ? "STREAM cols" : "ALL-WATER cols",
                        (long)var33[0],
                        (long)var33[1],
                        var33[1] > 0.0 ? String.format(Locale.ROOT, "%.2f", var33[2] / var33[1]) : "-",
                        var33[4] > 0.0 ? String.format(Locale.ROOT, "%.0f", 100.0 * var33[3] / var33[4]) : "-",
                        (long)var33[4],
                        (long)var33[5],
                        (long)var33[6],
                        (long)var33[7]
                     }
                  );
            }

            int var32 = 0;
            int var34 = 0;
            int var35 = 0;
             HashMap<String, Integer> var36 = new HashMap<>();
             HashMap<String, Integer> var37 = new HashMap<>();
             HashMap<String, Integer> var20 = new HashMap<>();

            for (BankTemplateFeature.PlacedRecord var22 : BankTemplateFeature.JOURNAL) {
               var20.merge(var22.branch(), 1, Integer::sum);
               if (Math.abs(var22.origin().getX() - var1) <= 48 && Math.abs(var22.origin().getZ() - var2) <= 48) {
                  var37.merge(var22.branch(), 1, Integer::sum);
                  var32++;
                  int var23 = 0;

                  for (int var24 = 0; var24 < var22.cells().size(); var24++) {
                     BlockState var25 = var0.getBlockState(var22.cells().get(var24));
                     if (var25 != var22.states().get(var24) && var25.getBlock() != var22.states().get(var24).getBlock()) {
                        var36.merge(BuiltInRegistries.BLOCK.getKey(var25.getBlock()).toString(), 1, Integer::sum);
                     } else {
                        var23++;
                     }
                  }

                  var34 += var23;
                  var35 += var22.cells().size();
                  if (var23 * 2 < var22.cells().size()) {
                     Constants.LOG
                        .info(
                           "[SR-BANKMAP]   destroyed placement at {}: {} of {} cells remain (cut {} at placement)",
                           new Object[]{var22.origin().toShortString(), var23, var22.cells().size(), var22.cutCells()}
                        );
                  }
               }
            }

            if (var32 == 0) {
               Constants.LOG
                  .info("[SR-BANKMAP] survival: no journaled placements within 48 blocks (placements are recorded per session -- generate fresh chunks first)");
            } else {
               Constants.LOG
                  .info(
                     "[SR-BANKMAP] survival: {} placements within 48 blocks, {}% of their blocks still standing; usurpers: {}",
                     new Object[]{var32, var35 == 0 ? 0 : 100 * var34 / var35, var36}
                  );
               Constants.LOG.info("[SR-BANKMAP] MEASURED placements by branch: near={}  whole session={}", var37, var20);
            }
         }
      }
   }

   private static void a(
      ServerLevel var0,
      RiverEngine var1,
      List<StreamVegetationFeature.ResolvedBankFeature> var2,
      QuartBiomeMemo var3,
      MutableBlockPos var4,
      int var5,
      int var6,
      double var7,
      double[] var9
   ) {
      var9[0]++;
      int var10 = a(var0, var5, var6);
      var4.set(var5, var10, var6);
      BlockState var11 = var0.getBlockState(var4);
      boolean var12 = !var11.getFluidState().isEmpty();
      if (!var12 || !(var7 < 0.0) && !(var7 > 8.0)) {
         if (!var12 && !var11.isAir() && !var11.canBeReplaced()) {
            var9[5]++;
         } else {
            BlockPos var13 = var4.below();
            if (!var0.getBlockState(var13).isFaceSturdy(var0, var13, Direction.UP)) {
               var9[7]++;
            } else {
               Holder var14 = var3.get(var0, var5, var10, var6);
               ResourceKey var15 = (ResourceKey)var14.unwrapKey().orElse(null);
               boolean var16 = false;
               double var17 = 0.0;
               BlockPos var19 = var4.immutable();

               for (StreamVegetationFeature.ResolvedBankFeature var21 : var2) {
                  if (var21.entry().matches(var14, var15)) {
                     var16 = true;
                     var17 += var21.entry().chance();
                     ConfiguredFeature var22 = var21.features().get(0);
                     if (var22.config() instanceof BankTemplateFeature.Config var23) {
                        var9[3] += BankTemplateFeature.fitFractionAt(var0, var19, var23);
                        var9[4]++;
                     }
                  }
               }

               if (var16) {
                  var9[1]++;
                  var9[2] += var17;
               }
            }
         }
      } else {
         var9[6]++;
      }
   }

   private static int a(WorldGenLevel var0, int var1, int var2) {
      return var0 instanceof ServerLevel ? var0.getHeight(Types.OCEAN_FLOOR, var1, var2) : var0.getHeight(Types.OCEAN_FLOOR_WG, var1, var2);
   }

   private static int b(WorldGenLevel var0, int var1, int var2) {
      return var0 instanceof ServerLevel ? var0.getHeight(Types.WORLD_SURFACE, var1, var2) : var0.getHeight(Types.WORLD_SURFACE_WG, var1, var2);
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, int var3, int var4, int var5, StreamVegetationFeature.ColumnAction var6) {
      ColumnPlan[] var7 = ChunkPlanCache.get(var2, var3, var4);
      new MutableBlockPos();
      boolean var9 = false;
      QuartBiomeMemo var10 = new QuartBiomeMemo();

      for (int var11 = 0; var11 < 16; var11++) {
         for (int var12 = 0; var12 < 16; var12++) {
            int var13 = var3 + var11;
            int var14 = var4 + var12;
            ColumnPlan var15 = var7 != null ? var7[var11 * 16 + var12] : null;
            if (var15 == null) {
               var15 = var2.plan(var13, var14);
            }

            if (var15 != ColumnPlan.NONE
               && !var15.inChannel()
               && (!StreamBiomeBlacklist.cutsAtCarve(var2) || !StreamBiomeBlacklist.isBlocked(var10.get(var1, var13, var15.waterSurfaceY(), var14)))) {
               double var16 = var15.distance() - var15.width();
               if (!(var16 > var5)) {
                  var9 |= var6.apply(var13, var14, Mth.clamp(var16 / var5, 0.0, 1.0), var15.width());
               }
            }
         }
      }

      return var9;
   }

   private boolean a(WorldGenLevel var1, int var2, int var3, int var4, MutableBlockPos var5, StreamVegetationFeature.ColumnAction var6) {
      int var7 = 16 + 2 * var4;
      boolean[] var8 = new boolean[var7 * var7];
      boolean var9 = false;

      for (int var10 = -var4; var10 < 16 + var4; var10++) {
         for (int var11 = -var4; var11 < 16 + var4; var11++) {
            int var12 = var2 + var10;
            int var13 = var3 + var11;
            int var14 = a(var1, var12, var13);
            var5.set(var12, var14, var13);
            if (var1.getBlockState(var5).getFluidState().is(FluidTags.WATER)) {
               var8[(var10 + var4) * var7 + var11 + var4] = true;
               var9 = true;
            }
         }
      }

      if (!var9) {
         return false;
      }

      double var21 = (double)var4 * var4;
      boolean var22 = false;

      for (int var23 = 0; var23 < 16; var23++) {
         for (int var24 = 0; var24 < 16; var24++) {
            if (!var8[(var23 + var4) * var7 + var24 + var4]) {
               double var15 = Double.MAX_VALUE;

               for (int var17 = -var4; var17 <= var4; var17++) {
                  for (int var18 = -var4; var18 <= var4; var18++) {
                     if (var8[(var23 + var17 + var4) * var7 + var24 + var18 + var4]) {
                        double var19 = var17 * var17 + var18 * var18;
                        if (var19 < var15) {
                           var15 = var19;
                        }
                     }
                  }
               }

               if (!(var15 > var21)) {
                  var22 |= var6.apply(var2 + var23, var3 + var24, Mth.clamp(Math.sqrt(var15) / var4, 0.0, 1.0), 0.0);
               }
            }
         }
      }

      return var22;
   }

   public static void releaseWorld() {
      i = List.of();
      j = Map.of();
      k = List.of();
      l = Map.of();
      h = null;
   }

   private static void a(WorldGenLevel var0) {
      RegistryAccess var1 = var0.registryAccess();
      if (h != var1) {
          ArrayList<BankFeature> var2 = new ArrayList<>(BankFeatureLoader.configEntries());
          HashMap<String, BankFeature> var3 = new HashMap<>(BankFeatureLoader.configEntriesById());
         var0.registryAccess().registry(BankFeature.REGISTRY_KEY).ifPresent(var2x -> var2x.entrySet().forEach(var2xx -> {
            var2.add((BankFeature)var2xx.getValue());
            var3.put(((ResourceKey)var2xx.getKey()).location().toString(), (BankFeature)var2xx.getValue());
         }));
          ArrayList<StreamVegetationFeature.ResolvedBankFeature> var4 = new ArrayList<>();
          HashMap<String, StreamVegetationFeature.ResolvedBankFeature> var5 = new HashMap<>();
         Optional var6 = var0.registryAccess().registry(Registries.PLACED_FEATURE);
         if (var6.isPresent()) {
             Registry<PlacedFeature> var7 = (Registry<PlacedFeature>)var6.get();
             IdentityHashMap<BankFeature, StreamVegetationFeature.ResolvedBankFeature> var8 = new IdentityHashMap<>();

            for (BankFeature var10 : var2) {
                ArrayList<ConfiguredFeature<?, ?>> var11 = new ArrayList<>();

               for (ResourceLocation var13 : var10.features()) {
                  var7.getOptional(var13)
                     .ifPresentOrElse(
                        var1x -> var11.add((ConfiguredFeature)var1x.feature().value()),
                        () -> Constants.LOG.warn("Custom bank feature references unknown placed feature {}", var13)
                     );
               }

               if (!var11.isEmpty()) {
                  StreamVegetationFeature.ResolvedBankFeature var17 = new StreamVegetationFeature.ResolvedBankFeature(var10, var11);
                  var8.put(var10, var17);
                  if (var10.standalone()) {
                     var4.add(var17);
                  }
               }
            }

             for (Entry<String, BankFeature> var15 : var3.entrySet()) {
                StreamVegetationFeature.ResolvedBankFeature var16 = var8.get(var15.getValue());
               if (var16 != null) {
                  var5.put((String)var15.getKey(), var16);
               }
            }
         }

         i = List.copyOf(var2);
         j = Map.copyOf(var3);
         k = List.copyOf(var4);
         l = Map.copyOf(var5);
         h = var1;
      }
   }

   private static List<BankFeature> b(WorldGenLevel var0) {
      a(var0);
      return i;
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, ChunkGenerator var3, RandomSource var4, int var5, int var6, int var7, MutableBlockPos var8) {
      a(var1);
      List var9 = k;
      Map var10 = l;
      List var11 = BankStyleLoader.configStyles();
      List var12 = RiverCarverFeature.a(var1);
      boolean var13 = !var10.isEmpty() && (a(var11) || a(var12));
      if (var9.isEmpty() && !var13) {
         return false;
      }

      QuartBiomeMemo var14 = new QuartBiomeMemo();
      int var15 = Services.PLATFORM.bankFeatures();
      if (var15 <= 0) {
         return false;
      }

      ArrayList var16 = new ArrayList();
      a(var1, var2, var5, var6, var7, (var1x, var2x, var3x) -> var16.add(new StreamVegetationFeature.BankSite(var1x, var2x, var3x.width(), var3x)));
      StreamVegetationFeature.RingContext var17 = new StreamVegetationFeature.RingContext(var1, var2, var11, var12, var10, var14);
      boolean var18 = this.a(var1, var3, var4, var8, var16, var9, var17, "stream", StreamVegetationFeature.SiteAnchor.LAND);
      if (var15 >= 2) {
         ColumnPlan[] var19 = ChunkPlanCache.get(var2, var5, var6);
         ArrayList var20 = new ArrayList();
         this.a(var1, var5, var6, var7, var8, (var7x, var8x, var9x, var11x) -> {
            ColumnPlan var13x = var19 != null ? var19[(var7x - var5) * 16 + (var8x - var6)] : null;
            if (var13x == null) {
               var13x = var2.plan(var7x, var8x);
            }

            if (var13x != ColumnPlan.NONE && !var13x.inChannel() && var13x.distance() - var13x.width() <= var7) {
               return false;
            }

            if (var15 == 2 && var2.lakeNear(var7x, var8x, 1) == null) {
               return false;
            }

            var20.add(new StreamVegetationFeature.BankSite(var7x, var8x, -1.0, null));
            return false;
         });
         var18 |= this.a(var1, var3, var4, var8, var20, var9, var17, "all-water", StreamVegetationFeature.SiteAnchor.LAND);
      }

      if (!var10.isEmpty() && (b(var11) || b(var12))) {
         ArrayList var24 = new ArrayList();
         a(var1, var2, var5, var6, (var1x, var2x, var3x) -> var24.add(new StreamVegetationFeature.BankSite(var1x, var2x, var3x.width(), var3x)));
         var18 |= this.a(var1, var3, var4, var8, var24, List.of(), var17, "stream-bed", StreamVegetationFeature.SiteAnchor.BED);
         var18 |= this.a(var1, var3, var4, var8, var24, List.of(), var17, "stream-surface", StreamVegetationFeature.SiteAnchor.SURFACE);
         if (var15 >= 2) {
            ColumnPlan[] var25 = ChunkPlanCache.get(var2, var5, var6);
            ArrayList var21 = new ArrayList();
            this.a(var1, var5, var6, var8, (var6x, var7x, var8x, var10x) -> {
               ColumnPlan var12x = var25 != null ? var25[(var6x - var5) * 16 + (var7x - var6)] : null;
               if (var12x == null) {
                  var12x = var2.plan(var6x, var7x);
               }

               if (var12x != ColumnPlan.NONE && var12x.inChannel()) {
                  return false;
               }

               if (var15 == 2 && var2.lakeAt(var6x, var7x) == null) {
                  return false;
               }

               var21.add(new StreamVegetationFeature.BankSite(var6x, var7x, -1.0, null));
               return false;
            });
            var18 |= this.a(var1, var3, var4, var8, var21, List.of(), var17, "water-bed", StreamVegetationFeature.SiteAnchor.BED);
            var18 |= this.a(var1, var3, var4, var8, var21, List.of(), var17, "water-surface", StreamVegetationFeature.SiteAnchor.SURFACE);
         }
      }

      return var18;
   }

   private static boolean a(List<BankStyle> var0) {
      for (BankStyle var2 : var0) {
         for (BankRing var4 : var2.rings()) {
            if (!var4.features().isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean b(List<BankStyle> var0) {
      for (BankStyle var2 : var0) {
         if (!var2.bed().features().isEmpty() || !var2.waterline().features().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static void a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, int var4, StreamVegetationFeature.PlanAction var5) {
      ColumnPlan[] var6 = ChunkPlanCache.get(var1, var2, var3);
      QuartBiomeMemo var7 = new QuartBiomeMemo();

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var2 + var8;
            int var11 = var3 + var9;
            ColumnPlan var12 = var6 != null ? var6[var8 * 16 + var9] : null;
            if (var12 == null) {
               var12 = var1.plan(var10, var11);
            }

            if (var12 != ColumnPlan.NONE
               && !var12.inChannel()
               && (!StreamBiomeBlacklist.cutsAtCarve(var1) || !StreamBiomeBlacklist.isBlocked(var7.get(var0, var10, var12.waterSurfaceY(), var11)))
               && !(var12.distance() - var12.width() > var4)) {
               var5.apply(var10, var11, var12);
            }
         }
      }
   }

   private static void a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, StreamVegetationFeature.PlanAction var4) {
      ColumnPlan[] var5 = ChunkPlanCache.get(var1, var2, var3);
      QuartBiomeMemo var6 = new QuartBiomeMemo();

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            int var9 = var2 + var7;
            int var10 = var3 + var8;
            ColumnPlan var11 = var5 != null ? var5[var7 * 16 + var8] : null;
            if (var11 == null) {
               var11 = var1.plan(var9, var10);
            }

            if (var11 != ColumnPlan.NONE
               && var11.inChannel()
               && (!StreamBiomeBlacklist.cutsAtCarve(var1) || !StreamBiomeBlacklist.isBlocked(var6.get(var0, var9, var11.waterSurfaceY(), var10)))) {
               var4.apply(var9, var10, var11);
            }
         }
      }
   }

   private boolean a(WorldGenLevel var1, int var2, int var3, MutableBlockPos var4, StreamVegetationFeature.ColumnAction var5) {
      boolean var6 = false;

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            int var9 = var2 + var7;
            int var10 = var3 + var8;
            var4.set(var9, a(var1, var9, var10), var10);
            if (var1.getBlockState(var4).getFluidState().is(FluidTags.WATER)) {
               var6 |= var5.apply(var9, var10, 0.0, 0.0);
            }
         }
      }

      return var6;
   }

   private boolean a(
      WorldGenLevel var1,
      ChunkGenerator var2,
      RandomSource var3,
      MutableBlockPos var4,
      List<StreamVegetationFeature.BankSite> var5,
      List<StreamVegetationFeature.ResolvedBankFeature> var6,
      StreamVegetationFeature.RingContext var7,
      String var8,
      StreamVegetationFeature.SiteAnchor var9
   ) {
      if (var5.isEmpty()) {
         return false;
      }

      BankTemplateFeature.PLACING_BRANCH.set(var8);
      int var10 = Math.min(4, 1 + var5.size() / 40);
      int var11 = Math.min(var5.size(), 12);
      boolean var12 = false;
      int var13 = 0;

      for (int var14 = 0; var13 < var11 && var14 < var10; var13++) {
         StreamVegetationFeature.BankSite var15 = (StreamVegetationFeature.BankSite)var5.get(var3.nextInt(var5.size()));
         int var16 = this.a(var1, var2, var3, var4, var15, var6, var7, var9);
         if (var16 > 0) {
            var14++;
            var12 |= var16 == 2;
         }
      }

      return var12;
   }

   private int a(
      WorldGenLevel var1,
      ChunkGenerator var2,
      RandomSource var3,
      MutableBlockPos var4,
      StreamVegetationFeature.BankSite var5,
      List<StreamVegetationFeature.ResolvedBankFeature> var6,
      StreamVegetationFeature.RingContext var7,
      StreamVegetationFeature.SiteAnchor var8
   ) {
      int var9 = var5.wx();
      int var10 = var5.wz();
      double var11 = var5.width();
      int var13;
      BlockPos var14;
      if (var8 == StreamVegetationFeature.SiteAnchor.SURFACE) {
         var13 = b(var1, var9, var10);
         var4.set(var9, var13, var10);
         BlockState var15 = var1.getBlockState(var4);
         if (!var15.isAir() && !var15.canBeReplaced()) {
            return 0;
         }

         BlockPos var16 = var4.below();
         if (!var1.getBlockState(var16).getFluidState().is(FluidTags.WATER)) {
            return 0;
         }

         var14 = var4.immutable();
      } else {
         var13 = a(var1, var9, var10);
         var4.set(var9, var13, var10);
         BlockState var26 = var1.getBlockState(var4);
         boolean var28 = !var26.getFluidState().isEmpty();
         if (var8 == StreamVegetationFeature.SiteAnchor.BED) {
            if (!var28 || !var26.getFluidState().is(FluidTags.WATER)) {
               return 0;
            }
         } else {
            if (var28 && (var11 < 0.0 || var11 > 8.0)) {
               return 0;
            }

            if (!var28 && !var26.isAir() && !var26.canBeReplaced()) {
               return 0;
            }
         }

         BlockPos var17 = var4.below();
         if (!var1.getBlockState(var17).isFaceSturdy(var1, var17, Direction.UP)) {
            return 0;
         }

         var14 = var4.immutable();
      }

      Holder var27 = var7.sourceBiomes().get(var1, var9, var13, var10);
      ResourceKey var29 = (ResourceKey)var27.unwrapKey().orElse(null);
      if (var8 != StreamVegetationFeature.SiteAnchor.LAND && UnderwaterFloraBlacklist.isBlocked(var27)) {
         return 0;
      }

      if (!var7.byId().isEmpty()) {
         BankStyle var30 = RiverCarverFeature.a(var7.styleConfig(), var7.styleDatapack(), var27);
         if (var30 != null && var30.enabled()) {
            BankRing var18 = switch (var8) {
               case BED -> var30.bed();
               case SURFACE -> var30.waterline();
               case LAND -> a(var1, var7.engine(), var30, var5, var13);
            };

            for (BankRing.Feature var20 : var18.features()) {
               if (!(var3.nextDouble() * 100.0 >= var20.chance())) {
                  StreamVegetationFeature.ResolvedBankFeature var21 = var7.byId().get(var20.id());
                  if (var21 != null) {
                     ConfiguredFeature var22 = var21.features().get(var21.features().size() == 1 ? 0 : var3.nextInt(var21.features().size()));

                     try {
                        if (var22.place(var1, var2, var3, var14)) {
                           return 2;
                        }
                     } catch (RuntimeException var25) {
                     }
                  }
               }
            }
         }
      }

      if (var8 != StreamVegetationFeature.SiteAnchor.LAND) {
         return 1;
      }

      for (StreamVegetationFeature.ResolvedBankFeature var32 : var6) {
         if (var32.entry().matches(var27, var29) && !(var3.nextDouble() * 100.0 >= var32.entry().chance())) {
            ConfiguredFeature var33 = var32.features().get(var32.features().size() == 1 ? 0 : var3.nextInt(var32.features().size()));

            try {
               if (var33.place(var1, var2, var3, var14)) {
                  return 2;
               }
            } catch (RuntimeException var24) {
            }
         }
      }

      return 1;
   }

   private static BankRing a(WorldGenLevel var0, RiverEngine var1, BankStyle var2, StreamVegetationFeature.BankSite var3, int var4) {
      ColumnPlan var5 = var3.plan();
      if (var5 != null && var5 != ColumnPlan.NONE && var5.inCorridor()) {
         double var6 = var5.width();
         double var8 = 2.0 * var6;
         double var10 = Math.max(5.0, var6 * var1.config().bankWidth());
         double var12 = Math.max(0.0, var5.distance() - var6);
         long var14 = var0.getSeed();
         int var16 = var2.ringIndexAt(
            var4 - var5.waterSurfaceY(),
            var12,
            var8,
            var10,
            false,
            BankRing.hash01(var14, var3.wx(), var4, var3.wz()),
            BankRing.hash01(var14, var3.wx(), 23063, var3.wz())
         );
         return var2.rings().get(var16);
      } else {
         return var2.bank();
      }
   }

   private boolean a(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, MutableBlockPos var4, int var5, int var6, double var7, int var9) {
      int var10 = a(var1, var5, var6);
      var4.set(var5, var10, var6);
      if (!var1.getBlockState(var4).isAir()) {
         return false;
      }

      boolean var11 = false;
      if (var9 >= 2) {
         double var12 = Mth.clamp(Services.PLATFORM.biomeBoostPercent(), 0, 100) / 100.0;
         if (var3.nextFloat() < var12 * (1.0 - var7)) {
            var11 = this.a(var1, var2, var3, var4);
         }
      }

      double var14 = Mth.clamp(Services.PLATFORM.bankGrassPercent(), 0, 100) / 100.0;
      if (var3.nextFloat() < var14 * (1.0 - 0.75 * var7) && var1.getBlockState(var4).isAir() && !var1.getBiome(var4).is(StreamsWorldgen.NO_BANK_GRASS)) {
         var11 |= this.a(var1, var4, var3);
      }

      return var11;
   }

   private boolean a(WorldGenLevel var1, BlockPos var2, RandomSource var3) {
      float var4 = var3.nextFloat();
      if (var4 < 0.6F) {
         return this.a(var1, var2, b);
      } else {
         return var4 < 0.8F ? this.a(var1, var2) : this.a(var1, var2, d);
      }
   }

   private boolean a(WorldGenLevel var1, BlockPos var2, BlockState var3) {
      if (var3.canSurvive(var1, var2)) {
         var1.setBlock(var2, var3, 2);
         return true;
      } else {
         return false;
      }
   }

   private boolean a(WorldGenLevel var1, BlockPos var2) {
      if (c.canSurvive(var1, var2) && var1.getBlockState(var2.above()).isAir()) {
         DoublePlantBlock.placeAt(var1, c, var2, 2);
         return true;
      } else {
         return false;
      }
   }

   private boolean a(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      BlockPos var5 = var4.below();
      if (!var1.getBlockState(var5).isFaceSturdy(var1, var5, Direction.UP)) {
         return false;
      }

       Holder<Biome> var6 = var1.getBiome(var4);
      BiomeGenerationSettings var7 = ((Biome)var6.value()).getGenerationSettings();
       List<HolderSet<PlacedFeature>> var8 = var7.features();
       ArrayList<Holder<PlacedFeature>> var9 = new ArrayList<>();
      a(var8, Decoration.VEGETAL_DECORATION, var9);
      a(var8, Decoration.LOCAL_MODIFICATIONS, var9);
      var9.removeIf(StreamVegetationFeature::b);
      var9.removeIf(StreamVegetationFeature::a);
      if (var9.isEmpty()) {
         return false;
      }

      ConfiguredFeature var10 = (ConfiguredFeature)((PlacedFeature)((Holder)var9.get(var3.nextInt(var9.size()))).value()).feature().value();

      try {
         boolean var11 = var10.place(var1, var2, var3, var4.immutable());
         if (var11) {
            b(var1, var4);
         }

         return var11;
      } catch (RuntimeException var12) {
         return false;
      }
   }

   private static boolean a(Holder<PlacedFeature> var0) {
      PlacedFeature var1 = (PlacedFeature)var0.value();

      for (PlacementModifier var3 : var1.placement()) {
         if (var3 instanceof HeightRangePlacement) {
            return true;
         }
      }

      Feature var4 = ((ConfiguredFeature)var1.feature().value()).feature();
      return var4 == StreamsWorldgen.STREAM_VEGETATION
         || var4 == StreamsWorldgen.RIVER_CARVER
         || var4 == Feature.TREE
         || var4 == Feature.RANDOM_SELECTOR
         || var4 == Feature.SIMPLE_RANDOM_SELECTOR;
   }

   private static void b(WorldGenLevel var0, BlockPos var1) {
      MutableBlockPos var2 = new MutableBlockPos();

      for (int var3 = -1; var3 <= 7; var3++) {
         for (int var4 = -2; var4 <= 2; var4++) {
            for (int var5 = -2; var5 <= 2; var5++) {
               var2.set(var1.getX() + var4, var1.getY() + var3, var1.getZ() + var5);
               BlockState var6 = var0.getBlockState(var2);
               if (var6.hasProperty(LeavesBlock.PERSISTENT) && !(Boolean)var6.getValue(LeavesBlock.PERSISTENT)) {
                  var0.setBlock(var2, (BlockState)var6.setValue(LeavesBlock.PERSISTENT, true), 2);
               }
            }
         }
      }
   }

   private static boolean b(Holder<PlacedFeature> var0) {
      if (var0.is(StreamsWorldgen.BIOME_BOOST_EXCLUDED)) {
         return true;
      }

      Set var1 = a();
      Set var2 = f;
      return !var1.isEmpty() || var2 != null && !var2.isEmpty()
         ? var0.unwrapKey().map(var2x -> var1.contains(var2x.location()) || var2 != null && var2.contains(var2x.location().getNamespace())).orElse(false)
         : false;
   }

   private static Set<ResourceLocation> a() {
      String var0 = Services.PLATFORM.biomeBoostBlacklist();
      if (var0 == null) {
         var0 = "";
      }

      Set var1 = e;
      if (var1 != null && var0.equals(g)) {
         return var1;
      }

      HashSet var2 = new HashSet();
      HashSet var3 = new HashSet();

      for (String var7 : var0.split(",")) {
         String var8 = var7.trim();
         if (!var8.isEmpty()) {
            if (var8.endsWith(":*")) {
               var3.add(var8.substring(0, var8.length() - 2));
            } else if (var8.indexOf(58) < 0) {
               var3.add(var8);
            } else {
               ResourceLocation var9 = ResourceLocation.tryParse(var8);
               if (var9 != null) {
                  var2.add(var9);
               }
            }
         }
      }

      f = var3;
      g = var0;
      e = var2;
      return var2;
   }

   private static void a(List<HolderSet<PlacedFeature>> var0, Decoration var1, List<Holder<PlacedFeature>> var2) {
      int var3 = var1.ordinal();
      if (var3 < var0.size()) {
          var0.get(var3).forEach(var2::add);
      }
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, MutableBlockPos var3, int var4, int var5, int var6, int var7) {
      var3.set(var4, var5, var6);
      if (!var1.getBlockState(var3).is(StreamsWorldgen.UNDERWATER_VEGETATION_FLOOR)) {
         return false;
      }

      Holder var8 = var1.getBiome(var3);
      if (UnderwaterFloraBlacklist.isBlocked(var8)) {
         return false;
      }

      double var9 = Mth.clamp((((Biome)var8.value()).getBaseTemperature() - 0.15) / 0.35, 0.0, 1.0);
      double var11 = Mth.clamp((Services.PLATFORM.biomeWetness(var8) - 0.1) / 0.3, 0.0, 1.0);
      int var13 = (int)Math.round(var7 * var9 * var11);
      if (var13 > 0 && var2.nextInt(100) < var13) {
         var3.set(var4, var5 + 1, var6);
         if (!var1.getBlockState(var3).is(Blocks.WATER)) {
            return false;
         }

         if (var1.getBlockState(var3.above()).is(Blocks.WATER) && var2.nextBoolean()) {
            DoublePlantBlock.placeAt(var1, Blocks.TALL_SEAGRASS.defaultBlockState(), var3, 2);
         } else {
            var1.setBlock(var3, Blocks.SEAGRASS.defaultBlockState(), 2);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, RandomSource var3, int var4, int var5, int var6) {
      MutableBlockPos var7 = new MutableBlockPos();
      boolean var8 = false;

      for (int var9 = 0; var9 < 16; var9++) {
         for (int var10 = 0; var10 < 16; var10++) {
            int var11 = var4 + var9;
            int var12 = var5 + var10;
            Lake var13 = var2.lakeAt(var11, var12);
            if (var13 != null) {
               int var14 = var1.getHeight(Types.OCEAN_FLOOR, var11, var12) - 1;
               if (var14 < var13.level()) {
                  var8 |= this.a(var1, var3, var7, var11, var14, var12, var6);
               }
            }
         }
      }

      return var8;
   }

   private boolean b(WorldGenLevel var1, RiverEngine var2, RandomSource var3, int var4, int var5, int var6) {
      ColumnPlan[] var7 = ChunkPlanCache.get(var2, var4, var5);
      MutableBlockPos var8 = new MutableBlockPos();
      new MutableBlockPos();
      boolean var10 = false;
      QuartBiomeMemo var11 = new QuartBiomeMemo();

      for (int var12 = 0; var12 < 16; var12++) {
         for (int var13 = 0; var13 < 16; var13++) {
            int var14 = var4 + var12;
            int var15 = var5 + var13;
            ColumnPlan var16 = var7 != null ? var7[var12 * 16 + var13] : null;
            if (var16 == null) {
               var16 = var2.plan(var14, var15);
            }

            if (var16 != ColumnPlan.NONE
               && var16.inChannel()
               && (!StreamBiomeBlacklist.cutsAtCarve(var2) || !StreamBiomeBlacklist.isBlocked(var11.get(var1, var14, var16.waterSurfaceY(), var15)))) {
               int var17 = var1.getHeight(Types.OCEAN_FLOOR, var14, var15) - 1;
               if (var17 < var16.waterSurfaceY()) {
                  var10 |= this.a(var1, var3, var8, var14, var17, var15, var6);
               }
            }
         }
      }

      return var10;
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, int var3, int var4, int var5) {
      MutableBlockPos var6 = new MutableBlockPos();
      boolean var7 = false;

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var3 + var8;
            int var11 = var4 + var9;
            int var12 = var1.getHeight(Types.OCEAN_FLOOR, var10, var11);
            var6.set(var10, var12, var11);
            if (var1.getBlockState(var6).is(Blocks.WATER)) {
               var7 |= this.a(var1, var2, var6, var10, var12 - 1, var11, var5);
            }
         }
      }

      return var7;
   }

   private record BankSite(int wx, int wz, double width, ColumnPlan plan) {

      private BankSite {
wz = wz;
plan = plan;
}
   }

   @FunctionalInterface
   private interface ColumnAction {
      boolean apply(int var1, int var2, double var3, double var5);
   }

   @FunctionalInterface
   private interface PlanAction {
      void apply(int var1, int var2, ColumnPlan var3);
   }

   private record ResolvedBankFeature(BankFeature entry, List<ConfiguredFeature<?, ?>> features) {

      private ResolvedBankFeature {
features = features;
}
   }

   private record RingContext(WorldGenLevel world, RiverEngine engine, List<BankStyle> styleConfig, List<BankStyle> styleDatapack, Map<String, StreamVegetationFeature.ResolvedBankFeature> byId, QuartBiomeMemo sourceBiomes) {

      private RingContext {
engine = engine;
styleDatapack = styleDatapack;
sourceBiomes = sourceBiomes;
}
   }

   private enum SiteAnchor {
      LAND,
      BED,
      SURFACE;
   }
}
