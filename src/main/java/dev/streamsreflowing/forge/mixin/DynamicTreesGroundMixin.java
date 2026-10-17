package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.compat.DynamicTreesCompat;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {"com.dtteam.dynamictrees.worldgen.OverworldGroundFinder", "com.ferreusveritas.dynamictrees.worldgen.OverworldGroundFinder"}, remap = false)
public abstract class DynamicTreesGroundMixin {
   @Inject(method = "findGround", at = @At("HEAD"), cancellable = true, require = 0, expect = 0, remap = false)
   private void streamsreflowing$noGroundInsideOurLakes(LevelAccessor accessor, BlockPos pos, Types type, CallbackInfoReturnable<List<BlockPos>> cir) {
      try {
         if (DynamicTreesCompat.refuse(accessor, pos)) {
            cir.setReturnValue(Collections.emptyList());
         }
      } catch (RuntimeException var6) {
      }
   }
}
