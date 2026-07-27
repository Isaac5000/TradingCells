package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.resources.Identifier;

/** Neutral background shared by both villager trading screens. */
public final class VillagerGuiTextures {
    public static final Identifier DEFAULT = texture("default");

    private VillagerGuiTextures() {
    }

    public static Identifier resolve() {
        return DEFAULT;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(
                TradingCells.MOD_ID,
                "textures/gui/trader/backgrounds/" + name + ".png"
        );
    }
}
