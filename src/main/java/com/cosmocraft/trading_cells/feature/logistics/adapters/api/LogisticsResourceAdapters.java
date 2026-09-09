package com.cosmocraft.trading_cells.feature.logistics.adapters.api;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;

public final class LogisticsResourceAdapters {
    private static final List<LogisticsResourceAdapter<?, ?>> ADAPTERS = new CopyOnWriteArrayList<>();

    private LogisticsResourceAdapters() {
    }

    public static void register(LogisticsResourceAdapter<?, ?> adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Logistics adapter cannot be null");
        }
        for (LogisticsResourceAdapter<?, ?> registered : ADAPTERS) {
            if (registered.id().equals(adapter.id())) {
                throw new IllegalArgumentException("Duplicate logistics adapter " + adapter.id());
            }
        }
        ADAPTERS.add(adapter);
    }

    public static List<LogisticsResourceAdapter<?, ?>> forType(LogisticsResourceType type) {
        List<LogisticsResourceAdapter<?, ?>> result = new ArrayList<>();
        for (LogisticsResourceAdapter<?, ?> adapter : ADAPTERS) {
            if (adapter.type() == type) {
                result.add(adapter);
            }
        }
        return List.copyOf(result);
    }

    public static LogisticsResourceAdapter<?, ?> byId(Identifier id) {
        for (LogisticsResourceAdapter<?, ?> adapter : ADAPTERS) {
            if (adapter.id().equals(id)) {
                return adapter;
            }
        }
        return null;
    }

    public static List<LogisticsResourceAdapter<?, ?>> all() {
        return List.copyOf(ADAPTERS);
    }
}
