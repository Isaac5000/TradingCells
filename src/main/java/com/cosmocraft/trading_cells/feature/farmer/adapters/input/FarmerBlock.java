package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class FarmerBlock extends AbstractPortableMachineBlock<VillagerFarmerBlockEntity> {

    public FarmerBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @NonNull VillagerFarmerBlockEntity newBlockEntity(
            @NonNull BlockPos pos,
            @NonNull BlockState state
    ) {
        return new VillagerFarmerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<VillagerFarmerBlockEntity> machineType() {
        return FarmerRegistrationAdapter.FARMER_BLOCK_ENTITY.get();
    }
}
