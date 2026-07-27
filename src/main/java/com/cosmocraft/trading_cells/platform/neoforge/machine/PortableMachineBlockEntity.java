package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class PortableMachineBlockEntity extends BlockEntity {
    private @Nullable CompoundTag preparedBlockDropData;

    protected PortableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Server-side lifecycle entry point invoked by the owning block adapter. */
    public abstract void processTick();

    public final void prepareForBlockDrop(HolderLookup.Provider registries) {
        beforeBlockDropSnapshot();
        preparedBlockDropData = saveCustomOnly(registries);
        clearContentsForBlockDrop();
    }

    public final CompoundTag getPreparedBlockDropData(HolderLookup.Provider registries) {
        return preparedBlockDropData == null ? saveCustomOnly(registries) : preparedBlockDropData.copy();
    }

    public final void discardContentsAfterBlockDrop() {
        preparedBlockDropData = null;
        clearContentsForBlockDrop();
    }

    protected void beforeBlockDropSnapshot() {
    }

    protected abstract void clearContentsForBlockDrop();

    protected final void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
}
