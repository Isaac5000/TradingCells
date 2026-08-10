package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

/** Fixed vanilla anchors used to place compatible modded swords. */
public enum VanillaSwordTier {
    WOODEN(0.0D),
    GOLDEN(1.0D),
    STONE(2.0D),
    COPPER(3.0D),
    IRON(4.0D),
    DIAMOND(5.0D),
    NETHERITE(6.0D);

    private final double timingPosition;

    VanillaSwordTier(double timingPosition) {
        this.timingPosition = timingPosition;
    }

    public double timingPosition() {
        return timingPosition;
    }
}
