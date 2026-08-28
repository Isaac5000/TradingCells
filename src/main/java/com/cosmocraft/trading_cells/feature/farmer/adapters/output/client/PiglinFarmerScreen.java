package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerMenu;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.PiglinFarmerMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineSlotSprites;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;
import com.cosmocraft.trading_cells.platform.neoforge.client.screen.TiledSurfaceRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class PiglinFarmerScreen extends AbstractContainerScreen<FarmerMenu> {
    public static final int RECIPE_VIEWER_X = 199;
    public static final int RECIPE_VIEWER_Y = 30;
    public static final int RECIPE_VIEWER_WIDTH = 69;
    public static final int RECIPE_VIEWER_HEIGHT = 13;
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "textures/gui/farmer/piglin_background.png"
    );
    private static final Identifier SURFACE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/netherrack.png"
    );
    private static final Identifier SURFACE_OVERLAY = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "textures/gui/farmer/piglin_surface_overlay.png"
    );
    private static final Identifier CRIMSON_FUNGUS = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/block/crimson_fungus.png"
    );
    private static final SlotRenderer.Palette SLOT_PALETTE = new SlotRenderer.Palette(
            0xFF2F2F2F,
            0xFF575757,
            0xFFBEBEBE,
            0xFFE2E2E2
    );
    private static final int LEFT_SURFACE_X = 6;
    private static final int LEFT_SURFACE_Y = 26;
    private static final int LEFT_SURFACE_WIDTH = 112;
    private static final int LEFT_SURFACE_HEIGHT = 179;
    private static final int MACHINE_SURFACE_X = 124;
    private static final int MACHINE_SURFACE_Y = 26;
    private static final int MACHINE_SURFACE_WIDTH = 218;
    private static final int MACHINE_SURFACE_HEIGHT = 75;
    private static final int[] INPUT_ROW_Y = {29, 64, 99};
    private static final int INPUT_ROW_X = 10;
    private static final int INPUT_ROW_WIDTH = 103;
    private static final int INPUT_ROW_HEIGHT = 32;
    private static final int OUTPUT_HEADER_X = 151;
    private static final int OUTPUT_HEADER_Y = 51;
    private static final int OUTPUT_HEADER_WIDTH = 162;
    private static final int OUTPUT_HEADER_HEIGHT = 11;
    private static final int OUTPUT_LABEL_HEIGHT = 13;
    private static final int INVENTORY_HEADER_X = 162;
    private static final int INVENTORY_HEADER_Y = 105;
    private static final int INVENTORY_HEADER_WIDTH = 162;
    private static final int INVENTORY_HEADER_HEIGHT = 11;
    private static final int INVENTORY_LABEL_HEIGHT = 13;
    private static final int TEXT_DARK = 0xFF303436;
    private static final int PANEL_DARK = 0xFF4A4E50;
    private static final int PANEL_LIGHT = 0xFFAEB3B5;
    private static final int ACCENT = 0xFF8D4652;

    public PiglinFarmerScreen(FarmerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PiglinFarmerMenuLayout.WIDTH, PiglinFarmerMenuLayout.HEIGHT);
        titleLabelY = 8;
        inventoryLabelX = 164;
        inventoryLabelY = 108;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackground(graphics);
        drawSurfaces(graphics);
        drawSurfaceOverlay(graphics);
        drawOutputHeader(graphics);
        drawInventoryHeader(graphics);
        drawInputRows(graphics);
        drawInventorySlots(graphics);
        drawMachineSlots(graphics);
        drawProgress(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabels(graphics);
    }

    private void drawBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                PiglinFarmerMenuLayout.WIDTH,
                PiglinFarmerMenuLayout.HEIGHT,
                PiglinFarmerMenuLayout.ATLAS_WIDTH,
                PiglinFarmerMenuLayout.ATLAS_HEIGHT
        );
    }

    private void drawSurfaces(GuiGraphicsExtractor graphics) {
        TiledSurfaceRenderer.draw(graphics, SURFACE, leftPos + LEFT_SURFACE_X, topPos + LEFT_SURFACE_Y,
                LEFT_SURFACE_WIDTH, LEFT_SURFACE_HEIGHT, 0x00000000);
        TiledSurfaceRenderer.draw(graphics, SURFACE, leftPos + MACHINE_SURFACE_X, topPos + MACHINE_SURFACE_Y,
                MACHINE_SURFACE_WIDTH, MACHINE_SURFACE_HEIGHT, 0x00000000);
    }

    private void drawSurfaceOverlay(GuiGraphicsExtractor graphics) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SURFACE_OVERLAY,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                PiglinFarmerMenuLayout.WIDTH,
                PiglinFarmerMenuLayout.HEIGHT,
                PiglinFarmerMenuLayout.WIDTH,
                PiglinFarmerMenuLayout.HEIGHT
        );
    }

    private void drawInputRows(GuiGraphicsExtractor graphics) {
        for (int rowY : INPUT_ROW_Y) {
            int x = leftPos + INPUT_ROW_X;
            int y = topPos + rowY;
            graphics.fill(x, y, x + INPUT_ROW_WIDTH, y + INPUT_ROW_HEIGHT, PANEL_DARK);
            graphics.fill(x + 1, y + 1, x + INPUT_ROW_WIDTH - 1, y + INPUT_ROW_HEIGHT - 1, PANEL_LIGHT);
            graphics.fill(x + 1, y + 1, x + 4, y + INPUT_ROW_HEIGHT - 1, ACCENT);
        }
    }

    private void drawOutputHeader(GuiGraphicsExtractor graphics) {
        int x = leftPos + OUTPUT_HEADER_X;
        int y = topPos + OUTPUT_HEADER_Y;
        graphics.fill(x, y, x + OUTPUT_HEADER_WIDTH, y + OUTPUT_HEADER_HEIGHT, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + OUTPUT_HEADER_WIDTH - 1, y + OUTPUT_HEADER_HEIGHT - 1, PANEL_LIGHT);
    }

    private void drawInventoryHeader(GuiGraphicsExtractor graphics) {
        int x = leftPos + INVENTORY_HEADER_X;
        int y = topPos + INVENTORY_HEADER_Y;
        graphics.fill(x, y, x + INVENTORY_HEADER_WIDTH, y + INVENTORY_HEADER_HEIGHT, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + INVENTORY_HEADER_WIDTH - 1, y + INVENTORY_HEADER_HEIGHT - 1, PANEL_LIGHT);
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
        FittedTextRenderer.centered(graphics, font, title, 126, 340, 8, 18, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, playerInventoryTitle,
                INVENTORY_HEADER_X, INVENTORY_HEADER_X + INVENTORY_HEADER_WIDTH,
                INVENTORY_HEADER_Y, INVENTORY_LABEL_HEIGHT, TEXT_DARK, false);
        FittedTextRenderer.centered(graphics, font, Component.translatable("gui.trading_cells.outputs"),
                OUTPUT_HEADER_X, OUTPUT_HEADER_X + OUTPUT_HEADER_WIDTH,
                OUTPUT_HEADER_Y, OUTPUT_LABEL_HEIGHT, TEXT_DARK, false);
        drawInputLabel(graphics, "gui.trading_cells.input.piglin", INPUT_ROW_Y[0]);
        drawInputLabel(graphics, "gui.trading_cells.input.hoe", INPUT_ROW_Y[1]);
        drawInputLabel(graphics, "gui.trading_cells.input.crop", INPUT_ROW_Y[2]);
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
