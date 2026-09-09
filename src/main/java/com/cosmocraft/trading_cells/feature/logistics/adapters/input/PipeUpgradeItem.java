package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PipeUpgradeItem extends Item {
    private static final String ROOT_TAG = "TradingCellsPipeUpgrade";
    private static final String SCHEMA_TAG = "SchemaVersion";
    private static final String PROFILES_TAG = "Profiles";
    private final PipeUpgradeTier tier;

    public PipeUpgradeItem(Properties properties, PipeUpgradeTier tier) {
        super(properties);
        if (tier == null || tier == PipeUpgradeTier.BARE) {
            throw new IllegalArgumentException("A pipe upgrade item requires a real tier");
        }
        this.tier = tier;
    }

    public PipeUpgradeTier tier() {
        return tier;
    }

    public static ItemStack withProfiles(ItemStack stack, CompoundTag profiles) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PipeUpgradeItem)) {
            return stack;
        }
        CompoundTag root = new CompoundTag();
        root.putInt(SCHEMA_TAG, LogisticsPipeBlockEntity.SCHEMA_VERSION);
        root.put(PROFILES_TAG, profiles.copy());
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        custom.put(ROOT_TAG, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return stack;
    }

    public static CompoundTag storedProfiles(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.copyTag().getCompound(ROOT_TAG)
                .filter(root -> root.getIntOr(SCHEMA_TAG, 0) == LogisticsPipeBlockEntity.SCHEMA_VERSION)
                .flatMap(root -> root.getCompound(PROFILES_TAG))
                .map(CompoundTag::copy)
                .orElse(null);
    }
}
