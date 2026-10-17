package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.forge.fluid.StreamFluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterFluid.class)
public class WaterFluidMixin {
   @Inject(method = "isSame", at = @At("HEAD"), cancellable = true)
   private void streamsreflowing$sameAsStream(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
      if (fluid == StreamFluids.SOURCE.get() || fluid == StreamFluids.FLOWING.get()) {
         cir.setReturnValue(true);
      }
   }
}
