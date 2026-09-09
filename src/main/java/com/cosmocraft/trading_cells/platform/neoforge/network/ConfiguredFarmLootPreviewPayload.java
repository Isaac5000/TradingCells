package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmDropRules.BaseDrop;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfiguredFarmLootPreviewPayload(int containerId, Identifier target, Map<Identifier, BaseDrop> drops)
        implements CustomPacketPayload {
    public static final Type<ConfiguredFarmLootPreviewPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("trading_cells", "configured_farm_loot_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfiguredFarmLootPreviewPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeIdentifier(payload.target());
                buffer.writeVarInt(payload.drops().size());
                payload.drops().forEach((id, drop) -> {
                    buffer.writeIdentifier(id);
                    buffer.writeVarInt(drop.probabilityPartsPerMillion());
                    buffer.writeVarInt(drop.minimumAmount());
                    buffer.writeVarInt(drop.maximumAmount());
                });
            }, buffer -> {
                int menu = buffer.readContainerId();
                Identifier target = buffer.readIdentifier();
                int size = buffer.readVarInt();
                if (size < 0 || size > 2048) { throw new IllegalArgumentException("Invalid loot preview size"); }
                Map<Identifier, BaseDrop> drops = new LinkedHashMap<>();
                for (int index = 0; index < size; index++) {
                    drops.put(buffer.readIdentifier(), new BaseDrop(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
                }
                return new ConfiguredFarmLootPreviewPayload(menu, target, drops);
            });

    public ConfiguredFarmLootPreviewPayload {
        if (containerId < 0 || target == null || drops.size() > 2048) { throw new IllegalArgumentException("Invalid loot preview"); }
        drops = Map.copyOf(drops);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
}
