package com.cosmocraft.trading_cells.feature.trader.adapters.minecraft;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.catalog.SafeDynamicCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class VillagerPoiAdapter {
    private static final String VILLAGER_DATA_TAG = "VillagerData";
    private static final String PROFESSION_TAG = "profession";
    private static final String LEVEL_TAG = "level";
    private static final String XP_TAG = "Xp";
    private static final String AGE_TAG = "Age";
    private static final String NONE_PROFESSION = "minecraft:none";
    private static final List<Option> VANILLA_OPTIONS = List.of(
            new Option(Items.BLAST_FURNACE, "minecraft:armorer"),
            new Option(Items.SMOKER, "minecraft:butcher"),
            new Option(Items.CARTOGRAPHY_TABLE, "minecraft:cartographer"),
            new Option(Items.BREWING_STAND, "minecraft:cleric"),
            new Option(Items.COMPOSTER, "minecraft:farmer"),
            new Option(Items.BARREL, "minecraft:fisherman"),
            new Option(Items.FLETCHING_TABLE, "minecraft:fletcher"),
            new Option(Items.CAULDRON, "minecraft:leatherworker"),
            new Option(Items.LECTERN, "minecraft:librarian"),
            new Option(Items.STONECUTTER, "minecraft:mason"),
            new Option(Items.LOOM, "minecraft:shepherd"),
            new Option(Items.SMITHING_TABLE, "minecraft:toolsmith"),
            new Option(Items.GRINDSTONE, "minecraft:weaponsmith")
    );
    private static final AtomicReference<List<Option>> OPTIONS = new AtomicReference<>();

    private VillagerPoiAdapter() {
    }

    public static @Nullable String professionFor(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return options(level).stream()
                .filter(option -> stack.is(option.item()))
                .map(Option::professionId)
                .findFirst()
                .orElse(null);
    }

    public static List<Option> options(Level level) {
        List<Option> cached = OPTIONS.get();
        if (cached != null) {
            return cached;
        }
        List<Option> discovered = discoverOptions(level);
        OPTIONS.compareAndSet(null, discovered);
        return OPTIONS.get();
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

    private static List<Option> discoverOptions(Level level) {
        try {
            List<Holder.Reference<VillagerProfession>> professions = level.registryAccess()
                    .lookupOrThrow(Registries.VILLAGER_PROFESSION)
                    .listElements()
                    .filter(profession -> !profession.is(VillagerProfession.NONE))
                    .filter(profession -> !profession.is(VillagerProfession.NITWIT))
                    .toList();
            Set<Item> knownItems = new HashSet<>();
            VANILLA_OPTIONS.forEach(option -> knownItems.add(option.item()));
            List<Option> additional = SafeDynamicCatalog.discover(
                    "villager POIs and professions",
                    () -> BuiltInRegistries.ITEM,
                    item -> dynamicOption(item, knownItems, professions),
                    Comparator.comparing(option -> BuiltInRegistries.ITEM.getKey(option.item()).toString()),
                    item -> BuiltInRegistries.ITEM.getKey(item).toString()
            );
            ArrayList<Option> combined = new ArrayList<>(VANILLA_OPTIONS);
            combined.addAll(additional);
            return List.copyOf(combined);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "No se pudo ampliar la lista de POI y empleos; se usará la lista vanilla.",
                    exception
            );
            return VANILLA_OPTIONS;
        }
    }

    private static Optional<Option> dynamicOption(
            Item item,
            Set<Item> knownItems,
            List<Holder.Reference<VillagerProfession>> professions
    ) {
        if (knownItems.contains(item) || !(item instanceof BlockItem blockItem)) {
            return Optional.empty();
        }
        if (BuiltInRegistries.ITEM.getKey(item) == null) {
            throw new IllegalArgumentException("Villager POI has no registered item identifier");
        }
        return Optional.ofNullable(optionFor(item, blockItem.getBlock(), professions));
    }

    private static @Nullable Option optionFor(
            Item item,
            Block block,
            List<Holder.Reference<VillagerProfession>> professions
    ) {
        String profession = block.getStateDefinition()
                .getPossibleStates()
                .stream()
                .map(state -> professionFor(state, professions))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return profession == null ? null : new Option(item, profession);
    }

    private static @Nullable String professionFor(
            BlockState state,
            List<Holder.Reference<VillagerProfession>> professions
    ) {
        Holder<PoiType> poiType = PoiTypes.forState(state).orElse(null);
        if (poiType == null) {
            return null;
        }
        return professions.stream()
                .filter(profession -> profession.value().heldJobSite().test(poiType)
                        || profession.value().acquirableJobSite().test(poiType))
                .findFirst()
                .flatMap(Holder::unwrapKey)
                .map(key -> key.identifier().toString())
                .orElse(null);
    }

    public record Option(Item item, String professionId) {
        public Option {
            Objects.requireNonNull(item);
            Objects.requireNonNull(professionId);
        }
    }
}
