package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import org.jspecify.annotations.Nullable;

/** Detached, stationary previews share the ordinary entity renderer, never a world entity. */
record SimulationEntityPreview(EntityRenderState state, float scale) {
    private static final float HORIZONTAL_VISUAL_MARGIN = 0.50F;
    private static final float VERTICAL_VISUAL_MARGIN = 0.80F;

    static @Nullable SimulationEntityPreview create(@Nullable LivingEntity entity,
            EntityRenderDispatcher dispatcher, float yaw, float width, float height) {
        if (entity == null) { return null; }
        try {
            PreviewEntityRenderUtil.prepare(entity);
            entity.setYRot(yaw);
            entity.yRotO = yaw;
            entity.setXRot(0.0F);
            entity.xRotO = 0.0F;
            entity.yHeadRot = entity.yHeadRotO = entity.yBodyRot = entity.yBodyRotO = yaw;
            if (entity instanceof WitherBoss wither) {
                // The auxiliary heads otherwise retain independent saved/server rotations.
                for (int head = 0; head < wither.getHeadYRots().length; head++) {
                    wither.getHeadYRots()[head] = yaw;
                    wither.getHeadXRots()[head] = 0.0F;
                }
            }
            EntityRenderState state = dispatcher.extractEntity(entity, 0.0F);
            PreviewEntityRenderUtil.suppressWorldEffects(state);
            if (entity instanceof WaterAnimal && state instanceof LivingEntityRenderState living) {
                // Preview fish must not use their out-of-water flopping pose.
                living.isInWater = true;
            }
            // Entity models can extend beyond their AABB through tails, wings or held items.
            float scale = Math.min(
                    width * HORIZONTAL_VISUAL_MARGIN / Math.max(0.1F, entity.getBbWidth()),
                    height * VERTICAL_VISUAL_MARGIN / Math.max(0.1F, entity.getBbHeight())
            );
            if (entity.getType() == EntityTypes.ENDER_DRAGON) {
                // The dragon's multipart hitbox is much larger than the pedestal preview.
                scale = Math.min(width * 0.14F, height * 0.08F);
            } else if (entity instanceof WitherBoss && height > 0.60F) {
                // Item models have less available height than the machine pedestal.
                scale = Math.min(
                        width * 0.60F / Math.max(0.1F, entity.getBbWidth()),
                        height * 1.05F / Math.max(0.1F, entity.getBbHeight())
                );
            }
            return new SimulationEntityPreview(state, scale);
        } catch (RuntimeException | LinkageError unsupportedRenderer) {
            // A missing/incompatible optional renderer leaves the surrounding model intact.
            LogUtils.getLogger().warn("Cannot prepare simulation preview for {}", entity.getType(), unsupportedRenderer);
            return null;
        }
    }

    void submit(EntityRenderDispatcher dispatcher, double x, double y, double z, int light,
            PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        PreviewEntityRenderUtil.applyLight(state, light);
        poseStack.pushPose();
        try {
            poseStack.translate(x, y, z);
            poseStack.scale(scale, scale, scale);
            dispatcher.submit(state, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
        } finally {
            poseStack.popPose();
        }
    }
}
