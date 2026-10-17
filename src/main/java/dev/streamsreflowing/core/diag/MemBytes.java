package dev.streamsreflowing.core.diag;

public final class MemBytes {
   private MemBytes() {
   }

   public static long align(long var0) {
      return var0 + 7L & -8L;
   }

   public static long array(long var0, int var2) {
      return align(16L + var0 * var2);
   }

   public static long object(int var0) {
      return align(12L + var0);
   }

   public static long hashMapNodes(long var0) {
      return var0 * 48L + array((long)(var0 * 1.34) + 16L, 4);
   }
}
