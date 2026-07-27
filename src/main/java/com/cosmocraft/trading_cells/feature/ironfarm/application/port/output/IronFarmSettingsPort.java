package com.cosmocraft.trading_cells.feature.ironfarm.application.port.output;

public interface IronFarmSettingsPort {
    int ironFarmCycleTicks();

    int ironFarmOneVillagerMultiplier();

    int ironFarmTwoVillagerMultiplier();

    int ironFarmThreeVillagerMultiplier();

    int ironFarmBaseIron();

    int ironFarmMaximumPoppies();

    int ironGolemAttackTicks();

    int ironGolemHitInterval();

    int ironGolemRedFlashTicks();
}
