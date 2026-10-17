package dev.streamsreflowing.client;

import net.minecraft.client.Minecraft;

public final class ClientTintDebug {
   private ClientTintDebug() {
   }

   public static void run() {
      Minecraft.getInstance().execute(StreamTint::debugMap);
   }
}
