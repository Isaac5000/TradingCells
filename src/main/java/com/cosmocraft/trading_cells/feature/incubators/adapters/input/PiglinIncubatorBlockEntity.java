package com.cosmocraft.trading_cells.feature.incubators.adapters.input;

import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class PiglinIncubatorBlockEntity extends IncubatorBlockEntity {
    public PiglinIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_BLOCK_ENTITY.get(), pos, state, CapturedMobKind.PIGLIN);
    }
}
