package dev.streamsreflowing.worldgen;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

public final class QuartBiomeMemo {
   private final Map<Long, Holder<Biome>> a = new HashMap<>();

   public Holder<Biome> get(WorldGenLevel var1, int var2, int var3, int var4) {
      int var5 = var2 >> 2;
      int var6 = var3 >> 2;
      int var7 = var4 >> 2;
      long var8 = (long)var6 << 48 ^ (var5 & 16777215L) << 24 ^ var7 & 16777215L;
      Holder var10 = this.a.get(var8);
      if (var10 == null) {
         var10 = var1.getUncachedNoiseBiome(var5, var6, var7);
         this.a.put(var8, var10);
      }

      return var10;
   }
}
