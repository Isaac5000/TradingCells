package com.cosmocraft.trading_cells.platform.neoforge.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.util.concurrent.atomic.AtomicInteger;

public final class PreviewEntityRenderUtil {
    private static final AtomicInteger NEXT_PREVIEW_ENTITY_ID = new AtomicInteger(-1_000_000);

    private PreviewEntityRenderUtil() {
    }

    public static void prepare(Entity entity) {
        entity.setId(NEXT_PREVIEW_ENTITY_ID.getAndDecrement());
        entity.setNoGravity(true);
        entity.clearFire();
        entity.setSilent(true);
        entity.setInvisible(false);
    }

    public static void applyLight(EntityRenderState state, int packedLight) {
        state.lightCoords = packedLight;
    }

    public static void suppressWorldEffects(EntityRenderState state) {
        state.shadowRadius = 0.0F;
        state.shadowPieces.clear();
        state.displayFireAnimation = false;
        state.nameTag = null;
        state.scoreText = null;
        state.leashStates = null;
        if (state instanceof LivingEntityRenderState livingState) {
            livingState.hasRedOverlay = false;
        }
    }

    public static int sampleCageLightCoords(Level level, BlockPos pos) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        for (Direction direction : Direction.values()) {
            BlockPos samplePos = pos.relative(direction);
            if (level.getBlockState(samplePos).canOcclude()) {
                continue;
            }
            blockLight = Math.max(blockLight, level.getBrightness(LightLayer.BLOCK, samplePos));
            skyLight = Math.max(skyLight, level.getBrightness(LightLayer.SKY, samplePos));
        }
        return LightCoordsUtil.pack(blockLight, skyLight);
    }
}
