package com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatItems;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmKind;
import com.cosmocraft.trading_cells.feature.creeperfarm.domain.model.CreeperFarmLoot;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Feature view of creepers plus the synthetic charged-creeper variant. */
public final class CreeperFarmTargetCatalog {
    public static final Identifier CHARGED_CREEPER_ID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "charged_creeper"
    );
    private static final Identifier CREEPER_ID = Identifier.withDefaultNamespace("creeper");
    private static final TagKey<Item> CREEPER_DROP_MUSIC_DISCS = TagKey.create(
            Registries.ITEM,
            Identifier.withDefaultNamespace("creeper_drop_music_discs")
    );

    private CreeperFarmTargetCatalog() {
    }

    public static List<MobFarmCatalog.Target> targets() {
        List<MobFarmCatalog.Target> catalog = MobFarmCatalog.targets(MobFarmCatalog.Family.CREEPER).stream()
                .map(CreeperFarmTargetCatalog::withoutMusicDiscs)
                .toList();
        List<MobFarmCatalog.Target> ordered = new ArrayList<>();
        catalog.stream().filter(target -> target.entityTypeId().equals(CREEPER_ID)).findFirst()
                .ifPresent(ordered::add);
        ordered.add(chargedTarget(catalog));
        catalog.stream().filter(target -> !target.entityTypeId().equals(CREEPER_ID)).forEach(ordered::add);
        return List.copyOf(ordered);
    }

    public static Identifier id(CreeperFarmKind kind) {
        return kind == CreeperFarmKind.CHARGED_CREEPER ? CHARGED_CREEPER_ID : CREEPER_ID;
    }

    public static Identifier entityTypeId(Identifier targetId) {
        return CHARGED_CREEPER_ID.equals(targetId) ? CREEPER_ID : targetId;
    }

    public static CreeperFarmKind staticKind(Identifier id) {
        return CHARGED_CREEPER_ID.equals(id) ? CreeperFarmKind.CHARGED_CREEPER : CreeperFarmKind.CREEPER;
    }

    public static boolean isStaticTarget(Identifier id) {
        return CREEPER_ID.equals(id) || CHARGED_CREEPER_ID.equals(id);
    }

    public static boolean isKnownTarget(Identifier id) {
        return targets().stream().anyMatch(target -> target.entityTypeId().equals(id));
    }

    public static List<CreeperFarmLoot> availableCategories(Identifier targetId, boolean hasDecapitation) {
        if (isStaticTarget(targetId)) {
            return staticKind(targetId).availableLoot(hasDecapitation);
        }
        LinkedHashSet<CreeperFarmLoot> categories = new LinkedHashSet<>();
        target(targetId).ifPresent(target -> target.lootItemIds().stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(CreeperFarmTargetCatalog::category)
                .filter(java.util.Objects::nonNull)
                .filter(loot -> loot != CreeperFarmLoot.HEADS || hasDecapitation)
                .forEach(categories::add));
        return List.copyOf(categories);
    }

    public static List<ItemStack> dynamicLoot(Identifier targetId) {
        return target(targetId).stream()
                .flatMap(target -> target.lootItemIds().stream())
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).orElse(null))
                .filter(item -> item != null && category(item) == null)
                .map(ItemStack::new)
                .toList();
    }

    public static CreeperFarmLoot category(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item == Items.GUNPOWDER) {
            return CreeperFarmLoot.GUNPOWDER;
        }
        if (item == CombatItems.stormShard()) {
            return CreeperFarmLoot.STORM_SHARDS;
        }
        if (stack.is(ItemTags.SKULLS)) {
            return CreeperFarmLoot.HEADS;
        }
        return null;
    }

    public static boolean isMusicDisc(Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        return new ItemStack(item).is(CREEPER_DROP_MUSIC_DISCS)
                || (itemId != null && itemId.getPath().startsWith("music_disc_"));
    }

    private static java.util.Optional<MobFarmCatalog.Target> target(Identifier id) {
        return targets().stream().filter(target -> target.entityTypeId().equals(id)).findFirst();
    }

    private static MobFarmCatalog.Target chargedTarget(List<MobFarmCatalog.Target> catalog) {
        LinkedHashSet<Identifier> loot = new LinkedHashSet<>();
        catalog.stream()
                .filter(target -> target.entityTypeId().equals(CREEPER_ID))
                .findFirst()
                .ifPresent(target -> loot.addAll(target.lootItemIds()));
        Identifier shardId = BuiltInRegistries.ITEM.getKey(CombatItems.stormShard());
        loot.add(shardId);
        return new MobFarmCatalog.Target(CHARGED_CREEPER_ID, shardId, List.copyOf(loot));
    }

    private static MobFarmCatalog.Target withoutMusicDiscs(MobFarmCatalog.Target target) {
        return new MobFarmCatalog.Target(
                target.entityTypeId(),
                target.generatorItemId(),
                target.lootItemIds().stream()
                        .filter(id -> BuiltInRegistries.ITEM.getOptional(id)
                                .map(item -> !isMusicDisc(item))
                                .orElse(true))
                        .toList()
        );
    }
}
