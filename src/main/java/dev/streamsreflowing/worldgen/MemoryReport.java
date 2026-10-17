package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class MemoryReport {
   private static final long a = a(System.getProperty("streamsreflowing.memreport"));
   private static final long b = MemBytes.object(140);
   private static final long c = 1048576L;
   private static volatile long d;
   private static volatile boolean e;

   private MemoryReport() {
   }

   private static long a(String var0) {
      if (var0 == null) {
         return 0L;
      }

      try {
         return Math.max(10L, Long.parseLong(var0.trim())) * 1000L;
      } catch (NumberFormatException var2) {
         return 60000L;
      }
   }

   public static void maybeReport(MinecraftServer var0) {
      if (a > 0L && !e) {
         long var1 = System.currentTimeMillis();
         if (var1 - d >= a) {
            d = var1;
            e = true;
            Thread var3 = new Thread(() -> {
               try {
                  a(var0);
               } catch (Throwable var5) {
                  Constants.LOG.warn("[SR-MEM] report failed", var5);
               } finally {
                  e = false;
               }
            }, "streamsreflowing-memreport");
            var3.setDaemon(true);
            var3.start();
         }
      }
   }

   private static String a(long var0) {
      return String.format(Locale.ROOT, "%.1f MB", var0 / 1048576.0);
   }

   private static void a(MinecraftServer var0) throws InterruptedException {
      System.gc();
      Thread.sleep(300L);
      System.gc();
      Runtime var1 = Runtime.getRuntime();
      long var2 = var1.totalMemory() - var1.freeMemory();
      long var4 = var1.maxMemory();
      ServerLevel var6 = var0.getLevel(Level.OVERWORLD);
      RiverEngine var7 = var6 == null ? null : StreamsWorldgen.engineIfPresent(var6);
      int var8 = 0;
      long var9 = 0L;
      long var11 = 0L;
      IdentityHashMap var13 = new IdentityHashMap();
      IdentityHashMap var14 = new IdentityHashMap();
       IdentityHashMap<RiverNetwork, Boolean> var15 = new IdentityHashMap<>();
      long var16 = 0L;
      int var18 = 0;
      int var19 = 0;
      if (var7 != null) {
          ArrayList<RiverNetwork> var20 = new ArrayList<>();
         var7.forEachCachedNetwork(var20::add);

         for (RiverNetwork var22 : var20) {
            if (var13.put(var22, Boolean.TRUE) == null) {
               var8++;
               var9 += var22.segmentCount();
               var11 += var22.approxBytes();
            }
         }

         for (Object var39 : var7.threadCacheArrays()) {
            var18++;
            if (var39 instanceof long[] var23) {
               var16 += MemBytes.array(var23.length, 8);
               if (var23.length == RiverEngine.planCacheSlots()) {
                  var19++;
               }
            } else if (var39 instanceof int[] var24) {
               var16 += MemBytes.array(var24.length, 4);
            } else if (var39 instanceof ColumnPlan[] var25) {
               var16 += MemBytes.array(var25.length, 4);

               for (ColumnPlan var55 : var25) {
                  if (var55 != null) {
                     var14.put(var55, Boolean.TRUE);
                  }
               }
            } else if (var39 instanceof Object[] var26) {
               var16 += MemBytes.array(var26.length, 4);

               for (Object var30 : var26) {
                  if (var30 instanceof RiverNetwork var31 && !var13.containsKey(var31)) {
                     var15.put(var31, Boolean.TRUE);
                  }
               }
            }
         }
      }

      int var36 = 0;
      long var38 = 0L;

      for (ColumnPlan[] var42 : ChunkPlanCache.snapshot()) {
         var36++;
         var38 += MemBytes.array(var42.length, 4) + MemBytes.object(12) + 48L;

         for (ColumnPlan var51 : var42) {
            if (var51 != null) {
               var14.put(var51, Boolean.TRUE);
            }
         }
      }

      long var41 = var14.size() * b;
      long var44 = 0L;

      for (RiverNetwork var52 : var15.keySet()) {
         var44 += var52.approxBytes();
      }

      long var49 = StreamFlowGrid.cacheBytes();
      long var54 = RawFlowTiles.cacheBytes();
      long[] var56 = RawFlowTileStore.memoryStats();
      long[] var32 = RiverFlowFields.memoryStats();
      long[] var33 = CachingTerrainSampler.memoryStats();
      long var34 = var11 + var44 + var41 + var16 + var38 + var49 + var54 + var56[2] + var32[1] + var32[3] + var33[2];
      Constants.LOG
         .info(
            "[SR-MEM] heap used after a full GC {} of {} max | this mod's caches {} ({}% of used)",
            new Object[]{a(var2), a(var4), a(var34), var2 == 0L ? 0L : 100L * var34 / var2}
         );
      Constants.LOG
         .info(
            "[SR-MEM]   stream plates: {} cached, {} segments, {} (budget {}, cold plates reload from disk) | networks kept alive only by thread memos: {} ({})",
            new Object[]{var8, var9, a(var11), a(PlateRiverCache.diskBackedBudgetBytes()), var15.size(), a(var44)}
         );
      Constants.LOG
         .info(
            "[SR-MEM]   column plans retained: {} distinct ({}) | per-thread cache arrays: {} (plan-key arrays {}, ~{} threads) {} | chunk plan cache: {} chunks {}",
            new Object[]{var14.size(), a(var41), var18, var19, var19 / 3, a(var16), var36, a(var38)}
         );
      Constants.LOG
         .info(
            "[SR-MEM]   terrain sample caches: {} sampler(s), {} entries {} | river-flow regions: {} {} | biome classifier: {} cells {}",
            new Object[]{var33[0], var33[1], a(var33[2]), var32[0], a(var32[1]), var32[2], a(var32[3])}
         );
      Constants.LOG
         .info(
            "[SR-MEM]   flow grids: {} chunks {} | raw flow tiles: {} {} | saved-tile store: {} regions, {} tiles {}",
            new Object[]{StreamFlowGrid.cacheEntries(), a(var49), RawFlowTiles.cacheEntries(), a(var54), var56[0], var56[1], a(var56[2])}
         );
   }
}
