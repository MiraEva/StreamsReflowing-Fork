package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.math.Hashing;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class StreamTint {
   public static final ResourceKey<Biome> STREAM_BIOME = ResourceKey.create(Registries.BIOME, new ResourceLocation("streamsreflowing", "stream"));
   private static final int GRID = 8;
   private static final int MAX_RINGS = 6;
   private static final int COARSE_RINGS = 24;
   private static final int MAX_ENTRIES = 32768;
   private static final ConcurrentHashMap<Long, Object> CACHE = new ConcurrentHashMap<>();
   private static final Object NO_LAND = new Object();
   private static final Object INCOMPLETE = new Object();
   private static volatile Biome oursInstance;
   private static volatile Object oursLevel;
   private static final ConcurrentHashMap<Long, StreamTint.Node> NODES = new ConcurrentHashMap<>();
   private static final int NODE_MAX_ENTRIES = 262144;
   private static volatile Object nodesLevel;
   private static final Set<Long> RETRY = ConcurrentHashMap.newKeySet();
   private static int retryTicks;

   private static long cellKey(int gx, int gz) {
      return Hashing.mix((long)gx << 32 ^ gz & 4294967295L);
   }

   private StreamTint() {
   }

   public static void clear() {
      CACHE.clear();
      NODES.clear();
      RETRY.clear();
      oursInstance = null;
      oursLevel = null;
   }

   public static ColorResolver wrap(ColorResolver delegate, StreamTint.Kind kind) {
      return delegate == null ? null : (biome, x, z) -> {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && biome == ourBiome(level)) {
            int xi = (int)Math.floor(x);
            int zi = (int)Math.floor(z);
            int gx = Math.floorDiv(xi, 8);
            int gz = Math.floorDiv(zi, 8);
            double fx = (x - (gx * 8 + 4.0)) / 8.0;
            double fz = (z - (gz * 8 + 4.0)) / 8.0;
            int x0 = fx < 0.0 ? gx - 1 : gx;
            int z0 = fz < 0.0 ? gz - 1 : gz;
            double tx = fx < 0.0 ? fx + 1.0 : fx;
            double tz = fz < 0.0 ? fz + 1.0 : fz;
            int c00 = cellColor(biome, kind, x0, z0, x, z);
            int c10 = cellColor(biome, kind, x0 + 1, z0, x, z);
            int c01 = cellColor(biome, kind, x0, z0 + 1, x, z);
            int c11 = cellColor(biome, kind, x0 + 1, z0 + 1, x, z);
            return c00 == c10 && c00 == c01 && c00 == c11 ? c00 : lerp2(c00, c10, c01, c11, tx, tz);
         } else {
            return delegate.getColor(biome, x, z);
         }
      };
   }

   public static void debugMap() {
      ClientLevel level = Minecraft.getInstance().level;
      LocalPlayer player = Minecraft.getInstance().player;
      if (level != null && player != null) {
         int pgx = Math.floorDiv((int)player.getX(), 8);
         int pgz = Math.floorDiv((int)player.getZ(), 8);
         Biome ours = ourBiome(level);
         Constants.LOG
            .info(
               "[SR-TINTCLIENT] around cell ({},{}) oursRegistered={} cache={} retryBacklog={}",
               new Object[]{pgx, pgz, ours != null, CACHE.size(), RETRY.size()}
            );
         StringBuilder samples = new StringBuilder();
         LinkedHashMap<Biome, Character> letters = new LinkedHashMap<>();

         for (int gz = pgz - 3; gz <= pgz + 3; gz++) {
            StringBuilder row = new StringBuilder();

            for (int gx = pgx - 3; gx <= pgx + 3; gx++) {
               long key = cellKey(gx, gz);
               Object hit = CACHE.get(key);
               char c;
               if (hit == null) {
                  c = '.';
               } else if (hit == NO_LAND) {
                  c = 'N';
               } else {
                  Biome borrowed = (Biome)hit;
                  Character known = letters.get(borrowed);
                  if (known == null) {
                     known = (char)(97 + letters.size() % 26);
                     letters.put(borrowed, known);
                  }

                  c = known;
               }

               if (c == '.' || c == 'N') {
                  int cx = gx * 8 + 4;
                  int cz = gz * 8 + 4;
                  int y = level.getHeight(Types.MOTION_BLOCKING, cx, cz) - 1;
                  boolean cellIsOurs = level.getBiome(new BlockPos(cx, y, cz)).value() == ours;
                  if (cellIsOurs) {
                     boolean[] inc = new boolean[]{false};
                     Biome found = nodeSpiral(gx, gz, inc);
                     c = (char)(found != null ? (c == 'N' ? 82 : 102) : (inc[0] ? 105 : 78));
                     if (samples.length() < 700) {
                        samples.append(
                           String.format(" (%d,%d %c y=%d found=%s inc=%b)", cx, cz, c, y, found == null ? "-" : found.toString().hashCode() % 1000, inc[0])
                        );
                     }
                  }
               }

               row.append(c);
            }

            Constants.LOG.info("[SR-TINTCLIENT]   {}", row);
         }

         Registry<Biome> reg = (Registry<Biome>)level.registryAccess().registry(Registries.BIOME).orElse(null);

         for (Entry<Biome, Character> e : letters.entrySet()) {
            String name = reg != null ? String.valueOf(reg.getKey(e.getKey())) : "?";
            Constants.LOG
               .info(
                  "[SR-TINTCLIENT] legend {} = {} water=#{} grass=#{}",
                  new Object[]{e.getValue(), name, Integer.toHexString(e.getKey().getWaterColor()), Integer.toHexString(e.getKey().getGrassColor(0.0, 0.0))}
               );
         }

         if (ours != null) {
            Constants.LOG.info("[SR-TINTCLIENT] ours registered water=#{} (the fallback blue)", Integer.toHexString(ours.getWaterColor()));
         }

         Constants.LOG
            .info(
               "[SR-TINTCLIENT] other: N=cached-noland(FALLBACK BLUE) R=stale-noland-would-fix f=uncached-would-find i=incomplete(unloaded) .=dry/never-queried; samples:{}",
               samples
            );
      }
   }

   private static int cellColor(Biome self, StreamTint.Kind kind, int gx, int gz, double x, double z) {
      Biome land = landBiomeAt(gx * 8 + 4, gz * 8 + 4);
      Biome src = land != null ? land : self;

      return switch (kind) {
         case WATER -> src.getWaterColor();
         case GRASS -> src.getGrassColor(x, z);
         case FOLIAGE -> src.getFoliageColor();
      };
   }

   private static int lerp2(int c00, int c10, int c01, int c11, double tx, double tz) {
      int r = ch(c00, 16, c10, c01, c11, tx, tz);
      int g = ch(c00, 8, c10, c01, c11, tx, tz);
      int b = ch(c00, 0, c10, c01, c11, tx, tz);
      return r << 16 | g << 8 | b;
   }

   private static int ch(int c00, int shift, int c10, int c01, int c11, double tx, double tz) {
      double a = (c00 >> shift & 0xFF) * (1.0 - tx) + (c10 >> shift & 0xFF) * tx;
      double b = (c01 >> shift & 0xFF) * (1.0 - tx) + (c11 >> shift & 0xFF) * tx;
      int v = (int)Math.round(a * (1.0 - tz) + b * tz);
      return v < 0 ? 0 : Math.min(v, 255);
   }

   private static Biome ourBiome(ClientLevel level) {
      if (oursLevel != level) {
         oursInstance = level.registryAccess().registry(Registries.BIOME).flatMap(reg -> reg.getOptional(STREAM_BIOME)).orElse(null);
         oursLevel = level;
      }

      return oursInstance;
   }

   public static void onChunkArrived(LevelChunk chunk) {
      Object lvl = chunk.getLevel();
      if (nodesLevel != lvl) {
         nodesLevel = lvl;
         NODES.clear();
         CACHE.clear();
         RETRY.clear();
      }

      if (NODES.size() > 262144) {
         NODES.clear();
      }

      int minX = chunk.getPos().getMinBlockX();
      int minZ = chunk.getPos().getMinBlockZ();

      for (int nx = 0; nx < 2; nx++) {
         for (int nz = 0; nz < 2; nz++) {
            int wx = minX + nx * 8 + 4;
            int wz = minZ + nz * 8 + 4;
            int y = chunk.getHeight(Types.MOTION_BLOCKING, wx & 15, wz & 15);
            Biome b = (Biome)chunk.getNoiseBiome(wx >> 2, y >> 2, wz >> 2).value();
            NODES.put(cellKey(Math.floorDiv(wx, 8), Math.floorDiv(wz, 8)), new StreamTint.Node(b, y + 1));
         }
      }
   }

   private static Biome landBiomeAt(int x, int z) {
      int gx = Math.floorDiv(x, 8);
      int gz = Math.floorDiv(z, 8);
      long key = cellKey(gx, gz);
      Object hit = CACHE.get(key);
      if (hit != null) {
         if (hit == INCOMPLETE) {
            retryLater(gx, gz, x, z);
            return null;
         } else {
            return hit == NO_LAND ? null : (Biome)hit;
         }
      } else {
         if (CACHE.size() > 32768) {
            CACHE.clear();
         }

         boolean[] incomplete = new boolean[]{false};
         Biome found = nodeSpiral(gx, gz, incomplete);
         if (found == null && incomplete[0]) {
            CACHE.put(key, INCOMPLETE);
            retryLater(gx, gz, x, z);
         } else {
            CACHE.put(key, found == null ? NO_LAND : found);
         }

         return found;
      }
   }

   private static void retryLater(int gx, int gz, int x, int z) {
      StreamTint.Node node = NODES.get(cellKey(gx, gz));
      if (node != null) {
         RETRY.add(SectionPos.asLong(x >> 4, node.surfaceY() >> 4, z >> 4));
      }
   }

   private static Biome nodeSpiral(int gx, int gz, boolean[] incomplete) {
      Biome ours = oursInstance;

      for (int ring = 1; ring <= 24; ring++) {
         for (int i = -ring; i <= ring; i++) {
            long[] keys = new long[]{cellKey(gx + i, gz - ring), cellKey(gx + i, gz + ring), cellKey(gx - ring, gz + i), cellKey(gx + ring, gz + i)};

            for (long nk : keys) {
               StreamTint.Node n = NODES.get(nk);
               if (n == null) {
                  incomplete[0] = true;
               } else if (n.biome() != ours) {
                  return n.biome();
               }
            }
         }
      }

      return null;
   }

   public static void clientTick() {
      if (++retryTicks % 40 == 0 && !RETRY.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.levelRenderer != null) {
            CACHE.values().removeIf(v -> v == INCOMPLETE);
            int budget = 64;
            Iterator<Long> it = RETRY.iterator();

            while (it.hasNext() && budget-- > 0) {
               long s = it.next();
               it.remove();
               int sx = SectionPos.x(s);
               int sy = SectionPos.y(s);
               int sz = SectionPos.z(s);
               mc.level.onChunkLoaded(new ChunkPos(sx, sz));
               mc.levelRenderer.setSectionDirty(sx, sy, sz);
            }
         } else {
            RETRY.clear();
         }
      }
   }

   public enum Kind {
      WATER,
      GRASS,
      FOLIAGE;
   }

   private record Node(Biome biome, int surfaceY) {
   }
}
