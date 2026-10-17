package dev.streamsreflowing.core.compose;

public record ColumnPlan(boolean inChannel, boolean inCorridor, int waterSurfaceY, int bedY, double bankBlend, double distance, double width, double gradeY, double slope, double flowDirX, double flowDirZ, boolean terminal, boolean source, double upstreamSlope, double fluidFlowDirX, double fluidFlowDirZ, double distToOutlet, double curvature, double lateral, double distToJunction, double flatRun, double speed) {
   public static final ColumnPlan NONE = new ColumnPlan(
      false, false, 0, 0, 1.0, Double.MAX_VALUE, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 0.0, 0.0, 0.0, Double.MAX_VALUE, 0.0, 0.0, Double.MAX_VALUE, 0.0, 1.0
   );

   public ColumnPlan {
inCorridor = inCorridor;
bedY = bedY;
distance = distance;
gradeY = gradeY;
flowDirX = flowDirX;
terminal = terminal;
upstreamSlope = upstreamSlope;
fluidFlowDirZ = fluidFlowDirZ;
curvature = curvature;
distToJunction = distToJunction;
speed = speed;
}

   public boolean hasLateral() {
      return this.lateral != 0.0 || this.curvature != 0.0;
   }
}
