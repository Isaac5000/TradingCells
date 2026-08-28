package com.cosmocraft.trading_cells.gametest.feature.silktouch;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SilkTouchTwoDropAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SilkTouchTwoMiningSpeedAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.PreservedSpawnerItemAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SpawnerRedstoneControl;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SpawnerRedstoneControlAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.TrialSpawnerExtension;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Behaviour-oriented GameTests for SilkTouchTwo. */
public final class SilkTouchTwoGameTests {
    private SilkTouchTwoGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("silk_touch_two_special_drops", 40, SilkTouchTwoGameTests::silkTouchTwoSpecialDrops),
            new GameTestCase("silk_touch_two_state_persistence", 60, SilkTouchTwoGameTests::silkTouchTwoStatePersistence),
            new GameTestCase("repeatable_trial_and_vault", 80, SilkTouchTwoGameTests::repeatableTrialAndVault)
        );
    }

    private static void silkTouchTwoSpecialDrops(GameTestHelper helper) {
        ItemStack pickaxe = enchantedTool(helper, Items.DIAMOND_PICKAXE, 2);
        ItemStack shovel = enchantedTool(helper, Items.DIAMOND_SHOVEL, 2);
        ItemStack axe = enchantedTool(helper, Items.DIAMOND_AXE, 2);

        assertSilkTouchTwoDrop(helper, Blocks.SPAWNER, pickaxe);
        assertSilkTouchTwoDrop(helper, Blocks.TRIAL_SPAWNER, pickaxe);
        assertSilkTouchTwoDrop(helper, Blocks.VAULT, pickaxe);
        assertSilkTouchTwoDrop(helper, Blocks.REINFORCED_DEEPSLATE, pickaxe);
        assertSilkTouchTwoDrop(helper, Blocks.BUDDING_AMETHYST, pickaxe);
        assertSilkTouchTwoDrop(helper, Blocks.SUSPICIOUS_SAND, shovel);
        assertSilkTouchTwoDrop(helper, Blocks.SUSPICIOUS_GRAVEL, shovel);
        assertSilkTouchTwoDrop(helper, Blocks.POWDER_SNOW, shovel);
        assertSilkTouchTwoDrop(helper, Blocks.FROGSPAWN, axe);
        assertSilkTouchTwoDrop(helper, Blocks.CAKE, axe);

        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.BUDDING_AMETHYST);
        helper.assertTrue(
                Block.getDrops(
                        Blocks.BUDDING_AMETHYST.defaultBlockState(),
                        helper.getLevel(),
                        helper.absolutePos(GameTestFixtures.TEST_POS),
                        null,
                        null,
                        enchantedTool(helper, Items.DIAMOND_PICKAXE, 1)
                ).isEmpty(),
                "Silk Touch I must not collect special blocks"
        );
        helper.assertTrue(
                Block.getDrops(
                        Blocks.BUDDING_AMETHYST.defaultBlockState(),
                        helper.getLevel(),
                        helper.absolutePos(GameTestFixtures.TEST_POS),
                        null,
                        null,
                        shovel
                ).isEmpty(),
                "A shovel must not collect pickaxe-only special blocks"
        );

        assertSpawnerDataRoundTrip(helper, pickaxe);
        assertSpecialMiningSpeedRequiresSilkTouchTwo(helper);
        assertSpawnerEggConversion(helper, pickaxe);
        assertAnvilKeepsSilkTouchTwo(helper);
        helper.succeed();
    }

    private static void assertAnvilKeepsSilkTouchTwo(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var silkTouch = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        AnvilMenu menu = new AnvilMenu(
                1,
                player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(GameTestFixtures.TEST_POS))
        );
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
        menu.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(silkTouch, 2)));
        helper.assertValueEqual(
                menu.getSlot(2).getItem().getEnchantmentLevel(silkTouch),
                2,
                "Anvil-applied Silk Touch level"
        );
    }

    private static void silkTouchTwoStatePersistence(GameTestHelper helper) {
        ItemStack shovel = enchantedTool(helper, Items.DIAMOND_SHOVEL, 2);
        ItemStack axe = enchantedTool(helper, Items.DIAMOND_AXE, 2);
        assertBrushableContentWithoutProgress(helper, Blocks.SUSPICIOUS_SAND, shovel);
        assertBrushableContentWithoutProgress(helper, Blocks.SUSPICIOUS_GRAVEL, shovel);
        assertCakeBitesRoundTrip(helper, axe);
        assertOminousVaultName(helper, enchantedTool(helper, Items.DIAMOND_PICKAXE, 2));
        helper.succeed();
    }

    private static void assertBrushableContentWithoutProgress(
            GameTestHelper helper,
            Block block,
            ItemStack shovel
    ) {
        helper.setBlock(GameTestFixtures.TEST_POS.below(), Blocks.STONE);
        helper.setBlock(GameTestFixtures.TEST_POS, block);
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        BrushableBlockEntity original = helper.getBlockEntity(GameTestFixtures.TEST_POS, BrushableBlockEntity.class);
        original.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, 42L);
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        original.brush(
                helper.getLevel().getGameTime(),
                helper.getLevel(),
                player,
                Direction.NORTH,
                new ItemStack(Items.BRUSH)
        );
        ItemStack expectedContent = original.getItem().copy();
        BlockState brushedState = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(
                brushedState.getValue(BlockStateProperties.DUSTED) > 0,
                "Brushable block must have progress before collection"
        );

        ItemStack drop = Block.getDrops(
                brushedState,
                helper.getLevel(),
                absolutePos,
                original,
                player,
                shovel
        ).getFirst();
        TypedEntityData<BlockEntityType<?>> data = drop.get(DataComponents.BLOCK_ENTITY_DATA);
        helper.assertTrue(data != null && data.contains("item"), "Brushable content must be retained");
        helper.assertTrue(data != null && !data.contains("hit_direction"), "Brush direction must not be retained");

        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        helper.placeAt(player, drop, GameTestFixtures.TEST_POS.below(), Direction.UP);
        BlockState restoredState = helper.getLevel().getBlockState(absolutePos);
        helper.assertValueEqual(
                restoredState.getValue(BlockStateProperties.DUSTED),
                0,
                "Brushable progress after Silk Touch II placement"
        );
        BrushableBlockEntity restored = helper.getBlockEntity(GameTestFixtures.TEST_POS, BrushableBlockEntity.class);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(restored.getItem(), expectedContent)
                        && restored.getItem().getCount() == expectedContent.getCount(),
                "Brushable content must remain unchanged"
        );
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
    }

    private static void assertCakeBitesRoundTrip(GameTestHelper helper, ItemStack axe) {
        helper.setBlock(GameTestFixtures.TEST_POS.below(), Blocks.STONE);
        BlockState eatenCake = Blocks.CAKE.defaultBlockState().setValue(CakeBlock.BITES, 4);
        helper.setBlock(GameTestFixtures.TEST_POS, eatenCake);
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        ItemStack drop = Block.getDrops(
                eatenCake,
                helper.getLevel(),
                absolutePos,
                null,
                null,
                axe
        ).getFirst();

        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        helper.placeAt(player, drop, GameTestFixtures.TEST_POS.below(), Direction.UP);
        helper.assertValueEqual(
                helper.getLevel().getBlockState(absolutePos).getValue(CakeBlock.BITES),
                4,
                "Cake bites after Silk Touch II placement"
        );
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
    }

    private static void assertOminousVaultName(GameTestHelper helper, ItemStack pickaxe) {
        BlockState ominousVault = Blocks.VAULT.defaultBlockState().setValue(VaultBlock.OMINOUS, true);
        helper.setBlock(GameTestFixtures.TEST_POS, ominousVault);
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        ItemStack drop = Block.getDrops(
                ominousVault,
                helper.getLevel(),
                absolutePos,
                helper.getLevel().getBlockEntity(absolutePos),
                null,
                pickaxe
        ).getFirst();
        helper.assertValueEqual(
                drop.get(DataComponents.CUSTOM_NAME),
                Component.translatable("item.trading_cells.ominous_vault", ominousVault.getBlock().getName()),
                "Ominous vault item name"
        );
    }

    private static void repeatableTrialAndVault(GameTestHelper helper) {
        assertSpawnerRedstonePersistence(helper);
        assertRepeatableTrialSpawner(helper);
        assertRepeatableVault(helper);
        helper.succeed();
    }

    private static void assertSpawnerRedstonePersistence(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.SPAWNER);
        SpawnerBlockEntity spawner = helper.getBlockEntity(GameTestFixtures.TEST_POS, SpawnerBlockEntity.class);
        SpawnerRedstoneControl control = SpawnerRedstoneControlAdapter.control(spawner).orElseThrow();
        control.tradingCells$setRedstoneControlInstalled(true);
        spawner.setChanged();

        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        ItemStack drop = Block.getDrops(
                spawner.getBlockState(),
                helper.getLevel(),
                absolutePos,
                spawner,
                null,
                enchantedTool(helper, Items.DIAMOND_PICKAXE, 2)
        ).getFirst();
        helper.assertTrue(SpawnerRedstoneControlAdapter.isInstalled(drop),
                "Collected spawner must retain redstone control");

        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        helper.setBlock(GameTestFixtures.TEST_POS.below(), Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        helper.placeAt(player, drop, GameTestFixtures.TEST_POS.below(), Direction.UP);
        SpawnerBlockEntity restored = helper.getBlockEntity(GameTestFixtures.TEST_POS, SpawnerBlockEntity.class);
        helper.assertTrue(
                SpawnerRedstoneControlAdapter.control(restored)
                        .orElseThrow()
                        .tradingCells$isRedstoneControlInstalled(),
                "Placed spawner must restore redstone control"
        );
    }

    private static void assertRepeatableTrialSpawner(GameTestHelper helper) {
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.TRIAL_SPAWNER);
        TrialSpawnerBlockEntity blockEntity = helper.getBlockEntity(GameTestFixtures.TEST_POS, TrialSpawnerBlockEntity.class);
        TrialSpawner trialSpawner = blockEntity.getTrialSpawner();
        trialSpawner.overrideEntityToSpawn(EntityTypes.ZOMBIE, helper.getLevel());
        trialSpawner.overridePeacefulAndMobSpawnRule();
        helper.assertTrue(trialSpawner.getPlayerDetector() == PlayerDetector.INCLUDING_CREATIVE_PLAYERS,
                "Trial spawners must detect creative players by distance");
        TrialSpawnerExtension extension = (TrialSpawnerExtension) (Object) trialSpawner;
        extension.tradingCells$setCompletedTrial(true);
        extension.tradingCells$setAwaitingPlayerExit(true);
        helper.getLevel().setBlock(
                absolutePos,
                helper.getLevel().getBlockState(absolutePos).setValue(TrialSpawnerBlock.STATE, TrialSpawnerState.WAITING_FOR_PLAYERS),
                3
        );

        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.snapTo(absolutePos.getX() + 1.5, absolutePos.getY(), absolutePos.getZ() + 0.5, 0.0F, 0.0F);
        trialSpawner.tickServer(helper.getLevel(), absolutePos, false);
        helper.assertValueEqual(blockEntity.getState(), TrialSpawnerState.WAITING_FOR_PLAYERS,
                "Trial must wait until players leave");

        helper.setBlock(GameTestFixtures.TEST_POS.relative(Direction.EAST), Blocks.REDSTONE_BLOCK);
        extension.tradingCells$setRedstoneControlInstalled(true);
        extension.tradingCells$setAwaitingPlayerExit(false);
        trialSpawner.tickServer(helper.getLevel(), absolutePos, false);
        helper.assertValueEqual(blockEntity.getState(), TrialSpawnerState.WAITING_FOR_PLAYERS,
                "Powered trial spawner must not restart");
        helper.setBlock(GameTestFixtures.TEST_POS.relative(Direction.EAST), Blocks.AIR);

        extension.tradingCells$setAwaitingPlayerExit(true);
        player.snapTo(absolutePos.getX() + 32.0, absolutePos.getY(), absolutePos.getZ(), 0.0F, 0.0F);
        trialSpawner.tickServer(helper.getLevel(), absolutePos, false);
        helper.assertTrue(!extension.tradingCells$isAwaitingPlayerExit(),
                "Leaving the trial radius must rearm the spawner");

        player.snapTo(absolutePos.getX() + 1.5, absolutePos.getY(), absolutePos.getZ() + 0.5, 0.0F, 0.0F);
        TrialSpawnerStateData.Packed reentryData = trialSpawner.getStateData().pack();
        trialSpawner.getStateData().apply(new TrialSpawnerStateData.Packed(
                Set.of(player.getUUID()),
                reentryData.currentMobs(),
                reentryData.cooldownEndsAt(),
                reentryData.nextMobSpawnsAt(),
                reentryData.totalMobsSpawned(),
                reentryData.nextSpawnData(),
                reentryData.ejectingLootTable()
        ));
        trialSpawner.tickServer(helper.getLevel(), absolutePos, false);
        helper.assertValueEqual(blockEntity.getState(), TrialSpawnerState.ACTIVE,
                "Re-entering must start the next trial");

        TrialSpawnerStateData.Packed beforePause = trialSpawner.getStateData().pack();
        player.snapTo(absolutePos.getX() + 32.0, absolutePos.getY(), absolutePos.getZ(), 0.0F, 0.0F);
        trialSpawner.tickServer(helper.getLevel(), absolutePos, false);
        TrialSpawnerStateData.Packed afterPause = trialSpawner.getStateData().pack();
        helper.assertValueEqual(afterPause.totalMobsSpawned(), beforePause.totalMobsSpawned(),
                "Suspended trial mob count");
        helper.assertValueEqual(blockEntity.getState(), TrialSpawnerState.ACTIVE,
                "Trial state while participants are away");

        var escaped = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(escaped != null, "Escaped trial zombie must be creatable");
        escaped.snapTo(absolutePos.getX() + 50.0, absolutePos.getY(), absolutePos.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(escaped);
        TrialSpawnerStateData.Packed packed = trialSpawner.getStateData().pack();
        trialSpawner.getStateData().apply(new TrialSpawnerStateData.Packed(
                packed.detectedPlayers(),
                Set.of(escaped.getUUID()),
                packed.cooldownEndsAt(),
                packed.nextMobSpawnsAt(),
                packed.totalMobsSpawned(),
                packed.nextSpawnData(),
                packed.ejectingLootTable()
        ));
        trialSpawner.tickServer(helper.getLevel(), absolutePos, false);
        helper.assertTrue(
                escaped.isRemoved() || !trialSpawner.getStateData().pack().currentMobs().contains(escaped.getUUID()),
                "Trial mobs outside the tracking area must be discarded or untracked"
        );

        helper.getLevel().setBlock(
                absolutePos,
                helper.getLevel().getBlockState(absolutePos)
                        .setValue(TrialSpawnerBlock.STATE, TrialSpawnerState.COOLDOWN)
                        .setValue(TrialSpawnerBlock.OMINOUS, true),
                3
        );
        trialSpawner.tickServer(helper.getLevel(), absolutePos, true);
        helper.assertValueEqual(blockEntity.getState(), TrialSpawnerState.WAITING_FOR_PLAYERS,
                "Completed trial rearm state");
        helper.assertTrue(extension.tradingCells$isAwaitingPlayerExit(),
                "Completed trial must require a fresh entry");
        helper.assertTrue(!helper.getLevel().getBlockState(absolutePos).getValue(TrialSpawnerBlock.OMINOUS),
                "Ominous state must reset after reward completion");

        BlockState collectedState = helper.getLevel().getBlockState(absolutePos)
                .setValue(TrialSpawnerBlock.OMINOUS, true);
        helper.getLevel().setBlock(absolutePos, collectedState, 3);
        ItemStack drop = Block.getDrops(
                collectedState,
                helper.getLevel(),
                absolutePos,
                blockEntity,
                null,
                enchantedTool(helper, Items.DIAMOND_PICKAXE, 2)
        ).getFirst();
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        helper.setBlock(GameTestFixtures.TEST_POS.below(), Blocks.STONE);
        var placingPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        placingPlayer.setItemInHand(InteractionHand.MAIN_HAND, drop);
        helper.placeAt(placingPlayer, drop, GameTestFixtures.TEST_POS.below(), Direction.UP);
        TrialSpawnerBlockEntity restored = helper.getBlockEntity(GameTestFixtures.TEST_POS, TrialSpawnerBlockEntity.class);
        TrialSpawnerExtension restoredExtension = (TrialSpawnerExtension) (Object) restored.getTrialSpawner();
        helper.assertTrue(restoredExtension.tradingCells$hasCompletedTrial(),
                "Collected trial spawner must retain completed state");
        helper.assertTrue(restoredExtension.tradingCells$isAwaitingPlayerExit(),
                "Collected trial spawner must retain rearm state");
        helper.assertTrue(restoredExtension.tradingCells$isRedstoneControlInstalled(),
                "Collected trial spawner must retain redstone control");
        helper.assertTrue(helper.getLevel().getBlockState(absolutePos).getValue(TrialSpawnerBlock.OMINOUS),
                "Collected trial spawner must retain ominous variant");
    }

    private static void assertRepeatableVault(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        BlockState activeVault = Blocks.VAULT.defaultBlockState().setValue(VaultBlock.STATE,
                net.minecraft.world.level.block.entity.vault.VaultState.ACTIVE);
        helper.setBlock(GameTestFixtures.TEST_POS, activeVault);
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        VaultBlockEntity vault = helper.getBlockEntity(GameTestFixtures.TEST_POS, VaultBlockEntity.class);
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack keys = new ItemStack(Items.TRIAL_KEY, 2);

        VaultBlockEntity.Server.tryInsertKey(
                helper.getLevel(), absolutePos, activeVault, vault.getConfig(),
                vault.getServerData(), vault.getSharedData(), player, keys
        );
        helper.assertValueEqual(keys.getCount(), 1, "First vault key consumption");
        helper.getLevel().setBlock(absolutePos, activeVault, 3);
        VaultBlockEntity.Server.tryInsertKey(
                helper.getLevel(), absolutePos, activeVault, vault.getConfig(),
                vault.getServerData(), vault.getSharedData(), player, keys
        );
        helper.assertTrue(keys.isEmpty(), "The same player must be able to reuse a vault");

        CompoundTag saved = vault.saveCustomOnly(helper.getLevel().registryAccess());
        CompoundTag serverData = saved.getCompound("server_data").orElseThrow();
        helper.assertTrue(!serverData.contains("rewarded_players"),
                "Vaults must not persist rewarded player UUIDs");
    }

    private static void assertSilkTouchTwoDrop(GameTestHelper helper, Block block, ItemStack tool) {
        helper.setBlock(GameTestFixtures.TEST_POS, block);
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolutePos);
        List<ItemStack> drops = Block.getDrops(
                block.defaultBlockState(),
                helper.getLevel(),
                absolutePos,
                blockEntity,
                null,
                tool
        );
        helper.assertValueEqual(drops.size(), 1, "Silk Touch II drop count for " + block);
        helper.assertTrue(drops.getFirst().is(block.asItem()), "Silk Touch II drop for " + block);
    }

    private static void assertSpawnerDataRoundTrip(GameTestHelper helper, ItemStack pickaxe) {
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.SPAWNER);
        BlockPos absolutePos = helper.absolutePos(GameTestFixtures.TEST_POS);
        SpawnerBlockEntity original = helper.getBlockEntity(GameTestFixtures.TEST_POS, SpawnerBlockEntity.class);
        original.setEntityId(EntityTypes.ZOMBIE, helper.getLevel().getRandom());
        CompoundTag expected = original.saveCustomOnly(helper.getLevel().registryAccess());

        ItemStack drop = Block.getDrops(
                Blocks.SPAWNER.defaultBlockState(),
                helper.getLevel(),
                absolutePos,
                original,
                null,
                pickaxe
        ).getFirst();
        TypedEntityData<BlockEntityType<?>> data = drop.get(DataComponents.BLOCK_ENTITY_DATA);
        helper.assertTrue(data != null && data.contains("SpawnData"), "Spawner drop must retain SpawnData");
        helper.assertTrue(
                SilkTouchTwoDropAdapter.hasPreservedDataMarker(drop),
                "Spawner drop must carry the protected placement marker"
        );

        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.AIR);
        helper.setBlock(GameTestFixtures.TEST_POS.below(), Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        helper.placeAt(player, drop, GameTestFixtures.TEST_POS.below(), Direction.UP);

        SpawnerBlockEntity restored = helper.getBlockEntity(GameTestFixtures.TEST_POS, SpawnerBlockEntity.class);
        helper.assertValueEqual(
                restored.saveCustomOnly(helper.getLevel().registryAccess()),
                expected,
                "Spawner data after Silk Touch II placement"
        );
    }

    private static void assertSpecialMiningSpeedRequiresSilkTouchTwo(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockState state = Blocks.SPAWNER.defaultBlockState();
        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                enchantedTool(helper, Items.DIAMOND_PICKAXE, 1)
        );
        PlayerEvent.BreakSpeed silkOne = new PlayerEvent.BreakSpeed(
                player,
                state,
                20.0F,
                helper.absolutePos(GameTestFixtures.TEST_POS)
        );
        SilkTouchTwoMiningSpeedAdapter.onBreakSpeed(silkOne);
        helper.assertValueEqual(silkOne.getNewSpeed(), 1.0F, "Silk Touch I spawner mining speed");

        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                enchantedTool(helper, Items.DIAMOND_PICKAXE, 2)
        );
        PlayerEvent.BreakSpeed silkTwo = new PlayerEvent.BreakSpeed(
                player,
                state,
                20.0F,
                helper.absolutePos(GameTestFixtures.TEST_POS)
        );
        SilkTouchTwoMiningSpeedAdapter.onBreakSpeed(silkTwo);
        helper.assertValueEqual(silkTwo.getNewSpeed(), 20.0F, "Silk Touch II spawner mining speed");
    }

    private static void assertSpawnerEggConversion(GameTestHelper helper, ItemStack pickaxe) {
        helper.setBlock(GameTestFixtures.TEST_POS, Blocks.SPAWNER);
        SpawnerBlockEntity plainSpawner = helper.getBlockEntity(GameTestFixtures.TEST_POS, SpawnerBlockEntity.class);
        plainSpawner.setEntityId(EntityTypes.ZOMBIE, helper.getLevel().getRandom());
        ItemStack plainDrop = SilkTouchTwoDropAdapter.createDrop(
                Blocks.SPAWNER.defaultBlockState(),
                pickaxe,
                helper.getLevel().registryAccess(),
                plainSpawner
        );
        ItemStack plainEgg = PreservedSpawnerItemAdapter.createSpawnEgg(helper.getLevel(), plainDrop)
                .orElseThrow();
        helper.assertTrue(plainEgg.is(Items.ZOMBIE_SPAWN_EGG), "Plain spawner must create its vanilla egg");
        helper.assertTrue(!PreservedSpawnerItemAdapter.isTrustedEgg(plainEgg),
                "Plain entities must not receive protected custom data");

        helper.assertTrue(
                plainSpawner.getSpawner().getOrCreateDisplayEntity(
                        helper.getLevel(),
                        helper.absolutePos(GameTestFixtures.TEST_POS)
                ) != null,
                "Configured spawner must expose a display entity before extraction"
        );
        ItemStack extractedEgg = PreservedSpawnerItemAdapter.extractSpawnEgg(helper.getLevel(), plainSpawner)
                .orElseThrow();
        helper.assertTrue(extractedEgg.is(Items.ZOMBIE_SPAWN_EGG),
                "Placed spawner must extract its configured spawn egg");
        helper.assertValueEqual(
                helper.getLevel().getBlockState(helper.absolutePos(GameTestFixtures.TEST_POS)).getBlock(),
                Blocks.SPAWNER,
                "Extracting an entity must keep the placed spawner"
        );
        helper.assertTrue(
                PreservedSpawnerItemAdapter.rootEntityData(
                        plainSpawner.saveCustomOnly(helper.getLevel().registryAccess())
                ).isEmpty(),
                "Placed spawner must be empty after extracting its entity"
        );
        helper.assertTrue(
                plainSpawner.getSpawner().getOrCreateDisplayEntity(
                        helper.getLevel(),
                        helper.absolutePos(GameTestFixtures.TEST_POS)
                ) == null,
                "Placed spawner preview must be empty after extracting its entity"
        );
        helper.assertTrue(
                PreservedSpawnerItemAdapter.extractSpawnEgg(helper.getLevel(), plainSpawner).isEmpty(),
                "An empty placed spawner must not duplicate its spawn egg"
        );
        plainSpawner.setEntityId(EntityTypes.ZOMBIE, helper.getLevel().getRandom());

        Hoglin mount = EntityTypes.HOGLIN.create(helper.getLevel(), EntitySpawnReason.LOAD);
        Skeleton rider = EntityTypes.SKELETON.create(helper.getLevel(), EntitySpawnReason.LOAD);
        Parrot passenger = EntityTypes.PARROT.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(mount != null && rider != null && passenger != null,
                "Spawner hierarchy entities must be creatable");
        rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        helper.assertTrue(rider.startRiding(mount, true, true), "Skeleton must mount the hoglin");
        helper.assertTrue(passenger.startRiding(rider, true, true), "Parrot must mount the skeleton");

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(mount.save(output), "Spawner hierarchy must serialize");
        CompoundTag fullSpawnerData = plainSpawner.saveWithFullMetadata(helper.getLevel().registryAccess());
        CompoundTag spawnData = fullSpawnerData.getCompound("SpawnData").orElseGet(CompoundTag::new);
        spawnData.put("entity", output.buildResult());
        fullSpawnerData.put("SpawnData", spawnData);
        BlockEntity loaded = BlockEntity.loadStatic(
                helper.absolutePos(GameTestFixtures.TEST_POS),
                Blocks.SPAWNER.defaultBlockState(),
                fullSpawnerData,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(loaded instanceof SpawnerBlockEntity, "Modified spawner must reload");

        ItemStack modifiedDrop = SilkTouchTwoDropAdapter.createDrop(
                Blocks.SPAWNER.defaultBlockState(),
                pickaxe,
                helper.getLevel().registryAccess(),
                loaded
        );
        ItemStack modifiedEgg = PreservedSpawnerItemAdapter.createSpawnEgg(helper.getLevel(), modifiedDrop)
                .orElseThrow();
        helper.assertTrue(modifiedEgg.is(Items.SKELETON_SPAWN_EGG),
                "The first armor-capable rider must define the egg");
        helper.assertTrue(PreservedSpawnerItemAdapter.isTrustedEgg(modifiedEgg),
                "Modified hierarchy egg must be trusted");
        helper.assertTrue(modifiedEgg.hasFoil(), "Modified hierarchy egg must have a glint");

        var description = PreservedSpawnerItemAdapter.describe(helper.getLevel(), modifiedEgg).orElseThrow();
        helper.assertValueEqual(description.selected().getType(), EntityTypes.SKELETON,
                "Selected hierarchy entity");
        helper.assertValueEqual(description.mount(), EntityTypes.HOGLIN, "Selected entity mount");
        helper.assertTrue(description.passengers().contains(EntityTypes.PARROT),
                "Selected entity passengers must be preserved");
        helper.assertTrue(
                description.selected() instanceof Skeleton selected
                        && selected.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET),
                "Selected entity armor must be preserved"
        );

        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack eggToSpawn = modifiedEgg.copy();
        helper.assertValueEqual(
                PreservedSpawnerItemAdapter.spawnTrustedEgg(
                        player,
                        eggToSpawn,
                        helper.getLevel(),
                        helper.absolutePos(GameTestFixtures.TEST_POS.above()),
                        false,
                        false
                ),
                net.minecraft.world.InteractionResult.SUCCESS,
                "Modified hierarchy spawn result"
        );
        helper.assertTrue(eggToSpawn.isEmpty(), "Spawning the hierarchy must consume exactly one egg");
    }

    private static ItemStack enchantedTool(GameTestHelper helper, Item item, int silkTouchLevel) {
        ItemStack tool = new ItemStack(item);
        var silkTouch = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(silkTouch, silkTouchLevel);
        EnchantmentHelper.setEnchantments(tool, enchantments.toImmutable());
        return tool;
    }
}
