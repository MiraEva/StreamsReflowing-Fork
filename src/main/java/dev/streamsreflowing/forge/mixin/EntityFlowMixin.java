package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.flow.DriftingEntities;
import dev.streamsreflowing.flow.MobFlowOverrides;
import dev.streamsreflowing.flow.ReelingCatch;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityFlowMixin {
   @Inject(method = "tick", at = @At("TAIL"), require = 0, expect = 0)
   private void streamsreflowing$driftInCurrent(CallbackInfo ci) {
      DriftingEntities.carry((Entity)(Object)this);
   }

   @ModifyReturnValue(method = "isPushedByFluid", at = @At("RETURN"), require = 0, expect = 0)
   private boolean streamsreflowing$waterMobsIgnoreFlow(boolean original) {
      return original
         && !streamsreflowing$isBobber((Entity)(Object)this)
         && !streamsreflowing$isReeledCatch((Entity)(Object)this)
         && !streamsreflowing$holdsInCurrent((Entity)(Object)this);
   }

   @ModifyExpressionValue(
      method = "updateFluidHeightAndDoFluidPushing",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z"),
      require = 0,
      expect = 0
   )
   private boolean streamsreflowing$waterMobsIgnoreFlowAtPushSite(boolean original) {
      return original
         && !streamsreflowing$isBobber((Entity)(Object)this)
         && !streamsreflowing$isReeledCatch((Entity)(Object)this)
         && !streamsreflowing$holdsInCurrent((Entity)(Object)this);
   }

   @Unique
   private static boolean streamsreflowing$isReeledCatch(Entity self) {
      return self instanceof ReelingCatch flying && flying.streamsreflowing$reeling();
   }

   @Unique
   private static boolean streamsreflowing$taggedIgnoresCurrent(Entity self) {
      return self.getType().is(StreamsWorldgen.IGNORES_CURRENT);
   }

   @Unique
   private static boolean streamsreflowing$holdsInCurrent(Entity self) {
      if (MobFlowOverrides.any()) {
         Boolean say = MobFlowOverrides.verdict(String.valueOf(EntityType.getKey(self.getType())));
         if (say != null) {
            return say;
         }
      }

      if (streamsreflowing$taggedIgnoresCurrent(self)) {
         return true;
      }

      if (!StreamsSettings.waterMobsIgnoreFlow()) {
         return false;
      }

      MobCategory cat = self.getType().getCategory();
      return cat == MobCategory.WATER_CREATURE
         || cat == MobCategory.WATER_AMBIENT
         || cat == MobCategory.UNDERGROUND_WATER_CREATURE
         || cat == MobCategory.AXOLOTLS
         || self instanceof Drowned
         || self instanceof Guardian
         || self instanceof LivingEntity living && living.canBreatheUnderwater() && living.getMobType() != MobType.UNDEAD;
   }

   @Unique
   private static boolean streamsreflowing$isBobber(Entity self) {
      return self instanceof FishingHook;
   }
}
