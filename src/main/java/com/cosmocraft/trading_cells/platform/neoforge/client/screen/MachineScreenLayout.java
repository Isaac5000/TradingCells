package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Shared geometry and rendering for every custom machine screen. */
public final class MachineScreenLayout {
    public static final int WIDTH = MachineMenuLayout.WIDTH;
    public static final int HEIGHT = MachineMenuLayout.HEIGHT;
    public static final int CONTENT_X = MachineMenuLayout.CONTENT_X;
    public static final int CONTENT_WIDTH = MachineMenuLayout.CONTENT_WIDTH;

    public static final int TITLE_Y = MachineMenuLayout.TITLE_Y;
    public static final int PLAYER_INVENTORY_X = MachineMenuLayout.PLAYER_INVENTORY_X;
    public static final int PLAYER_INVENTORY_SLOT_Y = MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y;
    public static final int PLAYER_INVENTORY_LABEL_X = MachineMenuLayout.PLAYER_INVENTORY_LABEL_X;
    public static final int PLAYER_INVENTORY_LABEL_Y = MachineMenuLayout.PLAYER_INVENTORY_LABEL_Y;

    public static final int PROGRESS_FRAME_WIDTH = 69;
    public static final int PROGRESS_FRAME_HEIGHT = 13;
    /** The fill starts inside the frame, one pixel farther right than before. */
    public static final int PROGRESS_FILL_X_OFFSET = 2;
    /** The fill now reaches the top interior row of the frame. */
    public static final int PROGRESS_FILL_Y_OFFSET = 2;
    /** One pixel shorter so the right end remains inside the decorative frame. */
    public static final int PROGRESS_FILL_WIDTH = 66;
    /** Fills the complete nine-pixel interior, including its bottom row. */
    public static final int PROGRESS_FILL_HEIGHT = 9;

    private static final int TILE_SIZE = 16;
    private static final int MACHINE_PANEL_X = 4;
    private static final int MACHINE_PANEL_Y = 22;
    private static final int MACHINE_PANEL_WIDTH = WIDTH - 8;
    private static final int MACHINE_PANEL_HEIGHT = 95;

    private MachineScreenLayout() {
    }

    public static int machineX(int localX) {
        return MachineMenuLayout.machineX(localX);
    }

    public static void drawBackground(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Identifier surfaceTexture,
            MachineScreenTheme theme
    ) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                theme.backgroundTexture(),
                x,
                y,
                0.0F,
                0.0F,
                WIDTH,
                HEIGHT,
                WIDTH,
                HEIGHT
        );
        drawMachineSurface(graphics, x, y, surfaceTexture);
        drawPlayerInventory(graphics, x, y, theme);
        drawEquipmentPanel(graphics, x, y, theme);
    }

    public static void drawLabels(
            GuiGraphicsExtractor graphics,
            Font font,
            Component title,
            Component inventoryTitle,
            MachineScreenTheme theme
    ) {
        graphics.text(
                font,
                title,
                (WIDTH - font.width(title)) / 2,
                TITLE_Y,
                theme.titleText(),
                true
        );
        graphics.text(
                font,
                inventoryTitle,
                PLAYER_INVENTORY_LABEL_X,
                PLAYER_INVENTORY_LABEL_Y,
                theme.titleText(),
                false
        );
    }

    private static void drawPlayerInventory(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            MachineScreenTheme theme
    ) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                        graphics,
                        x,
                        y,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_SLOT_Y + row * 18,
                        theme
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(
                    graphics,
                    x,
                    y,
                    PLAYER_INVENTORY_X + column * 18,
                    PLAYER_INVENTORY_SLOT_Y + 58,
                    theme
            );
        }
    }

    private static void drawEquipmentPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            MachineScreenTheme theme
    ) {
        drawSlot(graphics, x, y, PlayerEquipmentSlots.X, PlayerEquipmentSlots.HEAD_Y, theme);
        drawSlot(graphics, x, y, PlayerEquipmentSlots.X, PlayerEquipmentSlots.CHEST_Y, theme);
        drawSlot(graphics, x, y, PlayerEquipmentSlots.X, PlayerEquipmentSlots.LEGS_Y, theme);
        drawSlot(graphics, x, y, PlayerEquipmentSlots.X, PlayerEquipmentSlots.FEET_Y, theme);
        drawSlot(graphics, x, y, PlayerEquipmentSlots.X, PlayerEquipmentSlots.OFFHAND_Y, theme);
    }

    /**
     * Draws an 18x18 slot whose one-pixel frame is entirely outside the
     * 16x16 item area. Items therefore never cover the decorative border.
     */
    public static void drawSlot(
            GuiGraphicsExtractor graphics,
            int screenX,
            int screenY,
            int slotX,
            int slotY,
            MachineScreenTheme theme
    ) {
        SlotRenderer.drawAtItemPosition(
                graphics,
                screenX,
                screenY,
                slotX,
                slotY,
                new SlotRenderer.Palette(
                        theme.slot(),
                        theme.frameDark(),
                        theme.slotInner(),
                        theme.slotHighlight()
                )
        );
    }

    public static void drawEmptySlotSprite(
            GuiGraphicsExtractor graphics,
            int screenX,
            int screenY,
            int slotX,
            int slotY,
            Identifier sprite
    ) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                screenX + slotX,
                screenY + slotY,
                16,
                16
        );
    }

    public static void drawProgressFrame(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            MachineScreenTheme theme
    ) {
        graphics.fill(x, y, x + PROGRESS_FRAME_WIDTH, y + PROGRESS_FRAME_HEIGHT, theme.frameDark());
        graphics.fill(x + 1, y + 1, x + PROGRESS_FRAME_WIDTH - 1, y + PROGRESS_FRAME_HEIGHT - 1, theme.frameLight());
        graphics.fill(x + 2, y + 2, x + PROGRESS_FRAME_WIDTH - 2, y + PROGRESS_FRAME_HEIGHT - 2, theme.slotInner());
    }

    private static void drawMachineSurface(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Identifier surfaceTexture
    ) {
        int panelX = x + MACHINE_PANEL_X;
        int panelY = y + MACHINE_PANEL_Y;
        tile(graphics, surfaceTexture, panelX, panelY, MACHINE_PANEL_WIDTH, MACHINE_PANEL_HEIGHT);
        graphics.fill(panelX, panelY, panelX + MACHINE_PANEL_WIDTH, panelY + MACHINE_PANEL_HEIGHT, 0x24000000);
    }

    private static void tile(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x,
            int y,
            int width,
            int height
    ) {
        for (int offsetY = 0; offsetY < height; offsetY += TILE_SIZE) {
            int tileHeight = Math.min(TILE_SIZE, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += TILE_SIZE) {
                int tileWidth = Math.min(TILE_SIZE, width - offsetX);
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        x + offsetX,
                        y + offsetY,
                        0.0F,
                        0.0F,
                        tileWidth,
                        tileHeight,
                        TILE_SIZE,
                        TILE_SIZE
                );
            }
        }
    }
}
