package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederFood;
import com.cosmocraft.trading_cells.feature.breeders.domain.model.BreederKind;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.catalog.SafeDynamicCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MinecraftBreederFood {
    private static final int BREAD_EQUIVALENT_FOOD_POINTS = 4;
    private static final List<Option> VANILLA_VILLAGER_OPTIONS = List.of(
            new Option(Items.BREAD, BreederFood.BREAD),
            new Option(Items.CARROT, BreederFood.VEGETABLE),
            new Option(Items.POTATO, BreederFood.VEGETABLE),
            new Option(Items.BEETROOT, BreederFood.VEGETABLE)
    );
    private static final List<Option> PIGLIN_OPTIONS = List.of(
            new Option(Items.COOKED_PORKCHOP, BreederFood.COOKED_PORKCHOP),
            new Option(Items.NETHER_WART_BLOCK, BreederFood.NETHER_WART_BLOCK),
            new Option(Items.PORKCHOP, BreederFood.RAW_PORKCHOP),
            new Option(Items.CRIMSON_FUNGUS, BreederFood.CRIMSON_FUNGUS),
            new Option(Items.NETHER_WART, BreederFood.NETHER_WART)
    );
    private static final AtomicReference<List<Option>> VILLAGER_OPTIONS = new AtomicReference<>();

    private MinecraftBreederFood() {
    }

    public static BreederFood from(BreederKind kind, ItemStack stack) {
        for (Option option : options(kind)) {
            if (stack.is(option.item())) {
                return option.food();
            }
        }
        return BreederFood.NONE;
    }

    public static List<Option> options(BreederKind kind) {
        return kind == BreederKind.VILLAGER ? villagerOptions() : PIGLIN_OPTIONS;
    }

    private static List<Option> villagerOptions() {
        List<Option> cached = VILLAGER_OPTIONS.get();
        if (cached != null) {
            return cached;
        }
        List<Option> discovered = discoverVillagerOptions();
        VILLAGER_OPTIONS.compareAndSet(null, discovered);
        return VILLAGER_OPTIONS.get();
    }

    private static List<Option> discoverVillagerOptions() {
        try {
            Set<Item> knownItems = new HashSet<>();
            VANILLA_VILLAGER_OPTIONS.forEach(option -> knownItems.add(option.item()));
            List<Option> additional = SafeDynamicCatalog.discover(
                    "villager breeding foods",
                    () -> Villager.FOOD_POINTS.entrySet(),
                    entry -> dynamicVillagerOption(entry, knownItems),
                    Comparator.comparing(option -> BuiltInRegistries.ITEM.getKey(option.item()).toString()),
                    entry -> String.valueOf(BuiltInRegistries.ITEM.getKey(entry.getKey()))
            );
            ArrayList<Option> combined = new ArrayList<>(VANILLA_VILLAGER_OPTIONS);
            combined.addAll(additional);
            return List.copyOf(combined);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "No se pudo ampliar la lista de alimentos de aldeano; se usará la lista vanilla.",
                    exception
            );
            return VANILLA_VILLAGER_OPTIONS;
        }
    }

    private static Optional<Option> dynamicVillagerOption(
            Map.Entry<Item, Integer> entry,
            Set<Item> knownItems
    ) {
        Item item = entry.getKey();
        Integer foodPoints = entry.getValue();
        if (item == null || foodPoints == null || foodPoints <= 0 || knownItems.contains(item)) {
            return Optional.empty();
        }
        if (BuiltInRegistries.ITEM.getKey(item) == null) {
            throw new IllegalArgumentException("Villager breeding food has no registered item identifier");
        }
        BreederFood food = foodPoints >= BREAD_EQUIVALENT_FOOD_POINTS
                ? BreederFood.BREAD
                : BreederFood.VEGETABLE;
        return Optional.of(new Option(item, food));
    }

    public record Option(Item item, BreederFood food) {
    }
}
