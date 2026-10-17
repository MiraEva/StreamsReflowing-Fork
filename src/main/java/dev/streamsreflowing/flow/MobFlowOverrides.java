package dev.streamsreflowing.flow;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MobFlowOverrides {
   private static volatile Set<String> held = Collections.emptySet();
   private static volatile Set<String> swept = Collections.emptySet();
   private static volatile boolean any;

   private MobFlowOverrides() {
   }

   public static void set(String holdList, String sweepList) {
      Set<String> h = parse(holdList);
      Set<String> s = parse(sweepList);
      held = h;
      swept = s;
      any = !h.isEmpty() || !s.isEmpty();
   }

   private static Set<String> parse(String list) {
      if (list != null && !list.isBlank()) {
         Set<String> out = new LinkedHashSet<>();

         for (String raw : list.split(",")) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
               out.add(id.indexOf(58) < 0 ? "minecraft:" + id : id);
            }
         }

         return out.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(out);
      } else {
         return Collections.emptySet();
      }
   }

   public static boolean any() {
      return any;
   }

   public static Boolean verdict(String entityId) {
      if (!any || entityId == null) {
         return null;
      } else if (swept.contains(entityId)) {
         return Boolean.FALSE;
      } else {
         return held.contains(entityId) ? Boolean.TRUE : null;
      }
   }
}
