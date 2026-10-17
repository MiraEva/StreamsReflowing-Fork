package dev.streamsreflowing.core.diag;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLongArray;

public final class HeightCensus {
   public static final boolean ENABLED = Boolean.getBoolean("streamsreflowing.heightcensus") || !Boolean.getBoolean("streamsreflowing.gpuquiet");
   public static final int OTHER = 0;
   public static final int GRID_COARSE = 1;
   public static final int GRID_FULL = 2;
   public static final int SNAP = 3;
   public static final int FOLLOW = 4;
   public static final int MOUTH = 5;
   public static final int LAKE = 6;
   public static final int CAPTURE = 7;
   public static final int ATTRACTOR = 8;
   public static final int PFLOOD = 9;
   public static final int D8 = 10;
   public static final int PONDS = 11;
   public static final int REACH = 12;
   public static final int TRIM = 13;
   public static final int POST = 14;
   public static final int PRUNE = 15;
   public static final int BAND = 16;
   private static final String[] a = new String[]{
      "other",
      "grid-coarse",
      "grid-full",
      "valley-snap",
      "ground-follow",
      "mouths",
      "lakes",
      "capture",
      "attractors",
      "flood",
      "d8",
      "ponds",
      "reach",
      "trim",
      "post",
      "orphan-prune",
      "band"
   };
   private static final AtomicLongArray b = new AtomicLongArray(a.length);
   private static final AtomicLongArray c = new AtomicLongArray(a.length);
   private static final AtomicLongArray d = new AtomicLongArray(a.length);
   private static final AtomicLongArray e = new AtomicLongArray(a.length);
   private static final AtomicLongArray f = new AtomicLongArray(a.length);
   private static final AtomicLongArray g = new AtomicLongArray(a.length);
   private static final ThreadLocal<int[]> h = ThreadLocal.withInitial(() -> new int[]{0});

   private HeightCensus() {
   }

   public static String[] siteNames() {
      return (String[])a.clone();
   }

   public static int current() {
      return h.get()[0];
   }

   public static int enter(int var0) {
      if (!ENABLED) {
         return 0;
      }

      int[] var1 = h.get();
      int var2 = var1[0];
      var1[0] = var0;
      return var2;
   }

   public static void restore(int var0) {
      if (ENABLED) {
         h.get()[0] = var0;
      }
   }

   public static void record(boolean var0) {
      if (ENABLED) {
         int var1 = h.get()[0];
         b.incrementAndGet(var1);
         if (var0) {
            c.incrementAndGet(var1);
         }
      }
   }

   public static void recordEstimate() {
      if (ENABLED) {
         f.incrementAndGet(h.get()[0]);
      }
   }

   public static void recordBulk(int var0) {
      if (ENABLED && var0 > 0) {
         int var1 = h.get()[0];
         b.addAndGet(var1, var0);
         c.addAndGet(var1, var0);
      }
   }

   public static String drainPlateReport() {
      if (!ENABLED) {
         return null;
      }

      long var0 = 0L;
      long var2 = 0L;
      long var4 = 0L;
      StringBuilder var6 = new StringBuilder();
      HeightCensus.Row[] var7 = new HeightCensus.Row[a.length];

      for (int var8 = 0; var8 < a.length; var8++) {
         long var9 = b.get(var8) - d.getAndSet(var8, b.get(var8));
         long var11 = c.get(var8) - e.getAndSet(var8, c.get(var8));
         long var13 = f.get(var8) - g.getAndSet(var8, f.get(var8));
         var7[var8] = new HeightCensus.Row(a[var8], var9, var11, var13);
         var0 += var9;
         var2 += var11;
         var4 += var13;
      }

      if (var0 == 0L && var4 == 0L) {
         return null;
      }

      Arrays.sort(var7, (var0x, var1) -> Long.compare(var1.b + var1.d, var0x.b + var0x.d));

      for (HeightCensus.Row var17 : var7) {
         if (var17.b != 0L || var17.d != 0L) {
            var6.append(' ').append(var17.a).append(' ').append(var17.b).append('(').append(var0 == 0L ? 0L : 100L * var17.b / var0).append("%)");
            if (var17.d > 0L) {
               var6.append('+').append(var17.d).append("est");
            }
         }
      }

      return String.format(
         Locale.ROOT,
         "%d height calls, %d%% of them batchable today%s --%s",
         var0,
         var0 == 0L ? 0L : 100L * var2 / var0,
         var4 == 0L ? "" : String.format(Locale.ROOT, ", %d estimate lookups", var4),
         var6
      );
   }

   private static final class Row {
      final String a;
      final long b;
      final long c;
      final long d;

      Row(String var1, long var2, long var4, long var6) {
         this.a = var1;
         this.b = var2;
         this.c = var4;
         this.d = var6;
      }
   }
}
