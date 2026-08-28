package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.TrialSpawnerExtension;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.TrialSpawnerRuntimeRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrialSpawner.class)
abstract class TrialSpawnerMixin implements TrialSpawnerExtension {
    @Unique
    private static final String TRADING_CELLS_AWAITING_EXIT_TAG = "trading_cells_awaiting_player_exit";
    @Unique
    private static final String TRADING_CELLS_COMPLETED_TRIAL_TAG = "trading_cells_completed_trial";

    @Shadow
    private PlayerDetector playerDetector;

    @Unique
    private boolean trading_cells$awaitingPlayerExit;
    @Unique
    private boolean trading_cells$completedTrial;
    @Unique
    private boolean trading_cells$redstoneControlInstalled;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void trading_cells$includeCreativePlayersForDistanceActivation(CallbackInfo callbackInfo) {
        playerDetector = PlayerDetector.INCLUDING_CREATIVE_PLAYERS;
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void trading_cells$loadPersistentRules(ValueInput input, CallbackInfo callbackInfo) {
        trading_cells$awaitingPlayerExit = input.getBooleanOr(TRADING_CELLS_AWAITING_EXIT_TAG, false);
        trading_cells$completedTrial = input.getBooleanOr(TRADING_CELLS_COMPLETED_TRIAL_TAG, false);
        trading_cells$redstoneControlInstalled = input.getBooleanOr(PERSISTENCE_TAG, false);
    }

    @Inject(method = "store", at = @At("TAIL"))
    private void trading_cells$storePersistentRules(ValueOutput output, CallbackInfo callbackInfo) {
        if (trading_cells$awaitingPlayerExit) {
            output.putBoolean(TRADING_CELLS_AWAITING_EXIT_TAG, true);
        }
        if (trading_cells$completedTrial) {
            output.putBoolean(TRADING_CELLS_COMPLETED_TRIAL_TAG, true);
        }
        if (trading_cells$redstoneControlInstalled) {
            output.putBoolean(PERSISTENCE_TAG, true);
        }
    }

    @Inject(method = "tickClient", at = @At("HEAD"), cancellable = true)
    private void trading_cells$pauseDisplayWhenPowered(
            Level level,
            BlockPos spawnerPos,
            boolean isOminous,
            CallbackInfo callbackInfo
    ) {
        if (trading_cells$redstoneControlInstalled && level.hasNeighborSignal(spawnerPos)) {
            TrialSpawnerStateDataAccessor data =
                    (TrialSpawnerStateDataAccessor) (Object) ((TrialSpawner) (Object) this).getStateData();
            data.tradingCells$setOldSpin(data.tradingCells$getSpin());
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void trading_cells$discardEscapedTrialMobs(
            ServerLevel level,
            BlockPos spawnerPos,
            boolean isOminous,
            CallbackInfo callbackInfo
    ) {
        TrialSpawnerRuntimeRules.discardEscapedMobs((TrialSpawner) (Object) this, level, spawnerPos);
    }

    @Override
    public boolean tradingCells$isAwaitingPlayerExit() {
        return trading_cells$awaitingPlayerExit;
    }

    @Override
    public void tradingCells$setAwaitingPlayerExit(boolean awaitingPlayerExit) {
        trading_cells$awaitingPlayerExit = awaitingPlayerExit;
    }

    @Override
    public boolean tradingCells$hasCompletedTrial() {
        return trading_cells$completedTrial;
    }

    @Override
    public void tradingCells$setCompletedTrial(boolean completedTrial) {
        trading_cells$completedTrial = completedTrial;
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
