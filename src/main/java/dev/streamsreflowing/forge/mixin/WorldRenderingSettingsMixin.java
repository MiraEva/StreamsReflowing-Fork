package dev.streamsreflowing.forge.mixin;

import com.google.common.collect.UnmodifiableIterator;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import dev.streamsreflowing.forge.fluid.StreamLiquidBlock;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", remap = false)
public class WorldRenderingSettingsMixin {
   @Inject(method = "setBlockStateIds", at = @At("HEAD"), remap = false, require = 0)
   private void streamsreflowing$mapStreamToWater(Object2IntMap<BlockState> ids, CallbackInfo ci) {
      if (ids != null) {
         int waterId = ids.getInt(Blocks.WATER.defaultBlockState());
         if (waterId != ids.defaultReturnValue()) {
            UnmodifiableIterator var4 = ((StreamLiquidBlock)StreamFluids.STREAM_BLOCK.get()).getStateDefinition().getPossibleStates().iterator();

            while (var4.hasNext()) {
               BlockState state = (BlockState)var4.next();
               ids.put(state, waterId);
            }
         }
      }
   }
}
