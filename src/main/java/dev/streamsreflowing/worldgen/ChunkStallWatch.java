package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.platform.Services;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ChunkStallWatch {
   private static final boolean a = Boolean.getBoolean("streamsreflowing.nostalldump");
   private static final long b = 2000L;
   private static final long c = 20000L;
   private static final long d = 180000L;
   private static final long e = 300000L;
   private static final long f = 120000L;
   private static final int g = 6;
   private static final double h = 8.0;
   private static volatile Thread i;
   private static volatile WeakReference<MinecraftServer> j = new WeakReference<>(null);

   private ChunkStallWatch() {
   }

   public static synchronized void start(MinecraftServer var0) {
      if (!a && var0 != null) {
         Thread var1 = i;
         if (var1 == null || !var1.isAlive() || j.get() != var0) {
            j = new WeakReference<>(var0);
            var1 = new Thread(ChunkStallWatch::a, "streamsreflowing-stallwatch");
            var1.setDaemon(true);
            var1.setPriority(1);
            i = var1;
            var1.start();
         }
      }
   }

   public static synchronized void stop() {
      Thread var0 = i;
      i = null;
      if (var0 != null) {
         var0.interrupt();
      }
   }

   private static void a() {
      long var0 = -1L;
      long var2 = -1L;
      long var4 = System.currentTimeMillis();
      long var6 = 0L;
      int var8 = 0;
      HashMap var9 = new HashMap();
      long var10 = FlowTrace.deferred.get() - FlowTrace.resumed.get();

      while (i == Thread.currentThread()) {
         try {
            Thread.sleep(2000L);
         } catch (InterruptedException var31) {
            return;
         }

         MinecraftServer var12 = j.get();
         if (var12 == null || var12.isStopped()) {
            return;
         }

         try {
            ServerLevel var13 = var12.getLevel(Level.OVERWORLD);
            if (var13 != null) {
               long var14 = CarveCensus.chunks();
               long var16 = -1L;

               try {
                  var16 = var13.getChunkSource().getLoadedChunksCount();
               } catch (Throwable var30) {
               }

               long var18 = System.currentTimeMillis();
               boolean var20 = var14 != var0 || var16 != var2;
               var0 = var14;
               var2 = var16;
               if (var20) {
                  var4 = var18;
               } else {
                  long var21 = FlowTrace.deferred.get() - FlowTrace.resumed.get() - var10;
                  boolean var23 = false;

                  for (ServerPlayer var25 : var13.players()) {
                     double[] var26 = (double[])var9.get(var25.getUUID());
                     double[] var27 = new double[]{var25.getX(), var25.getZ()};
                     if (var26 != null && Math.hypot(var27[0] - var26[0], var27[1] - var26[1]) >= 8.0) {
                        var23 = true;
                     }

                     var9.put(var25.getUUID(), var27);
                  }

                  int var33 = PlateRiverCache.foreignHeld() + StreamsWorldgen.structureWaiters();
                  boolean var34 = !var12.isReady() || var21 > 0L || var23 || var33 > 0;
                  if (!var34) {
                     var4 = var18;
                  } else {
                     long var35 = var18 - var4;
                     long var28 = !var12.isReady() ? 180000L : (var21 <= 0L && !var23 ? 300000L : 20000L);
                     if (var35 >= var28 && var18 - var6 >= 120000L && var8 < 6) {
                        var8++;
                        var6 = var18;
                        a(var12, var13, var35, var21, var16, var14);
                     }
                  }
               }
            }
         } catch (Throwable var32) {
            Constants.LOG.warn("[SR-STALLWATCH] sample failed", var32);
         }
      }
   }

   private static void a(MinecraftServer var0, ServerLevel var1, long var2, long var4, long var6, long var8) {
      String var10 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
      Path var11 = Path.of("streamsreflowing-stall-" + var10 + ".txt");
      StringBuilder var12 = new StringBuilder(65536);
      var12.append("Streams Reflowing chunk-stall dump ").append(var10).append('\n');
      var12.append("no chunk progress for ")
         .append(var2 / 1000L)
         .append(" s | world ready=")
         .append(var0.isReady())
         .append(" | chunks carved=")
         .append(var8)
         .append(" | chunk holders=")
         .append(var6)
         .append(" | deferred steps pending=")
         .append(var4)
         .append(" | generation threads held on stream planning=")
         .append(PlateRiverCache.foreignHeld() + StreamsWorldgen.structureWaiters())
         .append('\n');
      var12.append("sampler pool ")
         .append(LowResGrid.samplerWidth())
         .append('/')
         .append(LowResGrid.samplerFullWidth())
         .append(" | ")
         .append(StreamPrefetcher.laneDiag())
         .append(" | terrain accuracy ")
         .append(Services.PLATFORM.terrainAccuracyPreset())
         .append(" | mod ")
         .append(Services.PLATFORM.modVersion("streamsreflowing"))
         .append('\n');
      int var13 = var0.getPlayerList().getViewDistance();
      RiverEngine var14 = StreamsWorldgen.engineIfPresent(var1);
       ArrayList<ServerPlayer> var15 = new ArrayList<>(var1.players());

      for (ServerPlayer var17 : var15) {
         int var18 = var17.getBlockX();
         int var19 = var17.getBlockZ();
         var12.append("player ")
            .append(var17.getName().getString())
            .append(" at (")
            .append(var18)
            .append(',')
            .append(var17.getBlockY())
            .append(',')
            .append(var19)
            .append(") view ")
            .append(var13)
            .append('\n');
         if (var14 != null) {
            for (PlateKey var23 : var14.platesNear(var18, var19, var13 * 16.0 + 48.0)) {
               var12.append("  plate ")
                  .append(var23)
                  .append(" ready=")
                  .append(var14.isPlateReady(var23))
                  .append(' ')
                  .append(StreamPrefetcher.plateDiag(var14, var23))
                  .append('\n');
            }
         }
      }

      if (var15.isEmpty()) {
         var12.append("no players online (world creation or a pregen): the plates being built are on the stacks below\n");
      }

      var12.append('\n');
       Map<Thread, StackTraceElement[]> var26 = Thread.getAllStackTraces();
       ArrayList<Entry<Thread, StackTraceElement[]>> var27 = new ArrayList<>(var26.entrySet());
      var27.sort((var0x, var1x) -> ((Thread)var0x.getKey()).getName().compareTo(((Thread)var1x.getKey()).getName()));

       for (Entry<Thread, StackTraceElement[]> var29 : var27) {
          Thread var30 = var29.getKey();
         var12.append('"')
            .append(var30.getName())
            .append("\" prio=")
            .append(var30.getPriority())
            .append(var30.isDaemon() ? " daemon" : "")
            .append(" state=")
            .append(var30.getState())
            .append('\n');

         for (StackTraceElement var24 : (StackTraceElement[])var29.getValue()) {
            var12.append("    at ").append(var24).append('\n');
         }

         var12.append('\n');
      }

      try {
         Files.writeString(var11, var12.toString(), StandardCharsets.UTF_8);
         Constants.LOG
            .warn(
               "Streams Reflowing: chunk generation has made no progress for {} s while land is waiting; a thread dump was written to {}",
               var2 / 1000L,
               var11.toAbsolutePath()
            );
      } catch (Throwable var25) {
         Constants.LOG
            .warn(
               "Streams Reflowing: chunk generation has made no progress for {} s while land is waiting; the thread dump could not be written",
               var2 / 1000L,
               var25
            );
      }
   }
}
