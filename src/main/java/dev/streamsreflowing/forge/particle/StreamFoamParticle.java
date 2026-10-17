package dev.streamsreflowing.forge.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class StreamFoamParticle extends CachedLightParticle {
   private float baseAlpha = 0.4F;

   protected StreamFoamParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
      super(level, x, y, z);
      this.xd = vx;
      this.yd = vy;
      this.zd = vz;
      this.gravity = 0.0F;
      this.friction = 0.92F;
      this.hasPhysics = false;
      this.lifetime = 24 + this.random.nextInt(16);
      this.quadSize = 0.9F + this.random.nextFloat() * 0.6F;
      this.rCol = 1.0F;
      this.gCol = 1.0F;
      this.bCol = 1.0F;
      this.alpha = this.baseAlpha;
   }

   public StreamFoamParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float scale, boolean solid, SpriteSet sprites) {
      this(level, x, y, z, vx, vy, vz);
      this.pickSprite(sprites);
      this.quadSize *= scale;
      this.baseAlpha = solid ? 0.85F : 0.4F;
      this.alpha = this.baseAlpha;
   }

   public void tick() {
      super.tick();
      int fadeStart = this.lifetime * 2 / 3;
      if (this.age > fadeStart) {
         this.alpha = this.baseAlpha * Math.max(0.0F, 1.0F - (float)(this.age - fadeStart) / (this.lifetime - fadeStart));
      }
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
         StreamFoamParticle particle = new StreamFoamParticle(level, x, y, z, vx, vy, vz);
         particle.pickSprite(this.sprites);
         return particle;
      }
   }
}
