package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
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

public final class EssenceWorkbenchBlock extends AbstractPortableMachineBlock<EssenceWorkbenchBlockEntity> {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 10, 1, 15, 13, 15), Block.box(3, 13, 3, 13, 14, 13),
            Block.box(2, 0, 2, 4, 10, 4), Block.box(2, 0, 12, 4, 10, 14),
            Block.box(12, 0, 2, 14, 10, 4), Block.box(12, 0, 12, 14, 10, 14)).optimize();

    public EssenceWorkbenchBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(EssenceWorkbenchBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssenceWorkbenchBlockEntity(pos, state); }
    @Override protected BlockEntityType<EssenceWorkbenchBlockEntity> machineType() {
        return MobFarmRegistrationAdapter.WORKBENCH_BLOCK_ENTITY.get();
    }
}
