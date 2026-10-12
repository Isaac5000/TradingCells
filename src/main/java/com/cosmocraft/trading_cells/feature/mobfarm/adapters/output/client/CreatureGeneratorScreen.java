package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Same synthesis/XP layout; a separate screen type keeps REI navigation unambiguous. */
public final class CreatureGeneratorScreen extends EssenceWorkbenchScreen {
    public CreatureGeneratorScreen(EssenceWorkbenchMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
}
