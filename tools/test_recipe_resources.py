"""Recipe regressions; run with python -B -m unittest discover -s tools -p test_recipe_resources.py.

Coverage uses the current client JAR and the sources from createMinecraftArtifacts.
MINECRAFT_CLIENT_JAR / MINECRAFT_SOURCES_JAR can override the local Gradle paths.
"""

from collections import Counter
from copy import deepcopy
import json
import os
from pathlib import Path
import re
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from generate_logistics_resources import ASSETS, DATA, resources
import validate_project_resources as validator


ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = ROOT / "src/main/resources"
RECIPES = RESOURCE_ROOT / DATA / "recipe"
EGG = "#trading_cells:arcane_infusion_eggs"
PIPE_MATERIALS = {
    "copper": "copper_ingot", "iron": "iron_ingot", "gold": "gold_ingot",
    "diamond": "diamond", "netherite": "netherite_ingot",
}
CORNERS = (0, 2, 6, 8)
EDGES = (1, 3, 5, 7)


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def crafting_grid(recipe):
    return [recipe["key"].get(symbol) for row in recipe["pattern"] for symbol in row]


def ingredient_signature(recipe):
    # Ignore XP, output and slot order: shuffling the same materials is not a new recipe.
    return tuple(sorted(Counter(
        slot["ingredient"] for slot in recipe["ingredients"]
    ).items()))


class SpawnEggRecipeTests(unittest.TestCase):
    def test_retired_recipes_are_recoverable_except_bosses(self):
        archive = ROOT / "docs/archive/arcane_infusions"
        archived = {path.stem for path in archive.glob("*_spawn_egg_infusion.json")}
        # Check the archive's purpose, not its historical item count: Minecraft
        # may add or remove spawn eggs without making the archive invalid.
        self.assertTrue(archived, "The recoverable spawn-egg recipe archive must not disappear")
        for name in archived:
            path = archive / (name + ".json")
            recipe = read_json(path)
            with self.subTest(recipe=name):
                self.assertEqual(recipe.get("type"), "trading_cells:arcane_infusion")
                self.assertTrue(recipe.get("result", {}).get("item", "").endswith("_spawn_egg"))
                self.assertFalse((RECIPES / path.name).exists(), "Archived recipes must not load")
        for name in ("wither_spawn_egg_infusion", "ender_dragon_spawn_egg_infusion"):
            self.assertNotIn(name, archived)
            self.assertFalse((RECIPES / (name + ".json")).exists())


class CurrentMinecraftRegistryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        properties = dict(
            line.strip().split("=", 1)
            for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
            if "=" in line and not line.lstrip().startswith("#")
        )
        cls.version = properties["minecraft_version"].removesuffix(".0")
        gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
        client = Path(os.environ.get("MINECRAFT_CLIENT_JAR",
            gradle_home / "caches/neoformruntime/artifacts" / f"minecraft_{cls.version}_client.jar"))
        sources = Path(os.environ.get("MINECRAFT_SOURCES_JAR",
            ROOT / "build/moddev/artifacts" / f"minecraft-patched-{properties['neo_version']}-sources.jar"))
        for path in (client, sources):
            if not path.is_file():
                raise FileNotFoundError(f"Current Minecraft JAR missing: {path}. "
                                        "Run createMinecraftArtifacts or set the JAR overrides.")
        with ZipFile(client) as jar:
            cls.jar_version = json.loads(jar.read("version.json"))["id"]
            cls.items = {
                "minecraft:" + Path(name).stem for name in jar.namelist()
                if name.startswith("assets/minecraft/items/") and name.endswith(".json")
            }
        with ZipFile(sources) as jar:
            source = jar.read("net/minecraft/world/item/Items.java").decode("utf-8")
        cls.registered_eggs = {
            "minecraft:" + name.lower() for name in re.findall(
                r"\bregisterSpawnEgg\(\s*ItemIds\.([A-Z_]+_SPAWN_EGG)\s*,", source
            )
        }
        cls.recipes = [read_json(path) for path in RECIPES.glob("*_spawn_egg_infusion.json")]

    def test_current_jar_and_registration_agree_on_all_vanilla_eggs(self):
        self.assertEqual(self.jar_version, self.version)
        eggs = {item for item in self.items if item.endswith("_spawn_egg")}
        self.assertTrue(eggs, "The current Minecraft client must contain spawn egg items")
        self.assertEqual(eggs, self.registered_eggs)

    def test_active_catalog_has_no_spawn_egg_infusions(self):
        self.assertEqual(self.recipes, [])


