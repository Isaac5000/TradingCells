package com.cosmocraft.trading_cells.feature.incubators.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlock;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlockEntity;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class IncubatorBlockEntityRenderer implements BlockEntityRenderer<IncubatorBlockEntity, IncubatorBlockEntityRenderer.State> {
    private static final float ADULT_SCALE = 0.32F;
    private static final float BABY_SCALE = 0.42F;
    private static final double ENTITY_Y = 0.12D;

    private final EntityRenderDispatcher entityRenderer;

    public IncubatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull IncubatorBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(IncubatorBlock.FACING);
        state.kind = blockEntity.kind();
        state.displayEntity = null;

        if (blockEntity.getLevel() == null) {
            state.clearCachedEntity();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        );

        ItemStack displayStack = blockEntity.copyDisplayStack();
        if (displayStack.isEmpty()) {
            state.clearCachedEntity();
            return;
        }

        Entity entity = state.getOrCreateEntity(blockEntity, displayStack);
        if (entity == null) {
            return;
        }

        boolean baby = CapturedMobStackAdapter.isBaby(state.kind, displayStack);
        if (state.kind == CapturedMobKind.PIGLIN && baby && entity instanceof LivingEntity livingEntity) {
            livingEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            livingEntity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        orientForIncubator(entity, state.facing.toYRot());
        PreviewEntityRenderUtil.prepare(entity);
        state.displayEntity = entityRenderer.extractEntity(entity, partialTicks);
        PreviewEntityRenderUtil.applyLight(state.displayEntity, state.lightCoords);
        PreviewEntityRenderUtil.suppressWorldEffects(state.displayEntity);
        state.scale = baby ? BABY_SCALE : ADULT_SCALE;
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        if (state.displayEntity == null) {
            return;
        }

        PreviewEntityRenderUtil.applyLight(state.displayEntity, state.lightCoords);
        poseStack.pushPose();
        poseStack.translate(0.5D, ENTITY_Y, 0.5D);
        poseStack.scale(state.scale, state.scale, state.scale);
        entityRenderer.submit(state.displayEntity, camera, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
        poseStack.popPose();
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(IncubatorBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    private static void orientForIncubator(Entity entity, float yaw) {
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

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState displayEntity;
        public float scale = BABY_SCALE;
        public Direction facing = Direction.NORTH;
        public CapturedMobKind kind = CapturedMobKind.VILLAGER;
        private ItemStack cachedStack = ItemStack.EMPTY;
        private @Nullable Entity cachedEntity;

        private @Nullable Entity getOrCreateEntity(IncubatorBlockEntity blockEntity, ItemStack stack) {
            if (cachedEntity == null || !ItemStack.isSameItemSameComponents(cachedStack, stack)) {
                cachedEntity = CapturedMobStackAdapter.createEntity(
                        blockEntity.kind(),
                        blockEntity.getLevel(),
                        stack,
                        blockEntity.getBlockPos()
                );
                cachedStack = stack.copy();
            }
            return cachedEntity;
        }

        private void clearCachedEntity() {
            cachedEntity = null;
            cachedStack = ItemStack.EMPTY;
        }
    }
}
