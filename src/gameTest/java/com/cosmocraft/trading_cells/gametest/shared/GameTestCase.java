package com.cosmocraft.trading_cells.gametest.shared;

import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** Stable registration contract shared by feature-oriented GameTest suites. */
public record GameTestCase(String name, int maxTicks, Consumer<GameTestHelper> function) {
}