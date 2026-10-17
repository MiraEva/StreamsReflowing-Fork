package dev.streamsreflowing.core.api;

import dev.streamsreflowing.core.terrain.TerrainSampler;

@FunctionalInterface
public interface DeadEndResolver {
   boolean keep(Sink var1, TerrainSampler var2, long var3);
}
