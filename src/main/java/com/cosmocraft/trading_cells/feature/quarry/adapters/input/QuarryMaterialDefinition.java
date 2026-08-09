package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Immutable material rule shared by runtime generation, the catalog GUI and REI. */
public record QuarryMaterialDefinition(
        Identifier id,
        QuarryKind kind,
        QuarryUpgradeTier minimumUpgrade,
        double minimumPickaxeSpeed,
        List<Integer> weights,
        int minimumAmount,
        int maximumAmount,
        Identifier normalResult,
        Identifier silkResult,
        @Nullable Identifier deepSilkResult,
        boolean fortuneCompatible,
        boolean rock,
        boolean dynamic,
        boolean useBlockLoot,
        String sourceMod,
        Pool pool
) {
    public static final int TIER_COUNT = QuarryUpgradeTier.values().length;

    public QuarryMaterialDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(minimumUpgrade);
        Objects.requireNonNull(weights);
        Objects.requireNonNull(normalResult);
        Objects.requireNonNull(silkResult);
        Objects.requireNonNull(sourceMod);
        Objects.requireNonNull(pool);
        if (weights.size() != TIER_COUNT || weights.stream().anyMatch(weight -> weight == null || weight < 0)) {
            throw new IllegalArgumentException("A quarry material requires six non-negative tier weights");
        }
        if (!Double.isFinite(minimumPickaxeSpeed) || minimumPickaxeSpeed < 0.0D) {
            throw new IllegalArgumentException("Minimum pickaxe speed must be finite and non-negative");
        }
        if (minimumAmount <= 0 || maximumAmount < minimumAmount) {
            throw new IllegalArgumentException("Invalid quarry output range");
        }
        weights = List.copyOf(weights);
    }

    public int weight(QuarryUpgradeTier tier) {
        return weights.get(tier.ordinal());
    }

    public Identifier silkResult(boolean deepMining) {
        return deepMining && hasDeepVariant() ? deepSilkResult : silkResult;
    }

    public boolean hasDeepVariant() {
        return deepSilkResult != null
                && BuiltInRegistries.BLOCK.containsKey(deepSilkResult)
                && BuiltInRegistries.ITEM.containsKey(deepSilkResult);
    }

    public enum Pool {
        NORMAL,
        PROTECTED_RARE
    }
}
