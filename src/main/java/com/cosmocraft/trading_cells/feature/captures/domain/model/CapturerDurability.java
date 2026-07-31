package com.cosmocraft.trading_cells.feature.captures.domain.model;

public final class CapturerDurability {
    public static final int DEFAULT_MAX_DAMAGE = 10;

    private CapturerDurability() {
    }

    public static int maximum(int configuredMaximum) {
        return Math.max(1, configuredMaximum);
    }
}
