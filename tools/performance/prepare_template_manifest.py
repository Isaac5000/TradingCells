#!/usr/bin/env python3
"""Create or refresh the manifest for an already prepared benchmark template."""

from __future__ import annotations

import argparse
from pathlib import Path

from template_contract import scenario_definition, template_fingerprint, write_manifest
from platform_tools import configure_utf8_stdio


def read_gradle_properties(root: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw_line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("template", type=Path)
    parser.add_argument("--category", choices=("server", "client"), required=True)
    parser.add_argument("--scenario", required=True)
    parser.add_argument("--notes", default="")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    tools = Path(__file__).resolve().parent
    template = args.template.resolve()
    if not template.is_dir():
        raise ValueError(f"Template directory does not exist: {template}")
    definition = scenario_definition(tools, args.category, args.scenario)
    properties = read_gradle_properties(root)
    path = write_manifest(
        template,
        args.category,
        args.scenario,
        definition,
        properties.get("minecraft_version", ""),
        properties.get("neo_version", ""),
        notes=args.notes,
    )
    print(f"Manifest: {path}")
    print(f"Fingerprint: {template_fingerprint(template)}")


if __name__ == "__main__":
    configure_utf8_stdio()
    main()
