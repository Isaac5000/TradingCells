package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;

/** Independent copy of the neutral trader palette for current and future mob-farm screens. */
public record SkeletonFarmGuiThemeColors(
        int scrollTrack,
        int scrollThumbLight,
        SlotRenderer.Palette slotPalette
) {
    private static final SkeletonFarmGuiThemeColors NEUTRAL = new SkeletonFarmGuiThemeColors(
            0xFF555555,
            0xFFDADADA,
            new SlotRenderer.Palette(
                    0xFF2F2F2F,
                    0xFF575757,
                    0xFFBEBEBE,
                    0xFFE2E2E2
            )
    );

    public static SkeletonFarmGuiThemeColors resolve() {
        return NEUTRAL;
    }
}
