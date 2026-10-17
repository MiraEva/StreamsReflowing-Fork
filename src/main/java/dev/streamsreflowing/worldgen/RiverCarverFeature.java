package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.config.HydraulicGeometry;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.math.Hashing;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.bank.BankRing;
import dev.streamsreflowing.worldgen.bank.BankStyle;
import dev.streamsreflowing.worldgen.bank.BankStyleLoader;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluids;

public class RiverCarverFeature extends Feature<NoneFeatureConfiguration> {
   private static final boolean a = false;
   private static final double b = 0.15;
   private static final double c = 1.0;
   private static final int d = 16;
   private static final int e = 4;
   private static final int f = 20;
   private static final double g = 3.0;
   private static final double h = 2.0;
   private static final int i = 3;
   private static final int j = 3;
   private static final double k = 16.0;
   private static final int l = 6;
   private static final boolean m = true;
   private static final int n = 24;
   private static final double o = 1.0;
   private static final int[][] p = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final int q = 192;
   private static final Map<Long, BlockState[]> r = new ConcurrentHashMap<>();
   private static final Queue<Long> s = new ConcurrentLinkedQueue<>();
   private static volatile Object t;
   private static final ThreadLocal<long[]> u = ThreadLocal.withInitial(() -> new long[1]);
   private static final int v = 24;
   private static final ThreadLocal<int[]> w = new ThreadLocal<>();
   private static final int x = 8;
   private static final int y = 3;
   private static final int z = 4;
   private static final int A = 4;
   private static final int B = 3;
   private static final ThreadLocal<HashMap<Long, Integer>> C = ThreadLocal.withInitial(HashMap::new);
   private static final int D = 3;
   private static final int E = 3;
   private static final int F = 6;
   private static final int G = 5;
   private static final double H = 16.0;
   private static final int I = 14;
   private static final int J = 24;
   private static final int K = 6;
   private static final int L = 4;
   private static final double M = 2.0;
   private static final int N = 3;
   private static final boolean O = false;
   private static final double P = 2.0;
   private static final int Q = 5;
   private static final int R = 1;
   private static final int S = 8;
   private static final int T = 4;
   private static final double U = 2.0;
   private static final double V = 3.0;
   private static final int W = 4;
   private static final int X = Integer.MIN_VALUE;
   private static final int Y = -2147483647;
   private static final int[][] Z = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final int aa = 6;
   private static final int ab = 6;
   private static final int ac = 16;
   private static final int ad = 4;
   private static final int ae = 48;
   private static final int af = 3;
   private static final int ag = 3;
   private static volatile Object ah;
   private static volatile List<BankStyle> ai = List.of();
   private static volatile Object aj;
   private static final ConcurrentHashMap<Holder<Biome>, Double> ak = new ConcurrentHashMap<>();
   private static final double al = Double.parseDouble(System.getProperty("streamsreflowing.cohesionOverride", "NaN"));
   private static final boolean[] am = new boolean[]{true, false};
   private static final int an = 2048;
   private static final int ao = 10;
   private static final boolean ap = Boolean.getBoolean("streamsreflowing.treetrace");
   private static final boolean aq = Boolean.getBoolean("streamsreflowing.shavetrees");
   private static final int ar = 15;
   private static final int as = 3;
   private static final int at = 4;
   private static final int au = 2;
   private static final int[][] av = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final int aw = 3;
   private static final int ax = 6;
   private static final int[] ay = new int[0];
   private static final boolean az = !Boolean.getBoolean("streamsreflowing.noforeignseal");
   private static final int aA = 24;
   private static final int aB = 4;
   private static final Set<Block> aC = Set.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.DIRT_PATH);
   private static final int aD = 4;
   private static final double aE = 2.0;
   private static final double aF = 0.45;
   private static final double aG = 0.35;
   private static final double aH = 8.0;
   private static final double aI = 16.0;
   private static final int aJ = 1;
   private static final int aK = 1;
   private static final int aL = 2;
   private static final int aM = 2;
   private static final double aN = 2.0;
   private static final double aO = 0.6;
   private static final int aP = 8;
   private static final int aQ = 6;
   private static final double aR = 8.0;
   private static final int aS = 2;
   private static final int[] aT = a();
   private static volatile int[] aU = null;
   private static final int[] aV = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] aW = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final boolean aX = Boolean.getBoolean("streamsreflowing.deferseam");
   private static final Map<ResourceKey<Level>, Map<Long, int[]>> aY = new ConcurrentHashMap<>();
   private static final int aZ = 2048;
   private static final int ba = 2;

   public static void releaseWorld() {
      r.clear();
      s.clear();
      t = null;
      ai = List.of();
      ah = null;
      ak.clear();
      aj = null;
   }

   private static void a(WorldGenLevel var0, int var1, int var2, BlockState[] var3, BlockState[] var4) {
      ServerLevel var5 = var0.getLevel();
      if (t != var5) {
         r.clear();
         s.clear();
         t = var5;
      }

      BlockState[] var6 = new BlockState[512];
      System.arraycopy(var3, 0, var6, 0, 256);
      System.arraycopy(var4, 0, var6, 256, 256);
      Long var7 = (long)(var1 >> 4) << 32 ^ var2 >> 4 & 4294967295L;
      if (r.put(var7, var6) == null) {
         s.add(var7);

         while (r.size() > 192) {
            Long var8 = s.poll();
            if (var8 == null) {
               break;
            }

            r.remove(var8);
         }
      }
   }

   private static BlockState a(int var0, int var1, int var2) {
      BlockState[] var3 = r.get((long)(var0 >> 4) << 32 ^ var1 >> 4 & 4294967295L);
      return var3 == null ? null : var3[var2 + (var0 & 15) * 16 + (var1 & 15)];
   }

   private static BlockState a(int var0, int var1, WorldGenLevel var2, int var3, int var4) {
      BlockState var5 = a(var0, var1, 0);
      return var5 != null ? var5 : a(var2, var0, var1, var3, var4);
   }

   private static BlockState a(int var0, int var1, WorldGenLevel var2, int var3) {
      BlockState var4 = a(var0, var1, 256);
      return var4 != null ? var4 : a(var2, var0, var1, var3);
   }

   private static boolean a(int var0, int var1) {
      int[] var2 = w.get();
      return var2 == null || var0 >= var2[0] && var0 <= var2[1] && var1 >= var2[2] && var1 <= var2[3];
   }

   public RiverCarverFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      }

      long var2 = System.nanoTime();
      WorldGenLevel var4 = var1.level();
      RiverEngine var5 = StreamsWorldgen.engine(var4);
      RandomSource var6 = var1.random();
      FlowTrace.carvedChunks.incrementAndGet();
      ChunkPos var7 = new ChunkPos(var1.origin());
      int var8 = var7.getMinBlockX();
      int var9 = var7.getMinBlockZ();
      w.set(new int[]{var8 - 16, var8 + 31, var9 - 16, var9 + 31});
      int var10 = var4.getMinBuildHeight() + 1;
      int var11 = var4.getMaxBuildHeight() - 1;
      int var12 = var5.config().seaLevel();
      boolean var13 = Services.PLATFORM.customBanks();
      BlockState var14 = Blocks.WATER.defaultBlockState();
      BlockState var15 = Blocks.AIR.defaultBlockState();
      BlockState var16 = Blocks.GRASS_BLOCK.defaultBlockState();
      BlockState var17 = Blocks.GRASS.defaultBlockState();
      BlockState var18 = Blocks.TALL_GRASS.defaultBlockState();
      BlockState var19 = (BlockState)Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
      MutableBlockPos var20 = new MutableBlockPos();
      boolean var21 = false;
      int[] var22 = new int[256];
      BlockState[] var23 = new BlockState[256];
      BlockState[] var24 = new BlockState[256];
      int[] var25 = new int[256];
      boolean[] var26 = new boolean[256];

      for (int var27 = 0; var27 < 16; var27++) {
         for (int var28 = 0; var28 < 16; var28++) {
            int var29 = var4.getHeight(Types.OCEAN_FLOOR_WG, var8 + var27, var9 + var28) - 1;
            var22[var27 * 16 + var28] = var29;
            var25[var27 * 16 + var28] = var29;
            var23[var27 * 16 + var28] = a(var4, var8 + var27, var9 + var28, var29, var10);
            var24[var27 * 16 + var28] = a(var4, var8 + var27, var9 + var28, var29);
         }
      }

      int[] var107 = a(var4, var7);
      int[] var108 = new int[256];
      C.get().clear();
      int[] var109 = new int[256];
      Arrays.fill(var109, Integer.MIN_VALUE);
      int[] var30 = new int[256];
      Arrays.fill(var30, Integer.MIN_VALUE);
      int[] var31 = new int[256];
      Arrays.fill(var31, Integer.MIN_VALUE);
      int[] var32 = new int[256];
      Arrays.fill(var32, Integer.MIN_VALUE);
      int[] var33 = new int[256];
      Arrays.fill(var33, Integer.MAX_VALUE);
      int[] var34 = new int[256];
      Arrays.fill(var34, Integer.MIN_VALUE);
      boolean[] var35 = new boolean[256];
      int[] var36 = new int[256];
      if (var5.config().growFromWater()) {
         for (int var37 = 0; var37 < 16; var37++) {
            for (int var38 = 0; var38 < 16; var38++) {
               int var39 = var8 + var37;
               int var40 = var9 + var38;
               Lake var41 = var5.lakeWetting(var39, var40, 2);
               if (var41 != null) {
                  int var42 = var37 * 16 + var38;
                  var32[var42] = Mth.clamp(var41.level(), var10, var11);
                  var36[var42] = 3;
               }
            }
         }

         if (var5.lakeNearChunk(var8, var9, 24)) {
            byte var110 = 44;
            int[] var113 = new int[1936];
            int[] var116 = new int[1936];
            Arrays.fill(var113, Integer.MIN_VALUE);
            Arrays.fill(var116, Integer.MIN_VALUE);

            for (int var119 = -14; var119 < 30; var119++) {
               for (int var123 = -14; var123 < 30; var123++) {
                  int var127 = var8 + var119;
                  int var43 = var9 + var123;
                  Lake var44 = var5.lakeWetting(var127, var43, 2);
                  if (var44 != null) {
                     int var45 = Mth.clamp(var44.level(), var10, var11);
                     int var46 = (var119 + 14) * 44 + var123 + 14;
                     var113[var46] = var45;
                     if (a(var4, var8, var9, var25, var127, var43) >= var45) {
                        var116[var46] = var45;
                     }
                  }
               }
            }

            for (int var120 = 0; var120 < 16; var120++) {
               for (int var124 = 0; var124 < 16; var124++) {
                  int var128 = var120 * 16 + var124;
                  if (var32[var128] == Integer.MIN_VALUE) {
                     int var130 = var8 + var120;
                     int var133 = var9 + var124;
                     int var137 = var25[var128];
                     int var141 = Integer.MAX_VALUE;
                     int var47 = Integer.MIN_VALUE;
                     int var48 = Integer.MIN_VALUE;

                     for (int var49 = -14; var49 <= 14; var49++) {
                        for (int var50 = -14; var50 <= 14; var50++) {
                           int var51 = (var120 + var49 + 14) * 44 + var124 + var50 + 14;
                           if (var113[var51] > var48) {
                              var48 = var113[var51];
                           }

                           int var52 = var116[var51];
                           if (var52 != Integer.MIN_VALUE) {
                              int var53 = var49 * var49 + var50 * var50;
                              if (var53 <= 196 && (var53 < var141 || var53 == var141 && var52 > var47)) {
                                 var141 = var53;
                                 var47 = var52;
                              }
                           }
                        }
                     }

                     if (var141 != Integer.MAX_VALUE
                        && var137 > var48
                        && !var5.inCorridor(var130, var133)
                        && (var107.length == 0 || !a(var107, var130, var133, var137))) {
                        double var157 = Math.sqrt(var141);
                        double var169 = Math.min(2.0, var5.config().bankSlope());
                        double var186 = Math.min(14.0, Math.max(5.0, 16.0 * var5.config().bankWidth()));
                        double var55 = a(var47, var157, var169, 16.0, var186, var137);
                        double var57 = Math.pow(Math.min(1.0, var157 / var186), var5.config().aRiver());
                        double var59 = var47 + var57 * (var137 - var47);
                        double var61 = (d(var130, var133) - 0.5) * var5.config().bankNoise();
                        int var63 = Mth.clamp((int)Math.floor(Math.max(var55, var59) + var61), var48, var137);
                        if (var63 < var137) {
                           for (int var64 = var137; var64 > var63 && var64 > var10; var64--) {
                              var20.set(var130, var64, var133);
                              var4.setBlock(var20, var15, 2);
                              a(var33, var34, var128, var64);
                           }

                           c(var4, var20, var130, var133, var137 + 1, var11);
                           d(var4, var20, var130, var133, var63 + 1, var137);
                           var35[var128] = var5.config().containmentBerm();
                           var108[var128] += var137 - var63;
                           var22[var128] = var63;
                           a(var4, var20, var130, var63, var133, var24[var128], var23[var128], var10);
                           var21 = true;
                           if (c(var130, var133)) {
                              b(
                                 var130,
                                 var133,
                                 String.format(
                                    Locale.ROOT,
                                    "LAKE-SHORE-TAPER ground=%d cutTo=%d d=%.2f lake=%d hold=%d slopeLine=%.2f terrainLine=%.2f reach=%.1f effSlope=%.2f",
                                    var137,
                                    var63,
                                    var157,
                                    var47,
                                    var48,
                                    var55,
                                    var59,
                                    var186,
                                    var169
                                 )
                              );
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var5.config().containmentBerm()) {
            for (int var111 = 0; var111 < 16; var111++) {
               for (int var114 = 0; var114 < 16; var114++) {
                  int var117 = var111 * 16 + var114;
                  if (var32[var117] == Integer.MIN_VALUE) {
                     int var121 = var8 + var111;
                     int var125 = var9 + var114;
                     Lake var129 = var5.lakeNear(var121, var125, 4);
                     if (var129 != null && var129.digDepth() <= 0 && !(var129.coverageSmooth(var121, var125) > 0.5)) {
                        int var131 = 0;
                        int var134 = 0;
                        int var138 = 0;

                        label2074:
                        for (int var142 = 1; var142 <= 6; var142++) {
                           for (int var146 = -var142; var146 <= var142; var146++) {
                              for (int var151 = -var142; var151 <= var142; var151++) {
                                 if (Math.max(Math.abs(var146), Math.abs(var151)) == var142 && var129.coverageSmooth(var121 + var146, var125 + var151) > 0.5) {
                                    var131 = var142;
                                    var134 = var146;
                                    var138 = var151;
                                    break label2074;
                                 }
                              }
                           }
                        }

                        if (var131 == 0) {
                           label2054:
                           for (int var143 = 1; var143 <= 6; var143++) {
                              for (int var147 = -var143; var147 <= var143; var147++) {
                                 for (int var152 = -var143; var152 <= var143; var152++) {
                                    if (Math.max(Math.abs(var147), Math.abs(var152)) == var143) {
                                       Lake var158 = var5.lakeWetting(var121 + var147, var125 + var152, 2);
                                       if (var158 != null) {
                                          var129 = var158;
                                          var131 = var143;
                                          var134 = var147;
                                          var138 = var152;
                                          break label2054;
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        if (var131 != 0 && !var5.inCorridor(var121, var125)) {
                           int var144 = Mth.clamp(var129.level(), var10, var11);
                           int var148 = var22[var117];
                           if (var148 < var144) {
                              boolean var153 = false;
                              int var159 = Math.max(Math.abs(var134), Math.abs(var138));

                              for (int var163 = 1; var163 < var159 && !var153; var163++) {
                                 int var170 = var121 + (int)Math.round(var134 * ((double)var163 / var159));
                                 int var179 = var125 + (int)Math.round(var138 * ((double)var163 / var159));
                                 if (var129.coverageSmooth(var170, var179) > 0.5) {
                                    break;
                                 }

                                 int var187 = var170 - var8;
                                 int var54 = var179 - var9;
                                 int var204 = var187 >= 0 && var187 < 16 && var54 >= 0 && var54 < 16
                                    ? var22[var187 * 16 + var54]
                                    : var4.getHeight(Types.OCEAN_FLOOR_WG, var170, var179) - 1;
                                 var153 = var204 >= var144;
                              }

                              if (!var153) {
                                 int var164;
                                 if (var131 <= 3) {
                                    var164 = var144;
                                 } else {
                                    double var171 = (var131 - 3) / 4.0;
                                    double var188 = var171 * var171 * (3.0 - 2.0 * var171);
                                    var164 = (int)Math.round(var144 - var188 * (var144 - var148));
                                    if (var164 <= var148) {
                                       continue;
                                    }
                                 }

                                 BlockState var172 = a(var4, var121, var125, var148, var10);
                                 boolean var180 = false;

                                 for (int var189 = var148 + 1; var189 <= var164; var189++) {
                                    var20.set(var121, var189, var125);
                                    BlockState var198 = var4.getBlockState(var20);
                                    if (a(var4, var20, var198)) {
                                       BlockState var205 = a(var4, var121, var125, var148, var164 - var189, var172, var10);
                                       a(var4, var20, var121, var189, var125, var205, var10);
                                       var180 = true;
                                    }
                                 }

                                 if (var180) {
                                    var21 = true;
                                    if (c(var121, var125)) {
                                       b(var121, var125, "LAKE-BERM ground=" + var148 + " top=" + var164);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      ColumnPlan[] var112 = new ColumnPlan[256];
      HashMap var115 = new HashMap();
      QuartBiomeMemo var118 = new QuartBiomeMemo();
      boolean var122 = StreamBiomeBlacklist.cutsAtCarve(var5);
      CarveCensus.add(0, var2);
      long var126 = System.nanoTime();
      int[] var132 = new int[256];
      Arrays.fill(var132, Integer.MAX_VALUE);

      for (int var135 = 0; var135 < 16; var135++) {
         for (int var139 = 0; var139 < 16; var139++) {
            var112[var135 * 16 + var139] = var5.plan(var8 + var135, var9 + var139);
         }
      }

      byte var136 = 20;
      int[] var140 = new int[400];
      double[] var145 = new double[400];
      Arrays.fill(var140, Integer.MIN_VALUE);

      for (int var149 = -2; var149 < 18; var149++) {
         for (int var154 = -2; var154 < 18; var154++) {
            int var160 = (var149 + 2) * 20 + var154 + 2;
            int var165 = var8 + var149;
            int var173 = var9 + var154;
            boolean var181 = var149 >= 0 && var149 < 16 && var154 >= 0 && var154 < 16;
            ColumnPlan var190 = var181 ? var112[var149 * 16 + var154] : var5.planLean(var165, var173);
            if (var190 != null && var190.inCorridor() && !var190.inChannel()) {
               var145[var160] = var190.distance();
               var140[var160] = a(var5, var4, var190, var165, var173, var10);
            }
         }
      }

      int[] var150 = new int[256];
      Arrays.fill(var150, -2147483647);

      for (int var155 = 0; var155 < 16; var155++) {
         for (int var161 = 0; var161 < 16; var161++) {
            int var166 = var8 + var155;
            int var174 = var9 + var161;
            ColumnPlan var182 = var5.plan(var166, var174);
            var112[var155 * 16 + var161] = var182;
            if (var182 != ColumnPlan.NONE && var182.inChannel()) {
               var132[var155 * 16 + var161] = a(var4, var5, var20, var166, var174, var182, var11);
            }
         }
      }

      int[] var156 = new int[256];
      int[] var162 = new int[256];

      for (int var167 = 0; var167 < 16; var167++) {
         for (int var175 = 0; var175 < 16; var175++) {
            ColumnPlan var183 = var112[var167 * 16 + var175];
            if (var183 != null && var183.inCorridor()) {
               int var191 = var183.waterSurfaceY();
               int var199 = Integer.MAX_VALUE;
               int var206 = Integer.MAX_VALUE;

               for (int var56 = -2; var56 <= 2; var56++) {
                  for (int var218 = -2; var218 <= 2; var218++) {
                     if (var56 != 0 || var218 != 0) {
                        int var58 = var167 + var56;
                        int var231 = var175 + var218;
                        ColumnPlan var60 = var58 >= 0 && var58 < 16 && var231 >= 0 && var231 < 16
                           ? var112[var58 * 16 + var231]
                           : var5.planLean(var8 + var58, var9 + var231);
                        if (var60 != null && var60.inCorridor()) {
                           int var248 = var60.waterSurfaceY();
                           if (Math.max(Math.abs(var56), Math.abs(var218)) <= 1) {
                              var199 = Math.min(var199, var248);
                           }

                           var206 = Math.min(var206, var248);
                        }
                     }
                  }
               }

               if (var199 != Integer.MAX_VALUE && var191 - var199 >= 4) {
                  var156[var167 * 16 + var175] = 2;
                  var162[var167 * 16 + var175] = var199 + 1;
               } else if (var206 != Integer.MAX_VALUE && var191 - var206 >= 4) {
                  var156[var167 * 16 + var175] = 1;
                  var162[var167 * 16 + var175] = var206 + 1;
               }
            }
         }
      }

      int[] var168 = (int[])var132.clone();
      u.get()[0] = System.nanoTime();
      CarveCensus.add(1, var126);

      for (int var176 = 0; var176 < 16; var176++) {
         for (int var184 = 0; var184 < 16; var184++) {
            if (var132[var176 * 16 + var184] != Integer.MAX_VALUE) {
               int var192 = 0;
               int var200 = 0;
               int var207 = Integer.MAX_VALUE;

               for (int var214 = -4; var214 <= 4; var214++) {
                  for (int var219 = -4; var219 <= 4; var219++) {
                     int var225 = var176 + var214;
                     int var232 = var184 + var219;
                     if (var225 >= 0 && var225 <= 15 && var232 >= 0 && var232 <= 15) {
                        int var238 = var132[var225 * 16 + var232];
                        if (var238 != Integer.MAX_VALUE) {
                           var200++;
                           if (var238 != Integer.MIN_VALUE) {
                              var192++;
                              var207 = Math.min(var207, var238);
                           }
                        }
                     }
                  }
               }

               var168[var176 * 16 + var184] = var192 * 2 > var200 ? var207 : Integer.MIN_VALUE;
            }
         }
      }

      System.arraycopy(var168, 0, var132, 0, var132.length);

      for (int var177 = 0; var177 < 16; var177++) {
         for (int var185 = 0; var185 < 16; var185++) {
            int var193 = var8 + var177;
            int var201 = var9 + var185;
            int var208 = var22[var177 * 16 + var185];
            var20.set(var193, var208, var201);
            if (!var122 || !StreamBiomeBlacklist.isBlocked(var118.get(var4, var193, var208, var201))) {
               int var215 = var107.length == 0 ? Integer.MIN_VALUE : b(var107, var193, var201, var208);
               if (var215 != Integer.MIN_VALUE && var32[var177 * 16 + var185] == Integer.MIN_VALUE && a(var5, var193, var201, var215)) {
                  var215 = Integer.MIN_VALUE;
                  if (c(var193, var201)) {
                     b(
                        var193,
                        var201,
                        "STRUCTURE-SHIELD stands down: the structure here tops out below the stream bed, so it is under the water, not under the ground"
                     );
                  }
               }

               if (var215 != Integer.MIN_VALUE) {
                  int var221 = var32[var177 * 16 + var185];
                  if (var221 != Integer.MIN_VALUE) {
                     for (int var227 = var221; var227 > var208 && var227 > var10; var227--) {
                        var20.set(var193, var227, var201);
                        a(var4, (BlockPos)var20, var14);
                     }

                     b(var4, var20, var193, var221, var201, var14);
                  }

                  if (c(var193, var201)) {
                     b(
                        var193,
                        var201,
                        "STRUCTURE-SHIELD: column inside a structure footprint -- carve yields"
                           + (var221 == Integer.MIN_VALUE ? "" : "; lake water still filled to " + var221 + " over ground " + var208)
                     );
                  }

                  StructureOverlapDiag.shieldedColumn(var5, var193, var201, var208, var215);
               } else {
                  boolean var220 = false;
                  int var226 = var32[var177 * 16 + var185];
                  if (var226 != Integer.MIN_VALUE) {
                     var150[var177 * 16 + var185] = var226;
                     ColumnPlan var233 = var5.plan(var193, var201);
                     if (c(var193, var201)) {
                        b(
                           var193,
                           var201,
                           String.format(
                              Locale.ROOT,
                              "LAKE-SNAP snapY=%d ground=%d corr=%s chan=%s dist=%.1f w=%.1f surf=%d%s",
                              var226,
                              var208,
                              var233.inCorridor(),
                              var233.inChannel(),
                              var233.distance(),
                              var233.width(),
                              var233.waterSurfaceY(),
                              var233.inCorridor() ? "" : " reject=[" + var5.planDiag(var193, var201) + "]"
                           )
                        );
                     }

                     if (var233.inChannel()) {
                        double var239 = var233.width() > 1.0E-6 ? Math.min(1.0, var233.distance() / var233.width()) : 1.0;
                        int var62 = Math.max(1, (int)Math.round(2.0 * var233.width() * var5.config().streamDepth()));
                        int var267 = Math.min(var226, var233.waterSurfaceY());
                        int var276 = var267 - (int)Math.round(var62 * (1.0 - var239 * var239));
                        int var65 = Math.max(var10, Math.min(var208, var276));
                        if (c(var193, var201)) {
                           b(var193, var201, "LAKE-SNAP-DIG ground=" + var208 + " digTo=" + var65 + " bowlRef=" + var267);
                        }

                        int var66 = Math.min(var65, var226 - 1);

                        for (int var67 = Math.min(var208, var226); var67 > var66; var67--) {
                           var20.set(var193, var67, var201);
                           var4.setBlock(var20, var14, 2);
                        }

                        if (var66 < var208) {
                           a(var4, var20, var5, var193, var201, var226, var66, var208, var10, var23[var177 * 16 + var185]);
                        }
                     }

                     if (var208 >= var226 && !var233.inChannel()) {
                        int var240 = Math.max(var10, var226 - 2);
                        int var249 = var233.inCorridor() ? Integer.MIN_VALUE : a(var5, var193, var201, var226, var240);
                        if (var249 > var240) {
                           var240 = Math.min(var249, var226 - 1);
                        }

                        for (int var257 = var208; var257 > var226 && var257 > var10; var257--) {
                           var20.set(var193, var257, var201);
                           var4.setBlock(var20, var15, 2);
                           a(var33, var34, var177 * 16 + var185, var257);
                        }

                        var35[var177 * 16 + var185] = var5.config().containmentBerm();

                        for (int var258 = Math.min(var226, var208); var258 > var240 && var258 > var10; var258--) {
                           var20.set(var193, var258, var201);
                           var4.setBlock(var20, var14, 2);
                        }

                        a(var4, var20, var5, var193, var201, var226, var240, var208, var10, var23[var177 * 16 + var185]);
                        if (c(var193, var201)) {
                           b(
                              var193,
                              var201,
                              "LAKE-EXCAVATE ground="
                                 + var208
                                 + " level="
                                 + var226
                                 + " bedTop="
                                 + var240
                                 + " shelf="
                                 + (var249 == Integer.MIN_VALUE ? "none" : String.valueOf(var249))
                           );
                        }
                     }

                     if (!var233.inCorridor() && var208 < var226 - 1) {
                        int var241 = a(var5, var193, var201, var226, var208);
                        if (var241 > var208) {
                           BlockState var250 = var23[var177 * 16 + var185];

                           for (int var259 = var208 + 1; var259 <= var241; var259++) {
                              var20.set(var193, var259, var201);
                              if (a(var4, var20, var4.getBlockState(var20))) {
                                 a(var4, var20, var193, var259, var201, a(var4, var193, var201, var208, Math.max(1, var241 - var259), var250, var10), var10);
                              }
                           }

                           if (c(var193, var201)) {
                              b(
                                 var193,
                                 var201,
                                 "LAKE-SHELF floor "
                                    + var208
                                    + " -> "
                                    + var241
                                    + " (lake "
                                    + var226
                                    + ", depth "
                                    + (var226 - var241)
                                    + " over "
                                    + (var226 - var208)
                                    + ")"
                              );
                           }

                           var208 = var241;
                           var22[var177 * 16 + var185] = var241;
                        }
                     }

                     for (int var242 = var226; var242 > var208 && var242 > var10; var242--) {
                        var20.set(var193, var242, var201);
                        a(var4, (BlockPos)var20, var14);
                     }

                     b(var4, var20, var193, var226, var201, var14);
                     c(var4, var20, var193, var201, Math.max(var208 + 1, var226 + 1), var11);
                     var21 = true;
                     var112[var177 * 16 + var185] = var233;
                     if (!var233.inCorridor()) {
                        continue;
                     }

                     var220 = true;
                  }

                  int var234 = var177 * 16 + var185;
                  int var243 = Integer.MIN_VALUE;

                  for (int[] var277 : p) {
                     int var286 = var193 + var277[0];
                     int var293 = var201 + var277[1];
                     Lake var299 = var5.lakeWetting(var286, var293, 2);
                     if (var299 != null) {
                        var243 = Math.max(var243, Mth.clamp(var299.level(), var10, var11));
                     }
                  }

                  Lake var252 = b(var5, var193, var201);
                  var150[var234] = var252 == null ? Integer.MIN_VALUE : var252.level();
                  if (!var220 && var252 != null) {
                     int var261 = Mth.clamp(var252.level(), var10, var11);
                     if (c(var193, var201)) {
                        b(
                           var193,
                           var201,
                           "LAKE level=" + var261 + " dig=" + var252.digDepth() + " ground=" + var208 + " fills=" + (var252.digDepth() > 0 || var208 < var261)
                        );
                     }

                     if (var252.digDepth() > 0) {
                        int var273 = Mth.clamp(Math.min(var208, var252.level() - var252.digDepth()), var10, var261);
                        ColumnPlan var283 = var5.plan(var193, var201);
                        var112[var177 * 16 + var185] = var283;
                        int var291 = Integer.MAX_VALUE;
                        if (var283.inChannel()) {
                           double var296 = var283.width() > 1.0E-6 ? Math.min(1.0, var283.distance() / var283.width()) : 1.0;
                           int var311 = Math.max(1, (int)Math.round(2.0 * var283.width() * var5.config().streamDepth()));
                           var291 = var261 - (int)Math.round(var311 * (1.0 - var296 * var296));
                           var273 = Mth.clamp(Math.min(var273, var291), var10, var261);
                        }

                        int var297 = Mth.clamp(Math.max(var208, var252.level()), var10, var11);

                        for (int var305 = var297; var305 > var273; var305--) {
                           var20.set(var193, var305, var201);
                           var4.setBlock(var20, var305 <= var261 ? var14 : var15, 2);
                           if (var305 > var261) {
                              a(var33, var34, var234, var305);
                           }
                        }

                        var35[var234] = var5.config().containmentBerm();
                        if (var291 != Integer.MAX_VALUE && var273 < var208) {
                           a(var4, var20, var5, var193, var201, var261, var273, var208, var10, var23[var234]);
                        }

                        c(var4, var20, var193, var201, var261 + 1, var11);
                        var21 = true;
                        continue;
                     }

                     if (var208 < var261) {
                        ColumnPlan var278 = var5.plan(var193, var201);
                        var112[var177 * 16 + var185] = var278;
                        if (c(var193, var201)) {
                           b(
                              var193,
                              var201,
                              String.format(
                                 Locale.ROOT,
                                 "LAKE-CROSS corr=%s chan=%s dist=%.1f w=%.1f surf=%d%s",
                                 var278.inCorridor(),
                                 var278.inChannel(),
                                 var278.distance(),
                                 var278.width(),
                                 var278.waterSurfaceY(),
                                 var278.inCorridor() ? "" : " reject=[" + var5.planDiag(var193, var201) + "]"
                              )
                           );
                        }

                        if (var278.inChannel()) {
                           double var287 = var278.width() > 1.0E-6 ? Math.min(1.0, var278.distance() / var278.width()) : 1.0;
                           int var300 = Math.max(1, (int)Math.round(2.0 * var278.width() * var5.config().streamDepth()));
                           int var68 = Math.min(var261, var278.waterSurfaceY());
                           int var69 = var68 - (int)Math.round(var300 * (1.0 - var287 * var287));
                           int var269 = Math.max(var10, Math.min(var208, var69));
                           if (c(var193, var201)) {
                              b(var193, var201, "LAKE-DIG ground=" + var208 + " digTo=" + var269 + " bowlRef=" + var68 + " centerDepth=" + var300);
                           }

                           for (int var70 = var208; var70 > var269; var70--) {
                              var20.set(var193, var70, var201);
                              var4.setBlock(var20, var14, 2);
                           }

                           if (var269 < var208) {
                              a(var4, var20, var5, var193, var201, var261, var269, var208, var10, var23[var234]);
                           }
                        }

                        for (int var288 = var261; var288 > var208 && var288 > var10; var288--) {
                           var20.set(var193, var288, var201);
                           a(var4, (BlockPos)var20, var14);
                        }

                        b(var4, var20, var193, var261, var201, var14);
                        c(var4, var20, var193, var201, Math.max(var208 + 1, var261 + 1), var11);
                        var21 = true;
                        if (!var5.plan(var193, var201).inCorridor()) {
                           if (c(var193, var201)) {
                              b(var193, var201, "LAKE-STOP not in any corridor -> lake fill only");
                           }
                           continue;
                        }

                        var220 = true;
                     }

                     if (var4.getBlockState(var20.set(var193, var261, var201)).isAir()) {
                        int var270 = Integer.MIN_VALUE;

                        for (int var279 = var261 - 1; var279 >= Math.max(var10, var261 - 24); var279--) {
                           var20.set(var193, var279, var201);
                           if (!var4.getBlockState(var20).isAir() && var4.getBlockState(var20).getFluidState().isEmpty()) {
                              var270 = var279;
                              break;
                           }
                        }

                        if (var270 != Integer.MIN_VALUE) {
                           for (int var282 = var261; var282 > var270; var282--) {
                              var20.set(var193, var282, var201);
                              a(var4, (BlockPos)var20, var14);
                           }

                           var21 = true;
                           continue;
                        }
                     }
                  }

                  ColumnPlan var262 = var5.plan(var193, var201);
                  var112[var234] = var262;
                  if (c(var193, var201)) {
                     b(
                        var193,
                        var201,
                        String.format(
                           Locale.ROOT,
                           "plan corr=%s chan=%s dist=%.1f w=%.1f surf=%d term=%s src=%s ground=%d lake=%s samp=%.1f exact=%.1f%s",
                           var262.inCorridor(),
                           var262.inChannel(),
                           var262.distance(),
                           var262.width(),
                           var262.waterSurfaceY(),
                           var262.terminal(),
                           var262.source(),
                           var208,
                           var252 == null ? "-" : String.valueOf(var252.level()),
                           var5.sampledHeight(var193, var201),
                           var5.exactHeight(var193, var201),
                           var262.inCorridor() ? "" : " reject=[" + var5.planDiag(var193, var201) + "]"
                        )
                     );
                  }

                  if (!var262.inCorridor()) {
                     if (b(var193, var201)) {
                        a(var193, var201, "EXIT: not in any corridor -- the carve never touches this column");
                     }
                  } else {
                     if (b(var193, var201)) {
                        a(var193, var201, "=== STACK for (" + var193 + "," + var201 + ") ===");
                        a(
                           var193,
                           var201,
                           "  plan: chan="
                              + var262.inChannel()
                              + " dist="
                              + String.format(Locale.ROOT, "%.2f", var262.distance())
                              + " width="
                              + String.format(Locale.ROOT, "%.2f", var262.width())
                              + " grade="
                              + var262.waterSurfaceY()
                              + " ground="
                              + var208
                              + " terminal="
                              + var262.terminal()
                              + " source="
                              + var262.source()
                        );
                     }

                     var20.set(var193, var208 + 1, var201);
                     if (!var220 && !var4.getBlockState(var20).getFluidState().isEmpty()) {
                        int var271 = var208 + 1;

                        while (var271 + 1 <= var11 && !var4.getBlockState(var20.set(var193, var271 + 1, var201)).getFluidState().isEmpty()) {
                           var271++;
                        }

                        byte var280 = 6;
                        boolean var289 = a(var4, var20, var193, var201, var262, var11) != Integer.MIN_VALUE;
                        if (c(var193, var201)) {
                           b(
                              var193,
                              var201,
                              "FLUSH check fluidTop="
                                 + var271
                                 + " margin="
                                 + var280
                                 + " fullWidth="
                                 + var289
                                 + " chan="
                                 + var262.inChannel()
                                 + " skip="
                                 + (var262.terminal() || var262.inChannel() && var289 && var262.waterSurfaceY() <= var271 + var280)
                           );
                        }

                        boolean var294 = var262.inChannel() && a(var4, var5, var20, var193, var201, var262, var11, var177, var185, var132) != Integer.MIN_VALUE;
                        if (b(var193, var201)) {
                           a(var193, var201, "== FLUSH stage ==");
                           a(
                              var193,
                              var201,
                              "  fluidTop="
                                 + var271
                                 + " grade="
                                 + var262.waterSurfaceY()
                                 + " terminal="
                                 + var262.terminal()
                                 + " reachDropped="
                                 + var294
                                 + " -> "
                                 + (!var262.terminal() && !var294 ? "carve on" : "SKIP (nothing built)")
                           );
                        }

                        if (c(var193, var201)) {
                           b(
                              var193,
                              var201,
                              "FLUSH check fluidTop="
                                 + var271
                                 + " chan="
                                 + var262.inChannel()
                                 + " reachDropped="
                                 + var294
                                 + " skip="
                                 + (var262.terminal() || var294)
                           );
                        }

                        if (var262.terminal() || var294) {
                           if (var262.inChannel()) {
                              RiverCarverFeature.BankEnv var304 = a(var4, var193, var201, var271, var115, var5.config());
                              double var310 = a(var5, var193, var201);
                              if (a(var4, var20, var193, var201, var262, var5, var208, var271, var10, var11, var14, var304, var310, var4.getSeed(), var208)) {
                                 var21 = true;
                              }
                           }
                           continue;
                        }
                     }

                     var20.set(var193, var208, var201);
                     boolean var272 = var4.getBlockState(var20).is(Blocks.GRASS_BLOCK);
                     boolean var281 = var262.terminal()
                        && var262.inCorridor()
                        && var5.sinkWithin(var193 + 0.5, var201 + 0.5, 16.0 + var262.width() * (1.0 + var5.config().bankWidth()) + 3.0);
                     boolean var290 = var262.terminal()
                        && (var262.waterSurfaceY() <= var12 - 1 + 3 || var252 != null && var252.level() >= var262.waterSurfaceY() - 3 || var281);
                     var35[var234] = var5.config().containmentBerm()
                        && (var262.waterSurfaceY() >= var12 || var262.distToOutlet() > 24.0)
                        && !var290
                        && !var262.source();
                     int var295 = var262.waterSurfaceY();
                     if (var252 == null
                        && !var220
                        && var5.config().containmentBerm()
                        && !var290
                        && !var262.source()
                        && var208 < var295
                        && var208 >= var295 - 4
                        && var262.distance() <= var262.width()
                        && a(var4, var193, var201, var262, var295)
                        && b(var4, var193, var201, var208, var295, var10)) {
                        int var303 = Math.min(var11, var295);
                        if (c(var193, var201)) {
                           b(var193, var201, "POOL surface=" + var303 + " ground=" + var208);
                        }

                        for (int var309 = var303; var309 > var208 && var309 > var10; var309--) {
                           var20.set(var193, var309, var201);
                           a(var4, (BlockPos)var20, var14);
                        }

                        c(var4, var20, var193, var201, Math.max(var208 + 1, var303 + 1), var11);
                        var21 = true;
                     } else if (var262.inChannel()) {
                        boolean var302 = var5.config().containmentBerm() && !var290 && !var220;
                        int var308 = var262.waterSurfaceY();
                        int var314 = Integer.MAX_VALUE;
                        int var317 = var302 ? a(var4, var193, var201, var262, var208, var10, var11) : Math.min(Math.min(var308, var208), var314);
                        var317 = Mth.clamp(var317, var10, var11);
                        if (b(var193, var201)) {
                           a(var193, var201, "== CHANNEL surface derivation ==");
                           a(
                              var193,
                              var201,
                              "  plan grade="
                                 + var308
                                 + " ground="
                                 + var208
                                 + " dist="
                                 + String.format(Locale.ROOT, "%.2f", var262.distance())
                                 + " width="
                                 + String.format(Locale.ROOT, "%.2f", var262.width())
                                 + " contain="
                                 + var302
                                 + " slopeCap="
                                 + var314
                                 + " terminalOpen="
                                 + var290
                                 + " throughLake="
                                 + var220
                           );
                           a(var193, var201, "  base (containedWaterLevel): " + var317);
                        }

                        if (var262.source() && var208 - 1 > var10) {
                           var317 = a(var193, var201, "source recess", var317, Math.min(var317, var208 - 1));
                        }

                        int var321 = nearbyLakeLevel(var5, var193, var201, var10, var11);
                        int var323 = a(var5, var4, var262, var193, var201, var208, var321, var12, var10, var11);
                        if (var323 != Integer.MIN_VALUE) {
                           var317 = a(var193, var201, "lake level-out (" + var323 + ")", var317, Mth.clamp(Math.max(var317, var323), var10, var11));
                        }

                        if (var302 && var321 != Integer.MIN_VALUE) {
                           Lake var325 = var5.lakeNear(var193, var201, 1);
                           if (var325 != null && var325.digDepth() <= 0 && var325.level() <= var308 && var317 < var325.level()) {
                              var317 = a(var193, var201, "shore floor (" + var325.level() + ")", var317, Mth.clamp(var325.level(), var10, var11));
                           }
                        }

                        if (var252 != null && var208 < var252.level()) {
                           if (c(var193, var201)) {
                              b(var193, var201, "LAKE-FLUSH surface " + var317 + " -> " + var252.level());
                           }

                           var317 = a(var193, var201, "submerged in lake " + var252.level(), var317, Mth.clamp(var252.level(), var10, var11));
                        }

                        if (var243 != Integer.MIN_VALUE) {
                           var317 = a(var193, var201, "flood level (" + var243 + ")", var317, Mth.clamp(Math.max(var317, var243), var10, var11));
                        }

                        double var326 = Math.hypot(var262.flowDirX(), var262.flowDirZ());
                        if (var326 > 1.0E-6) {
                           double var328 = -var262.flowDirZ() / var326;
                           double var330 = var262.flowDirX() / var326;

                           for (int var335 = -1; var335 <= 1; var335 += 2) {
                              for (int var337 = 2; var337 <= 6; var337 += 2) {
                                 ColumnPlan var341 = var5.plan(
                                    var193 + (int)Math.round(var328 * var337 * var335), var201 + (int)Math.round(var330 * var337 * var335)
                                 );
                                 if (var341.inCorridor() && var341.inChannel() && var341.waterSurfaceY() < var317 && var317 - var341.waterSurfaceY() <= 4) {
                                    if (c(var193, var201)) {
                                       b(
                                          var193,
                                          var201,
                                          "JUNCTION-POOL surface "
                                             + var317
                                             + " -> "
                                             + var341.waterSurfaceY()
                                             + " (channel at r="
                                             + var337
                                             + " side="
                                             + var335
                                             + ")"
                                       );
                                    }

                                    var317 = a(
                                       var193, var201, "junction pool (neighbour at r=" + var337 + " side=" + var335 + ")", var317, var341.waterSurfaceY()
                                    );
                                 }
                              }
                           }
                        }

                        int var329 = a(var4, var5, var20, var193, var201, var262, var11, var177, var185, var132);
                        int var76 = var329 == Integer.MIN_VALUE ? var317 : Math.min(var317, var329);
                        if (var76 != var317 && c(var193, var201)) {
                           b(var193, var201, "GRADE-CLAMP " + var317 + " -> " + var76 + " (fell into water upstream)");
                        }

                        var317 = a(var193, var201, "drop clamp (inherited=" + var329 + ")", var317, var76);
                        if (var156[var234] > 0) {
                           int var331 = Math.max(var162[var234], var317 - var156[var234]);
                           if (var331 < var317) {
                              if (c(var193, var201)) {
                                 b(var193, var201, "LIP-SHAVE surface " + var317 + " -> " + var331 + " floor=" + var162[var234] + " shave=" + var156[var234]);
                              }

                              var317 = a(var193, var201, "lip shave", var317, var331);
                           }
                        }

                        RiverCarverFeature.BankEnv var332 = a(var4, var193, var201, var317, var115, var5.config());
                        double var334 = a(var4, var332, var20, var193, var208, var201);
                        double var338 = var302 && !var262.source() && var243 == Integer.MIN_VALUE ? a(var5, var193, var201) : 0.0;
                        int var344 = ChannelBed.bedDepth(
                           var262, var5.config(), var334, var332.wetness(), var332.cold(), var4.getSeed(), var193, var201, var317, var208, var338
                        );
                        int var346 = var344 <= 0 ? 1 - var344 : 0;
                        int var348 = var346 > 0 ? Math.min(var11, var317 - var344) : Mth.clamp(var317 - var344, var10, var317 - 1);
                        if (c(var193, var201) || b(var193, var201)) {
                           String var350 = ChannelBed.braidDiag(var262, var5.config(), var334, var332.wetness(), var332.cold(), var317, var208, var338)
                              + " | contain="
                              + var302
                              + " flood="
                              + (var243 != Integer.MIN_VALUE)
                              + " altChannel="
                              + (var5.altPlan(var193, var201) != null && var5.altPlan(var193, var201).inChannel())
                              + " depth="
                              + var344;
                           if (c(var193, var201)) {
                              b(var193, var201, var350);
                           }

                           if (b(var193, var201)) {
                              a(var193, var201, "  " + var350);
                           }
                        }

                        if (var346 > 0 && c(var193, var201)) {
                           b(
                              var193,
                              var201,
                              "BAR level="
                                 + var346
                                 + " bed="
                                 + var348
                                 + " surface="
                                 + var317
                                 + " lat="
                                 + String.format(Locale.ROOT, "%.2f", var262.lateral())
                                 + " coh="
                                 + String.format(Locale.ROOT, "%.2f", var334)
                           );
                        }

                        int var351 = Math.min(var11, Math.max(var208, var317));
                        var20.set(var193, var348, var201);
                        BlockState var353 = var4.getBlockState(var20);
                        boolean var355 = !var353.isAir() && var353.getFluidState().isEmpty();
                        var109[var234] = var317;
                        if (b(var193, var201)) {
                           a(var193, var201, "  FINAL channel surface=" + var317 + " bed=" + var348 + " supported=" + var355);
                        }

                        if (c(var193, var201)) {
                           b(
                              var193,
                              var201,
                              "CHANNEL surface="
                                 + var317
                                 + " bed="
                                 + var348
                                 + " top="
                                 + Math.min(var11, Math.max(var208, var317))
                                 + " supported="
                                 + var355
                                 + " contain="
                                 + var302
                           );
                        }

                        int var356 = var348 - var208 >= 4 ? a(var4, var20, var193, var201, var262, var11) : Integer.MIN_VALUE;
                        boolean var357 = var356 != Integer.MIN_VALUE
                           && (var348 - var208 >= 4 && !a(var4, var5, var193, var201, var262, var356) || var262.distToOutlet() <= 24.0);
                        if (var357 && c(var193, var201)) {
                           b(var193, var201, "CUP released: waterfall over water (ground=" + var208 + " waterTop=" + var356 + " bed=" + var348 + ")");
                        }

                        if (var302 && !var262.source() && !var357) {
                           int var358 = a(var193, var201, var317, var262, var5.config().bankWidth());
                           a(var4, var20, var193, var201, var358, var348, var208, var10);
                           var355 = true;
                        }

                        BlockState var359 = Services.PLATFORM.streamWaterState(a(var262));

                        for (int var360 = var351; var360 > var348; var360--) {
                           var20.set(var193, var360, var201);
                           if (var360 > var317) {
                              BlockState var362 = var4.getBlockState(var20);
                              if (!var362.isAir() && !var362.is(BlockTags.LEAVES)) {
                                 if (var362.is(BlockTags.LOGS)) {
                                    var4.setBlock(var20, var15, 2);
                                    a(var33, var34, var234, var360);
                                    b(var4, var20, var193, var201, var360 + 1, var11);
                                 } else if (var355 || var362.getFluidState().isEmpty()) {
                                    var4.setBlock(var20, var15, 2);
                                    a(var33, var34, var234, var360);
                                 }
                              }
                           } else if (var355) {
                              var4.setBlock(var20, var359, 2);
                           } else {
                              BlockState var363 = var4.getBlockState(var20);
                              if (!var363.isAir() && var363.getFluidState().isEmpty()) {
                                 var4.setBlock(var20, var15, 2);
                                 a(var33, var34, var234, var360);
                              }
                           }
                        }

                        if (var355 && b(var5, var193, var201, var317)) {
                           int var361 = 5 + Math.floorMod(Mth.getSeed(var193, 0, var201), 20);
                           var4.scheduleTick(new BlockPos(var193, var317, var201), var359.getFluidState().getType(), var361);
                        }

                        c(var4, var20, var193, var201, var208 + 1, var11);
                        if (var317 < var208) {
                           a(var4, var20, var193, var201, var317 + 1, var208 + 1, var12);
                        }

                        var21 = true;
                     } else {
                        int var301 = var262.waterSurfaceY();
                        if (b(var193, var201)) {
                           a(var193, var201, "== BERM derivation ==");
                           a(var193, var201, "  base waterLevel=" + var301 + " ground=" + var208);
                        }

                        int var307 = a(var5, var4, var262, var193, var201, var208, nearbyLakeLevel(var5, var193, var201, var10, var11), var12, var10, var11);
                        if (var307 != Integer.MIN_VALUE) {
                           var301 = a(var193, var201, "berm lake level-out (" + var307 + ")", var301, Math.max(var301, var307));
                        }

                        if (var252 != null && var252.level() >= var301) {
                           var301 = var252.level();
                        }

                        Object var313 = null;

                        for (int var315 = 0; var315 < 4; var315++) {
                           int var71 = var193 + (var315 == 0 ? 1 : (var315 == 1 ? -1 : 0));
                           int var72 = var201 + (var315 == 2 ? 1 : (var315 == 3 ? -1 : 0));
                           Lake var73 = var5.lakeWetting(var71, var72, 2);
                           if (var73 != null && var73.level() > var301) {
                              var301 = a(var193, var201, "adjacent lake fill (" + var73.level() + ")", var301, var73.level());
                           }
                        }

                        ColumnPlan var316 = var5.altPlan(var193, var201);
                        if (var316 != null && var316.waterSurfaceY() > var301) {
                           var301 = a(var193, var201, "alt stream water (" + var316.waterSurfaceY() + ")", var301, var316.waterSurfaceY());
                        }

                        var31[var177 * 16 + var185] = var301;
                        int var320 = a(var4, var5, var20, var193, var201, var262, var11, var177, var185, var132);
                        boolean var322 = var320 != Integer.MIN_VALUE;
                        if (var322) {
                           var301 = a(var193, var201, "berm drop clamp (" + var320 + ")", var301, Math.min(var301, var320));
                        }

                        var31[var177 * 16 + var185] = var301;
                        double var324 = Math.hypot(var262.flowDirX(), var262.flowDirZ());
                        if (var324 > 1.0E-6) {
                           ColumnPlan var75 = var5.plan(
                              var193 - (int)Math.round(var262.flowDirX() / var324), var201 - (int)Math.round(var262.flowDirZ() / var324)
                           );
                           if (var75 != ColumnPlan.NONE && var75.waterSurfaceY() - var301 <= 2) {
                              var301 = a(var193, var201, "upstream push (" + var75.waterSurfaceY() + ")", var301, Math.max(var301, var75.waterSurfaceY()));
                           }
                        }

                        double var327 = var262.upstreamSlope();
                        boolean var77 = false;
                        if (var327 >= 0.15) {
                           double var78 = Math.hypot(var262.flowDirX(), var262.flowDirZ());
                           if (var78 > 1.0E-6) {
                              int var80 = var193 - (int)Math.round(4.0 * var262.flowDirX() / var78);
                              int var81 = var201 - (int)Math.round(4.0 * var262.flowDirZ() / var78);
                              ColumnPlan var82 = var5.plan(var80, var81);
                              if (var82 != null && var82 != ColumnPlan.NONE) {
                                 double var83 = Math.hypot(var82.flowDirX(), var82.flowDirZ());
                                 if (var83 > 1.0E-6) {
                                    double var85 = (var262.flowDirX() * var82.flowDirX() + var262.flowDirZ() * var82.flowDirZ()) / (var78 * var83);
                                    var77 = var85 < 0.906;
                                 }
                              }
                           }
                        }

                        int var333 = var77 && var327 >= 0.15 ? 1 : 0;
                        if (var262.waterSurfaceY() < var12) {
                           var333 = Math.max(0, var333 - 1);
                        }

                        int var79 = var301 + var333;
                        var20.set(var193, var301, var201);
                        boolean var336 = var322 && (var208 >= var301 || f(var4, var20, var193, var201, var208, var11) != Integer.MIN_VALUE);
                        if (var5.config().containmentBerm()
                           && (var262.waterSurfaceY() >= var12 || var262.distToOutlet() > 24.0)
                           && var208 < var79
                           && !var290
                           && !var262.source()
                           && var32[var177 * 16 + var185] == Integer.MIN_VALUE
                           && !var336) {
                           int var339 = var333 > 0 ? 2 : 0;
                           double var342 = var262.distance() - var262.width();
                           double var84 = var342 - var339;
                           double var86 = Math.min(2.0, var5.config().bankSlope() + var5.config().bankSteepFactor() * Math.pow(var262.slope(), 0.8));
                           var86 = Math.min(
                              2.0,
                              var86
                                 * ChannelBed.bankSlopeFactor(
                                    var262, var5.config(), a(var4, a(var4, var193, var201, var301, var115, var5.config()), var20, var193, var208, var201)
                                 )
                           );
                           double var88 = 2.0 * var301 - var208;
                           double var90 = var301 + Math.max(0.0, var84) * var86 / 2.0;
                           double var92 = var301 + var262.bankBlend() * (var88 - var301);
                           double var94 = 0.0;
                           int var96 = Mth.clamp((int)Math.floor(Math.max(var90, var92) + var94), var301, (int)Math.max(var301, var88));
                           int var97 = 2 * var301 - var96 + var333;
                           if (var342 < var339) {
                              var97 = Math.min(var11, var301);
                           } else {
                              if (var84 <= Math.max(1.0, var262.width() / 3.0) * 2.0) {
                                 int var98 = var79 + (int)Math.round(Math.max(0.0, var94));
                                 var97 = Math.min(var11, Math.max(var97, var98));
                              }

                              int var367 = var333 * 3;
                              if (var367 > 0 && var84 <= var367) {
                                 int var99 = var79 - (int)Math.round(var84 / var367 * var333);
                                 var97 = Math.min(var11, Math.max(var97, var99));
                              }
                           }

                           if (var333 >= 2 && var342 >= var339) {
                              int var368 = var301 + 1 + (int)Math.floor(var342 - var339);
                              var97 = Math.min(var97, var368);
                           }

                           Lake var369 = null;

                           for (int[] var102 : p) {
                              Lake var103 = var5.lakeWetting(var193 + var102[0], var201 + var102[1], 1);
                              if (var103 != null && (var369 == null || var103.level() > var369.level())) {
                                 var369 = var103;
                              }
                           }

                           if (var369 != null && var369.level() > var97 && var208 < var369.level()) {
                              var97 = Math.min(var11, var369.level());
                           }

                           BlockState var379 = a(var4, var193, var201, var208, var10);
                           boolean var386 = false;
                           if (c(var193, var201)) {
                              int var393 = f(var4, var20, var193, var201, var208, var11);
                              var30[var177 * 16 + var185] = Math.max(var30[var177 * 16 + var185], var97);
                              if (b(var193, var201)) {
                                 a(var193, var201, "  BERM BUILT water=" + var301 + " freeboard=" + var333 + " top=" + var97);
                              }

                              b(
                                 var193,
                                 var201,
                                 "BERM water="
                                    + var301
                                    + " freeboard="
                                    + var333
                                    + " dropVerdict="
                                    + (var320 == Integer.MIN_VALUE ? "none" : String.valueOf(var320))
                                    + " top="
                                    + var97
                                    + " ground="
                                    + var208
                                    + " dto="
                                    + (var262.distToOutlet() > 999.0 ? "999" : (int)var262.distToOutlet())
                                    + " wtb="
                                    + (var393 == Integer.MIN_VALUE ? "dry" : String.valueOf(var393))
                              );
                           }

                           int var394 = f(var4, var20, var193, var201, var208, var11);
                           if (var394 != Integer.MIN_VALUE && var394 >= var301) {
                              if (c(var193, var201)) {
                                 b(
                                    var193,
                                    var201,
                                    "BERM skipped: submerged to " + var394 + " >= containment water " + var301 + " (crest would be " + var97 + ")"
                                 );
                              }
                              continue;
                           }

                           var30[var177 * 16 + var185] = Math.max(var30[var177 * 16 + var185], var97);
                           int var399 = Math.max(var10, a(var193, var201, var301, var262, var5.config().bankWidth(), 4));

                           for (int var403 = var399; var403 <= var97 && var403 <= var11; var403++) {
                              var20.set(var193, var403, var201);
                              BlockState var104 = var4.getBlockState(var20);
                              if (a(var4, var20, var104)) {
                                 BlockState var105 = a(var4, var193, var201, var208, var97 - var403, var379, var10);
                                 a(var4, var20, var193, var403, var201, var105, var10);
                                 var386 = true;
                              }
                           }

                           if (var386) {
                              var21 = true;
                              continue;
                           }
                        } else if (c(var193, var201) && var5.config().containmentBerm() && var208 < var79 && !var290 && !var262.source()) {
                           if (b(var193, var201)) {
                              a(var193, var201, "  BERM SKIPPED (gates below)");
                           }

                           b(
                              var193,
                              var201,
                              "BERM skipped: waterLevel="
                                 + var301
                                 + " dropped="
                                 + var322
                                 + " mouthGate="
                                 + (var262.waterSurfaceY() < var12 && !(var262.distToOutlet() > 24.0))
                                 + " lakeFill="
                                 + (var32[var177 * 16 + var185] != Integer.MIN_VALUE)
                                 + " terminalOpen="
                                 + var290
                                 + " src="
                                 + var262.source()
                           );
                        }

                        int var340 = Integer.MAX_VALUE;
                        int var343 = Math.min(Math.min(var262.waterSurfaceY(), var208), var340);
                        int var345 = nearbyLakeLevel(var5, var193, var201, var10, var11);
                        int var347 = a(var5, var4, var262, var193, var201, var208, var345, var12, var10, var11);
                        if (var347 != Integer.MIN_VALUE) {
                           var343 = Math.max(var343, var347);
                        }

                        if (var252 != null && var252.level() >= var343 && var208 < var252.level()) {
                           if (c(var193, var201)) {
                              b(var193, var201, "BANK-LAKE-FLUSH surface " + var343 + " -> " + var252.level());
                           }

                           var343 = var252.level();
                        }

                        if (var243 != Integer.MIN_VALUE) {
                           var343 = Math.max(var343, var243);
                        }

                        double var349 = var262.distance() - var262.width();
                        double var87 = Math.min(2.0, var5.config().bankSlope() + var5.config().bankSteepFactor() * Math.pow(var262.slope(), 0.8));
                        var87 = Math.min(
                           2.0,
                           var87
                              * ChannelBed.bankSlopeFactor(
                                 var262, var5.config(), a(var4, a(var4, var193, var201, var343, var115, var5.config()), var20, var193, var208, var201)
                              )
                        );
                        double var89 = a(var343, var349, var87, var262.width(), Math.max(5.0, var262.width() * var5.config().bankWidth()), var208);
                        double var91 = var343 + var262.bankBlend() * (var208 - var343);
                        double var93 = (d(var193, var201) - 0.5) * var5.config().bankNoise();
                        int var95 = Mth.clamp((int)Math.floor(Math.max(var89, var91) + var93), var343, Math.max(var343, var208));
                        ColumnPlan var364 = var5.altPlan(var193, var201);
                        if (var364 != null) {
                           int var365 = a(var5, var364, var208, var93);
                           if (var365 != Integer.MAX_VALUE) {
                              int var370 = a(var262, var364);
                              int var380 = Math.max(var365, var370);
                              if (var380 < var95) {
                                 if (c(var193, var201)) {
                                    b(
                                       var193,
                                       var201,
                                       "ALT-MERGE was="
                                          + var95
                                          + " now="
                                          + var380
                                          + " altCut="
                                          + var365
                                          + " floor="
                                          + var370
                                          + " altW="
                                          + String.format(Locale.ROOT, "%.1f", var364.width())
                                          + " altD="
                                          + String.format(Locale.ROOT, "%.1f", var364.distance())
                                          + " altSurf="
                                          + var364.waterSurfaceY()
                                    );
                                 }

                                 var95 = var380;
                              }
                           }
                        }

                        int var366 = Integer.MAX_VALUE;
                        if (var95 >= var208) {
                           for (int var371 = 1; var371 <= 2; var371++) {
                              for (int var381 = 0; var381 < 4; var381++) {
                                 int var387 = var193 + (var381 == 0 ? var371 : (var381 == 1 ? -var371 : 0));
                                 int var395 = var201 + (var381 == 2 ? var371 : (var381 == 3 ? -var371 : 0));
                                 int var400 = var387 - var8 + 2;
                                 int var404 = var395 - var9 + 2;
                                 if (var400 >= 0 && var400 < 20 && var404 >= 0 && var404 < 20) {
                                    int var406 = var400 * 20 + var404;
                                    if (var140[var406] != Integer.MIN_VALUE && !(Math.abs(var145[var406] - var262.distance()) > 2.0)) {
                                       var366 = Math.min(var366, var140[var406] + 1 * (var371 - 1));
                                    }
                                 }
                              }
                           }
                        }

                        if (var366 != Integer.MAX_VALUE && var95 > var366 + 1 + 1) {
                           int var372 = Mth.clamp(var366 + 1, var343, Math.max(var343, var208));
                           if (c(var193, var201)) {
                              b(
                                 var193,
                                 var201,
                                 "BANK-TIE was=" + var95 + " now=" + var372 + " neighbourAllow=" + var366 + " surf=" + var343 + " ground=" + var208
                              );
                           }

                           var95 = var372;
                        }

                        if (var95 < var208) {
                           int var373 = Integer.MAX_VALUE;
                           int var382 = 0;

                           for (int var388 = 0; var388 < 4; var388++) {
                              int var396 = var193 + (var388 == 0 ? 1 : (var388 == 1 ? -1 : 0));
                              int var401 = var201 + (var388 == 2 ? 1 : (var388 == 3 ? -1 : 0));
                              int var405 = var396 - var8 + 2;
                              int var407 = var401 - var9 + 2;
                              if (var405 >= 0 && var405 < 20 && var407 >= 0 && var407 < 20) {
                                 int var408 = var140[var405 * 20 + var407];
                                 if (var408 != Integer.MIN_VALUE) {
                                    var382++;
                                    var373 = Math.min(var373, var408);
                                 }
                              }
                           }

                           if (var382 >= 2 && var95 < var373) {
                              int var389 = Math.min(Math.min(var373, var208), var95 + 2);
                              if (var389 > var95) {
                                 if (c(var193, var201)) {
                                    b(
                                       var193,
                                       var201,
                                       "BANK-DIMPLE was=" + var95 + " now=" + var389 + " lowestBeside=" + var373 + " beside=" + var382 + " ground=" + var208
                                    );
                                 }

                                 var95 = var389;
                              }
                           }
                        }

                        if (var345 != Integer.MIN_VALUE && var252 == null && var95 < var208 && var262.distToOutlet() > 24.0) {
                           int var374 = adjacentCoveringLakeLevel(var5, var193, var201);
                           if (var374 != Integer.MIN_VALUE && var208 > var374 && var95 <= var374 && var262.waterSurfaceY() < var374) {
                              int var383 = Mth.clamp(var374 + 1, var95, var208);
                              if (c(var193, var201)) {
                                 b(
                                    var193,
                                    var201,
                                    "LAKE-RIM-HOLD was="
                                       + var95
                                       + " now="
                                       + var383
                                       + " rim="
                                       + var374
                                       + " streamSurf="
                                       + var262.waterSurfaceY()
                                       + " ground="
                                       + var208
                                       + " distToOutlet="
                                       + String.format(Locale.ROOT, "%.1f", var262.distToOutlet())
                                 );
                              }

                              var95 = var383;
                           }
                        }

                        if (!Boolean.getBoolean("streamsreflowing.noedgegrade")
                           && var345 == Integer.MIN_VALUE
                           && var95 >= var208
                           && var208 > var343
                           && var349 <= 8.0) {
                           double var375 = Math.max(0.6, var87);
                           int var390 = var343 + (int)Math.round(Math.max(0.0, var349) * var375);
                           int var397 = var95;
                           var95 = Mth.clamp(var390, var343, var208);
                           if (var95 != var397 && c(var193, var201)) {
                              b(
                                 var193,
                                 var201,
                                 "EDGE-GRADE was="
                                    + var397
                                    + " now="
                                    + var95
                                    + " graded="
                                    + var390
                                    + " ground="
                                    + var208
                                    + " surf="
                                    + var343
                                    + String.format(Locale.ROOT, " d=%.1f gradeSlope=%.2f", var349, var375)
                              );
                           }
                        }

                        if (var156[var234] > 0 && var95 > var343) {
                           int var376 = Math.max(Math.max(var343, var162[var234]), var95 - var156[var234]);
                           if (var376 < var95) {
                              if (c(var193, var201)) {
                                 b(var193, var201, "LIP-SHAVE cutTop " + var95 + " -> " + var376 + " floor=" + var162[var234] + " shave=" + var156[var234]);
                              }

                              var95 = var376;
                           }
                        }

                        if (c(var193, var201)) {
                           var30[var177 * 16 + var185] = Math.max(var30[var177 * 16 + var185], Math.max(var95, var208));
                           b(
                              var193,
                              var201,
                              "BANK ref="
                                 + var343
                                 + " cutTop="
                                 + var95
                                 + " ground="
                                 + var208
                                 + " d="
                                 + String.format(Locale.ROOT, "%.1f", var349)
                                 + " cut="
                                 + (var208 > var95)
                                 + String.format(
                                    Locale.ROOT,
                                    " slope=%.3f effSlope=%.2f slopeLine=%.2f terrainLine=%.2f blend=%.3f jitter=%+.2f",
                                    var262.slope(),
                                    var87,
                                    var89,
                                    var91,
                                    var262.bankBlend(),
                                    var93
                                 )
                           );
                        }

                        int var377 = var208;
                        if (var208 > var95) {
                           for (int var384 = var208; var384 > var95; var384--) {
                              var20.set(var193, var384, var201);
                              BlockState var391 = var4.getBlockState(var20);
                              if (!var391.isAir() && !var391.is(BlockTags.LEAVES)) {
                                 if (var391.is(BlockTags.LOGS)) {
                                    var4.setBlock(var20, var15, 2);
                                    a(var33, var34, var234, var384);
                                    b(var4, var20, var193, var201, var384 + 1, var11);
                                    var21 = true;
                                 } else {
                                    var4.setBlock(var20, var15, 2);
                                    a(var33, var34, var234, var384);
                                    var21 = true;
                                 }
                              }
                           }

                           var377 = var95;
                           var108[var177 * 16 + var185] = var208 - var95;
                        }

                        if (c(var4, var20, var193, var201, var208 + 1, var11)) {
                           var21 = true;
                        }

                        if (var377 < var208 && d(var4, var20, var193, var201, var377 + 1, var208)) {
                           var21 = true;
                        }

                        if (var377 < var208) {
                           a(var4, var20, var193, var201, var377 + 1, var208 + 1, var12);
                        }

                        if (var272 && var377 >= var10 && var377 < var11) {
                           BlockPos var385 = new BlockPos(var193, var377, var201);
                           BlockState var392 = var4.getBlockState(var385);
                           if (var392.is(Blocks.DIRT) || var392.is(Blocks.COARSE_DIRT) || var392.is(Blocks.GRASS_BLOCK)) {
                              var4.setBlock(var385, var16, 2);
                              BlockPos var398 = var385.above();
                              if (var4.getBlockState(var398).isAir()) {
                                 float var402 = var6.nextFloat();
                                 if (var402 < 0.2F && var398.getY() + 1 < var11 && var4.getBlockState(var398.above()).isAir()) {
                                    var4.setBlock(var398, var18, 2);
                                    var4.setBlock(var398.above(), var19, 2);
                                 } else if (var402 < 0.55F) {
                                    var4.setBlock(var398, var17, 2);
                                 }
                              }

                              var21 = true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      CarveCensus.add(2, u.get()[0]);
      long var178 = System.nanoTime();

      for (int var194 = 0; var194 < 16; var194++) {
         for (int var202 = 0; var202 < 16; var202++) {
            int var209 = var194 * 16 + var202;
            int var216 = var30[var209];
            int var222 = var31[var209];
            if (var216 != Integer.MIN_VALUE && var222 != Integer.MIN_VALUE && var216 <= var222) {
               int var228 = 0;
               int[] var235 = new int[24];

               for (int var244 = -2; var244 <= 2; var244++) {
                  for (int var253 = -2; var253 <= 2; var253++) {
                     if (var244 != 0 || var253 != 0) {
                        int var263 = var194 + var244;
                        int var274 = var202 + var253;
                        if (var263 >= 0 && var263 <= 15 && var274 >= 0 && var274 <= 15) {
                           int var284 = var30[var263 * 16 + var274];
                           if (var284 != Integer.MIN_VALUE) {
                              var235[var228++] = var284;
                           }
                        }
                     }
                  }
               }

               if (var228 >= 4) {
                  Arrays.sort(var235, 0, var228);
                  int var245 = var235[var228 / 2];
                  int var254 = Math.min(var245, var222 + 1);
                  if (var254 - var216 >= 2) {
                     int var264 = var8 + var194;
                     int var275 = var9 + var202;
                     int var285 = var22[var209];
                     BlockState var292 = a(var4, var264, var275, var285, var10);
                     boolean var298 = false;

                     for (int var306 = var216 + 1; var306 <= var254 && var306 <= var11; var306++) {
                        var20.set(var264, var306, var275);
                        BlockState var312 = var4.getBlockState(var20);
                        if (a(var4, var20, var312)) {
                           if (!e(var4, var20, var264, var306, var275, var10)) {
                              break;
                           }

                           a(var4, var20, var264, var306, var275, a(var4, var264, var275, var285, Math.max(1, var254 - var306), var292, var10), var10);
                           var298 = true;
                        }
                     }

                     if (var298) {
                        var21 = true;
                        if (c(var264, var275)) {
                           b(var264, var275, "DIP FILL contain " + var216 + " -> " + var254 + " (median " + var245 + ", holds " + var222 + ")");
                        }

                        if (b(var264, var275)) {
                           a(var264, var275, "  DIP FILL PASS contain " + var216 + " -> " + var254);
                        }
                     }
                  }
               }
            }
         }
      }

      for (int var195 = 0; var195 < 16; var195++) {
         for (int var203 = 0; var203 < 16; var203++) {
            int var210 = var109[var195 * 16 + var203];
            if (var210 != Integer.MIN_VALUE) {
               int var217 = 0;
               int[] var223 = new int[24];

               for (int var229 = -2; var229 <= 2; var229++) {
                  for (int var236 = -2; var236 <= 2; var236++) {
                     if (var229 != 0 || var236 != 0) {
                        int var246 = var195 + var229;
                        int var255 = var203 + var236;
                        if (var246 >= 0 && var246 <= 15 && var255 >= 0 && var255 <= 15) {
                           int var265 = var109[var246 * 16 + var255];
                           if (var265 != Integer.MIN_VALUE) {
                              var223[var217++] = var265;
                           }
                        }
                     }
                  }
               }

               if (var217 >= 4) {
                  Arrays.sort(var223, 0, var217);
                  int var230 = var223[var217 / 2];
                  if (var210 - var230 >= 2) {
                     int var237 = var8 + var195;
                     int var247 = var9 + var203;
                     if (c(var237, var247)) {
                        b(var237, var247, "OUTLIER surface " + var210 + " vs median " + var230 + " -> re-levelled");
                     }

                     if (b(var237, var247)) {
                        a(var237, var247, "  OUTLIER PASS re-levelled " + var210 + " -> " + var230);
                     }

                     for (int var256 = var210; var256 > var230; var256--) {
                        var20.set(var237, var256, var247);
                        BlockState var266 = var4.getBlockState(var20);
                        if (!var266.isAir() && (!var266.getFluidState().isEmpty() || var256 > var230 + 1)) {
                           var4.setBlock(var20, var15, 2);
                           a(var33, var34, var195 * 16 + var203, var256);
                        }
                     }

                     var20.set(var237, var230, var247);
                     if (var4.getBlockState(var20).isAir() && e(var4, var20, var237, var230, var247, var10)) {
                        var4.setBlock(var20, var14, 2);
                     }

                     var21 = true;
                  }
               }
            }
         }
      }

      CarveCensus.add(3, var178);
      if (var21) {
         CarveCensus.touched();
         long var196 = System.nanoTime();
         a(var4, var5, var8, var9, var22, var112, var11);
         CarveCensus.add(4, var196);
         long var211 = System.nanoTime();
         a(var4, var5, var8, var9, var22, var26, var23, var24, var108, var112, var10, var11);
         CarveCensus.add(5, var211);
         a(var4, var5, var20, var8, var9, var33, var34, var35, var109, var32, var25, var23, var118, var10, var11);
         b(var4, var8, var9, var108);
         a(var4, var8, var9, var23, var24);
         if (var13) {
            long var224 = System.nanoTime();
            a(var4, var5, var8, var9, var22, var25, var26, var23, var24, var112, var150, var10, var11);
            CarveCensus.add(6, var224);
         }
      }

      long var197 = System.nanoTime();
      a(var4, var20, var32, var8, var9, var10, var14);
      CarveCensus.add(7, var197);
      if (var21 && !Boolean.getBoolean("streamsreflowing.nobakeflow")) {
         try {
            ServerLevel var212 = var4.getLevel();
            StreamPrefetcher.submitGridBuild(
               var212,
               var5,
               RiverFlowFields.classifierFor(var212),
               RiverFlowFields.orientationFor(var212),
               Services.PLATFORM.flowVanillaRivers(),
               var8 >> 4,
               var9 >> 4,
               false,
               null
            );
         } catch (RuntimeException var106) {
         }
      }

      long var213 = System.nanoTime();
      ChunkPlanCache.put(var5, var8, var9, var112);
      a(var8, var9, var112);
      CarveCensus.add(8, var213);
      CarveCensus.chunkDone();
      return var21;
   }

   private static void a(int var0, int var1, ColumnPlan[] var2) {
      try {
         byte[] var3 = new byte[256];
         byte[] var4 = new byte[256];
         short[] var5 = new short[256];
         short[] var6 = new short[256];
         Arrays.fill(var5, (short)-32768);
         Arrays.fill(var6, (short)-32768);

         for (int var7 = 0; var7 < var2.length && var7 < var3.length; var7++) {
            ColumnPlan var8 = var2[var7];
            if (var8 != null && StreamFlowGrid.carriesFlow(var8)) {
               double var9 = var8.fluidFlowDirX();
               double var11 = var8.fluidFlowDirZ();
               double var13 = Math.sqrt(var9 * var9 + var11 * var11);
               if (!(var13 < 0.5)) {
                  double var15 = 127.0 * (var8.speed() / 2.0) / var13;
                  var3[var7] = (byte)Math.round(var9 * var15);
                  var4[var7] = (byte)Math.round(var11 * var15);
                  int var17 = var8.waterSurfaceY();
                  var5[var7] = var17 >= -32768 && var17 <= 32767 ? (short)var17 : -32768;
                  var6[var7] = (short)Math.max(-32767, Math.min(32767, var8.bedY()));
               }
            }
         }

         RawFlowTiles.put(var0 >> 4, var1 >> 4, new RawFlowTiles.Tile(var3, var4, var5, var6));
         FlowTrace.tilesFromCarve.incrementAndGet();
      } catch (RuntimeException var18) {
      }
   }

   private static boolean a(WorldGenLevel var0, int var1, int var2, int[] var3) {
      byte var4 = 18;
      int[] var5 = new int[324];

      for (int var6 = -1; var6 <= 16; var6++) {
         for (int var7 = -1; var7 <= 16; var7++) {
            var5[(var6 + 1) * 18 + var7 + 1] = a(var0, var1 + var6, var2 + var7);
         }
      }

      for (int var15 = 0; var15 < 2; var15++) {
         for (int var16 = -1; var16 <= 15; var16++) {
            int var8 = 0;

            for (int var9 = 0; var9 < 16; var9++) {
               int var10 = var15 == 0 ? var9 : var16;
               int var11 = var15 == 0 ? var16 : var9;
               int var12 = var15 == 0 ? var9 : var16 + 1;
               int var13 = var15 == 0 ? var16 + 1 : var9;
               boolean var14 = (a(var3, var10, var11) || a(var3, var12, var13))
                  && Math.abs(var5[(var10 + 1) * 18 + var11 + 1] - var5[(var12 + 1) * 18 + var13 + 1]) >= 3;
               var8 = var14 ? var8 + 1 : 0;
               if (var8 >= 3) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean a(int[] var0, int var1, int var2) {
      return var1 >= 0 && var1 < 16 && var2 >= 0 && var2 < 16 && var0[var1 * 16 + var2] > 0;
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      int var2,
      int var3,
      int[] var4,
      boolean[] var5,
      BlockState[] var6,
      BlockState[] var7,
      int[] var8,
      ColumnPlan[] var9,
      int var10,
      int var11
   ) {
      if (!Boolean.getBoolean("streamsreflowing.nobanksmooth")) {
         if (!Boolean.getBoolean("streamsreflowing.alwaysbanksmooth") && !a(var0, var2, var3, var8)) {
            if (c(var2, var3)) {
               b(var2, var3, "SEAM-SKIP no wall at a border, a carve boundary or a stream drop -> blend not run");
            }
         } else {
            int var12 = Math.max(1, Integer.getInteger("streamsreflowing.banksmoothstep", 2));
            int var13 = Math.max(0, Integer.getInteger("streamsreflowing.banksmoothmincut", 4));
            byte var14 = 20;
            int[] var15 = new int[400];
            boolean[] var16 = new boolean[400];
            boolean[] var17 = new boolean[400];
            boolean[] var18 = new boolean[400];
            boolean[] var19 = new boolean[400];
            boolean[] var20 = new boolean[400];
            Arrays.fill(var15, var10);

            for (int var21 = -6; var21 < 22; var21++) {
               for (int var22 = -6; var22 < 22; var22++) {
                  boolean var23;
                  if (var21 >= 0 && var21 < 16 && var22 >= 0 && var22 < 16) {
                     ColumnPlan var24 = var9[var21 * 16 + var22];
                     var23 = var24 != null && var24 != ColumnPlan.NONE && var24.inCorridor();
                  } else {
                     var23 = var1.inCorridor(var2 + var21, var3 + var22);
                  }

                  if (var23) {
                     for (int var65 = -6; var65 <= 6; var65++) {
                        for (int var25 = -6; var25 <= 6; var25++) {
                           int var26 = var21 + var65;
                           int var27 = var22 + var25;
                           if (var26 >= -2 && var26 < 18 && var27 >= -2 && var27 < 18) {
                              var18[(var26 + 2) * 20 + var27 + 2] = true;
                           }
                        }
                     }
                  }
               }
            }

            MutableBlockPos var60 = new MutableBlockPos();

            for (int var61 = 0; var61 < 16; var61++) {
               for (int var63 = 0; var63 < 16; var63++) {
                  int var66 = (var61 + 2) * 20 + var63 + 2;
                  var19[var66] = true;
                  ColumnPlan var68 = var9[var61 * 16 + var63];
                  Lake var70 = b(var1, var2 + var61, var3 + var63);
                  if (var68 != null && var68 != ColumnPlan.NONE && var68.inChannel()) {
                     var15[var66] = var68.waterSurfaceY();
                     var16[var66] = true;
                     var17[var66] = true;
                  } else if (var70 != null) {
                     var15[var66] = var70.level();
                     var16[var66] = true;
                     var17[var66] = true;
                  } else {
                     int var74 = var0.getHeight(Types.OCEAN_FLOOR, var2 + var61, var3 + var63);
                     int var28 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var2 + var61, var3 + var63);
                     int var29 = Math.min(var74, var28) - 1;
                     BlockState var30 = var0.getBlockState(var60.set(var2 + var61, var29, var3 + var63));
                     if (var30.is(BlockTags.LOGS) || var30.is(BlockTags.LEAVES)) {
                        int var31 = var29;

                        for (int var32 = Math.max(var10, var29 - 48); var31 > var32; var31--) {
                           BlockState var33 = var0.getBlockState(var60.set(var2 + var61, var31, var3 + var63));
                           if (!var33.is(BlockTags.LOGS) && !var33.is(BlockTags.LEAVES)) {
                              break;
                           }
                        }

                        var29 = var31;
                        var16[var66] = true;
                     }

                     var15[var66] = var29;
                  }
               }
            }

            boolean var62 = false;
            boolean var64 = false;
            boolean var67 = false;
            boolean var69 = false;

            for (int var71 = 0; var71 < 16; var71++) {
               var62 |= var18[40 + var71 + 2];
               var64 |= var18[340 + var71 + 2];
               var67 |= var18[(var71 + 2) * 20 + 2];
               var69 |= var18[(var71 + 2) * 20 + 17];
            }

            for (int var72 = -2; var72 <= 17; var72++) {
               for (int var75 = -2; var75 <= -1; var75++) {
                  if (var62) {
                     a(var0, var1, var2, var3, var75, var72, 20, var15, var17, var19, var20, var10);
                  }

                  if (var67) {
                     a(var0, var1, var2, var3, var72, var75, 20, var15, var17, var19, var20, var10);
                  }
               }

               for (int var76 = 16; var76 <= 17; var76++) {
                  if (var64) {
                     a(var0, var1, var2, var3, var76, var72, 20, var15, var17, var19, var20, var10);
                  }

                  if (var69) {
                     a(var0, var1, var2, var3, var72, var76, 20, var15, var17, var19, var20, var10);
                  }
               }
            }

            int[] var73 = (int[])var15.clone();
            MutableBlockPos var77 = new MutableBlockPos();
            int[] var78 = new int[400];
            int[] var79 = new int[400];

            for (int var80 = 0; var80 < 20; var80++) {
               for (int var84 = 0; var84 < 20; var84++) {
                  int var89 = var80 * 20 + var84;
                  if (var15[var89] == var10) {
                     var78[var89] = var10;
                  } else {
                     int var95 = var15[var89];
                     if (var80 > 0 && var15[var89 - 20] != var10) {
                        var95 = Math.min(var95, var15[var89 - 20]);
                     }

                     if (var80 < 19 && var15[var89 + 20] != var10) {
                        var95 = Math.min(var95, var15[var89 + 20]);
                     }

                     if (var84 > 0 && var15[var89 - 1] != var10) {
                        var95 = Math.min(var95, var15[var89 - 1]);
                     }

                     if (var84 < 19 && var15[var89 + 1] != var10) {
                        var95 = Math.min(var95, var15[var89 + 1]);
                     }

                     var78[var89] = var95;
                  }
               }
            }

            for (int var81 = 0; var81 < 20; var81++) {
               for (int var85 = 0; var85 < 20; var85++) {
                  int var90 = var81 * 20 + var85;
                  if (var15[var90] == var10) {
                     var79[var90] = var10;
                  } else {
                     int var96 = var78[var90];
                     if (var81 > 0 && var78[var90 - 20] != var10) {
                        var96 = Math.max(var96, var78[var90 - 20]);
                     }

                     if (var81 < 19 && var78[var90 + 20] != var10) {
                        var96 = Math.max(var96, var78[var90 + 20]);
                     }

                     if (var85 > 0 && var78[var90 - 1] != var10) {
                        var96 = Math.max(var96, var78[var90 - 1]);
                     }

                     if (var85 < 19 && var78[var90 + 1] != var10) {
                        var96 = Math.max(var96, var78[var90 + 1]);
                     }

                     var79[var90] = var96;
                  }
               }
            }

            int var82 = 0;
            int var86 = 0;

            for (int var91 = 0; var91 < 400; var91++) {
               int var97 = var91 / 20 - 2;
               int var34 = var91 % 20 - 2;
               if ((var97 < 0 || var97 >= 16 || var34 < 0 || var34 >= 16) && var18[var91]) {
                  var82++;
                  if (!var19[var91]) {
                     var86++;
                  }
               }
            }

            if (c(var2 + 8, var3 + 8)) {
               b(var2 + 8, var3 + 8, "SEAM-TRUST apronZoned=" + var82 + " untrusted=" + var86 + " (untrusted apron = a seam this side cannot close)");
            }

            boolean[] var83 = new boolean[400];
            MutableBlockPos var87 = new MutableBlockPos();

            for (int var92 = 0; var92 < var83.length; var92++) {
               if (var18[var92] && var15[var92] != var10) {
                  int var98 = var2 + var92 / 20 - 2;
                  int var101 = var3 + var92 % 20 - 2;
                  BlockState var35 = var0.getBlockState(var87.set(var98, var15[var92], var101));
                  var83[var92] = var35.is(BlockTags.LOGS) || var35.is(BlockTags.LEAVES);
               }
            }

            int[] var88 = (int[])var15.clone();

            for (int var93 = 0; var93 < 32; var93++) {
               boolean var99 = false;

               for (int var102 = -2; var102 < 18; var102++) {
                  for (int var105 = -2; var105 < 18; var105++) {
                     int var36 = (var102 + 2) * 20 + var105 + 2;
                     if (var18[var36] && !var16[var36] && var19[var36] && var15[var36] != var10) {
                        boolean var37 = var102 >= 0 && var102 < 16 && var105 >= 0 && var105 < 16;
                        if (var37 && var8[var102 * 16 + var105] >= var13) {
                           int var38 = var102 + 2;
                           int var39 = var105 + 2;
                           int var40 = var2 + var102;
                           int var41 = var3 + var105;
                           long var42 = Math.floorMod(Mth.getSeed(var40, 0, var41), 6L);
                           int var44 = Math.min(6, var12 + var93);
                           int var45 = Math.max(1, var44 + (var42 == 0L ? -1 : (var42 == 5L ? 1 : 0)));
                           int var46 = var15[var36];
                           int var47 = Integer.MIN_VALUE;
                           if (var38 > 0 && !var83[var36 - 20]) {
                              var47 = Math.max(var47, var79[var36 - 20]);
                           }

                           if (var38 < 19 && !var83[var36 + 20]) {
                              var47 = Math.max(var47, var79[var36 + 20]);
                           }

                           if (var39 > 0 && !var83[var36 - 1]) {
                              var47 = Math.max(var47, var79[var36 - 1]);
                           }

                           if (var39 < 19 && !var83[var36 + 1]) {
                              var47 = Math.max(var47, var79[var36 + 1]);
                           }

                           if (var47 == Integer.MIN_VALUE || var47 - var15[var36] <= 16) {
                              int var48 = var47 - var45;
                              if (var38 > 0 && var17[var36 - 20]) {
                                 var48 = Math.min(var48, var15[var36 - 20] + 1);
                              }

                              if (var38 < 19 && var17[var36 + 20]) {
                                 var48 = Math.min(var48, var15[var36 + 20] + 1);
                              }

                              if (var39 > 0 && var17[var36 - 1]) {
                                 var48 = Math.min(var48, var15[var36 - 1] + 1);
                              }

                              if (var39 < 19 && var17[var36 + 1]) {
                                 var48 = Math.min(var48, var15[var36 + 1] + 1);
                              }

                              var48 = Math.min(var48, var73[var36]);
                              if (var15[var36] < var48 && !a(var0, var77, var40, var41, var15[var36] + 1, var48)) {
                                 int var49 = var15[var36];
                                 int var50 = var73[var36];
                                 BlockState var51 = a(var0, var40, var41, var50, var10);
                                 boolean var52 = var102 >= 0 && var102 < 16 && var105 >= 0 && var105 < 16;
                                 BlockState var53 = var52 ? null : a(var0, var40, var41, var15[var36]);
                                 int var54 = var52 ? var102 * 16 + var105 : -1;
                                 BlockState var55 = var54 >= 0 ? var6[var54] : null;
                                 BlockState var56 = var54 >= 0 ? var7[var54] : null;

                                 for (int var57 = var15[var36] + 1; var57 <= var48; var49 = var57++) {
                                    var77.set(var40, var57, var41);
                                    BlockState var58 = var0.getBlockState(var77);
                                    if (!var58.getFluidState().isEmpty()) {
                                       var16[var36] = true;
                                       break;
                                    }

                                    if (b(var0, var77, var40, var57, var41)) {
                                       var16[var36] = true;
                                       break;
                                    }

                                    var77.set(var40, var57, var41);
                                    if (!e(var0, var77, var40, var57, var41, var10)) {
                                       var16[var36] = true;
                                       break;
                                    }

                                    BlockState var59 = var57 == var48
                                       ? (
                                          var56 != null
                                             ? var56
                                             : (var53 != null ? var53 : a(var0, var40, var41, var48, var10, a(var0, var40, var41, var50, 0, var51, var10)))
                                       )
                                       : (var55 != null ? var55 : a(var0, var40, var41, var50, var48 - var57, var51, var10));
                                    a(var0, var77, var40, var57, var41, var59, var10);
                                    b(var0, var77, var40, var57 - 1, var41, var55 != null ? var55 : var51, var10);
                                 }

                                 if (var49 > var15[var36]) {
                                    if (c(var40, var41)) {
                                       b(
                                          var40,
                                          var41,
                                          "FILL iter="
                                             + var93
                                             + " from="
                                             + var46
                                             + " to="
                                             + var49
                                             + " target="
                                             + var48
                                             + " pulledBy="
                                             + var47
                                             + " step="
                                             + var45
                                             + " orig="
                                             + var73[var36]
                                       );
                                    }

                                    var15[var36] = var49;
                                    var79[var36] = Math.max(var79[var36], var49);
                                    if (var102 >= 0 && var102 < 16 && var105 >= 0 && var105 < 16) {
                                       var4[var102 * 16 + var105] = Mth.clamp(var49, var10, var11);
                                       var5[var102 * 16 + var105] = true;
                                    }

                                    var99 = true;
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (!var99) {
                  break;
               }
            }

            byte var94 = 3;
            byte var100 = 2;

            for (int var103 = 0; var103 < 2; var103++) {
               boolean var106 = false;

               for (int var108 = -2; var108 < 18; var108++) {
                  for (int var110 = -2; var110 < 18; var110++) {
                     int var113 = (var108 + 2) * 20 + var110 + 2;
                     if (var18[var113] && !var16[var113] && var19[var113] && var15[var113] != var10) {
                        boolean var117 = var108 >= 0 && var108 < 16 && var110 >= 0 && var110 < 16;
                        if (var117) {
                           boolean var120 = var8[var108 * 16 + var110] >= var13;

                           for (int var123 = -3; var123 <= 3 && !var120; var123++) {
                              if (var123 != 0) {
                                 int var129 = var108 + var123;
                                 if (var129 >= -2 && var129 < 18) {
                                    int var43 = (var129 + 2) * 20 + var110 + 2;
                                    var120 = var15[var43] != var10 && var15[var43] > var88[var43];
                                 }

                                 int var136 = var110 + var123;
                                 if (!var120 && var136 >= -2 && var136 < 18) {
                                    int var144 = (var108 + 2) * 20 + var136 + 2;
                                    var120 = var15[var144] != var10 && var15[var144] > var88[var144];
                                 }
                              }
                           }

                           if (var120) {
                              int var124 = Integer.MIN_VALUE;

                              for (int var130 = 0; var130 < 2; var130++) {
                                 int var137 = Integer.MIN_VALUE;
                                 int var145 = Integer.MIN_VALUE;

                                 for (int var152 = 1; var152 <= 3; var152++) {
                                    int var159 = var130 == 0 ? var108 + var152 : var108;
                                    int var167 = var130 == 0 ? var110 : var110 + var152;
                                    if (var159 > 17 || var167 > 17) {
                                       break;
                                    }

                                    int var175 = var15[(var159 + 2) * 20 + var167 + 2];
                                    if (var175 != var10 && var175 >= var15[var113] + 2) {
                                       var137 = var175;
                                       break;
                                    }
                                 }

                                 if (var137 != Integer.MIN_VALUE) {
                                    for (int var153 = 1; var153 <= 3; var153++) {
                                       int var160 = var130 == 0 ? var108 - var153 : var108;
                                       int var168 = var130 == 0 ? var110 : var110 - var153;
                                       if (var160 < -2 || var168 < -2) {
                                          break;
                                       }

                                       int var176 = var15[(var160 + 2) * 20 + var168 + 2];
                                       if (var176 != var10 && var176 >= var15[var113] + 2) {
                                          var145 = var176;
                                          break;
                                       }
                                    }

                                    if (var145 != Integer.MIN_VALUE) {
                                       var124 = Math.max(var124, Math.min(var137, var145));
                                    }
                                 }
                              }

                              if (var124 != Integer.MIN_VALUE) {
                                 int var131 = var108 + 2;
                                 int var138 = var110 + 2;
                                 if (var131 > 0 && var17[var113 - 20]) {
                                    var124 = Math.min(var124, var15[var113 - 20] + 1);
                                 }

                                 if (var131 < 19 && var17[var113 + 20]) {
                                    var124 = Math.min(var124, var15[var113 + 20] + 1);
                                 }

                                 if (var138 > 0 && var17[var113 - 1]) {
                                    var124 = Math.min(var124, var15[var113 - 1] + 1);
                                 }

                                 if (var138 < 19 && var17[var113 + 1]) {
                                    var124 = Math.min(var124, var15[var113 + 1] + 1);
                                 }

                                 var124 = Math.min(var124, var73[var113]);
                                 if (var15[var113] < var124) {
                                    int var146 = var2 + var108;
                                    int var154 = var3 + var110;
                                    if (a(var0, var77, var146, var154, var15[var113] + 1, var124)) {
                                       var16[var113] = true;
                                    } else {
                                       int var161 = var15[var113];
                                       int var169 = var73[var113];
                                       BlockState var177 = a(var0, var146, var154, var169, var10);
                                       boolean var182 = var108 >= 0 && var108 < 16 && var110 >= 0 && var110 < 16;
                                       BlockState var185 = var182 ? null : a(var0, var146, var154, var15[var113]);
                                       int var188 = var182 ? var108 * 16 + var110 : -1;
                                       BlockState var191 = var188 >= 0 ? var6[var188] : null;
                                       BlockState var194 = var188 >= 0 ? var7[var188] : null;

                                       for (int var197 = var15[var113] + 1; var197 <= var124; var161 = var197++) {
                                          var77.set(var146, var197, var154);
                                          BlockState var200 = var0.getBlockState(var77);
                                          if (!var200.getFluidState().isEmpty()) {
                                             var16[var113] = true;
                                             break;
                                          }

                                          if (b(var0, var77, var146, var197, var154)) {
                                             var16[var113] = true;
                                             break;
                                          }

                                          var77.set(var146, var197, var154);
                                          if (!e(var0, var77, var146, var197, var154, var10)) {
                                             var16[var113] = true;
                                             break;
                                          }

                                          BlockState var202 = var197 == var124
                                             ? (
                                                var194 != null
                                                   ? var194
                                                   : (
                                                      var185 != null
                                                         ? var185
                                                         : a(var0, var146, var154, var124, var10, a(var0, var146, var154, var169, 0, var177, var10))
                                                   )
                                             )
                                             : (var191 != null ? var191 : a(var0, var146, var154, var169, var124 - var197, var177, var10));
                                          a(var0, var77, var146, var197, var154, var202, var10);
                                          b(var0, var77, var146, var197 - 1, var154, var191 != null ? var191 : var177, var10);
                                       }

                                       if (var161 > var15[var113]) {
                                          var15[var113] = var161;
                                          if (var117) {
                                             var4[var108 * 16 + var110] = Mth.clamp(var161, var10, var11);
                                             var5[var108 * 16 + var110] = true;
                                          }

                                          var106 = true;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (!var106) {
                  break;
               }
            }

            byte var104 = 4;
            byte var107 = 2;
            int[] var109 = (int[])var15.clone();

            for (int var111 = 0; var111 < 16; var111++) {
               for (int var114 = 0; var114 < 16; var114++) {
                  int var118 = (var111 + 2) * 20 + var114 + 2;
                  if (var18[var118] && !var16[var118] && var19[var118] && var109[var118] != var10 && !var17[var118]) {
                     boolean var121 = var8[var111 * 16 + var114] >= var13;

                     for (int var126 = -3; var126 <= 3 && !var121; var126++) {
                        if (var126 != 0) {
                           int var132 = var111 + var126;
                           if (var132 >= 0 && var132 < 16) {
                              var121 = var8[var132 * 16 + var114] >= var13;
                              if (!var121) {
                                 int var139 = (var132 + 2) * 20 + var114 + 2;
                                 var121 = var109[var139] != var10 && var109[var139] > var88[var139];
                              }
                           }

                           int var140 = var114 + var126;
                           if (!var121 && var140 >= 0 && var140 < 16) {
                              var121 = var8[var111 * 16 + var140] >= var13;
                              if (!var121) {
                                 int var147 = (var111 + 2) * 20 + var140 + 2;
                                 var121 = var109[var147] != var10 && var109[var147] > var88[var147];
                              }
                           }
                        }
                     }

                     if (var121) {
                        int var127 = Integer.MIN_VALUE;

                        for (int var133 = 0; var133 < 2; var133++) {
                           int var141 = Integer.MIN_VALUE;
                           int var148 = Integer.MIN_VALUE;

                           for (int var155 = 1; var155 <= 2; var155++) {
                              int var162 = var133 == 0 ? var111 + var155 : var111;
                              int var170 = var133 == 0 ? var114 : var114 + var155;
                              if (var162 > 15 || var170 > 15) {
                                 break;
                              }

                              int var178 = var109[(var162 + 2) * 20 + var170 + 2];
                              if (var178 != var10 && var178 <= var109[var118] - 4) {
                                 var141 = var178;
                                 break;
                              }
                           }

                           if (var141 != Integer.MIN_VALUE) {
                              for (int var156 = 1; var156 <= 2; var156++) {
                                 int var163 = var133 == 0 ? var111 - var156 : var111;
                                 int var171 = var133 == 0 ? var114 : var114 - var156;
                                 if (var163 < 0 || var171 < 0) {
                                    break;
                                 }

                                 int var179 = var109[(var163 + 2) * 20 + var171 + 2];
                                 if (var179 != var10 && var179 <= var109[var118] - 4) {
                                    var148 = var179;
                                    break;
                                 }
                              }

                              if (var148 != Integer.MIN_VALUE) {
                                 var127 = Math.max(var127, Math.max(var141, var148) + 1);
                              }
                           }
                        }

                        if (var127 != Integer.MIN_VALUE && var127 < var109[var118]) {
                           int var134 = var2 + var111;
                           int var142 = var3 + var114;
                           if (a(var0, var77, var134, var142, var127 + 1, var109[var118] + 2)) {
                              var16[var118] = true;
                           } else {
                              boolean var149 = false;

                              for (int var157 = var109[var118]; var157 > var127; var157--) {
                                 BlockState var164 = var0.getBlockState(var77.set(var134, var157, var142));
                                 if (!var164.getFluidState().isEmpty()) {
                                    break;
                                 }

                                 if (!var164.isAir()) {
                                    var0.setBlock(var77, Blocks.AIR.defaultBlockState(), 2);
                                    var149 = true;
                                 }
                              }

                              if (var149) {
                                 if (c(var134, var142)) {
                                    b(var134, var142, "PILLAR-CULL was=" + var109[var118] + " now=" + var127);
                                 }

                                 c(var0, var77, var134, var142, var127 + 1, var11);
                                 var15[var118] = var127;
                                 var4[var111 * 16 + var114] = Mth.clamp(var127, var10, var11);
                                 var5[var111 * 16 + var114] = true;
                              }
                           }
                        }
                     }
                  }
               }
            }

            boolean[] var112 = new boolean[400];

            for (int var115 = 0; var115 < var112.length; var115++) {
               var112[var115] = var18[var115] && var19[var115] && !var16[var115] && var15[var115] != var10 && var15[var115] > var73[var115];
            }

            BlockState var116 = Blocks.AIR.defaultBlockState();
            boolean[] var119 = new boolean[400];
            int[] var122 = new int[400];
            int[] var128 = new int[400];

            for (int var135 = 0; var135 < var112.length; var135++) {
               if (var112[var135] && !var119[var135]) {
                  int var143 = 0;
                  int var150 = 0;
                  int var158 = 0;
                  var122[var150++] = var135;
                  var119[var135] = true;

                  while (var143 < var150) {
                     int var165 = var122[var143++];
                     var128[var158++] = var165;
                     int var172 = var165 / 20;
                     int var180 = var165 % 20;
                     if (var172 > 0 && var112[var165 - 20] && !var119[var165 - 20]) {
                        var119[var165 - 20] = true;
                        var122[var150++] = var165 - 20;
                     }

                     if (var172 < 19 && var112[var165 + 20] && !var119[var165 + 20]) {
                        var119[var165 + 20] = true;
                        var122[var150++] = var165 + 20;
                     }

                     if (var180 > 0 && var112[var165 - 1] && !var119[var165 - 1]) {
                        var119[var165 - 1] = true;
                        var122[var150++] = var165 - 1;
                     }

                     if (var180 < 19 && var112[var165 + 1] && !var119[var165 + 1]) {
                        var119[var165 + 1] = true;
                        var122[var150++] = var165 + 1;
                     }
                  }

                  int var166 = 0;
                  int var173 = 0;
                  int var181 = 0;

                  for (int var183 = 0; var183 < var158; var183++) {
                     int var186 = var128[var183];
                     var166 = Math.max(var166, var15[var186] - var73[var186]);
                     int var189 = var186 / 20;
                     int var192 = var186 % 20;

                     for (int var195 = 0; var195 < 4; var195++) {
                        int var198 = switch (var195) {
                           case 0 -> var189 > 0 ? var186 - 20 : -1;
                           case 1 -> var189 < 19 ? var186 + 20 : -1;
                           case 2 -> var192 > 0 ? var186 - 1 : -1;
                           default -> var192 < 19 ? var186 + 1 : -1;
                        };
                        if (var198 >= 0 && !var112[var198] && var15[var198] != var10) {
                           var173++;
                           if (var15[var198] >= var15[var186] - (var12 + 1)) {
                              var181++;
                           }
                        }
                     }
                  }

                  boolean var184 = var158 <= 24 && var166 > var12 && (var173 == 0 || var181 * 4 < var173);
                  if (var184) {
                     for (int var187 = 0; var187 < var158; var187++) {
                        int var190 = var128[var187];
                        int var193 = var190 / 20 - 2;
                        int var196 = var190 % 20 - 2;
                        boolean var199 = var193 >= 0 && var193 < 16 && var196 >= 0 && var196 < 16;
                        if (var199) {
                           int var201 = var2 + var193;
                           int var203 = var3 + var196;
                           int var204 = var15[var190];

                           for (int var205 = var204; var205 > var73[var190]; var205--) {
                              var77.set(var201, var205, var203);
                              BlockState var206 = var0.getBlockState(var77);
                              if (!var206.getFluidState().isEmpty()) {
                                 break;
                              }

                              if (!var206.isAir()) {
                                 var0.setBlock(var77, var116, 2);
                              }
                           }

                           var15[var190] = var73[var190];
                           if (var199) {
                              var4[var193 * 16 + var196] = Mth.clamp(var73[var190], var10, var11);
                              var5[var193 * 16 + var196] = true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static int a(WorldGenLevel var0, int var1, int var2, int[] var3, int var4, int var5) {
      int var6 = var4 - var1;
      int var7 = var5 - var2;
      return var6 >= 0 && var6 < 16 && var7 >= 0 && var7 < 16 ? var3[var6 * 16 + var7] : var0.getHeight(Types.OCEAN_FLOOR_WG, var4, var5) - 1;
   }

   private static Lake b(RiverEngine var0, int var1, int var2) {
      Lake var3 = var0.lakeWetting(var1, var2, 2);
      if (var3 != null) {
         return var3;
      }

      Lake var4 = var0.lakeAt(var1, var2);
      return var4 != null && var4.digDepth() > 0 ? var4 : null;
   }

   public static boolean inLakeWater(RiverEngine var0, int var1, int var2) {
      return b(var0, var1, var2) != null;
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int[] var7,
      boolean[] var8,
      boolean[] var9,
      boolean[] var10,
      int var11
   ) {
      int var12 = (var4 + 2) * var6 + var5 + 2;
      if (var7[var12] == var11) {
         int var13 = var2 + var4;
         int var14 = var3 + var5;

         try {
            ColumnPlan var15 = var1.plan(var13, var14);
            if (var15 != null && var15 != ColumnPlan.NONE && var15.inChannel()) {
               var8[var12] = true;
               var7[var12] = var15.waterSurfaceY();
               return;
            }

            Lake var16 = b(var1, var13, var14);
            if (var16 != null) {
               var8[var12] = true;
               var7[var12] = var16.level();
               return;
            }

            if (!b(var0, var13, var14)) {
               if (var15 != null && var15 != ColumnPlan.NONE && var15.inCorridor()) {
                  int var25 = a(var1, var0, var15, var13, var14, var11);
                  if (var25 != Integer.MIN_VALUE) {
                     var7[var12] = var25;
                     var9[var12] = true;
                  }

                  return;
               }

               int var24 = var0.getHeight(Types.OCEAN_FLOOR, var13, var14);
               int var26 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var13, var14);
               var7[var12] = Math.min(var24, var26) - 1;
               var9[var12] = true;
               return;
            }

            var9[var12] = true;
            var10[var12] = c(var0, var13, var14);
            int var17 = var0.getHeight(Types.OCEAN_FLOOR, var13, var14);
            int var18 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var13, var14);
            int var19 = Math.min(var17, var18) - 1;
            MutableBlockPos var20 = new MutableBlockPos();

            for (int var21 = 0; var21 < 48 && var19 > var11; var21++) {
               var20.set(var13, var19, var14);
               BlockState var22 = var0.getBlockState(var20);
               if (!var22.is(BlockTags.LOGS) && !var22.is(BlockTags.LEAVES) && !var22.isAir()) {
                  break;
               }

               var19--;
            }

            var7[var12] = var19;
         } catch (RuntimeException var23) {
         }
      }
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      for (int var6 = var4; var6 <= var5; var6++) {
         BlockState var7 = var0.getBlockState(var1.set(var2, var6, var3));
         if (var7.is(BlockTags.LOGS) || var7.is(BlockTags.LEAVES)) {
            return true;
         }
      }

      return false;
   }

   private static int a(WorldGenLevel var0, int var1, int var2) {
      int var3 = var0.getHeight(Types.OCEAN_FLOOR, var1, var2);
      int var4 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var1, var2);
      return Math.min(var3, var4) - 1;
   }

   private static BankRing.Metrics a(ColumnPlan var0, StreamsConfig var1, boolean var2) {
      double var3 = var0.width();
      double var5 = 2.0 * var3;
      double var7 = a(var0.slope());
      double var9 = a(var3 / Math.max(1.0E-6, var1.maxWidth()));
      double var11 = a(var0.speed() / 2.0);
      double var13 = 0.0;
      BankRing.Side var15 = null;
      if (var0.hasLateral()) {
         var13 = a(Math.abs(var0.curvature()) / 0.7);
         if (var0.curvature() != 0.0 && Math.abs(var0.lateral()) > 0.3) {
            var15 = var0.lateral() * var0.curvature() > 0.0 ? BankRing.Side.INNER : BankRing.Side.OUTER;
         }
      }

      double var16 = 16.0 * Math.max(1.0, var5);
      double var18 = var0.distToOutlet() >= Double.MAX_VALUE ? 1.0 : a(var0.distToOutlet() / var16);
      double var20 = var0.distToJunction() >= Double.MAX_VALUE ? 1.0 : a(var0.distToJunction() / var16);
      double var22 = a(var0.flatRun() / 96.0);
      return new BankRing.Metrics(var7, var9, var11, var13, var18, var20, var22, var15, var2);
   }

   private static double a(double var0) {
      return !(var0 <= 0.0) && !Double.isNaN(var0) ? (var0 >= 1.0 ? 1.0 : var0) : 0.0;
   }

   private static int a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = var4 - var2;
      int var8 = var5 - var3;
      boolean var9 = var7 >= 0 && var7 < 16 && var8 >= 0 && var8 < 16;
      if (!var9 && var1 != null && !b(var0, var4, var5)) {
         ColumnPlan var10 = var1.planLean(var4, var5);
         if (var10 != ColumnPlan.NONE && var10.inCorridor()) {
            if (var10.inChannel()) {
               return var10.bedY();
            }

            int var11 = a(var1, var0, var10, var4, var5, var6);
            if (var11 != Integer.MIN_VALUE) {
               return var11;
            }
         }
      }

      return var0.getHeight(Types.OCEAN_FLOOR, var4, var5) - 1;
   }

   private static boolean b(WorldGenLevel var0, int var1, int var2) {
      ChunkAccess var3 = var0.getChunk(var1 >> 4, var2 >> 4, ChunkStatus.EMPTY, false);
      return var3 == null ? false : var3.getStatus().isOrAfter(ChunkStatus.FEATURES);
   }

   private static boolean c(WorldGenLevel var0, int var1, int var2) {
      ChunkAccess var3 = var0.getChunk(var1 >> 4, var2 >> 4, ChunkStatus.EMPTY, false);
      if (var3 == null) {
         return false;
      }

      ChunkStatus var4 = var3.getStatus();
      return var4.isOrAfter(ChunkStatus.FEATURES) && !var4.isOrAfter(ChunkStatus.LIGHT);
   }

   static List<BankStyle> a(WorldGenLevel var0) {
      RegistryAccess var1 = var0.registryAccess();
      if (ah != var1) {
         ArrayList var2 = new ArrayList();
         var0.registryAccess().registry(BankStyle.REGISTRY_KEY).ifPresent(var1x -> var1x.forEach(var2::add));
         ai = List.copyOf(var2);
         ah = var1;
      }

      return ai;
   }

   public static double bankCohesion(WorldGenLevel var0, Holder<Biome> var1) {
      if (!Double.isNaN(al)) {
         return Math.max(0.0, Math.min(1.0, al));
      }

      RegistryAccess var2 = var0.registryAccess();
      if (aj != var2) {
         ak.clear();
         aj = var2;
      }

      Double var3 = ak.get(var1);
      if (var3 != null) {
         return var3;
      }

      List var4 = BankStyleLoader.configStyles();
      List var5 = a(var0);
      BankStyle var6 = a(var4, var5, var1);
      double var7 = var6 != null ? var6.cohesionOr(var1) : BankStyle.cohesionOf(var1);
      if (var6 != null && !var6.bedBlocks().isEmpty()) {
         double var9 = 0.0;

         for (BlockState var12 : var6.bedBlocks()) {
            var9 += blockStateCohesion(var12);
         }

         var7 = Math.min(var7, var9 / var6.bedBlocks().size());
      }

      ak.put(var1, var7);
      return var7;
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      int var2,
      int var3,
      int[] var4,
      int[] var5,
      boolean[] var6,
      BlockState[] var7,
      BlockState[] var8,
      ColumnPlan[] var9,
      int[] var10,
      int var11,
      int var12
   ) {
      List var13 = BankStyleLoader.configStyles();
      List var14 = a(var0);
      if (!var13.isEmpty() || !var14.isEmpty()) {
         MutableBlockPos var15 = new MutableBlockPos();
         HashMap var16 = new HashMap();
         QuartBiomeMemo var17 = new QuartBiomeMemo();
         HashMap var18 = new HashMap();

         for (int var19 = 0; var19 < 16; var19++) {
            for (int var20 = 0; var20 < 16; var20++) {
               int var21 = var2 + var19;
               int var22 = var3 + var20;
               int var23 = var19 * 16 + var20;
               int var24 = var4[var23];
               ColumnPlan var25 = var9[var23];
               if (var25 == null) {
                  var25 = var1.plan(var21, var22);
               }

               boolean var27 = false;
               int var26;
               if (var25 != ColumnPlan.NONE && var25.inCorridor()) {
                  var26 = var25.waterSurfaceY();
               } else {
                  int var28 = var10[var23];
                  if (var28 == -2147483647) {
                     Lake var29 = b(var1, var21, var22);
                     var28 = var29 == null ? Integer.MIN_VALUE : var29.level();
                  }

                  if (var28 == Integer.MIN_VALUE) {
                     continue;
                  }

                  var26 = var28;
                  var27 = true;
               }

               RiverCarverFeature.BankEnv var31 = a(var0, var21, var22, var26, var18, var1.config());
               boolean var32 = !var27
                  && var25.inChannel()
                  && ChannelBed.bedDepth(
                        var25,
                        var1.config(),
                        a(var0, var31, var15, var21, var24, var22),
                        var31.wetness(),
                        var31.cold(),
                        var0.getSeed(),
                        var21,
                        var22,
                        var26,
                        var24,
                        a(var1, var21, var22)
                     )
                     <= 0;
               boolean var30 = var32 || !var27 && ChannelBed.onPointBar(var25, var1.config());
               a(
                  var0,
                  var1,
                  var2,
                  var3,
                  var15,
                  var21,
                  var22,
                  var24,
                  var5[var23],
                  var6[var23],
                  var7[var23],
                  var8[var23],
                  var26,
                  !var27,
                  var13,
                  var14,
                  var16,
                  var17,
                  var11,
                  var12,
                  "bank",
                  var32,
                  var30,
                  var25
               );
            }
         }
      }
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      int var2,
      int var3,
      MutableBlockPos var4,
      int var5,
      int var6,
      int var7,
      int var8,
      boolean var9,
      BlockState var10,
      BlockState var11,
      int var12,
      boolean var13,
      List<BankStyle> var14,
      List<BankStyle> var15,
      Map<Holder<Biome>, BankStyle> var16,
      QuartBiomeMemo var17,
      int var18,
      int var19,
      String var20,
      boolean var21,
      boolean var22,
      ColumnPlan var23
   ) {
      int var24 = Integer.MIN_VALUE;
      int var25 = Math.min(var19, var7);
      int var26 = Math.max(var18 + 1, Math.min(var12, var25) - 24);

      for (int var27 = var25; var27 >= var26; var27--) {
         var4.set(var5, var27, var6);
         BlockState var28 = var0.getBlockState(var4);
         if (!var28.isAir() && var28.getFluidState().isEmpty() && var28.canOcclude()) {
            var24 = var27;
            break;
         }
      }

      boolean var65 = false;
      if (var13 && var24 != Integer.MIN_VALUE && var24 < var12 && var24 < var19 && var24 >= var8) {
         var4.set(var5, var24 + 1, var6);
         var65 = !var0.getBlockState(var4).getFluidState().isEmpty();
      }

      if (var24 != Integer.MIN_VALUE && (!var13 || var21 || var9 || var65 || var24 < var8)) {
         var4.set(var5, var24, var6);
         BlockState var66 = var0.getBlockState(var4);
         if (var66.is(Blocks.BEDROCK)) {
            if (c(var5, var6)) {
               b(var5, var6, "SKIN-SKIP pass=" + var20 + " why=bedrock surfaceY=" + var24 + " scanTop=" + var7);
            }
         } else {
            int var29 = var5 + (int)(d(var5, 0, var6) * 5.0) - 2;
            int var30 = var6 + (int)(d(var6, 1, var5) * 5.0) - 2;
             Holder<Biome> var31 = var17.get(var0, var29, var24, var30);
            BankStyle var32 = var16.computeIfAbsent(var31, var2x -> a(var14, var15, (Holder<Biome>)var2x));
            if (var32 != null && var32.enabled()) {
               long var33 = var0.getSeed();
               boolean var35 = var23 != null && var23 != ColumnPlan.NONE && var23.inCorridor() && var13;
               double var36 = var35 ? var23.width() : 16.0;
               double var38 = 2.0 * var36;
               double var40 = var35 ? Math.max(5.0, var36 * var1.config().bankWidth()) : Double.NaN;
               double var42 = var35 ? Math.max(0.0, var23.distance() - var36) : Double.NaN;
               BankRing.Metrics var44 = var35 ? a(var23, var1.config(), var22) : BankRing.Metrics.NONE;
               int var45 = var32.ringIndexAt(
                  var24 - var12, var42, var38, var40, var21, BankRing.hash01(var33, var5, var24, var6), BankRing.hash01(var33, var5, 23063, var6)
               );
               BankRing var46 = var32.rings().get(var45);
               boolean var47 = var45 >= 2;
               if (!var46.unpainted()) {
                  BlockState var48 = var46.pick(var33, var5, var24, var6, var38, var42, var44, var45);
                  int var49 = a(var0, var1, var2, var3, var5 + 1, var6, var18);
                  int var50 = a(var0, var1, var2, var3, var5 - 1, var6, var18);
                  int var51 = a(var0, var1, var2, var3, var5, var6 + 1, var18);
                  int var52 = a(var0, var1, var2, var3, var5, var6 - 1, var18);
                  int var53 = Math.max(Math.max(var24 - var49, var24 - var50), Math.max(var24 - var51, var24 - var52));
                  double var54 = Double.MAX_VALUE;

                  for (int var59 : new int[]{var49, var50, var51, var52}) {
                     var54 = Math.min(var54, Math.abs(var24 - var59));
                  }

                  BlockState var67 = null;
                  boolean var68 = var24 >= var8 - 1;
                  if (BankStyle.isChameleon(var48)) {
                     var48 = var10;
                     if (var11 != null && !var21 && var47 && (var68 || var54 <= 1.0)) {
                        var67 = var10;
                        var48 = var11;
                     }
                  }

                  if (c(var5, var6)) {
                     b(
                        var5,
                        var6,
                        "SKIN pass="
                           + var20
                           + " surfaceY="
                           + var24
                           + " ws="
                           + var12
                           + " ring="
                           + var46.kind()
                           + " exposure="
                           + var53
                           + " slope="
                           + String.format(Locale.ROOT, "%.2f", var54)
                           + " cur="
                           + var66.getBlock()
                           + " skin="
                           + (var48 == null ? "none" : var48.getBlock())
                           + " face="
                           + (var67 == null ? "same" : var67.getBlock().toString())
                           + " turf="
                           + (var67 != null)
                           + " grazed="
                           + var68
                           + " scanTop="
                           + var7
                           + " pre="
                           + var8
                           + " rebuilt="
                           + var9
                           + " nb="
                           + var49
                           + "/"
                           + var50
                           + "/"
                           + var51
                           + "/"
                           + var52
                           + " biome="
                           + var31.unwrapKey().map(var0x -> var0x.location().toString()).orElse("?")
                           + " ring["
                           + var46.kind()
                           + " "
                           + var46.blocks()
                           + " pattern="
                           + var46.pattern()
                           + " lateral="
                           + (Double.isNaN(var42) ? "?" : String.format(Locale.ROOT, "%.1f", var42))
                           + " width="
                           + var38
                           + "]"
                     );
                  }

                  if (var48 != null && !var48.isAir() && !var48.equals(var66)) {
                     a(var0, var4, var5, var24, var6, var48, var18);
                     int var69 = Mth.clamp(var53, 2, 8);
                     BlockState var70 = var67 != null ? var67 : var48;
                     if (a(var70)) {
                        var70 = Blocks.DIRT.defaultBlockState();
                     }

                     for (int var60 = 1; var60 <= var69; var60++) {
                        var4.set(var5, var24 - var60, var6);
                        BlockState var61 = var0.getBlockState(var4);
                        if (var61.isAir() || !var61.getFluidState().isEmpty()) {
                           break;
                        }

                        if (!var61.equals(var70)) {
                           var0.setBlock(var4, var70, 2);
                        }
                     }

                     var4.set(var5, var24 + 1, var6);
                     BlockState var71 = var0.getBlockState(var4);
                     if (!var71.isAir() && var71.getFluidState().isEmpty() && !var71.canSurvive(var0, var4)) {
                        var4.set(var5, var24 + 2, var6);
                        BlockState var72 = var0.getBlockState(var4);
                        boolean var62 = !var72.getFluidState().isEmpty() && !var72.hasProperty(BlockStateProperties.WATERLOGGED);
                        var4.set(var5, var24 + 1, var6);
                        if (var62) {
                           var0.setBlock(var4, var72, 2);
                           return;
                        }

                        var0.setBlock(var4, Blocks.AIR.defaultBlockState(), 2);

                        for (int var63 = var24 + 2; var63 <= var24 + 1 + 2; var63++) {
                           var4.set(var5, var63, var6);
                           BlockState var64 = var0.getBlockState(var4);
                           if (var64.isAir() || !var64.getFluidState().isEmpty() || var64.canSurvive(var0, var4)) {
                              break;
                           }

                           var0.setBlock(var4, Blocks.AIR.defaultBlockState(), 2);
                        }
                     }
                  }
               }
            } else {
               if (c(var5, var6)) {
                  b(
                     var5,
                     var6,
                     "SKIN-SKIP pass="
                        + var20
                        + " why="
                        + (var32 == null ? "nostyle" : "styleoff")
                        + " surfaceY="
                        + var24
                        + " scanTop="
                        + var7
                        + " biome="
                        + var31.unwrapKey().map(var0x -> var0x.location().toString()).orElse("?")
                  );
               }
            }
         }
      } else {
         if (c(var5, var6)) {
            b(
               var5,
               var6,
               "SKIN-SKIP pass="
                  + var20
                  + " why="
                  + (var24 == Integer.MIN_VALUE ? "nosurface" : "notlowered")
                  + " surfaceY="
                  + var24
                  + " scanTop="
                  + var7
                  + " pre="
                  + var8
                  + " rebuilt="
                  + var9
                  + " ws="
                  + var12
            );
         }
      }
   }

   public static void skinAllWater(WorldGenLevel var0, int var1, int var2, int var3) {
      if (var3 > 0 && Services.PLATFORM.customBanks()) {
         List var4 = BankStyleLoader.configStyles();
         List var5 = a(var0);
         if (!var4.isEmpty() || !var5.isEmpty()) {
            int var6 = var0.getMinBuildHeight();
            int var7 = var0.getMaxBuildHeight() - 1;
            byte var8 = 4;
            int var9 = 16 + 2 * var8;
            int[] var10 = new int[var9 * var9];
            QuartBiomeMemo var11 = new QuartBiomeMemo();
            Arrays.fill(var10, Integer.MIN_VALUE);
            MutableBlockPos var12 = new MutableBlockPos();
            RiverEngine var13 = StreamsWorldgen.engine(var0);

            for (int var14 = -var8; var14 < 16 + var8; var14++) {
               for (int var15 = -var8; var15 < 16 + var8; var15++) {
                  int var16 = var1 + var14;
                  int var17 = var2 + var15;
                  int var18 = Integer.MIN_VALUE;
                  if (var13 != null) {
                     Lake var19 = b(var13, var16, var17);
                     if (var19 != null) {
                        var18 = Mth.clamp(var19.level(), var6, var7);
                     } else {
                        ColumnPlan var20 = var13.planLean(var16, var17);
                        if (var20 != ColumnPlan.NONE && var20.inChannel()) {
                           var18 = Mth.clamp(var20.waterSurfaceY(), var6, var7);
                        }
                     }
                  }

                  if (var18 != Integer.MIN_VALUE) {
                     var10[(var14 + var8) * var9 + var15 + var8] = var18;
                  } else {
                     int var29 = var0.getHeight(Types.WORLD_SURFACE_WG, var16, var17) - 1;
                     var12.set(var16, var29, var17);
                     if (var0.getBlockState(var12).getFluidState().is(FluidTags.WATER)
                        && (var3 >= 2 || !var11.get(var0, var16, var29, var17).is(BiomeTags.IS_OCEAN))) {
                        var10[(var14 + var8) * var9 + var15 + var8] = var29;
                     }
                  }
               }
            }

            HashMap var24 = new HashMap();
            QuartBiomeMemo var25 = new QuartBiomeMemo();

            for (int var26 = 0; var26 < 16; var26++) {
               for (int var27 = 0; var27 < 16; var27++) {
                  int var28 = Integer.MIN_VALUE;
                  int var30 = Integer.MAX_VALUE;

                  for (int var31 = -var8; var31 <= var8; var31++) {
                     for (int var21 = -var8; var21 <= var8; var21++) {
                        int var22 = var10[(var26 + var31 + var8) * var9 + var27 + var21 + var8];
                        if (var22 != Integer.MIN_VALUE) {
                           int var23 = var31 * var31 + var21 * var21;
                           if (var23 <= var8 * var8 && var23 < var30) {
                              var30 = var23;
                              var28 = var22;
                           }
                        }
                     }
                  }

                  if (var28 != Integer.MIN_VALUE) {
                     int var32 = var1 + var26;
                     int var33 = var2 + var27;
                     int var34 = var0.getHeight(Types.OCEAN_FLOOR_WG, var32, var33) - 1;
                     a(
                        var0,
                        var13,
                        var1,
                        var2,
                        var12,
                        var32,
                        var33,
                        var34,
                        var34,
                        false,
                        a(var32, var33, var0, var34, var6),
                        a(var32, var33, var0, var34),
                        var28,
                        false,
                        var4,
                        var5,
                        var24,
                        var25,
                        var6,
                        var7,
                        "water",
                        false,
                        false,
                        null
                     );
                  }
               }
            }
         }
      }
   }

   static BankStyle a(List<BankStyle> var0, List<BankStyle> var1, Holder<Biome> var2) {
      ResourceKey var3 = (ResourceKey)var2.unwrapKey().orElse(null);

      for (boolean var7 : am) {
         BankStyle var8 = a(a(var0, var2, var3, var7), var3);
         if (var8 != null) {
            return var8;
         }

         var8 = a(a(var1, var2, var3, var7), var3);
         if (var8 != null) {
            return var8;
         }
      }

      for (boolean var16 : am) {
         int var19 = Math.max(a(var0, var2, var16), a(var1, var2, var16));
         if (var19 > 0) {
            BankStyle var9 = a(a(var0, var2, var19, var16), var3);
            if (var9 != null) {
               return var9;
            }

            var9 = a(a(var1, var2, var19, var16), var3);
            if (var9 != null) {
               return var9;
            }
         }
      }

      for (boolean var17 : am) {
         BankStyle var20 = a(b(var0, var2, var17), var3);
         if (var20 != null) {
            return var20;
         }

         var20 = a(b(var1, var2, var17), var3);
         if (var20 != null) {
            return var20;
         }
      }

      return null;
   }

   private static List<BankStyle> a(List<BankStyle> var0, Holder<Biome> var1, ResourceKey<Biome> var2, boolean var3) {
      ArrayList var4 = new ArrayList();

      for (BankStyle var6 : var0) {
         if (var6.star() == var3 && var6.matchesExact(var2) && !var6.excluded(var1)) {
            var4.add(var6);
         }
      }

      return var4;
   }

   private static int a(List<BankStyle> var0, Holder<Biome> var1, boolean var2) {
      int var3 = 0;

      for (BankStyle var5 : var0) {
         if (var5.star() == var2 && var5.matchesTags(var1) && !var5.excluded(var1)) {
            var3 = Math.max(var3, var5.tags().size());
         }
      }

      return var3;
   }

   private static List<BankStyle> a(List<BankStyle> var0, Holder<Biome> var1, int var2, boolean var3) {
      ArrayList var4 = new ArrayList();

      for (BankStyle var6 : var0) {
         if (var6.star() == var3 && var6.matchesTags(var1) && var6.tags().size() == var2 && !var6.excluded(var1)) {
            var4.add(var6);
         }
      }

      return var4;
   }

   private static List<BankStyle> b(List<BankStyle> var0, Holder<Biome> var1, boolean var2) {
      ArrayList var3 = new ArrayList();

      for (BankStyle var5 : var0) {
         if (var5.star() == var2 && var5.biomes().isEmpty() && var5.tags().isEmpty() && !var5.excluded(var1)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static BankStyle a(List<BankStyle> var0, ResourceKey<Biome> var1) {
      if (var0.isEmpty()) {
         return null;
      } else {
         return var0.size() != 1 && var1 != null ? (BankStyle)var0.get(Math.floorMod(var1.location().hashCode(), var0.size())) : (BankStyle)var0.get(0);
      }
   }

   private static void b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      int var6 = 0;
      int var7 = var4 - 1;

      for (int var8 = var4; var8 <= var5 && var6 <= 2; var8++) {
         var1.set(var2, var8, var3);
         BlockState var9 = var0.getBlockState(var1);
         if (var9.is(BlockTags.LOGS)) {
            var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
            var6 = 0;
            var7 = var8;
         } else if (var9.is(BlockTags.LEAVES)) {
            var6 = 0;
            var7 = var8;
         } else {
            if (!var9.isAir()) {
               break;
            }

            var6++;
         }
      }

      a(var0, var2, var3, var4 - 1, var7, var5);
   }

   private static void a(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5) {
      if (var4 >= var3) {
         MutableBlockPos var6 = new MutableBlockPos();
          ArrayDeque<long[]> var7 = new ArrayDeque<>();
          HashSet<Long> var8 = new HashSet<>();
          ArrayList<long[]> var9 = new ArrayList<>();

         for (int var10 = Math.max(var3, var4 - 10); var10 <= Math.min(var4 + 1, var5); var10++) {
            for (int[] var14 : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
               int var15 = var1 + var14[0];
               int var16 = var2 + var14[1];
               if (var0.getBlockState(var6.set(var15, var10, var16)).is(BlockTags.LEAVES)
                  && var8.add((long)var15 << 40 ^ (long)var10 << 20 ^ var16 & 1048575L)) {
                  var7.add(new long[]{var15, var10, var16});
               }
            }
         }

         while (!var7.isEmpty()) {
            long[] var22 = (long[])var7.poll();
            int var24 = (int)var22[0];
            int var26 = (int)var22[1];
            int var28 = (int)var22[2];
            var9.add(var22);
            if (var9.size() > 2048) {
               return;
            }

            for (int[] var17 : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
               int var18 = var24 + var17[0];
               int var19 = var26 + var17[1];
               int var20 = var28 + var17[2];
               if (Math.abs(var18 - var1) <= 10 && Math.abs(var20 - var2) <= 10 && var19 >= var3 - 10 && var19 <= var5) {
                  BlockState var21 = var0.getBlockState(var6.set(var18, var19, var20));
                  if (var21.is(BlockTags.LOGS)) {
                     return;
                  }

                  if (var21.is(BlockTags.LEAVES) && var8.add((long)var18 << 40 ^ (long)var19 << 20 ^ var20 & 1048575L)) {
                     var7.add(new long[]{var18, var19, var20});
                  }
               }
            }
         }

         BlockState var23 = Blocks.AIR.defaultBlockState();

         for (long[] var27 : var9) {
            var0.setBlock(var6.set((int)var27[0], (int)var27[1], (int)var27[2]), var23, 2);
         }
      }
   }

   private static boolean c(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      boolean var6 = false;
      int var7 = Math.min(var4 + 15, var5);

      for (int var8 = 0; var8 < 4; var8++) {
         boolean var9 = false;
         int var10 = 0;

         for (int var11 = var4; var11 <= var7; var11++) {
            var1.set(var2, var11, var3);
            BlockState var12 = var0.getBlockState(var1);
            if (var12.isAir()) {
               if (++var10 >= 3) {
                  break;
               }
            } else {
               var10 = 0;
               if (!var12.getFluidState().isEmpty()) {
                  break;
               }

               if (!var12.canSurvive(var0, var1)) {
                  var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
                  var9 = true;
               } else {
                  var1.set(var2, var11 - 1, var3);
                  boolean var13 = var0.getBlockState(var1).isAir();
                  var1.set(var2, var11, var3);
                  boolean var14 = var12.getBlock() instanceof SlabBlock;
                  boolean var15 = var12.is(Blocks.SNOW) || var12.is(Blocks.SNOW_BLOCK) || var12.is(Blocks.POWDER_SNOW);
                  if (!var13 || !var14 && !var15) {
                     if (var12.isCollisionShapeFullBlock(var0, var1)) {
                        break;
                     }
                  } else {
                     var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
                     var9 = true;
                  }
               }
            }
         }

         if (!var9) {
            break;
         }

         var6 = true;
      }

      return var6;
   }

   private static void a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, int[] var4, ColumnPlan[] var5, int var6) {
      MutableBlockPos var7 = new MutableBlockPos();

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var2 + var8;
            int var11 = var3 + var9;
            ColumnPlan var12 = var5[var8 * 16 + var9];
            if (var12 == null) {
               var12 = var1.plan(var10, var11);
            }

            if (var12 != ColumnPlan.NONE && var12.inCorridor()) {
               int var13 = var4[var8 * 16 + var9];
               var7.set(var10, var13, var11);
               if (var0.getBlockState(var7).isAir()) {
                  for (int var14 = var13 + 1; var14 <= var6; var14++) {
                     var7.set(var10, var14, var11);
                     BlockState var15 = var0.getBlockState(var7);
                     if (var15.isAir() || !var15.getFluidState().isEmpty()) {
                        break;
                     }

                     if (var15.is(BlockTags.LOGS) || var15.is(BlockTags.LEAVES)) {
                        if (!aq) {
                           if (ap) {
                              Constants.LOG
                                 .info(
                                    "[SR-TREESHAVE] kept tree at {},{},{} ({}) -- old rule would have shaved it",
                                    new Object[]{var10, var14, var11, var15.getBlock()}
                                 );
                           }
                           break;
                        }

                        if (ap) {
                           Constants.LOG.info("[SR-TREESHAVE] shaving tree material at {},{},{} ({})", new Object[]{var10, var14, var11, var15.getBlock()});
                        }
                     }

                     boolean var16 = var15.is(Blocks.SNOW) || var15.is(Blocks.SNOW_BLOCK) || var15.is(Blocks.POWDER_SNOW);
                     if (var16 || var15.canOcclude() && !var15.is(Blocks.BEDROCK)) {
                        var0.setBlock(var7, Blocks.AIR.defaultBlockState(), 2);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean d(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      boolean var6 = false;

      for (int var7 = 0; var7 < 4; var7++) {
         boolean var8 = false;

         for (int[] var12 : av) {
            int var13 = var2 + var12[0];
            int var14 = var3 + var12[1];
            if (a(var13, var14)) {
               for (int var15 = var5 + 2; var15 >= var4; var15--) {
                  var1.set(var13, var15, var14);
                  BlockState var16 = var0.getBlockState(var1);
                  if (!var16.isAir() && var16.getFluidState().isEmpty() && !var16.canSurvive(var0, var1)) {
                     var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
                     var8 = true;
                  }
               }
            }
         }

         if (!var8) {
            break;
         }

         var6 = true;
      }

      return var6;
   }

   private static int[] a(WorldGenLevel var0, ChunkPos var1) {
      int var2 = var1.getMinBlockX() >> 4;
      int var3 = var1.getMinBlockZ() >> 4;
      ChunkAccess var4 = var0.getChunk(var2, var3);
       Map<Structure, LongSet> var5 = var4.getAllReferences();
      if (var5.isEmpty()) {
         return ay;
      }

      int var6 = var1.getMinBlockX() - 3;
      int var7 = var1.getMaxBlockX() + 3;
      int var8 = var1.getMinBlockZ() - 3;
      int var9 = var1.getMaxBlockZ() + 3;
      int var10 = var0.getMaxBuildHeight() - 1;
      int[] var11 = ay;
      int var12 = 0;

      for (Entry var14 : var5.entrySet()) {
         LongIterator var15 = ((LongSet)var14.getValue()).iterator();

         while (var15.hasNext()) {
            long var16 = var15.nextLong();
            int var18 = ChunkPos.getX(var16);
            int var19 = ChunkPos.getZ(var16);
            ChunkAccess var20 = var18 == var2 && var19 == var3 ? var4 : var0.getChunk(var18, var19, ChunkStatus.STRUCTURE_STARTS, false);
            if (var20 != null) {
               StructureStart var21 = var20.getStartForStructure((Structure)var14.getKey());
               if (var21 != null && var21.isValid()) {
                   List<StructurePiece> var22 = var21.getPieces();
                  if (var22 != null && !var22.isEmpty()) {
                     for (StructurePiece var24 : var22) {
                        BoundingBox var25 = var24.getBoundingBox();
                        if (var25.maxX() >= var6 && var25.minX() <= var7 && var25.maxZ() >= var8 && var25.minZ() <= var9) {
                           if (var12 + 5 > var11.length) {
                              var11 = Arrays.copyOf(var11, Math.max(10, var11.length * 2));
                           }

                           var11[var12] = var25.minX() - 3;
                           var11[var12 + 1] = var25.minZ() - 3;
                           var11[var12 + 2] = var25.maxX() + 3;
                           var11[var12 + 3] = var25.maxZ() + 3;
                           var11[var12 + 4] = StreamsWorldgen.claimedTopY(var25, var10);
                           var12 += 5;
                        }
                     }
                  }
               }
            }
         }
      }

      return var12 == var11.length ? var11 : Arrays.copyOf(var11, var12);
   }

   private static boolean a(int[] var0, int var1, int var2, int var3) {
      return b(var0, var1, var2, var3) != Integer.MIN_VALUE;
   }

   private static int b(int[] var0, int var1, int var2, int var3) {
      int var4 = Integer.MIN_VALUE;

      for (int var5 = 0; var5 < var0.length; var5 += 5) {
         if (var1 >= var0[var5]
            && var1 <= var0[var5 + 2]
            && var2 >= var0[var5 + 1]
            && var2 <= var0[var5 + 3]
            && var0[var5 + 4] >= var3 - 6
            && var0[var5 + 4] > var4) {
            var4 = var0[var5 + 4];
         }
      }

      return var4;
   }

   private static boolean a(RiverEngine var0, int var1, int var2, int var3) {
      ColumnPlan var4 = var0.plan(var1, var2);
      return var4.inCorridor() && var3 < var4.bedY() - 6;
   }

   private static int a(RiverEngine var0, int var1, int var2, int var3, int var4) {
      if (var4 >= var3 - 1) {
         return Integer.MIN_VALUE;
      }

      int var5 = 0;

      for (int var6 = 5; var6 >= 1; var6--) {
         if (b(var6, var3, var4) > var4) {
            var5 = var6;
            break;
         }
      }

      if (var5 == 0) {
         return Integer.MIN_VALUE;
      }

      Lake var12 = var0.lakeWetting(var1, var2, 2);
      if (var12 == null) {
         return Integer.MIN_VALUE;
      }

      for (int var7 = 1; var7 <= var5; var7++) {
         for (int var8 = -var7; var8 <= var7; var8++) {
            for (int var9 = -var7; var9 <= var7; var9++) {
               if (Math.max(Math.abs(var8), Math.abs(var9)) == var7) {
                  int var10 = var1 + var8;
                  int var11 = var2 + var9;
                  if (!(var12.coverageSmooth(var10, var11) > 0.5) && !var0.inCorridor(var10, var11)) {
                     return b(var7, var3, var4);
                  }
               }
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private static int b(int var0, int var1, int var2) {
      double var3 = var0 / 6.0;
      double var5 = var3 * var3 * (3.0 - 2.0 * var3);
      return Math.min((int)Math.round(var1 - var5 * (var1 - var2)), var1 - 1);
   }

   private static int a(Lake var0, int var1, int var2) {
      for (int var3 = 1; var3 <= 5; var3++) {
         for (int var4 = -var3; var4 <= var3; var4++) {
            for (int var5 = -var3; var5 <= var3; var5++) {
               if (Math.max(Math.abs(var4), Math.abs(var5)) == var3 && !var0.wetAt(var1 + var4, var2 + var5)) {
                  return var3;
               }
            }
         }
      }

      return 6;
   }

   private static int a(Lake var0, int var1, int var2, int var3) {
      int var4 = a(var0, var1, var2);
      double var5 = 1.0 - Math.min(1.0, (var4 - 1) / 5.0);
      int var7 = (int)Math.round(8.0 - 7.0 * var5);
      int var8 = (int)Math.round((c(var1, var2, 5) - 0.5) * 2.0 * 2.0);
      return var3 - var7 + var8;
   }

   private static void a(
      WorldGenLevel var0, MutableBlockPos var1, RiverEngine var2, int var3, int var4, int var5, int var6, int var7, int var8, BlockState var9
   ) {
      int var10 = var6;
      if (var6 - 1 >= var8) {
         BlockState var11 = var0.getBlockState(var1.set(var3, var6 - 1, var4));
         if (var11.isAir() || !var11.getFluidState().isEmpty()) {
            Lake var12 = b(var2, var3, var4);
            if (var12 != null) {
               var10 = a(var12, var3, var4, var5);
            }
         }
      }

      a(var0, var1, var3, var4, var10, var6, var7, var8, var9);
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, BlockState var2) {
      if (var2.isAir()) {
         return true;
      }

      if (var2.getFluidState().isEmpty() && var2.canBeReplaced()) {
         int var3 = var1.getX();
         int var4 = var1.getY();
         int var5 = var1.getZ();
         var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);

         for (int var6 = var4 + 1; var6 <= var4 + 2; var6++) {
            var1.set(var3, var6, var5);
            BlockState var7 = var0.getBlockState(var1);
            if (var7.isAir() || !var7.getFluidState().isEmpty() || var7.canSurvive(var0, var1)) {
               break;
            }

            var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
         }

         var1.set(var3, var4, var5);
         return true;
      } else {
         return false;
      }
   }

   private static void a(WorldGenLevel var0, BlockPos var1, BlockState var2) {
      BlockState var3 = var0.getBlockState(var1);
      if (var3.hasProperty(BlockStateProperties.WATERLOGGED)) {
         if (!(Boolean)var3.getValue(BlockStateProperties.WATERLOGGED)) {
            var0.setBlock(var1, (BlockState)var3.setValue(BlockStateProperties.WATERLOGGED, true), 2);
         }
      } else if (var3.getFluidState().isEmpty()) {
         if (!var3.isCollisionShapeFullBlock(var0, var1)) {
            var0.setBlock(var1, var2, 2);
         }
      }
   }

   private static int a(ColumnPlan var0, int var1, int var2) {
      if (var2 <= var1) {
         return var1;
      }

      double var3 = var0.distToOutlet();
      double var5 = Math.max(24.0, 3.0 * var0.width());
      double var7 = Math.min(1.0, var3 / var5);
      double var9 = var7 * var7 * (3.0 - 2.0 * var7);
      return (int)Math.round(var2 + (var1 - var2) * var9);
   }

   private static boolean a(
      WorldGenLevel var0,
      MutableBlockPos var1,
      int var2,
      int var3,
      ColumnPlan var4,
      RiverEngine var5,
      int var6,
      int var7,
      int var8,
      int var9,
      BlockState var10,
      RiverCarverFeature.BankEnv var11,
      double var12,
      long var14,
      int var16
   ) {
      int var17 = var7;
      int var18 = ChannelBed.bedDepth(
         var4, var5.config(), a(var0, var11, var1, var2, var16, var3), var11.wetness(), var11.cold(), var14, var2, var3, var17, var16, var12
      );
      if (var18 <= 0) {
         var1.set(var2, var6, var3);
         BlockState var22 = var0.getBlockState(var1);
         int var23 = Math.min(var9, Math.min(var17 - var18, var17 + 2));
         if (var23 <= var6) {
            return false;
         }

         for (int var21 = var6 + 1; var21 <= var23; var21++) {
            var1.set(var2, var21, var3);
            var0.setBlock(var1, var22, 2);
         }

         return true;
      } else {
         int var19 = a(var4, var17 - var18, var6);
         if (var19 >= var6) {
            return false;
         }

         for (int var20 = var6; var20 > var19 && var20 > var8; var20--) {
            var1.set(var2, var20, var3);
            var0.setBlock(var1, var10, 2);
         }

         return true;
      }
   }

   private static int a(RiverEngine var0, WorldGenLevel var1, ColumnPlan var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      if (var6 != Integer.MIN_VALUE && !(var2.distToOutlet() > 24.0)) {
         int var10 = a(var0, var1, var3, var4, var2, var7, var8, var9);
         int var11 = Math.min(Math.min(var6, var10), var5);
         var11 = Math.min(var11, var2.waterSurfaceY() + 4);
         return var11 > var2.waterSurfaceY() ? var11 : Integer.MIN_VALUE;
      } else {
         return Integer.MIN_VALUE;
      }
   }

   public static int nearbyLakeLevel(RiverEngine var0, int var1, int var2, int var3, int var4) {
      int var5 = Math.max(1, var0.config().gridSpacing());
      int var6 = Integer.MIN_VALUE;

      for (int var7 = -2 * var5; var7 <= 2 * var5; var7 += var5) {
         for (int var8 = -2 * var5; var8 <= 2 * var5; var8 += var5) {
            Lake var9 = var0.lakeAt(var1 + var8, var2 + var7);
            if (var9 != null && var9.digDepth() <= 0) {
               var6 = Math.max(var6, Mth.clamp(var9.level(), var3, var4));
            }
         }
      }

      return var6;
   }

   private static int a(RiverEngine var0, WorldGenLevel var1, int var2, int var3, ColumnPlan var4, int var5, int var6, int var7) {
      double var8 = Math.hypot(var4.flowDirX(), var4.flowDirZ());
      if (var8 < 1.0E-6) {
         return Integer.MAX_VALUE;
      }

      double var10 = -var4.flowDirZ() / var8;
      double var12 = var4.flowDirX() / var8;
      double var14 = var4.width();
      int var16 = Integer.MAX_VALUE;

      for (double var17 = var14 + 1.0; var17 <= var14 + 4.0; var17 += 3.0) {
         for (int var19 = -1; var19 <= 1; var19 += 2) {
            int var20 = var2 + (int)Math.round(var10 * var17 * var19);
            int var21 = var3 + (int)Math.round(var12 * var17 * var19);
            Lake var22 = var0.lakeWetting(var20, var21, 2);
            int var23;
            if (var22 != null && var22.digDepth() <= 0) {
               var23 = Mth.clamp(var22.level(), var6, var7);
            } else {
               if (!a(var20, var21)) {
                  continue;
               }

               var23 = var1.getHeight(Types.OCEAN_FLOOR_WG, var20, var21) - 1;
            }

            var16 = Math.min(var16, var23);
         }
      }

      return var16 == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(var16, var5 - 1);
   }

   private static boolean b(RiverEngine var0, int var1, int var2, int var3) {
      for (int[] var7 : Z) {
         ColumnPlan var8 = var0.plan(var1 + var7[0], var2 + var7[1]);
         if (!var8.inCorridor() || !var8.inChannel() || var8.waterSurfaceY() < var3) {
            return true;
         }
      }

      return false;
   }

   private static void a(int[] var0, int[] var1, int var2, int var3) {
      if (var3 < var0[var2]) {
         var0[var2] = var3;
      }

      if (var3 > var1[var2]) {
         var1[var2] = var3;
      }
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      MutableBlockPos var2,
      int var3,
      int var4,
      int[] var5,
      int[] var6,
      boolean[] var7,
      int[] var8,
      int[] var9,
      int[] var10,
      BlockState[] var11,
      QuartBiomeMemo var12,
      int var13,
      int var14
   ) {
      if (az) {
         RiverFlowField.BiomeClass var15 = RiverFlowFields.classifierFor(var0.getLevel());
         BlockState var16 = Services.PLATFORM.streamWaterState(0);
         BlockState var17 = Blocks.AIR.defaultBlockState();
         boolean var18 = !Boolean.getBoolean("streamsreflowing.noforeignlift");
         int[] var19 = new int[256];

         for (int var20 = 0; var20 < 256; var20++) {
            var19[var20] = var6[var20];
            if (var18 && var7[var20] && var5[var20] <= var6[var20]) {
               int var21 = var3 + (var20 >> 4);
               int var22 = var4 + (var20 & 15);
               if (var0.getBlockState(var2.set(var21, var6[var20], var22)).isAir()) {
                  int var23 = Math.max(var8[var20], var9[var20]);
                  Lake var24 = b(var1, var21, var22);
                  if (var24 != null) {
                     var23 = Math.max(var23, var24.level());
                  }

                  for (int var25 = var6[var20] + 1; var25 <= Math.min(var14, var6[var20] + 24); var25++) {
                     BlockState var26 = var0.getBlockState(var2.set(var21, var25, var22));
                     if (!a(var26, var16) || var25 <= var23 || a(var0, var15, var12, var21, var25, var22)) {
                        break;
                     }

                     var0.setBlock(var2, var17, 2);
                     var19[var20] = var25;
                     if (c(var21, var22)) {
                        b(var21, var22, "FOREIGN-LIFT y=" + var25 + " was=" + var26 + " (standing over the cut)");
                     }
                  }
               }
            }
         }

         for (int var40 = 0; var40 < 256; var40++) {
            if (var7[var40] && var5[var40] <= var19[var40]) {
               int var41 = var3 + (var40 >> 4);
               int var42 = var4 + (var40 & 15);

               for (int var43 = var5[var40]; var43 <= var19[var40]; var43++) {
                  if (var0.getBlockState(var2.set(var41, var43, var42)).isAir()) {
                     for (int[] var27 : Z) {
                        int var28 = var41 + var27[0];
                        int var29 = var42 + var27[1];
                        BlockState var30 = var0.getBlockState(var2.set(var28, var43, var29));
                        if (a(var30, var16)) {
                           ColumnPlan var31 = var1.plan(var28, var29);
                           if (var31 == ColumnPlan.NONE || !var31.inCorridor() || !var31.inChannel()) {
                              ColumnPlan var32 = var1.altPlan(var28, var29);
                              if ((var32 == null || !var32.inChannel()) && b(var1, var28, var29) == null && !a(var0, var15, var12, var28, var43, var29)) {
                                 int var33 = var28 - var3;
                                 int var34 = var29 - var4;
                                 boolean var35 = var33 >= 0 && var33 < 16 && var34 >= 0 && var34 < 16;
                                 int var36 = var35 ? var10[var33 * 16 + var34] : var0.getHeight(Types.OCEAN_FLOOR_WG, var28, var29) - 1;
                                 BlockState var37 = var35 ? var11[var33 * 16 + var34] : a(var0, var28, var29, var36, var13);
                                 int var38 = var43;

                                 while (var38 + 1 <= var14 && var38 - var43 < 24 && a(var0.getBlockState(var2.set(var28, var38 + 1, var29)), var16)) {
                                    var38++;
                                 }

                                 BlockState var39 = a(var0, var28, var29, var36, var38 - var43, var37, var13);
                                 if (!a(var0, var2, var28, var43, var29, var39)) {
                                    var39 = var37;
                                 }

                                 if (!a(var0, var2, var28, var43, var29, var39)) {
                                    var39 = var11[var40];
                                 }

                                 if (!a(var0, var2, var28, var43, var29, var39)) {
                                    var39 = Blocks.DIRT.defaultBlockState();
                                 }

                                 a(var0, var2, var28, var43, var29, var39, var13);
                                 if (c(var28, var29)) {
                                    b(
                                       var28,
                                       var29,
                                       "FOREIGN-SEAL y=" + var43 + " was=" + var30 + " -> " + var39 + " (edge of the cut at " + var41 + "," + var42 + ")"
                                    );
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean a(BlockState var0, BlockState var1) {
      return !var0.getFluidState().is(FluidTags.WATER) ? false : var1.is(Blocks.WATER) || !var0.is(var1.getBlock());
   }

   private static boolean a(WorldGenLevel var0, RiverFlowField.BiomeClass var1, QuartBiomeMemo var2, int var3, int var4, int var5) {
      if (var1.classify(var3, var5) != 0) {
         return true;
      }

      Holder var6 = var2.get(var0, var3, var4, var5);
      return var6.is(BiomeTags.IS_OCEAN) || var6.is(BiomeTags.IS_RIVER);
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5) {
      return var5 != null && !var5.isAir() && var5.getFluidState().isEmpty() && var5.canOcclude() && var5.isCollisionShapeFullBlock(var0, var1.set(var2, var3, var4));
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = Math.max(var4, var6 + 1);

      for (int[] var11 : Z) {
         int var12 = var2 + var11[0];
         int var13 = var3 + var11[1];

         for (int var14 = var7; var14 <= var5; var14++) {
            var1.set(var12, var14, var13);
            if (var0.getBlockState(var1).getFluidState().is(FluidTags.WATER)) {
               var0.scheduleTick(new BlockPos(var12, var14, var13), Fluids.WATER, 5 + Math.floorMod(Mth.getSeed(var12, var14, var13), 20));
            }
         }
      }
   }

   static int a(ColumnPlan var0) {
      double var1 = var0.fluidFlowDirX();
      double var3 = var0.fluidFlowDirZ();
      if (Math.sqrt(var1 * var1 + var3 * var3) < 0.5) {
         return 8;
      }

      int var5 = (int)Math.round(Math.atan2(var0.fluidFlowDirZ(), var0.fluidFlowDirX()) / (Math.PI / 4));
      return (var5 % 8 + 8) % 8;
   }

   private static boolean b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      return var0.getBlockState(var1.set(var2 + 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2 - 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 + 1)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 - 1)).isAir();
   }

   private static boolean e(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      if (var3 - 1 >= var5) {
         BlockState var6 = var0.getBlockState(var1.set(var2, var3 - 1, var4));
         if (!var6.isAir() && var6.getFluidState().isEmpty()) {
            return true;
         }
      }

      return c(var0, var1, var2 + 1, var3, var4)
         || c(var0, var1, var2 - 1, var3, var4)
         || c(var0, var1, var2, var3, var4 + 1)
         || c(var0, var1, var2, var3, var4 - 1);
   }

   private static boolean c(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      BlockState var5 = var0.getBlockState(var1.set(var2, var3, var4));
      return !var5.isAir() && var5.getFluidState().isEmpty();
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5, int var6) {
      if (var5.getBlock() instanceof FallingBlock && var3 - 1 >= var6) {
         var1.set(var2, var3 - 1, var4);
         BlockState var7 = var0.getBlockState(var1);
         if (var7.isAir() || !var7.getFluidState().isEmpty()) {
            var0.setBlock(var1, Blocks.STONE.defaultBlockState(), 2);
         }
      }

      var1.set(var2, var3, var4);
      var0.setBlock(var1, var5, 2);
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      a(var0, var1, var2, var3, var4, var5, var6, var7, null);
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5, int var6, int var7, BlockState var8) {
      BlockState var9 = var8 != null && !var8.isAir() && var8.getFluidState().isEmpty() ? var8 : a(var0, var2, var3, var6, var7);
      int var10 = Math.max(var7, Math.min(var4, var5));
      if (c(var2, var3)) {
         var1.set(var2, Math.max(var7, var10 - 1), var3);
         BlockState var11 = var0.getBlockState(var1);
         if (var11.isAir() || !var11.getFluidState().isEmpty()) {
            b(var2, var3, "CUP void below floor: hanging funnel from " + var10 + " to bed " + var5);
         }
      }

      for (int var13 = var10; var13 <= var5; var13++) {
         var1.set(var2, var13, var3);
         BlockState var12 = var0.getBlockState(var1);
         if (a(var0, var1, var12)) {
            a(var0, var1, var2, var13, var3, var9, var7);
         }
      }
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3, int var4, BlockState var5) {
      for (int[] var9 : p) {
         int var10 = var1 + var9[0];
         int var11 = var2 + var9[1];
         int var12 = var0.getHeight(Types.OCEAN_FLOOR, var10, var11) - 1;
         if (var12 >= var3 && var12 >= var4) {
            BlockState var13 = var0.getBlockState(new BlockPos(var10, var12, var11));
            if (!var13.isAir() && var13.getFluidState().isEmpty() && var13.canOcclude()) {
               return var13;
            }
         }
      }

      return var5;
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5, BlockState var6, int var7) {
      if (var3 >= var7 && var6 != null) {
         var1.set(var2, var3, var4);
         BlockState var8 = var0.getBlockState(var1);
         if (!var8.isAir() && var8.getFluidState().isEmpty() && !var8.is(Blocks.BEDROCK)) {
            BlockState var9 = var5 != null ? var5 : a(var0, var2, var4, var3, var7, var6);
            a(var0, var1, var2, var3, var4, var9, var7);
            int var10 = var3;

            for (int var11 = var3 - 1; var11 > var3 - 4 && var11 >= var7; var10 = var11--) {
               var1.set(var2, var11, var4);
               BlockState var12 = var0.getBlockState(var1);
               if (var12.isAir() || !var12.getFluidState().isEmpty() || var12.is(Blocks.BEDROCK)) {
                  break;
               }

               a(var0, var1, var2, var11, var4, var6, var7);
            }

            b(var0, var1, var2, var10 - 1, var4, var6, var7);
         }
      }
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3) {
      BlockState var4 = var0.getBlockState(new BlockPos(var1, var3, var2));
      return !var4.isAir() && var4.getFluidState().isEmpty() && var4.canOcclude() ? var4 : null;
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3, int var4) {
      BlockState var5 = var0.getBlockState(new BlockPos(var1, var3, var2));
      BlockState var6 = var3 - 1 >= var4 ? var0.getBlockState(new BlockPos(var1, var3 - 1, var2)) : var5;
      return !var6.isAir() && var6.getFluidState().isEmpty() ? var6 : var5;
   }

   private static int a(
      WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6, int var7, int var8, int[] var9
   ) {
      int var10 = a(var7, var8, var9);
      if (var10 != Integer.MIN_VALUE) {
         return var10;
      }

      boolean var11 = false;

      for (int var12 = 0; var12 < 256 && !var11; var12++) {
         var11 = var9[var12] != Integer.MAX_VALUE;
      }

      return var11 && var5.distance() <= 8.0 ? Integer.MIN_VALUE : a(var0, var1, var2, var3, var4, var5, var6);
   }

   private static int a(int var0, int var1, int[] var2) {
      int var3 = Integer.MAX_VALUE;
      int var4 = Integer.MAX_VALUE;

      for (int var5 = -8; var5 <= 8; var5++) {
         for (int var6 = -8; var6 <= 8; var6++) {
            int var7 = var0 + var5;
            int var8 = var1 + var6;
            if (var7 >= 0 && var7 <= 15 && var8 >= 0 && var8 <= 15) {
               int var9 = var2[var7 * 16 + var8];
               if (var9 != Integer.MAX_VALUE) {
                  int var10 = var5 * var5 + var6 * var6;
                  if (var10 < var4) {
                     var4 = var10;
                     var3 = var9;
                  }
               }
            }
         }
      }

      return var3 == Integer.MAX_VALUE ? Integer.MIN_VALUE : var3;
   }

   private static int a(WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6) {
      double var7 = Math.hypot(var5.flowDirX(), var5.flowDirZ());
      if (var7 < 1.0E-6) {
         return Integer.MIN_VALUE;
      }

      if (var5.waterSurfaceY() <= var0.getHeight(Types.OCEAN_FLOOR_WG, var3, var4) - 1) {
         return Integer.MIN_VALUE;
      }

      double var9 = var5.flowDirX() / var7;
      double var11 = var5.flowDirZ() / var7;
      double var13 = -var5.flowDirZ() / var7;
      double var15 = var5.flowDirX() / var7;
      int var17 = (int)Math.round(var5.distance());
      int var18 = var3;
      int var19 = var4;
      if (var17 > 0) {
         int var20 = var3 + (int)Math.round(var13 * var17);
         int var21 = var4 + (int)Math.round(var15 * var17);
         int var22 = var3 - (int)Math.round(var13 * var17);
         int var23 = var4 - (int)Math.round(var15 * var17);
         ColumnPlan var24 = var1.plan(var20, var21);
         ColumnPlan var25 = var1.plan(var22, var23);
         boolean var26 = var24 != ColumnPlan.NONE && var24.inChannel();
         boolean var27 = var25 != ColumnPlan.NONE && var25.inChannel();
         if (var26 != var27) {
            var18 = var26 ? var20 : var22;
            var19 = var26 ? var21 : var23;
         } else {
            double var28 = var24 == ColumnPlan.NONE ? Double.MAX_VALUE : var24.distance();
            double var30 = var25 == ColumnPlan.NONE ? Double.MAX_VALUE : var25.distance();
            var18 = var28 <= var30 ? var20 : var22;
            var19 = var28 <= var30 ? var21 : var23;
         }
      }

      ColumnPlan var38 = var1.plan(var18, var19);
      if (var38 != ColumnPlan.NONE && var38.inChannel() && a(var18, var19)) {
         int var39 = var0.getHeight(Types.OCEAN_FLOOR_WG, var18, var19) - 1;
         int var41 = f(var0, var2, var18, var19, var39, var6);
         if (var41 != Integer.MIN_VALUE && var38.waterSurfaceY() <= var41 + 1) {
            return var41;
         }
      }

      int var40 = 0;
      int var42 = 0;
      int var43 = Integer.MAX_VALUE;

      for (int var44 = -3; var44 <= 3; var44 += 3) {
         int var45 = var18 + (int)Math.round(var9 * var44);
         int var46 = var19 + (int)Math.round(var11 * var44);
         ColumnPlan var47 = var44 == 0 ? var1.plan(var18, var19) : var1.plan(var45, var46);
         if (var47 != ColumnPlan.NONE && var47.inChannel()) {
            int var48 = Math.max(1, (int)Math.round(var47.width()));
            int var29 = Math.max(1, var48 / 2);
            int var49 = 0;
            int var31 = 0;
            int var32 = Integer.MAX_VALUE;

            for (int var33 = -var48; var33 <= var48; var33 += var29) {
               int var34 = var45 + (int)Math.round(var13 * var33);
               int var35 = var46 + (int)Math.round(var15 * var33);
               ColumnPlan var36 = var1.plan(var34, var35);
               if (var36 != ColumnPlan.NONE && var36.inCorridor()) {
                  var49++;
                  int var37 = b(var0, var1, var2, var34, var35, var36, var6);
                  if (var37 != Integer.MIN_VALUE) {
                     var31++;
                     var32 = Math.min(var32, var37);
                  }
               }
            }

            if (var49 >= 3) {
               var40++;
               if (var31 == var49) {
                  var42++;
                  var43 = Math.min(var43, var32);
               }
            }
         }
      }

      return var40 > 0 && var42 * 2 > var40 ? var43 : Integer.MIN_VALUE;
   }

   private static int b(WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6) {
      long var7 = Hashing.mix((long)var3 << 32 ^ var4 & 4294967295L);
      HashMap var9 = C.get();
      Integer var10 = (Integer)var9.get(var7);
      if (var10 != null) {
         return var10;
      }

      int var11 = var5.waterSurfaceY();
      int var12 = a(var0, var1, var2, var3, var4, var5, var11, var6);
      int var13 = var12 < var11 ? var12 : Integer.MIN_VALUE;
      if (var13 == Integer.MIN_VALUE && a(var3, var4)) {
         int var14 = var0.getHeight(Types.OCEAN_FLOOR_WG, var3, var4) - 1;
         int var15 = f(var0, var2, var3, var4, var14, var6);
         if (var15 != Integer.MIN_VALUE && var11 <= var15 + 1) {
            var13 = var15;
         }
      }

      var9.put(var7, var13);
      return var13;
   }

   private static int a(WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6, int var7) {
      double var8 = Math.hypot(var5.flowDirX(), var5.flowDirZ());
      if (var8 < 1.0E-6) {
         return var6;
      }

      double var10 = var5.flowDirX() / var8;
      double var12 = var5.flowDirZ() / var8;
      int var14 = var6;
      int var15 = a(var0, var2, var3, var4, var5, var7);
      if (var15 != Integer.MIN_VALUE && var15 < var14) {
         var14 = var15;
      }

      for (int var16 = 3; var16 <= 48; var16 += 3) {
         int var17 = var3 - (int)Math.round(var10 * var16);
         int var18 = var4 - (int)Math.round(var12 * var16);
         if (!a(var17, var18)) {
            break;
         }

         ColumnPlan var19 = var1.plan(var17, var18);
         if (var19 != ColumnPlan.NONE && var19.inChannel()) {
            int var20 = a(var0, var2, var17, var18, var19, var7);
            if (var20 != Integer.MIN_VALUE && var20 < var14) {
               var14 = var20;
            }
         }
      }

      return var14;
   }

   private static int a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, ColumnPlan var4, int var5) {
      double var6 = Math.hypot(var4.flowDirX(), var4.flowDirZ());
      if (var6 < 1.0E-6) {
         return Integer.MIN_VALUE;
      }

      double var8 = -var4.flowDirZ() / var6;
      double var10 = var4.flowDirX() / var6;
      int var12 = Math.max(1, (int)Math.round(var4.width()));
      int var13 = (int)Math.round(Math.min(var4.distance(), var4.width() + 8.0));

      for (int var14 = -1; var14 <= 1; var14 += 2) {
         int var15 = var2 + (int)Math.round(var8 * var13 * var14);
         int var16 = var3 + (int)Math.round(var10 * var13 * var14);
         int var17 = Integer.MIN_VALUE;
         int var18 = Integer.MAX_VALUE;
         boolean var19 = true;
         boolean var20 = false;

         for (int var21 = -var12; var21 <= var12; var21 += var12 == 0 ? 1 : var12) {
            int var22 = var15 + (int)Math.round(var8 * var21);
            int var23 = var16 + (int)Math.round(var10 * var21);
            if (!a(var22, var23)) {
               var20 = true;
               break;
            }

            int var24 = var0.getHeight(Types.OCEAN_FLOOR_WG, var22, var23) - 1;
            int var25 = f(var0, var1, var22, var23, var24, var5);
            if (var25 == Integer.MIN_VALUE) {
               var18 = Math.min(var18, var24);
            } else {
               var17 = Math.max(var17, var25);
            }
         }

         if (var20) {
            if (var13 == 0) {
               break;
            }
         } else {
            if (var17 == Integer.MIN_VALUE) {
               var19 = false;
            } else if (var18 != Integer.MAX_VALUE && var18 > var17) {
               var19 = false;
            }

            if (var19) {
               return var17;
            }

            if (var13 == 0) {
               break;
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private static int f(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      int var6 = Math.min(var5, var4 + 1);
      if (var0.getBlockState(var1.set(var2, var6, var3)).getFluidState().isEmpty()) {
         return Integer.MIN_VALUE;
      }

      int var7 = var6;

      while (var7 + 1 <= var5 && var7 - var6 < 24 && !var0.getBlockState(var1.set(var2, var7 + 1, var3)).getFluidState().isEmpty()) {
         var7++;
      }

      return var7;
   }

   private static boolean a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, ColumnPlan var4, int var5) {
      double var6 = Math.hypot(var4.flowDirX(), var4.flowDirZ());
      if (var6 < 1.0E-6) {
         return false;
      }

      double var8 = var4.flowDirX() / var6;
      double var10 = var4.flowDirZ() / var6;

      for (int var12 = 3; var12 <= 12; var12 += 3) {
         int var13 = var2 + (int)Math.round(var8 * var12);
         int var14 = var3 + (int)Math.round(var10 * var12);
         ColumnPlan var15 = var1.plan(var13, var14);
         if (var15 != ColumnPlan.NONE && var15.inChannel() && var15.waterSurfaceY() > var5 + 1) {
            int var16 = var0.getHeight(Types.OCEAN_FLOOR_WG, var13, var14) - 1;
            if (var16 >= var15.waterSurfaceY() - 3) {
               return true;
            }
         }
      }

      return false;
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3, int var4, BlockState var5, int var6) {
      int var7 = var3 - var4;
      if (var7 < var6) {
         return var5;
      }

      BlockState var8 = var0.getBlockState(new BlockPos(var1, var7, var2));
      return !var8.isAir() && var8.getFluidState().isEmpty() ? var8 : var5;
   }

   private static boolean a(BlockState var0) {
      return aC.contains(var0.getBlock()) || var0.hasProperty(BlockStateProperties.SNOWY);
   }

   private static void b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5, int var6) {
      if (var3 >= var6 && !var5.isAir()) {
         BlockState var7 = var0.getBlockState(var1.set(var2, var3, var4));
         if (a(var7) && !var7.equals(var5)) {
            var0.setBlock(var1, var5, 2);
         }
      }
   }

   private static int a(int var0, int var1, int var2, ColumnPlan var3, double var4) {
      return a(var0, var1, var2, var3, var4, 8);
   }

   private static int a(int var0, int var1, int var2, ColumnPlan var3, double var4, int var6) {
      double var7 = Math.max(5.0, var3.width() * var4);
      int var9 = Mth.clamp((int)Math.round(5.0 * var3.width()), Math.min(5, var6), var6);
      double var10 = Math.max(0.0, var3.distance() - var3.width());
      double var12 = var7 > 1.0E-6 ? Math.min(1.0, var10 / var7) : 1.0;
      int var14 = (int)Math.round(var9 - (var9 - 1) * var12);
      int var15 = (int)Math.round((c(var0, var1, 5) - 0.5) * 2.0 * 2.0);
      return var2 - var14 + var15;
   }

   private static int a(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4, int var5, int var6) {
      int var7 = var3.waterSurfaceY();
      if (var4 >= var7) {
         return var7;
      } else {
         return var3.source() ? Math.min(var7, a(var0, var1, var2, var3, var5, var6)) : var7;
      }
   }

   private static boolean a(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4) {
      double var5 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var5 < 1.0E-6) {
         return false;
      }

      double var7 = -var3.flowDirZ() / var5;
      double var9 = var3.flowDirX() / var5;

      for (int var11 = -1; var11 <= 1; var11 += 2) {
         boolean var12 = false;

         for (int var13 = 1; var13 <= 16; var13++) {
            int var14 = var1 + (int)Math.round(var7 * var13 * var11);
            int var15 = var2 + (int)Math.round(var9 * var13 * var11);
            if (var0.getHeight(Types.OCEAN_FLOOR_WG, var14, var15) - 1 >= var4) {
               var12 = true;
               break;
            }
         }

         if (!var12) {
            return false;
         }
      }

      return true;
   }

   private static boolean b(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5) {
      MutableBlockPos var6 = new MutableBlockPos();

      for (int var7 = var3; var7 >= Math.max(var5, var3 - 3); var7--) {
         var6.set(var1, var7, var2);
         BlockState var8 = var0.getBlockState(var6);
         if (var8.isAir() || !var8.getFluidState().isEmpty()) {
            return false;
         }
      }

      for (int var13 = 0; var13 < 4; var13++) {
         int var14 = var1 + (var13 == 0 ? 1 : (var13 == 1 ? -1 : 0));
         int var9 = var2 + (var13 == 2 ? 1 : (var13 == 3 ? -1 : 0));
         int var10 = var0.getHeight(Types.OCEAN_FLOOR_WG, var14, var9) - 1;

         for (int var11 = var3 + 1; var11 <= Math.min(var4, var10); var11++) {
            var6.set(var14, var11, var9);
            BlockState var12 = var0.getBlockState(var6);
            if (var12.isAir() || !var12.getFluidState().isEmpty()) {
               return false;
            }
         }
      }

      return true;
   }

   private static int a(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4, int var5) {
      double var6 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var6 < 1.0E-6) {
         return var5;
      }

      double var8 = -var3.flowDirZ() / var6;
      double var10 = var3.flowDirX() / var6;
      double var12 = var3.width() + 2.0;
      int var14 = Integer.MIN_VALUE;

      for (int var15 = -1; var15 <= 1; var15 += 2) {
         int var16 = var1 + (int)Math.round(var8 * var12 * var15);
         int var17 = var2 + (int)Math.round(var10 * var12 * var15);
         if (a(var16, var17)) {
            var14 = Math.max(var14, var0.getHeight(Types.OCEAN_FLOOR_WG, var16, var17) - 1);
         }
      }

      return var14 == Integer.MIN_VALUE ? var5 : Mth.clamp(var14, var4, var5);
   }

   private static int b(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4) {
      double var5 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var5 < 1.0E-6) {
         return Integer.MAX_VALUE;
      }

      double var7 = -var3.flowDirZ() / var5;
      double var9 = var3.flowDirX() / var5;
      double var11 = var3.width();
      int var13 = Integer.MAX_VALUE;

      for (double var14 = var11 + 1.0; var14 <= var11 + 4.0; var14 += 3.0) {
         for (int var16 = -1; var16 <= 1; var16 += 2) {
            int var17 = var1 + (int)Math.round(var7 * var14 * var16);
            int var18 = var2 + (int)Math.round(var9 * var14 * var16);
            if (a(var17, var18)) {
               var13 = Math.min(var13, var0.getHeight(Types.OCEAN_FLOOR_WG, var17, var18) - 1);
            }
         }
      }

      return var13 == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(var13, var4 - 1);
   }

   private static boolean a(RiverEngine var0, int var1, int var2, ColumnPlan var3) {
      double var4 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var4 < 1.0E-6) {
         return false;
      }

      double var6 = -var3.flowDirZ() / var4;
      double var8 = var3.flowDirX() / var4;
      double var10 = var1 + 0.5;
      double var12 = var2 + 0.5;
      boolean var14 = false;
      boolean var15 = false;

      for (int var16 = 2; var16 <= 8 && (!var14 || !var15); var16 += 2) {
         if (!var14 && a(var0, var10 + var6 * var16, var12 + var8 * var16)) {
            var14 = true;
         }

         if (!var15 && a(var0, var10 - var6 * var16, var12 - var8 * var16)) {
            var15 = true;
         }
      }

      return var14 && var15;
   }

   private static boolean a(RiverEngine var0, double var1, double var3) {
      return var0.plan((int)Math.round(var1), (int)Math.round(var3)).distance() <= 2.0;
   }

   private static double a(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = var0 + var2 * var4;
      double var14 = 0.45 * var6;
      if (!(var2 <= var14) && !(var8 <= var14)) {
         double var16 = var0 + var14 * var4;
         if (var10 <= var16) {
            return var12;
         }

         double var18 = Math.min(1.0, (var10 - var0) / 8.0);
         double var20 = 16.0 * var18;
         if (var20 <= 1.0E-6) {
            return var12;
         }

         double var22 = Math.min(1.0, (var2 - var14) / (var8 - var14));
         double var24 = var16 + (var10 - var16) * Math.log1p(var20 * var22) / Math.log1p(var20);
         double var26 = Math.min(1.0, var22 / 0.35);
         var26 = var26 * var26 * (3.0 - 2.0 * var26);
         return Math.max(var12, var12 + (var24 - var12) * var26);
      } else {
         return var12;
      }
   }

   private static int[] a() {
      String var0 = System.getProperty("streamsreflowing.carvetrace");
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim()), var1.length > 2 ? Integer.parseInt(var1[2].trim()) : 24};
         } catch (Exception var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void a(ServerLevel var0, BlockPos var1, int var2) {
      RiverEngine var3 = StreamsWorldgen.engine(var0);
      if (var3 != null) {
         int var4 = (var1.getX() >> 4) - var2 << 4;
         int var5 = (var1.getZ() >> 4) - var2 << 4;
         int var6 = (var2 * 2 + 1) * 16;
          TreeMap<Integer, Integer> var7 = new TreeMap<>();
         StringBuilder var8 = new StringBuilder();
         int var9 = 0;
         int var10 = 0;
         Constants.LOG
            .info(
               "[SR-WATERMAP] ==== attribution around ({},{}) r={} chunks, plate {} ====",
               new Object[]{var1.getX(), var1.getZ(), var2, var3.plateAt(var1.getX(), var1.getZ())}
            );

         for (int var11 = 0; var11 < var6; var11++) {
            StringBuilder var12 = new StringBuilder();

            for (int var13 = 0; var13 < var6; var13++) {
               if (var13 > 0 && (var13 & 15) == 0) {
                  var12.append('|');
               }

               int var14 = var4 + var13;
               int var15 = var5 + var11;
               int var16 = var0.getHeight(Types.MOTION_BLOCKING, var14, var15) - 1;
               int var17 = var0.getHeight(Types.OCEAN_FLOOR, var14, var15) - 1;
               boolean var18 = var16 > var17;
               ColumnPlan var19 = var3.planLean(var14, var15);
               boolean var20 = var19 != ColumnPlan.NONE && var19.inChannel();
               boolean var21 = var19 != ColumnPlan.NONE && !var20 && var18 && Math.abs(var16 - var19.waterSurfaceY()) <= 2;
               Lake var22 = var3.lakeAt(var14, var15);
               Lake var23 = var3.lakeNear(var14, var15, 2);
               boolean var24 = var23 != null && var23.digDepth() == 0 && var17 + 1 < var23.level();
               char var25;
               if (var18) {
                  if (var20) {
                     var25 = 'S';
                  } else if (var22 != null) {
                     var25 = 'L';
                  } else if (var24) {
                     var25 = 'l';
                  } else if (var21) {
                     var25 = 'c';
                  } else {
                     var25 = 'O';
                     var9++;
                     var7.merge(var16, 1, Integer::sum);
                     if (var8.length() < 600) {
                        var8.append(String.format(" (%d,%d,%d)", var14, var16, var15));
                     }
                  }
               } else if (!var20 && !var24) {
                  var25 = '.';
               } else {
                  var25 = 'm';
                  var10++;
               }

               var12.append(var25);
            }

            Constants.LOG.info("[SR-WATERMAP] {}", var12);
            if ((var11 & 15) == 15 && var11 < var6 - 1) {
               Constants.LOG.info("[SR-WATERMAP] {}", "-".repeat(var6 + var2 * 2));
            }
         }

         Constants.LOG.info("[SR-WATERMAP] orphans={} (levels {}), expected-but-dry={}; orphan samples:{}", new Object[]{var9, var7, var10, var8});
      }
   }

   public static void armTraceOverride(int var0, int var1, int var2) {
      aU = new int[]{var0, var1, var2};
   }

   public static void waterMap(ServerLevel var0, BlockPos var1, int var2) {
      a(var0, var1, var2);
   }

   private static boolean b(int var0, int var1) {
      int[] var2 = aU != null ? aU : aT;
      return var2 != null && var0 == var2[0] && var1 == var2[1];
   }

   private static void a(int var0, int var1, String var2) {
      Constants.LOG.info("[SR-STACK] ({},{}) {}", new Object[]{var0, var1, var2});
   }

   private static int a(int var0, int var1, String var2, int var3, int var4) {
      if (b(var0, var1) && var3 != var4) {
         a(var0, var1, "  " + var2 + ": " + var3 + " -> " + var4);
      }

      return var4;
   }

   public static int[] activeCarveTrace() {
      return aU != null ? aU : aT;
   }

   private static boolean c(int var0, int var1) {
      int[] var2 = aU != null ? aU : aT;
      return var2 != null && Math.abs(var0 - var2[0]) <= var2[2] && Math.abs(var1 - var2[1]) <= var2[2];
   }

   private static void b(int var0, int var1, String var2) {
      Constants.LOG.info("[SR-CARVETRACE] ({},{}) {}", new Object[]{var0, var1, var2});
   }

   public static int adjacentCoveringLakeLevel(RiverEngine var0, int var1, int var2) {
      int var3 = Integer.MIN_VALUE;

      for (int var4 = 0; var4 < 8; var4++) {
         for (int var5 = 2; var5 <= 4; var5 += 2) {
            Lake var6 = var0.lakeAt(var1 + aV[var4] * var5, var2 + aW[var4] * var5);
            if (var6 != null && var6.level() > var3) {
               var3 = var6.level();
            }
         }
      }

      return var3;
   }

   private static double d(int var0, int var1) {
      long var2 = var0 * -7046029254386353131L ^ var1 * -4417276706812531889L;
      var2 ^= var2 >>> 30;
      var2 *= -4658895280553007687L;
      var2 ^= var2 >>> 27;
      var2 *= -7723592293110705685L;
      var2 ^= var2 >>> 31;
      return (var2 >>> 11) * 1.110223E-16F;
   }

   private static double c(int var0, int var1, int var2) {
      int var3 = Math.floorDiv(var0, var2);
      int var4 = Math.floorDiv(var1, var2);
      double var5 = (double)(var0 - var3 * var2) / var2;
      double var7 = (double)(var1 - var4 * var2) / var2;
      var5 = var5 * var5 * (3.0 - 2.0 * var5);
      var7 = var7 * var7 * (3.0 - 2.0 * var7);
      double var9 = d(var3, var4);
      double var11 = d(var3 + 1, var4);
      double var13 = d(var3, var4 + 1);
      double var15 = d(var3 + 1, var4 + 1);
      double var17 = var9 + (var11 - var9) * var5;
      double var19 = var13 + (var15 - var13) * var5;
      return var17 + (var19 - var17) * var7;
   }

   static double a(RiverEngine var0, int var1, int var2) {
      return ChannelBed.overlapFactor(var0.altPlan(var1, var2));
   }

   public static double braidOverlapPublic(RiverEngine var0, int var1, int var2) {
      return a(var0, var1, var2);
   }

   static double a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      return blockStateCohesion(var0.getBlockState(var1.set(var2, var3, var4)));
   }

   public static double blockStateCohesion(BlockState var0) {
      if (!var0.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
         return 1.0;
      }

      float var1 = var0.getBlock().defaultDestroyTime();
      return Mth.clamp((var1 - 0.5) * 1.6, 0.0, 1.0);
   }

   static double a(WorldGenLevel var0, RiverCarverFeature.BankEnv var1, MutableBlockPos var2, int var3, int var4, int var5) {
      return Math.min(var1.cohesion(), a(var0, var2, var3, var4, var5));
   }

   public static double effectiveCohesionPublic(WorldGenLevel var0, RiverCarverFeature.BankEnv var1, int var2, int var3, int var4) {
      return a(var0, var1, new MutableBlockPos(), var2, var3, var4);
   }

   static RiverCarverFeature.BankEnv a(WorldGenLevel var0, int var1, int var2, int var3, Map<Long, RiverCarverFeature.BankEnv> var4, StreamsConfig var5) {
      HydraulicGeometry var6 = var5.hydraulics();
      if (var6 != null && var6.shapesBed()) {
         long var7 = (long)(var1 >> 2) << 32 ^ var2 >> 2 & 4294967295L;
         RiverCarverFeature.BankEnv var9 = (RiverCarverFeature.BankEnv)var4.get(var7);
         if (var9 != null) {
            return var9;
         }

         Holder var10 = var0.getUncachedNoiseBiome(var1 >> 2, var3 >> 2, var2 >> 2);
         double var11 = Mth.clamp((0.5 - ((Biome)var10.value()).getBaseTemperature()) / 0.5, 0.0, 1.0);
         RiverCarverFeature.BankEnv var13 = new RiverCarverFeature.BankEnv(bankCohesion(var0, var10), Services.PLATFORM.biomeWetness(var10), var11);
         var4.put(var7, var13);
         return var13;
      } else {
         return RiverCarverFeature.BankEnv.FIRM;
      }
   }

   static double b(WorldGenLevel var0, int var1, int var2, int var3, Map<Long, RiverCarverFeature.BankEnv> var4, StreamsConfig var5) {
      return a(var0, var1, var2, var3, var4, var5).cohesion();
   }

   private static double d(int var0, int var1, int var2) {
      long var3 = var0 * -7046029254386353131L ^ var1 * -4417276706812531889L ^ var2 * 1609587929392839161L;
      var3 ^= var3 >>> 30;
      var3 *= -4658895280553007687L;
      var3 ^= var3 >>> 27;
      var3 *= -7723592293110705685L;
      var3 ^= var3 >>> 31;
      return (var3 >>> 11) * 1.110223E-16F;
   }

   private static void b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5) {
      for (int var6 = 0; var6 < 4; var6++) {
         int var7 = var2 + (var6 == 0 ? 1 : (var6 == 1 ? -1 : 0));
         int var8 = var4 + (var6 == 2 ? 1 : (var6 == 3 ? -1 : 0));
         BlockState var9 = var0.getBlockState(var1.set(var7, var3, var8));
         if (var9.is(Blocks.SNOW) && !var9.isCollisionShapeFullBlock(var0, var1)) {
            var0.setBlock(var1, var5, 2);
         }
      }
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int[] var2, int var3, int var4, int var5, BlockState var6) {
      boolean var7 = false;

      for (int var11 : var2) {
         if (var11 != Integer.MIN_VALUE) {
            var7 = true;
            break;
         }
      }

      if (var7) {
         boolean[] var13 = new boolean[256];

         for (int var14 = 0; var14 < 256; var14++) {
            if (var2[var14] != Integer.MIN_VALUE) {
               int var15 = var2[var14];

               for (int var16 = 0; var16 < 4; var16++) {
                  int var12 = e(var14, var16);
                  if (var12 >= 0 && !var13[var12] && var2[var12] == Integer.MIN_VALUE) {
                     a(var0, var1, var12, var15, var13, var3, var4, var5, var6);
                  }
               }
            }
         }
      }
   }

   private static int e(int var0, int var1) {
      int var2 = var0 >> 4;
      int var3 = var0 & 15;
      int var4 = var2 + (var1 == 0 ? 1 : (var1 == 1 ? -1 : 0));
      int var5 = var3 + (var1 == 2 ? 1 : (var1 == 3 ? -1 : 0));
      return var4 >= 0 && var4 <= 15 && var5 >= 0 && var5 <= 15 ? var4 << 4 | var5 : -1;
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, boolean[] var4, int var5, int var6, int var7, BlockState var8) {
       ArrayList<Integer> var9 = new ArrayList<>();
      ArrayDeque var10 = new ArrayDeque();
      boolean[] var11 = new boolean[256];
      var10.add(var2);
      var11[var2] = true;
      boolean var12 = true;

      while (!var10.isEmpty() && var12) {
         int var13 = (Integer)var10.poll();
         int var14 = var5 + (var13 >> 4);
         int var15 = var6 + (var13 & 15);
         if (var0.getBlockState(var1.set(var14, var3, var15)).isAir()) {
            var9.add(var13);
            if (var9.size() > 24 || !g(var0, var1, var14, var3, var15, var7)) {
               var12 = false;
               break;
            }

            for (int var16 = 0; var16 < 4; var16++) {
               int var17 = e(var13, var16);
               if (var17 < 0) {
                  var12 = false;
                  break;
               }

               if (!var11[var17]) {
                  var11[var17] = true;
                  var10.add(var17);
               }
            }
         }
      }

      for (int var21 : var9) {
         var4[var21] = true;
      }

      if (var12 && !var9.isEmpty()) {
         for (int var22 : var9) {
            int var23 = var5 + (var22 >> 4);
            int var24 = var6 + (var22 & 15);

            for (int var25 = var3; var25 > var7; var25--) {
               var1.set(var23, var25, var24);
               BlockState var18 = var0.getBlockState(var1);
               if (!var18.isAir()) {
                  break;
               }

               a(var0, (BlockPos)var1, var8);
            }
         }
      }
   }

   private static boolean g(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      for (int var6 = var3 - 1; var6 >= Math.max(var5, var3 - 6); var6--) {
         BlockState var7 = var0.getBlockState(var1.set(var2, var6, var4));
         if (!var7.isAir() && var7.getFluidState().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static int a(RiverEngine var0, WorldGenLevel var1, ColumnPlan var2, int var3, int var4, int var5) {
      int var6 = a(var1, var3, var4);
      if (var6 == var5) {
         return Integer.MIN_VALUE;
      }

      int var7 = var2.waterSurfaceY();
      double var8 = var2.distance() - var2.width();
      double var10 = Math.min(2.0, var0.config().bankSlope() + var0.config().bankSteepFactor() * Math.pow(var2.slope(), 0.8));
      double var12 = a(var7, var8, var10, var2.width(), Math.max(5.0, var2.width() * var0.config().bankWidth()), var6);
      double var14 = var7 + var2.bankBlend() * (var6 - var7);
      return Mth.clamp((int)Math.floor(Math.max(var12, var14)), var7, Math.max(var7, var6));
   }

   private static int a(ColumnPlan var0, ColumnPlan var1) {
      ColumnPlan var2 = var0.waterSurfaceY() >= var1.waterSurfaceY() ? var0 : var1;
      int var3 = Math.min(var0.waterSurfaceY(), var1.waterSurfaceY());
      double var4 = Math.max(0.0, var2.distance() - var2.width());
      int var6 = (int)Math.floor(Math.max(0.0, var4 - 3.0) * 2.0);
      return Math.max(var3, var2.waterSurfaceY() + 1 - var6);
   }

   private static int a(RiverEngine var0, ColumnPlan var1, int var2, double var3) {
      double var5 = var1.distance() - var1.width();
      if (var5 <= 0.0) {
         return Integer.MAX_VALUE;
      }

      int var7 = Math.min(var1.waterSurfaceY(), var2);
      double var8 = Math.min(2.0, var0.config().bankSlope() + var0.config().bankSteepFactor() * Math.pow(var1.slope(), 0.8));
      double var10 = a(var7, var5, var8, var1.width(), Math.max(5.0, var1.width() * var0.config().bankWidth()), var2);
      double var12 = var7 + var1.bankBlend() * (var2 - var7);
      return Mth.clamp((int)Math.floor(Math.max(var10, var12) + var3), var7, Math.max(var7, var2));
   }

   private static void b(WorldGenLevel var0, int var1, int var2, int[] var3) {
      if (aX) {
         boolean var4 = false;

         for (int var8 : var3) {
            if (var8 > 0) {
               var4 = true;
               break;
            }
         }

         if (var4) {
            Map var9 = aY.computeIfAbsent(var0.getLevel().dimension(), var0x -> new ConcurrentHashMap<>());
            if (var9.size() >= 2048) {
               Iterator var10 = var9.keySet().iterator();
               if (var10.hasNext()) {
                  var10.next();
                  var10.remove();
               }
            }

            var9.put((long)(var1 >> 4) << 32 ^ var2 >> 4 & 4294967295L, (int[])var3.clone());
         }
      }
   }

   public static void drainDeferredSeams(ServerLevel var0) {
      if (aX) {
         Map var1 = aY.get(var0.dimension());
         if (var1 != null && !var1.isEmpty()) {
            RiverEngine var2 = StreamsWorldgen.engine(var0);
            if (var2 != null) {
               int var3 = 0;
               Iterator var4 = var1.entrySet().iterator();

               while (var4.hasNext() && var3 < 2) {
                  Entry var5 = (Entry)var4.next();
                  long var6 = (Long)var5.getKey();
                  int var8 = (int)(var6 >> 32);
                  int var9 = (int)var6;
                  int var10 = var8 << 4;
                  int var11 = var9 << 4;
                  if (!var0.hasChunk(var8, var9)) {
                     var4.remove();
                  } else {
                     boolean var12 = true;

                     for (int var13 = -1; var13 <= 1 && var12; var13++) {
                        for (int var14 = -1; var14 <= 1 && var12; var14++) {
                           var12 = b(var0, var10 + var13 * 16, var11 + var14 * 16);
                        }
                     }

                     if (var12) {
                        var4.remove();
                        int var23 = var0.getMinBuildHeight();
                        int var24 = var0.getMaxBuildHeight() - 1;
                        int[] var15 = new int[256];
                        ColumnPlan[] var16 = new ColumnPlan[256];
                        BlockState[] var17 = new BlockState[256];
                        BlockState[] var18 = new BlockState[256];
                        int[] var19 = new int[256];
                        Arrays.fill(var19, Integer.MAX_VALUE);

                        for (int var20 = 0; var20 < 16; var20++) {
                           for (int var21 = 0; var21 < 16; var21++) {
                              int var22 = var20 * 16 + var21;
                              var15[var22] = var0.getHeight(Types.OCEAN_FLOOR_WG, var10 + var20, var11 + var21) - 1;
                              var16[var22] = var2.plan(var10 + var20, var11 + var21);
                              var17[var22] = a(var10 + var20, var11 + var21, 0);
                              var18[var22] = a(var10 + var20, var11 + var21, 256);
                           }
                        }

                        a(var0, var2, var10, var11, var15, new boolean[256], var17, var18, (int[])var5.getValue(), var16, var23, var24);
                        var3++;
                     }
                  }
               }
            }
         }
      }
   }

   public static void forgetDeferredSeams(ServerLevel var0) {
      aY.remove(var0.dimension());
   }

   record BankEnv(double cohesion, double wetness, double cold) {
      static final RiverCarverFeature.BankEnv FIRM = new RiverCarverFeature.BankEnv(1.0, 0.5, 0.0);

      BankEnv {
wetness = wetness;
}
   }
}
