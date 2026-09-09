# Logistica universal

Estado: desarrollo no publicado. Este documento distingue implementacion de
aceptacion pendiente; no sustituye la matriz final de publicacion.

## Bloques y uso

- `item_pipe`, `fluid_pipe`, `gas_pipe`, `energy_pipe`, `universal_pipe`.
- `network_terminal` y `network_crafting_terminal`: acceso pasivo a capacidades.
- `pipe_wrench`: clic derecho abre una cara; Mayus + clic derecho alterna su modo
  o desconecta fisicamente el brazo que apunta a otra tuberia. Solo cuenta la
  mano usada para la interaccion.
- `pipe_target_selector`: clic derecho registra dimension y coordenadas de un
  bloque con inventario. Su tooltip muestra x/y/z en cian. El editor copia el
  destino desde un slot fantasma sin consumir el objeto.
- `INSERT` es el valor inicial junto a una capacidad compatible. `EXTRACT`
  muestra un marco; `NONE` oculta la conexion a la maquina.
- La prioridad acepta todo `int`: gana el mayor. Dentro de la misma prioridad,
  cada recurso elige cercano, lejano, equitativo o aleatorio. Por defecto los
  items usan cercano; liquidos, gases y energia usan equitativo. La distancia
  cuenta tuberias; los empates exactos usan turnos circulares deterministas.
- Cada recurso tiene activacion y hasta 16 reglas ordenadas. Modos de filtro:
  desactivado, whitelist o blacklist. El nivel instalado habilita las funciones
  de la tabla siguiente; las opciones superiores quedan inactivas, sin borrar
  los datos guardados de un perfil antiguo.
- El editor guarda al cerrar. Copiar/pegar incluye prioridad, canales, filtros
  y activacion por recurso; ignora campos invalidos individualmente. No copia
  mejoras fisicas ni cambia el modo de la conexion.
- Los campos de texto consumen las teclas de inventario, barra rapida y soltar
  mientras tienen el foco. Escape conserva el cierre y Tab la navegacion.
  El slot de mejora admite la secuencia completa de arrastre vanilla; solo una
  mutacion real espera confirmacion, nunca un clic vacio o sobre un slot lleno.
- Un slot real de mejora por cara. Las cinco mejoras son fabricables:
  cobre, hierro, oro, diamante y netherita. Conservan los IDs internos anteriores;
  `infinite_pipe_upgrade` ya no es exclusivo de creativo. El caudal depende solo
  de la mejora instalada, no de su cantidad.
- Las mejoras se retiran con el perfil activo y se sueltan por separado al romper.
  La cara recupera su perfil basico al quitar la ultima; colocar una tuberia
  nueva no copia sus ajustes anteriores.
- La migracion de los cuatro slots conserva la mejora de mayor nivel instalada.
  Las otras se guardan como devoluciones pendientes con su perfil: vuelven al
  jugador al abrir esa cara o se sueltan al romper. Recargar no las duplica.

| Mejora | Funciones acumulativas | Items/envio e intervalo | Liquidos/gases por tick | FE por tick |
| --- | --- | --- | --- | --- |
| Sin mejora | Transporte basico, activacion y prioridad | 4 / 20 ticks | 50 | 256 |
| Cobre (`basic`) | Seleccion del reparto | 8 / 15 ticks | 100 | 1024 |
| Hierro (`improved`) | Whitelist/blacklist por ID o tag | 16 / 10 ticks | 500 | 8192 |
| Oro (`advanced`) | Canal general por recurso | 32 / 5 ticks | 2000 | 32768 |
| Diamante (`ultimate`) | Componentes, inversion, subcanales y destino por regla | 64 / tick | 10000 | 131072 |
| Netherita (`infinite`) | Caudal ampliado | 512 / tick | 100000 | 1048576 |

Los perfiles completos viajan en la mejora. Un intercambio directo entre dos
mejoras del mismo nivel restaura el perfil entrante y conserva el saliente. Los
mensajes tardios de una mejora retirada no sobreescriben el perfil basico.

## Contratos

