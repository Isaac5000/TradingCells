package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellMenu;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.EnhancedPiglinBarterRewards;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.PiglinBarterCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeScreenCommon;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader.VillagerTradeSprites;
import java.util.List;
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

public final class NetheritePiglinBarteringCellScreen
        extends AbstractContainerScreen<NetheritePiglinBarteringCellMenu> {
    private static final MachineScreenTheme THEME = MachineScreenTheme.IRON_FARM;
    private static final Identifier NETHERITE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/block/netherite_block.png");
    private static final int MAX_TITLE_WIDTH = 188;
    private static final int FILTER_WARNING_Y = 106;

    private static final int FILTER_INFO_BUTTON_SIZE = 12;
    private static final int FILTER_INFO_BUTTON_X = MachineScreenLayout.MACHINE_PANEL_X
            + MachineScreenLayout.MACHINE_PANEL_WIDTH - FILTER_INFO_BUTTON_SIZE - 3;
    private static final int FILTER_INFO_BUTTON_Y = MachineScreenLayout.MACHINE_PANEL_Y
            + MachineScreenLayout.MACHINE_PANEL_HEIGHT - FILTER_INFO_BUTTON_SIZE - 3;
    private static final int FILTER_LIST_WIDTH = 174;
    private static final int FILTER_LIST_X = (MachineScreenLayout.WIDTH - FILTER_LIST_WIDTH) / 2;
    private static final int FILTER_LIST_Y = 42;
    private static final int FILTER_HEADER_HEIGHT = 18;
    private static final int FILTER_ROW_HEIGHT = 30;
    private static final int MAX_VISIBLE_FILTERS = 4;
    private static final int CATALOG_SLOT_SIZE = 18;
    private static final int FILTER_TO_ARROW_GAP = 18;
    private static final int ARROW_TO_OUTPUT_GAP = 18;
    private static final long OUTPUT_CYCLE_MILLIS = 1_200L;

    private Button filterInfoButton;
    private boolean filterListOpen;
    private int filterScroll;
    private List<PiglinBarterCatalog.Entry> cachedFilterEntries = List.of();

    public NetheritePiglinBarteringCellScreen(
            NetheritePiglinBarteringCellMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        filterInfoButton = addRenderableWidget(Button.builder(
                        Component.literal("?"),
                        button -> toggleFilterList()
                )
                .bounds(
                        leftPos + FILTER_INFO_BUTTON_X,
                        topPos + FILTER_INFO_BUTTON_Y,
                        FILTER_INFO_BUTTON_SIZE,
                        FILTER_INFO_BUTTON_SIZE
                )
                .build());
        filterInfoButton.setTooltip(Tooltip.create(
                Component.translatable("button.trading_cells.piglin_filter_help")
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!filterListOpen) {
            return;
        }
        int maximumScroll = Math.max(0, filterEntries().size() - MAX_VISIBLE_FILTERS);
        filterScroll = Mth.clamp(filterScroll, 0, maximumScroll);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        MachineScreenLayout.drawBackground(graphics, x, y, NETHERITE, THEME);

        drawControlSlot(
                graphics,
                x,
                y,
                NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT,
                NetheritePiglinBarteringCellMenu.UPGRADE_SLOT_Y,
                null
        );
        drawControlSlot(
                graphics,
                x,
                y,
                NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT,
                NetheritePiglinBarteringCellMenu.FILTER_SLOT_Y,
                MachineSlotSprites.WEAKNESS_POTION
        );

        for (int lane = 0; lane < NetheritePiglinBarteringCellBlockEntity.GOLD_SLOT_COUNT; lane++) {
            int slotX = NetheritePiglinBarteringCellMenu.GOLD_ROW_X
                    + lane * NetheritePiglinBarteringCellMenu.LANE_SPACING;
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    slotX,
                    NetheritePiglinBarteringCellMenu.GOLD_ROW_Y,
                    THEME
            );
        }

        for (int lane = 0; lane < NetheritePiglinBarteringCellBlockEntity.OUTPUT_SLOT_COUNT; lane++) {
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    NetheritePiglinBarteringCellMenu.OUTPUT_ROW_X
                            + lane * NetheritePiglinBarteringCellMenu.LANE_SPACING,
                    NetheritePiglinBarteringCellMenu.OUTPUT_ROW_Y,
                    THEME
            );
        }

        if (menu.filterNeedsUpgrade()) {
            Component warning = Component.translatable("gui.trading_cells.piglin_upgrade_required");
            graphics.text(
                    font,
                    warning,
                    x + (MachineScreenLayout.WIDTH - font.width(warning)) / 2,
                    y + FILTER_WARNING_Y,
                    0xFFFF6666,
                    true
            );
        } else if (!menu.filterIsSupported()) {
            Component warning = Component.translatable("gui.trading_cells.unsupported_piglin_filter");
            graphics.text(
                    font,
                    warning,
                    x + (MachineScreenLayout.WIDTH - font.width(warning)) / 2,
                    y + FILTER_WARNING_Y,
                    0xFFFF6666,
                    true
            );
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (!filterListOpen) {
            return;
        }
        graphics.nextStratum();
        drawFilterList(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        List<PiglinBarterCatalog.Entry> entries = filterEntries();
        if (filterListOpen && isWithinFilterList(x, y, entries.size()) && entries.size() > MAX_VISIBLE_FILTERS) {
            int maximumScroll = entries.size() - MAX_VISIBLE_FILTERS;
            filterScroll = Mth.clamp((int) (filterScroll - scrollY), 0, maximumScroll);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overFilterButton = filterInfoButton != null
                && filterInfoButton.isMouseOver(event.x(), event.y());
        if (filterListOpen
                && !overFilterButton
                && isWithinFilterList(event.x(), event.y(), filterEntries().size())) {
            return true;
        }
        if (filterListOpen && !overFilterButton) {
            closeFilterList();
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void toggleFilterList() {
        if (filterListOpen) {
            closeFilterList();
            return;
        }
        filterListOpen = true;
        filterScroll = 0;
    }

    private void closeFilterList() {
        filterListOpen = false;
    }

    private void drawFilterList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<PiglinBarterCatalog.Entry> entries = filterEntries();
        int visible = Math.clamp(entries.size() - filterScroll, 0, MAX_VISIBLE_FILTERS);
        int panelX = leftPos + FILTER_LIST_X;
        int panelY = topPos + FILTER_LIST_Y;
        int panelHeight = FILTER_HEADER_HEIGHT + visible * FILTER_ROW_HEIGHT + 2;
        boolean scrollable = entries.size() > MAX_VISIBLE_FILTERS;

        graphics.fill(panelX, panelY, panelX + FILTER_LIST_WIDTH, panelY + panelHeight, THEME.frameDark());
        graphics.fill(
                panelX + 1,
                panelY + 1,
                panelX + FILTER_LIST_WIDTH - 1,
                panelY + panelHeight - 1,
                THEME.frame()
        );
        graphics.text(
                font,
                Component.translatable("gui.trading_cells.piglin_filter_catalog"),
                panelX + 5,
                panelY + 4,
                THEME.titleText(),
                false
        );

        int rowWidth = FILTER_LIST_WIDTH - 4 - (scrollable ? 5 : 0);
        for (int row = 0; row < visible; row++) {
            int entryIndex = filterScroll + row;
            PiglinBarterCatalog.Entry entry = entries.get(entryIndex);
            int rowX = panelX + 2;
            int rowY = panelY + FILTER_HEADER_HEIGHT + row * FILTER_ROW_HEIGHT;
            boolean hovered = mouseX >= rowX
                    && mouseX < rowX + rowWidth
                    && mouseY >= rowY
                    && mouseY < rowY + FILTER_ROW_HEIGHT - 1;

            graphics.fill(
                    rowX,
                    rowY,
                    rowX + rowWidth,
                    rowY + FILTER_ROW_HEIGHT - 1,
                    hovered ? THEME.frame() : THEME.slot()
            );
            graphics.outline(
                    rowX,
                    rowY,
                    rowWidth,
                    FILTER_ROW_HEIGHT - 1,
                    hovered ? THEME.slotHighlight() : THEME.frameLight()
            );

            ItemStack filterStack = entry.filter();
            ItemStack outputStack = cyclingOutput(entry, entryIndex);
            int itemY = rowY + (FILTER_ROW_HEIGHT - CATALOG_SLOT_SIZE) / 2;
            int arrowY = rowY + (FILTER_ROW_HEIGHT - CATALOG_SLOT_SIZE) / 2 + 1;

            int contentWidth = CATALOG_SLOT_SIZE
                    + FILTER_TO_ARROW_GAP
                    + CATALOG_SLOT_SIZE
                    + ARROW_TO_OUTPUT_GAP
                    + CATALOG_SLOT_SIZE;
            int filterItemX = rowX + (rowWidth - contentWidth) / 2;
            int arrowX = filterItemX + CATALOG_SLOT_SIZE + FILTER_TO_ARROW_GAP;
            int outputItemX = arrowX + CATALOG_SLOT_SIZE + ARROW_TO_OUTPUT_GAP;

            MachineScreenLayout.drawSlot(graphics, 0, 0, filterItemX, itemY, THEME);
            graphics.fakeItem(filterStack, filterItemX, itemY);

            VillagerTradeScreenCommon.drawTradeArrow(
                    graphics,
                    arrowX,
                    arrowY,
                    VillagerTradeSprites.State.NORMAL
            );

            MachineScreenLayout.drawSlot(graphics, 0, 0, outputItemX, itemY, THEME);
            graphics.fakeItem(outputStack, outputItemX, itemY);
            drawNetheriteAmountRange(graphics, entry, outputStack, outputItemX, itemY);

            setCatalogItemTooltip(graphics, filterStack, filterItemX, itemY, mouseX, mouseY);
            setCatalogItemTooltip(graphics, outputStack, outputItemX, itemY, mouseX, mouseY);
        }

        drawFilterScrollbar(graphics, entries.size(), visible, panelX, panelY);
    }

    private void drawFilterScrollbar(
            GuiGraphicsExtractor graphics,
            int entryCount,
            int visible,
            int panelX,
            int panelY
    ) {
        if (entryCount <= visible) {
            return;
        }
        int trackX = panelX + FILTER_LIST_WIDTH - 5;
        int trackY = panelY + FILTER_HEADER_HEIGHT + 2;
        int trackHeight = visible * FILTER_ROW_HEIGHT - 5;
        int thumbHeight = Math.max(9, trackHeight * visible / entryCount);
        int maximumScroll = entryCount - visible;
        int thumbY = trackY + (trackHeight - thumbHeight) * filterScroll / maximumScroll;
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, THEME.frameDark());
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, THEME.slotHighlight());
    }

    private boolean isWithinFilterList(double mouseX, double mouseY, int entryCount) {
        int visible = Math.min(MAX_VISIBLE_FILTERS, entryCount);
        int panelHeight = FILTER_HEADER_HEIGHT + visible * FILTER_ROW_HEIGHT + 2;
        int panelX = leftPos + FILTER_LIST_X;
        int panelY = topPos + FILTER_LIST_Y;
        return mouseX >= panelX
                && mouseX < panelX + FILTER_LIST_WIDTH
                && mouseY >= panelY
                && mouseY < panelY + panelHeight;
    }

    private ItemStack cyclingOutput(PiglinBarterCatalog.Entry entry, int entryIndex) {
        return entry.outputs().get(cyclingOutputIndex(entry, entryIndex));
    }

    private int cyclingOutputIndex(PiglinBarterCatalog.Entry entry, int entryIndex) {
        if (entry.outputs().size() <= 1) {
            return 0;
        }
        long cycle = System.currentTimeMillis() / OUTPUT_CYCLE_MILLIS;
        return Math.floorMod((int) (cycle + entryIndex), entry.outputs().size());
    }

    private void setCatalogItemTooltip(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int itemX,
            int itemY,
            int mouseX,
            int mouseY
    ) {
        if (isOverItem(mouseX, mouseY, itemX, itemY)) {
            graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        }
    }

    private static boolean isOverItem(int mouseX, int mouseY, int itemX, int itemY) {
        return mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16;
    }


    private void drawNetheriteAmountRange(
            GuiGraphicsExtractor graphics,
            PiglinBarterCatalog.Entry entry,
            ItemStack output,
            int itemX,
            int itemY
    ) {
        int minimum = EnhancedPiglinBarterRewards.upgradedStackAmount(
                output,
                entry.minimumAmount(),
                EnhancedPiglinBarterRewards.NETHERITE_UPGRADE_LEVEL
        );
        int maximum = EnhancedPiglinBarterRewards.upgradedStackAmount(
                output,
                entry.maximumAmount(),
                EnhancedPiglinBarterRewards.NETHERITE_UPGRADE_LEVEL
        );
        if (minimum == 1 && maximum == 1) {
            return;
        }

        String amountRange = minimum == maximum ? "x" + maximum : minimum + "-" + maximum;
        graphics.text(
                font,
                Component.literal(amountRange),
                itemX + CATALOG_SLOT_SIZE + 3,
                itemY + CATALOG_SLOT_SIZE - 8,
                0xFFFFFFFF,
                true
        );
    }

    private List<PiglinBarterCatalog.Entry> filterEntries() {
        if (cachedFilterEntries.isEmpty()) {
            cachedFilterEntries = PiglinBarterCatalog.entries(
                    minecraft == null || minecraft.level == null
                            ? null
                            : minecraft.level.registryAccess()
            );
        }
        return cachedFilterEntries;
    }

    private void drawControlSlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int menuSlot,
            int slotY,
            @org.jspecify.annotations.Nullable Identifier emptyIcon
    ) {
        MachineScreenLayout.drawSlot(
                graphics,
                x,
                y,
                NetheritePiglinBarteringCellMenu.CONTROL_SLOT_X,
                slotY,
                THEME
        );
        if (emptyIcon != null && !menu.getSlot(menuSlot).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    NetheritePiglinBarteringCellMenu.CONTROL_SLOT_X,
                    slotY,
                    emptyIcon
            );
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (filterListOpen || hoveredSlot == null || hoveredSlot.hasItem()) {
            return;
        }
        Component tooltip = switch (hoveredSlot.index) {
            case NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT ->
                    Component.translatable("gui.trading_cells.piglin_upgrade_slot");
            case NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT -> menu.hasNetheriteUpgrade()
                    ? Component.translatable("gui.trading_cells.piglin_filter_slot")
                    : Component.translatable("gui.trading_cells.piglin_upgrade_required");
            default -> null;
        };
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawFittedTitle(graphics);
        graphics.text(
                font,
                playerInventoryTitle,
                MachineScreenLayout.PLAYER_INVENTORY_LABEL_X,
                MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y,
                THEME.titleText(),
                false
        );
    }

    private void drawFittedTitle(GuiGraphicsExtractor graphics) {
        int titleWidth = Math.max(1, font.width(title));
        float scale = Math.min(1.0F, MAX_TITLE_WIDTH / (float) titleWidth);
        float scaledWidth = titleWidth * scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate((MachineScreenLayout.WIDTH - scaledWidth) / 2.0F, MachineScreenLayout.TITLE_Y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, title, 0, 0, THEME.titleText(), true);
        graphics.pose().popMatrix();
    }

}
