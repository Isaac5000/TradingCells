"""Regression checks for portable generated-resource comparisons."""

from io import BytesIO
import hashlib
import json
from pathlib import Path
import unittest

from PIL import Image
from PIL.PngImagePlugin import PngInfo

from generate_logistics_resources import ASSETS, resource_matches, resources
from generate_mob_simulation_resources import outlined_union, resources as simulation_resources
from generate_pipe_textures import TEXTURES as PIPE_TEXTURES, blend
from generate_family_upgrades import BASES, FAMILIES, MATERIALS, MOB_FARM_SWORD_REGIONS, TERMINAL_BODY, family_base, family_texture, generated, luminance, material_ramp, recolor, rivet_pixels
from generate_family_upgrades import WOOD_PALETTE, fixed_emblem_pixels, wooden_emblem_pixels
from generate_family_upgrades import ORIGINALS, FRAMES, emblem_pixels, quarry_frame, generic_frame
from generate_family_upgrades import TERMINALS, TERMINAL_ORIGINALS, TERMINAL_STEEL, logistics_terminal_texture


def encoded(image: Image.Image, **options) -> bytes:
    output = BytesIO()
    image.save(output, format="PNG", **options)
    return output.getvalue()


class LogisticsResourceTests(unittest.TestCase):
    def setUp(self):
        self.image = Image.new("RGBA", (2, 2), (10, 20, 30, 255))
        self.expected = encoded(self.image)

    def test_generated_tools_allow_different_png_compression(self):
        generated = resources()
        for name in ("pipe_wrench", "pipe_target_selector"):
            with self.subTest(texture=name):
                relative = f"{ASSETS}/textures/item/{name}.png"
                expected = generated[relative]
                with Image.open(BytesIO(expected)) as image:
                    actual = encoded(image, compress_level=0)
                self.assertNotEqual(actual, expected)
                self.assertTrue(resource_matches(relative, actual, expected))

    def test_png_text_metadata_does_not_change_pixels(self):
        metadata = PngInfo()
        metadata.add_text("generator", "another platform")
        actual = encoded(self.image, pnginfo=metadata)
        self.assertNotEqual(actual, self.expected)
        self.assertTrue(resource_matches("sprite.png", actual, self.expected))

    def test_rgb_change_with_unchanged_alpha_is_rejected(self):
        self.image.putpixel((0, 0), (11, 20, 30, 255))
        self.assertFalse(resource_matches("sprite.png", encoded(self.image), self.expected))

    def test_alpha_change_is_rejected(self):
        self.image.putpixel((0, 0), (10, 20, 30, 254))
        self.assertFalse(resource_matches("sprite.png", encoded(self.image), self.expected))

    def test_dimensions_must_match(self):
        resized = self.image.resize((1, 4))
        self.assertFalse(resource_matches("sprite.png", encoded(resized), self.expected))

    def test_animation_is_rejected_for_a_static_texture(self):
        actual = encoded(self.image, save_all=True,
                         append_images=[Image.new("RGBA", (2, 2), (40, 50, 60, 255))])
        self.assertFalse(resource_matches("sprite.png", actual, self.expected))

    def test_non_png_image_is_rejected(self):
        output = BytesIO()
        self.image.save(output, format="TIFF")
        self.assertFalse(resource_matches("sprite.png", output.getvalue(), self.expected))

    def test_corrupt_png_is_rejected(self):
        self.assertFalse(resource_matches("sprite.png", b"not a PNG", self.expected))

    def test_json_still_requires_exact_bytes(self):
        self.assertTrue(resource_matches("model.json", b'{"a":1}', b'{"a":1}'))
        self.assertFalse(resource_matches("model.json", b'{"a":2}', b'{"a":1}'))
        self.assertFalse(resource_matches("model.json", b'{"a": 1}', b'{"a":1}'))


