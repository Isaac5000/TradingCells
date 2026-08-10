package com.cosmocraft.trading_cells.feature.captures.adapters.api;

import com.cosmocraft.trading_cells.feature.captures.adapters.input.PiglinCapturerItem;
import com.cosmocraft.trading_cells.feature.captures.adapters.input.VillagerCapturerItem;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.CaptureRegistrationAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

public final class CapturedMobStackAdapter {
    private static final String NEOFORGE_DATA_TAG = "NeoForgeData";
    private static final String VILLAGER_DATA_TAG = "VillagerData";
    private static final String VILLAGER_PROFESSION_TAG = "profession";

    private CapturedMobStackAdapter() {
    }

    public static boolean isBaby(CapturedMobKind kind, ItemStack stack) {
        CompoundTag data = copyData(kind, stack);
        return data != null && isBaby(kind, data);
    }

    public static boolean isBaby(CapturedMobKind kind, CompoundTag data) {
        return kind == CapturedMobKind.VILLAGER
                ? VillagerCapturerItem.isBabyVillager(data)
                : PiglinCapturerItem.isBabyPiglin(data);
    }

    public static boolean isCapturer(CapturedMobKind kind, ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(capturerItem(kind)) || stack.is(unbreakableCapturerItem(kind)));
    }

    public static boolean isFilledCapturer(CapturedMobKind kind, ItemStack stack) {
        if (!isCapturer(kind, stack)) {
            return false;
        }
        return kind == CapturedMobKind.VILLAGER
                ? VillagerCapturerItem.hasCapturedVillager(stack)
                : PiglinCapturerItem.hasCapturedPiglin(stack);
    }

    public static ItemStack mature(CapturedMobKind kind, ItemStack source) {
        CompoundTag adultData = copyData(kind, source);
        if (adultData == null) {
            return ItemStack.EMPTY;
        }

        if (kind == CapturedMobKind.VILLAGER) {
            adultData.putInt("Age", 0);
            adultData.putInt("ForcedAge", 0);
            adultData.remove("AgeLocked");
        } else {
            adultData.putBoolean("IsBaby", false);
            if (adultData.contains("Age")) {
                adultData.putInt("Age", 0);
            }
        }

        ItemStack adult = source.copy();
        adult.setCount(1);
        setData(kind, adult, adultData);
        return adult;
    }

    public static @Nullable Entity createEntity(CapturedMobKind kind, Level level, ItemStack stack, BlockPos pos) {
        CompoundTag data = copyData(kind, stack);
        return data == null ? null : createEntity(kind, level, data, pos);
    }

    public static @Nullable Entity createEntity(
            CapturedMobKind kind,
            Level level,
            CompoundTag data,
            BlockPos pos
    ) {
        return kind == CapturedMobKind.VILLAGER
                ? VillagerCapturerItem.createCapturedVillager(level, data, pos)
                : PiglinCapturerItem.createCapturedPiglin(level, data, pos);
    }

    public static @Nullable Villager createVillager(Level level, ItemStack stack, BlockPos pos) {
        return VillagerCapturerItem.createCapturedVillager(level, stack, pos);
    }

    public static @Nullable Villager createVillager(Level level, CompoundTag data, BlockPos pos) {
        return VillagerCapturerItem.createCapturedVillager(level, data, pos);
    }

    public static @Nullable Piglin createPiglin(Level level, ItemStack stack, BlockPos pos) {
        return PiglinCapturerItem.createCapturedPiglin(level, stack, pos);
    }

    public static @Nullable Piglin createPiglin(Level level, CompoundTag data, BlockPos pos) {
        return PiglinCapturerItem.createCapturedPiglin(level, data, pos);
    }

    public static CompoundTag createVillagerData(Villager villager) {
        return VillagerCapturerItem.createCapturedVillagerData(villager);
    }

    public static EntityType<Villager> villagerType() {
        return VillagerCapturerItem.capturedVillagerType();
    }

    public static Item capturerItem(CapturedMobKind kind) {
        return kind == CapturedMobKind.VILLAGER
                ? CaptureRegistrationAdapter.VILLAGER_CAPTURER_ITEM.get()
                : CaptureRegistrationAdapter.PIGLIN_CAPTURER_ITEM.get();
    }

    public static Item unbreakableCapturerItem(CapturedMobKind kind) {
        return kind == CapturedMobKind.VILLAGER
                ? CaptureRegistrationAdapter.UNBREAKABLE_VILLAGER_CAPTURER_ITEM.get()
                : CaptureRegistrationAdapter.UNBREAKABLE_PIGLIN_CAPTURER_ITEM.get();
    }

    public static @Nullable CompoundTag copyData(CapturedMobKind kind, ItemStack stack) {
        if (!isFilledCapturer(kind, stack)) {
            return null;
        }
        return kind == CapturedMobKind.VILLAGER
                ? VillagerCapturerItem.getCapturedVillagerData(stack)
                : PiglinCapturerItem.getCapturedPiglinData(stack);
    }

    public static void setData(CapturedMobKind kind, ItemStack stack, CompoundTag data) {
        if (kind == CapturedMobKind.VILLAGER) {
            VillagerCapturerItem.setCapturedVillagerData(stack, data);
        } else {
            PiglinCapturerItem.setCapturedPiglinData(stack, data);
        }
    }

    public static Optional<Identifier> villagerProfession(ItemStack stack) {
        CompoundTag entityData = copyData(CapturedMobKind.VILLAGER, stack);
        if (entityData == null) {
            return Optional.empty();
        }
        return entityData.getCompound(VILLAGER_DATA_TAG)
                .flatMap(data -> data.getString(VILLAGER_PROFESSION_TAG))
                .map(Identifier::tryParse)
                .filter(Objects::nonNull);
    }

    public static boolean hasVillagerProfession(ItemStack stack, Identifier profession) {
        return villagerProfession(stack).filter(profession::equals).isPresent();
    }

    public static ItemStack withVillagerProfession(ItemStack source, Identifier profession) {
        CompoundTag entityData = copyData(CapturedMobKind.VILLAGER, source);
        if (entityData == null) {
            return ItemStack.EMPTY;
        }
        Optional<CompoundTag> villagerData = entityData.getCompound(VILLAGER_DATA_TAG);
        if (villagerData.isEmpty()) {
            return ItemStack.EMPTY;
        }

        CompoundTag updatedVillagerData = villagerData.get().copy();
        updatedVillagerData.putString(VILLAGER_PROFESSION_TAG, profession.toString());
        entityData.put(VILLAGER_DATA_TAG, updatedVillagerData);
        ItemStack result = source.copyWithCount(1);
        setData(CapturedMobKind.VILLAGER, result, entityData);
        return result;
    }

    public static boolean updatePersistentInt(
            CapturedMobKind kind,
            ItemStack stack,
            String key,
            IntUnaryOperator updater
    ) {
        CompoundTag entityData = copyData(kind, stack);
        if (entityData == null) {
            return false;
        }

        CompoundTag persistentData = entityData.getCompound(NEOFORGE_DATA_TAG)
                .map(CompoundTag::copy)
                .orElseGet(CompoundTag::new);
        int current = persistentData.getInt(key).orElse(0);
        persistentData.putInt(key, updater.applyAsInt(current));
        entityData.put(NEOFORGE_DATA_TAG, persistentData);
        setData(kind, stack, entityData);
        return true;
    }

    public static void clearData(CapturedMobKind kind, ItemStack stack) {
        if (kind == CapturedMobKind.VILLAGER) {
            VillagerCapturerItem.clearCapturedVillager(stack);
        } else {
            PiglinCapturerItem.clearCapturedPiglin(stack);
        }
    }
}
