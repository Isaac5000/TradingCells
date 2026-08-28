package com.cosmocraft.trading_cells.gametest.feature.infusion;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Behaviour-oriented GameTests for ArcaneInfuser. */
public final class ArcaneInfuserGameTests {
    private ArcaneInfuserGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("manual_infuser_contract", 20,
                    ArcaneInfuserGameTests::manualInfuserContract)
        );
    }

    private static void manualInfuserContract(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, ArcaneInfuserRegistrationAdapter.BLOCK.get());
        for (var side : net.minecraft.core.Direction.values()) {
            ResourceHandler<ItemResource> handler = helper.requireCapability(
                    Capabilities.Item.BLOCK,
                    GameTestFixtures.TEST_POS,
                    side
            );
            helper.assertValueEqual(handler.size(), 0, "Automated Infuser slots on " + side);
        }
        helper.succeed();
    }
}
