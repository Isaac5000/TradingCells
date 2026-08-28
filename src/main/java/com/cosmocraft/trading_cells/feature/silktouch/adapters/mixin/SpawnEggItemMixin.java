package com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.PreservedSpawnerItemAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnEggItem.class)
abstract class SpawnEggItemMixin {
    @Inject(method = "shouldPrintOpWarning", at = @At("HEAD"), cancellable = true)
    private void tradingCells$hideWarningForTrustedEgg(
            ItemStack stack,
            @Nullable Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (PreservedSpawnerItemAdapter.isTrustedEgg(stack)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "spawnMob", at = @At("HEAD"), cancellable = true)
    private static void tradingCells$spawnPreservedHierarchy(
            EntityType<?> type,
            @Nullable LivingEntity user,
            ItemStack stack,
            ServerLevel level,
            BlockPos spawnPos,
            boolean tryMoveDown,
            boolean movedUp,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        if (PreservedSpawnerItemAdapter.isTrustedEgg(stack)) {
            callback.setReturnValue(PreservedSpawnerItemAdapter.spawnTrustedEgg(
                    user,
                    stack,
                    level,
                    spawnPos,
                    tryMoveDown,
                    movedUp
            ));
        }
    }
}
