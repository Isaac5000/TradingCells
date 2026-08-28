package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Draws a clipped vanilla block texture without coupling it to a particular menu. */
public final class TiledSurfaceRenderer {
    private static final int TILE_SIZE = 16;

    private TiledSurfaceRenderer() {
    }

    public static void draw(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x,
            int y,
            int width,
            int height,
            int overlayColor
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
        graphics.fill(x, y, x + width, y + height, overlayColor);
    }
}
