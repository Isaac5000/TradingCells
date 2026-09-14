# Revision de simulacion de entidades

Solicitud completa: 2026-09-11. Este documento conserva el alcance y la evidencia
pendiente del objetivo; no sustituye los requisitos del usuario.

## Punto de reanudacion: 2026-09-14

- R13: integrados en la granja general los extras de combate (cabezas, fragmentos
  de creeper cargado) y los perfiles heredados de equipo de esqueletos, zombis,
  ahogados, piglins y saqueadores, incluyendo conchas y recompensas ominosas.
  Usa la tabla de la instancia una sola vez por baja y evita duplicar un extra
  que ya haya salido de esa tabla. Variantes de arma mutuamente excluyentes;
  cantidades, probabilidades, dificultad y desgaste de equipo conservados.
  El ejecutor nativo compartido por las granjas antiguas no cambia.
- R13/R20: filtros de extras disponibles antes de la primera baja. Los objetos
  descubiertos se conservan al desgastarse/cambiar la espada y al guardar/cargar,
  con limite de 2048; se reinician al cambiar el modulo. Los IDs de la vista
  previa tambien pueden filtrarse. El analizador ya admite cantidades negativas
  de tablas vanilla, necesarias para el carbon del esqueleto Wither, sin cambiar
  las tiradas reales ni inventar probabilidades para tablas no soportadas.
- Ultima prueba dirigida: 113/113 GameTests pasan el 2026-09-14 a las 07:28.
  Incluye tablas propias/enlazadas, cabezas sin duplicados, probabilidad de skull
  Wither con Saqueo/Decapitacion, fragmentos cargados, filtros persistentes,
  equipo gastado, armas alternativas, botellas/estandartes y restauracion del
  contexto tras errores. `check --continue` conserva solo los dos fallos de
  recetas ya documentados; dominio, arquitectura, recursos graficos, portabilidad
  y contenido del JAR pasan. No dar R13 por cerrado globalmente: faltan tablas
  ciclicas/extremas y la matriz con mods externos. El siguiente trabajo funcional
  es retirar las 21 opciones antiguas de creativo/REI/recetas sin quitar sus IDs,
  y terminar las pruebas de interaccion de extractor/mesa de esencias.

- Peticion nueva: mango de pico y espadas con paleta real del palo vanilla;
  lingote/flechas de trueque dorados en todos los niveles; receta inicial de
  quarry con pico de hierro. Implementado en los PNG fuente/finales y en la
  receta, manteniendo la cadena de tiers. Pasan 18 pruebas de recursos y 19 de
  recetas. Vista: `artifacts/upgrades-fixed-emblem-palettes-20260913.png`.
- R16-R17, boceto posterior del usuario: el modulo ya no lleva jaula cerrada,
  sino base baja de dos escalones, sin barras/techo, criatura centrada. La granja
  conserva su marco exterior y trabajador, con otro soporte escalonado debajo
  de la criatura; no se dibuja ningun spawner. Los modelos compuestos conservan
  la base cuando no hay tipo compatible. El render ajusta escala por dimensiones.
  Se verifican creeper, vaca, warden, ghast, bacalao, dragon y tipo inexistente.
  OpenGL: `artifacts/mob-simulation-pedestals-opengl-20260913/`.
  Vulkan con encuadre final de primera persona (base completa, criatura mirando
  al jugador): `artifacts/mob-simulation-pedestals-vulkan-20260913-final/`.
  Ambos clientes completan sus ensayos; capturas en `run-1/result/capture.png`.
  Quedan variantes/orientaciones, mano zurda/tercera persona y textura propia
  del trabajador. No dar R16-R18 por terminados globalmente.
- R25, peticion nueva en el objetivo: tiempos mostrados en segundos enteros,
  redondeados hacia arriba. Implementado en pantalla de granja y formateador
  comun de menus/REI/Jade; no cambia los ticks reales de produccion. Pruebas
  puras cubren 5.7 -> 6, fracciones de segundo, limites y ausencia de overflow.
- Ultima validacion: `check --continue` conserva solo los dos fallos conocidos
  de recetas; pasan dominio, arquitectura, graficos, 18 pruebas de recursos,
  modelos y portabilidad. La UI Vulkan completa filtros/probabilidades,
  encendido, XP y 59 slots; captura muestra `8 criaturas / 6 s`, los diez PNG
  corregidos y modulos sobre sus bases:
  `artifacts/mob-simulation-pedestals-ui-vulkan-20260913/run-1/result/capture.png`.

- Trabajar sin subagentes: el usuario ha prohibido crear o reactivar mas agentes
  por consumo de creditos.
