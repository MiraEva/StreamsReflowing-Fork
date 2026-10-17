package dev.streamsreflowing.worldgen.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class BankFallenLogFeature extends Feature<BankFallenLogFeature.Config> {
   public BankFallenLogFeature(Codec<BankFallenLogFeature.Config> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<BankFallenLogFeature.Config> var1) {
      WorldGenLevel var2 = var1.level();
      RandomSource var3 = var1.random();
      BankFallenLogFeature.Config var4 = (BankFallenLogFeature.Config)var1.config();
      BlockPos var5 = var1.origin();
      Direction var6 = Plane.HORIZONTAL.getRandomDirection(var3);
      boolean var7 = var4.treeChance() > 0.0F && var3.nextFloat() < var4.treeChance();
      int var8 = var4.length().sample(var3) + (var7 ? 2 : 0);
       ArrayList<BlockPos> var9 = new ArrayList<>(var8);
      int var10 = 0;

      for (int var11 = 0; var11 < var8; var11++) {
         BlockPos var12 = var5.relative(var6, var11);
         BlockState var13 = var2.getBlockState(var12);
         if (!var13.getFluidState().isEmpty() || !var13.isAir() && !var13.canBeReplaced()) {
            break;
         }

         BlockPos var14 = var12.below();
         var10 = var2.getBlockState(var14).isFaceSturdy(var2, var14, Direction.UP) ? 0 : var10 + 1;
         if (var10 > 2) {
            break;
         }

         var9.add(var12);
      }

      if (var9.size() < 2) {
         return false;
      }

      for (BlockPos var17 : var9) {
         this.a(var2, var3, var4, var17, var6.getAxis());
      }

      if (var7) {
         this.b(var2, var3, var4, var9, var6);
      } else {
         this.a(var2, var3, var4, var9, var6);
      }

      if (var4.topper().isPresent() && var4.topperChance() > 0.0F) {
         for (BlockPos var18 : var9) {
            BlockPos var19 = var18.above();
            if (var3.nextFloat() < var4.topperChance() && var2.getBlockState(var19).isAir()) {
               BlockState var20 = a(var4.topper().get(), var2, var3, var19);
               if (var20.canSurvive(var2, var19)) {
                  var2.setBlock(var19, var20, 2);
               }
            }
         }
      }

      return true;
   }

   private void a(WorldGenLevel var1, RandomSource var2, BankFallenLogFeature.Config var3, List<BlockPos> var4, Direction var5) {
      if (!var3.leaves().isEmpty() && !(var3.leavesChance() <= 0.0F)) {
         Direction var6 = var5.getClockWise();

         for (int var7 = 1; var7 < var4.size(); var7++) {
            BlockPos var8 = (BlockPos)var4.get(var7);

            for (BlockPos var12 : new BlockPos[]{var8.above(), var8.relative(var6), var8.relative(var6.getOpposite())}) {
               if (var2.nextFloat() < var3.leavesChance()) {
                  this.a(var1, var2, var3, var12);
               }
            }
         }
      }
   }

   private void b(WorldGenLevel var1, RandomSource var2, BankFallenLogFeature.Config var3, List<BlockPos> var4, Direction var5) {
      BlockPos var6 = (BlockPos)var4.get(0);
      BlockPos var7 = (BlockPos)var4.get(var4.size() - 1);
      Direction var8 = var5.getClockWise();
      Direction var9 = var5.getCounterClockWise();
      int var10 = 1 + var2.nextInt(2);

      for (int var11 = 1; var11 <= var10; var11++) {
         this.a(var1, var2, var3, var6.above(var11), Axis.Y);
      }

      for (Direction var14 : new Direction[]{var8, var9, var5.getOpposite()}) {
         if (var2.nextBoolean()) {
            this.a(var1, var2, var3, var6.relative(var14), var14.getAxis());
         }
      }

      boolean var16 = var2.nextBoolean();

      for (int var17 = 2; var17 < var4.size() - 1; var17++) {
         if (var2.nextInt(3) != 0) {
            Direction var18 = var16 ? var8 : var9;
            var16 = !var16;
            BlockPos var19 = ((BlockPos)var4.get(var17)).relative(var18);
            if (this.a(var1, var2, var3, var19, var18.getAxis())) {
               if (var2.nextBoolean()) {
                  this.a(var1, var2, var3, var19.above(), Axis.Y);
                  this.a(var1, var2, var3, var19.above(), 1);
               } else {
                  this.a(var1, var2, var3, var19, 1);
               }
            }
         }
      }

      this.a(var1, var2, var3, var7.above(), 2);
      this.a(var1, var2, var3, var7.relative(var5), 1);
   }

   private void a(WorldGenLevel var1, RandomSource var2, BankFallenLogFeature.Config var3, BlockPos var4, int var5) {
      if (!var3.leaves().isEmpty()) {
         MutableBlockPos var6 = new MutableBlockPos();

         for (int var7 = -var5; var7 <= var5; var7++) {
            for (int var8 = -var5; var8 <= var5; var8++) {
               for (int var9 = -var5; var9 <= var5; var9++) {
                  if (Math.abs(var7) + Math.abs(var8) + Math.abs(var9) <= var5 + 1
                     && (var2.nextInt(4) != 0 || var7 == 0 && var8 == 0 && var9 == 0 && var5 > 0)
                     && (var7 != 0 || var8 != 0 || var9 != 0)) {
                     this.a(var1, var2, var3, var6.set(var4.getX() + var7, var4.getY() + var8, var4.getZ() + var9));
                  }
               }
            }
         }
      }
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, BankFallenLogFeature.Config var3, BlockPos var4, Axis var5) {
      BlockState var6 = var1.getBlockState(var4);
      if (var6.getFluidState().isEmpty() && (var6.isAir() || var6.canBeReplaced())) {
         BlockState var7 = a(var3.log(), var1, var2, var4);
         if (var7.hasProperty(BlockStateProperties.AXIS)) {
            var7 = (BlockState)var7.setValue(BlockStateProperties.AXIS, var5);
         }

         var1.setBlock(var4, var7, 2);
         return true;
      } else {
         return false;
      }
   }

   private void a(WorldGenLevel var1, RandomSource var2, BankFallenLogFeature.Config var3, BlockPos var4) {
      if (!var3.leaves().isEmpty() && var1.getBlockState(var4).isAir()) {
         BlockState var5 = a(var3.leaves().get(), var1, var2, var4);
         if (var5.hasProperty(BlockStateProperties.PERSISTENT)) {
            var5 = (BlockState)var5.setValue(BlockStateProperties.PERSISTENT, true);
         }

         var1.setBlock(var4, var5, 2);
      }
   }

   private static BlockState a(BlockStateProvider var0, WorldGenLevel var1, RandomSource var2, BlockPos var3) {
      return var0.getState(var2, var3);
   }

   public record Config(BlockStateProvider log, IntProvider length, float treeChance, Optional<BlockStateProvider> leaves, float leavesChance, Optional<BlockStateProvider> topper, float topperChance) implements FeatureConfiguration {
      private static final Codec<IntProvider> LENGTH_CODEC = IntProvider.codec(2, 26);
      public static final Codec<BankFallenLogFeature.Config> CODEC = RecordCodecBuilder.create(
         var0 -> var0.group(
               BlockStateProvider.CODEC.fieldOf("log").forGetter(BankFallenLogFeature.Config::log),
               LENGTH_CODEC.optionalFieldOf("length", UniformInt.of(3, 5)).forGetter(BankFallenLogFeature.Config::length),
               Codec.floatRange(0.0F, 1.0F).optionalFieldOf("tree_chance", 0.0F).forGetter(BankFallenLogFeature.Config::treeChance),
               BlockStateProvider.CODEC.optionalFieldOf("leaves").forGetter(BankFallenLogFeature.Config::leaves),
               Codec.floatRange(0.0F, 1.0F).optionalFieldOf("leaves_chance", 0.0F).forGetter(BankFallenLogFeature.Config::leavesChance),
               BlockStateProvider.CODEC.optionalFieldOf("topper").forGetter(BankFallenLogFeature.Config::topper),
               Codec.floatRange(0.0F, 1.0F).optionalFieldOf("topper_chance", 0.0F).forGetter(BankFallenLogFeature.Config::topperChance)
            )
            .apply(var0, BankFallenLogFeature.Config::new)
      );

      public Config {
length = length;
leaves = leaves;
topper = topper;
}
   }
}
