package dev.streamsreflowing.worldgen.regionprep;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.streamsreflowing.Constants;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.DensityFunctions.HolderHolder;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public final class FieldFormBuilder {
   public static volatile String lastDebugJson;
   public static volatile String lastTypeCensus;
   private final RegistryAccess b;
   private final PositionalRandomFactory c;
   private final JsonObject d = new JsonObject();
   private final Map<String, FieldForm.Node> e = new HashMap<>();
   private final Map<String, FieldForm.Node> f = new HashMap<>();
   private final Map<String, Integer> g = new HashMap<>();
   final Map<String, Integer> a = new TreeMap<>();
   private final FieldForm.NoiseRegistry h = new FieldForm.NoiseRegistry();
   private final RegistryOps<JsonElement> i;
   private DensityFunction j;
   private IdentityHashMap<Object, String> k;
   private final Map<String, FieldForm.Spline> l = new HashMap<>();
   private final Map<String, NoiseHolder> m = new HashMap<>();
   private static final double[][] n = new double[][]{{0.13, 7.5, -911.2}, {5001.7, -33.0, 12.9}, {-77.3, 120.0, 77.3}, {123456.0, 0.0, -654321.0}};

   private FieldFormBuilder(RegistryAccess var1, long var2) {
      this.b = var1;
      this.i = RegistryOps.create(JsonOps.INSTANCE, var1);
      this.c = new XoroshiroRandomSource(var2).forkPositional();
   }

   public static FieldForm build(RegistryAccess var0, long var1, boolean var3, DensityFunction var4, int var5, int var6, int var7) {
      if (var3) {
         throw new FieldFormBuilder.Unsupported("legacy random source world");
      }

      FieldFormBuilder var8 = new FieldFormBuilder(var0, var1);
      var8.j = var4;
      var8.c(var4);
      JsonElement var9 = var8.a(var4, "root");
      FieldForm.Node var10 = var8.a(var9);
      if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
         JsonObject var11 = new JsonObject();
         var11.add("root", var9);
         var11.add("refs", var8.d);
         lastDebugJson = var11.toString();
      }

      lastTypeCensus = var8.a.toString();
      return new FieldForm(var10, var8.h.b, var5, var6, var7);
   }

   static DensityFunction a(DensityFunction var0) {
      for (int var1 = 0; var1 < 8 && var0 != null; var1++) {
         Class var2 = var0.getClass();
         if (!var2.getName().startsWith("com.ishland.c2me.opts.dfc.")) {
            return var0;
         }

         DensityFunction var3 = null;

         try {
            for (Class var4 = var2; var4 != null && var3 == null; var4 = var4.getSuperclass()) {
               try {
                  Method var5 = var4.getDeclaredMethod("getFallback");
                  var5.setAccessible(true);
                  var3 = (DensityFunction)var5.invoke(var0);
               } catch (NoSuchMethodException var6) {
               }
            }
         } catch (Throwable var7) {
            throw new FieldFormBuilder.Unsupported("compiled density function " + var2.getName() + " not readable: " + var7);
         }

         if (var3 == null || var3 == var0) {
            throw new FieldFormBuilder.Unsupported("compiled density function " + var2.getName() + " kept no original tree");
         }

         var0 = var3;
      }

      return var0;
   }

   private static DensityFunction b(DensityFunction var0) {
      for (int var1 = 0; var1 < 16 && var0 != null; var1++) {
         if (var0 instanceof HolderHolder var2) {
            Holder var3 = var2.function();
            if (var3.unwrapKey().isPresent()) {
               return var0;
            }

            DensityFunction var4 = (DensityFunction)var3.value();
            if (var4 == var0) {
               return var0;
            }

            var0 = var4;
         } else {
            if (!var0.getClass().getName().startsWith("com.ishland.c2me.opts.dfc.")) {
               return var0;
            }

            var0 = a(var0);
         }
      }

      return var0;
   }

   private JsonElement a(DensityFunction var1, String var2) {
      var1 = a(var1);
      var1 = var1.mapAll(FieldFormBuilder::b);
      DataResult<JsonElement> var3 = DensityFunction.HOLDER_HELPER_CODEC.encodeStart(this.i, var1);
      return (JsonElement)var3.result()
         .orElseThrow(() -> new FieldFormBuilder.Unsupported(var2 + " did not encode: " + var3.error().map(Object::toString).orElse("?")));
   }

   private FieldForm.Node a(JsonElement var1) {
      if (var1.isJsonPrimitive()) {
         if (var1.getAsJsonPrimitive().isNumber()) {
            return new FieldForm.Const(var1.getAsDouble());
         }

         String var12 = var1.getAsString();
         FieldForm.Node var13 = this.e.get(var12);
         if (var13 != null) {
            return var13;
         }

         Registry var14 = this.b.registryOrThrow(Registries.DENSITY_FUNCTION);
         ResourceLocation var15 = ResourceLocation.tryParse(var12);
         DensityFunction var16 = var15 == null ? null : (DensityFunction)var14.get(var15);
         if (var16 == null) {
            throw new FieldFormBuilder.Unsupported("unresolvable density function reference: " + var12);
         }

         JsonElement var17 = this.a(var16, "reference " + var12);
         this.d.add(var12, var17);
         FieldForm.Node var18 = this.a(var17);
         this.e.put(var12, var18);
         return var18;
      } else {
         JsonObject var2 = var1.getAsJsonObject();
         String var3 = var2.toString();
         FieldForm.Node var4 = this.f.get(var3);
         if (var4 != null) {
            return var4;
         }

         String var5 = var2.get("type").getAsString();
         this.a.merge(var5, 1, Integer::sum);
         String var6 = var5.startsWith("minecraft:") ? var5.substring("minecraft:".length()) : var5;
         FieldForm.Node var10000;
         switch (var6) {
            case "constant":
               var10000 = new FieldForm.Const(var2.get("argument").getAsDouble());
               break;
            case "blend_alpha":
               var10000 = new FieldForm.Const(1.0);
               break;
            case "blend_offset":
               var10000 = new FieldForm.Const(0.0);
               break;
            case "beardifier":
               var10000 = new FieldForm.Const(0.0);
               break;
            case "blend_density":
            case "cache_once":
            case "cache_all_in_cell":
               var10000 = this.a(var2.get("argument"));
               break;
            case "cache_2d":
               var10000 = new FieldForm.Cache2D(this.a(var2.get("argument")));
               break;
            case "flat_cache":
               var10000 = new FieldForm.FlatCache(this.a(var2.get("argument")));
               break;
            case "interval_select":
               var10000 = this.b(var2);
               break;
            case "interpolated":
               var10000 = new FieldForm.Interp(this.a(var2.get("argument")));
               break;
            case "add":
            case "mul":
            case "min":
            case "max":
               var10000 = new FieldForm.Bin(switch (var6) {
                  case "add" -> FieldForm.BinOp.ADD;
                  case "mul" -> FieldForm.BinOp.MUL;
                  case "min" -> FieldForm.BinOp.MIN;
                  default -> FieldForm.BinOp.MAX;
               }, this.a(var2.get("argument1")), this.a(var2.get("argument2")));
               break;
            case "abs":
            case "square":
            case "cube":
            case "half_negative":
            case "quarter_negative":
            case "squeeze":
               var10000 = new FieldForm.Map1(switch (var6) {
                  case "abs" -> FieldForm.MapOp.ABS;
                  case "square" -> FieldForm.MapOp.SQUARE;
                  case "cube" -> FieldForm.MapOp.CUBE;
                  case "half_negative" -> FieldForm.MapOp.HALF_NEGATIVE;
                  case "quarter_negative" -> FieldForm.MapOp.QUARTER_NEGATIVE;
                  default -> FieldForm.MapOp.SQUEEZE;
               }, this.a(var2.get("argument")));
               break;
            case "tectonic:invert":
               var10000 = new FieldForm.Map1(FieldForm.MapOp.RECIPROCAL, this.a(var2.get("argument")));
               break;
            case "larionforgeport:div":
               var10000 = new FieldForm.Bin(FieldForm.BinOp.DIV, this.a(var2.get("argument1")), this.a(var2.get("argument2")));
               break;
            case "larionforgeport:signum":
               var10000 = new FieldForm.Map1(FieldForm.MapOp.SIGNUM, this.a(var2.get("argument")));
               break;
            case "larionforgeport:sine":
               var10000 = new FieldForm.Map1(FieldForm.MapOp.SIN, this.a(var2.get("argument")));
               break;
            case "larionforgeport:z":
               var10000 = new FieldForm.ZCoord();
               break;
            case "clamp":
               var10000 = new FieldForm.Clamp(this.a(var2.get("input")), var2.get("min").getAsDouble(), var2.get("max").getAsDouble());
               break;
            case "range_choice":
               var10000 = new FieldForm.RangeChoice(
                  this.a(var2.get("input")),
                  var2.get("min_inclusive").getAsDouble(),
                  var2.get("max_exclusive").getAsDouble(),
                  this.a(var2.get("when_in_range")),
                  this.a(var2.get("when_out_of_range"))
               );
               break;
            case "y_clamped_gradient":
               var10000 = new FieldForm.YGradient(
                  var2.get("from_y").getAsInt(), var2.get("to_y").getAsInt(), var2.get("from_value").getAsDouble(), var2.get("to_value").getAsDouble()
               );
               break;
            case "noise":
               var10000 = new FieldForm.NoiseNode(
                  this.c(var2.get("noise").getAsString()), var2.get("xz_scale").getAsDouble(), var2.get("y_scale").getAsDouble()
               );
               break;
            case "shift_a":
               var10000 = new FieldForm.ShiftNoise(FieldForm.ShiftKind.SHIFT_A, this.c(var2.get("argument").getAsString()));
               break;
            case "shift_b":
               var10000 = new FieldForm.ShiftNoise(FieldForm.ShiftKind.SHIFT_B, this.c(var2.get("argument").getAsString()));
               break;
            case "shift":
               var10000 = new FieldForm.ShiftNoise(FieldForm.ShiftKind.SHIFT, this.c(var2.get("argument").getAsString()));
               break;
            case "shifted_noise":
               var10000 = new FieldForm.ShiftedNoise(
                  this.a(var2.get("shift_x")),
                  this.a(var2.get("shift_y")),
                  this.a(var2.get("shift_z")),
                  var2.get("xz_scale").getAsDouble(),
                  var2.get("y_scale").getAsDouble(),
                  this.c(var2.get("noise").getAsString())
               );
               break;
            case "weird_scaled_sampler":
               var10000 = new FieldForm.WeirdScaled(
                  this.a(var2.get("input")),
                  this.c(var2.get("noise").getAsString()),
                  "type_1".equals(var2.get("rarity_value_mapper").getAsString()) ? FieldForm.RarityKind.TYPE1 : FieldForm.RarityKind.TYPE2
               );
               break;
            case "spline":
               var10000 = new FieldForm.SplineNode(this.b(var2.get("spline")));
               break;
            case "old_blended_noise":
               var10000 = this.a(var2);
               break;
            default:
               var10000 = this.a(var1, var5);
         }

         FieldForm.Node var7 = var10000;
         this.f.put(var3, var7);
         return var7;
      }
   }

   private FieldForm.Node a(JsonElement var1, String var2) {
      List<DensityFunction> var3 = this.a(var2);
      DensityFunction var4 = null;

      for (DensityFunction var6 : var3) {
         JsonElement var7;
         try {
            var7 = this.a(var6, "live node");
         } catch (RuntimeException var9) {
            continue;
         }

         if (var1.equals(var7)) {
            var4 = var6;
            break;
         }
      }

      if (var4 == null && var3.size() == 1) {
         var4 = (DensityFunction)var3.get(0);
      }

      if (var4 == null) {
         throw new FieldFormBuilder.Unsupported("density function type: " + var2 + " (" + var3.size() + " live candidates, none matched)");
      }

      this.a.merge(var2 + " (external)", 1, Integer::sum);
      DensityFunction var10 = var4;
      ThreadLocal var11 = ThreadLocal.withInitial(FieldFormBuilder.Ctx::new);
      return new FieldForm.Extern((var2x, var3x, var4x) -> {
         FieldFormBuilder.Ctx var5 = (FieldFormBuilder.Ctx)var11.get();
         var5.a = var2x;
         var5.b = var3x;
         var5.c = var4x;
         return var10.compute(var5);
      }, var2);
   }

   private List<DensityFunction> a(final String var1) {
      if (this.k == null) {
         this.k = new IdentityHashMap<>();

         for (Entry var3 : BuiltInRegistries.DENSITY_FUNCTION_TYPE.entrySet()) {
            this.k.put(var3.getValue(), ((ResourceKey)var3.getKey()).location().toString());
         }
      }

      final ArrayList<DensityFunction> var7 = new ArrayList<>(2);
      final TreeMap<String, Integer> var8 = new TreeMap<>();
      final int[] var4 = new int[]{0};

      try {
         a(this.j).mapAll(new Visitor() {
            public DensityFunction apply(DensityFunction var1x) {
               var4[0]++;
               DensityFunction var2 = FieldFormBuilder.b(var1x);

               String var3;
               try {
                  var3 = FieldFormBuilder.this.k.get(var2.codec().codec());
               } catch (RuntimeException var5) {
                  var8.merge("<codec() threw: " + var2.getClass().getSimpleName() + ">", 1, Integer::sum);
                  return var1x;
               }

               var8.merge(var3 == null ? "<unregistered: " + var2.getClass().getName() + ">" : var3, 1, Integer::sum);
               if (var1.equals(var3) && !var7.contains(var2)) {
                  var7.add(var2);
               }

               return var1x;
            }
         });
      } catch (RuntimeException var6) {
         throw new FieldFormBuilder.Unsupported("could not walk the live tree for " + var1 + ": " + var6);
      }

      if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
         Constants.LOG.info("[SR-PREP] live walk for {}: {} nodes visited, {} matched; types seen = {}", new Object[]{var1, var4[0], var7.size(), var8});
      }

      return var7;
   }

   private FieldForm.Spline b(JsonElement var1) {
      String var2 = var1.toString();
      FieldForm.Spline var3 = this.l.get(var2);
      if (var3 != null) {
         return var3;
      }

      FieldForm.Spline var4 = this.c(var1);
      this.l.put(var2, var4);
      return var4;
   }

   private FieldForm.Spline c(JsonElement var1) {
      JsonObject var2 = var1.getAsJsonObject();
      FieldForm.Node var3 = this.a(var2.get("coordinate"));
      JsonArray var4 = var2.getAsJsonArray("points");
      int var5 = var4.size();
      float[] var6 = new float[var5];
      float[] var7 = new float[var5];
      FieldForm.SplineValue[] var8 = new FieldForm.SplineValue[var5];

      for (int var9 = 0; var9 < var5; var9++) {
         JsonObject var10 = var4.get(var9).getAsJsonObject();
         var6[var9] = var10.get("location").getAsFloat();
         var7[var9] = var10.get("derivative").getAsFloat();
         JsonElement var11 = var10.get("value");
         var8[var9] = var11.isJsonPrimitive() && var11.getAsJsonPrimitive().isNumber()
            ? new FieldForm.SplineConst(var11.getAsFloat())
            : new FieldForm.SplineNested(this.b(var11));
      }

      return new FieldForm.Spline(var3, var6, var8, var7);
   }

   private FieldForm.Node a(JsonObject var1) {
      double var2 = var1.get("xz_scale").getAsDouble();
      double var4 = var1.get("y_scale").getAsDouble();
      double var6 = var1.get("xz_factor").getAsDouble();
      double var8 = var1.get("y_factor").getAsDouble();
      double var10 = var1.get("smear_scale_multiplier").getAsDouble();
      RandomSource var12 = this.c.fromHashOf(ResourceLocation.tryParse("minecraft:terrain"));
      int var13 = this.a("blended:minLimit", a(var12, 16));
      int var14 = this.a("blended:maxLimit", a(var12, 16));
      int var15 = this.a("blended:main", a(var12, 8));
      return new FieldForm.Blended(var13, var14, var15, 684.412 * var2, 684.412 * var4, var6, var8, var10);
   }

   private int a(String var1, FieldForm.PerlinState var2) {
      this.h.a.add(var1);
      this.h.b.add(new FieldForm.NoiseState(var1, false, var2, null, 1.0));
      return this.h.b.size() - 1;
   }

   private static FieldForm.PerlinState a(RandomSource var0, int var1) {
      int[][] var2 = new int[var1][];
      double[] var3 = new double[var1];
      double[] var4 = new double[var1];
      double[] var5 = new double[var1];
      double[] var6 = new double[var1];
      Arrays.fill(var6, 1.0);
      int[] var7 = new int[var1];
      var7[0] = var1 - 1;

      for (int var8 = 1; var8 < var1; var8++) {
         var7[var8] = var1 - 1 - var8;
      }

      for (int var11 : var7) {
         var3[var11] = var0.nextDouble() * 256.0;
         var4[var11] = var0.nextDouble() * 256.0;
         var5[var11] = var0.nextDouble() * 256.0;
         int[] var12 = new int[256];
         int var13 = 0;

         while (var13 < 256) {
            var12[var13] = var13++;
         }

         for (int var17 = 0; var17 < 256; var17++) {
            int var14 = var0.nextInt(256 - var17);
            int var15 = var12[var17];
            var12[var17] = var12[var17 + var14];
            var12[var17 + var14] = var15;
         }

         var2[var11] = var12;
      }

      return new FieldForm.PerlinState(Math.pow(2.0, -(var1 - 1)), 1.0, var6, var2, var3, var4, var5);
   }

   private void c(DensityFunction var1) {
      try {
         a(var1).mapAll(new Visitor() {
            public DensityFunction apply(DensityFunction var1) {
               return var1;
            }

            public NoiseHolder visitNoise(NoiseHolder var1) {
               if (var1.noise() != null) {
                  var1.noiseData().unwrapKey().ifPresent(var2 -> FieldFormBuilder.this.m.putIfAbsent(var2.location().toString(), var1));
               }

               return var1;
            }
         });
      } catch (RuntimeException var3) {
      }
   }

   private static boolean a(FieldForm.NoiseState var0, NoiseHolder var1) {
      for (double[] var5 : n) {
         double var6 = FormEval.a(var0, var5[0], var5[1], var5[2]);
         double var8 = var1.getValue(var5[0], var5[1], var5[2]);
         if (!(Math.abs(var6 - var8) <= 1.0E-9)) {
            return false;
         }
      }

      return true;
   }

   private static List<String> b(String var0) {
      int var1 = var0.indexOf(58);
      String var2 = var1 < 0 ? "minecraft" : var0.substring(0, var1);
      String var3 = var1 < 0 ? var0 : var0.substring(var1 + 1);
      String var4 = var3.contains("/") ? var3.substring(var3.lastIndexOf(47) + 1) : var3;
      return List.of("minecraft:" + var4, var2 + ":" + var4);
   }

   private FieldForm.Node b(JsonObject var1) {
      JsonArray var2 = var1.getAsJsonArray("thresholds");
      JsonArray var3 = var1.getAsJsonArray("functions");
      if (var2 != null && var3 != null && var3.size() == var2.size() + 1) {
         double[] var4 = new double[var2.size()];

         for (int var5 = 0; var5 < var4.length; var5++) {
            var4[var5] = var2.get(var5).getAsDouble();
         }

         FieldForm.Node[] var7 = new FieldForm.Node[var3.size()];

         for (int var6 = 0; var6 < var7.length; var6++) {
            var7[var6] = this.a(var3.get(var6));
         }

         return new FieldForm.IntervalSelect(this.a(var1.get("input")), var4, var7);
      } else {
         throw new FieldFormBuilder.Unsupported(
            "interval_select with " + (var2 == null ? "no" : var2.size()) + " thresholds and " + (var3 == null ? "no" : var3.size()) + " functions"
         );
      }
   }

   private int c(String var1) {
      Integer var2 = this.g.get(var1);
      if (var2 != null) {
         return var2;
      }

      Registry var3 = this.b.registryOrThrow(Registries.NOISE);
      ResourceLocation var4 = ResourceLocation.tryParse(var1);
      NoiseParameters var5 = var4 == null ? null : (NoiseParameters)var3.get(var4);
      if (var5 == null) {
         throw new FieldFormBuilder.Unsupported("unresolvable noise: " + var1);
      }

      FieldForm.NoiseState var6 = a(var1, this.c.fromHashOf(var4), var5);
      NoiseHolder var7 = this.m.get(var1);
      if (var7 != null && var7.noise() != null && !a(var6, var7)) {
         FieldForm.NoiseState var8 = null;

         for (String var10 : b(var1)) {
            ResourceLocation var11 = ResourceLocation.tryParse(var10);
            if (var11 != null) {
               FieldForm.NoiseState var12 = a(var1, this.c.fromHashOf(var11), var5);
               if (a(var12, var7)) {
                  var8 = var12;
                  break;
               }
            }
         }

         if (var8 == null) {
            throw new FieldFormBuilder.Unsupported("noise " + var1 + " is seeded by a scheme we cannot reproduce");
         }

         var6 = var8;
      }

      this.h.a.add(var1);
      this.h.b.add(var6);
      int var13 = this.h.b.size() - 1;
      this.g.put(var1, var13);
      return var13;
   }

   private static FieldForm.NoiseState a(String var0, RandomSource var1, NoiseParameters var2) {
      int var3 = var2.firstOctave();
      DoubleList var4 = var2.amplitudes();
      FieldForm.PerlinState var5 = a(var1, var3, var4);
      FieldForm.PerlinState var6 = a(var1, var3, var4);
      int var7 = Integer.MAX_VALUE;
      int var8 = Integer.MIN_VALUE;

      for (int var9 = 0; var9 < var4.size(); var9++) {
         if (var4.getDouble(var9) != 0.0) {
            var7 = Math.min(var7, var9);
            var8 = Math.max(var8, var9);
         }
      }

      double var13 = 0.1 * (1.0 + 1.0 / (var8 - var7 + 1));
      double var11 = 0.16666666666666666 / var13;
      return new FieldForm.NoiseState(var0, true, var5, var6, var11);
   }

   private static FieldForm.PerlinState a(RandomSource var0, int var1, DoubleList var2) {
      PositionalRandomFactory var3 = var0.forkPositional();
      int var4 = var2.size();
      double[] var5 = var2.toDoubleArray();
      int[][] var6 = new int[var4][];
      double[] var7 = new double[var4];
      double[] var8 = new double[var4];
      double[] var9 = new double[var4];

      for (int var10 = 0; var10 < var4; var10++) {
         if (var5[var10] != 0.0) {
            RandomSource var11 = var3.fromHashOf("octave_" + (var1 + var10));
            var7[var10] = var11.nextDouble() * 256.0;
            var8[var10] = var11.nextDouble() * 256.0;
            var9[var10] = var11.nextDouble() * 256.0;
            int[] var12 = new int[256];
            int var13 = 0;

            while (var13 < 256) {
               var12[var13] = var13++;
            }

            for (int var18 = 0; var18 < 256; var18++) {
               int var14 = var11.nextInt(256 - var18);
               int var15 = var12[var18];
               var12[var18] = var12[var18 + var14];
               var12[var18 + var14] = var15;
            }

            var6[var10] = var12;
         }
      }

      double var16 = Math.pow(2.0, var1);
      double var17 = Math.pow(2.0, var4 - 1) / (Math.pow(2.0, var4) - 1.0);
      return new FieldForm.PerlinState(var16, var17, var5, var6, var7, var8, var9);
   }

   private static final class Ctx implements FunctionContext {
      int a;
      int b;
      int c;

      public int blockX() {
         return this.a;
      }

      public int blockY() {
         return this.b;
      }

      public int blockZ() {
         return this.c;
      }
   }

   public static final class Unsupported extends RuntimeException {
      public Unsupported(String var1) {
         super(var1);
      }
   }
}
