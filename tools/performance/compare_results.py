#!/usr/bin/env python3
"""Compare two Trading Cells benchmark result sets using the acceptance gates."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path
import statistics


LOWER_IS_BETTER = (
    "mean_mspt",
    "p95_mspt",
    "jvm_cpu_percent",
    "allocation_bytes_per_second",
    "packets_sent",
    "packet_bytes_sent",
    "chunks_written",
)
PRIMARY_METRICS = ("mean_mspt", "p95_mspt")


def read_medians(path: Path) -> dict[str, float]:
    runs_file = path / "runs.csv" if path.is_dir() else path
    with runs_file.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise ValueError(f"No hay mediciones en {runs_file}")
    return {
        metric: statistics.median(float(row[metric]) for row in rows)
        for metric in LOWER_IS_BETTER
        if metric in rows[0]
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument(
        "--risk",
        choices=("local", "structural"),
        default="structural",
        help="Local changes require 3%%; shared/structural changes require 10%%.",
    )
    parser.add_argument("--minimum-improvement", type=float)
    parser.add_argument("--maximum-regression", type=float, default=1.0)
    args = parser.parse_args()
    minimum_improvement = (
        args.minimum_improvement
        if args.minimum_improvement is not None
        else (3.0 if args.risk == "local" else 10.0)
    )

    baseline = read_medians(args.baseline)
    candidate = read_medians(args.candidate)
    print("metric,baseline,candidate,improvement_percent,status")
    accepted_route = False
    regressed = False
    for metric in sorted(baseline.keys() & candidate.keys()):
        before = baseline[metric]
        after = candidate[metric]
        if before == 0.0:
            improvement = 0.0 if after == 0.0 else float("-inf")
        else:
            improvement = 100.0 * (before - after) / before
        accepted_route |= metric in PRIMARY_METRICS and improvement >= minimum_improvement
        if metric == "jvm_cpu_percent":
            metric_regressed = after - before > args.maximum_regression
        else:
            metric_regressed = improvement < -args.maximum_regression
        regressed |= metric in PRIMARY_METRICS and metric_regressed
        status = "improved" if improvement >= minimum_improvement else "stable"
        if metric_regressed:
            status = "regressed"
        print(f"{metric},{before:.6f},{after:.6f},{improvement:.2f},{status}")

    accepted = accepted_route and not regressed
    print(f"acceptance,{'accepted' if accepted else 'rejected'}")
    raise SystemExit(0 if accepted else 2)


if __name__ == "__main__":
    main()
