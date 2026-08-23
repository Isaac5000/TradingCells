package com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model;

/** Shared drop math used by both server generation and client previews. */
public final class SkeletonFarmDropRules {
    public static final int PROBABILITY_PARTS_PER_MILLION = 1_000_000;
    private static final int BASE_COMMON_DROP_BOUND = 3;
    private static final int BASE_COAL_DROP_BOUND = 2;
    private static final float BASE_WEAPON_CHANCE = 0.085F;
    private static final float LOOTING_WEAPON_CHANCE = 0.01F;
    private static final float BASE_SKULL_CHANCE = 0.025F;
    private static final float LOOTING_SKULL_CHANCE = 0.01F;

    private SkeletonFarmDropRules() {
    }

    public static int amountRollBound(SkeletonFarmLoot loot, int lootingLevel) {
        int looting = Math.max(0, lootingLevel);
        return switch (loot) {
            case BONES, ARROWS -> BASE_COMMON_DROP_BOUND + looting;
            case COAL -> BASE_COAL_DROP_BOUND + looting;
            case WEAPONS, SKULLS -> 1;
        };
    }

    public static float chance(SkeletonFarmLoot loot, int lootingLevel) {
        int looting = Math.max(0, lootingLevel);
        return switch (loot) {
            case WEAPONS -> Math.min(1.0F, BASE_WEAPON_CHANCE + looting * LOOTING_WEAPON_CHANCE);
            case SKULLS -> Math.min(1.0F, BASE_SKULL_CHANCE + looting * LOOTING_SKULL_CHANCE);
            case BONES, ARROWS, COAL -> 1.0F;
        };
    }

    public static BaseDrop baseDrop(SkeletonFarmLoot loot) {
        return cycleDrop(loot, 0, 1);
    }

    /** Estimates one completed cycle using the exact Looting and simulated-kill rules used by generation. */
    public static BaseDrop cycleDrop(SkeletonFarmLoot loot, int lootingLevel, int simulatedKills) {
        int kills = Math.max(1, simulatedKills);
        double positiveChance;
        long maximumAmount;
        if (loot == SkeletonFarmLoot.WEAPONS || loot == SkeletonFarmLoot.SKULLS) {
            double perKillChance = chance(loot, lootingLevel);
            positiveChance = 1.0D - Math.pow(1.0D - perKillChance, kills);
            maximumAmount = kills;
        } else {
            int rollBound = amountRollBound(loot, lootingLevel);
            positiveChance = 1.0D - Math.pow(1.0D / rollBound, kills);
            maximumAmount = (long) (rollBound - 1) * kills;
        }
        return new BaseDrop(
                probabilityParts(positiveChance),
                1,
                (int) Math.min(Integer.MAX_VALUE, maximumAmount)
        );
    }

    public static BaseDrop headCycleDrop(
            SkeletonFarmKind kind,
            int lootingLevel,
            int simulatedKills,
            int decapitationLevel
    ) {
        int kills = Math.max(1, simulatedKills);
        double perKillChance = DecapitationRules.farmHeadChance(
                lootingLevel,
                kind == SkeletonFarmKind.WITHER_SKELETON,
                decapitationLevel
        );
        double positiveChance = 1.0D - Math.pow(1.0D - perKillChance, kills);
        return new BaseDrop(probabilityParts(positiveChance), 1, kills);
    }

    private static int probabilityParts(double chance) {
        return (int) Math.round(Math.clamp(chance, 0.0D, 1.0D) * PROBABILITY_PARTS_PER_MILLION);
    }

    public record BaseDrop(int probabilityPartsPerMillion, int minimumAmount, int maximumAmount) {
        public BaseDrop {
            if (probabilityPartsPerMillion < 0
                    || probabilityPartsPerMillion > PROBABILITY_PARTS_PER_MILLION
                    || minimumAmount < 1
                    || maximumAmount < minimumAmount) {
                throw new IllegalArgumentException("Invalid Skeleton Farm base drop");
            }
        }
    }
}
