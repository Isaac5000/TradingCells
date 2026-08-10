package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerCropStackAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
        state.worker = null;

        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());

        FarmerCrop crop = blockEntity.crop();
        BlockState soil = FarmerCropStackAdapter.soilState(blockEntity.kind(), crop);
        state.cachedSoil = updateBlockState(state.soil, soil, state.cachedSoil);
        BlockState cropState = FarmerCropStackAdapter.cropState(
                blockEntity.kind(),
                blockEntity.getItem(FarmerBlockEntity.CROP_SLOT),
                blockEntity.growthTicks(),
                blockEntity.growthDurationTicks()
        );
        state.cachedCrop = updateBlockState(state.crop, cropState, state.cachedCrop);

        Entity entity = state.getOrCreateWorker(blockEntity, level);
        if (entity != null) {
            orient(entity, state.facing.toYRot());
            PreviewEntityRenderUtil.prepare(entity);
            state.worker = entityRenderer.extractEntity(entity, partialTicks);
            PreviewEntityRenderUtil.applyLight(state.worker, state.lightCoords);
            PreviewEntityRenderUtil.suppressWorldEffects(state.worker);
        }
    }

    private BlockState updateBlockState(
            BlockModelRenderState renderState,
            BlockState nextState,
            BlockState cachedState
    ) {
        if (nextState == cachedState) {
            return cachedState;
        }
        renderState.clear();
        if (!nextState.isAir()) {
            blockModelResolver.update(renderState, nextState, BlockDisplayContext.create());
            renderState.tintLayers().clear();
        }
        return nextState;
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
                state.soil,
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

        if (state.worker != null) {
            PreviewEntityRenderUtil.applyLight(state.worker, state.lightCoords);
            poseStack.pushPose();
            poseStack.translate(
                    0.5D - state.facing.getStepX() * ENTITY_OFFSET,
                    0.11D,
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
    public @NonNull AABB getRenderBoundingBox(FarmerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockModelRenderState soil = new BlockModelRenderState();
        public final BlockModelRenderState crop = new BlockModelRenderState();
        public @Nullable EntityRenderState worker;
        public Direction facing = Direction.NORTH;
        private BlockState cachedSoil = Blocks.AIR.defaultBlockState();
        private BlockState cachedCrop = Blocks.AIR.defaultBlockState();
        private ItemStack cachedWorkerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedWorker;

        private @Nullable Entity getOrCreateWorker(FarmerBlockEntity blockEntity, Level level) {
            ItemStack workerStack = blockEntity.getItem(FarmerBlockEntity.WORKER_SLOT);
            if (workerStack.isEmpty()) {
                cachedWorkerStack = ItemStack.EMPTY;
                cachedWorker = null;
                return null;
            }
            if (cachedWorker == null
                    || !ItemStack.isSameItemSameComponents(cachedWorkerStack, workerStack)) {
                cachedWorker = CapturedMobStackAdapter.createEntity(
                        blockEntity.kind() == FarmerKind.VILLAGER
                                ? CapturedMobKind.VILLAGER
                                : CapturedMobKind.PIGLIN,
                        level,
                        workerStack,
                        BlockPos.ZERO
                );
                if (blockEntity.kind() == FarmerKind.VILLAGER
                        && cachedWorker instanceof Villager villager) {
                    villager.setVillagerData(villager.getVillagerData().withProfession(
                            BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.FARMER)
                    ));
                }
                cachedWorkerStack = workerStack.copy();
            }
            return cachedWorker;
        }

        private void clearCaches() {
            soil.clear();
            crop.clear();
            cachedSoil = Blocks.AIR.defaultBlockState();
            cachedCrop = Blocks.AIR.defaultBlockState();
            cachedWorkerStack = ItemStack.EMPTY;
            cachedWorker = null;
        }
    }
}
