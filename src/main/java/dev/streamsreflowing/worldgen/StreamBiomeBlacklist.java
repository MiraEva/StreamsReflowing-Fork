package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.platform.Services;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class StreamBiomeBlacklist {
   public static final TagKey<Biome> NO_STREAMS = TagKey.create(Registries.BIOME, new ResourceLocation("streamsreflowing", "no_streams"));
   private static volatile String a;
   private static volatile Set<ResourceLocation> b = Set.of();
   private static volatile Set<String> c = Set.of();
   private static volatile List<TagKey<Biome>> d = List.of();
   private static final String e = "blacklist.pin";
   private static final String f = "planning";
   private static final String g = "carve";

   private StreamBiomeBlacklist() {
   }

   static boolean a(Path var0) {
      boolean var1 = false;

      try {
         Path var2 = var0.resolve("blacklist.pin");
         if (Files.isRegularFile(var2)) {
            return "planning".equals(Files.readString(var2).trim());
         }

         if (Files.isDirectory(var0)) {
             try (Stream<Path> var3 = Files.list(var0)) {
               var1 = var3.anyMatch(var0x -> {
                  String var1x = var0x.getFileName().toString();
                  return var1x.startsWith("r.") && var1x.endsWith(".srn");
               });
            }
         }

         Files.createDirectories(var0);
         Files.writeString(var2, (var1 ? "carve" : "planning") + System.lineSeparator());
         if (var1) {
            Constants.LOG
               .info(
                  "Streams Reflowing: this world's streams were planned before blacklisted biomes stopped streams from starting, so it keeps cutting streams off at those biomes; new worlds use the new rule."
               );
         }
      } catch (Exception var8) {
         Constants.LOG.warn("Streams Reflowing: could not read or write the world's blacklist pin in {}", var0, var8);
      }

      return !var1;
   }

   public static boolean cutsAtCarve(RiverEngine var0) {
      return var0 != null && !var0.biomeBarAtPlanning();
   }

   public static boolean isBlocked(Holder<Biome> var0) {
      if (var0.is(NO_STREAMS)) {
         return true;
      }

      a();
      List var1 = d;

      for (int var2 = 0; var2 < var1.size(); var2++) {
         if (var0.is((TagKey)var1.get(var2))) {
            return true;
         }
      }

      Set var5 = b;
      Set var3 = c;
      if (var5.isEmpty() && var3.isEmpty()) {
         return false;
      }

      ResourceKey var4 = (ResourceKey)var0.unwrapKey().orElse(null);
      return var4 != null && (var5.contains(var4.location()) || var3.contains(var4.location().getNamespace()));
   }

   private static void a() {
      String var0 = Services.PLATFORM.streamBiomeBlacklist();
      if (var0 == null) {
         var0 = "";
      }

      if (!var0.equals(a)) {
         HashSet var1 = new HashSet();
         HashSet var2 = new HashSet();
         ArrayList var3 = new ArrayList();

         for (String var7 : var0.split(",")) {
            String var8 = var7.trim();
            if (!var8.isEmpty()) {
               if (var8.startsWith("#")) {
                  ResourceLocation var9 = ResourceLocation.tryParse(var8.substring(1));
                  if (var9 != null) {
                     var3.add(TagKey.create(Registries.BIOME, var9));
                  }
               } else if (var8.endsWith(":*")) {
                  var2.add(var8.substring(0, var8.length() - 2));
               } else if (var8.indexOf(58) < 0) {
                  var2.add(var8);
               } else {
                  ResourceLocation var10 = ResourceLocation.tryParse(var8);
                  if (var10 != null) {
                     var1.add(var10);
                  }
               }
            }
         }

         b = var1;
         c = var2;
         d = var3;
         a = var0;
      }
   }
}
