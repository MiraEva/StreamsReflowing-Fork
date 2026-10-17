package dev.streamsreflowing.platform;

import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.forge.fluid.StreamFluids;
import dev.streamsreflowing.forge.fluid.StreamLiquidBlock;
import dev.streamsreflowing.forge.net.OffloadNet;
import dev.streamsreflowing.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

public class ForgePlatformHelper implements IPlatformHelper {
   @Override
   public String getPlatformName() {
      return "Forge";
   }

   @Override
   public boolean offloadRegionBuilds() {
      return StreamsSettings.offloadRegionBuilds();
   }

   @Override
   public boolean helpPrepareRegions() {
      return StreamsSettings.helpPrepareRegions();
   }

   @Override
   public int helpRegionsAtOnce() {
      return StreamsSettings.helpRegionsAtOnce();
   }

   @Override
   public boolean offloadToClient(ServerPlayer player, byte[] frame) {
      return OffloadNet.toClient(player, frame);
   }

   @Override
   public void offloadToServer(byte[] frame) {
      OffloadNet.toServer(frame);
   }

   @Override
   public BlockState streamWaterState(int flowDir8) {
      return !StreamsSettings.bakedStreamFluid()
         ? IPlatformHelper.super.streamWaterState(flowDir8)
         : (BlockState)((StreamLiquidBlock)StreamFluids.STREAM_BLOCK.get()).defaultBlockState().setValue(StreamLiquidBlock.FLOW, Mth.clamp(flowDir8, 0, 8));
   }

   @Override
   public BlockState streamFallingState(int flowDir8) {
      return !StreamsSettings.bakedStreamFluid()
         ? IPlatformHelper.super.streamFallingState(flowDir8)
         : (BlockState)((BlockState)((StreamLiquidBlock)StreamFluids.STREAM_BLOCK.get()).defaultBlockState().setValue(BlockStateProperties.LEVEL, 8))
            .setValue(StreamLiquidBlock.FLOW, Mth.clamp(flowDir8, 0, 8));
   }

   @Override
   public boolean flowVanillaRivers() {
      return StreamsSettings.flowVanillaRivers();
   }

   @Override
   public String terrainAccuracyPreset() {
      return StreamsSettings.preset().name();
   }

   @Override
   public boolean vanillaWaterFlow() {
      return StreamsSettings.vanillaWaterFlow();
   }

