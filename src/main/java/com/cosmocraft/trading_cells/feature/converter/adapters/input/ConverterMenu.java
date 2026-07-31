package com.cosmocraft.trading_cells.feature.converter.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public final class ConverterMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ConverterBlockEntity.STORAGE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;

    public ConverterMenu(int containerId, Inventory inventory) {
        this(
                containerId,
                inventory,
                new SimpleContainer(ConverterBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(4)
        );
    }

    public ConverterMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ConverterRegistrationAdapter.CONVERTER_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        addSlot(new HiddenVillagerSlot(container, ConverterBlockEntity.VILLAGER_SLOT));
        for (int index = 0; index < ConverterBlockEntity.POTION_SLOT_COUNT; index++) {
            addSlot(new PotionSlot(
                    container,
                    ConverterBlockEntity.FIRST_POTION_SLOT + index,
                    MachineMenuLayout.machineX(43) + index * 24,
                    35
            ));
        }
        for (int index = 0; index < ConverterBlockEntity.APPLE_SLOT_COUNT; index++) {
            addSlot(new AppleSlot(
                    container,
                    ConverterBlockEntity.FIRST_APPLE_SLOT + index,
                    MachineMenuLayout.machineX(43) + index * 24,
                    63
            ));
        }
        addStandardInventorySlots(
                inventory,
                MachineMenuLayout.PLAYER_INVENTORY_X,
                MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y
        );
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public ConverterStage stage() {
        return ConverterStage.fromId(data.get(0));
    }

    public int stageTicks() {
        return data.get(1);
    }

    public int maxStageTicks() {
        return Math.max(1, data.get(3));
    }

    public boolean isProcessing() {
        return stage().isProcessing();
    }

    public boolean hasVillager() {
        return CapturedMobStackAdapter.isFilledCapturer(
                CapturedMobKind.VILLAGER,
                container.getItem(ConverterBlockEntity.VILLAGER_SLOT)
        );
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return container.stillValid(player);
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        if (index == ConverterBlockEntity.VILLAGER_SLOT) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (!moveQuickMovedStack(index, stack)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private boolean moveQuickMovedStack(int index, ItemStack stack) {
        if (index < MACHINE_SLOT_COUNT) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true);
        }
        if (ConverterIngredientAdapter.isWeaknessPotion(stack)) {
            return moveItemStackTo(
                    stack,
                    ConverterBlockEntity.FIRST_POTION_SLOT,
                    ConverterBlockEntity.FIRST_APPLE_SLOT,
                    false
            );
        }
        if (ConverterIngredientAdapter.isGoldenApple(stack)) {
            return moveItemStackTo(
                    stack,
                    ConverterBlockEntity.FIRST_APPLE_SLOT,
                    ConverterBlockEntity.STORAGE_SLOT_COUNT,
                    false
            );
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }

    private static final class HiddenVillagerSlot extends Slot {
        private HiddenVillagerSlot(Container container, int slot) {
            super(container, slot, -1_000, -1_000);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return false;
        }
    }

    private static final class PotionSlot extends Slot {
        private PotionSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return ConverterIngredientAdapter.isWeaknessPotion(stack);
        }

        @Override
        public int getMaxStackSize() {
            return ConverterBlockEntity.POTION_SLOT_LIMIT;
        }

        @Override
        public int getMaxStackSize(@NonNull ItemStack stack) {
            return ConverterIngredientAdapter.isWeaknessPotion(stack)
                    ? ConverterBlockEntity.POTION_SLOT_LIMIT
                    : 0;
        }
    }

    private static final class AppleSlot extends Slot {
        private AppleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return ConverterIngredientAdapter.isGoldenApple(stack);
        }
    }
}
