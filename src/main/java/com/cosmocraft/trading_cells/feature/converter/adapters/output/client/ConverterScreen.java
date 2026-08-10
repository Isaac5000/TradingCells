package com.cosmocraft.trading_cells.feature.converter.adapters.output.client;

import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterBlockEntity;
import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterMenu;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenTheme;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ConverterScreen extends AbstractContainerScreen<ConverterMenu> {
    private static final Identifier MOSSY_COBBLESTONE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/mossy_cobblestone.png"
    );
    private static final MachineScreenTheme THEME = MachineScreenTheme.CONVERTER;
    private static final int PROGRESS_FRAME_X = MachineScreenLayout.machineX(54);
    private static final int PROGRESS_FRAME_Y = 95;
    private static final int PROGRESS_X = PROGRESS_FRAME_X + MachineScreenLayout.PROGRESS_FILL_X_OFFSET;
    private static final int PROGRESS_Y = PROGRESS_FRAME_Y + MachineScreenLayout.PROGRESS_FILL_Y_OFFSET;
    private static final int PROGRESS_WIDTH = MachineScreenLayout.PROGRESS_FILL_WIDTH;
    private static final int PROGRESS_HEIGHT = MachineScreenLayout.PROGRESS_FILL_HEIGHT;

    public ConverterScreen(ConverterMenu menu, Inventory inventory, Component title) {
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
        MachineScreenLayout.drawBackground(graphics, x, y, MOSSY_COBBLESTONE, THEME);
        MachineScreenLayout.drawSlot(
                graphics,
                x,
                y,
                ConverterMenu.VILLAGER_SLOT_X,
                ConverterMenu.VILLAGER_SLOT_Y,
                THEME
        );
        if (!menu.getSlot(ConverterBlockEntity.VILLAGER_SLOT).hasItem()) {
            MachineScreenLayout.drawEmptySlotSprite(
                    graphics,
                    x,
                    y,
                    ConverterMenu.VILLAGER_SLOT_X,
                    ConverterMenu.VILLAGER_SLOT_Y,
                    MachineSlotSprites.CAPTURER
            );
        }
        for (int index = 0; index < ConverterBlockEntity.POTION_SLOT_COUNT; index++) {
            int slotX = MachineScreenLayout.machineX(43) + index * 24;
            MachineScreenLayout.drawSlot(graphics, x, y, slotX, 35, THEME);
            if (!menu.getSlot(ConverterBlockEntity.FIRST_POTION_SLOT + index).hasItem()) {
                MachineScreenLayout.drawEmptySlotSprite(
                        graphics,
                        x,
                        y,
                        slotX,
                        35,
                        MachineSlotSprites.WEAKNESS_POTION
                );
            }
        }
        for (int index = 0; index < ConverterBlockEntity.APPLE_SLOT_COUNT; index++) {
            int slotX = MachineScreenLayout.machineX(43) + index * 24;
            MachineScreenLayout.drawSlot(graphics, x, y, slotX, 63, THEME);
            if (!menu.getSlot(ConverterBlockEntity.FIRST_APPLE_SLOT + index).hasItem()) {
                MachineScreenLayout.drawEmptySlotSprite(
                        graphics,
                        x,
                        y,
                        slotX,
                        63,
                        MachineSlotSprites.GOLDEN_APPLE
                );
            }
        }
        MachineScreenLayout.drawProgressFrame(graphics, x + PROGRESS_FRAME_X, y + PROGRESS_FRAME_Y, THEME);

        ConverterStage stage = menu.stage();
        graphics.centeredText(
                font,
                Component.translatable(stageKey(stage)),
                x + MachineScreenLayout.machineX(88),
                y + 83,
                THEME.titleText()
        );
        if (stage.isProcessing()) {
            int progress = Math.min(PROGRESS_WIDTH, menu.stageTicks() * PROGRESS_WIDTH / menu.maxStageTicks());
            if (progress > 0) {
                graphics.fill(
                        x + PROGRESS_X,
                        y + PROGRESS_Y,
                        x + PROGRESS_X + progress,
                        y + PROGRESS_Y + PROGRESS_HEIGHT,
                        0xFF8E5AA7
                );
            }
            MachineScreenUtil.drawCenteredCountdown(
                    graphics,
                    font,
                    x + PROGRESS_X + PROGRESS_WIDTH / 2,
                    y + PROGRESS_Y - 1,
                    menu.stageTicks(),
                    menu.maxStageTicks()
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineScreenLayout.drawLabels(graphics, font, title, playerInventoryTitle, THEME);
    }

    private static String stageKey(ConverterStage stage) {
        return switch (stage) {
            case IDLE -> "label.trading_cells.converter_ready";
            case INFECTING -> "label.trading_cells.converter_infecting";
            case CURING -> "label.trading_cells.converter_curing";
        };
    }
}
