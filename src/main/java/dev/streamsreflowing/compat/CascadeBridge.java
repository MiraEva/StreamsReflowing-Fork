package dev.streamsreflowing.compat;

import dev.streamsreflowing.RainCount;
import dev.streamsreflowing.client.CascadeBudget;
import dev.streamsreflowing.client.FoamSize;
import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

public final class CascadeBridge {
   private static final int FOAM_NONE = 0;
   private static final int FOAM_OURS = 2;
   private static final int FOAM_BOTH = 3;
   private static final int[] EDGE_DX = new int[]{1, -1, 0, 0};
   private static final int[] EDGE_DZ = new int[]{0, 0, 1, -1};
   private static final ResourceLocation EFFECTIVE_CASCADE_ID = new ResourceLocation("effective", "cascade");
   private static boolean effectiveParticleResolved;
   private static ParticleOptions effectiveCascadeParticle;

   private CascadeBridge() {
   }

   public static boolean holdsTheirsOff(BlockGetter level, BlockPos pos) {
      return StreamsSettings.partnerParticles() && ours(level, pos);
   }

   public static boolean ours(BlockGetter level, BlockPos pos) {
      if (StreamFlowGrid.angleForPhysics(level, pos.getX(), pos.getZ()) != -1) {
         return true;
      }

      FluidState fs = level.getFluidState(pos);
      return !fs.isEmpty() && "streamsreflowing".equals(BuiltInRegistries.FLUID.getKey(fs.getType()).getNamespace());
   }

   public static int fireAt(Level level, int x, int y, int z, int fallHeight) {
      return fireAt(level, x, y, z, fallHeight, false);
   }

   public static int fireAt(Level level, int x, int y, int z, int fallHeight, boolean fromScan) {
      return fireAt(level, x, y, z, fallHeight, fromScan, 1.0F);
   }

   public static int fireAt(Level level, int x, int y, int z, int fallHeight, boolean fromScan, float lodScale) {
      if (RainCount.bridgeOff) {
         return 0;
      }

      if (!StreamsSettings.partnerParticles()) {
         return 0;
      }

      ParticleOptions effective = effectiveCascade();
      ParticleOptions particular = ParticularCompat.cascade();
      if (effective == null && particular == null) {
         return 0;
      }

      int whose = StreamsSettings.waterfallFoam();
      if (whose == 0) {
         return -1;
      }

      if (whose == 2) {
         return 0;
      }

      if (!CascadeBudget.claim(level, x, y, z)) {
         return -1;
      }

      int made = 0;
      int sides = openSides(level, x, y, z);
      if (sides == 0) {
         sides = 15;
      }

      RandomSource random = level.getRandom();
      double strength = 0.8 + 0.3 * Math.min(fallHeight, 4);

      for (int k = 0; k < 4; k++) {
         if ((sides & 1 << k) != 0) {
            int dx = EDGE_DX[k];
            int dz = EDGE_DZ[k];
            double along = (random.nextDouble() - 0.5) * 0.7;
            double px = x + 0.5 + dx * 0.55 + (dx == 0 ? along : 0.0);
            double pz = z + 0.5 + dz * 0.55 + (dz == 0 ? along : 0.0);
            float size = 0.67F * lodScale * FoamSize.of(random.nextFloat(), fallHeight);
            double py = y + 1.1 + 0.3 * size + random.nextDouble() * 0.15;
            double out = (0.4 + random.nextDouble() * 0.6) * strength / 10.0;
            double vy = random.nextDouble() * strength / 10.0;
            if (effective != null) {
               FoamSize.place(level, effective, px, py, pz, dx * out, vy, dz * out, size, fromScan);
               made++;
            }

            if (particular != null) {
               FoamSize.place(level, particular, px, py, pz, dx * out * 0.5, vy * 0.5, dz * out * 0.5, size, fromScan);
               made++;
            }
         }
      }

      CascadeBudget.spend(made);
      RainCount.bridge(fromScan, made);
      return made;
   }

   public static boolean oursAfter(int fired) {
      return fired == 0 ? true : StreamsSettings.partnerParticles() && StreamsSettings.waterfallFoam() == 3;
   }

   private static int openSides(Level level, int x, int y, int z) {
      int mask = 0;
      MutableBlockPos p = new MutableBlockPos();

      for (int k = 0; k < 4; k++) {
         int nx = x + EDGE_DX[k];
         int nz = z + EDGE_DZ[k];
         FluidState side = level.getFluidState(p.set(nx, y, nz));
         if (!side.isEmpty()) {
            FluidState above = level.getFluidState(p.set(nx, y + 1, nz));
            boolean falling = above.hasProperty(FlowingFluid.FALLING) && (Boolean)above.getValue(FlowingFluid.FALLING);
            if (!falling) {
               mask |= 1 << k;
            }
         }
      }

      return mask;
   }

   private static ParticleOptions effectiveCascade() {
      if (!effectiveParticleResolved) {
         ParticleType<?> type = (ParticleType<?>)BuiltInRegistries.PARTICLE_TYPE.get(EFFECTIVE_CASCADE_ID);
         effectiveCascadeParticle = type instanceof ParticleOptions particle ? particle : null;
         effectiveParticleResolved = true;
      }

      return effectiveCascadeParticle;
   }
}
