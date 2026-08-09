package com.cosmocraft.trading_cells.feature.experience.domain.model;

import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;

/** Compatibility facade for the experience-storage feature. */
public final class ExperienceMath {

    private ExperienceMath() {
    }

    public static int pointsAtStartOfLevel(int level) {
        return MinecraftExperience.pointsAtStartOfLevel(level);
    }

    public static int pointsNeededForNextLevel(int level) {
        return MinecraftExperience.pointsNeededForNextLevel(level);
    }

    public static int totalPoints(int level, float progress) {
        return MinecraftExperience.totalPoints(level, progress);
    }

    public static int pointsAtLevelWithProgress(int level, float progress) {
        return MinecraftExperience.pointsAtLevelWithProgress(level, progress);
    }

    public static int maximumAdditionalLevels(int level, float progress, int availablePoints) {
        return MinecraftExperience.maximumAdditionalLevels(level, progress, availablePoints);
    }

    public static int levelForTotalPoints(int points) {
        return MinecraftExperience.levelForTotalPoints(points);
    }
}
