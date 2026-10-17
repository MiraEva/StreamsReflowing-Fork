package dev.streamsreflowing.worldgen.regionprep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

public final class FormEval {
   private static final double g = 1.0181268882175227;
   private static final double h = 3.3554432E7;
   private static final int[][] i = new int[][]{
      {1, 1, 0},
      {-1, 1, 0},
      {1, -1, 0},
      {-1, -1, 0},
      {1, 0, 1},
      {-1, 0, 1},
      {1, 0, -1},
      {-1, 0, -1},
      {0, 1, 1},
      {0, -1, 1},
      {0, 1, -1},
      {0, -1, -1},
      {1, 1, 0},
      {0, -1, 1},
      {-1, 1, 0},
      {0, -1, -1}
   };
   private final FieldForm j;
   private final IdentityHashMap<FieldForm.Node, Integer> k = new IdentityHashMap<>();
   private final FieldForm.Node[] l;
   private final int[][] m;
   private final boolean[] n;
   private final FormEval.CSpline[] o;
   private final int p;
   private static final int[] q = new int[0];
   private IdentityHashMap<FieldForm.Node, Double> r;
   private boolean s;
   private double[] t;
   private int[] u;
   private int v;
   private double[][] w;
   private List<FieldForm.Interp> x;
   private final double[] y;
   private final int[] z;
   private int A = 1;
   private boolean B;
   private boolean C;
   static volatile double a = Double.POSITIVE_INFINITY;
   static volatile double b = 0.0;
   static volatile double c = 0.0;
   boolean d = true;
   boolean e = true;
   public static final LongAdder WALK_BLOCKS = new LongAdder();
   public static final LongAdder WALK_COLUMNS = new LongAdder();
   public static final LongAdder MARK_BLOCKS = new LongAdder();
   public static final LongAdder MARK_COLUMNS = new LongAdder();
   private List<FieldForm.Interp> D;
   private int[] E;
   private int[] F;
   boolean f;

   public FormEval(FieldForm var1) {
      this.j = var1;
       ArrayList<FieldForm.Node> var2 = new ArrayList<>();
      this.p = this.a(var1.root, var2);
      int var3 = var2.size();
      this.l = var2.toArray(new FieldForm.Node[0]);
      this.m = new int[var3][];
      this.o = new FormEval.CSpline[var3];
      IdentityHashMap var4 = new IdentityHashMap();

      for (int var5 = 0; var5 < var3; var5++) {
         this.m[var5] = this.a(this.l[var5]);
         if (this.l[var5] instanceof FieldForm.SplineNode var6) {
            this.o[var5] = this.a(var6.spline(), var4);
         }
      }

      this.n = new boolean[var3];
      byte[] var8 = new byte[var3];

      for (int var9 = 0; var9 < var3; var9++) {
         this.n[var9] = this.a(var9, var8);
      }

      this.y = new double[var3];
      this.z = new int[var3];
      this.t = new double[var3];
      this.u = new int[var3];
   }

   private int a(FieldForm.Node var1, List<FieldForm.Node> var2) {
      Integer var3 = this.k.get(var1);
      if (var3 != null) {
         return var3;
      }

      int var4 = var2.size();
      var2.add(var1);
      this.k.put(var1, var4);
      if (var1 instanceof FieldForm.Bin var5) {
         this.a(var5.l(), var2);
         this.a(var5.r(), var2);
      } else if (var1 instanceof FieldForm.Map1 var6) {
         this.a(var6.in(), var2);
      } else if (var1 instanceof FieldForm.Clamp var7) {
         this.a(var7.in(), var2);
      } else if (var1 instanceof FieldForm.RangeChoice var8) {
         this.a(var8.input(), var2);
         this.a(var8.whenInRange(), var2);
         this.a(var8.whenOutOfRange(), var2);
      } else if (var1 instanceof FieldForm.ShiftedNoise var9) {
         this.a(var9.shiftX(), var2);
         this.a(var9.shiftY(), var2);
         this.a(var9.shiftZ(), var2);
      } else if (var1 instanceof FieldForm.WeirdScaled var10) {
         this.a(var10.input(), var2);
      } else if (var1 instanceof FieldForm.IntervalSelect var11) {
         this.a(var11.input(), var2);

         for (FieldForm.Node var19 : var11.functions()) {
            this.a(var19, var2);
         }
      } else if (var1 instanceof FieldForm.Cache2D var12) {
         this.a(var12.in(), var2);
      } else if (var1 instanceof FieldForm.FlatCache var13) {
         this.a(var13.in(), var2);
      } else if (var1 instanceof FieldForm.Interp var14) {
         this.a(var14.in(), var2);
      } else if (var1 instanceof FieldForm.SplineNode var15) {
         this.a(var15.spline(), var2);
      }

      return var4;
   }

   private void a(FieldForm.Spline var1, List<FieldForm.Node> var2) {
      this.a(var1.coordinate(), var2);

      for (FieldForm.SplineValue var6 : var1.values()) {
         if (var6 instanceof FieldForm.SplineNested var7) {
            this.a(var7.spline(), var2);
         }
      }
   }

