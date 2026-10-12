package com.cosmocraft.trading_cells.gametest.feature.quarry;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;

/** Behaviour-oriented GameTests for Quarry. */
public final class QuarryGameTests {
    private QuarryGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("quarry_partial_output_capacity", 20,
                    QuarryGameTests::quarryPartialOutputCapacity),
            new GameTestCase("quarry_filters_preserve_roll_and_pause", 40, helper -> filters(helper, false)),
            new GameTestCase("piglin_quarry_filters_preserve_roll_and_pause", 40, helper -> filters(helper, true))
        );
    }

    private static void filters(GameTestHelper helper, boolean piglin) {
        helper.setBlock(GameTestFixtures.TEST_POS, piglin ? QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK.get()
                : QuarryRegistrationAdapter.QUARRY_BLOCK.get());
        var quarry = helper.getBlockEntity(GameTestFixtures.TEST_POS, QuarryBlockEntity.class);
        quarry.setItem(0, piglin ? GameTestFixtures.adultPiglinCapture(helper) : GameTestFixtures.adultVillagerCapture(helper));
        quarry.setItem(1, new ItemStack(Items.DIAMOND_PICKAXE));
        var catalog = quarry.catalogSnapshot();
        helper.assertTrue(quarry.enabled() && !quarry.whitelist(), "Legacy defaults are enabled and blacklist");
        quarry.processTick();
        CompoundTag rolled = quarry.saveWithFullMetadata(helper.getLevel().registryAccess());
        Identifier selected = Identifier.parse(rolled.getStringOr("PendingMaterial", ""));
        helper.assertTrue(quarry.toggleMaterial(selected) && !quarry.collects(selected), "Blacklist excludes marked material");
        helper.assertTrue(quarry.catalogSnapshot() == catalog, "Filtering never rebuilds weighted selection");
        helper.assertFalse(quarry.toggleMaterial(Identifier.parse("external_test:missing")), "Unknown material rejected");
        quarry.toggleEnabled();
        CompoundTag paused = quarry.saveWithFullMetadata(helper.getLevel().registryAccess());
        reload(helper, quarry, paused);
        quarry.processTick();
        helper.assertValueEqual(quarry.dataAccess().get(0), rolled.getIntOr("CycleTicks", -1), "Pause survives reload without advancing");
        helper.assertTrue(!quarry.enabled() && !quarry.collects(selected), "Filter and pause persist");
        helper.assertValueEqual(quarry.saveWithFullMetadata(helper.getLevel().registryAccess()).getStringOr("PendingMaterial", ""),
                selected.toString(), "Pausing keeps the selected material");
        quarry.toggleEnabled();
        BlockEntityStateFixtures.fillIndexedSlots(helper, quarry, "Slot", 3, 27, new ItemStack(Items.COBBLESTONE, 64));
        int damage = quarry.getItem(1).getDamageValue();
        quarry.processTick();
        helper.assertValueEqual(quarry.dataAccess().get(0), rolled.getIntOr("CycleTicks", 0) + 1,
                "Discarded material still advances with full outputs");
        quarry.dataAccess().set(0, quarry.dataAccess().get(1) - 1);
        quarry.processTick();
        helper.assertValueEqual(quarry.dataAccess().get(0), 0, "Discarded cycle completes normally");
        helper.assertValueEqual(quarry.getItem(1).getDamageValue(), damage + 1, "Discarded cycle charges tool wear");
        helper.assertTrue(quarry.lastResult().isEmpty(), "Discarded cycle produces no result");
        helper.assertValueEqual(quarry.getItem(3).getCount(), 64, "Existing outputs unchanged");

        // Restore the same roll, then change only the filter and storage capacity.
        reload(helper, quarry, rolled);
        quarry.toggleFilterMode();
        helper.assertFalse(quarry.collects(selected), "Empty whitelist collects nothing");
        quarry.toggleMaterial(selected);
        helper.assertTrue(quarry.collects(selected), "Whitelist collects marked material");
        var allowed = quarry.saveWithFullMetadata(helper.getLevel().registryAccess());
        reload(helper, quarry, allowed);
        helper.assertTrue(quarry.whitelist() && quarry.collects(selected), "Whitelist survives reload");
        BlockEntityStateFixtures.fillIndexedSlots(helper, quarry, "Slot", 3, 27, new ItemStack(Items.COBBLESTONE, 64));
        quarry.dataAccess().set(0, quarry.dataAccess().get(1) - 1);
        int before = quarry.getItem(1).getDamageValue();
        quarry.processTick();
        helper.assertValueEqual(quarry.getItem(1).getDamageValue(), before, "Blocked output charges no wear");
        helper.assertValueEqual(quarry.saveWithFullMetadata(helper.getLevel().registryAccess()).getStringOr("PendingMaterial", ""),
                selected.toString(), "Full output never rerolls");
        quarry.removeItem(3, 64);
        quarry.processTick();
        helper.assertFalse(quarry.getItem(3).isEmpty(), "Selected result reaches freed output");
        helper.assertValueEqual(quarry.getItem(1).getDamageValue(), before + 1, "Accepted cycle charges once");
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(quarry.getBlockPos()));
        var menu = (com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu)
                quarry.createMenu(12, player.getInventory(), player);
        menu.broadcastChanges();
        menu.toggleMaterial(player, menu.filterRevision() - 1, selected);
        helper.assertTrue(quarry.collects(selected), "Stale filter edit rejected");
        menu.toggleMaterial(player, menu.filterRevision(), Identifier.parse("external_test:missing"));
        helper.assertTrue(quarry.collects(selected), "Unknown network selection rejected");
        menu.toggleMaterial(player, menu.filterRevision(), selected);
        helper.assertFalse(quarry.collects(selected), "Current menu edit accepted");
        menu.toggleMaterial(player, menu.filterRevision(), selected);
        quarry.toggleEnabled();
        quarry.prepareForBlockDrop(helper.getLevel().registryAccess());
        var dropped = quarry.getPreparedBlockDropData(helper.getLevel().registryAccess());
        reload(helper, quarry, dropped);
        helper.assertTrue(!quarry.enabled() && quarry.whitelist() && quarry.collects(selected),
                "Portable block snapshot retains pause and filter");
        helper.succeed();
    }

    private static void reload(GameTestHelper helper, QuarryBlockEntity quarry, CompoundTag state) {
        quarry.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), state));
    }

    private static void quarryPartialOutputCapacity(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, QuarryRegistrationAdapter.QUARRY_BLOCK.get());
        QuarryBlockEntity quarry = helper.getBlockEntity(GameTestFixtures.TEST_POS, QuarryBlockEntity.class);
        quarry.setItem(QuarryBlockEntity.WORKER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
        quarry.setItem(QuarryBlockEntity.PICKAXE_SLOT, new ItemStack(Items.WOODEN_PICKAXE));
        prepareQuarryPartialOutput(quarry);
        quarry.dataAccess().set(0, quarry.dataAccess().get(1) - 1);

        quarry.processTick();

        helper.assertValueEqual(
                quarry.getItem(QuarryBlockEntity.FIRST_OUTPUT_SLOT).getCount(),
                64,
                "Quarry must fill the remaining compatible capacity and discard only the overflow"
        );
        quarry.processTick();
        helper.assertValueEqual(
                quarry.dataAccess().get(0),
                0,
                "Quarry must pause the next cycle once no output has capacity"
        );
        helper.succeed();
    }

    private static void prepareQuarryPartialOutput(QuarryBlockEntity quarry) {
        try {
            var itemsField = QuarryBlockEntity.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            List<ItemStack> items = (List<ItemStack>) itemsField.get(quarry);
            items.set(QuarryBlockEntity.FIRST_OUTPUT_SLOT, new ItemStack(Items.REDSTONE, 63));
            for (int slot = QuarryBlockEntity.FIRST_OUTPUT_SLOT + 1;
                 slot < QuarryBlockEntity.FIRST_OUTPUT_SLOT + QuarryBlockEntity.OUTPUT_SLOT_COUNT;
                 slot++) {
                items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
            }

            var pendingField = QuarryBlockEntity.class.getDeclaredField("pendingResult");
            pendingField.setAccessible(true);
            pendingField.set(quarry, List.of(new ItemStack(Items.REDSTONE, 8)));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not prepare Quarry outputs", exception);
        }
    }
}
