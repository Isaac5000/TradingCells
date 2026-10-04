# Limpieza de recursos y codigo

## Reorganizacion GUI posterior, cerrada

La peticion posterior exige un estandar por maquina, no solo mover `themes`.
Convencion, auditoria y estado actual: `docs/GUI_RESOURCES.md`. Las 88 texturas
restantes se revisaron antes de copiar y todas tenian consumidores. El nuevo
codigo usa `container/<block_id>` y `sprites/<block_id|shared>`. Retiradas las 88
copias anteriores tras verificar las 103 huellas de destino. El usuario elimino
las carpetas vacias. Inventario GUI exacto y verificaciones globales aprobados;
las fuentes de arte permanecen intactas.

## Revision 2026-10-01

Alcance: fondos GUI sustituidos por los temas actuales y codigo de dibujo sin
consumidores. Sin cambios en IDs, NBT, payloads, mecanicas ni GitHub. No se han
retirado mundos, resultados de pruebas ni originales de `tools/assets/`.

Retirados 16 PNG, 178.564 bytes, bajo
`src/main/resources/assets/trading_cells/textures/gui/`:

- `machines/backgrounds/{villager_breeder,piglin_breeder,converter,farmer,villager_incubator,piglin_incubator,iron_farm}.png`:
  siete fondos sin llamadas a `MachineScreenTheme.backgroundTexture()`.
- `trader/backgrounds/default.png`: el parametro que lo recibia ya se ignoraba;
  el comercio usa `MachineGuiSkin.TRADER`.
- `farmer/{villager,piglin}_{background,surface_overlay}.png` y
  `quarry/{villager,piglin}_{background,surface_overlay}.png`: ocho fondos sin
  referencias en Java, JSON ni herramientas activas.

Retiradas tambien 26 variantes recientes de REI sin consumidores (935.505
bytes): variantes compactas `_200x124_12.png` y `_200x124_6.png`, mas los fondos
200x124 de comercio manual y simulacion. Total de esta revision: 42 PNG y
1.114.069 bytes. Esto reduce recursos empaquetados; no se afirma una mejora
medida de FPS.

Los fondos activos se trasladaron desde `gui/themes/` a sus carpetas:
`farmer/`, `quarry/`, `machines/backgrounds/`, `trader/backgrounds/`, `mobfarm/`
y `arcane_infuser/`. Los 82 archivos trasladados conservaron su SHA-256;
despues se retiraron las 26 variantes sin uso. Quedan 56 fondos activos.
La carpeta `themes` se elimino una vez vacia; generador, referencias Java y
entradas de validacion Gradle apuntan a las ubicaciones correspondientes.

Retirados tres helpers bajo `platform/neoforge/client/screen/`:
`MachineWorkSurface`, `TiledSurfaceRenderer` y `trader/VillagerGuiTextures`.
El primero solo alimentaba la ruta de dibujo antigua; el segundo solo tenia
ese consumidor. El resolutor del comercio solo proporcionaba un argumento
ignorado. Eliminados tambien ese argumento, constantes de atlas, superficies
privadas sin llamadas y campos/getters de tema sin uso.

`generate_machine_gui.py` conserva solo los fondos actuales y la validacion de
siluetas activas. `generate_villager_trade_gui.py` conserva los widgets activos,
sin fabricar el atlas retirado. Sus tareas Gradle mantienen los nombres y
vigilan las entradas reales. Nuevas pruebas impiden regenerar fondos obsoletos
y detectan cambios RGB aunque el canal alfa siga siendo identico. El modo
`--check` del generador de comercio no crea directorios. La fixture visual usa
los accesores de posicion actuales, sin las dos llamadas marcadas para retirada.

Metodo: referencias Java, valores de recursos JSON, generadores y revision de
las llamadas. No basta con que un nombre no aparezca literalmente: se conservan
recursos resueltos por convenciones, registros o integraciones opcionales.
Rutas absolutas comprobadas bajo la carpeta GUI antes del borrado individual.

