package com.cosmocraft.trading_cells.feature.incubators.adapters.input;

import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class VillagerIncubatorBlock extends IncubatorBlock<VillagerIncubatorBlockEntity> {

    public VillagerIncubatorBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @NonNull VillagerIncubatorBlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new VillagerIncubatorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<VillagerIncubatorBlockEntity> getBlockEntityType() {
        return IncubatorRegistrationAdapter.VILLAGER_INCUBATOR_BLOCK_ENTITY.get();
    }
}
