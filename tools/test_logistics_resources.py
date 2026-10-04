"""Regression checks for portable generated-resource comparisons."""

from io import BytesIO
import hashlib
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from PIL import Image
from PIL.PngImagePlugin import PngInfo

import injector_geometry
import simulation_worker_texture
import essence_tier_textures
import machine_gui_art
import generate_machine_gui
import generate_villager_trade_gui
from generate_logistics_resources import ASSETS, resource_matches, resources
from generate_mob_simulation_resources import outlined_union, resources as simulation_resources
from generate_pipe_textures import TEXTURES as PIPE_TEXTURES, blend
from generate_family_upgrades import BASES, FAMILIES, MATERIALS, TERMINAL_BODY, family_base, family_texture, generated, luminance, material_ramp, recolor, rivet_pixels, utility_emblem
from generate_family_upgrades import WOOD_PALETTE, fixed_emblem_pixels, wooden_emblem_pixels
from generate_family_upgrades import ORIGINALS, FRAMES, emblem_pixels, quarry_frame, generic_frame
from generate_family_upgrades import TERMINALS, TERMINAL_ORIGINALS, TERMINAL_STEEL, logistics_terminal_texture


def encoded(image: Image.Image, **options) -> bytes:
    output = BytesIO()
    image.save(output, format="PNG", **options)
    return output.getvalue()