- Correccion posterior del usuario sobre R07: SOLO diamante lleva remaches lilas
  y SOLO netherite rojos. Cobre, hierro y oro no llevan detalles especiales.
  Las dos mejoras de granja deben compartir espada, proporcion y margenes.
  Se han sustituido las dos bases y se ha autorizado pegar el mismo recorte
  de espada en el PNG fuente de capacidad; no volver a generar otra espada ni
  dejar la composicion solo en codigo. Fuente y variantes contienen el recorte.
  Las 16 pruebas de recursos pasan: mismo recorte en los cinco tiers, contorno
  60x60 como quarry y color especial limitado a los dos tiers indicados.
  Comparacion: `artifacts/mob-farm-upgrades-corrected-20260913.png`.
  Verificacion real OpenGL del 2026-09-13: las diez variantes se ven en el
  inventario y los clics de filtros/probabilidades/encendido/XP pasan.
  Evidencia: `artifacts/mob-simulation-ui-opengl-20260913/run-1/result/capture.png`
  y su `client.log`. No se ha repetido aun la matriz Vulkan con estos cambios.
- Conversion implementada en el primer tick de los bloques antiguos, incluso
  apagados o pausados por redstone. Las pruebas cubren las 21 variantes,
  trabajador, espada, 18 salidas, XP, progreso, filtros dinamicos, conversion
  unica, estado cargado del creeper y botin pendiente que se liquida una vez.
  Los 107 GameTests pasan el 2026-09-13 a las 06:19. Todavia faltan la retirada
  de opciones antiguas en creativo/REI/recetas y la auditoria de migracion final.
- No repetir la implementacion de recetas cerradas abajo. Conservar una tarea
  comun de validacion/publicacion: `check` aun falla porque diez expectativas
  del validador conservan recetas antiguas y el contrato espera 140 recetas en
  vez de las 174 actuales. Auditar el catalogo definitivo antes de actualizarlo;
  aun falta retirar las recetas de las granjas sustituidas.
- Las texturas, la conversion de mundos y la integracion completa de la granja
  no estan terminadas. No confundir una base implementada con un cierre visual
  o funcional de todo el requisito.

### Implementacion terminada que no hay que repetir

- R03-R06: receta del capturador piglin con dos blackstone, oro en la mejora
  cobre de trueque, cuatro bloques de diamante en la mejora diamante de quarry,
  y materiales por tier/chorus de diamante en las cinco mejoras de tuberias.
- R08: recetas diferenciadas de llama/caballo y flores de cactus para camello.
- R09: una receta para cada uno de los 88 huevos vanilla de Minecraft 26.2,
  uno o dos materiales tematicos, sin combinaciones duplicadas, XP multiplo de
  diez y costes superiores para criaturas poderosas. Se ha contrastado el JAR
  de la version actual y su registro en fuentes, no una lista de otra version.
- Parte de R24: end stone en el centro inferior de la receta del infusor.
  Su textura sigue pendiente.
- Parte de R01: los dos terminales usan el modelo completo del bloque en el
  inventario y tienen el panel en la cara superior. Falta adaptar los tonos y
  verificar el resultado visual completo.

Evidencia del 2026-09-13: las 18 pruebas de `test_recipe_resources.py` pasan.
Esto no certifica todavia la publicacion ni las partes graficas pendientes.

### Base ya implementada, cierre pendiente

- R11-R12: diez mejoras, recetas y slots separados; velocidad y cantidad por
  ciclo independientes, espada complementaria, desgaste salvo Toque del Guerrero.
  Reglas puras y remaches/paletas verificados; integracion final pendiente.
- R13-R15: instantanea de esencia, modulo reutilizable, tabla real de la entidad,
  clasificacion normal/alto nivel y sintesis con consumos/XP validados en servidor.
  Botin especial heredado integrado; faltan tablas extremas, mods externos y la
  interaccion del extractor.
- R19: XP y capacidad de extraccion, salidas transaccionales y cola persistente
  de botin implementadas. Falta la migracion completa desde granjas antiguas.
- R20: menu con filtros, probabilidades, encendido y retirada de XP implementado.
  Clics reales del menu de granja comprobados en OpenGL y Vulkan. La prueba
  de la mesa se interrumpio al preparar un warden en el mundo de ensayo; su
  preparacion se ha ajustado, pero aun no se ha repetido con exito.

Evidencia anterior al ultimo ajuste: 104/104 GameTests el 2026-09-12 a las
00:51; captura y registro en `artifacts/mob-simulation-ui-opengl-20260912/`.
En el arbol del 2026-09-13 pasan compilacion comun/cliente, reglas puras,
arquitectura, independencia grafica, 14 pruebas de recursos logistics y el
generador de 47 recursos de simulacion. La nueva inspeccion de tablas enlazadas
y el ajuste del ensayo de esencias compilan, pero necesitan pruebas dirigidas.

## Requisitos y evidencia

