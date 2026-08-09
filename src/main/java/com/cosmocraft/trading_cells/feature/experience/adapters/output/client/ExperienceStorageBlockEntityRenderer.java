package com.cosmocraft.trading_cells.feature.experience.adapters.output.client;

import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.experience.domain.model.ExperienceMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Renders the storage's decorative XP orb and its targeted status label. */
public final class ExperienceStorageBlockEntityRenderer implements BlockEntityRenderer<
        ExperienceStorageBlockEntity,
        ExperienceStorageBlockEntityRenderer.State> {
    private static final Identifier ORB_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private static final RenderType ORB_RENDER_TYPE = RenderTypes.entityTranslucentCullItemTarget(ORB_TEXTURE);
    private static final int FULL_BRIGHT = 15_728_880;
    private static final float ORB_SCALE = 0.46F;
    private static final int HOVER_XP_OFFSET = -5;
    private static final int HOVER_LEVELS_OFFSET = 5;

    public ExperienceStorageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // The renderer uses vanilla's orb texture and does not need baked context resources.
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull ExperienceStorageBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                state,
                partialTicks,
                cameraPosition,
                breakProgress
        );
        int storedExperience = blockEntity.storedExperience();
        state.icon = iconFor(storedExperience);
        state.ageInTicks = (blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime())
                + partialTicks;
        state.hoverExperienceText = Component.translatable(
                "gui.trading_cells.experience_value",
                storedExperience
        ).withStyle(ChatFormatting.GREEN);
        state.hoverLevelsText = Component.translatable(
                "gui.trading_cells.levels_value",
                ExperienceMath.levelForTotalPoints(storedExperience)
        ).withStyle(ChatFormatting.GREEN);

        boolean itemRender = blockEntity.getBlockPos().equals(BlockPos.ZERO)
                && cameraPosition.equals(Vec3.atCenterOf(BlockPos.ZERO));
        Minecraft minecraft = Minecraft.getInstance();
        state.targeted = !itemRender
                && minecraft.gui.screen() == null
                && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getBlockPos().equals(blockEntity.getBlockPos());
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        submitOrb(state, poseStack, submitNodeCollector, camera);
        if (state.targeted) {
            submitNodeCollector.submitNameTag(
                    poseStack,
                    new Vec3(0.5D, 1.0D, 0.5D),
                    HOVER_XP_OFFSET,
                    state.hoverExperienceText,
                    false,
                    FULL_BRIGHT,
                    camera
            );
            submitNodeCollector.submitNameTag(
                    poseStack,
                    new Vec3(0.5D, 1.0D, 0.5D),
                    HOVER_LEVELS_OFFSET,
                    state.hoverLevelsText,
                    false,
                    FULL_BRIGHT,
                    camera
            );
        }
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(ExperienceStorageBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 1.0D,
                pos.getY(),
                pos.getZ() - 1.0D,
                pos.getX() + 2.0D,
                pos.getY() + 3.0D,
                pos.getZ() + 2.0D
        );
    }

    private static void submitOrb(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        int icon = state.icon;
        float u0 = icon % 4 * 16 / 64.0F;
        float u1 = (icon % 4 * 16 + 16) / 64.0F;
        float v0 = icon / 4 * 16 / 64.0F;
        float v1 = (icon / 4 * 16 + 16) / 64.0F;
        float phase = state.ageInTicks / 2.0F;
        int red = (int) ((Mth.sin(phase) + 1.0F) * 0.5F * 255.0F);
        int blue = (int) ((Mth.sin(phase + (float) (Math.PI * 4.0D / 3.0D)) + 1.0F) * 0.1F * 255.0F);
        float bob = Mth.sin(state.ageInTicks * 0.08F) * 0.04F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.42F + bob, 0.5F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(ORB_SCALE, ORB_SCALE, ORB_SCALE);
        submitNodeCollector.submitCustomGeometry(poseStack, ORB_RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.25F, red, blue, u0, v1);
            vertex(buffer, pose, 0.5F, -0.25F, red, blue, u1, v1);
            vertex(buffer, pose, 0.5F, 0.75F, red, blue, u1, v0);
            vertex(buffer, pose, -0.5F, 0.75F, red, blue, u0, v0);
        });
        poseStack.popPose();
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            int red,
            int blue,
            float u,
            float v
    ) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(red, 255, blue, 200)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static int iconFor(int experience) {
        if (experience >= 2_477) {
            return 10;
        }
        if (experience >= 1_237) {
            return 9;
        }
        if (experience >= 617) {
            return 8;
        }
        if (experience >= 307) {
            return 7;
        }
        if (experience >= 149) {
            return 6;
        }
        if (experience >= 73) {
            return 5;
        }
        if (experience >= 37) {
            return 4;
        }
        if (experience >= 17) {
            return 3;
        }
        if (experience >= 7) {
            return 2;
        }
        return experience >= 3 ? 1 : 0;
    }

    public static final class State extends BlockEntityRenderState {
        private int icon;
        private float ageInTicks;
        private boolean targeted;
        private Component hoverExperienceText = Component.empty();
        private Component hoverLevelsText = Component.empty();
    }
}
