package com.cosmocraft.trading_cells.feature.infusion.adapters.output;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlock;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipeDisplay;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

public final class ArcaneInfuserRegistrationAdapter {
    public static final String ID = "arcane_infuser";
    public static final String RECIPE_ID = "arcane_infusion";
    private static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, ID);
    private static final Identifier RECIPE_IDENTIFIER =
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, RECIPE_ID);

    public static final DeferredBlock<ArcaneInfuserBlock> BLOCK = Registration.BLOCKS.register(
            ID,
            () -> new ArcaneInfuserBlock(MachineBlockProperties.villager(ID))
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneInfuserBlockEntity>> BLOCK_ENTITY =
            Registration.BLOCK_ENTITY_TYPES.register(
                    ID,
                    () -> new BlockEntityType<>(ArcaneInfuserBlockEntity::new, BLOCK.get())
            );
    public static final DeferredItem<BlockItem> ITEM = Registration.ITEMS.register(
            ID,
            () -> new BlockItem(BLOCK.get(), new Item.Properties().setId(ResourceKey.create(
                    Registries.ITEM,
                    IDENTIFIER
            )))
    );
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneInfuserMenu>> MENU =
            Registration.MENU_TYPES.register(
                    ID,
                    () -> new MenuType<>(ArcaneInfuserMenu::new, FeatureFlags.VANILLA_SET)
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcaneInfusionRecipe>>
            RECIPE_SERIALIZER = Registration.RECIPE_SERIALIZERS.register(
                    RECIPE_ID,
                    () -> ArcaneInfusionRecipe.SERIALIZER
            );
    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcaneInfusionRecipe>> RECIPE_TYPE =
            Registration.RECIPE_TYPES.register(RECIPE_ID, () -> RecipeType.simple(RECIPE_IDENTIFIER));
    public static final DeferredHolder<RecipeDisplay.Type<?>, RecipeDisplay.Type<ArcaneInfusionRecipeDisplay>>
            RECIPE_DISPLAY_TYPE = Registration.RECIPE_DISPLAY_TYPES.register(
                    RECIPE_ID,
                    () -> new RecipeDisplay.Type<>(
                            ArcaneInfusionRecipeDisplay.MAP_CODEC,
                            ArcaneInfusionRecipeDisplay.STREAM_CODEC
                    )
            );

    private ArcaneInfuserRegistrationAdapter() {
    }

    public static void load(IEventBus modEventBus) {
        modEventBus.addListener(ArcaneInfuserRegistrationAdapter::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                BLOCK_ENTITY.get(),
                WorldlyContainerWrapper::new
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                BLOCK_ENTITY.get(),
                (infuser, side) -> infuser.fluidHandler()
        );
    }
}
