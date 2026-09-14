package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Old IDs remain loadable, but their first server tick installs the general farm. */
public abstract class LegacyMobFarmBlock<T extends PortableMachineBlockEntity> extends AbstractPortableMachineBlock<T> {
    protected LegacyMobFarmBlock(Properties properties) { super(properties); }

    @Override public <B extends BlockEntity> @Nullable BlockEntityTicker<B> getTicker(
            Level level, BlockState state, BlockEntityType<B> type) {
        if (level.isClientSide()) { return null; }
        return createTickerHelper(type, machineType(), (_, _, _, machine) -> {
            if (!LegacyMobFarmMigration.convert(machine)) { machine.processServerTick(); }
        });
    }
}
