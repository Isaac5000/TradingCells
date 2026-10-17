package com.cosmocraft.trading_cells.feature.converter.adapters.input;

import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class ConverterBlock extends AbstractPortableMachineBlock<ConverterBlockEntity> {

    public ConverterBlock(Properties properties) {
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ConverterBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<ConverterBlockEntity> machineType() {
        return ConverterRegistrationAdapter.CONVERTER_BLOCK_ENTITY.get();
    }
}
