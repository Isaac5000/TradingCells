# Informe de entrega - Trading Cells

Fecha: 2026-07-27

Proyecto: `trading_cells` 1.0.0

Entorno objetivo: Minecraft 26.2.0 / NeoForge 26.2.0.8-beta

## Estado

La revisión solicitada de interfaces, mecánicas, Sonar, eficiencia y arquitectura
está aplicada en el árbol entregado. La compilación completa y las verificaciones
automatizadas terminaron correctamente. Las pruebas que necesitan ejecutar un
mundo real de Minecraft se identifican expresamente en `TEST_REPORT.md`.

## Cambios funcionales

### Criadero de piglins

- Las alternativas válidas son exactamente:
  - 2 chuletas de cerdo cocinadas.
  - 2 bloques de verrugas del Nether.
  - 4 chuletas de cerdo crudas.
  - 6 hongos carmesí.
  - 12 verrugas del Nether.
- Cada ciclo bloquea la receta elegida para no mezclar alimentos durante el
  progreso.
- La receta activa se persiste y sincroniza.
- El menú de ayuda muestra cantidad, icono, nombre, scroll y resaltado de la
  alternativa activa.
- El criadero de aldeanos conserva `empty_bread` y el de piglins usa
  `empty_porkchop`.

### Convertidor de aldeanos

- Los dos slots de pociones admiten internamente hasta 64 unidades idénticas.
- Se aceptan únicamente pociones normales, arrojadizas o persistentes de
  debilidad, incluida la variante larga.
- La identidad completa de componentes debe coincidir para poder apilar.
- La capacidad ampliada está encapsulada en el BlockEntity y no altera el
  tamaño máximo global de los ítems.
- Inserción, extracción, shift-click, teclas numéricas, automatización,
  persistencia y sincronización mantienen la regla de apilado.
- Cada fase consume una sola poción.
- Al extraer hacia el inventario del jugador se reconstruyen unidades vanilla,
  ya que una poción sigue teniendo tamaño máximo 1 fuera de la máquina.

### Descuento temporal por compra masiva

- Existe como máximo una capa temporal por identidad estable de oferta.
- Comprar de nuevo mientras está activa renueva la duración, pero no aumenta
  su magnitud.
- Las expiraciones son independientes entre ofertas.
- El descuento se conserva por NBT y no depende de la posición de la oferta en
  la lista.
- Los descuentos permanentes de curación siguen acumulándose según las reglas
  de Minecraft.
- Trader y Autotrader comparten la misma implementación.
- La próxima expiración se programa directamente; no se recorre ni reescribe
  NBT cada tick.

### Comportamiento conservado

- Los aldeanos bebé no entran en Trader ni Autotrader.
- Los piglins bebé no entran en el trocador.
- El Trader manual mantiene el procesamiento por lotes al extraer el resultado
  con shift-click.
- La conversión conserva los datos e intercambios del aldeano y añade el efecto
  de curación sin reconstruir su catálogo.
- Los bloques de aldeanos usan sonido y partículas de metal; los de piglins
  usan piedra negra.

## Cambios visuales

- Las filas de Trader y Autotrader comparten sprites normal, hover,
  seleccionado y deshabilitado.
- El interior normal usa RGBA `(26, 26, 26, 179)` y el hover
  `(42, 42, 42, 179)`.
- La fila seleccionada añade acento sin perder legibilidad y la deshabilitada
  reduce contraste.
- Las flechas son sprites de 14x10, simétricos verticalmente y con variantes
  normal, hover, seleccionada y deshabilitada.
- Filas, flechas y selector están centralizados en `VillagerTradeSprites`.
- Se regeneraron `empty_potion.png` y `empty_apple.png` para que funcionen como
  sprites visibles de slot.
- La casilla de amapolas de la granja de hierro se desplazó dos píxeles junto
  con su hitbox.
- Se mantienen las correcciones previas de cabeceras, texto `Inventario`,
  equipo, posiciones de intercambios y textura común `empty_capturer`.

## Optimizaciones

- Fondos y paletas de GUI se cachean en lugar de reconstruirse por frame.
- Las coordenadas estáticas de equipo se reutilizan.
- La persistencia de descuentos evita escrituras NBT cuando el estado no ha
  cambiado.
- La expiración temporal se despierta en el tick exacto más próximo.
- Los métodos con complejidad elevada se dividieron en decisiones y operaciones
  pequeñas, reduciendo trabajo duplicado y facilitando pruebas.
