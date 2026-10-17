package dev.streamsreflowing.flow;

import dev.streamsreflowing.config.StreamsSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;

public final class CurrentPush {
   private CurrentPush() {
   }

   public static double factor(Entity entity) {
      if (entity instanceof Player) {
         return StreamsSettings.playerPush();
      } else if (entity instanceof Boat) {
         return StreamsSettings.boatPush();
      } else {
         return entity instanceof ItemEntity ? 1.0 : StreamsSettings.mobPush();
      }
   }
}
