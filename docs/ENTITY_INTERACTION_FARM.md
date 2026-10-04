# Granja de interacciones

## Contrato

Maquina independiente de `mob_farm`, registrada como
`trading_cells:entity_interaction_farm` (bloque, objeto, Block Entity y menu).
Pertenece a la feature `mobfarm`; no hereda ni ejecuta la simulacion de muerte.
No modifica loot, XP, armas, desgaste ni multiplicadores de la granja anterior.

- Inventario: trabajador adulto `0`, Creature Model `1`, ITEM `2`, TOOL `3`,
  velocidad `4`, salidas `5-31` (nueve columnas, tres filas), capacidad `32`.
- Trabajador, modelo, herramienta y mejora: una unidad. ITEM: pila normal.
- Ciclo: `max(20, ceil(1200 / 2^nivel))`, niveles `0-5`:
  `1200/600/300/150/75/38` ticks. Capacidad independiente: `1/2/4/8/16/32`
  operaciones reales por canal y ciclo, sin multiplicar gratuitamente productos.
- Sin bonificacion de armas, XP generada ni XP operativa.
- Pausa manual conserva progreso. Sin control de redstone ni salida para comparadores.
  Configuraciones antiguas de redstone se ignoran al cargar, sin perder inventario.
- Caras relativas al frontal: arriba trabajador/modelo/velocidad/capacidad, izquierda
  ITEM, derecha TOOL. Salidas extraibles desde cualquier cara; entradas nunca
  extraibles automaticamente. Tolvas y capabilities comparten las reglas.

Receta de Infusor `entity_interaction_farm_infusion`, 50.000 XP, un bloque:

```text
LBL
BDB
LBL
```

`L`: bloque de lapislazuli; `B`: hormigon negro; `D`: dispensador.
Contrato de recetas actualizado explicitamente a 165 entradas; ninguna receta
anterior cambia.

## Ejecucion y aislamiento

`InteractionSource` y `InteractionFarmRules` son dominio puro. Los resultados con
`ItemStack`, entorno y ejecucion Minecraft viven en `adapters/interaction`.
`InteractionHarvestDispatcher` conserva un entorno temporal por servidor y lo
limpia entre intentos; entidades y actores siempre son nuevos.

El entorno es un `ServerLevel` no registrado como dimension jugable, con su
propio almacenamiento temporal, sin guardado del mundo principal ni tics
autonomos. Su contexto neutral es Overworld/llanura. Solo permite bloques
temporales expresamente preparados por un handler. Los drops se capturan antes
de incorporarse a un mundo. Al parar el servidor se cierra y elimina ese
almacenamiento temporal.

El actor no pertenece a la lista de jugadores, esta en supervivencia, sin
permisos administrativos, con una unidad en la mano principal y la otra vacia.
La ruta generica llama una sola vez a `Player.interactOn`, incluidos eventos
NeoForge e interaccion del objeto. No antepone otra llamada a la entidad.

Durante la operacion se bloquean accesos al mundo principal, jugadores reales,
comandos/datos globales, reloj, clima, marcador, punto de aparicion, cambios
externos de bloques, altas de entidades,
efectos externos, menus, monturas, teletransportes y tareas/tics diferidos.
Una operacion prohibida invalida el intento aunque un mod atrape su excepcion.
La limpieza de entidades y actores permanece dentro de esa misma proteccion.

Solo se confirman recursos fisicos resultantes. Alimentacion, nombres, tintes o
equipamiento sin productos no cobran nada. Muerte, retirada, desplazamiento o
sustitucion de la entidad se rechazan. Tambien se rechazan capturadores propios,
`MobBucketItem` y componentes `ENTITY_DATA`/`BUCKET_ENTITY_DATA`, tanto en entradas
como en productos o herramienta restante.

**Limite de compatibilidad:** no es una barrera de seguridad frente a Java
arbitrario. No cubre mods que eludan APIs estandar con referencias externas,
hilos propios, campos globales o formatos privados de captura indetectables.
Operaciones asincronas o dependientes de un mundo real no se ejecutan como
alternativa. Necesitan un handler explicito y pruebas de aislamiento.

## Resultados y persistencia

- ITEM consume una unidad si la interaccion la consumio o transformo; el
  recipiente transformado se envia a las salidas.
