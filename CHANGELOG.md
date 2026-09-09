# Changelog

## 1.0.0 - Unreleased

- Added vanilla Silk Touch II through Arcane Infusion, including protected tool-specific mining speed, collection of otherwise unobtainable blocks, preserved cake portions and archaeology contents, repeatable distance-activated Trial Spawners, and reusable Vaults without permanent player UUIDs.
- Added a persistent Comparator-installed redstone controller for normal and Trial Spawners, including frozen previews, state preservation and an optional REI interaction display.
- Added trusted preserved-Spawner previews and non-duplicating Spawn Egg conversion, including static entity rendering, concise modified-entity tooltips and hierarchy restoration.
- Added optional Jade support for compact machine status, Silk Touch II requirements and configured Spawner entities without making Jade a runtime requirement.
- Added portable villager and piglin trading, breeding, incubation, farming and quarry machines.
- Added villager conversion, automatic trading and iron production.
- Added durable and unbreakable villager and piglin capturers.
- Added the Netherite Piglin Bartering Cell and its five upgrade tiers.
- Added REI process displays for every relevant machine.
- Added dynamic compatibility for external professions, POIs, crops, foods and tool tiers.
- Added schema-v1 Farmer crop datapacks for Villager and Piglin inputs, visual
  stages, support surfaces and Fortune-aware outputs, with isolated rejection,
  immutable reload snapshots and an integrated fallback.
- Added Farmer's Touch and Miner's Touch, high-level enchantment preservation and Roman numerals through level 255.
- Added Experience Storage and NeoForge fluid transfer support for stored XP.
- Replaced the experimental Machine Controller, XP Distributor and XP pipes
  with item, fluid, gas, energy and universal logistics pipes (in development).
- Added an improved 16x16 pipe wrench, signed priorities, nearest/farthest/equal/random
  routing, tier-gated filters and transactional standard capabilities. Items default
  to nearest; fluids, gases and energy default to equal distribution.
- Added one profile-bearing upgrade slot per pipe face, a coordinate selector,
  advanced component/destination rules, bounded subchannel completion and autosave.
- Added editable 32x32 pipe sprites with continuous animation and junctions,
  external-edge shading and omitted internal connection faces.
- Restored machine-facing pipe caps beside transparent or partial blocks while
  keeping pipe-to-pipe connections open. Fixed upgrade drag transactions and
  prevented inventory shortcuts from closing focused logistics text fields.
- Added passive smoothly scrolling network terminals and atomic virtual-grid crafting.
  XP remains interoperable through fluid capabilities, without dedicated pipes.
- Removed the experimental Machine Configurator item while retaining internal
  settings contracts, three redstone pause modes and full-output comparator signals.
- Added the portable, manual Arcane Infuser with data-driven recipes and REI transfer support.
- Added a lilac vanilla-style Arcane Infuser recipe book with search, four
  categories, all/craftable filtering, exact server-side placement, maximum
  placement with Shift and explicit empty recipe slots.
- Aligned the Arcane Infuser recipe book beside its wider machine screen, moved
  its toggle above the output and exposed stored/required XP for ghost recipes.
- Added additive Echo Shard drops to player-killed Wardens, including Looting support.
- Added the Skeleton Farm, Warrior's Touch, renewable Spawners and Arcane Infusion recipes that turn normal eggs into all six supported skeleton Spawn Eggs.
- Added charged-creeper Storm Shards and the six-level Decapitation enchantment for swords and axes, with independent head chances and a one-head-per-entity limit.
- Refined the Skeleton Farm with direct Spawn Egg ingredients, persistent pause control, Sharpness-compatible timing, adaptive translated text, contextual live-loot help, a rotating REI output and corrected in-world previews.
- Standardized stored experience displays on the singular `Level` / `Nivel` label for every stored value.
- Added the Zombie Farm with six selectable targets, per-reward filters, loaded loot-table integration, Zombie-family Spawn Egg infusions and full REI support.
- Added Raider and Creeper Farms with loaded loot-table integration, a charged Creeper variant, Pillager Ominous Banner filtering, Arcane Infusion recipes, REI support and dynamic external targets.
- Added configurable Arthropod, Slime, Guardian, Piglin, Blaze, Ghast, Enderman,
  Shulker, Breeze and Phantom Farms with family tags, real loot tables, sided
  automation, persistence, REI/Jade integration and individual Arcane recipes.
