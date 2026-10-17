package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.RainCount;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class RainDiag {
   public static final boolean ON = RainCount.ON;
   private static final int PHASE_SECS = Integer.getInteger("streamsreflowing.raindiag.phase", 30);
   private static final int SETTLE_SECS = Integer.getInteger("streamsreflowing.raindiag.settle", 8);
   private static final int WARMUP_SECS = Integer.getInteger("streamsreflowing.raindiag.warmup", 90);
   private static final int CYCLES = Integer.getInteger("streamsreflowing.raindiag.cycles", 2);
   private static final String TP_RIVER = System.getProperty("streamsreflowing.raindiag.tp", "");
   private static final String TP_AWAY = System.getProperty("streamsreflowing.raindiag.tp2", "");
   private static final int PROFILE_PHASE = Integer.getInteger("streamsreflowing.raindiag.profilephase", -1);
   private static final boolean SURVEY = Boolean.getBoolean("streamsreflowing.raindiag.survey");
   private static final int SEEK_FALL = Integer.getInteger("streamsreflowing.raindiag.seekfall", 0);
   private static final int SEEK_DWELL = Integer.getInteger("streamsreflowing.raindiag.seekdwell", 10);
   private static final int SEEK_STEP = Integer.getInteger("streamsreflowing.raindiag.seekstep", 96);
   private static final int SEEK_TRIES = Integer.getInteger("streamsreflowing.raindiag.seektries", 12);
   private static String station = "";
   private static int seekTries;
   private static int stationTick;
   private static int ticksInWorld;
   private static int phase = -1;
   private static int phaseTick;
   private static long fpsSum;
   private static int fpsSamples;
   private static int minFps = Integer.MAX_VALUE;
   private static int maxFps;
   private static boolean profiling;
   private static final int PHASES = 8;

   private RainDiag() {
   }

   public static void tick(Minecraft mc) {
      if (ON) {
         RainCount.render = Thread.currentThread();
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            ticksInWorld++;
            unpause(mc);
            int fps = fps(mc);
            if (fps > 0) {
               fpsSum += fps;
               fpsSamples++;
               minFps = Math.min(minFps, fps);
               maxFps = Math.max(maxFps, fps);
            }

            if (SURVEY) {
               if (ticksInWorld % 100 == 0) {
                  Constants.LOG
                     .info(
                        "[SR-RC] survey t={}s pos={} {} {} fps={} live={} paused={} feet={} tallest={}@{}",
                        new Object[]{
                           ticksInWorld / 20,
                           (int)player.getX(),
                           (int)player.getY(),
                           (int)player.getZ(),
                           fps,
                           particleCount(mc),
                           mc.isPaused(),
                           RainCount.footCount,
                           RainCount.bestFootHeight,
                           show(RainCount.bestFoot)
                        }
                     );
                  resetWindow();
               }
            } else {
               boolean warming = SEEK_FALL > 0 ? station.isEmpty() || ticksInWorld < stationTick + SETTLE_SECS * 40 : ticksInWorld < WARMUP_SECS * 20;
               if (warming) {
                  if (ticksInWorld == 40) {
                     command(mc, "gamerule doWeatherCycle false");
                     command(mc, "gamerule doDaylightCycle false");
                     command(mc, "gamerule doMobSpawning false");
                     command(mc, "time set noon");
                     command(mc, "gamemode spectator");
                     command(mc, "weather clear");
                     tp(mc, TP_RIVER);
                  }

                  if (SEEK_FALL > 0) {
                     seek(mc);
                  } else if (ticksInWorld % 200 == 0) {
                     tp(mc, TP_RIVER);
                  }
               } else {
                  int total = 8 * CYCLES;
                  if (phase < total) {
                     if (phase < 0) {
                        startPhase(mc, 0);
                     } else {
                        phaseTick++;
                        if (phaseTick == SETTLE_SECS * 20) {
                           resetWindow();
                        }

                        if (phaseTick == 20 && phase == PROFILE_PHASE) {
                           profiling = startProfiler(mc);
                           Constants.LOG.info("[SR-RC] client profiler started for phase {}: {}", phase, profiling);
                        }

                        if (phaseTick >= PHASE_SECS * 20) {
                           endPhase(mc);
                           startPhase(mc, phase + 1);
                        }
                     }
                  }
               }
            }
         } else {
            ticksInWorld = 0;
            phase = -1;
         }
      }
   }

   private static void startPhase(Minecraft mc, int p) {
      phase = p;
      phaseTick = 0;
      if (p >= 8 * CYCLES) {
         Constants.LOG.info("[SR-RC] schedule complete after {} phases", p);
      } else {
         int step = p % 8;
         RainCount.particlesOff = !oursOn(step);
         RainCount.bridgeOff = !bridgeOn(step);
         CascadeBudget.off = !capped(step);
         tp(mc, station.isEmpty() ? TP_RIVER : station);
         command(mc, rainy(step) ? "weather rain 1000000" : "weather clear");
         resetWindow();
         Constants.LOG.info("[SR-RC] --- phase {} start: {} ---", p, label(step));
      }
   }

   private static void endPhase(Minecraft mc) {
      int step = phase % 8;
      double secs = Math.max(1, PHASE_SECS - SETTLE_SECS);
      double ticks = secs * 20.0;
      long frames = Math.max(1, fpsSamples);
      Constants.LOG
         .info(
            "[SR-RC] phase={} {} fps={} min={} max={} | spawn/s={} bridge/s={} bridgePart/s={} bridgePart/tick={} bridgeScan/s={} bridgeTick/s={} scanMs/s={} feet={} live={} paused={}",
            new Object[]{
               phase,
               label(step),
               fpsSamples == 0 ? -1L : fpsSum / frames,
               minFps == Integer.MAX_VALUE ? -1 : minFps,
               maxFps,
               (long)(RainCount.spawned / secs),
               (long)((RainCount.bridgeScanCalls + RainCount.bridgeTickCalls) / secs),
               (long)(RainCount.bridgeParticles / secs),
               String.format("%.1f", RainCount.bridgeParticles / ticks),
               (long)(RainCount.bridgeScanCalls / secs),
               (long)(RainCount.bridgeTickCalls / secs),
               String.format("%.2f", RainCount.scanNanos / 1000000.0 / secs),
               RainCount.footCount,
               particleCount(mc),
               mc.isPaused()
            }
         );
      if (profiling) {
         stopProfiler(mc);
         profiling = false;
      }
   }

   private static boolean rainy(int step) {
      return step == 1 || step == 2 || step == 4 || step == 6;
   }

   private static boolean bridgeOn(int step) {
      return step <= 3;
   }

   private static boolean capped(int step) {
      return step >= 2;
   }

   private static boolean oursOn(int step) {
      return step <= 5;
   }

   private static String label(int step) {
      String sky = rainy(step) ? "RAIN" : "CLEAR";
      if (!oursOn(step)) {
         return sky + "/all-OURS-OFF";
      } else {
         return !bridgeOn(step) ? sky + "/partners-ABSENT" : sky + "/partners-ON/" + (capped(step) ? "budgeted" : "uncapped");
      }
   }

   private static void seek(Minecraft mc) {
      if (!station.isEmpty()) {
         if (ticksInWorld % 200 == 0) {
            tp(mc, station);
         }
      } else {
         int h = RainCount.bestFootHeight;
         if (h >= SEEK_FALL) {
            long packed = RainCount.bestFoot;
            int fx = BlockPos.getX(packed);
            int fy = BlockPos.getY(packed);
            int fz = BlockPos.getZ(packed);
            station = fx + 6 + " " + (fy + 3) + " " + (fz + 6) + " 135 20";
            stationTick = ticksInWorld;
            Constants.LOG.info("[SR-RC] station taken at a {}-block fall {} {} {} -> standing at {}", new Object[]{h, fx, fy, fz, station});
            tp(mc, station);
         } else if (ticksInWorld % (SEEK_DWELL * 20) == 0) {
            if (seekTries >= SEEK_TRIES) {
               if (seekTries == SEEK_TRIES) {
                  Constants.LOG
                     .info("[SR-RC] NO FALL FOUND after {} candidates -- staying at the given station, so the spray numbers below may be empty", seekTries);
                  seekTries++;
               }

               station = TP_RIVER;
               stationTick = ticksInWorld;
            } else {
               seekTries++;
               int ring = (seekTries + 3) / 4;
               int side = seekTries % 4;
               int dx = side == 0 ? ring : (side == 1 ? -ring : 0);
               int dz = side == 2 ? ring : (side == 3 ? -ring : 0);
               String[] at = TP_RIVER.trim().split("[ ,]+");
               if (at.length >= 3) {
                  try {
                     int cx = (int)Double.parseDouble(at[0]) + dx * SEEK_STEP;
                     int cz = (int)Double.parseDouble(at[2]) + dz * SEEK_STEP;
                     Constants.LOG.info("[SR-RC] no fall yet (tallest={}); candidate {} at {} {} {}", new Object[]{h, seekTries, cx, at[1], cz});
                     command(mc, "tp @s " + cx + " " + at[1] + " " + cz);
                  } catch (RuntimeException e) {
                     station = TP_RIVER;
                  }
               } else {
                  station = TP_RIVER;
               }
            }
         }
      }
   }

   private static String show(long packed) {
      return BlockPos.getX(packed) + "," + BlockPos.getY(packed) + "," + BlockPos.getZ(packed);
   }

   private static void unpause(Minecraft mc) {
      try {
         if (Minecraft.class.getField("screen").get(mc) == null) {
            return;
         }

         for (Method m : Minecraft.class.getMethods()) {
            if (m.getParameterCount() == 1 && ("setScreen".equals(m.getName()) || "setScreenAndShow".equals(m.getName()))) {
               m.invoke(mc, (Object) null);
               return;
            }
         }
      } catch (Throwable var5) {
      }
   }

   private static void resetWindow() {
      RainCount.reset();
      fpsSum = 0L;
      fpsSamples = 0;
      minFps = Integer.MAX_VALUE;
      maxFps = 0;
   }

   private static void command(Minecraft mc, String cmd) {
      try {
         if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand(cmd);
         }
      } catch (Throwable t) {
         Constants.LOG.info("[SR-RC] command '{}' failed: {}", cmd, t.toString());
      }
   }

   private static void tp(Minecraft mc, String spec) {
      if (spec != null && !spec.isEmpty()) {
         command(mc, "tp @s " + spec.trim().replace(',', ' '));
      }
   }

   private static int fps(Minecraft mc) {
      try {
         return (Integer)Minecraft.class.getMethod("getFps").invoke(mc);
      } catch (Throwable t) {
         return -1;
      }
   }

   private static String particleCount(Minecraft mc) {
      try {
         Object pe = Minecraft.class.getField("particleEngine").get(mc);
         return String.valueOf(pe.getClass().getMethod("countParticles").invoke(pe));
      } catch (Throwable t) {
         return "?";
      }
   }

   private static boolean startProfiler(Minecraft mc) {
      try {
         return (Boolean)Minecraft.class.getMethod("debugClientMetricsStart", Consumer.class).invoke(mc, (Consumer<Object>)c -> {});
      } catch (Throwable t) {
         Constants.LOG.info("[SR-RC] profiler start failed: {}", t.toString());
         return false;
      }
   }

   private static void stopProfiler(Minecraft mc) {
      try {
         Method m = Minecraft.class.getDeclaredMethod("debugClientMetricsStop");
         m.setAccessible(true);
         m.invoke(mc);
      } catch (Throwable t) {
         Constants.LOG.info("[SR-RC] profiler stop failed (it self-stops after 10s): {}", t.toString());
      }
   }
}
