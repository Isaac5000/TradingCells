package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class EssenceWorkbenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 332, HEIGHT = 246;
    private final Container container;
    private final Player owner;
    private final ContainerData data;

    public EssenceWorkbenchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(4), new SimpleContainerData(5));
    }
    public EssenceWorkbenchMenu(int id, Inventory inventory, Container container) {
        this(id, inventory, container, new SimpleContainerData(5));
    }
    public EssenceWorkbenchMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(MobFarmRegistrationAdapter.WORKBENCH_MENU.get(), id);
        this.container = container;
        this.data = data;
        owner = inventory.player;
        checkContainerSize(container, 4);
        checkContainerDataCount(data, 5);
        int[] xs = {18, 58, 18};
        for (int index = 0; index < 3; index++) {
            final int input = index;
            addSlot(new Slot(container, index, xs[index], index == 2 ? 84 : 50) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return input == 0 ? stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())
                            : input == 1 && stack.getItem() instanceof CreatureModelBaseItem;
                }
                @Override public boolean isActive() { return input != 2 || hasItem(); }
            });
        }
        addSlot(new Slot(container, 3, 118, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return data.get(4) != 0; }
            @Override public ItemStack getItem() {
                return container instanceof EssenceWorkbenchBlockEntity bench ? bench.visibleResult() : super.getItem();
            }
            @Override public ItemStack remove(int count) {
                if (!(container instanceof EssenceWorkbenchBlockEntity bench)) { return super.remove(count); }
                ItemStack result = getItem();
                return count > 0 && !result.isEmpty() && bench.takeVisibleResult(result) ? result : ItemStack.EMPTY;
            }
            @Override public void set(ItemStack stack) {
                if (!(container instanceof EssenceWorkbenchBlockEntity)) { super.set(stack); }
            }
            @Override public boolean isFake() { return true; }
        });
        addStandardInventorySlots(inventory, 84, 158);
        addDataSlots(data);
    }

    public int storedExperience() { return (data.get(0) & 0xFFFF) | ((data.get(1) & 0x7FFF) << 16); }
    public boolean fillStorage() { return data.get(2) != 0; }
    public boolean automatic() { return data.get(3) != 0; }
    public boolean highLevel() { return EntityEssenceData.isHighLevel(creature()); }
    public int experienceCost() { return creature().isEmpty() ? 0 : EntityEssenceData.tier(creature()).modelExperience(); }
    public ItemStack requiredMaterial() {
        return MobFarmRegistrationAdapter.MODEL_BASES.get(EntityEssenceData.tier(creature()).id() - 1).get().getDefaultInstance();
    }
    public ItemStack creature() { return container.getItem(0); }
    public boolean canSynthesize() {
        return creature().is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())
                && EntityEssenceData.entityTypeId(creature()) != null && container.getItem(3).isEmpty()
                && container.getItem(1).getItem() instanceof CreatureModelBaseItem base
                && base.tier() == EntityEssenceData.tier(creature()) && storedExperience() >= experienceCost();
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player != owner || !stillValid(player) || !(player instanceof ServerPlayer server)
                || !(container instanceof EssenceWorkbenchBlockEntity workbench)) { return false; }
        if (button == 5) { workbench.experience().toggleMode(); return true; }
        if (button == 6) { workbench.toggleAutomatic(); return true; }
        if (button >= 1 && button <= 4) {
            workbench.experience().transfer(server, button - 1, 1);
            return true;
        }
        return false;
    }
    public void handleExperienceTransfer(ServerPlayer player, int action, int levels) {
        if (player == owner && stillValid(player) && container instanceof EssenceWorkbenchBlockEntity bench) {
            bench.experience().transfer(player, action, levels);
        }
    }
    @Override public boolean canTakeItemForPickAll(ItemStack carried, Slot target) { return target.index != 3; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index == 3 && container instanceof EssenceWorkbenchBlockEntity bench) {
            if (!hasInventorySpace(stack) || !bench.takeVisibleResult(before)) { return ItemStack.EMPTY; }
            moveItemStackTo(stack, 4, 40, true);
            slot.onTake(player, before);
            return before;
        }
        if (index < 4 ? !moveItemStackTo(stack, 4, 40, true) : !moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack);
        return before;
    }
    private boolean hasInventorySpace(ItemStack stack) {
        int needed = stack.getCount();
        for (int index = 4; index < 40; index++) {
            Slot slot = slots.get(index);
            ItemStack current = slot.getItem();
            if (slot.mayPlace(stack) && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack))) {
                needed -= Math.max(0, slot.getMaxStackSize(stack) - current.getCount());
                if (needed <= 0) { return true; }
            }
        }
        return false;
    }
}
