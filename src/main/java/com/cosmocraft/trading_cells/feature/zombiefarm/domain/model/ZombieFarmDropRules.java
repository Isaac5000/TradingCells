package com.cosmocraft.trading_cells.feature.zombiefarm.domain.model;

import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;

/** Vanilla 26.2 zombie-family drop math shared by generation and previews. */
public final class ZombieFarmDropRules {
    public static final int PROBABILITY_PARTS_PER_MILLION = 1_000_000;
    private static final double BASE_RARE_DROP_CHANCE = 0.025D;
    private static final double RARE_DROP_CHANCE_PER_LOOTING_LEVEL = 0.01D;
    private static final double BASE_COPPER_CHANCE = 0.11D;
    private static final double COPPER_CHANCE_PER_LOOTING_LEVEL = 0.02D;
    private static final double BASE_EQUIPMENT_DROP_CHANCE = 0.085D;
    private static final double EQUIPMENT_DROP_CHANCE_PER_LOOTING_LEVEL = 0.01D;
    private static final double ZOMBIE_WEAPON_SPAWN_CHANCE = 0.01D;
    private static final double DROWNED_WEAPON_SPAWN_CHANCE = 0.10D;
    private static final double DROWNED_TRIDENT_WEIGHT = 10.0D / 16.0D;
    private static final double DROWNED_FISHING_ROD_WEIGHT = 6.0D / 16.0D;
    private static final double DROWNED_NAUTILUS_CHANCE = 0.03D;
    private static final double ZOMBIFIED_PIGLIN_SPEAR_WEIGHT = 1.0D / 20.0D;

    private ZombieFarmDropRules() {
    }

    public static int fleshMaximumPerKill(int lootingLevel) {
        return 2 + Math.max(0, lootingLevel);
    }

    public static int zombifiedPiglinStackMaximumPerKill(int lootingLevel) {
        return 1 + Math.max(0, lootingLevel);
    }

    public static int zoglinFleshMaximumPerKill(int lootingLevel) {
        return 3 + Math.max(0, lootingLevel);
    }

    public static double fleshPositiveChance(int lootingLevel) {
        int looting = Math.max(0, lootingLevel);
        return 1.0D - 1.0D / (3.0D * (looting + 1.0D));
    }

    public static double zombifiedPiglinStackPositiveChance(int lootingLevel) {
        int looting = Math.max(0, lootingLevel);
        return 1.0D - 1.0D / (2.0D * (looting + 1.0D));
    }

    public static double rarePoolChance(int lootingLevel) {
        return Math.min(1.0D, BASE_RARE_DROP_CHANCE
                + Math.max(0, lootingLevel) * RARE_DROP_CHANCE_PER_LOOTING_LEVEL);
    }

    public static double rareItemChance(int lootingLevel) {
        return rarePoolChance(lootingLevel) / 3.0D;
    }

    public static double copperChance(int lootingLevel) {
        return Math.min(1.0D, BASE_COPPER_CHANCE
                + Math.max(0, lootingLevel) * COPPER_CHANCE_PER_LOOTING_LEVEL);
    }

    public static double equipmentDropChance(int lootingLevel) {
        return Math.min(1.0D, BASE_EQUIPMENT_DROP_CHANCE
                + Math.max(0, lootingLevel) * EQUIPMENT_DROP_CHANCE_PER_LOOTING_LEVEL);
    }

    public static double zombieWeaponChance(int lootingLevel) {
        return zombieWeaponChance(lootingLevel, false);
    }

    public static double zombieWeaponChance(int lootingLevel, boolean hardDifficulty) {
        double spawnChance = hardDifficulty ? 0.05D : ZOMBIE_WEAPON_SPAWN_CHANCE;
        return spawnChance * equipmentDropChance(lootingLevel);
    }

    public static double zombifiedPiglinSwordChance(int lootingLevel) {
        return equipmentDropChance(lootingLevel) * (1.0D - ZOMBIFIED_PIGLIN_SPEAR_WEIGHT);
    }

    public static double zombifiedPiglinSpearChance(int lootingLevel) {
        return equipmentDropChance(lootingLevel) * ZOMBIFIED_PIGLIN_SPEAR_WEIGHT;
    }

    public static double zombieSwordChance(int lootingLevel) {
        return zombieWeaponChance(lootingLevel) / 6.0D;
    }

    public static double zombieSwordChance(int lootingLevel, boolean hardDifficulty) {
        return zombieWeaponChance(lootingLevel, hardDifficulty) / 6.0D;
    }

    public static double zombieSpearChance(int lootingLevel) {
        return zombieWeaponChance(lootingLevel) / 6.0D;
    }

    public static double zombieSpearChance(int lootingLevel, boolean hardDifficulty) {
        return zombieWeaponChance(lootingLevel, hardDifficulty) / 6.0D;
    }

    public static double zombieShovelChance(int lootingLevel) {
        return zombieWeaponChance(lootingLevel) * 4.0D / 6.0D;
    }

    public static double zombieShovelChance(int lootingLevel, boolean hardDifficulty) {
        return zombieWeaponChance(lootingLevel, hardDifficulty) * 4.0D / 6.0D;
    }

    public static double drownedTridentChance(int lootingLevel) {
        return DROWNED_WEAPON_SPAWN_CHANCE
                * DROWNED_TRIDENT_WEIGHT
                * equipmentDropChance(lootingLevel);
    }

    public static double drownedFishingRodChance(int lootingLevel) {
        return DROWNED_WEAPON_SPAWN_CHANCE
                * DROWNED_FISHING_ROD_WEIGHT
                * equipmentDropChance(lootingLevel);
    }

    public static double drownedNautilusChance() {
        return DROWNED_NAUTILUS_CHANCE;
    }

    public static BaseDrop fleshCycleDrop(int lootingLevel, int simulatedKills) {
        return cycleDrop(
                fleshPositiveChance(lootingLevel),
                simulatedKills,
                fleshMaximumPerKill(lootingLevel)
        );
    }

    public static BaseDrop zombifiedPiglinStackCycleDrop(int lootingLevel, int simulatedKills) {
        return cycleDrop(
                zombifiedPiglinStackPositiveChance(lootingLevel),
                simulatedKills,
                zombifiedPiglinStackMaximumPerKill(lootingLevel)
        );
    }

    public static BaseDrop zoglinFleshCycleDrop(int lootingLevel, int simulatedKills) {
        return cycleDrop(1.0D, simulatedKills, zoglinFleshMaximumPerKill(lootingLevel));
    }

    public static BaseDrop binaryCycleDrop(double perKillChance, int simulatedKills) {
        return cycleDrop(perKillChance, simulatedKills, 1);
    }

    public static BaseDrop headCycleDrop(int simulatedKills, int decapitationLevel) {
        return binaryCycleDrop(
                DecapitationRules.decapitationHeadChance(decapitationLevel),
                simulatedKills
        );
    }

    private static BaseDrop cycleDrop(double perKillChance, int simulatedKills, int maximumPerKill) {
        int kills = Math.max(1, simulatedKills);
        double chance = Math.clamp(perKillChance, 0.0D, 1.0D);
        double positiveChance = 1.0D - Math.pow(1.0D - chance, kills);
        long maximum = (long) Math.max(1, maximumPerKill) * kills;
        return new BaseDrop(
                probabilityParts(positiveChance),
                1,
                (int) Math.min(Integer.MAX_VALUE, maximum)
        );
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
                throw new IllegalArgumentException("Invalid Zombie Farm drop preview");
            }
        }
    }
}
