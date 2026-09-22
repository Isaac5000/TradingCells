# Revision de simulacion de entidades

Solicitud completa: 2026-09-11. Este documento conserva el alcance y la evidencia
pendiente del objetivo; no sustituye los requisitos del usuario.

## Punto de reanudacion: 2026-09-22

### Objetivo vigente

La ultima peticion del usuario exige aplicar `docs/CLEANUP_CANDIDATES.md` y
eliminar restos cobrizos del recorte del lingote de las mejoras de trueque.
Ambos cambios aplicados; el objetivo visual anterior permanece terminado.

- Capturadores: cuatro texturas nuevas instaladas a 64x64 sin suavizado, con
  autorizacion del usuario. Irrompibles con mango metalico de cinco puntas y
  hueco real, no insignia pegada. Fuentes/prompts en `docs/ITEM_TEXTURES.md`.
- Fragmento de tormenta: cristal verde electrico instalado. La capa conserva
  `RenderTypes.energySwirl` y textura vanilla; malla calculada desde el alfa del
  PNG al hornear recursos. No usa mascara de estrella antigua ni I/O por frame.
  El primer ensayo detecto atlas no inicializado durante bake; corregido leyendo
  el recurso del pack actual antes de crear la malla, no el atlas pendiente.
- Mejoras: 25 PNG compuestos sobre los mismos cinco marcos de quarry, originales
  conservados. Prueba compara cada pixel de marco y emblema; madera, oro, acero
  y cian permanecen fijos. `artifacts/item-pipe-yellow-20260921/common-frame-upgrades.png`.
- Modulo: nuevo ajuste solicitado, rotacion [70,0,0], traslacion [0,2.75,3],
  escala 0.4. Baja desde la muneca y compensa la inclinacion del brazo. Izquierda
  comprobada en `artifacts/module-palm-flat-left-vulkan-20260921/` y derecha en
  `artifacts/module-palm-flat-right-opengl-20260921/`. Las capturas
  `simulation-module-palm-*-20260921` verificaron el
  contacto anterior, pero no aprueban este nuevo criterio de apoyo.
- Tuberias en mano: traslacion [0,0.5,0] en ambas vistas de tercera persona,
  conservando tamano/rotacion; las cinco familias comparten ajuste. Ambas manos
  comprobadas en `artifacts/pipe-hands-right-opengl-20260921/`.
- Mesa: completamente negra, incluidos ambos tableros; bordes azules conservados.
  Receta: siete hormigones negros y mesa de crafteo, sin amatista. Implementado.
  Huella actual de 153 recetas:
  `64761509EF63D71C03AE744166C42A998AF4FD1C618DD768DC990D23538F3BE0`.
- Tuberia de items: canal blanco convertido en amarillo, 17 PNG con sus ocho
  fotogramas. Resto de texturas y metadatos intactos. Prueba invierte solo tres
  colores y comprueba SHA-256 de todos los pixeles originales; 23 pruebas pasan.
  Previsualizacion en `artifacts/item-pipe-yellow-20260921/pipe-materials.png`.
- Limpieza aplicada: cinco Java sin consumidores, registro de overlay antiguo,
  siete directorios vacios, siete PNG obsoletos y cinco pruebas sustituidas.
  Unos 298 MB retirados. Originales, plantilla activa y evidencias finales
  conservados. Auditoria cerrada en `docs/CLEANUP_CANDIDATES.md`.
- Trueque: recorte por filas del lingote y flechas, con su contorno oscuro.
  Ya no copia fragmentos del panel de cobre; conserva sombras doradas y brillos
  blancos en los cinco PNG. Prueba especifica de fondo y continuidad del oro.
  Captura real: `artifacts/piglin-cleanup-client-opengl-20260922/`.

### Evidencia funcional reciente

Cierre vigente: `checkLogisticsResources check releaseCheck` aprobado el
2026-09-22 a las 06:59, 126/126 GameTests, 28 pruebas de recursos, 25 de recetas
y cierre normal del servidor. Incluye limpieza, correccion del oro y contratos
de 153 recetas; 1131 JSON y 181 PNG del juego validados.
Capturas y animacion verificadas en OpenGL/Vulkan; apoyo final del modulo en
ambas manos y agarre de tuberias comprobados. Los seis puntos del objetivo
anterior y la limpieza posterior estan terminados; lo que figura bajo alcance
anterior es historial.