   private int[] a(FieldForm.Node var1) {
      if (var1 instanceof FieldForm.Bin var14) {
         return new int[]{this.b(var14.l()), this.b(var14.r())};
      } else if (var1 instanceof FieldForm.Map1 var13) {
         return new int[]{this.b(var13.in())};
      } else if (var1 instanceof FieldForm.Clamp var12) {
         return new int[]{this.b(var12.in())};
      } else if (var1 instanceof FieldForm.RangeChoice var11) {
         return new int[]{this.b(var11.input()), this.b(var11.whenInRange()), this.b(var11.whenOutOfRange())};
      } else if (var1 instanceof FieldForm.ShiftedNoise var10) {
         return new int[]{this.b(var10.shiftX()), this.b(var10.shiftY()), this.b(var10.shiftZ())};
      } else if (var1 instanceof FieldForm.WeirdScaled var9) {
         return new int[]{this.b(var9.input())};
      } else if (!(var1 instanceof FieldForm.IntervalSelect var2)) {
         if (var1 instanceof FieldForm.Cache2D var8) {
            return new int[]{this.b(var8.in())};
         } else if (var1 instanceof FieldForm.FlatCache var7) {
            return new int[]{this.b(var7.in())};
         } else {
            return var1 instanceof FieldForm.Interp var6 ? new int[]{this.b(var6.in())} : q;
         }
      } else {
         FieldForm.Node[] var3 = var2.functions();
         int[] var4 = new int[var3.length + 1];
         var4[0] = this.b(var2.input());

         for (int var5 = 0; var5 < var3.length; var5++) {
            var4[var5 + 1] = this.b(var3[var5]);
         }

         return var4;
      }
   }

   private int b(FieldForm.Node var1) {
      Integer var2 = this.k.get(var1);
      if (var2 == null) {
         throw new IllegalStateException("node reached by evaluation but not by index assignment: " + var1);
      } else {
         return var2;
      }
   }

   private FormEval.CSpline a(FieldForm.Spline var1, IdentityHashMap<FieldForm.Spline, FormEval.CSpline> var2) {
      FormEval.CSpline var3 = (FormEval.CSpline)var2.get(var1);
      if (var3 != null) {
         return var3;
      }

      FieldForm.SplineValue[] var4 = var1.values();
      float[] var5 = new float[var4.length];
      FormEval.CSpline[] var6 = new FormEval.CSpline[var4.length];
      FormEval.CSpline var7 = new FormEval.CSpline(this.b(var1.coordinate()), var1.locations(), var1.derivatives(), var5, var6);
      var2.put(var1, var7);

      for (int var8 = 0; var8 < var4.length; var8++) {
         if (var4[var8] instanceof FieldForm.SplineConst var9) {
            var5[var8] = var9.v();
         } else {
            var6[var8] = this.a(((FieldForm.SplineNested)var4[var8]).spline(), var2);
         }
      }

      return var7;
   }

   private static double a(double var0, double var2, double var4) {
      return var2 + var0 * (var4 - var2);
   }

   public double compute(int var1, int var2, int var3) {
      return this.a(this.p, var1, var2, var3);
   }

   public int interpMarkerCount() {
      return this.a().size();
   }

   private List<FieldForm.Interp> a() {
      if (this.x == null) {
         ArrayList var1 = new ArrayList();
         a(this.j.root, var1, Collections.newSetFromMap(new IdentityHashMap<>()));
         this.x = var1;
      }

      return this.x;
   }

   private static void a(FieldForm.Node var0, List<FieldForm.Interp> var1, Set<FieldForm.Node> var2) {
      if (var0 != null && var2.add(var0)) {
         if (var0 instanceof FieldForm.Interp var3) {
            var1.add(var3);
            a(var3.in(), var1, var2);
         } else if (var0 instanceof FieldForm.Bin var4) {
            a(var4.l(), var1, var2);
            a(var4.r(), var1, var2);
         } else if (var0 instanceof FieldForm.IntervalSelect var5) {
            a(var5.input(), var1, var2);

            for (FieldForm.Node var17 : var5.functions()) {
               a(var17, var1, var2);
            }
         } else if (var0 instanceof FieldForm.Cache2D var6) {
            a(var6.in(), var1, var2);
         } else if (var0 instanceof FieldForm.FlatCache var7) {
            a(var7.in(), var1, var2);
         } else if (var0 instanceof FieldForm.Map1 var8) {
            a(var8.in(), var1, var2);
         } else if (var0 instanceof FieldForm.Clamp var9) {
            a(var9.in(), var1, var2);
         } else if (var0 instanceof FieldForm.RangeChoice var10) {
            a(var10.input(), var1, var2);
            a(var10.whenInRange(), var1, var2);
            a(var10.whenOutOfRange(), var1, var2);
         } else if (var0 instanceof FieldForm.ShiftedNoise var11) {
            a(var11.shiftX(), var1, var2);
            a(var11.shiftY(), var1, var2);
            a(var11.shiftZ(), var1, var2);
         } else if (var0 instanceof FieldForm.WeirdScaled var12) {
            a(var12.input(), var1, var2);
         } else if (var0 instanceof FieldForm.SplineNode var13) {
            a(var13.spline(), var1, var2);
         }
      }
   }

