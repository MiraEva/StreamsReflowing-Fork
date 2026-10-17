package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;

public final class RiverFlowFields {
   private static volatile int a = -1;
   private static final int b = 512;
   private static final int c = 8;
   private static final int d = 256;
   private static final ConcurrentHashMap<Long, RiverFlowField> e = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<Long> f = new ConcurrentLinkedQueue<>();
   private static volatile RiverFlowField.WaterProbe g;
   private static volatile RiverFlowField.DeepChannelProbe h;
   public static final LongAdder DEEP_DENSITY_COMPUTED = new LongAdder();
   public static final LongAdder DEEP_DENSITY_MEMO = new LongAdder();
   private static final int i = 1397900849;
   private static final int j = 29;
   private static volatile Path k;
   private static volatile long l;
   private static final Map<ServerLevel, RiverFlowField.BiomeClass> m = new ConcurrentHashMap<>();
   private static final int n = 262144;
   private static final List<WeakReference<ConcurrentHashMap<Long, Byte>>> o = new CopyOnWriteArrayList<>();
   private static final int[] p = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] q = new int[]{0, 1, 1, 1, 0, -1, -1, -1};

   private static int a() {
      int var0 = a;
      if (var0 <= 0) {
         var0 = switch (Services.PLATFORM.riverDrainageSize()) {
            case 2 -> 1024;
            case 3 -> 2048;
            default -> 512;
         };
         a = var0;
      }

      return var0;
   }

   private RiverFlowFields() {
   }

   public static int dirAt(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      RiverFlowField var4 = a(Math.floorDiv(var2, a()), Math.floorDiv(var3, a()));
      return var4 == null ? -1 : var4.dirAt(var2, var3);
   }

   public static double flowRadAt(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      RiverFlowField var4 = a(Math.floorDiv(var2, a()), Math.floorDiv(var3, a()));
      return var4 == null ? Double.NaN : var4.flowRadAt(var2, var3);
   }

   public static boolean bridgedNear(int var0, int var1) {
      RiverFlowField var2 = a(Math.floorDiv(var0, a()), Math.floorDiv(var1, a()));
      return var2 != null && var2.bridgedNear(var0, var1);
   }

   public static void setWaterProbe(RiverFlowField.WaterProbe var0) {
      g = var0;
   }

   public static void setDeepChannelProbe(RiverFlowField.DeepChannelProbe var0) {
      h = var0;
   }

   public static boolean deepChannelAt(int var0, int var1) {
      RiverFlowField.DeepChannelProbe var2 = h;
      return var2 != null && var2.channelAt(var0, var1);
   }

   public static boolean deepProbeReady() {
      return h != null;
   }

   public static void deepMap(int var0, int var1, int var2) {
      RiverFlowField.DeepChannelProbe var3 = h;
      if (var3 == null) {
         Constants.LOG.info("[SR-DEEPMAP] no underground probe on this world (its generator cannot be asked)");
      } else {
         int var4 = Math.max(4, Math.min(48, var2));
         byte var5 = 8;
         Constants.LOG.info("[SR-DEEPMAP] underground channel around ({},{}), one char per {} blocks", new Object[]{var0, var1, Integer.valueOf(var5)});
         Constants.LOG
            .info(
               "[SR-DEEPMAP] '#'=roofed channel  'o'=open channel (shallow, bracketed by banks)  'r'=open shallow water, unbracketed  '~'=deep water  '.'=solid at river level  '+'=the column you asked about"
            );
         StringBuilder var6 = new StringBuilder();

         for (int var7 = -var4; var7 <= var4; var7++) {
            var6.setLength(0);

            for (int var8 = -var4; var8 <= var4; var8++) {
               int var9 = var0 + var8 * var5;
               int var10 = var1 + var7 * var5;

               char var11 = switch (var3.verdictAt(var9, var10)) {
                  case 0 -> '#';
                  case 1 -> {
                     int var12 = var3.floorClass(var9, var10);
                     if (var12 == 2) {
                        yield '~';
                     } else {
                        if (var12 != 1) {
                           yield 'r';
                        }

                        boolean var13 = false;

                        for (int var14 = 0; var14 < 2 && !var13; var14++) {
                           boolean var15 = false;
                           boolean var16 = false;

                           for (int var17 = 1; var17 <= 3; var17++) {
                              if (var3.floorClass(var9 - (var14 == 0 ? var17 * var5 : 0), var10 - (var14 == 1 ? var17 * var5 : 0)) == 0) {
                                 var15 = true;
                              }

                              if (var3.floorClass(var9 + (var14 == 0 ? var17 * var5 : 0), var10 + (var14 == 1 ? var17 * var5 : 0)) == 0) {
                                 var16 = true;
                              }
                           }

                           var13 = var15 && var16;
                        }

                        yield var13 ? 'o' : 'r';
                     }
                  }
                  default -> '.';
               };
               if (var8 == 0 && var7 == 0) {
                  var11 = '+';
               }

               var6.append(var11);
            }

            Constants.LOG.info("[SR-DEEPMAP] {}", var6);
         }
      }
   }

   private static RiverFlowField a(int var0, int var1) {
      return e.get((long)var0 << 32 ^ var1 & 4294967295L);
   }

   public static void initStore(Path var0, long var1) {
      k = var0;
      l = var1;
   }

   private static RiverFlowField b(int var0, int var1) {
      Path var2 = k;
      if (var2 == null) {
         return null;
      }

      Path var3 = var2.resolve("f." + var0 + "." + var1 + ".srf");
      if (!Files.isRegularFile(var3)) {
         return null;
      }

      try (DataInputStream var4 = new DataInputStream(new BufferedInputStream(Files.newInputStream(var3)))) {
         return var4.readInt() != 1397900849 || var4.readInt() != 29 || var4.readLong() != l || var4.readInt() != a() ? null : RiverFlowField.readFrom(var4);
      } catch (IOException | RuntimeException var9) {
         return null;
      }
   }

   private static void a(int var0, int var1, RiverFlowField var2) {
      Path var3 = k;
      if (var3 != null) {
         try {
            Files.createDirectories(var3);
            Path var4 = var3.resolve("f." + var0 + "." + var1 + ".srf");
            Path var5 = var3.resolve("f." + var0 + "." + var1 + ".srf.tmp");

            try (DataOutputStream var6 = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(var5)))) {
               var6.writeInt(1397900849);
               var6.writeInt(29);
               var6.writeLong(l);
               var6.writeInt(a());
               var2.writeTo(var6);
            }

            Files.move(var5, var4, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException | RuntimeException var11) {
            Constants.LOG.debug("Flow-field save failed for region ({}, {}): {}", new Object[]{var0, var1, var11.toString()});
         }
      }
   }

   private static RiverFlowField a(int var0, int var1, RiverFlowField.BiomeClass var2, RiverFlowField.OrientationField var3) {
      long var4 = (long)var0 << 32 ^ var1 & 4294967295L;
      RiverFlowField[] var6 = new RiverFlowField[1];
      RiverFlowField var7 = e.computeIfAbsent(var4, var5 -> {
         RiverFlowField var6x = b(var0, var1);
         RiverFlowField var7x = var6x != null ? var6x : b(var0, var1, var2, var3);
         if (var6x == null) {
            RiverFlowField var8x = var7x.reconciledWith(a(var0, var1, true));
            if (var8x != var7x) {
               Constants.LOG.debug("Flow field ({}, {}) re-signed {} cells to agree with a neighbour", new Object[]{var0, var1, RiverFlowField.lastReconciled});
               var7x = var8x;
               StreamFlowGrid.invalidateArea(var0 * a(), var1 * a(), a());
            }

            a(var0, var1, var7x);
            var6[0] = var7x;
         }

         f.add(var5);
         return var7x;
      });
      if (var6[0] != null) {
         b(var0, var1, var6[0]);
      }

      while (e.size() > 256) {
         Long var8 = f.poll();
         if (var8 == null) {
            break;
         }

         e.remove(var8);
      }

      return var7;
   }

   private static RiverFlowField.NeighbourFlow c(int var0, int var1) {
      return a(var0, var1, false);
   }

   private static RiverFlowField.NeighbourFlow a(int var0, int var1, boolean var2) {
      if (!Services.PLATFORM.riverSeamAgreement()) {
         return null;
      }

      RiverFlowField[] var3 = new RiverFlowField[8];
      int var4 = 0;

      for (int var5 = -1; var5 <= 1; var5++) {
         for (int var6 = -1; var6 <= 1; var6++) {
            if ((var6 != 0 || var5 != 0) && (!var2 || var5 <= 0 && (var5 != 0 || var6 <= 0))) {
               RiverFlowField var7 = a(var0 + var6, var1 + var5);
               if (var7 == null) {
                  var7 = b(var0 + var6, var1 + var5);
               }

               if (var7 != null) {
                  var3[var4++] = var7;
               }
            }
         }
      }

      if (var4 == 0) {
         return null;
      }

      RiverFlowField[] var8 = Arrays.copyOf(var3, var4);
      return (var1x, var2x) -> {
         for (RiverFlowField var6x : var8) {
            int var7x = var6x.dirAt(var1x, var2x);
            if (var7x >= 0) {
               return var7x;
            }
         }

         return -1;
      };
   }

   private static void b(int var0, int var1, RiverFlowField var2) {
      if (Services.PLATFORM.riverSeamAgreement()) {
         RiverFlowField.NeighbourFlow var3 = var2::dirAt;

         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               if (var4 >= 0 && (var4 != 0 || var5 > 0)) {
                  int var6 = var0 + var5;
                  int var7 = var1 + var4;
                  RiverFlowField var8 = a(var6, var7);
                  if (var8 != null) {
                     RiverFlowField var9 = var8.reconciledWith(var3);
                     if (var9 != var8) {
                        Constants.LOG
                           .debug(
                              "Flow field ({}, {}) re-signed {} cells to agree with ({}, {})",
                              new Object[]{var6, var7, RiverFlowField.lastReconciled, var0, var1}
                           );
                        e.put((long)var6 << 32 ^ var7 & 4294967295L, var9);
                        a(var6, var7, var9);
                        StreamFlowGrid.invalidateArea(var6 * a(), var7 * a(), a());
                     }
                  }
               }
            }
         }
      }
   }

   private static String a(int var0) {
      double[] var1 = RiverFlowField.lastPhaseMs;
      return var1 != null && var0 < var1.length ? String.format(Locale.ROOT, "%.0f", var1[var0]) : "?";
   }

   private static RiverFlowField b(int var0, int var1, RiverFlowField.BiomeClass var2, RiverFlowField.OrientationField var3) {
      long var4 = System.nanoTime();
      byte var6 = 2;
      RiverFlowField var7 = RiverFlowField.build(var0 * a(), var1 * a(), a(), a(), 512, 8, var6, var2, var3, g, h, c(var0, var1));
      double var8 = (System.nanoTime() - var4) / 1000000.0;
      FlowTrace.fieldBuilds.incrementAndGet();
      FlowTrace.fieldBuildMs.addAndGet((long)var8);
      StreamFlowGrid.invalidateArea(var0 * a(), var1 * a(), a());
      int var10 = RiverFlowField.lastSampled;
      int var11 = RiverFlowField.lastCells;
      String var12 = "Built river-flow field ({}, {}): {} ocean, {} river, {} flowed cells (spacing {}), {} of {} cells classified ({}%), {} ms; bridges {} ({} cells, {} rejected, {} probes); deep {} cells / {} probes (density reads this session: {} computed, {} from memo); seam {} shared cells; phases ms: classify {} / deep {} / bridge {} / flood {} / bake {} / orient {}";
      Object[] var13 = new Object[]{
         var0,
         var1,
         var7.oceanCells,
         var7.riverCells,
         var7.flowedCells,
         8,
         var10,
         var11,
         var11 == 0 ? 0 : 100 * var10 / var11,
         String.format("%.1f", var8),
         RiverFlowField.lastBridges,
         RiverFlowField.lastBridgedCells,
         RiverFlowField.lastBridgeRejected,
         RiverFlowField.lastBridgeSamples,
         RiverFlowField.lastDeepCells,
         RiverFlowField.lastDeepProbes,
         DEEP_DENSITY_COMPUTED.sum(),
         DEEP_DENSITY_MEMO.sum(),
         RiverFlowField.lastSeamCells,
         a(0),
         a(1),
         a(2),
         a(3),
         a(4),
         a(5)
      };
      if (Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info(var12, var13);
      } else {
         Constants.LOG.debug(var12, var13);
      }

      return var7;
   }

   public static long[] memoryStats() {
      long var0 = 0L;
      long var2 = 0L;

      for (RiverFlowField var5 : e.values()) {
         var0++;
         var2 += var5.approxBytes();
      }

      var2 += MemBytes.hashMapNodes(var0) + var0 * 40L;
      long var10 = 0L;

      for (WeakReference var7 : o) {
         ConcurrentHashMap var8 = (ConcurrentHashMap)var7.get();
         if (var8 != null) {
            var10 += var8.size();
         }
      }

      return new long[]{var0, var2, var10, MemBytes.hashMapNodes(var10) + var10 * 40L};
   }

   public static RiverFlowField.BiomeClass classifierFor(ServerLevel var0) {
      return m.computeIfAbsent(var0, RiverFlowFields::a);
   }

   private static RiverFlowField.BiomeClass a(ServerLevel var0) {
      ChunkGenerator var1 = var0.getChunkSource().getGenerator();
      RandomState var2 = var0.getChunkSource().randomState();
      return newClassifier(var1.getBiomeSource(), var2.sampler(), var1.getSeaLevel());
   }

   public static RiverFlowField.BiomeClass newClassifier(BiomeSource var0, Sampler var1, int var2) {
      int var3 = QuartPos.fromBlock(var2);
      ConcurrentHashMap var4 = new ConcurrentHashMap();
      o.removeIf(var0x -> var0x.get() == null);
      o.add(new WeakReference<>(var4));
      AtomicInteger var5 = new AtomicInteger();
      byte var6 = 13;
      short var7 = 8191;
      ThreadLocal var8 = ThreadLocal.withInitial(() -> {
         long[] var0x = new long[8192];
         Arrays.fill(var0x, Long.MIN_VALUE);
         return var0x;
      });
      ThreadLocal var9 = ThreadLocal.withInitial(() -> new byte[8192]);
      return (var7x, var8x) -> {
         int var9x = var7x >> 2;
         int var10 = var8x >> 2;
         long var11 = (long)var9x << 32 ^ var10 & 4294967295L;
         long[] var13 = (long[])var8.get();
         byte[] var14 = (byte[])var9.get();
         int var15 = (int)(var11 ^ var11 >>> 32) & 8191;
         if (var13[var15] == var11) {
            return var14[var15];
         }

         Byte var16 = (Byte)var4.get(var11);
         if (var16 != null) {
            var14[var15] = var16;
            var13[var15] = var11;
            return var16;
         }

         Holder var17 = var0.getNoiseBiome(var9x, var3, var10, var1);
         int var18 = var17.is(BiomeTags.IS_OCEAN)
            ? 2
            : (!var17.is(BiomeTags.IS_RIVER) && !var17.is(Biomes.RIVER) && !var17.is(Biomes.FROZEN_RIVER) ? 0 : 1);
         var14[var15] = (byte)var18;
         var13[var15] = var11;
         if (var4.putIfAbsent(var11, Byte.valueOf((byte)var18)) == null && (var5.incrementAndGet() & 4095) == 0 && var4.size() > 262144) {
            var4.clear();
         }

         return var18;
      };
   }

   public static void unloadLevel(ServerLevel var0) {
      m.remove(var0);
   }

   public static RiverFlowField.OrientationField orientationFor(ServerLevel var0) {
      ChunkGenerator var1 = var0.getChunkSource().getGenerator();
      if (!(var1 instanceof NoiseBasedChunkGenerator)) {
         return null;
      }

      DensityFunction var2;
      try {
         var2 = var0.getChunkSource().randomState().router().continents();
      } catch (Throwable var4) {
         return null;
      }

      int var3 = var1.getSeaLevel();
      return (var2x, var3x) -> var2.compute(new SinglePointContext(var2x, var3, var3x));
   }

   public static int regionSize() {
      return a();
   }

   public static boolean isRegionReady(int var0, int var1) {
      long var2 = (long)Math.floorDiv(var0, a()) << 32 ^ Math.floorDiv(var1, a()) & 4294967295L;
      return e.containsKey(var2);
   }

   public static void ensureRegion(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      a(Math.floorDiv(var2, a()), Math.floorDiv(var3, a()), var0, var1);
   }

   public static long owningRegionKey(int var0, int var1) {
      return (long)Math.floorDiv(var0, a()) << 32 ^ Math.floorDiv(var1, a()) & 4294967295L;
   }

   public static void flowMap(int var0, int var1, int var2) {
      int var3 = Math.max(4, Math.min(48, var2));
      RiverFlowField var4 = a(Math.floorDiv(var0, a()), Math.floorDiv(var1, a()));
      if (var4 == null) {
         Constants.LOG.info("[SR-FLOWMAP] no river-current field built yet around ({},{}) -- it builds in the background; try again in a moment.", var0, var1);
      } else {
         Constants.LOG
            .info("[SR-FLOWMAP] river currents around ({},{}), one char per {} blocks, region size {}", new Object[]{var0, var1, var4.spacing(), a()});
         Constants.LOG
            .info(
               "[SR-FLOWMAP] E=east e=SE S=south s=SW W=west w=NW N=north n=NE  '.'=no current  UPPERCASE-X marks a cell a neighbour flows straight back into (a split/collision)"
            );
         String var5 = "EeSsWwNn";

         for (int var6 = -var3; var6 <= var3; var6++) {
            StringBuilder var7 = new StringBuilder();

            for (int var8 = -var3; var8 <= var3; var8++) {
               int var9 = var0 + var8 * var4.spacing();
               int var10 = var1 + var6 * var4.spacing();
               int var11 = var4.dirAt(var9, var10);
               if (var11 < 0) {
                  var7.append('.');
               } else {
                  boolean var12 = false;

                  for (int var13 = 0; var13 < 8 && !var12; var13++) {
                     int var14 = var4.dirAt(var9 + p[var13] * var4.spacing(), var10 + q[var13] * var4.spacing());
                     if (var14 >= 0) {
                        int var15 = var14 - var11 & 7;
                        var12 = var15 >= 3 && var15 <= 5;
                     }
                  }

                  var7.append(var12 ? '#' : var5.charAt(var11));
               }
            }

            Constants.LOG.info("[SR-FLOWMAP] {}", var7);
         }
      }
   }

   public static void clear() {
      e.clear();
      f.clear();
      m.clear();
      g = null;
      h = null;
      k = null;
   }
}
