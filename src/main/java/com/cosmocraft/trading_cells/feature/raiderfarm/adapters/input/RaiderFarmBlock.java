package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.RaiderFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.LegacyMobFarmBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class RaiderFarmBlock extends LegacyMobFarmBlock<RaiderFarmBlockEntity> {
    public static final MapCodec<RaiderFarmBlock> CODEC = simpleCodec(RaiderFarmBlock::new);

    public RaiderFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new RaiderFarmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<RaiderFarmBlockEntity> machineType() {
        return RaiderFarmRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
