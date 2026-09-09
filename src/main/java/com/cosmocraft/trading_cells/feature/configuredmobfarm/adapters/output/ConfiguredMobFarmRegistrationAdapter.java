package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlock;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmMenu;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineItemHandler;

public final class ConfiguredMobFarmRegistrationAdapter {
    public static final String TYPE_ID = "configured_mob_farm";
    private static final Map<ConfiguredMobFarmKind, DeferredBlock<ConfiguredMobFarmBlock>> BLOCKS =
            new EnumMap<>(ConfiguredMobFarmKind.class);
    private static final Map<ConfiguredMobFarmKind, DeferredItem<BlockItem>> ITEMS =
            new EnumMap<>(ConfiguredMobFarmKind.class);

    static {
        for (ConfiguredMobFarmKind kind : ConfiguredMobFarmKind.values()) {
            String id = kind.blockId();
            DeferredBlock<ConfiguredMobFarmBlock> block = Registration.BLOCKS.register(id, () ->
                    new ConfiguredMobFarmBlock(MachineBlockProperties.villager(id), kind)
            );
            BLOCKS.put(kind, block);
            ITEMS.put(kind, Registration.ITEMS.register(id, () ->
                    new BlockItem(block.get(), new Item.Properties().setId(ResourceKey.create(
                            Registries.ITEM,
                            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)
                    )))
            ));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfiguredMobFarmBlockEntity>> BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(TYPE_ID, () ->
                    new BlockEntityType<>(
                            ConfiguredMobFarmBlockEntity::new,
                            BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    )
            );

    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredMobFarmMenu>> MENU =
            Registration.MENU_TYPES.register(TYPE_ID, () ->
                    new MenuType<>(ConfiguredMobFarmMenu::new, FeatureFlags.VANILLA_SET)
            );

    private ConfiguredMobFarmRegistrationAdapter() {
    }

    public static DeferredBlock<ConfiguredMobFarmBlock> block(ConfiguredMobFarmKind kind) {
        return BLOCKS.get(kind);
    }

    public static DeferredItem<BlockItem> item(ConfiguredMobFarmKind kind) {
        return ITEMS.get(kind);
    }

    public static Collection<DeferredItem<BlockItem>> items() {
        return List.copyOf(ITEMS.values());
    }

    public static void load(IEventBus modEventBus) {
        modEventBus.addListener(ConfiguredMobFarmRegistrationAdapter::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                BLOCK_ENTITY.get(),
                PortableMachineItemHandler::new
        );
    }
}
