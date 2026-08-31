package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** A positional Arcane Infusion input which may deliberately require an empty slot. */
public record ArcaneInfusionIngredientSlot(Optional<SizedIngredient> ingredient) {
    private static final Codec<Boolean> EMPTY_CODEC = Codec.BOOL.fieldOf("empty").codec();

    public static final Codec<ArcaneInfusionIngredientSlot> CODEC = Codec.either(
            SizedIngredient.NESTED_CODEC,
            EMPTY_CODEC
    ).comapFlatMap(
            value -> value.map(
                    ingredient -> DataResult.success(present(ingredient)),
                    empty -> empty
                            ? DataResult.success(empty())
                            : DataResult.error(() -> "Arcane infusion empty slots must use {\"empty\": true}")
            ),
            slot -> slot.ingredient()
                    .<Either<SizedIngredient, Boolean>>map(Either::left)
                    .orElseGet(() -> Either.right(true))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionIngredientSlot> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, slot) -> {
                        buffer.writeBoolean(slot.ingredient().isPresent());
                        slot.ingredient().ifPresent(value -> SizedIngredient.STREAM_CODEC.encode(buffer, value));
                    },
                    buffer -> buffer.readBoolean()
                            ? present(SizedIngredient.STREAM_CODEC.decode(buffer))
                            : empty()
            );

    public ArcaneInfusionIngredientSlot {
        ingredient = ingredient == null ? Optional.empty() : ingredient;
    }

    public static ArcaneInfusionIngredientSlot present(SizedIngredient ingredient) {
        return new ArcaneInfusionIngredientSlot(Optional.of(ingredient));
    }

    public static ArcaneInfusionIngredientSlot empty() {
        return new ArcaneInfusionIngredientSlot(Optional.empty());
    }

    public boolean matches(ItemStack stack) {
        return ingredient.map(value -> value.test(stack)).orElseGet(stack::isEmpty);
    }

    public int count() {
        return ingredient.map(SizedIngredient::count).orElse(0);
    }
}
