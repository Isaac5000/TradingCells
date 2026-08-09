import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Compares eager and lazy replacement maps for high-level enchantment tooltips. */
public final class HighLevelTooltipAllocationBenchmark {
    private static final int EQUIVALENCE_CASES = 250_000;
    private static final int BENCHMARK_TOOLTIPS = 2_000_000;
    private static final int[] NORMAL_LEVELS = {1, 3, 5};
    private static final int[] MAX_LEVELS = {1, 3, 5};
    private static volatile Map<Integer, Integer> sink;

    private HighLevelTooltipAllocationBenchmark() {
    }

    public static void main(String[] args) {
        verifyEquivalence();
        for (int warmup = 0; warmup < 3; warmup++) {
            runEager(BENCHMARK_TOOLTIPS / 10);
            runLazy(BENCHMARK_TOOLTIPS / 10);
        }

        long eagerStart = System.nanoTime();
        runEager(BENCHMARK_TOOLTIPS);
        long eagerNanos = System.nanoTime() - eagerStart;
        long lazyStart = System.nanoTime();
        runLazy(BENCHMARK_TOOLTIPS);
        long lazyNanos = System.nanoTime() - lazyStart;

        double eagerMs = eagerNanos / 1_000_000.0D;
        double lazyMs = lazyNanos / 1_000_000.0D;
        double improvement = 100.0D * (eagerMs - lazyMs) / eagerMs;
        System.out.println("tooltips,eager_ms,lazy_ms,improvement_percent");
        System.out.printf(
                java.util.Locale.ROOT,
                "%d,%.3f,%.3f,%.2f%n",
                BENCHMARK_TOOLTIPS,
                eagerMs,
                lazyMs,
                improvement
        );
    }

    private static void verifyEquivalence() {
        Random random = new Random(0x544F4F4C544950L);
        for (int index = 0; index < EQUIVALENCE_CASES; index++) {
            int length = random.nextInt(8);
            int[] levels = new int[length];
            int[] maximums = new int[length];
            for (int entry = 0; entry < length; entry++) {
                levels[entry] = random.nextInt(256);
                maximums[entry] = 1 + random.nextInt(10);
            }
            Map<Integer, Integer> expected = eager(levels, maximums);
            Map<Integer, Integer> actual = lazy(levels, maximums);
            if (!expected.equals(actual)) {
                throw new AssertionError("Tooltip replacements differ at case " + index);
            }
        }
    }

    private static void runEager(int iterations) {
        for (int index = 0; index < iterations; index++) {
            sink = eager(NORMAL_LEVELS, MAX_LEVELS);
        }
    }

    private static void runLazy(int iterations) {
        for (int index = 0; index < iterations; index++) {
            sink = lazy(NORMAL_LEVELS, MAX_LEVELS);
        }
    }

    private static Map<Integer, Integer> eager(int[] levels, int[] maximums) {
        Map<Integer, Integer> replacements = new HashMap<>();
        collect(levels, maximums, replacements);
        return replacements;
    }

    private static Map<Integer, Integer> lazy(int[] levels, int[] maximums) {
        Map<Integer, Integer> replacements = Map.of();
        for (int index = 0; index < levels.length; index++) {
            if (levels[index] <= maximums[index]) {
                continue;
            }
            if (replacements.isEmpty()) {
                replacements = new HashMap<>();
            }
            replacements.put(index, levels[index]);
        }
        return replacements;
    }

    private static void collect(int[] levels, int[] maximums, Map<Integer, Integer> replacements) {
        for (int index = 0; index < levels.length; index++) {
            if (levels[index] > maximums[index]) {
                replacements.put(index, levels[index]);
            }
        }
    }
}
