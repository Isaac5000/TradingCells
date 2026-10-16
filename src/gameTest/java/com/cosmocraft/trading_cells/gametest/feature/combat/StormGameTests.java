package com.cosmocraft.trading_cells.gametest.feature.combat;

import com.cosmocraft.trading_cells.feature.combat.adapters.input.StormLightningEvents;
import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.*;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class StormGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private StormGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("storm_battery_capacities", 40, StormGameTests::capacities),
                new GameTestCase("storm_station_atomic_persistence", 40, StormGameTests::station),
                new GameTestCase("storm_inventory_baton_no_sky_lightning", 40, StormGameTests::baton),
                new GameTestCase("storm_welder_charge_per_second", 40, StormGameTests::welder),
                new GameTestCase("storm_reload_eject_and_recipe", 120, StormGameTests::reloadAndUpgrade),
                new GameTestCase("storm_reverse_unload_and_recipes", 40, StormGameTests::reverseUnload),
                new GameTestCase("storm_weapon_charge_durability_enchantments", 40, StormGameTests::chargeDurability),
                new GameTestCase("storm_welder_blocks_melee_and_mining", 40, StormGameTests::noMeleeOrMining),
                new GameTestCase("storm_welder_damage_conversion", 40, StormGameTests::damage),
                new GameTestCase("storm_lethal_hits_raw_loot", 40, StormGameTests::lethalHits),
                new GameTestCase("storm_surviving_transformations", 40, StormGameTests::survivingTransformations),
                new GameTestCase("storm_charged_explosion_drop", 80, StormGameTests::explosion));
    }
    private static ItemStack battery(int charge) {
        var stack = new ItemStack(CombatRegistrationAdapter.CAPACITOR.get());
        StormChargeData.setCharge(stack, charge);
        return stack;
    }

    private static void chargeDurability(GameTestHelper h) {
        var level = h.getLevel();
        var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var mending = enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);
        var unbreaking = enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
        for (var item : List.of(CombatRegistrationAdapter.BATON.get(), CombatRegistrationAdapter.WELDER.get())) {
            var weapon = item.getDefaultInstance();
            h.assertFalse(weapon.isBarVisible() || weapon.isDamaged(), "Unloaded weapon has no charge or repair demand");
            for (int charge : new int[] {0, 16, 128, 256}) {
                var capacitor = battery(charge);
                StormWeaponItem.setBattery(weapon, capacitor);
                h.assertValueEqual(capacitor.getBarColor(), weapon.getBarColor(), "Capacitor and weapon share vanilla charge colors");
                h.assertValueEqual(capacitor.getBarWidth(), weapon.getBarWidth(), "Capacitor and weapon share charge bar width");
            }
            StormWeaponItem.setBattery(weapon, ItemStack.EMPTY);
            h.assertTrue(weapon.supportsEnchantment(mending) && weapon.supportsEnchantment(unbreaking),
                    "Both battery enchantments support the weapon");
            var cell = battery(128);
            cell.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Named capacitor"));
            StormWeaponItem.setBattery(weapon, cell);
            h.assertTrue(weapon.isBarVisible(), "Installed battery shows a bar");
            h.assertValueEqual(weapon.getBarWidth(), 7, "Half charge occupies half the bar");
            h.assertValueEqual(weapon.getBarColor(), 0xFFFF00, "Half-charge bar is vanilla yellow");
            h.assertValueEqual(weapon.getDamageValue(), 128, "Durability view follows missing charge");
            var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            weapon.enchant(mending, 1);
            player.takeXpDelay = 0;
            new net.minecraft.world.entity.ExperienceOrb(level, player.position(), net.minecraft.world.phys.Vec3.ZERO, 5).playerTouch(player);
            h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), 138, "Vanilla Mending adds two charges per XP");
            h.assertValueEqual(player.totalExperience, 0, "Used repair XP is not awarded twice");
            h.assertValueEqual(StormWeaponItem.battery(weapon).getHoverName().getString(), "Named capacitor", "Mending preserves battery components");
            StormWeaponItem.setBattery(weapon, battery(254));
            player.takeXpDelay = 0;
            new net.minecraft.world.entity.ExperienceOrb(level, player.position(), net.minecraft.world.phys.Vec3.ZERO, 5).playerTouch(player);
            h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), 256, "Mending cannot overcharge");
            h.assertValueEqual(weapon.getBarColor(), 0x00FF00, "Full-charge bar is vanilla green");
            h.assertValueEqual(player.totalExperience, 4, "Unused XP reaches the player");
            StormWeaponItem.setBattery(weapon, ItemStack.EMPTY);
            player.takeXpDelay = 0;
            new net.minecraft.world.entity.ExperienceOrb(level, player.position(), net.minecraft.world.phys.Vec3.ZERO, 3).playerTouch(player);
            h.assertValueEqual(player.totalExperience, 7, "Unloaded weapon consumes no repair XP");
            h.assertTrue(StormWeaponItem.battery(weapon).isEmpty(), "Repair never creates a capacitor");
            StormWeaponItem.setBattery(weapon, battery(1));
            h.assertTrue(StormWeaponItem.consumeCharge(weapon, level), "Last charge pays once");
            h.assertFalse(StormWeaponItem.consumeCharge(weapon, level), "Empty battery cannot power an operation");
            h.assertValueEqual(weapon.getBarColor(), 0xFF0000, "Empty-charge bar is vanilla red");
            weapon.hurtAndBreak(10, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            h.assertFalse(weapon.isEmpty(), "Empty battery and vanilla sword wear never break weapon");
            player.takeXpDelay = 0;
            new net.minecraft.world.entity.ExperienceOrb(level, player.position(), net.minecraft.world.phys.Vec3.ZERO, 1).playerTouch(player);
            h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), 2, "Completely empty installed battery can be mended");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, weapon);
            player.takeXpDelay = 0;
            new net.minecraft.world.entity.ExperienceOrb(level, player.position(), net.minecraft.world.phys.Vec3.ZERO, 2).playerTouch(player);
            h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), 6, "Offhand Mending also repairs the installed capacitor");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            StormWeaponItem.setBattery(weapon, battery(256));
            weapon.enchant(unbreaking, 3);
            for (int i = 0; i < 128; i++) { h.assertTrue(StormWeaponItem.consumeCharge(weapon, level), "Unbreaking never cancels the operation"); }
            int remaining = StormChargeData.charge(StormWeaponItem.battery(weapon));
            h.assertTrue(remaining > 128 && remaining < 256, "Unbreaking III probabilistically saves charge");
            var encoded = ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), weapon).getOrThrow();
            var restored = ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), encoded).getOrThrow();
            h.assertValueEqual(restored.getDamageValue(), 256 - remaining, "Charge durability survives serialization");
            StormWeaponItem.ejectBattery(player, weapon);
            h.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> StormChargeData.charge(s) == remaining),
                    "Ejected capacitor retains its actual charge");
        }
        h.succeed();
    }

    private static void noMeleeOrMining(GameTestHelper h) {
        var weapon = CombatRegistrationAdapter.WELDER.get().getDefaultInstance();
        var target = h.spawn(EntityTypes.COW, POS);
        float health = target.getHealth();
        for (var mode : List.of(net.minecraft.world.level.GameType.SURVIVAL, net.minecraft.world.level.GameType.CREATIVE)) {
            var player = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "welder-test"));
            player.setGameMode(mode);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            for (int charge : List.of(0, 10)) {
                StormWeaponItem.setBattery(weapon, battery(charge));
                player.attack(target);
                h.assertValueEqual(target.getHealth(), health, "Welder cannot inflict melee damage");
                h.setBlock(POS, net.minecraft.world.level.block.Blocks.STONE);
                h.assertFalse(player.gameMode.destroyBlock(h.absolutePos(POS)), "Welder cannot break blocks in either game mode");
                h.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, POS);
                var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(
                        player, h.absolutePos(POS), Direction.UP,
                        net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
                h.assertTrue(event.isCanceled(), "Left-click mining never starts");
                h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), charge, "Blocked actions use no charge");
            }
        }
        h.succeed();
    }

    private static void reverseUnload(GameTestHelper h) {
        for (var item : List.of(CombatRegistrationAdapter.BATON.get(), CombatRegistrationAdapter.WELDER.get())) {
            for (var hand : InteractionHand.values()) {
                var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
                var weapon = item.getDefaultInstance();
                player.setItemInHand(hand, weapon);
                StormWeaponItem.setBattery(weapon, battery(17));
                StormWeaponItem.requestReload(player, hand, true);
                h.assertTrue(StormWeaponItem.unloading(weapon), "Both weapons and hands reverse their loading animation");
                var data = weapon.get(DataComponents.CUSTOM_DATA).copyTag();
                data.putLong("TradingCellsStormReloadStart", h.getLevel().getGameTime() - 15);
                weapon.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
                h.assertValueEqual(StormWeaponItem.animationProgress(weapon, h.getLevel(), 0), 0.5F, "Reverse animation passes through the same midpoint");
                data.putLong("TradingCellsStormReloadStart", h.getLevel().getGameTime() - 30);
                weapon.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
                h.assertValueEqual(StormWeaponItem.animationProgress(weapon, h.getLevel(), 0), 0F, "Reverse animation ends outside the socket");
                item.inventoryTick(weapon, h.getLevel(), player, hand == InteractionHand.MAIN_HAND
                        ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                h.assertFalse(StormWeaponItem.animating(weapon), "Visual completion clears transient state");
                h.assertTrue(StormWeaponItem.battery(weapon).isEmpty(), "Visual completion cannot auto-reload");
                h.assertValueEqual(player.getInventory().getNonEquipmentItems().stream()
                        .filter(stack -> StormChargeData.charge(stack) == 17).mapToInt(ItemStack::getCount).sum(), 1,
                        "Completing unload returns exactly one battery");
            }
        }
        var welderGrid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(
                Items.IRON_INGOT.getDefaultInstance(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        net.minecraft.resources.Identifier.withDefaultNamespace("copper_block")).getDefaultInstance(), Items.IRON_INGOT.getDefaultInstance(),
                Items.COPPER_INGOT.getDefaultInstance(), CombatRegistrationAdapter.STORM_SHARD_ITEM.get().getDefaultInstance(), Items.COPPER_INGOT.getDefaultInstance(),
                ItemStack.EMPTY, Items.IRON_INGOT.getDefaultInstance(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        net.minecraft.resources.Identifier.withDefaultNamespace("black_concrete")).getDefaultInstance()));
        var batonGrid = net.minecraft.world.item.crafting.CraftingInput.of(1, 3,
                List.of(Items.IRON_INGOT.getDefaultInstance(), Items.COPPER_INGOT.getDefaultInstance(), Items.STICK.getDefaultInstance()));
        for (var id : List.of("electric_arc_welder", "electric_baton")) {
            var recipe = (net.minecraft.world.item.crafting.ShapedRecipe) h.getLevel().recipeAccess().byKey(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", id))).orElseThrow().value();
            h.assertTrue(recipe.matches(id.equals("electric_baton") ? batonGrid : welderGrid, h.getLevel()),
                    "Updated copper and storm-shard recipe matches: " + id);
        }
        h.succeed();
    }
    private static void capacities(GameTestHelper helper) {
        helper.assertValueEqual(StormChargeData.charge(battery(Integer.MAX_VALUE)), 256, "Single battery clamps at 256");
        var advanced = CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance();
        StormChargeData.setCharge(advanced, Integer.MAX_VALUE);
        helper.assertValueEqual(StormChargeData.charge(advanced), 256, "Advanced battery clamps at 256");
        StormChargeData.setCharge(advanced, -1);
        helper.assertValueEqual(StormChargeData.charge(advanced), 0, "No negative charge");
        helper.assertValueEqual(StormChargeData.capacity(Items.IRON_INGOT.getDefaultInstance()), 0, "Other items are not batteries");
        helper.assertValueEqual(StormChargeData.charge(battery(4)), 4, "Old battery charge is retained");
        helper.succeed();
    }
    private static void ticks(StormStationBlockEntity station, int count) { for (int i = 0; i < count; i++) { station.processTick(); } }
    private static void station(GameTestHelper helper) {
        helper.setBlock(POS, CombatRegistrationAdapter.STATION.get());
        var station = helper.getBlockEntity(POS, StormStationBlockEntity.class);
        var input = helper.getLevel().getCapability(Capabilities.Item.BLOCK, station.getBlockPos(), Direction.UP);
        helper.assertTrue(input != null, "Station automation capability available");
        try (var tx = Transaction.openRoot()) { input.insert(ItemResource.of(battery(0)), 1, tx); }
        helper.assertTrue(station.isEmpty(), "Cancelled insertion rolls back");
        helper.assertFalse(station.canPlaceItem(0, Items.SAND.getDefaultInstance()), "Only shards accepted as fuel");
        station.setItem(0, new ItemStack(CombatRegistrationAdapter.STORM_SHARD_ITEM.get(), 10));
        station.setItem(1, battery(0));
        station.setItem(2, battery(4), true);
        ticks(station, 25);
        helper.assertValueEqual(StormChargeData.charge(station.getItem(1)), 16, "Shard gives 16 charges in place");
        helper.assertValueEqual(StormChargeData.charge(station.getItem(2)), 4, "Legacy output does not block charging");
        helper.assertTrue(station.getItem(3).isEmpty(), "No waste output is produced");
        ticks(station, 12);
        var saved = station.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = new StormStationBlockEntity(station.getBlockPos(), station.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        ticks(restored, 13);
        helper.assertValueEqual(StormChargeData.charge(restored.getItem(1)), 32, "Progress survives reload");
        restored.setItem(1, battery(255));
        ticks(restored, 25);
        helper.assertValueEqual(StormChargeData.charge(restored.getItem(1)), 256, "Last shard fills remaining one charge");
        helper.assertValueEqual(restored.getItem(0).getCount(), 7, "Whole shard spent even when excess is wasted");
        ticks(restored, 100);
        helper.assertValueEqual(restored.getItem(0).getCount(), 7, "Full battery stops fuel consumption");
        helper.assertTrue(restored.canTakeItemThroughFace(1, restored.getItem(1), Direction.DOWN), "Full battery can leave same slot");
        helper.assertTrue(restored.canTakeItemThroughFace(2, restored.getItem(2), Direction.UP)
                && !restored.canTakeItemThroughFace(1, battery(0), Direction.UP), "Only results are extracted automatically");
        var advanced = CombatRegistrationAdapter.CAPACITOR.get().getDefaultInstance();
        restored.setItem(1, advanced);
        restored.setItem(0, new ItemStack(CombatRegistrationAdapter.STORM_SHARD_ITEM.get(), 16));
        ticks(restored, 400);
        helper.assertValueEqual(StormChargeData.charge(restored.getItem(1)), 256, "Sixteen shards fill advanced battery");
        helper.assertTrue(restored.getItem(0).isEmpty(), "Exactly sixteen shards spent");
        helper.succeed();
    }
    private static void baton(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack = CombatRegistrationAdapter.BATON.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        double baseDamage = stack.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 1,
                net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        helper.assertValueEqual(baseDamage, 1D, "Unpowered baton deals one base damage");
        var empty = helper.spawn(EntityTypes.CREEPER, POS);
        helper.assertValueEqual(CombatRegistrationAdapter.BATON.get().getAttackDamageBonus(empty, 1,
                player.damageSources().playerAttack(player)), 0F, "No electrical damage without charge");
        CombatRegistrationAdapter.BATON.get().hurtEnemy(stack, empty, player);
        helper.assertFalse(empty.isPowered(), "No charged battery means ordinary hit only");
        var charged = battery(2);
        charged.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Preserved battery"));
        StormWeaponItem.setBattery(stack, charged);
        helper.assertValueEqual(CombatRegistrationAdapter.BATON.get().getAttackDamageBonus(empty, 1, player.damageSources().playerAttack(player)),
                15F, "Charged baton retains sixteen total damage through standard attack pipeline");
        CombatRegistrationAdapter.BATON.get().hurtEnemy(stack, empty, player);
        helper.assertTrue(empty.isPowered(), "Native lightning effect charges creeper");
        helper.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(stack)), 1, "Consumes loaded battery charge");
        helper.assertTrue(StormWeaponItem.battery(stack).has(DataComponents.CUSTOM_NAME), "Unrelated components preserved");
        helper.assertTrue(helper.getLevel().getEntities(EntityTypes.LIGHTNING_BOLT, empty.getBoundingBox().inflate(12), e -> true).isEmpty(),
                "No sky lightning entity spawned");
        helper.succeed();
    }
    private static void welder(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack = CombatRegistrationAdapter.WELDER.get().getDefaultInstance();
        var item = CombatRegistrationAdapter.WELDER.get();
        StormWeaponItem.setBattery(stack, battery(4));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 80; tick++) {
            item.onUseTick(helper.getLevel(), player, stack, 72_000 - tick);
            helper.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(stack)), 3 - tick / 20, "Exactly one charge per 20 ticks");
        }
        item.onUseTick(helper.getLevel(), player, stack, 71_920);
        helper.assertFalse(player.isUsingItem(), "Empty battery stops sustained discharge");
        helper.assertTrue(StormWeaponItem.battery(stack).is(CombatRegistrationAdapter.CAPACITOR.get()), "Empty physical battery retained inside weapon");
        StormWeaponItem.setBattery(stack, battery(2));
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        helper.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(stack)), 1, "Release before first tick still spends one");
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        helper.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(stack)), 0, "Repeated taps cannot fire free");
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(player.isUsingItem(), "Empty battery cannot start a beam");
        helper.succeed();
    }
    private static void reloadAndUpgrade(GameTestHelper h) {
        var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var item = CombatRegistrationAdapter.WELDER.get();
        var weapon = item.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.getInventory().setItem(9, battery(0));
        player.getInventory().setItem(10, battery(30));
        player.setItemInHand(InteractionHand.OFF_HAND, battery(60));
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, true);
        h.assertTrue(StormWeaponItem.battery(weapon).isEmpty(), "Cancelled reload never moves battery");
        h.assertValueEqual(StormChargeData.charge(player.getOffhandItem()), 60, "Offhand battery unchanged on cancel");
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        item.inventoryTick(weapon, h.getLevel(), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        h.assertTrue(StormWeaponItem.battery(weapon).isEmpty(), "No early reload commit");
        h.runAfterDelay(31, () -> {
        item.inventoryTick(weapon, h.getLevel(), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), 60, "Charged offhand has bow-style priority");
        h.assertTrue(player.getOffhandItem().isEmpty(), "Loaded battery removed exactly once");
        h.assertValueEqual(StormChargeData.charge(player.getInventory().getItem(10)), 30, "Spare remains untouched");
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        h.runAfterDelay(31, () -> {
        item.inventoryTick(weapon, h.getLevel(), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(weapon)), 30, "Partially used battery can be replaced");
        h.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> StormChargeData.charge(s) == 60), "Previous battery returned");
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, weapon).getOrThrow()).getOrThrow();
        h.assertValueEqual(StormChargeData.charge(StormWeaponItem.battery(restored)), 30, "Loaded battery persists in item data");
        var grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, Items.REDSTONE_BLOCK.getDefaultInstance(), ItemStack.EMPTY,
                Items.DIAMOND.getDefaultInstance(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_block")).getDefaultInstance(), Items.DIAMOND.getDefaultInstance(), ItemStack.EMPTY, Items.COPPER_INGOT.getDefaultInstance(), ItemStack.EMPTY));
        var recipe = h.getLevel().recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", "voltaic_capacitor"))).orElseThrow().value();
        var upgrade = (net.minecraft.world.item.crafting.ShapedRecipe) recipe;
        h.assertTrue(upgrade.matches(grid, h.getLevel()), "Battery uses central copper block");
        var output = upgrade.assemble(grid);
        h.assertTrue(output.is(CombatRegistrationAdapter.CAPACITOR.get()), "One battery ID");
        h.assertValueEqual(StormChargeData.capacity(output), 256, "Recipe creates 256 capacity");
        var oldId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("trading_cells", "advanced_voltaic_capacitor"));
        h.assertTrue(oldId == CombatRegistrationAdapter.CAPACITOR.get(), "Old advanced ID resolves to the retained item");
        // Empty tap ejects even without a replacement. Full inventories toss forwards once.
        for (int slot = 0; slot < 36; slot++) { player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64)); }
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(POS)));
        StormWeaponItem.setBattery(weapon, battery(0));
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        h.assertTrue(StormWeaponItem.battery(weapon).isEmpty(), "Empty capacitor ejects without charged spare");
        var drops = h.getLevel().getEntities(EntityTypes.ITEM, player.getBoundingBox().inflate(4),
                entity -> entity.getItem().is(CombatRegistrationAdapter.CAPACITOR.get()));
        h.assertValueEqual(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(), 1, "Full inventory drops capacitor once");
        StormWeaponItem.setBattery(weapon, battery(19));
        player.getInventory().setItem(15, ItemStack.EMPTY);
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, true);
        h.assertTrue(StormWeaponItem.battery(weapon).isEmpty(), "Hold ejects partial capacitor");
        h.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> StormChargeData.charge(s) == 19), "Eject preserves remaining charge");
        h.assertFalse(StormWeaponItem.reloading(weapon), "Ejection does not start another reload");
        h.assertTrue(StormWeaponItem.unloading(weapon) && StormWeaponItem.animating(weapon), "Ejection starts reverse visual animation");
        h.assertValueEqual(StormWeaponItem.animationProgress(weapon, h.getLevel(), 0), 1F, "Reverse animation starts at inserted endpoint");
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        h.assertTrue(StormWeaponItem.reloading(weapon), "New press reloads immediately after ejection");
        h.assertFalse(StormWeaponItem.unloading(weapon), "New press replaces reverse animation without delay");
        StormWeaponItem.cancelReload(weapon);
        StormWeaponItem.setBattery(weapon, battery(0));
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        h.assertTrue(StormWeaponItem.battery(weapon).isEmpty() && !StormWeaponItem.reloading(weapon),
                "Empty-battery press only ejects even with a charged spare available");
        StormWeaponItem.requestReload(player, InteractionHand.MAIN_HAND, false);
        h.assertTrue(StormWeaponItem.reloading(weapon), "Second press starts reload without cooldown");
        h.succeed();
        });
        });
    }
    private static void damage(GameTestHelper h) {
        var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var item = CombatRegistrationAdapter.WELDER.get();
        var weapon = item.getDefaultInstance();
        StormWeaponItem.setBattery(weapon, battery(8));
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        var cow = h.spawn(EntityTypes.COW, POS);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        player.setPos(cow.getX() - 1, cow.getY(), cow.getZ() - 1);
        var direction = cow.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        player.setYHeadRot(player.getYRot());
        player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
        h.assertTrue(ElectricArcWelderItem.trace(h.getLevel(), player, 1).target() == cow, "Test beam points at cow");
        item.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        item.onUseTick(h.getLevel(), player, weapon, 72_000);
        float first = cow.getHealth();
        for (int tick : List.of(5, 10, 15)) { item.onUseTick(h.getLevel(), player, weapon, 72_000 - tick); }
        h.assertValueEqual(cow.getHealth(), first - 6, "Three further pulses bypass vanilla hurt cooldown");
        cow.discard();
        var pig = h.spawn(EntityTypes.PIG, POS);
        var converted = StormLightningEvents.strike(h.getLevel(), pig, player);
        h.assertTrue(converted != pig && converted instanceof net.minecraft.world.entity.LivingEntity, "Conversion follows replacement entity");
        var living = (net.minecraft.world.entity.LivingEntity) converted;
        float before = living.getHealth();
        item.onUseTick(h.getLevel(), player, weapon, 71_980);
        h.assertTrue(living.getHealth() < before, "Converted creature takes beam damage");
        h.succeed();
    }
    private static void explosion(GameTestHelper helper) {
        var creeper = helper.spawn(EntityTypes.CREEPER, POS);
        creeper.setNoAi(true);
        creeper.setNoGravity(true);
        StormLightningEvents.strike(helper.getLevel(), creeper, helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        helper.assertTrue(creeper.isPowered(), "Explosion fixture uses a charged creeper");
        creeper.ignite();
        helper.runAfterDelay(38, () -> {
            var drops = helper.getLevel().getEntities(EntityTypes.ITEM, new net.minecraft.world.phys.AABB(helper.absolutePos(POS)).inflate(5),
                    entity -> entity.getItem().is(CombatRegistrationAdapter.STORM_SHARD_ITEM.get()));
            helper.assertValueEqual(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(), 1,
                    "Charged explosion leaves one surviving shard; alive=" + creeper.isAlive()
                            + "; removed=" + creeper.getRemovalReason() + "; position=" + creeper.position());
            helper.succeed();
        });
    }

    private static void aim(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.LivingEntity target) {
        player.setPos(target.getX() - 1, target.getY(), target.getZ() - 1);
        var direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        player.setYHeadRot(player.getYRot());
        player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
    }

    private static void lethalHits(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var baton = CombatRegistrationAdapter.BATON.get().getDefaultInstance();
        StormWeaponItem.setBattery(baton, battery(8));
        player.setItemInHand(InteractionHand.MAIN_HAND, baton);
        var pig = h.spawn(EntityTypes.PIG, POS);
        // Same order as Player.attack: accepted melee damage, then Item.hurtEnemy.
        pig.hurtServer(level, player.damageSources().playerAttack(player), 16);
        CombatRegistrationAdapter.BATON.get().hurtEnemy(baton, pig, player);
        h.assertFalse(pig.isAlive(), "Lethal baton hit kills the original pig");
        h.assertTrue(level.getEntities(EntityTypes.ZOMBIFIED_PIGLIN, pig.getBoundingBox().inflate(4), e -> true).isEmpty(),
                "Dead pig cannot also become a living zombified piglin");
        h.assertTrue(level.getEntities(EntityTypes.ITEM, pig.getBoundingBox().inflate(4), e -> e.getItem().is(Items.PORKCHOP))
                .stream().mapToInt(e -> e.getItem().getCount()).sum() > 0, "Original pig drops its ordinary loot");
        pig.discard();

        var welder = CombatRegistrationAdapter.WELDER.get();
        var weapon = welder.getDefaultInstance();
        StormWeaponItem.setBattery(weapon, battery(8));
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        var chicken = h.spawn(EntityTypes.CHICKEN, POS.above(2));
        aim(player, chicken);
        h.assertTrue(ElectricArcWelderItem.trace(level, player, 1).target() == chicken,
                "Test beam points at chicken: " + ElectricArcWelderItem.trace(level, player, 1));
        welder.use(level, player, InteractionHand.MAIN_HAND);
        welder.onUseTick(level, player, weapon, 72_000);
        h.assertValueEqual(chicken.getHealth(), 2F, "Electricity adds no hidden lightning damage");
        h.assertFalse(chicken.isOnFire(), "Electrical pulse does not cook the chicken");
        welder.onUseTick(level, player, weapon, 71_995);
        h.assertFalse(chicken.isAlive(), "Second pulse kills chicken");
        h.assertValueEqual(level.getEntities(EntityTypes.ITEM, chicken.getBoundingBox().inflate(4), e -> e.getItem().is(Items.CHICKEN))
                .stream().mapToInt(e -> e.getItem().getCount()).sum(), 1, "Welder leaves raw chicken");
        h.assertTrue(level.getEntities(EntityTypes.ITEM, chicken.getBoundingBox().inflate(4), e -> e.getItem().is(Items.COOKED_CHICKEN)).isEmpty(),
                "No cooked chicken from electrical fire");

        var weakPig = h.spawn(EntityTypes.PIG, POS.above(2));
        weakPig.setHealth(1);
        aim(player, weakPig);
        welder.onUseTick(level, player, weapon, 71_980);
        h.assertFalse(weakPig.isAlive(), "Lethal conversion pulse kills original pig");
        h.assertTrue(level.getEntities(EntityTypes.ZOMBIFIED_PIGLIN, weakPig.getBoundingBox().inflate(4), e -> true).isEmpty(),
                "Welder cannot transform a lethally hit pig");
        h.succeed();
    }

    private static void survivingTransformations(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pig = h.spawn(EntityTypes.PIG, POS);
        pig.hurtServer(level, player.damageSources().playerAttack(player), 2);
        var converted = (net.minecraft.world.entity.LivingEntity) StormLightningEvents.strike(level, pig, player);
        h.assertTrue(converted.getType() == EntityTypes.ZOMBIFIED_PIGLIN && !pig.isAlive(), "Surviving pig converts once");
        h.assertValueEqual(converted.getHealth(), 8F, "Conversion retains post-hit health");
        h.assertTrue(level.getEntities(EntityTypes.ITEM, converted.getBoundingBox().inflate(4), e -> e.getItem().is(Items.PORKCHOP)).isEmpty(),
                "Successful conversion does not drop original loot");
        converted.discard();
        var mooshroom = h.spawn(EntityTypes.MOOSHROOM, POS);
        var variant = mooshroom.getVariant();
        mooshroom.hurtServer(level, player.damageSources().playerAttack(player), 2);
        StormLightningEvents.strike(level, mooshroom, player);
        h.assertTrue(mooshroom.getVariant() != variant, "Surviving mooshroom changes variant");
        h.assertValueEqual(mooshroom.getHealth(), 8F, "Mooshroom still receives weapon damage");
        mooshroom.discard();
        var creeper = h.spawn(EntityTypes.CREEPER, POS);
        creeper.hurtServer(level, player.damageSources().playerAttack(player), 2);
        StormLightningEvents.strike(level, creeper, player);
        h.assertTrue(creeper.isPowered(), "Surviving creeper charges");
        h.assertValueEqual(creeper.getHealth(), 18F, "Charging adds no extra lightning damage");
        h.assertFalse(creeper.isOnFire(), "Charging does not ignite creeper");
        creeper.discard();
        var cow = h.spawn(EntityTypes.COW, POS);
        var naturalBolt = new net.minecraft.world.entity.LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        cow.thunderHit(level, naturalBolt);
        h.assertTrue(cow.getHealth() < 10, "Real vanilla lightning keeps its damage");
        h.succeed();
    }
}