   private static void a(FieldForm.Spline var0, List<FieldForm.Interp> var1, Set<FieldForm.Node> var2) {
      a(var0.coordinate(), var1, var2);

      for (FieldForm.SplineValue var6 : var0.values()) {
         if (var6 instanceof FieldForm.SplineNested var7) {
            a(var7.spline(), var1, var2);
         }
      }
   }

   public IdentityHashMap<FieldForm.Node, Double> traceCompute(int var1, int var2, int var3) {
      this.r = new IdentityHashMap<>();
      this.a(this.p, var1, var2, var3);
      IdentityHashMap var4 = this.r;
      this.r = null;
      return var4;
   }

   public double surfaceHeight(int var1, int var2, int var3, int var4, int var5, int var6, double var7, double[] var9) {
      this.d();
      this.B = true;
      this.C = true;

      try {
         int var10 = Math.floorDiv(var3, var5);
         int var11 = Math.floorDiv(var4 - var3, var5);
         List var12 = this.e ? this.a() : List.of();
         if (var12.isEmpty()) {
            for (int var34 = var11 - 1; var34 >= 0; var34--) {
               for (int var35 = var5 - 1; var35 >= 0; var35--) {
                  int var36 = (var10 + var34) * var5 + var35;
                  WALK_BLOCKS.increment();
                  if (this.a(this.p, var1, var36, var2) > var7) {
                     WALK_COLUMNS.increment();
                     return var36 + 1;
                  }
               }
            }

            WALK_COLUMNS.increment();
            return var3;
         } else {
            int var13 = var12.size();
            int var14 = Math.floorDiv(var1, var6);
            int var15 = Math.floorDiv(var2, var6);
            int var16 = var14 * var6;
            int var17 = var15 * var6;
            double var18 = (double)(var1 - var16) / var6;
            double var20 = (double)(var2 - var17) / var6;
            int[] var22 = new int[]{var16, var16 + var6, var16, var16 + var6};
            int[] var23 = new int[]{var17, var17, var17 + var6, var17 + var6};
            if (this.w == null || this.w.length != var13 * 4 || this.w[0].length != var11 + 1) {
               this.w = new double[var13 * 4][var11 + 1];
            }

            this.a(var12);
            this.b();

            try {
               this.a(var12, this.w, var22, var23, var3, var5, 0, var11);
               this.d();
               return this.a(var12, this.w, var1, var2, var3, var5, var11, var18, var20, var7, 0.0);
            } finally {
               this.c();
            }
         }
      } finally {
         this.B = false;
         this.C = false;
      }
   }

   public double surfaceHeightFromCorners(
      List<FieldForm.Interp> var1, double[][] var2, int var3, int var4, int var5, int var6, int var7, int var8, double var9, double var11
   ) {
      this.d();
      this.B = true;
      this.C = true;
      this.a(var1);
      this.b();

      try {
         int var13 = Math.floorDiv(var6 - var5, var7);
         int var14 = Math.floorDiv(var3, var8) * var8;
         int var15 = Math.floorDiv(var4, var8) * var8;
         double var16 = (double)(var3 - var14) / var8;
         double var18 = (double)(var4 - var15) / var8;
         return this.a(var1, var2, var3, var4, var5, var7, var13, var16, var18, var9, var11);
      } finally {
         this.c();
         this.B = false;
         this.C = false;
      }
   }

   public double[][] markerCorners(List<FieldForm.Interp> var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      this.B = true;
      this.C = true;
      this.a(var1);
      this.b();

      try {
         int var8 = Math.floorDiv(var5 - var4, var6);
         int var9 = Math.floorDiv(var2, var7) * var7;
         int var10 = Math.floorDiv(var3, var7) * var7;
         int[] var11 = new int[]{var9, var9 + var7, var9, var9 + var7};
         int[] var12 = new int[]{var10, var10, var10 + var7, var10 + var7};
         double[][] var13 = new double[var1.size() * 4][var8 + 1];
         this.a(var1, var13, var11, var12, var4, var6, 0, var8);
         return var13;
      } finally {
         this.c();
         this.B = false;
         this.C = false;
      }
   }

   public void refineCorners(List<FieldForm.Interp> var1, double[][] var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      this.B = true;
      this.C = true;
      this.a(var1);
      this.b();

      try {
         int var10 = Math.floorDiv(var3, var7) * var7;
         int var11 = Math.floorDiv(var4, var7) * var7;
         int[] var12 = new int[]{var10, var10 + var7, var10, var10 + var7};
         int[] var13 = new int[]{var11, var11, var11 + var7, var11 + var7};
         this.a(var1, var2, var12, var13, var5, var6, var8, var9);
      } finally {
         this.c();
         this.B = false;
         this.C = false;
      }
   }

   private void a(List<FieldForm.Interp> var1, double[][] var2, int[] var3, int[] var4, int var5, int var6, int var7, int var8) {
      int var9 = Math.floorDiv(var5, var6);
      int var10 = var1.size();
      int[] var11 = this.F;

      for (int var12 = 0; var12 < 4; var12++) {
         this.d();

         for (int var13 = var7; var13 <= var8; var13++) {
            int var14 = (var9 + var13) * var6;

            for (int var15 = 0; var15 < var10; var15++) {
               var2[var15 * 4 + var12][var13] = this.a(var11[var15], var3[var12], var14, var4[var12]);
            }
         }
      }
   }

