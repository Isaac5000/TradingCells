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

Counts are deterministic. `base_count` defaults to `1` and
`fortune_count` to `0`. Chances use a scale of `1` to `10000`:
`chance + Fortune * fortune_chance`, capped by `maximum_chance`. All three
chance fields default to `10000`, `0` and `10000`. Set
`requires_silk_touch` only when that output needs Silk Touch on the installed
tool.

Catalogs are validated and converted to immutable snapshots after server start
and after a complete datapack reload. Machine cycles only read those snapshots;
they do not scan registries or tags.
