package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.resources.Identifier;

/** Shared empty-slot sprites used by machine screens. */
public final class MachineSlotSprites {
    public static final Identifier VILLAGER_HEAD = sprite("captures/empty_villager_head");
    public static final Identifier PIGLIN_HEAD = sprite("captures/empty_piglin_head");
    public static final Identifier CAPTURER = sprite("captures/empty_capturer");
    public static final Identifier HOE = sprite("machines/slots/empty_hoe");
    public static final Identifier WHEAT_SEEDS = sprite("machines/slots/empty_wheat_seeds");
    public static final Identifier WEAKNESS_POTION = sprite("machines/slots/empty_potion");
    public static final Identifier GOLDEN_APPLE = sprite("machines/slots/empty_apple");
    public static final Identifier BREAD = sprite("machines/slots/empty_bread");
    public static final Identifier PORKCHOP = sprite("machines/slots/empty_porkchop");

    private MachineSlotSprites() {
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
    }
}
