#!/usr/bin/env python3
"""Run repeatable Trading Cells frame-time measurements on OpenGL or Vulkan."""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import os
from pathlib import Path
import platform
import shutil
import statistics
import subprocess
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend", choices=("opengl", "vulkan"), required=True)
    parser.add_argument("--scenario", default="vanilla-control")
    parser.add_argument("--runs", type=int, default=3)
    parser.add_argument("--warmup-seconds", type=float, default=15.0)
    parser.add_argument("--measure-seconds", type=float, default=30.0)
    parser.add_argument("--width", type=int, default=1920)
    parser.add_argument("--height", type=int, default=1080)
    parser.add_argument(
        "--camera",
        type=float,
        nargs=5,
        metavar=("X", "Y", "Z", "YAW", "PITCH"),
    )
    parser.add_argument("--template-directory", type=Path)
    parser.add_argument("--quick-play-world")
    parser.add_argument("--output-directory", type=Path)
    parser.add_argument("--without-rei", action="store_true")
    parser.add_argument("--without-trading-cells", action="store_true")
    return parser.parse_args()


def clone_template(template: Path | None, destination: Path) -> None:
    if template is None:
        destination.mkdir(parents=True)
        return
    shutil.copytree(
        template,
        destination,
        ignore=shutil.ignore_patterns("logs", "crash-reports", "screenshots"),
    )
    server_world = destination / "world"
    client_world = destination / "saves" / "world"
    if (server_world.joinpath("level.dat").is_file() and not client_world.exists()):
        client_world.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(server_world, client_world)


def normalize_options(game_directory: Path) -> None:
    options_file = game_directory / "options.txt"
    values: dict[str, str] = {}
    order: list[str] = []
    if options_file.is_file():
        for line in options_file.read_text(encoding="utf-8").splitlines():
            if ":" not in line:
                continue
            key, value = line.split(":", 1)
            values[key] = value
            order.append(key)
    fixed = {
        "bobView": "false",
        "enableVsync": "false",
        "fullscreen": "false",
        "guiScale": "2",
        "inactivityFpsLimit": '"minimized"',
        "maxFps": "260",
        "pauseOnLostFocus": "false",
        "renderClouds": '"false"',
        "renderDistance": "8",
        "simulationDistance": "5",
        "tutorialStep": "none",
    }
    for key, value in fixed.items():
        if key not in values:
            order.append(key)
        values[key] = value
    options_file.write_text(
        "\n".join(f"{key}:{values[key]}" for key in order) + "\n",
        encoding="utf-8",
    )


