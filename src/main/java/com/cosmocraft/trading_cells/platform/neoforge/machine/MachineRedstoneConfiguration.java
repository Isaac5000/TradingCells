package com.cosmocraft.trading_cells.platform.neoforge.machine;

import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineRedstoneMode;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MachineRedstoneConfiguration {
    public static final String TAG = "MachineRedstoneMode";

    private MachineRedstoneConfiguration() {
    }

    public static CompoundTag export(MachineRedstoneMode mode) {
        CompoundTag configuration = new CompoundTag();
        configuration.putString(TAG, mode.serializedName());
        return configuration;
    }

    public static boolean isValid(int schemaVersion, CompoundTag configuration) {
        if (schemaVersion != MachineConfigurationPort.CONFIGURATION_SCHEMA_VERSION || configuration == null) {
            return false;
        }
        String stored = configuration.getStringOr(TAG, MachineRedstoneMode.IGNORE.serializedName());
        return Arrays.stream(MachineRedstoneMode.values())
                .anyMatch(mode -> mode.serializedName().equals(stored));
    }

    public static MachineRedstoneMode fromConfiguration(CompoundTag configuration) {
        return MachineRedstoneMode.fromSerializedName(configuration.getStringOr(
                TAG,
                MachineRedstoneMode.IGNORE.serializedName()
        ));
    }

    public static MachineRedstoneMode load(ValueInput input) {
        return MachineRedstoneMode.fromSerializedName(input.getStringOr(
                TAG,
                MachineRedstoneMode.IGNORE.serializedName()
        ));
    }

    public static void save(ValueOutput output, MachineRedstoneMode mode) {
        if (mode != MachineRedstoneMode.IGNORE) {
            output.putString(TAG, mode.serializedName());
        }
    }

    public static MachineDiagnosticSnapshot applyPause(
            boolean paused,
            MachineDiagnosticSnapshot snapshot
    ) {
        if (!paused) {
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
}
