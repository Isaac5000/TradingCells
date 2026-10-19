# Project Context

## Objetivo del proyecto

Trading Cells es un mod NeoForge de maquinas portatiles para automatizar trabajo de
aldeanos y piglins. Conserva entidades, ofertas, inventarios, progreso, filtros y
XP al romper y recolocar los bloques. El codigo actual es la fuente de verdad.

## Estado actual

- Salidas de interacciones 2026-10-19: copia de ranuras intactas diferida hasta
  confirmar espacio completo. Sin mutaciones ni referencias compartidas en el
  resultado. Equivalencia con 500 casos deterministas; `runGameTestServer check`:
  175/175. Medicion local 41.79-68.97 % menor, no FPS/MSPT globales. Evidencia en
  `docs/AUDIT_20261018.md`.
- Interacciones 2026-10-19: limpieza en `finally` sin una segunda copia de reglas
  del mundo; cada intento sigue recibiendo reglas propias actualizadas.
  `runGameTestServer check`: 174/174. Preparacion/limpieza local 50.02 % menor,
  sin atribuir mejora global ni cambiar lotes. Evidencias y limites en
  `docs/AUDIT_20261018.md`.
- Listados 2026-10-18: metadatos de pilas repetidas reutilizados solo durante
  cada lectura, sin cache de cantidades entre ticks. Comparacion acotada tras
  ocho fallos y mapa de slots por cara consultado una vez. Equivalencia validada
  con componentes, huecos, cantidades distintas y cambios entre lecturas.
  `runGameTestServer check`: 173/173. Medicion local de 31 rondas y limites en
  `docs/AUDIT_20261018.md`; no atribuir mejora de FPS ni dar por resuelto el
  presupuesto por ranuras. Granjas de interacciones conservan sus lotes.
- Auditoria y correcciones 2026-10-18: chunks ajenos ya no invalidan redes;
  indice por chunk conserva bordes y puentes. Limpieza de temporales robusta,
  dos clases sin consumidores retiradas y documentacion vigente corregida.
  `compileGameTestJava verifyDomainRules` y `runGameTestServer check` correctos,
  172/172 GameTests. Graphify actualizado por AST para el proyecto.
  Pendientes de medicion: presupuesto por ranuras y reparto de interacciones;
  no cambiar produccion ni transacciones sin esa validacion.
  Alcance, limites y registros: `docs/AUDIT_20261018.md`.
- Cierre 2026-10-18: migracion a Minecraft 26.3.0 / NeoForge 26.3.0.88.
  `check` y 170 GameTests correctos; incluye nuevas ofertas de aldeanos enviadas
  al cliente sin cerrar el menu al subir de nivel. OpenGL con REI beta y Vulkan
  sin REI comprobados; tuberias y terminales verifican controles SDL3.
  Contrato y limites: `docs/MIGRATION_26_3.md`.
- Graphify 2026-10-18: analisis local de logistica; salida directa para componentes
  vacios, con identidad equivalente al codec vanilla. Medicion local 86,07 %,
  sin atribuir mejora global de FPS/MSPT. `check runGameTestServer`: 170 correctos.
  Informe y reproduccion: `docs/GRAPHIFY_OPTIMIZATION.md`. Grafo con referencias
  externas incompletas; no usarlo como prueba de codigo muerto.
- Limpieza solicitada: ya no se necesita `artifacts/` para este trabajo, pero el
  entorno rechazo su borrado. La carpeta sigue intacta y queda pendiente de
  eliminacion. Registros finales locales: `graphify-out/validation/`.

- Soldador 2026-10-16: brazo recto hacia delante, sin giro lateral fijo;
  sigue la mirada, conserva agarre alto, hombros fijos y recarga. Primera
  persona intacta. OpenGL+REI/derecha comprobado: `artifacts/storm-straight-arm-opengl`.
  Vulkan/izquierda sin REI comprobado el 2026-10-17, incluidos giros extremos,
  recarga y descarga: `artifacts/storm-straight-arm-vulkan-retry-20261017`.
  Reintento correcto sin cambios de codigo; primer inicio tuvo timeout grafico.
  `checkGraphicsBackendIndependence check releaseCheck`: 169 GameTests correctos,
  `artifacts/storm-straight-arm-check.log`.
  Cierre 2026-10-17: `check releaseCheck` repetido, 169 GameTests correctos;
  `artifacts/storm-straight-arm-final-20261017.log`.

