package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
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
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.output.ZombieFarmRegistrationAdapter;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

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
            new GameTestCase("raider_farm_vanilla_special_drops", 20,
                    MobFarmGameTests::raiderFarmVanillaSpecialDrops)
        );
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
        helper.setBlock(GameTestFixtures.TEST_POS, CreeperFarmRegistrationAdapter.BLOCK.get());
        CreeperFarmBlockEntity creeperFarm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                CreeperFarmBlockEntity.class
        );
        creeperFarm.selectTarget(CreeperFarmTargetCatalog.CHARGED_CREEPER_ID);
        creeperFarm.dataAccess().set(3, 0);
        CreeperFarmBlockEntity restoredCreeperFarm = reload(helper, creeperFarm, CreeperFarmBlockEntity.class);
        helper.assertTrue(
                CreeperFarmTargetCatalog.CHARGED_CREEPER_ID.equals(restoredCreeperFarm.selectedTargetId()),
                "Charged Creeper selection must survive a save/load round trip"
        );
        helper.assertValueEqual(restoredCreeperFarm.dataAccess().get(3), 0,
                "Creeper Farm loot filters must survive a save/load round trip");

        helper.setBlock(GameTestFixtures.TEST_POS, RaiderFarmRegistrationAdapter.BLOCK.get());
        RaiderFarmBlockEntity raiderFarm = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                RaiderFarmBlockEntity.class
        );
        var evokerId = RaiderFarmTargetCatalog.id(RaiderFarmKind.EVOKER);
        raiderFarm.selectTarget(evokerId);
        raiderFarm.dataAccess().set(3, 0);
        RaiderFarmBlockEntity restoredRaiderFarm = reload(helper, raiderFarm, RaiderFarmBlockEntity.class);
        helper.assertTrue(
                evokerId.equals(restoredRaiderFarm.selectedTargetId()),
                "Raider selection must survive a save/load round trip"
        );
        helper.assertValueEqual(restoredRaiderFarm.dataAccess().get(3), 0,
                "Raider Farm loot filters must survive a save/load round trip");
        helper.succeed();
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
        fillOutputs(raiderFarm, RaiderFarmBlockEntity.FIRST_OUTPUT_SLOT, RaiderFarmBlockEntity.OUTPUT_SLOT_COUNT);
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
        fillOutputs(creeperFarm, CreeperFarmBlockEntity.FIRST_OUTPUT_SLOT, CreeperFarmBlockEntity.OUTPUT_SLOT_COUNT);
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

    private static void fillOutputs(Object farm, int firstOutputSlot, int outputSlotCount) {
        try {
            var field = farm.getClass().getDeclaredField("items");
            field.setAccessible(true);
            List<ItemStack> items = (List<ItemStack>) field.get(farm);
            for (int slot = firstOutputSlot; slot < firstOutputSlot + outputSlotCount; slot++) {
                items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not prepare full mob-farm outputs", exception);
        }
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
