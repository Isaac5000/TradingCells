package com.cosmocraft.trading_cells.feature.experience.adapters.input;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.menu.MachineMenuLayout;
import com.cosmocraft.trading_cells.platform.neoforge.menu.PlayerEquipmentSlots;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ExperienceStorageMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_END = 27;
    private static final int PLAYER_HOTBAR_END = 36;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final @Nullable ExperienceStorageBlockEntity storage;

    public ExperienceStorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, new SimpleContainerData(2));
    }

    public ExperienceStorageMenu(
            int containerId,
            Inventory inventory,
            ExperienceStorageBlockEntity storage,
            ContainerData data
    ) {
        this(
                containerId,
                inventory,
                storage,
                ContainerLevelAccess.create(
                        Objects.requireNonNull(storage.getLevel()),
                        storage.getBlockPos()
                ),
                data
        );
    }

    private ExperienceStorageMenu(
            int containerId,
            Inventory inventory,
            @Nullable ExperienceStorageBlockEntity storage,
            ContainerLevelAccess access,
            ContainerData data
    ) {
        super(ExperienceStorageRegistrationAdapter.MENU.get(), containerId);
        checkContainerDataCount(data, 2);
        this.storage = storage;
        this.access = access;
        this.data = data;
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
        return ExperienceStorageBlockEntity.CAPACITY;
    }

    public void handleTransfer(
            ServerPlayer player,
            ExperienceTransferAction action,
            int requestedLevels
    ) {
        if (storage != null && stillValid(player)) {
            storage.transferExperience(player, action, requestedLevels);
        }
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(access, player, ExperienceStorageRegistrationAdapter.BLOCK.get());
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
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        boolean moved;
        if (index >= PLAYER_HOTBAR_END) {
            moved = moveItemStackTo(stack, 0, PLAYER_HOTBAR_END, false);
        } else if (index < PLAYER_INVENTORY_END) {
            moved = moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false);
        } else {
            moved = moveItemStackTo(stack, 0, PLAYER_INVENTORY_END, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
