package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipeDisplay;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Server-safe REI representation of a data-driven Arcane Infusion recipe. */
public final class ArcaneInfusionReiDisplay extends BasicDisplay {
    private static final int INPUT_COUNT = 9;
    private static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "arcane_infusion"
    );

    public static final CategoryIdentifier<ArcaneInfusionReiDisplay> CATEGORY =
            CategoryIdentifier.of(TradingCells.MOD_ID, "arcane_infusion");
    private static final MapCodec<ArcaneInfusionReiDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    EntryIngredient.codec().listOf().fieldOf("inputs")
                            .forGetter(ArcaneInfusionReiDisplay::getInputEntries),
                    EntryIngredient.codec().fieldOf("output")
                            .forGetter(display -> display.getOutputEntries().getFirst()),
                    Identifier.CODEC.fieldOf("location")
                            .forGetter(display -> display.getDisplayLocation().orElseThrow()),
                    ExtraCodecs.POSITIVE_INT.fieldOf("experience")
                            .forGetter(ArcaneInfusionReiDisplay::experience)
            ).apply(instance, ArcaneInfusionReiDisplay::new)
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionReiDisplay> STREAM_CODEC =
            StreamCodec.of(
                    ArcaneInfusionReiDisplay::encode,
                    ArcaneInfusionReiDisplay::decode
            );
    public static final DisplaySerializer<ArcaneInfusionReiDisplay> SERIALIZER =
            DisplaySerializer.of(MAP_CODEC, STREAM_CODEC);

    private final int experience;

    public ArcaneInfusionReiDisplay(
            List<EntryIngredient> inputs,
            EntryIngredient output,
            Identifier location,
            int experience
    ) {
        super(validateInputs(inputs), List.of(output), Optional.of(location));
        if (experience <= 0) {
            throw new IllegalArgumentException("Arcane infusion experience must be positive");
        }
        this.experience = experience;
    }

    public static ArcaneInfusionReiDisplay from(RecipeHolder<ArcaneInfusionRecipe> holder) {
        ArcaneInfusionRecipeDisplay recipe = (ArcaneInfusionRecipeDisplay) holder.value().display().getFirst();
        return new ArcaneInfusionReiDisplay(
                recipe.ingredients().stream()
                        .map(EntryIngredients::ofSlotDisplay)
                        .toList(),
                EntryIngredients.ofSlotDisplay(recipe.result()),
                holder.id().identifier(),
                recipe.experience()
        );
    }

    public static Identifier serializerId() {
        return SERIALIZER_ID;
    }

    public int experience() {
        return experience;
    }

    @Override
    public CategoryIdentifier<ArcaneInfusionReiDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public DisplaySerializer<ArcaneInfusionReiDisplay> getSerializer() {
        return SERIALIZER;
    }

    private static List<EntryIngredient> validateInputs(List<EntryIngredient> inputs) {
        if (inputs.size() != INPUT_COUNT) {
            throw new IllegalArgumentException("Arcane infusion requires exactly nine REI inputs");
        }
        return List.copyOf(inputs);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ArcaneInfusionReiDisplay display) {
        buffer.writeVarInt(INPUT_COUNT);
        for (EntryIngredient input : display.getInputEntries()) {
            EntryIngredient.streamCodec().encode(buffer, input);
        }
        EntryIngredient.streamCodec().encode(buffer, display.getOutputEntries().getFirst());
        Identifier.STREAM_CODEC.encode(buffer, display.getDisplayLocation().orElseThrow());
        buffer.writeVarInt(display.experience());
    }

    private static ArcaneInfusionReiDisplay decode(RegistryFriendlyByteBuf buffer) {
        int inputCount = buffer.readVarInt();
        if (inputCount != INPUT_COUNT) {
            throw new IllegalArgumentException("Invalid Arcane Infusion REI input count: " + inputCount);
        }
        List<EntryIngredient> inputs = new ArrayList<>(INPUT_COUNT);
        for (int index = 0; index < INPUT_COUNT; index++) {
            inputs.add(EntryIngredient.streamCodec().decode(buffer));
        }
        EntryIngredient output = EntryIngredient.streamCodec().decode(buffer);
        Identifier location = Identifier.STREAM_CODEC.decode(buffer);
        int experience = buffer.readVarInt();
        return new ArcaneInfusionReiDisplay(inputs, output, location, experience);
    }
}
