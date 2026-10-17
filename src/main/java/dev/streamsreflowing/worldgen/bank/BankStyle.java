package dev.streamsreflowing.worldgen.bank;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record BankStyle(List<ResourceLocation> biomes, List<TagKey<Biome>> tags, List<ResourceLocation> excludeBiomes, List<TagKey<Biome>> excludeTags, boolean star, boolean enabled, float cohesion, List<BankRing> rings) {
   public static final ResourceKey<Registry<BankStyle>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation("streamsreflowing", "bank_style"));
   public static final ResourceLocation CHAMELEON_ID = new ResourceLocation("streamsreflowing", "chameleon");
   public static final BlockState CHAMELEON = Blocks.STRUCTURE_VOID.defaultBlockState();
   private static volatile boolean anyStyleUsesBend;
   static final Codec<BlockState> BLOCK = ResourceLocation.CODEC.comapFlatMap(var0 -> {
      if (CHAMELEON_ID.equals(var0)) {
         return DataResult.success(CHAMELEON);
      }

      Block var1 = (Block)BuiltInRegistries.BLOCK.getOptional(var0).orElse(null);
      return var1 == null ? DataResult.error(() -> "Unknown block: " + var0) : DataResult.success(var1.defaultBlockState());
   }, var0 -> isChameleon(var0) ? CHAMELEON_ID : BuiltInRegistries.BLOCK.getKey(var0.getBlock()));
   static final Codec<List<BlockState>> BLOCK_LIST = Codec.either(BLOCK, BLOCK.listOf())
      .xmap(
         var0 -> (List)var0.map(var0x -> List.of(var0x), var0x -> var0x), var0 -> var0.size() == 1 ? Either.left((BlockState)var0.get(0)) : Either.right(var0)
      );
   private static final MapCodec<BankStyle.Main> MAIN_CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("biomes", List.of()).forGetter(BankStyle.Main::biomes),
            TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("tags", List.of()).forGetter(BankStyle.Main::tags),
            ResourceLocation.CODEC.listOf().optionalFieldOf("exclude_biomes", List.of()).forGetter(BankStyle.Main::excludeBiomes),
            TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("exclude_tags", List.of()).forGetter(BankStyle.Main::excludeTags),
            Codec.BOOL.optionalFieldOf("star", false).forGetter(BankStyle.Main::star),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(BankStyle.Main::enabled),
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("cohesion", -1.0F).forGetter(BankStyle.Main::cohesion),
            BankRing.SPEC_CODEC.optionalFieldOf("bed").forGetter(BankStyle.Main::bed),
            BankRing.SPEC_CODEC.optionalFieldOf("waterline").forGetter(BankStyle.Main::waterline),
            BankRing.SPEC_CODEC.optionalFieldOf("bank").forGetter(BankStyle.Main::bank),
            BankRing.SPEC_CODEC.optionalFieldOf("top_bank").forGetter(BankStyle.Main::topBank),
            BankRing.SPEC_CODEC.listOf().optionalFieldOf("rings", List.of()).forGetter(BankStyle.Main::rings)
         )
         .apply(var0, BankStyle.Main::new)
   );
   private static final MapCodec<BankStyle.Legacy> LEGACY_CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            Codec.BOOL.optionalFieldOf("bed_enabled").forGetter(BankStyle.Legacy::bedEnabled),
            Codec.BOOL.optionalFieldOf("waterline_enabled").forGetter(BankStyle.Legacy::waterlineEnabled),
            Codec.BOOL.optionalFieldOf("bank_enabled").forGetter(BankStyle.Legacy::bankEnabled),
            Codec.INT.optionalFieldOf("waterline_below").forGetter(BankStyle.Legacy::waterlineBelow),
            Codec.INT.optionalFieldOf("waterline_above").forGetter(BankStyle.Legacy::waterlineAbove),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("underwater_noise").forGetter(BankStyle.Legacy::underwaterNoise),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("above_water_noise").forGetter(BankStyle.Legacy::aboveWaterNoise),
            BLOCK_LIST.optionalFieldOf("point_bar", List.of()).forGetter(BankStyle.Legacy::pointBar)
         )
         .apply(var0, BankStyle.Legacy::new)
   );
   public static final Codec<BankStyle> CODEC = RecordCodecBuilder.create(
      var0 -> var0.group(MAIN_CODEC.forGetter(BankStyle::toMain), LEGACY_CODEC.forGetter(var0x -> BankStyle.Legacy.NONE)).apply(var0, BankStyle::assemble)
   );
   private static final List<BlockState> DEFAULT_BED = List.of(Blocks.GRAVEL.defaultBlockState());
   private static final List<BlockState> DEFAULT_WATERLINE = List.of(Blocks.COARSE_DIRT.defaultBlockState());
   private static final List<BlockState> DEFAULT_BANK = List.of(CHAMELEON);
   private static final List<BlockState> AIR = List.of(Blocks.AIR.defaultBlockState());
   private static final List<TagKey<Biome>> LOOSE_TAGS = List.of(
      tag("minecraft:is_beach"),
      tag("minecraft:is_badlands"),
      tag("minecraft:is_savanna"),
      tag("c:is_sandy"),
      tag("c:is_desert"),
      tag("c:is_beach"),
      tag("c:is_badlands"),
      tag("c:is_savanna"),
      tag("forge:is_sandy"),
      tag("forge:is_desert"),
      tag("forge:is_beach"),
      tag("forge:is_savanna")
   );
   private static final List<TagKey<Biome>> GRAVEL_TAGS = List.of(tag("c:is_stony_shores"), tag("forge:is_stony_shores"));
   private static final List<TagKey<Biome>> COHESIVE_TAGS = List.of(
      tag("minecraft:is_forest"),
      tag("minecraft:is_taiga"),
      tag("minecraft:is_jungle"),
      tag("c:is_swamp"),
      tag("c:is_jungle"),
      tag("c:is_wet"),
      tag("c:is_mangrove"),
      tag("c:is_lush"),
      tag("forge:is_swamp"),
      tag("forge:is_wet")
   );
   private static final List<TagKey<Biome>> DRY_TAGS = List.of(tag("c:is_dry"), tag("forge:is_dry"));
   private static final List<TagKey<Biome>> RIVER_TAGS = List.of(tag("minecraft:is_river"), tag("c:is_river"), tag("forge:is_river"));
   private static final List<TagKey<Biome>> MOUNTAIN_TAGS = List.of(
      tag("minecraft:is_mountain"), tag("minecraft:is_hill"), tag("c:is_mountain"), tag("forge:is_mountain")
   );

   public BankStyle {
tags = tags;
excludeTags = excludeTags;
enabled = enabled;
rings = rings;
}

   public static boolean isChameleon(BlockState var0) {
      return var0 != null && var0.is(Blocks.STRUCTURE_VOID);
   }

   public static boolean anyStyleUsesBend() {
      return anyStyleUsesBend;
   }

   private static BankStyle assemble(BankStyle.Main var0, BankStyle.Legacy var1) {
       ArrayList<BankRing> var2 = new ArrayList<>();
      BankRing.Spec var3 = var0.bed().orElseGet(() -> BankRing.Spec.bare(DEFAULT_BED));
      boolean var4 = var3.bare() && var1.bedEnabled().map(var0x -> !var0x).orElse(false);
      double var5 = var3.bare() ? var1.underwaterNoise().map(var0x -> (double)var0x.floatValue()).orElse(0.18) : var3.edgeNoise().orElse(0.3);
      List var7 = var3.overrides();
      if (var3.bare() && !var1.pointBar().isEmpty()) {
         var7 = List.of(new BankRing.RingOverride(List.of(BankRing.Condition.legacyPointBar()), 0.0, var1.pointBar(), null, 0.0));
      }

      var2.add(ring(BankRing.Kind.BED, var4 ? AIR : var3.blocks(), BankRing.Size.NONE, BankRing.Size.NONE, BankRing.Size.NONE, var3, var5, var7));
      var3 = var0.waterline().orElseGet(() -> BankRing.Spec.bare(DEFAULT_WATERLINE));
      var4 = var3.bare() && var1.waterlineEnabled().map(var0x -> !var0x).orElse(false);
      BankRing.Size var6;
      BankRing.Size var17;
      double var23;
      if (var3.bare()) {
         var6 = BankRing.Size.blocks(var1.waterlineBelow().orElse(1));
         var17 = BankRing.Size.blocks(var1.waterlineAbove().orElse(1));
         var23 = var1.aboveWaterNoise().map(var0x -> (double)var0x.floatValue()).orElse(0.18);
      } else {
         var6 = BankRing.Size.percent(var3.below().orElse(10.0));
         var17 = BankRing.Size.percent(var3.above().orElse(0.0));
         var23 = var3.edgeNoise().orElse(0.3);
      }

      var2.add(ring(BankRing.Kind.WATERLINE, var4 ? AIR : var3.blocks(), var17, var6, BankRing.Size.NONE, var3, var23, var3.overrides()));
      var3 = var0.bank().orElseGet(() -> BankRing.Spec.bare(DEFAULT_BANK));
      var4 = var3.bare() && var1.bankEnabled().map(var0x -> !var0x).orElse(false);
      BankRing.Size var18 = !var3.bare() && var3.reach().isPresent() ? BankRing.Size.percent(var3.reach().get()) : BankRing.Size.NONE;
      double var21 = var3.bare() ? 0.18 : var3.edgeNoise().orElse(0.3);
      var2.add(ring(BankRing.Kind.BANK, var4 ? AIR : var3.blocks(), BankRing.Size.NONE, BankRing.Size.NONE, var18, var3, var21, var3.overrides()));
      var0.topBank().ifPresent(var1x -> {
         BankRing.Size var2x = !var1x.bare() && var1x.reach().isPresent() ? BankRing.Size.percent(var1x.reach().get()) : BankRing.Size.NONE;
         double var3x = var1x.bare() ? 0.18 : var1x.edgeNoise().orElse(0.3);
         var2.add(ring(BankRing.Kind.TOP_BANK, var1x.blocks(), BankRing.Size.NONE, BankRing.Size.NONE, var2x, var1x, var3x, var1x.overrides()));
      });

      for (BankRing.Spec var15 : var0.rings()) {
         BankRing.Size var19 = !var15.bare() && var15.reach().isPresent() ? BankRing.Size.percent(var15.reach().get()) : BankRing.Size.NONE;
         double var22 = var15.bare() ? 0.18 : var15.edgeNoise().orElse(0.3);
         var2.add(ring(BankRing.Kind.OUTER, var15.blocks(), BankRing.Size.NONE, BankRing.Size.NONE, var19, var15, var22, var15.overrides()));
      }

      BankStyle var12 = new BankStyle(
         var0.biomes(), var0.tags(), var0.excludeBiomes(), var0.excludeTags(), var0.star(), var0.enabled(), var0.cohesion(), List.copyOf(var2)
      );

      for (BankRing var20 : var2) {
         if (var20.usesBend()) {
            anyStyleUsesBend = true;
         }
      }

      return var12;
   }

   private static BankRing ring(
      BankRing.Kind var0,
      List<BlockState> var1,
      BankRing.Size var2,
      BankRing.Size var3,
      BankRing.Size var4,
      BankRing.Spec var5,
      double var6,
      List<BankRing.RingOverride> var8
   ) {
      BankRing.Pattern var9 = var5.bare() ? BankRing.Pattern.SPECKLE : var5.pattern().orElse(BankRing.Pattern.BLOBS);
      double var10 = var5.scalePercent().orElse(30.0);
      return new BankRing(var0, var1, var2, var3, var4, var9, var10, var6, var8, var5.features(), var5.bare());
   }

   private BankStyle.Main toMain() {
      Optional var1 = Optional.empty();
      ArrayList var2 = new ArrayList();

      for (BankRing var4 : this.rings) {
         if (var4.kind() == BankRing.Kind.TOP_BANK) {
            var1 = Optional.of(var4.toSpec());
         } else if (var4.kind() == BankRing.Kind.OUTER) {
            var2.add(var4.toSpec());
         }
      }

      return new BankStyle.Main(
         this.biomes,
         this.tags,
         this.excludeBiomes,
         this.excludeTags,
         this.star,
         this.enabled,
         this.cohesion,
         Optional.of(this.bed().toSpec()),
         Optional.of(this.waterline().toSpec()),
         Optional.of(this.bank().toSpec()),
         var1,
         var2
      );
   }

   public BankRing bed() {
      return this.rings.get(0);
   }

   public BankRing waterline() {
      return this.rings.get(1);
   }

   public BankRing bank() {
      return this.rings.get(2);
   }

   public List<BankRing> lateralRings() {
      return this.rings.subList(2, this.rings.size());
   }

   public List<BlockState> bedBlocks() {
      return this.bed().unpainted() ? List.of() : this.bed().blocks();
   }

   public int ringIndexAt(int var1, double var2, double var4, double var6, boolean var8, double var9, double var11) {
      if (var8) {
         return 0;
      }

      BankRing var13 = this.bed();
      BankRing var14 = this.waterline();
      int var15 = -var14.below().blocksFor(var4);
      int var16 = var14.above().blocksFor(var4);
      double var17 = var13.edgeBand(var4);
      double var19 = var14.edgeBand(var4);
      double var21 = var1 - var15;
      double var23 = var1 - var16;
      if (var19 > 0.0 && Math.abs(var23) <= Math.abs(var21) && Math.abs(var23) < var19) {
         boolean var42 = var9 < 0.5 + 0.5 * var23 / var19;
         if (!var42) {
            return 1;
         }
      } else {
         if (var17 > 0.0 && Math.abs(var21) < var17) {
            return var9 < 0.5 + 0.5 * var21 / var17 ? 1 : 0;
         }

         if (var1 <= var16) {
            return var1 < var15 ? 0 : 1;
         }

         boolean var25 = true;
      }

      List var26 = this.lateralRings();
      if (var26.size() != 1 && !Double.isNaN(var2)) {
         double var27 = 0.0;
         double var29 = Math.max(0.0, var2);

         for (int var31 = 0; var31 < var26.size(); var31++) {
            BankRing var32 = (BankRing)var26.get(var31);
            double var33;
            if (var32.reach().isSet()) {
               var33 = var32.reach().blocksFor(var4);
            } else if (var31 == 0) {
               var33 = Math.max(1L, Math.round(0.5 * var4));
            } else {
               var33 = Math.max(0.0, var6 - var27);
            }

            double var35 = Math.min(var6, var27 + var33);
            boolean var37 = var31 == var26.size() - 1 || var35 >= var6;
            if (var37) {
               return 2 + var31;
            }

            double var38 = var32.edgeBand(var4);
            double var40 = var29 - var35;
            if (var38 > 0.0 && Math.abs(var40) < var38) {
               if (var11 >= 0.5 + 0.5 * var40 / var38) {
                  return 2 + var31;
               }
            } else if (var29 < var35) {
               return 2 + var31;
            }

            var27 = var35;
         }

         return 2 + var26.size() - 1;
      } else {
         return 2;
      }
   }

   public double cohesionOr(Holder<Biome> var1) {
      return this.cohesion >= 0.0F ? this.cohesion : cohesionOf(var1);
   }

   public static double cohesionOf(Holder<Biome> var0) {
      double var1 = 0.6;
      if (anyTag(var0, LOOSE_TAGS)) {
         return 0.0;
      } else if (anyTag(var0, GRAVEL_TAGS)) {
         return 0.3;
      } else if (anyTag(var0, RIVER_TAGS)) {
         return 0.3;
      } else if (anyTag(var0, COHESIVE_TAGS)) {
         return 1.0;
      } else if (anyTag(var0, DRY_TAGS)) {
         return 0.15;
      } else {
         return anyTag(var0, MOUNTAIN_TAGS) ? 0.8 : var1;
      }
   }

   private static boolean anyTag(Holder<Biome> var0, List<TagKey<Biome>> var1) {
      for (TagKey var3 : var1) {
         if (var0.is(var3)) {
            return true;
         }
      }

      return false;
   }

   private static TagKey<Biome> tag(String var0) {
      int var1 = var0.indexOf(58);
      return TagKey.create(Registries.BIOME, new ResourceLocation(var0.substring(0, var1), var0.substring(var1 + 1)));
   }

   public boolean matchesExact(ResourceKey<Biome> var1) {
      return var1 != null && this.biomes.contains(var1.location());
   }

   public boolean matchesTags(Holder<Biome> var1) {
      if (this.tags.isEmpty()) {
         return false;
      }

      for (TagKey var3 : this.tags) {
         if (!var1.is(var3)) {
            return false;
         }
      }

      return true;
   }

   public boolean excluded(Holder<Biome> var1) {
      ResourceKey var2 = (ResourceKey)var1.unwrapKey().orElse(null);
      if (var2 != null && this.excludeBiomes.contains(var2.location())) {
         return true;
      }

      for (TagKey var4 : this.excludeTags) {
         if (var1.is(var4)) {
            return true;
         }
      }

      return false;
   }

   private record Legacy(Optional<Boolean> bedEnabled, Optional<Boolean> waterlineEnabled, Optional<Boolean> bankEnabled, Optional<Integer> waterlineBelow, Optional<Integer> waterlineAbove, Optional<Float> underwaterNoise, Optional<Float> aboveWaterNoise, List<BlockState> pointBar) {
      static final BankStyle.Legacy NONE = new BankStyle.Legacy(
         Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of()
      );

      private Legacy {
waterlineEnabled = waterlineEnabled;
waterlineBelow = waterlineBelow;
underwaterNoise = underwaterNoise;
pointBar = pointBar;
}
   }

   private record Main(List<ResourceLocation> biomes, List<TagKey<Biome>> tags, List<ResourceLocation> excludeBiomes, List<TagKey<Biome>> excludeTags, boolean star, boolean enabled, float cohesion, Optional<BankRing.Spec> bed, Optional<BankRing.Spec> waterline, Optional<BankRing.Spec> bank, Optional<BankRing.Spec> topBank, List<BankRing.Spec> rings) {

      private Main {
tags = tags;
excludeTags = excludeTags;
enabled = enabled;
bed = bed;
bank = bank;
rings = rings;
}
   }
}
