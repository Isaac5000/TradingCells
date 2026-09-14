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
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import org.jspecify.annotations.Nullable;

/** Detached, stationary previews share the ordinary entity renderer, never a world entity. */
record SimulationEntityPreview(EntityRenderState state, float scale) {
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
            EntityRenderState state = dispatcher.extractEntity(entity, 0.0F);
            PreviewEntityRenderUtil.suppressWorldEffects(state);
            if (entity instanceof WaterAnimal && state instanceof LivingEntityRenderState living) {
                // Preview fish must not use their out-of-water flopping pose.
                living.isInWater = true;
            }
            float scale = Math.min(width / Math.max(0.1F, entity.getBbWidth()),
                    height / Math.max(0.1F, entity.getBbHeight()));
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
