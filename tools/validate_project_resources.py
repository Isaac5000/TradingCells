#!/usr/bin/env python3
"""Validate Trading Cells resources without launching Minecraft.

The checks intentionally cover references owned by Trading Cells only. Vanilla and
third-party namespaces are resolved by their respective mods at runtime.
"""

from __future__ import annotations

import argparse
import json
import re
import struct
import sys
from pathlib import Path
from typing import Any, Iterable


MOD_ID = "trading_cells"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
ENTITY_FARM_BLOCKS = {
    "arthropod_farm",
    "blaze_farm",
    "breeze_farm",
    "creeper_farm",
    "enderman_farm",
    "ghast_farm",
    "guardian_farm",
    "fish_farm",
    "livestock_farm",
    "phantom_farm",
    "piglin_farm",
    "raider_farm",
    "shulker_farm",
    "skeleton_farm",
    "slime_farm",
    "zombie_farm",
}
ENTITY_FARM_FRAME_TEXTURE = "minecraft:block/iron_block"
ENTITY_FARM_BASE_TEXTURES = {
    "arthropod_farm": "minecraft:block/pale_moss_block",
    "blaze_farm": "minecraft:block/nether_bricks",
    "breeze_farm": "minecraft:block/polished_tuff",
    "creeper_farm": "minecraft:block/pale_moss_block",
    "enderman_farm": "minecraft:block/end_stone",
    "ghast_farm": "minecraft:block/soul_soil",
    "guardian_farm": "minecraft:block/prismarine_bricks",
    "fish_farm": "minecraft:block/sand",
    "livestock_farm": "minecraft:block/hay_block_top",
    "phantom_farm": "minecraft:block/pale_moss_block",
    "piglin_farm": "minecraft:block/polished_blackstone",
    "raider_farm": "minecraft:block/dark_oak_planks",
    "shulker_farm": "minecraft:block/purpur_block",
    "skeleton_farm": "minecraft:block/pale_moss_block",
    "slime_farm": "trading_cells:block/slime_block_opaque",
    "zombie_farm": "minecraft:block/pale_moss_block",
}
LEGACY_ENTITY_FARM_BLOCKS = ENTITY_FARM_BLOCKS | {
    "aquatic_farm", "mount_farm", "amphibian_farm", "bee_farm", "creaking_farm",
}
CRAFTING_RECIPE_TYPES = {
    "minecraft:crafting_shaped",
    "minecraft:crafting_shapeless",
    "minecraft:smithing_transform",
    "minecraft:smithing_trim",
}
IDENTIFIER_PATTERN = re.compile(r"^(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+$")
MOB_FARM_FAMILIES = {"trading_cells:skeleton", "trading_cells:zombie"}


class ValidationFailure(Exception):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Project root (defaults to the parent of tools/).",
    )
    return parser.parse_args()


