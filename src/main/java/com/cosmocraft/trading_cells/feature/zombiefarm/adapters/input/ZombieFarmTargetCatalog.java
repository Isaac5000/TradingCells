package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input;

import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import java.util.List;
import java.util.LinkedHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Feature-specific view of the shared dynamic zombie catalog. */
public final class ZombieFarmTargetCatalog {
    private ZombieFarmTargetCatalog() {
    }

    public static List<MobFarmCatalog.Target> targets() {
        return MobFarmCatalog.targets(MobFarmCatalog.Family.ZOMBIE);
    }

    public static Identifier id(ZombieFarmKind kind) {
        return Identifier.withDefaultNamespace(switch (kind) {
            case ZOMBIE -> "zombie";
            case ZOMBIE_VILLAGER -> "zombie_villager";
            case HUSK -> "husk";
            case DROWNED -> "drowned";
            case ZOMBIFIED_PIGLIN -> "zombified_piglin";
            case ZOGLIN -> "zoglin";
        });
    }

    public static ZombieFarmKind staticKind(Identifier id) {
        if (!"minecraft".equals(id.getNamespace())) {
            return ZombieFarmKind.ZOMBIE;
        }
        return switch (id.getPath()) {
            case "zombie_villager" -> ZombieFarmKind.ZOMBIE_VILLAGER;
            case "husk" -> ZombieFarmKind.HUSK;
            case "drowned" -> ZombieFarmKind.DROWNED;
            case "zombified_piglin" -> ZombieFarmKind.ZOMBIFIED_PIGLIN;
            case "zoglin" -> ZombieFarmKind.ZOGLIN;
            default -> ZombieFarmKind.ZOMBIE;
        };
    }

    public static boolean isStaticTarget(Identifier id) {
        return id(staticKind(id)).equals(id);
    }

    public static List<ZombieFarmLoot> availableCategories(Identifier targetId, boolean hasDecapitation) {
        if (isStaticTarget(targetId)) {
            return staticKind(targetId).availableLoot(hasDecapitation);
        }
        LinkedHashSet<ZombieFarmLoot> categories = new LinkedHashSet<>();
        MobFarmCatalog.target(MobFarmCatalog.Family.ZOMBIE, targetId).ifPresent(target ->
                target.lootItemIds().stream()
                        .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .map(ZombieFarmTargetCatalog::category)
                        .filter(java.util.Objects::nonNull)
                        .forEach(categories::add)
        );
        return List.copyOf(categories);
    }

    public static List<ItemStack> dynamicLoot(Identifier targetId) {
        return MobFarmCatalog.target(MobFarmCatalog.Family.ZOMBIE, targetId).stream()
                .flatMap(target -> target.lootItemIds().stream())
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(item -> item != null && category(item) == null)
                .map(ItemStack::new)
                .toList();
    }

    public static ZombieFarmLoot category(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item == Items.ROTTEN_FLESH) {
            return ZombieFarmLoot.ROTTEN_FLESH;
        }
        if (item == Items.IRON_INGOT) {
            return ZombieFarmLoot.IRON_INGOTS;
        }
        if (item == Items.CARROT) {
            return ZombieFarmLoot.CARROTS;
        }
        if (item == Items.POTATO || item == Items.BAKED_POTATO) {
            return ZombieFarmLoot.POTATOES;
        }
        if (item == Items.COPPER_INGOT) {
            return ZombieFarmLoot.COPPER_INGOTS;
        }
        if (item == Items.NAUTILUS_SHELL) {
            return ZombieFarmLoot.NAUTILUS_SHELLS;
        }
        if (item == Items.GOLD_NUGGET) {
            return ZombieFarmLoot.GOLD_NUGGETS;
        }
        if (item == Items.GOLD_INGOT) {
            return ZombieFarmLoot.GOLD_INGOTS;
        }
        if (stack.is(ItemTags.SKULLS)) {
            return ZombieFarmLoot.HEADS;
        }
        if (stack.is(ItemTags.DROWNED_PREFERRED_WEAPONS)
                || stack.is(ItemTags.WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.SPEARS)) {
            return ZombieFarmLoot.WEAPONS;
        }
        return null;
    }
}
