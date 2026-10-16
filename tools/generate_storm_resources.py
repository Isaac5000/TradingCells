"""Export storm models/data. Art is shared; no per-frame textures or pixel-golden tests."""
import argparse
import json
from pathlib import Path
from PIL import Image
from storm_mesh import export as export_mesh

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/trading_cells"
DATA = ROOT / "data/trading_cells"
ATLAS = "trading_cells:item/storm/materials"


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def cube(start, end, tile, **extra):
    x, y = tile % 4 * 4, tile // 4 * 8
    faces = {}
    for side, axes in (("north", (0, 1)), ("south", (0, 1)), ("west", (2, 1)),
                       ("east", (2, 1)), ("up", (0, 2)), ("down", (0, 2))):
        width, height = (end[a] - start[a] for a in axes)
        scale = min(1, 8 / max(width, height))
        faces[side] = {"texture": "#atlas", "uv": [x, y, x + width * scale / 2, y + height * scale]}
    return {"from": start, "to": end, "faces": faces, **extra}


def rounded(axis, center, low, high, radius, tile, capped=True):
    return [{"prism": (axis, center, low, high, radius, tile)}]


def weapon_model(name, elements, display):
    export_mesh(ASSETS / f"models/item/storm/{name}.obj", elements)
    write(ASSETS / f"models/item/storm/{name}.json", {
        "loader": "neoforge:obj", "model": f"trading_cells:models/item/storm/{name}.obj",
        "automatic_culling": False, "emissive_ambient": False,
        "textures": {"atlas": "trading_cells:item/storm/industrial_materials", "particle": "trading_cells:item/storm/industrial_materials"},
        "display": display})


DISPLAY = {
    "gui": {"rotation": [25, -40, 0], "translation": [0, 0, 0], "scale": [0.75] * 3},
    "ground": {"translation": [0, 2, 0], "scale": [0.4] * 3},
    "fixed": {"rotation": [0, 90, 0], "scale": [0.7] * 3},
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3, 1], "scale": [0.8] * 3},
    "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 3, 1], "scale": [0.8] * 3},
    "firstperson_righthand": {"rotation": [0, 90, -10], "translation": [1, 2, 0], "scale": [0.7] * 3},
    "firstperson_lefthand": {"rotation": [0, -90, 10], "translation": [1, 2, 0], "scale": [0.7] * 3},
}


def model(elements, display=None, atlas=ATLAS):
    # Omit fully buried or joined faces, avoiding coplanar surfaces at joints.
    for element in elements:
        if "rotation" in element:
            continue
        for side, axis, positive in (("west", 0, False), ("east", 0, True),
                                     ("down", 1, False), ("up", 1, True),
                                     ("north", 2, False), ("south", 2, True)):
            plane = element["to" if positive else "from"][axis]
            for other in elements:
                if other is element or "rotation" in other or len(other["faces"]) < 6:
                    continue
                buried = other["from"][axis] < plane < other["to"][axis]
                joined = plane == other["from" if positive else "to"][axis]
                if (buried or joined) and all(other["from"][i] <= element["from"][i]
                        and other["to"][i] >= element["to"][i] for i in range(3) if i != axis):
                    element["faces"].pop(side, None)
                    break
    return {"textures": {"atlas": atlas, "particle": atlas}, "display": display or DISPLAY, "elements": elements}


def capacitor(offset=0, height=0, advanced=False):
    result = [cube([6.5 + offset, 2 + height, 6.5], [9.5 + offset, 8 + height, 9.5], 2 if advanced else 5)]
    for y in (1.5, 8):
        result.append(cube([6 + offset, y + height, 6], [10 + offset, y + 0.5 + height, 10], 1))
    result.append(cube([7 + offset, 8.5 + height, 7], [9 + offset, 9 + height, 9], 3))
    return result


def loaded_cell(result, x, y, z, loading):
    if loading is None:
        return
    x -= (1 - loading) * 7
    result.extend(rounded(1, [x, y, z], y - 2, y + 2, 0.85, 3))
    for height in (y - 2.2, y + 1.9):
        result.extend(rounded(1, [x, y, z], height, height + 0.3, 0.95, 1))


def baton(loading=None):
    result = rounded(1, [8, 0, 8], 0, 6.5, 1.6, 4)
    result += rounded(1, [8, 0, 8], -0.5, 0, 1.9, 1)
    result += rounded(1, [8, 0, 8], 6.5, 7.4, 2.2, 1)
    result += rounded(1, [8, 0, 8], 7.4, 23, 1.2, 0)
    for y in (8, 13, 18, 22.5):
        result += rounded(1, [8, 0, 8], y, y + 0.5, 1.7, 1)
    # External electrode cage and copper feed cable, like the industrial reference.
    for x, z in ((6.4, 8), (9.6, 8), (8, 6.4), (8, 9.6)):
        result += rounded(1, [x, 0, z], 8, 23, 0.18, 1)
    result += rounded(1, [8, 0, 8], 23, 24, 0.85, 1)
    result.append(cube([9.5, 3, 7.6], [9.8, 7.8, 8], 5))
    result.append(cube([9.62, 4.7, 8.1], [9.88, 6.4, 8.7], 2))
    result.append(cube([5.8, 1.3, 7], [6.5, 5.7, 9], 7))
    loaded_cell(result, 5.5, 3.5, 8, loading)
    return result


