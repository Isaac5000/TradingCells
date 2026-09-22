"""Derive each upgrade family from one fixed drawing and the original material palettes."""

import argparse
from collections import defaultdict, deque
import colorsys
from functools import lru_cache
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/trading_cells/textures/item"
ORIGINALS = TEXTURES / "upgrades"
BASES = Path(__file__).resolve().parent / "assets/upgrade_bases"
MATERIALS = ("copper", "iron", "gold", "diamond", "netherite")
FAMILIES = ("quarry", "piglin_barter", "pipe", "mob_farm_speed", "mob_farm_capacity")
TERMINALS = ("network_terminal", "network_crafting_terminal")
TERMINAL_BODY = "network_terminal_body"
TERMINAL_ORIGINALS = BASES / "originals/terminals"
# Neutral steel matching the pipe casings; the existing cyan displays stay untouched.
TERMINAL_STEEL = ((5, 7, 9), (17, 19, 22), (32, 35, 39), (51, 55, 59),
                  (73, 77, 82), (97, 101, 107), (116, 118, 121), (137, 139, 142),
                  (158, 160, 163), (184, 189, 194), (211, 218, 222), (240, 244, 246))
SPECIAL_RIVETS = {
    "diamond": ((24, 8, 39), (59, 20, 81), (99, 40, 133), (153, 75, 188),
                (203, 120, 229), (240, 179, 251), (255, 232, 255)),
    "netherite": ((25, 4, 4), (65, 8, 5), (125, 17, 9), (185, 30, 14),
                  (230, 60, 25), (255, 115, 57), (255, 212, 140)),
}
MOB_FARM_SWORD_REGIONS = ((27, 17, 37, 34), (23, 34, 41, 41), (28, 41, 35, 48))
# Opaque colors from Minecraft 26.2's stick.png, dark to light.
WOOD_PALETTE = ((40, 30, 11), (73, 54, 21), (104, 78, 30), (137, 103, 39))
QUARRY_HANDLE_ROWS = {
    22: (41, 44), 23: (41, 44), 24: (41, 44), 26: (35, 37),
    27: (34, 38), 28: (33, 38), 29: (32, 39), 30: (31, 37), 31: (30, 37),
    32: (29, 36), 33: (29, 36), 34: (27, 33), 35: (27, 33),
    36: (25, 32), 37: (25, 32), 38: (23, 30), 39: (23, 30),
    40: (21, 28), 41: (21, 28), 42: (20, 26), 43: (19, 26),
    44: (19, 25), 45: (19, 24), 46: (19, 24),
}
# Half-open row spans traced on the 64px gold artwork, excluding copper panel texels.
# Keep full runs: color thresholds alone punch holes in white highlights and shadows.
PIGLIN_GOLD_ROWS = {
    16: ((28, 35),), 17: ((26, 37),), 18: ((22, 23), (25, 39)),
    19: ((22, 23), (24, 29), (34, 40)), 20: ((22, 27), (37, 41)),
    21: ((21, 26), (38, 42)), 22: ((21, 26), (40, 42)),
    23: ((21, 27), (41, 42)), 24: ((21, 27), (41, 42)),
    25: ((21, 24),), 26: ((21, 22), (33, 38)), 27: ((32, 39),),
    28: ((28, 40),), 29: ((26, 41),), 30: ((23, 42),),
    31: ((22, 41),), 32: ((22, 41),), 33: ((22, 41),),
    34: ((22, 41),), 35: ((22, 39),), 36: ((22, 36),),
    37: ((23, 33),), 38: ((24, 31), (42, 43)),
    39: ((25, 28), (40, 43)), 40: ((21, 22), (37, 43)),
    41: ((21, 23), (36, 43)), 42: ((21, 24), (38, 43)),
    43: ((22, 26), (37, 41), (42, 43)), 44: ((22, 27), (35, 40)),
    45: ((24, 39),), 46: ((25, 38),), 47: ((27, 36),),
}


def wooden_emblem_pixels(family):
    if family == "quarry":
        return {(x, y) for y, (left, right) in QUARRY_HANDLE_ROWS.items() for x in range(left, right)}
    if family in ("mob_farm_speed", "mob_farm_capacity"):
        return {(x, y) for y in range(40, 43) for x in range(29, 34)}
    return set()


def fixed_emblem_pixels(family, base):
    if family == "piglin_barter":
        # The ingot and circulating arrows are gold, separate from the copper housing.
        pixels = set()
        for y in range(16, 48):
            for x in range(20, 44):
                red, green, blue, alpha = base.getpixel((x, y))
                if alpha and green > red * 0.52 and green > blue * 1.45:
                    pixels.add((x, y))
        return pixels
    return wooden_emblem_pixels(family)


