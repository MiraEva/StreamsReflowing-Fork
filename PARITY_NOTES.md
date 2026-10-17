# Parity notes

## S1 source preparation

- Replaced Forge SRG member identifiers with Mojang official names using the
  joined SRG-to-official mapping generated in `tools/srg2official.tsv`.
- No intentional behavior changes have been made. Build/runtime parity is not yet
  established; CFR 0.152 reported decompilation warnings in its summary, so those
  files must be reviewed as compile errors expose them.

## S2 source and decompilation repairs

- `RiverCarverFeature.java`: restored `place()` method body from isolated Vineflower 1.12.0
  decompilation with `-Xmx8G` (the initial full-jar run failed with `OutOfMemoryError`
  on this method, leaving a bytecode comment dump). Added missing imports `RandomSource`
  and `DoubleBlockHalf`.
- `RainDiag.java:261`: cast reflective null parameter `(Object) null` in `m.invoke(mc, ...)`
  to prevent compiler varargs warning.
- `NetworkTracer.java`, `RiverCarverFeature.java`, `StreamsWorldgen.java`, `LowResGrid.java`, `SettingsCatalog.java`:
  repaired decompiler integer narrowing artifacts where collection loop counters and
  accumulators were typed as `byte`. At runtime, list sizes exceeding 127 caused byte
  overflow to negative values (specifically `IndexOutOfBoundsException: Index -128 out of bounds`
  in `NetworkTracer.build()` at `var191.get(var427)`). Fixed all affected loop counters and
  cell counts to standard `int`.
- `RiverCarverFeature.java:3830`: fixed `byte var12 = 0` counter for structure bounding boxes
  in `a(WorldGenLevel, ChunkPos)`. When > 25 structure pieces intersected a chunk, `var12 += 5`
  overflowed byte range (130 -> -126), causing `ArrayIndexOutOfBoundsException: Index -126 out of bounds for length 160`
  at line 3852 during world generation (at 80% spawn prep). Replaced with `int var12 = 0` and cleaned up
  remaining `byte` loop counters across `RiverCarverFeature`, `ReadTrace`, `BankFallenLogFeature`, and `ChunkHeightCache`.

