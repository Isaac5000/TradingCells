#!/usr/bin/env python3
"""Compile and run a dependency-free Trading Cells Java microbenchmark."""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
from pathlib import Path
import statistics
import subprocess

from platform_tools import configure_utf8_stdio, find_jdk_tool


@dataclass(frozen=True)
class Benchmark:
    source: str
    value_columns: tuple[str, str, str, str]
    default_runs: int
    minimum_runs: int
    default_improvement: float | None
    output_directory: str


BENCHMARKS = {
    "output-inserter": Benchmark(
        "OutputInserterEquivalence.java",
        ("cases", "legacy_ms", "optimized_ms", "improvement_percent"),
        3,
        1,
        None,
        "output-inserter",
    ),
    "high-level-tooltip": Benchmark(
        "HighLevelTooltipAllocationBenchmark.java",
        ("tooltips", "eager_ms", "lazy_ms", "improvement_percent"),
        5,
        3,
        3.0,
        "high-level-tooltip",
    ),
    "autotrader-readiness": Benchmark(
        "AutotraderReadinessEquivalence.java",
        ("cases", "legacy_ms", "optimized_ms", "improvement_percent"),
        5,
        3,
        10.0,
        "autotrader-readiness",
    ),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("benchmark", choices=tuple(BENCHMARKS))
    parser.add_argument("--output-directory", type=Path)
    parser.add_argument("--runs", type=int)
    parser.add_argument("--minimum-improvement", type=float)
    return parser.parse_args()


def parse_measurement(output: str, columns: tuple[str, ...]) -> dict[str, int | float]:
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    if not lines:
        raise RuntimeError("The benchmark produced no measurement")
    values = next(csv.reader((lines[-1],)))
    if len(values) != len(columns):
        raise RuntimeError(f"Unexpected benchmark output: {lines[-1]!r}")
    result: dict[str, int | float] = {columns[0]: int(values[0])}
    result.update({column: float(value) for column, value in zip(columns[1:], values[1:])})
    return result


def main() -> None:
    args = parse_args()
    benchmark = BENCHMARKS[args.benchmark]
    runs = args.runs if args.runs is not None else benchmark.default_runs
    if not benchmark.minimum_runs <= runs <= 20:
        raise ValueError(f"--runs must be between {benchmark.minimum_runs} and 20")

    tools_directory = Path(__file__).resolve().parent
    project_root = tools_directory.parents[1]
    output = (
        args.output_directory
        or project_root / "build" / "performance" / benchmark.output_directory
    ).resolve()
    classes = output / "classes"
    classes.mkdir(parents=True, exist_ok=True)

    javac = find_jdk_tool("javac")
    java = find_jdk_tool("java")
    assert javac is not None and java is not None
    source = tools_directory / benchmark.source
    subprocess.run([str(javac), "-d", str(classes), str(source)], check=True)

    rows: list[dict[str, int | float]] = []
    for run in range(1, runs + 1):
        completed = subprocess.run(
            [str(java), "-cp", str(classes), source.stem],
            check=True,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        rows.append({"run": run, **parse_measurement(completed.stdout, benchmark.value_columns)})

    results_file = output / "runs.csv"
    with results_file.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

    identity, baseline_name, candidate_name, _ = benchmark.value_columns
    baseline = statistics.median(float(row[baseline_name]) for row in rows)
    candidate = statistics.median(float(row[candidate_name]) for row in rows)
    improvement = 100.0 * (baseline - candidate) / baseline
    print(f"{identity},{baseline_name},{candidate_name},improvement_percent")
    print(f"{rows[0][identity]},{baseline:.3f},{candidate:.3f},{improvement:.2f}")
    print(f"Results: {results_file}")

    minimum = (
        args.minimum_improvement
        if args.minimum_improvement is not None
        else benchmark.default_improvement
    )
    if minimum is not None and improvement < minimum:
        raise RuntimeError(
            f"Measured improvement {improvement:.2f}% is below {minimum:.2f}%."
        )


if __name__ == "__main__":
    configure_utf8_stdio()
    main()
