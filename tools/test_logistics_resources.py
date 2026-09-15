"""Regression checks for portable generated-resource comparisons."""

from io import BytesIO
import json
from pathlib import Path
import unittest

from PIL import Image
from PIL.PngImagePlugin import PngInfo

from generate_logistics_resources import ASSETS, resource_matches, resources
from generate_mob_simulation_resources import resources as simulation_resources
from generate_family_upgrades import BASES, FAMILIES, MATERIALS, MOB_FARM_SWORD_REGIONS, TERMINAL_BODY, family_base, family_texture, generated, luminance, material_ramp, recolor, rivet_pixels
from generate_family_upgrades import WOOD_PALETTE, fixed_emblem_pixels, wooden_emblem_pixels
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


class SimulationModelTests(unittest.TestCase):
    def test_entity_previews_keep_the_baked_pedestal(self):
        generated = simulation_resources()
        for name, base, renderer in (("entity_module", "item/entity_module", "entity_module"),
                                     ("mob_farm", "block/mob_farm", "block_entity_item")):
            with self.subTest(item=name):
                model = json.loads(generated[f"assets/trading_cells/items/{name}.json"])["model"]
                self.assertEqual(model, {"type": "minecraft:composite", "models": [
                    {"type": "minecraft:model", "model": f"trading_cells:{base}"},
                    {"type": "minecraft:special", "base": f"trading_cells:{base}",
                     "model": {"type": f"trading_cells:{renderer}"}}]})
        farm = json.loads(generated["assets/trading_cells/models/block/mob_farm.json"])
        self.assertEqual(farm["elements"][-1]["from"], [9, 3, 5])
        self.assertEqual(farm["elements"][-1]["to"], [13, 4.5, 11])
        self.assertNotIn("spawner", json.dumps(farm))
        module = json.loads(generated["assets/trading_cells/models/item/entity_module.json"])
        self.assertEqual(len(module["elements"]), 2, "Only two base tiers, no enclosing posts or roof")
        for element in module["elements"]:
            self.assertEqual(element["from"][0] + element["to"][0], 16)
            self.assertEqual(element["from"][2] + element["to"][2], 16)
        self.assertEqual(module["elements"][-1]["to"][1], 3.5)
        for hand in ("firstperson_righthand", "firstperson_lefthand"):
            self.assertEqual(module["display"][hand]["translation"], [0, 5.5, 0])
            self.assertEqual(module["display"][hand]["scale"], [0.32, 0.32, 0.32])


class UpgradePaletteTests(unittest.TestCase):
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
        names += [f"{tier}_pipe_upgrade" for tier in ("basic", "improved", "advanced", "ultimate", "infinite")]
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
        self.assertEqual(len(images), 30)
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