def reject_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValidationFailure(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def load_json(path: Path) -> Any:
    try:
        with path.open("r", encoding="utf-8") as handle:
            return json.load(handle, object_pairs_hook=reject_duplicate_keys)
    except (OSError, UnicodeError, json.JSONDecodeError, ValidationFailure) as error:
        raise ValidationFailure(f"{path}: {error}") from error


def resource_roots(project_root: Path) -> list[Path]:
    roots = [project_root / "src/main/resources"]
    generated = project_root / "src/generated/resources"
    if generated.is_dir():
        roots.append(generated)
    return roots


def all_files(roots: Iterable[Path], pattern: str) -> list[Path]:
    return sorted(path for root in roots if root.is_dir() for path in root.rglob(pattern))


def split_identifier(identifier: str) -> tuple[str, str]:
    if ":" in identifier:
        return tuple(identifier.split(":", 1))  # type: ignore[return-value]
    return "minecraft", identifier


def find_resource(roots: Iterable[Path], relative_path: Path) -> Path | None:
    for root in roots:
        candidate = root / relative_path
        if candidate.is_file():
            return candidate
    return None


def walk_values(value: Any) -> Iterable[tuple[str | None, Any]]:
    if isinstance(value, dict):
        for key, child in value.items():
            yield key, child
            yield from walk_values(child)
    elif isinstance(value, list):
        for child in value:
            yield None, child
            yield from walk_values(child)


def validate_languages(roots: list[Path], errors: list[str]) -> None:
    lang_relative = Path("assets") / MOD_ID / "lang"
    language_files: dict[str, Path] = {}
    for root in roots:
        lang_dir = root / lang_relative
        if not lang_dir.is_dir():
            continue
        for path in lang_dir.glob("*.json"):
            language_files.setdefault(path.stem, path)

    required = {"en_us", "es_es"}
    missing = required.difference(language_files)
    if missing:
        errors.append(f"missing required language files: {', '.join(sorted(missing))}")
        return

    parsed: dict[str, dict[str, Any]] = {}
    for language, path in language_files.items():
        data = load_json(path)
        if not isinstance(data, dict):
            errors.append(f"{path}: language root must be an object")
            continue
        parsed[language] = data
        for key, value in data.items():
            if not isinstance(value, str) or not value.strip():
                errors.append(f"{path}: {key} must contain non-empty text")

    if not required.issubset(parsed):
        return
    english_keys = set(parsed["en_us"])
    spanish_keys = set(parsed["es_es"])
    for key in sorted(english_keys - spanish_keys):
        errors.append(f"es_es.json: missing key present in en_us.json: {key}")
    for key in sorted(spanish_keys - english_keys):
        errors.append(f"en_us.json: missing key present in es_es.json: {key}")


def validate_data_directories(roots: list[Path], errors: list[str]) -> None:
    deprecated = {
        "loot_tables": "loot_table",
        "recipes": "recipe",
        "advancements": "advancement",
    }
    for root in roots:
        namespace = root / "data" / MOD_ID
        for old_name, current_name in deprecated.items():
            old_path = namespace / old_name
            if old_path.is_dir() and any(path.is_file() for path in old_path.rglob("*")):
                errors.append(
                    f"{old_path}: obsolete data directory; use {current_name}/ for Minecraft 26.2"
                )


def validate_pickaxe_coverage(
    roots: list[Path], parsed_json: dict[Path, Any], errors: list[str]
) -> None:
    tag_path = find_resource(
        roots, Path("data/minecraft/tags/block/mineable/pickaxe.json")
    )
    if tag_path is None:
        errors.append("missing minecraft:mineable/pickaxe block tag")
        return

    document = parsed_json.get(tag_path)
    values = document.get("values") if isinstance(document, dict) else None
    if not isinstance(values, list):
        errors.append(f"{tag_path}: values must be an array")
        return

    tagged_blocks: set[str] = set()
    for value in values:
        if isinstance(value, str):
            identifier = value
        elif isinstance(value, dict):
            identifier = value.get("id")
        else:
            identifier = None
        if isinstance(identifier, str) and identifier.startswith(f"{MOD_ID}:"):
            tagged_blocks.add(identifier)

    expected_blocks: set[str] = set()
    relative_root = Path("assets") / MOD_ID / "blockstates"
    for root in roots:
        directory = root / relative_root
        if not directory.is_dir():
            continue
        for path in directory.rglob("*.json"):
            block_id = path.relative_to(directory).with_suffix("").as_posix()
            expected_blocks.add(f"{MOD_ID}:{block_id}")

    for identifier in sorted(expected_blocks - tagged_blocks):
        errors.append(f"{tag_path}: mod block missing from pickaxe tag: {identifier}")


def validate_entity_farm_particles(
    roots: list[Path], parsed_json: dict[Path, Any], errors: list[str]
) -> None:
    for block_id in sorted(ENTITY_FARM_BLOCKS):
        model_paths = (
            Path("assets") / MOD_ID / "models" / "block" / f"{block_id}.json",
            Path("assets") / MOD_ID / "models" / "block" / f"{block_id}_frame.json",
            Path("assets") / MOD_ID / "models" / "item" / f"{block_id}.json",
        )
        for model_path in model_paths:
            path = find_resource(roots, model_path)
            if path is None:
                errors.append(f"missing entity-farm model: {model_path}")
                continue
            document = parsed_json.get(path)
            textures = document.get("textures") if isinstance(document, dict) else None
            if not isinstance(textures, dict):
                errors.append(f"{path}: textures must be an object")
                continue
            if textures.get("frame") != ENTITY_FARM_FRAME_TEXTURE:
                errors.append(f"{path}: entity-farm frame must use iron_block")
            if textures.get("particle") != ENTITY_FARM_FRAME_TEXTURE:
                errors.append(f"{path}: entity-farm particles must use the iron frame texture")
            expected_base = ENTITY_FARM_BASE_TEXTURES[block_id]
            for texture_key in ("base", "base_left", "base_center", "base_right"):
                if texture_key in textures and textures[texture_key] != expected_base:
                    errors.append(
                        f"{path}: entity-farm {texture_key} must use {expected_base}"
                    )
            expected_parent = (
                "trading_cells:item/villager_breeder"
                if model_path.parent.name == "item"
                else "trading_cells:block/machine_cage_frame"
                if model_path.stem.endswith("_frame")
                else "trading_cells:block/villager_breeder"
            )
            if document.get("parent") != expected_parent:
                errors.append(f"{path}: entity-farm model must inherit {expected_parent}")


def validate_models_and_textures(
    roots: list[Path], parsed_json: dict[Path, Any], errors: list[str]
) -> None:
    owned_assets = Path("assets") / MOD_ID
    for path, data in parsed_json.items():
        try:
            relative = next(path.relative_to(root) for root in roots if path.is_relative_to(root))
        except StopIteration:
            continue
        if not relative.is_relative_to(owned_assets):
            continue

        for key, value in walk_values(data):
            if not isinstance(value, str):
                continue
            if key in {"model", "parent"}:
                namespace, resource_path = split_identifier(value)
                if namespace != MOD_ID:
                    continue
                target = Path("assets") / namespace / "models" / f"{resource_path}.json"
                if find_resource(roots, target) is None:
                    errors.append(f"{path}: unresolved model reference {value}")

        if not relative.is_relative_to(owned_assets / "models"):
            continue
        textures = data.get("textures", {}) if isinstance(data, dict) else {}
        if not isinstance(textures, dict):
            errors.append(f"{path}: textures must be an object")
            continue
        for texture in textures.values():
            if not isinstance(texture, str) or texture.startswith("#"):
                continue
            namespace, resource_path = split_identifier(texture)
            if namespace != MOD_ID:
                continue
            target = Path("assets") / namespace / "textures" / f"{resource_path}.png"
            if find_resource(roots, target) is None:
                errors.append(f"{path}: unresolved texture reference {texture}")


def validate_png_files(roots: list[Path], errors: list[str]) -> None:
    for path in all_files(roots, "*.png"):
        try:
            header = path.read_bytes()[:24]
        except OSError as error:
            errors.append(f"{path}: cannot read PNG: {error}")
            continue
        if len(header) < 24 or header[:8] != PNG_SIGNATURE or header[12:16] != b"IHDR":
            errors.append(f"{path}: invalid PNG header")
            continue
        width, height = struct.unpack(">II", header[16:24])
        if width == 0 or height == 0:
            errors.append(f"{path}: PNG dimensions must be positive")


def owned_item_definitions(roots: list[Path]) -> set[str]:
    result: set[str] = set()
    relative_root = Path("assets") / MOD_ID / "items"
    for root in roots:
        directory = root / relative_root
        if not directory.is_dir():
            continue
        for path in directory.rglob("*.json"):
            result.add(path.relative_to(directory).with_suffix("").as_posix())
    return result


def owned_item_tag_definitions(roots: list[Path]) -> set[str]:
    result: set[str] = set()
    relative_root = Path("data") / MOD_ID / "tags" / "item"
    for root in roots:
        directory = root / relative_root
        if not directory.is_dir():
            continue
        for path in directory.rglob("*.json"):
            result.add(path.relative_to(directory).with_suffix("").as_posix())
    return result


def validate_owned_item_reference(
    value: Any,
    path: Path,
    definitions: set[str],
    errors: list[str],
) -> None:
    if isinstance(value, str):
        if value.startswith("#"):
            return
        namespace, resource_path = split_identifier(value)
        if namespace == MOD_ID and resource_path not in definitions:
            errors.append(f"{path}: unresolved Trading Cells item reference {value}")
    elif isinstance(value, list):
        for child in value:
            validate_owned_item_reference(child, path, definitions, errors)
    elif isinstance(value, dict):
        for key in ("item", "items", "ingredient", "id"):
            if key in value:
                validate_owned_item_reference(value[key], path, definitions, errors)


def validate_recipes(
    roots: list[Path], parsed_json: dict[Path, Any], errors: list[str]
) -> None:
    recipe_relative = Path("data") / MOD_ID / "recipe"
    definitions = owned_item_definitions(roots)
    for root in roots:
        recipe_dir = root / recipe_relative
        if not recipe_dir.is_dir():
            continue
        for path in sorted(recipe_dir.rglob("*.json")):
            data = parsed_json[path]
            if not isinstance(data, dict):
                errors.append(f"{path}: recipe root must be an object")
                continue
            recipe_type = data.get("type")
            if not isinstance(recipe_type, str):
                errors.append(f"{path}: recipe type must be a string")
                continue
            if recipe_type in CRAFTING_RECIPE_TYPES and data.get("show_notification") is not False:
                errors.append(f"{path}: known recipe must set show_notification to false")

            result = data.get("result")
            if isinstance(result, dict):
                if result.get("type") in {"enchanted_book", "nitwit_villager"}:
                    pass
                else:
                    for key in ("id", "item"):
                        if key in result:
                            validate_owned_item_reference(result[key], path, definitions, errors)
            elif result is not None:
                validate_owned_item_reference(result, path, definitions, errors)

            for key in ("base", "addition", "ingredient", "ingredients"):
                if key in data:
                    validate_owned_item_reference(data[key], path, definitions, errors)
            recipe_key = data.get("key")
            if isinstance(recipe_key, dict):
                for ingredient in recipe_key.values():
                    validate_owned_item_reference(ingredient, path, definitions, errors)


def validate_entity_farm_recipes(
    roots: list[Path], parsed_json: dict[Path, Any], errors: list[str]
) -> None:
    for block_id in sorted(LEGACY_ENTITY_FARM_BLOCKS):
        path = find_resource(roots, Path(f"data/trading_cells/recipe/{block_id}_infusion.json"))
        if path is not None:
            errors.append(f"{path}: replaced entity-farm recipe must not be published")

    path = find_resource(roots, Path("data/trading_cells/recipe/mob_farm_infusion.json"))
    recipe = parsed_json.get(path) if path else None
    expected = (
        "minecraft:iron_block", "minecraft:black_concrete", "minecraft:iron_block",
        "minecraft:black_concrete", "trading_cells:experience_storage", "minecraft:black_concrete",
        "minecraft:quartz_block", "minecraft:black_concrete", "minecraft:quartz_block",
    )
    if recipe != {
        "type": "trading_cells:arcane_infusion", "category": "production",
        "ingredients": [{"ingredient": item, "count": 1} for item in expected],
        "experience": 50_000, "result": {"type": "item", "item": "trading_cells:mob_farm"},
    }:
        errors.append(f"{path}: general entity-farm infusion must match its simulation recipe")

    signatures: dict[tuple[str, ...], Path] = {}
    for root in roots:
        for path in sorted((root / "data/trading_cells/recipe").glob("*_spawn_egg_infusion.json")):
            recipe = parsed_json.get(path)
            if not isinstance(recipe, dict):
                errors.append(f"{path}: spawn-egg recipe must be an object")
                continue
            entity = path.name.removesuffix("_spawn_egg_infusion.json")
            if recipe.get("type") != "trading_cells:arcane_infusion" or recipe.get("category") != "generators":
                errors.append(f"{path}: spawn egg must use the generators infusion category")
            if recipe.get("result") != {"type": "item", "item": f"minecraft:{entity}_spawn_egg"}:
                errors.append(f"{path}: spawn-egg output must match its entity")
            xp = recipe.get("experience")
            if type(xp) is not int or xp <= 0 or xp % 10:
                errors.append(f"{path}: spawn-egg XP must be positive and a multiple of ten")
            ingredients = recipe.get("ingredients")
            if (not isinstance(ingredients, list) or len(ingredients) != 9
                    or any(not isinstance(slot, dict) or set(slot) != {"ingredient", "count"}
                           or slot.get("count") != 1 or not isinstance(slot.get("ingredient"), str)
                           for slot in ingredients)):
                errors.append(f"{path}: spawn egg requires nine single-item slots")
                continue
            ids = tuple(slot["ingredient"] for slot in ingredients)
            if ids[4] != "#trading_cells:arcane_infusion_eggs":
                errors.append(f"{path}: center slot must contain the infusion egg tag")
            materials = {ids[index] for index in range(9) if index != 4}
            if (not 1 <= len(materials) <= 2
                    or any(not item.startswith("minecraft:") or item.endswith("_spawn_egg") for item in materials)
                    or len({ids[index] for index in (0, 2, 6, 8)}) != 1
                    or len({ids[index] for index in (1, 3, 5, 7)}) != 1):
                errors.append(f"{path}: spawn-egg materials must form one or two symmetric rings")
            signature = tuple(sorted(ids))
            if signature in signatures:
                errors.append(f"{path}: duplicate spawn-egg materials from {signatures[signature]}")
            signatures[signature] = path


def validate_mob_farm_targets(
    roots: list[Path], parsed_json: dict[Path, Any], errors: list[str]
) -> None:
    definitions = owned_item_definitions(roots)
    item_tags = owned_item_tag_definitions(roots)
    seen_targets: dict[tuple[str, str], Path] = {}
    for root in roots:
        data_root = root / "data"
        if not data_root.is_dir():
            continue
        for path in sorted(data_root.glob("*/trading_cells/mob_farm_target/**/*.json")):
            data = parsed_json[path]
            if not isinstance(data, dict):
                errors.append(f"{path}: mob-farm target root must be an object")
                continue
            if data.get("schema_version") != 1:
                errors.append(f"{path}: schema_version must be the integer 1")
            family = data.get("family")
            if family not in MOB_FARM_FAMILIES:
                errors.append(f"{path}: unknown mob-farm family {family!r}")
            entity_type = data.get("entity_type")
            generator_item = data.get("generator_item")
            order = data.get("order")
            for key, value in (("entity_type", entity_type), ("generator_item", generator_item)):
                if not isinstance(value, str) or not IDENTIFIER_PATTERN.fullmatch(value):
                    errors.append(f"{path}: {key} must be a valid identifier")
            if not isinstance(order, int) or isinstance(order, bool):
                errors.append(f"{path}: order must be an integer")
            if isinstance(generator_item, str):
                validate_owned_item_reference(generator_item, path, definitions, errors)

            loot = data.get("loot", {})
            if not isinstance(loot, dict):
                errors.append(f"{path}: loot must be an object")
                continue
            for key in ("include", "exclude"):
                references = loot.get(key, [])
                if not isinstance(references, list):
                    errors.append(f"{path}: loot.{key} must be an array")
                    continue
                for reference in references:
                    raw = reference[1:] if isinstance(reference, str) and reference.startswith("#") else reference
                    if not isinstance(raw, str) or not IDENTIFIER_PATTERN.fullmatch(raw):
                        errors.append(f"{path}: invalid loot.{key} reference {reference!r}")
                    elif isinstance(reference, str):
                        if reference.startswith("#"):
                            namespace, resource_path = split_identifier(raw)
                            if namespace == MOD_ID and resource_path not in item_tags:
                                errors.append(
                                    f"{path}: unresolved Trading Cells item tag {reference}"
                                )
                        else:
                            validate_owned_item_reference(reference, path, definitions, errors)

            if isinstance(family, str) and isinstance(entity_type, str):
                key = family, entity_type
                previous = seen_targets.setdefault(key, path)
                if previous != path:
                    errors.append(
                        f"{path}: duplicate descriptor for {family}/{entity_type}; first is {previous}"
                    )


def validate_rei_categories(
    project_root: Path,
    roots: list[Path],
    parsed_json: dict[Path, Any],
    errors: list[str],
) -> None:
    integration = (
        project_root
        / "src/main/java/com/cosmocraft/trading_cells/platform/neoforge/integration/rei"
    )
    if not integration.is_dir():
        errors.append(f"{integration}: missing REI integration directory")
        return

    source = "\n".join(
        path.read_text(encoding="utf-8") for path in sorted(integration.rglob("*.java"))
    )
    category_paths = set(re.findall(r'\bcategory\("([a-z0-9_./-]+)"\)', source))
    if "ArcaneInfusionReiCategory" in source:
        category_paths.add("arcane_infusion")
    expected_keys = {f"category.{MOD_ID}.{path}" for path in category_paths}

    language_keys: dict[str, set[str]] = {}
    for language in ("en_us", "es_es"):
        relative = Path("assets") / MOD_ID / "lang" / f"{language}.json"
        path = find_resource(roots, relative)
        if path is None:
            continue
        document = parsed_json.get(path)
        if isinstance(document, dict):
            language_keys[language] = set(document)

    for key in sorted(expected_keys):
        if key not in source:
            errors.append(f"REI category {key} has no registered translated title")
        for language, keys in language_keys.items():
            if key not in keys:
                errors.append(f"{language}.json: missing REI category key {key}")

    for language, keys in language_keys.items():
        stale = sorted(
            key
            for key in keys
            if key.startswith(f"category.{MOD_ID}.") and key not in expected_keys
        )
        for key in stale:
            errors.append(f"{language}.json: unused REI category key {key}")


def validate_mob_farm_examples(project_root: Path, errors: list[str]) -> None:
    examples = project_root / "docs/examples/mob_farm_datapacks"
    required = {
        "valid/data/example_mod/trading_cells/mob_farm_target/ashen_skeleton.json",
        "override_low/data/example_mod/trading_cells/mob_farm_target/ashen_skeleton.json",
        "override_high/data/example_mod/trading_cells/mob_farm_target/ashen_skeleton.json",
        "invalid/data/example_mod/trading_cells/mob_farm_target/bad_schema.json",
        "fallback/README.md",
    }
    for relative in sorted(required):
        if not (examples / relative).is_file():
            errors.append(f"{examples / relative}: missing public datapack example")

    for variant in ("valid", "override_low", "override_high"):
        path = examples / variant / "data/example_mod/trading_cells/mob_farm_target/ashen_skeleton.json"
        if not path.is_file():
            continue
        try:
            document = load_json(path)
        except ValidationFailure as error:
            errors.append(str(error))
            continue
        if not isinstance(document, dict) or document.get("schema_version") != 1:
            errors.append(f"{path}: public valid example must use schema_version 1")

    invalid = examples / "invalid/data/example_mod/trading_cells/mob_farm_target/bad_schema.json"
    if invalid.is_file():
        try:
            document = load_json(invalid)
        except ValidationFailure as error:
            errors.append(str(error))
        else:
            if isinstance(document, dict) and document.get("schema_version") == 1:
                errors.append(f"{invalid}: invalid example must remain deliberately unsupported")


def validate_farmer_crop_examples(project_root: Path, errors: list[str]) -> None:
    example = (
        project_root
        / "docs/examples/farmer_crop_datapacks/data/example/trading_cells/farmer_crop/dead_bush.json"
    )
    if not example.is_file():
        errors.append(f"{example}: missing public farmer crop datapack example")
        return
    try:
        document = load_json(example)
    except ValidationFailure as error:
        errors.append(str(error))
        return
    if not isinstance(document, dict) or document.get("schema_version") != 1:
        errors.append(f"{example}: public crop example must use schema_version 1")
    if document.get("kind") not in {"villager", "piglin"}:
        errors.append(f"{example}: public crop example must declare a supported kind")


def main() -> int:
    args = parse_args()
    project_root = args.root.resolve()
    roots = resource_roots(project_root)
    errors: list[str] = []
    parsed_json: dict[Path, Any] = {}

    for path in all_files(roots, "*.json"):
        try:
            parsed_json[path] = load_json(path)
        except ValidationFailure as error:
            errors.append(str(error))

    if errors:
        print("Resource validation failed:\n - " + "\n - ".join(errors), file=sys.stderr)
        return 1

    validate_languages(roots, errors)
    validate_data_directories(roots, errors)
    validate_pickaxe_coverage(roots, parsed_json, errors)
    validate_models_and_textures(roots, parsed_json, errors)
    validate_entity_farm_particles(roots, parsed_json, errors)
    validate_png_files(roots, errors)
    validate_recipes(roots, parsed_json, errors)
    validate_entity_farm_recipes(roots, parsed_json, errors)
    validate_mob_farm_targets(roots, parsed_json, errors)
    validate_rei_categories(project_root, roots, parsed_json, errors)
    validate_mob_farm_examples(project_root, errors)
    validate_farmer_crop_examples(project_root, errors)

    if errors:
        print("Resource validation failed:\n - " + "\n - ".join(errors), file=sys.stderr)
        return 1

    print(
        "Resource validation passed: "
        f"{len(parsed_json)} JSON files, "
        f"{len(all_files(roots, '*.png'))} PNG files."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
