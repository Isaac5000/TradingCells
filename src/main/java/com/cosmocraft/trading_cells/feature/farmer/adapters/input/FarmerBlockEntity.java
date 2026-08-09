package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.application.port.input.FarmerUseCase;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerYield;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.machine.OrderedOutputInserter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class FarmerBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int WORKER_SLOT = 0;
    public static final int CROP_SLOT = 1;
    public static final int HOE_SLOT = 2;
    public static final int FIRST_OUTPUT_SLOT = 3;
    public static final int OUTPUT_SLOT_COUNT = FarmerHarvest.MAX_DISTINCT_OUTPUTS;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String GROWTH_TICKS_TAG = "GrowthTicks";
    private static final String GROWTH_DURATION_TICKS_TAG = "GrowthDurationTicks";
    private static final int[] TOP_SLOTS = new int[]{CROP_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{WORKER_SLOT, HOE_SLOT};
    private static final int[] BOTTOM_SLOTS = IntStream.range(FIRST_OUTPUT_SLOT, CONTAINER_SIZE).toArray();
    private static final int[] NO_SLOTS = new int[0];
    private final FarmerKind kind;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final FarmerUseCase farmerService = FeatureComposition.farmer();
    private final MachineActivityController activity = new MachineActivityController();
    private int growthTicks;
    private int growthDurationTicks = farmerService.baseGrowthTicks();
    private boolean cultivating;
    private List<ItemStack> pendingDynamicHarvest = List.of();
    private boolean pendingDynamicHarvestReady;
    private ItemStack cachedWorker = ItemStack.EMPTY;
    private boolean cachedAdultWorker;
    private boolean workerCacheInitialized;
    private ItemStack cachedCropStack = ItemStack.EMPTY;
    private FarmerCrop cachedCrop = FarmerCrop.NONE;
    private boolean cachedDynamicCrop;
    private boolean cropCacheInitialized;
    private ItemStack cachedHoe = ItemStack.EMPTY;
    private double cachedHoeSpeed;
    private double cachedHoeTierPosition;
    private int cachedEfficiencyLevel;
    private int cachedFortuneLevel;
    private boolean hoeCacheInitialized;
    private FarmerHarvest cachedHarvest = FarmerHarvest.of();
    private List<ItemStack> cachedMaximumHarvest = List.of();
    private FarmerCrop cachedHarvestCrop = FarmerCrop.NONE;
    private int cachedHarvestFortune = Integer.MIN_VALUE;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> growthTicks;
                case 1 -> growthDurationTicks;
                case 2 -> cultivating ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                activity.wake();
                refreshRuntimeCaches();
                growthDurationTicks = effectiveGrowthTicks();
                growthTicks = Math.clamp(value, 0, growthDurationTicks);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    protected FarmerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            FarmerKind kind
    ) {
        super(type, pos, state);
        this.kind = kind;
    }

    public FarmerKind kind() {
        return kind;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public int growthTicks() {
        return growthTicks;
    }

    public int growthDurationTicks() {
        return growthDurationTicks;
    }

    public FarmerCrop crop() {
        return FarmerCropStackAdapter.from(kind, items.get(CROP_SLOT));
    }

    @Override
    public void processTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (activity.remainsInactive()) {
            return;
        }

        refreshRuntimeCaches();
        boolean canCultivate = cachedAdultWorker
                && (cachedCrop != FarmerCrop.NONE || cachedDynamicCrop);
        int maximumGrowthTicks = updateGrowthDuration();
        boolean completingCycle = growthTicks >= maximumGrowthTicks - 1;
        if (!cachedDynamicCrop || !canCultivate) {
            clearPendingDynamicHarvest();
        } else if (completingCycle && !pendingDynamicHarvestReady) {
            pendingDynamicHarvest = FarmerCropStackAdapter.dynamicVillagerHarvest(
                    serverLevel,
                    worldPosition,
                    items.get(CROP_SLOT),
                    items.get(HOE_SLOT),
                    cachedFortuneLevel
            );
            pendingDynamicHarvestReady = true;
        }
        boolean outputAvailable = false;
        if (canCultivate && activity.remainsBlocked()) {
            setCultivating(false);
            return;
        }
        if (canCultivate) {
            outputAvailable = cachedDynamicCrop
                    ? !completingCycle || canStoreStacks(pendingDynamicHarvest)
                    : canStoreStacks(cachedMaximumHarvest);
        }
        activity.transition(canCultivate
                ? outputAvailable
                        ? MachineActivityController.Activity.ACTIVE
                        : MachineActivityController.Activity.BLOCKED
                : MachineActivityController.Activity.INACTIVE);
        int previousTicks = growthTicks;
        TimedProcess.Step step = farmerService.advance(
                growthTicks,
                maximumGrowthTicks,
                canCultivate,
                outputAvailable
        );
        growthTicks = step.ticks();
        setCultivating(canCultivate && outputAvailable);
        if (step.transition() == TimedProcess.Transition.IDLE
                || step.transition() == TimedProcess.Transition.PAUSED) {
            return;
        }
        if (step.transition() == TimedProcess.Transition.RESET) {
            clearPendingDynamicHarvest();
            if (previousTicks != 0) {
                markChangedAndSync();
            }
            return;
        }
        if (step.transition() == TimedProcess.Transition.ADVANCED) {
            setChanged();
            if (growthTicks % 20 == 0) {
                markChangedAndSync();
            }
            return;
        }

        if (cachedDynamicCrop) {
            storeStacks(pendingDynamicHarvest);
            clearPendingDynamicHarvest();
        } else {
            storeHarvest(cachedHarvest);
        }
        damageHoeAfterHarvest();
        markChangedAndSync();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable(kind == FarmerKind.VILLAGER
                ? "container.trading_cells.farmer"
                : "container.trading_cells.piglin_farmer");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new FarmerMenu(kind, containerId, inventory, this, dataAccess);
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
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (!isValidSlot(slot) || count <= 0 || items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        updateGrowthDuration();
        ItemStack removed = items.get(slot).split(count);
        if (items.get(slot).isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == WORKER_SLOT || (slot == CROP_SLOT && items.get(slot).isEmpty())) {
            growthTicks = 0;
            cultivating = false;
        }
        if (slot == CROP_SLOT || slot == HOE_SLOT) {
            clearPendingDynamicHarvest();
        }
        invalidateCache(slot);
        if (slot == HOE_SLOT) {
            updateGrowthDuration();
        }
        markChangedAndSync();
        return removed;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        updateGrowthDuration();
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot == WORKER_SLOT || slot == CROP_SLOT) {
            growthTicks = 0;
            cultivating = false;
        }
        if (slot == CROP_SLOT || slot == HOE_SLOT) {
            clearPendingDynamicHarvest();
        }
        invalidateCache(slot);
        if (slot == HOE_SLOT) {
            updateGrowthDuration();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack previous = items.get(slot);
        updateGrowthDuration();
        ItemStack inserted = stack.copy();
        int max = slot == WORKER_SLOT || slot == CROP_SLOT || slot == HOE_SLOT
                ? 1
                : Math.min(64, inserted.getMaxStackSize());
        inserted.setCount(Math.min(max, inserted.getCount()));
        items.set(slot, inserted);
        if (slot == WORKER_SLOT || (slot == CROP_SLOT
                && !ItemStack.isSameItemSameComponents(previous, inserted))) {
            growthTicks = 0;
            cultivating = false;
        }
        if (slot == CROP_SLOT || slot == HOE_SLOT) {
            clearPendingDynamicHarvest();
        }
        invalidateCache(slot);
        if (slot == HOE_SLOT) {
            updateGrowthDuration();
        }
        markChangedAndSync();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        return switch (slot) {
            case WORKER_SLOT -> isAdultWorkerUncached(stack);
            case CROP_SLOT -> FarmerCropStackAdapter.isSupported(kind, stack)
                    && items.get(CROP_SLOT).isEmpty();
            case HOE_SLOT -> HoeTierCatalog.isSupported(stack);
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        clearContentsForBlockDrop();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        if (direction == Direction.UP) {
            return TOP_SLOTS;
        }
        if (direction == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        return direction.getAxis().isHorizontal() ? SIDE_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NonNull ItemStack stack, @Nullable Direction direction) {
        if (direction == Direction.DOWN) {
            return false;
        }
        if (slot == CROP_SLOT
                && (!items.get(CROP_SLOT).isEmpty() || stack.getCount() != 1)) {
            return false;
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && isOutputSlot(slot);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        int storedGrowthTicks = input.getIntOr(GROWTH_TICKS_TAG, 0);
        growthDurationTicks = Math.max(
                1,
                input.getIntOr(
                        GROWTH_DURATION_TICKS_TAG,
                        farmerService.baseGrowthTicks()
                )
        );
        growthTicks = Math.clamp(storedGrowthTicks, 0, growthDurationTicks);
        cultivating = false;
        clearPendingDynamicHarvest();
        invalidateRuntimeCaches();
        activity.reset();
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        if (growthTicks > 0) {
            output.putInt(GROWTH_TICKS_TAG, growthTicks);
            output.putInt(GROWTH_DURATION_TICKS_TAG, growthDurationTicks);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        growthTicks = 0;
        growthDurationTicks = farmerService.baseGrowthTicks();
        cultivating = false;
        clearPendingDynamicHarvest();
        invalidateRuntimeCaches();
        activity.reset();
        setChanged();
    }

    private void damageHoeAfterHarvest() {
        ItemStack hoe = items.get(HOE_SLOT);
        if (!farmerService.damagesHoe()
                || hoe.isEmpty()
                || !(level instanceof ServerLevel serverLevel)
                || FarmerEnchantments.protectsHoe(hoe, serverLevel.registryAccess())) {
            return;
        }

        hoe.hurtAndBreak(
                1,
                serverLevel,
                (LivingEntity) null,
                ignored -> items.set(HOE_SLOT, ItemStack.EMPTY)
        );
        invalidateHoeCache();
        updateGrowthDuration();
    }

    private int effectiveGrowthTicks() {
        return farmerService.effectiveGrowthTicks(
                cachedHoeSpeed,
                cachedHoeTierPosition,
                cachedEfficiencyLevel
        );
    }

    private int updateGrowthDuration() {
        refreshHoeCache();
        int currentDuration = effectiveGrowthTicks();
        if (growthDurationTicks != currentDuration) {
            rescaleProgress(growthDurationTicks, currentDuration);
            growthDurationTicks = currentDuration;
        }
        return currentDuration;
    }

    private void rescaleProgress(int previousMaximum, int newMaximum) {
        if (growthTicks <= 0) {
            return;
        }
        growthTicks = farmerService.rescaleProgress(
                growthTicks,
                previousMaximum,
                newMaximum
        );
    }

    private void setCultivating(boolean value) {
        if (cultivating == value) {
            return;
        }
        cultivating = value;
        markChangedAndSync();
    }

    private boolean canStoreStacks(List<ItemStack> stacks) {
        return OrderedOutputInserter.canInsertAll(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                stacks
        );
    }

    private void storeHarvest(FarmerHarvest harvest) {
        if (level == null) {
            return;
        }
        for (FarmerYield yield : harvest.yields()) {
                if (yield.isGuaranteed()
                    || yield.succeeds(level.getRandom().nextInt(FarmerYield.CHANCE_SCALE))) {
                OrderedOutputInserter.insert(
                        items,
                        FIRST_OUTPUT_SLOT,
                        OUTPUT_SLOT_COUNT,
                        FarmerCropStackAdapter.output(yield)
                );
            }
        }
    }

    private void storeStacks(List<ItemStack> stacks) {
        OrderedOutputInserter.insertAllValidated(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                stacks
        );
    }

    private void clearPendingDynamicHarvest() {
        pendingDynamicHarvest = List.of();
        pendingDynamicHarvestReady = false;
    }

    private boolean isAdultWorkerUncached(ItemStack stack) {
        CapturedMobKind capturedKind = capturedKind();
        return CapturedMobStackAdapter.isFilledCapturer(capturedKind, stack)
                && !CapturedMobStackAdapter.isBaby(capturedKind, stack);
    }

    private void refreshRuntimeCaches() {
        refreshWorkerCache();
        refreshCropCache();
        refreshHoeCache();
        refreshHarvestCache();
    }

    private void refreshWorkerCache() {
        ItemStack worker = items.get(WORKER_SLOT);
        if (workerCacheInitialized && ItemStack.isSameItemSameComponents(cachedWorker, worker)) {
            return;
        }
        cachedWorker = worker.copy();
        cachedAdultWorker = isAdultWorkerUncached(worker);
        workerCacheInitialized = true;
    }

    private void refreshCropCache() {
        ItemStack cropStack = items.get(CROP_SLOT);
        if (cropCacheInitialized && ItemStack.isSameItemSameComponents(cachedCropStack, cropStack)) {
            return;
        }
        cachedCropStack = cropStack.copy();
        cachedCrop = FarmerCropStackAdapter.from(kind, cropStack);
        cachedDynamicCrop = cachedCrop == FarmerCrop.NONE
                && kind == FarmerKind.VILLAGER
                && FarmerCropStackAdapter.isDynamicVillagerCrop(cropStack);
        cropCacheInitialized = true;
    }

    private void refreshHoeCache() {
        ItemStack hoe = items.get(HOE_SLOT);
        if (hoeCacheInitialized && ItemStack.isSameItemSameComponents(cachedHoe, hoe)) {
            return;
        }
        cachedHoe = hoe.copy();
        cachedHoeSpeed = HoeTierCatalog.miningSpeed(hoe);
        cachedHoeTierPosition = HoeTierCatalog.timingPosition(hoe);
        cachedEfficiencyLevel = enchantmentLevel(hoe, Enchantments.EFFICIENCY);
        cachedFortuneLevel = enchantmentLevel(hoe, Enchantments.FORTUNE);
        hoeCacheInitialized = true;
    }

    private int enchantmentLevel(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        if (stack.isEmpty() || level == null) {
            return 0;
        }
        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return Math.max(0, stack.getEnchantmentLevel(enchantment));
    }

    private void refreshHarvestCache() {
        if (cachedHarvestCrop == cachedCrop && cachedHarvestFortune == cachedFortuneLevel) {
            return;
        }
        cachedHarvest = farmerService.harvest(cachedCrop, cachedFortuneLevel);
        cachedMaximumHarvest = cachedHarvest.yields().stream()
                .map(FarmerCropStackAdapter::output)
                .toList();
        cachedHarvestCrop = cachedCrop;
        cachedHarvestFortune = cachedFortuneLevel;
    }

    private void invalidateCache(int slot) {
        activity.wake();
        switch (slot) {
            case WORKER_SLOT -> workerCacheInitialized = false;
            case CROP_SLOT -> cropCacheInitialized = false;
            case HOE_SLOT -> invalidateHoeCache();
            default -> {
                // Output slots do not affect the input-derived caches.
            }
        }
    }

    private void invalidateHoeCache() {
        hoeCacheInitialized = false;
        cachedHarvestFortune = Integer.MIN_VALUE;
    }

    private void invalidateRuntimeCaches() {
        workerCacheInitialized = false;
        cropCacheInitialized = false;
        invalidateHoeCache();
        cachedHarvestCrop = FarmerCrop.NONE;
        cachedMaximumHarvest = List.of();
    }

    private CapturedMobKind capturedKind() {
        return kind == FarmerKind.VILLAGER ? CapturedMobKind.VILLAGER : CapturedMobKind.PIGLIN;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < CONTAINER_SIZE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
