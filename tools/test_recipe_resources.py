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
    @classmethod
    def setUpClass(cls):
        cls.recipes = {
            path.name.removesuffix("_spawn_egg_infusion.json"): read_json(path)
            for path in sorted(RECIPES.glob("*_spawn_egg_infusion.json"))
        }

    def test_nine_single_item_slots_with_an_egg_in_the_center(self):
        for entity, recipe in self.recipes.items():
            with self.subTest(entity=entity):
                self.assertEqual(recipe["type"], "trading_cells:arcane_infusion")
                self.assertEqual(recipe["category"], "generators")
                self.assertEqual(len(recipe["ingredients"]), 9)
                self.assertEqual(recipe["ingredients"][4], {"ingredient": EGG, "count": 1})
                for index, slot in enumerate(recipe["ingredients"]):
                    self.assertEqual(set(slot), {"ingredient", "count"})
                    self.assertEqual(slot["count"], 1)
                    if index != 4:
                        self.assertTrue(slot["ingredient"].startswith("minecraft:"))
                        self.assertFalse(slot["ingredient"].endswith("_spawn_egg"))
                self.assertEqual(recipe["result"], {
                    "type": "item", "item": f"minecraft:{entity}_spawn_egg",
                })

    def test_at_most_two_thematic_materials_in_symmetric_rings(self):
        for entity, recipe in self.recipes.items():
            with self.subTest(entity=entity):
                slots = recipe["ingredients"]
                materials = {slot["ingredient"] for slot in slots} - {EGG}
                self.assertGreaterEqual(len(materials), 1)
                self.assertLessEqual(len(materials), 2)
                self.assertEqual(len({slots[i]["ingredient"] for i in CORNERS}), 1)
                self.assertEqual(len({slots[i]["ingredient"] for i in EDGES}), 1)

    def test_no_duplicate_layouts_or_reordered_ingredient_combinations(self):
        layouts, combinations = {}, {}
        for entity, recipe in self.recipes.items():
            with self.subTest(entity=entity):
                layout = tuple(slot["ingredient"] for slot in recipe["ingredients"])
                signature = ingredient_signature(recipe)
                self.assertNotIn(layout, layouts, f"Same layout as {layouts.get(layout)}")
                self.assertNotIn(signature, combinations,
                                 f"Same materials as {combinations.get(signature)}")
                layouts[layout] = entity
                combinations[signature] = entity

    def test_signature_rejects_reordering_and_cost_only_differences(self):
        original = self.recipes["axolotl"]
        changed = {**original, "experience": 9990,
                   "ingredients": list(reversed(original["ingredients"][1:]))
                   + original["ingredients"][:1]}
        self.assertEqual(ingredient_signature(original), ingredient_signature(changed))

    def test_positive_experience_in_multiples_of_ten(self):
        for entity, recipe in self.recipes.items():
            with self.subTest(entity=entity):
                self.assertIs(type(recipe["experience"]), int)
                self.assertGreater(recipe["experience"], 0)
                self.assertEqual(recipe["experience"] % 10, 0)

    def test_powerful_creatures_cost_more_than_ordinary_targets(self):
        for entity, minimum in {
            "piglin_brute": 600, "evoker": 800, "iron_golem": 800,
            "ravager": 1000, "elder_guardian": 1500,
            "warden": 5000, "wither": 10000, "ender_dragon": 20000,
        }.items():
            with self.subTest(entity=entity):
                self.assertGreaterEqual(self.recipes[entity]["experience"], minimum)
        for strong, ordinary in (
            ("elder_guardian", "guardian"), ("piglin_brute", "piglin"),
            ("ravager", "pillager"), ("wither", "wither_skeleton"),
            ("warden", "creaking"), ("ender_dragon", "enderman"),
        ):
            self.assertGreater(self.recipes[strong]["experience"],
                               self.recipes[ordinary]["experience"])

    def test_llama_and_horse_have_different_materials(self):
        self.assertNotEqual(ingredient_signature(self.recipes["llama"]),
                            ingredient_signature(self.recipes["horse"]))
        self.assertIn("minecraft:white_wool", {
            slot["ingredient"] for slot in self.recipes["llama"]["ingredients"]
        })

    def test_camel_uses_four_cactus_flowers_instead_of_cactus(self):
        ingredients = Counter(slot["ingredient"] for slot in self.recipes["camel"]["ingredients"])
        self.assertEqual(ingredients["minecraft:cactus_flower"], 4)
        self.assertNotIn("minecraft:cactus", ingredients)

    def test_reference_axolotl_and_creaking_recipes_are_preserved(self):
        for entity, corner, edge, experience in (
            ("axolotl", "clay_ball", "tropical_fish", 180),
            ("creaking", "resin_clump", "pale_moss_block", 500),
        ):
            recipe = self.recipes[entity]
            self.assertEqual(recipe["experience"], experience)
            self.assertEqual([slot["ingredient"] for slot in recipe["ingredients"]], [
                f"minecraft:{corner}", f"minecraft:{edge}", f"minecraft:{corner}",
                f"minecraft:{edge}", EGG, f"minecraft:{edge}",
                f"minecraft:{corner}", f"minecraft:{edge}", f"minecraft:{corner}",
            ])


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

    def test_current_jar_and_registration_agree_on_all_88_vanilla_eggs(self):
        self.assertEqual(self.jar_version, self.version)
        eggs = {item for item in self.items if item.endswith("_spawn_egg")}
        self.assertEqual(len(eggs), 88)
        self.assertEqual(eggs, self.registered_eggs)

    def test_exactly_one_recipe_for_every_registered_vanilla_egg(self):
        results = Counter(recipe["result"]["item"] for recipe in self.recipes)
        self.assertEqual(set(results), self.registered_eggs)
        self.assertTrue(all(count == 1 for count in results.values()))

    def test_every_spawn_egg_ingredient_exists_in_the_current_jar(self):
        for recipe in self.recipes:
            for slot in recipe["ingredients"]:
                ingredient = slot["ingredient"]
                if ingredient != EGG:
                    with self.subTest(result=recipe["result"]["item"], ingredient=ingredient):
                        self.assertIn(ingredient, self.items)


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

    def test_infuser_end_stone_is_visible_in_world_and_inventory_models(self):
        models = RESOURCE_ROOT / ASSETS / "models"
        for name, parent in (("arcane_infuser", "villager_breeder"), ("arcane_infuser_frame", "machine_cage_frame")):
            with self.subTest(model=name):
                model = read_json(models / "block" / f"{name}.json")
                self.assertEqual(model["parent"], f"trading_cells:block/{parent}")
                self.assertEqual(model["textures"]["base_center"], "minecraft:block/end_stone")
                for part in ("base", "base_left", "base_right"):
                    self.assertEqual(model["textures"][part], "minecraft:block/end_stone")
                self.assertEqual(model["textures"]["frame"], "minecraft:block/crying_obsidian")
                geometry = read_json(models / "block" / f"{parent}.json")
                center = [element for element in geometry["elements"]
                          if element["faces"].get("up", {}).get("texture") == "#base_center"]
                self.assertEqual(len(center), 1)
                self.assertEqual(center[0]["from"], [6, 0, 2])
                self.assertEqual(center[0]["to"], [10, 2, 14])
        self.assertEqual(read_json(models / "item/arcane_infuser.json")["parent"], "trading_cells:block/arcane_infuser")

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
                                  ("green_concrete", "lapis_lazuli", "diamond", "netherite_ingot")):
            grid = crafting_grid(read_json(RECIPES / f"{name}_creature_model_base.json"))
            self.assertEqual([grid[i] for i in CORNERS], [f"minecraft:{material}"] * 4)
            self.assertEqual([grid[i] for i in EDGES], ["minecraft:black_concrete"] * 4)
            self.assertEqual(grid[4], "trading_cells:storm_shard")
        for family in ("speed", "capacity"):
            previous = "minecraft:diamond_sword"
            for material in ("copper", "iron", "gold", "diamond", "netherite"):
                name = f"mob_farm_{family}_{material}_upgrade"
                recipe = read_json(RECIPES / f"{name}.json")
                if material == "netherite":
                    self.assertEqual(recipe["type"], "minecraft:smithing_transform")
                    self.assertEqual(recipe["template"], "minecraft:netherite_upgrade_smithing_template")
                    self.assertEqual(recipe["base"], previous)
                    self.assertEqual(recipe["addition"], "minecraft:netherite_block")
                else:
                    grid = crafting_grid(recipe)
                    corner = "popped_chorus_fruit" if material == "diamond" else "chest" if family == "capacity" else "clock"
                    self.assertEqual([grid[i] for i in CORNERS], [f"minecraft:{corner}"] * 4)
                    self.assertEqual([grid[i] for i in EDGES], [f"minecraft:{material}_block"] * 4)
                    self.assertEqual(grid[4], previous)
                previous = f"trading_cells:{name}"

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

    def test_active_catalog_passes(self):
        self.assertEqual(self.validate(), [])

    def test_removed_farm_recipes_remain_absent(self):
        for name in ("skeleton", "zombie", "creeper", "raider", "arthropod", "slime", "guardian", "piglin",
                     "blaze", "ghast", "enderman", "shulker", "breeze", "phantom", "livestock", "fish",
                     "aquatic", "mount", "amphibian", "bee", "creaking"):
            with self.subTest(farm=name):
                self.assertFalse((RECIPES / f"{name}_farm_infusion.json").exists())

    def test_general_farm_layout_and_cost_are_checked(self):
        for field, value in (("experience", 1), ("result", {"type": "item", "item": "trading_cells:skeleton_farm"})):
            recipe = read_json(RECIPES / "mob_farm_infusion.json")
            recipe[field] = value
            self.assertTrue(any("simulation recipe" in error for error in self.validate({"mob_farm_infusion.json": recipe})))

    def test_spawn_egg_center_cost_and_duplicates_are_checked(self):
        name = "spider_spawn_egg_infusion.json"
        original = read_json(RECIPES / name)
        wrong_center = deepcopy(original)
        wrong_center["ingredients"][4]["ingredient"] = "minecraft:egg"
        invalid_cost = {**original, "experience": 15}
        duplicate = {**original, "ingredients": read_json(RECIPES / "cow_spawn_egg_infusion.json")["ingredients"]}
        for recipe, expected in ((wrong_center, "center slot"), (invalid_cost, "multiple of ten"), (duplicate, "duplicate")):
            self.assertTrue(any(expected in error for error in self.validate({name: recipe})))


if __name__ == "__main__":
    unittest.main()
