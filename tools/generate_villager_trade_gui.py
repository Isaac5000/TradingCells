from __future__ import annotations

import argparse
from pathlib import Path
from typing import Final

from PIL import Image, ImageChops, ImageDraw

ROOT: Final = Path(__file__).resolve().parents[1] / "src/main/resources/assets/trading_cells/textures/gui"
TRADER: Final = ROOT / "trader"
BACKGROUNDS: Final = TRADER / "backgrounds"
WIDGETS: Final = TRADER / "widgets"
ROWS: Final = WIDGETS / "rows"
SELECTOR: Final = WIDGETS / "selector"
ARROWS: Final = WIDGETS / "arrows"
SLOTS: Final = WIDGETS / "slots"
SPRITES: Final = ROOT / "sprites"
RESET_SPRITES: Final = SPRITES / "trader" / "reset"
EQUIPMENT_SPRITES: Final = SPRITES / "container" / "slot"
CAPTURE_SPRITES: Final = SPRITES / "captures"
TRADE_DROPDOWN_FILE: Final = "trade_dropdown.png"
DISABLED_SLOT_OVERLAY_FILE: Final = "disabled_slot_overlay.png"
TRADE_ROW_NORMAL: Final = (26, 26, 26, 179)
TRADE_ROW_HOVERED: Final = (42, 42, 42, 179)
TRADE_ROW_SELECTED: Final = (24, 34, 24, 190)
TRADE_ROW_DISABLED: Final = (24, 24, 24, 118)
ATLAS_WIDTH: Final = 512
ATLAS_HEIGHT: Final = 256
MENU_WIDTH: Final = 348
MENU_HEIGHT: Final = 210
MANUAL_SLOT_Y: Final = 52
PLAYER_INVENTORY_X: Final = 150
PLAYER_INVENTORY_Y: Final = 116
PLAYER_HOTBAR_Y: Final = 174
EQUIPMENT_X: Final = 130
EQUIPMENT_Y: Final = (108, 126, 144, 162, 180)
AUTOTRADER_ROW_X: Final = 26
AUTOTRADER_ROW_Y: Final = (78, 125, 172)
DYNAMIC_SLOT_ANCHORS: Final = (
    (165, MANUAL_SLOT_Y),
    (191, MANUAL_SLOT_Y),
    (249, MANUAL_SLOT_Y),
    *((PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18) for row in range(3) for column in range(9)),
    *((PLAYER_INVENTORY_X + column * 18, PLAYER_HOTBAR_Y) for column in range(9)),
    *((EQUIPMENT_X, y) for y in EQUIPMENT_Y),
    *((AUTOTRADER_ROW_X + column * 18, y) for y in AUTOTRADER_ROW_Y for column in range(4)),
)

PALETTES = {
    "default": {
        "bg": (194, 199, 188),
        "panel": (190, 190, 190),
        "surface": (214, 214, 209),
        "inner": (87, 87, 87),
        "light": (235, 235, 229),
        "dark": (63, 68, 58),
        "slot_outer": (47, 47, 47),
        "slot_inner": (190, 190, 190),
        "slot_highlight": (226, 226, 226),
        "accent": (126, 157, 100),
    },
}


def framed_rect(draw: ImageDraw.ImageDraw, x: int, y: int, width: int, height: int, palette: dict, inner: tuple[int, int, int] | None = None) -> None:
    draw.rectangle((x, y, x + width - 1, y + height - 1), fill=palette["panel"])
    draw.line((x, y, x + width - 1, y), fill=palette["light"])
    draw.line((x, y, x, y + height - 1), fill=palette["light"])
    draw.line((x, y + height - 1, x + width - 1, y + height - 1), fill=palette["dark"])
    draw.line((x + width - 1, y, x + width - 1, y + height - 1), fill=palette["dark"])
    if inner is not None and width > 2 and height > 2:
        draw.rectangle((x + 1, y + 1, x + width - 2, y + height - 2), fill=inner)


