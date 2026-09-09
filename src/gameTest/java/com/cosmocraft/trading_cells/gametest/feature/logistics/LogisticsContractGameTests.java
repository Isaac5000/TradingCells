package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import static com.cosmocraft.trading_cells.gametest.feature.logistics.LogisticsTestFixtures.*;

public final class LogisticsContractGameTests {
    private LogisticsContractGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("logistics_profiles_persistence_and_upgrades", 20, LogisticsContractGameTests::profiles),
                new GameTestCase("logistics_reject_invalid_configuration", 20, LogisticsContractGameTests::invalidConfiguration),
                new GameTestCase("logistics_structured_component_filters", 20, LogisticsContractGameTests::components),
                new GameTestCase("logistics_external_failure_rollback", 20, LogisticsContractGameTests::rollback),
                new GameTestCase("logistics_fluid_gas_separation", 20, LogisticsContractGameTests::gases),
                new GameTestCase("logistics_energy_long_bounds", 20, LogisticsContractGameTests::energy),
                new GameTestCase("logistics_crafting_atomic_batch", 60, LogisticsContractGameTests::crafting),
                new GameTestCase("logistics_crafting_remainders_atomic", 60, LogisticsContractGameTests::remainders));
    }

    private static void profiles(GameTestHelper helper) {
        var pipe = pipe(helper, ORIGIN, PipeKind.UNIVERSAL);
        var bare = pipe.face(Direction.NORTH).copy();
        bare.setPriority(Integer.MIN_VALUE);
        bare.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "base", List.of()));
        pipe.applyFaceConfiguration(Direction.NORTH, bare);
        pipe.replaceUpgrade(Direction.NORTH, PipeUpgradeTier.BASIC,
                LogisticsRegistrationAdapter.upgradeItem(PipeUpgradeTier.BASIC).get().getDefaultInstance());
        var upgraded = pipe.face(Direction.NORTH).copy();
        upgraded.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "upgrade", List.of()));
        pipe.applyFaceConfiguration(Direction.NORTH, upgraded);
        var saved = pipe.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = (LogisticsPipeBlockEntity) BlockEntity.loadStatic(pipe.getBlockPos(), pipe.getBlockState(),
                saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded != null, "Pipe reload");
        helper.assertValueEqual(loaded.face(Direction.NORTH).save(), pipe.face(Direction.NORTH).save(), "Exact persisted face");
        ItemStack returned = pipe.replaceUpgrade(Direction.NORTH, PipeUpgradeTier.BARE, ItemStack.EMPTY);
        helper.assertValueEqual(pipe.face(Direction.NORTH).profile(LogisticsResourceType.ITEM).channel(), "base", "Bare profile restored");
        pipe.replaceUpgrade(Direction.NORTH, PipeUpgradeTier.BASIC, returned);
        helper.assertValueEqual(pipe.face(Direction.NORTH).profile(LogisticsResourceType.ITEM).channel(), "upgrade", "Upgrade retains own profile");
        helper.succeed();
    }

    private static void invalidConfiguration(GameTestHelper helper) {
        var configuration = new PipeFaceConfiguration();
        configuration.setPriority(Integer.MAX_VALUE);
        var valid = configuration.save();
        helper.assertTrue(PipeFaceConfiguration.isValidNetworkConfiguration(valid), "Maximum signed priority accepted");
        for (String key : List.of("SchemaVersion", "UpgradeTier", "Priority")) {
            var invalid = valid.copy();
            invalid.putLong(key, Long.MAX_VALUE);
            helper.assertFalse(PipeFaceConfiguration.isValidNetworkConfiguration(invalid), "Reject overflow: " + key);
        }
        var invalid = valid.copy();
        invalid.putString("Mode", "unknown");
        helper.assertFalse(PipeFaceConfiguration.isValidNetworkConfiguration(invalid), "Unknown mode rejected");
        var rule = valid.copy();
        var profiles = rule.getCompound("BareProfiles").orElseThrow();
        profiles.getCompound("Profileitem").orElseThrow().putInt("FilterCount", 17);
        helper.assertFalse(PipeFaceConfiguration.isValidNetworkConfiguration(rule), "Excessive filters rejected");
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        var stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.setDamageValue(10);
        CompoundTag custom = new CompoundTag();
        custom.putString("name", "alpha-beta");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        var resource = ItemResource.of(stack);
        String fingerprint = LogisticsComponentData.fingerprint(resource.getComponentsPatch());
        var adapter = new ItemLogisticsAdapter();
        var subset = new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID,
                "minecraft:diamond_sword", PipeFilterRule.ComponentMatch.SUBSET, "{\"minecraft:damage\":10}", "");
        helper.assertTrue(adapter.matches(resource, fingerprint, subset), "Subset matches a complete component value");
        var wrong = new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID,
                "minecraft:diamond_sword", PipeFilterRule.ComponentMatch.SUBSET, "{\"minecraft:damage\":1}", "");
        helper.assertFalse(adapter.matches(resource, fingerprint, wrong), "Substring is not component equality");
        helper.succeed();
    }

    private static void rollback(GameTestHelper helper) {
        var source = new ItemStacksResourceHandler(1);
        var destination = new ItemStacksResourceHandler(1) {
            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                super.insert(index, resource, amount, transaction);
                throw new IllegalStateException("Deliberately broken external provider");
            }
        };
        ItemResource diamond = ItemResource.of(Items.DIAMOND);
        try (var transaction = Transaction.openRoot()) {
            source.insert(diamond, 32, transaction);
            transaction.commit();
        }
        helper.assertValueEqual(new ItemLogisticsAdapter().transfer(source, destination, diamond, 12), 0L, "Broken operation rejected");
        helper.assertValueEqual(source.getAmountAsLong(0), 32L, "Source rolled back");
        helper.assertValueEqual(destination.getAmountAsLong(0), 0L, "Destination rolled back");
        helper.succeed();
    }

    private static void gases(GameTestHelper helper) {
        var water = FluidResource.of(Fluids.WATER);
        var taggedGas = FluidResource.of(Fluids.LAVA);
        helper.assertFalse(FluidGasClassifier.isGas(water), "not_gases overrides gases inclusion");
        helper.assertTrue(FluidGasClassifier.isGas(taggedGas), "Datapack explicitly classifies gas");
        var source = new FluidStacksResourceHandler(2, 1000);
        var destination = new FluidStacksResourceHandler(2, 1000);
        try (var transaction = Transaction.openRoot()) {
            source.insert(water, 400, transaction);
            source.insert(taggedGas, 400, transaction);
            transaction.commit();
        }
        var fluid = new FluidLogisticsAdapter(LogisticsResourceType.FLUID);
        var gas = new FluidLogisticsAdapter(LogisticsResourceType.GAS);
        helper.assertValueEqual(fluid.transfer(source, destination, taggedGas, 100), 0L, "Fluid route rejects gases");
        helper.assertValueEqual(gas.transfer(source, destination, water, 100), 0L, "Gas route rejects liquids");
        helper.assertValueEqual(gas.transfer(source, destination, taggedGas, 100), 100L, "Gas transfer");
        helper.assertValueEqual(fluid.transfer(source, destination, water, 100), 100L, "Fluid transfer");
        helper.succeed();
    }

    private static void energy(GameTestHelper helper) {
        var source = new SimpleEnergyHandler(Integer.MAX_VALUE);
        var target = new SimpleEnergyHandler(Integer.MAX_VALUE);
        source.set(Integer.MAX_VALUE);
        var adapter = new EnergyLogisticsAdapter();
        var energy = adapter.contents(source).getFirst().resource();
        helper.assertValueEqual(adapter.transfer(source, target, energy, Long.MAX_VALUE), (long) Integer.MAX_VALUE, "Long request bounded by capacity API");
        helper.assertValueEqual(source.getAmountAsLong(), 0L, "No negative source");
        helper.assertValueEqual(target.getAmountAsLong(), (long) Integer.MAX_VALUE, "No overflow");
        helper.succeed();
    }

    private static void crafting(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.OAK_LOG, 1);
        var origin = pipe(helper, ORIGIN, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        var player = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var grid = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(0, new ItemStack(Items.OAK_LOG));
        network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM), "Crafting routes ready")).thenExecute(() -> {
            helper.assertFalse(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 2), "Incomplete batch cancelled");
            helper.assertValueEqual(count(source, Items.OAK_LOG), 1, "All ingredients restored");
            helper.assertValueEqual(player.getInventory().countItem(Items.OAK_PLANKS), 0, "No partial batch result");
            helper.assertTrue(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1), "Available recipe crafted");
            helper.assertValueEqual(count(source, Items.OAK_LOG), 0, "Ingredient consumed once");
            helper.assertValueEqual(player.getInventory().countItem(Items.OAK_PLANKS), 4, "Exact recipe result");
            helper.succeed();
        });
    }

    private static void remainders(GameTestHelper helper) {
        var source = barrel(helper, SOURCE, Items.MILK_BUCKET, 1);
        source.setItem(1, new ItemStack(Items.MILK_BUCKET));
        source.setItem(2, new ItemStack(Items.MILK_BUCKET));
        source.setItem(3, new ItemStack(Items.SUGAR, 2));
        source.setItem(4, new ItemStack(Items.EGG));
        source.setItem(5, new ItemStack(Items.WHEAT, 3));
        var origin = pipe(helper, ORIGIN, PipeKind.ITEM);
        origin.setMode(Direction.WEST, PipeSideMode.EXTRACT);
        var settings = origin.face(Direction.WEST).copy();
        origin.setUpgradeTier(Direction.WEST, PipeUpgradeTier.IMPROVED);
        settings.setProfile(LogisticsResourceType.ITEM, new PipeResourceProfile(true, "", List.of(
                new PipeFilterRule(PipeFilterRule.Action.DENY, PipeFilterRule.MatchKind.ID,
                        "minecraft:bucket", PipeFilterRule.ComponentMatch.IGNORE, "", ""))));
        origin.applyFaceConfiguration(Direction.WEST, settings);
        var player = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        var grid = List.of(new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.SUGAR), new ItemStack(Items.EGG),
                new ItemStack(Items.SUGAR), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT),
                new ItemStack(Items.WHEAT));
        network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                network(helper).topologyReady(origin.getBlockPos(), LogisticsResourceType.ITEM), "Remainder routes ready")).thenExecute(() -> {
            helper.assertFalse(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1),
                    "Full inventory cancels output and all remainders");
            helper.assertValueEqual(count(source, Items.MILK_BUCKET), 3, "Milk buckets restored on rollback");
            helper.assertValueEqual(count(source, Items.SUGAR), 2, "Other ingredients restored");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.getInventory().setItem(1, ItemStack.EMPTY);
            helper.assertTrue(NetworkCraftingService.craft(helper.getLevel(), origin.getBlockPos(), player, grid, 1),
                    "Remainders fall back to player when network filters reject buckets");
            helper.assertValueEqual(player.getInventory().countItem(Items.BUCKET), 3, "All empty buckets preserved");
            helper.assertValueEqual(player.getInventory().countItem(Items.CAKE), 1, "Recipe output created once");
            helper.assertValueEqual(count(source, Items.MILK_BUCKET), 0, "Inputs consumed exactly once");
            helper.succeed();
        });
    }
}
