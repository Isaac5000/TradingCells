package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.NetheritePiglinBarteringCellBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.MachineEntityRenderScales;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class NetheritePiglinBarteringCellBlockEntityRenderer implements BlockEntityRenderer<
        NetheritePiglinBarteringCellBlockEntity,
        NetheritePiglinBarteringCellBlockEntityRenderer.State> {
    private static final float DISPLAY_SCALE = MachineEntityRenderScales.TRADER_ENTITY;
    private static final double ENTITY_BACK_OFFSET = 0.12D;
    private static final double ENTITY_BASE_Y = 0.10D;
    private static final float OUTPUT_ITEM_SCALE = 0.34F;
    private static final double OUTPUT_ITEM_FRONT_OFFSET = 0.20D;
    private static final double OUTPUT_ITEM_BASE_Y = 0.34D;

    private final EntityRenderDispatcher entityRenderer;
    private final ItemModelResolver itemModelResolver;

    public NetheritePiglinBarteringCellBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull NetheritePiglinBarteringCellBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (blockEntity.getLevel() != null) {
            state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(
                    blockEntity.getLevel(),
                    blockEntity.getBlockPos()
            );
        }
        state.displayPiglin = null;
        state.outputItem.clear();
        state.facing = blockEntity.getBlockState().getValue(NetheritePiglinBarteringCellBlock.FACING);

        CompoundTag piglinData = blockEntity.copyPiglinData();
        if (piglinData == null) {
            state.clearCachedPiglin();
        } else {
            Piglin piglin = state.getOrCreatePiglin(blockEntity, piglinData, blockEntity.isBartering());
            if (piglin != null) {
                orientForCellPreview(piglin, state.facing.toYRot());
                PreviewEntityRenderUtil.prepare(piglin);
                state.displayPiglin = entityRenderer.extractEntity(piglin, partialTicks);
                PreviewEntityRenderUtil.applyLight(state.displayPiglin, state.lightCoords);
                PreviewEntityRenderUtil.suppressWorldEffects(state.displayPiglin);
            }
        }

        ItemStack outputStack = blockEntity.copyFirstOutputStack();
        if (!outputStack.isEmpty()) {
            itemModelResolver.updateForTopItem(
                    state.outputItem,
                    outputStack,
                    ItemDisplayContext.FIXED,
                    blockEntity.getLevel(),
                    null,
                    (int) blockEntity.getBlockPos().asLong()
            );
        }
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        double stepX = state.facing.getStepX();
        double stepZ = state.facing.getStepZ();

        if (state.displayPiglin != null) {
            PreviewEntityRenderUtil.applyLight(state.displayPiglin, state.lightCoords);
            poseStack.pushPose();
            poseStack.translate(0.5D - stepX * ENTITY_BACK_OFFSET, ENTITY_BASE_Y, 0.5D - stepZ * ENTITY_BACK_OFFSET);
            poseStack.scale(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE);
            entityRenderer.submit(state.displayPiglin, camera, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);
            poseStack.popPose();
        }

        if (!state.outputItem.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(
                    0.5D + stepX * OUTPUT_ITEM_FRONT_OFFSET,
                    OUTPUT_ITEM_BASE_Y,
                    0.5D + stepZ * OUTPUT_ITEM_FRONT_OFFSET
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
            poseStack.scale(OUTPUT_ITEM_SCALE, OUTPUT_ITEM_SCALE, OUTPUT_ITEM_SCALE);
            state.outputItem.submit(
                    poseStack,
                    submitNodeCollector,
                    state.lightCoords,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    EntityRenderState.NO_OUTLINE
            );
            poseStack.popPose();
        }
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(NetheritePiglinBarteringCellBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D
        );
    }

    private static void orientForCellPreview(Piglin piglin, float yaw) {
        piglin.setYRot(yaw);
        piglin.setXRot(0.0F);
        piglin.yRotO = yaw;
        piglin.xRotO = 0.0F;
        piglin.yHeadRot = yaw;
        piglin.yHeadRotO = yaw;
        piglin.yBodyRot = yaw;
        piglin.yBodyRotO = yaw;
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState displayPiglin;
        public final ItemStackRenderState outputItem = new ItemStackRenderState();
        public Direction facing = Direction.NORTH;
        private @Nullable CompoundTag cachedPiglinData;
        private @Nullable Piglin cachedPiglin;
        private boolean cachedBartering;

        private @Nullable Piglin getOrCreatePiglin(
                NetheritePiglinBarteringCellBlockEntity blockEntity,
                CompoundTag piglinData,
                boolean bartering
        ) {
            if (cachedPiglin == null
                    || cachedPiglinData == null
                    || !cachedPiglinData.equals(piglinData)
                    || cachedBartering != bartering) {
                cachedPiglin = blockEntity.createPiglinForDisplay();
                cachedPiglinData = piglinData.copy();
                cachedBartering = bartering;
            }
            return cachedPiglin;
        }

        private void clearCachedPiglin() {
            cachedPiglin = null;
            cachedPiglinData = null;
            cachedBartering = false;
        }
    }
}
