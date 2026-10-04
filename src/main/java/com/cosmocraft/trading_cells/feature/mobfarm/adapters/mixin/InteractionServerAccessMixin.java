package com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.InteractionIsolation;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
abstract class InteractionServerAccessMixin {
    @Inject(method = {"getLevel", "getAllLevels", "overworld", "getPlayerList", "getDataStorage", "getCommands", "getWorldData", "getGameRules",
            "getScoreboard", "clockManager", "tickRateManager", "getWeatherData", "getScheduledEvents", "getStructureManager"}, at = @At("HEAD"))
    private void tradingCells$denyWorldAccess(CallbackInfoReturnable<?> callback) {
        if (InteractionIsolation.active()) { InteractionIsolation.deny(); }
    }
    @Inject(method = "setRespawnData", at = @At("HEAD"))
    private void tradingCells$denySpawnChanges(CallbackInfo callback) {
        if (InteractionIsolation.active()) { InteractionIsolation.deny(); }
    }
}
