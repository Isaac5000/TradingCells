package com.cosmocraft.trading_cells.gametest.feature.ironfarm;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.output.IronFarmRegistrationAdapter;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Behaviour-oriented GameTests for IronFarm. */
public final class IronFarmGameTests {
    private IronFarmGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("iron_farm_partial_output_capacity", 20,
                    IronFarmGameTests::ironFarmPartialOutputCapacity)
        );
    }

    private static void ironFarmPartialOutputCapacity(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, IronFarmRegistrationAdapter.IRON_FARM_BLOCK.get());
        IronFarmBlockEntity ironFarm = helper.getBlockEntity(GameTestFixtures.TEST_POS, IronFarmBlockEntity.class);
        ironFarm.setItem(IronFarmBlockEntity.FIRST_VILLAGER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
        ironFarm.dataAccess().set(1, 0);
        prepareIronFarmOutputs(ironFarm, 63, 0);
        ironFarm.dataAccess().set(0, ironFarm.dataAccess().get(3) - 1);

        ironFarm.processTick();

        helper.assertValueEqual(
                ironFarm.getItem(IronFarmBlockEntity.FIRST_OUTPUT_SLOT).getCount(),
                64,
                "Iron Farm must fill partial iron capacity and discard only the overflow"
        );
        ironFarm.processTick();
        helper.assertValueEqual(
                ironFarm.cycleTicks(),
                0,
                "Iron Farm must pause once no active output has capacity"
        );

        prepareIronFarmOutputs(ironFarm, 64, 63);
        ironFarm.dataAccess().set(1, 1);
        ironFarm.processTick();
        helper.assertValueEqual(
                ironFarm.cycleTicks(),
                1,
                "Iron Farm must run while poppies still have capacity"
        );
        helper.succeed();
    }

    private static void prepareIronFarmOutputs(IronFarmBlockEntity ironFarm, int ironCount, int poppyCount) {
        try {
            var itemsField = IronFarmBlockEntity.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            List<ItemStack> items = (List<ItemStack>) itemsField.get(ironFarm);
            items.set(IronFarmBlockEntity.FIRST_OUTPUT_SLOT, new ItemStack(Items.IRON_INGOT, ironCount));
            int firstBlockedSlot = IronFarmBlockEntity.FIRST_OUTPUT_SLOT + 1;
            if (poppyCount > 0) {
                items.set(firstBlockedSlot, new ItemStack(Items.POPPY, poppyCount));
                firstBlockedSlot++;
            }
            for (int slot = firstBlockedSlot; slot < IronFarmBlockEntity.CONTAINER_SIZE; slot++) {
                items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not prepare Iron Farm outputs", exception);
        }
    }
}
