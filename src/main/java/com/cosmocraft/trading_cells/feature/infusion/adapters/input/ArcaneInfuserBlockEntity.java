package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionInput;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.application.port.input.ArcaneInfusionUseCase;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionAttempt;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionDecision;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
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
        implements WorldlyContainer, MenuProvider {
    public static final int TOP_SLOT = 0;
    public static final int LEFT_SLOT = 1;
    public static final int CENTER_SLOT = 2;
    public static final int RIGHT_SLOT = 3;
    public static final int BOTTOM_SLOT = 4;
    public static final int OUTPUT_SLOT = 5;
    public static final int INPUT_SLOT_COUNT = 5;
    public static final int CONTAINER_SIZE = 6;
    public static final int EXPERIENCE_CAPACITY = Integer.MAX_VALUE;
    public static final int OUTPUT_STATE_EMPTY = 0;
    public static final int OUTPUT_STATE_INSUFFICIENT_EXPERIENCE = 1;
    public static final int OUTPUT_STATE_MANUAL_READY = 2;
    public static final int OUTPUT_STATE_PHYSICAL = 3;
    public static final int OUTPUT_STATE_AUTOMATIC_PENDING = 4;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String STORED_EXPERIENCE_TAG = "StoredExperience";
    private static final String AUTOMATIC_MODE_TAG = "AutomaticMode";
    private static final int[] ALL_SLOTS = IntStream.range(0, CONTAINER_SIZE).toArray();

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ArcaneInfusionUseCase service = FeatureComposition.arcaneInfusion();
    private final RecipeManager.CachedCheck<ArcaneInfusionInput, ArcaneInfusionRecipe> recipeCheck =
            RecipeManager.createCheck(ArcaneInfuserRegistrationAdapter.RECIPE_TYPE.get());
    private final ExperienceFluidHandler fluidHandler = new ExperienceFluidHandler(
            () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
            this::storedExperience,
            this::setStoredExperienceRaw,
            () -> EXPERIENCE_CAPACITY,
            true,
            this::experienceChanged
    );
    private int storedExperience;
    private boolean automaticMode;
    private boolean previewDirty = true;
    private @Nullable RecipeManager cachedPreviewManager;
    private @Nullable ArcaneInfusionRecipe cachedPreviewRecipe;
    private ItemStack cachedPreviewResult = ItemStack.EMPTY;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> storedExperience & 0xFFFF;
                case 1 -> storedExperience >>> 16;
                case 2 -> automaticMode ? 1 : 0;
                case 3 -> requiredExperience() & 0xFFFF;
                case 4 -> requiredExperience() >>> 16;
                case 5 -> outputState();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            int unsignedValue = value & 0xFFFF;
            if (index == 0) {
                setStoredExperienceRaw((storedExperience & 0x7FFF0000) | unsignedValue);
            } else if (index == 1) {
                setStoredExperienceRaw((storedExperience & 0xFFFF) | ((unsignedValue & 0x7FFF) << 16));
            } else if (index == 2) {
                automaticMode = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ArcaneInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneInfuserRegistrationAdapter.BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        previewDirty = true;
        scheduleCheck();
    }

    @Override
    public void processTick() {
        if (!automaticMode || !(level instanceof ServerLevel serverLevel)
                || !items.get(OUTPUT_SLOT).isEmpty()) {
            return;
        }
        refreshPreview();
        ArcaneInfusionRecipe recipe = cachedPreviewRecipe;
        if (recipe == null || cachedPreviewResult.isEmpty() || !hasRequiredExperience(recipe)) {
            return;
        }
        ItemStack result = cachedPreviewResult.copy();
        consume(recipe);
        items.set(OUTPUT_SLOT, result);
        contentsChanged();
        playCompletionFeedback(serverLevel, recipe);
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
        return automaticMode ? OUTPUT_STATE_AUTOMATIC_PENDING : OUTPUT_STATE_MANUAL_READY;
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
        if (automaticMode) {
            return false;
        }

        refreshPreview();
        ArcaneInfusionRecipe recipe = cachedPreviewRecipe;
        if (recipe == null
                || !hasRequiredExperience(recipe)
                || !sameResult(cachedPreviewResult, expectedResult)) {
            return false;
        }
        consume(recipe);
        contentsChanged();
        if (level instanceof ServerLevel serverLevel) {
            playCompletionFeedback(serverLevel, recipe);
        }
        return true;
    }

    public int storedExperience() {
        return storedExperience;
    }

    public boolean automaticMode() {
        return automaticMode;
    }

    public void toggleAutomaticMode() {
        automaticMode = !automaticMode;
        markChangedAndSync();
        scheduleCheck();
    }

    public ResourceHandler<FluidResource> fluidHandler() {
        return fluidHandler;
    }

    public void transferExperience(
            ServerPlayer player,
            ArcaneInfusionTransferAction action,
            int requestedLevels
    ) {
        if (level == null || level.isClientSide()) {
            return;
        }
        int points = switch (action) {
            case DEPOSIT -> service.depositLevels(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience,
                    EXPERIENCE_CAPACITY,
                    requestedLevels
            );
            case DEPOSIT_ALL -> service.depositAll(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience,
                    EXPERIENCE_CAPACITY
            );
            case WITHDRAW -> service.withdrawLevels(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience,
                    requestedLevels
            );
            case WITHDRAW_ALL -> service.withdrawAll(
                    player.experienceLevel,
                    player.experienceProgress,
                    storedExperience
            );
        };
        if (points <= 0) {
            return;
        }
        if (action == ArcaneInfusionTransferAction.WITHDRAW
                || action == ArcaneInfusionTransferAction.WITHDRAW_ALL) {
            storedExperience = Math.max(0, storedExperience - points);
            player.giveExperiencePoints(points);
        } else {
            storedExperience = (int) Math.min(
                    EXPERIENCE_CAPACITY,
                    (long) storedExperience + points
            );
            player.giveExperiencePoints(-points);
        }
        experienceChanged();
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
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot,
            @NonNull ItemStack stack,
            @Nullable Direction direction
    ) {
        return slot < INPUT_SLOT_COUNT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot,
            @NonNull ItemStack stack,
            @NonNull Direction direction
    ) {
        return automaticMode && slot == OUTPUT_SLOT;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        setStoredExperienceRaw(input.getIntOr(STORED_EXPERIENCE_TAG, 0));
        automaticMode = input.getBooleanOr(AUTOMATIC_MODE_TAG, false);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        if (storedExperience > 0) {
            output.putInt(STORED_EXPERIENCE_TAG, storedExperience);
        }
        if (automaticMode) {
            output.putBoolean(AUTOMATIC_MODE_TAG, true);
        }
    }

    @Override
    protected void clearContentsForBlockDrop() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        storedExperience = 0;
        invalidatePreview();
        setChanged();
    }

    private void contentsChanged() {
        invalidatePreview();
        markChangedAndSync();
        scheduleCheck();
    }

    private void scheduleCheck() {
        if (automaticMode && level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    private void setStoredExperienceRaw(int value) {
        int clamped = Math.clamp(value, 0, EXPERIENCE_CAPACITY);
        storedExperience = clamped;
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
        cachedPreviewResult = ItemStack.EMPTY;
        ArcaneInfusionInput input = new ArcaneInfusionInput(List.copyOf(items.subList(0, INPUT_SLOT_COUNT)));
        Optional<RecipeHolder<ArcaneInfusionRecipe>> match = recipeCheck.getRecipeFor(input, serverLevel);
        if (match.isEmpty()) {
            return;
        }
        ArcaneInfusionRecipe recipe = match.get().value();
        ItemStack result = recipe.result();
        if (!result.isEmpty()) {
            cachedPreviewRecipe = recipe;
            cachedPreviewResult = result;
        }
    }

    private void invalidatePreview() {
        previewDirty = true;
        cachedPreviewRecipe = null;
        cachedPreviewResult = ItemStack.EMPTY;
    }

    private void experienceChanged() {
        markChangedAndSync();
        scheduleCheck();
    }

    private boolean hasRequiredExperience(ArcaneInfusionRecipe recipe) {
        return service.evaluate(new ArcaneInfusionAttempt(
                true,
                true,
                storedExperience,
                recipe.experience()
        )) == ArcaneInfusionDecision.READY;
    }

    private void consume(ArcaneInfusionRecipe recipe) {
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            items.get(slot).shrink(recipe.ingredient(slot).count());
            if (items.get(slot).isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
        storedExperience -= recipe.experience();
    }

    private static boolean sameResult(ItemStack current, ItemStack expected) {
        return expected.getCount() > 0
                && expected.getCount() <= current.getCount()
                && ItemStack.isSameItemSameComponents(current, expected);
    }

    private void playCompletionFeedback(ServerLevel serverLevel, ArcaneInfusionRecipe recipe) {
        boolean farmer = recipe.enchantment().unwrapKey()
                .map(key -> "farmers_touch".equals(key.identifier().getPath()))
                .orElse(false);
        serverLevel.sendParticles(
                farmer ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SCULK_SOUL,
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
