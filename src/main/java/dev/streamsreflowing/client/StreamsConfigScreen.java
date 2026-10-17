package dev.streamsreflowing.client;

import dev.streamsreflowing.config.StreamsSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;

public final class StreamsConfigScreen extends Screen {
   private static final int ROW_HEIGHT = 22;
   private static final int CONTROL_WIDTH = 150;
   private static final int RESET_WIDTH = 40;
   private static final int TAB_PADDING = 10;
   private static final int TAB_GAP = 2;
   private static final int FLAG_WIDTH = 4;
   private static final int NAME_INSET = 8;
   private static final long SAVE_AFTER_MILLIS = 400L;
   private final Screen parent;
   private final List<StreamsConfigScreen.Row> rows = new ArrayList<>();
   private final StreamsConfigScreen.Row presetRow = new StreamsConfigScreen.Row();
   private final Map<String, String> pending = new LinkedHashMap<>();
   private long lastEdit;
   private String tab = SettingsCatalog.TABS.get(0);
   private int page;
   private String query = "";
   private EditBox search;
   private int rowsPerPage = 6;
   private int pageCount = 1;
   private int listX;
   private int listWidth;
   private final List<Object[]> shown = new ArrayList<>();
   private final List<Object[]> headings = new ArrayList<>();
   private int tipRowTop;
   private final ClientTooltipPositioner tipPlace = (screenWidth, screenHeight, x, y, tipWidth, tipHeight) -> {
      int left = Math.max(4, Math.min(x, screenWidth - tipWidth - 4));
      int below = this.tipRowTop + 22 + 4;
      int above = this.tipRowTop - tipHeight - 6;
      int top = below + tipHeight + 4 <= screenHeight ? below : (above >= 4 ? above : screenHeight - tipHeight - 4);
      return new Vector2i(left, Math.max(4, top));
   };
   private static final int WHITE = -1;
   private static final int CHANGED = -11393;
   private static final int QUIET = -6250336;
   private static final int KEY_GAP = 10;

   public StreamsConfigScreen(Screen parent) {
      super(Component.literal("Streams Reflowing"));
      this.parent = parent;
      this.read(false);
      this.read(true);
      this.rows.sort(Comparator.comparingInt(row -> SettingsCatalog.order(row.key)));
      this.presetRow.key = SettingsCatalog.PRESET.key;
      this.presetRow.value = "";
      this.presetRow.doc = "Настраивает размер речной системы, точность рельефа, минимальную длину ручья и минимальный размер озера разом.\nЕсли изменить любой из них вручную, пресет сменится на «Особый».";
      this.presetRow.fallback = SettingsCatalog.DEFAULT_PRESET.name();
      this.presetRow.entry = SettingsCatalog.PRESET;
      this.rows.add(0, this.presetRow);
   }

   private void read(boolean client) {
      Map<String, String> defaults = StreamsSettings.defaultValues(client);

      for (String[] kv : StreamsSettings.snapshotOrdered(client)) {
         StreamsConfigScreen.Row row = new StreamsConfigScreen.Row();
         row.key = kv[0];
         row.value = kv[1];
         row.client = client;
         row.doc = kv.length > 2 ? kv[2] : "";
         row.fallback = defaults.getOrDefault(kv[0], kv[1]);
         row.entry = SettingsCatalog.of(kv[0], kv[1]);
         this.rows.add(row);
      }
   }

   private String current(StreamsConfigScreen.Row row) {
      return row == this.presetRow ? this.presetNow() : this.pending.getOrDefault(row.key, row.value);
   }

   private StreamsConfigScreen.Row rowOf(String key) {
      for (StreamsConfigScreen.Row row : this.rows) {
         if (row.key.equals(key)) {
            return row;
         }
      }

      return null;
   }

   private static String wanted(StreamsConfigScreen.Row row, String value) {
      return value == null ? row.fallback : value;
   }

   private boolean matches(SettingsCatalog.Preset preset) {
      for (String[] kv : preset.values()) {
         StreamsConfigScreen.Row row = this.rowOf(kv[0]);
         if (row == null || !same(this.current(row), wanted(row, kv[1]))) {
            return false;
         }
      }

      return true;
   }

   private String presetNow() {
      for (SettingsCatalog.Preset preset : SettingsCatalog.PRESETS) {
         if (this.matches(preset)) {
            return preset.name();
         }
      }

      return "Особый";
   }

