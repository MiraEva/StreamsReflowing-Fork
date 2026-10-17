package dev.streamsreflowing.core;

public final class ColumnCache {
   private long[] a;
   private double[] b;
   private boolean[] c;
   private int d;
   private int e;
   private int f;

   public ColumnCache() {
      this(1024);
   }

   public ColumnCache(int var1) {
      int var2 = Integer.highestOneBit(Math.max(16, var1 - 1)) << 1;
      this.a = new long[var2];
      this.b = new double[var2];
      this.c = new boolean[var2];
      this.d = var2 - 1;
      this.f = var2 >>> 1;
   }

   private int a(long var1) {
      int var3 = (int)var1 & this.d;

      while (this.c[var3] && this.a[var3] != var1) {
         var3 = var3 + 1 & this.d;
      }

      return var3;
   }

   public double get(long var1, double var3) {
      int var5 = this.a(var1);
      return this.c[var5] ? this.b[var5] : var3;
   }

   public void put(long var1, double var3) {
      int var5 = this.a(var1);
      if (!this.c[var5]) {
         this.c[var5] = true;
         this.a[var5] = var1;
         this.e++;
         if (this.e > this.f) {
            this.a();
            var5 = this.a(var1);
         }
      }

      this.b[var5] = var3;
   }

   private void a() {
      long[] var1 = this.a;
      double[] var2 = this.b;
      boolean[] var3 = this.c;
      int var4 = var1.length << 1;
      this.a = new long[var4];
      this.b = new double[var4];
      this.c = new boolean[var4];
      this.d = var4 - 1;
      this.f = var4 >>> 1;

      for (int var5 = 0; var5 < var1.length; var5++) {
         if (var3[var5]) {
            int var6 = this.a(var1[var5]);
            this.c[var6] = true;
            this.a[var6] = var1[var5];
            this.b[var6] = var2[var5];
         }
      }
   }

   public int size() {
      return this.e;
   }
}
