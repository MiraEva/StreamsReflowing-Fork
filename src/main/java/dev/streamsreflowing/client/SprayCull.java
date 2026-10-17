package dev.streamsreflowing.client;

public final class SprayCull {
   public static final int KEEP_ALL = 24;
   private static final double COS_LIMIT = -0.17;
   public static final boolean ENABLED = !Boolean.getBoolean("streamsreflowing.noparticlecull");

   private SprayCull() {
   }

   public static boolean behind(double dx, double dz, double d, double lookX, double lookZ, double lookLen) {
      return ENABLED && !(d <= 24.0) && !(lookLen < 1.0E-4) ? dx * lookX + dz * lookZ < -0.17 * d * lookLen : false;
   }
}