- TOOL conserva exactamente el objeto devuelto por la interaccion, incluidos
  componentes, encantamientos, rotura y desgaste real. No se suma desgaste.
- Toque del Granjero cancela unicamente desgaste positivo por las APIs de
  durabilidad dentro del entorno desechable. No modifica componentes ni concede
  inmunidad al uso ordinario fuera de la maquina. Se admite en herramientas de
  `#trading_cells:enchantable/farmers_touch`, compuesto por durabilidad vanilla
  y `#c:tools`; ampliable por datapacks para herramientas externas.
- Se prepara el inventario completo antes de cobrar; cada canal confirma entrada,
  herramienta y productos conjuntamente. Un fallo no bloquea el otro canal.
- Prioridad ITEM/TOOL alternada cada ciclo, con entidades independientes.
- Cada ronda intenta ambos canales antes de la siguiente. Un canal se detiene
  al bloquearse, agotarse o romper su herramienta; el otro puede continuar.
- Si falta espacio, se guarda el resultado sin cobrar ni sortear otra vez.
  Cambiar modelo o componentes del objeto invalida ese pendiente. Cambiar solo
  cantidad permite reponer la misma entrada. Un rollback no invalida pendientes.
- Un resultado incapaz de caber en 27 salidas vacias se rechaza, no queda pendiente.
- Se conservan inventario, progreso, duracion, pausa manual, prioridad y
  pendientes al guardar o romper/recolocar. Entidades de mods ausentes quedan
  inactivas con su modelo recuperable.

NBT nuevo: `InteractionProgress`, `InteractionDuration`, `Enabled`,
`InteractionPriority`, `PendingITEM` y `PendingTOOL`; cada pendiente guarda
`Module`, `Input`, `Remainder` opcional y `Products`. No hay migracion de maquinas
existentes ni cambios en sus claves.

## Extensiones y presentacion

Registro ordenado de `InteractionHarvestHandler`, seguido del resolver generico.
`BeeHarvestHandler` prepara una colmena llena y usa su interaccion vanilla para
miel/panales; el ticker no contiene condiciones especiales para abejas.
`MooshroomHarvestHandler` utiliza la tabla vanilla de esquilado para champinones
rojos/marrones y aplica desgaste real, sin convertir ni quitar setas a la entidad.
Es una excepcion explicita a la interaccion vanilla destructiva de esquilado.
`CreakingHarvestHandler` obtiene 2-3 `resin_clump` por operacion (cantidad del
pulso vanilla del corazon), con un hacha o herramienta con `AXE_STRIP`. Aplica
un punto de desgaste, protegido por Toque del Granjero. No necesita un corazon
real, no dana la criatura ni programa tics; capacidad y pendientes conservan las
mismas reglas atomicas del resto de handlers.
No se implementa aun un formato de handlers por datapack.

GUI compacta de 256x228, progreso, pausa manual e inventario alineado.
Los estados por canal siguen en Jade, sin etiquetas redundantes bajo los slots.
Jade muestra diagnostico y canales; REI muestra la receta de infusor y coste.
Jaula y pedestal reutilizados, sin cartel frontal. Trabajador
con TOOL en principal e ITEM en secundaria; si solo existe uno, va en principal.
El nucleo trasero, conducto y brazo articulado son geometria estatica del bloque,
con texturas estaticas propias, sin calculos de animacion por tick. Bandejas delanteras muestran ITEM y TOOL con
estados de render cacheados; operador y entidad permanecen visibles.
Slots vacios tienen siluetas y salidas/inventario comparten nueve columnas.

## Hitos y evidencia

- [x] Dominio, registro, receta y contrato de recursos.
- [x] Entorno desechable y protecciones de APIs estandar.
- [x] Dos canales, atomismo, pendientes y persistencia.
- [x] Tolvas y tuberias universales reales, capabilities y rollback.
- [x] 12 GameTests nuevos; suite completa de 131 pruebas aprobada el 2026-09-28.
- [x] Leche, estofado, lana, cepillado, miel, panales y alimentacion rechazada.
- [x] Entidad/objeto de prueba fuera del namespace del mod, evento NeoForge y
  drops directos; independencia entre intentos, excepciones y canal sano.
- [x] Cantidades, componentes/encantamientos, rotura, falta de trabajador,
  entidad ausente, salidas parciales/llenas/imposibles, prioridad y reinicio.
