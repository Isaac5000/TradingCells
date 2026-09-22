package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.output.SkeletonFarmRegistrationAdapter;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class SkeletonFarmClientRegistrationAdapter {
    private SkeletonFarmClientRegistrationAdapter() {
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SkeletonFarmRegistrationAdapter.MENU.get(), SkeletonFarmScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                SkeletonFarmRegistrationAdapter.BLOCK_ENTITY.get(),
                SkeletonFarmBlockEntityRenderer::new
        );
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        event.getBakingResult().itemStackModels().computeIfPresent(
                Identifier.fromNamespaceAndPath("trading_cells", "storm_shard"),
                (id, model) -> (state, stack, resolver, context, level, owner, seed) -> {
                    model.update(state, stack, resolver, context, level, owner, seed);
                    // Special renderers are otherwise cached as static inventory sprites.
                    state.setAnimated();
                });
    }
}
