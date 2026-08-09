package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class NetheritePiglinBarteringCellScreen
        extends AbstractContainerScreen<NetheritePiglinBarteringCellMenu> {
    private static final MachineScreenTheme THEME = MachineScreenTheme.IRON_FARM;
    private static final Identifier NETHERITE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/block/netherite_block.png");
    private static final int MAX_TITLE_WIDTH = 188;
    private static final int FILTER_WARNING_Y = 108;

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

        for (int index = 0; index < NetheritePiglinBarteringCellBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    NetheritePiglinBarteringCellMenu.outputSlotX(index),
                    NetheritePiglinBarteringCellMenu.outputSlotY(index),
                    THEME
            );
        }

        if (menu.hasNetheriteUpgrade() && !menu.filterIsSupported()) {
            drawFilterWarning(
                    graphics,
                    x,
                    y,
                    Component.translatable("gui.trading_cells.unsupported_piglin_filter")
            );
        }
    }

    private void drawFilterWarning(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Component warning
    ) {
        graphics.text(
                font,
                warning,
                x + (MachineScreenLayout.WIDTH - font.width(warning)) / 2,
                y + FILTER_WARNING_Y,
                0xFFFF6666,
                true
        );
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
        if (hoveredSlot == null || hoveredSlot.hasItem()) {
            return;
        }
        List<Component> tooltip = switch (hoveredSlot.index) {
            case NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT -> List.of(
                    Component.translatable("gui.trading_cells.piglin_upgrade_slot")
            );
            case NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT -> filterSlotTooltip();
            default -> List.of();
        };
        if (!tooltip.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private List<Component> filterSlotTooltip() {
        Component title = Component.translatable("gui.trading_cells.piglin_filter_slot");
        if (menu.hasNetheriteUpgrade()) {
            return List.of(title);
        }
        return List.of(
                title,
                Component.translatable("gui.trading_cells.piglin_filter_upgrade_required")
                        .withStyle(ChatFormatting.RED)
        );
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