   private void a(List<FieldForm.Interp> var1) {
      if (var1 != this.D) {
         int var2 = var1.size();
         int[] var3 = new int[var2];
         int[] var4 = new int[var2];

         for (int var5 = 0; var5 < var2; var5++) {
            FieldForm.Interp var6 = (FieldForm.Interp)var1.get(var5);
            Integer var7 = this.k.get(var6);
            Integer var8 = this.k.get(var6.in());
            if (var7 == null || var8 == null) {
               throw new IllegalArgumentException("marker does not belong to this evaluator's tree");
            }

            var3[var5] = var7;
            var4[var5] = var8;
         }

         this.E = var3;
         this.F = var4;
         this.D = var1;
      }
   }

   private void b() {
      if (this.v == Integer.MAX_VALUE) {
         Arrays.fill(this.u, 0);
         this.v = 1;
      } else {
         this.v++;
      }

      this.s = true;
   }

   private void c() {
      this.s = false;
   }

   private double a(
      List<FieldForm.Interp> var1, double[][] var2, int var3, int var4, int var5, int var6, int var7, double var8, double var10, double var12, double var14
   ) {
      int var16 = Math.floorDiv(var5, var6);
      int var17 = var1.size();
      int[] var18 = this.E;

      for (int var19 = var7 - 1; var19 >= 0; var19--) {
         for (int var20 = var6 - 1; var20 >= 0; var20--) {
            double var21 = (double)var20 / var6;

            for (int var23 = 0; var23 < var17; var23++) {
               double var24 = a(var21, var2[var23 * 4][var19], var2[var23 * 4][var19 + 1]);
               double var26 = a(var21, var2[var23 * 4 + 1][var19], var2[var23 * 4 + 1][var19 + 1]);
               double var28 = a(var21, var2[var23 * 4 + 2][var19], var2[var23 * 4 + 2][var19 + 1]);
               double var30 = a(var21, var2[var23 * 4 + 3][var19], var2[var23 * 4 + 3][var19 + 1]);
               int var32 = var18[var23];
               this.t[var32] = a(var10, a(var8, var24, var26), a(var8, var28, var30)) + var14;
               this.u[var32] = this.v;
            }

            int var33 = (var16 + var19) * var6 + var20;
            MARK_BLOCKS.increment();
            if (this.a(this.p, var3, var33, var4) > var12) {
               MARK_COLUMNS.increment();
               return var33 + 1;
            }
         }
      }

      MARK_COLUMNS.increment();
      return var5;
   }

   private boolean a(int var1, byte[] var2) {
      byte var3 = var2[var1];
      if (var3 != 0) {
         return var3 == 1;
      }

      FieldForm.Node var4 = this.l[var1];
      int[] var5 = this.m[var1];
      boolean var6;
      if (var4 instanceof FieldForm.Extern) {
         var6 = false;
      } else if (var4 instanceof FieldForm.Cache2D) {
         var6 = this.a(var5[0], var2);
      } else if (var4 instanceof FieldForm.IntervalSelect) {
         var6 = false;
      } else if (var4 instanceof FieldForm.FlatCache) {
         var6 = this.a(var5[0], var2);
      } else if (var4 instanceof FieldForm.NoiseNode var7) {
         var6 = var7.yScale() == 0.0;
      } else if (var4 instanceof FieldForm.ShiftNoise var8) {
         var6 = var8.kind() != FieldForm.ShiftKind.SHIFT;
      } else if (var4 instanceof FieldForm.ShiftedNoise var9) {
         var6 = var9.yScale() == 0.0 && this.a(var5[0], var2) && this.a(var5[1], var2) && this.a(var5[2], var2);
      } else if (var4 instanceof FieldForm.Bin) {
         var6 = this.a(var5[0], var2) && this.a(var5[1], var2);
      } else if (var4 instanceof FieldForm.Map1) {
         var6 = this.a(var5[0], var2);
      } else if (var4 instanceof FieldForm.Clamp) {
         var6 = this.a(var5[0], var2);
      } else if (var4 instanceof FieldForm.RangeChoice) {
         var6 = this.a(var5[0], var2) && this.a(var5[1], var2) && this.a(var5[2], var2);
      } else if (var4 instanceof FieldForm.SplineNode) {
         var6 = this.a(this.o[var1], var2);
      } else if (var4 instanceof FieldForm.Const) {
         var6 = true;
      } else if (var4 instanceof FieldForm.ZCoord) {
         var6 = true;
      } else {
         var6 = false;
      }

      var2[var1] = (byte)(var6 ? 1 : 2);
      return var6;
   }

   private boolean a(FormEval.CSpline var1, byte[] var2) {
      if (!this.a(var1.a, var2)) {
         return false;
      }

      for (FormEval.CSpline var6 : var1.e) {
         if (var6 != null && !this.a(var6, var2)) {
            return false;
         }
      }

      return true;
   }

   private void d() {
      if (this.A == Integer.MAX_VALUE) {
         Arrays.fill(this.z, 0);
         this.A = 1;
      } else {
         this.A++;
      }
   }

