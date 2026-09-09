package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmMenu;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmDropRules;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Contextual live-yield panel owned by the normal mob-farm screen, never by REI. */
final class ConfiguredMobFarmLootHelpPanel {
    static final int BUTTON_X = 134;
    static final int BUTTON_Y = 66;
    static final int BUTTON_SIZE = 12;
    private static final int PANEL_X = 123;
    private static final int PANEL_Y = 63;
    private static final int PANEL_WIDTH = 220;
    private static final int HEADER_HEIGHT = 17;
    private static final int STATUS_HEIGHT = 12;
    private static final int ROW_HEIGHT = 17;
    private static final int VISIBLE_ROWS = 6;
    private static final int PANEL_HEIGHT = HEADER_HEIGHT + STATUS_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 3;
    private static final int PANEL_PADDING = 4;
    private static final int RIGHT_TEXT_WIDTH = 82;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFB8B8B8;
    private static final int TEXT_DISABLED = 0xFF999999;

    private boolean open;
    private int scroll;
    private ConfiguredMobFarmKind cachedKind;
    private int cachedLooting = Integer.MIN_VALUE;
    private int cachedKills = Integer.MIN_VALUE;
    private boolean cachedEnabled;
    private boolean cachedWorker;
    private boolean cachedSword;
    private int cachedDynamicState = Integer.MIN_VALUE;
    private List<Entry> entries = List.of();
    private Component status = Component.empty();

    boolean isOpen() {
        return open;
    }

    void tick(ConfiguredMobFarmMenu menu) {
        refresh(menu);
        scroll = Math.clamp(scroll, 0, maximumScroll());
    }

    void toggle() {
        open = !open;
        scroll = 0;
    }

