package com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.output.CreeperFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class CreeperFarmBlock extends AbstractPortableMachineBlock<CreeperFarmBlockEntity> {
    public static final MapCodec<CreeperFarmBlock> CODEC = simpleCodec(CreeperFarmBlock::new);

    public CreeperFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new CreeperFarmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<CreeperFarmBlockEntity> machineType() {
        return CreeperFarmRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
