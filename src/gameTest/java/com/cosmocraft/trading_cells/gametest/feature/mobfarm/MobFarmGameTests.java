package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmLootAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output.RaiderFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.output.CreeperFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.ZombieFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Behaviour-oriented GameTests for MobFarm. */
public final class MobFarmGameTests {
    private MobFarmGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("mob_farm_xp_only_cycles", 20, MobFarmGameTests::mobFarmXpOnlyCycles),
            new GameTestCase("mob_farm_output_capacity", 20, MobFarmGameTests::mobFarmOutputCapacity),
            new GameTestCase("mob_farm_selection_persistence", 20,
                    MobFarmGameTests::mobFarmSelectionPersistence),
            new GameTestCase("historical_mob_farm_activity_states", 20,
                    MobFarmGameTests::historicalMobFarmActivityStates),
            new GameTestCase("historical_mob_farm_real_sided_transfers", 20,
                    MobFarmGameTests::historicalMobFarmRealSidedTransfers),
            new GameTestCase("raider_farm_vanilla_special_drops", 20,
                    MobFarmGameTests::raiderFarmVanillaSpecialDrops)
        );
    }

    private static void historicalMobFarmActivityStates(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, SkeletonFarmRegistrationAdapter.BLOCK.get());
        SkeletonFarmBlockEntity skeleton = helper.getBlockEntity(
                GameTestFixtures.TEST_POS, SkeletonFarmBlockEntity.class);
        assertActivityStates(helper, "Skeleton Farm", skeleton, skeleton.dataAccess(),
                skeleton::processTick, skeleton::toggleEnabled);

        helper.setBlock(GameTestFixtures.TEST_POS, ZombieFarmRegistrationAdapter.BLOCK.get());
        ZombieFarmBlockEntity zombie = helper.getBlockEntity(
                GameTestFixtures.TEST_POS, ZombieFarmBlockEntity.class);
        assertActivityStates(helper, "Zombie Farm", zombie, zombie.dataAccess(),
                zombie::processTick, zombie::toggleEnabled);

        helper.setBlock(GameTestFixtures.TEST_POS, RaiderFarmRegistrationAdapter.BLOCK.get());
        RaiderFarmBlockEntity raider = helper.getBlockEntity(
                GameTestFixtures.TEST_POS, RaiderFarmBlockEntity.class);
        assertActivityStates(helper, "Raider Farm", raider, raider.dataAccess(),
                raider::processTick, raider::toggleEnabled);

        helper.setBlock(GameTestFixtures.TEST_POS, CreeperFarmRegistrationAdapter.BLOCK.get());
        CreeperFarmBlockEntity creeper = helper.getBlockEntity(
                GameTestFixtures.TEST_POS, CreeperFarmBlockEntity.class);
        assertActivityStates(helper, "Creeper Farm", creeper, creeper.dataAccess(),
                creeper::processTick, creeper::toggleEnabled);
        helper.succeed();
    }

    private static void assertActivityStates(
            GameTestHelper helper,
            String label,
            Container farm,
            ContainerData data,
            Runnable processTick,
            Runnable toggleEnabled
    ) {
        processTick.run();
        helper.assertValueEqual(data.get(0), 0, label + " without worker");

        farm.setItem(0, GameTestFixtures.adultVillagerCapture(helper));
        processTick.run();
        helper.assertValueEqual(data.get(0), 0, label + " without sword");

        farm.setItem(1, new ItemStack(Items.WOODEN_SWORD));
        processTick.run();
        helper.assertTrue(data.get(0) > 0, label + " active progress");

        int pausedAt = data.get(0);
        toggleEnabled.run();
        processTick.run();
        helper.assertValueEqual(data.get(0), pausedAt, label + " paused progress");
    }

    private static void historicalMobFarmRealSidedTransfers(GameTestHelper helper) {
        for (FarmFixture fixture : List.of(
                new FarmFixture("Skeleton Farm", SkeletonFarmRegistrationAdapter.BLOCK.get()),
                new FarmFixture("Zombie Farm", ZombieFarmRegistrationAdapter.BLOCK.get()),
                new FarmFixture("Raider Farm", RaiderFarmRegistrationAdapter.BLOCK.get()),
                new FarmFixture("Creeper Farm", CreeperFarmRegistrationAdapter.BLOCK.get())
        )) {
            for (Direction side : List.of(
                    Direction.UP,
                    Direction.NORTH,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.EAST
            )) {
                helper.setBlock(GameTestFixtures.TEST_POS, fixture.block());
                Container farm = (Container) helper.getBlockEntity(
                        GameTestFixtures.TEST_POS, BlockEntity.class);
                ResourceHandler<ItemResource> handler = helper.requireCapability(
                        Capabilities.Item.BLOCK, GameTestFixtures.TEST_POS, side);
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertValueEqual(handler.insert(0,
                                    ItemResource.of(GameTestFixtures.adultVillagerCapture(helper)),
                                    1, transaction),
                            1, fixture.label() + " worker insertion from " + side);
                    helper.assertValueEqual(handler.insert(1, ItemResource.of(Items.WOODEN_SWORD),
                                    1, transaction),
                            1, fixture.label() + " sword insertion from " + side);
                    transaction.commit();
                }
                helper.assertTrue(!farm.getItem(0).isEmpty(),
                        fixture.label() + " inserted worker from " + side);
                helper.assertTrue(farm.getItem(1).is(Items.WOODEN_SWORD),
                        fixture.label() + " inserted sword from " + side);
                farm.clearContent();
            }

            helper.setBlock(GameTestFixtures.TEST_POS, fixture.block());
            BlockEntity farmEntity = helper.getBlockEntity(GameTestFixtures.TEST_POS, BlockEntity.class);
            ResourceHandler<ItemResource> bottom = helper.requireCapability(
                    Capabilities.Item.BLOCK, GameTestFixtures.TEST_POS, Direction.DOWN);
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(bottom.insert(ItemResource.of(Items.WOODEN_SWORD), 1, transaction),
                        0, fixture.label() + " rejects insertion from below");
            }
            BlockEntityStateFixtures.fillIndexedSlots(
                    helper, farmEntity, "Slot", 2, 1, new ItemStack(Items.ROTTEN_FLESH));
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(bottom.extract(ItemResource.of(Items.ROTTEN_FLESH),
                                1, transaction),
                        1, fixture.label() + " output extraction from below");
                transaction.commit();
            }
        }
        helper.succeed();
    }

    private record FarmFixture(String label, Block block) {
    }

    private static void raiderFarmVanillaSpecialDrops(GameTestHelper helper) {
        Identifier vindicatorId = Identifier.withDefaultNamespace("vindicator");
        helper.assertTrue(
                RaiderFarmTargetCatalog.defaultWeapon(vindicatorId).is(Items.IRON_AXE),
                "Vindicator equipment must be represented by an iron axe"
        );
        helper.assertTrue(
                RaiderFarmTargetCatalog.availableCategories(vindicatorId, false)
                        .contains(RaiderFarmLoot.WEAPONS),
                "Vindicator must expose the weapons filter"
        );

        Identifier witchId = RaiderFarmTargetCatalog.id(RaiderFarmKind.WITCH);
        var redstone = RaiderFarmLootAdapter.currentDynamicCycleDrop(
                witchId,
                new ItemStack(Items.REDSTONE),
                3,
                4
        ).orElseThrow();
        helper.assertValueEqual(redstone.probabilityPartsPerMillion(), 1_000_000,
                "Witch redstone probability");
        helper.assertValueEqual(redstone.minimumAmount(), 16, "Witch redstone minimum");
        helper.assertValueEqual(redstone.maximumAmount(), 44, "Witch redstone maximum");

        helper.setBlock(GameTestFixtures.TEST_POS, RaiderFarmRegistrationAdapter.BLOCK.get());
        RaiderFarmBlockEntity farm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                RaiderFarmBlockEntity.class
        );
        farm.setItem(RaiderFarmBlockEntity.WORKER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
        farm.setItem(RaiderFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        farm.dataAccess().set(3, 0);
        farm.dataAccess().set(0, farm.cycleDurationTicks() - 1);
        farm.processTick();
        boolean foundBottle = false;
        for (int slot = RaiderFarmBlockEntity.FIRST_OUTPUT_SLOT;
             slot < RaiderFarmBlockEntity.CONTAINER_SIZE;
             slot++) {
            foundBottle |= farm.getItem(slot).is(Items.OMINOUS_BOTTLE);
        }
        helper.assertTrue(foundBottle, "Pillager target must produce a vanilla ominous bottle");
        helper.succeed();
    }

    private static void mobFarmSelectionPersistence(GameTestHelper helper) {
        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);

        helper.setBlock(GameTestFixtures.TEST_POS, SkeletonFarmRegistrationAdapter.BLOCK.get());
        SkeletonFarmBlockEntity skeletonFarm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS, SkeletonFarmBlockEntity.class);
        skeletonFarm.selectTarget(SkeletonFarmTargetCatalog.id(SkeletonFarmKind.WITHER_SKELETON));
        preparePersistentState(skeletonFarm, skeletonFarm.dataAccess(), skeletonFarm::processTick,
                skeletonFarm::toggleEnabled, worker);
        assertExactReload(helper, skeletonFarm, SkeletonFarmBlockEntity.class, "Skeleton Farm");

        helper.setBlock(GameTestFixtures.TEST_POS, ZombieFarmRegistrationAdapter.BLOCK.get());
        ZombieFarmBlockEntity zombieFarm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS, ZombieFarmBlockEntity.class);
        zombieFarm.selectTarget(ZombieFarmTargetCatalog.id(ZombieFarmKind.ZOMBIFIED_PIGLIN));
        preparePersistentState(zombieFarm, zombieFarm.dataAccess(), zombieFarm::processTick,
                zombieFarm::toggleEnabled, worker);
        assertExactReload(helper, zombieFarm, ZombieFarmBlockEntity.class, "Zombie Farm");

        helper.setBlock(GameTestFixtures.TEST_POS, CreeperFarmRegistrationAdapter.BLOCK.get());
        CreeperFarmBlockEntity creeperFarm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                CreeperFarmBlockEntity.class
        );
        creeperFarm.selectTarget(CreeperFarmTargetCatalog.CHARGED_CREEPER_ID);
        preparePersistentState(creeperFarm, creeperFarm.dataAccess(), creeperFarm::processTick,
                creeperFarm::toggleEnabled, worker);
        CreeperFarmBlockEntity restoredCreeperFarm = reload(helper, creeperFarm, CreeperFarmBlockEntity.class);
        helper.assertTrue(
                CreeperFarmTargetCatalog.CHARGED_CREEPER_ID.equals(restoredCreeperFarm.selectedTargetId()),
                "Charged Creeper selection must survive a save/load round trip"
        );
        assertExactState(helper, creeperFarm, restoredCreeperFarm, "Creeper Farm");

        helper.setBlock(GameTestFixtures.TEST_POS, RaiderFarmRegistrationAdapter.BLOCK.get());
        RaiderFarmBlockEntity raiderFarm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                RaiderFarmBlockEntity.class
        );
        var evokerId = RaiderFarmTargetCatalog.id(RaiderFarmKind.EVOKER);
        raiderFarm.selectTarget(evokerId);
        preparePersistentState(raiderFarm, raiderFarm.dataAccess(), raiderFarm::processTick,
                raiderFarm::toggleEnabled, worker);
        RaiderFarmBlockEntity restoredRaiderFarm = reload(helper, raiderFarm, RaiderFarmBlockEntity.class);
        helper.assertTrue(
                evokerId.equals(restoredRaiderFarm.selectedTargetId()),
                "Raider selection must survive a save/load round trip"
        );
        assertExactState(helper, raiderFarm, restoredRaiderFarm, "Raider Farm");
        helper.succeed();
    }

    private static void preparePersistentState(
            Container farm,
            ContainerData data,
            Runnable processTick,
            Runnable toggleEnabled,
            ItemStack worker
    ) {
        farm.setItem(0, worker);
        farm.setItem(1, new ItemStack(Items.WOODEN_SWORD));
        data.set(3, 0);
        data.set(0, data.get(1) - 1);
        processTick.run();
        data.set(0, 17);
        toggleEnabled.run();
    }

    private static <T extends BlockEntity> void assertExactReload(
            GameTestHelper helper,
            T original,
            Class<T> expectedType,
            String label
    ) {
        T restored = reload(helper, original, expectedType);
        assertExactState(helper, original, restored, label);
    }

    private static void assertExactState(
            GameTestHelper helper,
            BlockEntity original,
            BlockEntity restored,
            String label
    ) {
        CompoundTag expected = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        CompoundTag actual = restored.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(expected.equals(actual), label + " changed exact persistent state after reload");
    }

    private static <T extends BlockEntity> T reload(
            GameTestHelper helper,
            T original,
            Class<T> expectedType
    ) {
        CompoundTag full = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity restored = BlockEntity.loadStatic(
                original.getBlockPos(),
                original.getBlockState(),
                full,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(expectedType.isInstance(restored),
                "Could not reload " + expectedType.getSimpleName());
        return expectedType.cast(restored);
    }

    private static void mobFarmOutputCapacity(GameTestHelper helper) {
        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);

        helper.setBlock(GameTestFixtures.TEST_POS, SkeletonFarmRegistrationAdapter.BLOCK.get());
        SkeletonFarmBlockEntity skeletonFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, SkeletonFarmBlockEntity.class);
        skeletonFarm.setItem(SkeletonFarmBlockEntity.WORKER_SLOT, worker);
        skeletonFarm.setItem(SkeletonFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        fillOutputs(
                helper,
                skeletonFarm,
                SkeletonFarmBlockEntity.FIRST_OUTPUT_SLOT,
                SkeletonFarmBlockEntity.OUTPUT_SLOT_COUNT
        );
        skeletonFarm.processTick();
        helper.assertValueEqual(skeletonFarm.cycleTicks(), 0, "Full Skeleton Farm progress");
        helper.assertValueEqual(skeletonFarm.dataAccess().get(4), 0, "Full Skeleton Farm XP");
        skeletonFarm.removeItem(SkeletonFarmBlockEntity.FIRST_OUTPUT_SLOT, 64);
        skeletonFarm.dataAccess().set(0, skeletonFarm.cycleDurationTicks() - 1);
        skeletonFarm.processTick();
        helper.assertTrue(skeletonFarm.dataAccess().get(4) > 0,
                "Skeleton Farm must resume as soon as one output has capacity");

        helper.setBlock(GameTestFixtures.TEST_POS, ZombieFarmRegistrationAdapter.BLOCK.get());
        ZombieFarmBlockEntity zombieFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, ZombieFarmBlockEntity.class);
        zombieFarm.setItem(ZombieFarmBlockEntity.WORKER_SLOT, worker);
        zombieFarm.setItem(ZombieFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        fillOutputs(
                helper,
                zombieFarm,
                ZombieFarmBlockEntity.FIRST_OUTPUT_SLOT,
                ZombieFarmBlockEntity.OUTPUT_SLOT_COUNT
        );
        zombieFarm.processTick();
        helper.assertValueEqual(zombieFarm.cycleTicks(), 0, "Full Zombie Farm progress");
        helper.assertValueEqual(zombieFarm.dataAccess().get(4), 0, "Full Zombie Farm XP");
        zombieFarm.removeItem(ZombieFarmBlockEntity.FIRST_OUTPUT_SLOT, 64);
        zombieFarm.dataAccess().set(0, zombieFarm.cycleDurationTicks() - 1);
        zombieFarm.processTick();
        helper.assertTrue(zombieFarm.dataAccess().get(4) > 0,
                "Zombie Farm must resume as soon as one output has capacity");

        helper.setBlock(GameTestFixtures.TEST_POS, RaiderFarmRegistrationAdapter.BLOCK.get());
        RaiderFarmBlockEntity raiderFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, RaiderFarmBlockEntity.class);
        raiderFarm.setItem(RaiderFarmBlockEntity.WORKER_SLOT, worker);
        raiderFarm.setItem(RaiderFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        fillOutputs(helper, raiderFarm, RaiderFarmBlockEntity.FIRST_OUTPUT_SLOT,
                RaiderFarmBlockEntity.OUTPUT_SLOT_COUNT);
        raiderFarm.processTick();
        helper.assertValueEqual(raiderFarm.cycleTicks(), 0, "Full Raider Farm progress");
        helper.assertValueEqual(raiderFarm.dataAccess().get(4), 0, "Full Raider Farm XP");
        raiderFarm.removeItem(RaiderFarmBlockEntity.FIRST_OUTPUT_SLOT, 64);
        raiderFarm.dataAccess().set(0, raiderFarm.cycleDurationTicks() - 1);
        raiderFarm.processTick();
        helper.assertTrue(raiderFarm.dataAccess().get(4) > 0,
                "Raider Farm must resume as soon as one output has capacity");

        helper.setBlock(GameTestFixtures.TEST_POS, CreeperFarmRegistrationAdapter.BLOCK.get());
        CreeperFarmBlockEntity creeperFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, CreeperFarmBlockEntity.class);
        creeperFarm.setItem(CreeperFarmBlockEntity.WORKER_SLOT, worker);
        creeperFarm.setItem(CreeperFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        fillOutputs(helper, creeperFarm, CreeperFarmBlockEntity.FIRST_OUTPUT_SLOT,
                CreeperFarmBlockEntity.OUTPUT_SLOT_COUNT);
        creeperFarm.processTick();
        helper.assertValueEqual(creeperFarm.cycleTicks(), 0, "Full Creeper Farm progress");
        helper.assertValueEqual(creeperFarm.dataAccess().get(4), 0, "Full Creeper Farm XP");
        creeperFarm.removeItem(CreeperFarmBlockEntity.FIRST_OUTPUT_SLOT, 64);
        creeperFarm.dataAccess().set(0, creeperFarm.cycleDurationTicks() - 1);
        creeperFarm.processTick();
        helper.assertTrue(creeperFarm.dataAccess().get(4) > 0,
                "Creeper Farm must resume as soon as one output has capacity");
        helper.succeed();
    }

    private static void fillOutputs(
            GameTestHelper helper,
            BlockEntity farm,
            int firstOutputSlot,
            int outputSlotCount
    ) {
        BlockEntityStateFixtures.fillIndexedSlots(
                helper,
                farm,
                "Slot",
                firstOutputSlot,
                outputSlotCount,
                new ItemStack(Items.COBBLESTONE, 64)
        );
    }

    private static void mobFarmXpOnlyCycles(GameTestHelper helper) {
        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);

        helper.setBlock(GameTestFixtures.TEST_POS, SkeletonFarmRegistrationAdapter.BLOCK.get());
        SkeletonFarmBlockEntity skeletonFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, SkeletonFarmBlockEntity.class);
        skeletonFarm.setItem(SkeletonFarmBlockEntity.WORKER_SLOT, worker);
        skeletonFarm.setItem(SkeletonFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        skeletonFarm.dataAccess().set(3, 0);
        skeletonFarm.dataAccess().set(0, skeletonFarm.cycleDurationTicks() - 1);
        skeletonFarm.processTick();
        helper.assertTrue(skeletonFarm.dataAccess().get(4) > 0,
                "Skeleton Farm must produce XP when every loot filter is disabled");
        int skeletonPausedAt = skeletonFarm.cycleTicks();
        skeletonFarm.toggleEnabled();
        skeletonFarm.processTick();
        helper.assertTrue(skeletonFarm.cycleTicks() == skeletonPausedAt,
                "Disabled Skeleton Farm must not advance");

        helper.setBlock(GameTestFixtures.TEST_POS, ZombieFarmRegistrationAdapter.BLOCK.get());
        ZombieFarmBlockEntity zombieFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, ZombieFarmBlockEntity.class);
        zombieFarm.setItem(ZombieFarmBlockEntity.WORKER_SLOT, worker);
        zombieFarm.setItem(ZombieFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        zombieFarm.dataAccess().set(3, 0);
        zombieFarm.dataAccess().set(0, zombieFarm.cycleDurationTicks() - 1);
        zombieFarm.processTick();
        helper.assertTrue(zombieFarm.dataAccess().get(4) > 0,
                "Zombie Farm must produce XP when every loot filter is disabled");
        int zombiePausedAt = zombieFarm.cycleTicks();
        zombieFarm.toggleEnabled();
        zombieFarm.processTick();
        helper.assertTrue(zombieFarm.cycleTicks() == zombiePausedAt,
                "Disabled Zombie Farm must not advance");

        helper.setBlock(GameTestFixtures.TEST_POS, RaiderFarmRegistrationAdapter.BLOCK.get());
        RaiderFarmBlockEntity raiderFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, RaiderFarmBlockEntity.class);
        raiderFarm.setItem(RaiderFarmBlockEntity.WORKER_SLOT, worker);
        raiderFarm.setItem(RaiderFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        raiderFarm.dataAccess().set(3, 0);
        raiderFarm.dataAccess().set(0, raiderFarm.cycleDurationTicks() - 1);
        raiderFarm.processTick();
        helper.assertTrue(raiderFarm.dataAccess().get(4) > 0,
                "Raider Farm must produce XP when every loot filter is disabled");
        int raiderPausedAt = raiderFarm.cycleTicks();
        raiderFarm.toggleEnabled();
        raiderFarm.processTick();
        helper.assertTrue(raiderFarm.cycleTicks() == raiderPausedAt,
                "Disabled Raider Farm must not advance");

        helper.setBlock(GameTestFixtures.TEST_POS, CreeperFarmRegistrationAdapter.BLOCK.get());
        CreeperFarmBlockEntity creeperFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, CreeperFarmBlockEntity.class);
        creeperFarm.setItem(CreeperFarmBlockEntity.WORKER_SLOT, worker);
        creeperFarm.setItem(CreeperFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        creeperFarm.dataAccess().set(3, 0);
        creeperFarm.dataAccess().set(0, creeperFarm.cycleDurationTicks() - 1);
        creeperFarm.processTick();
        helper.assertTrue(creeperFarm.dataAccess().get(4) > 0,
                "Creeper Farm must produce XP when every loot filter is disabled");
        int creeperPausedAt = creeperFarm.cycleTicks();
        creeperFarm.toggleEnabled();
        creeperFarm.processTick();
        helper.assertTrue(creeperFarm.cycleTicks() == creeperPausedAt,
                "Disabled Creeper Farm must not advance");
        helper.succeed();
    }
}