def generate_theme(theme: str, palette: dict) -> Image.Image:
    image = Image.new("RGBA", (ATLAS_WIDTH, ATLAS_HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, MENU_WIDTH - 1, MENU_HEIGHT - 1), fill=palette["dark"])
    draw.rectangle((2, 2, MENU_WIDTH - 3, MENU_HEIGHT - 3), fill=palette["panel"])
    draw.line((0, 0, MENU_WIDTH - 1, 0), fill=palette["light"])
    draw.line((0, 0, 0, MENU_HEIGHT - 1), fill=palette["light"])
    draw.rectangle((4, 4, MENU_WIDTH - 5, MENU_HEIGHT - 5), fill=palette["bg"])

    framed_rect(draw, 5, 5, 114, 200, palette, palette["bg"])
    framed_rect(draw, 9, 8, 106, 16, palette, palette["surface"])
    framed_rect(draw, 9, 27, 106, 174, palette, palette["bg"])

    framed_rect(draw, 123, 5, 220, 66, palette, palette["bg"])
    framed_rect(draw, 128, 8, 210, 16, palette, palette["surface"])
    framed_rect(draw, 123, 74, 220, 28, palette, palette["bg"])

    framed_rect(draw, 123, 104, 220, 102, palette, palette["bg"])

    validate_theme_image(image, theme, palette)
    return image


def validate_theme_image(image: Image.Image, theme: str, palette: dict) -> None:
    if image.size != (ATLAS_WIDTH, ATLAS_HEIGHT):
        raise ValueError(f"{theme}: atlas size is {image.size}, expected {(ATLAS_WIDTH, ATLAS_HEIGHT)}")
    if image.getbbox() != (0, 0, MENU_WIDTH, MENU_HEIGHT):
        raise ValueError(
            f"{theme}: opaque bounds are {image.getbbox()}, expected {(0, 0, MENU_WIDTH, MENU_HEIGHT)}"
        )
    for x, y in DYNAMIC_SLOT_ANCHORS:
        if x < 0 or y < 0 or x + 18 > MENU_WIDTH or y + 18 > MENU_HEIGHT:
            raise ValueError(f"{theme}: slot anchor {(x, y)} falls outside the visible menu")
        if image.getpixel((x + 1, y + 1))[:3] != palette["bg"]:
            raise ValueError(f"{theme}: dynamic slot {(x, y)} is obstructed in the base texture")


def check_generated_textures() -> None:
    failures: list[str] = []
    for theme, palette in PALETTES.items():
        path = BACKGROUNDS / f"{theme}.png"
        if not path.exists():
            failures.append(f"missing {path.relative_to(ROOT.parent)}")
            continue
        expected = generate_theme(theme, palette)
        with Image.open(path) as source:
            actual = source.convert("RGBA")
        try:
            validate_theme_image(actual, theme, palette)
        except ValueError as error:
            failures.append(str(error))
            continue
        if actual.size != expected.size or ImageChops.difference(actual, expected).getbbox() is not None:
            failures.append(f"{theme}: texture does not match the current generator")
    if failures:
        raise SystemExit("\n".join(failures))
    validate_widgets()
    print(f"Validated neutral villager GUI texture and widgets at {MENU_WIDTH}x{MENU_HEIGHT}.")


def validate_widgets() -> None:
    failures: list[str] = []
    validate_widget_sizes(failures)
    validate_reset_sprites(failures)
    validate_equipment_sprites(failures)
    validate_capture_sprites(failures)
    validate_trade_state_colors(failures)
    validate_removed_legacy_textures(failures)
    if failures:
        raise SystemExit("\n".join(failures))


