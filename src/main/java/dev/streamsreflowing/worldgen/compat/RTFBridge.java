package dev.streamsreflowing.worldgen.compat;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.ColumnKey;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import dev.streamsreflowing.platform.Services;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RTFBridge {
   private static final String[][] a = new String[][]{
      {"raccoonman.reterraforged", "RTFRandomState", "reterraforged$getFlowField"},
      {"etcodehome.freeterraforged", "FTFRandomState", "freeterraforged$getFlowField"}
   };
   private static volatile boolean b;
   private static volatile RTFBridge.Surface c;
   private static final String[] d = new String[]{"reterraforged", "freeterraforged"};
   private static final Map<Object, Object> e = new WeakHashMap<>();
   private static final Object f = new Object();
   private static final AtomicBoolean g = new AtomicBoolean();
   private static final AtomicBoolean h = new AtomicBoolean();

   private RTFBridge() {
   }

   private static RTFBridge.Surface a() {
      if (!b) {
         synchronized (RTFBridge.class) {
            if (!b) {
               if (!Boolean.getBoolean("streamsreflowing.nortf")) {
                  for (String[] var4 : a) {
                     try {
                        c = new RTFBridge.Surface(var4);
                        Constants.LOG.debug("Streams Reflowing: ReTerraForged bridge available ({}{}).", var4[0], c.l == null ? ", no flow field" : "");
                        break;
                     } catch (ReflectiveOperationException | LinkageError var7) {
                        c = null;
                        Constants.LOG.debug("Streams Reflowing: no ReTerraForged bridge for {} ({}).", var4[0], var7.toString());
                     }
                  }

                  a(c);
               }

               b = true;
            }
         }
      }

      return c;
   }

   private static void a(RTFBridge.Surface var0) {
      if (var0 == null) {
         boolean var1 = false;

         try {
            for (String var5 : d) {
               var1 |= Services.PLATFORM.isModLoaded(var5);
            }
         } catch (RuntimeException | LinkageError var6) {
            return;
         }

         if (var1) {
            Constants.LOG
               .warn(
                  "Streams Reflowing: ReTerraForged is installed but this build of it could not be read, so its terrain, its rivers and their current are all being left alone. This usually means the two mods need a compatibility update -- please report it."
               );
         }
      }
   }

   public static boolean available() {
      RTFBridge.Surface var0 = a();
      return Services.PLATFORM.reterraforgedCompat() && var0 != null && var0.l != null;
   }

   public static byte[] flowUnderlay(Object var0) {
      if (!Services.PLATFORM.reterraforgedCompat()) {
         return null;
      }

      RTFBridge.Surface var1 = a();

      try {
         if (var1 != null && var1.l != null && var0 != null && var1.l.isInstance(var0)) {
            Object var2 = var1.m.invoke(var0);
            return var2 != null && Boolean.TRUE.equals(var1.n.invoke(var2)) ? RTFFlowUnderlay.convert((byte[])var1.o.invoke(var2)) : null;
         } else {
            return null;
         }
      } catch (ReflectiveOperationException | LinkageError var3) {
         return null;
      }
   }

   public static RTFBridge.Handle handleFor(Object var0) {
      if (!Services.PLATFORM.reterraforgedCompat()) {
         return null;
      }

      RTFBridge.Surface var1 = a();
      if (var1 != null && var0 != null) {
         synchronized (e) {
            Object var3 = e.get(var0);
            if (var3 != null) {
               return var3 == f ? null : (RTFBridge.Handle)var3;
            }
         }

         RTFBridge.Handle var11 = null;
         boolean var12 = false;

         try {
            if (var1.a.isInstance(var0)) {
               Object var4 = var1.b.invoke(var0);
               if (var4 == null) {
                  var12 = true;
               } else {
                  Object var5 = var1.c.get(var4);
                  Object var6 = var1.d.get(var4);
                  if (var5 != null && var6 != null) {
                     var11 = new RTFBridge.Handle(var1, var5, var1.e.getInt(var6));
                     Constants.LOG.info("Streams Reflowing: reading terrain and rivers from ReTerraForged for this world.");
                     if (var1.l == null && h.compareAndSet(false, true)) {
                        Constants.LOG
                           .warn(
                              "Streams Reflowing: this build of ReTerraForged does not publish the flow of its own rivers, so those rivers will look still. Streams' own rivers are unaffected. If you expected a current there, please report this line."
                           );
                     }
                  }
               }
            }
         } catch (ReflectiveOperationException | LinkageError var9) {
            Constants.LOG.debug("Streams Reflowing: RTF handle failed ({}).", var9.toString());
         }

         if (var12) {
            b();
            return null;
         }

         synchronized (e) {
            e.put(var0, var11 == null ? f : var11);
            return var11;
         }
      } else {
         return null;
      }
   }

   private static void b() {
      if (g.compareAndSet(false, true)) {
         Constants.LOG
            .warn(
               "Streams Reflowing: ReTerraForged is installed but its terrain was not available when this world's streams were prepared, so heights are being read the slow way. If stream generation is slow here, please report this line."
            );
      }
   }

   public static final class Handle {
      private final RTFBridge.Surface a;
      private final Object b;
      private final int c;
      private final ThreadLocal<Object> d;
      private final Map<Long, Long> e = new ConcurrentHashMap<>();
      private static final int f = 400000;
      private final Map<Long, Float> g = new ConcurrentHashMap<>();

      Handle(RTFBridge.Surface var1, Object var2, int var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = ThreadLocal.withInitial(() -> {
            try {
               return var1.g.newInstance();
            } catch (ReflectiveOperationException var2x) {
               throw new IllegalStateException(var2x);
            }
         });
      }

      private long a(int var1, int var2) {
         try {
            Object var3 = this.d.get();
            this.a.f.invoke(this.b, var3, var1, var2, false);
            float var4 = this.a.h.getFloat(var3) * this.c;
            boolean var5 = this.a.i.get(var3) == this.a.k;
            long var6 = (long)Float.floatToIntBits(var4) << 32 | (var5 ? 1L : 0L);
            if (var5) {
               this.g.put(ColumnKey.of(var1, var2), this.a.j.getFloat(var3) * this.c);
            }

            if (this.e.size() >= 400000) {
               this.e.clear();
               this.g.clear();
            }

            this.e.put(ColumnKey.of(var1, var2), var6);
            return var6;
         } catch (ReflectiveOperationException var8) {
            throw new IllegalStateException("RTF bridge invocation failed", var8);
         }
      }

      private long b(int var1, int var2) {
         Long var3 = this.e.get(ColumnKey.of(var1, var2));
         return var3 != null ? var3 : this.a(var1, var2);
      }

      public double height(int var1, int var2) {
         return Float.intBitsToFloat((int)(this.b(var1, var2) >>> 32));
      }

      public boolean river(int var1, int var2) {
         return (this.b(var1, var2) & 1L) != 0L;
      }

      public double riverSurface(int var1, int var2) {
         if ((this.b(var1, var2) & 1L) == 0L) {
            return Double.NaN;
         }

         Float var3 = this.g.get(ColumnKey.of(var1, var2));
         if (var3 == null) {
            this.a(var1, var2);
            var3 = this.g.get(ColumnKey.of(var1, var2));
         }

         return var3 != null ? var3.floatValue() : Double.NaN;
      }
   }

   private static final class Surface {
      final Class<?> a;
      final Method b;
      final Field c;
      final Field d;
      final Field e;
      final Method f;
      final Constructor<?> g;
      final Field h;
      final Field i;
      final Field j;
      final Object k;
      final Class<?> l;
      final Method m;
      final Method n;
      final Method o;

      Surface(String[] var1) throws ReflectiveOperationException {
         String var2 = var1[0] + ".world.worldgen.";
         Class var3 = Class.forName(var2 + var1[1]);
         Class var4 = Class.forName(var2 + "GeneratorContext");
         Class var5 = Class.forName(var2 + "cell.heightmap.WorldLookup");
         Class var6 = Class.forName(var2 + "cell.heightmap.Levels");
         Class var7 = Class.forName(var2 + "cell.Cell");
         Class var8 = Class.forName(var2 + "cell.terrain.TerrainType");
         this.a = var3;
         this.b = var3.getMethod("generatorContext");
         this.c = var4.getField("lookup");
         this.d = var4.getField("levels");

         Field var9;
         try {
            var9 = var6.getField("terrainScaleFactor");
         } catch (NoSuchFieldException var16) {
            var9 = var6.getField("worldHeight");
         }

         this.e = var9;
         this.f = var5.getMethod("applyCell", var7, int.class, int.class, boolean.class);
         this.g = var7.getConstructor();
         this.h = var7.getField("height");
         this.i = var7.getField("terrain");
         this.j = var7.getField("riverWaterLevel");
         this.k = var8.getField("RIVER").get(null);
         Class var10 = null;
         Object var11 = null;
         Object var12 = null;
         Object var13 = null;

         try {
            var10 = Class.forName(var2 + "IFlowFieldHolder");
            Class var14 = Class.forName(var2 + "ChunkFlowField");
            var11 = var10.getMethod(var1[2]);
            var12 = var14.getMethod("hasRivers");
            var13 = var14.getMethod("getRawGrid");
         } catch (ReflectiveOperationException | LinkageError var15) {
            var10 = null;
            var11 = null;
            var12 = null;
            var13 = null;
         }

         this.l = var10;
         this.m = (Method)var11;
         this.n = (Method)var12;
         this.o = (Method)var13;
      }
   }
}
