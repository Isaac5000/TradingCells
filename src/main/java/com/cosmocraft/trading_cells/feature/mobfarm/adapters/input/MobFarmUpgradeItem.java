package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MobFarmUpgradeItem extends Item {
    public enum Kind { SPEED, CAPACITY }
    private final Kind kind;
    private final int tier;

    public MobFarmUpgradeItem(Properties properties, Kind kind, int tier) {
        super(properties);
        this.kind = kind;
        this.tier = Math.clamp(tier, 1, 5);
    }

    public static boolean accepts(ItemStack stack, Kind kind) {
        return stack.getItem() instanceof MobFarmUpgradeItem upgrade && upgrade.kind == kind;
    }

    public static int tier(ItemStack stack, Kind kind) {
        return stack.getItem() instanceof MobFarmUpgradeItem upgrade && upgrade.kind == kind ? upgrade.tier : 0;
    }
}
