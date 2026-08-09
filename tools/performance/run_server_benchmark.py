#!/usr/bin/env python3
"""Run reproducible warmed Minecraft server measurements for Trading Cells."""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import os
from pathlib import Path
import platform
import queue
import re
import shutil
import socket
import statistics
import struct
import subprocess
import threading
import time


READY_PATTERN = re.compile(r"Done \([^)]+\)! For help")
PROFILE_PATTERN = re.compile(
    r"Stopped (?:tick )?profiling after ([0-9.,]+) second(?:\(s\)|s)? "
    r"and ([0-9]+) tick(?:\(s\)|s)?"
)
COMMAND_ERRORS = (
    "Unknown or incomplete command",
    "An unexpected error occurred trying to execute that command",
)
RCON_PASSWORD = "trading-cells-local-benchmark"


class ServerProcess:
    def __init__(self, root: Path, game_directory: Path, log_file: Path) -> None:
        command = [
            str(root / "gradlew.bat"),
            f"-PperformanceRunDirectory={game_directory}",
            "runPerformanceServer",
            "--console=plain",
        ]
        self._lines: list[str] = []
        self._queue: queue.Queue[str] = queue.Queue()
        self._log = log_file.open("w", encoding="utf-8", newline="\n")
        startup = subprocess.STARTUPINFO()
        startup.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        self.process = subprocess.Popen(
            command,
            cwd=root,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
            startupinfo=startup,
        )
        self._reader = threading.Thread(target=self._read_output, daemon=True)
        self._reader.start()

    @property
    def lines(self) -> tuple[str, ...]:
        return tuple(self._lines)

    def _read_output(self) -> None:
        assert self.process.stdout is not None
        for raw_line in self.process.stdout:
            line = raw_line.rstrip("\r\n")
            self._lines.append(line)
            self._queue.put(line)
            self._log.write(line + "\n")
            self._log.flush()

    def command(self, value: str) -> None:
        if self.process.poll() is not None:
            raise RuntimeError(f"El servidor ya terminó con código {self.process.returncode}")
        assert self.process.stdin is not None
        self.process.stdin.write(value + "\n")
        self.process.stdin.flush()

    def wait_for(self, pattern: re.Pattern[str], timeout: float) -> str:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self.process.poll() is not None and self._queue.empty():
                raise RuntimeError(
                    f"El servidor terminó antes de encontrar {pattern.pattern!r} "
                    f"(código {self.process.returncode})"
                )
            try:
                line = self._queue.get(timeout=min(0.25, max(0.01, deadline - time.monotonic())))
            except queue.Empty:
                continue
            if pattern.search(line):
                return line
        raise TimeoutError(f"El servidor no produjo {pattern.pattern!r} en {timeout:.0f} s")

    def stop(self, rcon_port: int, timeout: float = 90.0) -> None:
        if self.process.poll() is None:
            try:
                rcon_command(rcon_port, "stop", allow_disconnect=True)
            except OSError:
                pass
            try:
                self.process.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                self.process.terminate()
                try:
                    self.process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    self.process.kill()
                    self.process.wait(timeout=15)
        self._reader.join(timeout=5)
        self._log.close()

    def abort(self, rcon_port: int | None = None) -> None:
        if self.process.poll() is None:
            if rcon_port is not None:
                try:
                    rcon_command(rcon_port, "stop", allow_disconnect=True)
                    self.process.wait(timeout=30)
                except (OSError, subprocess.TimeoutExpired):
                    pass
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=15)
        self._reader.join(timeout=5)
        self._log.close()


def write_server_files(game_directory: Path, rcon_port: int) -> None:
    game_directory.mkdir(parents=True, exist_ok=True)
    (game_directory / "eula.txt").write_text("eula=true\n", encoding="ascii")
    properties = {
        "difficulty": "peaceful",
        "enable-command-block": "true",
        "enable-rcon": "true",
        "generate-structures": "false",
        "level-name": "world",
        "level-seed": "2620262",
        "level-type": "minecraft:flat",
        "max-tick-time": "-1",
        "motd": "Trading Cells performance harness",
        "online-mode": "false",
        "pause-when-empty-seconds": "0",
        "rcon.password": RCON_PASSWORD,
        "rcon.port": str(rcon_port),
        "server-port": "0",
        "simulation-distance": "4",
        "spawn-protection": "0",
        "view-distance": "4",
    }
    content = "\n".join(f"{key}={value}" for key, value in sorted(properties.items())) + "\n"
    (game_directory / "server.properties").write_text(content, encoding="ascii")


def read_workload(path: Path) -> list[str]:
    commands = []
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line and not line.startswith("#"):
            commands.append(line.removeprefix("/"))
    if not commands:
        raise ValueError(f"La carga {path} no contiene comandos")
    return commands


