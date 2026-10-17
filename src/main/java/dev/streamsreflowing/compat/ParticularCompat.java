package dev.streamsreflowing.compat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ParticularCompat {
   private static final ResourceLocation CASCADE_ID = new ResourceLocation("particular", "cascade");
   private static boolean resolved = false;
   private static ParticleOptions cascade = null;

   private ParticularCompat() {
   }

   public static ParticleOptions cascade() {
      if (!resolved) {
         ParticleType<?> type = (ParticleType<?>)BuiltInRegistries.PARTICLE_TYPE.get(CASCADE_ID);
         cascade = type instanceof ParticleOptions particle ? particle : null;
         resolved = true;
      }

      return cascade;
   }
}
