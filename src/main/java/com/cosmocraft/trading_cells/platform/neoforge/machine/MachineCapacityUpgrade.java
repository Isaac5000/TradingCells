package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.world.item.ItemStack;

/** Shared capacity contract; consumers do not depend on the owning feature. */
public interface MachineCapacityUpgrade {
    int capacityTier();

    static int tier(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MachineCapacityUpgrade upgrade
                ? Math.clamp(upgrade.capacityTier(), 0, 5) : 0;
    }

    static boolean accepts(ItemStack stack) { return tier(stack) > 0; }
    static int multiplier(ItemStack stack) { return 1 << tier(stack); }
}
