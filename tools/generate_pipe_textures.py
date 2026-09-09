"""Explicitly rebuild editable 32px pipe sprites and their shared vanilla animation metadata."""
import argparse
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/trading_cells/textures/block/logistics"
KINDS = ("item", "fluid", "gas", "energy", "universal")
SIZE = 32
FRAMES = 8
FRAME_TICKS = 4
JUNCTIONS = {f"core_{mask}": mask for mask in range(16)}


def base_texture(connections=0):
    image = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # N/E/S/W edges disappear at a join, including their corner highlights.
            distances = (y, SIZE - 1 - x, SIZE - 1 - y, x)
            bevels = [max(0, 5 - distance) if not connections & (1 << side) else 0
                      for side, distance in enumerate(distances)]
            corner = max(bevels[0], bevels[2]) * max(bevels[1], bevels[3]) // 3
            value = 116 + max(bevels) * 7 + corner
            image.putpixel((x, y), (value, value + 2, value + 5, 255))
    return image


def cap_texture():
    image = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            distance = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            value = 2 + round(38 * (1 - distance / 15) ** 1.5)
            image.putpixel((x, y), (value, value, value, 255))
    return image


def blend(dark, light, factor):
    return tuple(round(a + (b - a) * factor) for a, b in zip(dark, light)) + (255,)


def frame(kind, index, base):
    image = base.copy()
    phase = index * 4
    for x in range(SIZE):
        # One recessed conduit; colors are pixels, never emissive model layers or block light.
        image.putpixel((x, 11), (86, 89, 94, 255))
        image.putpixel((x, 20), (86, 89, 94, 255))
        for y in range(12, 20):
            image.putpixel((x, y), (97, 101, 107, 255))
        # All surfaces use the same positive world-axis phase, including adjacent blocks.
        along = x
        motion = (along - phase) % SIZE
        if kind == "item":
            for y in (15, 16):
                image.putpixel((x, y), (191, 195, 199, 255))
            for y in (13, 14, 17, 18):
                brightness = 0.9 if (motion + (min(y, 31 - y) - 15) * 2) % 16 < 5 else 0.25
                image.putpixel((x, y), blend((132, 140, 149), (224, 229, 232), brightness))
        elif kind == "fluid":
            for y in range(13, 19):
                crest = (math.sin((motion + (min(y, 31 - y) - 15) * 3) * math.tau / 16) + 1) / 2
                image.putpixel((x, y), blend((31, 98, 153), (115, 207, 227), crest * 0.8))
            if motion % 16 < 4:
                for y in (13, 18):
                    image.putpixel((x, y), (150, 224, 237, 255))
        elif kind == "gas":
            wave = round((math.sin(motion * math.tau / SIZE) + 1))
            for y in (15 - wave, 16 + wave):
                image.putpixel((x, y), (233, 242, 244, 255))
            for y in (14, 17):
                if motion % 16 < 6:
                    image.putpixel((x, y), (167, 184, 194, 255))
        elif kind == "energy":
            pulse = (1 - math.cos(index * math.tau / FRAMES)) / 2
            for y in (15, 16):
                image.putpixel((x, y), blend((93, 22, 28), (200, 44, 41), pulse))
            if along % 8 < 4:
                for y in (14, 17):
                    image.putpixel((x, y), blend((103, 20, 26), (237, 65, 58), pulse))
            if along % 8 == 0:
                for node_y in range(13, 19):
                    image.putpixel((x, node_y), blend((129, 27, 32), (250, 104, 75), pulse))
        else:
            for y in range(13, 19):
                spark = (motion + min(y, 31 - y) * 3) % 16
                strength = 1 if spark < 3 else 0.35 if spark < 8 else 0.05
                image.putpixel((x, y), blend((91, 53, 137), (222, 170, 246), strength))
    return image


def junction_frame(kind, index, mask):
    image = base_texture(mask)
    source = frame(kind, index, base_texture(10))
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - 15.5, y - 15.5
            segments = []
            for bit, along, across, coordinate in ((1, -dy, dx, dy), (2, dx, dy, dx),
                                                 (4, dy, dx, dy), (8, -dx, dy, dx)):
                if mask & bit:
                    segments.append((across * across + min(0, along) ** 2, across, coordinate))
            if not segments:
                segments.append((dx * dx + dy * dy, dy, dx))
            distance, across, along = min(segments)
            if distance <= 4.5 ** 2:
                # The core spans six of the sixteen longitudinal block pixels.
                # Arms sample 0..5, this junction 5..11, and the opposite arm 11..16.
                image.putpixel((x, y), source.getpixel((int(16 + along * 6 / 16) % SIZE, round(across + 15.5))))
    return image


