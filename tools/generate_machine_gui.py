from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw


ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/trading_cells/textures/gui"
BACKGROUNDS = ROOT / "machines" / "backgrounds"
MACHINE_SLOTS = ROOT / "sprites" / "machines" / "slots"
CAPTURES = ROOT / "sprites" / "captures"
WIDTH = 204
HEIGHT = 215

PALETTES = {
    "villager_breeder": (0x4A2E17, 0x8B5B2C, 0xD8A75D, 0xC18A4B),
    "piglin_breeder": (0x4B101C, 0x8B2337, 0xD15A68, 0xB64858),
    "converter": (0x28433A, 0x4E7463, 0x91B79E, 0x729984),
    "farmer": (0x3A4215, 0x6D7A25, 0xA6B94A, 0x879838),
    "villager_incubator": (0x624214, 0xA87520, 0xF0C553, 0xC79838),
    "piglin_incubator": (0x551526, 0x932A43, 0xDC6679, 0xB94860),
    "iron_farm": (0x34383B, 0x62686C, 0xAEB5B9, 0x858C90),
}


def rgba(rgb: int, alpha: int = 255) -> tuple[int, int, int, int]:
    return ((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha)


def panel(
    draw: ImageDraw.ImageDraw,
    bounds: tuple[int, int, int, int],
    dark: tuple[int, int, int, int],
    mid: tuple[int, int, int, int],
    light: tuple[int, int, int, int],
    inner: tuple[int, int, int, int],
) -> None:
    left, top, right, bottom = bounds
    draw.rectangle(bounds, fill=dark)
    draw.line((left, top, right, top), fill=light)
    draw.line((left, top, left, bottom), fill=light)
    if right - left > 2 and bottom - top > 2:
        draw.rectangle((left + 1, top + 1, right - 1, bottom - 1), fill=mid)
        draw.rectangle((left + 2, top + 2, right - 2, bottom - 2), fill=inner)
    draw.line((left, bottom, right, bottom), fill=dark)
    draw.line((right, top, right, bottom), fill=dark)


def machine_background(colors: tuple[int, int, int, int]) -> Image.Image:
    dark, mid, light, inner = map(rgba, colors)
    image = Image.new("RGBA", (WIDTH, HEIGHT), dark)
    draw = ImageDraw.Draw(image)

    draw.rectangle((1, 1, WIDTH - 2, HEIGHT - 2), fill=light)
    draw.rectangle((2, 2, WIDTH - 3, HEIGHT - 3), fill=mid)
    draw.rectangle((3, 3, WIDTH - 4, HEIGHT - 4), outline=dark)

    panel(draw, (6, 4, WIDTH - 7, 19), dark, mid, light, inner)
    panel(draw, (3, 21, WIDTH - 4, 117), dark, mid, light, mid)
    panel(draw, (28, 119, WIDTH - 5, 132), dark, mid, light, mid)
    panel(draw, (28, 133, WIDTH - 5, HEIGHT - 5), dark, mid, light, mid)
    panel(draw, (3, 118, 25, HEIGHT - 5), dark, mid, light, mid)
    return image


def empty_potion_sprite() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outline = (62, 62, 62, 255)
    highlight = (178, 178, 178, 230)
    draw.rectangle((6, 1, 9, 2), fill=outline)
    draw.rectangle((7, 3, 8, 5), fill=outline)
    draw.line(((6, 5), (4, 8), (4, 12), (6, 14), (9, 14), (11, 12), (11, 8), (9, 5)), fill=outline)
    draw.rectangle((5, 9, 10, 12), fill=highlight)
    draw.rectangle((6, 8, 9, 8), fill=highlight)
    return image


def empty_apple_sprite() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outline = (62, 62, 62, 255)
    highlight = (178, 178, 178, 220)
    draw.line(((8, 1), (8, 3), (10, 2)), fill=outline)
    draw.rectangle((10, 2, 12, 3), fill=highlight)
    draw.line(((5, 4), (3, 6), (3, 10), (5, 13), (7, 14)), fill=outline)
    draw.line(((10, 4), (12, 6), (12, 10), (10, 13), (8, 14)), fill=outline)
    draw.line(((5, 4), (7, 5), (8, 5), (10, 4)), fill=outline)
    draw.rectangle((4, 7, 4, 10), fill=highlight)
    return image


def generated_assets() -> dict[Path, Image.Image]:
    backgrounds = {
        BACKGROUNDS / f"{name}.png": machine_background(colors)
        for name, colors in PALETTES.items()
    }
    return backgrounds


def manually_maintained_sprites() -> dict[Path, tuple[int, int]]:
    return {
        MACHINE_SLOTS / "empty_potion.png": (16, 16),
        MACHINE_SLOTS / "empty_apple.png": (16, 16),
        CAPTURES / "empty_villager_head.png": (16, 16),
        CAPTURES / "empty_piglin_head.png": (16, 16),
        CAPTURES / "empty_capturer.png": (32, 32),
        MACHINE_SLOTS / "empty_hoe.png": (16, 16),
        MACHINE_SLOTS / "empty_wheat_seeds.png": (16, 16),
        MACHINE_SLOTS / "empty_bread.png": (16, 16),
        MACHINE_SLOTS / "empty_porkchop.png": (16, 16),
    }


def validate(committed: bool) -> None:
    failures: list[str] = []
    for path, expected in generated_assets().items():
        if not path.exists():
            failures.append(f"missing {path.relative_to(ROOT)}")
            continue
        with Image.open(path) as source:
            actual = source.convert("RGBA")
        if actual.size != expected.size:
            failures.append(f"{path.name}: {actual.size}, expected {expected.size}")
        elif committed and ImageChops.difference(actual, expected).getbbox() is not None:
            failures.append(f"{path.name}: does not match generator")
    for path, expected_size in manually_maintained_sprites().items():
        if not path.exists():
            failures.append(f"missing {path.relative_to(ROOT)}")
            continue
        with Image.open(path) as source:
            if source.size != expected_size:
                failures.append(f"{path.name}: {source.size}, expected {expected_size}")
    if failures:
        raise SystemExit("\n".join(failures))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        validate(True)
        print("Machine GUI textures valid.")
        return
    BACKGROUNDS.mkdir(parents=True, exist_ok=True)
    expected_backgrounds = {f"{name}.png" for name in PALETTES}
    for stale in BACKGROUNDS.glob("*.png"):
        if stale.name not in expected_backgrounds:
            stale.unlink()
    for path, image in generated_assets().items():
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)
    validate(True)
    print("Generated machine GUI backgrounds and validated empty-slot sprites.")


if __name__ == "__main__":
    main()
