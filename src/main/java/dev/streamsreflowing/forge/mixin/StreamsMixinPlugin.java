package dev.streamsreflowing.forge.mixin;

import dev.streamsreflowing.compat.DynamicTreesCompat;
import dev.streamsreflowing.compat.MixinTargetProbe;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class StreamsMixinPlugin implements IMixinConfigPlugin {
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      try {
         if (!MixinTargetProbe.isGameOrLoaderClass(targetClassName) && !MixinTargetProbe.classPresent(targetClassName)) {
            return false;
         }
      } catch (Throwable t) {
         return false;
      }

      if (mixinClassName.endsWith(".DynamicTreesGroundMixin")) {
         try {
            boolean here = LoadingModList.get().getModFileById("dynamictrees") != null;
            DynamicTreesCompat.present(here);
            boolean shaped = here && MixinTargetProbe.hasMethod(targetClassName.replace('.', '/'), "findGround", 3, false, "Ljava/util/List;");
            if (shaped) {
               DynamicTreesCompat.armed();
            }

            return shaped;
         } catch (Throwable t) {
            return false;
         }
      } else if (mixinClassName.endsWith(".WorldRenderingSettingsMixin")) {
         try {
            return MixinTargetProbe.hasMethod(targetClassName.replace('.', '/'), "setBlockStateIds", 1, false);
         } catch (Throwable t) {
            return false;
         }
      } else {
         return true;
      }
   }

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
      if (mixinClassName.endsWith(".DynamicTreesGroundMixin")) {
         DynamicTreesCompat.attached();
      }
   }
}
