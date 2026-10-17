package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.client.StreamTint;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeColors.class)
public abstract class WaterTintMixin {
   @Shadow
   @Final
   @Mutable
   public static ColorResolver WATER_COLOR_RESOLVER;
   @Shadow
   @Final
   @Mutable
   public static ColorResolver GRASS_COLOR_RESOLVER;
   @Shadow
   @Final
   @Mutable
   public static ColorResolver FOLIAGE_COLOR_RESOLVER;

   @Inject(method = "<clinit>", at = @At("TAIL"), require = 0)
   private static void streamsreflowing$wrapResolvers(CallbackInfo ci) {
      WATER_COLOR_RESOLVER = StreamTint.wrap(WATER_COLOR_RESOLVER, StreamTint.Kind.WATER);
      GRASS_COLOR_RESOLVER = StreamTint.wrap(GRASS_COLOR_RESOLVER, StreamTint.Kind.GRASS);
      FOLIAGE_COLOR_RESOLVER = StreamTint.wrap(FOLIAGE_COLOR_RESOLVER, StreamTint.Kind.FOLIAGE);
   }
}
