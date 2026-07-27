package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerType;

/** Resolves the visual theme exclusively from VillagerData without changing layout. */
public final class DefaultVillagerGuiThemeResolver implements VillagerGuiThemeResolver {
    public static final DefaultVillagerGuiThemeResolver INSTANCE = new DefaultVillagerGuiThemeResolver();

    private DefaultVillagerGuiThemeResolver() {
    }

    @Override
    public VillagerGuiTheme resolveTheme(VillagerData villagerData) {
        if (villagerData == null) {
            return VillagerGuiTheme.DEFAULT;
        }
        Optional<ResourceKey<VillagerType>> key = villagerData.type().unwrapKey();
        if (key.isEmpty()) {
            return VillagerGuiTheme.DEFAULT;
        }
        ResourceKey<VillagerType> type = key.get();
        if (type.equals(VillagerType.PLAINS)) {
            return VillagerGuiTheme.PLAINS;
        }
        if (type.equals(VillagerType.DESERT)) {
            return VillagerGuiTheme.DESERT;
        }
        if (type.equals(VillagerType.SAVANNA)) {
            return VillagerGuiTheme.SAVANNA;
        }
        if (type.equals(VillagerType.TAIGA)) {
            return VillagerGuiTheme.TAIGA;
        }
        if (type.equals(VillagerType.SNOW)) {
            return VillagerGuiTheme.SNOW;
        }
        if (type.equals(VillagerType.SWAMP)) {
            return VillagerGuiTheme.SWAMP;
        }
        if (type.equals(VillagerType.JUNGLE)) {
            return VillagerGuiTheme.JUNGLE;
        }
        return VillagerGuiTheme.DEFAULT;
    }
}
