"""Generate the repeatable models and recipes for the general entity simulation system."""

import argparse
from collections import defaultdict
from copy import deepcopy
import io
import json
from pathlib import Path
from PIL import Image
import injector_geometry
import simulation_worker_texture
import essence_tier_textures

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = "assets/trading_cells"
DATA = "data/trading_cells"
MATERIALS = ("copper", "iron", "gold", "diamond", "netherite")
BASES = ("tier_i", "tier_ii", "tier_iii", "tier_iv")


def block_model(elements):
    return {"parent": "minecraft:block/block", "textures": {
        "base": "minecraft:block/black_concrete",
        "edge": "trading_cells:block/logistics/fluid_pipe/fluid_pipe",
        "particle": "minecraft:block/black_concrete"}, "elements": elements}


def outlined_union(boxes, body):
    """Bake only the union's exterior, then merge coplanar cells into rectangles."""
    voxels = set()
    for start, end in boxes:
        for x in range(round(start[0] * 4), round(end[0] * 4)):
            for y in range(round(start[1] * 4), round(end[1] * 4)):
                for z in range(round(start[2] * 4), round(end[2] * 4)):
                    voxels.add((x, y, z))
    surfaces = defaultdict(set)
    for point in voxels:
        for axis, low, high in ((0, "west", "east"), (1, "down", "up"), (2, "north", "south")):
            u, v = [coordinate for coordinate in range(3) if coordinate != axis]
            for step, face in ((-1, low), (1, high)):
                neighbor = list(point)
                neighbor[axis] += step
                if tuple(neighbor) not in voxels:
                    plane = point[axis] + (step > 0)
                    surfaces[axis, face, plane].add((point[u], point[v]))
    result = []
    for (axis, face, plane), surface in sorted(surfaces.items()):
        u, v = [coordinate for coordinate in range(3) if coordinate != axis]
        regions = defaultdict(set)
        for x, y in surface:
            edge = any((x + dx, y + dy) not in surface for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)))
            regions["edge" if edge else body].add((x, y))
        for texture, remaining in sorted(regions.items()):
            while remaining:
                left, bottom = min(remaining)
                right = left + 1
                while (right, bottom) in remaining:
                    right += 1
                upper = bottom + 1
                while all((x, upper) in remaining for x in range(left, right)):
                    upper += 1
                remaining.difference_update((x, y) for x in range(left, right) for y in range(bottom, upper))
                first, last = [0, 0, 0], [0, 0, 0]
                first[axis] = last[axis] = plane / 4
                first[u], last[u], first[v], last[v] = left / 4, right / 4, bottom / 4, upper / 4
                face_data = {"texture": f"#{texture}"}
                element = {"from": first, "to": last, "faces": {face: face_data}}
                if texture == "edge":
                    # Sample only the pipe's blue conduit; its existing atlas animation is shared.
                    vertical = upper - bottom > right - left
                    face_data["uv"] = [bottom / 4 if vertical else left / 4, 7,
                                       upper / 4 if vertical else right / 4, 9]
                    if vertical:
                        face_data["rotation"] = 90
                    element["shade"] = False
                result.append(element)
    return result


