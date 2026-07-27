package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import net.minecraft.nbt.CompoundTag;
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

public final class BreederMenu extends AbstractContainerMenu {
    public static final int SELECT_VARIANT_BUTTON_BASE = 20;
    public static final int FOOD_SLOT_X = MachineMenuLayout.machineX(80);
    public static final int FOOD_SLOT_Y = 24;
    public static final int PARENT_A_SLOT_X = MachineMenuLayout.machineX(44);
    public static final int PARENT_B_SLOT_X = MachineMenuLayout.machineX(116);
    public static final int PARENT_SLOT_Y = 44;
    public static final int BABY_PREVIEW_SLOT_X = MachineMenuLayout.machineX(80);
    public static final int BABY_PREVIEW_SLOT_Y = 82;
    public static final int EMPTY_CAPTURER_SLOT_X = MachineMenuLayout.machineX(48);
    public static final int FILLED_CAPTURER_SLOT_X = MachineMenuLayout.machineX(112);
    public static final int CAPTURER_SLOT_Y = 100;
    private static final int DATA_COUNT = 6;
    private static final int BREEDER_SLOT_COUNT = BreederBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = BREEDER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final BreederKind kind;
    private final Container container;
    private final ContainerData data;

    public static BreederMenu villager(int containerId, Inventory inventory) {
        return new BreederMenu(
                BreederKind.VILLAGER,
                containerId,
                inventory,
                new SimpleContainer(BREEDER_SLOT_COUNT),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public static BreederMenu piglin(int containerId, Inventory inventory) {
        return new BreederMenu(
                BreederKind.PIGLIN,
                containerId,
                inventory,
                new SimpleContainer(BREEDER_SLOT_COUNT),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public BreederMenu(
            BreederKind kind,
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData containerData
    ) {
        super(kind == BreederKind.VILLAGER
                ? BreederRegistrationAdapter.VILLAGER_BREEDER_MENU.get()
                : BreederRegistrationAdapter.PIGLIN_BREEDER_MENU.get(), containerId);
        checkContainerSize(container, BREEDER_SLOT_COUNT);
        checkContainerDataCount(containerData, DATA_COUNT);
        this.kind = kind;
        this.container = container;
        this.data = containerData;

        addSlot(new FilteredSlot(container, BreederBlockEntity.FOOD_SLOT, FOOD_SLOT_X, FOOD_SLOT_Y, this::isValidFood));
        addSlot(new FilteredSlot(container, BreederBlockEntity.PARENT_A_SLOT, PARENT_A_SLOT_X, PARENT_SLOT_Y, this::isValidAdultParent));
        addSlot(new FilteredSlot(container, BreederBlockEntity.PARENT_B_SLOT, PARENT_B_SLOT_X, PARENT_SLOT_Y, this::isValidAdultParent));
        addSlot(new PreviewSlot(container, BreederBlockEntity.BABY_PREVIEW_SLOT, BABY_PREVIEW_SLOT_X, BABY_PREVIEW_SLOT_Y));
        addSlot(new FilteredSlot(container, BreederBlockEntity.EMPTY_CAPTURER_SLOT, EMPTY_CAPTURER_SLOT_X, CAPTURER_SLOT_Y, this::isEmptyCapturer));
        addSlot(new OutputSlot(container, BreederBlockEntity.FILLED_CAPTURER_SLOT, FILLED_CAPTURER_SLOT_X, CAPTURER_SLOT_Y));

        addStandardInventorySlots(inventory, MachineMenuLayout.PLAYER_INVENTORY_X, MachineMenuLayout.PLAYER_INVENTORY_SLOT_Y);
        for (Slot equipmentSlot : PlayerEquipmentSlots.create(inventory)) {
            addSlot(equipmentSlot);
        }
        addDataSlots(containerData);
    }

    public BreederKind kind() {
        return kind;
    }

    public int breedTicks() {
        return data.get(0);
    }

    public int pendingBabies() {
        return data.get(1);
    }

    public int maxBreedTicks() {
        return Math.max(1, data.get(3));
    }

    public int foodCost(BreederFood food) {
        if (!BreederRecipe.isFood(kind, food)) {
            return 0;
        }
        return Math.max(1, switch (food) {
            case BREAD, PORK -> data.get(4);
            case VEGETABLE, CRIMSON_FUNGUS -> data.get(5);
            case NONE -> 0;
        });
    }

    public String selectedVillagerVariantKey() {
        return VillagerVariantSelection.translationKey(data.get(2));
    }

    public int selectedVillagerVariant() {
        return VillagerVariantSelection.normalize(data.get(2));
    }

    public int villagerVariantCount() {
        return VillagerVariantSelection.count();
    }

    public String villagerVariantKey(int index) {
        return VillagerVariantSelection.translationKey(index);
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (kind != BreederKind.VILLAGER) {
            return false;
        }
        int variant = buttonId - SELECT_VARIANT_BUTTON_BASE;
        if (variant < 0 || variant >= VillagerVariantSelection.count()) {
            return false;
        }
        data.set(2, variant);
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

        if (index == BreederBlockEntity.BABY_PREVIEW_SLOT) {
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
        if (index < BREEDER_SLOT_COUNT) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true);
        }
        if (isValidFood(stack)) {
            return moveItemStackTo(
                    stack,
                    BreederBlockEntity.FOOD_SLOT,
                    BreederBlockEntity.FOOD_SLOT + 1,
                    false
            );
        }
        if (isValidAdultParent(stack)) {
            return moveItemStackTo(
                    stack,
                    BreederBlockEntity.PARENT_A_SLOT,
                    BreederBlockEntity.PARENT_B_SLOT + 1,
                    false
            );
        }
        if (isEmptyCapturer(stack)) {
            return moveItemStackTo(
                    stack,
                    BreederBlockEntity.EMPTY_CAPTURER_SLOT,
                    BreederBlockEntity.EMPTY_CAPTURER_SLOT + 1,
                    false
            );
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }

    private boolean isValidFood(ItemStack stack) {
        return BreederRecipe.isFood(kind, MinecraftBreederFood.from(stack));
    }

    private boolean isValidAdultParent(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(capturerItem())) {
            return false;
        }
        CompoundTag data = CapturedMobStackAdapter.copyData(capturedKind(), stack);
        return data != null && !CapturedMobStackAdapter.isBaby(capturedKind(), data);
    }

    private boolean isEmptyCapturer(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(capturerItem())) {
            return false;
        }
        return !CapturedMobStackAdapter.isFilledCapturer(capturedKind(), stack);
    }

    private Item capturerItem() {
        return CapturedMobStackAdapter.capturerItem(capturedKind());
    }

    private CapturedMobKind capturedKind() {
        return kind == BreederKind.VILLAGER
                ? CapturedMobKind.VILLAGER
                : CapturedMobKind.PIGLIN;
    }

    @FunctionalInterface
    private interface StackFilter {
        boolean test(ItemStack stack);
    }

    private static final class FilteredSlot extends Slot {
        private final StackFilter filter;

        private FilteredSlot(Container container, int slot, int x, int y, StackFilter filter) {
            super(container, slot, x, y);
            this.filter = filter;
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return filter.test(stack);
        }
    }

    private static final class PreviewSlot extends Slot {
        private PreviewSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
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
