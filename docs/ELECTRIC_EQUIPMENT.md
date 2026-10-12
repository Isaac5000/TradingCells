# Equipo electrico

## Contrato actual, 2026-10-12

- Fulgurita retirada por peticion expresa: bloque, objeto, generacion natural,
  descarga, loot y recursos. Un mundo que la contenga pierde ese contenido al
  actualizar; no se registra un alias que mantenga la mecanica retirada.
- IDs conservados: `storm_shard`, `voltaic_capacitor`, `storm_station`,
  `electric_baton`, `electric_arc_welder`. Solo existe un condensador, de 256
  cargas. `advanced_voltaic_capacitor` queda como alias de lectura, sin objeto,
  receta ni entrada creativa separados; conserva componentes y carga guardados.
- No hay descarga pasiva, FE ni XP.
  `TradingCellsStormCharge` conserva carga previa, nombre y otros componentes.
- Receta: redstone_block arriba, diamond/copper_block/diamond en medio y
  copper_ingot abajo. Apariencia azul de la antigua version de 256 cargas.
  Retirados la receta avanzada y el serializador `capacitor_upgrade`.

## Estacion

Dos entradas visibles: fragmentos de tormenta (0) y bateria (1). Cada 25 ticks
consume un fragmento y aporta 16 cargas. Si solo faltan 1-15, gasta igualmente
el fragmento completo. Sin bateria o estando llena, no consume combustible.
La bateria permanece en la misma ranura durante y despues de cargar.

Los indices 2/3 antiguos solo permiten recuperar contenido guardado; se ocultan
cuando estan vacios y no bloquean la carga. Una bateria antigua en 2 pasa a 1
al cargar datos si esa ranura esta libre. El antiguo `StormBuffer`, ligado al
combustible retirado, deja de utilizarse. Los slots y `ChargeProgress` persisten
al guardar/romper/recolocar. No se truncan componentes de baterias.

Tolvas y capabilities insertan fragmentos/baterias por arriba o laterales.
Extraen bateria llena (desde su misma ranura) o contenido de recuperacion por
cualquier cara. Una bateria parcial solo se retira manualmente. Las inserciones
canceladas no dejan cambios. Sin redstone, FE ni consumidores externos nuevos.

## Armas

- Ambas cargan una bateria fisica durante 30 ticks tras pulsar R una vez.
  Tecla configurable en Controles, categoria Trading Cells: equipo. Soltar
  la tecla no cancela. Mantenerla 12 ticks (0,6 s) cancela y expulsa la bateria,
  tenga o no carga. La pulsacion inicial expulsa una bateria agotada aunque
  no haya recambio; esa pulsacion solo expulsa. Una nueva pulsacion puede iniciar
  inmediatamente otra recarga, sin enfriamiento. Mantener R nunca la reinicia.
  Vuelve al inventario o cae delante del jugador si no cabe.
  Seleccion de municion de
  Minecraft: manos primero, despues inventario; solo condensadores con carga.
  `minecraft:charged_projectiles` guarda la bateria completa. Hasta terminar
  no se retira nada. Cancelar, perder la fuente o cambiar de objeto no consume.
  Soltar el arma o desconectarse cancela la operacion pendiente.
  Al sustituir, la anterior vuelve al inventario o cae una vez si no cabe.
  Una bateria descargada permanece dentro del arma.
- Descarga visual: 30 ticks con el mismo recorrido de carga reproducido al
  reves, incluidos mano opuesta y condensador, en ambas perspectivas y brazos.
  La bateria real se devuelve una sola vez al expulsar; terminar o cancelar la
  animacion no transfiere objetos. Otra pulsacion de R inicia la carga sin espera.
  `TradingCellsStormUnloading` es estado visual transitorio y comparte inicio y
  propietario de la recarga. Dejar de sostener el arma, soltar o desconectarse lo
  limpia con las reglas existentes de recarga.
- Recetas de armas: porra usa lingote de cobre en vez de pararrayos; soldador
  usa dos lingotes de cobre y fragmento de tormenta central en vez del bloque
  de redstone. Patrones, IDs y receta de estacion sin cambios.
- Soldador cargado: clic derecho dispara. Porra: clic izquierdo golpea como
  espada. Recarga de ambos exclusivamente con su tecla; no necesita mantener
  clic derecho. Disparar no gasta baterias de reserva.
- Porra: dano base 1 sin carga y 16 con carga (+15 electricos), antes de
  modificadores vanilla como criticos, armadura o encantamientos. Consume una
  carga por golpe acertado; conserva velocidad y desgaste de espada de hierro.
