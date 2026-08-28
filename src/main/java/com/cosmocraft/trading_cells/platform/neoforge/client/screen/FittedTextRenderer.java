package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Draws one-line labels at their native size, shrinking only when their bounds require it. */
public final class FittedTextRenderer {
    private FittedTextRenderer() {
    }

    public static void centered(
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
        centeredAtMost(graphics, font, text, minX, maxX, y, height, color, shadow, 1.0F);
    }

    public static void centeredAtMost(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int minX,
            int maxX,
            int y,
            int height,
            int color,
            boolean shadow,
            float maximumScale
    ) {
        draw(graphics, font, text, minX, maxX, y, height, color, shadow, true, maximumScale);
    }

    public static void left(
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
        draw(graphics, font, text, minX, maxX, y, height, color, shadow, false, 1.0F);
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
            boolean centered,
            float maximumScale
    ) {
        int availableWidth = Math.max(1, maxX - minX);
        int textWidth = Math.max(1, font.width(text));
        float scale = Math.min(maximumScale, availableWidth / (float) textWidth);
        float textX = centered ? (minX + maxX) / 2.0F : minX;
        float textY = y + (height - font.lineHeight * scale) / 2.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, textY);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, centered ? -textWidth / 2 : 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }
}
