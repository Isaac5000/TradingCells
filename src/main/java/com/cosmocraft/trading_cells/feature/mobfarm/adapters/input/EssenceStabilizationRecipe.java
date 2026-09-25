package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record EssenceStabilizationRecipe(int tier, SizedIngredient amethyst, SizedIngredient reagent, int duration)
        implements Recipe<EssenceStabilizationRecipe.Input> {
    public EssenceStabilizationRecipe {
        if (tier < 1 || tier > 4 || duration <= 0) {
            throw new IllegalArgumentException("Invalid stabilization tier or duration");
        }
    }
    public record Input(ItemStack vial, ItemStack amethyst, ItemStack reagent) implements RecipeInput {
        public ItemStack getItem(int slot) {
            return switch (slot) { case 0 -> vial; case 1 -> amethyst; case 2 -> reagent; default -> throw new IndexOutOfBoundsException(slot); };
        }
        public int size() { return 3; }
    }
    public static final MapCodec<EssenceStabilizationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 4).fieldOf("tier").forGetter(EssenceStabilizationRecipe::tier),
            SizedIngredient.NESTED_CODEC.fieldOf("amethyst").forGetter(EssenceStabilizationRecipe::amethyst),
            SizedIngredient.NESTED_CODEC.fieldOf("reagent").forGetter(EssenceStabilizationRecipe::reagent),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("duration", 100).forGetter(EssenceStabilizationRecipe::duration)
    ).apply(instance, EssenceStabilizationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceStabilizationRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeVarInt(recipe.tier());
                SizedIngredient.STREAM_CODEC.encode(buffer, recipe.amethyst());
                SizedIngredient.STREAM_CODEC.encode(buffer, recipe.reagent());
                buffer.writeVarInt(recipe.duration());
            }, buffer -> new EssenceStabilizationRecipe(buffer.readVarInt(), SizedIngredient.STREAM_CODEC.decode(buffer),
                    SizedIngredient.STREAM_CODEC.decode(buffer), buffer.readVarInt()));
    public static final RecipeSerializer<EssenceStabilizationRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    @Override public boolean matches(Input input, Level level) {
        return input.vial().is(MobFarmRegistrationAdapter.RAW_ESSENCE.get())
                && EntityEssenceData.entityTypeId(input.vial()) != null && EntityEssenceData.tier(input.vial()).id() == tier
                && amethyst.test(input.amethyst()) && reagent.test(input.reagent());
    }
    @Override public ItemStack assemble(Input input) { return EntityEssenceData.coreOf(input.vial()); }
    @Override public RecipeSerializer<? extends Recipe<Input>> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<? extends Recipe<Input>> getType() { return MobFarmRegistrationAdapter.STABILIZATION_TYPE.get(); }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public List<RecipeDisplay> display() { return List.of(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
}
