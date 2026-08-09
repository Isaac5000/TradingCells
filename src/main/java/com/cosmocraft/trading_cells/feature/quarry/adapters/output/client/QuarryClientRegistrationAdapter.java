package com.cosmocraft.trading_cells.feature.quarry.adapters.output.client;

import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class QuarryClientRegistrationAdapter {
    private QuarryClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(QuarryRegistrationAdapter.QUARRY_MENU.get(), QuarryScreen::new);
        event.register(QuarryRegistrationAdapter.PIGLIN_QUARRY_MENU.get(), QuarryScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                QuarryRegistrationAdapter.QUARRY_BLOCK_ENTITY.get(),
                QuarryBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK_ENTITY.get(),
                QuarryBlockEntityRenderer::new
        );
    }
}
