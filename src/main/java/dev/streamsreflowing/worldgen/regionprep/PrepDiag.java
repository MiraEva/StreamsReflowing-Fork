package dev.streamsreflowing.worldgen.regionprep;

public final class PrepDiag {
   private PrepDiag() {
   }

   public static boolean loud() {
      return Boolean.getBoolean("streamsreflowing.gpuloud");
   }
}
