package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.application.port.input.PiglinBarterUseCase;
import com.cosmocraft.trading_cells.feature.trader.domain.model.PiglinBarterCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class PiglinBarteringCellBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final String PIGLIN_DATA_TAG = "StoredPiglin";
    private static final String GOLD_BUFFER_TAG = "GoldBuffer";
    private static final String OUTPUT_BUFFER_TAG = "OutputBuffer";
    private static final String BARTER_TICKS_TAG = "BarterTicksRemaining";
    private static final int GOLD_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int[] TOP_SLOTS = new int[]{GOLD_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT};
    private static final int[] NO_SLOTS = new int[0];

    private final PiglinBarterUseCase barterService = FeatureComposition.piglinBarter();
    private @Nullable CompoundTag storedPiglinData;
    private ItemStack goldBuffer = ItemStack.EMPTY;
    private ItemStack outputBuffer = ItemStack.EMPTY;
    private int barterTicksRemaining = 0;
    private @Nullable CompoundTag preparedBlockDropData;

    public PiglinBarteringCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(), pos, blockState);
    }

    void processTick(Level level) {
        if (!needsServerTick()) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            tickBarter(serverLevel);
        }
    }

    public boolean hasPiglin() {
        return storedPiglinData != null && !storedPiglinData.isEmpty();
    }

    public boolean hasOutput() {
        return !outputBuffer.isEmpty();
    }

    public boolean isBartering() {
        return barterTicksRemaining > 0;
    }

    private boolean needsServerTick() {
        return isBartering()
                || (!goldBuffer.isEmpty()
                && goldBuffer.is(Items.GOLD_INGOT)
                && outputBuffer.isEmpty()
                && hasPiglin()
                && storedPiglinData != null
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, storedPiglinData));
    }

    public @NonNull ItemStack copyOutputStack() {
        return outputBuffer.copy();
    }

    public @Nullable CompoundTag copyPiglinData() {
        return hasPiglin() && storedPiglinData != null ? storedPiglinData.copy() : null;
    }

    public @Nullable Piglin createPiglinForDisplay() {
        if (level == null || !hasPiglin() || storedPiglinData == null) {
            return null;
        }

        Piglin piglin = CapturedMobStackAdapter.createPiglin(level, storedPiglinData, worldPosition);
        if (piglin != null && isBartering()) {
            piglin.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLD_INGOT));
        }
        return piglin;
    }

    public InteractionResult insertPiglinFromCapturer(ItemStack stack) {
        if (hasPiglin()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        CompoundTag piglinData = CapturedMobStackAdapter.copyData(CapturedMobKind.PIGLIN, stack);
        if (piglinData == null) {
            return InteractionResult.PASS;
        }
        if (CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, piglinData)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        storedPiglinData = piglinData.copy();
        CapturedMobStackAdapter.clearData(CapturedMobKind.PIGLIN, stack);
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractPiglinToCapturer(ItemStack stack, Player player) {
        if (!hasPiglin()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.PIGLIN, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        CompoundTag piglinData = copyPiglinData();
        if (piglinData == null) {
            return InteractionResult.FAIL;
        }

        ItemStack targetStack = createSingleCapturerTarget(stack);
        CapturedMobStackAdapter.setData(CapturedMobKind.PIGLIN, targetStack, piglinData);
        finishStackedExtraction(player, stack, targetStack);
        clearStoredPiglin();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult barterGold(ItemStack goldStack) {
        if (goldStack.isEmpty() || !goldStack.is(Items.GOLD_INGOT)) {
            return InteractionResult.PASS;
        }

        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        BarterResult result = tryStartBarter();
        if (result == BarterResult.SUCCESS) {
            goldStack.shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractOutputToPlayer(Player player) {
        if (outputBuffer.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!canPlayerInventoryFitEntireStack(player, outputBuffer)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack toInsert = outputBuffer.copy();
        boolean inserted = player.getInventory().add(toInsert);
        if (!inserted || !toInsert.isEmpty()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        outputBuffer = ItemStack.EMPTY;
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean canPlayerInventoryFitEntireStack(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        int remaining = stack.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inventoryStack = player.getInventory().getItem(i);
            if (inventoryStack.isEmpty()) {
                remaining -= Math.min(stack.getMaxStackSize(), stack.getCount());
            } else if (ItemStack.isSameItemSameComponents(inventoryStack, stack)) {
                remaining -= Math.max(0, inventoryStack.getMaxStackSize() - inventoryStack.getCount());
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private void tickBarter(ServerLevel serverLevel) {
        if (isBartering()) {
            PiglinBarterCycle.Step step = barterService.advance(barterTicksRemaining, false);
            barterTicksRemaining = step.ticksRemaining();
            if (step.transition() == PiglinBarterCycle.Transition.COMPLETED) {
                finishBarter(serverLevel);
                markChangedAndSync();
            } else {
                setChanged();
            }
            return;
        }

        if (!goldBuffer.isEmpty() && goldBuffer.is(Items.GOLD_INGOT) && outputBuffer.isEmpty()) {
            BarterResult result = tryStartBarter();
            if (result == BarterResult.SUCCESS) {
                goldBuffer.shrink(1);
                if (goldBuffer.isEmpty()) {
                    goldBuffer = ItemStack.EMPTY;
                }
                setChanged();
            }
        }
    }

    private BarterResult tryStartBarter() {
        if (isBartering()) {
            return BarterResult.BUSY;
        }

        if (!outputBuffer.isEmpty()) {
            return BarterResult.OUTPUT_PENDING;
        }

        if (!hasPiglin()) {
            return BarterResult.EMPTY_CELL;
        }

        if (storedPiglinData == null) {
            return BarterResult.INVALID_PIGLIN;
        }

        if (CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, storedPiglinData)) {
            return BarterResult.BABY_PIGLIN;
        }

        PiglinBarterCycle.Step step = barterService.advance(0, true);
        barterTicksRemaining = step.ticksRemaining();
        markChangedAndSync();
        return BarterResult.SUCCESS;
    }

    private void finishBarter(ServerLevel serverLevel) {
        if (!hasPiglin()
                || storedPiglinData == null
                || CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, storedPiglinData)) {
            return;
        }

        Piglin piglin = CapturedMobStackAdapter.createPiglin(serverLevel, storedPiglinData, worldPosition);
        if (piglin == null) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }
        LootTable lootTable = server.reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
        LootParams lootParams = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, piglin)
                .create(LootContextParamSets.PIGLIN_BARTER);
        List<ItemStack> barteredItems = lootTable.getRandomItems(lootParams);

        for (ItemStack result : barteredItems) {
            storeBarterResult(result.copy());
        }
    }

    private void storeBarterResult(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (outputBuffer.isEmpty()) {
            outputBuffer = stack.copy();
            return;
        }

        if (ItemStack.isSameItemSameComponents(outputBuffer, stack) && outputBuffer.getCount() < outputBuffer.getMaxStackSize()) {
            int room = outputBuffer.getMaxStackSize() - outputBuffer.getCount();
            int moved = Math.min(room, stack.getCount());
            outputBuffer.grow(moved);
            stack.shrink(moved);
        }
    }


    public void prepareForBlockDrop(HolderLookup.Provider registries) {
        preparedBlockDropData = saveCustomOnly(registries);
        clearStoredContents();
    }

    public CompoundTag getPreparedBlockDropData(HolderLookup.Provider registries) {
        return preparedBlockDropData == null ? saveCustomOnly(registries) : preparedBlockDropData.copy();
    }

    public void discardContentsAfterBlockDrop() {
        preparedBlockDropData = null;
        clearStoredContents();
    }

    private void clearStoredContents() {
        storedPiglinData = null;
        goldBuffer = ItemStack.EMPTY;
        outputBuffer = ItemStack.EMPTY;
        barterTicksRemaining = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        storedPiglinData = input.read(PIGLIN_DATA_TAG, CompoundTag.CODEC).orElse(null);
        if (storedPiglinData == null || storedPiglinData.isEmpty()) {
            storedPiglinData = null;
        }

        goldBuffer = input.read(GOLD_BUFFER_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!goldBuffer.is(Items.GOLD_INGOT)) {
            goldBuffer = ItemStack.EMPTY;
        }
        if (goldBuffer.getCount() > 1) {
            goldBuffer.setCount(1);
        }

        outputBuffer = input.read(OUTPUT_BUFFER_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        barterTicksRemaining = Math.max(0, input.getIntOr(BARTER_TICKS_TAG, 0));
        preparedBlockDropData = null;
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        if (storedPiglinData != null && !storedPiglinData.isEmpty()) {
            output.store(PIGLIN_DATA_TAG, CompoundTag.CODEC, storedPiglinData);
        }
        if (!goldBuffer.isEmpty()) {
            output.store(GOLD_BUFFER_TAG, ItemStack.CODEC, goldBuffer);
        }
        if (!outputBuffer.isEmpty()) {
            output.store(OUTPUT_BUFFER_TAG, ItemStack.CODEC, outputBuffer);
        }
        if (barterTicksRemaining > 0) {
            output.putInt(BARTER_TICKS_TAG, barterTicksRemaining);
        }
    }

    @Override
    public @NonNull Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveCustomOnly(registries);
    }


    private static ItemStack createSingleCapturerTarget(ItemStack heldStack) {
        if (heldStack.getCount() <= 1) {
            return heldStack;
        }
        return new ItemStack(heldStack.getItem());
    }

    private static void finishStackedExtraction(Player player, ItemStack heldStack, ItemStack targetStack) {
        if (targetStack == heldStack) {
            return;
        }

        heldStack.shrink(1);
        if (!player.getInventory().add(targetStack)) {
            player.drop(targetStack, false);
        }
    }

    private void clearStoredPiglin() {
        storedPiglinData = null;
        markChangedAndSync();
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return goldBuffer.isEmpty() && outputBuffer.isEmpty();
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        if (slot == GOLD_SLOT) {
            return goldBuffer;
        }
        if (slot == OUTPUT_SLOT) {
            return outputBuffer;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        if (slot == GOLD_SLOT && !goldBuffer.isEmpty()) {
            ItemStack removed = goldBuffer.split(count);
            if (goldBuffer.isEmpty()) {
                goldBuffer = ItemStack.EMPTY;
            }
            setChanged();
            return removed;
        }

        if (slot == OUTPUT_SLOT && !outputBuffer.isEmpty()) {
            ItemStack removed = outputBuffer.split(count);
            if (outputBuffer.isEmpty()) {
                outputBuffer = ItemStack.EMPTY;
            }
            markChangedAndSync();
            return removed;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        if (slot == GOLD_SLOT) {
            ItemStack removed = goldBuffer;
            goldBuffer = ItemStack.EMPTY;
            return removed;
        }

        if (slot == OUTPUT_SLOT) {
            ItemStack removed = outputBuffer;
            outputBuffer = ItemStack.EMPTY;
            return removed;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        if (slot == GOLD_SLOT) {
            if (stack.isEmpty()) {
                goldBuffer = ItemStack.EMPTY;
            } else if (canPlaceItem(slot, stack)) {
                goldBuffer = stack.copy();
                goldBuffer.setCount(1);
            }
            setChanged();
            return;
        }

        if (slot == OUTPUT_SLOT) {
            outputBuffer = stack.copy();
            setChanged();
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, @NonNull ItemStack stack) {
        return slot == GOLD_SLOT
                && stack.is(Items.GOLD_INGOT)
                && !isBartering()
                && outputBuffer.isEmpty()
                && goldBuffer.isEmpty()
                && hasPiglin()
                && storedPiglinData != null
                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.PIGLIN, storedPiglinData);
    }

    @Override
    public void clearContent() {
        goldBuffer = ItemStack.EMPTY;
        outputBuffer = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int @NonNull [] getSlotsForFace(@NonNull Direction direction) {
        if (direction == Direction.UP) {
            return TOP_SLOTS;
        }
        if (direction == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace( // NOSONAR - Minecraft explicitly permits a null automation side.
            int slot,
            @NonNull ItemStack stack,
            @Nullable Direction direction
    ) {
        return direction == Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NonNull ItemStack stack, @NonNull Direction direction) {
        return direction == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    private enum BarterResult {
        SUCCESS,
        BUSY,
        OUTPUT_PENDING,
        EMPTY_CELL,
        BABY_PIGLIN,
        INVALID_PIGLIN,
        FAILED
    }
}
