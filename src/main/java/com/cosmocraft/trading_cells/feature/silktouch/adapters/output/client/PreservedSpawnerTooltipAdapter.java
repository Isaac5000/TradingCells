package com.cosmocraft.trading_cells.feature.silktouch.adapters.output.client;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.PreservedSpawnerItemAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SpawnerRedstoneControlAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/** Adds concise, localized entity details to trusted spawners and modified eggs. */
@EventBusSubscriber(modid = TradingCells.MOD_ID, value = Dist.CLIENT)
public final class PreservedSpawnerTooltipAdapter {
    private static final ChatFormatting LABEL_COLOR = ChatFormatting.DARK_AQUA;
    private static final ChatFormatting ARMOR_LABEL_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting VALUE_COLOR = ChatFormatting.WHITE;
    private static final ChatFormatting SHIFT_HINT_COLOR = ChatFormatting.BLUE;
    private static final ChatFormatting FEATURE_COLOR = ChatFormatting.RED;

    private PreservedSpawnerTooltipAdapter() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        boolean entityDescription = PreservedSpawnerItemAdapter.isTrustedSpawner(stack)
                || PreservedSpawnerItemAdapter.isTrustedEgg(stack);
        boolean redstoneControl = SpawnerRedstoneControlAdapter.isInstalled(stack);
        if (!entityDescription && !redstoneControl) {
            return;
        }

