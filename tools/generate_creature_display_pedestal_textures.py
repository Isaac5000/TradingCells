#!/usr/bin/env python3
"""Deterministic, deliberately limited 16x16 palette for the display pedestal.

Each material uses flat colors and clean, symmetric accents. No random noise
or dithering: the same material looks consistent from every side.
Requires only the Python standard library. --check verifies generated outputs.
"""
from __future__ import annotations

import argparse
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "src/main/resources/assets/trading_cells/textures/block/display_pedestal"
SIZE = 16

# A restrained stone/metal/amethyst palette with no stochastic per-pixel tint.
STONE = (36, 33, 47)
STONE_SHADOW = (25, 23, 34)
STONE_EDGE = (55, 50, 66)
METAL = (127, 120, 137)
METAL_HIGHLIGHT = (151, 142, 157)
METAL_SHADOW = (72, 67, 82)
BRONZE = (159, 119, 94)
PURPLE_DARK = (43, 23, 78)
PURPLE = (78, 39, 131)
PURPLE_MID = (115, 64, 177)
PURPLE_LIGHT = (163, 105, 222)
PURPLE_CENTER = (202, 147, 240)


def pixels(painter):
    return [[painter(x, y) for x in range(SIZE)] for y in range(SIZE)]


def darkstone(x: int, y: int):
    """Mostly even dark stone, with subtle straight borders instead of noise."""
    if x in (0, 15) or y in (0, 15):
        return STONE_SHADOW
    if x in (1, 14) or y in (1, 14):
        return STONE_EDGE
    return STONE


def metal(x: int, y: int):
    """One identical, neutral grey on every metal face of the pedestal.

    In a cuboid model the UV mapping changes by face. Shaded/highlighted edges
    made identical trim pieces appear to have different colors from above.
    """
    return METAL


def crystal(x: int, y: int):
    """Symmetric facets limited to four violet shades."""
    distance = abs(x - 7.5) + abs(y - 7.5)
    if distance <= 3:
        return PURPLE_CENTER
    if distance <= 5:
        return PURPLE_LIGHT
    if distance <= 8:
        return PURPLE
    return PURPLE_DARK


def top(x: int, y: int):
    """Uniform dark background, crisp square ring, small glowing diamond."""
    edge = min(x, y, 15 - x, 15 - y)
    if edge == 0:
        return STONE_SHADOW
    if edge == 1:
        return PURPLE_DARK
    if edge == 2:
        return PURPLE
    distance = abs(x - 7.5) + abs(y - 7.5)
    if distance <= 2:
        return PURPLE_CENTER
    if distance <= 4:
        return PURPLE_LIGHT
    if distance <= 6:
        return PURPLE_MID
    return PURPLE_DARK


TEXTURES = {
    "polished_darkstone.png": darkstone,
    "aged_metal.png": metal,
    "violet_core.png": crystal,
    "rune_top.png": top,
}


def png(data):
    """Minimal unfiltered 8-bit RGBA PNG writer (16x16)."""
    scanlines = b"".join(
        b"\0" + b"".join(bytes((*rgb, 255)) for rgb in row)
        for row in data
    )

    def chunk(tag, raw):
        return (
            struct.pack(">I", len(raw)) + tag + raw
            + struct.pack(">I", zlib.crc32(tag + raw) & 0xFFFFFFFF)
        )

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(scanlines, 9))
        + chunk(b"IEND", b"")
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if not args.check:
        TARGET.mkdir(parents=True, exist_ok=True)
    differences = []
    for name, painter in TEXTURES.items():
        path = TARGET / name
        image = png(pixels(painter))
        if args.check:
            if not path.exists() or path.read_bytes() != image:
                differences.append(str(path))
        else:
            path.write_bytes(image)
            print(f"Generated {path.relative_to(ROOT)}")
    if differences:
        print("Pedestal textures differ from the generator:")
        print("\n".join(differences))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())