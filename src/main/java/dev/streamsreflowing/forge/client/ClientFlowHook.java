package dev.streamsreflowing.forge.client;

import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.FlowRemeshQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class ClientFlowHook {
   private ClientFlowHook() {
   }

   public static void store(int cx, int cz, byte[] grid) {
      if (ClientFlowGrids.put(cx, cz, grid)) {
         FlowRemeshQueue.enqueue(cx, cz);
      }
   }

   public static void drainTick() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel lvl = mc.level;
      if (lvl == null) {
         FlowRemeshQueue.clear();
      } else {
         FlowRemeshQueue.drain((cx, cz) -> {
            LevelChunk chunk = lvl.getChunkSource().getChunk(cx, cz, false);
            if (chunk != null) {
               LevelChunkSection[] secs = chunk.getSections();

               for (int i = 0; i < secs.length; i++) {
                  if (!secs[i].hasOnlyAir() && secs[i].maybeHas(st -> !st.getFluidState().isEmpty())) {
                     mc.levelRenderer.setSectionDirty(cx, (lvl.getMinBuildHeight() >> 4) + i, cz);
                  }
               }
            }
         });
      }
   }
}
