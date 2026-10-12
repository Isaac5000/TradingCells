# Sintetizador de materia

Desde 2026-10-10, el antiguo Infusor Arcano se presenta como Sintetizador de
materia / Matter Synthesizer. Conserva `trading_cells:arcane_infuser`, tipo de
receta `arcane_infusion`, clases, payloads y NBT existentes. No migra inventarios
ni modifica recetas por este cambio de aspecto.

## Maquina y presentacion

Camara central abierta con tres emisores, plataforma inferior y pantalla en
la parte superior. Modelo estatico propio; solo los nueve ingredientes se
renderizan dinamicamente dentro de la camara. Ya no utiliza jaula compartida,
pedestales de obsidiana ni mesa de encantamientos visual.

Cuatro texturas de bloque 16x16 en `textures/block/arcane_infuser/`. Menu y REI en
`textures/gui/container/arcane_infuser/`, con marco metalico y detalles cian.
Fuente generada conservada en `tools/assets/matter_synthesizer/atlas.png`;
exportadores `generate_matter_synthesizer.py` y `generate_machine_gui.py`.
Los ingredientes animados actualizan su estado, sin rebakear la camara.
La plataforma tiene nueve casillas, con cada ingrediente centrado en su
posicion 3x3. UV de carcasa a escala uniforme; pantalla superior cuadrada,
sin estirar el arte sobre un rectangulo. La mesa de huevos aplica tambien UV
proporcionadas sobre sus caras estrechas.
La finalizacion usa chispas electricas y sonido breve; Nitwit conserva sus
particulas de aldeano. Geometria/cajas de seleccion precalculadas por direccion.

## Inventario y XP

```text
Entrada | Entrada | Entrada
Entrada | Centro  | Entrada  -> Resultado
Entrada | Entrada | Entrada
```

Maquina portatil: conserva inventario, resultados, bloqueo y hasta
`2.147.483.647` XP al romper/recolocar. Infraestructura comun
`MachineExperienceAccount`, fluido/XP 1:1 y transacciones con rollback.
Modo XP independiente: Solo receta activa / Llenar almacenamiento. Cambiar
receta nunca recorta XP almacenada; transferencias manuales ignoran la cuota
automatica. Campo de niveles vacio transfiere todo lo posible.

En modo libre la salida previsualiza receta; retirar consume ingredientes y XP
atomicamente. Sin XP suficiente se muestra atenuada y no puede retirarse.
El indicador inferior muestra almacenada/necesaria, incluida receta fantasma.

Bloquear fija el ID de la receta actual valida, sin exigir XP.
Si desaparece tras recarga del datapack, queda bloqueada e invalida: no acepta
ingredientes ni elige otra receta. Desbloquear conserva inventario/XP/resultados.
En modo bloqueado fabrica automaticamente, como maximo una vez por tick, solo
si ingredientes, XP, resultado y recipientes completos pueden confirmarse.

Tolvas/capabilities insertan por arriba/laterales exclusivamente lo pendiente
para una ejecucion de receta bloqueada; extraen resultados fisicos, no entradas.
Ingredientes repetidos mantienen posiciones y componentes. No se recortan
lotes manuales mayores. Insertar dentro de una transaccion no ejecuta recetas.
No hay control de redstone nuevo. Detalles: [Esencias](ESSENCE_AUTOMATION.md).

## REI

Libro propio retirado por peticion del usuario el 2026-10-11, junto con boton,
categorias y transferencia del libro. La colocacion manual sigue funcionando
sin REI. Los ingredientes fantasma de la receta bloqueada permanecen visibles
al agotarse entradas/resultados.

REI muestra Sintesis de materia, costes y transferencia de ingredientes; no
controla el bloqueo ni transfiere XP. Sin categorias ni avisos en el libro vanilla.
La sincronizacion completa de recetas para REI se conserva.

## Contratos de recetas

Recetas JSON en `data/trading_cells/recipe/`; siguen siendo la fuente de verdad.
Cada receta admite nueve posiciones, restricciones de componentes, cantidades
y XP. `{"empty": true}` exige una posicion vacia. Retirado el campo `category`;
los datapacks antiguos que lo incluyan siguen cargando, pero se ignora.
El byte antiguo de categoria se conserva reservado en la sincronizacion para
no cambiar el formato de red; ya no controla ningun recetario.
La botella de experiencia usa una botella de vidrio en cualquier posicion,
sin otros ingredientes, por 11 XP. Restos de fabricacion se conservan.

Siguen disponibles maquinaria, equipo y libros, incluidos Toque de Granjero,
Toque de Minero, Toque de Guerrero, Toque de Seda II y Decapitacion.
Las granjas retiradas por familia no se restauran. Los Spawn Eggs ya no se
fabrican aqui: `creature_generator` usa nucleo + huevo de
`#trading_cells:arcane_infusion_eggs` + XP del tier (1.000/4.000/16.000/64.000).
Wither/Dragon permanecen en blacklist por defecto. Las 86 recetas de huevos
recuperables estan fuera del JAR en `docs/archive/arcane_infusions/`; las dos de
jefes se eliminaron definitivamente.

La receta del propio bloque se conserva:

```text
Obsidiana llorosa | Mesa de encantamientos       | Obsidiana llorosa
Obsidiana llorosa | Almacen de Experiencia vacio | Obsidiana llorosa
Obsidiana llorosa | Piedra del End               | Obsidiana llorosa
```

Un almacen con datos guardados no sirve. La presentacion futurista no altera
su progresion ni los costes solicitados anteriormente.

## Validacion

2026-10-11: libro/categorias retirados, recursos y `check releaseCheck` correctos
(158 GameTests). GUI OpenGL+REI/Vulkan sin REI. Fabricacion manual, consumo de
75.000 XP y fantasmas/tooltips comprobados en `artifacts/revision-synthesizer-ghost`.
Contrato de catalogo actualizado explicitamente: mismas 82 recetas e ingredientes,
solo retirada de categorias. Los avisos de recetas antiguas de la plantilla
local corresponden a IDs retirados en hitos anteriores, no a recetas nuevas.

2026-10-10: recursos, independencia de backend y 156 GameTests correctos.
Modelo/menu comprobados en OpenGL con REI y Vulkan sin REI, a 1280x720 con
escala automatica. `check releaseCheck` correctos; JAR sin modelos antiguos.
Evidencia: `artifacts/storm-rework-opengl-2`, `artifacts/storm-rework-vulkan`
y `artifacts/storm-rework-check.log`. No se han anadido trabajos de GitHub ni
tests por cada PNG.
