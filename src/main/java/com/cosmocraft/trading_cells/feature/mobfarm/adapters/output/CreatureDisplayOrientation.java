package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output;

import net.minecraft.core.Direction;

/**
 * Converts the pedestal's horizontal facing to the PoseStack Y-axis convention.
 *
 * Direction.toYRot() uses Minecraft's entity yaw sign: EAST=270, WEST=90.
 * Axis.YP rotations use the opposite sign (EAST=90, WEST=270).
 * The dragon's model has an additional reversed forward axis.
 */
public final class CreatureDisplayOrientation {
    private CreatureDisplayOrientation() { }

    public static float modelYaw(Direction front, boolean enderDragon) {
        return -front.toYRot() + (enderDragon ? 180.0F : 0.0F);
    }
}
