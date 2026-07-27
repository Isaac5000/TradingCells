package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineCageBlockShapes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.stats.Stats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PiglinBarteringCellBlock extends BaseEntityBlock {
    public static final MapCodec<PiglinBarteringCellBlock> CODEC = simpleCodec(PiglinBarteringCellBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PiglinBarteringCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NonNull BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected @NonNull VoxelShape getShape(
            @NonNull BlockState state,
            @NonNull BlockGetter level,
            @NonNull BlockPos pos,
            @NonNull CollisionContext context
    ) {
        return MachineCageBlockShapes.CAGE;
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0F;
    }


    @Override
    public @NonNull PiglinBarteringCellBlockEntity newBlockEntity(
            @NonNull BlockPos pos,
            @NonNull BlockState state
    ) {
        return new PiglinBarteringCellBlockEntity(pos, state);
    }

    @Override
    protected @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            @NonNull Level level,
            @NonNull BlockState state,
            @NonNull BlockEntityType<T> type
    ) {
        return level.isClientSide() ? null : createTickerHelper(
                type,
                TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(),
                (tickerLevel, _, _, cell) -> cell.processTick(tickerLevel)
        );
    }

    @Override
    protected @NonNull InteractionResult useItemOn(
            @NonNull ItemStack stack,
            @NonNull BlockState state,
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull Player player,
            @NonNull InteractionHand hand,
            @NonNull BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        PiglinBarteringCellBlockEntity cell = getCell(level, pos);
        if (cell == null) {
            return InteractionResult.FAIL;
        }

        if (CapturedMobStackAdapter.isCapturer(CapturedMobKind.PIGLIN, stack)) {
            if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.PIGLIN, stack)) {
                return cell.insertPiglinFromCapturer(stack);
            }
            return cell.extractPiglinToCapturer(stack, player);
        }

        if (stack.is(Items.GOLD_INGOT)) {
            return cell.barterGold(stack);
        }

        if (CapturedMobStackAdapter.isCapturer(CapturedMobKind.VILLAGER, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(
            @NonNull BlockState state,
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull Player player,
            @NonNull BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        PiglinBarteringCellBlockEntity cell = getCell(level, pos);
        if (cell == null) {
            return InteractionResult.FAIL;
        }

        if (cell.hasOutput()) {
            return cell.extractOutputToPlayer(player);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public @NonNull BlockState playerWillDestroy(
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull BlockState state,
            @NonNull Player player
    ) {
        if (!level.isClientSide() && !player.isCreative()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PiglinBarteringCellBlockEntity cell) {
                cell.prepareForBlockDrop(level.registryAccess());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(
            @NonNull Level level,
            @NonNull Player player,
            @NonNull BlockPos pos,
            @NonNull BlockState state,
            @Nullable BlockEntity blockEntity, // NOSONAR - Minecraft's Block API explicitly permits no block entity.
            @NonNull ItemStack tool
    ) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (level.isClientSide() || player.isCreative() || !(blockEntity instanceof PiglinBarteringCellBlockEntity cell)) {
            return;
        }

        ItemStack drop = new ItemStack(TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_ITEM.get());
        CompoundTag data = cell.getPreparedBlockDropData(level.registryAccess());
        if (!data.isEmpty()) {
            drop.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(cell.getType(), data));
        }
        Block.popResource(level, pos, drop);
        cell.discardContentsAfterBlockDrop();
    }

    private static @Nullable PiglinBarteringCellBlockEntity getCell(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PiglinBarteringCellBlockEntity cell) {
            return cell;
        }
        return null;
    }
}
