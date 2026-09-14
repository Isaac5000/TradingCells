package com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;

import com.cosmocraft.trading_cells.feature.zombiefarm.application.port.input.ZombieFarmUseCase;
import com.cosmocraft.trading_cells.feature.combat.domain.model.DecapitationRules;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmDropRules;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmLoot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ZombieFarmLootAdapter {
    private ZombieFarmLootAdapter() {
    }

    static List<ItemStack> generate(
            Identifier targetId,
            ZombieFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            int kills,
            int lootingLevel,
            int decapitationLevel,
            ServerLevel level,
            ItemStack sword,
            RandomSource random,
            ZombieFarmUseCase rules
    ) {
        int looting = Math.max(0, lootingLevel);
        List<ItemStack> drops = new ArrayList<>();
        boolean staticTarget = ZombieFarmTargetCatalog.isStaticTarget(targetId);
        List<List<ItemStack>> tableDrops = generateLootTableDrops(targetId, kills, level, sword);
        for (int kill = 0; kill < Math.max(1, kills); kill++) {
            if (tableDrops.isEmpty()) {
                if (staticTarget) {
                    addStaticVanillaDrops(drops, kind, enabledMask, looting, random, rules);
                }
            } else {
                addFilteredTableDrops(
                        drops,
                        tableDrops.get(kill),
                        kind,
                        enabledMask,
                        disabledDynamicLoot,
                        rules
                );
            }
            if (!staticTarget) {
                continue;
            }
            ItemStack weapon = rollWeapon(kind, looting, level.getDifficulty() == Difficulty.HARD, random);
            boolean nautilus = random.nextDouble() < ZombieFarmDropRules.drownedNautilusChance();
            boolean head = random.nextDouble() < DecapitationRules.decapitationHeadChance(decapitationLevel);

            if (!weapon.isEmpty() && rules.isEnabled(enabledMask, kind, ZombieFarmLoot.WEAPONS)) {
                drops.add(weapon);
            }
            if (kind == ZombieFarmKind.DROWNED
                    && nautilus
                    && rules.isEnabled(enabledMask, kind, ZombieFarmLoot.NAUTILUS_SHELLS)) {
                addStack(drops, new ItemStack(Items.NAUTILUS_SHELL));
            }
            if (head && rules.isEnabled(enabledMask, kind, ZombieFarmLoot.HEADS)) {
                addStack(drops, new ItemStack(headItem(kind)));
            }
        }
        return List.copyOf(drops);
    }

    private static List<List<ItemStack>> generateLootTableDrops(
            Identifier targetId,
            int kills,
            ServerLevel level,
            ItemStack sword
    ) {
        LivingEntity target = MobFarmLootTables.createTarget(level, targetId);
        return MobFarmLootTables.batches(level, target, sword, Math.max(1, kills));
    }

    private static void addFilteredTableDrops(
            List<ItemStack> drops,
            List<ItemStack> generated,
            ZombieFarmKind kind,
            int enabledMask,
            Set<Identifier> disabledDynamicLoot,
            ZombieFarmUseCase rules
    ) {
        for (ItemStack stack : generated) {
            ZombieFarmLoot category = ZombieFarmTargetCatalog.category(stack.getItem());
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            boolean enabled = category == null
                    ? itemId != null && !disabledDynamicLoot.contains(itemId)
                    : rules.isEnabled(enabledMask, kind, category);
            if (enabled) {
                addStack(drops, stack.copy());
            }
        }
    }

    private static void addStaticVanillaDrops(
            List<ItemStack> drops,
            ZombieFarmKind kind,
            int enabledMask,
            int looting,
            RandomSource random,
            ZombieFarmUseCase rules
    ) {
        switch (kind) {
            case ZOMBIFIED_PIGLIN -> addStaticZombifiedPiglinDrops(
                    drops, kind, enabledMask, looting, random, rules
            );
            case ZOGLIN -> addStaticZoglinDrops(drops, kind, enabledMask, looting, random, rules);
            case DROWNED -> {
                addStandardFlesh(drops, kind, enabledMask, looting, random, rules);
                if (random.nextDouble() < ZombieFarmDropRules.copperChance(looting)
                        && rules.isEnabled(enabledMask, kind, ZombieFarmLoot.COPPER_INGOTS)) {
                    addStack(drops, new ItemStack(Items.COPPER_INGOT));
                }
            }
            case ZOMBIE, ZOMBIE_VILLAGER, HUSK -> {
                addStandardFlesh(drops, kind, enabledMask, looting, random, rules);
                addStaticRareDrop(drops, kind, enabledMask, looting, random, rules);
            }
        }
    }

    private static void addStandardFlesh(
            List<ItemStack> drops,
            ZombieFarmKind kind,
            int enabledMask,
            int looting,
            RandomSource random,
            ZombieFarmUseCase rules
    ) {
        int amount = random.nextInt(3) + lootingBonus(looting, random);
        if (rules.isEnabled(enabledMask, kind, ZombieFarmLoot.ROTTEN_FLESH)) {
            addStack(drops, new ItemStack(Items.ROTTEN_FLESH, amount));
        }
    }

    private static void addStaticRareDrop(
            List<ItemStack> drops,
            ZombieFarmKind kind,
            int enabledMask,
            int looting,
            RandomSource random,
            ZombieFarmUseCase rules
    ) {
        if (random.nextDouble() >= ZombieFarmDropRules.rarePoolChance(looting)) {
            return;
        }
        ZombieFarmLoot loot = switch (random.nextInt(3)) {
            case 0 -> ZombieFarmLoot.IRON_INGOTS;
            case 1 -> ZombieFarmLoot.CARROTS;
            default -> ZombieFarmLoot.POTATOES;
        };
        Item item = switch (loot) {
            case IRON_INGOTS -> Items.IRON_INGOT;
            case CARROTS -> Items.CARROT;
            case POTATOES -> Items.POTATO;
            default -> throw new IllegalStateException("Unexpected zombie rare drop: " + loot);
        };
        if (rules.isEnabled(enabledMask, kind, loot)) {
            addStack(drops, new ItemStack(item));
        }
    }

    private static void addStaticZombifiedPiglinDrops(
            List<ItemStack> drops,
            ZombieFarmKind kind,
            int enabledMask,
            int looting,
            RandomSource random,
            ZombieFarmUseCase rules
    ) {
        int flesh = random.nextInt(2) + lootingBonus(looting, random);
        int nuggets = random.nextInt(2) + lootingBonus(looting, random);
        if (rules.isEnabled(enabledMask, kind, ZombieFarmLoot.ROTTEN_FLESH)) {
            addStack(drops, new ItemStack(Items.ROTTEN_FLESH, flesh));
        }
        if (rules.isEnabled(enabledMask, kind, ZombieFarmLoot.GOLD_NUGGETS)) {
            addStack(drops, new ItemStack(Items.GOLD_NUGGET, nuggets));
        }
        if (random.nextDouble() < ZombieFarmDropRules.rarePoolChance(looting)
                && rules.isEnabled(enabledMask, kind, ZombieFarmLoot.GOLD_INGOTS)) {
            addStack(drops, new ItemStack(Items.GOLD_INGOT));
        }
    }

    private static void addStaticZoglinDrops(
            List<ItemStack> drops,
            ZombieFarmKind kind,
            int enabledMask,
            int looting,
            RandomSource random,
            ZombieFarmUseCase rules
    ) {
        int flesh = 1 + random.nextInt(3) + lootingBonus(looting, random);
        if (rules.isEnabled(enabledMask, kind, ZombieFarmLoot.ROTTEN_FLESH)) {
            addStack(drops, new ItemStack(Items.ROTTEN_FLESH, flesh));
        }
    }

    private static int lootingBonus(int looting, RandomSource random) {
        return looting == 0 ? 0 : random.nextInt(looting + 1);
    }

    public static List<ItemStack> previewOutputs(ZombieFarmKind kind) {
        return previewOutputChances(kind).stream()
                .map(PreviewOutput::stack)
                .toList();
    }

    public static List<PreviewOutput> previewOutputChances(ZombieFarmKind kind) {
        List<PreviewOutput> outputs = new ArrayList<>();
        ZombieFarmDropRules.BaseDrop flesh = switch (kind) {
            case ZOMBIFIED_PIGLIN -> ZombieFarmDropRules.zombifiedPiglinStackCycleDrop(0, 1);
            case ZOGLIN -> ZombieFarmDropRules.zoglinFleshCycleDrop(0, 1);
            default -> ZombieFarmDropRules.fleshCycleDrop(0, 1);
        };
        addPreview(outputs, ZombieFarmLoot.ROTTEN_FLESH, Items.ROTTEN_FLESH, flesh);
        if (kind == ZombieFarmKind.ZOMBIE
                || kind == ZombieFarmKind.ZOMBIE_VILLAGER
                || kind == ZombieFarmKind.HUSK) {
            ZombieFarmDropRules.BaseDrop rare = ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.rareItemChance(0),
                    1
            );
            addPreview(outputs, ZombieFarmLoot.IRON_INGOTS, Items.IRON_INGOT, rare);
            addPreview(outputs, ZombieFarmLoot.CARROTS, Items.CARROT, rare);
            addPreview(outputs, ZombieFarmLoot.POTATOES, Items.POTATO, rare);
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.IRON_SWORD,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.zombieSwordChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.IRON_SPEAR,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.zombieSpearChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.IRON_SHOVEL,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.zombieShovelChance(0), 1));
        } else if (kind == ZombieFarmKind.DROWNED) {
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.TRIDENT,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.drownedTridentChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.FISHING_ROD,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.drownedFishingRodChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.COPPER_INGOTS, Items.COPPER_INGOT,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.copperChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.NAUTILUS_SHELLS, Items.NAUTILUS_SHELL,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.drownedNautilusChance(), 1));
        } else if (kind == ZombieFarmKind.ZOMBIFIED_PIGLIN) {
            addPreview(outputs, ZombieFarmLoot.GOLD_NUGGETS, Items.GOLD_NUGGET,
                    ZombieFarmDropRules.zombifiedPiglinStackCycleDrop(0, 1));
            addPreview(outputs, ZombieFarmLoot.GOLD_INGOTS, Items.GOLD_INGOT,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.rarePoolChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.GOLDEN_SWORD,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.zombifiedPiglinSwordChance(0), 1));
            addPreview(outputs, ZombieFarmLoot.WEAPONS, Items.GOLDEN_SPEAR,
                    ZombieFarmDropRules.binaryCycleDrop(ZombieFarmDropRules.zombifiedPiglinSpearChance(0), 1));
        }
        if (kind.supports(ZombieFarmLoot.HEADS)) {
            addPreview(outputs, ZombieFarmLoot.HEADS, headItem(kind),
                    ZombieFarmDropRules.headCycleDrop(1, 1));
        }
        return List.copyOf(outputs);
    }

    public static ZombieFarmDropRules.BaseDrop currentCycleDrop(
            ZombieFarmKind kind,
            ZombieFarmLoot loot,
            ItemStack previewStack,
            int lootingLevel,
            int simulatedKills,
            int decapitationLevel
    ) {
        if (loot == ZombieFarmLoot.ROTTEN_FLESH) {
            return switch (kind) {
                case ZOMBIFIED_PIGLIN -> ZombieFarmDropRules.zombifiedPiglinStackCycleDrop(
                        lootingLevel, simulatedKills
                );
                case ZOGLIN -> ZombieFarmDropRules.zoglinFleshCycleDrop(lootingLevel, simulatedKills);
                default -> ZombieFarmDropRules.fleshCycleDrop(lootingLevel, simulatedKills);
            };
        }
        if (loot == ZombieFarmLoot.IRON_INGOTS
                || loot == ZombieFarmLoot.CARROTS
                || loot == ZombieFarmLoot.POTATOES) {
            return ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.rareItemChance(lootingLevel),
                    simulatedKills
            );
        }
        if (loot == ZombieFarmLoot.COPPER_INGOTS) {
            return ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.copperChance(lootingLevel),
                    simulatedKills
            );
        }
        if (loot == ZombieFarmLoot.NAUTILUS_SHELLS) {
            return ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.drownedNautilusChance(),
                    simulatedKills
            );
        }
        if (loot == ZombieFarmLoot.GOLD_NUGGETS) {
            return ZombieFarmDropRules.zombifiedPiglinStackCycleDrop(lootingLevel, simulatedKills);
        }
        if (loot == ZombieFarmLoot.GOLD_INGOTS) {
            return ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.rarePoolChance(lootingLevel),
                    simulatedKills
            );
        }
        if (loot == ZombieFarmLoot.HEADS) {
            return ZombieFarmDropRules.headCycleDrop(simulatedKills, decapitationLevel);
        }
        double weaponChance = weaponChance(kind, previewStack.getItem(), lootingLevel);
        return ZombieFarmDropRules.binaryCycleDrop(weaponChance, simulatedKills);
    }

    private static void addPreview(
            List<PreviewOutput> outputs,
            ZombieFarmLoot loot,
            Item item,
            ZombieFarmDropRules.BaseDrop drop
    ) {
        outputs.add(new PreviewOutput(
                loot,
                new ItemStack(item),
                drop.probabilityPartsPerMillion(),
                drop.minimumAmount(),
                drop.maximumAmount()
        ));
    }

    private static ItemStack rollWeapon(
            ZombieFarmKind kind,
            int looting,
            boolean hardDifficulty,
            RandomSource random
    ) {
        if (kind == ZombieFarmKind.DROWNED) {
            if (random.nextDouble() >= 0.10D) {
                return ItemStack.EMPTY;
            }
            Item item = random.nextInt(16) < 10 ? Items.TRIDENT : Items.FISHING_ROD;
            return random.nextDouble() < ZombieFarmDropRules.equipmentDropChance(looting)
                    ? worn(item, random)
                    : ItemStack.EMPTY;
        }
        if (kind == ZombieFarmKind.ZOMBIFIED_PIGLIN) {
            if (random.nextDouble() >= ZombieFarmDropRules.equipmentDropChance(looting)) {
                return ItemStack.EMPTY;
            }
            Item item = random.nextInt(20) == 0 ? Items.GOLDEN_SPEAR : Items.GOLDEN_SWORD;
            return worn(item, random);
        }
        if (kind == ZombieFarmKind.ZOGLIN) {
            return ItemStack.EMPTY;
        }
        double spawnChance = hardDifficulty ? 0.05D : 0.01D;
        if (random.nextDouble() >= spawnChance) {
            return ItemStack.EMPTY;
        }
        int roll = random.nextInt(6);
        Item item = roll == 0 ? Items.IRON_SWORD : roll == 1 ? Items.IRON_SPEAR : Items.IRON_SHOVEL;
        return random.nextDouble() < ZombieFarmDropRules.equipmentDropChance(looting)
                ? worn(item, random)
                : ItemStack.EMPTY;
    }

    private static double weaponChance(ZombieFarmKind kind, Item item, int lootingLevel) {
        if (kind == ZombieFarmKind.DROWNED) {
            return item == Items.TRIDENT
                    ? ZombieFarmDropRules.drownedTridentChance(lootingLevel)
                    : ZombieFarmDropRules.drownedFishingRodChance(lootingLevel);
        }
        if (kind == ZombieFarmKind.ZOMBIFIED_PIGLIN) {
            return item == Items.GOLDEN_SPEAR
                    ? ZombieFarmDropRules.zombifiedPiglinSpearChance(lootingLevel)
                    : ZombieFarmDropRules.zombifiedPiglinSwordChance(lootingLevel);
        }
        if (item == Items.IRON_SWORD) {
            return ZombieFarmDropRules.zombieSwordChance(lootingLevel);
        }
        if (item == Items.IRON_SPEAR) {
            return ZombieFarmDropRules.zombieSpearChance(lootingLevel);
        }
        return ZombieFarmDropRules.zombieShovelChance(lootingLevel);
    }

    private static Item headItem(ZombieFarmKind kind) {
        return kind == ZombieFarmKind.ZOMBIFIED_PIGLIN ? Items.PIGLIN_HEAD : Items.ZOMBIE_HEAD;
    }

    private static ItemStack worn(Item item, RandomSource random) {
        ItemStack weapon = new ItemStack(item);
        int maximumDamage = weapon.getMaxDamage();
        if (maximumDamage > 1) {
            int minimumDamage = maximumDamage / 2;
            weapon.setDamageValue(minimumDamage + random.nextInt(Math.max(1, maximumDamage - minimumDamage)));
        }
        return weapon;
    }

    private static void addStack(List<ItemStack> drops, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return;
        }
        for (ItemStack existing : drops) {
            if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                existing.grow(incoming.getCount());
                return;
            }
        }
        drops.add(incoming);
    }

    public record PreviewOutput(
            ZombieFarmLoot loot,
            ItemStack stack,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount
    ) {
        public PreviewOutput {
            stack = stack.copy();
            if (stack.isEmpty()
                    || probabilityPartsPerMillion < 0
                    || probabilityPartsPerMillion > ZombieFarmDropRules.PROBABILITY_PARTS_PER_MILLION
                    || minimumAmount < 1
                    || maximumAmount < minimumAmount) {
                throw new IllegalArgumentException("Invalid Zombie Farm preview output");
            }
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
