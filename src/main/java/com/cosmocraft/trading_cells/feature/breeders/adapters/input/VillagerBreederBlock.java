package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class VillagerBreederBlock extends BreederBlock<VillagerBreederBlockEntity> {
    public static final MapCodec<VillagerBreederBlock> CODEC = simpleCodec(VillagerBreederBlock::new);

    public VillagerBreederBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
