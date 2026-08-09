package com.cosmocraft.trading_cells.feature.farmer.domain.model;

import java.util.List;

public enum FarmerCrop {
    NONE,
    WHEAT,
    CARROT,
    POTATO,
    BEETROOT,
    CRIMSON_FUNGUS,
    WARPED_FUNGUS,
    CRIMSON_ROOTS,
    NETHER_WART,
    WEEPING_VINES,
    NETHER_SPROUTS,
    WARPED_ROOTS,
    TWISTING_VINES;

    private static final List<FarmerCrop> VILLAGER_CROPS = List.of(
            WHEAT,
            CARROT,
            POTATO,
            BEETROOT
    );
    private static final List<FarmerCrop> PIGLIN_CROPS = List.of(
            CRIMSON_FUNGUS,
            WARPED_FUNGUS,
            CRIMSON_ROOTS,
            NETHER_WART,
            WEEPING_VINES,
            NETHER_SPROUTS,
            WARPED_ROOTS,
            TWISTING_VINES
    );

    public static List<FarmerCrop> supportedBy(FarmerKind kind) {
        return kind == FarmerKind.VILLAGER ? VILLAGER_CROPS : PIGLIN_CROPS;
    }

    public boolean isSupportedBy(FarmerKind kind) {
        return supportedBy(kind).contains(this);
    }
}
