package com.cosmocraft.trading_cells.gametest.shared;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.input.PiglinCapturerItem;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

/** Reusable world fixtures whose implementation is irrelevant to feature assertions. */
public final class GameTestFixtures {
    public static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    private GameTestFixtures() {
    }

    public static ItemStack adultVillagerCapture(GameTestHelper helper) {
        Villager villager = EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(villager != null, "Could not create GameTest villager");
        ItemStack worker = new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.VILLAGER));
        CapturedMobStackAdapter.setData(
                CapturedMobKind.VILLAGER,
                worker,
                CapturedMobStackAdapter.createVillagerData(villager)
        );
        return worker;
    }

    public static ItemStack adultPiglinCapture(GameTestHelper helper) {
        Piglin piglin = EntityTypes.PIGLIN.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(piglin != null, "Could not create GameTest piglin");
        ItemStack worker = new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.PIGLIN));
        CapturedMobStackAdapter.setData(
                CapturedMobKind.PIGLIN,
                worker,
                PiglinCapturerItem.createCapturedPiglinData(piglin)
        );
        return worker;
    }
}