class ItemSpriteTests(unittest.TestCase):
    def test_capturers_keep_a_symmetric_outline_and_structural_star_handle(self):
        root = Path(__file__).resolve().parents[1] / f"src/main/resources/{ASSETS}/textures/item"
        for prefix in ("", "unbreakable_"):
            bounds = []
            for mob in ("villager", "piglin"):
                with Image.open(root / f"{prefix}{mob}_capturer.png") as image:
                    self.assertEqual(image.size, (64, 64))
                    alpha = image.convert("RGBA").getchannel("A")
                self.assertEqual(set(alpha.get_flattened_data()), {0, 255})
                bounds.append(alpha.getbbox())
                asymmetric = sum(alpha.getpixel((x, y)) != alpha.getpixel((63 - x, y))
                                 for x in range(32) for y in range(64))
                self.assertLessEqual(asymmetric, 8)
                if prefix:
                    self.assertEqual(alpha.getpixel((31, 10)), 0, "Handle must contain an actual open center")
                    self.assertEqual(alpha.getpixel((31, 3)), 255, "Star's upper point must extend beyond its arms")
                    self.assertEqual(alpha.getpixel((26, 8)), 255)
                    self.assertEqual(alpha.getpixel((26, 3)), 0)
            self.assertEqual(bounds[0], bounds[1])

    def test_storm_shard_keeps_its_special_energy_layer_over_the_new_sprite(self):
        root = Path(__file__).resolve().parents[1] / f"src/main/resources/{ASSETS}"
        with Image.open(root / "textures/item/storm_shard.png") as image:
            self.assertEqual(image.size, (64, 64))
            self.assertEqual(set(image.getchannel("A").get_flattened_data()), {0, 255})
        model = json.loads((root / "items/storm_shard.json").read_text(encoding="utf-8"))["model"]
        self.assertEqual(model["type"], "minecraft:select")
        gui_model = model["cases"][0]["model"]
        self.assertEqual(gui_model["type"], "minecraft:composite")
        self.assertEqual(gui_model["models"][1]["model"]["type"], "trading_cells:storm_shard_charge")
        self.assertEqual(model["fallback"]["models"][1]["model"]["type"], "trading_cells:storm_shard_charge")


class PipePaletteTests(unittest.TestCase):
    def test_all_pipe_items_move_toward_the_palm_without_changing_scale(self):
        generated = resources()
        for kind in ("item", "fluid", "gas", "energy", "universal"):
            model = json.loads(generated[f"{ASSETS}/models/block/logistics/{kind}_pipe/{kind}_pipe_inventory.json"])
            for hand in ("thirdperson_righthand", "thirdperson_lefthand"):
                self.assertEqual(model["display"][hand], {"rotation": [75, 45, 0],
                                 "translation": [0, 0.5, 0], "scale": [0.375, 0.375, 0.375]})

    def test_item_pipe_recolors_only_the_original_white_conduit(self):
        original = hashlib.sha256()
        reverse = {(235, 192, 43, 255): (191, 195, 199, 255)}
        for strength in (0.25, 0.9):
            reverse[blend((156, 116, 22), (255, 232, 93), strength)] = blend(
                (132, 140, 149), (224, 229, 232), strength)
        paths = sorted((PIPE_TEXTURES / "item_pipe").glob("*.png"))
        self.assertEqual(len(paths), 17)
        for path in paths:
            with Image.open(path) as source:
                image = source.convert("RGBA")
            self.assertEqual(image.size, (32, 256))
            pixels = list(image.get_flattened_data())
            self.assertTrue(any(pixel in reverse for pixel in pixels))
            image.putdata([reverse.get(pixel, pixel) for pixel in pixels])
            original.update(path.name.encode() + b"\0")
            original.update(image.tobytes())
        # Original RGBA sprites, before this palette-only change; includes every animation frame.
        self.assertEqual(original.hexdigest(), "d6882e3ad9fe2e122f6fc5c2d66cd57daf21abbc7d1c37ea4eebb2e2c269c41f")


