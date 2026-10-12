"""Export the egg workbench's own static model and generated 16px block art."""
import argparse
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/trading_cells"
SOURCE = ROOT / "tools/assets/creature_generator/atlas.png"


def cube(start, end, texture="casing", **faces):
    result = {}
    for side in ("north", "south", "east", "west", "up", "down"):
        face = {"texture": "#" + faces.get(side, texture)}
        if side in faces:
            axes = (0, 2) if side in ("up", "down") else (0, 1) if side in ("north", "south") else (2, 1)
            width, height = (end[axis] - start[axis] for axis in axes)
            scale = 16 / max(width, height)
            u, v = (16 - width * scale) / 2, (16 - height * scale) / 2
            face["uv"] = [u, v, 16 - u, 16 - v]
        result[side] = face
    return {"from": start, "to": end, "faces": result}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=SOURCE)
    args = parser.parse_args()
    with Image.open(args.source) as source:
        atlas = source.convert("RGB")
    width, height = atlas.size
    target = ASSETS / "textures/block/creature_generator"
    target.mkdir(parents=True, exist_ok=True)
    for index, name in enumerate(("top", "front", "casing", "trim")):
        x, y = index % 2, index // 2
        tile = atlas.crop((x * width // 2, y * height // 2,
                           (x + 1) * width // 2, (y + 1) * height // 2))
        tile.resize((16, 16), Image.Resampling.NEAREST).save(target / f"{name}.png")
    model = {
        "parent": "minecraft:block/block",
        "textures": {name: f"trading_cells:block/creature_generator/{name}"
                     for name in ("top", "front", "casing", "trim")},
        "elements": [
            cube([2, 0, 2], [14, 2, 14], up="trim"),
            cube([4, 2, 4], [12, 9, 12], north="front"),
            cube([1, 9, 1], [15, 11, 15], "trim"),
            cube([1, 11, 1], [15, 13, 15], up="casing"),
            cube([3, 13, 3], [13, 14, 13], up="top"),
        ],
    }
    model["textures"]["particle"] = model["textures"]["casing"]
    (ASSETS / "models/block/creature_generator.json").write_text(
        json.dumps(model, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
