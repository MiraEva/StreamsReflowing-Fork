package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.math.Hashing;

public final class Lake {
   private final int a;
   private final int b;
   private final int c;
   private final int d;
   private final int e;
   private final int f;
   private final int g;
   private final long[] h;
   private final int i;
   public static final int DEFAULT_ROUNDING = Math.max(0, Integer.getInteger("streamsreflowing.lakerounding", 6));
   private static final double j = 0.35;
   private final double k;
   private final int l;
   private volatile float[] m;
   private static final double n = 4.0;
   private static final double o = 3.0;
   private static final int p = 16;
   private static final int q = 2;

   public Lake(int var1, int var2, int var3, int var4, int var5, int var6, int var7, long[] var8) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, 0);
   }

   public Lake(int var1, int var2, int var3, int var4, int var5, int var6, int var7, long[] var8, int var9) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      this.e = var5;
      this.f = var6;
      this.g = var7;
      this.h = var8;
      this.i = Math.max(0, var9);
      double var10 = 0.0;
      if (var2 == 0 && this.i > 0 && var3 > 0) {
         long var12 = 0L;

         for (long var17 : var8) {
            var12 += Long.bitCount(var17);
         }

         double var19 = Math.sqrt(var12 / Math.PI);
         var10 = Math.min((double)this.i / var3, 0.35 * var19);
         if (var10 < 0.5) {
            var10 = 0.0;
         }
      }

      this.k = var10;
      this.l = var10 > 0.0 ? (int)Math.ceil(2.0 * var10) : 0;
   }

   public int rounding() {
      return this.i;
   }

   public int reachCells() {
      return 2 + this.l;
   }

   public int level() {
      return this.a;
   }

   public int digDepth() {
      return this.b;
   }

   public boolean covers(int var1, int var2) {
      int var3 = Math.floorDiv(var1 - this.d + (this.c >> 1), this.c);
      int var4 = Math.floorDiv(var2 - this.e + (this.c >> 1), this.c);
      if (var3 >= 0 && var3 < this.f && var4 >= 0 && var4 < this.g) {
         int var5 = var4 * this.f + var3;
         return (this.h[var5 >>> 6] & 1L << (var5 & 63)) != 0L;
      } else {
         return false;
      }
   }

   public boolean coversWithin(int var1, int var2, int var3) {
      int var4 = Math.floorDiv(var1 - this.d + (this.c >> 1), this.c);
      int var5 = Math.floorDiv(var2 - this.e + (this.c >> 1), this.c);

      for (int var6 = Math.max(0, var5 - var3); var6 <= Math.min(this.g - 1, var5 + var3); var6++) {
         for (int var7 = Math.max(0, var4 - var3); var7 <= Math.min(this.f - 1, var4 + var3); var7++) {
            int var8 = var6 * this.f + var7;
            if ((this.h[var8 >>> 6] & 1L << (var8 & 63)) != 0L) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean wetAt(int var1, int var2) {
      if (this.b > 0) {
         return this.covers(var1, var2);
      }

      int var3 = Math.floorDiv(var1 - this.d + (this.c >> 1), this.c);
      int var4 = Math.floorDiv(var2 - this.e + (this.c >> 1), this.c);
      int var5 = this.reachCells();
      return var3 >= -var5 && var3 < this.f + var5 && var4 >= -var5 && var4 < this.g + var5 ? this.coverageSmooth(var1, var2) > 0.5 : false;
   }

   public boolean wetWithin(int var1, int var2, int var3) {
      if (!this.coversWithin(var1, var2, var3 + this.reachCells())) {
         return false;
      }

      if (this.wetAt(var1, var2)) {
         return true;
      }

      int var4 = Math.floorDiv(var1 - this.d + (this.c >> 1), this.c);
      int var5 = Math.floorDiv(var2 - this.e + (this.c >> 1), this.c);

      for (int var6 = var5 - var3; var6 <= var5 + var3; var6++) {
         for (int var7 = var4 - var3; var7 <= var4 + var3; var7++) {
            if (this.wetAt(this.d + var7 * this.c, this.e + var6 * this.c)) {
               return true;
            }
         }
      }

      return false;
   }

   public double coverageSmooth(int var1, int var2) {
      long var3 = this.d * 341873128712L ^ this.e * 132897987541L;
      double var5 = this.c * 4.0 / 3.0;
      double var7 = var1 + (a(var3, var1, var2, 3) - 0.5) * 2.0 * var5;
      double var9 = var2 + (a(var3, var1, var2, 4) - 0.5) * 2.0 * var5;
      double var11 = (var7 - this.d) / this.c;
      double var13 = (var9 - this.e) / this.c;
      int var15 = (int)Math.floor(var11);
      int var16 = (int)Math.floor(var13);
      double var17 = var11 - var15;
      double var19 = var13 - var16;
      var17 = var17 * var17 * (3.0 - 2.0 * var17);
      var19 = var19 * var19 * (3.0 - 2.0 * var19);
      double var21;
      double var23;
      double var25;
      double var27;
      if (this.l > 0) {
         float[] var29 = this.a();
         var21 = this.a(var29, var15, var16);
         var23 = this.a(var29, var15 + 1, var16);
         var25 = this.a(var29, var15, var16 + 1);
         var27 = this.a(var29, var15 + 1, var16 + 1);
      } else {
         var21 = this.a(var15, var16);
         var23 = this.a(var15 + 1, var16);
         var25 = this.a(var15, var16 + 1);
         var27 = this.a(var15 + 1, var16 + 1);
      }

      double var35 = var21 * (1.0 - var17) + var23 * var17;
      double var31 = var25 * (1.0 - var17) + var27 * var17;
      return var35 * (1.0 - var19) + var31 * var19;
   }

   private static double a(long var0, int var2, int var3, int var4) {
      int var5 = Math.floorDiv(var2, 16);
      int var6 = Math.floorDiv(var3, 16);
      double var7 = (var2 - var5 * 16.0) / 16.0;
      double var9 = (var3 - var6 * 16.0) / 16.0;
      var7 = var7 * var7 * (3.0 - 2.0 * var7);
      var9 = var9 * var9 * (3.0 - 2.0 * var9);
      double var11 = Hashing.unit(var0, var5, var6, var4);
      double var13 = Hashing.unit(var0, var5 + 1, var6, var4);
      double var15 = Hashing.unit(var0, var5, var6 + 1, var4);
      double var17 = Hashing.unit(var0, var5 + 1, var6 + 1, var4);
      double var19 = var11 + (var13 - var11) * var7;
      double var21 = var15 + (var17 - var15) * var7;
      return var19 + (var21 - var19) * var9;
   }

   private float[] a() {
      float[] var1 = this.m;
      if (var1 != null) {
         return var1;
      }

      int var2 = this.l;
      int var3 = this.f + 2 * var2;
      int var4 = this.g + 2 * var2;
      double[] var5 = new double[2 * var2 + 1];
      double var6 = 0.0;

      for (int var8 = -var2; var8 <= var2; var8++) {
         double var9 = Math.exp(-(var8 * var8) / (2.0 * this.k * this.k));
         var5[var8 + var2] = var9;
         var6 += var9;
      }

      for (int var16 = 0; var16 < var5.length; var16++) {
         var5[var16] /= var6;
      }

      double[] var17 = new double[var3 * this.g];

      for (int var18 = 0; var18 < this.g; var18++) {
         for (int var10 = 0; var10 < var3; var10++) {
            double var11 = 0.0;

            for (int var13 = -var2; var13 <= var2; var13++) {
               var11 += var5[var13 + var2] * this.a(var10 - var2 + var13, var18);
            }

            var17[var18 * var3 + var10] = var11;
         }
      }

      var1 = new float[var3 * var4];

      for (int var19 = 0; var19 < var4; var19++) {
         for (int var20 = 0; var20 < var3; var20++) {
            double var21 = 0.0;

            for (int var22 = -var2; var22 <= var2; var22++) {
               int var14 = var19 - var2 + var22;
               if (var14 >= 0 && var14 < this.g) {
                  var21 += var5[var22 + var2] * var17[var14 * var3 + var20];
               }
            }

            var1[var19 * var3 + var20] = (float)var21;
         }
      }

      this.m = var1;
      return var1;
   }

   private double a(float[] var1, int var2, int var3) {
      int var4 = var2 + this.l;
      int var5 = var3 + this.l;
      int var6 = this.f + 2 * this.l;
      return var4 >= 0 && var4 < var6 && var5 >= 0 && var5 < this.g + 2 * this.l ? var1[var5 * var6 + var4] : 0.0;
   }

   private double a(int var1, int var2) {
      if (var1 >= 0 && var1 < this.f && var2 >= 0 && var2 < this.g) {
         int var3 = var2 * this.f + var1;
         return (this.h[var3 >>> 6] & 1L << (var3 & 63)) != 0L ? 1.0 : 0.0;
      } else {
         return 0.0;
      }
   }

   public int spacing() {
      return this.c;
   }

   public int cellMinX() {
      return this.d;
   }

   public int cellMinZ() {
      return this.e;
   }

   public int cellsWide() {
      return this.f;
   }

   public int cellsHigh() {
      return this.g;
   }

   public long[] mask() {
      return this.h;
   }
}
