# Revisión de issues de SonarQube

Fecha: 2026-07-27

## Criterio

- **Corregido**: la causa existe y el árbol final contiene una corrección.
- **Falso positivo documentado**: el aviso contradice un contrato comprobado de
  Minecraft/NeoForge o mide una estructura impuesta por el framework. Se usa
  `NOSONAR` únicamente en la línea afectada y con motivo concreto.
- **/TODO revisión manual**: no existe una expresión identificable en el árbol
  actual. Hace falta volver a ejecutar Sonar y obtener ubicación y regla exactas.

No se añadió ninguna exclusión global. No se usó `NOSONAR` para ocultar lógica
corregible.

## Resultado por archivo

| Archivo | Avisos revisados | Decisión y resultado |
|---|---|---|
| `AbstractPortableMachineBlock.java` | wildcard genérico; nulabilidad del override | **Corregido** el wildcard. **Falso positivo documentado** para `@Nullable BlockEntity`: la API de `Block.playerDestroy` admite explícitamente que no exista BlockEntity. |
| `AutotraderBlockEntity.java` | mover método al anónimo; `equals`; 3 clamps; `player` sin usar; complejidad 23; 9 padres | **Corregido**: lógica movida, `Math.clamp`, parámetro/lógica y complejidad. **Falso positivo documentado**: BlockEntity y el proxy Villager tienen identidad de entidad y jerarquía fijada por Minecraft; un `equals` por valor sería incorrecto. |
| `AutotraderMenu.java` | complejidad 28; expresión siempre falsa | **Corregido** mediante helpers de movimiento de stacks y eliminación de la condición imposible. |
| `AutotraderMenuSyncPayload.java` | `TYPE` colisiona con `type()` | **Corregido**: constante `PAYLOAD_TYPE`. |
| `AutotraderScreen.java` | brain method; método vacío; ternario; clamp; complejidades 20/16/26; switch; jerarquías de 6 padres | **Corregido**: render dividido en helpers, comentario del no-op intencional, ternario extraído, `Math.clamp` y switch expression. **Falso positivo documentado** para las jerarquías de Screen/widgets impuestas por Minecraft. |
| `BlockEntityItemRenderSupport.java` | nulabilidad; cast innecesario | **Corregido** el cast. **Falso positivo documentado** para el stack nulo: `SpecialModelRenderer` permite explícitamente un argumento de render ausente. |
| `BreederBlock.java` | wildcard; nulabilidad | **Corregido** el wildcard. **Falso positivo documentado** para el BlockEntity nullable del override de Minecraft. |
| `BreederBlockEntity.java` | 2 métodos al anónimo; import; nulabilidad; 2 clamps; parámetros `pos/state` | **Corregido** todo lo accionable. La dirección nullable de `WorldlyContainer` está **documentada** porque Minecraft usa `null` para acceso sin cara. |
| `BreederBlockEntityRenderer.java` | import; método con 9 parámetros | **Corregido**: import eliminado y datos de render agrupados en un contexto. |
| `BreederMenu.java` | `data` oculta campo; asignación inútil; complejidad 21; expresión falsa | **Corregido**: nombre `capturedData`, eliminación de asignación/condición y extracción de helpers. |
| `BreederScreen.java` | mover método a `VariantButton`; clamp; jerarquía | **Corregido** método y clamp. **Falso positivo documentado** para la herencia requerida por Screen y Button. |
| `ConverterBlockEntity.java` | clamp; bloque vacío; `player` sin usar | **Corregido** clamp y comentario de estado `IDLE`. El parámetro de `MenuProvider` está **documentado localmente** porque la firma del framework lo exige. |
| `ConverterBlockEntityRenderer.java` | variable `villager` oculta campo; 9 parámetros | **Corregido**: renombrado y contexto de render. |
| `ConverterMenu.java` | complejidad 18; método privado sin usar; expresión falsa | **Corregido**: helpers para transferencias, método eliminado y condición imposible retirada. |
| `ConverterService.java` | clamp | **Corregido** con `Math.clamp`. |
| `ConverterStage.java` | clamp | **Corregido** con `Math.clamp`. |
| `DomainRulesVerification.java` | `args` sin usar | **Falso positivo documentado**: la JVM exige `String[] args` en el punto de entrada ejecutado por Gradle. |
| `ExtractTradingCellExperiencePayload.java` | `TYPE` colisiona con `type()` | **Corregido**: `PAYLOAD_TYPE`. |
| `FarmerBlockEntity.java` | clamp | **Corregido**: progreso cargado limitado con `Math.clamp`. |
| `FarmerBlockEntityRenderer.java` | 8 parámetros | **Corregido** agrupando los datos de render. |
| `FarmerCropStackAdapter.java` | clamp | **Corregido** con `Math.clamp`. |
| `FarmerCycle.java` | ternario anidado; 2 clamps | **Corregido**: decisión independiente y `Math.clamp`. |
| `FarmerMenu.java` | import; complejidad 20; expresión falsa | **Corregido**: import retirado, método dividido y condición imposible eliminada. |
| `FeatureSettingsProvider.java` | campo no thread-safe | **Corregido** con `AtomicReference`; no se confía en `volatile` para una operación compuesta. |
| `IncubationCycle.java` | ternario anidado | **Corregido** extrayendo la decisión. |
| `IncubatorBlock.java` | wildcard; nulabilidad | **Corregido** el wildcard. **Falso positivo documentado** para el contrato nullable de `Block.playerDestroy`. |
| `IncubatorBlockEntity.java` | import; nulabilidad; 2 clamps; `pos/state` | **Corregido** lo accionable. El lado nullable de automatización está **documentado** según `WorldlyContainer`. |
| `IncubatorMenu.java` | import; expresión falsa | **Corregido**. |
| `IronFarmBlockEntity.java` | 2 clamps; complejidad 16 | **Corregido** con `Math.clamp` y extracción de decisión. |
| `IronFarmBlockEntityRenderer.java` | 9 parámetros | **Corregido** mediante contexto de render. |
| `IronFarmCycle.java` | 3 clamps | **Corregido** con `Math.clamp`. |
| `IronFarmMenu.java` | import; expresión falsa | **Corregido**. |
| `IronFarmService.java` | ternario anidado | **Corregido** extrayendo la condición. |
| `MachineScreenTheme.java` | constructor con 8 parámetros | **Falso positivo documentado**: cada entrada del enum necesita la tupla completa e inmutable de colores; ocultarla en setters o arrays perdería seguridad de tipos. |
| `PiglinBarteringCellBlock.java` | nulabilidad | **Falso positivo documentado** para el BlockEntity nullable permitido por el override de Minecraft. |
| `PiglinBarteringCellBlockEntity.java` | nulabilidad; 2 `player`; `pos/state`; posible NPE de `getServer()` | **Corregido**: parámetros no necesarios retirados y guard de servidor. El lado nullable de `WorldlyContainer` está **documentado**. |
| `PiglinCapturerItemRenderSupport.java` | bloque vacío; patrones `e`; variable/asignación `rendered` | **Corregido**: comentario del fallback intencional, patrones sin nombre y eliminación de estado inútil. |
| `PortableMachineBlockEntity.java` | nulabilidad incompatible | **Corregido** alineando el override con el contrato no nulo real. |
| `ResetTradesPayload.java` | `TYPE` colisiona con `type()` | **Corregido**: `PAYLOAD_TYPE`. |
| `SelectAutotraderOfferPayload.java` | `TYPE` colisiona con `type()` | **Corregido**: `PAYLOAD_TYPE`. |
| `SelectTradingCellOfferPayload.java` | `TYPE` colisiona con `type()` | **Corregido**: `PAYLOAD_TYPE`. |
| `TimedProcess.java` | clamp | **Corregido** con `Math.clamp`. |
| `TradingCellExperiencePayload.java` | `TYPE` colisiona con `type()` | **Corregido**: `PAYLOAD_TYPE`. |
| `TradingCellMenuSyncPayload.java` | `TYPE` colisiona con `type()` | **Corregido**: `PAYLOAD_TYPE`. |
| `VillagerCapturerItemRenderSupport.java` | bloque vacío; patrones `e`; variable/asignación `rendered` | **Corregido**: comentario del fallback intencional, patrones sin nombre y eliminación de estado inútil. |
| `VillagerGuiTextures.java` | `theme` sin usar | **Corregido** retirando el parámetro. |
| `VillagerGuiThemeColors.java` | `theme` sin usar | **Corregido** retirando el parámetro y cacheando la paleta resultante. |
| `VillagerTradeScreenCommon.java` | 6 ternarios; 8 parámetros; suma a float | **Corregido**: helpers/decisiones independientes y conversión float explícita. **Falso positivo documentado** para la primitiva de dibujo, que necesita geometría y estado explícitos. |
| `VillagerTradeScreenLayout.java` | clamp | **Corregido** con `Math.clamp`. |
| `VillagerTradingCellBlock.java` | nulabilidad | **Falso positivo documentado** para el BlockEntity nullable del override de Minecraft. |
| `VillagerTradingCellBlockEntity.java` | mover método; `equals`; nulabilidad; parámetros sin usar; método sin usar; 9 padres; if fusionable; argumento nullable; NPE de servidor; expresión siempre true | **Corregido** todo lo accionable: método en proxy, firmas limpias, método retirado, if combinado, `requireNonNull`, guard de servidor y condición simplificada. **Falso positivo documentado** para identidad/herencia de Entity y para `Merchant.setTradingPlayer(null)`, que cierra la sesión por contrato. |
| `VillagerTradingCellMenu.java` | colección nula; complejidades 25/35; asignación; clamp; 10 parámetros; breaks/continues; expresión falsa | **Corregido**: colección vacía, helpers/contextos, eliminación de asignación/condición, `Math.clamp` y bucle simplificado. |
| `VillagerTradingCellScreen.java` | método vacío; 2 clamps; 2 ternarios; switch; 3 jerarquías | **Corregido**: no-op explicado, clamps, decisiones extraídas y switch expression. **Falso positivo documentado** para Screen y widgets de Minecraft. |
| `generate_machine_gui.py` | `inner` sin usar | **Corregido**: el valor se usa en la validación visual. |
| `generate_villager_trade_gui.py` | complejidades 17/43; constructor por literal; 2 literales triplicados | **Corregido**: funciones divididas, literal nativo y constantes para `disabled_slot_overlay.png` y `trade_dropdown.png`. |

