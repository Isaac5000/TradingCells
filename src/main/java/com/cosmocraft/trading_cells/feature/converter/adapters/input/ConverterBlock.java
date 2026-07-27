package com.cosmocraft.trading_cells.feature.converter.adapters.input;

import com.cosmocraft.trading_cells.feature.converter.adapters.output.ConverterRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
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

public final class ConverterBlock extends AbstractPortableMachineBlock<ConverterBlockEntity> {
    public static final MapCodec<ConverterBlock> CODEC = simpleCodec(ConverterBlock::new);

    public ConverterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ConverterBlockEntity(pos, state);
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
        ConverterBlockEntity converter = getConverter(level, pos);
        if (converter == null) {
            return InteractionResult.FAIL;
        }
        if (CapturedMobStackAdapter.isCapturer(CapturedMobKind.VILLAGER, stack)) {
            return CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                    ? converter.insertVillagerFromCapturer(stack)
                    : converter.extractVillagerToCapturer(stack, player);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected BlockEntityType<ConverterBlockEntity> machineType() {
        return ConverterRegistrationAdapter.CONVERTER_BLOCK_ENTITY.get();
    }

    private static ConverterBlockEntity getConverter(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ConverterBlockEntity converter ? converter : null;
    }
}
