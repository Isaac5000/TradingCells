package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public final class MobFarmMachineConfiguration {
    private static final String TARGET = "Target";
    private static final String LOOT_MASK = "LootMask";
    private static final String ENABLED = "Enabled";
    private static final String DISABLED_COUNT = "DisabledLootCount";
    private static final String DISABLED_PREFIX = "DisabledLoot";
    private static final int MAX_DISABLED = 2_048;

    private MobFarmMachineConfiguration() {
    }

    public static CompoundTag export(
            CompoundTag base,
            Identifier target,
            int lootMask,
            boolean enabled,
            Set<Identifier> disabledLoot
    ) {
        base.putString(TARGET, target.toString());
        base.putInt(LOOT_MASK, lootMask);
        base.putBoolean(ENABLED, enabled);
        List<Identifier> sorted = disabledLoot.stream().sorted().toList();
        base.putInt(DISABLED_COUNT, sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            base.putString(DISABLED_PREFIX + index, sorted.get(index).toString());
        }
        return base;
    }

    public static Optional<Settings> parse(
            CompoundTag configuration,
            MobFarmCatalog.Family family,
            int allowedLootMask
    ) {
        Identifier target = Identifier.tryParse(configuration.getStringOr(TARGET, ""));
        int lootMask = configuration.getIntOr(LOOT_MASK, -1);
        int disabledCount = configuration.getIntOr(DISABLED_COUNT, -1);
        if (target == null
                || !MobFarmCatalog.contains(family, target)
                || lootMask < 0
                || (lootMask & ~allowedLootMask) != 0
                || disabledCount < 0
                || disabledCount > MAX_DISABLED) {
            return Optional.empty();
        }
        Set<Identifier> available = MobFarmCatalog.target(family, target)
                .map(definition -> Set.copyOf(definition.lootItemIds()))
                .orElse(Set.of());
        LinkedHashSet<Identifier> disabled = new LinkedHashSet<>();
        for (int index = 0; index < disabledCount; index++) {
            Identifier itemId = Identifier.tryParse(configuration.getStringOr(DISABLED_PREFIX + index, ""));
            if (itemId == null || !available.contains(itemId)) {
                return Optional.empty();
            }
            disabled.add(itemId);
        }
        return Optional.of(new Settings(
                target,
                lootMask,
                configuration.getBooleanOr(ENABLED, true),
                Set.copyOf(disabled)
        ));
    }

    public record Settings(Identifier target, int lootMask, boolean enabled, Set<Identifier> disabledLoot) {
    }
}
