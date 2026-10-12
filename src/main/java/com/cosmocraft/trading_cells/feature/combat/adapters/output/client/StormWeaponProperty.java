package com.cosmocraft.trading_cells.feature.combat.adapters.output.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record StormWeaponProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<StormWeaponProperty> CODEC = MapCodec.unit(new StormWeaponProperty());
    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        var entity = owner == null ? null : owner.asLivingEntity();
        if (entity != null && level != null && com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem.animating(stack)) {
            float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            return 10 + StormArmAnimation.insertion(com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem.animationProgress(stack, level, partial));
        }
        if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
            float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            return 2 + ((entity.getTicksUsingItem() + partial) % 6) / 6F;
        }
        return com.cosmocraft.trading_cells.feature.combat.adapters.minecraft.StormWeaponItem.battery(stack).isEmpty() ? 0 : 1;
    }
    @Override public MapCodec<StormWeaponProperty> type() { return CODEC; }
}
