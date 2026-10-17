package com.cosmocraft.trading_cells.feature.infusion.adapters.output.client;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Only the stored ingredients are dynamic; the chamber and emitters are baked. */
public final class ArcaneInfuserBlockEntityRenderer implements BlockEntityRenderer<ArcaneInfuserBlockEntity, ArcaneInfuserBlockEntityRenderer.State> {
    private final ItemModelResolver items;

    public ArcaneInfuserBlockEntityRenderer(BlockEntityRendererProvider.Context context) { items = context.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(ArcaneInfuserBlockEntity block, State state, float partialTicks,
            Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(block, state, partialTicks, camera, breakProgress);
        state.facing = block.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        var level = block.getLevel();
        if (level == null) { return; }
        state.light = PreviewEntityRenderUtil.sampleCageLightCoords(level, block.getBlockPos());
        for (int slot = 0; slot < state.items.length; slot++) {
            ItemStack stack = block.getItem(slot);
            if (stack.isEmpty()) {
                state.items[slot].clear();
                state.cached[slot] = ItemStack.EMPTY;
            } else if (!ItemStack.isSameItemSameComponents(stack, state.cached[slot]) || state.items[slot].isAnimated()) {
                items.updateForTopItem(state.items[slot], stack, ItemDisplayContext.FIXED, level, null,
                        (int) (block.getBlockPos().asLong() + slot * 31L));
                state.cached[slot] = stack.copy();
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.335, 0.5);
        pose.rotate(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        for (int slot = 0; slot < state.items.length; slot++) {
            if (state.items[slot].isEmpty()) { continue; }
            pose.pushPose();
            pose.translate((slot % 3 - 1) * 0.1953125, 0, (slot / 3 - 1) * 0.1953125);
            pose.scale(0.14F, 0.14F, 0.14F);
            state.items[slot].submit(pose, collector, state.light, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        private final ItemStackRenderState[] items = new ItemStackRenderState[9];
        private final ItemStack[] cached = new ItemStack[9];
        private Direction facing = Direction.NORTH;
        private int light;

        State() {
            for (int slot = 0; slot < items.length; slot++) {
                items[slot] = new ItemStackRenderState();
                cached[slot] = ItemStack.EMPTY;
            }
        }
    }
}
