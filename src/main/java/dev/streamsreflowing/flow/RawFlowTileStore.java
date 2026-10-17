package dev.streamsreflowing.flow;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.diag.MemBytes;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class RawFlowTileStore {
   private static final int MAGIC = 1397904433;
   private static final int VERSION = 2;
   private static final int REGION_CHUNKS = 32;
   private static final int PER_REGION = 1024;
   private static volatile Path storeDir;
   private static volatile long fingerprint;
   private static final Map<Long, RawFlowTileStore.Region> REGIONS = new ConcurrentHashMap<>();
   private static final int MAX_IDLE_REGIONS = 16;
   private static final long IDLE_NANOS = 30000000000L;
   private static final ExecutorService FLUSH_EXEC = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "streamsreflowing-tileflush");
      t.setDaemon(true);
      t.setPriority(1);
      return t;
   });
   private static final AtomicBoolean FLUSHING = new AtomicBoolean();

   private RawFlowTileStore() {
   }

   public static long[] memoryStats() {
      long regions = 0L;
      long tiles = 0L;
      long bytes = 0L;

      for (RawFlowTileStore.Region r : REGIONS.values()) {
         regions++;
         bytes += MemBytes.array(r.tiles.length, 4) + MemBytes.object(8);

         for (RawFlowTiles.Tile t : r.tiles) {
            if (t != null) {
               tiles++;
               bytes += RawFlowTiles.tileBytes(t);
            }
         }
      }

      return new long[]{regions, tiles, bytes + MemBytes.hashMapNodes(regions)};
   }

   public static void initStore(Path dir, long fp) {
      storeDir = dir;
      fingerprint = fp;
      REGIONS.clear();
   }

   private static boolean enabled() {
      return storeDir != null;
   }

   private static long regionKey(int rx, int rz) {
      return (long)rx << 32 ^ rz & 4294967295L;
   }

   private static int slot(int cx, int cz) {
      return Math.floorMod(cz, 32) * 32 + Math.floorMod(cx, 32);
   }

   public static RawFlowTiles.Tile load(int cx, int cz) {
      if (!enabled()) {
         return null;
      }

      int rx = Math.floorDiv(cx, 32);
      int rz = Math.floorDiv(cz, 32);
      RawFlowTileStore.Region region = REGIONS.computeIfAbsent(regionKey(rx, rz), k -> readRegion(rx, rz));
      region.lastTouch = System.nanoTime();
      return region.tiles[slot(cx, cz)];
   }

   public static void store(int cx, int cz, RawFlowTiles.Tile tile) {
      if (enabled()) {
         int rx = Math.floorDiv(cx, 32);
         int rz = Math.floorDiv(cz, 32);
         long key = regionKey(rx, rz);
         RawFlowTileStore.Region region = REGIONS.computeIfAbsent(key, k -> readRegion(rx, rz));
         region.lastTouch = System.nanoTime();
         region.tiles[slot(cx, cz)] = tile;
         region.dirty = true;
         if (REGIONS.get(key) != region) {
            RawFlowTileStore.Region live = REGIONS.computeIfAbsent(key, k -> readRegion(rx, rz));
            live.lastTouch = System.nanoTime();
            live.tiles[slot(cx, cz)] = tile;
            live.dirty = true;
         }
      }
   }

   private static Path fileFor(int rx, int rz) {
      return storeDir.resolve("t." + rx + "." + rz + ".srt");
   }

   private static RawFlowTileStore.Region readRegion(int rx, int rz) {
      RawFlowTileStore.Region region = new RawFlowTileStore.Region();
      Path f = fileFor(rx, rz);
      if (!Files.isRegularFile(f)) {
         return region;
      }

      try (
         InputStream raw = Files.newInputStream(f);
         DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(raw)));
      ) {
         if (in.readInt() != 1397904433 || in.readInt() != 2 || in.readLong() != fingerprint) {
            return region;
         }

         byte[] present = new byte[128];
         in.readFully(present);

         for (int i = 0; i < 1024; i++) {
            if ((present[i >> 3] & 1 << (i & 7)) != 0) {
               byte[] dirX = new byte[256];
               byte[] dirZ = new byte[256];
               short[] surfaceY = new short[256];
               short[] bedY = new short[256];
               in.readFully(dirX);
               in.readFully(dirZ);

               for (int c = 0; c < 256; c++) {
                  surfaceY[c] = in.readShort();
               }

               for (int c = 0; c < 256; c++) {
                  bedY[c] = in.readShort();
               }

               region.tiles[i] = new RawFlowTiles.Tile(dirX, dirZ, surfaceY, bedY);
            }
         }

         return region;
      } catch (IOException | RuntimeException e) {
         Constants.LOG.debug("Flow-tile region ({},{}) unreadable, rebuilding: {}", new Object[]{rx, rz, e.toString()});
         return new RawFlowTileStore.Region();
      }
   }

   public static void flushAsync() {
      if (enabled() && FLUSHING.compareAndSet(false, true)) {
         FLUSH_EXEC.execute(() -> {
            try {
               flush();
            } finally {
               FLUSHING.set(false);
            }
         });
      }
   }

   public static void flush() {
      if (enabled()) {
         for (Entry<Long, RawFlowTileStore.Region> e : REGIONS.entrySet()) {
            RawFlowTileStore.Region region = e.getValue();
            if (region.dirty) {
               region.dirty = false;
               long k = e.getKey();
               writeRegion((int)(k >> 32), (int)k, region);
            }
         }

         dropIdleRegions();
      }
   }

   private static void dropIdleRegions() {
      int over = REGIONS.size() - 16;
      if (over > 0) {
         long now = System.nanoTime();
         List<Entry<Long, RawFlowTileStore.Region>> idle = new ArrayList<>();

         for (Entry<Long, RawFlowTileStore.Region> e : REGIONS.entrySet()) {
            RawFlowTileStore.Region r = e.getValue();
            if (!r.dirty && now - r.lastTouch > 30000000000L) {
               idle.add(e);
            }
         }

         idle.sort(Comparator.comparingLong(e -> e.getValue().lastTouch));

         for (int i = 0; i < idle.size() && over > 0; i++) {
            RawFlowTileStore.Region r = idle.get(i).getValue();
            if (!r.dirty && now - r.lastTouch > 30000000000L && REGIONS.remove(idle.get(i).getKey(), r)) {
               over--;
            }
         }
      }
   }

   private static void writeRegion(int rx, int rz, RawFlowTileStore.Region region) {
      try {
         Files.createDirectories(storeDir);
         byte[] present = new byte[128];
         int count = 0;

         for (int i = 0; i < 1024; i++) {
            if (region.tiles[i] != null && hasFlow(region.tiles[i])) {
               present[i >> 3] = (byte)(present[i >> 3] | (byte)(1 << (i & 7)));
               count++;
            }
         }

         Path tmp = fileFor(rx, rz).resolveSibling(fileFor(rx, rz).getFileName() + ".tmp");

         try (
            OutputStream raw = Files.newOutputStream(tmp);
            DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(raw)));
         ) {
            out.writeInt(1397904433);
            out.writeInt(2);
            out.writeLong(fingerprint);
            out.write(present);

            for (int i = 0; i < 1024; i++) {
               if ((present[i >> 3] & 1 << (i & 7)) != 0) {
                  RawFlowTiles.Tile t = region.tiles[i];
                  out.write(t.dirX());
                  out.write(t.dirZ());

                  for (short s : t.surfaceY()) {
                     out.writeShort(s);
                  }

                  for (short s : t.bedY()) {
                     out.writeShort(s);
                  }
               }
            }
         }

         Files.move(tmp, fileFor(rx, rz), StandardCopyOption.REPLACE_EXISTING);
         Constants.LOG.debug("Saved flow-tile region ({},{}): {} chunks with flow", new Object[]{rx, rz, count});
      } catch (IOException | RuntimeException e) {
         Constants.LOG.debug("Could not save flow-tile region ({},{}): {}", new Object[]{rx, rz, e.toString()});
      }
   }

   private static boolean hasFlow(RawFlowTiles.Tile t) {
      for (int i = 0; i < t.dirX().length; i++) {
         if (t.dirX()[i] != 0 || t.dirZ()[i] != 0) {
            return true;
         }
      }

      return false;
   }

   public static void close() {
      try {
         FLUSH_EXEC.submit(RawFlowTileStore::flush).get(30L, TimeUnit.SECONDS);
      } catch (Exception e) {
         flush();
      }

      REGIONS.clear();
      storeDir = null;
   }

   private static final class Region {
      final RawFlowTiles.Tile[] tiles = new RawFlowTiles.Tile[1024];
      volatile boolean dirty;
      volatile long lastTouch = System.nanoTime();
   }
}