class LogisticsResourceTests(unittest.TestCase):
    def test_gui_backgrounds_live_with_their_machine_resources(self):
        assets = machine_gui_art.generated_assets()
        self.assertEqual(len(assets), 71, "Nineteen menus, thirteen REI backgrounds and one XP summary")
        owners = set(machine_gui_art.SCREENS) | set(machine_gui_art.RECIPE_THEMES)
        blockstates = machine_gui_art.OUTPUT.parents[1] / "blockstates"
        for path in assets:
            relative = path.relative_to(machine_gui_art.OUTPUT)
            self.assertEqual(relative.parts[0], "container")
            self.assertIn(relative.parts[1], owners)
            self.assertTrue((blockstates / f"{relative.parts[1]}.json").is_file())
            self.assertIn(relative.name, {"background.png", "background_compact.png", "background_small.png",
                                          "recipe.png", "xp_summary.png"})

    def test_gui_generator_rejects_rgb_changes_with_unchanged_alpha(self):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "panel.png"
            expected = Image.new("RGBA", (2, 2), (10, 20, 30, 255))
            expected.save(path)
            with (patch.object(generate_machine_gui, "ROOT", Path(directory)),
                  patch.object(generate_machine_gui, "generated_assets", return_value={path: expected}),
                  patch.object(generate_machine_gui, "manually_maintained_sprites", return_value={}),
                  patch.object(generate_villager_trade_gui, "widget_sizes", return_value={})):
                generate_machine_gui.validate(True)
                changed = expected.copy()
                changed.putpixel((0, 0), (11, 20, 30, 255))
                changed.save(path)
                with self.assertRaisesRegex(SystemExit, "does not match generator"):
                    generate_machine_gui.validate(True)

    def test_gui_inventory_has_no_undeclared_textures(self):
        expected = (generate_machine_gui.generated_assets().keys()
                    | generate_machine_gui.manually_maintained_sprites().keys()
                    | generate_villager_trade_gui.widget_sizes().keys())
        self.assertEqual(len(expected), 103)
        self.assertEqual(set(generate_machine_gui.ROOT.rglob("*.png")), expected)

    def test_gui_validator_rejects_an_unused_texture(self):
        with TemporaryDirectory() as directory:
            Image.new("RGBA", (2, 2)).save(Path(directory) / "unused.png")
            with (patch.object(generate_machine_gui, "ROOT", Path(directory)),
                  patch.object(generate_machine_gui, "generated_assets", return_value={}),
                  patch.object(generate_machine_gui, "manually_maintained_sprites", return_value={}),
                  patch.object(generate_villager_trade_gui, "widget_sizes", return_value={})):
                with self.assertRaisesRegex(SystemExit, "without a declared consumer"):
                    generate_machine_gui.validate(True)

    def test_painted_gui_content_excludes_all_decorative_frame_texels(self):
        source = Image.new("RGBA", (256, 256), (255, 0, 255, 255))
        source.paste((20, 30, 40, 255), (56, 56, 200, 200))
        for _, size in machine_gui_art.SCREENS.values():
            for width, height in [size, (200, 124), (78, 36)]:
                for outset in (0,) if (width, height) == (78, 36) else machine_gui_art.FRAME_OUTSETS:
                    fitted = machine_gui_art.fit_panel(source, (width, height), outset)
                    self.assertEqual(fitted.size, (width + 2 * outset, height + 2 * outset))
                    self.assertEqual(set(fitted.crop((outset + 3, outset + 3, outset + width - 3, outset + height - 3)).get_flattened_data()),
                                     {(20, 30, 40, 255)}, "No frame pixels under controls or labels")

    def test_painted_gui_frames_retain_decorative_depth(self):
        source = Image.new("RGBA", (256, 256), (255, 0, 255, 255))
        source.paste((20, 30, 40, 255), (56, 56, 200, 200))
        panel = machine_gui_art.fit_panel(source, (256, 228))
        self.assertEqual(panel.size, (298, 270))
        self.assertEqual(set(panel.crop((0, 0, 24, 270)).get_flattened_data()), {(255, 0, 255, 255)})
        self.assertEqual(set(panel.crop((0, 0, 298, 24)).get_flattened_data()), {(255, 0, 255, 255)})
        self.assertEqual(panel.getpixel((24, 24)), (20, 30, 40, 255))

    def test_gui_corner_proportions_and_edge_details_do_not_stretch(self):
        source = Image.frombytes("RGBA", (256, 256), bytes(
            component for y in range(256) for x in range(256) for component in (x, y, 80, 255)))
        for outset in machine_gui_art.FRAME_OUTSETS:
            frame = outset + 3
            corner = source.crop((0, 0, 56, 56)).resize((frame, frame), Image.Resampling.NEAREST)
            length = round(144 * frame / 56)
            edge = source.crop((56, 0, 200, 56)).resize((length, frame), Image.Resampling.NEAREST)
            for size in ((204, 215), (348, 228), (200, 124)):
                panel = machine_gui_art.fit_panel(source, size, outset)
                self.assertEqual(panel.crop((0, 0, frame, frame)).tobytes(), corner.tobytes())
                for repeat in range(2):
                    x = frame + repeat * length
                    self.assertEqual(panel.crop((x, 0, x + length, frame)).tobytes(), edge.tobytes())

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

    def test_pipe_wrench_uses_the_authored_32_pixel_sprite(self):
        root = Path(__file__).resolve().parents[1]
        with Image.open(root / "tools/assets/logistics/pipe_wrench.png") as source, \
                Image.open(root / "src/main/resources/assets/trading_cells/textures/item/pipe_wrench.png") as actual:
            self.assertEqual(actual.size, (32, 32))
            self.assertEqual(actual.convert("RGBA").tobytes(), source.convert("RGBA").tobytes())
            self.assertEqual(actual.getchannel("A").getextrema()[0], 0)
            self.assertGreaterEqual(actual.getchannel("A").getextrema()[1], 250)

    def test_pipe_wrench_reverses_only_held_orientation(self):
        model = json.loads(resources()[f"{ASSETS}/models/item/pipe_wrench.json"])
        self.assertEqual(model["textures"]["layer0"], "trading_cells:item/pipe_wrench")
        self.assertEqual(model["parent"], "minecraft:item/handheld")
        for hand, original in (("right", 1), ("left", -1)):
            for view, angle in (("firstperson", 25), ("thirdperson", 55)):
                pose = model["display"][f"{view}_{hand}hand"]
                expected = [0, 90 * original, angle * original]
                self.assertEqual(pose["rotation"], expected)
                if view == "thirdperson":
                    self.assertEqual(pose["translation"], [0, 5.33, 0.94])
        self.assertEqual(len(model["display"]), 4)

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
    def test_interaction_machine_keeps_cage_trays_and_articulated_arm(self):
        generated = simulation_resources()
        model = json.loads(generated[f"{ASSETS}/models/block/entity_interaction_farm.json"])
        self.assertEqual(model["textures"]["base"], "minecraft:block/black_concrete")
        self.assertEqual(model["textures"]["edge"], "trading_cells:block/logistics/fluid_pipe/fluid_pipe")
        self.assertEqual(model["textures"]["mechanism"], "trading_cells:block/interaction_mechanism")
        self.assertEqual(model["textures"]["mechanism_edge"], "trading_cells:block/interaction_mechanism_edge")
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS / "textures/block"
        for name in ("interaction_mechanism", "interaction_mechanism_edge"):
            self.assertFalse((root / f"{name}.png.mcmeta").exists(), "Arm textures are static")
        elements = model["elements"]
        self.assertLessEqual(len(elements), 700, "Static mechanism must keep a bounded quad count")
        self.assertTrue(all(0 <= part["from"][axis] <= part["to"][axis] <= 16
                            for part in elements for axis in range(3)))

        def surface_at(x, y, z):
            return any("up" in part["faces"] and part["from"][1] == part["to"][1] == y
                       and part["from"][0] <= x <= part["to"][0]
                       and part["from"][2] <= z <= part["to"][2] for part in elements)

        for x in (1, 15):
            for z in (1, 15):
                self.assertTrue(surface_at(x, 16, z), "Keep all four cage pillars")
        self.assertTrue(surface_at(3.5, 2.75, 3), "ITEM tray must remain below the worker")
        self.assertTrue(surface_at(12, 2.75, 3), "TOOL tray must remain below the target")
        self.assertTrue(surface_at(11, 4.5, 8), "Target pedestal must remain solid")
        self.assertTrue(surface_at(4.5, 6, 12.5), "Keep rear processing core")
        mechanism = [part for part in elements
                     if any(face["texture"] == "#mechanism" for face in part["faces"].values())]
        self.assertGreater(len(mechanism), 12, "Keep the arm joints and end effector")
        self.assertTrue(all(part["from"][1] >= 4 and part["to"][1] <= 11.5
                            and part["from"][2] >= 8 for part in mechanism),
                        "Arm stays behind the operator and target, below the cage top")
        panel = [part for part in elements
                 if any(face["texture"] == "#panel" for face in part["faces"].values())]
        self.assertFalse(panel, "Front sign was removed")
        self.assertNotIn("panel", model["textures"])

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
            self.assertEqual(filled["textures"]["essence"], f"trading_cells:item/essence/tier_{tier}_liquid")
            poses.extend((start, filled))
        for model in poses:
            # All layers of a baked cuboid item must resolve into the same atlas.
            for texture in model["textures"].values():
                self.assertTrue(texture.startswith(("#", "trading_cells:item/")), texture)
            for cube in model["elements"]:
                self.assertEqual(set(cube["faces"]), {"north", "south", "east", "west", "up", "down"})
                self.assertTrue(all(end > start for start, end in zip(cube["from"], cube["to"])))

    def test_injector_atlas_preserves_glass_and_custom_materials(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS / "textures"
        with Image.open(root / "item/essence/injector_atlas.png") as atlas:
            self.assertEqual(atlas.size, (128, 128))
            self.assertLess(atlas.getpixel((48, 80))[3], 100)
            self.assertGreater(atlas.getpixel((48, 16))[3], 240)
        for tier in range(1, 5):
            with Image.open(root / f"item/essence/tier_{tier}_liquid.png") as liquid, \
                    Image.open(root / f"block/essence/tier_{tier}_edge.png") as edge:
                self.assertEqual(liquid.size, edge.size)
                self.assertEqual(liquid.tobytes(), edge.tobytes())
            self.assertEqual(
                json.loads((root / f"item/essence/tier_{tier}_liquid.png.mcmeta").read_text()),
                json.loads((root / f"block/essence/tier_{tier}_edge.png.mcmeta").read_text()))
        for element in injector_geometry.body() + injector_geometry.vial():
            for face in element["faces"].values():
                self.assertEqual(face["texture"], "#atlas")
                self.assertTrue(all(0 <= value <= 16 for value in face["uv"]))

    def test_injector_animation_translates_bevel_origins_with_geometry(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS / "models/item/essence"
        original = injector_geometry.shifted(injector_geometry.vial(), 0)
        self.assertTrue(any("rotation" in element for element in original))
        for frame in range(33):
            model = json.loads((root / f"syringe_vial_{frame:02d}.json").read_text(encoding="utf-8"))
            elements = model["elements"]
            delta = [b - a for a, b in zip(original[0]["from"], elements[0]["from"])]
            self.assertEqual(len(elements), len(original))
            for base, moved in zip(original, elements):
                points = [(base[key], moved[key]) for key in ("from", "to")]
                if "rotation" in base:
                    points.append((base["rotation"]["origin"], moved["rotation"]["origin"]))
                    self.assertEqual(base["rotation"]["angle"], moved["rotation"]["angle"])
                for before, after in points:
                    for a, b, shift in zip(before, after, delta):
                        self.assertAlmostEqual(b - a, shift, places=4)
            if frame == 32:
                self.assertEqual(elements, original)

    def test_syringe_reload_approaches_from_opposite_side_above_socket(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS
        original = injector_geometry.vial()[0]["from"]
        for frame in range(33):
            right = json.loads((root / f"models/item/essence/syringe_vial_{frame:02d}.json").read_text())
            left = json.loads((root / f"models/item/essence/syringe_vial_{frame:02d}_left.json").read_text())
            r, l = right["elements"][0]["from"], left["elements"][0]["from"]
            self.assertGreaterEqual(r[1], original[1], "Vial must never come from below")
            self.assertEqual(r[:2], l[:2])
            self.assertAlmostEqual(r[2] - original[2], original[2] - l[2], places=4)
            self.assertLessEqual(r[2], original[2])
            if frame >= 24:
                self.assertEqual(r, original, "Vial stays seated while the opposite arm retracts")
        body = json.loads((root / "models/item/essence_extractor.json").read_text())
        self.assertEqual(body["display"]["firstperson_righthand"]["translation"], [0, 2, -1])
        self.assertEqual(body["display"]["firstperson_lefthand"]["translation"], [0, 2, -1])
        definition = json.loads((root / "items/essence_extractor.json").read_text())["model"]["fallback"]
        for entry in definition["entries"]:
            self.assertEqual(entry["model"]["models"][0]["model"], "trading_cells:item/essence_extractor",
                             "Body remains present in every reload frame")

    def test_syringe_third_person_needle_points_away_and_grip_is_in_palm(self):
        import math

        def rotate(point, axis, degrees):
            angle = math.radians(degrees)
            c, s = math.cos(angle), math.sin(angle)
            x, y, z = point
            return ((x, c * y - s * z, s * y + c * z) if axis == 0 else
                    (c * x + s * z, y, -s * x + c * z) if axis == 1 else
                    (c * x - s * y, s * x + c * y, z))

        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS / "models/item"
        model = json.loads((root / "essence_extractor.json").read_text())
        for left in (False, True):
            transform = model["display"]["thirdperson_lefthand" if left else "thirdperson_righthand"]

            def display(point):
                value = tuple(p * scale for p, scale in zip(point, transform["scale"]))
                for axis in (2, 1, 0):
                    value = rotate(value, axis, transform["rotation"][axis] * (-1 if left and axis != 0 else 1))
                return value

            # ItemInHandLayer: Y 180, X -90; raised humanoid arm: X -90.
            needle = rotate(rotate(rotate(display((1, 0, 0)), 1, 180), 0, -90), 0, -90)
            self.assertLess(needle[2], -0.8, "Needle must point forward, not into the player")
            grip = display((3.8 - 8, 4.8 - 8, 0))
            for coordinate, translation in zip(grip, transform["translation"]):
                self.assertAlmostEqual(coordinate + translation, 0, places=4)

    def test_injector_liquid_stays_inside_vial_through_extraction(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS / "models/item/essence"
        for tier in range(1, 5):
            previous_height = 0
            for frame in range(25):
                model = json.loads((root / f"syringe_extract_{tier}_{frame:02d}.json").read_text(encoding="utf-8"))
                elements = model["elements"]
                liquid = [e for e in elements if e["faces"]["north"]["texture"] == "#essence"]
                if not liquid:
                    self.assertEqual(previous_height, 0)
                    continue
                self.assertEqual(len(liquid), 1)
                fill = liquid[0]
                height = fill["to"][1] - fill["from"][1]
                self.assertGreaterEqual(height, previous_height)
                previous_height = height
                dx = elements[0]["from"][0] - injector_geometry.body()[0]["from"][0]
                self.assertGreater(fill["from"][0] - dx, 3.32)
                self.assertLess(fill["to"][0] - dx, 4.88)
                self.assertGreater(fill["from"][1], 10.2)
                self.assertLess(fill["to"][1], 12.96)
                self.assertGreater(fill["from"][2], 7.22)
                self.assertLess(fill["to"][2], 8.78)
            self.assertAlmostEqual(previous_height, 2.68)

    def test_simulation_worker_clothing_preserves_face_and_vanilla_uvs(self):
        root = Path(__file__).resolve().parents[1]
        with Image.open(root / "tools/assets/essence/villager_reference.png") as reference, \
                Image.open(root / "tools/assets/essence/simulation_worker_clothing.png") as clothing, \
                Image.open(root / "src/main/resources" / ASSETS / "textures/entity/simulation_worker.png") as actual:
            expected = simulation_worker_texture.texture(reference.convert("RGBA"), clothing.convert("RGBA"))
            self.assertEqual(actual.size, (64, 64))
            self.assertEqual(actual.tobytes(), expected.tobytes())
            self.assertEqual(actual.crop((0, 0, 32, 18)).tobytes(),
                             reference.convert("RGBA").crop((0, 0, 32, 18)).tobytes())
            self.assertEqual(actual.crop((32, 0, 64, 18)).getchannel("A").getextrema(), (0, 0))
            for face in ((22, 26, 30, 38), (6, 44, 14, 64), (48, 26, 52, 34),
                         (44, 42, 52, 46), (4, 26, 8, 38)):
                self.assertEqual(actual.crop(face).getchannel("A").getextrema(), (255, 255))

    def test_essence_tier_variants_only_change_liquid_and_core_center(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources" / ASSETS
        for name in ("raw_creature_essence_vial", "entity_essence"):
            dispatch = json.loads((root / f"items/{name}.json").read_text(encoding="utf-8"))["model"]
            self.assertEqual(dispatch["property"], "trading_cells:essence_tier")
            self.assertEqual([entry["threshold"] for entry in dispatch["entries"]], [1, 2, 3, 4])
            self.assertEqual(dispatch["fallback"], dispatch["entries"][0]["model"])
            with Image.open(root / f"textures/item/essence/{name}.png") as original:
                mask = essence_tier_textures.center_pixels(name, original)
                self.assertGreater(len(mask), 30)
                for tier in range(1, 5):
                    model_name = f"item/essence/{name}_tier_{tier}"
                    self.assertEqual(dispatch["entries"][tier - 1]["model"]["model"], f"trading_cells:{model_name}")
                    model = json.loads((root / f"models/{model_name}.json").read_text(encoding="utf-8"))
                    self.assertEqual(model["textures"]["layer0"], f"trading_cells:{model_name}")
                    with Image.open(root / f"textures/{model_name}.png") as colored:
                        self.assertEqual(colored.size, original.size)
                        if name == "raw_creature_essence_vial":
                            source = Path(__file__).resolve().parent / "assets/essence/inventory_vials" / f"{name}_tier_{tier}.png"
                            with Image.open(source) as authored:
                                self.assertEqual(authored.size, (32, 32))
                                self.assertEqual(colored.tobytes(), authored.convert("RGBA").tobytes())
                            low, high = colored.getchannel("A").getextrema()
                            self.assertEqual(low, 0)
                            self.assertGreaterEqual(high, 254)
                        else:
                            self.assertEqual(colored.getchannel("A").tobytes(), original.getchannel("A").tobytes())
                            self.assertEqual(colored.tobytes(), essence_tier_textures.variant(name, original, tier).tobytes())
                            for y in range(32):
                                for x in range(32):
                                    if (x, y) not in mask:
                                        self.assertEqual(colored.getpixel((x, y)), original.getpixel((x, y)))
                        red, green, blue, _ = colored.getpixel((15, 17))
                        if tier == 1:
                            self.assertGreater(green, max(red, blue))
                        elif tier == 2:
                            self.assertGreater(blue, green)
                            self.assertGreater(green, red)
                        elif tier == 3:
                            self.assertGreater(blue, red)
                            self.assertGreater(red, green)
                        else:
                            self.assertGreater(red, green)
                            self.assertGreater(green, blue)

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

    def test_utility_upgrades_bake_vanilla_icons_without_changing_frames(self):
        with Image.open(BASES / "quarry.png") as source:
            bounds = source.convert("RGBA").getchannel("A").getbbox()
        icons = []
        for family in ("speed", "mob_farm_capacity"):
            base = family_base(family)
            self.assertEqual(base.getchannel("A").getbbox(), bounds)
            icon = utility_emblem(family)
            icons.append(icon.tobytes())
            left, top, right, bottom = icon.getchannel("A").getbbox()
            self.assertTrue(18 <= left < right <= 46 and 18 <= top < bottom <= 46)
            mask = emblem_pixels(family, base)
            for material in MATERIALS:
                result = generated()[ORIGINALS / family / f"{material}_upgrade.png"]
                frame = generic_frame(material)
                for y in range(64):
                    for x in range(64):
                        expected = icon if (x, y) in mask else frame
                        self.assertEqual(result.getpixel((x, y)), expected.getpixel((x, y)))
        self.assertNotEqual(icons[0], icons[1], "Clock and bundle remain different vanilla symbols")

    def test_emblem_wood_and_gold_do_not_follow_the_upgrade_material(self):
        for family in ("quarry", "piglin_barter"):
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
        names = [name.removeprefix("mob_farm_") for name in names]
        names = [name.removeprefix("mob_farm_") for name in names]
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
