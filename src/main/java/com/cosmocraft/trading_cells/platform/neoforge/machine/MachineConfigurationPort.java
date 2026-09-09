package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.nbt.CompoundTag;

/** Whitelisted, atomic machine settings transfer used by the configurator. */
public interface MachineConfigurationPort {
    int CONFIGURATION_SCHEMA_VERSION = 1;

    CompoundTag exportMachineConfiguration();

    boolean canApplyMachineConfiguration(int schemaVersion, CompoundTag configuration);

    void applyMachineConfiguration(int schemaVersion, CompoundTag configuration);
}
