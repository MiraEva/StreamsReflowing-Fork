package dev.streamsreflowing.command;

import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class WelcomedPlayers extends SavedData {
   public static final String ID = "streamsreflowing_welcomed";
   private boolean welcomed;

   public static Function<CompoundTag, WelcomedPlayers> loader() {
      return WelcomedPlayers::load;
   }

   public static WelcomedPlayers create() {
      return new WelcomedPlayers();
   }

   private WelcomedPlayers() {
   }

   private static WelcomedPlayers load(CompoundTag tag) {
      WelcomedPlayers data = new WelcomedPlayers();
      data.welcomed = tag.getBoolean("welcomed");
      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      tag.putBoolean("welcomed", this.welcomed);
      return tag;
   }

   public boolean markWelcomed() {
      if (!this.welcomed) {
         this.welcomed = true;
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }
}
