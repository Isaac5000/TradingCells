package com.cosmocraft.trading_cells.platform.neoforge.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TradingCellPayloadRegistration {
    private TradingCellPayloadRegistration() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
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
        registrar.playToClient(TradingCellExperiencePayload.PAYLOAD_TYPE, TradingCellExperiencePayload.STREAM_CODEC);
        registrar.playToClient(TradingCellMenuSyncPayload.PAYLOAD_TYPE, TradingCellMenuSyncPayload.STREAM_CODEC);
        registrar.playToClient(AutotraderMenuSyncPayload.PAYLOAD_TYPE, AutotraderMenuSyncPayload.STREAM_CODEC);
        registrar.playToClient(QuarryCatalogSyncPayload.PAYLOAD_TYPE, QuarryCatalogSyncPayload.STREAM_CODEC);
    }
}
