package dev.streamsreflowing.core;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.api.DeadEndResolver;
import dev.streamsreflowing.core.api.Sink;
import dev.streamsreflowing.core.build.BuildGate;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.diag.HeightCensus;
import dev.streamsreflowing.core.diag.ReadTrace;
import dev.streamsreflowing.core.diag.SampleTimer;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.river.FlowSpeed;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import dev.streamsreflowing.worldgen.bank.BankStyle;
import dev.streamsreflowing.worldgen.regionprep.FormEval;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class RiverEngine {
   public static final double OCEAN_SKIP_HEIGHT = -1000.0;
   public static final double RIVER_SKIP_HEIGHT = -1001.0;
   private static final double a = 3.0;
   private static volatile DeadEndResolver b;
   private static final double c = 1.5;
   private static final double d = 2.0;
   private static final double e = 0.5;
   private final long f;
   private final StreamsConfig g;
   private final TerrainSampler h;
   private final WetnessSampler i;
   private final PlateLattice j;
   private final PlateRiverCache k = new PlateRiverCache();
   private final PlateRiverCache l = new PlateRiverCache();
   private final boolean m;
   private final double n;
   private final double o;
   private final ThreadLocal<long[]> p = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0L});
   private final ThreadLocal<Object[]> q = ThreadLocal.withInitial(this::a);
   private final ThreadLocal<Object[]> r = ThreadLocal.withInitial(this::a);
   private final ThreadLocal<double[]> s = ThreadLocal.withInitial(() -> new double[4]);
   private final Set<Object[]> t = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private final Set<Object> u = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
   private volatile RiverEngine.BuildListener v;
   private volatile NetworkStore w;
   private static final AtomicInteger x = new AtomicInteger();
   private static volatile long y = System.nanoTime() - 60000000000L;
   private static final int z = 4;
   private static final int A = 1024;
   private static final int B = 30;
   private static final double C = 0.9375;
   private static final double D = 2.0;
   private static final double E = 6.0;
   private static final double F = 6.0;
   private static final double G = 16.0;
   private static final int H = 6;
   private static final double I = 0.1;
   private static final double J = 0.25;
   private volatile int K;
   private final Object L = new Object();
   private boolean M;
   private volatile IntConsumer N;
   private volatile boolean O;
   private static final int P = 8192;
   private final ThreadLocal<long[]> Q = ThreadLocal.withInitial(() -> {
      long[] var1x = new long[8192];
      Arrays.fill(var1x, Long.MIN_VALUE);
      return this.a(var1x);
   });
   private final ThreadLocal<ColumnPlan[]> R = ThreadLocal.withInitial(() -> this.a(new ColumnPlan[8192]));
   private final ThreadLocal<long[]> S = ThreadLocal.withInitial(() -> {
      long[] var1x = new long[8192];
      Arrays.fill(var1x, Long.MIN_VALUE);
      return this.a(var1x);
   });
   private final ThreadLocal<ColumnPlan[]> T = ThreadLocal.withInitial(() -> this.a(new ColumnPlan[8192]));
   private final ThreadLocal<RiverNetwork[]> U = ThreadLocal.withInitial(() -> this.a(new RiverNetwork[8192]));
   private final ThreadLocal<int[]> V = ThreadLocal.withInitial(() -> this.a(new int[8192]));
   private final ThreadLocal<RiverNetwork[]> W = ThreadLocal.withInitial(() -> new RiverNetwork[1]);
   private final ThreadLocal<int[]> X = ThreadLocal.withInitial(() -> new int[]{-1});
   private final ThreadLocal<long[]> Y = ThreadLocal.withInitial(() -> {
      long[] var1x = new long[8192];
      Arrays.fill(var1x, Long.MIN_VALUE);
      return this.a(var1x);
   });
   private final ThreadLocal<ColumnPlan[]> Z = ThreadLocal.withInitial(() -> this.a(new ColumnPlan[8192]));
   private volatile boolean aa = true;
   private volatile boolean ab;
   private static final int ac = 6;
   public static final AtomicInteger LEAN_CALLS = new AtomicInteger();
   private static final int ad = 4096;
   private final ThreadLocal<long[]> ae = ThreadLocal.withInitial(() -> {
      long[] var1x = new long[4096];
      Arrays.fill(var1x, Long.MIN_VALUE);
      return this.a(var1x);
   });
   private final ThreadLocal<long[]> af = ThreadLocal.withInitial(() -> this.a(new long[4096]));
   private final ThreadLocal<long[]> ag = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0L, 0L});
   private static volatile BiConsumer<RiverEngine, PlateKey> ah;
   public static final ThreadLocal<String> DEMAND_REASON = new ThreadLocal<>();
   private static final boolean ai = Boolean.getBoolean("streamsreflowing.caldiag");
   private static final Set<Long> aj = ConcurrentHashMap.newKeySet();

   public static void setDeadEndResolver(DeadEndResolver var0) {
      b = var0;
   }

   public static DeadEndResolver deadEndResolver() {
      return b;
   }

   public static int insetFor(double var0, StreamsConfig var2) {
      double var3 = 0.5 + 0.5 * Math.min(1.0, var0 / 3.0);
      return (int)Math.round(var2.ditchInset() * var3);
   }

   public TerrainSampler sampler() {
      return this.h;
   }

   private <T> T a(T var1) {
      this.u.add(var1);
      return (T)var1;
   }

   public List<Object> threadCacheArrays() {
      ArrayList var1 = new ArrayList<>(this.u);
      var1.addAll(this.t);
      return var1;
   }

   public long basePlateCacheBytes() {
      return this.k.residentBytes();
   }

   public void forEachCachedNetwork(Consumer<RiverNetwork> var1) {
      this.k.forEachResident(var1);
      this.l.forEachResident(var1);
   }

   public static int planCacheSlots() {
      return 8192;
   }

   private Object[] a() {
      Object[] var1 = new Object[2];
      this.t.add(var1);
      return var1;
   }

   public void releaseThreadMemos() {
      for (Object[] var2 : this.t) {
         var2[0] = null;
         var2[1] = null;
      }

      this.t.clear();
   }

   public RiverEngine(long var1, StreamsConfig var3, TerrainSampler var4) {
      this(var1, var3, var4, (var0, var1x, var2) -> 1.0);
   }

   public RiverEngine(long var1, StreamsConfig var3, TerrainSampler var4, WetnessSampler var5) {
      this.f = var1;
      this.g = var3;
      this.h = var4;
      this.i = var5;
      this.j = new PlateLattice(var1, var3.plateScale(), var3.plateJitter());
      this.m = var3.connectNearbyWater();
      this.n = var3.maxWidth() * (1.0 + var3.bankWidth()) + 2.0;
      this.o = this.m ? Math.max(this.n, var3.connectRadius()) : this.n;
   }

   public StreamsConfig config() {
      return this.g;
   }

   public double sampledHeight(int var1, int var2) {
      return this.h.height(var1, var2);
   }

   public double exactHeight(int var1, int var2) {
      return this.h.groundHeightExact(var1, var2);
   }

   public PlateKey plateAt(int var1, int var2) {
      return this.j.plateAt(var1 + 0.5, var2 + 0.5);
   }

   public double groundHeight(int var1, int var2) {
      return this.h.height(var1, var2);
   }

   public double groundHeightExact(int var1, int var2) {
      return this.h.groundHeightExact(var1, var2);
   }

   public double terrainHeight(int var1, int var2) {
      double var3 = var1 + 0.5;
      double var5 = var2 + 0.5;
      PlateKey var7 = this.j.plateAt(var3, var5);
      if (!this.isPlateReady(var7)) {
         return Double.NaN;
      } else {
         Relief var8 = this.networkFor(var7).relief();
         if (var8 != null && var8.a(var3, var5)) {
            double var9 = var8.b(var3, var5);
            return var9 <= -999.0 ? Double.NaN : var9;
         } else {
            return Double.NaN;
         }
      }
   }

   public PlateLattice lattice() {
      return this.j;
   }

   public void setBuildListener(RiverEngine.BuildListener var1) {
      this.v = var1;
   }

   public void setNetworkStore(NetworkStore var1) {
      this.w = var1;
      this.k.setDiskBacked(var1 != null);
   }

   public NetworkStore networkStore() {
      return this.w;
   }

   public RiverNetwork networkFor(PlateKey var1) {
      if (!this.m) {
         return this.baseFor(var1);
      }

      Object[] var2 = this.q.get();
      if (var1.equals(var2[0])) {
         return (RiverNetwork)var2[1];
      }

      RiverNetwork var3 = this.l
         .get(
            var1,
            () -> NetworkTracer.connectNetwork(var1, this::baseFor, this.j, this.h, this.g, this.f),
            () -> NetworkTracer.connectNetwork(var1, this::baseFor, this.j, this.h, this.g, this.f)
         );
      var2[0] = var1;
      var2[1] = var3;
      return var3;
   }

   public RiverNetwork baseFor(PlateKey var1) {
      Object[] var2 = this.r.get();
      if (var1.equals(var2[0])) {
         return (RiverNetwork)var2[1];
      }

      RiverNetwork var3 = this.a(var1);
      var2[0] = var1;
      var2[1] = var3;
      return var3;
   }

   private RiverNetwork a(PlateKey var1) {
      return this.k.get(var1, () -> {
         NetworkStore var2 = this.w;
         if (var2 != null) {
            RiverNetwork var3 = var2.load(var1);
            if (var3 != null) {
               return var3;
            }
         }

         RiverNetwork var4 = this.b(var1);
         if (var2 != null) {
            var2.save(var1, var4);
         }

         return var4;
      }, () -> this.b(var1));
   }

   public static boolean buildingNetworks() {
      return x.get() > 0 || System.nanoTime() - y < 700000000L;
   }

   public static int activeBuildCount() {
      return x.get();
   }

   private static String a(RiverNetwork var0) {
      long var1 = -3750763034362895579L;

      for (RiverSegment var4 : var0.segments()) {
         var1 = a(var1, var4.minX());
         var1 = a(var1, var4.maxX());
         var1 = a(var1, var4.minZ());
         var1 = a(var1, var4.maxZ());

         for (int var5 = 0; var5 <= 4; var5++) {
            double var6 = var5 / 4.0;
            var1 = a(var1, var4.widthAt(var6));
            var1 = a(var1, var4.gradeAt(var6));
         }
      }

      for (Lake var18 : var0.lakes()) {
         var1 = a(var1, var18.level());
         var1 = a(var1, var18.digDepth());
         var1 = a(var1, var18.spacing());
         var1 = a(var1, var18.cellMinX());
         var1 = a(var1, var18.cellMinZ());
         var1 = a(var1, var18.cellsWide());
      }

      return String.format("%016x", var1);
   }

   private static long a(long var0, double var2) {
      long var4 = Double.doubleToLongBits(var2);

      for (int var6 = 0; var6 < 8; var6++) {
         var0 = (var0 ^ var4 >>> var6 * 8 & 255L) * 1099511628211L;
      }

      return var0;
   }

   public static long plateId(PlateKey var0) {
      return (long)var0.cx() << 32 ^ var0.cz() & 4294967295L;
   }

   private RiverNetwork b(PlateKey var1) {
      x.incrementAndGet();
      PlateDomain var2 = new PlateDomain(this.j, var1);
      int var3 = (var2.minX + var2.maxX) / 2;
      int var4 = (var2.minZ + var2.maxZ) / 2;
      LowResGrid.enterBuildFocus(var3, var4);
      BuildGate.Ticket var5 = BuildGate.enter(plateId(var1), var2.minX, var2.minZ, var2.maxX, var2.maxZ);

      try {
         RiverNetwork var6 = this.c(var1);
         Constants.LOG.info("[SR-NETHASH] plate {} segs={} lakes={} shape={}", new Object[]{var1, var6.segments().size(), var6.lakes().size(), a(var6)});
         return var6;
      } finally {
         BuildGate.exit(var5);
         LowResGrid.exitBuildFocus();
         x.decrementAndGet();
         y = System.nanoTime();
      }
   }

   private RiverNetwork c(PlateKey var1) {
      final TerrainSampler var2 = this.d(var1);
      RiverEngine.BuildListener var3 = this.v;
      if (var3 == null) {
         return NetworkTracer.build(new PlateDomain(this.j, var1), var2, this.i, this.g, this.f);
      }

      final int[] var4 = new int[]{0};
      final int[] var5 = new int[]{0};
      long var6 = FormEval.WALK_BLOCKS.sum();
      long var8 = FormEval.WALK_COLUMNS.sum();
      long var10 = FormEval.MARK_BLOCKS.sum();
      long var12 = FormEval.MARK_COLUMNS.sum();
      long var14 = CachingTerrainSampler.HITS_ALL.sum();
      long var16 = CachingTerrainSampler.MISSES_ALL.sum();
      final ReadTrace var18 = ReadTrace.ON ? new ReadTrace() : null;
      TerrainSampler var19 = new TerrainSampler() {
         @Override
         public boolean batchCapable() {
            return var2.batchCapable();
         }

         @Override
         public void groundHeightsBatch(int[] var1, int[] var2x, double[] var3x, int var4x) {
            HeightCensus.recordBulk(var4x);
            var2.groundHeightsBatch(var1, var2x, var3x, var4x);
            if (var18 != null) {
               for (int var5x = 0; var5x < var4x; var5x++) {
                  var18.add(var1[var5x], var2x[var5x], 'B', var3x[var5x]);
               }
            }

            for (int var6x = 0; var6x < var4x; var6x++) {
               if (var3x[var6x] == -1000.0) {
                  var5[0]++;
               } else {
                  var4[0]++;
               }
            }
         }

         @Override
         public void heightsBatch(int[] var1, int[] var2x, double[] var3x, int var4x) {
            HeightCensus.recordBulk(var4x);
            var2.heightsBatch(var1, var2x, var3x, var4x);
            if (var18 != null) {
               for (int var5x = 0; var5x < var4x; var5x++) {
                  var18.add(var1[var5x], var2x[var5x], 'b', var3x[var5x]);
               }
            }

            for (int var6x = 0; var6x < var4x; var6x++) {
               if (var3x[var6x] == -1000.0) {
                  var5[0]++;
               } else {
                  var4[0]++;
               }
            }
         }

         @Override
         public double height(int var1, int var2x) {
            HeightCensus.record(false);
            long var3x = SampleTimer.start();
            double var5x = var2.height(var1, var2x);
            SampleTimer.height(var3x);
            if (var18 != null) {
               var18.add(var1, var2x, 'h', var5x);
            }

            if (var5x == -1000.0) {
               var5[0]++;
            } else {
               var4[0]++;
            }

            return var5x;
         }

         @Override
         public double surfaceHeight(int var1, int var2x) {
            var4[0]++;
            long var3x = SampleTimer.start();
            double var5x = var2.surfaceHeight(var1, var2x);
            SampleTimer.height(var3x);
            if (var18 != null) {
               var18.add(var1, var2x, 's', var5x);
            }

            return var5x;
         }

         @Override
         public double groundHeightReal(int var1, int var2x) {
            double var3x = var2.groundHeightReal(var1, var2x);
            if (var18 != null) {
               var18.add(var1, var2x, 'r', var3x);
            }

            return var3x;
         }

         @Override
         public double surfaceHeightReal(int var1, int var2x) {
            double var3x = var2.surfaceHeightReal(var1, var2x);
            if (var18 != null) {
               var18.add(var1, var2x, 'R', var3x);
            }

            return var3x;
         }

         @Override
         public double groundHeight(int var1, int var2x) {
            HeightCensus.record(false);
            long var3x = SampleTimer.start();
            double var5x = var2.groundHeight(var1, var2x);
            SampleTimer.height(var3x);
            if (var18 != null) {
               var18.add(var1, var2x, 'g', var5x);
            }

            if (var5x == -1000.0) {
               var5[0]++;
            } else {
               var4[0]++;
            }

            return var5x;
         }

         @Override
         public double groundHeightExact(int var1, int var2x) {
            HeightCensus.record(false);
            long var3x = SampleTimer.start();
            double var5x = var2.groundHeightExact(var1, var2x);
            SampleTimer.height(var3x);
            if (var18 != null) {
               var18.add(var1, var2x, 'x', var5x);
            }

            if (var5x == -1000.0) {
               var5[0]++;
            } else {
               var4[0]++;
            }

            return var5x;
         }

         @Override
         public boolean bakedCalibration() {
            return var2.bakedCalibration();
         }

         @Override
         public boolean hasEstimate() {
            return var2.hasEstimate();
         }

         @Override
         public boolean useHybridGridFill() {
            return var2.useHybridGridFill();
         }

         @Override
         public double estimate(int var1, int var2x) {
            HeightCensus.recordEstimate();
            return var2.estimate(var1, var2x);
         }

         @Override
         public boolean openAt(int var1, int var2x, int var3x) {
            return var2.openAt(var1, var2x, var3x);
         }

         @Override
         public int waterBiomeClass(int var1, int var2x) {
            long var3x = SampleTimer.start();
            int var5x = var2.waterBiomeClass(var1, var2x);
            SampleTimer.biome(var3x);
            return var5x;
         }

         @Override
         public int waterBiomeClassAt(int var1, int var2x, int var3x) {
            long var4x = SampleTimer.start();
            int var6x = var2.waterBiomeClassAt(var1, var2x, var3x);
            SampleTimer.biome(var4x);
            return var6x;
         }

         @Override
         public double riverSurface(int var1, int var2x) {
            return var2.riverSurface(var1, var2x);
         }

         @Override
         public boolean outletBiomeOk(int var1, int var2x, int var3x) {
            long var4x = SampleTimer.start();
            boolean var6x = var2.outletBiomeOk(var1, var2x, var3x);
            SampleTimer.biome(var4x);
            return var6x;
         }
      };
      long var20 = System.nanoTime();
      RiverNetwork var22 = NetworkTracer.build(new PlateDomain(this.j, var1), var19, this.i, this.g, this.f);
      double var23 = (System.nanoTime() - var20) / 1000000.0;
      SampleTimer.plateBuilt();
      if (SampleTimer.ON) {
         Constants.LOG.info("SAMPLETIME plate {} wall {} ms | {}", new Object[]{var1, String.format("%,.1f", var23), SampleTimer.report()});
      }

      long var25 = FormEval.WALK_BLOCKS.sum() - var6;
      long var27 = FormEval.WALK_COLUMNS.sum() - var8;
      long var29 = FormEval.MARK_BLOCKS.sum() - var10;
      long var31 = FormEval.MARK_COLUMNS.sum() - var12;
      if (var27 + var31 > 0L) {
         Constants.LOG
            .info(
               "[SR-WALK] plate {}: {} columns walked {} blocks (avg {}); two-phase {} columns, {} blocks (avg {})",
               new Object[]{var1, var27, var25, var27 == 0L ? 0L : var25 / var27, var31, var29, var31 == 0L ? 0L : var29 / var31}
            );
      }

      long var33 = CachingTerrainSampler.HITS_ALL.sum() - var14;
      long var35 = CachingTerrainSampler.MISSES_ALL.sum() - var16;
      Constants.LOG
         .info(
            "[SR-READS] plate {} calls={} ocean-skipped={} | memo hits={} misses={} ({}% served)",
            new Object[]{var1, var4[0], var5[0], var33, var35, var33 + var35 == 0L ? 0L : 100L * var33 / (var33 + var35)}
         );
      var3.onBuilt(var1, var4[0], var5[0], var23, var22.segmentCount());
      if (var18 != null) {
         var18.write(
            this.f,
            var1.cx(),
            var1.cz(),
            this.g.gridSpacing(),
            Math.max(this.g.gridSpacing(), this.g.terrainSampleSpacing()),
            this.g.terrainFit(),
            this.g.lakeSampleSpacing()
         );
      }

      return var22;
   }

   private static long a(int var0, int var1) {
      return (long)var0 << 32 ^ var1 & 4294967295L;
   }

   private static int a(double var0) {
      return (int)(Math.floor(var0 / 16.0) * 16.0);
   }

   public void setEstCostPersist(IntConsumer var1) {
      this.N = var1;
   }

   public int estCostVerdict() {
      return this.K;
   }

   public void setEstCostVerdict(int var1) {
      this.K = var1;
      if (var1 == 2) {
         this.O = true;
      }
   }

   private static TerrainSampler a(final TerrainSampler var0) {
      return new TerrainSampler() {
         @Override
         public boolean batchCapable() {
            return var0.batchCapable();
         }

         @Override
         public void heightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
            var0.heightsBatch(var1, var2, var3, var4);
         }

         @Override
         public void groundHeightsBatch(int[] var1, int[] var2, double[] var3, int var4) {
            var0.groundHeightsBatch(var1, var2, var3, var4);
         }

         @Override
         public int waterBiomeClass(int var1, int var2) {
            return var0.waterBiomeClass(var1, var2);
         }

         @Override
         public int waterBiomeClassAt(int var1, int var2, int var3) {
            return var0.waterBiomeClassAt(var1, var2, var3);
         }

         @Override
         public double riverSurface(int var1, int var2) {
            return var0.riverSurface(var1, var2);
         }

         @Override
         public boolean outletBiomeOk(int var1, int var2, int var3) {
            return var0.outletBiomeOk(var1, var2, var3);
         }

         @Override
         public double height(int var1, int var2) {
            return var0.groundHeightExact(var1, var2);
         }

         @Override
         public double groundHeight(int var1, int var2) {
            return var0.groundHeightExact(var1, var2);
         }

         @Override
         public double groundHeightExact(int var1, int var2) {
            return var0.groundHeightExact(var1, var2);
         }

         @Override
         public double surfaceHeight(int var1, int var2) {
            return var0.surfaceHeight(var1, var2);
         }

         @Override
         public double groundHeightReal(int var1, int var2) {
            return var0.groundHeightReal(var1, var2);
         }

         @Override
         public double surfaceHeightReal(int var1, int var2) {
            return var0.surfaceHeightReal(var1, var2);
         }

         @Override
         public boolean openAt(int var1, int var2, int var3) {
            return var0.openAt(var1, var2, var3);
         }
      };
   }

   private TerrainSampler d(PlateKey var1) {
      boolean var2 = this.h.bakedCalibration();
      if (!var2 && !this.h.hasEstimate()) {
         return this.h;
      }

      if (this.O) {
         return a(this.h);
      }

      boolean var3 = false;
      if (this.K == 0) {
         synchronized (this.L) {
            if (this.K == 0 && !this.M) {
               this.M = true;
               var3 = true;
            } else {
               while (this.K == 0) {
                  try {
                     this.L.wait(1000L);
                  } catch (InterruptedException var34) {
                     Thread.currentThread().interrupt();
                     break;
                  }
               }
            }
         }

         if (!var3 && this.O) {
            return a(this.h);
         }
      }

      PlateDomain var36 = new PlateDomain(this.j, var1);
      int var5 = Math.max(this.g.gridSpacing(), this.g.terrainSampleSpacing());
      int var6 = 4 * var5;

      while ((long)((var36.maxX - var36.minX) / var6 + 1) * ((var36.maxZ - var36.minZ) / var6 + 1) > 1024L) {
         var6 *= 2;
      }

      int var7 = Math.floorDiv(var36.minX, var6) * var6;
      int var8 = Math.floorDiv(var36.minZ, var6) * var6;
      ArrayList var9 = new ArrayList();

      for (int var10 = var7; var10 <= var36.maxX; var10 += var6) {
         for (int var11 = var8; var11 <= var36.maxZ; var11 += var6) {
            if (var36.contains(var10, var11) && this.h.height(var10, var11) != -1000.0) {
               var9.add(new int[]{var10, var11});
            }
         }
      }

      double[] var37 = new double[var9.size()];
      double[] var38 = new double[var9.size()];
      AtomicLong var12 = new AtomicLong();
      AtomicLong var13 = new AtomicLong();
      AtomicInteger var14 = new AtomicInteger();
      AtomicInteger var15 = new AtomicInteger();
      AtomicBoolean var16 = new AtomicBoolean();
      boolean var17 = var3;
      LowResGrid.parallelFor(var9.size(), var10x -> {
         int[] var11x = (int[])var9.get(var10x);
         long var12x = System.nanoTime();
         var37[var10x] = this.h.groundHeightExact(var11x[0], var11x[1]);
         long var14x = System.nanoTime();
         var12.addAndGet(var14x - var12x);
         var14.incrementAndGet();
         if (var17 && var16.get()) {
            var38[var10x] = var37[var10x];
         } else {
            var38[var10x] = this.h.estimate(var11x[0], var11x[1]);
            long var16x = System.nanoTime() - var14x;
            var13.addAndGet(var16x);
            int var18x = var15.incrementAndGet();
            if (var17 && var18x >= 8 && var13.get() / var18x > var12.get() / Math.max(1, var14.get())) {
               var16.set(true);
            }
         }
      });
      HashMap var18 = new HashMap(var9.size() * 2);
      HashMap<Integer, int[]> var19 = new HashMap<>();
      int var20 = 0;
      int var21 = 0;

      for (int var22 = 0; var22 < var9.size(); var22++) {
         int[] var23 = (int[])var9.get(var22);
         double var24 = var37[var22];
         if (var24 != -1000.0) {
            var18.put(a(var23[0], var23[1]), var24);
            double var26 = this.h.groundHeight(var23[0], var23[1]);
            double var28 = var26 - var24;
            if (!(var28 >= 6.0)) {
               int[] var30 = var19.computeIfAbsent(a(var38[var22]), var0 -> new int[2]);
               var30[0]++;
               var20++;
               if (var28 > 2.0 || var28 < -6.0) {
                  var30[1]++;
                  var21++;
               }
            }
         }
      }

      boolean var39 = Boolean.getBoolean("streamsreflowing.caldiag");
      final TerrainSampler var40 = this.h;
      double var41 = var14.get() > 0 ? (double)var12.get() / var14.get() : 0.0;
      double var42 = var15.get() > 0 ? (double)var13.get() / var15.get() : 0.0;
      boolean var43 = false;
      if (var3) {
         var43 = (var16.get() || var15.get() >= 8) && var41 > 0.0 && (var16.get() || var42 / var41 > 0.9375);
         int var29 = var43 ? 2 : 1;
         IntConsumer var45 = this.N;
         synchronized (this.L) {
            this.K = var29;
            this.M = false;
            if (var29 == 2) {
               this.O = true;
            }

            this.L.notifyAll();
         }

         if (var45 != null) {
            var45.accept(var29);
         }

         Constants.LOG
            .info(
               "Streams Reflowing: fast terrain cost verdict {} for this world ({}% of a real read){}. Remembered for future loads.",
               new Object[]{var29 == 2 ? "OFF" : "ON", var41 > 0.0 ? Math.round(100.0 * var42 / var41) : -1L, var16.get() ? " (early-abort)" : ""}
            );
      }

      if (var43 || var20 >= 30 && var21 > var20 * 0.25) {
         if (var39) {
            Constants.LOG
               .info(
                  "[SR-BAKEDCAL] plate {},{} failed table verification ({}/{} outside [-{},+{}]) -> real sampling for this plate",
                  new Object[]{var1.cx(), var1.cz(), var21, var20, 6.0, 2.0}
               );
         }

         return a(var40);
      } else {
         if (!var2) {
            return this.h;
         }

         HashSet<Integer> var44 = new HashSet<>();

         for (Entry var48 : var19.entrySet()) {
            int[] var32 = (int[])var48.getValue();
            if (var32[0] >= 6 && var32[1] > var32[0] * 0.1) {
               var44.add((Integer)var48.getKey());
            }
         }

         if (var39 && !var44.isEmpty()) {
            Constants.LOG
               .info(
                  "[SR-BAKEDCAL] plate {},{}: bands {} failed verification ({}/{} points outside [-{},+{}]) -> real grades for those bands only; {} anchors kept",
                  new Object[]{var1.cx(), var1.cz(), new TreeSet(var44), var21, var20, 6.0, 2.0, var18.size()}
               );
         }

         if (var18.isEmpty() && var44.isEmpty()) {
            return this.h;
         }

         final HashMap var47 = var18;
         final HashSet var49 = var44;
         return new TerrainSampler() {
            @Override
            public int waterBiomeClass(int var1, int var2x) {
               return var40.waterBiomeClass(var1, var2x);
            }

            @Override
            public int waterBiomeClassAt(int var1, int var2x, int var3x) {
               return var40.waterBiomeClassAt(var1, var2x, var3x);
            }

            @Override
            public double riverSurface(int var1, int var2x) {
               return var40.riverSurface(var1, var2x);
            }

            @Override
            public boolean outletBiomeOk(int var1, int var2x, int var3x) {
               return var40.outletBiomeOk(var1, var2x, var3x);
            }

            @Override
            public double height(int var1, int var2x) {
               Double var3x = (Double)var47.get(RiverEngine.a(var1, var2x));
               return var3x != null ? var3x : var40.height(var1, var2x);
            }

            @Override
            public double groundHeight(int var1, int var2x) {
               Double var3x = (Double)var47.get(RiverEngine.a(var1, var2x));
               if (var3x != null) {
                  return var3x;
               } else {
                  return !var49.isEmpty() && var49.contains(RiverEngine.a(var40.estimate(var1, var2x)))
                     ? var40.groundHeightExact(var1, var2x)
                     : var40.groundHeight(var1, var2x);
               }
            }

            @Override
            public double groundHeightExact(int var1, int var2x) {
               Double var3x = (Double)var47.get(RiverEngine.a(var1, var2x));
               return var3x != null ? var3x : var40.groundHeightExact(var1, var2x);
            }

            @Override
            public double surfaceHeight(int var1, int var2x) {
               return var40.surfaceHeight(var1, var2x);
            }

            @Override
            public double groundHeightReal(int var1, int var2x) {
               return var40.groundHeightReal(var1, var2x);
            }

            @Override
            public double surfaceHeightReal(int var1, int var2x) {
               return var40.surfaceHeightReal(var1, var2x);
            }

            @Override
            public boolean hasEstimate() {
               return var40.hasEstimate();
            }

            @Override
            public double estimate(int var1, int var2x) {
               return var40.estimate(var1, var2x);
            }

            @Override
            public boolean useHybridGridFill() {
               return var40.useHybridGridFill();
            }

            @Override
            public boolean bakedCalibration() {
               return var40.bakedCalibration();
            }

            @Override
            public boolean openAt(int var1, int var2x, int var3x) {
               return var40.openAt(var1, var2x, var3x);
            }
         };
      }
   }

   public RiverNetwork networkAt(int var1, int var2) {
      return this.networkFor(this.plateAt(var1, var2));
   }

   public void setBiomeBarAtPlanning(boolean var1) {
      this.ab = var1;
   }

   public boolean biomeBarAtPlanning() {
      return this.ab;
   }

   public void setStreamsEnabled(boolean var1) {
      this.aa = var1;
   }

   public ColumnPlan plan(int var1, int var2) {
      if (!this.aa) {
         return ColumnPlan.NONE;
      }

      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      int var5 = (int)(var1 * 31L + var2 & 2147483647L & 8191L);
      long[] var6 = this.Q.get();
      ColumnPlan[] var7 = this.R.get();
      if (var6[var5] == var3) {
         ColumnPlan var8 = var7[var5];
         if (var8 != null) {
            return var8;
         }
      }

      ColumnPlan var9 = this.b(var1, var2);
      var6[var5] = var3;
      var7[var5] = var9;
      this.U.get()[var5] = this.W.get()[0];
      this.V.get()[var5] = this.X.get()[0];
      return var9;
   }

   public ColumnPlan planWithin(PlateKey var1, int var2, int var3) {
      if (this.aa && this.isPlateReady(var1)) {
         double var4 = var2 + 0.5;
         double var6 = var3 + 0.5;
         RiverSample var8 = this.networkFor(var1).sampleTiered(var4, var6, this.o);
         return var8 == null ? ColumnPlan.NONE : this.a(var4, var6, var8, false, new PlateKey[]{var1});
      } else {
         return ColumnPlan.NONE;
      }
   }

   public String planDiag(int var1, int var2) {
      double var3 = var1 + 0.5;
      double var5 = var2 + 0.5;
      RiverSample var7 = this.b(var3, var5, this.o);
      if (var7 == null) {
         return String.format(
            Locale.ROOT, "NOSAMPLE radius=%.1f plates=%d segs=%d", this.o, this.j.platesWithin(var3, var5, this.o).length, this.a(var3, var5, this.o)
         );
      }

      double var8 = var7.width();
      double var10 = Math.max(5.0, var8 * this.g.bankWidth());
      return String.format(
         Locale.ROOT,
         "SAMPLED d=%.2f w=%.2f bank=%.2f limit=%.2f over=%+.2f grade=%.1f term=%s src=%s",
         var7.distance(),
         var8,
         var10,
         var8 + var10,
         var7.distance() - (var8 + var10),
         var7.gradeY(),
         var7.terminal(),
         var7.source()
      );
   }

   private int a(double var1, double var3, double var5) {
      int var7 = 0;

      for (PlateKey var11 : this.j.platesWithin(var1, var3, var5)) {
         var7 += this.networkFor(var11).segmentCount();
      }

      return var7;
   }

   private ColumnPlan b(int var1, int var2) {
      double var3 = var1 + 0.5;
      double var5 = var2 + 0.5;
      double var7 = this.o;
      RiverSample var9 = this.c(var3, var5, var7);
      return var9 == null ? ColumnPlan.NONE : this.a(var3, var5, var9, true);
   }

   private ColumnPlan a(double var1, double var3, RiverSample var5, boolean var6) {
      return this.a(var1, var3, var5, var6, null);
   }

   private ColumnPlan a(double var1, double var3, RiverSample var5, boolean var6, PlateKey[] var7) {
      double var8 = var5.width();
      double var10 = var5.distance();
      double var12 = Math.max(5.0, var8 * this.g.bankWidth());
      if (var10 > var8 + var12) {
         return ColumnPlan.NONE;
      }

      double var14 = var5.gradeY();
      int var16 = Math.max(this.g.seaLevel() - 1, (int)Math.floor(var14) - this.a(var8, var1, var3, var5.dirX(), var5.dirZ(), var14, var7));
      int var17 = Math.max(1, (int)Math.round(2.0 * var8 * this.g.streamDepth()));
      int var18 = var16 - var17;
      boolean var19 = var10 <= var8;
      double var20 = Math.pow(Math.max(0.0, Math.min(1.0, (var10 - var8) / var12)), this.g.aRiver());
      double var22 = var5.dirX();
      double var24 = var5.dirZ();
      double var26 = 0.0;
      double var28 = 0.0;
      if (var6) {
         boolean var30 = this.g.hydraulics() != null && this.g.hydraulics().shapesBed() || BankStyle.anyStyleUsesBend();
         double[] var31 = this.a(var1, var3, var10, var8, var5.dirX(), var5.dirZ(), var19, var30, this.o, this.s.get());
         var22 = var31[0];
         var24 = var31[1];
         if (var30) {
            var26 = var31[2];
            var28 = var31[3];
         }
      }

      return new ColumnPlan(
         var19,
         true,
         var16,
         var18,
         var20,
         var10,
         var8,
         var14,
         var5.slope(),
         var5.dirX(),
         var5.dirZ(),
         var5.terminal(),
         var5.source(),
         var5.upstreamSlope(),
         var22,
         var24,
         var5.distToOutlet(),
         var26,
         var28,
         var5.distToJunction(),
         var5.flatRun(),
         FlowSpeed.at(var5.speed(), var16 - var18)
      );
   }

   public ColumnPlan altPlan(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      int var5 = (int)(var1 * 31L + var2 & 2147483647L & 8191L);
      long[] var6 = this.Y.get();
      ColumnPlan[] var7 = this.Z.get();
      if (var6[var5] == var3) {
         ColumnPlan var8 = var7[var5];
         if (var8 != null) {
            return var8 == ColumnPlan.NONE ? null : var8;
         }
      }

      ColumnPlan var9 = this.c(var1, var2);
      var6[var5] = var3;
      var7[var5] = var9 == null ? ColumnPlan.NONE : var9;
      return var9;
   }

   private ColumnPlan c(int var1, int var2) {
      double var3 = var1 + 0.5;
      double var5 = var2 + 0.5;
      long var7 = (long)var1 << 32 ^ var2 & 4294967295L;
      int var9 = (int)(var1 * 31L + var2 & 2147483647L & 8191L);
      RiverNetwork var10 = null;
      int var11 = -1;
      if (this.Q.get()[var9] == var7 && this.R.get()[var9] != null) {
         if (!this.R.get()[var9].inCorridor()) {
            return null;
         }

         var10 = this.U.get()[var9];
         var11 = this.V.get()[var9];
      }

      if (var10 == null) {
         RiverSample var12 = this.c(var3, var5, this.o);
         if (var12 == null || !this.a(var12)) {
            return null;
         }

         var10 = this.W.get()[0];
         var11 = var12.segId();
      }

      RiverSample var19 = null;

      for (PlateKey var16 : this.j.platesWithin(var3, var5, this.o)) {
         RiverNetwork var17 = this.networkFor(var16);
         RiverSample var18 = var17.sampleCoveringForeign(var3, var5, this.o, var17 == var10 ? var11 : -1);
         if (var18 != null && (var19 == null || var18.distance() < var19.distance())) {
            var19 = var18;
         }
      }

      return var19 == null ? null : this.a(var3, var5, var19, false);
   }

   private int a(double var1, double var3, double var5, double var7, double var9, double var11, PlateKey[] var13) {
      int var14 = insetFor(var1, this.g);
      double var15 = Math.sqrt(var7 * var7 + var9 * var9);
      if (var14 > 0 && !(var15 < 1.0E-6)) {
         if (this.a((int)Math.floor(var3), (int)Math.floor(var5), var13) != null) {
            return var14;
         }

         double var17 = -var7 / var15;
         double var19 = -var9 / var15;

         for (int var21 = 1; var21 <= 6; var21++) {
            Lake var22 = this.a((int)Math.floor(var3 + var17 * var21), (int)Math.floor(var5 + var19 * var21), var13);
            if (var22 != null) {
               int var23 = (int)Math.round(var14 * (var21 / 7.0));
               int var24 = (int)Math.floor(var11) - var22.level();
               return Math.max(var23, Math.min(var14, var24));
            }
         }

         return var14;
      } else {
         return var14;
      }
   }

   public ColumnPlan planLean(int var1, int var2) {
      if (!this.aa) {
         return ColumnPlan.NONE;
      }

      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      int var5 = (int)(var1 * 31L + var2 & 2147483647L & 8191L);
      long[] var6 = this.S.get();
      ColumnPlan[] var7 = this.T.get();
      if (var6[var5] == var3) {
         ColumnPlan var8 = var7[var5];
         if (var8 != null) {
            return var8;
         }
      }

      LEAN_CALLS.incrementAndGet();
      RiverSample var10 = this.b(var1 + 0.5, var2 + 0.5, this.o);
      ColumnPlan var9 = var10 == null ? ColumnPlan.NONE : this.a(var1 + 0.5, var2 + 0.5, var10, false);
      var6[var5] = var3;
      var7[var5] = var9;
      return var9;
   }

   private RiverSample b(double var1, double var3, double var5) {
      RiverSample var7 = null;

      for (PlateKey var11 : this.j.platesWithin(var1, var3, var5)) {
         RiverSample var12 = this.networkFor(var11).sample(var1, var3, var5);
         if (var12 != null && (var7 == null || var12.distance() < var7.distance())) {
            var7 = var12;
         }
      }

      return var7;
   }

   private RiverSample c(double var1, double var3, double var5) {
      RiverSample var7 = null;
      RiverNetwork var8 = null;
      boolean var9 = false;
      boolean var10 = false;

      for (PlateKey var14 : this.j.platesWithin(var1, var3, var5)) {
         RiverNetwork var15 = this.networkFor(var14);
         RiverSample var16 = var15.sampleTiered(var1, var3, var5);
         if (var16 != null) {
            boolean var17 = var16.distance() <= var16.width();
            boolean var18 = this.a(var16);
            if (var7 == null || var17 && !var9 || var17 == var9 && (var18 && !var10 || var18 == var10 && var16.distance() < var7.distance())) {
               var7 = var16;
               var8 = var15;
               var9 = var17;
               var10 = var18;
            }
         }
      }

      this.W.get()[0] = var8;
      this.X.get()[0] = var7 == null ? -1 : var7.segId();
      return var7;
   }

   public boolean lakeNearChunk(int var1, int var2, int var3) {
      double var4 = var1 + 8.0;
      double var6 = var2 + 8.0;

      for (PlateKey var11 : this.j.platesWithin(var4, var6, this.g.gridSpacing() + var3)) {
         for (Lake var13 : this.baseFor(var11).lakes()) {
            int var14 = var13.cellMinX();
            int var15 = var14 + var13.cellsWide() * var13.spacing();
            if (var15 >= var1 - var3 && var14 <= var1 + 16 + var3) {
               int var16 = var13.cellMinZ();
               int var17 = var16 + var13.cellsHigh() * var13.spacing();
               if (var17 >= var2 - var3 && var16 <= var2 + 16 + var3) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean a(RiverSample var1) {
      double var2 = var1.width();
      return var1.distance() <= var2 + Math.max(5.0, var2 * this.g.bankWidth());
   }

   private double[] a(double var1, double var3, double var5, double var7, double var9, double var11, boolean var13, boolean var14, double var15, double[] var17) {
      double var18 = Math.sqrt(var9 * var9 + var11 * var11);
      var17[2] = 0.0;
      var17[3] = 0.0;
      boolean var20 = var13;
      if ((var20 || var14) && !(var18 < 1.0E-6) && !(var7 < 0.5)) {
         double var21 = var9 / var18;
         double var23 = var11 / var18;
         double var25 = -var23;
         double var27 = var21;
         double var29 = Math.max(4.0, var7);
         RiverSample var31 = this.b(var1 + var21 * var29, var3 + var23 * var29, var15);
         double var32 = 0.0;
         if (var31 != null) {
            double var34 = Math.sqrt(var31.dirX() * var31.dirX() + var31.dirZ() * var31.dirZ());
            if (var34 > 1.0E-6) {
               var32 = var21 * (var31.dirZ() / var34) - var23 * (var31.dirX() / var34);
            }
         }

         RiverSample var47 = this.b(var1 + var25 * 1.5, var3 + var27 * 1.5, var15);
         double var35 = var47 != null && var47.distance() < var5 ? -var5 : var5;
         double var37 = Math.max(-1.0, Math.min(1.0, var35 / var7));
         var17[2] = var32;
         var17[3] = var37;
         if (!var20) {
            var17[0] = var21;
            var17[1] = var23;
            return var17;
         } else {
            double var39 = 1.5 * var32 * var37;
            double var41 = var31 != null ? (var31.width() - var7) / var29 : 0.0;
            var39 += Math.max(-0.5, Math.min(0.5, 2.0 * var41 * var37));
            double var43 = Math.cos(var39);
            double var45 = Math.sin(var39);
            var17[0] = var21 * var43 - var23 * var45;
            var17[1] = var21 * var45 + var23 * var43;
            return var17;
         }
      } else {
         var17[0] = var9;
         var17[1] = var11;
         return var17;
      }
   }

   public boolean inAnyChannel(int var1, int var2) {
      double var3 = var1 + 0.5;
      double var5 = var2 + 0.5;
      double var7 = this.n;

      for (PlateKey var12 : this.j.platesWithin(var3, var5, var7)) {
         if (this.networkFor(var12).inAnyChannel(var3, var5, var7)) {
            return true;
         }
      }

      return false;
   }

   public boolean anyStreamsNear(double var1, double var3, double var5) {
      for (PlateKey var10 : this.j.platesWithin(var1, var3, var5 + this.o)) {
         if (this.networkFor(var10).segmentCount() > 0) {
            return true;
         }
      }

      return false;
   }

   public PlateKey[] platesNear(double var1, double var3, double var5) {
      return this.j.platesWithin(var1, var3, var5 + this.o);
   }

   public boolean inCorridor(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      long[] var5 = this.p.get();
      if (var5[0] == var3) {
         return var5[1] != 0L;
      }

      boolean var6 = false;
      RiverSample var7 = this.b(var1 + 0.5, var2 + 0.5, this.o);
      if (var7 != null) {
         double var8 = Math.max(5.0, var7.width() * this.g.bankWidth());
         var6 = var7.distance() <= var7.width() + var8;
      }

      var5[0] = var3;
      var5[1] = var6 ? 1L : 0L;
      return var6;
   }

   public boolean corridorNearChunk(int var1, int var2, int var3) {
      RiverSample var4 = this.b(var1 + 8.0, var2 + 8.0, this.o + var3);
      if (var4 == null) {
         return false;
      }

      double var5 = var4.width() + Math.max(5.0, var4.width() * this.g.bankWidth());
      return var4.distance() <= 11.32 + var3 + var5;
   }

   public boolean corridorTouchesChunk(int var1, int var2) {
      for (PlateKey var6 : this.j.platesWithin(var1 + 8.0, var2 + 8.0, this.o + 12.0)) {
         if (this.networkFor(var6).corridorTouchesBox(var1, var2, var1 + 15, var2 + 15)) {
            return true;
         }
      }

      return false;
   }

   public boolean corridorTouchesBox(int var1, int var2, int var3, int var4) {
      double var5 = (var1 + var3) * 0.5;
      double var7 = (var2 + var4) * 0.5;
      double var9 = Math.max(var3 - var1, var4 - var2) * 0.5;

      for (PlateKey var14 : this.j.platesWithin(var5, var7, this.o + var9 + 4.0)) {
         if (this.networkFor(var14).corridorTouchesBox(var1, var2, var3, var4)) {
            return true;
         }
      }

      return false;
   }

   public List<Sink> sinksNear(double var1, double var3, double var5) {
      ArrayList var7 = new ArrayList();
      double var8 = var5 * var5;

      for (PlateKey var13 : this.j.platesWithin(var1, var3, var5 + 4.0)) {
         if (this.isPlateReady(var13)) {
            for (Sink var15 : this.networkFor(var13).sinks()) {
               double var16 = var15.x() - var1;
               double var18 = var15.z() - var3;
               if (var16 * var16 + var18 * var18 <= var8) {
                  var7.add(var15);
               }
            }
         }
      }

      return var7;
   }

   public boolean sinkWithin(double var1, double var3, double var5) {
      double var7 = var5 * var5;

      for (PlateKey var12 : this.j.platesWithin(var1, var3, var5 + 4.0)) {
         if (this.isPlateReady(var12)) {
            for (Sink var14 : this.networkFor(var12).sinks()) {
               double var15 = var14.x() - var1;
               double var17 = var14.z() - var3;
               if (var15 * var15 + var17 * var17 <= var7) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public long vetoProbeAt(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      int var5 = (int)(var1 * 31L + var2 & 2147483647L & 4095L);
      long[] var6 = this.ae.get();
      long[] var7 = this.af.get();
      if (var6[var5] == var3) {
         return var7[var5];
      }

      long var8 = (long)(var1 >> 4) << 32 ^ var2 >> 4 & 4294967295L;
      long[] var10 = this.ag.get();
      if (var10[0] != var8) {
         var10[1] = this.corridorTouchesChunk(var1 >> 4 << 4, var2 >> 4 << 4) ? 1L : 0L;
         var10[2] = this.lakeNearChunk(var1 >> 4 << 4, var2 >> 4 << 4, 8) ? 1L : 0L;
         var10[0] = var8;
      }

      int var11 = Integer.MIN_VALUE;
      if (var10[1] != 0L) {
         RiverSample var12 = this.b(var1 + 0.5, var2 + 0.5, this.o);
         if (var12 != null && var12.distance() <= var12.width()) {
            var11 = Math.max(this.g.seaLevel() - 1, (int)Math.floor(var12.gradeY()) - insetFor(var12.width(), this.g));
         }
      }

      int var15 = var10[2] != 0L ? this.lakeSurfaceAt(var1, var2) : Integer.MIN_VALUE;
      long var13 = (long)var15 << 32 | var11 & 4294967295L;
      var6[var5] = var3;
      var7[var5] = var13;
      return var13;
   }

   public int lakeSurfaceAt(int var1, int var2) {
      Lake var3 = this.lakeAt(var1, var2);
      return var3 == null ? Integer.MIN_VALUE : var3.level();
   }

   public Lake lakeAt(int var1, int var2) {
      double var3 = var1 + 0.5;
      double var5 = var2 + 0.5;
      Lake var7 = null;

      for (PlateKey var11 : this.j.platesWithin(var3, var5, this.g.gridSpacing())) {
         Lake var12 = this.baseFor(var11).lakeAt(var1, var2);
         if (var12 != null && (var7 == null || var12.level() < var7.level())) {
            var7 = var12;
         }
      }

      return var7;
   }

   private Lake a(int var1, int var2, PlateKey[] var3) {
      return var3 == null ? this.lakeAt(var1, var2) : this.lakeAtAmong(var3, var1, var2);
   }

   public PlateKey[] platesOwningChunk(int var1, int var2) {
      int var3 = var1 << 4;
      int var4 = var2 << 4;
      LinkedHashSet<PlateKey> var5 = new LinkedHashSet<>(4);
      var5.add(this.plateAt(var3 + 8, var4 + 8));
      var5.add(this.plateAt(var3, var4));
      var5.add(this.plateAt(var3 + 15, var4));
      var5.add(this.plateAt(var3, var4 + 15));
      var5.add(this.plateAt(var3 + 15, var4 + 15));
      return var5.toArray(new PlateKey[0]);
   }

   public PlateKey[] platesTouchingBox(int var1, int var2, int var3, int var4) {
      double var5 = this.o;
      double var7 = var1 - var5;
      double var9 = var2 - var5;
      double var11 = var3 + var5;
      double var13 = var4 + var5;
      double var15 = Math.max(16.0, this.j.scale() * 0.5);
      LinkedHashSet<PlateKey> var17 = new LinkedHashSet<>(8);
      double var18 = var7;

      while (true) {
         double var20 = Math.min(var18, var11);
         double var22 = var9;

         while (true) {
            double var24 = Math.min(var22, var13);
            var17.add(this.j.plateAt(var20 + 0.5, var24 + 0.5));
            if (var24 >= var13) {
               if (var20 >= var11) {
                  return var17.toArray(new PlateKey[0]);
               }

               var18 += var15;
               break;
            }

            var22 += var15;
         }
      }
   }

   public boolean platesReady(PlateKey[] var1) {
      for (PlateKey var5 : var1) {
         if (!this.isPlateReady(var5)) {
            return false;
         }
      }

      return true;
   }

   public CompletableFuture<Void> whenPlatesReady(PlateKey[] var1) {
      ArrayList<CompletableFuture<?>> var2 = new ArrayList<>(var1.length);
      BiConsumer var3 = ah;

      for (PlateKey var7 : var1) {
         if (!this.isPlateReady(var7)) {
            var2.add((this.m ? this.l : this.k).whenReady(var7));
            if (var3 != null) {
               noteDemand(var7, this.j.seedX(var7.cx(), var7.cz()), this.j.seedZ(var7.cx(), var7.cz()), 0.0);
               var3.accept(this, var7);
            }
         }
      }

      return var2.isEmpty() ? CompletableFuture.completedFuture(null) : CompletableFuture.allOf(var2.toArray(new CompletableFuture<?>[0]));
   }

   private RiverSample a(PlateKey[] var1, double var2, double var4, double var6) {
      RiverSample var8 = null;

      for (PlateKey var12 : var1) {
         if (this.isPlateReady(var12)) {
            RiverSample var13 = this.networkFor(var12).sample(var2, var4, var6);
            if (var13 != null && (var8 == null || var13.distance() < var8.distance())) {
               var8 = var13;
            }
         }
      }

      return var8;
   }

   private RiverSample b(PlateKey[] var1, double var2, double var4, double var6) {
      RiverSample var8 = null;
      boolean var9 = false;
      boolean var10 = false;

      for (PlateKey var14 : var1) {
         if (this.isPlateReady(var14)) {
            RiverSample var15 = this.networkFor(var14).sampleTiered(var2, var4, var6);
            if (var15 != null) {
               boolean var16 = var15.distance() <= var15.width();
               boolean var17 = this.a(var15);
               if (var8 == null || var16 && !var9 || var16 == var9 && (var17 && !var10 || var17 == var10 && var15.distance() < var8.distance())) {
                  var8 = var15;
                  var9 = var16;
                  var10 = var17;
               }
            }
         }
      }

      return var8;
   }

   public boolean inCorridorAmong(PlateKey[] var1, int var2, int var3) {
      RiverSample var4 = this.a(var1, var2 + 0.5, var3 + 0.5, this.o);
      if (var4 == null) {
         return false;
      }

      double var5 = Math.max(5.0, var4.width() * this.g.bankWidth());
      return var4.distance() <= var4.width() + var5;
   }

   public boolean corridorNearChunkAmong(PlateKey[] var1, int var2, int var3, int var4) {
      RiverSample var5 = this.a(var1, var2 + 8.0, var3 + 8.0, this.o + var4);
      if (var5 == null) {
         return false;
      }

      double var6 = var5.width() + Math.max(5.0, var5.width() * this.g.bankWidth());
      return var5.distance() <= 11.32 + var4 + var6;
   }

   public int bedYAmong(PlateKey[] var1, int var2, int var3) {
      double var4 = var2 + 0.5;
      double var6 = var3 + 0.5;
      RiverSample var8 = this.b(var1, var4, var6, this.o);
      if (var8 == null) {
         return Integer.MIN_VALUE;
      }

      ColumnPlan var9 = this.a(var4, var6, var8, false, var1);
      return var9 == ColumnPlan.NONE ? Integer.MIN_VALUE : var9.bedY();
   }

   public Lake lakeAtAmong(PlateKey[] var1, int var2, int var3) {
      Lake var4 = null;

      for (PlateKey var8 : var1) {
         if (this.isPlateReady(var8)) {
            Lake var9 = this.baseFor(var8).lakeAt(var2, var3);
            if (var9 != null && (var4 == null || var9.level() < var4.level())) {
               var4 = var9;
            }
         }
      }

      return var4;
   }

   public Lake lakeWetting(int var1, int var2, int var3) {
      double var4 = var1 + 0.5;
      double var6 = var2 + 0.5;
      Lake var8 = null;

      for (PlateKey var12 : this.j.platesWithin(var4, var6, this.g.gridSpacing() * (1.0 + var3))) {
         Lake var13 = this.baseFor(var12).lakeWettingAt(var1, var2, var3);
         if (var13 != null && (var8 == null || var13.level() < var8.level())) {
            var8 = var13;
         }
      }

      return var8;
   }

   public Lake lakeNear(int var1, int var2, int var3) {
      double var4 = var1 + 0.5;
      double var6 = var2 + 0.5;
      Lake var8 = null;

      for (PlateKey var12 : this.j.platesWithin(var4, var6, this.g.gridSpacing() * (1.0 + var3))) {
         Lake var13 = this.baseFor(var12).lakeNear(var1, var2, var3);
         if (var13 != null && (var8 == null || var13.level() < var8.level())) {
            var8 = var13;
         }
      }

      return var8;
   }

   public boolean isPlateCached(PlateKey var1) {
      return (this.m ? this.l : this.k).contains(var1);
   }

   public void markPlateWanted(PlateKey var1) {
      this.k.markWanted(var1);
      if (this.m) {
         this.l.markWanted(var1);
      }
   }

   public boolean isPlateReady(PlateKey var1) {
      return (this.m ? this.l : this.k).isReady(var1);
   }

   public int plateBuildFailures(PlateKey var1) {
      return (this.m ? this.l : this.k).failures(var1);
   }

   public boolean platesReadyNear(double var1, double var3, double var5) {
      for (PlateKey var10 : this.platesNear(var1, var3, var5)) {
         if (!this.isPlateReady(var10)) {
            return false;
         }
      }

      return true;
   }

   public static void setBuildRequester(BiConsumer<RiverEngine, PlateKey> var0) {
      ah = var0;
   }

   public CompletableFuture<Void> whenPlatesReadyNear(double var1, double var3, double var5) {
      ArrayList<CompletableFuture<?>> var7 = new ArrayList<>(2);
      BiConsumer var8 = ah;

      for (PlateKey var12 : this.platesNear(var1, var3, var5)) {
         if (!this.isPlateReady(var12)) {
            var7.add((this.m ? this.l : this.k).whenReady(var12));
            if (var8 != null) {
               noteDemand(var12, var1, var3, var5 + this.o);
               var8.accept(this, var12);
            }
         }
      }

      return var7.isEmpty() ? CompletableFuture.completedFuture(null) : CompletableFuture.allOf(var7.toArray(new CompletableFuture<?>[0]));
   }

   public static void noteDemand(PlateKey var0, double var1, double var3, double var5) {
      if (ai && aj.add((long)var0.cx() << 32 ^ var0.cz() & 4294967295L)) {
         String var7 = DEMAND_REASON.get();
         Constants.LOG
            .info(
               "[SR-DEMAND] plate ({},{}) first asked by {} at ({},{}) consult radius {} on {}",
               new Object[]{var0.cx(), var0.cz(), var7 == null ? "unlabelled" : var7, (int)var1, (int)var3, (int)var5, Thread.currentThread().getName()}
            );
      }
   }

   public int cachedPlateCount() {
      return this.k.size();
   }

   @FunctionalInterface
   public interface BuildListener {
      void onBuilt(PlateKey var1, int var2, int var3, double var4, int var6);
   }
}
