package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.config.HydraulicGeometry;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.math.Hashing;
import java.util.Locale;

public final class ChannelBed {
   static final double a = 0.35;
   private static final double c = 0.6;
   private static final double d = 1.0;
   private static final double e = 0.3;
   private static final double f = 0.6;
   private static final double g = 0.6;
   private static final double h = 0.8;
   private static final double i = 0.8;
   private static final double j = 0.15;
   private static final double k = 0.12;
   private static final double l = 0.25;
   private static final double m = 0.25;
   private static final double n = 0.6;
   private static final double o = 0.04;
   private static final double p = 0.1;
   private static final double q = 0.04;
   private static final double r = 2.0;
   private static final double s = 0.35;
   private static final double t = 1.0;
   static final double b = 96.0;
   private static final double u = 2.0;
   private static final double v = 5.0;
   private static final double w = 0.0;
   private static final double x = 0.0;
   private static final double y = 6.0;
   private static final int z = 2;
   private static final double A = 1.2;
   private static final double B = 2.2;
   private static final double C = 0.75;
   public static final double BRAID_CREST = 2.0;
   private static final double D = 0.8;
   private static final double E = 0.9;
   private static final double F = -0.4;
   private static final double G = 3.0;
   private static final double H = 0.3;
   private static final double I = 1.5;
   private static final double J = 4.0;
   private static final double K = 0.4;
   private static final double L = 2.0;
   private static final double M = 4.0;
   private static final double N = 96.0;
   private static final double O = 0.3;
   private static final double P = 28.0;
   private static final double Q = 64.0;
   private static final double R = 0.28;
   private static final double S = 1.2;
   private static final double T = 64.0;
   private static final double U = 32.0;
   private static final double V = 16.0;
   private static final double W = 8.0;

   private ChannelBed() {
   }

   public static double thalwegShift(ColumnPlan var0, StreamsConfig var1, double var2) {
      HydraulicGeometry var4 = var1.hydraulics();
      if (var4 != null && !(var4.bendAsymmetry() <= 0.0) && var0.hasLateral()) {
         double var5 = Math.abs(var0.curvature());
         double var7 = b(var5 / 0.35);
         double var9 = a(var5, 0.6, 1.0);
         double var11 = 0.3 + 0.7 * b(var2);
         double var13 = var0.curvature() > 0.0 ? -1.0 : 1.0;
         return var4.bendAsymmetry() * 0.6 * var7 * var9 * var11 * var13;
      } else {
         return 0.0;
      }
   }

   public static double centreDepth(ColumnPlan var0, StreamsConfig var1, double var2) {
      double var4 = 2.0 * var0.width() * var1.streamDepth();
      HydraulicGeometry var6 = var1.hydraulics();
      if (var6 != null && var6.depthRatio() > 0.0) {
         double var7 = Math.min(1.0, Math.max(0.0, var0.slope()) / 0.12);
         double var9 = Math.max(0.0, Math.min(1.0, var2));
         var4 *= 1.0 + var6.depthRatio() * (0.25 * (2.0 * var7 - 1.0) + 0.25 * (2.0 * var9 - 1.0));
      }

      return var4;
   }

   public static int bowl(ColumnPlan var0, StreamsConfig var1, double var2) {
      double var4 = var0.width() > 1.0E-6 ? Math.min(1.0, var0.distance() / var0.width()) : 1.0;
      HydraulicGeometry var6 = var1.hydraulics();
      if (var6 != null && var6.shapesBed()) {
         double var14 = a(var0, var1, var2, 1.0);
         double var9 = thalwegShift(var0, var1, var2);
         double var11 = var0.hasLateral() ? var0.lateral() : var4;
         boolean var13 = var9 != 0.0 && (var11 - var9) * var9 < 0.0;
         if (Math.abs(var9) >= 0.15) {
            return var13 ? (int)Math.floor(var14) : (int)Math.ceil(var14);
         } else {
            return (int)Math.round(var14);
         }
      } else {
         int var7 = Math.max(1, (int)Math.round(2.0 * var0.width() * var1.streamDepth()));
         return (int)Math.round(var7 * (1.0 - var4 * var4));
      }
   }

