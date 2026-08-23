package com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmKind;
import com.cosmocraft.trading_cells.feature.skeletonfarm.domain.model.SkeletonFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Feature-specific view of the shared dynamic skeleton catalog. */
public final class SkeletonFarmTargetCatalog {
    private SkeletonFarmTargetCatalog() {
    }

    public static List<MobFarmCatalog.Target> targets() {
        return MobFarmCatalog.targets(MobFarmCatalog.Family.SKELETON);
    }

    public static Identifier id(SkeletonFarmKind kind) {
        return Identifier.withDefaultNamespace(switch (kind) {
            case SKELETON -> "skeleton";
            case WITHER_SKELETON -> "wither_skeleton";
            case STRAY -> "stray";
            case BOGGED -> "bogged";
            case PARCHED -> "parched";
            case SKELETON_HORSE -> "skeleton_horse";
        });
    }

    public static SkeletonFarmKind staticKind(Identifier id) {
        if (!"minecraft".equals(id.getNamespace())) {
            return SkeletonFarmKind.SKELETON;
        }
        return switch (id.getPath()) {
            case "wither_skeleton" -> SkeletonFarmKind.WITHER_SKELETON;
            case "stray" -> SkeletonFarmKind.STRAY;
            case "bogged" -> SkeletonFarmKind.BOGGED;
            case "parched" -> SkeletonFarmKind.PARCHED;
            case "skeleton_horse" -> SkeletonFarmKind.SKELETON_HORSE;
            default -> SkeletonFarmKind.SKELETON;
        };
    }

    public static boolean isStaticTarget(Identifier id) {
        return id(staticKind(id)).equals(id);
    }

    public static List<SkeletonFarmLoot> availableCategories(Identifier targetId, boolean hasDecapitation) {
        if (isStaticTarget(targetId)) {
            return staticKind(targetId).availableLoot(hasDecapitation);
        }
        LinkedHashSet<SkeletonFarmLoot> categories = new LinkedHashSet<>();
        MobFarmCatalog.target(MobFarmCatalog.Family.SKELETON, targetId).ifPresent(target ->
                target.lootItemIds().stream()
                        .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .map(SkeletonFarmTargetCatalog::category)
                        .filter(java.util.Objects::nonNull)
                        .forEach(categories::add)
        );
        return List.copyOf(categories);
    }

    public static List<ItemStack> dynamicLoot(Identifier targetId) {
        return MobFarmCatalog.target(MobFarmCatalog.Family.SKELETON, targetId).stream()
                .flatMap(target -> target.lootItemIds().stream())
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(item -> item != null && category(item) == null)
                .map(ItemStack::new)
                .toList();
    }

    public static Set<Identifier> tableExtensions(Identifier targetId) {
        LinkedHashSet<Identifier> extensions = new LinkedHashSet<>();
        MobFarmCatalog.target(MobFarmCatalog.Family.SKELETON, targetId).ifPresent(target ->
                target.lootItemIds().forEach(id -> {
                    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                    if (item != null && (!"minecraft".equals(id.getNamespace()) || category(item) == null)) {
                        extensions.add(id);
                    }
                })
        );
        return Set.copyOf(extensions);
    }

    public static SkeletonFarmLoot category(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item == Items.BONE) {
            return SkeletonFarmLoot.BONES;
        }
        if (stack.is(ItemTags.ARROWS)) {
            return SkeletonFarmLoot.ARROWS;
        }
        if (stack.is(ItemTags.SKULLS)) {
            return SkeletonFarmLoot.SKULLS;
        }
        if (item == Items.COAL) {
            return SkeletonFarmLoot.COAL;
        }
        if (stack.is(ItemTags.SKELETON_PREFERRED_WEAPONS)
                || stack.is(ItemTags.WEAPON_ENCHANTABLE)) {
            return SkeletonFarmLoot.WEAPONS;
        }
        return null;
    }
}
