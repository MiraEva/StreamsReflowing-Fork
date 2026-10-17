package dev.streamsreflowing.client;

import dev.streamsreflowing.core.math.Hashing;
import net.minecraft.world.level.Level;

public final class CascadeBudget {
   private static final int PER_TICK = 48;
   private static final int REST = 1;
   private static final double FLOOR = 0.25;
   private static final int SLOTS = 1024;
   private static final long[] footKey = new long[1024];
   private static final long[] footTick = new long[1024];
   private static long tick = Long.MIN_VALUE;
   private static int allowance;
   private static int rest;
   private static int spent;
   private static boolean minimal;
   public static volatile boolean off;
   private static Class<?> mcClass;
   private static boolean mcLooked;

   private CascadeBudget() {
   }

   public static boolean claim(Level level, int x, int y, int z) {
      if (off) {
         return true;
      }

      long now = level.getGameTime();
      if (now != tick) {
         tick = now;
         spent = 0;
         float wet = wetness(level);
         allowance = Math.max(1, (int)(48.0 * (1.0 - 0.75 * wet)));
         rest = (int)Math.ceil(1.0 / (1.0 - 0.75 * wet));
         minimal = minimalParticles();
      }

      if (minimal) {
         return false;
      }

      if (spent >= allowance) {
         return false;
      }

      long key = Hashing.mix((x & 67108863L) << 38 ^ (z & 67108863L) << 12 ^ y & 4095L);
      int slot = (int)key & 1023;
      if (footKey[slot] == key && now - footTick[slot] < rest) {
         return false;
      }

      footKey[slot] = key;
      footTick[slot] = now;
      return true;
   }

   public static void spend(int particles) {
      if (!off) {
         spent += particles;
      }
   }

   private static float wetness(Level level) {
      float wet = Math.max(level.getRainLevel(1.0F), level.getThunderLevel(1.0F));
      return wet < 0.0F ? 0.0F : (wet > 1.0F ? 1.0F : wet);
   }

   private static boolean minimalParticles() {
      try {
         if (!mcLooked) {
            mcLooked = true;
            mcClass = Class.forName("net.minecraft.client.Minecraft");
         }

         if (mcClass == null) {
            return false;
         }

         Object mc = mcClass.getMethod("getInstance").invoke(null);
         Object options = mcClass.getField("options").get(mc);
         Object value = options.getClass().getMethod("particles").invoke(options);
         return value.getClass().getMethod("get").invoke(value) instanceof Enum<?> e && e.ordinal() >= 2;
      } catch (Throwable t) {
         mcClass = null;
         return false;
      }
   }
}