   private void apply(SettingsCatalog.Preset preset) {
      for (String[] kv : preset.values()) {
         StreamsConfigScreen.Row row = this.rowOf(kv[0]);
         if (row != null) {
            this.edit(row, wanted(row, kv[1]));
         }
      }
   }

   private CycleButton<String> cycle(Map<String, String> labels, String start, int x, int y, String name, Consumer<String> picked) {
      List<String> options = new ArrayList<>(labels.keySet());
      return CycleButton.<String>builder(v -> Component.literal(labels.getOrDefault(v, v)))
         .withValues(options)
         .withInitialValue(start)
         .displayOnlyValue()
         .create(x, y, 150, 20, Component.literal(name), (button, v) -> picked.accept(v));
   }

   private static boolean same(String a, String b) {
      if (a != null && b != null) {
         if (a.trim().equalsIgnoreCase(b.trim())) {
            return true;
         }

         try {
            return Math.abs(Double.parseDouble(a.trim()) - Double.parseDouble(b.trim())) < 1.0E-9;
         } catch (NumberFormatException e) {
            return false;
         }
      } else {
         return a == b;
      }
   }

   private static double number(String value, double otherwise) {
      try {
         return Double.parseDouble(value.trim());
      } catch (NumberFormatException e) {
         return otherwise;
      }
   }

   private void edit(StreamsConfigScreen.Row row, String value) {
      if (same(value, row.value)) {
         this.pending.remove(row.key);
      } else {
         this.pending.put(row.key, value);
      }

      this.lastEdit = System.currentTimeMillis();
   }

   private void save() {
      if (!this.pending.isEmpty()) {
         Map<String, String> common = new HashMap<>();
         Map<String, String> client = new HashMap<>();

         for (StreamsConfigScreen.Row row : this.rows) {
            String value = this.pending.get(row.key);
            if (value != null) {
               (row.client ? client : common).put(row.key, value);
               row.value = value;
            }
         }

         this.pending.clear();
         StreamsSettings.setValues(common, client);
      }
   }

   public void tick() {
      super.tick();
      if (!this.pending.isEmpty() && System.currentTimeMillis() - this.lastEdit >= 400L) {
         this.save();
      }
   }

   public void removed() {
      this.save();
      super.removed();
   }

   private List<StreamsConfigScreen.Row> listed() {
      List<StreamsConfigScreen.Row> out = new ArrayList<>();
      String q = this.query.trim().toLowerCase(Locale.ROOT);

      for (StreamsConfigScreen.Row row : this.rows) {
         if (q.isEmpty()) {
            if (row.entry.tab.equals(this.tab)) {
               out.add(row);
            }
         } else if (row.entry.name.toLowerCase(Locale.ROOT).contains(q)
            || row.key.toLowerCase(Locale.ROOT).contains(q)
            || row.doc.toLowerCase(Locale.ROOT).contains(q)) {
            out.add(row);
         }
      }

      return out;
   }

   private boolean tabHasRows(String name) {
      for (StreamsConfigScreen.Row row : this.rows) {
         if (row.entry.tab.equals(name)) {
            return true;
         }
      }

      return false;
   }

   private void rebuild() {
      this.clearWidgets();
      this.init();
   }

