package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.MobFarmRules;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EssenceWorkbenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 236;
    public static final int HEIGHT = 222;
    private final Container container;
    private final Player owner;

    public EssenceWorkbenchMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(4)); }

    public EssenceWorkbenchMenu(int id, Inventory inventory, Container container) {
        super(MobFarmRegistrationAdapter.WORKBENCH_MENU.get(), id);
        this.container = container;
        owner = inventory.player;
        checkContainerSize(container, 4);
        for (int index = 0; index < 4; index++) {
            final int input = index;
            addSlot(new Slot(container, index, 35 + index * 48, 47) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return switch (input) {
                        case 0 -> stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get());
                        case 1 -> stack.is(Items.AMETHYST_SHARD);
                        case 2 -> stack.is(Items.IRON_INGOT) || stack.is(Items.NETHERITE_INGOT);
                        default -> false;
                    };
                }
            });
        }
        addStandardInventorySlots(inventory, 37, 130);
    }

    public boolean highLevel() { return EntityEssenceData.isHighLevel(creature()); }
    public int experienceCost() { return MobFarmRules.essenceExperienceCost(highLevel()); }
    public int requiredShards() { return MobFarmRules.essenceShardCost(highLevel()); }
    public ItemStack requiredMaterial() {
        return new ItemStack(highLevel() ? Items.NETHERITE_INGOT : Items.IRON_INGOT,
                MobFarmRules.essenceMaterialCost(highLevel()));
    }
    public ItemStack creature() { return container.getItem(0); }
    public boolean canSynthesize() {
        ItemStack material = requiredMaterial();
        return creature().is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())
                && EntityEssenceData.entityTypeId(creature()) != null && container.getItem(3).isEmpty()
                && container.getItem(1).is(Items.AMETHYST_SHARD) && container.getItem(1).getCount() >= requiredShards()
                && container.getItem(2).is(material.getItem()) && container.getItem(2).getCount() >= material.getCount()
                && MinecraftExperience.totalPoints(owner.experienceLevel, owner.experienceProgress) >= experienceCost();
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        return button == 0 && player == owner && player instanceof ServerPlayer server
                && container instanceof EssenceWorkbenchBlockEntity workbench && workbench.synthesize(server);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < 4 ? !moveItemStackTo(stack, 4, 40, true) : !moveItemStackTo(stack, 0, 3, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack);
        return before;
    }
}