   private static double a(ColumnPlan var0, StreamsConfig var1, double var2) {
      double var4 = Math.max(1L, Math.round(centreDepth(var0, var1, var2)));
      double var6 = thalwegShift(var0, var1, var2);
      double var8 = Math.abs(var6) / 0.6;
      return var4 * (1.0 + 0.6 * var8);
   }

   static double a(ColumnPlan var0, StreamsConfig var1, double var2, double var4) {
      double var6 = var0.width() > 1.0E-6 ? Math.min(1.0, var0.distance() / var0.width()) : 1.0;
      int var8 = Math.max(1, (int)Math.round(centreDepth(var0, var1, var2)));
      double var9 = thalwegShift(var0, var1, var2) * var4;
      double var11 = var0.hasLateral() ? var0.lateral() : var6;
      double var13 = var11 - var9 >= 0.0 ? 1.0 - var9 : 1.0 + var9;
      double var15 = var13 > 1.0E-6 ? Math.min(1.0, Math.abs(var11 - var9) / var13) : 1.0;
      double var17 = Math.abs(var9) / 0.6;
      double var19 = var8 * (1.0 + 0.6 * var17);
      boolean var21 = var9 != 0.0 && (var11 - var9) * var9 < 0.0;
      double var22 = var21 ? Math.pow(1.0 - var15 * var15, 1.0 + 0.8 * var17) : 1.0 - var15 * var15;
      return var19 * var22;
   }

   public static double bankSlopeFactor(ColumnPlan var0, StreamsConfig var1, double var2) {
      HydraulicGeometry var4 = var1.hydraulics();
      if (var4 != null && !(var4.bendAsymmetry() <= 0.0) && var0.hasLateral()) {
         double var5 = Math.abs(var0.curvature());
         double var7 = b(var5 / 0.35) * a(var5, 0.6, 1.0) * (0.3 + 0.7 * b(var2));
         double var9 = var0.lateral() * var0.curvature() > 0.0 ? -1.0 : 1.0;
         return 1.0 + var4.bendAsymmetry() * 0.8 * var7 * var9;
      } else {
         return 1.0;
      }
   }

   public static double braidIndex(ColumnPlan var0, StreamsConfig var1, double var2, double var4, double var6, int var8, int var9) {
      HydraulicGeometry var10 = var1.hydraulics();
      if (var10 != null && (!(var10.channelBars() <= 0.0) || !(var10.deltaMouths() <= 0.0)) && var0.inChannel() && var0.hasLateral() && !var0.source()) {
         double var11 = var0.width();
         if (var0.distToJunction() < 0.0) {
            return 0.0;
         }

         boolean var13 = var0.waterSurfaceY() <= var1.seaLevel();
         double var14 = b((0.6 - var2) / 0.6);
         double var16 = b((var11 - 2.0) / 3.0);
         double var18 = var13 ? 1.0 : a(var0.slope(), 0.0, 0.04);
         double var20 = var13 ? 1.0 : a(var0.upstreamSlope(), 0.04, 0.1);
         double var22 = a(var8 - var9, 2.0, 4.0);
         double var24 = !var13 && var0.distToOutlet() != Double.MAX_VALUE ? b(var0.distToOutlet(), 0.0, 12.0) : 1.0;
         double var26 = Math.max(4.0, var11);
         double var28 = var26 / (1.2 * var11);
         double var30 = Math.abs(var0.curvature()) * a(Math.abs(var0.curvature()), 0.6, 1.0);
         double var32 = a(var30, var28 * 0.35, var28);
         double var34 = Math.min(1.0, 0.55 + 0.45 * (1.0 - b(var4)) + 0.35 * b(var6));
         double var36 = var14 * var16 * var18 * var20 * var22 * var24 * var32;
         double var38 = var13 ? 1.0 : Math.min(1.0, var0.flatRun() / 96.0);
         double var40 = var36 * var34 * var38 * Math.sqrt(var10.channelBars());
         if (var10.deltaMouths() > 0.0) {
            double var42 = 1.0;
            if (!var13) {
               double var44 = var0.distToOutlet() < 64.0 ? 1.0 - var0.distToOutlet() / 64.0 : 0.0;
               var42 = var44 * var44 * (3.0 - 2.0 * var44);
            }

            var40 = Math.max(var40, var36 * var42 * Math.sqrt(var10.deltaMouths()));
         }

         return b(var40);
      } else {
         return 0.0;
      }
   }

