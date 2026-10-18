package com.cosmocraft.trading_cells.gametest.feature.trader;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellMenu;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Exercises the open menu, including the snapshot sent after a career promotion. */
public final class VillagerTradingGameTests {
    private VillagerTradingGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("villager_level_up_open_menu", 40, VillagerTradingGameTests::levelUp));
    }

    private static void levelUp(GameTestHelper helper) {
        var villager = EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(villager != null, "Villager created");
        var farmer = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .getOrThrow(VillagerProfession.FARMER);
        villager.setVillagerData(villager.getVillagerData().withProfession(farmer).withLevel(1));
        villager.setVillagerXp(9);
        var offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.BREAD), 12, 2, 0.05F));
        villager.setOffers(offers);
        var kind = CapturedMobKind.VILLAGER;
        var capture = new ItemStack(CapturedMobStackAdapter.capturerItem(kind));
        CapturedMobStackAdapter.setData(kind, capture, CapturedMobStackAdapter.createVillagerData(villager));
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("trading_cells", "villager_trading_cell"));
        helper.setBlock(GameTestFixtures.TEST_POS, block);
        var cell = helper.getBlockEntity(GameTestFixtures.TEST_POS, VillagerTradingCellBlockEntity.class);
        cell.insertVillagerFromCapturer(capture);
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "trading-test"),
                net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public boolean isClientAuthoritative() { return false; }
        };
        var cookie = new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(), 0,
                net.minecraft.server.level.ClientInformation.createDefault(), false,
                net.neoforged.neoforge.network.connection.ConnectionType.OTHER);
        var sent = new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player, cookie) {
            @Override
            public void send(net.minecraft.network.protocol.Packet<?> packet,
                    @org.jspecify.annotations.Nullable ChannelFutureListener listener) {
                sent.add(packet);
            }
        };
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(GameTestFixtures.TEST_POS)));
        try {
            cell.openTrade(player);
            helper.assertTrue(player.containerMenu instanceof VillagerTradingCellMenu, "Trading menu opened");
            var menu = (VillagerTradingCellMenu) player.containerMenu;
            int previousRevision = menu.offersRevision();
            int previousCount = menu.getOffers().size();
            menu.getSlot(0).set(new ItemStack(Items.EMERALD, 16));
            menu.slotsChanged(menu.getSlot(0).container);
            helper.assertTrue(menu.getSlot(2).hasItem(), "Trade result available");
            menu.clicked(2, 0, ContainerInput.PICKUP, player);
            helper.assertTrue(player.containerMenu == menu, "Same menu remains open");
            helper.assertValueEqual(menu.merchantLevel(), 2, "Level refreshed without reopening");
            helper.assertTrue(menu.getOffers().size() > previousCount, "New offers appended immediately");
            helper.assertTrue(menu.offersRevision() > previousRevision, "Client snapshot revision advanced");
            helper.assertTrue(menu.getCarried().is(Items.BREAD), "Original purchase completed once");
            helper.assertValueEqual(menu.selectedOfferIndex(), 0, "Original selection preserved");
            helper.assertTrue(sent.stream().anyMatch(packet ->
                    packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellMenuSyncPayload state
                    && state.containerId() == menu.containerId && state.villagerLevel() == 2
                    && state.offers().size() > previousCount && state.offersRevision() > previousRevision),
                    "Promoted offers sent to the client while its menu is open");
        } finally {
            player.closeContainer();
            villager.discard();
        }
        helper.succeed();
    }
}
