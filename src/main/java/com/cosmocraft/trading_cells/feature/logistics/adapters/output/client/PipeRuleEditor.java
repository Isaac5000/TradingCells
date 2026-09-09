package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationClipboard;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeTargetSelectorItem;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;

final class PipeRuleEditor {
    private final LogisticsResourceType type;
    private final boolean advanced;
    private final String parent;
    private final PipeFilterRule.Action action;
    private final Consumer<PipeFilterRule> submit;
    private PipeFilterRule.MatchKind kind;
    private PipeFilterRule.ComponentMatch match;
    private boolean inverted;
    private PipeRuleTarget target;
    private EditBox identifier, components, channel;
    private Button kindButton;
    private ItemStack icon = ItemStack.EMPTY;
    private int left, top;
    private Font font;
    private final PipeFilterRule initial;

    PipeRuleEditor(LogisticsResourceType type, PipeResourceProfile profile, boolean advanced, PipeFilterRule initial, Consumer<PipeFilterRule> submit) {
        this.type = type; this.advanced = advanced; this.parent = profile.channel(); this.initial = initial; this.submit = submit;
        action = initial.action(); kind = initial.matchKind(); match = initial.componentMatch(); inverted = initial.inverted(); target = initial.target();
    }

    void init(Font font, int left, int top, Consumer<AbstractWidget> widgets, Runnable cancel) {
        this.font = font; this.left = left; this.top = top;
        String keyText = identifier == null ? initial.key() : identifier.getValue();
        String componentText = components == null ? initial.componentFingerprint() : components.getValue();
        String channelText = channel == null ? null : channel.getValue();
        kindButton = Button.builder(Component.literal(kind == PipeFilterRule.MatchKind.ID ? "ID" : "Tag"), button -> {
            kind = kind == PipeFilterRule.MatchKind.ID ? PipeFilterRule.MatchKind.TAG : PipeFilterRule.MatchKind.ID;
            button.setMessage(Component.literal(kind == PipeFilterRule.MatchKind.ID ? "ID" : "Tag")); updateIcon();
        }).bounds(left + 36, top + 36, 46, 18).build();
        widgets.accept(kindButton);
        identifier = new EditBox(font, left + 86, top + 36, 276, 18, label("filter.identifier"));
        identifier.setMaxLength(PipeFilterRule.MAX_KEY_LENGTH); identifier.setValue(keyText);
        identifier.setResponder(value -> updateIcon()); widgets.accept(identifier);
        components = new EditBox(font, left + 12, top + 74, 274, 18, label("filter.components"));
        components.setMaxLength(2048); components.setValue(componentText); components.active = advanced; widgets.accept(components);
        var matchButton = Button.builder(label("match." + match.name().toLowerCase(Locale.ROOT)), button -> {
            match = PipeFilterRule.ComponentMatch.values()[(match.ordinal() + 1) % PipeFilterRule.ComponentMatch.values().length];
            button.setMessage(label("match." + match.name().toLowerCase(Locale.ROOT)));
        }).bounds(left + 290, top + 74, 72, 18).build();
        matchButton.active = advanced; widgets.accept(matchButton);
        channel = new EditBox(font, left + 12, top + 112, 350, 18, label("filter.route_channel"));
        int remaining = parent.isEmpty() ? 256 : Math.max(0, 255 - parent.length());
        channel.setMaxLength(remaining);
        String route = initial.routeChannel();
        channel.setValue(channelText != null ? channelText : parent.isEmpty() ? route : route.startsWith(parent + "/") ? route.substring(parent.length() + 1) : "");
        channel.active = advanced && remaining > 0; widgets.accept(channel);
        var invertButton = Button.builder(label(inverted ? "rule.inverted" : "rule.inherit"), button -> {
            inverted = !inverted; button.setMessage(label(inverted ? "rule.inverted" : "rule.inherit"));
        }).bounds(left + 204, top + 208, 158, 18).build();
        invertButton.active = advanced; widgets.accept(invertButton);
        var clear = Button.builder(Component.literal("x"), button -> target = null).bounds(left + 344, top + 163, 18, 18)
                .tooltip(Tooltip.create(label("rule.clear_target"))).build();
        clear.active = advanced; widgets.accept(clear);
        widgets.accept(Button.builder(Component.translatable("gui.cancel"), button -> cancel.run()).bounds(left + 204, top + 230, 76, 16).build());
        widgets.accept(Button.builder(label("rule.save"), button -> save()).bounds(left + 284, top + 230, 78, 16).build());
        if (!advanced) {
            var tooltip = Tooltip.create(label("requires", Component.translatable("item.trading_cells.ultimate_pipe_upgrade")));
            components.setTooltip(tooltip); channel.setTooltip(tooltip); invertButton.setTooltip(tooltip); matchButton.setTooltip(tooltip);
        }
        updateIcon();
    }

