# Trading Cells: indice rapido

Lee este archivo antes de recorrer el repositorio. Excluye de las busquedas amplias `.gradle/`, `build/`, `logs/`, `run/` y `run-server/`; contienen resultados o entornos locales, no fuentes del mod.

## Entorno

- Minecraft `26.2.0`, NeoForge `26.2.0.57`, Java 25 y Gradle 9.5.
- Mod `trading_cells`, version `1.0.0`.
- REI es opcional; versiones en `gradle.properties` y dependencias en `build.gradle`.
- Los clientes usan el source set de ejecucion `developmentClient`; el servidor conserva `main` sin REI.
- Comprobacion completa de publicacion: `./gradlew clean releaseCheck`.
- Arranques locales: `./gradlew runClient`, `./gradlew runClientVulkan`, `./gradlew runClientOpenGL` y `./gradlew runServer`.

## Puntos de entrada

- Servidor/comun: `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/bootstrap/TradingCells.java`.
- Cliente: `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/bootstrap/TradingCellsClient.java`.
- Registros: `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/registration/Registration.java`.
- Composicion de casos de uso: `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/bootstrap/FeatureComposition.java`.
- Configuracion: `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/bootstrap/Config.java`.
- Red: `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/network/`.

## Features

| Ruta bajo `feature/` | Contenido principal |
| --- | --- |
| `captures` | Capturadores, liberacion, durabilidad y datos de entidades. |
| `combat` | Encantamientos de combate, Decapitacion, Fragmentos de la Tormenta y eventos globales de botin. |
| `trader` | Trader, Autotrader y trocadores de Piglins normal/Netherite. |
| `breeders` | Criaderos de aldeanos y Piglins. |
| `incubators` | Incubadoras y crecimiento de capturas. |
| `farmer` | Cultivos de aldeanos/Piglins, azadas, fortuna y tiers dinamicos. |
| `quarry` | Canteras, catalogo de menas, picos, mejoras y minado profundo. |
| `converter` | Zombificacion, curacion y conservacion de intercambios. |
| `ironfarm` | Granja de hierro, ciclos y multiplicadores. |
| `skeletonfarm` | Granja de esqueletos, objetivos, filtros de botin, espadas y XP. |
| `zombiefarm` | Granja de zombis, seis objetivos, botin dinamico, espadas y XP. |
| `raiderfarm` | Granja de saqueadores, estandarte ominoso, botin dinamico, espadas y XP. |
| `creeperfarm` | Granja de creepers normales/cargados, Fragmentos de la Tormenta y XP. |
| `experience` | Almacen de XP y transferencia de fluido de experiencia. |
| `infusion` | Infusor Arcano, matriz 3x3 manual, recetas de datos y botin del Warden. |
| `silktouch` | Toque de Seda II, botin de bloques especiales y conservacion de sus Block Entities. |

Cada feature usa, cuando aporta valor, `domain`, `application/port`, `application/service` y `adapters`. Las reglas exactas y excepciones estan en `ARCHITECTURE.md`.

## Codigo compartido

- Maquinas portatiles y persistencia: `platform/neoforge/machine/`.
- Menus y primitivas visuales comunes: `platform/neoforge/menu/` y `platform/neoforge/client/screen/`.
- Cultivos y Canteras conservan un menu logico por familia, pero cada variante tiene layout, pantalla, fondo y capa tematica independientes dentro de su feature.
- Las coordenadas compactas de Cultivos y Canteras en REI pertenecen a `platform/neoforge/integration/rei/` y no dependen de sus menus normales.
- Campo numerico compartido para XP: `platform/neoforge/client/screen/NonNegativeIntegerEditBox.java`.
- Escalado de texto compartido: `platform/neoforge/client/screen/FittedTextRenderer.java`.
- Catalogos dinamicos tolerantes a fallos: `platform/neoforge/catalog/`.
- Catalogo de objetivos y botin de granjas de criaturas: `platform/neoforge/mobfarm/`; usa tags de entidad, descriptores de datapack y referencias ligeras de tablas de botin.
- Fluido de XP: `platform/neoforge/fluid/` y registros `ExperienceFluid*`.
- REI: `platform/neoforge/integration/rei/`.
- Reglas puras compartidas: `shared/machines/domain/model/` y `shared/mobfarm/domain/model/`.

## Recursos

