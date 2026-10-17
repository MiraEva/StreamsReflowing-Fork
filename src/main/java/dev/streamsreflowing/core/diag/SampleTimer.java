package dev.streamsreflowing.core.diag;

import java.util.concurrent.atomic.LongAdder;

public final class SampleTimer {
   public static final boolean ON = Boolean.getBoolean("streamsreflowing.sampletime");
   private static final LongAdder a = new LongAdder();
   private static final LongAdder b = new LongAdder();
   private static final LongAdder c = new LongAdder();
   private static final LongAdder d = new LongAdder();
   private static final LongAdder e = new LongAdder();

   private SampleTimer() {
   }

   public static long start() {
      return ON ? System.nanoTime() : 0L;
   }

   public static void height(long var0) {
      if (ON) {
         a.add(System.nanoTime() - var0);
         b.increment();
      }
   }

   public static void biome(long var0) {
      if (ON) {
         c.add(System.nanoTime() - var0);
         d.increment();
      }
   }

   public static void plateBuilt() {
      if (ON) {
         e.increment();
      }
   }

   public static String report() {
      long var0 = a.sum();
      long var2 = b.sum();
      long var4 = c.sum();
      long var6 = d.sum();
      double var8 = (double)var0 + var4;
      return String.format(
         "plates %,d | heights %,d calls / %,.1f ms cpu (%.2f us each) | biomes %,d calls / %,.1f ms cpu (%.2f us each) | biome share %.1f%%",
         e.sum(),
         var2,
         var0 / 1000000.0,
         var2 == 0L ? 0.0 : var0 / 1000.0 / var2,
         var6,
         var4 / 1000000.0,
         var6 == 0L ? 0.0 : var4 / 1000.0 / var6,
         var8 == 0.0 ? 0.0 : 100.0 * var4 / var8
      );
   }
}
