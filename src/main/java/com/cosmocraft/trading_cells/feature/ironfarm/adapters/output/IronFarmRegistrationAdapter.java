package com.cosmocraft.trading_cells.feature.ironfarm.adapters.output;

import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlock;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmMenu;
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

public final class IronFarmRegistrationAdapter {
    public static final String IRON_FARM_ID = "iron_farm";

    public static final DeferredBlock<IronFarmBlock> IRON_FARM_BLOCK = Registration.BLOCKS.register(IRON_FARM_ID, () ->
            new IronFarmBlock(MachineBlockProperties.villager(IRON_FARM_ID))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronFarmBlockEntity>> IRON_FARM_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(IRON_FARM_ID, () ->
                    new BlockEntityType<>(IronFarmBlockEntity::new, IRON_FARM_BLOCK.get())
            );

    public static final DeferredItem<BlockItem> IRON_FARM_ITEM = Registration.ITEMS.register(IRON_FARM_ID, () ->
            new BlockItem(IRON_FARM_BLOCK.get(), new Item.Properties().setId(
                    ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, IRON_FARM_ID))
            ))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<IronFarmMenu>> IRON_FARM_MENU =
            Registration.MENU_TYPES.register(IRON_FARM_ID, () -> new MenuType<>(IronFarmMenu::new, FeatureFlags.VANILLA_SET));

    private IronFarmRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so all DeferredRegister entries are created.
    }
}
