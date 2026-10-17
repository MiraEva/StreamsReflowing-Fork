package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.worldgen.StreamBiomeBridge;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class StreamBiomeSpawnsMixin {
   @Inject(method = "getMobSettings", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
   private void streamsreflowing$riverSpawns(CallbackInfoReturnable<MobSpawnSettings> cir) {
      if ((Object)this == StreamBiomeBridge.ours && StreamBiomeBridge.riverSpawns != null) {
         cir.setReturnValue(StreamBiomeBridge.riverSpawns);
      }
   }
}
