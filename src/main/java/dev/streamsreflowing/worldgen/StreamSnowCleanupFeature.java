package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.platform.Services;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class StreamSnowCleanupFeature extends Feature<NoneFeatureConfiguration> {
   private static final int a = 32;
   private static final int b = 12;
   private static final int c = 8;
   private static final List<Entry<TagKey<Biome>, ResourceKey<Biome>>> d = List.of(
      Map.entry(a("c", "is_swamp"), b("nomansland", "blackwater_river")),
      Map.entry(a("c", "is_jungle"), b("nomansland", "lush_river")),
      Map.entry(a("c", "is_lush"), b("nomansland", "lush_river")),
      Map.entry(a("c", "is_desert"), b("nomansland", "desert_river"))
   );
   private static final AtomicBoolean e = new AtomicBoolean(true);
   private static final AtomicBoolean f = new AtomicBoolean(true);
   private static final float[] g = new float[]{0.0F, -0.05F, 0.05F, -0.12F, 0.12F, -0.2F, 0.2F, -0.3F, 0.3F};
   private static final float[][] h = new float[][]{{Float.NaN, 0.0F}, {0.3F, Float.NaN}, {0.3F, 0.0F}, {0.3F, -0.45F}};
   private static final ResourceKey<Biome> i = b("still_life", "arctic_river");
   private static final ResourceKey<Biome> j = b("still_life", "tundra_river");
   private static final ResourceKey<Biome> k = b("still_life", "boreal_river");
   private static final ResourceKey<Biome> l = b("still_life", "temperate_river");
   private static final ResourceKey<Biome> m = b("still_life", "warm_temperate_river");
   private static final ResourceKey<Biome> n = b("still_life", "steppe_river");
   private static final ResourceKey<Biome> o = b("still_life", "mediterranean_river");
   private static final ResourceKey<Biome> p = b("still_life", "desert_river");
   private static final ResourceKey<Biome> q = b("still_life", "savanna_river");
   private static final ResourceKey<Biome> r = b("still_life", "tropical_rainforest_river");
   private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> s = Map.ofEntries(
      Map.entry(b("still_life", "arctic_beach"), i),
      Map.entry(b("still_life", "arctic_desert"), i),
      Map.entry(b("still_life", "arctic_desert_basin"), i),
      Map.entry(b("still_life", "arctic_wetlands"), i),
      Map.entry(b("still_life", "barren_arctic_desert"), i),
      Map.entry(b("still_life", "boreal_alpine_tundra"), k),
      Map.entry(b("still_life", "cold_taiga"), k),
      Map.entry(b("still_life", "cold_taiga_clearings"), k),
      Map.entry(b("still_life", "evergreen_taiga"), k),
      Map.entry(b("still_life", "fen"), k),
      Map.entry(b("still_life", "taiga_beach"), k),
      Map.entry(b("still_life", "taiga_highlands"), k),
      Map.entry(b("still_life", "arid_beach"), p),
      Map.entry(b("still_life", "arid_desert"), p),
      Map.entry(b("still_life", "arid_highlands"), p),
      Map.entry(b("still_life", "barren_peaks"), p),
      Map.entry(b("still_life", "desert_wetlands"), p),
      Map.entry(b("still_life", "semiarid_desert"), p),
      Map.entry(b("still_life", "wooded_semiarid_desert"), p),
      Map.entry(b("still_life", "xeric_shrubland"), p),
      Map.entry(b("still_life", "barren_mediterranean_highlands"), o),
      Map.entry(b("still_life", "mediterranean_beach"), o),
      Map.entry(b("still_life", "mediterranean_forest"), o),
      Map.entry(b("still_life", "mediterranean_highlands"), o),
      Map.entry(b("still_life", "mediterranean_marsh"), o),
      Map.entry(b("still_life", "mediterranean_shrubland"), o),
      Map.entry(b("still_life", "mediterranean_steppe"), o),
      Map.entry(b("still_life", "mediterranean_wooded_highlands"), o),
      Map.entry(b("still_life", "snowy_barren_peaks"), o),
      Map.entry(b("still_life", "wooded_mediterranean_steppe"), o),
      Map.entry(b("still_life", "dry_tropical_highlands"), q),
      Map.entry(b("still_life", "flooded_savanna"), q),
      Map.entry(b("still_life", "grassland_savanna"), q),
      Map.entry(b("still_life", "humid_savanna"), q),
      Map.entry(b("still_life", "savanna_beach"), q),
      Map.entry(b("still_life", "savanna_highlands"), q),
      Map.entry(b("still_life", "tropical_dry_forest"), q),
      Map.entry(b("still_life", "wooded_savanna"), q),
      Map.entry(b("still_life", "cold_barren_highlands"), n),
      Map.entry(b("still_life", "cold_desert"), n),
      Map.entry(b("still_life", "cold_semi_desert"), n),
      Map.entry(b("still_life", "cold_steppe"), n),
      Map.entry(b("still_life", "dry_cold_steppe"), n),
      Map.entry(b("still_life", "flooded_grasslands"), n),
      Map.entry(b("still_life", "shrub_steppe"), n),
      Map.entry(b("still_life", "alpine_heathlands"), l),
      Map.entry(b("still_life", "alpine_plains"), l),
      Map.entry(b("still_life", "alpine_tundra"), l),
      Map.entry(b("still_life", "birch_forest"), l),
      Map.entry(b("still_life", "larch_woodlands"), l),
      Map.entry(b("still_life", "mire"), l),
      Map.entry(b("still_life", "mixed_forest_steppe"), l),
      Map.entry(b("still_life", "old_growth_temperate_forest"), l),
      Map.entry(b("still_life", "pine_woods"), l),
      Map.entry(b("still_life", "snowy_peaks"), l),
      Map.entry(b("still_life", "temperate_beach"), l),
      Map.entry(b("still_life", "temperate_forest"), l),
      Map.entry(b("still_life", "temperate_marsh"), l),
      Map.entry(b("still_life", "temperate_mixed_forest"), l),
      Map.entry(b("still_life", "temperate_swamp"), l),
      Map.entry(b("still_life", "wooded_highlands"), l),
      Map.entry(b("still_life", "mangrove_marsh"), r),
      Map.entry(b("still_life", "monsoon_forest"), r),
      Map.entry(b("still_life", "old_growth_tropical_rainforest"), r),
      Map.entry(b("still_life", "tropical_beach"), r),
      Map.entry(b("still_life", "tropical_highlands"), r),
      Map.entry(b("still_life", "tropical_rainforest"), r),
      Map.entry(b("still_life", "tropical_wetlands"), r),
      Map.entry(b("still_life", "barren_tundra"), j),
      Map.entry(b("still_life", "bog"), j),
      Map.entry(b("still_life", "cold_tundra_beach"), j),
      Map.entry(b("still_life", "lush_tundra"), j),
      Map.entry(b("still_life", "snowy_boreal_alpine_tundra"), j),
      Map.entry(b("still_life", "snowy_tundra"), j),
      Map.entry(b("still_life", "tundra_beach"), j),
      Map.entry(b("still_life", "wooded_tundra"), j),
      Map.entry(b("still_life", "mediterranean_swamp"), m),
      Map.entry(b("still_life", "warm_temperate_clearings"), m),
      Map.entry(b("still_life", "warm_temperate_woodlands"), m)
   );
   private static final int t = 8;
   private static final int u = 6;
   private static final int v = 48;
   private static final int w = 6;
   private static final int x = 30;
   private static final int y = 320;
   private static final int z = 8;
   private static final int A = 48;
   private static final int B = 8;
   private static final int C = 12;
   private static final int D = 4;
   private static final int E = 10;
   private static final int F = 6;
   private static final boolean G = Boolean.getBoolean("streamsreflowing.treetrace");
   private static int H;
   private static int I;
   private static final int[] J = new int[]{1, -1, 0, 0, 1, 1, -1, -1};
   private static final int[] K = new int[]{0, 0, 1, -1, 1, -1, 1, -1};
   public static final ResourceKey<Biome> STREAM_BIOME = ResourceKey.create(Registries.BIOME, new ResourceLocation("streamsreflowing", "stream"));
   private static final ResourceKey<Biome> L = b("minecraft", "river");
   private static final ResourceKey<Biome> M = b("minecraft", "frozen_river");
   private static final AtomicBoolean N = new AtomicBoolean();

   public StreamSnowCleanupFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   private static TagKey<Biome> a(String var0, String var1) {
      return TagKey.create(Registries.BIOME, new ResourceLocation(var0, var1));
   }

   private static ResourceKey<Biome> b(String var0, String var1) {
      return ResourceKey.create(Registries.BIOME, new ResourceLocation(var0, var1));
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      }

      WorldGenLevel var2 = var1.level();
      RiverEngine var3 = StreamsWorldgen.engine(var2);
      ChunkPos var4 = new ChunkPos(var1.origin());
      int var5 = var4.getMinBlockX();
      int var6 = var4.getMinBlockZ();
      ColumnPlan[] var7 = ChunkPlanCache.take(var3, var5, var6);
      int var8 = RiverEngine.LEAN_CALLS.get();
      int var9 = 0;
      if (var7 != null) {
         for (ColumnPlan var13 : var7) {
            if (var13 == null) {
               var9++;
            }
         }
      }

      boolean var33 = var3.corridorNearChunk(var5, var6, 8);
      if (!var33 && !var3.lakeNearChunk(var5, var6, 8)) {
         return false;
      }

      boolean var34 = false;
      int var35 = Services.PLATFORM.rebiomeStreamsMode();
      int var36 = Services.PLATFORM.rebiomeLakesMode();
      if (var35 != 0 || var36 != 0) {
         var34 |= a(var2, var3, var7, var5, var6, var35, var36);
      }

      if (!Services.PLATFORM.clearFloatingSnow()) {
         return var34;
      }

      if (!var33) {
         return var34 | this.a(var2, var3, var5, var6);
      }

      MutableBlockPos var14 = new MutableBlockPos();
      MutableBlockPos var15 = new MutableBlockPos();
      boolean var16 = false;

      for (int var17 = 0; var17 < 16; var17++) {
         for (int var18 = 0; var18 < 16; var18++) {
            int var19 = var5 + var17;
            int var20 = var6 + var18;
            ColumnPlan var21 = var7 != null ? var7[var17 * 16 + var18] : null;
            if (var21 == null) {
               var21 = var3.planLean(var19, var20);
            }

            if (var21 != ColumnPlan.NONE) {
               boolean var22 = var21.inCorridor();
               int var23 = var21.waterSurfaceY();
               int var24 = var2.getHeight(Types.WORLD_SURFACE_WG, var19, var20) - 1;
               int var25 = var2.getHeight(Types.WORLD_SURFACE, var19, var20) - 1;
               int var26 = Math.max(Math.max(var24, var25), var23 + 32);

               for (int var27 = var26; var27 >= var23 - 2; var27--) {
                  var14.set(var19, var27, var20);
                  BlockState var28 = var2.getBlockState(var14);
                  if (!var28.isAir()) {
                     if (!var28.is(Blocks.SNOW) && !var28.is(Blocks.SNOW_BLOCK) && !var28.is(Blocks.POWDER_SNOW)) {
                        if (var28.is(Blocks.GRASS_BLOCK)) {
                           BlockState var40 = var2.getBlockState(var15.set(var19, var27 + 1, var20));
                           if (var40.isCollisionShapeFullBlock(var2, var15)) {
                              var2.setBlock(var14, Blocks.DIRT.defaultBlockState(), 2);
                              var16 = true;
                           }
                        } else if (var28.getFluidState().isEmpty() && a(var2, var15, var19, var27, var20)) {
                           var2.setBlock(var14, Blocks.AIR.defaultBlockState(), 2);
                           var16 = true;
                        }
                     } else {
                        BlockState var29 = var2.getBlockState(var15.set(var19, var27 - 1, var20));
                        boolean var30 = var29.is(Blocks.ICE)
                           || var29.is(Blocks.PACKED_ICE)
                           || var29.is(Blocks.BLUE_ICE)
                           || var29.is(Blocks.FROSTED_ICE);
                        if (var29.isAir() || var30 && var22 && var28.is(Blocks.SNOW)) {
                           var2.setBlock(var14, Blocks.AIR.defaultBlockState(), 2);
                           var16 = true;

                           for (int var31 = var27 + 1; var31 <= var26; var31++) {
                              BlockState var32 = var2.getBlockState(var15.set(var19, var31, var20));
                              if (!var32.is(Blocks.SNOW) && !var32.is(Blocks.SNOW_BLOCK) && !var32.is(Blocks.POWDER_SNOW)) {
                                 break;
                              }

                              var2.setBlock(var15, Blocks.AIR.defaultBlockState(), 2);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      var16 |= this.a(var2, var3, var7, var5, var6);
      var16 |= this.a(var2, var3, var5, var6);
      if (Boolean.getBoolean("streamsreflowing.caldiag")) {
         int var39 = RiverEngine.LEAN_CALLS.get() - var8;
         if (var39 > 0) {
            Constants.LOG
               .info("[SR-SNOWPASS] chunk ({},{}): cachedArray={} nullCols={} leanQueries={}", new Object[]{var5 >> 4, var6 >> 4, var7 != null, var9, var39});
         }
      }

      return var34 || var16;
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, ColumnPlan[] var3, int var4, int var5) {
      MutableBlockPos var6 = new MutableBlockPos();
      boolean var7 = false;
      int var8 = 0;
      int var9 = var4 - 16;
      int var10 = var4 + 31;
      int var11 = var5 - 16;
      int var12 = var5 + 31;

      for (int var13 = -8; var13 < 24; var13++) {
         for (int var14 = -8; var14 < 24; var14++) {
            int var15 = var4 + var13;
            int var16 = var5 + var14;
            int var17 = var1.getHeight(Types.MOTION_BLOCKING, var15, var16) - 1;
            int var18 = var1.getHeight(Types.OCEAN_FLOOR, var15, var16) - 1;
            if (var17 > var18) {
               ColumnPlan var19 = var3 != null && var13 >= 0 && var13 < 16 && var14 >= 0 && var14 < 16 ? var3[var13 * 16 + var14] : var2.planLean(var15, var16);
               if (var19 == null) {
                  var19 = var2.planLean(var15, var16);
               }

               if (var19 != ColumnPlan.NONE && var19.inChannel()) {
                  int var20 = var19.waterSurfaceY();
                  if (!var1.getBlockState(var6.set(var15, var20, var16)).getFluidState().isEmpty()
                     || !var1.getBlockState(var6.set(var15, var20 - 1, var16)).getFluidState().isEmpty()) {
                     int var21 = Integer.MIN_VALUE;

                     for (int var22 = var20 - 6; var22 <= var20 + 2; var22++) {
                        if (var1.getBlockState(var6.set(var15, var22, var16)).is(BlockTags.LOGS)) {
                           var21 = var22;
                           break;
                        }
                     }

                     if (var21 == Integer.MIN_VALUE) {
                        if (var8 < 4) {
                           for (int var26 = var20 + 1; var26 <= var20 + 12; var26++) {
                              if (var1.getBlockState(var6.set(var15, var26, var16)).is(BlockTags.LEAVES)) {
                                 if (a(var1, var15, var26, var16, var9, var10, var11, var12)) {
                                    var7 = true;
                                 }

                                 if (G) {
                                    Constants.LOG
                                       .info("[SR-TREEFELL] canopy over water at {},{},{} leavesInBox={} culled={}", new Object[]{var15, var26, var16, H, I});
                                 }

                                 var8++;
                                 break;
                              }
                           }
                        }
                     } else {
                         List<BlockPos> var25 = a(var1, var15, var21, var16);
                        if (var25 == null) {
                           if (G) {
                              Constants.LOG.info("[SR-TREEFELL] declined trunk at {},{},{} (past the flood bounds)", new Object[]{var15, var21, var16});
                           }
                        } else {
                           for (BlockPos var24 : var25) {
                              var1.setBlock(var24, var24.getY() <= var20 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                           }

                           a(var1, var15, var21 + 4, var16, var9, var10, var11, var12);
                           if (G) {
                              Constants.LOG
                                 .info(
                                    "[SR-TREEFELL] felled trunk at {},{},{} logs={} leavesInBox={} culled={}",
                                    new Object[]{var15, var21, var16, var25.size(), H, I}
                                 );
                           }

                           var7 = true;
                        }
                     }
                  }
               }
            }
         }
      }

      return var7;
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, int var3, int var4) {
      MutableBlockPos var5 = new MutableBlockPos();
      boolean var6 = false;
      int var7 = var3 - 16;
      int var8 = var3 + 31;
      int var9 = var4 - 16;
      int var10 = var4 + 31;

      for (int var11 = -8; var11 < 24; var11++) {
         for (int var12 = -8; var12 < 24; var12++) {
            int var13 = var3 + var11;
            int var14 = var4 + var12;
            int var15 = var1.getHeight(Types.OCEAN_FLOOR_WG, var13, var14) - 1;
            int var16 = var1.getHeight(Types.OCEAN_FLOOR, var13, var14) - 1;
            if (var16 > var15
               && var1.getBlockState(var5.set(var13, var15 + 1, var14)).is(BlockTags.LOGS)
               && var1.getBlockState(var5.set(var13, var15, var14)).isAir()
               && (var2.inCorridor(var13, var14) || var2.lakeNear(var13, var14, 8) != null)) {
               List<BlockPos> var17 = a(var1, var13, var15 + 1, var14, 320, 8, 48);
               if (var17 == null) {
                  if (G) {
                     Constants.LOG.info("[SR-TREEFELL] declined hanging trunk at {},{},{} (past the flood bounds)", new Object[]{var13, var15 + 1, var14});
                  }
               } else {
                  HashSet var18 = new HashSet(var17);
                  boolean var19 = false;
                  boolean var20 = true;
                  int var21 = var15 + 1;

                  for (BlockPos var23 : var17) {
                     var21 = Math.max(var21, var23.getY());
                     if (var23.getX() >= var7
                        && var23.getX() <= var8
                        && var23.getZ() >= var9
                        && var23.getZ() <= var10
                        && Math.abs(var23.getX() - var13) != 8
                        && Math.abs(var23.getZ() - var14) != 8
                        && var23.getY() != var15 + 1 + 48) {
                        BlockPos var24 = var23.below();
                        if (var18.contains(var24)) {
                           continue;
                        }

                        BlockState var25 = var1.getBlockState(var24);
                        if (var25.isAir() || !var25.getFluidState().isEmpty() || var25.is(BlockTags.LEAVES) || !var25.isCollisionShapeFullBlock(var1, var24)) {
                           continue;
                        }

                        var19 = true;
                        break;
                     }

                     var20 = false;
                     break;
                  }

                  if (!var19 && var20) {
                     for (BlockPos var28 : var17) {
                        var1.setBlock(var28, Blocks.AIR.defaultBlockState(), 2);
                     }

                     int var27 = 0;

                     for (int var29 = var15 + 5; var29 <= var21 + 10; var29 += 20) {
                        a(var1, var13, var29, var14, var7, var8, var9, var10);
                        var27 += I;
                     }

                     if (G) {
                        Constants.LOG
                           .info(
                              "[SR-TREEFELL] felled hanging trunk at {},{},{} logs={} top={} leavesCulled={}",
                              new Object[]{var13, var15 + 1, var14, var17.size(), var21, var27}
                           );
                     }

                     var6 = true;
                  }
               }
            }
         }
      }

      return var6;
   }

   private static List<BlockPos> a(WorldGenLevel var0, int var1, int var2, int var3) {
      return a(var0, var1, var2, var3, 48, 6, 30);
   }

   private static List<BlockPos> a(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      ArrayDeque var7 = new ArrayDeque();
      HashSet var8 = new HashSet();
      ArrayList var9 = new ArrayList();
      BlockPos var10 = new BlockPos(var1, var2, var3);
      var7.add(var10);
      var8.add(var10);

      while (!var7.isEmpty()) {
         BlockPos var11 = (BlockPos)var7.poll();
         if (var0.getBlockState(var11).is(BlockTags.LOGS)) {
            var9.add(var11);
            if (var9.size() > var4) {
               return null;
            }

            for (int var12 = -1; var12 <= 1; var12++) {
               for (int var13 = -1; var13 <= 1; var13++) {
                  for (int var14 = -1; var14 <= 1; var14++) {
                     if (var12 != 0 || var13 != 0 || var14 != 0) {
                        BlockPos var15 = var11.offset(var13, var12, var14);
                        if (Math.abs(var15.getX() - var1) <= var5
                           && Math.abs(var15.getZ() - var3) <= var5
                           && var15.getY() >= var2 - 2
                           && var15.getY() <= var2 + var6
                           && var8.add(var15)) {
                           var7.add(var15);
                        }
                     }
                  }
               }
            }
         }
      }

      return var9;
   }

   private static boolean a(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = var1 - 10;
      int var9 = var1 + 10;
      int var10 = var3 - 10;
      int var11 = var3 + 10;
      int var12 = var2 - 10;
      int var13 = var2 + 10;
      byte var14 = 6;
      int var15 = Math.max(var8 - var14, var4);
      int var16 = Math.min(var9 + var14, var5);
      int var17 = Math.max(var10 - var14, var6);
      int var18 = Math.min(var11 + var14, var7);
      MutableBlockPos var19 = new MutableBlockPos();
       HashSet<Long> var20 = new HashSet<>();
       ArrayDeque<long[]> var21 = new ArrayDeque<>();
       HashSet<Long> var22 = new HashSet<>();

      for (int var23 = var12 - var14; var23 <= var13 + var14; var23++) {
         for (int var24 = var15; var24 <= var16; var24++) {
            for (int var25 = var17; var25 <= var18; var25++) {
               BlockState var26 = var0.getBlockState(var19.set(var24, var23, var25));
               if (var26.is(BlockTags.LEAVES)) {
                  var20.add(BlockPos.asLong(var24, var23, var25));
               } else if (var26.is(BlockTags.LOGS)) {
                  var21.add(new long[]{BlockPos.asLong(var24, var23, var25), 0L});
               }
            }
         }
      }

      if (G) {
         H = var20.size();
         I = 0;
      }

      if (var20.isEmpty()) {
         return false;
      }

      while (!var21.isEmpty()) {
         long[] var33 = (long[])var21.poll();
         if (var33[1] < 6L) {
            int var35 = BlockPos.getX(var33[0]);
            int var37 = BlockPos.getY(var33[0]);
            int var39 = BlockPos.getZ(var33[0]);

            for (int var27 = 0; var27 < 6; var27++) {
               int var28 = var35 + (var27 == 0 ? 1 : (var27 == 1 ? -1 : 0));
               int var29 = var37 + (var27 == 2 ? 1 : (var27 == 3 ? -1 : 0));
               int var30 = var39 + (var27 == 4 ? 1 : (var27 == 5 ? -1 : 0));
               long var31 = BlockPos.asLong(var28, var29, var30);
               if (var20.contains(var31) && var22.add(var31)) {
                  var21.add(new long[]{var31, var33[1] + 1L});
               }
            }
         }
      }

      boolean var34 = false;

      for (long var38 : var20) {
         if (!var22.contains(var38)) {
            int var40 = BlockPos.getX(var38);
            int var41 = BlockPos.getY(var38);
            int var42 = BlockPos.getZ(var38);
            if (var40 >= var8
               && var40 <= var9
               && var41 >= var12
               && var41 <= var13
               && var42 >= var10
               && var42 <= var11
               && var40 >= var15 + var14
               && var40 <= var16 - var14
               && var42 >= var17 + var14
               && var42 <= var18 - var14
               && var40 >= var4
               && var40 <= var5
               && var42 >= var6
               && var42 <= var7) {
               var19.set(var40, BlockPos.getY(var38), var42);
               BlockState var43 = var0.getBlockState(var19);
               if (!var43.hasProperty(LeavesBlock.PERSISTENT) || !(Boolean)var43.getValue(LeavesBlock.PERSISTENT)) {
                  var0.setBlock(var19, Blocks.AIR.defaultBlockState(), 2);
                  if (G) {
                     I++;
                  }

                  var34 = true;
               }
            }
         }
      }

      return var34;
   }

   private static boolean a(WorldGenLevel var0, RiverEngine var1, ColumnPlan[] var2, int var3, int var4, int var5, int var6) {
      boolean var7 = var5 != 0;
      boolean var8 = var6 != 0;
      Registry var9 = (Registry)var0.registryAccess().registry(Registries.BIOME).orElse(null);
      if (var9 == null) {
         return false;
      }

      ResourceKey var10 = ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", "river"));
      Holder var11 = (Holder)var9.getHolder(var10).orElse(null);
      if (var11 == null) {
         return false;
      }

      int var12 = Math.max(var0.getSeaLevel(), var0.getHeight(Types.MOTION_BLOCKING, var3 + 8, var4 + 8) - 1);
      Holder var13 = a(var0, var9, var3 + 8, var4 + 8, var12, var11);
      Holder var14 = a(var9, STREAM_BIOME);
      Holder var15 = a(var5, var14, var13);
      Holder var16 = a(var6, var14, var13);
      if (Boolean.getBoolean("streamsreflowing.caldiag") || f.compareAndSet(true, false)) {
         Constants.LOG
            .info(
               "[SR-REBIOME] chunk {},{} streamMode={} lakeMode={} | picked river={} | ours={} | streamStamp={} lakeStamp={}",
               new Object[]{
                  var3,
                  var4,
                  var5,
                  var6,
                  var13.unwrapKey().map(Object::toString).orElse("?"),
                  var14 == null ? "MISSING" : var14.unwrapKey().map(Object::toString).orElse("?"),
                  var15.unwrapKey().map(Object::toString).orElse("?"),
                  var16.unwrapKey().map(Object::toString).orElse("?")
               }
            );
      }

      if (var14 != null) {
         StreamBiomeBridge.set((Biome)var14.value(), ((Biome)var11.value()).getMobSettings());
      }

      StreamBiomeBridge.noteStamp(var15);
      StreamBiomeBridge.noteStamp(var16);
      ChunkAccess var17 = var0.getChunk(var3 >> 4, var4 >> 4);
      boolean var18 = false;
      Holder[] var19 = new Holder[16];
      int[] var20 = new int[16];
      int[] var21 = new int[16];
      long[] var22 = new long[4];

      for (int var23 = 0; var23 < 4; var23++) {
         for (int var24 = 0; var24 < 4; var24++) {
            int var25 = Integer.MAX_VALUE;
            int var26 = Integer.MIN_VALUE;
            int var27 = Integer.MAX_VALUE;
            int var28 = Integer.MIN_VALUE;
            boolean var29 = false;

            for (int var30 = 0; var30 < 4; var30++) {
               for (int var31 = 0; var31 < 4; var31++) {
                  int var32 = var23 * 4 + var30;
                  int var33 = var24 * 4 + var31;
                  int var34 = var3 + var32;
                  int var35 = var4 + var33;
                  int var36 = var0.getHeight(Types.MOTION_BLOCKING, var34, var35) - 1;
                  int var37 = var0.getHeight(Types.OCEAN_FLOOR, var34, var35) - 1;
                  if (var36 > var37) {
                     var29 = true;
                     if (var8) {
                        Lake var38 = var1.lakeAt(var34, var35);
                        if (var38 != null && Math.abs(var36 - var38.level()) <= 2) {
                           var27 = Math.min(var27, var36);
                           var28 = Math.max(var28, var36);
                           var22[var32 * 16 + var33 >> 6] = var22[var32 * 16 + var33 >> 6] | 1L << (var32 * 16 + var33 & 63);
                           continue;
                        }
                     }

                     if (var7) {
                        ColumnPlan var62 = var2 != null ? var2[var32 * 16 + var33] : null;
                        if (var62 == null) {
                           var62 = var1.planLean(var34, var35);
                        }

                        if (var62 != ColumnPlan.NONE && (var62.inChannel() || Math.abs(var36 - var62.waterSurfaceY()) <= 2)) {
                           var25 = Math.min(var25, var36);
                           var26 = Math.max(var26, var36);
                           var22[var32 * 16 + var33 >> 6] = var22[var32 * 16 + var33 >> 6] | 1L << (var32 * 16 + var33 & 63);
                        }
                     }
                  }
               }
            }

            if (!var29) {
               for (int var47 = 0; var47 < 4 && var25 == Integer.MAX_VALUE; var47++) {
                  for (int var51 = 0; var51 < 4 && var25 == Integer.MAX_VALUE; var51++) {
                     int var55 = var23 * 4 + var47;
                     int var59 = var24 * 4 + var51;
                     if (var7) {
                        ColumnPlan var60 = var2 != null ? var2[var55 * 16 + var59] : null;
                        if (var60 == null) {
                           var60 = var1.planLean(var3 + var55, var4 + var59);
                        }

                        if (var60 != ColumnPlan.NONE && var60.inChannel()) {
                           var25 = var60.waterSurfaceY();
                           var26 = var25;
                        }
                     }

                     if (var8 && var27 == Integer.MAX_VALUE) {
                        Lake var61 = var1.lakeAt(var3 + var47 + var23 * 4, var4 + var51 + var24 * 4);
                        if (var61 != null) {
                           var27 = var61.level();
                           var28 = var27;
                        }
                     }
                  }
               }
            }

            Holder var48 = var15;
            int var52;
            int var56;
            if (var25 != Integer.MAX_VALUE) {
               var52 = var25;
               var56 = var26;
            } else if (var27 != Integer.MAX_VALUE) {
               var52 = var27;
               var56 = var28;
               var48 = var16;
            } else {
               var52 = Integer.MIN_VALUE;
               var56 = Integer.MIN_VALUE;
            }

            if (var52 == Integer.MIN_VALUE && var29 && var8) {
               var52 = b(var0, var1, var3 + var23 * 4, var4 + var24 * 4);
               var56 = var52;
               var48 = var16;
            }

            if (var52 != Integer.MIN_VALUE) {
               var19[var23 * 4 + var24] = var48;
               var20[var23 * 4 + var24] = var52;
               var21[var23 * 4 + var24] = var56;
            }
         }
      }

      Holder[] var39 = new Holder[2];
      Holder[] var40 = new Holder[16];

      for (int var41 = 0; var41 < 4; var41++) {
         for (int var42 = 0; var42 < 4; var42++) {
            Holder var43 = var19[var41 * 4 + var42];
            int var45;
            int var46;
            if (var43 != null) {
               var45 = var20[var41 * 4 + var42];
               var46 = var21[var41 * 4 + var42];
            } else {
               var45 = Integer.MAX_VALUE;
               var46 = Integer.MIN_VALUE;

               for (int var49 = Math.max(0, var41 - 1); var49 <= Math.min(3, var41 + 1); var49++) {
                  for (int var53 = Math.max(0, var42 - 1); var53 <= Math.min(3, var42 + 1); var53++) {
                     Holder var57 = var19[var49 * 4 + var53];
                     if (var57 != null) {
                        var43 = var57;
                        var45 = Math.min(var45, var20[var49 * 4 + var53]);
                        var46 = Math.max(var46, var21[var49 * 4 + var53]);
                     }
                  }
               }

               if (var43 == null) {
                  continue;
               }
            }

            var43 = a(var0, var9, var43, var14, var11, var3 + var41 * 4 + 2, var46, var4 + var42 * 4 + 2, var39);
            var40[var41 * 4 + var42] = var43;
            int var50 = var45 - 12;
            int var54 = var46 + 8;

            for (int var58 = var50 & -4; var58 <= var54; var58 += 4) {
               if (a(var17, var41, var58, var42, var43)) {
                  var18 = true;
               }
            }
         }
      }

      if (a(var0, var1, var2, var3, var4, var40, var22, var7, var8)) {
         var18 = true;
      }

      return var18;
   }

   private static boolean a(
      WorldGenLevel var0, RiverEngine var1, ColumnPlan[] var2, int var3, int var4, Holder<Biome>[] var5, long[] var6, boolean var7, boolean var8
   ) {
      boolean var9 = false;
      MutableBlockPos var10 = new MutableBlockPos();

      for (int var11 = 0; var11 < 4; var11++) {
         for (int var12 = 0; var12 < 4; var12++) {
            Holder var13 = var5[var11 * 4 + var12];
            if (var13 != null) {
               for (int var14 = 0; var14 < 4; var14++) {
                  for (int var15 = 0; var15 < 4; var15++) {
                     int var16 = var11 * 4 + var14;
                     int var17 = var12 * 4 + var15;
                     int var18 = var3 + var16;
                     int var19 = var4 + var17;
                     int var20 = var0.getHeight(Types.MOTION_BLOCKING, var18, var19) - 1;
                     var10.set(var18, var20, var19);
                     if ((var6[var16 * 16 + var17 >> 6] & 1L << (var16 * 16 + var17 & 63)) != 0L) {
                        if (!a(var13, var10, var0)) {
                           BlockState var21 = var0.getBlockState(var10);
                           if (var21.getBlock() instanceof LiquidBlock
                              && var21.getFluidState().isSource()
                              && var21.getFluidState().is(FluidTags.WATER)
                              && var0.getBrightness(LightLayer.BLOCK, var10) < 10) {
                              var0.setBlock(var10, Blocks.ICE.defaultBlockState(), 2);
                              var9 = true;
                           }
                        }
                     } else if (var0.getBlockState(var10).is(Blocks.ICE) && a(var13, var10, var0)) {
                        boolean var25 = false;
                        if (var8) {
                           Lake var22 = var1.lakeAt(var18, var19);
                           var25 = var22 != null && Math.abs(var20 - var22.level()) <= 2;
                        }

                        ColumnPlan var26 = null;
                        boolean var23 = false;
                        if (!var25 && var7) {
                           var26 = var2 != null ? var2[var16 * 16 + var17] : null;
                           if (var26 == null) {
                              var26 = var1.planLean(var18, var19);
                           }

                           var23 = var26 != ColumnPlan.NONE && (var26.inChannel() || Math.abs(var20 - var26.waterSurfaceY()) <= 2);
                        }

                        if (var25 || var23) {
                           BlockState var24 = var23 && var26.inChannel()
                              ? Services.PLATFORM.streamWaterState(RiverCarverFeature.a(var26))
                              : Blocks.WATER.defaultBlockState();
                           var0.setBlock(var10, var24, 2);
                           var9 = true;
                        }
                     }
                  }
               }
            }
         }
      }

      return var9;
   }

   public static void tintMap(ServerLevel var0, int var1, int var2) {
      RiverEngine var3 = StreamsWorldgen.engine(var0);
      if (var3 == null) {
         Constants.LOG.info("[SR-TINTMAP] no engine (rivers off?)");
      } else {
         Registry var4 = (Registry)var0.registryAccess().registry(Registries.BIOME).orElse(null);
         int var5 = (var1 >> 4) - 1 << 4;
         int var6 = (var2 >> 4) - 1 << 4;
         int var7 = 0;
         int var8 = 0;
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;
         int var12 = 0;
         StringBuilder var13 = new StringBuilder();
         StringBuilder[] var14 = new StringBuilder[12];

         for (int var15 = 0; var15 < 12; var15++) {
            var14[var15] = new StringBuilder();
         }

         for (int var36 = 0; var36 < 12; var36++) {
            for (int var16 = 0; var16 < 12; var16++) {
               char var17 = '.';

               for (int var18 = 0; var18 < 4 && var17 == '.'; var18++) {
                  for (int var19 = 0; var19 < 4 && var17 == '.'; var19++) {
                     int var20 = var5 + var16 * 4 + var18;
                     int var21 = var6 + var36 * 4 + var19;
                     int var22 = var0.getHeight(Types.MOTION_BLOCKING, var20, var21) - 1;
                     int var23 = var0.getHeight(Types.OCEAN_FLOOR, var20, var21) - 1;
                     if (var22 > var23) {
                        BlockPos var24 = new BlockPos(var20, var22, var21);
                         Holder<Biome> var25 = var0.getBiome(var24);
                        boolean var26 = var25.is(STREAM_BIOME) || a(var25);
                        Lake var27 = var3.lakeAt(var20, var21);
                        boolean var28 = var27 != null && Math.abs(var22 - var27.level()) <= 2;
                        ColumnPlan var29 = var3.planLean(var20, var21);
                        boolean var30 = var29 != ColumnPlan.NONE && var29.inChannel();
                        int var31 = var29 != ColumnPlan.NONE ? Math.abs(var22 - var29.waterSurfaceY()) : -1;
                        boolean var32 = var29 != ColumnPlan.NONE && (var30 || var31 <= 2);
                        boolean var33 = !var28 && !var32 && b(var0, var3, var20 & -4, var21 & -4) != Integer.MIN_VALUE;
                        boolean var34 = var28 || var32 || var33;
                        if (var26) {
                           var10++;
                           var17 = 'S';
                        } else if (var34) {
                           var12++;
                           var17 = 'X';
                           boolean var35 = var0.getBiome(var24.above(4)).is(STREAM_BIOME) || var0.getBiome(var24.below(4)).is(STREAM_BIOME);
                           if (var35) {
                              var9++;
                              var17 = 'v';
                           }

                           if (var13.length() < 900) {
                              var13.append(
                                 String.format(
                                    " (%d,%d,%d %s plan=%s chan=%b d=%d lake=%b rim=%b)",
                                    var20,
                                    var22,
                                    var21,
                                    var4 != null ? var25.unwrapKey().map(var0x -> var0x.location().getPath()).orElse("?") : "?",
                                    var29 == ColumnPlan.NONE ? "NONE" : "yes",
                                    var30,
                                    var31,
                                    var28,
                                    var33
                                 )
                              );
                           }
                        } else {
                           var11++;
                           var17 = 'o';
                           if (var29 == ColumnPlan.NONE) {
                              var7++;
                           } else {
                              var8++;
                           }
                        }
                     }
                  }
               }

               var14[var36].append(var17);
            }
         }

         Constants.LOG.info("[SR-TINTMAP] 3x3 chunks at ({},{}) cellgrid (N=up, 4-block cells):", var1, var2);

         for (int var37 = 0; var37 < 12; var37++) {
            Constants.LOG.info("[SR-TINTMAP]   {}", var14[var37]);
         }

         Constants.LOG
            .info(
               "[SR-TINTMAP] watered cells: stampedOK={} SHOULD-STAMP-BUT-LAND={} (of which vertical-miss={}) correctly-skipped={} (planNONE={} corridorDeltaFail={})",
               new Object[]{var10, var12, var9, var11, var7, var8}
            );
         if (var13.length() > 0) {
            Constants.LOG.info("[SR-TINTMAP] samples:{}", var13);
         }
      }
   }

   private static int b(WorldGenLevel var0, RiverEngine var1, int var2, int var3) {
      int var4 = Integer.MIN_VALUE;

      for (int var5 = 0; var5 < 4; var5++) {
         int var6 = var2 + (var5 & 1) * 3;
         int var7 = var3 + (var5 >> 1 & 1) * 3;
         int var8 = var0.getHeight(Types.MOTION_BLOCKING, var6, var7) - 1;
         int var9 = var0.getHeight(Types.OCEAN_FLOOR, var6, var7) - 1;
         if (var8 > var9) {
            var4 = var8;
            break;
         }
      }

      if (var4 == Integer.MIN_VALUE) {
         return Integer.MIN_VALUE;
      }

      int var10 = 2 + var1.config().gridSpacing() / 2;

      for (int var11 = 0; var11 < 8; var11++) {
         Lake var12 = var1.lakeAt(var2 + 1 + J[var11] * var10, var3 + 1 + K[var11] * var10);
         if (var12 != null && Math.abs(var4 - var12.level()) <= 2) {
            return var12.level();
         }
      }

      return Integer.MIN_VALUE;
   }

   private static Holder<Biome> a(int var0, Holder<Biome> var1, Holder<Biome> var2) {
      return var0 == 2 && var1 != null ? var1 : var2;
   }

   private static Holder<Biome> a(
      WorldGenLevel var0, Registry<Biome> var1, Holder<Biome> var2, Holder<Biome> var3, Holder<Biome> var4, int var5, int var6, int var7, Holder<Biome>[] var8
   ) {
      if (var2 != null && var2 != var3) {
         Holder var9 = var0.getUncachedNoiseBiome(var5 >> 2, var6 >> 2, var7 >> 2);
         if (var9 != null && var9 != var2) {
            BlockPos var10 = new BlockPos(var5, var6, var7);
            boolean var11 = !a(var9, var10, var0);
            if (var11 != a(var2, var10, var0)) {
               return var2;
            }

            if (var8[1] == null) {
               var8[1] = var2;
               Holder var12 = a(var0, var1, var5, var7, var6, var4);
               if (var11 != a(var12, var10, var0)) {
                  var8[0] = var12;
               } else {
                  Holder var13 = a(var1, var11 ? M : L);
                  if (var13 != null && var11 != a(var13, var10, var0)) {
                     var8[0] = var13;
                  }
               }
            }

            Holder var14 = var8[0];
            return var14 != null && var11 != a(var14, var10, var0) ? var14 : var2;
         } else {
            return var2;
         }
      } else {
         return var2;
      }
   }

   private static boolean a(Holder<Biome> var0, BlockPos var1, WorldGenLevel var2) {
      return ((Biome)var0.value()).warmEnoughToRain(var1);
   }

   private static Holder<Biome> a(WorldGenLevel var0, Registry<Biome> var1, int var2, int var3, int var4, Holder<Biome> var5) {
      Holder<Biome> var6 = var0.getUncachedNoiseBiome(var2 >> 2, var4 >> 2, var3 >> 2);
      boolean var7 = Boolean.getBoolean("streamsreflowing.caldiag") || e.compareAndSet(true, false);

      try {
         BiomeSource var8 = var0.getLevel().getChunkSource().getGenerator().getBiomeSource();
         Sampler var9 = var0.getLevel().getChunkSource().randomState().sampler();
         TargetPoint var10 = var9.sample(var2 >> 2, var4 >> 2, var3 >> 2);
         MultiNoiseBiomeSource var11 = var8 instanceof MultiNoiseBiomeSource var12 ? var12 : null;
         if (var7) {
            Constants.LOG
               .info(
                  "[SR-RIVERPICK] at {},{}: asking {} {}",
                  new Object[]{var2, var3, var8.getClass().getName(), var11 != null ? "directly" : "through a substituted climate sampler"}
               );
         }

         for (float var15 : g) {
            Holder var16 = a(var8, var11, var9, var10, var2, var4, var3, Float.NaN, Float.NaN, var15);
            if (var7) {
               Constants.LOG
                  .info(
                     "[SR-RIVERPICK] at {},{} weirdness={} -> {} (stream water={})",
                     new Object[]{var2, var3, var15, var16 == null ? "null" : var16.unwrapKey().map(Object::toString).orElse("?"), var16 != null && a(var16)}
                  );
            }

            if (var16 != null && a(var16)) {
               return var16;
            }
         }

         for (float[] var31 : h) {
            for (float var19 : g) {
               Holder var20 = a(var8, var11, var9, var10, var2, var4, var3, var31[0], var31[1], var19);
               if (var20 != null && var20.is(BiomeTags.IS_RIVER)) {
                  if (var7) {
                     Constants.LOG
                        .info(
                           "[SR-RIVERPICK] at {},{} relaxed c={} e={} weirdness={} -> {}",
                           new Object[]{var2, var3, var31[0], var31[1], var19, var20.unwrapKey().map(Object::toString).orElse("?")}
                        );
                  }

                  return var20;
               }
            }
         }

         if (var7) {
            Constants.LOG
               .info(
                  "[SR-RIVERPICK] at {},{}: no river-tagged biome from any valley probe (source={}, asked {}) -> falling through to the tables",
                  new Object[]{var2, var3, var8.getClass().getName(), var11 != null ? "directly" : "through a substituted climate sampler"}
               );
         }
      } catch (Throwable var21) {
         if (var7) {
            Constants.LOG.info("[SR-RIVERPICK] at {},{}: probe failed ({})", new Object[]{var2, var3, var21.toString()});
         }
      }

       ResourceKey<Biome> var22 = var6.unwrapKey().map(s::get).orElse(null);
      if (var22 != null) {
         Holder var23 = a(var1, var22);
         if (var23 != null) {
            return var23;
         }
      }

      for (Entry var25 : d) {
         if (var6.is((TagKey)var25.getKey())) {
            Holder var26 = a(var1, (ResourceKey<Biome>)var25.getValue());
            if (var26 != null) {
               return var26;
            }
         }
      }

      return var5;
   }

   private static Holder<Biome> a(
      BiomeSource var0, MultiNoiseBiomeSource var1, Sampler var2, TargetPoint var3, int var4, int var5, int var6, float var7, float var8, float var9
   ) {
      return var1 != null
         ? var1.getNoiseBiome(
            new TargetPoint(
               var3.temperature(),
               var3.humidity(),
               Float.isNaN(var7) ? var3.continentalness() : Climate.quantizeCoord(var7),
               Float.isNaN(var8) ? var3.erosion() : Climate.quantizeCoord(var8),
               Climate.quantizeCoord(0.0F),
               Climate.quantizeCoord(var9)
            )
         )
         : var0.getNoiseBiome(var4 >> 2, var5 >> 2, var6 >> 2, a(var2, var7, var8, var9));
   }

   private static Sampler a(Sampler var0, float var1, float var2, float var3) {
      return new Sampler(
         var0.temperature(),
         var0.humidity(),
         Float.isNaN(var1) ? var0.continentalness() : DensityFunctions.constant(var1),
         Float.isNaN(var2) ? var0.erosion() : DensityFunctions.constant(var2),
         DensityFunctions.constant(0.0),
         DensityFunctions.constant(var3),
         var0.spawnTarget()
      );
   }

   private static boolean a(Holder<Biome> var0) {
      return var0.is(BiomeTags.IS_RIVER) || var0.is(BiomeTags.IS_OCEAN) && !var0.is(BiomeTags.IS_DEEP_OCEAN);
   }

   private static Holder<Biome> a(Registry<Biome> var0, ResourceKey<Biome> var1) {
      return var0.getHolder(var1).map(var0x -> (Holder<Biome>)var0x).orElse(null);
   }

   private static boolean a(ChunkAccess var0, int var1, int var2, int var3, Holder<Biome> var4) {
      int var5 = var0.getSectionIndex(var2);
      if (var5 >= 0 && var5 < var0.getSections().length) {
         LevelChunkSection var6 = var0.getSection(var5);
         PalettedContainer var7 = (PalettedContainer)var6.getBiomes();

         try {
            var7.getAndSet(var1, var2 >> 2 & 3, var3, var4);
            return true;
         } catch (RuntimeException var9) {
            if (N.compareAndSet(false, true)) {
               Constants.LOG
                  .warn(
                     "Streams Reflowing: this world's chunk format doesn't accept biome relabeling; stream and lake areas keep their original biome names (cosmetic only). ({})",
                     var9.toString()
                  );
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      return var0.getBlockState(var1.set(var2, var3 - 1, var4)).isAir()
         && var0.getBlockState(var1.set(var2, var3 + 1, var4)).isAir()
         && var0.getBlockState(var1.set(var2 - 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2 + 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 - 1)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 + 1)).isAir();
   }
}
