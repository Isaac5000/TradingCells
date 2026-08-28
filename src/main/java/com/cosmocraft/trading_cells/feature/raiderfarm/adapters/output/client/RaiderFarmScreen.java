package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmMenu;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.network.RequestMobFarmCatalogPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class RaiderFarmScreen extends AbstractContainerScreen<RaiderFarmMenu> {
    public static final int RECIPE_VIEWER_X = 203;
    public static final int RECIPE_VIEWER_Y = 27;
    public static final int RECIPE_VIEWER_WIDTH = 64;
    public static final int RECIPE_VIEWER_HEIGHT = 13;
    private static final RaiderFarmGuiThemeColors COLORS = RaiderFarmGuiThemeColors.resolve();
    private static final int SELECTOR_X = 10;
    private static final int SELECTOR_Y = 28;
    private static final int SELECTOR_WIDTH = 104;
    private static final int SELECTOR_HEIGHT = 18;
    private static final int KIND_LIST_X = SELECTOR_X;
    private static final int KIND_LIST_Y = SELECTOR_Y + SELECTOR_HEIGHT + 1;
    private static final int KIND_ROW_HEIGHT = 18;
    private static final int VISIBLE_KINDS = 7;
    private static final int FILTER_X = 11;
    private static final int FILTER_Y = 64;
    private static final int FILTER_WIDTH = 102;
    private static final int FILTER_ROW_HEIGHT = 20;
    private static final int VISIBLE_FILTERS = 6;
    private static final int PROGRESS_X = RECIPE_VIEWER_X;
    private static final int PROGRESS_Y = RECIPE_VIEWER_Y;
    private static final int PROGRESS_WIDTH = RECIPE_VIEWER_WIDTH;
    private static final int PROGRESS_HEIGHT = RECIPE_VIEWER_HEIGHT;
    private static final int XP_X = 276;
    private static final int XP_Y = 27;
    private static final int XP_WIDTH = 52;
    private static final int XP_HEIGHT = 35;
    private static final int XP_BUTTON_X = PROGRESS_X;
    private static final int XP_BUTTON_Y = 42;
    private static final int XP_BUTTON_WIDTH = PROGRESS_WIDTH;
    private static final int XP_BUTTON_HEIGHT = 12;
    private static final int POWER_X = 10;
    private static final int POWER_Y = 185;
    private static final int POWER_WIDTH = 104;
    private static final int POWER_HEIGHT = 17;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_DARK = 0xFF3A3A3A;
    private static final int TEXT_XP = 0xFF80FF20;
    private static final float XP_TEXT_SCALE = 0.70F;
    private static final int OFFSCREEN_MOUSE_COORDINATE = -10_000;
    private static final int SELECTOR_ARROW_WIDTH = 18;
    private static final int SELECTOR_TEXT_PADDING = 3;
    private static final int SELECTOR_SCROLLBAR_WIDTH = 7;
    private static final int INPUT_PANEL_X = 140;
    private static final int INPUT_PANEL_Y = 27;
    private static final int INPUT_PANEL_WIDTH = 54;
    private static final int INPUT_PANEL_HEIGHT = 35;
    private static final ItemStack EMPTY_SWORD_PREVIEW = new ItemStack(Items.IRON_SWORD);
    private final RaiderFarmLootHelpPanel lootHelp = new RaiderFarmLootHelpPanel();
    private Button lootHelpButton;
    private boolean kindListOpen;
    private int kindScroll;
    private int lootScroll;

    public RaiderFarmScreen(RaiderFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, RaiderFarmMenu.WIDTH, RaiderFarmMenu.HEIGHT);
        titleLabelY = RaiderFarmScreenLayout.HEADER_TEXT_Y;
        inventoryLabelX = RaiderFarmScreenLayout.INVENTORY_LABEL_X;
        inventoryLabelY = RaiderFarmScreenLayout.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        lootHelpButton = addRenderableWidget(Button.builder(Component.literal("?"), button -> {
                    lootHelp.toggle();
                    kindListOpen = false;
                })
                .bounds(
                        leftPos + RaiderFarmLootHelpPanel.BUTTON_X,
                        topPos + RaiderFarmLootHelpPanel.BUTTON_Y,
                        RaiderFarmLootHelpPanel.BUTTON_SIZE,
                        RaiderFarmLootHelpPanel.BUTTON_SIZE
                )
                .build());
        lootHelpButton.setTooltip(Tooltip.create(Component.translatable("button.trading_cells.raider_loot_help")));
        ClientPacketDistributor.sendToServer(new RequestMobFarmCatalogPayload(menu.containerId));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int maximum = Math.max(0, totalLootOptions() - VISIBLE_FILTERS);
        lootScroll = Math.clamp(lootScroll, 0, maximum);
        lootHelp.tick(menu);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        RaiderFarmScreenLayout.drawBackground(graphics, leftPos, topPos);
        drawInventorySlots(graphics);
        drawInputPanel(graphics);
        drawMachineSlots(graphics);
        drawProgress(graphics);
        drawExperience(graphics, mouseX, mouseY);
        drawLootFilters(graphics);
        drawPowerButton(graphics, mouseX, mouseY);
        drawKindSelector(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.translatable("gui.trading_cells.raider_type"),
                9,
                115,
                8,
                18,
                TEXT_DARK,
                false
        );
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                title,
                126,
                340,
                8,
                18,
                TEXT_DARK,
                false
        );
        RaiderFarmTextRenderer.left(
                graphics,
                font,
                playerInventoryTitle,
                RaiderFarmScreenLayout.INVENTORY_LABEL_X,
                338,
                106,
                12,
                TEXT_DARK,
                false
        );
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.translatable("gui.trading_cells.raider_loot"),
                FILTER_X,
                FILTER_X + FILTER_WIDTH,
                FILTER_Y - 13,
                12,
                TEXT_DARK,
                false
        );
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.translatable("gui.trading_cells.raider_outputs"),
                RaiderFarmMenu.OUTPUT_FIRST_X - 1,
                RaiderFarmMenu.OUTPUT_FIRST_X + RaiderFarmMenu.OUTPUT_COLUMNS * 18,
                RaiderFarmMenu.OUTPUT_FIRST_Y - 11,
                12,
                TEXT_DARK,
                false
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean overlayOpen = kindListOpen || lootHelp.isOpen();
        int contentMouseX = overlayOpen ? OFFSCREEN_MOUSE_COORDINATE : mouseX;
        int contentMouseY = overlayOpen ? OFFSCREEN_MOUSE_COORDINATE : mouseY;
        super.extractContents(graphics, contentMouseX, contentMouseY, partialTick);
        if (!overlayOpen) {
            return;
        }
        graphics.nextStratum();
        if (lootHelp.isOpen()) {
            lootHelp.drawPanel(graphics, font, menu, leftPos, topPos, mouseX, mouseY, COLORS);
        } else {
            drawKindList(graphics);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (lootHelp.mouseScrolled(x, y, scrollY, leftPos, topPos)) {
            return true;
        }
        if (lootHelp.isOpen()) {
            return true;
        }
        if (kindListOpen && inside(x, y, KIND_LIST_X, KIND_LIST_Y, SELECTOR_WIDTH, visibleKindCount() * KIND_ROW_HEIGHT)) {
            kindScroll = Mth.clamp(
                    (int) (kindScroll - scrollY),
                    0,
                    maximumKindScroll()
            );
            return true;
        }
        if (!kindListOpen && inside(x, y, FILTER_X, FILTER_Y, FILTER_WIDTH, VISIBLE_FILTERS * FILTER_ROW_HEIGHT)) {
            int maximum = Math.max(0, totalLootOptions() - VISIBLE_FILTERS);
            lootScroll = Mth.clamp((int) (lootScroll - scrollY), 0, maximum);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();
        if (lootHelp.isOpen()) {
            if (inside(
                    x,
                    y,
                    RaiderFarmLootHelpPanel.BUTTON_X,
                    RaiderFarmLootHelpPanel.BUTTON_Y,
                    RaiderFarmLootHelpPanel.BUTTON_SIZE,
                    RaiderFarmLootHelpPanel.BUTTON_SIZE
            )) {
                lootHelp.toggle();
                return true;
            }
            if (lootHelp.consumeOverlayClick(x, y, leftPos, topPos)) {
                return true;
            }
        }
        if (inside(x, y, SELECTOR_X, SELECTOR_Y, SELECTOR_WIDTH, SELECTOR_HEIGHT)) {
            kindListOpen = !kindListOpen;
            if (kindListOpen) {
                kindScroll = Mth.clamp(
                        selectedTargetIndex() - 1,
                        0,
                        maximumKindScroll()
                );
            }
            return true;
        }
        if (kindListOpen) {
            if (inside(x, y, KIND_LIST_X, KIND_LIST_Y, SELECTOR_WIDTH, visibleKindCount() * KIND_ROW_HEIGHT)) {
                int row = (int) (y - topPos - KIND_LIST_Y) / KIND_ROW_HEIGHT;
                int selected = kindScroll + row;
                if (selected < menu.targetEntries().size()) {
                    sendButton(RaiderFarmMenu.SELECT_KIND_BUTTON_BASE + selected);
                    lootScroll = 0;
                }
                kindListOpen = false;
                return true;
            }
            kindListOpen = false;
            return true;
        }
        if (inside(x, y, FILTER_X, FILTER_Y, FILTER_WIDTH, VISIBLE_FILTERS * FILTER_ROW_HEIGHT)) {
            int row = (int) (y - topPos - FILTER_Y) / FILTER_ROW_HEIGHT;
            int index = lootScroll + row;
            var availableLoot = menu.availableLootOptions();
            if (index < availableLoot.size()) {
                RaiderFarmLoot loot = availableLoot.get(index);
                sendButton(RaiderFarmMenu.TOGGLE_LOOT_BUTTON_BASE + loot.ordinal());
            } else {
                int dynamicIndex = index - availableLoot.size();
                if (dynamicIndex < menu.dynamicLootOptions().size()) {
                    sendButton(RaiderFarmMenu.TOGGLE_DYNAMIC_LOOT_BUTTON_BASE + dynamicIndex);
                }
            }
            return true;
        }
        if (inside(x, y, XP_BUTTON_X, XP_BUTTON_Y, XP_BUTTON_WIDTH, XP_BUTTON_HEIGHT)) {
            sendButton(RaiderFarmMenu.EXTRACT_EXPERIENCE_BUTTON);
            return true;
        }
        if (inside(x, y, POWER_X, POWER_Y, POWER_WIDTH, POWER_HEIGHT)) {
            sendButton(RaiderFarmMenu.TOGGLE_ENABLED_BUTTON);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawInputPanel(GuiGraphicsExtractor graphics) {
        int x = leftPos + INPUT_PANEL_X;
        int y = topPos + INPUT_PANEL_Y;
        graphics.fill(x, y, x + INPUT_PANEL_WIDTH, y + INPUT_PANEL_HEIGHT, 0xFF4A4E50);
        graphics.fill(x + 1, y + 1, x + INPUT_PANEL_WIDTH - 1, y + INPUT_PANEL_HEIGHT - 1, 0xFFAEB3B5);
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.literal("x" + menu.simulatedKills()),
                x + 3,
                x + INPUT_PANEL_WIDTH - 3,
                y + 22,
                11,
                TEXT_DARK,
                false
        );
    }

    private void drawInventorySlots(GuiGraphicsExtractor graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                SlotRenderer.drawAtItemPosition(
                        graphics,
                        leftPos,
                        topPos,
                        RaiderFarmMenu.PLAYER_INVENTORY_X + column * 18,
                        RaiderFarmMenu.PLAYER_INVENTORY_Y + row * 18,
                        COLORS.slotPalette()
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            SlotRenderer.drawAtItemPosition(
                    graphics,
                    leftPos,
                    topPos,
                    RaiderFarmMenu.PLAYER_INVENTORY_X + column * 18,
                    RaiderFarmMenu.PLAYER_HOTBAR_Y,
                    COLORS.slotPalette()
            );
        }
        int[] equipmentY = {
                RaiderFarmMenuLayout.EQUIPMENT_HEAD_Y,
                RaiderFarmMenuLayout.EQUIPMENT_CHEST_Y,
                RaiderFarmMenuLayout.EQUIPMENT_LEGS_Y,
                RaiderFarmMenuLayout.EQUIPMENT_FEET_Y,
                RaiderFarmMenuLayout.EQUIPMENT_OFFHAND_Y
        };
        for (int frameY : equipmentY) {
            RaiderFarmScreenLayout.drawSlotAtFramePosition(
                    graphics,
                    leftPos,
                    topPos,
                    RaiderFarmMenuLayout.EQUIPMENT_X,
                    frameY,
                    COLORS
            );
        }
    }

    private void drawMachineSlots(GuiGraphicsExtractor graphics) {
        drawItemSlot(graphics, RaiderFarmMenu.WORKER_SLOT_X, RaiderFarmMenu.INPUT_SLOT_Y);
        drawItemSlot(graphics, RaiderFarmMenu.SWORD_SLOT_X, RaiderFarmMenu.INPUT_SLOT_Y);
        if (!menu.getSlot(RaiderFarmBlockEntity.WORKER_SLOT).hasItem()) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    MachineSlotSprites.CAPTURER,
                    leftPos + RaiderFarmMenu.WORKER_SLOT_X,
                    topPos + RaiderFarmMenu.INPUT_SLOT_Y,
                    16,
                    16
            );
        }
        if (!menu.getSlot(RaiderFarmBlockEntity.SWORD_SLOT).hasItem()) {
            int x = leftPos + RaiderFarmMenu.SWORD_SLOT_X;
            int y = topPos + RaiderFarmMenu.INPUT_SLOT_Y;
            graphics.fakeItem(EMPTY_SWORD_PREVIEW, x, y);
            graphics.fill(x, y, x + 16, y + 16, 0x90606060);
        }
        for (int index = 0; index < RaiderFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            drawItemSlot(graphics, RaiderFarmMenu.outputSlotX(index), RaiderFarmMenu.outputSlotY(index));
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
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.literal(time),
                x + 2,
                x + PROGRESS_WIDTH - 2,
                y + 1,
                PROGRESS_HEIGHT - 2,
                TEXT_WHITE,
                true
        );
    }

    private void drawExperience(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + XP_X;
        int y = topPos + XP_Y;
        drawStoredExperiencePanel(graphics, x, y);
        drawScaledXpText(
                graphics,
                Component.translatable("gui.trading_cells.stored_xp", menu.storedExperience()),
                x + 2,
                y + 3,
                XP_WIDTH - 4,
                13
        );
        drawScaledXpText(
                graphics,
                Component.translatable("gui.trading_cells.level_equivalent", menu.storedLevels()),
                x + 2,
                y + 19,
                XP_WIDTH - 4,
                13
        );
        boolean active = menu.storedExperience() > 0;
        boolean hovered = inside(mouseX, mouseY, XP_BUTTON_X, XP_BUTTON_Y, XP_BUTTON_WIDTH, XP_BUTTON_HEIGHT);
        drawBeveledButton(
                graphics,
                leftPos + XP_BUTTON_X,
                topPos + XP_BUTTON_Y,
                XP_BUTTON_WIDTH,
                XP_BUTTON_HEIGHT,
                active,
                hovered
        );
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.translatable("button.trading_cells.raider_withdraw_xp"),
                leftPos + XP_BUTTON_X + 2,
                leftPos + XP_BUTTON_X + XP_BUTTON_WIDTH - 2,
                topPos + XP_BUTTON_Y + 2,
                XP_BUTTON_HEIGHT - 2,
                active ? TEXT_DARK : 0xFFE0E0E0,
                false
        );
    }

    private void drawPowerButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + POWER_X;
        int y = topPos + POWER_Y;
        boolean hovered = inside(mouseX, mouseY, POWER_X, POWER_Y, POWER_WIDTH, POWER_HEIGHT);
        int fill = menu.isEnabled()
                ? hovered ? 0xFF78946A : 0xFF677F58
                : hovered ? 0xFF7A7A7A : 0xFF686868;
        graphics.fill(x, y, x + POWER_WIDTH, y + POWER_HEIGHT, 0xFF383838);
        graphics.fill(x + 1, y + 1, x + POWER_WIDTH - 1, y + POWER_HEIGHT - 1, fill);
        int boxX = x + 4;
        int boxY = y + 3;
        graphics.fill(boxX, boxY, boxX + 11, boxY + 11, 0xFF272727);
        graphics.fill(boxX + 1, boxY + 1, boxX + 10, boxY + 10, 0xFFB7B7B7);
        if (menu.isEnabled()) {
            drawCheckmark(graphics, boxX, boxY);
        }
        graphics.fill(x + 18, y + 3, x + 19, y + POWER_HEIGHT - 3, 0x80303030);
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                Component.translatable(menu.isEnabled()
                        ? "gui.trading_cells.raider_farm.active"
                        : "gui.trading_cells.raider_farm.paused"),
                x + 21,
                x + POWER_WIDTH - 4,
                y + 1,
                POWER_HEIGHT - 2,
                TEXT_WHITE,
                false
        );
    }

    private static void drawStoredExperiencePanel(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + XP_WIDTH, y + XP_HEIGHT, 0xFF343A31);
        graphics.fill(x + 1, y + 1, x + XP_WIDTH - 1, y + XP_HEIGHT - 1, 0xFFA8B0A0);
        graphics.fill(x + 1, y + 1, x + XP_WIDTH - 1, y + 2, 0xFF6C7468);
        graphics.fill(x + 1, y + 1, x + 2, y + XP_HEIGHT - 1, 0xFF6C7468);
        graphics.fill(x + 1, y + XP_HEIGHT - 2, x + XP_WIDTH - 1, y + XP_HEIGHT - 1, 0xFFD2D7CC);
        graphics.fill(x + XP_WIDTH - 2, y + 1, x + XP_WIDTH - 1, y + XP_HEIGHT - 1, 0xFFD2D7CC);
    }

    private void drawScaledXpText(
            GuiGraphicsExtractor graphics,
            Component text,
            int x,
            int y,
            int width,
            int height
    ) {
        FittedTextRenderer.centeredAtMost(
                graphics,
                font,
                text,
                x,
                x + width,
                y,
                height,
                TEXT_XP,
                true,
                XP_TEXT_SCALE
        );
    }

    private static void drawBeveledButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active,
            boolean hovered
    ) {
        int outer = active ? 0xFF404040 : 0xFF5D5D5D;
        int fill = !active ? 0xFF929292 : hovered ? 0xFFE2E2E2 : 0xFFD0D0D0;
        int light = active ? 0xFFF0F0F0 : 0xFFA8A8A8;
        int shadow = active ? 0xFF888888 : 0xFF747474;
        graphics.fill(x, y, x + width, y + height, outer);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, light);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, light);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, shadow);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, shadow);
    }

    private void drawKindSelector(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + SELECTOR_X;
        int y = topPos + SELECTOR_Y;
        boolean hovered = inside(mouseX, mouseY, SELECTOR_X, SELECTOR_Y, SELECTOR_WIDTH, SELECTOR_HEIGHT);
        drawRow(graphics, x, y, SELECTOR_WIDTH, SELECTOR_HEIGHT, hovered || kindListOpen);
        int arrowX = x + SELECTOR_WIDTH - SELECTOR_ARROW_WIDTH;
        graphics.fill(arrowX, y, x + SELECTOR_WIDTH, y + SELECTOR_HEIGHT, 0xB0383838);
        graphics.fill(arrowX, y, arrowX + 1, y + SELECTOR_HEIGHT, 0xFF2B2B2B);
        RaiderFarmTextRenderer.centered(
                graphics,
                font,
                targetName(menu.selectedTargetId()),
                x + SELECTOR_TEXT_PADDING,
                arrowX - SELECTOR_TEXT_PADDING,
                y,
                SELECTOR_HEIGHT,
                TEXT_WHITE,
                false
        );
        drawDropdownChevron(graphics, arrowX + 5, y + 7, kindListOpen, hovered);
    }

    private void drawKindList(GuiGraphicsExtractor graphics) {
        int x = leftPos + KIND_LIST_X;
        int y = topPos + KIND_LIST_Y;
        int visible = visibleKindCount();
        graphics.fill(x - 1, y - 1, x + SELECTOR_WIDTH + 1, y + visible * KIND_ROW_HEIGHT + 1, 0xFF202020);
        graphics.fill(x, y, x + SELECTOR_WIDTH, y + visible * KIND_ROW_HEIGHT, 0xFF4B4B4B);
        graphics.enableScissor(x, y, x + SELECTOR_WIDTH, y + visible * KIND_ROW_HEIGHT);
        for (int row = 0; row < visible; row++) {
            var target = menu.targetEntries().get(kindScroll + row);
            int rowY = y + row * KIND_ROW_HEIGHT;
            drawRow(
                    graphics,
                    x,
                    rowY,
                    SELECTOR_WIDTH,
                    KIND_ROW_HEIGHT,
                    target.entityTypeId().equals(menu.selectedTargetId())
            );
            RaiderFarmTextRenderer.centered(
                    graphics,
                    font,
                    targetName(target.entityTypeId()),
                    x + SELECTOR_TEXT_PADDING,
                    x + SELECTOR_WIDTH - SELECTOR_SCROLLBAR_WIDTH,
                    rowY,
                    KIND_ROW_HEIGHT,
                    TEXT_WHITE,
                    false
            );
        }
        graphics.disableScissor();
        drawScrollbar(
                graphics,
                x + SELECTOR_WIDTH - 4,
                y + 2,
                visible * KIND_ROW_HEIGHT - 4,
                kindScroll,
                menu.targetEntries().size(),
                VISIBLE_KINDS
        );
    }

    private void drawLootFilters(GuiGraphicsExtractor graphics) {
        int x = leftPos + FILTER_X;
        int y = topPos + FILTER_Y;
        var availableLoot = menu.availableLootOptions();
        var dynamicLoot = menu.dynamicLootOptions();
        int total = availableLoot.size() + dynamicLoot.size();
        int visible = Math.min(VISIBLE_FILTERS, total - lootScroll);
        graphics.fill(x - 1, y - 1, x + FILTER_WIDTH + 1, y + VISIBLE_FILTERS * FILTER_ROW_HEIGHT + 1, 0xFF343434);
        graphics.enableScissor(x, y, x + FILTER_WIDTH, y + VISIBLE_FILTERS * FILTER_ROW_HEIGHT);
        for (int row = 0; row < visible; row++) {
            int index = lootScroll + row;
            int rowY = y + row * FILTER_ROW_HEIGHT;
            boolean staticOption = index < availableLoot.size();
            RaiderFarmLoot loot = staticOption ? availableLoot.get(index) : null;
            ItemStack dynamicStack = staticOption ? ItemStack.EMPTY : dynamicLoot.get(index - availableLoot.size());
            boolean enabled = staticOption ? menu.isLootEnabled(loot) : menu.isDynamicLootEnabled(dynamicStack);
            drawRow(graphics, x, rowY, FILTER_WIDTH, FILTER_ROW_HEIGHT - 1, enabled);
            int boxX = x + 4;
            int boxY = rowY + 4;
            graphics.fill(boxX, boxY, boxX + 11, boxY + 11, 0xFF272727);
            graphics.fill(boxX + 1, boxY + 1, boxX + 10, boxY + 10, 0xFFB7B7B7);
            if (enabled) {
                drawCheckmark(graphics, boxX, boxY);
            }
            drawLootName(
                    graphics,
                    staticOption ? lootName(loot) : dynamicStack.getHoverName(),
                    x + 19,
                    rowY
            );
        }
        graphics.disableScissor();
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

    private void drawLootName(GuiGraphicsExtractor graphics, Component name, int x, int rowY) {
        RaiderFarmTextRenderer.left(
                graphics,
                font,
                name,
                x,
                leftPos + FILTER_X + FILTER_WIDTH - SELECTOR_SCROLLBAR_WIDTH,
                rowY,
                FILTER_ROW_HEIGHT - 1,
                TEXT_WHITE,
                false
        );
    }

    private static void drawCheckmark(GuiGraphicsExtractor graphics, int x, int y) {
        int color = 0xFF2F7D32;
        graphics.fill(x + 2, y + 5, x + 4, y + 7, color);
        graphics.fill(x + 3, y + 6, x + 5, y + 8, color);
        graphics.fill(x + 4, y + 7, x + 6, y + 9, color);
        graphics.fill(x + 5, y + 6, x + 7, y + 8, color);
        graphics.fill(x + 6, y + 5, x + 8, y + 7, color);
        graphics.fill(x + 7, y + 4, x + 9, y + 6, color);
        graphics.fill(x + 8, y + 3, x + 10, y + 5, color);
    }

    private static void drawDropdownChevron(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            boolean open,
            boolean hovered
    ) {
        int light = hovered ? 0xFFFFFFFF : 0xFFD8D8D8;
        if (open) {
            graphics.fill(x + 3, y, x + 4, y + 1, light);
            graphics.fill(x + 2, y + 1, x + 5, y + 2, light);
            graphics.fill(x + 1, y + 2, x + 6, y + 3, light);
            return;
        }
        graphics.fill(x + 1, y, x + 6, y + 1, light);
        graphics.fill(x + 2, y + 1, x + 5, y + 2, light);
        graphics.fill(x + 3, y + 2, x + 4, y + 3, light);
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

    private static Component targetName(net.minecraft.resources.Identifier id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .map(net.minecraft.world.entity.EntityType::getDescription)
                .orElse(Component.translatable("entity." + id.getNamespace() + "." + id.getPath()));
    }

    private static Component lootName(RaiderFarmLoot loot) {
        return Component.translatable(
                "gui.trading_cells.raider_loot." + loot.name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    private int maximumKindScroll() {
        return Math.max(0, menu.targetEntries().size() - VISIBLE_KINDS);
    }

    private int visibleKindCount() {
        return Math.min(VISIBLE_KINDS, menu.targetEntries().size() - kindScroll);
    }

    private int selectedTargetIndex() {
        for (int index = 0; index < menu.targetEntries().size(); index++) {
            if (menu.targetEntries().get(index).entityTypeId().equals(menu.selectedTargetId())) {
                return index;
            }
        }
        return 0;
    }

    private int totalLootOptions() {
        return menu.availableLootOptions().size() + menu.dynamicLootOptions().size();
    }
}
