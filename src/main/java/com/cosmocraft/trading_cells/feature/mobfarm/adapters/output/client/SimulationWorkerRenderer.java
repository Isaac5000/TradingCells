package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Local machine appearance; never replaces the registered villager renderer or its profession. */
final class SimulationWorkerRenderer implements RenderLayerParent<VillagerRenderState, VillagerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "trading_cells", "textures/entity/simulation_worker.png");
    private final VillagerModel model;
    private final CrossedArmsItemLayer<VillagerRenderState, VillagerModel> heldItem;

    SimulationWorkerRenderer(BlockEntityRendererProvider.Context context) {
        model = new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER));
        heldItem = new CrossedArmsItemLayer<>(this);
    }

    @Override
    public VillagerModel getModel() { return model; }

    void submit(SimulationEntityPreview preview, EntityRenderDispatcher dispatcher,
            double x, double y, double z, int light, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera) {
        if (!(preview.state() instanceof VillagerRenderState state)) {
            // An optional mod can replace the villager's render-state type.
            preview.submit(dispatcher, x, y, z, light, poseStack, collector, camera);
            return;
        }
        poseStack.pushPose();
        try {
            float scale = preview.scale() * state.scale;
            poseStack.translate(x, y, z);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            collector.submitModel(model, state, poseStack, model.renderType(TEXTURE),
                    light, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
            model.setupAnim(state);
            heldItem.submit(poseStack, collector, light, state, state.yRot, state.xRot);
        } finally {
            poseStack.popPose();
        }
    }
}
