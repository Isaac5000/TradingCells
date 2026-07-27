package com.cosmocraft.trading_cells.feature.incubators.adapters.input;

import com.cosmocraft.trading_cells.feature.incubators.adapters.output.IncubatorRegistrationAdapter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class PiglinIncubatorBlock extends IncubatorBlock<PiglinIncubatorBlockEntity> {
    public static final MapCodec<PiglinIncubatorBlock> CODEC = simpleCodec(PiglinIncubatorBlock::new);

    public PiglinIncubatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NonNull PiglinIncubatorBlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new PiglinIncubatorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<PiglinIncubatorBlockEntity> getBlockEntityType() {
        return IncubatorRegistrationAdapter.PIGLIN_INCUBATOR_BLOCK_ENTITY.get();
    }
}
