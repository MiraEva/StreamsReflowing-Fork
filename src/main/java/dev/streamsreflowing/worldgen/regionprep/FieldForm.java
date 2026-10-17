package dev.streamsreflowing.worldgen.regionprep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FieldForm {
   public final FieldForm.Node root;
   public final List<FieldForm.NoiseState> noises;
   public final int minY;
   public final int maxY;
   public final int cellHeight;

   public FieldForm(FieldForm.Node var1, List<FieldForm.NoiseState> var2, int var3, int var4, int var5) {
      this.root = var1;
      this.noises = var2;
      this.minY = var3;
      this.maxY = var4;
      this.cellHeight = var5;
   }

   public record Bin(FieldForm.BinOp op, FieldForm.Node l, FieldForm.Node r) implements FieldForm.Node {

      public Bin {
l = l;
}
   }

   public enum BinOp {
      ADD,
      MUL,
      MIN,
      MAX,
      DIV;
   }

   public record Blended(int minLimit, int maxLimit, int main, double xzMultiplier, double yMultiplier, double xzFactor, double yFactor, double smearScaleMultiplier) implements FieldForm.Node {

      public Blended {
maxLimit = maxLimit;
xzMultiplier = xzMultiplier;
xzFactor = xzFactor;
smearScaleMultiplier = smearScaleMultiplier;
}
   }

   public record Cache2D(FieldForm.Node in) implements FieldForm.Node {

      public Cache2D {
}
   }

   public record Clamp(FieldForm.Node in, double min, double max) implements FieldForm.Node {

      public Clamp {
min = min;
}
   }

   public record Const(double v) implements FieldForm.Node {

      public Const {
}
   }

   public record Extern(FieldForm.Fn fn, String type) implements FieldForm.Node {

      public Extern {
type = type;
}
   }

   public record FlatCache(FieldForm.Node in) implements FieldForm.Node {

      public FlatCache {
}
   }

   @FunctionalInterface
   public interface Fn {
      double at(int var1, int var2, int var3);
   }

   public record Interp(FieldForm.Node in) implements FieldForm.Node {

      public Interp {
}
   }

   public record IntervalSelect(FieldForm.Node input, double[] thresholds, FieldForm.Node[] functions) implements FieldForm.Node {

      public IntervalSelect {
thresholds = thresholds;
}
   }

   public record Map1(FieldForm.MapOp op, FieldForm.Node in) implements FieldForm.Node {

      public Map1 {
in = in;
}
   }

   public enum MapOp {
      ABS,
      SQUARE,
      CUBE,
      HALF_NEGATIVE,
      QUARTER_NEGATIVE,
      SQUEEZE,
      RECIPROCAL,
      SIGNUM,
      SIN;
   }

   public sealed interface Node
      permits FieldForm.Const,
      FieldForm.YGradient,
      FieldForm.Bin,
      FieldForm.Map1,
      FieldForm.Clamp,
      FieldForm.RangeChoice,
      FieldForm.NoiseNode,
      FieldForm.ShiftedNoise,
      FieldForm.ShiftNoise,
      FieldForm.WeirdScaled,
      FieldForm.Blended,
      FieldForm.SplineNode,
      FieldForm.Interp,
      FieldForm.FlatCache,
      FieldForm.IntervalSelect,
      FieldForm.Cache2D,
      FieldForm.Extern,
      FieldForm.ZCoord {
   }

   public record NoiseNode(int noiseIndex, double xzScale, double yScale) implements FieldForm.Node {

      public NoiseNode {
xzScale = xzScale;
}
   }

   static final class NoiseRegistry {
      final List<Object> a = new ArrayList<>();
      final List<FieldForm.NoiseState> b = new ArrayList<>();

      int a(Object var1, Supplier<FieldForm.NoiseState> var2) {
         for (int var3 = 0; var3 < this.a.size(); var3++) {
            if (this.a.get(var3) == var1) {
               return var3;
            }
         }

         this.a.add(var1);
         this.b.add((FieldForm.NoiseState)var2.get());
         return this.b.size() - 1;
      }
   }

   public static final class NoiseState {
      public final String name;
      public final boolean normal;
      public final FieldForm.PerlinState first;
      public final FieldForm.PerlinState second;
      public final double valueFactor;

      public NoiseState(String var1, boolean var2, FieldForm.PerlinState var3, FieldForm.PerlinState var4, double var5) {
         this.name = var1;
         this.normal = var2;
         this.first = var3;
         this.second = var4;
         this.valueFactor = var5;
      }
   }

   public static final class PerlinState {
      public final double lowestFreqInputFactor;
      public final double lowestFreqValueFactor;
      public final double[] amplitudes;
      public final int[][] perm;
      public final double[] xo;
      public final double[] yo;
      public final double[] zo;

      public PerlinState(double var1, double var3, double[] var5, int[][] var6, double[] var7, double[] var8, double[] var9) {
         this.lowestFreqInputFactor = var1;
         this.lowestFreqValueFactor = var3;
         this.amplitudes = var5;
         this.perm = var6;
         this.xo = var7;
         this.yo = var8;
         this.zo = var9;
      }
   }

   public record RangeChoice(FieldForm.Node input, double minInclusive, double maxExclusive, FieldForm.Node whenInRange, FieldForm.Node whenOutOfRange) implements FieldForm.Node {

      public RangeChoice {
minInclusive = minInclusive;
whenInRange = whenInRange;
}
   }

   public enum RarityKind {
      TYPE1,
      TYPE2;
   }

   public enum ShiftKind {
      SHIFT_A,
      SHIFT_B,
      SHIFT;
   }

   public record ShiftNoise(FieldForm.ShiftKind kind, int noiseIndex) implements FieldForm.Node {

      public ShiftNoise {
noiseIndex = noiseIndex;
}
   }

   public record ShiftedNoise(FieldForm.Node shiftX, FieldForm.Node shiftY, FieldForm.Node shiftZ, double xzScale, double yScale, int noiseIndex) implements FieldForm.Node {

      public ShiftedNoise {
shiftY = shiftY;
xzScale = xzScale;
noiseIndex = noiseIndex;
}
   }

   public record Spline(FieldForm.Node coordinate, float[] locations, FieldForm.SplineValue[] values, float[] derivatives) {

      public Spline {
locations = locations;
derivatives = derivatives;
}
   }

   public record SplineConst(float v) implements FieldForm.SplineValue {

      public SplineConst {
}
   }

   public record SplineNested(FieldForm.Spline spline) implements FieldForm.SplineValue {

      public SplineNested {
}
   }

   public record SplineNode(FieldForm.Spline spline) implements FieldForm.Node {

      public SplineNode {
}
   }

   public sealed interface SplineValue permits FieldForm.SplineConst, FieldForm.SplineNested {
   }

   public record WeirdScaled(FieldForm.Node input, int noiseIndex, FieldForm.RarityKind rarity) implements FieldForm.Node {

      public WeirdScaled {
noiseIndex = noiseIndex;
}
   }

   public record YGradient(int fromY, int toY, double fromV, double toV) implements FieldForm.Node {

      public YGradient {
toY = toY;
toV = toV;
}
   }

   public record ZCoord() implements FieldForm.Node {
   }
}
