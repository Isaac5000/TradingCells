package com.cosmocraft.trading_cells.feature.combat.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/** Raises an existing Decapitation enchantment by one while preserving the base stack exactly. */
public final class DecapitationSmithingRecipe extends SimpleSmithingRecipe {
    public static final MapCodec<DecapitationSmithingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                    Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
                    Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition)
            ).apply(instance, DecapitationSmithingRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DecapitationSmithingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC,
                    recipe -> recipe.commonInfo,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    recipe -> recipe.base,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    recipe -> recipe.addition,
                    DecapitationSmithingRecipe::new
            );
    public static final RecipeSerializer<DecapitationSmithingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient base;
    private final Ingredient addition;

    public DecapitationSmithingRecipe(
            Recipe.CommonInfo commonInfo,
            Ingredient base,
            Ingredient addition
    ) {
        super(commonInfo);
        this.base = base;
        this.addition = addition;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        int currentLevel = decapitationLevel(input.base());
        return input.template().isEmpty()
                && base.test(input.base())
                && addition.test(input.addition())
                && currentLevel > 0
                && currentLevel < DecapitationRules.MAX_DECAPITATION_LEVEL;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(input.base());
        Holder<Enchantment> decapitation = enchantments.entrySet().stream()
                .map(java.util.Map.Entry::getKey)
                .filter(holder -> holder.is(CombatEnchantments.DECAPITATION))
                .findFirst()
                .orElse(null);
        if (decapitation == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = input.base().copyWithCount(1);
        ItemEnchantments.Mutable upgraded = new ItemEnchantments.Mutable(enchantments);
        upgraded.set(decapitation, Math.min(
                DecapitationRules.MAX_DECAPITATION_LEVEL,
                enchantments.getLevel(decapitation) + 1
        ));
        EnchantmentHelper.setEnchantments(result, upgraded.toImmutable());
        return result;
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.empty();
    }

    @Override
    public Ingredient baseIngredient() {
        return base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(addition);
    }

    @Override
    public RecipeSerializer<DecapitationSmithingRecipe> getSerializer() {
        return CombatRegistrationAdapter.DECAPITATION_SMITHING_SERIALIZER.get();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(List.of(
                Optional.empty(),
                Optional.of(base),
                Optional.of(addition)
        ));
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new SmithingRecipeDisplay(
                Ingredient.optionalIngredientToDisplay(Optional.empty()),
                base.display(),
                addition.display(),
                base.display(),
                new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
        ));
    }

    private static int decapitationLevel(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().stream()
                .filter(entry -> entry.getKey().is(CombatEnchantments.DECAPITATION))
                .mapToInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue)
                .findFirst()
                .orElse(0);
    }
}
