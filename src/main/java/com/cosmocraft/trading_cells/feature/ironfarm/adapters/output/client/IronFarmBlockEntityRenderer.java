package com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class IronFarmBlockEntityRenderer implements BlockEntityRenderer<IronFarmBlockEntity, IronFarmBlockEntityRenderer.State> {
    private static final float VILLAGER_SCALE = 0.24F;
    private static final float ZOMBIE_SCALE = 0.24F;
    private static final float GOLEM_SCALE = 0.22F;
    private final EntityRenderDispatcher entityRenderer;

    public IronFarmBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        entityRenderer = context.entityRenderer();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull IronFarmBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        state.golem = null;
        state.zombie = null;
        for (int index = 0; index < state.villagers.length; index++) {
            state.villagers[index] = null;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            state.clearEntityCaches();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());

        Direction side = state.facing.getClockWise();
        for (int index = 0; index < IronFarmBlockEntity.VILLAGER_SLOT_COUNT; index++) {
            ItemStack stack = blockEntity.getItem(IronFarmBlockEntity.FIRST_VILLAGER_SLOT + index);
            Entity villager = state.getOrCreateVillager(index, blockEntity, stack);
            if (villager != null) {
                orient(villager, side.getOpposite().toYRot());
                state.villagers[index] = extractEntity(villager, partialTicks, state.lightCoords);
            }
        }

        Entity zombie = state.getOrCreateZombie(level);
        if (zombie != null) {
            orient(zombie, side.toYRot());
            state.zombie = extractEntity(zombie, partialTicks, state.lightCoords);
        }

        if (blockEntity.isGolemVisible()) {
            Entity golem = state.getOrCreateGolem(level);
            if (golem != null) {
                orient(golem, state.facing.toYRot());
                state.golem = extractEntity(golem, partialTicks, state.lightCoords);
                if (state.golem instanceof LivingEntityRenderState livingState) {
                    livingState.hasRedOverlay = blockEntity.hasGolemRedHitFlash();
                }
            }
        }
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        Direction side = state.facing.getClockWise();
        double leftX = 0.5D + side.getStepX() * 0.27D;
        double leftZ = 0.5D + side.getStepZ() * 0.27D;
        EntitySubmitContext context = new EntitySubmitContext(
                state.lightCoords,
                poseStack,
                collector,
                camera
        );
        for (int index = 0; index < state.villagers.length; index++) {
            double lineOffset = (index - 1) * 0.23D;
            submitEntity(
                    state.villagers[index],
                    VILLAGER_SCALE,
                    new Vec3(
                            leftX + state.facing.getStepX() * lineOffset,
                            0.11D,
                            leftZ + state.facing.getStepZ() * lineOffset
                    ),
                    context
            );
        }

        submitEntity(state.golem, GOLEM_SCALE, new Vec3(0.5D, 0.13D, 0.5D), context);
        submitEntity(
                state.zombie,
                ZOMBIE_SCALE,
                new Vec3(0.5D - side.getStepX() * 0.28D, 0.11D, 0.5D - side.getStepZ() * 0.28D),
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
    public @NonNull AABB getRenderBoundingBox(IronFarmBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static final class State extends BlockEntityRenderState {
        public final EntityRenderState[] villagers = new EntityRenderState[IronFarmBlockEntity.VILLAGER_SLOT_COUNT];
        public @Nullable EntityRenderState zombie;
        public @Nullable EntityRenderState golem;
        public Direction facing = Direction.NORTH;
        private final ItemStack[] cachedVillagerStacks = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        private final Entity[] cachedVillagers = new Entity[IronFarmBlockEntity.VILLAGER_SLOT_COUNT];
        private @Nullable Entity cachedZombie;
        private @Nullable Entity cachedGolem;

        private @Nullable Entity getOrCreateVillager(int index, IronFarmBlockEntity blockEntity, ItemStack stack) {
            if (stack.isEmpty()) {
                cachedVillagerStacks[index] = ItemStack.EMPTY;
                cachedVillagers[index] = null;
                return null;
            }
            if (cachedVillagers[index] == null || !ItemStack.isSameItemSameComponents(cachedVillagerStacks[index], stack)) {
                cachedVillagers[index] = CapturedMobStackAdapter.createEntity(
                        CapturedMobKind.VILLAGER,
                        blockEntity.getLevel(),
                        stack,
                        blockEntity.getBlockPos()
                );
                cachedVillagerStacks[index] = stack.copy();
            }
            return cachedVillagers[index];
        }

        private @Nullable Entity getOrCreateZombie(Level level) {
            if (cachedZombie == null) {
                cachedZombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.LOAD);
            }
            return cachedZombie;
        }

        private @Nullable Entity getOrCreateGolem(Level level) {
            if (cachedGolem == null) {
                cachedGolem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.LOAD);
            }
            return cachedGolem;
        }

        private void clearEntityCaches() {
            for (int index = 0; index < cachedVillagers.length; index++) {
                cachedVillagerStacks[index] = ItemStack.EMPTY;
                cachedVillagers[index] = null;
            }
            cachedZombie = null;
            cachedGolem = null;
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