class SimulationModelTests(unittest.TestCase):
    def test_entity_previews_keep_the_baked_pedestal(self):
        generated = simulation_resources()
        for name, base, renderer in (("mob_farm", "block/mob_farm", "block_entity_item"),):
            with self.subTest(item=name):
                model = json.loads(generated[f"assets/trading_cells/items/{name}.json"])["model"]
                self.assertEqual(model, {"type": "minecraft:composite", "models": [
                    {"type": "minecraft:model", "model": f"trading_cells:{base}"},
                    {"type": "minecraft:special", "base": f"trading_cells:{base}",
                     "model": {"type": f"trading_cells:{renderer}"}}]})
        farm = json.loads(generated["assets/trading_cells/models/block/mob_farm.json"])
        self.assertTrue(any(part["from"][1] == part["to"][1] == 4.5 and "up" in part["faces"]
                            for part in farm["elements"]))
        self.assertNotIn("spawner", json.dumps(farm))
        module = json.loads(generated["assets/trading_cells/models/item/entity_module.json"])
        dispatch = json.loads(generated["assets/trading_cells/items/entity_module.json"])["model"]
        self.assertEqual(dispatch["type"], "minecraft:range_dispatch")
        self.assertEqual(dispatch["property"], "trading_cells:essence_tier")
        self.assertEqual([entry["threshold"] for entry in dispatch["entries"]], [1, 2, 3, 4])
        for tier, name in enumerate(("tier_i", "tier_ii", "tier_iii", "tier_iv"), 1):
            base = f"item/{name}_creature_model_base"
            self.assertEqual(dispatch["entries"][tier - 1]["model"], {"type": "minecraft:composite", "models": [
                {"type": "minecraft:model", "model": f"trading_cells:{base}"},
                {"type": "minecraft:special", "base": "trading_cells:item/entity_module",
                 "model": {"type": "trading_cells:entity_module"}}]})
            variant = json.loads(generated[f"assets/trading_cells/models/{base}.json"])
            self.assertEqual(variant["elements"], module["elements"])
            self.assertEqual(variant["display"], module["display"])
            self.assertEqual(variant["textures"]["base"], "minecraft:block/black_concrete")
            self.assertEqual(variant["textures"]["edge"], f"trading_cells:block/essence/tier_{tier}_edge")
        self.assertEqual(module["elements"], outlined_union(
            [([1, 0, 1], [15, 1.5, 15]), ([3, 1.5, 3], [13, 3.5, 13])], "base"))
        for hand in ("firstperson_righthand", "firstperson_lefthand"):
            self.assertEqual(module["display"][hand]["translation"], [-1, 3.5, 3])
            self.assertEqual(module["display"][hand]["scale"], [0.48, 0.48, 0.48])
        for hand in ("thirdperson_righthand", "thirdperson_lefthand"):
            self.assertEqual(module["display"][hand]["rotation"], [70, 0, 0])
            self.assertEqual(module["display"][hand]["translation"], [0, 2.75, 3])
            self.assertEqual(module["display"][hand]["scale"], [0.4, 0.4, 0.4])

    def test_simulation_surfaces_keep_distinct_materials(self):
        generated = simulation_resources()
        for path in ("block/mob_farm", "block/essence_workbench", "item/entity_module"):
            with self.subTest(model=path):
                model = json.loads(generated[f"assets/trading_cells/models/{path}.json"])
                self.assertEqual(model["textures"]["base"], "minecraft:block/black_concrete")
                self.assertEqual(model["textures"]["edge"], "trading_cells:block/logistics/fluid_pipe/fluid_pipe")
                self.assertLessEqual(len(model["elements"]), 330, "Union meshing must keep a bounded static quad count")
                for element in model["elements"]:
                    self.assertEqual(len(element["faces"]), 1)
                    for direction, face in element["faces"].items():
                        if face["texture"] == "#edge":
                            self.assertEqual(face["uv"][1::2], [7, 9])
                            self.assertFalse(element["shade"])
                        else:
                            self.assertEqual(face["texture"], "#base")

    def test_coplanar_joins_have_no_blue_seams_or_internal_faces(self):
        split = outlined_union([([0, 0, 0], [4, 8, 4]), ([4, 0, 0], [8, 8, 4])], "base")
        self.assertEqual(split, outlined_union([([0, 0, 0], [8, 8, 4])], "base"))
        stacked = outlined_union([([0, 0, 0], [4, 2, 4]), ([0, 2, 0], [4, 8, 4])], "base")
        self.assertEqual(stacked, outlined_union([([0, 0, 0], [4, 8, 4])], "base"))

    def test_outlined_faces_cover_original_bounds_without_overlapping(self):
        start, end = [1, 2, 3], [15, 3.5, 13]
        elements = outlined_union([(start, end)], "base")
        for axis, directions in ((0, ("west", "east")), (1, ("down", "up")), (2, ("north", "south"))):
            u, v = [coordinate for coordinate in range(3) if coordinate != axis]
            for face, plane in zip(directions, (start[axis], end[axis])):
                parts = [part for part in elements if face in part["faces"]]
                area = 0
                for index, part in enumerate(parts):
                    first, last = part["from"], part["to"]
                    self.assertEqual(first[axis], plane)
                    self.assertEqual(last[axis], plane)
                    self.assertTrue(all(start[i] <= first[i] <= last[i] <= end[i] for i in range(3)))
                    area += (last[u] - first[u]) * (last[v] - first[v])
                    for other in parts[:index]:
                        overlap_u = min(last[u], other["to"][u]) - max(first[u], other["from"][u])
                        overlap_v = min(last[v], other["to"][v]) - max(first[v], other["from"][v])
                        self.assertFalse(overlap_u > 0 and overlap_v > 0, "Coplanar borders must not overlap")
                self.assertAlmostEqual(area, (end[u] - start[u]) * (end[v] - start[v]))