- Nucleo y brazos de 6 pixeles; marco de extraccion de 8 pixeles, grosor y fondo 1,
  sin hueco entre marco y brazo.
  Colisiones exactas compartidas por geometria, modelos horneados, sin BER.
- Bloques waterloggeables e inmoviles por piston. Sin ticker por tuberia.
- Solo hay transporte por bloques conectados y cargados, nunca por rango,
  dimensiones, tickets de chunk o union directa a tuberias de otros mods.
- Planificacion por cara EXTRACT. Presupuestos compartidos para topologia,
  transferencias y lectura de terminales; recorridos pendientes continuan.
- Prioridad y filtros se envian al cerrar el menu. El servidor valida esquema,
  enums, cantidades, herramienta, alcance, permisos y revision de configuracion.
- Transferencias simuladas y confirmadas con transacciones NeoForge. Un proveedor
  externo debe respetar el rollback de esa API; no se promete reparar proveedores
  que muten recursos fuera de la transaccion.
- Terminales: buscador y red a la derecha, fabricacion a la izquierda; inventario
  normal y cursor. Scroll continuo en pixeles, sin botones de pagina; internamente
  se sincronizan ventanas incrementales de diez filas solo con el menu abierto,
  incluyendo margen para las dos filas parcialmente visibles al desplazar.
  El acceso manual usa conexiones INSERT y EXTRACT; dichos modos distinguen
  la automatizacion. Cubos/recipientes se colocan en un slot real y vuelven al
  jugador al cerrar. Clic en el recurso retira; el boton junto al recipiente deposita.
  La cuadricula virtual no almacena objetos. Clic en el resultado fabrica al
  cursor; Shift fabrica mientras caben los resultados y quedan ingredientes.
  Cada receta con sus restos es atomica; las recetas ya terminadas se conservan.
  Si las rutas estan reconstruyendose, conserva una accion de la interfaz hasta
  que esten listas (maximo 200 ticks, solo con el menu abierto). Cambiar el cursor,
  recipiente, recurso o receta afectada cancela la accion; cerrar tambien la cancela.
  Fabricacion y transferencias comparten orden por prioridad, distancia y turno
  circular. Solo una transferencia efectiva avanza el turno; si se revierte un
  lote, tambien se restauran los turnos de ingredientes y recipientes restantes.
  Las retiradas y los depositos mantienen turnos independientes.

## Gases y extensiones

`not_gases` prevalece sobre `gases`; sin tags decide `FluidType.isLighterThanAir`.
Los datapacks pueden corregir clasificaciones. Universal mantiene las rutas
liquida y gaseosa separadas, sin convertir recursos.

`LogisticsResourceAdapter` API 1 permite resolver capacidades, enumerar recursos,
simular y ejecutar extraccion/insercion transaccionales y aportar presentacion.
El registro se realiza durante la inicializacion. No se cargan clases de mods
opcionales. Mekanism requiere un adaptador oficial compatible aun pendiente;
PneumaticCraft queda excluido por tratar presion, no gas almacenado.

La experiencia conserva su capacidad estandar de fluido: 1 XP = 1 unidad. El
Almacen permite insertar y extraer desde las seis caras, tambien sin cara; los
tests verifican ambas direcciones con tuberias de fluidos y universales. No
existe tuberia exclusiva de XP. Se mantienen Jade y redstone de
maquinas; las tuberias nuevas no dependen de redstone.

Las capacidades de items de las maquinas automaticas mantienen la insercion
nativa por cara y permiten extraer sus salidas desde cualquier cara. Sin cara
solo exponen las salidas, sin insercion. `PortableMachineItemHandler` comparte
las transacciones del contenedor nativo; las reglas de tolvas no cambian.
No se exponen slots de comercio manual ni capacidades
ficticias de energia. El deposito masivo del terminal excluye armadura y mano
secundaria; los recipientes se operan desde el slot dedicado del terminal.

## Texturas editables

