package com.cosmocraft.trading_cells.feature.mobfarm.domain.model;

public final class SyringeAnimation {
    public static final int APPROACH_TICKS = 6;
    public static final float APPROACH_FRACTION = 0.25F;

    private SyringeAnimation() { }

    public static int extractionTicks(int tier) { return 40 * Math.clamp(tier, 1, 4); }

    public static float armExtension(float elapsedTicks) {
        float approach = Math.clamp(elapsedTicks / APPROACH_TICKS, 0, 1);
        return approach * approach * (3 - 2 * approach);
    }

    public static float modelProgress(int tier, float elapsedTicks) {
        float elapsed = Math.clamp(elapsedTicks, 0, extractionTicks(tier));
        return elapsed <= APPROACH_TICKS ? APPROACH_FRACTION * elapsed / APPROACH_TICKS
                : APPROACH_FRACTION + (1 - APPROACH_FRACTION) * (elapsed - APPROACH_TICKS)
                        / (extractionTicks(tier) - APPROACH_TICKS);
    }
}
