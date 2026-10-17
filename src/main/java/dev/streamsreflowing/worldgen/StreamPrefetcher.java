package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.build.BuildGate;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.core.terrain.ChunkHeightCache;
import dev.streamsreflowing.core.terrain.ReadLocality;
import dev.streamsreflowing.core.terrain.SampleClock;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.offload.PlateOffload;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

public final class StreamPrefetcher {
   private static final int a = Math.max(
      1, Math.min(Runtime.getRuntime().availableProcessors() - 2, Math.max(2, Runtime.getRuntime().availableProcessors() / 2))
   );
   private static final ThreadPoolExecutor b = new ThreadPoolExecutor(a, a, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), var0 -> {
      Thread var1 = new Thread(var0, "streamsreflowing-prefetch");
      var1.setDaemon(true);
      var1.setPriority(1);
      return var1;
   });
   private static final int c = 2;
   private static final ThreadPoolExecutor d = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), var0 -> {
      Thread var1 = new Thread(var0, "streamsreflowing-urgent");
      var1.setDaemon(true);
      var1.setPriority(5);
      return var1;
   });
   private static final AtomicLong e = new AtomicLong();
   private static final AtomicInteger f = new AtomicInteger();
   private static final int g = 3;
   private static final Semaphore h = new Semaphore(3);
   private static final boolean i = !Boolean.getBoolean("streamsreflowing.nourgentlane");
   private static final int j = 60;
   private static final boolean k = Boolean.getBoolean("streamsreflowing.nojoingrace");
   private static final Object l = new Object();
   private static final int m = -1;
   private static final int n = 0;
   private static final int o = 1;
   private static final int p = 2;
   private static final int q = 256;
   private static final Set<Long> r = ConcurrentHashMap.newKeySet();
   private static final Set<Long> s = ConcurrentHashMap.newKeySet();
   private static final Set<Long> t = ConcurrentHashMap.newKeySet();
   private static final Set<Long> u = ConcurrentHashMap.newKeySet();
   private static final int v = 128;
   private static final ConcurrentHashMap<Long, Long> w = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Long, Runnable> x = new ConcurrentHashMap<>();
   private static final int y = 12;
   private static final int z = 8;
   private static final double A = 25.0;
   private static final Map<UUID, ArrayDeque<double[]>> B = new HashMap<>();
   private static final int C = 400;
   private static final int D = 160;
   private static final double E = 32.0;
   private static final double F = 1600.0;
   private static final double G = 1.25;
   private static final double H = 1.6;
   private static long I;
   private static int J;
   private static final boolean K = Boolean.getBoolean("streamsreflowing.noprefetch");
   private static final int[] L = a(System.getProperty("streamsreflowing.deepmeasure"));
   private static volatile boolean M;
   private static final double N = 42.0;
   private static volatile int O = -1;
   private static final Set<Long> P = ConcurrentHashMap.newKeySet();
   private static Set<Long> Q = Set.of();
   private static volatile boolean R;
   private static final int S = 480;
   private static final int T = 3;
   private static final int U = 10;
   private static final double V = 0.5;
   private static final int[] W = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] X = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final int Y = Integer.getInteger("streamsreflowing.lanescan", 256);
   private static final int[] Z = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] aa = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final AtomicBoolean ab = new AtomicBoolean(false);

   private static void a() {
      synchronized (l) {
         int var1 = b.getMaximumPoolSize();
         if (var1 < Math.min(a, 4) && b.getActiveCount() >= var1) {
            int var2 = var1 + 1;
            b.setMaximumPoolSize(var2);
            b.setCorePoolSize(var2);
         }
      }
   }

   private StreamPrefetcher() {
   }

   private static int[] a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim()), var1.length > 2 ? Integer.parseInt(var1[2].trim()) : 2};
         } catch (RuntimeException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void onServerTick(MinecraftServer var0) {
      PlateOffload.onServerTick(var0);
      J++;
      FlowTrace.maybeReport();
      MemoryReport.maybeReport(var0);
      if ((J & 1023) == 0) {
         RawFlowTileStore.flushAsync();
      }

      if (L != null && !M && var0.isReady() && RiverFlowFields.deepProbeReady()) {
         ServerLevel var1 = var0.getLevel(Level.OVERWORLD);
         if (var1 != null) {
            M = true;
            RiverFlowField.BiomeClass var2 = RiverFlowFields.classifierFor(var1);
            RiverFlowField.OrientationField var3 = RiverFlowFields.orientationFor(var1);
            int var4 = RiverFlowFields.regionSize();
            int var5 = L[2];

            for (int var6 = -var5; var6 <= var5; var6++) {
               for (int var7 = -var5; var7 <= var5; var7++) {
                  a(var2, var3, L[0] + var7 * var4, L[1] + var6 * var4, 1);
               }
            }

            Constants.LOG.info("[SR-DEEPMEASURE] requested {} river-flow regions around ({},{})", new Object[]{(2 * var5 + 1) * (2 * var5 + 1), L[0], L[1]});
         }
      }

      if (!K) {
         if (!Services.PLATFORM.generateRivers()) {
            if (Services.PLATFORM.flowVanillaRivers()) {
               for (ServerLevel var36 : var0.getAllLevels()) {
                  if (var36.dimension().equals(Level.OVERWORLD) && !var36.players().isEmpty()) {
                     a(var36);
                  }
               }
            }
         } else {
            boolean var29 = !x.isEmpty();
            if (var29 && (J & 3) == 0) {
               byte var31 = 12;
               Iterator var37 = x.values().iterator();
                ArrayList<Runnable> var42 = new ArrayList<>(var31);

               while (var37.hasNext() && var42.size() < var31) {
                  var42.add((Runnable)var37.next());
                  var37.remove();
               }

               for (Runnable var53 : var42) {
                  var53.run();
               }
            }

            if ((J & 31) == 0) {
               if (!B.isEmpty()) {
                  HashSet var32 = new HashSet();

                  for (ServerPlayer var43 : var0.getPlayerList().getPlayers()) {
                     var32.add(var43.getUUID());
                  }

                  B.keySet().retainAll(var32);
               }

               ArrayList var33 = new ArrayList();

               for (ServerLevel var44 : var0.getAllLevels()) {
                  if (var44.dimension().equals(Level.OVERWORLD)) {
                     for (ServerPlayer var54 : var44.players()) {
                        var33.add(new int[]{var54.blockPosition().getX(), var54.blockPosition().getZ()});
                     }
                  }
               }

               int[] var40 = new int[var33.size() * 2];

               for (int var45 = 0; var45 < var33.size(); var45++) {
                  var40[var45 * 2] = ((int[])var33.get(var45))[0];
                  var40[var45 * 2 + 1] = ((int[])var33.get(var45))[1];
               }

               LowResGrid.setFocusPoints(var40);
               int var34 = 0;
               boolean var41 = false;
               if (!k) {
                  for (ServerLevel var51 : var0.getAllLevels()) {
                     if (var51.dimension().equals(Level.OVERWORLD)) {
                        for (ServerPlayer var57 : var51.players()) {
                           if (var57.tickCount < 60) {
                              var41 = true;
                              break;
                           }
                        }
                     }
                  }
               }

               for (ServerLevel var52 : var0.getAllLevels()) {
                  if (var52.dimension().equals(Level.OVERWORLD)) {
                     RiverEngine var56 = StreamsWorldgen.engine(var52);
                     if (!var52.players().isEmpty()) {
                        int var58 = (int)Math.max(256.0, var56.config().plateScale());
                        int var8 = Math.min(var58, Math.max(160, var0.getPlayerList().getViewDistance() * 16));
                         HashMap<PlateKey, Double> var9 = new HashMap<>();
                         HashMap<PlateKey, Double> var10 = new HashMap<>();

                        for (ServerPlayer var12 : var52.players()) {
                           a(var56, var12.blockPosition(), var8);
                           if (!var41) {
                              double[] var13 = a(var12);
                              a(var56, var12.blockPosition(), var13, var58, var9, var10);
                              if (var34 < 2) {
                                 var34 = Math.max(var34, a(var56, var12, var58));
                              }
                           }
                        }

                        if (var41) {
                           a(var52);
                           a(var52, var56, var0);
                        } else {
                           d();
                           a(var56, var52, Services.PLATFORM.prefetchRadius());
                           PlateKey var59 = null;
                           double var60 = Double.MAX_VALUE;

                            for (Entry<PlateKey, Double> var15 : var9.entrySet()) {
                               if (var15.getValue() < var60) {
                                  var60 = var15.getValue();
                                  var59 = var15.getKey();
                              }
                           }

                            Map<PlateKey, Double> var61 = var59 != null && var56.isPlateCached(var59) ? var10 : var9;
                            ArrayList<Entry<PlateKey, Double>> var62 = new ArrayList<>(var61.entrySet());
                           var62.sort(Entry.comparingByValue());

                            for (Entry<PlateKey, Double> var17 : var62) {
                               a(var56, var17.getKey(), 0);
                           }

                           ServerLevel var63 = var0.getLevel(Level.NETHER);
                           if (var63 != null && !var63.players().isEmpty()) {
                              double var64 = var63.dimensionType().coordinateScale();

                              for (ServerPlayer var20 : var63.players()) {
                                 double var21 = var20.getX() * var64;
                                 double var23 = var20.getZ() * var64;

                                 for (PlateKey var28 : var56.platesNear(var21, var23, 0.0)) {
                                    a(var56, var28, 0);
                                 }
                              }
                           }

                           a(var56, var52);
                           a(var52);
                           a(var52, var56, var0);
                        }
                     }
                  }
               }

               int var35 = Math.max(0, var34 - b());
               if (var35 >= 1 && var35 <= 2 && a(var0) > 42.0) {
                  var35--;
               }

               int var48 = x.size();
               if (!var41 && (var29 || var48 > 0)) {
                  var35 = Math.max(var35, 2);
               }

               a(var35);
            }
         }
      }
   }

   private static double a(MinecraftServer var0) {
      return var0.getAverageTickTime();
   }

   private static int b() {
      long var0 = System.nanoTime();
      long var2 = I;
      I = var0;
      if (var2 == 0L) {
         return 0;
      } else {
         double var4 = (var0 - var2) / 1000000.0;
         if (var4 >= 2560.0) {
            return 2;
         } else {
            return var4 >= 2000.0 ? 1 : 0;
         }
      }
   }

   private static int a(RiverEngine var0, ServerPlayer var1, int var2) {
      int var3 = var1.getBlockX();
      int var4 = var1.getBlockZ();
      if (!var0.isPlateReady(var0.plateAt(var3, var4)) || a(var0, var3, var4, 16)) {
         return 4;
      } else if (a(var0, var3, var4, var2 / 3)) {
         return 3;
      } else if (a(var0, var3, var4, var2 * 2 / 3)) {
         return 2;
      } else {
         return a(var0, var3, var4, var2 * 5 / 3) ? 1 : 0;
      }
   }

   private static boolean a(RiverEngine var0, int var1, int var2, int var3) {
      for (int var4 = 0; var4 < 16; var4++) {
         double var5 = var4 * (Math.PI / 8);
         int var7 = var1 + (int)Math.round(Math.cos(var5) * var3);
         int var8 = var2 + (int)Math.round(Math.sin(var5) * var3);
         if (!var0.isPlateReady(var0.plateAt(var7, var8))) {
            return true;
         }
      }

      return false;
   }

   private static void a(int var0) {
      int var1 = var0 >= 4 ? a : (var0 == 3 ? Math.max(1, a * 3 / 4) : (var0 == 2 ? Math.max(1, a / 3) : (var0 == 1 ? Math.max(1, a / 5) : 1)));
      if (i) {
         var1 = Math.max(var1, Math.min(3, f.get()));
      }

      synchronized (l) {
         if (b.getCorePoolSize() != var1 || b.getMaximumPoolSize() != var1) {
            if (var1 >= b.getMaximumPoolSize()) {
               b.setMaximumPoolSize(var1);
               b.setCorePoolSize(var1);
            } else {
               b.setCorePoolSize(var1);
               b.setMaximumPoolSize(var1);
            }
         }
      }

      if (Boolean.getBoolean("streamsreflowing.caldiag") && var0 != O) {
         Constants.LOG.info("[SR-POOL] frontier tier {} -> {} (prefetch lanes {})", new Object[]{O, var0, var1});
         O = var0;
      }

      LowResGrid.setBackgroundThrottle(var0);
   }

   public static void clearPending() {
      b.getQueue().clear();
      d.getQueue().clear();
      r.clear();
      s.clear();
      t.clear();
      u.clear();
      x.clear();
      w.clear();
      B.clear();
   }

   private static void a(ServerLevel var0) {
      boolean var1 = Services.PLATFORM.vanillaWaterFlow() || !Services.PLATFORM.streamWaterState(0).is(Blocks.WATER);
      if (Services.PLATFORM.flowVanillaRivers() && var1) {
         RiverFlowField.BiomeClass var2 = RiverFlowFields.classifierFor(var0);
         RiverFlowField.OrientationField var3 = RiverFlowFields.orientationFor(var0);
         int var4 = RiverFlowFields.regionSize();

         for (ServerPlayer var6 : var0.players()) {
            int var7 = var6.getBlockX();
            int var8 = var6.getBlockZ();

            for (int var9 = -1; var9 <= 1; var9++) {
               for (int var10 = -1; var10 <= 1; var10++) {
                  a(var2, var3, var7 + var10 * var4, var8 + var9 * var4, var10 == 0 && var9 == 0 ? 0 : 1);
               }
            }
         }
      }
   }

   private static void a(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3, int var4) {
      long var5 = RiverFlowFields.owningRegionKey(var2, var3);
      if (t.add(var5)) {
         b.execute(new StreamPrefetcher.PrioritizedTask(var4, e.getAndIncrement(), Long.MIN_VALUE, var5, () -> {
            try {
               RiverFlowFields.ensureRegion(var0, var1, var2, var3);
            } catch (Throwable var10) {
               Constants.LOG.warn("Prefetch of river-flow field near ({},{}) failed", new Object[]{var2, var3, var10});
            } finally {
               t.remove(var5);
            }
         }));
      } else {
         for (Runnable var8 : b.getQueue()) {
            if (var8 instanceof StreamPrefetcher.PrioritizedTask var9 && var9.e == var5 && var9.b > var4 && b.getQueue().remove(var9)) {
               b.execute(new StreamPrefetcher.PrioritizedTask(var4, e.getAndIncrement(), Long.MIN_VALUE, var5, var9.f));
               break;
            }
         }
      }
   }

   private static void a(ServerLevel var0, RiverEngine var1, MinecraftServer var2) {
      if (Services.PLATFORM.vanillaWaterFlow()) {
         RiverFlowField.BiomeClass var3 = RiverFlowFields.classifierFor(var0);
         RiverFlowField.OrientationField var4 = RiverFlowFields.orientationFor(var0);
         boolean var5 = Services.PLATFORM.flowVanillaRivers();
         int var6 = var2.getPlayerList().getViewDistance() + 2;
         int var7 = 128;

         for (ServerPlayer var9 : var0.players()) {
            int var10 = var9.getBlockX() >> 4;
            int var11 = var9.getBlockZ() >> 4;

            for (int var12 = -var6; var12 <= var6 && var7 > 0; var12++) {
               for (int var13 = -var6; var13 <= var6 && var7 > 0; var13++) {
                  int var14 = var10 + var13;
                  int var15 = var11 + var12;
                  if (StreamFlowGrid.peek(var14, var15) == null) {
                     submitGridBuild(var0, var1, var3, var4, var5, var14, var15, false, null);
                     var7--;
                  }
               }
            }
         }
      }
   }

   public static void flowTrace(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      Logger var5 = Constants.LOG;
      int var6 = var2 >> 4;
      int var7 = var4 >> 4;
      long var8 = (long)var6 << 32 ^ var7 & 4294967295L;
      boolean var10 = Services.PLATFORM.flowVanillaRivers();
      int var11 = var1.getHeight(Types.MOTION_BLOCKING, var2, var4) - 1;
      var5.info(
         "[SR-FLOWCELL] ==== current at ({}, {}, {}) chunk ({},{}) water top y={} | vanillaWaterFlow={} flowVanillaRivers={} ====",
         new Object[]{var2, var3, var4, var6, var7, var11, Services.PLATFORM.vanillaWaterFlow(), var10}
      );

      for (int var15 : new int[]{var3, var11}) {
         BlockPos var16 = new BlockPos(var2, var15, var4);
         FluidState var17 = var1.getFluidState(var16);
         boolean var18 = var17.hasProperty(FlowingFluid.FALLING) && (Boolean)var17.getValue(FlowingFluid.FALLING);
         var5.info(
            "[SR-FLOWCELL] block y={}: fluid={} source={} amount={} falling={} | getFlow={} | push angle={} speed={} streamCurrent={}",
            new Object[]{
               var15,
               var17.isEmpty() ? "none" : var17.getType().getClass().getSimpleName(),
               var17.isSource(),
               var17.getAmount(),
               var18,
               var17.isEmpty() ? "-" : String.valueOf(var17.getFlow(var1, var16)),
               a(StreamFlowGrid.angleForEntityPush(var1, var2, var15, var4)),
               String.format(Locale.ROOT, "%.2f", StreamFlowGrid.speedForPhysics(var1, var2, var15, var4)),
               StreamFlowGrid.streamCurrentAt(var1, var2, var15, var4)
            }
         );
      }

      a(var5, "server", StreamFlowGrid.peek(var6, var7), var2, var4, var3, var11);
      a(var5, "client", ClientFlowGrids.gridAt(var6, var7), var2, var4, var3, var11);
      Long var25 = w.get(var8);
      RiverFlowField.BiomeClass var26 = RiverFlowFields.classifierFor(var1);
      RiverFlowField.OrientationField var27 = RiverFlowFields.orientationFor(var1);
      var5.info(
         "[SR-FLOWCELL] grid state: provisional={} parked retry={} parked for {} | every dependency in memory={}",
         new Object[]{
            StreamFlowGrid.isIncomplete(var6, var7),
            x.containsKey(var8),
            var25 == null ? "-" : System.currentTimeMillis() - var25 + " ms",
            a(var0, var26, var27, var10, var6, var7, false)
         }
      );
      boolean var28 = RiverFlowFields.isRegionReady(var2, var4);
      double var29 = var28 ? RiverFlowFields.flowRadAt(var26, var27, var2, var4) : Double.NaN;
      StringBuilder var30 = new StringBuilder();

      for (int var19 = -2; var19 <= 2; var19++) {
         var30.append(var19 == -2 ? "" : "/");

         for (int var20 = -2; var20 <= 2; var20++) {
            int var21 = (var2 >> 2) + var20 << 2;
            int var22 = (var4 >> 2) + var19 << 2;
            int var23 = var26.classify(var21, var22);
            int var24 = var23 == 1 ? 82 : (var23 == 2 ? 79 : 76);
            var30.append((char)(RiverFlowFields.bridgedNear(var21, var22) ? Character.toLowerCase((char)var24) : var24));
         }
      }

      var5.info(
         "[SR-FLOWCELL] river field: region ready={} current={} | biome class, 5x5 quarts around (R river, O ocean, L land; lower-case = gap-bridged): {}",
         new Object[]{var28, Double.isNaN(var29) ? "none" : String.format(Locale.ROOT, "%.0f deg", Math.toDegrees(var29)), var30}
      );
      if (var0.platesReadyNear(var2 + 0.5, var4 + 0.5, 0.0)) {
         ColumnPlan var31 = var0.plan(var2, var4);
         if (var31 == ColumnPlan.NONE) {
            var5.info("[SR-FLOWCELL] stream plan: none (not one of our streams)");
         } else {
            var5.info(
               "[SR-FLOWCELL] stream plan: carriesFlow={} inChannel={} dir=({}, {}) speed={} surface={} bed={}",
               new Object[]{
                  StreamFlowGrid.carriesFlow(var31),
                  var31.inChannel(),
                  String.format(Locale.ROOT, "%.2f", var31.fluidFlowDirX()),
                  String.format(Locale.ROOT, "%.2f", var31.fluidFlowDirZ()),
                  String.format(Locale.ROOT, "%.2f", var31.speed()),
                  var31.waterSurfaceY(),
                  var31.bedY()
               }
            );
         }
      } else {
         var5.info("[SR-FLOWCELL] stream plan: plate not in memory (not built from here)");
      }

      RawFlowTiles.Tile var32 = RawFlowTiles.get(var6, var7);
      if (var32 == null) {
         var5.info("[SR-FLOWCELL] carve tile: none for this chunk");
      } else {
         int var33 = (var2 & 15) * 16 + (var4 & 15);
         var5.info(
            "[SR-FLOWCELL] carve tile: dir=({}, {}) surface={} bed={}",
            new Object[]{var32.dirX()[var33], var32.dirZ()[var33], var32.surfaceY()[var33], var32.bedY()[var33]}
         );
      }

      StringBuilder var34 = new StringBuilder();

      for (int var35 = -2; var35 <= 2; var35++) {
         var34.append(var35 == -2 ? "" : " / ");

         for (int var36 = -2; var36 <= 2; var36++) {
            var34.append(a(var6 + var36, var7 + var35));
         }
      }

      var5.info("[SR-FLOWCELL] chunks around, 5x5 server|client (- none, 0 built still, F flowing, p provisional, P parked): {}", var34);
   }

   private static String a(byte var0) {
      return var0 == -1 ? "NONE" : String.format(Locale.ROOT, "%.0f deg", (var0 & 0xFF) / 255.0 * 360.0);
   }

   private static String a(int var0, int var1) {
      long var2 = (long)var0 << 32 ^ var1 & 4294967295L;
      byte[] var4 = StreamFlowGrid.peek(var0, var1);
      byte[] var5 = ClientFlowGrids.gridAt(var0, var1);
      char var6 = x.containsKey(var2) ? 'P' : (StreamFlowGrid.isIncomplete(var0, var1) ? 'p' : a(var4));
      return "" + var6 + a(var5);
   }

   private static char a(byte[] var0) {
      if (var0 == null) {
         return '-';
      }

      for (int var1 = 0; var1 < Math.min(var0.length, 256); var1++) {
         if (var0[var1] != -1) {
            return 'F';
         }
      }

      for (int var3 = 1; var3 < StreamFlowGrid.layerCount(var0); var3++) {
         for (int var2 = 0; var2 < 256; var2++) {
            if (var0[var3 * 1536 + var2] != -1) {
               return 'F';
            }
         }
      }

      return '0';
   }

   private static void a(Logger var0, String var1, byte[] var2, int var3, int var4, int... var5) {
      if (var2 == null) {
         var0.info("[SR-FLOWCELL] {} grid: none for this chunk", var1);
      } else if (var2.length == 0) {
         var0.info("[SR-FLOWCELL] {} grid: built, no current anywhere in this chunk", var1);
      } else {
         int var6 = (var3 & 15) + (var4 & 15) * 16;
         int var7 = StreamFlowGrid.layerCount(var2);
         StringBuilder var8 = new StringBuilder();

         for (int var9 = 0; var9 < var7; var9++) {
            int var10 = var2.length < 1536 ? 0 : var9 * 1536;
            var8.append(" | layer ").append(var9).append(": angle=").append(a(var2[var10 + var6]));
            if (var2.length >= 1536) {
               var8.append(" speed=")
                  .append(String.format(Locale.ROOT, "%.2f", StreamFlowGrid.unpackSpeed(var2[var10 + 256 + var6])))
                  .append(" band=")
                  .append(StreamFlowGrid.bandLo(var2, var9, var6))
                  .append("..")
                  .append(StreamFlowGrid.bandHi(var2, var9, var6));
            }
         }

         for (int var12 : var5) {
            var8.append(" | y=")
               .append(var12)
               .append(" reads layer ")
               .append(var2.length < 1536 ? (var2[var6] == -1 ? -1 : 0) : StreamFlowGrid.layerAt(var2, var6, var12));
         }

         var0.info("[SR-FLOWCELL] {} grid: {} bytes, {} layer(s), {} column(s) of this chunk flowing{}", new Object[]{var1, var2.length, var7, b(var2), var8});
      }
   }

   private static int b(byte[] var0) {
      int var1 = 0;
      int var2 = StreamFlowGrid.layerCount(var0);

      for (int var3 = 0; var3 < 256; var3++) {
         for (int var4 = 0; var4 < var2; var4++) {
            int var5 = var0.length < 1536 ? 0 : var4 * 1536;
            if (var5 + var3 < var0.length && var0[var5 + var3] != -1) {
               var1++;
               break;
            }
         }
      }

      return var1;
   }

   public static void submitGridBuild(
      ServerLevel var0,
      RiverEngine var1,
      RiverFlowField.BiomeClass var2,
      RiverFlowField.OrientationField var3,
      boolean var4,
      int var5,
      int var6,
      boolean var7,
      Runnable var8
   ) {
      long var9 = (long)var5 << 32 ^ var6 & 4294967295L;
      if (var7 || u.add(var9)) {
         (var7 ? d : b).execute(new StreamPrefetcher.PrioritizedTask(var7 ? 0 : 2, e.getAndIncrement(), () -> {
            boolean var11 = true;

            try {
               try {
                  if (StreamFlowGrid.peek(var5, var6) == null || StreamFlowGrid.isIncomplete(var5, var6)) {
                     if (!a(var1, var2, var3, var4, var5, var6, var7)) {
                        if (var7) {
                           x.putIfAbsent(var9, () -> submitGridBuild(var0, var1, var2, var3, var4, var5, var6, true, var8));
                           boolean var18 = w.putIfAbsent(var9, System.currentTimeMillis()) == null;
                           if (var18) {
                              StreamFlowGrid.buildProvisional(var0, var1, var2, var3, var4, var5, var6);
                              if (var8 != null) {
                                 var8.run();
                              }
                           }

                           var11 = false;
                        }

                        return;
                     }

                     StreamFlowGrid.getOrBuild(var0, var1, var2, var3, var4, var5, var6);
                  }

                  Long var12 = w.remove(var9);
                  if (var12 != null && FlowTrace.ON) {
                     FlowTrace.parkedDelivery(System.currentTimeMillis() - var12);
                     return;
                  }
               } catch (Throwable var16) {
                  Constants.LOG.warn("Flow-grid build at chunk ({},{}) failed", new Object[]{var5, var6, var16});
               }
            } finally {
               if (!var7) {
                  u.remove(var9);
               }

               if (var11 && var8 != null) {
                  var8.run();
               }
            }
         }));
      }
   }

   private static boolean a(
      RiverEngine var0, RiverFlowField.BiomeClass var1, RiverFlowField.OrientationField var2, boolean var3, int var4, int var5, boolean var6
   ) {
      boolean var7 = true;
      int var8 = (var4 << 4) + 8;
      int var9 = (var5 << 4) + 8;

      for (PlateKey var13 : var0.platesNear(var8, var9, 25.0)) {
         if (!var0.isPlateCached(var13)) {
            if (!var6) {
               return false;
            }

            FlowTrace.plateRequests.incrementAndGet();
            RiverEngine.DEMAND_REASON.set("flow grid for chunk (" + var4 + "," + var5 + ")");
            RiverEngine.noteDemand(var13, var8, var9, 25.0);
            RiverEngine.DEMAND_REASON.remove();
            a(var0, var13, 0);
            var7 = false;
         }
      }

      if (var3) {
         for (byte var14 = -1; var14 <= 1; var14 += 2) {
            for (byte var15 = -1; var15 <= 1; var15 += 2) {
               int var16 = var8 + var15 * 17;
               int var17 = var9 + var14 * 17;
               if (!RiverFlowFields.isRegionReady(var16, var17)) {
                  if (!var6) {
                     return false;
                  }

                  FlowTrace.fieldRequests.incrementAndGet();
                  a(var1, var2, var16, var17, 0);
                  var7 = false;
               }
            }
         }
      }

      return var7;
   }

   public static String plateDiag(RiverEngine var0, PlateKey var1) {
      long var2 = (long)var1.cx() << 32 ^ var1.cz() & 4294967295L;
      String var4 = "none";

      for (Runnable var6 : b.getQueue()) {
         if (var6 instanceof StreamPrefetcher.PrioritizedTask var7 && var7.d == var2) {
            var4 = var7.b == -1 ? "URGENT" : (var7.b == 0 ? "ahead" : "passive");
            break;
         }
      }

      return "cached=" + var0.isPlateCached(var1) + " marker=" + r.contains(var2) + " queued=" + var4;
   }

   public static String workingThreadStacks() {
      StringBuilder var0 = new StringBuilder(4096);
      Thread var1 = Thread.currentThread();
      int var2 = 0;

      for (Entry var4 : Thread.getAllStackTraces().entrySet()) {
         Thread var5 = (Thread)var4.getKey();
         StackTraceElement[] var6 = (StackTraceElement[])var4.getValue();
         if (var5 != var1
            && var6.length != 0
            && !var5.getName().startsWith("streamsreflowing-stallwatch")
            && !var5.getName().startsWith("streamsreflowing-defer-watchdog")) {
            boolean var7 = false;

            for (StackTraceElement var11 : var6) {
               if (var11.getClassName().startsWith("dev.streamsreflowing.")) {
                  var7 = true;
                  break;
               }
            }

            if (var7) {
               if (++var2 > 12) {
                  var0.append("\n  ... and more");
                  break;
               }

               var0.append("\n  ").append(var5.getName()).append(" [").append(var5.getState()).append(']');
               int var13 = 0;

               for (StackTraceElement var12 : var6) {
                  var0.append("\n      at ").append(var12);
                  if (++var13 == 16) {
                     break;
                  }
               }
            }
         }
      }

      return var2 == 0 ? " none of this mod's threads was working" : var0.toString();
   }

   public static String laneDiag() {
      return "exec="
         + b.getPoolSize()
         + "/"
         + b.getCorePoolSize()
         + " queue="
         + b.getQueue().size()
         + " activeBuilds="
         + RiverEngine.activeBuildCount()
         + " urgentWaiters="
         + LowResGrid.urgentWaiterCount();
   }

   private static void a(RiverEngine var0, PlateKey var1, int var2) {
      if (!R || var2 == -1) {
         if (var0.isPlateCached(var1)) {
            var0.markPlateWanted(var1);
            if (!i || var2 != -1 || var0.isPlateReady(var1)) {
               return;
            }
         }

         long var3 = (long)var1.cx() << 32 ^ var1.cz() & 4294967295L;
         if (!r.add(var3)) {
            if (var2 == -1 && PlateOffload.reclaim(var0, var1)) {
               if (i) {
                  f.incrementAndGet();
               }

               b.execute(new StreamPrefetcher.PrioritizedTask(-1, e.getAndIncrement(), var3, () -> a(var0, var1, var3, -1)));
               if (i) {
                  a();
               }
            } else {
               if (i && var2 == -1) {
                  boolean var5 = false;

                  for (Runnable var7 : b.getQueue()) {
                     if (var7 instanceof StreamPrefetcher.PrioritizedTask var8 && var8.d == var3 && var8.b != -1 && b.getQueue().remove(var8)) {
                        f.incrementAndGet();
                        b.execute(new StreamPrefetcher.PrioritizedTask(-1, e.getAndIncrement(), var3, () -> a(var0, var1, var3, -1)));
                        a();
                        var5 = true;
                        break;
                     }
                  }

                  if (!var5) {
                     BuildGate.promote(var3);
                  }

                  if (!var5 && P.add(var3)) {
                     LowResGrid.noteDeferredDemand();
                     if (!r.contains(var3) && P.remove(var3)) {
                        LowResGrid.deferredDemandDone();
                     }
                  }
               }
            }
         } else if (var2 == -1
            || !PlateOffload.offer(
               var0, var1, () -> b.execute(new StreamPrefetcher.PrioritizedTask(var2, e.getAndIncrement(), var3, () -> a(var0, var1, var3, var2)))
            )) {
            if (i && var2 == -1) {
               f.incrementAndGet();
            }

            b.execute(new StreamPrefetcher.PrioritizedTask(var2, e.getAndIncrement(), var3, () -> a(var0, var1, var3, var2)));
            if (i && var2 == -1) {
               a();
            }
         }
      }
   }

   public static void submitChunkDemand(RiverEngine var0, PlateKey var1) {
      a(var0, var1, -1);
   }

   private static boolean c() {
      for (Runnable var1 : b.getQueue()) {
         if (var1 instanceof StreamPrefetcher.PrioritizedTask var2 && var2.b == -1) {
            return true;
         }
      }

      return false;
   }

   private static void a(RiverEngine var0, PlateKey var1, long var2, int var4) {
      if (R && var4 != -1) {
         r.remove(var2);
      } else {
         boolean var5 = false;

         try {
            if (var4 != -1) {
               long var6 = System.nanoTime() + 60000000000L;

               while (LowResGrid.urgentWaiterCount() > 0 && RiverEngine.activeBuildCount() >= 4 && System.nanoTime() < var6) {
                  if (R) {
                     return;
                  }

                  if (var0.isPlateCached(var1)) {
                     return;
                  }

                  if (c()) {
                     b.execute(new StreamPrefetcher.PrioritizedTask(var4, e.getAndIncrement(), var2, () -> a(var0, var1, var2, var4)));
                     var5 = true;
                     return;
                  }

                  Thread.sleep(250L);
               }
            }

            if (i && var4 == -1) {
               h.acquireUninterruptibly();
            }

            try {
               if (!i || var4 != -1 || !var0.isPlateReady(var1)) {
                  if (var4 == -1) {
                     if (P.add(var2)) {
                        LowResGrid.noteDeferredDemand();
                     }

                     LowResGrid.setUrgentBuild(true);
                     BuildGate.asDemanded(() -> var0.networkFor(var1));
                  } else {
                     var0.networkFor(var1);
                  }

                  return;
               }
            } finally {
               if (i && var4 == -1) {
                  h.release();
               }
            }
         } catch (InterruptedException var19) {
            Thread.currentThread().interrupt();
            return;
         } catch (Throwable var20) {
            Constants.LOG.warn("Prefetch of river region {} failed", var1, var20);
            return;
         } finally {
            LowResGrid.setUrgentBuild(false);
            if (!var5) {
               r.remove(var2);
               if (P.remove(var2)) {
                  LowResGrid.deferredDemandDone();
               }
            }

            if (i && var4 == -1) {
               f.decrementAndGet();
            }
         }
      }
   }

   private static void a(RiverEngine var0, ServerLevel var1, int var2) {
      HashSet var3 = new HashSet();

      for (ServerPlayer var5 : var1.players()) {
         BlockPos var6 = var5.blockPosition();
         PlateKey var7 = var0.plateAt(var6.getX(), var6.getZ());

         for (int var8 = -var2; var8 <= var2; var8++) {
            for (int var9 = -var2; var9 <= var2; var9++) {
               var3.add((long)(var7.cx() + var9) << 32 ^ var7.cz() + var8 & 4294967295L);
            }
         }
      }

      if (!var3.equals(Q)) {
         Q = var3;

         for (Runnable var11 : b.getQueue()) {
            if (var11 instanceof StreamPrefetcher.PrioritizedTask var12
               && var12.b == 2
               && var12.d != Long.MIN_VALUE
               && !var3.contains(var12.d)
               && b.getQueue().remove(var12)) {
               r.remove(var12.d);
            }
         }
      }
   }

   private static void d() {
      for (Runnable var1 : b.getQueue()) {
         if (var1 instanceof StreamPrefetcher.PrioritizedTask var2 && var2.b == 0 && var2.d != Long.MIN_VALUE && b.getQueue().remove(var2)) {
            r.remove(var2.d);
         }
      }
   }

   private static void a(RiverEngine var0, ServerLevel var1) {
      int var2 = Services.PLATFORM.prefetchRadius();

      for (ServerPlayer var4 : var1.players()) {
         BlockPos var5 = var4.blockPosition();
         PlateKey var6 = var0.plateAt(var5.getX(), var5.getZ());

         for (int var7 = -var2; var7 <= var2; var7++) {
            for (int var8 = -var2; var8 <= var2; var8++) {
               if (b.getQueue().size() >= 256) {
                  return;
               }

               a(var0, new PlateKey(var6.cx() + var8, var6.cz() + var7), 2);
            }
         }
      }
   }

   private static void a(RiverEngine var0, BlockPos var1, int var2) {
      a(var0, var0.plateAt(var1.getX(), var1.getZ()));
      a(var0, var0.plateAt(var1.getX() + var2, var1.getZ() + var2));
      a(var0, var0.plateAt(var1.getX() + var2, var1.getZ() - var2));
      a(var0, var0.plateAt(var1.getX() - var2, var1.getZ() + var2));
      a(var0, var0.plateAt(var1.getX() - var2, var1.getZ() - var2));
   }

   public static void submitUrgentAt(ServerLevel var0, int var1, int var2) {
      try {
         if (var0 == null || !Services.PLATFORM.generateRivers() || !var0.dimension().equals(Level.OVERWORLD)) {
            return;
         }

         RiverEngine var3 = StreamsWorldgen.engine(var0);
         int var4 = (int)Math.max(256.0, var3.config().plateScale());
         int var5 = Math.min(var4, Math.max(160, var0.getServer().getPlayerList().getViewDistance() * 16));
         a(var3, new BlockPos(var1, var0.getSeaLevel(), var2), var5);
      } catch (Throwable var6) {
         Constants.LOG.warn("Urgent prefetch for teleport destination ({},{}) failed (ignored)", new Object[]{var1, var2, var6});
      }
   }

   public static boolean isServerStopping() {
      return R;
   }

   public static void onServerStopping() {
      R = true;
      PlateOffload.clear();
      ChunkStallWatch.stop();
      PlateRiverCache.releaseAllWaiters();

      for (Runnable var1 : b.getQueue()) {
         if (var1 instanceof StreamPrefetcher.PrioritizedTask var2 && var2.b != -1 && b.getQueue().remove(var2) && var2.d != Long.MIN_VALUE) {
            r.remove(var2.d);
         }
      }

      for (Runnable var4 : d.getQueue()) {
         if (var4 instanceof StreamPrefetcher.PrioritizedTask var5 && d.getQueue().remove(var5) && var5.d != Long.MIN_VALUE) {
            s.remove(var5.d);
         }
      }

      clearPending();
      LowResGrid.setBackgroundThrottle(4);
      if (PlateRiverCache.foreignHeld() > 0) {
         Constants.LOG
            .info(
               "Streams Reflowing: the server is stopping while chunk generation is still waiting on stream planning; the world is saved as soon as that planning finishes."
            );
      }
   }

   public static void warmWorldOrigin(RiverEngine var0) {
      R = false;
      PlateRiverCache.reopen();
      LowResGrid.setFocusPoints(new int[]{0, 0});
      PlateKey var1 = var0.plateAt(0, 0);
      a(var0, var1);
       HashSet<PlateKey> var2 = new HashSet<>();

      for (short var3 = -480; var3 <= 480; var3 += 160) {
         for (short var4 = -480; var4 <= 480; var4 += 160) {
            PlateKey var5 = var0.plateAt(var4, var3);
            if (!var5.equals(var1)) {
               var2.add(var5);
            }
         }
      }

      for (PlateKey var7 : var2) {
         a(var0, var7, 0);
      }
   }

   private static void a(RiverEngine var0, PlateKey var1) {
      if (!var0.isPlateCached(var1)) {
         long var2 = (long)var1.cx() << 32 ^ var1.cz() & 4294967295L;
         if (s.add(var2)) {
            d.execute(new StreamPrefetcher.PrioritizedTask(-1, e.getAndIncrement(), () -> {
               try {
                  var0.networkFor(var1);
               } catch (Throwable var8) {
                  Constants.LOG.warn("Urgent prefetch of river region {} failed", var1, var8);
               } finally {
                  s.remove(var2);
               }
            }));
         }
      }
   }

   private static void a(RiverEngine var0, BlockPos var1, double[] var2, int var3, Map<PlateKey, Double> var4, Map<PlateKey, Double> var5) {
      int var6 = Math.max(256, var3 / 2);
      int var7 = var3;

      for (int var8 = -var7; var8 <= var7; var8 += var6) {
         for (int var9 = -var7; var9 <= var7; var9 += var6) {
            double var10 = Math.hypot(var9, var8);
            double var12 = var10;
            if (var10 > 1.0E-6 && (var2[0] != 0.0 || var2[1] != 0.0)) {
               double var14 = (var9 * var2[0] + var8 * var2[1]) / var10;
               var12 = var10 * (1.25 - 0.75 * var14);
            }

            PlateKey var16 = var0.plateAt(var1.getX() + var9, var1.getZ() + var8);
            var4.merge(var16, var12, Math::min);
            var5.merge(var16, var10, Math::min);
         }
      }
   }

   private static double[] a(ServerPlayer var0) {
      ArrayDeque var1 = B.computeIfAbsent(var0.getUUID(), var0x -> new ArrayDeque<>());
      var1.addLast(new double[]{J, var0.getX(), var0.getZ()});

      while (!var1.isEmpty() && J - ((double[])var1.peekFirst())[0] > 400.0) {
         var1.removeFirst();
      }

      double[] var2 = (double[])var1.peekFirst();
      if (var2 != null && !(J - var2[0] < 160.0)) {
         double var3 = var0.getX() - var2[1];
         double var5 = var0.getZ() - var2[2];
         double var7 = Math.hypot(var3, var5);
         return var7 >= 32.0 ? new double[]{var3 / var7, var5 / var7} : new double[]{0.0, 0.0};
      } else {
         return new double[]{0.0, 0.0};
      }
   }

   public static void biomeBench(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      int var4 = (int)Math.round(var0.config().plateScale());
      int var5 = 4 * var4;
      byte var6 = 8;
      RiverFlowField.BiomeClass var7 = RiverFlowFields.classifierFor(var1);
      Thread var8 = new Thread(
         () -> {
            int var6x = var5 / 2;
            long var7x = 0L;
            int var9 = var3 - var6x;

            while (var9 < var3 - var6x + var6 * 48) {
               for (int var10 = var2 - var6x; var10 < var2 - var6x + var6 * 48; var10 += var6) {
                  var7x += var7.classify(var10, var9);
               }

               var9 += var6;
            }

            long var19 = System.nanoTime();
            long var11 = 0L;
            long var13 = 0L;

            for (int var15 = var3 - var6x; var15 <= var3 + var6x; var15 += var6) {
               for (int var16 = var2 - var6x; var16 <= var2 + var6x; var16 += var6) {
                  if (var7.classify(var16, var15) == 2) {
                     var13++;
                  }

                  var11++;
               }
            }

            double var20 = (System.nanoTime() - var19) / 1000000.0;
            double var17 = var20 * 1000.0 / Math.max(1L, var11);
            Constants.LOG
               .info(
                  "[SR-BIOMEBENCH] 4x plateScale tile {}x{} (plateScale={}), spacing {}: {} getNoiseBiome samples in {} ms = {} us/call, {}% ocean [warm {}]",
                  new Object[]{
                     var5,
                     var5,
                     var4,
                     var6,
                     var11,
                     String.format("%.0f", var20),
                     String.format("%.3f", var17),
                     String.format("%.1f", 100.0 * var13 / Math.max(1L, var11)),
                     var7x & 1L
                  }
               );
         },
         "streamsreflowing-biomebench"
      );
      var8.setDaemon(true);
      var8.start();
   }

   public static void diagnoseColumn(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      a(var0, var1, var2, var3);
      a(var0, var1, new ChunkPos(new BlockPos(var2, 0, var3)));
   }

   public static void autoTrace(MinecraftServer var0, int var1, int var2) {
      ServerLevel var3 = var0.overworld();
      int var4 = var1 >> 4;
      int var5 = var2 >> 4;
      Constants.LOG
         .info(
            "[SR-AUTOTRACE] target=({},{}) chunk=({},{}) -- force-generating a 5x5 chunk block (the precise plate region from the diagnostics); nothing else is loaded",
            new Object[]{var1, var2, var4, var5}
         );
      long var6 = FormSampler.keptIr.sum();
      long var8 = FormSampler.handedBack.sum();
      long var10 = System.nanoTime();
      int var12 = 0;

      for (int var13 = -2; var13 <= 2; var13++) {
         for (int var14 = -2; var14 <= 2; var14++) {
            var3.getChunk(var4 + var13, var5 + var14);
            var12++;
         }
      }

      long var32 = (System.nanoTime() - var10) / 1000000L;
      long var15 = FormSampler.keptIr.sum() - var6;
      long var17 = FormSampler.handedBack.sum() - var8;
      Constants.LOG.info("[SR-AUTOTRACE] generated {} chunks in {} ms; dumping placed-column diagnostics", var12, var32);
      Constants.LOG
         .info(
            "[SR-AUTOTRACE] height queries through the walk during that build: {} served, {} handed to the host, {} total{}",
            new Object[]{
               var15,
               var17,
               var15 + var17,
               var15 + var17 == 0L
                  ? " (zero -- either the walk is off, or this build asks it for nothing)"
                  : String.format(Locale.ROOT, "; %.1f us each if they owned the whole %d ms", var32 * 1000.0 / (var15 + var17), var32)
            }
         );
      ReadLocality.dump("this build");
      SampleClock.dump();
      ChunkHeightCache.report("this build");
      RiverEngine var19 = StreamsWorldgen.engine(var3);

      for (byte var20 = -24; var20 <= 24; var20 += 4) {
         diagnoseColumn(var19, var3, var1, var2 + var20);
      }

      traceColumn(var19, var3, var1, var2);
      superTrace(var19, var3, var1, var2);
      spoutScan(var19, var3, var1, var2, 768);
      laneScan(var19, var3, var1, var2, Y);
      String var33 = System.getProperty("streamsreflowing.sweep");
      if (var33 != null && !var33.isBlank()) {
         for (String var24 : var33.split(";")) {
            String[] var25 = var24.split(",");

            try {
               int var26 = Integer.parseInt(var25[0].trim());
               int var27 = Integer.parseInt(var25[1].trim());

               for (int var28 = -1; var28 <= 1; var28++) {
                  for (int var29 = -1; var29 <= 1; var29++) {
                     var3.getChunk((var26 >> 4) + var28, (var27 >> 4) + var29);
                  }
               }

               diagnoseColumn(var19, var3, var26, var27);
               traceColumn(var19, var3, var26, var27);
               superTrace(var19, var3, var26, var27);
               spoutScan(var19, var3, var26, var27, 768);
               laneScan(var19, var3, var26, var27, Y);
            } catch (RuntimeException var31) {
               Constants.LOG.warn("[SR-AUTOTRACE] bad sweep target '{}'", var24);
            }
         }
      }

      String var34 = System.getProperty("streamsreflowing.monoaudit");
      if (var34 != null && !var34.isBlank()) {
         String[] var35 = var34.split(",");

         try {
            int var37 = Integer.parseInt(var35[0].trim());
            int var38 = Integer.parseInt(var35[1].trim());
            int var39 = var35.length >= 3 ? Integer.parseInt(var35[2].trim()) : 96;
            monoAudit(var19, var3, var37, var38, var39);
         } catch (RuntimeException var30) {
            Constants.LOG.warn("[SR-MONOAUDIT] bad streamsreflowing.monoaudit '{}'", var34, var30);
         }
      }

      String var36 = System.getProperty("streamsreflowing.probefile");
      if (var36 != null && !var36.isBlank()) {
         a(var19, var3, var3.getChunkSource().getGenerator(), var3.getChunkSource().randomState(), var36);
      }

      Constants.LOG.info("[SR-AUTOTRACE] diagnostics complete; halting server");
      var0.halt(false);
   }

   private static void a(RiverEngine var0, ServerLevel var1, ChunkGenerator var2, RandomState var3, String var4) {
      List var5;
      try {
         var5 = Files.readAllLines(Path.of(var4));
      } catch (IOException var24) {
         Constants.LOG.warn("[SR-PROBE] cannot read {}", var4, var24);
         return;
      }

      int var6 = 0;
      int var7 = 0;
      Iterator var8 = var5.iterator();

      while (true) {
         int var12;
         int var13;
         while (true) {
            if (!var8.hasNext()) {
               Constants.LOG.info("[SR-PROBE] {} column(s): {} stand in water, {} are dry", new Object[]{var6 + var7, var6, var7});
               return;
            }

            String var9 = (String)var8.next();
            String var10 = var9.trim();
            if (!var10.isEmpty() && !var10.startsWith("#")) {
               String[] var11 = var10.split(",");

               try {
                  var12 = (int)Math.floor(Double.parseDouble(var11[0].trim()));
                  var13 = (int)Math.floor(Double.parseDouble(var11[1].trim()));
                  break;
               } catch (RuntimeException var25) {
                  Constants.LOG.warn("[SR-PROBE] bad line '{}'", var10);
               }
            }
         }

         int var14 = var2.getBaseHeight(var12, var13, Types.OCEAN_FLOOR_WG, var1, var3) - 1;
         int var15 = var2.getBaseHeight(var12, var13, Types.WORLD_SURFACE_WG, var1, var3) - 1;
         boolean var16 = var15 > var14;
         double var17 = Double.NaN;

         for (byte var19 = 4; var19 <= 32 && Double.isNaN(var17); var19 += 4) {
            for (int var20 = -var19; var20 <= var19 && Double.isNaN(var17); var20 += 4) {
               for (int var21 = -var19; var21 <= var19; var21 += 4) {
                  if (Math.max(Math.abs(var20), Math.abs(var21)) == var19) {
                     int var22 = var2.getBaseHeight(var12 + var20, var13 + var21, Types.OCEAN_FLOOR_WG, var1, var3) - 1;
                     int var23 = var2.getBaseHeight(var12 + var20, var13 + var21, Types.WORLD_SURFACE_WG, var1, var3) - 1;
                     if (var23 > var22) {
                        var17 = Math.hypot(var20, var21);
                        break;
                     }
                  }
               }
            }
         }

         if (var16) {
            var6++;
         } else {
            var7++;
         }

         Lake var26 = var0.lakeAt(var12, var13);
         ColumnPlan var27 = var0.plan(var12, var13);
         Constants.LOG
            .info(
               "[SR-PROBE] ({},{}) ground={} surface={} -> {} | nearest water {} | lake={} | this world's channel here: inCorridor={} surface={}",
               new Object[]{
                  var12,
                  var13,
                  var14,
                  var15,
                  var16 ? "WATER" : "dry",
                  Double.isNaN(var17) ? ">32" : String.format(Locale.ROOT, "%.0f", var17),
                  var26 == null ? "none" : "level " + var26.level(),
                  var27.inCorridor(),
                  var27.inCorridor() ? String.valueOf(var27.waterSurfaceY()) : "-"
               }
            );
      }
   }

   public static void monoAudit(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      ChunkGenerator var5 = var1.getChunkSource().getGenerator();
      RandomState var6 = var1.getChunkSource().randomState();
      int var7 = var4 * 2 + 1;
      int[] var8 = new int[var7 * var7];
      double[] var9 = new double[var7 * var7];
      double[] var10 = new double[var7 * var7];
      int[] var11 = new int[var7 * var7];
      Arrays.fill(var8, Integer.MIN_VALUE);
      Arrays.fill(var11, Integer.MIN_VALUE);
      PlateKey var12 = var0.plateAt(var2, var3);
      byte var13 = 32;

      for (int var14 = -var4; var14 <= var4; var14++) {
         for (int var15 = -var4; var15 <= var4; var15++) {
            int var16 = var2 + var15;
            int var17 = var3 + var14;
            if (var0.plateAt(var16, var17).equals(var12)
               && var0.plateAt(var16 - var13, var17).equals(var12)
               && var0.plateAt(var16 + var13, var17).equals(var12)
               && var0.plateAt(var16, var17 - var13).equals(var12)
               && var0.plateAt(var16, var17 + var13).equals(var12)) {
               int var18 = (var14 + var4) * var7 + var15 + var4;
               ColumnPlan var19 = var0.plan(var16, var17);
               int var20 = var5.getBaseHeight(var16, var17, Types.OCEAN_FLOOR_WG, var1, var6) - 1;
               int var21 = var20;
               int var22 = Integer.MIN_VALUE;
               if (var19.inCorridor()) {
                  var22 = var19.waterSurfaceY();
                  if (var19.distToOutlet() <= 24.0) {
                     int var23 = RiverCarverFeature.nearbyLakeLevel(var0, var16, var17, var1.getMinBuildHeight(), var1.getMaxBuildHeight());
                     if (var23 != Integer.MIN_VALUE) {
                        var22 = Math.max(var22, Math.min(var23, var19.waterSurfaceY() + 4));
                     }
                  }

                  int var50 = var19.waterSurfaceY() + (int)Math.round(var19.bankBlend() * Math.max(0, var20 - var19.waterSurfaceY()));
                  var21 = Math.min(var20, var50);
                  if (var21 < var20 && var0.lakeAt(var16, var17) == null && var19.distToOutlet() > 24.0) {
                     int var24 = RiverCarverFeature.adjacentCoveringLakeLevel(var0, var16, var17);
                     if (var24 != Integer.MIN_VALUE && var20 > var24 && var21 <= var24) {
                        var21 = Math.min(var20, var24 + 1);
                     }
                  }

                  double var53 = Math.hypot(var19.flowDirX(), var19.flowDirZ());
                  if (var53 > 1.0E-6) {
                     var9[var18] = var19.flowDirX() / var53;
                     var10[var18] = var19.flowDirZ() / var53;
                  }
               }

               int var51 = var0.lakeSurfaceAt(var16, var17);
               if (var51 != Integer.MIN_VALUE && var51 > var22) {
                  var22 = var51;
                  var9[var18] = 0.0;
                  var10[var18] = 0.0;
               }

               var8[var18] = var22;
               var11[var18] = var21;
            }
         }
      }

      int var39 = 0;
      int[] var40 = new int[33];
      StringBuilder var41 = new StringBuilder();
      int var42 = 0;

      for (int var43 = -var4; var43 <= var4; var43++) {
         for (int var45 = -var4; var45 <= var4; var45++) {
            int var47 = (var43 + var4) * var7 + var45 + var4;
            int var48 = var8[var47];
            if (var48 != Integer.MIN_VALUE) {
               int var49 = 0;
               int var52 = 0;
               int var54 = 0;
               int var25 = 0;

               for (int var26 = 0; var26 < 8; var26++) {
                  int var27 = W[var26];
                  int var28 = X[var26];
                  double var29 = Math.sqrt(var27 * var27 + var28 * var28);
                  if (!(var9[var47] * var27 / var29 + var10[var47] * var28 / var29 >= 0.5)) {
                     int var31 = Integer.MIN_VALUE;

                     for (int var32 = 1; var32 <= 10; var32++) {
                        int var33 = var45 + var27 * var32;
                        int var34 = var43 + var28 * var32;
                        if (var33 < -var4 || var33 > var4 || var34 < -var4 || var34 > var4) {
                           break;
                        }

                        int var35 = (var34 + var4) * var7 + var33 + var4;
                        int var36 = var8[var35];
                        int var37 = var11[var35];
                        if (var37 == Integer.MIN_VALUE) {
                           break;
                        }

                        if (var36 != Integer.MIN_VALUE && var32 >= 2 && var48 - var36 >= 3 && var31 < var48) {
                           int var38 = var48 - var36;
                           if (var38 > var49) {
                              var49 = var38;
                              var52 = var36;
                              var54 = var2 + var33;
                              var25 = var3 + var34;
                           }
                           break;
                        }

                        var31 = Math.max(var31, Math.max(var37, var36));
                        if (var31 >= var48) {
                           break;
                        }
                     }
                  }
               }

               if (var49 > 0) {
                  int var55 = (int)Math.ceil(Math.hypot(var54 - (var2 + var45), var25 - (var3 + var43)));
                  if (!a(var0, var2 + var45, var3 + var43, var52 + 1, var55 + 4)) {
                     var39++;
                     var40[Math.min(32, var49)]++;
                     if (var42 < 20 || var49 >= 12 && var42 < 90) {
                        var41.append(
                           String.format(
                              Locale.ROOT,
                              "%n  water %d at (%d,%d) would pour onto water %d at (%d,%d) drop=%d",
                              var48,
                              var2 + var45,
                              var3 + var43,
                              var52,
                              var54,
                              var25,
                              var49
                           )
                        );
                        var42++;
                     }
                  }
               }
            }
         }
      }

      StringBuilder var44 = new StringBuilder();

      for (int var46 = 3; var46 < var40.length; var46++) {
         if (var40[var46] > 0) {
            var44.append(String.format("%n   drop %2d : %d columns", var46, var40[var46]));
         }
      }

      Constants.LOG
         .info(
            "[SR-MONOAUDIT] {}x{} around ({},{}): {} uphill-water columns (drop >= {} onto nearby lower water, no containing ground line between, cascades exempt).{}{}{}",
            new Object[]{var7, var7, var2, var3, var39, 3, var44, var39 > 0 ? "\n worst offenders:" : " CLEAN.", var41}
         );
   }

   public static void segmentMap(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      StringBuilder var5 = new StringBuilder(32768);
      var5.append(String.format(Locale.ROOT, "%n=== STREAM SEGMENT MAP @ (%d,%d) radius %d, step 2 ===%n", var2, var3, var4));
      var5.append("  digit = channel water surface (last digit) | # = bank/berm corridor | L = lake | . = untouched | @ = here")
         .append(String.format(Locale.ROOT, "%n"));
       HashMap<Integer, Integer> var6 = new HashMap<>();

      record _Row(double d, String text) {
      }

      for (int var7 = var3 - var4; var7 <= var3 + var4; var7 += 2) {
         var5.append(String.format(Locale.ROOT, "  z%6d  ", var7));

         for (int var8 = var2 - var4; var8 <= var2 + var4; var8 += 2) {
            char var9;
            if (Math.abs(var8 - var2) <= 1 && Math.abs(var7 - var3) <= 1) {
               var9 = '@';
            } else {
               Lake var10 = var0.lakeAt(var8, var7);
               ColumnPlan var11 = var0.plan(var8, var7);
               if (var10 != null) {
                  var9 = 'L';
               } else if (var11 != null && var11 != ColumnPlan.NONE && var11.inChannel()) {
                  int var12 = var11.waterSurfaceY();
                  var6.merge(var12, 1, Integer::sum);
                  var9 = (char)(48 + Math.floorMod(var12, 10));
               } else if (var11 != null && var11 != ColumnPlan.NONE && var11.inCorridor()) {
                  var9 = '#';
               } else {
                  var9 = '.';
               }
            }

            var5.append(var9);
         }

         var5.append(String.format(Locale.ROOT, "%n"));
      }

      var5.append(String.format(Locale.ROOT, "  x from %d (left) to %d (right)%n", var2 - var4, var2 + var4));
       ArrayList<Integer> var22 = new ArrayList<>(var6.keySet());
      Collections.sort(var22);
      var5.append("  channel water levels on the map: ");

      for (int var25 : var22) {
         var5.append(var25).append('(').append(var6.get(var25)).append("col) ");
      }

      var5.append(String.format(Locale.ROOT, "%n%n  segments within %d blocks (nearest first):%n", var4));
        ArrayList<_Row> var24 = new ArrayList<>();

      for (PlateKey var31 : var0.platesNear(var2, var3, var4)) {
         RiverNetwork var13 = var0.networkFor(var31);
         List var14 = var13.segments();
         int[] var15 = NetworkTracer.chainIds(var14);

         for (int var16 = 0; var16 < var14.size(); var16++) {
            RiverSegment var17 = (RiverSegment)var14.get(var16);
            double var18 = var17.clampParam(var2 + 0.5, var3 + 0.5);
            double var20 = var17.distanceAt(var2 + 0.5, var3 + 0.5, var18);
            if (var20 <= var4) {
               var24.add(
               new _Row(
                     var20,
                     String.format(
                        Locale.ROOT,
                        "  d%5.1f | %s chain#%d seg %d | (%.0f,%.0f) y%.1f w%.1f -> (%.0f,%.0f) y%.1f w%.1f",
                        var20,
                        var31,
                        var15[var16],
                        var16,
                        var17.x1(),
                        var17.z1(),
                        var17.y1(),
                        var17.w1(),
                        var17.x2(),
                        var17.z2(),
                        var17.y2(),
                        var17.w2()
                     )
                  )
               );
            }
         }
      }

      var24.sort(Comparator.comparingDouble(_Row::d));
      int var27 = Math.min(var24.size(), 80);

      for (int var29 = 0; var29 < var27; var29++) {
         var5.append(((_Row)var24.get(var29)).text()).append(String.format(Locale.ROOT, "%n"));
      }

      if (var24.size() > var27) {
         var5.append("  (+").append(var24.size() - var27).append(" more)").append(String.format(Locale.ROOT, "%n"));
      }

      Constants.LOG.info(var5.toString());
   }

   public static void superTrace(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      ChunkGenerator var4 = var1.getChunkSource().getGenerator();
      RandomState var5 = var1.getChunkSource().randomState();
      RiverNetwork var6 = var0.networkAt(var2, var3);
      RiverSample var7 = var6.sample(var2 + 0.5, var3 + 0.5, 64.0);
      if (var7 != null && var7.segId() >= 0) {
          List<RiverSegment> var8 = var6.segments();
         int[] var9 = NetworkTracer.chainIds(var8);
         int var10 = var9[var7.segId()];
          HashMap<Long, List<Integer>> var11 = new HashMap<>();
          HashMap<Long, Integer> var12 = new HashMap<>();
          HashMap<Long, Integer> var13 = new HashMap<>();

         for (int var14 = 0; var14 < var8.size(); var14++) {
            RiverSegment var15 = (RiverSegment)var8.get(var14);
            var11.computeIfAbsent(a(var15.x2(), var15.z2()), var0x -> new ArrayList<>()).add(var14);
            var13.put(a(var15.x1(), var15.z1()), var14);
            if (var9[var14] == var10) {
               var12.put(a(var15.x1(), var15.z1()), var14);
            }
         }

         int var47 = -1;

          for (Entry<Long, Integer> var16 : var12.entrySet()) {
             List<Integer> var17 = var11.get(var16.getKey());
            boolean var18 = false;
            if (var17 != null) {
               for (int var20 : var17) {
                  if (var9[var20] == var10) {
                     var18 = true;
                     break;
                  }
               }
            }

            if (!var18) {
               var47 = (Integer)var16.getValue();
               break;
            }
         }

         if (var47 < 0) {
            var47 = var7.segId();
         }

         StringBuilder var49 = new StringBuilder();
         StringBuilder var50 = new StringBuilder();
         var49.append(String.format(Locale.ROOT, "%n=== STREAM SUPER TRACE @ (%d,%d): chain #%d, plate %s ===%n", var2, var3, var10, var6.key()));
         var49.append("  cols: pos | preGround | rawGrade | surface(plan) | placedWater | placedWall | lake | halfW | flags\n");
         int var51 = var47;
         int var52 = 0;
         double var53 = 0.0;
         int var21 = Integer.MIN_VALUE;
         double var22 = Double.NaN;
         int var24 = 0;
         int var25 = var10;
         RiverSegment var26 = null;
          HashSet<Integer> var27 = new HashSet<>();

         for (int var28 = 0; var28 < 100000 && var51 >= 0 && var27.add(var51); var28++) {
            RiverSegment var29 = (RiverSegment)var8.get(var51);
            var53 += Math.hypot(var29.x2() - var29.x1(), var29.z2() - var29.z1());
             List<Integer> var30 = var11.get(a(var29.x1(), var29.z1()));
            if (var30 != null) {
               for (int var32 : var30) {
                  if (var9[var32] != var9[var51]) {
                     RiverSegment var33 = (RiverSegment)var8.get(var32);
                     int var34 = a(var0, var29.y1(), var29.w1());
                     int var35 = a(var0, var33.y2(), var33.w2());
                     int var36 = a(var0, var29.y2(), var29.w2());
                     StringBuilder var37 = new StringBuilder();
                     int var38 = var32;
                     double var39 = 0.0;

                     for (int var41 = 0; var41 < 400 && var39 < 16.0; var41++) {
                        RiverSegment var42 = (RiverSegment)var8.get(var38);
                        var39 += Math.hypot(var42.x2() - var42.x1(), var42.z2() - var42.z1());
                        if (var41 % 8 == 0) {
                           var37.insert(0, a(var0, var42.y1(), var42.w1()) + " > ");
                        }

                         List<Integer> var43 = var11.get(a(var42.x1(), var42.z1()));
                        int var44 = -1;
                        if (var43 != null) {
                           for (int var46 : var43) {
                              if (var9[var46] == var9[var32]) {
                                 var44 = var46;
                                 break;
                              }
                           }
                        }

                        if (var44 < 0) {
                           break;
                        }

                        var38 = var44;
                     }

                     var50.append(
                        String.format(
                           Locale.ROOT,
                           "%n  JUNCTION at (%d,%d): inflow chain #%d [%s%d] (grade %.2f w %.1f) joins chain #%d at %d (grade %.2f w %.1f) -> downstream %d%s",
                           (int)var29.x1(),
                           (int)var29.z1(),
                           var9[var32],
                           var37,
                           var35,
                           var33.y2(),
                           var33.w2(),
                           var9[var51],
                           var34,
                           var29.y1(),
                           var29.w1(),
                           var36,
                           var36 > Math.min(var35, var34) ? "  << RISE (downstream above an inflow!)" : ""
                        )
                     );
                  }
               }
            }

            if (var53 >= var52 * 4.0) {
               int var57 = (int)Math.floor(var29.x1());
               int var60 = (int)Math.floor(var29.z1());
               int var62 = var4.getBaseHeight(var57, var60, Types.OCEAN_FLOOR_WG, var1, var5) - 1;
               ColumnPlan var64 = var0.plan(var57, var60);
               int var66 = var64.inCorridor() ? var64.waterSurfaceY() : a(var0, var29.y1(), var29.w1());
               String var67 = "-";
               String var68 = "-";
               if (var1.hasChunk(var57 >> 4, var60 >> 4)) {
                  int var69 = var1.getHeight(Types.OCEAN_FLOOR, var57, var60) - 1;
                  var68 = String.valueOf(var69);
                  int var71 = -9999;

                  for (int var40 = Math.max(var69, var66) + 4; var40 >= var66 - 12; var40--) {
                     if (!var1.getBlockState(new BlockPos(var57, var40, var60)).getFluidState().isEmpty()) {
                        var71 = var40;
                        break;
                     }
                  }

                  var67 = var71 == -9999 ? "-" : String.valueOf(var71);
               }

               Lake var70 = var0.lakeAt(var57, var60);
               int var72 = RiverCarverFeature.nearbyLakeLevel(var0, var57, var60, var1.getMinBuildHeight(), var1.getMaxBuildHeight());
               boolean var73 = !Double.isNaN(var22) && var29.y1() > var22 + 0.01;
               boolean var74 = var21 != Integer.MIN_VALUE && var66 > var21;
               if (var73 || var74) {
                  var24++;
               }

               var49.append(
                  String.format(
                     Locale.ROOT,
                     "  (%6d,%6d) | g %3d | raw %7.2f | surf %3d | water %4s | wall %4s | %s%s | w %4.1f |%s%s%s%n",
                     var57,
                     var60,
                     var62,
                     var29.y1(),
                     var66,
                     var67,
                     var68,
                     var70 != null ? "L" + var70.level() : "  - ",
                     var70 == null && var72 != Integer.MIN_VALUE ? "~L" + var72 : "",
                     var29.w1(),
                     var64.terminal() ? " TERM" : "",
                     var64.source() ? " SRC" : "",
                     !var73 && !var74 ? "" : "  << RISE " + (var73 ? "raw " + var22 + "->" + var29.y1() : "") + (var74 ? "surf " + var21 + "->" + var66 : "")
                  )
               );
               var21 = var66;
               var22 = var29.y1();
               var52++;
            }

            var26 = var29;
            Integer var58 = (Integer)var13.get(a(var29.x2(), var29.z2()));
            if (var58 != null && var58 != var51 && var9[var58] != var25) {
               var49.append(String.format(Locale.ROOT, "  -- continues as chain #%d --%n", var9[var58]));
               var25 = var9[var58];
            }

            var51 = var58 != null && var58 != var51 ? var58 : -1;
         }

         if (var26 != null) {
            int var54 = (int)Math.floor(var26.x2());
            int var55 = (int)Math.floor(var26.z2());
            Lake var56 = var0.lakeAt(var54, var55);
            int var59 = a(var0, var26.y2(), var26.w2());
            String var61;
            if (var56 != null) {
               var61 = "ends IN lake (level "
                  + var56.level()
                  + "): our surface "
                  + var59
                  + (var59 >= var56.level() ? " -> FLUSH/above (ok)" : " -> BELOW LAKE by " + (var56.level() - var59) + " << PROBLEM");
            } else if (var26.y2() <= var0.config().seaLevel()) {
               var61 = "ends at the SEA (grade " + String.format(Locale.ROOT, "%.1f", var26.y2()) + ")";
            } else {
               int var63 = var4.getBaseHeight(var54, var55, Types.OCEAN_FLOOR_WG, var1, var5) - 1;
               int var65 = var4.getBaseHeight(var54, var55, Types.WORLD_SURFACE_WG, var1, var5) - 1;
               var61 = var65 > var63
                  ? "ends IN water at "
                     + var65
                     + " over floor "
                     + var63
                     + " (grade "
                     + String.format(Locale.ROOT, "%.1f", var26.y2())
                     + " -- a river or other body above sea level)"
                  : "DRY end at grade " + String.format(Locale.ROOT, "%.1f", var26.y2()) + " over ground " + var63 + " -- ends on land, no outlet";
            }

            var49.append(String.format(Locale.ROOT, "  END at (%d,%d): %s%n", var54, var55, var61));
         }

         var49.append(
            String.format(
               Locale.ROOT,
               "  walked ~%.0f blocks to the terminus; %d downstream RISE row(s)%s; junctions:%s%n",
               var53,
               var24,
               var24 > 0 ? " << NOT MONOTONE" : " (monotone)",
               var50.length() == 0 ? " none" : var50.toString()
            )
         );
         var49.append("  (per-pass modifier history: rebuild fresh with -Dstreamsreflowing.segdump=x,z,r -- a store-loaded network no longer carries it)");
         Constants.LOG.info(var49.toString());
      } else {
         Constants.LOG.info("=== STREAM SUPER TRACE: no stream within 64 blocks of ({},{}) in this plate ===", var2, var3);
      }
   }

   private static int a(RiverEngine var0, double var1, double var3) {
      double var5 = 0.5 + 0.5 * Math.min(1.0, var3 / 3.0);
      return Math.max(var0.config().seaLevel() - 1, (int)Math.floor(var1) - (int)Math.round(var0.config().ditchInset() * var5));
   }

   private static long a(double var0, double var2) {
      long var4 = Math.round(var0 * 16.0) & 4294967295L;
      long var6 = Math.round(var2 * 16.0) & 4294967295L;
      return var4 << 32 | var6;
   }

   private static boolean a(RiverEngine var0, int var1, int var2, int var3, int var4) {
      double var5 = var1 + 0.5;
      double var7 = var2 + 0.5;

      for (int var9 = 0; var9 < var4 / 2; var9++) {
         ColumnPlan var10 = var0.plan((int)Math.floor(var5), (int)Math.floor(var7));
         if (!var10.inCorridor()) {
            return false;
         }

         if (var10.waterSurfaceY() <= var3) {
            return true;
         }

         double var11 = Math.hypot(var10.flowDirX(), var10.flowDirZ());
         if (var11 < 1.0E-6) {
            return false;
         }

         var5 += var10.flowDirX() / var11 * 2.0;
         var7 += var10.flowDirZ() / var11 * 2.0;
      }

      return false;
   }

   public static void laneScan(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      if (var4 > 0) {
         PlateKey var5 = var0.plateAt(var2, var3);
         RiverNetwork var6 = var0.networkFor(var5);
         List var7 = var6.segments();
         int var8 = var0.config().seaLevel();
         byte var9 = 32;
         int var10 = 0;
         int var11 = 0;
         int var12 = 0;
         int var13 = 0;
         int var14 = 0;
         int var15 = 0;
         int var16 = 0;
         StringBuilder var17 = new StringBuilder();
         int var18 = Integer.MIN_VALUE;
         int var19 = Integer.MAX_VALUE;
         int var20 = 0;
         StringBuilder var21 = new StringBuilder();
         int var22 = 0;
         int var23 = 0;
          HashMap<String, Integer> var24 = new HashMap<>();
         double var25 = 0.0;
         MutableBlockPos var27 = new MutableBlockPos();
         long[] var28 = new long[0];
         int var29 = 0;
         String var30 = "";
         boolean var31 = false;
         HashMap var32 = new HashMap();
         HashSet var33 = new HashSet();

         for (int var34 = 0; var34 < var7.size(); var34++) {
            RiverSegment var35 = (RiverSegment)var7.get(var34);
            if (var34 > 0) {
               RiverSegment var36 = (RiverSegment)var7.get(var34 - 1);
               double var37 = var36.x2() - var36.x1();
               double var39 = var36.z2() - var36.z1();
               boolean var41 = var37 * (var35.x2() - var35.x1()) + var39 * (var35.z2() - var35.z1()) < 0.0;
               if (var36.x2() != var35.x1() || var36.z2() != var35.z1() || var41) {
                  var31 = false;
                  var13 += var16;
                  var16 = 0;
                  var19 = Integer.MAX_VALUE;
                  var17.setLength(0);
                  var18 = Integer.MIN_VALUE;
               }
            }

            double var79 = var35.x2() - var35.x1();
            double var38 = var35.z2() - var35.z1();
            double var40 = Math.hypot(var79, var38);
            if (!(var40 < 1.0E-6)) {
               double var42 = -var38 / var40;
               double var44 = var79 / var40;
               int var46 = Math.max(1, (int)Math.ceil(var40));

               for (int var47 = 0; var47 < var46; var47++) {
                  double var48 = (double)var47 / var46;
                  double var50 = var35.x1() + var79 * var48;
                  double var52 = var35.z1() + var38 * var48;
                  int var54 = (int)Math.floor(var50);
                  int var55 = (int)Math.floor(var52);
                  if (Math.abs(var54 - var2) <= var4
                     && Math.abs(var55 - var3) <= var4
                     && var0.plateAt(var54, var55).equals(var5)
                     && var0.plateAt(var54 - var9, var55).equals(var5)
                     && var0.plateAt(var54 + var9, var55).equals(var5)
                     && var0.plateAt(var54, var55 - var9).equals(var5)
                     && var0.plateAt(var54, var55 + var9).equals(var5)) {
                     ColumnPlan var56 = var0.plan(var54, var55);
                     if (var56.inCorridor() && var56.inChannel() && var0.lakeAt(var54, var55) == null && var56.waterSurfaceY() > var8 - 1) {
                        double var57 = var35.widthAt(var48);
                        int var59 = Math.max(1, (int)Math.ceil(var57));
                        long[] var60 = new long[2 * var59 + 1];
                        int var61 = 0;
                        int var62 = 0;
                        int var63 = Integer.MIN_VALUE;
                        int var64 = Integer.MAX_VALUE;
                        StringBuilder var65 = new StringBuilder(
                           String.format(
                              Locale.ROOT,
                              "centre(%d,%d) dir(%.2f,%.2f) w=%.2f ws=%d:",
                              var54,
                              var55,
                              var79 / var40,
                              var38 / var40,
                              var57,
                              var56.waterSurfaceY()
                           )
                        );

                        for (int var66 = -var59; var66 <= var59; var66++) {
                           int var67 = (int)Math.floor(var50 + var42 * var66);
                           int var68 = (int)Math.floor(var52 + var44 * var66);
                           ColumnPlan var69 = var0.plan(var67, var68);
                           if (var69.inCorridor() && var69.inChannel()) {
                              long var70 = (long)(var67 >> 4) << 32 ^ var68 >> 4 & 4294967295L;
                              if (var33.add(var70) && !var1.getChunkSource().hasChunk(var67 >> 4, var68 >> 4)) {
                                 var20++;
                              }

                              var1.getChunk(var67 >> 4, var68 >> 4);
                              var62++;
                              long var72 = (long)var67 << 32 ^ var68 & 4294967295L;
                              Integer var74 = (Integer)var32.get(var72);
                              int var75 = var74 != null ? var74 : b(var1, var27, var67, var68, var69.waterSurfaceY());
                              var32.put(var72, var75);
                              if (var75 != Integer.MIN_VALUE) {
                                 var60[var61++] = (long)var67 << 32 ^ var68 & 4294967295L;
                                 var63 = Math.max(var63, var75);
                                 var65.append(String.format(Locale.ROOT, " (%d,%d)~%d", var67, var68, var75));
                              } else {
                                 var64 = Math.min(var64, a(var1, var27, var67, var68, var69.waterSurfaceY()));
                                 var27.set(var67, var69.waterSurfaceY(), var68);
                                 var65.append(
                                    String.format(
                                       Locale.ROOT,
                                       " (%d,%d)%s@%d lat=%.2f",
                                       var67,
                                       var68,
                                       var1.getBlockState(var27).getBlock().toString().replace("Block{minecraft:", "{"),
                                       var69.waterSurfaceY(),
                                       var69.lateral()
                                    )
                                 );
                                 var27.set(var67, var69.waterSurfaceY(), var68);
                                 if (!var1.getBlockState(var27).isAir()) {
                                    var23++;
                                    String var76 = var1.getBiome(var27).unwrapKey().map(var0x -> var0x.location().toString()).orElse("?");
                                    var24.merge(var76, 1, Integer::sum);
                                    var25 = Math.max(var25, var69.slope());
                                 }
                              }
                           }
                        }

                        if (var62 == 0) {
                           var31 = false;
                        } else {
                           var10++;
                           if (var61 == 0) {
                              var16++;
                              var19 = Math.min(var19, var64);
                              var17.append(" DRY(").append(var54).append(',').append(var55).append(')');
                              Lake var81 = var0.lakeAt(var54, var55);
                              ColumnPlan var83 = var0.altPlan(var54, var55);
                              Constants.LOG
                                 .info(
                                    "[SR-LANESCAN] dry section: {} | sill {} previous wet top {} | lake={} distToJunction={} flatRun={} altChannel={}",
                                    new Object[]{
                                       var65,
                                       var64,
                                       var18,
                                       var81 == null ? "none" : var81.level(),
                                       var56.distToJunction() == Double.MAX_VALUE ? "far" : String.format(Locale.ROOT, "%.1f", var56.distToJunction()),
                                       String.format(Locale.ROOT, "%.0f", var56.flatRun()),
                                       var83 != null && var83.inChannel()
                                    }
                                 );
                              var31 = false;
                           } else {
                              if (var16 > 0) {
                                 if (var18 != Integer.MIN_VALUE && var19 >= var18) {
                                    if (a(var0, var1, var27, var32, var28, var29, var60, var61, (int)Math.ceil(var57) * 2 + var16)) {
                                       var14 += var16;
                                    } else {
                                       var11 += var16;
                                       if (var22 < 24) {
                                          var21.append(var17);
                                          var22++;
                                       }
                                    }
                                 } else {
                                    var12 += var16;
                                 }

                                 var16 = 0;
                                 var19 = Integer.MAX_VALUE;
                                 var17.setLength(0);
                              }

                              var18 = var63;
                              if (var31) {
                                 boolean var80 = a(var0, var1, var27, var32, var28, var29, var60, var61, (int)Math.ceil(var57));
                                 if (!var80) {
                                    if (var18 != var63) {
                                       var12++;
                                    } else {
                                       var15++;
                                       if (var22 < 24) {
                                          var21.append(" BREAK(").append(var54).append(',').append(var55).append(')');
                                          var22++;
                                       }

                                       Lake var82 = var0.lakeAt(var54, var55);
                                       ColumnPlan var84 = var0.altPlan(var54, var55);
                                       Constants.LOG
                                          .info(
                                             "[SR-LANESCAN] break: {} | lake={} distToJunction={} altChannel={} | previous section: {}",
                                             new Object[]{
                                                var65,
                                                var82 == null ? "none" : var82.level(),
                                                var56.distToJunction() == Double.MAX_VALUE ? "far" : String.format(Locale.ROOT, "%.1f", var56.distToJunction()),
                                                var84 != null && var84.inChannel(),
                                                var30
                                             }
                                          );
                                    }
                                 }
                              }

                              var28 = var60;
                              var29 = var61;
                              var30 = var65.toString();
                              var31 = true;
                           }
                        }
                     } else {
                        var31 = false;
                     }
                  } else {
                     var31 = false;
                  }
               }
            }
         }

         var13 += var16;
         Constants.LOG
            .info(
               "[SR-LANESCAN] plate {} (within {} of {},{}): {} channel sections, {} DRY (dammed), {} broken lane transitions; {} waterfall lips + {} outlet drops (dry by design), {} corner sections the water went round ({} chunks generated for the read)",
               new Object[]{var5, var4, var2, var3, var10, var11, var15, var12, var13, var14, var20}
            );
         if (var11 == 0 && var15 == 0) {
            Constants.LOG.info("[SR-LANESCAN] CLEAN: every carved reach carries an unbroken wet lane.");
         } else {
            Constants.LOG.info("[SR-LANESCAN] FAIL:{}", var21);
         }

         StringBuilder var78 = new StringBuilder();
         var24.entrySet()
            .stream()
            .sorted((var0x, var1x) -> (Integer)var1x.getValue() - (Integer)var0x.getValue())
            .limit(12L)
            .forEach(var1x -> var78.append(' ').append((String)var1x.getKey()).append('=').append(var1x.getValue()));
         Constants.LOG
            .info(
               "[SR-LANESCAN] bars: {} raised in-channel columns, steepest reach {}; by biome:{}",
               new Object[]{var23, String.format(Locale.ROOT, "%.3f", var25), var78}
            );
      }
   }

   private static boolean a(
      RiverEngine var0, ServerLevel var1, MutableBlockPos var2, Map<Long, Integer> var3, long[] var4, int var5, long[] var6, int var7, int var8
   ) {
      HashSet var9 = new HashSet();

      for (int var10 = 0; var10 < var7; var10++) {
         var9.add(var6[var10]);
      }

      HashSet var25 = new HashSet();
      ArrayDeque var11 = new ArrayDeque();

      for (int var12 = 0; var12 < var5; var12++) {
         if (var9.contains(var4[var12])) {
            return true;
         }

         if (var25.add(var4[var12])) {
            var11.add(var4[var12]);
         }
      }

      int var26 = Math.max(64, 16 * var8 * var8);

      while (!var11.isEmpty() && var26-- > 0) {
         long var13 = (Long)var11.poll();
         int var15 = (int)(var13 >> 32);
         int var16 = (int)var13;

         for (int var17 = -1; var17 <= 1; var17++) {
            for (int var18 = -1; var18 <= 1; var18++) {
               if (var17 != 0 || var18 != 0) {
                  int var19 = var15 + var17;
                  int var20 = var16 + var18;
                  long var21 = (long)var19 << 32 ^ var20 & 4294967295L;
                  if (var25.add(var21)) {
                     Integer var23 = (Integer)var3.get(var21);
                     if (var23 == null) {
                        ColumnPlan var24 = var0.plan(var19, var20);
                        var23 = var24.inCorridor() && var24.inChannel() && var1.getChunkSource().hasChunk(var19 >> 4, var20 >> 4)
                           ? b(var1, var2, var19, var20, var24.waterSurfaceY())
                           : Integer.MIN_VALUE;
                        var3.put(var21, var23);
                     }

                     if (var23 != Integer.MIN_VALUE) {
                        if (var9.contains(var21)) {
                           return true;
                        }

                        var11.add(var21);
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static int a(ServerLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      for (int var5 = var4 + 2; var5 >= var4 - 8; var5--) {
         var1.set(var2, var5, var3);
         BlockState var6 = var0.getBlockState(var1);
         if (!var6.isAir() && var6.getFluidState().isEmpty()) {
            return var5;
         }
      }

      return Integer.MIN_VALUE;
   }

   private static int b(ServerLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      for (int var5 = var4 + 2; var5 >= var4 - 8; var5--) {
         var1.set(var2, var5, var3);
         if (var0.getBlockState(var1).getFluidState().is(FluidTags.WATER)) {
            return var5;
         }
      }

      return Integer.MIN_VALUE;
   }

   private static byte b(double var0, double var2) {
      if (var0 * var0 + var2 * var2 < 1.0E-6) {
         return -1;
      }

      int var4 = (int)Math.round(Math.atan2(var2, var0) / (Math.PI / 4));
      return (byte)Math.floorMod(var4, 8);
   }

   private static void a(PlateKey var0, int var1, int var2, int var3, int var4, int[] var5, boolean[] var6, byte[] var7) {
      int var8 = 0;
      int var9 = 0;
      int var10 = 0;
      int var11 = 0;
      int var12 = 0;
      StringBuilder var13 = new StringBuilder();
      int var14 = 0;

      for (int var15 = 0; var15 < var4; var15++) {
         for (int var16 = 0; var16 < var4; var16++) {
            int var17 = var15 * var4 + var16;
            byte var18 = var7[var17];
            if (var18 >= 0 && var6[var17]) {
               int var19 = var15 + aa[var18];
               int var20 = var16 + Z[var18];
               if (var19 >= 0 && var19 < var4 && var20 >= 0 && var20 < var4) {
                  int var21 = var19 * var4 + var20;
                  if (var6[var21]) {
                     byte var22 = var7[var21];
                     if (var22 >= 0) {
                        int var23 = Math.floorMod(var22 - var18, 8);
                        if (var23 > 1 && var23 < 7) {
                           continue;
                        }
                     }

                     var8++;
                     int var26 = var5[var21] - var5[var17];
                     if (var26 > 0) {
                        var9++;
                        int var24 = var1 + (var16 - var3);
                        int var25 = var2 + (var15 - var3);
                        if (var26 > var10) {
                           var10 = var26;
                           var11 = var24;
                           var12 = var25;
                        }

                        if (var14 < 24) {
                           var13.append(String.format(Locale.ROOT, " (%d,%d)+%d", var24, var25, var26));
                           var14++;
                        }
                     }
                  }
               }
            }
         }
      }

      Constants.LOG
         .info(
            "[SR-RISESCAN] plate {} (whole-plate sweep from {},{}): {} columns where the water surface RISES downstream, of {} checked",
            new Object[]{var0, var1, var2, var9, var8}
         );
      if (var9 == 0) {
         Constants.LOG.info("[SR-RISESCAN] CLEAN: water never rises downstream in the region.");
      } else {
         Constants.LOG.info("[SR-RISESCAN] worst: +{} at ({},{})", new Object[]{var10, var11, var12});
         Constants.LOG.info("[SR-RISESCAN] columns:{}", var13.toString());
      }
   }

   public static void spoutScan(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      ChunkGenerator var5 = var1.getChunkSource().getGenerator();
      RandomState var6 = var1.getChunkSource().randomState();
      int var7 = var4 * 2 + 1;
      int[] var8 = new int[var7 * var7];
      boolean[] var9 = new boolean[var7 * var7];
      byte[] var10 = new byte[var7 * var7];
      Arrays.fill(var10, (byte)-1);
      PlateKey var11 = var0.plateAt(var2, var3);
      byte var12 = 32;

      for (int var13 = -var4; var13 <= var4; var13++) {
         for (int var14 = -var4; var14 <= var4; var14++) {
            int var15 = var2 + var14;
            int var16 = var3 + var13;
            if (var0.plateAt(var15, var16).equals(var11)
               && var0.plateAt(var15 - var12, var16).equals(var11)
               && var0.plateAt(var15 + var12, var16).equals(var11)
               && var0.plateAt(var15, var16 - var12).equals(var11)
               && var0.plateAt(var15, var16 + var12).equals(var11)
               && var0.plateAt(var15 - var12, var16 - var12).equals(var11)
               && var0.plateAt(var15 + var12, var16 + var12).equals(var11)
               && var0.plateAt(var15 - var12, var16 + var12).equals(var11)
               && var0.plateAt(var15 + var12, var16 - var12).equals(var11)) {
               int var17 = (var13 + var4) * var7 + var14 + var4;
               int var18 = Integer.MIN_VALUE;
               ColumnPlan var19 = var0.plan(var15, var16);
               if (var19.inCorridor()) {
                  var18 = var19.waterSurfaceY();
                  var10[var17] = b(var19.flowDirX(), var19.flowDirZ());
               }

               int var20 = var0.lakeSurfaceAt(var15, var16);
               if (var20 != Integer.MIN_VALUE && var20 > var18) {
                  var18 = var20;
               }

               if (var18 != Integer.MIN_VALUE) {
                  var9[var17] = true;
                  var8[var17] = var18;
               }
            }
         }
      }

      byte var38 = 6;
      byte var39 = 1;
      byte var40 = 4;
      int var41 = 0;
      int var42 = 0;
      int var43 = 0;
      int var44 = 0;
      int var45 = 0;
      int var21 = 0;
      int[] var22 = new int[48];
      StringBuilder var23 = new StringBuilder();
      int var24 = 0;

      for (int var25 = 0; var25 < var7; var25++) {
         for (int var26 = 0; var26 < var7; var26++) {
            int var27 = var25 * var7 + var26;
            if (var9[var27]) {
               int var28 = var8[var27];
               int var29 = Integer.MAX_VALUE;
               int var30 = 0;
               int var31 = 0;

               for (int var32 = -var38; var32 <= var38; var32++) {
                  for (int var33 = -var38; var33 <= var38; var33++) {
                     if (var32 != 0 || var33 != 0) {
                        int var34 = var25 + var32;
                        int var35 = var26 + var33;
                        if (var34 >= 0 && var35 >= 0 && var34 < var7 && var35 < var7) {
                           int var36 = var34 * var7 + var35;
                           if (var9[var36]) {
                              var31++;
                              int var37 = var8[var36];
                              if (var37 < var29) {
                                 var29 = var37;
                              }

                              if (var37 >= var28) {
                                 var30++;
                              }
                           }
                        }
                     }
                  }
               }

               if (var31 >= 8 && var30 <= var31 / 10 && var28 - var29 >= var39) {
                  int var48 = 0;

                  for (int var49 = -var38; var49 <= var38; var49++) {
                     for (int var51 = -var38; var51 <= var38; var51++) {
                        int var53 = var25 + var49;
                        int var55 = var26 + var51;
                        if (var53 >= 0 && var55 >= 0 && var53 < var7 && var55 < var7) {
                           int var57 = var53 * var7 + var55;
                           if (var9[var57] && var8[var57] <= var29 + var40) {
                              var48++;
                           }
                        }
                     }
                  }

                  if (var48 * 10 < var31 * 6) {
                     var42++;
                  } else {
                     int var50 = var2 + (var26 - var4);
                     int var52 = var3 + (var25 - var4);
                     int var54 = var5.getBaseHeight(var50, var52, Types.OCEAN_FLOOR_WG, var1, var6) - 1;
                     if (var28 <= var54) {
                        var43++;
                     } else {
                        var41++;
                        int var56 = var28 - var29;
                        var22[Math.min(var22.length - 1, var56)]++;
                        if (var56 > var44) {
                           var44 = var56;
                           var45 = var2 + (var26 - var4);
                           var21 = var3 + (var25 - var4);
                        }

                        if (var24 < 24) {
                           var23.append(" (")
                              .append(var2 + (var26 - var4))
                              .append(',')
                              .append(var3 + (var25 - var4))
                              .append(")+")
                              .append(var56)
                              .append("@y")
                              .append(var28);
                           var24++;
                        }
                     }
                  }
               }
            }
         }
      }

      a(var11, var2, var3, var4, var7, var8, var9, var10);
      Constants.LOG
         .info(
            "[SR-SPOUTSCAN] plate {} (whole-plate sweep from {},{}): {} REAL perched-spout columns (water poking >= {} above a surrounding flat water body AND above natural ground) -- ignored {} cascades + {} buried (planned surface below ground)",
            new Object[]{var11, var2, var3, var41, Integer.valueOf(var39), var42, var43}
         );
      if (var41 == 0) {
         Constants.LOG.info("[SR-SPOUTSCAN] CLEAN: no water poking above a surrounding water body in the region.");
      } else {
         Constants.LOG.info("[SR-SPOUTSCAN] worst: +{} at ({},{})", new Object[]{var44, var45, var21});
         StringBuilder var46 = new StringBuilder("[SR-SPOUTSCAN] histogram (height:columns):");

         for (int var47 = var39; var47 < var22.length; var47++) {
            if (var22[var47] > 0) {
               var46.append(" +").append(var47).append(":").append(var22[var47]);
            }
         }

         Constants.LOG.info(var46.toString());
         Constants.LOG.info("[SR-SPOUTSCAN] columns:{}", var23.toString());
      }
   }

   public static void traceColumn(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      int var4 = var2;
      int var5 = var3;
      ColumnPlan var6 = var0.plan(var2, var3);
      if (!var6.inCorridor()) {
         double var7 = Double.MAX_VALUE;

         for (int var9 = -10; var9 <= 10; var9++) {
            for (int var10 = -10; var10 <= 10; var10++) {
               ColumnPlan var11 = var0.plan(var2 + var10, var3 + var9);
               if (var11.inCorridor() && var11.distance() < var7) {
                  var7 = var11.distance();
                  var6 = var11;
                  var4 = var2 + var10;
                  var5 = var3 + var9;
               }
            }
         }
      }

      if (!var6.inCorridor()) {
         Constants.LOG
            .info(
               "=== STREAM TRACE: no stream corridor within 10 blocks of ({},{}) ==={}{}",
               new Object[]{var2, var3, System.lineSeparator(), a(var1, var2, var3)}
            );
      } else {
         int var23 = var0.config().seaLevel();
         int var8 = var6.waterSurfaceY();
         ChunkGenerator var24 = var1.getChunkSource().getGenerator();
         RandomState var25 = var1.getChunkSource().randomState();
         int var26 = var24.getBaseHeight(var4, var5, Types.OCEAN_FLOOR_WG, var1, var25) - 1;
         StringBuilder var12 = new StringBuilder();
         var12.append(String.format("%n=== STREAM TRACE (%d,%d) ===%n", var4, var5));
         int var13 = Math.max(var6.waterSurfaceY(), var23);
         HashMap var14 = new HashMap();
         RiverCarverFeature.BankEnv var15 = RiverCarverFeature.a(var1, var4, var5, var13, var14, var0.config());
         double var16 = RiverCarverFeature.braidOverlapPublic(var0, var4, var5);
         double var18 = RiverCarverFeature.effectiveCohesionPublic(var1, var15, var4, var26, var5);
         int var20 = ChannelBed.bedDepth(
            var6, var0.config(), var18, var15.wetness(), var15.cold(), var1.getSeed(), var4, var5, var6.waterSurfaceY(), var26, var16
         );
         var12.append("BRAID-LIVE ")
            .append(ChannelBed.braidDiag(var6, var0.config(), var18, var15.wetness(), var15.cold(), var6.waterSurfaceY(), var26, var16))
            .append(" => bedDepth=")
            .append(var20)
            .append(System.lineSeparator());
         var12.append(a(var1, var4, var5));
         var12.append(String.format("plan: rawGradeY=%.2f  waterSurfaceY=%d  ground=%d  perch(water-ground)=%+d%n", var6.gradeY(), var8, var26, var8 - var26));
         var12.append(
            String.format(
               "  sea floor (seaLevel-1) = %d  ->  %s%n",
               var23 - 1,
               var8 <= var23 - 1
                  ? "water is AT/below the sea floor -- the floor MAY be lifting it (check ground vs grade below)"
                  : "water is ABOVE the sea floor -- floor is NOT the cause; the grade itself sits high here"
            )
         );
         var12.append(
            String.format(
               "  distance=%.1f halfWidth=%.1f source=%b terminal=%b inChannel=%b%n",
               var6.distance(),
               var6.width(),
               var6.source(),
               var6.terminal(),
               var6.inChannel()
            )
         );
         var13 = var6.bedY();
         byte var28 = 24;
         double var29 = var6.distToOutlet();
         boolean var17 = var26 < var8;
         String var30 = "-";
         if (var26 > var13) {
            double var19 = var29 >= 8.988465674311579E307 ? 1.0 : Math.min(1.0, var29 / var28);
            double var21 = var19 * var19 * (3.0 - 2.0 * var19);
            var30 = String.valueOf((int)Math.round(var26 + (var13 - var26) * var21));
         }

         var12.append(
            String.format(
               "  MOUTH NOTCH(offshore): deepBed=%d ambientFloor=%d distToOutlet=%s  submergedHere=%b  notchBed=%s%n",
               var13,
               var26,
               var29 >= 8.988465674311579E307 ? "none" : String.format("%.1f", var29),
               var17,
               var30
            )
         );
         var12.append("longitudinal (follow the channel; ground is the floor beneath the centreline):\n");
          List<String> var31 = a(var0, var1, var4, var5, -1);
         Collections.reverse(var31);

          for (String var34 : var31) {
            var12.append(var34).append('\n');
         }

         var12.append(String.format("   >> HERE (%d,%d): rawGrade=%.2f water=%d ground=%d perch=%+d%n", var4, var5, var6.gradeY(), var8, var26, var8 - var26));

         for (String var35 : a(var0, var1, var4, var5, 1)) {
            var12.append(var35).append('\n');
         }

         Constants.LOG.info(var12.toString());
      }
   }

   private static List<String> a(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
          ArrayList<String> var5 = new ArrayList<>();
      ChunkGenerator var6 = var1.getChunkSource().getGenerator();
      RandomState var7 = var1.getChunkSource().randomState();
      double var8 = var2 + 0.5;
      double var10 = var3 + 0.5;

      for (int var12 = 1; var12 <= 30; var12++) {
         ColumnPlan var13 = var0.plan((int)Math.floor(var8), (int)Math.floor(var10));
         double var14 = Math.hypot(var13.flowDirX(), var13.flowDirZ());
         if (!var13.inCorridor() || var14 < 1.0E-6) {
            break;
         }

         var8 += var4 * (var13.flowDirX() / var14) * 2.0;
         var10 += var4 * (var13.flowDirZ() / var14) * 2.0;
         int var16 = (int)Math.floor(var8);
         int var17 = (int)Math.floor(var10);
         ColumnPlan var18 = var0.plan(var16, var17);
         if (!var18.inCorridor()) {
            break;
         }

         int var19 = var6.getBaseHeight(var16, var17, Types.OCEAN_FLOOR_WG, var1, var7) - 1;
         int var20 = var18.waterSurfaceY();
         var5.add(
            String.format(
               "   %s%-2d (%d,%d): rawGrade=%6.2f water=%d ground=%d perch=%+d",
               var4 < 0 ? "up" : "dn",
               var12,
               var16,
               var17,
               var18.gradeY(),
               var20,
               var19,
               var20 - var19
            )
         );
      }

      return var5;
   }

   private static void a(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      int var4 = var2;
      int var5 = var3;
      ColumnPlan var6 = var0.plan(var2, var3);
      if (!var6.inCorridor()) {
         double var7 = Double.MAX_VALUE;

         for (int var9 = -10; var9 <= 10; var9++) {
            for (int var10 = -10; var10 <= 10; var10++) {
               ColumnPlan var11 = var0.plan(var2 + var10, var3 + var9);
               if (var11.inCorridor() && var11.distance() < var7) {
                  var7 = var11.distance();
                  var6 = var11;
                  var4 = var2 + var10;
                  var5 = var3 + var9;
               }
            }
         }
      }

      if (!var6.inCorridor()) {
         Constants.LOG.info("=== PERCH DIAG: no stream corridor within 10 blocks of ({},{}) ===", var2, var3);
      } else {
         int var41 = var6.waterSurfaceY();
         ChunkGenerator var8 = var1.getChunkSource().getGenerator();
         RandomState var42 = var1.getChunkSource().randomState();
         int var43 = var8.getBaseHeight(var4, var5, Types.OCEAN_FLOOR_WG, var1, var42) - 1;
         int var44 = var1.getHeight(Types.OCEAN_FLOOR, var4, var5) - 1;
         int var12 = var1.getHeight(Types.WORLD_SURFACE, var4, var5) - 1;
         double var13 = Math.hypot(var6.flowDirX(), var6.flowDirZ());
         StringBuilder var15 = new StringBuilder();
         String var16 = "n/a";
         String var17 = "n/a";
         if (var13 > 1.0E-6) {
            double var18 = -var6.flowDirZ() / var13;
            double var20 = var6.flowDirX() / var13;

            for (byte var22 = -1; var22 <= 1; var22 += 2) {
               StringBuilder var23 = new StringBuilder();
               int var24 = -1;

               for (int var25 = 1; var25 <= 16; var25++) {
                  int var26 = var4 + (int)Math.round(var18 * var25 * var22);
                  int var27 = var5 + (int)Math.round(var20 * var25 * var22);
                  int var28 = var8.getBaseHeight(var26, var27, Types.OCEAN_FLOOR_WG, var1, var42) - 1;
                  var23.append(String.format("%4d", var28));
                  if (var24 < 0 && var28 >= var41) {
                     var24 = var25;
                  }
               }

               String var54 = var22 < 0 ? "L" : "R";
               var15.append("  ").append(var54).append("(out->):").append(var23).append('\n');
               String var57 = var24 < 0 ? "NEVER (>= grade)" : "rises to grade at d=" + var24;
               if (var22 < 0) {
                  var16 = var57;
               } else {
                  var17 = var57;
               }
            }
         }

         boolean var45 = !"NEVER (>= grade)".equals(var16) && !"NEVER (>= grade)".equals(var17) && var13 > 1.0E-6;
         StringBuilder var19 = new StringBuilder();
         boolean var46 = false;
         boolean var21 = false;
         if (var13 > 1.0E-6) {
            double var47 = -var6.flowDirZ() / var13;
            double var51 = var6.flowDirX() / var13;

            for (byte var58 = -1; var58 <= 1; var58 += 2) {
               StringBuilder var62 = new StringBuilder();

               for (int var65 = 1; var65 <= 12; var65++) {
                  boolean var29 = var0.plan(var4 + (int)Math.round(var47 * var65 * var58), var5 + (int)Math.round(var51 * var65 * var58)).distance() <= 2.0;
                  var62.append(var29 ? " C" : " .");
                  if (var29) {
                     if (var58 < 0) {
                        var21 = true;
                     } else {
                        var46 = true;
                     }
                  }
               }

               var19.append("  ").append(var58 < 0 ? "L" : "R").append("(out->):").append(var62).append('\n');
            }
         }

         if (var13 > 1.0E-6) {
            double var48 = -var6.flowDirZ() / var13;
            double var52 = var6.flowDirX() / var13;
            StringBuilder var59 = new StringBuilder();
            var59.append("containment walk (d out each side; grade=col waterSurfaceY, g=pre-carve ground, T=terminal[disables cup+berm], ch=inChannel):\n");
            var59.append(
               String.format(
                  "    d= 0 (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%n",
                  var4,
                  var5,
                  var6.waterSurfaceY(),
                  var43,
                  var6.waterSurfaceY() - var43,
                  var6.terminal(),
                  var6.inChannel(),
                  var6.source(),
                  var6.distance()
               )
            );

            for (byte var63 = -1; var63 <= 1; var63 += 2) {
               var59.append("  ").append(var63 < 0 ? "L (rim/mountain side):" : "R (open/sea side):").append('\n');

               for (int var66 = 1; var66 <= 12; var66++) {
                  int var69 = var4 + (int)Math.round(var48 * var66 * var63);
                  int var30 = var5 + (int)Math.round(var52 * var66 * var63);
                  ColumnPlan var31 = var0.plan(var69, var30);
                  int var32 = var8.getBaseHeight(var69, var30, Types.OCEAN_FLOOR_WG, var1, var42) - 1;
                  var59.append(
                     String.format(
                        "    d=%2d (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%s%n",
                        var66,
                        var69,
                        var30,
                        var31.waterSurfaceY(),
                        var32,
                        var31.waterSurfaceY() - var32,
                        var31.terminal(),
                        var31.inChannel(),
                        var31.source(),
                        var31.distance(),
                        var31.inCorridor() ? "" : " [OUT-OF-CORRIDOR]"
                     )
                  );
               }
            }

            Constants.LOG.info(var59.toString());
         }

         Lake var49 = var0.lakeAt(var4, var5);
         int var50 = -1;
         int var53 = Integer.MIN_VALUE;

         for (int var55 = 1; var55 <= 24 && var50 < 0; var55++) {
            for (int var60 = -var55; var60 <= var55 && var50 < 0; var60++) {
               for (int var64 = -var55; var64 <= var55 && var50 < 0; var64++) {
                  if (Math.max(Math.abs(var64), Math.abs(var60)) == var55) {
                     Lake var67 = var0.lakeAt(var4 + var64, var5 + var60);
                     if (var67 != null) {
                        var50 = var55;
                        var53 = var67.level();
                     }
                  }
               }
            }
         }

         String var56 = "covering this column: "
            + (var49 != null ? "YES (level=" + var49.level() + ")" : "none")
            + ";  nearest Lake object within 24 blocks: "
            + (var50 >= 0 ? "d=" + var50 + " level=" + var53 : "NONE (this water is a hollow-pool / pond, not a basin Lake)");
         Constants.LOG
            .info(
               "=== PERCH DIAG column ({},{}) ===\nLAKE object {}\ngrade(waterSurfaceY)={}  rawGradeY={}  bedY={}  GROUND(pre-carve)={}  WALL(post-carve)={}  waterTop(placed)={}\nperch? grade-ground = {}   (>0 = water sits ABOVE ground here)\ninChannel={} distance={} halfWidth={} slope={} upstreamSlope={} terminal={} source={}\nperpendicular ground profile (each col = +1 block out from channel, to 16):\n{}left rim: {}   right rim: {}\n=> hollowEnclosed would be {} (both sides must rise to grade within 16) -> {}\nperpendicular STREAM scan (engine; C=stream centerline within 2 blocks, .=none, out to 12):\n{}this col inAnyChannel={}  stream on L-side={} R-side={}  => bermYieldsToNeighbor(both sides)={}",
               new Object[]{
                  var4,
                  var5,
                  var56,
                  var41,
                  String.format("%.1f", var6.gradeY()),
                  var6.bedY(),
                  var43,
                  var44,
                  var12,
                  var41 - var43,
                  var6.inChannel(),
                  String.format("%.1f", var6.distance()),
                  String.format("%.1f", var6.width()),
                  String.format("%.3f", var6.slope()),
                  String.format("%.3f", var6.upstreamSlope()),
                  var6.terminal(),
                  var6.source(),
                  var15,
                  var16,
                  var17,
                  var45,
                  var45 ? "POOLS to grade" : "falls through to berm/bank carve (stays perched if grade>ground)",
                  var19,
                  var0.inAnyChannel(var4, var5),
                  var21,
                  var46,
                  var46 && var21
               }
            );
         if (var13 > 1.0E-6) {
            double var61 = -var6.flowDirZ() / var13;
            double var68 = var6.flowDirX() / var13;
            byte var70 = 16;
            int var71 = var41 + 2;
            int var72 = var41 - 34;
            MutableBlockPos var33 = new MutableBlockPos();
            StringBuilder var34 = new StringBuilder();
            var34.append("     ").append(" ".repeat(var70)).append("v  centre (d=0), L=-d out to left, R=+d out to right\n");

            for (int var35 = var71; var35 >= var72; var35--) {
               var34.append(String.format("y%3d ", var35));

               for (int var36 = -var70; var36 <= var70; var36++) {
                  int var37 = var4 + (int)Math.round(var61 * var36);
                  int var38 = var5 + (int)Math.round(var68 * var36);
                  FluidState var39 = var1.getFluidState(var33.set(var37, var35, var38));
                  int var40;
                  if (!var39.isEmpty() && var39.is(FluidTags.WATER)) {
                     var40 = var39.getType() != Fluids.WATER && var39.getType() != Fluids.FLOWING_WATER ? 126 : 111;
                  } else if (var1.getBlockState(var33).isAir()) {
                     var40 = 32;
                  } else {
                     var40 = 35;
                  }

                  var34.append((char)var40);
               }

               var34.append('\n');
            }

            byte var73 = 48;
            StringBuilder var74 = new StringBuilder();

            for (int var75 = -2; var75 <= 2; var75++) {
               for (int var76 = -2; var76 <= 2; var76++) {
                  PlateKey var77 = var0.plateAt(var4 + var76 * var73, var5 + var75 * var73);
                  var74.append(String.format(" %3d,%-3d", var77.cx(), var77.cz()));
               }

               var74.append('\n');
            }

            Constants.LOG
               .info(
                  "cross-section perpendicular to flow (post-carve blocks; ~=stream o=vanilla-water #=solid):\n{}plate (cx,cz) on a {}-block grid, centre row/col = this column (differing ids => a plate seam):\n{}",
                  new Object[]{var34, Integer.valueOf(var73), var74}
               );
         }
      }
   }

   private static void a(RiverEngine var0, ServerLevel var1, ChunkPos var2) {
      int var3 = var2.getMinBlockX();
      int var4 = var2.getMinBlockZ();
      ChunkGenerator var5 = var1.getChunkSource().getGenerator();
      RandomState var6 = var1.getChunkSource().randomState();
      StringBuilder var7 = new StringBuilder();
      StringBuilder var8 = new StringBuilder();
      StringBuilder var9 = new StringBuilder();
      StringBuilder var10 = new StringBuilder();
      int var11 = 0;

      for (int var12 = 0; var12 < 16; var12++) {
         for (int var13 = 0; var13 < 16; var13++) {
            int var14 = var3 + var13;
            int var15 = var4 + var12;
            int var16 = var5.getBaseHeight(var14, var15, Types.OCEAN_FLOOR_WG, var1, var6) - 1;
            var8.append(String.format("%4d", var16));
            double var17 = var0.groundHeight(var14, var15);
            if (var17 <= -999.0) {
               var9.append("   ~");
            } else {
               var9.append(String.format("%4d", (int)Math.floor(var17)));
               var11 = Math.max(var11, Math.abs((int)Math.floor(var17) - var16));
            }

            var10.append(String.format("%4d", var1.getHeight(Types.OCEAN_FLOOR, var14, var15) - 1));
            ColumnPlan var19 = var0.plan(var14, var15);
            var7.append(var19.inCorridor() ? String.format("%4d", var19.waterSurfaceY()) : "   .");
         }

         var7.append('\n');
         var8.append('\n');
         var9.append('\n');
         var10.append('\n');
      }

      Constants.LOG
         .info(
            "=== PERCH DIAG chunk ({},{}) X[{}..{}] Z[{}..{}] (rows=+Z down, cols=+X right) ===\nGRADE (engine waterSurfaceY; . = not in corridor):\n{}\nGROUND (pre-carve OCEAN_FLOOR_WG, vanilla getBaseHeight, ALL cols):\n{}\nSAMPLED (the network's own height source; ~ = ocean fast path) -- worst disagreement with GROUND: {} blocks:\n{}\nWALL (post-carve OCEAN_FLOOR, ALL cols -- pool/berm shows here):\n{}",
            new Object[]{var2.x, var2.z, var3, var3 + 15, var4, var4 + 15, var7, var8, var11, var9, var10}
         );
   }

   public static boolean startCensus(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      if (var4 > 0 && ab.compareAndSet(false, true)) {
         ChunkGenerator var5 = var1.getChunkSource().getGenerator();
         RandomState var6 = var1.getChunkSource().randomState();
         Thread var7 = new Thread(() -> {
            try {
               a(var0, var5, var6, var1, var2, var3, var4);
            } finally {
               ab.set(false);
            }
         }, "streams-perch-census");
         var7.setDaemon(true);
         var7.setPriority(1);
         var7.start();
         Constants.LOG
            .info("=== PERCH CENSUS started: {}x{} blocks around ({},{}); building networks, please wait ===", new Object[]{var4 * 2, var4 * 2, var2, var3});
         return true;
      } else {
         return false;
      }
   }

   private static void a(RiverEngine var0, ChunkGenerator var1, RandomState var2, ServerLevel var3, int var4, int var5, int var6) {
      try {
         int var7 = var0.config().seaLevel();
         int[] var8 = new int[24];
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;
         int var12 = 0;
         int var13 = Integer.MIN_VALUE;
         int var14 = var4;
         int var15 = var5;
         int var16 = Integer.MAX_VALUE;
         int var17 = Integer.MIN_VALUE;
         int var18 = Integer.MAX_VALUE;
         int var19 = Integer.MIN_VALUE;
         int var20 = 0;
         StringBuilder var21 = new StringBuilder();
         int var22 = 0;

         for (int var23 = -var6; var23 <= var6; var23++) {
            for (int var24 = -var6; var24 <= var6; var24++) {
               int var25 = var4 + var24;
               int var26 = var5 + var23;
               ColumnPlan var27 = var0.plan(var25, var26);
               if (var27.inChannel()) {
                  double var28 = Math.hypot(var27.flowDirX(), var27.flowDirZ());
                  if (!(var28 < 1.0E-6)) {
                     double var30 = -var27.flowDirZ() / var28;
                     double var32 = var27.flowDirX() / var28;
                     double var34 = var27.width() + 2.0;
                     int var36 = var1.getBaseHeight(
                           (int)Math.round(var25 + var30 * var34), (int)Math.round(var26 + var32 * var34), Types.OCEAN_FLOOR_WG, var3, var2
                        )
                        - 1;
                     int var37 = var1.getBaseHeight(
                           (int)Math.round(var25 - var30 * var34), (int)Math.round(var26 - var32 * var34), Types.OCEAN_FLOOR_WG, var3, var2
                        )
                        - 1;
                     int var38 = Math.min(var36, var37);
                     if (var38 < var7) {
                        var10++;
                     } else {
                        var9++;
                        int var39 = var27.waterSurfaceY() - var38;
                        if (var39 > var13) {
                           var13 = var39;
                           var14 = var25;
                           var15 = var26;
                        }

                        if (var39 >= 1) {
                           var11++;
                        }

                        if (var39 >= 4) {
                           var12++;
                        }

                        if (var39 >= 13) {
                           var20++;
                           var16 = Math.min(var16, var25);
                           var17 = Math.max(var17, var25);
                           var18 = Math.min(var18, var26);
                           var19 = Math.max(var19, var26);
                           if (var22 < 16 && (var24 + var23 & 7) == 0) {
                              int var40 = var1.getBaseHeight(var25, var26, Types.OCEAN_FLOOR_WG, var3, var2) - 1;
                              var21.append(
                                 String.format(
                                    "    (%d,%d) grade=%d colGround=%d bankL=%d bankR=%d perch=%d%n",
                                    var25,
                                    var26,
                                    var27.waterSurfaceY(),
                                    var40,
                                    var37,
                                    var36,
                                    var39
                                 )
                              );
                              var22++;
                           }
                        }

                        var8[Math.max(0, Math.min(var8.length - 1, var39))]++;
                     }
                  }
               }
            }
         }

         StringBuilder var42 = new StringBuilder();
         var42.append(String.format("%n=== PERCH CENSUS COMPLETE: %dx%d blocks around (%d,%d) ===%n", var6 * 2, var6 * 2, var4, var5));
         var42.append(String.format("inland channel columns scanned: %d (excluded %d coastal/water-meeting)%n", var9, var10));
         if (var9 == 0) {
            var42.append("  (no stream channels in this area -- try a larger radius or a different spawn)\n");
         } else {
            var42.append(String.format("perched (water above lower bank >= 1): %d (%.1f%%)%n", var11, 100.0 * var11 / var9));
            var42.append(String.format("BADLY perched (>= 4, a raised aqueduct): %d (%.1f%%)%n", var12, 100.0 * var12 / var9));
            var42.append(String.format("max perch: %d blocks, at (%d,%d)%n", var13, var14, var15));
            var42.append("histogram (perch height in blocks : column count):\n");

            for (int var43 = 0; var43 < var8.length; var43++) {
               if (var8[var43] > 0) {
                  var42.append(String.format("  %2d%s : %d%n", var43, var43 == var8.length - 1 ? "+" : " ", var8[var43]));
               }
            }

            if (var20 > 0) {
               var42.append(
                  String.format(
                     "badly-perched (>=13) cluster: %d cols in X[%d..%d] Z[%d..%d] (span %dx%d)%n",
                     var20,
                     var16,
                     var17,
                     var18,
                     var19,
                     var17 - var16,
                     var19 - var18
                  )
               );
               var42.append("samples (grade vs column ground vs the two banks):\n");
               var42.append(var21);
            }
         }

         Constants.LOG.info(var42.toString());
      } catch (Throwable var41) {
         Constants.LOG.warn("Perch census failed", var41);
      }
   }

   private static String a(ServerLevel var0, int var1, int var2) {
      StringBuilder var3 = new StringBuilder();
      RiverFlowField.BiomeClass var4 = RiverFlowFields.classifierFor(var0);
      RiverFlowField.OrientationField var5 = RiverFlowFields.orientationFor(var0);
      boolean var6 = RiverFlowFields.isRegionReady(var1, var2);
      int var7 = RiverFlowFields.dirAt(var4, var5, var1, var2);
      boolean var8 = RiverFlowFields.bridgedNear(var1, var2);
      StringBuilder var9 = new StringBuilder();

      for (int var10 = -1; var10 <= 1; var10++) {
         for (int var11 = -1; var11 <= 1; var11++) {
            int var12 = var4.classify((var1 >> 2 << 2) + var11 * 4, (var2 >> 2 << 2) + var10 * 4);
            var9.append((char)(var12 == 1 ? 'R' : (var12 == 2 ? 'O' : '.')));
         }
      }

      byte[] var15 = StreamFlowGrid.peek(var1 >> 4, var2 >> 4);
      byte var16 = var15 != null && var15.length != 0 ? var15[(var1 & 15) + ((var2 & 15) << 4)] : -1;
      double var17 = var15 != null && var15.length != 0 ? StreamFlowGrid.speedOf(var15, var1, var2) : -1.0;
      byte var14 = ClientFlowGrids.angleAt(var1, var2);
      var3.append(
         String.format(
            Locale.ROOT,
            "FLOW-LIVE vanillaWaterFlow=%s | field: regionReady=%s dir8=%d bridgedNear=%s | quarts(3x3, R=river-biomed O=ocean .=land): %s | serverGrid=%s speed=%.2f | clientGrid=%s%n",
            Services.PLATFORM.vanillaWaterFlow(),
            var6,
            var7,
            var8,
            var9,
            var16 == -1 ? "none" : String.valueOf(var16),
            var17,
            var14 == -1 ? "none" : String.valueOf(var14)
         )
      );
      var3.append(String.format(Locale.ROOT, "FLOW-LIVE deepChannel=%s (roofed sea-level cavity here)%n", RiverFlowFields.deepChannelAt(var1, var2)));
      return var3.toString();
   }

   private static final class PrioritizedTask implements Comparable<StreamPrefetcher.PrioritizedTask>, Runnable {
      static final long a = Long.MIN_VALUE;
      private final int b;
      private final long c;
      private final long d;
      private final long e;
      private final Runnable f;

      PrioritizedTask(int var1, long var2, Runnable var4) {
         this(var1, var2, Long.MIN_VALUE, var4);
      }

      PrioritizedTask(int var1, long var2, long var4, Runnable var6) {
         this(var1, var2, var4, Long.MIN_VALUE, var6);
      }

      PrioritizedTask(int var1, long var2, long var4, long var6, Runnable var8) {
         this.b = var1;
         this.c = var2;
         this.d = var4;
         this.e = var6;
         this.f = var8;
      }

      @Override
      public void run() {
         this.f.run();
      }

      public int compareTo(StreamPrefetcher.PrioritizedTask var1) {
         int var2 = Integer.compare(this.b, var1.b);
         return var2 != 0 ? var2 : Long.compare(this.c, var1.c);
      }
   }
}
