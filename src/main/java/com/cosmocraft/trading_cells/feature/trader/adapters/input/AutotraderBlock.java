package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public final class AutotraderBlock extends AbstractPortableMachineBlock<AutotraderBlockEntity> {
    public static final MapCodec<AutotraderBlock> CODEC = simpleCodec(AutotraderBlock::new);

    public AutotraderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new AutotraderBlockEntity(pos, state);
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
        AutotraderBlockEntity autotrader = getAutotrader(level, pos);
        if (autotrader == null) {
            return InteractionResult.FAIL;
        }
        if (CapturedMobStackAdapter.isCapturer(CapturedMobKind.VILLAGER, stack)) {
            return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                    ? autotrader.insertVillagerFromCapturer(stack)
                    : autotrader.extractVillagerToCapturer(stack, player);
        }
        if (player.isShiftKeyDown()) {
            return autotrader.extractPoiToPlayer(player);
        }
        if (stack.getItem() instanceof BlockItem) {
            InteractionResult result = autotrader.insertPoiFromStack(stack, player);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(
            @NonNull BlockState state,
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull Player player,
            @NonNull BlockHitResult hit
    ) {
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            AutotraderBlockEntity autotrader = getAutotrader(level, pos);
            return autotrader == null ? InteractionResult.FAIL : autotrader.extractPoiToPlayer(player);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected BlockEntityType<AutotraderBlockEntity> machineType() {
        return AutotraderRegistrationAdapter.AUTOTRADER_BLOCK_ENTITY.get();
    }

    @Override
    protected void onMenuOpened(
            ServerPlayer player,
            PortableMachineBlockEntity machine,
            int containerId
    ) {
        if (machine instanceof AutotraderBlockEntity
                && player.containerMenu instanceof AutotraderMenu menu
                && menu.containerId == containerId) {
            menu.sendMenuSnapshot(player);
        }
    }

    private static AutotraderBlockEntity getAutotrader(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof AutotraderBlockEntity autotrader ? autotrader : null;
    }
}