   private double a(int var1, int var2, int var3, int var4) {
      double var5;
      if (this.B && this.n[var1]) {
         if (this.z[var1] == this.A) {
            var5 = this.y[var1];
            return this.f ? (float)var5 : var5;
         }

         var5 = this.b(var1, var2, var3, var4);
         this.y[var1] = var5;
         this.z[var1] = this.A;
      } else {
         var5 = this.b(var1, var2, var3, var4);
      }

      if (this.r != null) {
         this.r.put(this.l[var1], var5);
      }

      return this.f ? (float)var5 : var5;
   }

   private double b(int var1, int var2, int var3, int var4) {
      FieldForm.Node var5 = this.l[var1];
      int[] var6 = this.m[var1];
      if (var5 instanceof FieldForm.Extern var25) {
         return var25.fn().at(var2, var3, var4);
      } else if (var5 instanceof FieldForm.Const var24) {
         return var24.v();
      } else if (var5 instanceof FieldForm.YGradient var23) {
         double var36 = b(((double)var3 - var23.fromY()) / ((double)var23.toY() - var23.fromY()), 0.0, 1.0);
         return var23.fromV() + var36 * (var23.toV() - var23.fromV());
      } else if (var5 instanceof FieldForm.Bin var22) {
         double var35 = this.a(var6[0], var2, var3, var4);
         double var41 = this.a(var6[1], var2, var3, var4);

         return switch (var22.op()) {
            case ADD -> var35 + var41;
            case MUL -> var35 * var41;
            case MIN -> Math.min(var35, var41);
            case MAX -> Math.max(var35, var41);
            case DIV -> var41 == 0.0 ? 0.0 : var35 / var41;
         };
      } else if (var5 instanceof FieldForm.Map1 var21) {
         double var34 = this.a(var6[0], var2, var3, var4);

         return switch (var21.op()) {
            case ABS -> Math.abs(var34);
            case SQUARE -> var34 * var34;
            case CUBE -> var34 * var34 * var34;
            case HALF_NEGATIVE -> var34 > 0.0 ? var34 : var34 * 0.5;
            case QUARTER_NEGATIVE -> var34 > 0.0 ? var34 : var34 * 0.25;
            case RECIPROCAL -> {
               double var40 = Math.abs(var34);
               if (var40 < a) {
                  a = var40;
               }

               double var44 = 1.0 / var34;
               double var14 = Math.abs(var44);
               if (var14 > b) {
                  b = var14;
               }

               float var16 = (float)var34;
               double var17 = var16 == 0.0F ? Double.POSITIVE_INFINITY : Math.abs((1.0 / var16 - var44) / (var44 == 0.0 ? 1.0 : var44));
               if (var17 > c) {
                  c = var17;
               }

               yield var44;
            }
            case SQUEEZE -> {
               double var10 = b(var34, -1.0, 1.0);
               yield var10 / 2.0 - var10 * var10 * var10 / 24.0;
            }
            case SIGNUM -> Math.signum(var34);
            case SIN -> Math.sin(var34);
         };
      } else if (var5 instanceof FieldForm.Clamp var20) {
         return b(this.a(var6[0], var2, var3, var4), var20.min(), var20.max());
      } else if (var5 instanceof FieldForm.RangeChoice var19) {
         double var33 = this.a(var6[0], var2, var3, var4);
         return var33 >= var19.minInclusive() && var33 < var19.maxExclusive() ? this.a(var6[1], var2, var3, var4) : this.a(var6[2], var2, var3, var4);
      } else {
         boolean var7 = this.f;
         if (var5 instanceof FieldForm.NoiseNode var32) {
            return a(
               this.j.noises.get(var32.noiseIndex()), b(var2 * var32.xzScale(), var7), b(var3 * var32.yScale(), var7), b(var4 * var32.xzScale(), var7), var7
            );
         } else if (var5 instanceof FieldForm.ShiftNoise var31) {
            FieldForm.NoiseState var39 = this.j.noises.get(var31.noiseIndex());

            return switch (var31.kind()) {
               case SHIFT_A -> b(a(var39, b(var2 * 0.25, var7), 0.0, b(var4 * 0.25, var7), var7) * 4.0, var7);
               case SHIFT_B -> b(a(var39, b(var4 * 0.25, var7), b(var2 * 0.25, var7), 0.0, var7) * 4.0, var7);
               case SHIFT -> b(a(var39, b(var2 * 0.25, var7), b(var3 * 0.25, var7), b(var4 * 0.25, var7), var7) * 4.0, var7);
            };
         } else if (var5 instanceof FieldForm.ShiftedNoise var30) {
            double var38 = b(b(var2 * var30.xzScale(), var7) + this.a(var6[0], var2, var3, var4), var7);
            double var43 = b(b(var3 * var30.yScale(), var7) + this.a(var6[1], var2, var3, var4), var7);
            double var13 = b(b(var4 * var30.xzScale(), var7) + this.a(var6[2], var2, var3, var4), var7);
            return a(this.j.noises.get(var30.noiseIndex()), var38, var43, var13, var7);
         } else if (var5 instanceof FieldForm.WeirdScaled var29) {
            double var37 = this.a(var6[0], var2, var3, var4);
            double var42 = var29.rarity() == FieldForm.RarityKind.TYPE1 ? b(var37) : c(var37);
            return b(
               var42 * Math.abs(a(this.j.noises.get(var29.noiseIndex()), b(var2 / var42, var7), b(var3 / var42, var7), b(var4 / var42, var7), var7)), var7
            );
         } else if (var5 instanceof FieldForm.IntervalSelect var28) {
            double var9 = this.a(var6[0], var2, var3, var4);
            double[] var11 = var28.thresholds();

            for (int var12 = 0; var12 < var11.length; var12++) {
               if (var9 < var11[var12]) {
                  return this.a(var6[1 + var12], var2, var3, var4);
               }
            }

            return this.a(var6[var6.length - 1], var2, var3, var4);
         } else if (var5 instanceof FieldForm.Cache2D var27) {
            return this.a(var6[0], var2, var3, var4);
         } else if (!(var5 instanceof FieldForm.FlatCache var8)) {
            if (var5 instanceof FieldForm.Interp) {
               return this.s && this.u[var1] == this.v ? this.t[var1] : this.a(var6[0], var2, var3, var4);
            } else if (var5 instanceof FieldForm.Blended var26) {
               return this.a(var26, var2, var3, var4);
            } else {
               return var5 instanceof FieldForm.ZCoord ? b(var4, -3.0E7, 3.0E7) : this.a(this.o[var1], var2, var3, var4);
            }
         } else {
            return this.C && this.d ? this.a(var6[0], var2 & -4, 0, var4 & -4) : this.a(var6[0], var2, var3, var4);
         }
      }
   }

