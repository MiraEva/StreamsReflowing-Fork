package dev.streamsreflowing.offload;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.platform.Services;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;

public final class OffloadClient {
   private static final OffloadWire.Assembler IN = new OffloadWire.Assembler();
   private static final Set<Integer> CANCELLED = ConcurrentHashMap.newKeySet();
   private static volatile int worldId;
   private static volatile WorldSnapshot.World world;
   private static volatile boolean said;
   private static final ExecutorService WORK = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "streamsreflowing-helper");
      t.setDaemon(true);
      t.setPriority(1);
      return t;
   });
   private static final ExecutorService BUILDS = Executors.newFixedThreadPool(3, r -> {
      Thread t = new Thread(r, "streamsreflowing-helper-region");
      t.setDaemon(true);
      t.setPriority(1);
      return t;
   });
   private static final boolean TRACE = System.getProperty("streamsreflowing.caldiag") != null;

   private OffloadClient() {
   }

   private static void trace(String what, Object... args) {
      if (TRACE) {
         Constants.LOG.info("[SR-OFFLOAD] " + what, args);
      }
   }

   public static void onReceive(byte[] frame) {
      byte[] message = IN.accept(frame);
      if (message != null && message.length != 0) {
         try {
            DataInputStream in = OffloadWire.reader(message);
            byte type = in.readByte();
            if (type == 1) {
               int protocol = in.readInt();
               String version = in.readUTF();
               drop();
               boolean willing = Services.PLATFORM.helpPrepareRegions() && protocol == 2 && version.equals(PlateOffload.modVersion());
               ByteArrayOutputStream bytes = new ByteArrayOutputStream();
               DataOutputStream out = new DataOutputStream(bytes);
               out.writeByte(2);
               out.writeInt(2);
               out.writeUTF(PlateOffload.modVersion());
               out.writeBoolean(willing);
               out.writeInt(Math.max(1, Math.min(3, Services.PLATFORM.helpRegionsAtOnce())));
               send(bytes.toByteArray());
            } else if (type == 3) {
               int id = in.readInt();
               byte[] packed = in.readAllBytes();
               RegistryAccess local = localRegistries();
               trace("the server sent its world ({} KB)", packed.length / 1024);
               WORK.execute(() -> takeWorld(id, packed, local));
            } else if (type == 5) {
               int forWorld = in.readInt();
               int id = in.readInt();
               PlateKey key = new PlateKey(in.readInt(), in.readInt());
               int n = Math.max(0, Math.min(in.readInt(), 4096));
               int[] xs = new int[n];
               int[] zs = new int[n];

               for (int i = 0; i < n; i++) {
                  xs[i] = in.readInt();
                  zs[i] = in.readInt();
               }

               trace("asked to prepare region {}", key);
               BUILDS.execute(() -> build(forWorld, id, key, xs, zs));
            } else if (type == 7) {
               CANCELLED.add(in.readInt());
            }
         } catch (IOException | RuntimeException | Error e) {
            trace("a message from the server could not be handled: {}", e.toString());
         }
      }
   }

   private static RegistryAccess localRegistries() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      return connection == null ? null : connection.registryAccess();
   }

   private static void takeWorld(int id, byte[] packed, RegistryAccess local) {
      String reason = "";
      WorldSnapshot.World made = null;

      try {
         if (local == null) {
            reason = "not in a world";
         } else {
            made = WorldSnapshot.restore(packed, local);
         }
      } catch (Throwable t) {
         reason = String.valueOf(t.getMessage() == null ? t : t.getMessage());
         if (reason.length() > 200) {
            reason = reason.substring(0, 200);
         }
      }

      trace("the server's world {}: {}", made != null ? "is rebuilt here" : "could not be rebuilt", reason);
      drop();
      world = made;
      worldId = id;
      if (made != null && !said) {
         said = true;
         Constants.LOG.info("Streams Reflowing: helping this server prepare streams for the land you explore. (Setting: helpPrepareRegions.)");
      }

      try {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(bytes);
         out.writeByte(4);
         out.writeInt(id);
         out.writeBoolean(made != null);
         out.writeUTF(reason);
         send(bytes.toByteArray());
      } catch (IOException var7) {
      }
   }

   private static void build(int forWorld, int id, PlateKey key, int[] xs, int[] zs) {
      WorldSnapshot.World w = world;
      boolean ok = false;
      int[] heights = new int[0];
      byte[] region = new byte[0];
      if (w != null && forWorld == worldId && !CANCELLED.remove(id)) {
         try {
            heights = new int[xs.length];

            for (int i = 0; i < xs.length; i++) {
               heights[i] = w.groundAt(xs[i], zs[i]);
            }

            region = OffloadWire.deflate(w.build(key));
            ok = true;
         } catch (Throwable t) {
            Constants.LOG.debug("Could not prepare river region {} for the server ({})", key, t.toString());
         }
      }

      if (CANCELLED.remove(id)) {
         trace("region {} was taken back by the server", key);
      } else {
         trace("region {} {} ({} KB to send)", key, ok ? "prepared" : "could not be prepared", region.length / 1024);

         try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(region.length + 64);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(6);
            out.writeInt(id);
            out.writeBoolean(ok);
            out.writeInt(ok ? heights.length : 0);
            if (ok) {
               for (int h : heights) {
                  out.writeInt(h);
               }

               out.write(region);
            }

            send(bytes.toByteArray());
         } catch (IOException var15) {
         }
      }
   }

   private static void send(byte[] message) {
      for (byte[] frame : OffloadWire.frames(message)) {
         Services.PLATFORM.offloadToServer(frame);
      }
   }

   private static void drop() {
      WorldSnapshot.World old = world;
      world = null;
      CANCELLED.clear();
      if (old != null) {
         WORK.execute(old::close);
      }
   }
}
