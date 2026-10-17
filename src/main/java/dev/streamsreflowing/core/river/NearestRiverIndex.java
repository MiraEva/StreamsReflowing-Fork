package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.math.Hashing;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class NearestRiverIndex {
   private static final double a = 16.0;
   private static final double b = 4.0;
   private static final double c = 6.0;
   private static final double d = 6.0;
   private static final double e = 64.0;
   private final List<RiverSegment> f;
   private final double[] g;
   private final double[] h;
   private final double[] i;
   private final double[] j;
   private final double[] k;
   private final double[] l;
   private final double[] m;
   private final double[] n;
   private final double[] o;
   private final double[] p;
   private final double[] q;
   private final double[] r;
   private final double s;
   private final boolean[] t;
   private final double[] u;
   private final boolean[] v;
   private final double[] w;
   private final int[] x;
   private final double[] y;
   private static final double z = 64.0;
   private final double A;
   private final double[] B;
   private static final double C = 0.04;
   private static final double D = 256.0;
   private final int E;
   private final int[] F;
   private final int G;
   private final int H;
   private final int I;
   private final int J;
   private final int[] K;
   private final long[] L;
   private final int[] M;
   private final int[] N;
   private final int O;
   private static final ThreadLocal<NearestRiverIndex.TieredCand[]> P = ThreadLocal.withInitial(() -> {
      NearestRiverIndex.TieredCand[] var0 = new NearestRiverIndex.TieredCand[8];

      for (int var1 = 0; var1 < var0.length; var1++) {
         var0[var1] = new NearestRiverIndex.TieredCand();
      }

      return var0;
   });
   private static final ThreadLocal<int[]> Q = ThreadLocal.withInitial(() -> new int[1]);
   private static final ThreadLocal<long[]> R = ThreadLocal.withInitial(() -> new long[1]);
   private static final ThreadLocal<int[][]> S = ThreadLocal.withInitial(() -> new int[][]{new int[512]});

   public NearestRiverIndex(List<RiverSegment> var1, double var2, int var4, double var5) {
      this(var1, var2, var4, var5, List.of());
   }

   public NearestRiverIndex(List<RiverSegment> var1, double var2, int var4, double var5, List<Lake> var7) {
      this(var1, var2, var4, var5, var7, false);
   }

   public NearestRiverIndex(List<RiverSegment> var1, double var2, int var4, double var5, List<Lake> var7, boolean var8) {
      this.f = var1;
      this.E = Math.max(1, var4);
      this.s = var5;
      int var9 = var1.size();
      int[] var10 = new int[var9 * 4];
      int var11 = Integer.MAX_VALUE;
      int var12 = Integer.MAX_VALUE;
      int var13 = Integer.MIN_VALUE;
      int var14 = Integer.MIN_VALUE;
      long var15 = 0L;

      for (int var17 = 0; var17 < var9; var17++) {
         RiverSegment var18 = (RiverSegment)var1.get(var17);
         double var19 = Math.max(var18.w1(), var18.w2());
         double var21 = var8 ? var2 : Math.min(var2, var19 + Math.max(5.0, var19 * var5) + 2.0);
         int var23 = this.b(var18.minX() - var21);
         int var24 = this.b(var18.maxX() + var21);
         int var25 = this.c(var18.minZ() - var21);
         int var26 = this.c(var18.maxZ() + var21);
         var10[var17 * 4] = var23;
         var10[var17 * 4 + 1] = var24;
         var10[var17 * 4 + 2] = var25;
         var10[var17 * 4 + 3] = var26;
         var11 = Math.min(var11, var23);
         var13 = Math.max(var13, var24);
         var12 = Math.min(var12, var25);
         var14 = Math.max(var14, var26);
         var15 += (long)(var24 - var23 + 1) * (var26 - var25 + 1);
      }

      if (var15 > 2147483639L) {
         throw new IllegalStateException("river index bucket table too large: " + var15 + " entries");
      }

      long var60 = var9 == 0 ? 0L : (long)(var13 - var11 + 1) * (var14 - var12 + 1);
      boolean var61 = !var8 && var9 > 0 && var60 <= 8000000L && var60 <= 8L * var15;
      this.G = var9 == 0 ? 0 : var11;
      this.H = var9 == 0 ? 0 : var12;
      this.F = new int[(int)var15];
      if (var9 == 0) {
         this.I = 0;
         this.J = 0;
         this.K = null;
         this.L = null;
         this.M = null;
         this.N = null;
         this.O = 0;
      } else if (var61) {
         int var20 = var13 - var11 + 1;
         int var64 = var14 - var12 + 1;
         int[] var22 = new int[var20 * var64 + 1];

         for (int var67 = 0; var67 < var9; var67++) {
            for (int var73 = var10[var67 * 4]; var73 <= var10[var67 * 4 + 1]; var73++) {
               int var78 = (var73 - var11) * var64;

               for (int var83 = var10[var67 * 4 + 2]; var83 <= var10[var67 * 4 + 3]; var83++) {
                  var22[var78 + (var83 - var12) + 1]++;
               }
            }
         }

         for (int var68 = 0; var68 < var20 * var64; var68++) {
            var22[var68 + 1] = var22[var68 + 1] + var22[var68];
         }

         int[] var69 = Arrays.copyOf(var22, var20 * var64);

         for (int var74 = 0; var74 < var9; var74++) {
            for (int var79 = var10[var74 * 4]; var79 <= var10[var74 * 4 + 1]; var79++) {
               int var84 = (var79 - var11) * var64;

               for (int var27 = var10[var74 * 4 + 2]; var27 <= var10[var74 * 4 + 3]; var27++) {
                  this.F[var69[var84 + (var27 - var12)]++] = var74;
               }
            }
         }

         this.I = var20;
         this.J = var64;
         this.K = var22;
         this.L = null;
         this.M = null;
         this.N = null;
         this.O = 0;
      } else {
         long var62 = Math.min(var15, var60);
         int var66 = Integer.highestOneBit((int)Math.max(4L, Math.min(536870912L, var62 * 2L)) - 1) << 1;
         long[] var70 = new long[var66];
         int[] var75 = new int[var66];
         int[] var80 = new int[var66];
         int var85 = var66 - 1;

         for (int var88 = 0; var88 < var9; var88++) {
            for (int var28 = var10[var88 * 4]; var28 <= var10[var88 * 4 + 1]; var28++) {
               for (int var29 = var10[var88 * 4 + 2]; var29 <= var10[var88 * 4 + 3]; var29++) {
                  long var30 = b(var28, var29);
                  int var32 = (int)Hashing.mix(var30) & var85;

                  while (var80[var32] != 0 && var70[var32] != var30) {
                     var32 = var32 + 1 & var85;
                  }

                  var70[var32] = var30;
                  var80[var32]++;
               }
            }
         }

         int[] var89 = new int[var66];
         int var96 = 0;

         for (int var105 = 0; var105 < var66; var105++) {
            if (var80[var105] != 0) {
               var75[var105] = var96;
               var89[var105] = var96;
               var96 += var80[var105];
               var80[var105] = var96;
            }
         }

         for (int var106 = 0; var106 < var9; var106++) {
            for (int var113 = var10[var106 * 4]; var113 <= var10[var106 * 4 + 1]; var113++) {
               for (int var31 = var10[var106 * 4 + 2]; var31 <= var10[var106 * 4 + 3]; var31++) {
                  long var122 = b(var113, var31);
                  int var34 = (int)Hashing.mix(var122) & var85;

                  while (var80[var34] == 0 || var70[var34] != var122) {
                     var34 = var34 + 1 & var85;
                  }

                  this.F[var89[var34]++] = var106;
               }
            }
         }

         this.I = 0;
         this.J = 0;
         this.K = null;
         this.L = var70;
         this.M = var75;
         this.N = var80;
         this.O = var85;
      }

      int var63 = var1.size();
      this.g = new double[var63];
      this.h = new double[var63];
      this.i = new double[var63];
      this.j = new double[var63];
      this.k = new double[var63];
      this.l = new double[var63];
      this.m = new double[var63];
      this.n = new double[var63];
      this.o = new double[var63];
      this.p = new double[var63];
      this.q = new double[var63];
      this.r = new double[var63];
      double var65 = 0.0;

      for (int var71 = 0; var71 < var63; var71++) {
         RiverSegment var76 = (RiverSegment)var1.get(var71);
         this.p[var71] = var76.w1();
         this.q[var71] = var76.w2();
         double var81 = Math.max(var76.w1(), var76.w2());
         var65 = Math.max(var65, var81);
         double var90 = var81 + Math.max(5.0, var81 * var5);
         this.r[var71] = var90 * var90;
         double var107 = var76.x2() - var76.x1();
         double var117 = var76.z2() - var76.z1();
         this.g[var71] = var76.x1();
         this.h[var71] = var76.z1();
         this.i[var71] = var107;
         this.j[var71] = var117;
         this.k[var71] = var107 * var107 + var117 * var117;
         this.l[var71] = Math.min(var76.x1(), var76.x2());
         this.m[var71] = Math.max(var76.x1(), var76.x2());
         this.n[var71] = Math.min(var76.z1(), var76.z2());
         this.o[var71] = Math.max(var76.z1(), var76.z2());
      }

      this.A = var65;
      this.t = new boolean[var1.size()];
      this.v = new boolean[var1.size()];
      this.u = new double[var1.size()];
      Arrays.fill(this.u, Double.MAX_VALUE);
      HashSet var72 = new HashSet(var1.size() * 2);
      HashMap var77 = new HashMap(var1.size() * 2);
      HashMap var82 = new HashMap(var1.size() * 2);

      for (int var86 = 0; var86 < var1.size(); var86++) {
         RiverSegment var91 = (RiverSegment)var1.get(var86);
         var72.add(a(var91.x1(), var91.z1()));
         var82.put(a(var91.x1(), var91.z1()), var86);
         long var97 = a(var91.x2(), var91.z2());
         Integer var114 = (Integer)var77.get(var97);
         if (var114 == null || a(var91) > a((RiverSegment)var1.get(var114))) {
            var77.put(var97, var86);
         }
      }

      boolean[] var87 = new boolean[var1.size()];

      for (int var92 = 0; var92 < var1.size(); var92++) {
         RiverSegment var98 = (RiverSegment)var1.get(var92);
         var87[var92] = !var72.contains(a(var98.x2(), var98.z2()));
      }

      for (int var93 = 0; var93 < var1.size(); var93++) {
         int var99 = var93;
         double var108 = 0.0;

         for (int var118 = 0; var118 < 64; var118++) {
            if (var87[var99]) {
               this.t[var93] = var108 <= 6.0;
               this.u[var93] = var108;
               break;
            }

            RiverSegment var123 = (RiverSegment)var1.get(var99);
            Integer var33 = (Integer)var82.get(a(var123.x2(), var123.z2()));
            if (var33 == null || var33 == var99) {
               break;
            }

            var108 += Math.hypot(var123.x2() - var123.x1(), var123.z2() - var123.z1());
            if (var108 > 64.0) {
               break;
            }

            var99 = var33;
         }
      }

      for (int var94 = 0; var94 < var1.size(); var94++) {
         int var100 = var94;
         double var109 = 0.0;

         for (int var119 = 0; var119 < 64; var119++) {
            RiverSegment var124 = (RiverSegment)var1.get(var100);
            Integer var130 = (Integer)var77.get(a(var124.x1(), var124.z1()));
            if (var130 == null || var130 == var100) {
               this.v[var94] = var109 <= 6.0 && !a(var7, var124.x1(), var124.z1());
               break;
            }

            var109 += Math.hypot(var124.x2() - var124.x1(), var124.z2() - var124.z1());
            if (var109 > 6.0) {
               break;
            }

            var100 = var130;
         }
      }

      double[] var95 = new double[var1.size()];

      for (int var101 = 0; var101 < var1.size(); var101++) {
         var95[var101] = this.a(var101, var77);
      }

      this.w = new double[var1.size()];

      for (int var102 = 0; var102 < var1.size(); var102++) {
         double var110 = var95[var102];
         int var120 = var102;
         double var125 = 0.0;

         for (int var139 = 0; var139 < 64 && var125 < 16.0; var139++) {
            RiverSegment var35 = (RiverSegment)var1.get(var120);
            Integer var36 = (Integer)var77.get(a(var35.x1(), var35.z1()));
            if (var36 == null || var36 == var120) {
               break;
            }

            var110 = Math.max(var110, var95[var36]);
            RiverSegment var37 = (RiverSegment)var1.get(var36);
            var125 += Math.hypot(var37.x2() - var37.x1(), var37.z2() - var37.z1());
            var120 = var36;
         }

         this.w[var102] = var110;
      }

      this.B = new double[var1.size()];

      for (int var103 = 0; var103 < var1.size(); var103++) {
         if (!(var95[var103] > 0.04)) {
            RiverSegment var111 = (RiverSegment)var1.get(var103);
            double var115 = Math.hypot(var111.x2() - var111.x1(), var111.z2() - var111.z1());
            int var126 = var103;

            for (int var131 = 0; var131 < 400 && var115 < 256.0; var131++) {
               RiverSegment var140 = (RiverSegment)var1.get(var126);
               Integer var147 = (Integer)var77.get(a(var140.x1(), var140.z1()));
               if (var147 == null || var147 == var126 || var95[var147] > 0.04) {
                  break;
               }

               RiverSegment var152 = (RiverSegment)var1.get(var147);
               var115 += Math.hypot(var152.x2() - var152.x1(), var152.z2() - var152.z1());
               var126 = var147;
            }

            var126 = var103;

            for (int var132 = 0; var132 < 400 && var115 < 256.0; var132++) {
               RiverSegment var141 = (RiverSegment)var1.get(var126);
               Integer var148 = (Integer)var82.get(a(var141.x2(), var141.z2()));
               if (var148 == null || var148 == var126 || var95[var148] > 0.04) {
                  break;
               }

               RiverSegment var153 = (RiverSegment)var1.get(var148);
               var115 += Math.hypot(var153.x2() - var153.x1(), var153.z2() - var153.z1());
               var126 = var148;
            }

            this.B[var103] = Math.min(256.0, var115);
         }
      }

      HashMap<Long, Integer> var104 = new HashMap<>(var1.size() * 2);
      HashMap<Long, Integer> var112 = new HashMap<>(var1.size() * 2);
      HashMap<Long, Integer> var116 = new HashMap<>(var1.size() * 2);
      HashMap<Long, Integer> var121 = new HashMap<>(var1.size() * 2);

      for (int var128 = 0; var128 < var1.size(); var128++) {
         RiverSegment var133 = (RiverSegment)var1.get(var128);
         long var142 = a(var133.x1(), var133.z1());
         long var154 = a(var133.x2(), var133.z2());
         var104.merge(var142, 1, Integer::sum);
         var116.put(var142, var128);
         var112.merge(var154, 1, Integer::sum);
         var121.put(var154, var128);
      }

      int[] var129 = new int[var1.size()];
      int var134 = 0;

      while (var134 < var129.length) {
         var129[var134] = var134++;
      }

      for (Entry var143 : var116.entrySet()) {
         long var149 = (Long)var143.getKey();
         if (var104.getOrDefault(var149, 0) == 1 && var112.getOrDefault(var149, 0) == 1) {
            a(var129, (Integer)var121.get(var149), (Integer)var143.getValue());
         }
      }

      this.x = new int[var1.size()];

      for (int var136 = 0; var136 < this.x.length; var136++) {
         this.x[var136] = a(var129, var136);
      }

      this.y = new double[var1.size()];

      for (int var137 = 0; var137 < var1.size(); var137++) {
         double var144 = Double.MAX_VALUE;
         int var155 = var137;
         double var159 = 0.0;

         for (int var39 = 0; var39 < 256 && var159 <= 64.0; var39++) {
            RiverSegment var40 = (RiverSegment)var1.get(var155);
            long var41 = a(var40.x2(), var40.z2());
            if (var112.getOrDefault(var41, 0) >= 2) {
               var144 = var159;
               break;
            }

            Integer var43 = (Integer)var116.get(var41);
            if (var43 == null || var43 == var155) {
               break;
            }

            RiverSegment var44 = (RiverSegment)var1.get(var43);
            var159 += Math.hypot(var44.x2() - var44.x1(), var44.z2() - var44.z1());
            var155 = var43;
         }

         var155 = var137;
         var159 = 0.0;

         for (int var163 = 0; var163 < 256 && var159 <= 64.0 && var159 < var144; var163++) {
            RiverSegment var164 = (RiverSegment)var1.get(var155);
            long var167 = a(var164.x1(), var164.z1());
            if (var112.getOrDefault(var167, 0) >= 2) {
               var144 = Math.min(var144, var159);
               break;
            }

            Integer var170 = (Integer)var121.get(var167);
            if (var170 == null || var170 == var155) {
               break;
            }

            RiverSegment var172 = (RiverSegment)var1.get(var170);
            var159 += Math.hypot(var172.x2() - var172.x1(), var172.z2() - var172.z1());
            var155 = var170;
         }

         this.y[var137] = var144 > 64.0 ? Double.MAX_VALUE : var144;
      }

      double[] var138 = new double[var1.size()];
      Arrays.fill(var138, -1.0);

      for (int var145 = 0; var145 < var1.size(); var145++) {
         RiverSegment var150 = (RiverSegment)var1.get(var145);
         Integer var157 = (Integer)var121.get(a(var150.x1(), var150.z1()));
         boolean var161 = var157 == null || var157 == var145 || this.x[var157] != this.x[var145] || var112.getOrDefault(a(var150.x1(), var150.z1()), 0) != 1;
         if (var161) {
            double var38 = 0.0;
            int var165 = var145;

            for (int var168 = 0; var168 < var1.size() && var138[var165] < 0.0; var168++) {
               var138[var165] = var38;
               RiverSegment var42 = (RiverSegment)var1.get(var165);
               var38 += Math.hypot(var42.x2() - var42.x1(), var42.z2() - var42.z1());
               Integer var171 = (Integer)var116.get(a(var42.x2(), var42.z2()));
               if (var171 == null || var171 == var165 || this.x[var171] != this.x[var165]) {
                  break;
               }

               var165 = var171;
            }
         }
      }

      for (int var146 = 0; var146 < var1.size(); var146++) {
         if (!(this.B[var146] <= 0.0) && !(var138[var146] < 0.0)) {
            RiverSegment var151 = (RiverSegment)var1.get(var146);
            double var158 = Math.max(var151.w1(), var151.w2());
            double var162 = (var151.x1() + var151.x2()) * 0.5;
            double var166 = (var151.z1() + var151.z2()) * 0.5;
            double var169 = var158 + this.A + 2.0;
            boolean var173 = false;

            for (int var45 = this.b(var162 - var169); var45 <= this.b(var162 + var169) && !var173; var45++) {
               for (int var46 = this.c(var166 - var169); var46 <= this.c(var166 + var169) && !var173; var46++) {
                  long var47 = this.a(var45, var46);
                  int var49 = (int)(var47 >>> 32);
                  int var50 = (int)var47;
                  if (var49 != var50) {
                     for (int var51 = var49; var51 < var50; var51++) {
                        int var52 = this.F[var51];
                        if (var52 != var146 && this.x[var52] == this.x[var146] && !(var138[var52] < 0.0)) {
                           RiverSegment var53 = (RiverSegment)var1.get(var52);
                           double var54 = Math.max(var53.w1(), var53.w2());
                           double var56 = var53.clampParam(var162, var166);
                           double var58 = var53.distanceAt(var162, var166, var56);
                           if (!(var58 > var158 + var54 + 1.0) && Math.abs(var138[var52] - var138[var146]) > 2.0 * var58 + 8.0) {
                              var173 = true;
                              break;
                           }
                        }
                     }
                  }
               }
            }

            if (var173) {
               this.y[var146] = -1.0;
            }
         }
      }
   }

   private static int a(int[] var0, int var1) {
      while (var0[var1] != var1) {
         var0[var1] = var0[var0[var1]];
         var1 = var0[var1];
      }

      return var1;
   }

   private static void a(int[] var0, int var1, int var2) {
      int var3 = a(var0, var1);
      int var4 = a(var0, var2);
      if (var3 != var4) {
         var0[var3] = var4;
      }
   }

   private double a(int var1, Map<Long, Integer> var2) {
      RiverSegment var3 = this.f.get(var1);
      double var4 = var3.y1() - var3.y2();
      double var6 = Math.hypot(var3.x2() - var3.x1(), var3.z2() - var3.z1());
      int var8 = var1;

      for (int var9 = 0; var9 < 64 && var6 < 4.0; var9++) {
         RiverSegment var10 = this.f.get(var8);
         Integer var11 = (Integer)var2.get(a(var10.x1(), var10.z1()));
         if (var11 == null || var11 == var8) {
            break;
         }

         RiverSegment var12 = this.f.get(var11);
         var4 += var12.y1() - var12.y2();
         var6 += Math.hypot(var12.x2() - var12.x1(), var12.z2() - var12.z1());
         var8 = var11;
      }

      return var6 > 1.0E-6 ? Math.max(0.0, var4) / var6 : 0.0;
   }

   private static double a(RiverSegment var0) {
      double var1 = Math.hypot(var0.x2() - var0.x1(), var0.z2() - var0.z1());
      return var1 > 1.0E-6 ? (var0.y1() - var0.y2()) / var1 : 0.0;
   }

   private static boolean a(List<Lake> var0, double var1, double var3) {
      if (var0.isEmpty()) {
         return false;
      }

      int var5 = (int)Math.floor(var1);
      int var6 = (int)Math.floor(var3);

      for (Lake var8 : var0) {
         if (var8.wetWithin(var5, var6, 1)) {
            return true;
         }
      }

      return false;
   }

   public long approxBytes() {
      long var1 = 0L;

      for (double[] var6 : new double[][]{
         this.g, this.h, this.i, this.j, this.k, this.l, this.m, this.n, this.o, this.p, this.q, this.r, this.u, this.w, this.y, this.B
      }) {
         var1 += var6 == null ? 0L : MemBytes.array(var6.length, 8);
      }

      return var1
         + MemBytes.array(this.t.length, 1)
         + MemBytes.array(this.v.length, 1)
         + MemBytes.array(this.x.length, 4)
         + MemBytes.array(this.F.length, 4)
         + (this.K == null ? 0L : MemBytes.array(this.K.length, 4))
         + (this.L == null ? 0L : MemBytes.array(this.L.length, 8) + 2L * MemBytes.array(this.M.length, 4));
   }

   private static long a(double var0, double var2) {
      long var4 = Math.round(var0 * 16.0) & 4294967295L;
      long var6 = Math.round(var2 * 16.0) & 4294967295L;
      return var4 << 32 | var6;
   }

   public RiverSample nearest(double var1, double var3, double var5) {
      if (this.f.isEmpty()) {
         return null;
      }

      int[] var7 = this.a(var1, var3, var5);
      double var8 = Double.MAX_VALUE;
      int var10 = -1;
      double var11 = 0.0;
      double var13 = var5;

      for (int var15 = 0; var7[var15] >= 0; var15++) {
         int var16 = var7[var15];
         double var17 = var1 < this.l[var16] ? this.l[var16] - var1 : (var1 > this.m[var16] ? var1 - this.m[var16] : 0.0);
         double var19 = var3 < this.n[var16] ? this.n[var16] - var3 : (var3 > this.o[var16] ? var3 - this.o[var16] : 0.0);
         if (!(var17 * var17 + var19 * var19 > a(var13))) {
            double var21 = this.i[var16];
            double var23 = this.j[var16];
            double var25 = this.k[var16];
            double var27;
            if (var25 <= 1.0E-9) {
               var27 = 0.0;
            } else {
               var27 = ((var1 - this.g[var16]) * var21 + (var3 - this.h[var16]) * var23) / var25;
               var27 = var27 < 0.0 ? 0.0 : (var27 > 1.0 ? 1.0 : var27);
            }

            double var29 = this.g[var16] + var21 * var27;
            double var31 = this.h[var16] + var23 * var27;
            double var33 = var1 - var29;
            double var35 = var3 - var31;
            double var37 = Math.sqrt(var33 * var33 + var35 * var35);
            if (var37 < var8 || var37 == var8 && var16 < var10) {
               var8 = var37;
               var10 = var16;
               var13 = var37 < var5 ? var37 : var5;
               var11 = var27;
            }
         }
      }

      return var10 >= 0 && !(var8 > var5) ? this.a(var10, var11, var8) : null;
   }

   public RiverSample nearestTiered(double var1, double var3, double var5) {
      if (this.f.isEmpty()) {
         return null;
      }

      int[] var7 = this.a(var1, var3, var5);
      double var8 = Double.MAX_VALUE;
      int var10 = -1;
      double var11 = 0.0;
      double var13 = Double.MAX_VALUE;
      int var15 = -1;
      double var16 = 0.0;
      double var18 = Double.MAX_VALUE;
      int var20 = -1;
      double var21 = 0.0;
      double var23 = var5;

      for (int var25 = 0; var7[var25] >= 0; var25++) {
         int var26 = var7[var25];
         double var27 = var1 < this.l[var26] ? this.l[var26] - var1 : (var1 > this.m[var26] ? var1 - this.m[var26] : 0.0);
         double var29 = var3 < this.n[var26] ? this.n[var26] - var3 : (var3 > this.o[var26] ? var3 - this.o[var26] : 0.0);
         double var31 = var27 * var27 + var29 * var29;
         if (!(var31 > a(var23)) || !(var31 > this.r[var26])) {
            double var33 = this.i[var26];
            double var35 = this.j[var26];
            double var37 = this.k[var26];
            double var39;
            if (var37 <= 1.0E-9) {
               var39 = 0.0;
            } else {
               var39 = ((var1 - this.g[var26]) * var33 + (var3 - this.h[var26]) * var35) / var37;
               var39 = var39 < 0.0 ? 0.0 : (var39 > 1.0 ? 1.0 : var39);
            }

            double var41 = this.g[var26] + var33 * var39;
            double var43 = this.h[var26] + var35 * var39;
            double var45 = var1 - var41;
            double var47 = var3 - var43;
            double var49 = Math.sqrt(var45 * var45 + var47 * var47);
            if (var49 < var8 || var49 == var8 && var26 < var10) {
               var8 = var49;
               var10 = var26;
               var23 = var49 < var5 ? var49 : var5;
               var11 = var39;
            }

            if (var49 <= var5) {
               double var51 = this.p[var26] + (this.q[var26] - this.p[var26]) * var39;
               if (var49 <= var51 && (var49 < var18 || var49 == var18 && var26 < var20)) {
                  var18 = var49;
                  var20 = var26;
                  var21 = var39;
               }

               if (var49 <= var51 + Math.max(5.0, var51 * this.s) && (var49 < var13 || var49 == var13 && var26 < var15)) {
                  var13 = var49;
                  var15 = var26;
                  var16 = var39;
               }
            }
         }
      }

      if (var20 >= 0) {
         var10 = var20;
         var8 = var18;
         var11 = var21;
      } else if (var15 >= 0) {
         var10 = var15;
         var8 = var13;
         var11 = var16;
      }

      return var10 >= 0 && !(var8 > var5) ? this.a(var10, var11, var8) : null;
   }

   private int[] a(double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1 / 16.0);
      int var8 = (int)Math.floor(var3 / 16.0);
      long var9 = (long)var7 << 32 ^ var8 & 4294967295L;
      NearestRiverIndex.TieredCand[] var11 = P.get();

      for (NearestRiverIndex.TieredCand var15 : var11) {
         if (var15.a == this && var15.b == var9 && var15.c == var5) {
            return var15.d;
         }
      }

      long[] var32 = R.get();
      long var33 = var9 * 31L + Double.doubleToLongBits(var5) + System.identityHashCode(this);
      if (var32[0] != var33) {
         var32[0] = var33;
         int[] var35 = S.get()[0];
         int var16 = 0;
         int var36 = this.b(var1 - var5);
         int var18 = this.b(var1 + var5);
         int var37 = this.c(var3 - var5);
         int var38 = this.c(var3 + var5);

         for (int var39 = var36; var39 <= var18; var39++) {
            for (int var40 = var37; var40 <= var38; var40++) {
               long var41 = this.a(var39, var40);
               int var44 = (int)(var41 >>> 32);
               int var47 = (int)var41;
               if (var44 != var47) {
                  if (var16 + var47 - var44 + 1 > var35.length) {
                     var35 = Arrays.copyOf(var35, Math.max(var35.length * 2, var16 + var47 - var44 + 1));
                     S.get()[0] = var35;
                  }

                  System.arraycopy(this.F, var44, var35, var16, var47 - var44);
                  var16 += var47 - var44;
               }
            }
         }

         if (var16 + 1 > var35.length) {
            var35 = Arrays.copyOf(var35, var16 + 1);
            S.get()[0] = var35;
         }

         var35[var16] = -1;
         return var35;
      } else {
         double var34 = var7 * 16.0;
         double var17 = var8 * 16.0;
         int var19 = this.b(var34 - var5);
         int var20 = this.b(var34 + 16.0 + var5);
         int var21 = this.c(var17 - var5);
         int var22 = this.c(var17 + 16.0 + var5);
         int var23 = 0;

         for (int var24 = var19; var24 <= var20; var24++) {
            for (int var25 = var21; var25 <= var22; var25++) {
               long var26 = this.a(var24, var25);
               int var28 = (int)(var26 >>> 32);
               int var29 = (int)var26;
               if (var28 != var29) {
                  var23 += var29 - var28;
               }
            }
         }

         int[] var42 = new int[var23];
         int var43 = 0;

         for (int var45 = var19; var45 <= var20; var45++) {
            for (int var27 = var21; var27 <= var22; var27++) {
               long var50 = this.a(var45, var27);
               int var30 = (int)(var50 >>> 32);
               int var31 = (int)var50;
               if (var30 != var31) {
                  System.arraycopy(this.F, var30, var42, var43, var31 - var30);
                  var43 += var31 - var30;
               }
            }
         }

         Arrays.sort(var42);
         int var46 = 0;

         for (int var48 = 0; var48 < var42.length; var48++) {
            if (var48 == 0 || var42[var48] != var42[var48 - 1]) {
               var42[var46++] = var42[var48];
            }
         }

         int[] var49 = Arrays.copyOf(var42, var46 + 1);
         var49[var46] = -1;
         int[] var51 = Q.get();
         NearestRiverIndex.TieredCand var52 = var11[var51[0]];
         var51[0] = var51[0] + 1 & 7;
         var52.a = this;
         var52.b = var9;
         var52.c = var5;
         var52.d = var49;
         return var49;
      }
   }

   public boolean anyCorridorTouchesBox(double var1, double var3, double var5, double var7) {
      if (this.f.isEmpty()) {
         return false;
      }

      int var9 = this.b(var1);
      int var10 = this.b(var5);
      int var11 = this.c(var3);
      int var12 = this.c(var7);

      for (int var13 = var9; var13 <= var10; var13++) {
         for (int var14 = var11; var14 <= var12; var14++) {
            long var15 = this.a(var13, var14);
            int var17 = (int)(var15 >>> 32);
            int var18 = (int)var15;
            if (var17 != var18) {
               for (int var19 = var17; var19 < var18; var19++) {
                  int var20 = this.F[var19];
                  double var21 = this.l[var20] > var5 ? this.l[var20] - var5 : (this.m[var20] < var1 ? var1 - this.m[var20] : 0.0);
                  double var23 = this.n[var20] > var7 ? this.n[var20] - var7 : (this.o[var20] < var3 ? var3 - this.o[var20] : 0.0);
                  if (var21 * var21 + var23 * var23 <= this.r[var20]) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private RiverSample a(int var1, double var2, double var4) {
      RiverSegment var6 = this.f.get(var1);
      double var7 = var6.x2() - var6.x1();
      double var9 = var6.z2() - var6.z1();
      double var11 = Math.sqrt(var7 * var7 + var9 * var9);
      double var13 = var11 > 1.0E-6 ? (var6.y1() - var6.y2()) / var11 : 0.0;
      double var15 = var11 > 1.0E-6 ? (var6.x2() - var6.x1()) / var11 : 0.0;
      double var17 = var11 > 1.0E-6 ? (var6.z2() - var6.z1()) / var11 : 0.0;
      return new RiverSample(
         var4,
         var6.gradeAt(var2),
         var6.widthAt(var2),
         var13,
         var15,
         var17,
         this.t[var1],
         this.v[var1],
         this.w[var1],
         this.u[var1],
         var1,
         this.y[var1],
         this.B[var1],
         var6.speedAt(var2)
      );
   }

   public RiverSample nearestCoveringForeign(double var1, double var3, double var5, int var7) {
      if (this.f.isEmpty()) {
         return null;
      }

      int var8 = var7 >= 0 ? this.x[var7] : Integer.MIN_VALUE;
      int var9 = this.b(var1 - var5);
      int var10 = this.b(var1 + var5);
      int var11 = this.c(var3 - var5);
      int var12 = this.c(var3 + var5);
      double var13 = Double.MAX_VALUE;
      int var15 = -1;
      double var16 = 0.0;

      for (int var18 = var9; var18 <= var10; var18++) {
         for (int var19 = var11; var19 <= var12; var19++) {
            long var20 = this.a(var18, var19);
            int var22 = (int)(var20 >>> 32);
            int var23 = (int)var20;
            if (var22 != var23) {
               double var24 = var13 < var5 ? var13 : var5;

               for (int var26 = var22; var26 < var23; var26++) {
                  int var27 = this.F[var26];
                  if (this.x[var27] != var8) {
                     double var28 = var1 < this.l[var27] ? this.l[var27] - var1 : (var1 > this.m[var27] ? var1 - this.m[var27] : 0.0);
                     double var30 = var3 < this.n[var27] ? this.n[var27] - var3 : (var3 > this.o[var27] ? var3 - this.o[var27] : 0.0);
                     double var32 = var28 * var28 + var30 * var30;
                     if (!(var32 > a(var24)) && !(var32 > this.r[var27])) {
                        double var34 = this.i[var27];
                        double var36 = this.j[var27];
                        double var38 = this.k[var27];
                        double var40;
                        if (var38 <= 1.0E-9) {
                           var40 = 0.0;
                        } else {
                           var40 = ((var1 - this.g[var27]) * var34 + (var3 - this.h[var27]) * var36) / var38;
                           var40 = var40 < 0.0 ? 0.0 : (var40 > 1.0 ? 1.0 : var40);
                        }

                        double var42 = this.g[var27] + var34 * var40;
                        double var44 = this.h[var27] + var36 * var40;
                        double var46 = var1 - var42;
                        double var48 = var3 - var44;
                        double var50 = Math.sqrt(var46 * var46 + var48 * var48);
                        if (!(var50 > var5) && !(var50 > var13) && (var50 != var13 || var27 < var15)) {
                           double var52 = this.p[var27] + (this.q[var27] - this.p[var27]) * var40;
                           if (var50 <= var52 + Math.max(5.0, var52 * this.s)) {
                              var13 = var50;
                              var15 = var27;
                              var16 = var40;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return var15 < 0 ? null : this.a(var15, var16, var13);
   }

   public double lowerNeighborGrade(double var1, double var3, double var5, int var7, double var8, double var10) {
      if (this.f.isEmpty()) {
         return Double.NaN;
      }

      int var12 = var7 >= 0 ? this.x[var7] : -1;
      double var13 = var8 - var10;
      int var15 = this.b(var1 - var5);
      int var16 = this.b(var1 + var5);
      int var17 = this.c(var3 - var5);
      int var18 = this.c(var3 + var5);
      double var19 = Double.NaN;

      for (int var21 = var15; var21 <= var16; var21++) {
         for (int var22 = var17; var22 <= var18; var22++) {
            long var23 = this.a(var21, var22);
            int var25 = (int)(var23 >>> 32);
            int var26 = (int)var23;
            if (var25 != var26) {
               for (int var27 = var25; var27 < var26; var27++) {
                  int var28 = this.F[var27];
                  if (var28 != var7 && this.x[var28] != var12) {
                     RiverSegment var29 = this.f.get(var28);
                     double var30 = var29.clampParam(var1, var3);
                     if (!(var29.distanceAt(var1, var3, var30) > var5)) {
                        double var32 = var29.gradeAt(var30);
                        if (var32 < var13 && (Double.isNaN(var19) || var32 < var19)) {
                           var19 = var32;
                        }
                     }
                  }
               }
            }
         }
      }

      return var19;
   }

   public boolean inAnyChannel(double var1, double var3, double var5) {
      if (this.f.isEmpty()) {
         return false;
      }

      int var7 = this.b(var1 - var5);
      int var8 = this.b(var1 + var5);
      int var9 = this.c(var3 - var5);
      int var10 = this.c(var3 + var5);

      for (int var11 = var7; var11 <= var8; var11++) {
         for (int var12 = var9; var12 <= var10; var12++) {
            long var13 = this.a(var11, var12);
            int var15 = (int)(var13 >>> 32);
            int var16 = (int)var13;
            if (var15 != var16) {
               for (int var17 = var15; var17 < var16; var17++) {
                  int var18 = this.F[var17];
                  RiverSegment var19 = this.f.get(var18);
                  double var20 = var19.clampParam(var1, var3);
                  if (var19.distanceAt(var1, var3, var20) <= var19.widthAt(var20)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private long a(int var1, int var2) {
      if (this.I > 0) {
         int var7 = var1 - this.G;
         int var4 = var2 - this.H;
         if (var7 >= 0 && var4 >= 0 && var7 < this.I && var4 < this.J) {
            int var8 = var7 * this.J + var4;
            return (long)this.K[var8] << 32 | this.K[var8 + 1] & 4294967295L;
         } else {
            return 0L;
         }
      } else {
         if (this.L == null) {
            return 0L;
         }

         long var3 = b(var1, var2);
         int var5 = (int)Hashing.mix(var3) & this.O;

         while (true) {
            int var6 = this.N[var5];
            if (var6 == 0) {
               return 0L;
            }

            if (this.L[var5] == var3) {
               return (long)this.M[var5] << 32 | var6 & 4294967295L;
            }

            var5 = var5 + 1 & this.O;
         }
      }
   }

   private static double a(double var0) {
      return var0 * var0 * 1.000000001 + 1.0E-6;
   }

   private int b(double var1) {
      return (int)Math.floor(var1 / this.E);
   }

   private int c(double var1) {
      return (int)Math.floor(var1 / this.E);
   }

   private static long b(int var0, int var1) {
      return (long)var0 << 32 ^ var1 & 4294967295L;
   }

   private static final class TieredCand {
      NearestRiverIndex a;
      long b;
      double c;
      int[] d;
   }
}
