package com.cosmocraft.trading_cells.platform.neoforge.catalog;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Builds deterministic registry-backed catalog extensions without risking their fixed entries. */
public final class SafeDynamicCatalog {
    private SafeDynamicCatalog() {
    }

    public static <S, T> List<T> discover(
            String catalogName,
            Supplier<? extends Iterable<? extends S>> sources,
            Function<? super S, Optional<T>> mapper,
            Comparator<? super T> comparator,
            Function<? super S, String> sourceDescription
    ) {
        Objects.requireNonNull(catalogName);
        Objects.requireNonNull(sources);
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(comparator);
        Objects.requireNonNull(sourceDescription);

        try {
            Iterable<? extends S> candidates = Objects.requireNonNull(sources.get());
            Iterator<? extends S> iterator = Objects.requireNonNull(candidates.iterator());
            List<T> discovered = new ArrayList<>();
            int entryIndex = 0;
            while (iterator.hasNext()) {
                S source = iterator.next();
                entryIndex++;
                try {
                    Optional<T> mapped = Objects.requireNonNull(mapper.apply(source));
                    mapped.ifPresent(discovered::add);
                } catch (RuntimeException | LinkageError exception) {
                    TradingCells.LOGGER.warn(
                            "Discarding invalid entry '{}' from dynamic catalog '{}'.",
                            describe(source, entryIndex, sourceDescription),
                            catalogName,
                            exception
                    );
                }
            }
            discovered.sort(comparator);
            return List.copyOf(discovered);
        } catch (RuntimeException | LinkageError exception) {
            TradingCells.LOGGER.warn(
                    "Dynamic catalog '{}' could not be completed; its dynamic section will be ignored.",
                    catalogName,
                    exception
            );
            return List.of();
        }
    }

    private static <S> String describe(
            S source,
            int entryIndex,
            Function<? super S, String> sourceDescription
    ) {
        try {
            String description = sourceDescription.apply(source);
            return description == null || description.isBlank()
                    ? "#" + entryIndex
                    : description;
        } catch (RuntimeException | LinkageError ignored) {
            return "#" + entryIndex;
        }
    }
}
