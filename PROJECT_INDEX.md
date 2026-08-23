# Trading Cells: indice rapido

Lee este archivo antes de recorrer el repositorio. Excluye de las busquedas amplias `.gradle/`, `build/`, `logs/`, `run/` y `run-server/`; contienen resultados o entornos locales, no fuentes del mod.

## Entorno

- Minecraft `26.2.0`, NeoForge `26.2.0.57`, Java 25 y Gradle 9.5.
- Mod `trading_cells`, version `1.0.0`.
- REI es opcional; versiones en `gradle.properties` y dependencias en `build.gradle`.
- Los clientes usan el source set de ejecucion `developmentClient`; el servidor conserva `main` sin REI.
- Comprobacion completa: `./gradlew clean build verifyDomainRules checkArchitecture checkGraphicsBackendIndependence`.
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
| `trader` | Trader, Autotrader y trocadores de Piglins normal/Netherite. |
| `breeders` | Criaderos de aldeanos y Piglins. |
| `incubators` | Incubadoras y crecimiento de capturas. |
| `farmer` | Cultivos de aldeanos/Piglins, azadas, fortuna y tiers dinamicos. |
| `quarry` | Canteras, catalogo de menas, picos, mejoras y minado profundo. |
| `converter` | Zombificacion, curacion y conservacion de intercambios. |
| `ironfarm` | Granja de hierro, ciclos y multiplicadores. |
| `skeletonfarm` | Granja de esqueletos, objetivos, filtros de botin, espadas y XP. |
| `zombiefarm` | Granja de zombis, seis objetivos, botin dinamico, espadas y XP. |
| `experience` | Almacen de XP y transferencia de fluido de experiencia. |
| `infusion` | Infusor Arcano, matriz 3x3 manual, recetas de datos y botin del Warden. |

Cada feature usa, cuando aporta valor, `domain`, `application/port`, `application/service` y `adapters`. Las reglas exactas y excepciones estan en `ARCHITECTURE.md`.

## Codigo compartido

- Maquinas portatiles y persistencia: `platform/neoforge/machine/`.
- Menus y coordenadas comunes: `platform/neoforge/menu/` y `platform/neoforge/client/screen/`.
- Campo numerico compartido para XP: `platform/neoforge/client/screen/NonNegativeIntegerEditBox.java`.
- Catalogos dinamicos tolerantes a fallos: `platform/neoforge/catalog/`.
- Catalogo de objetivos y botin de granjas de criaturas: `platform/neoforge/mobfarm/`; usa tags de entidad y referencias ligeras de tablas de botin.
- Fluido de XP: `platform/neoforge/fluid/` y registros `ExperienceFluid*`.
- REI: `platform/neoforge/integration/rei/`.
- Reglas puras compartidas: `shared/machines/domain/model/`.

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

## Granja de Esqueletos

- Reglas puras de tiempo, bajas y filtros: `feature/skeletonfarm/domain/model/`.
- Probabilidades compartidas de Decapitación y fragmentos: `feature/skeletonfarm/domain/model/{DecapitationRules,StormShardDropRules}.java`.
- Botín de Fragmentos de la Tormenta y cabezas: `feature/skeletonfarm/adapters/input/{ChargedCreeperLootAdapter,DecapitationLootAdapter}.java`.
- Mejora de libros y armas con Decapitación mediante herrería: `feature/skeletonfarm/adapters/minecraft/DecapitationSmithingRecipe.java`.
- Inventario, botin, XP y persistencia: `feature/skeletonfarm/adapters/input/SkeletonFarmBlockEntity.java`.
- Menu ancho basado en Trader: `feature/skeletonfarm/adapters/input/SkeletonFarmMenu.java`.
- Pantalla y renderizado de entidades: `feature/skeletonfarm/adapters/output/client/`.
- Fondo, medidas y paleta de la pantalla: copias independientes dentro de `feature/skeletonfarm`; no dependen de la interfaz del Trader.
- Guia funcional: `docs/SKELETON_FARM.md`.

## Granja de Zombis

- Reglas puras de tiempo, bajas, filtros y probabilidades: `feature/zombiefarm/domain/model/`.
- Ejecucion de tablas de botin cargadas y fallback vanilla: `feature/zombiefarm/adapters/input/ZombieFarmLootAdapter.java`.
- Inventario, botin, XP, pausa y persistencia: `feature/zombiefarm/adapters/input/ZombieFarmBlockEntity.java`.
- Menu, pantalla y renderizado independientes: `feature/zombiefarm/adapters/{input,output/client}/`.
- Guia funcional: `docs/ZOMBIE_FARM.md`.

## Verificacion y mantenimiento

- Pruebas/reglas puras: `src/test/java/com/cosmocraft/trading_cells/architecture/DomainRulesVerification.java`.
- Regla de paquetes: tarea Gradle `checkArchitecture`.
- Independencia de Vulkan/OpenGL: tarea `checkGraphicsBackendIndependence` y `docs/GRAPHICS_BACKENDS.md`.
- Preparacion Vulkan: `prepareVulkanFmlConfig` desactiva la ventana temprana afectada por `NeoForge#3230` solo en `run/vulkan/`.
- Medicion: `tools/performance/README.md` y `tools/performance/RESULTS.md`.
- Matriz de escenarios: `tools/performance/scenario-matrix.json`; cliente con
  `run_client_benchmark.py`, servidor con `run_server_benchmark.py` y
  comparadores `compare_*_results.py`.
- Publicacion: `docs/CURSEFORGE_DESCRIPTION_TEMPLATE.md`, `README.md` y `CHANGELOG.md`.
- Candidatos regenerables o prescindibles: `DELETE.md`.

## Invariantes

- No cambiar IDs, claves NBT, payloads ni formatos persistentes sin una migracion explicita.
- Los cambios de inventario deben llamar a la ruta de persistencia/sincronizacion de la Block Entity.
- El servidor decide recetas, XP, botin y transferencias; el cliente solo presenta y solicita.
- REI debe seguir siendo opcional y no debe cargar clases cliente en servidor dedicado.
- El renderizado debe usar APIs neutrales de Blaze3D, sin clases internas de OpenGL o Vulkan.
- El arbol de trabajo puede contener cambios del usuario: no revertir ni limpiar archivos ajenos.
