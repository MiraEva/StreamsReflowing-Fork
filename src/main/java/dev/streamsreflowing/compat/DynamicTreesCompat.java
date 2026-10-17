package dev.streamsreflowing.compat;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;

public final class DynamicTreesCompat {
   private static final boolean STAND_DOWN = Boolean.getBoolean("streamsreflowing.dtnoprevent");
   private static final String[] GROUND_FINDERS = new String[]{
      "com.dtteam.dynamictrees.worldgen.OverworldGroundFinder", "com.ferreusveritas.dynamictrees.worldgen.OverworldGroundFinder"
   };
   private static volatile boolean present;
   private static volatile boolean armed;
   private static volatile boolean attached;
   private static final AtomicBoolean REPORTED = new AtomicBoolean();

   private DynamicTreesCompat() {
   }

   public static void present(boolean value) {
      present = value;
   }

   public static void armed() {
      armed = true;
   }

   public static void attached() {
      attached = true;
   }

   public static void reportOnce() {
      if (present && REPORTED.compareAndSet(false, true)) {
         if (armed) {
            for (String name : GROUND_FINDERS) {
               try {
                  Class.forName(name, false, DynamicTreesCompat.class.getClassLoader());
               } catch (Throwable var5) {
               }
            }
         }

         if (attached) {
            Constants.LOG.info("Streams Reflowing: Dynamic Trees is installed; its trees will not be planted inside this mod's lakes.");
         } else {
            Constants.LOG
               .warn(
                  "Streams Reflowing: Dynamic Trees is installed, but this build could not keep its trees out of this mod's lakes -- trees may generate standing in the water. Please report this together with the Dynamic Trees version you are using."
               );
         }
      }
   }

   public static boolean refuse(LevelAccessor accessor, BlockPos pos) {
      if (STAND_DOWN || accessor == null || pos == null) {
         return false;
      } else if (accessor instanceof WorldGenLevel worldGen) {
         ServerLevel level = worldGen.getLevel();
         RiverEngine engine = StreamsWorldgen.engineIfPresent(level);
         return engine == null ? false : RiverCarverFeature.inLakeWater(engine, pos.getX(), pos.getZ());
      } else {
         return false;
      }
   }
}
