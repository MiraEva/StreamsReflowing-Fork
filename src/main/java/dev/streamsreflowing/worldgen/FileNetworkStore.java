package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.Relief;
import dev.streamsreflowing.core.api.Sink;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.config.HydraulicGeometry;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class FileNetworkStore implements NetworkStore {
   private static final int a = 1397902897;
   private static final int b = 50;
   private static final int c = 2;
   private static final int d = 32;
   private static final int e = 29;
   private static final long f = 80L;
   private static final long g = 32L;
   private static final long h = 48L;
   private final Path i;
   private final StreamsConfig j;
   private final long k;
   private volatile boolean l;
   private volatile boolean m;
   private volatile boolean n;
   private static final long o = 6L;

   public FileNetworkStore(Path var1, long var2, StreamsConfig var4) {
      this.i = var1;
      this.j = var4;
      this.k = a(var2, var4);
   }

   public long fingerprint() {
      return this.k;
   }

   @Override
   public RiverNetwork load(PlateKey var1) {
      return this.a(this.i.resolve(a(var1)), var1);
   }

   public Path fileOf(PlateKey var1) {
      return this.i.resolve(a(var1));
   }

   public RiverNetwork adopt(PlateKey var1, byte[] var2, Predicate<RiverNetwork> var3) {
      Path var4 = this.i.resolve(a(var1));
      Path var5 = this.i.resolve(a(var1) + ".in");

      try {
         this.a();
         if (Files.exists(var4)) {
            return null;
         } else {
            Files.write(var5, var2);
            RiverNetwork var6 = this.a(var5, var1);
            if (var6 != null && var3.test(var6)) {
               Files.move(var5, var4);
               return var6;
            } else {
               return null;
            }
         }
      } catch (IOException | RuntimeException var19) {
         return null;
      } finally {
         try {
            Files.deleteIfExists(var5);
         } catch (IOException var18) {
         }
      }
   }

   private RiverNetwork a(Path var1, PlateKey var2) {
      if (!Files.isRegularFile(var1)) {
         return null;
      }

      try (DataInputStream var3 = new DataInputStream(new BufferedInputStream(Files.newInputStream(var1)))) {
         long var4 = Files.size(var1);
         if (var3.readInt() != 1397902897) {
            return null;
         }

         int var6 = var3.readInt();
         if (var6 < 29 || var6 > 32) {
            if (!this.n) {
               this.n = true;
               Constants.LOG
                  .warn(
                     "Streams Reflowing: some of this world's stored streams were saved by a version this one cannot read; those regions are being prepared again."
                  );
            }

            return null;
         } else {
            long var7 = var3.readLong();
            if (var7 != this.k && !this.m) {
               this.m = true;
               Constants.LOG
                  .info(
                     "Streams Reflowing: this world's streams were built under different settings; keeping them so existing land is unchanged. New settings apply to land not generated yet."
                  );
            }

            int var9 = var3.readInt();
            if (var9 < 0 || var9 * 80L > var4) {
               return null;
            }

            ArrayList var10 = new ArrayList(var9);

            for (int var11 = 0; var11 < var9; var11++) {
               var10.add(
                  new RiverSegment(
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble(),
                     var3.readDouble()
                  )
               );
            }

            int var32 = var3.readInt();
            if (var32 < 0 || var32 * 32L > var4) {
               return null;
            }

            ArrayList var12 = new ArrayList(var32);

            for (int var13 = 0; var13 < var32; var13++) {
               int var14 = var3.readInt();
               int var15 = var3.readInt();
               int var16 = var3.readInt();
               int var17 = var3.readInt();
               int var18 = var3.readInt();
               int var19 = var3.readInt();
               int var20 = var3.readInt();
               int var21 = var3.readInt();
               if (var21 < 0 || var21 * 8L > var4) {
                  return null;
               }

               long[] var22 = new long[var21];

               for (int var23 = 0; var23 < var21; var23++) {
                  var22[var23] = var3.readLong();
               }

               var12.add(new Lake(var14, var15, var16, var17, var18, var19, var20, var22));
            }

            int var34 = var6 >= 30 ? var3.readInt() : 0;
            if (var34 < 0 || var34 * 48L > var4) {
               return null;
            }

            ArrayList var35 = new ArrayList(var34);

            for (int var37 = 0; var37 < var34; var37++) {
               var35.add(new Sink(var3.readDouble(), var3.readDouble(), var3.readDouble(), var3.readDouble(), var3.readDouble(), var3.readDouble()));
            }

            Relief var38 = null;
            if (var6 >= 31) {
               try {
                  var38 = Relief.read(var3, var4);
               } catch (IOException var25) {
                  Constants.LOG.debug("Stored river region {} has an unreadable terrain record ({}); loaded without it", var2, var25.toString());
               }
            }

            if (var6 >= 32) {
               try {
                  int var39 = var3.readInt();
                  if (var39 == var12.size()) {
                     for (int var41 = 0; var41 < var39; var41++) {
                        int var43 = var3.readInt();
                        Lake var44 = (Lake)var12.get(var41);
                        if (var43 > 0) {
                           var12.set(
                              var41,
                              new Lake(
                                 var44.level(),
                                 var44.digDepth(),
                                 var44.spacing(),
                                 var44.cellMinX(),
                                 var44.cellMinZ(),
                                 var44.cellsWide(),
                                 var44.cellsHigh(),
                                 var44.mask(),
                                 var43
                              )
                           );
                        }
                     }
                  }
               } catch (IOException var26) {
                  Constants.LOG
                     .debug("Stored river region {} has an unreadable lake rounding record ({}); its lakes keep the original shoreline", var2, var26.toString());
               }
            }

            RiverNetwork var40 = NetworkTracer.assemble(var2, var10, var12, var35, var38, this.j);
            NetworkTracer.dumpLoadedSegments(var2, var10, var12);
            Constants.LOG.debug("Loaded river region {} from cache ({} segments, {} lakes)", new Object[]{var2, var9, var32});
            return var40;
         }
      } catch (IOException | RuntimeException var28) {
         Constants.LOG.warn("Failed to read cached river region {} ({}); rebuilding", var2, var28.toString());
         return null;
      }
   }

   @Override
   public void save(PlateKey var1, RiverNetwork var2) {
      try {
         this.a();
         Path var3 = this.i.resolve(a(var1));
         Path var4 = this.i.resolve(a(var1) + ".tmp");
          List<RiverSegment> var5 = var2.segments();

         try (DataOutputStream var6 = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(var4)))) {
            var6.writeInt(1397902897);
            var6.writeInt(32);
            var6.writeLong(this.k);
            var6.writeInt(var5.size());

            for (RiverSegment var8 : var5) {
               var6.writeDouble(var8.x1());
               var6.writeDouble(var8.z1());
               var6.writeDouble(var8.y1());
               var6.writeDouble(var8.w1());
               var6.writeDouble(var8.x2());
               var6.writeDouble(var8.z2());
               var6.writeDouble(var8.y2());
               var6.writeDouble(var8.w2());
               var6.writeDouble(var8.sp1());
               var6.writeDouble(var8.sp2());
            }

             List<Lake> var19 = var2.lakes();
            var6.writeInt(var19.size());

            for (Lake var9 : var19) {
               var6.writeInt(var9.level());
               var6.writeInt(var9.digDepth());
               var6.writeInt(var9.spacing());
               var6.writeInt(var9.cellMinX());
               var6.writeInt(var9.cellMinZ());
               var6.writeInt(var9.cellsWide());
               var6.writeInt(var9.cellsHigh());
               long[] var10 = var9.mask();
               var6.writeInt(var10.length);

               for (long var14 : var10) {
                  var6.writeLong(var14);
               }
            }

             List<Sink> var21 = var2.sinks();
            var6.writeInt(var21.size());

            for (Sink var24 : var21) {
               var6.writeDouble(var24.x());
               var6.writeDouble(var24.z());
               var6.writeDouble(var24.y());
               var6.writeDouble(var24.width());
               var6.writeDouble(var24.acc());
               var6.writeDouble(var24.chainLength());
            }

            Relief var23 = var2.relief();
            if (var23 == null) {
               Relief.writeAbsent(var6);
            } else {
               var23.write(var6);
            }

            var6.writeInt(var19.size());

            for (Lake var26 : var19) {
               var6.writeInt(var26.rounding());
            }
         }

         Files.move(var4, var3, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException | RuntimeException var18) {
         Constants.LOG.warn("Failed to persist river region {} ({})", var1, var18.toString());
      }
   }

   private void a() throws IOException {
      if (!this.l) {
         Files.createDirectories(this.i);
         this.l = true;
      }
   }

   private static String a(PlateKey var0) {
      return "r." + var0.cx() + "." + var0.cz() + ".srn";
   }

   private static long a(long var0, StreamsConfig var2) {
      long var3 = 1469598103934665603L;
      var3 = a(var3, 6L);
      var3 = a(var3, var0);
      var3 = a(var3, var2.seaLevel());
      var3 = a(var3, Double.doubleToLongBits(var2.plateScale()));
      var3 = a(var3, Double.doubleToLongBits(var2.plateJitter()));
      var3 = a(var3, var2.gridSpacing());
      var3 = a(var3, var2.terrainSampleSpacing());
      var3 = a(var3, var2.valleySnapRadius());
      var3 = a(var3, var2.terrainFit());
      var3 = a(var3, var2.lakeSampleSpacing());
      var3 = a(var3, var2.legacyShaping() ? 1L : 0L);
      var3 = a(var3, var2.adaptiveGridRead() ? 1L : 0L);
      var3 = a(var3, Double.doubleToLongBits(var2.minRiverAccumulation()));
      var3 = a(var3, Double.doubleToLongBits(var2.wetnessMultiplier()));
      var3 = a(var3, Double.doubleToLongBits(var2.baseWidth()));
      var3 = a(var3, Double.doubleToLongBits(var2.maxWidth()));
      var3 = a(var3, Double.doubleToLongBits(var2.widthScale()));
      var3 = a(var3, Double.doubleToLongBits(var2.borderMargin()));
      var3 = a(var3, var2.smoothingPasses());
      var3 = a(var3, Double.doubleToLongBits(var2.meanderStrength()));
      var3 = a(var3, var2.seaLevelExtension());
      var3 = a(var3, Double.doubleToLongBits(var2.reconnectRadius()));
      var3 = a(var3, Double.doubleToLongBits(var2.wetnessDryCutoff()));
      var3 = a(var3, Double.doubleToLongBits(var2.wetnessWetReference()));
      var3 = a(var3, var2.sinkRooted() ? 1L : 0L);
      var3 = a(var3, var2.growFromWater() ? 1L : 0L);
      var3 = a(var3, Double.doubleToLongBits(var2.growDensity()));
      var3 = a(var3, var2.confineToBanks() ? 1L : 0L);
      var3 = a(var3, var2.outletBiomes() == null ? 0L : var2.outletBiomes().hashCode());
      if (var2.minSeaChainNodes() != 50 / var2.gridSpacing()) {
         var3 = a(var3, var2.minSeaChainNodes());
      }

      if (var2.minLengthScope() != 2) {
         var3 = a(var3, var2.minLengthScope());
      }

      HydraulicGeometry var5 = var2.hydraulics();
      if (var5 != null && var5.shapesNetwork()) {
         var3 = a(var3, Double.doubleToLongBits(var5.widthSlope()));
         var3 = a(var3, Double.doubleToLongBits(var5.widthClimate()));
         var3 = a(var3, Double.doubleToLongBits(var5.widthVariation()));
         var3 = a(var3, Double.doubleToLongBits(var5.deltaMouths()));
      }

      return var3;
   }

   private static long a(long var0, long var2) {
      return (var0 ^ var2) * 1099511628211L;
   }
}
