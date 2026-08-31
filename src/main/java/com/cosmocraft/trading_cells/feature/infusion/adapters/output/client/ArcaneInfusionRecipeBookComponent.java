package com.cosmocraft.trading_cells.feature.infusion.adapters.output.client;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipeDisplay;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.client.event.RegisterRecipeBookSearchCategoriesEvent;

public final class ArcaneInfusionRecipeBookComponent extends RecipeBookComponent<ArcaneInfuserMenu> {
    private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted")
    );
    private static final Component ONLY_CRAFTABLES =
            Component.translatable("gui.recipebook.toggleRecipes.craftable");

    public ArcaneInfusionRecipeBookComponent(ArcaneInfuserMenu menu) {
        super(menu, tabs());
    }

    public static void registerSearchCategory(RegisterRecipeBookSearchCategoriesEvent event) {
        event.register(
                SearchCategory.ALL,
                ArcaneInfuserRegistrationAdapter.GENERATORS_CATEGORY.get(),
                ArcaneInfuserRegistrationAdapter.EQUIPMENT_CATEGORY.get(),
                ArcaneInfuserRegistrationAdapter.PRODUCTION_CATEGORY.get(),
                ArcaneInfuserRegistrationAdapter.MISC_CATEGORY.get()
        );
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_BUTTON_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        int machineSlotCount = ArcaneInfuserBlockEntity.OUTPUT_SLOT + 1;
        return menu.slots.subList(0, machineSlotCount).contains(slot);
    }

    @Override
    protected void selectMatchingRecipes(
            RecipeCollection collection,
            StackedItemContents stackedContents
    ) {
        collection.selectRecipes(stackedContents, ArcaneInfusionRecipeDisplay.class::isInstance);
    }

    @Override
    protected Component getRecipeFilterName() {
        return ONLY_CRAFTABLES;
    }

    @Override
    protected void fillGhostRecipe(
            GhostSlots ghostSlots,
            RecipeDisplay recipe,
            ContextMap context
    ) {
        if (!(recipe instanceof ArcaneInfusionRecipeDisplay infusion)) {
            return;
        }
        ghostSlots.setResult(menu.slots.get(ArcaneInfuserBlockEntity.OUTPUT_SLOT), context, infusion.result());
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            SlotDisplay ingredient = infusion.ingredients().get(slot);
            if (!(ingredient instanceof SlotDisplay.Empty)) {
                ghostSlots.setInput(menu.slots.get(slot), context, ingredient);
            }
        }
    }

    private static List<TabInfo> tabs() {
        return List.of(
                new TabInfo(new ItemStack(Items.COMPASS), Optional.empty(), SearchCategory.ALL),
                new TabInfo(Items.SPAWNER, ArcaneInfuserRegistrationAdapter.GENERATORS_CATEGORY.get()),
                new TabInfo(Items.ENCHANTED_BOOK, ArcaneInfuserRegistrationAdapter.EQUIPMENT_CATEGORY.get()),
                new TabInfo(ArcaneInfuserRegistrationAdapter.ITEM.get(), ArcaneInfuserRegistrationAdapter.PRODUCTION_CATEGORY.get()),
                new TabInfo(Items.CHORUS_FRUIT, ArcaneInfuserRegistrationAdapter.MISC_CATEGORY.get())
        );
    }

    private enum SearchCategory implements ExtendedRecipeBookCategory {
        ALL
    }
}
