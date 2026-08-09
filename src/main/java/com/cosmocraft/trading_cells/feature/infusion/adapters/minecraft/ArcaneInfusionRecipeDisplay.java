package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record ArcaneInfusionRecipeDisplay(
        SlotDisplay top,
        SlotDisplay left,
        SlotDisplay center,
        SlotDisplay right,
        SlotDisplay bottom,
        SlotDisplay result,
        SlotDisplay craftingStation,
        int experience
) implements RecipeDisplay {
    public static final MapCodec<ArcaneInfusionRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SlotDisplay.CODEC.fieldOf("top").forGetter(ArcaneInfusionRecipeDisplay::top),
                    SlotDisplay.CODEC.fieldOf("left").forGetter(ArcaneInfusionRecipeDisplay::left),
                    SlotDisplay.CODEC.fieldOf("center").forGetter(ArcaneInfusionRecipeDisplay::center),
                    SlotDisplay.CODEC.fieldOf("right").forGetter(ArcaneInfusionRecipeDisplay::right),
                    SlotDisplay.CODEC.fieldOf("bottom").forGetter(ArcaneInfusionRecipeDisplay::bottom),
                    SlotDisplay.CODEC.fieldOf("result").forGetter(ArcaneInfusionRecipeDisplay::result),
                    SlotDisplay.CODEC.fieldOf("crafting_station")
                            .forGetter(ArcaneInfusionRecipeDisplay::craftingStation),
                    com.mojang.serialization.Codec.INT.fieldOf("experience")
                            .forGetter(ArcaneInfusionRecipeDisplay::experience)
            ).apply(instance, ArcaneInfusionRecipeDisplay::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionRecipeDisplay> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, display) -> {
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.top());
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.left());
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.center());
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.right());
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.bottom());
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.result());
                        SlotDisplay.STREAM_CODEC.encode(buffer, display.craftingStation());
                        buffer.writeVarInt(display.experience());
                    },
                    buffer -> new ArcaneInfusionRecipeDisplay(
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            SlotDisplay.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt()
                    )
            );

    @Override
    public Type<? extends RecipeDisplay> type() {
        return ArcaneInfuserRegistrationAdapter.RECIPE_DISPLAY_TYPE.get();
    }
}
