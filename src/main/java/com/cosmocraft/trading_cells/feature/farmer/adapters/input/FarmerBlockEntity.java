package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.application.port.input.FarmerUseCase;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.shared.machines.domain.model.TimedProcess;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FarmerBlockEntity extends PortableMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int VILLAGER_SLOT = 0;
    public static final int CROP_SLOT = 1;
    public static final int HOE_SLOT = 2;
    public static final int FIRST_OUTPUT_SLOT = 3;
    public static final int OUTPUT_SLOT_COUNT = 4;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;

    private static final String SLOT_TAG_PREFIX = "Slot";
    private static final String GROWTH_TICKS_TAG = "GrowthTicks";
    private static final String GROWTH_DURATION_TICKS_TAG = "GrowthDurationTicks";
    private static final int[] TOP_SLOTS = new int[]{CROP_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{VILLAGER_SLOT, HOE_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{3, 4, 5, 6};
    private static final int[] NO_SLOTS = new int[0];
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final FarmerUseCase farmerService = FeatureComposition.farmer();
    private int growthTicks;
    private int growthDurationTicks = farmerService.effectiveGrowthTicks(0.0D, 0);
    private boolean cultivating;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> growthTicks;
                case 1 -> effectiveGrowthTicks();
                case 2 -> cultivating ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                growthDurationTicks = effectiveGrowthTicks();
                growthTicks = Math.clamp(value, 0, growthDurationTicks);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public FarmerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmerRegistrationAdapter.FARMER_BLOCK_ENTITY.get(), pos, state);
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
        return FarmerCropStackAdapter.from(items.get(CROP_SLOT));
    }

    @Override
    public void processTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        FarmerCrop crop = crop();
        boolean canCultivate = isAdultVillager(items.get(VILLAGER_SLOT))
                && crop != FarmerCrop.NONE;
        int maximumGrowthTicks = updateGrowthDuration();
        FarmerHarvest harvest = farmerService.harvest(crop, fortuneLevel());
        boolean outputAvailable = canCultivate && canStoreHarvest(crop, harvest);
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

        storeOutput(FarmerCropStackAdapter.produce(crop, harvest.produceCount()));
        storeOutput(FarmerCropStackAdapter.seeds(crop, harvest.seedCount()));
        damageHoeAfterHarvest();
        markChangedAndSync();
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.trading_cells.farmer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NonNull Inventory inventory, @NonNull Player player) {
        return new FarmerMenu(containerId, inventory, this, dataAccess);
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
        if (slot == VILLAGER_SLOT || (slot == CROP_SLOT && items.get(slot).isEmpty())) {
            growthTicks = 0;
            cultivating = false;
        } else if (slot == HOE_SLOT) {
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
        if (slot == VILLAGER_SLOT || slot == CROP_SLOT) {
            growthTicks = 0;
            cultivating = false;
        } else if (slot == HOE_SLOT) {
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
        FarmerCrop previousCrop = crop();
        updateGrowthDuration();
        ItemStack inserted = stack.copy();
        int max = slot == VILLAGER_SLOT || slot == CROP_SLOT || slot == HOE_SLOT
                ? 1
                : Math.min(64, inserted.getMaxStackSize());
        inserted.setCount(Math.min(max, inserted.getCount()));
        items.set(slot, inserted);
        if (slot == VILLAGER_SLOT || (slot == CROP_SLOT && previousCrop != crop())) {
            growthTicks = 0;
            cultivating = false;
        } else if (slot == HOE_SLOT) {
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
            case VILLAGER_SLOT -> isAdultVillager(stack);
            case CROP_SLOT -> FarmerCropStackAdapter.isSupported(stack)
                    && items.get(CROP_SLOT).isEmpty();
            case HOE_SLOT -> stack.getItem() instanceof HoeItem;
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
                        farmerService.effectiveGrowthTicks(0.0D, 0)
                )
        );
        growthTicks = Math.clamp(storedGrowthTicks, 0, growthDurationTicks);
        cultivating = false;
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
        growthDurationTicks = farmerService.effectiveGrowthTicks(0.0D, 0);
        cultivating = false;
        setChanged();
    }

    private int fortuneLevel() {
        ItemStack hoe = items.get(HOE_SLOT);
        if (hoe.isEmpty() || level == null) {
            return 0;
        }
        var fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return hoe.getEnchantmentLevel(fortune);
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
        updateGrowthDuration();
    }

    private int efficiencyLevel() {
        ItemStack hoe = items.get(HOE_SLOT);
        if (hoe.isEmpty() || level == null) {
            return 0;
        }
        var efficiency = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
        return Math.max(0, hoe.getEnchantmentLevel(efficiency));
    }

    private int effectiveGrowthTicks() {
        return farmerService.effectiveGrowthTicks(
                hoeMiningSpeed(),
                efficiencyLevel()
        );
    }

    private float hoeMiningSpeed() {
        ItemStack hoe = items.get(HOE_SLOT);
        if (hoe.isEmpty()) {
            return 0.0F;
        }

        Tool tool = hoe.get(DataComponents.TOOL);
        if (tool == null) {
            return 0.0F;
        }

        float fastestRuleSpeed = Math.max(0.0F, tool.defaultMiningSpeed());
        for (Tool.Rule rule : tool.rules()) {
            if (rule.speed().isEmpty()) {
                continue;
            }
            float ruleSpeed = Math.max(0.0F, rule.speed().orElse(0.0F));
            if (rule.blocks().unwrapKey().filter(BlockTags.MINEABLE_WITH_HOE::equals).isPresent()) {
                return ruleSpeed;
            }
            fastestRuleSpeed = Math.max(fastestRuleSpeed, ruleSpeed);
        }
        return fastestRuleSpeed;
    }

    private int updateGrowthDuration() {
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

    private boolean canStoreHarvest(FarmerCrop crop, FarmerHarvest harvest) {
        NonNullList<ItemStack> simulated = NonNullList.withSize(OUTPUT_SLOT_COUNT, ItemStack.EMPTY);
        for (int index = 0; index < OUTPUT_SLOT_COUNT; index++) {
            simulated.set(index, items.get(FIRST_OUTPUT_SLOT + index).copy());
        }
        return merge(simulated, FarmerCropStackAdapter.produce(crop, harvest.produceCount()))
                && merge(simulated, FarmerCropStackAdapter.seeds(crop, harvest.seedCount()));
    }

    private void storeOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NonNullList<ItemStack> outputs = NonNullList.withSize(OUTPUT_SLOT_COUNT, ItemStack.EMPTY);
        for (int index = 0; index < OUTPUT_SLOT_COUNT; index++) {
            outputs.set(index, items.get(FIRST_OUTPUT_SLOT + index));
        }
        merge(outputs, stack);
        for (int index = 0; index < OUTPUT_SLOT_COUNT; index++) {
            items.set(FIRST_OUTPUT_SLOT + index, outputs.get(index));
        }
    }

    private static boolean merge(NonNullList<ItemStack> outputs, ItemStack source) {
        if (source.isEmpty()) {
            return true;
        }
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
            if (outputs.get(index).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                outputs.set(index, inserted);
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAdultVillager(ItemStack stack) {
        return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < CONTAINER_SIZE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
