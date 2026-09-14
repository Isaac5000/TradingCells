"""Generate the repeatable models and recipes for the general entity simulation system."""

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = "assets/trading_cells"
DATA = "data/trading_cells"
MATERIALS = ("copper", "iron", "gold", "diamond", "netherite")


def cube(start, end, texture):
    return {"from": start, "to": end, "faces": {
        face: {"texture": f"#{texture}"} for face in ("north", "south", "east", "west", "up", "down")}}


def block_model(elements):
    return {"parent": "minecraft:block/block", "textures": {
        "frame": "trading_cells:block/logistics/pipe_base",
        "panel": "trading_cells:block/logistics/pipe_cap",
        "particle": "trading_cells:block/logistics/pipe_base"}, "elements": elements}


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

    frame = [cube([0, 0, 0], [16, 2, 16], "frame")]
    for x in (0, 14):
        for z in (0, 14):
            frame.append(cube([x, 2, z], [x + 2, 16, z + 2], "frame"))
    for z in (0, 14):
        frame.append(cube([2, 14, z], [14, 16, z + 2], "frame"))
    for x in (0, 14):
        frame.append(cube([x, 14, 2], [x + 2, 16, 14], "frame"))
    frame.extend([cube([8, 2, 4], [14, 3, 12], "frame"), cube([9, 3, 5], [13, 4.5, 11], "frame")])
    add(f"{ASSETS}/models/block/mob_farm.json", block_model(frame))

    table = [cube([1, 10, 1], [15, 13, 15], "frame"), cube([3, 13, 3], [13, 14, 13], "panel")]
    for x in (2, 12):
        for z in (2, 12):
            table.append(cube([x, 0, z], [x + 2, 10, z + 2], "frame"))
    add(f"{ASSETS}/models/block/essence_workbench.json", block_model(table))
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

    pedestal = [cube([1, 0, 1], [15, 1.5, 15], "frame"), cube([3, 1.5, 3], [13, 3.5, 13], "frame")]
    module_model = block_model(pedestal)
    module_model["display"] = {
        hand: {"rotation": [0, yaw, 0], "translation": [0, 5.5, 0], "scale": [0.32, 0.32, 0.32]}
        for hand, yaw in (("firstperson_righthand", 135), ("firstperson_lefthand", 225))}
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
        "pattern": ["IAI", "RCR", "I I"], "key": {"I": "minecraft:iron_ingot", "A": "minecraft:amethyst_block",
        "R": "minecraft:redstone", "C": "minecraft:crafting_table"}, "result": {"id": "trading_cells:essence_workbench", "count": 1}})
    add(f"{DATA}/recipe/essence_extractor.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": [" IA", " BR", "I  "], "key": {"I": "minecraft:iron_ingot", "A": "minecraft:amethyst_shard",
        "B": "minecraft:glass_bottle", "R": "minecraft:redstone"}, "result": {"id": "trading_cells:essence_extractor", "count": 1}})
    farm_inputs = ("minecraft:iron_block", "minecraft:diamond_sword", "minecraft:iron_block",
                   "minecraft:iron_bars", "trading_cells:experience_storage", "minecraft:iron_bars",
                   "minecraft:quartz_block", "minecraft:amethyst_block", "minecraft:quartz_block")
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
