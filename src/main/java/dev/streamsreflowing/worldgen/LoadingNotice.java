package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.RiverEngine;

public final class LoadingNotice {
   private static final double a = 20.0;
   private static final long b = 3000000000L;
   private static final long c = 1000000000L;
   private static final long d = 1200000000L;
   private static long e;
   private static long f;
   private static long g;
   private static long h;

   private LoadingNotice() {
   }

   public static synchronized String currentText() {
      long var0 = System.nanoTime();
      if (var0 - e > 3000000000L) {
         f = 0L;
         h = 0L;
      }

      e = var0;
      boolean var2 = RiverEngine.buildingNetworks();
      if (f == 0L) {
         if (!var2) {
            return null;
         }

         f = var0;
         g = var0;
      }

      if (var2) {
         g = var0;
      }

      if (h == 0L && !var2 && var0 - g > 1000000000L) {
         h = var0;
      }

      if (h == 0L) {
         double var3 = (var0 - f) / 1.0E9;
         int var5 = (int)Math.min(99L, Math.round(99.0 * (1.0 - Math.exp(-var3 / 20.0))));
         return "Streams Reflowing is weaving your waterways... [" + var5 + "%]";
      } else {
         return var0 - h < 1200000000L
            ? "Streams Reflowing is weaving your waterways... [100%]"
            : "Streams Reflowing has woven your waterways, generating world...";
      }
   }
}
