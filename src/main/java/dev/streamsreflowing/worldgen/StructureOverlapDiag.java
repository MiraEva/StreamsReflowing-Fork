package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.plate.PlateKey;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class StructureOverlapDiag {
   private static final boolean a = Boolean.getBoolean("streamsreflowing.caldiag");
   private static final AtomicLong b = new AtomicLong();
   private static final AtomicLong c = new AtomicLong();
   private static final AtomicLong d = new AtomicLong();
   private static final AtomicLong e = new AtomicLong();
   private static final ConcurrentLinkedQueue<String> f = new ConcurrentLinkedQueue<>();

   private StructureOverlapDiag() {
   }

   public static void shieldedColumn(RiverEngine var0, int var1, int var2, int var3, int var4) {
      if (a && var0 != null) {
         long var5 = b.incrementAndGet();
         boolean var7 = var0.inCorridor(var1, var2);
         boolean var8 = var0.lakeAt(var1, var2) != null;
         if (var7) {
            c.incrementAndGet();
         }

         if (var8) {
            d.incrementAndGet();
         }

         if ((var7 || var8) && f.size() < 12) {
            PlateKey[] var9 = var0.platesOwningChunk(var1 >> 4, var2 >> 4);
            f.add(
               var1
                  + ","
                  + var2
                  + (var7 ? " stream" : "")
                  + (var8 ? " lake" : "")
                  + " ground="
                  + var3
                  + " pieceTop="
                  + var4
                  + " bed="
                  + var0.bedYAmong(var9, var1, var2)
                  + " inCorridorOwn="
                  + var0.inCorridorAmong(var9, var1, var2)
                  + " ownReady="
                  + var0.platesReady(var9)
            );
         }

         report((var5 & 255L) == 0L);
      }
   }

   public static void report(boolean var0) {
      if (a) {
         long var1 = System.currentTimeMillis();
         long var3 = e.get();
         if (var0 || var1 >= var3 && e.compareAndSet(var3, var1 + 10000L)) {
            Constants.LOG
               .info(
                  "[SR-STRUCT-OVERLAP] shielded columns {} | of those on a stream corridor {} | in a lake {} | samples {}",
                  new Object[]{b.get(), c.get(), d.get(), String.join(" ; ", f)}
               );
         }
      }
   }
}
