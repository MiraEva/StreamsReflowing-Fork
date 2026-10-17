package dev.streamsreflowing.core.river;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.ColumnCache;
import dev.streamsreflowing.core.ColumnKey;
import dev.streamsreflowing.core.Relief;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.api.DeadEndResolver;
import dev.streamsreflowing.core.api.Sink;
import dev.streamsreflowing.core.build.BuildGate;
import dev.streamsreflowing.core.config.HydraulicGeometry;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.diag.HeightCensus;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.math.Hashing;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.terrain.BasinMemoSampler;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.core.terrain.ReadLattice;
import dev.streamsreflowing.worldgen.regionprep.PrepDiag;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

public final class NetworkTracer {
   private static final int[] b = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] c = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final double d = 0.001;
   private static final int e = 64;
   private static final double f = 1.0;
   private static final double g = 6.0;
   private static final double h = 2.0;
   private static final double i = 1.0;
   private static final double j = 2.0;
   private static final double k = 5.0;
   private static final double l = 40.0;
   private static final double m = 4.0;
   private static final double n = 6.0;
   private static final double o = 3.0;
   private static final double p = 4.0;
   private static final double q = 8.0;
   private static final double r = 2.0;
   private static final double s = 2.0;
   private static final int t = 4;
   private static final int u = 4;
   private static final double v = 8.0;
   private static final double w = 24.0;
   private static final int x = 2;
   private static final int y = 1;
   private static final double z = 1.0;
   private static final int A = 4;
   private static final int B = 4;
   private static final int C = 24;
   private static final int D = 1;
   private static final int E = 2;
   private static final ThreadLocal<int[]> F = ThreadLocal.withInitial(() -> new int[10]);
   private static final int G = 0;
   private static final int H = 1;
   private static final int I = 2;
   private static final int J = 3;
   private static final int K = 4;
   private static final int L = 5;
   private static final int M = 6;
   private static final int N = 7;
   private static final int O = 8;
   private static final int P = 9;
   private static final boolean Q = Boolean.getBoolean("streamsreflowing.mouthsingle");
   private static final boolean R = Boolean.getBoolean("streamsreflowing.legacyshaping");
   private static final double S = 0.25;
   private static final int T = 16;
   private static final int U = 24;
   private static final int V = 24;
   private static final double W = 8.0;
   private static final int X = 8;
   private static final int Y = 5;
   private static final double Z = 24.0;
   private static final double aa = 3.0;
   private static final int ab = 2;
   private static final double ac = 32.0;
   private static final int[] ad = a();
   private static final int ae = 96;
   private static final int[] af = b();
   private static final double ag = 0.5;
   private static final double ah = 0.5;
   private static final double ai = 1.5;
   private static final boolean aj = Boolean.getBoolean("streamsreflowing.deepdiag");
   private static final AtomicLongArray ak = new AtomicLongArray(7);
   private static final int al = 0;
   private static final int am = 1;
   private static final int an = 2;
   private static final int ao = 3;
   private static final int ap = 4;
   private static final int aq = 5;
   private static final int ar = 6;
   private static final String[] as = new String[]{"snap", "followGround", "meander", "confine", "smooth", "emit", "band"};
   private static final AtomicLong at = new AtomicLong();
   private static final ThreadLocal<long[]> au = ThreadLocal.withInitial(() -> new long[8]);
   private static final int av = 0;
   private static final int aw = 1;
   private static final int ax = 2;
   private static final int ay = 3;
   private static final int az = 4;
   private static final int aA = 5;
   private static final int aB = 6;
   private static final int aC = 7;
   private static final String[] aD = new String[]{"bfs+qualify", "borderPre", "probes", "redetect", "poolAt", "flood", "emit", "floodWarm"};
   private static final double aE = 6.0;
   private static final double aF = 3.0;
   private static final double aG = 2.0;
   private static final double aH = 1.5;
   private static final double aI = 2.0;
   private static final double aJ = Integer.getInteger("streamsreflowing.lakeminarea", 512).intValue();
   private static final double aK = 1024.0;
   private static final double aL = 3.0;
   private static final double aM = 4.0;
   private static final double aN = 4.0;
   private static final int aO = 48;
   private static final int aP = 2;
   private static final double aQ = 4.0;
   private static final int aR = Integer.getInteger("streamsreflowing.lakethinband", 8);
   private static final double aS = Integer.getInteger("streamsreflowing.lakethinsoft", 20).intValue() / 10.0;
   private static final double aT = 5.0;
   private static final int aU = 8;
   private static final int aV = 107749;
   private static final int aW = 2;
   private static final double aX = 16.0;
   private static final double aY = 0.03;
   private static final int aZ = 3;
   private static final double ba = 0.3;
   private static final int bb = 6;
   private static final double bc = 1.0;
   private static final double bd = 0.5;
   private static final double be = 3.5;
   private static final double bf = 3.0;
   private static final double bg = 1.0;
   private static final double bh = 12.0;
   private static final boolean bi = Boolean.getBoolean("streamsreflowing.legacyseatail");
   private static final boolean bj = System.getProperty("streamsreflowing.indexcheck") != null;
   private static final boolean bk = Boolean.getBoolean("streamsreflowing.deadendaudit");
   private static final int bl = 48;
   private static final double bm = 8.0;
   private static final boolean bn = Boolean.getBoolean("streamsreflowing.fillaudit");
   private static final AtomicLongArray bo = new AtomicLongArray(3);
   private static final ThreadLocal<AtomicLongArray> bp = ThreadLocal.withInitial(() -> new AtomicLongArray(9));
   private static final int bq = 0;
   private static final int br = 1;
   private static final int bs = 2;
   private static final int bt = 3;
   private static final int bu = 4;
   private static final int bv = 5;
   private static final int bw = 6;
   private static final int bx = 7;
   private static final int by = 8;
   private static final int bz = 16;
   private static final int bA = -1;
   private static final double bB = 4.0;
   static final LongAdder a = new LongAdder();
   private static final long bC = 4611686018427387904L;
   private static final double bD = 2.0;
   private static final double bE = 12.0;
   private static final double bF = 48.0;
   private static final double bG = 0.25;
   private static final double bH = 16.0;
   private static final double bI = 0.12;
   private static final double bJ = 0.3;
   private static final double bK = 0.15;
   private static final double bL = 0.25;
   private static final double bM = 0.18;
   private static final double bN = 2.5;
   private static final double bO = 64.0;
   private static final double bP = 0.04;
   private static final int bQ = 32;
   private static final double bR = 64.0;
   private static final double bS = 32.0;
   private static final double bT = 0.03;
   private static final double bU = 2.0;
   private static final int bV = 2;
   private static final int bW = 3;
   private static final boolean bX = Boolean.getBoolean("streamsreflowing.legacyseaend");
   private static final int bY = 0;
   private static final int bZ = 4;
   private static final int ca = 5;
   private static final int cb = 6;
   private static final Map<LowResGrid, AtomicIntegerArray> cc = Collections.synchronizedMap(new WeakHashMap<>());
   private static final double cd = 4.0;

   private NetworkTracer() {
   }

   public static RiverNetwork build(PlateDomain var0, TerrainSampler var1, WetnessSampler var2, StreamsConfig var3, long var4) {
      long var6 = System.nanoTime();
      LowResGrid var8 = new LowResGrid(var0, var1, var3);
      long var9 = System.nanoTime();
      int var11 = var8.size();
      int var12 = var3.seaLevel();
      if (bn) {
         a(var8, var1, var0);
      }

      boolean var13 = Boolean.getBoolean("streamsreflowing.caldiag") || aj || PrepDiag.loud();
      int var14 = HeightCensus.enter(9);
      long var15 = System.nanoTime();
      int[] var17 = new int[]{0};
      boolean[] var18 = new boolean[var11];
      ArrayDeque<Integer> var19 = new ArrayDeque<>();
      int[] var20 = new int[var11];
      Arrays.fill(var20, 0);
      LowResGrid.parallelRange(var11, (var3x, var4x) -> {
         for (int var5 = var3x; var5 < var4x; var5++) {
            if (var8.inDomain(var5) && var8.height(var5) != -1000.0) {
               var20[var5] = var1.waterBiomeClass(Math.round(var8.worldX(var8.gi(var5))), Math.round(var8.worldZ(var8.gj(var5))));
            }
         }
      });

      for (int var21 = 0; var21 < var11; var21++) {
         if (var8.inDomain(var21) && var8.height(var21) != -1000.0) {
            int var22 = var20[var21];
            if (var22 == 1) {
               var8.overrideHeight(var21, -1001.0);
               var8.markRiverOutlet(var21);
               var18[var21] = true;
               var19.add(var21);
            } else if (var22 == 2) {
               var8.overrideHeight(var21, -1000.0);
               var18[var21] = true;
            }
         }
      }

      if (Boolean.getBoolean("streamsreflowing.caldiag")) {
         int var183 = 0;

         for (int var185 = 0; var185 < var11; var185++) {
            if (var8.riverOutlet(var185)) {
               var183++;
            }
         }

         Constants.LOG.info("[SR-RTF] plate {}: {} river-outlet cells after tier 1", var0.key(), var183);
      }

      long var184 = System.nanoTime();
      if (!Boolean.getBoolean("streamsreflowing.nohighrivers")) {
         for (int var23 = 0; var23 < var11; var23++) {
            if (!var18[var23] && var8.inDomain(var23) && !(var8.height(var23) <= var12 + 8)) {
               int var24 = 0;
               int var25 = var8.gi(var23);
               int var26 = var8.gj(var23);

               for (int var27 = 0; var27 < 8; var27++) {
                  int var28 = var25 + b[var27];
                  int var29 = var26 + c[var27];
                  if (var8.valid(var28, var29) && var8.inDomain(var8.idx(var28, var29)) && var8.height(var8.idx(var28, var29)) > var8.height(var23)) {
                     var24++;
                  }
               }

               if (var24 >= 5) {
                  var18[var23] = true;
                  var17[0]++;
                  if (var1.waterBiomeClassAt(Math.round(var8.worldX(var25)), (int)Math.round(var8.height(var23)), Math.round(var8.worldZ(var26))) == 1) {
                     var8.overrideHeight(var23, -1001.0);
                     var8.markRiverOutlet(var23);
                     var19.add(var23);
                  }
               }
            }
         }

         int var186 = 0;

         while (!var19.isEmpty()) {
            int var188 = (Integer)var19.poll();
            int var189 = var8.gi(var188);
            int var192 = var8.gj(var188);

            for (int var196 = 0; var196 < 8; var196++) {
               int var199 = var189 + b[var196];
               int var202 = var192 + c[var196];
               if (var8.valid(var199, var202)) {
                  int var30 = var8.idx(var199, var202);
                  if (!var18[var30] && var8.inDomain(var30) && var8.height(var30) != -1000.0) {
                     var18[var30] = true;
                     var17[0]++;
                     if (var1.waterBiomeClassAt(Math.round(var8.worldX(var199)), (int)Math.round(var8.height(var30)), Math.round(var8.worldZ(var202))) == 1) {
                        var8.overrideHeight(var30, -1001.0);
                        var8.markRiverOutlet(var30);
                        var19.add(var30);
                        var186++;
                     }
                  }
               }
            }
         }

         if (var186 > 0) {
            Constants.LOG.debug("[SR-MERGE] high-river outlets: {} extra river cells found above the sea-level probe", var186);
         }
      }

      long var187 = System.nanoTime();
      if (aj) {
         int var190 = Math.max(1, var11 / 64);
         int var193 = 0;
         double var197 = 0.0;
         double var203 = 0.0;
         int var31 = 0;
         int var32 = 0;
         int var33 = 0;

         for (int var34 = 0; var34 < var11; var34 += var190) {
            if (var8.inDomain(var34)) {
               int var35 = var8.worldX(var8.gi(var34));
               int var36 = var8.worldZ(var8.gj(var34));
               double var37 = var8.height(var34);
               if (var37 != -1000.0 && var37 != -1001.0) {
                  double var39 = var1.groundHeightExact(var35, var36);
                  if (!Double.isNaN(var39)) {
                     double var41 = var37 - var39;
                     var193++;
                     var197 += Math.abs(var41);
                     if (Math.abs(var41) > 2.0) {
                        var33++;
                     }

                     if (Math.abs(var41) > Math.abs(var203)) {
                        var203 = var41;
                        var31 = var35;
                        var32 = var36;
                     }
                  }
               }
            }
         }

         if (var193 > 0) {
            Constants.LOG
               .info(
                  "[SR-SAMPLEAUDIT] plate {}: sampled-vs-exact over {} cells -- meanAbs {}, >2 blocks {} ({}%), worst {} at ({},{}); sampler baked={} hasEstimate={}",
                  new Object[]{
                     var0.key(),
                     var193,
                     String.format(Locale.ROOT, "%.2f", var197 / var193),
                     var33,
                     String.format(Locale.ROOT, "%.0f", 100.0 * var33 / var193),
                     String.format(Locale.ROOT, "%+.1f", var203),
                     var31,
                     var32,
                     var1.bakedCalibration(),
                     var1.hasEstimate()
                  }
               );
         }
      }

      ArrayList<Integer> var191 = new ArrayList<>();

      for (int var194 = 0; var194 < var8.nz; var194++) {
         for (int var198 = 0; var198 < var8.nx; var198++) {
            int var200 = var8.idx(var198, var194);
            if (var8.inDomain(var200)) {
               var191.add(var200);
            }
         }
      }

      long var195 = System.nanoTime();
      double[] var201 = new double[var11];
      boolean[] var204 = new boolean[var11];
      PriorityQueue<Integer> var205 = new PriorityQueue<>((var1x, var2x) -> Double.compare(var201[var1x], var201[var2x]));
      long var206 = System.nanoTime();
      long[] var207 = new long[]{var206};
      int[] var208 = new int[]{0};
      boolean[] var209 = new boolean[var11];
      ArrayList<Integer> var210 = new ArrayList<>();

      for (int var38 : var191) {
         if (var8.riverOutlet(var38)) {
            int var219 = var8.gi(var38);
            int var40 = var8.gj(var38);

            for (int var224 = 0; var224 < 8; var224++) {
               int var42 = var219 + b[var224];
               int var43 = var40 + c[var224];
               if (var8.valid(var42, var43)) {
                  int var44 = var8.idx(var42, var43);
                  if (var8.inDomain(var44) && !var8.riverOutlet(var44)) {
                     var209[var38] = true;
                     var210.add(var38);
                     break;
                  }
               }
            }
         }
      }

      if (!var210.isEmpty()) {
         double[] var212 = new double[var210.size()];
         ArrayList<Integer> var216 = var210;
         LowResGrid.parallelFor(var216.size(), var4x -> {
            int var5 = (Integer)var216.get(var4x);
            int var6x = Math.round(var8.worldX(var8.gi(var5)));
            int var7 = Math.round(var8.worldZ(var8.gj(var5)));
            double var8x = var1.riverSurface(var6x, var7);
            var212[var4x] = Double.isNaN(var8x) ? var1.surfaceHeight(var6x, var7) : var8x;
         });

         for (int var220 = 0; var220 < var216.size(); var220++) {
            var8.overrideHeight((Integer)var216.get(var220), var212[var220]);
         }

         var208[0] = var216.size();
         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-RTF] plate {}: {} river bank cells seeded", var0.key(), var216.size());
         }
      }

      var207[0] = System.nanoTime();

      for (int var217 : var191) {
         var201[var217] = var8.height(var217);
         if (var8.riverOutlet(var217)) {
            var204[var217] = true;
            if (var209[var217]) {
               var205.add(var217);
            }
         } else if (var8.height(var217) <= var12 || a(var8, var217)) {
            var204[var217] = true;
            var205.add(var217);
         }
      }

      while (!var205.isEmpty()) {
         int var214 = (Integer)var205.poll();
         int var218 = var8.gi(var214);
         int var221 = var8.gj(var214);

         for (int var223 = 0; var223 < 8; var223++) {
            int var225 = var218 + b[var223];
            int var228 = var221 + c[var223];
            if (var8.valid(var225, var228)) {
               int var230 = var8.idx(var225, var228);
               if (var8.inDomain(var230) && !var204[var230]) {
                  var201[var230] = Math.max(var8.height(var230), var201[var214] + 0.001);
                  var204[var230] = true;
                  var205.add(var230);
               }
            }
         }
      }

      long var215 = var207[0];
      long var222 = System.nanoTime();
      HeightCensus.restore(var14);
      var14 = HeightCensus.enter(10);
      if (var13) {
         long var226 = 1000000L;
         Constants.LOG
            .info(
               "[SR-PFLOOD] plate {} = {} ms: outlet-tier1 {} | high-rivers {} ({} probes) | audit {} | bank-seed {} ({} cells) | fill {}",
               new Object[]{
                  var0.key(),
                  (var222 - var15) / var226,
                  (var184 - var15) / var226,
                  (var187 - var184) / var226,
                  var17[0],
                  (var195 - var187) / var226,
                  (var215 - var206) / var226,
                  var208[0],
                  (var222 - var215) / var226
               }
            );
      }

      byte[] var227 = new byte[var11];
      Arrays.fill(var227, (byte)5);
      ArrayDeque<Integer> var229 = new ArrayDeque<>();

      for (int var234 : var191) {
         if (var201[var234] <= var12 - 4) {
            var227[var234] = 0;
            var229.add(var234);
         }
      }

      while (!var229.isEmpty()) {
         int var232 = (Integer)var229.poll();
         int var235 = var227[var232] + 1;
         if (var235 <= 4) {
            int var45 = var8.gi(var232);
            int var46 = var8.gj(var232);

            for (int var47 = 0; var47 < 8; var47++) {
               int var48 = var45 + b[var47];
               int var49 = var46 + c[var47];
               if (var8.valid(var48, var49)) {
                  int var50 = var8.idx(var48, var49);
                  if (var8.inDomain(var50) && var227[var50] > var235) {
                     var227[var50] = (byte)var235;
                     var229.add(var50);
                  }
               }
            }
         }
      }

      int[] var233 = new int[var11];

      for (int var238 : var191) {
         var233[var238] = -1;
         int var239 = var8.gi(var238);
         int var241 = var8.gj(var238);
         double var243 = var201[var238];
         double var247 = 0.0;
         int var52 = -1;

         for (int var53 = 0; var53 < 8; var53++) {
            int var54 = var239 + b[var53];
            int var55 = var241 + c[var53];
            if (var8.valid(var54, var55)) {
               int var56 = var8.idx(var54, var55);
               if (var8.inDomain(var56)) {
                  if (var8.riverOutlet(var56)) {
                     var52 = var56;
                     break;
                  }

                  double var57 = var243 - var201[var56];
                  if (!(var57 <= 0.0) && (!(var243 <= var12 - 2.0) || !(var57 < 2.0) || var227[var56] < var227[var238] && var227[var56] <= 4)) {
                     double var59 = var57 / (var53 % 2 == 0 ? 1.0 : 1.4142135623730951);
                     if (var59 > var247) {
                        var247 = var59;
                        var52 = var56;
                     }
                  }
               }
            }
         }

         var233[var238] = var52;
      }

      long var237 = System.nanoTime();
      var191.sort((var1x, var2x) -> {
         int var3x = Double.compare(var201[var2x], var201[var1x]);
         return var3x != 0 ? var3x : Integer.compare(var1x, var2x);
      });
      double[] var240 = new double[var11];
      double var242 = Math.max(0.0, var3.wetnessMultiplier());

      for (int var248 : var191) {
         var240[var248] = var242;
      }

      for (int var249 : var191) {
         if (var233[var249] >= 0) {
            var240[var233[var249]] = var240[var233[var249]] + var240[var249];
         }
      }

      long var246 = System.nanoTime();
      double var51 = var3.minRiverAccumulation();
      if (var3.growFromWater()) {
         var51 = Math.max(1.0, var51 * var3.growDensity());
      }

      if (!R && !var3.legacyShaping() && var3.gridSpacing() < 16) {
         double var250 = 16.0 / var3.gridSpacing();
         var51 *= var250 * var250;
      }

      boolean[] var251 = new boolean[var11];

      for (int var254 : var191) {
         var251[var254] = var201[var254] > var12 && var240[var254] >= var51 && !var8.riverOutlet(var254);
      }

      boolean[] var253 = (boolean[])var251.clone();
      long var255 = System.nanoTime();
      int var256 = (int)Math.round(var3.reconnectRadius() / Math.max(1, var3.gridSpacing()));
      if (var256 >= 1 && !var3.growFromWater()) {
         boolean var58 = false;

         for (int var60 : var191) {
            if (var251[var60] && var233[var60] < 0 && !(var201[var60] <= var12)) {
               int var61 = a(var8, var60, var201, var251, var12, var256);
               if (var61 >= 0) {
                  var233[var60] = var61;
                  var58 = true;
               }
            }
         }

         if (var58) {
            for (int var262 : var191) {
               var240[var262] = var242;
            }

            for (int var263 : var191) {
               if (var233[var263] >= 0) {
                  var240[var233[var263]] = var240[var233[var263]] + var240[var263];
               }
            }

            for (int var264 : var191) {
               var251[var264] = var201[var264] > var12 && var240[var264] >= var51;
            }
         }
      }

      long var257 = System.nanoTime();
      double var265 = var3.wetnessDryCutoff();
      double var62 = var3.wetnessWetReference();
      double var64 = var3.wetnessDryFloor();
      boolean var66 = var62 > var265;
      boolean var67 = var2.barsAnything();
      int var68 = 0;
      int var69 = 0;
      if (var66 || var67) {
         boolean[] var70 = new boolean[var11];

         for (int var72 : var191) {
            if (var251[var72] && var233[var72] >= 0 && var251[var233[var72]]) {
               var70[var233[var72]] = true;
            }
         }

         boolean[] var267 = new boolean[var11];
         ColumnCache var268 = new ColumnCache();

         for (int var74 : var191) {
            if (var251[var74] && !var70[var74]) {
               if (var67 && var2.barred(var8.worldX(var8.gi(var74)), (int)var8.height(var74), var8.worldZ(var8.gj(var74)))) {
                  var68++;
               } else if (!var66 || !a(var74, var8, var2, var268, var265, var62, var64, var4)) {
                  var267[var74] = true;
               }
            }
         }

         for (int var273 : var191) {
            if (var267[var273] && var233[var273] >= 0 && var251[var233[var273]]) {
               var267[var233[var273]] = true;
            }
         }

         for (int var274 : var191) {
            var251[var274] = var251[var274] && var267[var274];
         }
      }

      long var266 = System.nanoTime();
      HeightCensus.restore(var14);
      var14 = HeightCensus.enter(6);
      int[] var269 = new int[var11];
      Arrays.fill(var269, -1);
      ArrayList<Lake> var272 = new ArrayList<>();
      ArrayList<NetworkTracer.PendingOutflow> var275 = new ArrayList<>();
      double var75 = System.getProperty("streamsreflowing.lakeminarea") != null ? aJ : (var3.lakeMinArea() > 0.0 ? var3.lakeMinArea() : aJ);
      boolean var77 = !Double.isFinite(var75);
      int var78 = var77 ? Integer.MAX_VALUE : (int)Math.min(2147483647L, Math.max(1L, Math.round(var75 / ((double)var3.gridSpacing() * var3.gridSpacing()))));
      int var79 = var77 ? 0 : (int)Math.max(var78, Math.max(4L, Math.round(1024.0 / ((double)var3.gridSpacing() * var3.gridSpacing()))));
      boolean[] var80 = new boolean[var11];
      ArrayDeque<Integer> var81 = new ArrayDeque<>();
      ArrayList<Integer> var82 = new ArrayList<>();
      HashMap<Integer, Integer> var83 = new HashMap<>();
      boolean var84 = var1.hasEstimate() && !Boolean.getBoolean("streamsreflowing.noestslack");
      int var85 = Math.max(1, Math.min(var3.gridSpacing(), var3.lakeSampleSpacing()));
      int var86 = ReadLattice.cell();
      if (var86 > 1 && var86 <= var3.gridSpacing()) {
         var85 = var86;
      }

      int var87 = var85;
      double var88 = var84 ? 0.25 : 0.5;
      double var90 = var84 ? 1.0 : 0.0;
      double var92 = var84 ? Double.parseDouble(System.getProperty("streamsreflowing.lakedepthgate", "0")) : 0.0;
      int var94 = 0;
      int var95 = 0;
      int var96 = 0;
      int var97 = 0;
      int var98 = 0;
      int var99 = 0;
      int var100 = 0;
      int var101 = 0;
      int var102 = 0;
      int var103 = 0;
      BasinMemoSampler.resetCounters();
      bp.set(new AtomicLongArray(9));
      F.set(new int[10]);
      long var104 = 0L;
      int var106 = 0;
      boolean var107 = Boolean.getBoolean("streamsreflowing.caldiag") || aj || PrepDiag.loud();
      long[] var108 = au.get();
      Arrays.fill(var108, 0L);

      for (int var110 : var191) {
         if (var77) {
            break;
         }

         if (!var80[var110] && a(var110, var201, var8, var12, var88)) {
            var82.clear();
            var81.clear();
            var80[var110] = true;
            var81.add(var110);
            double var111 = Double.MAX_VALUE;
            int var113 = var110;
            double var114 = Double.MAX_VALUE;
            int var116 = var110;
            double var117 = 0.0;

            while (!var81.isEmpty()) {
               int var119 = (Integer)var81.poll();
               var82.add(var119);
               if (var201[var119] < var111) {
                  var111 = var201[var119];
                  var113 = var119;
               }

               if (var8.height(var119) < var114) {
                  var114 = var8.height(var119);
                  var116 = var119;
               }

               var117 = Math.max(var117, var240[var119]);
               int var120 = var8.gi(var119);
               int var121 = var8.gj(var119);

               for (int var122 = 0; var122 < 8; var122++) {
                  int var123 = var120 + b[var122];
                  int var124 = var121 + c[var122];
                  if (var8.valid(var123, var124)) {
                     int var125 = var8.idx(var123, var124);
                     if (!var80[var125] && a(var125, var201, var8, var12, var88)) {
                        var80[var125] = true;
                        var81.add(var125);
                     }
                  }
               }
            }

            var94++;
            int var301 = var191.size() / 4;
            if (var3.lakeMaxArea() > 0.0) {
               var301 = Math.min(var301, (int)Math.round(var3.lakeMaxArea() / ((double)var3.gridSpacing() * var3.gridSpacing())));
            }

            if (var82.size() > var301) {
               var95++;
            } else {
               double var309 = var111 - var114;
               boolean var319 = var117 >= var51 * 1.5;
               double var325 = (var319 ? 2.0 : 3.0) - var90;
               boolean var339 = var82.size() < var79 && a(var82, var251, var201, var8);
               if (var339) {
                  var97++;
                  if (var107) {
                     Constants.LOG
                        .info(
                           "[SR-LAKE] plate {} at {},{}: plunge pool cells={} depth={} acc={}",
                           new Object[]{
                              var0.key(),
                              var8.worldX(var8.gi(var113)),
                              var8.worldZ(var8.gj(var113)),
                              var82.size(),
                              String.format("%.1f", var309),
                              String.format("%.0f", var117)
                           }
                        );
                  }
               }

               int var126 = var339 ? 1 : var78;
               if (!(var309 < var325) && var82.size() >= var126 && !(var117 < var51)) {
                  if (!var319 && var309 < var92) {
                     var100++;
                  } else {
                     double var127 = Math.sqrt(var82.size()) * var3.gridSpacing();
                     if (var114 < var3.seaLevel() && var309 > 0.5 * var127) {
                        var101++;
                        if (var107) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEREJ] plate {} shaft-like: floor={} sea={} depth={} width={} cells={}",
                                 new Object[]{
                                    var0.key(),
                                    String.format(Locale.ROOT, "%.1f", var114),
                                    var3.seaLevel(),
                                    String.format(Locale.ROOT, "%.1f", var309),
                                    String.format(Locale.ROOT, "%.0f", var127),
                                    var82.size()
                                 }
                              );
                        }
                     } else {
                        if (var67) {
                           boolean var129 = false;

                           for (int var131 : var82) {
                              if (var251[var131]) {
                                 var129 = true;
                                 break;
                              }
                           }

                           if (!var129 && var2.barred(var8.worldX(var8.gi(var116)), (int)Math.floor(var114), var8.worldZ(var8.gj(var116)))) {
                              var69++;
                              if (var107) {
                                 Constants.LOG
                                    .info(
                                       "[SR-LAKEREJ] plate {} at {},{}: barred biome and no stream runs in, cells={}",
                                       new Object[]{var0.key(), var8.worldX(var8.gi(var116)), var8.worldZ(var8.gj(var116)), var82.size()}
                                    );
                              }
                              continue;
                           }
                        }

                        var96++;
                        double var360 = var117 * var3.gridSpacing() * var3.gridSpacing();
                        double var372 = 3.0 + 4.0 * Math.sqrt(var360 / 2500.0);
                        boolean var133 = var372 >= var309 - 1.0;
                        int var134;
                        if (var3.growFromWater()) {
                           var134 = Math.min((int)Math.floor(var111) - (var84 ? 2 : 1), (int)Math.floor(var114 + var372));
                           if (var339) {
                              var134 = (int)Math.floor(var111) - (var84 ? 2 : 1);
                           }

                           var133 = false;
                        } else {
                           var134 = (int)Math.floor(var114 + Math.min(var309 - 1.0, var372));
                        }

                        double var135 = a(var82, var8, var1);
                        if (var3.growFromWater()) {
                           if (!Double.isNaN(var135)) {
                              continue;
                           }
                        } else if (!Double.isNaN(var135)) {
                           int var137 = (int)Math.floor(var135) - 1;
                           if (var137 < var134) {
                              var134 = var137;
                              var133 = false;
                           }
                        }

                        int var456 = var8.worldX(var8.gi(var113));
                        int var138 = var8.worldZ(var8.gj(var113));
                        double var139 = var2.wetness(var456, var134, var138);
                        double var141 = Math.max(
                           var3.wetnessDryFloor(),
                           Math.min(1.0, (var139 - var3.wetnessDryCutoff()) / Math.max(1.0E-6, var3.wetnessWetReference() - var3.wetnessDryCutoff()))
                        );
                        double var143 = 1.0 - var141;
                        int var145 = 0;
                        if (var143 > 0.0 && !var339) {
                           double var146 = Hashing.unit(var4, var456, var138, 107749L);
                           var145 = (int)Math.round(8.0 * var143 * var146);
                           var134 -= var145;
                        }

                        if (var134 > (int)Math.floor(var114) || var84 && var3.growFromWater()) {
                           if (!var84 || !var3.growFromWater()) {
                              var134 = Math.max((int)Math.floor(var114) + 1, var134 - 2);
                           }

                           if ((!var84 || !var3.growFromWater()) && !var339) {
                              int var523 = 0;

                              for (int var148 : var82) {
                                 if (var8.height(var148) < var134) {
                                    var523 += 1;
                                 }
                              }

                              if (var523 < var78 && !a(var82, var251, var201, var8)) {
                                 var99++;
                                 if (var107) {
                                    Constants.LOG
                                       .info(
                                          "[SR-LAKEREJ] plate {} at {},{}: water under minimum wetCells={} minCells={} level={} basinCells={}",
                                          new Object[]{
                                             var0.key(),
                                             var8.worldX(var8.gi(var113)),
                                             var8.worldZ(var8.gj(var113)),
                                             Integer.valueOf(var523),
                                             var78,
                                             var134,
                                             var82.size()
                                          }
                                       );
                                 }
                                 continue;
                              }
                           }

                           if (!var319 && !var339) {
                              int var524 = Math.floorDiv(var134, aR);
                              int var532 = var83.getOrDefault(var524, 0);
                              double var544 = Math.min(1.0, (var309 - var325) / 5.0);
                              double var150 = aS / (aS + var532);
                              double var152 = var544 + (1.0 - var544) * var150;
                              double var154 = Hashing.unit(var4, var8.worldX(var8.gi(var113)), var8.worldZ(var8.gj(var113)), var524);
                              if (var154 >= var152) {
                                 var98++;
                                 continue;
                              }

                              var83.put(var524, var532 + 1);
                           }

                           int var525 = var272.size();
                           if (var3.growFromWater()) {
                              List<Integer> var533 = new ArrayList<>(var82);
                              boolean[] var545 = new boolean[var11];

                              for (int var561 : var82) {
                                 var545[var561] = true;
                              }

                              ArrayDeque<Integer> var554 = new ArrayDeque<>(var82);

                              while (!var554.isEmpty()) {
                                 int var562 = (Integer)var554.poll();
                                 int var151 = var8.gi(var562);
                                 int var577 = var8.gj(var562);

                                 for (int var153 = 0; var153 < 8; var153 += 1) {
                                    int var595 = var151 + b[var153];
                                    int var155 = var577 + c[var153];
                                    if (var8.valid(var595, var155)) {
                                       int var156 = var8.idx(var595, var155);
                                       if (var8.inDomain(var156) && var201[var156] - var8.height(var156) > 0.001 && !var545[var156]) {
                                          var545[var156] = true;
                                          var533.add(var156);
                                          var554.add(var156);
                                       }
                                    }
                                 }
                              }

                              long var563 = System.nanoTime();

                              try {
                                 if (var84 && !Boolean.getBoolean("streamsreflowing.nolakeborderpre")) {
                                    double var578 = var533.size() >= 64
                                       ? var3.borderMargin() + 3.0 * var8.spacing
                                       : var3.borderMargin() + (var8.spacing >> 1) + 5.0 * var87;
                                    boolean var596 = false;

                                    for (int var616 : var533) {
                                       if (var0.borderDistance(var8.worldX(var8.gi(var616)), var8.worldZ(var8.gj(var616))) < var578) {
                                          var596 = true;
                                          break;
                                       }
                                    }

                                    if (var596) {
                                       var106++;
                                       if (var107) {
                                          Constants.LOG
                                             .info(
                                                "[SR-LAKEREJ] plate {} at {},{}: border-pre fillCells={} reach={}",
                                                new Object[]{
                                                   var0.key(),
                                                   var8.worldX(var8.gi(var113)),
                                                   var8.worldZ(var8.gj(var113)),
                                                   var533.size(),
                                                   String.format("%.0f", var578)
                                                }
                                             );
                                       }
                                       continue;
                                    }
                                 }

                                 if (var84) {
                                    int var579 = 0;
                                    int var587 = 0;

                                    for (int var607 : var533) {
                                       int var617 = var8.gi(var607);
                                       int var157 = var8.gj(var607);

                                       for (int var158 = 0; var158 < 8; var158 += 1) {
                                          int var159 = var617 + b[var158];
                                          int var160 = var157 + c[var158];
                                          if (var8.valid(var159, var160) && !var545[var8.idx(var159, var160)]) {
                                             var579 += 1;
                                             if (var8.height(var8.idx(var159, var160)) - var134 < 2.5) {
                                                var587 += 1;
                                             }
                                          }
                                       }
                                    }

                                    if (var579 > 0 && var587 * 2 > var579) {
                                       var106++;
                                       if (var107) {
                                          Constants.LOG
                                             .info(
                                                "[SR-LAKEREJ] plate {} at {},{}: stage0 margLow={}/{} level={} fillCells={}",
                                                new Object[]{
                                                   var0.key(),
                                                   var8.worldX(var8.gi(var113)),
                                                   var8.worldZ(var8.gj(var113)),
                                                   Integer.valueOf(var587),
                                                   Integer.valueOf(var579),
                                                   var134,
                                                   var533.size()
                                                }
                                             );
                                       }
                                       continue;
                                    }

                                    int var598 = Math.min(24, Math.max(8, var533.size() / 16));
                                    int var608 = Math.max(3, var598 / 3);
                                    int var618 = 0;
                                    int var624 = 0;
                                    int var633 = Math.max(1, var533.size() / var598);

                                    for (int var641 = 0; var641 < var533.size() && var624 < var598 && var618 < var608; var641 += var633) {
                                       int var649 = (Integer)var533.get(var641);
                                       int var161 = var8.gi(var649);
                                       int var162 = var8.gj(var649);

                                       for (int var163 = 0; var163 < 8; var163 += 1) {
                                          int var164 = var161 + b[var163];
                                          int var165 = var162 + c[var163];
                                          if (var8.valid(var164, var165) && !var545[var8.idx(var164, var165)]) {
                                             var624 += 1;
                                             if (var1.groundHeightExact(var8.worldX(var164), var8.worldZ(var165)) < var134) {
                                                var618 += 1;
                                             }
                                             break;
                                          }
                                       }
                                    }

                                    if (var618 >= var608) {
                                       var106++;
                                       if (var107) {
                                          Constants.LOG
                                             .info(
                                                "[SR-LAKEREJ] plate {} at {},{}: stage1 wetRim={}/{} probed={} level={} fillCells={}",
                                                new Object[]{
                                                   var0.key(),
                                                   var8.worldX(var8.gi(var113)),
                                                   var8.worldZ(var8.gj(var113)),
                                                   Integer.valueOf(var618),
                                                   var608,
                                                   Integer.valueOf(var624),
                                                   var134,
                                                   var533.size()
                                                }
                                             );
                                       }
                                       continue;
                                    }
                                 }
                              } finally {
                                 var108[2] += System.nanoTime() - var563;
                              }

                              Lake var580 = null;
                              BuildGate.yieldPoint();
                              int var588 = HeightCensus.enter(6);
                              double[] var599 = new double[2];
                              if (var84) {
                                 long var610 = System.nanoTime();
                                 NetworkTracer.LocalBasin var625 = a(var8, var1, var533, var116);
                                 var108[3] += System.nanoTime() - var610;
                                 if (var625 == null) {
                                    var106++;
                                    if (var107) {
                                       Constants.LOG
                                          .info(
                                             "[SR-LAKEREJ] plate {} at {},{}: real-redetect-null fillCells={}",
                                             new Object[]{var0.key(), var8.worldX(var8.gi(var113)), var8.worldZ(var8.gj(var113)), var533.size()}
                                          );
                                    }
                                    continue;
                                 }

                                 int var634 = (int)Math.floor(var625.floor());
                                 int var642 = (int)Math.floor(var625.spill()) - 2 - var145;
                                 var102++;
                                 BasinMemoSampler var650 = new BasinMemoSampler(var1);
                                 var103++;

                                 label4391:
                                 for (int var655 = var642; var655 > var634 && var655 > var642 - 4; var655 -= 1) {
                                    long var659 = System.nanoTime();
                                    List<Integer> var665 = var625.poolAt(var655, var8);
                                    var108[4] += System.nanoTime() - var659;
                                    if (var665 != null) {
                                       if (!var339 && var665.size() < var78 && !a(var82, var251, var201, var8)) {
                                          var99++;
                                          if (var107) {
                                             Constants.LOG
                                                .info(
                                                   "[SR-LAKEREJ] plate {} at {},{}: water under minimum poolCells={} minCells={} level={}",
                                                   new Object[]{
                                                      var0.key(), var8.worldX(var8.gi(var113)), var8.worldZ(var8.gj(var113)), var665.size(), var78, var655
                                                   }
                                                );
                                          }
                                          break;
                                       }

                                       var599[0] = 0.0;
                                       var599[1] = 0.0;
                                       long var667 = System.nanoTime();
                                       Lake var167 = a(var665, var655, var87, var650, var8, var0, var3.borderMargin(), var599);
                                       var108[5] += System.nanoTime() - var667;
                                       if (var167 != null) {
                                          int var168 = Math.max(var634 + 1, var655 - 2);

                                          for (int var169 = var168; var169 <= var655; var169 += 1) {
                                             long var170 = System.nanoTime();
                                             List<Integer> var172 = var169 == var655 ? var665 : var625.poolAt(var169, var8);
                                             var108[4] += System.nanoTime() - var170;
                                             if (var172 != null) {
                                                if (var169 != var655) {
                                                   var599[0] = 0.0;
                                                   var599[1] = 0.0;
                                                }

                                                long var173 = System.nanoTime();
                                                Lake var175 = var169 == var655
                                                   ? var167
                                                   : a(var172, var169, var87, var650, var8, var0, var3.borderMargin(), var599);
                                                var108[5] += System.nanoTime() - var173;
                                                if (var175 != null) {
                                                   var580 = var175;
                                                   var134 = var169;
                                                   var533 = var172;
                                                   var104 += var172.size();
                                                   break label4391;
                                                }
                                             }
                                          }
                                          break;
                                       }
                                    }
                                 }
                              } else {
                                 var102++;
                                 var104 += var533.size();
                                 long var609 = System.nanoTime();
                                 var580 = a(var533, var134, var87, var1, var8, var0, var3.borderMargin(), var599);
                                 var108[5] += System.nanoTime() - var609;
                              }

                              HeightCensus.restore(var588);
                              if (var580 == null) {
                                 var106++;
                                 if (var107) {
                                    Constants.LOG
                                       .info(
                                          "[SR-LAKEREJ] plate {} at {},{}: flood-null level={} fillCells={}",
                                          new Object[]{var0.key(), var8.worldX(var8.gi(var113)), var8.worldZ(var8.gj(var113)), var134, var533.size()}
                                       );
                                 }
                                 continue;
                              }

                              double var611 = var599[0];
                              int var626 = (int)var599[1];
                              if (var107 && var626 > 0 && var611 / var626 < 1.5) {
                                 Constants.LOG
                                    .info(
                                       "[SR-LAKEREJ] plate {} at {},{}: film level={} meanDepth={} over {} cells (gate OFF: building anyway)",
                                       new Object[]{
                                          var0.key(),
                                          var8.worldX(var8.gi(var113)),
                                          var8.worldZ(var8.gj(var113)),
                                          var134,
                                          String.format(Locale.ROOT, "%.2f", var611 / var626),
                                          var626
                                       }
                                    );
                              }

                              if (!a(var580, 0.9)) {
                                 var106++;
                                 if (var107 || Boolean.getBoolean("streamsreflowing.caldiag")) {
                                    Constants.LOG
                                       .info(
                                          "[SR-LAKEREJ] plate {} at {},{}: fragmented mask level={} (largest component < 90%)",
                                          new Object[]{var0.key(), var8.worldX(var8.gi(var113)), var8.worldZ(var8.gj(var113)), var134}
                                       );
                                 }
                                 continue;
                              }

                              var272.add(var580);

                              for (int var643 : var533) {
                                 if (var8.height(var643) < var134 && var580.wetAt(var8.worldX(var8.gi(var643)), var8.worldZ(var8.gj(var643)))) {
                                    var269[var643] = var525;
                                 }
                              }
                           } else {
                              var272.add(a(var82, var134, 0, 1, var8));

                              for (int var546 : var82) {
                                 var269[var546] = var525;
                              }
                           }

                           int var535 = var233[var113];
                           if (var133 && var535 >= 0 && var269[var535] < 0 && a(var535, var233, var201, var12, var111, 4.0, 48)) {
                              var275.add(new NetworkTracer.PendingOutflow(var535, var134, var117));
                           }
                        } else if (var107) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEREJ] plate {} at {},{}: no-room spill={} floor={} level={} cells={}",
                                 new Object[]{
                                    var0.key(),
                                    var8.worldX(var8.gi(var113)),
                                    var8.worldZ(var8.gj(var113)),
                                    String.format("%.1f", var111),
                                    String.format("%.1f", var114),
                                    var134,
                                    var82.size()
                                 }
                              );
                        }
                     }
                  }
               } else if (var107 && var82.size() >= Math.max(4, var78 / 2)) {
                  Constants.LOG
                     .info(
                        "[SR-LAKEREJ] plate {} at {},{}: qualify spillDepth={} minDepth={} cells={} minCells={} acc={} thr={}",
                        new Object[]{
                           var0.key(),
                           var8.worldX(var8.gi(var113)),
                           var8.worldZ(var8.gj(var113)),
                           String.format("%.1f", var309),
                           String.format("%.1f", var325),
                           var82.size(),
                           var126,
                           String.format("%.0f", var117),
                           String.format("%.0f", var51)
                        }
                     );
               }
            }
         }
      }

      int var276 = 0;

      for (int var280 : var191) {
         if (var251[var280] && var269[var280] < 0 && var201[var280] - var8.height(var280) > 6.0) {
            a("PHANTOM strip (channel on an unfilled basin)", var8, var280, var201[var280]);
            var251[var280] = false;
            var276++;
         }
      }

      if (var276 > 0 && var107) {
         Constants.LOG
            .info("[SR-LAKEREJ] plate {}: dropped {} channel cells standing >{} above ground on unfilled basins", new Object[]{var0.key(), var276, 6.0});
      }

      if (aj) {
         int var278 = 0;
         int var281 = 0;
         double var112 = 0.0;
         int var284 = 0;
         int var115 = 0;
         int var290 = 0;

         for (int var118 : var191) {
            if (var251[var118]) {
               var278++;
               if (var269[var118] >= 0) {
                  var281++;
               } else {
                  double var302 = var201[var118] - var8.height(var118);
                  if (var302 > 3.0) {
                     var290++;
                  }

                  if (var302 > var112) {
                     var112 = var302;
                     var284 = var8.worldX(var8.gi(var118));
                     var115 = var8.worldZ(var8.gj(var118));
                  }
               }
            }
         }

         Constants.LOG
            .info(
               "[SR-FILLSTATS] plate {}: {} river cells ({} on lakes); non-lake fill >3 blocks: {}; deepest {} at ({},{})",
               new Object[]{var0.key(), var278, var281, var290, String.format(Locale.ROOT, "%.1f", var112), var284, var115}
            );
      }

      long var279 = System.nanoTime();
      HeightCensus.restore(var14);
      var14 = HeightCensus.enter(11);
      if (var107 && var103 > 0) {
         Constants.LOG
            .info(
               "[SR-LAKEMEMO] plate {}: {} candidate basins, {} columns learned, {} re-reads saved by the candidate memo (each one a real getBaseHeight on an all-real world)",
               new Object[]{var0.key(), var103, BasinMemoSampler.LEARNED.sum(), BasinMemoSampler.HITS.sum()}
            );
      }

      if (var107) {
         long var282 = var108[2] + var108[3] + var108[4] + var108[5];
         Constants.LOG
            .info(
               "[SR-LAKESTEP] plate {}: probes {} ms, redetect {} ms, poolAt {} ms, flood {} ms (warm {} ms of it), other {} ms of {} ms total",
               new Object[]{
                  var0.key(),
                  var108[2] / 1000000L,
                  var108[3] / 1000000L,
                  var108[4] / 1000000L,
                  var108[5] / 1000000L,
                  var108[7] / 1000000L,
                  (var279 - var266 - var282) / 1000000L,
                  (var279 - var266) / 1000000L
               }
            );
         AtomicLongArray var285 = bp.get();
         long var287 = var285.get(0);
         long var294 = var285.get(1);
         long var303 = var285.get(2) / 1000000L;
         long var314 = var285.get(3) / 1000000L;
         String var326;
         if (var303 <= 0L) {
            var326 = "no time recorded";
         } else if (var314 * 100L / Math.max(1L, var303) < 80L) {
            var326 = "BARRIER-BOUND (most of the wall is not reading)";
         } else if (var314 < var303 * 2L) {
            var326 = "SERIAL (no parallelism achieved -- pool starved or inline)";
         } else {
            var326 = "READ-BOUND across " + var314 / Math.max(1L, var303) + " effective threads";
         }

         Constants.LOG
            .info(
               "[SR-LAKEWARM] plate {}: {} columns in {} requests ({} inline), wall {} ms, read-thread {} ms -> {}; {} us per column of read time",
               new Object[]{var0.key(), var287, var294, var285.get(4), var303, var314, var326, var287 == 0L ? 0L : var285.get(3) / 1000L / var287}
            );
         Constants.LOG
            .info(
               "[SR-LAKEWARM] plate {}: by site -- interior {}, shoreline ring {}, flood frontier {}; the flood visited {} cells",
               new Object[]{var0.key(), var285.get(5), var285.get(6), var285.get(7), var285.get(8)}
            );
      }

      double var283 = var51 * 16.0;
      boolean[] var286 = new boolean[var11];

      for (int var291 : var191) {
         if (var77) {
            break;
         }

         if (var251[var291] && var269[var291] < 0 && !(var240[var291] < var283)) {
            int var295 = var233[var291];
            double var297 = 0.0;
            if (var295 >= 0) {
               double var310 = a(var8, var291, var295);
               var297 = var310 > 1.0E-9 ? Math.abs(var201[var291] - var201[var295]) / var310 : 0.0;
            }

            if (var297 <= 0.03) {
               var286[var291] = true;
            }
         }
      }

      boolean[] var289 = new boolean[var11];
      ArrayDeque<Integer> var292 = new ArrayDeque<>();
      ArrayList<Integer> var296 = new ArrayList<>();

      for (int var304 : var191) {
         if (var286[var304] && !var289[var304]) {
            var296.clear();
            var292.clear();
            var289[var304] = true;
            var292.add(var304);
            int var311 = Integer.MAX_VALUE;
            int var315 = Integer.MAX_VALUE;
            int var320 = Integer.MIN_VALUE;
            int var327 = Integer.MIN_VALUE;
            double var333 = Double.MAX_VALUE;
            double var346 = 0.0;

            while (!var292.isEmpty()) {
               int var128 = (Integer)var292.poll();
               var296.add(var128);
               int var361 = var8.gi(var128);
               int var365 = var8.gj(var128);
               var311 = Math.min(var311, var361);
               var320 = Math.max(var320, var361);
               var315 = Math.min(var315, var365);
               var327 = Math.max(var327, var365);
               var333 = Math.min(var333, var201[var128]);
               var346 = Math.max(var346, var240[var128]);

               for (int var373 = 0; var373 < 8; var373 += 1) {
                  int var132 = var361 + b[var373];
                  int var404 = var365 + c[var373];
                  if (var8.valid(var132, var404)) {
                     int var420 = var8.idx(var132, var404);
                     if (var286[var420] && !var289[var420]) {
                        var289[var420] = true;
                        var292.add(var420);
                     }
                  }
               }
            }

            int var356 = var320 - var311 + 1;
            int var362 = var327 - var315 + 1;
            if (var296.size() >= var78 && Math.min(var356, var362) >= 3 && !(var296.size() < 0.3 * var356 * var362) && !var3.growFromWater()) {
               int var366 = (int)Math.floor(var333) - 1;
               if (var366 > var12) {
                  double var374 = var346 * var3.gridSpacing() * var3.gridSpacing();
                  int var405 = Math.max(1, Math.min(6, (int)Math.round(3.0 + 4.0 * Math.sqrt(var374 / 2500.0))));
                  int var421 = var272.size();
                  var272.add(a(var296, var366, var405, 0, var8));

                  for (int var136 : var296) {
                     var269[var136] = var421;
                  }
               }
            }
         }
      }

      for (int var305 : var191) {
         if (var269[var305] >= 0) {
            var251[var305] = false;
         }
      }

      List<Lake> var300 = a(var272, var0.key());
      if (var300 != var272) {
         for (int var306 = 0; var306 < var272.size(); var306++) {
            var272.set(var306, (Lake)var300.get(var306));
         }
      }

      if (var3.growFromWater()) {
         int[] var307 = new int[var11];
         Arrays.fill(var307, -1);

         for (int var312 = var191.size() - 1; var312 >= 0; var312--) {
            int var316 = (Integer)var191.get(var312);
            if (var251[var316]) {
               int var321 = var233[var316];
               if (var321 >= 0 && var269[var321] >= 0) {
                  var307[var316] = var269[var321];
               } else if (var321 >= 0 && var251[var321] && var201[var321] > var12) {
                  var307[var316] = var307[var321];
               }
            }
         }

         int var313 = 0;

         for (int var322 : var191) {
            double var328 = var8.height(var322);
            if (var251[var322] && var307[var322] >= 0 && var269[var322] < 0 && var328 > var12 && var328 < ((Lake)var272.get(var307[var322])).level() - 6.0) {
               a("4b4.5 LAKE-DIP CUT (route below its lake)", var8, var322, var201[var322]);
               var251[var322] = false;
               var313++;
            }
         }

         if (var313 > 0) {
            Constants.LOG.debug("[SR-MERGE] lake-dip cut: {} cells on sub-lake routes removed (subtrees pruned below)", var313);
         }
      }

      long var308 = System.nanoTime();
      HeightCensus.restore(var14);
      var14 = HeightCensus.enter(12);
      boolean[] var318 = new boolean[var11];
      if (var3.growFromWater()) {
         boolean[] var323 = new boolean[var11];

         for (int var329 = var191.size() - 1; var329 >= 0; var329--) {
            int var334 = (Integer)var191.get(var329);
            boolean var340 = var269[var334] >= 0 || var201[var334] <= var12 || var8.riverOutlet(var334);
            var323[var334] = var340 || var233[var334] >= 0 && var323[var233[var334]];
         }

         if (var1.hasEstimate() && !Boolean.getBoolean("streamsreflowing.norescue")) {
            HashSet<Integer> var330 = new HashSet<>();
            HashSet<Integer> var335 = new HashSet<>();

            for (int var347 : var191) {
               if (var251[var347] && !var323[var347]) {
                  int var353 = var347;
                  int var357 = var11;

                  while (var233[var353] >= 0) {
                     int var10000 = var357;
                     var357 -= 1;
                     if (var10000 <= 0) {
                        break;
                     }

                     var353 = var233[var353];
                  }

                  if (var330.add(var353) && !(var201[var353] > var12 + 3.0)) {
                     int var363 = var8.gi(var353);
                     int var367 = var8.gj(var353);
                     boolean var375 = false;

                     label4183:
                     for (int var389 = -2; var389 <= 2; var389 += 1) {
                        for (int var406 = -2; var406 <= 2; var406 += 1) {
                           int var422 = var363 + var406;
                           int var435 = var367 + var389;
                           if (var8.valid(var422, var435) && var8.height(var8.idx(var422, var435)) <= var12 - 2) {
                              var375 = true;
                              break label4183;
                           }
                        }
                     }

                     if (var375) {
                        var335.add(var353);
                     }
                  }
               }
            }

            if (!var335.isEmpty()) {
               for (int var342 = var191.size() - 1; var342 >= 0; var342--) {
                  int var348 = (Integer)var191.get(var342);
                  var323[var348] = var323[var348] || var335.contains(var348) || var233[var348] >= 0 && var323[var233[var348]];
               }
            }
         }

         if (RiverEngine.deadEndResolver() != null) {
            HashSet<Integer> var331 = new HashSet<>();
            HashSet<Integer> var336 = new HashSet<>();

            for (int var349 : var191) {
               if (var251[var349] && !var323[var349]) {
                  int var354 = var349;
                  int var358 = var11;

                  while (var233[var354] >= 0) {
                     int var673 = var358;
                     var358 -= 1;
                     if (var673 <= 0) {
                        break;
                     }

                     var354 = var233[var354];
                  }

                  if (var331.add(var354)) {
                     ArrayList<Integer> var364 = new ArrayList<>();
                     int var368 = var354;

                     while (var364.size() < 4096) {
                        var364.add(var368);
                        int var376 = var8.gi(var368);
                        int var390 = var8.gj(var368);
                        int var407 = -1;
                        double var423 = -1.0;

                        for (int var446 = -1; var446 <= 1; var446 += 1) {
                           for (int var457 = -1; var457 <= 1; var457 += 1) {
                              if ((var457 | var446) != 0 && var8.valid(var376 + var457, var390 + var446)) {
                                 int var468 = var8.idx(var376 + var457, var390 + var446);
                                 if (var251[var468] && var233[var468] == var368 && var240[var468] > var423) {
                                    var407 = var468;
                                    var423 = var240[var468];
                                 }
                              }
                           }
                        }

                        if (var407 < 0 || var240[var407] < var240[var354] * 0.25) {
                           break;
                        }

                        var368 = var407;
                     }

                     if (var364.size() >= 4) {
                        long var377 = var4 ^ var8.worldX(var8.gi(var354)) * -7046029254386353131L ^ Long.rotateLeft(var8.worldZ(var8.gj(var354)), 23);
                        var377 ^= var377 >>> 29;
                        var377 *= -4658895280553007687L;
                        var377 ^= var377 >>> 32;
                        double var408 = 0.15 + 0.45 * (var377 >>> 11 & 65535L) / 65536.0;
                        int var436 = (Integer)var364.get(Math.max(1, Math.min(var364.size() - 2, (int)(var364.size() * var408))));
                        if (!(var201[var436] < var12 + 3)) {
                           var336.add(var436);
                        }
                     }
                  }
               }
            }

            if (!var336.isEmpty()) {
               bo.addAndGet(2, var336.size());

               for (int var350 : var336) {
                  var233[var350] = -1;
                  var318[var350] = true;
                  a("4b5 CUT (border drainage ends here for the resolver)", var8, var350, var201[var350]);
               }

               for (int var345 = var191.size() - 1; var345 >= 0; var345--) {
                  int var351 = (Integer)var191.get(var345);
                  boolean var355 = var269[var351] >= 0 || var201[var351] <= var12 || var8.riverOutlet(var351) || var233[var351] < 0 && var336.contains(var351);
                  var323[var351] = var355 || var233[var351] >= 0 && var323[var233[var351]];
               }
            }
         }

         for (int var337 : var191) {
            if (var251[var337] && !var323[var337]) {
               a("4b5 CULL (drainage reaches no water)", var8, var337, var201[var337]);
               var251[var337] = false;
            }
         }
      }

      long var324 = System.nanoTime();
      HeightCensus.restore(var14);
      var14 = HeightCensus.enter(13);
      int[] var369 = new int[var11];
      Arrays.fill(var369, -1);

      for (int var381 = var191.size() - 1; var381 >= 0; var381 -= 1) {
         int var391 = (Integer)var191.get(var381);
         if (var251[var391]) {
            var369[var391] = var233[var391] >= 0 && var251[var233[var391]] ? var369[var233[var391]] : var391;
         }
      }

      Map<Integer, Integer> var382 = new HashMap<>();

      for (int var409 : var191) {
         if (var251[var409]) {
            var382.merge(var369[var409], 1, Integer::sum);
         }
      }

      boolean[] var393 = new boolean[var11];

      for (int var424 : var191) {
         if (var251[var424]) {
            int var437 = var233[var424];
            if (var437 >= 0 && var269[var437] < 0 && !(var201[var437] <= var12)) {
               int var448 = var369[var424];
               int var459 = (Integer)var382.get(var448);
               int var470 = var8.gi(var424);
               int var477 = var8.gj(var424);

               for (int var140 = 0; var140 < 8; var140 += 1) {
                  int var489 = var470 + b[var140];
                  int var142 = var477 + c[var140];
                  if (var8.valid(var489, var142)) {
                     int var504 = var8.idx(var489, var142);
                     if (var8.inDomain(var504) && var251[var504]) {
                        int var144 = var369[var504];
                        if (var144 != var448) {
                           int var517 = var233[var504];
                           if (var517 >= 0 && var269[var517] < 0 && !(var201[var517] <= var12)) {
                              int var526 = (Integer)var382.get(var144);
                              if (af != null && b(var8.worldX(var470), var8.worldZ(var477))) {
                                 Constants.LOG
                                    .info(
                                       "[SR-SEGDUMP] 4b6 contact: cell ({},{}) sys {} size {} vs cell ({},{}) sys {} size {} -> {}",
                                       new Object[]{
                                          var8.worldX(var470),
                                          var8.worldZ(var477),
                                          var448,
                                          var459,
                                          var8.worldX(var8.gi(var504)),
                                          var8.worldZ(var8.gj(var504)),
                                          var144,
                                          var526,
                                          var459 >= var526 && (var459 != var526 || var448 <= var144) ? "keep" : "DROP this cell"
                                       }
                                    );
                              }

                              if (var318[var448] || var318[var144]) {
                                 if (var318[var448] && !var318[var144] && var201[var504] < var201[var424]) {
                                    var233[var424] = var504;
                                 }
                                 break;
                              }

                              if (var459 < var526 || var459 == var526 && var448 > var144) {
                                 var393[var424] = true;
                                 a("4b6 TRIM (smaller system yields the contact)", var8, var424, var201[var424]);
                                 break;
                              }
                           } else if (af != null && b(var8.worldX(var470), var8.worldZ(var477))) {
                              Constants.LOG
                                 .info(
                                    "[SR-SEGDUMP] 4b6 contact EXEMPT (neighbour is mouth): cell ({},{}) sys {} touches cell ({},{}) sys {}",
                                    new Object[]{
                                       var8.worldX(var470), var8.worldZ(var477), var448, var8.worldX(var8.gi(var504)), var8.worldZ(var8.gj(var504)), var144
                                    }
                                 );
                           }
                        }
                     }
                  }
               }
            } else if (af != null && b(var8.worldX(var8.gi(var424)), var8.worldZ(var8.gj(var424)))) {
               for (int var447 = 0; var447 < 8; var447 += 1) {
                  int var458 = var8.gi(var424) + b[var447];
                  int var469 = var8.gj(var424) + c[var447];
                  if (var8.valid(var458, var469) && var251[var8.idx(var458, var469)] && var369[var8.idx(var458, var469)] != var369[var424]) {
                     Constants.LOG
                        .info(
                           "[SR-SEGDUMP] 4b6 contact EXEMPT (own cell is mouth): cell ({},{}) sys {} touches cell ({},{}) sys {}",
                           new Object[]{
                              var8.worldX(var8.gi(var424)),
                              var8.worldZ(var8.gj(var424)),
                              var369[var424],
                              var8.worldX(var458),
                              var8.worldZ(var469),
                              var369[var8.idx(var458, var469)]
                           }
                        );
                  }
               }
            }
         }
      }

      int var411 = 0;

      for (int var438 : var191) {
         if (var393[var438]) {
            var251[var438] = false;
            var411 += 1;
         }
      }

      long var338 = System.nanoTime();
      if (var411 > 0) {
         boolean[] var426 = new boolean[var11];

         for (int var439 = var191.size() - 1; var439 >= 0; var439 -= 1) {
            int var449 = (Integer)var191.get(var439);
            if (var251[var449]) {
               int var460 = var233[var449];
               boolean var471 = var460 >= 0 && (var269[var460] >= 0 || var201[var460] <= var12);
               var426[var449] = var471 || var318[var449] || var460 >= 0 && var251[var460] && var426[var460];
            }
         }

         int var440 = 0;

         for (int var461 : var191) {
            if (var251[var461] && !var426[var461]) {
               a("4b7 RE-PRUNE (orphaned by the separation trim)", var8, var461, var201[var461]);
               var251[var461] = false;
               var440 += 1;
            }
         }

         Constants.LOG.debug("[SR-MERGE] local separation: trimmed {} contact cells, pruned {} orphaned", Integer.valueOf(var411), Integer.valueOf(var440));
      }

      long var352 = System.nanoTime();
      var369 = new int[var11];
      Arrays.fill(var369, -1);

      for (int var383 = var191.size() - 1; var383 >= 0; var383 -= 1) {
         int var394 = (Integer)var191.get(var383);
         if (var251[var394]) {
            var369[var394] = var233[var394] >= 0 && var251[var233[var394]] ? var369[var233[var394]] : var394;
         }
      }

      int[] var384 = new int[var11];

      for (int var412 : var191) {
         if (var251[var412] && var233[var412] >= 0 && var251[var233[var412]]) {
            var384[var233[var412]]++;
         }
      }

      int var396 = 0;
      var411 = 0;

      for (int var427 = 0; var427 < var191.size(); var427 += 1) {
         int var441 = (Integer)var191.get(var427);
         if (var251[var441]) {
            int var451 = var233[var441];
            if (var451 >= 0 && var269[var451] < 0 && !(var201[var451] <= var12)) {
               int var462 = var8.gi(var451) - var8.gi(var441);
               int var472 = var8.gj(var451) - var8.gj(var441);
               int var478 = var8.gi(var441);
               int var483 = var8.gj(var441);
               int var490 = -1;
               double var497 = var201[var441];

               for (int var511 = 0; var511 < 8; var511 += 1) {
                  int var518 = var478 + b[var511];
                  int var527 = var483 + c[var511];
                  if (var8.valid(var518, var527)) {
                     int var536 = var8.idx(var518, var527);
                     if (var8.inDomain(var536) && var251[var536] && var369[var536] == var369[var441] && !(var201[var536] >= var201[var441]) && var536 != var451
                        )
                      {
                        int var547 = var233[var536];
                        if (var547 >= 0) {
                           int var555 = var8.gi(var547) - var8.gi(var536);
                           int var564 = var8.gj(var547) - var8.gj(var536);
                           if (var462 * var555 + var472 * var564 > 0 && b[var511] * var462 + c[var511] * var472 >= 0) {
                              boolean var571 = false;
                              int var581 = var451;

                              for (int var589 = 0; var589 < 24 && var581 >= 0; var589 += 1) {
                                 if (var581 == var536) {
                                    var571 = true;
                                    break;
                                 }

                                 var581 = var233[var581];
                              }

                              if (!var571 && var201[var536] < var497) {
                                 var497 = var201[var536];
                                 var490 = var536;
                              }
                           }
                        }
                     }
                  }
               }

               if (var490 >= 0) {
                  int var512 = var233[var441];
                  var384[var512]--;
                  var233[var441] = var490;
                  var384[var490]++;
                  var396 += 1;

                  for (int var519 = var512; var519 >= 0 && var251[var519] && var384[var519] == 0; var411 += 1) {
                     var251[var519] = false;
                     int var528 = var233[var519];
                     if (var528 >= 0 && var251[var528]) {
                        var384[var528]--;
                     }

                     var519 = var528;
                  }
               }
            }
         }
      }

      if (var396 > 0) {
         Constants.LOG
            .debug("[SR-MERGE] branch fuse: {} parallel branches fused, {} redundant cells trimmed", Integer.valueOf(var396), Integer.valueOf(var411));
      }

      long var359 = System.nanoTime();
      var369 = new int[var11];
      int[] var369Ref = var369;

      for (int var397 : var191) {
         if (var251[var397] && var233[var397] >= 0) {
            var369[var233[var397]]++;
         }
      }

      if (var3.minSeaChainNodes() > 0) {
         Map<Integer, List<Integer>> var382b = new HashMap<>();

         for (int var414 : var191) {
            if (var251[var414] && var233[var414] >= 0) {
               var382b.computeIfAbsent(var233[var414], var0x -> new ArrayList<>()).add(var414);
            }
         }

         int var399 = var3.minLengthScope();
         boolean[] var415 = new boolean[var11];
         int var428 = 0;

         for (int var452 : var191) {
            if (var251[var452] && var369[var452] >= 2 && !var415[var452]) {
               List<Integer> var463 = var382b.get(var452);
               if (var463 != null && var463.size() >= 2) {
                  int var473 = 0;
                  boolean var479 = false;
                  boolean var484 = false;
                  boolean var491 = false;
                  int var498 = var452;

                  while (true) {
                     int var505 = var233[var498];
                     if (var505 < 0) {
                        var473 = -1;
                        break;
                     }

                     if (var269[var505] >= 0) {
                        var491 = true;
                        break;
                     }

                     if (var8.riverOutlet(var505)) {
                        var484 = true;
                        break;
                     }

                     var473 += 1;
                     if (!var251[var505]) {
                        var479 = true;
                        break;
                     }

                     if (var369[var505] != 1) {
                        var473 = -1;
                        break;
                     }

                     var498 = var505;
                  }

                  if (var473 >= 0 && var473 < var3.minSeaChainNodes()) {
                     boolean var506 = var479 || var399 >= 2 && var484 || var399 >= 3 && var491;
                     if (var506) {
                        ArrayList<int[]> var513 = new ArrayList<>();
                        boolean var520 = true;

                        for (int var537 : var463) {
                           int var548 = var537;
                           int var556 = 1;
                           boolean var565 = false;

                           for (int var572 = 0; var572 < 100000; var572 += 1) {
                              if (var369[var548] == 0) {
                                 var565 = true;
                                 break;
                              }

                              if (var369[var548] >= 2) {
                                 break;
                              }

                              List<Integer> var582 = var382b.get(var548);
                              if (var582 == null || var582.size() != 1) {
                                 break;
                              }

                              var548 = (Integer)var582.get(0);
                              var556 += 1;
                           }

                           if (!var565 || var556 >= var3.minSeaChainNodes()) {
                              var520 = false;
                              break;
                           }

                           var513.add(new int[]{var548, var556});
                        }

                        if (var520) {
                           boolean var530 = true;

                           for (int[] var549 : var513) {
                              int var557 = var549[0];
                              if (!(var2.wetness(var8.worldX(var8.gi(var557)), (int)var201[var557], var8.worldZ(var8.gj(var557))) > var3.wetnessDryCutoff())) {
                                 var530 = false;
                                 break;
                              }
                           }

                           if (var530) {
                              for (int var550 : var463) {
                                 int var558 = var550;

                                 for (int var566 = 0; var566 < 100000; var566 += 1) {
                                    var415[var558] = true;
                                    if (var369[var558] == 0) {
                                       break;
                                    }

                                    var558 = (Integer)var382b.get(var558).get(0);
                                 }
                              }

                              var498 = var452;

                              while (true) {
                                 var415[var498] = true;
                                 int var540 = var233[var498];
                                 if (var540 < 0 || var269[var540] >= 0 || var8.riverOutlet(var540) || !var251[var540] || var369[var540] != 1) {
                                    var428 += 1;
                                    break;
                                 }

                                 var498 = var540;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var428 > 0) {
            int var443 = 0;

            for (int var464 : var191) {
               if (var415[var464]) {
                  var251[var464] = false;
                  var443 += 1;
               }
            }

            Constants.LOG
               .debug("[SR-MERGE] minStreamLength confluence-cluster cull: {} cluster(s), {} cell(s) dropped", Integer.valueOf(var428), Integer.valueOf(var443));
            Arrays.fill(var369, 0);

            for (int var465 : var191) {
               if (var251[var465] && var233[var465] >= 0) {
                  var369[var233[var465]]++;
               }
            }
         }
      }

      if (!R && !var3.legacyShaping()) {
         double[] var387 = new double[var11];

         for (int var416 : var191) {
            var387[var416] = var242;
         }

         int var401 = 0;

         for (int var429 : var191) {
            if (var253[var429] && !var251[var429] && var269[var429] < 0) {
               var387[var429] = var242;
               var401 += 1;
            }

            if (var233[var429] >= 0) {
               var387[var233[var429]] = var387[var233[var429]] + var387[var429];
            }
         }

         System.arraycopy(var387, 0, var240, 0, var11);

         for (int var418 = 0; var418 < var275.size(); var418 += 1) {
            NetworkTracer.PendingOutflow var430 = (NetworkTracer.PendingOutflow)var275.get(var418);
            double var444 = var240[var430.outlet()];
            if (var444 < var430.acc()) {
               var275.set(var418, new NetworkTracer.PendingOutflow(var430.outlet(), var430.startLevel(), var444));
            }
         }

         if (var401 > 0) {
            Constants.LOG.debug("[SR-MERGE] live drainage: {} removed channel cell(s) no longer pass their catchment on", Integer.valueOf(var401));
         }
      }

      int var388 = 0;

      for (int var419 : var191) {
         if (var251[var419]) {
            var388 += 1;
         }
      }

      long var403 = System.nanoTime();
      HeightCensus.restore(var14);
      if (Boolean.getBoolean("streamsreflowing.caldiag") || PrepDiag.loud()) {
         long var431 = 1000000L;
         Constants.LOG
            .info(
               "[SR-ROUTE] plate {} = {} ms total: pflood {} | d8+thin {} | lakes {} | ponds {} | reach {} | trim {}",
               new Object[]{
                  var0.key(),
                  (var403 - var9) / var431,
                  (var222 - var9) / var431,
                  (var266 - var222) / var431,
                  (var279 - var266) / var431,
                  (var308 - var279) / var431,
                  (var324 - var308) / var431,
                  (var403 - var324) / var431
               }
            );
         Constants.LOG
            .info(
               "[SR-ROUTE]   d8+thin {} ms = d8 {} | accumulation {} | river-cells {} | reconnect {} | dryness-thin {}",
               new Object[]{
                  (var266 - var222) / var431,
                  (var237 - var222) / var431,
                  (var246 - var237) / var431,
                  (var255 - var246) / var431,
                  (var257 - var255) / var431,
                  (var266 - var257) / var431
               }
            );
         Constants.LOG
            .info(
               "[SR-ROUTE]   trim {} ms = separation {} | orphan-reprune {} | branch-fuse {} | confluences {}",
               new Object[]{
                  (var403 - var324) / var431, (var338 - var324) / var431, (var352 - var338) / var431, (var359 - var352) / var431, (var403 - var359) / var431
               }
            );
         Constants.LOG
            .info(
               "[SR-ROUTE]   scale: {} grid cells ({} in domain), {} river cells, {} lakes",
               new Object[]{var8.size(), var191.size(), Integer.valueOf(var388), var272.size()}
            );
      }

      for (int var432 = 0; var432 < ak.length(); var432 += 1) {
         ak.set(var432, 0L);
      }

      at.set(0L);
      ArrayList<RiverSegment> var433 = new ArrayList<>();
      ConcurrentLinkedQueue<Sink> var445 = new ConcurrentLinkedQueue<>();
      ArrayList<Integer> var455 = new ArrayList<>();

      for (int var474 : var191) {
         if (var251[var474] && var369[var474] != 1) {
            var455.add(var474);
         }
      }

      int[] var467 = new int[var11];
      Arrays.fill(var467, -1);

      for (int var475 = var191.size() - 1; var475 >= 0; var475 -= 1) {
         int var480 = (Integer)var191.get(var475);
         if (var251[var480]) {
            var467[var480] = var233[var480] >= 0 && var251[var233[var480]] ? var467[var233[var480]] : var480;
         }
      }

      HashMap<Integer, Lake> var476 = new HashMap<>();

      for (int var485 : var455) {
         int var492 = var467[var485];
         if (var492 >= 0 && !var476.containsKey(var492)) {
            int var500 = var233[var492];
            var476.put(var492, var500 >= 0 && var269[var500] >= 0 ? (Lake)var272.get(var269[var500]) : null);
         }
      }

      ArrayList<Integer> var482 = new ArrayList<>();
      if (var3.growFromWater()) {
         List<RiverSegment>[] var486 = new List[var455.size()];
         LowResGrid.parallelFor(
            var455.size(),
            var19x -> var486[var19x] = a(
               (Integer)var455.get(var19x),
               var233,
               var269,
               var272,
               (Lake)var476.get(var467[var455.get(var19x)]),
               var251,
               var369Ref,
               var201,
               var240,
               var8,
               var1,
               var2,
               var3,
               var4,
               var12,
               var445
            )
         );

         for (int var493 = 0; var493 < var486.length; var493 += 1) {
            int var501 = var467[var455.get(var493)];

            for (int var507 = 0; var507 < var486[var493].size(); var507 += 1) {
               var482.add(var501);
            }

            var433.addAll(var486[var493]);
         }
      } else {
         for (int var494 : var455) {
            List<RiverSegment> var502 = a(
               var494, var233, var269, var272, (Lake)var476.get(var467[var494]), var251, var369, var201, var240, var8, var1, var2, var3, var4, var12, var445
            );
            int var508 = var467[var494];

            for (int var514 = 0; var514 < var502.size(); var514 += 1) {
               var482.add(var508);
            }

            var433.addAll(var502);
         }
      }

      List<RiverSegment>[] var488 = new List[var275.size()];
      LowResGrid.parallelFor(var275.size(), var14x -> {
         ArrayList<RiverSegment> var15x = new ArrayList<>();
         a((NetworkTracer.PendingOutflow)var275.get(var14x), var8, var233, var269, var272, var201, var12, var1, var2, var3, var4, var15x, var445);
         var488[var14x] = var15x;
      });

      for (int var495 = 0; var495 < var488.length; var495 += 1) {
         NetworkTracer.PendingOutflow var503 = (NetworkTracer.PendingOutflow)var275.get(var495);
         int var509 = var503.outlet() >= 0 && var467[var503.outlet()] >= 0 ? var467[var503.outlet()] : -(var503.outlet() + 2);

         for (RiverSegment var521 : var488[var495] == null ? List.<RiverSegment>of() : var488[var495]) {
            var433.add(var521);
            var482.add(var509);
         }
      }

      long var496 = System.nanoTime();
      int var510 = HeightCensus.enter(14);
      boolean[] var516 = new boolean[var272.size()];
      int[] var522 = new int[]{0};
      int[] var531 = new int[]{0};
      int var541 = HeightCensus.enter(15);
      var522[0]++;
      a(var433, var482, var272, var516, var12, var3, var1, var445);
      HeightCensus.restore(var541);
      double[] var542 = new double[var433.size()];
      int[] var551 = new int[var433.size()];

      for (int var559 = 0; var559 <= var272.size() && !var433.isEmpty(); var559 += 1) {
         a(var433, var272, var516, var542, var551);
         int[] var567 = chainIds(var433);
         HashSet<Long> var573 = new HashSet<>();

         for (RiverSegment var590 : var433) {
            var573.add(a(var590.x2(), var590.z2()));
         }

         boolean var584 = false;
         int var591 = Integer.MIN_VALUE;

         for (int var600 = 0; var600 < var433.size(); var600 += 1) {
            if (var567[var600] != var591) {
               var591 = var567[var600];
               if (var551[var600] >= 0 && !var516[var551[var600]]) {
                  RiverSegment var612 = (RiverSegment)var433.get(var600);
                  if (!var573.contains(a(var612.x1(), var612.z1()))) {
                     double var619 = var612.y1() - RiverEngine.insetFor(var612.w1(), var3);
                     if (var619 < var542[var600] - 0.01) {
                        var516[var551[var600]] = true;
                        var584 = true;
                        Lake var636 = (Lake)var272.get(var551[var600]);
                        if (Boolean.getBoolean("streamsreflowing.caldiag") || af != null) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEKILL] plate {}: lake at {},{} level {} killed: a contacting stream's head surface {} is below it (cannot be grade-matched)",
                                 new Object[]{var0.key(), var636.cellMinX(), var636.cellMinZ(), var636.level(), String.format(Locale.ROOT, "%.1f", var619)}
                              );
                        }
                     }
                  }
               }
            }
         }

         if (!var584) {
            break;
         }

         int var601 = HeightCensus.enter(15);
         var522[0]++;
         a(var433, var482, var272, var516, var12, var3, var1, var445);
         HeightCensus.restore(var601);
         var542 = new double[var433.size()];
         var551 = new int[var433.size()];
      }

      int[] var543 = new int[var482.size()];

      for (int var552 = 0; var552 < var543.length; var552 += 1) {
         var543[var552] = (Integer)var482.get(var552);
      }

      HashMap<Integer, Integer> var553 = new HashMap<>();
      int[] var560 = new int[var543.length];

      for (int var568 = 0; var568 < var543.length; var568 += 1) {
         int var574 = var543[var568];
         if (var574 < 0) {
            var560[var568] = Integer.MIN_VALUE;
         } else {
            Integer var585 = (Integer)var553.get(var574);
            if (var585 == null) {
               int var592 = var233[var574];
               var585 = var592 >= 0 && var269[var592] >= 0 && !var516[var269[var592]] ? ((Lake)var272.get(var269[var592])).level() : Integer.MIN_VALUE;
               var553.put(var574, var585);
            }

            var560[var568] = var585 == Integer.MIN_VALUE
               ? Integer.MIN_VALUE
               : var585 + RiverEngine.insetFor((((RiverSegment)var433.get(var568)).w1() + ((RiverSegment)var433.get(var568)).w2()) * 0.5, var3);
         }
      }

      double[] var569 = new double[var433.size()];
      int[] var575 = new int[var433.size()];
      a(var433, var272, var516, var569, var575);

      for (int var586 = 0; var586 < var433.size(); var586 += 1) {
         if (var575[var586] >= 0) {
            RiverSegment var593 = (RiverSegment)var433.get(var586);
            var560[var586] = Math.max(var560[var586], (int)Math.round(var569[var586]) + RiverEngine.insetFor((var593.w1() + var593.w2()) * 0.5, var3));
         }
      }

      int[] var570 = chainIds(var433);
      double var576 = Math.max(8.0, var3.maxWidth() * 2.0);
      HashMap<Long, List<Integer>> var594 = new HashMap<>();

      for (int var602 = 0; var602 < var433.size(); var602 += 1) {
         RiverSegment var613 = (RiverSegment)var433.get(var602);
         int var620 = (int)Math.floor(var613.minX() / var576);
         int var627 = (int)Math.floor(var613.maxX() / var576);
         int var637 = (int)Math.floor(var613.minZ() / var576);
         int var644 = (int)Math.floor(var613.maxZ() / var576);

         for (int var651 = var620; var651 <= var627; var651 += 1) {
            for (int var656 = var637; var656 <= var644; var656 += 1) {
               var594.computeIfAbsent((long)var651 << 32 ^ var656 & 4294967295L, var0x -> new ArrayList<>()).add(Integer.valueOf(var602));
            }
         }
      }

      for (int var603 = 0; var603 < 4; var603 += 1) {
         var531[0]++;
         long var614 = CachingTerrainSampler.HITS_ALL.sum();
         long var628 = CachingTerrainSampler.MISSES_ALL.sum();
         int var645 = HeightCensus.enter(7);
         boolean var652 = a(var433, var543, var560, var3, var1, var570, var594, var576);
         HeightCensus.restore(var645);
         if (Boolean.getBoolean("streamsreflowing.caldiag") || PrepDiag.loud()) {
            Constants.LOG
               .info(
                  "[SR-CAPTURE] plate {} round {}: {} ridge asks, memo {} hits / {} misses, dropped={}",
                  new Object[]{
                     var0.key(), var531[0], a.sum(), CachingTerrainSampler.HITS_ALL.sum() - var614, CachingTerrainSampler.MISSES_ALL.sum() - var628, var652
                  }
               );
         }

         for (int var657 = 0; var657 < var433.size(); var657 += 1) {
            int var660 = var560[var657];
            if (var660 != Integer.MIN_VALUE) {
               RiverSegment var663 = (RiverSegment)var433.get(var657);
               if (var663.y1() < var660 || var663.y2() < var660) {
                  if (af != null && b((var663.x1() + var663.x2()) * 0.5, (var663.z1() + var663.z2()) * 0.5)) {
                     Constants.LOG
                        .info(
                           "[SR-SEGDUMP] modifier lakeFloor RAISE seg {} y {}..{} -> floor {}",
                           new Object[]{
                              Integer.valueOf(var657), String.format(Locale.ROOT, "%.1f", var663.y1()), String.format(Locale.ROOT, "%.1f", var663.y2()), var660
                           }
                        );
                  }

                  var433.set(
                     var657,
                     new RiverSegment(
                        var663.x1(),
                        var663.z1(),
                        Math.max(var663.y1(), var660),
                        var663.w1(),
                        var663.x2(),
                        var663.z2(),
                        Math.max(var663.y2(), var660),
                        var663.w2(),
                        var663.sp1(),
                        var663.sp2()
                     )
                  );
               }
            }
         }

         a(var433, var560);
         if (!var652) {
            break;
         }
      }

      if (af != null) {
         a(var0, var433, var543, var272);
      }

      if (ad != null) {
         a(ad[0], ad[1], var191, var8, var201, var233, var240, var251, var269, var12, var0);
      }

      HeightCensus.restore(var510);
      if (Boolean.getBoolean("streamsreflowing.caldiag") || aj || PrepDiag.loud()) {
         long var604 = System.nanoTime();
         Constants.LOG
            .info(
               "[SR-PHASES] plate {}: fill {} ms, route {} ms, chains+lakes {} ms, post {} ms ({} segs, sampler {}/{})",
               new Object[]{
                  var0.key(),
                  (var9 - var6) / 1000000L,
                  (var403 - var9) / 1000000L,
                  (var496 - var403) / 1000000L,
                  (var604 - var496) / 1000000L,
                  var433.size(),
                  LowResGrid.samplerWidth(),
                  LowResGrid.samplerFullWidth()
               }
            );
         Constants.LOG.info("[SR-POST] plate {}: orphan-prune ran {}x, capture ran {}x", new Object[]{var0.key(), var522[0], var531[0]});
         int[] var621 = F.get();
         if (var621[0] > 0) {
            Constants.LOG
               .info(
                  "[SR-OUTLET] plate {}: {} sea mouths -> {} connected ({} had to walk, {} cells each; {} deepened), {} dropped ({} no real outlet in reach, {} high ground on the way); candidates rejected: {} not an outlet biome, {} not below sea, {} dry",
                  new Object[]{
                     var0.key(),
                     var621[0],
                     var621[1],
                     var621[2],
                     var621[2] == 0 ? 0 : var621[3] / var621[2],
                     var621[4],
                     var621[5] + var621[6],
                     var621[5],
                     var621[6],
                     var621[7],
                     var621[8],
                     var621[9]
                  }
               );
         }

         AtomicIntegerArray var629 = cc.remove(var8);
         if (var629 != null) {
            Constants.LOG
               .info(
                  "[SR-SEAEND] plate {}: sea ends {} at once, {} after 1 step, {} after 2, {} after 3, {} dropped; {} terrain reads under the ocean skip, {} land lookups",
                  new Object[]{var0.key(), var629.get(0), var629.get(1), var629.get(2), var629.get(3), var629.get(4), var629.get(5), var629.get(6)}
               );
         }

         StringBuilder var638 = new StringBuilder();

         for (int var646 = 0; var646 < as.length; var646 += 1) {
            var638.append(' ').append(as[var646]).append(' ').append(ak.get(var646) / 1000000L).append("ms");
         }

         var638.append(" | oxbow cutoffs ").append(at.get());
         Constants.LOG.info("[SR-CHAINSTEP] plate {} (CPU across the parallel chain tasks, read as proportions):{}", var0.key(), var638);
         String var647 = HeightCensus.drainPlateReport();
         if (var647 != null) {
            Constants.LOG.info("[SR-HEIGHTS] plate {}: {}", var0.key(), var647);
         }

         double var653 = 0.0;
         double var661 = 0.0;
         double var666 = 0.0;

         for (RiverSegment var671 : var433) {
            var653 += Math.hypot(var671.x2() - var671.x1(), var671.z2() - var671.z1());
            var661 += (var671.y1() + var671.y2()) * 0.5;
            var666 += (var671.w1() + var671.w2()) * 0.5;
         }

         int var669 = Math.max(1, var433.size());
         Constants.LOG
            .info(
               "[SR-QUALITY] plate {}: segs {}, length {}, meanGrade {}, meanWidth {}, lakes {}",
               new Object[]{
                  var0.key(),
                  var433.size(),
                  String.format(Locale.ROOT, "%.1f", var653),
                  String.format(Locale.ROOT, "%.3f", var661 / var669),
                  String.format(Locale.ROOT, "%.3f", var666 / var669),
                  var272.size()
               }
            );
         Constants.LOG
            .info(
               "[SR-LAKES] plate {}: basins {} oversize {} depthGated {} qualified {} (plunge pools {}) thinned {} fineFloods {} ({} coarse cells, {} rejected) built {} waterUnderMin {}",
               new Object[]{var0.key(), var94, var95, var100, var96, var97, var98, var102, var104, var106, var272.size(), var99}
            );
         if (var67) {
            Constants.LOG
               .info("[SR-BARRED] plate {}: {} headwaters and {} stand-alone lakes not started in barred biomes", new Object[]{var0.key(), var68, var69});
         }
      }

      int var605 = b(var433, var3);
      if (var605 > 0 && Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info("[SR-MERGE] plate {}: absorbed {} thin engulfed crossing segment(s)", var0.key(), var605);
      }

      int var615 = a(var433, var3);
      if (var615 > 0 && Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info("[SR-WIDTH] plate {}: relaxed {} waist segment(s) to half the upstream max", var0.key(), var615);
      }

      for (int var622 = var272.size() - 1; var622 >= 0; var622 -= 1) {
         if (var516[var622]) {
            var272.remove(var622);
         }
      }

      int[] var623 = null;

      for (int var630 = var272.size() - 1; var630 >= 0; var630 -= 1) {
         Lake var639 = (Lake)var272.get(var630);
         if (var639.digDepth() <= 0) {
            boolean var648 = false;
            boolean[] var654 = null;

            for (int var658 = 0; var658 < var433.size(); var658 += 1) {
               RiverSegment var662 = (RiverSegment)var433.get(var658);
               double var664 = (var662.w1() + var662.w2()) * 0.5;
               if (!(var664 <= 3.0) && !(Math.min(var662.y1(), var662.y2()) >= var639.level() - 3)) {
                  int var668 = (int)Math.round((var662.x1() + var662.x2()) * 0.5);
                  int var670 = (int)Math.round((var662.z1() + var662.z2()) * 0.5);
                  int var672 = (int)Math.ceil(var664 / Math.max(1, var639.spacing()));
                  if (var639.wetWithin(var668, var670, var672)) {
                     if (var654 == null) {
                        if (var623 == null) {
                           var623 = chainIds(var433);
                        }

                        var654 = a(var433, var623, var639);
                     }

                     if (!var654[var658]) {
                        if (Boolean.getBoolean("streamsreflowing.caldiag") || af != null) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEREJ] plate {} at {},{}: drained-through level={} by w={} channel y={} at {},{}",
                                 new Object[]{
                                    var0.key(),
                                    var639.cellMinX(),
                                    var639.cellMinZ(),
                                    var639.level(),
                                    String.format(Locale.ROOT, "%.1f", var664),
                                    String.format(Locale.ROOT, "%.1f", Math.min(var662.y1(), var662.y2())),
                                    var668,
                                    var670
                                 }
                              );
                        }

                        var648 = true;
                        break;
                     }

                     if (Boolean.getBoolean("streamsreflowing.caldiag") || af != null) {
                        Constants.LOG
                           .info(
                              "[SR-LAKEREJ] plate {} at {},{}: level={} keeps its w={} outflow (y={} at {},{})",
                              new Object[]{
                                 var0.key(),
                                 var639.cellMinX(),
                                 var639.cellMinZ(),
                                 var639.level(),
                                 String.format(Locale.ROOT, "%.1f", var664),
                                 String.format(Locale.ROOT, "%.1f", Math.min(var662.y1(), var662.y2())),
                                 var668,
                                 var670
                              }
                           );
                     }

                     while (var658 + 1 < var433.size() && var623[var658 + 1] == var623[var658]) {
                        var658 += 1;
                     }
                  }
               }
            }

            if (var648) {
               var272.remove(var630);
            }
         }
      }

      for (int var631 = 0; var631 < 4; var631 += 1) {
         int var640 = var433.size();
         a(var433, new ArrayList<>(Collections.nCopies(var433.size(), -1)), var272, new boolean[var272.size()], var12, var3, var1, var445);
         a(var433, var272);
         if (var433.size() == var640) {
            break;
         }

         Constants.LOG.debug("[SR-MERGE] plate {}: final law check removed {} segment(s)", var0.key(), var640 - var433.size());
      }

      List<Sink> var632 = a(var445, var433);
      a(var0.key(), var433, var272, var632, var12, var1, var3, var8, var233, var269, var251, var201);
      if (RiverEngine.deadEndResolver() != null) {
         Constants.LOG
            .info(
               "[SR-SINKS] plate {}: cuts={} offered={} kept={} live={} (segs={})",
               new Object[]{var0.key(), bo.getAndSet(2, 0L), bo.getAndSet(0, 0L), bo.getAndSet(1, 0L), var632.size(), var433.size()}
            );
      }

      return assemble(var0.key(), var433, var272, var632, Relief.of(var8), var3);
   }

   private static void a(List<RiverSegment> var0, int[] var1) {
      int var2 = var0.size();
      if (var2 != 0) {
         HashMap<Long, Integer> var3 = new HashMap<>();
         ArrayList<Double> var4 = new ArrayList<>();
         int[] var5 = new int[var2];
         int[] var6 = new int[var2];

         for (int var7 = 0; var7 < var2; var7++) {
            RiverSegment var8 = (RiverSegment)var0.get(var7);
            var5[var7] = a(var8.x1(), var8.z1(), var8.y1(), var3, var4);
            var6[var7] = a(var8.x2(), var8.z2(), var8.y2(), var3, var4);
         }

         int var20 = var4.size();
         double[] var21 = new double[var20];
         Arrays.fill(var21, Double.NEGATIVE_INFINITY);

         for (int var9 = 0; var9 < var2; var9++) {
            if (var1[var9] != Integer.MIN_VALUE) {
               var21[var5[var9]] = Math.max(var21[var5[var9]], var1[var9]);
               var21[var6[var9]] = Math.max(var21[var6[var9]], var1[var9]);
            }
         }

         int[] var22 = new int[var20];
         ArrayList<List<Integer>> var10 = new ArrayList<>(var20);

         for (int var11 = 0; var11 < var20; var11++) {
            var10.add(new ArrayList<>());
         }

         for (int var23 = 0; var23 < var2; var23++) {
            if (var5[var23] != var6[var23]) {
               var10.get(var5[var23]).add(var6[var23]);
               var22[var6[var23]]++;
            }
         }

         double[] var24 = new double[var20];

         for (int var12 = 0; var12 < var20; var12++) {
            var24[var12] = Math.max((Double)var4.get(var12), var21[var12]);
         }

         ArrayDeque<Integer> var25 = new ArrayDeque<>();

         for (int var13 = 0; var13 < var20; var13++) {
            if (var22[var13] == 0) {
               var25.add(var13);
            }
         }

         while (!var25.isEmpty()) {
            int var26 = (Integer)var25.poll();

            for (int var15 : var10.get(var26)) {
               if (var24[var26] < var24[var15]) {
                  var24[var15] = Math.max(var24[var26], var21[var15]);
               }

               if (--var22[var15] == 0) {
                  var25.add(var15);
               }
            }
         }

         boolean var27 = true;

         for (int var28 = 0; var27 && var28 < 64; var28++) {
            var27 = false;

            for (int var30 = 0; var30 < var2; var30++) {
               if (var5[var30] != var6[var30] && var24[var5[var30]] < var24[var6[var30]]) {
                  double var16 = Math.max(var24[var5[var30]], var21[var6[var30]]);
                  if (var16 < var24[var6[var30]]) {
                     var24[var6[var30]] = var16;
                     var27 = true;
                  }
               }
            }
         }

         for (int var29 = 0; var29 < var2; var29++) {
            RiverSegment var31 = (RiverSegment)var0.get(var29);
            double var32 = var24[var5[var29]];
            double var18 = Math.min(var32, var24[var6[var29]]);
            if (var32 != var31.y1() || var18 != var31.y2()) {
               var0.set(var29, new RiverSegment(var31.x1(), var31.z1(), var32, var31.w1(), var31.x2(), var31.z2(), var18, var31.w2(), var31.sp1(), var31.sp2()));
            }
         }
      }
   }

   private static void a(List<RiverSegment> var0) {
      int var1 = var0.size();
      if (var1 != 0) {
         HashMap<Long, Integer> var2 = new HashMap<>();
         ArrayList<Double> var3 = new ArrayList<>();
         int[] var4 = new int[var1];
         int[] var5 = new int[var1];

         for (int var6 = 0; var6 < var1; var6++) {
            RiverSegment var7 = (RiverSegment)var0.get(var6);
            var4[var6] = a(var7.x1(), var7.z1(), var7.y1(), var2, var3);
            var5[var6] = a(var7.x2(), var7.z2(), var7.y2(), var2, var3);
         }

         int var20 = var3.size();
         ArrayList<List<Integer>> var21 = new ArrayList<>(var20);

         for (int var8 = 0; var8 < var20; var8++) {
            var21.add(new ArrayList<>());
         }

         int[] var22 = new int[var20];

         for (int var9 = 0; var9 < var1; var9++) {
            if (var4[var9] != var5[var9]) {
               var21.get(var5[var9]).add(var9);
               var22[var4[var9]]++;
            }
         }

         double[] var23 = new double[var20];

         for (int var10 = 0; var10 < var20; var10++) {
            var23[var10] = (Double)var3.get(var10);
         }

         ArrayDeque<Integer> var24 = new ArrayDeque<>();

         for (int var11 = 0; var11 < var20; var11++) {
            if (var22[var11] == 0) {
               var24.add(var11);
            }
         }

         while (!var24.isEmpty()) {
            int var25 = (Integer)var24.poll();

            for (int var13 : var21.get(var25)) {
               int var14 = var4[var13];
               RiverSegment var15 = (RiverSegment)var0.get(var13);
               double var16 = Math.hypot(var15.x2() - var15.x1(), var15.z2() - var15.z1());
               double var18 = var23[var25] + 1.0 * var16;
               if (var18 < var23[var14]) {
                  var23[var14] = var18;
               }

               if (--var22[var14] == 0) {
                  var24.add(var14);
               }
            }
         }

         for (int var26 = 0; var26 < var1; var26++) {
            RiverSegment var27 = (RiverSegment)var0.get(var26);
            double var28 = Math.min(var27.y1(), var23[var4[var26]]);
            double var29 = Math.min(var28, Math.min(var27.y2(), var23[var5[var26]]));
            if (var28 != var27.y1() || var29 != var27.y2()) {
               var0.set(var26, new RiverSegment(var27.x1(), var27.z1(), var28, var27.w1(), var27.x2(), var27.z2(), var29, var27.w2(), var27.sp1(), var27.sp2()));
            }
         }
      }
   }

   private static int a(double var0, double var2, double var4, Map<Long, Integer> var6, List<Double> var7) {
      long var8 = Math.round(var0 * 16.0) & 4294967295L;
      long var10 = Math.round(var2 * 16.0) & 4294967295L;
      long var12 = var8 << 32 | var10;
      Integer var14 = (Integer)var6.get(var12);
      if (var14 == null) {
         var6.put(var12, var7.size());
         var7.add(var4);
         return var7.size() - 1;
      }

      if (var4 < (Double)var7.get(var14)) {
         var7.set(var14, var4);
      }

      return var14;
   }

   private static boolean a(
      List<RiverSegment> var0, int[] var1, int[] var2, StreamsConfig var3, TerrainSampler var4, int[] var5, Map<Long, List<Integer>> var6, double var7
   ) {
      int var9 = var0.size();
      if (var9 < 2) {
         return false;
      }

      ConcurrentHashMap var10 = new ConcurrentHashMap();
      a.reset();
      double var11 = Math.max(var3.maxWidth() * 2.0 + 2.0, 40.0);
      double[] var13 = new double[var9];

      for (int var14 = 0; var14 < var9; var14++) {
         var13[var14] = ((RiverSegment)var0.get(var14)).y2();
      }

      LowResGrid.parallelFor(
         var9,
         var11x -> {
            RiverSegment var12 = (RiverSegment)var0.get(var11x);
            double var13x = (var12.x1() + var12.x2()) * 0.5;
            double var15x = (var12.z1() + var12.z2()) * 0.5;
            double var17x = var12.widthAt(0.5);
            double var19x = Double.NaN;
            int var21 = (int)Math.floor((var13x - var11) / var7);
            int var22 = (int)Math.floor((var13x + var11) / var7);
            int var23 = (int)Math.floor((var15x - var11) / var7);
            int var24 = (int)Math.floor((var15x + var11) / var7);

            for (int var25 = var21; var25 <= var22; var25++) {
               for (int var26 = var23; var26 <= var24; var26++) {
                  List<Integer> var27 = var6.get((long)var25 << 32 ^ var26 & 4294967295L);
                  if (var27 != null) {
                     for (int var29 : var27) {
                        if (var5[var29] != var5[var11x]) {
                           RiverSegment var30 = (RiverSegment)var0.get(var29);
                           double var31 = var30.clampParam(var13x, var15x);
                           double var33 = var30.distanceAt(var13x, var15x, var31);
                           double var35 = var30.widthAt(var31);
                           if (var33 > var17x + var35 + 2.0) {
                              double var51 = var17x + Math.max(5.0, var17x * var3.bankWidth()) + var35 + Math.max(5.0, var35 * var3.bankWidth());
                              if (!(var33 > Math.min(40.0, var51))) {
                                 double var39 = var30.gradeAt(var31);
                                 if (!(var39 >= var12.y2() - 1.0) && (Double.isNaN(var19x) || !(var39 >= var19x))) {
                                    double var41 = var30.x1() + (var30.x2() - var30.x1()) * var31;
                                    double var43 = var30.z1() + (var30.z2() - var30.z1()) * var31;
                                    double var45 = Double.NEGATIVE_INFINITY;
                                    int var47 = ReadLattice.cell();

                                    for (int var48 = 1; var48 <= 3; var48++) {
                                       double var49 = var48 / 4.0;
                                       var45 = Math.max(
                                          var45,
                                          a(
                                             var4,
                                             var10,
                                             ReadLattice.snap(var13x + (var41 - var13x) * var49, var47),
                                             ReadLattice.snap(var15x + (var43 - var15x) * var49, var47),
                                             a
                                          )
                                       );
                                    }

                                    if (!(var45 >= var12.y2() + 5.0)) {
                                       var19x = var39;
                                    }
                                 }
                              }
                           } else {
                              double var37 = var30.gradeAt(var31);
                              if (var37 < var12.y2() - 1.0 && (Double.isNaN(var19x) || var37 < var19x)) {
                                 var19x = var37;
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (!Double.isNaN(var19x)) {
               var13[var11x] = Math.min(var13[var11x], var19x);
            }
         }
      );
      boolean var19 = false;

      for (int var15 = 0; var15 < var9; var15++) {
         RiverSegment var16 = (RiverSegment)var0.get(var15);
         double var17 = var2[var15] == Integer.MIN_VALUE ? var13[var15] : Math.max(var13[var15], var2[var15]);
         if (var17 < var16.y2()) {
            var19 = true;
            if (af != null && b((var16.x1() + var16.x2()) * 0.5, (var16.z1() + var16.z2()) * 0.5)) {
               Constants.LOG
                  .info(
                     "[SR-SEGDUMP] capture DROP: seg {} sys={} y2 {} -> {}{}",
                     new Object[]{
                        var15,
                        var1[var15],
                        String.format(Locale.ROOT, "%.2f", var16.y2()),
                        String.format(Locale.ROOT, "%.2f", var17),
                        var2[var15] != Integer.MIN_VALUE && var13[var15] < var2[var15] ? " (held at lake floor " + var2[var15] + ")" : ""
                     }
                  );
            }

            var0.set(
               var15,
               new RiverSegment(
                  var16.x1(), var16.z1(), Math.max(var16.y1(), var17), var16.w1(), var16.x2(), var16.z2(), var17, var16.w2(), var16.sp1(), var16.sp2()
               )
            );
         }
      }

      return var19;
   }

   private static int a(List<RiverSegment> var0, StreamsConfig var1) {
      int var2 = var0.size();
      if (var2 != 0 && !Boolean.getBoolean("streamsreflowing.nowidthwaist")) {
         int[] var3 = chainIds(var0);
         HashMap<Integer, Map<Long, Integer>> var4 = new HashMap<>();
         HashMap<Integer, Set<Long>> var5 = new HashMap<>();

         for (int var6 = 0; var6 < var2; var6++) {
            RiverSegment var7 = (RiverSegment)var0.get(var6);
            var4.computeIfAbsent(var3[var6], var0x -> new HashMap<>()).put(a(var7.x1(), var7.z1()), var6);
            var5.computeIfAbsent(var3[var6], var0x -> new HashSet<>()).add(a(var7.x2(), var7.z2()));
         }

         ArrayList<int[]> var25 = new ArrayList<>();

         for (Entry<Integer, Map<Long, Integer>> var8 : var4.entrySet()) {
            Set<Long> var9 = var5.get(var8.getKey());

            for (Entry<Long, Integer> var11 : var8.getValue().entrySet()) {
               if (!var9.contains(var11.getKey())) {
                  var25.add(new int[]{(Integer)var8.getKey(), (Integer)var11.getValue()});
                  break;
               }
            }
         }

         HashMap<Long, Double> var27 = new HashMap<>();
         int var28 = 0;

         for (int var29 = 0; var29 < 64; var29++) {
            boolean var30 = false;

            for (int[] var12 : var25) {
               Map<Long, Integer> var13 = var4.get(var12[0]);
               RiverSegment var14 = (RiverSegment)var0.get(var12[1]);
               double var15 = var27.getOrDefault(a(var14.x1(), var14.z1()), 0.0);
               int var17 = var12[1];
               long var18 = 0L;

               for (int var20 = 0; var20 < var2 && var17 >= 0; var20++) {
                  RiverSegment var21 = (RiverSegment)var0.get(var17);
                  var15 = Math.max(var15, var21.w1());
                  double var22 = Math.max(var15 * 0.5, a(var15, var1));
                  if (var21.w1() < var22 || var21.w2() < var22) {
                     var0.set(
                        var17,
                        new RiverSegment(
                           var21.x1(),
                           var21.z1(),
                           var21.y1(),
                           Math.max(var21.w1(), var22),
                           var21.x2(),
                           var21.z2(),
                           var21.y2(),
                           Math.max(var21.w2(), var22),
                           var21.sp1(),
                           var21.sp2()
                        )
                     );
                     var28++;
                  }

                  var15 = Math.max(var15, var21.w2());
                  var18 = a(var21.x2(), var21.z2());
                  Integer var24 = (Integer)var13.get(var18);
                  var17 = var24 == null ? -1 : var24;
               }

               Double var33 = (Double)var27.get(var18);
               if (var33 == null || var15 > var33) {
                  var27.put(var18, var15);
                  var30 = true;
               }
            }

            if (!var30) {
               break;
            }
         }

         return var28;
      } else {
         return 0;
      }
   }

   private static double a(double var0, StreamsConfig var2) {
      int var3 = RiverEngine.insetFor(var0, var2);
      double var4 = 0.0;
      double var6 = var0;

      for (int var8 = 0; var8 < 24; var8++) {
         double var9 = (var4 + var6) * 0.5;
         if (RiverEngine.insetFor(var9, var2) >= var3) {
            var6 = var9;
         } else {
            var4 = var9;
         }
      }

      return var6;
   }

   private static int b(List<RiverSegment> var0, StreamsConfig var1) {
      int var2 = var0.size();
      if (var2 >= 2 && !Boolean.getBoolean("streamsreflowing.nomergecrossings")) {
         int[] var3 = chainIds(var0);
         HashMap<Long, List<Integer>> var4 = new HashMap<>(var2 * 2);
         HashMap<Integer, Set<Integer>> var5 = new HashMap<>();

         for (int var6 = 0; var6 < var2; var6++) {
            RiverSegment var7 = (RiverSegment)var0.get(var6);
            long[] var8 = new long[]{a(var7.x1(), var7.z1()), a(var7.x2(), var7.z2())};

            for (long var12 : var8) {
               List<Integer> var14 = var4.computeIfAbsent(var12, var0x -> new ArrayList<>(2));
               if (!var14.contains(var3[var6])) {
                  for (int var16 : var14) {
                     var5.computeIfAbsent(var3[var6], var0x -> new HashSet<>()).add(var16);
                     var5.computeIfAbsent(var16, var0x -> new HashSet<>()).add(var3[var6]);
                  }

                  var14.add(var3[var6]);
               }
            }
         }

         double var36 = Math.max(8.0, var1.maxWidth() * 2.0);
         HashMap<Long, List<Integer>> var37 = new HashMap<>();

         for (int var38 = 0; var38 < var2; var38++) {
            RiverSegment var40 = (RiverSegment)var0.get(var38);
            int var41 = (int)Math.floor(var40.minX() / var36);
            int var43 = (int)Math.floor(var40.maxX() / var36);
            int var13 = (int)Math.floor(var40.minZ() / var36);
            int var47 = (int)Math.floor(var40.maxZ() / var36);

            for (int var50 = var41; var50 <= var43; var50++) {
               for (int var52 = var13; var52 <= var47; var52++) {
                  var37.computeIfAbsent((long)var50 << 32 ^ var52 & 4294967295L, var0x -> new ArrayList<>()).add(var38);
               }
            }
         }

         double var39 = var1.maxWidth() + 1.0 + 2.0;
         boolean[] var42 = new boolean[var2];
         int var44 = 0;

         label98:
         for (int var45 = 0; var45 < var2; var45++) {
            RiverSegment var48 = (RiverSegment)var0.get(var45);
            if (!(var48.widthAt(0.5) > 3.5)) {
               double var51 = var48.widthAt(0.5);
               double var17 = (var48.x1() + var48.x2()) * 0.5;
               double var19 = (var48.z1() + var48.z2()) * 0.5;
               Set<Integer> var21 = var5.get(var3[var45]);
               int var22 = (int)Math.floor((var17 - var39) / var36);
               int var23 = (int)Math.floor((var17 + var39) / var36);
               int var24 = (int)Math.floor((var19 - var39) / var36);
               int var25 = (int)Math.floor((var19 + var39) / var36);

               for (int var26 = var22; var26 <= var23; var26++) {
                  for (int var27 = var24; var27 <= var25; var27++) {
                     List<Integer> var28 = var37.get((long)var26 << 32 ^ var27 & 4294967295L);
                     if (var28 != null) {
                        for (int var30 : var28) {
                           if (var3[var30] != var3[var45] && (var21 == null || !var21.contains(var3[var30]))) {
                              RiverSegment var31 = (RiverSegment)var0.get(var30);
                              double var32 = var31.clampParam(var17, var19);
                              double var34 = var31.widthAt(var32);
                              if (!(var34 < var51 * 3.0) && var31.distanceAt(var17, var19, var32) <= var34 - 1.0) {
                                 var42[var45] = true;
                                 var44++;
                                 continue label98;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var44 == 0) {
            return 0;
         }

         ArrayList<RiverSegment> var46 = new ArrayList<>(var2 - var44);

         for (int var49 = 0; var49 < var2; var49++) {
            if (!var42[var49]) {
               var46.add(var0.get(var49));
            }
         }

         var0.clear();
         var0.addAll(var46);
         return var44;
      } else {
         return 0;
      }
   }

   private static double a(TerrainSampler var0, int var1, int var2, int var3) {
      double var4 = var0.riverSurface(var1, var2);
      if (!Double.isNaN(var4)) {
         return var4;
      }

      int var6 = Math.max(2, (int)Math.round(var3 * 0.375));

      for (int var7 = -1; var7 <= 1; var7++) {
         for (int var8 = -1; var8 <= 1; var8++) {
            if (var8 != 0 || var7 != 0) {
               var4 = var0.riverSurface(var1 + var8 * var6, var2 + var7 * var6);
               if (!Double.isNaN(var4)) {
                  return var4;
               }
            }
         }
      }

      return Double.NaN;
   }

   private static boolean a(TerrainSampler var0, RiverSegment var1) {
      if (bi) {
         return true;
      }

      if (a(var0, var1.x2(), var1.z2())) {
         return true;
      }

      double var2 = var1.x2() - var1.x1();
      double var4 = var1.z2() - var1.z1();
      double var6 = Math.hypot(var2, var4);
      if (var6 < 1.0E-6) {
         return false;
      }

      for (double var8 = 4.0; var8 <= 12.000000001; var8 += 4.0) {
         if (a(var0, var1.x2() - var2 / var6 * var8, var1.z2() - var4 / var6 * var8)) {
            return true;
         }
      }

      return false;
   }

   private static boolean a(TerrainSampler var0, double var1, double var3) {
      int var5 = (int)Math.floor(var1);
      int var6 = (int)Math.floor(var3);
      return var0.surfaceHeight(var5, var6) - var0.groundHeightExact(var5, var6) >= 0.99;
   }

   private static void a(
      List<RiverSegment> var0, List<Integer> var1, List<Lake> var2, boolean[] var3, int var4, StreamsConfig var5, TerrainSampler var6, Collection<Sink> var7
   ) {
      if (var5.growFromWater() && !var0.isEmpty()) {
          HashSet<Long> var8 = new HashSet<>();

         for (Sink var10 : var7) {
            var8.add(a(var10.x(), var10.z()));
         }

         label357:
         for (int var37 = 0; var37 < 64; var37++) {
            int var38 = var0.size();
            int[] var11 = chainIds(var0);
             HashMap<Long, Set<Integer>> var12 = new HashMap<>(var38 * 2);

            for (int var13 = 0; var13 < var38; var13++) {
               RiverSegment var14 = (RiverSegment)var0.get(var13);
               var12.computeIfAbsent(a(var14.x1(), var14.z1()), var0x -> new HashSet<>(2)).add(var11[var13]);
            }

             HashMap<Integer, Integer> var39 = new HashMap<>();

            for (int var40 = 0; var40 < var38; var40++) {
               var39.put(var11[var40], var40);
            }

             HashSet<Integer> var41 = new HashSet<>();
             HashMap<Integer, List<Integer>> var15 = new HashMap<>();
             HashMap<Integer, Integer> var16 = af != null ? new HashMap<>() : null;
             Iterator<Entry<Integer, Integer>> var17 = var39.entrySet().iterator();

            while (true) {
                Entry<Integer, Integer> var18;
               RiverSegment var19;
               while (true) {
                  if (!var17.hasNext()) {
                      HashMap<Integer, List<Integer>> var42 = new HashMap<>();

                      for (Entry<Integer, List<Integer>> var45 : var15.entrySet()) {
                         for (Integer var55 : var45.getValue()) {
                            var42.computeIfAbsent(var55, var0x -> new ArrayList<>(2)).add(var45.getKey());
                        }
                     }

                      HashSet<Integer> var44 = new HashSet<>();
                      ArrayDeque<Integer> var46 = new ArrayDeque<>();

                     for (Integer var56 : var39.keySet()) {
                        if (!var15.containsKey(var56) && !var41.contains(var56)) {
                           var44.add(var56);
                           var46.add(var56);
                        }
                     }

                     while (!var46.isEmpty()) {
                         List<Integer> var50 = var42.get(var46.poll());
                        if (var50 != null) {
                           for (Integer var61 : var50) {
                              if (var44.add(var61)) {
                                 var46.add(var61);
                              }
                           }
                        }
                     }

                     for (Integer var58 : var15.keySet()) {
                        RiverSegment var62 = af != null ? (RiverSegment)var0.get((Integer)var39.get(var58)) : null;
                        boolean var64 = var62 != null && b(var62.x2(), var62.z2());
                        if (!var44.contains(var58)) {
                           var41.add(var58);
                           if (var64) {
                              Constants.LOG
                                 .info(
                                    "[SR-SEGDUMP] prune DROP chain #{} (no continuation reaches an outlet) tail=({},{}) pass {} | all: {}",
                                    new Object[]{var58, (int)var62.x2(), (int)var62.z2(), var37, var15.get(var58)}
                                 );
                           }
                        } else if (var64) {
                           HashSet var67 = new HashSet();
                           StringBuilder var69 = new StringBuilder();
                           int var71 = var58;
                           boolean var27 = false;
                           boolean var72 = true;

                           while (var67.add(var71)) {
                              Integer var74 = (Integer)var16.get(var71);
                              if (var74 == null) {
                                 var27 = var44.contains(var71);
                                 var72 = false;
                                 break;
                              }

                              RiverSegment var75 = (RiverSegment)var0.get((Integer)var39.get(var74));
                              var69.append(String.format(Locale.ROOT, " -> #%d (ends %.3f,%.3f)", var74, var75.x2(), var75.z2()));
                              var71 = var74;
                           }

                           if (!var27) {
                              Constants.LOG
                                 .info(
                                    "[SR-SEGDUMP] prune KEEP chain #{} tail=({},{}) pass {}: the first chain listed on its tail vertex leads nowhere (#{}{}{}), another reaches an outlet | all: {}",
                                    new Object[]{
                                       var58,
                                       String.format(Locale.ROOT, "%.3f", var62.x2()),
                                       String.format(Locale.ROOT, "%.3f", var62.z2()),
                                       var37,
                                       var58,
                                       var69,
                                       var72 ? " -> round again" : "",
                                       var15.get(var58)
                                    }
                                 );
                           }
                        }
                     }

                     if (var41.isEmpty()) {
                        return;
                     }

                     int var52 = 0;

                     for (int var59 = 0; var59 < var38; var59++) {
                        if (!var41.contains(var11[var59])) {
                           var0.set(var52, (RiverSegment)var0.get(var59));
                           var1.set(var52, (Integer)var1.get(var59));
                           var52++;
                        }
                     }

                     while (var0.size() > var52) {
                        var0.remove(var0.size() - 1);
                        var1.remove(var1.size() - 1);
                     }

                     Constants.LOG.debug("[SR-MERGE] orphan prune pass {}: removed {} chains", var37, var41.size());
                     continue label357;
                  }

                  var18 = (Entry)var17.next();
                  var19 = (RiverSegment)var0.get((Integer)var18.getValue());
                  if (!(var19.y2() <= var4 + 1)) {
                     break;
                  }

                  if (!a(var6, var19)) {
                     if (af == null || !b(var19.x2(), var19.z2())) {
                        break;
                     }

                     int var20 = (int)Math.floor(var19.x2());
                     int var21 = (int)Math.floor(var19.z2());
                     double var22 = Double.NaN;

                     for (int var24 = 4; var24 <= 32 && Double.isNaN(var22); var24 += 4) {
                        for (int var25 = -var24; var25 <= var24 && Double.isNaN(var22); var25 += 4) {
                           for (int var26 = -var24; var26 <= var24; var26 += 4) {
                              if (Math.max(Math.abs(var25), Math.abs(var26)) == var24 && a(var6, var20 + var25, var21 + var26)) {
                                 var22 = Math.hypot(var25, var26);
                                 break;
                              }
                           }
                        }
                     }

                     Constants.LOG
                        .info(
                           "[SR-SEGDUMP] prune: sea-grade tail at ({},{}) y={} has NO real water (ground {}, nearest water {}) -> not a sea mouth",
                           new Object[]{
                              var20,
                              var21,
                              String.format(Locale.ROOT, "%.2f", var19.y2()),
                              String.format(Locale.ROOT, "%.1f", var6.groundHeightReal(var20, var21)),
                              Double.isNaN(var22) ? ">32" : String.format(Locale.ROOT, "%.0f", var22)
                           }
                        );
                     break;
                  }
               }

               boolean var47 = false;

               for (int var53 = 0; var53 < var2.size(); var53++) {
                  if (!var3[var53] && ((Lake)var2.get(var53)).wetWithin((int)Math.floor(var19.x2()), (int)Math.floor(var19.z2()), 1)) {
                     var47 = true;
                     break;
                  }
               }

               if (!var47 && !var8.contains(a(var19.x2(), var19.z2()))) {
                   ArrayList<Integer> var54 = null;
                   Set<Integer> var60 = var12.get(a(var19.x2(), var19.z2()));
                  if (var60 != null) {
                     for (Integer var65 : var60) {
                        if (!var65.equals(var18.getKey())) {
                           if (var54 == null) {
                            var54 = new ArrayList<>(2);
                           }

                           var54.add(var65);
                        }
                     }
                  }

                  if (var54 != null) {
                     if (var16 != null) {
                            var16.put(var18.getKey(), var54.get(0));
                     }

                     var54.sort(null);
                            var15.put(var18.getKey(), var54);
                  } else {
                     int var63 = (int)Math.floor(var19.x2());
                     int var66 = (int)Math.floor(var19.z2());
                     if (Double.isNaN(a(var6, var63, var66, var5.gridSpacing())) && var6.waterBiomeClassAt(var63, (int)Math.round(var19.y2()), var66) != 1) {
                            var41.add(var18.getKey());
                        if (af != null && b(var19.x2(), var19.z2())) {
                           String var68 = "no chain head within 3";
                           double var70 = 9.0;
                           HashSet var28 = new HashSet();

                           for (int var29 = 0; var29 < var38; var29++) {
                              if (var28.add(var11[var29]) && var11[var29] != (Integer)var18.getKey()) {
                                 RiverSegment var30 = (RiverSegment)var0.get(var29);
                                 double var31 = var30.x1() - var19.x2();
                                 double var33 = var30.z1() - var19.z2();
                                 double var35 = var31 * var31 + var33 * var33;
                                 if (var35 < var70) {
                                    var70 = var35;
                                    var68 = String.format(
                                       Locale.ROOT,
                                       "chain #%d head=(%.3f,%.3f) y=%.2f w1=%.2f fedKeyPresent=%b",
                                       var11[var29],
                                       var30.x1(),
                                       var30.z1(),
                                       var30.y1(),
                                       var30.w1(),
                                       var12.containsKey(a(var30.x1(), var30.z1()))
                                    );
                                 }
                              }
                           }

                           long var73 = a(var19.x2(), var19.z2());
                           StringBuilder var76 = new StringBuilder();

                            for (int var32 = Math.max(0, var18.getValue() - 3); var32 <= var18.getValue(); var32++) {
                              RiverSegment var78 = (RiverSegment)var0.get(var32);
                              var76.append(
                                 String.format(
                                    Locale.ROOT,
                                    " [%d (%.3f,%.3f)->(%.3f,%.3f) len %.3f w %.2f/%.2f]",
                                    var32,
                                    var78.x1(),
                                    var78.z1(),
                                    var78.x2(),
                                    var78.z2(),
                                    Math.hypot(var78.x2() - var78.x1(), var78.z2() - var78.z1()),
                                    var78.w1(),
                                    var78.w2()
                                 )
                              );
                           }

                           Constants.LOG.info("[SR-SEGDUMP]   its last segments:{}", var76);
                           StringBuilder var77 = new StringBuilder();

                           for (int var79 = 0; var79 < var38; var79++) {
                              RiverSegment var34 = (RiverSegment)var0.get(var79);
                              if (a(var34.x1(), var34.z1()) == var73) {
                                 var77.append(" [seg ").append(var79).append(" chain #").append(var11[var79]).append(']');
                              }
                           }

                           Constants.LOG
                              .info(
                                 "[SR-SEGDUMP] prune DROP chain #{} (dry tail, no outlet, no continuation) tail=({},{}) y={} w2={} tailIdx={} pass {} | cont={} | segs starting at the tail vertex:{} | nearest head: {}",
                                 new Object[]{
                                    var18.getKey(),
                                    String.format(Locale.ROOT, "%.3f", var19.x2()),
                                    String.format(Locale.ROOT, "%.3f", var19.z2()),
                                    String.format(Locale.ROOT, "%.2f", var19.y2()),
                                    String.format(Locale.ROOT, "%.2f", var19.w2()),
                                    var18.getValue(),
                                    var37,
                                    var12.get(var73),
                                    var77,
                                    var68
                                 }
                              );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void a(List<RiverSegment> var0, List<Lake> var1, boolean[] var2, double[] var3, int[] var4) {
      int var5 = var0.size();
      Arrays.fill(var3, Double.NaN);
      Arrays.fill(var4, -1);
      if (var5 != 0) {
         int[] var6 = chainIds(var0);
         double[] var7 = new double[var5];
         int[] var8 = new int[var5];

         for (int var9 = 0; var9 < var5; var9++) {
            RiverSegment var10 = (RiverSegment)var0.get(var9);
            var7[var9] = Double.NaN;
            var8[var9] = -1;
            int var11 = (int)Math.floor((var10.x1() + var10.x2()) * 0.5);
            int var12 = (int)Math.floor((var10.z1() + var10.z2()) * 0.5);
            double var13 = (var10.w1() + var10.w2()) * 0.5;

            for (int var15 = 0; var15 < var1.size(); var15++) {
               if (!var2[var15]) {
                  Lake var16 = (Lake)var1.get(var15);
                  int var17 = (int)Math.ceil(var13 / Math.max(1, var16.spacing()));
                  if (var16.wetWithin(var11, var12, var17) && (Double.isNaN(var7[var9]) || var16.level() > var7[var9])) {
                     var7[var9] = var16.level();
                     var8[var9] = var15;
                  }
               }
            }
         }

         boolean var18 = true;

         while (var18) {
            var18 = false;
            HashMap var19 = new HashMap();

            for (int var20 = 0; var20 < var5; var20++) {
               if (!Double.isNaN(var3[var20])) {
                  RiverSegment var22 = (RiverSegment)var0.get(var20);
                  long var24 = a(var22.x1(), var22.z1());
                  double[] var25 = (double[])var19.get(var24);
                  if (var25 == null || var3[var20] > var25[0]) {
                     var19.put(var24, new double[]{var3[var20], var4[var20]});
                  }
               }
            }

            int var21 = Integer.MIN_VALUE;
            double var23 = Double.NaN;
            int var14 = -1;

            for (int var26 = var5 - 1; var26 >= 0; var26--) {
               if (var6[var26] != var21) {
                  var21 = var6[var26];
                  RiverSegment var27 = (RiverSegment)var0.get(var26);
                  double[] var28 = (double[])var19.get(a(var27.x2(), var27.z2()));
                  var23 = var28 != null ? var28[0] : Double.NaN;
                  var14 = var28 != null ? (int)var28[1] : -1;
               }

               if (!Double.isNaN(var7[var26]) && (Double.isNaN(var23) || var7[var26] > var23)) {
                  var23 = var7[var26];
                  var14 = var8[var26];
               }

               if (!Double.isNaN(var23) && (Double.isNaN(var3[var26]) || var23 > var3[var26] + 1.0E-9)) {
                  var3[var26] = var23;
                  var4[var26] = var14;
                  var18 = true;
               }
            }
         }
      }
   }

   public static int[] chainIds(List<RiverSegment> var0) {
      int var1 = var0.size();
      HashMap<Long, Integer> var2 = new HashMap<>(var1 * 2);
      HashMap<Long, Integer> var3 = new HashMap<>(var1 * 2);
      HashMap<Long, Integer> var4 = new HashMap<>(var1 * 2);
      HashMap<Long, Integer> var5 = new HashMap<>(var1 * 2);

      for (int var6 = 0; var6 < var1; var6++) {
         RiverSegment var7 = (RiverSegment)var0.get(var6);
         long var8 = a(var7.x1(), var7.z1());
         long var10 = a(var7.x2(), var7.z2());
         var2.merge(var8, 1, Integer::sum);
         var4.put(var8, var6);
         var3.merge(var10, 1, Integer::sum);
         var5.put(var10, var6);
      }

      int[] var12 = new int[var1];
      int var13 = 0;

      while (var13 < var1) {
         var12[var13] = var13++;
      }

      for (Entry<Long, Integer> var16 : var4.entrySet()) {
         long var9 = var16.getKey();
         if (var2.getOrDefault(var9, 0) == 1 && var3.getOrDefault(var9, 0) == 1) {
            a(var12, var5.get(var9), var16.getValue());
         }
      }

      int[] var15 = new int[var1];

      for (int var17 = 0; var17 < var1; var17++) {
         var15[var17] = a(var12, var17);
      }

      return var15;
   }

   private static long a(double var0, double var2) {
      long var4 = Math.round(var0 * 16.0) & 4294967295L;
      long var6 = Math.round(var2 * 16.0) & 4294967295L;
      return var4 << 32 | var6;
   }

   private static int a(int[] var0, int var1) {
      while (var0[var1] != var1) {
         var0[var1] = var0[var0[var1]];
         var1 = var0[var1];
      }

      return var1;
   }

   private static void a(int[] var0, int var1, int var2) {
      int var3 = a(var0, var1);
      int var4 = a(var0, var2);
      if (var3 != var4) {
         var0[var3] = var4;
      }
   }

   private static List<RiverSegment> a(
      int var0,
      int[] var1,
      int[] var2,
      List<Lake> var3,
      Lake var4,
      boolean[] var5,
      int[] var6,
      double[] var7,
      double[] var8,
      LowResGrid var9,
      TerrainSampler var10,
      WetnessSampler var11,
      StreamsConfig var12,
      long var13,
      int var15,
      Queue<Sink> var16
   ) {
      ArrayList var17 = new ArrayList();
      ColumnCache var18 = new ColumnCache();
      ArrayList var19 = new ArrayList();
      var19.add(new NetworkTracer.Pt(var9.worldX(var9.gi(var0)), var9.worldZ(var9.gj(var0)), var7[var0], var8[var0]));
      int var20 = var0;
      boolean var21 = false;
      int var22 = -1;
      int var23 = -1;
      double var24 = Double.NaN;
      Lake var26 = null;

      while (true) {
         int var27 = var1[var20];
         if (var27 < 0) {
            var21 = true;
            break;
         }

         if (var2[var27] >= 0) {
            Lake var28 = (Lake)var3.get(var2[var27]);
            if (!var5[var27] || b(var28, b(var8[var20], var12))) {
               var26 = var28;
               var19.add(new NetworkTracer.Pt(var9.worldX(var9.gi(var27)), var9.worldZ(var9.gj(var27)), var26.level(), var8[var20]));
               var24 = var26.level();
               break;
            }
         }

         if (var9.riverOutlet(var27)) {
            double var58 = var7[var27];
            var19.add(new NetworkTracer.Pt(var9.worldX(var9.gi(var27)), var9.worldZ(var9.gj(var27)), Math.min(var7[var20], var58), var8[var20]));
            var24 = var58;
            var23 = var27;
            break;
         }

         if (!var5[var27]) {
            var19.add(new NetworkTracer.Pt(var9.worldX(var9.gi(var27)), var9.worldZ(var9.gj(var27)), var15, var8[var20]));
            var24 = var15;
            var22 = var27;
            break;
         }

         var19.add(new NetworkTracer.Pt(var9.worldX(var9.gi(var27)), var9.worldZ(var9.gj(var27)), var7[var27], var8[var27]));
         if (var6[var27] != 1) {
            break;
         }

         var20 = var27;
      }

      boolean var57 = false;
      double var59 = 0.0;
      double var30 = 0.0;
      double var32 = 0.0;
      if (var6[var0] == 0) {
         int var34 = var9.gi(var0);
         int var35 = var9.gj(var0);
         int var36 = -1;
         int var37 = Integer.MAX_VALUE;

         for (int var38 = -2; var38 <= 2; var38++) {
            for (int var39 = -2; var39 <= 2; var39++) {
               int var40 = var34 + var39;
               int var41 = var35 + var38;
               if (var9.valid(var40, var41) && var2[var9.idx(var40, var41)] >= 0) {
                  int var42 = var39 * var39 + var38 * var38;
                  if (var42 < var37) {
                     var37 = var42;
                     var36 = var9.idx(var40, var41);
                  }
               }
            }
         }

         if (var36 >= 0 && Math.abs(((Lake)var3.get(var2[var36])).level() - (var7[var0] - var12.ditchInset())) <= 4.0) {
            var57 = true;
            var59 = var9.worldX(var9.gi(var36));
            var30 = var9.worldZ(var9.gj(var36));
            var32 = ((Lake)var3.get(var2[var36])).level();
            int[] var64 = a((Lake)var3.get(var2[var36]), var59, var30);
            if (var64 != null) {
               var59 = var64[0] + 0.5;
               var30 = var64[1] + 0.5;
            }

            if (var26 != null && var26.level() >= var32 - 1.0) {
               return var17;
            }
         }
      }

      int var60 = var12.minLengthScope();
      boolean var61 = var22 >= 0 || var60 >= 2 && var23 >= 0 || var60 >= 3 && var26 != null;
      if (var61
         && var6[var0] == 0
         && !var57
         && var19.size() - 1 < var12.minSeaChainNodes()
         && var11.wetness((int)((NetworkTracer.Pt)var19.get(0)).x(), (int)var7[var0], (int)((NetworkTracer.Pt)var19.get(0)).z()) > var12.wetnessDryCutoff()) {
         a("DROP short-stub (under minSeaChainNodes)", var19);
         return var17;
      }

      boolean var62 = var6[var0] == 0 && !var57;
      b("walk", var19);
      boolean var63 = R || var12.legacyShaping();
      boolean var65 = var63 && var12.valleySnapRadius() > 0;
      boolean var66 = !var63 && var12.terrainFit() > 0;
      List var67 = null;
      if (var66) {
         long var68 = System.nanoTime();
         int var43 = HeightCensus.enter(16);
         List var44 = a(a(a(var19, var12.gridSpacing() * 0.4), var13, var0, var12), 1);
         b("draft", var44);
         var67 = a(var44, var10, var18, var12, var62, var3);
         HeightCensus.restore(var43);
         ak.addAndGet(6, System.nanoTime() - var68);
         b("band", var67);
         if (var62 && !var67.isEmpty()) {
            double var45 = a(var10, var18, ((NetworkTracer.Pt)var19.get(0)).x(), ((NetworkTracer.Pt)var19.get(0)).z());
            if (((NetworkTracer.Pt)var67.get(0)).y() < var45 - 8.0) {
               Constants.LOG
                  .debug(
                     "[SR-SRCTRACE] dug source ({},{}) fineGround={}: coarse={} -> band={}",
                     new Object[]{
                        (int)Math.round(((NetworkTracer.Pt)var19.get(0)).x()),
                        (int)Math.round(((NetworkTracer.Pt)var19.get(0)).z()),
                        (int)var45,
                        String.format("%.1f", ((NetworkTracer.Pt)var19.get(0)).y()),
                        String.format("%.1f", ((NetworkTracer.Pt)var67.get(0)).y())
                     }
                  );
            }
         }
      }

      long var69 = System.nanoTime();
      int var70 = HeightCensus.enter(3);
      List var71 = var66 ? var67 : (var65 ? c(var19, var10, var18, var12, var62, var3) : (var62 ? a(var19, var10, var18) : var19));
      HeightCensus.restore(var70);
      ak.addAndGet(0, System.nanoTime() - var69);
      if (var65 && var71.size() >= 2) {
         double var72 = ((NetworkTracer.Pt)var19.get(0)).y();
         double var47 = a(var10, var18, ((NetworkTracer.Pt)var19.get(0)).x(), ((NetworkTracer.Pt)var19.get(0)).z());
         double var49 = ((NetworkTracer.Pt)var71.get(0)).y();
         long var51 = System.nanoTime();
         int var53 = HeightCensus.enter(4);
         b("snap", var71);
         var71 = a(a(var71, var12.gridSpacing() * 0.4), var10, var18, var12, var3);
         b("follow", var71);
         HeightCensus.restore(var53);
         ak.addAndGet(1, System.nanoTime() - var51);
         if (var62 && ((NetworkTracer.Pt)var71.get(0)).y() < var47 - 8.0) {
            Constants.LOG
               .debug(
                  "[SR-SRCTRACE] dug source ({},{}) fineGround={}: coarse={} -> snapToValley={} -> followGround={}",
                  new Object[]{
                     (int)Math.round(((NetworkTracer.Pt)var19.get(0)).x()),
                     (int)Math.round(((NetworkTracer.Pt)var19.get(0)).z()),
                     (int)var47,
                     String.format("%.1f", var72),
                     String.format("%.1f", var49),
                     String.format("%.1f", ((NetworkTracer.Pt)var71.get(0)).y())
                  }
               );
         }
      }

      if (var62 && !var71.isEmpty() && ((NetworkTracer.Pt)var71.get(0)).y() < var15 + 2) {
         a(
            "DROP headwater-below-sea+2 (source y " + String.format(Locale.ROOT, "%.1f", ((NetworkTracer.Pt)var71.get(0)).y()) + " < " + (var15 + 2) + ")",
            var71
         );
         return var17;
      }

      if (var57 && !var71.isEmpty()) {
         ArrayList var73 = new ArrayList(var71.size() + 1);
         var73.add(
            new NetworkTracer.Pt(
               var59, var30, var32 + RiverEngine.insetFor(b(((NetworkTracer.Pt)var71.get(0)).acc(), var12), var12), ((NetworkTracer.Pt)var71.get(0)).acc()
            )
         );
         var73.addAll(var71);
         var71 = var73;
      }

      Lake var74 = var26 != null ? var26 : var4;
      if (var74 != null && var71.size() >= 2) {
         int var46 = var71.size();
         var71 = a(var71, var74, var10, var18, var12);
         if (var71.size() < var46 || var71.size() < 2) {
            boolean var80 = a(var71, var10, var12, var13, var0, var62, var11, var17, var16);
            a((var80 ? "SINK" : "DROP") + " lake-approach-truncated", var71);
            return var17;
         }
      }

      List var75 = var71;
      if (var71.size() >= 2) {
         if (var22 >= 0) {
            List var76 = a(var71, var9, var22, var15, var10, var12);
            if (var76 == var71) {
               NetworkTracer.Pt var48 = (NetworkTracer.Pt)var71.get(var71.size() - 1);
               double var83 = var71.size() >= 2 ? ((NetworkTracer.Pt)var71.get(var71.size() - 2)).y() : var48.y();
               double var86 = Math.min(var83, Math.max(var15, var7[var22]));
               ArrayList var87 = new ArrayList(var71);
               var87.set(var87.size() - 1, new NetworkTracer.Pt(var48.x(), var48.z(), var86, var48.acc()));
               boolean var54 = a(var87, var10, var12, var13, var0, var62, var11, var17, var16);
               a((var54 ? "SINK" : "DROP") + " sea-walk-found-no-water", var71);
               return var17;
            }

            var71 = var76;
         } else if (var23 >= 0) {
            List var77 = a(var71, var9, var23, var10, var12);
            if (var77 == var71) {
               boolean var81 = a(var71, var10, var12, var13, var0, var62, var11, var17, var16);
               a((var81 ? "SINK" : "DROP") + " river-walk-found-no-water", var71);
               return var17;
            }

            var71 = var77;
         } else if (!Double.isNaN(var24)) {
            var71 = b(var71, var24, 8.0);
         } else if (var21) {
            List var78 = a(var71, var9, var20, var15, var10, var12);
            if (var78 == var71) {
               boolean var82 = a(var71, var10, var12, var13, var0, var62, var11, var17, var16);
               a((var82 ? "SINK" : "DROP") + " dead-end-drainage-no-water", var71);
               return var17;
            }

            var71 = var78;
         }

         long var79 = System.nanoTime();
         b("pre-shape", var71);
         List var84 = var66 ? a(var71, var75, var13, var0, var12) : a(a(var71, var12.gridSpacing() * 0.4), var13, var0, var12);
         b("meander", var84);
         long var50 = System.nanoTime();
         if (var12.confineToBanks()) {
            var84 = a(var84, var10, var18, var12);
            b("confine", var84);
         }

         long var52 = System.nanoTime();
         var84 = a(var84, var12.smoothingPasses());
         b("smooth", var84);
         if (!var63) {
            var84 = c(var84, var12);
            b("cutoff", var84);
         }

         long var88 = System.nanoTime();
         boolean var56 = var22 >= 0
            || var23 >= 0
            || var26 != null
            || !Double.isNaN(var24)
            || ((NetworkTracer.Pt)var84.get(var84.size() - 1)).y() <= var15 + 1.5;
         a(var84, var12, var17, var62, var11, var13, var0, var56);
         ak.addAndGet(2, var50 - var79);
         ak.addAndGet(3, var52 - var50);
         ak.addAndGet(4, var88 - var52);
         ak.addAndGet(5, System.nanoTime() - var88);
      }

      return var17;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, Lake var1, TerrainSampler var2, ColumnCache var3, StreamsConfig var4) {
      int var5 = var1.level();
      ArrayList var6 = new ArrayList(var0.size());
      boolean var7 = false;

      for (NetworkTracer.Pt var9 : var0) {
         if (!var1.wetAt((int)Math.floor(var9.x()), (int)Math.floor(var9.z()))) {
            double var10 = a(var2, var3, var9.x(), var9.z());
            if (var10 < var5 - 4.0) {
               if (af != null && b(var9.x(), var9.z())) {
                  Constants.LOG
                     .info(
                        "[SR-SEGDUMP] modifier lakeApproach TRUNCATE at ({},{}) ground={} < lake {} - {}",
                        new Object[]{(int)var9.x(), (int)var9.z(), (int)var10, var5, 4}
                     );
               }
               break;
            }

            double var12 = var5 + RiverEngine.insetFor(b(var9.acc(), var4), var4);
            if (var9.y() < var12) {
               if (!var7 && af != null && b(var9.x(), var9.z())) {
                  Constants.LOG
                     .info(
                        "[SR-SEGDUMP] modifier lakeApproach HOLD from ({},{}) y {} -> {} (lake {} + inset)",
                        new Object[]{
                           (int)var9.x(), (int)var9.z(), String.format(Locale.ROOT, "%.1f", var9.y()), String.format(Locale.ROOT, "%.1f", var12), var5
                        }
                     );
                  var7 = true;
               }

               var9 = new NetworkTracer.Pt(var9.x(), var9.z(), var12, var9.acc());
            }
         }

         var6.add(var9);
      }

      if (var6.size() == var0.size() && !var6.isEmpty()) {
         NetworkTracer.Pt var14 = (NetworkTracer.Pt)var6.get(var6.size() - 1);
         if (!var1.wetAt((int)Math.floor(var14.x()), (int)Math.floor(var14.z()))) {
            int[] var15 = a(var1, var14.x(), var14.z());
            if (var15 != null) {
               double var16 = var5 + RiverEngine.insetFor(b(var14.acc(), var4), var4);
               var6.add(new NetworkTracer.Pt(var15[0] + 0.5, var15[1] + 0.5, Math.min(var14.y(), var16), var14.acc()));
            }
         }
      }

      return var6;
   }

   private static int[] a(Lake var0, double var1, double var3) {
      int var5 = (int)Math.floor(var1);
      int var6 = (int)Math.floor(var3);
      if (var0.wetAt(var5, var6)) {
         return new int[]{var5, var6};
      }

      int var7 = (var0.reachCells() + 2) * Math.max(1, var0.spacing());

      for (int var8 = 1; var8 <= var7; var8++) {
         int var9 = Integer.MAX_VALUE;
         int var10 = 0;
         int var11 = 0;

         for (int var12 = -var8; var12 <= var8; var12++) {
            for (int var13 = -var8; var13 <= var8; var13++) {
               if (Math.max(Math.abs(var13), Math.abs(var12)) == var8) {
                  int var14 = var13 * var13 + var12 * var12;
                  if (var14 < var9 && var0.wetAt(var5 + var13, var6 + var12)) {
                     var9 = var14;
                     var10 = var5 + var13;
                     var11 = var6 + var12;
                  }
               }
            }
         }

         if (var9 != Integer.MAX_VALUE) {
            return new int[]{var10, var11};
         }
      }

      return null;
   }

   public static RiverNetwork assemble(PlateKey var0, List<RiverSegment> var1, List<Lake> var2, StreamsConfig var3) {
      return assemble(var0, var1, var2, List.of(), var3);
   }

   public static RiverNetwork assemble(PlateKey var0, List<RiverSegment> var1, List<Lake> var2, List<Sink> var3, StreamsConfig var4) {
      return assemble(var0, var1, var2, var3, null, var4);
   }

   public static RiverNetwork assemble(PlateKey var0, List<RiverSegment> var1, List<Lake> var2, List<Sink> var3, Relief var4, StreamsConfig var5) {
      List var6 = var2;
      NearestRiverIndex var7 = new NearestRiverIndex(
         var1, var5.maxWidth() * (1.0 + var5.bankWidth()) + 2.0, Math.max(16, var5.gridSpacing()), var5.bankWidth(), var6
      );
      RiverNetwork var8 = new RiverNetwork(var0, var1, var7, var6, var3, var4);
      if (bj) {
         a(var0, var8, var1, var6, var3, var4, var5);
      }

      return var8;
   }

   static List<Lake> a(List<Lake> var0, PlateKey var1) {
      int var2 = var0.size();
      if (var2 < 2) {
         return var0;
      }

      int[][] var3 = new int[var2][];
      int[][] var4 = new int[var2][];

      for (int var5 = 0; var5 < var2; var5++) {
         Lake var6 = (Lake)var0.get(var5);
         if (var6.digDepth() == 0) {
            int var7 = var6.cellsWide() * var6.cellsHigh();
            int[] var8 = new int[Math.min(var7, 65536)];
            int[] var9 = new int[var8.length];
            int var10 = 0;

            for (int var11 = 0; var11 < var6.cellsHigh() && var10 < var8.length; var11++) {
               for (int var12 = 0; var12 < var6.cellsWide() && var10 < var8.length; var12++) {
                  int var13 = var6.cellMinX() + var12 * var6.spacing();
                  int var14 = var6.cellMinZ() + var11 * var6.spacing();
                  if (var6.covers(var13, var14) && var6.wetAt(var13, var14)) {
                     var8[var10] = var13;
                     var9[var10] = var14;
                     var10++;
                  }
               }
            }

            var3[var5] = Arrays.copyOf(var8, var10);
            var4[var5] = Arrays.copyOf(var9, var10);
         }
      }

      int[] var16 = new int[var2];
      int var17 = 0;

      while (var17 < var2) {
         var16[var17] = var17++;
      }

      boolean var18 = false;

      for (int var19 = 0; var19 < var2; var19++) {
         if (var3[var19] != null && var3[var19].length != 0) {
            for (int var21 = var19 + 1; var21 < var2; var21++) {
               if (var3[var21] != null
                  && var3[var21].length != 0
                  && a((Lake)var0.get(var19), (Lake)var0.get(var21))
                  && (a(var3[var19], var4[var19], (Lake)var0.get(var21)) > 0 || a(var3[var21], var4[var21], (Lake)var0.get(var19)) > 0)) {
                  b(var16, var19, var21);
                  var18 = true;
               }
            }
         }
      }

      if (!var18) {
         return var0;
      }

      ArrayList<Lake> var20 = new ArrayList<>(var0);

      for (int var22 = 0; var22 < var2; var22++) {
         if (b(var16, var22) == var22) {
            ArrayList<Integer> var23 = new ArrayList<>();

            for (int var24 = 0; var24 < var2; var24++) {
               if (b(var16, var24) == var22) {
                  var23.add(var24);
               }
            }

            if (var23.size() >= 2) {
               int var25 = -1;

               for (int var28 : var23) {
                  boolean var31 = true;

                  for (int var15 : var23) {
                     if (var15 != var28 && a(var3[var15], var4[var15], (Lake)var0.get(var28)) < var3[var15].length) {
                        var31 = false;
                        break;
                     }
                  }

                  if (var31) {
                     var25 = var28;
                     break;
                  }
               }

               int var27 = var25 >= 0 ? ((Lake)var0.get(var25)).level() : Integer.MAX_VALUE;
               if (var25 < 0) {
                  for (int var32 : var23) {
                     var27 = Math.min(var27, ((Lake)var0.get(var32)).level());
                  }
               }

               for (int var33 : var23) {
                  Lake var35 = (Lake)var0.get(var33);
                  if (var35.level() != var27) {
                     var20.set(
                        var33,
                        new Lake(
                           var27,
                           var35.digDepth(),
                           var35.spacing(),
                           var35.cellMinX(),
                           var35.cellMinZ(),
                           var35.cellsWide(),
                           var35.cellsHigh(),
                           var35.mask(),
                           var35.rounding()
                        )
                     );
                     Constants.LOG
                        .debug(
                           "[SR-LAKEBODY] plate {}: lake at ({},{}) joins one body with the water around it, {} -> {}",
                           new Object[]{var1, var35.cellMinX(), var35.cellMinZ(), var35.level(), var27}
                        );
                  }
               }
            }
         }
      }

      return var20;
   }

   private static boolean a(Lake var0, Lake var1) {
      int var2 = Math.max(var0.spacing(), var1.spacing());
      int var3 = var0.cellMinX() + var0.cellsWide() * var0.spacing();
      int var4 = var0.cellMinZ() + var0.cellsHigh() * var0.spacing();
      int var5 = var1.cellMinX() + var1.cellsWide() * var1.spacing();
      int var6 = var1.cellMinZ() + var1.cellsHigh() * var1.spacing();
      return var0.cellMinX() - var2 <= var5 && var1.cellMinX() - var2 <= var3 && var0.cellMinZ() - var2 <= var6 && var1.cellMinZ() - var2 <= var4;
   }

   private static int a(int[] var0, int[] var1, Lake var2) {
      int var3 = 0;

      for (int var4 = 0; var4 < var0.length; var4++) {
         if (var2.wetAt(var0[var4], var1[var4])) {
            var3++;
         }
      }

      return var3;
   }

   private static int b(int[] var0, int var1) {
      while (var0[var1] != var1) {
         var0[var1] = var0[var0[var1]];
         var1 = var0[var1];
      }

      return var1;
   }

   private static void b(int[] var0, int var1, int var2) {
      int var3 = b(var0, var1);
      int var4 = b(var0, var2);
      if (var3 != var4) {
         var0[Math.max(var3, var4)] = Math.min(var3, var4);
      }
   }

   private static void a(PlateKey var0, RiverNetwork var1, List<RiverSegment> var2, List<Lake> var3, List<Sink> var4, Relief var5, StreamsConfig var6) {
      if (!var2.isEmpty()) {
         double var7 = var6.maxWidth() * (1.0 + var6.bankWidth()) + 2.0;
         RiverNetwork var9 = new RiverNetwork(
            var0, var2, new NearestRiverIndex(var2, var7, Math.max(16, var6.gridSpacing()), var6.bankWidth(), var3, true), var3, var4, var5
         );
         double var10 = Double.MAX_VALUE;
         double var12 = Double.MAX_VALUE;
         double var14 = -Double.MAX_VALUE;
         double var16 = -Double.MAX_VALUE;

         for (RiverSegment var19 : var2) {
            var10 = Math.min(var10, Math.min(var19.x1(), var19.x2()) - var7);
            var14 = Math.max(var14, Math.max(var19.x1(), var19.x2()) + var7);
            var12 = Math.min(var12, Math.min(var19.z1(), var19.z2()) - var7);
            var16 = Math.max(var16, Math.max(var19.z1(), var19.z2()) + var7);
         }

         SplittableRandom var45 = new SplittableRandom(var0.cx() * -7046029254386353131L ^ var0.cz());
         double[] var46 = new double[]{var7, var7 + 16.0, 8.0};
         int var20 = 0;
         int var21 = 0;
         String var22 = null;

         for (int var23 = 0; var23 < 6000; var23++) {
            double var24;
            double var26;
            if ((var23 & 1) == 0) {
               RiverSegment var28 = (RiverSegment)var2.get(var45.nextInt(var2.size()));
               var24 = var28.x1() + (var45.nextDouble() * 2.0 - 1.0) * var7;
               var26 = var28.z1() + (var45.nextDouble() * 2.0 - 1.0) * var7;
            } else {
               var24 = var10 + var45.nextDouble() * (var14 - var10);
               var26 = var12 + var45.nextDouble() * (var16 - var12);
            }

            double var47 = var46[var23 % var46.length];
            Object[][] var30 = new Object[7][];
            RiverSample var31 = var1.sample(var24, var26, var47);
            var30[0] = new Object[]{"sample", var31, var9.sample(var24, var26, var47)};
            RiverSample var32 = var1.sampleTiered(var24, var26, var47);
            RiverSample var33 = var1.sampleTiered(var24, var26, var47);
            RiverSample var34 = var9.sampleTiered(var24, var26, var47);
            RiverSample var35 = var9.sampleTiered(var24, var26, var47);
            var30[1] = new Object[]{"sampleTiered", var32, var34};
            var30[2] = new Object[]{"sampleTiered(union)", var33, var35};
            int var36 = var31 == null ? -1 : var31.segId();
            var30[3] = new Object[]{
               "sampleCoveringForeign", var1.sampleCoveringForeign(var24, var26, var47, var36), var9.sampleCoveringForeign(var24, var26, var47, var36)
            };
            var30[4] = new Object[]{"inAnyChannel", var1.inAnyChannel(var24, var26, var47), var9.inAnyChannel(var24, var26, var47)};
            var30[5] = new Object[]{
               "lowerNeighborGrade",
               var1.lowerNeighborGrade(var24, var26, var47, var36, 400.0, 1.0),
               var9.lowerNeighborGrade(var24, var26, var47, var36, 400.0, 1.0)
            };
            double var37 = Math.floor(var24 / 16.0) * 16.0;
            double var39 = Math.floor(var26 / 16.0) * 16.0;
            var30[6] = new Object[]{
               "corridorTouchesBox",
               var1.corridorTouchesBox(var37, var39, var37 + 16.0, var39 + 16.0),
               var9.corridorTouchesBox(var37, var39, var37 + 16.0, var39 + 16.0)
            };

            for (Object[] var44 : var30) {
               var20++;
               if (!Objects.equals(var44[1], var44[2])) {
                  var21++;
                  if (var22 == null) {
                     var22 = var44[0] + " at (" + var24 + ", " + var26 + ") r=" + var47 + ": new " + var44[1] + " old " + var44[2];
                  }
               }
            }
         }

         Constants.LOG
            .info(
               "[SR-INDEXCHECK] plate {}: {} lookups against the old padding, {} disagreements; index {} KB, reference {} KB{}",
               new Object[]{
                  var0, var20, var21, var1.index().approxBytes() / 1024L, var9.index().approxBytes() / 1024L, var22 == null ? "" : " -- first: " + var22
               }
            );
      }
   }

   private static void a(List<RiverSegment> var0, List<Lake> var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < 64 && !var0.isEmpty(); var3++) {
         HashSet<Long> var4 = new HashSet<>();

         for (RiverSegment var6 : var0) {
            var4.add(a(var6.x2(), var6.z2()));
         }

         int[] var14 = chainIds(var0);
         HashMap<Integer, Integer> var15 = new HashMap<>();

         for (int var7 = 0; var7 < var0.size(); var7++) {
            var15.putIfAbsent(var14[var7], var7);
         }

         HashSet<Integer> var16 = new HashSet<>();

         for (Entry<Integer, Integer> var9 : var15.entrySet()) {
            RiverSegment var10 = var0.get(var9.getValue());
            if (!(var10.w1() <= 0.01) && !var4.contains(a(var10.x1(), var10.z1()))) {
               boolean var11 = false;

               for (Lake var13 : var1) {
                  if (var13.wetWithin((int)Math.floor(var10.x1()), (int)Math.floor(var10.z1()), 1)) {
                     var11 = true;
                     break;
                  }
               }

               if (!var11) {
                  var16.add(var9.getKey());
               }
            }
         }

         if (var16.isEmpty()) {
            break;
         }

         int var17 = 0;

         for (int var18 = 0; var18 < var0.size(); var18++) {
            if (!var16.contains(var14[var18])) {
               var0.set(var17++, (RiverSegment)var0.get(var18));
            }
         }

         while (var0.size() > var17) {
            var0.remove(var0.size() - 1);
         }

         var2 += var16.size();
      }

      if (var2 > 0) {
         Constants.LOG.debug("[SR-LAW] dropped {} chain(s) starting wide from nothing", var2);
      }
   }

   private static boolean[] a(List<RiverSegment> var0, int[] var1, Lake var2) {
      int var3 = var0.size();
      boolean[] var4 = new boolean[var3];
      int var5 = Math.max(1, var2.spacing());

      for (int var6 = 0; var6 < var3; var6++) {
         RiverSegment var7 = (RiverSegment)var0.get(var6);
         if (!(Math.min(var7.y1(), var7.y2()) < var2.level() - 3)) {
            double var8 = (var7.w1() + var7.w2()) * 0.5;
            int var10 = (int)Math.ceil(var8 / var5);
            int var11 = (int)Math.round((var7.x1() + var7.x2()) * 0.5);
            int var12 = (int)Math.round((var7.z1() + var7.z2()) * 0.5);
            int var13 = (var10 + var2.reachCells() + 1) * var5;
            if (var11 >= var2.cellMinX() - var13
               && var11 <= var2.cellMinX() + var2.cellsWide() * var5 + var13
               && var12 >= var2.cellMinZ() - var13
               && var12 <= var2.cellMinZ() + var2.cellsHigh() * var5 + var13) {
               var4[var6] = var2.wetWithin(var11, var12, var10);
            }
         }
      }

      boolean[] var14 = new boolean[var3];
      HashSet var15 = new HashSet();
      boolean var16 = true;

      for (int var9 = 0; var16 && var9 < 16; var9++) {
         var16 = false;
         int var17 = Integer.MIN_VALUE;
         boolean var18 = false;

         for (int var19 = 0; var19 < var3; var19++) {
            RiverSegment var20 = (RiverSegment)var0.get(var19);
            if (var1[var19] != var17) {
               var17 = var1[var19];
               var18 = var15.contains(a(var20.x1(), var20.z1()));
            }

            if (var4[var19]) {
               var18 = true;
            }

            if (var18) {
               if (!var14[var19]) {
                  var14[var19] = true;
                  var16 = true;
               }

               if (var15.add(a(var20.x2(), var20.z2()))) {
                  var16 = true;
               }
            }
         }
      }

      return var14;
   }

   private static List<Sink> a(Collection<Sink> var0, List<RiverSegment> var1) {
      if (var0.isEmpty()) {
         return List.of();
      }

      HashSet<Long> var2 = new HashSet<>();
      int[] var3 = chainIds(var1);
      HashMap<Integer, Integer> var4 = new HashMap<>();

      for (int var5 = 0; var5 < var1.size(); var5++) {
         var4.put(var3[var5], var5);
      }

      for (int var6 : var4.values()) {
         RiverSegment var7 = (RiverSegment)var1.get(var6);
         var2.add(a(var7.x2(), var7.z2()));
      }

      ArrayList<Sink> var9 = new ArrayList<>();

      for (Sink var11 : var0) {
         if (var2.contains(a(var11.x(), var11.z()))) {
            var9.add(var11);
         }
      }

      var9.sort(Comparator.comparingDouble(Sink::x).thenComparingDouble(Sink::z));
      return var9;
   }

   private static void a(
      PlateKey var0,
      List<RiverSegment> var1,
      List<Lake> var2,
      Collection<Sink> var3,
      int var4,
      TerrainSampler var5,
      StreamsConfig var6,
      LowResGrid var7,
      int[] var8,
      int[] var9,
      boolean[] var10,
      double[] var11
   ) {
      if (bk && !var1.isEmpty()) {
         int var12 = var1.size();
         int[] var13 = chainIds(var1);
          HashMap<Integer, Integer> var14 = new HashMap<>();

         for (int var15 = 0; var15 < var12; var15++) {
            var14.put(var13[var15], var15);
         }

         HashSet var42 = new HashSet();

         for (Sink var17 : var3) {
            var42.add(a(var17.x(), var17.z()));
         }

         int var43 = 0;

          for (Entry<Integer, Integer> var18 : var14.entrySet()) {
             RiverSegment var19 = var1.get(var18.getValue());
            int var20 = (int)Math.floor(var19.x2());
            int var21 = (int)Math.floor(var19.z2());
            if (!var42.contains(a(var19.x2(), var19.z2()))) {
               boolean var22 = false;

               for (int var23 = 0; var23 < var12 && !var22; var23++) {
                   var22 = var13[var23] != var18.getKey()
                     && a(((RiverSegment)var1.get(var23)).x1(), ((RiverSegment)var1.get(var23)).z1()) == a(var19.x2(), var19.z2());
               }

               if (!var22) {
                  boolean var45 = false;

                  for (Lake var25 : var2) {
                     if (var25.wetWithin(var20, var21, 1)) {
                        var45 = true;
                        break;
                     }
                  }

                  if (!var45) {
                     double var46 = var5.groundHeightReal(var20, var21);
                     double var26 = var5.surfaceHeightReal(var20, var21);
                     if (!(var26 - var46 >= 0.99)) {
                        var43++;
                        int var28 = (int)Math.round((double)(var20 - var7.originX) / var7.spacing);
                        int var29 = (int)Math.round((double)(var21 - var7.originZ) / var7.spacing);
                        String var30 = "off-grid";
                        if (var7.valid(var28, var29)) {
                           int var31 = var7.idx(var28, var29);
                           int var32 = var8[var31];
                           var30 = "cell h="
                              + String.format(Locale.ROOT, "%.1f", var11[var31])
                              + " river="
                              + var10[var31]
                              + " riverOutlet="
                              + var7.riverOutlet(var31)
                              + " down="
                              + (
                                 var32 < 0
                                    ? "none"
                                    : (
                                       var9[var32] >= 0
                                          ? "lake"
                                          : (var7.riverOutlet(var32) ? "riverOutlet" : (var11[var32] <= var4 ? "sea" : (var10[var32] ? "river" : "land")))
                                    )
                              );
                        }

                        Constants.LOG
                           .info(
                              "[SR-DEADEND] plate {} chain #{} tail ({},{}) y={} w={} | game ground={} surface={} | seaBand={} seaTailIsWet={} samplerWet={} riverSurface={} biomeClass={} | {}",
                              new Object[]{
                                 var0,
                                 var18.getKey(),
                                 var20,
                                 var21,
                                 String.format(Locale.ROOT, "%.2f", var19.y2()),
                                 String.format(Locale.ROOT, "%.1f", var19.w2()),
                                 String.format(Locale.ROOT, "%.1f", var46),
                                 String.format(Locale.ROOT, "%.1f", var26),
                                 var19.y2() <= var4 + 1,
                                 a(var5, var19),
                                 a(var5, var19.x2(), var19.z2()),
                                 String.format(Locale.ROOT, "%.1f", a(var5, var20, var21, var6.gridSpacing())),
                                 var5.waterBiomeClassAt(var20, (int)Math.round(var19.y2()), var21),
                                 var30
                              }
                           );
                        double var47 = var19.x2() - var19.x1();
                        double var33 = var19.z2() - var19.z1();
                        double var35 = Math.hypot(var47, var33);
                        StringBuilder var37 = new StringBuilder();

                        for (double var38 = 0.0; var35 > 1.0E-6 && var38 <= 32.000000001; var38 += 8.0) {
                           int var40 = (int)Math.floor(var19.x2() - var47 / var35 * var38);
                           int var41 = (int)Math.floor(var19.z2() - var33 / var35 * var38);
                           var37.append(
                              String.format(
                                 Locale.ROOT,
                                 " [%.0f back (%d,%d) samp surf=%.1f ground=%.1f -> %s | game surf=%.1f ground=%.1f | cls=%d]",
                                 var38,
                                 var40,
                                 var41,
                                 var5.surfaceHeight(var40, var41),
                                 var5.groundHeightExact(var40, var41),
                                 a(var5, var40, var41) ? "WET" : "dry",
                                 var5.surfaceHeightReal(var40, var41),
                                 var5.groundHeightReal(var40, var41),
                                 var5.waterBiomeClass(var40, var41)
                              )
                           );
                        }

                        Constants.LOG.info("[SR-DEADEND]   look-back:{}", var37);
                     }
                  }
               }
            }
         }

         Constants.LOG.info("[SR-DEADEND] plate {}: {} chain(s) end dry of {} ({} segments)", new Object[]{var0, var43, var14.size(), var12});
      }
   }

   private static void a(LowResGrid var0, TerrainSampler var1, PlateDomain var2) {
      int var3 = var0.size();
      if (var3 != 0) {
         int var4 = Math.max(1, var3 / 48);
         double var5 = 0.0;
         int var7 = 0;
         int var8 = 0;
         double var9 = 0.0;
         double var11 = 0.0;
         int var13 = 0;
         int var14 = 0;

         for (int var15 = var4 / 2; var15 < var3; var15 += var4) {
            if (var0.inDomain(var15)) {
               double var16 = var0.height(var15);
               if (var16 != -1000.0 && var16 != -1001.0) {
                  int var18 = Math.round(var0.worldX(var0.gi(var15)));
                  int var19 = Math.round(var0.worldZ(var0.gj(var15)));
                  double var20 = var1.groundHeightReal(var18, var19);
                  if (!(var20 <= -999.0)) {
                     var14++;
                     double var22 = Math.abs(var16 - var20);
                     if (var22 >= 8.0) {
                        var13++;
                     }

                     if (var22 > var5) {
                        var5 = var22;
                        var7 = var18;
                        var8 = var19;
                        var9 = var16;
                        var11 = var20;
                     }
                  }
               }
            }
         }

         if (var5 >= 8.0) {
            Constants.LOG
               .warn(
                  "The stream heights for plate ({},{}) disagree with the game's own terrain: {} of {} checked columns are off by {} blocks or more, the worst at ({},{}) where the streams see {} and the game {}. Streams there may be routed wrongly.",
                  new Object[]{
                     var2.key().cx(),
                     var2.key().cz(),
                     var13,
                     var14,
                     8,
                     var7,
                     var8,
                     String.format(Locale.ROOT, "%.1f", var9),
                     String.format(Locale.ROOT, "%.1f", var11)
                  }
               );
         } else {
            Constants.LOG
               .debug(
                  "[SR-FILL-AUDIT] plate ({},{}): {} columns read back, worst {} at ({},{})",
                  new Object[]{var2.key().cx(), var2.key().cz(), var14, String.format(Locale.ROOT, "%.1f", var5), var7, var8}
               );
         }
      }
   }

   private static boolean a(
      List<NetworkTracer.Pt> var0,
      TerrainSampler var1,
      StreamsConfig var2,
      long var3,
      int var5,
      boolean var6,
      WetnessSampler var7,
      List<RiverSegment> var8,
      Queue<Sink> var9
   ) {
      DeadEndResolver var10 = RiverEngine.deadEndResolver();
      if (var10 != null && var9 != null && var0.size() >= 2) {
         NetworkTracer.Pt var11 = (NetworkTracer.Pt)var0.get(var0.size() - 1);
         double var12 = 0.0;

         for (int var14 = 1; var14 < var0.size(); var14++) {
            var12 += Math.hypot(
               ((NetworkTracer.Pt)var0.get(var14)).x() - ((NetworkTracer.Pt)var0.get(var14 - 1)).x(),
               ((NetworkTracer.Pt)var0.get(var14)).z() - ((NetworkTracer.Pt)var0.get(var14 - 1)).z()
            );
         }

         Sink var21 = new Sink(var11.x(), var11.z(), var11.y(), b(var11.acc(), var2), var11.acc(), var12);
         bo.incrementAndGet(0);
         if (!var10.keep(var21, var1, var3)) {
            return false;
         }

         bo.incrementAndGet(1);
         ColumnCache var15 = new ColumnCache();
         boolean var16 = R || var2.legacyShaping();
         boolean var17 = !var16 && var2.terrainFit() > 0;
         List var18 = var17 ? var0 : a(a(var0, var2.gridSpacing() * 0.4), var3, var5, var2);
         if (var2.confineToBanks()) {
            var18 = a(var18, var1, var15, var2);
         }

         var18 = a(var18, var2.smoothingPasses());
         if (!var16) {
            var18 = c(var18, var2);
         }

         int var19 = var8.size();
         a(var18, var2, var8, var6, var7, var3, var5, false);
         if (var8.size() > var19) {
            RiverSegment var20 = (RiverSegment)var8.get(var8.size() - 1);
            var9.add(new Sink(var20.x2(), var20.z2(), var20.y2(), var20.w2(), var11.acc(), var12));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int[] a() {
      String var0 = System.getProperty("streamsreflowing.laketrace");
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim())};
         } catch (RuntimeException var2) {
            System.out.println("[SR-LAKETRACE] bad streamsreflowing.laketrace value: " + var0);
            return null;
         }
      } else {
         return null;
      }
   }

   private static int[] b() {
      String var0 = System.getProperty("streamsreflowing.segdump");
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            int var2 = var1.length >= 3 ? Integer.parseInt(var1[2].trim()) : 64;
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim()), var2};
         } catch (RuntimeException var3) {
            System.out.println("[SR-SEGDUMP] bad streamsreflowing.segdump value: " + var0);
            return null;
         }
      } else {
         return null;
      }
   }

   private static void a(String var0, List<NetworkTracer.Pt> var1) {
      if (af != null && var1 != null && !var1.isEmpty()) {
         boolean var2 = false;

         for (NetworkTracer.Pt var4 : var1) {
            if (b(var4.x(), var4.z())) {
               var2 = true;
               break;
            }
         }

         if (var2) {
            NetworkTracer.Pt var5 = (NetworkTracer.Pt)var1.get(0);
            NetworkTracer.Pt var6 = (NetworkTracer.Pt)var1.get(var1.size() - 1);
            Constants.LOG
               .info(
                  "[SR-SEGDUMP] chain {}: head=({},{}) y={} acc={} -> tail=({},{}) y={} | {} pts",
                  new Object[]{
                     var0,
                     (int)var5.x(),
                     (int)var5.z(),
                     String.format(Locale.ROOT, "%.1f", var5.y()),
                     String.format(Locale.ROOT, "%.1f", var5.acc()),
                     (int)var6.x(),
                     (int)var6.z(),
                     String.format(Locale.ROOT, "%.1f", var6.y()),
                     var1.size()
                  }
               );
         }
      }
   }

   private static void b(String var0, List<NetworkTracer.Pt> var1) {
      if (af != null && var1 != null && var1.size() >= 2) {
         NetworkTracer.Pt var2 = (NetworkTracer.Pt)var1.get(var1.size() - 1);
         if (b(var2.x(), var2.z())) {
            StringBuilder var3 = new StringBuilder();

            for (int var4 = Math.max(0, var1.size() - 4); var4 < var1.size(); var4++) {
               NetworkTracer.Pt var5 = (NetworkTracer.Pt)var1.get(var4);
               double var6 = var4 > 0
                  ? Math.hypot(var5.x() - ((NetworkTracer.Pt)var1.get(var4 - 1)).x(), var5.z() - ((NetworkTracer.Pt)var1.get(var4 - 1)).z())
                  : 0.0;
               var3.append(String.format(Locale.ROOT, " (%.3f,%.3f y%.2f d%.3f)", var5.x(), var5.z(), var5.y(), var6));
            }

            Constants.LOG.info("[SR-SEGDUMP] tail@{} n={}:{}", new Object[]{var0, var1.size(), var3});
         }
      }
   }

   private static void a(String var0, LowResGrid var1, int var2, double var3) {
      if (af != null) {
         int var5 = var1.worldX(var1.gi(var2));
         int var6 = var1.worldZ(var1.gj(var2));
         if (b(var5, var6)) {
            Constants.LOG.info("[SR-SEGDUMP] cell {}: ({},{}) filled={}", new Object[]{var0, var5, var6, String.format(Locale.ROOT, "%.1f", var3)});
         }
      }
   }

   private static boolean b(double var0, double var2) {
      return af != null && Math.abs(var0 - af[0]) <= af[2] && Math.abs(var2 - af[1]) <= af[2];
   }

   public static void dumpLoadedSegments(PlateKey var0, List<RiverSegment> var1, List<Lake> var2) {
      if (af != null) {
         int[] var3 = new int[var1.size()];
         Arrays.fill(var3, -1);
         a("plate " + var0 + " (LOADED from store)", var1, var3, var2);
      }
   }

   private static void a(PlateDomain var0, List<RiverSegment> var1, int[] var2, List<Lake> var3) {
      a("plate " + var0.key(), var1, var2, var3);
   }

   private static void a(String var0, List<RiverSegment> var1, int[] var2, List<Lake> var3) {
      int[] var4 = chainIds(var1);
      StringBuilder var5 = new StringBuilder();
      int var6 = 0;

      for (int var7 = 0; var7 < var1.size(); var7++) {
         RiverSegment var8 = (RiverSegment)var1.get(var7);
         double var9 = (var8.x1() + var8.x2()) * 0.5;
         double var11 = (var8.z1() + var8.z2()) * 0.5;
         if (b(var9, var11)) {
            var5.append(
               String.format(
                  Locale.ROOT,
                  "%n  seg %5d sys=%-10d chain=%-5d (%8.1f,%8.1f y=%7.2f w=%4.1f) -> (%8.1f,%8.1f y=%7.2f w=%4.1f)",
                  var7,
                  var2[var7],
                  var4[var7],
                  var8.x1(),
                  var8.z1(),
                  var8.y1(),
                  var8.w1(),
                  var8.x2(),
                  var8.z2(),
                  var8.y2(),
                  var8.w2()
               )
            );
            var6++;
         }
      }

      for (Lake var19 : var3) {
         int var20 = var19.cellMinX();
         int var10 = var19.cellMinZ();
         int var21 = var20 + var19.cellsWide() * var19.spacing();
         int var12 = var10 + var19.cellsHigh() * var19.spacing();
         double var13 = (var20 + var21) * 0.5;
         double var15 = (var10 + var12) * 0.5;
         boolean var17 = b(var13, var15) || af != null && af[0] >= var20 - af[2] && af[0] <= var21 + af[2] && af[1] >= var10 - af[2] && af[1] <= var12 + af[2];
         if (var17) {
            var5.append(
               String.format(
                  Locale.ROOT,
                  "%n  lake level=%d dig=%d bbox=[%d..%d, %d..%d] covers-target=%s wets-target=%s",
                  var19.level(),
                  var19.digDepth(),
                  var20,
                  var21,
                  var10,
                  var12,
                  var19.covers(af[0], af[1]),
                  var19.wetAt(af[0], af[1])
               )
            );
         }
      }

      if (var6 > 0 || var5.length() > 0) {
         Constants.LOG.info("[SR-SEGDUMP] {} segments/lakes within {} of ({},{}):{}", new Object[]{var0, af[2], af[0], af[1], var5});
      }
   }

   private static void a(
      int var0,
      int var1,
      List<Integer> var2,
      LowResGrid var3,
      double[] var4,
      int[] var5,
      double[] var6,
      boolean[] var7,
      int[] var8,
      int var9,
      PlateDomain var10
   ) {
      ArrayList<Integer> var11 = new ArrayList<>();

      for (int var13 : var2) {
         if (Math.abs(var3.worldX(var3.gi(var13)) - var0) <= 96 && Math.abs(var3.worldZ(var3.gj(var13)) - var1) <= 96) {
            var11.add(var13);
         }
      }

      if (!var11.isEmpty()) {
         var11.sort((var1x, var2x) -> {
            int var3x = var3.worldZ(var3.gj(var1x));
            int var4x = var3.worldZ(var3.gj(var2x));
            return var3x != var4x ? Integer.compare(var3x, var4x) : Integer.compare(var3.worldX(var3.gi(var1x)), var3.worldX(var3.gi(var2x)));
         });
         StringBuilder var30 = new StringBuilder();
         var30.append(
            String.format("%n[SR-LAKETRACE] ===== plate=%s target=(%d,%d) sea=%d window=%d cells=%d =====%n", var10.key(), var0, var1, var9, 96, var11.size())
         );
         var30.append(String.format("[SR-LAKETRACE]   worldX  worldZ  terrainH  filled  f-h   acc    lakeId  flags%n"));
         int var31 = 0;
         int var14 = 0;

         for (int var16 : var11) {
            int var17 = var3.worldX(var3.gi(var16));
            int var18 = var3.worldZ(var3.gj(var16));
            double var19 = var3.height(var16);
            double var21 = var4[var16];
            boolean var23 = var19 <= var9;
            boolean var24 = var21 - var19 > 0.001;
            boolean var25 = a(var16, var4, var3, var9);
            boolean var26 = var21 <= var9;
            StringBuilder var27 = new StringBuilder();
            if (var23) {
               var27.append("OUTLET ");
            }

            if (var24) {
               var27.append("RAISED ");
            }

            if (var25) {
               var27.append("BASIN ");
            }

            if (var26) {
               var27.append("SUBSEA ");
            }

            if (var7[var16]) {
               var27.append("RIVER ");
            }

            if (var8[var16] >= 0) {
               var27.append("LAKE ");
            }

            if (var8[var16] >= 0) {
               var31++;
            }

            if (var24 && var26 && !var25) {
               var14++;
            }

            int var28 = var5[var16];
            String var29 = var28 >= 0 ? "->(" + var3.worldX(var3.gi(var28)) + "," + var3.worldZ(var3.gj(var28)) + ")" : "sink";
            var30.append(
               String.format(
                  "[SR-LAKETRACE]  %7d %7d %9.1f %7.1f %5.1f %6.1f %6d   %s%s%n",
                  var17,
                  var18,
                  var19,
                  var21,
                  var21 - var19,
                  var6[var16],
                  var8[var16],
                  var27.toString().trim(),
                  " " + var29
               )
            );
         }

         var30.append(
            String.format(
               "[SR-LAKETRACE] summary: %d cells in window, %d are lake cells, %d are RAISED+SUBSEA but NOT basin-eligible (the perch source: a sub-sea fill that isBasinCell rejects because filled<=sea, so it never becomes a contained lake)%n",
               var11.size(),
               var31,
               var14
            )
         );
         var30.append(
            String.format(
               "[SR-LAKETRACE] legend: OUTLET=seeded as ocean (terrainH<=sea); RAISED=Priority-Flood lifted it above terrain; BASIN=isBasinCell true (lake-eligible); SUBSEA=filled<=sea; RIVER=carries a stream; LAKE=assigned to a lake%n"
            )
         );
         System.out.print(var30);
         System.out.flush();
      }
   }

   private static boolean a(int var0, double[] var1, LowResGrid var2, int var3) {
      return a(var0, var1, var2, var3, 0.5);
   }

   private static boolean a(int var0, double[] var1, LowResGrid var2, int var3, double var4) {
      return var2.inDomain(var0) && !var2.riverOutlet(var0) && var1[var0] > var3 && var1[var0] - var2.height(var0) > var4;
   }

   private static boolean a(int var0, int[] var1, double[] var2, int var3, double var4, double var6, int var8) {
      int var9 = var0;

      for (int var10 = 0; var9 >= 0 && var10 < var8; var10++) {
         if (var2[var9] <= var3) {
            return true;
         }

         if (var4 - var2[var9] >= var6) {
            return true;
         }

         var9 = var1[var9];
      }

      return false;
   }

   private static boolean a(List<Integer> var0, boolean[] var1, double[] var2, LowResGrid var3) {
      for (int var5 : var0) {
         if (var1[var5] && var2[var5] - var3.height(var5) > 6.0) {
            return true;
         }
      }

      return false;
   }

   private static NetworkTracer.LocalBasin a(LowResGrid var0, TerrainSampler var1, List<Integer> var2, int var3) {
      int var4 = Integer.MAX_VALUE;
      int var5 = Integer.MIN_VALUE;
      int var6 = Integer.MAX_VALUE;
      int var7 = Integer.MIN_VALUE;

      for (int var9 : var2) {
         var4 = Math.min(var4, var0.gi(var9));
         var5 = Math.max(var5, var0.gi(var9));
         var6 = Math.min(var6, var0.gj(var9));
         var7 = Math.max(var7, var0.gj(var9));
      }

      var4 -= 8;
      var5 += 8;
      var6 -= 8;
      var7 += 8;
      if (var5 - var4 + 1 > 32) {
         int var38 = var4 + var5 >> 1;
         var4 = var38 - 16;
         var5 = var38 + 15;
      }

      if (var7 - var6 + 1 > 32) {
         int var39 = var6 + var7 >> 1;
         var6 = var39 - 16;
         var7 = var39 + 15;
      }

      int var40 = var5 - var4 + 1;
      int var41 = var7 - var6 + 1;
      double[] var10 = new double[var40 * var41];
      boolean[] var11 = new boolean[var40 * var41];

      for (int var12 = 0; var12 < var41; var12++) {
         for (int var13 = 0; var13 < var40; var13++) {
            var11[var12 * var40 + var13] = var0.valid(var4 + var13, var6 + var12);
         }
      }

      int var42 = var4;
      int var43 = var6;
      int var14 = var40;
      LowResGrid.parallelFor(var40 * var41, var7x -> {
         if (var11[var7x]) {
            var10[var7x] = var1.groundHeightExact(var0.worldX(var42 + var7x % var14), var0.worldZ(var43 + var7x / var14));
         }
      });
      double[] var15 = new double[var40 * var41];
      Arrays.fill(var15, Double.MAX_VALUE);
      PriorityQueue<double[]> var16 = new PriorityQueue<>((var0x, var1x) -> Double.compare(var0x[0], var1x[0]));

      for (int var17 = 0; var17 < var41; var17++) {
         for (int var18 = 0; var18 < var40; var18++) {
            int var19 = var17 * var40 + var18;
            if (var11[var19]) {
               boolean var20 = var18 == 0 || var17 == 0 || var18 == var40 - 1 || var17 == var41 - 1;
               if (!var20) {
                  for (int var21 = 0; var21 < 8 && !var20; var21++) {
                     int var22 = var18 + b[var21];
                     int var23 = var17 + c[var21];
                     var20 = !var11[var23 * var40 + var22];
                  }
               }

               if (var20) {
                  var15[var19] = var10[var19];
                  var16.add(new double[]{var10[var19], var19});
               }
            }
         }
      }

      while (!var16.isEmpty()) {
         double[] var44 = (double[])var16.poll();
         int var46 = (int)var44[1];
         if (!(var44[0] > var15[var46] + 1.0E-9)) {
            int var48 = var46 % var40;
            int var50 = var46 / var40;

            for (int var52 = 0; var52 < 8; var52++) {
               int var53 = var48 + b[var52];
               int var55 = var50 + c[var52];
               if (var53 >= 0 && var53 < var40 && var55 >= 0 && var55 < var41 && var11[var55 * var40 + var53]) {
                  int var24 = var55 * var40 + var53;
                  double var25 = Math.max(var10[var24], var15[var46]);
                  if (var25 < var15[var24]) {
                     var15[var24] = var25;
                     var16.add(new double[]{var25, var24});
                  }
               }
            }
         }
      }

      int var45 = var0.gi(var3) - var4;
      int var47 = var0.gj(var3) - var6;
      if (var45 >= 0 && var45 < var40 && var47 >= 0 && var47 < var41 && var11[var47 * var40 + var45]) {
         int var49 = var47 * var40 + var45;
         if (var15[var49] - var10[var49] <= 0.001) {
            return null;
         }

         double var51 = var15[var49];
         double var54 = var10[var49];
         int var56 = var49;
         boolean[] var57 = new boolean[var40 * var41];
         ArrayDeque var26 = new ArrayDeque();
         var57[var49] = true;
         var26.add(var49);

         while (!var26.isEmpty()) {
            int var27 = (Integer)var26.poll();
            if (var10[var27] < var54) {
               var54 = var10[var27];
               var56 = var27;
            }

            int var28 = var27 % var40;
            int var29 = var27 / var40;

            for (int var30 = 0; var30 < 8; var30++) {
               int var31 = var28 + b[var30];
               int var32 = var29 + c[var30];
               if (var31 >= 0 && var31 < var40 && var32 >= 0 && var32 < var41) {
                  int var33 = var32 * var40 + var31;
                  if (var11[var33] && !var57[var33] && !(var10[var33] >= var51 - 1.0E-6)) {
                     var57[var33] = true;
                     var26.add(var33);
                  }
               }
            }
         }

         return new NetworkTracer.LocalBasin(var4, var6, var40, var41, var10, var11, var56, var51, var54);
      } else {
         return null;
      }
   }

   private static double a(List<Integer> var0, LowResGrid var1, TerrainSampler var2) {
      double var3 = 0.0;
      double var5 = 0.0;
      int var7 = 0;

      for (int var9 : var0) {
         int var10 = var1.gi(var9);
         int var11 = var1.gj(var9);

         for (int var12 = 0; var12 < 8; var12++) {
            int var13 = var10 + b[var12];
            int var14 = var11 + c[var12];
            if (!var1.valid(var13, var14) || !var1.inDomain(var1.idx(var13, var14))) {
               var3 += (var1.worldX(var10) + var1.worldX(var13)) * 0.5;
               var5 += (var1.worldZ(var11) + var1.worldZ(var14)) * 0.5;
               var7++;
            }
         }
      }

      if (var7 == 0) {
         return Double.NaN;
      }

      double var20 = var3 / var7;
      double var21 = var5 / var7;
      double var22 = var1.spacing;
      double var23 = Double.MAX_VALUE;

      for (double var16 = -var22; var16 <= var22 + 1.0E-9; var16 += var22 / 2.0) {
         for (double var18 = -var22; var18 <= var22 + 1.0E-9; var18 += var22 / 2.0) {
            var23 = Math.min(var23, b(var2, var20 + var18, var21 + var16));
         }
      }

      return var23;
   }

   private static boolean a(LowResGrid var0, boolean[] var1, int var2, int var3, int var4) {
      if (var2 == var3) {
         return true;
      }

      int var5 = var0.gi(var2);
      int var6 = var0.gj(var2);

      for (int var7 = -2; var7 <= 2; var7++) {
         for (int var8 = -2; var8 <= 2; var8++) {
            if ((var8 != 0 || var7 != 0) && (!var0.valid(var5 + var8, var6 + var7) || !var1[var0.idx(var5 + var8, var6 + var7)])) {
               return false;
            }
         }
      }

      return true;
   }

   private static void a(TerrainSampler var0, int[] var1, int[] var2, int var3, int var4) {
      if (var3 > 0) {
         AtomicLongArray var5 = bp.get();
         var5.addAndGet(0, var3);
         var5.addAndGet(5 + var4, var3);
         long var6 = System.nanoTime();
         if (var3 < 16) {
            var5.incrementAndGet(1);
            var5.incrementAndGet(4);
            long var8 = System.nanoTime();
            var0.groundHeightsBatch(var1, var2, new double[var3], var3);
            var5.addAndGet(3, System.nanoTime() - var8);
            var5.addAndGet(2, System.nanoTime() - var6);
         } else {
            LowResGrid.parallelRange(var3, (var4x, var5x) -> {
               int var6x = var5x - var4x;
               var5.incrementAndGet(1);
               long var7 = System.nanoTime();
               var0.groundHeightsBatch(Arrays.copyOfRange(var1, var4x, var5x), Arrays.copyOfRange(var2, var4x, var5x), new double[var6x], var6x);
               var5.addAndGet(3, System.nanoTime() - var7);
            });
            var5.addAndGet(2, System.nanoTime() - var6);
         }
      }
   }

   private static Lake a(List<Integer> var0, int var1, int var2, TerrainSampler var3, LowResGrid var4, PlateDomain var5, double var6, double[] var8) {
      int var9 = 3 * var4.spacing;
      int var10 = var4.spacing >> 1;
      int var11 = Integer.MAX_VALUE;
      int var12 = Integer.MAX_VALUE;
      int var13 = Integer.MIN_VALUE;
      int var14 = Integer.MIN_VALUE;
      boolean[] var15 = new boolean[var4.size()];

      for (int var17 : var0) {
         var15[var17] = true;
      }

      for (int var52 : var0) {
         int var18 = var4.worldX(var4.gi(var52));
         int var19 = var4.worldZ(var4.gj(var52));
         var11 = Math.min(var11, var18);
         var13 = Math.max(var13, var18);
         var12 = Math.min(var12, var19);
         var14 = Math.max(var14, var19);
      }

      int var51 = var11 - var10 - var9;
      int var53 = var12 - var10 - var9;
      int var54 = ReadLattice.cell();
      if (var54 > 1 && var2 % var54 == 0) {
         var51 = Math.floorDiv(var51, var54) * var54;
         var53 = Math.floorDiv(var53, var54) * var54;
      }

      int var55 = (var13 + var10 + var9 - var51) / var2 + 1;
      int var20 = (var14 + var10 + var9 - var53) / var2 + 1;
      long[] var21 = new long[var55 * var20 + 63 >>> 6];
      boolean[] var22 = new boolean[var55 * var20];
      ArrayDeque<Integer> var23 = new ArrayDeque<>();
      ArrayList<Integer> var24 = new ArrayList<>(var0);
      var24.sort((var1x, var2x) -> Double.compare(var4.height(var1x), var4.height(var2x)));
      int var25 = -1;

      for (int var26 = 0; var26 < Math.min(4, var24.size()); var26++) {
         int var27 = (Integer)var24.get(var26);
         if (var3.groundHeight(var4.worldX(var4.gi(var27)), var4.worldZ(var4.gj(var27))) < var1) {
            var25 = var27;
            break;
         }
      }

      if (var25 < 0) {
         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-FINEFLOOD] floor-dry level={} fill={}", var1, var0.size());
         }

         return null;
      } else {
         ArrayList<Integer> var56 = new ArrayList<>();

         for (int var28 : var0) {
            if (a(var4, var15, var28, var25, var1)) {
               var56.add(var28);
            }
         }

         long var58 = System.nanoTime();
         int var29 = var56.size();
         int[] var30 = new int[var29];
         int[] var31 = new int[var29];

         for (int var32 = 0; var32 < var29; var32++) {
            int var33 = (Integer)var56.get(var32);
            var30[var32] = var4.worldX(var4.gi(var33));
            var31[var32] = var4.worldZ(var4.gj(var33));
         }

         a(var3, var30, var31, var29, 0);
         au.get()[7] += System.nanoTime() - var58;
         int var59 = 0;

         for (int var62 : var56) {
            int var65 = var4.worldX(var4.gi(var62));
            int var68 = var4.worldZ(var4.gj(var62));
            double var71 = var3.groundHeight(var65, var68);
            if (var71 >= var1) {
               var59++;
            } else {
               if (var8 != null) {
                  var8[0] += var1 - var71;
                  var8[1]++;
               }

               int var34 = (var65 - var10 - var51) / var2;
               int var35 = (var65 + var10 - var51) / var2;
               int var36 = (var68 - var10 - var53) / var2;
               int var37 = (var68 + var10 - var53) / var2;

               for (int var38 = var36; var38 <= var37; var38++) {
                  for (int var39 = var34; var39 <= var35; var39++) {
                     if (var39 >= 0 && var39 < var55 && var38 >= 0 && var38 < var20) {
                        int var40 = var38 * var55 + var39;
                        if (!var22[var40]) {
                           var22[var40] = true;
                           bp.get().incrementAndGet(8);
                           var21[var40 >>> 6] = var21[var40 >>> 6] | 1L << (var40 & 63);
                           var23.add(var40);
                        }
                     }
                  }
               }
            }
         }

         if (var59 * 2 > var56.size()) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
               Constants.LOG.info("[SR-FINEFLOOD] interior-dry {}/{} level={} fill={}", new Object[]{var59, var56.size(), var1, var0.size()});
            }

            return null;
         } else {
            boolean[] var61 = new boolean[var55 * var20];
            if (Integer.getInteger("streamsreflowing.lakeringreach", -1) >= 0) {
               var29 = var10 / var2 + Integer.getInteger("streamsreflowing.lakeringreach", -1);
               boolean[] var66 = var61;
             ArrayDeque<int[]> var69 = new ArrayDeque<>();

               for (int var75 : var0) {
                  if (!a(var4, var15, var75, var25, var1)) {
                     int var79 = (var4.worldX(var4.gi(var75)) - var51) / var2;
                     int var85 = (var4.worldZ(var4.gj(var75)) - var53) / var2;
                     if (var79 >= 0 && var79 < var55 && var85 >= 0 && var85 < var20 && !var66[var85 * var55 + var79]) {
                        var66[var85 * var55 + var79] = true;
                        var69.add(new int[]{var79, var85, 0});
                     }
                  }
               }

               ArrayList<int[]> var73 = new ArrayList<>();

               while (!var69.isEmpty()) {
                  int[] var76 = (int[])var69.poll();
                  var73.add(var76);
                  if (var76[2] < var29) {
                     for (int var80 = -1; var80 <= 1; var80++) {
                        for (int var86 = -1; var86 <= 1; var86++) {
                           int var90 = var76[0] + var86;
                           int var95 = var76[1] + var80;
                           if ((var86 != 0 || var80 != 0) && var90 >= 0 && var90 < var55 && var95 >= 0 && var95 < var20 && !var66[var95 * var55 + var90]) {
                              var66[var95 * var55 + var90] = true;
                              var69.add(new int[]{var90, var95, var76[2] + 1});
                           }
                        }
                     }
                  }
               }

               long var77 = System.nanoTime();
               int var87 = var73.size();
               int[] var91 = new int[var87];
               int[] var96 = new int[var87];

               for (int var100 = 0; var100 < var87; var100++) {
                  int[] var104 = (int[])var73.get(var100);
                  var91[var100] = var51 + var104[0] * var2;
                  var96[var100] = var53 + var104[1] * var2;
               }

               a(var3, var91, var96, var87, 1);
               au.get()[7] += System.nanoTime() - var77;
            }

            var29 = Math.max(4096, 4 * var0.size() * (var4.spacing / var2 + 1) * (var4.spacing / var2 + 1));
            int var67 = 0;
            ArrayList<Integer> var70 = new ArrayList<>();

            while (!var23.isEmpty()) {
               var70.add((Integer)var23.poll());
            }

            ArrayList<Integer> var74 = new ArrayList<>();

            while (!var70.isEmpty()) {
               BuildGate.yieldPoint();
               ArrayList<int[]> var78 = new ArrayList<>();

               for (int var88 : var70) {
                  int var92 = var88 % var55;
                  int var97 = var88 / var55;

                  for (int var101 = 0; var101 < 4; var101++) {
                     int var105 = var92 + (var101 == 0 ? -1 : (var101 == 1 ? 1 : 0));
                     int var108 = var97 + (var101 == 2 ? -1 : (var101 == 3 ? 1 : 0));
                     if (var105 >= 0 && var105 < var55 && var108 >= 0 && var108 < var20 && !var22[var108 * var55 + var105]) {
                        int var41 = var51 + var105 * var2;
                        int var42 = var53 + var108 * var2;
                        if (!var61[var108 * var55 + var105]) {
                           var61[var108 * var55 + var105] = true;
                           var78.add(new int[]{var41, var42});
                        }

                        int var43 = var51 + var92 * var2;
                        int var44 = var53 + var97 * var2;
                        int var45 = Integer.signum(var41 - var43);
                        int var46 = Integer.signum(var42 - var44);

                        for (int var47 = 1; var47 < var2; var47++) {
                           var78.add(new int[]{var43 + var45 * var47, var44 + var46 * var47});
                        }
                     }
                  }
               }

               if (!var78.isEmpty()) {
                  long var82 = System.nanoTime();
                  int var93 = var78.size();
                  int[] var98 = new int[var93];
                  int[] var102 = new int[var93];

                  for (int var106 = 0; var106 < var93; var106++) {
                     int[] var109 = (int[])var78.get(var106);
                     var98[var106] = var109[0];
                     var102[var106] = var109[1];
                  }

                  a(var3, var98, var102, var93, 2);
                  au.get()[7] += System.nanoTime() - var82;
               }

               for (int var83 = 0; var83 < var70.size(); var83++) {
                  if (++var67 > var29) {
                     if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                        Constants.LOG.info("[SR-FINEFLOOD] budget level={} fill={} budget={}", new Object[]{var1, var0.size(), var29});
                     }

                     return null;
                  }

                  int var89 = (Integer)var70.get(var83);
                  int var94 = var89 % var55;
                  int var99 = var89 / var55;

                  for (int var103 = 0; var103 < 4; var103++) {
                     int var107 = var94 + (var103 == 0 ? -1 : (var103 == 1 ? 1 : 0));
                     int var110 = var99 + (var103 == 2 ? -1 : (var103 == 3 ? 1 : 0));
                     if (var107 >= 0 && var107 < var55 && var110 >= 0 && var110 < var20) {
                        int var111 = var110 * var55 + var107;
                        if (!var22[var111]) {
                           var22[var111] = true;
                           bp.get().incrementAndGet(8);
                           int var112 = var51 + var107 * var2;
                           int var113 = var53 + var110 * var2;
                           if (var3.groundHeight(var112, var113) < var1) {
                              int var114 = var51 + var94 * var2;
                              int var115 = var53 + var99 * var2;
                              int var116 = Integer.signum(var112 - var114);
                              int var117 = Integer.signum(var113 - var115);
                              boolean var48 = false;

                              for (int var49 = 1; var49 < var2 && !var48; var49++) {
                                 var48 = var3.groundHeight(var114 + var116 * var49, var115 + var117 * var49) >= var1;
                              }

                              if (var48) {
                                 var22[var111] = false;
                              } else {
                                 if (var5.contains(var112, var113) && !(var5.borderDistance(var112, var113) < var6)) {
                                    if (var107 != 0 && var107 != var55 - 1 && var110 != 0 && var110 != var20 - 1) {
                                       if (var8 != null) {
                                          var8[0] += var1 - var3.groundHeight(var112, var113);
                                          var8[1]++;
                                       }

                                       var21[var111 >>> 6] = var21[var111 >>> 6] | 1L << (var111 & 63);
                                       var74.add(var111);
                                       continue;
                                    }

                                    if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                                       Constants.LOG.info("[SR-FINEFLOOD] bound at {},{} level={} fill={}", new Object[]{var112, var113, var1, var0.size()});
                                    }

                                    return null;
                                 }

                                 if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                                    Constants.LOG.info("[SR-FINEFLOOD] border at {},{} level={} fill={}", new Object[]{var112, var113, var1, var0.size()});
                                 }

                                 return null;
                              }
                           }
                        }
                     }
                  }
               }

               ArrayList var84 = var70;
               var70 = var74;
               var74 = var84;
               var74.clear();
            }

            return new Lake(var1, 0, var2, var51, var53, var55, var20, var21, Lake.DEFAULT_ROUNDING);
         }
      }
   }

   private static boolean a(Lake var0, double var1) {
      int var3 = var0.cellsWide();
      int var4 = var0.cellsHigh();
      long[] var5 = var0.mask();
      int var6 = 0;

      for (long var10 : var5) {
         var6 += Long.bitCount(var10);
      }

      if (var6 == 0) {
         return false;
      }

      boolean[] var21 = new boolean[var3 * var4];
      int var22 = 0;
      ArrayDeque var23 = new ArrayDeque();

      for (int var24 = 0; var24 < var3 * var4; var24++) {
         if (!var21[var24] && (var5[var24 >>> 6] & 1L << (var24 & 63)) != 0L) {
            int var11 = 0;
            var21[var24] = true;
            var23.add(var24);

            while (!var23.isEmpty()) {
               int var12 = (Integer)var23.poll();
               var11++;
               int var13 = var12 % var3;
               int var14 = var12 / var3;
               int[][] var15 = new int[][]{{var13 - 1, var14}, {var13 + 1, var14}, {var13, var14 - 1}, {var13, var14 + 1}};

               for (int[] var19 : var15) {
                  if (var19[0] >= 0 && var19[0] < var3 && var19[1] >= 0 && var19[1] < var4) {
                     int var20 = var19[1] * var3 + var19[0];
                     if (!var21[var20] && (var5[var20 >>> 6] & 1L << (var20 & 63)) != 0L) {
                        var21[var20] = true;
                        var23.add(var20);
                     }
                  }
               }
            }

            var22 = Math.max(var22, var11);
            if (var22 >= var6 * var1) {
               return true;
            }
         }
      }

      return var22 >= var6 * var1;
   }

   private static Lake a(List<Integer> var0, int var1, int var2, int var3, LowResGrid var4) {
      int var5 = Integer.MAX_VALUE;
      int var6 = Integer.MAX_VALUE;
      int var7 = Integer.MIN_VALUE;
      int var8 = Integer.MIN_VALUE;

      for (int var10 : var0) {
         var5 = Math.min(var5, var4.gi(var10));
         var7 = Math.max(var7, var4.gi(var10));
         var6 = Math.min(var6, var4.gj(var10));
         var8 = Math.max(var8, var4.gj(var10));
      }

      int var26 = var3;
      var5 -= var26;
      var6 -= var26;
      var7 += var26;
      var8 += var26;
      int var27 = var7 - var5 + 1;
      int var11 = var8 - var6 + 1;
      long[] var12 = new long[var27 * var11 + 63 >>> 6];

      for (int var14 : var0) {
         int var15 = var4.gi(var14) - var5;
         int var16 = var4.gj(var14) - var6;

         for (int var17 = -var26; var17 <= var26; var17++) {
            for (int var18 = -var26; var18 <= var26; var18++) {
               int var19 = var15 + var18;
               int var20 = var16 + var17;
               if (var19 >= 0 && var19 < var27 && var20 >= 0 && var20 < var11) {
                  int var21 = var20 * var27 + var19;
                  var12[var21 >>> 6] = var12[var21 >>> 6] | 1L << (var21 & 63);
               }
            }
         }
      }

      return new Lake(var1, var2, var4.spacing, var4.worldX(var5), var4.worldZ(var6), var27, var11, var12);
   }

   private static double a(LowResGrid var0, int var1, int var2) {
      double var3 = var0.gi(var1) - var0.gi(var2);
      double var5 = var0.gj(var1) - var0.gj(var2);
      return Math.hypot(var3, var5) * var0.spacing;
   }

   private static void a(
      NetworkTracer.PendingOutflow var0,
      LowResGrid var1,
      int[] var2,
      int[] var3,
      List<Lake> var4,
      double[] var5,
      int var6,
      TerrainSampler var7,
      WetnessSampler var8,
      StreamsConfig var9,
      long var10,
      List<RiverSegment> var12,
      Queue<Sink> var13
   ) {
      ArrayList var14 = new ArrayList();
      double var15 = var0.startLevel() + RiverEngine.insetFor(b(var0.acc(), var9), var9);
      var14.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var0.outlet())), var1.worldZ(var1.gj(var0.outlet())), var15, var0.acc()));
      int var17 = var0.outlet();
      boolean var18 = false;
      double var19 = Double.NaN;
      int var21 = -1;
      Lake var22 = null;
      int var23 = var1.size();

      while (var23-- > 0) {
         int var24 = var2[var17];
         if (var24 < 0) {
            var18 = true;
            break;
         }

         if (var3[var24] >= 0) {
            Lake var34 = (Lake)var4.get(var3[var24]);
            if (var34.level() >= var0.startLevel() - 1.0) {
               a("DROP outflow-curled-back-into-lake", var14);
               return;
            }

            var14.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var24)), var1.worldZ(var1.gj(var24)), var34.level(), var0.acc()));
            var19 = var34.level();
            var22 = var34;
            break;
         }

         double var25 = var5[var24];
         double var27 = a(var4, var1.worldX(var1.gi(var24)), var1.worldZ(var1.gj(var24)), var0.acc(), var9);
         if (!Double.isNaN(var27)) {
            var25 = Math.max(var25, var27);
         }

         double var29 = Math.min(((NetworkTracer.Pt)var14.get(var14.size() - 1)).y(), Math.max(var6, var25));
         var14.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var24)), var1.worldZ(var1.gj(var24)), var29, var0.acc()));
         if (var5[var24] <= var6) {
            var19 = var6;
            var21 = var24;
            break;
         }

         var17 = var24;
      }

      int var33 = var0.outlet() * 131 + 9;
      ColumnCache var35 = new ColumnCache();
      boolean var36 = R || var9.legacyShaping();
      boolean var28 = var36 && var9.valleySnapRadius() > 0;
      boolean var37 = !var36 && var9.terrainFit() > 0;
      List var26;
      if (var37) {
         int var30 = HeightCensus.enter(16);
         var26 = a(a(a(a(var14, var9.gridSpacing() * 0.4), var10, var33, var9), 1), var7, var35, var9, false, var4);
         HeightCensus.restore(var30);
      } else {
         var26 = var28 ? c(var14, var7, var35, var9, false, var4) : var14;
      }

      if (var22 != null && var26.size() >= 2) {
         int var38 = var26.size();
         var26 = a(var26, var22, var7, var35, var9);
         if (var26.size() < var38 || var26.size() < 2) {
            a("DROP outflow-lake-approach-truncated", var26);
            return;
         }
      }

      List var39 = var26;
      if (var26.size() >= 2) {
         if (var21 >= 0) {
            List var31 = a(var26, var1, var21, var6, var7, var9);
            if (var31 == var26) {
               boolean var32 = a(var26, var7, var9, var10, var33, false, var8, var12, var13);
               a((var32 ? "SINK" : "DROP") + " outflow-sea-walk-found-no-water", var26);
               return;
            }

            var26 = var31;
         } else if (!Double.isNaN(var19)) {
            var26 = b(var26, var19, 8.0);
         } else if (var18) {
            List var40 = a(var26, var1, var17, var6, var7, var9);
            if (var40 == var26) {
               boolean var43 = a(var26, var7, var9, var10, var33, false, var8, var12, var13);
               a((var43 ? "SINK" : "DROP") + " outflow-dead-end-no-water", var26);
               return;
            }

            var26 = var40;
         }

         List var41 = var37 ? a(var26, var39, var10, var33, var9) : a(a(var26, var9.gridSpacing() * 0.4), var10, var33, var9);
         if (var9.confineToBanks()) {
            var41 = a(var41, var7, var35, var9);
         }

         var41 = a(var41, var9.smoothingPasses());
         if (!var36) {
            var41 = c(var41, var9);
         }

         a(var41, var9, var12, false, var8, var10, var33, true);
      }
   }

   private static int a(LowResGrid var0, int var1, double[] var2, boolean[] var3, int var4, int var5) {
      int var6 = var0.gi(var1);
      int var7 = var0.gj(var1);
      int var8 = -1;
      int var9 = Integer.MAX_VALUE;

      for (int var10 = -var5; var10 <= var5; var10++) {
         for (int var11 = -var5; var11 <= var5; var11++) {
            if (var11 != 0 || var10 != 0) {
               int var12 = var6 + var11;
               int var13 = var7 + var10;
               if (var0.valid(var12, var13)) {
                  int var14 = var0.idx(var12, var13);
                  if (var0.inDomain(var14)) {
                     boolean var15 = var3[var14] && var2[var14] < var2[var1];
                     boolean var16 = var2[var14] <= var4;
                     if (var15 || var16) {
                        int var17 = var11 * var11 + var10 * var10;
                        if (var17 < var9) {
                           var9 = var17;
                           var8 = var14;
                        }
                     }
                  }
               }
            }
         }
      }

      return var8;
   }

   private static boolean a(int var0, LowResGrid var1, WetnessSampler var2, ColumnCache var3, double var4, double var6, double var8, long var10) {
      int var12 = var1.worldX(var1.gi(var0));
      int var13 = var1.worldZ(var1.gj(var0));
      long var14 = (long)Math.floorDiv(var12, 64) << 32 ^ Math.floorDiv(var13, 64) & 4294967295L;
      double var16 = var3.get(var14, Double.NaN);
      double var18;
      if (!Double.isNaN(var16)) {
         var18 = var16;
      } else {
         var18 = var2.wetness(var12, (int)var1.height(var0), var13);
         var3.put(var14, var18);
      }

      double var20 = Math.max(var8, Math.min(1.0, (var18 - var4) / (var6 - var4)));
      return Hashing.unit(var10, var0, 71L) >= var20;
   }

   private static boolean a(LowResGrid var0, int var1) {
      int var2 = var0.gi(var1);
      int var3 = var0.gj(var1);

      for (int var4 = 0; var4 < 8; var4++) {
         int var5 = var2 + b[var4];
         int var6 = var3 + c[var4];
         if (!var0.valid(var5, var6) || !var0.inDomain(var0.idx(var5, var6))) {
            return true;
         }
      }

      return false;
   }

   private static List<NetworkTracer.Pt> a(
      List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3, boolean var4, List<Lake> var5
   ) {
      ReadLattice.enterStreamChunks();

      try {
         return b(var0, var1, var2, var3, var4, var5);
      } finally {
         ReadLattice.exitStreamChunks();
      }
   }

   private static List<NetworkTracer.Pt> b(
      List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3, boolean var4, List<Lake> var5
   ) {
      if (var0.size() >= 2 && var3.terrainFit() > 0) {
         double var6 = Math.max(1.0, (double)var3.gridSpacing() / var3.terrainFit());
         int var8 = ReadLattice.cell();
         boolean var9 = var8 > 1;
         if (var9) {
            var6 = Math.max(var8, (double)Math.round(var6 / var8) * var8);
         }

         List var10 = a(var0, var6);
         int var11 = var10.size();
         double var12 = var3.seaLevel() - 1;
         int var14 = var3.valleySnapRadius();
         boolean var15 = var1.hasEstimate();
         double[] var16 = new double[var11];
         double[] var17 = new double[var11];

         for (int var18 = 0; var18 < var11; var18++) {
            NetworkTracer.Pt var19 = (NetworkTracer.Pt)var10.get(var18);
            boolean var20 = var18 > 0 && var18 < var11 - 1;
            var16[var18] = var9 && var20 ? ReadLattice.snap(var19.x(), var8) : var19.x();
            var17[var18] = var9 && var20 ? ReadLattice.snap(var19.z(), var8) : var19.z();
         }

         double[] var88 = new double[var11];
         double[] var89 = new double[var11];
         double[] var90 = new double[var11];

         for (int var21 = 0; var21 < var11; var21++) {
            NetworkTracer.Pt var22 = (NetworkTracer.Pt)var10.get(var21);
            var88[var21] = a(var1, var2, var16[var21], var17[var21]);
            var89[var21] = a(var5, var16[var21], var17[var21]);
            var90[var21] = var21 == 0
               ? 0.0
               : var90[var21 - 1]
                  + Math.hypot(var22.x() - ((NetworkTracer.Pt)var10.get(var21 - 1)).x(), var22.z() - ((NetworkTracer.Pt)var10.get(var21 - 1)).z());
         }

         int var91 = -1;
         int var92 = -1;

         for (int var23 = 0; var23 < var11; var23++) {
            if (!Double.isNaN(var89[var23])) {
               if (var91 < 0) {
                  var91 = var23;
               }

               if (var92 >= 0 && var89[var92] == var89[var23] && var90[var23] - var90[var92] <= 24.0) {
                  for (int var24 = var92 + 1; var24 < var23; var24++) {
                     var89[var24] = var89[var23];
                  }
               }

               var92 = var23;
            }
         }

         if (var92 >= 0) {
            double var93 = var89[var92];

            for (int var25 = var92 + 1; var25 < var11 && var90[var25] - var90[var92] <= 8.0 && var88[var25] >= var93 - 1.0; var25++) {
               var89[var25] = var93;
            }
         }

         double var94 = var91 >= 0 ? var89[var91] : Double.NaN;
         boolean[] var95 = new boolean[var11];
         double[] var26 = new double[var11];

         for (int var27 = 0; var27 < var11; var27++) {
            var95[var27] = !Double.isNaN(var89[var27]);
            var26[var27] = var95[var27] ? var89[var27] + RiverEngine.insetFor(b(((NetworkTracer.Pt)var10.get(var27)).acc(), var3), var3) : Double.NaN;
            if (var95[var27]) {
               var88[var27] = Math.max(var88[var27], var26[var27]);
            }
         }

         ArrayList var96 = new ArrayList(var11);
         NetworkTracer.Pt var28 = (NetworkTracer.Pt)var10.get(0);
         double var29 = a(var5, var28.x(), var28.z(), var28.acc(), var3);
         double var31 = var4 ? a(var1, var2, var5, var3, var28.acc(), var28.x(), var28.z()) : (Double.isNaN(var29) ? var28.y() : Math.max(var28.y(), var29));
         if (var95[0]) {
            var31 = Math.max(var31, var26[0]);
         } else if (var91 > 0) {
            var31 = Math.max(var31, var94 + RiverEngine.insetFor(b(var28.acc(), var3), var3));
         }

         var96.add(new NetworkTracer.Pt(var28.x(), var28.z(), var31, var28.acc()));
         double var33 = var31;
         double[][] var35 = new double[var11][];
         int[] var36 = new int[var11];
         double[] var37 = new double[var11];
         double[] var38 = new double[var11];
         double[] var39 = new double[var11];
         double[] var40 = new double[var11];
         double[] var41 = new double[var11];
         double[][] var42 = new double[var11][];
         double[][] var43 = new double[var11][];
         int var44 = 0;

         for (int var45 = 1; var45 < var11 - 1; var45++) {
            NetworkTracer.Pt var46 = (NetworkTracer.Pt)var10.get(var45 - 1);
            NetworkTracer.Pt var47 = (NetworkTracer.Pt)var10.get(var45);
            NetworkTracer.Pt var48 = (NetworkTracer.Pt)var10.get(var45 + 1);
            double var49 = var48.x() - var46.x();
            double var51 = var48.z() - var46.z();
            double var53 = Math.hypot(var49, var51);
            if (!(var53 <= 1.0E-6) && !var95[var45]) {
               var39[var45] = var49 / var53;
               var40[var45] = var51 / var53;
               double var55 = -var51 / var53;
               double var57 = var49 / var53;
               var37[var45] = var55;
               var38[var45] = var57;
               double var59 = b(var47.acc(), var3) + 2.0;
               var41[var45] = var59;
               double var61 = Math.max(var14, var59);
               int var63 = Math.max(2, (int)Math.ceil(var61 / var6 - 1.0E-9));
               var36[var45] = var63;
               var44 = Math.max(var44, var63);
               double[] var64 = new double[2 * var63 + 1];
               var35[var45] = var64;
               double[] var65 = new double[2 * var63 + 1];
               double[] var66 = new double[2 * var63 + 1];
               var42[var45] = var65;
               var43[var45] = var66;

               for (int var67 = -var63; var67 <= var63; var67++) {
                  double var68 = var67 == 0 ? var16[var45] : var47.x() + var55 * var67 * var6;
                  double var70 = var67 == 0 ? var17[var45] : var47.z() + var57 * var67 * var6;
                  if (var9) {
                     var68 = ReadLattice.snap(var68, var8);
                     var70 = ReadLattice.snap(var70, var8);
                  }

                  var65[var67 + var63] = var68;
                  var66[var67 + var63] = var70;
                  if (var67 == 0) {
                     var64[var63] = var15 ? b(var1, var2, var68, var70) : var88[var45];
                  } else if (var15) {
                     var64[var67 + var63] = Double.isNaN(a(var5, var68, var70)) ? b(var1, var2, var68, var70) : Double.NaN;
                  } else {
                     var64[var67 + var63] = a(var1, var2, var5, var3, var47.acc(), var68, var70);
                  }
               }
            }
         }

         int var97 = 2 * var44 + 1;
         double[] var98 = new double[var97];
         double[] var99 = new double[var97];
         int[][] var100 = new int[var11][];
         Arrays.fill(var98, Double.POSITIVE_INFINITY);
         var98[var44] = 0.0;

         for (int var101 = 1; var101 < var11; var101++) {
            Arrays.fill(var99, Double.POSITIVE_INFINITY);
            int[] var50 = new int[var97];
            var100[var101] = var50;
            double[] var104 = var35[var101];
            int var52 = var36[var101];

            for (int var107 = -var44; var107 <= var44; var107++) {
               double var54;
               if (var104 != null && var101 != var11 - 1) {
                  if (Math.abs(var107) >= var52) {
                     continue;
                  }

                  var54 = var104[var107 + var52];
                  if (Double.isNaN(var54)) {
                     continue;
                  }
               } else {
                  if (var107 != 0) {
                     continue;
                  }

                  var54 = 0.0;
               }

               double var56 = Double.POSITIVE_INFINITY;
               int var58 = 0;
               boolean var116 = var104 == null || var101 == var11 - 1;
               int var60 = var116 ? -var44 : Math.max(-var44, var107 - 1);
               int var118 = var116 ? var44 : Math.min(var44, var107 + 1);

               for (int var62 = var60; var62 <= var118; var62++) {
                  double var120 = var98[var62 + var44];
                  if (var120 != Double.POSITIVE_INFINITY) {
                     var120 += 0.25 * Math.abs(var107 - var62) * var6;
                     if (var120 < var56) {
                        var56 = var120;
                        var58 = var62;
                     }
                  }
               }

               if (var56 != Double.POSITIVE_INFINITY) {
                  var99[var107 + var44] = var56 + var54;
                  var50[var107 + var44] = var58;
               }
            }

            double[] var108 = var98;
            var98 = var99;
            var99 = var108;
         }

         int[] var102 = new int[var11];
         int var103 = 0;

         for (int var105 = var11 - 1; var105 >= 1; var105--) {
            var102[var105] = var103;
            var103 = var100[var105][var103 + var44];
         }

         double var106 = 0.0;
         int var109 = 0;

         for (int var110 = 1; var110 < var11 - 1; var110++) {
            NetworkTracer.Pt var112 = (NetworkTracer.Pt)var10.get(var110);
            double var114 = var16[var110];
            double var115 = var17[var110];
            double var117 = var88[var110];
            double var119 = Double.NaN;
            double var122 = Double.NaN;
            double[] var123 = var35[var110];
            if (var9 && var123 != null) {
               double var125 = var37[var110];
               double var128 = var38[var110];
               double var131 = var41[var110];
               int var132 = var36[var110];
               NetworkTracer.Pt var133 = (NetworkTracer.Pt)var96.get(var96.size() - 1);
               double var134 = 0.25 * var6;
               int var136 = Integer.MIN_VALUE;

               for (int var143 : new int[]{var102[var110], var109, 0}) {
                  if (Math.abs(var143) <= var132) {
                     double var144 = var42[var110][var143 + var132];
                     double var146 = var43[var110][var143 + var132];
                     if ((var144 - var133.x()) * var39[var110] + (var146 - var133.z()) * var40[var110] >= var134) {
                        var136 = var143;
                        var114 = var144;
                        var115 = var146;
                        break;
                     }
                  }
               }

               if (var136 == Integer.MIN_VALUE) {
                  continue;
               }

               var109 = var136;
               if (var136 != 0) {
                  var117 = var15 ? a(var1, var2, var5, var3, var112.acc(), var114, var115) : var123[var136 + var132];
               }

               int var138 = (int)Math.round(var131 / var6);
               int var140 = var136 + var138;
               int var142 = var136 - var138;
               var119 = !var15 && Math.abs(var140) <= var132
                  ? var123[var140 + var132]
                  : a(var1, var2, var5, var3, var112.acc(), ReadLattice.snap(var114 + var125 * var131, var8), ReadLattice.snap(var115 + var128 * var131, var8));
               var122 = !var15 && Math.abs(var142) <= var132
                  ? var123[var142 + var132]
                  : a(var1, var2, var5, var3, var112.acc(), ReadLattice.snap(var114 - var125 * var131, var8), ReadLattice.snap(var115 - var128 * var131, var8));
            } else if (var123 != null) {
               var114 = var112.x();
               var115 = var112.z();
               double var124 = var37[var110];
               double var69 = var38[var110];
               double var71 = var41[var110];
               int var73 = var36[var110];
               int var74 = var102[var110];
               double var75 = 0.0;
               if (var74 != 0) {
                  double var77 = var123[var74 - 1 + var73];
                  double var79 = var123[var74 + var73];
                  double var81 = var123[var74 + 1 + var73];
                  double var83 = 0.0;
                  if (!Double.isNaN(var77) && !Double.isNaN(var81)) {
                     double var85 = var77 - 2.0 * var79 + var81;
                     if (var85 > 1.0E-9) {
                        var83 = Math.max(-0.5, Math.min(0.5, 0.5 * (var77 - var81) / var85));
                     }
                  }

                  var75 = (var74 + var83) * var6;
               }

               if (var75 != 0.0 || var106 != 0.0) {
                  NetworkTracer.Pt var135 = (NetworkTracer.Pt)var96.get(var96.size() - 1);
                  double var78 = var39[var110];
                  double var80 = var40[var110];
                  double var82 = 0.25 * var6;
                  double var84 = var112.x() + var124 * var75;
                  double var86 = var112.z() + var69 * var75;
                  if ((var84 - var135.x()) * var78 + (var86 - var135.z()) * var80 < var82) {
                     var75 = var106;
                     var84 = var112.x() + var124 * var75;
                     var86 = var112.z() + var69 * var75;
                     if ((var84 - var135.x()) * var78 + (var86 - var135.z()) * var80 < var82) {
                        var75 = 0.0;
                     }
                  }
               }

               var106 = var75;
               if (var75 != 0.0) {
                  var114 = var112.x() + var124 * var75;
                  var115 = var112.z() + var69 * var75;
                  var117 = a(var1, var2, var5, var3, var112.acc(), var114, var115);
               }

               var119 = var15 ? Double.NaN : a(var123, var73, var6, var75 + var71);
               var122 = var15 ? Double.NaN : a(var123, var73, var6, var75 - var71);
               if (Double.isNaN(var119)) {
                  var119 = a(var1, var2, var5, var3, var112.acc(), var114 + var124 * var71, var115 + var69 * var71);
               }

               if (Double.isNaN(var122)) {
                  var122 = a(var1, var2, var5, var3, var112.acc(), var114 - var124 * var71, var115 - var69 * var71);
               }
            }

            if (var9) {
               NetworkTracer.Pt var126 = (NetworkTracer.Pt)var96.get(var96.size() - 1);
               if (var126.x() == var114 && var126.z() == var115) {
                  continue;
               }
            }

            double var127 = Math.max(var12, Math.min(var33, var117));
            if (!var95[var110] && !Double.isNaN(var119) && !Double.isNaN(var122)) {
               double var129 = Math.min(var119, var122);
               if (var129 > var12 && var129 < var127) {
                  var127 = Math.min(var127, Math.max(var129, var117 - 6.0));
               }
            }

            if (var91 >= 0 && var110 < var91) {
               double var130 = var94 + RiverEngine.insetFor(b(var112.acc(), var3), var3);
               var127 = Math.min(Math.max(var127, var130), var33);
            }

            var33 = var127;
            var96.add(new NetworkTracer.Pt(var114, var115, var127, var112.acc()));
         }

         NetworkTracer.Pt var111 = (NetworkTracer.Pt)var10.get(var11 - 1);
         double var113 = var95[var11 - 1] ? Math.min(var33, var88[var11 - 1]) : Math.min(var33, var111.y());
         var96.add(new NetworkTracer.Pt(var111.x(), var111.z(), var113, var111.acc()));
         return var96;
      } else {
         return var0;
      }
   }

   private static double a(double[] var0, int var1, double var2, double var4) {
      double var6 = var4 / var2;
      if (!(var6 < -var1) && !(var6 > var1)) {
         int var8 = Math.max(-var1, (int)Math.floor(var6));
         int var9 = Math.min(var1, var8 + 1);
         double var10 = var0[var8 + var1];
         double var12 = var0[var9 + var1];
         return !Double.isNaN(var10) && !Double.isNaN(var12) ? var10 + (var12 - var10) * (var6 - var8) : Double.NaN;
      } else {
         return Double.NaN;
      }
   }

   private static List<NetworkTracer.Pt> c(
      List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3, boolean var4, List<Lake> var5
   ) {
      int var6 = var0.size();
      if (var6 < 2) {
         return var0;
      }

      int var7 = var3.valleySnapRadius();
      double var8 = Math.max(1.0, var7 / 8.0);
      double var10 = var3.seaLevel() - 1;
      ArrayList var12 = new ArrayList(var6);
      NetworkTracer.Pt var13 = (NetworkTracer.Pt)var0.get(0);
      double var14 = a(var5, var13.x(), var13.z(), var13.acc(), var3);
      double var16 = var4 ? a(var1, var2, var5, var3, var13.acc(), var13.x(), var13.z()) : (Double.isNaN(var14) ? var13.y() : Math.max(var13.y(), var14));
      var12.add(new NetworkTracer.Pt(var13.x(), var13.z(), var16, var13.acc()));
      double var18 = var16;

      for (int var20 = 1; var20 < var6 - 1; var20++) {
         NetworkTracer.Pt var21 = (NetworkTracer.Pt)var0.get(var20 - 1);
         NetworkTracer.Pt var22 = (NetworkTracer.Pt)var0.get(var20);
         NetworkTracer.Pt var23 = (NetworkTracer.Pt)var0.get(var20 + 1);
         double var24 = var23.x() - var21.x();
         double var26 = var23.z() - var21.z();
         double var28 = Math.hypot(var24, var26);
         double var30 = var22.x();
         double var32 = var22.z();
         double var34 = a(var1, var2, var5, var3, var22.acc(), var22.x(), var22.z());
         double var36 = 0.0;
         if (var28 > 1.0E-6 && Double.isNaN(a(var5, var22.x(), var22.z()))) {
            double var38 = -var26 / var28;
            double var40 = var24 / var28;
            if (var1.hasEstimate()) {
               double var54 = b(var1, var2, var22.x(), var22.z());

               for (double var55 = -var7; var55 <= var7 + 1.0E-9; var55 += var8) {
                  double var46 = var22.x() + var38 * var55;
                  double var48 = var22.z() + var40 * var55;
                  if (!(Math.abs(var55) < 1.0E-9) && Double.isNaN(a(var5, var46, var48))) {
                     double var50 = b(var1, var2, var46, var48);
                     if (var50 < var54) {
                        var54 = var50;
                        var36 = var55;
                     }
                  }
               }

               if (Math.abs(var36) > 1.0E-9 && Math.abs(var36) < var7 - 1.0E-9) {
                  var30 = var22.x() + var38 * var36;
                  var32 = var22.z() + var40 * var36;
                  var34 = a(var1, var2, var5, var3, var22.acc(), var30, var32);
               }
            } else {
               for (double var42 = -var7; var42 <= var7 + 1.0E-9; var42 += var8) {
                  if (!(Math.abs(var42) < 1.0E-9)) {
                     double var44 = a(var1, var2, var5, var3, var22.acc(), var22.x() + var38 * var42, var22.z() + var40 * var42);
                     if (var44 < var34) {
                        var34 = var44;
                        var36 = var42;
                     }
                  }
               }

               if (Math.abs(var36) < var7 - 1.0E-9) {
                  var30 = var22.x() + var38 * var36;
                  var32 = var22.z() + var40 * var36;
               } else {
                  var34 = a(var1, var2, var5, var3, var22.acc(), var22.x(), var22.z());
               }
            }
         }

         double var53 = Math.max(var10, Math.min(var18, var34));
         if (af != null && b(var22.x(), var22.z())) {
            Constants.LOG
               .info(
                  "[SR-SEGDUMP] snap@({},{}) coarse={} centreGround={} bh={} at k={} -> ({},{}) prev={} grade={}",
                  new Object[]{
                     String.format(Locale.ROOT, "%.1f", var22.x()),
                     String.format(Locale.ROOT, "%.1f", var22.z()),
                     String.format(Locale.ROOT, "%.2f", var22.y()),
                     String.format(Locale.ROOT, "%.2f", a(var1, var2, var5, var3, var22.acc(), var22.x(), var22.z())),
                     String.format(Locale.ROOT, "%.2f", var34),
                     String.format(Locale.ROOT, "%.1f", var36),
                     String.format(Locale.ROOT, "%.1f", var30),
                     String.format(Locale.ROOT, "%.1f", var32),
                     String.format(Locale.ROOT, "%.2f", var18),
                     String.format(Locale.ROOT, "%.2f", var53)
                  }
               );
         }

         var18 = var53;
         var12.add(new NetworkTracer.Pt(var30, var32, var53, var22.acc()));
      }

      NetworkTracer.Pt var52 = (NetworkTracer.Pt)var0.get(var6 - 1);
      var12.add(new NetworkTracer.Pt(var52.x(), var52.z(), Math.min(var18, var52.y()), var52.acc()));
      return var12;
   }

   private static double a(TerrainSampler var0, ColumnCache var1, List<Lake> var2, StreamsConfig var3, double var4, double var6, double var8) {
      double var10 = a(var0, var1, var6, var8);
      double var12 = a(var2, var6, var8, var4, var3);
      return Double.isNaN(var12) ? var10 : Math.max(var10, var12);
   }

   private static double a(List<Lake> var0, double var1, double var3, double var5, StreamsConfig var7) {
      double var8 = a(var0, var1, var3);
      if (Double.isNaN(var8)) {
         var8 = a(var0, var1, var3, 4.0);
         if (Double.isNaN(var8)) {
            return Double.NaN;
         }
      }

      return var8 + RiverEngine.insetFor(b(var5, var7), var7);
   }

   private static double a(List<Lake> var0, double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1);
      int var8 = (int)Math.floor(var3);
      int var9 = (int)Math.ceil(var5);
      double var10 = Double.NaN;

      for (Lake var13 : var0) {
         if (var13.coversWithin(var7, var8, var9 / Math.max(1, var13.spacing()) + var13.reachCells() + 2)) {
            for (int var14 = -var9; var14 <= var9; var14++) {
               for (int var15 = -var9; var15 <= var9; var15++) {
                  if (!(var15 * var15 + var14 * var14 > var5 * var5) && var13.wetAt(var7 + var15, var8 + var14)) {
                     if (Double.isNaN(var10) || var13.level() < var10) {
                        var10 = var13.level();
                     }

                     var14 = var9;
                     break;
                  }
               }
            }
         }
      }

      return var10;
   }

   private static double a(List<Lake> var0, double var1, double var3) {
      int var5 = (int)Math.floor(var1);
      int var6 = (int)Math.floor(var3);

      for (Lake var8 : var0) {
         if (var8.wetAt(var5, var6)) {
            return var8.level();
         }
      }

      return Double.NaN;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3, List<Lake> var4) {
      int var5 = var0.size();
      if (var5 < 3) {
         return var0;
      }

      double var6 = var3.seaLevel() - 1;
      double[] var8 = new double[var5];
      boolean[] var9 = new boolean[var5];
      double[] var10 = new double[var5];
      double[] var11 = new double[var5];

      for (int var12 = 0; var12 < var5; var12++) {
         var8[var12] = a(var1, var2, ((NetworkTracer.Pt)var0.get(var12)).x(), ((NetworkTracer.Pt)var0.get(var12)).z());
         var11[var12] = a(var4, ((NetworkTracer.Pt)var0.get(var12)).x(), ((NetworkTracer.Pt)var0.get(var12)).z());
         var10[var12] = var12 == 0
            ? 0.0
            : var10[var12 - 1]
               + Math.hypot(
                  ((NetworkTracer.Pt)var0.get(var12)).x() - ((NetworkTracer.Pt)var0.get(var12 - 1)).x(),
                  ((NetworkTracer.Pt)var0.get(var12)).z() - ((NetworkTracer.Pt)var0.get(var12 - 1)).z()
               );
      }

      int var51 = -1;
      int var13 = -1;

      for (int var14 = 0; var14 < var5; var14++) {
         if (!Double.isNaN(var11[var14])) {
            if (var51 < 0) {
               var51 = var14;
            }

            if (var13 >= 0 && var11[var13] == var11[var14] && var10[var14] - var10[var13] <= 24.0) {
               for (int var15 = var13 + 1; var15 < var14; var15++) {
                  var11[var15] = var11[var14];
               }
            }

            var13 = var14;
         }
      }

      if (var13 >= 0) {
         double var52 = var11[var13];

         for (int var16 = var13 + 1; var16 < var5 && var10[var16] - var10[var13] <= 8.0 && var8[var16] >= var52 - 1.0; var16++) {
            var11[var16] = var52;
         }
      }

      double var53 = var51 >= 0 ? var11[var51] : Double.NaN;
      double[] var54 = new double[var5];

      for (int var17 = 0; var17 < var5; var17++) {
         var9[var17] = !Double.isNaN(var11[var17]);
         var54[var17] = var9[var17] ? var11[var17] + RiverEngine.insetFor(b(((NetworkTracer.Pt)var0.get(var17)).acc(), var3), var3) : Double.NaN;
         if (var9[var17]) {
            var8[var17] = Math.max(var8[var17], var54[var17]);
         }
      }

      ArrayList var55 = new ArrayList(var5);
      NetworkTracer.Pt var18 = (NetworkTracer.Pt)var0.get(0);
      double var19 = var9[0]
         ? Math.max(var18.y(), var54[0])
         : (var51 > 0 ? Math.max(var18.y(), var53 + RiverEngine.insetFor(b(var18.acc(), var3), var3)) : var18.y());
      var55.add(new NetworkTracer.Pt(var18.x(), var18.z(), var19, var18.acc()));
      double var21 = var19;

      for (int var23 = 1; var23 < var5 - 1; var23++) {
         NetworkTracer.Pt var24 = (NetworkTracer.Pt)var0.get(var23 - 1);
         NetworkTracer.Pt var25 = (NetworkTracer.Pt)var0.get(var23);
         NetworkTracer.Pt var26 = (NetworkTracer.Pt)var0.get(var23 + 1);
         double var27 = var8[var23];
         double var29 = var27;
         double var31 = var9[var23] ? Math.max(var6, Math.min(var21, var29)) : Math.max(var6, Math.min(var21, Math.min(var25.y(), var29)));
         double var33 = var26.x() - var24.x();
         double var35 = var26.z() - var24.z();
         double var37 = Math.hypot(var33, var35);
         if (var37 > 1.0E-6 && !var9[var23]) {
            double var39 = -var35 / var37;
            double var41 = var33 / var37;
            double var43 = b(var25.acc(), var3) + 2.0;
            double var45 = a(var1, var2, var4, var3, var25.acc(), var25.x() + var39 * var43, var25.z() + var41 * var43);
            double var47 = a(var1, var2, var4, var3, var25.acc(), var25.x() - var39 * var43, var25.z() - var41 * var43);
            double var49 = Math.min(var45, var47);
            if (var49 > var6 && var49 < var31) {
               var31 = Math.min(var31, Math.max(var49, var27 - 6.0));
            }
         }

         if (var51 >= 0 && var23 < var51) {
            double var58 = var53 + RiverEngine.insetFor(b(var25.acc(), var3), var3);
            var31 = Math.min(Math.max(var31, var58), var21);
         }

         var21 = var31;
         var55.add(new NetworkTracer.Pt(var25.x(), var25.z(), var31, var25.acc()));
      }

      NetworkTracer.Pt var56 = (NetworkTracer.Pt)var0.get(var5 - 1);
      double var57 = var9[var5 - 1] ? Math.min(var21, var8[var5 - 1]) : Math.min(var21, var56.y());
      var55.add(new NetworkTracer.Pt(var56.x(), var56.z(), var57, var56.acc()));
      return var55;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2) {
      if (var0.size() < 2) {
         return var0;
      }

      NetworkTracer.Pt var3 = (NetworkTracer.Pt)var0.get(0);
      double var4 = Math.min(var3.y(), a(var1, var2, var3.x(), var3.z()));
      if (var4 >= var3.y()) {
         return var0;
      }

      ArrayList var6 = new ArrayList(var0);
      var6.set(0, new NetworkTracer.Pt(var3.x(), var3.z(), var4, var3.acc()));
      return var6;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3) {
      int var4 = var0.size();
      if (var4 < 3) {
         return var0;
      }

      double var5 = var3.gridSpacing();
      double var7 = Math.max(1.0, var5 / 8.0);
      ArrayList var9 = new ArrayList(var4);
      var9.add((NetworkTracer.Pt)var0.get(0));

      for (int var10 = 1; var10 < var4 - 1; var10++) {
         NetworkTracer.Pt var11 = (NetworkTracer.Pt)var0.get(var10 - 1);
         NetworkTracer.Pt var12 = (NetworkTracer.Pt)var0.get(var10);
         NetworkTracer.Pt var13 = (NetworkTracer.Pt)var0.get(var10 + 1);
         double var14 = var13.x() - var11.x();
         double var16 = var13.z() - var11.z();
         double var18 = Math.hypot(var14, var16);
         if (var18 < 1.0E-6) {
            var9.add(var12);
         } else {
            double var20 = -var16 / var18;
            double var22 = var14 / var18;
            double var24 = b(var12.acc(), var3) + 3.0;
            double var26 = a(var1, var2, var12.x() - var20 * var24, var12.z() - var22 * var24);
            double var28 = a(var1, var2, var12.x() + var20 * var24, var12.z() + var22 * var24);
            if (Math.min(var26, var28) >= var12.y()) {
               var9.add(var12);
            } else {
               boolean var30 = var1.hasEstimate();
               double var31 = 0.0;
               double var33 = var30 ? b(var1, var2, var12.x(), var12.z()) : a(var1, var2, var12.x(), var12.z());

               for (double var35 = -var5; var35 <= var5 + 1.0E-9; var35 += var7) {
                  double var37 = var12.x() + var20 * var35;
                  double var39 = var12.z() + var22 * var35;
                  double var41 = var30 ? b(var1, var2, var37, var39) : a(var1, var2, var37, var39);
                  if (var41 < var33) {
                     var33 = var41;
                     var31 = var35;
                  }
               }

               if (Math.abs(var31) > 1.0E-9 && Math.abs(var31) < var5 - 1.0E-9) {
                  var9.add(new NetworkTracer.Pt(var12.x() + var20 * var31, var12.z() + var22 * var31, var12.y(), var12.acc()));
               } else {
                  var9.add(var12);
               }
            }
         }
      }

      var9.add((NetworkTracer.Pt)var0.get(var4 - 1));
      return var9;
   }

   private static double b(TerrainSampler var0, double var1, double var3) {
      return var0.groundHeight((int)Math.round(var1), (int)Math.round(var3));
   }

   private static double a(TerrainSampler var0, Map<Long, Double> var1, double var2, double var4, LongAdder var6) {
      var6.increment();
      return a(var0, var1, var2, var4);
   }

   private static double a(TerrainSampler var0, ColumnCache var1, double var2, double var4) {
      int var6 = (int)Math.round(var2);
      int var7 = (int)Math.round(var4);
      long var8 = ColumnKey.of(var6, var7);
      double var10 = var1.get(var8, Double.NaN);
      if (!Double.isNaN(var10)) {
         return var10;
      }

      double var12 = var0.groundHeight(var6, var7);
      var1.put(var8, var12);
      return var12;
   }

   private static double a(TerrainSampler var0, Map<Long, Double> var1, double var2, double var4) {
      int var6 = (int)Math.round(var2);
      int var7 = (int)Math.round(var4);
      long var8 = ColumnKey.of(var6, var7);
      Double var10 = (Double)var1.get(var8);
      if (var10 != null) {
         return var10;
      }

      double var11 = var0.groundHeight(var6, var7);
      var1.put(var8, var11);
      return var11;
   }

   private static double b(TerrainSampler var0, ColumnCache var1, double var2, double var4) {
      int var6 = (int)Math.round(var2);
      int var7 = (int)Math.round(var4);
      long var8 = ColumnKey.mix(ColumnKey.pack(var6, var7) ^ 4611686018427387904L);
      double var10 = var1.get(var8, Double.NaN);
      if (!Double.isNaN(var10)) {
         return var10;
      }

      double var12 = var0.estimate(var6, var7);
      var1.put(var8, var12);
      return var12;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, double var1) {
      if (var0.size() >= 2 && !(var1 < 1.0)) {
         ArrayList var3 = new ArrayList();

         for (int var4 = 0; var4 < var0.size() - 1; var4++) {
            NetworkTracer.Pt var5 = (NetworkTracer.Pt)var0.get(var4);
            NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var4 + 1);
            var3.add(var5);
            double var7 = Math.hypot(var6.x() - var5.x(), var6.z() - var5.z());
            int var9 = (int)Math.floor(var7 / var1);

            for (int var10 = 1; var10 <= var9; var10++) {
               double var11 = var10 * var1 / var7;
               if (var11 < 1.0) {
                  var3.add(a(var5, var6, var11));
               }
            }
         }

         var3.add((NetworkTracer.Pt)var0.get(var0.size() - 1));
         return var3;
      } else {
         return var0;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, List<NetworkTracer.Pt> var1, long var2, int var4, StreamsConfig var5) {
      int var6 = var1.size();
      if (var0 != var1 && var6 >= 1 && var0.size() > var6 && var0.get(var6 - 1) == var1.get(var6 - 1)) {
         List var7 = a(new ArrayList<>(var0.subList(var6 - 1, var0.size())), var5.gridSpacing() * 0.4);
         ArrayList var8 = new ArrayList(var6 - 1 + var7.size());
         var8.addAll(var0.subList(0, var6 - 1));
         var8.addAll(var7);
         int var9 = var8.size();
         int var10 = var6 - 1;
         double[] var11 = new double[var9];

         for (int var12 = 1; var12 < var9; var12++) {
            var11[var12] = var11[var12 - 1]
               + Math.hypot(
                  ((NetworkTracer.Pt)var8.get(var12)).x() - ((NetworkTracer.Pt)var8.get(var12 - 1)).x(),
                  ((NetworkTracer.Pt)var8.get(var12)).z() - ((NetworkTracer.Pt)var8.get(var12 - 1)).z()
               );
         }

         double var47 = var11[var9 - 1];
         if (var47 <= 1.0E-6) {
            return var0;
         }

         double var14 = var5.gridSpacing() * var5.meanderStrength();
         double var16 = Hashing.unit(var2, var4, 11L) * Math.PI * 2.0;
         double var18 = Hashing.unit(var2, var4, 12L) * Math.PI * 2.0;
         double var20 = Hashing.unit(var2, var4, 13L) * Math.PI * 2.0;
         ArrayList var22 = new ArrayList(var9);

         for (int var23 = 0; var23 <= var10; var23++) {
            var22.add((NetworkTracer.Pt)var8.get(var23));
         }

         for (int var48 = var10 + 1; var48 < var9 - 1; var48++) {
            NetworkTracer.Pt var24 = (NetworkTracer.Pt)var8.get(var48 - 1);
            NetworkTracer.Pt var25 = (NetworkTracer.Pt)var8.get(var48);
            NetworkTracer.Pt var26 = (NetworkTracer.Pt)var8.get(var48 + 1);
            double var27 = var26.x() - var24.x();
            double var29 = var26.z() - var24.z();
            double var31 = Math.hypot(var27, var29);
            if (var31 < 1.0E-6) {
               var22.add(var25);
            } else {
               double var33 = -var29 / var31;
               double var35 = var27 / var31;
               double var37 = Math.sin(Math.PI * var11[var48] / var47);
               double var39 = Math.min(1.0, (var11[var48] - var11[var10]) / Math.max(1.0, var5.gridSpacing()));
               double var41 = Math.abs(var24.y() - var26.y()) / var31;
               double var43 = Math.max(0.0, 1.0 - var41 / 1.0);
               double var45 = var14
                  * var37
                  * var39
                  * var43
                  * (0.5 * Math.sin(var11[var48] * 0.03 + var16) + 0.3 * Math.sin(var11[var48] * 0.07 + var18) + 0.2 * Math.sin(var11[var48] * 0.017 + var20));
               var22.add(new NetworkTracer.Pt(var25.x() + var33 * var45, var25.z() + var35 * var45, var25.y(), var25.acc()));
            }
         }

         var22.add((NetworkTracer.Pt)var8.get(var9 - 1));
         return var22;
      } else {
         return var0;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, long var1, int var3, StreamsConfig var4) {
      int var5 = var0.size();
      if (var5 < 3) {
         return var0;
      }

      double var6 = var4.gridSpacing() * var4.meanderStrength();
      double var8 = Hashing.unit(var1, var3, 11L) * Math.PI * 2.0;
      double var10 = Hashing.unit(var1, var3, 12L) * Math.PI * 2.0;
      double var12 = Hashing.unit(var1, var3, 13L) * Math.PI * 2.0;
      double[] var14 = new double[var5];

      for (int var15 = 1; var15 < var5; var15++) {
         var14[var15] = var14[var15 - 1]
            + Math.hypot(
               ((NetworkTracer.Pt)var0.get(var15)).x() - ((NetworkTracer.Pt)var0.get(var15 - 1)).x(),
               ((NetworkTracer.Pt)var0.get(var15)).z() - ((NetworkTracer.Pt)var0.get(var15 - 1)).z()
            );
      }

      ArrayList var38 = new ArrayList(var5);
      var38.add((NetworkTracer.Pt)var0.get(0));

      for (int var16 = 1; var16 < var5 - 1; var16++) {
         NetworkTracer.Pt var17 = (NetworkTracer.Pt)var0.get(var16 - 1);
         NetworkTracer.Pt var18 = (NetworkTracer.Pt)var0.get(var16);
         NetworkTracer.Pt var19 = (NetworkTracer.Pt)var0.get(var16 + 1);
         double var20 = var19.x() - var17.x();
         double var22 = var19.z() - var17.z();
         double var24 = Math.hypot(var20, var22);
         if (var24 < 1.0E-6) {
            var38.add(var18);
         } else {
            double var26 = -var22 / var24;
            double var28 = var20 / var24;
            double var30 = Math.sin(Math.PI * var16 / (var5 - 1));
            double var32 = Math.abs(var17.y() - var19.y()) / var24;
            double var34 = Math.max(0.0, 1.0 - var32 / 1.0);
            double var36 = var6
               * var30
               * var34
               * (0.5 * Math.sin(var14[var16] * 0.03 + var8) + 0.3 * Math.sin(var14[var16] * 0.07 + var10) + 0.2 * Math.sin(var14[var16] * 0.017 + var12));
            var38.add(new NetworkTracer.Pt(var18.x() + var26 * var36, var18.z() + var28 * var36, var18.y(), var18.acc()));
         }
      }

      var38.add((NetworkTracer.Pt)var0.get(var5 - 1));
      return var38;
   }

   private static List<NetworkTracer.Pt> c(List<NetworkTracer.Pt> var0, StreamsConfig var1) {
      int var2 = var0.size();
      if (var2 < 4) {
         return var0;
      }

      double[] var3 = new double[var2];
      double[] var4 = new double[var2];
      var4[0] = b(((NetworkTracer.Pt)var0.get(0)).acc(), var1);

      for (int var5 = 1; var5 < var2; var5++) {
         NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var5);
         NetworkTracer.Pt var7 = (NetworkTracer.Pt)var0.get(var5 - 1);
         var3[var5] = var3[var5 - 1] + Math.hypot(var6.x() - var7.x(), var6.z() - var7.z());
         var4[var5] = b(var6.acc(), var1);
      }

      ArrayList var21 = null;
      int var22 = 0;

      while (var22 < var2) {
         NetworkTracer.Pt var23 = (NetworkTracer.Pt)var0.get(var22);
         int var8 = -1;

         for (int var9 = var22 + 2; var9 < var2; var9++) {
            double var10 = 2.0 * Math.max(var4[var22], var4[var9]) + 2.0;
            double var12 = var3[var9] - var3[var22];
            if (var12 > Math.max(48.0, 12.0 * var10)) {
               break;
            }

            NetworkTracer.Pt var14 = (NetworkTracer.Pt)var0.get(var9);
            double var15 = var14.x() - var23.x();
            double var17 = var14.z() - var23.z();
            double var19 = var15 * var15 + var17 * var17;
            if (var19 < var10 * var10 && var12 - Math.sqrt(var19) > var10) {
               var8 = var9;
            }
         }

         if (var8 > 0) {
            if (var21 == null) {
               var21 = new ArrayList(var2);
               var21.addAll(var0.subList(0, var22));
            }

            var21.add(var23);
            at.incrementAndGet();
            var22 = var8;
         } else {
            if (var21 != null) {
               var21.add(var23);
            }

            var22++;
         }
      }

      return var21 == null ? var0 : var21;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, int var1) {
      for (int var2 = 0; var2 < var1 && var0.size() >= 3; var2++) {
         ArrayList var3 = new ArrayList(var0.size() * 2);
         var3.add((NetworkTracer.Pt)var0.get(0));

         for (int var4 = 0; var4 < var0.size() - 1; var4++) {
            var3.add(a((NetworkTracer.Pt)var0.get(var4), (NetworkTracer.Pt)var0.get(var4 + 1), 0.25));
            var3.add(a((NetworkTracer.Pt)var0.get(var4), (NetworkTracer.Pt)var0.get(var4 + 1), 0.75));
         }

         var3.add((NetworkTracer.Pt)var0.get(var0.size() - 1));
         var0 = var3;
      }

      return var0;
   }

   private static NetworkTracer.Pt a(NetworkTracer.Pt var0, NetworkTracer.Pt var1, double var2) {
      return new NetworkTracer.Pt(
         var0.x() + (var1.x() - var0.x()) * var2,
         var0.z() + (var1.z() - var0.z()) * var2,
         var0.y() + (var1.y() - var0.y()) * var2,
         var0.acc() + (var1.acc() - var0.acc()) * var2
      );
   }

   private static List<NetworkTracer.Pt> b(List<NetworkTracer.Pt> var0) {
      if (var0.size() < 3) {
         return var0;
      }

      ArrayList var1 = new ArrayList(var0.size());
      var1.add((NetworkTracer.Pt)var0.get(0));

      for (int var2 = 1; var2 < var0.size(); var2++) {
         NetworkTracer.Pt var3 = (NetworkTracer.Pt)var0.get(var2);
         NetworkTracer.Pt var4 = (NetworkTracer.Pt)var1.get(var1.size() - 1);
         boolean var5 = var2 == var0.size() - 1;
         if (Math.hypot(var3.x() - var4.x(), var3.z() - var4.z()) < 0.25) {
            if (var5 && var1.size() >= 2) {
               var1.set(var1.size() - 1, var3);
            } else if (var5) {
               var1.add(var3);
            }
         } else {
            var1.add(var3);
         }
      }

      return var1;
   }

   private static void a(
      List<NetworkTracer.Pt> var0, StreamsConfig var1, List<RiverSegment> var2, boolean var3, WetnessSampler var4, long var5, int var7, boolean var8
   ) {
      var0 = b(var0);
      double var9 = var3 ? var1.gridSpacing() * 1.5 : 0.0;
      HydraulicGeometry var11 = var1.hydraulics();
      if (var11 != null && var11.shapesNetwork()) {
         double[] var26 = a(var0, var1, var11, var4, var5, var7, var8);
         int var13 = var0.size();
         double var27 = 0.0;

         for (int var28 = 0; var28 < var0.size() - 1; var28++) {
            NetworkTracer.Pt var29 = (NetworkTracer.Pt)var0.get(var28);
            NetworkTracer.Pt var18 = (NetworkTracer.Pt)var0.get(var28 + 1);
            double var30 = Math.hypot(var18.x() - var29.x(), var18.z() - var29.z());
            double var31 = var26[var28] * c(var27, var9);
            double var32 = var26[var28 + 1] * c(var27 + var30, var9);
            var2.add(
               new RiverSegment(var29.x(), var29.z(), var29.y(), var31, var18.x(), var18.z(), var18.y(), var32, var26[var13 + var28], var26[var13 + var28 + 1])
            );
            var27 += var30;
         }
      } else {
         double var12 = 0.0;

         for (int var14 = 0; var14 < var0.size() - 1; var14++) {
            NetworkTracer.Pt var15 = (NetworkTracer.Pt)var0.get(var14);
            NetworkTracer.Pt var16 = (NetworkTracer.Pt)var0.get(var14 + 1);
            double var17 = Math.hypot(var16.x() - var15.x(), var16.z() - var15.z());
            double var19 = b(var15.acc(), var1) * c(var12, var9);
            double var21 = b(var16.acc(), var1) * c(var12 + var17, var9);
            double var23 = FlowSpeed.forReach(var17 > 1.0E-9 ? Math.abs(var15.y() - var16.y()) / var17 : 0.0, 1.0, 1.0);
            var2.add(new RiverSegment(var15.x(), var15.z(), var15.y(), var19, var16.x(), var16.z(), var16.y(), var21, var23, var23));
            var12 += var17;
         }
      }
   }

   private static double[] a(List<NetworkTracer.Pt> var0, StreamsConfig var1, HydraulicGeometry var2, WetnessSampler var3, long var4, int var6, boolean var7) {
      int var8 = var0.size();
      double[] var9 = new double[var8];

      for (int var10 = 1; var10 < var8; var10++) {
         var9[var10] = var9[var10 - 1]
            + Math.hypot(
               ((NetworkTracer.Pt)var0.get(var10)).x() - ((NetworkTracer.Pt)var0.get(var10 - 1)).x(),
               ((NetworkTracer.Pt)var0.get(var10)).z() - ((NetworkTracer.Pt)var0.get(var10 - 1)).z()
            );
      }

      double var58 = var9[var8 - 1];
      double var12 = Hashing.unit(var4, var6, 21L) * Math.PI * 2.0;
      double var14 = Hashing.unit(var4, var6, 22L) * Math.PI * 2.0;
      double[] var16 = new double[var8];
      double[] var17 = new double[var8];
      a(var0, var9, var3, var16, var17, var2);
      double[] var18 = new double[var8 * 2];
      double var19 = 0.0;
      int var21 = 0;
      int var22 = 0;

      for (int var23 = 0; var23 < var8; var23++) {
         while (var21 < var23 && var9[var23] - var9[var21] > 8.0) {
            var21++;
         }

         while (var22 < var8 - 1 && var9[var22 + 1] - var9[var23] <= 8.0) {
            var22++;
         }

         double var24 = var9[var22] - var9[var21];
         double var26 = var24 > 1.0E-6 ? Math.abs(((NetworkTracer.Pt)var0.get(var21)).y() - ((NetworkTracer.Pt)var0.get(var22)).y()) / var24 : 0.0;
         double var28 = Math.min(1.0, var26 / 0.12);
         double var30 = 1.0;
         if (var2.widthSlope() > 0.0) {
            var30 *= 1.0 + var2.widthSlope() * 0.3 * (1.0 - 2.0 * var28);
            var30 *= 1.0 + var2.widthSlope() * 0.15 * (1.0 - 2.0 * var17[var23]);
         }

         if (var2.widthClimate() > 0.0) {
            var30 *= 1.0 + var2.widthClimate() * 0.25 * (2.0 * var16[var23] - 1.0);
         }

         if (var2.widthVariation() > 0.0) {
            double var32 = 0.6 * Math.sin(var9[var23] * 0.045 + var12) + 0.4 * Math.sin(var9[var23] * 0.011 + var14);
            var30 *= 1.0 + var2.widthVariation() * 0.18 * var32;
         }

         double var61 = var23 > 0 ? var9[var23] - var9[var23 - 1] : 0.0;
         var19 = var26 <= 0.03 ? var19 + var61 : 0.0;
         double var34 = Math.max(0.0, Math.min(1.0, (0.6 - var17[var23]) / 0.6));
         double var36 = b(((NetworkTracer.Pt)var0.get(var23)).acc(), var1);
         double var38 = 0.0;
         if (var2.widthSlope() > 0.0) {
            double var40 = Math.max(0.0, 1.0 - var26 / 0.04);
            double var42 = Math.min(1.0, var19 / 64.0);
            if (((NetworkTracer.Pt)var0.get(var23)).y() <= var1.seaLevel() + 1) {
               var40 = 1.0;
               var42 = 1.0;
            }

            double var44 = var34 * var40 * var42 * Math.sqrt(var2.widthSlope());
            double var46 = Math.sqrt(var44);
            double var48 = 2.0 * var36 * var1.streamDepth();
            double var50 = var48 * (1.0 - var46) + HydraulicGeometry.braidFloor(var48) * var46;
            double var52 = 0.6666666666666666 + 0.33333333333333337 * var46;
            if (var50 > 1.0E-6) {
               double var54 = Math.max(0.2, 1.0 - HydraulicGeometry.expectedBarShare(var44));
               double var56 = var48 * 0.6666666666666666 / (var50 * var52 * var54);
               var38 = Math.max(0.0, Math.min(2.5, var56 - 1.0));
            }
         }

         if (var2.deltaMouths() > 0.0 && var7) {
            double var62 = var58 - var9[var23];
            if (var62 < 64.0) {
               double var64 = 1.0 - var62 / 64.0;
               double var66 = var64 * var64 * (3.0 - 2.0 * var64);
               double var67 = Math.min(1.0, var19 / 32.0);
               var38 = Math.max(var38, var2.deltaMouths() * 2.0 * var66 * var67 * var34);
            }
         }

         var30 *= 1.0 + var38;
         double var63 = var36;
         double var65 = Math.max(var1.baseWidth(), Math.min(var1.maxWidth(), var63 * var30));
         var18[var23] = a(var65, var63, var1);
         var18[var8 + var23] = FlowSpeed.forReach(var26, var18[var23], var63);
      }

      return var18;
   }

   private static void a(List<NetworkTracer.Pt> var0, double[] var1, WetnessSampler var2, double[] var3, double[] var4, HydraulicGeometry var5) {
      boolean var6 = var5.widthClimate() > 0.0;
      boolean var7 = var5.widthSlope() > 0.0 || var5.deltaMouths() > 0.0;
      double var8 = 0.5;
      double var10 = 1.0;
      double var12 = 0.0;

      for (int var14 = 0; var14 < var0.size(); var14++) {
         if (var1[var14] >= var12) {
            NetworkTracer.Pt var15 = (NetworkTracer.Pt)var0.get(var14);
            int var16 = (int)Math.round(var15.x());
            int var17 = (int)Math.round(var15.z());
            int var18 = (int)Math.round(var15.y());
            if (var6) {
               var8 = Math.max(0.0, Math.min(1.0, var2.wetness(var16, var18, var17)));
            }

            if (var7) {
               var10 = Math.max(0.0, Math.min(1.0, var2.cohesion(var16, var18, var17)));
            }

            var12 = var1[var14] + 32.0;
         }

         var3[var14] = var8;
         var4[var14] = var10;
      }
   }

   private static double a(double var0, double var2, StreamsConfig var4) {
      int var5 = RiverEngine.insetFor(var2, var4);
      if (RiverEngine.insetFor(var0, var4) == var5) {
         return var0;
      }

      if (var0 < var2) {
         return a(var2, var4);
      }

      double var6 = var2;
      double var8 = Math.max(var2, var4.maxWidth());

      for (int var10 = 0; var10 < 24; var10++) {
         double var11 = (var6 + var8) * 0.5;
         if (RiverEngine.insetFor(var11, var4) <= var5) {
            var6 = var11;
         } else {
            var8 = var11;
         }
      }

      return var6;
   }

   private static double c(double var0, double var2) {
      return var2 <= 0.0 ? 1.0 : Math.min(1.0, var0 / var2);
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, LowResGrid var1, int var2, int var3, TerrainSampler var4, StreamsConfig var5) {
      int var6 = var0.size();
      if (var6 >= 2 && var2 >= 0) {
         NetworkTracer.Pt var7 = (NetworkTracer.Pt)var0.get(var6 - 1);
         double var8 = Math.min(var7.y(), var3);
         int var10 = b(var7.acc(), var5) < 1.0 ? 1 : 2;
         if (Q) {
             List<Integer> var32 = a(var1, var2, var3 + 1, var3 - 1, 1);
            if (var32 != null && !a(var32, var1, var4, var3)) {
                ArrayList<NetworkTracer.Pt> var33 = new ArrayList<>(var0);

               for (int var37 : var32) {
                  var33.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var37)), var1.worldZ(var1.gj(var37)), var8, var7.acc()));
               }

               int var35 = var32.isEmpty() ? var2 : (Integer)var32.get(var32.size() - 1);
               if (!b(var1, var4, var35, var3)) {
                  return var0;
               } else {
                  return var1.height(var35) <= var3 - 4 ? a(var33, var8, 8.0, var4) : a(var33, var8, Math.max(32.0, 3.0 * b(var7.acc(), var5)), var4);
               }
            } else {
               return var0;
            }
         } else {
            int[] var11 = F.get();
            var11[0]++;
             List<Integer> var12 = a(var1, var4, var5, var2, var3, var8, var11);
            if (var12 == null) {
               var11[5]++;
               return var0;
            }

            if (a(var12, var1, var4, var3)) {
               var11[6]++;
               return var0;
            }

            if (!var12.isEmpty()) {
               var11[2]++;
               var11[3] += var12.size();
            }

             ArrayList<NetworkTracer.Pt> var13 = new ArrayList<>(var0);

            for (int var15 : var12) {
               var13.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var15)), var1.worldZ(var1.gj(var15)), var8, var7.acc()));
            }

            int var36 = var12.isEmpty() ? var2 : (Integer)var12.get(var12.size() - 1);
            if (var4.hasEstimate()) {
               int var38 = var36;
               double var16 = a(var1, var4, var38, var3);
               int var18 = 0;

               for (int var19 = 0; var19 < 24 && var16 > var3 - 24; var19++) {
                   List<Integer> var20 = a(var1, var4, var38, var3 + 1, var16 - 1.0, var10, var3);
                  if (var20 == null || var20.isEmpty() || a(var20, var1, var4, var3)) {
                     break;
                  }

                  for (int var22 : var20) {
                     var13.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var22)), var1.worldZ(var1.gj(var22)), var8, var7.acc()));
                     var18++;
                  }

                  var38 = (Integer)var20.get(var20.size() - 1);
                  var16 = a(var1, var4, var38, var3);
               }

               if (var38 != var36
                  && var16 < a(var1, var4, var36, var3) - 0.5
                  && b(var1, var4, var38, var3)
                  && (bX || b(var1, var4, var38, var3, new NetworkTracer.SeaEndReads()))) {
                  var11[4]++;
                  var11[1]++;
                  return var16 <= var3 - 4 ? a(var13, var8, 8.0, var4) : a(var13, var8, Math.max(32.0, 3.0 * b(var7.acc(), var5)), var4);
               }

               while (var18-- > 0) {
                  var13.remove(var13.size() - 1);
               }
            }

            int var39 = var36;
            int var41 = 0;
             HashSet<Integer> var17 = new HashSet<>();
            var17.add(var39);

            for (int var42 = 0; var42 < 2; var42++) {
               int var45 = var1.gi(var39);
               int var47 = var1.gj(var39);
               double var49 = var1.height(var39);
               int var23 = -1;
               double var24 = Double.POSITIVE_INFINITY;

               for (int var26 = 0; var26 < 8; var26++) {
                  int var27 = var45 + b[var26];
                  int var28 = var47 + c[var26];
                  if (var1.valid(var27, var28)) {
                     int var29 = var1.idx(var27, var28);
                     if (var1.inDomain(var29) && !var17.contains(var29)) {
                        double var30 = var1.height(var29);
                        if (!(var30 > var49) && !(var30 > var3 - 1) && !(var30 >= var24)) {
                           var23 = var29;
                           var24 = var30;
                        }
                     }
                  }
               }

               if (var23 < 0) {
                  break;
               }

               var17.add(var23);
               var39 = var23;
               var41++;
               var13.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var23)), var1.worldZ(var1.gj(var23)), var8, var7.acc()));
            }

            if (var41 > 0) {
               boolean var43 = var1.height(var39) < var1.height(var36) - 0.5;
               if (var43 && b(var1, var4, var39, var3)) {
                  var11[4]++;
               } else {
                  while (var41-- > 0) {
                     var13.remove(var13.size() - 1);
                  }

                  var39 = var36;
               }
            }

            if (!bX) {
               AtomicIntegerArray var44 = a(var1);
               NetworkTracer.SeaEndReads var46 = new NetworkTracer.SeaEndReads();
               boolean var48 = b(var1, var4, var39, var3, var46);
                List<Integer> var50 = null;
               if (!var48) {
                  var50 = a(var1, var4, var39, var3, var13, var46);
               }

               var44.addAndGet(5, var46.b);
               var44.addAndGet(6, var46.c);
               if (!var48) {
                  if (var50 == null) {
                     var44.incrementAndGet(4);
                     var11[5]++;
                     if (Boolean.getBoolean("streamsreflowing.seaenddiag")) {
                        a(var1, var4, var39, var3, var46);
                     }

                     if (af != null && b(var1.worldX(var1.gi(var39)), var1.worldZ(var1.gj(var39)))) {
                        Constants.LOG
                           .info(
                              "[SR-SEGDUMP] sea end at ({},{}) ground {} never reached sea-{} in {} steps -> dropped",
                              new Object[]{
                                 var1.worldX(var1.gi(var39)), var1.worldZ(var1.gj(var39)), String.format(Locale.ROOT, "%.0f", var46.a.get(var39)), 2, 3
                              }
                           );
                     }

                     return var0;
                  }

                  var44.incrementAndGet(0 + var50.size());
                  if (af != null && b(var1.worldX(var1.gi(var39)), var1.worldZ(var1.gj(var39)))) {
                     int var51 = (Integer)var50.get(var50.size() - 1);
                     Constants.LOG
                        .info(
                           "[SR-SEGDUMP] sea end at ({},{}) ground {} moved {} step(s) to ({},{}) ground {}",
                           new Object[]{
                              var1.worldX(var1.gi(var39)),
                              var1.worldZ(var1.gj(var39)),
                              String.format(Locale.ROOT, "%.0f", var46.a.get(var39)),
                              var50.size(),
                              var1.worldX(var1.gi(var51)),
                              var1.worldZ(var1.gj(var51)),
                              String.format(Locale.ROOT, "%.0f", var46.a.get(var51))
                           }
                        );
                  }

                  for (int var54 : var50) {
                     var13.add(new NetworkTracer.Pt(var1.worldX(var1.gi(var54)), var1.worldZ(var1.gj(var54)), var8, var7.acc()));
                  }

                  var39 = (Integer)var50.get(var50.size() - 1);
                  var11[1]++;
                  double var53 = var46.a.get(var39);
                  int var55 = var46.b;
                  int var25 = var46.c;
                   List<NetworkTracer.Pt> var56 = a(var13, var8, var53 <= var3 - 4 ? 8.0 : Math.max(32.0, 3.0 * b(var7.acc(), var5)), var4, var3, var46);
                  var44.addAndGet(5, var46.b - var55);
                  var44.addAndGet(6, var46.c - var25);
                  return var56;
               }

               var44.incrementAndGet(0);
            }

            var11[1]++;
            return var1.height(var39) <= var3 - 4 ? a(var13, var8, 8.0, var4) : a(var13, var8, Math.max(32.0, 3.0 * b(var7.acc(), var5)), var4);
         }
      } else {
         return var0;
      }
   }

   private static List<Integer> a(LowResGrid var0, TerrainSampler var1, StreamsConfig var2, int var3, int var4, double var5, int[] var7) {
      double var8 = var4 + 1;
      HashMap var10 = new HashMap();
      HashMap var11 = new HashMap();
      ArrayDeque var12 = new ArrayDeque();
      var10.put(var3, var3);
      var11.put(var3, 0);
      var12.add(var3);
      int var13 = 0;

      while (!var12.isEmpty()) {
         int var14 = (Integer)var12.poll();
         if (var0.height(var14) <= var4 && var13 < 24) {
            var13++;
            if (b(var0, var1, var2, var14, var4, var5, var7)) {
               LinkedList var22 = new LinkedList();

               for (int var23 = var14; var23 != var3; var23 = (Integer)var10.get(var23)) {
                  var22.addFirst(var23);
               }

               return var22;
            }
         }

         int var15 = (Integer)var11.get(var14);
         if (var15 < 4) {
            int var16 = var0.gi(var14);
            int var17 = var0.gj(var14);

            for (int var18 = 0; var18 < 8; var18++) {
               int var19 = var16 + b[var18];
               int var20 = var17 + c[var18];
               if (var0.valid(var19, var20)) {
                  int var21 = var0.idx(var19, var20);
                  if (!var10.containsKey(var21) && !(var0.height(var21) > var8)) {
                     var10.put(var21, var14);
                     var11.put(var21, var15 + 1);
                     var12.add(var21);
                  }
               }
            }
         }
      }

      return null;
   }

   private static boolean b(LowResGrid var0, TerrainSampler var1, StreamsConfig var2, int var3, int var4, double var5, int[] var7) {
      int var8 = Math.round(var0.worldX(var0.gi(var3)));
      int var9 = Math.round(var0.worldZ(var0.gj(var3)));
      String var10 = var2.outletBiomes();
      if (var10 != null && !var10.isBlank() && !var1.outletBiomeOk(var8, (int)Math.round(var5), var9)) {
         var7[7]++;
         return false;
      } else {
         double var11 = var1.groundHeightExact(var8, var9);
         if (var11 > var4 - 1) {
            var7[8]++;
            return false;
         } else if (var1.surfaceHeight(var8, var9) - var11 < 0.99) {
            var7[9]++;
            return false;
         } else {
            return true;
         }
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, LowResGrid var1, int var2, TerrainSampler var3, StreamsConfig var4) {
      int var5 = var0.size();
      if (var5 >= 2 && var2 >= 0) {
         NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var5 - 1);
         int var7 = Math.round(var1.worldX(var1.gi(var2)));
         int var8 = Math.round(var1.worldZ(var1.gj(var2)));
         double var9 = var3.riverSurface(var7, var8);
         boolean var11 = !Double.isNaN(var9);
         if (!var11) {
            var9 = var3.surfaceHeight(var7, var8);
            if (var9 - var3.groundHeightExact(var7, var8) < 0.99) {
               if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                  Constants.LOG
                     .info(
                        "[SR-RTF] river mouth at {},{}: DRY (surf={} ground={}) -> chain dropped",
                        new Object[]{var7, var8, var9, var3.groundHeightExact(var7, var8)}
                     );
               }

               return var0;
            }
         }

         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-RTF] river mouth at {},{}: surf={} ({}) -> accepted", new Object[]{var7, var8, var9, var11 ? "generator" : "placed water"});
         }

         double var12 = Math.min(var6.y(), var9 + RiverEngine.insetFor(b(var6.acc(), var4), var4));
         ArrayList var14 = new ArrayList(var0);
         var14.add(new NetworkTracer.Pt(var7, var8, var12, var6.acc()));
         return var11 ? b(var14, var12, 8.0) : a(var14, var12, 8.0, var3);
      } else {
         return var0;
      }
   }

   private static double a(LowResGrid var0, TerrainSampler var1, int var2, int var3) {
      double var4 = var0.height(var2);
      if (var4 == -1001.0) {
         return var1.surfaceHeight(Math.round(var0.worldX(var0.gi(var2))), Math.round(var0.worldZ(var0.gj(var2))));
      }

      if (var4 != -1000.0) {
         return var4;
      }

      double var6 = var1.estimate(Math.round(var0.worldX(var0.gi(var2))), Math.round(var0.worldZ(var0.gj(var2))));
      return Math.max(var6, var3 - 24);
   }

   private static boolean b(LowResGrid var0, TerrainSampler var1, int var2, int var3) {
      int var4 = Math.round(var0.worldX(var0.gi(var2)));
      int var5 = Math.round(var0.worldZ(var0.gj(var2)));
      boolean var6 = var1.surfaceHeight(var4, var5) - var1.groundHeightExact(var4, var5) >= 0.99;
      if (!var6 && Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info("[SR-MOUTH] final connection check failed at {},{} -> chain dropped", var4, var5);
      }

      return var6;
   }

   private static AtomicIntegerArray a(LowResGrid var0) {
      return cc.computeIfAbsent(var0, var0x -> new AtomicIntegerArray(7));
   }

   private static double a(LowResGrid var0, TerrainSampler var1, int var2, NetworkTracer.SeaEndReads var3) {
      Double var4 = var3.a.get(var2);
      if (var4 != null) {
         return var4;
      }

      int var5 = var0.worldX(var0.gi(var2));
      int var6 = var0.worldZ(var0.gj(var2));
      double var7 = var1.groundHeightExact(var5, var6);
      if (!(var7 > -999.0)) {
         var7 = var1.groundHeightReal(var5, var6);
         var3.b++;
      } else {
         var3.c++;
      }

      var3.a.put(var2, var7);
      return var7;
   }

   private static void a(LowResGrid var0, TerrainSampler var1, int var2, int var3, NetworkTracer.SeaEndReads var4) {
      StringBuilder var5 = new StringBuilder();

      for (Entry var7 : var4.a.entrySet()) {
         var5.append(
            String.format(
               Locale.ROOT, " (%d,%d)=%.0f", var0.worldX(var0.gi((Integer)var7.getKey())), var0.worldZ(var0.gj((Integer)var7.getKey())), var7.getValue()
            )
         );
      }

      int var14 = var0.worldX(var0.gi(var2));
      int var15 = var0.worldZ(var0.gj(var2));
      String var8 = ">" + 6 * var0.spacing;

      for (int var9 = 1; var9 <= 6 && var8.startsWith(">"); var9++) {
         for (int var10 = -var9; var10 <= var9 && var8.startsWith(">"); var10++) {
            for (int var11 = -var9; var11 <= var9; var11++) {
               if (Math.max(Math.abs(var11), Math.abs(var10)) == var9) {
                  int var12 = var14 + var11 * var0.spacing;
                  int var13 = var15 + var10 * var0.spacing;
                  if (var1.groundHeightReal(var12, var13) <= var3 - 2) {
                     var8 = String.format(Locale.ROOT, "%.0f at (%d,%d)", Math.hypot(var11, var10) * var0.spacing, var12, var13);
                     break;
                  }
               }
            }
         }
      }

      Constants.LOG.info("[SR-SEAEND] dropped sea end at ({},{}): read{} | nearest sea-{} {}", new Object[]{var14, var15, var5, 2, var8});
   }

   private static boolean b(LowResGrid var0, TerrainSampler var1, int var2, int var3, NetworkTracer.SeaEndReads var4) {
      double var5 = a(var0, var1, var2, var4);
      return var5 > var3 - 2 ? false : var1.surfaceHeight(var0.worldX(var0.gi(var2)), var0.worldZ(var0.gj(var2))) - var5 >= 0.99;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, double var1, double var3, TerrainSampler var5, int var6, NetworkTracer.SeaEndReads var7) {
      int var8 = var0.size();
      NetworkTracer.Pt var9 = (NetworkTracer.Pt)var0.get(var8 - 1);
      NetworkTracer.Pt var10 = (NetworkTracer.Pt)var0.get(var8 - 2);
      double var11 = var9.x() - var10.x();
      double var13 = var9.z() - var10.z();
      double var15 = Math.hypot(var11, var13);
      if (var15 < 1.0E-6) {
         return var0;
      }

      double var17 = 0.0;

      for (double var19 = 4.0; var19 <= var3 + 1.0E-9; var19 += 4.0) {
         int var21 = (int)Math.floor(var9.x() + var11 / var15 * var19);
         int var22 = (int)Math.floor(var9.z() + var13 / var15 * var19);
         double var23 = var5.groundHeightExact(var21, var22);
         if (!(var23 > -999.0)) {
            var23 = var5.groundHeightReal(var21, var22);
            var7.b++;
         } else {
            var7.c++;
         }

         if (var23 > var6 - 2 || var5.surfaceHeight(var21, var22) - var23 < 0.99) {
            break;
         }

         var17 = var19;
      }

      return var17 <= 0.0 ? var0 : b(var0, var1, var17);
   }

   private static List<Integer> a(LowResGrid var0, TerrainSampler var1, int var2, int var3, List<NetworkTracer.Pt> var4, NetworkTracer.SeaEndReads var5) {
      ArrayList var6 = new ArrayList(3);
      HashSet var7 = new HashSet();
      var7.add(var2);

      for (NetworkTracer.Pt var9 : var4) {
         int var10 = (int)Math.round((var9.x() - var0.originX) / var0.spacing);
         int var11 = (int)Math.round((var9.z() - var0.originZ) / var0.spacing);
         if (var0.valid(var10, var11)) {
            var7.add(var0.idx(var10, var11));
         }
      }

      int var35 = var2;
      NetworkTracer.Pt var36 = (NetworkTracer.Pt)var4.get(var4.size() - 2);
      NetworkTracer.Pt var37 = (NetworkTracer.Pt)var4.get(var4.size() - 1);
      double var38 = Math.hypot(var37.x() - var36.x(), var37.z() - var36.z());
      double var13 = var38 > 1.0E-9 ? (var37.x() - var36.x()) / var38 : 0.0;
      double var15 = var38 > 1.0E-9 ? (var37.z() - var36.z()) / var38 : 0.0;

      for (int var17 = 0; var17 < 3; var17++) {
         int var18 = var0.gi(var35);
         int var19 = var0.gj(var35);
         int var20 = -1;
         double var21 = Double.POSITIVE_INFINITY;
         double var23 = Double.NEGATIVE_INFINITY;

         for (int var25 = 0; var25 < 8; var25++) {
            int var26 = var18 + b[var25];
            int var27 = var19 + c[var25];
            if (var0.valid(var26, var27)) {
               int var28 = var0.idx(var26, var27);
               if (!var7.contains(var28)) {
                  double var29 = var0.height(var28);
                  if (!var0.inDomain(var28) || !(var29 > var3 + 1)) {
                     double var31 = a(var0, var1, var28, var5);
                     if (!(var31 > var3 + 1)) {
                        double var33 = (b[var25] * var13 + c[var25] * var15) / Math.hypot(b[var25], c[var25]);
                        if (var31 < var21 || var31 == var21 && var33 > var23) {
                           var20 = var28;
                           var21 = var31;
                           var23 = var33;
                        }
                     }
                  }
               }
            }
         }

         if (var20 < 0) {
            return null;
         }

         double var39 = var0.gi(var20) - var18;
         double var40 = var0.gj(var20) - var19;
         double var41 = Math.hypot(var39, var40);
         var13 = var39 / var41;
         var15 = var40 / var41;
         var7.add(var20);
         var6.add(var20);
         var35 = var20;
         if (b(var0, var1, var35, var3, var5)) {
            return var6;
         }
      }

      return null;
   }

   private static List<Integer> a(LowResGrid var0, TerrainSampler var1, int var2, double var3, double var5, int var7, int var8) {
      HashMap var9 = new HashMap();
      HashMap var10 = new HashMap();
      ArrayDeque var11 = new ArrayDeque();
      var9.put(var2, var2);
      var10.put(var2, 0);
      var11.add(var2);

      while (!var11.isEmpty()) {
         int var12 = (Integer)var11.poll();
         if (var12 != var2 && a(var0, var1, var12, var8) <= var5) {
            LinkedList var20 = new LinkedList();

            for (int var21 = var12; var21 != var2; var21 = (Integer)var9.get(var21)) {
               var20.addFirst(var21);
            }

            return var20;
         }

         int var13 = (Integer)var10.get(var12);
         if (var13 < var7) {
            int var14 = var0.gi(var12);
            int var15 = var0.gj(var12);

            for (int var16 = 0; var16 < 8; var16++) {
               int var17 = var14 + b[var16];
               int var18 = var15 + c[var16];
               if (var0.valid(var17, var18)) {
                  int var19 = var0.idx(var17, var18);
                  if (!var9.containsKey(var19) && !(a(var0, var1, var19, var8) > var3)) {
                     var9.put(var19, var12);
                     var10.put(var19, var13 + 1);
                     var11.add(var19);
                  }
               }
            }
         }
      }

      return null;
   }

   private static boolean a(List<Integer> var0, LowResGrid var1, TerrainSampler var2, int var3) {
      for (int var5 : var0) {
         if (var2.groundHeight(var1.worldX(var1.gi(var5)), var1.worldZ(var1.gj(var5))) > var3 + 24.0) {
            return true;
         }
      }

      return false;
   }

   private static List<Integer> a(LowResGrid var0, int var1, double var2, double var4, int var6) {
      HashMap var7 = new HashMap();
      HashMap var8 = new HashMap();
      ArrayDeque var9 = new ArrayDeque();
      var7.put(var1, var1);
      var8.put(var1, 0);
      var9.add(var1);

      while (!var9.isEmpty()) {
         int var10 = (Integer)var9.poll();
         if (var0.height(var10) <= var4) {
            LinkedList var18 = new LinkedList();

            for (int var19 = var10; var19 != var1; var19 = (Integer)var7.get(var19)) {
               var18.addFirst(var19);
            }

            return var18;
         }

         int var11 = (Integer)var8.get(var10);
         if (var11 < var6) {
            int var12 = var0.gi(var10);
            int var13 = var0.gj(var10);

            for (int var14 = 0; var14 < 8; var14++) {
               int var15 = var12 + b[var14];
               int var16 = var13 + c[var14];
               if (var0.valid(var15, var16)) {
                  int var17 = var0.idx(var15, var16);
                  if (!var7.containsKey(var17) && !(var0.height(var17) > var2)) {
                     var7.put(var17, var10);
                     var8.put(var17, var11 + 1);
                     var9.add(var17);
                  }
               }
            }
         }
      }

      return null;
   }

   private static List<NetworkTracer.Pt> b(List<NetworkTracer.Pt> var0, double var1, double var3) {
      int var5 = var0.size();
      if (var5 < 2) {
         return var0;
      }

      NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var5 - 1);
      NetworkTracer.Pt var7 = (NetworkTracer.Pt)var0.get(var5 - 2);
      double var8 = var6.x() - var7.x();
      double var10 = var6.z() - var7.z();
      double var12 = Math.hypot(var8, var10);
      if (var12 < 1.0E-6) {
         return var0;
      }

      double var14 = Math.min(var6.y(), var1);
      ArrayList var16 = new ArrayList(var0);
      var16.add(new NetworkTracer.Pt(var6.x() + var8 / var12 * var3, var6.z() + var10 / var12 * var3, var14, var6.acc()));
      return var16;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, double var1, double var3, TerrainSampler var5) {
      int var6 = var0.size();
      if (var6 < 2) {
         return var0;
      }

      NetworkTracer.Pt var7 = (NetworkTracer.Pt)var0.get(var6 - 1);
      NetworkTracer.Pt var8 = (NetworkTracer.Pt)var0.get(var6 - 2);
      double var9 = var7.x() - var8.x();
      double var11 = var7.z() - var8.z();
      double var13 = Math.hypot(var9, var11);
      if (var13 < 1.0E-6) {
         return var0;
      }

      double var15 = 0.0;

      for (double var17 = 4.0; var17 <= var3 + 1.0E-9 && a(var5, var7.x() + var9 / var13 * var17, var7.z() + var11 / var13 * var17); var17 += 4.0) {
         var15 = var17;
      }

      return var15 <= 0.0 ? var0 : b(var0, var1, var15);
   }

   public static RiverNetwork connectNetwork(
      PlateKey var0, Function<PlateKey, RiverNetwork> var1, PlateLattice var2, TerrainSampler var3, StreamsConfig var4, long var5
   ) {
      RiverNetwork var7 = (RiverNetwork)var1.apply(var0);
      double var8 = var4.connectRadius();
      int var10 = var4.seaLevel();
      ArrayList var11 = new ArrayList();
      HashSet var12 = new HashSet();
      HashMap var13 = new HashMap();
      var13.put(var0, var7);

      for (RiverSegment var15 : var7.segments()) {
         if (a(var15, var10)) {
            double var16 = var15.x1();
            double var18 = var15.z1();
            double var20 = var15.y1();
            PlateKey[] var22 = var2.platesWithin(var16, var18, var8);

            for (PlateKey var26 : var22) {
               var13.computeIfAbsent(var26, var1);
            }

            NetworkTracer.Connector var30 = a(var16, var18, var20, var22, var13, var3, var4, var10, var8);
            if (var30 != null) {
               List var31 = a(a(a(var30.path(), var4.gridSpacing() * 0.4), var5, d(var16, var18), var4), var4.smoothingPasses());
               a(var31, var30.wStart(), var30.wEnd(), var11);
               var12.add(var15);
            }
         }
      }

      if (var11.isEmpty()) {
         return var7;
      }

      ArrayList var27 = new ArrayList(var7.segments().size() + var11.size());

      for (RiverSegment var29 : var7.segments()) {
         if (!var12.contains(var29)) {
            var27.add(var29);
         }
      }

      var27.addAll(var11);
      return assemble(var0, var27, var7.lakes(), a(var7.sinks(), var27), var7.relief(), var4);
   }

   private static boolean a(RiverSegment var0, int var1) {
      return var0.x1() == var0.x2() && var0.z1() == var0.z2() && var0.y1() > var1 + 1;
   }

   private static NetworkTracer.Connector a(
      double var0,
      double var2,
      double var4,
      PlateKey[] var6,
      Map<PlateKey, RiverNetwork> var7,
      TerrainSampler var8,
      StreamsConfig var9,
      int var10,
      double var11
   ) {
      double[] var13 = a(var0, var2, var6, var7, var10);
      double var14 = Double.MAX_VALUE;
      double var16 = 0.0;
      double var18 = 0.0;
      double var20 = 0.0;
      double var22 = 0.0;

      for (PlateKey var27 : var6) {
         RiverNetwork var28 = (RiverNetwork)var7.get(var27);
         if (var28 != null) {
            for (RiverSegment var30 : var28.segments()) {
               if (!a(var30, var10)) {
                  double var31 = var30.clampParam(var0, var2);
                  double var33 = var30.x1() + (var30.x2() - var30.x1()) * var31;
                  double var35 = var30.z1() + (var30.z2() - var30.z1()) * var31;
                  double var37 = Math.hypot(var33 - var0, var35 - var2);
                  if (!(var37 < 3.0) && !(var37 > var11) && !(var37 >= var14)) {
                     double var39 = var30.y1() + (var30.y2() - var30.y1()) * var31;
                     if (!(var39 > var4) && a(var13, var33 - var0, var35 - var2)) {
                        var14 = var37;
                        var16 = var33;
                        var18 = var35;
                        var20 = var39;
                        var22 = var30.widthAt(var31);
                     }
                  }
               }
            }
         }
      }

      double[] var41 = a(var0, var2, var13, var8, var9, var10, var11);
      if (var41 != null && var41[3] < var14) {
         var16 = var41[0];
         var18 = var41[1];
         var20 = var10;
         var22 = 0.0;
         var14 = var41[3];
      }

      if (var14 == Double.MAX_VALUE) {
         return null;
      }

      if (a(var0, var2, var16, var18, var4, var8) > 4.0) {
         return null;
      }

      double var42 = b(var0, var2, var6, var7, var10);
      double var43 = var22 > 0.0 ? var22 : var42;
      ArrayList var44 = new ArrayList(2);
      var44.add(new NetworkTracer.Pt(var0, var2, var4, 0.0));
      var44.add(new NetworkTracer.Pt(var16, var18, var20, 0.0));
      return new NetworkTracer.Connector(var44, var42, var43);
   }

   private static double[] a(double var0, double var2, PlateKey[] var4, Map<PlateKey, RiverNetwork> var5, int var6) {
      double var7 = Double.MAX_VALUE;
      double var9 = 0.0;
      double var11 = 0.0;

      for (PlateKey var16 : var4) {
         RiverNetwork var17 = (RiverNetwork)var5.get(var16);
         if (var17 != null) {
            for (RiverSegment var19 : var17.segments()) {
               if (!a(var19, var6)) {
                  double var20 = Math.hypot(var19.x2() - var0, var19.z2() - var2);
                  double var22 = Math.hypot(var19.x2() - var19.x1(), var19.z2() - var19.z1());
                  if (var20 < var7 && var22 > 1.0E-6) {
                     var7 = var20;
                     var9 = (var19.x2() - var19.x1()) / var22;
                     var11 = (var19.z2() - var19.z1()) / var22;
                  }
               }
            }
         }
      }

      return var7 <= 3.0 ? new double[]{var9, var11} : null;
   }

   private static double b(double var0, double var2, PlateKey[] var4, Map<PlateKey, RiverNetwork> var5, int var6) {
      double var7 = Double.MAX_VALUE;
      double var9 = -1.0;

      for (PlateKey var14 : var4) {
         RiverNetwork var15 = (RiverNetwork)var5.get(var14);
         if (var15 != null) {
            for (RiverSegment var17 : var15.segments()) {
               if (!a(var17, var6)) {
                  double var18 = Math.hypot(var17.x2() - var0, var17.z2() - var2);
                  if (var18 < var7) {
                     var7 = var18;
                     var9 = var17.w2();
                  }
               }
            }
         }
      }

      return var9 > 0.0 ? var9 : 0.5;
   }

   private static boolean a(double[] var0, double var1, double var3) {
      return var0 == null ? true : var0[0] * var1 + var0[1] * var3 >= 0.0;
   }

   private static double[] a(double var0, double var2, double[] var4, TerrainSampler var5, StreamsConfig var6, int var7, double var8) {
      int var10 = Math.max(4, var6.gridSpacing() / 2);
      double var11 = Double.MAX_VALUE;
      double var13 = 0.0;
      double var15 = 0.0;

      for (int var17 = 0; var17 < 16; var17++) {
         double var18 = var17 * (Math.PI / 8);
         double var20 = Math.cos(var18);
         double var22 = Math.sin(var18);
         if (a(var4, var20, var22)) {
            for (double var24 = 3.0; var24 <= var8; var24 += var10) {
               double var26 = var0 + var20 * var24;
               double var28 = var2 + var22 * var24;
               if (b(var5, var26, var28) <= var7) {
                  if (var24 < var11) {
                     var11 = var24;
                     var13 = var26;
                     var15 = var28;
                  }
                  break;
               }
            }
         }
      }

      return var11 == Double.MAX_VALUE ? null : new double[]{var13, var15, var7, var11};
   }

   private static double a(double var0, double var2, double var4, double var6, double var8, TerrainSampler var10) {
      double var11 = Math.hypot(var4 - var0, var6 - var2);
      int var13 = Math.max(1, (int)Math.round(var11 / 4.0));
      double var14 = 0.0;

      for (int var16 = 1; var16 < var13; var16++) {
         double var17 = (double)var16 / var13;
         double var19 = b(var10, var0 + (var4 - var0) * var17, var2 + (var6 - var2) * var17);
         var14 = Math.max(var14, var19 - var8);
      }

      return var14;
   }

   private static void a(List<NetworkTracer.Pt> var0, double var1, double var3, List<RiverSegment> var5) {
      double var6 = 0.0;

      for (int var8 = 0; var8 < var0.size() - 1; var8++) {
         var6 += Math.hypot(
            ((NetworkTracer.Pt)var0.get(var8 + 1)).x() - ((NetworkTracer.Pt)var0.get(var8)).x(),
            ((NetworkTracer.Pt)var0.get(var8 + 1)).z() - ((NetworkTracer.Pt)var0.get(var8)).z()
         );
      }

      double var21 = 0.0;

      for (int var10 = 0; var10 < var0.size() - 1; var10++) {
         NetworkTracer.Pt var11 = (NetworkTracer.Pt)var0.get(var10);
         NetworkTracer.Pt var12 = (NetworkTracer.Pt)var0.get(var10 + 1);
         double var13 = Math.hypot(var12.x() - var11.x(), var12.z() - var11.z());
         double var15 = var6 > 1.0E-9 ? var1 + (var3 - var1) * (var21 / var6) : var1;
         double var17 = var6 > 1.0E-9 ? var1 + (var3 - var1) * ((var21 + var13) / var6) : var3;
         double var19 = FlowSpeed.forReach(var13 > 1.0E-9 ? Math.abs(var11.y() - var12.y()) / var13 : 0.0, 1.0, 1.0);
         var5.add(new RiverSegment(var11.x(), var11.z(), var11.y(), var15, var12.x(), var12.z(), var12.y(), var17, var19, var19));
         var21 += var13;
      }
   }

   private static int d(double var0, double var2) {
      return (int)(Math.round(var0) * 73856093L ^ Math.round(var2) * 19349663L);
   }

   private static boolean b(Lake var0, double var1) {
      int var3 = 0;

      for (long var7 : var0.mask()) {
         var3 += Long.bitCount(var7);
      }

      double var9 = (double)var3 * var0.spacing() * var0.spacing();
      return Math.sqrt(var9) >= 2.0 * (2.0 * var1);
   }

   private static double b(double var0, StreamsConfig var2) {
      double var3 = var0 * var2.gridSpacing() * var2.gridSpacing();
      return Math.min(var2.maxWidth(), var2.baseWidth() + var2.widthScale() * Math.sqrt(var3 / 2500.0));
   }

   private record Connector(List<NetworkTracer.Pt> path, double wStart, double wEnd) {

      private Connector {
wStart = wStart;
}
   }

   private record LocalBasin(int i0, int j0, int w, int h, double[] hgt, boolean[] ok, int fb, double spill, double floor) {

      private LocalBasin {
j0 = j0;
h = h;
ok = ok;
spill = spill;
}

      List<Integer> poolAt(int var1, LowResGrid var2) {
         if (this.hgt[this.fb] >= var1) {
            return null;
         }

         ArrayList var3 = new ArrayList();
         boolean[] var4 = new boolean[this.w * this.h];
         ArrayDeque var5 = new ArrayDeque();
         var4[this.fb] = true;
         var5.add(this.fb);

         while (!var5.isEmpty()) {
            int var6 = (Integer)var5.poll();
            int var7 = var6 % this.w;
            int var8 = var6 / this.w;
            if (var7 == 0 || var8 == 0 || var7 == this.w - 1 || var8 == this.h - 1) {
               return null;
            }

            var3.add(var2.idx(this.i0 + var7, this.j0 + var8));

            for (int var9 = 0; var9 < 8; var9++) {
               int var10 = var7 + NetworkTracer.b[var9];
               int var11 = var8 + NetworkTracer.c[var9];
               int var12 = var11 * this.w + var10;
               if (var10 >= 0 && var10 < this.w && var11 >= 0 && var11 < this.h && this.ok[var12] && !var4[var12] && this.hgt[var12] < var1 - 1.0E-6) {
                  var4[var12] = true;
                  var5.add(var12);
               }
            }
         }

         return var3.isEmpty() ? null : var3;
      }
   }

   private record PendingOutflow(int outlet, double startLevel, double acc) {

      private PendingOutflow {
startLevel = startLevel;
}
   }

   private record Pt(double x, double z, double y, double acc) {

      private Pt {
z = z;
acc = acc;
}
   }

   private static final class SeaEndReads {
      final Map<Integer, Double> a = new HashMap<>();
      int b;
      int c;
   }
}
