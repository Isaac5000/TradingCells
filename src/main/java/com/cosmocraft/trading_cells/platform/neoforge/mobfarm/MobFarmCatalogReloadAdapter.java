package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineActivityController;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class MobFarmCatalogReloadAdapter {
    private static final Identifier LOOT_TABLE_LISTENER_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "mob_farm_loot_tables"
    );
    private static final Identifier TARGET_LISTENER_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "mob_farm_targets"
    );

    private MobFarmCatalogReloadAdapter() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LOOT_TABLE_LISTENER_ID, new MobFarmLootTableReloadListener());
        event.addListener(TARGET_LISTENER_ID, new MobFarmTargetReloadListener());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent.ServerDataLoad event) {
        MobFarmCatalog.refresh(event);
        MachineActivityController.wakeAll();
    }
}
