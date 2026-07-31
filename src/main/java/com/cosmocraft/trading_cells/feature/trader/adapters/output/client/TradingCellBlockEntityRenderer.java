package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.MachineEntityRenderScales;
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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
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

public final class TradingCellBlockEntityRenderer implements BlockEntityRenderer<VillagerTradingCellBlockEntity, TradingCellBlockEntityRenderer.State> {
    private static final float ADULT_DISPLAY_SCALE = MachineEntityRenderScales.TRADER_ENTITY;
    private static final float BABY_DISPLAY_SCALE = ADULT_DISPLAY_SCALE;
    private static final float POI_BLOCK_SCALE = 0.23466668F;
    private static final double ENTITY_BACK_OFFSET = 0.18D;
    private static final double POI_FRONT_OFFSET = 0.12D;
    private static final double POI_BASE_Y = 0.14D;

    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;

    public TradingCellBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull VillagerTradingCellBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (blockEntity.getLevel() != null) {
            state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos());
        }
        state.displayEntity = null;
        state.scale = ADULT_DISPLAY_SCALE;
        state.facing = blockEntity.getBlockState().getValue(VillagerTradingCellBlock.FACING);
        state.poiState.clear();

        ItemStack poiStack = blockEntity.copyPoiStack();
        if (!poiStack.isEmpty() && poiStack.getItem() instanceof BlockItem blockItem) {
            BlockState poiBlockState = orientPoiBlockState(blockItem.getBlock().defaultBlockState(), state.facing.getOpposite());
            blockModelResolver.update(state.poiState, poiBlockState, BlockDisplayContext.create());
            state.poiState.tintLayers().clear();
        }

        CompoundTag entityData = blockEntity.copyStoredEntityData();
        String entityKindId = blockEntity.getStoredEntityKindId();
        if (entityData == null || entityKindId == null) {
            state.clearCachedEntity();
            return;
        }

        Entity entity = state.getOrCreateEntity(blockEntity, entityData, entityKindId);
        if (entity == null) {
            return;
        }

        orientForCellPreview(entity, state.facing.toYRot());
        PreviewEntityRenderUtil.prepare(entity);
        state.displayEntity = entityRenderer.extractEntity(entity, partialTicks);
        PreviewEntityRenderUtil.applyLight(state.displayEntity, state.lightCoords);
        PreviewEntityRenderUtil.suppressWorldEffects(state.displayEntity);
        state.scale = isBaby(entity) ? BABY_DISPLAY_SCALE : ADULT_DISPLAY_SCALE;
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector submitNodeCollector, @NonNull CameraRenderState camera) {
        double stepX = state.facing.getStepX();
        double stepZ = state.facing.getStepZ();

        if (state.displayEntity != null) {
            PreviewEntityRenderUtil.applyLight(state.displayEntity, state.lightCoords);
            poseStack.pushPose();
            poseStack.translate(0.5D - stepX * ENTITY_BACK_OFFSET, 0.1D, 0.5D - stepZ * ENTITY_BACK_OFFSET);
            poseStack.scale(state.scale, state.scale, state.scale);
            entityRenderer.submit(state.displayEntity, camera, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
            poseStack.popPose();
        }

        if (!state.poiState.isEmpty()) {
            poseStack.pushPose();
            double centerX = 0.5D + stepX * POI_FRONT_OFFSET;
            double centerZ = 0.5D + stepZ * POI_FRONT_OFFSET;
            poseStack.translate(centerX - POI_BLOCK_SCALE * 0.5D, POI_BASE_Y, centerZ - POI_BLOCK_SCALE * 0.5D);
            poseStack.scale(POI_BLOCK_SCALE, POI_BLOCK_SCALE, POI_BLOCK_SCALE);
            state.poiState.submit(poseStack, submitNodeCollector, state.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
            poseStack.popPose();
        }
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(VillagerTradingCellBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    private static BlockState orientPoiBlockState(BlockState state, Direction facing) {
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

    private static void orientForCellPreview(Entity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setXRot(0.0F);
        entity.yRotO = yaw;
        entity.xRotO = 0.0F;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.yHeadRot = yaw;
            livingEntity.yHeadRotO = yaw;
            livingEntity.yBodyRot = yaw;
            livingEntity.yBodyRotO = yaw;
        }
    }

    private static boolean isBaby(Entity entity) {
        if (entity instanceof Villager villager) {
            return villager.isBaby();
        }
        if (entity instanceof Piglin piglin) {
            return piglin.isBaby();
        }
        return false;
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState displayEntity;
        public float scale = ADULT_DISPLAY_SCALE;
        public Direction facing = Direction.NORTH;
        public final BlockModelRenderState poiState = new BlockModelRenderState();
        private @Nullable CompoundTag cachedEntityData;
        private @Nullable String cachedEntityKindId;
        private @Nullable Entity cachedEntity;

        private @Nullable Entity getOrCreateEntity(VillagerTradingCellBlockEntity blockEntity, CompoundTag entityData, String entityKindId) {
            if (cachedEntity == null
                    || cachedEntityData == null
                    || cachedEntityKindId == null
                    || !cachedEntityData.equals(entityData)
                    || !cachedEntityKindId.equals(entityKindId)) {
                cachedEntity = blockEntity.createStoredEntityForDisplay();
                cachedEntityData = entityData.copy();
                cachedEntityKindId = entityKindId;
            }
            return cachedEntity;
        }

        private void clearCachedEntity() {
            cachedEntity = null;
            cachedEntityData = null;
            cachedEntityKindId = null;
        }
    }
}
