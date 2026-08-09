package com.cosmocraft.trading_cells.feature.incubators.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.incubators.application.port.input.IncubatorUseCase;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class IncubatorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;

    private static final String INPUT_TAG = "Input";
    private static final String OUTPUT_TAG = "Output";
    private static final String INCUBATION_TICKS_TAG = "IncubationTicks";
    private static final int[] INPUT_SLOTS = new int[]{INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = new int[]{OUTPUT_SLOT};

    private final CapturedMobKind kind;
    private final IncubatorUseCase incubatorService = FeatureComposition.incubator();
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final MachineActivityController activity = new MachineActivityController();
    private int incubationTicks;
    private boolean inputCacheInitialized;
    private boolean cachedBabyInput;
    private @Nullable CompoundTag preparedBlockDropData;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> incubationTicks;
                case 1 -> incubatorService.durationTicks(kind);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                activity.wake();
                incubationTicks = Math.clamp(value, 0, incubatorService.durationTicks(kind));
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    protected IncubatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CapturedMobKind kind) {
        super(type, pos, state);
        this.kind = kind;
    }

    public CapturedMobKind kind() {
        return kind;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    void processTick() {
        if (activity.remainsInactive() || activity.remainsBlocked()) {
            return;
        }
        processIncubation();
        updateActivity();
    }

    private void processIncubation() {
        ItemStack input = items.get(INPUT_SLOT);
        TimedProcess.Step step = incubatorService.advance(
                kind,
                incubationTicks,
                isBabyInput(),
                items.get(OUTPUT_SLOT).isEmpty()
        );
        int previousTicks = incubationTicks;
        incubationTicks = step.ticks();
        if (step.transition() == TimedProcess.Transition.IDLE
                || step.transition() == TimedProcess.Transition.PAUSED) {
            return;
        }
        if (step.transition() == TimedProcess.Transition.RESET) {
            if (previousTicks != 0) {
                markChangedAndSync();
            }
            return;
        }
        if (step.transition() == TimedProcess.Transition.ADVANCED) {
            setChanged();
            return;
        }

        ItemStack adult = CapturedMobStackAdapter.mature(kind, input);
        if (adult.isEmpty()) {
            incubationTicks = 0;
            markChangedAndSync();
            return;
        }

        items.set(INPUT_SLOT, ItemStack.EMPTY);
        items.set(OUTPUT_SLOT, adult);
        incubationTicks = 0;
        invalidateInputCache();
        markChangedAndSync();
    }

    public ItemStack copyDisplayStack() {
        ItemStack output = items.get(OUTPUT_SLOT);
        return (output.isEmpty() ? items.get(INPUT_SLOT) : output).copy();
    }

    public void prepareForBlockDrop(HolderLookup.Provider registries) {
        preparedBlockDropData = saveCustomOnly(registries);
        clearStoredContents();
    }

    public CompoundTag getPreparedBlockDropData(HolderLookup.Provider registries) {
        return preparedBlockDropData == null ? saveCustomOnly(registries) : preparedBlockDropData.copy();
    }

    public void discardContentsAfterBlockDrop() {
        preparedBlockDropData = null;
        clearStoredContents();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable(kind == CapturedMobKind.VILLAGER
                ? "container.trading_cells.villager_incubator"
                : "container.trading_cells.piglin_incubator");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new IncubatorMenu(kind, containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(INPUT_SLOT).isEmpty() && items.get(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (!isValidSlot(slot) || count <= 0 || items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot).split(count);
        if (items.get(slot).isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        activity.wake();
        if (slot == INPUT_SLOT) {
            incubationTicks = 0;
            invalidateInputCache();
        }
        markChangedAndSync();
        return removed;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        activity.wake();
        if (slot == INPUT_SLOT) {
            incubationTicks = 0;
            invalidateInputCache();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (slot == OUTPUT_SLOT && stack.isEmpty()) {
            items.set(OUTPUT_SLOT, ItemStack.EMPTY);
            activity.wake();
            markChangedAndSync();
            return;
        }
        if (slot != INPUT_SLOT) {
            return;
        }
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        inserted.setCount(Math.min(1, inserted.getCount()));
        items.set(INPUT_SLOT, inserted);
        incubationTicks = 0;
        invalidateInputCache();
        markChangedAndSync();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        return slot == INPUT_SLOT
                && items.get(INPUT_SLOT).isEmpty()
                && CapturedMobStackAdapter.isBaby(kind, stack);
    }

    @Override
    public void clearContent() {
        preparedBlockDropData = null;
        invalidateInputCache();
        clearStoredContents();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        return direction == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace( // NOSONAR - Minecraft explicitly permits a null automation side.
            int slot,
            @NonNull ItemStack stack,
            @Nullable Direction direction
    ) {
        return direction != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        items.set(INPUT_SLOT, input.read(INPUT_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        items.set(OUTPUT_SLOT, input.read(OUTPUT_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        incubationTicks = Math.clamp(
                input.getIntOr(INCUBATION_TICKS_TAG, 0),
                0,
                incubatorService.durationTicks(kind)
        );
        preparedBlockDropData = null;
        inputCacheInitialized = false;
        activity.reset();
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        if (!items.get(INPUT_SLOT).isEmpty()) {
            output.store(INPUT_TAG, ItemStack.CODEC, items.get(INPUT_SLOT));
        }
        if (!items.get(OUTPUT_SLOT).isEmpty()) {
            output.store(OUTPUT_TAG, ItemStack.CODEC, items.get(OUTPUT_SLOT));
        }
        if (incubationTicks > 0) {
            output.putInt(INCUBATION_TICKS_TAG, incubationTicks);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveCustomOnly(registries);
    }

    protected void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void clearStoredContents() {
        items.set(INPUT_SLOT, ItemStack.EMPTY);
        items.set(OUTPUT_SLOT, ItemStack.EMPTY);
        incubationTicks = 0;
        invalidateInputCache();
        setChanged();
    }

    private boolean isBabyInput() {
        if (!inputCacheInitialized) {
            cachedBabyInput = CapturedMobStackAdapter.isBaby(kind, items.get(INPUT_SLOT));
            inputCacheInitialized = true;
        }
        return cachedBabyInput;
    }

    private void invalidateInputCache() {
        inputCacheInitialized = false;
        activity.wake();
    }

    private void updateActivity() {
        if (items.get(INPUT_SLOT).isEmpty()) {
            activity.transition(MachineActivityController.Activity.INACTIVE);
        } else if (!isBabyInput() || !items.get(OUTPUT_SLOT).isEmpty()) {
            activity.transition(MachineActivityController.Activity.BLOCKED);
        } else {
            activity.transition(MachineActivityController.Activity.ACTIVE);
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
