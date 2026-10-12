package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuarryFilterSyncPayload(int containerId, int revision, List<Identifier> materials) implements CustomPacketPayload {
    public static final Type<QuarryFilterSyncPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "quarry_filter_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuarryFilterSyncPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeVarInt(payload.materials().size());
        payload.materials().forEach(buffer::writeIdentifier);
    }, buffer -> {
        int id = buffer.readContainerId();
        int revision = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > 2_048) { throw new IllegalArgumentException("Invalid quarry filter count"); }
        List<Identifier> materials = new ArrayList<>(size);
        for (int index = 0; index < size; index++) { materials.add(buffer.readIdentifier()); }
        return new QuarryFilterSyncPayload(id, revision, materials);
    });

    public QuarryFilterSyncPayload {
        if (materials.size() > 2_048) { throw new IllegalArgumentException("Invalid quarry filter count"); }
        materials = List.copyOf(materials);
    }

    public static void handle(QuarryFilterSyncPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof QuarryMenu menu && menu.containerId == payload.containerId()) {
            menu.applyFilterSnapshot(payload.revision(), payload.materials());
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
