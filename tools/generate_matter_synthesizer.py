"""Bake the synthesizer artwork and static chamber, preserving existing IDs."""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/trading_cells"
SOURCE = ROOT / "tools/assets/matter_synthesizer/atlas.png"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def box(start, end, texture="case", **faces):
    # Vanilla computes world-unit UVs for casing; full decals only cover square faces.
    result = {side: {"texture": "#" + faces.get(side, texture),
                    **({"uv": [0, 0, 16, 16]} if side in faces else {})}
              for side in ("north", "south", "east", "west", "up", "down")}
    return {"from": start, "to": end, "faces": result}


def generate():
    with Image.open(SOURCE) as source:
        for name, bounds in {"screen": (0, 0, 627, 599), "case": (627, 0, 1254, 599),
                             "emitter": (0, 600, 627, 1208)}.items():
            path = ASSETS / f"textures/block/arcane_infuser/{name}.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            source.crop(bounds).convert("RGB").resize((16, 16), Image.Resampling.LANCZOS).save(path)
        source.crop((632, 603, 1251, 1207)).convert("RGBA").resize((256, 256), Image.Resampling.NEAREST).save(SOURCE.with_name("panel.png"))
    with Image.open(SOURCE.with_name("grid.png")) as grid:
        grid.convert("RGB").resize((16, 16), Image.Resampling.NEAREST).save(
            ASSETS / "textures/block/arcane_infuser/grid.png")
    elements = [box([1, 0, 1], [15, 3, 15]), box([3, 3, 3], [13, 4, 13], up="grid"),
                box([1, 3, 2], [3, 13, 14]), box([13, 3, 2], [15, 13, 14]),
                box([3, 3, 13], [13, 13, 15]),
                box([3, 6, 6], [4, 10, 10], east="emitter"),
                box([12, 6, 6], [13, 10, 10], west="emitter"),
                box([6, 6, 12], [10, 10, 13], north="emitter"),
                box([4, 13, 8], [12, 16, 16], up="screen")]
    write(ASSETS / "models/block/arcane_infuser.json", {
        "parent": "minecraft:block/block", "ambientocclusion": True,
        "textures": {**{name: "trading_cells:block/arcane_infuser/" + name for name in ("case", "screen", "emitter", "grid")},
                     "particle": "trading_cells:block/arcane_infuser/case"}, "elements": elements})
    write(ASSETS / "blockstates/arcane_infuser.json", {"variants": {
        "facing=" + facing: {"model": "trading_cells:block/arcane_infuser", "y": rotation}
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    for locale, title, category in (("es_es", "Sintetizador de materia", "Síntesis de materia"),
                                    ("en_us", "Matter Synthesizer", "Matter Synthesis")):
        path = ASSETS / f"lang/{locale}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for prefix in ("block", "item", "container"):
            data[f"{prefix}.trading_cells.arcane_infuser"] = title
        data["category.trading_cells.arcane_infusion"] = category
        write(path, data)


if __name__ == "__main__":
    generate()