- Creativo 2026-10-16: mejoras al final de Objetos, orden tuberias/barter/
  cantera/velocidad/capacidad; cada familia de cobre a netherita. Catalogo
  validado en GameTest; `check releaseCheck`, 169 pruebas correctas.
  Evidencia: `artifacts/creative-upgrade-order-check.log`.

- Recarga 2026-10-16 revisada: pulsacion corta de R expulsa y encadena carga;
  mantener 12 ticks solo expulsa, sin recargar al soltar. Recambio elegido
  antes de devolver la bateria usada; sin recambio solo descarga. Validacion
  de ranura y arma vacia antes de consumir; estados transitorios se cancelan
  al cambiar de arma/soltar/desconectar. Sustituye el bloqueo previo con carga.
  `verifyDomainRules check releaseCheck` correctos; 169 GameTests.
  Evidencia: `artifacts/storm-reload-swap-tests.log` y
  `artifacts/storm-reload-swap-check.log`. Sin repetir matriz visual.

- Revision 2026-10-16 cerrada: soldador sin minado, melee ni gesto de golpe;
  ambas armas muestran carga como durabilidad, con Irrompible y Reparacion
  vanilla (2 cargas/XP, sin crear baterias ni destruir el arma vacia).
  Condensador y armas comparten barras verde/amarillo/rojo. Soldador agarrado
  alto en el mango; postura anterior del brazo restaurada por peticion posterior
  (sin acercarlo al pecho ni bajarlo). Hombros fijos y primera persona intacta.
  `check releaseCheck`: 168 GameTests. OpenGL+REI/derecha y Vulkan sin
  REI/izquierda comprobados: `artifacts/storm-chest-charge-*`.
  Restauracion posterior del brazo: `check releaseCheck`, 168 GameTests,
  `artifacts/storm-arm-restore-check.log`; sin repetir la matriz visual.
  Contrato: `docs/ELECTRIC_EQUIPMENT.md`. Sin cambios en GitHub ni PNG.

- Revision 2026-10-13: cinco avisos de compilacion tratados. Armas usan las
  APIs de proyectiles con ItemStack; queda el puente abstracto obligatorio
  marcado Deprecated. Constructor de PrimaryLevelData aislado con supresion
  local documentada: NeoForge 26.2 aun exige SpecialWorldProperty, sin sustituto.
  Fixtures de cantera/hierro sin casts ni reflexion; trial spawner usa reglas
  normales restauradas en finally, sin API marcada para eliminacion.
  Compilacion dirigida sin esos avisos; `check releaseCheck` y 166 GameTests
  correctos. Evidencia: `artifacts/compiler-warnings-directed.log` y
  `artifacts/compiler-warnings-check.log`. Sin cambios en GitHub.

- Ampliacion 2026-10-12 cerrada: porra/soldador descargan con la animacion de
  carga invertida (30 ticks), sin duplicacion y con otra pulsacion de R inmediata.
  Recetas usan cobre en vez de pararrayos; soldador lleva fragmento de tormenta
  en vez de redstone_block. Huevo generico conserva tier y usa cuatro colores,
  tambien en REI; huevos registrados sin cambios. `check releaseCheck`: 166
  GameTests; OpenGL+REI/derecha y Vulkan sin REI/izquierda comprobados.
  Evidencia `artifacts/storm-unload-*`; contrato `docs/FIXES_20261012.md`.
  Sin GitHub, subagentes ni cambios de texturas de las armas.

- Revision 2026-10-12 cerrada: ambas canteras con pausa persistente y seleccion
  blacklist/whitelist por material. Exclusiones completan ciclo/desgaste sin
  salida, sin cambiar sorteos ni probabilidades, incluso con almacenamiento
  lleno. Granjas de entidades comparten modos; filtros antiguos siguen siendo
  blacklist. Casilla Activado/Enabled comun tambien en interacciones y tuberias.
  `check releaseCheck`: 165 GameTests; OpenGL+REI/es y Vulkan sin REI/en,
  controles, sincronizacion, snapshots portatiles y slots nativos comprobados.
  Evidencia `artifacts/quarry-filters-final-check.log`, `artifacts/quarry-filters-*`.
  Electricidad: dano antes de conversion, sin fuego/dano de rayo adicional;
  porra con audio de 0,3 s. Encantamientos farmer/miner/warrior_touch con alias
  de IDs antiguos y migracion de LockedRecipe. Jade compacta solo vistas de
  capturadores, sin tocar datos reales; prueba real 796 bytes en ambos clientes.
  Aviso de idioma: claves activas iguales; ingles antiguo dentro de artifacts
  contaminaba indice IDE. Exclusion Gradle configurada; requiere reimportar IDE.
  Contrato y limites: `docs/FIXES_20261012.md`. Sin GitHub ni subagentes.

