package com.cosmocraft.trading_cells.feature.quarry.domain.model;

/** Fixed vanilla anchors used to classify compatible modded pickaxes. */
public enum VanillaPickaxeTier {
    WOODEN(2.0D, 0.0D),
    STONE(4.0D, 2.0D),
    COPPER(5.0D, 3.0D),
    IRON(6.0D, 4.0D),
    DIAMOND(8.0D, 5.0D),
    NETHERITE(9.0D, 6.0D),
    GOLDEN(12.0D, 1.0D);

    private final double miningSpeed;
    private final double timingPosition;

    VanillaPickaxeTier(double miningSpeed, double timingPosition) {
        this.miningSpeed = miningSpeed;
        this.timingPosition = timingPosition;
    }

    public double miningSpeed() {
        return miningSpeed;
    }

    public double timingPosition() {
        return timingPosition;
    }
}
