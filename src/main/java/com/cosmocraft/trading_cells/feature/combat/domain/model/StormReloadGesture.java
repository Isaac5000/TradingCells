package com.cosmocraft.trading_cells.feature.combat.domain.model;

/** Short release reloads; a held press ejects once and never reloads on release. */
public final class StormReloadGesture {
    public enum Action { NONE, RELOAD, EJECT }
    private boolean down;
    private int ticks;

    public Action setDown(boolean pressed) {
        if (pressed == down) { return Action.NONE; }
        boolean reload = !pressed && ticks < 12;
        down = pressed;
        ticks = 0;
        return reload ? Action.RELOAD : Action.NONE;
    }

    public Action tick() {
        if (!down || ticks >= 12) { return Action.NONE; }
        return ++ticks == 12 ? Action.EJECT : Action.NONE;
    }

    public void reset() { down = false; ticks = 0; }
}
