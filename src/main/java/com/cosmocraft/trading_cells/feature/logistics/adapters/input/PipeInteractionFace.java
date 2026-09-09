package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

public final class PipeInteractionFace {
    private PipeInteractionFace() {
    }

    public static Direction resolve(BlockHitResult hit) {
        var point = hit.getLocation().subtract(hit.getBlockPos().getX() + 0.5,
                hit.getBlockPos().getY() + 0.5, hit.getBlockPos().getZ() + 0.5);
        double x = Math.abs(point.x), y = Math.abs(point.y), z = Math.abs(point.z);
        if (Math.max(x, Math.max(y, z)) <= 3.0 / 16.0 + 1.0E-6) {
            return hit.getDirection();
        }
        if (y >= x && y >= z) {
            return point.y < 0 ? Direction.DOWN : Direction.UP;
        }
        if (x >= z) {
            return point.x < 0 ? Direction.WEST : Direction.EAST;
        }
        return point.z < 0 ? Direction.NORTH : Direction.SOUTH;
    }
}
