"""Deterministic models and recipes for logistics; validate with --check."""
import argparse
import json
from io import BytesIO
from pathlib import Path
from PIL import Image
from zipfile import ZipFile
from generate_pipe_textures import validate as validate_pipe_textures

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = "assets/trading_cells"
DATA = "data/trading_cells"
DIRECTIONS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1),
              "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
# Minecraft face UV axes/signs. The material phase always advances along positive world axes.
FACE_AXES = {"up": (0, 2, 1, 1), "down": (0, 2, 1, -1), "north": (0, 1, -1, -1),
             "south": (0, 1, 1, -1), "west": (2, 1, 1, -1), "east": (2, 1, -1, -1)}


def uv_interval(cube, axis, sign, transverse=False):
    values = [cube["from"][axis], cube["to"][axis]]
    if transverse:
        values = [(value - 5) * 16 / 6 for value in values]
    return values if sign > 0 else values[::-1]


def core_edges(side):
    u, v, _, _ = FACE_AXES[side]
    return tuple(next(name for name, vector in DIRECTIONS.items() if vector[axis] == sign)
                 for axis, sign in ((v, -1), (u, 1), (v, 1), (u, -1)))


def resources():
    result = {}

    def put(path, value):
        result[path] = (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

    def element(start, end, texture="#pipe"):
        return {"from": start, "to": end, "faces": {
            side: {"texture": texture} for side in ("down", "up", "north", "south", "west", "east")
        }}

    def model(name, texture, elements, display=None):
        kind = name.split("_pipe", 1)[0]
        textures = {"pipe": texture, "frame": "trading_cells:block/logistics/pipe_base",
                    "particle": "trading_cells:block/logistics/pipe_base"}
        elements = json.loads(json.dumps(elements))
        for cube in elements:
            for side, face in cube["faces"].items():
                face.setdefault("uv", [0, 0, 16, 16])
                face.setdefault("rotation", 90 if side in ("up", "down") else 0)
        put(f"{ASSETS}/models/block/logistics/{kind}_pipe/{name}.json", {
            "parent": "minecraft:block/block", "textures": textures,
            "elements": elements,
            **({"display": display} if display is not None else {}),
        })

    def item(name, model_name):
        put(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": model_name}})

    def loot(name):
        put(f"{DATA}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"trading_cells:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"trading_cells:blocks/{name}",
        })

    def recipe(name, pattern, key, count=1):
        put(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped",
            "category": "misc", "show_notification": False, "pattern": pattern, "key": key,
            "result": {"id": f"trading_cells:{name}", "count": count}})

    textures = {"item": "iron_block", "fluid": "copper_block", "gas": "quartz_block_side",
                "energy": "gold_block", "universal": "amethyst_block"}
    rotations = {"north": {}, "south": {"y": 180}, "west": {"y": 270},
                 "east": {"y": 90}, "up": {"x": 270}, "down": {"x": 90}}
    core = element([5, 5, 5], [11, 11, 11])
    ring = [element([4, 4, 0], [12, 5, 1], "#frame"), element([4, 11, 0], [12, 12, 1], "#frame"),
            element([4, 5, 0], [5, 11, 1], "#frame"), element([11, 5, 0], [12, 11, 1], "#frame")]
    for cube, hidden in zip(ring, (("up",), ("down",), ("up", "down", "east"), ("up", "down", "west"))):
        for side in hidden:
            del cube["faces"][side]
        cube["faces"]["north"]["cullface"] = "north"
    for kind, texture in textures.items():
        name = kind + "_pipe"
        texture = f"trading_cells:block/logistics/{name}/{name}"
        model_prefix = f"trading_cells:block/logistics/{name}/{name}"
        for suffix, elements in (("", [core]), ("_extract", ring)):
            model(name + suffix, texture, elements)
        cap = element([5, 5, 0], [11, 11, 1])
        cap["faces"] = {"north": {"texture": "#pipe", "cullface": "north"}}
        model(name + "_cap", "trading_cells:block/logistics/pipe_cap", [cap])
        multipart = []
        # A core surface exists only where no arm hides it. Its four edge joins select
        # a shared baked sprite; this creates no per-block renderer or per-tick work.
        for mask in range(16):
            for side, (u, v, u_sign, v_sign) in FACE_AXES.items():
                face = element([5, 5, 5], [11, 11, 11])
                u0, u1 = uv_interval(face, u, u_sign, True)
                v0, v1 = uv_interval(face, v, v_sign, True)
                face["faces"] = {side: {"texture": "#pipe", "rotation": 0, "uv": [u0, v0, u1, v1]}}
                model(f"{name}_core_{side}_{mask}", f"{texture}_core_{mask}", [face])
                when = {side: "none"}
                for bit, edge in enumerate(core_edges(side)):
                    when[edge] = "pipe|insert|extract" if mask & (1 << bit) else "none"
                multipart.append({"when": when, "apply": {
                    "model": f"{model_prefix}_core_{side}_{mask}"}})
        for side, rotation in rotations.items():
            axis = next(i for i, value in enumerate(DIRECTIONS[side]) if value)
            for extract in (False, True):
                start, end = [5, 5, 5], [11, 11, 11]
                if DIRECTIONS[side][axis] < 0:
                    start[axis], end[axis] = 1 if extract else 0, 5
                else:
                    start[axis], end[axis] = 11, 15 if extract else 16
                arm = element(start, end)
                for normal in list(arm["faces"]):
                    if DIRECTIONS[normal][axis]:
                        del arm["faces"][normal]
                        continue
                    u, v, u_sign, v_sign = FACE_AXES[normal]
                    if axis == u:
                        u0, u1 = uv_interval(arm, u, u_sign)
                        v0, v1 = uv_interval(arm, v, v_sign, True)
                    else:
                        u0, u1 = uv_interval(arm, v, v_sign)
                        v1, v0 = uv_interval(arm, u, u_sign, True)
                    arm["faces"][normal].update(uv=[u0, v0, u1, v1], rotation=0 if axis == u else 90)
                suffix = f"_arm_{side}" + ("_extract" if extract else "")
                model(name + suffix, texture, [arm])
                multipart.append({"when": {side: "extract" if extract else "pipe|insert"}, "apply": {
                    "model": model_prefix + suffix}})
            multipart.append({"when": {side: "extract"}, "apply": {
                "model": f"{model_prefix}_extract", **rotation}})
            # Machine faces can be transparent or partial; only pipe-to-pipe ends stay open.
            multipart.append({"when": {side: "insert|extract"}, "apply": {
                "model": f"{model_prefix}_cap", **rotation}})
        put(f"{ASSETS}/blockstates/{name}.json", {"multipart": multipart})
        model(name + "_inventory", texture, [core], {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [1.25, 1.25, 1.25]},
            "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        })
        item(name, f"{model_prefix}_inventory")
        loot(name)

    for prefix, central, material, code in (
        ("item", "hopper", "iron_ingot", "I"),
        ("fluid", "bucket", "copper_ingot", "C"),
        ("energy", "gold_ingot", "redstone", "R"),
        ("gas", "wind_charge", "quartz", "Q"),
    ):
        recipe(prefix + "_pipe", [code * 3, "GXG", code * 3], {
            code: "minecraft:" + material, "G": "minecraft:glass_pane", "X": "minecraft:" + central}, 16)
    recipe("universal_pipe", [" I ", "FAG", " E "], {
        "I": "trading_cells:item_pipe", "F": "trading_cells:fluid_pipe",
        "G": "trading_cells:gas_pipe", "E": "trading_cells:energy_pipe", "A": "minecraft:amethyst_block"}, 16)
    recipe("pipe_wrench", [" II", " CI", "S  "], {
        "I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "S": "minecraft:stick"})
    wrench_pixels = (
        ".........###....", "........#HH#....", ".......#HLS#..#.", ".......#HLS#.#H#",
        ".......#HLL##LS#", "........#HLLLLS#", ".......##SHHLS#.", "......#HL###S#..",
        ".....#HLS#..#...", "....#CBDS#......", "...#CBDS#.......", "..#CBDS#........",
        ".#CBDS#.........", "#HLSS#..........", "#HOS#...........", ".###............",
    )
    palette = {".": (0, 0, 0, 0), "#": (29, 34, 40, 255), "H": (235, 242, 245, 255),
               "L": (185, 201, 211, 255), "S": (101, 125, 141, 255), "C": (200, 111, 65, 255),
               "B": (237, 159, 87, 255), "D": (76, 62, 57, 255), "O": (39, 45, 51, 255)}
    assert all(len(row) == 16 for row in wrench_pixels)
    wrench = Image.new("RGBA", (16, 16))
    wrench.putdata([palette[pixel] for row in wrench_pixels for pixel in row])
    encoded = BytesIO()
    wrench.save(encoded, format="PNG", optimize=False)
    result[f"{ASSETS}/textures/item/pipe_wrench.png"] = encoded.getvalue()
    put(f"{ASSETS}/models/item/pipe_wrench.json", {
        "parent": "minecraft:item/handheld", "textures": {"layer0": "trading_cells:item/pipe_wrench"}})
    item("pipe_wrench", "trading_cells:item/pipe_wrench")

    selector_pixels = (
        ".....##.........", "....#SS#........", "..###########...", ".#HLLLLLLLLLS#..",
        ".#L########LS#..", ".#L#DDCDD##LS#..", ".#L#DDCDD##LS#..", ".#L#CCCCCC#LS#..",
        ".#L#DDCDD##LS#..", ".#L#DDCDD##LS#..", ".#L########LS#..", ".#LSSSSSSSSLS#..",
        ".#LSCCSSBBSLS#..", ".#LSSSSSSSSLS#..", ".#HLLLLLLLLLS#..", "..###########...",
    )
    selector_palette = {**palette, "C": (62, 218, 225, 255), "D": (32, 68, 77, 255)}
    assert all(len(row) == 16 for row in selector_pixels)
    selector = Image.new("RGBA", (16, 16))
    selector.putdata([selector_palette[pixel] for row in selector_pixels for pixel in row])
    encoded = BytesIO()
    selector.save(encoded, format="PNG", optimize=False)
    result[f"{ASSETS}/textures/item/pipe_target_selector.png"] = encoded.getvalue()
    put(f"{ASSETS}/models/item/pipe_target_selector.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "trading_cells:item/pipe_target_selector"}})
    item("pipe_target_selector", "trading_cells:item/pipe_target_selector")
    recipe("pipe_target_selector", [" I ", "ICI", " R "], {
        "I": "minecraft:iron_ingot", "C": "minecraft:compass", "R": "minecraft:redstone"})

    previous = "minecraft:hopper"
    for tier, material, texture in (("basic", "copper_ingot", "copper"), ("improved", "iron_ingot", "iron"),
                           ("advanced", "gold_ingot", "gold"), ("ultimate", "diamond", "diamond"), ("infinite", "netherite_ingot", "netherite")):
        name = tier + "_pipe_upgrade"
        put(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated",
            "textures": {"layer0": f"trading_cells:item/upgrades/pipe/{texture}_upgrade"}})
        item(name, f"trading_cells:item/{name}")
        corners = "popped_chorus_fruit" if texture == "diamond" else material
        recipe(name, ["CMC", "MUM", "CMC"], {
            "C": "minecraft:" + corners, "M": "minecraft:" + material, "U": previous})
        previous = "trading_cells:" + name

    for crafting in (False, True):
        name = "network_crafting_terminal" if crafting else "network_terminal"
        cube = element([0, 0, 0], [16, 16, 16])
        for side, face in cube["faces"].items():
            face["uv"] = [0, 0, 16, 16]
        # The opaque housing backs the transparent icon; the offset avoids coplanar flicker.
        panel = {"from": [0, 16.01, 0], "to": [16, 16.01, 16],
                 "faces": {"up": {"uv": [0, 0, 16, 16], "rotation": 180,
                                  "texture": "#panel", "cullface": "up"}}}
        put(f"{ASSETS}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"pipe": "trading_cells:block/logistics/network_terminal_body",
                         "panel": f"trading_cells:block/logistics/{name}_front",
                         "particle": "trading_cells:block/logistics/network_terminal_body"}, "elements": [cube, panel]})
        put(f"{ASSETS}/blockstates/{name}.json", {"variants": {
            "facing=" + side: {"model": f"trading_cells:block/{name}", **rotation}
            for side, rotation in rotations.items() if side not in ("up", "down")}})
        # Inventory and hand rendering use the full block, including its top panel.
        put(f"{ASSETS}/models/item/{name}.json", {"parent": f"trading_cells:block/{name}"})
        item(name, f"trading_cells:block/{name}")
        loot(name)
    recipe("network_terminal", ["IGI", "PCP", "IRI"], {
        "I": "minecraft:iron_ingot", "G": "minecraft:glass", "P": "trading_cells:universal_pipe",
        "C": "minecraft:chest", "R": "minecraft:comparator"})
    put(f"{DATA}/recipe/network_crafting_terminal.json", {"type": "minecraft:crafting_shapeless",
        "category": "misc", "show_notification": False, "ingredients": ["trading_cells:network_terminal", "minecraft:crafting_table", "minecraft:book"],
        "result": {"id": "trading_cells:network_crafting_terminal", "count": 1}})
    for tag in ("gases", "not_gases"):
        put(f"{DATA}/tags/fluid/{tag}.json", {"replace": False, "values": []})
    return result


def atlas_uv(cube, side, tile):
    x, y, z = cube["from"]
    a, b, c = cube["to"]
    uv = {"down": [x, 16-c, a, 16-z], "up": [x, z, a, c],
          "north": [16-a, 16-b, 16-x, 16-y], "south": [x, 16-b, a, 16-y],
          "west": [z, 16-b, c, 16-y], "east": [16-c, 16-b, 16-z, 16-y]}[side]
    return [value / 2 + (tile % 2 if index % 2 == 0 else tile // 2) * 8 for index, value in enumerate(uv)]


EDITABLE_TEXTURES = {
    "item_pipe": ("iron_block", "iron_block", "iron_block"),
    "fluid_pipe": ("copper_block", "cyan_concrete", "copper_block"),
    "gas_pipe": ("quartz_block_side", "oxidized_copper", "quartz_block_side"),
    "energy_pipe": ("gold_block", "redstone_block", "gold_block"),
    "universal_pipe": ("amethyst_block", "amethyst_block", "amethyst_block"),
    "network_terminal": ("amethyst_block", "iron_block", "observer_front"),
    "network_crafting_terminal": ("amethyst_block", "crafting_table_top", "observer_front"),
}


def seed_textures(archive):
    # These PNGs are artist-owned. Seeding never overwrites an existing editable texture.
    with ZipFile(archive) as source:
        for name, tiles in EDITABLE_TEXTURES.items():
            if name.endswith("_pipe"):
                continue
            path = ROOT / ASSETS / "textures/block/logistics" / (name + ".png")
            if path.exists():
                continue
            atlas = Image.new("RGBA", (32, 32))
            for tile, texture in enumerate((*tiles, tiles[0])):
                image = Image.open(BytesIO(source.read(f"assets/minecraft/textures/block/{texture}.png"))).convert("RGBA")
                atlas.paste(image.crop((0, 0, 16, 16)), ((tile % 2) * 16, (tile // 2) * 16))
            path.parent.mkdir(parents=True, exist_ok=True)
            atlas.save(path)


def resource_matches(relative: str, actual: bytes, expected: bytes) -> bool:
    if not relative.endswith(".png"):
        return actual == expected
    # PNG compression can vary between platforms without changing any pixels.
    try:
        with Image.open(BytesIO(actual)) as committed, Image.open(BytesIO(expected)) as generated:
            return (committed.format == "PNG"
                    and committed.size == generated.size
                    and committed.n_frames == generated.n_frames
                    and committed.convert("RGBA").tobytes() == generated.convert("RGBA").tobytes())
    except OSError:
        return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--seed-textures", type=Path, metavar="MINECRAFT_JAR")
    options = parser.parse_args()
    if options.seed_textures:
        seed_textures(options.seed_textures)
    validate_pipe_textures()
    for name in ("network_terminal", "network_crafting_terminal"):
        path = ROOT / ASSETS / "textures/block/logistics" / (name + ".png")
        if not path.exists():
            raise SystemExit(f"Missing editable texture: {path}")
        with Image.open(path) as texture:
            if texture.width != texture.height or texture.width % 2:
                raise SystemExit(f"Editable texture must contain a square 2x2 atlas: {path}")
    stale = []
    generated = resources()
    if options.check:
        from verify_pipe_models import verify
        verify(generated)
    for relative, content in generated.items():
        path = ROOT / relative
        if options.check:
            if not path.is_file() or not resource_matches(relative, path.read_bytes(), content):
                stale.append(relative)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
    if stale:
        raise SystemExit("Outdated logistics resources:\n" + "\n".join(stale))


if __name__ == "__main__":
    main()
