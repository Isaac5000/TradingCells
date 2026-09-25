package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EssenceTierProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<EssenceTierProperty> CODEC = MapCodec.unit(new EssenceTierProperty());

    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return EntityEssenceData.tier(stack).id();
    }

    @Override public MapCodec<EssenceTierProperty> type() { return CODEC; }
}
