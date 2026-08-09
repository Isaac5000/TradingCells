package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class PiglinQuarryBlockEntity extends QuarryBlockEntity {
    public PiglinQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK_ENTITY.get(), pos, state, QuarryKind.PIGLIN);
    }
}
