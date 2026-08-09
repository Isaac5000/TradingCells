package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.EnhancedPiglinBarterRewards;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.application.port.input.PiglinBarterUseCase;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.machine.OrderedOutputInserter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Sequential piglin bartering machine with one progression upgrade slot. */
public final class NetheritePiglinBarteringCellBlockEntity extends PortableMachineBlockEntity
        implements WorldlyContainer, MenuProvider {
    private static final String PIGLIN_DATA_TAG = "StoredPiglin";
    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String PENDING_REWARD_TAG = "PendingReward";
    private static final String BARTER_TICKS_TAG = "BarterTicksRemaining";
    private static final String NEXT_GOLD_SLOT_TAG = "NextGoldSlot";
    private static final String LAYOUT_VERSION_TAG = "LayoutVersion";
    private static final int CURRENT_LAYOUT_VERSION = 4;

    /** Keys used by the first four-lane implementation, retained for world migration. */
    private static final String LEGACY_PENDING_TAG_PREFIX = "PendingReward";
    private static final String LEGACY_BARTER_TICKS_TAG_PREFIX = "BarterTicks";

    public static final int UPGRADE_SLOT = 0;
    public static final int FILTER_SLOT = 1;
    public static final int FIRST_GOLD_SLOT = 2;
    public static final int GOLD_SLOT_COUNT = 4;
    public static final int FIRST_OUTPUT_SLOT = FIRST_GOLD_SLOT + GOLD_SLOT_COUNT;
    public static final int OUTPUT_SLOT_COUNT = 8;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;

    private static final int[] INPUT_SLOTS = new int[]{2, 3, 4, 5};
    private static final int[] OUTPUT_SLOTS = IntStream.range(FIRST_OUTPUT_SLOT, CONTAINER_SIZE).toArray();
    private static final int[] NO_SLOTS = new int[0];

    private final PiglinBarterUseCase barterService = FeatureComposition.piglinBarter();
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final MachineActivityController activity = new MachineActivityController();
    private ItemStack pendingReward = ItemStack.EMPTY;
    private int barterTicksRemaining;
    private int nextGoldSlotOffset;
    private @Nullable CompoundTag storedPiglinData;

    public NetheritePiglinBarteringCellBlockEntity(BlockPos pos, BlockState state) {
        super(TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void processTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (activity.remainsInactive() || activity.remainsBlocked()) {
            return;
        }

        if (barterTicksRemaining > 0) {
            advanceCurrentBarter();
            updateActivity();
            return;
        }

        if (!pendingReward.isEmpty()) {
            if (tryStorePendingReward()) {
                markChangedAndSync();
            }
            updateActivity();
            return;
        }

        tryStartNextBarter(serverLevel);
        updateActivity();
    }

    private void advanceCurrentBarter() {
        PiglinBarterCycle.Step step = barterService.advance(barterTicksRemaining, false);
        barterTicksRemaining = step.ticksRemaining();
        if (step.transition() == PiglinBarterCycle.Transition.COMPLETED) {
            barterTicksRemaining = 0;
            tryStorePendingReward();
            markChangedAndSync();
        } else {
            setChanged();
        }
    }

    private void tryStartNextBarter(ServerLevel serverLevel) {
        if (!hasAdultPiglin()) {
            return;
        }

        int goldSlot = findNextGoldSlot();
        if (goldSlot < 0) {
            return;
        }

        Piglin piglin = createPiglin(serverLevel);
        if (piglin == null) {
            return;
        }

        ItemStack activeFilter = hasNetheriteUpgrade() ? items.get(FILTER_SLOT) : ItemStack.EMPTY;
        ItemStack reward = EnhancedPiglinBarterRewards.roll(
                serverLevel,
                piglin,
                activeFilter,
                progressionLevel()
        );
        if (reward.isEmpty()) {
            return;
        }

        ItemStack gold = items.get(goldSlot);
        gold.shrink(1);
        if (gold.isEmpty()) {
            items.set(goldSlot, ItemStack.EMPTY);
        }

        pendingReward = reward.copy();
        nextGoldSlotOffset = Math.floorMod(goldSlot - FIRST_GOLD_SLOT + 1, GOLD_SLOT_COUNT);
        PiglinBarterCycle.Step step = barterService.advance(0, true);
        barterTicksRemaining = step.ticksRemaining();
        markChangedAndSync();
    }

    /**
     * Selects the first gold slot at or after the rotating cursor. Empty slots
     * are skipped, so the four inputs behave as one sequential buffer.
     */
    private int findNextGoldSlot() {
        for (int step = 0; step < GOLD_SLOT_COUNT; step++) {
            int offset = (nextGoldSlotOffset + step) % GOLD_SLOT_COUNT;
            int slot = FIRST_GOLD_SLOT + offset;
            ItemStack gold = items.get(slot);
            if (!gold.isEmpty() && gold.is(Items.GOLD_INGOT)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Inserts the entire completed reward or nothing. If the eight outputs do
     * not have enough compatible capacity, the reward remains pending and the
     * machine pauses until space becomes available.
     */
    private boolean tryStorePendingReward() {
        if (pendingReward.isEmpty()) {
            return true;
        }
        if (!OrderedOutputInserter.canInsert(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                pendingReward
        )) {
            return false;
        }
        if (!OrderedOutputInserter.insert(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                pendingReward
        )) {
            return false;
        }

        pendingReward = ItemStack.EMPTY;
        return true;
    }

    private int progressionLevel() {
        ItemStack upgrade = items.get(UPGRADE_SLOT);
        if (upgrade.is(TraderRegistrationAdapter.PIGLIN_BARTER_COPPER_UPGRADE_ITEM.get())) {
            return 1;
        }
        if (upgrade.is(TraderRegistrationAdapter.PIGLIN_BARTER_IRON_UPGRADE_ITEM.get())) {
            return 2;
        }
        if (upgrade.is(TraderRegistrationAdapter.PIGLIN_BARTER_GOLD_UPGRADE_ITEM.get())) {
            return 3;
        }
        if (upgrade.is(TraderRegistrationAdapter.PIGLIN_BARTER_DIAMOND_UPGRADE_ITEM.get())) {
            return 4;
        }
        if (upgrade.is(TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get())) {
            return EnhancedPiglinBarterRewards.NETHERITE_UPGRADE_LEVEL;
        }
        return 0;
    }

    private boolean hasNetheriteUpgrade() {
        return items.get(UPGRADE_SLOT)
                .is(TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get());
    }

    public boolean hasPiglin() {
        return storedPiglinData != null && !storedPiglinData.isEmpty();
    }

    public boolean isBartering() {
        return barterTicksRemaining > 0;
    }

    private boolean hasAdultPiglin() {
        return hasPiglin()
                && storedPiglinData != null
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, storedPiglinData);
    }

    public @Nullable CompoundTag copyPiglinData() {
        return hasPiglin() && storedPiglinData != null ? storedPiglinData.copy() : null;
    }

    /** Returns the blocked reward first so it is rendered inside the cage. */
    public @NonNull ItemStack copyFirstOutputStack() {
        if (!isBartering() && !pendingReward.isEmpty()) {
            return pendingReward.copy();
        }
        for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT; slot++) {
            if (!items.get(slot).isEmpty()) {
                return items.get(slot).copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public @Nullable Piglin createPiglinForDisplay() {
        if (level == null || storedPiglinData == null) {
            return null;
        }
        Piglin piglin = CapturedMobStackAdapter.createPiglin(level, storedPiglinData, worldPosition);
        if (piglin != null && isBartering()) {
            piglin.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLD_INGOT));
        }
        return piglin;
    }

    private @Nullable Piglin createPiglin(ServerLevel serverLevel) {
        if (storedPiglinData == null) {
            return null;
        }
        return CapturedMobStackAdapter.createPiglin(serverLevel, storedPiglinData, worldPosition);
    }

    public InteractionResult insertPiglinFromCapturer(ItemStack stack) {
        if (hasPiglin()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        CompoundTag piglinData = CapturedMobStackAdapter.copyData(CapturedMobKind.PIGLIN, stack);
        if (piglinData == null || CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, piglinData)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        storedPiglinData = piglinData.copy();
        activity.wake();
        CapturedMobStackAdapter.clearData(CapturedMobKind.PIGLIN, stack);
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractPiglinToCapturer(ItemStack stack, Player player) {
        if (!hasPiglin() || isBartering() || CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.PIGLIN, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        CompoundTag piglinData = copyPiglinData();
        if (piglinData == null) {
            return InteractionResult.FAIL;
        }
        ItemStack target = stack.getCount() <= 1 ? stack : new ItemStack(stack.getItem());
        CapturedMobStackAdapter.setData(CapturedMobKind.PIGLIN, target, piglinData);
        if (target != stack) {
            stack.shrink(1);
            if (!player.getInventory().add(target)) {
                player.drop(target, false);
            }
        }
        storedPiglinData = null;
        activity.wake();
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.netherite_piglin_bartering_cell");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new NetheritePiglinBarteringCellMenu(containerId, inventory, this);
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
        activity.wake();
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
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        ItemStack inserted = stack.copy();
        inserted.setCount(Math.min(slotLimit(slot, inserted), inserted.getCount()));
        items.set(slot, inserted);
        activity.wake();
        markChangedAndSync();
    }

    private static int slotLimit(int slot, ItemStack stack) {
        if (slot == UPGRADE_SLOT) {
            return 1;
        }
        if (slot == FILTER_SLOT) {
            return 1;
        }
        return Math.min(64, stack.getMaxStackSize());
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        if (slot == UPGRADE_SLOT) {
            return isProgressionUpgrade(stack);
        }
        if (slot == FILTER_SLOT) {
            return hasNetheriteUpgrade();
        }
        return isGoldSlot(slot) && stack.is(Items.GOLD_INGOT);
    }

    private static boolean isProgressionUpgrade(ItemStack stack) {
        return stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_COPPER_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_IRON_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_GOLD_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_DIAMOND_UPGRADE_ITEM.get())
                || stack.is(TraderRegistrationAdapter.PIGLIN_BARTER_NETHERITE_UPGRADE_ITEM.get());
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        pendingReward = ItemStack.EMPTY;
        barterTicksRemaining = 0;
        nextGoldSlotOffset = 0;
        activity.reset();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        if (direction == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        if (direction == Direction.UP || direction.getAxis().isHorizontal()) {
            return INPUT_SLOTS;
        }
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NonNull ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && isGoldSlot(slot) && stack.is(Items.GOLD_INGOT);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && isOutputSlot(slot);
    }

    private static boolean isGoldSlot(int slot) {
        return slot >= FIRST_GOLD_SLOT && slot < FIRST_GOLD_SLOT + GOLD_SLOT_COUNT;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        storedPiglinData = input.read(PIGLIN_DATA_TAG, CompoundTag.CODEC).orElse(null);
        if (storedPiglinData != null && storedPiglinData.isEmpty()) {
            storedPiglinData = null;
        }

        int layoutVersion = input.getIntOr(LAYOUT_VERSION_TAG, 1);
        if (layoutVersion >= CURRENT_LAYOUT_VERSION) {
            loadCurrentLayout(input);
        } else if (layoutVersion == 3) {
            loadVersionThreeLayout(input);
        } else if (layoutVersion == 2) {
            loadVersionTwoLayout(input);
        } else {
            loadLegacyLayout(input);
        }

        pendingReward = input.read(PENDING_REWARD_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        barterTicksRemaining = Math.max(0, input.getIntOr(BARTER_TICKS_TAG, 0));
        nextGoldSlotOffset = Math.floorMod(input.getIntOr(NEXT_GOLD_SLOT_TAG, 0), GOLD_SLOT_COUNT);
        migrateLegacyLaneState(input);

        if (pendingReward.isEmpty()) {
            barterTicksRemaining = 0;
        }
        activity.reset();
    }

    private void loadCurrentLayout(ValueInput input) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            ItemStack stack = input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty() && canPlaceSavedItem(slot, stack)) {
                stack.setCount(Math.min(slotLimit(slot, stack), stack.getCount()));
                items.set(slot, stack);
            } else {
                items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    private void loadVersionThreeLayout(ValueInput input) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }

        ItemStack copperUpgrade = input.read(SLOT_TAG_PREFIX + 0, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack tierUpgrade = input.read(SLOT_TAG_PREFIX + 1, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack filter = input.read(SLOT_TAG_PREFIX + 2, ItemStack.CODEC).orElse(ItemStack.EMPTY);

        ItemStack upgrade = !tierUpgrade.isEmpty() && isProgressionUpgrade(tierUpgrade)
                ? tierUpgrade
                : copperUpgrade;
        if (!upgrade.isEmpty() && isProgressionUpgrade(upgrade)) {
            upgrade.setCount(1);
            items.set(UPGRADE_SLOT, upgrade);
        }
        if (!filter.isEmpty() && !items.get(UPGRADE_SLOT).isEmpty()) {
            filter.setCount(1);
            items.set(FILTER_SLOT, filter);
        }

        for (int lane = 0; lane < GOLD_SLOT_COUNT; lane++) {
            ItemStack gold = input.read(SLOT_TAG_PREFIX + (3 + lane), ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!gold.isEmpty() && gold.is(Items.GOLD_INGOT)) {
                items.set(FIRST_GOLD_SLOT + lane, gold);
            }
        }
        for (int lane = 0; lane < OUTPUT_SLOT_COUNT; lane++) {
            ItemStack output = input.read(SLOT_TAG_PREFIX + (7 + lane), ItemStack.CODEC).orElse(ItemStack.EMPTY);
            items.set(FIRST_OUTPUT_SLOT + lane, output);
        }
    }

    private void loadVersionTwoLayout(ValueInput input) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }

        ItemStack oldUpgrade = input.read(SLOT_TAG_PREFIX + 0, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack oldFilter = input.read(SLOT_TAG_PREFIX + 1, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack migratedTier = migrateLegacyUpgrade(oldUpgrade, ItemStack.EMPTY);
        if (!migratedTier.isEmpty()) {
            items.set(UPGRADE_SLOT, migratedTier);
        } else if (!oldFilter.isEmpty()) {
            items.set(UPGRADE_SLOT, new ItemStack(
                    TraderRegistrationAdapter.PIGLIN_BARTER_COPPER_UPGRADE_ITEM.get()
            ));
        }
        if (!oldFilter.isEmpty() && !items.get(UPGRADE_SLOT).isEmpty()) {
            oldFilter.setCount(1);
            items.set(FILTER_SLOT, oldFilter);
        }

        for (int lane = 0; lane < GOLD_SLOT_COUNT; lane++) {
            ItemStack gold = input.read(SLOT_TAG_PREFIX + (2 + lane), ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!gold.isEmpty() && gold.is(Items.GOLD_INGOT)) {
                items.set(FIRST_GOLD_SLOT + lane, gold);
            }
        }
        for (int lane = 0; lane < OUTPUT_SLOT_COUNT; lane++) {
            ItemStack output = input.read(SLOT_TAG_PREFIX + (6 + lane), ItemStack.CODEC).orElse(ItemStack.EMPTY);
            items.set(FIRST_OUTPUT_SLOT + lane, output);
        }
    }

    private void loadLegacyLayout(ValueInput input) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }

        ItemStack quality = input.read(SLOT_TAG_PREFIX + 0, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack yield = input.read(SLOT_TAG_PREFIX + 1, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack filter = input.read(SLOT_TAG_PREFIX + 2, ItemStack.CODEC).orElse(ItemStack.EMPTY);

        ItemStack migratedUpgrade = migrateLegacyUpgrade(quality, yield);
        if (!migratedUpgrade.isEmpty()) {
            items.set(UPGRADE_SLOT, migratedUpgrade);
        } else if (!filter.isEmpty()) {
            items.set(UPGRADE_SLOT, new ItemStack(
                    TraderRegistrationAdapter.PIGLIN_BARTER_COPPER_UPGRADE_ITEM.get()
            ));
        }
        if (!filter.isEmpty() && !items.get(UPGRADE_SLOT).isEmpty()) {
            filter.setCount(1);
            items.set(FILTER_SLOT, filter);
        }

        for (int lane = 0; lane < GOLD_SLOT_COUNT; lane++) {
            ItemStack gold = input.read(SLOT_TAG_PREFIX + (3 + lane), ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!gold.isEmpty() && gold.is(Items.GOLD_INGOT)) {
                gold.setCount(Math.min(slotLimit(FIRST_GOLD_SLOT + lane, gold), gold.getCount()));
                items.set(FIRST_GOLD_SLOT + lane, gold);
            }
        }
        for (int lane = 0; lane < OUTPUT_SLOT_COUNT; lane++) {
            ItemStack output = input.read(SLOT_TAG_PREFIX + (7 + lane), ItemStack.CODEC).orElse(ItemStack.EMPTY);
            items.set(FIRST_OUTPUT_SLOT + lane, output);
        }
    }

    private static ItemStack migrateLegacyUpgrade(ItemStack first, ItemStack second) {
        int legacyLevel = 0;
        if (first.is(TraderRegistrationAdapter.PIGLIN_BARTER_QUALITY_UPGRADE_ITEM.get())
                || first.is(TraderRegistrationAdapter.PIGLIN_BARTER_YIELD_UPGRADE_ITEM.get())
                || first.is(TraderRegistrationAdapter.PIGLIN_BARTER_HYBRID_UPGRADE_ITEM.get())) {
            legacyLevel = Math.max(legacyLevel, first.getCount());
        }
        if (second.is(TraderRegistrationAdapter.PIGLIN_BARTER_QUALITY_UPGRADE_ITEM.get())
                || second.is(TraderRegistrationAdapter.PIGLIN_BARTER_YIELD_UPGRADE_ITEM.get())
                || second.is(TraderRegistrationAdapter.PIGLIN_BARTER_HYBRID_UPGRADE_ITEM.get())) {
            legacyLevel = Math.max(legacyLevel, second.getCount());
        }
        return switch (Math.clamp(legacyLevel, 0, 3)) {
            case 1 -> new ItemStack(TraderRegistrationAdapter.PIGLIN_BARTER_IRON_UPGRADE_ITEM.get());
            case 2 -> new ItemStack(TraderRegistrationAdapter.PIGLIN_BARTER_GOLD_UPGRADE_ITEM.get());
            case 3 -> new ItemStack(TraderRegistrationAdapter.PIGLIN_BARTER_DIAMOND_UPGRADE_ITEM.get());
            default -> ItemStack.EMPTY;
        };
    }

    private void migrateLegacyLaneState(ValueInput input) {
        if (!pendingReward.isEmpty()) {
            return;
        }
        for (int lane = 0; lane < GOLD_SLOT_COUNT; lane++) {
            ItemStack legacyReward = input.read(
                    LEGACY_PENDING_TAG_PREFIX + lane,
                    ItemStack.CODEC
            ).orElse(ItemStack.EMPTY);
            if (legacyReward.isEmpty()) {
                continue;
            }
            pendingReward = legacyReward;
            barterTicksRemaining = Math.max(
                    barterTicksRemaining,
                    input.getIntOr(LEGACY_BARTER_TICKS_TAG_PREFIX + lane, 0)
            );
            nextGoldSlotOffset = (lane + 1) % GOLD_SLOT_COUNT;
            return;
        }
    }

    private static boolean canPlaceSavedItem(int slot, ItemStack stack) {
        if (isOutputSlot(slot)) {
            return true;
        }
        if (slot == FILTER_SLOT) {
            return true;
        }
        if (slot == UPGRADE_SLOT) {
            return isProgressionUpgrade(stack);
        }
        return isGoldSlot(slot) && stack.is(Items.GOLD_INGOT);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(LAYOUT_VERSION_TAG, CURRENT_LAYOUT_VERSION);
        if (storedPiglinData != null && !storedPiglinData.isEmpty()) {
            output.store(PIGLIN_DATA_TAG, CompoundTag.CODEC, storedPiglinData);
        }
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        if (!pendingReward.isEmpty()) {
            output.store(PENDING_REWARD_TAG, ItemStack.CODEC, pendingReward);
        }
        if (barterTicksRemaining > 0) {
            output.putInt(BARTER_TICKS_TAG, barterTicksRemaining);
        }
        if (nextGoldSlotOffset != 0) {
            output.putInt(NEXT_GOLD_SLOT_TAG, nextGoldSlotOffset);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        storedPiglinData = null;
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        pendingReward = ItemStack.EMPTY;
        barterTicksRemaining = 0;
        nextGoldSlotOffset = 0;
        activity.reset();
        setChanged();
    }

    private void updateActivity() {
        if (barterTicksRemaining > 0) {
            activity.transition(MachineActivityController.Activity.ACTIVE);
        } else if (!pendingReward.isEmpty()) {
            activity.transition(MachineActivityController.Activity.BLOCKED);
        } else if (!hasAdultPiglin() || findNextGoldSlot() < 0) {
            activity.transition(MachineActivityController.Activity.INACTIVE);
        } else {
            activity.transition(MachineActivityController.Activity.ACTIVE);
        }
    }
}
