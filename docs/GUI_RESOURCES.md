# Organizacion de texturas GUI

## Convencion

Raiz: `src/main/resources/assets/trading_cells/textures/gui/`.

```text
gui/
  container/<block_id>/
    background.png
    background_compact.png
    background_small.png
    recipe.png
  sprites/<block_id>/<funcion>/<nombre>.png
  sprites/shared/<funcion>/<nombre>.png
```

El propietario usa exactamente el ID del bloque, no el nombre de una clase,
una familia de implementacion ni el material del fondo. Por ejemplo:
`container/entity_interaction_farm/background.png`. No hay carpetas genericas
`themes`, `machines/backgrounds`, `mobfarm` o `trader/widgets` en la convencion
nueva. `sprites` se conserva porque Minecraft incorpora esos recursos al atlas
GUI; los fondos completos permanecen fuera de ese atlas.

Cada pantalla tiene sus tres variantes, con el mismo contenido y proporciones
anteriores. `compact` y `small` describen el marco, no un escalado de los controles.
`recipe.png` existe solo cuando REI utiliza ese fondo. El resumen de XP de la mesa
vive en `container/essence_workbench/xp_summary.png`. No crear archivos vacios
para interfaces que dibujan sus elementos sin texturas propias.

## Localizar una maquina

Todas las rutas siguientes parten de `gui/container/`:

| Maquina | Directorio |
| --- | --- |
| Cultivo de aldeanos | `farmer/` |
| Cultivo de piglins | `piglin_farmer/` |
| Cantera de aldeanos | `quarry/` |
| Cantera de piglins | `piglin_quarry/` |
| Criaderos | `villager_breeder/`, `piglin_breeder/` |
| Incubadoras | `villager_incubator/`, `piglin_incubator/` |
| Comercio manual | `villager_trading_cell/` |
| Comercio automatico | `autotrader/` |
| Trueque de piglins | `piglin_bartering_cell/` (solo receta REI) |
| Trueque de netherita | `netherite_piglin_bartering_cell/` |
| Hierro | `iron_farm/` |
| Conversion | `converter/` |
| Almacen de experiencia | `experience_storage/` |
| Granja de entidades | `mob_farm/` |
| Granja de interacciones | `entity_interaction_farm/` |
| Mesa de esencias | `essence_workbench/` |
| Estabilizador de esencias | `essence_stabilizer/` |
| Infusor | `arcane_infuser/` |

Se mantienen PNG independientes cuando dos maquinas comparten arte: ahora
pueden localizarse y personalizarse por su ID. El atlas fuente se comparte en
`tools/assets/machine_gui/themes_atlas.png`; no se duplica ni altera ese original.

## Sprites y consumidores

| Ruta bajo `gui/sprites/` | Uso comprobado |
| --- | --- |
| `arcane_infuser/buttons/` | Boton normal/resaltado del recetario |
| `autotrader/selector/`, `autotrader/slots/` | Selector, cuatro estados de fila y overlay de entrada no usada |
| `villager_trading_cell/rows/` | Cuatro estados de ofertas del comercio manual |
| `farmer/slots/empty_wheat_seeds.png` | Silueta de semillas del cultivo de aldeanos |
| `converter/slots/empty_apple.png` | Silueta de manzana de conversion |
| `villager_breeder/slots/`, `piglin_breeder/slots/` | Siluetas de comida |
| `shared/slots/` | Cabezas, capturador, azada y pocion, compartidos entre pantallas |
| `shared/slots/equipment/` | Armadura/mano secundaria del inventario |
| `shared/trading/buttons/` | Reinicio de ofertas manual y automatico |
| `shared/arrows/` | Flechas de comercio, infusor y REI |

Los widgets de comercio usan ahora `blitSprite`, igual que los demas iconos:
sus IDs son relativos a `gui/sprites/` y no cargan tambien una textura suelta.
Los fondos usan `MachineGuiSkin`, con propietario explicito. Ya no se elige el
fondo interpretando el nombre de una textura vanilla de bloque.

## Auditoria y validacion

Antes de copiar se revisaron los 88 PNG: 56 fondos, 14 widgets y 18 sprites.
Todos tenian consumidores; ninguno se borro por carecer de una referencia
literal. Se comprobaron `MachineGuiSkin`, pantallas, REI, `MachineSlotSprites`,
`VillagerTradeSprites`, slots de equipo y las rutas construidas por estado.

Inventario nuevo: 71 fondos (19 pantallas por tres variantes, 13 recetas REI y
un resumen XP) y 32 sprites, 103 PNG en total. Las 15 copias adicionales separan
consumidores antes vinculados al mismo fondo. Cada destino conserva el SHA-256
del origen. Mapa completo y huellas previas:
`artifacts/gui-layout-audit-20261001/moves.json`.

`checkMachineGuiTextures` valida todos los PNG GUI y rechaza archivos sin
consumidor declarado. Las pruebas Python comprueban IDs de bloque, nombres,
inventario exacto y deteccion de sobrantes. La fixture `machine-fixes` comprueba
las rutas Java contra recursos cargados y abre 19 menus, incluido XP.

**Reorganizacion cerrada:** retiradas las 88 copias antiguas tras verificar todos
los SHA-256 de origen y destino. El usuario retiro las carpetas vacias. Quedan
103 PNG GUI declarados, sin copias antiguas. Los originales siguen en `tools/assets`.

Validacion realizada el 2026-10-01:

- Compilacion principal/cliente y reglas puras: aprobadas.
- 43 pruebas Python de recursos y 28 de recetas: aprobadas.
- `check releaseCheck`: aprobado despues de retirar las copias. Registro y
  correcciones posteriores en `docs/FIXES_20261001.md` y
  `artifacts/fixes-20261001/check-release-final.log`.
- OpenGL con REI: `artifacts/gui-layout-opengl-rei-20261001/`, 19 menus,
  12 categorias y comprobacion de existencia de todos los fondos desde Java.
- Vulkan sin REI: `artifacts/gui-layout-vulkan-no-rei-20261001/`, 19 menus
  y las mismas comprobaciones de recursos. Capturas revisadas, aspecto intacto.

Las rutas de sprites incluidas en el atlas y las sueltas se unificaron sin
alterar PNG; se verificaron nuevamente los 103 SHA-256 despues de las pruebas.

Correccion 2026-10-02: `MachineContainerScreen` mantiene la escala configurada,
incluida la automatica predeterminada. Menus compactados a 204 unidades de alto,
slots de 16 con marco/paso de 18 y margen inferior de 24. Reserva lateral para
al menos tres columnas REI, teniendo en cuenta el grosor real del marco.
Terminales y conexiones comparten la colocacion; listas largas conservan scroll.
Los 57 fondos se regeneran para las nuevas dimensiones desde el mismo original,
sin estirar esquinas ni detalles. Los hashes de la reorganizacion anterior son
evidencia historica del traslado, no de estos fondos redimensionados.

Cierre: `clean check releaseCheck` aprobado el 2026-10-02; 103 PNG GUI del JAR
coinciden con las fuentes actuales. Matriz automatica desde 854x480 y pruebas
de armadura con recetario abierto: `docs/FIXES_20261001.md`.
