package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record ArcaneInfusionRecipe(
        List<SizedIngredient> ingredients,
        int experience,
        ArcaneInfusionResult result
) implements Recipe<ArcaneInfusionInput> {
    private static final Codec<List<SizedIngredient>> INGREDIENTS_CODEC =
            SizedIngredient.NESTED_CODEC.listOf().validate(ArcaneInfusionRecipe::validateIngredients);

    public static final MapCodec<ArcaneInfusionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(ArcaneInfusionRecipe::ingredients),
                    ExtraCodecs.POSITIVE_INT.fieldOf("experience").forGetter(ArcaneInfusionRecipe::experience),
                    ArcaneInfusionResult.CODEC.fieldOf("result").forGetter(ArcaneInfusionRecipe::result)
            ).apply(instance, ArcaneInfusionRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionRecipe> STREAM_CODEC =
            StreamCodec.of(ArcaneInfusionRecipe::encode, ArcaneInfusionRecipe::decode);
    public static final RecipeSerializer<ArcaneInfusionRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ArcaneInfusionRecipe {
        if (ingredients.size() != ArcaneInfusionInput.SIZE) {
            throw new IllegalArgumentException("Arcane infusion requires exactly nine ingredients");
        }
        ingredients = List.copyOf(ingredients);
    }

    @Override
    public boolean matches(ArcaneInfusionInput input, Level level) {
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            if (!ingredients.get(slot).test(input.getItem(slot))) {
                return false;
            }
        }
        return result.matchesInput(input);
    }

    @Override
    public ItemStack assemble(ArcaneInfusionInput input) {
        return result.assemble(input);
    }

    public SizedIngredient ingredient(int slot) {
        return ingredients.get(slot);
    }

    public static boolean isPlainBook(ItemStack stack) {
        return stack.is(Items.BOOK) && stack.getComponentsPatch().isEmpty();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<ArcaneInfusionInput>> getSerializer() {
        return ArcaneInfuserRegistrationAdapter.RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<ArcaneInfusionInput>> getType() {
        return ArcaneInfuserRegistrationAdapter.RECIPE_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public List<RecipeDisplay> display() {
        List<SlotDisplay> inputs = new ArrayList<>(ArcaneInfusionInput.SIZE);
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            int inputSlot = slot;
            inputs.add(result.displayInputOverride(inputSlot)
                    .map(ArcaneInfusionRecipe::display)
                    .orElseGet(() -> display(ingredients.get(inputSlot))));
        }
        ItemStack displayResult = result.displayResult();
        return List.of(new ArcaneInfusionRecipeDisplay(
                inputs,
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(displayResult)),
                new SlotDisplay.ItemSlotDisplay(ArcaneInfuserRegistrationAdapter.ITEM.get()),
                experience
        ));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ArcaneInfusionRecipe recipe) {
        for (SizedIngredient ingredient : recipe.ingredients()) {
            SizedIngredient.STREAM_CODEC.encode(buffer, ingredient);
        }
        buffer.writeVarInt(recipe.experience());
        ArcaneInfusionResult.STREAM_CODEC.encode(buffer, recipe.result());
    }

    private static ArcaneInfusionRecipe decode(RegistryFriendlyByteBuf buffer) {
        List<SizedIngredient> ingredients = new ArrayList<>(ArcaneInfusionInput.SIZE);
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            ingredients.add(SizedIngredient.STREAM_CODEC.decode(buffer));
        }
        return new ArcaneInfusionRecipe(
                ingredients,
                buffer.readVarInt(),
                ArcaneInfusionResult.STREAM_CODEC.decode(buffer)
        );
    }

    private static DataResult<List<SizedIngredient>> validateIngredients(List<SizedIngredient> ingredients) {
        return ingredients.size() == ArcaneInfusionInput.SIZE
                ? DataResult.success(List.copyOf(ingredients))
                : DataResult.error(() -> "Arcane infusion requires exactly nine ingredients");
    }

    @SuppressWarnings("deprecation")
    private static SlotDisplay display(SizedIngredient ingredient) {
        List<SlotDisplay> displays = ingredient.ingredient().items()
                .map(holder -> (SlotDisplay) new SlotDisplay.ItemStackSlotDisplay(
                        new ItemStackTemplate(holder, ingredient.count(), DataComponentPatch.EMPTY)
                ))
                .toList();
        return displays.size() == 1 ? displays.getFirst() : new SlotDisplay.Composite(displays);
    }

    private static SlotDisplay display(ItemStack stack) {
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    }
}
