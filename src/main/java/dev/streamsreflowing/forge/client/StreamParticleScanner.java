package dev.streamsreflowing.forge.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.RainCount;
import dev.streamsreflowing.client.FoamSize;
import dev.streamsreflowing.client.SprayBudget;
import dev.streamsreflowing.client.SprayCull;
import dev.streamsreflowing.client.StreamAudio;
import dev.streamsreflowing.compat.CascadeBridge;
import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import dev.streamsreflowing.forge.particle.StreamParticles;
import dev.streamsreflowing.forge.particle.StreamSplashParticle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public final class StreamParticleScanner {
   public static SpriteSet splashSprites;
   public static SpriteSet foamSprites;
   private static final int NEAR = 16;
   private static final int SCAN_FROM = 4;
   private static final int COLUMNS_PER_TICK = 160;
   private static final int SCAN_DEPTH = 16;
   private static final int MAX_FALL_SCAN = 16;
   private static final int REF_DIST = 16;
   private static final double SIZE_EXP = 0.65;
   private static final double NEAR_RATE = 1.5;
   private static final int MAX_SPAWN_PER_TICK = 1000;
   private static final int REVERIFY = 40;
   private static final int EXPIRE = 300;
   private static final RandomSource RAND = RandomSource.create();
   private static final MutableBlockPos COL = new MutableBlockPos();
   private static final List<int[]> gridOffsets = new ArrayList<>();
   private static int gridRadius = -1;
   private static int scanCursor = 0;
   private static HashMap<Long, long[]> feet = new HashMap<>();
   private static int sweepRound = 0;
   private static Object lastLevel = null;
   private static int diagSpawned;
   private static final int SURFACE_SAMPLES = 667;
   private static long surfaceTick = -1L;
   private static int surfaceSpawned = 0;
   private static Object particleCheckLevel;
   private static boolean particlesAvailable;
   private static final int BAND_REACH = 48;

   private StreamParticleScanner() {
   }

   public static void onClientTick() {
      if (!RainCount.particlesOff) {
         if (!RainCount.ON) {
            streamsreflowing$tick();
         } else {
            long t0 = System.nanoTime();
            int before = diagSpawned;
            streamsreflowing$tick();
            RainCount.scan(System.nanoTime() - t0, diagSpawned - before);
            streamsreflowing$publishFeet();
         }
      }
   }

   private static void streamsreflowing$publishFeet() {
      RainCount.footCount = feet.size();
      int bestH = -1;
      long bestP = 0L;

      for (Entry<Long, long[]> e : feet.entrySet()) {
         int h = (int)e.getValue()[0];
         if (h > bestH) {
            bestH = h;
            bestP = e.getKey();
         }
      }

      RainCount.bestFootHeight = bestH;
      RainCount.bestFoot = bestP;
   }

   private static void streamsreflowing$tick() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      LocalPlayer player = mc.player;
      if (level != null && player != null && !mc.isPaused()) {
         double ambience = StreamsSettings.waterAmbience();
         boolean audio = ambience > 0.0;
         int mode = StreamsSettings.streamBubbles();
         surfaceFlowEmit(level, player, mode, audio);
         StreamAudio.tick(level, player, feet, audio, ambience);
         if (mode != 0) {
            if (splashSprites != null && foamSprites != null) {
               int radius = StreamsSettings.streamParticleScanRadius();
               if (radius <= 16) {
                  if (!feet.isEmpty()) {
                     feet.clear();
                  }
               } else {
                  if (level != lastLevel) {
                     feet.clear();
                     scanCursor = 0;
                     sweepRound = 0;
                     lastLevel = level;
                  }

                  if (radius != gridRadius) {
                     buildGrid(radius);
                  }

                  int cx = Mth.floor(player.getX());
                  int cz = Mth.floor(player.getZ());
                  long now = level.getGameTime();
                  boolean vanilla = StreamsSettings.particlesOnVanillaWater();
                  if (!gridOffsets.isEmpty()) {
                     int perTick = Math.min(160, gridOffsets.size());

                     for (int n = 0; n < perTick; n++) {
                        int[] off = gridOffsets.get(scanCursor);
                        int step = off[2];
                        int cellX = Math.floorDiv(cx + off[0], step);
                        int cellZ = Math.floorDiv(cz + off[1], step);
                        int wx = cellX * step + Math.floorMod(cellHash(cellX, cellZ, -1640531527) + sweepRound, step);
                        int wz = cellZ * step + Math.floorMod(cellHash(cellX, cellZ, -2048144789) + sweepRound / step, step);
                        int[] foot = scanColumn(level, wx, wz, vanilla);
                        if (foot != null) {
                           feet.put(BlockPos.asLong(foot[0], foot[1], foot[2]), new long[]{foot[3], now, foot[4]});
                        }

                        scanCursor++;
                        if (scanCursor >= gridOffsets.size()) {
                           scanCursor = 0;
                           sweepRound++;
                        }
                     }
                  }

                  double maxScale = Math.pow(radius / 16.0, 0.65);
                  double sizeKnob = StreamsSettings.particleSize();
                  double rateKnob = StreamsSettings.particleRate();
                  int spawnLimit = SprayBudget.inWeather(Math.min(1000, StreamsSettings.maxStreamParticles()), level);
                  Vec3 look = player.getViewVector(1.0F);
                  double lookX = look.x;
                  double lookZ = look.z;
                  double lookLen = Math.sqrt(lookX * lookX + lookZ * lookZ);
                  int spawned = 0;
                  List<long[]> moved = null;
                  Iterator<Entry<Long, long[]>> it = feet.entrySet().iterator();

                  while (it.hasNext() && spawned < spawnLimit) {
                     Entry<Long, long[]> e = it.next();
                     long[] val = e.getValue();
                     if (now - val[1] > 300L) {
                        it.remove();
                     } else {
                        long key = e.getKey();
                        int fx = BlockPos.getX(key);
                        int fy = BlockPos.getY(key);
                        int fz = BlockPos.getZ(key);
                        double dx = fx + 0.5 - player.getX();
                        double dz = fz + 0.5 - player.getZ();
                        double d = Math.sqrt(dx * dx + dz * dz);
                        double minD = (int)val[2] == 2 ? 4.0 : 16.0;
                        if (!(d < minD) && !(d > radius + 4)) {
                           if (now - val[1] > 40L) {
                              int[] again = scanColumn(level, fx, fz, vanilla);
                              if (again == null) {
                                 it.remove();
                                 continue;
                              }

                              if (again[1] != fy) {
                                 it.remove();
                                 if (moved == null) {
                                    moved = new ArrayList<>();
                                 }

                                 moved.add(new long[]{BlockPos.asLong(again[0], again[1], again[2]), again[3], now, again[4]});
                                 continue;
                              }

                              val[0] = again[3];
                              val[1] = now;
                              val[2] = again[4];
                           }

                           if (!SprayCull.behind(dx, dz, d, lookX, lookZ, lookLen)) {
                              double lod = Mth.clamp(Math.pow(d / 16.0, 0.65), 1.0, maxScale);
                              double scale = lod * sizeKnob;
                              double rate = Math.min(1.0, 1.5 / (lod * lod)) * rateKnob / (sizeKnob * sizeKnob);
                              int made = emit(mc, level, fx, fy, fz, (int)val[0], (float)scale, rate, spawnLimit - spawned);
                              spawned += made;
                              diagSpawned += made;
                           }
                        }
                     }
                  }

                  if (moved != null) {
                     for (long[] m : moved) {
                        feet.put(m[0], new long[]{m[1], m[2], m[3]});
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean particlesAvailable(ClientLevel level) {
      if (particleCheckLevel != level) {
         particleCheckLevel = level;
         particlesAvailable = StreamParticles.STREAM_BUBBLE.isPresent()
            && StreamParticles.STREAM_SPLASH.isPresent()
            && StreamParticles.STREAM_FOAM.isPresent()
            && StreamParticles.STREAM_LEAF.isPresent()
            && StreamParticles.STREAM_STICK.isPresent();
         if (!particlesAvailable) {
            Constants.LOG.info("Stream spray is unavailable on this server, so stream water stays still. Nothing else changes.");
         }
      }

      return particlesAvailable;
   }

   private static void surfaceFlowEmit(ClientLevel level, LocalPlayer player, int mode, boolean audio) {
      long now = level.getGameTime();
      if (now != surfaceTick) {
         surfaceTick = now;
         surfaceSpawned = 0;
      }

      int cap = SprayBudget.inWeather(StreamsSettings.maxStreamParticles(), level);
      boolean particles = mode != 0 && particlesAvailable(level);
      if (particles || audio) {
         boolean debris = StreamsSettings.streamDebris();
         double drift = mode >= 2 ? 0.13 : 0.0;
         double pxd = player.getX();
         double pyd = player.getY();
         double pzd = player.getZ();
         int px0 = Mth.floor(pxd);
         int py0 = Mth.floor(pyd);
         int pz0 = Mth.floor(pzd);

         for (int i = 0; i < 667; i++) {
            int x = px0 + RAND.nextInt(33) - 16;
            int y = py0 + RAND.nextInt(33) - 16;
            int z = pz0 + RAND.nextInt(33) - 16;
            byte angle = ClientFlowGrids.angleAt(x, z);
            if (angle != -1) {
               COL.set(x, y, z);
               if (level.getFluidState(COL).is(FluidTags.WATER)) {
                  COL.set(x, y + 1, z);
                  if (level.getFluidState(COL).isEmpty() && particles && surfaceSpawned < cap) {
                     emitSurfaceAt(level, x, y, z, angle, drift, debris);
                  }
               }
            }
         }
      }
   }

   private static void emitSurfaceAt(ClientLevel level, int x, int y, int z, byte angle, double drift, boolean debris) {
      double fx = StreamFlowGrid.vx(angle);
      double fz = StreamFlowGrid.vz(angle);
      double sp = ClientFlowGrids.speedAt(x, z);
      double fast = Math.max(0.0, sp - 1.0);
      drift *= sp;
      double density = StreamsSettings.particleRate();
      double px = x + RAND.nextDouble();
      double pz = z + RAND.nextDouble();
      if (RAND.nextDouble() < density * sp / 24.0) {
         int clump = 2 + RAND.nextInt(3);

         for (int i = 0; i < clump; i++) {
            surfaceSpawn(
               level,
               (ParticleOptions)StreamParticles.STREAM_BUBBLE.get(),
               px + (RAND.nextDouble() - 0.5) * 0.4,
               y + 0.8,
               pz + (RAND.nextDouble() - 0.5) * 0.4,
               fx * drift,
               0.0,
               fz * drift
            );
         }
      }

      if (debris && RAND.nextDouble() < density / 120.0) {
         surfaceSpawn(level, (ParticleOptions)StreamParticles.STREAM_LEAF.get(), px, y + 0.9, pz, fx * drift, 0.0, fz * drift);
      }

      if (debris && RAND.nextDouble() < density / 2000.0) {
         surfaceSpawn(level, (ParticleOptions)StreamParticles.STREAM_STICK.get(), px, y + 0.9, pz, fx * drift, 0.0, fz * drift);
      }

      if (fast > 0.0 && RAND.nextDouble() < density * fast * fast * 0.5) {
         int foam = 1 + RAND.nextInt(2);

         for (int i = 0; i < foam; i++) {
            surfaceSpawn(
               level,
               (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
               px + (RAND.nextDouble() - 0.5) * 0.7,
               y + 0.86,
               pz + (RAND.nextDouble() - 0.5) * 0.7,
               fx * drift * 0.6,
               0.05 + RAND.nextDouble() * 0.07,
               fz * drift * 0.6
            );
         }
      }

      for (Direction d : Plane.HORIZONTAL) {
         int nx = x + d.getStepX();
         int nz = z + d.getStepZ();
         byte na = ClientFlowGrids.angleAt(nx, nz);
         COL.set(nx, y, nz);
         boolean sameDir = na != -1 && level.getFluidState(COL).is(FluidTags.WATER) && angleClose(na, angle);
         if (!sameDir && RAND.nextDouble() < density * sp * sp / 8.0) {
            double bx = px + d.getStepX() * 0.5;
            double bz = pz + d.getStepZ() * 0.5;
            int flecks = 1 + RAND.nextInt(2);

            for (int i = 0; i < flecks; i++) {
               double ang = RAND.nextDouble() * (Math.PI * 2);
               double spd = 0.05 * (0.5 + RAND.nextDouble()) * sp;
               surfaceSpawn(
                  level,
                  (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
                  bx + (RAND.nextDouble() - 0.5) * 0.25,
                  y + 0.82,
                  bz + (RAND.nextDouble() - 0.5) * 0.25,
                  Math.cos(ang) * spd,
                  0.06 + RAND.nextDouble() * 0.08,
                  Math.sin(ang) * spd
               );
            }
         }
      }

      int aox = (int)Math.round(fx);
      int aoz = (int)Math.round(fz);
      if (aox != 0 || aoz != 0) {
         COL.set(x + aox, y, z + aoz);
         boolean streamAhead = ClientFlowGrids.angleAt(x + aox, z + aoz) != -1 && level.getFluidState(COL).is(FluidTags.WATER);
         if (!streamAhead && RAND.nextDouble() < density * sp / 7.0) {
            surfaceSpawn(
               level,
               (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
               px + aox * 0.4,
               y + 0.85,
               pz + aoz * 0.4,
               fx * 0.12 * sp,
               0.05 + RAND.nextDouble() * 0.07,
               fz * 0.12 * sp
            );
         }
      }
   }

   private static boolean angleClose(byte a, byte b) {
      int diff = Math.abs((a & 255) - (b & 255));
      if (diff > 128) {
         diff = 256 - diff;
      }

      return diff <= 32;
   }

   private static void surfaceSpawn(ClientLevel level, ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
      surfaceSpawned++;
      diagSpawned++;
      level.addParticle(type, x, y, z, vx, vy, vz);
   }

   private static void buildGrid(int radius) {
      gridOffsets.clear();
      int band = 12;

      for (int dMin = 4; dMin < radius; dMin += 12) {
         int dMax = Math.min(dMin + 12, radius);
         double dMid = (dMin + dMax) * 0.5;
         int step;
         if (dMid < 48.0) {
            step = 2;
         } else if (dMid < 96.0) {
            step = 3;
         } else if (dMid < 160.0) {
            step = 5;
         } else if (dMid < 320.0) {
            step = 8;
         } else {
            step = 12;
         }

         int dMinSq = dMin * dMin;
         int dMaxSq = dMax * dMax;

         for (int ox = -dMax; ox <= dMax; ox += step) {
            for (int oz = -dMax; oz <= dMax; oz += step) {
               int dsq = ox * ox + oz * oz;
               if (dsq >= dMinSq && dsq < dMaxSq) {
                  gridOffsets.add(new int[]{ox, oz, step});
               }
            }
         }
      }

      gridRadius = radius;
      scanCursor = 0;
   }

   private static int cellHash(int x, int z, int salt) {
      int h = x * -1640531535 ^ z * -2048144777 ^ salt;
      h ^= h >>> 15;
      h *= 739982445;
      h ^= h >>> 12;
      return h & 2147483647;
   }

   private static int[] scanColumn(ClientLevel level, int x, int z, boolean vanilla) {
      int topY = level.getHeight(Types.MOTION_BLOCKING, x, z);
      int[] foot = scanBand(level, x, z, topY, topY - 16, vanilla);
      if (foot != null) {
         return foot;
      }

      byte[] g = ClientFlowGrids.gridAt(x >> 4, z >> 4);
      if (g != null && g.length >= 1536) {
         int i = (x & 15) + (z & 15) * 16;
         int layers = g.length / 1536;

         for (int l = 1; l < layers; l++) {
            if (g[l * 1536 + i] != -1) {
               int hi = Math.min(topY - 16 - 1, StreamFlowGrid.bandHi(g, l, i) + 1);
               int lo = Math.max(StreamFlowGrid.bandLo(g, l, i) - 1, hi - 48);
               foot = scanBand(level, x, z, hi, lo, vanilla);
               if (foot != null) {
                  return foot;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static int[] scanBand(ClientLevel level, int x, int z, int topY, int bottomY, boolean vanilla) {
      int floorY = level.getMinBuildHeight() + 1;

      for (int y = topY; y >= bottomY && y >= floorY; y--) {
         int aboveKind = fluidKind(level, x, y + 1, z, vanilla);
         if (aboveKind != 0 && fluidKind(level, x, y - 1, z, vanilla) == 0) {
            boolean thisFalling = fluidKind(level, x, y, z, vanilla) != 0;
            int h = 0;

            for (int dy = thisFalling ? 0 : 1; dy <= 16 && fluidKind(level, x, y + dy, z, vanilla) != 0; dy++) {
               h++;
            }

            if (h >= 2 && fallInOpenAir(level, x, y + 1, z)) {
               return new int[]{x, y, z, h, aboveKind};
            }
         }
      }

      return null;
   }

   private static boolean fallInOpenAir(ClientLevel level, int x, int y, int z) {
      MutableBlockPos p = new MutableBlockPos();
      return level.getBlockState(p.set(x + 1, y, z)).isAir()
         || level.getBlockState(p.set(x - 1, y, z)).isAir()
         || level.getBlockState(p.set(x, y, z + 1)).isAir()
         || level.getBlockState(p.set(x, y, z - 1)).isAir();
   }

   private static int fluidKind(ClientLevel level, int x, int y, int z, boolean vanilla) {
      COL.set(x, y, z);
      FluidState fs = level.getFluidState(COL);
      if (!fs.isEmpty() && fs.hasProperty(FlowingFluid.FALLING) && (Boolean)fs.getValue(FlowingFluid.FALLING)) {
         if (fs.getType() == StreamFluids.FLOWING.get()) {
            return 1;
         } else {
            return vanilla && fs.getType() == Fluids.FLOWING_WATER ? 2 : 0;
         }
      } else {
         return 0;
      }
   }

   private static int emit(Minecraft mc, ClientLevel level, int fx, int fy, int fz, int h, float scale, double rate, int budget) {
      int video = ((ParticleStatus)mc.options.particles().get()).ordinal();
      if (video >= 2) {
         return 0;
      }

      boolean thin = video == 1;
      double baseX = fx + 0.5;
      double baseZ = fz + 0.5;
      int count = 0;
      int spray = Mth.clamp(4 + h * 2, 4, 16);
      double want = spray * rate;
      int sprayNow = (int)want;
      if (RAND.nextDouble() < want - sprayNow) {
         sprayNow++;
      }

      sprayNow = Math.min(sprayNow, budget);
      double power = 0.18 + h * 0.04;

      for (int i = 0; i < sprayNow; i++) {
         if (!thin || RAND.nextInt(3) != 0) {
            double ang = RAND.nextDouble() * (Math.PI * 2);
            double spd = power * (0.4 + RAND.nextDouble() * 0.8);
            double ox = Math.cos(ang);
            double oz = Math.sin(ang);
            mc.particleEngine
               .add(
                  new StreamSplashParticle(
                     level,
                     baseX + ox * 0.25,
                     fy + 0.6 + RAND.nextDouble() * 0.3,
                     baseZ + oz * 0.25,
                     ox * spd,
                     0.1 + RAND.nextDouble() * 0.1,
                     oz * spd,
                     scale * FoamSize.of(RAND.nextFloat(), h),
                     splashSprites
                  )
               );
            count++;
         }
      }

      if (count < budget && (!thin || RAND.nextInt(3) != 0) && RAND.nextDouble() < Math.min(h, 3) / 5.0 * rate) {
         double ox = (RAND.nextDouble() - 0.5) * 0.6;
         double oz = (RAND.nextDouble() - 0.5) * 0.6;
         mc.particleEngine
            .add(
               new StreamSplashParticle(
                  level,
                  baseX + ox,
                  fy + 0.9 + RAND.nextDouble() * 0.2,
                  baseZ + oz,
                  ox * 0.04,
                  0.03 + RAND.nextDouble() * 0.03,
                  oz * 0.04,
                  scale * FoamSize.of(RAND.nextFloat(), h),
                  true,
                  splashSprites
               )
            );
         count++;
      }

      if (count < budget && h >= 2 && RAND.nextDouble() < Math.min(h, 9) / 9.0 * rate) {
         int partners = CascadeBridge.fireAt(level, (int)Math.floor(baseX), fy, (int)Math.floor(baseZ), h, true, scale);
         if (partners > 0) {
            count += partners;
         }

         if (!CascadeBridge.oursAfter(partners) || scale >= 1.3) {
            return count;
         }

         double ox = (RAND.nextDouble() - 0.5) * 0.6;
         double oz = (RAND.nextDouble() - 0.5) * 0.6;
         level.addParticle(ParticleTypes.CLOUD, baseX + ox, fy + 0.9 + RAND.nextDouble() * 0.2, baseZ + oz, ox * 0.15, 0.04 + RAND.nextDouble() * 0.06, oz * 0.15);
         count++;
      }

      return count;
   }
}
