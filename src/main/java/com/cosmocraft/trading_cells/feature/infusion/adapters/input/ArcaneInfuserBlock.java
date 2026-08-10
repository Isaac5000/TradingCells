package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ArcaneInfuserBlock extends AbstractPortableMachineBlock<ArcaneInfuserBlockEntity> {
    public static final MapCodec<ArcaneInfuserBlock> CODEC = simpleCodec(ArcaneInfuserBlock::new);

    public ArcaneInfuserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NonNull BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ArcaneInfuserBlockEntity(pos, state);
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
    protected BlockEntityType<ArcaneInfuserBlockEntity> machineType() {
        return ArcaneInfuserRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
