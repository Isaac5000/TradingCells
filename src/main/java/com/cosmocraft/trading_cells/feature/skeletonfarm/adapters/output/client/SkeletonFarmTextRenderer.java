package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Draws translated Skeleton Farm labels on one line, shrinking only when their container requires it. */
final class SkeletonFarmTextRenderer {
    private SkeletonFarmTextRenderer() {
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
        draw(graphics, font, text, minX, maxX, y, height, color, shadow, true);
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
        draw(graphics, font, text, minX, maxX, y, height, color, shadow, false);
    }

    private static void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int minX,
            int maxX,
            int y,
            int height,
            int color,
            boolean shadow,
            boolean centered
    ) {
        int availableWidth = Math.max(1, maxX - minX);
        int textWidth = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, availableWidth / (float) textWidth);
        float textX = centered ? (minX + maxX) / 2.0F : minX;
        float textY = y + (height - font.lineHeight * scale) / 2.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, textY);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, centered ? -textWidth / 2 : 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }
}