def wait_seconds(seconds: float) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        time.sleep(min(0.25, deadline - time.monotonic()))


def free_tcp_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        probe.bind(("127.0.0.1", 0))
        return int(probe.getsockname()[1])


def receive_exact(connection: socket.socket, size: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < size:
        chunk = connection.recv(size - len(chunks))
        if not chunk:
            raise ConnectionError("RCON cerró la conexión antes de completar la respuesta")
        chunks.extend(chunk)
    return bytes(chunks)


def send_rcon_packet(connection: socket.socket, request_id: int, packet_type: int, body: str) -> None:
    encoded = body.encode("utf-8")
    payload = struct.pack("<ii", request_id, packet_type) + encoded + b"\0\0"
    connection.sendall(struct.pack("<i", len(payload)) + payload)


def receive_rcon_packet(connection: socket.socket) -> tuple[int, int, str]:
    length = struct.unpack("<i", receive_exact(connection, 4))[0]
    payload = receive_exact(connection, length)
    request_id, packet_type = struct.unpack("<ii", payload[:8])
    return request_id, packet_type, payload[8:-2].decode("utf-8", errors="replace")


def rcon_command(port: int, command: str, allow_disconnect: bool = False) -> str:
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=10) as connection:
            connection.settimeout(15)
            send_rcon_packet(connection, 1, 3, RCON_PASSWORD)
            request_id, _, _ = receive_rcon_packet(connection)
            if request_id == -1:
                raise PermissionError("Minecraft rechazó la contraseña RCON")
            send_rcon_packet(connection, 2, 2, command)
            response_id, _, response = receive_rcon_packet(connection)
            if response_id == -1:
                raise RuntimeError(f"RCON rechazó el comando {command!r}")
            return response
    except (ConnectionError, OSError):
        if allow_disconnect:
            return ""
        raise


def wait_for_rcon(port: int, timeout: float = 30.0) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            rcon_command(port, "list")
            return
        except OSError:
            time.sleep(0.25)
    raise TimeoutError(f"RCON no respondió en el puerto local {port}")


def prepare_template(
    root: Path,
    template: Path,
    commands: list[str],
    output: Path,
    rcon_port: int,
) -> None:
    write_server_files(template, rcon_port)
    server = ServerProcess(root, template, output / "template-server.log")
    try:
        server.wait_for(READY_PATTERN, 180)
        wait_for_rcon(rcon_port)
        for command in commands:
            response = rcon_command(rcon_port, command)
            if any(error in response for error in COMMAND_ERRORS):
                raise RuntimeError(f"Minecraft rechazó {command!r}: {response}")
            time.sleep(0.05)
        rcon_command(rcon_port, "save-all flush")
        wait_seconds(5)
        errors = [line for line in server.lines if any(error in line for error in COMMAND_ERRORS)]
        if errors:
            raise RuntimeError("La preparación del mundo rechazó comandos:\n" + "\n".join(errors))
        server.stop(rcon_port)
    except BaseException:
        server.abort(rcon_port)
        raise


def clone_template(template: Path, destination: Path) -> None:
    shutil.copytree(
        template,
        destination,
        ignore=shutil.ignore_patterns("logs", "debug", "crash-reports"),
    )


def latest_file(root: Path, pattern: str) -> Path | None:
    matches = list(root.rglob(pattern))
    return max(matches, key=lambda path: path.stat().st_mtime_ns) if matches else None


def directory_size(root: Path) -> int:
    return sum(path.stat().st_size for path in root.rglob("*") if path.is_file())


def parse_profile(lines: tuple[str, ...]) -> tuple[float, int]:
    for line in reversed(lines):
        match = PROFILE_PATTERN.search(line)
        if match:
            seconds = float(match.group(1).replace(",", "."))
            return seconds, int(match.group(2))
    raise RuntimeError("Minecraft no informó del resultado de 'debug stop'")


def percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    if not ordered:
        return 0.0
    position = (len(ordered) - 1) * fraction
    lower = int(position)
    upper = min(lower + 1, len(ordered) - 1)
    weight = position - lower
    return ordered[lower] * (1.0 - weight) + ordered[upper] * weight


def find_jfr_tool() -> Path | None:
    java_home = os.environ.get("JAVA_HOME")
    candidates = []
    if java_home:
        candidates.append(Path(java_home) / "bin" / "jfr.exe")
    command = shutil.which("jfr") or shutil.which("jfr.exe")
    if command:
        candidates.append(Path(command))
    java_root = Path(os.environ.get("ProgramFiles", r"C:\Program Files")) / "Java"
    if java_root.is_dir():
        candidates.extend(path / "bin" / "jfr.exe" for path in sorted(java_root.iterdir(), reverse=True))
    return next((candidate for candidate in candidates if candidate.is_file()), None)


