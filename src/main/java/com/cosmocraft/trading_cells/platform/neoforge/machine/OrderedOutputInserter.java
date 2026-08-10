package com.cosmocraft.trading_cells.platform.neoforge.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Inserts machine outputs in slot order without allocating a simulated inventory. */
public final class OrderedOutputInserter {
    private OrderedOutputInserter() {
    }

    public static boolean canInsert(
            NonNullList<ItemStack> inventory,
            int firstSlot,
            int slotCount,
            ItemStack source
    ) {
        if (source.isEmpty()) {
            return true;
        }
        int remaining = source.getCount();
        for (int slot = firstSlot; slot < firstSlot + slotCount; slot++) {
            ItemStack output = inventory.get(slot);
            if (output.isEmpty()) {
                remaining -= source.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(output, source)) {
                remaining -= Math.max(0, output.getMaxStackSize() - output.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean canInsertAll(
            NonNullList<ItemStack> inventory,
            int firstSlot,
            int slotCount,
            List<ItemStack> sources
    ) {
        int emptySlots = 0;
        for (int slot = firstSlot; slot < firstSlot + slotCount; slot++) {
            if (inventory.get(slot).isEmpty()) {
                emptySlots++;
            }
        }

        int requiredEmptySlots = 0;
        for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
            ItemStack source = sources.get(sourceIndex);
            if (source.isEmpty() || appearedEarlier(sources, sourceIndex, source)) {
                continue;
            }

            long remaining = combinedCount(sources, sourceIndex, source);
            for (int slot = firstSlot; slot < firstSlot + slotCount && remaining > 0; slot++) {
                ItemStack output = inventory.get(slot);
                if (!output.isEmpty() && ItemStack.isSameItemSameComponents(output, source)) {
                    remaining -= Math.max(0, output.getMaxStackSize() - output.getCount());
                }
            }
            if (remaining <= 0) {
                continue;
            }

            requiredEmptySlots += divideRoundUp(remaining, source.getMaxStackSize());
            if (requiredEmptySlots > emptySlots) {
                return false;
            }
        }
        return true;
    }

    public static void insertAllValidated(
            NonNullList<ItemStack> inventory,
            int firstSlot,
            int slotCount,
            List<ItemStack> sources
    ) {
        for (ItemStack source : sources) {
            if (!insert(inventory, firstSlot, slotCount, source)) {
                throw new IllegalStateException("Validated machine output no longer fits");
            }
        }
    }

    /**
     * Inserts a strict prefix of the supplied outputs and returns the untouched remainder.
     * This is used only when a generated batch can never fit in the machine at once.
     */
    public static PartialInsert insertAvailable(
            NonNullList<ItemStack> inventory,
            int firstSlot,
            int slotCount,
            List<ItemStack> sources
    ) {
        List<ItemStack> remainingSources = new ArrayList<>();
        boolean insertedAny = false;
        boolean capacityExhausted = false;
        for (ItemStack source : sources) {
            if (source.isEmpty()) {
                continue;
            }
            if (capacityExhausted) {
                remainingSources.add(source);
                continue;
            }

            int remaining = insertAvailableCount(inventory, firstSlot, slotCount, source);
            insertedAny |= remaining < source.getCount();
            if (remaining > 0) {
                remainingSources.add(source.copyWithCount(remaining));
                capacityExhausted = true;
            }
        }
        return new PartialInsert(List.copyOf(remainingSources), insertedAny);
    }

    public static boolean canFitInEmptySlots(int slotCount, List<ItemStack> sources) {
        int requiredSlots = 0;
        for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
            ItemStack source = sources.get(sourceIndex);
            if (source.isEmpty() || appearedEarlier(sources, sourceIndex, source)) {
                continue;
            }
            requiredSlots += divideRoundUp(
                    combinedCount(sources, sourceIndex, source),
                    source.getMaxStackSize()
            );
            if (requiredSlots > slotCount) {
                return false;
            }
        }
        return true;
    }

    public static boolean insert(
            NonNullList<ItemStack> inventory,
            int firstSlot,
            int slotCount,
            ItemStack source
    ) {
        if (source.isEmpty()) {
            return true;
        }
        int remaining = source.getCount();
        for (int slot = firstSlot; slot < firstSlot + slotCount && remaining > 0; slot++) {
            ItemStack output = inventory.get(slot);
            if (output.isEmpty() || !ItemStack.isSameItemSameComponents(output, source)) {
                continue;
            }
            int moved = Math.min(remaining, Math.max(0, output.getMaxStackSize() - output.getCount()));
            output.grow(moved);
            remaining -= moved;
        }
        for (int slot = firstSlot; slot < firstSlot + slotCount && remaining > 0; slot++) {
            if (!inventory.get(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining, source.getMaxStackSize());
            inventory.set(slot, source.copyWithCount(moved));
            remaining -= moved;
        }
        return remaining == 0;
    }

    private static int insertAvailableCount(
            NonNullList<ItemStack> inventory,
            int firstSlot,
            int slotCount,
            ItemStack source
    ) {
        int remaining = source.getCount();
        for (int slot = firstSlot; slot < firstSlot + slotCount && remaining > 0; slot++) {
            ItemStack output = inventory.get(slot);
            if (output.isEmpty() || !ItemStack.isSameItemSameComponents(output, source)) {
                continue;
            }
            int moved = Math.min(remaining, Math.max(0, output.getMaxStackSize() - output.getCount()));
            output.grow(moved);
            remaining -= moved;
        }
        for (int slot = firstSlot; slot < firstSlot + slotCount && remaining > 0; slot++) {
            if (!inventory.get(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining, source.getMaxStackSize());
            inventory.set(slot, source.copyWithCount(moved));
            remaining -= moved;
        }
        return remaining;
    }

    private static boolean appearedEarlier(List<ItemStack> sources, int sourceIndex, ItemStack source) {
        for (int index = 0; index < sourceIndex; index++) {
            ItemStack previous = sources.get(index);
            if (!previous.isEmpty() && ItemStack.isSameItemSameComponents(previous, source)) {
                return true;
            }
        }
        return false;
    }

    private static long combinedCount(List<ItemStack> sources, int sourceIndex, ItemStack source) {
        long count = 0;
        for (int index = sourceIndex; index < sources.size(); index++) {
            ItemStack candidate = sources.get(index);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, source)) {
                count += candidate.getCount();
            }
        }
        return count;
    }

    private static int divideRoundUp(long value, int divisor) {
        return Math.toIntExact((value + divisor - 1L) / divisor);
    }

    public record PartialInsert(List<ItemStack> remaining, boolean insertedAny) {
    }
}
