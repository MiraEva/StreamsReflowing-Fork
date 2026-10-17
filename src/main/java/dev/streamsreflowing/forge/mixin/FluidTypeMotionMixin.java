package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.flow.CurrentPush;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FluidType.class, remap = false)
public abstract class FluidTypeMotionMixin {
   @ModifyReturnValue(method = "motionScale", at = @At("RETURN"), require = 0, expect = 0, remap = false)
   private double streamsreflowing$flowSpeedForNonPlayers(double original, Entity entity) {
      if ((Object)this != ForgeMod.WATER_TYPE.get()) {
         return original;
      }

      double push = CurrentPush.factor(entity);
      if (entity instanceof Player) {
         return push == 1.0 ? original : original * push;
      }

      double sp = StreamFlowGrid.speedForPhysics(entity.level(), entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
      if (entity instanceof LivingEntity) {
         sp = Math.max(1.0, sp);
      }

      return original * sp * push;
   }
}
