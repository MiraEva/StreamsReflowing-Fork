package dev.streamsreflowing.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FrostWalkerEnchantment.class)
public abstract class FrostWalkerMixin {
   @WrapOperation(
      method = "onEntityMoved",
      at = @At(
         value = "INVOKE",
         ordinal = 1,
         target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
      ),
      require = 0,
      expect = 0
   )
   private static BlockState streamsreflowing$streamTakesIce(Level level, BlockPos pos, Operation<BlockState> original) {
      BlockState state = (BlockState)original.call(new Object[]{level, pos});
      return state.getFluidState().getType() != StreamFluids.SOURCE.get() ? state : Blocks.WATER.defaultBlockState();
   }
}
