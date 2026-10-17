package dev.streamsreflowing.forge.particle;

import dev.streamsreflowing.client.ParticleLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;

public abstract class CachedLightParticle extends TextureSheetParticle {
   private long litKey = Long.MIN_VALUE;
   private int litAge = -1024;
   private int litVal;

   protected CachedLightParticle(ClientLevel level, double x, double y, double z) {
      super(level, x, y, z);
   }

   protected int getLightColor(float partialTick) {
      if (!ParticleLight.ENABLED || this.stale()) {
         this.litVal = super.getLightColor(partialTick);
      }

      return this.litVal;
   }

   private boolean stale() {
      long key = ParticleLight.key(this.x, this.y, this.z);
      if (key == this.litKey && this.age - this.litAge < 8) {
         return false;
      }

      this.litKey = key;
      this.litAge = this.age;
      return true;
   }
}
