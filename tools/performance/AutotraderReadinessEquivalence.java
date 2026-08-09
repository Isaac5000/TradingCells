import java.util.Random;

/**
 * Compares the per-tick Autotrader readiness scan with a revision-backed cache.
 * This is dependency-free so it can run without launching Minecraft.
 */
public final class AutotraderReadinessEquivalence {
    private static final int EQUIVALENCE_CASES = 500_000;
    private static final int BENCHMARK_TICKS = 5_000_000;
    private static volatile State benchmarkState;
    private static volatile int sink;

    private AutotraderReadinessEquivalence() {
    }

    public static void main(String[] args) {
        verifyEquivalence();
        State blocked = State.blocked();
        benchmarkState = blocked;
        Cache cache = new Cache();

        for (int warmup = 0; warmup < 3; warmup++) {
            runLegacy(blocked, BENCHMARK_TICKS / 10);
            runCached(blocked, cache, BENCHMARK_TICKS / 10);
        }

        long legacyStart = System.nanoTime();
        int legacy = runLegacy(blocked, BENCHMARK_TICKS);
        long legacyNanos = System.nanoTime() - legacyStart;

        cache.invalidate();
        long cachedStart = System.nanoTime();
        int optimized = runCached(blocked, cache, BENCHMARK_TICKS);
        long cachedNanos = System.nanoTime() - cachedStart;
        if (legacy != optimized) {
            throw new AssertionError("Benchmark paths produced different results");
        }
        sink = legacy + optimized;

        double legacyMs = legacyNanos / 1_000_000.0D;
        double optimizedMs = cachedNanos / 1_000_000.0D;
        double improvement = 100.0D * (legacyMs - optimizedMs) / legacyMs;
        System.out.println("cases,legacy_ms,optimized_ms,improvement_percent");
        System.out.printf(
                java.util.Locale.ROOT,
                "%d,%.3f,%.3f,%.2f%n",
                BENCHMARK_TICKS,
                legacyMs,
                optimizedMs,
                improvement
        );
    }

    private static void verifyEquivalence() {
        Random random = new Random(0x545241444552L);
        State state = State.blocked();
        Cache cache = new Cache();
        long contentsRevision = 0L;
        int offersRevision = 0;
        int selectedOffer = 0;

        for (int index = 0; index < EQUIVALENCE_CASES; index++) {
            int mutation = random.nextInt(12);
            if (mutation < 4) {
                state.inputA[mutation] = random.nextInt(65);
                contentsRevision++;
            } else if (mutation < 8) {
                state.inputB[mutation - 4] = random.nextInt(65);
                contentsRevision++;
            } else if (mutation < 10) {
                state.output[mutation - 8] = random.nextInt(65);
                contentsRevision++;
            } else if (mutation == 10) {
                state.requiredA = 1 + random.nextInt(64);
                state.requiredB = random.nextInt(65);
                state.resultCount = 1 + random.nextInt(64);
                offersRevision++;
            } else {
                selectedOffer = random.nextInt(8);
            }

            boolean expected = legacyReady(state);
            boolean actual = cache.ready(
                    contentsRevision,
                    offersRevision,
                    selectedOffer,
                    state
            );
            if (expected != actual) {
                throw new AssertionError("Readiness mismatch at case " + index);
            }
        }
    }

    private static int runLegacy(State state, int ticks) {
        int ready = 0;
        for (int tick = 0; tick < ticks; tick++) {
            State current = benchmarkState;
            ready += legacyReady(current) ? 1 : 0;
        }
        return ready;
    }

    private static int runCached(State state, Cache cache, int ticks) {
        int ready = 0;
        long revision = 0L;
        for (int tick = 0; tick < ticks; tick++) {
            // The real block persists its exact age counters every 20 ticks.
            if (tick % 20 == 0) {
                revision++;
            }
            State current = benchmarkState;
            ready += cache.ready(revision, 0, 0, current) ? 1 : 0;
        }
        return ready;
    }

    private static boolean legacyReady(State state) {
        return count(state.inputA) >= state.requiredA
                && count(state.inputB) >= state.requiredB
                && outputCapacity(state.output) >= state.resultCount;
    }

    private static int count(int[] slots) {
        int count = 0;
        for (int slot : slots) {
            count += slot;
        }
        return count;
    }

    private static int outputCapacity(int[] slots) {
        int capacity = 0;
        for (int slot : slots) {
            capacity += slot == 0 ? 64 : 64 - slot;
        }
        return capacity;
    }

    private static final class Cache {
        private long contentsRevision = Long.MIN_VALUE;
        private int offersRevision = Integer.MIN_VALUE;
        private int selectedOffer = Integer.MIN_VALUE;
        private boolean ready;

        private boolean ready(
                long nextContentsRevision,
                int nextOffersRevision,
                int nextSelectedOffer,
                State state
        ) {
            if (contentsRevision != nextContentsRevision
                    || offersRevision != nextOffersRevision
                    || selectedOffer != nextSelectedOffer) {
                ready = legacyReady(state);
                contentsRevision = nextContentsRevision;
                offersRevision = nextOffersRevision;
                selectedOffer = nextSelectedOffer;
            }
            return ready;
        }

        private void invalidate() {
            contentsRevision = Long.MIN_VALUE;
            offersRevision = Integer.MIN_VALUE;
            selectedOffer = Integer.MIN_VALUE;
        }
    }

    private static final class State {
        private final int[] inputA = new int[4];
        private final int[] inputB = new int[4];
        private final int[] output = new int[4];
        private int requiredA;
        private int requiredB;
        private int resultCount;

        private static State blocked() {
            State state = new State();
            state.inputA[0] = 1;
            state.requiredA = 64;
            state.requiredB = 0;
            state.resultCount = 1;
            return state;
        }
    }
}
