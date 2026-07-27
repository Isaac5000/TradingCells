package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerCropStackAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FarmerBlockEntityRenderer implements BlockEntityRenderer<FarmerBlockEntity, FarmerBlockEntityRenderer.State> {
    private static final float ENTITY_SCALE = 0.30F;
    private static final float PLOT_SCALE = 0.30F;
    private static final double ENTITY_OFFSET = 0.20D;
    private static final double PLOT_OFFSET = 0.20D;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;

    public FarmerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull FarmerBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.villager = null;
        state.farmland.clear();
        state.crop.clear();

        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());

        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
        blockModelResolver.update(state.farmland, farmland, BlockDisplayContext.create());
        state.farmland.tintLayers().clear();

        FarmerCrop crop = blockEntity.crop();
        if (crop != FarmerCrop.NONE) {
            BlockState cropState = FarmerCropStackAdapter.cropState(
                    crop,
                    blockEntity.growthTicks(),
                    blockEntity.growthDurationTicks()
            );
            blockModelResolver.update(state.crop, cropState, BlockDisplayContext.create());
            state.crop.tintLayers().clear();
        }

        Entity entity = CapturedMobStackAdapter.createEntity(
                CapturedMobKind.VILLAGER,
                level,
                blockEntity.getItem(FarmerBlockEntity.VILLAGER_SLOT),
                BlockPos.ZERO
        );
        if (entity != null) {
            orient(entity, state.facing.toYRot());
            PreviewEntityRenderUtil.prepare(entity);
            state.villager = entityRenderer.extractEntity(entity, partialTicks);
            PreviewEntityRenderUtil.applyLight(state.villager, state.lightCoords);
            PreviewEntityRenderUtil.suppressWorldEffects(state.villager);
        }
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        double plotX = 0.5D + state.facing.getStepX() * PLOT_OFFSET;
        double plotZ = 0.5D + state.facing.getStepZ() * PLOT_OFFSET;
        submitBlock(
                state.farmland,
                new Vec3(plotX, 0.02D, plotZ),
                PLOT_SCALE,
                state.lightCoords,
                poseStack,
                submitNodeCollector
        );
        submitBlock(
                state.crop,
                new Vec3(plotX, 0.29D, plotZ),
                PLOT_SCALE,
                state.lightCoords,
                poseStack,
                submitNodeCollector
        );

        if (state.villager != null) {
            PreviewEntityRenderUtil.applyLight(state.villager, state.lightCoords);
            poseStack.pushPose();
            poseStack.translate(
                    0.5D - state.facing.getStepX() * ENTITY_OFFSET,
                    0.11D,
                    0.5D - state.facing.getStepZ() * ENTITY_OFFSET
            );
            poseStack.scale(ENTITY_SCALE, ENTITY_SCALE, ENTITY_SCALE);
            entityRenderer.submit(state.villager, camera, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
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
    public @NonNull AABB getRenderBoundingBox(FarmerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockModelRenderState farmland = new BlockModelRenderState();
        public final BlockModelRenderState crop = new BlockModelRenderState();
        public @Nullable EntityRenderState villager;
        public Direction facing = Direction.NORTH;
    }
}
