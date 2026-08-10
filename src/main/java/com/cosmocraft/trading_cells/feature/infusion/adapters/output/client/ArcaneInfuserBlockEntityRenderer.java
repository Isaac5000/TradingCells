package com.cosmocraft.trading_cells.feature.infusion.adapters.output.client;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.PreviewEntityRenderUtil;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Renders the Arcane Infuser's nine stored ingredients on its physical work surface. */
public final class ArcaneInfuserBlockEntityRenderer implements BlockEntityRenderer<
        ArcaneInfuserBlockEntity,
        ArcaneInfuserBlockEntityRenderer.State> {
    private static final Identifier TABLE_VISUAL_MODEL = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "arcane_infuser_table_visual"
    );
    private static final double PEDESTAL_DISTANCE = 0.28D;
    private static final double PEDESTAL_Y = 0.10D;
    private static final float PEDESTAL_WIDTH = 0.12F;
    private static final float PEDESTAL_HEIGHT = 0.18F;
    private static final double TABLE_ITEM_Y = 0.25D;
    private static final float TABLE_ITEM_SCALE = 0.52F;
    private static final double PEDESTAL_ITEM_Y = 0.34D;
    private static final double CENTER_ITEM_Y = 0.38D;
    private static final float ITEM_SCALE = 0.15F;
    private static final float CENTER_ITEM_SCALE = 0.17F;

    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;

    public ArcaneInfuserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        blockModelResolver = context.blockModelResolver();
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public @NonNull State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NonNull ArcaneInfuserBlockEntity blockEntity,
            @NonNull State state,
            float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                state,
                partialTicks,
                cameraPosition,
                breakProgress
        );
        state.facing = blockEntity.getBlockState().getValue(AbstractPortableMachineBlock.FACING);
        Level level = blockEntity.getLevel();
        if (!(level instanceof ClientLevel clientLevel)) {
            state.clearItems();
            return;
        }
        state.lightCoords = PreviewEntityRenderUtil.sampleCageLightCoords(level, blockEntity.getBlockPos());
        if (!state.modelsReady) {
            updateBlockModel(state.pedestal, Blocks.CRYING_OBSIDIAN.defaultBlockState());
            Minecraft.getInstance().getModelManager().getItemModel(TABLE_VISUAL_MODEL).update(
                    state.enchantingTable,
                    new ItemStack(Items.OBSIDIAN),
                    itemModelResolver,
                    ItemDisplayContext.FIXED,
                    clientLevel,
                    null,
                    (int) blockEntity.getBlockPos().asLong()
            );
            state.modelsReady = true;
        }
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            updateItem(state, slot, blockEntity.getItem(slot), blockEntity, level);
        }
    }

    @Override
    public void submit(
            State state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector collector,
            @NonNull CameraRenderState camera
    ) {
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            if (slot != ArcaneInfuserBlockEntity.CENTER_SLOT) {
                submitPedestal(state, position(slot, state.facing), poseStack, collector);
            }
        }
        submitItem(
                state.enchantingTable,
                new Vec3(0.5D, TABLE_ITEM_Y, 0.5D),
                TABLE_ITEM_SCALE,
                state,
                poseStack,
                collector
        );

        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            Vec3 position = slot == ArcaneInfuserBlockEntity.CENTER_SLOT
                    ? new Vec3(0.5D, CENTER_ITEM_Y, 0.5D)
                    : position(slot, state.facing);
            float scale = slot == ArcaneInfuserBlockEntity.CENTER_SLOT
                    ? CENTER_ITEM_SCALE
                    : ITEM_SCALE;
            submitItem(state.item(slot), position, scale, state, poseStack, collector);
        }
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(ArcaneInfuserBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1.0D,
                pos.getY() + 1.0D,
                pos.getZ() + 1.0D
        );
    }

    private void updateBlockModel(
            BlockModelRenderState renderState,
            net.minecraft.world.level.block.state.BlockState blockState
    ) {
        blockModelResolver.update(renderState, blockState, BlockDisplayContext.create());
        renderState.tintLayers().clear();
    }

    private void updateItem(
            State state,
            int slot,
            ItemStack stack,
            ArcaneInfuserBlockEntity blockEntity,
            Level level
    ) {
        ItemStackRenderState renderState = state.item(slot);
        ItemStack cached = state.cachedItem(slot);
        if (stack.isEmpty()) {
            renderState.clear();
            state.cacheItem(slot, ItemStack.EMPTY);
            return;
        }
        if (ItemStack.isSameItemSameComponents(cached, stack)) {
            return;
        }
        itemModelResolver.updateForTopItem(
                renderState,
                stack,
                ItemDisplayContext.FIXED,
                level,
                null,
                (int) (blockEntity.getBlockPos().asLong() + slot * 31L)
        );
        state.cacheItem(slot, stack.copy());
    }

    private static Vec3 position(int slot, Direction facing) {
        int row = slot / 3 - 1;
        int column = slot % 3 - 1;
        double forwardX = facing.getStepX();
        double forwardZ = facing.getStepZ();
        double rightX = -forwardZ;
        double rightZ = forwardX;
        return new Vec3(
                0.5D + (row * forwardX + column * rightX) * PEDESTAL_DISTANCE,
                PEDESTAL_ITEM_Y,
                0.5D + (row * forwardZ + column * rightZ) * PEDESTAL_DISTANCE
        );
    }

    private static void submitPedestal(
            State state,
            Vec3 itemPosition,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitBlock(
                state.pedestal,
                new Vec3(itemPosition.x(), PEDESTAL_Y, itemPosition.z()),
                PEDESTAL_WIDTH,
                PEDESTAL_HEIGHT,
                PEDESTAL_WIDTH,
                state.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitBlock(
            BlockModelRenderState state,
            Vec3 position,
            float scaleX,
            float scaleY,
            float scaleZ,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (state.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(
                position.x() - scaleX * 0.5D,
                position.y(),
                position.z() - scaleZ * 0.5D
        );
        poseStack.scale(scaleX, scaleY, scaleZ);
        state.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
        poseStack.popPose();
    }

    private static void submitItem(
            ItemStackRenderState item,
            Vec3 position,
            float scale,
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(position.x(), position.y(), position.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.scale(scale, scale, scale);
        item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        private final BlockModelRenderState pedestal = new BlockModelRenderState();
        private final ItemStackRenderState enchantingTable = new ItemStackRenderState();
        private final ItemStackRenderState[] items = {
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState()
        };
        private final ItemStack[] cachedItems = {
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY
        };
        private Direction facing = Direction.NORTH;
        private int lightCoords;
        private boolean modelsReady;

        private ItemStackRenderState item(int slot) {
            return items[slot];
        }

        private ItemStack cachedItem(int slot) {
            return cachedItems[slot];
        }

        private void cacheItem(int slot, ItemStack stack) {
            cachedItems[slot] = stack;
        }

        private void clearItems() {
            for (int slot = 0; slot < items.length; slot++) {
                items[slot].clear();
                cachedItems[slot] = ItemStack.EMPTY;
            }
        }
    }
}