   @Override
   public boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }

   @Override
   public String modVersion(String modId) {
      return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getVersion().toString()).orElse(null);
   }

   @Override
   public boolean isDevelopmentEnvironment() {
      return !FMLLoader.isProduction();
   }

   @Override
   public boolean generateStreams() {
      return StreamsSettings.generateStreams();
   }

   @Override
   public boolean generateLakes() {
      return StreamsSettings.generateLakes();
   }

   @Override
   public double biomeWetness(Holder<Biome> biome) {
      return ((Biome)biome.value()).getModifiedClimateSettings().downfall();
   }

   @Override
   public int gridSpacing() {
      return StreamsSettings.gridSpacing();
   }

   @Override
   public int terrainFit() {
      return StreamsSettings.terrainFit();
   }

   @Override
   public double widthScale() {
      return StreamsSettings.widthScale();
   }

   @Override
   public double wetnessMultiplier() {
      return StreamsSettings.wetnessMultiplier();
   }

   @Override
   public double wetnessDryFloor() {
      return StreamsSettings.wetnessDryFloor();
   }

   @Override
   public int minStreamLength() {
      return StreamsSettings.minStreamLength();
   }

   @Override
   public int minLengthScope() {
      return StreamsSettings.minLengthScope();
   }

   @Override
   public int riverDrainageSize() {
      return StreamsSettings.riverDrainageSize();
   }

   @Override
   public double lakeMinArea() {
      return StreamsSettings.lakeMinArea();
   }

   @Override
   public double lakeMaxArea() {
      return StreamsSettings.lakeMaxArea();
   }

   @Override
   public int plateScale() {
      return StreamsSettings.plateScale();
   }

   @Override
   public int terrainSampleSpacing() {
      return StreamsSettings.terrainSampleSpacing();
   }

   @Override
   public int valleySnapRadius() {
      return StreamsSettings.valleySnapRadius();
   }

   @Override
   public int lakeSampleSpacing() {
      return StreamsSettings.lakeSampleSpacing();
   }

   @Override
   public boolean legacyShaping() {
      return StreamsSettings.legacyShaping();
   }

   @Override
   public boolean adaptiveGridRead() {
      return StreamsSettings.adaptiveGridRead();
   }

   @Override
   public boolean connectNearbyWater() {
      return StreamsSettings.connectNearbyWater();
   }

   @Override
   public int connectRadius() {
      return StreamsSettings.connectRadius();
   }

   @Override
   public boolean fastChunkLoading() {
      return StreamsSettings.fastChunkLoading();
   }

   @Override
   public int prefetchRadius() {
      return StreamsSettings.prefetchRadius();
   }

   @Override
   public boolean sinkRooted() {
      return StreamsSettings.sinkRooted();
   }

   @Override
   public double growDensity() {
      return StreamsSettings.growDensity();
   }

   @Override
   public boolean confineToBanks() {
      return StreamsSettings.confineToBanks();
   }

   @Override
   public boolean containmentBerm() {
      return StreamsSettings.containmentBerm();
   }

   @Override
   public double meanderStrength() {
      return StreamsSettings.meanderStrength();
   }

   @Override
   public double baseWidth() {
      return StreamsSettings.baseWidth();
   }

   @Override
   public double maxWidth() {
      return StreamsSettings.maxWidth();
   }

   @Override
   public double streamDepth() {
      return StreamsSettings.streamDepth();
   }

   @Override
   public double widthFollowsSlope() {
      return StreamsSettings.widthFollowsSlope();
   }

   @Override
   public double widthFollowsClimate() {
      return StreamsSettings.widthFollowsClimate();
   }

   @Override
   public double widthVariation() {
      return StreamsSettings.widthVariation();
   }

   @Override
   public double depthFollowsTerrain() {
      return StreamsSettings.depthFollowsTerrain();
   }

   @Override
   public double bendAsymmetry() {
      return StreamsSettings.bendAsymmetry();
   }

   @Override
   public double channelBars() {
      return StreamsSettings.channelBars();
   }

   @Override
   public double deltaMouths() {
      return StreamsSettings.deltaMouths();
   }

   @Override
   public double bankNoise() {
      return StreamsSettings.bankNoise();
   }

   @Override
   public boolean bermYieldToStreams() {
      return StreamsSettings.bermYieldToStreams();
   }

   @Override
   public boolean customBanks() {
      return StreamsSettings.customBanks();
   }

   @Override
   public boolean fastTerrainEstimate() {
      return StreamsSettings.fastTerrainEstimate();
   }

   @Override
   public int terrainSamplingMode() {
      return StreamsSettings.terrainSamplingMode();
   }

   @Override
   public double gradeWindowAbove() {
      return PresetOverrides.gradeWindowAbove(StreamsSettings.preset().gradeWindowAbove);
   }

   @Override
   public double gradeWindowBelow() {
      return PresetOverrides.gradeWindowBelow(StreamsSettings.preset().gradeWindowBelow);
   }

   @Override
   public int vegetation() {
      return StreamsSettings.vegetation();
   }

   @Override
   public int allWaterFlora() {
      return StreamsSettings.allWaterFlora();
   }

   @Override
   public int allWaterBanks() {
      return StreamsSettings.allWaterBanks();
   }

   @Override
   public int physicsModWater() {
      return StreamsSettings.physicsModWater();
   }

   @Override
   public String outletBiomes() {
      return StreamsSettings.outletBiomes();
   }

   @Override
   public boolean reterraforgedCompat() {
      return StreamsSettings.reterraforgedCompat();
   }

   @Override
   public boolean fastOceanSkip() {
      return StreamsSettings.fastOceanSkip();
   }

   @Override
   public int biomeBoostPercent() {
      return StreamsSettings.biomeBoostPercent();
   }

   @Override
   public String biomeBoostBlacklist() {
      return StreamsSettings.biomeBoostBlacklist();
   }

   @Override
   public String streamBiomeBlacklist() {
      return StreamsSettings.streamBiomeBlacklist();
   }

   @Override
   public String underwaterFloraBiomeBlacklist() {
      return StreamsSettings.underwaterFloraBiomeBlacklist();
   }

   @Override
   public int bankGrassPercent() {
      return StreamsSettings.bankGrassPercent();
   }

   @Override
   public int underwaterFlora() {
      return StreamsSettings.underwaterFlora();
   }

   @Override
   public int underwaterFloraPercent() {
      return StreamsSettings.underwaterFloraPercent();
   }

   @Override
   public int floraRadius() {
      return StreamsSettings.floraRadius();
   }

   @Override
   public boolean clearFloatingSnow() {
      return StreamsSettings.clearFloatingSnow();
   }

   @Override
   public int rebiomeStreamsMode() {
      return StreamsSettings.rebiomeStreams();
   }

   @Override
   public int rebiomeLakesMode() {
      return StreamsSettings.rebiomeLakes();
   }

   @Override
   public boolean welcomeMessage() {
      return StreamsSettings.welcomeMessage();
   }

   @Override
   public Path configDir() {
      return FMLPaths.CONFIGDIR.get();
   }
}
