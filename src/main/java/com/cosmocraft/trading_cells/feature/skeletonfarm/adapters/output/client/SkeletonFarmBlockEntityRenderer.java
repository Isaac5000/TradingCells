package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SkeletonFarmBlockEntityRenderer implements BlockEntityRenderer<
        SkeletonFarmBlockEntity,
        SkeletonFarmBlockEntityRenderer.State
> {
    private static final float ENTITY_SCALE = 0.27F;
    private static final double SIDE_OFFSET = 0.22D;
    private final EntityRenderDispatcher entityRenderer;

    public SkeletonFarmBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull SkeletonFarmBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.worker = null;
        state.skeleton = null;

        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        Direction side = state.facing.getClockWise();

        Entity worker = state.getOrCreateWorker(blockEntity, level);
        if (worker != null) {
            orient(worker, side.toYRot());
            state.worker = extractEntity(worker, partialTicks, state.lightCoords);
        }

        Entity skeleton = state.getOrCreateSkeleton(level, blockEntity.selectedKind());
        if (skeleton != null) {
            orient(skeleton, side.getOpposite().toYRot());
            state.skeleton = extractEntity(skeleton, partialTicks, state.lightCoords);
        }
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector collector,
            @NonNull CameraRenderState camera
    ) {
        Direction side = state.facing.getClockWise();
        submitEntity(
                state.worker,
                new Vec3(0.5D - side.getStepX() * SIDE_OFFSET, 0.11D, 0.5D - side.getStepZ() * SIDE_OFFSET),
                state,
                poseStack,
                collector,
                camera
        );
        submitEntity(
                state.skeleton,
                new Vec3(0.5D + side.getStepX() * SIDE_OFFSET, 0.11D, 0.5D + side.getStepZ() * SIDE_OFFSET),
                state,
                poseStack,
                collector,
                camera
        );
    }

    private EntityRenderState extractEntity(Entity entity, float partialTicks, int lightCoords) {
        PreviewEntityRenderUtil.prepare(entity);
        EntityRenderState renderState = entityRenderer.extractEntity(entity, partialTicks);
        PreviewEntityRenderUtil.applyLight(renderState, lightCoords);
        PreviewEntityRenderUtil.suppressWorldEffects(renderState);
        return renderState;
    }

    private void submitEntity(
            @Nullable EntityRenderState entity,
            Vec3 position,
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (entity == null) {
            return;
        }
        PreviewEntityRenderUtil.applyLight(entity, state.lightCoords);
        poseStack.pushPose();
        poseStack.translate(position.x(), position.y(), position.z());
        poseStack.scale(ENTITY_SCALE, ENTITY_SCALE, ENTITY_SCALE);
        entityRenderer.submit(entity, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
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
    public @NonNull AABB getRenderBoundingBox(SkeletonFarmBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState worker;
        public @Nullable EntityRenderState skeleton;
        public Direction facing = Direction.NORTH;
        private ItemStack cachedWorkerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedWorker;
        private @Nullable Entity cachedSkeleton;
        private SkeletonFarmKind cachedKind = SkeletonFarmKind.SKELETON;

        private @Nullable Entity getOrCreateWorker(SkeletonFarmBlockEntity blockEntity, Level level) {
            ItemStack workerStack = blockEntity.getItem(SkeletonFarmBlockEntity.WORKER_SLOT);
            if (workerStack.isEmpty()) {
                cachedWorkerStack = ItemStack.EMPTY;
                cachedWorker = null;
                return null;
            }
            if (cachedWorker == null || !ItemStack.isSameItemSameComponents(cachedWorkerStack, workerStack)) {
                cachedWorker = CapturedMobStackAdapter.createEntity(
                        CapturedMobKind.VILLAGER,
                        level,
                        workerStack,
                        blockEntity.getBlockPos()
                );
                if (cachedWorker instanceof Villager villager) {
                    villager.setVillagerData(villager.getVillagerData().withProfession(
                            BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.WEAPONSMITH)
                    ));
                }
                cachedWorkerStack = workerStack.copy();
            }
            return cachedWorker;
        }

        private @Nullable Entity getOrCreateSkeleton(Level level, SkeletonFarmKind kind) {
            if (cachedSkeleton == null || cachedKind != kind) {
                cachedSkeleton = switch (kind) {
                    case SKELETON -> EntityTypes.SKELETON.create(level, EntitySpawnReason.LOAD);
                    case WITHER_SKELETON -> EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.LOAD);
                    case STRAY -> EntityTypes.STRAY.create(level, EntitySpawnReason.LOAD);
                    case BOGGED -> EntityTypes.BOGGED.create(level, EntitySpawnReason.LOAD);
                    case PARCHED -> EntityTypes.PARCHED.create(level, EntitySpawnReason.LOAD);
                };
                if (cachedSkeleton instanceof LivingEntity living) {
                    living.setItemSlot(
                            EquipmentSlot.MAINHAND,
                            new ItemStack(kind == SkeletonFarmKind.WITHER_SKELETON ? Items.STONE_SWORD : Items.BOW)
                    );
                }
                cachedKind = kind;
            }
            return cachedSkeleton;
        }

        private void clearCaches() {
            cachedWorkerStack = ItemStack.EMPTY;
            cachedWorker = null;
            cachedSkeleton = null;
            cachedKind = SkeletonFarmKind.SKELETON;
        }
    }
}
