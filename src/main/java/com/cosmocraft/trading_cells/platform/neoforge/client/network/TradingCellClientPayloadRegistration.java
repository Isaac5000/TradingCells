package com.cosmocraft.trading_cells.platform.neoforge.client.network;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellMenu;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderMenu;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellExperiencePayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.AutotraderMenuSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellMenuSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.QuarryCatalogSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.MobFarmCatalogSyncPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class TradingCellClientPayloadRegistration {
    private TradingCellClientPayloadRegistration() {
    }

    public static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(TradingCellExperiencePayload.PAYLOAD_TYPE, (payload, context) -> {
            TradingCellClientExperienceState.update(payload.containerId(), payload.experience());
            if (context.player().containerMenu instanceof VillagerTradingCellMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.setStoredExperience(payload.experience());
            }
        });
        event.register(AutotraderMenuSyncPayload.PAYLOAD_TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof AutotraderMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.applyServerState(
                        payload.hasVillager(),
                        payload.offers(),
                        payload.villagerData(),
                        payload.villagerXp(),
                        payload.canResetTrades(),
                        payload.offersRevision()
                );
            }
        });
        event.register(TradingCellMenuSyncPayload.PAYLOAD_TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof VillagerTradingCellMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.applyServerState(new VillagerTradingCellMenu.ServerState(
                        payload.offers(),
                        new VillagerTradingCellMenu.MerchantState(
                                payload.villagerData(),
                                payload.villagerLevel(),
                                payload.villagerXp()
                        ),
                        new VillagerTradingCellMenu.MenuState(
                                payload.storedExperience(),
                                payload.selectedOfferIndex(),
                                payload.showProgress(),
                                payload.canRestock(),
                                payload.canResetTrades(),
                                payload.offersRevision()
                        )
                ));
                TradingCellClientExperienceState.update(payload.containerId(), payload.storedExperience());
            }
        });
        event.register(QuarryCatalogSyncPayload.PAYLOAD_TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof QuarryMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.applyCatalogSnapshot(payload);
            }
        });
        event.register(MobFarmCatalogSyncPayload.PAYLOAD_TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof SkeletonFarmMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.applyCatalogSnapshot(payload);
            } else if (context.player().containerMenu instanceof ZombieFarmMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.applyCatalogSnapshot(payload);
            }
        });
    }
}
