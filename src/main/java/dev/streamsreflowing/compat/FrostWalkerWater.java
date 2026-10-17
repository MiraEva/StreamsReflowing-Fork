package dev.streamsreflowing.compat;

public final class FrostWalkerWater {
   private static volatile Thread open;

   public static void open() {
      open = Thread.currentThread();
   }

   public static void close() {
      open = null;
   }

   public static boolean isOpen() {
      return open == Thread.currentThread();
   }

   private FrostWalkerWater() {
   }
}
