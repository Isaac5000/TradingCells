package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;

/** Independent copy of the neutral trader palette for current and future mob-farm screens. */
public record ZombieFarmGuiThemeColors(
        int scrollTrack,
        int scrollThumbLight,
        SlotRenderer.Palette slotPalette
) {
    private static final ZombieFarmGuiThemeColors NEUTRAL = new ZombieFarmGuiThemeColors(
            0xFF555555,
            0xFFDADADA,
            new SlotRenderer.Palette(
                    0xFF2F2F2F,
                    0xFF575757,
                    0xFFBEBEBE,
                    0xFFE2E2E2
            )
    );

    public static ZombieFarmGuiThemeColors resolve() {
        return NEUTRAL;
    }
}
