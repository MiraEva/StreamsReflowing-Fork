package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.flow.FlowTrace;
import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class FrameDiag {
   private static final boolean ON = Boolean.getBoolean("streamsreflowing.framediag");
   private static final int STALL_MS = Math.max(16, Integer.getInteger("streamsreflowing.framestall", 100));
   private static final long WATCH_INTERVAL_MS = 10L;
   private static final long CAPTURE_GAP_MS = 50L;
   private static final int MAX_CAPTURES_PER_STALL = 14;
   private static final long REPORT_EVERY_MS = 30000L;
   private static final long WARMUP_MS = 30000L;
   private static volatile long heartbeatMs;
   private static volatile Thread clientThread;
   private static volatile boolean perFrame;
   private static volatile boolean started;
   private static volatile long firstTickMs;
   private static long ticks;
   private static final ThreadMXBean MX = ManagementFactory.getThreadMXBean();
   private static final boolean CPU_TIME_OK = cpuTimeAvailable();
   private static final long[] BAND_MAX = new long[]{100L, 250L, 1000L, Long.MAX_VALUE};
   private static final String[] BAND_NAME = new String[]{"under 100ms", "100-250ms", "250ms-1s", "over 1s"};
   private static final FrameDiag.Band[] bands = newBands();
   private static final List<Long> durations = new ArrayList<>();
   private static final List<FrameDiag.Stall> worst = new ArrayList<>();
   private static long totalStallMs;
   private static long worstStallMs;
   private static int stallCount;
   private static int idleCount;
   private static long idleMs;

   private static boolean cpuTimeAvailable() {
      try {
         if (!MX.isThreadCpuTimeSupported()) {
            return false;
         }

         if (!MX.isThreadCpuTimeEnabled()) {
            MX.setThreadCpuTimeEnabled(true);
         }

         return MX.isThreadCpuTimeEnabled();
      } catch (Throwable t) {
         return false;
      }
   }

   private static FrameDiag.Band[] newBands() {
      FrameDiag.Band[] b = new FrameDiag.Band[BAND_MAX.length];

      for (int i = 0; i < b.length; i++) {
         b[i] = new FrameDiag.Band();
      }

      return b;
   }

   private FrameDiag() {
   }

   public static void tick(int fps) {
      beat();
   }

   public static void frame() {
      perFrame = true;
      beat();
   }

   private static void beat() {
      if (ON) {
         long now = System.currentTimeMillis();
         clientThread = Thread.currentThread();
         heartbeatMs = now;
         ticks++;
         if (!started) {
            firstTickMs = now;
            started = true;
            startWatchdog();
         }
      }
   }

   private static void startWatchdog() {
      Thread t = new Thread(FrameDiag::watch, "streamsreflowing-framediag");
      t.setDaemon(true);
      t.setPriority(Math.min(10, 6));
      t.start();
   }

   private static void watch() {
      long lastReportMs = System.currentTimeMillis();
      long lastSeenBeat = 0L;
      long baseCpuNanos = -1L;
      long baseAtMs = 0L;

      while (true) {
         try {
            Thread.sleep(10L);
         } catch (InterruptedException e) {
            return;
         }

         try {
            long beat = heartbeatMs;
            long now = System.currentTimeMillis();
            if (beat != lastSeenBeat) {
               lastSeenBeat = beat;
               baseCpuNanos = clientCpuNanos();
               baseAtMs = now;
            }

            if (now - beat >= STALL_MS && now - firstTickMs >= 30000L) {
               if (HarnessPilot.active()) {
                  FlowTrace.harnessStalls.incrementAndGet();
               }

               observeStall(beat, baseCpuNanos, baseAtMs);
               now = System.currentTimeMillis();
            }

            if (now - lastReportMs >= 30000L) {
               lastReportMs = now;
               report();
            }
         } catch (Throwable var12) {
         }
      }
   }

   private static long clientCpuNanos() {
      Thread t = clientThread;
      if (CPU_TIME_OK && t != null) {
         try {
            return MX.getThreadCpuTime(t.getId());
         } catch (Throwable ignored) {
            return -1L;
         }
      } else {
         return -1L;
      }
   }

   private static void observeStall(long beatAtStart, long baseCpuNanos, long baseAtMs) {
      Map<String, Long> blame = new HashMap<>();
      String clientState = "unknown";
      int captures = 0;
      int sleeping = 0;
      int awake = 0;
      long lastCaptureMs = 0L;
      List<String> pending = new ArrayList<>();
      Map<String, Long> clientWhere = new HashMap<>();
      List<String> clientPending = new ArrayList<>();
      long watchdogStartMs = System.currentTimeMillis();
      long gap = 50L;
      long cpuWindowFromMs = System.currentTimeMillis();
      Map<Long, Long> cpuAtStart = snapshotCpu();

      while (heartbeatMs == beatAtStart) {
         long now = System.currentTimeMillis();
         if (captures < 14 && now - lastCaptureMs >= gap) {
            long span = captures == 0 ? 0L : now - lastCaptureMs;
            credit(blame, pending, span);
            credit(clientWhere, clientPending, span);
            lastCaptureMs = now;
            captures++;
            gap = Math.min(1000L, gap * 8L / 5L);
            String st = sampleInto(pending);
            if (st != null) {
               clientState = st;
               clientPending.add(st);
               if (st.contains("in Thread.sleep")) {
                  sleeping++;
               } else {
                  awake++;
               }
            }
         }

         if (now - watchdogStartMs > 60000L) {
            return;
         }

         try {
            Thread.sleep(captures >= 14 ? 50L : 10L);
         } catch (InterruptedException e) {
            return;
         }
      }

      long tail = Math.max(0L, heartbeatMs - lastCaptureMs);
      credit(blame, pending, tail);
      credit(clientWhere, clientPending, tail);
      long duration = Math.max(0L, heartbeatMs - beatAtStart);
      Map<String, Long> cpuMs = new HashMap<>();
      long[] totalCpu = new long[1];
      diffCpu(cpuAtStart, cpuMs, totalCpu);
      long totalWindow = Math.max(1L, heartbeatMs - cpuWindowFromMs);
      long clientWindow = Math.max(1L, heartbeatMs - baseAtMs);
      long clientCpuMs = -1L;
      if (baseCpuNanos >= 0L) {
         long endCpu = clientCpuNanos();
         if (endCpu >= 0L) {
            clientCpuMs = Math.max(0L, (endCpu - baseCpuNanos) / 1000000L);
         }
      }

      long sleepMs = 0L;
      long awakeMs = 0L;

      for (Entry<String, Long> e : clientWhere.entrySet()) {
         if (e.getKey().contains("in Thread.sleep")) {
            sleepMs += e.getValue();
         } else {
            awakeMs += e.getValue();
         }
      }

      if (sleepMs <= awakeMs && (sleeping <= 0 || awake != 0)) {
         stallCount++;
         totalStallMs += duration;
         if (duration > worstStallMs) {
            worstStallMs = duration;
         }

         FrameDiag.Band band = bands[bandOf(duration)];
         band.count++;
         band.totalMs += duration;
         if (clientCpuMs >= 0L) {
            band.clientCpuMs += clientCpuMs;
            band.clientCpuOverMs += clientWindow;
         }

         band.totalCpuMs = band.totalCpuMs + totalCpu[0];
         band.totalCpuOverMs += totalWindow;
         blame.forEach((k, v) -> band.blame.merge(k, v, Long::sum));
         cpuMs.forEach((k, v) -> band.cpu.merge(k, v, Long::sum));
         clientWhere.forEach((k, v) -> band.clientWhere.merge(k, v, Long::sum));
         if (durations.size() < 20000) {
            durations.add(duration);
         }

         FrameDiag.Stall stall = new FrameDiag.Stall(duration, clientState, blame, cpuMs, clientCpuMs, clientWhere, totalCpu[0]);
         worst.add(stall);
         worst.sort(Comparator.comparingLong(FrameDiag.Stall::durationMs).reversed());

         while (worst.size() > 3) {
            worst.remove(worst.size() - 1);
         }
      } else {
         idleCount++;
         idleMs += duration;
      }
   }

   private static int bandOf(long durationMs) {
      for (int i = 0; i < BAND_MAX.length; i++) {
         if (durationMs < BAND_MAX[i]) {
            return i;
         }
      }

      return BAND_MAX.length - 1;
   }

   private static void credit(Map<String, Long> blame, List<String> pending, long ms) {
      if (ms > 0L) {
         for (String frame : pending) {
            blame.merge(frame, ms, Long::sum);
         }
      }

      pending.clear();
   }

   private static Map<Long, Long> snapshotCpu() {
      Map<Long, Long> out = new HashMap<>();
      if (!CPU_TIME_OK) {
         return out;
      }

      try {
         for (Thread t : Thread.getAllStackTraces().keySet()) {
            long cpu = MX.getThreadCpuTime(t.getId());
            if (cpu >= 0L) {
               out.put(t.getId(), cpu);
            }
         }
      } catch (Throwable var5) {
      }

      return out;
   }

   private static long diffCpu(Map<Long, Long> start, Map<String, Long> perPoolMs, long[] totalOut) {
      if (CPU_TIME_OK && !start.isEmpty()) {
         long clientMs = -1L;

         try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
               Long before = start.get(t.getId());
               if (before != null) {
                  long now = MX.getThreadCpuTime(t.getId());
                  if (now >= 0L) {
                     long ms = (now - before) / 1000000L;
                     if (ms > 0L) {
                        totalOut[0] += ms;
                        if (t.getName().equals("Render thread")) {
                           clientMs = ms;
                        } else if (accountable(t.getName())) {
                           perPoolMs.merge(pool(t.getName()), ms, Long::sum);
                        }
                     }
                  }
               }
            }
         } catch (Throwable var12) {
         }

         return clientMs;
      } else {
         return -1L;
      }
   }

   private static boolean accountable(String name) {
      return name.equals("Render thread")
         || name.startsWith("streamsreflowing-")
         || name.startsWith("c2me")
         || name.startsWith("Worker-Main")
         || name.startsWith("Server thread")
         || name.equals("main");
   }

   private static String sampleInto(List<String> out) {
      String clientState = null;
      Thread self = Thread.currentThread();

      for (Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
         Thread t = e.getKey();
         String name = t.getName();
         StackTraceElement[] st = e.getValue();
         if (st.length != 0 && t != self) {
            if (name.equals("Render thread")) {
               clientState = t.getState() + " in " + shorten(st[0]) + lockDetail(t);
            } else if (t.getState() == State.RUNNABLE
               && (
                  name.startsWith("streamsreflowing-")
                     || name.startsWith("c2me-")
                     || name.startsWith("Worker-Main")
                     || name.equals("main")
                     || name.startsWith("Server thread")
               )) {
               String frame = null;
               StackTraceElement[] var9 = st;
               int var10 = var9.length;
               int var11 = 0;

               while (true) {
                  if (var11 < var10) {
                     StackTraceElement f = var9[var11];
                     String cls = f.getClassName();
                     if (cls.endsWith(".FrameDiag")
                        || cls.contains("ReflowingFabricClient")
                        || cls.contains("ReflowingNeoForgeClient")
                        || !cls.startsWith("dev.streamsreflowing.")) {
                        var11++;
                        continue;
                     }

                     frame = shorten(f);
                  }

                  if (frame == null) {
                     frame = "[not ours] " + shorten(st[0]);
                  }

                  out.add(frame + " [" + pool(name) + "]");
                  break;
               }
            }
         }
      }

      return clientState;
   }

   private static String lockDetail(Thread t) {
      if (!CPU_TIME_OK) {
         return "";
      }

      try {
         ThreadInfo info = MX.getThreadInfo(t.getId());
         if (info == null) {
            return "";
         }

         String owner = info.getLockOwnerName();
         String lock = info.getLockName();
         if (owner != null) {
            return " blocked by " + pool(owner);
         }

         if (lock != null) {
            int at = lock.indexOf(64);
            String cls = at > 0 ? lock.substring(0, at) : lock;
            int dot = cls.lastIndexOf(46);
            return " on " + (dot < 0 ? cls : cls.substring(dot + 1));
         }
      } catch (Throwable var7) {
      }

      return "";
   }

   private static String pool(String name) {
      if (name.startsWith("streamsreflowing-")) {
         return "sr-" + name.substring("streamsreflowing-".length()).replaceAll("[-_]?\\d+$", "");
      } else if (name.startsWith("c2me")) {
         return "c2me";
      } else if (name.startsWith("Worker-Main")) {
         return "mc-worker";
      } else {
         return name.startsWith("Server thread") ? "server" : name;
      }
   }

   private static String shorten(StackTraceElement f) {
      String cls = f.getClassName();
      int dot = cls.lastIndexOf(46);
      return (dot < 0 ? cls : cls.substring(dot + 1)) + "." + f.getMethodName();
   }

   private static String top(Map<String, Long> blame, int limit) {
      StringBuilder sb = new StringBuilder();
      blame.entrySet()
         .stream()
         .sorted(Comparator.<Entry<String, Long>>comparingLong(Entry::getValue).reversed())
         .limit(limit)
         .forEach(en -> sb.append(sb.length() == 0 ? "" : "; ").append(en.getValue()).append("ms ").append(en.getKey()));
      return sb.length() == 0 ? "nothing of ours was running" : sb.toString();
   }

   private static void report() {
      if (stallCount == 0) {
         Constants.LOG.info("[SR-FRAME] {} ticks, no freeze longer than {}ms ({} idle stretches ignored)", new Object[]{ticks, STALL_MS, idleCount});
      } else {
         Constants.LOG
            .info(
               "[SR-FRAME] {} {}, {} freezes over {}ms, {}ms lost, worst {}ms, median {}ms; {} idle ignored ({}ms)",
               new Object[]{
                  ticks,
                  perFrame ? "frames" : "TICKS ONLY (sub-100ms figures are tick jitter, not freezes)",
                  stallCount,
                  STALL_MS,
                  totalStallMs,
                  worstStallMs,
                  medianMs(),
                  idleCount,
                  idleMs
               }
            );

         for (int i = 0; i < bands.length; i++) {
            FrameDiag.Band b = bands[i];
            if (b.count != 0) {
               Constants.LOG
                  .info(
                     "[SR-FRAME]   {} x{}: {}ms lost, avg {}ms, client {}, whole game used {} | cpu: {}",
                     new Object[]{BAND_NAME[i], b.count, b.totalMs, b.totalMs / b.count, share(b), cores(b), cpuLine(b.cpu)}
                  );
               Constants.LOG.info("[SR-FRAME]       client was: {}", top(b.clientWhere, 3));
               Constants.LOG.info("[SR-FRAME]       running: {}", top(b.blame, 6));
            }
         }

         for (FrameDiag.Stall s : worst) {
            Constants.LOG
               .info("[SR-FRAME]   longest {}ms: client {}{} | running: {}", new Object[]{s.durationMs(), s.clientState(), clientShare(s), top(s.blame(), 4)});
         }
      }
   }

   private static String clientShare(FrameDiag.Stall s) {
      return s.clientCpuMs() >= 0L && s.durationMs() > 0L
         ? String.format(", got %dms cpu (%d%% of a core)", s.clientCpuMs(), Math.round(100.0 * s.clientCpuMs() / s.durationMs()))
         : "";
   }

   private static String cpuLine(Map<String, Long> cpuMs) {
      if (cpuMs.isEmpty()) {
         return "unavailable";
      }

      StringBuilder sb = new StringBuilder();
      cpuMs.entrySet()
         .stream()
         .sorted(Comparator.<Entry<String, Long>>comparingLong(Entry::getValue).reversed())
         .limit(5L)
         .forEach(en -> sb.append(sb.length() == 0 ? "" : ", ").append(en.getKey()).append(' ').append(en.getValue()).append("ms"));
      return sb.toString();
   }

   private static String share(FrameDiag.Band b) {
      return b.clientCpuOverMs <= 0L ? "cpu unknown" : "held " + Math.round(100.0 * b.clientCpuMs / b.clientCpuOverMs) + "% of a core";
   }

   private static String cores(FrameDiag.Band b) {
      if (b.totalCpuOverMs <= 0L) {
         return "?";
      }

      double used = (double)b.totalCpuMs / b.totalCpuOverMs;
      return String.format("%.1f of %d cores", used, Runtime.getRuntime().availableProcessors());
   }

   private static long medianMs() {
      if (durations.isEmpty()) {
         return 0L;
      }

      long[] d = durations.stream().mapToLong(Long::longValue).sorted().toArray();
      return d[d.length / 2];
   }

   private static final class Band {
      int count;
      long totalMs;
      long clientCpuMs;
      long clientCpuOverMs;
      long totalCpuMs;
      long totalCpuOverMs;
      final Map<String, Long> blame = new HashMap<>();
      final Map<String, Long> cpu = new HashMap<>();
      final Map<String, Long> clientWhere = new HashMap<>();
   }

   private record Stall(
      long durationMs, String clientState, Map<String, Long> blame, Map<String, Long> cpuMs, long clientCpuMs, Map<String, Long> clientWhere, long totalCpuMs
   ) {
   }
}
