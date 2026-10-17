package dev.streamsreflowing.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;

public final class FoamSize {
   private static final float SMALLEST = 0.75F;
   private static final float GROWTH_PER_BLOCK = 0.12F;
   private static final int GROWTH_BLOCKS = 8;

   private FoamSize() {
   }

   public static float of(float roll, int fallHeight) {
      int past = Math.max(0, Math.min(8, fallHeight - 1));
      return 0.75F * (1.0F + roll * roll * 0.12F * past);
   }

   public static void place(Level level, ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz, float size, boolean far) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.level != level) {
            return;
         }

         int video = ((ParticleStatus)mc.options.particles().get()).ordinal();
         if (video >= 2 || video == 1 && level.getRandom().nextInt(3) == 0) {
            return;
         }

         if (!far && mc.player.distanceToSqr(x, y, z) > 1024.0) {
            return;
         }

         Particle made = mc.particleEngine.createParticle(particle, x, y, z, vx, vy, vz);
         if (made != null) {
            made.scale(size);
         }
      } catch (RuntimeException e) {
         level.addParticle(particle, x, y, z, vx, vy, vz);
      }
   }
}