def welder(active=0, loading=None):
    result = rounded(1, [8, 0, 11.5], 0, 7, 1.65, 4)
    result += rounded(2, [8, 9.5, 0], 5.5, 16.5, 2.8, 2)
    for z in (5, 10.5, 16.5):
        result += rounded(2, [8, 9.5, 0], z, z + 0.5, 2.95, 1)
    result += rounded(2, [8, 9.5, 0], -3, 5, 1.9, 0)
    result += rounded(2, [8, 9.5, 0], -7, -3, 1.3, 3)
    for z in (-8, -3.3):
        result += rounded(2, [8, 9.5, 0], z, z + 0.7, 2, 1)
    for x, y in ((6.4, 9.5), (9.6, 9.5), (8, 7.9), (8, 11.1)):
        result += rounded(2, [x, y, 0], -7.3, -3.3, 0.17, 1)
    result += rounded(2, [8, 9.5, 0], -11.5, -8, 1.05, 0)
    result += rounded(2, [8, 9.5, 0], -12, -11.5, 1.3, 5)
    # Split fork electrode, not a gun barrel.
    for x, angle in ((6.8, -22.5), (9.2, 22.5)):
        result.append(cube([x - 0.3, 9, -14.5], [x + 0.3, 10, -11.5], 1,
                          rotation={"origin": [x, 9.5, -11.5], "axis": "y", "angle": angle}))
    result += rounded(2, [8, 13.8, 0], -1, 7, 0.65, 4)
    for z in (-0.8, 6.1):
        result.append(cube([7.5, 11, z], [8.5, 13.8, z + 0.7], 1))
    # Rear carry handle and insulated pistol grip.
    for z in (11.2, 14.8):
        result.append(cube([7.5, 12.3, z], [8.5, 15.5, z + 0.7], 1))
    result += rounded(2, [8, 15.5, 0], 11, 15.7, 0.65, 4)
    result.append(cube([7.6, 4.3, 7.5], [8.4, 6.8, 8.3], 1,
                       rotation={"origin": [8, 6.8, 8], "axis": "x", "angle": -22.5}))
    result.append(cube([4.5, 7.2, 8], [5.2, 11.8, 10.4], 7))
    loaded_cell(result, 4.3, 9.5, 9.2, loading)
    if active:
        result.append(cube([7.5, 9, -12.5 - active * 0.1], [8.5, 10, -12.1], 3, light_emission=15))
    return result


def item_ref(name):
    return {"type": "minecraft:model", "model": "trading_cells:item/storm/" + name}


def geometry():
    write(ASSETS / "models/block/storm_station.json", {"parent": "minecraft:block/orientable_with_bottom",
          "textures": {face: f"trading_cells:block/storm/station_{face}" for face in ("front", "top", "side", "bottom")}})
    write(ASSETS / "blockstates/storm_station.json", {"variants": {
        f"facing={side}": {"model": "trading_cells:block/storm_station", "y": rotation}
        for side, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    for name in ("storm_station",):
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"trading_cells:block/{name}"}})
    write(ASSETS / "models/item/storm/voltaic_capacitor.json", model(capacitor(height=2, advanced=True)))
    write(ASSETS / "items/voltaic_capacitor.json", {"model": item_ref("voltaic_capacitor")})
    baton_display = dict(DISPLAY)
    baton_display["gui"] = {"rotation": [15, -35, -35], "translation": [0, -2, 0], "scale": [0.6] * 3}
    for hand in ("right", "left"):
        baton_display[f"thirdperson_{hand}hand"] = {"rotation": [0, 0, 0], "translation": [0, 3.5, 0], "scale": [0.75] * 3}
        baton_display[f"firstperson_{hand}hand"] = {"rotation": [0, 0, 0], "translation": [0, -1, 1], "scale": [0.7] * 3}
    welder_display = dict(DISPLAY)
    welder_display["gui"] = {"rotation": [25, 135, 0], "translation": [2, -1, 0], "scale": [0.48] * 3}
    for hand, sign in (("right", 1), ("left", -1)):
        welder_display[f"thirdperson_{hand}hand"] = {"rotation": [0, 0, 0], "translation": [0, 1.5, -2], "scale": [0.6] * 3}
        welder_display[f"firstperson_{hand}hand"] = {"rotation": [0, 0, 0], "translation": [0, -1, 1], "scale": [0.5] * 3}
    for name, builder, display in (("electric_arc_welder", welder, welder_display), ("electric_baton", baton, baton_display)):
        weapon_model(name, builder(), display)
        weapon_model(name + "_loaded", builder(loading=1), display)
        entries = [{"threshold": 1, "model": item_ref(name + "_loaded")}]
        if name == "electric_arc_welder":
            for step in range(3):
                frame = f"{name}_active_{step}"
                weapon_model(frame, welder(step + 1, 1), display)
                entries.append({"threshold": 2 + step / 3, "model": item_ref(frame)})
        for step in range(11):
            frame = f"{name}_reload_{step}"
            weapon_model(frame, builder(loading=step / 10), display)
            entries.append({"threshold": 10 + step / 10, "model": item_ref(frame)})
        dispatch = {"type": "minecraft:range_dispatch", "property": "trading_cells:storm_weapon", "fallback": item_ref(name), "entries": entries}
        write(ASSETS / f"items/{name}.json", {"model": {"type": "trading_cells:storm_beam", "model": dispatch} if name == "electric_arc_welder" else dispatch})


