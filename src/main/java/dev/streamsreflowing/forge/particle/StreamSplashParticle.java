package dev.streamsreflowing.forge.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public class StreamSplashParticle extends CachedLightParticle {
   protected StreamSplashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
      super(level, x, y, z);
      this.xd = vx;
      this.yd = vy;
      this.zd = vz;
      this.gravity = 0.4F;
      this.friction = 0.9F;
      this.hasPhysics = false;
      this.lifetime = 14 + this.random.nextInt(10);
      this.quadSize = 0.45F + this.random.nextFloat() * 0.35F;
      int wc = BiomeColors.getAverageWaterColor(level, BlockPos.containing(x, y, z));
      this.rCol = 0.82F + 0.18F * ((wc >> 16 & 0xFF) / 255.0F);
      this.gCol = 0.82F + 0.18F * ((wc >> 8 & 0xFF) / 255.0F);
      this.bCol = 0.82F + 0.18F * ((wc & 0xFF) / 255.0F);
      this.alpha = 0.92F;
   }

   public StreamSplashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float scale, SpriteSet sprites) {
      this(level, x, y, z, vx, vy, vz);
      this.pickSprite(sprites);
      this.quadSize *= scale;
   }

   public StreamSplashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float scale, boolean poof, SpriteSet sprites) {
      this(level, x, y, z, vx, vy, vz, scale, sprites);
      if (poof) {
         this.gravity = 0.03F;
         this.friction = 0.96F;
         this.lifetime = 28 + this.random.nextInt(20);
         this.quadSize *= 1.7F;
         this.rCol = 1.0F;
         this.gCol = 1.0F;
         this.bCol = 1.0F;
      }
   }

   public void tick() {
      super.tick();
      int fadeStart = this.lifetime * 2 / 3;
      if (this.age > fadeStart) {
         this.alpha = 0.92F * Math.max(0.0F, 1.0F - (float)(this.age - fadeStart) / (this.lifetime - fadeStart));
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
         StreamSplashParticle particle = new StreamSplashParticle(level, x, y, z, vx, vy, vz);
         particle.pickSprite(this.sprites);
         return particle;
      }
   }
}
