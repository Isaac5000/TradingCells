package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

public final class LogisticsRuleGameTests {
    private LogisticsRuleGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("logistics_advanced_rule_roundtrip", 20, LogisticsRuleGameTests::roundtrip),
                new GameTestCase("logistics_bounded_channel_search", 100, LogisticsRuleGameTests::channels),
                new GameTestCase("logistics_target_and_channel_replacement", 200, LogisticsRuleGameTests::targetRoute),
                new GameTestCase("logistics_target_marker_inventory_only", 20, LogisticsRuleGameTests::marker));
    }

    private static void channels(GameTestHelper helper) {
        var origin = line(helper, PipeKind.UNIVERSAL);
        for (int node = 0; node < 3; node++) {
            var pipe = helper.getBlockEntity(ORIGIN.east(node), LogisticsPipeBlockEntity.class);
            for (Direction side : List.of(Direction.NORTH, Direction.SOUTH)) {
                pipe.setUpgradeTier(side, PipeUpgradeTier.ULTIMATE);
                var settings = pipe.face(side).copy();
                settings.setMode(PipeSideMode.EXTRACT, true);
                var rules = new java.util.ArrayList<PipeFilterRule>();
                for (int index = 0; index < 16; index++) {
                    rules.add(new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID, "minecraft:diamond",
                            PipeFilterRule.ComponentMatch.IGNORE, "", "ores/" + node + side.getSerializedName() + String.format(java.util.Locale.ROOT, "%02d", index)));
                }
                settings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "ores", rules));
                pipe.applyFaceConfiguration(side, settings);
            }
        }
        var manager = network(helper);
        var search = manager.searchChannels(origin.getBlockPos(), LogisticsResourceType.ITEM, "ORES/");
        helper.startSequence().thenWaitUntil(() -> {
            manager.advanceChannelSearch(search);
            helper.assertTrue(search.results().size() <= 16, "At most sixteen channel strings retained");
            helper.assertTrue(search.complete(), "Incremental search completes");
        }).thenExecute(() -> {
            helper.assertValueEqual(search.results().size(), 16, "Returns a bounded window, not the entire catalogue");
            helper.assertValueEqual(search.results(), search.results().stream().sorted().toList(), "Results sorted");
            helper.assertTrue(search.results().stream().allMatch(value -> value.startsWith("ores/0north")), "Earliest matches independent of traversal order");
            manager.closeChannelSearch(search);
            manager.advanceChannelSearch(search);
            helper.assertTrue(search.results().isEmpty(), "Closed query never resumes");
        }).thenSucceed();
    }

    private static void roundtrip(GameTestHelper helper) {
        var target = new PipeRuleTarget("minecraft:overworld", -200, 64, 300);
        var rule = new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID, "minecraft:diamond",
                PipeFilterRule.ComponentMatch.SUBSET, "{\"minecraft:custom_data\":{quality:1}}", "ores/rare", true, target);
        var profile = new PipeResourceProfile(true, "ores", List.of(rule), PipeResourceProfile.FilterMode.BLACKLIST, PipeRoutingMode.RANDOM);
        helper.assertValueEqual(profile.withChannel("new").filters().getFirst().routeChannel(), "new/rare", "Rule inherits changed parent");
        helper.assertValueEqual(profile.withChannel("").filters().getFirst().routeChannel(), "rare", "Removing parent preserves suffix");
        boolean rejectedParent = false;
        try { profile.withChannel("x".repeat(256)); } catch (IllegalArgumentException expected) { rejectedParent = true; }
        helper.assertTrue(rejectedParent, "Parent cannot silently truncate a child's destination channel");
        var face = new PipeFaceConfiguration();
        face.setUpgradeTier(PipeUpgradeTier.ULTIMATE);
        face.setProfile(LogisticsResourceType.ITEM, profile);
        helper.assertTrue(profile.allows(candidate -> true), "Inverted blacklist permits the matched rule");
        var inverse = new PipeResourceProfile(true, "", List.of(rule), PipeResourceProfile.FilterMode.WHITELIST);
        helper.assertFalse(inverse.allows(candidate -> true), "Inverted whitelist denies the matched rule");
        helper.assertTrue(PipeFaceConfiguration.isValidNetworkConfiguration(face.save()), "Advanced NBT is valid");
        helper.assertValueEqual(PipeFaceConfiguration.load(face.save()).save(), face.save(), "Advanced NBT roundtrip exact");
        helper.assertValueEqual(PipeConfigurationClipboard.paste(PipeConfigurationClipboard.copy(face), face)
                .profile(LogisticsResourceType.ITEM), profile, "Clipboard preserves target, routing, inversion and components");
        var pipe = pipe(helper, ORIGIN, PipeKind.ITEM);
        pipe.setUpgradeTier(Direction.WEST, PipeUpgradeTier.ULTIMATE);
        pipe.applyFaceConfiguration(Direction.WEST, face);
        var removed = pipe.upgrades(Direction.WEST).removeItem(0, 1);
        helper.assertValueEqual(PipeUpgradeItem.storedProfiles(removed), face.saveActiveProfiles(), "Upgrade owns advanced rules");
        helper.assertValueEqual(PipeFilterRule.subchannel("ORES", "/Rare//Gems/"), "ores/rare/gems", "Normalized hierarchy");
        for (String parent : List.of("x".repeat(255), "a/".repeat(15) + "a")) {
            boolean rejectedChild = false;
            try { PipeFilterRule.subchannel(parent, "abc"); } catch (IllegalArgumentException expected) { rejectedChild = true; }
            helper.assertTrue(rejectedChild, "Over-budget subchannel rejected without changing its destination");
        }
        helper.assertValueEqual(PipeFilterRule.subchannel("x".repeat(254), "y").length(), 256, "Exact length limit accepted");
        helper.assertValueEqual(PipeFilterRule.validatedChannel("a/".repeat(15) + "a").split("/").length, 16, "Exact depth limit accepted");
        helper.assertValueEqual(PipeFilterRule.validatedChannel("a/".repeat(16)).split("/").length, 16, "Trailing separator does not add a segment");
        helper.assertTrue(PipeFilterRule.normalizeChannel("a/".repeat(100)).split("/").length <= 16, "Depth bounded");
        helper.assertTrue(PipeFilterRule.normalizeChannel("\u0130".repeat(256)).length() <= 256, "Case expansion bounded");
        helper.succeed();
    }

    private static void targetRoute(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.DIAMOND, 8);
        var near = barrel(helper, ORIGIN.above(), Items.DIAMOND, 0);
        barrel(helper, TARGET, Items.DIAMOND, 0);
        var pipe = line(helper, PipeKind.ITEM);
        var end = helper.getBlockEntity(ORIGIN.east(2), LogisticsPipeBlockEntity.class);
        pipe.setUpgradeTier(Direction.WEST, PipeUpgradeTier.ULTIMATE);
        end.setUpgradeTier(Direction.EAST, PipeUpgradeTier.ADVANCED);
        var targetPos = helper.absolutePos(TARGET);
        var target = new PipeRuleTarget(helper.getLevel().dimension().identifier().toString(), targetPos.getX(), targetPos.getY(), targetPos.getZ());
        var settings = pipe.face(Direction.WEST).copy();
        settings.setMode(PipeSideMode.EXTRACT, true);
        settings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "ores", List.of(
                new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID, "minecraft:diamond",
                        PipeFilterRule.ComponentMatch.IGNORE, "", "ores/rare", false, target)),
                PipeResourceProfile.FilterMode.WHITELIST, PipeRoutingMode.EQUAL));
        pipe.applyFaceConfiguration(Direction.WEST, settings);
        var destination = end.face(Direction.EAST).copy();
        destination.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "other", List.of()));
        end.applyFaceConfiguration(Direction.EAST, destination);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(pipe.getBlockPos(), LogisticsResourceType.ITEM), "Initial routes ready"))
                .thenIdle(15).thenExecute(() -> {
                    helper.assertValueEqual(count(source, Items.DIAMOND), 8, "Matching target cannot override wrong channel");
                    helper.assertValueEqual(count(near, Items.DIAMOND), 0, "Wildcard channel cannot override wrong target");
                    helper.setBlock(TARGET, Blocks.AIR);
                    var correct = end.face(Direction.EAST).copy();
                    correct.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "ores/rare", List.of()));
                    end.applyFaceConfiguration(Direction.EAST, correct);
                }).thenIdle(10).thenExecute(() -> {
                    helper.assertValueEqual(count(source, Items.DIAMOND), 8, "Missing target retains resources");
                    barrel(helper, TARGET, Items.DIAMOND, 0);
                    end.refreshConnectionsAround();
                }).thenWaitUntil(() -> helper.assertValueEqual(count(source, Items.DIAMOND), 0, "Replacement at same coordinates receives resources"))
                .thenExecute(() -> {
                    helper.assertValueEqual(count(helper.getBlockEntity(TARGET, net.minecraft.world.level.block.entity.BarrelBlockEntity.class), Items.DIAMOND), 8, "Exact conservation");
                    helper.assertValueEqual(pipe.face(Direction.WEST).profile(LogisticsResourceType.ITEM).filters().getFirst().target(), target, "Coordinates never erased");
                }).thenSucceed();
    }

    private static void marker(GameTestHelper helper) {
        barrel(helper, SOURCE, Items.DIAMOND, 3);
        var player = connectedPlayer(helper, GameType.SURVIVAL);
        var stack = LogisticsRegistrationAdapter.TARGET_SELECTOR_ITEM.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var pos = helper.absolutePos(SOURCE);
        player.setPos(Vec3.atCenterOf(pos));
        var item = (PipeTargetSelectorItem) stack.getItem();
        item.onItemUseFirst(stack, new UseOnContext(player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false)));
        var recorded = PipeTargetSelectorItem.target(stack);
        helper.assertTrue(recorded != null && recorded.x() == pos.getX() && recorded.y() == pos.getY() && recorded.z() == pos.getZ(), "Records actual interaction hand and coordinates");
        helper.assertTrue(player.getMainHandItem().is(Items.STICK), "Other hand unchanged");
        helper.setBlock(SOURCE, Blocks.STONE);
        item.onItemUseFirst(stack, new UseOnContext(player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false)));
        helper.assertValueEqual(PipeTargetSelectorItem.target(stack), recorded, "Incompatible block leaves previous coordinates intact");
        helper.assertValueEqual(stack.getCount(), 1, "Marker never consumed");
        helper.succeed();
    }
}