class SpecificRecipeTests(unittest.TestCase):
    def test_piglin_capturer_replaces_exactly_two_iron_ingots(self):
        recipe = read_json(RECIPES / "piglin_capturer.json")
        self.assertEqual(crafting_grid(recipe), [
            "minecraft:gold_ingot", "minecraft:blackstone", "minecraft:gold_ingot",
            "minecraft:glass", "minecraft:ender_pearl", "minecraft:glass",
            "minecraft:gold_ingot", "minecraft:blackstone", "minecraft:gold_ingot",
        ])

    def test_copper_barter_upgrade_has_gold_in_the_center(self):
        recipe = read_json(RECIPES / "piglin_barter_copper_upgrade.json")
        self.assertEqual(crafting_grid(recipe), [
            "minecraft:copper_nugget", "minecraft:copper_ingot", "minecraft:copper_nugget",
            "minecraft:copper_ingot", "minecraft:gold_ingot", "minecraft:copper_ingot",
            "minecraft:copper_nugget", "minecraft:copper_ingot", "minecraft:copper_nugget",
        ])

    def test_diamond_quarry_upgrade_has_four_diamond_blocks_and_chorus_corners(self):
        grid = crafting_grid(read_json(RECIPES / "quarry_diamond_upgrade.json"))
        self.assertEqual([grid[i] for i in CORNERS], ["minecraft:popped_chorus_fruit"] * 4)
        self.assertEqual([grid[i] for i in EDGES], ["minecraft:diamond_block"] * 4)
        self.assertEqual(grid[4], "trading_cells:quarry_gold_upgrade")

    def test_quarry_upgrade_starts_with_an_iron_pickaxe_and_keeps_the_tier_chain(self):
        materials = ("copper", "iron", "gold", "diamond")
        previous = "minecraft:iron_pickaxe"
        for material in materials:
            grid = crafting_grid(read_json(RECIPES / f"quarry_{material}_upgrade.json"))
            self.assertEqual(grid[4], previous)
            previous = f"trading_cells:quarry_{material}_upgrade"
        recipe = read_json(RECIPES / "quarry_netherite_upgrade.json")
        self.assertEqual(recipe["base"], previous)

    def test_infuser_bottom_center_is_end_stone_and_storage_must_be_empty(self):
        grid = crafting_grid(read_json(RECIPES / "arcane_infuser.json"))
        self.assertEqual(grid[7], "minecraft:end_stone")
        self.assertEqual(grid[1], "minecraft:enchanting_table")
        for index in (0, 2, 3, 5, 6, 8):
            self.assertEqual(grid[index], "minecraft:crying_obsidian")
        self.assertEqual(grid[4], {
            "neoforge:ingredient_type": "neoforge:components",
            "items": "trading_cells:experience_storage",
            "components": {"!minecraft:block_entity_data": {}},
        })

    def test_essence_machine_recipes(self):
        grid = crafting_grid(read_json(RECIPES / "essence_workbench.json"))
        self.assertEqual(grid, ["minecraft:lapis_block", "minecraft:black_concrete", "minecraft:lapis_block",
                                "minecraft:black_concrete", "trading_cells:experience_storage", "minecraft:black_concrete",
                                "minecraft:black_concrete", "trading_cells:storm_shard", "minecraft:black_concrete"])
        self.assertEqual(crafting_grid(read_json(RECIPES / "essence_stabilizer.json")), [
            "minecraft:lapis_block", "minecraft:glass", "minecraft:lapis_block",
            "minecraft:black_concrete", "minecraft:diamond_block", "minecraft:black_concrete",
            "minecraft:lapis_block", "minecraft:black_concrete", "minecraft:lapis_block"])
        vial = read_json(RECIPES / "empty_essence_vial.json")
        self.assertEqual(vial["pattern"], ["A", "G", "G"])
        self.assertEqual(vial["key"], {"A": "minecraft:amethyst_shard", "G": "minecraft:glass"})
        self.assertEqual(vial["result"]["count"], 4)
        farm = read_json(RECIPES / "mob_farm_infusion.json")
        self.assertEqual(farm["ingredients"], [{"ingredient": item, "count": 1} for item in (
            "minecraft:lapis_block", "minecraft:black_concrete", "minecraft:lapis_block",
            "minecraft:black_concrete", "trading_cells:experience_storage", "minecraft:black_concrete",
            "minecraft:lapis_block", "trading_cells:storm_shard", "minecraft:lapis_block")])
        self.assertEqual(farm["experience"], 50_000)

    def test_interaction_farm_exact_infusion(self):
        recipe = read_json(RECIPES / "entity_interaction_farm_infusion.json")
        self.assertEqual(recipe["type"], "trading_cells:arcane_infusion")
        self.assertEqual(recipe["experience"], 50_000)
        self.assertEqual(recipe["ingredients"], [{"ingredient": "minecraft:" + item, "count": 1} for item in (
            "lapis_block", "black_concrete", "lapis_block", "black_concrete", "dispenser",
            "black_concrete", "lapis_block", "black_concrete", "lapis_block")])
        self.assertEqual(recipe["result"], {"type": "item", "item": "trading_cells:entity_interaction_farm"})

    def test_essence_stabilization_and_bottle_costs(self):
        for tier, reagent in enumerate(("redstone", "glowstone_dust", "ender_pearl", "dragon_breath"), 1):
            recipe = read_json(RECIPES / f"essence_stabilization_{tier}.json")
            self.assertEqual(recipe["tier"], tier)
            self.assertEqual(recipe["duration"], 100)
            self.assertEqual(recipe["amethyst"], {"ingredient": "minecraft:amethyst_shard", "count": 2 ** (tier - 1)})
            self.assertEqual(recipe["reagent"], {"ingredient": f"minecraft:{reagent}", "count": 2})
        bottle = read_json(RECIPES / "experience_bottle_infusion.json")
        self.assertEqual(bottle["experience"], 11)
        self.assertTrue(bottle["shapeless"])
        self.assertEqual(bottle["ingredients"], [{"ingredient": "minecraft:glass_bottle", "count": 1} if i == 4
                                                else {"empty": True} for i in range(9)])

    def test_essence_bases_and_progressive_upgrades(self):
        for name, material in zip(("tier_i", "tier_ii", "tier_iii", "tier_iv"),
                                  ("emerald", "diamond", "netherite_ingot", "nether_star")):
            grid = crafting_grid(read_json(RECIPES / f"{name}_creature_model_base.json"))
            self.assertEqual([grid[i] for i in CORNERS], [f"minecraft:{material}"] * 4)
            self.assertEqual([grid[i] for i in EDGES], ["minecraft:black_concrete"] * 4)
            expected_center = {
                "tier_i": "trading_cells:storm_shard",
                "tier_ii": "trading_cells:tier_i_creature_model_base",
                "tier_iii": "trading_cells:tier_ii_creature_model_base",
                "tier_iv": "trading_cells:tier_iii_creature_model_base",
            }[name]
            self.assertEqual(grid[4], expected_center)
        for family in ("speed", "quantity"):
            previous = "minecraft:clock" if family == "speed" else "minecraft:bundle"
            prefix = family
            for material in ("copper", "iron", "gold", "diamond", "netherite"):
                name = f"{prefix}_{material}_upgrade"
                recipe = read_json(RECIPES / f"{name}.json")
                if material == "netherite":
                    self.assertEqual(recipe["type"], "minecraft:smithing_transform")
                    self.assertEqual(recipe["template"], "minecraft:netherite_upgrade_smithing_template")
                    self.assertEqual(recipe["base"], previous)
                    self.assertEqual(recipe["addition"], "minecraft:netherite_block")
                else:
                    grid = crafting_grid(recipe)
                    corner = "popped_chorus_fruit" if material == "diamond" else f"{material}_block"
                    self.assertEqual([grid[i] for i in CORNERS], [f"minecraft:{corner}"] * 4)
                    self.assertEqual([grid[i] for i in EDGES], [f"minecraft:{material}_block"] * 4)
                    self.assertEqual(grid[4], previous)
                previous = f"trading_cells:{name}"

    def test_syringe_recipe_has_iron_barrel_lapis_and_concrete_grip(self):
        recipe = read_json(RECIPES / "essence_extractor.json")
        self.assertEqual(recipe["pattern"], ["III", " LB", "  B"])
        self.assertEqual(recipe["key"], {"I": "minecraft:iron_ingot", "L": "minecraft:lapis_lazuli", "B": "minecraft:black_concrete"})
        self.assertEqual(recipe["result"], {"id": "trading_cells:essence_extractor", "count": 1})

    def test_all_pipe_tiers_use_their_material_and_keep_the_upgrade_chain(self):
        generated = resources()
        previous = "minecraft:hopper"
        for tier, material in PIPE_MATERIALS.items():
            name = f"{tier}_pipe_upgrade"
            relative = f"{DATA}/recipe/{name}.json"
            with self.subTest(tier=tier):
                recipe = read_json(RESOURCE_ROOT / relative)
                self.assertEqual((RESOURCE_ROOT / relative).read_bytes(), generated[relative])
                if tier == "netherite":
                    self.assertEqual(recipe["type"], "minecraft:smithing_transform")
                    self.assertEqual(recipe["template"], "minecraft:netherite_upgrade_smithing_template")
                    self.assertEqual(recipe["base"], previous)
                    self.assertEqual(recipe["addition"], "minecraft:netherite_ingot")
                    self.assertEqual(recipe["result"], {"id": f"trading_cells:{name}"})
                else:
                    grid = crafting_grid(recipe)
                    corner = "popped_chorus_fruit" if tier == "diamond" else material
                    self.assertEqual([grid[i] for i in CORNERS], [f"minecraft:{corner}"] * 4)
                    self.assertEqual([grid[i] for i in EDGES], [f"minecraft:{material}"] * 4)
                    self.assertEqual(grid[4], previous)
                    self.assertEqual(recipe["result"], {"id": f"trading_cells:{name}", "count": 1})
                self.assertIs(recipe["show_notification"], False)
            previous = f"trading_cells:{name}"

    def test_terminal_inventory_models_use_the_full_3d_block(self):
        generated = resources()
        for name in ("network_terminal", "network_crafting_terminal"):
            with self.subTest(terminal=name):
                for relative, expected in (
                    (f"{ASSETS}/items/{name}.json", {"model": {
                        "type": "minecraft:model", "model": f"trading_cells:block/{name}",
                    }}),
                    (f"{ASSETS}/models/item/{name}.json", {"parent": f"trading_cells:block/{name}"}),
                ):
                    self.assertEqual(read_json(RESOURCE_ROOT / relative), expected)
                    self.assertEqual(json.loads(generated[relative]), expected)
                block = read_json(RESOURCE_ROOT / ASSETS / f"models/block/{name}.json")
                self.assertEqual(block, json.loads(generated[f"{ASSETS}/models/block/{name}.json"]))
                housing, panel = block["elements"]
                self.assertEqual(housing["from"], [0, 0, 0])
                self.assertEqual(housing["to"], [16, 16, 16])
                self.assertEqual(set(panel["faces"]), {"up"})


