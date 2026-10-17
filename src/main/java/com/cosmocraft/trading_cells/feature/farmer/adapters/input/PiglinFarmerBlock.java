package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class PiglinFarmerBlock extends AbstractPortableMachineBlock<PiglinFarmerBlockEntity> {

    public PiglinFarmerBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @NonNull PiglinFarmerBlockEntity newBlockEntity(
            @NonNull BlockPos pos,
            @NonNull BlockState state
    ) {
        return new PiglinFarmerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<PiglinFarmerBlockEntity> machineType() {
        return FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK_ENTITY.get();
    }
}