- Revision 2026-10-11 cerrada: porra base 1 sin carga/16 cargada; expulsar
  condensador no inicia recarga, otra pulsacion de R la inicia sin demora.
  Repeticion de teclado ignorada y pulsaciones entre ticks reconocidas.
  Criadero de aldeanos crea datos nuevos sin profesion/ofertas/NBT paternos,
  incluso con capturadores duplicados y BabyTemplate antiguo pendiente.
  Sintetizador sin libro ni categorias; REI, XP, bloqueo y fantasmas conservados.
  Ambos efectos electricos usan `electricity.ogg` aportado por el usuario, mono.
  `check releaseCheck`: 158 GameTests, registro
  `artifacts/revision-machines-final-check.log`. GUI y recarga OpenGL+REI/derecha
  y Vulkan sin REI/izquierda: `artifacts/revision-machines-{opengl,vulkan}`.
  Fabricacion/75.000 XP/fantasmas: `artifacts/revision-synthesizer-ghost`.
  Contratos: `docs/BREEDERS.md`, `docs/ELECTRIC_EQUIPMENT.md`,
  `docs/ARCANE_INFUSER.md`. Sin GitHub ni subagentes; no repetir este hito.

- Ajuste posterior 2026-10-11: soldador con agarre de una mano autorizado;
  la otra interviene solo al recargar, sin trasladar hombros. Condensador visible
  en primera persona, asas sin caras coplanares e icono centrado (+3 px).
  Texturas originales conservadas. `check releaseCheck`: 158 GameTests;
  OpenGL+REI/derecha y Vulkan sin REI/izquierda, incluyendo vistas extremas.
  Evidencia: `artifacts/storm-single-grip-check.log` y `artifacts/storm-single-grip-*`.

- Correccion de armas 2026-10-11 cerrada: modelos OBJ cerrados sin las tapas
  incompletas y recarga vertical con la mano opuesta.
  R configurable: pulsar recarga; mantener 0,6 s expulsa. Bateria agotada sale
  al pulsar incluso sin recambio; inventario lleno la arroja delante, sin duplicar.
  Solo `voltaic_capacitor`, 256 cargas, receta con bloque de cobre central;
  ID avanzado retirado como item activo, alias conserva partidas y componentes.
  Haz unido al modelo real y sonidos propios de arco/porra. Pruebas: 158
  GameTests, `check releaseCheck`, OpenGL+REI/derecha y Vulkan sin REI/izquierda.
  Registro `artifacts/storm-reload-verified.log`; capturas en
  `artifacts/storm-reload-verified-opengl` y `artifacts/storm-reload-verified-vulkan`.
  Fixture prueba pulsacion, mantener y volver a pulsar R, ademas de Controles.
  Contrato: `docs/ELECTRIC_EQUIPMENT.md`. Sin GitHub ni subagentes.

- Revision de sintetizador/equipo electrico 2026-10-10 cerrada: plataforma 3x3
  con ingredientes centrados, UV proporcionadas en sintetizador/mesa de huevos;
  condensadores intercambiados visualmente, receta avanzada conserva carga y
  componentes; cobre sustituye pararrayos en ambas recetas. Nombre de estacion
  corregido. Armas 3D industriales segun referencias, recarga fisica lateral
  con bateria cargada, porra tipo espada y soldador con cuatro pulsos/s incluso
  tras conversion. Dragon + botella produce aliento en interacciones.
  NeoForge actualizado a estable 26.2.0.90, Minecraft 26.2 sin cambios.
  `check releaseCheck`, 158 GameTests y OpenGL+REI/Vulkan sin REI correctos.
  Registro final `artifacts/storm-neoforge90-final.log`; visuales
  `artifacts/storm-industrial-final-opengl` y `artifacts/storm-industrial-vulkan`.
  Contratos: `docs/ELECTRIC_EQUIPMENT.md`, `docs/ARCANE_INFUSER.md`,
  `docs/ENTITY_INTERACTION_FARM.md`. Sin GitHub ni subagentes. No repetir el hito.

