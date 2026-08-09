import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** Standalone randomized equivalence and allocation-oriented microbenchmark. */
public final class OutputInserterEquivalence {
    private static final int SLOT_COUNT = 18;
    private static final int SOURCE_COUNT = 8;
    private static final int TYPES = 7;
    private static final int CASES = 250_000;

    private OutputInserterEquivalence() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        Random random = new Random(0x5EED_262L);
        CaseData[] cases = new CaseData[CASES];
        for (int index = 0; index < cases.length; index++) {
            cases[index] = randomCase(random);
            verify(cases[index], index);
        }

        for (int warmup = 0; warmup < 4; warmup++) {
            runLegacy(cases);
            runOptimized(cases);
        }
        long legacyStart = System.nanoTime();
        long legacyChecksum = runLegacy(cases);
        long legacyNanos = System.nanoTime() - legacyStart;
        long optimizedStart = System.nanoTime();
        long optimizedChecksum = runOptimized(cases);
        long optimizedNanos = System.nanoTime() - optimizedStart;
        if (legacyChecksum != optimizedChecksum) {
            throw new AssertionError("Benchmark checksums differ");
        }

        double improvement = 100.0D * (legacyNanos - optimizedNanos) / legacyNanos;
        System.out.printf("cases,legacy_ms,optimized_ms,improvement_percent%n");
        System.out.printf(
                "%d,%.3f,%.3f,%.2f%n",
                CASES,
                legacyNanos / 1_000_000.0D,
                optimizedNanos / 1_000_000.0D,
                improvement
        );
    }

    private static void verify(CaseData data, int index) {
        int[] legacyTypes = data.slotTypes().clone();
        int[] legacyCounts = data.slotCounts().clone();
        boolean legacyFits = legacyInsertAll(legacyTypes, legacyCounts, data);
        boolean optimizedFits = optimizedFits(data);
        if (legacyFits != optimizedFits) {
            throw new AssertionError("Capacity mismatch in case " + index);
        }
        if (!optimizedFits) {
            return;
        }
        int[] optimizedTypes = data.slotTypes().clone();
        int[] optimizedCounts = data.slotCounts().clone();
        optimizedInsertAll(optimizedTypes, optimizedCounts, data);
        if (!Arrays.equals(legacyTypes, optimizedTypes)
                || !Arrays.equals(legacyCounts, optimizedCounts)) {
            throw new AssertionError("Slot order mismatch in case " + index);
        }
    }

    private static long runLegacy(CaseData[] cases) {
        long checksum = 0L;
        for (CaseData data : cases) {
            int[] types = data.slotTypes().clone();
            int[] counts = data.slotCounts().clone();
            checksum += legacyInsertAll(types, counts, data) ? 1L : 0L;
        }
        return checksum;
    }

    private static long runOptimized(CaseData[] cases) {
        long checksum = 0L;
        for (CaseData data : cases) {
            checksum += optimizedFits(data) ? 1L : 0L;
        }
        return checksum;
    }

    private static boolean optimizedFits(CaseData data) {
        int emptySlots = 0;
        for (int type : data.slotTypes()) {
            if (type < 0) {
                emptySlots++;
            }
        }
        int requiredSlots = 0;
        for (int source = 0; source < SOURCE_COUNT; source++) {
            int type = data.sourceTypes()[source];
            if (type < 0 || appearedEarlier(data.sourceTypes(), source, type)) {
                continue;
            }
            long remaining = 0L;
            for (int candidate = source; candidate < SOURCE_COUNT; candidate++) {
                if (data.sourceTypes()[candidate] == type) {
                    remaining += data.sourceCounts()[candidate];
                }
            }
            for (int slot = 0; slot < SLOT_COUNT && remaining > 0L; slot++) {
                if (data.slotTypes()[slot] == type) {
                    remaining -= Math.max(0, data.maxStacks()[type] - data.slotCounts()[slot]);
                }
            }
            if (remaining > 0L) {
                requiredSlots += (int) ((remaining + data.maxStacks()[type] - 1L) / data.maxStacks()[type]);
                if (requiredSlots > emptySlots) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean appearedEarlier(int[] sources, int end, int type) {
        for (int index = 0; index < end; index++) {
            if (sources[index] == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean legacyInsertAll(int[] slotTypes, int[] slotCounts, CaseData data) {
        for (int source = 0; source < SOURCE_COUNT; source++) {
            int type = data.sourceTypes()[source];
            if (type >= 0 && !insert(slotTypes, slotCounts, type, data.sourceCounts()[source], data.maxStacks()[type])) {
                return false;
            }
        }
        return true;
    }

    private static void optimizedInsertAll(int[] slotTypes, int[] slotCounts, CaseData data) {
        for (int source = 0; source < SOURCE_COUNT; source++) {
            int type = data.sourceTypes()[source];
            if (type >= 0 && !insert(slotTypes, slotCounts, type, data.sourceCounts()[source], data.maxStacks()[type])) {
                throw new AssertionError("Validated output stopped fitting");
            }
        }
    }

    private static boolean insert(int[] types, int[] counts, int type, int count, int maximum) {
        int remaining = count;
        for (int slot = 0; slot < SLOT_COUNT && remaining > 0; slot++) {
            if (types[slot] == type) {
                int moved = Math.min(remaining, Math.max(0, maximum - counts[slot]));
                counts[slot] += moved;
                remaining -= moved;
            }
        }
        for (int slot = 0; slot < SLOT_COUNT && remaining > 0; slot++) {
            if (types[slot] < 0) {
                int moved = Math.min(remaining, maximum);
                types[slot] = type;
                counts[slot] = moved;
                remaining -= moved;
            }
        }
        return remaining == 0;
    }

    private static CaseData randomCase(Random random) {
        int[] maximums = {64, 64, 64, 16, 16, 1, 1};
        int[] slotTypes = new int[SLOT_COUNT];
        int[] slotCounts = new int[SLOT_COUNT];
        Arrays.fill(slotTypes, -1);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (random.nextInt(4) == 0) {
                continue;
            }
            int type = random.nextInt(TYPES);
            slotTypes[slot] = type;
            slotCounts[slot] = 1 + random.nextInt(maximums[type]);
        }
        int[] sourceTypes = new int[SOURCE_COUNT];
        int[] sourceCounts = new int[SOURCE_COUNT];
        Arrays.fill(sourceTypes, -1);
        int sources = random.nextInt(SOURCE_COUNT + 1);
        for (int source = 0; source < sources; source++) {
            int type = random.nextInt(TYPES);
            sourceTypes[source] = type;
            sourceCounts[source] = 1 + random.nextInt(maximums[type] * 3);
        }
        return new CaseData(slotTypes, slotCounts, sourceTypes, sourceCounts, maximums);
    }

    private record CaseData(
            int[] slotTypes,
            int[] slotCounts,
            int[] sourceTypes,
            int[] sourceCounts,
            int[] maxStacks
    ) {
    }
}
