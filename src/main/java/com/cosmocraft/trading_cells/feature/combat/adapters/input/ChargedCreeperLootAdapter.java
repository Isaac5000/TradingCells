package com.cosmocraft.trading_cells.feature.combat.adapters.input;

import com.cosmocraft.trading_cells.feature.combat.adapters.api.CombatEnchantments;
import com.cosmocraft.trading_cells.feature.combat.adapters.output.CombatRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.combat.domain.model.StormShardDropRules;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Adds a guaranteed Storm Shard to charged creepers and lets Looting increase its amount. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class ChargedCreeperLootAdapter {
    private static final int MAX_SUPPORTED_ENCHANTMENT_LEVEL = 255;

    private ChargedCreeperLootAdapter() {
    }

    @SubscribeEvent
    public static void onChargedCreeperDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper) || !creeper.isPowered()) {
            return;
        }

        int looting = 0;
        if (event.getSource().getEntity() instanceof Player player) {
            looting = Math.clamp(
                    CombatEnchantments.lootingLevel(event.getSource().getWeaponItem(), player.registryAccess()),
                    0,
                    MAX_SUPPORTED_ENCHANTMENT_LEVEL
            );
        }
        int amount = 1 + creeper.getRandom().nextInt(StormShardDropRules.maximumAmount(looting));
        addDrops(event, creeper, amount);
    }

    private static void addDrops(LivingDropsEvent event, Creeper creeper, int amount) {
        int remaining = amount;
        int maximumStackSize = CombatRegistrationAdapter.STORM_SHARD_ITEM.get().getDefaultMaxStackSize();
        while (remaining > 0) {
            int count = Math.min(remaining, maximumStackSize);
            event.getDrops().add(new ItemEntity(
                    creeper.level(),
                    creeper.getX(),
                    creeper.getY(),
                    creeper.getZ(),
                    new ItemStack(CombatRegistrationAdapter.STORM_SHARD_ITEM.get(), count)
            ));
            remaining -= count;
        }
    }
}