- [x] Matriz visual OpenGL/Vulkan, con y sin REI (2026-09-27).
- [x] `clean check releaseCheck` aprobado el 2026-09-28: 131 GameTests,
  35 pruebas Python de recursos, 28 de recetas, dominio, arquitectura,
  portabilidad y un unico JAR publicable.

La fixture `--ui-fixture interaction-farm` abre el menu real, pulsa pausa y
reanudacion, prepara dos trabajadores con uno/dos objetos y abre la receta REI
cuando esta presente. Captura menu, modelos y receta por separado.

Evidencia local en `artifacts/`, a 1280x720, backend real confirmado en cada log:

| Directorio | Backend | REI | Idioma |
| --- | --- | --- | --- |
| `interaction-farm-opengl-final-20260927` | OpenGL | Si | es_es |
| `interaction-farm-vulkan-final-20260927` | Vulkan | Si | en_us |
| `interaction-farm-opengl-no-rei-20260927` | OpenGL | No | es_es |
| `interaction-farm-vulkan-no-rei-20260927` | Vulkan | No | en_us |

Capturas en `run-1/game/screenshots/`: `interaction-farm-menu.png`,
`interaction-farm-models.png` y, con REI, `interaction-farm-recipe.png`.
Evidencia previa a la retirada de redstone: menu sin solapes, controles
comprobados mediante paquetes reales, receta
de 50.000 XP visible. Las capturas sin REI mejoran el encuadre del bloque y
eliminan drops procedentes de la plantilla para revisar ambas manos.
No se deducen mejoras de rendimiento de estas ejecuciones cortas de UI.

Validacion dirigida previa: `checkMobSimulationResources` (224 recursos) y
`testLogisticsResourceValidation` (35 pruebas). La validacion final comprueba
1.185 JSON, 200 PNG y el contrato de 165 recetas. Estos hitos quedan terminados;
no repetir implementacion al reanudar. La publicacion, matriz multijugador y
pruebas con mods externos reales siguen fuera de esta validacion local.

Se sincronizaron, con autorizacion expresa del usuario, los cinco PNG de vial
(vacio y tiers I-IV) con su generador ya existente. No cambia su diseno ni paleta.
Los ensayos previos detectaron intermitencia en dos pruebas antiguas
(`syringe_animated_extraction` y `logistics_partial_chunk_split_reconnect`);
las siguientes suites completas pasaron sin modificar esas pruebas ni mecanicas.

## Retirada de redstone (2026-09-28)

Peticion posterior: eliminar control de redstone y salida de comparador solo en
esta maquina. Pausa manual, recetas, entradas, canales y tiempos no cambian.
Modos NBT antiguos se ignoran y no vuelven a guardarse. El indice de datos del
menu antes usado para redstone queda reservado a cero; el antiguo boton no se acepta.

GameTest `interaction_farm_ignores_redstone` aprobado: senales reales, migracion,
pausa manual y persistencia, salidas llenas sin comparador y rechazo del boton viejo.
Fixture OpenGL sin REI aprobada, con captura revisada y clics de pausa/reanudacion:
`artifacts/interaction-no-redstone-opengl-20260928/run-1/game/screenshots/interaction-farm-menu.png`.
La matriz Vulkan/REI no se repitio en ese paso puntual; su repeticion conjunta
se registra con las correcciones posteriores en `docs/FIXES_20260928.md`.

En ese punto el arbol parcial tenia tres fallos por la ampliacion aun incompleta
de cultivos y salidas. Resueltos con el cambio conjunto: 135/135 GameTests y
`check releaseCheck` aprobados el 2026-09-28. Las salidas antiguas conservan sus
indices y las nueve nuevas se anaden al final. Cierre visual y evidencia actual:
`docs/FIXES_20260928.md`.

## Capacidad y correcciones posteriores (2026-09-30)

Slot de capacidad `32` anadido sin desplazar las 27 salidas existentes. Cinco
tiers con coste real por operacion, Mooshrooms renovables de ambos colores y
Toque del Granjero para herramientas de interacciones comprobados. Brazo con
textura estatica, cartel frontal eliminado y menu de 256x228 sin etiquetas
bajo las entradas. Matriz OpenGL/Vulkan con/sin REI y menu a 854x480 aprobados.
Suite completa: 139 GameTests, `check releaseCheck`. Evidencia y cierre de todos
estos hitos: `docs/FIXES_20260929.md`.
