package com.cosmocraft.trading_cells.platform.neoforge.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MachineCageBlockShapes {
    public static final VoxelShape CAGE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 2.0D, 0.0D, 2.0D, 14.0D, 2.0D),
            Block.box(14.0D, 2.0D, 0.0D, 16.0D, 14.0D, 2.0D),
            Block.box(0.0D, 2.0D, 14.0D, 2.0D, 14.0D, 16.0D),
            Block.box(14.0D, 2.0D, 14.0D, 16.0D, 14.0D, 16.0D),
            Block.box(2.0D, 2.0D, 0.5D, 14.0D, 14.0D, 1.0D),
            Block.box(2.0D, 2.0D, 15.0D, 14.0D, 14.0D, 15.5D),
            Block.box(0.5D, 2.0D, 2.0D, 1.0D, 14.0D, 14.0D),
            Block.box(15.0D, 2.0D, 2.0D, 15.5D, 14.0D, 14.0D)
    ).optimize();

    private MachineCageBlockShapes() {
    }
}
