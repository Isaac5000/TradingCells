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

        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());

        Direction bedFacing = state.facing.getOpposite();
        BlockState bedFoot = createBedState(state.kind, bedFacing, BedPart.FOOT);
        BlockState bedHead = createBedState(state.kind, bedFacing, BedPart.HEAD);
        if (bedFoot != state.cachedBedFoot) {
            state.bedFoot.clear();
            blockModelResolver.update(state.bedFoot, bedFoot, BlockDisplayContext.create());
            state.bedFoot.tintLayers().clear();
            state.cachedBedFoot = bedFoot;
        }
        if (bedHead != state.cachedBedHead) {
            state.bedHead.clear();
            blockModelResolver.update(state.bedHead, bedHead, BlockDisplayContext.create());
            state.bedHead.tintLayers().clear();
            state.cachedBedHead = bedHead;
        }

        Entity parentA = state.getOrCreateParent(0, blockEntity, level);
        Entity parentB = state.getOrCreateParent(1, blockEntity, level);
        state.parentA = extractParent(parentA, state.facing.getClockWise(), partialTicks, state.lightCoords);
        state.parentB = extractParent(parentB, state.facing.getCounterClockWise(), partialTicks, state.lightCoords);
    }

    static BlockState createBedState(BreederKind kind, Direction facing, BedPart part) {
        return (kind == BreederKind.VILLAGER ? Blocks.BED.yellow() : Blocks.BED.red())
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BedBlock.PART, part);
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector submitNodeCollector, @NonNull CameraRenderState camera) {
        Direction bedFacing = state.facing.getOpposite();
        submitBedPart(state.bedFoot, bedFacing, false, poseStack, submitNodeCollector, state.lightCoords);
        submitBedPart(state.bedHead, bedFacing, true, poseStack, submitNodeCollector, state.lightCoords);

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

    private @Nullable EntityRenderState extractParent(
            @Nullable Entity entity,
            Direction lookDirection,
            float partialTicks,
            int lightCoords
    ) {
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
        public float parentAScale = ADULT_SCALE;
        public float parentBScale = ADULT_SCALE;
        public Direction facing = Direction.NORTH;
        public BreederKind kind = BreederKind.VILLAGER;
        private BlockState cachedBedFoot = Blocks.AIR.defaultBlockState();
        private BlockState cachedBedHead = Blocks.AIR.defaultBlockState();
        private final ItemStack[] cachedParentStacks = {ItemStack.EMPTY, ItemStack.EMPTY};
        private final Entity[] cachedParents = new Entity[2];
        private final BreederKind[] cachedParentKinds = {BreederKind.VILLAGER, BreederKind.VILLAGER};

        private @Nullable Entity getOrCreateParent(
                int index,
                BreederBlockEntity blockEntity,
                Level level
        ) {
            int slot = index == 0 ? BreederBlockEntity.PARENT_A_SLOT : BreederBlockEntity.PARENT_B_SLOT;
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.isEmpty()) {
                cachedParentStacks[index] = ItemStack.EMPTY;
                cachedParents[index] = null;
                return null;
            }
            if (cachedParents[index] == null
                    || cachedParentKinds[index] != blockEntity.kind()
                    || !ItemStack.isSameItemSameComponents(cachedParentStacks[index], stack)) {
                CapturedMobKind capturedKind = blockEntity.kind() == BreederKind.VILLAGER
                        ? CapturedMobKind.VILLAGER
                        : CapturedMobKind.PIGLIN;
                cachedParents[index] = CapturedMobStackAdapter.createEntity(
                        capturedKind,
                        level,
                        stack,
                        BlockPos.ZERO
                );
                cachedParentStacks[index] = stack.copy();
                cachedParentKinds[index] = blockEntity.kind();
            }
            return cachedParents[index];
        }

        private void clearCaches() {
            bedFoot.clear();
            bedHead.clear();
            cachedBedFoot = Blocks.AIR.defaultBlockState();
            cachedBedHead = Blocks.AIR.defaultBlockState();
            for (int index = 0; index < cachedParents.length; index++) {
                cachedParentStacks[index] = ItemStack.EMPTY;
                cachedParents[index] = null;
            }
        }
    }
}