- Idiomas: `src/main/resources/assets/trading_cells/lang/{en_us,es_es}.json`.
- Bloques/items/modelos/texturas: `src/main/resources/assets/trading_cells/`.
- Bases compartidas de mejoras: `assets/trading_cells/textures/item/upgrades/`; los distintivos de Cantera y Trocador se componen con `platform/neoforge/client/render/UpgradeBadgeItemRenderSupport.java`.
- Recetas y datos: `src/main/resources/data/trading_cells/`.
- Tags vanilla ampliados: `src/main/resources/data/minecraft/tags/`.
- Metadatos del mod: `src/main/resources/META-INF/neoforge.mods.toml`.

## Infusor Arcano

- Estado, XP, inventario y commit atomico: `feature/infusion/adapters/input/ArcaneInfuserBlockEntity.java`.
- Interaccion de slots: `feature/infusion/adapters/input/ArcaneInfuserMenu.java`.
- Pantalla y boton REI: `feature/infusion/adapters/output/client/ArcaneInfuserScreen.java`.
- Pedestales, mesa e items en el mundo: `feature/infusion/adapters/output/client/ArcaneInfuserBlockEntityRenderer.java`.
- Codec de recetas y resultados dinamicos: `feature/infusion/adapters/minecraft/{ArcaneInfusionRecipe,ArcaneInfusionResult}.java`.
- Huevos normales ampliables para infusiones: `data/trading_cells/tags/item/arcane_infusion_eggs.json`.
- Guia funcional: `docs/ARCANE_INFUSER.md`.

## Toque de Seda II

- Modificador global de botin: `feature/silktouch/adapters/input/SilkTouchTwoLootModifier.java`.
- Clasificacion de herramientas y copia de datos: `feature/silktouch/adapters/input/SilkTouchTwoDropAdapter.java`.
- Proteccion de velocidad sin el nivel II: `feature/silktouch/adapters/input/SilkTouchTwoMiningSpeedAdapter.java`.
- Recolocacion de datos protegidos: `feature/silktouch/adapters/input/SilkTouchTwoPlacementAdapter.java`.
- Generadores, huevos confiables y jerarquias de entidades:
  `feature/silktouch/adapters/input/PreservedSpawner{Item,Interaction}Adapter.java`.
- Control persistente por comparador y redstone:
  `feature/silktouch/adapters/input/SpawnerRedstoneControl*.java` y mixins de
  `SpawnerBlockEntity`/`TrialSpawner`.
- Tooltip y vista fija del generador en inventario:
  `feature/silktouch/adapters/output/client/PreservedSpawner*.java`.
- Reglas repetibles de generadores de desafio y arcas: `feature/silktouch/adapters/{input,mixin}/` y `trading_cells.mixins.json`.
- REI documenta la instalacion del comparador en la categoria
  `spawner_redstone_control`.
- Bloques ampliables por datapack: `data/trading_cells/tags/block/silk_touch_two/`.
- Registro funcional completo: `docs/SILK_TOUCH_II.md`.

## Jade opcional

- Plugin y proveedores: `platform/neoforge/integration/jade/`.
- La API se compila como `compileOnly`; el runtime de desarrollo puede omitirse
  mediante `-PwithoutJade`.
- Maquinas: XP, nivel y progreso compacto. Bloques especiales: requisito de
  Toque de Seda II y entidad del generador.

## Granja de Esqueletos

- Fachada de reglas propias y botin de esqueletos: `feature/skeletonfarm/domain/model/`.
- Tiempo, bajas simuladas, progreso y filtros compartidos: `shared/mobfarm/domain/model/`.
- Tiers y snapshot de arma/encantamientos: `platform/neoforge/mobfarm/{MobFarmSwordTierCatalog,MobFarmWeaponSnapshot}.java`.
- Decapitacion, Fragmentos de la Tormenta, eventos y herreria: `feature/combat/`.
- Inventario, botin, XP y persistencia: `feature/skeletonfarm/adapters/input/SkeletonFarmBlockEntity.java`.
- Menu ancho basado en Trader: `feature/skeletonfarm/adapters/input/SkeletonFarmMenu.java`.
- Pantalla y renderizado de entidades: `feature/skeletonfarm/adapters/output/client/`.
- Fondo, medidas y paleta de la pantalla: copias independientes dentro de `feature/skeletonfarm`; no dependen de la interfaz del Trader.
- Guia funcional: `docs/SKELETON_FARM.md`.

## Granja de Zombis

