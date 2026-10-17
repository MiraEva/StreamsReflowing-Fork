package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.build.BuildGate;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlateRiverCache {
   private static final int a = 128;
   private static final long b = Long.getLong("streamsreflowing.platecachemb", 64L) * 1024L * 1024L;
   private static final long c = 60000000000L;
   private static final long d = 5000000000L;
   private static final Set<PlateRiverCache> e = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
   private static volatile boolean f;
   private static final boolean g = Boolean.getBoolean("streamsreflowing.caldiag");
   private final ConcurrentHashMap<PlateKey, CompletableFuture<RiverNetwork>> h = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<PlateKey, long[]> i = new ConcurrentHashMap<>();
   private final AtomicLong j = new AtomicLong();
   private final ConcurrentHashMap<PlateKey, Integer> k = new ConcurrentHashMap<>();
   private final ReentrantLock l = new ReentrantLock();
   private volatile long m;
   private volatile boolean n;
   private final ConcurrentHashMap<PlateKey, CompletableFuture<Void>> o = new ConcurrentHashMap<>();
   private static final AtomicInteger p = new AtomicInteger();
   private static final long q = 30000000000L;
   private static final ConcurrentHashMap<PlateKey, Long> r = new ConcurrentHashMap<>();
   private static final long s = 300000000000L;

   public PlateRiverCache() {
      e.add(this);
   }

   public static long diskBackedBudgetBytes() {
      return b;
   }

   public static void releaseAllWaiters() {
      f = true;
      synchronized (e) {
         for (PlateRiverCache var2 : e) {
            for (Entry var4 : var2.o.entrySet()) {
               ((CompletableFuture)var4.getValue()).complete(null);
            }

            var2.o.clear();
         }
      }
   }

   public static void reopen() {
      f = false;
   }

   public int failures(PlateKey var1) {
      return this.k.getOrDefault(var1, 0);
   }

   public static int foreignHeld() {
      return p.get();
   }

   private static void a(PlateKey var0, long var1, boolean var3) {
      long var4 = System.nanoTime();
      Long var6 = r.get(var0);
      if (!var3) {
         if (var6 != null && var4 - var6 < 28000000000L) {
            return;
         }

         if (var6 == null ? r.putIfAbsent(var0, var4) != null : !r.replace(var0, var6, var4)) {
            return;
         }
      }

      long var7 = (var4 - var1) / 1000000000L;
      if (var3) {
         Constants.LOG
            .warn(
               "Streams Reflowing: the streams of region ({},{}) were still not planned after {} s; chunk generation is planning them itself.",
               new Object[]{var0.cx(), var0.cz(), var7}
            );
      } else {
         Constants.LOG
            .info(
               "Streams Reflowing: chunk generation is waiting for the streams of region ({},{}) to be planned ({} s so far).",
               new Object[]{var0.cx(), var0.cz(), var7}
            );
      }
   }

   public void setDiskBacked(boolean var1) {
      this.n = var1;
   }

   public RiverNetwork get(PlateKey var1, Supplier<RiverNetwork> var2, Supplier<RiverNetwork> var3) {
      CompletableFuture var4 = this.h.get(var1);
      if (var4 != null) {
         RiverNetwork var20 = a(var1, var4, var3);
         this.a(var1);
         return var20;
      }

      CompletableFuture var5 = new CompletableFuture();
      CompletableFuture var6 = this.h.putIfAbsent(var1, var5);
      if (var6 != null) {
         RiverNetwork var22 = a(var1, var6, var3);
         this.a(var1);
         return var22;
      }

      if (g) {
         String var7 = Thread.currentThread().getName();
         StringBuilder var8 = new StringBuilder();
         if (!var7.startsWith("streamsreflowing-")) {
            int var9 = 0;

            for (StackTraceElement var13 : Thread.currentThread().getStackTrace()) {
               String var14 = var13.getClassName();
               if (!var14.startsWith("java.") && !var14.startsWith("jdk.") && !var14.endsWith("PlateRiverCache") && !var14.endsWith("Thread")) {
                  var8.append(var9 == 0 ? " via " : " < ").append(var14.substring(var14.lastIndexOf(46) + 1)).append('.').append(var13.getMethodName());
                  if (++var9 == 8) {
                     break;
                  }
               }
            }
         }

         Constants.LOG.info("[SR-BUILDSTART] plate ({},{}) on {}{}", new Object[]{var1.cx(), var1.cz(), var7, var8});
      }

      boolean var23 = !Thread.currentThread().getName().startsWith("streamsreflowing-");
      if (var23) {
         p.incrementAndGet();
      }

      RiverNetwork var21;
      try {
         var21 = (RiverNetwork)var2.get();
      } catch (Throwable var18) {
         this.h.remove(var1, var5);
         var5.completeExceptionally(var18);
         int var25 = this.k.merge(var1, 1, Integer::sum);
         if (var25 <= 2) {
            Constants.LOG
               .warn(
                  "Streams Reflowing: preparing the streams of plate ({},{}) failed (attempt {}); land there generates without them until this is fixed.",
                  new Object[]{var1.cx(), var1.cz(), var25, var18}
               );
         }

         CompletableFuture var27 = this.o.remove(var1);
         if (var27 != null) {
            var27.complete(null);
         }

         throw var18;
      } finally {
         if (var23) {
            p.decrementAndGet();
         }
      }

      var5.complete(var21);
      this.k.remove(var1);
      CompletableFuture var24 = this.o.remove(var1);
      if (var24 != null) {
         var24.complete(null);
      }

      long var26 = var21.approxBytes();
      this.i.put(var1, new long[]{var26, System.nanoTime()});
      this.j.addAndGet(var26);
      this.b(var1);
      return var21;
   }

   private void a(PlateKey var1) {
      long[] var2 = this.i.get(var1);
      if (var2 != null) {
         long var3 = System.nanoTime();
         var2[1] = var3;
         if (this.n && this.j.get() > b && var3 - this.m > 5000000000L) {
            this.b(null);
         }
      }
   }

   private void b(PlateKey var1) {
      if (this.l.tryLock()) {
         try {
            long var2 = System.nanoTime();
            this.m = var2;
            long var4 = this.n ? b : Long.MAX_VALUE;
            if (this.j.get() <= var4 && this.h.size() <= 128) {
               return;
            }

            ArrayList<Entry<PlateKey, long[]>> var6 = new ArrayList<>(this.i.entrySet());
            var6.sort(Comparator.comparingLong(var0 -> ((long[])var0.getValue())[1]));

            for (Entry var8 : var6) {
               boolean var9 = this.h.size() > 128;
               boolean var10 = this.j.get() > var4;
               if (!var9 && !var10) {
                  break;
               }

               if (!((PlateKey)var8.getKey()).equals(var1)) {
                  if (!var9 && var2 - ((long[])var8.getValue())[1] < 60000000000L) {
                     break;
                  }

                  this.a((PlateKey)var8.getKey(), (long[])var8.getValue());
               }
            }
         } finally {
            this.l.unlock();
         }
      }
   }

   private void a(PlateKey var1, long[] var2) {
      CompletableFuture var3 = this.h.get(var1);
      if (var3 != null && var3.isDone()) {
         if (this.h.remove(var1, var3)) {
            this.i.remove(var1, var2);
            this.j.addAndGet(-var2[0]);
         }
      }
   }

   private static RiverNetwork a(PlateKey var0, CompletableFuture<RiverNetwork> var1, Supplier<RiverNetwork> var2) {
      if (var1.isDone()) {
         return (RiverNetwork)var1.join();
      }

      boolean var3 = Thread.currentThread().getName().startsWith("streamsreflowing-");
      boolean var4 = LowResGrid.noteUrgentWaiter();
      if (!var3 || LowResGrid.isUrgentBuild()) {
         BuildGate.promote((long)var0.cx() << 32 ^ var0.cz() & 4294967295L);
      }

      try {
         if (!var3 && var2 != null) {
            p.incrementAndGet();
            long var5 = System.nanoTime();

            try {
               while (true) {
                  long var7 = 300000000000L - (System.nanoTime() - var5);
                  if (var7 <= 0L) {
                     a(var0, var5, true);
                     return (RiverNetwork)var2.get();
                  }

                  try {
                     return (RiverNetwork)var1.get(Math.min(var7, 30000000000L), TimeUnit.NANOSECONDS);
                  } catch (TimeoutException var22) {
                     a(var0, var5, false);
                  }
               }
            } catch (InterruptedException var23) {
               Thread.currentThread().interrupt();
               return (RiverNetwork)var2.get();
            } catch (ExecutionException var24) {
               Throwable var8 = var24.getCause();
               throw new CompletionException(var8);
            } finally {
               p.decrementAndGet();
            }
         } else {
            return (RiverNetwork)var1.join();
         }
      } finally {
         if (var4) {
            LowResGrid.urgentWaiterDone();
         }
      }
   }

   public CompletableFuture<Void> whenReady(PlateKey var1) {
      if (f) {
         return CompletableFuture.completedFuture(null);
      }

      CompletableFuture var2 = this.h.get(var1);
      if (var2 != null) {
         return var2.handle((var0, var1x) -> null);
      }

      CompletableFuture var3 = this.o.computeIfAbsent(var1, var0 -> new CompletableFuture<>());
      CompletableFuture var4 = this.h.get(var1);
      if (var4 != null && var4.isDone()) {
         this.o.remove(var1);
         var3.complete(null);
      }

      return var3;
   }

   public void markWanted(PlateKey var1) {
      this.a(var1);
   }

   public boolean contains(PlateKey var1) {
      return this.h.containsKey(var1);
   }

   public boolean isReady(PlateKey var1) {
      CompletableFuture var2 = this.h.get(var1);
      return var2 != null && var2.isDone() && !var2.isCompletedExceptionally() && !var2.isCancelled();
   }

   public void forEachResident(Consumer<RiverNetwork> var1) {
      for (CompletableFuture var3 : this.h.values()) {
         if (var3.isDone() && !var3.isCompletedExceptionally()) {
            RiverNetwork var4 = (RiverNetwork)var3.getNow(null);
            if (var4 != null) {
               var1.accept(var4);
            }
         }
      }
   }

   public int size() {
      return this.h.size();
   }

   public void clear() {
      this.h.clear();
      this.i.clear();
      this.j.set(0L);
   }

   public long residentBytes() {
      return this.j.get();
   }
}
