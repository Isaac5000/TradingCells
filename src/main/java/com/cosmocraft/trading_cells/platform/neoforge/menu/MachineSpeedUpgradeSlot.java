package com.cosmocraft.trading_cells.platform.neoforge.menu;

import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineSpeedUpgrade;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MachineSpeedUpgradeSlot extends Slot {
    public MachineSpeedUpgradeSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean mayPlace(ItemStack stack) { return MachineSpeedUpgrade.accepts(stack); }
}
