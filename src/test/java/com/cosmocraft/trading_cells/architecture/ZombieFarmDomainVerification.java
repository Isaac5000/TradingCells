package com.cosmocraft.trading_cells.architecture;

import com.cosmocraft.trading_cells.shared.mobfarm.domain.model.VanillaSwordTier;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmMenu;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmMenuLayout;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmCycle;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmDropRules;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmKind;
import com.cosmocraft.trading_cells.feature.zombiefarm.domain.model.ZombieFarmLoot;

final class ZombieFarmDomainVerification {
    private ZombieFarmDomainVerification() {
    }

    static void verify() {
        verifyZombieFarmRules();
    }

    private static void verifyZombieFarmRules() {
        require(ZombieFarmMenu.WIDTH == 348 && ZombieFarmMenu.HEIGHT == 210,
                "The Zombie Farm must retain its independent 348x210 menu geometry");
        int inventoryGroupLeft = ZombieFarmMenuLayout.EQUIPMENT_X;
        int inventoryGroupRight = ZombieFarmMenuLayout.PLAYER_INVENTORY_X + 9 * 18;
        require(inventoryGroupLeft - 123 == 343 - inventoryGroupRight,
                "The Zombie Farm inventory and equipment slots must be horizontally centered");
        require(ZombieFarmBlockEntity.OUTPUT_SLOT_COUNT == 18,
                "The Zombie Farm must expose eighteen output slots");
        require(ZombieFarmCycle.effectiveCycleTicks(
                        com.cosmocraft.trading_cells.shared.mobfarm.domain.model.VanillaSwordTier.WOODEN
                                .timingPosition(),
                        0
                ) == 2_400,
                "A wooden sword must take 120 seconds in the Zombie Farm");
        require(ZombieFarmCycle.effectiveCycleTicks(
                        com.cosmocraft.trading_cells.shared.mobfarm.domain.model.VanillaSwordTier.NETHERITE
                                .timingPosition(),
                        5
                ) == 100,
                "A netherite Smite V sword must take five seconds in the Zombie Farm");
        require(ZombieFarmKind.DROWNED.supports(ZombieFarmLoot.COPPER_INGOTS)
                        && ZombieFarmKind.DROWNED.supports(ZombieFarmLoot.NAUTILUS_SHELLS),
                "Drowned filters must expose copper, nautilus shells and their equipment");
        require(ZombieFarmKind.ZOMBIFIED_PIGLIN.supports(ZombieFarmLoot.GOLD_NUGGETS)
                        && ZombieFarmKind.ZOMBIFIED_PIGLIN.supports(ZombieFarmLoot.GOLD_INGOTS)
                        && ZombieFarmKind.ZOMBIFIED_PIGLIN.supports(ZombieFarmLoot.WEAPONS),
                "Zombified Piglins must expose both gold drops and their golden sword");
        require(ZombieFarmKind.ZOGLIN.supports(ZombieFarmLoot.ROTTEN_FLESH)
                        && !ZombieFarmKind.ZOGLIN.supports(ZombieFarmLoot.WEAPONS)
                        && !ZombieFarmKind.ZOGLIN.supports(ZombieFarmLoot.HEADS),
                "Zoglins must remain a flesh-only vanilla target without an invented head");
        require(ZombieFarmKind.values().length == 6,
                "The Zombie Farm selector must expose all six supported zombie-family targets");
        verifyZombieFarmPreview(
                ZombieFarmKind.DROWNED,
                ZombieFarmLoot.COPPER_INGOTS,
                110_000,
                1,
                1
        );
        verifyZombieFarmPreview(
                ZombieFarmKind.DROWNED,
                ZombieFarmLoot.NAUTILUS_SHELLS,
                30_000,
                1,
                1
        );
        verifyZombieFarmPreview(
                ZombieFarmKind.ZOMBIFIED_PIGLIN,
                ZombieFarmLoot.GOLD_NUGGETS,
                500_000,
                1,
                1
        );
        verifyZombieFarmPreview(
                ZombieFarmKind.ZOMBIFIED_PIGLIN,
                ZombieFarmLoot.GOLD_INGOTS,
                25_000,
                1,
                1
        );
        verifyZombieFarmPreview(
                ZombieFarmKind.ZOMBIFIED_PIGLIN,
                ZombieFarmLoot.WEAPONS,
                80_750,
                1,
                1
        );
        verifyZombieFarmPreview(
                ZombieFarmKind.ZOGLIN,
                ZombieFarmLoot.ROTTEN_FLESH,
                1_000_000,
                1,
                3
        );
        require(ZombieFarmDropRules.zombifiedPiglinStackMaximumPerKill(3) == 4
                        && ZombieFarmDropRules.zoglinFleshMaximumPerKill(3) == 6,
                "Looting must scale Zombified Piglin and Zoglin stack maxima exactly like vanilla");
        require(ZombieFarmDropRules.zombieWeaponChance(0, true)
                        == ZombieFarmDropRules.zombieWeaponChance(0) * 5.0D,
                "Hard difficulty must preserve vanilla's fivefold Zombie weapon spawn chance");
    }

    private static void verifyZombieFarmPreview(
            ZombieFarmKind kind,
            ZombieFarmLoot loot,
            int probabilityPartsPerMillion,
            int minimumAmount,
            int maximumAmount
    ) {
        ZombieFarmDropRules.BaseDrop preview = switch (loot) {
            case ROTTEN_FLESH -> kind == ZombieFarmKind.ZOGLIN
                    ? ZombieFarmDropRules.zoglinFleshCycleDrop(0, 1)
                    : ZombieFarmDropRules.zombifiedPiglinStackCycleDrop(0, 1);
            case GOLD_NUGGETS -> ZombieFarmDropRules.zombifiedPiglinStackCycleDrop(0, 1);
            case GOLD_INGOTS -> ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.rarePoolChance(0),
                    1
            );
            case WEAPONS -> ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.zombifiedPiglinSwordChance(0),
                    1
            );
            case COPPER_INGOTS -> ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.copperChance(0),
                    1
            );
            case NAUTILUS_SHELLS -> ZombieFarmDropRules.binaryCycleDrop(
                    ZombieFarmDropRules.drownedNautilusChance(),
                    1
            );
            default -> throw new IllegalArgumentException("Unsupported Zombie Farm preview assertion: " + loot);
        };
        require(kind.supports(loot), "Zombie Farm preview references an unavailable filter for " + kind + '/' + loot);
        require(preview.probabilityPartsPerMillion() == probabilityPartsPerMillion,
                "Zombie Farm REI probability changed for " + kind + '/' + loot);
        require(preview.minimumAmount() == minimumAmount && preview.maximumAmount() == maximumAmount,
                "Zombie Farm REI amount changed for " + kind + '/' + loot);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