def bake_wooden_emblems():
    """Save fixed wood colors into the source PNGs before generating material tiers."""
    for family in ("quarry", "mob_farm_speed", "mob_farm_capacity"):
        base = family_base(family)
        pixels = wooden_emblem_pixels(family)
        lights = [luminance(base.getpixel(point)[:3]) for point in pixels
                  if luminance(base.getpixel(point)[:3]) >= 18]
        low, high = min(lights), max(lights)
        for point in pixels:
            *rgb, alpha = base.getpixel(point)
            light = luminance(rgb)
            if light < 18 or tuple(rgb) in WOOD_PALETTE:
                continue
            target = luminance(WOOD_PALETTE[0]) + (light - low) / max(1, high - low) * (
                luminance(WOOD_PALETTE[-1]) - luminance(WOOD_PALETTE[0]))
            color = min(WOOD_PALETTE, key=lambda rgb: abs(luminance(rgb) - target))
            base.putpixel(point, (*color, alpha))
        base.save(BASES / f"{family}.png")


def family_base(family):
    with Image.open(BASES / f"{family}.png") as source:
        base = source.convert("RGBA")
    if base.size != (64, 64):
        raise ValueError(f"Unexpected base size: {family}")
    return base


def bake_mob_farm_sword():
    """Persist the shared sword into the canonical PNG, not a runtime composition."""
    speed = family_base("mob_farm_speed")
    capacity = family_base("mob_farm_capacity")
    for bounds in MOB_FARM_SWORD_REGIONS:
        capacity.paste(speed.crop(bounds), bounds[:2])
    capacity.save(BASES / "mob_farm_capacity.png")


