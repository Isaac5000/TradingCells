package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class VoltaicCapacitorItem extends Item {
    private final int capacity;
    public VoltaicCapacitorItem(Properties properties, int capacity) { super(properties); this.capacity = capacity; }
    public int capacity() { return capacity; }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * StormChargeData.charge(stack) / capacity); }
    @Override public int getBarColor(ItemStack stack) {
        return net.minecraft.util.Mth.hsvToRgb((float) StormChargeData.charge(stack) / capacity / 3F, 1, 1);
    }
}
