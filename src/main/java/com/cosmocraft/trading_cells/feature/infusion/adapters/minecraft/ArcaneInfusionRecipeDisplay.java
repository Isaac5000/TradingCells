package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record ArcaneInfusionRecipeDisplay(
        List<SlotDisplay> ingredients,
        SlotDisplay result,
        SlotDisplay craftingStation,
        int experience
) implements RecipeDisplay {
    private static final Codec<List<SlotDisplay>> INGREDIENTS_CODEC =
            SlotDisplay.CODEC.listOf().validate(ArcaneInfusionRecipeDisplay::validateIngredients);

    public static final MapCodec<ArcaneInfusionRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    INGREDIENTS_CODEC.fieldOf("ingredients")
                            .forGetter(ArcaneInfusionRecipeDisplay::ingredients),
                    SlotDisplay.CODEC.fieldOf("result").forGetter(ArcaneInfusionRecipeDisplay::result),
                    SlotDisplay.CODEC.fieldOf("crafting_station")
                            .forGetter(ArcaneInfusionRecipeDisplay::craftingStation),
                    Codec.INT.fieldOf("experience").forGetter(ArcaneInfusionRecipeDisplay::experience)
            ).apply(instance, ArcaneInfusionRecipeDisplay::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionRecipeDisplay> STREAM_CODEC =
            StreamCodec.of(ArcaneInfusionRecipeDisplay::encode, ArcaneInfusionRecipeDisplay::decode);

    public ArcaneInfusionRecipeDisplay {
        if (ingredients.size() != ArcaneInfusionInput.SIZE) {
            throw new IllegalArgumentException("Arcane infusion display requires exactly nine ingredients");
        }
        ingredients = List.copyOf(ingredients);
    }

    @Override
    public Type<? extends RecipeDisplay> type() {
        return ArcaneInfuserRegistrationAdapter.RECIPE_DISPLAY_TYPE.get();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ArcaneInfusionRecipeDisplay display) {
        for (SlotDisplay ingredient : display.ingredients()) {
            SlotDisplay.STREAM_CODEC.encode(buffer, ingredient);
        }
        SlotDisplay.STREAM_CODEC.encode(buffer, display.result());
        SlotDisplay.STREAM_CODEC.encode(buffer, display.craftingStation());
        buffer.writeVarInt(display.experience());
    }

    private static ArcaneInfusionRecipeDisplay decode(RegistryFriendlyByteBuf buffer) {
        List<SlotDisplay> ingredients = new ArrayList<>(ArcaneInfusionInput.SIZE);
        for (int slot = 0; slot < ArcaneInfusionInput.SIZE; slot++) {
            ingredients.add(SlotDisplay.STREAM_CODEC.decode(buffer));
        }
        return new ArcaneInfusionRecipeDisplay(
                ingredients,
                SlotDisplay.STREAM_CODEC.decode(buffer),
                SlotDisplay.STREAM_CODEC.decode(buffer),
                buffer.readVarInt()
        );
    }

    private static DataResult<List<SlotDisplay>> validateIngredients(List<SlotDisplay> ingredients) {
        return ingredients.size() == ArcaneInfusionInput.SIZE
                ? DataResult.success(List.copyOf(ingredients))
                : DataResult.error(() -> "Arcane infusion display requires exactly nine ingredients");
    }
}
