package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Invalidates the cached preview after extracting a placed spawner's entity. */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
    @Accessor("displayEntity")
    void tradingCells$setDisplayEntity(@Nullable Entity entity);

    @Accessor("spin")
    double tradingCells$getSpin();

    @Accessor("oSpin")
    void tradingCells$setOldSpin(double spin);
}
