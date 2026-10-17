package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.Constants;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public final class ReadLocality {
   public static final boolean ON = System.getProperty("streamsreflowing.locality") != null;
   private static final long a = 30000000000L;
   private static final ConcurrentHashMap<Long, LongAdder> b = new ConcurrentHashMap<>();
   private static final LongAdder c = new LongAdder();
   private static final AtomicLong d = new AtomicLong(System.nanoTime());
   private static final LongAdder e = new LongAdder();
   private static final ReadLocality.OffCounter f = new ReadLocality.OffCounter();
   private static final ConcurrentHashMap<String, LongAdder> g = new ConcurrentHashMap<>();
   private static final Set<String> h = Arrays.stream(TerrainSampler.class.getMethods()).map(Method::getName).collect(Collectors.toUnmodifiableSet());

   private ReadLocality() {
   }

   public static void miss(int var0, int var1) {
      if (ON) {
         long var2 = a((long)(var0 >> 4) << 32 ^ var1 >> 4 & 4294967295L);
         b.computeIfAbsent(var2, var0x -> new LongAdder()).increment();
         c.increment();
         if ((var0 & 3) == 0 && (var1 & 3) == 0) {
            e.increment();
         } else if ((f.a() & 15L) == 0L) {
            g.computeIfAbsent(a(), var0x -> new LongAdder()).increment();
         }

         b();
      }
   }

   private static String a() {
      return StackWalker.getInstance()
         .walk(
            var0 -> var0.filter(
                  var0x -> var0x.getClassName().startsWith("dev.streamsreflowing")
                     && !var0x.getClassName().startsWith("dev.streamsreflowing.core.terrain")
                     && !var0x.getClassName().startsWith("dev.streamsreflowing.worldgen.StreamsWorldgen")
                     && !h.contains(var0x.getMethodName())
                     && !var0x.getClassName().endsWith("$PrioritizedTask")
               )
               .limit(3L)
               .map(
                  var0x -> var0x.getClassName().substring(var0x.getClassName().lastIndexOf(46) + 1) + "." + var0x.getMethodName() + ":" + var0x.getLineNumber()
               )
               .collect(Collectors.joining(" <- "))
         );
   }

   private static long a(long var0) {
      var0 = (var0 ^ var0 >>> 33) * -49064778989728563L;
      var0 = (var0 ^ var0 >>> 33) * -4265267296055464877L;
      return var0 ^ var0 >>> 33;
   }

   private static void b() {
      long var0 = d.get();
      long var2 = System.nanoTime();
      if (var2 - var0 >= 30000000000L && d.compareAndSet(var0, var2)) {
         dump("so far");
      }
   }

   public static void dump(String var0) {
      if (ON && !b.isEmpty()) {
         int[] var1 = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256};
         long[] var2 = new long[var1.length + 1];
         long[] var3 = new long[var1.length + 1];
         long var4 = 0L;

         for (LongAdder var7 : b.values()) {
            long var8 = var7.sum();
            var4++;
            int var10 = 0;

            while (var10 < var1.length && var8 >= var1[var10]) {
               var10++;
            }

            var2[var10]++;
            var3[var10] += var8;
         }

         long var20 = c.sum();
         StringBuilder var21 = new StringBuilder();

         for (int var9 = 1; var9 <= var1.length; var9++) {
            if (var2[var9] != 0L) {
               String var23 = var9 == var1.length ? var1[var9 - 1] + "+" : var1[var9 - 1] + "-" + (var1[var9] - 1);
               var21.append(" | ")
                  .append(var23)
                  .append(": ")
                  .append(var2[var9])
                  .append(" chunks, ")
                  .append(var3[var9])
                  .append(" reads (")
                  .append(Math.round(100.0 * var3[var9] / var20))
                  .append("%)");
            }
         }

         StringBuilder var22 = new StringBuilder();

         for (int var13 : new int[]{4, 8, 16, 32}) {
            long var14 = 0L;

            for (LongAdder var17 : b.values()) {
               long var18 = var17.sum();
               if (var18 >= var13) {
                  var14 += var18;
               }
            }

            var22.append(" >=").append(var13).append(": ").append(Math.round(100.0 * var14 / var20)).append('%');
         }

         Constants.LOG
            .info(
               "[SR-LOCALITY] {} -- {} real reads over {} chunks ({} per chunk){}",
               new Object[]{var0, var20, var4, String.format(Locale.ROOT, "%.1f", (double)var20 / var4), var21}
            );
         Constants.LOG.info("[SR-LOCALITY] share of reads in chunks with at least N reads --{}", var22);
         long var25 = e.sum();
         long var26 = f.b();
         Constants.LOG
            .info(
               "[SR-LOCALITY] cell corners: {} reads on a corner, {} off ({}%); off-corner callers, 1 in 16 sampled:",
               new Object[]{var25, var26, Math.round(100.0 * var26 / Math.max(1L, var25 + var26))}
            );
         g.entrySet()
            .stream()
            .sorted((var0x, var1x) -> Long.compare(var1x.getValue().sum(), var0x.getValue().sum()))
            .limit(25L)
            .forEach(var0x -> Constants.LOG.info("[SR-LOCALITY]   {} x16  {}", var0x.getValue().sum(), var0x.getKey()));
      }
   }

   private static final class OffCounter {
      private final AtomicLong a = new AtomicLong();

      long a() {
         return this.a.getAndIncrement();
      }

      long b() {
         return this.a.get();
      }
   }
}