   private double a(FieldForm.Blended var1, int var2, int var3, int var4) {
      FieldForm.PerlinState var5 = this.j.noises.get(var1.main()).first;
      FieldForm.PerlinState var6 = this.j.noises.get(var1.minLimit()).first;
      FieldForm.PerlinState var7 = this.j.noises.get(var1.maxLimit()).first;
      boolean var8 = this.f;
      double var9 = b(var2 * var1.xzMultiplier(), var8);
      double var11 = b(var3 * var1.yMultiplier(), var8);
      double var13 = b(var4 * var1.xzMultiplier(), var8);
      double var15 = b(var9 / var1.xzFactor(), var8);
      double var17 = b(var11 / var1.yFactor(), var8);
      double var19 = b(var13 / var1.xzFactor(), var8);
      double var21 = b(var1.yMultiplier() * var1.smearScaleMultiplier(), var8);
      double var23 = b(var21 / var1.yFactor(), var8);
      double var25 = 0.0;
      double var27 = 1.0;

      for (int var29 = 0; var29 < var5.perm.length; var29++) {
         int var30 = var5.perm.length - 1 - var29;
         if (var5.perm[var30] != null) {
            var25 = b(
               var25
                  + b(
                     a(
                           var5,
                           var30,
                           a(b(var15 * var27, var8), var8),
                           a(b(var17 * var27, var8), var8),
                           a(b(var19 * var27, var8), var8),
                           b(var23 * var27, var8),
                           b(var17 * var27, var8),
                           var8
                        )
                        / var27,
                     var8
                  ),
               var8
            );
         }

         var27 /= 2.0;
      }

      double var48 = b(b(b(var25 / 10.0, var8) + 1.0, var8) / 2.0, var8);
      boolean var31 = var48 >= 1.0;
      boolean var32 = var48 <= 0.0;
      double var33 = 0.0;
      double var35 = 0.0;
      var27 = 1.0;

      for (int var37 = 0; var37 < var6.perm.length; var37++) {
         int var38 = var6.perm.length - 1 - var37;
         double var39 = a(b(var9 * var27, var8), var8);
         double var41 = a(b(var11 * var27, var8), var8);
         double var43 = a(b(var13 * var27, var8), var8);
         double var45 = b(var21 * var27, var8);
         if (!var31 && var6.perm[var38] != null) {
            var33 = b(var33 + b(a(var6, var38, var39, var41, var43, var45, b(var11 * var27, var8), var8) / var27, var8), var8);
         }

         if (!var32 && var7.perm[var38] != null) {
            var35 = b(var35 + b(a(var7, var38, var39, var41, var43, var45, b(var11 * var27, var8), var8) / var27, var8), var8);
         }

         var27 /= 2.0;
      }

      double var49 = b(var48, 0.0, 1.0);
      double var50 = b(var33 / 512.0, var8);
      double var51 = b(var35 / 512.0, var8);
      return b(b(var50 + b(var49 * b(var51 - var50, var8), var8), var8) / 128.0, var8);
   }

   private static double a(FieldForm.PerlinState var0, int var1, double var2, double var4, double var6, double var8, double var10, boolean var12) {
      double var13 = b(var2 + var0.xo[var1], var12);
      double var15 = b(var4 + var0.yo[var1], var12);
      double var17 = b(var6 + var0.zo[var1], var12);
      int var19 = e(var13);
      int var20 = e(var15);
      int var21 = e(var17);
      double var22 = b(var13 - var19, var12);
      double var24 = b(var15 - var20, var12);
      double var26 = b(var17 - var21, var12);
      double var28 = 0.0;
      if (var8 != 0.0) {
         double var30 = var10 >= 0.0 && var10 < var24 ? var10 : var24;
         var28 = b((var12 ? Math.floor(b(b(var30 / var8, var12) + 1.0E-7, var12)) : d(var30 / var8 + 1.0E-7)) * var8, var12);
      }

      return a(var0.perm[var1], var19, var20, var21, var22, b(var24 - var28, var12), var26, var24, var12);
   }

