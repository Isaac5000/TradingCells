package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.menu.VillagerTradeMenuLayout;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MinecraftExperience;
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

public final class SkeletonFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = VillagerTradeMenuLayout.WIDTH;
    public static final int HEIGHT = VillagerTradeMenuLayout.HEIGHT;
    public static final int WORKER_SLOT_X = 134;
    public static final int SWORD_SLOT_X = 162;
    public static final int INPUT_SLOT_Y = 31;
    public static final int OUTPUT_FIRST_X = 153;
    public static final int OUTPUT_FIRST_Y = 69;
    public static final int OUTPUT_COLUMNS = 9;
    public static final int PLAYER_INVENTORY_X = VillagerTradeMenuLayout.itemX(
            VillagerTradeMenuLayout.PLAYER_INVENTORY_X
    );
    public static final int PLAYER_INVENTORY_Y = VillagerTradeMenuLayout.itemY(
            VillagerTradeMenuLayout.PLAYER_INVENTORY_Y
    );
    public static final int PLAYER_HOTBAR_Y = VillagerTradeMenuLayout.itemY(
            VillagerTradeMenuLayout.PLAYER_HOTBAR_Y
    );
    public static final int SELECT_KIND_BUTTON_BASE = 100;
    public static final int TOGGLE_LOOT_BUTTON_BASE = 200;
    public static final int EXTRACT_EXPERIENCE_BUTTON = 300;
    private static final int MACHINE_SLOT_COUNT = SkeletonFarmBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;

    public SkeletonFarmMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(8));
    }

    public SkeletonFarmMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(SkeletonFarmRegistrationAdapter.MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 8);
        this.container = container;
        this.data = data;

        addSlot(new WorkerSlot(container, SkeletonFarmBlockEntity.WORKER_SLOT, WORKER_SLOT_X, INPUT_SLOT_Y));
        addSlot(new SwordSlot(container, SkeletonFarmBlockEntity.SWORD_SLOT, SWORD_SLOT_X, INPUT_SLOT_Y));
        for (int index = 0; index < SkeletonFarmBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            addSlot(new OutputSlot(
                    container,
                    SkeletonFarmBlockEntity.FIRST_OUTPUT_SLOT + index,
                    outputSlotX(index),
                    outputSlotY(index)
            ));
        }
        addStandardInventorySlots(inventory, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        for (Slot equipmentSlot : VillagerTradeEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(data);
    }

    public SkeletonFarmKind selectedKind() {
        return SkeletonFarmKind.fromId(data.get(2));
    }

    public int cycleTicks() {
        return data.get(0);
    }

    public int maxCycleTicks() {
        return Math.max(1, data.get(1));
    }

    public int storedExperience() {
        return Math.max(0, data.get(4));
    }

    public int storedLevels() {
        return MinecraftExperience.levelForTotalPoints(storedExperience());
    }

    public int simulatedKills() {
        return Math.max(1, data.get(6));
    }

    public boolean isHunting() {
        return data.get(5) != 0;
    }

    public boolean isLootEnabled(SkeletonFarmLoot loot) {
        return selectedKind().supports(loot) && (data.get(3) & loot.bit()) != 0;
    }

    public boolean isDeliveringOversizedBatch() {
        return data.get(7) != 0;
    }

    public static int outputSlotX(int index) {
        return OUTPUT_FIRST_X + index % OUTPUT_COLUMNS * 18;
    }

    public static int outputSlotY(int index) {
        return OUTPUT_FIRST_Y + index / OUTPUT_COLUMNS * 18;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (isDeliveringOversizedBatch()
                && buttonId >= SELECT_KIND_BUTTON_BASE
                && buttonId < TOGGLE_LOOT_BUTTON_BASE + SkeletonFarmLoot.values().length) {
            return false;
        }
        if (buttonId >= SELECT_KIND_BUTTON_BASE
                && buttonId < SELECT_KIND_BUTTON_BASE + SkeletonFarmKind.values().length) {
            data.set(2, buttonId - SELECT_KIND_BUTTON_BASE);
            return true;
        }
        if (buttonId >= TOGGLE_LOOT_BUTTON_BASE
                && buttonId < TOGGLE_LOOT_BUTTON_BASE + SkeletonFarmLoot.values().length) {
            SkeletonFarmLoot loot = SkeletonFarmLoot.values()[buttonId - TOGGLE_LOOT_BUTTON_BASE];
            if (!selectedKind().supports(loot)) {
                return false;
            }
            data.set(3, data.get(3) ^ loot.bit());
            return true;
        }
        if (buttonId == EXTRACT_EXPERIENCE_BUTTON) {
            if (container instanceof SkeletonFarmBlockEntity farm) {
                farm.extractExperience(player);
            }
            return true;
        }
        return false;
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
            if (!moveItemStackTo(stack, SkeletonFarmBlockEntity.WORKER_SLOT, SkeletonFarmBlockEntity.WORKER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (SwordTierCatalog.isSupported(stack)) {
            if (!moveItemStackTo(stack, SkeletonFarmBlockEntity.SWORD_SLOT, SkeletonFarmBlockEntity.SWORD_SLOT + 1, false)) {
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

    private static final class WorkerSlot extends Slot {
        private WorkerSlot(Container container, int slot, int x, int y) {
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

    private static final class SwordSlot extends Slot {
        private SwordSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return SwordTierCatalog.isSupported(stack);
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
