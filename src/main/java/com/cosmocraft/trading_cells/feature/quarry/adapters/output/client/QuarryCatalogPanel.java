package com.cosmocraft.trading_cells.feature.quarry.adapters.output.client;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMaterialCatalog;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.network.QuarryCatalogSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.RequestQuarryCatalogPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Contextual live-yield panel shared by the two normal Quarry screens. */
final class QuarryCatalogPanel {
    static final int X = 123;
    static final int Y = 63;
    static final int WIDTH = 220;
    private static final int HEADER_HEIGHT = 17;
    private static final int STATUS_HEIGHT = 12;
    private static final int ROW_HEIGHT = 17;
    private static final int VISIBLE_ROWS = 6;
    private static final int HEIGHT = HEADER_HEIGHT + STATUS_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 3;
    private static final int PADDING = 4;
    private static final int RIGHT_TEXT_WIDTH = 82;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFB8B8B8;
    private static final int TEXT_DISABLED = 0xFF999999;

    private boolean open;
    private int scroll;
    private ItemStack requestedPickaxe = ItemStack.EMPTY;
    private ItemStack requestedUpgrade = ItemStack.EMPTY;
    private boolean requestedDeepMining;
    private int requestedCatalogRevision = -1;
    private List<QuarryCatalogSyncPayload.Entry> cachedSource = List.of();
    private List<QuarryCatalogSyncPayload.Entry> activeEntries = List.of();

    boolean isOpen() {
        return open;
    }

    void toggle(QuarryMenu menu) {
        open = !open;
        scroll = 0;
        if (open) {
            request(menu);
            refreshActiveEntries(menu);
        }
    }

    void close() {
        open = false;
    }

    void tick(QuarryMenu menu) {
        if (!open) {
            return;
        }
        refreshActiveEntries(menu);
        scroll = Math.clamp(scroll, 0, maximumScroll());
        if (!ItemStack.isSameItemSameComponents(requestedPickaxe, menu.pickaxe())
                || !ItemStack.isSameItemSameComponents(requestedUpgrade, menu.upgrade())
                || requestedDeepMining != menu.deepMining()
                || requestedCatalogRevision != menu.catalogRevision()) {
            request(menu);
        }
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollY,
            int left,
            int top,
            QuarryMenu menu
    ) {
        if (!open || !contains(mouseX, mouseY, left, top)) {
            return false;
        }
        refreshActiveEntries(menu);
        scroll = Mth.clamp((int) (scroll - scrollY), 0, maximumScroll());
        return true;
    }

