package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
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
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public final class FarmerMenu extends AbstractContainerMenu {
    public static final int VILLAGER_SLOT_X = MachineMenuLayout.machineX(50);
    public static final int HOE_SLOT_X = MachineMenuLayout.machineX(80);
    public static final int CROP_SLOT_X = MachineMenuLayout.machineX(110);
    public static final int INPUT_SLOT_Y = 30;
    private static final int MACHINE_SLOT_COUNT = FarmerBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;

    public FarmerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(3));
    }

    public FarmerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(FarmerRegistrationAdapter.FARMER_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;

        addSlot(new VillagerSlot(container, FarmerBlockEntity.VILLAGER_SLOT, VILLAGER_SLOT_X, INPUT_SLOT_Y));
        addSlot(new CropSlot(container, FarmerBlockEntity.CROP_SLOT, CROP_SLOT_X, INPUT_SLOT_Y));
        addSlot(new HoeSlot(container, FarmerBlockEntity.HOE_SLOT, HOE_SLOT_X, INPUT_SLOT_Y));
        addSlot(new OutputSlot(container, FarmerBlockEntity.FIRST_OUTPUT_SLOT, MachineMenuLayout.machineX(43), 94));
        addSlot(new OutputSlot(container, FarmerBlockEntity.FIRST_OUTPUT_SLOT + 1, MachineMenuLayout.machineX(67), 94));
        addSlot(new OutputSlot(container, FarmerBlockEntity.FIRST_OUTPUT_SLOT + 2, MachineMenuLayout.machineX(91), 94));
        addSlot(new OutputSlot(container, FarmerBlockEntity.FIRST_OUTPUT_SLOT + 3, MachineMenuLayout.machineX(115), 94));
        addStandardInventorySlots(inventory, MachineMenuLayout.PLAYER_INVENTORY_X, MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y);
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public int growthTicks() {
        return data.get(0);
    }

    public int maxGrowthTicks() {
        return Math.max(1, data.get(1));
    }

    public boolean isCultivating() {
        return data.get(2) != 0;
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
        if (isAdultVillager(stack)) {
            return moveItemStackTo(
                    stack,
                    FarmerBlockEntity.VILLAGER_SLOT,
                    FarmerBlockEntity.VILLAGER_SLOT + 1,
                    false
            );
        }
        if (FarmerCropStackAdapter.isSupported(stack)) {
            return moveItemStackTo(
                    stack,
                    FarmerBlockEntity.CROP_SLOT,
                    FarmerBlockEntity.CROP_SLOT + 1,
                    false
            );
        }
        if (stack.getItem() instanceof HoeItem) {
            return moveItemStackTo(
                    stack,
                    FarmerBlockEntity.HOE_SLOT,
                    FarmerBlockEntity.HOE_SLOT + 1,
                    false
            );
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
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

    private static final class CropSlot extends Slot {
        private CropSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return FarmerCropStackAdapter.isSupported(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class HoeSlot extends Slot {
        private HoeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return stack.getItem() instanceof HoeItem;
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
