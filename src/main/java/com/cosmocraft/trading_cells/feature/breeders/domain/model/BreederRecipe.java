package com.cosmocraft.trading_cells.feature.breeders.domain.model;

public final class BreederRecipe {
    private BreederRecipe() {
    }

    public static boolean isFood(BreederKind kind, BreederFood food) {
        return switch (kind) {
            case VILLAGER -> food == BreederFood.BREAD || food == BreederFood.VEGETABLE;
            case PIGLIN -> food == BreederFood.PORK || food == BreederFood.CRIMSON_FUNGUS;
        };
    }

    public static int cost(BreederKind kind, BreederFood food, BreederRules rules) {
        if (!isFood(kind, food)) {
            return Integer.MAX_VALUE;
        }
        return switch (kind) {
            case VILLAGER -> food == BreederFood.BREAD
                    ? rules.villagerBreadCost()
                    : rules.villagerVegetableCost();
            case PIGLIN -> food == BreederFood.PORK
                    ? rules.piglinPorkCost()
                    : rules.piglinCrimsonFungusCost();
        };
    }

    public static int breedTicks(BreederKind kind, BreederRules rules) {
        return kind == BreederKind.VILLAGER
                ? rules.villagerBreedTicks()
                : rules.piglinBreedTicks();
    }
}
