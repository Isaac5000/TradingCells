package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.EnhancedPiglinBarterRewards;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public final class NetheritePiglinBarteringCellMenu extends AbstractContainerMenu {
    public static final int CONTROL_SLOT_X = MachineMenuLayout.machineX(8);
    public static final int UPGRADE_SLOT_Y = 42;
    public static final int FILTER_SLOT_Y = 68;
    public static final int GOLD_ROW_X = MachineMenuLayout.machineX(55);
    public static final int GOLD_ROW_Y = 36;
    public static final int OUTPUT_ROW_X = MachineMenuLayout.machineX(55);
    public static final int OUTPUT_ROW_Y = 72;
    public static final int OUTPUT_COLUMN_COUNT = 4;
    public static final int OUTPUT_ROW_SPACING = 18;
    public static final int LANE_SPACING = 18;

    private static final int MACHINE_SLOT_COUNT = NetheritePiglinBarteringCellBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;

    public NetheritePiglinBarteringCellMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT));
    }

    public NetheritePiglinBarteringCellMenu(int containerId, Inventory inventory, Container container) {
        super(TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        this.container = container;

        addSlot(new UpgradeSlot(
                container,
                NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT,
                CONTROL_SLOT_X,
                UPGRADE_SLOT_Y
        ));
        addSlot(new FilterSlot(
                container,
                NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT,
                CONTROL_SLOT_X,
                FILTER_SLOT_Y
        ));

        for (int lane = 0; lane < NetheritePiglinBarteringCellBlockEntity.GOLD_SLOT_COUNT; lane++) {
            addSlot(new GoldSlot(
                    container,
                    NetheritePiglinBarteringCellBlockEntity.FIRST_GOLD_SLOT + lane,
                    GOLD_ROW_X + lane * LANE_SPACING,
                    GOLD_ROW_Y
            ));
        }
        for (int index = 0; index < NetheritePiglinBarteringCellBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            addSlot(new OutputSlot(
                    container,
                    NetheritePiglinBarteringCellBlockEntity.FIRST_OUTPUT_SLOT + index,
                    outputSlotX(index),
                    outputSlotY(index)
            ));
        }

        addStandardInventorySlots(inventory, MachineMenuLayout.PLAYER_INVENTORY_X, MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y);
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
    }

    public boolean filterIsSupported() {
        ItemStack filter = getSlot(NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT).getItem();
        return filter.isEmpty() || EnhancedPiglinBarterRewards.isSupportedFilter(filter);
    }

    public boolean hasNetheriteUpgrade() {
        return getSlot(NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT)
                .getItem()
                .is(TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get());
    }

    public static int outputSlotX(int index) {
        return OUTPUT_ROW_X + index % OUTPUT_COLUMN_COUNT * LANE_SPACING;
    }

    public static int outputSlotY(int index) {
        return OUTPUT_ROW_Y + index / OUTPUT_COLUMN_COUNT * OUTPUT_ROW_SPACING;
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
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isProgressionUpgrade(stack)) {
            if (!moveItemStackTo(
                    stack,
                    NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT,
                    NetheritePiglinBarteringCellBlockEntity.UPGRADE_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.GOLD_INGOT)) {
            if (!moveItemStackTo(
                    stack,
                    NetheritePiglinBarteringCellBlockEntity.FIRST_GOLD_SLOT,
                    NetheritePiglinBarteringCellBlockEntity.FIRST_GOLD_SLOT
                            + NetheritePiglinBarteringCellBlockEntity.GOLD_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (hasNetheriteUpgrade()
                && getSlot(NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT).getItem().isEmpty()) {
            if (!moveItemStackTo(
                    stack,
                    NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT,
                    NetheritePiglinBarteringCellBlockEntity.FILTER_SLOT + 1,
                    false
            )) {
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
        return original;
    }

    private static boolean isProgressionUpgrade(ItemStack stack) {
        return stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_COPPER_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_IRON_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_GOLD_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_DIAMOND_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get());
    }

    private static final class UpgradeSlot extends Slot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return isProgressionUpgrade(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private final class FilterSlot extends Slot {
        private FilterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return hasNetheriteUpgrade();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class GoldSlot extends Slot {
        private GoldSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return stack.is(Items.GOLD_INGOT);
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
