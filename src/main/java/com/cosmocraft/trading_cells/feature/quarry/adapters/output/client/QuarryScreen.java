package com.cosmocraft.trading_cells.feature.quarry.adapters.output.client;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMaterialCatalog;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.network.QuarryCatalogSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.RequestQuarryCatalogPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class QuarryScreen extends AbstractContainerScreen<QuarryMenu> {
    private static final Identifier STONE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    private static final Identifier BLACKSTONE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/polished_blackstone.png"
    );
    private static final int PROGRESS_FRAME_X =
            (MachineScreenLayout.WIDTH - MachineScreenLayout.PROGRESS_FRAME_WIDTH) / 2;
    private static final int PROGRESS_FRAME_Y = 64;
    private static final int PROGRESS_X = PROGRESS_FRAME_X + MachineScreenLayout.PROGRESS_FILL_X_OFFSET;
    private static final int PROGRESS_Y = PROGRESS_FRAME_Y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET;
    private static final int PROGRESS_WIDTH = MachineScreenLayout.PROGRESS_FILL_WIDTH;
    private static final int PROGRESS_HEIGHT = MachineScreenLayout.PROGRESS_FILL_HEIGHT;
    private static final int CATALOG_BUTTON_SIZE = 12;
    private static final int CATALOG_BUTTON_X = MachineScreenLayout.MACHINE_PANEL_X
            + MachineScreenLayout.MACHINE_PANEL_WIDTH - CATALOG_BUTTON_SIZE - 3;
    private static final int CATALOG_BUTTON_Y = MachineScreenLayout.MACHINE_PANEL_Y
            + MachineScreenLayout.MACHINE_PANEL_HEIGHT - CATALOG_BUTTON_SIZE - 3;
    private static final int DEEP_BUTTON_WIDTH = 140;
    private static final int DEEP_BUTTON_HEIGHT = 17;
    private static final int DEEP_BUTTON_X = (MachineScreenLayout.WIDTH - DEEP_BUTTON_WIDTH) / 2;
    private static final int DEEP_BUTTON_Y = 44;
    private static final int CATALOG_X = -23;
    private static final int CATALOG_Y = 38;
    private static final int CATALOG_WIDTH = MachineScreenLayout.WIDTH + 46;
    private static final int CATALOG_HEADER_HEIGHT = 16;
    private static final int CATALOG_ROW_HEIGHT = 23;
    private static final int CATALOG_VISIBLE_ROWS = 4;
    private static final int OFFSCREEN_MOUSE_COORDINATE = -10_000;

    private Button catalogButton;
    private Button deepMiningButton;
    private boolean catalogOpen;
    private int catalogScroll;
    private ItemStack requestedPickaxe = ItemStack.EMPTY;
    private ItemStack requestedUpgrade = ItemStack.EMPTY;
    private boolean requestedDeepMining;
    private int requestedCatalogRevision = -1;

    public QuarryScreen(QuarryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        catalogButton = addRenderableWidget(Button.builder(Component.literal("?"), button -> toggleCatalog())
                .bounds(
                        leftPos + CATALOG_BUTTON_X,
                        topPos + CATALOG_BUTTON_Y,
                        CATALOG_BUTTON_SIZE,
                        CATALOG_BUTTON_SIZE
                )
                .build());
        catalogButton.setTooltip(Tooltip.create(Component.translatable("button.trading_cells.quarry_catalog")));
        if (menu.kind() == QuarryKind.VILLAGER) {
            deepMiningButton = addRenderableWidget(Button.builder(deepButtonLabel(), button -> toggleDeepMining())
                    .bounds(
                            leftPos + DEEP_BUTTON_X,
                            topPos + DEEP_BUTTON_Y,
                            DEEP_BUTTON_WIDTH,
                            DEEP_BUTTON_HEIGHT
                    )
                    .build());
            updateDeepButton();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateDeepButton();
        if (!catalogOpen) {
            return;
        }
        int maximumScroll = Math.max(0, menu.catalogEntries().size() - CATALOG_VISIBLE_ROWS);
        catalogScroll = Mth.clamp(catalogScroll, 0, maximumScroll);
        if (!ItemStack.isSameItemSameComponents(requestedPickaxe, menu.pickaxe())
                || !ItemStack.isSameItemSameComponents(requestedUpgrade, menu.upgrade())
                || requestedDeepMining != menu.deepMining()
                || requestedCatalogRevision != menu.catalogRevision()) {
            requestCatalog();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        boolean villager = menu.kind() == QuarryKind.VILLAGER;
        MachineScreenTheme theme = theme();
        MachineScreenLayout.drawBackground(graphics, x, y, villager ? STONE : BLACKSTONE, theme);
        drawMachineSlots(graphics, x, y, theme, villager);
        drawProgress(graphics, x, y, villager);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, theme());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean catalogHovered = catalogOpen && isWithinCatalog(mouseX, mouseY);
        int contentMouseX = catalogHovered ? OFFSCREEN_MOUSE_COORDINATE : mouseX;
        int contentMouseY = catalogHovered ? OFFSCREEN_MOUSE_COORDINATE : mouseY;
        super.extractContents(graphics, contentMouseX, contentMouseY, partialTick);
        if (!catalogOpen) {
            return;
        }
        graphics.nextStratum();
        drawCatalog(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (catalogOpen && isWithinCatalog(x, y) && menu.catalogEntries().size() > CATALOG_VISIBLE_ROWS) {
            int maximum = menu.catalogEntries().size() - CATALOG_VISIBLE_ROWS;
            catalogScroll = Mth.clamp((int) (catalogScroll - scrollY), 0, maximum);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overCatalogButton = catalogButton != null && catalogButton.isMouseOver(event.x(), event.y());
        if (catalogOpen && !overCatalogButton && isWithinCatalog(event.x(), event.y())) {
            return true;
        }
        if (catalogOpen && !overCatalogButton) {
            catalogOpen = false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawMachineSlots(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            MachineScreenTheme theme,
            boolean villager
    ) {
        MachineScreenLayout.drawSlot(graphics, x, y, QuarryMenu.WORKER_SLOT_X, QuarryMenu.INPUT_SLOT_Y, theme);
        MachineScreenLayout.drawSlot(graphics, x, y, QuarryMenu.PICKAXE_SLOT_X, QuarryMenu.INPUT_SLOT_Y, theme);
        MachineScreenLayout.drawSlot(graphics, x, y, QuarryMenu.UPGRADE_SLOT_X, QuarryMenu.INPUT_SLOT_Y, theme);
        if (!menu.getSlot(QuarryBlockEntity.WORKER_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    QuarryMenu.WORKER_SLOT_X,
                    QuarryMenu.INPUT_SLOT_Y,
                    villager ? MachineSlotSprites.VILLAGER_HEAD : MachineSlotSprites.PIGLIN_HEAD
            );
        }
        if (!menu.getSlot(QuarryBlockEntity.PICKAXE_SLOT).hasItem()) {
            drawGhostItem(graphics, new ItemStack(Items.IRON_PICKAXE), x + QuarryMenu.PICKAXE_SLOT_X, y + QuarryMenu.INPUT_SLOT_Y);
        }
        if (!menu.getSlot(QuarryBlockEntity.UPGRADE_SLOT).hasItem()) {
            drawGhostItem(
                    graphics,
                    QuarryRegistrationAdapter.QUARRY_COPPER_UPGRADE_ITEM.get().getDefaultInstance(),
                    x + QuarryMenu.UPGRADE_SLOT_X,
                    y + QuarryMenu.INPUT_SLOT_Y
            );
        }
        for (int index = 0; index < QuarryBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    QuarryMenu.outputSlotX(index),
                    QuarryMenu.outputSlotY(index),
                    theme
            );
        }
    }

    private static void drawGhostItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.fakeItem(stack, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0x80606060);
    }

    private void drawProgress(GuiGraphicsExtractor graphics, int x, int y, boolean villager) {
        MachineScreenLayout.drawProgressFrame(graphics, x + PROGRESS_FRAME_X, y + PROGRESS_FRAME_Y, theme());
        int progress = Math.min(
                PROGRESS_WIDTH,
                menu.cycleTicks() * PROGRESS_WIDTH / menu.maximumCycleTicks()
        );
        if (progress > 0) {
            graphics.fill(
                    x + PROGRESS_X,
                    y + PROGRESS_Y,
                    x + PROGRESS_X + progress,
                    y + PROGRESS_Y + PROGRESS_HEIGHT,
                    villager ? 0xFF8AA6B7 : 0xFFD06A3A
            );
        }
        if (menu.cycleTicks() > 0) {
            MachineScreenUtil.drawCenteredCountdown(
                    graphics,
                    font,
                    x + PROGRESS_X + PROGRESS_WIDTH / 2,
                    y + PROGRESS_Y - 1,
                    menu.cycleTicks(),
                    menu.maximumCycleTicks()
            );
        }
    }

    private void toggleCatalog() {
        catalogOpen = !catalogOpen;
        catalogScroll = 0;
        if (catalogOpen) {
            requestCatalog();
        }
    }

    private void requestCatalog() {
        requestedPickaxe = menu.pickaxe().copy();
        requestedUpgrade = menu.upgrade().copy();
        requestedDeepMining = menu.deepMining();
        requestedCatalogRevision = menu.catalogRevision();
        ClientPacketDistributor.sendToServer(new RequestQuarryCatalogPayload(menu.containerId));
    }

    private void toggleDeepMining() {
        if (!menu.deepMiningAvailable() || minecraft == null || minecraft.gameMode == null) {
            return;
        }
        boolean enabled = !menu.deepMining();
        menu.setClientDeepMining(enabled);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, QuarryMenu.TOGGLE_DEEP_MINING_BUTTON);
        updateDeepButton();
    }

    private void updateDeepButton() {
        if (deepMiningButton == null) {
            return;
        }
        deepMiningButton.active = menu.deepMiningAvailable();
        deepMiningButton.setMessage(deepButtonLabel());
        Component tooltip = menu.deepMiningAvailable()
                ? Component.translatable("button.trading_cells.deep_mining")
                : Component.translatable("tooltip.trading_cells.deep_mining_requires_diamond");
        deepMiningButton.setTooltip(Tooltip.create(tooltip));
    }

    private Component deepButtonLabel() {
        return Component.translatable(menu.deepMining()
                ? "button.trading_cells.deep_mining_on"
                : "button.trading_cells.deep_mining_off");
    }

    private void drawCatalog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenTheme theme = theme();
        int panelX = leftPos + CATALOG_X;
        int panelY = topPos + CATALOG_Y;
        int panelHeight = CATALOG_HEADER_HEIGHT + CATALOG_VISIBLE_ROWS * CATALOG_ROW_HEIGHT + 3;
        graphics.fill(panelX, panelY, panelX + CATALOG_WIDTH, panelY + panelHeight, 0xF0101010);
        graphics.outline(panelX, panelY, CATALOG_WIDTH, panelHeight, theme.frameLight());
        graphics.centeredText(
                font,
                Component.translatable("gui.trading_cells.quarry_catalog"),
                panelX + CATALOG_WIDTH / 2,
                panelY + 4,
                0xFFFFFFFF
        );
        List<QuarryCatalogSyncPayload.Entry> entries = menu.catalogEntries();
        if (entries.isEmpty()) {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.trading_cells.quarry_catalog_loading"),
                    panelX + CATALOG_WIDTH / 2,
                    panelY + 43,
                    0xFFB8B8B8
            );
            return;
        }
        int visible = Math.min(CATALOG_VISIBLE_ROWS, entries.size() - catalogScroll);
        for (int row = 0; row < visible; row++) {
            QuarryCatalogSyncPayload.Entry entry = entries.get(catalogScroll + row);
            drawCatalogRow(graphics, entry, panelX, panelY + CATALOG_HEADER_HEIGHT + row * CATALOG_ROW_HEIGHT);
            if (isWithinRow(mouseX, mouseY, row)) {
                graphics.setComponentTooltipForNextFrame(
                        font,
                        catalogTooltip(entry),
                        mouseX,
                        mouseY,
                        entry.preview()
                );
            }
        }
        drawCatalogScrollbar(graphics, panelX, panelY, entries.size(), theme);
    }

    private void drawCatalogRow(
            GuiGraphicsExtractor graphics,
            QuarryCatalogSyncPayload.Entry entry,
            int panelX,
            int rowY
    ) {
        boolean available = entry.blockedReason() == QuarryMaterialCatalog.BlockedReason.NONE.ordinal();
        graphics.fill(
                panelX + 2,
                rowY,
                panelX + CATALOG_WIDTH - 2,
                rowY + CATALOG_ROW_HEIGHT - 1,
                available ? 0xD0444444 : 0xD02A2A2A
        );
        graphics.fakeItem(entry.preview(), panelX + 5, rowY + 3);
        String name = fit(entry.preview().getHoverName().getString(), 124);
        graphics.text(font, name, panelX + 26, rowY + 3, available ? 0xFFFFFFFF : 0xFF999999, false);
        graphics.text(
                font,
                probability(entry.probabilityPartsPerMillion()),
                panelX + CATALOG_WIDTH - 91,
                rowY + 3,
                available ? 0xFF8FE38F : 0xFFB06666,
                false
        );
        graphics.text(
                font,
                amount(entry.minimumAmount(), entry.maximumAmount()),
                panelX + CATALOG_WIDTH - 39,
                rowY + 3,
                0xFFE6E6E6,
                false
        );
        if (!available) {
            graphics.text(
                    font,
                    Component.translatable(blockedTranslation(entry.blockedReason())),
                    panelX + 26,
                    rowY + 12,
                    0xFFFF7777,
                    false
            );
        }
    }

    private List<Component> catalogTooltip(QuarryCatalogSyncPayload.Entry entry) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(entry.preview().getHoverName());
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
        if (entry.blockedReason() != QuarryMaterialCatalog.BlockedReason.NONE.ordinal()) {
            tooltip.add(Component.translatable(blockedTranslation(entry.blockedReason()))
                    .withStyle(ChatFormatting.RED));
        }
        return List.copyOf(tooltip);
    }

    private void drawCatalogScrollbar(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int entryCount,
            MachineScreenTheme theme
    ) {
        if (entryCount <= CATALOG_VISIBLE_ROWS) {
            return;
        }
        int trackX = panelX + CATALOG_WIDTH - 5;
        int trackY = panelY + CATALOG_HEADER_HEIGHT;
        int trackHeight = CATALOG_VISIBLE_ROWS * CATALOG_ROW_HEIGHT - 1;
        int thumbHeight = Math.max(12, trackHeight * CATALOG_VISIBLE_ROWS / entryCount);
        int maximumScroll = entryCount - CATALOG_VISIBLE_ROWS;
        int thumbY = trackY + (trackHeight - thumbHeight) * catalogScroll / maximumScroll;
        graphics.fill(trackX, trackY, trackX + 3, trackY + trackHeight, theme.frameDark());
        graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, theme.frameLight());
    }

    private boolean isWithinCatalog(double x, double y) {
        int panelHeight = CATALOG_HEADER_HEIGHT + CATALOG_VISIBLE_ROWS * CATALOG_ROW_HEIGHT + 3;
        return x >= leftPos + CATALOG_X
                && x < leftPos + CATALOG_X + CATALOG_WIDTH
                && y >= topPos + CATALOG_Y
                && y < topPos + CATALOG_Y + panelHeight;
    }

    private boolean isWithinRow(double x, double y, int row) {
        int panelX = leftPos + CATALOG_X;
        int rowY = topPos + CATALOG_Y + CATALOG_HEADER_HEIGHT + row * CATALOG_ROW_HEIGHT;
        return x >= panelX + 2
                && x < panelX + CATALOG_WIDTH - 2
                && y >= rowY
                && y < rowY + CATALOG_ROW_HEIGHT - 1;
    }

    private MachineScreenTheme theme() {
        return menu.kind() == QuarryKind.VILLAGER
                ? MachineScreenTheme.IRON_FARM
                : MachineScreenTheme.PIGLIN_BREEDER;
    }

    private String fit(String value, int maximumWidth) {
        if (font.width(value) <= maximumWidth) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, maximumWidth - font.width("..."))) + "...";
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
        return minimum == maximum ? Integer.toString(minimum) : minimum + "-" + maximum;
    }

    private static String formatSpeed(double speed) {
        return String.format(Locale.ROOT, "%.1f", speed);
    }

    private static String blockedTranslation(int reason) {
        return reason == QuarryMaterialCatalog.BlockedReason.UPGRADE.ordinal()
                ? "tooltip.trading_cells.quarry_locked_upgrade"
                : "tooltip.trading_cells.quarry_locked_pickaxe";
    }

    private static String upgradeTranslation(int index) {
        QuarryUpgradeTier tier = QuarryUpgradeTier.fromIndex(index);
        return "upgrade.trading_cells.quarry." + tier.name().toLowerCase(Locale.ROOT);
    }
}
