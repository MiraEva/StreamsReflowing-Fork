package dev.streamsreflowing.flow;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientFlowGrids {
   private static final ConcurrentHashMap<Long, byte[]> GRIDS = new ConcurrentHashMap<>();
   private static final int MAX = 16384;
   private static volatile int version;
   private static final ThreadLocal<ClientFlowGrids.Memo> MEMO = ThreadLocal.withInitial(ClientFlowGrids.Memo::new);

   private ClientFlowGrids() {
   }

   private static long key(int cx, int cz) {
      return (long)cx << 32 ^ cz & 4294967295L;
   }

   public static boolean put(int cx, int cz, byte[] grid) {
      byte[] prev = GRIDS.put(key(cx, cz), grid);
      version++;
      boolean changed = prev == null || !Arrays.equals(prev, grid);
      if (GRIDS.size() > 16384) {
         Iterator<Long> it = GRIDS.keySet().iterator();

         while (GRIDS.size() > 12288 && it.hasNext()) {
            it.next();
            it.remove();
         }
      }

      return changed;
   }

   public static void remove(int cx, int cz) {
      GRIDS.remove(key(cx, cz));
      version++;
   }

   public static void clear() {
      RTFFlowUnderlay.clearClient();
      GRIDS.clear();
      FlowRemeshQueue.clear();
      version++;
   }

   public static byte[] gridAt(int cx, int cz) {
      return GRIDS.get(key(cx, cz));
   }

   public static double speedAt(int x, int z) {
      return speedAt(x, Integer.MIN_VALUE, z);
   }

   public static double speedAt(int x, int y, int z) {
      long k = key(x >> 4, z >> 4);
      ClientFlowGrids.Memo memo = MEMO.get();
      int v = version;
      if (memo.key != k || memo.version != v) {
         memo.grid = GRIDS.get(k);
         memo.key = k;
         memo.version = v;
      }

      return StreamFlowGrid.speedOf(memo.grid, x, z, y);
   }

   public static byte angleAt(int x, int z) {
      return angleAt(x, Integer.MIN_VALUE, z);
   }

   public static byte angleAt(int x, int y, int z) {
      long k = key(x >> 4, z >> 4);
      ClientFlowGrids.Memo memo = MEMO.get();
      int v = version;
      if (memo.key != k || memo.version != v) {
         memo.grid = GRIDS.get(k);
         memo.key = k;
         memo.version = v;
      }

      byte[] g = memo.grid;
      return g == null ? -1 : StreamFlowGrid.angleOf(g, x, z, y);
   }

   private static final class Memo {
      long key = Long.MIN_VALUE;
      int version = -1;
      byte[] grid;
   }
}
