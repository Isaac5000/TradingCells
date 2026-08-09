package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
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

public final class ArcaneInfuserMenu extends AbstractContainerMenu {
    public static final int TOGGLE_AUTOMATIC_BUTTON = 0;
    public static final int TOP_SLOT_X = MachineMenuLayout.machineX(16);
    public static final int TOP_SLOT_Y = 44;
    public static final int LEFT_SLOT_X = MachineMenuLayout.machineX(-2);
    public static final int LEFT_SLOT_Y = 63;
    public static final int CENTER_SLOT_X = MachineMenuLayout.machineX(16);
    public static final int CENTER_SLOT_Y = 63;
    public static final int RIGHT_SLOT_X = MachineMenuLayout.machineX(34);
    public static final int RIGHT_SLOT_Y = 63;
    public static final int BOTTOM_SLOT_X = MachineMenuLayout.machineX(16);
    public static final int BOTTOM_SLOT_Y = 82;
    public static final int OUTPUT_SLOT_X = MachineMenuLayout.machineX(75);
    public static final int OUTPUT_SLOT_Y = 63;

    private static final int MACHINE_SLOT_COUNT = ArcaneInfuserBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;

    public ArcaneInfuserMenu(int containerId, Inventory inventory) {
        this(
                containerId,
                inventory,
                new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(6)
        );
    }

    public ArcaneInfuserMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(ArcaneInfuserRegistrationAdapter.MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 6);
        this.container = container;
        this.data = data;

        addSlot(new Slot(container, ArcaneInfuserBlockEntity.TOP_SLOT, TOP_SLOT_X, TOP_SLOT_Y));
        addSlot(new Slot(container, ArcaneInfuserBlockEntity.LEFT_SLOT, LEFT_SLOT_X, LEFT_SLOT_Y));
        addSlot(new Slot(container, ArcaneInfuserBlockEntity.CENTER_SLOT, CENTER_SLOT_X, CENTER_SLOT_Y));
        addSlot(new Slot(container, ArcaneInfuserBlockEntity.RIGHT_SLOT, RIGHT_SLOT_X, RIGHT_SLOT_Y));
        addSlot(new Slot(container, ArcaneInfuserBlockEntity.BOTTOM_SLOT, BOTTOM_SLOT_X, BOTTOM_SLOT_Y));
        addSlot(new OutputSlot(
                container,
                ArcaneInfuserBlockEntity.OUTPUT_SLOT,
                OUTPUT_SLOT_X,
                OUTPUT_SLOT_Y,
                this::resultCanBeTaken
        ));
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

    public int storedExperience() {
        return (data.get(0) & 0xFFFF) | ((data.get(1) & 0x7FFF) << 16);
    }

    public int capacity() {
        return ArcaneInfuserBlockEntity.EXPERIENCE_CAPACITY;
    }

    public boolean automaticMode() {
        return data.get(2) != 0;
    }

    public int requiredExperience() {
        return (data.get(3) & 0xFFFF) | ((data.get(4) & 0x7FFF) << 16);
    }

    public boolean insufficientRecipeExperience() {
        return data.get(5) == ArcaneInfuserBlockEntity.OUTPUT_STATE_INSUFFICIENT_EXPERIENCE;
    }

    private boolean resultCanBeTaken() {
        int outputState = data.get(5);
        return outputState == ArcaneInfuserBlockEntity.OUTPUT_STATE_MANUAL_READY
                || outputState == ArcaneInfuserBlockEntity.OUTPUT_STATE_PHYSICAL;
    }

    public void setClientAutomaticMode(boolean enabled) {
        data.set(2, enabled ? 1 : 0);
    }

    public void handleTransfer(
            ServerPlayer player,
            ArcaneInfusionTransferAction action,
            int requestedLevels
    ) {
        if (container instanceof ArcaneInfuserBlockEntity infuser && stillValid(player)) {
            infuser.transferExperience(player, action, requestedLevels);
        }
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId != TOGGLE_AUTOMATIC_BUTTON
                || !(container instanceof ArcaneInfuserBlockEntity infuser)
                || !stillValid(player)) {
            return false;
        }
        infuser.toggleAutomaticMode();
        return true;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return container.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return !(target instanceof OutputSlot);
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (index == ArcaneInfuserBlockEntity.OUTPUT_SLOT) {
            return quickMoveResult(player, slot);
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

    private ItemStack quickMoveResult(Player player, Slot slot) {
        if (!slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = slot.getItem();
        ItemStack original = result.copy();
        if (!moveItemStackTo(result, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
            return ItemStack.EMPTY;
        }
        slot.onQuickCraft(result, original);
        if (result.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(ItemStack.EMPTY, original);
        slot.onTake(player, result);
        return original;
    }

    private boolean moveQuickMovedStack(int index, ItemStack stack) {
        if (index < MACHINE_SLOT_COUNT) {
            return moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true);
        }
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            if (slots.get(slot).mayPlace(stack) && moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return index < PLAYER_INVENTORY_END
                ? moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)
                : moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false);
    }

    private static final class OutputSlot extends Slot {
        private final BooleanSupplier canTakeResult;
        private ItemStack pendingResult = ItemStack.EMPTY;

        private OutputSlot(
                Container container,
                int slot,
                int x,
                int y,
                BooleanSupplier canTakeResult
        ) {
            super(container, slot, x, y);
            this.canTakeResult = canTakeResult;
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return canTakeResult.getAsBoolean();
        }

        @Override
        public @NonNull ItemStack getItem() {
            return container instanceof ArcaneInfuserBlockEntity infuser
                    ? infuser.visibleResult()
                    : super.getItem();
        }

        @Override
        public @NonNull ItemStack remove(int amount) {
            if (!(container instanceof ArcaneInfuserBlockEntity)) {
                return super.remove(amount);
            }
            if (!canTakeResult.getAsBoolean()) {
                return ItemStack.EMPTY;
            }
            ItemStack visible = getItem();
            if (visible.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            pendingResult = visible.copyWithCount(Math.min(amount, visible.getCount()));
            return pendingResult.copy();
        }

        @Override
        public void setByPlayer(@NonNull ItemStack stack, @NonNull ItemStack previous) {
            if (!(container instanceof ArcaneInfuserBlockEntity)) {
                super.setByPlayer(stack, previous);
            } else if (stack.isEmpty() && !previous.isEmpty()) {
                pendingResult = previous.copy();
            }
        }

        @Override
        public void set(@NonNull ItemStack stack) {
            if (!(container instanceof ArcaneInfuserBlockEntity)) {
                super.set(stack);
            }
        }

        @Override
        public void onTake(@NonNull Player player, @NonNull ItemStack stack) {
            if (container instanceof ArcaneInfuserBlockEntity infuser && !pendingResult.isEmpty()) {
                infuser.takeVisibleResult(pendingResult);
                pendingResult = ItemStack.EMPTY;
            }
            super.onTake(player, stack);
        }

        @Override
        public boolean isFake() {
            return true;
        }
    }
}
