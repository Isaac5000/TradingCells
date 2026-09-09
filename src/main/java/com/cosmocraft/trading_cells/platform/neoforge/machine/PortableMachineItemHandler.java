package com.cosmocraft.trading_cells.platform.neoforge.machine;

import java.util.Arrays;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/** Exposes downward output slots to external transports without changing vanilla hopper rules. */
public final class PortableMachineItemHandler implements ResourceHandler<ItemResource> {
    private static final int[] EMPTY = new int[0];
    private final WorldlyContainer container;
    private final ResourceHandler<ItemResource> contents;
    private final @Nullable Direction side;
    private int[] sidedSlots = EMPTY;
    private int[] outputSlots = EMPTY;
    private int[] slots = EMPTY;

    public PortableMachineItemHandler(WorldlyContainer container, @Nullable Direction side) {
        this.container = container;
        this.contents = VanillaContainerWrapper.of(container);
        this.side = side;
        refreshSlots();
    }

    private void refreshSlots() {
        int[] inputs = side == null ? EMPTY : container.getSlotsForFace(side);
        int[] outputs = container.getSlotsForFace(Direction.DOWN);
        if (inputs == sidedSlots && outputs == outputSlots) { return; }
        sidedSlots = inputs;
        outputSlots = outputs;
        int[] merged = Arrays.copyOf(inputs, inputs.length + outputs.length);
        int size = inputs.length;
        for (int output : outputs) {
            if (!contains(inputs, output)) { merged[size++] = output; }
        }
        slots = Arrays.copyOf(merged, size);
    }

    private int slot(int index) {
        refreshSlots();
        return slots[Objects.checkIndex(index, slots.length)];
    }

    @Override
    public int size() { refreshSlots(); return slots.length; }

    @Override
    public ItemResource getResource(int index) { return contents.getResource(slot(index)); }

    @Override
    public long getAmountAsLong(int index) { return contents.getAmountAsLong(slot(index)); }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return contents.getCapacityAsLong(slot(index), resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        int slot = slot(index);
        return accepts(slot, resource) && contents.isValid(slot, resource);
    }

    private boolean accepts(int slot, ItemResource resource) {
        return side != null && contains(sidedSlots, slot)
                && container.canPlaceItemThroughFace(slot, resource.toStack(), side);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int slot = slot(index);
        return accepts(slot, resource) ? contents.insert(slot, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int slot = slot(index);
        boolean output = contains(outputSlots, slot)
                && container.canTakeItemThroughFace(slot, resource.toStack(), Direction.DOWN);
        boolean sided = side != null && contains(sidedSlots, slot)
                && container.canTakeItemThroughFace(slot, resource.toStack(), side);
        return output || sided ? contents.extract(slot, resource, amount, transaction) : 0;
    }

    private static boolean contains(int[] slots, int slot) {
        for (int candidate : slots) { if (candidate == slot) { return true; } }
        return false;
    }
}
