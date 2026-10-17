package dev.streamsreflowing.core.terrain;

public final class ReadLattice {
   private static final boolean a = System.getProperty("streamsreflowing.nolattice") != null;
   private static volatile int b = 1;
   public static final boolean CORNER_BATCH = System.getProperty("streamsreflowing.cornerbatch") != null;
   private static final boolean c = !CORNER_BATCH || System.getProperty("streamsreflowing.nostreambatch") != null;
   private static final ThreadLocal<int[]> d = ThreadLocal.withInitial(() -> new int[1]);

   private ReadLattice() {
   }

   public static int cell() {
      return b;
   }

   public static void set(int var0) {
      b = a ? 1 : Math.max(1, var0);
   }

   public static int snap(int var0, int var1) {
      return var1 <= 1 ? var0 : Math.floorDiv(var0 + (var1 >> 1), var1) * var1;
   }

   public static void enterStreamChunks() {
      d.get()[0]++;
   }

   public static void exitStreamChunks() {
      int[] var0 = d.get();
      var0[0] = Math.max(0, var0[0] - 1);
   }

   public static boolean inStreamChunks() {
      return !c && d.get()[0] > 0;
   }

   public static double snap(double var0, int var2) {
      return var2 <= 1 ? var0 : (double)Math.round(var0 / var2) * var2;
   }
}
