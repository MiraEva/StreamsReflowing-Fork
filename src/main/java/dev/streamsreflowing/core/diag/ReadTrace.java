package dev.streamsreflowing.core.diag;

import dev.streamsreflowing.Constants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class ReadTrace {
   public static final boolean ON = Boolean.getBoolean("streamsreflowing.readtrace");
   public static final char HEIGHT = 'h';
   public static final char GROUND = 'g';
   public static final char GROUND_EXACT = 'x';
   public static final char SURFACE = 's';
   public static final char GROUND_REAL = 'r';
   public static final char SURFACE_REAL = 'R';
   public static final char BATCH_HEIGHT = 'b';
   public static final char BATCH_GROUND = 'B';
   private int[] a = new int[32768];
   private int b;

   public synchronized void add(int var1, int var2, char var3, double var4) {
      if (this.b + 4 > this.a.length) {
         this.a = Arrays.copyOf(this.a, this.a.length * 2);
      }

      this.a[this.b++] = var1;
      this.a[this.b++] = var2;
      this.a[this.b++] = HeightCensus.current();
      this.a[this.b++] = var4 == -1000.0 ? -var3 : var3;
   }

   public void write(long var1, int var3, int var4, int var5, int var6, int var7, int var8) {
      int[] var9;
      int var10;
      synchronized (this) {
         var9 = this.a;
         var10 = this.b;
      }

      try {
         Path var11 = Path.of("streamsreflowing-readtrace", Long.toString(var1));
         Files.createDirectories(var11);
         Path var12 = var11.resolve("plate_" + var3 + "_" + var4 + ".txt");

         try (BufferedWriter var13 = Files.newBufferedWriter(var12, StandardCharsets.UTF_8)) {
            var13.write("# seed " + var1 + " plate " + var3 + " " + var4 + " grid " + var5 + " sample " + var6 + " fit " + var7 + " lakePitch " + var8 + "\n");
            var13.write("# sites " + String.join(",", HeightCensus.siteNames()) + "\n");
            var13.write("# x z site kind ocean\n");
            StringBuilder var14 = new StringBuilder(64);

            for (int var15 = 0; var15 < var10; var15 += 4) {
               int var16 = var9[var15 + 3];
               var14.setLength(0);
               var14.append(var9[var15])
                  .append(' ')
                  .append(var9[var15 + 1])
                  .append(' ')
                  .append(var9[var15 + 2])
                  .append(' ')
                  .append((char)Math.abs(var16))
                  .append(' ')
                  .append(var16 < 0 ? 1 : 0)
                  .append('\n');
               var13.write(var14.toString());
            }
         }

         Constants.LOG.info("[SR-READTRACE] plate {},{}: {} reads written to {}", new Object[]{var3, var4, var10 / 4, var12.toAbsolutePath()});
      } catch (IOException | RuntimeException var20) {
         Constants.LOG.warn("[SR-READTRACE] plate {},{}: could not write the read trace", new Object[]{var3, var4, var20});
      }
   }
}
