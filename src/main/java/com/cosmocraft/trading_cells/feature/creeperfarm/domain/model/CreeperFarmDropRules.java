package com.cosmocraft.trading_cells.feature.creeperfarm.domain.model;

import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;

/** Creeper drop math shared by generation previews and help text. */
public final class CreeperFarmDropRules {
    public static final int PROBABILITY_PARTS_PER_MILLION = 1_000_000;

    private CreeperFarmDropRules() {
    }

    public static BaseDrop gunpowderCycleDrop(int lootingLevel, int simulatedKills) {
        int looting = Math.max(0, lootingLevel);
        double positiveChance = 1.0D - 1.0D / (3.0D * (looting + 1.0D));
        return cycleDrop(positiveChance, simulatedKills, 2 + looting);
    }

    public static BaseDrop stormShardCycleDrop(int lootingLevel, int simulatedKills) {
        int kills = Math.max(1, simulatedKills);
        int maximumPerKill = 1 + Math.max(0, lootingLevel);
        return new BaseDrop(PROBABILITY_PARTS_PER_MILLION, kills, saturatingMultiply(kills, maximumPerKill));
    }

    public static BaseDrop headCycleDrop(int simulatedKills, int decapitationLevel) {
        return cycleDrop(
                DecapitationRules.decapitationHeadChance(decapitationLevel),
                simulatedKills,
                1
        );
    }

    public static BaseDrop binaryCycleDrop(double perKillChance, int simulatedKills) {
        return cycleDrop(perKillChance, simulatedKills, 1);
    }

    private static BaseDrop cycleDrop(double perKillChance, int simulatedKills, int maximumPerKill) {
        int kills = Math.max(1, simulatedKills);
        double chance = Math.clamp(perKillChance, 0.0D, 1.0D);
        double positiveChance = 1.0D - Math.pow(1.0D - chance, kills);
        return new BaseDrop(
                probabilityParts(positiveChance),
                1,
                saturatingMultiply(kills, Math.max(1, maximumPerKill))
        );
    }

    private static int saturatingMultiply(int left, int right) {
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
                throw new IllegalArgumentException("Invalid Creeper Farm drop preview");
            }
        }
    }
}
