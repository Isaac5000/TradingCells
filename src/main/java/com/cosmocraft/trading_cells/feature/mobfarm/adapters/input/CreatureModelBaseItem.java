package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import net.minecraft.world.item.Item;

public final class CreatureModelBaseItem extends Item {
    private final EssenceTier tier;
    public CreatureModelBaseItem(Properties properties, EssenceTier tier) { super(properties); this.tier = tier; }
    public EssenceTier tier() { return tier; }
}
