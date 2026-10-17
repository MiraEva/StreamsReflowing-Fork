package dev.streamsreflowing.core;

public final class ColumnKey {
   private ColumnKey() {
   }

   public static long of(int var0, int var1) {
      return mix(pack(var0, var1));
   }

   public static long pack(int var0, int var1) {
      return (long)var0 << 32 ^ var1 & 4294967295L;
   }

   public static long mix(long var0) {
      long var2 = var0;
      var2 ^= var2 >>> 33;
      var2 *= -49064778989728563L;
      var2 ^= var2 >>> 33;
      var2 *= -4265267296055464877L;
      return var2 ^ var2 >>> 33;
   }
}
