package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmMenu;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmMenu;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmMenu;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmMenu;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestMobFarmCatalogPayload(int containerId) implements CustomPacketPayload {
    public static final Type<RequestMobFarmCatalogPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "request_mob_farm_catalog"
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMobFarmCatalogPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeContainerId(payload.containerId()),
            buffer -> new RequestMobFarmCatalogPayload(buffer.readContainerId())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(RequestMobFarmCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)
                    || serverPlayer.containerMenu.containerId != payload.containerId()
                    || !serverPlayer.containerMenu.stillValid(serverPlayer)) {
                return;
            }
            if (serverPlayer.containerMenu instanceof SkeletonFarmMenu skeletonMenu) {
                PacketDistributor.sendToPlayer(serverPlayer, MobFarmCatalogSyncPayload.from(skeletonMenu));
            } else if (serverPlayer.containerMenu instanceof ZombieFarmMenu zombieMenu) {
                PacketDistributor.sendToPlayer(serverPlayer, MobFarmCatalogSyncPayload.from(zombieMenu));
            } else if (serverPlayer.containerMenu instanceof RaiderFarmMenu raiderMenu) {
                PacketDistributor.sendToPlayer(serverPlayer, MobFarmCatalogSyncPayload.from(raiderMenu));
            } else if (serverPlayer.containerMenu instanceof CreeperFarmMenu creeperMenu) {
                PacketDistributor.sendToPlayer(serverPlayer, MobFarmCatalogSyncPayload.from(creeperMenu));
            } else if (serverPlayer.containerMenu instanceof ConfiguredMobFarmMenu configuredMenu) {
                PacketDistributor.sendToPlayer(serverPlayer, MobFarmCatalogSyncPayload.from(configuredMenu));
            }
        });
    }
}
