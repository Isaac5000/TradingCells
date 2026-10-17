package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class QuarryBlock extends AbstractPortableMachineBlock<VillagerQuarryBlockEntity> {

    public QuarryBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @NonNull VillagerQuarryBlockEntity newBlockEntity(
            @NonNull BlockPos pos,
            @NonNull BlockState state
    ) {
        return new VillagerQuarryBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<VillagerQuarryBlockEntity> machineType() {
        return QuarryRegistrationAdapter.QUARRY_BLOCK_ENTITY.get();
    }
}
