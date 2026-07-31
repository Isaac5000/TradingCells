package com.cosmocraft.trading_cells.feature.trader.adapters.output.client;

import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class NetheritePiglinBarteringCellClientRegistrationAdapter {
    private NetheritePiglinBarteringCellClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(
                TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_MENU.get(),
                NetheritePiglinBarteringCellScreen::new
        );
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TraderRegistrationAdapter.NETHERITE_PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(),
                NetheritePiglinBarteringCellBlockEntityRenderer::new
        );
    }
}
