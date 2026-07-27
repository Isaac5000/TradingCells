package com.cosmocraft.trading_cells.platform.neoforge.event;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.AutotraderScreen;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.VillagerTradingCellScreen;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.network.TradingCellClientExperienceState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/** Shared keyboard/cleanup hooks for the two custom villager trading screens. */
@EventBusSubscriber(modid = TradingCells.MOD_ID, value = Dist.CLIENT)
public final class TradingCellClientScreenEvent {
    private TradingCellClientScreenEvent() {
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof VillagerTradingCellScreen screen) {
            TradingCellClientExperienceState.clear(screen.getMenu().containerId);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() == GLFW.GLFW_KEY_C
                && event.getScreen() instanceof AutotraderScreen screen
                && screen.resetTradesFromShortcut()) {
            event.setCanceled(true);
        }
    }
}
