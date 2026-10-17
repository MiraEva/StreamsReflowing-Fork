package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.platform.Services;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WaterSkinFeature extends Feature<NoneFeatureConfiguration> {
   public WaterSkinFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      }

      ChunkPos var2 = new ChunkPos(var1.origin());
      RiverCarverFeature.skinAllWater(var1.level(), var2.getMinBlockX(), var2.getMinBlockZ(), Services.PLATFORM.allWaterBanks());
      return true;
   }
}
