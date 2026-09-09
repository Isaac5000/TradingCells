# Mob-farm target datapacks

Trading Cells 1.0 exposes schema version `1` for adding targets to an already
registered mob-farm family. A descriptor changes discovery, selector order,
generator icon and filterable loot only. Production still runs the entity's
loaded loot table, so a datapack cannot replace or duplicate the real drop roll.

## Location

```text
data/<namespace>/trading_cells/mob_farm_target/<id>.json
```

## Schema 1

```json
{
  "schema_version": 1,
  "family": "trading_cells:skeleton",
  "entity_type": "example_mod:ashen_skeleton",
  "generator_item": "example_mod:ashen_skeleton_spawn_egg",
  "order": 100,
  "loot": {
    "include": ["example_mod:ashen_bone", "#example_mod:ashen_drops"],
    "exclude": ["minecraft:bone"]
  }
}
```

- `family`: one of `trading_cells:skeleton`, `trading_cells:zombie`,
  `trading_cells:raider`, `trading_cells:creeper`,
  `trading_cells:arthropod`, `trading_cells:slime`,
  `trading_cells:guardian`, `trading_cells:piglin`,
  `trading_cells:blaze`, `trading_cells:ghast`,
  `trading_cells:enderman`, `trading_cells:shulker`,
  `trading_cells:breeze`, `trading_cells:phantom`,
  `trading_cells:livestock`, `trading_cells:fish`,
  `trading_cells:aquatic`, `trading_cells:mount`,
  `trading_cells:amphibian`, `trading_cells:bee` or
  `trading_cells:creaking`.
- `entity_type`: registered entity used by the target.
- `generator_item`: item shown as the target generator/icon.
- `order`: ascending selector order; ties use the entity identifier.
- `loot.include` and `loot.exclude`: optional item IDs or item tags prefixed by
  `#`. Tags are expanded on the server and concrete item IDs are synchronized.

The real loot table remains authoritative. `include` makes an item selectable;
it does not create that drop. `exclude` hides/disables a filter; it does not
remove an item produced by another enabled category in the entity's loot table.

## Resolution and fallback

Vanilla targets are installed first, targets found through each family's entity
tag are added second, and datapack descriptors are applied last. Skeletons and
zombies use the vanilla family tags; every other family exposes a matching
`#trading_cells:<name>_farm_targets` tag. Normal resource-pack priority resolves
two files with the same resource ID. If different descriptor IDs target the
same entity, the lexicographically first ID wins and the server writes a warning.

An invalid descriptor is discarded without affecting the others. If rebuilding
the complete snapshot fails, Trading Cells restores its fixed vanilla catalog.
Arbitrary modded loot is labelled as dynamic; exact percentages are shown only
for rules Trading Cells can calculate from the same source used for generation.

This format extends an existing family. Registering a completely new block and
family still requires code.

The complete built-in target inventory and the classification of future mobs
are recorded in [`MOB_FARM_ROADMAP.md`](MOB_FARM_ROADMAP.md).

Charged Creepers are a code-owned synthetic variant of their vanilla entity
type. A descriptor can add a registered modded entity, but cannot create
another synthetic state variant by itself.

## Examples

The folders under `docs/examples/mob_farm_datapacks/` contain:

- `valid`: a normal descriptor with item and tag filters.
- `override_low` and `override_high`: the same resource ID at different pack
  priorities; the higher pack replaces the lower file.
- `invalid`: an unsupported schema version that is deliberately rejected.
- `fallback`: notes for testing that one broken descriptor does not remove the
  fixed vanilla targets.
