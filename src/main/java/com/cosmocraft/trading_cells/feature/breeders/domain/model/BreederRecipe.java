package com.cosmocraft.trading_cells.feature.breeders.domain.model;

public final class BreederRecipe {
    private BreederRecipe() {
    }

    public static boolean isFood(BreederKind kind, BreederFood food) {
        return switch (kind) {
            case VILLAGER -> food == BreederFood.BREAD || food == BreederFood.VEGETABLE;
            case PIGLIN -> switch (food) {
                case COOKED_PORKCHOP, NETHER_WART_BLOCK, RAW_PORKCHOP, CRIMSON_FUNGUS, NETHER_WART -> true;
                default -> false;
            };
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
            case PIGLIN -> piglinCost(food);
        };
    }

    public static int piglinCost(BreederFood food) {
        return switch (food) {
            case COOKED_PORKCHOP, NETHER_WART_BLOCK -> 2;
            case RAW_PORKCHOP -> 4;
            case CRIMSON_FUNGUS -> 6;
            case NETHER_WART -> 12;
            default -> Integer.MAX_VALUE;
        };
    }

    public static int breedTicks(BreederKind kind, BreederRules rules) {
        return kind == BreederKind.VILLAGER
                ? rules.villagerBreedTicks()
                : rules.piglinBreedTicks();
    }
}
