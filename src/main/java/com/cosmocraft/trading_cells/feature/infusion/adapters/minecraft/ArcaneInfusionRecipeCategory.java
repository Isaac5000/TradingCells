package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum ArcaneInfusionRecipeCategory {
    GENERATORS,
    EQUIPMENT,
    PRODUCTION,
    MISC;

    public static final Codec<ArcaneInfusionRecipeCategory> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Arcane Infusion category: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public static ArcaneInfusionRecipeCategory fromOrdinal(int ordinal) {
        ArcaneInfusionRecipeCategory[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Unknown Arcane Infusion category ordinal: " + ordinal);
        }
        return values[ordinal];
    }
}
