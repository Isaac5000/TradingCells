package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class StormStationMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public StormStationMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(4), new SimpleContainerData(2)); }
    public StormStationMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(CombatRegistrationAdapter.STATION_MENU.get(), id);
        this.container = container;
        this.data = data;
        int[] xs = {64, 136, 172, 190};
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            addSlot(new Slot(container, i, xs[i], i >= 2 ? 70 : 48) {
                @Override public boolean mayPlace(ItemStack stack) { return StormStationBlockEntity.accepts(slot, stack); }
                @Override public boolean isActive() { return slot < 2 || hasItem(); }
            });
        }
        addStandardInventorySlots(inventory, 28, 96);
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int charge() { return StormChargeData.charge(container.getItem(1)); }
    public int capacity() { return StormChargeData.capacity(container.getItem(1)); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 4 ? !moveItemStackTo(stack, 4, 40, true) : !moveItemStackTo(stack, 0, 2, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack);
        return original;
    }
}