def rivet_pixels(base):
    """Keep the metal surround outside each inset rivet's dark outline unchanged."""
    result = set()
    for left, top, right, bottom in ((5, 5, 14, 14), (50, 5, 59, 14),
                                      (5, 50, 14, 59), (50, 50, 59, 59)):
        center = ((left + right) // 2, (top + bottom) // 2)
        pending = deque([center])
        visited = set()
        while pending:
            x, y = pending.popleft()
            if (x, y) in visited or not (left <= x < right and top <= y < bottom):
                continue
            visited.add((x, y))
            red, green, blue, alpha = base.getpixel((x, y))
            if not alpha or luminance((red, green, blue)) < 18:
                continue
            result.add((x, y))
            pending.extend(((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))
    return result


def family_texture(base, material, family=None):
    result = recolor(base, material, fixed_emblem_pixels(family, base))
    palette = SPECIAL_RIVETS.get(material)
    if palette is not None:
        for x, y in rivet_pixels(base):
            red, green, blue, alpha = base.getpixel((x, y))
            light = luminance((red, green, blue))
            color = min(palette, key=lambda rgb: abs(luminance(rgb) - light))
            result.putpixel((x, y), (*color, alpha))
    return result


def emblem_pixels(family, base):
    """Extract the existing artwork, including its dark outline, but not its panel."""
    if family == "piglin_barter":
        result = {(x, y) for y, runs in PIGLIN_GOLD_ROWS.items()
                  for left, right in runs for x in range(left, right)}
        outline = set()
        for x, y in result:
            for point in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                red, green, blue, alpha = base.getpixel(point)
                if alpha and red < 110 and luminance((red, green, blue)) < 50:
                    outline.add(point)
        return result | outline
    result = set(wooden_emblem_pixels(family))
    for y in range(16, 49):
        for x in range(14, 51):
            red, green, blue, alpha = base.getpixel((x, y))
            if not alpha:
                continue
            selected = max(red, green, blue) > 45 and blue >= red * 0.85
            if selected:
                result.add((x, y))
    # The dark outline is at most two pixels thick; do not flood into the frame.
    for _ in range(2):
        outline = set()
        for x, y in result:
            for point in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                px, py = point
                if 14 <= px < 51 and 16 <= py < 49:
                    red, green, blue, alpha = base.getpixel(point)
                    if alpha and max(red, green, blue) < 55:
                        outline.add(point)
        result.update(outline)
    return result


def quarry_frame(base):
    """Clone unobstructed quarry panel texels into the removed pickaxe silhouette."""
    mask = emblem_pixels("quarry", base)
    result = base.copy()
    clean = [(x, y) for y in range(15, 50) for x in range(16, 48)
             if (x, y) not in mask and base.getpixel((x, y))[0] > 85
             and base.getpixel((x, y))[0] > base.getpixel((x, y))[1] * 1.4]
    for x, y in sorted(mask):
        # Reflected samples retain the source drawing's vertical light/shadow bands.
        point = min(clean, key=lambda p: (abs(p[1] - y), abs(p[0] - (63 - x))))
        result.putpixel((x, y), base.getpixel(point))
    return result


def common_frame_textures():
    quarry = family_base("quarry")
    frame = quarry_frame(quarry)
    frames = {material: family_texture(frame, material) for material in MATERIALS}
    result = {}
    for family in FAMILIES:
        base = family_base(family)
        mask = emblem_pixels(family, base)
        for material, shared in frames.items():
            icon = shared.copy()
            for point in mask:
                icon.putpixel(point, base.getpixel(point))
            result[ORIGINALS / family / f"{material}_upgrade.png"] = icon
    return result


def luminance(rgb):
    return sum(channel * weight for channel, weight in zip(rgb, (0.2126, 0.7152, 0.0722)))


@lru_cache(maxsize=None)
def material_ramp(material):
    with Image.open(ORIGINALS / f"{material}_upgrade.png") as source:
        source = source.convert("RGBA")
        pixels = [source.getpixel((x, y)) for y in range(source.height) for x in range(source.width)]
    bands = defaultdict(list)
    for red, green, blue, alpha in pixels:
        if not alpha:
            continue
        hue, saturation, value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
        neutral = saturation < 0.16
        accepted = {
            "iron": neutral,
            "gold": 0.09 <= hue <= 0.18 or neutral and value > 0.85,
            "diamond": 0.45 <= hue <= 0.57 or neutral and value > 0.85,
            "netherite": saturation < 0.25 and value < 0.7,
        }[material]
        if accepted:
            bands[int(luminance((red, green, blue)) // 12)].append((red, green, blue))
    # A monotonic ramp preserves light/shadow geometry instead of pairing unrelated pixels.
    return ((0, 0, 0),) + tuple(tuple(round(sum(rgb[channel] for rgb in colors) / len(colors)) for channel in range(3))
                              for _, colors in sorted(bands.items()))


def recolor(base: Image.Image, material: str, preserved_pixels=()) -> Image.Image:
    if material == "copper":
        return base.copy()
    palette = material_ramp(material)

    @lru_cache(maxsize=None)
    def mapped(rgb):
        red, green, blue = rgb
        # Stone, steel and cyan resource emblems retain their own colors.
        if not (red > blue * 1.2 and red > green * 1.03):
            return rgb
        light = luminance(rgb) * (0.58 if material == "netherite" else 1)
        return min(palette, key=lambda color: abs(luminance(color) - light))

    result = base.copy()
    for y in range(64):
        for x in range(64):
            red, green, blue, alpha = base.getpixel((x, y))
            if alpha and (x, y) not in preserved_pixels:
                result.putpixel((x, y), (*mapped((red, green, blue)), alpha))
    assert result.getchannel("A").tobytes() == base.getchannel("A").tobytes()
    return result


def generated():
    result = common_frame_textures()
    for family in TERMINALS:
        with Image.open(BASES / f"{family}.png") as source:
            result[TEXTURES.parent / "block/logistics" / f"{family}_front.png"] = source.convert("RGBA")
    with Image.open(BASES / f"{TERMINAL_BODY}.png") as source:
        result[TEXTURES.parent / "block/logistics" / f"{TERMINAL_BODY}.png"] = source.convert("RGBA")
    return result


def logistics_terminal_texture(base):
    result = base.copy()
    for y in range(base.height):
        for x in range(base.width):
            red, green, blue, alpha = base.getpixel((x, y))
            if alpha and red > blue * 1.2 and red > green * 1.03:
                light = luminance((red, green, blue))
                color = min(TERMINAL_STEEL, key=lambda rgb: abs(luminance(rgb) - light))
                result.putpixel((x, y), (*color, alpha))
    return result


def bake_terminal_steel():
    """Bake the recolor into the canonical PNGs while retaining the original drawings."""
    TERMINAL_ORIGINALS.mkdir(parents=True, exist_ok=True)
    for name in (*TERMINALS, TERMINAL_BODY):
        original = TERMINAL_ORIGINALS / f"{name}.png"
        if not original.exists():
            with Image.open(BASES / f"{name}.png") as source:
                source.save(original)
        with Image.open(original) as source:
            logistics_terminal_texture(source.convert("RGBA")).save(BASES / f"{name}.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--import-base", nargs=2, metavar=("FAMILY", "IMAGE"))
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--preview-family-prefix", default="")
    parser.add_argument("--inspect-bases", type=Path)
    parser.add_argument("--inspect-name")
    parser.add_argument("--extract-background", action="store_true")
    parser.add_argument("--bake-mob-farm-sword", action="store_true")
    parser.add_argument("--bake-wooden-emblems", action="store_true")
    parser.add_argument("--bake-terminal-steel", action="store_true")
    args = parser.parse_args()
    if args.bake_mob_farm_sword:
        bake_mob_farm_sword()
    if args.bake_wooden_emblems:
        bake_wooden_emblems()
    if args.bake_terminal_steel:
        bake_terminal_steel()
    if args.import_base:
        family, path = args.import_base
        if family not in (*FAMILIES, *TERMINALS, TERMINAL_BODY):
            parser.error("Unknown family")
        with Image.open(path) as source:
            source = source.convert("RGBA")
            if family != TERMINAL_BODY and source.getchannel("A").getextrema()[0] != 0 and not args.extract_background:
                raise ValueError("Base must have real transparency")
            if family == TERMINAL_BODY and source.getchannel("A").getextrema() != (255, 255):
                raise ValueError("Block housing must be fully opaque")
            image = source.resize((64, 64), Image.Resampling.NEAREST)
        if args.extract_background:
            # Only the edge-connected neutral generated backdrop is removed.
            pending = deque((x, y) for y in range(64) for x in range(64) if x in (0, 63) or y in (0, 63))
            seen = set()
            while pending:
                x, y = pending.popleft()
                if (x, y) in seen or not (0 <= x < 64 and 0 <= y < 64):
                    continue
                seen.add((x, y))
                red, green, blue, alpha = image.getpixel((x, y))
                if alpha and (max(red, green, blue) - min(red, green, blue) > 24 or min(red, green, blue) < 96):
                    continue
                image.putpixel((x, y), (0, 0, 0, 0))
                pending.extend(((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))
        # Inventory pixel art needs opaque texels, not near-opaque generated alpha.
        image.putalpha(image.getchannel("A").point(lambda alpha: 255 if alpha >= 128 else 0))
        if family in ("mob_farm_speed", "mob_farm_capacity"):
            with Image.open(BASES / "quarry.png") as reference:
                left, top, right, bottom = reference.convert("RGBA").getchannel("A").getbbox()
            visible = image.getchannel("A").getbbox()
            fitted = image.crop(visible).resize((right - left, bottom - top), Image.Resampling.NEAREST)
            image = Image.new("RGBA", (64, 64))
            image.paste(fitted, (left, top))
        BASES.mkdir(parents=True, exist_ok=True)
        image.save(BASES / f"{family}.png")
        return
    if args.inspect_bases:
        names = (*FAMILIES, *TERMINALS, TERMINAL_BODY, *(f"originals/{m}" for m in MATERIALS))
        if args.inspect_name:
            names = (args.inspect_name,)
        columns = min(4, len(names))
        sheet = Image.new("RGB", (columns * 540, ((len(names) + columns - 1) // columns) * 560), (38, 44, 47))
        draw = ImageDraw.Draw(sheet)
        for index, name in enumerate(names):
            path = BASES / f"{name}.png"
            if not path.exists():
                continue
            with Image.open(path) as source:
                icon = source.convert("RGBA")
            x, y = index % columns * 540 + 24, index // columns * 560 + 24
            icon = icon.resize((512, 512), Image.Resampling.NEAREST)
            sheet.paste(icon, (x, y), icon)
            for i in range(0, 64, 4):
                draw.text((x + i * 8, y - 14), str(i), fill="white")
                draw.text((x - 22, y + i * 8), str(i), fill="white")
            draw.text((x, y + 518), name, fill="white")
        sheet.save(args.inspect_bases)
        return
    images = generated()
    stale = []
    for path, image in images.items():
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            image.save(path)
        if args.check:
            if not path.is_file():
                stale.append(str(path.relative_to(ROOT)))
                continue
            with Image.open(path) as existing:
                if existing.size != image.size or existing.convert("RGBA").tobytes() != image.tobytes():
                    stale.append(str(path.relative_to(ROOT)))
    if stale:
        raise SystemExit("Outdated upgrade textures:\n" + "\n".join(stale))
    if args.preview:
        preview_images = {path: image for path, image in images.items()
                          if path.parent.name.startswith(args.preview_family_prefix)}
        sheet = Image.new("RGB", (5 * 160, ((len(preview_images) + 4) // 5) * 180), (38, 44, 47))
        draw = ImageDraw.Draw(sheet)
        for index, (path, icon) in enumerate(preview_images.items()):
            x, y = index % 5 * 160 + 16, index // 5 * 180 + 12
            enlarged = icon.resize((128, 128), Image.Resampling.NEAREST)
            sheet.paste(enlarged, (x, y), enlarged)
            draw.text((x, y + 132), path.parent.name if index < len(FAMILIES) * 5 else "terminal", fill="white")
            draw.text((x, y + 146), path.stem, fill=(154, 223, 225))
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(args.preview)
    print(f"Upgrade families: {len(FAMILIES) * 5} palette-only variants, 2 terminal panels and opaque terminal housing verified")


if __name__ == "__main__":
    main()
