package com.cosmocraft.trading_cells.platform.neoforge.machine;

import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineRedstoneMode;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class PortableMachineBlockEntity extends BlockEntity
        implements MachineDiagnosticSource, MachineConfigurationPort {
    private static final String REDSTONE_MODE_TAG = "MachineRedstoneMode";

    private @Nullable CompoundTag preparedBlockDropData;
    private MachineRedstoneMode redstoneMode = MachineRedstoneMode.IGNORE;
    private int lastComparatorOutput = -1;

    protected PortableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Server-side lifecycle entry point invoked by the owning block adapter. */
    public abstract void processTick();

    /** Applies common pause rules before invoking feature-owned processing. */
    public final void processServerTick() {
        if (!isPausedByRedstone()) {
            processTick();
        }
    }

    public boolean supportsRedstoneControl() {
        return true;
    }

    public final MachineRedstoneMode redstoneMode() {
        return supportsRedstoneControl() ? redstoneMode : MachineRedstoneMode.IGNORE;
    }

    public final boolean isPausedByRedstone() {
        return supportsRedstoneControl()
                && level != null
                && redstoneMode.pauses(level.hasNeighborSignal(worldPosition));
    }

    public final void setRedstoneMode(MachineRedstoneMode mode) {
        MachineRedstoneMode sanitized = supportsRedstoneControl() && mode != null
                ? mode
                : MachineRedstoneMode.IGNORE;
        if (redstoneMode != sanitized) {
            redstoneMode = sanitized;
            markChangedAndSync();
        }
    }

    public final int comparatorOutput() {
        return supportsRedstoneControl() && machineDiagnosticSnapshot().outputFull() ? 15 : 0;
    }

    @Override
    public MachineDiagnosticSnapshot machineDiagnosticSnapshot() {
        return applyRedstonePause(MachineDiagnosticSnapshot.inactive());
    }

    protected final MachineDiagnosticSnapshot applyRedstonePause(MachineDiagnosticSnapshot snapshot) {
        if (!isPausedByRedstone()) {
            return snapshot;
        }
        return new MachineDiagnosticSnapshot(
                MachineDiagnosticStatus.PAUSED,
                "redstone",
                snapshot.progress(),
                snapshot.progressMaximum(),
                snapshot.storedExperience(),
                snapshot.outputUsed(),
                snapshot.outputCapacity()
        );
    }

    @Override
    public CompoundTag exportMachineConfiguration() {
        CompoundTag configuration = new CompoundTag();
        if (supportsRedstoneControl()) {
            configuration.putString(REDSTONE_MODE_TAG, redstoneMode.serializedName());
        }
        return configuration;
    }

    @Override
    public boolean canApplyMachineConfiguration(int schemaVersion, CompoundTag configuration) {
        if (schemaVersion != CONFIGURATION_SCHEMA_VERSION || configuration == null) {
            return false;
        }
        if (!configuration.contains(REDSTONE_MODE_TAG)) {
            return true;
        }
        String stored = configuration.getStringOr(REDSTONE_MODE_TAG, "");
        return java.util.Arrays.stream(MachineRedstoneMode.values())
                .anyMatch(mode -> mode.serializedName().equals(stored));
    }

    @Override
    public void applyMachineConfiguration(int schemaVersion, CompoundTag configuration) {
        if (!canApplyMachineConfiguration(schemaVersion, configuration)) {
            return;
        }
        setRedstoneMode(MachineRedstoneMode.fromSerializedName(
                configuration.getStringOr(REDSTONE_MODE_TAG, MachineRedstoneMode.IGNORE.serializedName())
        ));
    }

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
        // Extension hook for machines that must flush transient state before serialization.
    }

    protected abstract void clearContentsForBlockDrop();

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        redstoneMode = supportsRedstoneControl()
                ? MachineRedstoneMode.fromSerializedName(input.getStringOr(
                        REDSTONE_MODE_TAG,
                        MachineRedstoneMode.IGNORE.serializedName()
                ))
                : MachineRedstoneMode.IGNORE;
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        if (supportsRedstoneControl() && redstoneMode != MachineRedstoneMode.IGNORE) {
            output.putString(REDSTONE_MODE_TAG, redstoneMode.serializedName());
        }
    }

    protected final void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            int comparatorOutput = comparatorOutput();
            if (lastComparatorOutput != comparatorOutput) {
                lastComparatorOutput = comparatorOutput;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
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