   private static double a(int[] var0, int var1, int var2, int var3, double var4, double var6, double var8, double var10, boolean var12) {
      int var13 = var0[var1 & 0xFF];
      int var14 = var0[var1 + 1 & 0xFF];
      int var15 = var0[var13 + var2 & 0xFF];
      int var16 = var0[var13 + var2 + 1 & 0xFF];
      int var17 = var0[var14 + var2 & 0xFF];
      int var18 = var0[var14 + var2 + 1 & 0xFF];
      double var19 = a(var0[var15 + var3 & 0xFF], var4, var6, var8, var12);
      double var21 = a(var0[var17 + var3 & 0xFF], b(var4 - 1.0, var12), var6, var8, var12);
      double var23 = a(var0[var16 + var3 & 0xFF], var4, b(var6 - 1.0, var12), var8, var12);
      double var25 = a(var0[var18 + var3 & 0xFF], b(var4 - 1.0, var12), b(var6 - 1.0, var12), var8, var12);
      double var27 = a(var0[var15 + var3 + 1 & 0xFF], var4, var6, b(var8 - 1.0, var12), var12);
      double var29 = a(var0[var17 + var3 + 1 & 0xFF], b(var4 - 1.0, var12), var6, b(var8 - 1.0, var12), var12);
      double var31 = a(var0[var16 + var3 + 1 & 0xFF], var4, b(var6 - 1.0, var12), b(var8 - 1.0, var12), var12);
      double var33 = a(var0[var18 + var3 + 1 & 0xFF], b(var4 - 1.0, var12), b(var6 - 1.0, var12), b(var8 - 1.0, var12), var12);
      double var35 = c(var4, var12);
      double var37 = c(var10, var12);
      double var39 = c(var8, var12);
      return a(var35, var37, var39, var19, var21, var23, var25, var27, var29, var31, var33, var12);
   }

   private static double b(double var0) {
      if (var0 < -0.5) {
         return 0.75;
      } else if (var0 < 0.0) {
         return 1.0;
      } else {
         return var0 < 0.5 ? 1.5 : 2.0;
      }
   }

   private static double c(double var0) {
      if (var0 < -0.75) {
         return 0.5;
      } else if (var0 < -0.5) {
         return 0.75;
      } else if (var0 < 0.5) {
         return 1.0;
      } else {
         return var0 < 0.75 ? 2.0 : 3.0;
      }
   }

   private double a(FormEval.CSpline var1, int var2, int var3, int var4) {
      float var5 = (float)this.a(var1.a, var2, var3, var4);
      float[] var6 = var1.b;
      int var7 = var6.length;
      int var8 = a(var6, var5);
      if (var8 < 0) {
         return this.a(var1, 0, var2, var3, var4) + var1.c[0] * (var5 - var6[0]);
      }

      if (var8 == var7 - 1) {
         return this.a(var1, var7 - 1, var2, var3, var4) + var1.c[var7 - 1] * (var5 - var6[var7 - 1]);
      }

      float var9 = var6[var8];
      float var10 = var6[var8 + 1];
      float var11 = (var5 - var9) / (var10 - var9);
      float var12 = this.a(var1, var8, var2, var3, var4);
      float var13 = this.a(var1, var8 + 1, var2, var3, var4);
      float var14 = var1.c[var8];
      float var15 = var1.c[var8 + 1];
      float var16 = var10 - var9;
      float var17 = var14 * var16 - (var13 - var12);
      float var18 = -var15 * var16 + (var13 - var12);
      return a(var11, var12, var13) + var11 * (1.0F - var11) * a(var11, var17, var18);
   }

   static int a(float[] var0, float var1) {
      int var2 = 0;
      int var3 = var0.length;

      while (var3 > 0) {
         int var4 = var3 / 2;
         int var5 = var2 + var4;
         if (var1 < var0[var5]) {
            var3 = var4;
         } else {
            var2 = var5 + 1;
            var3 -= var4 + 1;
         }
      }

      return var2 - 1;
   }

   private float a(FormEval.CSpline var1, int var2, int var3, int var4, int var5) {
      FormEval.CSpline var6 = var1.e[var2];
      return var6 == null ? var1.d[var2] : (float)this.a(var6, var3, var4, var5);
   }

   private static float a(float var0, float var1, float var2) {
      return var1 + var0 * (var2 - var1);
   }

   static double a(FieldForm.NoiseState var0, double var1, double var3, double var5) {
      return a(var0, var1, var3, var5, false);
   }

   static double a(FieldForm.NoiseState var0, double var1, double var3, double var5, boolean var7) {
      double var8 = a(var0.first, b(var1, var7), b(var3, var7), b(var5, var7), var7);
      if (!var0.normal) {
         return var8;
      }

      double var10 = a(var0.second, b(var1 * 1.0181268882175227, var7), b(var3 * 1.0181268882175227, var7), b(var5 * 1.0181268882175227, var7), var7);
      return b(b(var8 + var10, var7) * var0.valueFactor, var7);
   }

   private static double b(double var0, boolean var2) {
      return var2 ? (float)var0 : var0;
   }

