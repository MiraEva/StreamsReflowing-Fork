package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.compat.PhysicsModCompat;
import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.forge.fluid.StreamFlowingFluid;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
   private static boolean streamsreflowing$announced = false;
   private static final double STREAMS_INFLOW_COS = -0.5;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void streamsreflowing$confirmApplied(CallbackInfo ci) {
      if (!streamsreflowing$announced) {
         streamsreflowing$announced = true;
         LoggerFactory.getLogger("StreamsReflowing").debug("FlowingFluidMixin loaded — spill-over-water gated by config 'spillOverWater'");
      }
   }

   @Inject(method = "isWaterHole", at = @At("HEAD"), cancellable = true)
   private void streamsreflowing$spillOverWater(
      BlockGetter level, Fluid fluid, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState, CallbackInfoReturnable<Boolean> cir
   ) {
      if ((Object)this instanceof WaterFluid && StreamsSettings.spillOverWater()) {
      if (!StreamsSettings.forceBakedFluid() || (Object)this == StreamFluids.SOURCE.get() || (Object)this == StreamFluids.FLOWING.get()) {
            if (!StreamsSettings.spillOnStreamsOnly() || StreamFlowGrid.angleForPhysics(level, pos.getX(), pos.getY(), pos.getZ()) != -1) {
               FluidState below = spreadState.getFluidState();
      if (below.isSource() && below.getType().isSame((FlowingFluid)(Object)this)) {
                  cir.setReturnValue(false);
               }
            }
         }
      }
   }

   @Inject(method = "getFlow", at = @At("RETURN"), cancellable = true)
   private void streamsreflowing$imposeVanillaWaterFlow(BlockGetter level, BlockPos pos, FluidState state, CallbackInfoReturnable<Vec3> cir) {
      if ((Object)this instanceof WaterFluid && StreamsSettings.vanillaWaterFlow()) {
         if (level instanceof Level || !PhysicsModCompat.noImposedRenderFlow()) {
            byte angle = level instanceof Level
               ? StreamFlowGrid.angleForEntityPush(level, pos.getX(), pos.getY(), pos.getZ())
               : StreamFlowGrid.angleForPhysics(level, pos.getX(), pos.getY(), pos.getZ());
            if (angle != -1) {
               double sp = StreamFlowGrid.speedForPhysics(level, pos.getX(), pos.getY(), pos.getZ());
               cir.setReturnValue(StreamFlowGrid.imposedFlow(level, pos, state, angle, sp, (Vec3)cir.getReturnValue()));
            }
         }
      }
   }

   @WrapOperation(
      method = "getNewLiquid",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z"),
      require = 0,
      expect = 0
   )
   private boolean streamsreflowing$notTheSameWhenDecidingWhatFillsACell(Fluid neighbour, Fluid self, Operation<Boolean> original) {
      return streamsreflowing$isStreamFluid(neighbour) != streamsreflowing$isStreamFluid(self) ? false : (Boolean)original.call(new Object[]{neighbour, self});
   }

   @Inject(method = "getNewLiquid", at = @At("RETURN"), cancellable = true, require = 0)
   private void streamsreflowing$sourceOnlyWhenEnclosed(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FluidState> cir) {
      FluidState made = (FluidState)cir.getReturnValue();
      if (made != null && made.isSource()) {
         Object self = this;
         if (self instanceof WaterFluid || streamsreflowing$isStreamFluid((Fluid)self)) {
            for (Direction d : Plane.HORIZONTAL) {
               if (level.getBlockState(pos.relative(d)).isAir()) {
                  FlowingFluid fluid = (FlowingFluid)self;
                  FluidState above = level.getFluidState(pos.above());
                  cir.setReturnValue(!above.isEmpty() && above.getType().isSame(fluid) ? fluid.getFlowing(8, true) : fluid.getFlowing(7, false));
                  return;
               }
            }
         }
      }
   }

   private static boolean streamsreflowing$isStreamFluid(Fluid fluid) {
      return fluid instanceof StreamFlowingFluid.Source || fluid instanceof StreamFlowingFluid.Flowing;
   }
}
