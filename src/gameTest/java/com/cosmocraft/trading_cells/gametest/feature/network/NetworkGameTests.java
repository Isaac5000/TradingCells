package com.cosmocraft.trading_cells.gametest.feature.network;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.network.ArcaneInfuserTransferPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.AutotraderMenuSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.ExperienceStorageTransferPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.ExtractTradingCellExperiencePayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.MobFarmCatalogSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.QuarryCatalogSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.RequestMobFarmCatalogPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.RequestQuarryCatalogPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.ResetTradesPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.SelectAutotraderOfferPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.SelectTradingCellOfferPayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellExperiencePayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellMenuSyncPayload;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.connection.ConnectionType;

/** Behaviour-oriented GameTests for Network. */
public final class NetworkGameTests {
    private NetworkGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("payload_codec_round_trip", 20,
                    NetworkGameTests::payloadCodecRoundTrip)
        );
    }

    private static void payloadCodecRoundTrip(GameTestHelper helper) {
        assertPayloadId(helper, ArcaneInfuserTransferPayload.PAYLOAD_TYPE, "arcane_infuser_transfer");
        assertPayloadId(helper, ExperienceStorageTransferPayload.PAYLOAD_TYPE, "experience_storage_transfer");
        assertPayloadId(
                helper,
                ExtractTradingCellExperiencePayload.PAYLOAD_TYPE,
                "extract_trading_cell_experience"
        );
        assertPayloadId(helper, ResetTradesPayload.PAYLOAD_TYPE, "reset_trades");
        assertPayloadId(helper, SelectAutotraderOfferPayload.PAYLOAD_TYPE, "select_autotrader_offer");
        assertPayloadId(helper, SelectTradingCellOfferPayload.PAYLOAD_TYPE, "select_trading_cell_offer");
        assertPayloadId(helper, TradingCellExperiencePayload.PAYLOAD_TYPE, "trading_cell_experience");
        assertPayloadId(helper, TradingCellMenuSyncPayload.PAYLOAD_TYPE, "trading_cell_menu_sync");
        assertPayloadId(helper, AutotraderMenuSyncPayload.PAYLOAD_TYPE, "autotrader_menu_sync");
        assertPayloadId(helper, RequestQuarryCatalogPayload.PAYLOAD_TYPE, "request_quarry_catalog");
        assertPayloadId(helper, QuarryCatalogSyncPayload.PAYLOAD_TYPE, "quarry_catalog_sync");
        assertPayloadId(helper, RequestMobFarmCatalogPayload.PAYLOAD_TYPE, "request_mob_farm_catalog");
        assertPayloadId(helper, MobFarmCatalogSyncPayload.PAYLOAD_TYPE, "mob_farm_catalog_sync");

        helper.assertValueEqual(
                new ArcaneInfuserTransferPayload(1, (byte) 0, -1).requestedLevels(),
                0,
                "Negative Infuser level request"
        );
        helper.assertValueEqual(
                new ExperienceStorageTransferPayload(1, (byte) 0, -1).requestedLevels(),
                0,
                "Negative Experience Storage level request"
        );
        helper.assertTrue(
                !ExtractTradingCellExperiencePayload.isSupportedMode((byte) -1),
                "Unknown Trader XP extraction modes must be rejected"
        );
        helper.assertValueEqual(
                new ResetTradesPayload(1, -1).knownOffersRevision(),
                -1,
                "A stale negative trade revision must not be rewritten as revision zero"
        );
        SelectAutotraderOfferPayload invalidAutotraderSelection =
                new SelectAutotraderOfferPayload(1, -1, -1);
        helper.assertValueEqual(
                invalidAutotraderSelection.selectedOfferIndex(),
                -1,
                "A negative Autotrader offer must remain invalid"
        );
        SelectTradingCellOfferPayload invalidTraderSelection =
                new SelectTradingCellOfferPayload(1, -1, -1);
        helper.assertValueEqual(
                invalidTraderSelection.selectedOfferIndex(),
                -1,
                "A negative Trader offer must remain invalid"
        );

        assertCodecRoundTrip(
                helper,
                ArcaneInfuserTransferPayload.STREAM_CODEC,
                new ArcaneInfuserTransferPayload(7, (byte) 3, Integer.MAX_VALUE),
                "Arcane Infuser transfer"
        );
        assertCodecRoundTrip(
                helper,
                ExperienceStorageTransferPayload.STREAM_CODEC,
                new ExperienceStorageTransferPayload(8, (byte) 2, 21_863),
                "Experience Storage transfer"
        );
        assertCodecRoundTrip(
                helper,
                ExtractTradingCellExperiencePayload.STREAM_CODEC,
                new ExtractTradingCellExperiencePayload(9, ExtractTradingCellExperiencePayload.NEXT_LEVEL),
                "Trader XP extraction"
        );
        assertCodecRoundTrip(
                helper,
                ResetTradesPayload.STREAM_CODEC,
                new ResetTradesPayload(10, 42),
                "Trade reset"
        );
        assertCodecRoundTrip(
                helper,
                SelectAutotraderOfferPayload.STREAM_CODEC,
                new SelectAutotraderOfferPayload(11, 7, 43),
                "Autotrader offer selection"
        );
        assertCodecRoundTrip(
                helper,
                SelectTradingCellOfferPayload.STREAM_CODEC,
                new SelectTradingCellOfferPayload(12, 5, 44),
                "Trader offer selection"
        );
        assertCodecRoundTrip(
                helper,
                TradingCellExperiencePayload.STREAM_CODEC,
                new TradingCellExperiencePayload(13, Integer.MAX_VALUE),
                "Trader XP sync"
        );
        assertCodecRoundTrip(
                helper,
                RequestQuarryCatalogPayload.STREAM_CODEC,
                new RequestQuarryCatalogPayload(14),
                "Quarry catalog request"
        );
        assertCodecRoundTrip(
                helper,
                RequestMobFarmCatalogPayload.STREAM_CODEC,
                new RequestMobFarmCatalogPayload(15),
                "Mob farm catalog request"
        );

        Identifier stoneId = Identifier.fromNamespaceAndPath("minecraft", "stone");
        QuarryCatalogSyncPayload.Entry quarryEntry = new QuarryCatalogSyncPayload.Entry(
                stoneId,
                new ItemStack(Items.STONE),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.STONE),
                ItemStack.EMPTY,
                125_000,
                1,
                2,
                0,
                1.0D,
                true,
                false,
                false,
                0,
                "minecraft"
        );
        assertCodecRoundTrip(
                helper,
                QuarryCatalogSyncPayload.STREAM_CODEC,
                new QuarryCatalogSyncPayload(16, 5, false, List.of(quarryEntry)),
                "Quarry catalog sync"
        );

        Identifier skeletonId = Identifier.fromNamespaceAndPath("minecraft", "skeleton");
        Identifier skeletonEggId = Identifier.fromNamespaceAndPath("minecraft", "skeleton_spawn_egg");
        Identifier boneId = Identifier.fromNamespaceAndPath("minecraft", "bone");
        assertCodecRoundTrip(
                helper,
                MobFarmCatalogSyncPayload.STREAM_CODEC,
                new MobFarmCatalogSyncPayload(
                        MobFarmCatalogSyncPayload.CURRENT_PROTOCOL_VERSION,
                        17,
                        6,
                        MobFarmCatalog.Family.SKELETON.id(),
                        skeletonId,
                        List.of(new MobFarmCatalogSyncPayload.TargetEntry(
                                skeletonId,
                                skeletonEggId,
                                List.of(boneId)
                        )),
                        Set.of(boneId)
                ),
                "Mob farm catalog sync"
        );
        assertMobFarmCatalogRejectsProtocol(helper, 0);
        assertMobFarmCatalogRejectsProtocol(helper, MobFarmCatalogSyncPayload.CURRENT_PROTOCOL_VERSION + 1);
        assertMobFarmCatalogRejectsUnknownFamily(helper);
        assertMobFarmCatalogRejectsOversizedTargets(helper);

        var registries = helper.getLevel().registryAccess();
        VillagerData villagerData = new VillagerData(
                registries.lookupOrThrow(Registries.VILLAGER_TYPE).getOrThrow(VillagerType.PLAINS),
                registries.lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER),
                3
        );
        assertCodecRoundTrip(
                helper,
                TradingCellMenuSyncPayload.STREAM_CODEC,
                new TradingCellMenuSyncPayload(
                        18,
                        new MerchantOffers(),
                        villagerData,
                        3,
                        75,
                        1_234,
                        2,
                        true,
                        true,
                        false,
                        7
                ),
                "Trader menu sync"
        );
        assertCodecRoundTrip(
                helper,
                AutotraderMenuSyncPayload.STREAM_CODEC,
                new AutotraderMenuSyncPayload(
                        19,
                        true,
                        new MerchantOffers(),
                        villagerData,
                        75,
                        true,
                        8
                ),
                "Autotrader menu sync"
        );
        helper.succeed();
    }

    private static void assertPayloadId(
            GameTestHelper helper,
            CustomPacketPayload.Type<?> type,
            String expectedPath
    ) {
        helper.assertValueEqual(
                type.id(),
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, expectedPath),
                "Frozen payload identifier"
        );
    }

    private static void assertMobFarmCatalogRejectsProtocol(GameTestHelper helper, int protocolVersion) {
        RegistryFriendlyByteBuf input = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                helper.getLevel().registryAccess(),
                ConnectionType.OTHER
        );
        try {
            input.writeVarInt(protocolVersion);
            input.readerIndex(0);
            try {
                MobFarmCatalogSyncPayload.STREAM_CODEC.decode(input);
                helper.fail("Mob farm catalog accepted protocol " + protocolVersion);
            } catch (IllegalArgumentException expected) {
                // Rejection is the protocol contract for unknown versions.
            }
        } finally {
            input.release();
        }
    }

    private static void assertMobFarmCatalogRejectsUnknownFamily(GameTestHelper helper) {
        assertMobFarmCatalogDecodeFails(helper, "unknown family", input -> {
            input.writeVarInt(MobFarmCatalogSyncPayload.CURRENT_PROTOCOL_VERSION);
            input.writeContainerId(1);
            input.writeVarInt(0);
            input.writeIdentifier(id("unknown_family"));
        });
    }

    private static void assertMobFarmCatalogRejectsOversizedTargets(GameTestHelper helper) {
        assertMobFarmCatalogDecodeFails(helper, "oversized target list", input -> {
            input.writeVarInt(MobFarmCatalogSyncPayload.CURRENT_PROTOCOL_VERSION);
            input.writeContainerId(1);
            input.writeVarInt(0);
            input.writeIdentifier(MobFarmCatalog.Family.SKELETON.id());
            input.writeIdentifier(Identifier.withDefaultNamespace("skeleton"));
            input.writeVarInt(513);
        });
    }

    private static void assertMobFarmCatalogDecodeFails(
            GameTestHelper helper,
            String label,
            Consumer<RegistryFriendlyByteBuf> writer
    ) {
        RegistryFriendlyByteBuf input = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                helper.getLevel().registryAccess(),
                ConnectionType.OTHER
        );
        try {
            writer.accept(input);
            input.readerIndex(0);
            try {
                MobFarmCatalogSyncPayload.STREAM_CODEC.decode(input);
                helper.fail("Mob farm catalog accepted " + label);
            } catch (IllegalArgumentException expected) {
                // Malformed catalogs are rejected before they reach a menu.
            }
        } finally {
            input.release();
        }
    }

    private static <T> void assertCodecRoundTrip(
            GameTestHelper helper,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            T value,
            String label
    ) {
        byte[] encoded = encode(helper, codec, value);
        RegistryFriendlyByteBuf input = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(encoded),
                helper.getLevel().registryAccess(),
                ConnectionType.OTHER
        );
        T decoded;
        try {
            decoded = codec.decode(input);
            helper.assertValueEqual(input.readableBytes(), 0, label + " unread bytes");
        } finally {
            input.release();
        }
        byte[] reencoded = encode(helper, codec, decoded);
        helper.assertTrue(Arrays.equals(encoded, reencoded), label + " changed after codec round trip");
    }

    private static <T> byte[] encode(
            GameTestHelper helper,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            T value
    ) {
        RegistryFriendlyByteBuf output = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                helper.getLevel().registryAccess(),
                ConnectionType.OTHER
        );
        try {
            codec.encode(output, value);
            byte[] bytes = new byte[output.readableBytes()];
            output.getBytes(output.readerIndex(), bytes);
            return bytes;
        } finally {
            output.release();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
    }
}
