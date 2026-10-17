package dev.streamsreflowing.forge.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class StreamParticles {
   public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "streamsreflowing");
   public static final RegistryObject<SimpleParticleType> STREAM_BUBBLE = PARTICLE_TYPES.register("stream_bubble", () -> new SimpleParticleType(false) {});
   public static final RegistryObject<SimpleParticleType> STREAM_SPLASH = PARTICLE_TYPES.register("stream_splash", () -> new SimpleParticleType(false) {});
   public static final RegistryObject<SimpleParticleType> STREAM_FOAM = PARTICLE_TYPES.register("stream_foam", () -> new SimpleParticleType(false) {});
   public static final RegistryObject<SimpleParticleType> STREAM_LEAF = PARTICLE_TYPES.register("stream_leaf", () -> new SimpleParticleType(false) {});
   public static final RegistryObject<SimpleParticleType> STREAM_STICK = PARTICLE_TYPES.register("stream_stick", () -> new SimpleParticleType(false) {});

   public static void register(IEventBus modEventBus) {
      PARTICLE_TYPES.register(modEventBus);
   }

   private StreamParticles() {
   }
}
