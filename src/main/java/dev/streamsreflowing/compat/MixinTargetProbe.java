package dev.streamsreflowing.compat;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class MixinTargetProbe {
   private MixinTargetProbe() {
   }

   public static boolean isGameOrLoaderClass(String dottedName) {
      return dottedName.startsWith("net.minecraft.")
         || dottedName.startsWith("com.mojang.")
         || dottedName.startsWith("net.neoforged.")
         || dottedName.startsWith("net.minecraftforge.")
         || dottedName.startsWith("net.fabricmc.");
   }

   public static boolean classPresent(String dottedName) {
      String res = dottedName.replace('.', '/') + ".class";

      try (InputStream in = MixinTargetProbe.class.getClassLoader().getResourceAsStream(res)) {
         return in != null;
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean hasMethod(String internalName, String method, int argCount, boolean isStatic) {
      return hasMethod(internalName, method, argCount, isStatic, "V");
   }

   public static boolean hasMethod(String internalName, String method, int argCount, boolean isStatic, String returnDesc) {
      try (InputStream in = MixinTargetProbe.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
         if (in == null) {
            return false;
         }

         ClassNode node = new ClassNode();
         new ClassReader(in).accept(node, 7);

         for (MethodNode m : node.methods) {
            if (m.name.equals(method)) {
               boolean statik = (m.access & 8) != 0;
               if (statik == isStatic && Type.getArgumentTypes(m.desc).length == argCount && Type.getReturnType(m.desc).getDescriptor().equals(returnDesc)) {
                  return true;
               }
            }
         }

         return false;
      } catch (Throwable t) {
         return false;
      }
   }
}
