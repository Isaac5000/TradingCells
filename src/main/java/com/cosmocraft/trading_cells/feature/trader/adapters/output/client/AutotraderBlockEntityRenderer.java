package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockEntity;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.MachineEntityRenderScales;
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
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AutotraderBlockEntityRenderer implements BlockEntityRenderer<AutotraderBlockEntity, AutotraderBlockEntityRenderer.State> {
    private static final float ENTITY_SCALE = MachineEntityRenderScales.TRADER_ENTITY;
    private static final float POI_SCALE = 0.28F;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;

    public AutotraderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull AutotraderBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.displayEntity = null;
        state.poi.clear();
        if (blockEntity.getLevel() == null) {
            state.clearCache();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos());
        ItemStack poiStack = blockEntity.copyPoiStack();
        if (poiStack.getItem() instanceof BlockItem blockItem) {
            BlockState poiState = orientPoi(blockItem.getBlock().defaultBlockState(), state.facing.getOpposite());
            blockModelResolver.update(state.poi, poiState, BlockDisplayContext.create());
            state.poi.tintLayers().clear();
        }
        ItemStack stack = blockEntity.getItem(AutotraderBlockEntity.VILLAGER_SLOT);
        if (stack.isEmpty()) {
            state.clearCache();
            return;
        }
        Entity entity = state.getOrCreateEntity(blockEntity, stack);
        if (entity == null) {
            return;
        }
        orient(entity, state.facing.toYRot());
        PreviewEntityRenderUtil.prepare(entity);
        state.displayEntity = entityRenderer.extractEntity(entity, partialTicks);
        PreviewEntityRenderUtil.applyLight(state.displayEntity, state.lightCoords);
        PreviewEntityRenderUtil.suppressWorldEffects(state.displayEntity);
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        if (state.displayEntity != null) {
            PreviewEntityRenderUtil.applyLight(state.displayEntity, state.lightCoords);
            poseStack.pushPose();
            poseStack.translate(
                    0.5D - state.facing.getStepX() * 0.16D,
                    0.12D,
                    0.5D - state.facing.getStepZ() * 0.16D
            );
            poseStack.scale(ENTITY_SCALE, ENTITY_SCALE, ENTITY_SCALE);
            entityRenderer.submit(state.displayEntity, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
            poseStack.popPose();
        }

        if (!state.poi.isEmpty()) {
            poseStack.pushPose();
            double centerX = 0.5D + state.facing.getStepX() * 0.15D;
            double centerZ = 0.5D + state.facing.getStepZ() * 0.15D;
            poseStack.translate(centerX - POI_SCALE * 0.5D, 0.14D, centerZ - POI_SCALE * 0.5D);
            poseStack.scale(POI_SCALE, POI_SCALE, POI_SCALE);
            state.poi.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
            poseStack.popPose();
        }
    }

    private static BlockState orientPoi(BlockState state, Direction facing) {
        if (state.is(Blocks.GRINDSTONE) && state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, facing);
        }
        return state;
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
    public @NonNull AABB getRenderBoundingBox(AutotraderBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockModelRenderState poi = new BlockModelRenderState();
        public @Nullable EntityRenderState displayEntity;
        public Direction facing = Direction.NORTH;
        private ItemStack cachedStack = ItemStack.EMPTY;
        private @Nullable Entity cachedEntity;

        private @Nullable Entity getOrCreateEntity(AutotraderBlockEntity blockEntity, ItemStack stack) {
            if (cachedEntity == null || !ItemStack.isSameItemSameComponents(cachedStack, stack)) {
                cachedEntity = CapturedMobStackAdapter.createEntity(
                        CapturedMobKind.VILLAGER,
                        blockEntity.getLevel(),
                        stack,
                        blockEntity.getBlockPos()
                );
                cachedStack = stack.copy();
            }
            return cachedEntity;
        }

        private void clearCache() {
            cachedStack = ItemStack.EMPTY;
            cachedEntity = null;
        }
    }
}
