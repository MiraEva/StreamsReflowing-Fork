package dev.streamsreflowing.client;

import dev.streamsreflowing.core.config.QualityPreset;
import dev.streamsreflowing.core.config.WatershedSize;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SettingsCatalog {
   public static final String PERFORMANCE = "Производительность и планирование";
   public static final String SHAPE = "Форма русел";
   public static final String BANKS = "Окружение и берега";
   public static final String WATER = "Вода и течение";
   public static final String MODS = "Моды";
   public static final String EFFECTS = "Эффекты";
   public static final String OTHER = "Прочее";
   public static final List<String> TABS = List.of(
      "Производительность и планирование", "Форма русел", "Окружение и берега", "Вода и течение", "Моды", "Эффекты", "Прочее"
   );
   public static final SettingsCatalog.Preset DEFAULT_PRESET = new SettingsCatalog.Preset("По умолчанию", null, null, 75, 1024);
   public static final List<SettingsCatalog.Preset> PRESETS = List.of(
      new SettingsCatalog.Preset("Малый масштаб, быстрый", "BLOCKS_512", "POTATO", 50, 512),
      new SettingsCatalog.Preset("Малый масштаб, детальный", "BLOCKS_512", "HIGH", 50, 512),
      DEFAULT_PRESET,
      new SettingsCatalog.Preset("Крупный масштаб, быстрый", "BLOCKS_1536", "LOW", 150, 1500),
      new SettingsCatalog.Preset("Крупный масштаб, детальный", "BLOCKS_1536", "HIGH", 150, 1500),
      new SettingsCatalog.Preset("Огромный мир", "BLOCKS_4096", "POTATO", 300, 2000),
      new SettingsCatalog.Preset("Колоссальный масштаб (предгенерация)", "BLOCKS_8192", "EXTREME", 300, 2000)
   );
   public static final String CUSTOM_PRESET = "Особый";
   private static final Map<String, SettingsCatalog.Entry> ENTRIES = new LinkedHashMap<>();
   public static final SettingsCatalog.Entry PRESET = new SettingsCatalog.Entry(
      "preset", "Пресет", "Производительность и планирование", SettingsCatalog.Kind.CHOICE
   );
   private static final String[] RIVER_BIOME = new String[]{"0", "Выкл", "1", "Подходящий биом реки", "2", "Биом реки с цветами суши"};

   private SettingsCatalog() {
   }

   public static SettingsCatalog.Preset preset(String var0) {
      for (SettingsCatalog.Preset var2 : PRESETS) {
         if (var2.name().equals(var0)) {
            return var2;
         }
      }

      return null;
   }

   private static String trim(double var0) {
      String var2 = String.format(Locale.ROOT, "%.3f", var0);

      while (var2.contains(".") && (var2.endsWith("0") || var2.endsWith("."))) {
         var2 = var2.substring(0, var2.length() - 1);
      }

      return var2;
   }

   private static SettingsCatalog.Entry toggle(String var0, String var1, String var2) {
      SettingsCatalog.Entry var3 = new SettingsCatalog.Entry(var1, var2, var0, SettingsCatalog.Kind.TOGGLE);
      ENTRIES.put(var1, var3);
      return var3;
   }

   private static SettingsCatalog.Entry text(String var0, String var1, String var2) {
      SettingsCatalog.Entry var3 = new SettingsCatalog.Entry(var1, var2, var0, SettingsCatalog.Kind.TEXT);
      ENTRIES.put(var1, var3);
      return var3;
   }

   private static SettingsCatalog.Entry choice(String var0, String var1, String var2, String... var3) {
      SettingsCatalog.Entry var4 = new SettingsCatalog.Entry(var1, var2, var0, SettingsCatalog.Kind.CHOICE);

      for (int var5 = 0; var5 + 1 < var3.length; var5 += 2) {
         var4.choices.put(var3[var5], var3[var5 + 1]);
      }

      ENTRIES.put(var1, var4);
      return var4;
   }

   private static SettingsCatalog.Entry slider(String var0, String var1, String var2, String var3, boolean var4, double... var5) {
      SettingsCatalog.Entry var6 = new SettingsCatalog.Entry(var1, var2, var0, SettingsCatalog.Kind.SLIDER);
      var6.stops = var5;
      var6.whole = var4;
      var6.unit = var3;
      ENTRIES.put(var1, var6);
      return var6;
   }

   private static SettingsCatalog.Entry percent(String var0, String var1, String var2, int var3, int var4) {
      SettingsCatalog.Entry var5 = new SettingsCatalog.Entry(var1, var2, var0, SettingsCatalog.Kind.SLIDER);
      var5.continuous = true;
      var5.min = var3 / 100.0;
      var5.max = var4 / 100.0;
      ENTRIES.put(var1, var5);
      return var5;
   }

   public static int order(String var0) {
      int var1 = 0;

      for (String var3 : ENTRIES.keySet()) {
         if (var3.equals(var0)) {
            return var1;
         }

         var1++;
      }

      return Integer.MAX_VALUE;
   }

   private static String title(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT).replace('_', ' ');
      return var1.isEmpty() ? var1 : Character.toUpperCase(var1.charAt(0)) + var1.substring(1);
   }

   public static SettingsCatalog.Entry of(String var0, String var1) {
      SettingsCatalog.Entry var2 = ENTRIES.get(var0);
      if (var2 != null) {
         return var2;
      }

      boolean var3 = "true".equalsIgnoreCase(var1) || "false".equalsIgnoreCase(var1);
      return new SettingsCatalog.Entry(var0, humanise(var0), "Прочее", var3 ? SettingsCatalog.Kind.TOGGLE : SettingsCatalog.Kind.TEXT);
   }

   private static String humanise(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (Character.isUpperCase(var3) && var2 > 0) {
            var1.append(' ').append(Character.toLowerCase(var3));
         } else {
            var1.append(var2 == 0 ? Character.toUpperCase(var3) : var3);
         }
      }

      return var1.toString();
   }

   static {
      toggle("Производительность и планирование", "generateStreams", "Генерация ручьёв").cost(SettingsCatalog.Cost.HIGH);
      SettingsCatalog.Entry var0 = choice("Производительность и планирование", "terrainAccuracyLevel", "Точность рельефа").cost(SettingsCatalog.Cost.HIGH);

      for (QualityPreset var4 : QualityPreset.values()) {
         String var5 = switch (var4) {
            case POTATO -> "Минимальная (картофель)";
            case LOW -> "Низкая";
            case MEDIUM -> "Средняя";
            case HIGH -> "Высокая";
            case EXTREME -> "Экстремальная";
            default -> title(var4.name());
         };
         var0.choices.put(var4.name(), var5);
      }

      SettingsCatalog.Entry var6 = choice("Производительность и планирование", "watershedSize", "Размер речной системы").cost(SettingsCatalog.Cost.MEDIUM);

      for (WatershedSize var13 : WatershedSize.values()) {
         var6.choices.put(var13.name(), var13.name().replace("BLOCKS_", "") + " бл.");
      }

      toggle("Производительность и планирование", "fastTerrainEstimate", "Оценка высот рельефа").cost(SettingsCatalog.Cost.BIG_BOOST);
      toggle("Производительность и планирование", "adaptiveGridRead", "Адаптивное чтение рельефа").cost(SettingsCatalog.Cost.MEDIUM);
      choice("Производительность и планирование", "terrainFit", "Сглаживание по рельефу", "-1", "Авто", "0", "Выкл", "2", "Мягкое", "4", "Точное")
         .cost(SettingsCatalog.Cost.MEDIUM);
      choice(
            "Производительность и планирование",
            "riverDrainageSize",
            "Дальность течения рек",
            "1",
            "1 речная система",
            "2",
            "2 речные системы",
            "3",
            "4 речные системы"
         )
         .cost(SettingsCatalog.Cost.MEDIUM);
      toggle("Производительность и планирование", "fastChunkLoading", "Быстрая загрузка чанков").cost(SettingsCatalog.Cost.BOOST);
      toggle("Производительность и планирование", "alignReadPoints", "Сетка чтения в 4 блока").cost(SettingsCatalog.Cost.BIG_BOOST);
      toggle("Производительность и планирование", "legacyStreamShaping", "Форма русел до версии 2.14").cost(SettingsCatalog.Cost.LOW);
      percent("Производительность и планирование", "wetnessMultiplier", "Общий объём воды", 25, 300).cost(SettingsCatalog.Cost.MEDIUM).newPage();
      percent("Производительность и планирование", "wetnessDryFloor", "Вода в сухих биомах", 0, 100).cost(SettingsCatalog.Cost.LOW);
      SettingsCatalog.Entry var8 = percent("Производительность и планирование", "growDensity", "Ветвление ручьёв", 40, 300)
         .cost(SettingsCatalog.Cost.MEDIUM)
         .note(
            "Насколько сильно ветвятся ручьи. Больше = больше мелких притоков и выше нагрузка на генерацию.",
            "100% — значение по умолчанию.",
            "Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла."
         );
      var8.inverseOf = 0.75;
      slider(
         "Производительность и планирование",
         "minStreamLength",
         "Минимальная длина ручья",
         "бл.",
         true,
         0.0,
         10.0,
         20.0,
         30.0,
         50.0,
         75.0,
         100.0,
         150.0,
         200.0,
         300.0,
         500.0,
         750.0,
         1000.0
      );
      choice("Производительность и планирование", "minLengthScope", "Мин. длина действует на", "1", "Океан", "2", "Океан и реки", "3", "Океан, реки и озёра");
      text("Производительность и планирование", "outletBiomes", "Биомы устьев ручьёв");
      text("Производительность и планирование", "streamBiomeBlacklist", "Биомы без ручьёв");
      toggle("Производительность и планирование", "connectNearbyWater", "Соединять тупики (экспер.)").cost(SettingsCatalog.Cost.MEDIUM);
      choice(
            "Производительность и планирование",
            "connectRadius",
            "Дистанция поиска соединений",
            "-1",
            "Авто",
            "64",
            "64 блока",
            "128",
            "128 блоков",
            "256",
            "256 блоков"
         )
         .cost(SettingsCatalog.Cost.LOW);
      toggle("Производительность и планирование", "generateLakes", "Генерация озёр").cost(SettingsCatalog.Cost.HIGH).newPage().heading("Озёра");
      SettingsCatalog.Entry var10 = slider(
            "Производительность и планирование", "lakeMinArea", "Минимальный размер озера", "", false, 256.0, 512.0, 1024.0, 1500.0, 2000.0, 4096.0
         )
         .cost(SettingsCatalog.Cost.MEDIUM)
         .note(
            "Минимальная впадина, которая станет озером (по её поперечнику).",
            "Глубокие ямы на пути ручья заполняются водой независимо от их размера.",
            "Вступает в силу в новых мирах. Уже созданная местность сохраняет свои озёра."
         );
      var10.across = true;
      choice(
            "Производительность и планирование",
            "lakeMaxArea",
            "Максимальный размер озера",
            "0.0",
            "Авто",
            "16384.0",
            "128 бл. в поперечнике",
            "65536.0",
            "256 бл. в поперечнике",
            "262144.0",
            "512 бл. в поперечнике"
         )
         .cost(SettingsCatalog.Cost.LOW)
         .note(
            "Максимально допустимый размер озера по ширине. Авто = автоматический расчёт.",
            "Вступает в силу в новых мирах. Уже созданная местность сохраняет свои озёра."
         );
      toggle("Производительность и планирование", "offloadRegionBuilds", "Подготовка русел игроками").cost(SettingsCatalog.Cost.BIG_BOOST).newPage();
      toggle("Производительность и планирование", "helpPrepareRegions", "Помощь серверу в подготовке").cost(SettingsCatalog.Cost.MEDIUM);
      choice("Производительность и планирование", "helpRegionsAtOnce", "Число областей помощи за раз", "1", "1", "2", "2", "3", "3")
         .cost(SettingsCatalog.Cost.MEDIUM);
      percent("Форма русел", "widthScale", "Ширина русла", 50, 200).cost(SettingsCatalog.Cost.LOW);
      slider("Форма русел", "baseWidth", "Ширина у истока", "бл.", false, 0.2, 0.4, 0.6, 0.8, 1.0);
      SettingsCatalog.Entry var12 = slider("Форма русел", "maxWidth", "Максимальная ширина", "бл.", false, 8.0, 12.0, 16.0, 24.0, 32.0)
         .cost(SettingsCatalog.Cost.LOW)
         .note("Наибольшая возможная ширина ручья от берега до берега.", "Вступает в силу в новых мирах. Уже созданная местность сохраняет свои русла.");
      var12.shownTimes = 2.0;
      percent("Форма русел", "meanderStrength", "Извилистость", 0, 200);
      percent("Форма русел", "streamDepth", "Глубина русла", 10, 50);
      percent("Форма русел", "widthFollowsSlope", "Сужение на крутых склонах", 0, 100);
      percent("Форма русел", "widthFollowsClimate", "Расширение во влажных биомах", 0, 100);
      percent("Форма русел", "widthVariation", "Изменчивость ширины", 0, 100);
      percent("Форма русел", "depthFollowsTerrain", "Углубление на крутых склонах", 0, 100);
      percent("Форма русел", "bendAsymmetry", "Глубокие внешние излучины", 0, 100);
      percent("Форма русел", "channelBars", "Гравийные отмели", 0, 100);
      percent("Форма русел", "deltaMouths", "Дельты в устьях", 0, 100);
      toggle("Форма русел", "containmentBerm", "Удержание воды на склонах");
      slider("Форма русел", "bankNoise", "Неровность береговой линии", "бл.", false, 0.0, 0.1, 0.21, 0.4, 0.6, 1.0);
      choice("Форма русел", "rebiomeStreams", "Биом ручьёв", RIVER_BIOME);
      choice("Форма русел", "rebiomeLakes", "Биом озёр", RIVER_BIOME);
      toggle("Окружение и берега", "customBanks", "Стили берегов").cost(SettingsCatalog.Cost.LOW);
      choice("Окружение и берега", "allWaterBanks", "Берега у другой воды", "0", "Выкл", "1", "Реки, озёра и пруды", "2", "Также берега океана")
         .cost(SettingsCatalog.Cost.LOW);
      choice("Окружение и берега", "bankFeatures", "Декорации берегов", "0", "Выкл", "1", "Ручьи", "2", "Ручьи и озёра", "3", "Вся вода")
         .cost(SettingsCatalog.Cost.LOW);
      choice("Окружение и берега", "vegetation", "Прибрежные растения", "0", "Выкл", "1", "Трава и папоротники", "2", "Также растения биома")
         .cost(SettingsCatalog.Cost.LOW);
      choice("Окружение и берега", "allWaterFlora", "Растения у прочей воды", "0", "Выкл", "1", "Трава и папоротники", "2", "Также растения биома")
         .cost(SettingsCatalog.Cost.LOW);
      slider("Окружение и берега", "floraRadius", "Радиус растений от воды", "бл.", true, 2.0, 3.0, 5.0, 8.0, 12.0).cost(SettingsCatalog.Cost.LOW);
      slider("Окружение и берега", "bankGrassPercent", "Густота береговой травы", "%", true, 0.0, 10.0, 20.0, 40.0, 60.0, 80.0, 100.0);
      slider("Окружение и берега", "biomeBoostPercent", "Плотность растений биома", "%", true, 0.0, 2.0, 5.0, 10.0, 20.0, 40.0).cost(SettingsCatalog.Cost.LOW);
      text("Окружение и берега", "biomeBoostBlacklist", "Исключения растений биома");
      choice("Окружение и берега", "underwaterFlora", "Подводные растения", "0", "Выкл", "1", "Озёра", "2", "Озёра и ручьи", "3", "Вся вода")
         .cost(SettingsCatalog.Cost.LOW);
      slider("Окружение и берега", "underwaterFloraPercent", "Густота подводных растений", "%", true, 0.0, 10.0, 20.0, 35.0, 50.0, 75.0, 100.0);
      text("Окружение и берега", "underwaterFloraBiomeBlacklist", "Биомы без подводных растений");
      toggle("Вода и течение", "vanillaWaterFlow", "Течение воды").cost(SettingsCatalog.Cost.LOW);
      toggle("Вода и течение", "flowVanillaRivers", "Течение в ванильных реках").cost(SettingsCatalog.Cost.MEDIUM);
      toggle("Вода и течение", "riverSeamAgreement", "Единое направление рек");
      toggle("Вода и течение", "spillOverWater", "Растекание по водоёмам").cost(SettingsCatalog.Cost.LOW);
      toggle("Вода и течение", "spillOnStreamsOnly", "Только там, где есть течение");
      toggle("Вода и течение", "boatsFloatOnFlowingWater", "Лодки держатся на течении");
      toggle("Вода и течение", "waterMobsIgnoreFlow", "Рыбы сопротивляются течению");
      slider("Вода и течение", "itemDriftSpeed", "Скорость сноса предметов", "бл/с", false, 0.0, 0.5, 1.0, 1.5, 2.0, 3.0, 4.0);
      percent("Вода и течение", "playerPush", "Сила течения на игрока", 0, 400);
      percent("Вода и течение", "boatPush", "Сила течения на лодки", 0, 400);
      percent("Вода и течение", "mobPush", "Сила течения на мобов", 0, 400);
      text("Вода и течение", "mobsHoldInCurrent", "Мобы, стоящие на месте");
      text("Вода и течение", "mobsDriftInCurrent", "Мобы, уносимые течением");
      toggle("Моды", "reterraforgedCompat", "Совместимость с ReTerraForged");
      choice("Моды", "physicsModWater", "Вода Physics Mod на ручьях", "0", "Оба эффекта", "1", "Только Physics Mod", "3", "Только этого мода");
      slider("Моды", "vesselDriftSpeed", "Скорость сноса судов Sable", "бл/с", false, 0.0, 10.0, 20.0, 30.0, 45.0, 60.0);
      toggle("Моды", "partnerParticles", "Интеграция с Effective и Particular");
      choice("Моды", "waterfallFoam", "Пена у водопадов", "0", "Нет", "1", "Их частицы", "2", "Наши частицы", "3", "Оба вида");
      toggle("Моды", "bakedFluidWhenFlowingFluids", "Вода ручьёв при Flowing Fluids");
      toggle("Моды", "forceBakedFluid", "Всегда использовать воду мода");
      percent("Эффекты", "waterAmbience", "Громкость звуков воды", 0, 200);
      choice("Эффекты", "streamBubbles", "Пузыри и всплески", "0", "Выкл", "1", "Простые", "2", "Текущие").cost(SettingsCatalog.Cost.MEDIUM);
      toggle("Эффекты", "streamDebris", "Плывущие палки и листья").cost(SettingsCatalog.Cost.LOW);
      percent("Эффекты", "particleSize", "Размер капель брызг", 50, 200);
      percent("Эффекты", "particleDensity", "Количество брызг", 0, 200).cost(SettingsCatalog.Cost.MEDIUM);
      slider("Эффекты", "maxStreamParticlesPerTick", "Лимит частиц в такт", "", true, 0.0, 250.0, 500.0, 1000.0, 2000.0, 3000.0, 5000.0)
         .cost(SettingsCatalog.Cost.MEDIUM);
      choice(
            "Эффекты",
            "streamParticleScanRadius",
            "Брызги на дальних водопадах",
            "0",
            "Только рядом",
            "64",
            "64 блока",
            "128",
            "128 блоков",
            "256",
            "256 блоков",
            "384",
            "384 блока",
            "512",
            "512 блоков"
         )
         .cost(SettingsCatalog.Cost.LOW);
      toggle("Эффекты", "particlesOnVanillaWater", "Брызги на ванильных водопадах").cost(SettingsCatalog.Cost.LOW);
      toggle("Эффекты", "serverStreamParticles", "Брызги для игроков сервера");
      toggle("Прочее", "welcomeMessage", "Приветственное сообщение");
      toggle("Прочее", "loadingNotice", "Прогресс на экране загрузки");
   }

   public enum Cost {
      HIGH("высокая", "Высокая", -2079686),
      MEDIUM("средняя", "Средняя", -881624),
      LOW("низкая", "Низкая", -863942),
      NONE("почти отсутствует", "Нет", -11751600),
      BOOST("быстрее при включении", "Быстрее", -13912664),
      BIG_BOOST("намного быстрее при включении", "Намного быстрее", -6596144);

      public final String label;
      public final String key;
      public final int colour;

      Cost(String nullxx, String nullxxx, int nullxxxx) {
         this.label = nullxx;
         this.key = nullxxx;
         this.colour = nullxxxx;
      }
   }

   public static final class Entry {
      public final String key;
      public final String name;
      public final String tab;
      public final SettingsCatalog.Kind kind;
      public final Map<String, String> choices = new LinkedHashMap<>();
      public double[] stops = new double[0];
      public boolean whole;
      public String unit = "";
      public boolean asPercentOfOne;
      public SettingsCatalog.Cost cost = SettingsCatalog.Cost.NONE;
      public boolean continuous;
      public double min;
      public double max = 1.0;
      public double inverseOf;
      public double shownTimes = 1.0;
      public String[] note;
      public boolean across;
      public String heading;
      public boolean newPage;

      SettingsCatalog.Entry note(String... var1) {
         this.note = var1;
         return this;
      }

      public double shownOf(double var1) {
         return this.inverseOf > 0.0 ? (var1 > 0.0 ? this.inverseOf / var1 : this.max) : var1;
      }

      public String storeShown(double var1) {
         return this.inverseOf > 0.0
            ? Double.toString(Math.round(this.inverseOf / Math.max(0.01, var1) * 10000.0) / 10000.0)
            : Double.toString(Math.round(var1 * 100.0) / 100.0);
      }

      SettingsCatalog.Entry heading(String var1) {
         this.heading = var1;
         return this;
      }

      SettingsCatalog.Entry newPage() {
         this.newPage = true;
         return this;
      }

      SettingsCatalog.Entry cost(SettingsCatalog.Cost var1) {
         this.cost = var1;
         return this;
      }

      Entry(String var1, String var2, String var3, SettingsCatalog.Kind var4) {
         this.key = var1;
         this.name = var2;
         this.tab = var3;
         this.kind = var4;
      }

      public double[] stopsWith(double... var1) {
         ArrayList<Double> var2 = new ArrayList<>();

         for (double var6 : this.stops) {
            var2.add(var6);
         }

         for (double var17 : var1) {
            boolean var8 = false;

            for (double var10 : var2) {
               var8 |= Math.abs(var10 - var17) < 1.0E-9;
            }

            if (!var8 && !Double.isNaN(var17)) {
               var2.add(var17);
            }
         }

         var2.sort(Double::compare);
         double[] var13 = new double[var2.size()];

         for (int var15 = 0; var15 < var13.length; var15++) {
            var13[var15] = (Double)var2.get(var15);
         }

         return var13;
      }

      public String show(double var1) {
         if (this.continuous) {
            return Math.round(this.shownOf(var1) * 100.0) + "%";
         }

         if (this.asPercentOfOne) {
            return Math.round(var1 * 100.0) + "%";
         }

         if (this.across) {
            return Math.round(Math.sqrt(var1)) + " бл. в поперечнике";
         }

         double var4 = var1 * this.shownTimes;
         String var3 = !this.whole && var4 != Math.rint(var4) ? SettingsCatalog.trim(var4) : Long.toString(Math.round(var4));
         return this.unit.isEmpty() ? var3 : (!this.unit.equals("%") && !this.unit.equals("x") ? var3 + " " + this.unit : var3 + this.unit);
      }

      public String store(double var1) {
         return this.whole ? Long.toString(Math.round(var1)) : Double.toString(var1);
      }
   }

   public enum Kind {
      TOGGLE,
      CHOICE,
      SLIDER,
      TEXT;
   }

   public record Preset(String name, String size, String accuracy, int shortest, int smallestLake) {
      public String[][] values() {
         return new String[][]{
            {"watershedSize", this.size},
            {"terrainAccuracyLevel", this.accuracy},
            {"minStreamLength", Integer.toString(this.shortest)},
            {"lakeMinArea", Integer.toString(this.smallestLake)}
         };
      }
   }
}
