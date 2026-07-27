package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class AutotraderClientRegistrationAdapter {
    private AutotraderClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(AutotraderRegistrationAdapter.AUTOTRADER_MENU.get(), AutotraderScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AutotraderRegistrationAdapter.AUTOTRADER_BLOCK_ENTITY.get(), AutotraderBlockEntityRenderer::new);
    }
}
