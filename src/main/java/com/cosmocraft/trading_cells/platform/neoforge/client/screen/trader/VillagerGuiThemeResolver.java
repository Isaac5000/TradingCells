package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import net.minecraft.world.entity.npc.villager.VillagerData;

@FunctionalInterface
public interface VillagerGuiThemeResolver {
    VillagerGuiTheme resolveTheme(VillagerData villagerData);
}
