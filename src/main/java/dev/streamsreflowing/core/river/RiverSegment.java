package dev.streamsreflowing.core.river;

public record RiverSegment(double x1, double z1, double y1, double w1, double x2, double z2, double y2, double w2, double sp1, double sp2) {

   public RiverSegment(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15) {
      this(var1, var3, var5, var7, var9, var11, var13, var15, 1.0, 1.0);
   }

   public RiverSegment {
z1 = z1;
w1 = w1;
z2 = z2;
w2 = w2;
sp2 = sp2;
}

   public double speedAt(double var1) {
      return this.sp1 + (this.sp2 - this.sp1) * var1;
   }

   public double clampParam(double var1, double var3) {
      double var5 = this.x2 - this.x1;
      double var7 = this.z2 - this.z1;
      double var9 = var5 * var5 + var7 * var7;
      if (var9 <= 1.0E-9) {
         return 0.0;
      }

      double var11 = ((var1 - this.x1) * var5 + (var3 - this.z1) * var7) / var9;
      return var11 < 0.0 ? 0.0 : (var11 > 1.0 ? 1.0 : var11);
   }

   public double distanceAt(double var1, double var3, double var5) {
      double var7 = this.x1 + (this.x2 - this.x1) * var5;
      double var9 = this.z1 + (this.z2 - this.z1) * var5;
      double var11 = var1 - var7;
      double var13 = var3 - var9;
      return Math.sqrt(var11 * var11 + var13 * var13);
   }

   public double gradeAt(double var1) {
      return this.y1 + (this.y2 - this.y1) * var1;
   }

   public double widthAt(double var1) {
      return this.w1 + (this.w2 - this.w1) * var1;
   }

   public double minX() {
      return Math.min(this.x1, this.x2);
   }

   public double maxX() {
      return Math.max(this.x1, this.x2);
   }

   public double minZ() {
      return Math.min(this.z1, this.z2);
   }

   public double maxZ() {
      return Math.max(this.z1, this.z2);
   }
}
