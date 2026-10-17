package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.flow.ReelingCatch;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
   @WrapOperation(
      method = "retrieve",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V"),
      require = 0,
      expect = 0
   )
   private void streamsreflowing$markReeledCatch(ItemEntity item, double x, double y, double z, Operation<Void> original) {
      original.call(new Object[]{item, x, y, z});
      if (item instanceof ReelingCatch catchInFlight) {
         catchInFlight.streamsreflowing$beginReel();
      }
   }
}
