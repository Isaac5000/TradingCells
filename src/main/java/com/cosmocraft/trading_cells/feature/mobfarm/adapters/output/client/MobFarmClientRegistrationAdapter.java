package com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.client;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

public final class MobFarmClientRegistrationAdapter {
    private MobFarmClientRegistrationAdapter() { }
    public static void onRegisterModelProperties(net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath("trading_cells", "essence_tier"), EssenceTierProperty.CODEC);
        event.register(Identifier.fromNamespaceAndPath("trading_cells", "syringe_load"), SyringeLoadProperty.CODEC);
        event.register(Identifier.fromNamespaceAndPath("trading_cells", "syringe_extraction"), SyringeExtractionProperty.CODEC);
    }
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MobFarmRegistrationAdapter.MENU.get(), MobFarmScreen::new);
        event.register(MobFarmRegistrationAdapter.WORKBENCH_MENU.get(), EssenceWorkbenchScreen::new);
        event.register(MobFarmRegistrationAdapter.STABILIZER_MENU.get(), EssenceStabilizerScreen::new);
    }
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MobFarmRegistrationAdapter.BLOCK_ENTITY.get(), MobFarmBlockEntityRenderer::new);
    }
    public static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath("trading_cells", "entity_module"), EntityModuleItemRenderer.Unbaked.MAP_CODEC);
    }
}
