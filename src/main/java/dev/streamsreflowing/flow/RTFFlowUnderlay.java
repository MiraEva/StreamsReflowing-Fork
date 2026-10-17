package dev.streamsreflowing.flow;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class RTFFlowUnderlay {
   private static final int MAX = 16384;
   private static final ConcurrentHashMap<Long, byte[]> CLIENT = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Long, byte[]> SERVER = new ConcurrentHashMap<>();

   private RTFFlowUnderlay() {
   }

   private static long key(int cx, int cz) {
      return (long)cx << 32 ^ cz & 4294967295L;
   }

   private static ConcurrentHashMap<Long, byte[]> side(boolean client) {
      return client ? CLIENT : SERVER;
   }

   public static byte[] convert(byte[] raw) {
      if (raw != null && raw.length == 256) {
         byte[] out = null;

         for (int i = 0; i < 256; i++) {
            int b = raw[i] & 255;
            if (b >>> 5 == 0) {
               if (out != null) {
                  out[i] = -1;
               }
            } else {
               if (out == null) {
                  out = new byte[256];
                  Arrays.fill(out, 0, i, (byte)-1);
               }

               out[i] = (byte)Math.round((b & 31) * 255.0 / 32.0);
            }
         }

         return out;
      } else {
         return null;
      }
   }

   public static boolean has(boolean client, int cx, int cz) {
      return side(client).containsKey(key(cx, cz));
   }

   public static void put(boolean client, int cx, int cz, byte[] grid) {
      ConcurrentHashMap<Long, byte[]> map = side(client);
      map.put(key(cx, cz), grid);
      if (map.size() > 16384) {
         Iterator<Long> it = map.keySet().iterator();

         while (map.size() > 12288 && it.hasNext()) {
            it.next();
            it.remove();
         }
      }
   }

   public static void remove(boolean client, int cx, int cz) {
      side(client).remove(key(cx, cz));
   }

   public static void clearClient() {
      CLIENT.clear();
   }

   public static void clearServer() {
      SERVER.clear();
   }

   public static byte angleAt(boolean client, int x, int z) {
      byte[] g = side(client).get(key(x >> 4, z >> 4));
      return g == null ? -1 : g[(x & 15) + ((z & 15) << 4)];
   }

   public static boolean empty() {
      return CLIENT.isEmpty() && SERVER.isEmpty();
   }
}
