package dev.streamsreflowing.flow;

import dev.streamsreflowing.Constants;
import java.util.concurrent.atomic.AtomicLong;

public final class FlowTrace {
   public static final boolean ON = System.getProperty("streamsreflowing.flowtrace") != null;
   public static final AtomicLong gridBuilds = new AtomicLong();
   public static final AtomicLong tilesComputed = new AtomicLong();
   public static final AtomicLong tilesFromCarve = new AtomicLong();
   public static final AtomicLong deliverHit = new AtomicLong();
   public static final AtomicLong deliverMiss = new AtomicLong();
   public static final AtomicLong plateRequests = new AtomicLong();
   public static final AtomicLong fieldRequests = new AtomicLong();
   public static final AtomicLong fieldBuilds = new AtomicLong();
   public static final AtomicLong fieldBuildMs = new AtomicLong();
   public static final AtomicLong deferred = new AtomicLong();
   public static final AtomicLong resumed = new AtomicLong();
   public static final AtomicLong carvedChunks = new AtomicLong();
   public static final AtomicLong harnessStalls = new AtomicLong();
   public static final AtomicLong parkedDelivered = new AtomicLong();
   public static final AtomicLong parkedAgeMs = new AtomicLong();
   public static final AtomicLong parkedAgeMaxMs = new AtomicLong();
   private static long lastReportMs;
   private static final long[] PREV = new long[14];

   private FlowTrace() {
   }

   public static void parkedDelivery(long ageMs) {
      parkedDelivered.incrementAndGet();
      parkedAgeMs.addAndGet(ageMs);
      parkedAgeMaxMs.accumulateAndGet(ageMs, Math::max);
   }

   public static void maybeReport() {
      if (ON) {
         long now = System.currentTimeMillis();
         if (lastReportMs == 0L) {
            lastReportMs = now;
         } else if (now - lastReportMs >= 30000L) {
            long secs = Math.max(1L, (now - lastReportMs) / 1000L);
            lastReportMs = now;
            long[] cur = new long[]{
               gridBuilds.get(),
               tilesComputed.get(),
               tilesFromCarve.get(),
               deliverHit.get(),
               deliverMiss.get(),
               plateRequests.get(),
               fieldRequests.get(),
               deferred.get(),
               resumed.get(),
               fieldBuilds.get(),
               fieldBuildMs.get(),
               parkedDelivered.get(),
               parkedAgeMs.get(),
               parkedAgeMaxMs.get()
            };
            long[] d = new long[cur.length];

            for (int i = 0; i < cur.length; i++) {
               d[i] = cur[i] - PREV[i];
               PREV[i] = cur[i];
            }

            long parkedMax = parkedAgeMaxMs.getAndSet(0L);
            PREV[13] = 0L;
            Constants.LOG
               .info(
                  "[SR-FLOW] {}s: gridBuilds={} tiles(computed={} fromCarve={}) deliver(hit={} miss={}) requests(plates={} fields={}) chunks(deferred={} resumed={}) FIELD BUILDS={} costing {}ms | PARKED delivered={} avgWait={}ms maxWait={}ms",
                  new Object[]{secs, d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9], d[10], d[11], d[11] > 0L ? d[12] / d[11] : 0L, parkedMax}
               );
         }
      }
   }
}
