#!/usr/bin/env python3
"""Verify compatibility identifiers frozen for the Trading Cells 1.0 line."""

from __future__ import annotations

import json
import hashlib
import re
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = Path(__file__).with_name("contracts-1.0.0.json")
JAVA_ROOT = PROJECT_ROOT / "src/main/java/com/cosmocraft/trading_cells"
NETWORK_ROOT = JAVA_ROOT / "platform/neoforge/network"
RECIPE_ROOT = PROJECT_ROOT / "src/main/resources/data/trading_cells/recipe"


def read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def integer_constant(source: str, name: str) -> int | None:
    match = re.search(rf"\b{name}\s*=\s*(\d+)\s*;", source)
    return int(match.group(1)) if match else None


def recipe_catalog_fingerprint() -> tuple[int, str]:
    digest = hashlib.sha256()
    recipes = sorted(RECIPE_ROOT.rglob("*.json"), key=lambda path: path.relative_to(RECIPE_ROOT).as_posix())
    for path in recipes:
        relative_path = path.relative_to(RECIPE_ROOT).as_posix()
        canonical_json = json.dumps(
            json.loads(path.read_text(encoding="utf-8")),
            sort_keys=True,
            separators=(",", ":"),
            ensure_ascii=True,
        )
        digest.update(relative_path.encode("utf-8"))
        digest.update(b"\0")
        digest.update(canonical_json.encode("utf-8"))
        digest.update(b"\0")
    return len(recipes), digest.hexdigest().upper()


def main() -> int:
    contract = json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))
    errors: list[str] = []

    properties = read_properties(PROJECT_ROOT / "gradle.properties")
    if properties.get("mod_version") != contract["mod_version"]:
        errors.append(
            "mod_version changed: "
            f"expected {contract['mod_version']}, got {properties.get('mod_version')}"
        )

    registration = (NETWORK_ROOT / "TradingCellPayloadRegistration.java").read_text(encoding="utf-8")
    registration_match = re.search(r'event\.registrar\("([^"]+)"\)', registration)
    actual_registration = registration_match.group(1) if registration_match else None
    if actual_registration != contract["network_registration_version"]:
        errors.append(
            "network registration version changed: "
            f"expected {contract['network_registration_version']}, got {actual_registration}"
        )

    schema_source = (
        JAVA_ROOT / "platform/neoforge/mobfarm/MobFarmTargetReloadListener.java"
    ).read_text(encoding="utf-8")
    actual_schema = integer_constant(schema_source, "SCHEMA_VERSION")
    if actual_schema != contract["mob_farm_target_schema_version"]:
        errors.append(
            "mob_farm_target schema changed: "
            f"expected {contract['mob_farm_target_schema_version']}, got {actual_schema}"
        )

    crop_schema_source = (
        JAVA_ROOT / "feature/farmer/adapters/input/FarmerCropReloadListener.java"
    ).read_text(encoding="utf-8")
    actual_crop_schema = integer_constant(crop_schema_source, "SCHEMA_VERSION")
    if actual_crop_schema != contract["farmer_crop_schema_version"]:
        errors.append(
            "farmer_crop schema changed: "
            f"expected {contract['farmer_crop_schema_version']}, got {actual_crop_schema}"
        )

    catalog_source = (NETWORK_ROOT / "MobFarmCatalogSyncPayload.java").read_text(encoding="utf-8")
    actual_protocol = integer_constant(catalog_source, "CURRENT_PROTOCOL_VERSION")
    if actual_protocol != contract["mob_farm_catalog_protocol_version"]:
        errors.append(
            "mob-farm catalog protocol changed: "
            f"expected {contract['mob_farm_catalog_protocol_version']}, got {actual_protocol}"
        )

    recipe_count, recipe_fingerprint = recipe_catalog_fingerprint()
    if recipe_count != contract["recipe_catalog_count"]:
        errors.append(
            "recipe catalog count changed: "
            f"expected {contract['recipe_catalog_count']}, got {recipe_count}"
        )
    if recipe_fingerprint != contract["recipe_catalog_sha256"]:
        errors.append(
            "recipe catalog content changed: "
            f"expected {contract['recipe_catalog_sha256']}, got {recipe_fingerprint}"
        )

    for class_name, payload_id in contract["payload_ids"].items():
        source_path = NETWORK_ROOT / f"{class_name}.java"
        if not source_path.is_file():
            errors.append(f"missing payload class: {source_path.relative_to(PROJECT_ROOT)}")
            continue
        source = source_path.read_text(encoding="utf-8")
        if f'"{payload_id}"' not in source:
            errors.append(f"{class_name}: missing frozen payload id {payload_id}")
        if f"{class_name}.PAYLOAD_TYPE" not in registration:
            errors.append(f"{class_name}: payload is no longer registered")

    for relative_path, keys in contract["persistent_nbt_keys"].items():
        source_path = JAVA_ROOT / relative_path
        if not source_path.is_file():
            errors.append(f"missing persistent class: {source_path.relative_to(PROJECT_ROOT)}")
            continue
        source = source_path.read_text(encoding="utf-8")
        for key in keys:
            if f'"{key}"' not in source:
                errors.append(f"{relative_path}: missing frozen NBT key {key}")

    if errors:
        print("Release contract violations:", file=sys.stderr)
        for error in errors:
            print(f" - {error}", file=sys.stderr)
        return 1

    print(
        "Release contracts valid: "
        f"{len(contract['payload_ids'])} payload IDs, "
        f"{sum(map(len, contract['persistent_nbt_keys'].values()))} NBT keys, "
        f"{recipe_count} recipes, schema {actual_schema}, protocol {actual_protocol}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
