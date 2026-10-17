package dev.streamsreflowing.core.config;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record StreamsConfig(int seaLevel, double plateScale, double plateJitter, int gridSpacing, int terrainSampleSpacing, int valleySnapRadius, int terrainFit, int lakeSampleSpacing, boolean legacyShaping, boolean adaptiveGridRead, double minRiverAccumulation, double baseWidth, double maxWidth, double widthScale, double streamDepth, int stepDeltaY, double bankWidth, double bankSlope, double bankNoise, double bankSteepFactor, double ditchInset, double aRiver, double borderMargin, int smoothingPasses, double meanderStrength, int seaLevelExtension, double reconnectRadius, double wetnessDryCutoff, double wetnessWetReference, boolean connectNearbyWater, double connectRadius, boolean sinkRooted, boolean growFromWater, double growDensity, boolean confineToBanks, boolean containmentBerm, double wetnessMultiplier, double wetnessDryFloor, int minSeaChainNodes, int minLengthScope, double lakeMinArea, double lakeMaxArea, String outletBiomes, HydraulicGeometry hydraulics) {
   public static final int WIRE_FIELDS = 44;

   public StreamsConfig {
plateScale = plateScale;
gridSpacing = gridSpacing;
valleySnapRadius = valleySnapRadius;
lakeSampleSpacing = lakeSampleSpacing;
adaptiveGridRead = adaptiveGridRead;
baseWidth = baseWidth;
widthScale = widthScale;
stepDeltaY = stepDeltaY;
bankSlope = bankSlope;
bankSteepFactor = bankSteepFactor;
aRiver = aRiver;
smoothingPasses = smoothingPasses;
seaLevelExtension = seaLevelExtension;
wetnessDryCutoff = wetnessDryCutoff;
connectNearbyWater = connectNearbyWater;
sinkRooted = sinkRooted;
growDensity = growDensity;
containmentBerm = containmentBerm;
wetnessDryFloor = wetnessDryFloor;
minLengthScope = minLengthScope;
lakeMaxArea = lakeMaxArea;
hydraulics = hydraulics;
}

   public void write(DataOutput var1) throws IOException {
      var1.writeInt(44);
      var1.writeInt(this.seaLevel);
      var1.writeDouble(this.plateScale);
      var1.writeDouble(this.plateJitter);
      var1.writeInt(this.gridSpacing);
      var1.writeInt(this.terrainSampleSpacing);
      var1.writeInt(this.valleySnapRadius);
      var1.writeInt(this.terrainFit);
      var1.writeInt(this.lakeSampleSpacing);
      var1.writeBoolean(this.legacyShaping);
      var1.writeBoolean(this.adaptiveGridRead);
      var1.writeDouble(this.minRiverAccumulation);
      var1.writeDouble(this.baseWidth);
      var1.writeDouble(this.maxWidth);
      var1.writeDouble(this.widthScale);
      var1.writeDouble(this.streamDepth);
      var1.writeInt(this.stepDeltaY);
      var1.writeDouble(this.bankWidth);
      var1.writeDouble(this.bankSlope);
      var1.writeDouble(this.bankNoise);
      var1.writeDouble(this.bankSteepFactor);
      var1.writeDouble(this.ditchInset);
      var1.writeDouble(this.aRiver);
      var1.writeDouble(this.borderMargin);
      var1.writeInt(this.smoothingPasses);
      var1.writeDouble(this.meanderStrength);
      var1.writeInt(this.seaLevelExtension);
      var1.writeDouble(this.reconnectRadius);
      var1.writeDouble(this.wetnessDryCutoff);
      var1.writeDouble(this.wetnessWetReference);
      var1.writeBoolean(this.connectNearbyWater);
      var1.writeDouble(this.connectRadius);
      var1.writeBoolean(this.sinkRooted);
      var1.writeBoolean(this.growFromWater);
      var1.writeDouble(this.growDensity);
      var1.writeBoolean(this.confineToBanks);
      var1.writeBoolean(this.containmentBerm);
      var1.writeDouble(this.wetnessMultiplier);
      var1.writeDouble(this.wetnessDryFloor);
      var1.writeInt(this.minSeaChainNodes);
      var1.writeInt(this.minLengthScope);
      var1.writeDouble(this.lakeMinArea);
      var1.writeDouble(this.lakeMaxArea);
      var1.writeUTF(this.outletBiomes == null ? "" : this.outletBiomes);
      var1.writeDouble(this.hydraulics.widthSlope());
      var1.writeDouble(this.hydraulics.widthClimate());
      var1.writeDouble(this.hydraulics.widthVariation());
      var1.writeDouble(this.hydraulics.depthRatio());
      var1.writeDouble(this.hydraulics.bendAsymmetry());
      var1.writeDouble(this.hydraulics.channelBars());
      var1.writeDouble(this.hydraulics.deltaMouths());
   }

   public static StreamsConfig read(DataInput var0) throws IOException {
      if (var0.readInt() != 44) {
         throw new IOException("settings from a different build");
      } else {
         return new StreamsConfig(
            var0.readInt(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readInt(),
            var0.readInt(),
            var0.readInt(),
            var0.readInt(),
            var0.readInt(),
            var0.readBoolean(),
            var0.readBoolean(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readInt(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readInt(),
            var0.readDouble(),
            var0.readInt(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readBoolean(),
            var0.readDouble(),
            var0.readBoolean(),
            var0.readBoolean(),
            var0.readDouble(),
            var0.readBoolean(),
            var0.readBoolean(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readInt(),
            var0.readInt(),
            var0.readDouble(),
            var0.readDouble(),
            var0.readUTF(),
            new HydraulicGeometry(
               var0.readDouble(), var0.readDouble(), var0.readDouble(), var0.readDouble(), var0.readDouble(), var0.readDouble(), var0.readDouble()
            )
         );
      }
   }

   public static StreamsConfig defaults() {
      return defaults(12, 1.0, 1024, 0, 0, 4, 3, false);
   }

   public static StreamsConfig defaults(int var0, double var1, int var3, int var4, int var5, int var6, int var7, boolean var8) {
      return new StreamsConfig(
         63,
         var3,
         0.45,
         var0,
         var4,
         var5,
         var6,
         var7,
         var8,
         true,
         3.0,
         0.4,
         16.0,
         var1,
         0.24,
         1,
         2.0,
         0.2,
         0.21,
         3.5,
         3.0,
         1.0,
         0.0,
         3,
         1.2,
         128,
         96.0,
         0.2,
         0.7,
         false,
         24.0,
         false,
         true,
         0.75,
         false,
         true,
         1.0,
         0.02,
         3,
         2,
         512.0,
         0.0,
         "",
         HydraulicGeometry.OFF
      );
   }

   public static StreamsConfig defaults(int var0, double var1, int var3, int var4, int var5, int var6, int var7) {
      return defaults(var0, var1, var3, var4, var5, var6, var7, false);
   }

   public static StreamsConfig defaults(int var0, double var1, int var3, int var4, int var5, int var6) {
      return defaults(var0, var1, var3, var4, var5, var6, 3);
   }

   public static StreamsConfig defaults(int var0, double var1, int var3, int var4, int var5) {
      return defaults(var0, var1, var3, var4, var5, 4);
   }

   public StreamsConfig withOutletBiomes(String var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         var1,
         this.hydraulics
      );
   }

   public StreamsConfig withLakeArea(double var1, double var3) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         var1,
         var3,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withSeaLevel(int var1) {
      return new StreamsConfig(
         var1,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withMinLengthScope(int var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         var1,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withMinSeaChainNodes(int var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         var1,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withMinRiverAccumulation(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         var1,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withWetnessMultiplier(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         var1,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withWetnessDryFloor(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         var1,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withConnect(boolean var1, double var2) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         var1,
         var2,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withSinkRooted(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         var1,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withGrowFromWater(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         var1,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withAdaptiveGridRead(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         var1,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withGrowDensity(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         var1,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withConfineToBanks(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         var1,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withContainmentBerm(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         var1,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withAppearance(double var1, double var3, double var5, double var7) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         var3,
         var5,
         this.widthScale,
         var7,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         var1,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withBankNoise(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         var1,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         this.hydraulics
      );
   }

   public StreamsConfig withHydraulics(HydraulicGeometry var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.terrainFit,
         this.lakeSampleSpacing,
         this.legacyShaping,
         this.adaptiveGridRead,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.minLengthScope,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes,
         var1 == null ? HydraulicGeometry.OFF : var1
      );
   }
}