   public static String braidDiag(ColumnPlan var0, StreamsConfig var1, double var2, double var4, double var6, int var8, int var9, double var10) {
      HydraulicGeometry var12 = var1.hydraulics();
      double var13 = var0.width();
      double var15 = braidIndex(var0, var1, var2, var4, var6, var8, var9);
      return String.format(
         Locale.ROOT,
         "BRAID index=%.3f emerge=%.3f | dial bars=%.2f delta=%.2f | coh=%.2f(max %.2f) wet=%.2f cold=%.2f | w=%.2f(min %.1f full %.1f) slope=%.4f(max %.3f) upSlope=%.3f(max %.2f) flatRun=%.0f(ref %.0f) | distJunction=%s(clear %.1f) distOutlet=%s(clear %.1f) ground-surface=%d(min -%d) | lateral=%.2f curv=%.3f(max %.2f for this width) source=%s inChannel=%s",
         var15,
         var15 * b(var10),
         var12 == null ? 0.0 : var12.channelBars(),
         var12 == null ? 0.0 : var12.deltaMouths(),
         var2,
         0.6,
         var4,
         var6,
         var13,
         2.0,
         5.0,
         var0.slope(),
         0.04,
         var0.upstreamSlope(),
         0.1,
         var0.flatRun(),
         96.0,
         var0.distToJunction() == Double.MAX_VALUE ? "far" : String.format(Locale.ROOT, "%.1f", var0.distToJunction()),
         Math.max(0.0, 0.0 * var13),
         var0.distToOutlet() == Double.MAX_VALUE ? "far" : String.format(Locale.ROOT, "%.1f", var0.distToOutlet()),
         6.0,
         var9 - var8,
         2,
         var0.lateral(),
         var0.curvature(),
         Math.max(4.0, var13) / (1.2 * var13),
         var0.source(),
         var0.inChannel()
      );
   }

   public static int bedDepth(
      ColumnPlan var0, StreamsConfig var1, double var2, double var4, double var6, long var8, int var10, int var11, int var12, int var13, double var14
   ) {
      long var16 = System.nanoTime();

      try {
         return a(var0, var1, var2, var4, var6, var8, var10, var11, var12, var13, var14);
      } finally {
         CarveCensus.bedEval(var16);
      }
   }

   private static int a(
      ColumnPlan var0, StreamsConfig var1, double var2, double var4, double var6, long var8, int var10, int var11, int var12, int var13, double var14
   ) {
      double var16 = braidIndex(var0, var1, var2, var4, var6, var12, var13);
      double var18 = var16 * b(var14);
      if (var16 <= 0.0) {
         return Math.max(1, bowl(var0, var1, var2));
      }

      double var20 = Math.sqrt(var16);
      double var22 = a(var0, var1, var2, 1.0);
      double var24 = Math.max(1.0E-6, a(var0, var1, var2));
      double var26 = b(var22 / var24);
      double var28 = var24 * (1.0 - var20) + HydraulicGeometry.braidFloor(var24) * var20;
      double var30 = var28 * Math.pow(var26, 1.0 - 0.75 * var20);
      double var32 = Math.max(1.0, var30);
      double var34 = a(var8, var10, var11, 64.0, 45) * (Math.PI * 2);
      double var36 = Math.sin(var0.lateral() * var0.width() * (Math.PI * 2) / 28.0 + var34);
      double var38 = 0.7 * a(var8, var10, var11) + 0.3 * var36;
      double var40 = 0.9 - 1.3 * Math.sqrt(b(var18));
      double var42 = b((a(var8, var10, var11, 96.0, 44) - 0.28) / Math.max(0.05, 0.43999999999999995));
      var42 = b(var42 - thalwegShift(var0, var1, var2) * 1.2);
      double var44 = Math.min(1.5, Math.max(0.0, 2.0 - 4.0 / Math.max(1.0E-6, var0.width())));
      double var46 = -1.0 + (2.0 - var44) * var42;
      double var48 = Math.min(4.0, var44 * var0.width() * 0.5);
      double var50 = Math.min(var0.lateral() - var46, var46 + var44 - var0.lateral()) * var0.width();
      double var52 = var44 > 0.0 && var48 > 1.0E-6 ? b(var50 / var48) : 0.0;
      double var54 = Math.max(0.0, Math.min(Math.min(2.0, var13 - var12 + 1.0), var48 / 3.0));
      double var56;
      if (var38 > var40) {
         double var58 = b((var38 - var40) / 0.8);
         var56 = (var32 + var54) * a(var58);
      } else {
         var56 = -(var32 + var54) * 0.3 * var16 * b((var40 - var38) / 0.5);
      }

      var56 *= var52;
      double var66 = var32 - var56;
      double var60 = (1.0 - Math.abs(var0.lateral())) * var0.width();
      double var62 = Math.max(var32, var28 * (1.0 + 0.4 * var18) * b(var60 / 2.0));
      var66 = var66 * var52 + var62 * (1.0 - var52);
      if (var66 < 1.0 && (var56 <= 0.0 || var52 <= 0.0)) {
         var66 = 1.0;
      }

      return (int)Math.round(var66);
   }

