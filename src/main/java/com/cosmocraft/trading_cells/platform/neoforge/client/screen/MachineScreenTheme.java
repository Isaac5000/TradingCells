package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.Locale;
import net.minecraft.resources.Identifier;

/**
 * Visual palette shared by the custom block-entity screens.
 *
 * <p>Keeping the palette in one place prevents every screen from inventing a
 * slightly different inventory frame and makes the GUI easier to extend.</p>
 */
public enum MachineScreenTheme {
    VILLAGER_BREEDER(
            0xFF4A2E17, 0xFF8B5B2C, 0xFFD8A75D,
            0xFF4A2E17, 0xFFC18A4B, 0xFFF0C77F,
            0xFFFFF0D2, 0xFF34200F
    ),
    PIGLIN_BREEDER(
            0xFF4B101C, 0xFF8B2337, 0xFFD15A68,
            0xFF4B101C, 0xFFB64858, 0xFFF2919C,
            0xFFFFE7E9, 0xFF2E0B12
    ),
    CONVERTER(
            0xFF28433A, 0xFF4E7463, 0xFF91B79E,
            0xFF28433A, 0xFF729984, 0xFFC1DDC9,
            0xFFF0FFF7, 0xFF1A2D27
    ),
    FARMER(
            0xFF3A4215, 0xFF6D7A25, 0xFFA6B94A,
            0xFF3A4215, 0xFF879838, 0xFFD0DD79,
            0xFFF8FFD7, 0xFF252A0E
    ),
    VILLAGER_INCUBATOR(
            0xFF624214, 0xFFA87520, 0xFFF0C553,
            0xFF624214, 0xFFC79838, 0xFFFFE18A,
            0xFFFFF4C9, 0xFF3D290C
    ),
    PIGLIN_INCUBATOR(
            0xFF551526, 0xFF932A43, 0xFFDC6679,
            0xFF551526, 0xFFB94860, 0xFFF59AAD,
            0xFFFFE8EC, 0xFF330C17
    ),
    IRON_FARM(
            0xFF34383B, 0xFF62686C, 0xFFAEB5B9,
            0xFF34383B, 0xFF858C90, 0xFFD9DEE0,
            0xFFF4F7F8, 0xFF24282A
    );

    private final int frameDark;
    private final int frame;
    private final int frameLight;
    private final int slot;
    private final int slotInner;
    private final int slotHighlight;
    private final int titleText;
    private final int bodyText;

    MachineScreenTheme( // NOSONAR - Enum palette entries require the complete color tuple.
            int frameDark,
            int frame,
            int frameLight,
            int slot,
            int slotInner,
            int slotHighlight,
            int titleText,
            int bodyText
    ) {
        this.frameDark = frameDark;
        this.frame = frame;
        this.frameLight = frameLight;
        this.slot = slot;
        this.slotInner = slotInner;
        this.slotHighlight = slotHighlight;
        this.titleText = titleText;
        this.bodyText = bodyText;
    }

    public int frameDark() {
        return frameDark;
    }

    public int frame() {
        return frame;
    }

    public int frameLight() {
        return frameLight;
    }

    public int slot() {
        return slot;
    }

    public int slotInner() {
        return slotInner;
    }

    public int slotHighlight() {
        return slotHighlight;
    }

    public int titleText() {
        return titleText;
    }

    public int bodyText() {
        return bodyText;
    }

    public Identifier backgroundTexture() {
        return Identifier.fromNamespaceAndPath(
                TradingCells.MOD_ID,
                "textures/gui/machines/backgrounds/" + name().toLowerCase(Locale.ROOT) + ".png"
        );
    }
}
