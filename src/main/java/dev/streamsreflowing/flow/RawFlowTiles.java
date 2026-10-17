package dev.streamsreflowing.flow;

import dev.streamsreflowing.core.diag.MemBytes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RawFlowTiles {
   public static final int COLUMNS = 256;
   public static final short NO_SURFACE = -32768;
   private static final int MAX_CACHED = 4096;
   private static final ConcurrentHashMap<Long, RawFlowTiles.Tile> CACHE = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<Long> ORDER = new ConcurrentLinkedQueue<>();

   private RawFlowTiles() {
   }

   public static int cacheEntries() {
      return CACHE.size();
   }

   public static long cacheBytes() {
      long b = MemBytes.hashMapNodes(CACHE.size()) + CACHE.size() * 40L;

      for (RawFlowTiles.Tile t : CACHE.values()) {
         b += tileBytes(t);
      }

      return b;
   }

   public static long tileBytes(RawFlowTiles.Tile t) {
      return MemBytes.object(16)
         + MemBytes.array(t.dirX().length, 1)
         + MemBytes.array(t.dirZ().length, 1)
         + MemBytes.array(t.surfaceY().length, 2)
         + (t.bedY() == null ? 0L : MemBytes.array(t.bedY().length, 2));
   }

   private static long key(int cx, int cz) {
      return (long)cx << 32 ^ cz & 4294967295L;
   }

   public static void put(int cx, int cz, RawFlowTiles.Tile tile) {
      RawFlowTileStore.store(cx, cz, tile);
      if (CACHE.put(key(cx, cz), tile) == null) {
         ORDER.offer(key(cx, cz));
      }

      while (CACHE.size() > 4096) {
         Long old = ORDER.poll();
         if (old == null) {
            break;
         }

         CACHE.remove(old);
      }
   }

   public static RawFlowTiles.Tile get(int cx, int cz) {
      RawFlowTiles.Tile t = CACHE.get(key(cx, cz));
      if (t != null) {
         return t;
      }

      t = RawFlowTileStore.load(cx, cz);
      if (t != null && CACHE.put(key(cx, cz), t) == null) {
         ORDER.offer(key(cx, cz));
      }

      return t;
   }

   public static void clear() {
      CACHE.clear();
      ORDER.clear();
   }

   public record Tile(byte[] dirX, byte[] dirZ, short[] surfaceY, short[] bedY) {
   }
}