Validacion: compilacion principal/fixture, `check` y `releaseCheck` aprobados
el 2026-10-01. Pasan 139 GameTests, 42 pruebas Python de recursos y 28 de recetas;
1186 JSON y 241 PNG validados. Contratos intactos: 16 payloads, 74 claves NBT,
165 recetas, schema 1 y protocolo 2. El JAR contiene los 56 fondos con bytes
identicos a los del proyecto y ninguna textura/clase retirada ni `gui/themes/`.
Todos los PNG de `tools/assets/` conservan su huella conjunta anterior:
`008609E30C68277C80E43E82B245AFB881FBB014BA965F03A46417A8900D3316`.

Cliente OpenGL con REI y las rutas finales: 18 menus y 12 categorias revisados
en `artifacts/cleanup-frames-opengl-rei-20261001/`. Vulkan sin REI tambien pasa
los 18 menus en `artifacts/cleanup-paths-vulkan-no-rei-20261001/`.
El resto de evidencia de
marcos se detalla en `docs/FIXES_20260929.md`. Esta limpieza acotada no certifica
la ausencia de codigo muerto en todo el repositorio.

## Historial: limpieza completada 2026-09-22

Lo siguiente describe el estado de aquella fecha. Las cinco mejoras genericas
fueron retiradas posteriormente por el usuario; no restaurarlas. Las bases
vigentes estan en `tools/assets/upgrade_bases/frames/`.

Inventario aplicado por peticion del usuario. Rutas relativas a la raiz del
proyecto. Retirados cinco archivos Java, siete directorios de codigo vacios,
siete PNG obsoletos y cinco carpetas de pruebas sustituidas. PNG y pruebas
retirados: 690 archivos, 298.244.688 bytes (298 MB decimales), ademas del codigo.
La limpieza de pruebas libera disco, no RAM ni FPS del juego.

Auditoria de rutas/tamanos y huellas de originales conservados:
`artifacts/piglin-cleanup-20260922/`. Se comprobaron rutas absolutas dentro del
proyecto, ausencia de enlaces y directorios vacios antes de borrar.

## Retirados

Codigo sin consumidores en fuentes, pruebas, recursos ni herramientas actuales:

- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/SignedIntegerEditBox.java`.
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/DefaultVillagerGuiThemeResolver.java`.
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/VillagerGuiThemeResolver.java`.
- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/screen/trader/VillagerGuiTheme.java`.

Los dos resolutores y su enum se retiraron juntos: solo se referenciaban entre
ellos. `VillagerGuiThemeColors` y el resto de `trader` se conservan: siguen
teniendo consumidores.

Directorios de codigo comprobados vacios, incluidos sus descendientes:

- `src/main/java/com/cosmocraft/trading_cells/feature/machinecontroller/`.
- `src/main/java/com/cosmocraft/trading_cells/feature/experiencenetwork/`.
- `src/main/java/com/cosmocraft/trading_cells/feature/skeletonfarm/adapters/minecraft/`.
- `src/main/java/com/cosmocraft/trading_cells/feature/machinecontrol/adapters/input/`.
- `src/main/java/com/cosmocraft/trading_cells/feature/machinecontrol/adapters/output/`.
- `src/main/java/com/cosmocraft/trading_cells/feature/incubators/application/usecase/`.
- `src/main/java/com/cosmocraft/trading_cells/feature/breeders/application/usecase/`.

Hojas de inspeccion antiguas sin consumidor de compilacion ni del juego:

- `tools/assets/upgrade_bases/before-preview.png`: 140.727 bytes.
- `tools/assets/upgrade_bases/inspect.png`: 221.951 bytes.
- `tools/assets/upgrade_bases/rivet-detail.png`: 26.796 bytes.

No eran texturas fuente: su retirada no modifica las mejoras.

Pruebas fallidas o sustituidas retiradas; no eran plantillas activas:

| Directorio | Tamano | Motivo |
| --- | ---: | --- |
| `artifacts/item-textures-opengl-20260921/` | 58.374.159 bytes | Fallo inicial de atlas y fixture; corregido y repetido |
| `artifacts/module-palm-lower-right-opengl-20260921/` | 60.503.830 bytes | Inclinacion descartada; sustituida por `module-palm-flat-*` |
| `artifacts/simulation-black-neon-thirdperson-20260920/` | 59.808.326 bytes | Pedestal vertical descartado |
| `artifacts/simulation-module-palm-vulkan-20260921/` | 60.780.246 bytes | Posicion anterior que ocultaba la entidad |
| `artifacts/simulation-loot-checks-opengl-20260920/` | 58.375.764 bytes | Fixture fuera del alcance del menu; repetida correctamente el 21 |

