package dev.streamsreflowing.forge.client;

import dev.streamsreflowing.StallWatch;
import dev.streamsreflowing.client.StreamTint;
import dev.streamsreflowing.client.StreamsConfigScreen;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import dev.streamsreflowing.forge.particle.StreamBubbleParticle;
import dev.streamsreflowing.forge.particle.StreamFoamParticle;
import dev.streamsreflowing.forge.particle.StreamLeafParticle;
import dev.streamsreflowing.forge.particle.StreamParticles;
import dev.streamsreflowing.forge.particle.StreamSplashParticle;
import dev.streamsreflowing.forge.particle.StreamStickParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "streamsreflowing", bus = Bus.MOD, value = Dist.CLIENT)
public final class StreamClientSetup {
   private StreamClientSetup() {
   }

   @SubscribeEvent
   static void onClientSetup(FMLClientSetupEvent event) {
      try {
         ModLoadingContext.get()
            .registerExtensionPoint(ConfigScreenFactory.class, () -> new ConfigScreenFactory((minecraft, parent) -> new StreamsConfigScreen(parent)));
      } catch (Throwable var2) {
      }

      MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
         if (e.phase == Phase.END) {
            StreamParticleScanner.onClientTick();
            StreamTint.clientTick();
            ClientFlowHook.drainTick();
            if (StallWatch.ENABLED) {
               StallWatch.beatClient();
            }
         }
      });
      MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
         if (e.getLevel() != null && e.getLevel().isClientSide() && e.getChunk() instanceof LevelChunk lc) {
            StreamTint.onChunkArrived(lc);
         }
      });
      event.enqueueWork(() -> {
         ItemBlockRenderTypes.setRenderLayer((Fluid)StreamFluids.SOURCE.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)StreamFluids.FLOWING.get(), RenderType.translucent());
      });
   }

   @SubscribeEvent
   static void onRegisterBlockColors(Block event) {
      event.register(
         (state, view, pos, tintIndex) -> view != null && pos != null ? BiomeColors.getAverageWaterColor(view, pos) : -1,
         new net.minecraft.world.level.block.Block[]{(net.minecraft.world.level.block.Block)StreamFluids.STREAM_BLOCK.get()}
      );
   }

   @SubscribeEvent
   static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet((ParticleType)StreamParticles.STREAM_BUBBLE.get(), StreamBubbleParticle.Provider::new);
      event.registerSpriteSet((ParticleType)StreamParticles.STREAM_SPLASH.get(), sprites -> {
         StreamParticleScanner.splashSprites = sprites;
         return new StreamSplashParticle.Provider(sprites);
      });
      event.registerSpriteSet((ParticleType)StreamParticles.STREAM_FOAM.get(), sprites -> {
         StreamParticleScanner.foamSprites = sprites;
         return new StreamFoamParticle.Provider(sprites);
      });
      event.registerSpriteSet((ParticleType)StreamParticles.STREAM_LEAF.get(), StreamLeafParticle.Provider::new);
      event.registerSpriteSet((ParticleType)StreamParticles.STREAM_STICK.get(), StreamStickParticle.Provider::new);
   }
}
