package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.flow.FlowTrace;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class HarnessPilot {
   private static final boolean ON = "fly".equals(System.getProperty("streamsreflowing.harness"));
   private static final boolean TINT = "tint".equals(System.getProperty("streamsreflowing.harness"));
   private static final double SPEED = Integer.getInteger("streamsreflowing.harnessspeed", 13).intValue() / 20.0;
   private static final int SETTLE_TICKS = 200;
   private static final int REPORT_TICKS = 100;
   private static int ticksInWorld;
   private static double cruiseY = Double.NaN;
   private static int wallTicks;
   private static boolean inWorldLogged;
   private static final int TINT_SETTLE = 300;
   private static final int TINT_HOLD = 300;
   private static final int TINT_REACH = 160;
   private static final int TINT_WINDOW = 12;
   private static int tintTicks;
   private static int tintStage;
   private static float tintYaw;
   private static float tintPitch;
   private static double tintCruiseY = Double.NaN;
   private static double tintHoldX;
   private static double tintHoldY;
   private static double tintHoldZ;
   private static BlockPos tintWater;

   private HarnessPilot() {
   }

   public static boolean active() {
      return ON || TINT;
   }

   public static void tick(Minecraft mc) {
      if (TINT) {
         tintTick(mc);
      } else if (ON) {
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            ticksInWorld++;
            if (!inWorldLogged && mc.screen == null) {
               inWorldLogged = true;
               Constants.LOG.info("[SR-HARNESS] in-world (drop-in screen closed)");
            }

            if (ticksInWorld >= 200) {
               if (Double.isNaN(cruiseY)) {
                  if (player.getAbilities().mayfly && !player.getAbilities().flying) {
                     player.getAbilities().flying = true;
                     player.onUpdateAbilities();
                  }

                  cruiseY = player.getY() + Integer.getInteger("streamsreflowing.harnessclimb", 135).intValue();
                  Constants.LOG
                     .info("[SR-HARNESS] takeoff at x={} z={} cruiseY={}", new Object[]{(int)player.getX(), (int)player.getZ(), (int)cruiseY});
               }

               if (player.getY() < cruiseY) {
                  player.setPos(player.getX(), Math.min(cruiseY, player.getY() + 0.375), player.getZ());
               } else if (mc.level.hasChunk(((int)Math.floor(player.getX() + SPEED) >> 4) + 1, (int)Math.floor(player.getZ()) >> 4)) {
                  player.setPos(player.getX() + SPEED, cruiseY, player.getZ());
               } else {
                  wallTicks++;
               }

               player.setDeltaMovement(0.0, 0.0, 0.0);
               player.fallDistance = 0.0F;
               if (ticksInWorld % 100 == 0) {
                  Constants.LOG
                     .info(
                        "[SR-HARNESS] t={} x={} carves={} stalls={} wall={}",
                        new Object[]{
                           (ticksInWorld - 200) / 20, (int)player.getX(), FlowTrace.carvedChunks.get(), FlowTrace.harnessStalls.get(), wallTicks / 20
                        }
                     );
               }
            }
         } else {
            if (mc.screen instanceof ConfirmScreen cs) {
               for (GuiEventListener child : cs.children()) {
                  if (child instanceof Button b) {
                     String label = b.getMessage().getString();
                     if (label.equalsIgnoreCase("Proceed") || label.equalsIgnoreCase("Yes") || label.equalsIgnoreCase("Create Backup and Load")) {
                        Constants.LOG.info("[SR-HARNESS] auto-confirmed '{}'", label);
                        b.onPress();
                        break;
                     }
                  }
               }
            }

            ticksInWorld = 0;
            cruiseY = Double.NaN;
            inWorldLogged = false;
         }
      }
   }

   private static void tintTick(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) {
         tintAutoConfirm(mc);
         tintTicks = 0;
         tintStage = 0;
         tintWater = null;
         tintCruiseY = Double.NaN;
      } else if (tintStage < 2) {
         tintTicks++;
         if (tintStage != 0) {
            player.setPos(tintHoldX, tintHoldY, tintHoldZ);
            player.setDeltaMovement(0.0, 0.0, 0.0);
            player.fallDistance = 0.0F;
            player.setYRot(tintYaw);
            player.setXRot(tintPitch);
            player.setYHeadRot(tintYaw);
            if (tintTicks >= 300) {
               tintRecord(mc);
               tintStage = 2;
            }
         } else if (tintTicks >= 300) {
            if (tintTicks % 20 == 0) {
               tintWater = tintFindFrontier(mc);
            }

            if (tintWater == null) {
               tintCruise(mc, player);
               if (tintTicks % 200 == 0) {
                  Constants.LOG
                     .info(
                        "[SR-TINTPROBE] searching, t={} at {} {} {}",
                        new Object[]{tintTicks / 20, (int)player.getX(), (int)player.getY(), (int)player.getZ()}
                     );
               }
            } else {
               tintHoldX = tintWater.getX() + 9.5;
               tintHoldY = tintWater.getY() + 9.0;
               tintHoldZ = tintWater.getZ() + 9.5;
               player.setPos(tintHoldX, tintHoldY, tintHoldZ);
               tintAim(mc, player);
               tintStage = 1;
               tintTicks = 0;
            }
         }
      }
   }

   private static void tintCruise(Minecraft mc, LocalPlayer player) {
      if (Double.isNaN(tintCruiseY)) {
         player.connection.sendCommand("gamemode creative");
         tintCruiseY = player.getY() + Integer.getInteger("streamsreflowing.harnessclimb", 45).intValue();
         Constants.LOG.info("[SR-TINTPROBE] takeoff at {} {} cruiseY={}", new Object[]{(int)player.getX(), (int)player.getZ(), (int)tintCruiseY});
      }

      if (player.getAbilities().mayfly && !player.getAbilities().flying) {
         player.getAbilities().flying = true;
         player.onUpdateAbilities();
      }

      if (player.getY() < tintCruiseY) {
         player.setPos(player.getX(), Math.min(tintCruiseY, player.getY() + 0.375), player.getZ());
      } else if (mc.level.hasChunk(((int)Math.floor(player.getX() + SPEED) >> 4) + 1, (int)Math.floor(player.getZ()) >> 4)) {
         player.setPos(player.getX() + SPEED, tintCruiseY, player.getZ());
      }

      player.setDeltaMovement(0.0, 0.0, 0.0);
      player.fallDistance = 0.0F;
   }

   private static BlockPos tintFindFrontier(Minecraft mc) {
      ClientLevel level = mc.level;
      Biome ours = level.registryAccess().registry(Registries.BIOME).flatMap(r -> r.getOptional(StreamTint.STREAM_BIOME)).orElse(null);
      if (ours == null) {
         Constants.LOG.info("[SR-TINTPROBE] no stream biome registered on this world");
         return null;
      }

      int px = (int)Math.floor(mc.player.getX());
      int pz = (int)Math.floor(mc.player.getZ());
      MutableBlockPos p = new MutableBlockPos();

      for (int ring = 1; ring <= 160; ring++) {
         for (int dx = -ring; dx <= ring; dx++) {
            for (int dz = -ring; dz <= ring; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                  int x = px + dx;
                  int z = pz + dz;
                  if (tintIsStreamWater(level, ours, p, x, z)) {
                     for (Direction d : Plane.HORIZONTAL) {
                        for (int k = 1; k <= 3; k++) {
                           int lx = x + d.getStepX() * k;
                           int lz = z + d.getStepZ() * k;
                           if (level.hasChunk(lx >> 4, lz >> 4)) {
                              int ly = level.getHeight(Types.MOTION_BLOCKING, lx, lz) - 1;
                              p.set(lx, ly, lz);
                              if (level.getBlockState(p).getFluidState().isEmpty()) {
                                 return new BlockPos(x, level.getHeight(Types.MOTION_BLOCKING, x, z) - 1, z);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private static boolean tintIsStreamWater(ClientLevel level, Biome ours, MutableBlockPos p, int x, int z) {
      if (!level.hasChunk(x >> 4, z >> 4)) {
         return false;
      }

      int y = level.getHeight(Types.MOTION_BLOCKING, x, z) - 1;
      p.set(x, y, z);
      return level.getBlockState(p).getFluidState().is(FluidTags.WATER) && level.getBiome(p).value() == ours;
   }

   private static void tintAim(Minecraft mc, LocalPlayer player) {
      double dx = tintWater.getX() + 0.5 - player.getX();
      double dy = tintWater.getY() + 0.5 - player.getEyeY();
      double dz = tintWater.getZ() + 0.5 - player.getZ();
      double flat = Math.sqrt(dx * dx + dz * dz);
      tintYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      tintPitch = (float)(-Math.toDegrees(Math.atan2(dy, flat)));
      Constants.LOG
         .info(
            "[SR-TINTPROBE] frontier at {} {} {}, {} blocks from the player at {} {} {}; yaw={} pitch={}",
            new Object[]{
               tintWater.getX(),
               tintWater.getY(),
               tintWater.getZ(),
               (int)flat,
               (int)player.getX(),
               (int)player.getY(),
               (int)player.getZ(),
               (int)tintYaw,
               (int)tintPitch
            }
         );
   }

   private static void tintRecord(Minecraft mc) {
      ClientLevel level = mc.level;
      MutableBlockPos p = new MutableBlockPos();
      LinkedHashMap<Integer, Character> legend = new LinkedHashMap<>();
      List<String> rows = new ArrayList<>();

      for (int dz = -12; dz <= 12; dz++) {
         StringBuilder row = new StringBuilder();

         for (int dx = -12; dx <= 12; dx++) {
            int x = tintWater.getX() + dx;
            int z = tintWater.getZ() + dz;
            if (!level.hasChunk(x >> 4, z >> 4)) {
               row.append('?');
            } else {
               int y = level.getHeight(Types.MOTION_BLOCKING, x, z) - 1;
               p.set(x, y, z);
               if (!level.getBlockState(p).getFluidState().is(FluidTags.WATER)) {
                  row.append('.');
               } else {
                  int colour = BiomeColors.getAverageWaterColor(level, p);
                  Character c = legend.get(colour);
                  if (c == null) {
                     c = (char)(97 + legend.size() % 26);
                     legend.put(colour, c);
                  }

                  row.append(c);
               }
            }
         }

         rows.add(row.toString());
      }

      Constants.LOG
         .info(
            "[SR-TINTPROBE] map centred on {} {} {} ({} colours)",
            new Object[]{tintWater.getX(), tintWater.getY(), tintWater.getZ(), legend.size()}
         );

      for (String row : rows) {
         Constants.LOG.info("[SR-TINTPROBE]   {}", row);
      }

      for (Entry<Integer, Character> e : legend.entrySet()) {
         Constants.LOG.info("[SR-TINTPROBE] legend {} = #{}", e.getValue(), String.format("%06x", e.getKey() & 16777215));
      }

      try {
         Screenshot.grab(
            mc.gameDirectory,
            "sr-tintprobe-" + System.getProperty("streamsreflowing.harnesslabel", "run") + ".png",
            mc.getMainRenderTarget(),
            msg -> Constants.LOG.info("[SR-TINTPROBE] {}", msg.getString())
         );
      } catch (RuntimeException e) {
         Constants.LOG.info("[SR-TINTPROBE] screenshot failed: {}", e.toString());
      }

      Constants.LOG.info("[SR-TINTPROBE] done");
   }

   private static void tintAutoConfirm(Minecraft mc) {
      if (mc.screen instanceof ConfirmScreen cs) {
         for (GuiEventListener child : cs.children()) {
            if (child instanceof Button b) {
               String label = b.getMessage().getString();
               if (label.equalsIgnoreCase("Proceed") || label.equalsIgnoreCase("Yes") || label.equalsIgnoreCase("Create Backup and Load")) {
                  Constants.LOG.info("[SR-TINTPROBE] auto-confirmed '{}'", label);
                  b.onPress();
                  break;
               }
            }
         }
      }
   }
}
