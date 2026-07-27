package com.cosmocraft.trading_cells.feature.breeders.adapters.output.client;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederBlock;
import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederBlockEntity;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BreederBlockEntityRenderer implements BlockEntityRenderer<BreederBlockEntity, BreederBlockEntityRenderer.State> {
    private static final float ADULT_SCALE = MachineEntityRenderScales.VILLAGER_BREEDER_ENTITY;
    private static final float BABY_SCALE = 0.25F;
    private static final double PARENT_SIDE_OFFSET = 0.23D;
    private static final double PARENT_BACK_OFFSET = 0.05D;
    private static final double ENTITY_Y = 0.11D;
    private static final double BED_Y = 0.12D;
    private static final double BED_SCALE = 0.26D;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;

    public BreederBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull BreederBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(BreederBlock.FACING);
        state.kind = blockEntity.kind();
        state.parentA = null;
        state.parentB = null;
        state.baby = null;
        state.bedFoot.clear();
        state.bedHead.clear();

        Level level = blockEntity.getLevel();
        if (level != null) {
            state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());

            BlockState bedFoot = createBedState(state.kind, state.facing, BedPart.FOOT);
            BlockState bedHead = createBedState(state.kind, state.facing, BedPart.HEAD);
            blockModelResolver.update(state.bedFoot, bedFoot, BlockDisplayContext.create());
            blockModelResolver.update(state.bedHead, bedHead, BlockDisplayContext.create());
            state.bedFoot.tintLayers().clear();
            state.bedHead.tintLayers().clear();

            state.parentA = extractParent(level, blockEntity.getItem(BreederBlockEntity.PARENT_A_SLOT), state.kind, state.facing.getClockWise(), partialTicks, state.lightCoords);
            state.parentB = extractParent(level, blockEntity.getItem(BreederBlockEntity.PARENT_B_SLOT), state.kind, state.facing.getCounterClockWise(), partialTicks, state.lightCoords);

            ItemStack babyStack = blockEntity.createBabyPreviewStack();
            state.baby = extractParent(level, babyStack, state.kind, state.facing, partialTicks, state.lightCoords);
        }
    }

    static BlockState createBedState(BreederKind kind, Direction facing, BedPart part) {
        return (kind == BreederKind.VILLAGER ? Blocks.BED.yellow() : Blocks.BED.red())
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BedBlock.PART, part);
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector submitNodeCollector, @NonNull CameraRenderState camera) {
        submitBedPart(state.bedFoot, state.facing, false, poseStack, submitNodeCollector, state.lightCoords);
        submitBedPart(state.bedHead, state.facing, true, poseStack, submitNodeCollector, state.lightCoords);

        Direction side = state.facing.getClockWise();
        double sideX = side.getStepX();
        double sideZ = side.getStepZ();
        double backX = -state.facing.getStepX() * PARENT_BACK_OFFSET;
        double backZ = -state.facing.getStepZ() * PARENT_BACK_OFFSET;
        EntitySubmitContext context = new EntitySubmitContext(
                state.lightCoords,
                poseStack,
                submitNodeCollector,
                camera
        );

        submitEntity(
                state.parentA,
                state.parentAScale,
                new Vec3(
                        0.5D - sideX * PARENT_SIDE_OFFSET + backX,
                        ENTITY_Y,
                        0.5D - sideZ * PARENT_SIDE_OFFSET + backZ
                ),
                context
        );
        submitEntity(
                state.parentB,
                state.parentBScale,
                new Vec3(
                        0.5D + sideX * PARENT_SIDE_OFFSET + backX,
                        ENTITY_Y,
                        0.5D + sideZ * PARENT_SIDE_OFFSET + backZ
                ),
                context
        );
    }

    private static void submitBedPart(BlockModelRenderState bedState, Direction facing, boolean head, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        if (bedState.isEmpty()) {
            return;
        }

        double partOffset = (head ? 0.5D : -0.5D) * BED_SCALE;
        double x = 0.5D - BED_SCALE * 0.5D + facing.getStepX() * partOffset;
        double z = 0.5D - BED_SCALE * 0.5D + facing.getStepZ() * partOffset;

        poseStack.pushPose();
        poseStack.translate(x, BED_Y, z);
        poseStack.scale((float) BED_SCALE, (float) BED_SCALE, (float) BED_SCALE);
        bedState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
        poseStack.popPose();
    }

    private void submitEntity(
            @Nullable EntityRenderState entityState,
            float scale,
            Vec3 position,
            EntitySubmitContext context
    ) {
        if (entityState == null) {
            return;
        }
        PreviewEntityRenderUtil.applyLight(entityState, context.lightCoords());
        context.poseStack().pushPose();
        context.poseStack().translate(position.x(), position.y(), position.z());
        context.poseStack().scale(scale, scale, scale);
        entityRenderer.submit(
                entityState,
                context.camera(),
                0.0D,
                0.0D,
                0.0D,
                context.poseStack(),
                context.collector()
        );
        context.poseStack().popPose();
    }

    private @Nullable EntityRenderState extractParent(Level level, ItemStack stack, BreederKind kind, Direction lookDirection, float partialTicks, int lightCoords) {
        Entity entity = null;
        CapturedMobKind capturedKind = kind == BreederKind.VILLAGER
                ? CapturedMobKind.VILLAGER
                : CapturedMobKind.PIGLIN;
        entity = CapturedMobStackAdapter.createEntity(capturedKind, level, stack, BlockPos.ZERO);

        if (entity == null) {
            return null;
        }

        orientForBreeder(entity, lookDirection.toYRot());
        PreviewEntityRenderUtil.prepare(entity);
        EntityRenderState renderState = entityRenderer.extractEntity(entity, partialTicks);
        PreviewEntityRenderUtil.applyLight(renderState, lightCoords);
        PreviewEntityRenderUtil.suppressWorldEffects(renderState);
        return renderState;
    }

    private static void orientForBreeder(Entity entity, float yaw) {
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

    private record EntitySubmitContext(
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(BreederBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockModelRenderState bedFoot = new BlockModelRenderState();
        public final BlockModelRenderState bedHead = new BlockModelRenderState();
        public @Nullable EntityRenderState parentA;
        public @Nullable EntityRenderState parentB;
        public @Nullable EntityRenderState baby;
        public float parentAScale = ADULT_SCALE;
        public float parentBScale = ADULT_SCALE;
        public float babyScale = BABY_SCALE;
        public Direction facing = Direction.NORTH;
        public BreederKind kind = BreederKind.VILLAGER;
    }
}
