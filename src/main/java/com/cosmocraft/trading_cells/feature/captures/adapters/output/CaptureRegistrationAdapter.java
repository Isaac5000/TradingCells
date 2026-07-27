package com.cosmocraft.trading_cells.feature.captures.adapters.output;

import com.cosmocraft.trading_cells.feature.captures.adapters.input.PiglinCapturerItem;
import com.cosmocraft.trading_cells.feature.captures.adapters.input.VillagerCapturerItem;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public final class CaptureRegistrationAdapter {
    private static final String VILLAGER_CAPTURER_ID = "villager_capturer";
    private static final String PIGLIN_CAPTURER_ID = "piglin_capturer";

    public static final DeferredItem<VillagerCapturerItem> VILLAGER_CAPTURER_ITEM =
            Registration.ITEMS.register(VILLAGER_CAPTURER_ID, () -> new VillagerCapturerItem(
                    capturerProperties(VILLAGER_CAPTURER_ID)
            ));

    public static final DeferredItem<PiglinCapturerItem> PIGLIN_CAPTURER_ITEM =
            Registration.ITEMS.register(PIGLIN_CAPTURER_ID, () -> new PiglinCapturerItem(
                    capturerProperties(PIGLIN_CAPTURER_ID)
            ));

    private CaptureRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so the DeferredRegister entries are created.
    }

    private static Item.Properties capturerProperties(String id) {
        return new Item.Properties()
                .setId(ResourceKey.create(
                        Registries.ITEM,
                        Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)
                ))
                .stacksTo(64);
    }
}