- Mesa de generacion de criaturas 2026-10-04 implementada: nucleo/huevo del tag,
  XP compartida, blacklist de servidor y huevo externo generico. 86 recetas de
  huevos archivadas fuera de recursos; Wither/Dragon eliminadas. Cuatro GameTests
  nuevos y `check` correctos; GUI OpenGL+REI/Vulkan sin REI comprobada. Contrato:
  `docs/ESSENCE_AUTOMATION.md`. Cierre 2026-10-08: `check releaseCheck` correcto,
  155 GameTests. No repetir implementacion de esta mesa.
  Correccion posterior 2026-10-08: el ingrediente pedido era un huevo de
  `arcane_infusion_eggs`, no una base. Filtros, REI y consumo actualizados;
  costes segun tier del nucleo, mesa con modelo y cuatro texturas 16x16 propias.
  Cierre 2026-10-09: `check releaseCheck`, 156 GameTests y flujo completo en
  OpenGL+REI/Vulkan sin REI correctos. Fallo de ingrediente resuelto.

- Revision electrica 2026-10-10 cerrada: fulgurita retirada; baterias
  128/256, estacion de fragmentos (16 cargas) con bateria en una sola ranura;
  porra +10 de dano cargada, soldador continuo con consumo inicial inmediato.
  Modelos/materiales propios y rayo ramificado. Contrato: `docs/ELECTRIC_EQUIPMENT.md`.
  Sintetizador de materia sustituye aspecto/nombre del infusor manteniendo
  `arcane_infuser`, recetas y persistencia. Camara, emisores y pantalla superior.
  `check releaseCheck`, 156 GameTests y OpenGL+REI/Vulkan sin REI pasan.
  Armas en ambas perspectivas y manos, haz continuo y menus verificados.
  JAR sin fulgurita ni modelos del infusor retirados. No repetir este hito.
  Cofres dobles y capa animada del fragmento resueltos anteriormente. Sin GitHub.

- Correcciones 2026-10-04: IDs de capacidad generales con alias; jeringa con
  agarre/recarga por ambas manos, extraccion 2/4/6/8 s y avance fijo de 6 ticks;
  azada obligatoria, siluetas de cobre, Jade cian y llave nueva 32x32.
  Sin cooldown de jeringa; brazo extendido hasta completar llenado. Capacidad
  movida al TAB Objetos; llave orientada por el mango sin tocar el PNG del usuario.
  Cierre `check releaseCheck`: 145 GameTests, 47 pruebas de recursos y 29 de
  recetas correctos; animaciones OpenGL/Vulkan con/sin REI comprobadas.
  Evidencias y alcance en `docs/FIXES_20261004.md`. No repetir estos hitos.

- Ajustes 2026-10-03: granjas de cultivos con capacidad x2..x32, Fortuna
  conservada y Eficiencia sin efecto. Generadores extraen huevo solo con Shift
  + clic derecho, soltandolo sobre el bloque. Marcos REI reducidos sin recorte;
  PNG de viales aprobados preservados como fuentes finales. Detalles y pruebas:
  `docs/FIXES_20261001.md` y `docs/FARMER_CROP_DATAPACKS.md`.

- Version de desarrollo no publicada: `1.0.0` para Minecraft `26.3.0` y
  NeoForge `26.3.0.88`; versiones efectivas en `gradle.properties`.
- Java 25 y Gradle Wrapper 9.8.0; `gradlew` esta registrado como ejecutable.
- La referencia reproducible anterior se documento el `2026-08-31` en
  `docs/releases/1.0.0-validation.md`: dos JAR identicos con 26 GameTests. Es
  evidencia historica anterior a los hitos actuales. El arbol de desarrollo del
  `2026-09-10` pasa `releaseCheck`, incluido `check`, 100/100 GameTests y
  14 pruebas Python de recursos.
  Los terminales pasan clics reales y scroll en OpenGL/Vulkan con REI/Jade.
  Evidencia y JAR de desarrollo en `docs/LOGISTICS.md`; la matriz manual,
  reproducibilidad final y CI del commit definitivo siguen abiertas.
- El arbol actual conserva recetas del Sintetizador (sin libro propio), cultivos por datapack,
  diagnostico comun y modos de redstone persistentes. Las granjas antiguas por
  familia fueron retiradas por el usuario: no restaurar sus clases ni recursos.
  El objeto Configurador se ha retirado por peticion expresa.
  La sustitucion del Controlador y la red experimental de XP por logistica
  universal esta en desarrollo; estado y validacion en `docs/LOGISTICS.md`.
  Texturas de tuberias por tipo, uniones/animacion continuas, caras internas
  omitidas y migracion a una mejora cerradas. Editor por niveles, reparto por
  recurso, marcador de inventarios, reglas avanzadas y autocompletado acotado
  implementados y comprobados con clics reales en OpenGL/Vulkan.
  Pendientes: escala de redes, topologia compartida y memoria de canales.
