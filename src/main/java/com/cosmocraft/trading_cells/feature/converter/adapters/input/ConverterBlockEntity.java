package com.cosmocraft.trading_cells.feature.converter.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.converter.application.port.input.ConverterUseCase;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterCycle;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ConverterBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int VILLAGER_SLOT = 0;
    public static final int FIRST_POTION_SLOT = 1;
    public static final int POTION_SLOT_COUNT = 4;
    public static final int FIRST_APPLE_SLOT = FIRST_POTION_SLOT + POTION_SLOT_COUNT;
    public static final int APPLE_SLOT_COUNT = 4;
    public static final int STORAGE_SLOT_COUNT = FIRST_APPLE_SLOT + APPLE_SLOT_COUNT;
    public static final int CONTAINER_SIZE = STORAGE_SLOT_COUNT;
    public static final int POTION_SLOT_LIMIT = 1;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String STAGE_TAG = "Stage";
    private static final String STAGE_TICKS_TAG = "StageTicks";
    private static final String CURED_READY_TAG = "CuredReady";
    private static final String CURE_DISCOUNT_TAG = "TradingCellsCureDiscount";
    private static final int[] POTION_SLOTS = new int[]{1, 2, 3, 4};
    private static final int[] APPLE_SLOTS = new int[]{5, 6, 7, 8};
    private static final int[] NO_SLOTS = new int[0];

    private final NonNullList<ItemStack> items = NonNullList.withSize(STORAGE_SLOT_COUNT, ItemStack.EMPTY);
    private final ConverterUseCase converterService = FeatureComposition.converter();
    private ConverterStage stage = ConverterStage.IDLE;
    private int stageTicks;
    private boolean curedReady;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> stage.ordinal();
                case 1 -> stageTicks;
                case 2 -> curedReady ? 1 : 0;
                case 3 -> converterService.durationTicks(stage);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                stage = ConverterStage.fromId(value);
            } else if (index == 1) {
                stageTicks = Math.max(0, value);
            } else if (index == 2) {
                curedReady = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ConverterRegistrationAdapter.CONVERTER_BLOCK_ENTITY.get(), pos, state);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public ConverterStage stage() {
        return stage;
    }

    public int stageTicks() {
        return stageTicks;
    }

    public boolean isProcessing() {
        return stage.isProcessing();
    }

    public boolean hasStoredVillager() {
        return isAdultVillager(items.get(VILLAGER_SLOT));
    }

    public ItemStack copyDisplayVillagerStack() {
        return items.get(VILLAGER_SLOT).copy();
    }

    public InteractionResult insertVillagerFromCapturer(ItemStack stack) {
        if (hasStoredVillager()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!isAdultVillager(stack)) {
            return InteractionResult.PASS;
        }
        items.set(VILLAGER_SLOT, stack.copyWithCount(1));
        CapturedMobStackAdapter.clearData(CapturedMobKind.VILLAGER, stack);
        stage = ConverterStage.IDLE;
        stageTicks = 0;
        curedReady = false;
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractVillagerToCapturer(ItemStack stack, Player player) {
        if (isProcessing()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!hasStoredVillager()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        CompoundTag data = CapturedMobStackAdapter.copyData(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT)
        );
        if (data == null) {
            return InteractionResult.FAIL;
        }
        ItemStack target = stack.getCount() <= 1 ? stack : new ItemStack(stack.getItem());
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, target, data);
        if (target != stack) {
            stack.shrink(1);
            if (!player.getInventory().add(target)) {
                player.drop(target, false);
            }
        }
        items.set(VILLAGER_SLOT, ItemStack.EMPTY);
        stage = ConverterStage.IDLE;
        stageTicks = 0;
        curedReady = false;
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void processTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean canStart = hasIngredient(POTION_SLOTS, true)
                && hasIngredient(APPLE_SLOTS, false);
        ConverterCycle.Step step = converterService.advance(
                stage,
                stageTicks,
                isAdultVillager(items.get(VILLAGER_SLOT)),
                canStart,
                curedReady
        );
        stage = step.stage();
        stageTicks = step.ticks();
        switch (step.transition()) {
            case IDLE -> {
                // No state mutation is required while the converter remains idle.
            }
            case STARTED -> {
                consumeIngredient(POTION_SLOTS, true);
                consumeIngredient(APPLE_SLOTS, false);
                markChangedAndSync();
            }
            case ADVANCED -> {
                setChanged();
                if (stageTicks % 20 == 0) {
                    markChangedAndSync();
                }
            }
            case INFECTED, CANCELLED -> markChangedAndSync();
            case CURED -> {
                applyCureDiscount();
                curedReady = true;
                markChangedAndSync();
            }
        }
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.converter");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player // NOSONAR - MenuProvider fixes this parameter even when this menu needs only the inventory.
    ) {
        return new ConverterMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        return isStorageSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (!isStorageSlot(slot)
                || slot == VILLAGER_SLOT && isProcessing()
                || count <= 0
                || items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        int amount = isPotionSlot(slot) ? Math.min(POTION_SLOT_LIMIT, count) : count;
        ItemStack removed = items.get(slot).split(amount);
        if (items.get(slot).isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == VILLAGER_SLOT) {
            curedReady = false;
            cancelProcess();
        }
        markChangedAndSync();
        return removed;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (!isStorageSlot(slot) || slot == VILLAGER_SLOT && isProcessing()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot == VILLAGER_SLOT) {
            curedReady = false;
            cancelProcess();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isStorageSlot(slot)
                || slot == VILLAGER_SLOT && isProcessing()
                || !stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        if (slot == VILLAGER_SLOT) {
            inserted.setCount(Math.min(1, inserted.getCount()));
            curedReady = false;
            cancelProcess();
        } else if (isPotionSlot(slot)) {
            inserted.setCount(Math.min(POTION_SLOT_LIMIT, inserted.getCount()));
        } else {
            inserted.setCount(Math.min(inserted.getMaxStackSize(), inserted.getCount()));
        }
        items.set(slot, inserted);
        markChangedAndSync();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        if (slot == VILLAGER_SLOT) {
            return !isProcessing() && isAdultVillager(stack);
        }
        if (isPotionSlot(slot)) {
            return ConverterIngredientAdapter.isWeaknessPotion(stack);
        }
        return isAppleSlot(slot) && ConverterIngredientAdapter.isGoldenApple(stack);
    }

    @Override
    public void clearContent() {
        clearContentsForBlockDrop();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        Direction facing = getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        // Viewed from the front of the block, clockwise is its physical left side.
        if (direction == facing.getClockWise()) {
            return POTION_SLOTS;
        }
        if (direction == facing.getCounterClockWise()) {
            return APPLE_SLOTS;
        }
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NonNull ItemStack stack, @Nullable Direction direction) {
        if (direction == null) {
            return false;
        }
        for (int allowedSlot : getSlotsForFace(direction)) {
            if (allowedSlot == slot) {
                return canPlaceItem(slot, stack);
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return false;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < STORAGE_SLOT_COUNT; slot++) {
            ItemStack loaded = input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (isPotionSlot(slot)) {
                if (ConverterIngredientAdapter.isWeaknessPotion(loaded)) {
                    loaded.setCount(Math.min(POTION_SLOT_LIMIT, loaded.getCount()));
                } else {
                    loaded = ItemStack.EMPTY;
                }
            } else if (isAppleSlot(slot) && !ConverterIngredientAdapter.isGoldenApple(loaded)) {
                loaded = ItemStack.EMPTY;
            }
            items.set(slot, loaded);
        }
        stage = ConverterStage.fromId(input.getIntOr(STAGE_TAG, 0));
        stageTicks = Math.clamp(
                input.getIntOr(STAGE_TICKS_TAG, 0),
                0,
                converterService.durationTicks(stage)
        );
        curedReady = input.getBooleanOr(CURED_READY_TAG, false);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < STORAGE_SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, stack);
            }
        }
        if (stage != ConverterStage.IDLE) {
            output.putInt(STAGE_TAG, stage.ordinal());
            output.putInt(STAGE_TICKS_TAG, stageTicks);
        }
        if (curedReady) {
            output.putBoolean(CURED_READY_TAG, true);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < STORAGE_SLOT_COUNT; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        stage = ConverterStage.IDLE;
        stageTicks = 0;
        curedReady = false;
        setChanged();
    }

    private void applyCureDiscount() {
        CapturedMobStackAdapter.updatePersistentInt(
                CapturedMobKind.VILLAGER,
                items.get(VILLAGER_SLOT),
                CURE_DISCOUNT_TAG,
                converterService::increasedCureDiscount
        );
    }

    private boolean hasIngredient(int[] slots, boolean potion) {
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (potion
                    ? ConverterIngredientAdapter.isWeaknessPotion(stack)
                    : ConverterIngredientAdapter.isGoldenApple(stack)) {
                return true;
            }
        }
        return false;
    }

    private void consumeIngredient(int[] slots, boolean potion) {
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (potion
                    ? ConverterIngredientAdapter.isWeaknessPotion(stack)
                    : ConverterIngredientAdapter.isGoldenApple(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    private void cancelProcess() {
        if (stage != ConverterStage.IDLE || stageTicks != 0) {
            stage = ConverterStage.IDLE;
            stageTicks = 0;
            markChangedAndSync();
        }
    }

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static boolean isPotionSlot(int slot) {
        return slot >= FIRST_POTION_SLOT && slot < FIRST_APPLE_SLOT;
    }

    private static boolean isAppleSlot(int slot) {
        return slot >= FIRST_APPLE_SLOT && slot < STORAGE_SLOT_COUNT;
    }

    private static boolean isStorageSlot(int slot) {
        return slot >= 0 && slot < STORAGE_SLOT_COUNT;
    }
}
