package dev.streamsreflowing.platform;

import dev.streamsreflowing.Constants;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class PresetOverrides {
   public static final String DIR_NAME = "streamsreflowing";
   public static final String FILE_NAME = "advanced.properties";
   public static final String K_GRID_SPACING = "gridSpacing";
   public static final String K_TERRAIN_SAMPLE_SPACING = "terrainSampleSpacing";
   public static final String K_VALLEY_SNAP_RADIUS = "valleySnapRadius";
   public static final String K_LAKE_SAMPLE_SPACING = "lakeSampleSpacing";
   public static final String K_PREFETCH_RADIUS = "prefetchRadius";
   public static final String K_GRADE_WINDOW_ABOVE = "gradeWindowAbove";
   public static final String K_GRADE_WINDOW_BELOW = "gradeWindowBelow";
   private static final Object[][] KEYS = new Object[][]{
      {"gridSpacing", 2.0, 64.0, true},
      {"terrainSampleSpacing", 0.0, 256.0, true},
      {"valleySnapRadius", 0.0, 64.0, true},
      {"lakeSampleSpacing", 1.0, 64.0, true},
      {"prefetchRadius", 0.0, 3.0, true},
      {"gradeWindowAbove", 0.0, 32.0, false},
      {"gradeWindowBelow", 0.0, 32.0, false}
   };
   private static volatile Map<String, Double> values;

   private PresetOverrides() {
   }

   public static synchronized void reload(Path configDir, Properties mainFile) {
      Map<String, String> raw = new HashMap<>();
      Path side = configDir.resolve("streamsreflowing").resolve("advanced.properties");
      if (Files.isRegularFile(side)) {
         Properties p = new Properties();

         try (InputStream in = Files.newInputStream(side)) {
            p.load(in);
            take(raw, p);
         } catch (Exception e) {
            Constants.LOG.warn("Streams Reflowing: could not read {} -- ignored", side, e);
         }
      }

      if (mainFile != null) {
         take(raw, mainFile);
      }

      Map<String, Double> parsed = new HashMap<>();

      for (Object[] spec : KEYS) {
         String key = (String)spec[0];
         String text = raw.get(key);
         if (text != null) {
            double lo = (Double)spec[1];
            double hi = (Double)spec[2];
            boolean integer = (Boolean)spec[3];

            double v;
            try {
               v = integer ? Integer.parseInt(text) : Double.parseDouble(text);
            } catch (NumberFormatException e) {
               Constants.LOG.warn("Streams Reflowing: preset override {}={} is not a number -- ignored", key, text);
               continue;
            }

            double c = Math.max(lo, Math.min(hi, v));
            if (c != v) {
               Constants.LOG
                  .warn(
                     "Streams Reflowing: preset override {}={} is outside {}..{} -- using {}",
                     new Object[]{
                        key,
                        text,
                        integer ? Integer.toString((int)lo) : Double.toString(lo),
                        integer ? Integer.toString((int)hi) : Double.toString(hi),
                        integer ? Integer.toString((int)c) : Double.toString(c)
                     }
                  );
            }

            parsed.put(key, c);
            Constants.LOG.info("Streams Reflowing: preset override {}={}", key, integer ? Integer.toString((int)c) : Double.toString(c));
         }
      }

      values = parsed;
   }

   private static void take(Map<String, String> into, Properties from) {
      for (Object[] spec : KEYS) {
         String key = (String)spec[0];
         String v = from.getProperty(key);
         if (v != null && !v.trim().isEmpty()) {
            into.put(key, v.trim());
         }
      }
   }

   private static Map<String, Double> current() {
      Map<String, Double> v = values;
      if (v == null) {
         reload(Services.PLATFORM.configDir(), null);
         v = values;
      }

      return v;
   }

   private static int intOr(String key, int preset) {
      Double v = current().get(key);
      return v == null ? preset : (int)v.doubleValue();
   }

   private static double doubleOr(String key, double preset) {
      Double v = current().get(key);
      return v == null ? preset : v;
   }

   public static int gridSpacing(int preset) {
      return intOr("gridSpacing", preset);
   }

   public static int terrainSampleSpacing(int preset) {
      return intOr("terrainSampleSpacing", preset);
   }

   public static int valleySnapRadius(int preset) {
      return intOr("valleySnapRadius", preset);
   }

   public static int lakeSampleSpacing(int preset) {
      return intOr("lakeSampleSpacing", preset);
   }

   public static int prefetchRadius(int preset) {
      return intOr("prefetchRadius", preset);
   }

   public static double gradeWindowAbove(double preset) {
      return doubleOr("gradeWindowAbove", preset);
   }

   public static double gradeWindowBelow(double preset) {
      return doubleOr("gradeWindowBelow", preset);
   }
}
