package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrialSpawnerStateData.class)
public interface TrialSpawnerStateDataAccessor {
    @Accessor("cooldownEndsAt")
    long tradingCells$getCooldownEndsAt();

    @Accessor("cooldownEndsAt")
    void tradingCells$setCooldownEndsAt(long value);

    @Accessor("nextMobSpawnsAt")
    long tradingCells$getNextMobSpawnsAt();

    @Accessor("nextMobSpawnsAt")
    void tradingCells$setNextMobSpawnsAt(long value);

    @Accessor("spin")
    double tradingCells$getSpin();

    @Accessor("oSpin")
    void tradingCells$setOldSpin(double spin);
}
