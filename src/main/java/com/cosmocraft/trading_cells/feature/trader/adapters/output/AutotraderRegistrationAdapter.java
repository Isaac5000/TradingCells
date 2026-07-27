package com.cosmocraft.trading_cells.feature.trader.adapters.output;

import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlock;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockItem;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class AutotraderRegistrationAdapter {
    public static final String AUTOTRADER_ID = "autotrader";

    public static final DeferredBlock<AutotraderBlock> AUTOTRADER_BLOCK = Registration.BLOCKS.register(AUTOTRADER_ID, () ->
            new AutotraderBlock(MachineBlockProperties.villager(AUTOTRADER_ID))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutotraderBlockEntity>> AUTOTRADER_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(AUTOTRADER_ID, () ->
                    new BlockEntityType<>(AutotraderBlockEntity::new, AUTOTRADER_BLOCK.get())
            );

    public static final DeferredItem<AutotraderBlockItem> AUTOTRADER_ITEM = Registration.ITEMS.register(AUTOTRADER_ID, () ->
            new AutotraderBlockItem(AUTOTRADER_BLOCK.get(), new Item.Properties().setId(
                    ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, AUTOTRADER_ID))
            ))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<AutotraderMenu>> AUTOTRADER_MENU =
            Registration.MENU_TYPES.register(AUTOTRADER_ID, () -> new MenuType<>(AutotraderMenu::new, FeatureFlags.VANILLA_SET));

    private AutotraderRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
