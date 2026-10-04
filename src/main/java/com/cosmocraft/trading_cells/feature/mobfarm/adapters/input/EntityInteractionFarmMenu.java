package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionHarvestResult.Status;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class EntityInteractionFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = 256, HEIGHT = 204, PLAYER_X = 47, PLAYER_Y = 124;
    public static final int OUTPUT_X = PLAYER_X, OUTPUT_Y = 56, OUTPUT_COLUMNS = 9;
    private final Container container;
    private final ContainerData data;
    public EntityInteractionFarmMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(EntityInteractionFarmBlockEntity.SIZE), new SimpleContainerData(7));
    }
    public EntityInteractionFarmMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(MobFarmRegistrationAdapter.INTERACTION_MENU.get(), id);
        this.container = container; this.data = data;
        checkContainerSize(container, EntityInteractionFarmBlockEntity.SIZE); checkContainerDataCount(data, 7);
        for (int i = 0; i < 5; i++) {
            final int input = i;
            addSlot(new Slot(container, i, 47 + i * 28, 32) {
                @Override public int getMaxStackSize() { return input == 2 ? 64 : 1; }
                @Override public boolean mayPlace(ItemStack stack) { return EntityInteractionFarmBlockEntity.accepts(input, stack); }
            });
        }
        for (int i = 0; i < EntityInteractionFarmBlockEntity.OUTPUT_COUNT; i++) {
            addSlot(new Slot(container, i + 5, OUTPUT_X + i % OUTPUT_COLUMNS * 18, OUTPUT_Y + i / OUTPUT_COLUMNS * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addSlot(new Slot(container, EntityInteractionFarmBlockEntity.CAPACITY, 187, 32) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) {
                return EntityInteractionFarmBlockEntity.accepts(EntityInteractionFarmBlockEntity.CAPACITY, stack);
            }
        });
        addStandardInventorySlots(inventory, PLAYER_X, PLAYER_Y); addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    public boolean enabled() { return data.get(2) != 0; }
    public boolean paused() { return data.get(6) != 0; }
    public Status status(int channel) { return Status.values()[Math.clamp(data.get(3 + channel), 0, Status.values().length - 1)]; }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || !(container instanceof EntityInteractionFarmBlockEntity farm)) { return false; }
        if (button == 0) { farm.toggleEnabled(); return true; }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(), before = stack.copy();
        boolean moved = false;
        int start = EntityInteractionFarmBlockEntity.SIZE;
        if (index < start) { moved = moveItemStackTo(stack, start, start + 36, true); }
        else {
            // Specialized inputs must win over the two deliberately unrestricted channels.
            for (int target : new int[]{0, 1, 4, EntityInteractionFarmBlockEntity.CAPACITY}) {
                if (EntityInteractionFarmBlockEntity.accepts(target, stack)) { moved = moveItemStackTo(stack, target, target + 1, false); break; }
            }
            if (!moved) { moved = moveItemStackTo(stack, stack.isDamageableItem() ? 3 : 2, stack.isDamageableItem() ? 4 : 3, false); }
        }
        if (!moved) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack); return before;
    }
}
