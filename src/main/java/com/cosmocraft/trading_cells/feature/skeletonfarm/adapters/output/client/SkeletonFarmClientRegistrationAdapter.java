package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class SkeletonFarmClientRegistrationAdapter {
    private SkeletonFarmClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SkeletonFarmRegistrationAdapter.MENU.get(), SkeletonFarmScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                SkeletonFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                SkeletonFarmBlockEntityRenderer::new
        );
    }
}
