package dev.streamsreflowing.forge.particle;

import dev.streamsreflowing.config.StreamsSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;

public class StreamBubbleParticle extends CachedLightParticle {
   private static final double FALL_ACCEL = 0.045;
   private final boolean pathed;
   private final MutableBlockPos scan = new MutableBlockPos();

   protected StreamBubbleParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
      super(level, x, y, z);
      this.pathed = StreamsSettings.streamBubbles() >= 2;
      if (this.pathed) {
         this.xd = vx;
         this.zd = vz;
      } else {
         this.xd = 0.0;
         this.zd = 0.0;
      }

      this.yd = 0.0;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.hasPhysics = false;
      this.lifetime = 24 + this.random.nextInt(24);
      this.quadSize = this.quadSize * (0.5F + this.random.nextFloat() * 0.5F);
      this.rCol = 0.82F;
      this.gCol = 0.9F;
      this.bCol = 1.0F;
      this.alpha = 0.75F;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         if (this.pathed) {
            double surfaceY = this.waterSurfaceNear(this.x, this.y, this.z);
            double ydelta;
            if (!Double.isNaN(surfaceY) && !(this.y > surfaceY + 0.08)) {
               ydelta = surfaceY + 0.06 - this.y;
               this.yd = 0.0;
            } else {
               this.yd -= 0.045;
               ydelta = this.yd;
            }

            this.move(this.xd, ydelta, this.zd);
            this.xd *= 0.99;
            this.zd *= 0.99;
         }

         int fadeStart = this.lifetime * 2 / 3;
         if (this.age > fadeStart) {
            this.alpha = 0.75F * Math.max(0.0F, 1.0F - (float)(this.age - fadeStart) / (this.lifetime - fadeStart));
         }
      }
   }

   private double waterSurfaceNear(double x, double y, double z) {
      int bx = Mth.floor(x);
      int bz = Mth.floor(z);
      int topY = Mth.floor(y + 0.1);

      for (int dy = 0; dy <= 3; dy++) {
         int by = topY - dy;
         this.scan.set(bx, by, bz);
         FluidState fs = this.level.getFluidState(this.scan);
         if (!fs.isEmpty()) {
            return by + fs.getHeight(this.level, this.scan);
         }
      }

      return Double.NaN;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
         StreamBubbleParticle particle = new StreamBubbleParticle(level, x, y, z, vx, vy, vz);
         particle.pickSprite(this.sprites);
         return particle;
      }
   }
}
