package com.cosmocraft.trading_cells.feature.breeders.domain.model;

/** Configurable values required by the breeder domain. */
public record BreederRules(
        int villagerBreedTicks,
        int piglinBreedTicks,
        int villagerBreadCost,
        int villagerVegetableCost,
        int maximumPendingBabies
) {
    public BreederRules {
        villagerBreedTicks = Math.max(1, villagerBreedTicks);
        piglinBreedTicks = Math.max(1, piglinBreedTicks);
        villagerBreadCost = Math.max(1, villagerBreadCost);
        villagerVegetableCost = Math.max(1, villagerVegetableCost);
        maximumPendingBabies = Math.max(1, maximumPendingBabies);
    }
}
