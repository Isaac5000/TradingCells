package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Trial activation is based on distance, even when blocks obstruct line of sight. */
@Mixin(TrialSpawnerStateData.class)
abstract class TrialSpawnerStateDataMixin {
    @ModifyArg(
            method = "tryDetectPlayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/trialspawner/PlayerDetector;detect("
                            + "Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/level/block/entity/trialspawner/PlayerDetector$EntitySelector;"
                            + "Lnet/minecraft/core/BlockPos;DZ)Ljava/util/List;"
            ),
            index = 4
    )
    private boolean trading_cells$useDistanceOnly(boolean requireLineOfSight) {
        return false;
    }
}
