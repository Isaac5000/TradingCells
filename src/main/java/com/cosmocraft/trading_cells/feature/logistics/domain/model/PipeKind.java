package com.cosmocraft.trading_cells.feature.logistics.domain.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum PipeKind {
    ITEM(EnumSet.of(LogisticsResourceType.ITEM)),
    FLUID(EnumSet.of(LogisticsResourceType.FLUID)),
    GAS(EnumSet.of(LogisticsResourceType.GAS)),
    ENERGY(EnumSet.of(LogisticsResourceType.ENERGY)),
    UNIVERSAL(EnumSet.allOf(LogisticsResourceType.class));

    private final Set<LogisticsResourceType> resourceTypes;

    PipeKind(Set<LogisticsResourceType> resourceTypes) {
        this.resourceTypes = Set.copyOf(resourceTypes);
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean supports(LogisticsResourceType type) {
        return resourceTypes.contains(type);
    }

    public Set<LogisticsResourceType> resourceTypes() {
        return resourceTypes;
    }

    public boolean connectsTo(PipeKind other) {
        if (other == null) {
            return false;
        }
        for (LogisticsResourceType type : resourceTypes) {
            if (other.supports(type)) {
                return true;
            }
        }
        return false;
    }
}