def jfr_tick_p95(recording: Path) -> float:
    jfr_tool = find_jfr_tool()
    if jfr_tool is None:
        return 0.0
    output = subprocess.check_output(
        [str(jfr_tool), "print", "--json", "--events", "minecraft.ServerTickTime", str(recording)],
        text=True,
        encoding="utf-8",
    )
    document = json.loads(output)
    durations = []
    for event in document.get("recording", {}).get("events", []):
        duration = event.get("values", {}).get("averageTickDuration", "")
        match = re.fullmatch(r"PT([0-9.]+)S", duration)
        if match:
            durations.append(float(match.group(1)) * 1000.0)
    return percentile(durations, 0.95)


def report_metrics(game_directory: Path, recording: Path) -> dict[str, float | int | str]:
    report = latest_file(game_directory, "jfr-report-*.json")
    if report is None:
        raise RuntimeError("Minecraft no produjo el resumen JSON de JFR")
    document = json.loads(report.read_text(encoding="utf-8"))
    server_tick = document.get("serverTick", {})
    heap = document.get("heap", {})
    cpu = document.get("cpuPercent", {}).get("jvm", {})
    network = document.get("network", {})
    file_io = document.get("fileIO", {})
    sent = network.get("sent", {})
    received = network.get("received", {})
    chunks_written = file_io.get("chunksWritten", {})
    writes = file_io.get("write", {})
    return {
        "jfr_report": str(report),
        "mean_mspt": round(float(server_tick.get("averageMs", 0.0)), 6),
        "p95_mspt": round(jfr_tick_p95(recording), 6),
        "p99_mspt": round(float(server_tick.get("p99", 0.0)), 6),
        "jvm_cpu_percent": round(float(cpu.get("average", 0.0)) * 100.0, 6),
        "allocation_bytes_per_second": round(float(heap.get("allocationRateBytesPerSecond", 0.0)), 3),
        "packets_sent": int(sent.get("count", 0)),
        "packet_bytes_sent": int(sent.get("totalBytes", 0)),
        "packets_received": int(received.get("count", 0)),
        "packet_bytes_received": int(received.get("totalBytes", 0)),
        "chunks_written": int(chunks_written.get("count", 0)),
        "chunk_bytes_written": int(chunks_written.get("totalBytes", 0)),
        "file_write_count": int(writes.get("count", 0)),
        "file_bytes_written": int(writes.get("totalBytes", 0)),
    }


def run_measurement(
    root: Path,
    template: Path,
    output: Path,
    run_number: int,
    warmup_seconds: float,
    measure_seconds: float,
    rcon_port: int,
) -> dict[str, object]:
    run_root = output / f"run-{run_number}"
    game_directory = run_root / "game"
    run_root.mkdir(parents=True)
    clone_template(template, game_directory)
    write_server_files(game_directory, rcon_port)
    server = ServerProcess(root, game_directory, run_root / "server.log")
    try:
        server.wait_for(READY_PATTERN, 180)
        wait_for_rcon(rcon_port)
        wait_seconds(warmup_seconds)
        rcon_command(rcon_port, "jfr start")
        rcon_command(rcon_port, "debug start")
        wait_seconds(measure_seconds)
        profile_response = rcon_command(rcon_port, "debug stop")
        rcon_command(rcon_port, "jfr stop")
        wait_seconds(2)
        measured_seconds, ticks = parse_profile(server.lines + (profile_response,))
        server.stop(rcon_port)
    except BaseException:
        server.abort(rcon_port)
        raise

    jfr = latest_file(game_directory, "*.jfr")
    profile = latest_file(game_directory, "*.zip")
    if jfr is None:
        raise RuntimeError(f"La ejecución {run_number} no produjo una grabación JFR")
    debug_tick_interval_ms = measured_seconds * 1000.0 / ticks
    result = {
        "run": run_number,
        "measure_seconds": round(measured_seconds, 6),
        "ticks": ticks,
        "debug_tick_interval_ms": round(debug_tick_interval_ms, 6),
        "profile_ticks_per_second": round(ticks / measured_seconds, 6),
        "jfr_file": str(jfr.relative_to(output)),
        "jfr_bytes": jfr.stat().st_size,
        "profile_file": str(profile.relative_to(output)) if profile else "",
        "profile_bytes": profile.stat().st_size if profile else 0,
        "world_bytes": directory_size(game_directory / "world"),
    }
    result.update(report_metrics(game_directory, jfr))
    result["jfr_report"] = str(Path(str(result["jfr_report"])).relative_to(output))
    return result


