package dev.streamsreflowing.worldgen;

import com.mojang.datafixers.util.Either;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.platform.Services;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.ChunkHolder.ChunkLoadingFailure;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class ChunkStatusDefer {
   private static final boolean a = Boolean.getBoolean("streamsreflowing.nochunkdefer");
   private static volatile Boolean b;
   private static final double c = 48.0;

   private ChunkStatusDefer() {
   }

   private static boolean a() {
      Boolean var0 = b;
      if (var0 == null) {
         var0 = Services.PLATFORM.isModLoaded("c2me");
         b = var0;
      }

      return var0;
   }

   public static CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>> defer(
      ChunkStatus var0,
      Executor var1,
      ServerLevel var2,
      ChunkGenerator var3,
      StructureTemplateManager var4,
      ThreadedLevelLightEngine var5,
      Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>>> var6,
      List<ChunkAccess> var7
   ) {
      if (a || !Services.PLATFORM.fastChunkLoading() || !a()) {
         return null;
      }

      if (StreamPrefetcher.isServerStopping()) {
         return null;
      }

      if (var0 != ChunkStatus.CARVERS && var0 != ChunkStatus.FEATURES) {
         return null;
      }

      if (!Services.PLATFORM.generateRivers()) {
         return null;
      }

      if (!var2.dimension().equals(Level.OVERWORLD)) {
         return null;
      }

      ChunkAccess var8 = (ChunkAccess)var7.get(var7.size() / 2);
      if (var8.getStatus().isOrAfter(var0)) {
         return null;
      }

      RiverEngine var9 = StreamsWorldgen.engine(var2);
      if (var9 == null) {
         return null;
      }

      ChunkPos var10 = var8.getPos();
      double var11 = var10.getMinBlockX() + 8.0;
      double var13 = var10.getMinBlockZ() + 8.0;
      return var9.platesReadyNear(var11, var13, 48.0)
         ? null
         : var9.whenPlatesReadyNear(var11, var13, 48.0).thenComposeAsync(var8x -> var0.generate(var1, var2, var3, var4, var5, var6, var7), var1);
   }
}
