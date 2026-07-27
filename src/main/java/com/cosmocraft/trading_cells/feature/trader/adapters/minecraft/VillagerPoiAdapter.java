package com.cosmocraft.trading_cells.feature.trader.adapters.minecraft;

import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class VillagerPoiAdapter {
    private static final String VILLAGER_DATA_TAG = "VillagerData";
    private static final String PROFESSION_TAG = "profession";
    private static final String LEVEL_TAG = "level";
    private static final String XP_TAG = "Xp";
    private static final String AGE_TAG = "Age";
    private static final String NONE_PROFESSION = "minecraft:none";

    private VillagerPoiAdapter() {
    }

    public static @Nullable String professionFor(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return blockItem.getBlock()
                .getStateDefinition()
                .getPossibleStates()
                .stream()
                .map(state -> professionFor(level, state))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static boolean hasPersistentProfession(CompoundTag villagerData) {
        if (villagerData.getInt(XP_TAG).orElse(0) > 0) {
            return true;
        }
        return villagerData.getCompound(VILLAGER_DATA_TAG)
                .flatMap(data -> data.getInt(LEVEL_TAG))
                .map(level -> level > 1)
                .orElse(false);
    }

    public static boolean refreshProfession(CompoundTag villagerData, @Nullable String professionId) {
        if (hasPersistentProfession(villagerData)) {
            return false;
        }
        if (villagerData.getInt(AGE_TAG).orElse(0) < 0 || professionId == null) {
            return clearTransientProfession(villagerData);
        }
        return setProfession(villagerData, professionId);
    }

    public static boolean clearTransientProfession(CompoundTag villagerData) {
        if (!hasPersistentProfession(villagerData)) {
            boolean changed = setProfession(villagerData, NONE_PROFESSION);
            if (villagerData.getInt(XP_TAG).orElse(0) != 0) {
                villagerData.putInt(XP_TAG, 0);
                changed = true;
            }
            return changed;
        }
        return false;
    }

    private static boolean setProfession(CompoundTag villagerData, String professionId) {
        CompoundTag data = villagerData.getCompound(VILLAGER_DATA_TAG).map(CompoundTag::copy).orElseGet(CompoundTag::new);
        String previousProfession = data.getString(PROFESSION_TAG).orElse(NONE_PROFESSION);
        boolean changed = !professionId.equals(previousProfession);
        data.putString(PROFESSION_TAG, professionId);
        if (data.getInt(LEVEL_TAG).isEmpty()) {
            data.putInt(LEVEL_TAG, 1);
        }
        villagerData.put(VILLAGER_DATA_TAG, data);
        if (changed) {
            villagerData.remove("Offers");
        }
        return changed;
    }

    private static @Nullable String professionFor(Level level, BlockState state) {
        Holder<PoiType> poiType = PoiTypes.forState(state).orElse(null);
        if (poiType == null) {
            return null;
        }
        return level.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .listElements()
                .filter(profession -> !profession.is(VillagerProfession.NONE))
                .filter(profession -> !profession.is(VillagerProfession.NITWIT))
                .filter(profession -> profession.value().heldJobSite().test(poiType)
                        || profession.value().acquirableJobSite().test(poiType))
                .findFirst()
                .flatMap(Holder::unwrapKey)
                .map(key -> key.identifier().toString())
                .orElse(null);
    }
}
