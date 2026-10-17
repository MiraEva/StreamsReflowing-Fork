package dev.streamsreflowing.flow;

import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class DriftingEntities {
   private DriftingEntities() {
   }

   public static void carry(Entity self) {
      if (self.level() instanceof ServerLevel && self.isInWater() && driftsInCurrent(self)) {
         BlockPos bp = self.blockPosition();
         byte angle = StreamFlowGrid.angleForEntityPush(self.level(), bp.getX(), bp.getY(), bp.getZ());
         if (angle != -1) {
            double v = Services.PLATFORM.vesselDriftSpeed()
               * StreamFlowGrid.speedForPhysics(self.level(), bp.getX(), bp.getY(), bp.getZ())
               / 20.0;
            if (!(v <= 0.0)) {
               self.move(MoverType.SELF, new Vec3(StreamFlowGrid.vx(angle) * v, 0.0, StreamFlowGrid.vz(angle) * v));
            }
         }
      }
   }

   private static boolean driftsInCurrent(Entity self) {
      return self.getType().is(StreamsWorldgen.DRIFTS_IN_CURRENT);
   }
}
