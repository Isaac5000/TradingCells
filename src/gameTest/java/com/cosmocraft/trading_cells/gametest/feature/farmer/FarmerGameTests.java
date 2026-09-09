package com.cosmocraft.trading_cells.gametest.feature.farmer;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerCropStackAdapter;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.output.FarmerRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCrop;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerCycle;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerProduct;
import com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerKind;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Behaviour-oriented GameTests for Farmer. */
public final class FarmerGameTests {
    private FarmerGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("villager_crop_catalog", 20, FarmerGameTests::villagerCropCatalog),
            new GameTestCase("farmer_crop_datapack_snapshot", 20,
                    FarmerGameTests::farmerCropDatapackSnapshot),
            new GameTestCase("farmer_partial_output_capacity", 20, FarmerGameTests::farmerPartialOutputCapacity),
            new GameTestCase("farmer_fortune_matrix", 20, FarmerGameTests::farmerFortuneMatrix)
        );
    }

    private static void farmerCropDatapackSnapshot(GameTestHelper helper) {
        ItemStack deadBush = new ItemStack(Items.DEAD_BUSH);
        helper.assertTrue(FarmerCropStackAdapter.isDynamicCrop(FarmerKind.VILLAGER, deadBush),
                "The valid GameTest crop descriptor must be loaded");
        helper.assertTrue(
                FarmerCropStackAdapter.cropState(FarmerKind.VILLAGER, deadBush, 20, 100)
                        .is(Blocks.DEAD_BUSH),
                "The descriptor crop block must drive rendering"
        );
        helper.assertTrue(FarmerCropStackAdapter.soilState(FarmerKind.VILLAGER, deadBush).is(Blocks.SAND),
                "The descriptor support block must drive the farm base");
        List<ItemStack> drops = FarmerCropStackAdapter.dynamicHarvest(
                helper.getLevel(),
                helper.absolutePos(GameTestFixtures.TEST_POS),
                FarmerKind.VILLAGER,
                deadBush,
                new ItemStack(Items.WOODEN_HOE),
                3,
                false
        );
        helper.assertTrue(drops.size() == 1
                        && drops.getFirst().is(Items.STICK)
                        && drops.getFirst().getCount() == 4,
                "The descriptor output must apply Fortune without registry scans per cycle");
        helper.assertTrue(FarmerCropStackAdapter.isSupported(FarmerKind.VILLAGER,
                        new ItemStack(Items.WHEAT_SEEDS)),
                "An invalid neighboring descriptor must not remove the vanilla fallback");
        ItemStack deadFireCoral = new ItemStack(Items.DEAD_FIRE_CORAL);
        helper.assertTrue(FarmerCropStackAdapter.isDynamicCrop(FarmerKind.PIGLIN, deadFireCoral),
                "Piglin crop descriptors must be present in the startup snapshot");
        helper.assertTrue(FarmerCropStackAdapter.soilState(FarmerKind.PIGLIN, deadFireCoral)
                        .is(Blocks.BLACKSTONE),
                "Piglin crop descriptors must preserve their support block");
        helper.succeed();
    }

    private static void farmerFortuneMatrix(GameTestHelper helper) {
        for (int fortune : new int[]{0, 3, 7, 255}) {
            assertDynamicYieldCount(helper, Items.DANDELION, Items.DANDELION, fortune, fortune + 1);
            assertDynamicYieldCount(helper, Items.TUBE_CORAL_BLOCK, Items.TUBE_CORAL_BLOCK, fortune, fortune + 1);
            assertDynamicYieldCount(helper, Items.RED_MUSHROOM, Items.RED_MUSHROOM, fortune, fortune + 1);
            assertDynamicYieldCount(helper, Items.OAK_SAPLING, Items.OAK_LOG, fortune, fortune + 4);
            assertDynamicYieldCount(
                    helper,
                    Items.POISONOUS_POTATO,
                    Items.POISONOUS_POTATO,
                    fortune,
                    fortune + 1
            );

            var torchflower = FarmerCycle.harvest(FarmerCrop.TORCHFLOWER, fortune);
            helper.assertValueEqual(
                    staticYieldCount(torchflower, FarmerProduct.TORCHFLOWER),
                    fortune + 1,
                    "Torchflower fruit at Fortune " + fortune
            );
            helper.assertValueEqual(
                    staticYieldCount(torchflower, FarmerProduct.TORCHFLOWER_SEEDS),
                    fortune + 1,
                    "Torchflower seed at Fortune " + fortune
            );
            helper.assertValueEqual(
                    staticYieldCount(
                            FarmerCycle.harvest(FarmerCrop.CRIMSON_FUNGUS, fortune),
                            FarmerProduct.CRIMSON_STEM
                    ),
                    fortune + 4,
                    "Crimson stem at Fortune " + fortune
            );
        }
        helper.succeed();
    }

    private static void assertDynamicYieldCount(
            GameTestHelper helper,
            Item input,
            Item output,
            int fortune,
            int expected
    ) {
        int actual = FarmerCropStackAdapter.dynamicVillagerHarvest(
                        helper.getLevel(),
                        helper.absolutePos(GameTestFixtures.TEST_POS),
                        new ItemStack(input),
                        new ItemStack(Items.WOODEN_HOE),
                        fortune,
                        false
                ).stream()
                .filter(stack -> stack.is(output))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(
                actual,
                expected,
                BuiltInRegistries.ITEM.getKey(output) + " at Fortune " + fortune
        );
    }

    private static int staticYieldCount(
            com.cosmocraft.trading_cells.feature.farmer.domain.model.FarmerHarvest harvest,
            FarmerProduct product
    ) {
        return harvest.yields().stream()
                .filter(yield -> yield.product() == product)
                .findFirst()
                .orElseThrow()
                .count();
    }

    private static void farmerPartialOutputCapacity(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, FarmerRegistrationAdapter.FARMER_BLOCK.get());
        FarmerBlockEntity villagerFarmer = helper.getBlockEntity(GameTestFixtures.TEST_POS, FarmerBlockEntity.class);
        villagerFarmer.setItem(FarmerBlockEntity.WORKER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
        villagerFarmer.setItem(FarmerBlockEntity.CROP_SLOT, new ItemStack(Items.WHEAT_SEEDS));
        villagerFarmer.setItem(FarmerBlockEntity.HOE_SLOT, new ItemStack(Items.WOODEN_HOE));
        fillFarmerOutputsExceptFirst(helper, villagerFarmer);

        completeFarmerCycle(villagerFarmer);
        helper.assertTrue(
                villagerFarmer.getItem(FarmerBlockEntity.FIRST_OUTPUT_SLOT).is(Items.WHEAT),
                "Villager Farmer must insert the part of a multi-output harvest that still fits"
        );
        completeFarmerCycle(villagerFarmer);
        helper.assertValueEqual(
                villagerFarmer.getItem(FarmerBlockEntity.FIRST_OUTPUT_SLOT).getCount(),
                2,
                "Villager Farmer partial output count"
        );
        villagerFarmer.getItem(FarmerBlockEntity.FIRST_OUTPUT_SLOT).setCount(64);
        villagerFarmer.dataAccess().set(0, villagerFarmer.growthDurationTicks() - 1);
        villagerFarmer.processTick();
        helper.assertValueEqual(
                villagerFarmer.growthTicks(),
                villagerFarmer.growthDurationTicks() - 1,
                "Villager Farmer must pause when no possible output can fit"
        );
        villagerFarmer.setItem(FarmerBlockEntity.CROP_SLOT, new ItemStack(Items.MANGROVE_PROPAGULE));
        fillFarmerOutputsExceptFirst(helper, villagerFarmer);
        completeFarmerCycle(villagerFarmer);
        helper.assertTrue(
                villagerFarmer.getItem(FarmerBlockEntity.FIRST_OUTPUT_SLOT).is(Items.MANGROVE_LOG),
                "Dynamic villager crops must insert the part of their harvest that still fits"
        );
        villagerFarmer.setItem(FarmerBlockEntity.CROP_SLOT, new ItemStack(Items.SHORT_GRASS));
        fillFarmerOutputs(helper, villagerFarmer);
        villagerFarmer.dataAccess().set(0, villagerFarmer.growthDurationTicks() - 1);
        villagerFarmer.processTick();
        helper.assertValueEqual(
                villagerFarmer.growthTicks(),
                0,
                "A valid crop roll without drops must complete instead of blocking on a full output"
        );

        helper.setBlock(GameTestFixtures.TEST_POS, FarmerRegistrationAdapter.PIGLIN_FARMER_BLOCK.get());
        FarmerBlockEntity piglinFarmer = helper.getBlockEntity(GameTestFixtures.TEST_POS, FarmerBlockEntity.class);
        piglinFarmer.setItem(FarmerBlockEntity.WORKER_SLOT, GameTestFixtures.adultPiglinCapture(helper));
        piglinFarmer.setItem(FarmerBlockEntity.CROP_SLOT, new ItemStack(Items.CRIMSON_FUNGUS));
        piglinFarmer.setItem(FarmerBlockEntity.HOE_SLOT, new ItemStack(Items.WOODEN_HOE));
        fillFarmerOutputsExceptFirst(helper, piglinFarmer);
        completeFarmerCycle(piglinFarmer);
        helper.assertTrue(
                piglinFarmer.getItem(FarmerBlockEntity.FIRST_OUTPUT_SLOT).is(Items.CRIMSON_STEM),
                "Piglin Farmer must insert the part of a multi-output harvest that still fits"
        );
        helper.succeed();
    }

    private static void completeFarmerCycle(FarmerBlockEntity farmer) {
        farmer.dataAccess().set(0, farmer.growthDurationTicks() - 1);
        farmer.processTick();
    }

    private static void fillFarmerOutputsExceptFirst(GameTestHelper helper, FarmerBlockEntity farmer) {
        BlockEntityStateFixtures.clearIndexedSlots(
                helper,
                farmer,
                "Slot",
                FarmerBlockEntity.FIRST_OUTPUT_SLOT,
                1
        );
        BlockEntityStateFixtures.fillIndexedSlots(
                helper,
                farmer,
                "Slot",
                FarmerBlockEntity.FIRST_OUTPUT_SLOT + 1,
                FarmerBlockEntity.OUTPUT_SLOT_COUNT - 1,
                new ItemStack(Items.COBBLESTONE, 64)
        );
    }

    private static void fillFarmerOutputs(GameTestHelper helper, FarmerBlockEntity farmer) {
        BlockEntityStateFixtures.fillIndexedSlots(
                helper,
                farmer,
                "Slot",
                FarmerBlockEntity.FIRST_OUTPUT_SLOT,
                FarmerBlockEntity.OUTPUT_SLOT_COUNT,
                new ItemStack(Items.COBBLESTONE, 64)
        );
    }

    private static void villagerCropCatalog(GameTestHelper helper) {
        List<FarmerCropStackAdapter.Option> options = FarmerCropStackAdapter.villagerOptions();
        helper.assertTrue(options.size() >= 60, "Expected the extended vanilla villager crop catalogue");
        helper.assertTrue(
                options.stream().anyMatch(option -> option.item() == Items.DANDELION),
                "Dandelions must be supported"
        );
        helper.assertTrue(
                options.stream().anyMatch(option -> option.item() == Items.BAMBOO),
                "Bamboo must be supported"
        );
        helper.assertTrue(
                options.stream().noneMatch(option -> option.item() == Items.NETHERRACK),
                "Netherrack must not be treated as a villager crop"
        );
        helper.assertTrue(
                options.stream().noneMatch(option -> option.item() == Items.MANGROVE_LEAVES),
                "Mangrove leaves must not duplicate the mangrove propagule profile"
        );
        helper.assertTrue(
                options.stream().noneMatch(option -> option.item() == Items.TORCHFLOWER),
                "Torchflowers must use their seed profile"
        );
        helper.assertTrue(
                options.stream().anyMatch(option -> option.item() == Items.MOSS_BLOCK),
                "Moss blocks must be supported"
        );
        helper.assertTrue(
                options.stream().anyMatch(option -> option.item() == Items.PALE_MOSS_BLOCK),
                "Pale moss blocks must be supported"
        );
        helper.assertTrue(
                options.stream().anyMatch(option -> option.item() == Items.GOLDEN_DANDELION),
                "Golden dandelions must be supported"
        );
        for (Item required : List.of(
                Items.KELP,
                Items.SEAGRASS,
                Items.SEA_PICKLE,
                Items.TUBE_CORAL_BLOCK,
                Items.TUBE_CORAL,
                Items.TUBE_CORAL_FAN,
                Items.BRAIN_CORAL_BLOCK,
                Items.BRAIN_CORAL,
                Items.BRAIN_CORAL_FAN,
                Items.BUBBLE_CORAL_BLOCK,
                Items.BUBBLE_CORAL,
                Items.BUBBLE_CORAL_FAN,
                Items.FIRE_CORAL_BLOCK,
                Items.FIRE_CORAL,
                Items.FIRE_CORAL_FAN,
                Items.HORN_CORAL_BLOCK,
                Items.HORN_CORAL,
                Items.HORN_CORAL_FAN,
                Items.MOSS_BLOCK,
                Items.MOSS_CARPET,
                Items.PALE_MOSS_BLOCK,
                Items.PALE_MOSS_CARPET,
                Items.PALE_HANGING_MOSS,
                Items.GLOW_LICHEN,
                Items.CHORUS_FLOWER
        )) {
            helper.assertTrue(
                    options.stream().anyMatch(option -> option.item() == required),
                    "Missing reviewed villager crop: " + BuiltInRegistries.ITEM.getKey(required)
            );
        }
        FarmerCropStackAdapter.Option oak = options.stream()
                .filter(option -> option.item() == Items.OAK_SAPLING)
                .findFirst()
                .orElseThrow();
        List<FarmerCropStackAdapter.PreviewYield> yields = FarmerCropStackAdapter.previewYields(oak);
        helper.assertTrue(yields.size() == 4, "Oak must expose four balanced outputs");
        helper.assertTrue(
                yields.stream().anyMatch(yield -> yield.stack().is(Items.OAK_LOG)),
                "Oak must produce logs"
        );
        helper.assertTrue(
                yields.stream().anyMatch(yield -> yield.stack().is(Items.OAK_LEAVES)),
                "Oak must produce leaves"
        );
        helper.assertTrue(
                yields.stream().anyMatch(yield -> yield.stack().is(Items.OAK_SAPLING)),
                "Oak must return saplings"
        );
        helper.assertTrue(
                yields.stream().anyMatch(yield -> yield.stack().is(Items.APPLE)),
                "Oak must be able to produce apples"
        );
        FarmerCropStackAdapter.Option mangrove = options.stream()
                .filter(option -> option.item() == Items.MANGROVE_PROPAGULE)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                FarmerCropStackAdapter.previewYields(mangrove).stream()
                        .anyMatch(yield -> yield.stack().is(Items.MANGROVE_ROOTS)),
                "Mangroves must produce mangrove roots"
        );
        for (Item coralItem : List.of(
                Items.TUBE_CORAL_BLOCK,
                Items.TUBE_CORAL,
                Items.TUBE_CORAL_FAN
        )) {
            FarmerCropStackAdapter.Option coral = options.stream()
                    .filter(option -> option.item() == coralItem)
                    .findFirst()
                    .orElseThrow();
            List<FarmerCropStackAdapter.PreviewYield> coralYields =
                    FarmerCropStackAdapter.previewYields(coral);
            helper.assertTrue(
                    coralYields.size() == 1
                            && coralYields.getFirst().stack().is(coralItem)
                            && coralYields.getFirst().stack().getCount() == 1
                            && coralYields.getFirst().isGuaranteed(),
                    "Each coral form must only reproduce itself"
            );
        }
        FarmerCropStackAdapter.Option dandelion = options.stream()
                .filter(option -> option.item() == Items.DANDELION)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                FarmerCropStackAdapter.previewYields(dandelion).stream()
                        .anyMatch(yield -> yield.stack().is(Items.DANDELION)
                                && yield.stack().getCount() == 1
                                && yield.isGuaranteed()),
                "Simple plants must produce one item before Fortune"
        );
        List<ItemStack> fortunateFlowers = FarmerCropStackAdapter.dynamicVillagerHarvest(
                helper.getLevel(),
                helper.absolutePos(GameTestFixtures.TEST_POS),
                new ItemStack(Items.DANDELION),
                new ItemStack(Items.WOODEN_HOE),
                3,
                false
        );
        helper.assertTrue(
                fortunateFlowers.size() == 1
                        && fortunateFlowers.getFirst().is(Items.DANDELION)
                        && fortunateFlowers.getFirst().getCount() == 4,
                "Fortune must increase simple plant output"
        );
        FarmerCropStackAdapter.Option poisonousPotato = options.stream()
                .filter(option -> option.item() == Items.POISONOUS_POTATO)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                FarmerCropStackAdapter.previewYields(poisonousPotato).stream()
                        .anyMatch(yield -> yield.stack().is(Items.POISONOUS_POTATO)
                                && yield.stack().getCount() == 1),
                "Poisonous potatoes must have an independent crop profile"
        );
        List<ItemStack> fortunatePoisonousPotatoes = FarmerCropStackAdapter.dynamicVillagerHarvest(
                helper.getLevel(),
                helper.absolutePos(GameTestFixtures.TEST_POS),
                new ItemStack(Items.POISONOUS_POTATO),
                new ItemStack(Items.WOODEN_HOE),
                3,
                false
        );
        helper.assertTrue(
                fortunatePoisonousPotatoes.size() == 1
                        && fortunatePoisonousPotatoes.getFirst().is(Items.POISONOUS_POTATO)
                        && fortunatePoisonousPotatoes.getFirst().getCount() == 4,
                "Fortune must increase independently cultivated poisonous potatoes"
        );
        helper.assertTrue(
                FarmerCropStackAdapter.renderSupport(
                        FarmerKind.VILLAGER,
                        new ItemStack(Items.VINE)
                ) == FarmerCropStackAdapter.RenderSupport.WALL,
                "Villager wall plants must use the shared wall support"
        );
        helper.assertTrue(
                FarmerCropStackAdapter.renderSupport(
                        FarmerKind.PIGLIN,
                        new ItemStack(Items.WEEPING_VINES)
                ) == FarmerCropStackAdapter.RenderSupport.CEILING,
                "Piglin ceiling plants must use the shared ceiling support"
        );
        FarmerCropStackAdapter.Option moss = options.stream()
                .filter(option -> option.item() == Items.MOSS_BLOCK)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                FarmerCropStackAdapter.previewYields(moss).stream()
                        .allMatch(yield -> yield.stack().is(Items.MOSS_BLOCK)),
                "Moss carpets and azaleas must have independent crop profiles"
        );
        FarmerCropStackAdapter.Option paleMoss = options.stream()
                .filter(option -> option.item() == Items.PALE_MOSS_BLOCK)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                FarmerCropStackAdapter.previewYields(paleMoss).stream()
                        .allMatch(yield -> yield.stack().is(Items.PALE_MOSS_BLOCK)),
                "Pale moss carpet and hanging moss must have independent crop profiles"
        );
        FarmerCropStackAdapter.Option chorus = options.stream()
                .filter(option -> option.item() == Items.CHORUS_FLOWER)
                .findFirst()
                .orElseThrow();
        List<FarmerCropStackAdapter.PreviewYield> chorusYields = FarmerCropStackAdapter.previewYields(chorus);
        helper.assertTrue(
                chorusYields.stream().noneMatch(yield -> yield.stack().is(Items.CHORUS_PLANT)),
                "Chorus plants are technical blocks and must not be obtainable as crop output"
        );
        helper.assertTrue(
                chorusYields.stream().anyMatch(yield -> yield.stack().is(Items.CHORUS_FRUIT)),
                "Chorus flowers must be able to produce chorus fruit"
        );
        FarmerCropStackAdapter.Option redMushroom = options.stream()
                .filter(option -> option.item() == Items.RED_MUSHROOM)
                .findFirst()
                .orElseThrow();
        List<FarmerCropStackAdapter.PreviewYield> mushroomYields =
                FarmerCropStackAdapter.previewYields(redMushroom);
        helper.assertTrue(
                mushroomYields.stream().anyMatch(yield -> yield.stack().is(Items.RED_MUSHROOM)
                        && yield.isGuaranteed()
                        && yield.stack().getCount() == 1),
                "Mushroom crops must always produce one mushroom before Fortune"
        );
        helper.assertTrue(
                mushroomYields.stream()
                        .filter(yield -> yield.stack().is(Items.MUSHROOM_STEM)
                                || yield.stack().is(Items.RED_MUSHROOM_BLOCK))
                        .allMatch(yield -> yield.requiresSilkTouch() && !yield.isGuaranteed()),
                "Mushroom blocks and stems must be chance-based and require Silk Touch"
        );
        List<ItemStack> withoutSilkTouch = FarmerCropStackAdapter.dynamicVillagerHarvest(
                helper.getLevel(),
                helper.absolutePos(GameTestFixtures.TEST_POS),
                new ItemStack(Items.RED_MUSHROOM),
                new ItemStack(Items.WOODEN_HOE),
                255,
                false
        );
        helper.assertTrue(
                withoutSilkTouch.stream().noneMatch(stack -> stack.is(Items.MUSHROOM_STEM)
                        || stack.is(Items.RED_MUSHROOM_BLOCK)),
                "Mushroom blocks must never be harvested without Silk Touch"
        );
        helper.assertTrue(
                withoutSilkTouch.stream().anyMatch(stack -> stack.is(Items.RED_MUSHROOM)
                        && stack.getCount() == 256),
                "Fortune must increase the guaranteed mushroom output"
        );
        helper.succeed();
    }
}
