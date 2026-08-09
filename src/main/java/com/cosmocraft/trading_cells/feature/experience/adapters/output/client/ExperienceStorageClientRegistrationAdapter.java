package com.cosmocraft.trading_cells.feature.experience.adapters.output.client;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ExperienceStorageClientRegistrationAdapter {
    private ExperienceStorageClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ExperienceStorageRegistrationAdapter.MENU.get(), ExperienceStorageScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ExperienceStorageRegistrationAdapter.BLOCK_ENTITY.get(),
                ExperienceStorageBlockEntityRenderer::new
        );
    }
}
