package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.Relief;
import dev.streamsreflowing.core.api.Sink;
import dev.streamsreflowing.core.diag.MemBytes;
import dev.streamsreflowing.core.plate.PlateKey;
import java.util.List;

public final class RiverNetwork {
   public static final int NO_LAKE = Integer.MIN_VALUE;
   private final PlateKey a;
   private final List<RiverSegment> b;
   private final NearestRiverIndex c;
   private final List<Lake> d;
   private final List<Sink> e;
   private final Relief f;

   public RiverNetwork(PlateKey var1, List<RiverSegment> var2, NearestRiverIndex var3, List<Lake> var4) {
      this(var1, var2, var3, var4, List.of(), null);
   }

   public RiverNetwork(PlateKey var1, List<RiverSegment> var2, NearestRiverIndex var3, List<Lake> var4, List<Sink> var5) {
      this(var1, var2, var3, var4, var5, null);
   }

   public RiverNetwork(PlateKey var1, List<RiverSegment> var2, NearestRiverIndex var3, List<Lake> var4, List<Sink> var5, Relief var6) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      this.e = var5;
      this.f = var6;
   }

   public NearestRiverIndex index() {
      return this.c;
   }

   public long approxBytes() {
      int var1 = this.b.size();
      long var2 = MemBytes.object(24) + var1 * MemBytes.object(80) + MemBytes.array(var1, 4);
      if (this.c != null) {
         var2 += this.c.approxBytes();
      }

      if (this.f != null) {
         var2 += this.f.approxBytes();
      }

      for (Lake var5 : this.d) {
         var2 += MemBytes.object(28) + MemBytes.array(var5.mask().length, 8);
      }

      return var2 + this.e.size() * MemBytes.object(48);
   }

   public Relief relief() {
      return this.f;
   }

   public List<Sink> sinks() {
      return this.e;
   }

   public PlateKey key() {
      return this.a;
   }

   public List<RiverSegment> segments() {
      return this.b;
   }

   public int segmentCount() {
      return this.b.size();
   }

   public List<Lake> lakes() {
      return this.d;
   }

   public RiverSample sample(double var1, double var3, double var5) {
      return this.c.nearest(var1, var3, var5);
   }

   public RiverSample sampleTiered(double var1, double var3, double var5) {
      return this.c.nearestTiered(var1, var3, var5);
   }

   public RiverSample sampleCoveringForeign(double var1, double var3, double var5, int var7) {
      return this.c.nearestCoveringForeign(var1, var3, var5, var7);
   }

   public boolean corridorTouchesBox(double var1, double var3, double var5, double var7) {
      return this.c.anyCorridorTouchesBox(var1, var3, var5, var7);
   }

   public boolean inAnyChannel(double var1, double var3, double var5) {
      return this.c.inAnyChannel(var1, var3, var5);
   }

   public double lowerNeighborGrade(double var1, double var3, double var5, int var7, double var8, double var10) {
      return this.c.lowerNeighborGrade(var1, var3, var5, var7, var8, var10);
   }

   public Lake lakeAt(int var1, int var2) {
      for (Lake var4 : this.d) {
         if (var4.covers(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   public int lakeLevelAt(int var1, int var2) {
      Lake var3 = this.lakeAt(var1, var2);
      return var3 == null ? Integer.MIN_VALUE : var3.level();
   }

   public Lake lakeNear(int var1, int var2, int var3) {
      Lake var4 = null;

      for (Lake var6 : this.d) {
         if (var6.coversWithin(var1, var2, var3) && (var4 == null || var6.level() < var4.level())) {
            var4 = var6;
         }
      }

      return var4;
   }

   public Lake lakeWettingAt(int var1, int var2, int var3) {
      Lake var4 = null;

      for (Lake var6 : this.d) {
         if (var6.digDepth() == 0
            && (var4 == null || var6.level() < var4.level())
            && var6.coversWithin(var1, var2, Math.max(var3, var6.reachCells()))
            && var6.wetAt(var1, var2)) {
            var4 = var6;
         }
      }

      return var4;
   }
}