    boolean contains(double mouseX, double mouseY, int left, int top) {
        return inside(mouseX, mouseY, left + X, top + Y, WIDTH, HEIGHT);
    }

    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            QuarryMenu menu,
            int left,
            int top,
            int mouseX,
            int mouseY,
            int frameDark,
            int frameLight
    ) {
        refreshActiveEntries(menu);
        int panelX = left + X;
        int panelY = top + Y;
        graphics.fill(panelX, panelY, panelX + WIDTH, panelY + HEIGHT, 0xFF303436);
        graphics.fill(panelX + 1, panelY + 1, panelX + WIDTH - 1, panelY + HEIGHT - 1, 0xF044494C);
        graphics.fill(panelX + 2, panelY + 2, panelX + WIDTH - 2, panelY + HEADER_HEIGHT, 0xFF5B6164);
        FittedTextRenderer.centered(
                graphics,
                font,
                Component.translatable("gui.trading_cells.quarry_catalog"),
                panelX + 28,
                panelX + WIDTH - 28,
                panelY + 2,
                HEADER_HEIGHT - 2,
                TEXT_WHITE,
                false
        );

        Component status = menu.catalogEntries().isEmpty()
                ? Component.translatable("gui.trading_cells.quarry_catalog_loading")
                : Component.translatable("gui.trading_cells.quarry_help.available", activeEntries.size());
        FittedTextRenderer.centered(
                graphics,
                font,
                status,
                panelX + PADDING,
                panelX + WIDTH - PADDING,
                panelY + HEADER_HEIGHT,
                STATUS_HEIGHT,
                TEXT_MUTED,
                false
        );

        int firstRowY = panelY + HEADER_HEIGHT + STATUS_HEIGHT;
        int visible = Math.min(VISIBLE_ROWS, activeEntries.size() - scroll);
        for (int row = 0; row < visible; row++) {
            QuarryCatalogSyncPayload.Entry entry = activeEntries.get(scroll + row);
            drawRow(graphics, font, menu, entry, panelX, firstRowY + row * ROW_HEIGHT, mouseX, mouseY);
        }
        if (!menu.catalogEntries().isEmpty() && activeEntries.isEmpty()) {
            FittedTextRenderer.centered(
                    graphics,
                    font,
                    Component.translatable("gui.trading_cells.quarry_help.no_results"),
                    panelX + PADDING,
                    panelX + WIDTH - PADDING,
                    firstRowY + 10,
                    ROW_HEIGHT,
                    TEXT_DISABLED,
                    false
            );
        }
        drawScrollbar(graphics, panelX, firstRowY, frameDark, frameLight);
    }

    private void request(QuarryMenu menu) {
        requestedPickaxe = menu.pickaxe().copy();
        requestedUpgrade = menu.upgrade().copy();
        requestedDeepMining = menu.deepMining();
        requestedCatalogRevision = menu.catalogRevision();
        ClientPacketDistributor.sendToServer(new RequestQuarryCatalogPayload(menu.containerId));
    }

    private void refreshActiveEntries(QuarryMenu menu) {
        List<QuarryCatalogSyncPayload.Entry> source = menu.catalogEntries();
        if (source == cachedSource) {
            return;
        }
        cachedSource = source;
        activeEntries = source.stream()
                .filter(entry -> entry.blockedReason() == QuarryMaterialCatalog.BlockedReason.NONE.ordinal())
                .toList();
        scroll = Math.clamp(scroll, 0, maximumScroll());
    }

    private static void drawRow(
            GuiGraphicsExtractor graphics,
            Font font,
            QuarryMenu menu,
            QuarryCatalogSyncPayload.Entry entry,
            int panelX,
            int rowY,
            int mouseX,
            int mouseY
    ) {
        int rowX = panelX + PADDING;
        int rowWidth = WIDTH - PADDING * 2 - 3;
        boolean hovered = inside(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT - 1);
        graphics.fill(rowX, rowY, rowX + rowWidth, rowY + ROW_HEIGHT - 1,
                hovered ? 0xFF5B6164 : 0xFF383D40);
        graphics.fakeItem(entry.preview(), rowX + 1, rowY);
        int rightStart = rowX + rowWidth - RIGHT_TEXT_WIDTH;
        FittedTextRenderer.left(
                graphics,
                font,
                entry.preview().getHoverName(),
                rowX + 20,
                rightStart - 2,
                rowY,
                ROW_HEIGHT - 1,
                TEXT_WHITE,
                false
        );
        FittedTextRenderer.centered(
                graphics,
                font,
                Component.literal(amount(entry.minimumAmount(), entry.maximumAmount())),
                rightStart,
                rightStart + 34,
                rowY,
                ROW_HEIGHT - 1,
                TEXT_WHITE,
                false
        );
        FittedTextRenderer.centered(
                graphics,
                font,
                Component.literal(probability(entry.probabilityPartsPerMillion())),
                rightStart + 34,
                rowX + rowWidth - 1,
                rowY,
                ROW_HEIGHT - 1,
                TEXT_WHITE,
                false
        );
        if (hovered) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    tooltip(menu, entry),
                    mouseX,
                    mouseY,
                    entry.preview()
            );
        }
    }

    private static List<Component> tooltip(QuarryMenu menu, QuarryCatalogSyncPayload.Entry entry) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(entry.preview().getHoverName());
        tooltip.add(Component.translatable(
                "gui.trading_cells.skeleton_help.chance",
                probability(entry.probabilityPartsPerMillion())
        ).withStyle(ChatFormatting.GRAY));
        Component amount = entry.minimumAmount() == entry.maximumAmount()
                ? Component.translatable(
                        "gui.trading_cells.skeleton_help.amount_exact",
                        entry.maximumAmount()
                )
                : Component.translatable(
                        "gui.trading_cells.skeleton_help.amount_range",
                        entry.minimumAmount(),
                        entry.maximumAmount()
                );
        tooltip.add(amount.copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.trading_cells.quarry_source", entry.sourceMod())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.trading_cells.quarry_minimum_upgrade",
                Component.translatable(upgradeTranslation(entry.minimumUpgrade()))
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.trading_cells.quarry_minimum_pickaxe",
                formatSpeed(entry.minimumPickaxeSpeed())
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.trading_cells.quarry_normal_result",
                entry.normalResult().getHoverName()
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.trading_cells.quarry_silk_result",
                entry.silkResult().getHoverName()
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(entry.fortuneCompatible()
                ? "tooltip.trading_cells.quarry_fortune_yes"
                : "tooltip.trading_cells.quarry_fortune_no").withStyle(ChatFormatting.GRAY));
        if (menu.catalogDeepMining() && entry.defaultDeepFallback()) {
            tooltip.add(Component.translatable("tooltip.trading_cells.deep_variant_unavailable")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.trading_cells.default_ore_used")
                    .withStyle(ChatFormatting.YELLOW));
        } else if (entry.deepVariantAvailable()) {
            tooltip.add(Component.translatable(
                    "tooltip.trading_cells.quarry_deep_result",
                    entry.deepResult().getHoverName()
            ).withStyle(ChatFormatting.GRAY));
        }
        return List.copyOf(tooltip);
    }

    private void drawScrollbar(
            GuiGraphicsExtractor graphics,
            int panelX,
            int firstRowY,
            int frameDark,
            int frameLight
    ) {
        int maximum = maximumScroll();
        if (maximum == 0) {
            return;
        }
        int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 1;
        int trackX = panelX + WIDTH - PADDING - 2;
        int thumbHeight = Math.max(8, trackHeight * VISIBLE_ROWS / activeEntries.size());
        int thumbY = firstRowY + (trackHeight - thumbHeight) * scroll / maximum;
        graphics.fill(trackX, firstRowY, trackX + 2, firstRowY + trackHeight, frameDark);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, frameLight);
    }

    private int maximumScroll() {
        return Math.max(0, activeEntries.size() - VISIBLE_ROWS);
    }

    private static String probability(int partsPerMillion) {
        int value = Math.max(0, partsPerMillion);
        double percent = value / 10_000.0D;
        if (value < 1_000) {
            return String.format(Locale.ROOT, "%.4f%%", percent);
        }
        if (value < 10_000) {
            return String.format(Locale.ROOT, "%.3f%%", percent);
        }
        return String.format(Locale.ROOT, "%.2f%%", percent);
    }

    private static String amount(int minimum, int maximum) {
        return minimum == maximum ? "x" + maximum : "x" + minimum + '-' + maximum;
    }

    private static String formatSpeed(double speed) {
        return String.format(Locale.ROOT, "%.1f", speed);
    }

    private static String upgradeTranslation(int index) {
        QuarryUpgradeTier tier = QuarryUpgradeTier.fromIndex(index);
        return "upgrade.trading_cells.quarry." + tier.name().toLowerCase(Locale.ROOT);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
