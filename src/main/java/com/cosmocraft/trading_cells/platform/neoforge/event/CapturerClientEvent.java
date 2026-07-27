package com.cosmocraft.trading_cells.platform.neoforge.event;

import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.client.BreederBlockEntityRenderer;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.PiglinBarteringCellBlockEntityRenderer;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.client.PiglinCapturerItemRenderSupport;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.client.TradingCellBlockEntityRenderer;
import com.cosmocraft.trading_cells.feature.captures.adapters.output.client.VillagerCapturerItemRenderSupport;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.client.render.BlockEntityItemRenderSupport;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

public final class CapturerClientEvent {
    private CapturerClientEvent() {
    }

    // onRenderHand removed; rendering is handled by model-selected SpecialModelRenderers.

    public static void onRegisterSpecialModelRenderer(RegisterSpecialModelRendererEvent event) {
        register(event, "villager_capturer", VillagerCapturerItemRenderSupport.Default.Unbaked.MAP_CODEC);
        register(event, "villager_capturer_gui", VillagerCapturerItemRenderSupport.Gui.Unbaked.MAP_CODEC);
        register(event, "villager_capturer_fixed", VillagerCapturerItemRenderSupport.Fixed.Unbaked.MAP_CODEC);
        register(event, "villager_capturer_on_shelf", VillagerCapturerItemRenderSupport.OnShelf.Unbaked.MAP_CODEC);
        register(event, "villager_capturer_third_person", VillagerCapturerItemRenderSupport.ThirdPerson.Unbaked.MAP_CODEC);
        register(event, "villager_capturer_first_person", VillagerCapturerItemRenderSupport.FirstPerson.Unbaked.MAP_CODEC);
        // Piglin special renderers (reuse same per-profile pattern)
        register(event, "piglin_capturer", PiglinCapturerItemRenderSupport.Default.Unbaked.MAP_CODEC);
        register(event, "piglin_capturer_gui", PiglinCapturerItemRenderSupport.Gui.Unbaked.MAP_CODEC);
        register(event, "piglin_capturer_fixed", PiglinCapturerItemRenderSupport.Fixed.Unbaked.MAP_CODEC);
        register(event, "piglin_capturer_on_shelf", PiglinCapturerItemRenderSupport.OnShelf.Unbaked.MAP_CODEC);
        register(event, "piglin_capturer_third_person", PiglinCapturerItemRenderSupport.ThirdPerson.Unbaked.MAP_CODEC);
        register(event, "piglin_capturer_first_person", PiglinCapturerItemRenderSupport.FirstPerson.Unbaked.MAP_CODEC);
        register(event, "block_entity_item", BlockEntityItemRenderSupport.Unbaked.MAP_CODEC);
    }

    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                TraderRegistrationAdapter.VILLAGER_TRADING_CELL_BLOCK_ENTITY.get(),
                TradingCellBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                TraderRegistrationAdapter.PIGLIN_BARTERING_CELL_BLOCK_ENTITY.get(),
                PiglinBarteringCellBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                BreederRegistrationAdapter.VILLAGER_BREEDER_BLOCK_ENTITY.get(),
                BreederBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                BreederRegistrationAdapter.PIGLIN_BREEDER_BLOCK_ENTITY.get(),
                BreederBlockEntityRenderer::new
        );
    }

    private static void register(RegisterSpecialModelRendererEvent event, String path, com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<?>> codec) {
        Identifier id = Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
        event.register(id, codec);
    }
}
