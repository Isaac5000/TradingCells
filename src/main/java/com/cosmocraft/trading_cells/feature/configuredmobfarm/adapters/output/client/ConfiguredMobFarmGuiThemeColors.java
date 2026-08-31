package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;

/** Independent copy of the neutral trader palette for current and future mob-farm screens. */
public record ConfiguredMobFarmGuiThemeColors(
        int scrollTrack,
        int scrollThumbLight,
        SlotRenderer.Palette slotPalette
) {
    private static final ConfiguredMobFarmGuiThemeColors NEUTRAL = new ConfiguredMobFarmGuiThemeColors(
            0xFF555555,
            0xFFDADADA,
            new SlotRenderer.Palette(
                    0xFF2F2F2F,
                    0xFF575757,
                    0xFFBEBEBE,
                    0xFFE2E2E2
            )
    );

    public static ConfiguredMobFarmGuiThemeColors resolve() {
        return NEUTRAL;
    }
}
