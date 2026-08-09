package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.network.QuarryCatalogSyncPayload;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public final class QuarryMenu extends AbstractContainerMenu {
    public static final int TOGGLE_DEEP_MINING_BUTTON = 0;
    public static final int WORKER_SLOT_X = MachineMenuLayout.machineX(44);
    public static final int PICKAXE_SLOT_X = MachineMenuLayout.machineX(80);
    public static final int UPGRADE_SLOT_X = MachineMenuLayout.machineX(116);
    public static final int INPUT_SLOT_Y = 24;
    public static final int OUTPUT_COLUMN_COUNT = 9;
    public static final int OUTPUT_SLOT_FIRST_X = MachineMenuLayout.machineX(8);
    public static final int OUTPUT_SLOT_FIRST_Y = 81;
    public static final int OUTPUT_SLOT_SPACING = 18;

    private static final int MACHINE_SLOT_COUNT = QuarryBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final QuarryKind kind;
    private final Container container;
    private final ContainerData data;
    private List<QuarryCatalogSyncPayload.Entry> catalogEntries = List.of();
    private boolean catalogDeepMining;
    private int catalogNetworkRevision = -1;

    public static QuarryMenu villager(int containerId, Inventory inventory) {
        return clientMenu(QuarryKind.VILLAGER, containerId, inventory);
    }

    public static QuarryMenu piglin(int containerId, Inventory inventory) {
        return clientMenu(QuarryKind.PIGLIN, containerId, inventory);
    }

    private static QuarryMenu clientMenu(QuarryKind kind, int containerId, Inventory inventory) {
        return new QuarryMenu(
                kind,
                containerId,
                inventory,
                new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(8)
        );
    }

    public QuarryMenu(
            QuarryKind kind,
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(kind == QuarryKind.VILLAGER
                ? QuarryRegistrationAdapter.QUARRY_MENU.get()
                : QuarryRegistrationAdapter.PIGLIN_QUARRY_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 8);
        this.kind = kind;
        this.container = container;
        this.data = data;

        addSlot(new WorkerSlot(container, QuarryBlockEntity.WORKER_SLOT, WORKER_SLOT_X, INPUT_SLOT_Y));
        addSlot(new PickaxeSlot(container, QuarryBlockEntity.PICKAXE_SLOT, PICKAXE_SLOT_X, INPUT_SLOT_Y));
        addSlot(new UpgradeSlot(container, QuarryBlockEntity.UPGRADE_SLOT, UPGRADE_SLOT_X, INPUT_SLOT_Y));
        for (int index = 0; index < QuarryBlockEntity.OUTPUT_SLOT_COUNT; index++) {
            addSlot(new OutputSlot(
                    container,
                    QuarryBlockEntity.FIRST_OUTPUT_SLOT + index,
                    outputSlotX(index),
                    outputSlotY(index)
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

    public QuarryKind kind() {
        return kind;
    }

    public int cycleTicks() {
        return data.get(0);
    }

    public int maximumCycleTicks() {
        return Math.max(1, data.get(1));
    }

    public boolean isMining() {
        return data.get(2) != 0;
    }

    public boolean deepMining() {
        return data.get(3) != 0;
    }

    public boolean deepMiningAvailable() {
        return data.get(4) != 0;
    }

    public QuarryStatus status() {
        return QuarryStatus.fromIndex(data.get(5));
    }

    public ItemStack lastResult() {
        int rawId = data.get(6);
        Item item = BuiltInRegistries.ITEM.byId(rawId);
        return rawId <= 0 || item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public int catalogRevision() {
        return data.get(7);
    }

    public List<QuarryCatalogSyncPayload.Entry> catalogEntries() {
        return catalogEntries;
    }

    public boolean catalogDeepMining() {
        return catalogDeepMining;
    }

    public int catalogNetworkRevision() {
        return catalogNetworkRevision;
    }

    public void applyCatalogSnapshot(QuarryCatalogSyncPayload payload) {
        catalogEntries = List.copyOf(payload.entries());
        catalogDeepMining = payload.deepMining();
        catalogNetworkRevision = payload.revision();
    }

    public ItemStack pickaxe() {
        return container.getItem(QuarryBlockEntity.PICKAXE_SLOT);
    }

    public ItemStack upgrade() {
        return container.getItem(QuarryBlockEntity.UPGRADE_SLOT);
    }

    public void setClientDeepMining(boolean enabled) {
        data.set(3, enabled ? 1 : 0);
    }

    public QuarryMaterialCatalog.CatalogSnapshot serverCatalogSnapshot() {
        if (container instanceof QuarryBlockEntity quarry) {
            return quarry.catalogSnapshot();
        }
        return QuarryMaterialCatalog.snapshot(
                kind,
                QuarryUpgradeItems.tier(upgrade()),
                pickaxe(),
                deepMining()
        );
    }

    public static int outputSlotX(int index) {
        return OUTPUT_SLOT_FIRST_X + index % OUTPUT_COLUMN_COUNT * OUTPUT_SLOT_SPACING;
    }

    public static int outputSlotY(int index) {
        return OUTPUT_SLOT_FIRST_Y + index / OUTPUT_COLUMN_COUNT * OUTPUT_SLOT_SPACING;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId != TOGGLE_DEEP_MINING_BUTTON
                || !(container instanceof QuarryBlockEntity quarry)
                || !quarry.toggleDeepMining(player)) {
            return false;
        }
        container.setChanged();
        return true;
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
        if (QuarryBlockEntity.isValidWorker(kind, stack)) {
            return moveItemStackTo(stack, QuarryBlockEntity.WORKER_SLOT, QuarryBlockEntity.WORKER_SLOT + 1, false);
        }
        if (QuarryPickaxeCatalog.isSupported(stack)) {
            return moveItemStackTo(stack, QuarryBlockEntity.PICKAXE_SLOT, QuarryBlockEntity.PICKAXE_SLOT + 1, false);
        }
        if (QuarryUpgradeItems.isUpgrade(stack)) {
            return moveItemStackTo(stack, QuarryBlockEntity.UPGRADE_SLOT, QuarryBlockEntity.UPGRADE_SLOT + 1, false);
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }

    private final class WorkerSlot extends Slot {
        private WorkerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return QuarryBlockEntity.isValidWorker(kind, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class PickaxeSlot extends Slot {
        private PickaxeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return QuarryPickaxeCatalog.isSupported(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class UpgradeSlot extends Slot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return QuarryUpgradeItems.isUpgrade(stack);
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
