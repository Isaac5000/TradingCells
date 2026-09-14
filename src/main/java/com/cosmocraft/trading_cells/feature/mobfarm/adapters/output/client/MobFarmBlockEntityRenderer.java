package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class MobFarmBlockEntityRenderer implements BlockEntityRenderer<MobFarmBlockEntity, MobFarmBlockEntityRenderer.State> {
    private final EntityRenderDispatcher dispatcher;
    private final Map<MobFarmBlockEntity, PreviewCache> previews = new WeakHashMap<>();

    public MobFarmBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.entityRenderer();
    }

    @Override public @NonNull State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(@NonNull MobFarmBlockEntity blockEntity, @NonNull State state, float partialTicks,
            @NonNull Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.worker = null;
        state.creature = null;
        if (blockEntity.getLevel() == null) { previews.remove(blockEntity); return; }
        state.side = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING).getClockWise();
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos());
        PreviewCache cache = previews.computeIfAbsent(blockEntity, ignored -> new PreviewCache());
        cache.update(blockEntity, state.side);
        state.worker = cache.worker;
        state.creature = cache.creature;
    }

    @Override
    public void submit(State state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector,
            @NonNull CameraRenderState camera) {
        if (state.worker != null) {
            state.worker.submit(dispatcher, 0.5D - state.side.getStepX() * 0.22D, 0.125D,
                    0.5D - state.side.getStepZ() * 0.22D, state.lightCoords, poseStack, collector, camera);
        }
        if (state.creature != null) {
            // The baked pedestal is centered at (11, 4.5, 8) in the north-facing model.
            state.creature.submit(dispatcher, 0.5D + state.side.getStepX() * 0.1875D, 0.28125D,
                    0.5D + state.side.getStepZ() * 0.1875D, state.lightCoords, poseStack, collector, camera);
        }
    }

    public static final class State extends BlockEntityRenderState {
        private @Nullable SimulationEntityPreview worker;
        private @Nullable SimulationEntityPreview creature;
        private Direction side = Direction.EAST;
    }

    private final class PreviewCache {
        private ItemStack workerStack = ItemStack.EMPTY;
        private ItemStack swordStack = ItemStack.EMPTY;
        private ItemStack moduleStack = ItemStack.EMPTY;
        private @Nullable Direction side;
        private @Nullable SimulationEntityPreview worker;
        private @Nullable SimulationEntityPreview creature;

        void update(MobFarmBlockEntity farm, Direction newSide) {
            ItemStack sword = farm.getItem(MobFarmBlockEntity.SWORD_SLOT);
            if (side != newSide || !ItemStack.isSameItemSameComponents(workerStack, farm.worker())
                    || !ItemStack.isSameItemSameComponents(swordStack, sword)) {
                workerStack = farm.worker().copy();
                swordStack = sword.copy();
                var entity = CapturedMobStackAdapter.createEntity(CapturedMobKind.VILLAGER,
                        farm.getLevel(), workerStack, farm.getBlockPos());
                if (entity instanceof LivingEntity living) {
                    living.setItemSlot(EquipmentSlot.MAINHAND, swordStack);
                    worker = SimulationEntityPreview.create(living, dispatcher, newSide.toYRot(), 0.31F, 0.62F);
                } else { worker = null; }
            }
            if (side != newSide || !ItemStack.isSameItemSameComponents(moduleStack, farm.creature())) {
                moduleStack = farm.creature().copy();
                creature = SimulationEntityPreview.create(EntityEssenceData.createEntity(farm.getLevel(), moduleStack),
                        dispatcher, newSide.getOpposite().toYRot(), 0.30F, 0.54F);
            }
            side = newSide;
        }
    }
}
