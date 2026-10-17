package dev.streamsreflowing.client;

import net.minecraft.world.level.Level;

public final class SprayBudget {
   private static final double FLOOR = 0.25;

   private SprayBudget() {
   }

   public static int inWeather(int budget, Level level) {
      if (budget > 0 && level != null) {
         float wet = Math.max(level.getRainLevel(1.0F), level.getThunderLevel(1.0F));
         if (wet <= 0.0F) {
            return budget;
         }

         if (wet > 1.0F) {
            wet = 1.0F;
         }

         int kept = (int)(budget * (1.0 - 0.75 * wet));
         return Math.max(1, kept);
      } else {
         return budget;
      }
   }
}
