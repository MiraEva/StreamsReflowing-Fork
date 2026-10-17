package dev.streamsreflowing.flow;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class StreamFlowGrid {
   public static final int SIZE = 256;
   public static final int SPEED_OFF = 256;
   public static final int PACKED = 512;
   public static final int BAND_LO_OFF = 512;
   public static final int BAND_HI_OFF = 1024;
   public static final int LAYER = 1536;
   public static final int ANY_Y = Integer.MIN_VALUE;
   public static final double RENDER_MIN_SPEED = 0.25;
   private static final CopyOnWriteArrayList<StreamFlowGrid.LayerProducer> LAYER_PRODUCERS = new CopyOnWriteArrayList<>();
   public static final byte NONE = -1;
   private static final double TWO_PI = Math.PI * 2;
   public static final double MIN_FLOW = 0.5;
   private static final int SMOOTH_R = 2;
   private static final int FEATHER = 4;
   private static final int WIDE_MIN = 10;
   private static final int Q_DILATE = 1;
   private static final double BANK_FOLLOW = 1.6;
   private static final int BANK_R = 8;
   private static final int REACH_BLOCKS = 8;
   private static final int[] NX4 = new int[]{1, -1, 0, 0};
   private static final int[] NZ4 = new int[]{0, 0, 1, -1};
   private static final int MOUTH_LOOK = 6;
   private static final double MOUTH_FAN = 0.9;
   private static final double MOUTH_TURN_MAX = Math.PI / 4;
   private static final double RIVER_FLARE = 1.5;
   private static final double FLARE_MAX_R = 0.5;
   private static final double[] LUT_COS = new double[256];
   private static final double[] LUT_SIN = new double[256];
   private static final int MAX_CACHED = 4096;
   private static final byte[] EMPTY;
   private static final ConcurrentHashMap<Long, byte[]> CACHE;
   private static final ConcurrentLinkedQueue<Long> ORDER;
   static final int RIVER_BAND_SPAN = 12;
   private static final int FALL_BAND_REACH = 2;
   private static final Set<Long> INCOMPLETE;
   public static final double RIVER_PUSH = 0.65;
   private static final double RIVER_TURN_COS_MIN = 0.0;
   private static final RawFlowTiles.Tile STILL_TILE;

   private StreamFlowGrid() {
   }

   public static int layerCount(byte[] grid) {
      return grid != null && grid.length >= 1536 ? grid.length / 1536 : 1;
   }

   public static int bandLo(byte[] grid, int layer, int i) {
      int o = layer * 1536 + 512 + i * 2;
      return (short)((grid[o] & 0xFF) << 8 | grid[o + 1] & 0xFF);
   }

   public static int bandHi(byte[] grid, int layer, int i) {
      int o = layer * 1536 + 1024 + i * 2;
      return (short)((grid[o] & 0xFF) << 8 | grid[o + 1] & 0xFF);
   }

   public static void putBand(byte[] grid, int layer, int i, int lo, int hi) {
      int o = layer * 1536 + 512 + i * 2;
      grid[o] = (byte)(lo >> 8);
      grid[o + 1] = (byte)lo;
      o = layer * 1536 + 1024 + i * 2;
      grid[o] = (byte)(hi >> 8);
      grid[o + 1] = (byte)hi;
   }

   public static int layerAt(byte[] grid, int i, int y) {
      if (grid != null && grid.length != 0) {
         if (grid.length < 1536) {
            return grid[i] == -1 ? -1 : 0;
         }

         int n = grid.length / 1536;

         for (int l = 0; l < n; l++) {
            if (grid[l * 1536 + i] != -1 && (y == Integer.MIN_VALUE || y >= bandLo(grid, l, i) && y <= bandHi(grid, l, i))) {
               return l;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static byte angleOf(byte[] grid, int lx, int lz, int y) {
      int i = (lx & 15) + (lz & 15) * 16;
      int l = layerAt(grid, i, y);
      return l < 0 ? -1 : grid[l * 1536 + i];
   }

   public static double speedOf(byte[] grid, int lx, int lz, int y) {
      int i = (lx & 15) + (lz & 15) * 16;
      int l = layerAt(grid, i, y);
      if (l < 0) {
         return 1.0;
      }

      int o = l * 1536 + 256 + i;
      return grid.length <= o ? 1.0 : unpackSpeed(grid[o]);
   }

   public static void registerLayerProducer(StreamFlowGrid.LayerProducer producer) {
      LAYER_PRODUCERS.add(producer);
   }

   private static byte[] withProducerLayers(ServerLevel level, RiverEngine engine, int cx, int cz, byte[] grid) {
      if (LAYER_PRODUCERS.isEmpty()) {
         return grid;
      }

      List<StreamFlowGrid.Layer> extra = new ArrayList<>();

      for (StreamFlowGrid.LayerProducer producer : LAYER_PRODUCERS) {
         try {
            List<StreamFlowGrid.Layer> l = producer.layers(level, engine, cx, cz);
            if (l != null) {
               extra.addAll(l);
            }
         } catch (RuntimeException e) {
            Constants.LOG.warn("Flow layer producer failed for chunk {},{} (ignored)", new Object[]{cx, cz, e});
         }
      }

      if (extra.isEmpty()) {
         return grid;
      }

      byte[] base = grid;
      if (base.length < 1536) {
         base = new byte[1536];
         Arrays.fill(base, 0, 256, (byte)-1);
         System.arraycopy(grid, 0, base, 0, Math.min(grid.length, 512));

         for (int i = 0; i < 256; i++) {
            putBand(base, 0, i, -32768, 32767);
         }
      }

      byte[] out = new byte[base.length + extra.size() * 1536];
      System.arraycopy(base, 0, out, 0, base.length);
      int l = base.length / 1536;

      for (StreamFlowGrid.Layer layer : extra) {
         System.arraycopy(layer.angle(), 0, out, l * 1536, 256);
         System.arraycopy(layer.speed(), 0, out, l * 1536 + 256, 256);

         for (int i = 0; i < 256; i++) {
            int alo = layer.lo()[i];
            int ahi = layer.hi()[i];
            putBand(out, l, i, ahi - alo == 12 ? alo - 1 : alo, ahi);
         }

         l++;
      }

      return out;
   }

   public static byte encode(double dx, double dz) {
      double a = Math.atan2(dz, dx);
      if (a < 0.0) {
         a += Math.PI * 2;
      }

      int idx = (int)Math.round(a / (Math.PI * 2) * 255.0);
      if (idx >= 255) {
         idx -= 255;
      }

      return (byte)idx;
   }

   public static Vec3 renderFallbackFlow(BlockGetter level, int x, int y, int z) {
      byte a = angleForPhysics(level, x, y, z);
      return a != -1 ? new Vec3(vx(a) * 0.25, 0.0, vz(a) * 0.25) : new Vec3(0.25, 0.0, 0.0);
   }

   public static double vx(byte b) {
      return LUT_COS[b & 0xFF];
   }

   public static double vz(byte b) {
      return LUT_SIN[b & 0xFF];
   }

   public static byte packSpeed(double speed) {
      double v = speed / 2.0;
      int q = (int)Math.round((v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v)) * 255.0);
      return (byte)q;
   }

   public static double unpackSpeed(byte b) {
      return (b & 0xFF) / 255.0 * 2.0;
   }

   public static double speedOf(byte[] grid, int lx, int lz) {
      int i = 256 + (lx & 15) + (lz & 15) * 16;
      return grid != null && grid.length > i ? unpackSpeed(grid[i]) : 1.0;
   }

   public static int cacheEntries() {
      return CACHE.size();
   }

   public static long cacheBytes() {
      long b = MemBytes.hashMapNodes(CACHE.size()) + ORDER.size() * 40L;

      for (byte[] g : CACHE.values()) {
         if (g != EMPTY) {
            b += MemBytes.array(g.length, 1);
         }
      }

      return b;
   }

   private static long key(int cx, int cz) {
      return (long)cx << 32 ^ cz & 4294967295L;
   }

   public static boolean streamCurrentAt(BlockGetter level, int x, int y, int z) {
      int i = (x & 15) + (z & 15) * 16;
      byte[] grid = ClientFlowGrids.gridAt(x >> 4, z >> 4);

      for (int pass = 0; pass < 2; pass++) {
         if (grid != null && grid.length > 0) {
            if (grid.length < 1536) {
               if (grid[i] != -1) {
                  return true;
               }
            } else {
               int l = layerAt(grid, i, y);
               if (l >= 0) {
                  return bandHi(grid, l, i) - bandLo(grid, l, i) != 12;
               }
            }
         }

         grid = CACHE.get(key(x >> 4, z >> 4));
      }

      return RTFFlowUnderlay.empty();
   }

   public static Vec3 imposedFlow(BlockGetter level, BlockPos pos, FluidState state, byte angle, double speed, Vec3 vanilla) {
      if (!(level instanceof Level) && speed < 0.25) {
         speed = 0.25;
      }

      return !state.isSource() && vanilla.lengthSqr() > 1.0E-6 && !streamCurrentAt(level, pos.getX(), pos.getY(), pos.getZ())
         ? vanilla.normalize().scale(speed)
         : new Vec3(vx(angle) * speed, 0.0, vz(angle) * speed);
   }

   public static byte angleForPhysics(BlockGetter level, int x, int z) {
      return angleForPhysics(level, x, Integer.MIN_VALUE, z);
   }

   public static byte angleForPhysics(BlockGetter level, int x, int y, int z) {
      if (level instanceof Level lv && !lv.dimension().equals(Level.OVERWORLD)) {
         return -1;
      } else {
         byte own = angleForPhysics(x, y, z);
         if (own != -1) {
            return own;
         }

         if (RTFFlowUnderlay.empty()) {
            return -1;
         }

         boolean client = !(level instanceof Level l2 && !l2.isClientSide());
         return RTFFlowUnderlay.angleAt(client, x, z);
      }
   }

   public static byte angleForEntityPush(BlockGetter level, int x, int z) {
      return angleForEntityPush(level, x, Integer.MIN_VALUE, z);
   }

   public static byte angleForEntityPush(BlockGetter level, int x, int y, int z) {
      if (level instanceof Level lv && !lv.dimension().equals(Level.OVERWORLD)) {
         return -1;
      } else {
         byte a = angleForPhysics(x, y, z);
         return a != -1 ? a : columnFallback(level, x, y, z);
      }
   }

   private static byte columnFallback(BlockGetter level, int x, int y, int z) {
      if (!(level instanceof ServerLevel server)) {
         return -1;
      } else {
         RiverEngine engine = StreamsWorldgen.engine(server);
         if (engine != null && engine.platesReadyNear(x + 0.5, z + 0.5, 0.0)) {
            ColumnPlan p = engine.plan(x, z);
            if (!carriesFlow(p)) {
               return -1;
            } else if (y == Integer.MIN_VALUE || y >= p.bedY() - 1 && y <= p.waterSurfaceY() + 1) {
               double fx = p.fluidFlowDirX();
               double fz = p.fluidFlowDirZ();
               double len = Math.sqrt(fx * fx + fz * fz);
               return len < 0.5 ? -1 : encode(fx / len, fz / len);
            } else {
               return -1;
            }
         } else {
            return -1;
         }
      }
   }

   private static byte angleForPhysics(int x, int y, int z) {
      byte a = ClientFlowGrids.angleAt(x, y, z);
      if (a != -1) {
         return a;
      }

      byte[] grid = CACHE.get(key(x >> 4, z >> 4));
      return grid != null && grid.length != 0 ? angleOf(grid, x, z, y) : -1;
   }

   public static double speedForPhysics(BlockGetter level, int x, int z) {
      return speedForPhysics(level, x, Integer.MIN_VALUE, z);
   }

   public static double speedForPhysics(BlockGetter level, int x, int y, int z) {
      if (level instanceof Level lv && !lv.dimension().equals(Level.OVERWORLD)) {
         return 1.0;
      } else {
         byte[] cg = ClientFlowGrids.gridAt(x >> 4, z >> 4);
         if (cg != null && cg.length > 256) {
            return speedOf(cg, x, z, y);
         }

         byte[] grid = CACHE.get(key(x >> 4, z >> 4));
         return grid != null && grid.length > 256 ? speedOf(grid, x, z, y) : 1.0;
      }
   }

   public static boolean carriesFlow(ColumnPlan p) {
      return p.inChannel();
   }

   public static boolean isIncomplete(int cx, int cz) {
      return INCOMPLETE.contains(key(cx, cz));
   }

   public static byte[] getOrBuild(
      ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, boolean flowRivers, int cx, int cz
   ) {
      long k = key(cx, cz);
      byte[] cached = CACHE.get(k);
      if (cached != null && !INCOMPLETE.contains(k)) {
         return cached.length == 0 ? null : cached;
      }

      FlowTrace.gridBuilds.incrementAndGet();
      byte[] grid = withProducerLayers(level, engine, cx, cz, build(engine, classifier, orient, flowRivers, cx, cz, false, null));
      byte[] out = cache(k, grid, cached == null);
      INCOMPLETE.remove(k);
      return out;
   }

   public static byte[] buildProvisional(
      ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, boolean flowRivers, int cx, int cz
   ) {
      long k = key(cx, cz);
      byte[] cached = CACHE.get(k);
      if (cached != null) {
         return cached.length == 0 ? null : cached;
      }

      FlowTrace.gridBuilds.incrementAndGet();
      boolean[] incomplete = new boolean[]{false};
      byte[] grid = withProducerLayers(level, engine, cx, cz, build(engine, classifier, orient, flowRivers, cx, cz, true, incomplete));
      byte[] out = cache(k, grid, true);
      if (incomplete[0]) {
         INCOMPLETE.add(k);
      }

      return out;
   }

   private static byte[] cache(long k, byte[] grid, boolean newEntry) {
      boolean any = false;
      int layers = layerCount(grid);

      for (int l = 0; l < layers && !any; l++) {
         int base = grid.length < 1536 ? 0 : l * 1536;

         for (int i = 0; i < 256 && base + i < grid.length; i++) {
            if (grid[base + i] != -1) {
               any = true;
               break;
            }
         }
      }

      CACHE.put(k, any ? grid : EMPTY);
      if (newEntry) {
         ORDER.add(k);
      }

      while (CACHE.size() > 4096) {
         Long old = ORDER.poll();
         if (old == null) {
            break;
         }

         CACHE.remove(old);
         INCOMPLETE.remove(old);
      }

      return any ? grid : null;
   }

   private static RawFlowTiles.Tile makeStillTile() {
      short[] sy = new short[256];
      Arrays.fill(sy, (short)-32768);
      return new RawFlowTiles.Tile(new byte[256], new byte[256], sy, sy);
   }

   private static double flowRadReady(
      RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, int x, int z, boolean readyOnly, boolean[] incomplete
   ) {
      if (readyOnly && !RiverFlowFields.isRegionReady(x, z)) {
         incomplete[0] = true;
         return Double.NaN;
      } else {
         return RiverFlowFields.flowRadAt(classifier, orient, x, z);
      }
   }

   private static byte[] build(
      RiverEngine engine,
      RiverFlowField.BiomeClass classifier,
      RiverFlowField.OrientationField orient,
      boolean flowRivers,
      int cx,
      int cz,
      boolean readyOnly,
      boolean[] incomplete
   ) {
      int m = 1 + Math.max(8, Math.max(4, 8));
      int span = 16 + 2 * m;
      int baseX = (cx << 4) - m;
      int baseZ = (cz << 4) - m;
      boolean anyRiverNear = false;
      if (flowRivers) {
         label707:
         for (int j = 0; j < span + 7; j += 8) {
            int pz = Math.min(j, span - 1);

            for (int i = 0; i < span + 7; i += 8) {
               int px = Math.min(i, span - 1);
               if (!Double.isNaN(flowRadReady(classifier, orient, baseX + px, baseZ + pz, readyOnly, incomplete))) {
                  anyRiverNear = true;
                  break label707;
               }
            }
         }
      }

      boolean useRivers = flowRivers && anyRiverNear;
      if (!anyRiverNear
         && (!readyOnly || engine.platesReadyNear(baseX + span / 2.0, baseZ + span / 2.0, span * 0.7072))
         && !engine.anyStreamsNear(baseX + span / 2.0, baseZ + span / 2.0, span * 0.7072)) {
         byte[] none = new byte[512];
         Arrays.fill(none, (byte)-1);
         return none;
      }

      double[] rawX = new double[span * span];
      double[] rawZ = new double[span * span];
      double[] rawS = new double[span * span];
      boolean[] has = new boolean[span * span];
      boolean[] fromStream = new boolean[span * span];
      short[] rawLo = new short[span * span];
      short[] rawHi = new short[span * span];
      Arrays.fill(rawLo, (short)-32768);
      int seaBandLo = engine.config().seaLevel() - 10;
      int seaBandHi = seaBandLo + 12;
      double[] riverAngle = null;
      if (useRivers) {
         riverAngle = new double[span * span];
         Arrays.fill(riverAngle, Double.NaN);
      }

      boolean anyRiver = false;
      int baseChunkX = baseX >> 4;
      int baseChunkZ = baseZ >> 4;
      RawFlowTiles.Tile[] tiles = new RawFlowTiles.Tile[9];

      for (int tz = 0; tz < 3; tz++) {
         for (int tx = 0; tx < 3; tx++) {
            int tcx = baseChunkX + tx;
            int tcz = baseChunkZ + tz;
            if (readyOnly && !engine.platesReadyNear((tcx << 4) + 8.0, (tcz << 4) + 8.0, 9.0)) {
               RawFlowTiles.Tile existing = RawFlowTiles.get(tcx, tcz);
               if (existing != null) {
                  tiles[tz * 3 + tx] = existing;
               } else {
                  incomplete[0] = true;
                  tiles[tz * 3 + tx] = STILL_TILE;
               }
            } else {
               tiles[tz * 3 + tx] = tileOrCompute(engine, tcx, tcz);
            }
         }
      }

      for (int j = 0; j < span; j++) {
         for (int i = 0; i < span; i++) {
            int wx = baseX + i;
            int wz = baseZ + j;
            int idx = j * span + i;
            double ux = 0.0;
            double uz = 0.0;
            double sp = 1.0;
            RawFlowTiles.Tile tile = tileFor(tiles, baseChunkX, baseChunkZ, wx, wz);
            boolean hasFlow;
            if (tile != null) {
               int ti = (wx & 15) * 16 + (wz & 15);
               byte bx = tile.dirX()[ti];
               byte bz = tile.dirZ()[ti];
               hasFlow = bx != 0 || bz != 0;
               if (hasFlow) {
                  double tl = Math.sqrt((double)bx * bx + (double)bz * bz);
                  sp = tl / 127.0 * 2.0;
                  ux = bx / tl;
                  uz = bz / tl;
                  short ws = tile.surfaceY()[ti];
                  short bd = tile.bedY()[ti];
                  if (ws != -32768) {
                     rawLo[idx] = (short)((bd != -32768 ? bd : ws - 12) - 1);
                     rawHi[idx] = (short)(ws + 1);
                  }
               }
            } else if (readyOnly && !engine.platesReadyNear(wx + 0.5, wz + 0.5, 0.0)) {
               incomplete[0] = true;
               hasFlow = false;
            } else {
               ColumnPlan p = engine.plan(wx, wz);
               double pfx = p.fluidFlowDirX();
               double pfz = p.fluidFlowDirZ();
               double pl = Math.sqrt(pfx * pfx + pfz * pfz);
               hasFlow = carriesFlow(p) && pl >= 0.5;
               if (hasFlow) {
                  ux = pfx / pl;
                  uz = pfz / pl;
                  sp = p.speed();
                  rawLo[idx] = (short)(p.bedY() - 1);
                  rawHi[idx] = (short)(p.waterSurfaceY() + 1);
               }
            }

            if (hasFlow) {
               rawX[idx] = ux;
               rawZ[idx] = uz;
               rawS[idx] = sp;
               has[idx] = true;
               fromStream[idx] = true;
            }

            if (useRivers) {
               double a = flowRadReady(classifier, orient, wx, wz, readyOnly, incomplete);
               if (!Double.isNaN(a)) {
                  riverAngle[idx] = a;
                  anyRiver = true;
               }
            }
         }
      }

      if (anyRiver) {
         int qBaseX = (baseX >> 2) - 1;
         int qBaseZ = (baseZ >> 2) - 1;
         int qSpanX = (baseX + span - 1 >> 2) - qBaseX + 1 + 1;
         int qSpanZ = (baseZ + span - 1 >> 2) - qBaseZ + 1 + 1;
         boolean[] riverQ = new boolean[qSpanX * qSpanZ];

         for (int qj = 0; qj < qSpanZ; qj++) {
            for (int qi = 0; qi < qSpanX; qi++) {
               int qwx = qBaseX + qi << 2;
               int qwz = qBaseZ + qj << 2;
               riverQ[qj * qSpanX + qi] = classifier.classify(qwx, qwz) == 1 || RiverFlowFields.bridgedNear(qwx, qwz);
            }
         }

         float[] rqf = new float[qSpanX * qSpanZ];

         for (int qi = 0; qi < riverQ.length; qi++) {
            rqf[qi] = riverQ[qi] ? 1.0F : 0.0F;
         }

         for (int j = 0; j < span; j++) {
            for (int i = 0; i < span; i++) {
               int idx = j * span + i;
               if (!Double.isNaN(riverAngle[idx]) && bilinearRiver(rqf, qSpanX, qSpanZ, qBaseX, qBaseZ, baseX + i, baseZ + j) >= 0.5) {
                  double a = riverAngle[idx];
                  double rx = Math.cos(a);
                  double rz = Math.sin(a);
                  if (has[idx]) {
                     double bx = rawX[idx] + (rx - rawX[idx]) * 0.65;
                     double bz = rawZ[idx] + (rz - rawZ[idx]) * 0.65;
                     double bl = Math.sqrt(bx * bx + bz * bz);
                     if (bl >= 0.5 && bx / bl * rawX[idx] + bz / bl * rawZ[idx] > 0.0) {
                        rawX[idx] = bx / bl;
                        rawZ[idx] = bz / bl;
                     }
                  } else {
                     rawX[idx] = rx;
                     rawZ[idx] = rz;
                     rawS[idx] = 1.0;
                     has[idx] = true;
                  }
               }
            }
         }
      }

      boolean[] core = (boolean[])has.clone();
      boolean[] hasB = new boolean[span * span];
      double[] rxB = new double[span * span];
      double[] rzB = new double[span * span];
      double[] rsB = new double[span * span];
      boolean[] fsB = new boolean[span * span];

      for (int pass = 0; pass < 8; pass++) {
         System.arraycopy(has, 0, hasB, 0, has.length);
         System.arraycopy(rawX, 0, rxB, 0, rawX.length);
         System.arraycopy(rawZ, 0, rzB, 0, rawZ.length);
         System.arraycopy(rawS, 0, rsB, 0, rawS.length);
         System.arraycopy(fromStream, 0, fsB, 0, fromStream.length);

         for (int j = 1; j < span - 1; j++) {
            for (int i = 1; i < span - 1; i++) {
               int idx = j * span + i;
               if (!has[idx]) {
                  for (int k = 0; k < 4; k++) {
                     int nidx = (j + NZ4[k]) * span + i + NX4[k];
                     if (has[nidx]) {
                        hasB[idx] = true;
                        rxB[idx] = rawX[nidx];
                        rzB[idx] = rawZ[nidx];
                        rsB[idx] = rawS[nidx];
                        fsB[idx] = fromStream[nidx];
                        break;
                     }
                  }
               }
            }
         }

         boolean[] ht = has;
         has = hasB;
         hasB = ht;
         double[] xt = rawX;
         rawX = rxB;
         rxB = xt;
         double[] zt = rawZ;
         rawZ = rzB;
         rzB = zt;
         double[] st = rawS;
         rawS = rsB;
         rsB = st;
         boolean[] ft = fromStream;
         fromStream = fsB;
         fsB = ft;
      }

      double[] mouthX = new double[span * span];
      double[] mouthZ = new double[span * span];
      double[] mouthW = new double[span * span];
      int[] seedOf = new int[span * span];
      Arrays.fill(seedOf, -1);
      ArrayDeque<Integer> mouths = new ArrayDeque<>();

      for (int j = 1; j < span - 1; j++) {
         for (int i = 1; i < span - 1; i++) {
            int idx = j * span + i;
            if (core[idx]) {
               double dx = rawX[idx];
               double dz = rawZ[idx];
               if (!(dx * dx + dz * dz < 1.0E-9)) {
                  int ai = i + (int)Math.round(dx * 2.0);
                  int aj = j + (int)Math.round(dz * 2.0);
                  if (ai >= 0 && aj >= 0 && ai < span && aj < span && !core[aj * span + ai] && classifier.classify(baseX + ai, baseZ + aj) == 2) {
                     seedOf[idx] = idx;
                     mouths.add(idx);
                  }
               }
            }
         }
      }

      if (!mouths.isEmpty()) {
         ArrayDeque<Integer> sea = new ArrayDeque<>(mouths);

         while (!sea.isEmpty()) {
            int cur = sea.poll();
            int ci0 = cur % span;
            int cj0 = cur / span;

            for (int k = 0; k < 4; k++) {
               int ni = ci0 + NX4[k];
               int nj = cj0 + NZ4[k];
               if (ni >= 1 && nj >= 1 && ni < span - 1 && nj < span - 1) {
                  int nid = nj * span + ni;
                  if (seedOf[nid] < 0 && !core[nid] && has[nid]) {
                     seedOf[nid] = seedOf[cur];
                     sea.add(nid);
                  }
               }
            }
         }

         for (int idx = 0; idx < seedOf.length; idx++) {
            if (seedOf[idx] >= 0 && !core[idx]) {
               double vx = idx % span - seedOf[idx] % span;
               double vz = idx / span - seedOf[idx] / span;
               double vl = Math.hypot(vx, vz);
               if (vl > 1.0E-9) {
                  mouthX[idx] = vx / vl;
                  mouthZ[idx] = vz / vl;
                  mouthW[idx] = 1.0;
               }
            }
         }

         int[] up = new int[span * span];
         Arrays.fill(up, -1);
         ArrayDeque<Integer> chan = new ArrayDeque<>();

         for (int idx : mouths) {
            up[idx] = 0;
            chan.add(idx);
         }

         while (!chan.isEmpty()) {
            int cur = chan.poll();
            if (up[cur] < 6) {
               int ci0 = cur % span;
               int cj0 = cur / span;

               for (int k = 0; k < 4; k++) {
                  int ni = ci0 + NX4[k];
                  int nj = cj0 + NZ4[k];
                  if (ni >= 1 && nj >= 1 && ni < span - 1 && nj < span - 1) {
                     int nid = nj * span + ni;
                     if (up[nid] < 0 && core[nid]) {
                        up[nid] = up[cur] + 1;
                        chan.add(nid);
                     }
                  }
               }
            }
         }

         for (int idx = 0; idx < up.length; idx++) {
            if (up[idx] >= 0) {
               mouthW[idx] = 1.0 - up[idx] / 6.0;
            }
         }
      }

      int blurR = m - 1;
      byte[] padded = new byte[324];
      double[] paddedS = new double[324];
      Arrays.fill(padded, (byte)-1);
      Arrays.fill(paddedS, 1.0);

      for (int lz = -1; lz <= 16; lz++) {
         for (int lx = -1; lx <= 16; lx++) {
            int ci = lx + m;
            int cj = lz + m;
            if (has[cj * span + ci]) {
               double sx = 0.0;
               double sz = 0.0;

               for (int dj = -blurR; dj <= blurR; dj++) {
                  for (int di = -blurR; di <= blurR; di++) {
                     int idx = (cj + dj) * span + ci + di;
                     if (has[idx]) {
                        double wgt = 1.0 / (1.0 + di * di + dj * dj);
                        sx += rawX[idx] * wgt;
                        sz += rawZ[idx] * wgt;
                     }
                  }
               }

               double dirX = sx;
               double dirZ = sz;
               if (dirX * dirX + dirZ * dirZ <= 1.0E-9) {
                  dirX = rawX[cj * span + ci];
                  dirZ = rawZ[cj * span + ci];
               }

               int mi = cj * span + ci;
               double fan = mouthW[mi];
               if (fan > 0.0) {
                  double turn;
                  if (mouthX[mi] == 0.0 && mouthZ[mi] == 0.0) {
                     double px = -dirZ;
                     double pz = dirX;
                     int runP = perpRun(core, span, ci, cj, px, pz);
                     int runM = perpRun(core, span, ci, cj, -px, -pz);
                     turn = 0.9 * ((double)(runM - runP) / Math.max(1, runM + runP));
                  } else {
                     turn = Math.atan2(dirX * mouthZ[mi] - dirZ * mouthX[mi], dirX * mouthX[mi] + dirZ * mouthZ[mi]);
                  }

                  turn = Math.max(-Math.PI / 4, Math.min(Math.PI / 4, turn * fan));
                  double cs = Math.cos(turn);
                  double sn = Math.sin(turn);
                  double tx = dirX * cs - dirZ * sn;
                  double tz = dirX * sn + dirZ * cs;
                  double tl = Math.hypot(tx, tz);
                  if (tl > 1.0E-9) {
                     dirX = tx / tl;
                     dirZ = tz / tl;
                  }
               }

               double bnx = 0.0;
               double bnz = 0.0;
               int nonFlow = 0;

               for (int dj = -8; dj <= 8; dj++) {
                  for (int di = -8; di <= 8; di++) {
                     if ((di != 0 || dj != 0) && !has[(cj + dj) * span + ci + di]) {
                        double d = Math.sqrt(di * di + dj * dj);
                        bnx += di / d;
                        bnz += dj / d;
                        nonFlow++;
                     }
                  }
               }

               if (nonFlow > 0) {
                  double nlen = Math.hypot(bnx, bnz);
                  double asym = nlen / nonFlow;
                  if (nlen > 1.0E-6 && asym > 0.001) {
                     double px = -bnz / nlen;
                     double pz = bnx / nlen;
                     if (px * dirX + pz * dirZ < 0.0) {
                        px = -px;
                        pz = -pz;
                     }

                     double w = Math.min(0.85, asym * 1.6) * (1.0 - fan);
                     double bx = (1.0 - w) * dirX + w * px;
                     double bz = (1.0 - w) * dirZ + w * pz;
                     double bl = Math.hypot(bx, bz);
                     if (bl > 1.0E-9) {
                        dirX = bx / bl;
                        dirZ = bz / bl;
                     }

                     int dci = ci + (int)Math.round(dirX * 4.0);
                     int dcj = cj + (int)Math.round(dirZ * 4.0);
                     if (dci >= 0 && dcj >= 0 && dci < span && dcj < span) {
                        double dWds = (perpWidth(has, span, dci, dcj, dirX, dirZ) - perpWidth(has, span, ci, cj, dirX, dirZ)) / 4.0;
                        double flare = Math.max(-0.5, Math.min(0.5, 1.5 * dWds * asym));
                        double fx2 = dirX + flare * (bnx / nlen);
                        double fz2 = dirZ + flare * (bnz / nlen);
                        double fl2 = Math.hypot(fx2, fz2);
                        if (fl2 > 1.0E-9) {
                           dirX = fx2 / fl2;
                           dirZ = fz2 / fl2;
                        }
                     }
                  }
               }

               double edge = edgeDistance(has, span, ci, cj, 4);
               if (!(edge < 4.0) || perpWidth(has, span, ci, cj, dirX, dirZ) < 10 || !(edge < wave((cx << 4) + lx, (cz << 4) + lz))) {
                  padded[lx + 1 + (lz + 1) * 18] = encode(dirX, dirZ);
                  paddedS[lx + 1 + (lz + 1) * 18] = rawS[cj * span + ci];
               }
            }
         }
      }

      byte[] grid = new byte[1536];
      Arrays.fill(grid, 0, 256, (byte)-1);

      for (int lz = 0; lz < 16; lz++) {
         for (int lx = 0; lx < 16; lx++) {
            byte own = padded[lx + 1 + (lz + 1) * 18];
            if (own != -1) {
               int bi = (lz + m) * span + lx + m;
               int lo = rawLo[bi];
               int hi = rawHi[bi];
               if (lo == -32768) {
                  for (int r = 1; r <= 3 && lo == -32768; r++) {
                     for (int dj = -r; dj <= r && lo == -32768; dj++) {
                        for (int di = -r; di <= r; di++) {
                           int ni = bi + dj * span + di;
                           if (ni >= 0 && ni < rawLo.length && rawLo[ni] != -32768) {
                              lo = rawLo[ni];
                              hi = rawHi[ni];
                              break;
                           }
                        }
                     }
                  }
               }

               if (lo != -32768) {
                  for (int dj = -2; dj <= 2; dj++) {
                     for (int di = -2; di <= 2; di++) {
                        int ni = bi + dj * span + di;
                        if (ni >= 0 && ni < rawHi.length && rawLo[ni] != -32768 && rawHi[ni] > hi) {
                           hi = rawHi[ni];
                        }
                     }
                  }
               }

               boolean streamColumn = fromStream[bi];

               for (int dj = -2; dj <= 2 && !streamColumn; dj++) {
                  for (int di = -2; di <= 2 && !streamColumn; di++) {
                     int ni = bi + dj * span + di;
                     streamColumn = ni >= 0 && ni < fromStream.length && fromStream[ni];
                  }
               }

               if (lo == -32768) {
                  lo = seaBandLo;
                  hi = seaBandHi;
                  if (streamColumn) {
                     lo--;
                  }
               } else if (hi - lo == 12) {
                  lo--;
               }

               putBand(grid, 0, lx + lz * 16, lo, hi);
               double sx = 0.0;
               double sz = 0.0;

               for (int dj = -1; dj <= 1; dj++) {
                  for (int di = -1; di <= 1; di++) {
                     byte b = padded[lx + 1 + di + (lz + 1 + dj) * 18];
                     if (b != -1) {
                        double wgt = 1.0 / (1.0 + di * di + dj * dj);
                        sx += LUT_COS[b & 0xFF] * wgt;
                        sz += LUT_SIN[b & 0xFF] * wgt;
                     }
                  }
               }

               grid[lx + lz * 16] = sx * sx + sz * sz > 1.0E-9 ? encode(sx, sz) : own;
               grid[256 + lx + lz * 16] = packSpeed(paddedS[lx + 1 + (lz + 1) * 18]);
            }
         }
      }

      return grid;
   }

   private static double bilinearRiver(float[] rqf, int qSpanX, int qSpanZ, int qBaseX, int qBaseZ, int wx, int wz) {
      double fx = (wx - 1.5) / 4.0 - qBaseX;
      double fz = (wz - 1.5) / 4.0 - qBaseZ;
      int i0 = (int)Math.floor(fx);
      int j0 = (int)Math.floor(fz);
      double tx = fx - i0;
      double tz = fz - j0;
      double a = rqAt(rqf, qSpanX, qSpanZ, i0, j0) + (rqAt(rqf, qSpanX, qSpanZ, i0 + 1, j0) - rqAt(rqf, qSpanX, qSpanZ, i0, j0)) * tx;
      double b = rqAt(rqf, qSpanX, qSpanZ, i0, j0 + 1) + (rqAt(rqf, qSpanX, qSpanZ, i0 + 1, j0 + 1) - rqAt(rqf, qSpanX, qSpanZ, i0, j0 + 1)) * tx;
      return a + (b - a) * tz;
   }

   private static double rqAt(float[] rqf, int qSpanX, int qSpanZ, int qi, int qj) {
      return qi >= 0 && qj >= 0 && qi < qSpanX && qj < qSpanZ ? rqf[qj * qSpanX + qi] : 0.0;
   }

   private static double edgeDistance(boolean[] has, int span, int ci, int cj, int r) {
      double best = r;

      for (int dj = -r; dj <= r; dj++) {
         for (int di = -r; di <= r; di++) {
            if ((di != 0 || dj != 0) && !has[(cj + dj) * span + ci + di]) {
               double d = Math.sqrt(di * di + dj * dj);
               if (d < best) {
                  best = d;
               }
            }
         }
      }

      return best;
   }

   private static int perpWidth(boolean[] has, int span, int ci, int cj, double dirX, double dirZ) {
      double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
      if (len < 1.0E-9) {
         return 0;
      }

      double px = -dirZ / len;
      double pz = dirX / len;
      return 1 + perpRun(has, span, ci, cj, px, pz) + perpRun(has, span, ci, cj, -px, -pz);
   }

   private static int perpRun(boolean[] has, int span, int ci, int cj, double px, double pz) {
      int run = 0;

      for (int k = 1; k <= 10; k++) {
         int i = ci + (int)Math.round(px * k);
         int j = cj + (int)Math.round(pz * k);
         if (i < 0 || j < 0 || i >= span || j >= span) {
            return 10;
         }

         if (!has[j * span + i]) {
            return run;
         }

         run++;
      }

      return run;
   }

   private static double wave(int x, int z) {
      double s = Math.sin(x * 0.14 + z * 0.05) + 0.7 * Math.sin(x * 0.045 - z * 0.17);
      return (s / 1.7 + 1.0) * 0.5 * 4.0;
   }

   private static RawFlowTiles.Tile tileOrCompute(RiverEngine engine, int cx, int cz) {
      RawFlowTiles.Tile tile = RawFlowTiles.get(cx, cz);
      if (tile != null) {
         return tile;
      }

      FlowTrace.tilesComputed.incrementAndGet();
      byte[] dirX = new byte[256];
      byte[] dirZ = new byte[256];
      short[] surfaceY = new short[256];
      short[] bedY = new short[256];
      Arrays.fill(surfaceY, (short)-32768);
      Arrays.fill(bedY, (short)-32768);
      int baseX = cx << 4;
      int baseZ = cz << 4;

      for (int dx = 0; dx < 16; dx++) {
         for (int dz = 0; dz < 16; dz++) {
            ColumnPlan p = engine.plan(baseX + dx, baseZ + dz);
            if (carriesFlow(p)) {
               double fx = p.fluidFlowDirX();
               double fz = p.fluidFlowDirZ();
               double len = Math.sqrt(fx * fx + fz * fz);
               if (!(len < 0.5)) {
                  int i = dx * 16 + dz;
                  double sc = 127.0 * (p.speed() / 2.0) / len;
                  dirX[i] = (byte)Math.round(fx * sc);
                  dirZ[i] = (byte)Math.round(fz * sc);
                  int ws = p.waterSurfaceY();
                  surfaceY[i] = ws >= -32768 && ws <= 32767 ? (short)ws : -32768;
                  bedY[i] = (short)Math.max(-32767, Math.min(32767, p.bedY()));
               }
            }
         }
      }

      tile = new RawFlowTiles.Tile(dirX, dirZ, surfaceY, bedY);
      RawFlowTiles.put(cx, cz, tile);
      return tile;
   }

   private static RawFlowTiles.Tile tileFor(RawFlowTiles.Tile[] tiles, int baseChunkX, int baseChunkZ, int wx, int wz) {
      int tx = (wx >> 4) - baseChunkX;
      int tz = (wz >> 4) - baseChunkZ;
      return tx >= 0 && tx <= 2 && tz >= 0 && tz <= 2 ? tiles[tz * 3 + tx] : null;
   }

   public static byte[] peek(int cx, int cz) {
      return CACHE.get(key(cx, cz));
   }

   public static void invalidateArea(int minX, int minZ, int size) {
      int c0x = minX >> 4;
      int c0z = minZ >> 4;
      int c1x = minX + size - 1 >> 4;
      int c1z = minZ + size - 1 >> 4;

      for (int cx = c0x; cx <= c1x; cx++) {
         for (int cz = c0z; cz <= c1z; cz++) {
            CACHE.remove(key(cx, cz));
         }
      }
   }

   public static void clear() {
      RTFFlowUnderlay.clearServer();
      CACHE.clear();
      ORDER.clear();
   }

   static {
      for (int b = 0; b < 256; b++) {
         LUT_COS[b] = Math.cos(b / 255.0 * (Math.PI * 2));
         LUT_SIN[b] = Math.sin(b / 255.0 * (Math.PI * 2));
      }

      EMPTY = new byte[0];
      CACHE = new ConcurrentHashMap<>();
      ORDER = new ConcurrentLinkedQueue<>();
      INCOMPLETE = ConcurrentHashMap.newKeySet();
      STILL_TILE = makeStillTile();
   }

   public record Layer(byte[] angle, byte[] speed, short[] lo, short[] hi) {
   }

   @FunctionalInterface
   public interface LayerProducer {
      List<StreamFlowGrid.Layer> layers(ServerLevel var1, RiverEngine var2, int var3, int var4);
   }
}
