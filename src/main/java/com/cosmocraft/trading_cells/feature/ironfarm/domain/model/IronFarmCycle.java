package com.cosmocraft.trading_cells.feature.ironfarm.domain.model;

public record IronFarmCycle(
        int cycleTicks,
        int oneVillagerMultiplier,
        int twoVillagerMultiplier,
        int threeVillagerMultiplier,
        int golemAttackTicks,
        int golemHitInterval,
        int golemRedFlashTicks
) {
    public static final int BASE_ONE_VILLAGER_MULTIPLIER = 1;
    public static final int BASE_TWO_VILLAGER_MULTIPLIER = 2;
    public static final int BASE_THREE_VILLAGER_MULTIPLIER = 3;

    public IronFarmCycle {
        cycleTicks = Math.max(1, cycleTicks);
        oneVillagerMultiplier = Math.max(0, oneVillagerMultiplier);
        twoVillagerMultiplier = Math.max(0, twoVillagerMultiplier);
        threeVillagerMultiplier = Math.max(0, threeVillagerMultiplier);
        golemAttackTicks = Math.clamp(golemAttackTicks, 0, cycleTicks);
        golemHitInterval = Math.max(1, golemHitInterval);
        golemRedFlashTicks = Math.clamp(golemRedFlashTicks, 0, golemHitInterval - 1);
    }

    public int multiplier(int villagerCount) {
        return switch (Math.clamp(villagerCount, 0, 3)) {
            case 1 -> oneVillagerMultiplier;
            case 2 -> twoVillagerMultiplier;
            case 3 -> threeVillagerMultiplier;
            default -> 0;
        };
    }

    public boolean isGolemVisible(int currentTicks) {
        return currentTicks >= cycleTicks - golemAttackTicks;
    }

    public boolean isGolemHitTick(int currentTicks) {
        if (!isGolemVisible(currentTicks) || currentTicks >= cycleTicks) {
            return false;
        }
        return (currentTicks - (cycleTicks - golemAttackTicks)) % golemHitInterval == 0;
    }

    public boolean hasRedHitFlash(int currentTicks) {
        if (!isGolemVisible(currentTicks)) {
            return false;
        }
        int elapsed = currentTicks - (cycleTicks - golemAttackTicks);
        return elapsed % golemHitInterval < golemRedFlashTicks;
    }

    public boolean isRedHitFlashEnding(int currentTicks) {
        if (!isGolemVisible(currentTicks)) {
            return false;
        }
        int elapsed = currentTicks - (cycleTicks - golemAttackTicks);
        return elapsed % golemHitInterval == golemRedFlashTicks;
    }
}
