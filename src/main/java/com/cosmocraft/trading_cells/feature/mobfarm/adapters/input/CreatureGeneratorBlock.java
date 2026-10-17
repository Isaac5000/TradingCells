package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CreatureGeneratorBlock extends AbstractPortableMachineBlock<CreatureGeneratorBlockEntity> {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14), Block.box(4, 2, 4, 12, 9, 12),
            Block.box(1, 9, 1, 15, 13, 15), Block.box(3, 13, 3, 13, 14, 13)).optimize();
    public CreatureGeneratorBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CreatureGeneratorBlockEntity(pos, state); }
    @Override protected BlockEntityType<CreatureGeneratorBlockEntity> machineType() { return MobFarmRegistrationAdapter.CREATURE_GENERATOR_BLOCK_ENTITY.get(); }
}
