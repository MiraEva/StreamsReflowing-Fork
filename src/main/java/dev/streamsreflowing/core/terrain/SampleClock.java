package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.Constants;
import java.util.concurrent.atomic.AtomicLong;

public final class SampleClock {
   public static final boolean ON = System.getProperty("streamsreflowing.caldiag") != null;
   private static final long a = 20000000000L;
   public static final int HEIGHT = 0;
   public static final int SURFACE = 1;
   public static final int GROUND_REAL = 2;
   public static final int SURFACE_REAL = 3;
   private static final String[] b = new String[]{"height", "surface", "ground-real", "surface-real"};
   private static final AtomicLong[] c = new AtomicLong[]{new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong()};
   private static final AtomicLong[] d = new AtomicLong[]{new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong()};
   private static final AtomicLong e = new AtomicLong();
   private static final AtomicLong f = new AtomicLong();
   private static final AtomicLong g = new AtomicLong(System.nanoTime());

   private SampleClock() {
   }

   public static long now() {
      return ON ? System.nanoTime() : 0L;
   }

   public static void height(long var0) {
      read(0, var0);
   }

   public static void read(int var0, long var1) {
      if (ON) {
         c[var0].addAndGet(System.nanoTime() - var1);
         d[var0].incrementAndGet();
         a();
      }
   }

   public static void biome(long var0) {
      if (ON) {
         e.addAndGet(System.nanoTime() - var0);
         f.incrementAndGet();
         a();
      }
   }

   public static void dump() {
      if (ON) {
         g.set(System.nanoTime() - 20000000000L - 1L);
         a();
      }
   }

   private static void a() {
      long var0 = g.get();
      long var2 = System.nanoTime();
      if (var2 - var0 >= 20000000000L && g.compareAndSet(var0, var2)) {
         long var4 = 0L;
         double var6 = 0.0;
         StringBuilder var8 = new StringBuilder();

         for (int var9 = 0; var9 < b.length; var9++) {
            long var10 = d[var9].get();
            double var12 = c[var9].get() / 1000000.0;
            var4 += var10;
            var6 += var12;
            if (var10 > 0L) {
               var8.append(String.format(" %s %d (%d ms, %d us each)", b[var9], var10, Math.round(var12), Math.round(var12 * 1000.0 / var10)));
            }
         }

         long var14 = f.get();
         if (var4 != 0L || var14 != 0L) {
            double var11 = e.get() / 1000000.0;
            Constants.LOG
               .info(
                  "[SR-CAL] reads so far -- terrain {} calls, {} ms total, {} us each [{} ] | biome {} calls, {} ms total, {} us each | biome is {}x the terrain bill",
                  new Object[]{
                     var4,
                     Math.round(var6),
                     var4 == 0L ? 0L : Math.round(var6 * 1000.0 / var4),
                     var8,
                     var14,
                     Math.round(var11),
                     var14 == 0L ? 0L : Math.round(var11 * 1000.0 / var14),
                     var6 <= 0.0 ? "n/a" : String.format("%.2f", var11 / var6)
                  }
               );
         }
      }
   }
}
