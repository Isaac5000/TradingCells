package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class PiglinQuarryBlock extends AbstractPortableMachineBlock<PiglinQuarryBlockEntity> {
    public static final MapCodec<PiglinQuarryBlock> CODEC = simpleCodec(PiglinQuarryBlock::new);

    public PiglinQuarryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NonNull PiglinQuarryBlockEntity newBlockEntity(
            @NonNull BlockPos pos,
            @NonNull BlockState state
    ) {
        return new PiglinQuarryBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<PiglinQuarryBlockEntity> machineType() {
        return QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK_ENTITY.get();
    }
}
