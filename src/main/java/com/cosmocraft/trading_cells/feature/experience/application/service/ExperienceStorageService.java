package com.cosmocraft.trading_cells.feature.experience.application.service;

import com.cosmocraft.trading_cells.feature.experience.application.port.input.ExperienceStorageUseCase;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceMath;

public final class ExperienceStorageService implements ExperienceStorageUseCase {
    @Override
    public int depositLevels(
            int playerLevel,
            float playerProgress,
            int storedPoints,
            int capacity,
            int requestedLevels
    ) {
        if (requestedLevels <= 0) {
            return 0;
        }
        int currentPoints = ExperienceMath.totalPoints(playerLevel, playerProgress);
        int targetPoints = requestedLevels >= playerLevel
                ? 0
                : ExperienceMath.pointsAtLevelWithProgress(playerLevel - requestedLevels, playerProgress);
        return transferable(
                (long) currentPoints - targetPoints,
                (long) Math.max(0, capacity) - Math.max(0, storedPoints)
        );
    }

    @Override
    public int depositAll(int playerLevel, float playerProgress, int storedPoints, int capacity) {
        return transferable(
                ExperienceMath.totalPoints(playerLevel, playerProgress),
                (long) Math.max(0, capacity) - Math.max(0, storedPoints)
        );
    }

    @Override
    public int withdrawLevels(
            int playerLevel,
            float playerProgress,
            int storedPoints,
            int requestedLevels
    ) {
        if (requestedLevels <= 0 || storedPoints <= 0) {
            return 0;
        }
        int currentPoints = ExperienceMath.totalPoints(playerLevel, playerProgress);
        long requestedTargetLevel = (long) Math.max(0, playerLevel) + requestedLevels;
        int targetLevel = requestedTargetLevel >= Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) requestedTargetLevel;
        int targetPoints = ExperienceMath.pointsAtLevelWithProgress(targetLevel, playerProgress);
        return transferable((long) targetPoints - currentPoints, storedPoints);
    }

    @Override
    public int withdrawAll(int playerLevel, float playerProgress, int storedPoints) {
        int playerPoints = ExperienceMath.totalPoints(playerLevel, playerProgress);
        return transferable(storedPoints, (long) Integer.MAX_VALUE - playerPoints);
    }

    private static int transferable(long requestedPoints, long availablePoints) {
        if (requestedPoints <= 0 || availablePoints <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.min(requestedPoints, availablePoints));
    }
}