   private static double a(long var0, int var2, int var3) {
      double var4 = 0.6 * a(var0, var2, var3, 32.0, 43) + 0.3 * a(var0, var2, var3, 16.0, 41) + 0.1 * a(var0, var2, var3, 8.0, 42);
      return 2.0 * var4 - 1.0;
   }

   private static double a(long var0, int var2, int var3, double var4, int var6) {
      double var7 = var2 / var4;
      double var9 = var3 / var4;
      int var11 = (int)Math.floor(var7);
      int var12 = (int)Math.floor(var9);
      double var13 = var7 - var11;
      double var15 = var9 - var12;
      var13 = var13 * var13 * (3.0 - 2.0 * var13);
      var15 = var15 * var15 * (3.0 - 2.0 * var15);
      double var17 = Hashing.unit(var0, var11, var12, var6);
      double var19 = Hashing.unit(var0, var11 + 1, var12, var6);
      double var21 = Hashing.unit(var0, var11, var12 + 1, var6);
      double var23 = Hashing.unit(var0, var11 + 1, var12 + 1, var6);
      return (var17 + (var19 - var17) * var13) * (1.0 - var15) + (var21 + (var23 - var21) * var13) * var15;
   }

   private static double a(double var0) {
      return var0 * var0 * (3.0 - 2.0 * var0);
   }

   private static double b(double var0) {
      return var0 < 0.0 ? 0.0 : (var0 > 1.0 ? 1.0 : var0);
   }

   private static double a(double var0, double var2, double var4) {
      if (var4 <= var2) {
         return var0 <= var2 ? 1.0 : 0.0;
      }

      double var6 = b((var4 - var0) / (var4 - var2));
      return var6 * var6 * (3.0 - 2.0 * var6);
   }

   private static double b(double var0, double var2, double var4) {
      if (var4 <= var2) {
         return var0 >= var4 ? 1.0 : 0.0;
      }

      double var6 = b((var0 - var2) / (var4 - var2));
      return var6 * var6 * (3.0 - 2.0 * var6);
   }

   public static double overlapFactor(ColumnPlan var0) {
      return var0 != null && var0.inCorridor() && !(var0.width() <= 1.0E-6) ? b(var0.distance() / var0.width(), 1.0, 2.0) : 1.0;
   }

   public static boolean onPointBar(ColumnPlan var0, StreamsConfig var1) {
      HydraulicGeometry var2 = var1.hydraulics();
      return var2 != null && !(var2.bendAsymmetry() <= 0.0) && var0.hasLateral() && var0.inChannel()
         ? Math.abs(var0.curvature()) >= 0.13999999999999999 && var0.lateral() * var0.curvature() > 0.0 && Math.abs(var0.lateral()) > 0.3
         : false;
   }
}
