package dev.streamsreflowing.core.api;

import dev.streamsreflowing.core.RiverEngine;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface FootprintVeto {
   boolean blocks(ServerLevel var1, RiverEngine var2, int var3, int var4, int var5, int var6, int var7, int var8);
}