def validate_widget_sizes(failures: list[str]) -> None:
    expected_sizes = {
        ROWS / "trade_row.png": (100, 24),
        ROWS / "trade_row_hovered.png": (100, 24),
        ROWS / "trade_row_selected.png": (100, 24),
        ROWS / "trade_row_disabled.png": (100, 24),
        SELECTOR / TRADE_DROPDOWN_FILE: (102, 18),
        SELECTOR / "trade_dropdown_row.png": (94, 18),
        SELECTOR / "trade_dropdown_row_hovered.png": (94, 18),
        SELECTOR / "trade_dropdown_row_selected.png": (94, 18),
        SELECTOR / "trade_dropdown_row_disabled.png": (94, 18),
        SLOTS / DISABLED_SLOT_OVERLAY_FILE: (18, 18),
        ARROWS / "trade_arrow.png": (14, 10),
        ARROWS / "trade_arrow_hovered.png": (14, 10),
        ARROWS / "trade_arrow_selected.png": (14, 10),
        ARROWS / "trade_arrow_disabled.png": (14, 10),
    }
    for path, expected_size in expected_sizes.items():
        name = path.name
        if not path.exists():
            failures.append(f"missing widget {path.relative_to(ROOT)}")
            continue
        with Image.open(path) as source:
            image = source.convert("RGBA")
        if image.size != expected_size:
            failures.append(f"{name}: size {image.size}, expected {expected_size}")
        if name.startswith("trade_arrow"):
            mismatch = first_vertical_symmetry_mismatch(image)
            if mismatch is not None:
                failures.append(f"{name}: arrow is not vertically symmetric at {mismatch}")


def first_vertical_symmetry_mismatch(image: Image.Image) -> tuple[int, int] | None:
    for y in range(image.height // 2):
        for x in range(image.width):
            if image.getpixel((x, y)) != image.getpixel((x, image.height - 1 - y)):
                return x, y
    return None


def validate_trade_state_colors(failures: list[str]) -> None:
    expected = {
        ROWS / "trade_row.png": TRADE_ROW_NORMAL,
        ROWS / "trade_row_hovered.png": TRADE_ROW_HOVERED,
        ROWS / "trade_row_selected.png": TRADE_ROW_SELECTED,
        ROWS / "trade_row_disabled.png": TRADE_ROW_DISABLED,
        SELECTOR / "trade_dropdown_row.png": TRADE_ROW_NORMAL,
        SELECTOR / "trade_dropdown_row_hovered.png": TRADE_ROW_HOVERED,
        SELECTOR / "trade_dropdown_row_selected.png": TRADE_ROW_SELECTED,
        SELECTOR / "trade_dropdown_row_disabled.png": TRADE_ROW_DISABLED,
    }
    for path, expected_color in expected.items():
        if not path.exists():
            continue
        with Image.open(path) as source:
            actual_color = source.convert("RGBA").getpixel((4, 4))
        if actual_color != expected_color:
            failures.append(
                f"{path.name}: interior color {actual_color}, expected {expected_color}"
            )


def validate_reset_sprites(failures: list[str]) -> None:
    for name in ("reset_trades.png", "reset_trades_hovered.png"):
        path = RESET_SPRITES / name
        if not path.exists():
            failures.append(f"missing sprite {name}")
            continue
        with Image.open(path) as source:
            if source.size != (42, 28):
                failures.append(f"{name}: size {source.size}, expected {(42, 28)}")


def validate_equipment_sprites(failures: list[str]) -> None:
    for name in ("helmet", "chestplate", "leggings", "boots", "shield"):
        path = EQUIPMENT_SPRITES / f"{name}.png"
        if not path.exists():
            failures.append(f"missing equipment sprite {name}.png")
            continue
        with Image.open(path) as source:
            image = source.convert("RGBA")
        if image.size != (16, 16):
            failures.append(f"{name}.png: size {image.size}, expected {(16, 16)}")
        visible_colors = {pixel[:3] for pixel in image.get_flattened_data() if pixel[3] > 0}
        if visible_colors != {(72, 72, 72)}:
            failures.append(f"{name}.png: expected darkened vanilla silhouette")


def validate_capture_sprites(failures: list[str]) -> None:
    for name in ("empty_villager_head.png", "empty_piglin_head.png"):
        path = CAPTURE_SPRITES / name
        if not path.exists():
            failures.append(f"missing capture sprite {path.relative_to(ROOT)}")


def validate_removed_legacy_textures(failures: list[str]) -> None:
    for path in legacy_generated_textures():
        if path.exists():
            failures.append(f"legacy generated texture remains at {path.relative_to(ROOT)}")


def generate_widgets() -> None:
    generate_trade_rows()
    generate_trade_selector()
    generate_disabled_slot_overlay()
    generate_trade_arrows()
    generate_reset_sprites()


def generate_trade_rows() -> None:
    for name, fill, border in (
        ("trade_row", TRADE_ROW_NORMAL, (76, 76, 76, 210)),
        ("trade_row_hovered", TRADE_ROW_HOVERED, (126, 126, 126, 225)),
        ("trade_row_selected", TRADE_ROW_SELECTED, (126, 180, 102, 235)),
        ("trade_row_disabled", TRADE_ROW_DISABLED, (55, 55, 55, 145)),
    ):
        image = Image.new("RGBA", (100, 24), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 99, 23), fill=border)
        draw.rectangle((1, 1, 98, 22), fill=fill)
        draw.line((1, 1, 98, 1), fill=border)
        image.save(ROWS / f"{name}.png")


