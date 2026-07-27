package com.cosmocraft.trading_cells.feature.incubators.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
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

public final class IncubatorMenu extends AbstractContainerMenu {
    private static final int INCUBATOR_SLOT_COUNT = IncubatorBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = INCUBATOR_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final CapturedMobKind kind;
    private final Container container;
    private final ContainerData data;

    public static IncubatorMenu villager(int containerId, Inventory inventory) {
        return new IncubatorMenu(
                CapturedMobKind.VILLAGER,
                containerId,
                inventory,
                new SimpleContainer(INCUBATOR_SLOT_COUNT),
                new SimpleContainerData(2)
        );
    }

    public static IncubatorMenu piglin(int containerId, Inventory inventory) {
        return new IncubatorMenu(
                CapturedMobKind.PIGLIN,
                containerId,
                inventory,
                new SimpleContainer(INCUBATOR_SLOT_COUNT),
                new SimpleContainerData(2)
        );
    }

    public IncubatorMenu(
            CapturedMobKind kind,
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(kind == CapturedMobKind.VILLAGER
                ? IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_MENU.get()
                : IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_MENU.get(), containerId);
        checkContainerSize(container, INCUBATOR_SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.kind = kind;
        this.container = container;
        this.data = data;

        addSlot(new BabySlot(container, IncubatorBlockEntity.INPUT_SLOT, MachineMenuLayout.machineX(45), 48));
        addSlot(new OutputSlot(container, IncubatorBlockEntity.OUTPUT_SLOT, MachineMenuLayout.machineX(115), 48));
        addStandardInventorySlots(inventory, MachineMenuLayout.PLAYER_INVENTORY_X, MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y);
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public CapturedMobKind kind() {
        return kind;
    }

    public int incubationTicks() {
        return data.get(0);
    }

    public int maxIncubationTicks() {
        return Math.max(1, data.get(1));
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return container.stillValid(player);
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < INCUBATOR_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CapturedMobStackAdapter.isBaby(kind, stack)) {
            if (!moveItemStackTo(stack, IncubatorBlockEntity.INPUT_SLOT, IncubatorBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private final class BabySlot extends Slot {
        private BabySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return CapturedMobStackAdapter.isBaby(kind, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }
    }
}