- Esencias y automatizacion, 2026-09-25: flujo de cuatro tiers, estabilizador,
  jeringa 3D con recarga/extraccion, mesa con salida de crafting/XP/autocraft e
  infusor bloqueable desde recetario. Estado y pruebas: `docs/ESSENCE_AUTOMATION.md`.
  Reglas de equipo y previsualizacion de loot trasladadas al sistema general.
  Marcos de mejoras generales independientes en `tools/assets/upgrade_bases/frames`;
  no recuperar los cinco PNG genericos antiguos. Nombres de mejoras resueltos
  por el usuario: no volver a renombrarlos.
- Granja de interacciones, 2026-09-27: maquina independiente con canales ITEM y
  TOOL, entorno desechable, colmena vanilla para abejas, pendientes atomicos y
  automatizacion lateral. Contratos e hitos: `docs/ENTITY_INTERACTION_FARM.md`.
  No altera la granja de muerte ni requiere XP operativo. Sin trabajo en GitHub.
  Cierre 2026-09-28: `clean check releaseCheck`, 131 GameTests y cuatro perfiles
  visuales OpenGL/Vulkan con/sin REI aprobados; cinco PNG de viales sincronizados
  con autorizacion del usuario. Implementacion terminada; limites en el contrato.
- Correcciones posteriores (2026-09-28): progreso en `docs/FIXES_20260928.md`.
  La granja de interacciones queda sin control de redstone ni salida de comparador;
  conserva pausa manual e ignora modos antiguos guardados. El usuario cancelo
  el nuevo boton de automatizacion del estabilizador. Cierre 2026-09-29: velocidad
  dedicada en cultivos/canteras sin Eficiencia, 27 salidas en las siete maquinas,
  fondos/siluetas, control XP comun, mecanismo visual de interacciones y maximo
  10 XP por entidad implementados. `check releaseCheck`, 135/135 GameTests,
  36 pruebas Python de recursos y matriz visual OpenGL/Vulkan con/sin REI pasan.
  Hitos terminados; no repetirlos al reanudar.
- Correcciones nuevas 2026-09-29/30: `docs/FIXES_20260929.md`. Capacidad en
  interacciones (slot nuevo 32), Mooshrooms renovables, reloj/cofre vanilla en
  mejoras, recetas y tab de velocidad, fondos PNG tematicos y tooltips con Shift.
  Toque del Granjero protege herramientas solo en el entorno de interacciones.
  `check releaseCheck` pasa con 139 GameTests, 37 pruebas Python de recursos y
  28 de recetas. Matriz visual OpenGL/Vulkan con/sin REI cerrada, incluidos
  tooltips es/en y menu compacto a 854x480. Hitos terminados; no repetirlos.
- Revision 2026-10-01: marcos con esquinas proporcionadas y detalles repetidos,
  variantes compactas sin deformar PNG, contenido y REI fuera del borde.
  Fondos en la carpeta de cada maquina, no `gui/themes/`. Retirados 16 PNG
  obsoletos, 26 variantes sin uso y tres helpers de dibujo; fuentes intactas.
  `check releaseCheck`: 139 GameTests, 42 pruebas de recursos y 28 de recetas.
  Alcance/evidencia en `docs/CLEANUP_CANDIDATES.md` y `docs/FIXES_20260929.md`.
- Reorganizacion GUI posterior del 2026-10-01 cerrada: convencion por ID de
  bloque en `gui/container/` y `gui/sprites/`, documentada en `docs/GUI_RESOURCES.md`.
  Retiradas las 88 copias anteriores tras verificar hashes; 103 PNG activos y
  fuentes intactas. `check`/`releaseCheck` vuelven a pasar.
- Correcciones siguientes del 2026-10-01: `docs/FIXES_20261001.md`. Margen REI,
  resina del crepitante con hacha, huevos/monturas, tooltips sin mundo, IDs de
  velocidad generales con alias y mejoras de capacidad con bundle. Consultar
  ese documento antes de repetir tareas o afirmar cerrada la matriz visual.
- Cierre 2026-10-02: menus compactados sin cambiar escala ni slots; buscador y
  tres columnas REI en escala automatica desde 854x480. Infusor con equipo
  utilizable bajo recetario abierto, coste XP centrado y controles separados.
  `clean check releaseCheck`: 142 GameTests, 43 pruebas de recursos y 28 de
  recetas; OpenGL/Vulkan con/sin REI, terminal y tuberias comprobados. JAR limpio
  verificado; detalles y evidencias en `docs/FIXES_20261001.md`. No repetir hitos.

