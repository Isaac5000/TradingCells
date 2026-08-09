package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.quarry.application.port.input.QuarryUseCase;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.machine.OrderedOutputInserter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class QuarryBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int WORKER_SLOT = 0;
    public static final int PICKAXE_SLOT = 1;
    public static final int UPGRADE_SLOT = 2;
    public static final int FIRST_OUTPUT_SLOT = 3;
    public static final int OUTPUT_SLOT_COUNT = 18;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;
    public static final int QUARRY_DATA_VERSION = 1;

    private static final String DATA_VERSION_TAG = "QuarryDataVersion";
    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String CYCLE_TICKS_TAG = "CycleTicks";
    private static final String CYCLE_DURATION_TAG = "CycleDurationTicks";
    private static final String DEEP_MINING_TAG = "DeepMining";
    private static final String LAST_RESULT_TAG = "LastResult";
    private static final String PENDING_COUNT_TAG = "PendingResultCount";
    private static final String PENDING_SLOT_PREFIX = "PendingResult";
    private static final String PENDING_MATERIAL_TAG = "PendingMaterial";
    private static final String PENDING_PREVIEW_TAG = "PendingPreview";
    private static final int[] INPUT_SLOTS = {WORKER_SLOT, PICKAXE_SLOT, UPGRADE_SLOT};
    private static final int[] OUTPUT_SLOTS = IntStream.range(FIRST_OUTPUT_SLOT, CONTAINER_SIZE).toArray();
    private static final int[] NO_SLOTS = new int[0];
    private static final String NITWIT_PROFESSION = "minecraft:nitwit";

    private final QuarryKind kind;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final QuarryUseCase quarryService = FeatureComposition.quarry();
    private final MachineActivityController activity = new MachineActivityController();
    private int cycleTicks;
    private int cycleDurationTicks = quarryService.durationTicks(0.0D, 0);
    private boolean deepMining;
    private boolean mining;
    private QuarryStatus status = QuarryStatus.STOPPED;
    private ItemStack lastResult = ItemStack.EMPTY;
    private List<ItemStack> pendingResult = List.of();
    private @Nullable String pendingMaterial;
    private ItemStack pendingPreview = ItemStack.EMPTY;
    private QuarryMaterialCatalog.@Nullable CatalogSnapshot cachedCatalog;
    private ItemStack cachedCatalogPickaxe = ItemStack.EMPTY;
    private ItemStack cachedCatalogUpgrade = ItemStack.EMPTY;
    private boolean cachedCatalogDeepMining;
    private int cachedCatalogRevision = -1;
    private ItemStack cachedWorker = ItemStack.EMPTY;
    private boolean cachedWorkerValid;
    private boolean workerCacheInitialized;
    private ItemStack cachedDurationPickaxe = ItemStack.EMPTY;
    private boolean durationCacheInitialized;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cycleTicks;
                case 1 -> cycleDurationTicks;
                case 2 -> mining ? 1 : 0;
                case 3 -> deepMining ? 1 : 0;
                case 4 -> deepMiningAvailable() ? 1 : 0;
                case 5 -> status.ordinal();
                case 6 -> lastResult.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(lastResult.getItem());
                case 7 -> QuarryMaterialCatalog.revision();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                cycleTicks = Math.clamp(value, 0, Math.max(0, cycleDurationTicks - 1));
            } else if (index == 3 && kind == QuarryKind.VILLAGER) {
                setDeepMining(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    protected QuarryBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            QuarryKind kind
    ) {
        super(type, pos, state);
        this.kind = kind;
    }

    public QuarryKind kind() {
        return kind;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public ItemStack lastResult() {
        return lastResult.copy();
    }

    public BlockState currentMaterialState() {
        if (pendingPreview.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    public QuarryMaterialCatalog.CatalogSnapshot catalogSnapshot() {
        ItemStack pickaxe = items.get(PICKAXE_SLOT);
        ItemStack upgrade = items.get(UPGRADE_SLOT);
        int revision = QuarryMaterialCatalog.revision();
        if (cachedCatalog == null
                || cachedCatalogRevision != revision
                || cachedCatalogDeepMining != deepMining
                || !ItemStack.isSameItemSameComponents(cachedCatalogPickaxe, pickaxe)
                || !ItemStack.isSameItemSameComponents(cachedCatalogUpgrade, upgrade)) {
            int fortuneLevel = level == null
                    ? 0
                    : QuarryEnchantments.fortuneLevel(pickaxe, level.registryAccess());
            boolean silkTouch = level != null
                    && QuarryEnchantments.hasSilkTouch(pickaxe, level.registryAccess());
            cachedCatalog = QuarryMaterialCatalog.snapshot(
                    kind,
                    QuarryUpgradeItems.tier(upgrade),
                    pickaxe,
                    deepMining,
                    fortuneLevel,
                    silkTouch
            );
            cachedCatalogPickaxe = pickaxe.copy();
            cachedCatalogUpgrade = upgrade.copy();
            cachedCatalogDeepMining = deepMining;
            cachedCatalogRevision = revision;
        }
        return cachedCatalog;
    }

    @Override
    public void processTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (activity.remainsInactive()) {
            return;
        }
        int duration = updateCycleDuration();
        if (!isValidWorker(items.get(WORKER_SLOT))) {
            pause(QuarryStatus.WORKER_REQUIRED);
            return;
        }
        ItemStack pickaxe = items.get(PICKAXE_SLOT);
        if (!QuarryPickaxeCatalog.isSupported(pickaxe)) {
            pause(pickaxe.isEmpty() ? QuarryStatus.PICKAXE_REQUIRED : QuarryStatus.PICKAXE_BROKEN);
            return;
        }
        if (isBroken(pickaxe)) {
            pause(QuarryStatus.PICKAXE_BROKEN);
            return;
        }
        if (activity.remainsBlocked()) {
            pause(QuarryStatus.INVENTORY_FULL);
            return;
        }

        boolean prepared = false;
        if (pendingResult.isEmpty()) {
            QuarryMaterialCatalog.CatalogSnapshot catalog = catalogSnapshot();
            if (catalog.entries().stream().noneMatch(QuarryMaterialCatalog.CatalogEntry::available)) {
                pause(QuarryStatus.NO_COMPATIBLE_MATERIALS);
                return;
            }
            if (!hasAnyOutputSpace()) {
                pause(QuarryStatus.INVENTORY_FULL);
                return;
            }
            if (!preparePendingResult(serverLevel, catalog, pickaxe)) {
                pause(QuarryStatus.NO_COMPATIBLE_MATERIALS);
                return;
            }
            prepared = true;
        }
        if (!canStoreStacks(pendingResult)) {
            activity.transition(MachineActivityController.Activity.BLOCKED);
            pause(QuarryStatus.INVENTORY_FULL, prepared);
            return;
        }
        activity.transition(MachineActivityController.Activity.ACTIVE);

        boolean stateChanged = setMining(true) | setStatus(QuarryStatus.MINING);
        if (cycleTicks < duration - 1) {
            cycleTicks++;
            setChanged();
            if (prepared || stateChanged) {
                markChangedAndSync();
            }
            return;
        }

        storeStacks(pendingResult);
        lastResult = pendingResult.getFirst().copy();
        lastResult.setCount(1);
        clearPendingResult();
        cycleTicks = 0;
        damagePickaxeAfterCycle(serverLevel);
        markChangedAndSync();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable(kind == QuarryKind.VILLAGER
                ? "container.trading_cells.quarry"
                : "container.trading_cells.piglin_quarry");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new QuarryMenu(kind, containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
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
        if (slot == PICKAXE_SLOT) {
            refreshCycleForPickaxeChange();
            updateCycleDuration();
        }
        if (slot <= UPGRADE_SLOT) {
            invalidateCatalog();
        }
        if (slot == UPGRADE_SLOT) {
            disableDeepMiningIfUnavailable();
        }
        if (slot == WORKER_SLOT) {
            invalidateWorker();
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
        if (slot == PICKAXE_SLOT) {
            refreshCycleForPickaxeChange();
            updateCycleDuration();
        }
        if (slot <= UPGRADE_SLOT) {
            invalidateCatalog();
        }
        if (slot == UPGRADE_SLOT) {
            disableDeepMiningIfUnavailable();
        }
        if (slot == WORKER_SLOT) {
            invalidateWorker();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot) || !stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        int maximum = slot < FIRST_OUTPUT_SLOT ? 1 : Math.min(64, inserted.getMaxStackSize());
        inserted.setCount(Math.min(maximum, inserted.getCount()));
        boolean pickaxeChanged = slot == PICKAXE_SLOT
                && !ItemStack.isSameItemSameComponents(items.get(slot), inserted);
        items.set(slot, inserted);
        activity.wake();
        if (pickaxeChanged) {
            refreshCycleForPickaxeChange();
            updateCycleDuration();
        }
        if (slot <= UPGRADE_SLOT) {
            invalidateCatalog();
        }
        if (slot == UPGRADE_SLOT) {
            disableDeepMiningIfUnavailable();
        }
        if (slot == WORKER_SLOT) {
            invalidateWorker();
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
            case WORKER_SLOT -> isValidWorker(stack);
            case PICKAXE_SLOT -> QuarryPickaxeCatalog.isSupported(stack);
            case UPGRADE_SLOT -> QuarryUpgradeItems.isUpgrade(stack);
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
        if (direction == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        return direction.getAxis().isHorizontal() ? INPUT_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NonNull ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && slot < FIRST_OUTPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && isOutputSlot(slot);
    }

    public boolean toggleDeepMining(Player player) {
        if (kind != QuarryKind.VILLAGER || !stillValid(player) || !deepMiningAvailable()) {
            return false;
        }
        setDeepMining(!deepMining);
        return true;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        int version = input.getIntOr(DATA_VERSION_TAG, 0);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        cycleDurationTicks = Math.max(1, input.getIntOr(CYCLE_DURATION_TAG, effectiveCycleTicks()));
        cycleTicks = Math.clamp(input.getIntOr(CYCLE_TICKS_TAG, 0), 0, Math.max(0, cycleDurationTicks - 1));
        boolean savedDeepMining = input.getBooleanOr(DEEP_MINING_TAG, false);
        deepMining = savedDeepMining && deepMiningAvailable();
        lastResult = input.read(LAST_RESULT_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int pendingCount = Math.clamp(input.getIntOr(PENDING_COUNT_TAG, 0), 0, OUTPUT_SLOT_COUNT);
        List<ItemStack> restoredPending = new ArrayList<>(pendingCount);
        for (int index = 0; index < pendingCount; index++) {
            ItemStack stack = input.read(PENDING_SLOT_PREFIX + index, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                restoredPending.add(stack);
            }
        }
        pendingResult = List.copyOf(restoredPending);
        pendingMaterial = input.getString(PENDING_MATERIAL_TAG).orElse(null);
        pendingPreview = input.read(PENDING_PREVIEW_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (pendingPreview.isEmpty()) {
            pendingPreview = previewForMaterial(pendingMaterial, deepMining);
        }
        mining = false;
        status = QuarryStatus.STOPPED;
        invalidateCatalog();
        invalidateWorker();
        invalidateDuration();
        activity.reset();
        migrate(version);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(DATA_VERSION_TAG, QUARRY_DATA_VERSION);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        output.putInt(CYCLE_TICKS_TAG, cycleTicks);
        output.putInt(CYCLE_DURATION_TAG, cycleDurationTicks);
        output.putBoolean(DEEP_MINING_TAG, deepMining);
        if (!lastResult.isEmpty()) {
            output.store(LAST_RESULT_TAG, ItemStack.CODEC, lastResult);
        }
        if (!pendingResult.isEmpty()) {
            output.putInt(PENDING_COUNT_TAG, pendingResult.size());
            for (int index = 0; index < pendingResult.size(); index++) {
                output.store(PENDING_SLOT_PREFIX + index, ItemStack.CODEC, pendingResult.get(index));
            }
        }
        if (pendingMaterial != null) {
            output.putString(PENDING_MATERIAL_TAG, pendingMaterial);
        }
        if (!pendingPreview.isEmpty()) {
            output.store(PENDING_PREVIEW_TAG, ItemStack.CODEC, pendingPreview);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        cycleTicks = 0;
        cycleDurationTicks = quarryService.durationTicks(0.0D, 0);
        deepMining = false;
        mining = false;
        status = QuarryStatus.STOPPED;
        lastResult = ItemStack.EMPTY;
        clearPendingResult();
        invalidateCatalog();
        invalidateWorker();
        invalidateDuration();
        activity.reset();
        setChanged();
    }

    public static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < CONTAINER_SIZE;
    }

    public static boolean isValidWorker(QuarryKind kind, ItemStack stack) {
        CapturedMobKind capturedKind = kind == QuarryKind.VILLAGER
                ? CapturedMobKind.VILLAGER
                : CapturedMobKind.PIGLIN;
        if (!CapturedMobStackAdapter.isFilledCapturer(capturedKind, stack)
                || CapturedMobStackAdapter.isBaby(capturedKind, stack)) {
            return false;
        }
        if (kind == QuarryKind.PIGLIN) {
            return true;
        }
        CompoundTag data = CapturedMobStackAdapter.copyData(CapturedMobKind.VILLAGER, stack);
        String profession = data == null
                ? ""
                : data.getCompound("VillagerData")
                        .flatMap(villagerData -> villagerData.getString("profession"))
                        .orElse("");
        return !NITWIT_PROFESSION.equals(profession);
    }

    private boolean isValidWorker(ItemStack stack) {
        if (!workerCacheInitialized || !ItemStack.isSameItemSameComponents(cachedWorker, stack)) {
            cachedWorker = stack.copy();
            cachedWorkerValid = isValidWorker(kind, stack);
            workerCacheInitialized = true;
        }
        return cachedWorkerValid;
    }

    private boolean preparePendingResult(
            ServerLevel serverLevel,
            QuarryMaterialCatalog.CatalogSnapshot catalog,
            ItemStack pickaxe
    ) {
        Optional<QuarryMaterialCatalog.CatalogEntry> selection = pendingMaterial == null
                ? catalog.select(serverLevel.getRandom())
                : catalog.entries().stream()
                        .filter(QuarryMaterialCatalog.CatalogEntry::available)
                        .filter(entry -> entry.definition().id().toString().equals(pendingMaterial))
                        .findFirst();
        if (selection.isEmpty() && pendingMaterial != null) {
            clearPendingResult();
            selection = catalog.select(serverLevel.getRandom());
        }
        if (selection.isEmpty()) {
            return false;
        }
        QuarryMaterialDefinition definition = selection.get().definition();
        List<ItemStack> generated = QuarryResultFactory.create(
                definition,
                serverLevel,
                worldPosition,
                pickaxe,
                catalog.deepMining()
        );
        if (generated.isEmpty() || generated.stream().anyMatch(ItemStack::isEmpty)) {
            return false;
        }
        List<ItemStack> compacted = compactResult(generated);
        if (compacted.isEmpty()) {
            return false;
        }
        pendingResult = compacted;
        if (pendingMaterial == null) {
            pendingMaterial = definition.id().toString();
            pendingPreview = preview(definition, catalog.deepMining());
        } else if (pendingPreview.isEmpty()) {
            pendingPreview = preview(definition, catalog.deepMining());
        }
        setChanged();
        return true;
    }

    private void damagePickaxeAfterCycle(ServerLevel serverLevel) {
        ItemStack pickaxe = items.get(PICKAXE_SLOT);
        if (pickaxe.isEmpty() || QuarryEnchantments.protectsPickaxe(pickaxe, serverLevel.registryAccess())) {
            return;
        }
        pickaxe.hurtAndBreak(
                1,
                serverLevel,
                (LivingEntity) null,
                ignored -> items.set(PICKAXE_SLOT, ItemStack.EMPTY)
        );
        updateCycleDuration();
    }

    private int efficiencyLevel() {
        if (!(level instanceof ServerLevel serverLevel) || items.get(PICKAXE_SLOT).isEmpty()) {
            return 0;
        }
        return serverLevel.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(Enchantments.EFFICIENCY))
                .map(items.get(PICKAXE_SLOT)::getEnchantmentLevel)
                .orElse(0);
    }

    private int effectiveCycleTicks() {
        return quarryService.durationTicks(
                QuarryPickaxeCatalog.timingPosition(items.get(PICKAXE_SLOT)),
                efficiencyLevel()
        );
    }

    private int updateCycleDuration() {
        ItemStack pickaxe = items.get(PICKAXE_SLOT);
        if (durationCacheInitialized
                && ItemStack.isSameItemSameComponents(cachedDurationPickaxe, pickaxe)) {
            return cycleDurationTicks;
        }
        int nextDuration = effectiveCycleTicks();
        if (cycleDurationTicks != nextDuration) {
            cycleTicks = quarryService.rescaleProgress(cycleTicks, cycleDurationTicks, nextDuration);
            cycleDurationTicks = nextDuration;
            setChanged();
        }
        cachedDurationPickaxe = pickaxe.copy();
        durationCacheInitialized = true;
        return nextDuration;
    }

    private void refreshCycleForPickaxeChange() {
        pendingResult = List.of();
        mining = false;
        status = QuarryStatus.STOPPED;
        invalidateDuration();
    }

    private boolean deepMiningAvailable() {
        return kind == QuarryKind.VILLAGER
                && QuarryUpgradeItems.tier(items.get(UPGRADE_SLOT)).supportsDeepMining();
    }

    private void setDeepMining(boolean enabled) {
        boolean nextValue = kind == QuarryKind.VILLAGER && enabled && deepMiningAvailable();
        if (deepMining == nextValue) {
            return;
        }
        deepMining = nextValue;
        activity.wake();
        invalidateCatalog();
        markChangedAndSync();
    }

    private void disableDeepMiningIfUnavailable() {
        if (!deepMining || deepMiningAvailable()) {
            return;
        }
        deepMining = false;
        invalidateCatalog();
    }

    private void pause(QuarryStatus nextStatus) {
        pause(nextStatus, false);
    }

    private void pause(QuarryStatus nextStatus, boolean forceSync) {
        activity.transition(nextStatus == QuarryStatus.INVENTORY_FULL
                ? MachineActivityController.Activity.BLOCKED
                : MachineActivityController.Activity.INACTIVE);
        if (setMining(false) | setStatus(nextStatus) | forceSync) {
            markChangedAndSync();
        }
    }

    private boolean setMining(boolean value) {
        if (mining == value) {
            return false;
        }
        mining = value;
        return true;
    }

    private boolean setStatus(QuarryStatus value) {
        if (status == value) {
            return false;
        }
        status = value;
        return true;
    }

    private boolean hasAnyOutputSpace() {
        for (int slot = FIRST_OUTPUT_SLOT; slot < CONTAINER_SIZE; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean canStoreStacks(List<ItemStack> stacks) {
        return OrderedOutputInserter.canInsertAll(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                stacks
        );
    }

    private void storeStacks(List<ItemStack> stacks) {
        OrderedOutputInserter.insertAllValidated(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                stacks
        );
    }

    private static boolean merge(NonNullList<ItemStack> outputs, ItemStack source) {
        ItemStack remaining = source.copy();
        for (ItemStack output : outputs) {
            if (!output.isEmpty() && ItemStack.isSameItemSameComponents(output, remaining)) {
                int moved = Math.min(remaining.getCount(), output.getMaxStackSize() - output.getCount());
                output.grow(moved);
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        for (int index = 0; index < outputs.size(); index++) {
            if (!outputs.get(index).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack inserted = remaining.copy();
            inserted.setCount(moved);
            outputs.set(index, inserted);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return remaining.isEmpty();
    }

    private static List<ItemStack> compactResult(List<ItemStack> generated) {
        NonNullList<ItemStack> compacted = NonNullList.withSize(OUTPUT_SLOT_COUNT, ItemStack.EMPTY);
        for (ItemStack stack : generated) {
            if (!merge(compacted, stack)) {
                return List.of();
            }
        }
        return compacted.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }

    private void clearPendingResult() {
        pendingResult = List.of();
        pendingMaterial = null;
        pendingPreview = ItemStack.EMPTY;
    }

    private ItemStack previewForMaterial(@Nullable String materialId, boolean useDeepVariant) {
        if (materialId == null) {
            return ItemStack.EMPTY;
        }
        return QuarryMaterialCatalog.definitions(kind).stream()
                .filter(definition -> definition.id().toString().equals(materialId))
                .findFirst()
                .map(definition -> preview(definition, useDeepVariant))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack preview(QuarryMaterialDefinition definition, boolean deepMining) {
        return BuiltInRegistries.ITEM.getOptional(definition.silkResult(deepMining))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private void invalidateCatalog() {
        cachedCatalog = null;
        cachedCatalogPickaxe = ItemStack.EMPTY;
        cachedCatalogUpgrade = ItemStack.EMPTY;
        cachedCatalogRevision = -1;
    }

    private void invalidateWorker() {
        cachedWorker = ItemStack.EMPTY;
        cachedWorkerValid = false;
        workerCacheInitialized = false;
    }

    private void invalidateDuration() {
        cachedDurationPickaxe = ItemStack.EMPTY;
        durationCacheInitialized = false;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private static boolean isBroken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    private void migrate(int version) {
        if (version > QUARRY_DATA_VERSION) {
            // Unknown future fields stay preserved by their ItemStack components; known fields use safe defaults.
            return;
        }
        cycleDurationTicks = Math.max(1, cycleDurationTicks);
        cycleTicks = Math.clamp(cycleTicks, 0, Math.max(0, cycleDurationTicks - 1));
    }
}