| ID | Requisito | Estado | Evidencia exigida |
| --- | --- | --- | --- |
| R01 | Terminales como bloques 3D en inventario, pantallas arriba y tonos de logistics | En curso | Modelos, paletas y capturas de inventario/mundo |
| R02 | Cuatro capturadores simetricos y homogeneos, inspirados en originales/recetas | En curso | PNG finales y comparacion visual |
| R03 | Capturador piglin: dos blackstone en lugar de hierro | Receta terminada | Prueba dirigida pasa; validacion final comun pendiente |
| R04 | Mejora cobre de trueque: oro en lugar de redstone | Receta terminada | Prueba dirigida pasa; validacion final comun pendiente |
| R05 | Mejora diamante quarry: bloque diamante en lugar de estrella | Receta terminada | Prueba dirigida pasa; validacion final comun pendiente |
| R06 | Mejoras tuberias: material del tier por redstone; chorus cocinado en esquinas de diamante | Recetas terminadas | Cinco recetas y prueba del generador pasan |
| R07 | Remaches normales en cobre/hierro/oro; lila en diamante y rojo en netherite, sin ruido ni cambios de forma | En curso | 16 pruebas de pixel y captura OpenGL pasan; matriz Vulkan pendiente |
| R08 | Recetas distintas llama/caballo y flores de cactus para camello | Recetas terminadas | Pruebas de ingredientes y duplicados pasan |
| R09 | Huevos vanilla completos, recetas simples coherentes y unicas, XP multiplo de 10 y costes altos para criaturas poderosas | Recetas terminadas | 88 huevos contrastados con el JAR/registro 26.2; validacion final comun pendiente |
| R10 | Una granja general reemplaza las 21 granjas, sin perder contenido de mundos existentes | En curso | Conversion de 21 bloques y colas comprobada; creativo/REI/recetas pendientes |
| R11 | Mejoras de espada por tier: velocidad y cantidad por ciclo como controles separados | En curso | Diez items, recetas y reglas verificados; acabado/integracion pendiente |
| R12 | Arma se desgasta salvo Toque del Guerrero; tier/encantamientos complementan mejoras significativas | En curso | Reglas y desgaste basico verificados; cobertura final pendiente |
| R13 | Botin dinamico real, incluidos objetivos ya adaptados y criaturas externas | En curso | Tabla real/enlazada y especiales heredados comprobados; tablas extremas y mods externos pendientes |
| R14 | Extraccion generica de esencia de criaturas, sin lista de familias predefinida | En curso | Datos implementados; prueba de interaccion pendiente |
| R15 | Mesa de esencias distingue normal/alto nivel y exige requisitos para crear modulo | En curso | Transaccion/XP y clasificacion comprobadas; ensayo UI pendiente |
| R16 | Modulo con nombre/modelo de criatura sobre base escalonada centrada (boceto posterior sustituye la jaula) | En curso | Renderer, fallback y capturas reales pasan; variantes/vistas pendientes |
| R17 | Granja conserva aldeano y presenta criatura sobre pedestal, no spawner | En curso | Renderer y soporte escalonado visibles en OpenGL/Vulkan; auditoria final pendiente |
| R18 | Apariencia local de aldeano cazador de monstruos; ningun oficio ni POI nuevo | Pendiente | Textura, renderer y registro |
| R19 | Mismo almacenamiento/extraccion de XP y automatizacion | En curso | Cola, transacciones y migracion de XP comprobadas; auditoria final pendiente |
| R20 | Filtros de botin con checks, probabilidades, encendido/apagado; menu estilo logistics | En curso | Interaccion real OpenGL/Vulkan comprobada; integracion final pendiente |
| R21 | Pie de tooltip muestra solo Trading Cells, no nombre del tab | En curso | Fuente del tooltip y captura con REI |
| R22 | Fragmento de tormenta mas elaborado y mantiene capa de energia | En curso | PNG/modelo/capa y captura |
| R23 | Infusor: Libro encantado como nombre, Toque de Seda II en detalle, sin duplicado | En curso | Tooltip vanilla y captura |
| R24 | Infusor: end stone centro inferior en receta y textura | En curso | Receta, modelo/textura y captura |
| R25 | Tiempos mostrados sin decimales y redondeados hacia arriba, sin alterar produccion | Terminado | Pruebas puras pasan y captura real muestra 6 s en vez de 5.7 s |

## Diseno de la granja

- Una entrada de modulo de criatura sustituye la seleccion de familias.
- Un extractor obtiene esencia sin eliminar la criatura. La mesa convierte una
  esencia y sus materiales/XP en un modulo reutilizable. Los datos deben estar
  acotados y el servidor debe validar todos los consumos.
- La clasificacion de alto nivel usa datos de la entidad y admite excepciones
  por tags. No inventa un porcentaje exacto para tablas que no puede analizar.
- Dos slots de mejora independientes controlan velocidad y cantidad. Las mejoras
  dominan estos multiplicadores; tier del arma y encantamientos son complementos.
- La conversion de granjas antiguas debe conservar trabajador, espada, salidas,
  XP, estado de encendido, redstone y filtros. La compatibilidad interna no debe
  dejar 21 opciones de granja en recetas, creativo o REI.

## Puertas de salida

- Pruebas dirigidas de cada requisito y generadores reproducibles.
- `check` y `releaseCheck` completos, sin rebajar validadores para ocultar fallos.
- Actualizar huella de recetas solo tras auditar los cambios autorizados.
- Capturas y clics reales de menus nuevos, inventario y bloques en OpenGL/Vulkan.
- JAR final, evidencia y auditoria requisito por requisito antes de dar el objetivo
  por completado. Los estados pendientes o sin verificar mantienen el objetivo abierto.
