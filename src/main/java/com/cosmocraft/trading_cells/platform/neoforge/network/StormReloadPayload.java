package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StormReloadPayload(InteractionHand hand, boolean eject) implements CustomPacketPayload {
    public static final Type<StormReloadPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "storm_reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StormReloadPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> { buffer.writeVarInt(value.hand().ordinal()); buffer.writeBoolean(value.eject()); },
            buffer -> new StormReloadPayload(buffer.readById(index -> InteractionHand.values()[index]), buffer.readBoolean()));
    public static void handle(StormReloadPayload payload, IPayloadContext context) {
        StormWeaponItem.requestReload(context.player(), payload.hand(), payload.eject());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
