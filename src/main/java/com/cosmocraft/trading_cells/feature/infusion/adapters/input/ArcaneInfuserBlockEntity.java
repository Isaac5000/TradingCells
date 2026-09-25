package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionInput;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.application.port.input.ArcaneInfusionUseCase;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionAttempt;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionDecision;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.experience.PlayerExperienceTransfer;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandlers;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import com.cosmocraft.trading_cells.platform.neoforge.experience.MachineExperienceAccount;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineInsertionLimit;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ArcaneInfuserBlockEntity extends PortableMachineBlockEntity
        implements WorldlyContainer, MenuProvider, MachineInsertionLimit {
    public static final int TOP_LEFT_SLOT = 0;
    public static final int TOP_SLOT = 1;
    public static final int TOP_RIGHT_SLOT = 2;
    public static final int LEFT_SLOT = 3;
    public static final int CENTER_SLOT = 4;
    public static final int RIGHT_SLOT = 5;
    public static final int BOTTOM_LEFT_SLOT = 6;
    public static final int BOTTOM_SLOT = 7;
    public static final int BOTTOM_RIGHT_SLOT = 8;
    public static final int OUTPUT_SLOT = 9;
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int CONTAINER_SIZE = 10;
    public static final int EXPERIENCE_CAPACITY = Integer.MAX_VALUE;
    public static final int OUTPUT_STATE_EMPTY = 0;
    public static final int OUTPUT_STATE_INSUFFICIENT_EXPERIENCE = 1;
    public static final int OUTPUT_STATE_MANUAL_READY = 2;
    public static final int OUTPUT_STATE_PHYSICAL = 3;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String INVENTORY_VERSION_TAG = "InventoryVersion";
    private static final int INVENTORY_VERSION = 2;
    private static final int[] INPUTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUTPUTS = {OUTPUT_SLOT};
    private static final int[] LEGACY_SLOT_TARGETS = {
            TOP_SLOT,
            LEFT_SLOT,
            CENTER_SLOT,
            RIGHT_SLOT,
            BOTTOM_SLOT,
            OUTPUT_SLOT
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ArcaneInfusionUseCase service = FeatureComposition.arcaneInfusion();
    private final RecipeManager.CachedCheck<ArcaneInfusionInput, ArcaneInfusionRecipe> recipeCheck =
            RecipeManager.createCheck(ArcaneInfuserRegistrationAdapter.RECIPE_TYPE.get());
    private final MachineExperienceAccount experience = new MachineExperienceAccount(this::experienceChanged);
    private final ExperienceFluidHandler fluidHandler = experience.handler(this::requiredExperience);
    private @Nullable Identifier lockedRecipe;
    private @Nullable Identifier cachedRecipeId;
    private boolean previewDirty = true;
    private @Nullable RecipeManager cachedPreviewManager;
    private @Nullable ArcaneInfusionRecipe cachedPreviewRecipe;
    private ItemStack cachedPreviewResult = ItemStack.EMPTY;

    @Override
    public boolean supportsRedstoneControl() {
        return false;
    }

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> experience.amount() & 0xFFFF;
                case 1 -> experience.amount() >>> 16;
                case 2 -> requiredExperience() & 0xFFFF;
                case 3 -> requiredExperience() >>> 16;
                case 4 -> outputState();
                case 5 -> experience.fillStorage() ? 1 : 0;
                case 6 -> lockState();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            int unsignedValue = value & 0xFFFF;
            if (index == 0) {
                setStoredExperienceRaw((experience.amount() & 0x7FFF0000) | unsignedValue);
            } else if (index == 1) {
                setStoredExperienceRaw((experience.amount() & 0xFFFF) | ((unsignedValue & 0x7FFF) << 16));
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public ArcaneInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneInfuserRegistrationAdapter.BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        previewDirty = true;
    }

    @Override
    public void processTick() {
        if (!(level instanceof ServerLevel server) || lockedRecipe == null) { return; }
        refreshPreview();
        ArcaneInfusionRecipe recipe = cachedPreviewRecipe;
        if (recipe == null || cachedPreviewResult.isEmpty() || !hasRequiredExperience(recipe)) { return; }
        ItemStack output = items.get(OUTPUT_SLOT);
        ItemStack result = cachedPreviewResult.copy();
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || (long) output.getCount() + result.getCount() > output.getMaxStackSize())) { return; }
        List<ItemStack> remaining = consumptionPlan(recipe);
        if (remaining == null) { return; }
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) { items.set(slot, remaining.get(slot)); }
        if (!output.isEmpty()) { result.grow(output.getCount()); }
        items.set(OUTPUT_SLOT, result);
        experience.spend(recipe.experience());
        contentsChanged();
        playCompletionFeedback(server, recipe);
    }

    /** Returns a non-persistent recipe preview, or a completed physical result if one exists. */
    public ItemStack visibleResult() {
        ItemStack storedResult = items.get(OUTPUT_SLOT);
        if (!storedResult.isEmpty()) {
            return storedResult.copy();
        }
        refreshPreview();
        return cachedPreviewResult.copy();
    }

    public int requiredExperience() {
        refreshPreview();
        return cachedPreviewRecipe == null ? 0 : cachedPreviewRecipe.experience();
    }

    public int outputState() {
        if (!items.get(OUTPUT_SLOT).isEmpty()) {
            return OUTPUT_STATE_PHYSICAL;
        }
        refreshPreview();
        ArcaneInfusionRecipe recipe = cachedPreviewRecipe;
        if (recipe == null || cachedPreviewResult.isEmpty()) {
            return OUTPUT_STATE_EMPTY;
        }
        if (!hasRequiredExperience(recipe)) {
            return OUTPUT_STATE_INSUFFICIENT_EXPERIENCE;
        }
        return lockedRecipe == null && consumptionPlan(recipe) != null
                ? OUTPUT_STATE_MANUAL_READY : OUTPUT_STATE_EMPTY;
    }

    /** Atomically consumes the resources represented by the result the player just took. */
    public boolean takeVisibleResult(ItemStack expectedResult) {
        if (expectedResult.isEmpty()) {
            return false;
        }
        ItemStack storedResult = items.get(OUTPUT_SLOT);
        if (!storedResult.isEmpty()) {
            if (!sameResult(storedResult, expectedResult)) {
                return false;
            }
            storedResult.shrink(expectedResult.getCount());
            if (storedResult.isEmpty()) {
                items.set(OUTPUT_SLOT, ItemStack.EMPTY);
            }
            contentsChanged();
            return true;
        }
        refreshPreview();
        ArcaneInfusionRecipe recipe = cachedPreviewRecipe;
        if (lockedRecipe != null || recipe == null
                || !hasRequiredExperience(recipe)
                || !sameResult(cachedPreviewResult, expectedResult)) {
            return false;
        }
        ItemStack remainder = cachedPreviewResult.copy();
        remainder.shrink(expectedResult.getCount());
        if (!consume(recipe)) { return false; }
        items.set(OUTPUT_SLOT, remainder);
        contentsChanged();
        if (level instanceof ServerLevel serverLevel) {
            playCompletionFeedback(serverLevel, recipe);
        }
        return true;
    }

    public int storedExperience() {
        return experience.amount();
    }

    @Override
    public MachineDiagnosticSnapshot machineDiagnosticSnapshot() {
        int state = outputState();
        ItemStack output = items.get(OUTPUT_SLOT);
        MachineDiagnosticStatus diagnosticStatus = switch (state) {
            case OUTPUT_STATE_MANUAL_READY -> MachineDiagnosticStatus.RUNNING;
            case OUTPUT_STATE_PHYSICAL -> MachineDiagnosticStatus.BLOCKED;
            default -> MachineDiagnosticStatus.INACTIVE;
        };
        String reason = switch (state) {
            case OUTPUT_STATE_INSUFFICIENT_EXPERIENCE -> "missing_experience";
            case OUTPUT_STATE_PHYSICAL -> "output_full";
            case OUTPUT_STATE_EMPTY -> "missing_recipe";
            default -> MachineDiagnosticSnapshot.NONE;
        };
        return new MachineDiagnosticSnapshot(
                diagnosticStatus,
                reason,
                0,
                0,
                experience.amount(),
                output.getCount(),
                output.isEmpty() ? getMaxStackSize() : output.getMaxStackSize()
        );
    }

    public ResourceHandler<FluidResource> fluidHandler() {
        return fluidHandler;
    }

    public void transferExperience(ServerPlayer player, ArcaneInfusionTransferAction action, int requestedLevels) {
        if (level instanceof ServerLevel && stillValid(player)) {
            experience.transfer(player, action.id(), requestedLevels);
        }
    }

    public MachineExperienceAccount experience() { return experience; }
    public @Nullable Identifier lockedRecipeId() { return lockedRecipe; }
    public ItemStack lockedRecipeResult() {
        refreshPreview();
        return lockedRecipe == null || cachedPreviewRecipe == null ? ItemStack.EMPTY : cachedPreviewRecipe.result().displayResult();
    }
    public int lockState() {
        refreshPreview();
        return lockedRecipe == null ? 0 : cachedPreviewRecipe == null ? 2 : 1;
    }
    public void toggleRecipeLock() {
        toggleRecipeLock(null);
    }
    public void toggleRecipeLock(@Nullable Identifier selected) {
        refreshPreview();
        if (lockedRecipe != null) { lockedRecipe = null; }
        else {
            Identifier candidate = cachedPreviewResult.isEmpty() ? selected : cachedRecipeId;
            if (recipe(candidate) == null) { return; }
            lockedRecipe = candidate;
        }
        contentsChanged();
    }
    public @Nullable Identifier activeRecipeId() { refreshPreview(); return cachedRecipeId; }
    public @Nullable ArcaneInfusionRecipe recipe(@Nullable Identifier id) {
        if (id == null || !(level instanceof ServerLevel server)) { return null; }
        return server.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id))
                .map(RecipeHolder::value).filter(ArcaneInfusionRecipe.class::isInstance)
                .map(ArcaneInfusionRecipe.class::cast).orElse(null);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.arcane_infuser");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new ArcaneInfuserMenu(containerId, inventory, this, dataAccess);
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
        return validSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (!validSlot(slot) || count <= 0 || items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot).split(count);
        if (items.get(slot).isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        contentsChanged();
        return removed;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        invalidatePreview();
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!validSlot(slot) || !canReplaceSlotContents(slot, stack)) {
            return;
        }
        ItemStack inserted = stack.copy();
        inserted.setCount(Math.min(inserted.getCount(), inserted.getMaxStackSize()));
        items.set(slot, inserted);
        contentsChanged();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        return slot >= 0 && slot < INPUT_SLOT_COUNT && !stack.isEmpty();
    }

    @Override
    public void clearContent() {
        clearContentsForBlockDrop();
        markChangedAndSync();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        return direction == Direction.DOWN ? OUTPUTS : INPUTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot,
            @NonNull ItemStack stack,
            @Nullable Direction direction
    ) {
        return direction != Direction.DOWN && insertionLimit(slot, stack) > 0;
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot,
            @NonNull ItemStack stack,
            @NonNull Direction direction
    ) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public int insertionLimit(int slot, ItemStack stack) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT || stack.isEmpty()) { return 0; }
        if (lockedRecipe == null) { return stack.getMaxStackSize(); }
        refreshPreview();
        ArcaneInfusionRecipe recipe = cachedPreviewRecipe;
        var input = new ArcaneInfusionInput(List.copyOf(items.subList(0, INPUT_SLOT_COUNT)));
        if (recipe == null || recipe.inputIngredient(slot, input).ingredient().isEmpty()) { return 0; }
        var ingredient = recipe.inputIngredient(slot, input).ingredient().orElseThrow();
        if (!ingredient.ingredient().test(stack) || !recipe.matchesInputRestrictions(slot, stack)) { return 0; }
        ItemStack current = items.get(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) { return 0; }
        return Math.max(0, ingredient.count() - current.getCount());
    }

    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        if (insideTransaction && validSlot(slot)) {
            items.set(slot, stack.copy());
            invalidatePreview();
            setChanged();
        } else {
            setItem(slot, stack);
        }
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        int inventoryVersion = input.getIntOr(INVENTORY_VERSION_TAG, 0);
        if (inventoryVersion >= INVENTORY_VERSION) {
            for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
                items.set(slot, input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            }
        } else {
            for (int legacySlot = 0; legacySlot < LEGACY_SLOT_TARGETS.length; legacySlot++) {
                items.set(
                        LEGACY_SLOT_TARGETS[legacySlot],
                        input.read(SLOT_TAG_PREFIX + legacySlot, ItemStack.CODEC).orElse(ItemStack.EMPTY)
                );
            }
        }
        experience.load(input);
        String recipeId = input.getStringOr("LockedRecipe", "");
        lockedRecipe = recipeId.isEmpty() ? null : Identifier.tryParse(recipeId);
        invalidatePreview();
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(INVENTORY_VERSION_TAG, INVENTORY_VERSION);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        experience.save(output);
        if (lockedRecipe != null) { output.putString("LockedRecipe", lockedRecipe.toString()); }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        experience.clear();
        lockedRecipe = null;
        invalidatePreview();
        setChanged();
    }

    private void contentsChanged() {
        invalidatePreview();
        markChangedAndSync();
    }

    private void setStoredExperienceRaw(int value) {
        int clamped = Math.clamp(value, 0, EXPERIENCE_CAPACITY);
        experience.setRaw(clamped);
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private boolean canReplaceSlotContents(int slot, ItemStack replacement) {
        if (slot != OUTPUT_SLOT) {
            return replacement.isEmpty() || canPlaceItem(slot, replacement);
        }
        ItemStack current = items.get(OUTPUT_SLOT);
        return replacement.isEmpty()
                || !current.isEmpty()
                && replacement.getCount() <= current.getCount()
                && ItemStack.isSameItemSameComponents(current, replacement);
    }

    private void refreshPreview() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        RecipeManager currentManager = serverLevel.recipeAccess();
        if (cachedPreviewManager != currentManager) {
            cachedPreviewManager = currentManager;
            previewDirty = true;
        }
        if (!previewDirty) {
            return;
        }
        previewDirty = false;
        cachedPreviewRecipe = null;
        cachedRecipeId = null;
        cachedPreviewResult = ItemStack.EMPTY;
        ArcaneInfusionInput input = new ArcaneInfusionInput(List.copyOf(items.subList(0, INPUT_SLOT_COUNT)));
        Optional<RecipeHolder<ArcaneInfusionRecipe>> match;
        if (lockedRecipe == null) {
            match = recipeCheck.getRecipeFor(input, serverLevel);
        } else {
            match = currentManager.byKey(ResourceKey.create(Registries.RECIPE, lockedRecipe))
                    .filter(holder -> holder.value() instanceof ArcaneInfusionRecipe)
                    .map(holder -> new RecipeHolder<>(holder.id(), (ArcaneInfusionRecipe) holder.value()));
        }
        if (match.isEmpty()) {
            return;
        }
        ArcaneInfusionRecipe recipe = match.get().value();
        cachedRecipeId = match.get().id().identifier();
        cachedPreviewRecipe = recipe;
        ItemStack result = recipe.matches(input, serverLevel) ? recipe.assemble(input) : ItemStack.EMPTY;
        if (!result.isEmpty()) {
            cachedPreviewRecipe = recipe;
            cachedPreviewResult = result;
        }
    }

    private void invalidatePreview() {
        previewDirty = true;
        cachedPreviewRecipe = null;
        cachedRecipeId = null;
        cachedPreviewResult = ItemStack.EMPTY;
    }

    private void experienceChanged() {
        markChangedAndSync();
    }

    private boolean hasRequiredExperience(ArcaneInfusionRecipe recipe) {
        return service.evaluate(new ArcaneInfusionAttempt(
                true,
                true,
                experience.amount(),
                recipe.experience()
        )) == ArcaneInfusionDecision.READY;
    }

    private @Nullable List<ItemStack> consumptionPlan(ArcaneInfusionRecipe recipe) {
        List<ItemStack> remaining = new java.util.ArrayList<>();
        var input = new ArcaneInfusionInput(List.copyOf(items.subList(0, INPUT_SLOT_COUNT)));
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack consumed = items.get(slot);
            int count = recipe.consumedCount(slot, input);
            ItemStack next = consumed.copy();
            if (count > 0) {
                ItemStackTemplate remainder = consumed.getCraftingRemainder();
                next.shrink(count);
                if (remainder != null) {
                    ItemStack returned = remainder.create();
                    long amount = (long) returned.getCount() * count;
                    if (amount > returned.getMaxStackSize()) { return null; }
                    returned.setCount((int) amount);
                    if (!next.isEmpty()) {
                        if (!ItemStack.isSameItemSameComponents(next, returned)
                                || next.getCount() + returned.getCount() > next.getMaxStackSize()) { return null; }
                        returned.grow(next.getCount());
                    }
                    next = returned;
                }
            }
            remaining.add(next);
        }
        return remaining;
    }

    private boolean consume(ArcaneInfusionRecipe recipe) {
        List<ItemStack> remaining = consumptionPlan(recipe);
        if (remaining == null) { return false; }
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) { items.set(slot, remaining.get(slot)); }
        experience.spend(recipe.experience());
        return true;
    }

    private static boolean sameResult(ItemStack current, ItemStack expected) {
        return expected.getCount() > 0
                && expected.getCount() <= current.getCount()
                && ItemStack.isSameItemSameComponents(current, expected);
    }

    private void playCompletionFeedback(ServerLevel serverLevel, ArcaneInfusionRecipe recipe) {
        boolean happyParticles = recipe.result().usesHappyVillagerParticles();
        serverLevel.sendParticles(
                happyParticles ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SCULK_SOUL,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.7D,
                worldPosition.getZ() + 0.5D,
                14,
                0.32D,
                0.28D,
                0.32D,
                0.03D
        );
        serverLevel.playSound(
                null,
                worldPosition,
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS,
                0.8F,
                0.9F + serverLevel.getRandom().nextFloat() * 0.2F
        );
    }
}
