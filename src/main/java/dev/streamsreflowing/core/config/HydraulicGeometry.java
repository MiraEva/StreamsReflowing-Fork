package dev.streamsreflowing.core.config;

public record HydraulicGeometry(double widthSlope, double widthClimate, double widthVariation, double depthRatio, double bendAsymmetry, double channelBars, double deltaMouths) {
   public static final HydraulicGeometry OFF = new HydraulicGeometry(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   public static final double BRAID_BED_DEPTH = 2.2;
   private static final double BRAID_FLOOR_FRACTION = 0.7;
   public static final double SECTION_PARABOLA = 0.6666666666666666;
   public static final double SECTION_FLAT = 1.0;
   private static final double BAR_SHARE_FULL = 0.22;
   private static final double BAR_SHARE_POWER = 1.8;
   public static final double BAR_BAND_SHARE = 0.75;

   public HydraulicGeometry {
      widthSlope = clamp(widthSlope);
      widthClimate = clamp(widthClimate);
      widthVariation = clamp(widthVariation);
      depthRatio = clamp(depthRatio);
      bendAsymmetry = clamp(bendAsymmetry);
      channelBars = clamp(channelBars);
      deltaMouths = clamp(deltaMouths);
widthClimate = widthClimate;
depthRatio = depthRatio;
channelBars = channelBars;
}

   public static double braidFloor(double var0) {
      return Math.min(2.2, Math.max(0.0, var0) * 0.7);
   }

   public static double expectedBarShare(double var0) {
      double var2 = var0 < 0.0 ? 0.0 : (var0 > 1.0 ? 1.0 : var0);
      return 0.22 * Math.pow(var2, 1.8) * 0.75;
   }

   private static double clamp(double var0) {
      return Double.isNaN(var0) ? 0.0 : Math.max(0.0, Math.min(1.0, var0));
   }

   public boolean shapesNetwork() {
      return this.widthSlope > 0.0 || this.widthClimate > 0.0 || this.widthVariation > 0.0 || this.deltaMouths > 0.0;
   }

   public boolean shapesBed() {
      return this.depthRatio > 0.0 || this.bendAsymmetry > 0.0 || this.channelBars > 0.0 || this.deltaMouths > 0.0;
   }
}
