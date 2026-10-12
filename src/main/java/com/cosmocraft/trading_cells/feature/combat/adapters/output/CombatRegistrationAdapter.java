package com.cosmocraft.trading_cells.feature.combat.adapters.output;

import com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.DecapitationSmithingRecipe;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class CombatRegistrationAdapter {
    public static final DeferredItem<com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.VoltaicCapacitorItem> CAPACITOR = Registration.ITEMS.register("voltaic_capacitor",
            () -> new com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.VoltaicCapacitorItem(properties("voltaic_capacitor").stacksTo(1),
                    com.cosmocraft.trading_cells.feature.combat.domain.model.StormChargeRules.CAPACITY));
    public static final DeferredItem<com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricBatonItem> BATON = Registration.ITEMS.register("electric_baton",
            () -> new com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricBatonItem(properties("electric_baton")
                    .sword(net.minecraft.world.item.ToolMaterial.IRON, -2, -2.4F)));
    public static final DeferredItem<com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem> WELDER = Registration.ITEMS.register("electric_arc_welder",
            () -> new com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.ElectricArcWelderItem(properties("electric_arc_welder").stacksTo(1)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationBlock> STATION = Registration.BLOCKS.register("storm_station",
            () -> new com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationBlock(com.cosmocraft.trading_cells.platform.neoforge.machine.MachineBlockProperties.villager("storm_station")));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STATION_ITEM = Registration.ITEMS.register("storm_station",
            () -> new net.minecraft.world.item.BlockItem(STATION.get(), properties("storm_station").useBlockDescriptionPrefix()));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationBlockEntity>> STATION_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register("storm_station", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationBlockEntity::new, STATION.get()));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationMenu>> STATION_MENU =
            Registration.MENU_TYPES.register("storm_station", () -> new net.minecraft.world.inventory.MenuType<>(com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormStationMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    private static Item.Properties properties(String id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)));
    }
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> ARC_LOOP = sound("electric_arc_loop");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> BATON_HIT = sound("electric_baton_hit");
    private static DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> sound(String name) {
        return Registration.SOUND_EVENTS.register(name, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, name)));
    }
    public static final String STORM_SHARD_ID = "storm_shard";
    public static final DeferredItem<Item> STORM_SHARD_ITEM = Registration.ITEMS.register(
            STORM_SHARD_ID,
            () -> new Item(new Item.Properties().setId(ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, STORM_SHARD_ID)
            )))
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DecapitationSmithingRecipe>>
            DECAPITATION_SMITHING_SERIALIZER = Registration.RECIPE_SERIALIZERS.register(
                    "decapitation_smithing_upgrade",
                    () -> DecapitationSmithingRecipe.SERIALIZER
            );

    private CombatRegistrationAdapter() {
    }

    public static void load() {
        Registration.ITEMS.addAlias(Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "advanced_voltaic_capacitor"), CAPACITOR.getId());
    }
}
