package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class SkeletonFarmBlock extends AbstractPortableMachineBlock<SkeletonFarmBlockEntity> {
    public static final MapCodec<SkeletonFarmBlock> CODEC = simpleCodec(SkeletonFarmBlock::new);

    public SkeletonFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new SkeletonFarmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<SkeletonFarmBlockEntity> machineType() {
        return SkeletonFarmRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
