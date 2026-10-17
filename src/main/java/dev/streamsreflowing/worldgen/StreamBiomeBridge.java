package dev.streamsreflowing.worldgen;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

public final class StreamBiomeBridge {
   public static volatile Biome ours;
   public static volatile MobSpawnSettings riverSpawns;
   public static volatile StreamBiomeBridge.LandProbe landProbe;
   public static final Set<Biome> stamped = ConcurrentHashMap.newKeySet();
   public static volatile Object bridgedAttributes;

   public static StreamBiomeBridge.LandProbe memoised(StreamBiomeBridge.LandProbe var0) {
      ThreadLocal var1 = ThreadLocal.withInitial(StreamBiomeBridge.QuartMemo::new);
      return (var2, var3, var4) -> ((StreamBiomeBridge.QuartMemo)var1.get()).a(var0, var2, var3, var4);
   }

   public static void noteStamp(Holder<Biome> var0) {
      if (var0 != null && var0.unwrapKey().map(var0x -> "streamsreflowing".equals(var0x.location().getNamespace())).orElse(false)) {
         stamped.add((Biome)var0.value());
      }
   }

   public static boolean isStamp(Object var0) {
      return var0 != null && (var0 == ours || stamped.contains(var0));
   }

   private StreamBiomeBridge() {
   }

   public static void set(Biome var0, MobSpawnSettings var1) {
      if (ours != var0) {
         ours = var0;
         riverSpawns = var1;
      }
   }

   public static void clear() {
      ours = null;
      bridgedAttributes = null;
      riverSpawns = null;
      landProbe = null;
      stamped.clear();
   }

   public interface LandProbe {
      Holder<Biome> at(int var1, int var2, int var3);
   }

   private static final class QuartMemo {
      private static final int a = 1024;
      private final long[] b = new long[1024];
      private final Holder<Biome>[] c = new Holder[1024];

      QuartMemo() {
         Arrays.fill(this.b, Long.MIN_VALUE);
      }

      Holder<Biome> a(StreamBiomeBridge.LandProbe var1, int var2, int var3, int var4) {
         long var5 = var2 >> 2;
         long var7 = var3 >> 2;
         long var9 = var4 >> 2;
         long var11 = (var5 & 4194303L) << 42 | (var7 & 1048575L) << 22 | var9 & 4194303L;
         long var13 = var11 * -7046029254386353131L;
         int var15 = (int)(var13 ^ var13 >>> 29) & 1023;
         if (this.b[var15] == var11) {
            return this.c[var15];
         }

         Holder var16 = var1.at(var2, var3, var4);
         this.b[var15] = var11;
         this.c[var15] = var16;
         return var16;
      }
   }
}
