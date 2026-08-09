package com.cosmocraft.trading_cells.feature.breeders.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.catalog.SafeDynamicCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class VillagerVariantSelection {
    private static final List<String> VANILLA_VARIANT_IDS = List.of(
            "minecraft:plains",
            "minecraft:desert",
            "minecraft:jungle",
            "minecraft:savanna",
            "minecraft:snow",
            "minecraft:swamp",
            "minecraft:taiga"
    );
    private static final AtomicReference<List<String>> VARIANT_IDS = new AtomicReference<>();

    private VillagerVariantSelection() {
    }

    public static int count() {
        return variants().size();
    }

    public static int normalize(int index) {
        return Math.floorMod(index, count());
    }

    public static String id(int index) {
        return variants().get(normalize(index));
    }

    public static int indexOf(String id) {
        int index = variants().indexOf(id);
        return index < 0 ? 0 : index;
    }

    public static Component displayName(int index) {
        String id = id(index);
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return Component.literal(id);
        }
        String path = identifier.getPath();
        if ("minecraft".equals(identifier.getNamespace()) && VANILLA_VARIANT_IDS.contains(id)) {
            return Component.translatable("variant.trading_cells." + path);
        }
        return Component.translatableWithFallback(
                "villager_type." + identifier.getNamespace() + "." + path,
                readableName(path)
        );
    }

    public static List<String> variants() {
        List<String> cached = VARIANT_IDS.get();
        if (cached != null) {
            return cached;
        }
        List<String> discovered = discoverVariants();
        VARIANT_IDS.compareAndSet(null, discovered);
        return VARIANT_IDS.get();
    }

    private static List<String> discoverVariants() {
        try {
            Set<String> vanillaIds = new HashSet<>(VANILLA_VARIANT_IDS);
            List<String> additional = SafeDynamicCatalog.discover(
                    "villager biome skins",
                    () -> BuiltInRegistries.VILLAGER_TYPE.keySet(),
                    id -> dynamicVariant(id, vanillaIds),
                    Comparator.naturalOrder(),
                    Identifier::toString
            );
            ArrayList<String> combined = new ArrayList<>(VANILLA_VARIANT_IDS);
            combined.addAll(additional);
            return List.copyOf(combined);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "No se pudo ampliar la lista de skins de aldeano; se usará la lista vanilla.",
                    exception
            );
            return VANILLA_VARIANT_IDS;
        }
    }

    private static Optional<String> dynamicVariant(Identifier id, Set<String> vanillaIds) {
        if (id == null) {
            throw new IllegalArgumentException("Villager variant has no identifier");
        }
        String value = id.toString();
        return vanillaIds.contains(value) ? Optional.empty() : Optional.of(value);
    }

    private static String readableName(String path) {
        String[] words = path.replace('/', '_').split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            name.append(word.substring(1));
        }
        return name.isEmpty() ? path : name.toString();
    }
}
