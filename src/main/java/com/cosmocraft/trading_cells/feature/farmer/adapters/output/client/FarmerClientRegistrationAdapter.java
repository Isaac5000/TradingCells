package com.cosmocraft.trading_cells.feature.farmer.adapters.output.client;

import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class FarmerClientRegistrationAdapter {
    private FarmerClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(FarmerRegistrationAdapter.FARMER_MENU.get(), FarmerScreen::new);
        event.register(FarmerRegistrationAdapter.PIGLIN_FARMER_MENU.get(), FarmerScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FarmerRegistrationAdapter.FARMER_BLOCK_ENTITY.get(), FarmerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK_ENTITY.get(),
                FarmerBlockEntityRenderer::new
        );
    }
}
