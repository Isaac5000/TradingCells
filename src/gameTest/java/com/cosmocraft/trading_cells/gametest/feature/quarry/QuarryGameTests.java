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

/** Behaviour-oriented GameTests for Quarry. */
public final class QuarryGameTests {
    private QuarryGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("quarry_partial_output_capacity", 20,
                    QuarryGameTests::quarryPartialOutputCapacity)
        );
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
                 slot < QuarryBlockEntity.CONTAINER_SIZE;
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