- Added configurable Livestock and Fish Farms with eleven vanilla targets,
  death-loot production, mod-extensible family tags and individual Spawn Egg infusions.
- Added configurable Aquatic, Mount, Amphibian, Bee and Creaking Farms, and added
  Vexes to the existing Raider Farm, while retaining XP-only cycles for targets
  without physical vanilla loot.
- Restored NeoForge item automation for the four historical mob farms and added
  real sided-transfer, activity-state and exact persistent-state GameTests for
  all twenty-one farm families.
- Added 67 themed Spawn Egg infusions and rebalanced all Spawn Egg costs under
  the 550 XP cap corresponding to twenty levels.
- Fixed the configurable Piglin Farm menu crash by keeping equipment previews
  on the dynamic-loot path and rejecting missing loot categories safely.
- Corrected configurable-farm recipes and thematic bases, resized Ghast, Slime
  and Elder Guardian previews, removed the impossible Ghast music disc, and
  made Piglin weapons and natural gold armor drop with vanilla-style wear.
- Changed Arthropod and Phantom Farm bases to Pale Moss, raised Ghast previews
  above their Spawners, and made the Slime Farm floor opaque without changing
  its vanilla Slime Block recipe ingredient.
- Fixed Raider and Creeper Farm target/filter synchronization across menu reopenings, removed Creeper music-disc production and stabilized cached entity previews.
- Added tag-backed Skeleton, Zombie, Raider and Creeper Farm target discovery, individual filters for external loot-table items and safe vanilla fallbacks for invalid third-party data.
- Added the public schema-v1 `mob_farm_target` datapack format for extending all twenty-one registered mob-farm families, including item-tag expansion, per-file rejection and a vanilla fallback.
- Versioned the existing mob-farm catalog payload before publication and synchronized stable family, generator and concrete loot identifiers.
- Extracted combat ownership and a pure composition-based mob-farm cycle core while preserving existing block IDs, NBT, timing, random rolls and family-specific loot rules.
- Added one-level Decapitation upgrades for enchanted books and weapons in the Smithing Table using one Storm Shard, while retaining conventional combining through level VI.
- Added release resource validation for JSON, translations, models, textures, recipes and obsolete data paths.
- Added feature-oriented isolated server GameTests for Block Entity persistence, XP capabilities, Infuser automation boundaries, every public network codec and preserved Spawner behavior.
- Expanded release GameTests to cover all portable-machine drop round trips, sided capabilities, partial outputs and Fortune levels 0, 3, 7 and 255.
- Hardened server payload handlers against negative, stale, unsupported and out-of-range requests without changing valid packet IDs or formats.
- Added a frozen 1.0.0 compatibility manifest for public payload IDs, persistent NBT keys, the canonical 130-recipe catalog and mob-farm schema/protocol versions.
- Added a non-destructive release audit and a single-artifact check for reproducible publication evidence.
- Added release JAR inspection so tests, tools and obsolete resources cannot enter the published artifact.
- Added reproducible server/client benchmark template manifests and fingerprints, preventing comparisons between different worlds or machine states.
- Unified fitted one-line text rendering used by Skeleton and Zombie Farm interfaces without changing their layout.
- Expanded the Villager Farm with balanced tree, moss, aquatic plant, live coral and End chorus profiles while excluding bonemeal substrates and duplicate plant forms.
- Allowed Villager and Piglin Farms to finish partially fitting harvests until no compatible output capacity remains.
- Reworked all mob-farm infusions around central Experience Storage, a bottom Spawner, a thematic base and an Iron Block, with gunpowder and pale moss for the Creeper Farm.
- Standardized every mob-farm frame and its break particles on iron, including inventory models.
- Corrected the ten configurable farm item transforms and orientations, fitted
  oversized entity previews, stabilized Phantom poses and returned crafting
  containers such as water and lava buckets from Arcane Infusion.
- Removed obsolete duplicate block loot tables left under the pre-26.2 plural data directory.
