package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.flow.StreamIceMelt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IceBlock.class)
public abstract class IceMeltMixin {
   @WrapOperation(
      method = "melt",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
      ),
      require = 0,
      expect = 0
   )
   private boolean streamsreflowing$thawIntoStream(Level level, BlockPos pos, BlockState meltsInto, Operation<Boolean> original) {
      BlockState stream = StreamIceMelt.meltsInto(level, pos);
      return (Boolean)original.call(new Object[]{level, pos, stream != null ? stream : meltsInto});
   }
}
