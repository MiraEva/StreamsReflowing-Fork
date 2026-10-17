package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.platform.Services;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class StreamRiverFlowFeature extends Feature<NoneFeatureConfiguration> {
   public StreamRiverFlowFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.flowVanillaRivers()) {
         return false;
      }

      if (Services.PLATFORM.streamWaterState(0).is(Blocks.WATER)) {
         return false;
      }

      WorldGenLevel var2 = var1.level();
      RiverFlowField.BiomeClass var3 = RiverFlowFields.classifierFor(var2.getLevel());
      RiverFlowField.OrientationField var4 = RiverFlowFields.orientationFor(var2.getLevel());
      ChunkPos var5 = new ChunkPos(var1.origin());
      int var6 = var5.getMinBlockX();
      int var7 = var5.getMinBlockZ();
      int var8 = RiverFlowFields.regionSize();
      MutableBlockPos var9 = new MutableBlockPos();
      boolean var10 = false;

      for (int var11 = 0; var11 < 16; var11++) {
         for (int var12 = 0; var12 < 16; var12++) {
            int var13 = var6 + var11;
            int var14 = var7 + var12;
            int var15 = var2.getHeight(Types.WORLD_SURFACE_WG, var13, var14) - 1;
            int var16 = var2.getHeight(Types.OCEAN_FLOOR_WG, var13, var14) - 1;
            if (var15 > var16) {
               var9.set(var13, var15, var14);
               if (!var2.getBiome(var9).is(BiomeTags.IS_OCEAN)) {
                  int var17 = RiverFlowFields.dirAt(var3, var4, var13, var14);
                  if (var17 >= 0) {
                     BlockState var18 = Services.PLATFORM.streamWaterState(var17);

                     for (int var19 = var15; var19 > var16; var19--) {
                        var9.set(var13, var19, var14);
                        if (var2.getBlockState(var9).is(Blocks.WATER)) {
                           var2.setBlock(var9, var18, 2);
                           var10 = true;
                        }
                     }
                  }
               }
            }
         }
      }

      return var10;
   }
}
