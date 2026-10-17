package dev.streamsreflowing.worldgen.bank;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.world.level.block.state.BlockState;

public record BankRing(BankRing.Kind kind, List<BlockState> blocks, BankRing.Size above, BankRing.Size below, BankRing.Size reach, BankRing.Pattern pattern, double scalePercent, double edgeNoise, List<BankRing.RingOverride> overrides, List<BankRing.Feature> features, boolean legacyForm) {
   public static final double DEFAULT_SCALE_PERCENT = 30.0;
   public static final double DEFAULT_EDGE_NOISE = 0.3;
   public static final double LEGACY_EDGE_NOISE = 0.18;
   public static final double DEFAULT_FADE = 0.1;
   public static final double LEGACY_NOISE_RANGE = 3.0;
   private static final Codec<BankRing.Pattern> PATTERN_CODEC = Codec.STRING.comapFlatMap(var0 -> {
      try {
         return DataResult.success(BankRing.Pattern.valueOf(var0.trim().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException var2) {
         return DataResult.error(() -> "pattern must be blobs, bands or speckle, got \"" + var0 + "\"");
      }
   }, var0 -> var0.name().toLowerCase(Locale.ROOT));
   private static final Codec<BankRing.Side> SIDE_CODEC = Codec.STRING.comapFlatMap(var0 -> {
      try {
         return DataResult.success(BankRing.Side.valueOf(var0.trim().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException var2) {
         return DataResult.error(() -> "side must be inner or outer, got \"" + var0 + "\"");
      }
   }, var0 -> var0.name().toLowerCase(Locale.ROOT));
   private static final Codec<List<BankRing.Condition>> WHEN_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
      .comapFlatMap(
         var0 -> {
             ArrayList<BankRing.Condition> var1 = new ArrayList<>();

             for (Entry<String, String> var3 : var0.entrySet()) {
                String var4 = var3.getKey().trim().toLowerCase(Locale.ROOT);
               if (var4.equals("side")) {
                  try {
                     var1.add(BankRing.Condition.side(BankRing.Side.valueOf(((String)var3.getValue()).trim().toUpperCase(Locale.ROOT))));
                  } catch (IllegalArgumentException var7) {
                      return DataResult.error(() -> "when.side must be inner or outer, got \"" + var3.getValue() + "\"");
                  }
               } else {
                  BankRing.Metric var5;
                  try {
                     var5 = BankRing.Metric.valueOf(var4.toUpperCase(Locale.ROOT));
                  } catch (IllegalArgumentException var8) {
                     return DataResult.error(
                      () -> "when: unknown property \"" + var3.getKey() + "\" (slope, width, speed, bend, to_mouth, to_junction, flat_run, side)"
                     );
                  }

                   DataResult<BankRing.Condition> var6 = BankRing.Condition.parse(var5, var3.getValue());
                  if (var6.error().isPresent()) {
                     return DataResult.error(() -> ((PartialResult)var6.error().get()).message());
                  }

                   var1.add(var6.result().get());
               }
            }

            return var1.isEmpty() ? DataResult.error(() -> "when must name at least one property") : DataResult.success(List.copyOf(var1));
         },
         var0 -> {
            LinkedHashMap var1 = new LinkedHashMap();

            for (BankRing.Condition var3 : var0) {
               if (var3.side() != null) {
                  var1.put("side", var3.side().name().toLowerCase(Locale.ROOT));
               } else if (var3.metric() != null) {
                  var1.put(var3.metric().name().toLowerCase(Locale.ROOT), (var3.greater() ? ">" : "<") + (var3.orEqual() ? "=" : "") + " " + var3.value());
               }
            }

            return var1;
         }
      );
   public static final Codec<BankRing.RingOverride> RING_OVERRIDE_CODEC = RecordCodecBuilder.create(
      var0 -> var0.group(
            WHEN_CODEC.fieldOf("when").forGetter(BankRing.RingOverride::when),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("fade", 0.1).forGetter(BankRing.RingOverride::fade),
            BankStyle.BLOCK_LIST.fieldOf("block").forGetter(BankRing.RingOverride::blocks),
            PATTERN_CODEC.optionalFieldOf("pattern").forGetter(var0x -> Optional.ofNullable(var0x.pattern())),
            Codec.doubleRange(1.0, 1000.0)
               .optionalFieldOf("scale")
               .forGetter(var0x -> var0x.scalePercent() > 0.0 ? Optional.of(var0x.scalePercent()) : Optional.empty())
         )
         .apply(var0, (var0x, var1, var2, var3, var4) -> new BankRing.RingOverride(var0x, var1, var2, (BankRing.Pattern)var3.orElse(null), var4.orElse(0.0)))
   );
   public static final Codec<BankRing.Feature> FEATURE_CODEC = RecordCodecBuilder.create(
      var0 -> var0.group(
            Codec.STRING.fieldOf("id").forGetter(BankRing.Feature::id),
            Codec.intRange(0, 100).optionalFieldOf("chance", 100).forGetter(BankRing.Feature::chance)
         )
         .apply(var0, BankRing.Feature::new)
   );
   private static final Codec<BankRing.Spec> OBJECT_CODEC = RecordCodecBuilder.create(
      var0 -> var0.group(
            BankStyle.BLOCK_LIST.fieldOf("block").forGetter(BankRing.Spec::blocks),
            Codec.doubleRange(0.0, 1000.0).optionalFieldOf("reach").forGetter(BankRing.Spec::reach),
            Codec.doubleRange(0.0, 1000.0).optionalFieldOf("above").forGetter(BankRing.Spec::above),
            Codec.doubleRange(0.0, 1000.0).optionalFieldOf("below").forGetter(BankRing.Spec::below),
            PATTERN_CODEC.optionalFieldOf("pattern").forGetter(BankRing.Spec::pattern),
            Codec.doubleRange(1.0, 1000.0).optionalFieldOf("scale").forGetter(BankRing.Spec::scalePercent),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("edge_noise").forGetter(BankRing.Spec::edgeNoise),
            RING_OVERRIDE_CODEC.listOf().optionalFieldOf("overrides", List.of()).forGetter(BankRing.Spec::overrides),
            FEATURE_CODEC.listOf().optionalFieldOf("features", List.of()).forGetter(BankRing.Spec::features)
         )
         .apply(
            var0, (var0x, var1, var2, var3, var4, var5, var6, var7, var8) -> new BankRing.Spec(var0x, var1, var2, var3, var4, var5, var6, var7, var8, false)
         )
   );
   public static final Codec<BankRing.Spec> SPEC_CODEC = Codec.either(BankStyle.BLOCK_LIST, OBJECT_CODEC)
      .xmap(var0 -> (BankRing.Spec)var0.map(BankRing.Spec::bare, var0x -> var0x), var0 -> var0.bare() ? Either.left(var0.blocks()) : Either.right(var0));

   public BankRing {
blocks = blocks;
below = below;
pattern = pattern;
edgeNoise = edgeNoise;
features = features;
}

   public boolean unpainted() {
      for (BlockState var2 : this.blocks) {
         if (!var2.isAir()) {
            return false;
         }
      }

      return true;
   }

   public double edgeBand(double var1) {
      double var3 = this.legacyForm ? 3.0 : Math.max(3.0, var1 * 0.25);
      return this.edgeNoise * var3;
   }

   public boolean usesBend() {
      for (BankRing.RingOverride var2 : this.overrides) {
         if (var2.usesBend()) {
            return true;
         }
      }

      return false;
   }

   public BlockState pick(long var1, int var3, int var4, int var5, double var6, double var8, BankRing.Metrics var10, int var11) {
      List var12 = this.blocks;
      BankRing.Pattern var13 = this.pattern;
      double var14 = this.scalePercent;

      for (int var16 = 0; var16 < this.overrides.size(); var16++) {
         BankRing.RingOverride var17 = this.overrides.get(var16);
         double var18 = var17.strength(var10);
         if (!(var18 <= 0.0) && (var18 >= 1.0 || hash01(var1, var3, 977 + var11 * 31 + var16, var5) < var18)) {
            var12 = var17.blocks();
            if (var17.pattern() != null) {
               var13 = var17.pattern();
            }

            if (var17.scalePercent() > 0.0) {
               var14 = var17.scalePercent();
            }
            break;
         }
      }

      if (var12.isEmpty()) {
         return null;
      }

      int var26 = var12.size();
      BlockState var27;
      if (var26 == 1) {
         var27 = (BlockState)var12.get(0);
      } else if (var13 == BankRing.Pattern.SPECKLE) {
         var27 = (BlockState)var12.get(Math.min(var26 - 1, (int)(hash01(var1, var5, var4, var3) * var26)));
      } else {
         double var28 = Math.max(2.0, var14 / 100.0 * var6);
         double var20;
         if (var13 == BankRing.Pattern.BANDS && !Double.isNaN(var8)) {
            double var22 = (hash01(var1, var3, 311 + var11, var5) - 0.5) * 0.4 * var28;
            double var24 = Math.floor((Math.max(0.0, var8) + var22) / var28);
            var20 = (var24 % var26 + var26) % var26 / var26 + 0.5 / var26;
         } else {
            var20 = valueNoise(var1 + 7919L * (var11 + 1), var3 / var28, var5 / var28);
         }

         var27 = (BlockState)var12.get(Math.min(var26 - 1, (int)(var20 * var26)));
      }

      return var27;
   }

   public static double hash01(long var0, int var2, int var3, int var4) {
      long var5 = var2 * -7046029254386353131L ^ var3 * -4417276706812531889L ^ var4 * 1609587929392839161L ^ var0 * 2685821657736338717L;
      var5 ^= var5 >>> 30;
      var5 *= -4658895280553007687L;
      var5 ^= var5 >>> 27;
      var5 *= -7723592293110705685L;
      var5 ^= var5 >>> 31;
      return (var5 >>> 11) * 1.110223E-16F;
   }

   public static double valueNoise(long var0, double var2, double var4) {
      int var6 = (int)Math.floor(var2);
      int var7 = (int)Math.floor(var4);
      double var8 = var2 - var6;
      double var10 = var4 - var7;
      var8 = var8 * var8 * (3.0 - 2.0 * var8);
      var10 = var10 * var10 * (3.0 - 2.0 * var10);
      double var12 = hash01(var0, var6, 0, var7);
      double var14 = hash01(var0, var6 + 1, 0, var7);
      double var16 = hash01(var0, var6, 0, var7 + 1);
      double var18 = hash01(var0, var6 + 1, 0, var7 + 1);
      double var20 = var12 + (var14 - var12) * var8;
      double var22 = var16 + (var18 - var16) * var8;
      double var24 = var20 + (var22 - var20) * var10;
      return var24 >= 1.0 ? 0.9999999 : var24;
   }

   public BankRing.Spec toSpec() {
      return this.legacyForm
         ? BankRing.Spec.bare(this.blocks)
         : new BankRing.Spec(
            this.blocks,
            this.reach.isSet() && this.reach.isPercent() ? Optional.of(this.reach.percent()) : Optional.empty(),
            this.above.isSet() && this.above.isPercent() ? Optional.of(this.above.percent()) : Optional.empty(),
            this.below.isSet() && this.below.isPercent() ? Optional.of(this.below.percent()) : Optional.empty(),
            Optional.of(this.pattern),
            Optional.of(this.scalePercent),
            Optional.of(this.edgeNoise),
            this.overrides,
            this.features,
            false
         );
   }

   public record Condition(BankRing.Metric metric, boolean greater, boolean orEqual, double value, BankRing.Side side, boolean pointBar) {

      public Condition {
greater = greater;
value = value;
pointBar = pointBar;
}

      public static BankRing.Condition side(BankRing.Side var0) {
         return new BankRing.Condition(null, false, false, 0.0, var0, false);
      }

      public static BankRing.Condition legacyPointBar() {
         return new BankRing.Condition(null, false, false, 0.0, null, true);
      }

      static DataResult<BankRing.Condition> parse(BankRing.Metric var0, String var1) {
         String var2 = var1.trim();
         boolean var4 = false;
         boolean var3;
         if (var2.startsWith(">=")) {
            var3 = true;
            var4 = true;
            var2 = var2.substring(2);
         } else if (var2.startsWith("<=")) {
            var3 = false;
            var4 = true;
            var2 = var2.substring(2);
         } else if (var2.startsWith(">")) {
            var3 = true;
            var2 = var2.substring(1);
         } else {
            if (!var2.startsWith("<")) {
               return DataResult.error(() -> "when." + var0.name().toLowerCase(Locale.ROOT) + ": expected a comparison like \"> 0.3\", got \"" + var1 + "\"");
            }

            var3 = false;
            var2 = var2.substring(1);
         }

         try {
            double var5 = Double.parseDouble(var2.trim());
            return !(var5 < 0.0) && !(var5 > 1.0)
               ? DataResult.success(new BankRing.Condition(var0, var3, var4, var5, null, false))
               : DataResult.error(() -> "when." + var0.name().toLowerCase(Locale.ROOT) + ": the value must be 0..1, got " + var1);
         } catch (NumberFormatException var7) {
            return DataResult.error(() -> "when." + var0.name().toLowerCase(Locale.ROOT) + ": not a number in \"" + var1 + "\"");
         }
      }

      String describe() {
         if (this.pointBar) {
            return "point_bar";
         } else {
            return this.side != null
               ? "side=" + this.side.name().toLowerCase(Locale.ROOT)
               : this.metric.name().toLowerCase(Locale.ROOT) + (this.greater ? ">" : "<") + (this.orEqual ? "=" : "") + this.value;
         }
      }

      double strength(BankRing.Metrics var1, double var2) {
         if (this.pointBar) {
            return var1.pointBar() ? 1.0 : 0.0;
         } else if (this.side != null) {
            return var1.side() == this.side ? 1.0 : 0.0;
         } else {
            double var4 = var1.get(this.metric);
            double var6 = this.greater ? var4 - this.value : this.value - var4;
            if (!(var2 <= 0.0)) {
               double var8 = (var6 + var2) / (2.0 * var2);
               return var8 <= 0.0 ? 0.0 : (var8 >= 1.0 ? 1.0 : var8 * var8 * (3.0 - 2.0 * var8));
            } else {
               return !(var6 > 0.0) && (!this.orEqual || var6 != 0.0) ? 0.0 : 1.0;
            }
         }
      }
   }

   public record Feature(String id, int chance) {

      public Feature {
chance = chance;
}
   }

   public enum Kind {
      BED,
      WATERLINE,
      BANK,
      TOP_BANK,
      OUTER;
   }

   public enum Metric {
      SLOPE,
      WIDTH,
      SPEED,
      BEND,
      TO_MOUTH,
      TO_JUNCTION,
      FLAT_RUN;
   }

   public record Metrics(double slope, double width, double speed, double bend, double toMouth, double toJunction, double flatRun, BankRing.Side side, boolean pointBar) {
      public static final BankRing.Metrics NONE = new BankRing.Metrics(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, null, false);

      public Metrics {
width = width;
bend = bend;
toJunction = toJunction;
side = side;
}

      double get(BankRing.Metric var1) {
         return switch (var1) {
            case SLOPE -> this.slope;
            case WIDTH -> this.width;
            case SPEED -> this.speed;
            case BEND -> this.bend;
            case TO_MOUTH -> this.toMouth;
            case TO_JUNCTION -> this.toJunction;
            case FLAT_RUN -> this.flatRun;
         };
      }
   }

   public enum Pattern {
      BLOBS,
      BANDS,
      SPECKLE;
   }

   public record RingOverride(List<BankRing.Condition> when, double fade, List<BlockState> blocks, BankRing.Pattern pattern, double scalePercent) {

      public RingOverride {
fade = fade;
pattern = pattern;
}

      public double strength(BankRing.Metrics var1) {
         double var2 = 1.0;

         for (BankRing.Condition var5 : this.when) {
            var2 *= var5.strength(var1, this.fade);
            if (var2 <= 0.0) {
               return 0.0;
            }
         }

         return var2;
      }

      public boolean usesBend() {
         for (BankRing.Condition var2 : this.when) {
            if (var2.side() != null || var2.metric() == BankRing.Metric.BEND) {
               return true;
            }
         }

         return false;
      }
   }

   public enum Side {
      INNER,
      OUTER;
   }

   public record Size(double percent, int blocks, boolean isPercent, boolean isSet) {
      public static final BankRing.Size NONE = new BankRing.Size(0.0, 0, true, false);

      public Size {
blocks = blocks;
isSet = isSet;
}

      public static BankRing.Size percent(double var0) {
         return new BankRing.Size(var0, 0, true, true);
      }

      public static BankRing.Size blocks(int var0) {
         return new BankRing.Size(0.0, var0, false, true);
      }

      public int blocksFor(double var1) {
         if (!this.isPercent) {
            return this.blocks;
         }

         double var3 = this.percent / 100.0 * var1;
         return var3 <= 0.0 ? 0 : Math.max(1, (int)Math.round(var3));
      }
   }

   public record Spec(List<BlockState> blocks, Optional<Double> reach, Optional<Double> above, Optional<Double> below, Optional<BankRing.Pattern> pattern, Optional<Double> scalePercent, Optional<Double> edgeNoise, List<BankRing.RingOverride> overrides, List<BankRing.Feature> features, boolean bare) {

      public Spec {
reach = reach;
below = below;
scalePercent = scalePercent;
overrides = overrides;
bare = bare;
}

      public static BankRing.Spec bare(List<BlockState> var0) {
         return new BankRing.Spec(
            var0, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), true
         );
      }
   }
}
