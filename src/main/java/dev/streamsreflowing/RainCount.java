package dev.streamsreflowing;

public final class RainCount {
   public static final boolean ON = Boolean.getBoolean("streamsreflowing.raindiag");
   public static volatile Thread render;
   public static volatile boolean particlesOff;
   public static volatile boolean bridgeOff;
   public static long climateCalls;
   public static long climateStamp;
   public static long probeNanos;
   public static long scanNanos;
   public static long scanCalls;
   public static long spawned;
   public static long bridgeScanCalls;
   public static long bridgeTickCalls;
   public static long bridgeParticles;
   public static long bridgeFeet;
   public static volatile int footCount;
   public static volatile long bestFoot;
   public static volatile int bestFootHeight = -1;

   private RainCount() {
   }

   public static void bridge(boolean fromScan, int particles) {
      if (ON) {
         if (fromScan) {
            bridgeScanCalls++;
         } else {
            bridgeTickCalls++;
         }

         bridgeParticles += particles;
      }
   }

   public static void climate(boolean stamp) {
      if (ON && Thread.currentThread() == render) {
         climateCalls++;
         if (stamp) {
            climateStamp++;
         }
      }
   }

   public static void probe(long nanos) {
      if (ON && Thread.currentThread() == render) {
         probeNanos += nanos;
      }
   }

   public static void scan(long nanos, int particles) {
      if (ON) {
         scanNanos += nanos;
         scanCalls++;
         spawned += particles;
      }
   }

   public static void reset() {
      climateCalls = 0L;
      climateStamp = 0L;
      probeNanos = 0L;
      scanNanos = 0L;
      scanCalls = 0L;
      spawned = 0L;
      bridgeScanCalls = 0L;
      bridgeTickCalls = 0L;
      bridgeParticles = 0L;
      bridgeFeet = 0L;
   }
}
