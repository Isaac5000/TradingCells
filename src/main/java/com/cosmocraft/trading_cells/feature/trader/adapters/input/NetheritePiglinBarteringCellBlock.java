package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public final class NetheritePiglinBarteringCellBlock
        extends AbstractPortableMachineBlock<NetheritePiglinBarteringCellBlockEntity> {
    public static final MapCodec<NetheritePiglinBarteringCellBlock> CODEC =
            simpleCodec(NetheritePiglinBarteringCellBlock::new);

    public NetheritePiglinBarteringCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new NetheritePiglinBarteringCellBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<NetheritePiglinBarteringCellBlockEntity> machineType() {
        return TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get();
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
        if (!CapturedMobStackAdapter.isCapturer(CapturedMobKind.PIGLIN, stack)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof NetheritePiglinBarteringCellBlockEntity cell)) {
            return InteractionResult.FAIL;
        }
        if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.PIGLIN, stack)) {
            return cell.insertPiglinFromCapturer(stack);
        }
        return cell.extractPiglinToCapturer(stack, player);
    }
}
