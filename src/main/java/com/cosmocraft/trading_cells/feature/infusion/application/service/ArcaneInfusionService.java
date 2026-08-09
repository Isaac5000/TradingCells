package com.cosmocraft.trading_cells.feature.infusion.application.service;

import com.cosmocraft.trading_cells.feature.infusion.application.port.input.ArcaneInfusionUseCase;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionAttempt;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionDecision;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;

public final class ArcaneInfusionService implements ArcaneInfusionUseCase {
    @Override
    public ArcaneInfusionDecision evaluate(ArcaneInfusionAttempt attempt) {
        return attempt.decision();
    }

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
        int currentPoints = MinecraftExperience.totalPoints(playerLevel, playerProgress);
        int targetPoints = requestedLevels >= playerLevel
                ? 0
                : MinecraftExperience.pointsAtLevelWithProgress(playerLevel - requestedLevels, playerProgress);
        return transferable(
                (long) currentPoints - targetPoints,
                (long) Math.max(0, capacity) - Math.max(0, storedPoints)
        );
    }

    @Override
    public int depositAll(int playerLevel, float playerProgress, int storedPoints, int capacity) {
        return transferable(
                MinecraftExperience.totalPoints(playerLevel, playerProgress),
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
        int currentPoints = MinecraftExperience.totalPoints(playerLevel, playerProgress);
        long requestedTargetLevel = (long) Math.max(0, playerLevel) + requestedLevels;
        int targetLevel = requestedTargetLevel >= Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) requestedTargetLevel;
        int targetPoints = MinecraftExperience.pointsAtLevelWithProgress(targetLevel, playerProgress);
        return transferable((long) targetPoints - currentPoints, storedPoints);
    }

    @Override
    public int withdrawAll(int playerLevel, float playerProgress, int storedPoints) {
        int playerPoints = MinecraftExperience.totalPoints(playerLevel, playerProgress);
        return transferable(storedPoints, (long) Integer.MAX_VALUE - playerPoints);
    }

    private static int transferable(long requestedPoints, long availablePoints) {
        if (requestedPoints <= 0 || availablePoints <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.min(requestedPoints, availablePoints));
    }
}
