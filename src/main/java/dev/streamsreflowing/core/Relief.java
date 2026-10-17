package dev.streamsreflowing.core;

import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.grid.LowResGrid;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class Relief {
   private final int a;
   private final int b;
   private final int c;
   private final int d;
   private final int e;
   private final float[] f;

   private Relief(int var1, int var2, int var3, int var4, int var5, float[] var6) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      this.e = var5;
      this.f = var6;
   }

   public static Relief of(LowResGrid var0) {
      float[] var1 = new float[var0.nx * var0.nz];

      for (int var2 = 0; var2 < var1.length; var2++) {
         var1[var2] = (float)var0.height(var2);
      }

      return new Relief(var0.nx, var0.nz, var0.originX, var0.originZ, var0.spacing, var1);
   }

   public void write(DataOutput var1) throws IOException {
      var1.writeInt(this.a);
      var1.writeInt(this.b);
      var1.writeInt(this.c);
      var1.writeInt(this.d);
      var1.writeInt(this.e);

      for (float var5 : this.f) {
         var1.writeFloat(var5);
      }
   }

   public static Relief read(DataInput var0) throws IOException {
      return read(var0, 2147483639L);
   }

   public static Relief read(DataInput var0, long var1) throws IOException {
      int var3 = var0.readInt();
      if (var3 <= 0) {
         return null;
      }

      int var4 = var0.readInt();
      int var5 = var0.readInt();
      int var6 = var0.readInt();
      int var7 = var0.readInt();
      if (var4 > 0 && var7 > 0 && (long)var3 * var4 * 4L <= Math.min(var1, 2147483639L)) {
         float[] var8 = new float[var3 * var4];

         for (int var9 = 0; var9 < var8.length; var9++) {
            var8[var9] = var0.readFloat();
         }

         return new Relief(var3, var4, var5, var6, var7, var8);
      } else {
         throw new IOException("relief record out of range");
      }
   }

   public long approxBytes() {
      return MemBytes.object(24) + MemBytes.array(this.f.length, 4);
   }

   public static void writeAbsent(DataOutput var0) throws IOException {
      var0.writeInt(0);
   }

   boolean a(double var1, double var3) {
      return var1 >= this.c && var3 >= this.d && var1 <= this.c + (double)(this.a - 1) * this.e && var3 <= this.d + (double)(this.b - 1) * this.e;
   }

   double b(double var1, double var3) {
      double var5 = (var1 - this.c) / this.e;
      double var7 = (var3 - this.d) / this.e;
      int var9 = (int)Math.floor(var5);
      int var10 = (int)Math.floor(var7);
      var9 = Math.max(0, Math.min(this.a - 2, var9));
      var10 = Math.max(0, Math.min(this.b - 2, var10));
      double var11 = Math.max(0.0, Math.min(1.0, var5 - var9));
      double var13 = Math.max(0.0, Math.min(1.0, var7 - var10));
      double var15 = this.f[var9 + var10 * this.a];
      double var17 = this.f[var9 + 1 + var10 * this.a];
      double var19 = this.f[var9 + (var10 + 1) * this.a];
      double var21 = this.f[var9 + 1 + (var10 + 1) * this.a];
      double var23 = -999.0;
      return !(var15 <= var23) && !(var17 <= var23) && !(var19 <= var23) && !(var21 <= var23)
         ? (var15 * (1.0 - var11) + var17 * var11) * (1.0 - var13) + (var19 * (1.0 - var11) + var21 * var11) * var13
         : -1000.0;
   }
}
