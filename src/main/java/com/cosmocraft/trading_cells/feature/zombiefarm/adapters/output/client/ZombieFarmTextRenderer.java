package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.FittedTextRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Draws translated Zombie Farm labels on one line, shrinking only when their container requires it. */
final class ZombieFarmTextRenderer {
    private ZombieFarmTextRenderer() {
    }

    static void centered(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int minX,
            int maxX,
            int y,
            int height,
            int color,
            boolean shadow
    ) {
        FittedTextRenderer.centered(graphics, font, text, minX, maxX, y, height, color, shadow);
    }

    static void left(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int minX,
            int maxX,
            int y,
            int height,
            int color,
            boolean shadow
    ) {
        FittedTextRenderer.left(graphics, font, text, minX, maxX, y, height, color, shadow);
    }
}
