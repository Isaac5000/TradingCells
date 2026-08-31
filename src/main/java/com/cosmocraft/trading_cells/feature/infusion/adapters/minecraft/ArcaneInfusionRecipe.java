package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record ArcaneInfusionRecipe(
        List<ArcaneInfusionIngredientSlot> ingredients,
        int experience,
        ArcaneInfusionResult result,
        ArcaneInfusionRecipeCategory category
) implements Recipe<ArcaneInfusionInput> {
    private static final Codec<List<ArcaneInfusionIngredientSlot>> INGREDIENTS_CODEC =
            ArcaneInfusionIngredientSlot.CODEC.listOf().validate(ArcaneInfusionRecipe::validateIngredients);

    public static final MapCodec<ArcaneInfusionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(ArcaneInfusionRecipe::ingredients),
                    ExtraCodecs.POSITIVE_INT.fieldOf("experience").forGetter(ArcaneInfusionRecipe::experience),
                    ArcaneInfusionResult.CODEC.fieldOf("result").forGetter(ArcaneInfusionRecipe::result),
                    ArcaneInfusionRecipeCategory.CODEC.optionalFieldOf(
                            "category",
                            ArcaneInfusionRecipeCategory.MISC
                    ).forGetter(ArcaneInfusionRecipe::category)
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
        category = category == null ? ArcaneInfusionRecipeCategory.MISC : category;
    }

    @Override
    public boolean matches(ArcaneInfusionInput input, Level level) {
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            if (!ingredients.get(slot).matches(input.getItem(slot))) {
                return false;
            }
        }
        return result.matchesInput(input);
    }

    @Override
    public ItemStack assemble(ArcaneInfusionInput input) {
        return result.assemble(input);
    }

    public ArcaneInfusionIngredientSlot ingredient(int slot) {
        return ingredients.get(slot);
    }

    public boolean matchesPlacementStack(int slot, ItemStack stack) {
        return ingredients.get(slot).matches(stack) && result.matchesPlacementInput(slot, stack);
    }

    public static boolean isPlainBook(ItemStack stack) {
        return stack.is(Items.BOOK) && stack.getComponentsPatch().isEmpty();
    }

    @Override
    public boolean isSpecial() {
        return false;
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
        List<Optional<net.minecraft.world.item.crafting.Ingredient>> placement = ingredients.stream()
                .map(slot -> slot.ingredient().map(SizedIngredient::ingredient))
                .toList();
        return PlacementInfo.createFromOptionals(placement);
    }

    @Override
    public List<RecipeDisplay> display() {
        List<SlotDisplay> inputs = new ArrayList<>(ArcaneInfusionInput.SIZE);
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            int inputSlot = slot;
            ArcaneInfusionIngredientSlot ingredient = ingredients.get(inputSlot);
            inputs.add(ingredient.ingredient().isEmpty()
                    ? SlotDisplay.Empty.INSTANCE
                    : result.displayInputOverride(inputSlot)
                            .map(ArcaneInfusionRecipe::display)
                            .orElseGet(() -> display(ingredient.ingredient().orElseThrow())));
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
        return ArcaneInfuserRegistrationAdapter.recipeBookCategory(category);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ArcaneInfusionRecipe recipe) {
        for (ArcaneInfusionIngredientSlot ingredient : recipe.ingredients()) {
            ArcaneInfusionIngredientSlot.STREAM_CODEC.encode(buffer, ingredient);
        }
        buffer.writeVarInt(recipe.experience());
        ArcaneInfusionResult.STREAM_CODEC.encode(buffer, recipe.result());
        buffer.writeByte(recipe.category().ordinal());
    }

    private static ArcaneInfusionRecipe decode(RegistryFriendlyByteBuf buffer) {
        List<ArcaneInfusionIngredientSlot> ingredients = new ArrayList<>(ArcaneInfusionInput.SIZE);
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            ingredients.add(ArcaneInfusionIngredientSlot.STREAM_CODEC.decode(buffer));
        }
        return new ArcaneInfusionRecipe(
                ingredients,
                buffer.readVarInt(),
                ArcaneInfusionResult.STREAM_CODEC.decode(buffer),
                ArcaneInfusionRecipeCategory.fromOrdinal(buffer.readUnsignedByte())
        );
    }

    private static DataResult<List<ArcaneInfusionIngredientSlot>> validateIngredients(
            List<ArcaneInfusionIngredientSlot> ingredients
    ) {
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