def generate_trade_selector() -> None:
    image = Image.new("RGBA", (102, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 101, 17), fill=(76, 76, 76, 220))
    draw.rectangle((1, 1, 100, 16), fill=TRADE_ROW_NORMAL)
    draw.rectangle((86, 2, 99, 15), fill=(58, 58, 58, 205))
    image.save(SELECTOR / TRADE_DROPDOWN_FILE)

    for name, fill, border in (
        ("trade_dropdown_row", TRADE_ROW_NORMAL, (76, 76, 76, 210)),
        ("trade_dropdown_row_hovered", TRADE_ROW_HOVERED, (126, 126, 126, 225)),
        ("trade_dropdown_row_selected", TRADE_ROW_SELECTED, (126, 180, 102, 235)),
        ("trade_dropdown_row_disabled", TRADE_ROW_DISABLED, (55, 55, 55, 145)),
    ):
        image = Image.new("RGBA", (94, 18), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 93, 17), fill=border)
        draw.rectangle((1, 1, 92, 16), fill=fill)
        image.save(SELECTOR / f"{name}.png")


def generate_disabled_slot_overlay() -> None:
    image = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 17, 17), fill=(0, 0, 0, 48))
    draw.line((6, 6, 11, 11), fill=(72, 72, 72, 190))
    draw.line((11, 6, 6, 11), fill=(72, 72, 72, 190))
    image.save(SLOTS / DISABLED_SLOT_OVERLAY_FILE)


def generate_trade_arrows() -> None:
    for name, border, body in (
        ("trade_arrow", (12, 12, 12, 255), (218, 218, 210, 255)),
        ("trade_arrow_hovered", (12, 12, 12, 255), (244, 224, 132, 255)),
        ("trade_arrow_selected", (20, 42, 18, 255), (142, 214, 108, 255)),
        ("trade_arrow_disabled", (54, 54, 54, 185), (126, 126, 122, 170)),
    ):
        image = Image.new("RGBA", (14, 10), (0, 0, 0, 0))
        pixels = image.load()
        outer_rows = (
            (8, 9),
            (8, 10),
            (8, 11),
            (0, 12),
            (0, 13),
            (0, 13),
            (0, 12),
            (8, 11),
            (8, 10),
            (8, 9),
        )
        inner_rows = (
            None,
            (8, 9),
            (8, 10),
            (1, 11),
            (1, 12),
            (1, 12),
            (1, 11),
            (8, 10),
            (8, 9),
            None,
        )
        for y, (start, end) in enumerate(outer_rows):
            for x in range(start, end + 1):
                pixels[x, y] = border
        for y, row in enumerate(inner_rows):
            if row is None:
                continue
            for x in range(row[0], row[1] + 1):
                pixels[x, y] = body
        image.save(ARROWS / f"{name}.png")


