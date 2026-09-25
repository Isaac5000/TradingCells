package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = "trading_cells_gametest")
public final class EssenceTestEntities {
    private static final Map<String, EntityType<Zombie>> TYPES = new LinkedHashMap<>();
    private EssenceTestEntities() { }

    @SubscribeEvent public static void register(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ENTITY_TYPE)) { return; }
        for (String name : new String[]{"external_elite", "external_boss", "external_override"}) {
            Identifier id = Identifier.fromNamespaceAndPath("trading_cells_gametest", name);
            EntityType<Zombie> type = EntityType.Builder.<Zombie>of(Zombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).build(ResourceKey.create(Registries.ENTITY_TYPE, id));
            TYPES.put(name, type);
            event.register(Registries.ENTITY_TYPE, id, () -> type);
        }
    }

    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        TYPES.values().forEach(type -> event.put(type, Zombie.createAttributes().build()));
    }
}
