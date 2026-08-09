package com.cosmocraft.trading_cells.feature.experience.adapters.input;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ExperienceStorageBlock extends AbstractPortableMachineBlock<ExperienceStorageBlockEntity> {
    public static final MapCodec<ExperienceStorageBlock> CODEC = simpleCodec(ExperienceStorageBlock::new);

    public ExperienceStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ExperienceStorageBlockEntity(pos, state);
    }

    @Override
    public <B extends BlockEntity> @Nullable BlockEntityTicker<B> getTicker(
            @NonNull Level level,
            @NonNull BlockState state,
            @NonNull BlockEntityType<B> type
    ) {
        return null;
    }

    @Override
    protected BlockEntityType<ExperienceStorageBlockEntity> machineType() {
        return ExperienceStorageRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
