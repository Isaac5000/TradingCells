package com.cosmocraft.trading_cells.feature.quarry.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class QuarryCatalogReloadAdapter {
    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "quarry_materials"
    );

    private QuarryCatalogReloadAdapter() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new QuarryMaterialReloadListener());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent.ServerDataLoad event) {
        QuarryMaterialCatalog.refreshDynamic();
        MachineActivityController.wakeAll();
    }
}
