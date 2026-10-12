package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.domain.model.StormChargeRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Physical, component-preserving batteries; no timers or external energy capability. */
public final class StormChargeData {
    private static final String CHARGE = "TradingCellsStormCharge";

    private StormChargeData() { }

    public static int charge(ItemStack battery) {
        return StormChargeRules.clamp(battery.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(CHARGE, 0), capacity(battery));
    }

    public static int capacity(ItemStack battery) {
        return battery.getItem() instanceof VoltaicCapacitorItem item ? item.capacity() : 0;
    }

    public static void setCharge(ItemStack battery, int charge) {
        if (capacity(battery) == 0) { return; }
        var data = battery.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt(CHARGE, StormChargeRules.clamp(charge, capacity(battery)));
        battery.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

}
