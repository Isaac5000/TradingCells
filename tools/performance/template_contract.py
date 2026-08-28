"""Shared contract for reproducible Trading Cells benchmark templates."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any


SCHEMA_VERSION = 1
MANIFEST_NAME = "trading-cells-performance.json"
IGNORED_DIRECTORIES = {".cache", "crash-reports", "debug", "logs", "screenshots"}
IGNORED_FILES = {"session.lock"}


def load_matrix(tools_directory: Path) -> dict[str, Any]:
    path = tools_directory / "scenario-matrix.json"
    return json.loads(path.read_text(encoding="utf-8"))


def scenario_definition(
    tools_directory: Path,
    category: str,
    scenario: str,
) -> dict[str, Any]:
    matrix = load_matrix(tools_directory)
    for definition in matrix.get(category, []):
        if definition.get("id") == scenario:
            return definition
    known = ", ".join(entry.get("id", "") for entry in matrix.get(category, []))
    raise ValueError(f"Unknown {category} scenario {scenario!r}; expected one of: {known}")


def expected_workload(definition: dict[str, Any]) -> dict[str, Any]:
    ignored = {"id", "notes", "template_required", "variants", "workload"}
    return {key: value for key, value in definition.items() if key not in ignored}


def write_manifest(
    template: Path,
    category: str,
    scenario: str,
    definition: dict[str, Any],
    minecraft_version: str,
    neo_version: str,
    workload_sha256: str = "",
    notes: str = "",
) -> Path:
    document = {
        "schema_version": SCHEMA_VERSION,
        "category": category,
        "scenario": scenario,
        "minecraft_version": minecraft_version,
        "neo_version": neo_version,
        "expected": expected_workload(definition),
        "workload_sha256": workload_sha256,
        "notes": notes,
    }
    path = template / MANIFEST_NAME
    path.write_text(json.dumps(document, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return path


def validate_manifest(
    template: Path,
    category: str,
    scenario: str,
    definition: dict[str, Any],
    minecraft_version: str,
    neo_version: str,
) -> dict[str, Any]:
    path = template / MANIFEST_NAME
    if not path.is_file():
        raise ValueError(
            f"Template {template} has no {MANIFEST_NAME}; create it with "
            "prepare_template_manifest.py before benchmarking"
        )
    document = json.loads(path.read_text(encoding="utf-8"))
    expected_fields = {
        "schema_version": SCHEMA_VERSION,
        "category": category,
        "scenario": scenario,
        "minecraft_version": minecraft_version,
        "neo_version": neo_version,
        "expected": expected_workload(definition),
    }
    differences = [
        f"{key}={document.get(key)!r}/{expected!r}"
        for key, expected in expected_fields.items()
        if document.get(key) != expected
    ]
    if differences:
        raise ValueError("Template manifest does not match the benchmark: " + ", ".join(differences))
    return document


def template_fingerprint(template: Path) -> str:
    digest = hashlib.sha256()
    files = sorted(
        path
        for path in template.rglob("*")
        if path.is_file()
        and path.name not in IGNORED_FILES
        and not IGNORED_DIRECTORIES.intersection(path.relative_to(template).parts)
    )
    for path in files:
        relative = path.relative_to(template).as_posix().encode("utf-8")
        digest.update(len(relative).to_bytes(4, "big"))
        digest.update(relative)
        with path.open("rb") as handle:
            while chunk := handle.read(1024 * 1024):
                digest.update(chunk)
    return digest.hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while chunk := handle.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()
