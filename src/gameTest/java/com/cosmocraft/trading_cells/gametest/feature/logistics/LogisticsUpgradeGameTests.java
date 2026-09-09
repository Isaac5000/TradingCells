package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.network.PipeConfigurationPayload;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

public final class LogisticsUpgradeGameTests {
    private LogisticsUpgradeGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("logistics_upgrade_and_filters_single_confirmation", 20, LogisticsUpgradeGameTests::confirmation),
                new GameTestCase("logistics_legacy_upgrades_migrate_without_loss", 20, LogisticsUpgradeGameTests::migration),
                new GameTestCase("logistics_features_follow_installed_material", 20, LogisticsUpgradeGameTests::features),
                new GameTestCase("logistics_upgrade_swap_restores_owned_profile", 20, LogisticsUpgradeGameTests::swap),
                new GameTestCase("logistics_upgrade_server_permissions", 20, LogisticsUpgradeGameTests::permissions));
    }

    private static void swap(GameTestHelper helper) {
        var pipe = pipe(helper, ORIGIN, PipeKind.ITEM);
        pipe.setUpgradeTier(Direction.NORTH, PipeUpgradeTier.ULTIMATE);
        var settings = pipe.face(Direction.NORTH).copy();
        settings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "old", List.of()));
        pipe.applyFaceConfiguration(Direction.NORTH, settings);
        var owner = player(helper, pipe);
        var menu = new PipeConfigurationMenu(15, owner.getInventory(), pipe, Direction.NORTH, InteractionHand.OFF_HAND);
        owner.containerMenu = menu;
        menu.broadcastChanges();
        long generation = menu.upgradeGeneration();
        var replacement = settings.copy();
        replacement.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "new", List.of(), PipeResourceProfile.FilterMode.OFF, PipeRoutingMode.FARTHEST));
        menu.setCarried(PipeUpgradeItem.withProfiles(upgrade(PipeUpgradeTier.ULTIMATE), replacement.saveActiveProfiles()));
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, owner);
        menu.broadcastChanges();
        helper.assertValueEqual(pipe.face(Direction.NORTH).profile(LogisticsResourceType.ITEM).channel(), "new", "Same-tier swap loads incoming profile");
        helper.assertValueEqual(PipeUpgradeItem.storedProfiles(menu.getCarried()), settings.saveActiveProfiles(), "Cursor receives outgoing profile exactly");
        helper.assertTrue(menu.upgradeGeneration() > generation, "Client can distinguish physical replacement from normal save");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, owner);
        helper.assertValueEqual(pipe.face(Direction.NORTH).upgradeTier(), PipeUpgradeTier.BARE, "Removing upgrade restores bare tier");
        PipeConfigurationPayload.handleOnServer(payload(pipe, replacement, InteractionHand.OFF_HAND), owner);
        helper.assertFalse(pipe.face(Direction.NORTH).profile(LogisticsResourceType.ITEM).channel().equals("new"), "Late upgraded draft cannot overwrite bare profile");
        helper.succeed();
    }

    private static void confirmation(GameTestHelper helper) {
        var pipe = pipe(helper, ORIGIN, PipeKind.UNIVERSAL);
        var player = player(helper, pipe);
        var menu = new PipeConfigurationMenu(12, player.getInventory(), pipe, Direction.NORTH, InteractionHand.OFF_HAND);
        player.containerMenu = menu;
        player.getInventory().setItem(9, upgrade(PipeUpgradeTier.ULTIMATE));
        helper.assertFalse(menu.quickMoveStack(player, 1).isEmpty(), "Real shift-click inserts speed upgrade");
        helper.assertTrue(menu.getSlot(0).hasItem(), "Single upgrade slot populated");
        helper.assertValueEqual(menu.slots.size(), 37, "Exactly one upgrade slot and player inventory");
        player.getInventory().setItem(10, upgrade(PipeUpgradeTier.BASIC));
        helper.assertTrue(menu.quickMoveStack(player, 2).isEmpty(), "A second upgrade cannot be inserted");
        long rate = PipeUpgradeTier.ULTIMATE.rate(LogisticsResourceType.ENERGY);
        helper.assertValueEqual(pipe.transferRate(Direction.NORTH, LogisticsResourceType.ENERGY), rate, "Only installed tier determines speed");
        var requested = pipe.face(Direction.NORTH).copy();
        requested.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "edited", List.of(),
                PipeResourceProfile.FilterMode.WHITELIST));
        requested.setProfile(LogisticsResourceType.GAS, new PipeResourceProfile(false, "gas", List.of()));
        requested.setMode(PipeSideMode.EXTRACT, true);
        requested.setPriority(Integer.MAX_VALUE);
        PipeConfigurationPayload.handleOnServer(payload(pipe, requested, InteractionHand.OFF_HAND), player);
        helper.assertValueEqual(pipe.face(Direction.NORTH).saveActiveProfiles(), requested.saveActiveProfiles(), "Profile saved independently of slots");
        var data = pipe.saveWithFullMetadata(helper.getLevel().registryAccess());
        pipe.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        helper.assertValueEqual(pipe.installedUpgradeDrops().size(), 1, "Single installed upgrade persists and drops separately");
        helper.assertValueEqual(pipe.transferRate(Direction.NORTH, LogisticsResourceType.ENERGY), rate, "Persisted speed exact");
        var removed = pipe.upgrades(Direction.NORTH).removeItem(0, 1);
        helper.assertValueEqual(PipeUpgradeItem.storedProfiles(removed), requested.saveActiveProfiles(), "Removed upgrade retains profile");
        pipe.upgrades(Direction.NORTH).setItem(0, upgrade(PipeUpgradeTier.INFINITE));
        helper.assertValueEqual(pipe.face(Direction.NORTH).upgradeTier(), PipeUpgradeTier.INFINITE, "Fifth material works in survival");
        helper.succeed();
    }

    private static void migration(GameTestHelper helper) {
        var pipe = pipe(helper, ORIGIN, PipeKind.ITEM);
        var data = pipe.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.putInt("UpgradeSlotsVersion", 1);
        for (int index = 0; index < 4; index++) {
            pipe.setUpgradeTier(Direction.NORTH, PipeUpgradeTier.fromId(index + 1));
            var encoded = pipe.saveWithFullMetadata(helper.getLevel().registryAccess());
            data.put("Upgradenorth" + index, encoded.get("Upgradenorth0").copy());
        }
        pipe.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        helper.assertValueEqual(pipe.face(Direction.NORTH).upgradeTier(), PipeUpgradeTier.ULTIMATE, "Highest old tier remains installed");
        helper.assertValueEqual(pipe.installedUpgradeDrops().size(), 4, "Legacy upgrades are never discarded");
        var saved = pipe.saveWithFullMetadata(helper.getLevel().registryAccess());
        pipe.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertValueEqual(pipe.installedUpgradeDrops().size(), 4, "Pending returns survive reload");
        var owner = player(helper, pipe);
        new PipeConfigurationMenu(12, owner.getInventory(), pipe, Direction.NORTH, InteractionHand.OFF_HAND);
        helper.assertValueEqual(pipe.installedUpgradeDrops().size(), 1, "Opening returns only old surplus upgrades");
        for (int id = 1; id <= 3; id++) {
            helper.assertValueEqual(owner.getInventory().countItem(upgrade(PipeUpgradeTier.fromId(id)).getItem()), 1,
                    "Every old upgrade returned once");
        }
        pipe.reclaimLegacyUpgrades(Direction.NORTH, owner);
        helper.assertValueEqual(owner.getInventory().countItem(upgrade(PipeUpgradeTier.BASIC).getItem()), 1, "No duplicate return");
        helper.succeed();
    }

    private static void features(GameTestHelper helper) {
        var settings = new PipeFaceConfiguration();
        for (LogisticsResourceType type : LogisticsResourceType.values()) {
            helper.assertValueEqual(settings.effectiveProfile(type).routingMode(),
                    type == LogisticsResourceType.ITEM ? PipeRoutingMode.NEAREST : PipeRoutingMode.EQUAL,
                    "Items prefer nearest; liquids, gases and energy share by default");
        }
        var rule = new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID,
                "minecraft:diamond", PipeFilterRule.ComponentMatch.EXACT, "{test:1}", "reserved");
        var profile = new PipeResourceProfile(true, "main", List.of(rule), PipeResourceProfile.FilterMode.WHITELIST,
                PipeRoutingMode.FARTHEST);
        settings.setProfile(LogisticsResourceType.ITEM, profile);
        for (PipeUpgradeTier tier : PipeUpgradeTier.values()) {
            settings.setUpgradeTier(tier);
            var effective = settings.effectiveProfile(LogisticsResourceType.ITEM);
            helper.assertValueEqual(effective.routingMode(), tier.allowsRouting() ? PipeRoutingMode.FARTHEST : PipeRoutingMode.NEAREST,
                    "Copper enables routing modes");
            helper.assertValueEqual(effective.filters().size(), tier.allowsFilters() ? 1 : 0, "Iron enables filters");
            helper.assertValueEqual(effective.channel(), tier.allowsChannels() ? "main" : "", "Gold enables channel");
            if (tier.allowsFilters()) {
                helper.assertValueEqual(effective.filters().getFirst().componentMatch(), tier.allowsAdvancedRules()
                        ? PipeFilterRule.ComponentMatch.EXACT : PipeFilterRule.ComponentMatch.IGNORE, "Diamond enables NBT");
                helper.assertValueEqual(effective.filters().getFirst().routeChannel(), tier.allowsAdvancedRules() ? "reserved" : "",
                        "Diamond enables per-rule channels");
            }
            helper.assertValueEqual(settings.profile(LogisticsResourceType.ITEM), profile, "Inactive settings retained without granting their effect");
            helper.assertTrue(effective == settings.effectiveProfile(LogisticsResourceType.ITEM), "No per-tick profile allocation");
        }
        helper.assertTrue(PipeUpgradeTier.INFINITE.rate(LogisticsResourceType.ITEM) >= PipeUpgradeTier.ULTIMATE.rate(LogisticsResourceType.ITEM) * 8,
                "Netherite significantly raises throughput");
        helper.succeed();
    }

    private static void permissions(GameTestHelper helper) {
        var pipe = pipe(helper, ORIGIN, PipeKind.ITEM);
        var player = player(helper, pipe);
        player.getInventory().setItem(0, upgrade(PipeUpgradeTier.BASIC));
        var original = pipe.face(Direction.NORTH).save();
        var requested = pipe.face(Direction.NORTH).withReplacementUpgrade(PipeUpgradeTier.BASIC, null);
        requested.setPriority(Integer.MIN_VALUE);
        PipeConfigurationPayload.handleOnServer(payload(pipe, requested, InteractionHand.MAIN_HAND), player);
        helper.assertValueEqual(pipe.face(Direction.NORTH).save(), original, "Wrench must be in interaction hand");
        var stale = payload(pipe, requested, InteractionHand.OFF_HAND);
        pipe.setMode(Direction.SOUTH, PipeSideMode.NONE);
        PipeConfigurationPayload.handleOnServer(stale, player);
        helper.assertValueEqual(pipe.face(Direction.NORTH).save(), original, "Obsolete revision rejected");
        for (PipeUpgradeTier tier : List.of(PipeUpgradeTier.IMPROVED, PipeUpgradeTier.INFINITE)) {
            var missing = pipe.face(Direction.NORTH).withReplacementUpgrade(tier, null);
            PipeConfigurationPayload.handleOnServer(payload(pipe, missing, InteractionHand.OFF_HAND), player);
            helper.assertValueEqual(pipe.face(Direction.NORTH).save(), original, "Upgrade tier without an installed item rejected");
        }
        var malformed = requested.save();
        malformed.putInt("SchemaVersion", 99);
        PipeConfigurationPayload.handleOnServer(new PipeConfigurationPayload(pipe.getBlockPos(), Direction.NORTH,
                InteractionHand.OFF_HAND, pipe.revision(), malformed), player);
        helper.assertValueEqual(pipe.face(Direction.NORTH).save(), original, "Malformed profile rejected");
        player.setPos(Vec3.atCenterOf(pipe.getBlockPos().east(9)));
        PipeConfigurationPayload.handleOnServer(payload(pipe, requested, InteractionHand.OFF_HAND), player);
        helper.assertValueEqual(pipe.face(Direction.NORTH).save(), original, "Out-of-reach request rejected");
        player.setPos(Vec3.atCenterOf(pipe.getBlockPos()));
        player.setGameMode(GameType.SPECTATOR);
        PipeConfigurationPayload.handleOnServer(payload(pipe, requested, InteractionHand.OFF_HAND), player);
        helper.assertValueEqual(pipe.face(Direction.NORTH).save(), original, "Spectator cannot configure");
        helper.assertValueEqual(player.getInventory().countItem(upgrade(PipeUpgradeTier.BASIC).getItem()), 1, "Rejected requests do not consume upgrade");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, LogisticsPipeBlockEntity pipe) {
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pipe.getBlockPos()));
        player.setItemInHand(InteractionHand.OFF_HAND, LogisticsRegistrationAdapter.WRENCH_ITEM.get().getDefaultInstance());
        return player;
    }

    private static PipeConfigurationPayload payload(LogisticsPipeBlockEntity pipe, PipeFaceConfiguration requested, InteractionHand hand) {
        return new PipeConfigurationPayload(pipe.getBlockPos(), Direction.NORTH, hand, pipe.revision(), requested.save());
    }

    private static ItemStack upgrade(PipeUpgradeTier tier) {
        return LogisticsRegistrationAdapter.upgradeItem(tier).get().getDefaultInstance();
    }

    private static ItemStack findUpgrade(ServerPlayer player, PipeUpgradeTier tier) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof PipeUpgradeItem item && item.tier() == tier) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
