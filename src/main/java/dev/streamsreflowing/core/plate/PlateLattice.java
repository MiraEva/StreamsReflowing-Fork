package dev.streamsreflowing.core.plate;

import dev.streamsreflowing.core.math.Hashing;

public final class PlateLattice {
   private final long a;
   private final double b;
   private final double c;
   private static final int d = 1023;
   private final PlateKey[] e = new PlateKey[1024];

   public PlateLattice(long var1, double var3, double var5) {
      this.a = var1;
      this.b = var3;
      this.c = Math.min(0.49, Math.max(0.0, var5));
   }

   public double scale() {
      return this.b;
   }

   public double seedX(int var1, int var2) {
      return (var1 + 0.5 + (Hashing.unit(this.a, var1, var2, 1L) - 0.5) * 2.0 * this.c) * this.b;
   }

   public double seedZ(int var1, int var2) {
      return (var2 + 0.5 + (Hashing.unit(this.a, var1, var2, 2L) - 0.5) * 2.0 * this.c) * this.b;
   }

   public PlateKey plateAt(double var1, double var3) {
      int var5 = (int)Math.floor(var1 / this.b);
      int var6 = (int)Math.floor(var3 / this.b);
      double var7 = Double.MAX_VALUE;
      int var9 = var5;
      int var10 = var6;

      for (int var11 = -1; var11 <= 1; var11++) {
         for (int var12 = -1; var12 <= 1; var12++) {
            int var13 = var5 + var12;
            int var14 = var6 + var11;
            double var15 = this.seedX(var13, var14) - var1;
            double var17 = this.seedZ(var13, var14) - var3;
            double var19 = var15 * var15 + var17 * var17;
            if (var19 < var7 || var19 == var7 && (var13 < var9 || var13 == var9 && var14 < var10)) {
               var7 = var19;
               var9 = var13;
               var10 = var14;
            }
         }
      }

      return new PlateKey(var9, var10);
   }

   public PlateKey[] nearestTwo(double var1, double var3) {
      int var5 = (int)Math.floor(var1 / this.b);
      int var6 = (int)Math.floor(var3 / this.b);
      double var7 = Double.MAX_VALUE;
      double var9 = Double.MAX_VALUE;
      PlateKey var11 = new PlateKey(var5, var6);
      PlateKey var12 = var11;

      for (int var13 = -1; var13 <= 1; var13++) {
         for (int var14 = -1; var14 <= 1; var14++) {
            int var15 = var5 + var14;
            int var16 = var6 + var13;
            double var17 = this.seedX(var15, var16) - var1;
            double var19 = this.seedZ(var15, var16) - var3;
            double var21 = var17 * var17 + var19 * var19;
            if (var21 < var7) {
               var9 = var7;
               var12 = var11;
               var7 = var21;
               var11 = new PlateKey(var15, var16);
            } else if (var21 < var9) {
               var9 = var21;
               var12 = new PlateKey(var15, var16);
            }
         }
      }

      return new PlateKey[]{var11, var12};
   }

   public PlateKey[] platesWithin(double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1 / this.b);
      int var8 = (int)Math.floor(var3 / this.b);
      double[] var9 = new double[9];
      double var10 = Double.MAX_VALUE;
      int var12 = 0;

      for (int var13 = -1; var13 <= 1; var13++) {
         for (int var14 = -1; var14 <= 1; var14++) {
            double var15 = this.seedX(var7 + var14, var8 + var13) - var1;
            double var17 = this.seedZ(var7 + var14, var8 + var13) - var3;
            double var19 = Math.sqrt(var15 * var15 + var17 * var17);
            var9[var12++] = var19;
            if (var19 < var10) {
               var10 = var19;
            }
         }
      }

      double var22 = var10 + 2.0 * var5;
      int var23 = 0;

      for (int var16 = 0; var16 < 9; var16++) {
         if (var9[var16] <= var22) {
            var23++;
         }
      }

      PlateKey[] var24 = new PlateKey[var23];
      int var25 = 0;
      var12 = 0;

      for (int var18 = -1; var18 <= 1; var18++) {
         for (int var26 = -1; var26 <= 1; var26++) {
            if (var9[var12++] <= var22) {
               var24[var25++] = this.a(var7 + var26, var8 + var18);
            }
         }
      }

      return var24;
   }

   private PlateKey a(int var1, int var2) {
      int var3 = (var1 * -1640531535 ^ var2 * -2048144777) & 1023;
      PlateKey var4 = this.e[var3];
      if (var4 != null && var4.cx() == var1 && var4.cz() == var2) {
         return var4;
      }

      PlateKey var5 = new PlateKey(var1, var2);
      this.e[var3] = var5;
      return var5;
   }

   public double borderDistance(double var1, double var3) {
      int var5 = (int)Math.floor(var1 / this.b);
      int var6 = (int)Math.floor(var3 / this.b);
      double var7 = Double.MAX_VALUE;
      double var9 = Double.MAX_VALUE;

      for (int var11 = -1; var11 <= 1; var11++) {
         for (int var12 = -1; var12 <= 1; var12++) {
            double var13 = this.seedX(var5 + var12, var6 + var11) - var1;
            double var15 = this.seedZ(var5 + var12, var6 + var11) - var3;
            double var17 = Math.sqrt(var13 * var13 + var15 * var15);
            if (var17 < var7) {
               var9 = var7;
               var7 = var17;
            } else if (var17 < var9) {
               var9 = var17;
            }
         }
      }

      return 0.5 * (var9 - var7);
   }
}
