package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.compat.PhysicsModCompat;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.diebuddies.physics.ocean.BlockToByte", remap = false)
public abstract class PhysicsOceanIngestMixin {
   @Inject(method = "convert", at = @At("HEAD"), cancellable = true, require = 0, expect = 0, remap = false)
   private static void streamsreflowing$streamsAreNotOcean(BlockState state, CallbackInfoReturnable<Byte> cir) {
      try {
         if (!PhysicsModCompat.keepOceanOffStreams()) {
            return;
         }

         Fluid type = state.getFluidState().getType();
         if (type == StreamFluids.SOURCE.get() || type == StreamFluids.FLOWING.get()) {
            cir.setReturnValue((byte)0);
         }
      } catch (RuntimeException | LinkageError var3) {
      }
   }
}
