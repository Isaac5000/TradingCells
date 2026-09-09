package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.client.PipeConfigurationScreen;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/** Native inventory gestures and keyboard regression checks, never packaged with the mod. */
final class PipeInteractionUiFixture {
    private static int phase;
    private static long nextAction, started;

    private PipeInteractionUiFixture() { }

    static boolean ready() { return phase >= 12; }

    static void inspect(PipeConfigurationScreen screen) {
        if (ready() || System.nanoTime() < nextAction) { return; }
        if (started == 0) { started = System.nanoTime(); }
        require(System.nanoTime() - started < 40_000_000_000L, "Interaction timeout at phase " + phase);
        var menu = screen.getMenu();
        int left = screen.getLeftPos(), top = screen.getTopPos();
        switch (phase) {
            case 0 -> {
                click(screen, 25, 64, 0);
                require(modeActive(screen), "Empty upgrade click disabled the editor");
            }
            case 1 -> {
                click(screen, 20, 168, 0);
                require(menu.getCarried().getCount() == 64, "Upgrade stack pickup");
            }
            case 2 -> {
                var down = new MouseButtonEvent(left + 24, top + 63, new MouseButtonInfo(0, 0));
                var moved = new MouseButtonEvent(left + 25, top + 64, new MouseButtonInfo(0, 0));
                screen.mouseClicked(down, false);
                screen.mouseDragged(moved, 1, 1);
                screen.mouseReleased(moved);
                require(menu.getSlot(0).getItem().getCount() == 1 && menu.getCarried().getCount() == 63,
                        "Three-packet drag must install exactly one upgrade");
            }
            case 3 -> {
                if (menu.latest().upgradeTier() != PipeUpgradeTier.INFINITE || !modeActive(screen)) { return; }
                click(screen, 25, 64, 0);
                require(modeActive(screen), "Full upgrade slot no-op disabled the editor");
                require(menu.getSlot(0).getItem().getCount() == 1 && menu.getCarried().getCount() == 63,
                        "Full slot must preserve both upgrade stacks");
            }
            case 4 -> click(screen, 20, 168, 0);
            case 5 -> {
                click(screen, 25, 64, 0);
                click(screen, 74, 168, 0);
                require(menu.getCarried().isEmpty(), "Upgrade acknowledgement must not block the next inventory click");
            }
            case 6 -> {
                if (menu.latest().upgradeTier() != PipeUpgradeTier.BARE || !modeActive(screen)) { return; }
                click(screen, 20, 168, 1);
            }
            case 7 -> {
                if (menu.latest().upgradeTier() != PipeUpgradeTier.INFINITE || !modeActive(screen)) { return; }
                int count = menu.slots.stream().mapToInt(slot -> slot.getItem().is(
                        LogisticsRegistrationAdapter.upgradeItem(PipeUpgradeTier.INFINITE).get()) ? slot.getItem().getCount() : 0).sum();
                require(count == 64, "Upgrade conservation after drag, pickup and shift click");
                for (var child : screen.children()) {
                    if (child instanceof EditBox field) { checkTyping(screen, field); }
                }
                field(screen, 28).setValue("777");
                field(screen, 185).setValue("metales");
                screen.keyPressed(new KeyEvent(256, 0, 0));
                require(Minecraft.getInstance().gui.screen() != screen, "Escape must still close a focused field");
                System.out.println("Pipe upgrade drag, full/empty no-op, shift click and inventory conservation verified");
            }
            case 8 -> {
                require(menu.initial().priority() == 777 && menu.initial().profile(
                        com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType.ITEM).channel().equals("metales"),
                        "Typed values must persist after Escape");
                click(screen, 25, 118, 0);
                click(screen, 223, 133, 0);
            }
            case 9 -> {
                for (var child : screen.children()) {
                    if (child instanceof EditBox field) { checkTyping(screen, field); }
                }
                field(screen, 36).setValue("minecraft:iron_ingot");
                field(screen, 74).setValue("{\"minecraft:custom_data\":{name:\"e1qf\"}}");
                field(screen, 112).setValue("hierro");
                click(screen, 320, 238, 0);
                screen.onClose();
            }
            case 10 -> {
                var rules = menu.initial().profile(com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType.ITEM).filters();
                require(rules.size() == 1 && rules.getFirst().routeChannel().equals("metales/hierro"), "Rule fields persist after typing shortcuts");
                System.out.println("Pipe main/rule text fields consume inventory, drop and hotbar keys; Escape still closes");
            }
            case 11 -> require(modeActive(screen), "Editor remains responsive after reopening");
            default -> { }
        }
        phase++;
        nextAction = System.nanoTime() + 550_000_000L;
    }

    static void checkTyping(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, EditBox field) {
        String before = field.getValue();
        click(screen, field.getX() - screen.getLeftPos() + 4, field.getY() - screen.getTopPos() + 4, 0);
        field.setValue("");
        for (char character : "e1qf".toCharArray()) {
            screen.keyPressed(new KeyEvent(Character.toUpperCase(character), 0, 0));
            require(Minecraft.getInstance().gui.screen() == screen, "Text key closed screen: " + character);
            screen.charTyped(new CharacterEvent(character));
        }
        require(field.getValue().equals("e1qf"), "Text input must reach its focused field");
        screen.keyPressed(new KeyEvent(65, 0, 2));
        screen.keyPressed(new KeyEvent(259, 0, 0));
        require(field.getValue().isEmpty(), "Ctrl+A and Backspace must still edit text");
        field.setValue(before);
    }

    private static EditBox field(PipeConfigurationScreen screen, int y) {
        return screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
                .filter(field -> field.getY() == screen.getTopPos() + y).findFirst().orElseThrow();
    }

    private static boolean modeActive(PipeConfigurationScreen screen) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getX() == screen.getLeftPos() + 12 && button.getY() == screen.getTopPos() + 28)
                .findFirst().orElseThrow().active;
    }

    private static void click(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, int x, int y, int modifiers) {
        var event = new MouseButtonEvent(screen.getLeftPos() + x, screen.getTopPos() + y, new MouseButtonInfo(0, modifiers));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
