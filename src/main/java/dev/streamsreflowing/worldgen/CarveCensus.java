package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.worldgen.regionprep.PrepDiag;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class CarveCensus {
   public static final int PLAN = 0;
   public static final int DECIDE = 1;
   public static final int CARVE = 2;
   public static final int CORRECT = 3;
   public static final int CAPS = 4;
   public static final int SEAMS = 5;
   public static final int STYLES = 6;
   public static final int POCKETS = 7;
   public static final int FLOW = 8;
   private static final String[] a = new String[]{"plan", "decide", "carve", "correct", "floating-caps", "seams", "bank-styles", "lake-pockets", "flow"};
   private static final LongAdder[] b = new LongAdder[a.length];
   private static final AtomicLong c;
   private static final LongAdder d;
   private static final int e = 512;
   private static final LongAdder f;
   private static final LongAdder g;
   private static final AtomicLong h;
   private static final LongAdder i;
   private static final LongAdder j;

   private CarveCensus() {
   }

   public static long chunks() {
      return c.get();
   }

   public static void waited(long var0) {
      f.add(var0);
      g.increment();
      long var2 = var0 / 1000000L;
      h.accumulateAndGet(var2, Math::max);
   }

   public static void bedEval(long var0) {
      i.increment();
      j.add(System.nanoTime() - var0);
   }

   public static void add(int var0, long var1) {
      b[var0].add(System.nanoTime() - var1);
   }

   public static void touched() {
      d.increment();
   }

   public static void chunkDone() {
      long var0 = c.incrementAndGet();
      if (var0 % 512L == 0L && PrepDiag.loud()) {
         long var2 = 0L;

         for (LongAdder var7 : b) {
            var2 += var7.sum();
         }

         StringBuilder var8 = new StringBuilder();

         for (int var9 = 0; var9 < a.length; var9++) {
            long var11 = b[var9].sum() / 1000000L;
            var8.append(' ').append(a[var9]).append(' ').append(var11).append("ms");
            if (var2 > 0L) {
               var8.append('(').append(100L * b[var9].sum() / var2).append("%)");
            }
         }

         Constants.LOG.info("[SR-CARVE] {} chunks ({} with our water), {} ms of worker time:{}", new Object[]{var0, d.sum(), var2 / 1000000L, var8});
         Constants.LOG
            .info(
               "[SR-CARVE] {} chunks parked for their area first: {} ms total, worst {} ms -- carving is {} ms of the two",
               new Object[]{g.sum(), f.sum() / 1000000L, h.get(), var2 / 1000000L}
            );
         long var10 = i.sum();
         Constants.LOG
            .info(
               "[SR-CARVE] channel-bed (braid/bars/asymmetry): {} evaluations, {} ms ({}% of carve), {} per water chunk",
               new Object[]{var10, j.sum() / 1000000L, var2 == 0L ? 0L : 100L * j.sum() / var2, d.sum() == 0L ? 0L : var10 / d.sum()}
            );
      }
   }

   static {
      for (int var0 = 0; var0 < b.length; var0++) {
         b[var0] = new LongAdder();
      }

      c = new AtomicLong();
      d = new LongAdder();
      f = new LongAdder();
      g = new LongAdder();
      h = new AtomicLong();
      i = new LongAdder();
      j = new LongAdder();
   }
}
