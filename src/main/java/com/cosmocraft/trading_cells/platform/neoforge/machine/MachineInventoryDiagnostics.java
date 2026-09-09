package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class MachineInventoryDiagnostics {
    private MachineInventoryDiagnostics() {
    }

    public static OutputUsage outputUsage(Container container, int firstSlot, int slotCount) {
        int used = 0;
        int capacity = 0;
        int end = Math.min(container.getContainerSize(), firstSlot + Math.max(0, slotCount));
        for (int slot = Math.max(0, firstSlot); slot < end; slot++) {
            ItemStack stack = container.getItem(slot);
            int slotCapacity = stack.isEmpty() ? container.getMaxStackSize() : stack.getMaxStackSize();
            capacity += Math.max(0, slotCapacity);
            used += Math.max(0, stack.getCount());
        }
        return new OutputUsage(used, capacity);
    }

    public record OutputUsage(int used, int capacity) {
        public boolean full() {
            return capacity > 0 && used >= capacity;
        }
    }
}
