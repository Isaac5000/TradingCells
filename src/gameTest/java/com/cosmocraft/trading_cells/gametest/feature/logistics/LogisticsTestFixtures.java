package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsNetworkManager;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

final class LogisticsTestFixtures {
    static final BlockPos ORIGIN = new BlockPos(1, 2, 2);
    static final BlockPos SOURCE = new BlockPos(0, 2, 2);
    static final BlockPos TARGET = new BlockPos(4, 2, 2);

    private LogisticsTestFixtures() {
    }

    static LogisticsPipeBlockEntity pipe(GameTestHelper helper, BlockPos pos, PipeKind kind) {
        helper.setBlock(pos, LogisticsRegistrationAdapter.pipeBlock(kind).get());
        var pipe = helper.getBlockEntity(pos, LogisticsPipeBlockEntity.class);
        pipe.refreshConnectionsAround();
        return pipe;
    }

    static LogisticsPipeBlockEntity line(GameTestHelper helper, PipeKind kind) {
        var origin = pipe(helper, ORIGIN, kind);
        pipe(helper, ORIGIN.east(), kind);
        pipe(helper, ORIGIN.east(2), kind);
        origin.refreshConnectionsAround();
        return origin;
    }

    static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos pos, Item item, int count) {
        helper.setBlock(pos, Blocks.BARREL);
        var barrel = helper.getBlockEntity(pos, BarrelBlockEntity.class);
        if (count > 0) {
            barrel.setItem(0, new ItemStack(item, count));
        }
        return barrel;
    }

    static int count(BarrelBlockEntity barrel, Item item) {
        return barrel.countItem(item);
    }

    static LogisticsNetworkManager network(GameTestHelper helper) {
        return LogisticsNetworkManager.get(helper.getLevel());
    }

    static net.minecraft.server.level.ServerPlayer connectedPlayer(
            GameTestHelper helper, net.minecraft.world.level.GameType mode
    ) {
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "logistics-test"),
                net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        var cookie = new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(), 0,
                net.minecraft.server.level.ClientInformation.createDefault(), false,
                net.neoforged.neoforge.network.connection.ConnectionType.OTHER);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player, cookie) {
            @Override
            public void send(net.minecraft.network.protocol.Packet<?> packet,
                             @org.jspecify.annotations.Nullable ChannelFutureListener listener) {
                // GameTests exercise server interaction without a client transport.
            }
        };
        player.setGameMode(mode);
        return player;
    }
}
