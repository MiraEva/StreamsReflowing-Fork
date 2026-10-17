package dev.streamsreflowing.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.config.WatershedSize;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigMigration {
   private static final String[][] COMMON = new String[][]{
      {"generateStreams", "planning.generateStreams"},
      {"generateLakes", "planning.generateLakes"},
      {"performance.terrainAccuracyLevel", "planning.terrainAccuracyLevel"},
      {"appearance.riverDrainageSize", "planning.riverDrainageSize"},
      {"performance.terrainFit", "planning.terrainFit"},
      {"performance.legacyStreamShaping", "planning.legacyStreamShaping"},
      {"performance.gpuAcceleration", "planning.gpuAcceleration"},
      {"water.fastChunkLoading", "planning.fastChunkLoading"},
      {"appearance.streamBiomeBlacklist", "planning.where.streamBiomeBlacklist"},
      {"water.outletBiomes", "planning.where.outletBiomes"},
      {"water.reterraforgedCompat", "planning.where.reterraforgedCompat"},
      {"experimental.connectNearbyWater", "planning.where.connectNearbyWater"},
      {"experimental.connectRadius", "planning.where.connectRadius"},
      {"appearance.wetnessMultiplier", "appearance.network.wetnessMultiplier"},
      {"appearance.wetnessDryFloor", "appearance.network.wetnessDryFloor"},
      {"appearance.growDensity", "appearance.network.growDensity"},
      {"appearance.minStreamLength", "appearance.network.minStreamLength"},
      {"appearance.minLengthScope", "appearance.network.minLengthScope"},
      {"appearance.lakeMinArea", "appearance.network.lakeMinArea"},
      {"appearance.lakeMaxArea", "appearance.network.lakeMaxArea"},
      {"appearance.widthScale", "appearance.channel.widthScale"},
      {"appearance.baseWidth", "appearance.channel.baseWidth"},
      {"appearance.maxWidth", "appearance.channel.maxWidth"},
      {"appearance.meanderStrength", "appearance.channel.meanderStrength"},
      {"appearance.streamDepth", "appearance.channel.streamDepth"},
      {"appearance.widthFollowsSlope", "appearance.channel.widthFollowsSlope"},
      {"appearance.widthFollowsClimate", "appearance.channel.widthFollowsClimate"},
      {"appearance.widthVariation", "appearance.channel.widthVariation"},
      {"appearance.depthFollowsTerrain", "appearance.channel.depthFollowsTerrain"},
      {"appearance.bendAsymmetry", "appearance.channel.bendAsymmetry"},
      {"appearance.channelBars", "appearance.channel.channelBars"},
      {"appearance.deltaMouths", "appearance.channel.deltaMouths"},
      {"appearance.bankNoise", "appearance.channel.bankNoise"},
      {"appearance.containmentBerm", "appearance.channel.containmentBerm"},
      {"appearance.customBanks", "appearance.banks.customBanks"},
      {"appearance.allWaterBanks", "appearance.banks.allWaterBanks"},
      {"appearance.bankFeatures", "appearance.banks.bankFeatures"},
      {"appearance.vegetation", "appearance.banks.vegetation"},
      {"appearance.allWaterFlora", "appearance.banks.allWaterFlora"},
      {"appearance.floraRadius", "appearance.banks.floraRadius"},
      {"appearance.bankGrassPercent", "appearance.banks.bankGrassPercent"},
      {"appearance.biomeBoostPercent", "appearance.banks.biomeBoostPercent"},
      {"appearance.biomeBoostBlacklist", "appearance.banks.biomeBoostBlacklist"},
      {"appearance.underwaterFlora", "appearance.banks.underwaterFlora"},
      {"appearance.underwaterFloraPercent", "appearance.banks.underwaterFloraPercent"},
      {"appearance.underwaterFloraBiomeBlacklist", "appearance.banks.underwaterFloraBiomeBlacklist"},
      {"rebiomeStreams", "appearance.biomes.rebiomeStreams"},
      {"rebiomeLakes", "appearance.biomes.rebiomeLakes"},
      {"water.vanillaWaterFlow", "behavior.vanillaWaterFlow"},
      {"water.flowVanillaRivers", "behavior.flowVanillaRivers"},
      {"water.riverSeamAgreement", "behavior.riverSeamAgreement"},
      {"water.spillOverWater", "behavior.spillOverWater"},
      {"water.spillOnStreamsOnly", "behavior.spillOnStreamsOnly"},
      {"water.boatsFloatOnFlowingWater", "behavior.boatsFloatOnFlowingWater"},
      {"water.waterMobsIgnoreFlow", "behavior.waterMobsIgnoreFlow"},
      {"appearance.mobsHoldInCurrent", "behavior.mobsHoldInCurrent"},
      {"appearance.mobsDriftInCurrent", "behavior.mobsDriftInCurrent"},
      {"water.itemDriftSpeed", "behavior.itemDriftSpeed"},
      {"water.physicsModWater", "behavior.otherMods.physicsModWater"},
      {"water.bakedFluidWhenFlowingFluids", "behavior.otherMods.bakedFluidWhenFlowingFluids"},
      {"water.forceBakedFluid", "behavior.otherMods.forceBakedFluid"},
      {"water.serverStreamParticles", "effects.serverStreamParticles"},
      {"welcomeMessage", "misc.welcomeMessage"}
   };
   private static final String[][] CLIENT = new String[][]{
      {"waterAmbience", "sound.waterAmbience"},
      {"streamBubbles", "particles.streamBubbles"},
      {"streamDebris", "particles.streamDebris"},
      {"particleSize", "particles.particleSize"},
      {"particleDensity", "particles.particleDensity"},
      {"maxStreamParticlesPerTick", "particles.maxStreamParticlesPerTick"},
      {"streamParticleScanRadius", "particles.streamParticleScanRadius"},
      {"particlesOnVanillaWater", "particles.particlesOnVanillaWater"},
      {"loadingNotice", "misc.loadingNotice"}
   };

   private ConfigMigration() {
   }

   public static void migrate(Path configDir) {
      move(configDir.resolve("streamsreflowing-common.toml"), COMMON, true);
      move(configDir.resolve("streamsreflowing-client.toml"), CLIENT, false);
   }

   private static void move(Path file, String[][] moves, boolean common) {
      if (Files.isRegularFile(file)) {
         try {
            CommentedFileConfig cfg = (CommentedFileConfig)CommentedFileConfig.builder(file).sync().build();

            try {
               cfg.load();
               int moved = 0;

               for (String[] mv : moves) {
                  if (cfg.contains(mv[0])) {
                     Object v = cfg.get(mv[0]);
                     cfg.remove(mv[0]);
                     if (!cfg.contains(mv[1])) {
                        cfg.set(mv[1], v);
                        moved++;
                     }
                  }
               }

               if (common) {
                  if (cfg.contains("performance.qualityPreset")) {
                     Object v = cfg.get("performance.qualityPreset");
                     cfg.remove("performance.qualityPreset");
                     if (!cfg.contains("planning.terrainAccuracyLevel") && v != null) {
                        cfg.set("planning.terrainAccuracyLevel", v);
                        moved++;
                     }
                  }

                  boolean hadPlate = cfg.contains("performance.plateSize");
                  Object plate = hadPlate ? cfg.get("performance.plateSize") : null;
                  if (hadPlate) {
                     cfg.remove("performance.plateSize");
                  }

                  if ((hadPlate || moved > 0) && !cfg.contains("planning.watershedSize")) {
                     int n = plate instanceof Number ? ((Number)plate).intValue() : -1;
                     if (n <= 0) {
                        Object level = cfg.get("planning.terrainAccuracyLevel");
                        n = WatershedSize.formerPresetBlocks(level == null ? null : level.toString());
                     }

                     cfg.set("planning.watershedSize", WatershedSize.nearest(n).name());
                     moved++;
                  }

                  for (String table : new String[]{"performance", "appearance", "water", "experimental"}) {
                     if (cfg.get(table) instanceof Config c && c.isEmpty()) {
                        cfg.remove(table);
                     }
                  }
               }

               if (moved > 0) {
                  cfg.save();
                  Constants.LOG.info("Streams Reflowing: moved {} setting(s) in {} to the 2.14 layout", moved, file.getFileName());
               }
            } catch (Throwable var14) {
               if (cfg != null) {
                  try {
                     cfg.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }
               }

               throw var14;
            }

            if (cfg != null) {
               cfg.close();
            }
         } catch (Exception e) {
            Constants.LOG.warn("Streams Reflowing: could not move the settings in {} to the 2.14 layout; the file will be corrected to defaults", file, e);
         }
      }
   }
}
