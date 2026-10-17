package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.grid.LowResGrid;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;

public final class RiverFlowField {
   public static final int LAND = 0;
   public static final int RIVER = 1;
   public static final int OCEAN = 2;
   private static final int[] a = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] b = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final double c = Math.sqrt(2.0);
   private static final int d = 2;
   private static final int e = 3;
   private static final double[] f = new double[8];
   private static final double[] g = new double[8];
   private final int h;
   private final int i;
   private final int j;
   private final int k;
   private final int l;
   private final byte[] m;
   private final long[] n;
   public final int oceanCells;
   public final int riverCells;
   public final int flowedCells;
   private static final int o = 4;
   private static final int p = 2;
   private static final int q = 8;
   private static final int r = 2;
   public static volatile int lastSampled;
   public static volatile double[] lastPhaseMs;
   public static volatile int lastCells;
   public static volatile int lastReconciled;
   private static final int s = 8;
   private static final int t = 4;
   private static final int u = 512;
   public static volatile int lastBridges;
   public static volatile int lastBridgedCells;
   public static volatile int lastBridgeRejected;
   public static volatile int lastBridgeSamples;
   private static final int v = 4096;
   private static final int w = 4096;
   private static final int x = 4096;
   private static final int y = 0;
   public static volatile int lastSeamCells;
   public static volatile int lastDeepCells;
   public static volatile int lastDeepProbes;
   private static final boolean z;
   private static final ThreadLocal<StringBuilder> A;
   private static final ThreadLocal<int[]> B;
   private static final Object C;
   private static final int D = 256;
   private static final int E = 3;
   private static final int F = 3;
   private static final long G = 9221120239188049920L;

   private RiverFlowField(int var1, int var2, int var3, int var4, int var5, byte[] var6, int var7, int var8, int var9, long[] var10) {
      this.h = var1;
      this.i = var2;
      this.j = var3;
      this.k = var4;
      this.l = var5;
      this.m = var6;
      this.oceanCells = var7;
      this.riverCells = var8;
      this.flowedCells = var9;
      this.n = var10;
   }

   public long approxBytes() {
      return MemBytes.object(40) + MemBytes.array(this.m.length, 1) + (this.n == null ? 0L : MemBytes.array(this.n.length, 8));
   }

   public boolean bridgedNear(int var1, int var2) {
      if (this.n == null) {
         return false;
      }

      int var3 = Math.floorDiv(var1 - this.h, this.j);
      int var4 = Math.floorDiv(var2 - this.i, this.j);

      for (int var5 = -3; var5 <= 3; var5++) {
         for (int var6 = -3; var6 <= 3; var6++) {
            int var7 = var3 + var6;
            int var8 = var4 + var5;
            if (var7 >= 0 && var8 >= 0 && var7 < this.k && var8 < this.l) {
               int var9 = var8 * this.k + var7;
               if ((this.n[var9 >> 6] & 1L << (var9 & 63)) != 0L) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public void writeTo(DataOutputStream var1) throws IOException {
      var1.writeInt(this.h);
      var1.writeInt(this.i);
      var1.writeInt(this.j);
      var1.writeInt(this.k);
      var1.writeInt(this.l);
      var1.write(this.m);
      var1.writeInt(this.oceanCells);
      var1.writeInt(this.riverCells);
      var1.writeInt(this.flowedCells);
      if (this.n == null) {
         var1.writeInt(0);
      } else {
         var1.writeInt(this.n.length);

         for (long var5 : this.n) {
            var1.writeLong(var5);
         }
      }
   }

   public static RiverFlowField readFrom(DataInputStream var0) throws IOException {
      int var1 = var0.readInt();
      int var2 = var0.readInt();
      int var3 = var0.readInt();
      int var4 = var0.readInt();
      int var5 = var0.readInt();
      if (var3 > 0 && var4 > 0 && var5 > 0 && var4 <= 4096 && var5 <= 4096) {
         byte[] var6 = new byte[var4 * var5];
         var0.readFully(var6);
         int var7 = var0.readInt();
         int var8 = var0.readInt();
         int var9 = var0.readInt();
         int var10 = var0.readInt();
         long[] var11 = null;
         if (var10 > 0) {
            if (var10 != var4 * var5 + 63 >> 6) {
               return null;
            }

            var11 = new long[var10];

            for (int var12 = 0; var12 < var10; var12++) {
               var11[var12] = var0.readLong();
            }
         }

         return new RiverFlowField(var1, var2, var3, var4, var5, var6, var7, var8, var9, var11);
      } else {
         return null;
      }
   }

   private static void a(byte[] var0, int var1, int var2, int var3, int var4, int var5, int var6, RiverFlowField.BiomeClass var7) {
      int var8 = Math.max(2, Math.min(8, var6 > 0 ? var6 : 4));
      boolean[] var9 = new boolean[var1 * var2];
      int var10 = 0;
      int var11 = (var1 + var8 - 1) / var8;
      int var12 = (var2 + var8 - 1) / var8;
      LowResGrid.parallelFor(var11 * var12, var9x -> {
         int var10x = var9x % var11 * var8;
         int var11x = var9x / var11 * var8;
         int var12x = var11x * var1 + var10x;
         var0[var12x] = (byte)var7.classify(var3 + var10x * var5, var4 + var11x * var5);
         var9[var12x] = true;
      });
      var10 += var11 * var12;

      for (int var13 = var8; var13 > 1; var13 >>= 1) {
         int var14 = var13 >> 1;
         boolean[] var15 = new boolean[var1 * var2];

         for (int var16 = 0; var16 < var2; var16 += var13) {
            for (int var17 = 0; var17 < var1; var17 += var13) {
               if (a(var0, var9, var1, var2, var17, var16, var13)) {
                  int var18 = 2 * var13;

                  for (int var19 = -var18; var19 <= var18; var19 += var14) {
                     for (int var20 = -var18; var20 <= var18; var20 += var14) {
                        int var21 = var17 + var20;
                        int var22 = var16 + var19;
                        if (var21 >= 0 && var21 < var1 && var22 >= 0 && var22 < var2) {
                           var15[var22 * var1 + var21] = true;
                        }
                     }
                  }
               }
            }
         }

         int var31 = 0;
         int[] var34 = new int[var1 * var2];

         for (int var37 = 0; var37 < var2; var37 += var14) {
            for (int var40 = 0; var40 < var1; var40 += var14) {
               int var42 = var37 * var1 + var40;
               if (var15[var42] && !var9[var42]) {
                  var34[var31++] = var42;
               }
            }
         }

         int[] var38 = var34;
         LowResGrid.parallelFor(var31, var8x -> {
            int var9x = var38[var8x];
            var0[var9x] = (byte)var7.classify(var3 + var9x % var1 * var5, var4 + var9x / var1 * var5);
            var9[var9x] = true;
         });
         var10 += var31;
      }

      ArrayDeque var24 = new ArrayDeque();

      for (int var26 = 0; var26 < var1 * var2; var26++) {
         if (var9[var26] && var0[var26] == 1) {
            var24.add(var26);
         }
      }

      int var27 = var1 * var2 - var10;

      while (!var24.isEmpty() && var27 > 0) {
         int var29 = (Integer)var24.poll();
         int var32 = var29 % var1;
         int var35 = var29 / var1;

         for (int var39 = 0; var39 < 8 && var27 > 0; var39++) {
            int var41 = var32 + a[var39];
            int var43 = var35 + b[var39];
            if (var41 >= 0 && var41 < var1 && var43 >= 0 && var43 < var2) {
               int var44 = var43 * var1 + var41;
               if (!var9[var44]) {
                  var0[var44] = (byte)var7.classify(var3 + var41 * var5, var4 + var43 * var5);
                  var9[var44] = true;
                  var10++;
                  var27--;
                  if (var0[var44] == 1) {
                     var24.add(var44);
                  }
               }
            }
         }
      }

      for (int var25 = 0; var25 < var2; var25++) {
         for (int var28 = 0; var28 < var1; var28++) {
            int var30 = var25 * var1 + var28;
            if (!var9[var30]) {
               int var33 = Math.min(var28 - var28 % var8, var1 - 1);
               int var36 = Math.min(var25 - var25 % var8, var2 - 1);
               var0[var30] = var0[var36 * var1 + var33];
            }
         }
      }

      lastSampled = var10;
      lastCells = var1 * var2;
   }

   private static boolean a(byte[] var0, boolean[] var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = var5 * var2 + var4;
      if (!var1[var7]) {
         return false;
      }

      if (var0[var7] == 1) {
         return true;
      }

      for (int var8 = 0; var8 < 4; var8++) {
         int var9 = var4 + (var8 == 0 ? var6 : (var8 == 1 ? -var6 : 0));
         int var10 = var5 + (var8 == 2 ? var6 : (var8 == 3 ? -var6 : 0));
         if (var9 >= 0 && var9 < var2 && var10 >= 0 && var10 < var3) {
            int var11 = var10 * var2 + var9;
            if (var1[var11] && var0[var11] != var0[var7]) {
               return true;
            }
         }
      }

      return false;
   }

   public static RiverFlowField build(
      int var0,
      int var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      RiverFlowField.BiomeClass var7,
      RiverFlowField.OrientationField var8,
      RiverFlowField.WaterProbe var9,
      RiverFlowField.DeepChannelProbe var10,
      RiverFlowField.NeighbourFlow var11
   ) {
      int var12 = var0 - var4;
      int var13 = var1 - var4;
      int var14 = (var2 + 2 * var4) / var5 + 1;
      int var15 = (var3 + 2 * var4) / var5 + 1;
      byte[] var16 = new byte[var14 * var15];
      float[] var17 = new float[var14 * var15];
      Arrays.fill(var17, Float.POSITIVE_INFINITY);
      PriorityQueue var18 = new PriorityQueue();
      long var19 = System.nanoTime();
      a(var16, var14, var15, var12, var13, var5, var6, var7);
      long var21 = System.nanoTime();
      long[] var23 = a(var16, var14, var15, var12, var13, var5, var10, null);
      long var24 = System.nanoTime();
      var23 = a(var16, var14, var15, var12, var13, var5, var8, var9, var23);
      long var26 = System.nanoTime();
      int var28 = 0;
      int var29 = 0;

      for (int var30 = 0; var30 < var14 * var15; var30++) {
         if (var16[var30] == 2) {
            var28++;
         } else if (var16[var30] == 1) {
            var29++;
         }
      }

      boolean[] var61 = new boolean[var14 * var15];
      boolean[] var31 = new boolean[var14 * var15];
      int[] var32 = new int[var14 * var15];
      Arrays.fill(var32, -1);
      int[] var33 = new int[var14 * var15];
      Arrays.fill(var33, -1);
      int var34 = 0;
      ArrayDeque var35 = new ArrayDeque();

      for (int var36 = 0; var36 < var14 * var15; var36++) {
         if (var16[var36] == 1 && !var31[var36]) {
            int var37 = var34++;
            var31[var36] = true;
            var32[var36] = var37;
            var35.add(var36);
            int var38 = var36;
            boolean var39 = false;

            while (!var35.isEmpty()) {
               int var40 = (Integer)var35.poll();
               var38 = var40;
               int var41 = var40 % var14;
               int var42 = var40 / var14;

               for (int var43 = 0; var43 < 8; var43++) {
                  int var44 = var41 + a[var43];
                  int var45 = var42 + b[var43];
                  if (var44 >= 0 && var45 >= 0 && var44 < var14 && var45 < var15) {
                     int var46 = var45 * var14 + var44;
                     if (var16[var46] == 1) {
                        if (!var31[var46]) {
                           var31[var46] = true;
                           var32[var46] = var37;
                           var35.add(var46);
                        }
                     } else if (var16[var46] == 2) {
                        var39 = true;
                        var33[var37] = var40;
                     }
                  }
               }
            }

            if (var39 || var8 != null) {
               var61[var38] = true;
            }
         }
      }

      for (int var62 = 0; var62 < var14 * var15; var62++) {
         if (var61[var62]) {
            var17[var62] = 0.0F;
            var18.add(a(0.0F, var62));
         }
      }

      while (!var18.isEmpty()) {
         long var63 = (Long)var18.poll();
         int var65 = (int)(var63 & 4294967295L);
         float var67 = Float.intBitsToFloat((int)(var63 >>> 32));
         if (!(var67 > var17[var65])) {
            int var69 = var65 % var14;
            int var72 = var65 / var14;

            for (int var74 = 0; var74 < 8; var74++) {
               int var77 = var69 + a[var74];
               int var80 = var72 + b[var74];
               if (var77 >= 0 && var80 >= 0 && var77 < var14 && var80 < var15) {
                  int var82 = var80 * var14 + var77;
                  if (var16[var82] == 1) {
                     float var86 = var67 + ((var74 & 1) == 0 ? 1.0F : (float)c);
                     if (var86 < var17[var82]) {
                        var17[var82] = var86;
                        var18.add(a(var86, var82));
                     }
                  }
               }
            }
         }
      }

      long var64 = System.nanoTime();
      byte[] var66 = new byte[var14 * var15];
      Arrays.fill(var66, (byte)-1);
      int var68 = 0;

      for (int var70 = 0; var70 < var15; var70++) {
         for (int var73 = 0; var73 < var14; var73++) {
            int var75 = var70 * var14 + var73;
            if (var16[var75] == 1 && var17[var75] != Float.POSITIVE_INFINITY) {
               int var78 = a(var17, var14, var15, var73, var70);
               if (var78 >= 0) {
                  var66[var75] = (byte)var78;
                  var68++;
               }
            }
         }
      }

      long var71 = System.nanoTime();
      int[] var76 = var34 > 0 ? new int[var34] : null;
      double[] var79 = var34 > 0 ? new double[var34] : null;
      int var81 = 0;
      if (var11 != null && var34 > 0) {
         for (int var83 = 0; var83 < var15; var83++) {
            for (int var87 = 0; var87 < var14; var87++) {
               int var47 = var83 * var14 + var87;
               if (var66[var47] >= 0 && var32[var47] >= 0) {
                  int var48 = var11.dirAt(var12 + var87 * var5, var13 + var83 * var5);
                  if (var48 >= 0) {
                     var79[var32[var47]] = var79[var32[var47]] + (f[var66[var47]] * f[var48] + g[var66[var47]] * g[var48]);
                     var76[var32[var47]]++;
                     var81++;
                  }
               }
            }
         }

         for (int var84 = 0; var84 < var14 * var15; var84++) {
            int var88 = var32[var84];
            if (var66[var84] >= 0 && var88 >= 0 && var76[var88] > 0 && var79[var88] < 0.0) {
               var66[var84] = (byte)(var66[var84] + 4 & 7);
            }
         }
      }

      lastSeamCells = var81;
      boolean[] var85 = var34 > 0 ? new boolean[var34] : null;
      if (var34 > 0) {
         for (int var89 = 0; var89 < var34; var89++) {
            if (var76 == null || var76[var89] <= 0) {
               int var92 = var33[var89];
               if (var92 >= 0 && var66[var92] >= 0) {
                  int var94 = var92 % var14;
                  int var49 = var92 / var14;
                  boolean var50 = false;
                  boolean var51 = false;

                  for (int var52 = 0; var52 < 8; var52++) {
                     int var53 = var94 + a[var52];
                     int var54 = var49 + b[var52];
                     if (var53 >= 0 && var54 >= 0 && var53 < var14 && var54 < var15 && var16[var54 * var14 + var53] == 2) {
                        int var55 = Math.abs(var66[var92] - var52);
                        var55 = Math.min(var55, 8 - var55);
                        if (var55 <= 1) {
                           var50 = true;
                        } else if (var55 >= 3) {
                           var51 = true;
                        }
                     }
                  }

                  if (var50 && !var51) {
                     var85[var89] = true;
                  } else if (var51 && !var50) {
                     for (int var102 = 0; var102 < var14 * var15; var102++) {
                        if (var32[var102] == var89 && var66[var102] >= 0) {
                           var66[var102] = (byte)(var66[var102] + 4 & 7);
                        }
                     }

                     var85[var89] = true;
                  }
               }
            }
         }
      }

      if (var8 != null && var34 > 0) {
         byte var90 = 8;
         int var93 = (var14 - 1) / var90 + 2;
         int var95 = (var15 - 1) / var90 + 2;
         float[] var96 = new float[var93 * var95];

         for (int var97 = 0; var97 < var95; var97++) {
            for (int var99 = 0; var99 < var93; var99++) {
               int var103 = Math.min(var14 - 1, var99 * var90);
               int var106 = Math.min(var15 - 1, var97 * var90);
               var96[var97 * var93 + var99] = (float)var8.sample(var12 + var103 * var5, var13 + var106 * var5);
            }
         }

         double[] var98 = new double[var34];

         for (int var100 = 0; var100 < var15; var100++) {
            for (int var104 = 0; var104 < var14; var104++) {
               int var107 = var100 * var14 + var104;
               if (var66[var107] >= 0 && var32[var107] >= 0) {
                  int var108 = Math.min(var93 - 2, var104 / var90);
                  int var110 = Math.min(var95 - 2, var100 / var90);
                  double var56 = -(var96[var110 * var93 + var108 + 1] - var96[var110 * var93 + var108]);
                  double var58 = -(var96[(var110 + 1) * var93 + var108] - var96[var110 * var93 + var108]);
                  var98[var32[var107]] = var98[var32[var107]] + (f[var66[var107]] * var56 + g[var66[var107]] * var58);
               }
            }
         }

         for (int var101 = 0; var101 < var14 * var15; var101++) {
            int var105 = var32[var101];
            if (var66[var101] >= 0 && var105 >= 0 && (var76 == null || var76[var105] <= 0) && (var85 == null || !var85[var105]) && var98[var105] < 0.0) {
               var66[var101] = (byte)(var66[var101] + 4 & 7);
            }
         }
      }

      long var91 = System.nanoTime();
      lastPhaseMs = new double[]{
         (var21 - var19) / 1000000.0,
         (var24 - var21) / 1000000.0,
         (var26 - var24) / 1000000.0,
         (var64 - var26) / 1000000.0,
         (var71 - var64) / 1000000.0,
         (var91 - var71) / 1000000.0
      };
      return new RiverFlowField(var12, var13, var5, var14, var15, var66, var28, var29, var68, var23);
   }

   public RiverFlowField reconciledWith(RiverFlowField.NeighbourFlow var1) {
      if (var1 == null) {
         return this;
      }

      int[] var2 = new int[this.k * this.l];
      Arrays.fill(var2, -1);
      int var3 = 0;
      ArrayDeque var4 = new ArrayDeque();

      for (int var5 = 0; var5 < this.k * this.l; var5++) {
         if (this.m[var5] >= 0 && var2[var5] < 0) {
            var2[var5] = var3;
            var4.add(var5);

            while (!var4.isEmpty()) {
               int var6 = (Integer)var4.poll();
               int var7 = var6 % this.k;
               int var8 = var6 / this.k;

               for (int var9 = 0; var9 < 8; var9++) {
                  int var10 = var7 + a[var9];
                  int var11 = var8 + b[var9];
                  if (var10 >= 0 && var11 >= 0 && var10 < this.k && var11 < this.l) {
                     int var12 = var11 * this.k + var10;
                     if (this.m[var12] >= 0 && var2[var12] < 0) {
                        var2[var12] = var3;
                        var4.add(var12);
                     }
                  }
               }
            }

            var3++;
         }
      }

      if (var3 == 0) {
         return this;
      }

      double[] var13 = new double[var3];
      int[] var14 = new int[var3];

      for (int var15 = 0; var15 < this.l; var15++) {
         for (int var17 = 0; var17 < this.k; var17++) {
            int var19 = var15 * this.k + var17;
            if (this.m[var19] >= 0) {
               int var21 = var1.dirAt(this.h + var17 * this.j, this.i + var15 * this.j);
               if (var21 >= 0) {
                  var13[var2[var19]] = var13[var2[var19]] + (f[this.m[var19]] * f[var21] + g[this.m[var19]] * g[var21]);
                  var14[var2[var19]]++;
               }
            }
         }
      }

      byte[] var16 = null;
      int var18 = 0;

      for (int var20 = 0; var20 < this.k * this.l; var20++) {
         int var22 = var2[var20];
         if (this.m[var20] >= 0 && var22 >= 0 && var14[var22] != 0 && !(var13[var22] >= 0.0)) {
            if (var16 == null) {
               var16 = (byte[])this.m.clone();
            }

            var16[var20] = (byte)(this.m[var20] + 4 & 7);
            var18++;
         }
      }

      lastReconciled = var18;
      return var16 == null
         ? this
         : new RiverFlowField(this.h, this.i, this.j, this.k, this.l, var16, this.oceanCells, this.riverCells, this.flowedCells, this.n);
   }

   private static long[] a(
      byte[] var0, int var1, int var2, int var3, int var4, int var5, RiverFlowField.OrientationField var6, RiverFlowField.WaterProbe var7, long[] var8
   ) {
      lastBridges = 0;
      lastBridgedCells = 0;
      lastBridgeRejected = 0;
      lastBridgeSamples = 0;
      long[] var9 = var8;
      int[] var10 = new int[var1 * var2];
      Arrays.fill(var10, -1);
      int var11 = 0;
      ArrayDeque var12 = new ArrayDeque();

      for (int var13 = 0; var13 < var1 * var2; var13++) {
         if (var0[var13] == 1 && var10[var13] < 0) {
            var10[var13] = var11;
            var12.add(var13);

            while (!var12.isEmpty()) {
               int var14 = (Integer)var12.poll();
               int var15 = var14 % var1;
               int var16 = var14 / var1;

               for (int var17 = 0; var17 < 8; var17++) {
                  int var18 = var15 + a[var17];
                  int var19 = var16 + b[var17];
                  if (var18 >= 0 && var19 >= 0 && var18 < var1 && var19 < var2) {
                     int var20 = var19 * var1 + var18;
                     if (var0[var20] == 1 && var10[var20] < 0) {
                        var10[var20] = var11;
                        var12.add(var20);
                     }
                  }
               }
            }

            var11++;
         }
      }

      if (var11 < 2) {
         return var9;
      }

      int[] var24 = new int[512];
      int var25 = 0;

      for (int var26 = 8; var26 < var2 - 8 && var25 < 512; var26++) {
         for (int var28 = 8; var28 < var1 - 8 && var25 < 512; var28++) {
            int var30 = var26 * var1 + var28;
            if (var0[var30] == 1) {
               int var33 = 0;

               for (int var37 = 0; var37 < 8; var37++) {
                  int var40 = (var26 + b[var37]) * var1 + var28 + a[var37];
                  if (var0[var40] == 1) {
                     var33++;
                  }
               }

               if (var33 <= 1) {
                  var24[var25++] = var30;
               }
            }
         }
      }

      if (var25 < 2) {
         return var9;
      }

      long[] var27 = new long[64];
      int var29 = 0;

      for (int var31 = 0; var31 < var25; var31++) {
         for (int var34 = var31 + 1; var34 < var25; var34++) {
            if (var10[var24[var31]] != var10[var24[var34]]) {
               int var38 = Math.abs(var24[var31] % var1 - var24[var34] % var1);
               int var41 = Math.abs(var24[var31] / var1 - var24[var34] / var1);
               int var21 = Math.max(var38, var41);
               if (var21 >= 2 && var21 <= 8) {
                  if (var29 == var27.length) {
                     var27 = Arrays.copyOf(var27, var29 * 2);
                  }

                  var27[var29++] = (long)var21 << 42 | (long)var31 << 21 | var34;
               }
            }
         }
      }

      if (var29 == 0) {
         return var9;
      }

      Arrays.sort(var27, 0, var29);
      int[] var32 = new int[var11];
      int var35 = 0;

      while (var35 < var11) {
         var32[var35] = var35++;
      }

      for (int var36 = 0; var36 < var29; var36++) {
         int var39 = (int)(var27[var36] >> 21 & 2097151L);
         int var42 = (int)(var27[var36] & 2097151L);
         int var43 = a(var32, var10[var24[var39]]);
         int var22 = a(var32, var10[var24[var42]]);
         if (var43 != var22) {
            long[] var23 = a(var0, var1, var3, var4, var5, var6, var7, var24[var39], var24[var42], var8);
            if (var23 != null) {
               var8 = var23;
               var32[var43] = var22;
               lastBridges++;
            } else {
               lastBridgeRejected++;
            }
         }
      }

      return var8;
   }

   private static long[] a(byte[] var0, int var1, int var2, int var3, int var4, int var5, RiverFlowField.DeepChannelProbe var6, long[] var7) {
      lastDeepCells = 0;
      lastDeepProbes = 0;
      if (var6 == null) {
         return var7;
      }

       ArrayDeque<Long> var8 = new ArrayDeque<>();
      boolean[] var9 = new boolean[var1 * var2];
      boolean[] var10 = z ? new boolean[var1 * var2] : null;
      int var11 = 0;
      if (!z) {
         a(var0, var1, var2, var3, var4, var5, var6);
      }

      for (int var12 = 2; var12 < var2 - 2; var12++) {
         for (int var13 = 2; var13 < var1 - 2; var13++) {
            int var14 = var12 * var1 + var13;
            if (var0[var14] == 1) {
               int var15 = 0;

               for (int var16 = 0; var16 < 8; var16++) {
                  if (var0[(var12 + b[var16]) * var1 + var13 + a[var16]] == 1) {
                     var15++;
                  }
               }

               boolean var30 = var15 <= 1;

               for (int var17 = 0; var17 < 8; var17++) {
                  int var18 = (var12 + b[var17]) * var1 + var13 + a[var17];
                  if (var0[var18] == 0 && !var9[var18]) {
                     boolean var19 = var30;
                     if (!var19 && var11 < 4096) {
                        int var20 = var3 + (var13 + a[var17]) * var5;
                        int var21 = var4 + (var12 + b[var17]) * var5;
                        if (var6.floorClass(var20, var21) == 0) {
                           var11++;
                           var19 = var6.channelAt(var20, var21);
                        }
                     }

                     if (var19) {
                        var9[var18] = true;
                        var8.add(a(var18, 0, var17));
                     }
                  }
               }
            }
         }
      }

      int var26 = 0;
      int var27 = 0;
      boolean[] var28 = z ? null : new boolean[var1 * var2];

      while (!var8.isEmpty() && var26 < 4096 && var27 < 4096) {
         if (var28 != null && !var28[(int)(var8.peek() & 4294967295L)]) {
            a(var8, var28, var6, var3, var4, var5, var1, var2);
         }

         long var29 = (Long)var8.poll();
         int var31 = (int)(var29 & 4294967295L);
         int var32 = (int)(var29 >>> 36 & 15L);
         int var33 = (int)(var29 >>> 32 & 7L);
         int var34 = var31 % var1;
         int var35 = var31 / var1;
         var26++;
         if (var10 != null) {
            B.get()[0] = var10[var31] ? 1 : 0;
         }

         if (!b(var6, var3, var4, var5, var1, var2, var34, var35)) {
            if (var32 > 0) {
               int var36 = var34 + a[var33];
               int var37 = var35 + b[var33];
               if (var36 >= 1 && var37 >= 1 && var36 < var1 - 1 && var37 < var2 - 1) {
                  int var38 = var37 * var1 + var36;
                  if (var0[var38] == 0 && !var9[var38]) {
                     var9[var38] = true;
                     var8.add(a(var38, var32 - 1, var33));
                  }
               }
            }
         } else {
            var0[var31] = 1;
            if (var7 == null) {
               var7 = new long[var0.length + 63 >> 6];
            }

            var7[var31 >> 6] = var7[var31 >> 6] | 1L << (var31 & 63);
            var27++;

            for (int var22 = 0; var22 < 8; var22++) {
               int var23 = var34 + a[var22];
               int var24 = var35 + b[var22];
               if (var23 >= 1 && var24 >= 1 && var23 < var1 - 1 && var24 < var2 - 1) {
                  int var25 = var24 * var1 + var23;
                  if (var0[var25] == 0 && !var9[var25]) {
                     var9[var25] = true;
                     if (var10 != null) {
                        var10[var25] = true;
                     }

                     var8.add(a(var25, 0, var22));
                  }
               }
            }
         }
      }

      lastDeepCells = var27;
      lastDeepProbes = var26 + var11;
      if (z) {
         a();
      }

      return var7;
   }

   private static void a(byte[] var0, int var1, int var2, int var3, int var4, int var5, RiverFlowField.DeepChannelProbe var6) {
      int[] var7 = new int[var1 * var2];
      boolean[] var8 = new boolean[var1 * var2];
      int var9 = 0;

      for (int var10 = 2; var10 < var2 - 2; var10++) {
         for (int var11 = 2; var11 < var1 - 2; var11++) {
            int var12 = var10 * var1 + var11;
            if (var0[var12] == 1) {
               int var13 = 0;

               for (int var14 = 0; var14 < 8; var14++) {
                  if (var0[(var10 + b[var14]) * var1 + var11 + a[var14]] == 1) {
                     var13++;
                  }
               }

               if (var13 > 1) {
                  for (int var20 = 0; var20 < 8; var20++) {
                     int var15 = (var10 + b[var20]) * var1 + var11 + a[var20];
                     if (var0[var15] == 0 && !var8[var15]) {
                        var8[var15] = true;
                        var7[var9++] = var15;
                     }
                  }
               }
            }
         }
      }

      if (var9 != 0) {
         int[] var16 = var7;
         int[] var17 = new int[var9];
         LowResGrid.parallelFor(var9, var7x -> var17[var7x] = var6.floorClass(var3 + var16[var7x] % var1 * var5, var4 + var16[var7x] / var1 * var5));
         int[] var18 = new int[Math.min(var9, 4096)];
         int var19 = 0;

         for (int var21 = 0; var21 < var9 && var19 < var18.length; var21++) {
            if (var17[var21] == 0) {
               var18[var19++] = var16[var21];
            }
         }

         LowResGrid.parallelFor(var19, var6x -> var6.channelAt(var3 + var18[var6x] % var1 * var5, var4 + var18[var6x] / var1 * var5));
      }
   }

   private static void a(ArrayDeque<Long> var0, boolean[] var1, RiverFlowField.DeepChannelProbe var2, int var3, int var4, int var5, int var6, int var7) {
      int[] var8 = new int[256];
      int var9 = 0;

      for (long var11 : var0) {
         if (var9 == var8.length) {
            break;
         }

         int var13 = (int)(var11 & 4294967295L);
         if (!var1[var13]) {
            var1[var13] = true;
            var8[var9++] = var13;
         }
      }

      LowResGrid.parallelFor(var9, var7x -> a(var2, var3, var4, var5, var6, var7, var8[var7x] % var6, var8[var7x] / var6));
   }

   private static void a(RiverFlowField.DeepChannelProbe var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = var1 + var6 * var3;
      int var9 = var2 + var7 * var3;
      int var10 = var0.floorClass(var8, var9);
      if (var10 == 0) {
         var0.channelAt(var8, var9);

         for (int var11 = 0; var11 < 4; var11++) {
            int var12 = var8 + (var11 == 0 ? 3 : (var11 == 1 ? -3 : 0));
            int var13 = var9 + (var11 == 2 ? 3 : (var11 == 3 ? -3 : 0));
            if (var0.floorClass(var12, var13) == 0) {
               var0.channelAt(var12, var13);
            }
         }
      } else if (var10 == 1) {
         for (int var16 = 0; var16 < 2; var16++) {
            for (byte var17 = -1; var17 <= 1; var17 += 2) {
               for (int var18 = 1; var18 <= 3; var18++) {
                  int var14 = var6 + (var16 == 0 ? var18 * var17 : 0);
                  int var15 = var7 + (var16 == 1 ? var18 * var17 : 0);
                  if (var14 < 0 || var15 < 0 || var14 >= var4 || var15 >= var5) {
                     break;
                  }

                  var0.floorClass(var1 + var14 * var3, var2 + var15 * var3);
               }
            }
         }
      }
   }

   private static boolean a(RiverFlowField.DeepChannelProbe var0, int var1, int var2) {
      double var3 = var0.densityAt(var1, var2);
      boolean var5 = var3 < 0.0;
      StringBuilder var6 = A.get();
      var6.append(var1).append(',').append(var2).append(',').append(B.get()[0]).append(',').append(var3);

      for (int var7 = 0; var7 < 4; var7++) {
         int var8 = var1 + (var7 == 0 ? 3 : (var7 == 1 ? -3 : 0));
         int var9 = var2 + (var7 == 2 ? 3 : (var7 == 3 ? -3 : 0));
         double var10 = var0.floorClass(var8, var9) == 0 ? var0.densityAt(var8, var9) : Double.NaN;
         if (var10 < 0.0) {
            var5 = true;
         }

         var6.append(',').append(var10);
      }

      var6.append('\n');
      return var5;
   }

   private static void a() {
      StringBuilder var0 = A.get();
      if (var0.length() != 0) {
         synchronized (C) {
            try {
               Files.writeString(Path.of("streamsreflowing-deepprobe.csv"), var0, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException var4) {
            }
         }

         var0.setLength(0);
      }
   }

   private static boolean b(RiverFlowField.DeepChannelProbe var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = var1 + var6 * var3;
      int var9 = var2 + var7 * var3;
      int var10 = var0.floorClass(var8, var9);
      if (var10 == 0 && z) {
         return a(var0, var8, var9);
      }

      if (var10 == 0) {
         if (var0.channelAt(var8, var9)) {
            return true;
         }

         for (int var17 = 0; var17 < 4; var17++) {
            int var18 = var8 + (var17 == 0 ? 3 : (var17 == 1 ? -3 : 0));
            int var19 = var9 + (var17 == 2 ? 3 : (var17 == 3 ? -3 : 0));
            if (var0.floorClass(var18, var19) == 0 && var0.channelAt(var18, var19)) {
               return true;
            }
         }

         return false;
      } else {
         if (var10 == 2) {
            return false;
         }

         label107:
         for (int var11 = 0; var11 < 2; var11++) {
            for (byte var12 = -1; var12 <= 1; var12 += 2) {
               boolean var13 = false;

               for (int var14 = 1; var14 <= 3; var14++) {
                  int var15 = var6 + (var11 == 0 ? var14 * var12 : 0);
                  int var16 = var7 + (var11 == 1 ? var14 * var12 : 0);
                  if (var15 < 0 || var16 < 0 || var15 >= var4 || var16 >= var5) {
                     break;
                  }

                  if (var0.floorClass(var1 + var15 * var3, var2 + var16 * var3) == 0) {
                     var13 = true;
                     break;
                  }
               }

               if (!var13) {
                  continue label107;
               }
            }

            return true;
         }

         return false;
      }
   }

   private static long a(int var0, int var1, int var2) {
      return (long)var1 << 36 | (long)var2 << 32 | var0;
   }

   private static int a(int[] var0, int var1) {
      while (var0[var1] != var1) {
         var0[var1] = var0[var0[var1]];
         var1 = var0[var1];
      }

      return var1;
   }

   private static long[] a(
      byte[] var0,
      int var1,
      int var2,
      int var3,
      int var4,
      RiverFlowField.OrientationField var5,
      RiverFlowField.WaterProbe var6,
      int var7,
      int var8,
      long[] var9
   ) {
      int var10 = var7 % var1;
      int var11 = var7 / var1;
      int var12 = var8 % var1;
      int var13 = var8 / var1;
      int var14 = Math.max(Math.abs(var12 - var10), Math.abs(var13 - var11));
      int[] var15 = new int[Math.max(0, var14 - 1)];
      int var16 = 0;
      int var17 = 0;

      for (int var18 = 1; var18 < var14; var18++) {
         int var19 = var10 + Math.round((float)(var12 - var10) * var18 / var14);
         int var20 = var11 + Math.round((float)(var13 - var11) * var18 / var14);
         int var21 = var20 * var1 + var19;
         var15[var16++] = var21;
         if (var0[var21] != 1 && var0[var21] != 2) {
            if (var6 != null) {
               lastBridgeSamples++;
               if (var6.isWater(var2 + var19 * var4, var3 + var20 * var4)) {
                  continue;
               }
            }

            if (++var17 > 4) {
               return null;
            }
         }
      }

      if (var5 != null) {
         double var24 = var5.sample(var2 + var10 * var4, var3 + var11 * var4);
         double var28 = var5.sample(var2 + var12 * var4, var3 + var13 * var4);
         double var22 = var5.sample(var2 + (var10 + var12) / 2 * var4, var3 + (var11 + var13) / 2 * var4);
         if (var22 > Math.max(var24, var28) + 1.0E-9) {
            return null;
         }
      } else if (var17 > 0 || var6 != null) {
         for (int var25 = 0; var25 < var16; var25++) {
            if (var0[var15[var25]] != 1 && var0[var15[var25]] != 2) {
               return null;
            }
         }
      }

      for (int var26 = 0; var26 < var16; var26++) {
         int var27 = var15[var26];
         if (var0[var27] != 1) {
            var0[var27] = 1;
            if (var9 == null) {
               var9 = new long[var0.length + 63 >> 6];
            }

            var9[var27 >> 6] = var9[var27 >> 6] | 1L << (var27 & 63);
            lastBridgedCells++;
         }
      }

      return var9 == null ? new long[var0.length + 63 >> 6] : var9;
   }

   private static long a(float var0, int var1) {
      return (long)Float.floatToIntBits(var0) << 32 | var1 & 4294967295L;
   }

   private static int a(float[] var0, int var1, int var2, int var3, int var4) {
      double var5 = 0.0;
      double var7 = 0.0;
      double var9 = 0.0;
      double var11 = 0.0;

      for (int var13 = -2; var13 <= 2; var13++) {
         for (int var14 = -2; var14 <= 2; var14++) {
            int var15 = var3 + var14;
            int var16 = var4 + var13;
            if (var15 >= 0 && var16 >= 0 && var15 < var1 && var16 < var2) {
               float var17 = var0[var16 * var1 + var15];
               if (var17 != Float.POSITIVE_INFINITY) {
                  var5++;
                  var7 += var14;
                  var9 += var13;
                  var11 += var17;
               }
            }
         }
      }

      if (var5 < 2.0) {
         return -1;
      }

      double var45 = var7 / var5;
      double var46 = var9 / var5;
      double var47 = var11 / var5;
      double var19 = 0.0;
      double var21 = 0.0;
      double var23 = 0.0;
      double var25 = 0.0;
      double var27 = 0.0;

      for (int var29 = -2; var29 <= 2; var29++) {
         for (int var30 = -2; var30 <= 2; var30++) {
            int var31 = var3 + var30;
            int var32 = var4 + var29;
            if (var31 >= 0 && var32 >= 0 && var31 < var1 && var32 < var2) {
               float var33 = var0[var32 * var1 + var31];
               if (var33 != Float.POSITIVE_INFINITY) {
                  double var34 = var30 - var45;
                  double var36 = var29 - var46;
                  double var38 = var33 - var47;
                  var19 += var34 * var34;
                  var21 += var34 * var36;
                  var23 += var36 * var36;
                  var25 += var34 * var38;
                  var27 += var36 * var38;
               }
            }
         }
      }

      double var48 = 0.001 * (var19 + var23) + 1.0E-6;
      double var49 = var19 + var48;
      double var50 = var23 + var48;
      double var35 = var49 * var50 - var21 * var21;
      if (var35 <= 1.0E-12) {
         return -1;
      }

      double var37 = (var50 * var25 - var21 * var27) / var35;
      double var39 = (var49 * var27 - var21 * var25) / var35;
      double var41 = -var37;
      double var43 = -var39;
      return var41 * var41 + var43 * var43 < 1.0E-9 ? -1 : a(var41, var43);
   }

   private static int a(double var0, double var2) {
      int var4 = (int)Math.round(Math.atan2(var2, var0) / (Math.PI / 4));
      return (var4 % 8 + 8) % 8;
   }

   public int spacing() {
      return this.j;
   }

   public int dirAt(int var1, int var2) {
      long var3 = this.a(var1, var2);
      return var3 == 9221120239188049920L ? -1 : a(a(var3), b(var3));
   }

   public double flowRadAt(int var1, int var2) {
      long var3 = this.a(var1, var2);
      return var3 == 9221120239188049920L ? Double.NaN : Math.atan2(b(var3), a(var3));
   }

   private long a(int var1, int var2) {
      double var3 = (double)(var1 - this.h) / this.j;
      double var5 = (double)(var2 - this.i) / this.j;
      int var7 = (int)Math.floor(var3);
      int var8 = (int)Math.floor(var5);
      double var9 = 0.0;
      double var11 = 0.0;
      double var13 = 0.0;

      for (int var15 = -3; var15 <= 3; var15++) {
         for (int var16 = -3; var16 <= 3; var16++) {
            int var17 = var7 + var16;
            int var18 = var8 + var15;
            if (var17 >= 0 && var18 >= 0 && var17 < this.k && var18 < this.l) {
               byte var19 = this.m[var18 * this.k + var17];
               if (var19 >= 0) {
                  double var20 = var17 - var3;
                  double var22 = var18 - var5;
                  double var24 = 1.0 / (1.0 + var20 * var20 + var22 * var22);
                  var9 += f[var19] * var24;
                  var11 += g[var19] * var24;
                  var13 += var24;
               }
            }
         }
      }

      return var13 <= 0.0 ? 9221120239188049920L : (long)Float.floatToRawIntBits((float)var9) << 32 | Float.floatToRawIntBits((float)var11) & 4294967295L;
   }

   private static double a(long var0) {
      return Float.intBitsToFloat((int)(var0 >>> 32));
   }

   private static double b(long var0) {
      return Float.intBitsToFloat((int)var0);
   }

   static {
      for (int var0 = 0; var0 < 8; var0++) {
         f[var0] = Math.cos(var0 * (Math.PI / 4));
         g[var0] = Math.sin(var0 * (Math.PI / 4));
      }

      lastPhaseMs = new double[6];
      z = System.getProperty("streamsreflowing.deepprobecsv") != null;
      A = ThreadLocal.withInitial(StringBuilder::new);
      B = ThreadLocal.withInitial(() -> new int[1]);
      C = new Object();
   }

   public interface BiomeClass {
      int classify(int var1, int var2);
   }

   public interface DeepChannelProbe {
      boolean channelAt(int var1, int var2);

      default int verdictAt(int var1, int var2) {
         return this.channelAt(var1, var2) ? 0 : 2;
      }

      default int floorClass(int var1, int var2) {
         return 2;
      }

      default double densityAt(int var1, int var2) {
         return this.channelAt(var1, var2) ? -1.0 : 1.0;
      }
   }

   public interface NeighbourFlow {
      int dirAt(int var1, int var2);
   }

   public interface OrientationField {
      double sample(int var1, int var2);
   }

   public interface WaterProbe {
      boolean isWater(int var1, int var2);
   }
}
