package dev.streamsreflowing.worldgen.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class BankRockFeature extends Feature<BankRockFeature.Config> {
   public BankRockFeature(Codec<BankRockFeature.Config> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<BankRockFeature.Config> var1) {
      WorldGenLevel var2 = var1.level();
      RandomSource var3 = var1.random();
      BankRockFeature.Config var4 = (BankRockFeature.Config)var1.config();
      BlockPos var5 = var1.origin();
       HashMap<Long, Integer> var6 = new HashMap<>();
      boolean var7 = "spike".equals(var4.style()) ? this.b(var2, var3, var4, var5, var6) : this.a(var2, var3, var4, var5, var6);
      if (var7 && var4.topper().isPresent() && var4.topperChance() > 0.0F) {
         MutableBlockPos var8 = new MutableBlockPos();

          for (Entry<Long, Integer> var10 : var6.entrySet()) {
             if (!(var3.nextFloat() >= var4.topperChance())) {
                var8.set(BlockPos.getX(var10.getKey()), var10.getValue() + 1, BlockPos.getZ(var10.getKey()));
               if (var2.getBlockState(var8).isAir()) {
                  BlockState var11 = a(var4.topper().get(), var2, var3, var8);
                  if (var11.canSurvive(var2, var8)) {
                     var2.setBlock(var8, var11, 2);
                  }
               }
            }
         }
      }

      return var7;
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, BankRockFeature.Config var3, BlockPos var4, Map<Long, Integer> var5) {
      int var6 = Math.max(1, var3.size().sample(var2));
      BlockPos var7 = var4.below();
      MutableBlockPos var8 = new MutableBlockPos();
      boolean var9 = false;

      for (int var10 = -var6; var10 <= var6; var10++) {
         for (int var11 = -var6; var11 <= var6; var11++) {
            int var12 = var2.nextInt(2);

            for (int var13 = -1; var13 <= var6; var13++) {
               if (var10 * var10 + var11 * var11 + var13 * var13 * 2 <= var6 * var6 + var12) {
                  var8.set(var7.getX() + var10, var7.getY() + var13, var7.getZ() + var11);
                  if (this.a(var1, var2, var3, var8, var13 < 0)) {
                     var9 = true;
                     var5.merge(BlockPos.asLong(var8.getX(), 0, var8.getZ()), var8.getY(), Math::max);
                  }
               }
            }
         }
      }

      return var9;
   }

   private boolean b(WorldGenLevel var1, RandomSource var2, BankRockFeature.Config var3, BlockPos var4, Map<Long, Integer> var5) {
      int var6 = Math.max(2, var3.size().sample(var2));
      boolean var7 = this.a(var1, var2, var3, var4.below(), var6 + 1, var5);
      if (var6 >= 3) {
         Direction var8 = Plane.HORIZONTAL.getRandomDirection(var2);
         var7 |= this.a(var1, var2, var3, var4.below().relative(var8), Math.max(2, var6 / 2) + 1, var5);
         if (var6 >= 5) {
            Direction var9 = var2.nextBoolean() ? var8.getClockWise() : var8.getOpposite();
            var7 |= this.a(var1, var2, var3, var4.below().relative(var9), Math.max(2, var6 / 3) + 1, var5);
         }
      }

      return var7;
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, BankRockFeature.Config var3, BlockPos var4, int var5, Map<Long, Integer> var6) {
      MutableBlockPos var7 = new MutableBlockPos();
      boolean var8 = false;

      for (int var9 = 0; var9 < var5; var9++) {
         var7.set(var4.getX(), var4.getY() + var9, var4.getZ());
         if (!this.a(var1, var2, var3, var7, var9 == 0)) {
            if (var9 <= 0) {
               return false;
            }
            break;
         }

         var8 = true;
         var6.merge(BlockPos.asLong(var7.getX(), 0, var7.getZ()), var7.getY(), Math::max);
      }

      return var8;
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, BankRockFeature.Config var3, MutableBlockPos var4, boolean var5) {
      BlockState var6 = var1.getBlockState(var4);
      if (!var6.getFluidState().isEmpty()) {
         return false;
      }

      if (!var5 && !var6.isAir() && !var6.canBeReplaced()) {
         return false;
      }

      var1.setBlock(var4, a(var3.block(), var1, var2, var4), 2);
      return true;
   }

   private static BlockState a(BlockStateProvider var0, WorldGenLevel var1, RandomSource var2, BlockPos var3) {
      return var0.getState(var2, var3);
   }

   public record Config(BlockStateProvider block, String style, IntProvider size, Optional<BlockStateProvider> topper, float topperChance) implements FeatureConfiguration {
      private static final Codec<IntProvider> SIZE_CODEC = IntProvider.codec(1, 8);
      public static final Codec<BankRockFeature.Config> CODEC = RecordCodecBuilder.create(
         var0 -> var0.group(
               BlockStateProvider.CODEC.fieldOf("block").forGetter(BankRockFeature.Config::block),
               Codec.STRING.optionalFieldOf("style", "boulder").forGetter(BankRockFeature.Config::style),
               SIZE_CODEC.optionalFieldOf("size", ConstantInt.of(2)).forGetter(BankRockFeature.Config::size),
               BlockStateProvider.CODEC.optionalFieldOf("topper").forGetter(BankRockFeature.Config::topper),
               Codec.floatRange(0.0F, 1.0F).optionalFieldOf("topper_chance", 0.0F).forGetter(BankRockFeature.Config::topperChance)
            )
            .apply(var0, BankRockFeature.Config::new)
      );

      public Config {
style = style;
topper = topper;
}
   }
}
