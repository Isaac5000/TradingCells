package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class RaiderFarmBlockEntityRenderer implements BlockEntityRenderer<
        RaiderFarmBlockEntity,
        RaiderFarmBlockEntityRenderer.State
> {
    private static final float WORKER_SCALE = 0.27F;
    private static final float SPAWNER_SCALE = 0.27F;
    private static final float SPAWNER_ENTITY_SCALE = 0.13F;
    private static final double SPAWNER_Y = 0.14D;
    private static final double SPAWNER_ENTITY_Y = SPAWNER_Y + SPAWNER_SCALE;
    private static final double SIDE_OFFSET = 0.18D;
    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;
    private final BlockModelRenderState spawner = new BlockModelRenderState();
    private final Map<RaiderFarmBlockEntity, EntityCache> entityCaches = new WeakHashMap<>();
    private boolean spawnerReady;

    public RaiderFarmBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull RaiderFarmBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.worker = null;
        state.raider = null;

        Level level = blockEntity.getLevel();
        if (level == null) {
            entityCaches.remove(blockEntity);
            return;
        }
        EntityCache entityCache = entityCaches.computeIfAbsent(blockEntity, ignored -> new EntityCache());
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        if (!spawnerReady) {
            blockModelResolver.update(
                    spawner,
                    Blocks.SPAWNER.defaultBlockState(),
                    BlockDisplayContext.create()
            );
            spawner.tintLayers().clear();
            spawnerReady = true;
        }
        Direction side = state.facing.getClockWise();

        Entity worker = entityCache.getOrCreateWorker(blockEntity, level);
        if (worker != null) {
            state.worker = entityCache.getOrCreateWorkerRenderState(worker, side.toYRot());
        }

        Entity raider = entityCache.getOrCreateRaider(level, blockEntity.selectedTargetId());
        if (raider != null) {
            state.raider = entityCache.getOrCreateRaiderRenderState(raider, side.getOpposite().toYRot());
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
                0.5D - side.getStepX() * SIDE_OFFSET,
                0.11D,
                0.5D - side.getStepZ() * SIDE_OFFSET,
                WORKER_SCALE,
                state,
                poseStack,
                collector,
                camera
        );
        double spawnerX = 0.5D + side.getStepX() * SIDE_OFFSET;
        double spawnerZ = 0.5D + side.getStepZ() * SIDE_OFFSET;
        submitSpawner(state, spawner, spawnerX, SPAWNER_Y, spawnerZ, poseStack, collector);
        submitEntity(
                state.raider,
                spawnerX,
                SPAWNER_ENTITY_Y,
                spawnerZ,
                SPAWNER_ENTITY_SCALE,
                state,
                poseStack,
                collector,
                camera
        );
    }

    private EntityRenderState extractEntity(Entity entity) {
        PreviewEntityRenderUtil.prepare(entity);
        EntityRenderState renderState = entityRenderer.extractEntity(entity, 0.0F);
        PreviewEntityRenderUtil.suppressWorldEffects(renderState);
        return renderState;
    }

    private void submitEntity(
            @Nullable EntityRenderState entity,
            double x,
            double y,
            double z,
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
        poseStack.translate(x, y, z);
        poseStack.scale(scale, scale, scale);
        entityRenderer.submit(entity, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
        poseStack.popPose();
    }

    private static void submitSpawner(
            State state,
            BlockModelRenderState spawner,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (spawner.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(
                x - SPAWNER_SCALE * 0.5D,
                y,
                z - SPAWNER_SCALE * 0.5D
        );
        poseStack.scale(SPAWNER_SCALE, SPAWNER_SCALE, SPAWNER_SCALE);
        spawner.submit(
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
    public @NonNull AABB getRenderBoundingBox(RaiderFarmBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState worker;
        public @Nullable EntityRenderState raider;
        public Direction facing = Direction.NORTH;
    }

    private final class EntityCache {
        private ItemStack cachedWorkerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedWorker;
        private @Nullable Entity cachedRaider;
        private @Nullable EntityRenderState cachedWorkerRenderState;
        private @Nullable EntityRenderState cachedRaiderRenderState;
        private float cachedWorkerYaw = Float.NaN;
        private float cachedRaiderYaw = Float.NaN;
        private Identifier cachedTargetId = Identifier.withDefaultNamespace("raider");

        private @Nullable Entity getOrCreateWorker(RaiderFarmBlockEntity blockEntity, Level level) {
            ItemStack workerStack = blockEntity.getItem(RaiderFarmBlockEntity.WORKER_SLOT);
            if (workerStack.isEmpty()) {
                cachedWorkerStack = ItemStack.EMPTY;
                cachedWorker = null;
                cachedWorkerRenderState = null;
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
                cachedWorkerRenderState = null;
            }
            return cachedWorker;
        }

        private EntityRenderState getOrCreateWorkerRenderState(Entity worker, float yaw) {
            if (cachedWorkerRenderState == null || cachedWorkerYaw != yaw) {
                orient(worker, yaw);
                cachedWorkerRenderState = extractEntity(worker);
                cachedWorkerYaw = yaw;
            }
            return cachedWorkerRenderState;
        }

        private @Nullable Entity getOrCreateRaider(Level level, Identifier targetId) {
            if (cachedRaider == null || !cachedTargetId.equals(targetId)) {
                Identifier entityTypeId = RaiderFarmTargetCatalog.entityTypeId(targetId);
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
                cachedRaider = type == null ? null : type.create(level, EntitySpawnReason.LOAD);
                if (cachedRaider instanceof LivingEntity living) {
                    RaiderFarmKind kind = RaiderFarmTargetCatalog.staticKind(targetId);
                    ItemStack weapon = RaiderFarmTargetCatalog.defaultWeapon(targetId);
                    living.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                    if (RaiderFarmTargetCatalog.isPillager(targetId)) {
                        living.setItemSlot(
                                EquipmentSlot.HEAD,
                                RaiderFarmLootAdapter.ominousBanner(level.registryAccess())
                        );
                    }
                }
                cachedTargetId = targetId;
                cachedRaiderRenderState = null;
            }
            return cachedRaider;
        }

        private EntityRenderState getOrCreateRaiderRenderState(Entity raider, float yaw) {
            if (cachedRaiderRenderState == null || cachedRaiderYaw != yaw) {
                orient(raider, yaw);
                cachedRaiderRenderState = extractEntity(raider);
                cachedRaiderYaw = yaw;
            }
            return cachedRaiderRenderState;
        }

    }
}