def data():
    recipes = {
        "voltaic_capacitor": ([" R ", "DPD", " L "], {"R": "redstone_block", "D": "diamond", "P": "copper_block", "L": "copper_ingot"}),
        "electric_baton": (["I", "L", "S"], {"I": "iron_ingot", "L": "copper_ingot", "S": "stick"}),
        "electric_arc_welder": (["ICI", "LRL", " IB"], {"C": "copper_block", "I": "iron_ingot", "L": "copper_ingot", "R": "trading_cells:storm_shard", "B": "black_concrete"}),
        "storm_station": (["ILI", "CFC", "IRI"], {"I": "iron_ingot", "L": "lightning_rod", "C": "copper_block", "F": "furnace", "R": "redstone"}),
    }
    for name, (pattern, keys) in recipes.items():
        write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
              "pattern": pattern, "key": {key: value if ":" in value else "minecraft:" + value for key, value in keys.items()},
              "result": {"id": "trading_cells:" + name, "count": 1}, "show_notification": False})
    tag = ROOT / "data/minecraft/tags/block/mineable/pickaxe.json"
    mining = json.loads(tag.read_text(encoding="utf-8"))
    for name in ("storm_station",):
        if "trading_cells:" + name not in mining["values"]:
            mining["values"].append("trading_cells:" + name)
    write(tag, mining)
    write(DATA / "loot_table/blocks/storm_station.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "trading_cells:storm_station"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for name, values in {
        "voltaic_capacitor": ("Condensador voltaico", "Voltaic Capacitor"),
        "electric_baton": ("Porra eléctrica", "Electric Baton"), "electric_arc_welder": ("Soldador de arco eléctrico", "Electric Arc Welder"),
        "storm_station": ("Estación de tormentas", "Storm Station")}.items():
        for locale, value in zip(("es_es", "en_us"), values):
            path = ASSETS / f"lang/{locale}.json"
            content = json.loads(path.read_text(encoding="utf-8"))
            content[("block" if name == "storm_station" else "item") + ".trading_cells." + name] = value
            write(path, content)
    texts = {
        "tooltip.trading_cells.storm.charge": ("Cargas: %s / %s", "Charges: %s / %s"),
        "tooltip.trading_cells.storm.baton": ("1 de daño sin carga; 16 con carga. Consume una carga por golpe.", "1 damage uncharged; 16 when charged. One charge per hit."),
        "tooltip.trading_cells.storm.welder": ("Haz continuo: una carga por segundo, 4 impactos por segundo.", "Continuous beam: one charge per second, 4 hits per second."),
        "tooltip.trading_cells.storm.unloaded": ("Sin condensador cargado.", "No charged capacitor loaded."),
        "tooltip.trading_cells.storm.reload": ("%s: recargar. Mantener: expulsar condensador.", "%s: reload. Hold: eject capacitor."),
        "key.trading_cells.storm_reload": ("Recargar / expulsar condensador", "Reload / eject capacitor"),
        "key.category.trading_cells.equipment": ("Trading Cells: equipo", "Trading Cells: equipment"),
        "subtitles.trading_cells.electric_arc_loop": ("Arco eléctrico crepita", "Electric arc crackles"),
        "subtitles.trading_cells.electric_baton_hit": ("Descarga de porra eléctrica", "Electric baton discharge"),
        "death.attack.trading_cells.storm_arc": ("%1$s recibió una descarga eléctrica", "%1$s was electrocuted"),
        "death.attack.trading_cells.storm_arc.player": ("%1$s recibió una descarga de %2$s", "%1$s was electrocuted by %2$s"),
    }
    for locale, index in (("es_es", 0), ("en_us", 1)):
        path = ASSETS / f"lang/{locale}.json"
        content = json.loads(path.read_text(encoding="utf-8"))
        content.pop("item.trading_cells.advanced_voltaic_capacitor", None)
        content.update({key: value[index] for key, value in texts.items()})
        write(path, content)


def artwork(source):
    atlas = Image.open(source).convert("RGB").resize((128, 64), Image.Resampling.LANCZOS)
    destination = ASSETS / "textures/item/storm/materials.png"
    destination.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(destination, optimize=True)


def industrial_artwork():
    source = ROOT.parents[2] / "tools/assets/storm/industrial_materials.png"
    with Image.open(source) as atlas:
        atlas.convert("RGB").resize((128, 64), Image.Resampling.LANCZOS).save(
            ASSETS / "textures/item/storm/industrial_materials.png", optimize=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artwork", type=Path)
    args = parser.parse_args()
    if args.artwork:
        artwork(args.artwork)
    industrial_artwork()
    geometry()
    data()
