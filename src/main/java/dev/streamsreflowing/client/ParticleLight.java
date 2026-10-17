package dev.streamsreflowing.client;

public final class ParticleLight {
   public static final int REFRESH_TICKS = 8;
   public static final boolean ENABLED = !Boolean.getBoolean("streamsreflowing.nolightcache");

   private ParticleLight() {
   }

   public static long key(double x, double y, double z) {
      long bx = (long)Math.floor(x);
      long by = (long)Math.floor(y);
      long bz = (long)Math.floor(z);
      return (bx & 67108863L) << 38 | (by & 4095L) << 26 | bz & 67108863L;
   }
}