- El procesamiento masivo del Trader agrupa actualizaciones de estado y ofertas
  durante el lote.

## Arquitectura

- Las reglas de alimentos y descuentos viven en dominio, sin tipos de
  Minecraft.
- Los casos de uso de criaderos dependen de puertos de aplicación y la
  configuración NeoForge queda en adaptadores.
- La conversión de stacks, NBT, menús y renderizado permanece en adaptadores
  Minecraft/NeoForge.
- `feature/trader` reúne Trader y Autotrader porque comparten oferta, descuento,
  layout y sincronización; el trueque de piglins reutiliza solo conceptos que
  realmente son comunes.
- `feature/captures` no tiene una capa `application` artificial: expone el tipo
  de entidad capturada y adaptadores de NBT/ítems usados por otros casos de uso,
  pero no posee una operación independiente que orquestar. Añadir interfaces
  vacías únicamente para simetría empeoraría la arquitectura.
- `checkArchitecture` impide dependencias Minecraft en dominio/aplicación y
  dependencias cruzadas no autorizadas entre features.

## Archivos Java modificados o añadidos

### Criaderos

- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/adapters/input/BreederBlockEntity.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/adapters/input/BreederMenu.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/adapters/input/MinecraftBreederFood.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/adapters/output/client/BreederBlockEntityRenderer.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/adapters/output/client/BreederScreen.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/application/port/output/BreederSettingsPort.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/application/service/BreederService.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/domain/model/BreederFood.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/domain/model/BreederRecipe.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/domain/model/BreederRules.java`

### Convertidor, granjas y Trader

- `src/main/java/com/cosmocraft/trading_cells/feature/converter/adapters/input/ConverterBlockEntity.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/converter/adapters/input/ConverterIngredientAdapter.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/converter/adapters/input/ConverterMenu.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/farmer/adapters/input/FarmerBlockEntity.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/ironfarm/adapters/output/client/IronFarmScreen.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/trader/adapters/input/AutotraderBlockEntity.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/trader/adapters/input/VillagerTradingCellBlockEntity.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/trader/adapters/minecraft/TemporaryTradeDiscountStore.java` (nuevo)
- `src/main/java/com/cosmocraft/trading_cells/feature/trader/adapters/output/client/AutotraderScreen.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/trader/adapters/output/client/VillagerTradingCellScreen.java`
- `src/main/java/com/cosmocraft/trading_cells/feature/trader/domain/service/TradeDiscountPolicy.java`

### Plataforma, configuración y pruebas

- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/render/BlockEntityItemRenderSupport.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/MachineScreenLayout.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/MachineScreenTheme.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/VillagerGuiThemeColors.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/VillagerTradeScreenCommon.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/VillagerTradeScreenLayout.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/VillagerTradeSprites.java` (nuevo)
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/config/FeatureSettingsProvider.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/config/NeoForgeFeatureSettingsAdapter.java`
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/machine/PortableMachineBlockEntity.java`
- `src/test/java/com/cosmocraft/trading_cells/architecture/DomainRulesVerification.java`

## Texturas modificadas o añadidas

- `src/main/resources/assets/trading_cells/textures/gui/sprites/machines/slots/empty_apple.png`
- `src/main/resources/assets/trading_cells/textures/gui/sprites/machines/slots/empty_potion.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/arrows/trade_arrow.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/arrows/trade_arrow_disabled.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/arrows/trade_arrow_hovered.png` (nueva)
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/arrows/trade_arrow_selected.png` (nueva)
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/rows/trade_row.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/rows/trade_row_disabled.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/rows/trade_row_hovered.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/rows/trade_row_selected.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/selector/trade_dropdown.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/selector/trade_dropdown_row.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/selector/trade_dropdown_row_hovered.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/selector/trade_dropdown_row_selected.png`
- `src/main/resources/assets/trading_cells/textures/gui/trader/widgets/selector/trade_dropdown_row_disabled.png` (nueva)

También se modificaron los validadores:

- `tools/generate_machine_gui.py`
- `tools/generate_villager_trade_gui.py`

## Claves de idioma

No hay claves de idioma modificadas en este conjunto de cambios. Los ficheros
`en_us.json` y `es_es.json` son JSON válidos y conservan paridad exacta de 100
claves. Los nombres españoles solicitados con la preposición `para` ya estaban
presentes y se conservaron.

## Informes relacionados

- `SONAR_REVIEW.md`: decisión para cada aviso suministrado.
- `TEST_REPORT.md`: comandos ejecutados, cobertura y comprobaciones manuales
  pendientes.

