package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public final class ArcaneInfuserBlock extends AbstractPortableMachineBlock<ArcaneInfuserBlockEntity> {
    public static final MapCodec<ArcaneInfuserBlock> CODEC = simpleCodec(ArcaneInfuserBlock::new);
    private static final VoxelShape[] SHAPES = chamberShapes();

    private static VoxelShape[] chamberShapes() {
        VoxelShape north = Shapes.or(Block.box(1, 0, 1, 15, 3, 15),
                Block.box(1, 3, 2, 3, 13, 14), Block.box(13, 3, 2, 15, 13, 14),
                Block.box(3, 3, 13, 13, 13, 15), Block.box(4, 13, 8, 12, 16, 16),
                Block.box(3, 3, 3, 13, 4, 13), Block.box(3, 6, 6, 4, 10, 10),
                Block.box(12, 6, 6, 13, 10, 10), Block.box(6, 6, 12, 10, 10, 13)).optimize();
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north;
        for (int rotation = 1; rotation < 4; rotation++) {
            final VoxelShape[] next = {Shapes.empty()};
            result[rotation - 1].forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                    next[0] = Shapes.or(next[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            result[rotation] = next[0].optimize();
        }
        return result;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[switch (state.getValue(FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        }];
    }

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
    protected BlockEntityType<ArcaneInfuserBlockEntity> machineType() {
        return ArcaneInfuserRegistrationAdapter.BLOCK_ENTITY.get();
    }
}
