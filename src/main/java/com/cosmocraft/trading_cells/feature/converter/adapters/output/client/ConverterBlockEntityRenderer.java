package com.cosmocraft.trading_cells.feature.converter.adapters.output.client;

import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterBlockEntity;
import com.cosmocraft.trading_cells.feature.converter.domain.model.ConverterStage;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.MachineEntityRenderScales;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ConverterBlockEntityRenderer implements BlockEntityRenderer<ConverterBlockEntity, ConverterBlockEntityRenderer.State> {
    private static final float SIDE_ENTITY_SCALE = MachineEntityRenderScales.VILLAGER_BREEDER_ENTITY;
    private final EntityRenderDispatcher entityRenderer;

    public ConverterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull ConverterBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.villager = null;
        state.zombie = null;

        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        Direction side = state.facing.getClockWise();

        ItemStack villagerStack = blockEntity.getItem(ConverterBlockEntity.VILLAGER_SLOT);
        Entity containedVillager = blockEntity.stage() == ConverterStage.CURING
                ? state.getOrCreateZombieVillager(blockEntity, villagerStack)
                : state.getOrCreateVillager(blockEntity, villagerStack);
        if (containedVillager != null) {
            orient(containedVillager, side.getOpposite().toYRot());
            state.villager = extractEntity(containedVillager, partialTicks, state.lightCoords);
        }

        Entity zombie = state.getOrCreateZombie(level);
        if (zombie != null) {
            orient(zombie, side.toYRot());
            state.zombie = extractEntity(zombie, partialTicks, state.lightCoords);
        }
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        Direction side = state.facing.getClockWise();
        EntitySubmitContext context = new EntitySubmitContext(
                state.lightCoords,
                poseStack,
                collector,
                camera
        );
        submitEntity(
                state.villager,
                SIDE_ENTITY_SCALE,
                new Vec3(0.5D + side.getStepX() * 0.25D, 0.11D, 0.5D + side.getStepZ() * 0.25D),
                context
        );
        submitEntity(
                state.zombie,
                SIDE_ENTITY_SCALE,
                new Vec3(0.5D - side.getStepX() * 0.25D, 0.11D, 0.5D - side.getStepZ() * 0.25D),
                context
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
            @Nullable EntityRenderState state,
            float scale,
            Vec3 position,
            EntitySubmitContext context
    ) {
        if (state == null) {
            return;
        }
        PreviewEntityRenderUtil.applyLight(state, context.lightCoords());
        context.poseStack().pushPose();
        context.poseStack().translate(position.x(), position.y(), position.z());
        context.poseStack().scale(scale, scale, scale);
        entityRenderer.submit(
                state,
                context.camera(),
                0.0D,
                0.0D,
                0.0D,
                context.poseStack(),
                context.collector()
        );
        context.poseStack().popPose();
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
    public @NonNull AABB getRenderBoundingBox(ConverterBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState villager;
        public @Nullable EntityRenderState zombie;
        public Direction facing = Direction.NORTH;
        private ItemStack cachedVillagerStack = ItemStack.EMPTY;
        private @Nullable Entity cachedVillager;
        private @Nullable Entity cachedZombie;
        private @Nullable Entity cachedZombieVillager;
        private ItemStack cachedZombieVillagerStack = ItemStack.EMPTY;

        private @Nullable Entity getOrCreateVillager(ConverterBlockEntity blockEntity, ItemStack stack) {
            if (stack.isEmpty()) {
                cachedVillagerStack = ItemStack.EMPTY;
                cachedVillager = null;
                return null;
            }
            if (cachedVillager == null || !ItemStack.isSameItemSameComponents(cachedVillagerStack, stack)) {
                cachedVillager = CapturedMobStackAdapter.createEntity(
                        CapturedMobKind.VILLAGER,
                        blockEntity.getLevel(),
                        stack,
                        blockEntity.getBlockPos()
                );
                cachedVillagerStack = stack.copy();
            }
            return cachedVillager;
        }

        private @Nullable Entity getOrCreateZombie(Level level) {
            if (cachedZombie == null) {
                cachedZombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.LOAD);
            }
            return cachedZombie;
        }

        private @Nullable Entity getOrCreateZombieVillager(ConverterBlockEntity blockEntity, ItemStack stack) {
            Level level = blockEntity.getLevel();
            if (level == null || stack.isEmpty()) {
                cachedZombieVillager = null;
                cachedZombieVillagerStack = ItemStack.EMPTY;
                return null;
            }
            if (cachedZombieVillager == null
                    || !ItemStack.isSameItemSameComponents(cachedZombieVillagerStack, stack)) {
                Entity source = CapturedMobStackAdapter.createEntity(
                        CapturedMobKind.VILLAGER,
                        level,
                        stack,
                        blockEntity.getBlockPos()
                );
                ZombieVillager zombieVillager = EntityTypes.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.LOAD);
                if (source instanceof Villager sourceVillager && zombieVillager != null) {
                    zombieVillager.setVillagerData(sourceVillager.getVillagerData());
                    zombieVillager.setCustomName(sourceVillager.getCustomName());
                }
                cachedZombieVillager = zombieVillager;
                cachedZombieVillagerStack = stack.copy();
            }
            return cachedZombieVillager;
        }

        private void clearCaches() {
            cachedVillagerStack = ItemStack.EMPTY;
            cachedVillager = null;
            cachedZombie = null;
            cachedZombieVillager = null;
            cachedZombieVillagerStack = ItemStack.EMPTY;
        }
    }

    private record EntitySubmitContext(
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
    }
}
