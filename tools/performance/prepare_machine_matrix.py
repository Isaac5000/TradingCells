#!/usr/bin/env python3
"""Build a benchmark template by cloning real configured machines in Minecraft."""

from __future__ import annotations

import argparse
from pathlib import Path
import shutil

from run_server_benchmark import free_tcp_port, prepare_template, read_gradle_properties
from template_contract import scenario_definition, template_fingerprint, write_manifest
from platform_tools import configure_utf8_stdio


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_world", type=Path)
    parser.add_argument("output_template", type=Path)
    parser.add_argument("--category", choices=("client", "server"), default="client")
    parser.add_argument("--scenario", default="machine-grid-405")
    parser.add_argument(
        "--source",
        action="append",
        nargs=3,
        type=int,
        metavar=("X", "Y", "Z"),
        required=True,
        help="Configured source Block Entity. Repeat to create a mixed matrix.",
    )
    parser.add_argument("--origin", nargs=3, type=int, default=(-213, -60, -212))
    parser.add_argument("--size", nargs=3, type=int, default=(9, 5, 9), metavar=("X", "Y", "Z"))
    parser.add_argument(
        "--setup-command",
        action="append",
        default=[],
        help="Extra server command applied after the matrix has been cloned.",
    )
    parser.add_argument("--notes", default="")
    return parser.parse_args()


def clone_command(source: tuple[int, int, int], destination: tuple[int, int, int]) -> str:
    sx, sy, sz = source
    dx, dy, dz = destination
    return f"clone {sx} {sy} {sz} {sx} {sy} {sz} {dx} {dy} {dz} replace force"


def matrix_commands(args: argparse.Namespace) -> list[str]:
    origin_x, origin_y, origin_z = args.origin
    size_x, size_y, size_z = args.size
    if min(size_x, size_y, size_z) < 1:
        raise ValueError("Matrix dimensions must be positive")

    sources = [tuple(value) for value in args.source]
    stage_origin = (origin_x - 32, origin_y, origin_z - 32)
    stages = [
        (stage_origin[0] + index, stage_origin[1], stage_origin[2])
        for index in range(len(sources))
    ]
    extent = sources + stages + [
        (origin_x, origin_y, origin_z),
        (origin_x + size_x - 1, origin_y + size_y - 1, origin_z + size_z - 1),
    ]
    min_x = min(position[0] for position in extent)
    min_z = min(position[2] for position in extent)
    max_x = max(position[0] for position in extent)
    max_z = max(position[2] for position in extent)
    commands = [
        f"forceload add {min_x} {min_z} {max_x} {max_z}",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "weather clear",
        "time set noon",
    ]
    commands.extend(clone_command(source, stage) for source, stage in zip(sources, stages, strict=True))

    index = 0
    for offset_y in range(size_y):
        for offset_z in range(size_z):
            for offset_x in range(size_x):
                destination = (
                    origin_x + offset_x,
                    origin_y + offset_y,
                    origin_z + offset_z,
                )
                commands.append(clone_command(stages[index % len(stages)], destination))
                index += 1
    commands.extend(args.setup_command)
    commands.extend(f"setblock {x} {y} {z} minecraft:air" for x, y, z in stages)
    return commands


def main() -> None:
    args = parse_args()
    root = Path(__file__).resolve().parents[2]
    tools = Path(__file__).resolve().parent
    source_world = args.source_world.resolve()
    template = args.output_template.resolve()
    if not (source_world / "level.dat").is_file():
        raise ValueError(f"Source world is invalid: {source_world}")
    if template.exists():
        raise ValueError(f"Output template already exists: {template}")

    template.mkdir(parents=True)
    shutil.copytree(
        source_world,
        template / "world",
        ignore=shutil.ignore_patterns("session.lock"),
    )
    commands = matrix_commands(args)
    prepare_template(root, template, commands, template, free_tcp_port())

    properties = read_gradle_properties(root)
    definition = scenario_definition(tools, args.category, args.scenario)
    machine_count = args.size[0] * args.size[1] * args.size[2]
    notes = args.notes or (
        f"{machine_count} real configured machines cloned from {len(args.source)} source blocks."
    )
    path = write_manifest(
        template,
        args.category,
        args.scenario,
        definition,
        properties.get("minecraft_version", ""),
        properties.get("neo_version", ""),
        notes=notes,
    )
    print(f"Template: {template}")
    print(f"Manifest: {path}")
    print(f"Fingerprint: {template_fingerprint(template)}")


if __name__ == "__main__":
    configure_utf8_stdio()
    main()
