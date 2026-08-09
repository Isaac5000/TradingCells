package com.cosmocraft.trading_cells.shared.machines.domain.model;

/** Saturating vanilla experience formulas for machines that exchange player XP. */
public final class MinecraftExperience {
    private static final float MAX_PROGRESS = Math.nextDown(1.0F);
    private static final int MAX_FULL_LEVEL_WITH_INT_POINTS = 21_863;

    private MinecraftExperience() {
    }

    public static int pointsAtStartOfLevel(int level) {
        int safeLevel = Math.max(0, level);
        if (safeLevel > MAX_FULL_LEVEL_WITH_INT_POINTS) {
            return Integer.MAX_VALUE;
        }
        long points;
        if (safeLevel <= 16) {
            points = (long) safeLevel * safeLevel + 6L * safeLevel;
        } else if (safeLevel <= 31) {
            points = (5L * safeLevel * safeLevel - 81L * safeLevel + 720L) / 2L;
        } else {
            points = (9L * safeLevel * safeLevel - 325L * safeLevel + 4_440L) / 2L;
        }
        return saturated(points);
    }

    public static int pointsNeededForNextLevel(int level) {
        int safeLevel = Math.max(0, level);
        long points = safeLevel >= 30
                ? 112L + (long) (safeLevel - 30) * 9L
                : safeLevel >= 15
                        ? 37L + (long) (safeLevel - 15) * 5L
                        : 7L + (long) safeLevel * 2L;
        return saturated(points);
    }

    public static int totalPoints(int level, float progress) {
        float safeProgress = Math.clamp(progress, 0.0F, MAX_PROGRESS);
        long partial = (long) Math.floor(safeProgress * pointsNeededForNextLevel(level));
        return saturated((long) pointsAtStartOfLevel(level) + partial);
    }

    public static int pointsAtLevelWithProgress(int level, float progress) {
        return totalPoints(level, progress);
    }

    public static int maximumAdditionalLevels(int level, float progress, int availablePoints) {
        if (availablePoints <= 0) {
            return 0;
        }
        int currentPoints = totalPoints(level, progress);
        long targetPoints = Math.min(Integer.MAX_VALUE, (long) currentPoints + availablePoints);
        int low = Math.max(0, level);
        if (low >= MAX_FULL_LEVEL_WITH_INT_POINTS) {
            return 0;
        }
        int high = low;
        int step = 1;
        while (high <= MAX_FULL_LEVEL_WITH_INT_POINTS - step
                && pointsAtLevelWithProgress(high + step, progress) <= targetPoints
                && pointsAtLevelWithProgress(high + step, progress) < Integer.MAX_VALUE) {
            high += step;
            step = Math.min(step * 2, 1 << 20);
        }
        high = Math.min(MAX_FULL_LEVEL_WITH_INT_POINTS, high + step);
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1L) / 2L);
            if (pointsAtLevelWithProgress(middle, progress) <= targetPoints) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return Math.max(0, low - Math.max(0, level));
    }

    public static int levelForTotalPoints(int points) {
        return maximumAdditionalLevels(0, 0.0F, Math.max(0, points));
    }

    private static int saturated(long value) {
        return (int) Math.clamp(value, 0L, Integer.MAX_VALUE);
    }
}
