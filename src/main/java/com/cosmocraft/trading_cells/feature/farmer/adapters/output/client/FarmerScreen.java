package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerMenu;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class FarmerScreen extends AbstractContainerScreen<FarmerMenu> {
    private static final Identifier DIRT = Identifier.fromNamespaceAndPath("minecraft", "textures/block/dirt.png");
    private static final MachineScreenTheme THEME = MachineScreenTheme.FARMER;
    private static final int PROGRESS_FRAME_X = MachineScreenLayout.machineX(54);
    private static final int PROGRESS_FRAME_Y = 66;
    private static final int PROGRESS_X = PROGRESS_FRAME_X + MachineScreenLayout.PROGRESS_FILL_X_OFFSET;
    private static final int PROGRESS_Y = PROGRESS_FRAME_Y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET;
    private static final int PROGRESS_WIDTH = MachineScreenLayout.PROGRESS_FILL_WIDTH;
    private static final int PROGRESS_HEIGHT = MachineScreenLayout.PROGRESS_FILL_HEIGHT;

    public FarmerScreen(FarmerMenu menu, Inventory inventory, Component title) {
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
        MachineScreenLayout.drawBackground(graphics, x, y, DIRT, THEME);

        MachineScreenLayout.drawSlot(graphics, x, y, FarmerMenu.VILLAGER_SLOT_X, FarmerMenu.INPUT_SLOT_Y, THEME);
        MachineScreenLayout.drawSlot(graphics, x, y, FarmerMenu.HOE_SLOT_X, FarmerMenu.INPUT_SLOT_Y, THEME);
        MachineScreenLayout.drawSlot(graphics, x, y, FarmerMenu.CROP_SLOT_X, FarmerMenu.INPUT_SLOT_Y, THEME);
        if (!menu.getSlot(FarmerBlockEntity.VILLAGER_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    FarmerMenu.VILLAGER_SLOT_X,
                    FarmerMenu.INPUT_SLOT_Y,
                    MachineSlotSprites.VILLAGER_HEAD
            );
        }
        if (!menu.getSlot(FarmerBlockEntity.HOE_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    FarmerMenu.HOE_SLOT_X,
                    FarmerMenu.INPUT_SLOT_Y,
                    MachineSlotSprites.HOE
            );
        }
        if (!menu.getSlot(FarmerBlockEntity.CROP_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    FarmerMenu.CROP_SLOT_X,
                    FarmerMenu.INPUT_SLOT_Y,
                    MachineSlotSprites.WHEAT_SEEDS
            );
        }
        for (int index = 0; index < 4; index++) {
            MachineScreenLayout.drawSlot(
                    graphics,
                    x,
                    y,
                    MachineScreenLayout.machineX(43) + index * 24,
                    94,
                    THEME
            );
        }
        MachineScreenLayout.drawProgressFrame(graphics, x + PROGRESS_FRAME_X, y + PROGRESS_FRAME_Y, THEME);

        if (menu.isCultivating()) {
            int progress = Math.min(PROGRESS_WIDTH, menu.growthTicks() * PROGRESS_WIDTH / menu.maxGrowthTicks());
            if (progress > 0) {
                graphics.fill(
                        x + PROGRESS_X,
                        y + PROGRESS_Y,
                        x + PROGRESS_X + progress,
                        y + PROGRESS_Y + PROGRESS_HEIGHT,
                        0xFF55A630
                );
            }
            MachineScreenUtil.drawCenteredCountdown(
                    graphics,
                    font,
                    x + PROGRESS_X + PROGRESS_WIDTH / 2,
                    y + PROGRESS_Y - 1,
                    menu.growthTicks(),
                    menu.maxGrowthTicks()
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, THEME);
    }
}
