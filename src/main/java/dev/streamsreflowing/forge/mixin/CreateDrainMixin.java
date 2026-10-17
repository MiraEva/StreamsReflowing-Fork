package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour", remap = false)
public abstract class CreateDrainMixin {
   @ModifyReturnValue(method = "getDrainableFluid", at = @At("RETURN"), require = 0, expect = 0, remap = false)
   private FluidStack streamsreflowing$drainAsWater(FluidStack original) {
      if (original != null && !original.isEmpty()) {
         Fluid fluid = original.getFluid();
         return fluid != StreamFluids.SOURCE.get() && fluid != StreamFluids.FLOWING.get() ? original : new FluidStack(Fluids.WATER, original.getAmount());
      } else {
         return original;
      }
   }
}
