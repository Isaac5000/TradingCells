package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Decorative one-block stand for already synthesized creature models. */
public final class CreatureDisplayPedestalBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(5, 3, 5, 11, 11, 11),
            Block.box(1, 11, 1, 15, 15, 15)
    ).optimize();

    public CreatureDisplayPedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NonNull BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        // The displayed creature faces the player who placed the stand.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
            @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new CreatureDisplayPedestalBlockEntity(pos, state);
    }

    @Override
    protected @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
            @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player,
            @NonNull InteractionHand hand, @NonNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CreatureDisplayPedestalBlockEntity stand)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) { stand.toggleRotation(); }
            return interactionResult(level);
        }
        // Important: using a filled hand does NOT invoke useWithoutItem in Minecraft.
        // Extract first regardless of the held item so survival players can always retrieve a model.
        if (stand.hasModel()) {
            if (!level.isClientSide()) { giveBackModel(stand, player); }
            return interactionResult(level);
        }
        if (!CreatureDisplayPedestalBlockEntity.accepts(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && stand.insert(stack) && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return interactionResult(level);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level level,
            @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CreatureDisplayPedestalBlockEntity stand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) { stand.toggleRotation(); }
            else if (stand.hasModel()) { giveBackModel(stand, player); }
        }
        return interactionResult(level);
    }

    private static InteractionResult interactionResult(Level level) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private static void giveBackModel(CreatureDisplayPedestalBlockEntity stand, Player player) {
        ItemStack model = stand.removeModel();
        if (model.isEmpty()) { return; }
        if (!player.getInventory().add(model)) {
            player.drop(model, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        player.getInventory().setChanged();
    }

    @Override
    public @NonNull BlockState playerWillDestroy(
            @NonNull Level level, @NonNull BlockPos pos,
            @NonNull BlockState state, @NonNull Player player) {
        // Creative breaking bypasses loot tables. Drop the displayed model
        // before the block entity is removed; clear it to prevent duplicates.
        if (!level.isClientSide() && player.isCreative()
                && level.getBlockEntity(pos) instanceof CreatureDisplayPedestalBlockEntity stand
                && stand.hasModel()) {
            Block.popResource(level, pos, stand.removeModel());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected @NonNull List<ItemStack> getDrops(@NonNull BlockState state, LootParams.@NonNull Builder params) {
        var drops = new ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof CreatureDisplayPedestalBlockEntity stand && stand.hasModel()) {
            drops.add(stand.model().copy());
        }
        return drops;
    }
}
