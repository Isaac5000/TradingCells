package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EssenceStabilizerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public EssenceStabilizerMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(6), new SimpleContainerData(2)); }
    public EssenceStabilizerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(MobFarmRegistrationAdapter.STABILIZER_MENU.get(), id);
        this.container = container;
        this.data = data;
        int[] xs = {28, 64, 100, 154, 178, 202};
        for (int i = 0; i < 6; i++) {
            final int index = i;
            addSlot(new Slot(container, i, xs[i], 48) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return switch (index) {
                        case 0 -> stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get());
                        case 1 -> stack.is(Items.AMETHYST_SHARD);
                        case 2 -> stack.is(Items.REDSTONE) || stack.is(Items.GLOWSTONE_DUST)
                                || stack.is(Items.ENDER_PEARL) || stack.is(Items.DRAGON_BREATH);
                        default -> false;
                    };
                }
            });
        }
        addStandardInventorySlots(inventory, 37, 130);
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 6 ? !moveItemStackTo(stack, 6, 42, true) : !moveItemStackTo(stack, 0, 3, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack);
        return original;
    }
}
