"""Bake GUI backgrounds and validate the complete, owner-organized GUI inventory."""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops
import machine_gui_art
import generate_villager_trade_gui as trade_gui


ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/trading_cells/textures/gui"


def generated_assets() -> dict[Path, Image.Image]:
    return machine_gui_art.generated_assets()


def manually_maintained_sprites() -> dict[Path, tuple[int, int]]:
    sprites = ROOT / "sprites"
    return {
        **{sprites / "shared/slots" / f"{name}.png": (16, 16) for name in (
            "empty_villager_head", "empty_piglin_head", "empty_capturer", "empty_hoe", "empty_potion")},
        **{sprites / "shared/slots/equipment" / f"{name}.png": (16, 16) for name in (
            "helmet", "chestplate", "leggings", "boots", "shield")},
        sprites / "farmer/slots/empty_wheat_seeds.png": (16, 16),
        sprites / "converter/slots/empty_apple.png": (16, 16),
        sprites / "villager_breeder/slots/empty_bread.png": (16, 16),
        sprites / "piglin_breeder/slots/empty_porkchop.png": (16, 16),
        **{sprites / "shared/trading/buttons" / f"{name}.png": (42, 28) for name in (
            "reset_trades", "reset_trades_hovered")},
        **{sprites / "arcane_infuser/buttons" / f"{name}.png": (20, 18) for name in (
            "recipe_book", "recipe_book_highlighted")},
    }


def validate(committed: bool) -> None:
    failures: list[str] = []
    backgrounds = generated_assets()
    sprites = manually_maintained_sprites() | trade_gui.widget_sizes()
    for path, expected in backgrounds.items():
        if not path.exists():
            failures.append(f"missing {path.relative_to(ROOT)}")
            continue
        with Image.open(path) as source:
            actual = source.convert("RGBA")
        if actual.size != expected.size:
            failures.append(f"{path.relative_to(ROOT)}: {actual.size}, expected {expected.size}")
        elif committed and ImageChops.difference(actual, expected).getbbox(alpha_only=False) is not None:
            failures.append(f"{path.relative_to(ROOT)}: does not match generator")
    for path, expected_size in sprites.items():
        if not path.exists():
            failures.append(f"missing {path.relative_to(ROOT)}")
            continue
        with Image.open(path) as source:
            if source.size != expected_size:
                failures.append(f"{path.relative_to(ROOT)}: size {source.size}, expected {expected_size}")
    for path in set(ROOT.rglob("*.png")) - backgrounds.keys() - sprites.keys():
        failures.append(f"GUI texture without a declared consumer: {path.relative_to(ROOT)}")
    if failures:
        raise SystemExit("\n".join(failures))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if not args.check:
        for path, image in generated_assets().items():
            path.parent.mkdir(parents=True, exist_ok=True)
            image.save(path)
    validate(True)
    print("GUI backgrounds, sprites and owner layout valid.")


if __name__ == "__main__":
    main()
