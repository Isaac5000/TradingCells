package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MobSimulationLootPayload(int containerId, int revision, List<MobFarmMenu.LootEntry> entries) implements CustomPacketPayload {
    public static final Type<MobSimulationLootPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "mob_simulation_loot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobSimulationLootPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeContainerId(payload.containerId());
        buffer.writeVarInt(payload.revision());
        buffer.writeVarInt(payload.entries().size());
        for (var entry : payload.entries()) {
            ItemStack.STREAM_CODEC.encode(buffer, entry.stack());
            buffer.writeBoolean(entry.enabled());
            buffer.writeVarInt(entry.probability());
            buffer.writeVarInt(entry.minimum());
            buffer.writeVarInt(entry.maximum());
        }
    }, buffer -> {
        int id = buffer.readContainerId();
        int revision = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > 2_048) { throw new IllegalArgumentException("Invalid simulation loot count"); }
        List<MobFarmMenu.LootEntry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new MobFarmMenu.LootEntry(ItemStack.STREAM_CODEC.decode(buffer), buffer.readBoolean(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
        }
        return new MobSimulationLootPayload(id, revision, entries);
    });

    public MobSimulationLootPayload {
        if (entries.size() > 2_048) { throw new IllegalArgumentException("Invalid simulation loot count"); }
        entries = List.copyOf(entries);
    }

    public static void handle(MobSimulationLootPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof MobFarmMenu menu && menu.containerId == payload.containerId()) {
            menu.applyLootSnapshot(payload.revision(), payload.entries());
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