   protected void init() {
      this.shown.clear();
      this.headings.clear();
      this.listWidth = Math.min(380, this.width - 16);
      this.listX = (this.width - this.listWidth) / 2;
      if (this.search == null) {
         this.search = new EditBox(this.font, 0, 0, 200, 16, Component.literal("Поиск"));
         this.search.setMaxLength(64);
         this.search.setHint(Component.literal("Поиск параметров..."));
         this.search.setResponder(text -> {
            if (!text.equals(this.query)) {
               this.query = text;
               this.page = 0;
               this.rebuild();
            }
         });
      }

      int searchWidth = Math.min(220, this.listWidth);
      this.search.setX(this.width / 2 - searchWidth / 2);
      this.search.setY(20);
      this.search.setWidth(searchWidth);
      this.addRenderableWidget(this.search);
      List<String> tabNames = new ArrayList<>();
      int namesWidth = 0;

      for (String name : SettingsCatalog.TABS) {
         if (this.tabHasRows(name)) {
            tabNames.add(name);
            namesWidth += this.font.width(name) + 10;
         }
      }

      int gaps = 2 * Math.max(0, tabNames.size() - 1);
      int room = this.width - 8;
      double squeeze = namesWidth + gaps > room ? (double)(room - gaps) / namesWidth : 1.0;
      int tabY = 42;
      int tabX = (this.width - (int)Math.floor(namesWidth * squeeze) - gaps) / 2;
      boolean searching = !this.query.trim().isEmpty();

      for (String name : tabNames) {
         int w = (int)Math.floor((this.font.width(name) + 10) * squeeze);
         Button button = Button.builder(Component.literal(name), b -> {
            this.tab = name;
            this.page = 0;
            this.query = "";
            this.search.setValue("");
            this.rebuild();
         }).bounds(tabX, tabY, w, 18).build();
         button.active = searching || !name.equals(this.tab);
         this.addRenderableWidget(button);
         tabX += w + 2;
      }

      int top = tabY + 26;
      int bottom = this.height - 46;
      this.rowsPerPage = Math.max(1, (bottom - top) / 22);
      List<List<StreamsConfigScreen.Row>> pages = new ArrayList<>();
      List<StreamsConfigScreen.Row> filling = new ArrayList<>();
      int lines = 0;

      for (StreamsConfigScreen.Row row : this.listed()) {
         int needs = row.entry.heading != null && !searching ? 2 : 1;
         if (lines + needs > this.rowsPerPage || row.entry.newPage && !searching && !filling.isEmpty()) {
            pages.add(filling);
            filling = new ArrayList<>();
            lines = 0;
         }

         filling.add(row);
         lines += needs;
      }

      if (!filling.isEmpty() || pages.isEmpty()) {
         pages.add(filling);
      }

      this.pageCount = pages.size();
      this.page = Math.max(0, Math.min(this.page, this.pageCount - 1));
      int y = top;

      for (StreamsConfigScreen.Row row : pages.get(this.page)) {
         if (row.entry.heading != null && !searching) {
            this.headings.add(new Object[]{y, row.entry.heading});
            y += 22;
         }

         this.place(row, y);
         y += 22;
      }

      int navY = this.height - 26;
      if (this.pageCount > 1) {
         this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            this.page = Math.floorMod(this.page - 1, this.pageCount);
            this.rebuild();
         }).bounds(this.width / 2 - 124, navY, 20, 20).build());
         this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            this.page = Math.floorMod(this.page + 1, this.pageCount);
            this.rebuild();
         }).bounds(this.width / 2 + 104, navY, 20, 20).build());
      }

      this.addRenderableWidget(
         Button.builder(Component.literal("Готово"), b -> this.onClose()).bounds(this.width / 2 - 100, this.height - 26, 200, 20).build()
      );
      if (searching) {
         this.setFocused(this.search);
      }
   }

   private void place(StreamsConfigScreen.Row row, int y) {
      this.shown.add(new Object[]{y, row});
      int controlX = this.listX + this.listWidth - 40 - 2 - 150;
      String value = this.current(row);
      SettingsCatalog.Entry entry = row.entry;
      if (row == this.presetRow) {
         Map<String, String> names = new LinkedHashMap<>();

         for (SettingsCatalog.Preset preset : SettingsCatalog.PRESETS) {
            names.put(preset.name(), preset.name());
         }

         names.putIfAbsent(value, value);
         this.addRenderableWidget(this.cycle(names, value, controlX, y, entry.name, picked -> {
            SettingsCatalog.Preset preset = SettingsCatalog.preset(picked);
            if (preset != null) {
               this.apply(preset);
               this.rebuild();
            }
         }));
         Button reset = Button.builder(Component.literal("Сброс"), b -> {
            this.apply(SettingsCatalog.DEFAULT_PRESET);
            this.rebuild();
         }).bounds(this.listX + this.listWidth - 40, y, 40, 20).build();
         reset.active = !same(value, row.fallback);
         this.addRenderableWidget(reset);
      } else {
         switch (entry.kind) {
            case TOGGLE:
               boolean on = "true".equalsIgnoreCase(value.trim());
               this.addRenderableWidget(
                  CycleButton.onOffBuilder(on)
                     .displayOnlyValue()
                     .create(controlX, y, 150, 20, Component.literal(entry.name), (button, v) -> this.edit(row, String.valueOf(v)))
               );
               break;
            case CHOICE:
               Map<String, String> labels = new LinkedHashMap<>(entry.choices);
               String start = null;

               for (String option : labels.keySet()) {
                  if (same(option, value)) {
                     start = option;
                  }
               }

               if (start == null) {
                  labels.put(value, value);
                  start = value;
               }

               this.addRenderableWidget(this.cycle(labels, start, controlX, y, entry.name, v -> this.edit(row, v)));
               break;
            case SLIDER:
               if (entry.continuous) {
                  this.addRenderableWidget(new StreamsConfigScreen.PercentSlider(controlX, y, 150, row, number(value, number(row.fallback, entry.min))));
               } else {
                  double now = number(value, Double.NaN);
                  double[] stops = entry.stopsWith(now, number(row.fallback, Double.NaN));
                  this.addRenderableWidget(new StreamsConfigScreen.SnapSlider(controlX, y, 150, row, stops, Double.isNaN(now) ? stops[0] : now));
               }
               break;
            default:
               EditBox box = new EditBox(this.font, controlX + 1, y + 1, 148, 18, Component.literal(entry.name));
               box.setMaxLength(4096);
               box.setValue(value);
               box.setResponder(text -> this.edit(row, text));
               this.addRenderableWidget(box);
         }

         Button reset = Button.builder(Component.literal("Сброс"), b -> {
            this.edit(row, row.fallback);
            this.rebuild();
         }).bounds(this.listX + this.listWidth - 40, y, 40, 20).build();
         reset.active = !same(value, row.fallback);
         this.addRenderableWidget(reset);
      }
   }

   private List<FormattedCharSequence> describe(StreamsConfigScreen.Row row) {
      StringBuilder text = new StringBuilder();
      String said = row.entry.note != null ? String.join("\n", row.entry.note) : row.doc;

      for (String line : said.split("\n")) {
         String t = line.trim();
         if (!t.isEmpty()) {
            text.append(t).append('\n');
         }
      }

      text.append("\nВлияние на производительность: ").append(row.entry.cost.label);
      text.append("\nПо умолчанию: ").append(labelOf(row, row.fallback));
      text.append("\nВ файле конфигурации: ").append(row.key);
      List<FormattedCharSequence> out = new ArrayList<>();

      for (String line : text.toString().split("\n")) {
         if (line.isEmpty()) {
            out.add(FormattedCharSequence.EMPTY);
         } else {
            out.addAll(this.font.split(Component.literal(line), Math.min(260, this.width - 24)));
         }
      }

      return out;
   }

   private static String labelOf(StreamsConfigScreen.Row row, String value) {
      SettingsCatalog.Entry entry = row.entry;
      switch (entry.kind) {
         case TOGGLE:
            return "true".equalsIgnoreCase(value.trim()) ? "Вкл" : "Выкл";
         case CHOICE:
            for (Entry<String, String> option : entry.choices.entrySet()) {
               if (same(option.getKey(), value)) {
                  return option.getValue();
               }
            }

            return value;
         case SLIDER:
            double v = number(value, Double.NaN);
            return Double.isNaN(v) ? value : entry.show(v);
         default:
            return value.isEmpty() ? "(пусто)" : value;
      }
   }

   private StreamsConfigScreen.Row hovered(int mouseX, int mouseY) {
      for (Object[] at : this.shown) {
         int y = (Integer)at[0];
         if (at[1] != this.presetRow && mouseY >= y && mouseY < y + 20 && mouseX >= this.listX && mouseX < this.listX + this.listWidth - 40 - 2) {
            this.tipRowTop = y;
            return (StreamsConfigScreen.Row)at[1];
         }
      }

      return null;
   }

   private String status() {
      if (!this.query.trim().isEmpty() && this.shown.isEmpty()) {
         return "Ни один параметр не найден.";
      } else {
         return this.pending.isEmpty() ? "Изменения сохраняются автоматически." : "Сохранение...";
      }
   }

   private int keyWidth() {
      int w = 0;

      for (SettingsCatalog.Cost cost : SettingsCatalog.Cost.values()) {
         w += 7 + this.font.width(cost.key) + 10;
      }

      return w - 10;
   }

   public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gfx);
      super.render(gfx, mouseX, mouseY, partialTick);
      gfx.drawCenteredString(this.font, this.title, this.width / 2, 6, -1);

      for (Object[] at : this.shown) {
         StreamsConfigScreen.Row row = (StreamsConfigScreen.Row)at[1];
         int rowY = (Integer)at[0];
         if (row != this.presetRow) {
            gfx.fill(this.listX, rowY + 5, this.listX + 4, rowY + 14, row.entry.cost.colour);
         }

         gfx.drawString(this.font, row.entry.name, this.listX + 8, rowY + 6, same(this.current(row), row.fallback) ? -1 : -11393);
      }

      for (Object[] at : this.headings) {
         gfx.drawCenteredString(this.font, (String)at[1], this.width / 2, (Integer)at[0] + 8, -6250336);
      }

      String status = this.status();
      gfx.drawString(this.font, status, this.listX + this.listWidth - this.font.width(status), 6, -6250336);
      int keyX = this.width / 2 - this.keyWidth() / 2;
      int keyY = this.height - 40;

      for (SettingsCatalog.Cost cost : SettingsCatalog.Cost.values()) {
         gfx.fill(keyX, keyY - 1, keyX + 4, keyY + 8, cost.colour);
         gfx.drawString(this.font, cost.key, keyX + 4 + 3, keyY, -6250336);
         keyX += 7 + this.font.width(cost.key) + 10;
      }

      if (this.pageCount > 1) {
         gfx.drawString(this.font, this.page + 1 + "/" + this.pageCount, this.width / 2 + 128, this.height - 20, -11393);
      }

      StreamsConfigScreen.Row over = this.hovered(mouseX, mouseY);
      if (over != null) {
         gfx.renderTooltip(this.font, this.describe(over), this.tipPlace, this.listX, mouseY);
      }
   }

   public void onClose() {
      this.save();
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private final class PercentSlider extends AbstractSliderButton {
      private final StreamsConfigScreen.Row row;
      private final double low;
      private final double high;

      PercentSlider(int x, int y, int width, StreamsConfigScreen.Row row, double current) {
         super(x, y, width, 20, Component.empty(), 0.0);
         this.row = row;
         double shown = row.entry.shownOf(current);
         this.low = Math.min(row.entry.min, shown);
         this.high = Math.max(row.entry.max, shown);
         this.value = this.high <= this.low ? 0.0 : (shown - this.low) / (this.high - this.low);
         this.updateMessage();
      }

      private double amount() {
         return Math.round((this.low + this.value * (this.high - this.low)) * 100.0) / 100.0;
      }

      protected void updateMessage() {
         this.setMessage(Component.literal(Math.round(this.amount() * 100.0) + "%"));
      }

      protected void applyValue() {
         StreamsConfigScreen.this.edit(this.row, this.row.entry.storeShown(this.amount()));
      }
   }

   private static final class Row {
      String key;
      String value;
      boolean client;
      String doc;
      String fallback;
      SettingsCatalog.Entry entry;
   }

   private final class SnapSlider extends AbstractSliderButton {
      private final StreamsConfigScreen.Row row;
      private final double[] stops;

      SnapSlider(int x, int y, int width, StreamsConfigScreen.Row row, double[] stops, double current) {
         super(x, y, width, 20, Component.empty(), 0.0);
         this.row = row;
         this.stops = stops;
         int nearest = 0;

         for (int i = 1; i < stops.length; i++) {
            if (Math.abs(stops[i] - current) < Math.abs(stops[nearest] - current)) {
               nearest = i;
            }
         }

         this.value = stops.length < 2 ? 0.0 : (double)nearest / (stops.length - 1);
         this.updateMessage();
      }

      private int index() {
         return this.stops.length < 2 ? 0 : Math.max(0, Math.min(this.stops.length - 1, (int)Math.round(this.value * (this.stops.length - 1))));
      }

      protected void updateMessage() {
         this.setMessage(Component.literal(this.row.entry.show(this.stops[this.index()])));
      }

      protected void applyValue() {
         int i = this.index();
         this.value = this.stops.length < 2 ? 0.0 : (double)i / (this.stops.length - 1);
         StreamsConfigScreen.this.edit(this.row, this.row.entry.store(this.stops[i]));
      }
   }
}