- Soldador: alcance 8 bloques, choque con bloques y primera entidad viva en
  trayectoria. Pulsar consume inmediatamente una carga, aunque se suelte antes
  del primer tick. Cada 20 ticks consume otra; agotarse interrumpe el uso.
  Cada carga prepaga su segundo; no se reintegra una fraccion no utilizada.
- Cuatro pulsos de 2 puntos de dano por segundo, sin cooldown de heridas
  vanilla, respetando armadura y eventos. Efecto de rayo una vez por segundo;
  solo los objetivos supervivientes reciben la conversion NeoForge despues del
  dano. La criatura resultante conserva como maximo la salud posterior al golpe.
  Un golpe letal no transforma ni genera un segundo botin.
- Haz continuo blanco-cian con ramificaciones, generado desde estado de uso
  sincronizado. Su origen comparte la transformacion real del modelo, incluida
  mano, postura, perspectiva y movimiento de camara. No hay un offset mundial
  estimado. Geometria Blaze3D neutral, sin particulas enviadas por cada cuadro.
- Soldador sujeto por la empunadura inferior con una mano, por autorizacion
  posterior del usuario. La otra conserva su postura normal salvo al recargar.
  Arma vertical y mano opuesta siguiendo la bateria lateral durante la recarga;
  admite zurdos. Los hombros no se trasladan ni se estiran los brazos.
  El giro de la mano cargada acerca el alojamiento al alcance de la otra mano.
  En primera persona el condensador descansa sobre la palma, sin quedar oculto
  dentro de su volumen. Se respeta la anchura de los modelos de piel slim.
- Sonidos propios: bucle de arco mientras se dispara y descarga al golpear con
  la porra cargada. Ambos usan `electricity.ogg`, grabacion aportada por el
  usuario, convertida de estereo a mono para audio posicional. El arco conserva
  2 s; la porra usa un recorte de 0,3 s (seis ticks de golpe), con desvanecimiento
  final de 40 ms. Fuente intacta en `tools/assets/storm/electricity.ogg`.
  Un bucle por jugador,
  sin paquetes por fotograma. Se detiene al soltar, agotarse o salir del mundo.

Los efectos usan el evento NeoForge y `Entity.thunderHit` con un LightningBolt
de contexto que nunca se incorpora al mundo ni recibe ticks. Hay conversiones,
carga de creeper y efectos sobre la entidad; no hay rayo del cielo, destello
meteorologico ni fuego de area. Para este rayo artificial, el cuerpo base de
`Entity.thunderHit` no anade fuego ni sus cinco puntos de dano: solo cuenta el
dano propio del arma. Esto evita cocinar al pollo. Rayos naturales, fuego previo
y efectos de encantamientos conservan su comportamiento. Mods dependientes de un rayo real en el mundo
quedan fuera de este contrato. Explosion de creeper cargado deja un fragmento;
el loot de muerte y Botin no cambian.

## Recursos y validacion

Revision posterior 2026-10-11: dano base 1/total cargado 16, pulsaciones separadas
para expulsar/recargar y audio del usuario integrados. `check releaseCheck`
correctos, 158 GameTests. Evidencia: `artifacts/revision-machines-final-check.log`;
OpenGL+REI/derecha y Vulkan sin REI/izquierda en `artifacts/revision-machines-*`.
Vorbis validado: 48 kHz, mono, 2 s; fuente original conservada sin cambios.
No se ha evaluado manualmente el volumen o timbre escuchandolo en el juego.

Bloques: PNG 16x16 en `textures/block/storm/`. Armas 3D con atlas propio 128x64
en `textures/item/storm/industrial_materials.png`, generado desde la fuente
`tools/assets/storm/industrial_materials.png`. Metal envejecido, esmalte rojizo,
laton y goma inspirados en las referencias del usuario; soldador alargado con
celda azul, horquilla y asa, porra con jaula de electrodos. Perfiles octogonales
y alojamiento de bateria izquierdo. Prismas cerrados y tapas trianguladas en
OBJ, mediante el cargador estandar de NeoForge; eliminan las esquinas abiertas
de las tapas cuadradas anteriores. Once estados de insercion comparten atlas;
no crean texturas por fotograma. Los condensadores conservan `materials.png`.
Generadores: `tools/generate_storm_resources.py`, `tools/storm_mesh.py` y
`tools/generate_storm_sounds.py` (ffmpeg convierte la grabacion original a mono).
Los soportes de las asas quedan 0,2 unidades hacia dentro de las tapas para
evitar caras coplanares. Icono del soldador desplazado 3 px a la derecha en GUI;
no se han retocado los PNG para estos ajustes.

Arte creado con imagegen integrado; reduccion autorizada. Prompts: cuadricula
metalica cian 3x3 vacia, frontal ortografico; atlas industrial 4x2 de acero
oscuro/plata/esmalte rojo/celda azul/goma/laton/ambar/ceramica, sin logotipos.
Fuentes: `tools/assets/matter_synthesizer/grid.png` y atlas anterior;
exportaciones 16x16 y 128x64 respectivamente.

