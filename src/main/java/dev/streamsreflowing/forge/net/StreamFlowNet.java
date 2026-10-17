package dev.streamsreflowing.forge.net;

import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.forge.client.ClientFlowHook;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkWatchEvent.Watch;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;

public final class StreamFlowNet {
   private static final String PROTOCOL = "1";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation("streamsreflowing", "flow"), () -> "1", NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1")
   );
   private static final int SCAN_INTERVAL_TICKS = 10;
   private static int scanThrottle;
   private static final Map<UUID, Set<Long>> SENT = new ConcurrentHashMap<>();
   private static final Set<UUID> WATCH_EVENT_PLAYERS = ConcurrentHashMap.newKeySet();

   private StreamFlowNet() {
   }

   public static void register() {
      CHANNEL.registerMessage(
         0, StreamFlowNet.FlowGridMsg.class, StreamFlowNet.FlowGridMsg::encode, StreamFlowNet.FlowGridMsg::decode, StreamFlowNet.FlowGridMsg::handle
      );
   }

   public static void onChunkWatch(Watch event) {
      if (StreamsSettings.vanillaWaterFlow() && (Services.PLATFORM.generateRivers() || Services.PLATFORM.flowVanillaRivers())) {
         ServerLevel level = event.getLevel();
         if (level.dimension().equals(Level.OVERWORLD)) {
            ChunkPos cp = event.getPos();
            ServerPlayer player = event.getPlayer();
            byte[] cached = StreamFlowGrid.peek(cp.x, cp.z);
            if (cached != null) {
               if (CHANNEL.isRemotePresent(player.connection.connection)) {
                  markSent(player, cp.x, cp.z);
                  if (cached.length > 0) {
                     CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StreamFlowNet.FlowGridMsg(cp.x, cp.z, cached));
                  }
               }
            } else {
               RiverEngine engine = StreamsWorldgen.engine(level);
               if (engine != null) {
                  RiverFlowField.BiomeClass classifier = RiverFlowFields.classifierFor(level);
                  RiverFlowField.OrientationField orient = RiverFlowFields.orientationFor(level);
                  boolean flowRivers = StreamsSettings.flowVanillaRivers();
                  StreamPrefetcher.submitGridBuild(level, engine, classifier, orient, flowRivers, cp.x, cp.z, true, () -> {
                     byte[] grid = StreamFlowGrid.peek(cp.x, cp.z);
                     if (grid != null && grid.length > 0) {
                        level.getServer().execute(() -> {
                           StreamFlowNet.FlowGridMsg msg = new StreamFlowNet.FlowGridMsg(cp.x, cp.z, grid);

                           for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayers(cp, false)) {
                              if (CHANNEL.isRemotePresent(watcher.connection.connection)) {
                                 markSent(watcher, cp.x, cp.z);
                                 CHANNEL.send(PacketDistributor.PLAYER.with(() -> watcher), msg);
                              }
                           }
                        });
                     }
                  });
               }
            }
         }
      }
   }

   private static long packKey(int cx, int cz) {
      return (long)cx << 32 | cz & 4294967295L;
   }

   private static void markSent(ServerPlayer player, int cx, int cz) {
      SENT.computeIfAbsent(player.getUUID(), u -> ConcurrentHashMap.newKeySet()).add(packKey(cx, cz));
   }

   public static void noteWatchEvent(Watch event) {
      if (event.getLevel().dimension().equals(Level.OVERWORLD)) {
         WATCH_EVENT_PLAYERS.add(event.getPlayer().getUUID());
      }
   }

   private static int reach(MinecraftServer server) {
      return Mth.clamp(server.getPlayerList().getViewDistance(), 2, 32);
   }

   private static boolean withinReach(ServerPlayer player, int dx, int dz, int reach) {
      return WATCH_EVENT_PLAYERS.contains(player.getUUID()) ? dx * dx + dz * dz <= reach * reach : Math.abs(dx) <= reach && Math.abs(dz) <= reach;
   }

   public static void forget(ServerPlayer player) {
      SENT.remove(player.getUUID());
      WATCH_EVENT_PLAYERS.remove(player.getUUID());
   }

   public static void clear() {
      SENT.clear();
      WATCH_EVENT_PLAYERS.clear();
   }

   public static void onServerTick(MinecraftServer server) {
      if (++scanThrottle >= 10) {
         scanThrottle = 0;
         if (StreamsSettings.vanillaWaterFlow() && (Services.PLATFORM.generateRivers() || Services.PLATFORM.flowVanillaRivers())) {
            ServerLevel level = server.overworld();
            if (level.players().isEmpty()) {
               SENT.clear();
               WATCH_EVENT_PLAYERS.clear();
            } else {
               RiverEngine engine = StreamsWorldgen.engine(level);
               if (engine != null) {
                  RiverFlowField.BiomeClass classifier = RiverFlowFields.classifierFor(level);
                  RiverFlowField.OrientationField orient = RiverFlowFields.orientationFor(level);
                  boolean flowRivers = StreamsSettings.flowVanillaRivers();
                  int reach = reach(server);
                  Set<UUID> present = new HashSet<>();

                  for (ServerPlayer player : level.players()) {
                     present.add(player.getUUID());
                     if (CHANNEL.isRemotePresent(player.connection.connection)) {
                        int pcx = player.getBlockX() >> 4;
                        int pcz = player.getBlockZ() >> 4;
                        Set<Long> sent = SENT.computeIfAbsent(player.getUUID(), u -> ConcurrentHashMap.newKeySet());
                        sent.removeIf(k -> !withinReach(player, (int)(k >> 32) - pcx, (int)k.longValue() - pcz, reach));

                        for (int dz = -reach; dz <= reach; dz++) {
                           for (int dx = -reach; dx <= reach; dx++) {
                              if (withinReach(player, dx, dz, reach)) {
                                 int cx = pcx + dx;
                                 int cz = pcz + dz;
                                 if (!sent.contains(packKey(cx, cz))) {
                                    byte[] cached = StreamFlowGrid.peek(cx, cz);
                                    if (cached != null) {
                                       sent.add(packKey(cx, cz));
                                       if (cached.length > 0) {
                                          CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StreamFlowNet.FlowGridMsg(cx, cz, cached));
                                       }
                                    } else {
                                       StreamPrefetcher.submitGridBuild(
                                          level, engine, classifier, orient, flowRivers, cx, cz, true, () -> deliverBuilt(level, cx, cz)
                                       );
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  SENT.keySet().retainAll(present);
                  WATCH_EVENT_PLAYERS.retainAll(present);
               }
            }
         }
      }
   }

   private static void deliverBuilt(ServerLevel level, int cx, int cz) {
      byte[] grid = StreamFlowGrid.peek(cx, cz);
      if (grid != null) {
         level.getServer()
            .execute(
               () -> {
                  int reach = reach(level.getServer());
                  StreamFlowNet.FlowGridMsg msg = new StreamFlowNet.FlowGridMsg(cx, cz, grid);

                  for (ServerPlayer player : level.players()) {
                     if (CHANNEL.isRemotePresent(player.connection.connection)
                        && withinReach(player, cx - (player.getBlockX() >> 4), cz - (player.getBlockZ() >> 4), reach)) {
                        markSent(player, cx, cz);
                        if (grid.length > 0) {
                           CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
                        }
                     }
                  }
               }
            );
      }
   }

   public static void onClientChunkUnload(int cx, int cz) {
      ClientFlowGrids.remove(cx, cz);
   }

   public record FlowGridMsg(int cx, int cz, byte[] grid) {
      static void encode(StreamFlowNet.FlowGridMsg m, FriendlyByteBuf buf) {
         buf.writeVarInt(m.cx);
         buf.writeVarInt(m.cz);
         buf.writeByteArray(m.grid);
      }

      static StreamFlowNet.FlowGridMsg decode(FriendlyByteBuf buf) {
         return new StreamFlowNet.FlowGridMsg(buf.readVarInt(), buf.readVarInt(), buf.readByteArray());
      }

      static void handle(StreamFlowNet.FlowGridMsg m, Supplier<Context> ctxSup) {
         Context ctx = ctxSup.get();
         ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFlowHook.store(m.cx, m.cz, m.grid)));
         ctx.setPacketHandled(true);
      }
   }
}
