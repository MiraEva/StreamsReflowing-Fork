package dev.streamsreflowing.core.terrain;

@FunctionalInterface
public interface WetnessSampler {
   double wetness(int var1, int var2, int var3);

   default double cohesion(int var1, int var2, int var3) {
      return 1.0;
   }

   default boolean barsAnything() {
      return false;
   }

   default boolean barred(int var1, int var2, int var3) {
      return false;
   }
}
