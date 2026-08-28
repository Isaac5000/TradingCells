package com.cosmocraft.trading_cells.feature.silktouch.adapters.output;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SilkTouchTwoLootModifier;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SilkTouchTwoRegistrationAdapter {
    public static final DeferredHolder<
            MapCodec<? extends IGlobalLootModifier>,
            MapCodec<SilkTouchTwoLootModifier>
            > LOOT_MODIFIER = Registration.GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(
                    "silk_touch_two",
                    () -> SilkTouchTwoLootModifier.CODEC
            );

    private SilkTouchTwoRegistrationAdapter() {
    }

    public static void load() {
        // Forces class loading so the deferred entry is created.
    }
}
