package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input;

import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.ZombieFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class ZombieFarmBlock extends AbstractPortableMachineBlock<ZombieFarmBlockEntity> {
    public static final MapCodec<ZombieFarmBlock> CODEC = simpleCodec(ZombieFarmBlock::new);

    public ZombieFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ZombieFarmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<ZombieFarmBlockEntity> machineType() {
        return ZombieFarmRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
