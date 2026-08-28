package com.cosmocraft.trading_cells.feature.combat.adapters.api;

import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import net.minecraft.world.item.Item;

/** Public item boundary for features that consume combat rewards. */
public final class CombatItems {
    private CombatItems() {
    }

    public static Item stormShard() {
        return CombatRegistrationAdapter.STORM_SHARD_ITEM.get();
    }
}
