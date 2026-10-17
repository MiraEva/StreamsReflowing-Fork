package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.Constants;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class ChunkHeightCache {
   private volatile int a = Integer.MAX_VALUE;
   private volatile double b = 2.13;
   private volatile int c = Integer.MAX_VALUE;
   private static final Integer d = Integer.getInteger("streamsreflowing.cornerfillafter");
   private static final short e = -32768;
   private final ConcurrentHashMap<Long, short[]> f = new ConcurrentHashMap<>();
   private final Set<Long> g = ConcurrentHashMap.newKeySet();
   private static final LongAdder h = new LongAdder();
   private static final LongAdder i = new LongAdder();
   private static final LongAdder j = new LongAdder();
   private static final LongAdder k = new LongAdder();
   private static final LongAdder l = new LongAdder();
   private static final Integer m = Integer.getInteger("streamsreflowing.chunkfillafter");
   private static final int n = 8192;
   private final ServerLevel o;
   private final ConcurrentHashMap<Long, short[]> p = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Long, AtomicInteger> q = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> r = new ConcurrentLinkedQueue<>();
   private static final LongAdder s = new LongAdder();
   private static final LongAdder t = new LongAdder();
   private static final LongAdder u = new LongAdder();
   private static final DoubleAdder v = new DoubleAdder();
   private static final LongAdder w = new LongAdder();

   public ChunkHeightCache(ServerLevel var1) {
      this.o = var1;
      if (d != null) {
         this.c = Math.max(1, d);
      }

      if (m != null) {
         this.a = Math.max(2, m);
         v.add(this.b);
         w.increment();
         if (d != null) {
            return;
         }
      }

      Thread var2 = new Thread(() -> this.a(var1), "sr-chunkfill-calibrate");
      var2.setDaemon(true);
      var2.start();
   }

   private void a(ServerLevel var1) {
      try {
         ChunkGenerator var2 = var1.getChunkSource().getGenerator();
         RandomState var3 = var1.getChunkSource().randomState();
         long var4 = 0L;
         long var6 = 0L;
         int var8 = 0;
         int var9 = 0;
         this.a(var1, var2, var3);
         if (m != null) {
            return;
         }

         for (int var10 = 0; var10 < 4; var10++) {
            int var11 = 512 + var10 * 37;
            int var12 = 512 + var10 * 53;
            long var13 = System.nanoTime();
            int[] var15 = ChunkHeightFill.heights(var1, new ChunkPos(var11, var12));
            long var16 = System.nanoTime();
            if (var15 == null) {
               return;
            }

            var4 += var16 - var13;
            var8++;

            for (int var18 = 0; var18 < 4; var18++) {
               int var19 = (var11 << 4) + 2048 + var18 * 149;
               int var20 = (var12 << 4) + 2048 + var18 * 211;
               long var21 = System.nanoTime();
               var2.getBaseHeight(var19, var20, Types.OCEAN_FLOOR_WG, var1, var3);
               var6 += System.nanoTime() - var21;
               var9++;
            }
         }

         double var24 = var4 / 1000000.0 / Math.max(1, var8);
         double var25 = var6 / 1000000.0 / Math.max(1, var9);
         if (var25 <= 0.0 || var24 <= 0.0) {
            return;
         }

         double var14 = var24 / var25;
         int var26 = Math.max(2, Math.min(8, (int)Math.ceil(var14)));
         this.b = var14;
         this.a = var26;
         v.add(var14);
         w.increment();
         Constants.LOG
            .info(
               "Streams Reflowing: a chunk's heights cost {} reads here, so chunks are filled after {} reads ({} ms a fill, {} ms a read).",
               new Object[]{
                  String.format(Locale.ROOT, "%.2f", var14), var26, String.format(Locale.ROOT, "%.1f", var24), String.format(Locale.ROOT, "%.1f", var25)
               }
            );
      } catch (Throwable var23) {
      }
   }

   private void a(ServerLevel var1, ChunkGenerator var2, RandomState var3) {
      if (d == null && ReadLattice.CORNER_BATCH) {
         try {
            long var4 = 0L;
            long var6 = 0L;
            int var8 = 0;
            int var9 = 0;

            for (int var10 = -2; var10 < 6; var10++) {
               int var11 = -600 + var10 * 41;
               int var12 = 700 + var10 * 29;
               long var13 = System.nanoTime();
               int[] var15 = ChunkHeightFill.cornerHeights(var1, new ChunkPos(var11, var12));
               long var16 = System.nanoTime();
               if (var15 == null) {
                  return;
               }

               long var18 = 0L;

               for (int var20 = 0; var20 < 4; var20++) {
                  int var21 = (var11 + 9 << 4) + var20 * 16 * 7;
                  int var22 = (var12 - 9 << 4) + var20 * 16 * 5;
                  long var23 = System.nanoTime();
                  var2.getBaseHeight(var21, var22, Types.OCEAN_FLOOR_WG, var1, var3);
                  var18 += System.nanoTime() - var23;
               }

               if (var10 >= 0) {
                  var4 += var16 - var13;
                  var8++;
                  var6 += var18;
                  var9 += 4;
               }
            }

            double var26 = var4 / 1000000.0 / Math.max(1, var8);
            double var27 = var6 / 1000000.0 / Math.max(1, var9);
            if (var26 <= 0.0 || var27 <= 0.0) {
               return;
            }

            double var14 = var26 / var27;
            this.c = Math.max(2, (int)Math.ceil(var14));
            Constants.LOG
               .info(
                  "Streams Reflowing: a chunk's corner heights cost {} corner reads here ({} ms a batch, {} ms a read).",
                  new Object[]{String.format(Locale.ROOT, "%.2f", var14), String.format(Locale.ROOT, "%.2f", var26), String.format(Locale.ROOT, "%.2f", var27)}
               );
         } catch (Throwable var25) {
         }
      }
   }

   public static void report(String var0) {
      long var1 = h.sum();
      if (var1 > 0L || k.sum() > 0L) {
         Constants.LOG
            .info(
               "[SR-CORNERHEIGHTS] {} -- {} corner batches ({} for stream chunks, {} ms each), {} single corner reads, {} reads answered from memory",
               new Object[]{var0, var1, i.sum(), String.format(Locale.ROOT, "%.2f", var1 == 0L ? 0.0 : l.sum() / 1000000.0 / var1), k.sum(), j.sum()}
            );
      }

      long var3 = s.sum();
      long var5 = t.sum();
      long var7 = u.sum();
      if (var3 != 0L || var7 != 0L) {
         double var9 = var5 + var7;
         double var11 = var7 + var3 * v.sum() / Math.max(1L, w.sum());
         Constants.LOG
            .info(
               "[SR-CHUNKHEIGHTS] {} -- {} columns from {} fills, {} read the ordinary way | {} reads' worth of work instead of {} ({}%)",
               new Object[]{var0, var5, var3, var7, Math.round(var11), Math.round(var9), var9 <= 0.0 ? 0L : Math.round(100.0 * (var9 - var11) / var9)}
            );
      }
   }

   public double heightOrNaN(int var1, int var2) {
      int var3 = ReadLattice.cell();
      if (var3 > 1) {
         return this.a(var1, var2, var3);
      }

      long var4 = a(var1 >> 4, var2 >> 4);
      short[] var6 = this.p.get(var4);
      if (var6 != null) {
         t.increment();
         return var6[(var2 & 15) * 16 + (var1 & 15)];
      }

      if (this.q.computeIfAbsent(var4, var0 -> new AtomicInteger()).incrementAndGet() < this.a) {
         u.increment();
         return Double.NaN;
      }

      try {
         int var7 = var1 >> 4;
         int var8 = var2 >> 4;
         int[] var9 = ChunkHeightFill.heights(this.o, new ChunkPos(var7, var8));
         if (var9 == null) {
            u.increment();
            return Double.NaN;
         }

         s.increment();
         short[] var10 = new short[256];

         for (int var11 = 0; var11 < 256; var11++) {
            var10[var11] = (short)var9[var11];
         }

         short[] var15 = this.p.putIfAbsent(var4, var10);
         short[] var12 = var15 != null ? var15 : var10;
         if (var15 == null) {
            this.r.offer(var4);

            while (this.p.size() > 8192) {
               Long var13 = this.r.poll();
               if (var13 == null) {
                  break;
               }

               this.p.remove(var13);
               this.q.remove(var13);
            }
         }

         t.increment();
         return var12[(var2 & 15) * 16 + (var1 & 15)];
      } catch (Throwable var14) {
         u.increment();
         return Double.NaN;
      }
   }

   private static boolean a(int var0) {
      return var0 > 1 && var0 <= 16 && 16 % var0 == 0;
   }

   private double a(int var1, int var2, int var3) {
      if (a(var3) && (var1 & var3 - 1) == 0 && (var2 & var3 - 1) == 0 && Integer.bitCount(var3) == 1) {
         int var4 = 16 / var3;
         int var5 = (var2 & 15) / var3 * var4 + (var1 & 15) / var3;
         long var6 = a(var1 >> 4, var2 >> 4);
         short[] var8 = this.f.get(var6);
         if (var8 != null && var8.length == var4 * var4 && var8[var5] != -32768) {
            j.increment();
            return var8[var5];
         }

         if (this.g.contains(var6)) {
            return Double.NaN;
         }

         boolean var9 = ReadLattice.inStreamChunks() && this.c != Integer.MAX_VALUE;
         if (!var9 && this.q.computeIfAbsent(var6, var0 -> new AtomicInteger()).incrementAndGet() < this.c) {
            return Double.NaN;
         }

         try {
            int var10 = var1 >> 4;
            int var11 = var2 >> 4;
            long var12 = System.nanoTime();
            int[] var14 = ChunkHeightFill.cornerHeights(this.o, new ChunkPos(var10, var11));
            if (var14 != null && var14.length == var4 * var4) {
               l.add(System.nanoTime() - var12);
               h.increment();
               if (var9) {
                  i.increment();
               }

               short[] var15 = new short[var4 * var4];

               for (int var16 = 0; var16 < var15.length; var16++) {
                  var15[var16] = (short)var14[var16];
               }

               if (this.f.put(var6, var15) == null) {
                  this.a(var6);
               }

               this.g.add(var6);
               j.increment();
               return var15[var5];
            } else {
               return Double.NaN;
            }
         } catch (Throwable var17) {
            return Double.NaN;
         }
      } else {
         u.increment();
         return Double.NaN;
      }
   }

   public void remember(int var1, int var2, double var3) {
      int var5 = ReadLattice.cell();
      if (a(var5) && Integer.bitCount(var5) == 1 && (var1 & var5 - 1) == 0 && (var2 & var5 - 1) == 0) {
         k.increment();
         int var6 = 16 / var5;
         long var7 = a(var1 >> 4, var2 >> 4);
         short[] var9 = this.f.get(var7);
         if (var9 == null || var9.length != var6 * var6) {
            short[] var10 = new short[var6 * var6];
            Arrays.fill(var10, (short)-32768);
            short[] var11 = this.f.putIfAbsent(var7, var10);
            if (var11 == null) {
               this.a(var7);
               var9 = var10;
            } else {
               var9 = var11;
            }
         }

         if (var9.length == var6 * var6) {
            var9[(var2 & 15) / var5 * var6 + (var1 & 15) / var5] = (short)Math.round(var3);
         }
      }
   }

   private void a(long var1) {
      this.r.offer(var1);

      while (this.f.size() + this.p.size() > 8192) {
         Long var3 = this.r.poll();
         if (var3 == null) {
            break;
         }

         this.p.remove(var3);
         this.f.remove(var3);
         this.g.remove(var3);
         this.q.remove(var3);
      }
   }

   private static long a(int var0, int var1) {
      long var2 = (long)var0 << 32 ^ var1 & 4294967295L;
      var2 = (var2 ^ var2 >>> 33) * -49064778989728563L;
      var2 = (var2 ^ var2 >>> 33) * -4265267296055464877L;
      return var2 ^ var2 >>> 33;
   }
}
