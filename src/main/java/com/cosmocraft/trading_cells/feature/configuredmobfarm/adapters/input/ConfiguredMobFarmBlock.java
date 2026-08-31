package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.ConfiguredMobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class ConfiguredMobFarmBlock extends AbstractPortableMachineBlock<ConfiguredMobFarmBlockEntity> {
    private final ConfiguredMobFarmKind kind;
    private final MapCodec<ConfiguredMobFarmBlock> codec;

    public ConfiguredMobFarmBlock(Properties properties, ConfiguredMobFarmKind kind) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(copyProperties -> new ConfiguredMobFarmBlock(copyProperties, kind));
    }

    public ConfiguredMobFarmKind kind() {
        return kind;
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ConfiguredMobFarmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<ConfiguredMobFarmBlockEntity> machineType() {
        return ConfiguredMobFarmRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
