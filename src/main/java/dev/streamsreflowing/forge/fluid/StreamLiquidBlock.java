package dev.streamsreflowing.forge.fluid;

import dev.streamsreflowing.compat.CascadeBridge;
import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.forge.particle.StreamParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class StreamLiquidBlock extends LiquidBlock {
   public static final IntegerProperty FLOW = IntegerProperty.create("flow", 0, 8);
   public static final int FLOW_INHERIT = 8;
   private static final int MAX_FALL_SCAN = 16;
   private static long lastParticleTick = Long.MIN_VALUE;
   private static int particlesThisTick = 0;
   private static int particleBudget = 0;
   private static boolean capExempt = false;
   public static final Vec3[] DIRS = new Vec3[8];

   public StreamLiquidBlock(FlowingFluid fluid, Properties properties) {
      super(fluid, properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(LEVEL, 0)).setValue(FLOW, 0));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FLOW});
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (StreamsSettings.streamBubbles() != 0 && !(StreamsSettings.particleRate() <= 0.0)) {
         long gameTime = level.getGameTime();
         if (gameTime != lastParticleTick) {
            lastParticleTick = gameTime;
            particlesThisTick = 0;
         }

         particleBudget = StreamsSettings.maxStreamParticles();
         Vec3 flow = state.getFluidState().getFlow(level, pos);
         double fx = flow.x;
         double fz = flow.z;
         double mag = Math.sqrt(fx * fx + fz * fz);
         if (mag > 1.0E-4) {
            fx /= mag;
            fz /= mag;
         }

         boolean spilling = (Integer)state.getValue(LEVEL) != 0;
         double px = pos.getX() + random.nextDouble();
         double pz = pos.getZ() + random.nextDouble();
         capExempt = particleBudget > 0;
         FluidState here = state.getFluidState();
         FluidState above = level.getFluidState(pos.above());
         FluidState below = level.getFluidState(pos.below());
         boolean thisFalling = here.hasProperty(FlowingFluid.FALLING) && (Boolean)here.getValue(FlowingFluid.FALLING);
         boolean aboveFalling = above.hasProperty(FlowingFluid.FALLING) && (Boolean)above.getValue(FlowingFluid.FALLING);
         boolean belowFalling = below.hasProperty(FlowingFluid.FALLING) && (Boolean)below.getValue(FlowingFluid.FALLING);
         boolean cascadeBase = aboveFalling && !belowFalling;
         int fallHeight = 0;
         if (cascadeBase) {
            for (int dy = thisFalling ? 0 : 1; dy <= 16; dy++) {
               FluidState col = level.getFluidState(pos.above(dy));
               if (!col.hasProperty(FlowingFluid.FALLING) || !(Boolean)col.getValue(FlowingFluid.FALLING)) {
                  break;
               }

               fallHeight++;
            }
         }

         boolean bigDropFoot = cascadeBase && fallHeight >= 2;
         boolean columnFace = thisFalling && (aboveFalling || belowFalling);
         if (bigDropFoot) {
            double surfaceY = pos.getY() + 0.9;
            if (random.nextInt(9) < fallHeight
               && CascadeBridge.oursAfter(CascadeBridge.fireAt(level, pos.getX(), pos.getY(), pos.getZ(), fallHeight))) {
               double ox = (random.nextDouble() - 0.5) * 0.6;
               double oz = (random.nextDouble() - 0.5) * 0.6;
               spawn(
                  level,
                  ParticleTypes.CLOUD,
                  px + ox,
                  surfaceY + random.nextDouble() * 0.2,
                  pz + oz,
                  fx * 0.04 + ox * 0.15,
                  0.04 + random.nextDouble() * 0.06,
                  fz * 0.04 + oz * 0.15
               );
            }

            if (random.nextInt(2) == 0) {
               double ox = (random.nextDouble() - 0.5) * 0.6;
               double oz = (random.nextDouble() - 0.5) * 0.6;
               spawn(
                  level,
                  ParticleTypes.SPLASH,
                  px + ox,
                  surfaceY + random.nextDouble() * 0.2,
                  pz + oz,
                  fx * 0.08 + ox * 0.5,
                  0.16 + random.nextDouble() * 0.24,
                  fz * 0.08 + oz * 0.5
               );
            }
         } else if (columnFace) {
            if (random.nextInt(6) == 0) {
               spawn(level, ParticleTypes.SPLASH, px, pos.getY() + 0.6, pz, fx * 0.06, 0.06 + random.nextDouble() * 0.1, fz * 0.06);
            }
         } else if (cascadeBase) {
            if (random.nextInt(3) == 0) {
               spawn(level, ParticleTypes.SPLASH, px, pos.getY() + 0.5, pz, fx * 0.06, 0.08 + random.nextDouble() * 0.08, fz * 0.06);
            }

            if (random.nextInt(10) == 0) {
               double ang = random.nextDouble() * (Math.PI * 2);
               double spd = 0.08 * (0.4 + random.nextDouble() * 0.8);
               spawn(
                  level,
                  (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
                  px,
                  pos.getY() + 0.5,
                  pz,
                  Math.cos(ang) * spd,
                  0.05 + random.nextDouble() * 0.06,
                  Math.sin(ang) * spd
               );
            }
         }

         if (bigDropFoot) {
            double surfaceY = pos.getY() + 1.0;
            int spray = Math.min((int)(fallHeight * 1.75), 28);
            double power = 0.22 + fallHeight * 0.03;

            for (int i = 0; i < spray; i++) {
               double ang = random.nextDouble() * (Math.PI * 2);
               double spd = power * (0.5 + random.nextDouble() * 0.7);
               double ox = Math.cos(ang);
               double oz = Math.sin(ang);
               spawn(
                  level,
                  ParticleTypes.SPLASH,
                  px + ox * 0.2,
                  surfaceY + random.nextDouble() * 0.2,
                  pz + oz * 0.2,
                  ox * spd,
                  0.3 + random.nextDouble() * 0.3,
                  oz * spd
               );
            }
         }

         if (bigDropFoot) {
            int burst = Math.min((int)((8 + fallHeight * 2) * 1.75), 70);
            double power = 0.18 + fallHeight * 0.04;

            for (int i = 0; i < burst; i++) {
               double ang = random.nextDouble() * (Math.PI * 2);
               double spd = power * (0.4 + random.nextDouble() * 0.8);
               double ox = Math.cos(ang);
               double oz = Math.sin(ang);
               spawn(
                  level,
                  (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
                  px + ox * 0.25,
                  pos.getY() + 0.6 + random.nextDouble() * 0.3,
                  pz + oz * 0.25,
                  ox * spd,
                  0.06 + random.nextDouble() * 0.08,
                  oz * spd
               );
            }
         }

         capExempt = false;
         if (above.isEmpty()) {
            double drift = spilling ? 0.16 : 0.1;
            if (random.nextInt(spilling ? 14 : 42) == 0) {
               int clump = 2 + random.nextInt(3);

               for (int i = 0; i < clump; i++) {
                  spawn(
                     level,
                     (ParticleOptions)StreamParticles.STREAM_BUBBLE.get(),
                     px + (random.nextDouble() - 0.5) * 0.4,
                     pos.getY() + 0.8,
                     pz + (random.nextDouble() - 0.5) * 0.4,
                     fx * drift,
                     0.0,
                     fz * drift
                  );
               }
            }

            if (StreamsSettings.streamDebris() && random.nextInt(357) == 0) {
               spawn(level, (ParticleOptions)StreamParticles.STREAM_LEAF.get(), px, pos.getY() + 0.9, pz, fx * drift, 0.0, fz * drift);
            }

            if (StreamsSettings.streamDebris() && random.nextInt(9600) == 0) {
               spawn(level, (ParticleOptions)StreamParticles.STREAM_STICK.get(), px, pos.getY() + 0.9, pz, fx * drift, 0.0, fz * drift);
            }

            if (spilling && mag > 1.0E-4) {
               int aox = (int)Math.round(fx);
               int aoz = (int)Math.round(fz);
               if (aox != 0 || aoz != 0) {
                  FluidState ahead = level.getFluidState(pos.offset(aox, 0, aoz));
                  boolean streamAhead = ahead.getType() == StreamFluids.FLOWING.get() || ahead.getType() == StreamFluids.SOURCE.get();
                  if (!streamAhead && random.nextInt(7) == 0) {
                     spawn(
                        level,
                        (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
                        px + aox * 0.4,
                        pos.getY() + 0.85,
                        pz + aoz * 0.4,
                        fx * 0.12,
                        0.05 + random.nextDouble() * 0.07,
                        fz * 0.12
                     );
                  }
               }
            }

            int fi = (Integer)state.getValue(FLOW);

            for (Direction d : Plane.HORIZONTAL) {
               BlockPos np = pos.relative(d);
               BlockState ns = level.getBlockState(np);
               boolean matches = ns.getBlock() instanceof StreamLiquidBlock && ((Integer)ns.getValue(FLOW) == fi || (Integer)ns.getValue(FLOW) == 8 || fi == 8);
               if (!matches && random.nextInt(8) == 0) {
                  double bx = px + d.getStepX() * 0.5;
                  double bz = pz + d.getStepZ() * 0.5;
                  int flecks = 1 + random.nextInt(2);

                  for (int i = 0; i < flecks; i++) {
                     double ang = random.nextDouble() * (Math.PI * 2);
                     double spd = 0.05 * (0.5 + random.nextDouble());
                     spawn(
                        level,
                        (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
                        bx + (random.nextDouble() - 0.5) * 0.25,
                        pos.getY() + 0.82,
                        bz + (random.nextDouble() - 0.5) * 0.25,
                        Math.cos(ang) * spd,
                        0.06 + random.nextDouble() * 0.08,
                        Math.sin(ang) * spd
                     );
                  }
               }
            }

            if (spilling && random.nextInt(5) == 0) {
               spawn(level, ParticleTypes.SPLASH, px, pos.getY() + 0.9, pz, fx * 0.14, 0.1 + random.nextDouble() * 0.12, fz * 0.14);
            }

            if (spilling && random.nextInt(8) == 0) {
               spawn(
                  level,
                  (ParticleOptions)StreamParticles.STREAM_SPLASH.get(),
                  px,
                  pos.getY() + 0.7,
                  pz,
                  fx * 0.06,
                  0.03 + random.nextDouble() * 0.04,
                  fz * 0.06
               );
            }
         }
      }
   }

   private static void spawn(Level level, ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
      if (capExempt || particlesThisTick < particleBudget) {
         particlesThisTick++;
         level.addParticle(type, x, y, z, vx, vy, vz);
      }
   }

   static {
      for (int i = 0; i < 8; i++) {
         double a = i * (Math.PI / 4);
         DIRS[i] = new Vec3(Math.cos(a), 0.0, Math.sin(a));
      }
   }
}
