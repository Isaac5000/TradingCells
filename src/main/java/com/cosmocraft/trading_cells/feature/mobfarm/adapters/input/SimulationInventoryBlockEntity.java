package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

abstract class SimulationInventoryBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer {
    protected final NonNullList<ItemStack> items;
    private final int firstOutput;
    private final int[] allSlots;
    private final int[] outputSlots;

    SimulationInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, int firstOutput) {
        super(type, pos, state);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.firstOutput = firstOutput;
        allSlots = IntStream.range(0, size).toArray();
        outputSlots = IntStream.range(firstOutput, size).toArray();
    }

    protected void slotChanged(int slot) { }
    protected boolean acceptsInput(int slot, ItemStack stack) { return false; }
    protected boolean canChangeInput(int slot) { return true; }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.getMaxStackSize(); }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < firstOutput && !canChangeInput(slot)) { return ItemStack.EMPTY; }
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) { slotChanged(slot); markChangedAndSync(); }
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < firstOutput && !canChangeInput(slot)) { return ItemStack.EMPTY; }
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) { slotChanged(slot); }
        return removed;
    }

    @Override public void setItem(int slot, ItemStack stack) { setItem(slot, stack, false); }

    @Override public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        // Rollback must be able to restore output slots which reject ordinary insertion.
        if (!insideTransaction && (!canChangeInput(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack)))) {
            return;
        }
        items.set(slot, stack);
        slotChanged(slot);
        markChangedAndSync();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < firstOutput && canChangeInput(slot) && acceptsInput(slot, stack);
    }

    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? outputSlots : allSlots; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= firstOutput; }
    @Override public void clearContent() { items.clear(); markChangedAndSync(); }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, input.read("Slot" + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) { output.store("Slot" + slot, ItemStack.CODEC, items.get(slot)); }
        }
    }

    @Override protected void clearContentsForBlockDrop() { items.clear(); setChanged(); }
}
