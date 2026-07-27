package com.cosmocraft.trading_cells.feature.ironfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class IronFarmBlock extends AbstractPortableMachineBlock<IronFarmBlockEntity> {
    public static final MapCodec<IronFarmBlock> CODEC = simpleCodec(IronFarmBlock::new);

    public IronFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new IronFarmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<IronFarmBlockEntity> machineType() {
        return IronFarmRegistrationAdapter.IRON_FARM_BLOCK_ENTITY.get();
    }
}
