package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.world.item.ItemStack;

/** Per-operation quota; does not reduce inventory capacity or existing manual buffers. */
public interface MachineInsertionLimit {
    int insertionLimit(int slot, ItemStack stack);
}
