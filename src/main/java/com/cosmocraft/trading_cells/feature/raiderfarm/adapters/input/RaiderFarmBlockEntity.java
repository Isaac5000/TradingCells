package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.RaiderFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.application.port.input.RaiderFarmUseCase;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSwordTierCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmWeaponSnapshot;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.machine.OrderedOutputInserter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
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

public final class RaiderFarmBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int WORKER_SLOT = 0;
    public static final int SWORD_SLOT = 1;
    public static final int FIRST_OUTPUT_SLOT = 2;
    public static final int OUTPUT_SLOT_COUNT = 18;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String CYCLE_TICKS_TAG = "CycleTicks";
    private static final String CYCLE_DURATION_TAG = "CycleDurationTicks";
    private static final String KIND_TAG = "RaiderKind";
    private static final String TARGET_TAG = "RaiderTarget";
    private static final String LOOT_MASK_TAG = "EnabledLootMask";
    private static final String DISABLED_DYNAMIC_LOOT_COUNT_TAG = "DisabledDynamicLootCount";
    private static final String DISABLED_DYNAMIC_LOOT_TAG_PREFIX = "DisabledDynamicLoot";
    private static final String STORED_EXPERIENCE_TAG = "StoredExperience";
    private static final String PENDING_COUNT_TAG = "PendingLootCount";
    private static final String PENDING_READY_TAG = "PendingLootReady";
    private static final String PENDING_BATCH_STARTED_TAG = "PendingBatchStarted";
    private static final String PENDING_TAG_PREFIX = "PendingLoot";
    private static final String ENABLED_TAG = "Enabled";
    private static final int MAX_PERSISTED_PENDING_STACKS = 1_024;
    private static final float DEATH_SOUND_VOLUME = 0.4F;
    private static final float DEATH_SOUND_RANGE = 8.0F;
    private static final SoundEvent PILLAGER_DEATH_SOUND = shortRange(SoundEvents.PILLAGER_DEATH);
    private static final SoundEvent EVOKER_DEATH_SOUND = shortRange(SoundEvents.EVOKER_DEATH);
    private static final SoundEvent RAVAGER_DEATH_SOUND = shortRange(SoundEvents.RAVAGER_DEATH);
    private static final SoundEvent WITCH_DEATH_SOUND = shortRange(SoundEvents.WITCH_DEATH);
    private static final int[] INPUT_SLOTS = new int[]{WORKER_SLOT, SWORD_SLOT};
    private static final int[] OUTPUT_SLOTS = IntStream.range(FIRST_OUTPUT_SLOT, CONTAINER_SIZE).toArray();

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final RaiderFarmUseCase rules = FeatureComposition.raiderFarm();
    private final MachineActivityController activity = new MachineActivityController();
    private RaiderFarmKind kind = RaiderFarmKind.PILLAGER;
    private Identifier targetId = RaiderFarmTargetCatalog.id(RaiderFarmKind.PILLAGER);
    private int enabledLootMask = RaiderFarmLoot.allEnabledMask();
    private final Set<Identifier> disabledDynamicLoot = new HashSet<>();
    private int cycleTicks;
    private int cycleDurationTicks = rules.effectiveCycleTicks(0.0D, 0);
    private int storedExperience;
    private boolean enabled = true;
    private boolean hunting;
    private List<ItemStack> pendingLoot = List.of();
    private boolean pendingLootReady;
    private boolean pendingBatchStarted;
    private boolean cachedAdultWorker;
    private boolean workerCacheInitialized;
    private boolean cachedSupportedSword;
    private double cachedTierPosition;
    private double cachedDamageLevel;
    private int cachedLooting;
    private int cachedSweeping;
    private boolean cachedWarriorsTouch;
    private int cachedDecapitationLevel;
    private boolean swordCacheInitialized;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cycleTicks;
                case 1 -> cycleDurationTicks;
                case 2 -> kind.ordinal();
                case 3 -> enabledLootMask;
                case 4 -> storedExperience;
                case 5 -> hunting ? 1 : 0;
                case 6 -> rules.simulatedKills(cachedSweeping);
                case 7 -> pendingBatchStarted ? 1 : 0;
                case 8 -> enabled ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cycleTicks = Math.clamp(value, 0, Math.max(1, cycleDurationTicks));
                case 2 -> selectKind(RaiderFarmKind.fromId(value));
                case 3 -> setEnabledLootMask(value);
                default -> {
                    // Read-only values are synchronized from the server.
                }
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public RaiderFarmBlockEntity(BlockPos pos, BlockState state) {
        super(RaiderFarmRegistrationAdapter.BLOCK_ENTITY.get(), pos, state);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public RaiderFarmKind selectedKind() {
        return kind;
    }

    public Identifier selectedTargetId() {
        return targetId;
    }

    public Set<Identifier> disabledDynamicLoot() {
        return Set.copyOf(disabledDynamicLoot);
    }

    public int cycleTicks() {
        return cycleTicks;
    }

    public int cycleDurationTicks() {
        return cycleDurationTicks;
    }

    public void extractExperience(Player player) {
        if (level == null || level.isClientSide() || storedExperience <= 0) {
            return;
        }
        int extracted = storedExperience;
        storedExperience = 0;
        player.giveExperiencePoints(extracted);
        markChangedAndSync();
    }

    public void toggleEnabled() {
        if (level == null || level.isClientSide()) {
            return;
        }
        enabled = !enabled;
        if (enabled) {
            activity.wake();
        } else {
            setHunting(false);
            activity.transition(MachineActivityController.Activity.INACTIVE);
        }
        markChangedAndSync();
    }

    @Override
    public void processTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!enabled) {
            setHunting(false);
            activity.transition(MachineActivityController.Activity.INACTIVE);
            return;
        }
        if (activity.remainsInactive() || activity.remainsBlocked()) {
            return;
        }

        refreshInputCaches(serverLevel);
        int duration = updateCycleDuration();
        boolean canHunt = cachedAdultWorker && cachedSupportedSword;
        boolean hasEnabledLoot = hasGeneratableLoot();
        boolean completingCycle = cycleTicks >= duration - 1;
        boolean outputHasCapacity = OrderedOutputInserter.hasAnyCapacity(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT
        );
        if (!canHunt) {
            clearPendingLoot();
        } else if (completingCycle
                && !pendingLootReady
                && (!hasEnabledLoot || outputHasCapacity)) {
            pendingLoot = RaiderFarmLootAdapter.generate(
                    targetId,
                    kind,
                    enabledLootMask,
                    disabledDynamicLoot,
                    rules.simulatedKills(cachedSweeping),
                    cachedLooting,
                    cachedDecapitationLevel,
                    serverLevel,
                    items.get(SWORD_SLOT),
                    serverLevel.getRandom(),
                    rules
            );
            pendingLootReady = true;
            setChanged();
        }

        // A pending result can only come from an older save. Complete it once and discard
        // any overflow instead of preserving invisible loot that blocks later cycles.
        boolean outputAvailable = !hasEnabledLoot
                || outputHasCapacity
                || completingCycle && pendingLootReady;
        activity.transition(!canHunt
                ? MachineActivityController.Activity.INACTIVE
                : outputAvailable
                        ? MachineActivityController.Activity.ACTIVE
                        : MachineActivityController.Activity.BLOCKED);

        int previousTicks = cycleTicks;
        TimedProcess.Step step = rules.advance(cycleTicks, duration, canHunt, outputAvailable);
        cycleTicks = step.ticks();
        setHunting(canHunt && outputAvailable);
        switch (step.transition()) {
            case IDLE, PAUSED -> {
                // The machine is waiting without changing its persisted state.
            }
            case RESET -> {
                clearPendingLoot();
                if (previousTicks != 0) {
                    markChangedAndSync();
                }
            }
            case ADVANCED -> {
                setChanged();
            }
            case COMPLETED -> completeCycle(serverLevel);
        }
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.raider_farm");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new RaiderFarmMenu(containerId, inventory, this, dataAccess);
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
        handleSlotChanged(slot, true);
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
        handleSlotChanged(slot, true);
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot) || !stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        int maximum = isInputSlot(slot) ? 1 : Math.min(64, inserted.getMaxStackSize());
        inserted.setCount(Math.min(maximum, inserted.getCount()));
        boolean changedInput = isInputSlot(slot)
                && !ItemStack.isSameItemSameComponents(items.get(slot), inserted);
        items.set(slot, inserted);
        handleSlotChanged(slot, changedInput);
        markChangedAndSync();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        return switch (slot) {
            case WORKER_SLOT -> isAdultVillager(stack);
            case SWORD_SLOT -> MobFarmSwordTierCatalog.isSupported(stack);
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
        return direction == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
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
        kind = RaiderFarmKind.fromId(input.getIntOr(KIND_TAG, 0));
        targetId = loadTargetId(input.getStringOr(TARGET_TAG, ""), kind);
        kind = RaiderFarmTargetCatalog.staticKind(targetId);
        enabledLootMask = input.getIntOr(LOOT_MASK_TAG, RaiderFarmLoot.allEnabledMask())
                & RaiderFarmLoot.allEnabledMask();
        disabledDynamicLoot.clear();
        int disabledCount = Math.clamp(input.getIntOr(DISABLED_DYNAMIC_LOOT_COUNT_TAG, 0), 0, 2_048);
        for (int index = 0; index < disabledCount; index++) {
            Identifier itemId = Identifier.tryParse(input.getStringOr(
                    DISABLED_DYNAMIC_LOOT_TAG_PREFIX + index,
                    ""
            ));
            if (itemId != null) {
                disabledDynamicLoot.add(itemId);
            }
        }
        cycleDurationTicks = Math.max(1, input.getIntOr(
                CYCLE_DURATION_TAG,
                rules.effectiveCycleTicks(0.0D, 0)
        ));
        cycleTicks = Math.clamp(input.getIntOr(CYCLE_TICKS_TAG, 0), 0, cycleDurationTicks);
        storedExperience = Math.max(0, input.getIntOr(STORED_EXPERIENCE_TAG, 0));
        int pendingCount = Math.clamp(input.getIntOr(PENDING_COUNT_TAG, 0), 0, MAX_PERSISTED_PENDING_STACKS);
        List<ItemStack> loadedPending = new ArrayList<>(pendingCount);
        for (int index = 0; index < pendingCount; index++) {
            ItemStack stack = input.read(PENDING_TAG_PREFIX + index, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                loadedPending.add(stack);
            }
        }
        pendingLoot = List.copyOf(loadedPending);
        pendingLootReady = input.getBooleanOr(PENDING_READY_TAG, pendingCount > 0);
        pendingBatchStarted = input.getBooleanOr(PENDING_BATCH_STARTED_TAG, false);
        enabled = input.getBooleanOr(ENABLED_TAG, true);
        hunting = false;
        invalidateInputCaches();
        activity.reset();
    }

    private static Identifier loadTargetId(String storedTarget, RaiderFarmKind fallbackKind) {
        Identifier fallback = RaiderFarmTargetCatalog.id(fallbackKind);
        if (storedTarget.isBlank()) {
            return fallback;
        }
        Identifier parsed = Identifier.tryParse(storedTarget);
        return parsed != null && RaiderFarmTargetCatalog.isKnownTarget(parsed) ? parsed : fallback;
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
            output.putInt(CYCLE_DURATION_TAG, cycleDurationTicks);
        }
        if (kind != RaiderFarmKind.PILLAGER) {
            output.putInt(KIND_TAG, kind.persistentId());
        }
        if (!targetId.equals(RaiderFarmTargetCatalog.id(kind))) {
            output.putString(TARGET_TAG, targetId.toString());
        }
        if (enabledLootMask != RaiderFarmLoot.allEnabledMask()) {
            output.putInt(LOOT_MASK_TAG, enabledLootMask);
        }
        if (!disabledDynamicLoot.isEmpty()) {
            List<Identifier> sortedDisabled = disabledDynamicLoot.stream().sorted().toList();
            output.putInt(DISABLED_DYNAMIC_LOOT_COUNT_TAG, sortedDisabled.size());
            for (int index = 0; index < sortedDisabled.size(); index++) {
                output.putString(DISABLED_DYNAMIC_LOOT_TAG_PREFIX + index, sortedDisabled.get(index).toString());
            }
        }
        if (storedExperience > 0) {
            output.putInt(STORED_EXPERIENCE_TAG, storedExperience);
        }
        if (pendingLootReady) {
            output.putBoolean(PENDING_READY_TAG, true);
            output.putInt(PENDING_COUNT_TAG, pendingLoot.size());
            for (int index = 0; index < pendingLoot.size(); index++) {
                output.store(PENDING_TAG_PREFIX + index, ItemStack.CODEC, pendingLoot.get(index));
            }
        }
        if (pendingBatchStarted) {
            output.putBoolean(PENDING_BATCH_STARTED_TAG, true);
        }
        if (!enabled) {
            output.putBoolean(ENABLED_TAG, false);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        cycleTicks = 0;
        storedExperience = 0;
        enabled = true;
        targetId = RaiderFarmTargetCatalog.id(RaiderFarmKind.PILLAGER);
        kind = RaiderFarmKind.PILLAGER;
        disabledDynamicLoot.clear();
        hunting = false;
        clearPendingLoot();
        invalidateInputCaches();
        activity.reset();
        setChanged();
    }

    private void completeCycle(ServerLevel serverLevel) {
        OrderedOutputInserter.insertAllAvailable(
                items,
                FIRST_OUTPUT_SLOT,
                OUTPUT_SLOT_COUNT,
                pendingLoot
        );
        int kills = rules.simulatedKills(cachedSweeping);
        storedExperience = (int) Math.min(Integer.MAX_VALUE, (long) storedExperience + kills * 5L);
        clearPendingLoot();
        damageSword(serverLevel);
        serverLevel.playSound(
                null,
                worldPosition,
                switch (kind) {
                    case PILLAGER -> PILLAGER_DEATH_SOUND;
                    case EVOKER -> EVOKER_DEATH_SOUND;
                    case RAVAGER -> RAVAGER_DEATH_SOUND;
                    case WITCH -> WITCH_DEATH_SOUND;
                },
                SoundSource.BLOCKS,
                DEATH_SOUND_VOLUME,
                1.0F
        );
        markChangedAndSync();
    }

    private static SoundEvent shortRange(SoundEvent sound) {
        return SoundEvent.createFixedRangeEvent(sound.location(), DEATH_SOUND_RANGE);
    }

    private void damageSword(ServerLevel serverLevel) {
        ItemStack sword = items.get(SWORD_SLOT);
        if (sword.isEmpty() || cachedWarriorsTouch) {
            return;
        }
        sword.hurtAndBreak(
                1,
                serverLevel,
                (LivingEntity) null,
                ignored -> items.set(SWORD_SLOT, ItemStack.EMPTY)
        );
        swordCacheInitialized = false;
        updateCycleDuration();
    }

    private void selectKind(RaiderFarmKind selected) {
        selectTarget(RaiderFarmTargetCatalog.id(selected));
    }

    public void selectTarget(Identifier selectedTargetId) {
        if (targetId.equals(selectedTargetId)
                || pendingBatchStarted
                || !RaiderFarmTargetCatalog.isKnownTarget(selectedTargetId)) {
            return;
        }
        targetId = selectedTargetId;
        kind = RaiderFarmTargetCatalog.staticKind(selectedTargetId);
        cycleTicks = 0;
        clearPendingLoot();
        swordCacheInitialized = false;
        activity.wake();
        markChangedAndSync();
    }

    private void setEnabledLootMask(int mask) {
        int sanitized = mask & RaiderFarmLoot.allEnabledMask();
        if (enabledLootMask == sanitized || pendingBatchStarted) {
            return;
        }
        enabledLootMask = sanitized;
        clearPendingLoot();
        activity.wake();
        markChangedAndSync();
    }

    public void toggleDynamicLoot(Identifier itemId) {
        if (pendingBatchStarted || !RaiderFarmTargetCatalog.dynamicLoot(targetId).stream()
                .anyMatch(stack -> itemId.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                )))) {
            return;
        }
        if (!disabledDynamicLoot.remove(itemId)) {
            disabledDynamicLoot.add(itemId);
        }
        clearPendingLoot();
        activity.wake();
        markChangedAndSync();
    }

    private void handleSlotChanged(int slot, boolean resetProcess) {
        activity.wake();
        if (slot == WORKER_SLOT) {
            workerCacheInitialized = false;
        } else if (slot == SWORD_SLOT) {
            swordCacheInitialized = false;
        }
        if (isInputSlot(slot) && resetProcess) {
            cycleTicks = 0;
            hunting = false;
            clearPendingLoot();
        }
    }

    private int updateCycleDuration() {
        if (level instanceof ServerLevel serverLevel) {
            refreshInputCaches(serverLevel);
        }
        int duration = rules.effectiveCycleTicks(cachedTierPosition, cachedDamageLevel);
        if (cycleDurationTicks != duration) {
            cycleTicks = rules.rescaleProgress(cycleTicks, cycleDurationTicks, duration);
            cycleDurationTicks = duration;
        }
        return duration;
    }

    private void refreshInputCaches(ServerLevel serverLevel) {
        if (!workerCacheInitialized) {
            ItemStack worker = items.get(WORKER_SLOT);
            cachedAdultWorker = isAdultVillager(worker);
            workerCacheInitialized = true;
        }
        if (!swordCacheInitialized) {
            ItemStack sword = items.get(SWORD_SLOT);
            MobFarmWeaponSnapshot weapon = MobFarmWeaponSnapshot.inspect(
                    sword,
                    serverLevel,
                    BuiltInRegistries.ENTITY_TYPE.getOptional(
                            RaiderFarmTargetCatalog.entityTypeId(targetId)
                    ).orElse(EntityTypes.PILLAGER)
            );
            cachedSupportedSword = weapon.supported();
            cachedTierPosition = weapon.tierPosition();
            cachedDamageLevel = weapon.effectiveDamageLevel();
            cachedLooting = weapon.lootingLevel();
            cachedSweeping = weapon.sweepingEdgeLevel();
            cachedWarriorsTouch = weapon.warriorsTouch();
            cachedDecapitationLevel = weapon.decapitationLevel();
            swordCacheInitialized = true;
        }
    }

    private boolean hasGeneratableLoot() {
        for (RaiderFarmLoot loot : RaiderFarmTargetCatalog.availableCategories(
                targetId,
                cachedDecapitationLevel > 0
        )) {
            if (!rules.isEnabled(enabledLootMask, kind, loot)) {
                continue;
            }
            return true;
        }
        return RaiderFarmTargetCatalog.dynamicLoot(targetId).stream().anyMatch(stack -> {
            Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return itemId != null && !disabledDynamicLoot.contains(itemId);
        });
    }

    private void invalidateInputCaches() {
        workerCacheInitialized = false;
        swordCacheInitialized = false;
    }

    private void setHunting(boolean value) {
        if (hunting == value) {
            return;
        }
        hunting = value;
        markChangedAndSync();
    }

    private void clearPendingLoot() {
        pendingLoot = List.of();
        pendingLootReady = false;
        pendingBatchStarted = false;
    }

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static boolean isInputSlot(int slot) {
        return slot == WORKER_SLOT || slot == SWORD_SLOT;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < CONTAINER_SIZE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