Estas cinco carpetas suman aproximadamente 298 MB decimales. Otros resultados
antiguos pueden archivarse, pero no se incluyen como borrables indiscriminados.

## Cambios Asociados Aplicados

Retirados junto con el registro o las comprobaciones obsoletas que los exigian:

- `src/main/java/com/cosmocraft/trading_cells/platform/neoforge/client/render/UpgradeBadgeItemRenderSupport.java`:
  ningun JSON actual solicita `trading_cells:upgrade_badge`. Retirados tambien
  su import y registro en `platform/neoforge/event/CapturerClientEvent.java`.
  Un resource pack externo que usara ese tipo antiguo deja de funcionar.
- `src/main/resources/assets/trading_cells/textures/block/logistics/network_terminal.png`
  y `network_crafting_terminal.png`: atlas antiguos de 32x32, sin referencias
  en modelos actuales. Retiradas su comprobacion, siembra y funcion UV obsoletas
  en `tools/generate_logistics_resources.py`. Los paneles
  vigentes son los archivos terminados en `_front.png` y la carcasa `_body.png`.
- `src/main/resources/assets/trading_cells/textures/item/network_terminal.png`
  y `network_crafting_terminal.png`: copias sin consumidor directo; los items
  usan el modelo del bloque y su panel superior. `generate_family_upgrades.py`
  ya no las produce ni exige. Generador y prueba cuentan 28 salidas, sin borrar
  las bases de los paneles.

## Conservar

- `artifacts/mob-simulation-pedestals-ui-vulkan-20260913/run-1/game/`: plantilla
  empleada por las pruebas visuales recientes.
- `build/performance/templates/logistics-client-1024-active/`: otra plantilla
  registrada en metadatos, pero no presente al verificar esta limpieza. No se
  ha borrado nada de `build/` ni ejecutado `clean`; no confundir una referencia
  historica con una plantilla disponible.
- `artifacts/item-texture-drafts-20260921/`: originales generados, respaldos y
  comparaciones de las texturas nuevas. No eliminarlos con limpieza general.
- Capturas finales `item-textures-animated-*`, `module-palm-flat-*`,
  `pipe-hands-right-opengl-20260921` y `simulation-loot-checks-opengl-20260921`:
  evidencia de los cambios terminados.
- `tools/assets/upgrade_bases/*.png` salvo las tres hojas de inspeccion listadas:
  fuentes activas del generador. Conservar tambien `originals/`.
- Las cinco mejoras genericas en `textures/item/upgrades/`: conservacion pedida
  por el usuario y paletas utilizadas por el generador. No son basura.
- Texturas GUI, mixins, plugins REI/Jade y granjas historicas: pueden cargarse
  mediante IDs construidos, registros o metadatos. Ausencia de una referencia
  literal a su archivo no demuestra que esten sin uso.

Metodo: grafo de valores JSON, referencias de Java y herramientas, revision
manual de candidatos, directorios vacios y metadatos de plantillas. No se ha
usado un simple listado de nombres sin referencias como permiso de borrado.

## Validacion

`checkLogisticsResources check releaseCheck` aprobado el 2026-09-22 a las 06:59
(Europe/Madrid): 126 GameTests, 28 pruebas de recursos y 25 de recetas. Contratos
intactos: 17 payloads, 116 claves NBT, 153 recetas, schema 1 y protocolo 1.
21 PNG fuente/genericos y 25 PNG de mejoras ajenas al trueque conservan sus
SHA-256 previos. Los cinco PNG de trueque incluyen el recorte dorado corregido.
JAR inspeccionado: no contiene clases ni texturas retiradas; sus cinco PNG de
trueque coinciden exactamente con los del proyecto. Arranque y captura real
OpenGL aprobados en `artifacts/piglin-cleanup-client-opengl-20260922/`.
