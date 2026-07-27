package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class FarmerBlock extends AbstractPortableMachineBlock<FarmerBlockEntity> {
    public static final MapCodec<FarmerBlock> CODEC = simpleCodec(FarmerBlock::new);

    public FarmerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new FarmerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<FarmerBlockEntity> machineType() {
        return FarmerRegistrationAdapter.FARMER_BLOCK_ENTITY.get();
    }
}
