package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederRecipe;
import com.cosmocraft.trading_cells.feature.breeders.application.port.input.BreederUseCase;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
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

public abstract class BreederBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int FOOD_SLOT = 0;
    public static final int PARENT_A_SLOT = 1;
    public static final int PARENT_B_SLOT = 2;
    public static final int BABY_PREVIEW_SLOT = 3;
    public static final int EMPTY_CAPTURER_SLOT = 4;
    public static final int FILLED_CAPTURER_SLOT = 5;
    public static final int CONTAINER_SIZE = 6;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String BREED_TICKS_TAG = "BreedTicks";
    private static final String PENDING_BABIES_TAG = "PendingBabies";
    private static final String BABY_TEMPLATE_TAG = "BabyTemplate";
    private static final String VILLAGER_VARIANT_TAG = "VillagerVariant";
    private static final String ACTIVE_FOOD_TAG = "ActiveFood";

    // Automation contract: food enters from above, adult/empty capturers
    // enter from either horizontal side, and the captured baby leaves below.
    private static final int[] TOP_SLOTS = new int[]{FOOD_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{PARENT_A_SLOT, PARENT_B_SLOT, EMPTY_CAPTURER_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{FILLED_CAPTURER_SLOT};
    private static final int[] NO_SLOTS = new int[0];

    private final BreederKind kind;
    private final BreederUseCase breederService = FeatureComposition.breeder();
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int breedTicks;
    private int pendingBabies;
    private int villagerVariant;
    private BreederFood activeFood = BreederFood.NONE;
    private @Nullable CompoundTag babyTemplate;
    private @Nullable CompoundTag preparedBlockDropData;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> breedTicks;
                case 1 -> pendingBabies;
                case 2 -> villagerVariant;
                case 3 -> breederService.durationTicks(kind);
                case 4 -> breederService.foodCost(kind, BreederFood.BREAD);
                case 5 -> breederService.foodCost(kind, BreederFood.VEGETABLE);
                case 6 -> activeFood.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                breedTicks = value;
            } else if (index == 1) {
                pendingBabies = Math.clamp(value, 0, breederService.maximumPendingBabies());
            } else if (index == 2 && kind == BreederKind.VILLAGER) {
                int normalized = VillagerVariantSelection.normalize(value);
                if (villagerVariant != normalized) {
                    villagerVariant = normalized;
                    markChangedAndSync();
                }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    protected BreederBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, BreederKind kind) {
        super(type, pos, blockState);
        this.kind = kind;
    }

    public BreederKind kind() {
        return kind;
    }

    public int getBreedTicks() {
        return breedTicks;
    }

    public int getPendingBabies() {
        return pendingBabies;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    void processTick() {
        processAutomation();
        processBreedingProgress();
    }

    private void processAutomation() {
        if (pendingBabies <= 0 || !getItem(FILLED_CAPTURER_SLOT).isEmpty()) {
            return;
        }

        ItemStack capturers = getItem(EMPTY_CAPTURER_SLOT);
        if (!isEmptyCapturer(capturers)) {
            return;
        }

        CompoundTag babyData = createBabyDataForOutput();
        if (babyData == null) {
            return;
        }

        ItemStack output = capturers.copy();
        output.setCount(1);
        setCapturedData(output, babyData);
        capturers.shrink(1);
        if (capturers.isEmpty()) {
            items.set(EMPTY_CAPTURER_SLOT, ItemStack.EMPTY);
        }
        items.set(FILLED_CAPTURER_SLOT, output);
        pendingBabies--;
        if (pendingBabies <= 0) {
            pendingBabies = 0;
        }
        markChangedAndSync();
    }

    private void processBreedingProgress() {
        if (breedTicks == 0 && activeFood == BreederFood.NONE) {
            activeFood = eligibleFood();
        }
        int previousTicks = breedTicks;
        TimedProcess.Step step = breederService.advance(
                breedTicks,
                kind,
                canGenerateBaby(activeFood)
        );
        breedTicks = step.ticks();
        if (step.transition() == TimedProcess.Transition.IDLE
                || step.transition() == TimedProcess.Transition.PAUSED) {
            return;
        }
        if (step.transition() == TimedProcess.Transition.RESET) {
            activeFood = BreederFood.NONE;
            if (previousTicks != 0) {
                markChangedAndSync();
            }
            return;
        }
        if (step.transition() == TimedProcess.Transition.ADVANCED) {
            setChanged();
            return;
        }

        consumeFoodCost(activeFood);
        activeFood = BreederFood.NONE;
        babyTemplate = createBabyTemplateFromParents();
        pendingBabies = Math.min(breederService.maximumPendingBabies(), pendingBabies + 1);
        markChangedAndSync();
    }

    private BreederFood eligibleFood() {
        if (pendingBabies != 0
                || !isValidAdultParent(getItem(PARENT_A_SLOT))
                || !isValidAdultParent(getItem(PARENT_B_SLOT))) {
            return BreederFood.NONE;
        }
        ItemStack food = getItem(FOOD_SLOT);
        BreederFood candidate = MinecraftBreederFood.from(kind, food);
        return hasFoodCost(food, candidate) ? candidate : BreederFood.NONE;
    }

    private boolean canGenerateBaby(BreederFood recipe) {
        return pendingBabies == 0
                && isValidAdultParent(getItem(PARENT_A_SLOT))
                && isValidAdultParent(getItem(PARENT_B_SLOT))
                && hasFoodCost(getItem(FOOD_SLOT), recipe);
    }

    private boolean hasFoodCost(ItemStack food, BreederFood recipe) {
        return recipe != BreederFood.NONE
                && MinecraftBreederFood.from(kind, food) == recipe
                && BreederRecipe.isFood(kind, recipe)
                && food.getCount() >= breederService.foodCost(kind, recipe);
    }

    private void consumeFoodCost(BreederFood recipe) {
        ItemStack food = getItem(FOOD_SLOT);
        if (hasFoodCost(food, recipe)) {
            food.shrink(breederService.foodCost(kind, recipe));
            if (food.isEmpty()) {
                items.set(FOOD_SLOT, ItemStack.EMPTY);
            }
        }
    }

    private @Nullable CompoundTag createBabyDataForOutput() {
        if (babyTemplate != null && !babyTemplate.isEmpty()) {
            return makeBabyData(babyTemplate);
        }
        return createBabyTemplateFromParents();
    }

    private @Nullable CompoundTag createBabyTemplateFromParents() {
        CompoundTag parentData = getParentData(getItem(PARENT_A_SLOT));
        if (parentData == null) {
            parentData = getParentData(getItem(PARENT_B_SLOT));
        }
        if (parentData == null) {
            return null;
        }
        return makeBabyData(parentData);
    }

    private CompoundTag makeBabyData(CompoundTag source) {
        CompoundTag baby = source.copy();
        baby.remove("UUID");
        baby.remove("CustomName");
        baby.remove("LoveCause");
        baby.remove("LoveCauseLeast");
        baby.remove("LoveCauseMost");
        baby.remove("AgeLocked");
        baby.putInt("Age", -24000);
        if (kind == BreederKind.VILLAGER) {
            CompoundTag villagerData = baby.getCompound("VillagerData").map(CompoundTag::copy).orElseGet(CompoundTag::new);
            villagerData.putString("type", VillagerVariantSelection.id(villagerVariant));
            baby.put("VillagerData", villagerData);
        } else {
            baby.putBoolean("IsBaby", true);
            // Babies produced by the breeder must always be completely unarmed.
            baby.remove("HandItems");
            baby.remove("ArmorItems");
            baby.remove("HandDropChances");
            baby.remove("ArmorDropChances");
            baby.remove("equipment");
        }
        return baby;
    }

    public ItemStack createBabyPreviewStack() {
        if (pendingBabies <= 0) {
            return ItemStack.EMPTY;
        }
        CompoundTag babyData = createBabyDataForOutput();
        if (babyData == null) {
            return ItemStack.EMPTY;
        }
        ItemStack preview = new ItemStack(capturerItem());
        setCapturedData(preview, babyData);
        preview.setCount(Math.min(pendingBabies, preview.getMaxStackSize()));
        return preview;
    }

    private @Nullable CompoundTag getParentData(ItemStack stack) {
        return CapturedMobStackAdapter.copyData(capturedKind(), stack);
    }

    private boolean isValidAdultParent(ItemStack stack) {
        if (!CapturedMobStackAdapter.isCapturer(capturedKind(), stack)) {
            return false;
        }
        CompoundTag data = getParentData(stack);
        if (data == null) {
            return false;
        }
        return !CapturedMobStackAdapter.isBaby(capturedKind(), data);
    }

    private boolean isEmptyCapturer(ItemStack stack) {
        if (!CapturedMobStackAdapter.isCapturer(capturedKind(), stack)) {
            return false;
        }
        return !CapturedMobStackAdapter.isFilledCapturer(capturedKind(), stack);
    }

    private net.minecraft.world.item.Item capturerItem() {
        return CapturedMobStackAdapter.capturerItem(capturedKind());
    }

    private void setCapturedData(ItemStack stack, CompoundTag data) {
        CapturedMobStackAdapter.setData(capturedKind(), stack, data);
    }

    private CapturedMobKind capturedKind() {
        return kind == BreederKind.VILLAGER
                ? CapturedMobKind.VILLAGER
                : CapturedMobKind.PIGLIN;
    }


    public void prepareForBlockDrop(HolderLookup.Provider registries) {
        preparedBlockDropData = saveCustomOnly(registries);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (slot != BABY_PREVIEW_SLOT) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
        breedTicks = 0;
        pendingBabies = 0;
        activeFood = BreederFood.NONE;
        babyTemplate = null;
        setChanged();
    }

    public CompoundTag getPreparedBlockDropData(HolderLookup.Provider registries) {
        if (preparedBlockDropData != null) {
            return preparedBlockDropData.copy();
        }
        return saveCustomOnly(registries);
    }

    public void discardContentsAfterBlockDrop() {
        preparedBlockDropData = null;
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (slot != BABY_PREVIEW_SLOT) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
        breedTicks = 0;
        pendingBabies = 0;
        activeFood = BreederFood.NONE;
        babyTemplate = null;
        setChanged();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable(kind == BreederKind.VILLAGER
                ? "container.trading_cells.villager_breeder"
                : "container.trading_cells.piglin_breeder");
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(
            int containerId,
            @NonNull Inventory inventory,
            @NonNull Player player
    ) {
        return new BreederMenu(kind, containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        if (pendingBabies > 0) {
            return false;
        }
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (i != BABY_PREVIEW_SLOT && !items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        if (slot == BABY_PREVIEW_SLOT) {
            return createBabyPreviewStack();
        }
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (!isValidSlot(slot) || slot == BABY_PREVIEW_SLOT || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(count);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        markChangedAndSync();
        return removed;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot) || slot == BABY_PREVIEW_SLOT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (!isValidSlot(slot) || slot == BABY_PREVIEW_SLOT) {
            return;
        }
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        items.set(slot, stack.copy());
        if (!items.get(slot).isEmpty() && items.get(slot).getCount() > getMaxStackSize()) {
            items.get(slot).setCount(getMaxStackSize());
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
            case FOOD_SLOT -> BreederRecipe.isFood(kind, MinecraftBreederFood.from(kind, stack));
            case PARENT_A_SLOT, PARENT_B_SLOT -> isValidAdultParent(stack);
            case EMPTY_CAPTURER_SLOT -> isEmptyCapturer(stack);
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (slot != BABY_PREVIEW_SLOT) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
        breedTicks = 0;
        pendingBabies = 0;
        activeFood = BreederFood.NONE;
        babyTemplate = null;
        preparedBlockDropData = null;
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
    public boolean canPlaceItemThroughFace( // NOSONAR - Minecraft explicitly permits a null automation side.
            int slot,
            @NonNull ItemStack stack,
            @Nullable Direction direction
    ) {
        if (direction == Direction.UP) {
            return slot == FOOD_SLOT && canPlaceItem(slot, stack);
        }
        if (direction != null && direction.getAxis().isHorizontal()) {
            return (slot == PARENT_A_SLOT || slot == PARENT_B_SLOT || slot == EMPTY_CAPTURER_SLOT) && canPlaceItem(slot, stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && slot == FILLED_CAPTURER_SLOT;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (slot == BABY_PREVIEW_SLOT) {
                items.set(slot, ItemStack.EMPTY);
            } else {
                items.set(slot, input.read(SLOT_TAG_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            }
        }
        breedTicks = Math.max(0, input.getIntOr(BREED_TICKS_TAG, 0));
        pendingBabies = Math.clamp(
                input.getIntOr(PENDING_BABIES_TAG, 0),
                0,
                breederService.maximumPendingBabies()
        );
        babyTemplate = input.read(BABY_TEMPLATE_TAG, CompoundTag.CODEC).orElse(null);
        villagerVariant = VillagerVariantSelection.normalize(input.getIntOr(VILLAGER_VARIANT_TAG, 0));
        activeFood = BreederFood.fromName(input.getStringOr(ACTIVE_FOOD_TAG, BreederFood.NONE.name()));
        preparedBlockDropData = null;
        if (breedTicks == 0 || !BreederRecipe.isFood(kind, activeFood)) {
            activeFood = BreederFood.NONE;
        }
        if (babyTemplate != null && babyTemplate.isEmpty()) {
            babyTemplate = null;
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (slot != BABY_PREVIEW_SLOT && !items.get(slot).isEmpty()) {
                output.store(SLOT_TAG_PREFIX + slot, ItemStack.CODEC, items.get(slot));
            }
        }
        if (breedTicks > 0) {
            output.putInt(BREED_TICKS_TAG, breedTicks);
        }
        if (pendingBabies > 0) {
            output.putInt(PENDING_BABIES_TAG, pendingBabies);
        }
        if (babyTemplate != null && !babyTemplate.isEmpty()) {
            output.store(BABY_TEMPLATE_TAG, CompoundTag.CODEC, babyTemplate);
        }
        if (kind == BreederKind.VILLAGER && villagerVariant != 0) {
            output.putInt(VILLAGER_VARIANT_TAG, villagerVariant);
        }
        if (breedTicks > 0 && activeFood != BreederFood.NONE) {
            output.putString(ACTIVE_FOOD_TAG, activeFood.name());
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

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
