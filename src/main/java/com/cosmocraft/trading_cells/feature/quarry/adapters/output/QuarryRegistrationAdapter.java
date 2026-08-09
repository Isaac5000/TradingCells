package com.cosmocraft.trading_cells.feature.quarry.adapters.output;

import com.cosmocraft.trading_cells.feature.quarry.adapters.input.PiglinQuarryBlock;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.PiglinQuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlock;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryMenu;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.VillagerQuarryBlockEntity;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

public final class QuarryRegistrationAdapter {
    public static final String QUARRY_ID = "quarry";
    public static final String PIGLIN_QUARRY_ID = "piglin_quarry";
    public static final String COPPER_UPGRADE_ID = "quarry_copper_upgrade";
    public static final String IRON_UPGRADE_ID = "quarry_iron_upgrade";
    public static final String GOLD_UPGRADE_ID = "quarry_gold_upgrade";
    public static final String DIAMOND_UPGRADE_ID = "quarry_diamond_upgrade";
    public static final String NETHERITE_UPGRADE_ID = "quarry_netherite_upgrade";

    public static final DeferredBlock<QuarryBlock> QUARRY_BLOCK = Registration.BLOCKS.register(QUARRY_ID, () ->
            new QuarryBlock(MachineBlockProperties.villager(QUARRY_ID))
    );
    public static final DeferredBlock<PiglinQuarryBlock> PIGLIN_QUARRY_BLOCK =
            Registration.BLOCKS.register(PIGLIN_QUARRY_ID, () ->
                    new PiglinQuarryBlock(MachineBlockProperties.piglin(PIGLIN_QUARRY_ID))
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillagerQuarryBlockEntity>>
            QUARRY_BLOCK_ENTITY = Registration.BLOCK_ENTITY_TYPES.register(QUARRY_ID, () ->
                    new BlockEntityType<>(VillagerQuarryBlockEntity::new, QUARRY_BLOCK.get())
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PiglinQuarryBlockEntity>>
            PIGLIN_QUARRY_BLOCK_ENTITY = Registration.BLOCK_ENTITY_TYPES.register(PIGLIN_QUARRY_ID, () ->
                    new BlockEntityType<>(PiglinQuarryBlockEntity::new, PIGLIN_QUARRY_BLOCK.get())
            );
    public static final DeferredItem<BlockItem> QUARRY_ITEM = Registration.ITEMS.register(QUARRY_ID, () ->
            new BlockItem(QUARRY_BLOCK.get(), itemProperties(QUARRY_ID))
    );
    public static final DeferredItem<BlockItem> PIGLIN_QUARRY_ITEM = Registration.ITEMS.register(PIGLIN_QUARRY_ID, () ->
            new BlockItem(PIGLIN_QUARRY_BLOCK.get(), itemProperties(PIGLIN_QUARRY_ID))
    );
    public static final DeferredItem<Item> QUARRY_COPPER_UPGRADE_ITEM = upgrade(COPPER_UPGRADE_ID);
    public static final DeferredItem<Item> QUARRY_IRON_UPGRADE_ITEM = upgrade(IRON_UPGRADE_ID);
    public static final DeferredItem<Item> QUARRY_GOLD_UPGRADE_ITEM = upgrade(GOLD_UPGRADE_ID);
    public static final DeferredItem<Item> QUARRY_DIAMOND_UPGRADE_ITEM = upgrade(DIAMOND_UPGRADE_ID);
    public static final DeferredItem<Item> QUARRY_NETHERITE_UPGRADE_ITEM = upgrade(NETHERITE_UPGRADE_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<QuarryMenu>> QUARRY_MENU =
            Registration.MENU_TYPES.register(QUARRY_ID, () ->
                    new MenuType<>(QuarryMenu::villager, FeatureFlags.VANILLA_SET)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<QuarryMenu>> PIGLIN_QUARRY_MENU =
            Registration.MENU_TYPES.register(PIGLIN_QUARRY_ID, () ->
                    new MenuType<>(QuarryMenu::piglin, FeatureFlags.VANILLA_SET)
            );

    private QuarryRegistrationAdapter() {
    }

    public static void load(IEventBus modEventBus) {
        modEventBus.addListener(QuarryRegistrationAdapter::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                QUARRY_BLOCK_ENTITY.get(),
                WorldlyContainerWrapper::new
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                PIGLIN_QUARRY_BLOCK_ENTITY.get(),
                WorldlyContainerWrapper::new
        );
    }

    private static DeferredItem<Item> upgrade(String id) {
        return Registration.ITEMS.register(id, () -> new Item(itemProperties(id)));
    }

    private static Item.Properties itemProperties(String id) {
        return new Item.Properties().setId(ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)
        ));
    }
}