`releaseCheck` completo aprobado el 2026-09-20 a las 22:23: 126 GameTests,
22 pruebas de recursos y 25 de recetas. Incluye botin inmediato, descarte de
resultados imposibles, hitbox propia y guardado sin cargar chunks al cerrar.
Esta ejecucion es anterior al ultimo cambio de mesa/tuberia/transformacion.
Ejecucion del 21 a las 07:07: `check` y 126 GameTests aprobados, proceso terminado;
el handle de salida final se perdio al continuar. No usar como verificacion de
las texturas/capturadores/agarres editados por la noche. 27 pruebas de recursos
pasan tras esos cambios; el cierre completo posterior se registra arriba.
Texturas reales comprobadas en OpenGL y Vulkan. Se descubrio que el inventario
congelaba la capa electrica: el modelo del fragmento ahora marca su estado como
animado mediante `ModelEvent.ModifyBakingResult`, sin afectar otros objetos.
`artifacts/item-textures-animated-vulkan-20260921/`: dos frames cambian 678 pixeles,
todos dentro de la silueta del fragmento; los otros 29 objetos quedan identicos.
OpenGL final: `artifacts/item-textures-animated-opengl-20260921/`, 653 pixeles
cambiantes solo dentro del fragmento. La fixture exige estado animado real.
Menu con casillas y clics reales aprobado en OpenGL/es_es:
`artifacts/simulation-loot-checks-opengl-20260921/`. El intento anterior del 20
fallo porque la camara estaba a mas de ocho bloques y Minecraft cerraba el menu;
usar `--camera 4097 -59 4100 180 26` para esta fixture, no la camara de modelos.

### Alcance anterior: 2026-09-20

La ultima peticion sustituye el acabado gris y la mano visible descritos en la
evidencia historica inferior. No rehacerlos. El objetivo activo ahora exige:

- Capturadores normales/irrompibles simetricos; fragmento de tormenta renovado
  conservando su energia. Pendientes.
- Marcos comunes extraidos de quarry y emblemas centrales pegados en los PNG.
  Remaches lilas de chorus, rojos en netherite. Pendiente aplicar esta nueva
  composicion; las pruebas anteriores de paleta no cierran este requisito.
- Tooltip solo `Trading Cells`: terminado y probado, conservar R21.
- Modulo sin mano dibujada, cercano a camara y apoyado correctamente en tercera
  persona. Transformaciones nuevas; comprobacion visual en curso.
- Botin/probabilidades inmediatos al abrir, cambiar modulo o espada; excluir
  resultados imposibles y no mezclar la cola del objetivo anterior. Implementado:
  el menu refresca entradas antes de enviar el estado, distingue tabla analizada
  de tabla desconocida y no persiste predicciones como botin observado.
  Nuevos GameTests para warden/golem, tabla echo shard, creeper sin cabezas/discos,
  cambio de encantamiento, cola pendiente y tabla no analizable.
- Casillas de encendido y probabilidades como las de botin, texto de encendido
  a su izquierda. Implementado, comprobacion visual en curso.
- Granja y mesa negras, bordes azules animados sin costuras coplanares. Geometria
  exterior unificada y horneada; comparte la animacion existente de tuberias.
  Granja 254 caras, mesa 200, modulo 66; sin renderer/coloracion por fotograma.
  Mesa con superficies superiores de amatista (sustituido el 21) y hitbox fija de tablero/patas.
- Recetas de granja/mesa con hormigon negro segun peticion, otros ingredientes
  y XP conservados. Dos recetas auditadas, catalogo sigue en 153; nueva huella
  `5E26B88EB3F33ABD1203931ADBC8B0867288E40247C199FC5BA98EF605EBAC6D`.
- Infusor con toda la base de piedra del End, incluidos item y cuatro orientaciones.
  Modelos y prueba actualizados; captura pendiente.
- Inventario final de texturas/codigo/directorios prescindibles: SOLO al terminar
  el resto, sin borrarlos. Pendiente; no eliminar `artifacts` indiscriminadamente.

Durante las pruebas se detecto un bloqueo real de cierre: `saveAdditional`
llamaba `isHunting`, que consultaba redstone y solicitaba chunks vecinos durante
descarga. Ahora serializa el estado conocido sin acceder al mundo. Regresion
con granja separada en un chunk no cargado; conservar esta correccion.

### Evidencia anterior, no criterio visual actual

