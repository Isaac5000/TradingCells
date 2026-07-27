package com.cosmocraft.trading_cells.feature.captures.adapters.input;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Objects;

final class CapturedEntityTypes {
    private static final String MINECRAFT_NAMESPACE = "minecraft";

    private CapturedEntityTypes() {
    }

    static EntityType<Villager> villagerType() {
        return entityType("villager");
    }

    static EntityType<Piglin> piglinType() {
        return entityType("piglin");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityType<T> entityType(String path) {
        return (EntityType<T>) Objects.requireNonNull(
                BuiltInRegistries.ENTITY_TYPE.get(Identifier.fromNamespaceAndPath(MINECRAFT_NAMESPACE, path))
                        .orElseThrow()
                        .value(),
                "Missing entity type: " + path
        );
    }
}
