package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormChargeRules;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class StormStationBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    private static final int[] INPUTS = {0, 1, 2, 3}, OUTPUTS = {1, 2, 3};
    // Indices 2/3 are withdrawal-only recovery for previously saved output and sand.
    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int progress;
    private MachineDiagnosticStatus status = MachineDiagnosticStatus.INACTIVE;
    private final ContainerData data = new ContainerData() {
        public int get(int index) { return index == 0 ? progress : 0; }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };

    public StormStationBlockEntity(BlockPos pos, BlockState state) { super(CombatRegistrationAdapter.STATION_ENTITY.get(), pos, state); }
    @Override public boolean supportsRedstoneControl() { return false; }
    public static boolean accepts(int slot, ItemStack stack) {
        return slot == 0 ? stack.is(CombatRegistrationAdapter.STORM_SHARD_ITEM.get()) : slot == 1 && StormChargeData.capacity(stack) > 0;
    }
    @Override public void processTick() {
        status = MachineDiagnosticStatus.INACTIVE;
        if (level == null || level.isClientSide()) { return; }
        ItemStack battery = items.get(1);
        int capacity = StormChargeData.capacity(battery), charge = StormChargeData.charge(battery);
        if (capacity == 0 || charge >= capacity || !accepts(0, items.get(0))) { resetProgress(); return; }
        status = MachineDiagnosticStatus.RUNNING;
        if (++progress < StormChargeRules.TRANSFER_TICKS) { setChanged(); return; }
        // The battery never moves; the whole shard is spent even if its last units cannot fit.
        items.get(0).shrink(1);
        StormChargeData.setCharge(battery, Math.min(capacity, charge + StormChargeRules.SHARD_CHARGE));
        progress = 0;
        markChangedAndSync();
    }
    private void resetProgress() { if (progress != 0) { progress = 0; setChanged(); } }
    @Override public MachineDiagnosticSnapshot machineDiagnosticSnapshot() {
        return new MachineDiagnosticSnapshot(status, status == MachineDiagnosticStatus.INACTIVE ? "input_required" : MachineDiagnosticSnapshot.NONE,
                progress, StormChargeRules.TRANSFER_TICKS, 0, StormChargeData.charge(items.get(1)), StormChargeData.capacity(items.get(1)));
    }
    @Override public int getContainerSize() { return 4; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.getMaxStackSize(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return accepts(slot, stack); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) { resetProgress(); markChangedAndSync(); }
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { resetProgress(); return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { setItem(slot, stack, false); }
    @Override public void setItem(int slot, ItemStack stack, boolean transaction) {
        if (!transaction && !stack.isEmpty() && !accepts(slot, stack)) { return; }
        items.set(slot, stack);
        if (!transaction) { resetProgress(); }
        markChangedAndSync();
    }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUTS : INPUTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && accepts(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 2 || slot == 1 && StormChargeData.capacity(stack) > 0 && StormChargeData.charge(stack) == StormChargeData.capacity(stack);
    }
    @Override public void clearContent() { items.clear(); resetProgress(); markChangedAndSync(); }
    @Override protected void clearContentsForBlockDrop() { items.clear(); progress = 0; setChanged(); }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < 4; slot++) { if (!items.get(slot).isEmpty()) { output.store("Slot" + slot, ItemStack.CODEC, items.get(slot)); } }
        output.putInt("ChargeProgress", progress);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < 4; slot++) { items.set(slot, input.read("Slot" + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY)); }
        if (items.get(1).isEmpty() && accepts(1, items.get(2))) { items.set(1, items.get(2)); items.set(2, ItemStack.EMPTY); }
        progress = Math.clamp(input.getIntOr("ChargeProgress", 0), 0, StormChargeRules.TRANSFER_TICKS - 1);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.storm_station"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new StormStationMenu(id, inventory, this, data); }
}
