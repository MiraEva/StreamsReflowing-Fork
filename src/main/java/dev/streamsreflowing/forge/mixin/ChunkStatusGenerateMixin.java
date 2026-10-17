package dev.streamsreflowing.forge.mixin;

import com.mojang.datafixers.util.Either;
import dev.streamsreflowing.worldgen.ChunkStatusDefer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.ChunkHolder.ChunkLoadingFailure;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusGenerateMixin {
   @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
   private void streamsreflowing$deferUntilStreamRegionsReady(
      Executor executor,
      ServerLevel level,
      ChunkGenerator generator,
      StructureTemplateManager structureManager,
      ThreadedLevelLightEngine lightEngine,
      Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>>> loadTask,
      List<ChunkAccess> neighbors,
      CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>>> cir
   ) {
      CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>> deferred = ChunkStatusDefer.defer(
         (ChunkStatus)(Object)this, executor, level, generator, structureManager, lightEngine, loadTask, neighbors
      );
      if (deferred != null) {
         cir.setReturnValue(deferred);
      }
   }
}
