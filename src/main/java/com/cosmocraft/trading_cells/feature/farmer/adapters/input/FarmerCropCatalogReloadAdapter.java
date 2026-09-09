package com.cosmocraft.trading_cells.feature.farmer.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class FarmerCropCatalogReloadAdapter {
    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "farmer_crops"
    );

    private FarmerCropCatalogReloadAdapter() {
    }

    @SubscribeEvent
    public static void addReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new FarmerCropReloadListener());
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        FarmerCropStackAdapter.refreshCatalogs();
    }

    @SubscribeEvent
    public static void datapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            FarmerCropStackAdapter.refreshCatalogs();
        }
    }
}
