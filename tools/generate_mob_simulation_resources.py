"""Generate the repeatable models and recipes for the general entity simulation system."""

import argparse
from collections import defaultdict
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = "assets/trading_cells"
DATA = "data/trading_cells"
MATERIALS = ("copper", "iron", "gold", "diamond", "netherite")


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
        if value.get("type") == "minecraft:crafting_shaped":
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
    for name in ("mob_farm", "essence_workbench"):
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
    entity_item("entity_module", "item/entity_module", "entity_module")
    for name, texture in (("entity_essence", "minecraft:item/experience_bottle"),
                          ("essence_extractor", "trading_cells:item/pipe_target_selector")):
        add(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": texture}})
        model_item(name, f"item/{name}")

    for family in ("speed", "capacity"):
        for index, material in enumerate(MATERIALS):
            name = f"mob_farm_{family}_{material}_upgrade"
            add(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {
                "layer0": f"trading_cells:item/upgrades/mob_farm_{family}/{material}_upgrade"}})
            model_item(name, f"item/{name}")
            ingot = "diamond" if material == "diamond" else f"{material}_ingot"
            previous = ("minecraft:clock" if family == "speed" else "minecraft:amethyst_block") if index == 0 \
                else f"trading_cells:mob_farm_{family}_{MATERIALS[index - 1]}_upgrade"
            add(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc",
                "pattern": ["CMC", "MSM", "CPC"], "key": {
                    "C": "minecraft:blaze_powder" if material == "netherite" else "minecraft:popped_chorus_fruit",
                    "M": f"minecraft:{ingot}", "S": "minecraft:iron_sword" if family == "speed" else "minecraft:diamond_sword",
                    "P": previous}, "result": {"id": f"trading_cells:{name}", "count": 1}})

    add(f"{DATA}/recipe/essence_workbench.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["BBB", "BCB", "B B"], "key": {"B": "minecraft:black_concrete",
        "C": "minecraft:crafting_table"}, "result": {"id": "trading_cells:essence_workbench", "count": 1}})
    add(f"{DATA}/recipe/essence_extractor.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": [" IA", " BR", "I  "], "key": {"I": "minecraft:iron_ingot", "A": "minecraft:amethyst_shard",
        "B": "minecraft:glass_bottle", "R": "minecraft:redstone"}, "result": {"id": "trading_cells:essence_extractor", "count": 1}})
    farm_inputs = ("minecraft:iron_block", "minecraft:black_concrete", "minecraft:iron_block",
                   "minecraft:black_concrete", "trading_cells:experience_storage", "minecraft:black_concrete",
                   "minecraft:quartz_block", "minecraft:black_concrete", "minecraft:quartz_block")
    add(f"{DATA}/recipe/mob_farm_infusion.json", {"type": "trading_cells:arcane_infusion", "category": "production",
        "ingredients": [{"ingredient": item, "count": 1} for item in farm_inputs], "experience": 50_000,
        "result": {"type": "item", "item": "trading_cells:mob_farm"}})
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
    if stale:
        raise SystemExit("Stale simulation resources:\n" + "\n".join(stale))
    print(f"Simulation resources verified: {len(resources())}")


if __name__ == "__main__":
    main()
