# Project Context

## Objetivo del proyecto

Trading Cells es un mod NeoForge de maquinas portatiles para automatizar trabajo de
aldeanos y piglins. Conserva entidades, ofertas, inventarios, progreso, filtros y
XP al romper y recolocar los bloques. El codigo actual es la fuente de verdad.

## Estado actual

- Version publica: `1.0.0` para Minecraft `26.2.0` y NeoForge `26.2.0.57`.
- Java 25 y Gradle Wrapper 9.5; `gradlew` esta registrado como ejecutable.
- La validacion automatica mas reciente se documento el `2026-08-31` en
  `docs/releases/1.0.0-validation.md`: dos JAR identicos y 26/26 GameTests. La
  matriz manual y la CI del commit definitivo siguen abiertas.
- Cambios recientes añadieron granjas de Saqueadores/Creepers y optimizaciones de
  render y servidor. El arbol actual incorpora ademas diez familias configurables
  y un libro de recetas propio para el Infusor; 26/26 GameTests pasan. Las bases
  de Artropodos y Fantasmas usan musgo palido, el suelo de Slimes es opaco y el
  render del Ghast dispone de un desplazamiento vertical propio.

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
`incubators`, `farmer`, `quarry`, `converter`, `ironfarm`, `skeletonfarm`,
`zombiefarm`, `raiderfarm`, `creeperfarm`, `experience`, `infusion` y `silktouch`.
`configuredmobfarm` implementa por composicion Artropodos, Slimes, Guardianes,
Piglins, Blazes, Ghasts, Endermen, Shulkers, Breezes y Phantoms.
`platform/neoforge/mobfarm` mantiene el catalogo dinamico comun; las cuatro
familias historicas conservan su implementacion y las diez nuevas comparten una
Block Entity configurable sin compartir IDs persistentes.

## Funcionalidades implementadas

- Captura, liberacion, cria, incubacion, comercio manual/automatico y trueque.
- Cultivos y Canteras para aldeanos y piglins con herramientas, Fortuna/Eficiencia,
  catalogos dinamicos, salidas parciales y persistencia.
- Conversion de aldeanos, Granja de Hierro y granjas de Esqueletos, Zombis,
  Saqueadores, Creepers, Artropodos, Slimes, Guardianes, Piglins, Blazes, Ghasts,
  Endermen, Shulkers, Breezes y Phantoms con filtros, objetivos ampliables y XP.
- Almacen y fluido de XP, Infusor Arcano con recetario categorizado y recetas
  posicionales dispersas, y encantamientos propios.
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
  multijugador, granjas y REI antes de una optimizacion estructural.
- Futuras mecanicas estan aisladas en `docs/ROADMAP.md`.

## Ultimos cambios relevantes

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
