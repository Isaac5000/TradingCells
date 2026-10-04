package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityInteractionFarmBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class EntityInteractionFarmRenderer implements BlockEntityRenderer<EntityInteractionFarmBlockEntity, EntityInteractionFarmRenderer.State> {
    private final EntityRenderDispatcher dispatcher;
    private final SimulationWorkerRenderer workerRenderer;
    private final Map<EntityInteractionFarmBlockEntity, PreviewCache> previews = new WeakHashMap<>();
    public EntityInteractionFarmRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.entityRenderer(); workerRenderer = new SimulationWorkerRenderer(context);
    }
    @Override public @NonNull State createRenderState() { return new State(); }
    @Override public void extractRenderState(@NonNull EntityInteractionFarmBlockEntity farm, @NonNull State state, float partialTick,
            @NonNull Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(farm, state, partialTick, camera, breaking);
        state.worker = null; state.creature = null; state.secondary = null; state.itemTray = null; state.toolTray = null;
        if (farm.getLevel() == null) { previews.remove(farm); return; }
        state.side = farm.getBlockState().getValue(AbstractPortableMachineBlock.FACING).getClockWise();
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(farm.getLevel(), farm.getBlockPos());
        var cache = previews.computeIfAbsent(farm, ignored -> new PreviewCache());
        cache.update(farm, state.side);
        state.worker = cache.worker; state.creature = cache.creature; state.secondary = cache.secondary;
        state.itemTray = cache.itemTray; state.toolTray = cache.toolTray;
    }
    @Override public void submit(State state, @NonNull PoseStack poses, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        submitTray(state, state.itemTray, -0.265625, poses, collector);
        submitTray(state, state.toolTray, 0.265625, poses, collector);
        if (state.worker != null) {
            workerRenderer.submit(state.worker, dispatcher, 0.5 - state.side.getStepX() * 0.22, 0.125,
                    0.5 - state.side.getStepZ() * 0.22, state.lightCoords, poses, collector, camera, state.secondary);
        }
        if (state.creature != null) {
            state.creature.submit(dispatcher, 0.5 + state.side.getStepX() * 0.1875, 0.28125,
                    0.5 + state.side.getStepZ() * 0.1875, state.lightCoords, poses, collector, camera);
        }
    }
    private static void submitTray(State state, @Nullable ItemStackRenderState item, double offset,
            PoseStack poses, SubmitNodeCollector collector) {
        if (item == null || item.isEmpty()) { return; }
        Direction front = state.side.getCounterClockWise();
        poses.pushPose();
        poses.translate(0.5 + state.side.getStepX() * offset + front.getStepX() * 0.296875, 0.185,
                0.5 + state.side.getStepZ() * offset + front.getStepZ() * 0.296875);
        poses.mulPose(Axis.YP.rotationDegrees(-front.toYRot()));
        poses.mulPose(Axis.XP.rotationDegrees(90));
        poses.scale(0.18F, 0.18F, 0.18F);
        item.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }
    public static final class State extends BlockEntityRenderState {
        private @Nullable SimulationEntityPreview worker, creature;
        private @Nullable ItemStackRenderState secondary;
        private @Nullable ItemStackRenderState itemTray, toolTray;
        private Direction side = Direction.EAST;
    }
    private final class PreviewCache {
        private ItemStack workerStack = ItemStack.EMPTY, moduleStack = ItemStack.EMPTY, itemStack = ItemStack.EMPTY, toolStack = ItemStack.EMPTY;
        private @Nullable Direction side;
        private @Nullable SimulationEntityPreview worker, creature;
        private final ItemStackRenderState secondary = new ItemStackRenderState();
        private final ItemStackRenderState itemTray = new ItemStackRenderState(), toolTray = new ItemStackRenderState();
        void update(EntityInteractionFarmBlockEntity farm, Direction facing) {
            ItemStack item = farm.getItem(EntityInteractionFarmBlockEntity.ITEM), tool = farm.getItem(EntityInteractionFarmBlockEntity.TOOL);
            if (side != facing || !ItemStack.isSameItemSameComponents(workerStack, farm.worker())
                    || !ItemStack.isSameItemSameComponents(itemStack, item) || !ItemStack.isSameItemSameComponents(toolStack, tool)) {
                workerStack = farm.worker().copy(); itemStack = item.copy(); toolStack = tool.copy(); secondary.clear();
                var resolver = Minecraft.getInstance().getItemModelResolver();
                resolver.updateForTopItem(itemTray, item, ItemDisplayContext.FIXED, farm.getLevel(), null, 0);
                resolver.updateForTopItem(toolTray, tool, ItemDisplayContext.FIXED, farm.getLevel(), null, 0);
                var entity = CapturedMobStackAdapter.createEntity(CapturedMobKind.VILLAGER, farm.getLevel(), workerStack, farm.getBlockPos());
                if (entity instanceof LivingEntity living) {
                    living.setItemSlot(EquipmentSlot.MAINHAND, tool.isEmpty() ? item.copy() : tool.copy());
                    living.setItemSlot(EquipmentSlot.OFFHAND, tool.isEmpty() ? ItemStack.EMPTY : item.copy());
                    worker = SimulationEntityPreview.create(living, dispatcher, facing.toYRot(), 0.31F, 0.62F);
                    Minecraft.getInstance().getItemModelResolver().updateForLiving(secondary, living.getOffhandItem(), ItemDisplayContext.GROUND, living);
                } else { worker = null; }
            }
            if (side != facing || !ItemStack.isSameItemSameComponents(moduleStack, farm.creature())) {
                moduleStack = farm.creature().copy();
                creature = SimulationEntityPreview.create(EntityEssenceData.createEntity(farm.getLevel(), moduleStack), dispatcher,
                        facing.getOpposite().toYRot(), 0.30F, 0.54F);
            }
            side = facing;
        }
    }
}
