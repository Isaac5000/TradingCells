package com.cosmocraft.trading_cells.feature.combat.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.mojang.authlib.GameProfile;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Adds at most one real vanilla or conventionally registered modded head per victim. */
public final class DecapitationLootAdapter {
    private static final List<String> CONVENTIONAL_HEAD_SUFFIXES = List.of("_head", "_skull");
    private static final List<String> CONVENTIONAL_HEAD_PREFIXES = List.of("head_", "skull_");
    private static final Map<EntityType<?>, Optional<Item>> MODDED_HEAD_CACHE = new IdentityHashMap<>();

    private DecapitationLootAdapter() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DecapitationLootAdapter::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(DecapitationLootAdapter::onTagsUpdated);
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            weapon = attacker.getMainHandItem();
        }
        int decapitation = CombatEnchantments.decapitationLevel(weapon, attacker.registryAccess());
        if (decapitation <= 0) {
            return;
        }

        LivingEntity victim = event.getEntity();
        ItemStack head = headFor(victim);
        if (head.isEmpty() || alreadyDropped(event, head.getItem())) {
            return;
        }

        int looting = CombatEnchantments.lootingLevel(weapon, attacker.registryAccess());
        double chance = victim.getType() == EntityTypes.WITHER_SKELETON
                ? DecapitationRules.supplementalNativeHeadChance(looting, decapitation)
                : DecapitationRules.decapitationHeadChance(decapitation);
        if (victim.getRandom().nextDouble() >= chance) {
            return;
        }

        event.getDrops().add(new ItemEntity(
                victim.level(),
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                head
        ));
    }

    public static void onTagsUpdated(TagsUpdatedEvent.ServerDataLoad event) {
        MODDED_HEAD_CACHE.clear();
    }

    /** Shared by world drops and detached simulations, including conventionally named modded heads. */
    public static ItemStack headFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            return playerHead(player.getGameProfile());
        }

        EntityType<?> type = victim.getType();
        if (type == EntityTypes.WITHER_SKELETON) {
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        }
        if (type == EntityTypes.SKELETON
                || type == EntityTypes.STRAY
                || type == EntityTypes.BOGGED
                || type == EntityTypes.PARCHED) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        if (type == EntityTypes.ZOMBIE
                || type == EntityTypes.ZOMBIE_VILLAGER
                || type == EntityTypes.HUSK
                || type == EntityTypes.DROWNED) {
            return new ItemStack(Items.ZOMBIE_HEAD);
        }
        if (type == EntityTypes.CREEPER) {
            return new ItemStack(Items.CREEPER_HEAD);
        }
        if (type == EntityTypes.PIGLIN
                || type == EntityTypes.PIGLIN_BRUTE
                || type == EntityTypes.ZOMBIFIED_PIGLIN) {
            return new ItemStack(Items.PIGLIN_HEAD);
        }
        if (type == EntityTypes.ENDER_DRAGON) {
            return new ItemStack(Items.DRAGON_HEAD);
        }
        return conventionalModdedHead(type);
    }

    private static ItemStack playerHead(GameProfile profile) {
        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        ResolvableProfile resolvableProfile = profile.properties().containsKey("textures")
                ? ResolvableProfile.createResolved(profile)
                : ResolvableProfile.createUnresolved(profile.name());
        playerHead.set(DataComponents.PROFILE, resolvableProfile);
        return playerHead;
    }

    private static ItemStack conventionalModdedHead(EntityType<?> entityType) {
        return MODDED_HEAD_CACHE.computeIfAbsent(entityType, DecapitationLootAdapter::findConventionalModdedHead)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static Optional<Item> findConventionalModdedHead(EntityType<?> entityType) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        for (String suffix : CONVENTIONAL_HEAD_SUFFIXES) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(
                    entityId.getNamespace(),
                    entityId.getPath() + suffix
            )).orElse(null);
            if (item != null && item != Items.AIR) {
                return Optional.of(item);
            }
        }
        for (String prefix : CONVENTIONAL_HEAD_PREFIXES) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(
                    entityId.getNamespace(),
                    prefix + entityId.getPath()
            )).orElse(null);
            if (item != null && item != Items.AIR) {
                return Optional.of(item);
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (isConventionalHeadPath(itemId.getPath(), entityId.getPath())
                    && new ItemStack(item).is(ItemTags.SKULLS)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static boolean isConventionalHeadPath(String itemPath, String entityPath) {
        return CONVENTIONAL_HEAD_SUFFIXES.stream().anyMatch(suffix -> itemPath.equals(entityPath + suffix))
                || CONVENTIONAL_HEAD_PREFIXES.stream().anyMatch(prefix -> itemPath.equals(prefix + entityPath));
    }

    private static boolean alreadyDropped(LivingDropsEvent event, Item head) {
        return event.getDrops().stream().anyMatch(drop -> drop.getItem().is(head));
    }
}
