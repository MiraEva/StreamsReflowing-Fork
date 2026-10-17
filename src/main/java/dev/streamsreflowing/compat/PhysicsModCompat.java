package dev.streamsreflowing.compat;

import dev.streamsreflowing.platform.Services;

public final class PhysicsModCompat {
   private static boolean present;
   private static boolean lookedUp;

   private PhysicsModCompat() {
   }

   public static boolean noImposedRenderFlow() {
      int mode = Services.PLATFORM.physicsModWater();
      return mode == 0 ? false : mode == 2 || physicsModInstalled();
   }

   public static boolean physicsModInstalled() {
      if (!lookedUp) {
         lookedUp = true;

         try {
            Class.forName("net.diebuddies.physics.StarterClient", false, PhysicsModCompat.class.getClassLoader());
            present = true;
         } catch (ClassNotFoundException | LinkageError e) {
            present = false;
         }
      }

      return present;
   }

   public static boolean keepOceanOffStreams() {
      return Services.PLATFORM.physicsModWater() == 3;
   }
}
