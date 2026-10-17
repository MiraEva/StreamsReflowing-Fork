package dev.streamsreflowing.flow;

import dev.streamsreflowing.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class StreamIceMelt {
   private StreamIceMelt() {
   }

   public static BlockState meltsInto(Level level, BlockPos pos) {
      if (!(level instanceof ServerLevel)) {
         return null;
      }

      byte angle = StreamFlowGrid.angleForEntityPush(level, pos.getX(), pos.getY(), pos.getZ());
      if (angle == -1) {
         return null;
      }

      BlockState stream = Services.PLATFORM.streamWaterState(flowDir8(angle));
      return stream.is(Blocks.WATER) ? null : stream;
   }

   private static int flowDir8(byte angle) {
      double fx = StreamFlowGrid.vx(angle);
      double fz = StreamFlowGrid.vz(angle);
      int d = (int)Math.round(Math.atan2(fz, fx) / (Math.PI / 4));
      return (d % 8 + 8) % 8;
   }
}
