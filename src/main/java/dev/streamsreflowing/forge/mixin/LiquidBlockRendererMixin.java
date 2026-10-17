package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.forge.fluid.StreamFluids;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
   private static boolean streamsreflowing$isWaterFamily(FluidState state) {
      if (state.isEmpty()) {
         return false;
      }

      Fluid type = state.getType();
      return state.is(FluidTags.WATER) || type == StreamFluids.SOURCE.get() || type == StreamFluids.FLOWING.get();
   }

   @Inject(
      method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void streamsreflowing$cullWaterFamilySeam(
      BlockAndTintGetter level,
      BlockPos pos,
      FluidState fluidState,
      BlockState selfState,
      Direction direction,
      FluidState neighborFluid,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (streamsreflowing$isWaterFamily(fluidState) && streamsreflowing$isWaterFamily(neighborFluid)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = "getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F",
      at = @At("HEAD"),
      cancellable = true
   )
   private void streamsreflowing$blendWaterFamilyHeight(
      BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState, CallbackInfoReturnable<Float> cir
   ) {
      if (!fluidState.isEmpty()
         && !fluid.isSame(fluidState.getType())
         && streamsreflowing$isWaterFamily(fluid.defaultFluidState())
         && streamsreflowing$isWaterFamily(fluidState)) {
         BlockState above = level.getBlockState(pos.above());
         cir.setReturnValue(streamsreflowing$isWaterFamily(above.getFluidState()) ? 1.0F : fluidState.getOwnHeight());
      }
   }

   @Inject(
      method = "isNeighborSameFluid(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/material/FluidState;)Z",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void streamsreflowing$mergeWaterFamily(FluidState first, FluidState second, CallbackInfoReturnable<Boolean> cir) {
      if (streamsreflowing$isWaterFamily(first) && streamsreflowing$isWaterFamily(second)) {
         cir.setReturnValue(true);
      }
   }
}
