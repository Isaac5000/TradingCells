package com.cosmocraft.trading_cells.platform.neoforge.network;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeFaceConfiguration;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeUpgradeItem;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PipeConfigurationPayload(
        BlockPos pipePos,
        Direction face,
        InteractionHand hand,
        long expectedRevision,
        CompoundTag configuration
) implements CustomPacketPayload {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0D;
    public static final Type<PipeConfigurationPayload> PAYLOAD_TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "pipe_configuration")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeConfigurationPayload> STREAM_CODEC =
            StreamCodec.of(PipeConfigurationPayload::encode, PipeConfigurationPayload::decode);

    public PipeConfigurationPayload {
        if (pipePos == null || face == null || hand == null || configuration == null || expectedRevision < 0) {
            throw new IllegalArgumentException("Incomplete pipe configuration");
        }
        pipePos = pipePos.immutable();
        configuration = configuration.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(PipeConfigurationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handleOnServer(payload, player);
            }
        });
    }

    public static void handleOnServer(PipeConfigurationPayload payload, ServerPlayer player) {
        boolean editing = player.containerMenu instanceof
                com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationMenu menu
                && menu.pos().equals(payload.pipePos()) && menu.face() == payload.face()
                && menu.hand() == payload.hand() && menu.stillValid(player);
        if (!(player.level() instanceof ServerLevel level)
                || player.isSpectator()
                || !editing && !(player.getItemInHand(payload.hand()).getItem()
                instanceof com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeWrenchItem)
                || player.distanceToSqr(Vec3.atCenterOf(payload.pipePos()))
                > MAX_INTERACTION_DISTANCE_SQUARED
                || !level.mayInteract(player, payload.pipePos())
                || !player.mayBuild()) {
            return;
        }
        var chunk = level.getChunkSource().getChunkNow(
                payload.pipePos().getX() >> 4,
                payload.pipePos().getZ() >> 4
        );
        if (chunk == null
                || !(chunk.getBlockEntity(payload.pipePos()) instanceof LogisticsPipeBlockEntity pipe)) {
            return;
        }

        if ((!editing && pipe.revision() != payload.expectedRevision())
                || !PipeFaceConfiguration.isValidNetworkConfiguration(payload.configuration())) {
            return;
        }
        PipeFaceConfiguration requested = PipeFaceConfiguration.load(payload.configuration());
        if (requested.upgradeTier() != pipe.face(payload.face()).upgradeTier()) { return; }
        for (var type : com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType.values()) {
            var profile = requested.profile(type);
            requested.setProfile(type, new com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeResourceProfile(
                    profile.enabled(), profile.channel(), profile.filters().stream()
                    .filter(rule -> com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationClipboard.known(type, rule))
                    .toList(), profile.filterMode(), profile.routingMode()));
        }
        // Physical upgrades are exclusively moved by validated inventory slot transactions.
        pipe.applyFaceConfiguration(payload.face(), requested);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PipeConfigurationPayload payload) {
        buffer.writeLong(payload.pipePos().asLong());
        buffer.writeByte(payload.face().ordinal());
        buffer.writeByte(payload.hand().ordinal());
        buffer.writeVarLong(payload.expectedRevision());
        buffer.writeNbt(payload.configuration());
    }

    private static PipeConfigurationPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos pos = BlockPos.of(buffer.readLong());
        int faceId = buffer.readUnsignedByte();
        int handId = buffer.readUnsignedByte();
        long revision = buffer.readVarLong();
        CompoundTag configuration = buffer.readNbt();
        if (faceId >= Direction.values().length
                || handId >= InteractionHand.values().length
                || configuration == null) {
            throw new IllegalArgumentException("Unknown pipe configuration value");
        }
        return new PipeConfigurationPayload(
                pos,
                Direction.values()[faceId],
                InteractionHand.values()[handId],
                revision,
                configuration
        );
    }
}
