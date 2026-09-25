# Project Context

## Objetivo del proyecto

Trading Cells es un mod NeoForge de maquinas portatiles para automatizar trabajo de
aldeanos y piglins. Conserva entidades, ofertas, inventarios, progreso, filtros y
XP al romper y recolocar los bloques. El codigo actual es la fuente de verdad.

## Estado actual

- Version de desarrollo no publicada: `1.0.0` para Minecraft `26.2.0` y
  NeoForge `26.2.0.88`.
- Java 25 y Gradle Wrapper 9.5; `gradlew` esta registrado como ejecutable.
- La referencia reproducible anterior se documento el `2026-08-31` en
  `docs/releases/1.0.0-validation.md`: dos JAR identicos con 26 GameTests. Es
  evidencia historica anterior a los hitos actuales. El arbol de desarrollo del
  `2026-09-10` pasa `releaseCheck`, incluido `check`, 100/100 GameTests y
  14 pruebas Python de recursos.
  Los terminales pasan clics reales y scroll en OpenGL/Vulkan con REI/Jade.
  Evidencia y JAR de desarrollo en `docs/LOGISTICS.md`; la matriz manual,
  reproducibilidad final y CI del commit definitivo siguen abiertas.
- El arbol actual conserva el recetario del Infusor, cultivos por datapack,
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
- Cultivos y Canteras para aldeanos y piglins con herramientas, Fortuna/Eficiencia,
  catalogos dinamicos, salidas parciales y persistencia. Los cultivos aceptan
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
- Las veintiuna granjas de entidades exponen automatizacion real: entrada desde
  arriba y laterales y extraccion de salidas desde cualquier cara. La restauracion
  transaccional de salidas no permite inserciones externas en ellas.
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
