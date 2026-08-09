package com.cosmocraft.trading_cells.feature.quarry.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.domain.model.QuarryKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class QuarryBlockEntityRenderer
        implements BlockEntityRenderer<QuarryBlockEntity, QuarryBlockEntityRenderer.State> {
    private static final float ENTITY_SCALE = 0.31F;
    private static final float MATERIAL_SCALE = 0.24F;
    private static final double ENTITY_OFFSET = 0.18D;
    private static final double MATERIAL_OFFSET = 0.20D;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;

    public QuarryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull QuarryBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.worker = null;
        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        BlockState material = blockEntity.currentMaterialState();
        if (material != state.cachedMaterial) {
            state.material.clear();
            if (!material.isAir()) {
                blockModelResolver.update(state.material, material, BlockDisplayContext.create());
                state.material.tintLayers().clear();
            }
            state.cachedMaterial = material;
        }
        Entity entity = state.getOrCreateWorker(blockEntity, level);
        if (entity == null) {
            return;
        }
        orient(entity, state.facing.toYRot());
        PreviewEntityRenderUtil.prepare(entity);
        state.worker = entityRenderer.extractEntity(entity, partialTicks);
        PreviewEntityRenderUtil.applyLight(state.worker, state.lightCoords);
        PreviewEntityRenderUtil.suppressWorldEffects(state.worker);
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        double materialX = 0.5D + state.facing.getStepX() * MATERIAL_OFFSET;
        double materialZ = 0.5D + state.facing.getStepZ() * MATERIAL_OFFSET;
        submitBlock(
                state.material,
                new Vec3(materialX, 0.10D, materialZ),
                MATERIAL_SCALE,
                state.lightCoords,
                poseStack,
                submitNodeCollector
        );

        if (state.worker != null) {
            PreviewEntityRenderUtil.applyLight(state.worker, state.lightCoords);
            poseStack.pushPose();
            poseStack.translate(
                    0.5D - state.facing.getStepX() * ENTITY_OFFSET,
                    0.10D,
                    0.5D - state.facing.getStepZ() * ENTITY_OFFSET
            );
            poseStack.scale(ENTITY_SCALE, ENTITY_SCALE, ENTITY_SCALE);
            entityRenderer.submit(state.worker, camera, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
            poseStack.popPose();
        }
    }

    private static void submitBlock(
            BlockModelRenderState state,
            Vec3 position,
            float scale,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (state.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(
                position.x() - scale * 0.5D,
                position.y(),
                position.z() - scale * 0.5D
        );
        poseStack.scale(scale, scale, scale);
        state.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
        poseStack.popPose();
    }

    private static void orient(Entity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setXRot(0.0F);
        entity.yRotO = yaw;
        entity.xRotO = 0.0F;
        if (entity instanceof LivingEntity living) {
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
        }
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(QuarryBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1.0D,
                pos.getY() + 1.0D,
                pos.getZ() + 1.0D
        );
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockModelRenderState material = new BlockModelRenderState();
        public @Nullable EntityRenderState worker;
        public Direction facing = Direction.NORTH;
        private BlockState cachedMaterial = Blocks.AIR.defaultBlockState();
        private ItemStack cachedWorkerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedWorker;

        private @Nullable Entity getOrCreateWorker(QuarryBlockEntity blockEntity, Level level) {
            ItemStack workerStack = blockEntity.getItem(QuarryBlockEntity.WORKER_SLOT);
            if (workerStack.isEmpty()) {
                cachedWorkerStack = ItemStack.EMPTY;
                cachedWorker = null;
                return null;
            }
            if (cachedWorker == null
                    || !ItemStack.isSameItemSameComponents(cachedWorkerStack, workerStack)) {
                CapturedMobKind capturedKind = blockEntity.kind() == QuarryKind.VILLAGER
                        ? CapturedMobKind.VILLAGER
                        : CapturedMobKind.PIGLIN;
                cachedWorker = CapturedMobStackAdapter.createEntity(
                        capturedKind,
                        level,
                        workerStack,
                        BlockPos.ZERO
                );
                cachedWorkerStack = workerStack.copy();
            }
            return cachedWorker;
        }

        private void clearCaches() {
            material.clear();
            cachedMaterial = Blocks.AIR.defaultBlockState();
            cachedWorkerStack = ItemStack.EMPTY;
            cachedWorker = null;
        }
    }
}
