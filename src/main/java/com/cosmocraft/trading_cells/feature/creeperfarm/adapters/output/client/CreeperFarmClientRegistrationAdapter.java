package com.cosmocraft.trading_cells.feature.creeperfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.output.CreeperFarmRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class CreeperFarmClientRegistrationAdapter {
    private CreeperFarmClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CreeperFarmRegistrationAdapter.MENU.get(), CreeperFarmScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                CreeperFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                CreeperFarmBlockEntityRenderer::new
        );
    }
}