    boolean save() {
        String route;
        try {
            route = advanced && !channel.getValue().isBlank() ? PipeFilterRule.subchannel(parent, channel.getValue()) : "";
            channel.setTextColor(0xFFF5F5F5);
        } catch (IllegalArgumentException ignored) { channel.setTextColor(0xFFFF7777); return false; }
        var rule = new PipeFilterRule(action, kind, identifier.getValue(), match, components.getValue(),
                route, inverted, target);
        if (!PipeConfigurationClipboard.known(type, rule)) { identifier.setTextColor(0xFFFF7777); return false; }
        if (!rule.componentFingerprint().isEmpty()) {
            try { TagParser.parseCompoundFully(rule.componentFingerprint()); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) { components.setTextColor(0xFFFF7777); return false; }
        }
        submit.accept(rule);
        return true;
    }

    boolean click(double x, double y, int button, ItemStack carried) {
        if (inside(x, y, 12, 36, 18, 18)) {
            if (button == 1) { identifier.setValue(""); return true; }
            if (carried.isEmpty()) { return true; }
            String id = BuiltInRegistries.ITEM.getKey(carried.getItem()).toString();
            if ((type == LogisticsResourceType.FLUID || type == LogisticsResourceType.GAS) && carried.getItem() instanceof BucketItem bucket) {
                id = BuiltInRegistries.FLUID.getKey(bucket.content).toString();
            }
            if (type == LogisticsResourceType.ENERGY) { id = "neoforge:energy"; }
            kind = PipeFilterRule.MatchKind.ID; kindButton.setMessage(Component.literal("ID"));
            identifier.setValue(id);
            if (advanced && type == LogisticsResourceType.ITEM) { components.setValue(LogisticsComponentData.fingerprint(carried.getComponentsPatch())); }
            return true;
        }
        if (inside(x, y, 204, 164, 18, 18)) {
            if (advanced) {
                if (button == 1) { target = null; }
                else { var recorded = PipeTargetSelectorItem.target(carried); if (recorded != null) { target = recorded; } }
            }
            return true;
        }
        return false;
    }

    void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PipeConfigurationScreen.drawSlot(graphics, left + 12, top + 36);
        if (!icon.isEmpty()) { graphics.fakeItem(icon, left + 13, top + 37); }
        graphics.text(font, label("filter.components"), left + 12, top + 62, 0xFFB8C1C4, false);
        Component channelLabel = parent.isEmpty() ? label("channel") : Component.literal(parent + "/");
        graphics.text(font, font.plainSubstrByWidth(channelLabel.getString(), 350), left + 12, top + 100, 0xFFB8C1C4, false);
        graphics.text(font, label("rule.target"), left + 204, top + 150, 0xFFB8C1C4, false);
        PipeConfigurationScreen.drawSlot(graphics, left + 204, top + 164);
        if (target != null) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var pos = new net.minecraft.core.BlockPos(target.x(), target.y(), target.z());
            if (minecraft.level != null && minecraft.level.dimension().identifier().toString().equals(target.dimension()) && minecraft.level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                graphics.fakeItem(minecraft.level.getBlockState(pos).getBlock().asItem().getDefaultInstance(), left + 205, top + 165);
            }
            String xyz = target.x() + ", " + target.y() + ", " + target.z();
            graphics.text(font, font.plainSubstrByWidth(xyz, 114), left + 226, top + 169, 0xFF65DDE2, false);
            graphics.text(font, font.plainSubstrByWidth(target.dimension(), 158), left + 204, top + 189, 0xFFB8C1C4, false);
            if (inside(mouseX, mouseY, 204, 164, 134, 36)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(xyz), Component.literal(target.dimension())), mouseX, mouseY, ItemStack.EMPTY);
            }
        } else { graphics.text(font, label("rule.any_target"), left + 226, top + 169, 0xFFB8C1C4, false); }
    }

    private void updateIcon() {
        if (identifier != null) { icon = PipeEditorIcons.rule(type, new PipeFilterRule(action, kind, identifier.getValue(), match, "", "")); }
    }
    EditBox channelField() { return channel; }
    String parentChannel() { return parent; }
    private boolean inside(double x, double y, int a, int b, int w, int h) { return x >= left + a && x < left + a + w && y >= top + b && y < top + b + h; }
    private static Component label(String name, Object... args) { return Component.translatable("gui.trading_cells.pipe." + name, args); }
}
