package dev.streamsreflowing.offload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class OffloadWire {
   public static final int PROTOCOL = 2;
   public static final byte OFFER = 1;
   public static final byte HELLO = 2;
   public static final byte WORLD = 3;
   public static final byte READY = 4;
   public static final byte BUILD = 5;
   public static final byte RESULT = 6;
   public static final byte CANCEL = 7;
   private static final int FRAME_BYTES = 30000;
   private static final int HEADER_BYTES = 12;
   private static final int MAX_FRAMES = 4096;
   private static final AtomicInteger STREAMS = new AtomicInteger();

   private OffloadWire() {
   }

   public static List<byte[]> frames(byte[] message) {
      int stream = STREAMS.incrementAndGet();
      int total = Math.max(1, (message.length + 30000 - 1) / 30000);
      List<byte[]> out = new ArrayList<>(total);

      for (int i = 0; i < total; i++) {
         int from = i * 30000;
         int len = Math.min(30000, message.length - from);
         ByteBuffer frame = ByteBuffer.allocate(12 + len);
         frame.putInt(stream).putInt(i).putInt(total);
         frame.put(message, from, len);
         out.add(frame.array());
      }

      return out;
   }

   public static byte[] deflate(byte[] raw) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(64, raw.length / 4));

      try (DeflaterOutputStream out = new DeflaterOutputStream(bytes)) {
         out.write(raw);
      }

      return bytes.toByteArray();
   }

   public static byte[] inflate(byte[] packed, int maxBytes) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(packed.length * 3);

      try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(packed))) {
         byte[] buf = new byte[8192];

         int n;
         while ((n = in.read(buf)) > 0) {
            if (bytes.size() + n > maxBytes) {
               throw new IOException("larger than a region can be");
            }

            bytes.write(buf, 0, n);
         }
      }

      return bytes.toByteArray();
   }

   public static DataInputStream reader(byte[] message) {
      return new DataInputStream(new ByteArrayInputStream(message));
   }

   public static final class Assembler {
      private final Map<Integer, byte[][]> partial = new HashMap<>();

      public byte[] accept(byte[] frame) {
         if (frame != null && frame.length >= 12) {
            ByteBuffer in = ByteBuffer.wrap(frame);
            int stream = in.getInt();
            int index = in.getInt();
            int total = in.getInt();
            if (total >= 1 && total <= 4096 && index >= 0 && index < total) {
               byte[] body = new byte[frame.length - 12];
               in.get(body);
               if (total == 1) {
                  return body;
               }

               if (this.partial.size() > 8 && !this.partial.containsKey(stream)) {
                  this.partial.clear();
               }

               byte[][] parts = this.partial.computeIfAbsent(stream, s -> new byte[total][]);
               if (parts.length != total) {
                  this.partial.remove(stream);
                  return null;
               }

               parts[index] = body;
               int size = 0;

               for (byte[] part : parts) {
                  if (part == null) {
                     return null;
                  }

                  size += part.length;
               }

               this.partial.remove(stream);
               byte[] whole = new byte[size];
               int at = 0;

               for (byte[] part : parts) {
                  System.arraycopy(part, 0, whole, at, part.length);
                  at += part.length;
               }

               return whole;
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }
}
