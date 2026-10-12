package com.cosmocraft.trading_cells.platform.neoforge.item;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Display-only copies. Never write these snapshots back to inventory or persistence. */
public final class EntityItemPreviewData {
    private EntityItemPreviewData() { }

    public static ItemStack compact(ItemStack source) {
        if (source.isEmpty() || !source.has(DataComponents.CUSTOM_DATA)) { return source; }
        boolean capture = CapturedMobStackAdapter.isCapturer(CapturedMobKind.VILLAGER, source)
                || CapturedMobStackAdapter.isCapturer(CapturedMobKind.PIGLIN, source);
        if (!capture) { return source; }
        CompoundTag data = source.get(DataComponents.CUSTOM_DATA).copyTag();
        if (data.isEmpty()) { return source; }
        CompoundTag preview = new CompoundTag();
        preview.putInt("Age", data.getIntOr("Age", 0));
        preview.putBoolean("IsBaby", data.getBooleanOr("IsBaby", false));
        copyShortString(data, preview, "CustomName");
        data.getCompound("VillagerData").ifPresent(villager -> {
            CompoundTag appearance = new CompoundTag();
            copyShortString(villager, appearance, "type");
            copyShortString(villager, appearance, "profession");
            appearance.putInt("level", villager.getIntOr("level", 1));
            preview.put("VillagerData", appearance);
        });
        ItemStack result = source.copy();
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(preview));
        return result;
    }

    private static void copyShortString(CompoundTag source, CompoundTag target, String key) {
        source.getString(key).filter(value -> value.length() <= 160).ifPresent(value -> target.putString(key, value));
    }
}
