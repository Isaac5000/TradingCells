package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceItem;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceExtractorItem;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchBlock;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceWorkbenchMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlock;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmMenu;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmUpgradeItem;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineItemHandler;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import java.util.ArrayList;
import java.util.List;
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

public final class MobFarmRegistrationAdapter {
    public static final DeferredBlock<MobFarmBlock> BLOCK = Registration.BLOCKS.register("mob_farm",
            () -> new MobFarmBlock(MachineBlockProperties.villager("mob_farm")));
    public static final DeferredItem<BlockItem> ITEM = Registration.ITEMS.register("mob_farm",
            () -> new BlockItem(BLOCK.get(), properties("mob_farm")));
    public static final DeferredBlock<EssenceWorkbenchBlock> ESSENCE_WORKBENCH = Registration.BLOCKS.register("essence_workbench",
            () -> new EssenceWorkbenchBlock(MachineBlockProperties.villager("essence_workbench")));
    public static final DeferredItem<BlockItem> WORKBENCH_ITEM = Registration.ITEMS.register("essence_workbench",
            () -> new BlockItem(ESSENCE_WORKBENCH.get(), properties("essence_workbench")));
    public static final DeferredItem<EntityEssenceItem> ENTITY_ESSENCE = Registration.ITEMS.register("entity_essence",
            () -> new EntityEssenceItem(properties("entity_essence").stacksTo(1), false));
    public static final DeferredItem<EntityEssenceItem> ENTITY_MODULE = Registration.ITEMS.register("entity_module",
            () -> new EntityEssenceItem(properties("entity_module").stacksTo(1), true));
    public static final DeferredItem<EssenceExtractorItem> ESSENCE_EXTRACTOR = Registration.ITEMS.register("essence_extractor",
            () -> new EssenceExtractorItem(properties("essence_extractor").durability(256)));
    public static final List<DeferredItem<MobFarmUpgradeItem>> UPGRADES = registerUpgrades();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobFarmBlockEntity>> BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register("mob_farm", () -> new BlockEntityType<>(MobFarmBlockEntity::new, BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceWorkbenchBlockEntity>> WORKBENCH_BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register("essence_workbench",
                    () -> new BlockEntityType<>(EssenceWorkbenchBlockEntity::new, ESSENCE_WORKBENCH.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<MobFarmMenu>> MENU = Registration.MENU_TYPES.register("mob_farm",
            () -> new MenuType<>(MobFarmMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<EssenceWorkbenchMenu>> WORKBENCH_MENU = Registration.MENU_TYPES.register("essence_workbench",
            () -> new MenuType<>(EssenceWorkbenchMenu::new, FeatureFlags.VANILLA_SET));

    private MobFarmRegistrationAdapter() { }
    private static Item.Properties properties(String id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trading_cells", id)));
    }
    private static List<DeferredItem<MobFarmUpgradeItem>> registerUpgrades() {
        List<DeferredItem<MobFarmUpgradeItem>> upgrades = new ArrayList<>();
        String[] materials = {"copper", "iron", "gold", "diamond", "netherite"};
        for (MobFarmUpgradeItem.Kind kind : MobFarmUpgradeItem.Kind.values()) {
            for (int index = 0; index < materials.length; index++) {
                String id = "mob_farm_" + kind.name().toLowerCase(java.util.Locale.ROOT) + "_" + materials[index] + "_upgrade";
                int tier = index + 1;
                upgrades.add(Registration.ITEMS.register(id, () -> new MobFarmUpgradeItem(properties(id).stacksTo(1), kind, tier)));
            }
        }
        return List.copyOf(upgrades);
    }

    public static void load(IEventBus bus) { bus.addListener(MobFarmRegistrationAdapter::registerCapabilities); }
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, WORKBENCH_BLOCK_ENTITY.get(), PortableMachineItemHandler::new);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BLOCK_ENTITY.get(), (farm, side) -> farm.experienceFluidHandler());
    }
}
