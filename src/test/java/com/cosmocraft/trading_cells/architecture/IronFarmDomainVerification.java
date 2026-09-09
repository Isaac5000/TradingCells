package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;

final class IronFarmDomainVerification {
    private IronFarmDomainVerification() {
    }

    static void verify() {
        verifyIronFarmRules();
    }

    private static void verifyIronFarmRules() {
        IronFarmCycle cycle = new IronFarmCycle(
                1_200,
                IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER,
                IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER,
                IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER,
                80,
                16,
                5
        );
        require(cycle.multiplier(1) == 1, "One villager must use the x1 base multiplier");
        require(cycle.multiplier(3) == 3, "Three villagers must use the x3 base multiplier");
        require(cycle.multiplier(1, 1) == 2, "One Nitwit must produce at x2");
        require(cycle.multiplier(3, 3) == 6, "Three Nitwits must produce at x6");
        require(cycle.isGolemVisible(1_120), "The golem must appear during its attack window");

        int bonus = 15;
        IronFarmCycle configuredCycle = new IronFarmCycle(
                1_200,
                IronFarmCycle.BASE_ONE_VILLAGER_MULTIPLIER + bonus,
                IronFarmCycle.BASE_TWO_VILLAGER_MULTIPLIER + bonus,
                IronFarmCycle.BASE_THREE_VILLAGER_MULTIPLIER + bonus,
                80,
                16,
                5
        );
        require(configuredCycle.multiplier(1) == 16 && configuredCycle.multiplier(3) == 18,
                "The configured iron-farm value must be added to every base multiplier");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
