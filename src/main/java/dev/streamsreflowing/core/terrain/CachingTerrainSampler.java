package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.core.ColumnKey;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class CachingTerrainSampler implements TerrainSampler {
   private final TerrainSampler a;
   private static final List<WeakReference<CachingTerrainSampler>> b = new CopyOnWriteArrayList<>();
   private final int c;
   private final double d;
   private final ConcurrentHashMap<Long, Double> e = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> f = new ConcurrentLinkedQueue<>();
   private final ConcurrentHashMap<Long, Double> g = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> h = new ConcurrentLinkedQueue<>();
   private final AtomicLong i = new AtomicLong();
   private final AtomicLong j = new AtomicLong();
   private final ConcurrentHashMap<Long, Double> k = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> l = new ConcurrentLinkedQueue<>();
   private final ConcurrentHashMap<Long, Double> m = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> n = new ConcurrentLinkedQueue<>();
   public static final LongAdder HITS_ALL = new LongAdder();
   public static final LongAdder MISSES_ALL = new LongAdder();

   public static long[] memoryStats() {
      long var0 = 0L;
      long var2 = 0L;

      for (WeakReference var5 : b) {
         CachingTerrainSampler var6 = (CachingTerrainSampler)var5.get();
         if (var6 != null) {
            var0++;
            var2 += var6.e.size() + var6.g.size() + var6.k.size() + var6.m.size();
         }
      }

      return new long[]{var0, var2, var2 * 110L};
   }

   private static long a(int var0, int var1) {
      return ColumnKey.of(var0, var1);
   }

   public CachingTerrainSampler(TerrainSampler var1, int var2) {
      this(var1, var2, Double.NaN);
   }

   public CachingTerrainSampler(TerrainSampler var1, int var2, double var3) {
      this.a = var1;
      this.c = var2;
      b.removeIf(var0 -> var0.get() == null);
      b.add(new WeakReference<>(this));
      this.d = var3;
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
   public double riverSurface(int var1, int var2) {
      return this.a.riverSurface(var1, var2);
   }

   @Override
   public boolean outletBiomeOk(int var1, int var2, int var3) {
      return this.a.outletBiomeOk(var1, var2, var3);
   }

   @Override
   public boolean batchCapable() {
      return this.a.batchCapable();
   }

   @Override
   public void heightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
      int[] var5 = new int[var4];
      int[] var6 = new int[var4];
      int[] var7 = new int[var4];
      int var8 = 0;

      for (int var9 = 0; var9 < var4; var9++) {
         Double var10 = this.e.get(a(var1[var9], var2[var9]));
         if (var10 != null) {
            this.i.incrementAndGet();
            HITS_ALL.increment();
            var3[var9] = var10;
         } else {
            var5[var8] = var1[var9];
            var6[var8] = var2[var9];
            var7[var8] = var9;
            var8++;
         }
      }

      if (var8 != 0) {
         double[] var13 = new double[var8];
         this.a.heightsBatch(var5, var6, var13, var8);

         for (int var14 = 0; var14 < var8; var14++) {
            double var11 = var13[var14];
            var3[var7[var14]] = var11;
            if (var11 != this.d) {
               if (this.e.putIfAbsent(a(var5[var14], var6[var14]), var11) != null) {
                  this.i.incrementAndGet();
                  HITS_ALL.increment();
               } else {
                  this.j.incrementAndGet();
                  MISSES_ALL.increment();
               }
            }
         }
      }
   }

   @Override
   public double height(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.e.get(var3);
      if (var5 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var5;
      }

      double var6 = this.a.height(var1, var2);
      if (var6 == this.d) {
         return var6;
      }

      Double var8 = this.e.putIfAbsent(var3, var6);
      if (var8 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var8;
      }

      this.j.incrementAndGet();
      MISSES_ALL.increment();
      this.f.offer(var3);

      while (this.e.size() > this.c) {
         Long var9 = this.f.poll();
         if (var9 == null) {
            break;
         }

         this.e.remove(var9);
      }

      return var6;
   }

   @Override
   public double surfaceHeight(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.g.get(var3);
      if (var5 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var5;
      }

      double var6 = this.a.surfaceHeight(var1, var2);
      Double var8 = this.g.putIfAbsent(var3, var6);
      if (var8 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var8;
      }

      this.j.incrementAndGet();
      MISSES_ALL.increment();
      this.h.offer(var3);

      while (this.g.size() > this.c) {
         Long var9 = this.h.poll();
         if (var9 == null) {
            break;
         }

         this.g.remove(var9);
      }

      return var6;
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
   public void groundHeightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
      int[] var5 = new int[var4];
      int[] var6 = new int[var4];
      int[] var7 = new int[var4];
      int var8 = 0;

      for (int var9 = 0; var9 < var4; var9++) {
         Double var10 = this.k.get(a(var1[var9], var2[var9]));
         if (var10 != null) {
            this.i.incrementAndGet();
            HITS_ALL.increment();
            var3[var9] = var10;
         } else {
            var5[var8] = var1[var9];
            var6[var8] = var2[var9];
            var7[var8] = var9;
            var8++;
         }
      }

      if (var8 != 0) {
         double[] var13 = new double[var8];
         this.a.groundHeightsBatch(var5, var6, var13, var8);

         for (int var14 = 0; var14 < var8; var14++) {
            double var11 = var13[var14];
            var3[var7[var14]] = var11;
            if (var11 != this.d) {
               if (this.k.putIfAbsent(a(var5[var14], var6[var14]), var11) != null) {
                  this.i.incrementAndGet();
                  HITS_ALL.increment();
               } else {
                  this.j.incrementAndGet();
                  MISSES_ALL.increment();
                  this.l.offer(a(var5[var14], var6[var14]));
               }
            }
         }
      }
   }

   @Override
   public double groundHeight(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.k.get(var3);
      if (var5 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var5;
      }

      double var6 = this.a.groundHeight(var1, var2);
      if (var6 == this.d) {
         return var6;
      }

      Double var8 = this.k.putIfAbsent(var3, var6);
      if (var8 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var8;
      }

      this.j.incrementAndGet();
      MISSES_ALL.increment();
      this.l.offer(var3);

      while (this.k.size() > this.c) {
         Long var9 = this.l.poll();
         if (var9 == null) {
            break;
         }

         this.k.remove(var9);
      }

      return var6;
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
   public boolean openAt(int var1, int var2, int var3) {
      return this.a.openAt(var1, var2, var3);
   }

   @Override
   public double groundHeightExact(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.m.get(var3);
      if (var5 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var5;
      }

      double var6 = this.a.groundHeightExact(var1, var2);
      if (var6 == this.d) {
         return var6;
      }

      Double var8 = this.m.putIfAbsent(var3, var6);
      if (var8 != null) {
         this.i.incrementAndGet();
         HITS_ALL.increment();
         return var8;
      }

      this.j.incrementAndGet();
      MISSES_ALL.increment();
      this.n.offer(var3);

      while (this.m.size() > this.c) {
         Long var9 = this.n.poll();
         if (var9 == null) {
            break;
         }

         this.m.remove(var9);
      }

      return var6;
   }

   @Override
   public boolean bakedCalibration() {
      return this.a.bakedCalibration();
   }

   public long hits() {
      return this.i.get();
   }

   public long misses() {
      return this.j.get();
   }

   public int hitPercent() {
      long var1 = this.i.get();
      long var3 = var1 + this.j.get();
      return var3 > 0L ? (int)(var1 * 100L / var3) : 0;
   }
}
