package com.cosmocraft.trading_cells.gametest;

import com.cosmocraft.trading_cells.gametest.feature.silktouch.SilkTouchTwoGameTests;
import com.cosmocraft.trading_cells.gametest.feature.configuredmobfarm.ConfiguredMobFarmGameTests;
import com.cosmocraft.trading_cells.gametest.feature.farmer.FarmerGameTests;
import com.cosmocraft.trading_cells.gametest.feature.machine.PortableMachineGameTests;
import com.cosmocraft.trading_cells.gametest.feature.machine.MachineCapabilityGameTests;
import com.cosmocraft.trading_cells.gametest.feature.ironfarm.IronFarmGameTests;
import com.cosmocraft.trading_cells.gametest.feature.quarry.QuarryGameTests;
import com.cosmocraft.trading_cells.gametest.feature.mobfarm.MobFarmGameTests;
import com.cosmocraft.trading_cells.gametest.feature.experience.ExperienceGameTests;
import com.cosmocraft.trading_cells.gametest.feature.infusion.ArcaneInfuserGameTests;
import com.cosmocraft.trading_cells.gametest.feature.network.NetworkGameTests;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityGameTests;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers feature-oriented server integration tests outside the published mod JAR. */
@EventBusSubscriber(modid = TradingCellsGameTests.TEST_MOD_ID)
public final class TradingCellsGameTests {
    static final String TEST_MOD_ID = "trading_cells_gametest";
    private static final List<GameTestCase> TESTS = collectTests();

    private TradingCellsGameTests() {
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        for (GameTestCase test : TESTS) {
            event.register(Registries.TEST_FUNCTION, id(test.name()), test::function);
        }
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("empty_environment"),
                new TestEnvironmentDefinition.AllOf()
        );
        for (GameTestCase test : TESTS) {
            registerTest(event, environment, test);
        }
    }

    private static List<GameTestCase> collectTests() {
        List<GameTestCase> tests = new ArrayList<>();
        tests.addAll(SilkTouchTwoGameTests.tests());
        tests.addAll(FarmerGameTests.tests());
        tests.addAll(PortableMachineGameTests.tests());
        tests.addAll(MachineCapabilityGameTests.tests());
        tests.addAll(IronFarmGameTests.tests());
        tests.addAll(QuarryGameTests.tests());
        tests.addAll(MobFarmGameTests.tests());
        tests.addAll(ConfiguredMobFarmGameTests.tests());
        tests.addAll(ExperienceGameTests.tests());
        tests.addAll(ArcaneInfuserGameTests.tests());
        tests.addAll(NetworkGameTests.tests());
        tests.addAll(BlockEntityGameTests.tests());
        return List.copyOf(tests);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            GameTestCase test
    ) {
        Identifier identifier = id(test.name());
        ResourceKey<Consumer<GameTestHelper>> functionKey = ResourceKey.create(
                Registries.TEST_FUNCTION,
                identifier
        );
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.fromNamespaceAndPath("minecraft", "empty"),
                test.maxTicks(),
                0,
                true
        );
        event.registerTest(identifier, new FunctionGameTestInstance(functionKey, data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
    }
}
