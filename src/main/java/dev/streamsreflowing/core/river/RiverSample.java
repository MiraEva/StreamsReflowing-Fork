package dev.streamsreflowing.core.river;

public record RiverSample(double distance, double gradeY, double width, double slope, double dirX, double dirZ, boolean terminal, boolean source, double upstreamSlope, double distToOutlet, int segId, double distToJunction, double flatRun, double speed) {

   public RiverSample {
gradeY = gradeY;
slope = slope;
dirZ = dirZ;
source = source;
distToOutlet = distToOutlet;
distToJunction = distToJunction;
speed = speed;
}
}
