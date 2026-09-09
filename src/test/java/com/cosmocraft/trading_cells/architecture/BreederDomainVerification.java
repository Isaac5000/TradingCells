package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRules;

final class BreederDomainVerification {
    private BreederDomainVerification() {
    }

    static void verify() {
        verifyBreederRules();
    }

    private static void verifyBreederRules() {
        BreederRules rules = new BreederRules(100, 200, 3, 12, 64);
        require(BreederRecipe.breedTicks(BreederKind.PIGLIN, rules) == 200,
                "Piglin duration must come from configuration");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.COOKED_PORKCHOP, rules) == 2,
                "Cooked porkchops must cost two");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.NETHER_WART_BLOCK, rules) == 2,
                "Nether wart blocks must cost two");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.RAW_PORKCHOP, rules) == 4,
                "Raw porkchops must cost four");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.CRIMSON_FUNGUS, rules) == 6,
                "Crimson fungi must cost six");
        require(BreederRecipe.cost(BreederKind.PIGLIN, BreederFood.NETHER_WART, rules) == 12,
                "Nether wart must cost twelve");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
