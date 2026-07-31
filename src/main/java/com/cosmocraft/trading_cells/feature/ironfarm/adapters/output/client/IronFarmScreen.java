package com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class IronFarmScreen extends AbstractContainerScreen<IronFarmMenu> {
    private static final Identifier COBBLESTONE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/cobblestone.png"
    );
    private static final MachineScreenTheme THEME = MachineScreenTheme.IRON_FARM;
    private static final int INFO_TEXT_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_FRAME_X = MachineScreenLayout.machineX(54);
    private static final int PROGRESS_FRAME_Y = 47;
    private static final int PROGRESS_X = PROGRESS_FRAME_X + MachineScreenLayout.PROGRESS_FILL_X_OFFSET;
    private static final int PROGRESS_Y = PROGRESS_FRAME_Y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET;
    private static final int PROGRESS_WIDTH = MachineScreenLayout.PROGRESS_FILL_WIDTH;
    private static final int PROGRESS_HEIGHT = MachineScreenLayout.PROGRESS_FILL_HEIGHT;
    private static final int FLOWERS_X = MachineScreenLayout.machineX(8) - 14;
    private Checkbox flowersCheckbox;

    public IronFarmScreen(IronFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineScreenLayout.WIDTH, MachineScreenLayout.HEIGHT);
        titleLabelY = MachineScreenLayout.TITLE_Y;
        inventoryLabelX = MachineScreenLayout.PLAYER_INVENTORY_LABEL_X;
        inventoryLabelY = MachineScreenLayout.PLAYER_INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        rebuildFlowersCheckbox();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (flowersCheckbox != null && flowersCheckbox.selected() != menu.flowersEnabled()) {
            rebuildFlowersCheckbox();
        }
    }

    private void rebuildFlowersCheckbox() {
        if (flowersCheckbox != null) {
            removeWidget(flowersCheckbox);
        }
        flowersCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("label.trading_cells.flowers"), font)
                .pos(leftPos + FLOWERS_X, topPos + 93)
                .maxWidth(74)
                .selected(menu.flowersEnabled())
                .onValueChange((checkbox, selected) -> setFlowersEnabled(selected))
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        MachineScreenLayout.drawBackground(graphics, x, y, COBBLESTONE, THEME);
        for (int index = 0; index < IronFarmBlockEntity.VILLAGER_SLOT_COUNT; index++) {
            int slotX = IronFarmMenu.VILLAGER_ROW_X + index * 24;
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    slotX,
                    IronFarmMenu.VILLAGER_ROW_Y,
                    THEME
            );
            if (!menu.getSlot(IronFarmBlockEntity.FIRST_VILLAGER_SLOT + index).hasItem()) {
                MachineScreenLayout.drawEmptySlotSprite(
                        graphics,
                        x,
                        y,
                        slotX,
                        IronFarmMenu.VILLAGER_ROW_Y,
                        MachineSlotSprites.VILLAGER_HEAD
                );
            }
        }
        for (int index = 0; index < IronFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    IronFarmMenu.OUTPUT_ROW_X + index * 24,
                    IronFarmMenu.OUTPUT_ROW_Y,
                    THEME
            );
        }
        MachineScreenLayout.drawProgressFrame(graphics, x + PROGRESS_FRAME_X, y + PROGRESS_FRAME_Y, THEME);
        drawEfficiencyInfo(graphics, x + MachineScreenLayout.machineX(88), y + 98);

        int progress = Math.min(PROGRESS_WIDTH, menu.cycleTicks() * PROGRESS_WIDTH / menu.maxCycleTicks());
        if (progress > 0) {
            graphics.fill(
                    x + PROGRESS_X,
                    y + PROGRESS_Y,
                    x + PROGRESS_X + progress,
                    y + PROGRESS_Y + PROGRESS_HEIGHT,
                    0xFFD7D7D7
            );
        }
        if (menu.cycleTicks() > 0) {
            MachineScreenUtil.drawCenteredCountdown(
                    graphics,
                    font,
                    x + PROGRESS_X + PROGRESS_WIDTH / 2,
                    y + PROGRESS_Y - 1,
                    menu.cycleTicks(),
                    menu.maxCycleTicks()
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, THEME);
    }

    private void drawEfficiencyInfo(GuiGraphicsExtractor graphics, int x, int y) {
        int villagers = menu.villagerCount();
        int maximum = menu.maximumVillagers();
        int multiplier = menu.currentMultiplier();
        graphics.text(
                font,
                Component.translatable("label.trading_cells.iron_villagers", villagers, maximum),
                x,
                y,
                INFO_TEXT_COLOR,
                true
        );
        graphics.text(
                font,
                Component.translatable("label.trading_cells.iron_current_efficiency", multiplier),
                x,
                y + 10,
                INFO_TEXT_COLOR,
                true
        );
    }

    private void setFlowersEnabled(boolean enabled) {
        menu.setClientFlowersEnabled(enabled);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    enabled ? IronFarmMenu.ENABLE_FLOWERS_BUTTON : IronFarmMenu.DISABLE_FLOWERS_BUTTON
            );
        }
    }
}
