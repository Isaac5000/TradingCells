package com.cosmocraft.trading_cells.feature.combat.domain.model;

/** Exact head-drop probabilities shared by world loot, mob farms and their UI. */
public final class DecapitationRules {
    public static final int MAX_DECAPITATION_LEVEL = 6;
    private static final double BASE_HEAD_CHANCE = 0.025D;
    private static final double HEAD_CHANCE_PER_LEVEL = 0.01D;

    private DecapitationRules() {
    }

    public static double vanillaHeadChance(int lootingLevel) {
        return Math.clamp(
                BASE_HEAD_CHANCE + Math.max(0, lootingLevel) * HEAD_CHANCE_PER_LEVEL,
                0.0D,
                1.0D
        );
    }

    public static double decapitationHeadChance(int decapitationLevel) {
        int level = Math.max(0, decapitationLevel);
        return level == 0
                ? 0.0D
                : Math.min(1.0D, BASE_HEAD_CHANCE + level * HEAD_CHANCE_PER_LEVEL);
    }

    public static double farmHeadChance(
            int lootingLevel,
            boolean hasNativeHeadDrop,
            int decapitationLevel
    ) {
        if (hasNativeHeadDrop) {
            return Math.clamp(
                    BASE_HEAD_CHANCE
                            + Math.max(0, lootingLevel) * HEAD_CHANCE_PER_LEVEL
                            + Math.max(0, decapitationLevel) * HEAD_CHANCE_PER_LEVEL,
                    0.0D,
                    1.0D
            );
        }
        return decapitationHeadChance(decapitationLevel);
    }

    /** Conditional roll used after vanilla did not already add the native head. */
    public static double supplementalNativeHeadChance(int lootingLevel, int decapitationLevel) {
        double vanillaChance = vanillaHeadChance(lootingLevel);
        double targetChance = farmHeadChance(lootingLevel, true, decapitationLevel);
        if (vanillaChance >= 1.0D) {
            return 0.0D;
        }
        return Math.clamp((targetChance - vanillaChance) / (1.0D - vanillaChance), 0.0D, 1.0D);
    }
}
