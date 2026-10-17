package dev.streamsreflowing;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class StallWatch {
   public static final boolean ENABLED = System.getProperty("streamsreflowing.stalltrace") != null;
   private static final long SAMPLE_MS = 20L;
   private static final long CLIENT_STALL_NS = 150000000L;
   private static final long SERVER_STALL_NS = 100000000L;
   private static final long REDUMP_NS = 500000000L;
   private static final long WINDOW_NS = 10000000000L;
   private static final int MAX_FRAMES = 30;
   private static final StallWatch.Side CLIENT = new StallWatch.Side("CLIENT", 150000000L);
   private static final StallWatch.Side SERVER = new StallWatch.Side("SERVER", 100000000L);
   private static volatile boolean started;

   private StallWatch() {
   }

   public static void beatClient() {
      CLIENT.thread = Thread.currentThread();
      CLIENT.beat = System.nanoTime();
      start();
   }

   public static void beatServer() {
      SERVER.thread = Thread.currentThread();
      SERVER.beat = System.nanoTime();
      start();
   }

   private static synchronized void start() {
      if (!started) {
         started = true;
         Thread t = new Thread(StallWatch::run, "SR-StallWatch");
         t.setDaemon(true);
         t.setPriority(10);
         t.start();
         Constants.LOG.info("[SR-STALL] watch armed (sample {}ms, client stall {}ms, server stall {}ms)", new Object[]{20L, 150L, 100L});
      }
   }

   private static void run() {
      long windowStart = System.nanoTime();
      long gcBase = gcMillis();

      while (true) {
         try {
            Thread.sleep(20L);
         } catch (InterruptedException e) {
            return;
         }

         long now = System.nanoTime();
         sample(CLIENT, now);
         sample(SERVER, now);
         if (now - windowStart >= 10000000000L) {
            long gcNow = gcMillis();
            report(CLIENT);
            report(SERVER);
            Runtime rt = Runtime.getRuntime();
            Constants.LOG
               .info(
                  "[SR-STALL] window {}s gc={}ms heap={}/{}MB",
                  new Object[]{(now - windowStart) / 1000000000L, gcNow - gcBase, rt.totalMemory() - rt.freeMemory() >> 20, rt.totalMemory() >> 20}
               );
            gcBase = gcNow;
            windowStart = now;
         }
      }
   }

   private static void sample(StallWatch.Side side, long now) {
      Thread t = side.thread;
      long beat = side.beat;
      if (t != null && beat != 0L && t.isAlive()) {
         if (beat != side.prevBeat) {
            if (side.prevBeat != 0L && side.gapCount < side.gaps.length) {
               long gap = beat - side.prevBeat;
               side.gaps[side.gapCount++] = gap;
               if (gap > 55000000L) {
                  side.slowTicks++;
               }
            }

            side.prevBeat = beat;
         }

         long quiet = now - beat;
         StackTraceElement[] stack = t.getStackTrace();
         if (stack.length > 0) {
            String ours = null;

            for (StackTraceElement f : stack) {
               if (f.getClassName().startsWith("dev.streamsreflowing")) {
                  ours = shortName(f);
                  break;
               }
            }

            if (ours != null) {
               side.oursHist.merge(ours, 1, Integer::sum);
            }

            side.topHist.merge(shortName(stack[0]), 1, Integer::sum);
         }

         if (quiet > side.stallNs) {
            if (side.stallStart == 0L) {
               side.stallStart = beat;
               side.lastDump = 0L;
            }

            if (now - side.lastDump > 500000000L) {
               side.lastDump = now;
               StringBuilder sb = new StringBuilder();
               sb.append("[SR-STALL] ").append(side.name).append(" stalled ").append(quiet / 1000000L).append("ms; stack of ").append(t.getName()).append(':');
               int n = 0;

               for (StackTraceElement f : stack) {
                  sb.append("\n[SR-STALL]   at ").append(f);
                  if (++n >= 30) {
                     sb.append("\n[SR-STALL]   ... ").append(stack.length - n).append(" more");
                     break;
                  }
               }

               Constants.LOG.info(sb.toString());
            }
         } else if (side.stallStart != 0L) {
            Constants.LOG.info("[SR-STALL] {} stall ended: {}ms total", side.name, (beat - side.stallStart) / 1000000L);
            side.stallStart = 0L;
         }
      }
   }

   private static void report(StallWatch.Side side) {
      if (side.gapCount != 0 || !side.topHist.isEmpty()) {
         long[] gaps = Arrays.copyOf(side.gaps, side.gapCount);
         Arrays.sort(gaps);
         long p50 = gaps.length > 0 ? gaps[gaps.length / 2] / 1000000L : 0L;
         long max = gaps.length > 0 ? gaps[gaps.length - 1] / 1000000L : 0L;
         Constants.LOG
            .info(
               "[SR-STALL] {} ticks={} gapP50={}ms gapMax={}ms slow(>55ms)={} | ours: {} | top: {}",
               new Object[]{side.name, side.gapCount, p50, max, side.slowTicks, topEntries(side.oursHist, 6), topEntries(side.topHist, 6)}
            );
         side.oursHist.clear();
         side.topHist.clear();
         side.gapCount = 0;
         side.slowTicks = 0;
      }
   }

   private static String topEntries(Map<String, Integer> hist, int limit) {
      return hist.entrySet()
         .stream()
         .sorted((a, b) -> b.getValue() - a.getValue())
         .limit(limit)
         .map(e -> e.getKey() + "x" + e.getValue())
         .reduce((a, b) -> a + " " + b)
         .orElse("-");
   }

   private static String shortName(StackTraceElement f) {
      String cn = f.getClassName();
      int dot = cn.lastIndexOf(46);
      return (dot >= 0 ? cn.substring(dot + 1) : cn) + "." + f.getMethodName();
   }

   private static long gcMillis() {
      long total = 0L;

      for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
         long t = gc.getCollectionTime();
         if (t > 0L) {
            total += t;
         }
      }

      return total;
   }

   private static final class Side {
      final String name;
      final long stallNs;
      volatile Thread thread;
      volatile long beat;
      long stallStart;
      long lastDump;
      long prevBeat;
      final Map<String, Integer> oursHist = new HashMap<>();
      final Map<String, Integer> topHist = new HashMap<>();
      long[] gaps = new long[1024];
      int gapCount;
      int slowTicks;

      Side(String name, long stallNs) {
         this.name = name;
         this.stallNs = stallNs;
      }
   }
}
