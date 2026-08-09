package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record ArcaneInfusionRecipe(
        SizedIngredient top,
        SizedIngredient left,
        SizedIngredient center,
        SizedIngredient right,
        SizedIngredient bottom,
        int experience,
        net.minecraft.core.Holder<Enchantment> enchantment,
        int enchantmentLevel
) implements Recipe<ArcaneInfusionInput> {
    public static final MapCodec<ArcaneInfusionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SizedIngredient.NESTED_CODEC.fieldOf("top").forGetter(ArcaneInfusionRecipe::top),
                    SizedIngredient.NESTED_CODEC.fieldOf("left").forGetter(ArcaneInfusionRecipe::left),
                    SizedIngredient.NESTED_CODEC.fieldOf("center").forGetter(ArcaneInfusionRecipe::center),
                    SizedIngredient.NESTED_CODEC.fieldOf("right").forGetter(ArcaneInfusionRecipe::right),
                    SizedIngredient.NESTED_CODEC.fieldOf("bottom").forGetter(ArcaneInfusionRecipe::bottom),
                    ExtraCodecs.POSITIVE_INT.fieldOf("experience").forGetter(ArcaneInfusionRecipe::experience),
                    Enchantment.CODEC.fieldOf("enchantment").forGetter(ArcaneInfusionRecipe::enchantment),
                    ExtraCodecs.intRange(1, Enchantment.MAX_LEVEL).optionalFieldOf("level", 1)
                            .forGetter(ArcaneInfusionRecipe::enchantmentLevel)
            ).apply(instance, ArcaneInfusionRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, recipe) -> {
                        SizedIngredient.STREAM_CODEC.encode(buffer, recipe.top());
                        SizedIngredient.STREAM_CODEC.encode(buffer, recipe.left());
                        SizedIngredient.STREAM_CODEC.encode(buffer, recipe.center());
                        SizedIngredient.STREAM_CODEC.encode(buffer, recipe.right());
                        SizedIngredient.STREAM_CODEC.encode(buffer, recipe.bottom());
                        buffer.writeVarInt(recipe.experience());
                        Enchantment.STREAM_CODEC.encode(buffer, recipe.enchantment());
                        buffer.writeVarInt(recipe.enchantmentLevel());
                    },
                    buffer -> new ArcaneInfusionRecipe(
                            SizedIngredient.STREAM_CODEC.decode(buffer),
                            SizedIngredient.STREAM_CODEC.decode(buffer),
                            SizedIngredient.STREAM_CODEC.decode(buffer),
                            SizedIngredient.STREAM_CODEC.decode(buffer),
                            SizedIngredient.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt(),
                            Enchantment.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt()
                    )
            );
    public static final RecipeSerializer<ArcaneInfusionRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(ArcaneInfusionInput input, Level level) {
        return top.test(input.getItem(0))
                && left.test(input.getItem(1))
                && isPlainBook(input.getItem(2))
                && center.test(input.getItem(2))
                && right.test(input.getItem(3))
                && bottom.test(input.getItem(4));
    }

    @Override
    public ItemStack assemble(ArcaneInfusionInput input) {
        return result();
    }

    public ItemStack result() {
        return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, enchantmentLevel));
    }

    public SizedIngredient ingredient(int slot) {
        return switch (slot) {
            case 0 -> top;
            case 1 -> left;
            case 2 -> center;
            case 3 -> right;
            case 4 -> bottom;
            default -> throw new IndexOutOfBoundsException(slot);
        };
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
        return List.of(new ArcaneInfusionRecipeDisplay(
                display(top),
                display(left),
                display(center),
                display(right),
                display(bottom),
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(result())),
                new SlotDisplay.ItemSlotDisplay(ArcaneInfuserRegistrationAdapter.ITEM.get()),
                experience
        ));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
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
}
