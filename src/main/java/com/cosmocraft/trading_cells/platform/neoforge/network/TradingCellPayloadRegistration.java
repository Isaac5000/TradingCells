package com.cosmocraft.trading_cells.platform.neoforge.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TradingCellPayloadRegistration {
    private TradingCellPayloadRegistration() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(InfuserAutomationPayload.PAYLOAD_TYPE, InfuserAutomationPayload.STREAM_CODEC, InfuserAutomationPayload::handle);
        registrar.playToServer(
                ResetTradesPayload.PAYLOAD_TYPE,
                ResetTradesPayload.STREAM_CODEC,
                ResetTradesPayload::handle
        );
        registrar.playToServer(
                SelectAutotraderOfferPayload.PAYLOAD_TYPE,
                SelectAutotraderOfferPayload.STREAM_CODEC,
                SelectAutotraderOfferPayload::handle
        );
        registrar.playToServer(
                SelectTradingCellOfferPayload.PAYLOAD_TYPE,
                SelectTradingCellOfferPayload.STREAM_CODEC,
                SelectTradingCellOfferPayload::handle
        );
        registrar.playToServer(
                ExtractTradingCellExperiencePayload.PAYLOAD_TYPE,
                ExtractTradingCellExperiencePayload.STREAM_CODEC,
                ExtractTradingCellExperiencePayload::handle
        );
        registrar.playToServer(
                RequestQuarryCatalogPayload.PAYLOAD_TYPE,
                RequestQuarryCatalogPayload.STREAM_CODEC,
                RequestQuarryCatalogPayload::handle
        );
        registrar.playToServer(
                ExperienceStorageTransferPayload.PAYLOAD_TYPE,
                ExperienceStorageTransferPayload.STREAM_CODEC,
                ExperienceStorageTransferPayload::handle
        );
        registrar.playToServer(
                ArcaneInfuserTransferPayload.PAYLOAD_TYPE,
                ArcaneInfuserTransferPayload.STREAM_CODEC,
                ArcaneInfuserTransferPayload::handle
        );
        registrar.playToServer(
                PipeConfigurationPayload.PAYLOAD_TYPE,
                PipeConfigurationPayload.STREAM_CODEC,
                PipeConfigurationPayload::handle
        );
        registrar.playToServer(
                NetworkTerminalActionPayload.PAYLOAD_TYPE,
                NetworkTerminalActionPayload.STREAM_CODEC,
                NetworkTerminalActionPayload::handle
        );
        registrar.playToClient(TradingCellExperiencePayload.PAYLOAD_TYPE, TradingCellExperiencePayload.STREAM_CODEC);
        registrar.playToClient(TradingCellMenuSyncPayload.PAYLOAD_TYPE, TradingCellMenuSyncPayload.STREAM_CODEC);
        registrar.playToClient(AutotraderMenuSyncPayload.PAYLOAD_TYPE, AutotraderMenuSyncPayload.STREAM_CODEC);
        registrar.playToClient(QuarryCatalogSyncPayload.PAYLOAD_TYPE, QuarryCatalogSyncPayload.STREAM_CODEC);
        registrar.playToClient(NetworkTerminalSyncPayload.PAYLOAD_TYPE, NetworkTerminalSyncPayload.STREAM_CODEC);
        registrar.playToClient(NetworkCraftingSyncPayload.PAYLOAD_TYPE, NetworkCraftingSyncPayload.STREAM_CODEC);
        registrar.playToClient(PipeMenuSyncPayload.PAYLOAD_TYPE, PipeMenuSyncPayload.STREAM_CODEC);
        registrar.playToClient(PipeChannelSuggestionsPayload.PAYLOAD_TYPE, PipeChannelSuggestionsPayload.STREAM_CODEC);
        registrar.playToServer(PipeChannelQueryPayload.PAYLOAD_TYPE, PipeChannelQueryPayload.STREAM_CODEC, PipeChannelQueryPayload::handle);
        registrar.playToClient(MobSimulationLootPayload.PAYLOAD_TYPE, MobSimulationLootPayload.STREAM_CODEC, MobSimulationLootPayload::handle);
        registrar.playToServer(MobSimulationFilterPayload.PAYLOAD_TYPE, MobSimulationFilterPayload.STREAM_CODEC, MobSimulationFilterPayload::handle);
    }
}
