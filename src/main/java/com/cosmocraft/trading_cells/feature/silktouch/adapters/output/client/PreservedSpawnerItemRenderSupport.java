package com.cosmocraft.trading_cells.feature.silktouch.adapters.output.client;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.PreservedSpawnerItemAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Renders the complete fixed entity hierarchy inside a trusted spawner item. */
public final class PreservedSpawnerItemRenderSupport {
    private static final float CAGE_HEIGHT = 0.58F;
    private static final float CAGE_WIDTH = 0.54F;

    private PreservedSpawnerItemRenderSupport() {
    }

    public static final class Renderer implements SpecialModelRenderer<ItemStack> {
        @Override
        public ItemStack extractArgument(@NonNull ItemStack stack) {
            return stack;
        }

        @Override
        public void submit(
                @Nullable ItemStack stack,
                @NonNull PoseStack poseStack,
                @NonNull SubmitNodeCollector collector,
                int packedLight,
                int packedOverlay,
                boolean hasFoil,
                int outlineColor
        ) {
            Minecraft minecraft = Minecraft.getInstance();
            if (stack == null || minecraft.level == null
                    || !PreservedSpawnerItemAdapter.isTrustedSpawner(stack)) {
                return;
            }

            Entity root = PreservedSpawnerItemAdapter.createRootDisplayEntity(minecraft.level, stack);
            if (root == null) {
                return;
            }

            List<Entity> entities = root.getSelfAndPassengers().toList();
            float maximumWidth = entities.stream()
                    .map(Entity::getBbWidth)
                    .max(Float::compare)
                    .orElse(root.getBbWidth());
            float stackedHeight = hierarchyHeight(entities);

            float scale = Math.min(
                    CAGE_WIDTH / Math.max(0.25F, maximumWidth),
                    CAGE_HEIGHT / Math.max(0.25F, stackedHeight)
            );
            poseStack.pushPose();
            try {
                poseStack.translate(0.5D, 0.16D, 0.5D);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(scale, scale, scale);
                submitStaticHierarchy(entities, poseStack, collector, packedLight);
            } finally {
                poseStack.popPose();
            }
        }

        private static float hierarchyHeight(List<Entity> entities) {
            float height = 0.0F;
            for (int index = 0; index < entities.size(); index++) {
                Entity entity = entities.get(index);
                height += index == 0 ? entity.getBbHeight() : entity.getBbHeight() * 0.55F;
            }
            return height;
        }

        private static void submitStaticHierarchy(
                List<Entity> entities,
                PoseStack poseStack,
                SubmitNodeCollector collector,
                int packedLight
        ) {
            float verticalOffset = 0.0F;
            for (int index = 0; index < entities.size(); index++) {
                Entity entity = entities.get(index);
                poseStack.pushPose();
                try {
                    poseStack.translate(0.0D, verticalOffset, 0.0D);
                    submitStaticEntity(entity, poseStack, collector, packedLight);
                } finally {
                    poseStack.popPose();
                }
                verticalOffset += index == 0 ? entity.getBbHeight() * 0.45F : entity.getBbHeight() * 0.55F;
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> extents) {
            extents.accept(new Vector3f(0.0F, 0.0F, 0.0F));
            extents.accept(new Vector3f(1.0F, 1.0F, 1.0F));
        }

        private static void submitStaticEntity(
                Entity entity,
                PoseStack poseStack,
                SubmitNodeCollector collector,
                int packedLight
        ) {
            zeroRotation(entity);
            PreviewEntityRenderUtil.prepare(entity);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderState state = dispatcher.extractEntity(entity, 0.0F);
            PreviewEntityRenderUtil.applyLight(state, packedLight);
            PreviewEntityRenderUtil.suppressWorldEffects(state);
            CameraRenderState camera = new CameraRenderState();
            camera.initialized = true;
            dispatcher.submit(state, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
        }

        private static void zeroRotation(Entity entity) {
            entity.setYRot(0.0F);
            entity.setXRot(0.0F);
            entity.yRotO = 0.0F;
            entity.xRotO = 0.0F;
            if (entity instanceof LivingEntity living) {
                living.yHeadRot = 0.0F;
                living.yHeadRotO = 0.0F;
                living.yBodyRot = 0.0F;
                living.yBodyRotO = 0.0F;
            }
        }
    }

    public static final class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        private Unbaked() {
        }

        @Override
        public @NonNull MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.@NonNull BakingContext context) {
            return new Renderer();
        }
    }
}
