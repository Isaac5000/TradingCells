package com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmKind;
import com.cosmocraft.trading_cells.feature.raiderfarm.domain.model.RaiderFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Feature view of the vanilla and data-driven raider targets. */
public final class RaiderFarmTargetCatalog {
    private static final Identifier PILLAGER_ID = Identifier.withDefaultNamespace("pillager");
    private static final Identifier VINDICATOR_ID = Identifier.withDefaultNamespace("vindicator");
    private static final Identifier WHITE_BANNER_ID = Identifier.withDefaultNamespace("white_banner");

    private RaiderFarmTargetCatalog() {
    }

    public static List<MobFarmCatalog.Target> targets() {
        List<MobFarmCatalog.Target> catalog = MobFarmCatalog.targets(MobFarmCatalog.Family.RAIDER);
        Map<Identifier, MobFarmCatalog.Target> byId = new LinkedHashMap<>();
        catalog.forEach(target -> byId.put(target.entityTypeId(), target));
        List<MobFarmCatalog.Target> ordered = new ArrayList<>();
        add(ordered, byId.remove(PILLAGER_ID));
        add(ordered, byId.remove(Identifier.withDefaultNamespace("evoker")));
        add(ordered, byId.remove(Identifier.withDefaultNamespace("ravager")));
        add(ordered, byId.remove(Identifier.withDefaultNamespace("witch")));
        ordered.addAll(byId.values());
        return List.copyOf(ordered);
    }

    public static Identifier id(RaiderFarmKind kind) {
        return switch (kind) {
            case PILLAGER -> PILLAGER_ID;
            case EVOKER -> Identifier.withDefaultNamespace("evoker");
            case RAVAGER -> Identifier.withDefaultNamespace("ravager");
            case WITCH -> Identifier.withDefaultNamespace("witch");
        };
    }

    public static Identifier entityTypeId(Identifier targetId) {
        return targetId;
    }

    public static RaiderFarmKind staticKind(Identifier id) {
        if (!"minecraft".equals(id.getNamespace())) {
            return RaiderFarmKind.PILLAGER;
        }
        return switch (id.getPath()) {
            case "evoker" -> RaiderFarmKind.EVOKER;
            case "ravager" -> RaiderFarmKind.RAVAGER;
            case "witch" -> RaiderFarmKind.WITCH;
            default -> RaiderFarmKind.PILLAGER;
        };
    }

    public static boolean isStaticTarget(Identifier id) {
        return id(staticKind(id)).equals(id);
    }

    public static boolean isKnownTarget(Identifier id) {
        return targets().stream().anyMatch(target -> target.entityTypeId().equals(id));
    }

    public static List<RaiderFarmLoot> availableCategories(Identifier targetId, boolean ignoredDecapitation) {
        if (isStaticTarget(targetId)) {
            return staticKind(targetId).availableLoot();
        }
        LinkedHashSet<RaiderFarmLoot> categories = new LinkedHashSet<>();
        target(targetId).ifPresent(target -> target.lootItemIds().stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(RaiderFarmTargetCatalog::category)
                .filter(java.util.Objects::nonNull)
                .forEach(categories::add));
        if (!defaultWeapon(targetId).isEmpty()) {
            categories.add(RaiderFarmLoot.WEAPONS);
        }
        return List.copyOf(categories);
    }

    public static ItemStack defaultWeapon(Identifier targetId) {
        if (PILLAGER_ID.equals(targetId)) {
            return new ItemStack(Items.CROSSBOW);
        }
        if (VINDICATOR_ID.equals(targetId)) {
            return new ItemStack(Items.IRON_AXE);
        }
        return ItemStack.EMPTY;
    }

    public static boolean isPillager(Identifier targetId) {
        return PILLAGER_ID.equals(targetId);
    }

    public static boolean isWitch(Identifier targetId) {
        return Identifier.withDefaultNamespace("witch").equals(targetId);
    }

    public static List<ItemStack> dynamicLoot(Identifier targetId) {
        return target(targetId).stream()
                .flatMap(target -> target.lootItemIds().stream())
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(item -> item != null && category(item) == null)
                .map(ItemStack::new)
                .toList();
    }

    public static RaiderFarmLoot category(Item item) {
        ItemStack stack = new ItemStack(item);
        if (WHITE_BANNER_ID.equals(BuiltInRegistries.ITEM.getKey(item))) {
            return RaiderFarmLoot.OMINOUS_BANNER;
        }
        return stack.is(ItemTags.WEAPON_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
                ? RaiderFarmLoot.WEAPONS
                : null;
    }

    private static java.util.Optional<MobFarmCatalog.Target> target(Identifier id) {
        return targets().stream().filter(target -> target.entityTypeId().equals(id)).findFirst();
    }

    private static void add(List<MobFarmCatalog.Target> targets, MobFarmCatalog.Target target) {
        if (target != null) {
            targets.add(target);
        }
    }
}
