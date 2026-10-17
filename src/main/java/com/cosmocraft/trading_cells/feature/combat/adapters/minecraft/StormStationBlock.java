package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StormStationBlock extends AbstractPortableMachineBlock<StormStationBlockEntity> {
    public StormStationBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StormStationBlockEntity(pos, state); }
    @Override protected BlockEntityType<StormStationBlockEntity> machineType() { return CombatRegistrationAdapter.STATION_ENTITY.get(); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
}
