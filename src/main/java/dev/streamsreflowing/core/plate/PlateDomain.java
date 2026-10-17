package dev.streamsreflowing.core.plate;

public final class PlateDomain {
   private final PlateLattice a;
   private final PlateKey b;
   public final double centerX;
   public final double centerZ;
   public final int minX;
   public final int minZ;
   public final int maxX;
   public final int maxZ;

   public PlateDomain(PlateLattice var1, PlateKey var2) {
      this.a = var1;
      this.b = var2;
      this.centerX = var1.seedX(var2.cx(), var2.cz());
      this.centerZ = var1.seedZ(var2.cx(), var2.cz());
      double var3 = var1.scale();
      this.minX = (int)Math.floor(this.centerX - var3);
      this.minZ = (int)Math.floor(this.centerZ - var3);
      this.maxX = (int)Math.ceil(this.centerX + var3);
      this.maxZ = (int)Math.ceil(this.centerZ + var3);
   }

   public PlateKey key() {
      return this.b;
   }

   public boolean contains(double var1, double var3) {
      return this.a.plateAt(var1, var3).equals(this.b);
   }

   public double borderDistance(double var1, double var3) {
      return this.a.borderDistance(var1, var3);
   }
}
