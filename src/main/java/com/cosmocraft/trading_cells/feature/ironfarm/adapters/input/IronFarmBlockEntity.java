package com.cosmocraft.trading_cells.feature.ironfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.ironfarm.application.port.input.IronFarmUseCase;
import com.cosmocraft.trading_cells.feature.ironfarm.domain.model.IronFarmCycle;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.machine.OrderedOutputInserter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class IronFarmBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int FIRST_VILLAGER_SLOT = 0;
    public static final int VILLAGER_SLOT_COUNT = 3;
    public static final int FIRST_OUTPUT_SLOT = FIRST_VILLAGER_SLOT + VILLAGER_SLOT_COUNT;
    public static final int OUTPUT_SLOT_COUNT = 4;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String CYCLE_TICKS_TAG = "CycleTicks";
    private static final String FLOWERS_ENABLED_TAG = "FlowersEnabled";
    private static final int[] VILLAGER_SLOTS = new int[]{0, 1, 2};
    private static final int[] OUTPUT_SLOTS = new int[]{3, 4, 5, 6};
    private static final Identifier NITWIT_PROFESSION = Identifier.withDefaultNamespace("nitwit");

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final IronFarmUseCase ironFarmService = FeatureComposition.ironFarm();
    private final MachineActivityController activity = new MachineActivityController();
    private int cycleTicks;
    private boolean flowersEnabled = true;
    private int cachedVillagerCount = -1;
    private int cachedNitwitCount = -1;
    private int cachedOutputMultiplier = -1;
    private int cachedBaseIron = -1;
    private int cachedMaximumPoppies = -1;
    private boolean cachedFlowersEnabled;
    private ItemStack cachedIron = ItemStack.EMPTY;
    private List<ItemStack> cachedMaximumOutputs = List.of();

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cycleTicks;
                case 1 -> flowersEnabled ? 1 : 0;
                case 2 -> villagerCount();
                case 3 -> ironFarmService.cycle().cycleTicks();
                case 4 -> productionMultiplier(ironFarmService.cycle());
                case 5 -> nextProductionMultiplier(ironFarmService.cycle());
                case 6 -> VILLAGER_SLOT_COUNT;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            activity.wake();
            if (index == 0) {
                cycleTicks = Math.clamp(value, 0, ironFarmService.cycle().cycleTicks());
            } else if (index == 1) {
                flowersEnabled = value != 0;
                markChangedAndSync();
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public IronFarmBlockEntity(BlockPos pos, BlockState state) {
        super(IronFarmRegistrationAdapter.IRON_FARM_BLOCK_ENTITY.get(), pos, state);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public int cycleTicks() {
        return cycleTicks;
    }

    public boolean isGolemVisible() {
        return ironFarmService.cycle().isGolemVisible(cycleTicks);
    }

    public boolean hasGolemRedHitFlash() {
        return ironFarmService.cycle().hasRedHitFlash(cycleTicks);
    }

    @Override
    public void processTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (activity.remainsInactive() || activity.remainsBlocked()) {
            return;
        }
        IronFarmCycle cycle = ironFarmService.cycle();
        int villagerCount = villagerCount();
        int multiplier = productionMultiplier(cycle);
        refreshMaximumOutputs(multiplier);
        boolean outputAvailable = multiplier > 0 && OrderedOutputInserter.canInsertAll(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                cachedMaximumOutputs
        );
        int previousTicks = cycleTicks;
        TimedProcess.Step step = ironFarmService.advance(
                cycleTicks,
                villagerCount,
                outputAvailable,
                cycle
        );
        cycleTicks = step.ticks();
        applyCycleTransition(step.transition(), previousTicks, cycle, cachedIron, multiplier);
        activity.transition(multiplier <= 0 && cycleTicks == 0
                ? MachineActivityController.Activity.INACTIVE
                : outputAvailable
                        ? MachineActivityController.Activity.ACTIVE
                        : MachineActivityController.Activity.BLOCKED);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.iron_farm");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NonNull Inventory inventory, @NonNull Player player) {
        return new IronFarmMenu(containerId, inventory, this, dataAccess);
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
        ItemStack removed = items.get(slot).split(count);
        if (items.get(slot).isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        invalidateVillagerCount(slot);
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
        invalidateVillagerCount(slot);
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot) || !stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        inserted.setCount(Math.min(1, inserted.getCount()));
        items.set(slot, inserted);
        invalidateVillagerCount(slot);
        markChangedAndSync();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        return isVillagerSlot(slot) && isAdultVillager(stack);
    }

    @Override
    public void clearContent() {
        clearContentsForBlockDrop();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        return direction == Direction.DOWN ? OUTPUT_SLOTS : VILLAGER_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NonNull ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && canPlaceItem(slot, stack);
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
        cycleTicks = Math.clamp(
                input.getIntOr(CYCLE_TICKS_TAG, 0),
                0,
                ironFarmService.cycle().cycleTicks()
        );
        flowersEnabled = input.getBooleanOr(FLOWERS_ENABLED_TAG, true);
        invalidateRuntimeCaches();
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        if (cycleTicks > 0) {
            output.putInt(CYCLE_TICKS_TAG, cycleTicks);
        }
        output.putBoolean(FLOWERS_ENABLED_TAG, flowersEnabled);
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        cycleTicks = 0;
        invalidateRuntimeCaches();
        setChanged();
    }

    private int villagerCount() {
        refreshVillagerCounts();
        return cachedVillagerCount;
    }

    private int nitwitCount() {
        refreshVillagerCounts();
        return cachedNitwitCount;
    }

    private void refreshVillagerCounts() {
        if (cachedVillagerCount >= 0 && cachedNitwitCount >= 0) {
            return;
        }
        int count = 0;
        int nitwits = 0;
        for (int slot : VILLAGER_SLOTS) {
            ItemStack villager = items.get(slot);
            if (isAdultVillager(villager)) {
                count++;
                if (CapturedMobStackAdapter.hasVillagerProfession(villager, NITWIT_PROFESSION)) {
                    nitwits++;
                }
            }
        }
        cachedVillagerCount = count;
        cachedNitwitCount = nitwits;
    }

    private int productionMultiplier(IronFarmCycle cycle) {
        return cycle.multiplier(villagerCount(), nitwitCount());
    }

    private int nextProductionMultiplier(IronFarmCycle cycle) {
        int villagers = villagerCount();
        if (villagers >= VILLAGER_SLOT_COUNT) {
            return productionMultiplier(cycle);
        }
        return cycle.multiplier(villagers + 1, nitwitCount());
    }

    private void applyCycleTransition(
            TimedProcess.Transition transition,
            int previousTicks,
            IronFarmCycle cycle,
            ItemStack iron,
            int multiplier
    ) {
        switch (transition) {
            case IDLE, PAUSED -> {
                // The process state is already up to date.
            }
            case RESET -> {
                if (previousTicks != 0) {
                    markChangedAndSync();
                }
            }
            case ADVANCED -> processAdvancedTick(cycle);
            case COMPLETED -> completeCycle(iron, multiplier);
        }
    }

    private void processAdvancedTick(IronFarmCycle cycle) {
        if (cycle.isGolemHitTick(cycleTicks)) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.IRON_GOLEM_HURT,
                    SoundSource.BLOCKS,
                    0.9F,
                    0.9F
            );
        }
        setChanged();
        if (cycleTicks % 20 == 0
                || cycle.isGolemHitTick(cycleTicks)
                || cycle.isRedHitFlashEnding(cycleTicks)) {
            markChangedAndSync();
        }
    }

    private void completeCycle(ItemStack iron, int multiplier) {
        level.playSound(
                null,
                worldPosition,
                SoundEvents.IRON_GOLEM_DEATH,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        storeOutput(iron);
        if (flowersEnabled) {
            int poppies = level.getRandom().nextInt(ironFarmService.maximumPoppies() + 1) * multiplier;
            storeOutput(new ItemStack(Items.POPPY, poppies));
        }
        markChangedAndSync();
    }

    private void storeOutput(ItemStack source) {
        OrderedOutputInserter.insert(items, FIRST_OUTPUT_SLOT, OUTPUT_SLOT_COUNT, source);
    }

    private void refreshMaximumOutputs(int multiplier) {
        int baseIron = ironFarmService.baseIron();
        int maximumPoppies = ironFarmService.maximumPoppies();
        if (cachedOutputMultiplier == multiplier
                && cachedBaseIron == baseIron
                && cachedMaximumPoppies == maximumPoppies
                && cachedFlowersEnabled == flowersEnabled) {
            return;
        }
        cachedIron = new ItemStack(Items.IRON_INGOT, baseIron * multiplier);
        ItemStack poppies = flowersEnabled
                ? new ItemStack(Items.POPPY, maximumPoppies * multiplier)
                : ItemStack.EMPTY;
        cachedMaximumOutputs = poppies.isEmpty()
                ? List.of(cachedIron)
                : List.of(cachedIron, poppies);
        cachedOutputMultiplier = multiplier;
        cachedBaseIron = baseIron;
        cachedMaximumPoppies = maximumPoppies;
        cachedFlowersEnabled = flowersEnabled;
    }

    private void invalidateVillagerCount(int slot) {
        activity.wake();
        if (isVillagerSlot(slot)) {
            cachedVillagerCount = -1;
            cachedNitwitCount = -1;
            cachedOutputMultiplier = -1;
        }
    }

    private void invalidateRuntimeCaches() {
        cachedVillagerCount = -1;
        cachedNitwitCount = -1;
        cachedOutputMultiplier = -1;
        cachedBaseIron = -1;
        cachedMaximumPoppies = -1;
        cachedIron = ItemStack.EMPTY;
        cachedMaximumOutputs = List.of();
        activity.reset();
    }

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static boolean isVillagerSlot(int slot) {
        return slot >= FIRST_VILLAGER_SLOT && slot < FIRST_OUTPUT_SLOT;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < CONTAINER_SIZE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