   private static double a(FieldForm.PerlinState var0, double var1, double var3, double var5, boolean var7) {
      double var8 = var0.lowestFreqInputFactor;
      double var10 = var0.lowestFreqValueFactor;
      double var12 = 0.0;

      for (int var14 = 0; var14 < var0.amplitudes.length; var14++) {
         if (var0.perm[var14] != null) {
            var12 = b(
               var12
                  + b(
                     b(var0.amplitudes[var14] * var10, var7)
                        * a(
                           var0.perm[var14],
                           var0.xo[var14],
                           var0.yo[var14],
                           var0.zo[var14],
                           a(b(var1 * var8, var7), var7),
                           a(b(var3 * var8, var7), var7),
                           a(b(var5 * var8, var7), var7),
                           var7
                        ),
                     var7
                  ),
               var7
            );
         }

         var8 *= 2.0;
         var10 /= 2.0;
      }

      return var12;
   }

   static double a(double var0) {
      return a(var0, false);
   }

   static double a(double var0, boolean var2) {
      return b(var0 - (var2 ? Math.floor(b(b(var0 / 3.3554432E7, var2) + 0.5, var2)) : d(var0 / 3.3554432E7 + 0.5)) * 3.3554432E7, var2);
   }

   private static long d(double var0) {
      long var2 = (long)var0;
      return var0 < var2 ? var2 - 1L : var2;
   }

   private static double a(int[] var0, double var1, double var3, double var5, double var7, double var9, double var11, boolean var13) {
      double var14 = b(var7 + var1, var13);
      double var16 = b(var9 + var3, var13);
      double var18 = b(var11 + var5, var13);
      int var20 = e(var14);
      int var21 = e(var16);
      int var22 = e(var18);
      double var23 = b(var14 - var20, var13);
      double var25 = b(var16 - var21, var13);
      double var27 = b(var18 - var22, var13);
      int var29 = var0[var20 & 0xFF];
      int var30 = var0[var20 + 1 & 0xFF];
      int var31 = var0[var29 + var21 & 0xFF];
      int var32 = var0[var29 + var21 + 1 & 0xFF];
      int var33 = var0[var30 + var21 & 0xFF];
      int var34 = var0[var30 + var21 + 1 & 0xFF];
      double var35 = a(var0[var31 + var22 & 0xFF], var23, var25, var27, var13);
      double var37 = a(var0[var33 + var22 & 0xFF], b(var23 - 1.0, var13), var25, var27, var13);
      double var39 = a(var0[var32 + var22 & 0xFF], var23, b(var25 - 1.0, var13), var27, var13);
      double var41 = a(var0[var34 + var22 & 0xFF], b(var23 - 1.0, var13), b(var25 - 1.0, var13), var27, var13);
      double var43 = a(var0[var31 + var22 + 1 & 0xFF], var23, var25, b(var27 - 1.0, var13), var13);
      double var45 = a(var0[var33 + var22 + 1 & 0xFF], b(var23 - 1.0, var13), var25, b(var27 - 1.0, var13), var13);
      double var47 = a(var0[var32 + var22 + 1 & 0xFF], var23, b(var25 - 1.0, var13), b(var27 - 1.0, var13), var13);
      double var49 = a(var0[var34 + var22 + 1 & 0xFF], b(var23 - 1.0, var13), b(var25 - 1.0, var13), b(var27 - 1.0, var13), var13);
      double var51 = c(var23, var13);
      double var53 = c(var25, var13);
      double var55 = c(var27, var13);
      return a(var51, var53, var55, var35, var37, var39, var41, var43, var45, var47, var49, var13);
   }

   private static int e(double var0) {
      int var2 = (int)var0;
      return var0 < var2 ? var2 - 1 : var2;
   }

   private static double a(int var0, double var1, double var3, double var5, boolean var7) {
      int[] var8 = i[var0 & 15];
      return b(b(b(var8[0] * var1, var7) + b(var8[1] * var3, var7), var7) + b(var8[2] * var5, var7), var7);
   }

   private static double c(double var0, boolean var2) {
      return b(b(b(b(var0 * var0, var2) * var0, var2) * b(b(var0 * b(b(var0 * 6.0, var2) - 15.0, var2), var2) + 10.0, var2), var2), var2);
   }

   private static double a(double var0, double var2, double var4, boolean var6) {
      return var6 ? b(b(var0 * b(1.0 - var4, var6), var6) + b(var2 * var4, var6), var6) : var0 + var4 * (var2 - var0);
   }

   private static double a(
      double var0,
      double var2,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      double var16,
      double var18,
      double var20,
      boolean var22
   ) {
      double var23 = a(var6, var8, var0, var22);
      double var25 = a(var10, var12, var0, var22);
      double var27 = a(var14, var16, var0, var22);
      double var29 = a(var18, var20, var0, var22);
      double var31 = a(var23, var25, var2, var22);
      double var33 = a(var27, var29, var2, var22);
      return a(var31, var33, var4, var22);
   }

   private static double b(double var0, double var2, double var4) {
      return var0 < var2 ? var2 : Math.min(var0, var4);
   }

   private static final class CSpline {
      final int a;
      final float[] b;
      final float[] c;
      final float[] d;
      final FormEval.CSpline[] e;

      CSpline(int var1, float[] var2, float[] var3, float[] var4, FormEval.CSpline[] var5) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.e = var5;
      }
   }
}
