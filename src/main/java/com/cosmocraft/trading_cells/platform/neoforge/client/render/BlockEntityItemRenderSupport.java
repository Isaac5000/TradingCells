package com.cosmocraft.trading_cells.platform.neoforge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public final class BlockEntityItemRenderSupport {
    private BlockEntityItemRenderSupport() {
    }

    public static final class Renderer implements SpecialModelRenderer<ItemStack> {
        @Override
        public ItemStack extractArgument(@NonNull ItemStack stack) {
            return stack;
        }

        @Override
        public void submit(
                @Nullable ItemStack stack, // NOSONAR - SpecialModelRenderer explicitly permits a missing render argument.
                @NonNull PoseStack poseStack,
                @NonNull SubmitNodeCollector submitNodeCollector,
                int packedLight,
                int packedOverlay,
                boolean hasFoil,
                int outlineColor
        ) {
            Minecraft minecraft = Minecraft.getInstance();
            Level level = minecraft.level;
            if (stack == null
                    || level == null
                    || !(stack.getItem() instanceof BlockItem blockItem)
                    || !(blockItem.getBlock() instanceof EntityBlock entityBlock)) {
                return;
            }

            BlockState blockState = blockItem.getBlock().defaultBlockState();
            BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockState);
            if (blockEntity == null) {
                return;
            }

            blockEntity.setLevel(level);
            loadItemData(stack, blockEntity, level);
            submitBlockEntity(
                    minecraft.getBlockEntityRenderDispatcher(),
                    blockEntity,
                    poseStack,
                    submitNodeCollector,
                    packedLight
            );
        }

        private static void loadItemData(ItemStack stack, BlockEntity blockEntity, Level level) {
            TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data == null || data.type() != blockEntity.getType()) {
                return;
            }

            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                    blockEntity.problemPath(),
                    TradingCells.LOGGER
            )) {
                blockEntity.loadCustomOnly(TagValueInput.create(
                        reporter,
                        level.registryAccess(),
                        data.copyTagWithoutId()
                ));
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> extents) {
            extents.accept(new Vector3f(0.0F, 0.0F, 0.0F));
            extents.accept(new Vector3f(1.0F, 1.0F, 1.0F));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static void submitBlockEntity(
                BlockEntityRenderDispatcher dispatcher,
                BlockEntity blockEntity,
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int packedLight
        ) {
            BlockEntityRenderer renderer = dispatcher.getRenderer(blockEntity);
            if (renderer == null) {
                return;
            }

            BlockEntityRenderState state = renderer.createRenderState();
            renderer.extractRenderState(blockEntity, state, 0.0F, Vec3.atCenterOf(BlockPos.ZERO), null);
            state.lightCoords = packedLight;
            renderer.submit(state, poseStack, submitNodeCollector, new CameraRenderState());
        }
    }

    public static final class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        private Unbaked() {
        }

        @Override
        public @NonNull MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.@NonNull BakingContext context) {
            return new Renderer();
        }
    }
}
