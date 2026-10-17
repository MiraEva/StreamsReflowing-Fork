package dev.streamsreflowing.platform.services;

import dev.streamsreflowing.core.config.WatershedSize;
import java.nio.file.Path;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public interface IPlatformHelper {
   default Path configDir() {
      return Path.of("config");
   }

   String getPlatformName();

   boolean isModLoaded(String var1);

   default String modVersion(String modId) {
      return null;
   }

   boolean isDevelopmentEnvironment();

   default String terrainAccuracyPreset() {
      return "unknown";
   }

   default boolean generateStreams() {
      return true;
   }

   default boolean offloadRegionBuilds() {
      return false;
   }

   default boolean helpPrepareRegions() {
      return true;
   }

   default int helpRegionsAtOnce() {
      return 3;
   }

   default boolean offloadToClient(ServerPlayer player, byte[] frame) {
      return false;
   }

   default void offloadToServer(byte[] frame) {
   }

   default boolean generateLakes() {
      return true;
   }

   default boolean generateRivers() {
      return this.generateStreams() || this.generateLakes();
   }

   default BlockState streamWaterState(int flowDir8) {
      return Blocks.WATER.defaultBlockState();
   }

   default BlockState streamFallingState(int flowDir8) {
      return null;
   }

   default boolean flowVanillaRivers() {
      return false;
   }

   default boolean riverSeamAgreement() {
      return true;
   }

   default int bankFeatures() {
      return 3;
   }

   default boolean vanillaWaterFlow() {
      return false;
   }

   default double vesselDriftSpeed() {
      return 30.0;
   }

   default boolean fastChunkLoading() {
      return true;
   }

   default boolean gpuAcceleration() {
      return false;
   }

   default double biomeWetness(Holder<Biome> biome) {
      return 1.0;
   }

   default int gridSpacing() {
      return 16;
   }

   default double widthScale() {
      return 1.0;
   }

   default double wetnessMultiplier() {
      return 1.0;
   }

   default double wetnessDryFloor() {
      return 0.02;
   }

   default int minStreamLength() {
      return 75;
   }

   default int minLengthScope() {
      return 2;
   }

   default int riverDrainageSize() {
      return 1;
   }

   default double lakeMinArea() {
      return 1024.0;
   }

   default double lakeMaxArea() {
      return 0.0;
   }

   default int plateScale() {
      return WatershedSize.DEFAULT.blocks();
   }

   default int terrainSampleSpacing() {
      return 32;
   }

   default int valleySnapRadius() {
      return 8;
   }

   default int terrainFit() {
      return 4;
   }

   default int lakeSampleSpacing() {
      return 4;
   }

   default boolean adaptiveGridRead() {
      return true;
   }

   default boolean alignReadPoints() {
      return true;
   }

   default boolean legacyShaping() {
      return false;
   }

   default int prefetchRadius() {
      return 1;
   }

   default boolean connectNearbyWater() {
      return false;
   }

   default int connectRadius() {
      return 24;
   }

   default boolean sinkRooted() {
      return false;
   }

   default double growDensity() {
      return 0.75;
   }

   default boolean confineToBanks() {
      return false;
   }

   default boolean containmentBerm() {
      return true;
   }

   default double meanderStrength() {
      return 1.2;
   }

   default double baseWidth() {
      return 0.4;
   }

   default double maxWidth() {
      return 10.0;
   }

   default double streamDepth() {
      return 0.24;
   }

   default double widthFollowsSlope() {
      return 0.5;
   }

   default double widthFollowsClimate() {
      return 0.5;
   }

   default double widthVariation() {
      return 0.5;
   }

   default double depthFollowsTerrain() {
      return 0.5;
   }

   default double bendAsymmetry() {
      return 0.5;
   }

   default double channelBars() {
      return 0.5;
   }

   default double deltaMouths() {
      return 0.5;
   }

   default double bankNoise() {
      return 0.21;
   }

   default boolean bermYieldToStreams() {
      return true;
   }

   default boolean customBanks() {
      return true;
   }

   default int physicsModWater() {
      return 1;
   }

   default String outletBiomes() {
      return "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp";
   }

   default boolean reterraforgedCompat() {
      return true;
   }

   default boolean fastOceanSkip() {
      return true;
   }

   default boolean fastTerrainEstimate() {
      return true;
   }

   default int terrainSamplingMode() {
      return 1;
   }

   default double gradeWindowAbove() {
      return 2.0;
   }

   default double gradeWindowBelow() {
      return 6.0;
   }

   default int vegetation() {
      return 1;
   }

   default int allWaterFlora() {
      return 1;
   }

   default int allWaterBanks() {
      return 1;
   }

   default int floraRadius() {
      return 5;
   }

   default int biomeBoostPercent() {
      return 4;
   }

   default String biomeBoostBlacklist() {
      return "";
   }

   default String streamBiomeBlacklist() {
      return "";
   }

   default String underwaterFloraBiomeBlacklist() {
      return "";
   }

   default int bankGrassPercent() {
      return 40;
   }

   default int underwaterFlora() {
      return 3;
   }

   default int underwaterFloraPercent() {
      return 35;
   }

   default boolean clearFloatingSnow() {
      return true;
   }

   default boolean welcomeMessage() {
      return true;
   }

   default boolean rebiomeStreams() {
      return this.rebiomeStreamsMode() != 0;
   }

   default boolean rebiomeLakes() {
      return this.rebiomeLakesMode() != 0;
   }

   default int rebiomeStreamsMode() {
      return 1;
   }

   default int rebiomeLakesMode() {
      return 1;
   }

   default String getEnvironmentName() {
      return this.isDevelopmentEnvironment() ? "development" : "production";
   }
}
