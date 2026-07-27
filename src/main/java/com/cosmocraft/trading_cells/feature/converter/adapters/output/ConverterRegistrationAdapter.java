package com.cosmocraft.trading_cells.feature.converter.adapters.output;

import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterBlock;
import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterBlockEntity;
import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterMenu;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ConverterRegistrationAdapter {
    public static final String CONVERTER_ID = "converter";

    public static final DeferredBlock<ConverterBlock> CONVERTER_BLOCK = Registration.BLOCKS.register(CONVERTER_ID, () ->
            new ConverterBlock(MachineBlockProperties.villager(CONVERTER_ID))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConverterBlockEntity>> CONVERTER_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(CONVERTER_ID, () ->
                    new BlockEntityType<>(ConverterBlockEntity::new, CONVERTER_BLOCK.get())
            );

    public static final DeferredItem<BlockItem> CONVERTER_ITEM = Registration.ITEMS.register(CONVERTER_ID, () ->
            new BlockItem(CONVERTER_BLOCK.get(), new Item.Properties().setId(
                    ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, CONVERTER_ID))
            ))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ConverterMenu>> CONVERTER_MENU =
            Registration.MENU_TYPES.register(CONVERTER_ID, () -> new MenuType<>(ConverterMenu::new, FeatureFlags.VANILLA_SET));

    private ConverterRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