## Arquitectura

Arquitectura por features con capas `domain`, `application` y `adapters` cuando
aportan valor. NeoForge, registro, red y composicion viven en `platform/neoforge`.
Las reglas puras comunes estan en `shared`. La definicion completa de dependencias
permitidas esta en `ARCHITECTURE.md` y la ruta corta de cada feature en
`PROJECT_INDEX.md`.

## Estructura relevante del repositorio

| Ruta | Responsabilidad |
| --- | --- |
| `src/main/java/.../feature/` | implementacion vertical de cada mecanica |
| `src/main/java/.../platform/neoforge/` | adaptacion, registro, red e integraciones |
| `src/main/java/.../shared/` | reglas puras realmente compartidas |
| `src/main/resources/` | assets, datos, tags, recetas, mixins y metadatos |
| `src/test/` | reglas y contratos Java sin servidor |
| `src/gameTest/` | integracion NeoForge/Minecraft por feature |
| `tools/` | validadores, generadores, publicacion y rendimiento |
| `docs/` | contratos funcionales, compatibilidad y publicacion |

No buscar dentro de `.gradle/`, `build/`, `logs/`, `run/` o `run-server/` salvo
que la tarea trate expresamente resultados o mundos locales.

## Ruta documental minima

| Necesidad | Leer | Motivo |
| --- | --- | --- |
| Empezar una tarea | `AGENTS.md` y este archivo | reglas operativas, estado e invariantes |
| Localizar una feature | `PROJECT_INDEX.md` | indice corto de rutas y validacion dirigida |
| Cambiar fronteras Java | `ARCHITECTURE.md` | dependencias completas que no se duplican aqui |
| Usar o publicar el mod | `README.md`, `CHANGELOG.md` | documentacion publica |
| Tocar un contrato concreto | solo el archivo enlazado de `docs/` | mecanica, compatibilidad o publicacion |
| Modificar herramientas | `tools/README.md` o `tools/performance/README.md` | comandos y salidas propias del tooling |

`PROJECT_INDEX.md` se conserva deliberadamente: evita cargar este contexto y la
arquitectura completa para localizar una clase. Los documentos especializados no
se deben abrir en bloque ni fusionar con este archivo, porque cambian con ritmos y
responsables distintos.

## Modulos y responsabilidades

Las features registradas son `captures`, `combat`, `trader`, `breeders`,
`incubators`, `farmer`, `quarry`, `converter`, `ironfarm`, `experience`, `logistics`,
  `infusion` y `silktouch`; `machinecontrol` conserva contratos internos.
`mobfarm` implementa una granja general por composicion y modulo de entidad;
el catalogo dinamico conserva objetivos vanilla y de datapacks sin registrar
una granja distinta por familia.
`platform/neoforge/mobfarm` mantiene el catalogo dinamico comun y ejecuta loot
tables desde entidades concretas con `MobFarmLootTables`; `MobFarmLootPreview`
analiza probabilidades sin ejecutar loot y `MobFarmEquipmentLoot` conserva
perfiles de equipo sin depender de granjas por familia. La granja general usa
el estado saneado de Creature Models; no restaurar las implementaciones retiradas.

## Funcionalidades implementadas

- Captura, liberacion, cria, incubacion, comercio manual/automatico y trueque.
- Cultivos y Canteras para aldeanos y piglins con herramientas, Fortuna y mejora
  independiente de velocidad, catalogos dinamicos, 27 salidas y persistencia.
  Eficiencia no modifica sus tiempos; el material de la herramienta si. Los cultivos aceptan
  descriptores aditivos `schema_version: 1` mediante snapshots inmutables.
- Conversion de aldeanos, Granja de Hierro y simulacion general de entidades
  con filtros, objetivos ampliables y XP. Los objetivos se seleccionan mediante
  modulos y no mediante bloques por familia.
- Almacen y fluido de XP, Infusor Arcano con recetario categorizado y recetas
  posicionales dispersas, y encantamientos propios.
- Tres modos de redstone y contratos internos de configuracion. La logistica conserva
  capacidades de XP por fluido, sin distribuidor ni tuberia exclusiva de XP.
  Sus tuberias no cargan chunks ni dependen de redstone.
- Toque de Seda II sobre el encantamiento vanilla, bloques especiales, generadores
  preservados, pruebas/Arcas repetibles y control persistente por redstone.
- Integraciones opcionales REI y Jade; clientes OpenGL y Vulkan.

