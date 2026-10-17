package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

public final class StructurePromises {
   private static final String a = "structure_promises.txt";
   private static final ConcurrentHashMap<Path, Set<String>> b = new ConcurrentHashMap<>();

   private StructurePromises() {
   }

   private static Path a(ServerLevel var0) {
      String var1 = var0.dimension().location().getPath();
      return var0.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var1).resolve("structure_promises.txt");
   }

   private static String a(String var0, int var1, int var2) {
      return var0 + " " + var1 + " " + var2;
   }

   private static Set<String> a(Path var0) {
      return b.computeIfAbsent(var0, var0x -> {
         KeySetView var1 = ConcurrentHashMap.newKeySet();

         try {
            if (Files.exists(var0x)) {
               var1.addAll(Files.readAllLines(var0x));
            }
         } catch (IOException var3) {
            Constants.LOG.warn("Could not read the structure promise ledger at {} -- promises made before now may not be kept.", var0x, var3);
         }

         return var1;
      });
   }

   public static boolean promise(ServerLevel var0, String var1, int var2, int var3) {
      Path var4 = a(var0);
      Set var5 = a(var4);
      String var6 = a(var1, var2, var3);
      if (!var5.add(var6)) {
         return false;
      }

      try {
         Files.createDirectories(var4.getParent());
         Files.writeString(var4, var6 + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException var8) {
         Constants.LOG.warn("Could not record a structure promise in {} -- the compass may point at a structure that is later removed.", var4, var8);
      }

      return true;
   }

   public static boolean promised(ServerLevel var0, String var1, int var2, int var3) {
      return a(a(var0)).contains(a(var1, var2, var3));
   }

   public static void forget(ServerLevel var0) {
      b.remove(a(var0));
   }
}
