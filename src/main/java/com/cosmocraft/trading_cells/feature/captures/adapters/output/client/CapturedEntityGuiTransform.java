package com.cosmocraft.trading_cells.feature.captures.adapters.output.client;

/**
 * Preserves the visual X center when a GUI entity uses a different scale.
 *
 * <p>The capturer renderers apply scale before translation, so translation is
 * scaled too. This value is the adult's existing effective center:
 * {@code 0.38 * 1.3 = 0.494}.</p>
 */
public final class CapturedEntityGuiTransform {
    private static final double GUI_EFFECTIVE_CENTER_X = 0.494D;

    private CapturedEntityGuiTransform() {
    }

    public static double translationX(float scale) {
        if (scale <= 0.0F) {
            throw new IllegalArgumentException("GUI entity scale must be positive");
        }
        return GUI_EFFECTIVE_CENTER_X / scale;
    }

    public static double effectiveCenterX(float scale) {
        return scale * translationX(scale);
    }
}