`src/main/resources/assets/trading_cells/textures/block/logistics/` tiene una
carpeta por tipo: `item_pipe/`, `fluid_pipe/`, `gas_pipe/`, `energy_pipe/` y
`universal_pipe/`. Cada una contiene el tramo y las 16 combinaciones de uniones,
con fotogramas de 32x32 apilados en PNG y sus `.mcmeta`. La base gris comun
`pipe_base.png` y los terminales permanecen en la carpeta raiz. Los terminales
conservan sus atlas 2x2 de celdas de 16x16. Modelos por tipo en
`models/block/logistics/<tipo>/`; blockstates e items mantienen sus IDs.

Las animaciones comparten ocho fotogramas de cuatro ticks, sin interpolacion ni
luz emitida. El mapeado usa coordenadas positivas de cada eje: brazos y nucleos
muestrean la misma escala y fase, incluidos el ultimo fotograma y el primero.
Solo hay aclarado en bordes externos. Se omiten tapas entre tuberias y caras del
nucleo/collar ocultas por conexiones. Las conexiones INSERT/EXTRACT a maquinas
conservan su tapa, con culling vanilla: visible junto a cristal o formas parciales
y descartable junto a una cara opaca completa. No hay BER, ticker ni animacion por instancia:
los sprites se comparten en el atlas. El badge de mejoras usa el primer fotograma.

`pipe_cap.png` es la tapa comun de 32x32, negra y mas oscura hacia el centro,
totalmente opaca y sin `.mcmeta`. `generate_pipe_textures.py --write-cap` permite
regenerar solo esa textura sin sobrescribir los cinco disenos editables.

`generate_logistics_resources.py` regenera modelos/recetas y valida, sin modificar
las texturas editables. Solo `generate_pipe_textures.py --write` sobrescribe todos
los sprites de tuberias. La llave de 16x16 se reproduce con el generador de modelos.

## Editor y canales

El editor usa iconos de recursos solo en la universal; los cuatro pueden operar
simultaneamente. La lista permite seleccionar, crear, editar y quitar reglas.
El slot fantasma copia el ID; tambien se puede escribir un ID o tag. Diamante
habilita componentes SNBT ignorados, parciales o exactos, inversion de una regla
respecto a la lista, subcanal y destino concreto. No hay controles de redstone.

El canal vacio es comodin; los demas son insensibles a mayusculas. `/` separa
subcanales, con 256 caracteres en total y hasta 16 segmentos. El prefijo general
se muestra separado y no se duplica al completar un subcanal. Cambiar el canal
general actualiza los prefijos de sus reglas; se rechaza un cambio que recortaria
un destino. Destino y canal se deben cumplir simultaneamente. Las coordenadas
permanecen aunque el almacen desaparezca; otro inventario compatible colocado en
el mismo sitio vuelve a ser valido sin editar la regla.

El autocompletado trabaja solo con el menu abierto y el campo activo. La consulta
se retrasa cinco ticks tras escribir y conserva hasta 16 coincidencias ordenadas,
con cinco visibles. Recorre hasta 32 nodos por avance dentro del presupuesto de
topologia compartido. No copia los nodos del grafo ni materializa todas las cadenas
de la red. Cerrar/cambiar consulta libera su recorrido; una busqueda no deja
trabajo de topologia propio ejecutandose despues de cerrar el menu.

Esto acota la memoria adicional de busqueda, no la memoria total de una red:
los perfiles de las tuberias cargadas siguen guardando cadenas y las rutas siguen
siendo por origen/recurso. El diccionario compacto y el grafo por componente son
trabajo pendiente, no una optimizacion ya certificada para millones de canales.

Rutas de mantenimiento bajo `feature/logistics/`:
- `domain/model/`: niveles, perfiles, reglas, destinos y reparto.
- `adapters/input/`: persistencia de mejoras, marcador, menu y motor de transporte.
- `adapters/output/client/PipeConfigurationScreen`, `PipeRuleEditor` y
  `PipeChannelCompletion`: editor principal, regla y sugerencias, respectivamente.
- GameTests dirigidos: `feature/logistics/LogisticsUpgradeGameTests`,
  `LogisticsRuleGameTests`, `LogisticsPayloadGameTests` y suites de transporte.

## Validacion registrada

