package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.CreatureDisplayPedestalBlock;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.CreatureDisplayPedestalBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.CreatureDisplayOrientation;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Decorative detached preview with a strict maximum one-block display height above the plinth. */
public final class CreatureDisplayPedestalRenderer implements
        BlockEntityRenderer<CreatureDisplayPedestalBlockEntity, CreatureDisplayPedestalRenderer.State> {
    private final EntityRenderDispatcher dispatcher;
    private final Map<CreatureDisplayPedestalBlockEntity, PreviewCache> previews = new WeakHashMap<>();

    public CreatureDisplayPedestalRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.entityRenderer();
    }

    @Override public @NonNull State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(@NonNull CreatureDisplayPedestalBlockEntity pedestal, @NonNull State state,
            float partialTick, @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pedestal, state, partialTick, cameraPosition, breakProgress);
        state.preview = null;
        if (pedestal.getLevel() == null) {
            previews.remove(pedestal);
            return;
        }
        var cache = previews.computeIfAbsent(pedestal, ignored -> new PreviewCache());
        cache.update(pedestal);
        state.preview = cache.preview;
        Direction front = pedestal.getBlockState().getValue(CreatureDisplayPedestalBlock.FACING);
        // PoseStack uses the opposite yaw sign to Direction on the east/west axis.
        state.rotation = CreatureDisplayOrientation.modelYaw(front, cache.dragon) + (pedestal.rotating()
                ? ((pedestal.getLevel().getGameTime() + partialTick) * 0.8F) % 360.0F : 0.0F);
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(
                pedestal.getLevel(), pedestal.getBlockPos());
    }

    @Override
    public void submit(State state, @NonNull PoseStack poses,
            @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        if (state.preview == null) { return; }
        poses.pushPose();
        try {
            poses.translate(0.5D, 0.9375D, 0.5D);
            poses.rotate(Axis.YP.rotationDegrees(state.rotation));
            state.preview.submit(dispatcher, 0.0D, 0.0D, 0.0D,
                    state.lightCoords, poses, collector, camera);
        } finally {
            poses.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        private @Nullable SimulationEntityPreview preview;
        private float rotation;
    }

    private final class PreviewCache {
        private ItemStack model = ItemStack.EMPTY;
        private @Nullable SimulationEntityPreview preview;
        private boolean dragon;

        void update(CreatureDisplayPedestalBlockEntity pedestal) {
            if (!ItemStack.isSameItemSameComponents(model, pedestal.model())) {
                model = pedestal.model().copy();
                dragon = net.minecraft.resources.Identifier.withDefaultNamespace("ender_dragon")
                        .equals(EntityEssenceData.entityTypeId(model));
                // Width/height bound the actual entity model; use modest extra margins for
                // wings, horns and oversized render models. Pedestal top is y=0.9375.
                preview = SimulationEntityPreview.create(
                        EntityEssenceData.createEntity(pedestal.getLevel(), model),
                        dispatcher, 0.0F, 0.90F, 0.84F);
            }
        }
    }
}
