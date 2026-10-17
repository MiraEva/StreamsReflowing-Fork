package dev.streamsreflowing.forge.fluid;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class StreamFluids {
   public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, "streamsreflowing");
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "streamsreflowing");
   public static final RegistryObject<FlowingFluid> SOURCE = FLUIDS.register("stream", StreamFlowingFluid.Source::new);
   public static final RegistryObject<FlowingFluid> FLOWING = FLUIDS.register("flowing_stream", StreamFlowingFluid.Flowing::new);
   public static final RegistryObject<StreamLiquidBlock> STREAM_BLOCK = BLOCKS.register(
      "stream", () -> new StreamLiquidBlock((FlowingFluid)SOURCE.get(), Properties.copy(Blocks.WATER).mapColor(MapColor.WATER))
   );

   public static void register(IEventBus modEventBus) {
      FLUIDS.register(modEventBus);
      BLOCKS.register(modEventBus);
   }

   private StreamFluids() {
   }
}