## Decisiones tecnicas importantes

- IDs de registro, payloads, claves NBT, recetas congeladas y formatos publicos no
  dependen de nombres de paquetes Java. La linea 1.0.x exige compatibilidad exacta.
- El servidor es autoritativo. Pantallas y renderizadores no calculan botin,
  recetas, XP ni transferencias.
- `MobFarmCatalog` construye snapshots inmutables solo durante recargas. Los ciclos
  no recorren registros ni tags por tick; un datapack invalido cae al fallback.
- Toque de Seda II sigue siendo `minecraft:silk_touch` nivel 2 para compatibilidad
  externa. Tags propios controlan bloques/herramientas y los datos sensibles solo
  se restauran en objetos marcados por el mod.
- REI/Jade son `compileOnly` u opcionales. El source set cliente no puede filtrarse
  al servidor dedicado ni al JAR.
- El render usa abstracciones Minecraft/Blaze3D. No hay ramas OpenGL/Vulkan.
- Los GameTests verifican contratos observables y se distribuyen por feature; no
  deben acoplarse a metodos internos que puedan moverse entre versiones.
- Las granjas de entidades e interacciones exponen automatizacion real, con
  entradas por las caras definidas por cada maquina y extraccion de salidas.
  La restauracion transaccional no permite inserciones externas en salidas.
- Las optimizaciones se aceptan desde 3 % local o 10 % estructural, sin regresion
  primaria superior al 1 % y con plantilla identica.

## Intentos que no deben repetirse sin nueva evidencia

- Ticker por estado `ACTIVE`: beneficio insuficiente y riesgo de cambiar ticks.
- Cache de preparacion del Autotrader: entre regresion y mejora inferior al umbral.
- Caches de renderizado/posiciones: empeoraron claramente media y p95.
- Oclusion por trazado de visibilidad: redujo asignaciones, pero empeoro escenas
  visibles de piglins; el codigo fue retirado.
- Agrupar paquetes, snapshots compactos o intercambio masivo: faltan plantillas
  multijugador equivalentes para demostrar seguridad.

Detalles y numeros: `tools/performance/RESULTS.md`.

## Convenciones de codigo

- Preferir patrones existentes y cambios dentro de la feature propietaria.
- Introducir codigo compartido solo para una regla verdaderamente comun.
- Usar `Path`/`Files` en Java y `pathlib.Path` en Python; UTF-8 explicito.
- No invocar shells para operaciones que cubren Java, Gradle o Python.
- Toda mutacion persistente de una Block Entity debe marcar y sincronizar su estado.
- Conservar cambios de usuario; no limpiar el arbol de trabajo automaticamente.
- No añadir un generador o script sin consumidor comprobado y salida reproducible.

## Dependencias y herramientas

- Desarrollo/verificacion: Git, JDK 25 y Python 3.11 o posterior.
- `tools/requirements.txt` fija Pillow para validadores/generadores graficos.
- Gradle Wrapper descarga el resto. No se requiere PowerShell, Bash ni coreutils
  para verificaciones internas; `gradlew.bat` y `gradlew` son launchers necesarios.

## Compatibilidad multiplataforma

- Windows usa `gradlew.bat`; Linux/macOS usan `./gradlew`.
- Los medidores eligen el wrapper nativo y localizan herramientas JDK mediante
  `JAVA_HOME`, `PATH` o toolchains de Gradle.
- `tools/check_portability.py` rechaza scripts auxiliares por SO, shells externos,
  rutas absolutas, APIs de proceso Windows sin proteger y colisiones de case.
- `.gitattributes` fija LF para fuentes/scripts Unix, CRLF para Batch y binarios.

## Sistema de compilacion

`build.gradle`, `settings.gradle`, `gradle.properties` y `gradle/wrapper/` son la
interfaz de build. No actualizar Java, Gradle, NeoForge o plugins como parte de un
cambio ordinario. `check` integra arquitectura, dominio, graficos, recursos,
contratos, portabilidad y contenido del JAR.

## Sistema de pruebas

- `test`: pruebas Java puras; se permite que no descubra pruebas JUnit porque el
  contrato principal se ejecuta con `verifyDomainRules`.
- `runGameTestServer`: suites Minecraft bajo `src/gameTest`.
- `releaseCheck`: `check`, GameTests y un solo JAR publicable.
- Las mediciones reproducibles y sus umbrales viven en `tools/performance/README.md`.

## CI/CD