def sprite_names(kind):
    return [f"{kind}_pipe", *(f"{kind}_pipe_{name}" for name in JUNCTIONS)]


def sprite_path(name):
    return TEXTURES / (name.split("_pipe", 1)[0] + "_pipe") / f"{name}.png"


def generate(preview=None):
    TEXTURES.mkdir(parents=True, exist_ok=True)
    base_texture().save(TEXTURES / "pipe_base.png")
    cap_texture().save(TEXTURES / "pipe_cap.png", optimize=True)
    base = base_texture(10)
    previews = [Image.new("RGBA", (SIZE * len(KINDS), SIZE)) for _ in range(FRAMES)]
    for column, kind in enumerate(KINDS):
        for name in sprite_names(kind):
            strip = Image.new("RGBA", (SIZE, SIZE * FRAMES))
            for index in range(FRAMES):
                sprite = (frame(kind, index, base) if name == f"{kind}_pipe" else
                          junction_frame(kind, index, int(name.rsplit("_", 1)[1])))
                strip.paste(sprite, (0, index * SIZE))
                if name == f"{kind}_pipe":
                    previews[index].paste(sprite, (column * SIZE, 0))
            target = sprite_path(name)
            target.parent.mkdir(parents=True, exist_ok=True)
            strip.save(target, optimize=True)
            target.with_suffix(".png.mcmeta").write_text(json.dumps({"animation": {
                "width": SIZE, "height": SIZE, "frametime": FRAME_TICKS, "interpolate": False,
            }}, indent=2) + "\n", encoding="utf-8")
    if preview:
        preview.mkdir(parents=True, exist_ok=True)
        enlarged = [image.resize((960, 192), Image.Resampling.NEAREST) for image in previews]
        enlarged[0].save(preview / "pipe-materials.png")
        enlarged[0].save(preview / "pipe-materials.gif", save_all=True, append_images=enlarged[1:],
                         duration=FRAME_TICKS * 50, loop=0, disposal=2)


def validate():
    with Image.open(TEXTURES / "pipe_base.png") as base:
        if base.size != (SIZE, SIZE):
            raise ValueError("Pipe base must be a single 32x32 sprite")
    cap = TEXTURES / "pipe_cap.png"
    with Image.open(cap) as image:
        if image.size != (SIZE, SIZE) or image.convert("RGBA").getextrema()[3] != (255, 255):
            raise ValueError("Pipe cap must be a single opaque 32x32 sprite")
    if cap.with_suffix(".png.mcmeta").exists():
        raise ValueError("Pipe caps must remain static")
    for name in (name for kind in KINDS for name in sprite_names(kind)):
        target = sprite_path(name)
        metadata = json.loads(target.with_suffix(".png.mcmeta").read_text(encoding="utf-8"))["animation"]
        with Image.open(target) as image:
            if image.width != SIZE or image.height % SIZE or not 1 <= image.height // SIZE <= 32:
                raise ValueError(f"{target.name}: expected 1-32 vertically stacked 32x32 frames")
        if metadata.get("width") != SIZE or metadata.get("height") != SIZE:
            raise ValueError(f"{target.name}: frame dimensions must be explicit")
        if metadata.get("interpolate", False) or not 2 <= metadata.get("frametime", 1) <= 40:
            raise ValueError(f"{target.name}: use discrete frames at 0.5-10 updates per second")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true", help="Overwrite all five pipe designs, not terminals")
    parser.add_argument("--write-cap", action="store_true", help="Overwrite only the shared static machine-facing cap")
    parser.add_argument("--preview", type=Path)
    options = parser.parse_args()
    if options.write:
        generate(options.preview)
    elif options.write_cap:
        TEXTURES.mkdir(parents=True, exist_ok=True)
        cap_texture().save(TEXTURES / "pipe_cap.png", optimize=True)
    validate()


if __name__ == "__main__":
    main()
