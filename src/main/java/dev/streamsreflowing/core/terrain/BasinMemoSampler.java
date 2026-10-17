package dev.streamsreflowing.core.terrain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class BasinMemoSampler implements TerrainSampler {
   private final TerrainSampler a;
   private final ConcurrentHashMap<Long, Double> b = new ConcurrentHashMap<>();
   public static final LongAdder LEARNED = new LongAdder();
   public static final LongAdder HITS = new LongAdder();

   public BasinMemoSampler(TerrainSampler var1) {
      this.a = var1;
   }

   public static void resetCounters() {
      LEARNED.reset();
      HITS.reset();
   }

   public int memoSize() {
      return this.b.size();
   }

   @Override
   public double groundHeight(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      Double var5 = this.b.get(var3);
      if (var5 != null) {
         HITS.increment();
         return var5;
      }

      double var6 = this.a.groundHeight(var1, var2);
      if (this.b.putIfAbsent(var3, var6) == null) {
         LEARNED.increment();
      }

      return var6;
   }

   @Override
   public double height(int var1, int var2) {
      return this.a.height(var1, var2);
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
   public boolean batchCapable() {
      return this.a.batchCapable();
   }

   @Override
   public void heightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
      this.a.heightsBatch(var1, var2, var3, var4);
   }

   @Override
   public void groundHeightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
      this.a.groundHeightsBatch(var1, var2, var3, var4);
   }

   @Override
   public boolean openAt(int var1, int var2, int var3) {
      return this.a.openAt(var1, var2, var3);
   }

   @Override
   public int waterBiomeClass(int var1, int var2) {
      return this.a.waterBiomeClass(var1, var2);
   }

   @Override
   public int waterBiomeClassAt(int var1, int var2, int var3) {
      return this.a.waterBiomeClassAt(var1, var2, var3);
   }

   @Override
   public boolean outletBiomeOk(int var1, int var2, int var3) {
      return this.a.outletBiomeOk(var1, var2, var3);
   }

   @Override
   public double riverSurface(int var1, int var2) {
      return this.a.riverSurface(var1, var2);
   }
}
