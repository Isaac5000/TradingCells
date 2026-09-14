package com.cosmocraft.trading_cells.feature.logistics.adapters.output.client;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import com.cosmocraft.trading_cells.platform.neoforge.network.PipeConfigurationPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class PipeConfigurationScreen extends AbstractContainerScreen<PipeConfigurationMenu> {
    public static final int WIDTH = 374, HEIGHT = 248;
    private static final int WHITE = 0xFFF5F5F5, MUTED = 0xFFB8C1C4;
    private PipeFaceConfiguration draft;
    private LogisticsResourceType selected;
    private EditBox priority, channel;
    private PipeRuleEditor editor;
    private PipeChannelCompletion completion;
    private List<ItemStack> ruleIcons = List.of();
    private int selectedRule = -1, pendingUpgradeTicks;
    private long observedUpgradeGeneration;
    private double scroll, targetScroll;
    private boolean sent, ghostClick;
    private CompoundTag lastSent;

    public PipeConfigurationScreen(PipeConfigurationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        draft = menu.initial();
        selected = java.util.Arrays.stream(LogisticsResourceType.values()).filter(menu.kind()::supports).findFirst().orElseThrow();
        inventoryLabelX = 36; inventoryLabelY = 149;
        for (var type : LogisticsResourceType.values()) {
            var profile = draft.profile(type);
            if (profile.filterMode() != PipeResourceProfile.FilterMode.RULES) { continue; }
            var rules = profile.filters().stream().map(rule -> new PipeFilterRule(rule.action(), rule.matchKind(), rule.key(), rule.componentMatch(),
                    rule.componentFingerprint(), rule.routeChannel(), (rule.action() == PipeFilterRule.Action.ALLOW) != rule.inverted(), rule.target())).toList();
            draft.setProfile(type, new PipeResourceProfile(profile.enabled(), profile.channel(), rules,
                    rules.isEmpty() ? PipeResourceProfile.FilterMode.OFF : PipeResourceProfile.FilterMode.BLACKLIST, profile.routingMode()));
        }
    }

    @Override
    protected void init() {
        super.init();
        if (completion != null) { completion.close(); completion = null; }
        priority = null; channel = null;
        menu.setRuleEditing(editor != null);
        if (editor != null) {
            editor.init(font, leftPos, topPos, this::addRenderableWidget, this::closeRule);
            completion = new PipeChannelCompletion(menu, selected, editor.channelField(), editor.parentChannel(), () -> draft.profile(selected));
            return;
        }
        var tier = draft.upgradeTier();
        var profile = draft.profile(selected);
        button(label("mode." + draft.mode().serializedName()), 12, 28, 108, ignored -> {
            storeFields(); draft.setMode(draft.mode().next(), true); rebuildWidgets();
        });
        if (menu.kind() == PipeKind.UNIVERSAL) {
            int index = 0;
            for (var type : LogisticsResourceType.values()) {
                icon(LogisticsRegistrationAdapter.pipeItem(PipeKind.valueOf(type.name())).get().getDefaultInstance(),
                        label("resource." + type.serializedName()), 128 + index++ * 24 + (selected == type ? -1 : 0),
                        selected == type ? 23 : 28, selected == type ? 24 : 20, selected == type, ignored -> {
                            storeFields(); selected = type; selectedRule = -1; scroll = targetScroll = 0; rebuildWidgets();
                        });
            }
        }
        priority = addRenderableWidget(new EditBox(font, leftPos + 276, topPos + 28, 86, 18, label("priority")));
        priority.setMaxLength(32); priority.setValue(Integer.toString(draft.priority()));
        var routing = draft.effectiveProfile(selected).routingMode();
        ItemStack routingIcon = switch (routing) {
            case NEAREST -> Items.COMPASS.getDefaultInstance(); case FARTHEST -> Items.SPYGLASS.getDefaultInstance();
            case EQUAL -> Items.HOPPER.getDefaultInstance(); case RANDOM -> Items.DROPPER.getDefaultInstance();
        };
        gate(icon(routingIcon, label("routing." + routing.name().toLowerCase(Locale.ROOT)), 12, 78, 26, false, ignored -> {
            storeFields(); var current = draft.profile(selected);
            setProfile(new PipeResourceProfile(current.enabled(), current.channel(), current.filters(), current.filterMode(),
                    PipeRoutingMode.values()[(current.routingMode().ordinal() + 1) % PipeRoutingMode.values().length]));
        }), tier.allowsRouting(), PipeUpgradeTier.BASIC);
        ItemStack filterIcon = switch (profile.filterMode()) {
            case OFF -> Items.BARRIER.getDefaultInstance(); case BLACKLIST -> Items.COAL.getDefaultInstance();
            case WHITELIST -> Items.PAPER.getDefaultInstance(); case RULES -> Items.WRITABLE_BOOK.getDefaultInstance();
        };
        gate(icon(filterIcon, label("filter.mode." + profile.filterMode().name().toLowerCase(Locale.ROOT)), 12, 108, 26, false, ignored -> {
            storeFields(); var current = draft.profile(selected);
            var next = switch (current.filterMode()) {
                case OFF, RULES -> PipeResourceProfile.FilterMode.WHITELIST;
                case WHITELIST -> PipeResourceProfile.FilterMode.BLACKLIST;
                case BLACKLIST -> PipeResourceProfile.FilterMode.OFF;
            };
            setProfile(new PipeResourceProfile(current.enabled(), current.channel(), current.filters(), next, current.routingMode()));
        }), tier.allowsFilters(), PipeUpgradeTier.IMPROVED);
        gate(button(label("rule.add"), 198, 124, 50, ignored -> openRule(-1)),
                tier.allowsFilters() && profile.filters().size() < PipeResourceProfile.MAX_FILTERS, PipeUpgradeTier.IMPROVED);
        gate(button(label("rule.edit"), 252, 124, 52, ignored -> openRule(selectedRule)),
                tier.allowsFilters() && selectedRule >= 0, PipeUpgradeTier.IMPROVED);
        gate(button(label("rule.remove"), 308, 124, 54, ignored -> {
            storeFields(); var rules = new ArrayList<>(draft.profile(selected).filters());
            if (selectedRule >= 0 && selectedRule < rules.size()) { rules.remove(selectedRule); }
            selectedRule = Math.min(selectedRule, rules.size() - 1); replaceRules(rules);
        }), tier.allowsFilters() && selectedRule >= 0, PipeUpgradeTier.IMPROVED);
        button(label(profile.enabled() ? "enabled" : "disabled"), 204, 146, 158, ignored -> {
            storeFields(); var current = draft.profile(selected);
            setProfile(new PipeResourceProfile(!current.enabled(), current.channel(), current.filters(), current.filterMode(), current.routingMode()));
        });
        channel = addRenderableWidget(new EditBox(font, leftPos + 204, topPos + 179, 158, 18, label("channel")));
        channel.setMaxLength(PipeFilterRule.MAX_CHANNEL_LENGTH); channel.setValue(profile.channel());
        gate(channel, tier.allowsChannels(), PipeUpgradeTier.ADVANCED);
        completion = new PipeChannelCompletion(menu, selected, channel, "", () -> draft.profile(selected));
        button(label("copy"), 204, 202, 76, ignored -> { storeFields(); minecraft.keyboardHandler.setClipboard(PipeConfigurationClipboard.copy(draft)); });
        button(label("paste"), 284, 202, 78, ignored -> {
            storeFields(); draft = PipeConfigurationClipboard.paste(minecraft.keyboardHandler.getClipboard(), draft); rebuildWidgets();
        });
        button(Component.translatable("gui.done"), 204, 224, 158, ignored -> onClose());
        ruleIcons = profile.filters().stream().map(rule -> PipeEditorIcons.rule(selected, rule)).toList();
        scroll = Math.clamp(scroll, 0, maximumScroll()); targetScroll = Math.clamp(targetScroll, 0, maximumScroll());
    }

    private void openRule(int index) {
        storeFields(); var profile = draft.profile(selected);
        if (index >= profile.filters().size()) { return; }
        var rule = index < 0 ? new PipeFilterRule(PipeFilterRule.Action.ALLOW, PipeFilterRule.MatchKind.ID, "", PipeFilterRule.ComponentMatch.IGNORE, "", "")
                : profile.filters().get(index);
        editor = new PipeRuleEditor(selected, profile, draft.upgradeTier().allowsAdvancedRules(), rule, saved -> {
            var rules = new ArrayList<>(draft.profile(selected).filters());
            if (index >= 0) { rules.set(index, saved); } else if (rules.size() < PipeResourceProfile.MAX_FILTERS) { rules.add(saved); }
            selectedRule = index >= 0 ? index : rules.size() - 1;
            editor = null; replaceRules(rules);
        });
        rebuildWidgets();
    }

    private void closeRule() { editor = null; rebuildWidgets(); }
    private void replaceRules(List<PipeFilterRule> rules) {
        var current = draft.profile(selected);
        setProfile(new PipeResourceProfile(current.enabled(), current.channel(), rules, current.filterMode(), current.routingMode()));
    }
    private void setProfile(PipeResourceProfile profile) { draft.setProfile(selected, profile); rebuildWidgets(); }
    private void storeFields() {
        if (priority == null || channel == null) { return; }
        try { draft.setPriority(Integer.parseInt(priority.getValue())); } catch (NumberFormatException ignored) { }
        var current = draft.profile(selected);
        try { draft.setProfile(selected, current.withChannel(channel.getValue())); channel.setTextColor(WHITE); }
        catch (IllegalArgumentException ignored) { channel.setTextColor(0xFFFF7777); }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput input) {
        // A drag is a three-packet transaction. Never interrupt it after its slot-selection packet.
        sendDraft();
        PipeUpgradeItem.withProfiles(menu.getSlot(0).getItem(), draft.saveActiveProfiles());
        ItemStack before = menu.getSlot(0).getItem().copy();
        super.slotClicked(slot, slotId, button, input);
        if (!ItemStack.matches(before, menu.getSlot(0).getItem())) {
            var stack = menu.getSlot(0).getItem();
            var tier = stack.getItem() instanceof PipeUpgradeItem upgrade ? upgrade.tier() : PipeUpgradeTier.BARE;
            var profiles = PipeUpgradeItem.storedProfiles(stack);
            draft = draft.withReplacementUpgrade(tier, profiles == null ? new CompoundTag() : profiles);
            pendingUpgradeTicks = 30;
            selectedRule = -1;
            rebuildWidgets();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (completion != null) { completion.tick(); }
        boolean replaced = menu.upgradeGeneration() != observedUpgradeGeneration;
        // Native clicks predict the physical slot. Acknowledgements must not erase edits made meanwhile.
        if (replaced) {
            observedUpgradeGeneration = menu.upgradeGeneration();
            if (pendingUpgradeTicks == 0) { acceptServerUpgrade(); }
        }
        if (pendingUpgradeTicks > 0 && --pendingUpgradeTicks == 0
                && menu.latest().upgradeTier() != draft.upgradeTier()) {
            acceptServerUpgrade();
        }
    }

    private void acceptServerUpgrade() {
        draft = menu.latest(); lastSent = draft.save(); editor = null; selectedRule = -1; rebuildWidgets();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ghostClick = false;
        if (completion != null && completion.click(event.x(), event.y())) { ghostClick = true; return true; }
        if (editor != null && editor.click(event.x(), event.y(), event.button(), menu.getCarried())) { ghostClick = true; return true; }
        if (editor == null && overList(event.x(), event.y())) {
            int index = (int) ((event.y() - topPos - 52 + scroll) / 22);
            selectedRule = index < draft.profile(selected).filters().size() ? index : -1;
            storeFields(); rebuildWidgets(); ghostClick = true; return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (ghostClick) { ghostClick = false; return true; }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!event.isEscape() && event.key() != 258 && getFocused() instanceof EditBox field && field.isFocused()) {
            field.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (completion != null && completion.scroll(x, y, dy)) { return true; }
        if (editor == null && overList(x, y)) { targetScroll = Math.clamp(targetScroll - dy * 16, 0, maximumScroll()); return true; }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (completion != null) { completion.draw(graphics, font, mouseX, mouseY); }
    }

    @Override
    public void resize(int width, int height) { storeFields(); super.resize(width, height); }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, 0xFF899296);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + WIDTH - 2, topPos + HEIGHT - 2, 0xF0272D30);
        for (var slot : menu.slots) { if (slot.isActive()) { drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1); } }
        if (editor != null) { editor.draw(graphics, mouseX, mouseY); return; }
        graphics.fill(leftPos + 44, topPos + 50, leftPos + 363, topPos + 122, 0xFF727D82);
        graphics.fill(leftPos + 46, topPos + 52, leftPos + 361, topPos + 120, 0xFF1F2528);
        scroll += (targetScroll - scroll) * .35;
        var rules = draft.profile(selected).filters();
        graphics.enableScissor(leftPos + 46, topPos + 52, leftPos + 356, topPos + 120);
        for (int index = 0; index < rules.size(); index++) {
            int y = topPos + 52 + index * 22 - (int) scroll;
            if (y + 22 <= topPos + 52 || y >= topPos + 120) { continue; }
            var rule = rules.get(index);
            graphics.fill(leftPos + 47, y + 1, leftPos + 355, y + 21, index == selectedRule ? 0xFF506663 : 0xFF343C40);
            var icon = ruleIcons.get(index);
            if (!icon.isEmpty()) { graphics.fakeItem(icon, leftPos + 50, y + 3); }
            String name = rule.matchKind() == PipeFilterRule.MatchKind.TAG ? "#" + rule.key() : rule.key();
            graphics.text(font, font.plainSubstrByWidth(name, 280), leftPos + 72, y + 3, rule.inverted() ? 0xFFFFBC80 : WHITE, false);
            String detail = rule.routeChannel().isEmpty() ? rule.target() == null ? "" : label("rule.target").getString() : rule.routeChannel();
            graphics.text(font, font.plainSubstrByWidth(detail, 280), leftPos + 72, y + 12, MUTED, false);
            if (overList(mouseX, mouseY) && mouseY >= y && mouseY < y + 22) {
                var lines = new ArrayList<Component>(); lines.add(Component.literal(name));
                if (!rule.routeChannel().isEmpty()) { lines.add(Component.literal(rule.routeChannel())); }
                if (rule.target() != null) { var target = rule.target(); lines.add(Component.literal(target.dimension() + " " + target.x() + ", " + target.y() + ", " + target.z())); }
                graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, icon);
            }
        }
        graphics.disableScissor();
        int thumb = Math.max(10, (int) (68.0 * 68 / Math.max(68, rules.size() * 22)));
        int y = topPos + 52 + (int) (maximumScroll() == 0 ? 0 : scroll / maximumScroll() * (68 - thumb));
        graphics.fill(leftPos + 357, y, leftPos + 361, y + thumb, MUTED);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.nextStratum();
        graphics.text(font, title, 12, 9, WHITE, true);
        graphics.text(font, Component.translatable("gui.trading_cells.pipe.face", label("direction." + menu.face().getSerializedName())),
                12 + font.width(title) + 16, 9, MUTED, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
        if (editor == null) {
            graphics.text(font, label("priority"), 228, 33, MUTED, false);
            graphics.text(font, label("channel"), 204, 168, MUTED, false);
        }
    }

    @Override
    public void onClose() { if (editor != null) { editor.save(); } save(); super.onClose(); }
    @Override
    public void removed() { if (completion != null) { completion.close(); } save(); super.removed(); }
    private void save() { if (!sent) { sent = true; sendDraft(); } }
    private void sendDraft() {
        storeFields(); var data = draft.save();
        if (data.equals(lastSent)) { return; }
        lastSent = data;
        ClientPacketDistributor.sendToServer(new PipeConfigurationPayload(menu.pos(), menu.face(), menu.hand(), menu.latestRevision(), data));
    }

    private boolean overList(double x, double y) { return x >= leftPos + 46 && x < leftPos + 356 && y >= topPos + 52 && y < topPos + 120; }
    private double maximumScroll() { return Math.max(0, draft.profile(selected).filters().size() * 22 - 68); }
    private Button button(Component text, int x, int y, int width, Button.OnPress press) {
        return addRenderableWidget(Button.builder(text, press).bounds(leftPos + x, topPos + y, width, y == 224 ? 16 : 18).build());
    }
    private Button icon(ItemStack stack, Component text, int x, int y, int size, boolean selected, Button.OnPress press) {
        var button = addRenderableWidget(new PipeEditorIcons.IconButton(leftPos + x, topPos + y, size, stack, text, selected, press));
        button.setTooltip(Tooltip.create(text)); return button;
    }
    private void gate(AbstractWidget widget, boolean enabled, PipeUpgradeTier minimum) {
        widget.active = enabled;
        if (!enabled) { widget.setTooltip(Tooltip.create(label("requires", Component.translatable("item.trading_cells." + minimum.serializedName() + "_pipe_upgrade")))); }
    }
    static Component label(String name, Object... args) { return Component.translatable("gui.trading_cells.pipe." + name, args); }
    static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF727D82); graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF343C40);
    }
}
