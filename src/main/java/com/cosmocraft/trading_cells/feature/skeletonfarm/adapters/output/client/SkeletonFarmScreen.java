package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerGuiTextures;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerGuiThemeColors;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class SkeletonFarmScreen extends AbstractContainerScreen<SkeletonFarmMenu> {
    public static final int RECIPE_VIEWER_X = 194;
    public static final int RECIPE_VIEWER_Y = 35;
    public static final int RECIPE_VIEWER_WIDTH = 64;
    public static final int RECIPE_VIEWER_HEIGHT = 13;
    private static final VillagerGuiThemeColors COLORS = VillagerGuiThemeColors.resolve();
    private static final int MACHINE_PANEL_X = 123;
    private static final int MACHINE_PANEL_Y = 26;
    private static final int MACHINE_PANEL_WIDTH = 220;
    private static final int MACHINE_PANEL_HEIGHT = 77;
    private static final int SELECTOR_X = 10;
    private static final int SELECTOR_Y = 29;
    private static final int SELECTOR_WIDTH = 103;
    private static final int SELECTOR_HEIGHT = 18;
    private static final int KIND_LIST_X = SELECTOR_X;
    private static final int KIND_LIST_Y = SELECTOR_Y + SELECTOR_HEIGHT + 1;
    private static final int KIND_ROW_HEIGHT = 18;
    private static final int VISIBLE_KINDS = 4;
    private static final int FILTER_X = 10;
    private static final int FILTER_Y = 64;
    private static final int FILTER_WIDTH = 103;
    private static final int FILTER_ROW_HEIGHT = 20;
    private static final int VISIBLE_FILTERS = 3;
    private static final int PROGRESS_X = RECIPE_VIEWER_X;
    private static final int PROGRESS_Y = RECIPE_VIEWER_Y;
    private static final int PROGRESS_WIDTH = RECIPE_VIEWER_WIDTH;
    private static final int PROGRESS_HEIGHT = RECIPE_VIEWER_HEIGHT;
    private static final int XP_X = 265;
    private static final int XP_Y = 29;
    private static final int XP_WIDTH = 73;
    private static final int XP_HEIGHT = 32;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_DARK = 0xFF252525;
    private static final int TEXT_XP = 0xFF55FF00;
    private boolean kindListOpen;
    private int kindScroll;
    private int lootScroll;

    public SkeletonFarmScreen(SkeletonFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SkeletonFarmMenu.WIDTH, SkeletonFarmMenu.HEIGHT);
        titleLabelY = VillagerTradeScreenLayout.HEADER_TEXT_Y;
        inventoryLabelX = VillagerTradeScreenLayout.INVENTORY_LABEL_X;
        inventoryLabelY = VillagerTradeScreenLayout.INVENTORY_LABEL_Y;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int maximum = Math.max(0, menu.selectedKind().availableLoot().size() - VISIBLE_FILTERS);
        lootScroll = Math.clamp(lootScroll, 0, maximum);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        VillagerTradeScreenLayout.drawBackground(
                graphics,
                VillagerGuiTextures.resolve(),
                leftPos,
                topPos
        );
        drawMachinePanel(graphics);
        drawInventorySlots(graphics);
        drawMachineSlots(graphics);
        drawProgress(graphics);
        drawExperience(graphics, mouseX, mouseY);
        drawLootFilters(graphics);
        drawKindSelector(graphics);
        if (kindListOpen) {
            drawKindList(graphics);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.centeredText(
                font,
                Component.translatable("gui.trading_cells.skeleton_type"),
                VillagerTradeScreenLayout.TRADES_TITLE_CENTER_X,
                VillagerTradeScreenLayout.HEADER_TEXT_Y,
                TEXT_DARK
        );
        graphics.centeredText(
                font,
                title,
                VillagerTradeScreenLayout.PROFESSION_TITLE_CENTER_X,
                VillagerTradeScreenLayout.HEADER_TEXT_Y,
                TEXT_DARK
        );
        graphics.text(
                font,
                playerInventoryTitle,
                VillagerTradeScreenLayout.INVENTORY_LABEL_X,
                VillagerTradeScreenLayout.INVENTORY_LABEL_Y,
                TEXT_DARK,
                false
        );
        graphics.text(
                font,
                Component.translatable("gui.trading_cells.skeleton_loot"),
                FILTER_X,
                FILTER_Y - 11,
                TEXT_DARK,
                false
        );
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (kindListOpen && inside(x, y, KIND_LIST_X, KIND_LIST_Y, SELECTOR_WIDTH, VISIBLE_KINDS * KIND_ROW_HEIGHT)) {
            kindScroll = Mth.clamp(
                    (int) (kindScroll - scrollY),
                    0,
                    SkeletonFarmKind.values().length - VISIBLE_KINDS
            );
            return true;
        }
        if (!kindListOpen && inside(x, y, FILTER_X, FILTER_Y, FILTER_WIDTH, VISIBLE_FILTERS * FILTER_ROW_HEIGHT)) {
            int maximum = Math.max(0, menu.selectedKind().availableLoot().size() - VISIBLE_FILTERS);
            lootScroll = Mth.clamp((int) (lootScroll - scrollY), 0, maximum);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();
        if (inside(x, y, SELECTOR_X, SELECTOR_Y, SELECTOR_WIDTH, SELECTOR_HEIGHT)) {
            kindListOpen = !kindListOpen;
            if (kindListOpen) {
                kindScroll = Mth.clamp(
                        menu.selectedKind().ordinal() - 1,
                        0,
                        SkeletonFarmKind.values().length - VISIBLE_KINDS
                );
            }
            return true;
        }
        if (kindListOpen) {
            if (inside(x, y, KIND_LIST_X, KIND_LIST_Y, SELECTOR_WIDTH, VISIBLE_KINDS * KIND_ROW_HEIGHT)) {
                int row = (int) (y - topPos - KIND_LIST_Y) / KIND_ROW_HEIGHT;
                int selected = kindScroll + row;
                if (selected < SkeletonFarmKind.values().length) {
                    sendButton(SkeletonFarmMenu.SELECT_KIND_BUTTON_BASE + selected);
                    lootScroll = 0;
                }
                kindListOpen = false;
                return true;
            }
            kindListOpen = false;
        }
        if (inside(x, y, FILTER_X, FILTER_Y, FILTER_WIDTH, VISIBLE_FILTERS * FILTER_ROW_HEIGHT)) {
            int row = (int) (y - topPos - FILTER_Y) / FILTER_ROW_HEIGHT;
            int index = lootScroll + row;
            if (index < menu.selectedKind().availableLoot().size()) {
                SkeletonFarmLoot loot = menu.selectedKind().availableLoot().get(index);
                sendButton(SkeletonFarmMenu.TOGGLE_LOOT_BUTTON_BASE + loot.ordinal());
            }
            return true;
        }
        if (inside(x, y, XP_X, XP_Y, XP_WIDTH, XP_HEIGHT)) {
            sendButton(SkeletonFarmMenu.EXTRACT_EXPERIENCE_BUTTON);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawMachinePanel(GuiGraphicsExtractor graphics) {
        int x = leftPos + MACHINE_PANEL_X;
        int y = topPos + MACHINE_PANEL_Y;
        graphics.fill(x, y, x + MACHINE_PANEL_WIDTH, y + MACHINE_PANEL_HEIGHT, 0xFF4A4E50);
        graphics.fill(x + 1, y + 1, x + MACHINE_PANEL_WIDTH - 1, y + MACHINE_PANEL_HEIGHT - 1, 0xFFC3C7C8);
    }

    private void drawInventorySlots(GuiGraphicsExtractor graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                SlotRenderer.drawAtItemPosition(
                        graphics,
                        leftPos,
                        topPos,
                        SkeletonFarmMenu.PLAYER_INVENTORY_X + column * 18,
                        SkeletonFarmMenu.PLAYER_INVENTORY_Y + row * 18,
                        COLORS.slotPalette()
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            SlotRenderer.drawAtItemPosition(
                    graphics,
                    leftPos,
                    topPos,
                    SkeletonFarmMenu.PLAYER_INVENTORY_X + column * 18,
                    SkeletonFarmMenu.PLAYER_HOTBAR_Y,
                    COLORS.slotPalette()
            );
        }
        int[] equipmentY = {
                VillagerTradeMenuLayout.EQUIPMENT_HEAD_Y,
                VillagerTradeMenuLayout.EQUIPMENT_CHEST_Y,
                VillagerTradeMenuLayout.EQUIPMENT_LEGS_Y,
                VillagerTradeMenuLayout.EQUIPMENT_FEET_Y,
                VillagerTradeMenuLayout.EQUIPMENT_OFFHAND_Y
        };
        for (int frameY : equipmentY) {
            VillagerTradeScreenLayout.drawSlotAtFramePosition(
                    graphics,
                    leftPos,
                    topPos,
                    VillagerTradeMenuLayout.EQUIPMENT_X,
                    frameY,
                    COLORS
            );
        }
    }

    private void drawMachineSlots(GuiGraphicsExtractor graphics) {
        drawItemSlot(graphics, SkeletonFarmMenu.WORKER_SLOT_X, SkeletonFarmMenu.INPUT_SLOT_Y);
        drawItemSlot(graphics, SkeletonFarmMenu.SWORD_SLOT_X, SkeletonFarmMenu.INPUT_SLOT_Y);
        if (!menu.getSlot(SkeletonFarmBlockEntity.WORKER_SLOT).hasItem()) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    MachineSlotSprites.CAPTURER,
                    leftPos + SkeletonFarmMenu.WORKER_SLOT_X,
                    topPos + SkeletonFarmMenu.INPUT_SLOT_Y,
                    16,
                    16
            );
        }
        for (int index = 0; index < SkeletonFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            drawItemSlot(graphics, SkeletonFarmMenu.outputSlotX(index), SkeletonFarmMenu.outputSlotY(index));
        }
    }

    private void drawItemSlot(GuiGraphicsExtractor graphics, int itemX, int itemY) {
        SlotRenderer.drawAtItemPosition(
                graphics,
                leftPos,
                topPos,
                itemX,
                itemY,
                COLORS.slotPalette()
        );
    }

    private void drawProgress(GuiGraphicsExtractor graphics) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        graphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, 0xFF303436);
        graphics.fill(x + 1, y + 1, x + PROGRESS_WIDTH - 1, y + PROGRESS_HEIGHT - 1, 0xFF8A9093);
        graphics.fill(x + 2, y + 2, x + PROGRESS_WIDTH - 2, y + PROGRESS_HEIGHT - 2, 0xFF555B5E);
        int fill = Math.min(
                PROGRESS_WIDTH - 4,
                menu.cycleTicks() * (PROGRESS_WIDTH - 4) / menu.maxCycleTicks()
        );
        if (fill > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fill, y + PROGRESS_HEIGHT - 2, 0xFF9EB7D1);
        }
        String time = MachineScreenUtil.remainingTime(menu.cycleTicks(), menu.maxCycleTicks());
        graphics.text(font, time, x + PROGRESS_WIDTH / 2 - font.width(time) / 2, y + 3, TEXT_WHITE, true);
        graphics.text(
                font,
                "x" + menu.simulatedKills(),
                leftPos + 180,
                topPos + 37,
                TEXT_DARK,
                false
        );
    }

    private void drawExperience(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + XP_X;
        int y = topPos + XP_Y;
        graphics.fill(x, y, x + XP_WIDTH, y + XP_HEIGHT, 0xFF34383B);
        graphics.fill(x + 1, y + 1, x + XP_WIDTH - 1, y + XP_HEIGHT - 1, 0xFF202426);
        graphics.centeredText(font, Component.literal("XP"), x + XP_WIDTH / 2, y + 3, TEXT_WHITE);
        graphics.centeredText(font, compactExperience(menu.storedExperience()), x + XP_WIDTH / 2, y + 13, TEXT_XP);
        graphics.centeredText(
                font,
                Component.translatable("gui.trading_cells.level_equivalent", menu.storedLevels()),
                x + XP_WIDTH / 2,
                y + 22,
                TEXT_XP
        );
        if (inside(mouseX, mouseY, XP_X, XP_Y, XP_WIDTH, XP_HEIGHT)) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("button.trading_cells.extract_xp", menu.storedExperience(), menu.storedLevels()),
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawKindSelector(GuiGraphicsExtractor graphics) {
        int x = leftPos + SELECTOR_X;
        int y = topPos + SELECTOR_Y;
        drawRow(graphics, x, y, SELECTOR_WIDTH, SELECTOR_HEIGHT, true);
        graphics.centeredText(
                font,
                kindName(menu.selectedKind()),
                x + SELECTOR_WIDTH / 2,
                y + 5,
                TEXT_WHITE
        );
    }

    private void drawKindList(GuiGraphicsExtractor graphics) {
        int x = leftPos + KIND_LIST_X;
        int y = topPos + KIND_LIST_Y;
        int visible = Math.min(VISIBLE_KINDS, SkeletonFarmKind.values().length - kindScroll);
        graphics.fill(x - 1, y - 1, x + SELECTOR_WIDTH + 1, y + visible * KIND_ROW_HEIGHT + 1, 0xFF202020);
        for (int row = 0; row < visible; row++) {
            SkeletonFarmKind kind = SkeletonFarmKind.values()[kindScroll + row];
            int rowY = y + row * KIND_ROW_HEIGHT;
            drawRow(graphics, x, rowY, SELECTOR_WIDTH, KIND_ROW_HEIGHT, kind == menu.selectedKind());
            graphics.centeredText(font, kindName(kind), x + SELECTOR_WIDTH / 2, rowY + 5, TEXT_WHITE);
        }
        drawScrollbar(
                graphics,
                x + SELECTOR_WIDTH - 4,
                y + 2,
                visible * KIND_ROW_HEIGHT - 4,
                kindScroll,
                SkeletonFarmKind.values().length,
                VISIBLE_KINDS
        );
    }

    private void drawLootFilters(GuiGraphicsExtractor graphics) {
        int x = leftPos + FILTER_X;
        int y = topPos + FILTER_Y;
        int total = menu.selectedKind().availableLoot().size();
        int visible = Math.min(VISIBLE_FILTERS, total - lootScroll);
        graphics.fill(x - 1, y - 1, x + FILTER_WIDTH + 1, y + VISIBLE_FILTERS * FILTER_ROW_HEIGHT + 1, 0xFF343434);
        for (int row = 0; row < visible; row++) {
            SkeletonFarmLoot loot = menu.selectedKind().availableLoot().get(lootScroll + row);
            int rowY = y + row * FILTER_ROW_HEIGHT;
            drawRow(graphics, x, rowY, FILTER_WIDTH, FILTER_ROW_HEIGHT - 1, menu.isLootEnabled(loot));
            int boxX = x + 4;
            int boxY = rowY + 4;
            graphics.fill(boxX, boxY, boxX + 11, boxY + 11, 0xFF272727);
            graphics.fill(boxX + 1, boxY + 1, boxX + 10, boxY + 10, 0xFFB7B7B7);
            if (menu.isLootEnabled(loot)) {
                graphics.text(font, "x", boxX + 2, boxY, 0xFF245D24, false);
            }
            graphics.text(font, lootName(menu.selectedKind(), loot), x + 19, rowY + 5, TEXT_WHITE, false);
        }
        drawScrollbar(
                graphics,
                x + FILTER_WIDTH - 4,
                y + 2,
                VISIBLE_FILTERS * FILTER_ROW_HEIGHT - 5,
                lootScroll,
                total,
                VISIBLE_FILTERS
        );
    }

    private static void drawRow(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean selected
    ) {
        graphics.fill(x, y, x + width, y + height, 0xFF383838);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, selected ? 0xFF677F58 : 0xFF686868);
    }

    private static void drawScrollbar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int height,
            int scroll,
            int total,
            int visible
    ) {
        if (total <= visible) {
            return;
        }
        int thumbHeight = Math.max(8, height * visible / total);
        int maximum = total - visible;
        int thumbY = y + (height - thumbHeight) * scroll / maximum;
        graphics.fill(x, y, x + 2, y + height, COLORS.scrollTrack());
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, COLORS.scrollThumbLight());
    }

    private void sendButton(int buttonId) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        if (!menu.clickMenuButton(minecraft.player, buttonId)) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x
                && mouseX < leftPos + x + width
                && mouseY >= topPos + y
                && mouseY < topPos + y + height;
    }

    private static Component kindName(SkeletonFarmKind kind) {
        return Component.translatable("gui.trading_cells.skeleton_kind." + kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Component lootName(SkeletonFarmKind kind, SkeletonFarmLoot loot) {
        if (loot != SkeletonFarmLoot.ARROWS) {
            return Component.translatable("gui.trading_cells.skeleton_loot." + loot.name().toLowerCase(java.util.Locale.ROOT));
        }
        String arrow = switch (kind) {
            case STRAY -> "slowness_arrows";
            case BOGGED -> "poison_arrows";
            case PARCHED -> "weakness_arrows";
            default -> "arrows";
        };
        return Component.translatable("gui.trading_cells.skeleton_loot." + arrow);
    }

    private static Component compactExperience(int experience) {
        if (experience < 1_000_000) {
            return Component.literal(experience + " XP");
        }
        if (experience < 1_000_000_000) {
            return Component.literal("%.2fM XP".formatted(java.util.Locale.ROOT, experience / 1_000_000.0D));
        }
        return Component.literal("%.2fB XP".formatted(java.util.Locale.ROOT, experience / 1_000_000_000.0D));
    }
}
