package dev.streamsreflowing.core.river;

public final class FlowSpeed {
   public static final double SLOPE_REF = 0.14285714285714285;
   public static final double MAX = 2.0;
   public static final double MIN = 0.25;
   private static final double a = 0.3333333333333333;
   public static final double DEPTH_REF = 2.0;
   private static final double b = 0.3333333333333333;
   private static final double c = 0.65;
   private static final double d = 1.35;
   private static final double e = 0.6;
   private static final double f = 1.8;

   private FlowSpeed() {
   }

   public static double forReach(double var0, double var2, double var4) {
      double var6 = Math.pow(Math.max(var0, 1.0E-4) / 0.14285714285714285, 0.3333333333333333);
      double var8 = 1.0;
      if (var4 > 1.0E-6 && var2 > 1.0E-6) {
         var8 = a(var4 / var2, 0.6, 1.8);
      }

      return a(var6 * var8, 0.25, 2.0);
   }

   public static double depthFactor(double var0) {
      return a(Math.pow(Math.max(var0, 0.5) / 2.0, 0.3333333333333333), 0.65, 1.35);
   }

   public static double at(double var0, double var2) {
      return a(var0 * depthFactor(var2), 0.25, 2.0);
   }

   private static double a(double var0, double var2, double var4) {
      return var0 < var2 ? var2 : (var0 > var4 ? var4 : var0);
   }
}
