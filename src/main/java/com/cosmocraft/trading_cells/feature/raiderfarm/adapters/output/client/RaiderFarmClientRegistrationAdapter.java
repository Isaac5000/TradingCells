package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.RaiderFarmRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class RaiderFarmClientRegistrationAdapter {
    private RaiderFarmClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(RaiderFarmRegistrationAdapter.MENU.get(), RaiderFarmScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                RaiderFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                RaiderFarmBlockEntityRenderer::new
        );
    }
}
