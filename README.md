# StreamsReflowing private fork

Local B2 development fork for a personal Minecraft 1.20.1 / Forge 47.1.3 modpack.

This fork is based on a local decompilation of StreamsReflowing 2.15.0. The original
mod declares `All Rights Reserved`; this repository and its build artifacts are for
private local use and must not be redistributed without the author's permission.

The B2 architecture and execution plans are kept in [`docs/`](docs/). The original
copies remain in the source workspace; the copies in this repository are the working
references for fork development.

The original jar is kept under `oracle/` only as a local reference for parity work.
It is excluded from version control by `.gitignore`.

## Build

Use JDK 17 and run `gradlew.bat build`. The first build may need network access to
resolve ForgeGradle and Minecraft dependencies.

## Current stage

S0 is prepared. S1 remapping is complete and the decoder spike is in progress. The
first compileJava run exposed decompiler artifacts; repairs are recorded in
[docs/S1-REPORT.md](docs/S1-REPORT.md), and a static scan now finds no invalid
decompiler tokens. Awaiting the next compileJava result before selecting the final
decoder and completing S1. TFC integration remains gated on S3 parity.