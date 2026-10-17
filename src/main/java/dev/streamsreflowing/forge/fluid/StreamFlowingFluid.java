package dev.streamsreflowing.forge.fluid;

import dev.streamsreflowing.compat.PhysicsModCompat;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;

public final class StreamFlowingFluid {
   private static final int CALM_MAX = 3;

   private StreamFlowingFluid() {
   }

   private static Vec3 bakedFlow(BlockGetter level, BlockPos pos) {
      BlockState bs = level.getBlockState(pos);
      if (!(bs.getBlock() instanceof StreamLiquidBlock)) {
         return null;
      }

      int f = (Integer)bs.getValue(StreamLiquidBlock.FLOW);
      if (f != 8) {
         return StreamLiquidBlock.DIRS[f];
      }

      double x = 0.0;
      double z = 0.0;
      int n = 0;

      for (Direction d : Plane.HORIZONTAL) {
         BlockState ns = level.getBlockState(pos.relative(d));
         if (ns.getBlock() instanceof StreamLiquidBlock) {
            int nf = (Integer)ns.getValue(StreamLiquidBlock.FLOW);
            if (nf != 8) {
               Vec3 v = StreamLiquidBlock.DIRS[nf];
               x += v.x;
               z += v.z;
               n++;
            }
         }
      }

      return n > 0 ? new Vec3(x, 0.0, z).normalize() : null;
   }

   private static Vec3 resolve(Vec3 natural, BlockGetter level, BlockPos pos) {
      Vec3 flow = resolveFlow(natural, level, pos);
      return flow.x == 0.0 && flow.z == 0.0 && !(level instanceof Level) && PhysicsModCompat.keepOceanOffStreams()
         ? StreamFlowGrid.renderFallbackFlow(level, pos.getX(), pos.getY(), pos.getZ())
         : flow;
   }

   private static Vec3 resolveFlow(Vec3 natural, BlockGetter level, BlockPos pos) {
      if (!(level instanceof Level) && PhysicsModCompat.noImposedRenderFlow()) {
         return natural;
      }

      int d = axisDistToVanillaWater(level, pos, 3);
      if (d > 0 && d <= calmReach(pos.getX(), pos.getZ())) {
         return Vec3.ZERO;
      }

      if (natural.lengthSqr() > 1.0E-6) {
         return natural;
      }

      Vec3 baked = bakedFlow(level, pos);
      if (baked == null) {
         return natural;
      }

      double sp = StreamFlowGrid.speedForPhysics(level, pos.getX(), pos.getY(), pos.getZ());
      return sp == 1.0 ? baked : baked.scale(sp);
   }

   private static int calmReach(int x, int z) {
      int h = Math.floorDiv(x, 3) * 374761393 + Math.floorDiv(z, 3) * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return 1 + Math.floorMod(h >>> 8, 3);
   }

   private static int axisDistToVanillaWater(BlockGetter level, BlockPos pos, int max) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      MutableBlockPos m = new MutableBlockPos();

      for (int r = 1; r <= max; r++) {
         if (isVanillaWater(level, m.set(x + r, y, z))
            || isVanillaWater(level, m.set(x - r, y, z))
            || isVanillaWater(level, m.set(x, y, z + r))
            || isVanillaWater(level, m.set(x, y, z - r))) {
            return r;
         }
      }

      return -1;
   }

   private static boolean isVanillaWater(BlockGetter level, BlockPos pos) {
      BlockState ns = level.getBlockState(pos);
      return !(ns.getBlock() instanceof StreamLiquidBlock) && ns.getFluidState().is(FluidTags.WATER);
   }

   private static BlockState legacyBlock(int level) {
      return (BlockState)((BlockState)((StreamLiquidBlock)StreamFluids.STREAM_BLOCK.get()).defaultBlockState().setValue(LiquidBlock.LEVEL, level))
         .setValue(StreamLiquidBlock.FLOW, 8);
   }

   private static Fluid waterEquivalent(Fluid fluid) {
      if (fluid == StreamFluids.SOURCE.get()) {
         return Fluids.WATER;
      } else {
         return (Fluid)(fluid == StreamFluids.FLOWING.get() ? Fluids.FLOWING_WATER : fluid);
      }
   }

   private static boolean spreadIntoContainer(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
      if (state.getBlock() instanceof LiquidBlockContainer container) {
         if (!container.placeLiquid(level, pos, state, fluidState) && fluidState.isSource()) {
            container.placeLiquid(level, pos, state, Fluids.WATER.defaultFluidState());
         }

         return true;
      } else {
         return false;
      }
   }

   public static final class Flowing extends net.minecraft.world.level.material.WaterFluid.Flowing {
      public FluidType getFluidType() {
         return (FluidType)ForgeMod.WATER_TYPE.get();
      }

      public Fluid getFlowing() {
         return (Fluid)StreamFluids.FLOWING.get();
      }

      public Fluid getSource() {
         return (Fluid)StreamFluids.SOURCE.get();
      }

      public boolean isSame(Fluid fluid) {
         return fluid == StreamFluids.SOURCE.get() || fluid == StreamFluids.FLOWING.get() || fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
      }

      public BlockState createLegacyBlock(FluidState state) {
         return StreamFlowingFluid.legacyBlock(getLegacyLevel(state));
      }

      public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
         return StreamFlowingFluid.resolve(super.getFlow(level, pos, state), level, pos);
      }

      protected boolean canSpreadTo(
         BlockGetter level, BlockPos fromPos, BlockState fromState, Direction direction, BlockPos toPos, BlockState toState, FluidState toFluid, Fluid fluid
      ) {
         return super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, fluid)
            || toState.getBlock() instanceof LiquidBlockContainer
               && super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, StreamFlowingFluid.waterEquivalent(fluid));
      }

      protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
         if (!StreamFlowingFluid.spreadIntoContainer(level, pos, state, fluidState)) {
            super.spreadTo(level, pos, state, direction, fluidState);
         }
      }
   }

   public static final class Source extends net.minecraft.world.level.material.WaterFluid.Source {
      public FluidType getFluidType() {
         return (FluidType)ForgeMod.WATER_TYPE.get();
      }

      public Fluid getFlowing() {
         return (Fluid)StreamFluids.FLOWING.get();
      }

      public Fluid getSource() {
         return (Fluid)StreamFluids.SOURCE.get();
      }

      public boolean isSame(Fluid fluid) {
         return fluid == StreamFluids.SOURCE.get() || fluid == StreamFluids.FLOWING.get() || fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
      }

      public BlockState createLegacyBlock(FluidState state) {
         return StreamFlowingFluid.legacyBlock(getLegacyLevel(state));
      }

      public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
         return StreamFlowingFluid.resolve(super.getFlow(level, pos, state), level, pos);
      }

      protected boolean canSpreadTo(
         BlockGetter level, BlockPos fromPos, BlockState fromState, Direction direction, BlockPos toPos, BlockState toState, FluidState toFluid, Fluid fluid
      ) {
         return super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, fluid)
            || toState.getBlock() instanceof LiquidBlockContainer
               && super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, StreamFlowingFluid.waterEquivalent(fluid));
      }

      protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
         if (!StreamFlowingFluid.spreadIntoContainer(level, pos, state, fluidState)) {
            super.spreadTo(level, pos, state, direction, fluidState);
         }
      }
   }
}
