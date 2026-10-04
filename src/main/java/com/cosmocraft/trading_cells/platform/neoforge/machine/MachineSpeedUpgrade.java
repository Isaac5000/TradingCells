package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.world.item.ItemStack;

/** Item-side contract shared by machines without importing another feature. */
public interface MachineSpeedUpgrade {
    int speedTier();

    static int tier(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MachineSpeedUpgrade upgrade
                ? Math.clamp(upgrade.speedTier(), 0, 5) : 0;
    }

    static boolean accepts(ItemStack stack) { return tier(stack) > 0; }
}
