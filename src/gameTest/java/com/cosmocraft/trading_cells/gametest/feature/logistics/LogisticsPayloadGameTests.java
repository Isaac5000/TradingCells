package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalMenu;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeFaceConfiguration;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalActionPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.NetworkTerminalSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.PipeConfigurationPayload;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.connection.ConnectionType;

public final class LogisticsPayloadGameTests {
    private LogisticsPayloadGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("logistics_pipe_payload_bounds", 20, LogisticsPayloadGameTests::pipe),
                new GameTestCase("logistics_channel_payload_bounds", 20, LogisticsPayloadGameTests::channels),
                new GameTestCase("logistics_terminal_payload_long_amounts", 20, LogisticsPayloadGameTests::terminal),
                new GameTestCase("logistics_terminal_ignores_stale_revision", 20, LogisticsPayloadGameTests::revisions));
    }

    private static void channels(GameTestHelper helper) {
        var query = new com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelQueryPayload(2, LogisticsResourceType.GAS, "x".repeat(256), true);
        helper.assertValueEqual(roundTrip(helper, com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelQueryPayload.STREAM_CODEC, query), query, "Maximum query roundtrip");
        var result = new com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload(2, LogisticsResourceType.GAS, "x",
                java.util.Collections.nCopies(16, "x".repeat(256)), true);
        helper.assertValueEqual(roundTrip(helper, com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload.STREAM_CODEC, result), result, "Maximum response roundtrip");
        var state = new com.cosmocraft.trading_cells.platform.neoforge.network.PipeMenuSyncPayload(2, Long.MAX_VALUE, Long.MAX_VALUE, new PipeFaceConfiguration().save());
        helper.assertValueEqual(roundTrip(helper, com.cosmocraft.trading_cells.platform.neoforge.network.PipeMenuSyncPayload.STREAM_CODEC, state), state, "Menu revision and physical generation roundtrip");
        var buffer = buffer(helper);
        try {
            buffer.writeContainerId(2); buffer.writeEnum(LogisticsResourceType.ITEM); buffer.writeUtf(""); buffer.writeVarInt(Integer.MAX_VALUE);
            boolean rejected = false;
            try { com.cosmocraft.trading_cells.platform.neoforge.network.PipeChannelSuggestionsPayload.STREAM_CODEC.decode(buffer); }
            catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Reject oversized result count before allocating entries");
        } finally { buffer.release(); }
        helper.succeed();
    }

    private static void pipe(GameTestHelper helper) {
        var face = new PipeFaceConfiguration();
        face.setPriority(Integer.MIN_VALUE);
        var payload = new PipeConfigurationPayload(new BlockPos(-30, -60, 90), Direction.WEST,
                InteractionHand.OFF_HAND, Long.MAX_VALUE, face.save());
        helper.assertValueEqual(roundTrip(helper, PipeConfigurationPayload.STREAM_CODEC, payload), payload,
                "Exact position, hand, priority and revision round trip");
        for (int invalidField = 0; invalidField < 3; invalidField++) {
            var input = buffer(helper);
            try {
                input.writeLong(BlockPos.ZERO.asLong());
                input.writeByte(invalidField == 0 ? 255 : Direction.UP.ordinal());
                input.writeByte(invalidField == 1 ? 255 : InteractionHand.MAIN_HAND.ordinal());
                input.writeVarLong(invalidField == 2 ? -1 : 0);
                input.writeNbt(face.save());
                boolean rejected = false;
                try {
                    PipeConfigurationPayload.STREAM_CODEC.decode(input);
                } catch (IllegalArgumentException expected) {
                    rejected = true;
                }
                helper.assertTrue(rejected, "Reject invalid face, hand or revision before applying configuration");
            } finally {
                input.release();
            }
        }
        helper.succeed();
    }

    private static void terminal(GameTestHelper helper) {
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            var action = new NetworkTerminalActionPayload(7, NetworkTerminalActionPayload.Action.WITHDRAW,
                    type, 0, "", InteractionHand.OFF_HAND, id("trading_cells", type.serializedName()),
                    id("minecraft", "diamond"), "{}", Long.MAX_VALUE);
            helper.assertValueEqual(roundTrip(helper, NetworkTerminalActionPayload.STREAM_CODEC, action), action,
                    "Terminal preserves long quantities and resource identity");
        }
        var entry = new NetworkTerminalSyncPayload.Entry(id("trading_cells", "item"), id("minecraft", "diamond"),
                "{}", "Diamond", new ItemStack(Items.DIAMOND), Long.MAX_VALUE);
        var page = new NetworkTerminalSyncPayload(1, 7, LogisticsResourceType.ITEM, 0, 1, 8,
                true, List.of(entry), List.of());
        var decoded = roundTrip(helper, NetworkTerminalSyncPayload.STREAM_CODEC, page);
        helper.assertValueEqual(decoded.entries().getFirst().amount(), Long.MAX_VALUE, "Listing amount cannot overflow");
        helper.assertTrue(ItemStack.matches(decoded.entries().getFirst().icon(), entry.icon()), "Icon round trip");
        helper.succeed();
    }

    private static void revisions(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var menu = new NetworkTerminalMenu(7, player.getInventory(), false);
        var entry = new NetworkTerminalSyncPayload.Entry(id("trading_cells", "item"), id("minecraft", "diamond"),
                "{}", "Diamond", new ItemStack(Items.DIAMOND), 64);
        menu.applyServerState(new NetworkTerminalSyncPayload(1, 7, LogisticsResourceType.ITEM, 0, 1, 9,
                true, List.of(entry), List.of()));
        menu.applyServerState(new NetworkTerminalSyncPayload(1, 7, LogisticsResourceType.FLUID, 0, 1, 8,
                true, List.of(), List.of()));
        helper.assertValueEqual(menu.revision(), 9L, "Older revision ignored");
        helper.assertValueEqual(menu.selectedType(), LogisticsResourceType.ITEM, "Older page cannot change tab");
        helper.assertValueEqual(menu.entries().size(), 1, "Older page cannot erase entries");
        helper.succeed();
    }

    private static RegistryFriendlyByteBuf buffer(GameTestHelper helper) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.OTHER);
    }

    private static <T> T roundTrip(GameTestHelper helper, StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        var buffer = buffer(helper);
        try {
            codec.encode(buffer, value);
            T decoded = codec.decode(buffer);
            helper.assertValueEqual(buffer.readableBytes(), 0, "No trailing payload bytes");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