## Falsos positivos y supresiones locales

Las supresiones quedan limitadas a:

- contratos nullable comprobados en fuentes de Minecraft;
- parámetros obligatorios de JVM, `MenuProvider` o `Merchant`;
- identidad de BlockEntity/Entity, donde implementar `equals` por valor alteraría
  colecciones y ciclo de vida;
- jerarquías de Screen, Widget, Entity y BlockEntity impuestas por Minecraft;
- dos firmas de dibujo/configuración cuyo número de parámetros representa una
  tupla coherente y no una responsabilidad múltiple.

Cada `NOSONAR` contiene su explicación en la misma línea. Sonar debe volver a
ejecutarse sobre este árbol para cerrar formalmente los avisos antiguos.

## Pendientes dudosos

- **/TODO revisión manual** - `AutotraderScreen.java`: un aviso histórico sobre
  resta de `long` no incluye línea ni expresión y la clase actual no declara
  campos `long`.
- **/TODO revisión manual** - `BreederScreen.java`: el mismo aviso no se puede
  asociar a una expresión del árbol actual.
- **/TODO revisión manual** - `VillagerTradingCellScreen.java`: quedan dos
  avisos históricos de resta de `long` sin ubicación; la clase actual no declara
  campos `long`.

No se añadió un comentario `TODO` aleatorio al código porque crearía una nueva
issue y podría ocultar una alerta distinta. Para resolverlos hace falta el
resultado fresco de Sonar con clave de regla, línea y flujo de tipos.

