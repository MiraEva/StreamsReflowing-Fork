package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.config.StreamsSettings;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Boat.Status;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Boat.class)
public abstract class BoatMixin {
   private static final double STREAMSREFLOWING$MAX_FLOAT_STEP = 1.25;

   @Inject(method = "isUnderwater", at = @At("RETURN"), cancellable = true)
   private void streamsreflowing$floatOnFlowingWater(CallbackInfoReturnable<Status> cir) {
      if (StreamsSettings.boatsFloatOnFlowingWater() && cir.getReturnValue() == Status.UNDER_FLOWING_WATER) {
         cir.setReturnValue(null);
      }
   }

   @Inject(method = "floatBoat", at = @At("TAIL"))
   private void streamsreflowing$rideTheSurface(CallbackInfo ci) {
      if (StreamsSettings.boatsFloatOnFlowingWater()) {
      Boat self = (Boat)(Object)this;
         double surface = streamsreflowing$streamSurface(self);
         if (!Double.isInfinite(surface)) {
            double target = surface - self.getBbHeight() + 0.1;
            double y = self.getY();
            Vec3 v = self.getDeltaMovement();
            if (y < target - 0.05) {
               self.setDeltaMovement(v.x, Math.min(0.15, (target - y) * 0.4), v.z);
            } else if (y <= target + 0.05) {
               self.setDeltaMovement(v.x, 0.0, v.z);
            }
         }
      }
   }

   private static double streamsreflowing$streamSurface(Boat self) {
      AABB bb = self.getBoundingBox();
      int x0 = Mth.floor(bb.minX);
      int x1 = Mth.ceil(bb.maxX);
      int z0 = Mth.floor(bb.minZ);
      int z1 = Mth.ceil(bb.maxZ);
      int yTop = Mth.ceil(bb.maxY) + 1;
      int yBot = Mth.floor(bb.minY) - 1;
      boolean flowingPresent = false;
      MutableBlockPos p = new MutableBlockPos();
      double[] columns = new double[(x1 - x0 + 1) * (z1 - z0 + 1)];
      int nCols = 0;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            for (int y = yTop; y >= yBot; y--) {
               FluidState fs = self.level().getFluidState(p.set(x, y, z));
               if (!fs.isEmpty() && fs.is(FluidTags.WATER)) {
                  if (!fs.isSource()) {
                     flowingPresent = true;
                  }

                  columns[nCols++] = y + fs.getHeight(self.level(), p);
                  break;
               }
            }
         }
      }

      if (flowingPresent && nCols != 0) {
         double lowest = Double.POSITIVE_INFINITY;

         for (int i = 0; i < nCols; i++) {
            lowest = Math.min(lowest, columns[i]);
         }

         double surface = Double.NEGATIVE_INFINITY;

         for (int i = 0; i < nCols; i++) {
            if (columns[i] <= lowest + 1.25) {
               surface = Math.max(surface, columns[i]);
            }
         }

         return surface;
      } else {
         return Double.NEGATIVE_INFINITY;
      }
   }
}
