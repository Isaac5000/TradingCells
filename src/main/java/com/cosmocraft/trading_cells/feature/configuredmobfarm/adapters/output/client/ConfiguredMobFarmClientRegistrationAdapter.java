package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.ConfiguredMobFarmRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ConfiguredMobFarmClientRegistrationAdapter {
    private ConfiguredMobFarmClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ConfiguredMobFarmRegistrationAdapter.MENU.get(), ConfiguredMobFarmScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ConfiguredMobFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                ConfiguredMobFarmBlockEntityRenderer::new
        );
    }
}
