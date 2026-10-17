package dev.streamsreflowing.forge;

import dev.streamsreflowing.StreamsReflowing;
import dev.streamsreflowing.command.RegenPending;
import dev.streamsreflowing.command.StreamsCommands;
import dev.streamsreflowing.compat.FlowingFluidsCompat;
import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import dev.streamsreflowing.forge.net.OffloadNet;
import dev.streamsreflowing.forge.net.StreamFlowNet;
import dev.streamsreflowing.forge.particle.StreamParticles;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.bank.BankStyle;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("streamsreflowing")
public class StreamsReflowingForge {
   private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, "streamsreflowing");

   public StreamsReflowingForge() {
      StreamsReflowing.init();
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      FEATURES.register(modEventBus);
      OverworldFeaturesModifier.register(modEventBus);
      StreamFluids.register(modEventBus);
      if (FMLEnvironment.dist != Dist.DEDICATED_SERVER || StreamsSettings.serverStreamParticlesFromFile()) {
         StreamParticles.register(modEventBus);
      }

      ConfigMigration.migrate(FMLPaths.CONFIGDIR.get());
      StreamsSettings.ensureLoaded();
      StreamsSettings.startWatching();
      MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> StreamsSettings.load());
      modEventBus.addListener((DataPackRegistryEvent.NewRegistry e) -> e.dataPackRegistry(BankStyle.REGISTRY_KEY, BankStyle.CODEC));
      MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
         if (e.phase == Phase.END) {
            StreamPrefetcher.onServerTick(e.getServer());
            StreamFlowNet.onServerTick(e.getServer());
         }
      });
      MinecraftForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> {
         RegenPending.applyAtBoot(e.getServer());
         FlowingFluidsCompat.exemptFluidsEarly(e.getServer(), StreamFluids.SOURCE.get(), StreamFluids.FLOWING.get());
      });
      MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> FlowingFluidsCompat.exemptFluids(e.getServer(), StreamFluids.SOURCE.get(), StreamFluids.FLOWING.get()));
      MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
         StreamPrefetcher.onServerStopping();
         StreamsWorldgen.clearActive();
         StreamFlowNet.clear();
      });
      MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
         if (e.getLevel() instanceof ServerLevel sl) {
            StreamsWorldgen.unloadLevel(sl);
            StreamFlowGrid.clear();
         } else if (e.getLevel() instanceof Level lv && lv.isClientSide()) {
            ClientFlowGrids.clear();
         }
      });
      StreamFlowNet.register();
      OffloadNet.register();
      MinecraftForge.EVENT_BUS.addListener((ChunkWatchEvent.Watch e) -> {
         StreamFlowNet.noteWatchEvent(e);
         StreamFlowNet.onChunkWatch(e);
      });
      MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
         if (e.getEntity() instanceof ServerPlayer sp) {
            StreamFlowNet.forget(sp);
         }
      });
      MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
         if (e.getEntity() instanceof ServerPlayer sp) {
            StreamFlowNet.forget(sp);
         }
      });
      MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
         if (e.getLevel() instanceof Level lv && lv.dimension().equals(Level.OVERWORLD)) {
            byte[] u = RTFBridge.flowUnderlay(e.getChunk());
            if (u != null) {
               RTFFlowUnderlay.put(lv.isClientSide(), e.getChunk().getPos().getMinBlockX() >> 4, e.getChunk().getPos().getMinBlockZ() >> 4, u);
            }
         }
      });
      MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> {
         if (e.getLevel() instanceof Level lv) {
            RTFFlowUnderlay.remove(lv.isClientSide(), e.getChunk().getPos().getMinBlockX() >> 4, e.getChunk().getPos().getMinBlockZ() >> 4);
         }
      });
      MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> {
         if (e.getLevel() instanceof Level lv && lv.isClientSide() && lv.dimension().equals(Level.OVERWORLD)) {
            ChunkPos cp = e.getChunk().getPos();
            StreamFlowNet.onClientChunkUnload(cp.x, cp.z);
         }
      });
      MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> StreamsCommands.register(e.getDispatcher()));
      MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
         if (e.getEntity() instanceof ServerPlayer sp && StreamsCommands.shouldWelcome(sp)) {
            sp.sendSystemMessage(StreamsCommands.welcomeMessage());
         }
      });
   }

   static {
      FEATURES.register("river_carver", () -> StreamsWorldgen.RIVER_CARVER);
      FEATURES.register("stream_vegetation", () -> StreamsWorldgen.STREAM_VEGETATION);
      FEATURES.register("snow_cleanup", () -> StreamsWorldgen.SNOW_CLEANUP);
      FEATURES.register("river_flow", () -> StreamsWorldgen.RIVER_FLOW);
      FEATURES.register("water_skin", () -> StreamsWorldgen.WATER_SKIN);
      FEATURES.register("bank_rock", () -> StreamsWorldgen.BANK_ROCK);
      FEATURES.register("fallen_log", () -> StreamsWorldgen.FALLEN_LOG);
      FEATURES.register("bank_template", () -> StreamsWorldgen.BANK_TEMPLATE);
   }
}
