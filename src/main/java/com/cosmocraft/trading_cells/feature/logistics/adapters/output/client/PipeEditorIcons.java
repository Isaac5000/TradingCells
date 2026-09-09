package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class PipeEditorIcons {
    private PipeEditorIcons() { }

    static ItemStack rule(LogisticsResourceType type, PipeFilterRule rule) {
        String key = rule.key().substring(rule.key().lastIndexOf('|') + 1);
        Identifier id = Identifier.tryParse(key);
        if (id == null) { return ItemStack.EMPTY; }
        if (type == LogisticsResourceType.ITEM) {
            return rule.matchKind() == PipeFilterRule.MatchKind.ID
                    ? BuiltInRegistries.ITEM.get(id).map(holder -> holder.value().getDefaultInstance()).orElse(ItemStack.EMPTY)
                    : BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id)).flatMap(tag -> tag.stream().findFirst())
                    .map(holder -> holder.value().getDefaultInstance()).orElse(ItemStack.EMPTY);
        }
        if (type == LogisticsResourceType.ENERGY) { return Items.REDSTONE.getDefaultInstance(); }
        return rule.matchKind() == PipeFilterRule.MatchKind.ID
                ? BuiltInRegistries.FLUID.get(id).map(holder -> holder.value().getBucket().getDefaultInstance()).orElse(ItemStack.EMPTY)
                : BuiltInRegistries.FLUID.get(TagKey.create(Registries.FLUID, id)).flatMap(tag -> tag.stream().findFirst())
                .map(holder -> holder.value().getBucket().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }

    static final class IconButton extends Button {
        private final ItemStack icon;
        private final boolean selected;

        IconButton(int x, int y, int size, ItemStack icon, Component message, boolean selected, OnPress press) {
            super(x, y, size, size, message, press, DEFAULT_NARRATION);
            this.icon = icon;
            this.selected = selected;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            extractDefaultSprite(graphics);
            graphics.fakeItem(icon, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
            if (selected) { graphics.fill(getX() + 3, getY() + getHeight() - 3, getX() + getWidth() - 3, getY() + getHeight() - 2, 0xFF73CEC2); }
        }
    }
}