- 2026-09-09: `check` y 93/93 GameTests en
  `artifacts/pipe-cap-interaction-check-20260909.log`; `clean releaseCheck`
  y 93/93 en `artifacts/pipe-fixes-release-20260909.log`. El validador recorre
  960 configuraciones: uniones sin caras internas, tapas de maquinas con culling
  vanilla y textura opaca estatica compartida. XP se transfiere de ida y vuelta
  por las seis caras, con tuberias de fluidos y universales; el contrato de
  capacidades comprueba tambien rollback por cara.
  Gestos reales y teclado OpenGL/Vulkan en
  `artifacts/pipe-interactions-{opengl,vulkan}-20260909/`; buscador, cursor y
  fabricacion en `artifacts/terminal-text-input-opengl-20260909/`.
  Tapa negra vista desde dentro del Almacen transparente en
  `artifacts/pipe-dark-cap-{opengl,vulkan}-20260909/`.
  JAR de desarrollo SHA-256:
  `7695EDC4D043393FDACB90F6B9BAB1AE2B038B5A99A3463A8DB2DBB73D4E22AB`.
  Informes/JAR previos en `artifacts/preclean-pipe-fixes-20260909/` y mediciones
  restauradas en `build/performance/`. Sin cambios en el mundo original `Test`.
  No es una comparativa de FPS ni evidencia final de reproducibilidad.

- 2026-09-08, editor por niveles: `check` y 92/92 GameTests en
  `artifacts/pipe-editor-complete-tests-20260908.log`; cierre posterior con
  `clean releaseCheck` y 92/92 en `artifacts/pipe-editor-release-20260908.log`.
  Cobertura: los cuatro repartos, liquidos/gases/energia equitativos desde el
  primer lote, perfiles al intercambiar mejoras del mismo nivel, paquetes
  tardios/invalidos, NBT y copia, destino mas canal, reemplazo del destino,
  marcador sin consumo, consultas acotadas y limites exactos de canales.
  Capturas y clics reales OpenGL/Vulkan con REI/Jade en
  `artifacts/pipe-advanced-completion-{opengl,vulkan}-20260908/`; incluyen
  guardado/reapertura, regla avanzada y autocompletado de subcanal desde servidor.
  La pantalla principal tambien se comprueba en
  `artifacts/pipe-editor-tier-opengl-20260908/`.
  JAR de desarrollo: `build/libs/trading_cells-1.0.0.jar`, SHA-256
  `EE6585136DAF3B96FA3285E16D2D674C283122884F2252E8E4C50A33DE89E450`.
  Informes y JAR anteriores en `artifacts/preclean-pipe-editor-20260908/`;
  `build/performance/` restaurado despues de limpiar. El mundo original `Test`
  no se modifica. Estas pruebas funcionales no certifican FPS ni regresiones
  inferiores al 1 %, y no sustituyen la reproducibilidad final de publicacion.

- 2026-09-08: `check`, 80/80 GameTests y `clean releaseCheck` pasan. Logs:
  `artifacts/pipe-visuals-single-upgrade-20260908.log` y
  `artifacts/pipe-visuals-release-20260908.log`. Incluyen extraccion real de
  salidas de maquinas por todas las caras, rollback, marco de un voxel,
  slot unico y devolucion de mejoras antiguas sin perdida/duplicacion.
  El validador de recursos recorre 640 configuraciones, sus superficies
  exteriores exactas, UV de brazos y los ocho fotogramas de todas las uniones.
  Capturas reales de codos/T/cruces y animacion en
  `artifacts/pipe-continuous-close-{opengl,vulkan}/`; camara interior sin tapas
  en `artifacts/pipe-continuous-inside-opengl/`. No son una comparativa de FPS.
  Informes previos en `artifacts/preclean-pipe-visuals-20260908/reports/`.

- Cierre de esta tanda, 2026-09-07: `clean releaseCheck` pasa con `check` y
  78/78 GameTests. Log: `artifacts/logistics-ui-release-20260907.log`.
  JAR de desarrollo: `build/libs/trading_cells-1.0.0.jar`, SHA-256
  `6071757CA95E503858664E5128CB7D7D5CFE36CDEC869FF16CC6073A43B2B961`.
  Informes previos en `artifacts/preclean-logistics-ui-20260907/reports/`;
  plantillas y mediciones conservadas en `build/performance/`. No constituye
  congelacion de funcionalidades ni la prueba final de reproducibilidad.

