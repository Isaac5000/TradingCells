import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import java.util.Arrays;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.NbtOps;

/** Local codec comparison; not an estimate of whole-game performance. */
public class LogisticsFingerprintBenchmark {
    private static volatile String sink;
    private static final DataComponentPatch[] INPUT = {DataComponentPatch.EMPTY, DataComponentPatch.builder().build()};

    private static String baseline(DataComponentPatch patch) {
        return DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, patch).getOrThrow().toString();
    }

    private static String candidate(DataComponentPatch patch) {
        return LogisticsComponentData.serialized(patch);
    }

    private static long measure(boolean optimized, int count) {
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            sink = optimized ? candidate(INPUT[i & 1]) : baseline(INPUT[i & 1]);
        }
        return System.nanoTime() - start;
    }

    public static void main(String[] args) {
        for (var patch : INPUT) {
            if (!baseline(patch).equals(candidate(patch)) || !baseline(patch).equals(LogisticsComponentData.fingerprint(patch))) {
                throw new AssertionError("Empty component fingerprint changed");
            }
        }
        for (int i = 0; i < 6; i++) { measure(false, 50_000); measure(true, 50_000); }
        long[] baseline = new long[9], candidate = new long[9];
        System.out.println("round,baseline_ns,candidate_ns");
        for (int i = 0; i < 9; i++) {
            if ((i & 1) == 0) {
                baseline[i] = measure(false, 200_000);
                candidate[i] = measure(true, 200_000);
            } else {
                candidate[i] = measure(true, 200_000);
                baseline[i] = measure(false, 200_000);
            }
            System.out.println(i + "," + baseline[i] + "," + candidate[i]);
        }
        Arrays.sort(baseline); Arrays.sort(candidate);
        System.out.printf(java.util.Locale.ROOT, "median_ms=%.3f -> %.3f; improvement=%.2f%%%n",
                baseline[4] / 1e6, candidate[4] / 1e6, 100.0 * (baseline[4] - candidate[4]) / baseline[4]);
    }
}
