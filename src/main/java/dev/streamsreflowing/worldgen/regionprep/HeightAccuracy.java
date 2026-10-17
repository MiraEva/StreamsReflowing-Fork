package dev.streamsreflowing.worldgen.regionprep;

public final class HeightAccuracy {
   public static final double MAX_ABOVE = 3.0;
   public static final double MAX_BELOW = 6.0;
   public static final int SAMPLE_COLUMNS = 1000;

   private HeightAccuracy() {
   }

   public static boolean inWindow(double var0) {
      return !Double.isNaN(var0) && var0 <= 3.0 && var0 >= -6.0;
   }

   public static String describe(int var0, int var1, double var2, double var4) {
      return var0 + "/" + var1 + " columns outside +3.0/-6.0 (highest " + var2 + ", lowest " + var4 + ")";
   }
}