- 2026-09-07: 78/78 GameTests en `artifacts/logistics-pending-route-final.log`.
  Se verifica la espera de una transferencia durante una reconstruccion y su
  cancelacion cuando cambia el cursor. Las acciones antiguas por mano conservan
  su comportamiento inmediato; la espera corresponde a los controles actuales.
  Capturas y clics reales de fabricacion OpenGL/Vulkan en espanol con REI/Jade:
  `artifacts/logistics-ui-final-{opengl,vulkan}/`. Se prueban recogida, plantilla,
  deposito, fabricacion y desplazamiento con ambas filas limite parciales.
  El guardado al salir y reapertura del editor se verifica en
  `artifacts/logistics-ui-pipe-verified-opengl/`. El editor tambien se reviso
  en ambos backends; el conteo de pixeles confirma los 36 marcos del inventario
  completos.
  Son comprobaciones funcionales y visuales, no una comparativa de FPS.

- Correcciones de interfaz del 2026-09-06: `check` y 77/77 GameTests en
  `artifacts/logistics-ui-fixes-validation.log`. Incluyen cursor, fabricacion,
  slots de recipientes, copia parcial, cuatro mejoras, energia equitativa,
  extraccion lateral de granjas y rollback exacto de salidas de todas las maquinas.
  Las cifras del panel de botin configurable se analizan desde tablas cargadas
  sin tirar botin ni avanzar las secuencias aleatorias del mundo. Los casos
  externos no analizables conservan la indicacion de informacion desconocida.

- 2026-09-06, continuacion: `clean releaseCheck` y 69/69 GameTests, incluidos
  32 casos de logistica. Log: `artifacts/logistics-routing-release-20260906.log`.
  Se suman reparto circular transaccional de recetas, fallos de retirada sin
  saltar turnos, restos repartidos hacia la red con rollback, cambio de mejora y
  filtros en una confirmacion, permisos reales del servidor y recipientes de
  liquidos/gases en ambas manos. Se comprueban operaciones indivisibles,
  conservacion del cubo y de la otra mano, menu obsoleto y espectador.
- Tres repeticiones adicionales de esta continuacion pasan 69/69, seguidas de
  `check`: `artifacts/logistics-routing-stability-20260906-{1,2,3}.log` y
  `artifacts/logistics-routing-check-20260906.log`. No se ha repetido en esta
  tanda la matriz visual OpenGL/Vulkan ni se atribuyen mejoras de rendimiento.
- La nueva prueba de red parcial mantiene cargados ambos extremos, retira el
  tramo central y verifica que no se carga ni se atraviesa, conservando los
  objetos y reanudando al reconectarlo. Distingue un chunk inaccesible para la
  red de su descarte completo en memoria; el caso anterior sigue verificando
  descarga completa y persistencia. Ninguna prueba modifica el mundo `Test`.
- JAR de esta continuacion: `build/libs/trading_cells-1.0.0.jar`, SHA-256
  `72763E072F75CDFD5570BF03958C1B0CF4541812E17EF2A324718496F4EAFDAF`.
  Es una compilacion de desarrollo, no evidencia final de reproducibilidad.
  Informes anteriores preservados en `artifacts/preclean-routing-20260906-110345/`;
  las mediciones anteriores siguen en `build/performance/`.
- Referencia anterior del 2026-09-06: `check` y 59/59 GameTests, incluidos 22 casos de logistica.
  Casos: inventarios reales, salida parcial, prioridad extrema, canales/filtros,
  rotura de enlace, XP por fluido, perfiles persistentes, mejoras, paquetes de
  configuracion invalidos, componentes, rollback, gases, FE y crafteo por lotes.
- Se incluyen distancia y reparto circular, cuatro recursos simultaneos,
  desaparicion/reaparicion de capacidades sin cambiar el bloque, descarga real
  de dos chunks y recuperacion del transporte, restos de crafteo con rollback,
  limites y round trips de paquetes, y rechazo de paginas antiguas. Una suite
  recorre todas las maquinas WorldlyContainer y sus seis caras con inserciones
  y extracciones transaccionales reales, sin depender de campos privados.
