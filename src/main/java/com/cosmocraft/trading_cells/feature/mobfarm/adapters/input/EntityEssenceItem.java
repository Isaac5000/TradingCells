package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;

public final class EntityEssenceItem extends Item {
    private final boolean module;
    public EntityEssenceItem(Properties properties, boolean module) { super(properties); this.module = module; }

    @Override public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.Entity owner, net.minecraft.world.entity.EquipmentSlot slot) {
        if (level.getGameTime() % 20 == 0 && EntityEssenceData.classificationVersion(stack) == 0
                && EntityEssenceData.ensureClassified(level, stack)
                && owner instanceof net.minecraft.world.entity.player.Player player) {
            player.getInventory().setChanged();
        }
    }

    @SuppressWarnings("deprecation")
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        if (EntityEssenceData.entityTypeId(stack) == null) { return; }
        tooltip.accept(Component.translatable("gui.trading_cells.essence.tier", EntityEssenceData.tier(stack).name())
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        if (flag.isAdvanced()) {
            tooltip.accept(Component.literal(EntityEssenceData.entityTypeId(stack).toString())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    @Override public Component getName(ItemStack stack) {
        if (EntityEssenceData.entityTypeId(stack) == null) { return super.getName(stack); }
        return Component.translatable(stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get())
                ? "item.trading_cells.raw_creature_essence_vial.named" : module ? "item.trading_cells.entity_module.named"
                : "item.trading_cells.entity_essence.named", EntityEssenceData.displayName(stack));
    }
}
