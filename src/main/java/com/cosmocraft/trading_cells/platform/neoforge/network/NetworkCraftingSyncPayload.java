package com.cosmocraft.trading_cells.platform.neoforge.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record NetworkCraftingSyncPayload(int containerId, List<ItemStack> grid, ItemStack result)
        implements CustomPacketPayload {
    public static final Type<NetworkCraftingSyncPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("trading_cells", "network_crafting_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkCraftingSyncPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                for (ItemStack stack : payload.grid()) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
                }
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.result());
            }, buffer -> {
                int id = buffer.readContainerId();
                List<ItemStack> grid = new ArrayList<>(9);
                for (int index = 0; index < 9; index++) {
                    grid.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
                }
                return new NetworkCraftingSyncPayload(id, grid, ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            });

    public NetworkCraftingSyncPayload {
        if (containerId < 0 || grid.size() != 9) {
            throw new IllegalArgumentException("Invalid crafting grid");
        }
        grid = grid.stream().map(ItemStack::copy).toList();
        result = result.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }
}
