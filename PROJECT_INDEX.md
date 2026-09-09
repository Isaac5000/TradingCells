# Trading Cells: indice de rutas

Este archivo localiza codigo con pocas lecturas. Las decisiones y el estado viven
en `PROJECT_CONTEXT.md`; los contratos detallados, en `ARCHITECTURE.md` y `docs/`.

## Ruta de consulta

1. Elige una fila de feature.
2. Busca el simbolo concreto solo bajo esa ruta y sus integraciones indicadas.
3. Abre tests de la misma feature antes de editar.
4. Amplia a codigo compartido solo cuando exista una dependencia real.

Excluir siempre de busquedas amplias: `.gradle/`, `build/`, `logs/`, `run/` y
`run-server/`.

## Entorno y entradas

- Minecraft `26.2.0`, NeoForge `26.2.0.57`, Java 25, Gradle 9.5, mod `1.0.0`.
- Arranque comun/cliente: `platform/neoforge/bootstrap/{TradingCells,TradingCellsClient}.java`.
- Registro y composicion: `platform/neoforge/{registration/Registration,bootstrap/FeatureComposition}.java`.
- Configuracion: `platform/neoforge/bootstrap/Config.java` y `docs/CONFIGURATION.md`.
- Red: `platform/neoforge/network/`.

Todas las rutas Java siguientes parten de
`src/main/java/com/cosmocraft/trading_cells/`.

## Features

| Feature | Ruta principal | Contrato o ancla util |
| --- | --- | --- |
| Capturas | `feature/captures/` | `CapturedMobStackAdapter`, `CaptureInteractionAdapter` |
| Combate | `feature/combat/` | encantamientos, Decapitacion y Fragmentos de la Tormenta |
| Comercio | `feature/trader/` | Trader, Autotrader y trueque de piglins |
| Criaderos | `feature/breeders/` | variantes aldeano/piglin |
| Incubadoras | `feature/incubators/` | crecimiento de capturas |
| Cultivos | `feature/farmer/` | `FarmerCropStackAdapter`, Fortuna, soportes, render y `docs/FARMER_CROP_DATAPACKS.md` |
| Canteras | `feature/quarry/` | catalogo, herramientas y minado profundo |
| Conversion | `feature/converter/` | infeccion, curacion, ofertas y descuentos |
| Hierro | `feature/ironfarm/` | ciclos, salidas y multiplicadores |
| Esqueletos | `feature/skeletonfarm/` | `docs/SKELETON_FARM.md` |
| Zombis | `feature/zombiefarm/` | `docs/ZOMBIE_FARM.md` |
| Saqueadores | `feature/raiderfarm/` | objetivos, estandarte ominoso, botin y XP |
| Creepers | `feature/creeperfarm/` | normal/cargado, botin y Fragmentos de la Tormenta |
| Granjas configurables | `feature/configuredmobfarm/` | diecisiete familias; `docs/MOB_FARM_ROADMAP.md` |
| Experiencia | `feature/experience/` | almacenamiento, calculo y fluido XP |
| Logistica | `feature/logistics/` | tuberias, mejoras, reglas, canales, marcador y terminales; `docs/LOGISTICS.md` |
| Infusor | `feature/infusion/` | `ArcaneInfuserBlockEntity`; `docs/ARCANE_INFUSER.md` |
| Control de maquinas | `feature/machinecontrol/`, `platform/neoforge/machine/` | contratos internos y redstone; objeto Configurador retirado |
| Toque de Seda II | `feature/silktouch/` | `SilkTouchTwo*`, `PreservedSpawner*`; `docs/SILK_TOUCH_II.md` |

La estructura de capas y las dependencias permitidas estan en `ARCHITECTURE.md`.

## Fronteras compartidas

| Responsabilidad | Ruta |
| --- | --- |
| Maquinas, persistencia, diagnostico y configuracion | `platform/neoforge/machine/`, `docs/MACHINE_INFRASTRUCTURE.md` |
| Menus y dibujo comun | `platform/neoforge/menu/`, `platform/neoforge/client/screen/` |
| REI opcional | `platform/neoforge/integration/rei/` |
| Jade opcional | `platform/neoforge/integration/jade/` |
| Catalogos dinamicos | `platform/neoforge/catalog/` |
| Catalogo de granjas | `platform/neoforge/mobfarm/`, `docs/MOB_FARM_DATAPACKS.md` |
| Inventario futuro de criaturas | `docs/MOB_FARM_ROADMAP.md` |
| Fluido XP | `platform/neoforge/fluid/` |
| Reglas puras temporizadas | `shared/machines/domain/model/` |
| Reglas puras de granjas | `shared/mobfarm/domain/model/` |
| Mixins comunes | `src/main/resources/trading_cells.mixins.json` |

## Recursos y pruebas

- Assets: `src/main/resources/assets/trading_cells/`.
- Datos y recetas: `src/main/resources/data/trading_cells/`.
- Unitarios/contratos puros: `src/test/java/`.
- GameTests: `src/gameTest/java/.../gametest/feature/<feature>/`; fixtures en
  `gametest/shared/`.
- Indice de herramientas: `tools/README.md`; recursos en `tools/generate_*` y
  `tools/validate_project_resources.py`.
- Rendimiento: `tools/performance/README.md`; resultados y descartes en
  `tools/performance/RESULTS.md`.
- Publicacion: `docs/RELEASE_CHECKLIST.md` y `docs/releases/1.0.0-validation.md`.

## Validacion dirigida

| Cambio | Comprobacion minima |
| --- | --- |
| Python, CI, rutas o tooling | `python tools/check_portability.py` |
| Dominio puro | `gradlew.bat verifyDomainRules` / `./gradlew verifyDomainRules` |
| Arquitectura Java | `checkArchitecture` |
| Renderizado | `checkGraphicsBackendIndependence` y matriz de `docs/GRAPHICS_BACKENDS.md` |
| Recursos, modelos o recetas | `checkProjectResources` y generador afectado con `--check` |
| Logistica | `checkLogisticsResources`; GameTests `feature/logistics/` |
| Persistencia o mecanica | suite GameTest afectada y `runGameTestServer` |
| Cambio normal completo | `check` |
| Candidata publicable | `clean releaseCheck` |

## Invariantes rapidos

- No cambiar IDs, claves NBT, payloads, recetas congeladas ni formatos persistentes
  sin migracion explicita.
- El servidor decide recetas, XP, botin y transferencias; el cliente presenta.
- REI y Jade siguen opcionales; el servidor dedicado no carga clases cliente.
- Renderizado solo mediante APIs neutrales de Blaze3D, nunca backend concreto.
- No reintroducir optimizaciones rechazadas sin medicion equivalente y repetible.