def resources():
    result = {}

    def add(path, value):
        if value.get("type") in ("minecraft:crafting_shaped", "minecraft:smithing_transform"):
            value["show_notification"] = False
        result[path] = json.dumps(value, ensure_ascii=False, indent=2) + "\n"

    def model_item(name, model):
        add(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"trading_cells:{model}"}})

    def entity_item(name, model, renderer):
        add(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:composite", "models": [
            {"type": "minecraft:model", "model": f"trading_cells:{model}"},
            {"type": "minecraft:special", "base": f"trading_cells:{model}",
             "model": {"type": f"trading_cells:{renderer}"}}]}})

    frame_boxes = [([0, 0, 0], [16, 2, 16])]
    for x in (0, 14):
        for z in (0, 14):
            frame_boxes.append(([x, 2, z], [x + 2, 16, z + 2]))
    for z in (0, 14):
        frame_boxes.append(([2, 14, z], [14, 16, z + 2]))
    for x in (0, 14):
        frame_boxes.append(([x, 14, 2], [x + 2, 16, 14]))
    frame_boxes.extend([([8, 2, 4], [14, 3, 12]), ([9, 3, 5], [13, 4.5, 11])])
    frame = outlined_union(frame_boxes, "base")
    add(f"{ASSETS}/models/block/mob_farm.json", block_model(frame))

    table_boxes = [([1, 10, 1], [15, 13, 15]), ([3, 13, 3], [13, 14, 13])]
    for x in (2, 12):
        for z in (2, 12):
            table_boxes.append(([x, 0, z], [x + 2, 10, z + 2]))
    add(f"{ASSETS}/models/block/essence_workbench.json", block_model(outlined_union(table_boxes, "base")))
    add(f"{ASSETS}/models/block/essence_stabilizer.json", {"parent": "minecraft:block/block",
        "textures": {"atlas": "trading_cells:block/essence_stabilizer", "particle": "#atlas"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            face: {"texture": "#atlas", "uv": uv, "cullface": face} for face, uv in {
                "up": [0, 0, 8, 8], "north": [8, 0, 16, 8], "down": [8, 8, 16, 16],
                "south": [0, 8, 8, 16], "east": [0, 8, 8, 16], "west": [0, 8, 8, 16]}.items()}}]})
    for name in ("mob_farm", "essence_workbench", "essence_stabilizer"):
        add(f"{ASSETS}/blockstates/{name}.json", {"variants": {
            f"facing={facing}": {"model": f"trading_cells:block/{name}", "y": index * 90}
            for index, facing in enumerate(("north", "east", "south", "west"))}})
        if name == "mob_farm":
            entity_item(name, f"block/{name}", "block_entity_item")
        else:
            model_item(name, f"block/{name}")
        add(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"trading_cells:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    pedestal = outlined_union([([1, 0, 1], [15, 1.5, 15]), ([3, 1.5, 3], [13, 3.5, 13])], "base")
    module_model = block_model(pedestal)
    module_model["textures"]["particle"] = "#base"
    module_model["display"] = {
        hand: {"rotation": [0, yaw, 0], "translation": [-1, 3.5, 3], "scale": [0.48, 0.48, 0.48]}
        for hand, yaw in (("firstperson_righthand", 135), ("firstperson_lefthand", 225))}
    for hand in ("thirdperson_righthand", "thirdperson_lefthand"):
        module_model["display"][hand] = {"rotation": [70, 0, 0], "translation": [0, 2.75, 3], "scale": [0.4, 0.4, 0.4]}
    add(f"{ASSETS}/models/item/entity_module.json", module_model)
    tiers = []
    for tier, base in enumerate(BASES, 1):
        name = f"{base}_creature_model_base"
        model = deepcopy(module_model)
        model["textures"]["edge"] = f"trading_cells:block/essence/tier_{tier}_edge"
        add(f"{ASSETS}/textures/block/essence/tier_{tier}_edge.png.mcmeta",
            {"animation": {"width": 32, "height": 32, "frametime": 4, "interpolate": False}})
        add(f"{ASSETS}/models/item/{name}.json", model)
        model_item(name, f"item/{name}")
        tiers.append({"threshold": tier, "model": {"type": "minecraft:composite", "models": [
            {"type": "minecraft:model", "model": f"trading_cells:item/{name}"},
            {"type": "minecraft:special", "base": "trading_cells:item/entity_module",
             "model": {"type": "trading_cells:entity_module"}}]}})
        add(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": ["GBG", "BSB", "GBG"], "key": {"G": "minecraft:" + (
                "emerald", "diamond", "netherite_ingot", "nether_star")[tier - 1],
                "B": "minecraft:black_concrete", "S": (
                    "trading_cells:storm_shard" if tier == 1
                    else f"trading_cells:tier_{('i', 'ii', 'iii')[tier - 2]}_creature_model_base")},
            "result": {"id": f"trading_cells:{name}", "count": 1}})
        add(f"{DATA}/recipe/essence_stabilization_{tier}.json", {"type": "trading_cells:essence_stabilization",
            "tier": tier, "duration": 100, "amethyst": {"ingredient": "minecraft:amethyst_shard", "count": 2 ** (tier - 1)},
            "reagent": {"ingredient": "minecraft:" + ("redstone", "glowstone_dust", "ender_pearl", "dragon_breath")[tier - 1], "count": 2}})
    add(f"{ASSETS}/items/entity_module.json", {"model": {"type": "minecraft:range_dispatch",
        "property": "trading_cells:essence_tier", "entries": tiers, "fallback": tiers[0]["model"]}})
    for name in ("entity_essence", "essence_extractor", "empty_essence_vial", "raw_creature_essence_vial"):
        add(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated",
            "textures": {"layer0": f"trading_cells:item/essence/{name}"}})
        model_item(name, f"item/{name}")

    for name in ("raw_creature_essence_vial", "entity_essence"):
        variants = []
        for tier in range(1, 5):
            model = f"item/essence/{name}_tier_{tier}"
            add(f"{ASSETS}/models/{model}.json", {"parent": "minecraft:item/generated",
                "textures": {"layer0": f"trading_cells:{model}"}})
            variants.append({"threshold": tier, "model": {"type": "minecraft:model", "model": f"trading_cells:{model}"}})
        add(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:range_dispatch",
            "property": "trading_cells:essence_tier", "entries": variants, "fallback": variants[0]["model"]}})

    syringe_display = {
        "firstperson_righthand": {"rotation": [0, 70, 0], "translation": [0, 2, -1], "scale": [0.8] * 3},
        "firstperson_lefthand": {"rotation": [0, -110, 0], "translation": [0, 2, -1], "scale": [0.8] * 3},
        "thirdperson_righthand": {"rotation": [0, 90, 90], "translation": [0, 3, 0], "scale": [0.85] * 3},
        "thirdperson_lefthand": {"rotation": [0, -90, -90], "translation": [0, 3, 0], "scale": [0.85] * 3},
        "gui": {"rotation": [15, -25, 0], "scale": [1, 1, 1]},
    }
    textures = {"atlas": injector_geometry.ATLAS, "particle": "#atlas"}
    body, vial = injector_geometry.body(), injector_geometry.vial()
    shifted = injector_geometry.shifted

    def syringe_model(elements, extra=None):
        return {"parent": "minecraft:item/generated", "textures": textures | (extra or {}),
                "display": syringe_display, "elements": elements}

    add(f"{ASSETS}/models/item/essence_extractor.json", syringe_model(body))
    syringe = {"type": "minecraft:model", "model": "trading_cells:item/essence_extractor"}
    loading = []
    for frame in range(33):
        progress = frame / 32
        if progress < 0.4:
            fraction = progress / 0.4
            fraction = fraction * fraction * (3 - 2 * fraction)
            dx, dy = 5 * (1 - fraction), -6 + 10 * fraction
        elif progress < 0.75:
            dx, dy = 0, 4 - 3.25 * (progress - 0.4) / 0.35
        else:
            dx, dy = 0, 0.75 * (1 - progress) / 0.25
        name = f"syringe_vial_{frame:02d}"
        add(f"{ASSETS}/models/item/essence/{name}.json", syringe_model(shifted(vial, dx, dy)))
        loading.append({"threshold": progress, "model": {"type": "minecraft:composite", "models": [
            syringe, {"type": "minecraft:model", "model": f"trading_cells:item/essence/{name}"}]}})
    extracting = []
    for tier in range(1, 5):
        for frame in range(25):
            progress = frame / 24
            advance = min(1, progress / 0.15) * min(1, (1 - progress) / 0.2)
            advance = advance * advance * (3 - 2 * advance)
            fill = max(0, min(1, (progress - 0.15) / 0.7))
            liquid = injector_geometry.liquid(fill)
            name = f"syringe_extract_{tier}_{frame:02d}"
            # Submit the opaque fill before the translucent vial walls.
            add(f"{ASSETS}/models/item/essence/{name}.json", syringe_model(shifted(body + liquid + vial, advance * 2),
                {"essence": f"trading_cells:item/essence/tier_{tier}_liquid"}))
            extracting.append({"threshold": (tier - 1) * 2 + progress,
                               "model": {"type": "minecraft:model", "model": f"trading_cells:item/essence/{name}"}})
    add(f"{ASSETS}/items/essence_extractor.json", {"model": {"type": "minecraft:range_dispatch",
        "property": "trading_cells:syringe_extraction", "entries": extracting, "fallback": {"type": "minecraft:range_dispatch",
        "property": "trading_cells:syringe_load", "fallback": syringe, "entries": loading}}})

    for family in ("speed", "capacity"):
        for index, material in enumerate(MATERIALS):
            name = f"mob_farm_{family}_{material}_upgrade"
            add(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {
                "layer0": f"trading_cells:item/upgrades/mob_farm_{family}/{material}_upgrade"}})
            model_item(name, f"item/{name}")
            previous = "minecraft:diamond_sword" if index == 0 \
                else f"trading_cells:mob_farm_{family}_{MATERIALS[index - 1]}_upgrade"
            if material == "netherite":
                add(f"{DATA}/recipe/{name}.json", {"type": "minecraft:smithing_transform",
                    "template": "minecraft:netherite_upgrade_smithing_template", "base": previous,
                    "addition": "minecraft:netherite_block", "result": {"id": f"trading_cells:{name}", "count": 1}})
            else:
                add(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc",
                    "pattern": ["CBC", "BSB", "CBC"], "key": {
                        "C": "minecraft:popped_chorus_fruit" if material == "diamond" else
                             "minecraft:clock" if family == "speed" else "minecraft:chest",
                        "B": f"minecraft:{material}_block", "S": previous},
                    "result": {"id": f"trading_cells:{name}", "count": 1}})

    add(f"{DATA}/recipe/essence_workbench.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["LBL", "BCB", "BSB"], "key": {"B": "minecraft:black_concrete", "L": "minecraft:lapis_block",
        "C": "trading_cells:experience_storage", "S": "trading_cells:storm_shard"},
        "result": {"id": "trading_cells:essence_workbench", "count": 1}})
    add(f"{DATA}/recipe/essence_extractor.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["  A", " G ", "I  "], "key": {"I": "minecraft:iron_ingot", "A": "minecraft:amethyst_shard",
        "G": "minecraft:glass_pane"}, "result": {"id": "trading_cells:essence_extractor", "count": 1}})
    add(f"{DATA}/recipe/empty_essence_vial.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["A", "G", "G"], "key": {"G": "minecraft:glass", "A": "minecraft:amethyst_shard"},
        "result": {"id": "trading_cells:empty_essence_vial", "count": 4}})
    add(f"{DATA}/recipe/essence_stabilizer.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["LGL", "BDB", "LBL"], "key": {"L": "minecraft:lapis_block", "G": "minecraft:glass",
        "B": "minecraft:black_concrete", "D": "minecraft:diamond_block"},
        "result": {"id": "trading_cells:essence_stabilizer", "count": 1}})
    farm_inputs = ("minecraft:lapis_block", "minecraft:black_concrete", "minecraft:lapis_block",
                   "minecraft:black_concrete", "trading_cells:experience_storage", "minecraft:black_concrete",
                   "minecraft:lapis_block", "trading_cells:storm_shard", "minecraft:lapis_block")
    add(f"{DATA}/recipe/mob_farm_infusion.json", {"type": "trading_cells:arcane_infusion", "category": "production",
        "ingredients": [{"ingredient": item, "count": 1} for item in farm_inputs], "experience": 50_000,
        "result": {"type": "item", "item": "trading_cells:mob_farm"}})
    add(f"{DATA}/recipe/experience_bottle_infusion.json", {"type": "trading_cells:arcane_infusion", "category": "misc",
        "shapeless": True,
        "ingredients": [{"ingredient": "minecraft:glass_bottle", "count": 1} if slot == 4 else {"empty": True}
                        for slot in range(9)], "experience": 11,
        "result": {"type": "item", "item": "minecraft:experience_bottle"}})
    return result