def read_summary(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if len(rows) != 1:
        raise RuntimeError(f"Expected one frame summary in {path}")
    return rows[0]


def verify_backend(row: dict[str, str], backend: str) -> None:
    effective = row["backend"].lower()
    if backend not in effective:
        raise RuntimeError(
            f"Requested {backend}, but Minecraft reported {row['backend']!r}. "
            "The run may have fallen back to another graphics backend."
        )


def find_jfr_tool() -> Path | None:
    java_home = os.environ.get("JAVA_HOME")
    candidates: list[Path] = []
    if java_home:
        candidates.append(Path(java_home) / "bin" / "jfr.exe")
    command = shutil.which("jfr") or shutil.which("jfr.exe")
    if command:
        candidates.append(Path(command))
    gradle_jdks = Path.home() / ".gradle" / "jdks"
    if gradle_jdks.is_dir():
        candidates.extend(gradle_jdks.glob("**/bin/jfr.exe"))
    return next((candidate for candidate in candidates if candidate.is_file()), None)


def jfr_metrics(recording: Path, measured_seconds: float) -> dict[str, str]:
    tool = find_jfr_tool()
    if tool is None or not recording.is_file():
        return {}
    event_names = ",".join(
        (
            "jdk.CPULoad",
            "jdk.ThreadAllocationStatistics",
            "jdk.ResidentSetSize",
            "minecraft.NetworkSummary",
        )
    )
    raw = subprocess.check_output(
        [str(tool), "print", "--json", "--events", event_names, str(recording)],
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=90,
    )
    events: list[dict[str, Any]] = json.loads(raw).get("recording", {}).get("events", [])
    cpu: list[float] = []
    resident: list[int] = []
    allocation_by_thread: dict[int, list[int]] = {}
    sent_bytes = sent_packets = received_bytes = received_packets = 0
    for event in events:
        event_type = event.get("type", "")
        values = event.get("values", {})
        if event_type == "jdk.CPULoad":
            cpu.append(100.0 * (float(values.get("jvmUser", 0.0)) + float(values.get("jvmSystem", 0.0))))
        elif event_type == "jdk.ResidentSetSize":
            resident.append(int(values.get("size", 0)))
        elif event_type == "jdk.ThreadAllocationStatistics":
            thread_id = int(values.get("thread", {}).get("javaThreadId", -1))
            allocation_by_thread.setdefault(thread_id, []).append(int(values.get("allocated", 0)))
        elif event_type == "minecraft.NetworkSummary":
            sent_bytes += int(values.get("sentBytes", 0))
            sent_packets += int(values.get("sentPackets", 0))
            received_bytes += int(values.get("receivedBytes", 0))
            received_packets += int(values.get("receivedPackets", 0))
    allocated = sum(max(values) - min(values) for values in allocation_by_thread.values() if values)
    duration = max(measured_seconds, 0.001)
    return {
        "jvm_cpu_percent": f"{statistics.median(cpu) if cpu else 0.0:.6f}",
        "allocation_bytes_per_second": f"{allocated / duration:.3f}",
        "resident_bytes": str(int(statistics.median(resident)) if resident else 0),
        "packets_sent": str(sent_packets),
        "packet_bytes_sent": str(sent_bytes),
        "packets_received": str(received_packets),
        "packet_bytes_received": str(received_bytes),
    }


def run_once(
    root: Path,
    output: Path,
    run_number: int,
    args: argparse.Namespace,
) -> dict[str, str]:
    run_root = output / f"run-{run_number}"
    game_directory = run_root / "game"
    result_directory = run_root / "result"
    run_root.mkdir(parents=True)
    clone_template(args.template_directory, game_directory)
    normalize_options(game_directory)
    result_directory.mkdir(parents=True)
    jfr = run_root / "client.jfr"
    task = (
        "runPerformanceClientOpenGL"
        if args.backend == "opengl"
        else "runPerformanceClientVulkan"
    )
    command = [
        str(root / "gradlew.bat"),
        f"-PperformanceClientRunDirectory={game_directory}",
        f"-PperformanceClientOutput={result_directory}",
        f"-PperformanceClientWarmup={args.warmup_seconds}",
        f"-PperformanceClientMeasure={args.measure_seconds}",
        f"-PperformanceClientScenario={args.scenario}",
        f"-PperformanceClientWidth={args.width}",
        f"-PperformanceClientHeight={args.height}",
        f"-PperformanceClientJfr={jfr}",
    ]
    if args.camera:
        command.append(
            "-PperformanceClientCamera=" + ",".join(str(value) for value in args.camera)
        )
    if args.quick_play_world:
        command.append(f"-PquickPlayWorld={args.quick_play_world}")
    if args.without_rei:
        command.append("-PwithoutRei")
    if args.without_trading_cells:
        command.append("-PperformanceExcludeTradingCells")
    command.extend((task, "--console=plain"))
    completed = subprocess.run(
        command,
        cwd=root,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=args.warmup_seconds + args.measure_seconds + 240.0,
    )
    (run_root / "client.log").write_text(completed.stdout, encoding="utf-8")
    if completed.returncode != 0:
        raise RuntimeError(
            f"Client run {run_number} failed with code {completed.returncode}; "
            f"see {run_root / 'client.log'}"
        )
    summary_file = result_directory / "summary.csv"
    if not summary_file.is_file():
        raise RuntimeError(
            "The development recorder did not produce summary.csv; "
            f"see {run_root / 'client.log'}"
        )
    row = read_summary(summary_file)
    verify_backend(row, args.backend)
    row.update(jfr_metrics(jfr, float(row["measured_seconds"])))
    row["run"] = str(run_number)
    row["jfr_file"] = str(jfr.relative_to(output)) if jfr.is_file() else ""
    screenshots = list((game_directory / "screenshots").glob("*.png"))
    if screenshots:
        screenshot = max(screenshots, key=lambda path: path.stat().st_mtime_ns)
        target = result_directory / "capture.png"
        shutil.copy2(screenshot, target)
        row["screenshot_file"] = str(target.relative_to(output))
    else:
        row["screenshot_file"] = ""
    return row


def write_results(output: Path, rows: list[dict[str, str]], args: argparse.Namespace) -> None:
    fields = ["run"] + [field for field in rows[0] if field != "run"]
    with (output / "runs.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    numeric = (
        "mean_frame_ms",
        "p50_frame_ms",
        "p95_frame_ms",
        "p99_frame_ms",
        "frames_per_second",
        "jvm_cpu_percent",
        "allocation_bytes_per_second",
        "resident_bytes",
        "packets_sent",
        "packet_bytes_sent",
        "packets_received",
        "packet_bytes_received",
    )
    summary: dict[str, str | int | float] = {
        "scenario": args.scenario,
        "backend": args.backend,
        "runs": len(rows),
        "with_rei": str(not args.without_rei).lower(),
        "with_trading_cells": str(not args.without_trading_cells).lower(),
    }
    for metric in numeric:
        summary[f"median_{metric}"] = round(
            statistics.median(float(row[metric]) for row in rows), 6
        )
    with (output / "summary.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(summary))
        writer.writeheader()
        writer.writerow(summary)


def read_gradle_properties(root: Path) -> dict[str, str]:
    result = {}
    for raw_line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def graphics_adapters() -> list[str]:
    if os.name != "nt":
        return []
    try:
        output = subprocess.check_output(
            [
                "powershell",
                "-NoProfile",
                "-Command",
                "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name",
            ],
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=15,
        )
        return [line.strip() for line in output.splitlines() if line.strip()]
    except (OSError, subprocess.SubprocessError):
        return []


def write_metadata(root: Path, output: Path, args: argparse.Namespace) -> None:
    properties = read_gradle_properties(root)
    metadata = {
        "created_utc": dt.datetime.now(dt.UTC).isoformat(),
        "git_commit": subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=root, text=True, encoding="utf-8"
        ).strip(),
        "git_dirty": bool(
            subprocess.check_output(
                ["git", "status", "--porcelain"], cwd=root, text=True, encoding="utf-8"
            ).strip()
        ),
        "minecraft_version": properties.get("minecraft_version", ""),
        "neo_version": properties.get("neo_version", ""),
        "backend_requested": args.backend,
        "scenario": args.scenario,
        "runs": args.runs,
        "warmup_seconds": args.warmup_seconds,
        "measure_seconds": args.measure_seconds,
        "resolution": f"{args.width}x{args.height}",
        "with_rei": not args.without_rei,
        "with_trading_cells": not args.without_trading_cells,
        "quick_play_world": args.quick_play_world or "",
        "camera": args.camera or [],
        "template_directory": str(args.template_directory or ""),
        "operating_system": platform.platform(),
        "processor": platform.processor(),
        "graphics_adapters": graphics_adapters(),
    }
    (output / "metadata.json").write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def main() -> None:
    args = parse_args()
    if args.runs < 1:
        raise ValueError("--runs must be at least one")
    if args.warmup_seconds < 0.0 or args.measure_seconds <= 0.0:
        raise ValueError("Warmup and measurement durations are invalid")
    if args.width < 320 or args.height < 240:
        raise ValueError("The client resolution is too small")
    if args.template_directory:
        args.template_directory = args.template_directory.resolve()
        if not args.template_directory.is_dir():
            raise ValueError(f"Template directory does not exist: {args.template_directory}")

    root = Path(__file__).resolve().parents[2]
    timestamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
    output = (
        args.output_directory
        or root / "build" / "performance" / "client" / args.scenario / args.backend / timestamp
    ).resolve()
    output.mkdir(parents=True, exist_ok=False)
    write_metadata(root, output, args)
    rows = [run_once(root, output, run, args) for run in range(1, args.runs + 1)]
    write_results(output, rows, args)
    print(f"Results: {output}")


if __name__ == "__main__":
    main()
