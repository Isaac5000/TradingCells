package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.TrialSpawnerExtension;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.TrialSpawnerRuntimeRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrialSpawnerState.class)
abstract class TrialSpawnerStateMixin {
    @Inject(method = "tickAndGetNext", at = @At("HEAD"), cancellable = true)
    private void trading_cells$applyRepeatableTrialRules(
            BlockPos spawnerPos,
            TrialSpawner trialSpawner,
            ServerLevel level,
            CallbackInfoReturnable<TrialSpawnerState> callback
    ) {
        TrialSpawnerState state = (TrialSpawnerState) (Object) this;
        TrialSpawnerExtension extension = (TrialSpawnerExtension) (Object) trialSpawner;

        if (state == TrialSpawnerState.WAITING_FOR_PLAYERS
                && extension.tradingCells$isRedstoneControlInstalled()
                && level.hasNeighborSignal(spawnerPos)) {
            callback.setReturnValue(TrialSpawnerState.WAITING_FOR_PLAYERS);
            return;
        }

        if (state == TrialSpawnerState.ACTIVE
                && !TrialSpawnerRuntimeRules.hasParticipantInRange(trialSpawner, level, spawnerPos)) {
            TrialSpawnerRuntimeRules.freezeActiveTimers(trialSpawner);
            callback.setReturnValue(TrialSpawnerState.ACTIVE);
            return;
        }

        if (state == TrialSpawnerState.COOLDOWN) {
            extension.tradingCells$setCompletedTrial(true);
            extension.tradingCells$setAwaitingPlayerExit(true);
            trialSpawner.getStateData().reset();
            trialSpawner.removeOminous(level, spawnerPos);
            trialSpawner.markUpdated();
            callback.setReturnValue(TrialSpawnerState.WAITING_FOR_PLAYERS);
            return;
        }

        if (state != TrialSpawnerState.WAITING_FOR_PLAYERS || !extension.tradingCells$hasCompletedTrial()) {
            return;
        }

        if (extension.tradingCells$isAwaitingPlayerExit()) {
            if (!TrialSpawnerRuntimeRules.hasEligiblePlayerInRange(trialSpawner, level, spawnerPos)) {
                extension.tradingCells$setAwaitingPlayerExit(false);
                trialSpawner.markUpdated();
            }
            callback.setReturnValue(TrialSpawnerState.WAITING_FOR_PLAYERS);
        }
    }
}