def generate_reset_sprites() -> None:
    for name, background, arrow in (
        ("reset_trades", (194, 199, 188), (82, 151, 72)),
        ("reset_trades_hovered", (214, 214, 209), (105, 177, 94)),
    ):
        image = Image.new("RGBA", (42, 28), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 41, 27), fill=(35, 35, 35))
        draw.rectangle((1, 1, 40, 26), fill=background)
        draw.line((1, 1, 40, 1), fill=(215, 215, 215))
        draw.line((1, 1, 1, 26), fill=(215, 215, 215))
        shadow = (48, 99, 43)
        draw.line((12, 13, 12, 10, 15, 10, 15, 8, 27, 8), fill=shadow, width=2)
        draw.polygon(((27, 5), (33, 9), (27, 13)), fill=shadow)
        draw.line((30, 15, 30, 18, 27, 18, 27, 20, 15, 20), fill=shadow, width=2)
        draw.polygon(((15, 15), (9, 19), (15, 23)), fill=shadow)
        draw.line((12, 12, 12, 10, 15, 10, 15, 8, 27, 8), fill=arrow)
        draw.polygon(((27, 6), (31, 9), (27, 12)), fill=arrow)
        draw.line((30, 16, 30, 18, 27, 18, 27, 20, 15, 20), fill=arrow)
        draw.polygon(((15, 16), (11, 19), (15, 22)), fill=arrow)
        image.save(RESET_SPRITES / f"{name}.png")


def legacy_generated_textures() -> list[Path]:
    return [
        *(ROOT / "widgets" / name for name in (
            "trade_row.png",
            "trade_row_hovered.png",
            "trade_row_selected.png",
            "trade_row_disabled.png",
            TRADE_DROPDOWN_FILE,
            "trade_dropdown_row.png",
            "trade_dropdown_row_hovered.png",
            "trade_dropdown_row_selected.png",
            "trade_dropdown_row_disabled.png",
            DISABLED_SLOT_OVERLAY_FILE,
            "trade_arrow.png",
            "trade_arrow_hovered.png",
            "trade_arrow_selected.png",
            "trade_arrow_disabled.png",
        )),
        TRADER / "default.png",
        SPRITES / "reset_trades.png",
        SPRITES / "reset_trades_hovered.png",
    ]


def remove_legacy_generated_textures() -> None:
    for path in legacy_generated_textures():
        path.unlink(missing_ok=True)
    legacy_widgets = ROOT / "widgets"
    if legacy_widgets.exists() and not any(legacy_widgets.iterdir()):
        legacy_widgets.rmdir()

def main() -> None:
    parser = argparse.ArgumentParser(description="Generate and validate villager trade GUI textures.")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate committed textures without modifying them.",
    )
    args = parser.parse_args()

    for directory in (
        BACKGROUNDS,
        ROWS,
        SELECTOR,
        ARROWS,
        SLOTS,
        RESET_SPRITES,
    ):
        directory.mkdir(parents=True, exist_ok=True)
    if args.check:
        check_generated_textures()
        return
    remove_legacy_generated_textures()
    for stale_texture in BACKGROUNDS.glob("*.png"):
        if stale_texture.stem not in PALETTES:
            stale_texture.unlink()
    for theme, palette in PALETTES.items():
        generate_theme(theme, palette).save(BACKGROUNDS / f"{theme}.png")
    generate_widgets()
    check_generated_textures()


if __name__ == "__main__":
    main()
