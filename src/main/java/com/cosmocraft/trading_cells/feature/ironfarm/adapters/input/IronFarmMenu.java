package com.cosmocraft.trading_cells.feature.ironfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
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

public final class IronFarmMenu extends AbstractContainerMenu {
    public static final int TOGGLE_FLOWERS_BUTTON = 0;
    public static final int DISABLE_FLOWERS_BUTTON = 1;
    public static final int ENABLE_FLOWERS_BUTTON = 2;
    public static final int VILLAGER_ROW_X = MachineMenuLayout.machineX(56);
    public static final int VILLAGER_ROW_Y = 24;
    public static final int OUTPUT_ROW_X = MachineMenuLayout.machineX(44);
    public static final int OUTPUT_ROW_Y = 72;
    private static final int MACHINE_SLOT_COUNT = IronFarmBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;

    public IronFarmMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(7));
    }

    public IronFarmMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(IronFarmRegistrationAdapter.IRON_FARM_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 7);
        this.container = container;
        this.data = data;

        for (int index = 0; index < IronFarmBlockEntity.VILLAGER_SLOT_COUNT; index++) {
            addSlot(new VillagerSlot(
                    container,
                    IronFarmBlockEntity.FIRST_VILLAGER_SLOT + index,
                    VILLAGER_ROW_X + index * 24,
                    VILLAGER_ROW_Y
            ));
        }
        for (int index = 0; index < IronFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            addSlot(new OutputSlot(
                    container,
                    IronFarmBlockEntity.FIRST_OUTPUT_SLOT + index,
                    OUTPUT_ROW_X + index * 24,
                    OUTPUT_ROW_Y
            ));
        }
        addStandardInventorySlots(inventory, MachineMenuLayout.PLAYER_INVENTORY_X, MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y);
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public int cycleTicks() {
        return data.get(0);
    }

    public int maxCycleTicks() {
        return Math.max(1, data.get(3));
    }

    public boolean flowersEnabled() {
        return data.get(1) != 0;
    }

    public int villagerCount() {
        return data.get(2);
    }

    public int currentMultiplier() {
        return data.get(4);
    }

    public int nextMultiplier() {
        return data.get(5);
    }

    public int maximumVillagers() {
        return data.get(6);
    }

    public void setClientFlowersEnabled(boolean enabled) {
        data.set(1, enabled ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        int enabled = switch (buttonId) {
            case TOGGLE_FLOWERS_BUTTON -> data.get(1) == 0 ? 1 : 0;
            case DISABLE_FLOWERS_BUTTON -> 0;
            case ENABLE_FLOWERS_BUTTON -> 1;
            default -> -1;
        };
        if (enabled < 0) {
            return false;
        }
        data.set(1, enabled);
        container.setChanged();
        return true;
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
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isAdultVillager(stack)) {
            if (!moveItemStackTo(stack, IronFarmBlockEntity.FIRST_VILLAGER_SLOT, IronFarmBlockEntity.FIRST_OUTPUT_SLOT, false)) {
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

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static final class VillagerSlot extends Slot {
        private VillagerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return isAdultVillager(stack);
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