Revision validada: NeoForge estable 26.2.0.90, Minecraft sin cambios.
Fuente: https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml
Comprobada el 2026-10-10. Sin cambios en workflows de GitHub.

### Correccion posterior del agarre, 2026-10-11

Retirado el apoyo permanente de la segunda mano: la solucion anterior desplazaba
el hombro para compensar la distancia y atravesaba el arma al girar la vista.
La recarga ahora resuelve el giro de la mano principal manteniendo ambos hombros
en su origen. Sin cambios en carga, consumo, IDs, recetas, sonido ni disparo.

`checkGraphicsBackendIndependence checkProjectResources check releaseCheck`
correctos; 158 GameTests. Registro: `artifacts/storm-single-grip-check.log`.
OpenGL+REI/derecha y Vulkan sin REI/izquierda: `artifacts/storm-single-grip-opengl`
y `artifacts/storm-single-grip-vulkan`. Capturas de inventario, asas, recarga y
vistas a +/-65 grados de inclinacion y +/-55 grados laterales. Hombros fijos
comprobados en los registros; la fixture local conserva esa comprobacion.
La matriz no certifica todos los skins, poses especiales ni mods de animacion.

### Cierre inicial de la revision del 2026-10-11

158 GameTests, `check releaseCheck` y comprobacion de recursos correctos:
`artifacts/storm-reload-verified.log`. Incluye recarga sin consumo anticipado,
cancelacion, prioridad de mano secundaria, sustitucion, persistencia, alias,
receta con cobre, expulsion parcial y agotada, e inventario lleno sin duplicados.
Catalogo actualizado explicitamente a 82 recetas; sin nuevas tareas de GitHub.
El validador existente ahora reconoce referencias del cargador OBJ estandar.

OpenGL con REI/mano derecha/espanol y Vulkan sin REI/mano izquierda/ingles,
1280x720, escala automatica: modelos, recarga vertical, manos y haz revisados.
La fixture usa la tecla registrada (no solo invoca al servidor), comprueba
su presencia en Controles y prueba mantener para expulsar seguido de pulsacion
rapida para recargar. Evidencias: `artifacts/storm-reload-verified-opengl` y
`artifacts/storm-reload-verified-vulkan`. JAR con 29 mallas compartiendo atlas,
dos OGG y cero recursos activos de `advanced_voltaic_capacitor`.
Sonidos empaquetados y registrados; no se ha realizado una evaluacion auditiva
manual del timbre o volumen. La matriz grafica no certifica todos los drivers.

### Cierre anterior, 2026-10-10

158 GameTests y `check releaseCheck` correctos en
`artifacts/storm-neoforge90-final.log`. Cobertura nueva: prioridad de bateria
cargada, cancelacion/reemplazo/persistencia, mejora conservando componentes,
cuatro impactos/s y criatura convertida, aliento de dragon sin tocar el modelo.
La prueba de punteria fija tambien la rotacion de cabeza del jugador simulado.

OpenGL con REI/mano derecha/espanol y Vulkan sin REI/mano izquierda/ingles,
1280x720, escala automatica: inventario, agarre y recarga en ambas perspectivas,
haz conectado a la horquilla, cuadrantes de ambas mesas y menus correctos.
Evidencias: `artifacts/storm-industrial-final-opengl` y
`artifacts/storm-industrial-vulkan`. Backend/version confirmados en registros.
La plantilla visual antigua avisa de recetas retiradas del recetario guardado;
no son errores nuevos de recursos ni de carga del mod.

### Hito anterior

Antes de recarga: capacidad/clamp, carga anterior, 16 cargas por fragmento,
exceso perdido, bateria llena, persistencia de progreso, recuperacion antigua,
rollback, consumo desde inventario, bonus de porra, pulsaciones cortas,
cadencia, agotamiento y ausencia de rayos reales. 156 GameTests correctos.
`check releaseCheck` correctos; registro `artifacts/storm-rework-check.log`.
OpenGL con REI/mano derecha y Vulkan sin REI/mano izquierda comprobados a
1280x720, escala automatica: arma quieta/activa, primera/tercera persona,
materiales, haz sostenido en dos fotogramas y menus. Evidencia local:
`artifacts/storm-rework-opengl-2` y `artifacts/storm-rework-vulkan`.
Backends reales confirmados en registros. El JAR no contiene fulgurita ni
modelos visuales retirados. Sin tareas nuevas de GitHub ni pruebas por PNG.
Esta matriz no certifica todos los drivers o combinaciones de mods.
