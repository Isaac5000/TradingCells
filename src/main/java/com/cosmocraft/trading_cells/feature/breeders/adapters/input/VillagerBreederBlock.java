package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class VillagerBreederBlock extends BreederBlock<VillagerBreederBlockEntity> {

    public VillagerBreederBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @NonNull VillagerBreederBlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new VillagerBreederBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<VillagerBreederBlockEntity> getBlockEntityType() {
        return BreederRegistrationAdapter.VILLAGER_BREEDER_BLOCK_ENTITY.get();
    }
}
