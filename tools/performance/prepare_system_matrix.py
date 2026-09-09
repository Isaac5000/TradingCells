#!/usr/bin/env python3
"""Prepare loaded-chunk stress templates for the universal logistics network."""

from __future__ import annotations

import argparse
from pathlib import Path
import shutil

from platform_tools import configure_utf8_stdio
from run_server_benchmark import free_tcp_port, prepare_template, read_gradle_properties
from template_contract import scenario_definition, template_fingerprint, write_manifest


SYSTEMS = {f"logistics-{size}-{state}": size
           for size in (1024, 4096) for state in ("idle", "active")}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_world", type=Path)
    parser.add_argument("output_template", type=Path)
    parser.add_argument("--system", choices=tuple(SYSTEMS), required=True)
    parser.add_argument("--origin", nargs=3, type=int, default=(4096, -60, 4096))
    parser.add_argument("--category", choices=("server", "client"), default="server")
    parser.add_argument("--notes", default="")
    return parser.parse_args()


def logistics_commands(origin: tuple[int, int, int], system: str) -> tuple[list[str], str]:
    x, y, z = origin
    edge = 32 if SYSTEMS[system] == 1024 else 64
    end_x, end_z = x + edge - 1, z + edge - 1
    # The benchmark harness holds this fixed workload loaded; the pipes never create tickets.
    commands = [
        f"forceload add {x - 1} {z - 1} {end_x + 1} {end_z + 1}",
        "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
        "weather clear", "time set noon",
        f"fill {x} {y} {z} {end_x} {y} {end_z} trading_cells:universal_pipe replace",
        f"setblock {x} {y} {z - 1} trading_cells:experience_storage",
        f"data merge block {x} {y} {z - 1} {{StoredExperience:2147483647}}",
        f"setblock {end_x} {y} {end_z + 1} trading_cells:arcane_infuser",
        f"setblock {x - 1} {y} {z} trading_cells:network_terminal",
    ]
    mode = "extract" if system.endswith("active") else "none"
    commands.append(f'data merge block {x} {y} {z} '
                    f'{{LogisticsFacenorth:{{SchemaVersion:1,Mode:"{mode}",ExplicitMode:1b,UpgradeTier:4}}}}')
    return commands, f"{x - 1},{y},{z}"


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
    origin = tuple(args.origin)
    commands, interaction = logistics_commands(origin, args.system)
    prepare_template(root, template, commands, template, free_tcp_port())

    category, scenario = args.category, args.system
    properties = read_gradle_properties(root)
    definition = scenario_definition(tools, category, scenario)
    default_notes = f"Generated {args.system} stress template at {origin}."
    if interaction:
        default_notes += f" Open block: {interaction}."
    path = write_manifest(
        template,
        category,
        scenario,
        definition,
        properties.get("minecraft_version", ""),
        properties.get("neo_version", ""),
        notes=args.notes or default_notes,
    )
    print(f"Template: {template}")
    print(f"Manifest: {path}")
    print(f"Fingerprint: {template_fingerprint(template)}")
    if interaction:
        print(f"Open block: {interaction}")


if __name__ == "__main__":
    configure_utf8_stdio()
    main()
