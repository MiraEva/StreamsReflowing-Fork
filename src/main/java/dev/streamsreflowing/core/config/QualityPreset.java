package dev.streamsreflowing.core.config;

import java.util.Locale;

public enum QualityPreset {
   POTATO(24, 48, 12, 1, 0, 2.0, 6.0, 2, 5),
   LOW(20, 40, 10, 1, 1, 2.0, 6.0, 4, 4),
   MEDIUM(16, 32, 8, 1, 1, 2.0, 6.0, 4, 3),
   HIGH(12, 24, 6, 1, 1, 1.0, 5.0, 4, 3),
   EXTREME(8, 16, 4, 1, 1, 1.0, 3.0, 4, 2),
   MAX(4, 0, 2, 1, 2, 0.0, 0.0, 2, 1);

   public final int gridSpacing;
   public final int terrainSampleSpacing;
   public final int valleySnapRadius;
   public final int terrainFit;
   public final int lakeSampleSpacing;
   public final int prefetchRadius;
   public final int terrainSamplingMode;
   public final double gradeWindowAbove;
   public final double gradeWindowBelow;

   public static int legacyValleySnapRadius(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toUpperCase(Locale.ROOT);

      return switch (var1) {
         case "POTATO" -> 0;
         case "EXTREME" -> 4;
         case "MAX" -> 2;
         default -> 8;
      };
   }

   QualityPreset(int var3, int var4, int var5, int var6, int var7, double var8, double var10, int var12, int var13) {
      this.gridSpacing = var3;
      this.terrainSampleSpacing = var4;
      this.valleySnapRadius = var5;
      this.terrainFit = var12;
      this.lakeSampleSpacing = var13;
      this.prefetchRadius = var6;
      this.terrainSamplingMode = var7;
      this.gradeWindowAbove = var8;
      this.gradeWindowBelow = var10;
   }
}
