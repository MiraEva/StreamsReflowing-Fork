package dev.streamsreflowing.worldgen.bank;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.platform.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

public final class BankFeatureLoader {
   private static volatile List<BankFeature> a;
   private static volatile Map<String, BankFeature> b = Map.of();
   private static boolean c;
   private static final Set<String> d = Set.of(
      "streamsreflowing:sharp_rocks",
      "streamsreflowing:hill_rocks",
      "streamsreflowing:mossy_rocks",
      "streamsreflowing:driftwood",
      "streamsreflowing:dry_bones",
      "streamsreflowing:bank_rock_1",
      "streamsreflowing:bank_rock_2",
      "streamsreflowing:bank_rock_3",
      "streamsreflowing:bank_rock_4",
      "streamsreflowing:bank_rock_5",
      "streamsreflowing:bank_rock_6",
      "streamsreflowing:bank_rock_7"
   );
   private static final Map<String, String> e = Map.ofEntries(
      Map.entry(
         "fallen_logs.json",
         "{\n  \"features\": [\n    \"streamsreflowing:fallen_log_a\",\n    \"streamsreflowing:fallen_log_b\",\n    \"streamsreflowing:fallen_log_stub_a\",\n    \"streamsreflowing:fallen_log_stub_b\",\n    \"streamsreflowing:fallen_tree_small\"\n  ],\n  \"tags\": [\n    \"minecraft:is_forest\"\n  ],\n  \"chance\": 2\n}\n"
      ),
      Map.entry(
         "taiga_fallen_logs.json",
         "{\n  \"features\": [\n    \"streamsreflowing:fallen_log_a\",\n    \"streamsreflowing:fallen_log_b\",\n    \"streamsreflowing:fallen_log_stub_a\",\n    \"streamsreflowing:fallen_log_stub_b\"\n  ],\n  \"tags\": [\n    \"minecraft:is_taiga\"\n  ],\n  \"chance\": 2\n}\n"
      ),
      Map.entry(
         "fallen_tree_jungle.json",
         "{\n  \"features\": [\n    \"streamsreflowing:fallen_tree_jungle_a\",\n    \"streamsreflowing:fallen_tree_jungle_b\",\n    \"streamsreflowing:fallen_tree_acacia\"\n  ],\n  \"tags\": [\n    \"minecraft:is_jungle\"\n  ],\n  \"chance\": 2\n}\n"
      ),
      Map.entry(
         "fallen_tree_savanna.json",
         "{\n  \"features\": [\n    \"streamsreflowing:fallen_tree_acacia\"\n  ],\n  \"biomes\": [\n    \"minecraft:savanna\",\n    \"minecraft:savanna_plateau\",\n    \"minecraft:windswept_savanna\"\n  ],\n  \"chance\": 2\n}\n"
      ),
      Map.entry(
         "pale_driftwood_beach.json",
         "{\n  \"features\": [\n    \"streamsreflowing:fallen_log_a_pale\",\n    \"streamsreflowing:fallen_log_b_pale\",\n    \"streamsreflowing:fallen_log_stub_a_pale\",\n    \"streamsreflowing:fallen_log_stub_b_pale\"\n  ],\n  \"tags\": [\n    \"minecraft:is_beach\"\n  ],\n  \"chance\": 3\n}\n"
      ),
      Map.entry(
         "pale_driftwood_dry.json",
         "{\n  \"features\": [\n    \"streamsreflowing:fallen_log_a_pale\",\n    \"streamsreflowing:fallen_log_b_pale\",\n    \"streamsreflowing:fallen_log_stub_a_pale\",\n    \"streamsreflowing:fallen_log_stub_b_pale\"\n  ],\n  \"biomes\": [\n    \"minecraft:desert\",\n    \"minecraft:badlands\",\n    \"minecraft:eroded_badlands\",\n    \"minecraft:wooded_badlands\",\n    \"minecraft:savanna\",\n    \"minecraft:savanna_plateau\",\n    \"minecraft:windswept_savanna\"\n  ],\n  \"chance\": 2\n}\n"
      )
   );
   private static final String f = "{\n  \"features\": [\"minecraft:patch_pumpkin\"],\n  \"tags\": [\"minecraft:is_overworld\"],\n  \"exclude_tags\": [\"minecraft:is_frozen_ocean\"],\n  \"exclude_biomes\": [\"minecraft:snowy_plains\", \"minecraft:snowy_taiga\", \"minecraft:ice_spikes\"],\n  \"chance\": 8\n}\n";
   private static final String g = "{\n  \"_about\": \"A definition: copy it into bank_features/ and a bank style ring can place it as riverside_logs.\",\n  \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"]\n}\n";
   private static final String h = "{\n  \"_about\": \"A definition: copy it into bank_features/ and a bank style ring can place it as riverside_stumps.\",\n  \"features\": [\"streamsreflowing:fallen_log_stub_a\", \"streamsreflowing:fallen_log_stub_b\"]\n}\n";
   private static final String i = "{\n  \"_about\": \"A definition for a WATERLINE ring: lily pads on the open surface. Copy it into bank_features/.\",\n  \"features\": [\"minecraft:patch_waterlily\"]\n}\n";
   private static final String j = "{\n  \"_about\": \"A definition for a BED ring: grass on the submerged floor. Copy it into bank_features/.\",\n  \"features\": [\"minecraft:seagrass_river\", \"minecraft:seagrass_simple\"]\n}\n";
   private static final String k = "{\n  \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"]\n}\n";
   private static final String l = "# Streams Reflowing -- custom bank features\n\nThis pass plants features on the land around water. A file here is one of two things:\n\n- A **definition** -- just `features` (and maybe `chance`), no biomes, no tags. It places nothing by itself.\n  A bank style puts it on a ring: `\"features\": [{ \"id\": \"<this file's name without .json>\", \"chance\": 2 }]`\n  inside the style's `bank`, `bed` or any other ring. See `examples/riverside_logs.json` and\n  `examples/riverside_stumps.json` for dry rings, and `examples/riverside_lilies.json` (lily pads at a\n  `waterline`) and `examples/riverside_seagrass.json` (a `bed`) for the wet ones, plus the bank-style\n  GUIDE. This is the way to put scenery exactly where you want it. A ring names the DEFINITION, never a\n  placed feature: `\"id\": \"streamsreflowing:fallen_log_a\"` in a ring places nothing.\n- A **self-placing entry** -- `features` plus `biomes` and/or `tags`. It runs on its own wherever those\n  match, as it always has. Every matching entry runs (there is no override contest like bank styles have).\n\n- **How often one appears**: `chance` in its file (or in the style's ring, for a definition).\n- **Where**: the ring of the style that names it, or `biomes` / `tags` / the excludes.\n- **What**: any placed feature by id. The mod's own bank pieces are available to reference --\n  `streamsreflowing:fallen_log_a` / `_b`, `fallen_log_stub_a` / `_b` (and their `_pale` driftwood\n  variants), `fallen_tree_small`, `fallen_tree_acacia`, `fallen_tree_jungle_a` / `_b` -- and a structure\n  you save with a structure block can be placed the same way (see the guide).\n\n> **Full guide** (every field, more examples, datapacks, and bank styles too): see `GUIDE.md` in this\n> folder. This file is the quick reference.\n\n## How to add one\n1. Look in `examples/example.json` for the shape. 2. Copy it UP into this folder (or write your own) and\nedit it. 3. Relaunch. (Files in `examples/` are reference-only and are NOT loaded.)\n\n## Where features place\nOn the bank around water, bounded by your **bank scenery** setting (`bankFeatures`): 0 = nowhere,\n1 = this mod's streams, 2 = also its lakes, 3 = every water edge. Each listed feature is run AT the\nchosen column, on sturdy ground, exactly as written -- trees and big features included (you chose them).\n\nA definition on a style's ring follows THAT ring: `bank`, `top_bank` and extra rings plant on the dry\nland beside the water; `bed` plants on the submerged floor (water above it -- sea grass, kelp, coral);\n`waterline` plants at the open surface (water below, air above -- lily pads). Wet placements are skipped\nin biomes tagged `#streamsreflowing:no_underwater_flora`, the same opt-out `underwaterFlora` honours.\n\n## Fields\n- `features` : a POOL of PLACED-feature ids -- ONE is chosen at random per placement. Required.\n- `biomes` : exact biome ids to include.   `tags` : biome tags the biome must ALL have to include.\n  (If both are empty, the file is a DEFINITION and places nothing until a bank style names it.)\n- `exclude_biomes` / `exclude_tags` : biomes/tags to drop AFTER the include -- so \"all overworld but not\n  frozen\" is `tags: [\"minecraft:is_overworld\"]` + `exclude_tags: [\"minecraft:is_frozen_ocean\", ...]`\n  (plus any snowy biomes/tags you want out).\n- `chance` : 0-100, the percent chance per sampled SITE (a few are drawn per chunk from each band,\n  so density is the same along a brook and a wide river). Default 100.\n\n## Shipping entries with a mod / datapack\nPut `<name>.json` files at `data/<namespace>/streamsreflowing/bank_feature/` in any datapack or mod jar --\nthey load automatically and are merged with this folder.\n";

   private BankFeatureLoader() {
   }

   public static void seed() {
      a(b());
   }

   public static List<BankFeature> configEntries() {
      List var0 = a;
      if (var0 != null) {
         return var0;
      }

      synchronized (BankFeatureLoader.class) {
         if (a != null) {
            return a;
         }

         a = c();
         return a;
      }
   }

   public static Map<String, BankFeature> configEntriesById() {
      configEntries();
      return b;
   }

   private static Path b() {
      return Services.PLATFORM.configDir().resolve("streamsreflowing").resolve("bank_features");
   }

   private static List<BankFeature> c() {
      Path var0 = b();
      a(var0);
       ArrayList<BankFeature> var1 = new ArrayList<>();
       LinkedHashMap<String, BankFeature> var2 = new LinkedHashMap<>();

       try (Stream<Path> var3 = Files.list(var0)) {
         var3.filter(var0x -> var0x.getFileName().toString().endsWith(".json"))
            .sorted()
            .forEach(
               var2x -> {
                  try {
                     JsonElement var3x = JsonParser.parseString(Files.readString(var2x));
                      DataResult<BankFeature> var4 = BankFeature.CODEC.parse(JsonOps.INSTANCE, var3x);
                     if (var4.result().isPresent()) {
                        BankFeature var5 = (BankFeature)var4.result().get();
                        if (a(var5)) {
                           Files.delete(var2x);
                           Constants.LOG
                              .info(
                                 "Removed bank feature file {}: every feature it names ({}) was withdrawn from the mod, so it could never place anything.",
                                 var2x.getFileName(),
                                 var5.features()
                              );
                           return;
                        }

                        String var6 = var2x.getFileName().toString();
                        var6 = var6.substring(0, var6.length() - ".json".length());
                        var1.add(var5);
                        var2.put(var6, var5);
                        if (!var5.standalone()) {
                           Constants.LOG
                              .info(
                                 "Bank feature {} names no biomes or tags: it is a definition, placed only where a bank style's ring lists \"id\": \"{}\".",
                                 var2x.getFileName(),
                                 var6
                              );
                        }
                     } else {
                        Constants.LOG
                           .warn(
                              "Custom bank feature {} failed to parse: {}",
                              var2x.getFileName(),
                              var4.error().map(var0xx -> var0xx.message()).orElse("unknown error")
                           );
                     }
                  } catch (Exception var7) {
                     Constants.LOG.warn("Failed to read custom bank feature {}", var2x, var7);
                  }
               }
            );
      } catch (IOException var8) {
         Constants.LOG.warn("Failed to list custom bank features in {}", var0, var8);
      }

      if (!var1.isEmpty()) {
         Constants.LOG.info("Loaded {} custom bank feature(s) from {}", var1.size(), var0);
      }

      b = Map.copyOf(var2);
      return var1;
   }

   private static boolean a(BankFeature var0) {
      if (var0.features().isEmpty()) {
         return false;
      }

      for (ResourceLocation var2 : var0.features()) {
         if (!d.contains(var2.toString())) {
            return false;
         }
      }

      return true;
   }

   private static synchronized void a(Path var0) {
      if (!c) {
         c = true;

         try {
            Files.createDirectories(var0);
            Path var1 = var0.resolve(".guide_version");
            boolean var2 = !Files.exists(var1) || !BankStyleLoader.guideVersion().equals(Files.readString(var1).trim());
            a(
               var0.resolve("README.md"),
               "# Streams Reflowing -- custom bank features\n\nThis pass plants features on the land around water. A file here is one of two things:\n\n- A **definition** -- just `features` (and maybe `chance`), no biomes, no tags. It places nothing by itself.\n  A bank style puts it on a ring: `\"features\": [{ \"id\": \"<this file's name without .json>\", \"chance\": 2 }]`\n  inside the style's `bank`, `bed` or any other ring. See `examples/riverside_logs.json` and\n  `examples/riverside_stumps.json` for dry rings, and `examples/riverside_lilies.json` (lily pads at a\n  `waterline`) and `examples/riverside_seagrass.json` (a `bed`) for the wet ones, plus the bank-style\n  GUIDE. This is the way to put scenery exactly where you want it. A ring names the DEFINITION, never a\n  placed feature: `\"id\": \"streamsreflowing:fallen_log_a\"` in a ring places nothing.\n- A **self-placing entry** -- `features` plus `biomes` and/or `tags`. It runs on its own wherever those\n  match, as it always has. Every matching entry runs (there is no override contest like bank styles have).\n\n- **How often one appears**: `chance` in its file (or in the style's ring, for a definition).\n- **Where**: the ring of the style that names it, or `biomes` / `tags` / the excludes.\n- **What**: any placed feature by id. The mod's own bank pieces are available to reference --\n  `streamsreflowing:fallen_log_a` / `_b`, `fallen_log_stub_a` / `_b` (and their `_pale` driftwood\n  variants), `fallen_tree_small`, `fallen_tree_acacia`, `fallen_tree_jungle_a` / `_b` -- and a structure\n  you save with a structure block can be placed the same way (see the guide).\n\n> **Full guide** (every field, more examples, datapacks, and bank styles too): see `GUIDE.md` in this\n> folder. This file is the quick reference.\n\n## How to add one\n1. Look in `examples/example.json` for the shape. 2. Copy it UP into this folder (or write your own) and\nedit it. 3. Relaunch. (Files in `examples/` are reference-only and are NOT loaded.)\n\n## Where features place\nOn the bank around water, bounded by your **bank scenery** setting (`bankFeatures`): 0 = nowhere,\n1 = this mod's streams, 2 = also its lakes, 3 = every water edge. Each listed feature is run AT the\nchosen column, on sturdy ground, exactly as written -- trees and big features included (you chose them).\n\nA definition on a style's ring follows THAT ring: `bank`, `top_bank` and extra rings plant on the dry\nland beside the water; `bed` plants on the submerged floor (water above it -- sea grass, kelp, coral);\n`waterline` plants at the open surface (water below, air above -- lily pads). Wet placements are skipped\nin biomes tagged `#streamsreflowing:no_underwater_flora`, the same opt-out `underwaterFlora` honours.\n\n## Fields\n- `features` : a POOL of PLACED-feature ids -- ONE is chosen at random per placement. Required.\n- `biomes` : exact biome ids to include.   `tags` : biome tags the biome must ALL have to include.\n  (If both are empty, the file is a DEFINITION and places nothing until a bank style names it.)\n- `exclude_biomes` / `exclude_tags` : biomes/tags to drop AFTER the include -- so \"all overworld but not\n  frozen\" is `tags: [\"minecraft:is_overworld\"]` + `exclude_tags: [\"minecraft:is_frozen_ocean\", ...]`\n  (plus any snowy biomes/tags you want out).\n- `chance` : 0-100, the percent chance per sampled SITE (a few are drawn per chunk from each band,\n  so density is the same along a brook and a wide river). Default 100.\n\n## Shipping entries with a mod / datapack\nPut `<name>.json` files at `data/<namespace>/streamsreflowing/bank_feature/` in any datapack or mod jar --\nthey load automatically and are merged with this folder.\n",
               var2
            );
            a(
               var0.resolve("GUIDE.md"),
               "# Streams Reflowing — Bank Styles and Bank Features Guide\n\nThis guide tells you how to change the land around your streams. It is written in\nsimplified technical English. The sentences are short. Each instruction is one step.\n\nStreams Reflowing has two systems for the banks:\n\n- **Bank style** — the blocks of a channel, in rings out from the water: the bed, the\n  waterline, the bank, and more rings if you want them.\n- **Bank feature** — scenery on a bank. A style can put a feature on one of its rings\n  (fallen logs on the bank, for example), or a feature can place itself by biome.\n\nYou edit both systems with JSON files. The JSON is the same for the config folder and for a\ndatapack.\n\n---\n\n## 1. Before you start\n\nObey these three rules for both systems.\n\n1. The mod reads the files one time, at game start.\n2. A change applies to new chunks only. The mod does not change land that exists already.\n3. After you edit a file, load the change again:\n   - Config folder: stop the game. Then start the game again.\n   - Datapack: run the `/reload` command, or make a new world.\n\n---\n\n## 2. Two ways to add a file\n\nYou can put a bank style or a bank feature in two places. Use the place that is correct for\nyour task.\n\n| Place | Folder | Use it for |\n|---|---|---|\n| Config folder | `config/streamsreflowing/` on one instance | A quick change on your own game. It wins over a datapack. |\n| Datapack | `data/<namespace>/streamsreflowing/` in a datapack or a mod | A change that you send with a modpack. |\n\n`<namespace>` is a name that you choose. Use only lowercase letters, numbers, and the\n`_` sign.\n\n---\n\n## 3. Bank styles\n\nA bank style paints the ground around the water in **rings**, from the water outward.\n\n```\n   top_bank     (a second bank ring, if you want two)\n      bank      (the cut slope beside the water)\n  ~~~~~~~~~~~~  (the waterline, a strip at the water surface)\n      bed       (the floor below the water)\n```\n\n### 3.1 One unit: percent of the stream's width\n\nEvery size in a style is a **percent of the stream's width** at that place, bank to bank.\n`100` means \"as wide as the stream is right here\". A wide river gets wide rings. A brook\ngets narrow rings. The same file does both.\n\nTwo rules:\n\n- A ring whose size is not zero is never thinner than **one block**. A brook keeps its waterline.\n- A pattern is never finer than **two blocks**. Blobs on a brook are small specks.\n\nA lake or sea shore counts as a stream **32 blocks wide**. It is painted under the water only\n(the bed and the waterline).\n\n### 3.2 Where the file goes\n\n- **Config folder:** `config/streamsreflowing/bank_styles/`\n  At the first game start, the mod makes this folder. It puts a `README.md` file and an\n  `examples/` folder in it. The `examples/` folder has one small, complete style for each\n  technique in this guide (see 3.11). The top folder is empty at the start.\n\n  To change a style, do these steps:\n  1. Copy a file from the `examples/` folder to the top folder, or write your own.\n  2. Edit the copy in the top folder.\n\n  The mod loads the top-level files only. It does not load the `examples/` folder.\n\n  You do not need the mod's own files to change a biome: a style you write wins over the\n  shipped one for the biomes it names. 3.2 lists what each biome uses now.\n\n- **Datapack:** `data/<namespace>/streamsreflowing/bank_style/<name>.json`\n  To replace a default style, use the same file name as the default.\n\n### 3.3 The rings\n\nEach ring is a block, a list of blocks, or an object (see 3.5). The rings sit side by side,\nfrom the water outward. A later ring does not paint over an earlier one.\n\nThis is one bank, cut through. The water is on the left.\n\n```\n                       water surface\n                            |   above (up the bank)\n    ~~~~~~~~~~~~~~~~~~~~~~~~+~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n                            |   below (under the water)\n         bed                |  bank      | top_bank | rings...\n    (the floor under water) |<-------- the cut -------->|\n```\n\n**Sizes are measured in two different ways.** Both are a percent of the stream's width.\n\n| Ring | What it covers | How its size is measured | Default |\n|---|---|---|---|\n| `bed` | the floor under the water | no size: everything below the waterline strip | — |\n| `waterline` | a band hugging the water surface | UP and DOWN from the surface: `below` under it, `above` onto the bank | `below` 10, `above` 0 |\n| `bank` | the cut slope beside the water | OUTWARD from the water's edge | `reach` 100, or 50 with a `top_bank` |\n| `top_bank` | a second band, further out | OUTWARD from where `bank` stopped | `reach` the rest of the cut |\n| `rings: [ ... ]` | any number of further bands | OUTWARD from where the last one stopped | `reach` required |\n\n**The reaches add up.** A `reach` is the width of that band, not its distance from the water.\nA `bank` of 50 with a `top_bank` of 30 means: bank from 0 to 50, top_bank from 50 to 80.\n\n**The cut is the budget.** A stream cuts the ground on each side as wide as the stream itself,\nnever less than 5 blocks. `bank`, `top_bank` and `rings` share that cut.\n\n- You do not have to reach 100. Stop early and the rest of the cut keeps what the carve left.\n- Go past 100 and the extra paints nothing: there is no cut ground out there.\n- Write no sizes at all and the defaults fill the cut for you.\n\n`bed`, `waterline` and `bank` are always there. If you leave one out, it takes gravel,\ncoarse dirt, or the column's own material. A `top_bank` or extra ring you leave out is simply\nnot there.\n\nTwo blocks have a special meaning in any ring:\n\n- `streamsreflowing:chameleon` — the column's own material: dirt under grass, sandstone under\n  sand, terracotta in badlands.\n- `minecraft:air` — leave that share of the ring as it was.\n\n`bed`'s and `waterline`'s `features` (3.7) plant IN the water, not beside it: `bed` on the\nsubmerged floor, `waterline` at the open surface. `bank`, `top_bank` and any extra ring still\nplant on dry land, exactly as before.\n\n### 3.4 The style fields\n\n| Field | Type | Default | Function |\n|---|---|---|---|\n| `biomes` | list of ids | none | Use this style in these exact biomes. |\n| `tags` | list of tags | none | Use this style in a biome that has ALL of these tags. |\n| `exclude_biomes` | list of ids | none | Never use this style in these exact biomes. |\n| `exclude_tags` | list of tags | none | Never use this style in a biome that has ANY of these tags. |\n| `star` | true or false | false | This style beats every unstarred style at its level (see 3.8). |\n| `enabled` | true or false | true | Set `false` to leave the carved land raw here. |\n| `cohesion` | 0.0 to 1.0 | inferred | How firmly the banks hold. 0 = loose sand or gravel: streams there spread wide and shallow and can form bars. 1 = clay or roots: narrow, deep, no bars. Leave it out and the mod infers it from the biome tags. |\n| `bed`, `waterline`, `bank`, `top_bank` | block, list, or ring object | see 3.3 | The rings. |\n| `rings` | list of ring objects | none | More rings outward. Each needs `reach`. |\n| `_about` (or any field that starts with `_`) | anything | none | A note for people. The mod ignores it. |\n\nWrite tags without `#`. Write `\"minecraft:is_forest\"`, not `\"#minecraft:is_forest\"`. A tag\nwith `#` stops the file from loading.\n\n### 3.5 A ring as an object\n\nA bare block or list is the short form. The object form adds more:\n\n```json\n\"bank\": {\n  \"reach\": 50,\n  \"block\": [\"minecraft:cobblestone\", \"streamsreflowing:chameleon\"],\n  \"pattern\": \"blobs\",\n  \"scale\": 30,\n  \"edge_noise\": 0.3,\n  \"overrides\": [ ... ],\n  \"features\": [ ... ]\n}\n```\n\n| Field | Default | Function |\n|---|---|---|\n| `block` | required | One block, or a list of blocks mixed by `pattern`. |\n| `reach`, `above`, `below` | see 3.3 | The ring's size, in percent of the stream's width. |\n| `pattern` | `blobs` | How a list is mixed: `blobs` (patches), `bands` (stripes along the stream), `speckle` (a random pick per column). |\n| `scale` | `30` | The patch size or the stripe width, in percent of the stream's width. |\n| `edge_noise` | `0.3` | How softly the ring's outer edge blends into the next ring. 0 = a sharp line. 1 = very soft. |\n| `overrides` | none | Swap the ring's blocks where the stream is a certain way (3.6). |\n| `features` | none | Scenery placed in this ring (3.7). |\n\nA list is divided into equal shares, one share for each block. Repeat a block for more\nshares. `[\"minecraft:mud\", \"minecraft:air\", \"minecraft:air\"]` paints one third with mud and\nleaves two thirds natural.\n\n### 3.6 Overrides: where the stream is a certain way\n\nAn override is a sticker on the ring. `when` names stream properties with a comparison.\n`fade` says how softly the sticker comes in. The mod tries the overrides top to bottom. The\nfirst one whose roll succeeds paints the column. If none does, the ring's own block paints it.\n\n```json\n\"overrides\": [\n  { \"when\": { \"slope\": \"> 0.5\" }, \"fade\": 0.1, \"block\": \"minecraft:stone\" },\n  { \"when\": { \"bend\": \"> 0.3\", \"side\": \"inner\" }, \"block\": \"minecraft:gravel\" },\n  { \"when\": { \"to_mouth\": \"< 0.2\", \"width\": \"> 0.4\" }, \"block\": [\"minecraft:sand\", \"minecraft:gravel\"], \"scale\": 60 }\n]\n```\n\n| Field | Default | Function |\n|---|---|---|\n| `when` | required | Properties and comparisons. All must hold. Comparisons: `\"> 0.3\"`, `\">= 0.3\"`, `\"< 0.3\"`, `\"<= 0.3\"`. |\n| `fade` | `0.1` | At the threshold the override claims half the columns. `fade` past it, all of them. |\n| `block` | required | The sticker's block or list. |\n| `pattern`, `scale` | the ring's | As for a ring. |\n\nEvery property runs from 0 to 1:\n\n| Property | 0 | 1 | Anchors |\n|---|---|---|---|\n| `slope` | flat | a sheer drop | 0.05 gentle, 0.2 brisk, 0.5 steep |\n| `width` | a 1-block brook | a 32-block river | 0.25 = 8 blocks |\n| `speed` | still | the fastest the mod models | |\n| `bend` | straight | a hairpin | 0.2 = a clear curve |\n| `side` | | | `\"inner\"` (the inside of the bend, where deposits form) or `\"outer\"` (the cut bank) |\n| `to_mouth` | at the outlet | 16 stream widths upstream or further | 0.2 = the last few widths before the sea or lake |\n| `to_junction` | at a confluence | 16 widths from any | |\n| `flat_run` | just steepened | a long flat, settled reach | what braids and bars follow |\n\n### 3.7 Features on a ring\n\n```json\n\"features\": [\n  { \"id\": \"riverside_logs\", \"chance\": 2 },\n  { \"id\": \"mypack:mossy_boulder\", \"chance\": 1 }\n]\n```\n\n`id` names a bank feature **definition**: a file in `config/streamsreflowing/bank_features/`\nby its name without `.json`, or a datapack entry as `namespace:name`. It does not name a placed\nfeature directly. `\"id\": \"streamsreflowing:fallen_log_a\"` places nothing, because no definition\nhas that name. Make a definition that lists the placed features, then name the definition.\n\n`chance` is the percent of the sampled sites in this ring that get one. Keep it low: 1 to 5.\n\nWhere a feature lands depends on its ring. `bank`, `top_bank` and any extra ring plant on dry\nland beside the water, as always. `bed` plants ON the submerged floor, water directly above —\nsea grass, kelp, coral and the like. `waterline` plants AT the open water's surface, water\ndirectly below and air above — a lily pad is the obvious case. One limit: a `bed` or `waterline`\nfeature never appears in a biome tagged `#streamsreflowing:no_underwater_flora` (frozen and dry\nbiomes by default), the same opt-out the `underwaterFlora` setting honours. A feature that\ncannot survive where its ring puts it is skipped quietly, never a crash.\n\nThe `bank_features/examples/` folder has four definitions to start from: `riverside_logs.json`\n(fallen logs) and `riverside_stumps.json` (log stumps) for the dry rings, `riverside_lilies.json`\n(lily pads) for a `waterline` ring and `riverside_seagrass.json` for a `bed` ring. Copy them into\n`bank_features/`. Then `{ \"id\": \"riverside_logs\", \"chance\": 3 }` works in any ring.\nSee section 4 for the feature files.\n\n### 3.8 Which style the mod uses\n\nFor each column of a stream, the mod uses one style only.\n\n1. An exact biome match (`biomes`) wins over all tag matches. Both win over a style with no\n   `biomes` and no `tags`.\n2. At each of those levels, a **starred** style (`\"star\": true`) wins over every unstarred\n   style, whatever their tags.\n3. Then a config-folder style wins over a datapack style. Among tag styles, the style with\n   the most matching tags wins. A tag style matches only if the biome has EVERY tag in the list.\n4. `exclude_biomes` and `exclude_tags` are a block list. The mod does not use a style for a\n   biome on its exclude list. The mod then uses the next style.\n\nTo write \"all overworld biomes but not frozen biomes\", do this:\n\n```json\n{ \"tags\": [\"minecraft:is_overworld\"], \"exclude_tags\": [\"minecraft:is_frozen\"] }\n```\n\n### 3.9 Bank style examples\n\n**The smallest useful style:**\n```json\n{ \"biomes\": [\"minecraft:plains\"], \"bed\": \"minecraft:mud\", \"waterline\": \"minecraft:mud\", \"bank\": \"streamsreflowing:chameleon\" }\n```\n\n**Stone mountain streams, but not when frozen. Skin the bed and the waterline. Keep the bank\nnatural:**\n```json\n{\n  \"tags\": [\"minecraft:is_mountain\"],\n  \"exclude_tags\": [\"minecraft:is_frozen\"],\n  \"bed\": \"minecraft:stone\",\n  \"waterline\": \"minecraft:gravel\",\n  \"bank\": \"minecraft:air\"\n}\n```\n\n**Leave the swamp streams raw:**\n```json\n{ \"biomes\": [\"minecraft:swamp\"], \"enabled\": false }\n```\n\n**Everything at once:**\n```json\n{\n  \"tags\": [\"minecraft:is_mountain\"],\n  \"exclude_biomes\": [\"minecraft:stony_peaks\"],\n  \"star\": true,\n  \"cohesion\": 0.8,\n  \"bed\": [\"minecraft:gravel\", \"minecraft:stone\"],\n  \"waterline\": { \"above\": 5, \"below\": 15, \"block\": \"minecraft:mossy_cobblestone\", \"edge_noise\": 0.35 },\n  \"bank\": {\n    \"reach\": 50,\n    \"block\": [\"minecraft:cobblestone\", \"streamsreflowing:chameleon\"],\n    \"pattern\": \"blobs\", \"scale\": 30, \"edge_noise\": 0.3,\n    \"overrides\": [\n      { \"when\": { \"slope\": \"> 0.5\" }, \"fade\": 0.1, \"block\": \"minecraft:stone\" },\n      { \"when\": { \"bend\": \"> 0.3\", \"side\": \"inner\" }, \"block\": \"minecraft:gravel\" }\n    ],\n    \"features\": [ { \"id\": \"riverside_logs\", \"chance\": 2 } ]\n  },\n  \"top_bank\": { \"reach\": 30, \"block\": [\"streamsreflowing:chameleon\", \"minecraft:air\"], \"edge_noise\": 0.5 },\n  \"rings\": [\n    { \"reach\": 20, \"block\": [\"minecraft:coarse_dirt\", \"minecraft:air\", \"minecraft:air\"], \"pattern\": \"speckle\" }\n  ]\n}\n```\n\n### 3.10 Old files\n\nA style written for version 2.13 or earlier works unchanged. Its fields mean the same rings:\n\n| Old field | Now means |\n|---|---|\n| `bed` / `waterline` / `bank` as a block or list | the same ring, `speckle` pattern, with every old default |\n| `waterline_below` / `waterline_above` (blocks) | the waterline's `below` / `above`, still counted in blocks |\n| `underwater_noise` / `above_water_noise` | `edge_noise` of the bed ring and of the waterline ring |\n| `bed_enabled` / `waterline_enabled` / `bank_enabled: false` | that ring painted as `minecraft:air` |\n| `point_bar` | an override on the bed for the inside of bends |\n\nIf a file gives a ring as an object AND the old fields for it, the object wins.\n\n### 3.11 Worked examples: one file per technique\n\nThe folder `config/streamsreflowing/bank_styles/examples/` has one small, complete style for\neach technique. Each file explains itself in its `_about` field. To try one, copy it to\n`config/streamsreflowing/bank_styles/`. Then start the game again and make a new world.\n\n| File | Technique | What to look for |\n|---|---|---|\n| `01_smallest.json` | The smallest complete style | Mud streams in mangrove swamps. |\n| `02_rings_and_sizes.json` | Every ring and its size | Four bands of blocks out from the water in meadows. |\n| `03_patterns_and_mixes.json` | `blobs`, `bands`, `speckle`, `scale`, `edge_noise`, weighted lists, `air` | Striped terracotta banks in the badlands. |\n| `04_overrides.json` | All eight stream properties, the comparisons, `fade`, order | Sand inside bends, stone on steep reaches, mud on long flats in old-growth taiga. |\n| `05_scenery_on_rings.json` | Features on rings | Fallen logs on dark forest banks. Needs the two definitions (3.7). |\n| `06_matching_and_priority.json` | `biomes` and `tags` together, excludes, `star`, `cohesion` | Jungle streams, vanilla and modded, but not bamboo jungle. |\n| `07_leave_raw.json` | `enabled: false` | Unpainted streams in mushroom fields. |\n| `08_everything.json` | Every field there is: targeting, all five ring kinds, all three patterns, all eight stream properties, scenery under the water, at the surface and on land | Windswept hills and mountain streams: striped banks, gravel beds with sand inside the bends, lily pads and seagrass, logs and stumps. Needs the four definitions (3.7). |\n| `09_old_2_13_format.json` | The old format | A 2.13 river style that still loads. |\n\nSome things to know:\n\n- An example for a biome that already has a style of its own replaces that style. A copied\n  example is a config file, and a config file wins over the mod's own style at the same level.\n- A tag example paints only the biomes that no style names by name. The mod's own styles name\n  many vanilla biomes by name. To take one over, add it to `biomes`.\n- The examples use vanilla blocks and biomes, so they load on every Minecraft version the mod\n  supports.\n\n---\n\n## 4. Bank features\n\nA bank feature file is one of two things.\n\n- A **definition**: only `features` (and maybe `chance`). It places nothing by itself. A bank\n  style puts it on a ring with `\"features\": [{ \"id\": \"<file name without .json>\", \"chance\": 2 }]`.\n  This is the way to put scenery exactly where you want it.\n- A **self-placing entry**: `features` plus `biomes` and/or `tags`. It runs on its own wherever\n  those match. Every matching entry runs. There is no selection contest.\n\n### 4.1 Where the file goes\n\n- **Config folder:** `config/streamsreflowing/bank_features/`\n  At the first game start, the mod puts a `README.md` file and an `examples/` folder here.\n  Copy an example to the top folder. Then edit it. The mod loads the top-level files only.\n\n- **Datapack:** `data/<namespace>/streamsreflowing/bank_feature/<name>.json`\n  A style names a datapack entry as `namespace:name`.\n\n### 4.2 The fields\n\n| Field | Type | Default | Function |\n|---|---|---|---|\n| `features` | list of ids | none (required) | The placed features. ONE of them is chosen per placement. |\n| `biomes` | list of ids | none | Run this entry on its own in these exact biomes. |\n| `tags` | list of tags | none | Run this entry on its own in a biome that has ALL of these tags. |\n| `exclude_biomes` | list of ids | none | Never run this entry in these exact biomes. |\n| `exclude_tags` | list of tags | none | Never run this entry in a biome that has ANY of these tags. |\n| `chance` | 0 to 100 | 100 | The chance, in percent, per sampled bank site. A style's ring can give its own `chance` instead. |\n\nIf you give no `biomes` and no `tags`, the file is a definition. It places nothing until a\nstyle names it.\n\n### 4.3 Which sites get a feature\n\nThe mod samples a few sites on each bank per chunk. At a site:\n\n1. The site's style is found, and the ring the site is in. Each feature on that ring rolls\n   its `chance`. The first hit places, and the site is done.\n2. Otherwise each self-placing entry that includes the biome (and does not exclude it) rolls\n   its `chance`. The first hit places.\n\n**Warning:** The mod puts each feature on the bank as it is. The mod checks for solid ground\nbelow only, so nothing floats. A tree feature makes trees at the water. Use small features\nfor the bank, unless you want trees at the water.\n\n### 4.4 Bank feature examples\n\n**A definition, placed by a style (see 3.7).** This is `examples/riverside_logs.json`. Copy it\nto `bank_features/riverside_logs.json`, and a style ring names it as `riverside_logs`:\n```json\n{ \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"] }\n```\n\n**Another definition,** `examples/riverside_stumps.json`:\n```json\n{ \"features\": [\"streamsreflowing:fallen_log_stub_a\", \"streamsreflowing:fallen_log_stub_b\"] }\n```\n\nThe mod's own pieces you can list in a definition: `streamsreflowing:fallen_log_a` and `_b`,\n`fallen_log_stub_a` and `_b`, the `_pale` driftwood version of each of those four,\n`fallen_tree_small`, `fallen_tree_acacia`, and `fallen_tree_jungle_a` and `_b`.\n\n**Reeds and bushes on overworld banks on their own, but not in frozen biomes:**\n```json\n{\n  \"features\": [\"minecraft:patch_sugar_cane\", \"minecraft:patch_berry_bush\"],\n  \"tags\": [\"minecraft:is_overworld\"],\n  \"exclude_tags\": [\"minecraft:is_frozen\"],\n  \"chance\": 30\n}\n```\n\n---\n\n## 5. Config toggles\n\nThese options are in the main config file:\n\n- NeoForge and Forge: `config/streamsreflowing-common.toml`\n- Fabric: `config/streamsreflowing.properties`\n\n| Option | Values | Default | Function |\n|---|---|---|---|\n| `customBanks` | true or false | true | The main switch for bank styles. Set `false` to show raw carved land. |\n| `allWaterBanks` | 0, 1, 2 | 1 | Bank styles on other water too: `0` this mod's streams and lakes only, `1` also vanilla rivers, lakes and ponds, `2` also ocean shores. |\n| `bankFeatures` | 0 to 3 | 3 | Where bank features may appear: `0` nowhere, `1` this mod's streams, `2` streams and its lakes, `3` every water edge. |\n\n---\n\n## 6. How to make a datapack\n\nDo these steps to make a datapack for your bank files.\n\n1. Make a folder for the datapack. Use any name.\n2. Make a `pack.mcmeta` file in that folder. Put this text in it:\n\n```json\n{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"My custom stream banks\"\n  }\n}\n```\n\n3. Set the correct `pack_format` number for your Minecraft version:\n\n| Minecraft version | `pack_format` |\n|---|---|\n| 1.20.1 | 15 |\n| 1.21 / 1.21.1 | 48 |\n\n   The number is different for each Minecraft version, and it changes often. For a version\n   that is not in the table, search online for its data pack format number.\n\n   **Note:** Minecraft shows a warning for a wrong `pack_format`, but it loads the datapack in\n   most cases. The bank files still work.\n\n4. Make the folders for your files:\n   - Bank styles: `data/<namespace>/streamsreflowing/bank_style/`\n   - Bank features: `data/<namespace>/streamsreflowing/bank_feature/`\n5. Put your JSON files in the correct folder.\n6. Put the datapack folder in a world:\n   - Put it in the `datapacks/` folder of the world save. Then run `/reload`, or make a new\n     world.\n7. To use the datapack in all worlds, use a global-datapack mod.\n\n---\n\n## 7. Quick reference\n\n| Task | Action |\n|---|---|\n| Change the bank blocks in one biome | Make a bank style with `biomes` or `tags` and `bed`/`waterline`/`bank`. |\n| Two bank zones | Add `top_bank`. `bank` takes the inner half of the cut, `top_bank` the rest. |\n| Patches instead of a random mix | A list of blocks is patches already. Set `scale` for the patch size. |\n| Stripes along the stream | `\"pattern\": \"bands\"` on the ring. |\n| Rock on steep reaches, sand near the mouth | An `overrides` entry with `\"when\": { \"slope\": \"> 0.5\" }` or `{ \"to_mouth\": \"< 0.2\" }`. |\n| Gravel on the inside of bends | `\"when\": { \"bend\": \"> 0.2\", \"side\": \"inner\" }`. |\n| Make one style always win in its biomes | `\"star\": true`. |\n| Leave one biome's stream raw | `{ \"biomes\": [...], \"enabled\": false }`. |\n| Keep the bank natural, skin the bed | `\"bank\": \"minecraft:air\"`. |\n| Put scenery on a ring | A feature definition file, named in the ring's `features`. |\n| Send it with a modpack | Put the JSON in `data/<namespace>/streamsreflowing/bank_style` or `bank_feature`. |\n| See a technique working | Copy a file from `bank_styles/examples/` (3.11). |\n| Keep notes in a file | Any field that starts with `_`, for example `\"_about\"`. |\n\n",
               var2
            );
            Path var3 = var0.resolve("examples");
            Files.createDirectories(var3);
            a(
               var3.resolve("example.json"),
               "{\n  \"features\": [\"minecraft:patch_pumpkin\"],\n  \"tags\": [\"minecraft:is_overworld\"],\n  \"exclude_tags\": [\"minecraft:is_frozen_ocean\"],\n  \"exclude_biomes\": [\"minecraft:snowy_plains\", \"minecraft:snowy_taiga\", \"minecraft:ice_spikes\"],\n  \"chance\": 8\n}\n",
               var2
            );
            a(var3.resolve("definition_example.json"), "{\n  \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"]\n}\n", var2);
            a(
               var3.resolve("riverside_logs.json"),
               "{\n  \"_about\": \"A definition: copy it into bank_features/ and a bank style ring can place it as riverside_logs.\",\n  \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"]\n}\n",
               var2
            );
            a(
               var3.resolve("riverside_stumps.json"),
               "{\n  \"_about\": \"A definition: copy it into bank_features/ and a bank style ring can place it as riverside_stumps.\",\n  \"features\": [\"streamsreflowing:fallen_log_stub_a\", \"streamsreflowing:fallen_log_stub_b\"]\n}\n",
               var2
            );
            a(
               var3.resolve("riverside_lilies.json"),
               "{\n  \"_about\": \"A definition for a WATERLINE ring: lily pads on the open surface. Copy it into bank_features/.\",\n  \"features\": [\"minecraft:patch_waterlily\"]\n}\n",
               var2
            );
            a(
               var3.resolve("riverside_seagrass.json"),
               "{\n  \"_about\": \"A definition for a BED ring: grass on the submerged floor. Copy it into bank_features/.\",\n  \"features\": [\"minecraft:seagrass_river\", \"minecraft:seagrass_simple\"]\n}\n",
               var2
            );
            if (var2) {
               Files.writeString(var1, BankStyleLoader.guideVersion() + System.lineSeparator());
            }

            b(var0);
         } catch (IOException var4) {
            Constants.LOG.warn("Failed to seed custom bank features config in {}", var0, var4);
         }
      }
   }

   private static void b(Path var0) throws IOException {
      Path var1 = var0.resolve(".withdrew_2_13_2");
      if (!Files.exists(var1)) {
         int var2 = 0;
         int var3 = 0;

         for (Entry var5 : e.entrySet()) {
            Path var6 = var0.resolve((String)var5.getKey());
            if (Files.exists(var6)) {
               if (Files.readString(var6).equals(var5.getValue())) {
                  Files.delete(var6);
                  var2++;
               } else {
                  var3++;
                  Constants.LOG.info("Bank scenery '{}' has been edited, so it stays -- delete it yourself if you no longer want it.", var5.getKey());
               }
            }
         }

         Files.writeString(var1, "The fallen logs, trees and driftwood seeded by 2.13.0/2.13.1 were withdrawn here." + System.lineSeparator());
         if (var2 > 0 || var3 > 0) {
            Constants.LOG
               .info(
                  "Withdrew {} seeded bank scenery file(s) from {}{}",
                  new Object[]{var2, var0, var3 > 0 ? " (" + var3 + " left in place because they had been edited)" : ""}
               );
         }
      }
   }

   private static void c(Path var0) throws IOException {
      Path var1 = var0.resolve(".seeded_defaults");
      LinkedHashSet var2 = new LinkedHashSet();
      if (Files.exists(var1)) {
         var2.addAll(Files.readAllLines(var1));
      }

      boolean var3 = false;

      for (Entry var5 : e.entrySet()) {
         if (!var2.contains(var5.getKey())) {
            a(var0.resolve((String)var5.getKey()), (String)var5.getValue());
            var2.add((String)var5.getKey());
            var3 = true;
         }
      }

      if (var3) {
         Files.writeString(var1, String.join(System.lineSeparator(), var2) + System.lineSeparator());
      }
   }

   private static void a(Path var0, String var1) throws IOException {
      a(var0, var1, false);
   }

   private static void a(Path var0, String var1, boolean var2) throws IOException {
      if (var2 || !Files.exists(var0)) {
         Files.writeString(var0, var1);
      }
   }

   static String a() {
      return "# Streams Reflowing -- custom bank features\n\nThis pass plants features on the land around water. A file here is one of two things:\n\n- A **definition** -- just `features` (and maybe `chance`), no biomes, no tags. It places nothing by itself.\n  A bank style puts it on a ring: `\"features\": [{ \"id\": \"<this file's name without .json>\", \"chance\": 2 }]`\n  inside the style's `bank`, `bed` or any other ring. See `examples/riverside_logs.json` and\n  `examples/riverside_stumps.json` for dry rings, and `examples/riverside_lilies.json` (lily pads at a\n  `waterline`) and `examples/riverside_seagrass.json` (a `bed`) for the wet ones, plus the bank-style\n  GUIDE. This is the way to put scenery exactly where you want it. A ring names the DEFINITION, never a\n  placed feature: `\"id\": \"streamsreflowing:fallen_log_a\"` in a ring places nothing.\n- A **self-placing entry** -- `features` plus `biomes` and/or `tags`. It runs on its own wherever those\n  match, as it always has. Every matching entry runs (there is no override contest like bank styles have).\n\n- **How often one appears**: `chance` in its file (or in the style's ring, for a definition).\n- **Where**: the ring of the style that names it, or `biomes` / `tags` / the excludes.\n- **What**: any placed feature by id. The mod's own bank pieces are available to reference --\n  `streamsreflowing:fallen_log_a` / `_b`, `fallen_log_stub_a` / `_b` (and their `_pale` driftwood\n  variants), `fallen_tree_small`, `fallen_tree_acacia`, `fallen_tree_jungle_a` / `_b` -- and a structure\n  you save with a structure block can be placed the same way (see the guide).\n\n> **Full guide** (every field, more examples, datapacks, and bank styles too): see `GUIDE.md` in this\n> folder. This file is the quick reference.\n\n## How to add one\n1. Look in `examples/example.json` for the shape. 2. Copy it UP into this folder (or write your own) and\nedit it. 3. Relaunch. (Files in `examples/` are reference-only and are NOT loaded.)\n\n## Where features place\nOn the bank around water, bounded by your **bank scenery** setting (`bankFeatures`): 0 = nowhere,\n1 = this mod's streams, 2 = also its lakes, 3 = every water edge. Each listed feature is run AT the\nchosen column, on sturdy ground, exactly as written -- trees and big features included (you chose them).\n\nA definition on a style's ring follows THAT ring: `bank`, `top_bank` and extra rings plant on the dry\nland beside the water; `bed` plants on the submerged floor (water above it -- sea grass, kelp, coral);\n`waterline` plants at the open surface (water below, air above -- lily pads). Wet placements are skipped\nin biomes tagged `#streamsreflowing:no_underwater_flora`, the same opt-out `underwaterFlora` honours.\n\n## Fields\n- `features` : a POOL of PLACED-feature ids -- ONE is chosen at random per placement. Required.\n- `biomes` : exact biome ids to include.   `tags` : biome tags the biome must ALL have to include.\n  (If both are empty, the file is a DEFINITION and places nothing until a bank style names it.)\n- `exclude_biomes` / `exclude_tags` : biomes/tags to drop AFTER the include -- so \"all overworld but not\n  frozen\" is `tags: [\"minecraft:is_overworld\"]` + `exclude_tags: [\"minecraft:is_frozen_ocean\", ...]`\n  (plus any snowy biomes/tags you want out).\n- `chance` : 0-100, the percent chance per sampled SITE (a few are drawn per chunk from each band,\n  so density is the same along a brook and a wide river). Default 100.\n\n## Shipping entries with a mod / datapack\nPut `<name>.json` files at `data/<namespace>/streamsreflowing/bank_feature/` in any datapack or mod jar --\nthey load automatically and are merged with this folder.\n{\n  \"features\": [\"minecraft:patch_pumpkin\"],\n  \"tags\": [\"minecraft:is_overworld\"],\n  \"exclude_tags\": [\"minecraft:is_frozen_ocean\"],\n  \"exclude_biomes\": [\"minecraft:snowy_plains\", \"minecraft:snowy_taiga\", \"minecraft:ice_spikes\"],\n  \"chance\": 8\n}\n{\n  \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"]\n}\n{\n  \"_about\": \"A definition: copy it into bank_features/ and a bank style ring can place it as riverside_logs.\",\n  \"features\": [\"streamsreflowing:fallen_log_a\", \"streamsreflowing:fallen_log_b\"]\n}\n{\n  \"_about\": \"A definition: copy it into bank_features/ and a bank style ring can place it as riverside_stumps.\",\n  \"features\": [\"streamsreflowing:fallen_log_stub_a\", \"streamsreflowing:fallen_log_stub_b\"]\n}\n{\n  \"_about\": \"A definition for a WATERLINE ring: lily pads on the open surface. Copy it into bank_features/.\",\n  \"features\": [\"minecraft:patch_waterlily\"]\n}\n{\n  \"_about\": \"A definition for a BED ring: grass on the submerged floor. Copy it into bank_features/.\",\n  \"features\": [\"minecraft:seagrass_river\", \"minecraft:seagrass_simple\"]\n}\n";
   }
}
