package dev.streamsreflowing.core.terrain;

@FunctionalInterface
public interface TerrainSampler {
   int CLASS_LAND = 0;
   int CLASS_RIVER = 1;
   int CLASS_OCEAN = 2;

   default int waterBiomeClass(int var1, int var2) {
      return 0;
   }

   default int waterBiomeClassAt(int var1, int var2, int var3) {
      return this.waterBiomeClass(var1, var3);
   }

   default boolean outletBiomeOk(int var1, int var2, int var3) {
      return true;
   }

   default double riverSurface(int var1, int var2) {
      return Double.NaN;
   }

   double height(int var1, int var2);

   default double surfaceHeight(int var1, int var2) {
      return this.height(var1, var2);
   }

   default double groundHeight(int var1, int var2) {
      return this.height(var1, var2);
   }

   default void groundHeightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
      for (int var5 = 0; var5 < var4; var5++) {
         var3[var5] = this.groundHeight(var1[var5], var2[var5]);
      }
   }

   default boolean hasEstimate() {
      return false;
   }

   default boolean useHybridGridFill() {
      return false;
   }

   default double estimate(int var1, int var2) {
      return this.height(var1, var2);
   }

   default double groundHeightExact(int var1, int var2) {
      return this.groundHeight(var1, var2);
   }

   default double groundHeightReal(int var1, int var2) {
      return this.groundHeightExact(var1, var2);
   }

   default double surfaceHeightReal(int var1, int var2) {
      return this.surfaceHeight(var1, var2);
   }

   default boolean bakedCalibration() {
      return false;
   }

   default boolean batchCapable() {
      return false;
   }

   default void heightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
      for (int var5 = 0; var5 < var4; var5++) {
         var3[var5] = this.height(var1[var5], var2[var5]);
      }
   }

   default boolean openAt(int var1, int var2, int var3) {
      return var2 > this.height(var1, var3);
   }
}
