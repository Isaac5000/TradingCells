# Farmer crop datapacks

Trading Cells loads additive crop descriptors from
`data/<namespace>/trading_cells/farmer_crop/*.json`. Descriptors use
`schema_version: 1` and extend either the Villager or Piglin crop catalog. They
do not replace built-in crops: an invalid file is rejected independently and
the integrated catalog remains available.

## Format

```json
{
  "schema_version": 1,
  "kind": "villager",
  "input": "minecraft:dead_bush",
  "block": "minecraft:dead_bush",
  "support": "minecraft:sand",
  "growth_style": "scaled",
  "render_support": "floor",
  "visual_stages": 6,
  "outputs": [
    {
      "item": "minecraft:stick",
      "base_count": 1,
      "fortune_count": 1,
      "chance": 10000,
      "fortune_chance": 0,
      "maximum_chance": 10000,
      "requires_silk_touch": false
    }
  ]
}
```

Required fields:

- `schema_version`: currently `1`.
- `kind`: `villager` or `piglin`.
- `input`: item accepted by the crop slot.
- `block`: block shown while the crop grows.
- `support`: block shown as its fixed support surface.

Optional fields:

- `growth_style`: `natural` (default) or `scaled`.
- `render_support`: `floor` (default), `wall`, `water` or `ceiling`.
- `visual_stages`: visual stage count from `2` to `64`; default `8`.
- `outputs`: harvest entries. An empty list permits an XP-only cycle.

Counts use `base_count + Fortune * fortune_count` (defaults `1` and `0`).
Chances use a scale of `1` to `10000`: `chance + Fortune * fortune_chance`,
capped by `maximum_chance`. Chance fields default to `10000`, `0` and `10000`. Set
`requires_silk_touch` only when that output needs Silk Touch on the installed
tool.

Catalogs are validated and converted to immutable snapshots after server start
and after a complete datapack reload. Machine cycles only read those snapshots;
they do not scan registries or tags.

## Discovery and upgrades

Villager farms automatically discover registered BlockItems in
`minecraft:villager_plantable_seeds` or `trading_cells:villager_farmer_plants`.
This is tag-based, not automatic support for every mod plant. Vanilla trees
have explicit profiles. Mod saplings do not infer their logs, leaves or fruits:
provide a descriptor with those outputs. Piglin farms extend their built-in
catalog through descriptors, not the villager discovery tags.

Both kinds support `requires_silk_touch` per output. This suppresses that output
without Silk Touch; it does not forbid planting or suppress other outputs.

Both farms require a supported hoe to cultivate, including custom datapack
crops. A missing hoe reports `tool_required`; when the hoe breaks, remaining
capacity attempts stop immediately. Inserting a replacement resumes operation.

Capacity upgrades add 2/4/8/16/32 harvest attempts per cycle, with ordinary tool
wear per harvest. Speed upgrades control cycle acceleration. Efficiency
on the hoe does not increase speed; Fortune, Silk Touch and
Farmer Touch retain their behavior. Capacity is appended after the existing
speed slot, preserving old saved inventory indices and accepting side insertion.
Fortune applies independently to every harvest, including the extra harvests
from capacity upgrades and custom datapack outputs.
