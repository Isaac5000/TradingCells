package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import me.shedaniel.rei.forge.REIPluginCommon;

@REIPluginCommon
public final class TradingCellsReiCommonPlugin implements REICommonPlugin {
    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        registry.beginRecipeFiller(ArcaneInfusionRecipe.class)
                .fill(ArcaneInfusionReiDisplay::from);
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(ArcaneInfusionReiDisplay.serializerId(), ArcaneInfusionReiDisplay.SERIALIZER);
    }
}
