package com.cosmocraft.trading_cells.feature.infusion.adapters.output.client;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ArcaneInfuserClientRegistrationAdapter {
    private ArcaneInfuserClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ArcaneInfuserRegistrationAdapter.MENU.get(), ArcaneInfuserScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ArcaneInfuserRegistrationAdapter.BLOCK_ENTITY.get(),
                ArcaneInfuserBlockEntityRenderer::new
        );
    }
}
