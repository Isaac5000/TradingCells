package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EssenceStabilizationRecipe;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import com.mojang.serialization.Codec;
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
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** One tier recipe, not a catalogue of the entities installed in the current world. */
public final class EssenceReiDisplay extends BasicDisplay {
    public static final CategoryIdentifier<EssenceReiDisplay> STABILIZATION = CategoryIdentifier.of("trading_cells", "essence_stabilization");
    public static final CategoryIdentifier<EssenceReiDisplay> SYNTHESIS = CategoryIdentifier.of("trading_cells", "essence_synthesis");
    public static final Identifier ID = Identifier.fromNamespaceAndPath("trading_cells", "essence_processing");
    private final boolean synthesis;
    private final int tier;
    private final int cost;
    private static final MapCodec<EssenceReiDisplay> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(EssenceReiDisplay::getInputEntries),
            EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(EssenceReiDisplay::getOutputEntries),
            Identifier.CODEC.fieldOf("location").forGetter(display -> display.getDisplayLocation().orElseThrow()),
            Codec.BOOL.fieldOf("synthesis").forGetter(EssenceReiDisplay::synthesis),
            Codec.intRange(1, 4).fieldOf("tier").forGetter(EssenceReiDisplay::tier),
            Codec.INT.fieldOf("cost").forGetter(EssenceReiDisplay::cost)
    ).apply(instance, EssenceReiDisplay::new));
    public static final DisplaySerializer<EssenceReiDisplay> SERIALIZER = DisplaySerializer.of(CODEC,
            StreamCodec.of((buffer, display) -> {
                buffer.writeVarInt(display.getInputEntries().size());
                for (var ingredient : display.getInputEntries()) { EntryIngredient.streamCodec().encode(buffer, ingredient); }
                buffer.writeVarInt(display.getOutputEntries().size());
                for (var ingredient : display.getOutputEntries()) { EntryIngredient.streamCodec().encode(buffer, ingredient); }
                Identifier.STREAM_CODEC.encode(buffer, display.getDisplayLocation().orElseThrow());
                buffer.writeBoolean(display.synthesis); buffer.writeVarInt(display.tier); buffer.writeVarInt(display.cost);
            }, buffer -> new EssenceReiDisplay(readEntries(buffer), readEntries(buffer), Identifier.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt())));

    private EssenceReiDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Identifier location, boolean synthesis, int tier, int cost) {
        super(inputs, outputs, Optional.of(location));
        if (tier < 1 || tier > 4 || cost < 0 || inputs.size() > 3 || outputs.size() > 3) {
            throw new IllegalArgumentException("Invalid essence display");
        }
        this.synthesis = synthesis; this.tier = tier; this.cost = cost;
    }
    private static List<EntryIngredient> readEntries(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 3) { throw new IllegalArgumentException("Invalid essence display entry count"); }
        List<EntryIngredient> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) { entries.add(EntryIngredient.streamCodec().decode(buffer)); }
        return entries;
    }
    public static EssenceReiDisplay from(RecipeHolder<EssenceStabilizationRecipe> holder) {
        var recipe = holder.value();
        List<EntryIngredient> outputs = new ArrayList<>();
        outputs.add(EntryIngredients.of(tierStack(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get(), recipe.tier())));
        outputs.add(EntryIngredients.of(MobFarmRegistrationAdapter.EMPTY_VIAL.get()));
        var remainders = recipe.reagent().ingredient().getValues().stream().map(item -> item.value().getDefaultInstance().getCraftingRemainder())
                .filter(java.util.Objects::nonNull).map(template -> {
                    ItemStack stack = template.create(); stack.setCount(stack.getCount() * recipe.reagent().count()); return EntryStacks.of(stack);
                }).toList();
        if (!remainders.isEmpty()) { outputs.add(EntryIngredient.of(remainders)); }
        return new EssenceReiDisplay(List.of(EntryIngredients.of(tierStack(MobFarmRegistrationAdapter.RAW_ESSENCE.get(), recipe.tier())),
                ingredient(recipe.amethyst()), ingredient(recipe.reagent())), outputs, holder.id().identifier(), false, recipe.tier(), recipe.duration());
    }
    private static EntryIngredient ingredient(SizedIngredient sized) {
        return EntryIngredient.of(sized.ingredient().getValues().stream()
                .map(item -> EntryStacks.of(new ItemStack(item, sized.count()))).toList());
    }
    public static EssenceReiDisplay synthesis(EssenceTier tier) {
        return new EssenceReiDisplay(List.of(EntryIngredients.of(tierStack(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get(), tier.id())),
                EntryIngredients.of(MobFarmRegistrationAdapter.MODEL_BASES.get(tier.id() - 1).get())),
                List.of(EntryIngredients.of(tierStack(MobFarmRegistrationAdapter.ENTITY_MODULE.get(), tier.id()))),
                Identifier.fromNamespaceAndPath("trading_cells", "essence_synthesis_" + tier.id()), true, tier.id(), tier.modelExperience());
    }
    private static ItemStack tierStack(Item item, int tier) {
        ItemStack stack = item.getDefaultInstance();
        CompoundTag root = new CompoundTag(), data = new CompoundTag();
        data.putInt("Tier", tier); root.put("TradingCellsEssence", data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }
    public boolean synthesis() { return synthesis; }
    public int tier() { return tier; }
    public int cost() { return cost; }
    @Override public CategoryIdentifier<EssenceReiDisplay> getCategoryIdentifier() { return synthesis ? SYNTHESIS : STABILIZATION; }
    @Override public DisplaySerializer<EssenceReiDisplay> getSerializer() { return SERIALIZER; }
}
