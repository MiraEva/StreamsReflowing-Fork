package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin {
   @Inject(method = "carveBlock", at = @At("HEAD"), cancellable = true)
   private void streamsreflowing$skipRiverCorridor(
      CarvingContext context,
      CarverConfiguration config,
      ChunkAccess chunk,
      Function<BlockPos, Holder<Biome>> biomeAccessor,
      CarvingMask mask,
      MutableBlockPos pos,
      MutableBlockPos checkPos,
      Aquifer aquifer,
      MutableBoolean reachedSurface,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (StreamsSettings.generateRivers()) {
         RiverEngine engine = StreamsWorldgen.active();
         if (engine != null && engine.inCorridor(pos.getX(), pos.getZ())) {
            cir.setReturnValue(false);
         }
      }
   }
}
