package dev.streamsreflowing.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.streamsreflowing.worldgen.OverworldBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.BiomeModifier.Phase;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries.Keys;
import net.minecraftforge.server.ServerLifecycleHooks;

public record OverworldFeaturesModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features, Decoration step) implements BiomeModifier {
   private static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS = DeferredRegister.create(
      Keys.BIOME_MODIFIER_SERIALIZERS, "streamsreflowing"
   );
   public static final RegistryObject<Codec<OverworldFeaturesModifier>> CODEC = SERIALIZERS.register(
      "add_features_overworld",
      () -> RecordCodecBuilder.create(
         b -> b.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(OverworldFeaturesModifier::biomes),
               PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(OverworldFeaturesModifier::features),
               Decoration.CODEC.fieldOf("step").forGetter(OverworldFeaturesModifier::step)
            )
            .apply(b, OverworldFeaturesModifier::new)
      )
   );

   public static void register(IEventBus modEventBus) {
      SERIALIZERS.register(modEventBus);
   }

   public void modify(Holder<Biome> biome, Phase phase, Builder builder) {
      if (phase == Phase.ADD && (this.biomes.contains(biome) || OverworldBiomes.contains(ServerLifecycleHooks.getCurrentServer(), biome))) {
         BiomeGenerationSettingsBuilder settings = builder.getGenerationSettings();
         this.features.forEach(f -> settings.addFeature(this.step, f));
      }
   }

   public Codec<? extends BiomeModifier> codec() {
      return (Codec<? extends BiomeModifier>)CODEC.get();
   }
}