- El catalogo del terminal detecta nuevas instantaneas de recetas tras recargas.
  La fabricacion establece el contexto de jugador de NeoForge y notifica las
  fabricaciones solo despues del commit completo, nunca tras un rollback.
- `clean releaseCheck` pasa el 2026-09-06; log conservado en
  `artifacts/logistics-release-check-20260906-final.log`. Las pruebas limpias
  detectaron el reinicio injustificado del turno circular al invalidar rutas:
  corregido conservando turnos y eliminando solo grupos obsoletos. Los tests
  de reconexion esperan el resultado observable, y el de chunks espera la
  descarga completa, no solo la retirada del conjunto de chunks accesibles.
- Tres repeticiones adicionales: 59/59 en todas; registros
  `artifacts/logistics-stability-20260906-{1,2,3}.log`. JAR de desarrollo:
  `build/libs/trading_cells-1.0.0.jar`, SHA-256
  `38AF2D268261D4A46A90D9A32273EA145F24A24F9254AD8B14913852E12DF39A`.
  No es evidencia de reproducibilidad de publicacion. El build anterior se
  conserva en `artifacts/preclean-logistics-20260906-0405/`; las mediciones y
  capturas siguen en `build/performance/`.
- Arranque dedicado sin REI ni Jade realizado por GameTests. Las geometrías de
  colision repetidas demoraban el arranque mas de 100 segundos;
  compartir las 729 combinaciones distintas permitio completar la ejecucion
  completa de 42 pruebas en 15 segundos. No es una comparativa de FPS o TPS.
- Generador reproducible: `checkLogisticsResources` y
  `python tools/generate_logistics_resources.py --check`.
- Plantillas reproducibles `logistics-{1024,4096}-{idle,active}` en
  `tools/performance/prepare_system_matrix.py`. Resultados medidos en
  `tools/performance/RESULTS.md`, no inferidos del numero de tuberias.
- Cuatro escenarios dedicados, tres pasadas cada uno. Terminal de 1.024 tuberias:
  tres pasadas OpenGL y tres Vulkan a 1280x720, Jade presente y REI ausente.
  El panel fue identico pixel a pixel en las tres parejas; esto no certifica
  todas las pestañas, escalas, menus o la representacion de la red descubierta.

## Pendiente de aceptacion

- Completar todos los caudales y desgaste del presupuesto, recipientes de energia
  y de adaptadores externos, recetas especiales de mods y permisos de proteccion
  externos. Los casos vanilla de liquidos/gases en ambas manos y restos hacia la
  red ya estan cubiertos, al igual que el corte y reconexion de un tramo central.
- Medir el resto de escenarios cliente (red visible y terminal con contenido),
  muchos extractores simultaneos, y repetir la referencia dedicada con el mismo
  codigo antes de aceptar optimizaciones. Validar formas, agua, drops, recetas,
  filtros y pantallas con REI/Jade, idiomas y escalas.
- Revisar equidad entre terminales grandes, memoria de rutas por componente y
  consultas de inventarios externos de gran tamano.
- Las rutas se cachean por origen/recurso;
  no hay todavia un grafo compartido por componente conectado.
- Separar el siguiente trabajo de escala en tres cambios medibles: primero
  limitar la preparacion de candidatos del reparto equitativo (hoy recorre el
  grupo de prioridad antes de transferir); despues compartir topologia por
  componente y estudiar IDs compactos de canales; finalmente medir redes de
  1.024/4.096 tuberias y catalogos extremos con varios editores simultaneos.
  No considerar que la ventana de 16 sugerencias resuelve por si sola estos costes.
- Mekanism aplazado: el 2026-09-05 la [ultima version oficial publicada](https://github.com/mekanism/Mekanism/releases)
  es `v1.21.1-10.7.19.85`, no una estable para 26.2. No integrar una API de otra
  version ni cargar clases ausentes; revisar antes de implementar quimicos.
- No declarar el plan integral completado
  hasta cerrar los puntos anteriores.
