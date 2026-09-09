package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.output;

import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlock;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmMenu;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineItemHandler;

public final class RaiderFarmRegistrationAdapter {
    public static final String ID = "raider_farm";

    public static final DeferredBlock<RaiderFarmBlock> BLOCK = Registration.BLOCKS.register(ID, () ->
            new RaiderFarmBlock(MachineBlockProperties.villager(ID))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RaiderFarmBlockEntity>> BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(ID, () ->
                    new BlockEntityType<>(RaiderFarmBlockEntity::new, BLOCK.get())
            );

    public static final DeferredItem<BlockItem> ITEM = Registration.ITEMS.register(ID, () ->
            new BlockItem(BLOCK.get(), new Item.Properties().setId(ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, ID)
            )))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<RaiderFarmMenu>> MENU =
            Registration.MENU_TYPES.register(ID, () ->
                    new MenuType<>(RaiderFarmMenu::new, FeatureFlags.VANILLA_SET)
            );

    private RaiderFarmRegistrationAdapter() {
    }

    public static void load(IEventBus modEventBus) {
        modEventBus.addListener(RaiderFarmRegistrationAdapter::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
    }
}
