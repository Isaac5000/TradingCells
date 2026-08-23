package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.ZombieFarmRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ZombieFarmClientRegistrationAdapter {
    private ZombieFarmClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ZombieFarmRegistrationAdapter.MENU.get(), ZombieFarmScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ZombieFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                ZombieFarmBlockEntityRenderer::new
        );
    }
}
