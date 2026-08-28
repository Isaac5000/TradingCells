package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SpawnerRedstoneControl;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrialSpawnerBlockEntity.class)
abstract class TrialSpawnerBlockEntityMixin {
    @Shadow
    public abstract TrialSpawner getTrialSpawner();

    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void trading_cells$syncRedstoneControl(
            HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> callback
    ) {
        SpawnerRedstoneControl control = (SpawnerRedstoneControl) (Object) getTrialSpawner();
        if (control.tradingCells$isRedstoneControlInstalled()) {
            callback.getReturnValue().putBoolean(SpawnerRedstoneControl.PERSISTENCE_TAG, true);
        }
    }
}
