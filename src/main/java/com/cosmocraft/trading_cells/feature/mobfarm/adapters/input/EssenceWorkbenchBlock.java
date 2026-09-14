package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class EssenceWorkbenchBlock extends AbstractPortableMachineBlock<EssenceWorkbenchBlockEntity> {
    public EssenceWorkbenchBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(EssenceWorkbenchBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssenceWorkbenchBlockEntity(pos, state); }
    @Override protected BlockEntityType<EssenceWorkbenchBlockEntity> machineType() {
        return MobFarmRegistrationAdapter.WORKBENCH_BLOCK_ENTITY.get();
    }
}
