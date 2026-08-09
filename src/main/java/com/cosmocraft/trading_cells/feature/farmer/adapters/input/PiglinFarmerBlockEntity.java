package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class PiglinFarmerBlockEntity extends FarmerBlockEntity {
    public PiglinFarmerBlockEntity(BlockPos pos, BlockState state) {
        super(
                FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK_ENTITY.get(),
                pos,
                state,
                FarmerKind.PIGLIN
        );
    }
}