def texture_resources():
    """Preserve generated silhouettes; tier variants change palette only."""
    result = {}
    sources = Path(__file__).resolve().parent / "assets/essence"
    with Image.open(sources / "villager_reference.png") as reference, \
            Image.open(sources / "simulation_worker_clothing.png") as clothing:
        worker = simulation_worker_texture.texture(reference.convert("RGBA"), clothing.convert("RGBA"))
        stream = io.BytesIO()
        worker.save(stream, format="PNG")
        result[f"{ASSETS}/textures/entity/simulation_worker.png"] = stream.getvalue()
    with Image.open(sources / "injector_atlas.png") as source:
        atlas = source.convert("RGBA").resize((128, 128), Image.Resampling.NEAREST)
        stream = io.BytesIO()
        atlas.save(stream, format="PNG")
        result[f"{ASSETS}/textures/item/essence/injector_atlas.png"] = stream.getvalue()
    for name in ("essence_extractor", "empty_essence_vial", "raw_creature_essence_vial", "entity_essence", "essence_stabilizer"):
        size = 128 if name == "essence_stabilizer" else 32
        image = Image.open(sources / f"{name}.png").convert("RGBA").resize((size, size), Image.Resampling.NEAREST)
        stream = io.BytesIO()
        image.save(stream, format="PNG")
        path = "block/essence_stabilizer" if name == "essence_stabilizer" else f"item/essence/{name}"
        result[f"{ASSETS}/textures/{path}.png"] = stream.getvalue()
        if name in ("raw_creature_essence_vial", "entity_essence"):
            for tier in range(1, 5):
                variant = essence_tier_textures.variant(name, image, tier)
                stream = io.BytesIO()
                variant.save(stream, format="PNG")
                result[f"{ASSETS}/textures/item/essence/{name}_tier_{tier}.png"] = stream.getvalue()
    pipe = Image.open(ROOT / f"{ASSETS}/textures/block/logistics/fluid_pipe/fluid_pipe.png").convert("RGBA")
    for tier, (base, highlight) in enumerate(essence_tier_textures.PALETTES, 1):
        variant = pipe.copy()
        values = [max(pipe.getpixel((x, y))[:3]) for y in range(pipe.height) if 14 <= y % 32 < 18 for x in range(pipe.width)]
        low, high = min(values), max(values)
        for y in range(pipe.height):
            if not 14 <= y % 32 < 18:
                continue
            for x in range(pipe.width):
                pixel = pipe.getpixel((x, y))
                step = round(3 * (max(pixel[:3]) - low) / max(1, high - low))
                palette = [tuple(round(c * 0.55) for c in base), base,
                           tuple((c + h) // 2 for c, h in zip(base, highlight)), highlight]
                variant.putpixel((x, y), (*palette[step], pixel[3]))
        stream = io.BytesIO()
        variant.save(stream, format="PNG")
        result[f"{ASSETS}/textures/block/essence/tier_{tier}_edge.png"] = stream.getvalue()
        # Item models cannot combine the block and item atlases in Minecraft 26.2.
        result[f"{ASSETS}/textures/item/essence/tier_{tier}_liquid.png"] = stream.getvalue()
        result[f"{ASSETS}/textures/item/essence/tier_{tier}_liquid.png.mcmeta"] = (
            ROOT / f"{ASSETS}/textures/block/essence/tier_{tier}_edge.png.mcmeta").read_bytes()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    stale = []
    for relative, content in resources().items():
        path = ROOT / relative
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8", newline="\n")
        if args.check and (not path.is_file() or path.read_text(encoding="utf-8") != content):
            stale.append(relative)
    for relative, content in texture_resources().items():
        path = ROOT / relative
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        if args.check and (not path.is_file() or path.read_bytes() != content):
            stale.append(relative)
    if stale:
        raise SystemExit("Stale simulation resources:\n" + "\n".join(stale))
    print(f"Simulation resources verified: {len(resources())}")


if __name__ == "__main__":
    main()
