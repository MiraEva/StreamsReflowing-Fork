package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.config.WatershedSize;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class WatershedPin {
   private static final String a = "watershed.pin";
   private static final int b = 12;
   private static final int c = 64;
   private static final int d = 12;

   private WatershedPin() {
   }

   static int a(Path var0, long var1, int var3) {
      try {
         Path var4 = var0.resolve("watershed.pin");
         if (Files.isRegularFile(var4)) {
            int var5 = Integer.parseInt(Files.readString(var4).trim());
            if (var5 >= 64) {
               if (var5 != var3) {
                  Constants.LOG
                     .info(
                        "Streams Reflowing: this world keeps its watershed size of {} blocks; the watershedSize setting ({}) applies to new worlds.",
                        var5,
                        var3
                     );
               }

               return var5;
            }
         }

         int var9 = var3;
         String var6 = "the watershedSize setting";
         int var7 = b(var0, var1, var3);
         if (var7 > 0) {
            var9 = var7;
            var6 = "this world's stored streams";
         }

         Files.createDirectories(var0);
         Files.writeString(var4, var9 + System.lineSeparator());
         Constants.LOG.info("Streams Reflowing: this world's watershed size is {} blocks (from {}); it keeps that size for good.", var9, var6);
         return var9;
      } catch (Exception var8) {
         Constants.LOG
            .warn("Streams Reflowing: could not read or write the world's watershed pin in {}; using the setting ({})", new Object[]{var0, var3, var8});
         return var3;
      }
   }

   private static int b(Path var0, long var1, int var3) throws IOException {
      if (!Files.isDirectory(var0)) {
         return -1;
      }

       List<Path> var4;
       try (Stream<Path> var5 = Files.list(var0)) {
         var4 = var5.filter(var0x -> {
            String var1x = var0x.getFileName().toString();
            return var1x.startsWith("r.") && var1x.endsWith(".srn");
         }).sorted().limit(48L).toList();
      }

      if (var4.isEmpty()) {
         return -1;
      }

      StreamsConfig var28 = StreamsConfig.defaults();
      FileNetworkStore var6 = new FileNetworkStore(var0, var1, var28);
       ArrayList<double[]> var7 = new ArrayList<>();
       ArrayList<PlateKey> var8 = new ArrayList<>();
      int var9 = 0;

      for (Path var11 : var4) {
         if (var9 >= 12) {
            break;
         }

         String[] var12 = var11.getFileName().toString().split("\\.");
         if (var12.length == 4) {
            PlateKey var13;
            try {
               var13 = new PlateKey(Integer.parseInt(var12[1]), Integer.parseInt(var12[2]));
            } catch (NumberFormatException var27) {
               continue;
            }

            RiverNetwork var14 = var6.load(var13);
            if (var14 != null && !var14.segments().isEmpty()) {
               List var15 = var14.segments();
               int var16 = Math.max(1, var15.size() / 64);

               for (int var17 = 0; var17 < var15.size(); var17 += var16) {
                  RiverSegment var18 = (RiverSegment)var15.get(var17);
                  var7.add(new double[]{(var18.x1() + var18.x2()) * 0.5, (var18.z1() + var18.z2()) * 0.5});
                  var8.add(var13);
               }

               var9++;
            }
         }
      }

      if (var7.size() < 12) {
         return -1;
      }

      double var29 = var28.plateJitter();
       ArrayList<Integer> var30 = new ArrayList<>();

      for (WatershedSize var36 : WatershedSize.values()) {
         var30.add(var36.blocks());
      }

      if (!var30.contains(var3)) {
         var30.add(var3);
      }

      var30.sort(Integer::compare);
      double var32 = -1.0;
      int var35 = -1;

      for (int var39 : var30) {
         double var41 = a(var1, var39, var29, var7, var8);
         if (var41 > var32 + 0.005) {
            var32 = var41;
            var35 = var39;
         }
      }

      if (var32 >= 0.97) {
         return var35;
      }

      ArrayList var38 = new ArrayList();
      ArrayList var40 = new ArrayList();
      int var42 = Math.max(1, var7.size() / 160);

      for (int var19 = 0; var19 < var7.size(); var19 += var42) {
         var38.add((double[])var7.get(var19));
         var40.add((PlateKey)var8.get(var19));
      }

      double var43 = -1.0;
      int var21 = -1;

      for (int var22 = 256; var22 <= 8192; var22++) {
         double var23 = a(var1, var22, var29, var38, var40);
         if (var23 > var43 + 1.0E-9 || Math.abs(var23 - var43) <= 1.0E-9 && Math.abs(var22 - var3) < Math.abs(var21 - var3)) {
            var43 = var23;
            var21 = var22;
         }
      }

      if (var43 >= 0.9 && var43 > var32) {
         return var21;
      } else {
         return var32 >= 0.8 ? var35 : -1;
      }
   }

   private static double a(long var0, int var2, double var3, List<double[]> var5, List<PlateKey> var6) {
      PlateLattice var7 = new PlateLattice(var0, var2, var3);
      int var8 = 0;

      for (int var9 = 0; var9 < var5.size(); var9++) {
         double[] var10 = (double[])var5.get(var9);
         if (var7.plateAt(var10[0], var10[1]).equals(var6.get(var9))) {
            var8++;
         }
      }

      return (double)var8 / var5.size();
   }
}
