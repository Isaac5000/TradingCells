package com.cosmocraft.trading_cells.platform.neoforge.bootstrap;

import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturerDurability;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BREEDER_TICKS;
    public static final ModConfigSpec.IntValue INCUBATOR_TICKS;
    public static final ModConfigSpec.IntValue FARMER_GROWTH_TICKS;
    public static final ModConfigSpec.BooleanValue FARMER_DAMAGE_HOES;
    public static final ModConfigSpec.IntValue CAPTURER_DURABILITY;
    public static final ModConfigSpec.IntValue IRON_FARM_CYCLE_TICKS;
    public static final ModConfigSpec.IntValue IRON_FARM_MULTIPLIER_BONUS;
    public static final ModConfigSpec.BooleanValue VILLAGER_INFINITE_TRADES;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Machine durations in game ticks.").push("timers");
        BUILDER.comment("Shared breeding duration for villager and piglin breeders.");
        BREEDER_TICKS = ticks("breeder", 6_000);
        BUILDER.comment("Shared growth duration for villager and piglin incubators.");
        INCUBATOR_TICKS = ticks("incubator", 3_000);
        BUILDER.comment("Base villager crop duration without a hoe. Default: 150 seconds.");
        FARMER_GROWTH_TICKS = ticks("farmerGrowth", 3_000);
        BUILDER.comment("Iron farm cycle duration. Default: 60 seconds.");
        IRON_FARM_CYCLE_TICKS = ticks("ironFarmCycle", 1_200);
        BUILDER.comment("When enabled, villager offers in the Trading Cell and Autotrader never run out of stock.");
        VILLAGER_INFINITE_TRADES = BUILDER.define("villagerInfiniteTrades", true);
        BUILDER.pop();

        BUILDER.comment("Production tuning.").push("production");
        BUILDER.comment("When enabled, each completed crop cycle can consume one point of hoe durability.");
        FARMER_DAMAGE_HOES = BUILDER.define("farmerDamageHoes", true);
        BUILDER.comment(
                "Adds this value to every iron-farm multiplier. Base multipliers are x1, x2 and x3; ",
                "for example, 15 changes them to x16, x17 and x18. Use 0 for the defaults."
        );
        IRON_FARM_MULTIPLIER_BONUS = BUILDER.defineInRange("ironFarmMultiplierBonus", 0, 0, 1_024);
        BUILDER.pop();

        BUILDER.comment("Capturer item settings.").push("capturers");
        BUILDER.comment("Maximum durability shared by villager and piglin capturers. Default: 10 releases.");
        CAPTURER_DURABILITY = BUILDER.defineInRange(
                "durability",
                CapturerDurability.DEFAULT_MAX_DAMAGE,
                1,
                32_767
        );
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {
    }

    private static ModConfigSpec.IntValue ticks(String name, int defaultValue) {
        return BUILDER.defineInRange(name + "Ticks", defaultValue, 1, 72_000);
    }
}