class SimulationRecipeValidationTests(unittest.TestCase):
    def validate(self, changes=None):
        parsed = {path: read_json(path) for path in RECIPES.glob("*.json")}
        for name, value in (changes or {}).items():
            parsed[RECIPES / name] = value
        errors = []
        with patch.object(validator, "find_resource", side_effect=lambda roots, relative:
                          RESOURCE_ROOT / relative if RESOURCE_ROOT / relative in parsed else None):
            validator.validate_entity_farm_recipes([RESOURCE_ROOT], parsed, errors)
        return errors

    def test_removed_farm_recipes_remain_absent(self):
        for name in ("skeleton", "zombie", "creeper", "raider", "arthropod", "slime", "guardian", "piglin",
                     "blaze", "ghast", "enderman", "shulker", "breeze", "phantom", "livestock", "fish",
                     "aquatic", "mount", "amphibian", "bee", "creaking"):
            with self.subTest(farm=name):
                self.assertFalse((RECIPES / f"{name}_farm_infusion.json").exists())

    def test_general_farm_layout_and_cost_are_checked(self):
        self.assertEqual(self.validate(), [], "The real recipe catalog must pass validation")
        for field, value in (("experience", 1), ("result", {"type": "item", "item": "trading_cells:skeleton_farm"})):
            recipe = read_json(RECIPES / "mob_farm_infusion.json")
            recipe[field] = value
            self.assertTrue(any("simulation recipe" in error for error in self.validate({"mob_farm_infusion.json": recipe})))

    def test_spawn_egg_infusions_cannot_be_restored_accidentally(self):
        path = "cow_spawn_egg_infusion.json"
        recipe = read_json(ROOT / "docs/archive/arcane_infusions" / path)
        self.assertTrue(any("creature generator" in error for error in self.validate({path: recipe})))


if __name__ == "__main__":
    unittest.main()