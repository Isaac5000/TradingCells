package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Keeps special blocks slow unless the correct tool carries Silk Touch II. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class SilkTouchTwoMiningSpeedAdapter {
    private static final float WRONG_ENCHANTMENT_SPEED = 1.0F;

    private SilkTouchTwoMiningSpeedAdapter() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!SilkTouchTwoDropAdapter.isSpecialBlock(event.getState())) {
            return;
        }

        ItemStack tool = event.getEntity().getMainHandItem();
        if (!SilkTouchTwoDropAdapter.isCorrectTool(event.getState(), tool)) {
            return;
        }

        if (!SilkTouchTwoDropAdapter.hasSilkTouchTwo(tool, event.getEntity().registryAccess())) {
            event.setNewSpeed(Math.min(event.getNewSpeed(), WRONG_ENCHANTMENT_SPEED));
        }
    }
}
