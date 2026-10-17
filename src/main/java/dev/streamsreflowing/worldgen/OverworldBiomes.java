package dev.streamsreflowing.worldgen;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;

public final class OverworldBiomes {
   private static WeakReference<MinecraftServer> a = new WeakReference<>(null);
   private static Set<ResourceKey<Biome>> b = Set.of();

   private OverworldBiomes() {
   }

   public static synchronized boolean contains(MinecraftServer var0, Holder<Biome> var1) {
      if (var0 == null) {
         return false;
      }

      if (a.get() != var0) {
         b = a(var0);
         a = new WeakReference<>(var0);
      }

      return var1.unwrapKey().map(b::contains).orElse(false);
   }

   private static Set<ResourceKey<Biome>> a(MinecraftServer var0) {
      HashSet var1 = new HashSet();
      var0.registryAccess()
         .lookupOrThrow(Registries.LEVEL_STEM)
         .get(LevelStem.OVERWORLD)
         .ifPresent(var1x -> ((LevelStem)var1x.value()).generator().getBiomeSource().possibleBiomes().forEach(var1xx -> var1xx.unwrapKey().ifPresent(var1::add)));
      return var1;
   }
}
