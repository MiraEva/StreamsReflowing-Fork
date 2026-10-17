package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.Constants;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class ChunkFillProbe {
   private static final int a = 12;

   private ChunkFillProbe() {
   }

   public static void run(ServerLevel var0, TerrainSampler var1) {
      if (System.getProperty("streamsreflowing.chunkfillprobe") != null) {
         Thread var2 = new Thread(() -> {
            try {
               a(var0, var1);
            } catch (Throwable var3) {
               Constants.LOG.info("[SR-FILLPROBE] could not run: {}", String.valueOf(var3));
            }
         }, "sr-fillprobe");
         var2.setDaemon(true);
         var2.start();
      }
   }

   private static void a(ServerLevel var0, TerrainSampler var1) throws Throwable {
      short var2 = 4096;
      short var3 = 4096;
      long var4 = 0L;
      long var6 = 0L;
      int var8 = 0;
      int var9 = 0;
      int var10 = 0;
      long var11 = 0L;

      for (int var13 = 0; var13 < 12; var13++) {
         ChunkPos var14 = new ChunkPos((var2 >> 4) + var13 * 7, (var3 >> 4) + var13 * 11);
         long var15 = System.nanoTime();
         int[] var17 = ChunkHeightFill.heights(var0, var14);
         var4 += System.nanoTime() - var15;
         if (var17 == null) {
            Constants.LOG.info("[SR-FILLPROBE] this world cannot be filled that way");
            return;
         }

         var11 += 256L;

         for (int var18 = 0; var18 < 4; var18++) {
            int var19 = var18 * 5 + 1 & 15;
            int var20 = var18 * 7 + 3 & 15;
            int var21 = var14.getMinBlockX() + var19;
            int var22 = var14.getMinBlockZ() + var20;
            int var23 = var17[var20 * 16 + var19];
            long var24 = System.nanoTime();
            double var26 = var0.getChunkSource().getGenerator().getBaseHeight(var21, var22, Types.OCEAN_FLOOR_WG, var0, var0.getChunkSource().randomState());
            var6 += System.nanoTime() - var24;
            var8++;
            int var28 = (int)Math.round(var23 - var26);
            if (var28 != 0) {
               var9++;
               if (Math.abs(var28) > Math.abs(var10)) {
                  var10 = var28;
               }
            }
         }
      }

      double var29 = var4 / 1000000.0 / 12.0;
      double var30 = var6 / 1000000.0 / Math.max(1, var8);
      Constants.LOG
         .info(
            "[SR-FILLPROBE] one chunk noise fill {} ms ({} columns) | one getBaseHeight {} ms -> a fill costs {} reads, and this world reads {} columns of a chunk",
            new Object[]{a(var29), var11 / 12L, a(var30), a(var30 <= 0.0 ? 0.0 : var29 / var30), "14.1 (measured)"}
         );
      Constants.LOG
         .info(
            "[SR-FILLPROBE] heightmap vs getBaseHeight: {} columns compared, {} disagree{}",
            new Object[]{var8, var9, var9 == 0 ? " -- exact" : ", worst " + var10 + " blocks"}
         );
   }

   private static String a(double var0) {
      return String.format(Locale.ROOT, "%.2f", var0);
   }
}
