package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin.TrialSpawnerStateDataAccessor;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;

/** Server-only additions around the vanilla trial-spawner state machine. */
public final class TrialSpawnerRuntimeRules {
    private static final int MAX_MOB_TRACKING_DISTANCE = 47;
    private static final int MAX_MOB_TRACKING_DISTANCE_SQR = MAX_MOB_TRACKING_DISTANCE
            * MAX_MOB_TRACKING_DISTANCE;

    private TrialSpawnerRuntimeRules() {
    }

    public static boolean hasParticipantInRange(
            TrialSpawner trialSpawner,
            ServerLevel level,
            BlockPos spawnerPos
    ) {
        for (UUID playerId : trialSpawner.getStateData().pack().detectedPlayers()) {
            Player player = level.getPlayerByUUID(playerId);
            if (player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && player.blockPosition().closerThan(spawnerPos, trialSpawner.getRequiredPlayerRange())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasEligiblePlayerInRange(
            TrialSpawner trialSpawner,
            ServerLevel level,
            BlockPos spawnerPos
    ) {
        return !trialSpawner.getPlayerDetector().detect(
                level,
                trialSpawner.getEntitySelector(),
                spawnerPos,
                trialSpawner.getRequiredPlayerRange(),
                false
        ).isEmpty();
    }

    public static void freezeActiveTimers(TrialSpawner trialSpawner) {
        TrialSpawnerStateDataAccessor data = (TrialSpawnerStateDataAccessor) trialSpawner.getStateData();
        data.tradingCells$setNextMobSpawnsAt(incrementIfScheduled(data.tradingCells$getNextMobSpawnsAt()));
        data.tradingCells$setCooldownEndsAt(incrementIfScheduled(data.tradingCells$getCooldownEndsAt()));
    }

    public static void discardEscapedMobs(TrialSpawner trialSpawner, ServerLevel level, BlockPos spawnerPos) {
        for (UUID mobId : trialSpawner.getStateData().pack().currentMobs()) {
            Entity entity = level.getEntity(mobId);
            if (entity != null
                    && entity.isAlive()
                    && entity.level().dimension().equals(level.dimension())
                    && entity.blockPosition().distSqr(spawnerPos) > MAX_MOB_TRACKING_DISTANCE_SQR) {
                entity.discard();
            }
        }
    }

    private static long incrementIfScheduled(long value) {
        return value <= 0L || value == Long.MAX_VALUE ? value : value + 1L;
    }
}
