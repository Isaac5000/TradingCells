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
ENTITY_FARM_RECIPE_BASES = {
    key: value.replace("minecraft:block/", "minecraft:")
    for key, value in ENTITY_FARM_BASE_TEXTURES.items()
}
ENTITY_FARM_RECIPE_BASES["slime_farm"] = "minecraft:slime_block"
ENTITY_FARM_RECIPE_BASES["fish_farm"] = "minecraft:sand"
ENTITY_FARM_RECIPE_BASES["livestock_farm"] = "minecraft:hay_block"
CONFIGURED_FARM_UPPER_INGREDIENTS = {
    "arthropod_farm": (
        "minecraft:cave_spider_spawn_egg",
        "minecraft:string",
        "minecraft:spider_spawn_egg",
        "minecraft:endermite_spawn_egg",
        "minecraft:silverfish_spawn_egg",
    ),
    "blaze_farm": (
        "minecraft:blaze_powder",
        "minecraft:blaze_spawn_egg",
        "minecraft:blaze_powder",
        "minecraft:blaze_rod",
        "minecraft:blaze_rod",
    ),
    "breeze_farm": (
        "minecraft:wind_charge",
        "minecraft:breeze_spawn_egg",
        "minecraft:wind_charge",
        "minecraft:breeze_rod",
        "minecraft:breeze_rod",
    ),
    "enderman_farm": (
        "minecraft:chorus_fruit",
        "minecraft:enderman_spawn_egg",
        "minecraft:chorus_fruit",
        "minecraft:ender_pearl",
        "minecraft:ender_pearl",
    ),
    "ghast_farm": (
        "minecraft:ghast_tear",
        "minecraft:fire_charge",
        "minecraft:ghast_tear",
        "minecraft:ghast_spawn_egg",
        "minecraft:happy_ghast_spawn_egg",
    ),
    "guardian_farm": (
        "minecraft:prismarine_crystals",
        "minecraft:heart_of_the_sea",
        "minecraft:prismarine_crystals",
        "minecraft:elder_guardian_spawn_egg",
        "minecraft:guardian_spawn_egg",
    ),
    "fish_farm": (
        "minecraft:cod_spawn_egg",
        "minecraft:fishing_rod",
        "minecraft:tropical_fish_spawn_egg",
        "minecraft:pufferfish_spawn_egg",
        "minecraft:salmon_spawn_egg",
    ),
    "livestock_farm": (
        "minecraft:cow_spawn_egg",
        "minecraft:sheep_spawn_egg",
        "minecraft:pig_spawn_egg",
        "minecraft:chicken_spawn_egg",
        "minecraft:rabbit_spawn_egg",
    ),
    "phantom_farm": (
        "minecraft:phantom_membrane",
        "minecraft:phantom_spawn_egg",
        "minecraft:phantom_membrane",
        "minecraft:phantom_membrane",
        "minecraft:phantom_membrane",
    ),
    "piglin_farm": (
        "minecraft:gold_ingot",
        "minecraft:crossbow",
        "minecraft:gold_ingot",
        "minecraft:piglin_brute_spawn_egg",
        "minecraft:piglin_spawn_egg",
    ),
    "shulker_farm": (
        "minecraft:end_rod",
        "minecraft:shulker_spawn_egg",
        "minecraft:end_rod",
        "minecraft:shulker_shell",
        "minecraft:shulker_shell",
    ),
    "slime_farm": (
        "minecraft:slime_ball",
        "minecraft:sulfur_cube_spawn_egg",
        "minecraft:slime_ball",
        "minecraft:magma_cube_spawn_egg",
        "minecraft:slime_spawn_egg",
    ),
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
    for block_id in sorted(ENTITY_FARM_BLOCKS):
        recipe_path = find_resource(
            roots, Path(f"data/trading_cells/recipe/{block_id}_infusion.json")
        )
        if recipe_path is None:
            errors.append(f"missing entity-farm infusion recipe: {block_id}")
            continue
        recipe = parsed_json.get(recipe_path)
        ingredients = recipe.get("ingredients") if isinstance(recipe, dict) else None
        if not isinstance(ingredients, list) or len(ingredients) != 9:
            errors.append(f"{recipe_path}: entity-farm infusion must contain nine slots")
            continue

        ingredient_ids = [
            entry.get("ingredient") if isinstance(entry, dict) else None
            for entry in ingredients
        ]
        if ingredient_ids[4] != "trading_cells:experience_storage":
            errors.append(f"{recipe_path}: center slot must contain Experience Storage")
        if ingredient_ids[6] != "minecraft:spawner":
            errors.append(f"{recipe_path}: bottom-left slot must contain a Spawner")
        if ingredient_ids[8] != "minecraft:iron_block":
            errors.append(f"{recipe_path}: bottom-right slot must contain an Iron Block")
        expected_base = ENTITY_FARM_RECIPE_BASES[block_id]
        if ingredient_ids[7] != expected_base:
            errors.append(f"{recipe_path}: bottom base must contain {expected_base}")

        expected_upper = CONFIGURED_FARM_UPPER_INGREDIENTS.get(block_id)
        if expected_upper is not None:
            actual_upper = tuple(ingredient_ids[index] for index in (0, 1, 2, 3, 5))
            if actual_upper != expected_upper:
                errors.append(
                    f"{recipe_path}: configured-farm upper ingredients do not match its family pattern"
                )

        if block_id == "creeper_farm":
            upper_corners = ingredient_ids[0], ingredient_ids[2]
            if upper_corners != ("minecraft:gunpowder", "minecraft:gunpowder"):
                errors.append(f"{recipe_path}: both upper corners must contain gunpowder")

    configured_generator_patterns = {
        "spider_spawn_egg_infusion": (
            (
                "minecraft:string",
                "minecraft:fermented_spider_eye",
                "minecraft:string",
                "minecraft:spider_eye",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:spider_eye",
                "minecraft:string",
                "minecraft:fermented_spider_eye",
                "minecraft:string",
            ),
            "minecraft:spider_spawn_egg",
        ),
        "cave_spider_spawn_egg_infusion": (
            (
                "minecraft:string",
                "minecraft:poisonous_potato",
                "minecraft:string",
                "minecraft:spider_eye",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:spider_eye",
                "minecraft:string",
                "minecraft:poisonous_potato",
                "minecraft:string",
            ),
            "minecraft:cave_spider_spawn_egg",
        ),
        "endermite_spawn_egg_infusion": (
            (
                "minecraft:end_stone",
                "minecraft:ender_pearl",
                "minecraft:end_stone",
                "minecraft:purpur_block",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:purpur_block",
                "minecraft:end_stone",
                "minecraft:chorus_fruit",
                "minecraft:end_stone",
            ),
            "minecraft:endermite_spawn_egg",
        ),
        "silverfish_spawn_egg_infusion": (
            (
                "minecraft:stone",
                "minecraft:stone",
                "minecraft:stone",
                "minecraft:stone",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:stone",
                "minecraft:stone",
                "minecraft:stone",
                "minecraft:stone",
            ),
            "minecraft:silverfish_spawn_egg",
        ),
        "slime_spawn_egg_infusion": (
            (
                "minecraft:slime_ball",
                "minecraft:slime_ball",
                "minecraft:slime_ball",
                "minecraft:slime_ball",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:slime_ball",
                "minecraft:slime_ball",
                "minecraft:slime_ball",
                "minecraft:slime_ball",
            ),
            "minecraft:slime_spawn_egg",
        ),
        "sulfur_cube_spawn_egg_infusion": (
            (
                "minecraft:slime_ball",
                "minecraft:sulfur",
                "minecraft:slime_ball",
                "minecraft:sulfur",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:sulfur",
                "minecraft:slime_ball",
                "minecraft:sulfur",
                "minecraft:slime_ball",
            ),
            "minecraft:sulfur_cube_spawn_egg",
        ),
        "magma_cube_spawn_egg_infusion": (
            (
                "minecraft:magma_block",
                "minecraft:magma_block",
                "minecraft:magma_block",
                "minecraft:magma_cream",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:magma_cream",
                "minecraft:magma_block",
                "minecraft:magma_block",
                "minecraft:magma_block",
            ),
            "minecraft:magma_cube_spawn_egg",
        ),
        "guardian_spawn_egg_infusion": (
            (
                "minecraft:prismarine_shard",
                "minecraft:prismarine_crystals",
                "minecraft:prismarine_shard",
                "minecraft:cod",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:cod",
                "minecraft:prismarine_shard",
                "minecraft:prismarine_crystals",
                "minecraft:prismarine_shard",
            ),
            "minecraft:guardian_spawn_egg",
        ),
        "elder_guardian_spawn_egg_infusion": (
            (
                "minecraft:prismarine_crystals",
                "minecraft:wet_sponge",
                "minecraft:prismarine_crystals",
                "minecraft:prismarine_shard",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:prismarine_shard",
                "minecraft:prismarine_crystals",
                "minecraft:wet_sponge",
                "minecraft:prismarine_crystals",
            ),
            "minecraft:elder_guardian_spawn_egg",
        ),
        "piglin_brute_spawn_egg_infusion": (
            (
                "minecraft:golden_axe",
                "minecraft:gold_block",
                "minecraft:golden_axe",
                "minecraft:bone_block",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:bone_block",
                "minecraft:blackstone",
                "minecraft:blackstone",
                "minecraft:blackstone",
            ),
            "minecraft:piglin_brute_spawn_egg",
        ),
        "blaze_spawn_egg_infusion": (
            (
                "minecraft:blaze_rod",
                "minecraft:blaze_powder",
                "minecraft:blaze_rod",
                "minecraft:blaze_powder",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:blaze_powder",
                "minecraft:blaze_rod",
                "minecraft:blaze_powder",
                "minecraft:blaze_rod",
            ),
            "minecraft:blaze_spawn_egg",
        ),
        "enderman_spawn_egg_infusion": (
            (
                "minecraft:ender_pearl",
                "minecraft:crying_obsidian",
                "minecraft:ender_pearl",
                "minecraft:crying_obsidian",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:crying_obsidian",
                "minecraft:ender_pearl",
                "minecraft:crying_obsidian",
                "minecraft:ender_pearl",
            ),
            "minecraft:enderman_spawn_egg",
        ),
        "shulker_spawn_egg_infusion": (
            (
                "minecraft:chorus_fruit",
                "minecraft:shulker_shell",
                "minecraft:chorus_fruit",
                "minecraft:end_stone",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:end_stone",
                "minecraft:chorus_fruit",
                "minecraft:shulker_shell",
                "minecraft:chorus_fruit",
            ),
            "minecraft:shulker_spawn_egg",
        ),
        "breeze_spawn_egg_infusion": (
            (
                "minecraft:breeze_rod",
                "minecraft:wind_charge",
                "minecraft:breeze_rod",
                "minecraft:wind_charge",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:wind_charge",
                "minecraft:breeze_rod",
                "minecraft:wind_charge",
                "minecraft:breeze_rod",
            ),
            "minecraft:breeze_spawn_egg",
        ),
        "phantom_spawn_egg_infusion": (
            (
                "minecraft:phantom_membrane",
                "minecraft:phantom_membrane",
                "minecraft:phantom_membrane",
                "minecraft:phantom_membrane",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:phantom_membrane",
                "minecraft:phantom_membrane",
                "minecraft:phantom_membrane",
                "minecraft:phantom_membrane",
            ),
            "minecraft:phantom_spawn_egg",
        ),
        "ghast_spawn_egg_infusion": (
            (
                "minecraft:ghast_tear",
                "minecraft:fire_charge",
                "minecraft:ghast_tear",
                "minecraft:gunpowder",
                "#trading_cells:arcane_infusion_eggs",
                "minecraft:gunpowder",
                "minecraft:soul_sand",
                "minecraft:lava_bucket",
                "minecraft:soul_sand",
            ),
            "minecraft:ghast_spawn_egg",
        ),
        "happy_ghast_spawn_egg_infusion": (
            (
                "minecraft:ghast_tear",
                "minecraft:snowball",
                "minecraft:ghast_tear",
                "minecraft:white_wool",
                "minecraft:dried_ghast",
                "minecraft:white_wool",
                "minecraft:sand",
                "minecraft:water_bucket",
                "minecraft:sand",
            ),
            "minecraft:happy_ghast_spawn_egg",
        ),
    }
    for recipe_id, (expected_ingredients, result_item) in configured_generator_patterns.items():
        recipe_path = find_resource(
            roots, Path(f"data/trading_cells/recipe/{recipe_id}.json")
        )
        recipe = parsed_json.get(recipe_path) if recipe_path is not None else None
        ingredients = recipe.get("ingredients") if isinstance(recipe, dict) else None
        result = recipe.get("result") if isinstance(recipe, dict) else None
        if not isinstance(ingredients, list) or len(ingredients) != 9:
            errors.append(f"{recipe_path}: configured generator infusion must contain nine slots")
            continue
        ingredient_ids = tuple(
            entry.get("ingredient") if isinstance(entry, dict) else None
            for entry in ingredients
        )
        if ingredient_ids != expected_ingredients:
            errors.append(f"{recipe_path}: configured generator ingredients do not match its pattern")
        if not isinstance(result, dict) or result.get("item") != result_item:
            errors.append(f"{recipe_path}: result must be {result_item}")


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
