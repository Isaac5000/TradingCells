package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class LogisticsPipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, EnumProperty<PipeConnection>> CONNECTIONS = createProperties();
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = createArms();
    private static final Map<Direction, VoxelShape> EXTRACTOR_RINGS = createExtractorRings();
    private static final VoxelShape[] CONNECTION_SHAPES = createConnectionShapes();

    private final PipeKind kind;
    private final MapCodec<LogisticsPipeBlock> codec;

    public LogisticsPipeBlock(Properties properties, PipeKind kind) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(value -> new LogisticsPipeBlock(value, kind));
        BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);
        for (EnumProperty<PipeConnection> property : CONNECTIONS.values()) {
            state = state.setValue(property, PipeConnection.NONE);
        }
        registerDefaultState(state);
    }

    public PipeKind kind() {
        return kind;
    }

    public static EnumProperty<PipeConnection> connectionProperty(Direction direction) {
        return CONNECTIONS.get(direction);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new LogisticsPipeBlockEntity(pos, state);
    }

    @Override
    public @NonNull BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        for (EnumProperty<PipeConnection> property : CONNECTIONS.values()) {
            builder.add(property);
        }
    }

    @Override
    protected @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NonNull VoxelShape getShape(
            @NonNull BlockState state,
            @NonNull BlockGetter level,
            @NonNull BlockPos pos,
            @NonNull CollisionContext context
    ) {
        int index = 0;
        int multiplier = 1;
        for (Direction direction : Direction.values()) {
            PipeConnection connection = state.getValue(connectionProperty(direction));
            index += multiplier * (connection == PipeConnection.NONE ? 0 : connection == PipeConnection.EXTRACT ? 2 : 1);
            multiplier *= 3;
        }
        return CONNECTION_SHAPES[index];
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(
            @NonNull BlockState state,
            @NonNull BlockGetter level,
            @NonNull BlockPos pos,
            @NonNull CollisionContext context
    ) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected @NonNull FluidState getFluidState(@NonNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected @NonNull BlockState updateShape(@NonNull BlockState state,
            net.minecraft.world.level.@NonNull LevelReader level,
            net.minecraft.world.level.@NonNull ScheduledTickAccess ticks, @NonNull BlockPos pos,
            @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighbor,
            net.minecraft.util.@NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    private static VoxelShape[] createConnectionShapes() {
        // PIPE and INSERT have identical geometry; water and resource kind do not change it.
        VoxelShape[] shapes = new VoxelShape[729];
        for (int index = 0; index < shapes.length; index++) {
            int encoded = index;
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) {
                int connection = encoded % 3;
                encoded /= 3;
                if (connection != 0) {
                    shape = Shapes.joinUnoptimized(shape, ARMS.get(direction), net.minecraft.world.phys.shapes.BooleanOp.OR);
                    if (connection == 2) {
                        shape = Shapes.joinUnoptimized(shape, EXTRACTOR_RINGS.get(direction), net.minecraft.world.phys.shapes.BooleanOp.OR);
                    }
                }
            }
            shapes[index] = shape.optimize();
        }
        return shapes;
    }

    @Override
    protected void neighborChanged(
            @NonNull BlockState state,
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LogisticsPipeBlockEntity pipe) {
            pipe.refreshConnectionsAround();
        }
    }

    @Override
    protected java.util.@NonNull List<net.minecraft.world.item.ItemStack> getDrops(
            @NonNull BlockState state, net.minecraft.world.level.storage.loot.LootParams.@NonNull Builder params) {
        var drops = new java.util.ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof LogisticsPipeBlockEntity pipe) {
            drops.addAll(pipe.installedUpgradeDrops());
        }
        return drops;
    }

    private static Map<Direction, EnumProperty<PipeConnection>> createProperties() {
        Map<Direction, EnumProperty<PipeConnection>> properties = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            properties.put(direction, EnumProperty.create(direction.getSerializedName(), PipeConnection.class));
        }
        return Map.copyOf(properties);
    }

    private static Map<Direction, VoxelShape> createArms() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        shapes.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
        shapes.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        shapes.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        shapes.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        shapes.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
        return Map.copyOf(shapes);
    }

    private static Map<Direction, VoxelShape> createExtractorRings() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.DOWN, horizontalRing(0, 1));
        shapes.put(Direction.UP, horizontalRing(15, 16));
        shapes.put(Direction.NORTH, northSouthRing(0, 1));
        shapes.put(Direction.SOUTH, northSouthRing(15, 16));
        shapes.put(Direction.WEST, eastWestRing(0, 1));
        shapes.put(Direction.EAST, eastWestRing(15, 16));
        return Map.copyOf(shapes);
    }

    private static VoxelShape horizontalRing(double y1, double y2) {
        return Shapes.or(
                Block.box(4, y1, 4, 12, y2, 5),
                Block.box(4, y1, 11, 12, y2, 12),
                Block.box(4, y1, 5, 5, y2, 11),
                Block.box(11, y1, 5, 12, y2, 11)
        );
    }

    private static VoxelShape northSouthRing(double z1, double z2) {
        return Shapes.or(
                Block.box(4, 4, z1, 12, 5, z2),
                Block.box(4, 11, z1, 12, 12, z2),
                Block.box(4, 5, z1, 5, 11, z2),
                Block.box(11, 5, z1, 12, 11, z2)
        );
    }

    private static VoxelShape eastWestRing(double x1, double x2) {
        return Shapes.or(
                Block.box(x1, 4, 4, x2, 5, 12),
                Block.box(x1, 11, 4, x2, 12, 12),
                Block.box(x1, 5, 4, x2, 11, 5),
                Block.box(x1, 5, 11, x2, 11, 12)
        );
    }
}
