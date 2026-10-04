package com.cosmocraft.trading_cells.feature.farmer.domain.model;

import java.util.Arrays;
import java.util.List;

public record FarmerHarvest(List<FarmerYield> yields) {
    public static final int MAX_DISTINCT_OUTPUTS = 27;

    public FarmerHarvest {
        yields = List.copyOf(yields);
        if (yields.size() > MAX_DISTINCT_OUTPUTS) {
            throw new IllegalArgumentException("A farmer harvest cannot exceed twenty-seven distinct outputs");
        }
    }

    public static FarmerHarvest of(FarmerYield... yields) {
        return new FarmerHarvest(Arrays.asList(yields));
    }
}
