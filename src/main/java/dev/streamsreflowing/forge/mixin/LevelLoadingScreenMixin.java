package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.config.StreamsSettings;
import dev.streamsreflowing.worldgen.LoadingNotice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {
   @Inject(method = "render", at = @At("TAIL"), require = 0)
   private void streamsreflowing$buildNotice(GuiGraphics gfx, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      try {
         if (!StreamsSettings.loadingNotice()) {
            return;
         }

         String notice = LoadingNotice.currentText();
         if (notice == null) {
            return;
         }

         gfx.drawCenteredString(Minecraft.getInstance().font, Component.literal(notice), gfx.guiWidth() / 2, gfx.guiHeight() - 30, -1);
      } catch (Throwable var7) {
      }
   }
}
