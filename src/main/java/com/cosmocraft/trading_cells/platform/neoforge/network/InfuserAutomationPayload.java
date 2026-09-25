package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipeDisplay;
import org.jspecify.annotations.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InfuserAutomationPayload(int containerId, String recipe, ItemStack result,
        @Nullable ArcaneInfusionRecipeDisplay display) implements CustomPacketPayload {
    public static final Type<InfuserAutomationPayload> PAYLOAD_TYPE = new Type<>(Identifier.fromNamespaceAndPath("trading_cells", "infuser_automation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfuserAutomationPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeContainerId(payload.containerId());
                buffer.writeUtf(payload.recipe(), 512);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.result());
                buffer.writeBoolean(payload.display() != null);
                if (payload.display() != null) { ArcaneInfusionRecipeDisplay.STREAM_CODEC.encode(buffer, payload.display()); }
            }, buffer -> new InfuserAutomationPayload(buffer.readContainerId(), buffer.readUtf(512),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    buffer.readBoolean() ? ArcaneInfusionRecipeDisplay.STREAM_CODEC.decode(buffer) : null));
    @Override public Type<? extends CustomPacketPayload> type() { return PAYLOAD_TYPE; }
    public static void handle(InfuserAutomationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ArcaneInfuserMenu menu && menu.containerId == payload.containerId()) {
                menu.setAutomationDisplay(payload.recipe(), payload.result(), payload.display());
            }
        });
    }
}
