package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

final class TerrainModeStore {
   private static final int a = 1397904461;
   private static final int b = 3;
   private static final String c = "terrainmode.bin";

   private TerrainModeStore() {
   }

   private static Path a(ServerLevel var0) {
      String var1 = var0.dimension().location().toString().replace(':', '_').replace('/', '_');
      return var0.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var1).resolve("terrainmode.bin");
   }

   static Boolean a(ServerLevel var0, long var1) {
      Path var3;
      try {
         var3 = a(var0);
      } catch (RuntimeException var8) {
         return null;
      }

      if (!Files.isRegularFile(var3)) {
         return null;
      }

      try (DataInputStream var4 = new DataInputStream(Files.newInputStream(var3))) {
         return var4.readInt() != 1397904461 || var4.readInt() != 3 || var4.readLong() != var1 ? null : var4.readBoolean();
      } catch (Exception var10) {
         return null;
      }
   }

   static void a(ServerLevel var0, long var1, boolean var3) {
      try {
         Path var4 = a(var0);
         Files.createDirectories(var4.getParent());

         try (DataOutputStream var5 = new DataOutputStream(Files.newOutputStream(var4))) {
            var5.writeInt(1397904461);
            var5.writeInt(3);
            var5.writeLong(var1);
            var5.writeBoolean(var3);
         }
      } catch (Exception var10) {
         Constants.LOG.warn("Streams Reflowing: could not record this world's terrain mode ({}); it will be decided again next time.", var10.toString());
      }
   }
}
