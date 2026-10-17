package dev.streamsreflowing.flow;

import java.util.Iterator;
import java.util.LinkedHashSet;

public final class FlowRemeshQueue {
   private static final int PER_TICK = 16;
   private static final LinkedHashSet<Long> PENDING = new LinkedHashSet<>();

   private FlowRemeshQueue() {
   }

   public static synchronized void enqueue(int cx, int cz) {
      PENDING.add((long)cx << 32 ^ cz & 4294967295L);
   }

   public static synchronized void clear() {
      PENDING.clear();
   }

   public static void drain(FlowRemeshQueue.ChunkDirty mark) {
      long[] batch;
      synchronized (FlowRemeshQueue.class) {
         if (PENDING.isEmpty()) {
            return;
         }

         int n = Math.min(16, PENDING.size());
         batch = new long[n];
         Iterator<Long> it = PENDING.iterator();

         for (int i = 0; i < n; i++) {
            batch[i] = it.next();
            it.remove();
         }
      }

      for (long k : batch) {
         mark.mark((int)(k >> 32), (int)k);
      }
   }

   public interface ChunkDirty {
      void mark(int var1, int var2);
   }
}
