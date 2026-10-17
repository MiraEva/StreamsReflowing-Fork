package dev.streamsreflowing.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.streamsreflowing.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class BakedBandTable {
   private static final String[] a = new String[]{"tectonic", "mr_lithosphere", "mr_still_life", "terralith", "mr_epicterrain", "larion"};
   private final String b;
   private final String c;
   private final double[] d;
   private final double[] e;
   private final int[] f;
   private final double[] g;
   private final double[] h;

   private BakedBandTable(String var1, String var2, double[] var3, double[] var4, int[] var5, double[] var6, double[] var7) {
      this.b = var1;
      this.c = var2;
      this.d = var3;
      this.e = var4;
      this.f = var5;
      this.g = var6;
      this.h = var7;
   }

   public static BakedBandTable lookup(Predicate<String> var0, UnaryOperator<String> var1) {
      TreeSet var2 = new TreeSet();

      for (String var6 : a) {
         if (var0.test(var6)) {
            var2.add(var6);
         }
      }

      String var12 = var2.isEmpty() ? "vanilla" : String.join("+", var2);
      String var13 = "/streamsreflowing/fastcal/";
      BakedBandTable var14 = null;
      int var15 = -1;

      for (String var10 : new String[]{"", ".v2", ".v3", ".v4", ".v5"}) {
         BakedBandTable.Loaded var11 = a(var13 + var12 + var10 + ".json", var12, var1);
         if (var11 != null && var11.spec >= var15) {
            var14 = var11.table;
            var15 = var11.spec;
         }
      }

      return var14;
   }

   private static BakedBandTable.Loaded a(String var0, String var1, UnaryOperator<String> var2) {
      try (InputStream var3 = BakedBandTable.class.getResourceAsStream(var0)) {
         if (var3 == null) {
            return null;
         }

         JsonObject var4 = JsonParser.parseReader(new InputStreamReader(var3, StandardCharsets.UTF_8)).getAsJsonObject();
         int var5 = 0;
         if (var4.has("requires")) {
            for (Entry var7 : var4.getAsJsonObject("requires").entrySet()) {
               String var8 = (String)var2.apply((String)var7.getKey());
               if (var8 == null || !a(var8, ((JsonElement)var7.getValue()).getAsString())) {
                  return null;
               }

               var5++;
            }
         }

         JsonArray var19 = var4.getAsJsonArray("bands");
         int var20 = var19.size();
         if (var20 == 0) {
            return null;
         }

         double[] var21 = new double[var20];
         double[] var23 = new double[var20];
         int[] var10 = new int[var20];
         double[] var11 = new double[var20];
         double[] var12 = new double[var20];

         for (int var13 = 0; var13 < var20; var13++) {
            JsonObject var14 = var19.get(var13).getAsJsonObject();
            var21[var13] = var14.get("center").getAsDouble();
            var23[var13] = var14.get("bias").getAsDouble();
            var10[var13] = var14.get("n").getAsInt();
            var11[var13] = var14.get("p025").getAsDouble();
            var12[var13] = var14.get("p975").getAsDouble();
         }

         for (int var24 = 1; var24 < var20; var24++) {
            if (var21[var24] <= var21[var24 - 1]) {
               return null;
            }
         }

         String var25 = var4.has("verdict") ? var4.get("verdict").getAsString() : "ON";
         return new BakedBandTable.Loaded(new BakedBandTable(var1, var25, var21, var23, var10, var11, var12), var5);
      } catch (RuntimeException | IOException var17) {
         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.warn("baked fastcal table '{}' unreadable -> per-world calibration", var0, var17);
         }

         return null;
      }
   }

   static boolean a(String var0, String var1) {
      String var2 = ">=";
      String var3 = var1.trim();

      for (String var7 : new String[]{">=", "<=", "==", ">", "<"}) {
         if (var3.startsWith(var7)) {
            var2 = var7;
            var3 = var3.substring(var7.length()).trim();
            break;
         }
      }

      int var8 = b(var0, var3);

      return switch (var2) {
         case ">=" -> var8 >= 0;
         case "<=" -> var8 <= 0;
         case ">" -> var8 > 0;
         case "<" -> var8 < 0;
         default -> var8 == 0;
      };
   }

   private static int b(String var0, String var1) {
      int[] var2 = a(var0);
      int[] var3 = a(var1);

      for (int var4 = 0; var4 < Math.max(var2.length, var3.length); var4++) {
         int var5 = var4 < var2.length ? var2[var4] : 0;
         int var6 = var4 < var3.length ? var3[var4] : 0;
         if (var5 != var6) {
            return Integer.compare(var5, var6);
         }
      }

      return 0;
   }

   private static int[] a(String var0) {
      String var1 = var0.split("\\+")[0].trim();
      ArrayList var2 = new ArrayList();

      for (String var6 : var1.split("[^0-9]+")) {
         if (!var6.isEmpty()) {
            try {
               var2.add(Integer.parseInt(var6));
            } catch (NumberFormatException var8) {
            }
         }
      }

      int[] var9 = new int[var2.size()];

      for (int var10 = 0; var10 < var9.length; var10++) {
         var9[var10] = (Integer)var2.get(var10);
      }

      return var9;
   }

   public String name() {
      return this.b;
   }

   public String verdict() {
      return this.c;
   }

   public double[] centers() {
      return this.d;
   }

   public double[] bias() {
      return this.e;
   }

   public boolean[] safeForWindow(double var1, double var3, int var5) {
      boolean[] var6 = new boolean[this.d.length];

      for (int var7 = 0; var7 < this.d.length; var7++) {
         var6[var7] = this.f[var7] >= var5 && this.h[var7] <= var1 && this.g[var7] >= -var3;
      }

      return var6;
   }

   private record Loaded(BakedBandTable table, int spec) {

      private Loaded {
spec = spec;
}
   }
}