def write_results(output: Path, rows: list[dict[str, object]], scenario: str) -> None:
    runs_file = output / "runs.csv"
    with runs_file.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    summary = {
        "scenario": scenario,
        "runs": len(rows),
        "median_mean_mspt": round(statistics.median(float(row["mean_mspt"]) for row in rows), 6),
        "median_p95_mspt": round(
            statistics.median(float(row["p95_mspt"]) for row in rows), 6
        ),
        "median_jvm_cpu_percent": round(
            statistics.median(float(row["jvm_cpu_percent"]) for row in rows), 6
        ),
        "median_allocation_bytes_per_second": round(
            statistics.median(float(row["allocation_bytes_per_second"]) for row in rows), 3
        ),
        "median_packets_sent": int(statistics.median(int(row["packets_sent"]) for row in rows)),
        "median_packet_bytes_sent": int(
            statistics.median(int(row["packet_bytes_sent"]) for row in rows)
        ),
        "median_chunks_written": int(statistics.median(int(row["chunks_written"]) for row in rows)),
        "median_profile_ticks_per_second": round(
            statistics.median(float(row["profile_ticks_per_second"]) for row in rows), 6
        ),
        "median_jfr_bytes": int(statistics.median(int(row["jfr_bytes"]) for row in rows)),
        "median_world_bytes": int(statistics.median(int(row["world_bytes"]) for row in rows)),
    }
    with (output / "summary.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(summary))
        writer.writeheader()
        writer.writerow(summary)
    print(",".join(summary))
    print(",".join(str(value) for value in summary.values()))
    print(f"Resultados: {output}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scenario", default="idle-machines")
    parser.add_argument("--workload", type=Path)
    parser.add_argument("--runs", type=int, default=3)
    parser.add_argument("--warmup-seconds", type=float, default=15.0)
    parser.add_argument("--measure-seconds", type=float, default=30.0)
    parser.add_argument("--output-directory", type=Path)
    parser.add_argument(
        "--template-directory",
        type=Path,
        help="Reuse an already prepared game directory instead of creating a new world.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.runs < 1:
        raise ValueError("--runs debe ser al menos 1")
    if args.warmup_seconds < 0 or args.measure_seconds <= 0:
        raise ValueError("Los tiempos de calentamiento y medición no son válidos")
    root = Path(__file__).resolve().parents[2]
    workload = args.workload or Path(__file__).with_name(f"{args.scenario}.txt")
    workload = workload.resolve()
    timestamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
    output = (args.output_directory or root / "build" / "performance" / "results" / args.scenario / timestamp).resolve()
    output.mkdir(parents=True, exist_ok=False)
    properties = read_gradle_properties(root)
    metadata = {
        "scenario": args.scenario,
        "workload": str(workload),
        "template_directory": str(args.template_directory or ""),
        "runs": args.runs,
        "warmup_seconds": args.warmup_seconds,
        "measure_seconds": args.measure_seconds,
        "git_commit": git_commit(root),
        "git_dirty": git_dirty(root),
        "minecraft_version": properties.get("minecraft_version", ""),
        "neo_version": properties.get("neo_version", ""),
        "java_home": os.environ.get("JAVA_HOME", ""),
        "operating_system": platform.platform(),
        "processor": platform.processor(),
        "created_utc": dt.datetime.now(dt.UTC).isoformat(),
    }
    (output / "metadata.txt").write_text(
        "\n".join(f"{key}={value}" for key, value in metadata.items()) + "\n",
        encoding="utf-8",
    )
    (output / "metadata.json").write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    template = args.template_directory.resolve() if args.template_directory else output / "template"
    commands = read_workload(workload)
    rcon_port = free_tcp_port()
    if args.template_directory:
        if not (template / "world" / "level.dat").is_file():
            raise ValueError(f"La plantilla {template} no contiene un mundo de Minecraft")
        print(f"Reutilizando plantilla: {template}")
    else:
        print(f"Preparando plantilla '{args.scenario}' con {len(commands)} comandos...")
        prepare_template(root, template, commands, output, rcon_port)
    rows = []
    for run_number in range(1, args.runs + 1):
        print(f"Ejecución {run_number}/{args.runs}...")
        rows.append(
            run_measurement(
                root,
                template,
                output,
                run_number,
                args.warmup_seconds,
                args.measure_seconds,
                rcon_port,
            )
        )
    write_results(output, rows, args.scenario)


def git_commit(root: Path) -> str:
    return subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=root, text=True, encoding="utf-8"
    ).strip()


def git_dirty(root: Path) -> str:
    status = subprocess.check_output(
        ["git", "status", "--porcelain"], cwd=root, text=True, encoding="utf-8"
    )
    return str(bool(status.strip())).lower()


def read_gradle_properties(root: Path) -> dict[str, str]:
    result = {}
    for raw_line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


if __name__ == "__main__":
    main()
