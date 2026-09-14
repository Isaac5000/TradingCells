package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MobSimulationFilterPayload(int containerId, int revision, Identifier item) implements CustomPacketPayload {
    public static final Type<MobSimulationFilterPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "mob_simulation_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobSimulationFilterPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeIdentifier(payload.item());
    }, buffer -> new MobSimulationFilterPayload(buffer.readContainerId(), buffer.readVarInt(), buffer.readIdentifier()));
    public static void handle(MobSimulationFilterPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof MobFarmMenu menu && menu.containerId == payload.containerId()) {
            menu.toggleLoot(context.player(), payload.revision(), payload.item());
        }
    }
    @Override public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
