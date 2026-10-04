from __future__ import annotations

import argparse
from pathlib import Path
from typing import Final

from PIL import Image, ImageDraw

ROOT: Final = Path(__file__).resolve().parents[1] / "src/main/resources/assets/trading_cells/textures/gui"
SPRITES: Final = ROOT / "sprites"
ROWS: Final = SPRITES / "villager_trading_cell" / "rows"
SELECTOR: Final = SPRITES / "autotrader" / "selector"
ARROWS: Final = SPRITES / "shared" / "arrows"
SLOTS: Final = SPRITES / "autotrader" / "slots"
RESET_SPRITES: Final = SPRITES / "shared" / "trading" / "buttons"
EQUIPMENT_SPRITES: Final = SPRITES / "shared" / "slots" / "equipment"
CAPTURE_SPRITES: Final = SPRITES / "shared" / "slots"
TRADE_DROPDOWN_FILE: Final = "trade_dropdown.png"
DISABLED_SLOT_OVERLAY_FILE: Final = "disabled_slot_overlay.png"
TRADE_ROW_NORMAL: Final = (26, 26, 26, 179)
TRADE_ROW_HOVERED: Final = (42, 42, 42, 179)
TRADE_ROW_SELECTED: Final = (24, 34, 24, 190)
TRADE_ROW_DISABLED: Final = (24, 24, 24, 118)


def validate_widgets() -> None:
    failures: list[str] = []
    validate_widget_sizes(failures)
    validate_reset_sprites(failures)
    validate_equipment_sprites(failures)
    validate_capture_sprites(failures)
    validate_trade_state_colors(failures)
    if failures:
        raise SystemExit("\n".join(failures))


def widget_sizes() -> dict[Path, tuple[int, int]]:
    return {
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


def validate_widget_sizes(failures: list[str]) -> None:
    for path, expected_size in widget_sizes().items():
        name = path.name
        if not path.exists():
            failures.append(f"missing widget {path.relative_to(ROOT)}")
            continue
        with Image.open(path) as source:
            image = source.convert("RGBA")
        if image.size != expected_size:
            failures.append(f"{name}: size {image.size}, expected {expected_size}")
        # Trade arrows use a one-pixel directional bevel. Their upper and lower
        # edges are intentionally asymmetric, so dimensions and generated-state
        # colors are the stable validation contract.


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


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate and validate villager trade GUI textures.")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate committed textures without modifying them.",
    )
    args = parser.parse_args()

    if args.check:
        validate_widgets()
        print("Villager trade widgets valid.")
        return
    for directory in (
        ROWS,
        SELECTOR,
        ARROWS,
        SLOTS,
        RESET_SPRITES,
    ):
        directory.mkdir(parents=True, exist_ok=True)
    generate_widgets()
    validate_widgets()
    print("Villager trade widgets valid.")


if __name__ == "__main__":
    main()