class UpgradePaletteTests(unittest.TestCase):
    def test_all_families_share_exact_quarry_frames_and_baked_emblems(self):
        images = generated()
        quarry = family_base("quarry")
        for material in MATERIALS:
            expected_frame = generic_frame(material)
            for family in FAMILIES:
                with self.subTest(material=material, family=family):
                    base = family_base(family)
                    mask = emblem_pixels(family, base)
                    self.assertGreater(len(mask), 200)
                    self.assertTrue(all(14 <= x < 51 and 15 <= y < 49 for x, y in mask))
                    result = images[ORIGINALS / family / f"{material}_upgrade.png"]
                    self.assertEqual(result.getchannel("A").tobytes(), quarry.getchannel("A").tobytes())
                    for y in range(64):
                        for x in range(64):
                            expected = base if (x, y) in mask else expected_frame
                            self.assertEqual(result.getpixel((x, y)), expected.getpixel((x, y)), (x, y))
                    for point in wooden_emblem_pixels(family):
                        self.assertEqual(result.getpixel(point), base.getpixel(point))
        self.assertEqual(images[ORIGINALS / "quarry/copper_upgrade.png"].tobytes(), quarry.tobytes())
        for material in MATERIALS:
            speed = images[ORIGINALS / "mob_farm_speed" / f"{material}_upgrade.png"]
            capacity = images[ORIGINALS / "mob_farm_capacity" / f"{material}_upgrade.png"]
            for bounds in MOB_FARM_SWORD_REGIONS:
                self.assertEqual(speed.crop(bounds).tobytes(), capacity.crop(bounds).tobytes())

    def test_generic_frames_keep_quarry_colors_without_the_pickaxe(self):
        quarry = family_base("quarry")
        mask = emblem_pixels("quarry", quarry)
        images = generated()
        for material in MATERIALS:
            frame = generic_frame(material)
            icon = images[ORIGINALS / "quarry" / f"{material}_upgrade.png"]
            self.assertEqual(frame.size, (64, 64))
            self.assertEqual(frame.getchannel("A").tobytes(), icon.getchannel("A").tobytes())
            self.assertEqual(frame.tobytes(), images[ORIGINALS / "generic" / f"{material}_upgrade.png"].tobytes())
            self.assertEqual(frame.tobytes(), quarry_frame(icon, quarry).tobytes())
            self.assertTrue(any(frame.getpixel(point) != icon.getpixel(point) for point in mask))
            for y in range(64):
                for x in range(64):
                    if (x, y) not in mask:
                        self.assertEqual(frame.getpixel((x, y)), icon.getpixel((x, y)))

    def test_syringe_models_are_closed_3d_and_fill_each_tier(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS / "models/item"
        body = json.loads((root / "essence_extractor.json").read_text(encoding="utf-8"))
        self.assertNotIn("layer0", body["textures"])
        self.assertGreaterEqual(len(body["elements"]), 10)
        poses = [body]
        for tier in range(1, 5):
            start = json.loads((root / "essence" / f"syringe_extract_{tier}_00.json").read_text(encoding="utf-8"))
            filled = json.loads((root / "essence" / f"syringe_extract_{tier}_24.json").read_text(encoding="utf-8"))
            self.assertEqual(len(filled["elements"]), len(start["elements"]) + 1)
            self.assertEqual(filled["textures"]["essence"], f"trading_cells:block/essence/tier_{tier}_edge")
            poses.extend((start, filled))
        for model in poses:
            for cube in model["elements"]:
                self.assertEqual(set(cube["faces"]), {"north", "south", "east", "west", "up", "down"})
                self.assertTrue(all(end > start for start, end in zip(cube["from"], cube["to"])))

    def test_barter_emblem_excludes_copper_fragments_without_holes_in_gold(self):
        base = family_base("piglin_barter")
        mask = emblem_pixels("piglin_barter", base)
        images = generated()
        # These panel texels were accidentally included by the old polygon crop.
        background = ((18, 20), (20, 18), (44, 24), (20, 30), (20, 32),
                      (43, 33), (43, 36), (18, 39), (44, 43), (28, 40))
        gold = {(x, y) for y in range(31, 35) for x in range(22, 41)}
        gold.update(((22, 18), (28, 16), (34, 19), (41, 24), (28, 37),
                     (42, 38), (42, 43), (25, 46), (35, 47)))
        self.assertTrue(gold <= mask, "Gold highlights and orange shadows must remain solid")
        self.assertTrue(set(background).isdisjoint(mask))
        for point in mask:
            red, green, _, _ = base.getpixel(point)
            self.assertFalse(red >= 110 and green <= red * 0.30,
                             f"Copper panel fragment copied at {point}")
        frame = quarry_frame(family_base("quarry"))
        for material in MATERIALS:
            with self.subTest(material=material):
                icon = images[ORIGINALS / "piglin_barter" / f"{material}_upgrade.png"]
                expected_frame = family_texture(frame, material)
                for point in background:
                    self.assertEqual(icon.getpixel(point), expected_frame.getpixel(point), point)
                for point in gold:
                    self.assertEqual(icon.getpixel(point), base.getpixel(point), point)

    def test_terminal_steel_is_baked_without_changing_alpha_or_screens(self):
        for name in (*TERMINALS, TERMINAL_BODY):
            with self.subTest(terminal=name):
                with Image.open(TERMINAL_ORIGINALS / f"{name}.png") as source:
                    original = source.convert("RGBA")
                with Image.open(BASES / f"{name}.png") as source:
                    actual = source.convert("RGBA")
                self.assertEqual(actual.size, original.size)
                self.assertEqual(actual.tobytes(), logistics_terminal_texture(original).tobytes())
                self.assertEqual(actual.getchannel("A").tobytes(), original.getchannel("A").tobytes())
                self.assertEqual(logistics_terminal_texture(actual).tobytes(), actual.tobytes())
                changed = 0
                for before, after in zip(original.get_flattened_data(), actual.get_flattened_data()):
                    red, green, blue, alpha = before
                    if alpha and red > blue * 1.2 and red > green * 1.03:
                        self.assertIn(after[:3], TERMINAL_STEEL)
                        changed += before != after
                    else:
                        self.assertEqual(after, before, "Displays, steel, indicators and transparent pixels remain exact")
                self.assertGreater(changed, 1000)

    def test_only_diamond_and_netherite_have_special_rivets(self):
        for family in FAMILIES:
            base = family_base(family)
            mask = rivet_pixels(base)
            self.assertGreater(len(mask), 60, family)
            for material in MATERIALS:
                with self.subTest(family=family, material=material):
                    plain = recolor(base, material, fixed_emblem_pixels(family, base))
                    result = family_texture(base, material, family)
                    differences = {(x, y) for y in range(64) for x in range(64)
                                   if plain.getpixel((x, y)) != result.getpixel((x, y))}
                    self.assertEqual(result.getchannel("A").tobytes(), base.getchannel("A").tobytes())
                    if material in ("copper", "iron", "gold"):
                        self.assertFalse(differences, "Ordinary tiers must keep material-colored corners")
                    else:
                        self.assertTrue(differences)
                        self.assertTrue(differences <= mask, "Magic colors may not leak into the frame or emblem")
                        for x, y in mask:
                            red, green, blue, _ = result.getpixel((x, y))
                            if material == "diamond":
                                self.assertGreaterEqual(blue, red)
                                self.assertGreater(blue, green)
                            else:
                                self.assertGreater(red, blue)
                                self.assertGreater(red, green)

    def test_simulation_upgrades_match_the_quarry_footprint_and_sword_model(self):
        with Image.open(BASES / "quarry.png") as source:
            bounds = source.convert("RGBA").getchannel("A").getbbox()
        swords = []
        for family in ("mob_farm_speed", "mob_farm_capacity"):
            base = family_base(family)
            self.assertEqual(base.getchannel("A").getbbox(), bounds)
            # Shared blade, crossguard and pommel silhouette, excluding the side indicators.
            silhouette = []
            for left, top, right, bottom in ((28, 18, 36, 34), (24, 35, 40, 40), (28, 42, 35, 47)):
                for y in range(top, bottom):
                    for x in range(left, right):
                        red, green, blue, _ = base.getpixel((x, y))
                        silhouette.append(green > red * 1.15 and blue > red * 1.15
                                          or max(red, green, blue) - min(red, green, blue) < 25
                                          and max(red, green, blue) > 100)
            swords.append(silhouette)
            for y in (16, 17, 48, 49):
                for x in range(27, 37):
                    red, green, blue, _ = base.getpixel((x, y))
                    self.assertGreater(red, green, "Orange gap must separate the sword and the frame")
                    self.assertGreater(red, blue)
        self.assertEqual(swords[0], swords[1], "Both upgrades must depict the same sword model")
        for material in MATERIALS:
            first = family_texture(family_base("mob_farm_speed"), material, "mob_farm_speed")
            second = family_texture(family_base("mob_farm_capacity"), material, "mob_farm_capacity")
            for bounds in MOB_FARM_SWORD_REGIONS:
                self.assertEqual(first.crop(bounds).tobytes(), second.crop(bounds).tobytes(),
                                 "Sword pixels must match exactly at every material tier")

    def test_emblem_wood_and_gold_do_not_follow_the_upgrade_material(self):
        for family in ("quarry", "piglin_barter", "mob_farm_speed", "mob_farm_capacity"):
            base = family_base(family)
            mask = fixed_emblem_pixels(family, base)
            self.assertGreater(len(mask), 10)
            for point in wooden_emblem_pixels(family):
                rgb = base.getpixel(point)[:3]
                if luminance(rgb) >= 18:
                    self.assertIn(rgb, WOOD_PALETTE, (family, point))
            for material in MATERIALS:
                with self.subTest(family=family, material=material):
                    result = family_texture(base, material, family)
                    for point in mask:
                        self.assertEqual(result.getpixel(point), base.getpixel(point), point)
        gold = family_base("piglin_barter")
        for material in MATERIALS:
            result = family_texture(gold, material, "piglin_barter")
            for point in ((30, 31), (27, 18), (39, 41)):
                self.assertEqual(result.getpixel(point), gold.getpixel(point))

    def test_all_upgrade_items_use_only_their_family_texture(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources"
        names = [f"{family}_{material}_upgrade" for family in FAMILIES if family != "pipe" for material in MATERIALS]
        names += [f"{tier}_pipe_upgrade" for tier in ("copper", "iron", "gold", "diamond", "netherite")]
        generated_resources = resources()
        for name in names:
            with self.subTest(item=name):
                relative = f"{ASSETS}/items/{name}.json"
                expected = {"model": {"type": "minecraft:model", "model": f"trading_cells:item/{name}"}}
                self.assertEqual(json.loads((root / relative).read_text(encoding="utf-8")), expected)
                if relative in generated_resources:
                    self.assertEqual(json.loads(generated_resources[relative]), expected)

    def test_sprite_transparency_and_opaque_block_housing(self):
        images = generated()
        self.assertEqual(len(images), 33)
        self.assertFalse(any(path.parent.name == "item" for path in images),
                         "Terminal items use block models, not duplicate item PNGs")
        for path, image in images.items():
            with self.subTest(texture=path.name):
                self.assertEqual(image.size, (64, 64))
                expected = (255, 255) if path.stem == TERMINAL_BODY else (0, 255)
                self.assertEqual(image.getchannel("A").getextrema(), expected)

    def test_placed_terminals_use_distinct_top_panels_over_solid_housing(self):
        generated_resources = resources()
        for name in ("network_terminal", "network_crafting_terminal"):
            model = json.loads(generated_resources[f"{ASSETS}/models/block/{name}.json"])
            self.assertEqual(model["render_type"], "minecraft:cutout")
            self.assertEqual(model["textures"]["panel"], f"trading_cells:block/logistics/{name}_front")
            with Image.open(BASES / f"{name}.png") as source:
                front = next(image for path, image in generated().items() if path.stem == f"{name}_front")
                self.assertEqual(front.tobytes(), source.convert("RGBA").tobytes())
            self.assertEqual(model["textures"]["pipe"], f"trading_cells:block/logistics/{TERMINAL_BODY}")
            housing, panel = model["elements"]
            self.assertEqual(len(housing["faces"]), 6)
            self.assertEqual(set(panel["faces"]), {"up"})
            self.assertEqual(panel["faces"]["up"]["texture"], "#panel")
            self.assertEqual(panel["faces"]["up"]["cullface"], "up")
            self.assertEqual(panel["faces"]["up"]["rotation"], 180)
            self.assertGreater(panel["from"][1], housing["to"][1])
            self.assertEqual(panel["from"][1], panel["to"][1])
            self.assertEqual(panel["faces"]["up"]["uv"], [0, 0, 16, 16])
            for face in housing["faces"].values():
                self.assertEqual(face["texture"], "#pipe")

    def test_material_ramps_keep_light_order(self):
        for material in MATERIALS[1:]:
            ramp = material_ramp(material)
            self.assertGreater(len(ramp), 3)
            values = [luminance(rgb) for rgb in ramp]
            self.assertEqual(values, sorted(values))

    def test_recolor_preserves_pixel_positions_and_shading(self):
        for family in FAMILIES:
            with Image.open(BASES / f"{family}.png") as source:
                base = source.convert("RGBA")
            for material in MATERIALS:
                with self.subTest(family=family, material=material):
                    result = recolor(base, material)
                    self.assertEqual(base.getchannel("A").tobytes(), result.getchannel("A").tobytes())
                    mapped = {}
                    for y in range(64):
                        for x in range(64):
                            before, after = base.getpixel((x, y)), result.getpixel((x, y))
                            red, green, blue, alpha = before
                            if not alpha or not (red > blue * 1.2 and red > green * 1.03) or material == "copper":
                                self.assertEqual(before, after)
                            else:
                                self.assertEqual(mapped.setdefault(before[:3], after[:3]), after[:3])
                    shades = [luminance(after) for before, after in sorted(mapped.items(), key=lambda pair: luminance(pair[0]))]
                    self.assertEqual(shades, sorted(shades), "Shadows must never become brighter than highlights")


if __name__ == "__main__":
    unittest.main()
