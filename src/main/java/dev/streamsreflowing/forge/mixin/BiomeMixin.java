package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.RainCount;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import dev.streamsreflowing.worldgen.StreamBiomeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {
   @WrapOperation(
      method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;")
   )
   private Fluid streamsreflowing$streamFreezesLikeWater(FluidState state, Operation<Fluid> original) {
      if (state == null) {
         return Fluids.EMPTY;
      }

      Fluid type = state.getType();
      return (Fluid)(type != StreamFluids.SOURCE.get() && type != StreamFluids.FLOWING.get() ? (Fluid)original.call(new Object[]{state}) : Fluids.WATER);
   }

   @Unique
   private static Biome streamsreflowing$localLand(Object self, BlockPos pos) {
      boolean stamp = StreamBiomeBridge.isStamp(self);
      RainCount.climate(stamp);
      if (!stamp) {
         return null;
      }

      StreamBiomeBridge.LandProbe probe = StreamBiomeBridge.landProbe;
      if (probe == null) {
         return null;
      }

      long t0 = RainCount.ON ? System.nanoTime() : 0L;
      Holder<Biome> land = probe.at(pos.getX(), pos.getY(), pos.getZ());
      RainCount.probe(System.nanoTime() - t0);
      return land != null && land.value() != self ? (Biome)land.value() : null;
   }

   @Inject(method = "warmEnoughToRain(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
   private void streamsreflowing$adaptiveWarmEnough(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      Biome land = streamsreflowing$localLand(this, pos);
      if (land != null) {
         cir.setReturnValue(land.warmEnoughToRain(pos));
      }
   }

   @Inject(method = "coldEnoughToSnow(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
   private void streamsreflowing$adaptiveColdEnough(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      Biome land = streamsreflowing$localLand(this, pos);
      if (land != null) {
         cir.setReturnValue(land.coldEnoughToSnow(pos));
      }
   }
}
