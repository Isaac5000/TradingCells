package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerMenu;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.PiglinFarmerMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class PiglinFarmerScreen extends MachineContainerScreen<FarmerMenu> {
    public static final int RECIPE_VIEWER_X = 199;
    public static final int RECIPE_VIEWER_Y = 21;
    public static final int RECIPE_VIEWER_WIDTH = 69;
    public static final int RECIPE_VIEWER_HEIGHT = 13;
    private static final Identifier SURFACE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/netherrack.png"
    );
    private static final Identifier CRIMSON_FUNGUS = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/crimson_fungus.png"
    );
    private static final SlotRenderer.Palette SLOT_PALETTE = new SlotRenderer.Palette(
            0xFF2F2F2F,
            0xFF575757,
            0xFF343D41,
            0xFF869397
    );
    private static final int[] INPUT_ROW_Y = {29, 64, 99, 134};
    private static final int INPUT_ROW_X = 10;
    private static final int INPUT_ROW_WIDTH = 103;
    private static final int INPUT_ROW_HEIGHT = 32;
    private static final int OUTPUT_HEADER_X = 151;
    private static final int OUTPUT_HEADER_Y = 36;
    private static final int OUTPUT_HEADER_WIDTH = 162;
    private static final int OUTPUT_HEADER_HEIGHT = 11;
    private static final int OUTPUT_LABEL_HEIGHT = 13;
    private static final int INVENTORY_HEADER_X = 162;
    private static final int INVENTORY_HEADER_Y = 111;
    private static final int INVENTORY_HEADER_WIDTH = 162;
    private static final int INVENTORY_HEADER_HEIGHT = 11;
    private static final int INVENTORY_LABEL_HEIGHT = 13;
    private static final int TEXT_DARK = 0xFFE0E5E6;
    private static final int PANEL_DARK = 0xFF566166;
    private static final int PANEL_LIGHT = 0xFF343D41;
    private static final int ACCENT = 0xFF8D4652;

    public PiglinFarmerScreen(FarmerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PiglinFarmerMenuLayout.WIDTH, PiglinFarmerMenuLayout.HEIGHT);
        titleLabelY = 8;
        inventoryLabelX = 164;
        inventoryLabelY = 112;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackground(graphics);
        drawInventorySlots(graphics);
        drawMachineSlots(graphics);
        drawProgress(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabels(graphics);
    }

    private void drawBackground(GuiGraphicsExtractor graphics) {
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineGuiSkin.PIGLIN_FARMER.draw(
                graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    private void drawInventorySlots(GuiGraphicsExtractor graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, PiglinFarmerMenuLayout.PLAYER_INVENTORY_X + column * 18,
                        PiglinFarmerMenuLayout.PLAYER_INVENTORY_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, PiglinFarmerMenuLayout.PLAYER_INVENTORY_X + column * 18,
                    PiglinFarmerMenuLayout.PLAYER_HOTBAR_Y);
        }
        drawSlot(graphics, PiglinFarmerMenuLayout.EQUIPMENT_X, PiglinFarmerMenuLayout.EQUIPMENT_HEAD_Y);
        drawSlot(graphics, PiglinFarmerMenuLayout.EQUIPMENT_X, PiglinFarmerMenuLayout.EQUIPMENT_CHEST_Y);
        drawSlot(graphics, PiglinFarmerMenuLayout.EQUIPMENT_X, PiglinFarmerMenuLayout.EQUIPMENT_LEGS_Y);
        drawSlot(graphics, PiglinFarmerMenuLayout.EQUIPMENT_X, PiglinFarmerMenuLayout.EQUIPMENT_FEET_Y);
        drawSlot(graphics, PiglinFarmerMenuLayout.EQUIPMENT_X, PiglinFarmerMenuLayout.EQUIPMENT_OFFHAND_Y);
    }

    private void drawMachineSlots(GuiGraphicsExtractor graphics) {
        drawSlot(graphics, 17, 177);
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotGhosts.upgrade(
                graphics, menu.getSlot(FarmerBlockEntity.CAPACITY_SLOT), leftPos, topPos, true);
        drawSlot(graphics, 17, 142);
        com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotGhosts.upgrade(
                graphics, menu.getSlot(FarmerBlockEntity.SPEED_SLOT), leftPos, topPos, false);
        drawSlot(graphics, PiglinFarmerMenuLayout.WORKER_SLOT_X, PiglinFarmerMenuLayout.WORKER_SLOT_Y);
        drawSlot(graphics, PiglinFarmerMenuLayout.HOE_SLOT_X, PiglinFarmerMenuLayout.HOE_SLOT_Y);
        drawSlot(graphics, PiglinFarmerMenuLayout.CROP_SLOT_X, PiglinFarmerMenuLayout.CROP_SLOT_Y);
        if (!menu.getSlot(FarmerBlockEntity.WORKER_SLOT).hasItem()) {
            drawEmptySprite(graphics, PiglinFarmerMenuLayout.WORKER_SLOT_X,
                    PiglinFarmerMenuLayout.WORKER_SLOT_Y, MachineSlotSprites.PIGLIN_HEAD);
        }
        if (!menu.getSlot(FarmerBlockEntity.HOE_SLOT).hasItem()) {
            drawEmptySprite(graphics, PiglinFarmerMenuLayout.HOE_SLOT_X,
                    PiglinFarmerMenuLayout.HOE_SLOT_Y, MachineSlotSprites.HOE);
        }
        if (!menu.getSlot(FarmerBlockEntity.CROP_SLOT).hasItem()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CRIMSON_FUNGUS,
                    leftPos + PiglinFarmerMenuLayout.CROP_SLOT_X,
                    topPos + PiglinFarmerMenuLayout.CROP_SLOT_Y,
                    0.0F, 0.0F, 16, 16, 16, 16, 0x80606060);
        }
        for (int index = 0; index < FarmerBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            drawSlot(graphics, menu.outputSlotX(index), menu.outputSlotY(index));
        }
    }

    private void drawProgress(GuiGraphicsExtractor graphics) {
        int x = leftPos + RECIPE_VIEWER_X;
        int y = topPos + RECIPE_VIEWER_Y;
        drawProgressFrame(graphics, x, y);
        if (!menu.isCultivating()) {
            return;
        }
        int fill = Math.min(RECIPE_VIEWER_WIDTH - 4,
                menu.growthTicks() * (RECIPE_VIEWER_WIDTH - 4) / menu.maxGrowthTicks());
        if (fill > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fill, y + RECIPE_VIEWER_HEIGHT - 2, 0xFFE04A4A);
        }
        MachineScreenUtil.drawCenteredCountdown(graphics, font, x + RECIPE_VIEWER_WIDTH / 2, y + 1,
                menu.growthTicks(), menu.maxGrowthTicks());
    }

    private void drawLabels(GuiGraphicsExtractor graphics) {
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.inputs"),
                9, 115, 8, 18, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, title, 126, 340, 7, 12, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, playerInventoryTitle,
                INVENTORY_HEADER_X, INVENTORY_HEADER_X + INVENTORY_HEADER_WIDTH,
                INVENTORY_HEADER_Y, INVENTORY_LABEL_HEIGHT, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.outputs"),
                OUTPUT_HEADER_X, OUTPUT_HEADER_X + OUTPUT_HEADER_WIDTH,
                OUTPUT_HEADER_Y, OUTPUT_LABEL_HEIGHT, TEXT_DARK, false);
        drawInputLabel(graphics, "gui.trading_cells.input.piglin", INPUT_ROW_Y[0]);
        drawInputLabel(graphics, "gui.trading_cells.input.hoe", INPUT_ROW_Y[1]);
        drawInputLabel(graphics, "gui.trading_cells.input.crop", INPUT_ROW_Y[2]);
        drawInputLabel(graphics, "gui.trading_cells.interaction.speed", INPUT_ROW_Y[3]);
        drawInputLabel(graphics, "gui.trading_cells.interaction.capacity", 169);
    }

    private void drawInputLabel(GuiGraphicsExtractor graphics, String key, int rowY) {
        FittedTextRenderer.centered(graphics, font, Component.translatable(key),
                39, 109, rowY, INPUT_ROW_HEIGHT, TEXT_DARK, false);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        SlotRenderer.drawAtItemPosition(graphics, leftPos, topPos, x, y, SLOT_PALETTE);
    }

    private void drawEmptySprite(GuiGraphicsExtractor graphics, int x, int y, Identifier sprite) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, leftPos + x, topPos + y, 16, 16);
    }

    private static void drawProgressFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + RECIPE_VIEWER_WIDTH, y + RECIPE_VIEWER_HEIGHT, 0xFF303436);
        graphics.fill(x + 1, y + 1, x + RECIPE_VIEWER_WIDTH - 1, y + RECIPE_VIEWER_HEIGHT - 1, 0xFF8A9093);
        graphics.fill(x + 2, y + 2, x + RECIPE_VIEWER_WIDTH - 2, y + RECIPE_VIEWER_HEIGHT - 2, 0xFF555B5E);
    }
}
