#!/usr/bin/env python3
"""Compare client frame/JFR results and deterministic captures."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


LOWER_IS_BETTER = (
    "median_mean_frame_ms",
    "median_p50_frame_ms",
    "median_p95_frame_ms",
    "median_p99_frame_ms",
    "median_jvm_cpu_percent",
    "median_allocation_bytes_per_second",
    "median_resident_bytes",
    "median_packets_sent",
    "median_packet_bytes_sent",
)
HIGHER_IS_BETTER = ("median_frames_per_second",)
PRIMARY_METRICS = ("median_mean_frame_ms", "median_p95_frame_ms")
METADATA_KEYS = (
    "minecraft_version",
    "neo_version",
    "backend_requested",
    "scenario",
    "warmup_seconds",
    "measure_seconds",
    "resolution",
    "with_rei",
    "with_trading_cells",
    "quick_play_world",
    "camera",
    "template_directory",
)


def read_single_csv(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if len(rows) != 1:
        raise ValueError(f"Expected exactly one row in {path}")
    return rows[0]


def validate_metadata(
    baseline: Path,
    candidate: Path,
    allow_mod_presence_difference: bool,
) -> None:
    before = json.loads((baseline / "metadata.json").read_text(encoding="utf-8"))
    after = json.loads((candidate / "metadata.json").read_text(encoding="utf-8"))
    differences = [
        key
        for key in METADATA_KEYS
        if before.get(key) != after.get(key)
        and not (allow_mod_presence_difference and key == "with_trading_cells")
    ]
    if differences:
        details = ", ".join(
            f"{key}={before.get(key)!r}/{after.get(key)!r}" for key in differences
        )
        raise ValueError(f"Benchmark metadata differs: {details}")


def compare_images(
    baseline: Path,
    candidate: Path,
    maximum_changed_pixels: int,
    maximum_channel_delta: int,
) -> bool:
    try:
        from PIL import Image, ImageChops
    except ImportError as exception:
        raise RuntimeError("Pillow is required for screenshot comparison") from exception

    before_runs = list(csv.DictReader((baseline / "runs.csv").open(encoding="utf-8", newline="")))
    after_runs = list(csv.DictReader((candidate / "runs.csv").open(encoding="utf-8", newline="")))
    if len(before_runs) != len(after_runs):
        raise ValueError("Baseline and candidate have a different number of client runs")
    accepted = True
    print("image_run,changed_pixels,max_channel_delta,status")
    for index, (before_row, after_row) in enumerate(zip(before_runs, after_runs), start=1):
        before_name = before_row.get("screenshot_file", "")
        after_name = after_row.get("screenshot_file", "")
        if not before_name or not after_name:
            raise ValueError(f"Missing screenshot for run {index}")
        with Image.open(baseline / before_name).convert("RGBA") as before_image:
            with Image.open(candidate / after_name).convert("RGBA") as after_image:
                if before_image.size != after_image.size:
                    raise ValueError(
                        f"Screenshot dimensions differ in run {index}: "
                        f"{before_image.size}/{after_image.size}"
                    )
                difference = ImageChops.difference(before_image, after_image)
                pixels = list(difference.getdata())
                changed = sum(1 for pixel in pixels if max(pixel) > maximum_channel_delta)
                largest_delta = max((max(pixel) for pixel in pixels), default=0)
        run_accepted = changed <= maximum_changed_pixels
        accepted &= run_accepted
        print(
            f"{index},{changed},{largest_delta},"
            f"{'identical' if run_accepted else 'different'}"
        )
    return accepted


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("--risk", choices=("local", "structural"), default="structural")
    parser.add_argument("--minimum-improvement", type=float)
    parser.add_argument("--maximum-regression", type=float, default=1.0)
    parser.add_argument("--maximum-changed-pixels", type=int, default=0)
    parser.add_argument("--maximum-channel-delta", type=int, default=0)
    parser.add_argument("--skip-images", action="store_true")
    parser.add_argument(
        "--stability-only",
        action="store_true",
        help="Accept unchanged baselines without requiring an improvement.",
    )
    parser.add_argument(
        "--allow-mod-presence-difference",
        action="store_true",
        help="Permit the vanilla control to compare Trading Cells enabled and disabled.",
    )
    args = parser.parse_args()
    minimum = args.minimum_improvement
    if minimum is None:
        minimum = 3.0 if args.risk == "local" else 10.0

    baseline = args.baseline.resolve()
    candidate = args.candidate.resolve()
    validate_metadata(baseline, candidate, args.allow_mod_presence_difference)
    before = read_single_csv(baseline / "summary.csv")
    after = read_single_csv(candidate / "summary.csv")

    accepted_route = False
    regressed = False
    print("metric,baseline,candidate,improvement_percent,status")
    for metric in LOWER_IS_BETTER + HIGHER_IS_BETTER:
        if metric not in before or metric not in after:
            continue
        old = float(before[metric])
        new = float(after[metric])
        if old == 0.0:
            improvement = 0.0 if new == 0.0 else float("-inf")
        elif metric in HIGHER_IS_BETTER:
            improvement = 100.0 * (new - old) / old
        else:
            improvement = 100.0 * (old - new) / old
        accepted_route |= metric in PRIMARY_METRICS and improvement >= minimum
        metric_regressed = metric in PRIMARY_METRICS and improvement < -args.maximum_regression
        regressed |= metric_regressed
        status = "improved" if improvement >= minimum else "stable"
        if metric_regressed:
            status = "regressed"
        print(f"{metric},{old:.6f},{new:.6f},{improvement:.2f},{status}")

    images_accepted = args.skip_images or compare_images(
        baseline,
        candidate,
        args.maximum_changed_pixels,
        args.maximum_channel_delta,
    )
    accepted = (accepted_route or args.stability_only) and not regressed and images_accepted
    print(f"acceptance,{'accepted' if accepted else 'rejected'}")
    raise SystemExit(0 if accepted else 2)


if __name__ == "__main__":
    main()
