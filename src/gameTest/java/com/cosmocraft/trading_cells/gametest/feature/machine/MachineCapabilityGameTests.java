package com.cosmocraft.trading_cells.gametest.feature.machine;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.output.QuarryRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Behaviour-oriented GameTests for MachineCapability. */
public final class MachineCapabilityGameTests {
    private MachineCapabilityGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("sided_capability_contracts", 20,
                    MachineCapabilityGameTests::sidedCapabilityContracts)
        );
    }

    private static void sidedCapabilityContracts(GameTestHelper helper) {
        for (Block block : List.of(
                QuarryRegistrationAdapter.QUARRY_BLOCK.get(),
                QuarryRegistrationAdapter.PIGLIN_QUARRY_BLOCK.get()
        )) {
            helper.setBlock(GameTestFixtures.TEST_POS, block);
            QuarryBlockEntity quarry = helper.getBlockEntity(GameTestFixtures.TEST_POS, QuarryBlockEntity.class);
            for (Direction side : Direction.values()) {
                ResourceHandler<ItemResource> handler = helper.requireCapability(
                        Capabilities.Item.BLOCK,
                        GameTestFixtures.TEST_POS,
                        side
                );
                helper.assertValueEqual(
                        handler.size(),
                        quarry.getSlotsForFace(side).length,
                        "Quarry item slots on " + side
                );
            }
        }

        for (Block block : List.of(
                ExperienceStorageRegistrationAdapter.BLOCK.get(),
                ArcaneInfuserRegistrationAdapter.BLOCK.get(),
                TraderRegistrationAdapter.VILLAGER_TRADER_BLOCK.get(),
                AutotraderRegistrationAdapter.AUTOTRADER_BLOCK.get()
        )) {
            helper.setBlock(GameTestFixtures.TEST_POS, block);
            for (Direction side : Direction.values()) {
                ResourceHandler<FluidResource> handler = helper.requireCapability(
                        Capabilities.Fluid.BLOCK,
                        GameTestFixtures.TEST_POS,
                        side
                );
                helper.assertValueEqual(handler.size(), 1, "XP fluid tank count on " + side);
            }
        }
        helper.succeed();
    }
}
