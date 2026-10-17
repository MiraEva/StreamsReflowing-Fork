package dev.streamsreflowing.core.math;

public final class Hashing {
   private static final long a = -7046029254386353131L;
   private static final long b = -3335678366873096957L;
   private static final long c = -6884282663029611473L;
   private static final long d = -1800455987208640293L;

   private Hashing() {
   }

   public static long mix(long var0) {
      var0 = (var0 ^ var0 >>> 30) * -4658895280553007687L;
      var0 = (var0 ^ var0 >>> 27) * -7723592293110705685L;
      return var0 ^ var0 >>> 31;
   }

   public static long hash(long var0, long var2, long var4) {
      long var6 = var0 * -7046029254386353131L;
      var6 = mix(var6 ^ var2 * -3335678366873096957L);
      var6 = mix(var6 ^ var4 * -6884282663029611473L);
      return mix(var6);
   }

   public static long hash(long var0, long var2, long var4, long var6) {
      return mix(hash(var0, var2, var4) ^ var6 * -1800455987208640293L);
   }

   public static double toUnit(long var0) {
      return (var0 >>> 11) * 1.110223E-16F;
   }

   public static double unit(long var0, long var2, long var4) {
      return toUnit(hash(var0, var2, var4));
   }

   public static double unit(long var0, long var2, long var4, long var6) {
      return toUnit(hash(var0, var2, var4, var6));
   }
}