- Segunda rectificacion visual: el azul incluye TODOS los cantos y marcos,
  no solo el reborde superior. `outlined_cube` reparte las seis caras exteriores
  entre centro gris/negro y cuatro bordes azules sin superficies superpuestas.
  Aplicado a patas y tablero de la mesa, marco completo de la granja y ambos
  niveles de los pedestales. Conserva limites, alturas y centrado; usa geometria
  estatica horneada, sin nuevos renderizadores de bloque. Nueva prueba verifica
  cobertura completa de las caras y ausencia de solapamientos. Captura OpenGL:
  `artifacts/simulation-full-blue-frames-opengl-20260920/run-1/result/capture.png`.
  Mismo resultado comprobado en Vulkan:
  `artifacts/simulation-full-blue-frames-vulkan-20260920/run-1/result/capture.png`.
  Ambas muestran mesa completa, marcos y modulo con mano; 21 pruebas de
  recursos logistics aprobadas, incluidas geometria y las paletas anteriores.
  Esta correccion sustituye las capturas anteriores con azul solo arriba.
- R23/R24 retomados antes de la segunda rectificacion: ambos modelos del
  infusor (`arcane_infuser` y `arcane_infuser_frame`) usan `end_stone` en
  `base_center`, preservando amatista lateral y marco de obsidiana llorosa.
  Prueba nueva para receta/modelos/inventario aprobada, 24 pruebas de recetas.
  Capturas del bloque colocado e inventario pendientes.
  La fixture nueva `infusion` observa el tooltip real del resultado:
  `[Libro encantado, Toque de seda II, Minecraft]`. Su paso posterior de Shift
  llega a fase 1 pero no completa la comprobacion del gasto de XP; dos ensayos
  fallidos en `artifacts/infuser-book-opengl-es-20260920/` y
  `artifacts/infuser-book-opengl-es-20260920-final/`. Ambos procesos terminaron.
  No asumir que sea solo sincronizacion: esperar el paquete no lo resolvio.
  Proximo paso: comprobar inventario/XP de servidor y cliente tras Shift con
  diagnostico dirigido; no rebajar la prueba ni dar R23 por cerrado todavia.
- Rectificacion visual del usuario: mesa con el gris de las tuberias; pedestal
  del modulo negro, ambos con rebordes finos azul entre cian y azul primario.
  Aplicado tambien al pedestal interior de la granja, sin recolorear su carcasa.
  Los modelos nativos reutilizan `pipe_base`, hormigon negro y azul claro;
  mantienen alturas, centrado, recetas y datos. No se generan nuevas imagenes
  ni se incorpora un renderer de bloque para colorear superficies estaticas.
  `EntityModuleHandRenderer` dibuja el brazo con la skin y manga del jugador
  solo al sostener modulos, respeta invisibilidad y mano principal/secundaria,
  y deja que Minecraft siga dibujando el item compuesto. Primera persona
  comprobada visualmente en OpenGL/derecha y Vulkan/izquierda:
  `artifacts/simulation-blue-hand-opengl-20260920/` y
  `artifacts/simulation-blue-hand-vulkan-left-20260920/`.
  Mano secundaria comprobada en OpenGL, con el otro brazo vacio visible y sin
  duplicados: `artifacts/simulation-blue-offhand-opengl-20260920/`.
  Captura final despejada, mesa completa y modulo en mano derecha (Vulkan):
  `artifacts/simulation-blue-hand-vulkan-final-20260920/run-1/result/capture.png`.
  `check releaseCheck` aprobado tras estos cambios: 122/122 GameTests,
  20 pruebas de recursos y 23 de recetas; contratos de publicacion intactos.
- R21 terminado: probado en cliente para los 84 items registrados del mod,
  informacion normal/avanzada y tooltips propios de REI. El tooltip de creativo
  conserva el nombre y muestra una sola linea `Trading Cells`, sin el titulo
  del tab. La nueva fixture `tooltips` reproduce un fallo adicional de borrado
  de lore con ese mismo texto; corregido comparando el componente completo del
  pie, no solo su cadena. Se conservan nombres personalizados, lore e imagenes.
  Los items vanilla no reciben nuestro pie ni cambios de nuestro listener.
  Tambien se corrigieron los nombres de los items de granja y mesa de esencias:
  usan la traduccion del bloque mediante `useBlockDescriptionPrefix`, sin cambiar
  IDs, modelos ni recetas. Regresion de servidor `mob_simulation_block_item_names`.
  Capturas reales con REI a 1280x720: OpenGL/es_es en
  `artifacts/tooltips-opengl-20260920-final-names/` y Vulkan/en_us en
  `artifacts/tooltips-vulkan-20260920-final/`. Ambos logs confirman el backend.
  Repetido sin REI en OpenGL/es_es:
  `artifacts/tooltips-opengl-without-rei-20260920-final/`; mismos 84 items y
  tooltip real correcto. `check releaseCheck` paso con 122 GameTests antes
  de los cambios visuales de mesa/pedestales/mano descritos arriba.
