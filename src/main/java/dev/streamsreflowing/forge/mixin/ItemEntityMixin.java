package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.flow.ReelingCatch;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.forge.fluid.StreamLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements ReelingCatch {
   @Unique
   private int streamsreflowing$reelTicks;

   @Override
   public void streamsreflowing$beginReel() {
      this.streamsreflowing$reelTicks = 40;
   }

   @Override
   public boolean streamsreflowing$reeling() {
      return this.streamsreflowing$reelTicks > 0;
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void streamsreflowing$streamItemDrag(CallbackInfo ci) {
      ItemEntity self = (ItemEntity)(Object)this;
      if (this.streamsreflowing$reelTicks > 0) {
         this.streamsreflowing$reelTicks--;
      } else if (self.isInWater()) {
         BlockPos bp = self.blockPosition();
         BlockState here = self.level().getBlockState(bp);
         int speedY = bp.getY();
         boolean carried = here.getBlock() instanceof StreamLiquidBlock
            || StreamsSettings.vanillaWaterFlow() && StreamFlowGrid.angleForPhysics(self.level(), bp.getX(), bp.getY(), bp.getZ()) != -1;
         if (!carried) {
            FluidState film = here.getFluidState();
            BlockPos under = bp.below();
            FluidState pool = self.level().getFluidState(under);
            if (!film.is(FluidTags.WATER) || film.isSource() || !pool.is(FluidTags.WATER) || !pool.isSource()) {
               return;
            }

            speedY = under.getY();
         }

         double sp = StreamFlowGrid.speedForPhysics(self.level(), bp.getX(), speedY, bp.getZ());
         double cap = StreamsSettings.itemDriftSpeed() * sp / 20.0;
         Vec3 v = self.getDeltaMovement();
         double h = Math.hypot(v.x, v.z);
         if (h > cap) {
            double s = cap / h;
            self.setDeltaMovement(v.x * s, v.y, v.z * s);
         }
      }
   }
}
