# Trading Cells

[IMAGE: Main banner showing representative villager and piglin machines]

Trading Cells turns villagers and piglins into compact, portable automation without discarding the data that makes each creature unique. Capture them, install them in purpose-built machines, automate their work, and move the complete machine later without losing inventories, offers, XP, progress, filters, or configuration.

## 🌐 Languages

- 🇬🇧 English
- 🇪🇸 Spanish

## Core Features

[IMAGE: Villager machines overview]

- Manual and automatic villager trading with discounts, profession levels, persistent offers, mass trading, POIs, and stored trade XP.
- Villager breeding, incubation, farming of Overworld crops, trees, flowers and plants, curing, iron farming, and Overworld quarrying.
- A configurable Skeleton Farm hunts Skeletons, Wither Skeletons, Strays, Bogged, or Parched and lets each supported reward be enabled independently.
- A separate Zombie Farm supports Zombies, Zombie Villagers, Husks, Drowned, Zombified Piglins, and Zoglins while honoring loaded loot-table additions.

[IMAGE: Piglin machines overview]

- Vanilla-style piglin bartering and an advanced Netherite bartering machine with five upgrade tiers, reward filters, and eight output slots.
- Piglin breeding, incubation, Nether crop farming, and Nether quarrying with optional deep mining.

[IMAGE: Capturers holding a villager and a piglin]

- Durable capturers preserve entity NBT and only take damage when releasing a creature.
- Unbreakable capturer recipes provide a permanent late-game version.
- Baby villagers and piglins are rejected by machines that require adult workers.

## Experience Storage

[IMAGE: Experience Storage screen and a connected fluid pipe]

Store or withdraw a chosen number of levels from a dedicated portable block; leave the amount blank to transfer everything possible. Its overflow-safe capacity reaches 2,147,483,647 XP points. Experience uses NeoForge's fluid transfer API at one XP point per fluid unit, allowing compatible third-party pipes and tanks to insert or extract it. Villager Traders and Autotraders expose their trade XP as output-only fluid storage.

## Arcane Infusion

[IMAGE: Arcane Infuser screen with a complete recipe]

The Arcane Infuser uses a manual 3x3 ingredient grid and stores up to 2,147,483,647 XP points. Its output behaves like a crafting table: resources are consumed atomically only when the player takes a valid result. Item hoppers and pipes cannot automate it, while NeoForge-compatible fluid pipes can still insert or extract liquid experience.

It can also create vanilla Silk Touch II. Other mods still detect it as normal `minecraft:silk_touch`, while Trading Cells uses level II to collect otherwise unobtainable blocks such as Spawners, Trial Spawners, Vaults, Reinforced Deepslate and Budding Amethyst. Stateful blocks retain their entity, configuration or archaeology data when moved.

Its Silk Touch II infusion uses a Silk Touch I book in the center, four Echo
Shards, one Amethyst Shard, two Turtle Eggs and a Nether Star. The input book
must contain only Silk Touch I so valuable multi-enchanted books cannot be
consumed accidentally.

Collected Spawners show a fixed entity preview and can be converted into one
matching Spawn Egg without duplication. Plain entities create a normal vanilla
egg; modified entities preserve relevant equipment, effects, passengers and
mount data in a glinting egg with a concise tooltip.

Right-clicking a normal Spawner or Trial Spawner with a Redstone Comparator
installs a persistent redstone controller and consumes the comparator. A
constant signal fully pauses a normal Spawner. On a Trial Spawner it blocks new
trials while allowing an already active trial to finish. REI documents both
interactions.

- Farmer's Touch costs 15,000 XP and combines a book with a Chorus Flower, four crops, a Totem of Undying, a Nether Star, and a Netherite Block.
- Miner's Touch costs 30,000 XP and uses an Echo Shard, two End Stone, and two Sculk Catalysts alongside the shared late-game ingredients.
- A third 5,000 XP recipe transforms a captured unemployed villager into a Nitwit without replacing its other saved data.
- The Skeleton Farm costs 50,000 XP and combines its five required skeleton Spawn Eggs around a central Spawner; Skeleton Horses remain selectable without entering the block recipe.
- The Zombie Farm costs 50,000 XP and combines five Zombie-family Spawn Eggs around a central Spawner; Zoglins remain selectable without being required by the block recipe.
- The Raider Farm costs 100,000 XP and combines Pillager, Evoker, Ravager and Witch Spawn Eggs with a Spawner and any Ominous Bottle. Pillagers expose their Ominous Banner as a separate loot filter.
- The Creeper Farm costs 25,000 XP and supports normal and charged Creepers, including renewable Storm Shards.
- Warrior's Touch costs 45,000 XP and prevents sword durability loss inside every Trading Cells mob farm.
- Decapitation costs 25,000 XP and uses one Storm Shard in each corner to create a level-I book for swords or axes.
- Six compact recipes turn a tagged normal egg, including Turtle and Sniffer Eggs, into a Skeleton, Stray, Bogged, Parched, Wither Skeleton, or Skeleton Horse Spawn Egg. Costs range from 55 XP to 160 XP, so the rarest conversion never exceeds ten vanilla levels.
- A renewable late-game Spawner recipe uses Iron Bars, Obsidian, one Dragon's Breath, and a Nether Star.
- Player-killed Wardens add one Echo Shard to their normal loot. Looting can add up to its level to the shard and guarantees between one and its level of extra Sculk Catalysts.

