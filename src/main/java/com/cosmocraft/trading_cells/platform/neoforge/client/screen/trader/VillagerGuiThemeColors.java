package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.SlotRenderer;

/** Pixel-art colors used by dynamic controls that cannot be baked into the shared texture. */
public record VillagerGuiThemeColors(
        int slotOuter,
        int slotLight,
        int slotInner,
        int slotDark,
        int scrollTrack,
        int scrollBorder,
        int scrollThumb,
        int scrollThumbLight,
        int accent
) {
    private static final VillagerGuiThemeColors NEUTRAL = colors(
            0xFF2F2F2F,
            0xFFE2E2E2,
            0xFFBEBEBE,
            0xFF575757,
            0xFF555555,
            0xFF353535,
            0xFFB5B5B5,
            0xFFDADADA,
            0xFF7E9D64
    );

    public static VillagerGuiThemeColors resolve() {
        return NEUTRAL;
    }

    public SlotRenderer.Palette slotPalette() {
        return new SlotRenderer.Palette(
                slotOuter,
                slotDark,
                slotInner,
                slotLight
        );
    }

    private static VillagerGuiThemeColors colors(
            int slotOuter,
            int slotLight,
            int slotInner,
            int slotDark,
            int scrollTrack,
            int scrollBorder,
            int scrollThumb,
            int scrollThumbLight,
            int accent
    ) {
        return new VillagerGuiThemeColors(
                slotOuter,
                slotLight,
                slotInner,
                slotDark,
                scrollTrack,
                scrollBorder,
                scrollThumb,
                scrollThumbLight,
                accent
        );
    }
}
