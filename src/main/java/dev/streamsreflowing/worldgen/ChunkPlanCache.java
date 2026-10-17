package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkPlanCache {
   private static final int a = 1024;
   private static final ConcurrentHashMap<ChunkPlanCache.Key, ColumnPlan[]> b = new ConcurrentHashMap<>();

   private ChunkPlanCache() {
   }

   public static List<ColumnPlan[]> snapshot() {
      return new ArrayList<>(b.values());
   }

   private static long a(int var0, int var1) {
      return (long)(var0 >> 4) << 32 ^ var1 >> 4 & 4294967295L;
   }

   public static void put(RiverEngine var0, int var1, int var2, ColumnPlan[] var3) {
      b.put(new ChunkPlanCache.Key(var0, a(var1, var2)), var3);
      if (b.size() > 1024) {
         Iterator var4 = b.keySet().iterator();

         while (b.size() > 768 && var4.hasNext()) {
            var4.next();
            var4.remove();
         }
      }
   }

   public static ColumnPlan[] get(RiverEngine var0, int var1, int var2) {
      return b.get(new ChunkPlanCache.Key(var0, a(var1, var2)));
   }

   public static ColumnPlan[] take(RiverEngine var0, int var1, int var2) {
      return b.remove(new ChunkPlanCache.Key(var0, a(var1, var2)));
   }

   public static void clear() {
      b.clear();
   }

   private record Key(RiverEngine engine, long chunk) {

      private Key {
chunk = chunk;
}
   }
}
