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

## Runtime entity loot boundary

`platform/neoforge/mobfarm/MobFarmLootTables` owns detached entity creation and
native loot-table execution for all farm adapters. `roll` accepts a concrete
`LivingEntity`, resolves its current `getLootTable()` (including saved
`DeathLootTable` state), and invokes Minecraft's loaded loot pipeline. It does
not construct a table path from the entity ID or require catalog membership.
Player-kill context and the supplied weapon are applied for the roll; the shared
fake player's previous weapon is restored even when a table or consumer fails.

Call on the server thread with a detached entity only. A future capture adapter
can restore entity state on that detached instance before calling this boundary.
No live entity is spawned or removed by the executor. Per-kill batches preserve
the existing ordering of table rolls, equipment rolls and farm-specific filters.
The historical fixed Skeleton Farm drops and external-table fallback policies
remain unchanged. Catalog discovery still uses type defaults for filter snapshots;
it is not the authority for the runtime roll of a supplied entity instance.

The general simulation farm now restores a bounded essence snapshot into that
detached instance. `MobFarmSimulationLoot` composes its native table with combat
heads, charged-creeper shards and the historical equipment/ominous rewards.
It never rerolls the default type table. Each supplement is skipped if that same
item already appeared in the native batch for that kill. Alternative weapons
remain mutually exclusive; equipment probabilities and damage ranges retain the
legacy profiles. Ordinary native drops, including custom tables, remain dynamic.

The simulation menu combines table references, supplement IDs and bounded observed
loot. Observed IDs survive weapon wear, weapon replacement and save/reload, but
are reset for a different creature module. Disabled filters apply after native
and supplemental rolls and do not suppress earned XP. Probability previews use
the same supplement profiles, without random sampling, and retain unknown values
when a native table cannot be analysed safely.

The old adapters keep their original generation policies while their registered
blocks migrate to the general farm. Full rework status, including remaining
capture-interaction and compatibility checks, is tracked in
[`ENTITY_SIMULATION_REWORK.md`](ENTITY_SIMULATION_REWORK.md).

## Examples

The folders under `docs/examples/mob_farm_datapacks/` contain:

- `valid`: a normal descriptor with item and tag filters.
- `override_low` and `override_high`: the same resource ID at different pack
  priorities; the higher pack replaces the lower file.
- `invalid`: an unsupported schema version that is deliberately rejected.
- `fallback`: notes for testing that one broken descriptor does not remove the
  fixed vanilla targets.
