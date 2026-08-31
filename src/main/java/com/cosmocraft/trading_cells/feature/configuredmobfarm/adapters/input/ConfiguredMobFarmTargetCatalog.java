package com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Feature view of the data-driven targets belonging to one configurable farm family. */
public final class ConfiguredMobFarmTargetCatalog {
    private static final Identifier PIGLIN = Identifier.withDefaultNamespace("piglin");
    private static final Identifier PIGLIN_BRUTE = Identifier.withDefaultNamespace("piglin_brute");

    private ConfiguredMobFarmTargetCatalog() {
    }

    public static MobFarmCatalog.Family family(ConfiguredMobFarmKind kind) {
        return switch (kind) {
            case ARTHROPOD -> MobFarmCatalog.Family.ARTHROPOD;
            case SLIME -> MobFarmCatalog.Family.SLIME;
            case GUARDIAN -> MobFarmCatalog.Family.GUARDIAN;
            case PIGLIN -> MobFarmCatalog.Family.PIGLIN;
            case BLAZE -> MobFarmCatalog.Family.BLAZE;
            case GHAST -> MobFarmCatalog.Family.GHAST;
            case ENDERMAN -> MobFarmCatalog.Family.ENDERMAN;
            case SHULKER -> MobFarmCatalog.Family.SHULKER;
            case BREEZE -> MobFarmCatalog.Family.BREEZE;
            case PHANTOM -> MobFarmCatalog.Family.PHANTOM;
        };
    }

    public static List<MobFarmCatalog.Target> targets(ConfiguredMobFarmKind kind) {
        return MobFarmCatalog.targets(family(kind));
    }

    /** Compatibility view used only while a menu has not received its block family yet. */
    public static List<MobFarmCatalog.Target> targets() {
        return Arrays.stream(ConfiguredMobFarmKind.values())
                .flatMap(kind -> targets(kind).stream())
                .distinct()
                .toList();
    }

    public static Identifier id(ConfiguredMobFarmKind kind) {
        return targets(kind).stream()
                .map(MobFarmCatalog.Target::entityTypeId)
                .findFirst()
                .orElseGet(() -> Identifier.withDefaultNamespace(kind.path()));
    }

    public static Identifier entityTypeId(Identifier targetId) {
        return targetId;
    }

    public static Optional<ConfiguredMobFarmKind> kindFor(Identifier targetId) {
        return Arrays.stream(ConfiguredMobFarmKind.values())
                .filter(kind -> isKnownTarget(kind, targetId))
                .findFirst();
    }

    public static ConfiguredMobFarmKind staticKind(Identifier targetId) {
        return kindFor(targetId).orElse(ConfiguredMobFarmKind.ARTHROPOD);
    }

    public static boolean isStaticTarget(Identifier targetId) {
        return kindFor(targetId).map(kind -> id(kind).equals(targetId)).orElse(false);
    }

    public static boolean isKnownTarget(ConfiguredMobFarmKind kind, Identifier targetId) {
        return MobFarmCatalog.contains(family(kind), targetId);
    }

    public static boolean isKnownTarget(Identifier targetId) {
        return kindFor(targetId).isPresent();
    }

    public static List<ConfiguredMobFarmLoot> availableCategories(
            ConfiguredMobFarmKind ignoredKind,
            Identifier ignoredTargetId,
            boolean ignoredDecapitation
    ) {
        return List.of();
    }

    public static List<ConfiguredMobFarmLoot> availableCategories(
            Identifier ignoredTargetId,
            boolean ignoredDecapitation
    ) {
        return List.of();
    }

    public static ItemStack defaultWeapon(Identifier targetId) {
        if (PIGLIN_BRUTE.equals(targetId)) {
            return new ItemStack(Items.GOLDEN_AXE);
        }
        if (PIGLIN.equals(targetId)) {
            return new ItemStack(Items.GOLDEN_SWORD);
        }
        return ItemStack.EMPTY;
    }

    public static List<ItemStack> equipment(Identifier targetId) {
        if (PIGLIN_BRUTE.equals(targetId)) {
            return List.of(new ItemStack(Items.GOLDEN_AXE));
        }
        if (PIGLIN.equals(targetId)) {
            return List.of(
                    new ItemStack(Items.CROSSBOW),
                    new ItemStack(Items.GOLDEN_SPEAR),
                    new ItemStack(Items.GOLDEN_SWORD),
                    new ItemStack(Items.GOLDEN_HELMET),
                    new ItemStack(Items.GOLDEN_CHESTPLATE),
                    new ItemStack(Items.GOLDEN_LEGGINGS),
                    new ItemStack(Items.GOLDEN_BOOTS)
            );
        }
        return List.of();
    }

    public static double equipmentSpawnChance(Identifier targetId, Item item) {
        if (PIGLIN_BRUTE.equals(targetId)) {
            return item == Items.GOLDEN_AXE ? 1.0D : 0.0D;
        }
        if (!PIGLIN.equals(targetId)) {
            return 0.0D;
        }
        if (item == Items.CROSSBOW) {
            return 0.50D;
        }
        if (item == Items.GOLDEN_SPEAR) {
            return 0.05D;
        }
        if (item == Items.GOLDEN_SWORD) {
            return 0.45D;
        }
        if (item == Items.GOLDEN_HELMET
                || item == Items.GOLDEN_CHESTPLATE
                || item == Items.GOLDEN_LEGGINGS
                || item == Items.GOLDEN_BOOTS) {
            return 0.10D;
        }
        return 0.0D;
    }

    public static boolean isPillager(Identifier ignoredTargetId) {
        return false;
    }

    public static boolean isWitch(Identifier ignoredTargetId) {
        return false;
    }

    public static List<ItemStack> dynamicLoot(ConfiguredMobFarmKind kind, Identifier targetId) {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        target(kind, targetId).stream()
                .flatMap(target -> target.lootItemIds().stream())
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .forEach(items::add);
        equipment(targetId).stream().map(ItemStack::getItem).forEach(items::add);
        return items.stream().map(ItemStack::new).toList();
    }

    public static List<ItemStack> dynamicLoot(Identifier targetId) {
        return kindFor(targetId).map(kind -> dynamicLoot(kind, targetId)).orElseGet(List::of);
    }

    public static ConfiguredMobFarmLoot category(Item ignoredItem) {
        return null;
    }

    public static Optional<MobFarmCatalog.Target> target(ConfiguredMobFarmKind kind, Identifier targetId) {
        return MobFarmCatalog.target(family(kind), targetId);
    }
}
