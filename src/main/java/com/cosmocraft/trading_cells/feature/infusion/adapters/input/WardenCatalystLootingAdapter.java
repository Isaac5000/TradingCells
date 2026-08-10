package com.cosmocraft.trading_cells.feature.infusion.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Applies Looting to the Warden's existing Sculk Catalyst drop. */
@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class WardenCatalystLootingAdapter {
    private static final int MAX_SUPPORTED_ENCHANTMENT_LEVEL = 255;

    private WardenCatalystLootingAdapter() {
    }

    @SubscribeEvent
    public static void onWardenDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Warden warden)
                || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        ItemStack weapon = event.getSource().getWeaponItem();
        int bonusLevel = enchantmentLevel(player, weapon, Enchantments.LOOTING);
        if (bonusLevel <= 0) {
            return;
        }

        int bonus = 1 + warden.getRandom().nextInt(bonusLevel);

        List<ItemEntity> extraDrops = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(Items.SCULK_CATALYST)) {
                addBonus(drop, bonus, extraDrops);
                break;
            }
        }
        event.getDrops().addAll(extraDrops);
    }

    private static int enchantmentLevel(
            Player player,
            ItemStack weapon,
            ResourceKey<Enchantment> enchantment
    ) {
        if (weapon.isEmpty()) {
            return 0;
        }
        return player.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(enchantment))
                .map(weapon::getEnchantmentLevel)
                .map(level -> Math.clamp(level, 0, MAX_SUPPORTED_ENCHANTMENT_LEVEL))
                .orElse(0);
    }

    private static void addBonus(ItemEntity drop, int bonus, List<ItemEntity> extraDrops) {
        ItemStack catalyst = drop.getItem();
        int total = catalyst.getCount() + bonus;
        int stackSize = catalyst.getMaxStackSize();
        catalyst.setCount(Math.min(total, stackSize));
        int remaining = total - catalyst.getCount();
        while (remaining > 0) {
            int count = Math.min(remaining, stackSize);
            ItemEntity extra = new ItemEntity(
                    drop.level(),
                    drop.getX(),
                    drop.getY(),
                    drop.getZ(),
                    catalyst.copyWithCount(count)
            );
            extra.setDeltaMovement(drop.getDeltaMovement());
            extraDrops.add(extra);
            remaining -= count;
        }
    }
}
