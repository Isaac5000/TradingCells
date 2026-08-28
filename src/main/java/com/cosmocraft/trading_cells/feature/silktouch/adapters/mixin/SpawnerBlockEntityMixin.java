package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SpawnerRedstoneControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlockEntity.class)
abstract class SpawnerBlockEntityMixin implements SpawnerRedstoneControl {
    @Unique
    private boolean trading_cells$redstoneControlInstalled;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void trading_cells$loadRedstoneControl(ValueInput input, CallbackInfo callbackInfo) {
        trading_cells$redstoneControlInstalled = input.getBooleanOr(PERSISTENCE_TAG, false);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void trading_cells$saveRedstoneControl(ValueOutput output, CallbackInfo callbackInfo) {
        if (trading_cells$redstoneControlInstalled) {
            output.putBoolean(PERSISTENCE_TAG, true);
        }
    }

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private static void trading_cells$pauseClientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SpawnerBlockEntity blockEntity,
            CallbackInfo callbackInfo
    ) {
        if (isPowered(blockEntity, level, pos)) {
            BaseSpawnerAccessor spawner = (BaseSpawnerAccessor) (Object) blockEntity.getSpawner();
            spawner.tradingCells$setOldSpin(spawner.tradingCells$getSpin());
            callbackInfo.cancel();
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void trading_cells$pauseServerTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SpawnerBlockEntity blockEntity,
            CallbackInfo callbackInfo
    ) {
        if (level instanceof ServerLevel && isPowered(blockEntity, level, pos)) {
            callbackInfo.cancel();
        }
    }

    @Unique
    private static boolean isPowered(SpawnerBlockEntity blockEntity, Level level, BlockPos pos) {
        return ((SpawnerRedstoneControl) (Object) blockEntity).tradingCells$isRedstoneControlInstalled()
                && level.hasNeighborSignal(pos);
    }

    @Override
    public boolean tradingCells$isRedstoneControlInstalled() {
        return trading_cells$redstoneControlInstalled;
    }

    @Override
    public void tradingCells$setRedstoneControlInstalled(boolean installed) {
        trading_cells$redstoneControlInstalled = installed;
    }
}
