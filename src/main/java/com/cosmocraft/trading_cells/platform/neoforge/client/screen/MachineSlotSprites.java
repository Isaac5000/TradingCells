package com.cosmocraft.trading_cells.platform.neoforge.client.screen;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.resources.Identifier;

/** Shared empty-slot sprites used by machine screens. */
public final class MachineSlotSprites {
    public static final Identifier VILLAGER_HEAD = sprite("shared/slots/empty_villager_head");
    public static final Identifier PIGLIN_HEAD = sprite("shared/slots/empty_piglin_head");
    public static final Identifier CAPTURER = sprite("shared/slots/empty_capturer");
    public static final Identifier HOE = sprite("shared/slots/empty_hoe");
    public static final Identifier WHEAT_SEEDS = sprite("farmer/slots/empty_wheat_seeds");
    public static final Identifier WEAKNESS_POTION = sprite("shared/slots/empty_potion");
    public static final Identifier GOLDEN_APPLE = sprite("converter/slots/empty_apple");
    public static final Identifier BREAD = sprite("villager_breeder/slots/empty_bread");
    public static final Identifier PORKCHOP = sprite("piglin_breeder/slots/empty_porkchop");

    private MachineSlotSprites() {
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
    }
}
