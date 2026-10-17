package dev.streamsreflowing.core.config;

import java.util.Locale;

public enum WatershedSize {
   BLOCKS_256(256),
   BLOCKS_512(512),
   BLOCKS_768(768),
   BLOCKS_1024(1024),
   BLOCKS_1536(1536),
   BLOCKS_2048(2048),
   BLOCKS_4096(4096),
   BLOCKS_8192(8192);

   public static final WatershedSize DEFAULT = BLOCKS_768;
   private final int a;

   WatershedSize(int var3) {
      this.a = var3;
   }

   public int blocks() {
      return this.a;
   }

   public static WatershedSize nearest(int var0) {
      if (var0 <= 0) {
         return DEFAULT;
      }

      WatershedSize var1 = DEFAULT;
      int var2 = Integer.MAX_VALUE;

      for (WatershedSize var6 : values()) {
         int var7 = Math.abs(var6.a - var0);
         if (var7 < var2) {
            var2 = var7;
            var1 = var6;
         }
      }

      return var1;
   }

   public static int formerPresetBlocks(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toUpperCase(Locale.ROOT);

      return switch (var1) {
         case "POTATO" -> 512;
         case "HIGH" -> 1024;
         case "EXTREME" -> 1536;
         case "MAX" -> 2048;
         default -> 768;
      };
   }

   public static WatershedSize parse(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var1 = var0.trim().toUpperCase(Locale.ROOT);

         for (WatershedSize var5 : values()) {
            if (var5.name().equals(var1) || Integer.toString(var5.a).equals(var1)) {
               return var5;
            }
         }

         try {
            return nearest(Integer.parseInt(var1));
         } catch (NumberFormatException var6) {
            return DEFAULT;
         }
      } else {
         return DEFAULT;
      }
   }
}
