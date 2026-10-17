package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.Constants;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LinearPalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin({LinearPalette.class, HashMapPalette.class})
public abstract class PaletteResizeLoopMixin {
   @Unique
   private static final ThreadLocal<Integer> STREAMSREFLOWING$RESIZE_DEPTH = ThreadLocal.withInitial(() -> 0);
   @Unique
   private static final int STREAMSREFLOWING$MAX_DEPTH = 8;
   @Unique
   private static final AtomicBoolean STREAMSREFLOWING$WARNED = new AtomicBoolean();

   @WrapOperation(
      method = "idFor",
      require = 0,
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/PaletteResize;onResize(ILjava/lang/Object;)I")
   )
   private int streamsreflowing$breakResizeLoop(@Coerce Object resizeHandler, int bits, Object value, Operation<Integer> original) {
      int depth = STREAMSREFLOWING$RESIZE_DEPTH.get();
      if (depth >= 8) {
         if (STREAMSREFLOWING$WARNED.compareAndSet(false, true)) {
            Constants.LOG
               .warn(
                  "Recovered a chunk palette from a resize loop instead of crashing. This happens when another mod lets two threads write one chunk section at once (typically parallel-worldgen mods under heavy pregeneration); the section was already corrupted before the recovery. Reported once per session."
               );
         }

         return 0;
      } else {
         STREAMSREFLOWING$RESIZE_DEPTH.set(depth + 1);

         try {
            return (Integer)original.call(new Object[]{resizeHandler, bits, value});
         } finally {
            STREAMSREFLOWING$RESIZE_DEPTH.set(depth);
         }
      }
   }
}
