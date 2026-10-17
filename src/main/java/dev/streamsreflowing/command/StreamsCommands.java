package dev.streamsreflowing.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.offload.PlateOffload;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamBiomeBlacklist;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamSnowCleanupFeature;
import dev.streamsreflowing.worldgen.StreamVegetationFeature;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.UnderwaterFloraBlacklist;
import java.io.IOException;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class StreamsCommands {
   private static long regenConfirmExpiry;
   private static int[] regenConfirmWhat;

   private StreamsCommands() {
   }

   private static int runTrace(CommandSourceStack src) {
      ServerLevel level = src.getLevel();
      if (notOverworld(src, level)) {
         return 0;
      }

      Vec3 p = src.getPosition();
      int x = (int)Math.floor(p.x);
      int z = (int)Math.floor(p.z);
      RiverEngine engine = StreamsWorldgen.engine(level);
      StreamPrefetcher.diagnoseColumn(engine, level, x, z);
      StreamPrefetcher.traceColumn(engine, level, x, z);
      StreamPrefetcher.superTrace(engine, level, x, z);
      StreamPrefetcher.flowTrace(engine, level, x, (int)Math.floor(p.y), z);
      probeFlora(src, level, x, z);
      src.sendSuccess(() -> Component.literal("Full column trace for (" + x + ", " + z + ") written to the server log."), false);
      return 1;
   }

   private static int runMap(CommandSourceStack src, int radius) {
      ServerLevel level = src.getLevel();
      if (notOverworld(src, level)) {
         return 0;
      }

      Vec3 p = src.getPosition();
      int x = (int)Math.floor(p.x);
      int z = (int)Math.floor(p.z);
      StreamPrefetcher.segmentMap(StreamsWorldgen.engine(level), level, x, z, radius);
      RiverCarverFeature.waterMap(level, BlockPos.containing(p.x, p.y, p.z), Math.max(1, radius / 16));
      StreamSnowCleanupFeature.tintMap(level, x, z);
      RiverFlowFields.flowMap(x, z, Math.max(8, radius / 4));
      RiverFlowFields.deepMap(x, z, Math.max(8, radius / 4));
      StreamVegetationFeature.bankMap(level, x, z);
      src.sendSuccess(
         () -> Component.literal(
            "Segment + water-attribution + tint + river-current maps for (" + x + ", " + z + ") radius " + radius + " written to the server log."
         ),
         false
      );
      return 1;
   }

   private static int runRegen(CommandSourceStack src, int radiusChunks) {
      ServerLevel level = src.getLevel();
      if (notOverworld(src, level)) {
         return 0;
      }

      Vec3 p = src.getPosition();
      int x = (int)Math.floor(p.x);
      int z = (int)Math.floor(p.z);
      int[] what = new int[]{x >> 4, z >> 4, radiusChunks};
      boolean confirmed = regenConfirmWhat != null && Arrays.equals(regenConfirmWhat, what) && System.nanoTime() < regenConfirmExpiry;
      if (!confirmed) {
         regenConfirmWhat = what;
         regenConfirmExpiry = System.nanoTime() + 30000000000L;
         int d0 = radiusChunks * 2 + 1;
         src.sendSuccess(
            () -> Component.literal(
                  "This will PERMANENTLY regenerate the "
                     + d0
                     + "x"
                     + d0
                     + " chunks around ("
                     + x
                     + ", "
                     + z
                     + ") at next world open -- everything in them, including anything built, is discarded."
               )
               .withStyle(ChatFormatting.RED)
               .append(Component.literal("\nRun the same command again within 30 seconds to confirm.").withStyle(ChatFormatting.GOLD)),
            false
         );
         return 1;
      }

      regenConfirmWhat = null;

      try {
         RegenPending.mark(level, x, z, radiusChunks);
      } catch (IOException e) {
         src.sendFailure(Component.literal("Could not record the regen mark: " + e.getMessage()));
         return 0;
      }

      int d = radiusChunks * 2 + 1;
      src.sendSuccess(
         () -> Component.literal("Marked the " + d + "x" + d + " chunks around (" + x + ", " + z + ") for regeneration.")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("\nQuit and reopen the world").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}))
            .append(
               Component.literal(
                     " -- the wipe happens at world open. Then return here: the area regenerates fresh, with the full carve trace written to the log."
                  )
                  .withStyle(ChatFormatting.GRAY)
            ),
         false
      );
      return 1;
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "streams"
                              )
                              .requires(src -> src.hasPermission(2)))
                           .then(Commands.literal("trace").executes(ctx -> runTrace((CommandSourceStack)ctx.getSource()))))
                        .then(
                           Commands.literal("offload")
                              .then(
                                 Commands.literal("test")
                                    .executes(
                                       ctx -> {
                                          CommandSourceStack src = (CommandSourceStack)ctx.getSource();
                                          Vec3 at = src.getPosition();
                                          src.sendSuccess(
                                             () -> Component.literal(
                                                "Copying this world the way a player's game would and preparing the region here on the copy..."
                                             ),
                                             false
                                          );
                                          PlateOffload.selfTest(
                                             src.getLevel(),
                                             Mth.floor(at.x),
                                             Mth.floor(at.z),
                                             line -> src.sendSuccess(() -> Component.literal(line), false)
                                          );
                                          return 1;
                                       }
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("map").executes(ctx -> runMap((CommandSourceStack)ctx.getSource(), 48)))
                           .then(
                              Commands.argument("radius", IntegerArgumentType.integer(8, 160))
                                 .executes(ctx -> runMap((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("regen").executes(ctx -> runRegen((CommandSourceStack)ctx.getSource(), 1)))
                        .then(
                           Commands.argument("radius", IntegerArgumentType.integer(1, 6))
                              .executes(ctx -> runRegen((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))
                        )
                  ))
               .then(Commands.literal("client").executes(ctx -> {
                  CommandSourceStack src = (CommandSourceStack)ctx.getSource();

                  try {
                     Object s = Class.forName("dev.streamsreflowing.client.StreamAudio").getMethod("debugSummary").invoke(null);

                     for (String line : String.valueOf(s).split("\n")) {
                        src.sendSuccess(() -> Component.literal(line), false);
                     }

                     Class.forName("dev.streamsreflowing.client.ClientTintDebug").getMethod("run").invoke(null);
                     src.sendSuccess(() -> Component.literal("Client tint map written to the log."), false);
                  } catch (Throwable e) {
                     src.sendSuccess(() -> Component.literal("client diagnostics need the integrated client (" + e.getClass().getSimpleName() + ")."), false);
                  }

                  return 1;
               })))
            .then(
               Commands.literal("census")
                  .then(
                     Commands.argument("radius", IntegerArgumentType.integer(1, 8192))
                        .executes(
                           ctx -> {
                              CommandSourceStack src = (CommandSourceStack)ctx.getSource();
                              ServerLevel level = src.getLevel();
                              if (notOverworld(src, level)) {
                                 return 0;
                              } else {
                                 Vec3 p = src.getPosition();
                                 int cx = (int)Math.floor(p.x);
                                 int cz = (int)Math.floor(p.z);
                                 int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                 boolean started = StreamPrefetcher.startCensus(StreamsWorldgen.engine(level), level, cx, cz, radius);
                                 if (!started) {
                                    src.sendFailure(Component.literal("A perch census is already running."));
                                    return 0;
                                 } else {
                                    src.sendSuccess(
                                       () -> Component.literal(
                                          "Perch census started ("
                                             + radius * 2
                                             + "x"
                                             + radius * 2
                                             + " blocks around "
                                             + cx
                                             + ", "
                                             + cz
                                             + "); results go to the server log."
                                       ),
                                       false
                                    );
                                    return 1;
                                 }
                              }
                           }
                        )
                  )
            )
      );
   }

   private static boolean notOverworld(CommandSourceStack src, ServerLevel level) {
      if (level.dimension().equals(Level.OVERWORLD)) {
         return false;
      }

      src.sendFailure(Component.literal("Streams only generate in the overworld."));
      return true;
   }

   private static void probeFlora(CommandSourceStack src, ServerLevel level, int x, int z) {
      RiverEngine engine = StreamsWorldgen.engine(level);
      ColumnPlan plan = engine.plan(x, z);
      boolean inChannel = plan != ColumnPlan.NONE && plan.inChannel();
      int ws = plan == ColumnPlan.NONE ? Integer.MIN_VALUE : plan.waterSurfaceY();
      int floorY = level.getHeight(Types.OCEAN_FLOOR, x, z) - 1;
      BlockPos floorPos = new BlockPos(x, floorY, z);
      BlockState floor = level.getBlockState(floorPos);
      BlockState above = level.getBlockState(floorPos.above());
      Holder<Biome> biome = level.getBiome(floorPos);
      int floraLevel = Services.PLATFORM.underwaterFlora();
      int percent = Services.PLATFORM.underwaterFloraPercent();
      double temp = ((Biome)biome.value()).getBaseTemperature();
      double wet = Services.PLATFORM.biomeWetness(biome);
      double cold = Mth.clamp((temp - 0.15) / 0.35, 0.0, 1.0);
      double dry = Mth.clamp((wet - 0.1) / 0.3, 0.0, 1.0);
      int eff = (int)Math.round(percent * cold * dry);
      boolean inTag = floor.is(StreamsWorldgen.UNDERWATER_VEGETATION_FLOOR);
      boolean waterAbove = above.is(Blocks.WATER);
      boolean submerged = ws != Integer.MIN_VALUE && floorY < ws;
      boolean streamBlocked = StreamBiomeBlacklist.cutsAtCarve(engine) && StreamBiomeBlacklist.isBlocked(biome);
      boolean floraBlocked = UnderwaterFloraBlacklist.isBlocked(biome);
      line(src, "== flora probe @ " + x + ", " + floorY + ", " + z + " ==", ChatFormatting.AQUA);
      line(
         src,
         "underwaterFlora level = " + floraLevel + " (need >= 2 for streams, 3 for all water) | percent = " + percent,
         floraLevel >= 2 ? ChatFormatting.GRAY : ChatFormatting.RED
      );
      line(
         src,
         "in stream channel: " + inChannel + (ws != Integer.MIN_VALUE ? " (waterSurfaceY=" + ws + ")" : ""),
         inChannel ? ChatFormatting.GREEN : ChatFormatting.YELLOW
      );
      line(
         src,
         "submerged (floorY " + floorY + " < waterSurfaceY " + (ws == Integer.MIN_VALUE ? "n/a" : ws) + "): " + submerged,
         submerged ? ChatFormatting.GREEN : ChatFormatting.RED
      );
      line(src, "floor block: " + key(floor) + "  | in underwater_vegetation_floor: " + inTag, inTag ? ChatFormatting.GREEN : ChatFormatting.RED);
      line(src, "block above floor: " + key(above) + "  | is minecraft:water: " + waterAbove, waterAbove ? ChatFormatting.GREEN : ChatFormatting.RED);
      line(src, "biome: " + biome.unwrapKey().map(k -> k.location().toString()).orElse("?"), ChatFormatting.GRAY);
      line(
         src,
         String.format("temp=%.2f -> cold=%.2f | downfall=%.2f -> dry=%.2f  => eff=%d%%", temp, cold, wet, dry, eff),
         eff > 0 ? ChatFormatting.GREEN : ChatFormatting.RED
      );
      if (streamBlocked) {
         line(src, "biome is in the STREAM blacklist -> no channel carved here", ChatFormatting.RED);
      }

      if (floraBlocked) {
         line(src, "biome is in the UNDERWATER-FLORA blacklist (#no_underwater_flora) -> opted out", ChatFormatting.RED);
      }

      boolean ok = floraLevel >= 2 && inChannel && submerged && !streamBlocked && inTag && !floraBlocked && eff > 0 && waterAbove;
      line(
         src,
         ok ? "VERDICT: would plant here (subject to the " + eff + "% roll)" : "VERDICT: BLOCKED -> fix the red gate(s) above",
         ok ? ChatFormatting.GREEN : ChatFormatting.RED
      );
   }

   private static String key(BlockState state) {
      return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
   }

   private static void line(CommandSourceStack src, String text, ChatFormatting color) {
      Constants.LOG.info("[SR-FLORA] {}", text);
   }

   public static Component welcomeMessage() {
      return Component.literal("\n")
         .append(Component.literal("Streams Reflowing").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}))
         .append(Component.literal("\n"))
         .append(
            Component.literal(
                  "Rivers and streams are now weaving across your world. Chunks may load a little slowly for the first few minutes, and when you explore quickly into fresh regions."
               )
               .withStyle(ChatFormatting.GRAY)
         )
         .append(Component.literal("\n"))
         .append(Component.literal("It also ships a ").withStyle(ChatFormatting.GRAY))
         .append(Component.literal("very comprehensive config").withStyle(ChatFormatting.AQUA))
         .append(
            Component.literal(
                  ": detailed performance and quality options, plus a toggle for every single feature in the mod. Open it to tailor Streams Reflowing to your world and hardware."
               )
               .withStyle(ChatFormatting.GRAY)
         );
   }

   public static boolean shouldWelcome(ServerPlayer player) {
      if (!Services.PLATFORM.welcomeMessage()) {
         return false;
      }

      ServerLevel overworld = player.getServer().overworld();
      WelcomedPlayers data = (WelcomedPlayers)overworld.getDataStorage().computeIfAbsent(WelcomedPlayers.loader(), WelcomedPlayers::create, "streamsreflowing_welcomed");
      return data.markWelcomed();
   }
}
