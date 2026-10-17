package dev.streamsreflowing.core.grid;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.diag.HeightCensus;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class LowResGrid {
   public final int nx;
   public final int nz;
   public final int spacing;
   public final int originX;
   public final int originZ;
   private final double[] a;
   private final boolean[] b;
   private static final int c = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
   private static final AtomicLong d = new AtomicLong();
   private static final ThreadPoolExecutor e = new ThreadPoolExecutor(c, c, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), var0 -> {
      Thread var1 = new Thread(var0, "streamsreflowing-sampler");
      var1.setDaemon(true);
      var1.setPriority(1);
      return var1;
   });
   private static final int f = 4096;
   private static final boolean g = !Boolean.getBoolean("streamsreflowing.gpuquiet");
   private static final long h = 5000000000L;
   private static volatile long i = System.nanoTime();
   private static final AtomicInteger j = new AtomicInteger();
   private static volatile int[] k = new int[0];
   private static final ThreadLocal<long[]> l = ThreadLocal.withInitial(() -> new long[]{Long.MAX_VALUE});
   private static final Object m = new Object();
   private static final boolean n = Boolean.getBoolean("streamsreflowing.caldiag");
   private static final ThreadLocal<Boolean> o = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static final AtomicInteger p = new AtomicInteger();
   private static final double q = Double.parseDouble(System.getProperty("streamsreflowing.adaptivebend", "4.0"));
   private static final double r = 8.0;
   private static final double s = 1.0;
   private long[] t;

   public static void submitSamplerTask(Runnable var0) {
      e.execute(new LowResGrid.PrioritizedTask(b(), l.get()[0], d.getAndIncrement(), var0));
   }

   public static void setBackgroundThrottle(int var0) {
      if (j.get() <= 0 && p.get() <= 0) {
         int var1 = var0 >= 4 ? c : (var0 == 3 ? Math.max(2, c * 3 / 4) : (var0 == 2 ? Math.max(2, c / 3) : (var0 == 1 ? Math.max(2, c / 5) : 1)));
         if (var1 >= e.getCorePoolSize() || System.nanoTime() - i >= 5000000000L) {
            a(var1);
         }
      } else {
         a(c);
      }
   }

   public static boolean noteUrgentWaiter() {
      if (Thread.currentThread().getName().startsWith("streamsreflowing-prefetch") && !o.get()) {
         return false;
      }

      j.incrementAndGet();
      a(c);
      return true;
   }

   public static void urgentWaiterDone() {
      a();
   }

   private static void a() {
      int var0 = j.decrementAndGet();
      if (var0 < 0) {
         j.compareAndSet(var0, 0);
         if (n) {
            Constants.LOG.warn("[SR-POOL] urgent waiter count went negative (from {})", Thread.currentThread().getName());
         }
      }
   }

   public static void noteDeferredDemand() {
      j.incrementAndGet();
      a(c);
   }

   public static void deferredDemandDone() {
      a();
   }

   public static int urgentWaiterCount() {
      return j.get();
   }

   public static void setFocusPoints(int[] var0) {
      k = var0 != null ? var0 : new int[0];
   }

   public static long focusDistanceToBoxSq(int var0, int var1, int var2, int var3) {
      int[] var4 = k;
      long var5 = Long.MAX_VALUE;

      for (int var7 = 0; var7 + 1 < var4.length; var7 += 2) {
         long var8 = var4[var7] < var0 ? (long)var0 - var4[var7] : (var4[var7] > var2 ? (long)var4[var7] - var2 : 0L);
         long var10 = var4[var7 + 1] < var1 ? (long)var1 - var4[var7 + 1] : (var4[var7 + 1] > var3 ? (long)var4[var7 + 1] - var3 : 0L);
         long var12 = var8 * var8 + var10 * var10;
         if (var12 < var5) {
            var5 = var12;
         }
      }

      return var5;
   }

   public static long focusDistanceSq(int var0, int var1) {
      return a(var0, var1);
   }

   private static long a(int var0, int var1) {
      int[] var2 = k;
      long var3 = Long.MAX_VALUE;

      for (int var5 = 0; var5 + 1 < var2.length; var5 += 2) {
         long var6 = var2[var5] - var0;
         long var8 = var2[var5 + 1] - var1;
         long var10 = var6 * var6 + var8 * var8;
         if (var10 < var3) {
            var3 = var10;
         }
      }

      return var3;
   }

   public static void enterBuildFocus(int var0, int var1) {
      l.get()[0] = a(var0, var1);
   }

   public static void exitBuildFocus() {
      l.get()[0] = Long.MAX_VALUE;
   }

   public static int samplerWidth() {
      return e.getCorePoolSize();
   }

   public static int samplerFullWidth() {
      return c;
   }

   private static void a(int var0) {
      synchronized (m) {
         b(var0);
      }
   }

   private static void b(int var0) {
      int var1 = e.getCorePoolSize();
      if (n && var1 != var0) {
         Constants.LOG.info("[SR-POOL] samplers {} -> {} (urgent waiters {}, from {})", new Object[]{var1, var0, j.get(), Thread.currentThread().getName()});
      }

      if (e.getCorePoolSize() != var0 || e.getMaximumPoolSize() != var0) {
         if (var0 >= e.getMaximumPoolSize()) {
            e.setMaximumPoolSize(var0);
            e.setCorePoolSize(var0);
            i = System.nanoTime();
         } else {
            e.setCorePoolSize(var0);
            e.setMaximumPoolSize(var0);
         }
      }
   }

   private static int b() {
      if (o.get()) {
         return 0;
      } else {
         return Thread.currentThread().getName().startsWith("streamsreflowing-prefetch") ? 1 : 0;
      }
   }

   public static void setUrgentBuild(boolean var0) {
      boolean var1 = o.get();
      o.set(var0);
      if (var0 && !var1) {
         p.incrementAndGet();
      } else if (!var0 && var1) {
         p.decrementAndGet();
      }
   }

   public static boolean isUrgentBuild() {
      return o.get();
   }

   public LowResGrid(PlateDomain var1, TerrainSampler var2, StreamsConfig var3) {
      this.spacing = var3.gridSpacing();
      int var4 = !var3.legacyShaping() && !Boolean.getBoolean("streamsreflowing.legacyshaping") ? Math.max(this.spacing, var3.terrainSampleSpacing()) : 1;
      this.originX = Math.floorDiv(var1.minX, var4) * var4;
      this.originZ = Math.floorDiv(var1.minZ, var4) * var4;
      this.nx = Math.max(1, (var1.maxX - this.originX) / this.spacing + 1);
      this.nz = Math.max(1, (var1.maxZ - this.originZ) / this.spacing + 1);
      this.a = new double[this.nx * this.nz];
      this.b = new boolean[this.nx * this.nz];
      int var5 = Math.max(this.spacing, var3.terrainSampleSpacing());
      if (g) {
         Constants.LOG
            .info(
               "[SR-FILL] {}x{} cells, spacing {}, sampleStep {} -> {} | batchCapable={} hybridFill={}",
               new Object[]{
                  this.nx,
                  this.nz,
                  this.spacing,
                  var5,
                  var5 > this.spacing ? "interpolated" : (var2.batchCapable() ? "batched" : (this.nx * this.nz >= 4096 ? "parallel" : "serial")),
                  var2.batchCapable(),
                  var2.useHybridGridFill()
               }
            );
      }

      int var6 = HeightCensus.enter(var5 > this.spacing ? 1 : 2);

      try {
         if (var5 > this.spacing) {
            this.a(var1, var2, var3, var5);
         } else if (var2.batchCapable()) {
            this.a(var1, var2, var3);
         } else if (this.nx * this.nz >= 4096) {
            this.b(var1, var2, var3);
         } else {
            this.a(0, this.nz, var1, var2, var3);
         }
      } finally {
         HeightCensus.restore(var6);
      }
   }

   private void a(PlateDomain var1, TerrainSampler var2, StreamsConfig var3) {
      int[] var4 = new int[this.nx * this.nz];
      int[] var5 = new int[this.nx * this.nz];
      int[] var6 = new int[this.nx * this.nz];
      int var7 = 0;

      for (int var8 = 0; var8 < this.nz; var8++) {
         for (int var9 = 0; var9 < this.nx; var9++) {
            int var10 = this.worldX(var9);
            int var11 = this.worldZ(var8);
            int var12 = this.idx(var9, var8);
            boolean var13 = var1.contains(var10, var11) && var1.borderDistance(var10, var11) >= var3.borderMargin();
            this.b[var12] = var13;
            if (var13) {
               var4[var7] = var10;
               var5[var7] = var11;
               var6[var7] = var12;
               var7++;
            }
         }
      }

      double[] var14 = new double[var7];
      var2.heightsBatch(var4, var5, var14, var7);

      for (int var15 = 0; var15 < var7; var15++) {
         this.a[var6[var15]] = var14[var15];
      }
   }

   private void a(int var1, int var2, PlateDomain var3, TerrainSampler var4, StreamsConfig var5) {
      for (int var6 = var1; var6 < var2; var6++) {
         for (int var7 = 0; var7 < this.nx; var7++) {
            int var8 = this.worldX(var7);
            int var9 = this.worldZ(var6);
            int var10 = this.idx(var7, var6);
            boolean var11 = var3.contains(var8, var9) && var3.borderDistance(var8, var9) >= var5.borderMargin();
            this.b[var10] = var11;
            if (var11) {
               this.a[var10] = var4.height(var8, var9);
            }
         }
      }
   }

   private void b(PlateDomain var1, TerrainSampler var2, StreamsConfig var3) {
      a(this.nz, (var4, var5) -> this.a(var4, var5, var1, var2, var3));
   }

   private void a(PlateDomain var1, TerrainSampler var2, StreamsConfig var3, int var4) {
      int var5 = Math.floorDiv(this.originX, var4) * var4;
      int var6 = Math.floorDiv(this.originZ, var4) * var4;
      int var7 = (Math.floorDiv(this.worldX(this.nx - 1), var4) * var4 + var4 - var5) / var4 + 1;
      int var8 = (Math.floorDiv(this.worldZ(this.nz - 1), var4) * var4 + var4 - var6) / var4 + 1;
      double[] var9 = new double[var7 * var8];
      boolean[] var10 = new boolean[var7 * var8];
      a(this.nz, (var8x, var9x) -> {
         for (int var10x = var8x; var10x < var9x; var10x++) {
            for (int var11x = 0; var11x < this.nx; var11x++) {
               int var12 = this.worldX(var11x);
               int var13 = this.worldZ(var10x);
               boolean var14 = var1.contains(var12, var13) && var1.borderDistance(var12, var13) >= var3.borderMargin();
               this.b[this.idx(var11x, var10x)] = var14;
               if (var14) {
                  int var15x = (Math.floorDiv(var12, var4) * var4 - var5) / var4;
                  int var16x = (Math.floorDiv(var13, var4) * var4 - var6) / var4;
                  int var17x = var15x + var16x * var7;
                  var10[var17x] = true;
                  var10[var17x + 1] = true;
                  var10[var17x + var7] = true;
                  var10[var17x + var7 + 1] = true;
               }
            }
         }
      });
      if (g) {
         int var11 = 0;

         for (boolean var15 : var10) {
            if (var15) {
               var11++;
            }
         }

         Constants.LOG
            .info(
               "[SR-FILL] coarse {}x{} step {}, {} points needed -> {}",
               new Object[]{var7, var8, var4, var11, var2.useHybridGridFill() ? "hybrid (no batch)" : (var2.batchCapable() ? "batched" : "banded CPU")}
            );
      }

      if (var2.useHybridGridFill()) {
         this.a(var2, var3, var4, var5, var6, var7, var8, var9, var10);
      } else if (var2.batchCapable()) {
         int[] var20 = new int[var7 * var8];
         int[] var22 = new int[var7 * var8];
         int[] var24 = new int[var7 * var8];
         int var26 = 0;

         for (int var30 = 0; var30 < var8; var30++) {
            int var16 = var6 + var30 * var4;

            for (int var17 = 0; var17 < var7; var17++) {
               int var18 = var17 + var30 * var7;
               if (var10[var18]) {
                  var20[var26] = var5 + var17 * var4;
                  var22[var26] = var16;
                  var24[var26] = var18;
                  var26++;
               }
            }
         }

         double[] var31 = new double[var26];
         var2.heightsBatch(var20, var22, var31, var26);

         for (int var35 = 0; var35 < var26; var35++) {
            var9[var24[var35]] = var31[var35];
         }
      } else {
         a(var8, (var7x, var8x) -> {
            for (int var9x = var7x; var9x < var8x; var9x++) {
               int var10x = var6 + var9x * var4;

               for (int var11x = 0; var11x < var7; var11x++) {
                  int var12 = var11x + var9x * var7;
                  if (var10[var12]) {
                     var9[var12] = var2.height(var5 + var11x * var4, var10x);
                  }
               }
            }
         });
      }

      boolean[] var21 = var3.adaptiveGridRead() ? a(var9, var10, var7, var8, q) : null;
      boolean[] var23 = new boolean[this.nx * this.nz];
      a(this.nz, (var8x, var9x) -> {
         for (int var10x = var8x; var10x < var9x; var10x++) {
            for (int var11x = 0; var11x < this.nx; var11x++) {
               int var12 = this.idx(var11x, var10x);
               if (this.b[var12]) {
                  int var13 = this.worldX(var11x);
                  int var14 = this.worldZ(var10x);
                  boolean var15x = Math.floorMod(var13, var4) == 0 && Math.floorMod(var14, var4) == 0;
                  if (var15x || !a(var13, var14, var4, var9, var7, var5, var6) && (var21 == null || !var21[a(var13, var14, var4, var7, var5, var6)])) {
                     this.a[var12] = b(var13, var14, var4, var9, var7, var5, var6);
                  } else {
                     var23[var12] = true;
                  }
               }
            }
         }
      });
      int var25 = 0;

      for (boolean var40 : var23) {
         if (var40) {
            var25++;
         }
      }

      if (var25 > 0) {
         if (var2.batchCapable()) {
            int[] var28 = new int[var25];
            int[] var33 = new int[var25];
            int[] var37 = new int[var25];
            int var41 = 0;

            for (int var42 = 0; var42 < var23.length; var42++) {
               if (var23[var42]) {
                  var28[var41] = this.worldX(var42 % this.nx);
                  var33[var41] = this.worldZ(var42 / this.nx);
                  var37[var41] = var42;
                  var41++;
               }
            }

            double[] var43 = new double[var25];
            var2.heightsBatch(var28, var33, var43, var25);

            for (int var19 = 0; var19 < var25; var19++) {
               this.a[var37[var19]] = var43[var19];
            }
         } else {
            a(this.nz, (var3x, var4x) -> {
               for (int var5x = var3x; var5x < var4x; var5x++) {
                  for (int var6x = 0; var6x < this.nx; var6x++) {
                     int var7x = this.idx(var6x, var5x);
                     if (var23[var7x]) {
                        this.a[var7x] = var2.height(this.worldX(var6x), this.worldZ(var5x));
                     }
                  }
               }
            });
         }
      }

      if (g) {
         int var29 = 0;
         int var34 = 0;

         for (int var38 = 0; var38 < this.b.length; var38++) {
            if (this.b[var38]) {
               var29++;
               if (Math.floorMod(this.worldX(var38 % this.nx), var4) == 0 && Math.floorMod(this.worldZ(var38 / this.nx), var4) == 0) {
                  var34++;
               }
            }
         }

         int var39 = Math.max(1, var29 - var34);
         Constants.LOG
            .info(
               "[SR-FILL] adaptive read {}: {} of {} blended nodes read for real ({}%)",
               new Object[]{var21 != null ? "on" : "off", var25, var39, Math.round(100.0 * var25 / var39)}
            );
      }
   }

   private static int a(int var0, int var1, int var2, int var3, int var4, int var5) {
      int var6 = (Math.floorDiv(var0, var2) * var2 - var4) / var2;
      int var7 = (Math.floorDiv(var1, var2) * var2 - var5) / var2;
      return var6 + var7 * var3;
   }

   private static boolean[] a(double[] var0, boolean[] var1, int var2, int var3, double var4) {
      double[] var6 = new double[var2 * var3];
      Arrays.fill(var6, Double.NaN);

      for (int var7 = 1; var7 < var3 - 1; var7++) {
         for (int var8 = 1; var8 < var2 - 1; var8++) {
            int var9 = var8 + var7 * var2;
            int var10 = var9 - 1;
            int var11 = var9 + 1;
            int var12 = var9 - var2;
            int var13 = var9 + var2;
            if (var1[var9] && var1[var10] && var1[var11] && var1[var12] && var1[var13]) {
               double var14 = var0[var9];
               if (var14 != -1000.0 && var0[var10] != -1000.0 && var0[var11] != -1000.0 && var0[var12] != -1000.0 && var0[var13] != -1000.0) {
                  var6[var9] = Math.abs((var0[var10] + var0[var11] + var0[var12] + var0[var13]) * 0.25 - var14);
               }
            }
         }
      }

      boolean[] var16 = new boolean[var2 * var3];

      for (int var17 = 0; var17 < var3 - 1; var17++) {
         for (int var18 = 0; var18 < var2 - 1; var18++) {
            int var19 = var18 + var17 * var2;
            double var20 = Math.max(Math.max(a(var6[var19]), a(var6[var19 + 1])), Math.max(a(var6[var19 + var2]), a(var6[var19 + var2 + 1])));
            var16[var19] = var20 > var4;
         }
      }

      return var16;
   }

   private static double a(double var0) {
      return Double.isNaN(var0) ? 0.0 : var0;
   }

   private void a(TerrainSampler var1, StreamsConfig var2, int var3, int var4, int var5, int var6, int var7, double[] var8, boolean[] var9) {
      int var10 = Math.floorDiv(var4, var3);
      int var11 = Math.floorDiv(var5, var3);
      boolean[] var12 = new boolean[var6 * var7];

      for (int var13 = 0; var13 < var7; var13++) {
         boolean var14 = (var11 + var13 & 1) == 0;

         for (int var15 = 0; var15 < var6; var15++) {
            int var16 = var15 + var13 * var6;
            if (var9[var16]) {
               if (var14 && (var10 + var15 & 1) == 0) {
                  var12[var16] = true;
               } else {
                  int var17 = (var10 + var15 & 1) == 0 ? var15 : var15 - 1;
                  int var18 = var14 ? var13 : var13 - 1;

                  for (int var19 = 0; var19 <= 2; var19 += 2) {
                     for (int var20 = 0; var20 <= 2; var20 += 2) {
                        int var21 = var17 + var20;
                        int var22 = var18 + var19;
                        if (var21 >= 0 && var21 < var6 && var22 >= 0 && var22 < var7) {
                           var12[var21 + var22 * var6] = true;
                        }
                     }
                  }
               }
            }
         }
      }

      a(var7, (var7x, var8x) -> {
         for (int var9x = var7x; var9x < var8x; var9x++) {
            int var10x = var5 + var9x * var3;

            for (int var11x = 0; var11x < var6; var11x++) {
               int var12x = var11x + var9x * var6;
               if (var12[var12x]) {
                  var8[var12x] = var1.height(var4 + var11x * var3, var10x);
               }
            }
         }
      });
      double[] var23 = new double[var6 * var7];
      a(var7, (var8x, var9x) -> {
         for (int var10x = var8x; var10x < var9x; var10x++) {
            int var11x = var5 + var10x * var3;

            for (int var12x = 0; var12x < var6; var12x++) {
               int var13x = var12x + var10x * var6;
               if (var9[var13x] || var12[var13x]) {
                  var23[var13x] = var1.estimate(var4 + var12x * var3, var11x);
               }
            }
         }
      });
      double var24 = var2.seaLevel() + 8.0;
      a(var7, (var14x, var15x) -> {
         for (int var16x = var14x; var16x < var15x; var16x++) {
            boolean var17x = (var11 + var16x & 1) == 0;
            int var18x = var5 + var16x * var3;

            for (int var19x = 0; var19x < var6; var19x++) {
               int var20x = var19x + var16x * var6;
               if (var9[var20x] && !var12[var20x]) {
                  int var21x = var4 + var19x * var3;
                  int var22x = (var10 + var19x & 1) == 0 ? var19x : var19x - 1;
                  int var23x = var17x ? var16x : var16x - 1;
                  var8[var20x] = a(var1, var8, var23, var6, var7, var22x, var23x, var19x, var16x, var21x, var18x, var24);
               }
            }
         }
      });
   }

   private static double a(
      TerrainSampler var0, double[] var1, double[] var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, double var11
   ) {
      if (var5 >= 0 && var6 >= 0 && var5 + 2 < var3 && var6 + 2 < var4) {
         int var13 = var5 + var6 * var3;
         int var14 = var13 + 2;
         int var15 = var13 + 2 * var3;
         int var16 = var15 + 2;
         double var17 = var1[var13];
         double var19 = var1[var14];
         double var21 = var1[var15];
         double var23 = var1[var16];
         if (var17 != -1000.0 && var19 != -1000.0 && var21 != -1000.0 && var23 != -1000.0) {
            double var25 = (var7 - var5) / 2.0;
            double var27 = (var8 - var6) / 2.0;
            double var29 = var2[var13];
            double var31 = var2[var14];
            double var33 = var2[var15];
            double var35 = var2[var16];
            double var37 = var2[var7 + var8 * var3];
            double var39 = Math.min(Math.min(var29, var31), Math.min(var33, var35));
            double var41 = Math.max(Math.max(var29, var31), Math.max(var33, var35));
            if (!(var37 > var41 + 1.0) && !(var37 < var39 - 1.0)) {
               double var43 = a(var17, var19, var21, var23, var25, var27);
               double var45 = a(var29, var31, var33, var35, var25, var27);
               double var47 = var43 + (var37 - var45);
               return !(var47 <= var11) && !(var43 <= var11) ? var47 : var0.height(var9, var10);
            } else {
               return var0.height(var9, var10);
            }
         } else {
            return var0.height(var9, var10);
         }
      } else {
         return var0.height(var9, var10);
      }
   }

   private static double a(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = var0 + (var2 - var0) * var8;
      double var14 = var4 + (var6 - var4) * var8;
      return var12 + (var14 - var12) * var10;
   }

   private static boolean a(int var0, int var1, int var2, double[] var3, int var4, int var5, int var6) {
      int var7 = (Math.floorDiv(var0, var2) * var2 - var5) / var2;
      int var8 = (Math.floorDiv(var1, var2) * var2 - var6) / var2;
      int var9 = var7 + var8 * var4;
      return var3[var9] == -1000.0 || var3[var9 + 1] == -1000.0 || var3[var9 + var4] == -1000.0 || var3[var9 + var4 + 1] == -1000.0;
   }

   private static double b(int var0, int var1, int var2, double[] var3, int var4, int var5, int var6) {
      int var7 = Math.floorDiv(var0, var2) * var2;
      int var8 = Math.floorDiv(var1, var2) * var2;
      double var9 = (double)(var0 - var7) / var2;
      double var11 = (double)(var1 - var8) / var2;
      int var13 = (var7 - var5) / var2;
      int var14 = (var8 - var6) / var2;
      int var15 = var13 + var14 * var4;
      double var16 = var3[var15];
      double var18 = var3[var15 + 1];
      double var20 = var3[var15 + var4];
      double var22 = var3[var15 + var4 + 1];
      double var24 = var16 + (var18 - var16) * var9;
      double var26 = var20 + (var22 - var20) * var9;
      return var24 + (var26 - var24) * var11;
   }

   public static void parallelRange(int var0, LowResGrid.RowBand var1) {
      a(var0, var1);
   }

   private static void a(int var0, LowResGrid.RowBand var1) {
      if (var0 > 0) {
         int var2 = HeightCensus.current();
         LowResGrid.RowBand var3 = var1;
         LowResGrid.RowBand var4 = (var2x, var3x) -> {
            int var4x = HeightCensus.enter(var2);

            try {
               var3.run(var2x, var3x);
            } finally {
               HeightCensus.restore(var4x);
            }
         };
         b(var0, var4);
      }
   }

   private static void b(int var0, LowResGrid.RowBand var1) {
      if (Thread.currentThread().getName().startsWith("streamsreflowing-sampler")) {
         var1.run(0, var0);
      } else {
         int var2 = Math.min(c, var0);
         if (var2 <= 1) {
            var1.run(0, var0);
         } else {
            int var3 = Math.max(1, (var0 + var2 * 8 - 1) / (var2 * 8));
            AtomicInteger var4 = new AtomicInteger();
            int var5 = b();
            long var6 = l.get()[0];
            if (var5 == 0) {
               a(c);
            }

            CountDownLatch var8 = new CountDownLatch(var2);
            AtomicReference var9 = new AtomicReference();

            try {
               for (int var10 = 0; var10 < var2; var10++) {
                  e.execute(new LowResGrid.PrioritizedTask(var5, var6, d.getAndIncrement(), () -> {
                     try {
                        int var6x;
                        try {
                           while ((var6x = var4.getAndAdd(var3)) < var0) {
                              var1.run(var6x, Math.min(var0, var6x + var3));
                           }
                        } catch (Throwable var10x) {
                           var9.compareAndSet(null, var10x);
                        }
                     } finally {
                        var8.countDown();
                     }
                  }));
               }
            } catch (RejectedExecutionException var12) {
               var1.run(0, var0);
               return;
            }

            try {
               var8.await();
            } catch (InterruptedException var11) {
               Thread.currentThread().interrupt();
               throw new RuntimeException("interrupted during parallel terrain sampling", var11);
            }

            Throwable var13 = (Throwable)var9.get();
            if (var13 != null) {
               throw new RuntimeException("parallel terrain sampling failed", var13);
            }
         }
      }
   }

   public static void parallelFor(int var0, IntConsumer var1) {
      a(var0, (var1x, var2) -> {
         for (int var3 = var1x; var3 < var2; var3++) {
            var1.accept(var3);
         }
      });
   }

   public int worldX(int var1) {
      return this.originX + var1 * this.spacing;
   }

   public int worldZ(int var1) {
      return this.originZ + var1 * this.spacing;
   }

   public int idx(int var1, int var2) {
      return var1 + var2 * this.nx;
   }

   public int gi(int var1) {
      return var1 % this.nx;
   }

   public int gj(int var1) {
      return var1 / this.nx;
   }

   public boolean valid(int var1, int var2) {
      return var1 >= 0 && var1 < this.nx && var2 >= 0 && var2 < this.nz;
   }

   public double height(int var1) {
      return this.a[var1];
   }

   public void overrideHeight(int var1, double var2) {
      this.a[var1] = var2;
   }

   public void markRiverOutlet(int var1) {
      if (this.t == null) {
         this.t = new long[this.size() + 63 >>> 6];
      }

      this.t[var1 >>> 6] = this.t[var1 >>> 6] | 1L << (var1 & 63);
   }

   public boolean riverOutlet(int var1) {
      return this.t != null && (this.t[var1 >>> 6] & 1L << (var1 & 63)) != 0L;
   }

   public boolean inDomain(int var1) {
      return this.b[var1];
   }

   public int size() {
      return this.nx * this.nz;
   }

   private static final class PrioritizedTask implements Comparable<LowResGrid.PrioritizedTask>, Runnable {
      private final int a;
      private final long b;
      private final long c;
      private final Runnable d;

      PrioritizedTask(int var1, long var2, long var4, Runnable var6) {
         this.a = var1;
         this.b = var2;
         this.c = var4;
         this.d = var6;
      }

      @Override
      public void run() {
         this.d.run();
      }

      public int compareTo(LowResGrid.PrioritizedTask var1) {
         int var2 = Integer.compare(this.a, var1.a);
         if (var2 != 0) {
            return var2;
         }

         var2 = Long.compare(this.b, var1.b);
         return var2 != 0 ? var2 : Long.compare(this.c, var1.c);
      }
   }

   @FunctionalInterface
   public interface RowBand {
      void run(int var1, int var2);
   }
}
