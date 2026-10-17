package dev.streamsreflowing.config;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.PhysicsModCompat;
import dev.streamsreflowing.core.config.QualityPreset;
import dev.streamsreflowing.core.config.WatershedSize;
import dev.streamsreflowing.flow.MobFlowOverrides;
import dev.streamsreflowing.platform.PresetOverrides;
import dev.streamsreflowing.platform.Services;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StreamsSettings {
   private static final String FILE_NAME = "streamsreflowing.properties";
   private static final String FILE_NAME_CLIENT = "streamsreflowing-client.properties";
   private static final String K_GENERATE_STREAMS = "generateStreams";
   private static final String K_GENERATE_LAKES = "generateLakes";
   private static final String K_OFFLOAD_REGION_BUILDS = "offloadRegionBuilds";
   private static final String K_HELP_PREPARE_REGIONS = "helpPrepareRegions";
   private static final String K_HELP_REGIONS_AT_ONCE = "helpRegionsAtOnce";
   private static final String K_QUALITY_PRESET = "terrainAccuracyLevel";
   private static final String K_QUALITY_PRESET_LEGACY = "qualityPreset";
   private static final String K_WATERSHED_SIZE = "watershedSize";
   private static final String K_PLATE_SIZE = "plateSize";
   private static final String K_TERRAIN_FIT = "terrainFit";
   private static final String K_LEGACY_STREAM_SHAPING = "legacyStreamShaping";
   private static final String K_ADAPTIVE_GRID_READ = "adaptiveGridRead";
   private static final String K_FAST_TERRAIN_ESTIMATE = "fastTerrainEstimate";
   private static final String K_ALIGN_READ_POINTS = "alignReadPoints";
   private static final String K_WETNESS_MULTIPLIER = "wetnessMultiplier";
   private static final String K_WETNESS_DRY_FLOOR = "wetnessDryFloor";
   private static final String K_GROW_DENSITY = "growDensity";
   private static final String K_MIN_STREAM_LENGTH = "minStreamLength";
   private static final String K_MIN_LENGTH_SCOPE = "minLengthScope";
   private static final String K_RIVER_DRAINAGE_SIZE = "riverDrainageSize";
   private static final String K_LAKE_MIN_AREA = "lakeMinArea";
   private static final String K_LAKE_MAX_AREA = "lakeMaxArea";
   private static final String K_WIDTH_SCALE = "widthScale";
   private static final String K_BASE_WIDTH = "baseWidth";
   private static final String K_MAX_WIDTH = "maxWidth";
   private static final String K_MEANDER_STRENGTH = "meanderStrength";
   private static final String K_STREAM_DEPTH = "streamDepth";
   private static final String K_WIDTH_FOLLOWS_SLOPE = "widthFollowsSlope";
   private static final String K_WIDTH_FOLLOWS_CLIMATE = "widthFollowsClimate";
   private static final String K_WIDTH_VARIATION = "widthVariation";
   private static final String K_DEPTH_FOLLOWS_TERRAIN = "depthFollowsTerrain";
   private static final String K_BEND_ASYMMETRY = "bendAsymmetry";
   private static final String K_CHANNEL_BARS = "channelBars";
   private static final String K_DELTA_MOUTHS = "deltaMouths";
   private static final String K_CONTAINMENT_BERM = "containmentBerm";
   private static final String K_CUSTOM_BANKS = "customBanks";
   private static final String K_VEGETATION = "vegetation";
   private static final String K_ALL_WATER_FLORA = "allWaterFlora";
   private static final String K_ALL_WATER_BANKS = "allWaterBanks";
   private static final String K_BANK_FEATURES = "bankFeatures";
   private static final String K_FLORA_RADIUS = "floraRadius";
   private static final String K_BIOME_BOOST_PERCENT = "biomeBoostPercent";
   private static final String K_BIOME_BOOST_BLACKLIST = "biomeBoostBlacklist";
   private static final String K_STREAM_BIOME_BLACKLIST = "streamBiomeBlacklist";
   private static final String K_MOBS_HOLD_IN_CURRENT = "mobsHoldInCurrent";
   private static final String K_MOBS_DRIFT_IN_CURRENT = "mobsDriftInCurrent";
   private static final String K_BANK_GRASS_PERCENT = "bankGrassPercent";
   private static final String K_UNDERWATER_FLORA = "underwaterFlora";
   private static final String K_UNDERWATER_FLORA_PERCENT = "underwaterFloraPercent";
   private static final String K_UNDERWATER_FLORA_BIOME_BLACKLIST = "underwaterFloraBiomeBlacklist";
   private static final String K_SPILL_OVER_WATER = "spillOverWater";
   private static final String K_SPILL_STREAMS_ONLY = "spillOnStreamsOnly";
   private static final String K_FAST_CHUNK_LOADING = "fastChunkLoading";
   private static final String K_BOATS_FLOAT = "boatsFloatOnFlowingWater";
   private static final String K_WATER_MOBS_IGNORE_FLOW = "waterMobsIgnoreFlow";
   private static final String K_ITEM_DRIFT_SPEED = "itemDriftSpeed";
   private static final String K_PLAYER_PUSH = "playerPush";
   private static final String K_BOAT_PUSH = "boatPush";
   private static final String K_MOB_PUSH = "mobPush";
   private static final String K_BANK_NOISE = "bankNoise";
   private static final String K_SERVER_STREAM_PARTICLES = "serverStreamParticles";
   private static final String K_VESSEL_DRIFT_SPEED = "vesselDriftSpeed";
   private static final String K_VANILLA_WATER_FLOW = "vanillaWaterFlow";
   private static final String K_FLOW_VANILLA_RIVERS = "flowVanillaRivers";
   private static final String K_RIVER_SEAM_AGREEMENT = "riverSeamAgreement";
   private static final String K_RETERRAFORGED_COMPAT = "reterraforgedCompat";
   private static final String K_BAKED_FLUID_WFF = "bakedFluidWhenFlowingFluids";
   private static final String K_OUTLET_BIOMES = "outletBiomes";
   private static final String K_PHYSICS_MOD_WATER = "physicsModWater";
   private static final String K_FORCE_BAKED_FLUID = "forceBakedFluid";
   private static final String K_STREAM_BUBBLES = "streamBubbles";
   private static final String K_PARTNER_PARTICLES = "partnerParticles";
   private static final String K_WATERFALL_FOAM = "waterfallFoam";
   private static final String K_WATER_AMBIENCE = "waterAmbience";
   private static final String K_MAX_STREAM_PARTICLES = "maxStreamParticlesPerTick";
   private static final String K_STREAM_PARTICLE_SCAN_RADIUS = "streamParticleScanRadius";
   private static final String K_PARTICLES_ON_VANILLA_WATER = "particlesOnVanillaWater";
   private static final String K_STREAM_DEBRIS = "streamDebris";
   private static final String K_PARTICLE_SIZE = "particleSize";
   private static final String K_PARTICLE_RATE = "particleDensity";
   private static final String K_LOADING_NOTICE = "loadingNotice";
   private static final String K_CONNECT_NEARBY_WATER = "connectNearbyWater";
   private static final String K_CONNECT_RADIUS = "connectRadius";
   private static final String K_REBIOME_STREAMS = "rebiomeStreams";
   private static final String K_REBIOME_LAKES = "rebiomeLakes";
   private static final String K_WELCOME_MESSAGE = "welcomeMessage";
   private static final boolean D_GENERATE_STREAMS = true;
   private static final boolean D_GENERATE_LAKES = true;
   private static final boolean D_OFFLOAD_REGION_BUILDS = false;
   private static final boolean D_HELP_PREPARE_REGIONS = true;
   private static final QualityPreset D_QUALITY_PRESET = QualityPreset.MEDIUM;
   private static final String D_WATERSHED_SIZE = WatershedSize.DEFAULT.name();
   private static final int PLATE_SIZE_MIN = 256;
   private static final int PLATE_SIZE_MAX = 8192;
   private static final double D_WETNESS_MULTIPLIER = 1.0;
   private static final double D_WETNESS_DRY_FLOOR = 0.1;
   private static final double D_GROW_DENSITY = 0.75;
   private static final int D_MIN_STREAM_LENGTH = 75;
   private static final int D_RIVER_DRAINAGE_SIZE = 1;
   private static final double D_LAKE_MIN_AREA = 1024.0;
   private static final double D_LAKE_MAX_AREA = 0.0;
   private static final double D_WIDTH_SCALE = 1.0;
   private static final double D_BASE_WIDTH = 0.4;
   private static final double D_MAX_WIDTH = 16.0;
   private static final double D_MEANDER_STRENGTH = 1.2;
   private static final double D_STREAM_DEPTH = 0.24;
   private static final double D_WIDTH_FOLLOWS_SLOPE = 0.5;
   private static final double D_WIDTH_FOLLOWS_CLIMATE = 0.5;
   private static final double D_WIDTH_VARIATION = 0.5;
   private static final double D_DEPTH_FOLLOWS_TERRAIN = 0.5;
   private static final double D_BEND_ASYMMETRY = 0.5;
   private static final double D_CHANNEL_BARS = 0.5;
   private static final double D_DELTA_MOUTHS = 0.5;
   private static final boolean D_CONTAINMENT_BERM = true;
   private static final boolean D_CUSTOM_BANKS = true;
   private static final int D_VEGETATION = 1;
   private static final int D_ALL_WATER_FLORA = 1;
   private static final int D_ALL_WATER_BANKS = 1;
   private static final int D_TERRAIN_FIT = -1;
   private static final boolean D_LEGACY_STREAM_SHAPING = false;
   private static final boolean D_ADAPTIVE_GRID_READ = true;
   private static final boolean D_ALIGN_READ_POINTS = true;
   private static final int D_FLORA_RADIUS = 5;
   private static final int D_BIOME_BOOST_PERCENT = 4;
   private static final String D_BIOME_BOOST_BLACKLIST = "";
   private static final String D_STREAM_BIOME_BLACKLIST = "";
   private static final int D_BANK_GRASS_PERCENT = 20;
   private static final int D_UNDERWATER_FLORA = 3;
   private static final int D_UNDERWATER_FLORA_PERCENT = 15;
   private static final String D_UNDERWATER_FLORA_BIOME_BLACKLIST = "";
   private static final boolean D_SPILL_OVER_WATER = true;
   private static final boolean D_SPILL_STREAMS_ONLY = true;
   private static final boolean D_FAST_CHUNK_LOADING = true;
   private static final boolean D_BOATS_FLOAT = true;
   private static final boolean D_WATER_MOBS_IGNORE_FLOW = true;
   private static final double D_ITEM_DRIFT_SPEED = 1.5;
   private static final double D_VESSEL_DRIFT_SPEED = 30.0;
   private static final boolean D_VANILLA_WATER_FLOW = true;
   private static final boolean D_FLOW_VANILLA_RIVERS = true;
   private static final boolean D_RIVER_SEAM_AGREEMENT = true;
   private static final boolean D_RETERRAFORGED_COMPAT = true;
   private static final int D_PHYSICS_MOD_WATER = 3;
   private static final String D_OUTLET_BIOMES = "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp";
   private static final boolean D_BAKED_FLUID_WFF = true;
   private static final boolean D_FORCE_BAKED_FLUID = false;
   private static final int D_STREAM_BUBBLES = 2;
   private static final double D_WATER_AMBIENCE = 1.0;
   private static final int D_MAX_STREAM_PARTICLES = 1000;
   private static final int D_STREAM_PARTICLE_SCAN_RADIUS = 256;
   private static final boolean D_PARTICLES_ON_VANILLA_WATER = true;
   private static final boolean D_STREAM_DEBRIS = true;
   private static final double D_PARTICLE_SIZE = 1.0;
   private static final double D_PARTICLE_RATE = 1.0;
   private static final boolean D_LOADING_NOTICE = true;
   private static final boolean D_CONNECT_NEARBY_WATER = false;
   private static final int D_CONNECT_RADIUS = -1;
   private static final int D_REBIOME_STREAMS = 1;
   private static final int D_REBIOME_LAKES = 1;
   private static final boolean D_WELCOME_MESSAGE = true;
   private static volatile boolean generateStreams = true;
   private static volatile boolean generateLakes = true;
   private static volatile boolean offloadRegionBuilds = false;
   private static volatile boolean helpPrepareRegions = true;
   private static volatile int helpRegionsAtOnce = 3;
   private static volatile QualityPreset qualityPreset = D_QUALITY_PRESET;
   private static volatile WatershedSize watershedSize = WatershedSize.DEFAULT;
   private static volatile double wetnessMultiplier = 1.0;
   private static volatile double wetnessDryFloor = 0.1;
   private static volatile double growDensity = 0.75;
   private static volatile int minStreamLength = 75;
   private static volatile int minLengthScope = 2;
   private static volatile int riverDrainageSize = 1;
   private static volatile double lakeMinArea = 1024.0;
   private static volatile double lakeMaxArea = 0.0;
   private static volatile double widthScale = 1.0;
   private static volatile double baseWidth = 0.4;
   private static volatile double maxWidth = 16.0;
   private static volatile double meanderStrength = 1.2;
   private static volatile double streamDepth = 0.24;
   private static volatile double widthFollowsSlope = 0.5;
   private static volatile double widthFollowsClimate = 0.5;
   private static volatile double widthVariation = 0.5;
   private static volatile double depthFollowsTerrain = 0.5;
   private static volatile double bendAsymmetry = 0.5;
   private static volatile double channelBars = 0.5;
   private static volatile double deltaMouths = 0.5;
   private static volatile boolean containmentBerm = true;
   private static volatile boolean customBanks = true;
   private static volatile int vegetation = 1;
   private static volatile int allWaterFlora = 1;
   private static volatile int allWaterBanks = 1;
   private static volatile int terrainFit = -1;
   private static volatile boolean legacyStreamShaping = false;
   private static volatile boolean adaptiveGridRead = true;
   private static volatile boolean fastTerrainEstimate = true;
   private static volatile boolean alignReadPoints = true;
   private static volatile int bankFeatures = 3;
   private static volatile int floraRadius = 5;
   private static volatile int biomeBoostPercent = 4;
   private static volatile String biomeBoostBlacklist = "";
   private static volatile String streamBiomeBlacklist = "";
   private static volatile String mobsHoldInCurrent = "";
   private static volatile String mobsDriftInCurrent = "";
   private static volatile int bankGrassPercent = 20;
   private static volatile int underwaterFlora = 3;
   private static volatile int underwaterFloraPercent = 15;
   private static volatile String underwaterFloraBiomeBlacklist = "";
   private static volatile boolean spillOverWater = true;
   private static volatile boolean spillOnStreamsOnly = true;
   private static volatile boolean fastChunkLoading = true;
   private static volatile boolean boatsFloatOnFlowingWater = true;
   private static volatile boolean waterMobsIgnoreFlow = true;
   private static volatile double itemDriftSpeed = 1.5;
   private static final double D_PLAYER_PUSH = 1.2;
   private static final double D_BOAT_PUSH = 2.0;
   private static final double D_MOB_PUSH = 1.2;
   private static volatile double playerPush = 1.2;
   private static volatile double boatPush = 2.0;
   private static volatile double mobPush = 1.2;
   private static volatile double bankNoise = 0.21;
   private static volatile boolean serverStreamParticles = true;
   private static volatile double vesselDriftSpeed = 30.0;
   private static volatile boolean vanillaWaterFlow = true;
   private static volatile boolean flowVanillaRivers = true;
   private static volatile boolean riverSeamAgreement = true;
   private static volatile boolean reterraforgedCompat = true;
   private static volatile boolean bakedFluidWhenFlowingFluids = true;
   private static volatile String outletBiomes = "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp";
   private static volatile int physicsModWater = 3;
   private static volatile boolean forceBakedFluid = false;
   private static volatile int streamBubbles = 2;
   private static final boolean D_PARTNER_PARTICLES = true;
   private static final int D_WATERFALL_FOAM = 3;
   private static volatile boolean partnerParticles = true;
   private static volatile int waterfallFoam = 3;
   private static volatile double waterAmbience = 1.0;
   private static volatile int maxStreamParticles = 1000;
   private static volatile int streamParticleScanRadius = 256;
   private static volatile boolean particlesOnVanillaWater = true;
   private static volatile boolean streamDebris = true;
   private static volatile double particleSize = 1.0;
   private static volatile double particleRate = 1.0;
   private static volatile boolean loadingNotice = true;
   private static volatile boolean connectNearbyWater = false;
   private static volatile int connectRadius = -1;
   private static volatile int rebiomeStreams = 1;
   private static volatile int rebiomeLakes = 1;
   private static volatile boolean welcomeMessage = true;
   private static volatile boolean loadedOnce;
   private static final String TOML_COMMON = "streamsreflowing-common.toml";
   private static final String TOML_CLIENT = "streamsreflowing-client.toml";
   private static Boolean flowingFluidsLoaded;
   private static final Map<String, String> LOADED_DIGEST = new ConcurrentHashMap<>();
   private static volatile Thread watchThread;

   private StreamsSettings() {
   }

   public static void load() {
      try {
         Path var0 = Services.PLATFORM.configDir().resolve("streamsreflowing.properties");
         if (!Files.exists(var0)) {
            writeDefaults(var0, defaultFileContents());
         }

         refreshComments(var0, defaultFileContents());
         Properties var1 = new Properties();

         try (InputStream var2 = Files.newInputStream(var0)) {
            var1.load(var2);
         }

         generateStreams = parseBool(var1, "generateStreams", true);
         generateLakes = parseBool(var1, "generateLakes", true);
         offloadRegionBuilds = parseBool(var1, "offloadRegionBuilds", false);
         helpPrepareRegions = parseBool(var1, "helpPrepareRegions", true);
         helpRegionsAtOnce = Math.max(1, Math.min(3, parseInt(var1, "helpRegionsAtOnce", 3)));
         String var14 = var1.getProperty("terrainAccuracyLevel");
         if (var14 == null) {
            var14 = var1.getProperty("qualityPreset");
         }

         qualityPreset = parsePreset(var14);
         String var3 = var1.getProperty("watershedSize");
         if (var3 == null && var1.getProperty("plateSize") != null) {
            int var4 = parsePlateSize(var1.getProperty("plateSize"));
            var3 = Integer.toString(var4 > 0 ? var4 : WatershedSize.formerPresetBlocks(qualityPreset.name()));
         }

         watershedSize = WatershedSize.parse(var3);
         wetnessMultiplier = parseDouble(var1, "wetnessMultiplier", 1.0);
         wetnessDryFloor = parseDouble(var1, "wetnessDryFloor", 0.1);
         growDensity = Math.max(0.1, Math.min(2.0, parseDouble(var1, "growDensity", 0.75)));
         minStreamLength = Math.max(0, Math.min(2000, parseInt(var1, "minStreamLength", 75)));
         minLengthScope = Math.max(1, Math.min(3, parseInt(var1, "minLengthScope", 2)));
         riverDrainageSize = Math.max(1, Math.min(3, parseInt(var1, "riverDrainageSize", 1)));
         lakeMinArea = parseDouble(var1, "lakeMinArea", 1024.0);
         lakeMaxArea = parseDouble(var1, "lakeMaxArea", 0.0);
         widthScale = parseDouble(var1, "widthScale", 1.0);
         baseWidth = parseDouble(var1, "baseWidth", 0.4);
         maxWidth = parseDouble(var1, "maxWidth", 16.0);
         meanderStrength = parseDouble(var1, "meanderStrength", 1.2);
         streamDepth = parseDouble(var1, "streamDepth", 0.24);
         widthFollowsSlope = Math.max(0.0, Math.min(1.0, parseDouble(var1, "widthFollowsSlope", 0.5)));
         widthFollowsClimate = Math.max(0.0, Math.min(1.0, parseDouble(var1, "widthFollowsClimate", 0.5)));
         widthVariation = Math.max(0.0, Math.min(1.0, parseDouble(var1, "widthVariation", 0.5)));
         depthFollowsTerrain = Math.max(0.0, Math.min(1.0, parseDouble(var1, "depthFollowsTerrain", 0.5)));
         bendAsymmetry = Math.max(0.0, Math.min(1.0, parseDouble(var1, "bendAsymmetry", 0.5)));
         channelBars = Math.max(0.0, Math.min(1.0, parseDouble(var1, "channelBars", 0.5)));
         deltaMouths = Math.max(0.0, Math.min(1.0, parseDouble(var1, "deltaMouths", 0.5)));
         containmentBerm = parseBool(var1, "containmentBerm", true);
         customBanks = parseBool(var1, "customBanks", true);
         vegetation = levelOr(parseInt(var1, "vegetation", 1), 1, 2);
         allWaterFlora = levelOr(parseInt(var1, "allWaterFlora", 1), 1, 2);
         allWaterBanks = levelOr(parseInt(var1, "allWaterBanks", 1), 1, 2);
         terrainFit = parseInt(var1, "terrainFit", -1);
         legacyStreamShaping = parseBool(var1, "legacyStreamShaping", false);
         adaptiveGridRead = parseBool(var1, "adaptiveGridRead", true);
         fastTerrainEstimate = parseBool(var1, "fastTerrainEstimate", true);
         PresetOverrides.reload(Services.PLATFORM.configDir(), var1);
         bankFeatures = parseInt(var1, "bankFeatures", 3);
         floraRadius = parseInt(var1, "floraRadius", 5);
         biomeBoostPercent = parseInt(var1, "biomeBoostPercent", 4);
         biomeBoostBlacklist = var1.getProperty("biomeBoostBlacklist", "");
         streamBiomeBlacklist = var1.getProperty("streamBiomeBlacklist", "");
         mobsHoldInCurrent = var1.getProperty("mobsHoldInCurrent", "");
         mobsDriftInCurrent = var1.getProperty("mobsDriftInCurrent", "");
         MobFlowOverrides.set(mobsHoldInCurrent, mobsDriftInCurrent);
         bankGrassPercent = parseInt(var1, "bankGrassPercent", 20);
         underwaterFlora = levelOr(parseInt(var1, "underwaterFlora", 3), 3, 3);
         underwaterFloraPercent = parseInt(var1, "underwaterFloraPercent", 15);
         underwaterFloraBiomeBlacklist = var1.getProperty("underwaterFloraBiomeBlacklist", "");
         spillOverWater = parseBool(var1, "spillOverWater", true);
         spillOnStreamsOnly = parseBool(var1, "spillOnStreamsOnly", true);
         fastChunkLoading = parseBool(var1, "fastChunkLoading", true);
         boatsFloatOnFlowingWater = parseBool(var1, "boatsFloatOnFlowingWater", true);
         waterMobsIgnoreFlow = parseBool(var1, "waterMobsIgnoreFlow", true);
         itemDriftSpeed = parseDouble(var1, "itemDriftSpeed", 1.5);
         playerPush = Math.max(0.0, Math.min(4.0, parseDouble(var1, "playerPush", 1.2)));
         boatPush = Math.max(0.0, Math.min(4.0, parseDouble(var1, "boatPush", 2.0)));
         mobPush = Math.max(0.0, Math.min(4.0, parseDouble(var1, "mobPush", 1.2)));
         bankNoise = Math.max(0.0, Math.min(3.0, parseDouble(var1, "bankNoise", 0.21)));
         serverStreamParticles = parseBool(var1, "serverStreamParticles", true);
         vesselDriftSpeed = parseDouble(var1, "vesselDriftSpeed", 30.0);
         vanillaWaterFlow = parseBool(var1, "vanillaWaterFlow", true);
         flowVanillaRivers = parseBool(var1, "flowVanillaRivers", true);
         riverSeamAgreement = parseBool(var1, "riverSeamAgreement", true);
         reterraforgedCompat = parseBool(var1, "reterraforgedCompat", true);
         bakedFluidWhenFlowingFluids = parseBool(var1, "bakedFluidWhenFlowingFluids", true);
         outletBiomes = var1.getProperty("outletBiomes", "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp");
         physicsModWater = parseMode(var1, "physicsModWater", 3, 3);
         if (physicsModWater == 1) {
         }

         forceBakedFluid = parseBool(var1, "forceBakedFluid", false);
         connectNearbyWater = parseBool(var1, "connectNearbyWater", false);
         connectRadius = parseInt(var1, "connectRadius", -1);
         rebiomeStreams = parseMode(var1, "rebiomeStreams", 1, 2);
         rebiomeLakes = parseMode(var1, "rebiomeLakes", 1, 2);
         welcomeMessage = parseBool(var1, "welcomeMessage", true);
         Path var15 = Services.PLATFORM.configDir().resolve("streamsreflowing-client.properties");
         if (!Files.exists(var15)) {
            writeDefaults(var15, defaultClientFileContents());
         }

         refreshComments(var15, defaultClientFileContents());
         Properties var5 = new Properties();

         try (InputStream var6 = Files.newInputStream(var15)) {
            var5.load(var6);
         }

         streamBubbles = parseInt(var5, "streamBubbles", 2);
         partnerParticles = parseBool(var5, "partnerParticles", true);
         waterfallFoam = Math.max(0, Math.min(3, parseInt(var5, "waterfallFoam", 3)));
         waterAmbience = parseDouble(var5, "waterAmbience", 1.0);
         maxStreamParticles = parseInt(var5, "maxStreamParticlesPerTick", 1000);
         streamParticleScanRadius = parseInt(var5, "streamParticleScanRadius", 256);
         particlesOnVanillaWater = parseBool(var5, "particlesOnVanillaWater", true);
         streamDebris = parseBool(var5, "streamDebris", true);
         particleSize = Math.max(0.05, Math.min(8.0, parseDouble(var5, "particleSize", 1.0)));
         particleRate = Math.max(0.0, Math.min(8.0, parseDouble(var5, "particleDensity", 1.0)));
         loadingNotice = parseBool(var5, "loadingNotice", true);
         migrateFile(var0, var1, var15, var5);
      } catch (Exception var13) {
         Constants.LOG.warn("Failed to load {} -- using defaults", "streamsreflowing.properties", var13);
      }
   }

   public static Map<String, String> defaultValues(boolean var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (String var5 : (var0 ? defaultClientFileContents() : defaultFileContents()).split("\n")) {
         int var6 = var5.indexOf(61);
         if (var6 > 0 && var5.charAt(0) != '#') {
            var1.put(var5.substring(0, var6).trim(), var5.substring(var6 + 1).trim());
         }
      }

      return var1;
   }

   public static List<String[]> snapshotOrdered(boolean var0) {
      ArrayList var1 = new ArrayList();

      try {
         Path var2 = Services.PLATFORM.configDir().resolve(var0 ? "streamsreflowing-client.properties" : "streamsreflowing.properties");
         if (Files.exists(var2)) {
            StringBuilder var3 = new StringBuilder();
            String var4 = var0 ? "PARTICLES & SOUND" : "GENERAL";

            for (String var6 : Files.readAllLines(var2)) {
               String var7 = var6.trim();
               if (var7.isEmpty()) {
                  var3.setLength(0);
               } else if (var7.startsWith("#")) {
                  String var8 = var7.substring(1).trim();
                  if (var8.startsWith("=====")) {
                     var4 = var8.replace("=", "").trim();
                     var3.setLength(0);
                  } else {
                     if (var3.length() > 0) {
                        var3.append('\n');
                     }

                     var3.append(var8);
                  }
               } else {
                  int var10 = var7.indexOf(61);
                  if (var10 > 0) {
                     var1.add(new String[]{var7.substring(0, var10).trim(), var7.substring(var10 + 1).trim(), var3.toString(), var4});
                  }

                  var3.setLength(0);
               }
            }
         }
      } catch (Exception var9) {
      }

      return var1;
   }

   public static void setValues(Map<String, String> var0, Map<String, String> var1) {
      try {
         if (var0 != null && !var0.isEmpty()) {
            editProperties(Services.PLATFORM.configDir().resolve("streamsreflowing.properties"), var0);
         }

         if (var1 != null && !var1.isEmpty()) {
            editProperties(Services.PLATFORM.configDir().resolve("streamsreflowing-client.properties"), var1);
         }
      } catch (Exception var3) {
         Constants.LOG.warn("Failed to update config from screen", var3);
      }

      load();
      recordDigests();
   }

   private static void refreshComments(Path var0, String var1) throws IOException {
      String var2 = new String(Files.readAllBytes(var0), StandardCharsets.UTF_8);
      String var3 = null;

      for (String var7 : var1.split("\n")) {
         if (var7.startsWith("# layout:")) {
            var3 = var7.trim();
            break;
         }
      }

      if (var3 != null) {
         for (String var15 : var2.split("\n")) {
            if (var15.trim().equals(var3)) {
               return;
            }
         }
      }

      LinkedHashMap var10 = new LinkedHashMap();

      for (String var14 : Files.readAllLines(var0)) {
         String var16 = var14.trim();
         int var8 = var16.indexOf(61);
         if (!var16.isEmpty() && !var16.startsWith("#") && var8 > 0) {
            var10.put(var16.substring(0, var8).trim(), var16.substring(var8 + 1).trim());
         }
      }

      writeDefaults(var0, var1);
      if (!var10.isEmpty()) {
         editProperties(var0, var10);
      }
   }

   private static void migrateFile(Path var0, Properties var1, Path var2, Properties var3) throws IOException {
      LinkedHashMap var5 = new LinkedHashMap();
      canon(var1, var5, "generateStreams", Boolean.toString(generateStreams));
      canon(var1, var5, "generateLakes", Boolean.toString(generateLakes));
      canon(var1, var5, "offloadRegionBuilds", Boolean.toString(offloadRegionBuilds));
      canon(var1, var5, "helpPrepareRegions", Boolean.toString(helpPrepareRegions));
      canon(var1, var5, "helpRegionsAtOnce", Integer.toString(helpRegionsAtOnce));
      canon(var1, var5, "terrainAccuracyLevel", qualityPreset.name());
      canon(var1, var5, "watershedSize", watershedSize.name());
      canon(var1, var5, "terrainFit", Integer.toString(terrainFit));
      canon(var1, var5, "legacyStreamShaping", Boolean.toString(legacyStreamShaping));
      canon(var1, var5, "adaptiveGridRead", Boolean.toString(adaptiveGridRead));
      canon(var1, var5, "fastTerrainEstimate", Boolean.toString(fastTerrainEstimate));
      canon(var1, var5, "wetnessMultiplier", Double.toString(wetnessMultiplier));
      canon(var1, var5, "wetnessDryFloor", Double.toString(wetnessDryFloor));
      canon(var1, var5, "growDensity", Double.toString(growDensity));
      canon(var1, var5, "minStreamLength", Integer.toString(minStreamLength));
      canon(var1, var5, "minLengthScope", Integer.toString(minLengthScope));
      canon(var1, var5, "riverDrainageSize", Integer.toString(riverDrainageSize));
      canon(var1, var5, "lakeMinArea", Double.toString(lakeMinArea));
      canon(var1, var5, "lakeMaxArea", Double.toString(lakeMaxArea));
      canon(var1, var5, "widthScale", Double.toString(widthScale));
      canon(var1, var5, "baseWidth", Double.toString(baseWidth));
      canon(var1, var5, "maxWidth", Double.toString(maxWidth));
      canon(var1, var5, "meanderStrength", Double.toString(meanderStrength));
      canon(var1, var5, "streamDepth", Double.toString(streamDepth));
      canon(var1, var5, "widthFollowsSlope", Double.toString(widthFollowsSlope));
      canon(var1, var5, "widthFollowsClimate", Double.toString(widthFollowsClimate));
      canon(var1, var5, "widthVariation", Double.toString(widthVariation));
      canon(var1, var5, "depthFollowsTerrain", Double.toString(depthFollowsTerrain));
      canon(var1, var5, "bendAsymmetry", Double.toString(bendAsymmetry));
      canon(var1, var5, "channelBars", Double.toString(channelBars));
      canon(var1, var5, "deltaMouths", Double.toString(deltaMouths));
      canon(var1, var5, "containmentBerm", Boolean.toString(containmentBerm));
      canon(var1, var5, "customBanks", Boolean.toString(customBanks));
      canon(var1, var5, "vegetation", Integer.toString(vegetation));
      canon(var1, var5, "allWaterFlora", Integer.toString(allWaterFlora));
      canon(var1, var5, "allWaterBanks", Integer.toString(allWaterBanks));
      canon(var1, var5, "bankFeatures", Integer.toString(bankFeatures));
      canon(var1, var5, "floraRadius", Integer.toString(floraRadius));
      canon(var1, var5, "biomeBoostPercent", Integer.toString(biomeBoostPercent));
      canon(var1, var5, "bankGrassPercent", Integer.toString(bankGrassPercent));
      canon(var1, var5, "underwaterFlora", Integer.toString(underwaterFlora));
      canon(var1, var5, "underwaterFloraPercent", Integer.toString(underwaterFloraPercent));
      canon(var1, var5, "spillOverWater", Boolean.toString(spillOverWater));
      canon(var1, var5, "spillOnStreamsOnly", Boolean.toString(spillOnStreamsOnly));
      canon(var1, var5, "fastChunkLoading", Boolean.toString(fastChunkLoading));
      canon(var1, var5, "boatsFloatOnFlowingWater", Boolean.toString(boatsFloatOnFlowingWater));
      canon(var1, var5, "waterMobsIgnoreFlow", Boolean.toString(waterMobsIgnoreFlow));
      canon(var1, var5, "itemDriftSpeed", Double.toString(itemDriftSpeed));
      canon(var1, var5, "playerPush", Double.toString(playerPush));
      canon(var1, var5, "boatPush", Double.toString(boatPush));
      canon(var1, var5, "mobPush", Double.toString(mobPush));
      canon(var1, var5, "bankNoise", Double.toString(bankNoise));
      canon(var1, var5, "serverStreamParticles", Boolean.toString(serverStreamParticles));
      canon(var1, var5, "vesselDriftSpeed", Double.toString(vesselDriftSpeed));
      canon(var1, var5, "vanillaWaterFlow", Boolean.toString(vanillaWaterFlow));
      canon(var1, var5, "flowVanillaRivers", Boolean.toString(flowVanillaRivers));
      canon(var1, var5, "riverSeamAgreement", Boolean.toString(riverSeamAgreement));
      canon(var1, var5, "reterraforgedCompat", Boolean.toString(reterraforgedCompat));
      appendIfMissing(var1, var5, "outletBiomes", outletBiomes);
      canon(var1, var5, "physicsModWater", Integer.toString(physicsModWater));
      canon(var1, var5, "bakedFluidWhenFlowingFluids", Boolean.toString(bakedFluidWhenFlowingFluids));
      canon(var1, var5, "forceBakedFluid", Boolean.toString(forceBakedFluid));
      canon(var1, var5, "connectNearbyWater", Boolean.toString(connectNearbyWater));
      canon(var1, var5, "connectRadius", Integer.toString(connectRadius));
      canon(var1, var5, "rebiomeStreams", Integer.toString(rebiomeStreams));
      canon(var1, var5, "rebiomeLakes", Integer.toString(rebiomeLakes));
      canon(var1, var5, "welcomeMessage", Boolean.toString(welcomeMessage));
      appendIfMissing(var1, var5, "biomeBoostBlacklist", biomeBoostBlacklist);
      appendIfMissing(var1, var5, "streamBiomeBlacklist", streamBiomeBlacklist);
      appendIfMissing(var1, var5, "mobsHoldInCurrent", mobsHoldInCurrent);
      appendIfMissing(var1, var5, "mobsDriftInCurrent", mobsDriftInCurrent);
      appendIfMissing(var1, var5, "underwaterFloraBiomeBlacklist", underwaterFloraBiomeBlacklist);
      boolean var6 = var1.getProperty("qualityPreset") != null;
      boolean var4 = var1.getProperty("plateSize") != null;
      if (!var5.isEmpty() || var6 || var4) {
         editProperties(var0, var5);
         if (var6) {
            removeKeyLine(var0, "qualityPreset");
         }

         if (var4) {
            removeKeyLine(var0, "plateSize");
         }
      }

      LinkedHashMap var8 = new LinkedHashMap();
      canon(var3, var8, "streamBubbles", Integer.toString(streamBubbles));
      canon(var3, var8, "partnerParticles", Boolean.toString(partnerParticles));
      canon(var3, var8, "waterfallFoam", Integer.toString(waterfallFoam));
      canon(var3, var8, "waterAmbience", Double.toString(waterAmbience));
      canon(var3, var8, "maxStreamParticlesPerTick", Integer.toString(maxStreamParticles));
      canon(var3, var8, "streamParticleScanRadius", Integer.toString(streamParticleScanRadius));
      canon(var3, var8, "particlesOnVanillaWater", Boolean.toString(particlesOnVanillaWater));
      canon(var3, var8, "streamDebris", Boolean.toString(streamDebris));
      canon(var3, var8, "particleSize", Double.toString(particleSize));
      canon(var3, var8, "particleDensity", Double.toString(particleRate));
      canon(var3, var8, "loadingNotice", Boolean.toString(loadingNotice));
      if (!var8.isEmpty()) {
         editProperties(var2, var8);
      }
   }

   private static void canon(Properties var0, Map<String, String> var1, String var2, String var3) {
      String var4 = var0.getProperty(var2);
      if (var4 == null || !var4.trim().equals(var3)) {
         var1.put(var2, var3);
      }
   }

   private static void appendIfMissing(Properties var0, Map<String, String> var1, String var2, String var3) {
      if (var0.getProperty(var2) == null) {
         var1.put(var2, var3);
      }
   }

   private static void removeKeyLine(Path var0, String var1) throws IOException {
      String var2 = Files.readString(var0, StandardCharsets.UTF_8);
      String var3 = var2.replaceAll("(?m)^" + Pattern.quote(var1) + "\\s*=.*\\R?", "");
      if (!var3.equals(var2)) {
         Files.writeString(var0, var3, StandardCharsets.UTF_8);
      }
   }

   private static void editProperties(Path var0, Map<String, String> var1) throws IOException {
      String var2 = Files.exists(var0) ? Files.readString(var0, StandardCharsets.UTF_8) : "";

      for (Entry var4 : var1.entrySet()) {
         String var5 = (String)var4.getKey() + "=" + (String)var4.getValue();
         Pattern var6 = Pattern.compile("(?m)^" + Pattern.quote((String)var4.getKey()) + "\\s*=.*$");
         if (var6.matcher(var2).find()) {
            var2 = var6.matcher(var2).replaceFirst(Matcher.quoteReplacement(var5));
         } else {
            var2 = var2 + (!var2.endsWith("\n") && !var2.isEmpty() ? "\n" : "") + var5 + "\n";
         }
      }

      if (var0.getParent() != null) {
         Files.createDirectories(var0.getParent());
      }

      Files.writeString(var0, var2, StandardCharsets.UTF_8);
   }

   private static void writeDefaults(Path var0, String var1) {
      try {
         if (var0.getParent() != null) {
            Files.createDirectories(var0.getParent());
         }

         try (OutputStream var2 = Files.newOutputStream(var0)) {
            var2.write(var1.getBytes(StandardCharsets.UTF_8));
         }
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   private static String defaultFileContents() {
      StringBuilder var0 = new StringBuilder();
      var0.append("# Streams Reflowing -- настройки (этот файл: мир и сервер; streamsreflowing-client.properties: частицы и звуки).").append('\n');
      var0.append("# Формат: ключ=значение. Большинство настроек вступают в силу сразу; остальные указывают время применения.").append('\n');
      var0.append("# layout: 2.14 text 50ff902a-ru").append('\n');
      var0.append('\n');
      var0.append("# ===================== ПРОИЗВОДИТЕЛЬНОСТЬ И ПЛАНИРОВАНИЕ РУСЕЛ =====================").append('\n');
      var0.append("# Уровень точности определяет объём вычислений для новой местности. Размер речной системы задаёт длину каждой паузы.").append('\n');
      var0.append("# Русла планируются один раз и сохраняются вместе с миром, поэтому нагрузка возникает единожды для каждой области.").append('\n');
      var0.append('\n');
      var0.append("# Вырезать ручьи и реки этого мода в новой местности.").append('\n');
      var0.append("# false = ручьи не генерируются (озёра и течения продолжают работать).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("generateStreams").append('=').append(true).append("\n\n");
      var0.append("# Заполнять озёрами впадины, найденные модом.").append('\n');
      var0.append("# false = озёра этого мода не создаются.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("generateLakes").append('=').append(true).append("\n\n");
      var0.append("# Мультиплеер: клиенты игроков сами готовят русла для исследуемой местности, разгружая сервер.").append('\n');
      var0.append("# Помогают только игроки с установленным модом; остальные обслуживаются сервером как обычно.").append('\n');
      var0.append("# Помощникам отправляется сид мира. Вступает в силу сразу.").append('\n');
      var0.append("offloadRegionBuilds").append('=').append(false).append("\n\n");
      var0.append("# На сервере, запрашивающем помощь, разрешить этой игре подготавливать русла исследуемой местности.").append('\n');
      var0.append("# Использует свободные ресурсы процессора во время путешествий. false = сервер делает всё сам.").append('\n');
      var0.append("# Вступает в силу при следующем подключении к серверу.").append('\n');
      var0.append("helpPrepareRegions").append('=').append(true).append("\n\n");
      var0.append("# Сколько областей игра подготавливает для сервера одновременно (от 1 до 3).").append('\n');
      var0.append("# Уменьшите, если помощь серверу приводит к просадкам FPS при перемещении.").append('\n');
      var0.append("# Вступает в силу при следующем подключении к серверу.").append('\n');
      var0.append("helpRegionsAtOnce").append('=').append(3).append("\n\n");
      var0.append("# Точность считывания рельефа для планирования русел. Выше = точнее к рельефу, но больше вычислений на чанк.").append('\n');
      var0.append("# EXTREME и MAX требуют огромных ресурсов: рекомендуется предгенерация. Количество ручьёв зависит от growDensity и wetness.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("terrainAccuracyLevel").append('=').append(D_QUALITY_PRESET.name()).append("\n\n");
      var0.append("# Размер одной речной системы в блоках. Больше = более длинные и широкие русла, меньше стыков на границах.").append('\n');
      var0.append("# Производительность: тот же общий объём работы, но крупными частями. Удвоение размера = паузы в 4 раза длиннее, но в 4 раза реже.")
         .append('\n');
      var0.append("# Только для новых миров: мир навсегда сохраняет размер, с которым был начат.").append('\n');
      var0.append("watershedSize").append('=').append(D_WATERSHED_SIZE).append("\n\n");
      var0.append("# На какую дальность просчитывается течение ванильных рек (в единицах речной системы).").append('\n');
      var0.append("# 1 = одна система (по умолчанию), 2 = две, 3 = четыре. Большие значения ищут далёкие устья, но требуют больше ресурсов.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("riverDrainageSize").append('=').append(1).append("\n\n");
      var0.append("# Насколько плотно русло огибает микрорельеф после прокладки курса.").append('\n');
      var0.append("# 4 = максимально точно, 2 = мягче, 0 = без сглаживания, -1 = в соответствии с уровнем точности (terrainAccuracyLevel).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("terrainFit").append('=').append(-1).append("\n\n");
      var0.append("# Считывать рельеф целиком на изгибах (долины, хребты, пики) вместо примерной оценки.").append('\n');
      var0.append("# Ручьи точнее повторяют сложный рельеф ценой дополнительных вычислений. false = быстрее.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("adaptiveGridRead").append('=').append(true).append("\n\n");
      var0.append("# Приблизительно оценивать высоту рельефа там, где оценка оказывается точной, без полного считывания каждого блока.").append('\n');
      var0.append("# Значительно быстрее на подходящем рельефе. false = высота каждого блока считывается полностью.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("fastTerrainEstimate").append('=').append(true).append("\n\n");
      var0.append("# Формировать русла так, как это делалось в версиях до 2.14.").append('\n');
      var0.append("# Для сравнения или сохранения единого стиля новой местности со старым миром.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("legacyStreamShaping").append('=').append(false).append("\n\n");
      var0.append("# Ускорять загрузку чанков в сборках, использующих параллельную генерацию чанков.").append('\n');
      var0.append("# В остальных случаях не даёт эффекта. Отключайте только при сбоях загрузки чанков.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("fastChunkLoading").append('=').append(true).append("\n\n");
      var0.append("# ----- Где могут течь ручьи -----").append('\n');
      var0.append('\n');
      var0.append("# Биомы, в которых ручьи не начинаются и озёра не образуются сами по себе.").append('\n');
      var0.append("# Ручей, начавшийся в другом месте, всё равно может протекать через них.").append('\n');
      var0.append("# Список через запятую: ID биомов, #теги или маски вида modid:*.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("streamBiomeBlacklist").append('=').append("").append("\n\n");
      var0.append("# Биомы, в которых ручей может закончиться (устье).").append('\n');
      var0.append("# Список через запятую: #теги или ID биомов.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("outletBiomes").append('=').append("#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp").append("\n\n");
      var0.append("# Следовать рельефу и рекам мода ReTerraForged при его наличии.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("reterraforgedCompat").append('=').append(true).append("\n\n");
      var0.append("# Экспериментально: позволить пересыхающему ручью искать соседний водоём или ручей для слияния.").append('\n');
      var0.append("# Увеличивает время планирования на границах областей.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("connectNearbyWater").append('=').append(false).append("\n\n");
      var0.append("# Радиус поиска (в блоках) для тупиковых русел при включённом connectNearbyWater.").append('\n');
      var0.append("# -1 = автоматически.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("connectRadius").append('=').append(-1).append("\n\n");
      var0.append("# ----- Внешний вид русел: количество, длина и ветвление -----").append('\n');
      var0.append('\n');
      var0.append("# Общее количество воды в мире. Выше 1 = больше ручьёв, шире русла, выше нагрузка на генерацию.").append('\n');
      var0.append("# 1.0 — значение по умолчанию. Ниже 1 = меньше истоков и уже русла.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("wetnessMultiplier").append('=').append(1.0).append("\n\n");
      var0.append("# Какая доля обычной воды сохраняется в самых засушливых биомах.").append('\n');
      var0.append("# 0 = никакой, 0.1 = десятая часть (по умолчанию), 1 = столько же, сколько везде.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("wetnessDryFloor").append('=').append(0.1).append("\n\n");
      var0.append("# Плотность ветвления ручьёв. Меньше = больше мелких притоков и выше нагрузка на планирование.").append('\n');
      var0.append("# 0.75 — значение по умолчанию; 2.0 — самое редкое ветвление.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("growDensity").append('=').append(0.75).append("\n\n");
      var0.append("# Минимальная допустимая длина ручья (в блоках) от истока до впадения в воду.").append('\n');
      var0.append("# 0 = сохранять абсолютно все ручьи. Засушливые биомы не ограничиваются.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("minStreamLength").append('=').append(75).append("\n\n");
      var0.append("# К каким водоёмам применяется правило минимальной длины ручья.").append('\n');
      var0.append("# 1 = к океану, 2 = к океану и рекам, 3 = к океану, рекам и озёрам.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("minLengthScope").append('=').append(2).append("\n\n");
      var0.append("# Минимальная площадь впадины (блоков водного зеркала), превращаемая в озеро.").append('\n');
      var0.append("# Глубокие ямы на пути ручья заполняются водой независимо от размера.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои озёра.").append('\n');
      var0.append("lakeMinArea").append('=').append(1024.0).append("\n\n");
      var0.append("# Максимально допустимая площадь озера (блоков водного зеркала). 0 = автоматически.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои озёра.").append('\n');
      var0.append("lakeMaxArea").append('=').append(0.0).append("\n\n");
      var0.append("# ----- Внешний вид русел: ширина, глубина и извилистость -----").append('\n');
      var0.append('\n');
      var0.append("# Множитель ширины русел для собираемой ими площади воды.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("widthScale").append('=').append(1.0).append("\n\n");
      var0.append("# Начальная полуширина ручья у самого истока (в блоках).").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("baseWidth").append('=').append(0.4).append("\n\n");
      var0.append("# Максимальная полуширина, которой может достичь ручей (в блоках).").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("maxWidth").append('=').append(16.0).append("\n\n");
      var0.append("# Насколько сильно петляют ручьи. Крутые участки выпрямляются автоматически.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("meanderStrength").append('=').append(1.2).append("\n\n");
      var0.append("# Глубина русла относительно его ширины.").append('\n');
      var0.append("# 0.24 = около четверти ширины (по умолчанию). Выше = глубже и круче берега.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("streamDepth").append('=').append(0.24).append("\n\n");
      var0.append("# Сужение русла на крутых склонах и расширение на равнинах.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("widthFollowsSlope").append('=').append(0.5).append("\n\n");
      var0.append("# Расширение русел во влажных биомах и сужение в засушливых.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("widthFollowsClimate").append('=').append(0.5).append("\n\n");
      var0.append("# Случайные сужения и расширения по ходу течения ручья.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.").append('\n');
      var0.append("widthVariation").append('=').append(0.5).append("\n\n");
      var0.append("# На крутых участках русло прорезается глубже; на песчаных равнинах остаётся мелким.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("depthFollowsTerrain").append('=').append(0.5).append("\n\n");
      var0.append("# Внешний берег излучины глубже, на внутреннем образуется пологая гравийная отмель.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("bendAsymmetry").append('=').append(0.5).append("\n\n");
      var0.append("# На пологих участках образуются песчано-гравийные островки и разветвления.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("channelBars").append('=').append(0.5).append("\n\n");
      var0.append("# Веерообразные дельты при впадении ручьёв в озёра или океан на равнинном грунте.").append('\n');
      var0.append("# 0 = выкл, 1 = максимальный эффект.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("deltaMouths").append('=').append(0.5).append("\n\n");
      var0.append("# Формировать защитную бровку вдоль русла при пересечении склона, чтобы вода не переливалась.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("containmentBerm").append('=').append(true).append("\n\n");
      var0.append("# Степень неровности и изрезанности береговой линии (в блоках, от 0 до 3).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("bankNoise").append('=').append(0.21).append("\n\n");
      var0.append("# ----- Берега, растительность и декорации -----").append('\n');
      var0.append('\n');
      var0.append("# Оформлять русло материалами берегов из config/streamsreflowing/bank_styles.").append('\n');
      var0.append("# false = стандартные вырезанные блоки грунта.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("customBanks").append('=').append(true).append("\n\n");
      var0.append("# Применять материалы берегов и к другим водоёмам мира.").append('\n');
      var0.append("# 0 = только ручьи мода, 1 = также реки, озёра и пруды, 2 = также океанские побережья.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("allWaterBanks").append('=').append(1).append("\n\n");
      var0.append("# Где могут появляться декорации берегов (из config/streamsreflowing/bank_features).").append('\n');
      var0.append("# 0 = нигде, 1 = ручьи мода, 2 = также его озёра, 3 = любые водоёмы.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("bankFeatures").append('=').append(3).append("\n\n");
      var0.append("# Дополнительная растительность по берегам ручьёв мода.").append('\n');
      var0.append("# 0 = нет, 1 = трава и папоротники, 2 = также характерные растения биома.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("vegetation").append('=').append(1).append("\n\n");
      var0.append("# Дополнительная растительность у прочих водоёмов (реки, озёра, океаны).").append('\n');
      var0.append("# 0 = выкл, 1 = трава и папоротники, 2 = также растения биома.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("allWaterFlora").append('=').append(1).append("\n\n");
      var0.append("# Дальность распространения прибрежных растений от кромки воды (в блоках).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("floraRadius").append('=').append(5).append("\n\n");
      var0.append("# Плотность береговой травы у самой кромки воды (в процентах).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("bankGrassPercent").append('=').append(20).append("\n\n");
      var0.append("# Плотность дополнительных растений биома у кромки воды (в процентах).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("biomeBoostPercent").append('=').append(4).append("\n\n");
      var0.append("# Декорации биома, которые запрещено размещать по берегам.").append('\n');
      var0.append("# Список ID декораций (features) через запятую.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("biomeBoostBlacklist").append('=').append("").append("\n\n");
      var0.append("# Морская трава и водоросли на затопленном дне.").append('\n');
      var0.append("# 0 = выкл, 1 = озёра мода, 2 = также его ручьи, 3 = любая вода.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("underwaterFlora").append('=').append(3).append("\n\n");
      var0.append("# Густота подводных растений (в процентах).").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("underwaterFloraPercent").append('=').append(15).append("\n\n");
      var0.append("# Биомы, в которых не высаживаются подводные растения.").append('\n');
      var0.append("# Список ID биомов или #тегов через запятую.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("underwaterFloraBiomeBlacklist").append('=').append("").append("\n\n");
      var0.append("# ----- Присвоение биома реки -----").append('\n');
      var0.append('\n');
      var0.append("# Присваивать вырезанным ручьям биом реки (для окраски воды, спавна рыб и звуков).").append('\n');
      var0.append("# 0 = выкл, 1 = наиболее подходящий биом реки, 2 = биом реки с окраской окружающей суши.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("rebiomeStreams").append('=').append(1).append("\n\n");
      var0.append("# То же самое для озёр этого мода.").append('\n');
      var0.append("# 0 = выкл, 1 = наиболее подходящий биом реки, 2 = биом реки с окраской окружающей суши.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("rebiomeLakes").append('=').append(1).append("\n\n");
      var0.append("# ===================== ПОВЕДЕНИЕ ВОДЫ: ТЕЧЕНИЯ, ЛОДКИ, МОБЫ И ПРЕДМЕТЫ =====================").append('\n');
      var0.append("# Эти параметры влияют на всю воду в мире, а не только на ручьи этого мода.").append('\n');
      var0.append('\n');
      var0.append("# Придать ручьям реальное нисходящее течение, пока они остаются обычной водой.").append('\n');
      var0.append("# false = ручьи будут состоять из стоячей воды.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("vanillaWaterFlow").append('=').append(true).append("\n\n");
      var0.append("# Создавать согласованное течение к океану для ванильных рек и рек из модов генерации.").append('\n');
      var0.append("# Требует включённого параметра vanillaWaterFlow.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("flowVanillaRivers").append('=').append(true).append("\n\n");
      var0.append("# Сохранять непрерывное единое направление реки при пересечении границ областей карты.").append('\n');
      var0.append("# Её направление может зависеть от того, какая сторона была исследована первой.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("riverSeamAgreement").append('=').append(true).append("\n\n");
      var0.append("# Вода плавно растекается по поверхности водоёмов, позволяя течению непрерывно проходить через них.").append('\n');
      var0.append("# false = стандартное ванильное растекание воды.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("spillOverWater").append('=').append(true).append("\n\n");
      var0.append("# Ограничить растекание только водой, которой этот мод придаёт течение.").append('\n');
      var0.append("# false = каждый блок воды в мире переливается через воду.").append('\n');
      var0.append("# Рекомендуется оставить включённым: при выключении каналы и резервуары из других модов могут переполняться.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("spillOnStreamsOnly").append('=').append(true).append("\n\n");
      var0.append("# Лодки держатся на поверхности текущей воды вместо того, чтобы тонуть.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("boatsFloatOnFlowingWater").append('=').append(true).append("\n\n");
      var0.append("# Рыбы, спруты, дельфины и другие водные существа держат позицию и сопротивляются сносу течением.").append('\n');
      var0.append("# false = течение уносит и их.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("waterMobsIgnoreFlow").append('=').append(true).append("\n\n");
      var0.append("# Мобы, которые всегда сопротивляются течению независимо от автоматических правил.").append('\n');
      var0.append("# Список ID сущностей через запятую.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("mobsHoldInCurrent").append('=').append("").append("\n\n");
      var0.append("# Мобы, которых всегда сносит течением независимо от автоматических правил.").append('\n');
      var0.append("# Список ID сущностей через запятую.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("mobsDriftInCurrent").append('=').append("").append("\n\n");
      var0.append("# Скорость сноса выброшенных предметов течением (в блоках в секунду).").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("itemDriftSpeed").append('=').append(1.5).append("\n\n");
      var0.append("# Сила, с которой текущая вода толкает игрока. 1.0 = ванильный толчок, 0 = без толчка, до 4.0.").append('\n');
      var0.append("# Действует на всю текущую воду в мире. На сервере рассчитывается клиентом каждого игрока.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("playerPush").append('=').append(1.2).append("\n\n");
      var0.append("# Сила, с которой текущая вода толкает лодки и плоты. 1.0 = ванильный толчок, 0 = без толчка, до 4.0.").append('\n');
      var0.append("# Действует на всю текущую воду в мире. На сервере рассчитывается тем, кто управляет лодкой.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("boatPush").append('=').append(2.0).append("\n\n");
      var0.append("# Сила, с которой текущая вода толкает мобов и прочие сущности. 1.0 = ванильный толчок, 0 = без толчка, до 4.0.").append('\n');
      var0.append("# Действует на всю текущую воду в мире (выброшенные предметы настраиваются отдельно).").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("mobPush").append('=').append(1.2).append("\n\n");
      var0.append("# Скорость сноса судов мода Sable течением (в блоках в секунду).").append('\n');
      var0.append("# 0 = выкл. Имеет значение только при установленном моде Sable.").append('\n');
      var0.append("# Sable Extras добавляет собственный снос: при наличии может потребоваться настройка.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("vesselDriftSpeed").append('=').append(30.0).append("\n\n");
      var0.append("# ----- Поведение воды: сторонние моды -----").append('\n');
      var0.append('\n');
      var0.append("# Отрисовка эффектов текущей воды из Physics Mod над ручьями.").append('\n');
      var0.append("# 0 = показывать оба эффекта, 1 = скрыть на ручьях (волны в океане останутся), 3 = эффекты этого мода.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("physicsModWater").append('=').append(3).append("\n\n");
      var0.append("# При установленном моде Flowing Fluids заполнять русла собственной жидкостью мода,").append('\n');
      var0.append("# чтобы вода не вытекала и не исчезала. Не действует без Flowing Fluids.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("bakedFluidWhenFlowingFluids").append('=').append(true).append("\n\n");
      var0.append("# Всегда заполнять русла собственной жидкостью мода вместо обычной воды.").append('\n');
      var0.append("# Для сборок, где другие моды осушают или вытесняют ванильную воду.").append('\n');
      var0.append("# Применяется только к вновь создаваемым областям мира.").append('\n');
      var0.append("forceBakedFluid").append('=').append(false).append("\n\n");
      var0.append("# ===================== ЧАСТИЦЫ И ЗВУКИ (СЕРВЕР) =====================").append('\n');
      var0.append('\n');
      var0.append("# ===================== ПРОЧЕЕ =====================").append('\n');
      var0.append('\n');
      var0.append("# Показывать однократное сообщение Streams Reflowing в чате при первом входе в мир.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("welcomeMessage").append('=').append(true).append("\n\n");
      var0.append("# Серверы Forge/NeoForge: видят ли игроки на сервере брызги и всплески ручьёв.").append('\n');
      var0.append("# true также не позволяет зайти игрокам без установленного мода.").append('\n');
      var0.append("# Вступает в силу при следующем запуске сервера.").append('\n');
      var0.append("serverStreamParticles").append('=').append(true).append("\n\n");
      return var0.toString();
   }

   private static String defaultClientFileContents() {
      StringBuilder var0 = new StringBuilder();
      var0.append("# Streams Reflowing -- клиентские настройки (частицы и звуки). Ключ=значение; правки вступают в силу сразу.").append('\n');
      var0.append("# layout: 2.14 text 74786b88-ru").append('\n');
      var0.append('\n');
      var0.append("# ===================== ЗВУКИ =====================").append('\n');
      var0.append('\n');
      var0.append("# Громкость звуков текущей воды. Ручьи журчат рядом, водопады слышны издалека.").append('\n');
      var0.append("# 0 = без звука, 1 = обычная, 2 = громко. Подчиняется ползунку «Окружение» в настройках звука игры.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("waterAmbience").append('=').append(1.0).append("\n\n");
      var0.append("# ===================== ЧАСТИЦЫ =====================").append('\n');
      var0.append('\n');
      var0.append("# Пузыри на течении и брызги при падении воды.").append('\n');
      var0.append("# 0 = выкл, 1 = пузыри исчезают на месте (наименьшая нагрузка), 2 = пузыри плывут вниз по течению.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("streamBubbles").append('=').append(2).append("\n\n");
      var0.append("# При установленных модах Effective или Particular: размещать их пену у подножия собственных").append('\n');
      var0.append("# водопадов, а их встроенное распознавание водопадов отключать на ручьях этого мода.").append('\n');
      var0.append("# false = каждый мод работает изолированно.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("partnerParticles").append('=').append(true).append("\n\n");
      var0.append("# Чья пена отображается у подножия водопада при включённой интеграции.").append('\n');
      var0.append("# 0 = нет, 1 = Effective / Particular, 2 = этого мода, 3 = оба варианта.").append('\n');
      var0.append("# Имеет значение только при наличии Effective или Particular.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("waterfallFoam").append('=').append(3).append("\n\n");
      var0.append("# Веточки и листья, плывущие по течению на поверхности воды.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("streamDebris").append('=').append(true).append("\n\n");
      var0.append("# Размер каждой капли брызг (множитель).").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("particleSize").append('=').append(1.0).append("\n\n");
      var0.append("# Количество образующихся брызг (множитель).").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("particleDensity").append('=').append(1.0).append("\n\n");
      var0.append("# Ограничение числа частиц ручьёв за такт (тик). Уменьшите при падении FPS рядом с ручьями.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("maxStreamParticlesPerTick").append('=').append(1000).append("\n\n");
      var0.append("# Радиус поиска водопадов (в блоках) для отображения брызг на расстоянии.").append('\n');
      var0.append("# 0 = только водопады в непосредственной близости.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("streamParticleScanRadius").append('=').append(256).append("\n\n");
      var0.append("# Создавать брызги и на ванильных водопадах, а не только на ручьях этого мода.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("particlesOnVanillaWater").append('=').append(true).append("\n\n");
      var0.append("# ===================== ПРОЧЕЕ =====================").append('\n');
      var0.append('\n');
      var0.append("# Отображать строку прогресса генерации Streams Reflowing на экране загрузки мира.").append('\n');
      var0.append("# Вступает в силу сразу.").append('\n');
      var0.append("loadingNotice").append('=').append(true).append("\n\n");
      return var0.toString();
   }

   private static boolean parseBool(Properties var0, String var1, boolean var2) {
      String var3 = var0.getProperty(var1);
      if (var3 == null) {
         return var2;
      } else if ((var3 = var3.trim()).equalsIgnoreCase("true")) {
         return true;
      } else {
         return var3.equalsIgnoreCase("false") ? false : var2;
      }
   }

   private static int parseMode(Properties var0, String var1, int var2, int var3) {
      String var4 = var0.getProperty(var1);
      if (var4 == null) {
         return var2;
      }

      if ((var4 = var4.trim()).equalsIgnoreCase("false")) {
         return 0;
      }

      if (var4.equalsIgnoreCase("true")) {
         return 2;
      }

      try {
         int var5 = Integer.parseInt(var4);
         return var5 >= 0 && var5 <= var3 ? var5 : var2;
      } catch (NumberFormatException var6) {
         return var2;
      }
   }

   private static int levelOr(int var0, int var1, int var2) {
      return var0 >= 0 && var0 <= var2 ? var0 : var1;
   }

   private static int parseInt(Properties var0, String var1, int var2) {
      String var3 = var0.getProperty(var1);
      if (var3 == null) {
         return var2;
      }

      try {
         return Integer.parseInt(var3.trim());
      } catch (NumberFormatException var5) {
         return var2;
      }
   }

   private static double parseDouble(Properties var0, String var1, double var2) {
      String var4 = var0.getProperty(var1);
      if (var4 == null) {
         return var2;
      }

      try {
         return Double.parseDouble(var4.trim());
      } catch (NumberFormatException var6) {
         return var2;
      }
   }

   private static QualityPreset parsePreset(String var0) {
      if (var0 == null) {
         return D_QUALITY_PRESET;
      }

      try {
         return QualityPreset.valueOf(var0.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var2) {
         return D_QUALITY_PRESET;
      }
   }

   private static int parsePlateSize(String var0) {
      if (var0 != null && !var0.trim().equalsIgnoreCase("auto") && !var0.trim().isEmpty()) {
         try {
            int var1 = Integer.parseInt(var0.trim());
            return var1 < 0 ? -1 : Math.max(256, Math.min(8192, var1));
         } catch (NumberFormatException var2) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public static boolean generateStreams() {
      return generateStreams;
   }

   public static boolean offloadRegionBuilds() {
      return offloadRegionBuilds;
   }

   public static boolean helpPrepareRegions() {
      return helpPrepareRegions;
   }

   public static int helpRegionsAtOnce() {
      return helpRegionsAtOnce;
   }

   public static boolean generateLakes() {
      return generateLakes;
   }

   public static boolean generateRivers() {
      return generateStreams || generateLakes;
   }

   public static int minStreamLength() {
      return minStreamLength;
   }

   public static int minLengthScope() {
      return minLengthScope;
   }

   public static int riverDrainageSize() {
      return riverDrainageSize;
   }

   public static QualityPreset qualityPreset() {
      return qualityPreset;
   }

   public static boolean reterraforgedCompat() {
      return reterraforgedCompat;
   }

   public static int physicsModWater() {
      return physicsModWater;
   }

   public static String outletBiomes() {
      return outletBiomes;
   }

   public static boolean fastOceanSkip() {
      return true;
   }

   public static int gridSpacing() {
      return PresetOverrides.gridSpacing(qualityPreset.gridSpacing);
   }

   public static int terrainFit() {
      int var0 = terrainFit < 0 ? qualityPreset.terrainFit : terrainFit;
      return var0 <= 0 ? 0 : (var0 <= 2 ? 2 : 4);
   }

   public static boolean legacyShaping() {
      return legacyStreamShaping;
   }

   public static boolean adaptiveGridRead() {
      return adaptiveGridRead;
   }

   public static boolean alignReadPoints() {
      return alignReadPoints;
   }

   public static int plateScale() {
      return watershedSize.blocks();
   }

   public static int terrainSampleSpacing() {
      return PresetOverrides.terrainSampleSpacing(qualityPreset.terrainSampleSpacing);
   }

   public static int valleySnapRadius() {
      return PresetOverrides.valleySnapRadius(qualityPreset.valleySnapRadius);
   }

   public static int lakeSampleSpacing() {
      return PresetOverrides.lakeSampleSpacing(qualityPreset.lakeSampleSpacing);
   }

   public static int prefetchRadius() {
      return PresetOverrides.prefetchRadius(qualityPreset.prefetchRadius);
   }

   public static boolean clearFloatingSnow() {
      return true;
   }

   public static double wetnessMultiplier() {
      return wetnessMultiplier;
   }

   public static double wetnessDryFloor() {
      return wetnessDryFloor;
   }

   public static double growDensity() {
      return growDensity;
   }

   public static double lakeMinArea() {
      return lakeMinArea;
   }

   public static double lakeMaxArea() {
      return lakeMaxArea;
   }

   public static double widthScale() {
      return widthScale;
   }

   public static double baseWidth() {
      return baseWidth;
   }

   public static double maxWidth() {
      return maxWidth;
   }

   public static double meanderStrength() {
      return meanderStrength;
   }

   public static double streamDepth() {
      return streamDepth;
   }

   public static double widthFollowsSlope() {
      return widthFollowsSlope;
   }

   public static double widthFollowsClimate() {
      return widthFollowsClimate;
   }

   public static double widthVariation() {
      return widthVariation;
   }

   public static double depthFollowsTerrain() {
      return depthFollowsTerrain;
   }

   public static double bendAsymmetry() {
      return bendAsymmetry;
   }

   public static double channelBars() {
      return channelBars;
   }

   public static double deltaMouths() {
      return deltaMouths;
   }

   public static boolean containmentBerm() {
      return containmentBerm;
   }

   public static int vegetation() {
      return vegetation;
   }

   public static int allWaterFlora() {
      return allWaterFlora;
   }

   public static int bankFeatures() {
      return bankFeatures;
   }

   public static int allWaterBanks() {
      return allWaterBanks;
   }

   public static boolean customBanks() {
      return customBanks;
   }

   public static boolean fastTerrainEstimate() {
      return fastTerrainEstimate;
   }

   public static int terrainSamplingMode() {
      return qualityPreset.terrainSamplingMode;
   }

   public static int floraRadius() {
      return Math.max(1, floraRadius);
   }

   public static int biomeBoostPercent() {
      return Math.max(0, Math.min(100, biomeBoostPercent));
   }

   public static String biomeBoostBlacklist() {
      return biomeBoostBlacklist;
   }

   public static String mobsHoldInCurrent() {
      return mobsHoldInCurrent;
   }

   public static String mobsDriftInCurrent() {
      return mobsDriftInCurrent;
   }

   public static String streamBiomeBlacklist() {
      return streamBiomeBlacklist;
   }

   public static int bankGrassPercent() {
      return Math.max(0, Math.min(100, bankGrassPercent));
   }

   public static int underwaterFlora() {
      return underwaterFlora;
   }

   public static int underwaterFloraPercent() {
      return Math.max(0, Math.min(100, underwaterFloraPercent));
   }

   public static String underwaterFloraBiomeBlacklist() {
      return underwaterFloraBiomeBlacklist;
   }

   public static boolean fastChunkLoading() {
      return fastChunkLoading;
   }

   public static boolean spillOverWater() {
      return spillOverWater;
   }

   public static boolean spillOnStreamsOnly() {
      return spillOnStreamsOnly;
   }

   public static boolean boatsFloatOnFlowingWater() {
      return boatsFloatOnFlowingWater;
   }

   public static boolean waterMobsIgnoreFlow() {
      return waterMobsIgnoreFlow;
   }

   public static double bankNoise() {
      return bankNoise;
   }

   public static boolean serverStreamParticles() {
      return serverStreamParticles;
   }

   public static boolean serverStreamParticlesFromFile() {
      ensureLoaded();
      return serverStreamParticles;
   }

   public static void ensureLoaded() {
      if (!loadedOnce) {
         Class<StreamsSettings> var1 = StreamsSettings.class;
         synchronized (StreamsSettings.class) {
            if (!loadedOnce) {
               importToml(Services.PLATFORM.configDir());
               load();
               loadedOnce = true;
            }
         }
      }
   }

   public static QualityPreset preset() {
      return qualityPreset();
   }

   public static void refreshHotFlags() {
   }

   public static boolean bermYieldToStreams() {
      return true;
   }

   static void importToml(Path var0) {
      try {
         carryOver(var0.resolve("streamsreflowing-common.toml"), var0.resolve("streamsreflowing.properties"), true);
         carryOver(var0.resolve("streamsreflowing-client.toml"), var0.resolve("streamsreflowing-client.properties"), false);
      } catch (RuntimeException var2) {
         Constants.LOG
            .warn("Streams Reflowing: could not carry the old settings file over ({}); starting from defaults. The old file is untouched.", var2.toString());
      }
   }

   private static void carryOver(Path var0, Path var1, boolean var2) {
      if (Files.isRegularFile(var0) && !Files.exists(var1)) {
         LinkedHashMap var3 = new LinkedHashMap();

         try {
            for (String var6 : Files.readAllLines(var0)) {
               String var7 = var6.trim();
               int var4;
               if (!var7.isEmpty() && var7.charAt(0) != '#' && var7.charAt(0) != '[' && (var4 = var7.indexOf(61)) > 0) {
                  String var8 = var7.substring(0, var4).trim();
                  String var9 = var7.substring(var4 + 1).trim();
                  if (var9.length() >= 2 && var9.charAt(0) == '"' && var9.charAt(var9.length() - 1) == '"') {
                     var9 = var9.substring(1, var9.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
                  }

                  var3.put(var8, var9);
               }
            }

            writeDefaults(var1, var2 ? defaultFileContents() : defaultClientFileContents());
            HashSet var12 = new HashSet();

            for (String var14 : Files.readAllLines(var1)) {
               int var11 = var14.indexOf(61);
               if (var11 > 0 && var14.charAt(0) != '#') {
                  var12.add(var14.substring(0, var11).trim());
               }
            }

            var3.keySet().retainAll(var12);
            if (!var3.isEmpty()) {
               editProperties(var1, var3);
            }

            Files.move(var0, var0.resolveSibling(var0.getFileName() + ".imported"), StandardCopyOption.REPLACE_EXISTING);
            Constants.LOG
               .info(
                  "Streams Reflowing: settings moved from {} to {} ({} values carried over).",
                  new Object[]{var0.getFileName(), var1.getFileName(), var3.size()}
               );
         } catch (IOException var10) {
            throw new UncheckedIOException(var10);
         }
      }
   }

   public static double playerPush() {
      return playerPush;
   }

   public static double boatPush() {
      return boatPush;
   }

   public static double mobPush() {
      return mobPush;
   }

   public static double itemDriftSpeed() {
      return itemDriftSpeed;
   }

   public static double vesselDriftSpeed() {
      return vesselDriftSpeed;
   }

   public static boolean vanillaWaterFlow() {
      return vanillaWaterFlow;
   }

   private static boolean flowingFluidsLoaded() {
      if (flowingFluidsLoaded == null) {
         try {
            flowingFluidsLoaded = Services.PLATFORM.isModLoaded("flowing_fluids");
         } catch (Throwable var1) {
            flowingFluidsLoaded = false;
         }
      }

      return flowingFluidsLoaded;
   }

   public static boolean bakedStreamFluid() {
      return forceBakedFluid || bakedFluidWhenFlowingFluids && (flowingFluidsLoaded() || PhysicsModCompat.physicsModInstalled());
   }

   public static boolean forceBakedFluid() {
      return forceBakedFluid;
   }

   public static boolean riverSeamAgreement() {
      return riverSeamAgreement;
   }

   public static boolean flowVanillaRivers() {
      return flowVanillaRivers;
   }

   public static double waterAmbience() {
      return Math.max(0.0, Math.min(2.0, waterAmbience));
   }

   public static boolean partnerParticles() {
      return partnerParticles;
   }

   public static int waterfallFoam() {
      return waterfallFoam;
   }

   public static int streamBubbles() {
      return Math.max(0, Math.min(2, streamBubbles));
   }

   public static int maxStreamParticles() {
      return Math.max(0, maxStreamParticles);
   }

   public static int streamParticleScanRadius() {
      return Math.max(0, Math.min(512, streamParticleScanRadius));
   }

   public static boolean particlesOnVanillaWater() {
      return particlesOnVanillaWater;
   }

   public static boolean streamDebris() {
      return streamDebris;
   }

   public static double particleSize() {
      return particleSize;
   }

   public static double particleRate() {
      return particleRate;
   }

   public static boolean loadingNotice() {
      return loadingNotice;
   }

   public static boolean confineToBanks() {
      return false;
   }

   public static boolean connectNearbyWater() {
      return connectNearbyWater;
   }

   public static int connectRadius() {
      return connectRadius >= 8 ? connectRadius : 24;
   }

   public static boolean sinkRooted() {
      return false;
   }

   public static int rebiomeStreams() {
      return rebiomeStreams;
   }

   public static int rebiomeLakes() {
      return rebiomeLakes;
   }

   public static boolean welcomeMessage() {
      return welcomeMessage;
   }

   private static String digestOf(Path var0) {
      try {
         byte[] var1 = Files.readAllBytes(var0);
         MessageDigest var2 = MessageDigest.getInstance("MD5");
         return Base64.getEncoder().encodeToString(var2.digest(var1));
      } catch (Exception var3) {
         return "";
      }
   }

   private static void recordDigests() {
      Path var0 = Services.PLATFORM.configDir();
      LOADED_DIGEST.put("streamsreflowing.properties", digestOf(var0.resolve("streamsreflowing.properties")));
      LOADED_DIGEST.put("streamsreflowing-client.properties", digestOf(var0.resolve("streamsreflowing-client.properties")));
   }

   private static boolean changedOnDisk() {
      Path var0 = Services.PLATFORM.configDir();

      for (String var4 : new String[]{"streamsreflowing.properties", "streamsreflowing-client.properties"}) {
         String var5 = digestOf(var0.resolve(var4));
         if (!var5.isEmpty() && !var5.equals(LOADED_DIGEST.get(var4))) {
            return true;
         }
      }

      return false;
   }

   public static synchronized void startWatching() {
      if (watchThread == null) {
         recordDigests();
         Thread var0 = new Thread(() -> {
            Path var0x = Services.PLATFORM.configDir();

            try (WatchService var1 = var0x.getFileSystem().newWatchService()) {
               var0x.register(var1, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

               while (!Thread.currentThread().isInterrupted()) {
                  WatchKey var2 = var1.take();
                  boolean var3 = false;

                  for (WatchEvent var5 : var2.pollEvents()) {
                     if (var5.context() instanceof Path var7) {
                        String var8 = var7.getFileName().toString();
                        var3 |= "streamsreflowing.properties".equals(var8) || "streamsreflowing-client.properties".equals(var8);
                     }
                  }

                  if (!var2.reset()) {
                     break;
                  }

                  if (var3) {
                     Thread.sleep(300L);
                     if (changedOnDisk()) {
                        load();
                        recordDigests();
                        Constants.LOG.info("Streams Reflowing: config reloaded from disk.");
                     }
                  }
               }
            } catch (InterruptedException var11) {
               Thread.currentThread().interrupt();
            } catch (Exception var12) {
               Constants.LOG.debug("Streams Reflowing: config file watching unavailable ({}).", var12.toString());
            }
         }, "streamsreflowing-config-watch");
         var0.setDaemon(true);
         var0.start();
         watchThread = var0;
      }
   }
}
