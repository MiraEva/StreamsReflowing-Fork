package dev.streamsreflowing.core.terrain;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class ChunkHeightFill {
   private static final BeardifierOrMarker a = new BeardifierOrMarker() {
      public double compute(FunctionContext var1) {
         return 0.0;
      }

      public void fillArray(double[] var1, ContextProvider var2) {
         Arrays.fill(var1, 0.0);
      }

      public double minValue() {
         return 0.0;
      }

      public double maxValue() {
         return 0.0;
      }
   };
   private static final MethodHandle b = b();

   static BeardifierOrMarker a() {
      return a;
   }

   static Object a(NoiseChunk var0) throws Throwable {
      return b == null ? null : (Object)b.invoke((NoiseChunk)var0);
   }

   private static MethodHandle b() {
      try {
         Method var0 = NoiseChunk.class.getDeclaredMethod("getInterpolatedState");
         var0.setAccessible(true);
         return MethodHandles.lookup().unreflect(var0);
      } catch (Throwable var1) {
         return null;
      }
   }

   private ChunkHeightFill() {
   }

   public static int[] cornerHeights(ServerLevel var0, ChunkPos var1) {
      return null;
   }

   public static int[] heights(ServerLevel var0, ChunkPos var1) throws Throwable {
      if (b == null) {
         return null;
      } else if (!(var0.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator var2)) {
         return null;
      } else {
         RandomState var28 = var0.getChunkSource().randomState();
         NoiseGeneratorSettings var4 = (NoiseGeneratorSettings)var2.generatorSettings().value();
         ProtoChunk var5 = new ProtoChunk(var1, UpgradeData.EMPTY, var0, var0.registryAccess().registryOrThrow(Registries.BIOME), null);
         NoiseSettings var6 = var4.noiseSettings().clampToHeightAccessor(var5);
         int var7 = var6.getCellHeight();
         int var8 = var6.getCellWidth();
         int var9 = var6.height() / var7;
         int var10 = Math.floorDiv(var6.minY(), var7);
         int var11 = 16 / var8;
         int var12 = var4.seaLevel();
         FluidStatus var13 = new FluidStatus(-54, Blocks.LAVA.defaultBlockState());
         FluidStatus var14 = new FluidStatus(var12, var4.defaultFluid());
         FluidPicker var15 = (var3, var4x, var5x) -> var4x < Math.min(-54, var12) ? var13 : var14;
         NoiseChunk var16 = NoiseChunk.forChunk(var5, var28, a, var4, var15, Blender.empty());
         Heightmap var17 = var5.getOrCreateHeightmapUnprimed(Types.OCEAN_FLOOR_WG);
         var16.initializeForFirstCellX();

         for (int var18 = 0; var18 < var11; var18++) {
            var16.advanceCellX(var18);

            for (int var19 = 0; var19 < var11; var19++) {
               for (int var20 = var9 - 1; var20 >= 0; var20--) {
                  var16.selectCellYZ(var20, var19);

                  for (int var21 = var7 - 1; var21 >= 0; var21--) {
                     int var22 = (var10 + var20) * var7 + var21;
                     var16.updateForY(var22, (double)var21 / var7);

                     for (int var23 = 0; var23 < var8; var23++) {
                        int var24 = var18 * var8 + var23;
                        var16.updateForX(var1.getMinBlockX() + var24, (double)var23 / var8);

                        for (int var25 = 0; var25 < var8; var25++) {
                           int var26 = var19 * var8 + var25;
                           var16.updateForZ(var1.getMinBlockZ() + var26, (double)var25 / var8);
                           BlockState var27 = (BlockState)b.invokeExact((NoiseChunk)var16);
                           var17.update(var24, var22, var26, var27 == null ? var4.defaultBlock() : var27);
                        }
                     }
                  }
               }
            }

            var16.swapSlices();
         }

         var16.stopInterpolation();
         int[] var29 = new int[256];

         for (int var30 = 0; var30 < 16; var30++) {
            for (int var31 = 0; var31 < 16; var31++) {
               var29[var30 * 16 + var31] = var17.getFirstAvailable(var31, var30);
            }
         }

         return var29;
      }
   }
}
