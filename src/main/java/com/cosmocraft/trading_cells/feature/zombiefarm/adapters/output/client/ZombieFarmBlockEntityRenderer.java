package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ZombieFarmBlockEntityRenderer implements BlockEntityRenderer<
        ZombieFarmBlockEntity,
        ZombieFarmBlockEntityRenderer.State
> {
    private static final float WORKER_SCALE = 0.27F;
    private static final float SPAWNER_SCALE = 0.27F;
    private static final float SPAWNER_ENTITY_SCALE = 0.13F;
    private static final double SPAWNER_Y = 0.14D;
    private static final double SPAWNER_ENTITY_Y = SPAWNER_Y + SPAWNER_SCALE;
    private static final double SIDE_OFFSET = 0.18D;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;

    public ZombieFarmBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull ZombieFarmBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.worker = null;
        state.zombie = null;

        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        if (!state.spawnerReady) {
            blockModelResolver.update(
                    state.spawner,
                    Blocks.SPAWNER.defaultBlockState(),
                    BlockDisplayContext.create()
            );
            state.spawner.tintLayers().clear();
            state.spawnerReady = true;
        }
        Direction side = state.facing.getClockWise();

        Entity worker = state.getOrCreateWorker(blockEntity, level);
        if (worker != null) {
            orient(worker, side.toYRot());
            state.worker = extractEntity(worker, partialTicks, state.lightCoords);
        }

        Entity zombie = state.getOrCreateZombie(level, blockEntity.selectedTargetId());
        if (zombie != null) {
            orient(zombie, side.getOpposite().toYRot());
            state.zombie = extractEntity(zombie, partialTicks, state.lightCoords);
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
                WORKER_SCALE,
                state,
                poseStack,
                collector,
                camera
        );
        Vec3 spawnerPosition = new Vec3(
                0.5D + side.getStepX() * SIDE_OFFSET,
                SPAWNER_Y,
                0.5D + side.getStepZ() * SIDE_OFFSET
        );
        submitSpawner(state, spawnerPosition, poseStack, collector);
        submitEntity(
                state.zombie,
                new Vec3(spawnerPosition.x(), SPAWNER_ENTITY_Y, spawnerPosition.z()),
                SPAWNER_ENTITY_SCALE,
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
            float scale,
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
        poseStack.scale(scale, scale, scale);
        entityRenderer.submit(entity, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
        poseStack.popPose();
    }

    private static void submitSpawner(
            State state,
            Vec3 position,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (state.spawner.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(
                position.x() - SPAWNER_SCALE * 0.5D,
                position.y(),
                position.z() - SPAWNER_SCALE * 0.5D
        );
        poseStack.scale(SPAWNER_SCALE, SPAWNER_SCALE, SPAWNER_SCALE);
        state.spawner.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                EntityRenderState.NO_OUTLINE
        );
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
    public @NonNull AABB getRenderBoundingBox(ZombieFarmBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState worker;
        public @Nullable EntityRenderState zombie;
        public Direction facing = Direction.NORTH;
        private final BlockModelRenderState spawner = new BlockModelRenderState();
        private ItemStack cachedWorkerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedWorker;
        private @Nullable Entity cachedZombie;
        private Identifier cachedTargetId = Identifier.withDefaultNamespace("zombie");
        private boolean spawnerReady;

        private @Nullable Entity getOrCreateWorker(ZombieFarmBlockEntity blockEntity, Level level) {
            ItemStack workerStack = blockEntity.getItem(ZombieFarmBlockEntity.WORKER_SLOT);
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

        private @Nullable Entity getOrCreateZombie(Level level, Identifier targetId) {
            if (cachedZombie == null || !cachedTargetId.equals(targetId)) {
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(targetId).orElse(null);
                cachedZombie = type == null ? null : type.create(level, EntitySpawnReason.LOAD);
                if (cachedZombie instanceof ZombieVillager zombieVillager) {
                    zombieVillager.setVillagerData(new VillagerData(
                            BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
                            BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
                            1
                    ));
                }
                if (cachedZombie instanceof LivingEntity living
                        && com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input
                        .ZombieFarmTargetCatalog.isStaticTarget(targetId)) {
                    ZombieFarmKind kind = com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input
                            .ZombieFarmTargetCatalog.staticKind(targetId);
                    ItemStack weapon = switch (kind) {
                        case DROWNED -> new ItemStack(Items.TRIDENT);
                        case ZOMBIFIED_PIGLIN -> new ItemStack(Items.GOLDEN_SWORD);
                        default -> ItemStack.EMPTY;
                    };
                    living.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                    living.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                cachedTargetId = targetId;
            }
            return cachedZombie;
        }

        private void clearCaches() {
            cachedWorkerStack = ItemStack.EMPTY;
            cachedWorker = null;
            cachedZombie = null;
            cachedTargetId = Identifier.withDefaultNamespace("zombie");
        }
    }
}
