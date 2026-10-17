package dev.streamsreflowing.worldgen.regionprep;

import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class FormSampler implements TerrainSampler {
   private final TerrainSampler a;
   private final FieldForm b;
   private final int c;
   private final int d;
   private final int e;
   private final int f;
   private final double g;
   private final int h;
   private final ConcurrentHashMap<Thread, FormSampler.Work> i = new ConcurrentHashMap<>();
   private static final int j = 0;
   private volatile boolean k;
   public static final LongAdder handedBack = new LongAdder();
   public static final LongAdder keptIr = new LongAdder();

   public FormSampler(TerrainSampler var1, FieldForm var2, int var3, int var4, int var5, int var6, double var7, int var9) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      this.e = var5;
      this.f = Math.max(1, var6);
      this.g = var7;
      this.h = var9;
   }

   private FormSampler.Work a() {
      Thread var1 = Thread.currentThread();
      FormSampler.Work var2 = this.i.get(var1);
      if (var2 == null) {
         this.i.keySet().removeIf(var0 -> !var0.isAlive());
         var2 = this.i.computeIfAbsent(var1, var1x -> new FormSampler.Work(this.b, (this.d - this.c) / this.e + 1));
      }

      return var2;
   }

   @Override
   public double height(int var1, int var2) {
      FormSampler.Work var3 = this.a();
      if (this.k && var3.a.compute(var1, this.h + 0, var2) <= this.g) {
         handedBack.increment();
         return this.a.height(var1, var2);
      } else {
         keptIr.increment();
         return var3.a.surfaceHeight(var1, var2, this.c, this.d, this.e, this.f, this.g, var3.b);
      }
   }

   public boolean aquiferHandBack() {
      return this.k;
   }

   public void setAquiferHandBack(boolean var1) {
      this.k = var1;
      handedBack.reset();
      keptIr.reset();
   }

   public double heightWithSemantics(int var1, int var2, boolean var3, boolean var4) {
      FormSampler.Work var5 = this.a();
      boolean var6 = var5.a.d;
      boolean var7 = var5.a.e;

      try {
         var5.a.d = var3;
         var5.a.e = var4;
         return var5.a.surfaceHeight(var1, var2, this.c, this.d, this.e, this.f, this.g, var5.b);
      } finally {
         var5.a.d = var6;
         var5.a.e = var7;
      }
   }

   public double density(int var1, int var2, int var3) {
      FormSampler.Work var4 = this.a();
      return var4.a.compute(var1, var2, var3);
   }

   public double solidThreshold() {
      return this.g;
   }

   @Override
   public double surfaceHeight(int var1, int var2) {
      return this.a.surfaceHeight(var1, var2);
   }

   @Override
   public double groundHeightReal(int var1, int var2) {
      return this.a.groundHeightReal(var1, var2);
   }

   @Override
   public double surfaceHeightReal(int var1, int var2) {
      return this.a.surfaceHeightReal(var1, var2);
   }

   @Override
   public double groundHeight(int var1, int var2) {
      return this.a.groundHeight(var1, var2);
   }

   @Override
   public double groundHeightExact(int var1, int var2) {
      return this.a.groundHeightExact(var1, var2);
   }

   @Override
   public boolean hasEstimate() {
      return this.a.hasEstimate();
   }

   @Override
   public boolean useHybridGridFill() {
      return this.a.useHybridGridFill();
   }

   @Override
   public double estimate(int var1, int var2) {
      return this.a.estimate(var1, var2);
   }

   @Override
   public boolean bakedCalibration() {
      return this.a.bakedCalibration();
   }

   @Override
   public boolean openAt(int var1, int var2, int var3) {
      return this.a.openAt(var1, var2, var3);
   }

   private static final class Work {
      final FormEval a;
      final double[] b;

      Work(FieldForm var1, int var2) {
         this.a = new FormEval(var1);
         this.b = new double[var2];
      }
   }
}