    void drawPanel(
            GuiGraphicsExtractor graphics,
            Font font,
            ConfiguredMobFarmMenu menu,
            int left,
            int top,
            int mouseX,
            int mouseY,
            ConfiguredMobFarmGuiThemeColors colors
    ) {
        refresh(menu);
        int x = left + PANEL_X;
        int y = top + PANEL_Y;
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF303436);
        graphics.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, 0xF044494C);
        graphics.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + HEADER_HEIGHT, 0xFF5B6164);
        ConfiguredMobFarmTextRenderer.centered(
                graphics,
                font,
                Component.translatable("gui.trading_cells.raider_help.title"),
                x + 28,
                x + PANEL_WIDTH - 28,
                y + 2,
                HEADER_HEIGHT - 2,
                TEXT_WHITE,
                false
        );
        ConfiguredMobFarmTextRenderer.centered(
                graphics,
                font,
                status,
                x + PANEL_PADDING,
                x + PANEL_WIDTH - PANEL_PADDING,
                y + HEADER_HEIGHT,
                STATUS_HEIGHT,
                TEXT_MUTED,
                false
        );

        int firstRowY = y + HEADER_HEIGHT + STATUS_HEIGHT;
        int visible = Math.min(VISIBLE_ROWS, entries.size() - scroll);
        for (int row = 0; row < visible; row++) {
            drawRow(graphics, font, entries.get(scroll + row), x, firstRowY + row * ROW_HEIGHT, mouseX, mouseY);
        }
        if (entries.isEmpty()) {
            ConfiguredMobFarmTextRenderer.centered(
                    graphics,
                    font,
                    Component.translatable("gui.trading_cells.raider_help.no_loot"),
                    x + PANEL_PADDING,
                    x + PANEL_WIDTH - PANEL_PADDING,
                    firstRowY + 10,
                    ROW_HEIGHT,
                    TEXT_DISABLED,
                    false
            );
        }
        drawScrollbar(graphics, colors, x, firstRowY);
    }

    boolean consumeOverlayClick(double mouseX, double mouseY, int left, int top) {
        if (inside(mouseX, mouseY, left + PANEL_X, top + PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT)) {
            return true;
        }
        open = false;
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int left, int top) {
        if (!open || !inside(mouseX, mouseY, left + PANEL_X, top + PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT)) {
            return false;
        }
        scroll = Mth.clamp((int) (scroll - scrollY), 0, maximumScroll());
        return true;
    }

    private void refresh(ConfiguredMobFarmMenu menu) {
        ConfiguredMobFarmKind kind = menu.selectedKind();
        int looting = menu.lootingLevel();
        int kills = menu.simulatedKills();
        boolean enabled = menu.isEnabled();
        boolean worker = menu.hasWorker();
        boolean sword = menu.hasSword();
        int dynamicState = dynamicState(menu);
        if (kind == cachedKind
                && looting == cachedLooting
                && kills == cachedKills
                && enabled == cachedEnabled
                && worker == cachedWorker
                && sword == cachedSword
                && dynamicState == cachedDynamicState) {
            return;
        }
        cachedKind = kind;
        cachedLooting = looting;
        cachedKills = kills;
        cachedEnabled = enabled;
        cachedWorker = worker;
        cachedSword = sword;
        cachedDynamicState = dynamicState;

        boolean previewAvailable = sword;
        List<Entry> refreshed = new ArrayList<>();
        for (ItemStack stack : menu.dynamicLootOptions()) {
            if (menu.isDynamicLootEnabled(stack)) {
                var knownDrop = menu.previewDrop(stack).or(() -> ConfiguredMobFarmLootAdapter.currentDynamicCycleDrop(
                        menu.selectedTargetId(),
                        stack,
                        looting,
                        kills
                ));
                if (knownDrop.isPresent() && previewAvailable) {
                    ConfiguredMobFarmDropRules.BaseDrop drop = knownDrop.orElseThrow();
                    refreshed.add(new Entry(
                            stack,
                            drop.probabilityPartsPerMillion(),
                            drop.minimumAmount(),
                            drop.maximumAmount(),
                            false
                    ));
                } else {
                    refreshed.add(new Entry(stack, 0, 0, 0, true));
                }
            }
        }
        entries = List.copyOf(refreshed);
        status = status(enabled, worker, sword, looting, kills, entries.isEmpty());
        scroll = Math.clamp(scroll, 0, maximumScroll());
    }

    private static int dynamicState(ConfiguredMobFarmMenu menu) {
        int result = 31 * menu.selectedTargetId().hashCode() + menu.lootPreviewRevision();
        for (ItemStack stack : menu.dynamicLootOptions()) {
            result = 31 * result + stack.getItem().hashCode();
            result = 31 * result + Boolean.hashCode(menu.isDynamicLootEnabled(stack));
        }
        return result;
    }

    private static Component status(
            boolean enabled,
            boolean worker,
            boolean sword,
            int looting,
            int kills,
            boolean empty
    ) {
        if (!enabled) {
            return Component.translatable("gui.trading_cells.raider_help.paused");
        }
        if (!worker) {
            return Component.translatable("gui.trading_cells.raider_help.missing_villager");
        }
        if (!sword) {
            return Component.translatable("gui.trading_cells.raider_help.missing_sword");
        }
        if (empty) {
            return Component.translatable("gui.trading_cells.raider_help.no_loot");
        }
        return Component.translatable("gui.trading_cells.raider_help.stats", looting, kills);
    }

    private static void drawRow(
            GuiGraphicsExtractor graphics,
            Font font,
            Entry entry,
            int panelX,
            int rowY,
            int mouseX,
            int mouseY
    ) {
        int rowX = panelX + PANEL_PADDING;
        int rowWidth = PANEL_WIDTH - PANEL_PADDING * 2 - 3;
        boolean hovered = inside(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT - 1);
        graphics.fill(rowX, rowY, rowX + rowWidth, rowY + ROW_HEIGHT - 1, hovered ? 0xFF5B6164 : 0xFF383D40);
        graphics.fakeItem(entry.stack(), rowX + 1, rowY);
        int rightStart = rowX + rowWidth - RIGHT_TEXT_WIDTH;
        ConfiguredMobFarmTextRenderer.left(
                graphics,
                font,
                entry.dynamic()
                        ? entry.stack().getHoverName()
                        : entry.stack().getHoverName(),
                rowX + 20,
                rightStart - 2,
                rowY,
                ROW_HEIGHT - 1,
                entry.maximumAmount() == 0 ? TEXT_DISABLED : TEXT_WHITE,
                false
        );
        ConfiguredMobFarmTextRenderer.centered(
                graphics,
                font,
                entry.dynamic()
                        ? Component.literal("?")
                        : Component.literal(amount(entry.minimumAmount(), entry.maximumAmount())),
                rightStart,
                rightStart + 34,
                rowY,
                ROW_HEIGHT - 1,
                TEXT_WHITE,
                false
        );
        Component probability = entry.dynamic()
                ? Component.literal("?")
                : percentage(entry.probabilityPartsPerMillion());
        ConfiguredMobFarmTextRenderer.centered(
                graphics,
                font,
                probability,
                rightStart + 34,
                rowX + rowWidth - 1,
                rowY,
                ROW_HEIGHT - 1,
                TEXT_WHITE,
                false
        );
        if (hovered) {
            Component amountTooltip = entry.minimumAmount() == entry.maximumAmount()
                    ? Component.translatable(
                            "gui.trading_cells.raider_help.amount_exact",
                            entry.maximumAmount()
                    )
                    : Component.translatable(
                            "gui.trading_cells.raider_help.amount_range",
                            entry.minimumAmount(),
                            entry.maximumAmount()
                    );
            List<Component> tooltip = entry.dynamic()
                    ? List.of(
                            entry.stack().getHoverName(),
                            Component.translatable("gui.trading_cells.raider_help.dynamic")
                    )
                    : List.of(
                            entry.stack().getHoverName(),
                            Component.translatable("gui.trading_cells.raider_help.chance", probability),
                            amountTooltip
                    );
            graphics.setComponentTooltipForNextFrame(
                    font,
                    tooltip,
                    mouseX,
                    mouseY,
                    entry.stack()
            );
        }
    }

    private void drawScrollbar(
            GuiGraphicsExtractor graphics,
            ConfiguredMobFarmGuiThemeColors colors,
            int panelX,
            int firstRowY
    ) {
        int maximum = maximumScroll();
        if (maximum == 0) {
            return;
        }
        int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 1;
        int trackX = panelX + PANEL_WIDTH - PANEL_PADDING - 2;
        int thumbHeight = Math.max(8, trackHeight * VISIBLE_ROWS / entries.size());
        int thumbY = firstRowY + (trackHeight - thumbHeight) * scroll / maximum;
        graphics.fill(trackX, firstRowY, trackX + 2, firstRowY + trackHeight, colors.scrollTrack());
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, colors.scrollThumbLight());
    }

    private int maximumScroll() {
        return Math.max(0, entries.size() - VISIBLE_ROWS);
    }

    private static Component percentage(int partsPerMillion) {
        int hundredths = (int) Math.round(partsPerMillion / 100.0D);
        return Component.translatable(
                "rei.trading_cells.percentage",
                hundredths / 100,
                String.format(Locale.ROOT, "%02d", hundredths % 100)
        );
    }

    private static String amount(int minimum, int maximum) {
        if (maximum == 0) {
            return "x0";
        }
        return minimum == maximum ? "x" + maximum : "x" + minimum + '-' + maximum;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Entry(
            ItemStack stack,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount,
            boolean dynamic
    ) {
        private Entry {
            stack = stack.copy();
        }
    }
}