- GitHub resuelto y confirmado remotamente: el commit `74c402a` pasa Build en
  [Actions 34956324855](https://github.com/Isaac5000/TradingCells/actions/runs/34956324855).
  No repetir el diagnostico de 140/153 recetas ni tratar la ejecucion vieja en
  rojo como un fallo nuevo. Los cambios posteriores al commit siguen locales.
- R18 en curso, NO terminado: renderer local preparado, modelo vanilla y capa
  de espada, sin registrar oficio/POI ni cambiar datos del capturador. Falta el
  PNG `textures/entity/simulation_worker.png`. Mientras no exista, conserva el
  render normal del aldeano; la presencia se comprueba una vez al crear el
  renderer, tambien tras recargar recursos, no por fotograma. El primer borrador
  de ImageGen desplaza islas UV y no se ha instalado. El ajuste de recortes en
  el PNG sigue pendiente de respuesta; no dar por valido ese borrador.

### Evidencia de terminales y esencias: 2026-09-15

- R01 terminado: carcasas y marcos de ambos terminales recoloreados a acero gris
  de logistics, sin redibujar pantallas, mover pixeles ni alterar el alfa. Los
  tres PNG fuente y cinco PNG del mod contienen el acabado; originales de cobre
  conservados en `tools/assets/upgrade_bases/originals/terminals/`.
  Modelos completos en inventario y paneles exclusivamente arriba verificados.
  Capturas reales: `artifacts/terminal-steel-vulkan-20260915-clean/`,
  `artifacts/terminal-steel-opengl-20260915/` y
  `artifacts/terminal-steel-inventory-opengl-20260915-final/`. Las dos primeras
  muestran los terminales colocados junto a tuberias; la tercera, ambos items 3D.
  `check` y `releaseCheck` pasan con 121 GameTests, 19 pruebas de recursos
  logistics y 23 pruebas de recetas. La nueva regresion compara pixel por pixel
  el recoloreado, la conservacion de pantallas y su idempotencia.
  No repetir este apartado; siguen pendientes R02 y los acabados R18/R22/R24,
  ademas de la compatibilidad y auditorias indicadas abajo.

- R14: cuatro pruebas nuevas pasan por el evento real de interaccion de NeoForge:
  extraccion de vaca/aldeano/zombi/warden sin eliminar ni herir la criatura,
  frasco y desgaste exactos, manos principal/secundaria, cooldown de 40 ticks,
  rechazo sin frasco/de entidades muertas/jugadores, creativo, ultimo uso y
  desbordamiento del inventario. Corregido el cooldown al romperse el extractor:
  ahora se registra antes del desgaste y no se asigna al stack vacio.
- R15: mesa comprobada con clics reales en Vulkan/es_es y OpenGL/en_us a 1280x720:
  sintesis normal, retirada Shift, clasificacion de warden y boton deshabilitado
  cuando faltan requisitos. Los costes ahora muestran iconos concretos, cantidades
  y XP, con tooltip de cada material, incluso si hay hierro en vez de netherita.
  Una prueba adicional verifica sintesis de alto nivel, rechazos por distancia,
  material, fragmentos y 2999 XP; consumo exacto de 16 fragmentos, 1 netherita y
  3000 XP; modulo de warden restaurable y ausencia de duplicacion.
- Validacion actual: 121/121 GameTests, `check` y `releaseCheck` aprobados.
  Capturas y registros: `artifacts/essence-workbench-vulkan-20260914-final/` y
  `artifacts/essence-workbench-opengl-20260914-final/`. Esta evidencia no cierra
  las texturas pendientes ni la compatibilidad con entidades de mods externos.
- R23: retirado el nombre personalizado del libro de Toque de Seda II en
  `ArcaneInfusionResult`. Tanto la vista previa como el resultado fabricado
  conservan exactamente los componentes vanilla y el encantamiento a nivel 2.
  Nueva regresion `arcane_infuser_enchanted_book_name`; 116/116 GameTests pasan.
  No modifica libros existentes ni nombres elegidos por jugadores. Captura
  del tooltip en el cliente pendiente.
- R10: retiradas las 21 recetas antiguas y sus categorias/displays de REI.
  Los IDs registrados y la migracion se conservan. Creativo tiene 13 entradas
  de la granja general; dos GameTests comprueban catalogo y recetas retiradas.
  Ensayo cliente Vulkan con REI aprobado: exactamente 13 entradas compartidas
  con creativo y ninguna granja antigua. Filtros, probabilidades, encendido y XP
  comprobados de nuevo. Evidencia: `artifacts/mob-simulation-catalog-vulkan-20260914-final/`.
  El ensayo reinicia el bloque de la copia e inicializa las pestanas creativas;
  no depende de que el mundo guardado dejase la maquina encendida ni del cache
  de una pantalla creativa que no se ha abierto.
- GitHub: el commit se subia, pero `checkReleaseContracts` fallaba en los tres
  sistemas por esperar 140 recetas. Catalogo revisado: 153 recetas, huella
  `3A39AEF0CD2A1CEA15C8C18553B8F7375948917C2338C456EE55BC5113839270`.
  Referencia actualizada sin desactivar controles. Fuentes de Minecraft
  habilitadas tambien en CI para las 23 pruebas de recetas. Antes de R23 pasan
  `clean build releaseCheck` normal y con `CI=true`, 115/115 GameTests.
  La ejecucion remota ya esta aprobada; enlace en el punto de reanudacion.

### Evidencia anterior de botin

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
  contexto tras errores. Los dos fallos de recetas de esa ejecucion ya estan
  corregidos (ver punto de reanudacion). No dar R13 por cerrado globalmente:
  faltan tablas ciclicas/extremas y la matriz con mods externos. Sigue pendiente
  ampliar la compatibilidad de entidades externas y completar el acabado visual.

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
  Los 107 GameTests pasan el 2026-09-13 a las 06:19. Todavia falta
  la auditoria de migracion final; la retirada de creativo/REI/recetas ya esta
  comprobada en el punto de reanudacion.
- No repetir la implementacion de recetas cerradas abajo. El validador y el
  contrato de 153 recetas ya estan actualizados; `releaseCheck` pasa. Conservar
  la validacion final y la confirmacion de CI del commit definitivo.
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
- R01: terminales completos en inventario, panel superior y tonos de logistics
  terminados; evidencia final del 2026-09-15 en el punto de reanudacion.

Evidencia del 2026-09-13: las 18 pruebas de `test_recipe_resources.py` pasan.
Esto no certifica todavia la publicacion ni las partes graficas pendientes.

### Base ya implementada, cierre pendiente

- R11-R12: diez mejoras, recetas y slots separados; velocidad y cantidad por
  ciclo independientes, espada complementaria, desgaste salvo Toque del Guerrero.
  Reglas puras y remaches/paletas verificados; integracion final pendiente.
- R13-R15: instantanea de esencia, modulo reutilizable, tabla real de la entidad,
  clasificacion normal/alto nivel y sintesis con consumos/XP validados en servidor.
  Botin especial heredado e interaccion del extractor verificados; faltan tablas
  extremas y mods externos.
- R19: XP y capacidad de extraccion, salidas transaccionales y cola persistente
  de botin implementadas. Falta la migracion completa desde granjas antiguas.
- R20: menu con filtros, probabilidades, encendido y retirada de XP implementado.
  Clics reales del menu de granja y de la mesa comprobados en OpenGL y Vulkan.
  El fallo anterior al preparar un warden en pacifico ya esta resuelto.

Evidencia anterior al ultimo ajuste: 104/104 GameTests el 2026-09-12 a las
00:51; captura y registro en `artifacts/mob-simulation-ui-opengl-20260912/`.
En el arbol del 2026-09-13 pasan compilacion comun/cliente, reglas puras,
arquitectura, independencia grafica, 14 pruebas de recursos logistics y el
generador de 47 recursos de simulacion. La nueva inspeccion de tablas enlazadas
y el ajuste del ensayo de esencias compilan, pero necesitan pruebas dirigidas.

## Requisitos y evidencia

| ID | Requisito | Estado | Evidencia exigida |
| --- | --- | --- | --- |
| R01 | Terminales como bloques 3D en inventario, pantallas arriba y tonos de logistics | Terminado | PNG y regresion exacta; mundo OpenGL/Vulkan e inventario OpenGL verificados el 2026-09-15 |
| R02 | Cuatro capturadores simetricos y homogeneos, inspirados en originales/recetas | En curso | PNG finales y comparacion visual |
| R03 | Capturador piglin: dos blackstone en lugar de hierro | Receta terminada | Prueba dirigida pasa; validacion final comun pendiente |
| R04 | Mejora cobre de trueque: oro en lugar de redstone | Receta terminada | Prueba dirigida pasa; validacion final comun pendiente |
| R05 | Mejora diamante quarry: bloque diamante en lugar de estrella | Receta terminada | Prueba dirigida pasa; validacion final comun pendiente |
| R06 | Mejoras tuberias: material del tier por redstone; chorus cocinado en esquinas de diamante | Recetas terminadas | Cinco recetas y prueba del generador pasan |
| R07 | Remaches normales en cobre/hierro/oro; lila en diamante y rojo en netherite, sin ruido ni cambios de forma | En curso | 16 pruebas de pixel y captura OpenGL pasan; matriz Vulkan pendiente |
| R08 | Recetas distintas llama/caballo y flores de cactus para camello | Recetas terminadas | Pruebas de ingredientes y duplicados pasan |
| R09 | Huevos vanilla completos, recetas simples coherentes y unicas, XP multiplo de 10 y costes altos para criaturas poderosas | Recetas terminadas | 88 huevos contrastados con el JAR/registro 26.2; validacion final comun pendiente |
| R10 | Una granja general reemplaza las 21 granjas, sin perder contenido de mundos existentes | En curso | Conversion, creativo y recetas comprobados; REI real aprobado en Vulkan; auditoria de mundos pendientes |
| R11 | Mejoras de espada por tier: velocidad y cantidad por ciclo como controles separados | En curso | Diez items, recetas y reglas verificados; acabado/integracion pendiente |
| R12 | Arma se desgasta salvo Toque del Guerrero; tier/encantamientos complementan mejoras significativas | En curso | Reglas y desgaste basico verificados; cobertura final pendiente |
| R13 | Botin dinamico real, incluidos objetivos ya adaptados y criaturas externas | En curso | Tabla real/enlazada y especiales heredados comprobados; tablas extremas y mods externos pendientes |
| R14 | Extraccion generica de esencia de criaturas, sin lista de familias predefinida | En curso | Eventos reales, consumos, cooldown, rechazos e inventario lleno comprobados; entidades externas pendientes |
| R15 | Mesa de esencias distingue normal/alto nivel y exige requisitos para crear modulo | Funcionalidad verificada | Costes normales/altos y atomicidad comprobados; clics y capturas Vulkan/es_es y OpenGL/en_us |
| R16 | Modulo con nombre/modelo de criatura sobre base escalonada centrada (boceto posterior sustituye la jaula) | En curso | Renderer, fallback y capturas reales pasan; variantes/vistas pendientes |
| R17 | Granja conserva aldeano y presenta criatura sobre pedestal, no spawner | En curso | Renderer y soporte escalonado visibles en OpenGL/Vulkan; auditoria final pendiente |
| R18 | Apariencia local de aldeano cazador de monstruos; ningun oficio ni POI nuevo | En curso | Renderer local preparado; PNG y captura final pendientes |
| R19 | Mismo almacenamiento/extraccion de XP y automatizacion | En curso | Cola, transacciones y migracion de XP comprobadas; auditoria final pendiente |
| R20 | Filtros de botin con checks, probabilidades, encendido/apagado; menu estilo logistics | En curso | Interaccion real OpenGL/Vulkan comprobada; integracion final pendiente |
| R21 | Pie de tooltip muestra solo Trading Cells, no nombre del tab | Terminado | Fixture tooltips: 84 items, normal/avanzado, REI, lore y capturas OpenGL/es_es y Vulkan/en_us |
| R22 | Fragmento de tormenta mas elaborado y mantiene capa de energia | En curso | PNG/modelo/capa y captura |
| R23 | Infusor: Libro encantado como nombre, Toque de Seda II en detalle, sin duplicado | Codigo terminado | Vista previa y resultado iguales a libro vanilla verificados; captura pendiente |
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
