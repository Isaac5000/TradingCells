# Graph Report - logistics  (2026-10-18)

## Corpus Check
- Corpus is ~21,526 words - fits in a single context window. You may not need a graph.

## Summary
- 837 nodes · 2708 edges · 43 communities (13 shown, 30 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 236 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 23
- Community 24
- Community 25
- Community 26
- Community 30
- Community 31
- Community 33
- Community 34
- Community 35
- Community 36
- Community 37
- Community 38
- Community 39
- Community 40

## God Nodes (most connected - your core abstractions)
1. `LogisticsResourceType` - 86 edges
2. `LogisticsNetworkManager` - 74 edges
3. `LogisticsPipeBlockEntity` - 55 edges
4. `PipeFaceConfiguration` - 52 edges
5. `NetworkTerminalMenu` - 49 edges
6. `LogisticsResourceAdapter` - 42 edges
7. `PipeConfigurationScreen` - 39 edges
8. `PipeConfigurationMenu` - 38 edges
9. `PipeUpgradeTier` - 38 edges
10. `PipeFilterRule` - 37 edges

## Surprising Connections (you probably didn't know these)
- `LogisticsListingEntry` --references--> `LogisticsResourceSnapshot`  [EXTRACTED]
  src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/api/LogisticsListingEntry.java → src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/api/LogisticsResourceSnapshot.java
- `LogisticsNetworkManager` --references--> `LogisticsPipeBlockEntity`  [EXTRACTED]
  src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/input/LogisticsNetworkManager.java → src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/input/LogisticsPipeBlockEntity.java
- `ChannelSearch` --references--> `LogisticsResourceType`  [EXTRACTED]
  src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/input/LogisticsNetworkManager.java → src/main/java/com/cosmocraft/trading_cells/feature/logistics/domain/model/LogisticsResourceType.java
- `NetworkEndpoint` --references--> `PipeFaceConfiguration`  [EXTRACTED]
  src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/input/LogisticsNetworkManager.java → src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/input/PipeFaceConfiguration.java
- `AggregatedResource` --references--> `LogisticsResourceType`  [EXTRACTED]
  src/main/java/com/cosmocraft/trading_cells/feature/logistics/adapters/input/LogisticsNetworkManager.java → src/main/java/com/cosmocraft/trading_cells/feature/logistics/domain/model/LogisticsResourceType.java

## Import Cycles
- None detected.

## Communities (43 total, 30 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (6): ChannelSearch, RouteKey, LogisticsPipeBlockEntity, net.minecraft.world.SimpleContainer, PipeConfigurationMenu, Slot

### Community 1 - "Community 1"
Cohesion: 0.07
Nodes (22): LogisticsResourceAdapter, LogisticsResourceAdapters, AggregatedResource, CandidateProgress, CursorAdvance, Destination, EndpointOrder, ExtractionKey (+14 more)

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (7): LogisticsPipeBlock, NetworkTerminalBlock, PipeConnection, EXTRACT, INSERT, NONE, PIPE

### Community 3 - "Community 3"
Cohesion: 0.12
Nodes (6): NetworkTerminalMenu, Slot, PendingAction, Action, ALLOW, DENY

### Community 4 - "Community 4"
Cohesion: 0.08
Nodes (4): LogisticsNetworkEvents, LogisticsPipeInteractionAdapter, PipeInteractionFace, LogisticsPipeClientInteractionAdapter

### Community 9 - "Community 9"
Cohesion: 0.20
Nodes (6): LogisticsResourceType, ENERGY, FLUID, GAS, ITEM, PipeResourceProfile

### Community 12 - "Community 12"
Cohesion: 0.16
Nodes (7): PipeUpgradeTier, ADVANCED, BARE, BASIC, IMPROVED, INFINITE, ULTIMATE

### Community 14 - "Community 14"
Cohesion: 0.17
Nodes (8): ComponentMatch, EXACT, IGNORE, SUBSET, MatchKind, ID, TAG, PipeFilterRule

### Community 16 - "Community 16"
Cohesion: 0.28
Nodes (3): LogisticsListingEntry, StorageKey, ItemLogisticsAdapter

### Community 26 - "Community 26"
Cohesion: 0.17
Nodes (6): PipeKind, ENERGY, FLUID, GAS, ITEM, UNIVERSAL

### Community 34 - "Community 34"
Cohesion: 0.32
Nodes (4): PipeSideMode, EXTRACT, INSERT, NONE

### Community 38 - "Community 38"
Cohesion: 0.33
Nodes (5): PipeRoutingMode, EQUAL, FARTHEST, NEAREST, RANDOM

### Community 40 - "Community 40"
Cohesion: 0.40
Nodes (5): FilterMode, BLACKLIST, OFF, RULES, WHITELIST

## Knowledge Gaps
- **37 isolated node(s):** `NONE`, `PIPE`, `INSERT`, `EXTRACT`, `ITEM` (+32 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 155 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `LogisticsNetworkManager` connect `Community 1` to `Community 0`, `Community 3`, `Community 4`, `Community 15`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **What connects `NONE`, `PIPE`, `INSERT` to the rest of the system?**
  _37 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.055040197897340756 - nodes in this community are weakly interconnected._
- **Why does `LogisticsPipeBlockEntity` connect `Community 0` to `Community 2`, `Community 8`, `Community 10`, `Community 15`, `Community 24`, `Community 28`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.06685366993922394 - nodes in this community are weakly interconnected._
- **Why does `PipeFaceConfiguration` connect `Community 8` to `Community 0`, `Community 34`, `Community 6`, `Community 9`, `Community 12`?**
  _High betweenness centrality (0.011) - this node is a cross-community bridge._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.058738738738738736 - nodes in this community are weakly interconnected._