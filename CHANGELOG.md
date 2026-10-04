# Changelog

## 1.0.0 - Unreleased

- Renamed capacity upgrade IDs to `capacity_<material>_upgrade`, retaining old
  save aliases. Updated the syringe recipe to iron, lapis lazuli and black concrete.
- Syringe extraction takes 2/4/6/8 seconds by tier. Third-person grip and arm
  poses follow extraction/reloading; mirrored vial paths approach from the other
  hand. Load-state changes no longer trigger a disappearing reequip animation.
- Syringe approach lasts six ticks for every tier; only vial filling slows down.
- Crop farms require a hoe and stop capacity batches when it breaks. Empty speed
  and capacity slots display faint copper upgrades, including farms and quarries.
- Stabilizer Jade progress now includes seconds; entity farm progress bars share
  its cyan color. Removed the redundant stabilization duration/XP note from REI.
- Replaced the Pipe Wrench sprite with custom transparent 32x32 artwork.
- Keep the extraction arm extended through the final filling tick. Corrected
  held Pipe Wrench orientation in both views without changing user-edited pixels.
- Removed the syringe's post-extraction cooldown; reload alone gates its next use.
- Moved all five capacity upgrades from Farms to Items in the creative inventory.

- Added capacity upgrades to Villager and Piglin crop farms, preserving saved
  slot indices. Fortune applies to every harvest; Efficiency does not boost speed.
- Spawner egg extraction now requires Shift-right-click and drops the egg above
  the placed block, without requiring an empty hand or inserting into inventory.
- Adjusted recipe-viewer bottom margins, pipe controls/rule spacing and the
  Autotrader selector arrow. Preserved the approved hand-finished vial PNGs as
  generator sources, including their individual glass transparency.

- Standardized GUI resources as `gui/container/<block_id>/` backgrounds and
  `gui/sprites/<block_id|shared>/` widgets, with explicit screen ownership and
  validation for undeclared textures. Removed 88 obsolete copies after checking
  every destination hash; source artwork is retained.

- Compacted machine, terminal and pipe menus to leave a complete REI search
  footer and at least three item columns at the default automatic GUI scale.
  Native item/slot sizes and the player's configured scale remain unchanged.
- Kept Infuser equipment visible and interactive while its recipe book is open;
  centered the recipe XP cost and separated its bar and controls from the frame.
- Fixed worldless enchantment tooltip indexing that caused an REI NullPointerException.
- Held spawner blocks no longer turn into eggs. Installing a preserved egg now
  restores the complete mount/rider tree; plain eggs clear the previous tree.
- Added renewable Creaking resin harvesting with axes and the Interaction Farm
  activation label. Renamed speed upgrade IDs to `speed_<material>_upgrade`,
  with legacy item aliases; capacity now uses the vanilla bundle icon and recipe.

- Added capacity upgrades to the Interaction Farm, with up to 32 paid operations
  per channel and cycle, persistent pending results and renewable red/brown
  Mooshroom shearing. Existing output slot indices remain unchanged.
- Added vanilla inventory clock/chest emblems to speed/capacity upgrades,
  updated their early-tier recipes and moved speed upgrades to the Items tab.
- Replaced machine and REI backgrounds with themed PNG artwork, kept frames
  outside content, improved trader text contrast and compacted the Interaction
  Farm menu. Its mechanism now uses static textures without a front sign.
  Decorative corners keep their proportions; tiled border details and smaller
  pre-baked variants avoid stretching at compact window sizes.
- Removed 16 unused GUI backgrounds, three unused rendering helpers and their
  obsolete generator paths, retaining the source artwork and active widgets.
  Kept backgrounds in their machine-specific folders and removed 26 unused
  REI size variants instead of distributing them from a shared themes folder.
- Renamed the simulation machine to Entity Farm. Enchantment names remain
  visible while Shift reveals descriptions; only Shift is cyan in the shared hint.
- Extended Farmer's Touch to interaction tools, preventing standard durability
  damage inside the Interaction Farm without protecting ordinary world usage.
- Added a dedicated speed-upgrade input to both Farmers and Quarries, replacing
  Efficiency timing while preserving tool tiers, Fortune, Silk Touch and wear.
  Existing stored output indices remain unchanged; speed is appended after outputs.
- Expanded simulation, interaction, crop, quarry and Netherite Piglin Bartering
  outputs to 27 slots in a 9x3 grid, including automation and portable storage.
- Refined crop/quarry screen backgrounds, added empty-input silhouettes to
  simulation and essence machines, and shared the Infuser XP-mode icon with the Workbench.
- Added an articulated mechanism, rear processing core, conduits and visible
  ITEM/TOOL trays inside the Interaction Farm's existing black/blue cage.
- Capped simulation XP at 10 per entity before batch multiplication without
  reducing previously stored XP.
- Added an independent Entity Interaction Farm with two isolated interaction
  channels, native honey/honeycomb harvesting, atomic persistent pending results,
  sided hopper/pipe automation, speed upgrades and REI/Jade support.
  Manual pause only: no redstone control or comparator output; saved legacy redstone modes are ignored.
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
