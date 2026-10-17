package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Structure.class)
public abstract class StructureMixin {
   @Unique
   private static final ThreadLocal<Boolean> streamsreflowing$generating = ThreadLocal.withInitial(() -> Boolean.FALSE);

   @Inject(method = "generate", at = @At("HEAD"))
   private void streamsreflowing$markGenerating(CallbackInfoReturnable<StructureStart> cir) {
      streamsreflowing$generating.set(Boolean.TRUE);
   }

   @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
   private void streamsreflowing$keepFootprintOffRivers(CallbackInfoReturnable<StructureStart> cir) {
      if (StreamsSettings.generateRivers()) {
         streamsreflowing$generating.set(Boolean.FALSE);
         StructureStart start = (StructureStart)cir.getReturnValue();
         if (start != null && start.isValid() && StreamsWorldgen.structureFootprintOnStream(start)) {
            if (!StreamsWorldgen.structureAlwaysGenerates((Structure)(Object)this)) {
               String id = StreamsWorldgen.structureId((Structure)(Object)this);
               if (id == null || !StreamsWorldgen.structurePromisedAt(id, start.getChunkPos().getMinBlockX() >> 4, start.getChunkPos().getMinBlockZ() >> 4)) {
                  cir.setReturnValue(StructureStart.INVALID_START);
               }
            }
         }
      }
   }

   @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
   private void streamsreflowing$keepGenerationPointOffRivers(GenerationContext context, CallbackInfoReturnable<Optional<GenerationStub>> cir) {
      if (StreamsSettings.generateRivers()) {
         Optional<GenerationStub> stub = (Optional<GenerationStub>)cir.getReturnValue();
         if (stub != null && !stub.isEmpty()) {
            BlockPos p = stub.get().position();
            String promiseId = streamsreflowing$generating.get() ? null : StreamsWorldgen.structureId((Structure)(Object)this);
            if (StreamsWorldgen.generationPointOnWater(p.getX(), p.getY(), p.getZ(), promiseId)
               && !StreamsWorldgen.structureAlwaysGenerates((Structure)(Object)this)) {
               cir.setReturnValue(Optional.empty());
            }
         }
      }
   }
}
