package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;

public interface NetworkStore {
   RiverNetwork load(PlateKey var1);

   void save(PlateKey var1, RiverNetwork var2);
}
