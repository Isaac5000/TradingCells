package com.cosmocraft.trading_cells.feature.raiderfarm.domain.model;

/** Stable preview math for grouped Raider Farm drops. Real drops still use loaded loot tables. */
public final class RaiderFarmDropRules {
    public static final int PROBABILITY_PARTS_PER_MILLION = 1_000_000;
    private static final double BASE_EQUIPMENT_DROP_CHANCE = 0.085D;
    private static final double EQUIPMENT_DROP_CHANCE_PER_LOOTING_LEVEL = 0.01D;

    private RaiderFarmDropRules() {
    }

    public static double weaponChance(int lootingLevel) {
        return Math.min(1.0D, BASE_EQUIPMENT_DROP_CHANCE
                + Math.max(0, lootingLevel) * EQUIPMENT_DROP_CHANCE_PER_LOOTING_LEVEL);
    }

    public static BaseDrop binaryCycleDrop(double perKillChance, int simulatedKills) {
        int kills = Math.max(1, simulatedKills);
        double chance = Math.clamp(perKillChance, 0.0D, 1.0D);
        double positiveChance = 1.0D - Math.pow(1.0D - chance, kills);
        return new BaseDrop(probabilityParts(positiveChance), 1, kills);
    }

    public static BaseDrop guaranteedCycleDrop(int minimumPerKill, int maximumPerKill, int simulatedKills) {
        int kills = Math.max(1, simulatedKills);
        int minimum = boundedProduct(Math.max(1, minimumPerKill), kills);
        int maximum = boundedProduct(Math.max(minimumPerKill, maximumPerKill), kills);
        return new BaseDrop(PROBABILITY_PARTS_PER_MILLION, minimum, maximum);
    }

    public static BaseDrop witchCommonCycleDrop(int entryWeight, int lootingLevel, int simulatedKills) {
        int weight = Math.clamp(entryWeight, 1, 7);
        int looting = Math.max(0, lootingLevel);
        int kills = Math.max(1, simulatedKills);
        double positiveCountChance = 1.0D - 1.0D / (3.0D * (looting + 1.0D));
        double positiveRollChance = weight / 7.0D * positiveCountChance;
        double noDropPerKill = 0.0D;
        for (int rolls = 1; rolls <= 3; rolls++) {
            noDropPerKill += Math.pow(1.0D - positiveRollChance, rolls) / 3.0D;
        }
        double positiveCycleChance = 1.0D - Math.pow(noDropPerKill, kills);
        int maximum = boundedProduct(3 * (2 + looting), kills);
        return new BaseDrop(probabilityParts(positiveCycleChance), 1, maximum);
    }

    private static int boundedProduct(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left * right);
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
                throw new IllegalArgumentException("Invalid Raider Farm drop preview");
            }
        }
    }
}
