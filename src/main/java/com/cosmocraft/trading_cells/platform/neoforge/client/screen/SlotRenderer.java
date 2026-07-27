package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Canonical 18x18 slot frame with a 16x16 item area at {@code +1,+1}. */
public final class SlotRenderer {
    public static final int FRAME_SIZE = 18;
    public static final int ITEM_SIZE = 16;
    public static final int ITEM_OFFSET = 1;

    private SlotRenderer() {
    }

    public static void drawAtFramePosition(
            GuiGraphicsExtractor graphics,
            int screenX,
            int screenY,
            int frameX,
            int frameY,
            Palette palette
    ) {
        int left = screenX + frameX;
        int top = screenY + frameY;
        int right = left + FRAME_SIZE;
        int bottom = top + FRAME_SIZE;

        graphics.fill(left, top, right, bottom, palette.outer());
        graphics.fill(
                left + ITEM_OFFSET,
                top + ITEM_OFFSET,
                right - ITEM_OFFSET,
                bottom - ITEM_OFFSET,
                palette.inner()
        );
        graphics.fill(left, top, right, top + 1, palette.shadow());
        graphics.fill(left, top, left + 1, bottom, palette.shadow());
        graphics.fill(left, bottom - 1, right, bottom, palette.highlight());
        graphics.fill(right - 1, top, right, bottom, palette.highlight());
    }

    public static void drawAtItemPosition(
            GuiGraphicsExtractor graphics,
            int screenX,
            int screenY,
            int itemX,
            int itemY,
            Palette palette
    ) {
        drawAtFramePosition(
                graphics,
                screenX,
                screenY,
                itemX - ITEM_OFFSET,
                itemY - ITEM_OFFSET,
                palette
        );
    }

    public record Palette(int outer, int shadow, int inner, int highlight) {
    }
}
