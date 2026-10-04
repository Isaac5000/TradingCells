package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class EntityInteractionFarmBlock extends AbstractPortableMachineBlock<EntityInteractionFarmBlockEntity> {
    public EntityInteractionFarmBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(EntityInteractionFarmBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EntityInteractionFarmBlockEntity(pos, state); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return false; }
    @Override protected BlockEntityType<EntityInteractionFarmBlockEntity> machineType() { return MobFarmRegistrationAdapter.INTERACTION_BLOCK_ENTITY.get(); }
}
