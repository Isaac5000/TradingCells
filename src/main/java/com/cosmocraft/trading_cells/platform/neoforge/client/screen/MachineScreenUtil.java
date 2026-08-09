package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MachineScreenUtil {
    private MachineScreenUtil() {
    }

    public static String remainingTime(int progressTicks, int maxTicks) {
        int remainingTicks = Math.max(0, maxTicks - progressTicks);
        return formatDuration(remainingTicks);
    }

    public static String formatDuration(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return "%d:%02d".formatted(totalSeconds / 60, totalSeconds % 60);
    }

    public static void drawCenteredCountdown(
            GuiGraphicsExtractor graphics,
            Font font,
            int centerX,
            int y,
            int progressTicks,
            int maxTicks
    ) {
        String text = remainingTime(progressTicks, maxTicks);
        graphics.text(font, text, centerX - font.width(text) / 2, y + 2, 0xFFFFFFFF, true);
    }
}