        var level = Minecraft.getInstance().level;
        boolean detailsVisible = isDetailsKeyDown();
        List<Component> lines = new ArrayList<>();
        if (entityDescription && level != null) {
            PreservedSpawnerItemAdapter.describe(level, stack).ifPresent(description -> {
                event.getToolTip().removeIf(component -> component.getString()
                        .equals(description.rootType().getDescription().getString()));
                lines.addAll(descriptionLines(description, detailsVisible));
            });
        }
        event.getToolTip().addAll(1, withSpawnerFeatures(
                lines,
                redstoneControl,
                detailsVisible
        ));
    }

    /** Builds the same localized entity summary for item and Jade tooltips. */
    public static List<Component> descriptionLines(
            PreservedSpawnerItemAdapter.EntityDescription description,
            boolean detailsVisible
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(field(
                "tooltip.trading_cells.spawner.entity",
                description.selected().getType().getDescription()
        ));
        if (!description.modified()) {
            return List.copyOf(lines);
        }
        if (!detailsVisible) {
            lines.add(Component.translatable("tooltip.trading_cells.spawner.shift_details")
                    .withStyle(SHIFT_HINT_COLOR));
            return List.copyOf(lines);
        }

        if (!description.weapon().isEmpty()) {
            lines.add(field("tooltip.trading_cells.spawner.weapon", description.weapon().getHoverName()));
        }
        if (description.selected() instanceof LivingEntity living) {
            addArmor(lines, living);
            addEffects(lines, living);
        }
        addEntityList(lines, "tooltip.trading_cells.spawner.passenger", description.passengers());
        if (description.mount() != null) {
            lines.add(field("tooltip.trading_cells.spawner.mount", description.mount().getDescription()));
        }
        return List.copyOf(lines);
    }

    /** Adds installed spawner features without duplicating the Shift hint. */
    public static List<Component> withSpawnerFeatures(
            List<Component> baseLines,
            boolean installed,
            boolean detailsVisible
    ) {
        if (!installed) {
            return List.copyOf(baseLines);
        }

        List<Component> lines = new ArrayList<>(baseLines);
        if (!detailsVisible) {
            Component hint = Component.translatable("tooltip.trading_cells.spawner.shift_details")
                    .withStyle(SHIFT_HINT_COLOR);
            if (lines.stream().noneMatch(line -> line.getString().equals(hint.getString()))) {
                lines.add(hint);
            }
            return List.copyOf(lines);
        }

        lines.add(labelOnly("tooltip.trading_cells.spawner.features"));
        lines.add(Component.literal("    ")
                .append(Component.translatable("tooltip.trading_cells.spawner.feature.redstone_control")
                        .withStyle(FEATURE_COLOR)));
        return List.copyOf(lines);
    }

    private static void addArmor(List<Component> tooltip, LivingEntity entity) {
        List<ArmorEntry> armor = new ArrayList<>();
        addArmorPart(armor, entity, EquipmentSlot.HEAD, "tooltip.trading_cells.spawner.armor.head");
        addArmorPart(armor, entity, EquipmentSlot.CHEST, "tooltip.trading_cells.spawner.armor.chest");
        addArmorPart(armor, entity, EquipmentSlot.LEGS, "tooltip.trading_cells.spawner.armor.legs");
        addArmorPart(armor, entity, EquipmentSlot.FEET, "tooltip.trading_cells.spawner.armor.feet");
        if (armor.isEmpty()) {
            return;
        }

        tooltip.add(labelOnly("tooltip.trading_cells.spawner.armor"));
        for (ArmorEntry entry : armor) {
            tooltip.add(indentedField(entry.labelKey(), entry.stack().getHoverName()));
        }
    }

    private static void addArmorPart(
            List<ArmorEntry> armor,
            LivingEntity entity,
            EquipmentSlot slot,
            String labelKey
    ) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (!stack.isEmpty()) {
            armor.add(new ArmorEntry(labelKey, stack));
        }
    }

    private static void addEffects(List<Component> tooltip, LivingEntity entity) {
        if (entity.getActiveEffects().isEmpty()) {
            return;
        }

        MutableComponent values = Component.empty();
        int index = 0;
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (index++ > 0) {
                values.append(Component.literal(", ").withStyle(VALUE_COLOR));
            }
            values.append(effectName(effect));
        }
        tooltip.add(field("tooltip.trading_cells.spawner.effect", values));
    }

    private static Component effectName(MobEffectInstance effect) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy().withStyle(VALUE_COLOR);
        int level = effect.getAmplifier() + 1;
        if (level > 1) {
            name.append(Component.literal(" ").withStyle(VALUE_COLOR));
            name.append(Component.translatable("enchantment.level." + level).withStyle(VALUE_COLOR));
        }
        return name;
    }

    private static void addEntityList(
            List<Component> tooltip,
            String labelKey,
            List<EntityType<?>> entities
    ) {
        if (entities.isEmpty()) {
            return;
        }

        MutableComponent values = Component.empty();
        for (int index = 0; index < entities.size(); index++) {
            if (index > 0) {
                values.append(Component.literal(", ").withStyle(VALUE_COLOR));
            }
            values.append(entities.get(index).getDescription().copy().withStyle(VALUE_COLOR));
        }
        tooltip.add(field(labelKey, values));
    }

    private static MutableComponent field(String labelKey, Component value) {
        return Component.literal("- ").withStyle(VALUE_COLOR)
                .append(Component.translatable(labelKey).withStyle(LABEL_COLOR))
                .append(Component.literal(": ").withStyle(LABEL_COLOR))
                .append(value.copy().withStyle(VALUE_COLOR));
    }

    private static MutableComponent indentedField(String labelKey, Component value) {
        return Component.literal("    ").withStyle(ARMOR_LABEL_COLOR)
                .append(Component.translatable(labelKey).withStyle(ARMOR_LABEL_COLOR))
                .append(Component.literal(": ").withStyle(ARMOR_LABEL_COLOR))
                .append(value.copy().withStyle(VALUE_COLOR));
    }

    private static MutableComponent labelOnly(String labelKey) {
        return Component.literal("- ").withStyle(VALUE_COLOR)
                .append(Component.translatable(labelKey).withStyle(LABEL_COLOR))
                .append(Component.literal(":").withStyle(LABEL_COLOR));
    }

    private static boolean isDetailsKeyDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private record ArmorEntry(String labelKey, ItemStack stack) {
    }
}
