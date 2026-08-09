package com.cosmocraft.trading_cells.feature.farmer.domain.model;

import java.util.Objects;

public record FarmerYield(FarmerProduct product, int count, int chanceBasisPoints) {
    public static final int CHANCE_SCALE = 10_000;

    public FarmerYield {
        Objects.requireNonNull(product);
        if (count < 1) {
            throw new IllegalArgumentException("Farmer yield count must be positive");
        }
        if (chanceBasisPoints < 1 || chanceBasisPoints > CHANCE_SCALE) {
            throw new IllegalArgumentException("Farmer yield chance must be between 1 and 10000");
        }
    }

    public static FarmerYield guaranteed(FarmerProduct product, int count) {
        return new FarmerYield(product, count, CHANCE_SCALE);
    }

    public static FarmerYield chance(FarmerProduct product, int count, int chanceBasisPoints) {
        return new FarmerYield(product, count, chanceBasisPoints);
    }

    public boolean isGuaranteed() {
        return chanceBasisPoints == CHANCE_SCALE;
    }

    public boolean succeeds(int roll) {
        return isGuaranteed() || Math.clamp(roll, 0, CHANCE_SCALE - 1) < chanceBasisPoints;
    }
}
