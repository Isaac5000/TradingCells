package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobExperienceAccessor {
    @Accessor("xpReward") int tradingCells$baseExperience();

    @Invoker("getBaseExperienceReward")
    int tradingCells$experienceReward(ServerLevel level);
}
