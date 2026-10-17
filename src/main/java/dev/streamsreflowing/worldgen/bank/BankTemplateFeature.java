package dev.streamsreflowing.worldgen.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BankTemplateFeature extends Feature<BankTemplateFeature.Config> {
   public static final ThreadLocal<String> PLACING_BRANCH = ThreadLocal.withInitial(() -> "?");
   public static final ConcurrentLinkedDeque<BankTemplateFeature.PlacedRecord> JOURNAL = new ConcurrentLinkedDeque<>();
   private static final int a = 2048;

   public BankTemplateFeature(Codec<BankTemplateFeature.Config> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<BankTemplateFeature.Config> var1) {
      WorldGenLevel var2 = var1.level();
      RandomSource var3 = var1.random();
      BankTemplateFeature.Config var4 = (BankTemplateFeature.Config)var1.config();
      BlockPos var5 = var1.origin();
      if (var4.blocks().isEmpty()) {
         return false;
      }

      StructurePlaceSettings var6 = new StructurePlaceSettings()
         .setRotation(var4.rotate() ? Rotation.getRandom(var3) : Rotation.NONE)
         .setMirror(var4.mirror() && var3.nextBoolean() ? Mirror.FRONT_BACK : Mirror.NONE);
       Map<ResourceLocation, ResourceLocation> var7 = var4.variants().isEmpty() ? Map.of() : (Map<ResourceLocation, ResourceLocation>)var4.variants().get(var3.nextInt(var4.variants().size()));
      MutableBlockPos var8 = new MutableBlockPos();
       ArrayList<BlockPos> var9 = new ArrayList<>(var4.blocks().size());
       ArrayList<BlockState> var10 = new ArrayList<>(var4.blocks().size());
      int var11 = 0;
      int var12 = 0;

      for (BankTemplateFeature.Placed var14 : var4.blocks()) {
         BlockPos var15 = var5.offset(StructureTemplate.calculateRelativePosition(var6, var14.offset()));
         BlockState var16 = var2.getBlockState(var15);
         boolean var17 = !var16.getFluidState().isEmpty();
         if (var14.offset().getY() >= 0) {
            var12++;
         }

         if (var17 ? !var4.placeInWater() : !var16.isAir() && !var16.canBeReplaced()) {
            if (var14.offset().getY() >= 0) {
               var11++;
            }
         } else {
            var9.add(var15.immutable());
            var10.add(a(var14.state(), var7, var6));
         }
      }

      if (!var9.isEmpty() && var11 * 2 <= var12) {
         int var18 = var11;
         int var19 = var9.stream().mapToInt(Vec3i::getY).min().orElse(0);
         boolean var20 = false;

         for (BlockPos var23 : var9) {
            if (var23.getY() == var19) {
               var8.set(var23.getX(), var23.getY() - 1, var23.getZ());
               if (var2.getBlockState(var8).isFaceSturdy(var2, var8, Direction.UP)) {
                  var20 = true;
                  break;
               }
            }
         }

         if (!var20) {
            return false;
         }

         for (int var22 = 0; var22 < var9.size(); var22++) {
            var2.setBlock((BlockPos)var9.get(var22), (BlockState)var10.get(var22), 2);
         }

         a(var5, var9, var10, var18);
         return true;
      } else {
         return false;
      }
   }

   private static void a(BlockPos var0, List<BlockPos> var1, List<BlockState> var2, int var3) {
      JOURNAL.addLast(new BankTemplateFeature.PlacedRecord(var0, List.copyOf(var1), List.copyOf(var2), var3, PLACING_BRANCH.get()));

      while (JOURNAL.size() > 2048) {
         JOURNAL.pollFirst();
      }
   }

   public static double fitFractionAt(WorldGenLevel var0, BlockPos var1, BankTemplateFeature.Config var2) {
      int var3 = 0;
      int var4 = 0;
      MutableBlockPos var5 = new MutableBlockPos();

      for (BankTemplateFeature.Placed var7 : var2.blocks()) {
         if (var7.offset().getY() >= 0) {
            var4++;
            var5.set(
               var1.getX() + var7.offset().getX(), var1.getY() + var7.offset().getY(), var1.getZ() + var7.offset().getZ()
            );
            BlockState var8 = var0.getBlockState(var5);
            boolean var9 = !var8.getFluidState().isEmpty();
            if (var9 ? var2.placeInWater() : var8.isAir() || var8.canBeReplaced()) {
               var3++;
            }
         }
      }

      return var4 == 0 ? 0.0 : (double)var3 / var4;
   }

   private static BlockState a(BlockState var0, Map<ResourceLocation, ResourceLocation> var1, StructurePlaceSettings var2) {
      ResourceLocation var3 = (ResourceLocation)var1.get(BuiltInRegistries.BLOCK.getKey(var0.getBlock()));
      if (var3 != null) {
         Block var4 = (Block)BuiltInRegistries.BLOCK.getOptional(var3).orElse(null);
         if (var4 != null) {
            BlockState var5 = var4.defaultBlockState();

            for (Property var7 : var0.getProperties()) {
               if (var5.hasProperty(var7)) {
                  var5 = a(var5, var0, var7);
               }
            }

            var0 = var5;
         }
      }

      if (var0.hasProperty(BlockStateProperties.PERSISTENT)) {
         var0 = (BlockState)var0.setValue(BlockStateProperties.PERSISTENT, true);
      }

      return var0.mirror(var2.getMirror()).rotate(var2.getRotation());
   }

   private static <T extends Comparable<T>> BlockState a(BlockState var0, BlockState var1, Property<T> var2) {
      return (BlockState)var0.setValue(var2, var1.getValue(var2));
   }

   public record Config(List<BankTemplateFeature.Placed> blocks, boolean rotate, boolean mirror, List<Map<ResourceLocation, ResourceLocation>> variants, boolean placeInWater) implements FeatureConfiguration {
      public static final Codec<BankTemplateFeature.Config> CODEC = RecordCodecBuilder.create(
         var0 -> var0.group(
               BankTemplateFeature.Placed.CODEC.listOf().fieldOf("blocks").forGetter(BankTemplateFeature.Config::blocks),
               Codec.BOOL.optionalFieldOf("rotate", true).forGetter(BankTemplateFeature.Config::rotate),
               Codec.BOOL.optionalFieldOf("mirror", true).forGetter(BankTemplateFeature.Config::mirror),
               Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
                  .listOf()
                  .optionalFieldOf("variants", List.of())
                  .forGetter(BankTemplateFeature.Config::variants),
               Codec.BOOL.optionalFieldOf("place_in_water", false).forGetter(BankTemplateFeature.Config::placeInWater)
            )
            .apply(var0, BankTemplateFeature.Config::new)
      );

      public Config(List<BankTemplateFeature.Placed> var1, boolean var2, boolean var3, List<Map<ResourceLocation, ResourceLocation>> var4) {
         this(var1, var2, var3, var4, false);
      }

      public Config {
rotate = rotate;
variants = variants;
}
   }

   public record Placed(BlockPos offset, BlockState state) {
      static final Codec<BankTemplateFeature.Placed> CODEC = RecordCodecBuilder.create(
         var0 -> var0.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(BankTemplateFeature.Placed::offset),
               BlockState.CODEC.fieldOf("state").forGetter(BankTemplateFeature.Placed::state)
            )
            .apply(var0, BankTemplateFeature.Placed::new)
      );

      public Placed {
state = state;
}
   }

   public record PlacedRecord(BlockPos origin, List<BlockPos> cells, List<BlockState> states, int cutCells, String branch) {

      public PlacedRecord {
cells = cells;
cutCells = cutCells;
}
   }
}
