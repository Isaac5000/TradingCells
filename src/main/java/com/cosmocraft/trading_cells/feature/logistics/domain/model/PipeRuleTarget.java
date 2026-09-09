package com.cosmocraft.trading_cells.feature.logistics.domain.model;

/** Stable coordinates, independent of whether the destination block is currently present. */
public record PipeRuleTarget(String dimension, int x, int y, int z) {
    public PipeRuleTarget {
        if (dimension == null || dimension.isBlank() || dimension.length() > 256
                || Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000
                || y < -2048 || y > 2047) {
            throw new IllegalArgumentException("Invalid logistics target");
        }
    }

    public boolean matches(String dimension, int x, int y, int z) {
        return this.dimension.equals(dimension) && this.x == x && this.y == y && this.z == z;
    }
}
