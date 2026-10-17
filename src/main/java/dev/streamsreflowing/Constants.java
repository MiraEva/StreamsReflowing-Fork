package dev.streamsreflowing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants {
   public static final String MOD_ID = "streamsreflowing";
   public static final String MOD_NAME = "StreamsReflowing";
   public static final Logger LOG = LoggerFactory.getLogger("StreamsReflowing");

   private Constants() {
   }

   static {
      for (String key : System.getProperties().stringPropertyNames()) {
         if (key.startsWith("streamsreflowing.") && "".equals(System.getProperty(key))) {
            System.setProperty(key, "true");
         }
      }
   }
}
