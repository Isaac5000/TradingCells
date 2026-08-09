package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class VillagerFarmerBlockEntity extends FarmerBlockEntity {
    public VillagerFarmerBlockEntity(BlockPos pos, BlockState state) {
        super(
                FarmerRegistrationAdapter.FARMER_BLOCK_ENTITY.get(),
                pos,
                state,
                FarmerKind.VILLAGER
        );
    }
}