- Fachada de reglas propias, filtros y probabilidades: `feature/zombiefarm/domain/model/`.
- Ejecucion de tablas de botin cargadas y fallback vanilla: `feature/zombiefarm/adapters/input/ZombieFarmLootAdapter.java`.
- Inventario, botin, XP, pausa y persistencia: `feature/zombiefarm/adapters/input/ZombieFarmBlockEntity.java`.
- Menu, pantalla y renderizado independientes: `feature/zombiefarm/adapters/{input,output/client}/`.
- Guia funcional: `docs/ZOMBIE_FARM.md`.

## Granjas de Saqueadores y Creepers

- Objetivos, reglas de botin y filtros: `feature/{raiderfarm,creeperfarm}/domain/model/`.
- Ejecucion de tablas cargadas, variantes sinteticas e inventarios:
  `feature/{raiderfarm,creeperfarm}/adapters/input/`.
- Menus, pantallas y renderizadores independientes:
  `feature/{raiderfarm,creeperfarm}/adapters/output/client/`.
- El estado cargado del Creeper se expone solo a su renderizador mediante
  `trading_cells.creeperfarm.mixins.json`; no se usa para reglas de servidor.

## Catalogo de Granjas

- Snapshot inmutable, descubrimiento por tags y fallback vanilla: `platform/neoforge/mobfarm/MobFarmCatalog.java`.
- Carga de descriptores: `platform/neoforge/mobfarm/MobFarmTargetReloadListener.java`.
- Payload versionado conservando el ID publico: `platform/neoforge/network/MobFarmCatalogSyncPayload.java`.
- Formato publico y ejemplos: `docs/MOB_FARM_DATAPACKS.md` y `docs/examples/mob_farm_datapacks/`.

## Verificacion y mantenimiento

- Pruebas/reglas puras: `src/test/java/com/cosmocraft/trading_cells/architecture/DomainRulesVerification.java`.
- Integracion de servidor: registrador en
  `src/gameTest/java/com/cosmocraft/trading_cells/gametest/TradingCellsGameTests.java`,
  suites por feature bajo `gametest/feature/`, fixtures en `gametest/shared/` y
  tarea `runGameTestServer`.
- Regla de paquetes: tarea Gradle `checkArchitecture`.
- Recursos y JAR: tareas `checkProjectResources` y `checkPublishedJarContents`.
- Contratos 1.0.0: `tools/release/contracts-1.0.0.json`, tarea
  `checkReleaseContracts` y verificador `tools/release/verify_release_contracts.py`.
- Auditoria no destructiva: tarea `recordReleaseEvidence`; genera JSON y Markdown
  bajo `build/reports/release/` sin mover ni borrar fuentes.
- Independencia de Vulkan/OpenGL: tarea `checkGraphicsBackendIndependence` y `docs/GRAPHICS_BACKENDS.md`.
- Preparacion Vulkan: `prepareVulkanFmlConfig` desactiva la ventana temprana afectada por `NeoForge#3230` solo en `run/vulkan/`.
- Medicion: `tools/performance/README.md` y `tools/performance/RESULTS.md`.
- Matriz de escenarios: `tools/performance/scenario-matrix.json`; cliente con
  `run_client_benchmark.py`, servidor con `run_server_benchmark.py` y
  comparadores `compare_*_results.py`. Las plantillas se sellan con
  `prepare_template_manifest.py` y una huella SHA-256. Las matrices reales se
  inspeccionan con `inspect_world_block_entities.py` y se preparan mediante
  `prepare_machine_matrix.py`.
- Publicacion: `docs/CURSEFORGE_DESCRIPTION_TEMPLATE.md`, `README.md` y `CHANGELOG.md`.
- Checklist, configuracion, evidencia y futuro:
  `docs/{RELEASE_CHECKLIST,CONFIGURATION,ROADMAP}.md` y
  `docs/releases/1.0.0-validation.md`.
- Candidatos regenerables o prescindibles: `DELETE.md`.

## Invariantes

- No cambiar IDs, claves NBT, payloads ni formatos persistentes sin una migracion explicita.
- Los cambios de inventario deben llamar a la ruta de persistencia/sincronizacion de la Block Entity.
- El servidor decide recetas, XP, botin y transferencias; el cliente solo presenta y solicita.
- REI debe seguir siendo opcional y no debe cargar clases cliente en servidor dedicado.
- El renderizado debe usar APIs neutrales de Blaze3D, sin clases internas de OpenGL o Vulkan.
- El arbol de trabajo puede contener cambios del usuario: no revertir ni limpiar archivos ajenos.