`.github/workflows/build.yml` ejecuta build y `releaseCheck` en Ubuntu. Una matriz
adicional ejecuta `clean check` en Windows y macOS para detectar rutas, permisos,
case, encoding y herramientas no portables sin triplicar los GameTests costosos.

## Comandos de compilacion y verificacion

Linux/macOS:

```text
./gradlew clean releaseCheck
./gradlew runClient
./gradlew runServer
```

Windows:

```text
gradlew.bat clean releaseCheck
gradlew.bat runClient
gradlew.bat runServer
```

Control rapido de tooling: `python tools/check_portability.py`. Evidencia final:
`recordReleaseEvidence`. Rutas y validacion dirigida: `PROJECT_INDEX.md`.

## Restricciones

- No cambiar mecanicas, probabilidades, tiempos, orden aleatorio, IDs, NBT,
  payloads ni recetas por una tarea de mantenimiento.
- No cargar clases cliente en servidor ni hacer obligatorios REI/Jade.
- No mover GameTests a `src/main`; nunca entran en el JAR.
- No conservar una optimizacion sin equivalencia funcional y medicion comparable.

## Problemas conocidos

- Falta completar la matriz manual OpenGL/Vulkan, REI, idiomas, escalas GUI, dos
  clientes, mods de compatibilidad, mundo existente e instalacion limpia.
- Vulkan desactiva solo en su perfil local la ventana temprana OpenGL afectada por
  `NeoForge#3230`; es una limitacion externa, no un workaround del mod.
- La evidencia actual corresponde al arbol de auditoria sin commit; debe repetirse
  tras crear el commit definitivo para asociarla a un estado Git limpio.

## Trabajo pendiente

- Ejecutar y adjuntar `docs/RELEASE_CHECKLIST.md` antes de publicar.
- Repetir dos `clean releaseCheck`, comparar JAR y registrar nueva evidencia.
- Ejecutar las plantillas de rendimiento activas, bloqueadas, automatizadas,
  multijugador, granjas y REI antes de otra optimizacion estructural. Ya existen
  referencias reales para 1.024 maquinas diagnosticadas y una red de 1.024 tubos.
- Futuras mecanicas estan aisladas en `docs/ROADMAP.md`.

## Ultimos cambios relevantes

- `2026-09-10`: mejoras por familia con recoloreado sin ruido y originales
  conservados; iconos de terminales, margen del editor, canales guardados,
  cantidades es/en ajustadas al slot, tooltip normal/avanzado y deposito con
  Shift doble clic. `releaseCheck` con 97 GameTests; pruebas visuales en
  `docs/LOGISTICS.md`. La matriz manual y la publicacion definitiva siguen abiertas.
- `2026-09-09`: tapas negras estaticas en conexiones a maquinas, arrastre completo
  de mejoras sin bloquear clics vacios/llenos y texto protegido frente a atajos de
  inventario. XP bidireccional por las seis caras verificado con tuberias reales.
  `clean releaseCheck` con 93 GameTests; capturas OpenGL/Vulkan en `docs/LOGISTICS.md`.
- `2026-09-08`: una mejora por cara con perfil propio y funciones acumulativas;
  cuatro modos de reparto (items cercano, resto equitativo por defecto), reglas
  avanzadas y marcador de inventarios; editor y autocompletado con memoria acotada
  por consulta. `clean releaseCheck` con 92 GameTests; rendimiento extremo pendiente.
- `2026-09-04`: diagnostico comun, Controlador, Configurador, redstone, red de XP,
  cinco familias configurables nuevas y Vexes; 47 GameTests y baselines reales de
  1.024 bloques.
- `2026-09-02`: Animales y Peces amplian a doce las familias configurables, con
  once objetivos vanilla, tags dinamicos y recetas de Infusor.
- `2026-08-30`: diez familias configurables de granjas, recetas y Spawn Eggs;
  recetario lila del Infusor con colocacion exacta; inventario futuro de criaturas.
- `2026-08-29`: herramientas de rendimiento portables, control automatico de
  portabilidad, CI Windows/macOS y documentacion de contexto por capas.
- Commits previos: `f9b8646` corrige el permiso de `gradlew`; `a944f56` incorpora
  granjas optimizadas de Creepers y Saqueadores.

## Informacion critica para futuras sesiones

Empieza por `PROJECT_INDEX.md`, abre una sola feature y sus tests, y consulta
`ARCHITECTURE.md` solo para una frontera. El historial conversacional anterior es
prescindible cuando contradiga al repositorio o a este archivo. Conserva siempre
las decisiones descartadas, contratos 1.0.x y pendientes de publicacion anteriores.
