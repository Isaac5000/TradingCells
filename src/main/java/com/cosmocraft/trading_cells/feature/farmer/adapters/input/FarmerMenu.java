package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
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
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public final class FarmerMenu extends AbstractContainerMenu {
    public static final int WORKER_SLOT_X = MachineMenuLayout.machineX(50);
    public static final int HOE_SLOT_X = MachineMenuLayout.machineX(80);
    public static final int CROP_SLOT_X = MachineMenuLayout.machineX(110);
    public static final int INPUT_SLOT_Y = 30;
    public static final int OUTPUT_COLUMN_COUNT = 9;
    public static final int OUTPUT_SLOT_FIRST_X = MachineMenuLayout.machineX(8);
    public static final int OUTPUT_SLOT_SPACING = 18;
    public static final int OUTPUT_SLOT_FIRST_Y = 82;
    public static final int OUTPUT_ROW_SPACING = 18;
    private static final int MACHINE_SLOT_COUNT = FarmerBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final FarmerKind kind;
    private final Container container;
    private final ContainerData data;

    public static FarmerMenu villager(int containerId, Inventory inventory) {
        return clientMenu(FarmerKind.VILLAGER, containerId, inventory);
    }

    public static FarmerMenu piglin(int containerId, Inventory inventory) {
        return clientMenu(FarmerKind.PIGLIN, containerId, inventory);
    }

    private static FarmerMenu clientMenu(FarmerKind kind, int containerId, Inventory inventory) {
        return new FarmerMenu(
                kind,
                containerId,
                inventory,
                new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(3)
        );
    }

    public FarmerMenu(
            FarmerKind kind,
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(kind == FarmerKind.VILLAGER
                ? FarmerRegistrationAdapter.FARMER_MENU.get()
                : FarmerRegistrationAdapter.PIGLIN_FARMER_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 3);
        this.kind = kind;
        this.container = container;
        this.data = data;

        addSlot(new WorkerSlot(container, FarmerBlockEntity.WORKER_SLOT, WORKER_SLOT_X, INPUT_SLOT_Y));
        addSlot(new CropSlot(container, FarmerBlockEntity.CROP_SLOT, CROP_SLOT_X, INPUT_SLOT_Y));
        addSlot(new HoeSlot(container, FarmerBlockEntity.HOE_SLOT, HOE_SLOT_X, INPUT_SLOT_Y));
        for (int index = 0; index < FarmerBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            addSlot(new OutputSlot(
                    container,
                    FarmerBlockEntity.FIRST_OUTPUT_SLOT + index,
                    outputSlotX(index),
                    outputSlotY(index)
            ));
        }
        addStandardInventorySlots(inventory, MachineMenuLayout.PLAYER_INVENTORY_X, MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y);
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public int growthTicks() {
        return data.get(0);
    }

    public FarmerKind kind() {
        return kind;
    }

    public static int outputSlotX(int index) {
        return OUTPUT_SLOT_FIRST_X + index % OUTPUT_COLUMN_COUNT * OUTPUT_SLOT_SPACING;
    }

    public static int outputSlotY(int index) {
        return OUTPUT_SLOT_FIRST_Y + index / OUTPUT_COLUMN_COUNT * OUTPUT_ROW_SPACING;
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
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != container || target instanceof OutputSlot;
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
        if (isAdultWorker(stack)) {
            return moveItemStackTo(
                    stack,
                    FarmerBlockEntity.WORKER_SLOT,
                    FarmerBlockEntity.WORKER_SLOT + 1,
                    false
            );
        }
        if (FarmerCropStackAdapter.isSupported(kind, stack)) {
            return moveItemStackTo(
                    stack,
                    FarmerBlockEntity.CROP_SLOT,
                    FarmerBlockEntity.CROP_SLOT + 1,
                    false
            );
        }
        if (HoeTierCatalog.isSupported(stack)) {
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

    private boolean isAdultWorker(ItemStack stack) {
        CapturedMobKind capturedKind = kind == FarmerKind.VILLAGER
                ? CapturedMobKind.VILLAGER
                : CapturedMobKind.PIGLIN;
        return CapturedMobStackAdapter.isFilledCapturer(capturedKind, stack)
                && !CapturedMobStackAdapter.isBaby(capturedKind, stack);
    }

    private final class WorkerSlot extends Slot {
        private WorkerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return isAdultWorker(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private final class CropSlot extends Slot {
        private CropSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return FarmerCropStackAdapter.isSupported(kind, stack);
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
            return HoeTierCatalog.isSupported(stack);
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
