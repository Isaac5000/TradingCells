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
        // Forces class loading so all DeferredRegister entries are created.
    }
}
