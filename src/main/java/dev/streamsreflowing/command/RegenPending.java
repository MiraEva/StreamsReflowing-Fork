package dev.streamsreflowing.command;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

public final class RegenPending {
   private RegenPending() {
   }

   private static Path pendingFile(Path worldRoot) {
      return worldRoot.resolve("streamsreflowing").resolve("regen_pending.txt");
   }

   public static void mark(ServerLevel level, int x, int z, int radiusChunks) throws IOException {
      Path root = level.getServer().getWorldPath(LevelResource.ROOT);
      Path file = pendingFile(root);
      Files.createDirectories(file.getParent());
      StringBuilder sb = new StringBuilder();
      if (Files.exists(file)) {
         sb.append(Files.readString(file, StandardCharsets.UTF_8));
      }

      sb.append("trace ").append(x).append(' ').append(z).append(' ').append(8 + radiusChunks * 16).append('\n');
      int ccx = x >> 4;
      int ccz = z >> 4;

      for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
         for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
            sb.append("chunk ").append(ccx + dx).append(' ').append(ccz + dz).append('\n');
         }
      }

      Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
   }

   public static void applyAtBoot(MinecraftServer server) {
      Path root = server.getWorldPath(LevelResource.ROOT);
      Path file = pendingFile(root);
      if (Files.exists(file)) {
         List<int[]> chunks = new ArrayList<>();
         int[] trace = null;

         try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
               String[] p = line.trim().split("\\s+");
               if (p.length == 4 && p[0].equals("trace")) {
                  trace = new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])};
               } else if (p.length == 3 && p[0].equals("chunk")) {
                  chunks.add(new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2])});
               }
            }
         } catch (IOException | NumberFormatException e) {
            Constants.LOG.warn("[SR-REGEN] unreadable regen_pending.txt; ignoring", e);
            return;
         }

         int wiped = 0;

         for (String dir : new String[]{"region", "entities", "poi"}) {
            Map<Path, List<int[]>> byRegion = new HashMap<>();

            for (int[] c : chunks) {
               Path mca = root.resolve(dir).resolve("r." + (c[0] >> 5) + "." + (c[1] >> 5) + ".mca");
               if (Files.exists(mca)) {
                  byRegion.computeIfAbsent(mca, k -> new ArrayList<>()).add(c);
               }
            }

            for (Entry<Path, List<int[]>> e : byRegion.entrySet()) {
               try (RandomAccessFile raf = new RandomAccessFile(e.getKey().toFile(), "rw")) {
                  for (int[] c : e.getValue()) {
                     int idx = (c[0] & 31) + (c[1] & 31) * 32;
                     raf.seek(idx * 4L);
                     raf.writeInt(0);
                     raf.seek(4096L + idx * 4L);
                     raf.writeInt(0);
                     if (dir.equals("region")) {
                        wiped++;
                     }
                  }
               } catch (IOException ex) {
                  Constants.LOG.warn("[SR-REGEN] failed wiping {} -- skipped", e.getKey(), ex);
               }
            }
         }

         if (trace != null) {
            RiverCarverFeature.armTraceOverride(trace[0], trace[1], trace[2]);
         }

         try {
            Files.delete(file);
         } catch (IOException var17) {
         }

         Constants.LOG
            .info(
               "[SR-REGEN] wiped {} chunk(s) for regeneration; carve trace armed{}",
               wiped,
               trace == null ? "" : " at (" + trace[0] + "," + trace[1] + ") r=" + trace[2]
            );
      }
   }
}
