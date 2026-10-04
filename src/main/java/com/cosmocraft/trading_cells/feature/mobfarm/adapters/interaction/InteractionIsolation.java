package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import net.minecraft.world.level.Level;

/** Synchronous boundary, not a security manager for arbitrary mod Java code. */
public final class InteractionIsolation implements AutoCloseable {
    private static final ThreadLocal<InteractionIsolation> CURRENT = new ThreadLocal<>();
    private final Level level;
    private boolean rejected;

    private InteractionIsolation(Level level) { this.level = level; }
    public static InteractionIsolation enter(Level level) {
        if (CURRENT.get() != null) { throw new IllegalStateException("Nested entity interaction"); }
        InteractionIsolation scope = new InteractionIsolation(level);
        CURRENT.set(scope);
        return scope;
    }
    public static boolean active() { return CURRENT.get() != null; }
    public static void requireLocal(Level level) {
        InteractionIsolation scope = CURRENT.get();
        if (scope != null && scope.level != level) { deny(); }
    }
    public static void deny() {
        InteractionIsolation scope = CURRENT.get();
        if (scope != null) { scope.rejected = true; }
        throw new UnsupportedOperationException("Interaction requires unavailable world context");
    }
    public boolean rejected() { return rejected; }
    @Override public void close() { CURRENT.remove(); }
}
