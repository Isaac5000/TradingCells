package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceStabilizerBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class EssenceAutomationGameTests {
    private EssenceAutomationGameTests() { }
    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("essence_external_classification_migration", 40, EssenceAutomationGameTests::classification),
                new GameTestCase("essence_stabilization_four_tiers", 40, EssenceAutomationGameTests::stabilization),
                new GameTestCase("essence_shared_xp_quota_rollback", 40, EssenceAutomationGameTests::experience),
                new GameTestCase("infuser_locked_continuous_bottles", 40, EssenceAutomationGameTests::bottles),
                new GameTestCase("infuser_missing_locked_recipe", 40, EssenceAutomationGameTests::missingRecipe),
                new GameTestCase("infuser_locked_positional_quota", 40, EssenceAutomationGameTests::positionalQuota),
                new GameTestCase("infuser_real_hopper_ticks", 100, EssenceAutomationGameTests::hoppers),
                new GameTestCase("infuser_locked_components_remainders", 40, EssenceAutomationGameTests::componentsAndRemainders),
                new GameTestCase("essence_workbench_four_tiers", 40, EssenceAutomationGameTests::workbenchTiers),
                new GameTestCase("essence_workbench_output_and_automation", 40, EssenceAutomationGameTests::workbenchOutput),
                new GameTestCase("essence_directional_inputs", 40, EssenceAutomationGameTests::directionalInputs),
                new GameTestCase("infuser_shapeless_and_ghost_lock", 40, EssenceAutomationGameTests::shapelessAndGhost),
                new GameTestCase("infuser_live_datapack_reload", 400, EssenceAutomationGameTests::datapackReload));
    }

    private static void classification(GameTestHelper helper) {
        int[] expected = {3, 4, 2};
        String[] ids = {"external_elite", "external_boss", "external_override"};
        for (int index = 0; index < ids.length; index++) {
            var entity = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.fromNamespaceAndPath("trading_cells_gametest", ids[index]));
            helper.assertTrue(entity != null, "External registered entity is constructible");
            ItemStack core = EntityEssenceData.essenceOf(entity);
            helper.assertValueEqual(EntityEssenceData.tier(core).id(), expected[index], "Tag precedence for " + ids[index]);
            helper.assertTrue(EntityEssenceData.threatScore(core) > 0, "Base stats and read-only XP sampled");
            CompoundTag root = core.get(DataComponents.CUSTOM_DATA).copyTag();
            CompoundTag data = root.getCompoundOrEmpty("TradingCellsEssence");
            data.remove("ClassificationVersion"); data.remove("Tier"); data.putBoolean("HighLevel", true);
            root.put("TradingCellsEssence", data);
            core.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            helper.assertTrue(EntityEssenceData.ensureClassified(helper.getLevel(), core), "Legacy external core migrates");
            helper.assertValueEqual(EntityEssenceData.tier(core).id(), expected[index], "Override wins even over legacy HighLevel");
            ItemStack classified = core.copy();
            EntityEssenceData.createEntity(helper.getLevel(), core);
            helper.assertTrue(ItemStack.matches(classified, core), "Classification is persisted once");
        }
        ItemStack missing = tiered(helper, 1);
        CompoundTag root = missing.get(DataComponents.CUSTOM_DATA).copyTag();
        CompoundTag data = root.getCompoundOrEmpty("TradingCellsEssence");
        data.putString("Type", "absent_mod:creature"); data.remove("ClassificationVersion");
        root.put("TradingCellsEssence", data); missing.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        ItemStack before = missing.copy();
        helper.assertFalse(EntityEssenceData.ensureClassified(helper.getLevel(), missing), "Absent type remains inactive");
        helper.assertTrue(ItemStack.matches(before, missing), "Missing mod never destroys creature data");
        var oversized = MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow"));
        oversized.getPersistentData().putString("OversizedFixture", "x".repeat(70_000));
        helper.assertTrue(EntityEssenceData.rawEssenceOf(oversized).isEmpty(), "Oversized serialization is rejected before spending resources");
        helper.succeed();
    }

    private static ItemStack tiered(GameTestHelper helper, int tier) {
        ItemStack vial = EntityEssenceData.rawEssenceOf(MobFarmLootTables.createTarget(helper.getLevel(), Identifier.withDefaultNamespace("cow")));
        CompoundTag root = vial.get(DataComponents.CUSTOM_DATA).copyTag();
        CompoundTag data = root.getCompoundOrEmpty("TradingCellsEssence");
        data.putInt("Tier", tier); root.put("TradingCellsEssence", data);
        vial.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return vial;
    }

    private static void stabilization(GameTestHelper helper) {
        var reagents = List.of(Items.REDSTONE, Items.GLOWSTONE_DUST, Items.ENDER_PEARL, Items.DRAGON_BREATH);
        for (int tier = 1; tier <= 4; tier++) {
            helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.STABILIZER.get());
            var machine = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceStabilizerBlockEntity.class);
            machine.clearContent();
            ItemStack vial = tiered(helper, tier);
            machine.setItem(0, vial);
            machine.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 1 << (tier - 1)));
            machine.setItem(2, new ItemStack(reagents.get(tier - 1), 2));
            BlockEntityStateFixtures.fillIndexedSlots(helper, machine, "Slot", 4, 1, new ItemStack(MobFarmRegistrationAdapter.EMPTY_VIAL.get(), 64));
            for (int tick = 0; tick < 110; tick++) { machine.processTick(); }
            helper.assertTrue(ItemStack.matches(machine.getItem(0), vial), "Blocked returned vial prevents consumption");
            machine.removeItem(4, 64);
            for (int tick = 0; tick < 99; tick++) { machine.processTick(); }
            helper.assertTrue(machine.getItem(3).isEmpty(), "Processing takes 100 ticks");
            CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            machine.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
            machine.processTick();
            helper.assertTrue(machine.getItem(3).is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get()), "Core output after saved progress");
            helper.assertValueEqual(machine.getItem(3).get(DataComponents.CUSTOM_DATA), vial.get(DataComponents.CUSTOM_DATA), "Complete creature snapshot copied");
            helper.assertValueEqual(machine.getItem(4).getCount(), 1, "Empty essence vial returned once");
            helper.assertValueEqual(machine.getItem(5).getCount(), tier == 4 ? 2 : 0, "Dragon breath returns both glass bottles");
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty() && machine.getItem(2).isEmpty(), "Exact tier resources consumed");
        }
        helper.succeed();
    }

    private static int insertXp(ResourceHandler<FluidResource> handler, int amount, boolean commit) {
        try (Transaction tx = Transaction.openRoot()) {
            int result = handler.insert(0, FluidResource.of(ExperienceFluidRegistration.SOURCE.get()), amount, tx);
            if (commit) { tx.commit(); }
            return result;
        }
    }

    private static void experience(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get());
        var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
        bench.setItem(0, EntityEssenceData.coreOf(tiered(helper, 1)));
        bench.setItem(1, MobFarmRegistrationAdapter.MODEL_BASES.getFirst().get().getDefaultInstance());
        helper.assertValueEqual(insertXp(bench.fluidHandler(), 5_000, false), 1_000, "Automatic XP quota equals one recipe");
        helper.assertValueEqual(bench.experience().amount(), 0, "Cancelled XP insertion rolls back");
        helper.assertValueEqual(insertXp(bench.fluidHandler(), 5_000, true), 1_000, "Committed insertion respects quota");
        bench.experience().toggleMode();
        helper.assertValueEqual(insertXp(bench.fluidHandler(), Integer.MAX_VALUE, true), Integer.MAX_VALUE - 1_000, "Fill mode reaches hard capacity without overflow");
        bench.experience().toggleMode(); bench.removeItem(1, 1);
        helper.assertValueEqual(bench.experience().amount(), Integer.MAX_VALUE, "Recipe removal never truncates stored XP");
        for (Direction side : Direction.values()) {
            var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, bench.getBlockPos(), side);
            helper.assertTrue(handler != null, "Workbench XP capability on " + side);
            try (Transaction tx = Transaction.openRoot()) {
                helper.assertValueEqual(handler.extract(0, FluidResource.of(ExperienceFluidRegistration.SOURCE.get()), 71, tx), 71, "XP can be extracted");
            }
            helper.assertValueEqual(bench.experience().amount(), Integer.MAX_VALUE, "Cancelled extraction leaves XP intact");
        }
        helper.succeed();
    }

    private static ArcaneInfuserBlockEntity infuser(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, ArcaneInfuserRegistrationAdapter.BLOCK.get());
        return helper.getBlockEntity(GameTestFixtures.TEST_POS, ArcaneInfuserBlockEntity.class);
    }
    private static int insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean commit) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            if (commit) { tx.commit(); }
            return moved;
        }
    }

    private static void bottles(GameTestHelper helper) {
        var infuser = infuser(helper);
        infuser.setItem(4, Items.GLASS_BOTTLE.getDefaultInstance());
        infuser.toggleRecipeLock();
        helper.assertValueEqual(infuser.lockState(), 1, "Lock requires recipe but no XP");
        infuser.removeItem(4, 1);
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, infuser.getBlockPos(), Direction.UP);
        helper.assertTrue(handler != null, "Infuser exposes top inputs");
        for (int cycle = 1; cycle <= 2; cycle++) {
            helper.assertValueEqual(insertItem(handler, 4, new ItemStack(Items.GLASS_BOTTLE, 64), false), 1, "Cancelled insertion admits only one bottle");
            helper.assertTrue(infuser.getItem(4).isEmpty(), "Item rollback leaves no ingredients");
            helper.assertValueEqual(insertItem(handler, 4, new ItemStack(Items.GLASS_BOTTLE, 64), true), 1, "Exactly next craft is supplied");
            helper.assertTrue(infuser.getItem(9).getCount() == cycle - 1, "Insertion never performs crafting");
            helper.assertValueEqual(insertXp(infuser.fluidHandler(), 999, true), 11, "XP quota is 11 per bottle");
            infuser.processTick();
            helper.assertValueEqual(infuser.getItem(9).getCount(), cycle, "Continuous locked output count");
            helper.assertValueEqual(infuser.storedExperience(), 0, "Exactly 11 XP consumed");
        }
        BlockEntityStateFixtures.fillIndexedSlots(helper, infuser, "Slot", 9, 1, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
        insertItem(handler, 4, new ItemStack(Items.GLASS_BOTTLE, 64), true);
        insertXp(infuser.fluidHandler(), 11, true); infuser.processTick();
        helper.assertValueEqual(infuser.getItem(4).getCount(), 1, "Full result preserves ingredients");
        helper.assertValueEqual(infuser.storedExperience(), 11, "Full result preserves XP");
        infuser.removeItem(9, 1); infuser.processTick();
        helper.assertValueEqual(infuser.getItem(9).getCount(), 64, "Production resumes once output has room");
        helper.succeed();
    }

    private static void missingRecipe(GameTestHelper helper) {
        var infuser = infuser(helper);
        infuser.setItem(4, Items.GLASS_BOTTLE.getDefaultInstance());
        var saved = infuser.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putString("LockedRecipe", "absent_mod:removed_recipe"); saved.putInt("StoredExperience", 777);
        infuser.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertValueEqual(infuser.lockState(), 2, "Missing recipe stays visibly locked-invalid");
        helper.assertValueEqual(infuser.insertionLimit(4, Items.GLASS_BOTTLE.getDefaultInstance()), 0, "Invalid lock rejects inputs");
        infuser.processTick();
        helper.assertTrue(infuser.getItem(9).isEmpty(), "Invalid lock never substitutes a matching recipe");
        infuser.toggleRecipeLock();
        helper.assertValueEqual(infuser.storedExperience(), 777, "Unlock preserves XP");
        helper.assertValueEqual(infuser.getItem(4).getCount(), 1, "Unlock preserves inventory");
        helper.succeed();
    }

    private static void positionalQuota(GameTestHelper helper) {
        var infuser = infuser(helper);
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var menu = (ArcaneInfuserMenu) infuser.createMenu(1, player.getInventory(), player);
        var recipe = helper.getLevel().recipeAccess().getRecipes().stream()
                .filter(holder -> holder.id().identifier().toString().equals("trading_cells:mob_farm_infusion")).findFirst().orElseThrow();
        var blackConcrete = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:black_concrete")).orElseThrow();
        var ingredients = List.of(Items.LAPIS_BLOCK, blackConcrete, Items.LAPIS_BLOCK, blackConcrete,
                com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter.ITEM.get(),
                blackConcrete, Items.LAPIS_BLOCK,
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse("trading_cells:storm_shard")).orElseThrow(), Items.LAPIS_BLOCK);
        for (int slot = 0; slot < 9; slot++) { infuser.setItem(slot, new ItemStack(ingredients.get(slot), 2)); }
        infuser.toggleRecipeLock();
        helper.assertValueEqual(infuser.lockState(), 1, "Repeated ingredients lock correctly");
        menu.handlePlacement(false, false, recipe, helper.getLevel(), player.getInventory());
        helper.assertValueEqual(infuser.getItem(0).getCount(), 2, "Recipe book cannot replace locked manual batches");
        for (int slot = 0; slot < 9; slot++) {
            helper.assertValueEqual(infuser.insertionLimit(slot, new ItemStack(ingredients.get(slot))), 0, "Existing manual batches are not trimmed or topped up");
            infuser.removeItem(slot, 2);
            helper.assertValueEqual(infuser.insertionLimit(slot, new ItemStack(ingredients.get(slot))), 1, "Each repeated position has its own quota");
        }
        helper.assertValueEqual(infuser.insertionLimit(0, blackConcrete.getDefaultInstance()), 0, "Ingredient at wrong position rejected");
        helper.succeed();
    }

    private static void hoppers(GameTestHelper helper) {
        var machine = infuser(helper);
        machine.setItem(4, Items.GLASS_BOTTLE.getDefaultInstance());
        machine.toggleRecipeLock(); machine.removeItem(4, 1);
        var inputPos = GameTestFixtures.TEST_POS.above();
        var outputPos = GameTestFixtures.TEST_POS.below();
        helper.setBlock(inputPos, net.minecraft.world.level.block.Blocks.HOPPER);
        helper.setBlock(outputPos, net.minecraft.world.level.block.Blocks.HOPPER);
        var input = helper.getBlockEntity(inputPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        var output = helper.getBlockEntity(outputPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        input.setItem(0, new ItemStack(Items.GLASS_BOTTLE, 64));
        helper.runAtTickTime(16, () -> {
            helper.assertValueEqual(bottleInputs(machine), 1, "Real hopper supplies only one recipe without XP");
            helper.assertValueEqual(input.getItem(0).getCount(), 63, "Hopper does not fill the physical input stack");
        });
        helper.onEachTick(() -> {
            helper.assertTrue(bottleInputs(machine) <= 1, "Top hopper always respects the locked quota");
            if (helper.getTick() >= 17 && output.getItem(0).getCount() + machine.getItem(9).getCount() < 2) {
                insertXp(machine.fluidHandler(), 11, true);
            }
        });
        helper.runAtTickTime(65, () -> {
            helper.assertTrue(output.getItem(0).is(Items.EXPERIENCE_BOTTLE), "Bottom hopper extracts physical output");
            helper.assertValueEqual(output.getItem(0).getCount(), 2, "Real server ticks produce two successive bottles");
            helper.assertValueEqual(machine.storedExperience(), 0, "Hopper production consumes exactly 22 XP");
            helper.assertValueEqual(input.getItem(0).getCount() + bottleInputs(machine), 62, "Only two bottles consumed");
            helper.succeed();
        });
    }

    private static int bottleInputs(ArcaneInfuserBlockEntity machine) {
        int total = 0;
        for (int slot = 0; slot < 9; slot++) { total += machine.getItem(slot).getCount(); }
        return total;
    }

    private static void componentsAndRemainders(GameTestHelper helper) {
        var machine = infuser(helper);
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var silk = net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(new net.minecraft.world.item.enchantment.EnchantmentInstance(
                enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1));
        var fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(new net.minecraft.world.item.enchantment.EnchantmentInstance(
                enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 1));
        var ingredients = List.of(Items.ECHO_SHARD, Items.AMETHYST_SHARD, Items.ECHO_SHARD, Items.TURTLE_EGG,
                Items.ENCHANTED_BOOK, Items.TURTLE_EGG, Items.ECHO_SHARD, Items.NETHER_STAR, Items.ECHO_SHARD);
        for (int slot = 0; slot < 9; slot++) { machine.setItem(slot, slot == 4 ? silk : ingredients.get(slot).getDefaultInstance()); }
        machine.toggleRecipeLock(); machine.removeItem(4, 1);
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, machine.getBlockPos(), Direction.NORTH);
        helper.assertValueEqual(insertItem(handler, 4, fortune, true), 0, "Locked output restrictions reject the wrong enchantment");
        helper.assertValueEqual(insertItem(handler, 4, silk, true), 1, "Required enchanted component remains accepted");
        machine.toggleRecipeLock(); machine.clearContent();
        ItemStack bucketLot = new ItemStack(Items.WATER_BUCKET, 2);
        bucketLot.set(DataComponents.MAX_STACK_SIZE, 64);
        machine.setItem(7, bucketLot);
        machine.toggleRecipeLock(); insertXp(machine.fluidHandler(), 10, true);
        machine.processTick();
        helper.assertTrue(machine.getItem(9).isEmpty(), "Incompatible bucket remainder cannot erase an existing manual lot");
        helper.assertValueEqual(machine.getItem(7).getCount(), 2, "Blocked remainder keeps inputs");
        helper.assertValueEqual(machine.storedExperience(), 10, "Blocked remainder keeps XP");
        machine.removeItem(7, 1); machine.processTick();
        helper.assertTrue(machine.getItem(7).is(Items.BUCKET), "Automatic craft returns its bucket in the original slot");
        helper.assertTrue(machine.getItem(9).is(Items.COBBLESTONE), "Automatic result stored physically");
        helper.succeed();
    }

    private static void workbenchTiers(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get());
        var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
        var player = MobSimulationGameTests.connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(bench.getBlockPos()));
        for (int tier = 1; tier <= 4; tier++) {
            bench.clearContent();
            ItemStack core = EntityEssenceData.coreOf(tiered(helper, tier));
            bench.setItem(0, core.copy());
            bench.setItem(1, MobFarmRegistrationAdapter.MODEL_BASES.get(tier % 4).get().getDefaultInstance());
            int cost = EssenceTier.values()[tier - 1].modelExperience();
            bench.experience().setRaw(cost);
            helper.assertFalse(bench.synthesize(player), "Different-tier base rejected");
            helper.assertValueEqual(bench.experience().amount(), cost, "Invalid synthesis does not spend XP");
            bench.setItem(1, MobFarmRegistrationAdapter.MODEL_BASES.get(tier - 1).get().getDefaultInstance());
            helper.assertTrue(bench.synthesize(player), "Matching base synthesizes tier " + tier);
            helper.assertValueEqual(bench.experience().amount(), 0, "Exact cost consumed for tier " + tier);
            helper.assertValueEqual(bench.getItem(3).get(DataComponents.CUSTOM_DATA), core.get(DataComponents.CUSTOM_DATA), "Model retains complete creature data");
        }
        helper.succeed();
    }

    private static void datapackReload(GameTestHelper helper) {
        var machine = infuser(helper);
        machine.setItem(4, Items.GLASS_BOTTLE.getDefaultInstance()); machine.toggleRecipeLock();
        var manager = helper.getLevel().recipeAccess();
        var id = machine.lockedRecipeId();
        var server = helper.getLevel().getServer();
        var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> {
            helper.assertTrue(reload.isDone(), "Datapack reload completes");
            helper.assertFalse(reload.isCompletedExceptionally(), "Datapack reload succeeds");
            helper.assertTrue(helper.getLevel().recipeAccess() != manager, "Real recipe manager replaced");
            helper.assertValueEqual(machine.lockedRecipeId(), id, "Lock identity survives reload");
            helper.assertValueEqual(machine.lockState(), 1, "Recipe resolves against the replacement manager");
            helper.assertValueEqual(machine.requiredExperience(), 11, "Reload retains XP cost");
        });
    }

    private static void workbenchOutput(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get());
        var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
        var player = MobSimulationGameTests.connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(bench.getBlockPos()));
        var menu = (com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu) bench.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu.getSlot(3).getItem().isEmpty(), "Empty workbench has no default result");
        for (int mode = 0; mode < 3; mode++) {
            bench.setItem(0, EntityEssenceData.coreOf(tiered(helper, 1)));
            bench.setItem(1, MobFarmRegistrationAdapter.MODEL_BASES.getFirst().get().getDefaultInstance());
            bench.experience().setRaw(1_000);
            bench.processTick();
            helper.assertTrue(bench.getItem(3).isEmpty(), "Manual preview is not a physical output");
            helper.assertValueEqual(bench.experience().amount(), 1_000, "Preview does not consume XP");
            if (mode == 0) {
                menu.clicked(3, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
                helper.assertTrue(menu.getCarried().is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Normal output pickup crafts once");
                menu.setCarried(ItemStack.EMPTY);
            } else if (mode == 1) {
                for (int slot = 0; slot < 36; slot++) { player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64)); }
                helper.assertTrue(menu.quickMoveStack(player, 3).isEmpty(), "Full inventory rejects shift-craft");
                helper.assertValueEqual(bench.experience().amount(), 1_000, "Failed shift-craft preserves XP");
                player.getInventory().setItem(8, ItemStack.EMPTY);
                helper.assertTrue(menu.quickMoveStack(player, 3).is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Shift pickup crafts into free inventory slot");
            } else {
                bench.toggleAutomatic();
                var saved = bench.saveWithFullMetadata(helper.getLevel().registryAccess());
                bench.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
                helper.assertTrue(bench.automatic(), "Automatic mode persists");
                bench.processTick();
                helper.assertTrue(bench.getItem(3).is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()), "Automatic mode stores a physical model");
                bench.removeItem(3, 1);
            }
            helper.assertValueEqual(bench.experience().amount(), 0, "Each craft consumes exact XP");
            helper.assertTrue(bench.getItem(0).isEmpty() && bench.getItem(1).isEmpty(), "Each craft consumes core and base once");
        }
        helper.succeed();
    }

    private static void directionalInputs(GameTestHelper helper) {
        for (Direction front : Direction.Plane.HORIZONTAL) {
            helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.ESSENCE_WORKBENCH.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, front));
            var bench = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceWorkbenchBlockEntity.class);
            for (Direction side : Direction.values()) {
                var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, bench.getBlockPos(), side);
                ItemStack core = EntityEssenceData.coreOf(tiered(helper, 1));
                ItemStack base = MobFarmRegistrationAdapter.MODEL_BASES.getFirst().get().getDefaultInstance();
                base.setCount(64);
                helper.assertValueEqual(insertItem(handler, 0, core, false), side == front.getClockWise() ? 1 : 0, "Only left accepts core " + front + "/" + side);
                helper.assertValueEqual(insertItem(handler, 0, base, false), side == front.getCounterClockWise() ? 1 : 0, "Only right accepts one base " + front + "/" + side);
            }
            helper.setBlock(GameTestFixtures.TEST_POS, MobFarmRegistrationAdapter.STABILIZER.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, front));
            var stabilizer = helper.getBlockEntity(GameTestFixtures.TEST_POS, EssenceStabilizerBlockEntity.class);
            for (Direction side : Direction.values()) {
                var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, stabilizer.getBlockPos(), side);
                helper.assertValueEqual(insertItem(handler, 0, tiered(helper, 3), false), side == front.getClockWise() ? 1 : 0, "Only left accepts raw vial");
            }
            stabilizer.setItem(0, tiered(helper, 3));
            for (Direction side : Direction.values()) {
                var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, stabilizer.getBlockPos(), side);
                helper.assertValueEqual(insertItem(handler, 0, new ItemStack(Items.AMETHYST_SHARD, 64), false), side == Direction.UP ? 4 : 0, "Only top accepts tier amethyst quota");
                helper.assertValueEqual(insertItem(handler, 0, new ItemStack(Items.ENDER_PEARL, 16), false), side == front.getCounterClockWise() ? 2 : 0, "Only right accepts matching reagent quota");
                helper.assertValueEqual(insertItem(handler, 0, new ItemStack(Items.REDSTONE, 64), false), 0, "Wrong-tier reagent rejected");
            }
        }
        helper.succeed();
    }

    private static void shapelessAndGhost(GameTestHelper helper) {
        var machine = infuser(helper);
        var player = MobSimulationGameTests.connectedPlayer(helper);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()));
        var menu = (ArcaneInfuserMenu) machine.createMenu(1, player.getInventory(), player);
        var recipe = helper.getLevel().recipeAccess().getRecipes().stream()
                .filter(holder -> holder.id().identifier().toString().equals("trading_cells:experience_bottle_infusion")).findFirst().orElseThrow();
        helper.assertValueEqual(menu.handlePlacement(false, false, recipe, helper.getLevel(), player.getInventory()),
                net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE, "Book selects absent ingredients as ghost");
        helper.assertTrue(menu.clickMenuButton(player, 1), "Ghost can be locked without ingredients or XP");
        helper.assertValueEqual(machine.lockState(), 1, "Book-selected recipe is valid and locked");
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, machine.getBlockPos(), Direction.UP);
        for (int slot = 0; slot < 9; slot++) {
            helper.assertValueEqual(insertItem(handler, slot, new ItemStack(Items.GLASS_BOTTLE, 64), true), 1, "Shapeless bottle accepted at slot " + slot);
            helper.assertValueEqual(insertItem(handler, (slot + 1) % 9, Items.GLASS_BOTTLE.getDefaultInstance(), true), 0, "Shapeless quota shared across positions");
            machine.experience().setRaw(11); machine.processTick();
            helper.assertValueEqual(machine.getItem(9).getCount(), slot + 1, "Bottle crafts from every position");
            helper.assertTrue(machine.getItem(slot).isEmpty(), "Actual ingredient position consumed");
        }
        machine.removeItem(9, 9);
        helper.assertValueEqual(machine.lockedRecipeId(), recipe.id().identifier(), "Empty output keeps locked recipe identity");
        helper.succeed();
    }
}
