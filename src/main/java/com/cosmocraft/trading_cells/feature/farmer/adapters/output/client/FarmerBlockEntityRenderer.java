package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerCropStackAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FarmerBlockEntityRenderer implements BlockEntityRenderer<FarmerBlockEntity, FarmerBlockEntityRenderer.State> {
    private static final float ENTITY_SCALE = 0.30F;
    private static final float PLOT_SCALE = 0.30F;
    private static final float WALL_SUPPORT_BLOCK_SIZE = PLOT_SCALE * 0.80F;
    private static final float WALL_CROP_MAX_SCALE = 0.24F;
    private static final int WALL_SUPPORT_BLOCK_COUNT = 1;
    private static final float WALL_SUPPORT_HEIGHT = WALL_SUPPORT_BLOCK_SIZE * WALL_SUPPORT_BLOCK_COUNT;
    private static final double WALL_SUPPORT_BASE_Y = 0.10D;
    private static final double WALL_SUPPORT_BACK_OFFSET = 0.04D;
    private static final double WALL_CROP_SURFACE_GAP = 0.008D;
    private static final double WALL_CROP_CENTER_HEIGHT = WALL_SUPPORT_BASE_Y + WALL_SUPPORT_HEIGHT * 0.5D;
    private static final double FLOOR_CROP_HEIGHT = 0.32D;
    private static final float WATER_BASE_HEIGHT = 0.08F;
    private static final double WATER_CONTAINER_Y_OFFSET = 0.025D;
    private static final float WATER_CONTAINER_WIDTH = 0.30F;
    private static final float WATER_WALL_THICKNESS = 0.025F;
    private static final float WATER_WALL_HEIGHT = 0.27F;
    private static final float WATER_WALL_BODY_HEIGHT = WATER_WALL_HEIGHT - WATER_WALL_THICKNESS;
    private static final float WATER_WALL_INNER_SPAN = WATER_CONTAINER_WIDTH - 2.0F * WATER_WALL_THICKNESS;
    private static final float WATER_INNER_WIDTH = 0.245F;
    private static final float WATER_SEDIMENT_HEIGHT = 0.035F;
    private static final float WATER_MEDIUM_HEIGHT = 0.19F;
    private static final float WATER_CROP_SCALE = 0.62F;
    private static final float CEILING_UNIT = 0.09F;
    private static final int CEILING_POST_BLOCK_COUNT = 5;
    private static final double CEILING_BASE_Y = 0.10D;
    private static final double CEILING_POST_OFFSET = CEILING_UNIT * 2.0D;
    private static final double CEILING_CAP_Y = CEILING_BASE_Y
            + CEILING_UNIT * CEILING_POST_BLOCK_COUNT;
    private static final double CEILING_ROOF_Y = CEILING_CAP_Y + CEILING_UNIT;
    private static final double CEILING_MIDDLE_Y = CEILING_ROOF_Y + CEILING_UNIT;
    private static final double CEILING_TOP_Y = CEILING_MIDDLE_Y + CEILING_UNIT;
    private static final double CEILING_SUPPORT_Y = CEILING_ROOF_Y;
    private static final float CEILING_SUPPORT_SIZE = PLOT_SCALE * 0.85F;
    private static final float CEILING_SUPPORT_THICKNESS = 0.01F;
    private static final double CEILING_SUPPORT_FACE_OFFSET = 0.001D;
    private static final double ENTITY_OFFSET = 0.20D;
    private static final double PLOT_OFFSET = 0.20D;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;
    private final Map<FarmerBlockEntity, EntityCache> entityCaches = new WeakHashMap<>();

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
            entityCaches.remove(blockEntity);
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        state.updatePlainsTints(level);

        ItemStack cropStack = blockEntity.getItem(FarmerBlockEntity.CROP_SLOT);
        state.cropScale = PLOT_SCALE * FarmerCropStackAdapter.visualGrowthScale(
                blockEntity.kind(),
                cropStack,
                blockEntity.growthTicks(),
                blockEntity.growthDurationTicks()
        );
        state.renderSupport = FarmerCropStackAdapter.renderSupport(blockEntity.kind(), cropStack);
        BlockState soil = FarmerCropStackAdapter.renderSupportState(
                blockEntity.kind(),
                cropStack,
                state.renderSupport
        );
        state.cachedSoil = updateBlockState(state.soil, soil, state.cachedSoil);
        applyPlainsTint(state.soil, soil, state);
        if (state.renderSupport == FarmerCropStackAdapter.RenderSupport.CEILING) {
            updateCeilingStructure(state, blockEntity.kind());
        }
        BlockState containerGlass = state.renderSupport == FarmerCropStackAdapter.RenderSupport.WATER
                ? Blocks.GLASS.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        state.cachedContainerGlass = updateBlockState(
                state.containerGlass,
                containerGlass,
                state.cachedContainerGlass
        );
        BlockState liquidMedium = FarmerCropStackAdapter.renderFluidVisualState(
                blockEntity.kind(),
                cropStack,
                state.renderSupport
        );
        state.cachedLiquidMedium = updateBlockState(
                state.liquidMedium,
                liquidMedium,
                state.cachedLiquidMedium
        );
        BlockState cropState = FarmerCropStackAdapter.cropState(
                blockEntity.kind(),
                cropStack,
                blockEntity.growthTicks(),
                blockEntity.growthDurationTicks()
        );
        if (state.renderSupport == FarmerCropStackAdapter.RenderSupport.WALL) {
            cropState = orientWallCrop(cropState, state.facing.getOpposite());
        }
        updateWallCropGeometry(state, cropState);
        state.cachedCrop = updateBlockState(state.crop, cropState, state.cachedCrop);
        applyPlainsTint(state.crop, cropState, state);

        EntityCache entityCache = entityCaches.computeIfAbsent(blockEntity, ignored -> new EntityCache());
        Entity entity = entityCache.getOrCreateWorker(blockEntity, level);
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
        }
        return nextState;
    }

    private static void applyPlainsTint(
            BlockModelRenderState renderState,
            BlockState blockState,
            State state
    ) {
        int tintColor = blockState.getBlock() instanceof LeavesBlock
                || blockState.getBlock() instanceof VineBlock
                ? state.plainsFoliageTint
                : state.plainsGrassTint;
        var tintLayers = renderState.tintLayers();
        for (int index = 0; index < tintLayers.size(); index++) {
            tintLayers.set(index, tintColor);
        }
    }

    private static BlockState orientWallCrop(BlockState cropState, Direction attachedFace) {
        if (cropState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return cropState.setValue(BlockStateProperties.HORIZONTAL_FACING, attachedFace);
        }
        if (cropState.hasProperty(BlockStateProperties.FACING)) {
            return cropState.setValue(BlockStateProperties.FACING, attachedFace);
        }
        if (cropState.getBlock() instanceof VineBlock) {
            return cropState.setValue(VineBlock.getPropertyForFace(attachedFace), true);
        }
        if (cropState.getBlock() instanceof MultifaceBlock) {
            return cropState.setValue(MultifaceBlock.getFaceProperty(attachedFace), true);
        }
        return cropState;
    }

    private static void updateWallCropGeometry(State state, BlockState cropState) {
        if (state.renderSupport != FarmerCropStackAdapter.RenderSupport.WALL || cropState.isAir()) {
            state.resetWallCropGeometry();
            return;
        }
        if (state.cachedWallGeometryState == cropState
                && state.cachedWallGeometryFacing == state.facing) {
            return;
        }

        VoxelShape shape = cropState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        state.wallCropInnerInset = 0.0D;
        state.wallCropVerticalCenter = 0.5D;
        state.wallCropCoversFace = false;
        if (!shape.isEmpty()) {
            Direction.Axis wallAxis = state.facing.getAxis();
            int outwardStep = wallAxis == Direction.Axis.X
                    ? state.facing.getStepX()
                    : state.facing.getStepZ();
            double innerInset = outwardStep > 0
                    ? shape.min(wallAxis)
                    : 1.0D - shape.max(wallAxis);
            state.wallCropInnerInset = Math.clamp(innerInset, 0.0D, 0.5D);
            state.wallCropVerticalCenter = Math.clamp(
                    (shape.min(Direction.Axis.Y) + shape.max(Direction.Axis.Y)) * 0.5D,
                    0.0D,
                    1.0D
            );
            double verticalSpan = shape.max(Direction.Axis.Y) - shape.min(Direction.Axis.Y);
            double depthSpan = shape.max(wallAxis) - shape.min(wallAxis);
            state.wallCropCoversFace = verticalSpan >= 0.85D && depthSpan <= 0.25D;
        }
        state.cachedWallGeometryState = cropState;
        state.cachedWallGeometryFacing = state.facing;
    }

    private void updateCeilingStructure(State state, FarmerKind farmerKind) {
        boolean piglin = farmerKind == FarmerKind.PIGLIN;
        BlockState wall = (piglin ? Blocks.BLACKSTONE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState();
        BlockState cobblestone = (piglin ? Blocks.BLACKSTONE : Blocks.COBBLESTONE).defaultBlockState();
        BlockState slab = (piglin ? Blocks.BLACKSTONE_SLAB : Blocks.COBBLESTONE_SLAB).defaultBlockState();
        BlockState stair = (piglin ? Blocks.BLACKSTONE_STAIRS : Blocks.COBBLESTONE_STAIRS).defaultBlockState();
        BlockState frontStair = stair.setValue(
                BlockStateProperties.HORIZONTAL_FACING,
                state.facing.getOpposite()
        );
        BlockState backStair = stair.setValue(
                BlockStateProperties.HORIZONTAL_FACING,
                state.facing
        );
        Direction side = state.facing.getClockWise();
        BlockState leftStair = stair.setValue(
                BlockStateProperties.HORIZONTAL_FACING,
                side
        );
        BlockState rightStair = stair.setValue(
                BlockStateProperties.HORIZONTAL_FACING,
                side.getOpposite()
        );
        state.cachedCeilingWall = updateBlockState(state.ceilingWall, wall, state.cachedCeilingWall);
        state.cachedCeilingCobblestone = updateBlockState(
                state.ceilingCobblestone,
                cobblestone,
                state.cachedCeilingCobblestone
        );
        state.cachedCeilingSlab = updateBlockState(state.ceilingSlab, slab, state.cachedCeilingSlab);
        state.cachedCeilingFrontStair = updateBlockState(
                state.ceilingFrontStair,
                frontStair,
                state.cachedCeilingFrontStair
        );
        state.cachedCeilingBackStair = updateBlockState(
                state.ceilingBackStair,
                backStair,
                state.cachedCeilingBackStair
        );
        state.cachedCeilingLeftStair = updateBlockState(
                state.ceilingLeftStair,
                leftStair,
                state.cachedCeilingLeftStair
        );
        state.cachedCeilingRightStair = updateBlockState(
                state.ceilingRightStair,
                rightStair,
                state.cachedCeilingRightStair
        );
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
        submitPlot(state, plotX, plotZ, poseStack, submitNodeCollector);

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

    private static void submitPlot(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        switch (state.renderSupport) {
            case FLOOR -> submitFloorPlot(state, plotX, plotZ, poseStack, collector);
            case WALL -> submitWallPlot(state, plotX, plotZ, poseStack, collector);
            case WATER -> submitWaterPlot(state, plotX, plotZ, poseStack, collector);
            case CEILING -> submitCeilingPlot(state, plotX, plotZ, poseStack, collector);
        }
    }

    private static void submitFloorPlot(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitBlock(state.soil, new Vec3(plotX, 0.02D, plotZ), PLOT_SCALE, state.lightCoords, poseStack, collector);
        submitBlock(
                state.crop,
                new Vec3(plotX, FLOOR_CROP_HEIGHT, plotZ),
                state.cropScale,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitWallPlot(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        double supportX = plotX - state.facing.getStepX() * WALL_SUPPORT_BACK_OFFSET;
        double supportZ = plotZ - state.facing.getStepZ() * WALL_SUPPORT_BACK_OFFSET;
        for (int block = 0; block < WALL_SUPPORT_BLOCK_COUNT; block++) {
            submitBlock(
                    state.soil,
                    new Vec3(
                            supportX,
                            WALL_SUPPORT_BASE_Y + block * WALL_SUPPORT_BLOCK_SIZE,
                            supportZ
                    ),
                    WALL_SUPPORT_BLOCK_SIZE,
                    state.lightCoords,
                    poseStack,
                    collector
            );
        }

        float growthScale = Math.clamp(state.cropScale / PLOT_SCALE, 0.20F, 1.0F);
        float regularCropScale = Math.min(state.cropScale, WALL_CROP_MAX_SCALE);
        float cropSideScale = state.wallCropCoversFace
                ? WALL_SUPPORT_BLOCK_SIZE * growthScale
                : regularCropScale;
        float cropVerticalScale = state.wallCropCoversFace
                ? WALL_SUPPORT_HEIGHT * growthScale
                : regularCropScale;
        float cropDepthScale = regularCropScale;
        double cropOffset = WALL_SUPPORT_BLOCK_SIZE * 0.5D
                + cropDepthScale * (0.5D - state.wallCropInnerInset)
                + WALL_CROP_SURFACE_GAP;
        double cropHeight = WALL_CROP_CENTER_HEIGHT
                - cropVerticalScale * state.wallCropVerticalCenter;
        boolean alongX = state.facing.getAxis() == Direction.Axis.X;
        submitBlock(
                state.crop,
                new Vec3(
                        supportX + state.facing.getStepX() * cropOffset,
                        cropHeight,
                        supportZ + state.facing.getStepZ() * cropOffset
                ),
                alongX ? cropDepthScale : cropSideScale,
                cropVerticalScale,
                alongX ? cropSideScale : cropDepthScale,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitWaterPlot(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitBlock(
                state.soil,
                new Vec3(plotX, 0.02D, plotZ),
                PLOT_SCALE,
                WATER_BASE_HEIGHT,
                PLOT_SCALE,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.containerGlass,
                new Vec3(plotX, 0.10D + WATER_CONTAINER_Y_OFFSET, plotZ),
                WATER_CONTAINER_WIDTH,
                WATER_WALL_THICKNESS,
                WATER_CONTAINER_WIDTH,
                state.lightCoords,
                poseStack,
                collector
        );
        submitWaterWalls(state, plotX, plotZ, poseStack, collector);
        submitBlock(
                state.soil,
                new Vec3(plotX, 0.125D + WATER_CONTAINER_Y_OFFSET, plotZ),
                WATER_INNER_WIDTH,
                WATER_SEDIMENT_HEIGHT,
                WATER_INNER_WIDTH,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.liquidMedium,
                new Vec3(plotX, 0.16D + WATER_CONTAINER_Y_OFFSET, plotZ),
                WATER_INNER_WIDTH,
                WATER_MEDIUM_HEIGHT,
                WATER_INNER_WIDTH,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.crop,
                new Vec3(plotX, 0.16D + WATER_CONTAINER_Y_OFFSET, plotZ),
                state.cropScale * WATER_CROP_SCALE,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitWaterWalls(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        double wallOffset = (WATER_CONTAINER_WIDTH - WATER_WALL_THICKNESS) * 0.5D;
        double wallBase = 0.10D + WATER_CONTAINER_Y_OFFSET + WATER_WALL_THICKNESS;
        submitBlock(
                state.containerGlass,
                new Vec3(plotX - wallOffset, wallBase, plotZ),
                WATER_WALL_THICKNESS,
                WATER_WALL_BODY_HEIGHT,
                WATER_CONTAINER_WIDTH,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.containerGlass,
                new Vec3(plotX + wallOffset, wallBase, plotZ),
                WATER_WALL_THICKNESS,
                WATER_WALL_BODY_HEIGHT,
                WATER_CONTAINER_WIDTH,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.containerGlass,
                new Vec3(plotX, wallBase, plotZ - wallOffset),
                WATER_WALL_INNER_SPAN,
                WATER_WALL_BODY_HEIGHT,
                WATER_WALL_THICKNESS,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.containerGlass,
                new Vec3(plotX, wallBase, plotZ + wallOffset),
                WATER_WALL_INNER_SPAN,
                WATER_WALL_BODY_HEIGHT,
                WATER_WALL_THICKNESS,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitCeilingPlot(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        Direction side = state.facing.getClockWise();
        submitCeilingPillar(state, plotX, plotZ, side, -1, poseStack, collector);
        submitCeilingPillar(state, plotX, plotZ, side, 1, poseStack, collector);
        submitCeilingRoof(state, plotX, plotZ, poseStack, collector);
        float cropScale = state.cropScale * 0.85F;
        double supportFaceY = CEILING_SUPPORT_Y - CEILING_SUPPORT_FACE_OFFSET;
        submitBlock(
                state.soil,
                new Vec3(plotX, supportFaceY, plotZ),
                CEILING_SUPPORT_SIZE,
                CEILING_SUPPORT_THICKNESS,
                CEILING_SUPPORT_SIZE,
                state.lightCoords,
                poseStack,
                collector
        );
        submitBlock(
                state.crop,
                new Vec3(plotX, supportFaceY - cropScale, plotZ),
                cropScale,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitCeilingPillar(
            State state,
            double plotX,
            double plotZ,
            Direction side,
            int sign,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        double pillarX = plotX + side.getStepX() * CEILING_POST_OFFSET * sign;
        double pillarZ = plotZ + side.getStepZ() * CEILING_POST_OFFSET * sign;
        for (int block = 0; block < CEILING_POST_BLOCK_COUNT; block++) {
            submitBlock(
                    state.ceilingWall,
                    new Vec3(pillarX, CEILING_BASE_Y + block * CEILING_UNIT, pillarZ),
                    CEILING_UNIT,
                    state.lightCoords,
                    poseStack,
                    collector
            );
        }
        submitBlock(
                state.ceilingWall,
                new Vec3(pillarX, CEILING_CAP_Y, pillarZ),
                CEILING_UNIT,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitCeilingRoof(
            State state,
            double plotX,
            double plotZ,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitCeilingRoofLayer(state, plotX, plotZ, 2, CEILING_ROOF_Y, true, poseStack, collector);
        submitCeilingRoofLayer(state, plotX, plotZ, 1, CEILING_MIDDLE_Y, false, poseStack, collector);
        submitCeilingRoofPiece(
                state.ceilingSlab,
                state,
                plotX,
                CEILING_TOP_Y,
                plotZ,
                poseStack,
                collector
        );
    }

    private static void submitCeilingRoofLayer(
            State state,
            double plotX,
            double plotZ,
            int radius,
            double layerY,
            boolean leaveCenterForSoil,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        Direction side = state.facing.getClockWise();
        for (int forwardOffset = -radius; forwardOffset <= radius; forwardOffset++) {
            for (int sideOffset = -radius; sideOffset <= radius; sideOffset++) {
                if (leaveCenterForSoil && forwardOffset == 0 && sideOffset == 0) {
                    continue;
                }
                submitCeilingRoofPiece(
                        ceilingRoofState(state, radius, forwardOffset, sideOffset),
                        state,
                        plotX + state.facing.getStepX() * CEILING_UNIT * forwardOffset
                                + side.getStepX() * CEILING_UNIT * sideOffset,
                        layerY,
                        plotZ + state.facing.getStepZ() * CEILING_UNIT * forwardOffset
                                + side.getStepZ() * CEILING_UNIT * sideOffset,
                        poseStack,
                        collector
                );
            }
        }
    }

    private static BlockModelRenderState ceilingRoofState(
            State state,
            int radius,
            int forwardOffset,
            int sideOffset
    ) {
        if (forwardOffset == -radius) {
            return state.ceilingBackStair;
        }
        if (forwardOffset == radius) {
            return state.ceilingFrontStair;
        }
        if (sideOffset == -radius) {
            return state.ceilingLeftStair;
        }
        if (sideOffset == radius) {
            return state.ceilingRightStair;
        }
        return state.ceilingCobblestone;
    }

    private static void submitCeilingRoofPiece(
            BlockModelRenderState renderState,
            State state,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitBlock(
                renderState,
                new Vec3(x, y, z),
                CEILING_UNIT,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitBlock(
            BlockModelRenderState state,
            Vec3 position,
            float scale,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitBlock(state, position, scale, scale, scale, lightCoords, poseStack, collector);
    }

    private static void submitBlock(
            BlockModelRenderState state,
            Vec3 position,
            float scaleX,
            float scaleY,
            float scaleZ,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (state.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(
                position.x() - scaleX * 0.5D,
                position.y(),
                position.z() - scaleZ * 0.5D
        );
        poseStack.scale(scaleX, scaleY, scaleZ);
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
        public final BlockModelRenderState containerGlass = new BlockModelRenderState();
        public final BlockModelRenderState liquidMedium = new BlockModelRenderState();
        public final BlockModelRenderState ceilingWall = new BlockModelRenderState();
        public final BlockModelRenderState ceilingCobblestone = new BlockModelRenderState();
        public final BlockModelRenderState ceilingSlab = new BlockModelRenderState();
        public final BlockModelRenderState ceilingFrontStair = new BlockModelRenderState();
        public final BlockModelRenderState ceilingBackStair = new BlockModelRenderState();
        public final BlockModelRenderState ceilingLeftStair = new BlockModelRenderState();
        public final BlockModelRenderState ceilingRightStair = new BlockModelRenderState();
        public @Nullable EntityRenderState worker;
        public Direction facing = Direction.NORTH;
        public FarmerCropStackAdapter.RenderSupport renderSupport = FarmerCropStackAdapter.RenderSupport.FLOOR;
        public float cropScale = PLOT_SCALE;
        private BlockState cachedSoil = Blocks.AIR.defaultBlockState();
        private BlockState cachedCrop = Blocks.AIR.defaultBlockState();
        private BlockState cachedContainerGlass = Blocks.AIR.defaultBlockState();
        private BlockState cachedLiquidMedium = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingWall = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingCobblestone = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingSlab = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingFrontStair = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingBackStair = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingLeftStair = Blocks.AIR.defaultBlockState();
        private BlockState cachedCeilingRightStair = Blocks.AIR.defaultBlockState();
        private BlockState cachedWallGeometryState = Blocks.AIR.defaultBlockState();
        private Direction cachedWallGeometryFacing = Direction.NORTH;
        private double wallCropInnerInset;
        private double wallCropVerticalCenter = 0.5D;
        private boolean wallCropCoversFace;
        private int plainsGrassTint = ARGB.opaque(0x91BD59);
        private int plainsFoliageTint = ARGB.opaque(0x77AB2F);
        private @Nullable Level cachedTintLevel;

        private void updatePlainsTints(Level level) {
            if (cachedTintLevel == level) {
                return;
            }
            Biome plains = level.registryAccess()
                    .lookupOrThrow(Registries.BIOME)
                    .getOrThrow(Biomes.PLAINS)
                    .value();
            plainsGrassTint = ARGB.opaque(plains.getGrassColor(0.0D, 0.0D));
            plainsFoliageTint = ARGB.opaque(plains.getFoliageColor());
            cachedTintLevel = level;
        }

        private void resetWallCropGeometry() {
            cachedWallGeometryState = Blocks.AIR.defaultBlockState();
            cachedWallGeometryFacing = Direction.NORTH;
            wallCropInnerInset = 0.0D;
            wallCropVerticalCenter = 0.5D;
            wallCropCoversFace = false;
        }

        private void clearCaches() {
            soil.clear();
            crop.clear();
            containerGlass.clear();
            liquidMedium.clear();
            ceilingWall.clear();
            ceilingCobblestone.clear();
            ceilingSlab.clear();
            ceilingFrontStair.clear();
            ceilingBackStair.clear();
            ceilingLeftStair.clear();
            ceilingRightStair.clear();
            cachedSoil = Blocks.AIR.defaultBlockState();
            cachedCrop = Blocks.AIR.defaultBlockState();
            cachedContainerGlass = Blocks.AIR.defaultBlockState();
            cachedLiquidMedium = Blocks.AIR.defaultBlockState();
            cachedCeilingWall = Blocks.AIR.defaultBlockState();
            cachedCeilingCobblestone = Blocks.AIR.defaultBlockState();
            cachedCeilingSlab = Blocks.AIR.defaultBlockState();
            cachedCeilingFrontStair = Blocks.AIR.defaultBlockState();
            cachedCeilingBackStair = Blocks.AIR.defaultBlockState();
            cachedCeilingLeftStair = Blocks.AIR.defaultBlockState();
            cachedCeilingRightStair = Blocks.AIR.defaultBlockState();
            resetWallCropGeometry();
            renderSupport = FarmerCropStackAdapter.RenderSupport.FLOOR;
            cropScale = PLOT_SCALE;
            cachedTintLevel = null;
        }
    }

    private static final class EntityCache {
        private ItemStack cachedWorkerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedWorker;
        private FarmerKind cachedKind = FarmerKind.VILLAGER;

        private @Nullable Entity getOrCreateWorker(FarmerBlockEntity blockEntity, Level level) {
            ItemStack workerStack = blockEntity.getItem(FarmerBlockEntity.WORKER_SLOT);
            if (workerStack.isEmpty()) {
                cachedWorkerStack = ItemStack.EMPTY;
                cachedWorker = null;
                return null;
            }
            if (cachedWorker == null
                    || cachedKind != blockEntity.kind()
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
                cachedKind = blockEntity.kind();
            }
            return cachedWorker;
        }
    }
}
