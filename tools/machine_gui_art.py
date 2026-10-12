"""Bake generated GUI artwork to exact layouts; no painted backgrounds at runtime."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools/assets/machine_gui/themes_atlas.png"
OUTPUT = ROOT / "src/main/resources/assets/trading_cells/textures/gui"
FRAME_OUTSETS = (21, 12, 6)
THEMES = ("farmer", "piglin_farmer", "quarry", "piglin_quarry", "villager_breeder",
          "piglin_breeder", "trader", "piglin_trader", "iron_farm", "converter", "simulation", "infusion")
SCREENS = {
    "farmer": ("farmer", (348, 204)), "piglin_farmer": ("piglin_farmer", (348, 204)),
    "quarry": ("quarry", (348, 204)), "piglin_quarry": ("piglin_quarry", (348, 204)),
    "villager_breeder": ("villager_breeder", (204, 204)),
    "piglin_breeder": ("piglin_breeder", (204, 204)),
    "villager_incubator": ("villager_breeder", (204, 204)),
    "piglin_incubator": ("piglin_breeder", (204, 204)),
    "villager_trading_cell": ("trader", (348, 204)), "autotrader": ("trader", (348, 204)),
    "netherite_piglin_bartering_cell": ("piglin_trader", (204, 204)),
    "iron_farm": ("iron_farm", (204, 204)), "experience_storage": ("iron_farm", (204, 204)),
    "converter": ("converter", (204, 204)),
    "mob_farm": ("simulation", (348, 204)),
    "entity_interaction_farm": ("simulation", (256, 204)),
    "essence_workbench": ("simulation", (332, 204)),
    "essence_stabilizer": ("simulation", (236, 204)),
    "arcane_infuser": ("infusion", (204, 204)),
}
RECIPE_THEMES = {owner: theme for owner, (theme, _) in SCREENS.items()
                 if owner not in ("villager_trading_cell", "autotrader", "experience_storage",
                                  "mob_farm", "entity_interaction_farm", "essence_workbench", "essence_stabilizer")}
RECIPE_THEMES["piglin_bartering_cell"] = "piglin_trader"
FRAME_SUFFIXES = {21: "", 12: "_compact", 6: "_small"}


def paste_tiled(result, tile, box):
    x0, y0, x1, y1 = box
    for y in range(y0, y1, tile.height):
        for x in range(x0, x1, tile.width):
            result.paste(tile.crop((0, 0, min(tile.width, x1 - x), min(tile.height, y1 - y))), (x, y))


def fit_panel(source, size, outset=None):
    """Preserve square corners and repeat uniformly scaled border artwork offline."""
    width, height = size
    if outset is None:
        outset = 0 if size == (78, 36) else FRAME_OUTSETS[0]
    frame = outset + 3
    width += 2 * outset
    height += 2 * outset
    result = Image.new("RGBA", (width, height))
    patches = ((0, 56), (56, 200), (200, 256))
    dx = (0, frame, width - frame, width)
    dy = (0, frame, height - frame, height)
    for row in range(3):
        for col in range(3):
            part = source.crop((patches[col][0], patches[row][0], patches[col][1], patches[row][1]))
            box = (dx[col], dy[row], dx[col + 1], dy[row + 1])
            if row == 1 and col == 1:
                result.paste(part.resize((box[2] - box[0], box[3] - box[1]), Image.Resampling.NEAREST), box[:2])
            else:
                tile = part.resize((round(part.width * frame / 56), round(part.height * frame / 56)), Image.Resampling.NEAREST)
                paste_tiled(result, tile, box)
    return result


def generated_assets():
    result = {}
    with Image.open(SOURCE) as atlas:
        for index, name in enumerate(THEMES):
            col, row = index % 4, index // 4
            # The generated sheet has a small unused strip below the third row.
            x0, x1 = (0, 362, 724, 1086, 1448)[col:col + 2]
            y0, y1 = (0, 350, 701, 1062)[row:row + 2]
            panel = atlas.crop((x0 + 2, y0 + 2, x1 - 2, y1 - 2)).convert("RGBA").resize((256, 256), Image.Resampling.NEAREST)
            if name == "infusion":
                with Image.open(ROOT / "tools/assets/matter_synthesizer/panel.png") as generated:
                    panel = generated.convert("RGBA")
            for owner, (theme, size) in SCREENS.items():
                if theme == name:
                    for outset in FRAME_OUTSETS:
                        result[OUTPUT / f"container/{owner}/background{FRAME_SUFFIXES[outset]}.png"] = fit_panel(panel, size, outset)
            for owner, theme in RECIPE_THEMES.items():
                if theme == name:
                    result[OUTPUT / f"container/{owner}/recipe.png"] = fit_panel(panel, (200, 124))
            if name == "simulation":
                result[OUTPUT / "container/essence_workbench/xp_summary.png"] = fit_panel(panel, (78, 36), 0)
    return result
