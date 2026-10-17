package dev.streamsreflowing.worldgen.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public record BankFeature(List<ResourceLocation> features, List<ResourceLocation> biomes, List<TagKey<Biome>> tags, List<ResourceLocation> excludeBiomes, List<TagKey<Biome>> excludeTags, int chance) {
   public static final ResourceKey<Registry<BankFeature>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation("streamsreflowing", "bank_feature"));
   public static final Codec<BankFeature> CODEC = RecordCodecBuilder.create(
      var0 -> var0.group(
            ResourceLocation.CODEC.listOf().fieldOf("features").forGetter(BankFeature::features),
            ResourceLocation.CODEC.listOf().optionalFieldOf("biomes", List.of()).forGetter(BankFeature::biomes),
            TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("tags", List.of()).forGetter(BankFeature::tags),
            ResourceLocation.CODEC.listOf().optionalFieldOf("exclude_biomes", List.of()).forGetter(BankFeature::excludeBiomes),
            TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("exclude_tags", List.of()).forGetter(BankFeature::excludeTags),
            Codec.intRange(0, 100).optionalFieldOf("chance", 100).forGetter(BankFeature::chance)
         )
         .apply(var0, BankFeature::new)
   );

   public BankFeature {
biomes = biomes;
excludeBiomes = excludeBiomes;
chance = chance;
}

   public boolean standalone() {
      return !this.biomes.isEmpty() || !this.tags.isEmpty();
   }

   public boolean matches(Holder<Biome> var1, ResourceKey<Biome> var2) {
      if (var2 != null && this.excludeBiomes.contains(var2.location())) {
         return false;
      }

      for (TagKey var4 : this.excludeTags) {
         if (var1.is(var4)) {
            return false;
         }
      }

      if (this.biomes.isEmpty() && this.tags.isEmpty()) {
         return false;
      }

      if (var2 != null && this.biomes.contains(var2.location())) {
         return true;
      }

      if (this.tags.isEmpty()) {
         return false;
      }

      for (TagKey var6 : this.tags) {
         if (!var1.is(var6)) {
            return false;
         }
      }

      return true;
   }
}