## REI Integration

[IMAGE: REI category tabs]

Roughly Enough Items is optional. When installed, it displays the processes for breeders, incubators, crop farms, the converter, iron, skeleton, zombie, raider and creeper farms, both quarries, standard piglin bartering, Netherite piglin bartering, and data-driven Arcane Infusion. Normal crafting recipes remain available through REI as usual.

Jade support is optional as well. It can display compact machine XP/progress
information plus Silk Touch II harvesting requirements and configured Spawner
entities. Trading Cells does not require Jade on either the client or server.

## Datapack Extensibility

Skeleton, Zombie, Raider and Creeper Farms can discover compatible targets from tags and can be extended through the public `trading_cells/mob_farm_target` datapack format. Packs may define the target entity, generator icon, ordering, and visible loot filters without replacing the entity's real loot table. Invalid third-party entries are isolated and the built-in vanilla catalog remains available as a fallback.

## Small but Important Improvements

- Farmer's Touch prevents hoe durability loss inside both crop farms.
- Miner's Touch prevents pickaxe durability loss inside both quarries.
- Warrior's Touch prevents sword durability loss inside every Trading Cells mob farm.
- Decapitation has six levels and raises head chances independently from Looting. On Wither Skeletons both levels contribute to one roll with at most one skull.
- Decapitation combines normally or gains exactly one level in a Smithing Table by combining an enchanted book or weapon with one Storm Shard, up to level VI.
- Conventionally named external heads in `#minecraft:skulls` are detected dynamically across mod namespaces.
- Fortune scales machine output beyond vanilla level III where supported.
- Fortune and Silk Touch can coexist in quarries; together they increase ore selection and ore-block yield.
- Efficiency is capped functionally at level V in crop farms and quarries.
- Adding an enchantment in an anvil no longer deletes an existing command-level enchantment.
- Enchantment levels XI through CCLV use Roman numerals.
- Over-limit enchantment names use a dynamic blue-to-green-to-magenta color range.
- External villager professions use their registered translated names instead of raw identifiers.
- External professions, POIs, crops, foods, biome skins, and tool tiers are discovered dynamically with a vanilla fallback if third-party data is invalid.
- External monsters in the Skeleton, Zombie, Raider or Creeper discovery tags join their farm selector, with one filter per enumerable loot-table item and fixed targets retained as a safe fallback.

## Configuration

[IMAGE: NeoForge configuration screen]

Configure machine timers, capturer durability, hoe damage, infinite villager trades, and the additive iron-farm production bonus. Defaults are designed to work as a complete progression without mandatory configuration.

## Graphics Compatibility

Trading Cells uses Minecraft 26.2's backend-neutral Blaze3D rendering APIs and supports the official OpenGL and experimental Vulkan backends without forcing either one. Minecraft may fall back to OpenGL when Vulkan is unavailable. This statement covers Minecraft's built-in backends and does not claim compatibility with the legacy third-party VulkanMod.

NeoForge 26.2.0.57 has a known upstream early-loading-window issue when Vulkan is selected. Until NeoForge resolves it, set `earlyWindowControl = false` in `config/fml.toml` when using Vulkan. OpenGL is unaffected.

## Requirements

- Minecraft 26.2.0
- NeoForge 26.2.0.57 or a later compatible 26.2 build
- Java 25
- Roughly Enough Items 26.2.820+ (optional, client-side)

## Gallery

[IMAGE: Trader and Autotrader screens]

[IMAGE: Villager and Piglin Crop Farms]

[IMAGE: Villager and Piglin Quarries]

[IMAGE: Netherite Piglin Bartering filters and upgrades]

## Modpack Permission

[REPLACE WITH YOUR MODPACK PERMISSION POLICY]

## Support

[REPLACE WITH ISSUE TRACKER OR CONTACT LINK]
