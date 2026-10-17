package dev.streamsreflowing.offload;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.FileNetworkStore;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class PlateOffload {
   private static final long BUILD_NANOS = 240000000000L;
   private static final int CHECK_COLUMNS = 12;
   public static final int MAX_AT_ONCE = 3;
   private static final int MAX_REGION_BYTES = 67108864;
   private static final Map<UUID, PlateOffload.Peer> PEERS = new HashMap<>();
   private static final Map<PlateOffload.Ref, PlateOffload.Pending> PENDING = new HashMap<>();
   private static int nextId;
   private static int tick;
   private static final String AUTO_TEST = System.getProperty("streamsreflowing.offloadtest");
   private static boolean autoTested;
   private static final ExecutorService WORK = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "streamsreflowing-offload");
      t.setDaemon(true);
      return t;
   });
   private static final boolean TRACE = System.getProperty("streamsreflowing.caldiag") != null;

   private PlateOffload() {
   }

   private static void trace(String what, Object... args) {
      if (TRACE) {
         Constants.LOG.info("[SR-OFFLOAD] " + what, args);
      }
   }

   private static boolean enabled() {
      return Services.PLATFORM.offloadRegionBuilds();
   }

   public static void onServerTick(MinecraftServer server) {
      if (++tick % 20 == 0) {
         if (AUTO_TEST != null && tick >= 100 && !autoTested) {
            autoTested = true;
            String[] at = AUTO_TEST.split(",");
            selfTest(server.overworld(), Integer.parseInt(at[0].trim()), Integer.parseInt(at[1].trim()), line -> {
               Constants.LOG.info("[SR-OFFLOAD] {}", line);
               server.halt(false);
            });
         }

         List<Runnable> after = new ArrayList<>();
         synchronized (PlateOffload.class) {
            if (!enabled() && PEERS.isEmpty()) {
               return;
            }

            Set<UUID> online = new HashSet<>();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
               online.add(player.getUUID());
               PlateOffload.Peer peer = PEERS.computeIfAbsent(player.getUUID(), u -> new PlateOffload.Peer());
               if (enabled() && !peer.offered && peer.offerTries < 15) {
                  peer.offerTries++;

                  try {
                     ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                     DataOutputStream out = new DataOutputStream(bytes);
                     out.writeByte(1);
                     out.writeInt(2);
                     out.writeUTF(modVersion());
                     peer.offered = send(player, bytes.toByteArray());
                  } catch (IOException e) {
                     peer.offerTries = 99;
                  }
               }
            }

            long now = System.nanoTime();

            for (PlateOffload.Pending p : new ArrayList<>(PENDING.values())) {
               if (!online.contains(p.player)) {
                  after.add(finish(p));
               } else if (now > p.deadline) {
                  ServerPlayer player = server.getPlayerList().getPlayer(p.player);
                  if (player != null) {
                     cancel(player, p.id);
                  }

                  trace("region {} was not back in time; preparing it here", p.ref.key());
                  after.add(finish(p));
               }
            }

            PEERS.keySet().retainAll(online);
         }

         after.forEach(Runnable::run);
      }
   }

   public static boolean offer(RiverEngine engine, PlateKey key, Runnable then) {
      if (!enabled()) {
         return false;
      }

      ServerLevel level = StreamsWorldgen.levelOf(engine);
      if (level != null && StreamsWorldgen.plainTerrain(engine) && engine.networkStore() != null) {
         if (engine.networkStore() instanceof FileNetworkStore stored && Files.exists(stored.fileOf(key))) {
            return false;
         } else {
            PlateDomain domain = new PlateDomain(engine.lattice(), key);
            synchronized (PlateOffload.class) {
               ServerPlayer best = null;
               PlateOffload.Peer bestPeer = null;
               double bestDist = Double.MAX_VALUE;

               for (ServerPlayer player : level.players()) {
                  PlateOffload.Peer peer = PEERS.get(player.getUUID());
                  if (peer != null && peer.willing && !peer.distrusted && peer.pending.size() < peer.atOnce) {
                     double dx = player.getX() - domain.centerX;
                     double dz = player.getZ() - domain.centerZ;
                     double d = dx * dx + dz * dz;
                     if (d < bestDist) {
                        bestDist = d;
                        best = player;
                        bestPeer = peer;
                     }
                  }
               }

               if (best == null) {
                  return false;
               }

               if (!level.dimension().equals(bestPeer.worldSent)) {
                  bestPeer.worldSent = level.dimension();
                  bestPeer.worldReady = false;
                  int worldId = bestPeer.worldId = ++nextId;
                  ServerPlayer target = best;
                  trace("sending this world to {}", target.getScoreboardName());
                  WORK.execute(() -> sendWorld(target, level, engine, worldId));
                  return false;
               }

               if (!bestPeer.worldReady) {
                  return false;
               }

               PlateOffload.Pending p = new PlateOffload.Pending();
               p.id = ++nextId;
               p.ref = new PlateOffload.Ref(engine, key);
               p.player = best.getUUID();
               p.level = level;
               p.then = then;
               p.started = System.nanoTime();
               p.deadline = p.started + 240000000000L;
               p.xs = new int[12];
               p.zs = new int[12];
               ThreadLocalRandom rnd = ThreadLocalRandom.current();

               for (int i = 0; i < 12; i++) {
                  p.xs[i] = rnd.nextInt(domain.minX, domain.maxX + 1);
                  p.zs[i] = rnd.nextInt(domain.minZ, domain.maxZ + 1);
               }

               try {
                  ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                  DataOutputStream out = new DataOutputStream(bytes);
                  out.writeByte(5);
                  out.writeInt(bestPeer.worldId);
                  out.writeInt(p.id);
                  out.writeInt(key.cx());
                  out.writeInt(key.cz());
                  out.writeInt(12);

                  for (int i = 0; i < 12; i++) {
                     out.writeInt(p.xs[i]);
                     out.writeInt(p.zs[i]);
                  }

                  if (!send(best, bytes.toByteArray())) {
                     return false;
                  }
               } catch (IOException e) {
                  return false;
               }

               bestPeer.pending.add(p);
               PENDING.put(p.ref, p);
               trace("region {} handed to {}", key, best.getScoreboardName());
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean reclaim(RiverEngine engine, PlateKey key) {
      synchronized (PlateOffload.class) {
         PlateOffload.Pending p = PENDING.remove(new PlateOffload.Ref(engine, key));
         if (p != null && !p.done) {
            p.done = true;
            PlateOffload.Peer peer = PEERS.get(p.player);
            if (peer != null) {
               peer.pending.remove(p);
            }

            MinecraftServer server = p.level.getServer();
            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(p.player);
            if (player != null) {
               cancel(player, p.id);
            }

            trace("region {} taken back: a chunk needs it now", key);
            return true;
         } else {
            return false;
         }
      }
   }

   public static void onServerReceive(ServerPlayer player, byte[] frame) {
      byte[] message;
      PlateOffload.Peer peer;
      synchronized (PlateOffload.class) {
         peer = PEERS.computeIfAbsent(player.getUUID(), u -> new PlateOffload.Peer());
         message = peer.in.accept(frame);
      }

      if (message != null && message.length != 0) {
         try {
            DataInputStream in = OffloadWire.reader(message);
            byte type = in.readByte();
            if (type == 2) {
               int protocol = in.readInt();
               String version = in.readUTF();
               boolean willing = in.readBoolean();
               int atOnce = protocol == 2 ? in.readInt() : 1;
               synchronized (PlateOffload.class) {
                  peer.willing = willing && protocol == 2 && version.equals(modVersion());
                  peer.atOnce = Math.max(1, Math.min(3, atOnce));
               }

               trace("{} answered the offer: willing={} protocol={} version={} regions at once={}", player.getScoreboardName(), willing, protocol, version, atOnce);
            } else if (type == 4) {
               int worldId = in.readInt();
               boolean ok = in.readBoolean();
               String reason = in.readUTF();
               synchronized (PlateOffload.class) {
                  if (worldId == peer.worldId) {
                     peer.worldReady = ok;
                     if (!ok) {
                        peer.distrusted = true;
                     }
                  }
               }

               trace("{} has this world: ready={} {}", player.getScoreboardName(), ok, reason);
               if (!ok) {
                  Constants.LOG
                     .info("Streams Reflowing: {}'s game cannot prepare regions for this world ({}); the server prepares them.", player.getScoreboardName(), reason);
               }
            } else if (type == 6) {
               int id = in.readInt();
               boolean ok = in.readBoolean();
               int n = in.readInt();
               int[] heights = new int[Math.max(0, Math.min(n, 4096))];

               for (int i = 0; i < heights.length; i++) {
                  heights[i] = in.readInt();
               }

               byte[] packed = in.readAllBytes();
               PlateOffload.Pending p = null;
               synchronized (PlateOffload.class) {
                  for (PlateOffload.Pending candidate : peer.pending) {
                     if (candidate.id == id && !candidate.done) {
                        p = candidate;
                     }
                  }
               }

               if (p == null) {
                  return;
               }

               PlateOffload.Pending settled = p;
               String name = player.getScoreboardName();
               WORK.execute(() -> settle(settled, name, ok, heights, packed));
            }
         } catch (IOException | RuntimeException var20) {
         }
      }
   }

   private static void settle(PlateOffload.Pending p, String name, boolean ok, int[] heights, byte[] packed) {
      String refused = null;

      try {
         if (!ok) {
            trace("region {} could not be prepared by {}'s game; preparing it here", p.ref.key(), name);
            finish(p).run();
            return;
         }

         if (heights.length != p.xs.length) {
            refused = "the answer was incomplete";
         } else {
            ChunkGenerator generator = p.level.getChunkSource().getGenerator();
            RandomState randomState = p.level.getChunkSource().randomState();

            for (int i = 0; i < heights.length && refused == null; i++) {
               int ours = generator.getBaseHeight(p.xs[i], p.zs[i], Types.OCEAN_FLOOR_WG, p.level, randomState);
               if (ours != heights[i]) {
                  refused = "its terrain differs from this world's";
               }
            }
         }

         if (refused == null && !p.ref.engine().isPlateCached(p.ref.key())) {
            byte[] region = OffloadWire.inflate(packed, 67108864);
            FileNetworkStore store = (FileNetworkStore)p.ref.engine().networkStore();
            RiverNetwork net = store.adopt(p.ref.key(), region, candidate -> liesIn(candidate, p));
            if (net == null) {
               if (!p.ref.engine().isPlateCached(p.ref.key()) && !Files.exists(store.fileOf(p.ref.key()))) {
                  refused = "what it sent does not read as a region for that place";
               }
            } else {
               trace(
                  "region {} prepared by {}'s game in {} ms ({} segments, {} KB sent)",
                  p.ref.key(),
                  name,
                  (System.nanoTime() - p.started) / 1000000L,
                  net.segments().size(),
                  packed.length / 1024
               );
            }
         }
      } catch (IOException | RuntimeException e) {
         refused = "what it sent could not be read";
      }

      if (refused != null) {
         Constants.LOG
            .info("Streams Reflowing: not using a region from {}'s game ({}); the server prepares regions for that player from here on.", name, refused);
      }

      Runnable then;
      synchronized (PlateOffload.class) {
         if (refused != null) {
            PlateOffload.Peer peer = PEERS.get(p.player);
            if (peer != null) {
               peer.distrusted = true;
            }
         }

         then = finish(p);
      }

      then.run();
   }

   private static boolean liesIn(RiverNetwork net, PlateOffload.Pending p) {
      PlateDomain domain = new PlateDomain(p.ref.engine().lattice(), p.ref.key());
      double reach = Math.max(domain.maxX - domain.minX, domain.maxZ - domain.minZ);
      double minX = domain.minX - reach;
      double maxX = domain.maxX + reach;
      double minZ = domain.minZ - reach;
      double maxZ = domain.maxZ + reach;
      int low = p.level.getMinBuildHeight() - 1;
      int high = p.level.getMaxBuildHeight() + 1;

      for (RiverSegment s : net.segments()) {
         if (!(s.minX() >= minX)
            || !(s.maxX() <= maxX)
            || !(s.minZ() >= minZ)
            || !(s.maxZ() <= maxZ)
            || !(s.y1() >= low)
            || !(s.y1() <= high)
            || !(s.y2() >= low)
            || !(s.y2() <= high)) {
            return false;
         }
      }

      return true;
   }

   private static Runnable finish(PlateOffload.Pending p) {
      synchronized (PlateOffload.class) {
         if (p.done) {
            return () -> {};
         }

         p.done = true;
         PENDING.remove(p.ref, p);
         PlateOffload.Peer peer = PEERS.get(p.player);
         if (peer != null) {
            peer.pending.remove(p);
         }

         return p.then;
      }
   }

   private static void sendWorld(ServerPlayer player, ServerLevel level, RiverEngine engine, int worldId) {
      try {
         byte[] world = WorldSnapshot.capture(level, engine);
         ByteArrayOutputStream bytes = new ByteArrayOutputStream(world.length + 16);
         DataOutputStream out = new DataOutputStream(bytes);
         out.writeByte(3);
         out.writeInt(worldId);
         out.write(world);
         boolean sent = send(player, bytes.toByteArray());
         trace("this world is {} KB; sent to {}: {}", world.length / 1024, player.getScoreboardName(), sent);
      } catch (IOException | RuntimeException | Error e) {
         Constants.LOG
            .info("Streams Reflowing: this world's generator cannot be shared with players' games ({}); the server prepares every region.", e.toString());
         synchronized (PlateOffload.class) {
            PlateOffload.Peer peer = PEERS.get(player.getUUID());
            if (peer != null) {
               peer.distrusted = true;
            }
         }
      }
   }

   private static void cancel(ServerPlayer player, int id) {
      try {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(bytes);
         out.writeByte(7);
         out.writeInt(id);
         send(player, bytes.toByteArray());
      } catch (IOException var4) {
      }
   }

   private static boolean send(ServerPlayer player, byte[] message) {
      for (byte[] frame : OffloadWire.frames(message)) {
         if (!Services.PLATFORM.offloadToClient(player, frame)) {
            return false;
         }
      }

      return true;
   }

   static String modVersion() {
      return Services.PLATFORM.modVersion("streamsreflowing");
   }

   public static void clear() {
      synchronized (PlateOffload.class) {
         PEERS.clear();
         PENDING.clear();
      }
   }

   public static void selfTest(ServerLevel level, int x, int z, Consumer<String> say) {
      WORK.execute(() -> {
         String line = selfTestNow(level, x, z);
         level.getServer().execute(() -> say.accept(line));
      });
   }

   private static String selfTestNow(ServerLevel level, int x, int z) {
      RiverEngine engine = StreamsWorldgen.engine(level);

      try {
         long s0 = System.nanoTime();
         engine.baseFor(engine.plateAt(x, z));
         long serverMs = (System.nanoTime() - s0) / 1000000L;
         if (!StreamsWorldgen.plainTerrain(engine)) {
            return "This world's terrain cannot be reproduced on a player's machine.";
         }

         long t0 = System.nanoTime();
         byte[] packed = WorldSnapshot.capture(level, engine);
         long t1 = System.nanoTime();
         WorldSnapshot.World copy = WorldSnapshot.restore(packed, level.registryAccess());
         long t2 = System.nanoTime();

         try {
            PlateKey key = engine.plateAt(x, z);
            PlateDomain domain = new PlateDomain(engine.lattice(), key);
            int differ = 0;
            int checked = 64;
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            RandomState randomState = level.getChunkSource().randomState();
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            for (int i = 0; i < checked; i++) {
               int cx = rnd.nextInt(domain.minX, domain.maxX + 1);
               int cz = rnd.nextInt(domain.minZ, domain.maxZ + 1);
               if (copy.groundAt(cx, cz) != generator.getBaseHeight(cx, cz, Types.OCEAN_FLOOR_WG, level, randomState)) {
                  differ++;
               }
            }

            if (Boolean.getBoolean("streamsreflowing.offloaddiag")) {
               int dx = (domain.minX + domain.maxX) / 2;
               int dz = (domain.minZ + domain.maxZ) / 2;
               Constants.LOG
                  .info(
                     "[SR-OFFLOAD] at {},{} server: {} h={}",
                     new Object[]{dx, dz, WorldSnapshot.fields(randomState, dx, dz), generator.getBaseHeight(dx, dz, Types.OCEAN_FLOOR_WG, level, randomState)}
                  );
               Constants.LOG.info("[SR-OFFLOAD] at {},{} copy:   {} h={}", new Object[]{dx, dz, copy.fieldsAt(dx, dz), copy.groundAt(dx, dz)});
            }

            long t3 = System.nanoTime();
            byte[] theirs = copy.build(key);
            long t4 = System.nanoTime();
            long t5 = t4 + serverMs * 1000000L;
            FileNetworkStore store = (FileNetworkStore)engine.networkStore();
            byte[] ours = Files.readAllBytes(store.fileOf(key));
            boolean same = Arrays.equals(ours, theirs);
            return String.format(
               Locale.ROOT,
               "World snapshot %d KB (packed in %d ms, unpacked in %d ms). Terrain: %d of %d columns differ. Region %d,%d: copy %d ms / %d KB (%d KB sent), server %d ms / %d KB -- %s.",
               packed.length / 1024,
               (t1 - t0) / 1000000L,
               (t2 - t1) / 1000000L,
               differ,
               checked,
               key.cx(),
               key.cz(),
               (t4 - t3) / 1000000L,
               theirs.length / 1024,
               OffloadWire.deflate(theirs).length / 1024,
               (t5 - t4) / 1000000L,
               ours.length / 1024,
               same ? "identical" : "DIFFERENT"
            );
         } finally {
            copy.close();
         }
      } catch (IOException | RuntimeException | Error e) {
         Constants.LOG.info("[SR-OFFLOAD] self-test failed", e);
         return "The world could not be copied: " + e;
      }
   }

   private static final class Peer {
      final OffloadWire.Assembler in = new OffloadWire.Assembler();
      boolean offered;
      int offerTries;
      boolean willing;
      boolean distrusted;
      ResourceKey<Level> worldSent;
      int worldId;
      boolean worldReady;
      int atOnce = 1;
      final List<PlateOffload.Pending> pending = new ArrayList<>();
   }

   private static final class Pending {
      int id;
      PlateOffload.Ref ref;
      UUID player;
      ServerLevel level;
      int[] xs;
      int[] zs;
      long deadline;
      long started;
      Runnable then;
      boolean done;
   }

   private record Ref(RiverEngine engine, PlateKey key) {
   }
}
