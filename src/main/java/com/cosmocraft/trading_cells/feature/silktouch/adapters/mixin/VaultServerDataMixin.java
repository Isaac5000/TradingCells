package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VaultServerData.class)
abstract class VaultServerDataMixin {
    @Inject(method = "getRewardedPlayers", at = @At("HEAD"), cancellable = true)
    private void trading_cells$omitRewardedPlayers(CallbackInfoReturnable<Set<UUID>> callback) {
        callback.setReturnValue(Set.of());
    }

    @Inject(method = "hasRewardedPlayer", at = @At("HEAD"), cancellable = true)
    private void trading_cells$allowRepeatedUse(Player player, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "addToRewardedPlayers", at = @At("HEAD"), cancellable = true)
    private void trading_cells$doNotRecordPlayer(Player player, CallbackInfo callback) {
        callback.cancel();
    }
}
